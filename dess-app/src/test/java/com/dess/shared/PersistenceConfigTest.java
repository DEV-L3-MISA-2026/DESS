package com.dess.shared;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @brief Verifies the Jakarta Persistence configuration of the application.
 *
 * <p>The persistence unit must use the WildFly datasource and must leave all schema changes to
 * Flyway.</p>
 */
class PersistenceConfigTest
{
    private static final String PERSISTENCE_NAMESPACE = "https://jakarta.ee/xml/ns/persistence";

    private static final Path PERSISTENCE_XML = Path.of(
            "src", "main", "resources", "META-INF", "persistence.xml");

    @Test
    void persistenceUnitUsesDessDatasourceAndDisablesSchemaGeneration() throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(PERSISTENCE_XML.toFile());

        NodeList units = document.getElementsByTagNameNS(PERSISTENCE_NAMESPACE, "persistence-unit");
        assertEquals(1, units.getLength());

        Element unit = (Element) units.item(0);
        assertEquals("dessPU", unit.getAttribute("name"));
        assertEquals("JTA", unit.getAttribute("transaction-type"));
        assertEquals("java:/jdbc/DessDS", textOf(unit, "jta-data-source"));
        assertEquals("org.hibernate.jpa.HibernatePersistenceProvider", textOf(unit, "provider"));
        assertEquals("none", propertyValue(unit, "hibernate.hbm2ddl.auto"));
        assertEquals("none", propertyValue(unit, "jakarta.persistence.schema-generation.database.action"));
    }

    private static String textOf(Element parent, String localName)
    {
        NodeList nodes = parent.getElementsByTagNameNS(PERSISTENCE_NAMESPACE, localName);

        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }

    private static String propertyValue(Element unit, String name)
    {
        NodeList properties = unit.getElementsByTagNameNS(PERSISTENCE_NAMESPACE, "property");

        for (int index = 0; index < properties.getLength(); index++)
        {
            Element property = (Element) properties.item(index);

            if (name.equals(property.getAttribute("name")))
            {
                return property.getAttribute("value");
            }
        }

        return null;
    }
}
