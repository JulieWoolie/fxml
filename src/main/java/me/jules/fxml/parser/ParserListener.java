package me.jules.fxml.parser;

public interface ParserListener {

  void warn(Location location, String format, Object... args);

  void warn(String format, Object... args);

  void error(Location location, String format, Object... args);

  void error(String format, Object... args);
}
