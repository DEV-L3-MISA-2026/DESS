package com.dess.institution.infrastructure.xml;

import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeType;
import com.dess.institution.domain.AttributeValue;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.ConfigurationLoadException;
import com.dess.institution.exception.MalformedXmlException;
import com.dess.institution.exception.SchemaViolationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @brief Reads institution configuration from XML.
 *
 * <p>The loader performs well-formedness and XSD validation, then maps the DOM to domain objects.
 * It contains no business rule: semantic validation is applied afterwards by
 * {@link com.dess.institution.validation.InstitutionValidator}.</p>
 */
public final class XmlConfigLoader
{
    /**
     * Classpath location of the configuration schema.
     */
    static final String XSD_RESOURCE = "/institution.xsd";

    private final Schema schema;

    /**
     * Creates a loader using the bundled {@code institution.xsd} schema.
     *
     * @throws IllegalStateException if the schema resource is missing or invalid
     */
    public XmlConfigLoader()
    {
        this.schema = loadDefaultSchema();
    }

    /**
     * Loads and validates a configuration file.
     *
     * @param file XML file to read
     * @return the parsed configuration
     * @throws ConfigurationLoadException if the file cannot be read
     * @throws MalformedXmlException if the file is not well-formed XML
     * @throws SchemaViolationException if the document violates the XSD
     */
    public InstitutionConfig load(Path file)
    {
        try (InputStream in = Files.newInputStream(file))
        {
            return load(in, file.toString());
        }
        catch (IOException e)
        {
            throw new ConfigurationLoadException(
                "Unable to read file " + file + ": " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Parses and validates a configuration from an XML string.
     *
     * @param xml XML document
     * @return the parsed configuration
     * @throws MalformedXmlException if the string is not well-formed XML
     * @throws SchemaViolationException if the document violates the XSD
     */
    public InstitutionConfig parse(String xml)
    {
        return read(new InputSource(new StringReader(xml)), "<XML string>");
    }

    /**
     * Loads and validates a configuration from a stream.
     *
     * @param in         XML input stream; the caller remains responsible for closing it
     * @param sourceName source name used in error messages
     * @return the parsed configuration
     * @throws MalformedXmlException if the stream is not well-formed XML
     * @throws SchemaViolationException if the document violates the XSD
     * @throws ConfigurationLoadException if the stream cannot be read
     */
    public InstitutionConfig load(InputStream in, String sourceName)
    {
        return read(new InputSource(in), sourceName);
    }

    private InstitutionConfig read(InputSource source, String sourceName)
    {
        Document document = parseAndValidate(source, sourceName);

        return toConfig(document.getDocumentElement());
    }

    private Document parseAndValidate(InputSource source, String sourceName)
    {
        DocumentBuilderFactory factory = SecureXml.documentBuilderFactory();
        factory.setSchema(schema);

        List<String> schemaErrors = new ArrayList<>();

        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler()
            {
                @Override
                public void warning(SAXParseException e)
                {
                    // Schema warnings do not invalidate the document.
                }

                @Override
                public void error(SAXParseException e)
                {
                    schemaErrors.add("line " + e.getLineNumber() + ": " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException
                {
                    throw e;
                }
            });

            Document document = builder.parse(source);

            if (!schemaErrors.isEmpty())
            {
                throw new SchemaViolationException(sourceName, schemaErrors);
            }

            return document;
        }
        catch (SAXParseException e)
        {
            throw new MalformedXmlException(
                "File " + sourceName + " is not valid XML (line "
                + e.getLineNumber() + "): " + e.getMessage(),
                e
            );
        }
        catch (SAXException e)
        {
            throw new MalformedXmlException(
                "File " + sourceName + " is not valid XML: " + e.getMessage(),
                e
            );
        }
        catch (IOException e)
        {
            throw new ConfigurationLoadException(
                "Unable to read " + sourceName + ": " + e.getMessage(),
                e
            );
        }
        catch (ParserConfigurationException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Maps the document root to a configuration.
     *
     * @param root {@code institution} element
     * @return the mapped configuration
     */
    private InstitutionConfig toConfig(Element root)
    {
        List<UnitTypeDefinition> types = new ArrayList<>();
        Element typesElement = child(root, "unitTypes");

        if (typesElement != null)
        {
            for (Element typeElement : children(typesElement, "type"))
            {
                types.add(readType(typeElement));
            }
        }

        List<AllowedParentChildPair> pairs = new ArrayList<>();
        Element pairsElement = child(root, "allowedParentChildPairs");

        if (pairsElement != null)
        {
            for (Element allowElement : children(pairsElement, "allow"))
            {
                pairs.add(new AllowedParentChildPair(
                    attr(allowElement, "parent"),
                    attr(allowElement, "child")
                ));
            }
        }

        List<OrganizationalUnit> units = new ArrayList<>();
        Element unitsElement = child(root, "units");

        if (unitsElement != null)
        {
            for (Element unitElement : children(unitsElement, "unit"))
            {
                readUnit(unitElement, null, units);
            }
        }

        return new InstitutionConfig(
            attr(root, "id"),
            attr(root, "code"),
            attr(root, "name"),
            attr(root, "description"),
            types,
            pairs,
            units
        );
    }

    /**
     * Maps one {@code type} element and its attribute definitions.
     *
     * @param typeElement {@code type} element
     * @return the mapped unit type
     */
    private UnitTypeDefinition readType(Element typeElement)
    {
        List<AttributeDefinition> attributes = new ArrayList<>();
        Element attributesElement = child(typeElement, "attributes");

        if (attributesElement != null)
        {
            for (Element attributeElement : children(attributesElement, "attribute"))
            {
                attributes.add(new AttributeDefinition(
                    attr(attributeElement, "key"),
                    attr(attributeElement, "label"),
                    attr(attributeElement, "description"),
                    AttributeType.valueOf(attr(attributeElement, "type")),
                    bool(attributeElement, "required", false),
                    attr(attributeElement, "default")
                ));
            }
        }

        return new UnitTypeDefinition(
            attr(typeElement, "code"),
            attr(typeElement, "label"),
            attr(typeElement, "description"),
            bool(typeElement, "active", true),
            attributes
        );
    }

    /**
     * Maps a unit and its nested children. The parent identifier is derived from the XML nesting.
     *
     * @param element  {@code unit} element
     * @param parentId identifier of the enclosing unit, or {@code null} for a root unit
     * @param out      list receiving the unit and its descendants in document order
     */
    private void readUnit(Element element, String parentId, List<OrganizationalUnit> out)
    {
        Map<String, AttributeValue> values = new LinkedHashMap<>();
        Element valuesElement = child(element, "values");

        if (valuesElement != null)
        {
            for (Element valueElement : children(valuesElement, "value"))
            {
                String key = attr(valueElement, "key");

                // Duplicate keys are already rejected by the xs:unique constraint of the XSD.
                values.put(key, new AttributeValue(key, valueElement.getTextContent()));
            }
        }

        String id = attr(element, "id");
        out.add(new OrganizationalUnit(
            id,
            attr(element, "code"),
            attr(element, "name"),
            attr(element, "description"),
            attr(element, "type"),
            parentId,
            bool(element, "active", true),
            values
        ));

        Element childrenElement = child(element, "children");

        if (childrenElement != null)
        {
            for (Element childElement : children(childrenElement, "unit"))
            {
                readUnit(childElement, id, out);
            }
        }
    }

    /**
     * Returns an attribute value, or {@code null} when the attribute is absent.
     *
     * @param element element to read
     * @param name    attribute name
     * @return the attribute value, or {@code null}
     */
    private static String attr(Element element, String name)
    {
        return element.hasAttribute(name) ? element.getAttribute(name) : null;
    }

    /**
     * Reads a boolean attribute.
     *
     * @param element      element to read
     * @param name         attribute name
     * @param defaultValue value used when the attribute is absent
     * @return {@code true} for {@code true} or {@code 1}, {@code false} otherwise
     */
    private static boolean bool(Element element, String name, boolean defaultValue)
    {
        if (!element.hasAttribute(name))
        {
            return defaultValue;
        }

        String value = element.getAttribute(name).strip();

        return value.equals("true") || value.equals("1");
    }

    /**
     * Returns the first direct child element with the given name.
     *
     * @param parent parent element
     * @param name   child element name
     * @return the matching child, or {@code null} when none exists
     */
    private static Element child(Element parent, String name)
    {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
        {
            if (node instanceof Element element && element.getNodeName().equals(name))
            {
                return element;
            }
        }

        return null;
    }

    /**
     * Returns all direct child elements with the given name, in document order.
     *
     * @param parent parent element
     * @param name   child element name
     * @return a list of matching children, possibly empty
     */
    private static List<Element> children(Element parent, String name)
    {
        List<Element> result = new ArrayList<>();

        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
        {
            if (node instanceof Element element && element.getNodeName().equals(name))
            {
                result.add(element);
            }
        }

        return result;
    }

    /**
     * Loads the bundled XSD schema.
     *
     * @return the compiled schema
     * @throws IllegalStateException if the resource is missing or invalid
     */
    private static Schema loadDefaultSchema()
    {
        try (InputStream in = XmlConfigLoader.class.getResourceAsStream(XSD_RESOURCE))
        {
            if (in == null)
            {
                throw new IllegalStateException("Resource " + XSD_RESOURCE + " not found on the classpath");
            }

            return SecureXml.schemaFactory().newSchema(new StreamSource(in));
        }
        catch (IOException | SAXException e)
        {
            throw new IllegalStateException("institution.xsd schema unreadable or invalid", e);
        }
    }
}
