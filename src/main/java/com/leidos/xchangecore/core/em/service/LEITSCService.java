package com.leidos.xchangecore.core.em.service;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.xmlbeans.XmlException;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageException;
import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageXMLException;
import com.leidos.xchangecore.core.em.exceptions.LEITSCIncidentPublicationException;
import com.leidos.xchangecore.core.infrastructure.exceptions.UICDSException;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;

/**
 * The XchangeCore LEITSC Service provides the ability for a CAD system to post Detailed CFS messages to
 * the XchangeCore core, when an incident is created, updated, and cleared.
 * 
 * This service is an adaptor for LEITSC messages to the IncidentManagementService interface.
 * 
 * @author Aruna Hau
 * @since 1.0
 * @ssdd
 * 
 */
@Transactional
public interface LEITSCService {

    /**
     * Posts a Detailed CFS message to the XchangeCore core. A XchangeCore incident will be created, updated, or
     * closed/archived, based on the message's activity status.
     * 
     * @param XmlObject
     * @return boolean - status of the request
     * @throws DetailedCFSMessageException
     * @throws DetailedCFSMessageXMLException
     * @throws LEITSCIncidentPublicationException
     * @throws XmlException
     * @throws XmlException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws XmlException
     * @throws UICDSException
     * @throws XmlException
     * 
     *             @ return leitscIncidentID
     * @ssdd
     */
    public String postDetailedCFSMessage(String message) throws DetailedCFSMessageException,
        DetailedCFSMessageXMLException, LEITSCIncidentPublicationException, XmlException;

    /**
     * Gets the current version of the XchangeCore incident work product published for a LEITSC incident.
     * 
     * @param LEITSC incident ID
     * 
     * @return incident work product
     * @throws LEITSCIncidentPublicationException
     * @see WorkProduct
     * @ssdd
     */
    public WorkProduct getLEITSCIncident(String leitscIncidentID);

}
