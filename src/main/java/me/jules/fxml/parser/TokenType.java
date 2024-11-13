package me.jules.fxml.parser;

public enum TokenType {
  EOF ("<eof>"),

  SEQUENCE ("<sequence>"),
  TAG_NAME ("<tag-name>"),
  IDENTIFIER ("<identifier>"),
  QUOTED_STRING ("<quoted-string>"),

  ARGUMENTS_START ("("),
  ARGUMENTS_END (")"),
  SQUIGGLY_OPEN ("{"),
  SQUIGGLY_CLOSE ("}"),
  EQUALS ("="),
  ;

  private final String string;

  TokenType(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }
}
