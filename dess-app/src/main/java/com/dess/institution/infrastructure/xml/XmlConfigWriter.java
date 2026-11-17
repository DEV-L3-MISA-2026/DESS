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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sérialisation d'une {@link InstitutionConfig} en XML (UTF-8, indenté). Aucune règle métier :
 * la configuration doit avoir été validée avant l'écriture.
 */
public final class XmlConfigWriter {

    public byte[] toBytes(InstitutionConfig config) {
        try {
            DocumentBuilderFactory factory = SecureXml.documentBuilderFactory();
            Document doc = factory.newDocumentBuilder().newDocument();
            doc.setXmlStandalone(true);

            Element root = doc.createElement("institution");
            doc.appendChild(root);
            root.setAttribute("id", config.id());
            root.setAttribute("code", config.code());
            root.setAttribute("name", config.name());
            if (config.description() != null) {
                root.setAttribute("description", config.description());
            }

            Element typesEl = doc.createElement("unitTypes");
            root.appendChild(typesEl);
            for (UnitTypeDefinition type : config.unitTypes()) {
                typesEl.appendChild(typeElement(doc, type));
            }

            Element pairsEl = doc.createElement("allowedParentChildPairs");
            root.appendChild(pairsEl);
            for (AllowedParentChildPair pair : config.allowedPairs()) {
                Element allow = doc.createElement("allow");
                allow.setAttribute("parent", pair.parent());
                allow.setAttribute("child", pair.child());
                pairsEl.appendChild(allow);
            }

            Element unitsEl = doc.createElement("units");
            root.appendChild(unitsEl);
            writeUnits(doc, unitsEl, config);

            return serialize(doc);
        } catch (ConfigurationSaveException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationSaveException("Sérialisation XML impossible : " + e.getMessage(), e);
        }
    }

    private Element typeElement(Document doc, UnitTypeDefinition type) {
        Element el = doc.createElement("type");
        el.setAttribute("code", type.code());
        el.setAttribute("label", type.label());
        if (type.description() != null) {
            el.setAttribute("description", type.description());
        }
        if (!type.active()) {
            el.setAttribute("active", "false");
        }
        if (!type.attributes().isEmpty()) {
            Element attrsEl = doc.createElement("attributes");
            el.appendChild(attrsEl);
            for (AttributeDefinition def : type.attributes()) {
                Element a = doc.createElement("attribute");
                a.setAttribute("key", def.key());
                a.setAttribute("label", def.label());
                a.setAttribute("type", def.type().name());
                a.setAttribute("required", Boolean.toString(def.required()));
                if (def.description() != null) {
                    a.setAttribute("description", def.description());
                }
                if (def.defaultValue() != null) {
                    a.setAttribute("default", def.defaultValue());
                }
                attrsEl.appendChild(a);
            }
        }
        return el;
    }

    private void writeUnits(Document doc, Element unitsEl, InstitutionConfig config) {
        Map<String, List<OrganizationalUnit>> byParent = new HashMap<>();
        List<OrganizationalUnit> roots = new ArrayList<>();
        for (OrganizationalUnit u : config.units()) {
            if (u.parentId() == null) {
                roots.add(u);
            } else {
                byParent.computeIfAbsent(u.parentId(), k -> new ArrayList<>()).add(u);
            }
        }
        Set<String> written = new HashSet<>();
        for (OrganizationalUnit root : roots) {
            unitsEl.appendChild(unitElement(doc, root, byParent, written));
        }
        // Garde-fou : ne jamais perdre silencieusement une unité (parent inconnu ou cycle).
        if (written.size() != config.units().size()) {
            throw new ConfigurationSaveException("Configuration incohérente : " + (config.units().size() - written.size())
                    + " unité(s) inaccessible(s) depuis les racines ne pourraient pas être sauvegardées");
        }
    }

    private Element unitElement(Document doc, OrganizationalUnit unit,
                                Map<String, List<OrganizationalUnit>> byParent, Set<String> written) {
        if (!written.add(unit.id())) {
            throw new ConfigurationSaveException("Unité « " + unit.id() + " » rencontrée deux fois (cycle ?)");
        }
        Element el = doc.createElement("unit");
        el.setAttribute("id", unit.id());
        el.setAttribute("code", unit.code());
        el.setAttribute("name", unit.name());
        el.setAttribute("type", unit.typeCode());
        if (unit.description() != null) {
            el.setAttribute("description", unit.description());
        }
        el.setAttribute("active", Boolean.toString(unit.active()));

        if (!unit.values().isEmpty()) {
            Element valuesEl = doc.createElement("values");
            el.appendChild(valuesEl);
            for (AttributeValue v : unit.values().values()) {
                Element ve = doc.createElement("value");
                ve.setAttribute("key", v.key());
                ve.setTextContent(v.value());
                valuesEl.appendChild(ve);
            }
        }

        List<OrganizationalUnit> kids = byParent.getOrDefault(unit.id(), List.of());
        if (!kids.isEmpty()) {
            Element childrenEl = doc.createElement("children");
            el.appendChild(childrenEl);
            for (OrganizationalUnit kid : kids) {
                childrenEl.appendChild(unitElement(doc, kid, byParent, written));
            }
        }
        return el;
    }

    private byte[] serialize(Document doc) throws Exception {
        Transformer transformer = SecureXml.transformerFactory().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        String xml = out.toString(StandardCharsets.UTF_8);
        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
