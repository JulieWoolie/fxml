package me.jules.fxml.parser;

import java.util.Stack;

public class TokenStream {

  static final int EOF = -1;
  static final int LF = '\n';
  static final int CR = '\r';

  private final StringBuffer input;
  private final ParserListener errors;

  private int cursor  = 0;
  private int line    = 0;
  private int col     = 0;

  private int currentChar;

  private Location lastTokenStart;
  private Token peeked;

  private final Stack<LexerScope> scopeStack = new Stack<>();

  private String preCursor;
  private String postCursor;

  public TokenStream(StringBuffer input, ParserListener listener) {
    this.input = input;
    this.errors = listener;

    this.currentChar = charAt(0);
    updateContexts();
  }

  private void updateContexts() {
    if (cursor <= 0) {
      preCursor = "";
    } else {
      int start = Math.max(0, cursor - 10);
      preCursor = input.substring(start, cursor);
    }

    int end = Math.min(input.length(), cursor + 10);
    postCursor = input.substring(cursor, end);
  }

  public void pushScope(LexerScope scope) {
    scopeStack.push(scope);
  }

  public void popScope() {
    scopeStack.pop();
  }

  public LexerScope scope() {
    if (scopeStack.isEmpty()) {
      return LexerScope.REGULAR;
    }

    return scopeStack.peek();
  }

  public Location location() {
    return new Location(line, col, cursor);
  }

  public StreamState saveState() {
    return new StreamState(location(), currentChar, peeked, lastTokenStart);
  }

  public void restoreState(StreamState state) {
    this.line = state.location().line();
    this.col = state.location().column();
    this.cursor = state.location().index();

    this.currentChar = state.currentChar();

    this.peeked = state.peeked();
    this.lastTokenStart = state.lastTokenStart();
  }

  int ahead() {
    return ahead(1);
  }

  int ahead(int off) {
    return charAt(cursor + off);
  }

  void skip(String sequence) {
    skip(sequence.length());
  }

  void skip(int chars) {
    for (int i = 0; i < chars; i++) {
      advance();
    }
  }

  void advance() {
    int nCursor = cursor + 1;

    if (nCursor >= input.length()) {
      currentChar = EOF;
      cursor = nCursor;
      return;
    }

    int nChar = charAt(nCursor);

    if (nChar == LF || nChar == CR) {
      line++;
      col = 0;

      if (nChar == CR && charAt(nCursor + 1) == LF) {
        nCursor++;
      }

      // Normalize all line breaks to LF
      nChar = LF;
    } else {
      col++;
    }

    cursor = nCursor;
    currentChar = nChar;

    updateContexts();
  }

  int charAt(int index) {
    if (index < 0 || index >= input.length()) {
      return EOF;
    }

    return input.charAt(index);
  }

  public void skipWhitespace() {
    while (Character.isWhitespace(currentChar)) {
      advance();
    }
  }

  public boolean hasNext() {
    return peek().type() != TokenType.EOF;
  }

  public Token next() {
    if (peeked != null) {
      Token p = peeked;
      peeked = null;
      return p;
    }

    return readToken();
  }

  public Token peek() {
    if (peeked != null) {
      return peeked;
    }

    return peeked = readToken();
  }

  private Token readToken() {
    lastTokenStart = location();

    if (currentChar == EOF) {
      return token(TokenType.EOF);
    }

    if (currentChar == '}') {
      popScope();
      return token(TokenType.SQUIGGLY_CLOSE);
    }

    LexerScope scope = scope();

    if (scope == LexerScope.REGULAR) {
      if (isTagStart()) {
        pushScope(LexerScope.TAG);
        return readTagName();
      }

      String seq = readSequence();
      return token(TokenType.SEQUENCE, seq);
    }

    // scope == TAG
    switch (currentChar) {
      case '(':
        advance();
        pushScope(LexerScope.TAG_ATTRS);
        return token(TokenType.ARGUMENTS_START);
      case ')':
        advance();
        popScope();

        int c = cursor;
        while (Character.isWhitespace(charAt(c))) {
          c++;
        }
        if (charAt(c) != '}') {
          popScope();
        }

        return token(TokenType.ARGUMENTS_END);
      case '{':
        advance();
        pushScope(LexerScope.REGULAR);
        return token(TokenType.SQUIGGLY_OPEN);
      case '=':
        advance();
        return token(TokenType.EQUALS);

      case '"':
      case '\'':
      case '`':
        return readQuotedString();

      default:
        return readUntilWhitespace();
    }
  }

  private Token readUntilWhitespace() {
    StringBuffer buffer = new StringBuffer();

    while (isValidUnquotedStringChar(currentChar)) {
      buffer.appendCodePoint(currentChar);
      advance();
    }

    return token(TokenType.IDENTIFIER, buffer.toString());
  }

  private static boolean isValidUnquotedStringChar(int ch) {
    if (ch == '=' || ch == ')') {
      return false;
    }
    return !Character.isWhitespace(ch);
  }

  private Token readQuotedString() {
    int quote = currentChar;
    advance();

    StringBuffer buf = new StringBuffer();
    boolean escaped = false;

    while (true) {
      if (currentChar == quote) {
        advance();

        if (escaped) {
          buf.appendCodePoint(currentChar);
          escaped = false;
          continue;
        }

        break;
      }

      if (currentChar == '\\') {
        advance();

        if (escaped) {
          buf.append('\\');
          escaped = false;
        } else {
          escaped = true;
        }

        continue;
      }

      if (escaped) {
        escaped = false;
        int ch = currentChar;

        advance();

        switch (ch) {
          case 't', 'T' -> buf.append("\t");
          case 'n', 'N' -> buf.append("\n");
          case 'r', 'R' -> buf.append("\r");
          case 'u', 'U' -> buf.append(readHexChar());

          default -> {
            errors.error(location(), "Invalid escape sequence");
            continue;
          }
        };
      }

      buf.appendCodePoint(currentChar);
      advance();
    }

    return token(TokenType.QUOTED_STRING, buf.toString());
  }

  private String readHexChar() {
    int start = cursor;
    Location location = location();

    while (isHexChar(currentChar)) {
      advance();
    }

    String sub = input.substring(start, cursor);
    if (sub.length() != 4) {
      errors.error(location, "Invalid hex sequence");
    }

    int i = Integer.parseUnsignedInt(sub, 16);
    return Character.toString(i);
  }

  private static boolean isHexChar(int ch) {
    return (ch >= '0' && ch <= '9')
        || (ch >= 'a' && ch <= 'f')
        || (ch >= 'A' && ch <= 'F');
  }

  private String readSequence() {
    StringBuffer buf = new StringBuffer();

    while (isValidSequenceChar()) {
      buf.appendCodePoint(currentChar);
      advance();
    }

    return buf.toString();
  }

  private boolean isValidSequenceChar() {
    if (isTagStart()) {
      return false;
    }
    if (currentChar == '}') {
      return false;
    }

    return true;
  }

  private Token readTagName() {
    StringBuffer buf = new StringBuffer();
    while (isNameChar(currentChar)) {
      buf.appendCodePoint(currentChar);
      advance();
    }

    return token(TokenType.TAG_NAME, buf.toString());
  }

  public boolean isTagStart() {
    if (!isNameStartChar(currentChar)) {
      return false;
    }

    int c = cursor + 1;
    while (isNameChar(charAt(c))) {
      c++;
    }

    while (Character.isWhitespace(charAt(c))) {
      c++;
    }

    int ch = charAt(c);
    return ch == '{' || ch == '(';
  }

  private static boolean isNameChar(int ch) {
    return isNameStartChar(ch)
        || ch == '-'
        || ch == '.'
        || (ch >= '0' && ch <= '9')
        || ch == '\u00b7'
        || (ch >= '\u0300' && ch <= '\u036f')
        || (ch >= '\u203f' && ch <= '\u2040');
  }

  private static boolean isNameStartChar(int ch) {
    return (ch >= 'a' && ch <= 'z')
        || (ch >= 'A' && ch <= 'Z')
        || ch == '_'
        || ch == ':'
        || ch == '@'
        || ch == '$'
        || (ch >= '\u2070' && ch <= '\u218F')
        || (ch >= '\u2c00' && ch <= '\u2fef')
        || (ch >= '\u3001' && ch <= '\ud7ff')
        || (ch >= '\uf900' && ch <= '\ufdcf')
        || (ch >= '\ufdf0' && ch <= '\ufffd');
  }

  private Token token(TokenType type) {
    return token(type, null);
  }

  private Token token(TokenType type, String value) {
    Location start = lastTokenStart;
    Location end = location();

    if (start == null) {
      start = location();
    }

    return new Token(type, value, start, end);
  }
}
