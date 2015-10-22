package com.leidos.xchangecore.core.em.util;

import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.structures.WorkProductDocument;

import gov.ucore.ucore.x20.DigestDocument;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.uicds.incident.IncidentDocument;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument;
import x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument;

import org.uicds.sensorService.SensorObservationInfoDocument;

public class DigestGenerator {

    private javax.xml.transform.Source xsltSource;

    private ClassPathResource xsltResource;

    private ClassPathResource iconConfigXmlResource;

    private javax.xml.transform.TransformerFactory transformerFactory;

    private javax.xml.transform.Transformer transformer;

    private String iconStr = "";

    private String colorStr = "";

    private String scaleStr = "";

    private String activityCatStr = "";

    private final String iconTag = "<SimpleProperty ucore:label=\"Icon\" ucore:code=\"icon\" ucore:codespace=\"http://uicds.us/style\"></SimpleProperty>";
    private final String iconBegTag = "<SimpleProperty ucore:label=\"Icon\" ucore:code=\"icon\" ucore:codespace=\"http://uicds.us/style\">";
    private final String iconEndTag = "</SimpleProperty>";

    private final String colorTag = "<SimpleProperty ucore:label=\"Color\" ucore:code=\"color\" ucore:codespace=\"http://uicds.us/style\"></SimpleProperty>";
    private final String colorBegTag = "<SimpleProperty ucore:label=\"Color\" ucore:code=\"color\" ucore:codespace=\"http://uicds.us/style\">";
    private final String colorEndTag = "</SimpleProperty>";

    private final String scaleTag = "<SimpleProperty ucore:label=\"Scalar\" ucore:code=\"scalar\" ucore:codespace=\"http://uicds.us/style\"></SimpleProperty>";
    private final String scaleBegTag = "<SimpleProperty ucore:label=\"Scalar\" ucore:code=\"scalar\" ucore:codespace=\"http://uicds.us/style\">";
    private final String scaleEndTag = "</SimpleProperty>";

    Logger log = LoggerFactory.getLogger(this.getClass());

    private static String convertStreamToString(InputStream is) throws Exception {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        is.close();
        return sb.toString();
    }

    public DigestGenerator(String xsltFilePath, String iconConfigXmlFilePath) {

        // Setup XSLT source for creating the digest of Incident work products
        xsltResource = new ClassPathResource(xsltFilePath);
        if (!xsltResource.exists()) {
            log.error("Can't find XSLT to create digest: " + xsltFilePath);
        }

        iconConfigXmlResource = new ClassPathResource(iconConfigXmlFilePath);
        if (!iconConfigXmlResource.exists()) {
            log.error("Can't find XML to create use for icon config: " + iconConfigXmlFilePath);
        }

        xsltSource = null;
        try {
            String string1 = convertStreamToString(xsltResource.getInputStream());
            // log.info("xslt=" + string1);
            xsltSource = new javax.xml.transform.stream.StreamSource(xsltResource.getInputStream());
        } catch (IOException e1) {
            log.error("Error reading " + xsltFilePath + "as XSLT source.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // create an instance of TransformerFactory
        try {
            transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
        } catch (TransformerFactoryConfigurationError e) {
            log.error("Error creating TransformerFactory: " + e.getMessage());
        }

        // create the transformer
        try {
            transformer = transformerFactory.newTransformer(xsltSource);
            // log.info("Transformer ready.");
        } catch (TransformerConfigurationException e) {
            log.error("Error creating Transformer: " + e.getMessage());
            log.error("Cause: " + e.getCause());
            log.error("Exception: " + e.getException());
            log.error("Location as string: " + e.getLocationAsString());
            log.error("Message and Location: " + e.getMessageAndLocation());
            e.printStackTrace();
        }
    }

    private ByteArrayOutputStream createDigestFromDoc(SensorObservationInfoDocument soiDoc,
                                                      ByteArrayOutputStream baos) {

        javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);
        try {
            // log.info("incidentDoc="+incidentDoc.toString());
            javax.xml.transform.Source xmlSource = new javax.xml.transform.dom.DOMSource(soiDoc.getDomNode());
            transformer.transform(xmlSource, result);
        } catch (Throwable e) {
            log.error("Transformation failed: " + e.getMessage() + ".");
            e.printStackTrace();
        }
        return baos;
    }

    private synchronized ByteArrayOutputStream createDigestFromDoc(IncidentDocument incidentDoc,
                                                                   ByteArrayOutputStream baos) {

        javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);
        try {
            // log.info("incidentDoc="+incidentDoc.toString());
            javax.xml.transform.Source xmlSource = new javax.xml.transform.dom.DOMSource(incidentDoc.getDomNode());
            transformer.transform(xmlSource, result);
        } catch (Throwable e) {
            log.error("Transformation failed: " + e.getMessage() + ".");
            e.printStackTrace();
        }
        return baos;
    }

    private ByteArrayOutputStream createDigestFromDoc(RequestResourceDocument reqResDoc,
                                                      ByteArrayOutputStream baos) {

        javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);
        try {
            // log.info("reqResDoc=" + reqResDoc.toString());
            javax.xml.transform.Source xmlSource = new javax.xml.transform.dom.DOMSource(reqResDoc.getDomNode());
            transformer.transform(xmlSource, result);
        } catch (Throwable e) {
            log.error("Transformation failed: " + e.getMessage() + ".");
            e.printStackTrace();
        }
        return baos;
    }

    private ByteArrayOutputStream createDigestFromDoc(CommitResourceDocument comResDoc,
                                                      ByteArrayOutputStream baos) {

        javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);
        try {
            log.info("comResDoc=" + comResDoc.toString());
            javax.xml.transform.Source xmlSource = new javax.xml.transform.dom.DOMSource(comResDoc.getDomNode());
            transformer.transform(xmlSource, result);
        } catch (Throwable e) {
            log.error("Transformation failed: " + e.getMessage() + ".");
            e.printStackTrace();
        }
        return baos;
    }

    public DigestDocument insertIcon(DigestDocument digestDoc, IncidentDocument incidentDoc) {

        DigestDocument resultDigestDoc = DigestDocument.Factory.newInstance();

        try {

            // get activitytype from incident
            // We map the prefixes to URIs
            String incDocString = incidentDoc.toString();
            // System.out.println("incDocString="+incDocString);
            InputStream incIS = new ByteArrayInputStream(incDocString.getBytes());
            DocumentBuilderFactory incidentDomFactory = DocumentBuilderFactory.newInstance();
            incidentDomFactory.setNamespaceAware(true); // never forget this!
            NamespaceContext incidentCtx = new NamespaceContext() {

                public String getNamespaceURI(String prefix) {

                    String uri;
                    if (prefix.equals("ns1"))
                        uri = "http://niem.gov/niem/niem-core/2.0";
                    else if (prefix.equals("inc"))
                        uri = "http://uicds.org/incident";
                    else
                        uri = null;
                    return uri;
                }

                // Dummy implementation - not used!
                public Iterator getPrefixes(String val) {

                    return null;
                }

                // Dummy implemenation - not used!
                public String getPrefix(String uri) {

                    return null;
                }
            };
            DocumentBuilder incBuilder = incidentDomFactory.newDocumentBuilder();
            Document incDoc = incBuilder.parse(incIS);
            XPathFactory incidentFactory = XPathFactory.newInstance();
            XPath incidentXpath = incidentFactory.newXPath();
            incidentXpath.setNamespaceContext(incidentCtx);
            XPathExpression incidentExpr = incidentXpath.compile("//inc:Incident/ns1:ActivityCategoryText/text()");
            Object incidentResult = incidentExpr.evaluate(incDoc, XPathConstants.NODESET);
            NodeList incNodes = (NodeList) incidentResult;
            for (int i = 0; i < incNodes.getLength(); i++) {
                activityCatStr = incNodes.item(i).getNodeValue();
                // System.out.println("activityCatStr="+activityCatStr);
            }

            // Get types_icons values
            String iconXmlString = convertStreamToString(iconConfigXmlResource.getInputStream());
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true); // never forget this!
            // We map the prefixes to URIs
            NamespaceContext ctx = new NamespaceContext() {

                public String getNamespaceURI(String prefix) {

                    String uri;
                    if (prefix.equals("tim"))
                        uri = "http://uicds.org/typeiconmap";
                    else if (prefix.equals("ns2"))
                        uri = "http://uicds.org";
                    else
                        uri = null;
                    return uri;
                }

                // Dummy implementation - not used!
                public Iterator getPrefixes(String val) {

                    return null;
                }

                // Dummy implemenation - not used!
                public String getPrefix(String uri) {

                    return null;
                }
            };
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            Document doc = builder.parse(iconConfigXmlResource.getInputStream());
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            xpath.setNamespaceContext(ctx);
            // get icon
            XPathExpression expr = xpath.compile("//tim:TypeIconMapDocument/tim:Mapping[tim:WorkProduct='Incident'][tim:Event='" +
                                                 activityCatStr + "']/tim:Icon/text()");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                iconStr = nodes.item(i).getNodeValue();
                // System.out.println("iconStr="+iconStr);
            }
            // get color
            XPathExpression expr2 = xpath.compile("//tim:TypeIconMapDocument/tim:Mapping[tim:WorkProduct='Incident'][tim:Event='" +
                                                  activityCatStr + "']/tim:Color/text()");
            Object result2 = expr2.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes2 = (NodeList) result2;
            for (int i = 0; i < nodes2.getLength(); i++) {
                colorStr = nodes2.item(i).getNodeValue();
                // System.out.println("colorStr="+colorStr);
            }
            // get scale
            XPathExpression expr3 = xpath.compile("//tim:TypeIconMapDocument/tim:Mapping[tim:WorkProduct='Incident'][tim:Event='" +
                                                  activityCatStr + "']/tim:Scale/text()");
            Object result3 = expr3.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes3 = (NodeList) result3;
            for (int i = 0; i < nodes3.getLength(); i++) {
                scaleStr = nodes3.item(i).getNodeValue();
                // System.out.println("scaleStr="+scaleStr);
            }

            String digestDocStr = digestDoc.toString();

            // log.info("digestDocStr before="+digestDocStr);

            digestDocStr = digestDocStr.replaceFirst(iconTag, iconBegTag + iconStr + iconEndTag);
            digestDocStr = digestDocStr.replaceFirst(colorTag, colorBegTag + colorStr + colorEndTag);
            digestDocStr = digestDocStr.replaceFirst(scaleTag, scaleBegTag + scaleStr + scaleEndTag);

            // log.info("digestDocStr after="+digestDocStr);

            InputStream is = new ByteArrayInputStream(digestDocStr.getBytes());

            // ByteArrayInputStream resultBais = new ByteArrayInputStream(testStr.toByteArray());
            resultDigestDoc = DigestDocument.Factory.parse(is);

        } catch (IOException ioe) {
            log.error("IOException insertIcon: " + ioe.getMessage());
        } catch (SAXException se) {
            log.error("SAXException insertIcon: " + se.getMessage());
        } catch (Exception e) {
            log.error("Exception insertIcon: " + e.getMessage());
        }
        return resultDigestDoc;
        // return digestDoc;
    }

    public DigestDocument createDigest(SensorObservationInfoDocument soiDoc) {

        DigestDocument digestDoc = DigestDocument.Factory.newInstance();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (xsltSource == null) {
            log.info("SOI XSLT is missing!");
            return digestDoc;
        } else {
            log.info("SOI XSLT is found. - OK");
        }

        if (transformer == null) {
            log.info("Transformer is null!");
            return digestDoc;
        } else {
            log.info("Transformer is ready. - OK");
        }
        baos = createDigestFromDoc(soiDoc, baos);
        //log.info("baos.size()="+baos.size());

        try {
            if (baos.size() > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                digestDoc = DigestDocument.Factory.parse(bais);
            } else {
                digestDoc.addNewDigest();
            }
        } catch (XmlException e) {
            log.error("XmlException Failed parsing digest: " + e.getMessage());
        } catch (IOException e) {
            log.error("IOException Failed parsing digest: " + e.getMessage());
        }

        //log.info ("createDigest(SensorObservationInfo soi)  the final digestDoc="+digestDoc);

        return digestDoc;
    }

    public DigestDocument createDigest(CommitResourceDocument comResDoc) {

        DigestDocument digestDoc = DigestDocument.Factory.newInstance();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (xsltSource == null) {
            log.info("CommitResource XSLT is missing!");
            return digestDoc;
        } else {
            log.info("CommitResource XSLT is found. - OK");
        }

        if (transformer == null) {
            log.info("Transformer is null!");
            return digestDoc;
        } else {
            log.info("Transformer is ready. - OK");
        }
        baos = createDigestFromDoc(comResDoc, baos);
        //log.info("baos.size()="+baos.size());

        try {
            if (baos.size() > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                digestDoc = DigestDocument.Factory.parse(bais);
            } else {
                digestDoc.addNewDigest();
            }
        } catch (XmlException e) {
            log.error("XmlException Failed parsing digest: " + e.getMessage());
        } catch (IOException e) {
            log.error("IOException Failed parsing digest: " + e.getMessage());
        }

        //insert SimpleProperties icon based on config file
        //digestDoc=insertIcon(digestDoc, reqResDoc);

        log.info("createDigest(CommitResourceDocument comResDoc)  the final digestDoc=" + digestDoc);

        return digestDoc;
    }

    public DigestDocument createDigest(RequestResourceDocument reqResDoc) {

        DigestDocument digestDoc = DigestDocument.Factory.newInstance();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (xsltSource == null) {
            log.info("RequestResource XSLT is missing!");
            return digestDoc;
        } else {
            log.debug("RequestResource XSLT is found. - OK");
        }

        if (transformer == null) {
            log.info("Transformer is null!");
            return digestDoc;
        } else {
            log.debug("Transformer is ready. - OK");
        }
        baos = createDigestFromDoc(reqResDoc, baos);
        // log.info("baos.size()="+baos.size());

        try {
            if (baos.size() > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                digestDoc = DigestDocument.Factory.parse(bais);
            } else {
                digestDoc.addNewDigest();
            }
        } catch (XmlException e) {
            log.error("XmlException Failed parsing digest: " + e.getMessage());
        } catch (IOException e) {
            log.error("IOException Failed parsing digest: " + e.getMessage());
        }

        // insert SimpleProperties icon based on config file
        // digestDoc=insertIcon(digestDoc, reqResDoc);

        // log.info("createDigest(RequestResourceDocument reqResDoc)  the final digestDoc="
        // + digestDoc);

        return digestDoc;
    }

    //  ------------------  BEGIN NEW CODE ----------------------------------

    public DigestDocument createDigest(WorkProduct workproduct) {

        DigestDocument digestDoc = DigestDocument.Factory.newInstance();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (xsltSource == null) {
            log.info("Incident XSLT is missing!");
            return digestDoc;
        } else {
            log.debug("Incident XSLT is found. - OK");
        }

        if (transformer == null) {
            log.info("transformer is null!");
            return digestDoc;
        } else {
            log.debug("Transformer is ready. - OK");
        }

        // create WP Document from model
        WorkProductDocument wpDoc = WorkProductHelper.toWorkProductDocument(workproduct);

        baos = createDigestFromDoc(wpDoc, baos);

        try {
            if (baos.size() > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                digestDoc = DigestDocument.Factory.parse(bais);
            } else {
                digestDoc.addNewDigest();
            }
        } catch (XmlException e) {
            log.error("XmlException Failed parsing digest: " + e.getMessage());
        } catch (IOException e) {
            log.error("IOException Failed parsing digest: " + e.getMessage());
        }

        return digestDoc;
    }

    private ByteArrayOutputStream createDigestFromDoc(WorkProductDocument wpDoc,
                                                      ByteArrayOutputStream baos) {

        javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);
        try {
            // log.info("wpDoc="+wpDoc.toString());
            javax.xml.transform.Source xmlSource = new javax.xml.transform.dom.DOMSource(wpDoc.getDomNode());
            transformer.transform(xmlSource, result);
        } catch (Throwable e) {
            log.error("Transformation failed: " + e.getMessage() + ".");
            e.printStackTrace();
        }
        return baos;
    }

    //  ------------------  END NEW CODE ----------------------------------

    public DigestDocument createDigest(IncidentDocument incidentDoc) {

        DigestDocument digestDoc = DigestDocument.Factory.newInstance();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (xsltSource == null) {
            log.info("Incident XSLT is missing!");
            return digestDoc;
        } else {
            log.debug("Incident XSLT is found. - OK");
        }

        if (transformer == null) {
            log.info("transformer is null!");
            return digestDoc;
        } else {
            log.debug("Transformer is ready. - OK");
        }

        baos = createDigestFromDoc(incidentDoc, baos);
        // log.info("baos.size()="+baos.size());

        try {
            if (baos.size() > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                digestDoc = DigestDocument.Factory.parse(bais);
            } else {
                digestDoc.addNewDigest();
            }
        } catch (XmlException e) {
            log.error("XmlException Failed parsing digest: " + e.getMessage());
        } catch (IOException e) {
            log.error("IOException Failed parsing digest: " + e.getMessage());
        }

        // insert SimpleProperties icon based on config file
        digestDoc = insertIcon(digestDoc, incidentDoc);

        // log.info ("the final digestDoc="+digestDoc);

        return digestDoc;
    }

}
