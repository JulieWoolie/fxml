package me.jules.fxml.parser;

public record StreamState(
    Location location,
    int currentChar,
    Token peeked,
    Location lastTokenStart
) {

}
