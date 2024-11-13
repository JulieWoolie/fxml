package me.jules.fxml.parser;

public record Token(TokenType type, String input, Location start, Location end) {

  @Override
  public String toString() {
    if (input == null || input.isEmpty()) {
      return type.toString();
    }

    return "%s(%s)".formatted(
        type.toString(),
        input
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    );
  }
}
