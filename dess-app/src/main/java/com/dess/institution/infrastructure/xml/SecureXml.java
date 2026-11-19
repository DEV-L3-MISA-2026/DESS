package com.dess.institution.infrastructure.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;

/**
 * @brief Hardened XML factories used by the institution XML infrastructure.
 *
 * <p>The factories disable external entity resolution and DOCTYPE declarations, and prevent network
 * or filesystem access from XML content. Configuration XML is treated as purely declarative data.</p>
 */
final class SecureXml
{
    private SecureXml()
    {
    }

    /**
     * Creates a hardened namespace-aware document builder factory.
     *
     * @return a document builder factory with secure processing enabled
     * @throws IllegalStateException if the platform parser cannot apply the required settings
     */
    static DocumentBuilderFactory documentBuilderFactory()
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            try
            {
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            }
            catch (IllegalArgumentException ignored)
            {
                // Attribute not supported by this implementation; the features above already protect it.
            }

            return factory;
        }
        catch (ParserConfigurationException e)
        {
            throw new IllegalStateException("Unable to configure a secure XML parser", e);
        }
    }

    /**
     * Creates a hardened schema factory for the institution XSD.
     *
     * @return a schema factory with secure processing enabled
     * @throws IllegalStateException if the platform schema factory cannot be configured
     */
    static SchemaFactory schemaFactory()
    {
        try
        {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            return factory;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to configure the XSD schema factory", e);
        }
    }

    /**
     * Creates a hardened transformer factory for XML serialization.
     *
     * @return a transformer factory with secure processing enabled
     * @throws IllegalStateException if the platform transformer factory cannot be configured
     */
    static TransformerFactory transformerFactory()
    {
        try
        {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            try
            {
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            }
            catch (IllegalArgumentException ignored)
            {
                // Attribute not supported by this implementation; secure processing is still enabled.
            }

            return factory;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to configure the XML transformer factory", e);
        }
    }
}
