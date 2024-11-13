package me.jules.fxml.parser;

public class DefaultListener implements ParserListener {

  public static final String UNNAMED = "<unnamed source>";

  private final StringBuffer input;
  private final String sourceName;

  public DefaultListener(StringBuffer input, String sourceName) {
    this.input = input;
    this.sourceName = sourceName;
  }

  @Override
  public void warn(Location location, String format, Object... args) {
    String msg = format(input, location, format.formatted(args));
    System.out.print(msg);
    System.out.print("\n");
  }

  @Override
  public void warn(String format, Object... args) {
    warn(null, format, args);
  }

  @Override
  public void error(Location location, String format, Object... args) {
    String msg = format(input, location, format.formatted(args));
    throw new RuntimeException(msg);
  }

  @Override
  public void error(String format, Object... args) {
    error(null, format, args);
  }


  public String format(StringBuffer input, Location location, String message) {
    if (location == null) {
      return message;
    }

    int pos = location.index();

    final int lineStart = findLineBoundary(input, pos, -1);
    final int lineEnd = findLineBoundary(input, pos, 1);

    final int lineNumber = location.line();
    final int column = location.column();

    String lineNumStr = String.valueOf(lineNumber);
    String linePad = " ".repeat(lineNumStr.length());
    String context = input.substring(lineStart, lineEnd)
        .replace("\n", "")
        .replace("\r", "");

    StringBuilder builder = new StringBuilder();

    builder
        .append(message)

        .append('\n')
        .append(linePad)
        .append("--> ")
        .append(sourceName == null ? UNNAMED : sourceName)
        .append(':')
        .append(lineNumStr)
        .append(':')
        .append(column)

        .append('\n')
        .append(linePad)
        .append(" |")

        .append('\n')
        .append(lineNumStr)
        .append(" |")
        .append(context)

        .append('\n')
        .append(linePad)
        .append(" |")
        .append(" ".repeat(Math.max(0, column)))
        .append("^ ")
        .append(message)

        .append('\n')
        .append(linePad)
        .append(" |");

    return builder.toString();
  }

  static int findLineBoundary(StringBuffer buf, int pos, int direction) {
    int p = pos + direction;

    while (true) {
      if (p >= buf.length()) {
        return buf.length();
      }
      if (p <= 0) {
        return 0;
      }

      char ch = buf.charAt(p);

      if (ch == '\n' || ch == '\r') {
        if (direction == 1) {
          return p + 1;
        }

        return p;
      }

      p += direction;
    }
  }
}
