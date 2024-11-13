package me.jules.fxml.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import me.jules.fxml.Fxml;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

class ParserTest {

  @Test
  void test() throws IOException, ParserConfigurationException {
    URL input = getClass().getClassLoader().getResource("valid.fexml");
    InputStream inputStream = input.openStream();
    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    StringWriter writer = new StringWriter();
    reader.transferTo(writer);

    StringBuffer buf = writer.getBuffer();
    ParserListener listener = new DefaultListener(buf, "valid.fexml");

    TokenStream stream = new TokenStream(buf, listener);

    DocumentBuilder builder = Fxml.getDocumentBuilder();
    Parser parser = new Parser(stream, listener, builder);

    Document doc = parser.parse();
    System.out.println(doc.getTextContent());

    StringBuffer buffer = new StringBuffer();
    append(doc.getDocumentElement(), buffer, 0);

    System.out.println(buffer);
  }

  public void append(Node node, StringBuffer buffer, int indent) {
    Objects.requireNonNull(node);

    if (node instanceof Text text) {
      buffer.append(text.getTextContent());
      return;
    }

    if (node instanceof Element el) {
      buffer.append("<").append(el.getTagName());

      NamedNodeMap map = el.getAttributes();

      for (int i = 0; i < map.getLength(); i++) {
        Attr attr = (Attr) map.item(i);

        buffer.append(" ");
        buffer.append(attr.getName());
        buffer.append("=");
        buffer.append('"');
        buffer.append(attr.getValue());
        buffer.append('"');
      }

      NodeList childNodes = el.getChildNodes();

      if (childNodes.getLength() > 0) {
        buffer.append(">");
        indent++;

        for (int i = 0; i < childNodes.getLength(); i++) {
          Node child = childNodes.item(i);

          nlIndent(buffer, indent);
          append(child, buffer, indent);
        }

        indent--;

        nlIndent(buffer, indent);
        buffer.append("</").append(el.getTagName()).append(">");
      } else {
        buffer.append(" />");
      }
    }
  }

  private void nlIndent(StringBuffer buf, int indent) {
    buf.append("\n").append("  ".repeat(indent));
  }
}