package com.dess.institution.infrastructure.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;

/**
 * Fabriques XML durcies contre XXE : DOCTYPE interdit, aucune entité externe, aucun accès réseau
 * ni fichier depuis le XML. Le XML est traité comme des données purement déclaratives.
 */
final class SecureXml {

    private SecureXml() {
    }

    static DocumentBuilderFactory documentBuilderFactory() {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            try {
                f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            } catch (IllegalArgumentException ignored) {
                // attribut non supporté par l'implémentation : les features ci-dessus protègent déjà
            }
            return f;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Impossible de configurer un parseur XML sécurisé", e);
        }
    }

    static SchemaFactory schemaFactory() {
        try {
            SchemaFactory f = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return f;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de configurer la fabrique de schémas XSD", e);
        }
    }

    static TransformerFactory transformerFactory() {
        try {
            TransformerFactory f = TransformerFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            try {
                f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                f.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            } catch (IllegalArgumentException ignored) {
                // voir ci-dessus
            }
            return f;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de configurer la fabrique de transformation XML", e);
        }
    }
}
