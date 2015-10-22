package com.leidos.xchangecore.core.em.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageException;
import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageXMLException;

public class DetailedCFSMessageReader {

    Logger log = LoggerFactory.getLogger(DetailedCFSMessageReader.class);

    public static enum ActivityStatusText {
        CREATED, CLEARED
    };

    public static final String NS_DETAILED_CFS_INFO = "http://leitsc-lib/2.0/doc/DetailedCFSInformation";

    public static final String ROOT_ELEMENT = "/PostDetailedCFSMessageRequest";
    public static final String DETAILED_CFS_INFO = ROOT_ELEMENT + "/DetailedCFSInformation";
    public static final String PAYLOAD = DETAILED_CFS_INFO + "/Payload";

    public static final String SERVICE_CALL = PAYLOAD + "/ServiceCall";
    public static final String ACTIVITY_IDENTIFICATION = SERVICE_CALL + "/ActivityIdentification";
    public static final String ACTIVITY_DESC_TEXT = SERVICE_CALL + "/ActivityDescriptionText";
    public static final String ACTIVITY_STATUS = SERVICE_CALL + "/ActivityStatus";
    public static final String ACTIVITY_STATUS_TEXT = ACTIVITY_STATUS + "/StatusText";

    public static final String SERVICE_CALL_AUGMENTATION = SERVICE_CALL +
                                                           "/ServiceCallAugmentation";
    public static final String ARRIVED_DATE = SERVICE_CALL_AUGMENTATION + "/ServiceCallArrivedDate";
    public static final String ARRIVED_DATE_TIME = ARRIVED_DATE + "/DateTime";

    public static final String CALL_TYPE_TEXT = SERVICE_CALL_AUGMENTATION + "/CallTypeText";

    public static final String RESPONSE_LOCATION = SERVICE_CALL + "/ServiceCallResponseLocation";
    public static final String LOCATION_TWO_DIMENSIONAL_GEO_COORDINATE = RESPONSE_LOCATION +
                                                                         "/LocationTwoDimensionalGeographicCoordinate";
    public static final String GEO_COORDINATE_LATITUDE = LOCATION_TWO_DIMENSIONAL_GEO_COORDINATE +
                                                         "/GeographicCoordinateLatitude";
    public static final String GEO_COORDINATE_LONGITUDE = LOCATION_TWO_DIMENSIONAL_GEO_COORDINATE +
                                                          "/GeographicCoordinateLongitude";

    public static final String LOCATION = PAYLOAD + "/Location";
    public static final String LOCATION_ADDRESS = LOCATION + "/LocationAddress";
    public static final String ADDRESS_FULLTEXT = LOCATION_ADDRESS + "/AddressFullText";

    public static final String EXCHANGE_META_DATA = DETAILED_CFS_INFO + "/ExchangeMetadata";
    public static final String SUBMITTER_META_DATA = EXCHANGE_META_DATA + "/DataSubmitterMetadata";
    public static final String ORG_IDENTIFICATION = SUBMITTER_META_DATA +
                                                    "/OrganizationIdentification";
    public static final String ORG_IDENTIFICATION_ID = ORG_IDENTIFICATION + "/IdentificationID";

    private Document xmlDocument;
    private XPath xPath;

    public void init(String message) throws DetailedCFSMessageXMLException {

        try {

            xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(message.getBytes("UTF-8")));
        } catch (SAXException e) {
            log.error("Caught exception SAXException message=" + e.getMessage());
            throw new DetailedCFSMessageXMLException(e.getMessage());
        } catch (IOException e) {
            log.error("Caught exception IOException message=" + e.getMessage());
            throw new DetailedCFSMessageXMLException(e.getMessage());
        } catch (ParserConfigurationException e) {
            log.error("Caught exception ParserConfigurationException message=" + e.getMessage());
            throw new DetailedCFSMessageXMLException(e.getMessage());
        }
        xPath = XPathFactory.newInstance().newXPath();

    }

    public String read(String expression) throws DetailedCFSMessageXMLException,
        DetailedCFSMessageException {

        // System.out.println("====> readStringValue - expr[" + expression + "]");

        XPathExpression xPathExpression;
        try {
            xPathExpression = xPath.compile(expression);
        } catch (XPathExpressionException e) {
            throw new DetailedCFSMessageXMLException(e.getMessage());
        }
        Object obj;
        try {
            obj = xPathExpression.evaluate(xmlDocument, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            log.error("Caught exception XPathExpressionException message=" + e.getMessage());
            throw new DetailedCFSMessageXMLException(e.getMessage());
        }

        if (obj.toString().isEmpty()) {
            log.error("====> expression " + expression + " generates empty String");
            throw new DetailedCFSMessageException(expression);
        }

        // System.out.println("====> readStringValue - expr[" + expression + "]  objSt :["
        // + obj.toString() + "]");

        return obj.toString().trim();
    }
}