package com.example.studytrack.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;

class BuildConfigurationTest {

  private static final String FORCE_CREATION_XPATH =
      "/*[local-name()='project']"
          + "/*[local-name()='build']"
          + "/*[local-name()='plugins']"
          + "/*[local-name()='plugin']"
          + "[*[local-name()='artifactId']='maven-jar-plugin']"
          + "/*[local-name()='configuration']"
          + "/*[local-name()='forceCreation']/text()";

  @Test
  void jarPluginRecreatesTheOriginalJarBeforeEveryShade() throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setFeature(
        "http://apache.org/xml/features/disallow-doctype-decl", true);
    documentBuilderFactory.setXIncludeAware(false);
    documentBuilderFactory.setExpandEntityReferences(false);

    var pomDocument =
        documentBuilderFactory.newDocumentBuilder().parse(Path.of("pom.xml").toFile());
    String forceCreation =
        XPathFactory.newInstance()
            .newXPath()
            .evaluate(FORCE_CREATION_XPATH, pomDocument)
            .strip();

    assertEquals(
        "true",
        forceCreation,
        """
        Build invariant violated: maven-jar-plugin must set forceCreation=true.
        Fix pom.xml so repeated mvnw verify runs recreate the original project JAR before Shade.
        See ARCHITECTURE.md section "构建幂等性".
        """);
  }
}
