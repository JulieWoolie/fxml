package me.jules.fxml.parser;

import static me.jules.fxml.parser.TokenType.ARGUMENTS_END;
import static me.jules.fxml.parser.TokenType.ARGUMENTS_START;
import static me.jules.fxml.parser.TokenType.EQUALS;
import static me.jules.fxml.parser.TokenType.IDENTIFIER;
import static me.jules.fxml.parser.TokenType.QUOTED_STRING;
import static me.jules.fxml.parser.TokenType.SEQUENCE;
import static me.jules.fxml.parser.TokenType.SQUIGGLY_CLOSE;
import static me.jules.fxml.parser.TokenType.SQUIGGLY_OPEN;
import static me.jules.fxml.parser.TokenType.TAG_NAME;

import java.util.Stack;
import java.util.StringJoiner;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Parser {

  private final TokenStream stream;
  private final ParserListener listener;
  private final DocumentBuilder dom;

  private Document document;
  private Stack<Element> stack = new Stack<>();

  public Parser(TokenStream stream, ParserListener listener, DocumentBuilder builder) {
    this.stream = stream;
    this.listener = listener;
    this.dom = builder;
  }

  public void warn(Location location, String format, Object... args) {
    listener.warn(location, format, args);
  }

  public void warn(String format, Object... args) {
    listener.warn(format, args);
  }

  public void error(Location location, String format, Object... args) {
    listener.error(location, format, args);
  }

  public void error(String format, Object... args) {
    listener.error(format, args);
  }

  public StreamState saveState() {
    return stream.saveState();
  }

  public LexerScope scope() {
    return stream.scope();
  }

  public void popScope() {
    stream.popScope();
  }

  public void pushScope(LexerScope scope) {
    stream.pushScope(scope);
  }

  public void restoreState(StreamState state) {
    stream.restoreState(state);
  }

  public boolean matches(TokenType... types) {
    Token p = peek();

    for (TokenType type : types) {
      if (type == p.type()) {
        return true;
      }
    }

    return false;
  }

  public Token expect(TokenType... types) {
    return expect(next(), types);
  }

  public Token expect(Token t, TokenType... types) {
    for (TokenType type : types) {
      if (type == t.type()) {
        return t;
      }
    }

    String expectedString;
    if (types.length == 1) {
      expectedString = types[0].toString();
    } else {
      StringJoiner joiner = new StringJoiner(", ", "any of ", "");
      for (TokenType type : types) {
        joiner.add(type.toString());
      }
      expectedString = joiner.toString();
    }

    error(t.start(), "Unexpected token! Expected %s, found %s",
       expectedString,
       t.toString()
    );

    return t;
  }

  public Token next() {
    return stream.next();
  }

  public Token peek() {
    return stream.peek();
  }

  public boolean hasNext() {
    return stream.hasNext();
  }

  public void skipWhitespace() {
    stream.skipWhitespace();
  }

  public Document parse() {
    skipWhitespace();

    Token peek = peek();
    expect(peek, TAG_NAME);

    document = dom.newDocument();
    Element root = element();
    document.appendChild(root);

    return document;
  }

  Element element() {
    Token t = expect(TAG_NAME);
    String name = t.input();

    Element e = document.createElement(name);
    skipWhitespace();

    if (matches(ARGUMENTS_START)) {
      next();
      skipWhitespace();

      while (!matches(ARGUMENTS_END)) {
        attribute(e);
        skipWhitespace();
      }

      next();
    }

    skipWhitespace();

    if (matches(SQUIGGLY_OPEN)) {
      next();

      while (true) {
        Token tk = peek();

        if (tk.type() == SEQUENCE) {
          next();

          if (tk.input().isBlank()) {
            continue;
          }

          Node n = document.createTextNode(tk.input());
          e.appendChild(n);

          continue;
        }

        if (tk.type() == SQUIGGLY_CLOSE) {
          next();
          break;
        }

        if (tk.type() == TAG_NAME) {
          Element el = element();
          e.appendChild(el);
          continue;
        }

        next();
        error(tk.start(), "Unexpected token: %s", tk.toString());
      }
    }

    return e;
  }

  void attribute(Element element) {
    Token attrToken = expect(QUOTED_STRING, IDENTIFIER);
    String attrName = attrToken.input();
    String attrValue;

    skipWhitespace();

    if (matches(EQUALS)) {
      next();
      skipWhitespace();

      if (matches(ARGUMENTS_END)) {
        attrValue = "";
      } else {
        Token valToken = expect(QUOTED_STRING, IDENTIFIER);
        attrValue = valToken.input();
      }
    } else {
      attrValue = "";
    }

    element.setAttribute(attrName, attrValue);
  }
}
