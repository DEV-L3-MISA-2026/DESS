package com.dess.institution.infrastructure.xml;

import com.dess.institution.domain.AllowedParentChildPair;
import com.dess.institution.domain.AttributeDefinition;
import com.dess.institution.domain.AttributeValue;
import com.dess.institution.domain.InstitutionConfig;
import com.dess.institution.domain.OrganizationalUnit;
import com.dess.institution.domain.UnitTypeDefinition;
import com.dess.institution.exception.ConfigurationSaveException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @brief Serializes an {@link InstitutionConfig} to indented UTF-8 XML.
 *
 * <p>The writer contains no business rule: the configuration must have been validated before it is
 * written. The document is produced with the hardened factories of {@link SecureXml}.</p>
 */
public final class XmlConfigWriter
{
    /**
     * Serializes a configuration.
     *
     * @param config configuration to serialize
     * @return the UTF-8 XML document
     * @throws ConfigurationSaveException if the DOM cannot be built or serialized
     */
    public byte[] toBytes(InstitutionConfig config)
    {
        try
        {
            DocumentBuilderFactory factory = SecureXml.documentBuilderFactory();
            Document document = factory.newDocumentBuilder().newDocument();
            document.setXmlStandalone(true);

            Element root = document.createElement("institution");
            document.appendChild(root);
            root.setAttribute("id", config.id());
            root.setAttribute("code", config.code());
            root.setAttribute("name", config.name());

            if (config.description() != null)
            {
                root.setAttribute("description", config.description());
            }

            Element typesElement = document.createElement("unitTypes");
            root.appendChild(typesElement);

            for (UnitTypeDefinition type : config.unitTypes())
            {
                typesElement.appendChild(typeElement(document, type));
            }

            Element pairsElement = document.createElement("allowedParentChildPairs");
            root.appendChild(pairsElement);

            for (AllowedParentChildPair pair : config.allowedPairs())
            {
                Element allowElement = document.createElement("allow");
                allowElement.setAttribute("parent", pair.parent());
                allowElement.setAttribute("child", pair.child());
                pairsElement.appendChild(allowElement);
            }

            Element unitsElement = document.createElement("units");
            root.appendChild(unitsElement);
            writeUnits(document, unitsElement, config);

            return serialize(document);
        }
        catch (ParserConfigurationException | TransformerException e)
        {
            throw new ConfigurationSaveException("XML serialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the {@code type} element of a unit type.
     *
     * @param document owning document
     * @param type     unit type to serialize
     * @return the {@code type} element
     */
    private Element typeElement(Document document, UnitTypeDefinition type)
    {
        Element typeElement = document.createElement("type");
        typeElement.setAttribute("code", type.code());
        typeElement.setAttribute("label", type.label());

        if (type.description() != null)
        {
            typeElement.setAttribute("description", type.description());
        }

        if (!type.active())
        {
            typeElement.setAttribute("active", "false");
        }

        if (!type.attributes().isEmpty())
        {
            Element attributesElement = document.createElement("attributes");
            typeElement.appendChild(attributesElement);

            for (AttributeDefinition definition : type.attributes())
            {
                Element attributeElement = document.createElement("attribute");
                attributeElement.setAttribute("key", definition.key());
                attributeElement.setAttribute("label", definition.label());
                attributeElement.setAttribute("type", definition.type().name());
                attributeElement.setAttribute("required", Boolean.toString(definition.required()));

                if (definition.description() != null)
                {
                    attributeElement.setAttribute("description", definition.description());
                }

                if (definition.defaultValue() != null)
                {
                    attributeElement.setAttribute("default", definition.defaultValue());
                }

                attributesElement.appendChild(attributeElement);
            }
        }

        return typeElement;
    }

    /**
     * Writes the unit tree in document order.
     *
     * @param document     owning document
     * @param unitsElement {@code units} container
     * @param config       configuration being serialized
     * @throws ConfigurationSaveException if a unit is unreachable or encountered twice
     */
    private void writeUnits(Document document, Element unitsElement, InstitutionConfig config)
    {
        Map<String, List<OrganizationalUnit>> childrenByParent = config.childrenIndex();
        List<OrganizationalUnit> roots = config.units()
                                                .stream()
                                                .filter(OrganizationalUnit::isRoot)
                                                .toList();
        Set<String> written = new HashSet<>();

        for (OrganizationalUnit root : roots)
        {
            unitsElement.appendChild(unitElement(document, root, childrenByParent, written));
        }

        // Safety net: never silently lose a unit because of an unknown parent or a cycle.
        if (written.size() != config.units().size())
        {
            int missing = config.units().size() - written.size();
            throw new ConfigurationSaveException("Inconsistent configuration: " + missing
                                                 + " unit(s) unreachable from the roots "
                                                 + "could not be saved");
        }
    }

    /**
     * Builds a {@code unit} element and recursively appends its children.
     *
     * @param document         owning document
     * @param unit             unit to serialize
     * @param childrenByParent index from parent identifier to children
     * @param written          identifiers already serialized
     * @return the {@code unit} element
     * @throws ConfigurationSaveException if the unit is encountered twice
     */
    private Element unitElement(Document document, OrganizationalUnit unit,
                                Map<String, List<OrganizationalUnit>> childrenByParent, Set<String> written)
    {
        if (!written.add(unit.id()))
        {
            throw new ConfigurationSaveException("Unit '" + unit.id() + "' encountered twice (cycle?)");
        }

        Element unitElement = document.createElement("unit");
        unitElement.setAttribute("id", unit.id());
        unitElement.setAttribute("code", unit.code());
        unitElement.setAttribute("name", unit.name());
        unitElement.setAttribute("type", unit.typeCode());

        if (unit.description() != null)
        {
            unitElement.setAttribute("description", unit.description());
        }

        unitElement.setAttribute("active", Boolean.toString(unit.active()));

        if (!unit.values().isEmpty())
        {
            Element valuesElement = document.createElement("values");
            unitElement.appendChild(valuesElement);

            for (AttributeValue value : unit.values().values())
            {
                Element valueElement = document.createElement("value");
                valueElement.setAttribute("key", value.key());
                valueElement.setTextContent(value.value());
                valuesElement.appendChild(valueElement);
            }
        }

        List<OrganizationalUnit> children = childrenByParent.getOrDefault(unit.id(), List.of());

        if (!children.isEmpty())
        {
            Element childrenElement = document.createElement("children");
            unitElement.appendChild(childrenElement);

            for (OrganizationalUnit child : children)
            {
                childrenElement.appendChild(unitElement(document, child, childrenByParent, written));
            }
        }

        return unitElement;
    }

    /**
     * Serializes a DOM document to UTF-8 bytes.
     *
     * @param document document to serialize
     * @return the serialized bytes
     * @throws TransformerException if serialization fails
     */
    private byte[] serialize(Document document) throws TransformerException
    {
        Transformer transformer = SecureXml.transformerFactory().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(out));

        return out.toByteArray();
    }
}
