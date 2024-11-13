package me.jules.fxml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import me.jules.fxml.parser.DefaultListener;
import me.jules.fxml.parser.Parser;
import me.jules.fxml.parser.ParserListener;
import me.jules.fxml.parser.TokenStream;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

public final class Fxml {

  public static Document parseDocument(String in) throws ParserConfigurationException {
    StringBuffer buffer = new StringBuffer(in);
    ParserListener listener = new DefaultListener(buffer, null);
    TokenStream stream = new TokenStream(buffer, listener);

    Parser parser = new Parser(stream, listener, getDocumentBuilder());

    return parser.parse();
  }

  public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder();
  }
}
