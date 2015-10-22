package com.leidos.xchangecore.core.em.endpoint;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.leitscService.GetLEITSCIncidentRequestDocument;
import org.uicds.leitscService.GetLEITSCIncidentResponseDocument;
import org.uicds.leitscService.PostDetailedCFSMessageRequestDocument;
import org.uicds.leitscService.PostDetailedCFSMessageResponseDocument;
import org.uicds.leitscService.GetLEITSCIncidentResponseDocument.GetLEITSCIncidentResponse;
import org.uicds.leitscService.PostDetailedCFSMessageResponseDocument.PostDetailedCFSMessageResponse;

import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageException;
import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageXMLException;
import com.leidos.xchangecore.core.em.exceptions.LEITSCIncidentPublicationException;
import com.leidos.xchangecore.core.em.service.LEITSCService;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;

/**
 * The XchangeCore LEITSC Service allows clients to create, update, close, and archive
 * XchangeCore incidents based on LEITSC Detailed Call For Service messages.
 * <p>
 * The actions to apply are based on the Detailed Call For Service 
 * Payload/ServiceCall/ActivityStatus value.
 * The following table shows how this value is interpreted by XchangeCore:
 * <table>
 * <thead><tr><td width="30%"><b>Value</b></td><td width="70%"><b>XchangeCore Action</b></td></thead>
 * <tbody>
 * <tr><td width="30%">CREATED</td><td width="70%">Create a new incident</td></tr>
 * <tr><td width="30%">CLEARED</td><td width="70%">Close and archive the incident</td></tr>
 * <tr><td width="30%">Not CREATED or CLEARED</td><td width="70%">Update the incident</td></tr>
 * </tbody>
 * </table>
 * <p>
 * The following table shows the elements from the LEITSC Detailed Call For Service
 * message that are used to populate the UICDSIncidentType document.
 * <table>
 * <thead><tr><td><b>LEITSC Element</b></td><td><b>UICDSIncidentType Element</b></td></thead>
 * <tbody>
 * <tr><td>Payload/ServiceCall/ActitivityDescriptionText</td><td">ActivityDescriptionText</td></tr>
 * <tr><td>Payload/ServiceCall/ServiceCallAugmentation/CallTypeText</td><td>ActivityCategoryText</td></tr>
 * <tr><td>Payload/ServiceCall/ServiceCallAugmentation/ServiceCallAririvedDate</td><td>ActivityDate</td></tr>
 * <tr><td>Payload/Location/LocationAddress</td><td>IncidentLocation/LocationAddress</td></tr>
 * <tr><td>Playload/ServiceCallResponseLocation/LocationTwoDimensionalGeographicCoordinates</td><td>IncidentLocation/LocationArea/AreaCircularRegion</td></tr>
 * <tr><td>ExchangeMetadata/DataSubmitterMetadata/OrganizationIdentification</td><td>IncidentJurisdictionalOrganization</td></tr>
 * <tr><td>ServiceCall/ActivityIdentification</td><td>IncidentEvent/ActivityIdentification</td></tr>
 * <tr><td>ServiceCall/ActivityStatus</td><td>IncidentEvent/ActivityStatus</td></tr>
 * <tr><td>Payload/ServiceCall/ServiceCallAugmentation/ServiceCallAririvedDate</td><td>IncidentEvent/ActivityDate</td></tr>
 * <tr><td>ExchangeMetadata/DataSubmitterMetadata/OrganizationIdentification</td><td>IncidentEvent/ActivityDescriptionText</td></tr>
 * </tbody>
 * </table>
 * <p>
 * @author Aruna Hau
 * @see <a href="../../wsdl/LEITSCService.wsdl">Appendix: LEITSCService.wsdl</a>
 * @see <a href="../../services/LEITSC/0.1/LEITSCService.xsd">Appendix:
 *      LEITSCService.xsd</a>
 * @see <a href="http://www.leitsc.org/IEPDs.htm">LEITSC IEPD</a>
 * 
 * @idd
 */

@Endpoint
public class LEITSCServiceEndpoint
    implements ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(LEITSCServiceEndpoint.class);

    @Autowired
    private LEITSCService leitscService;

    private static final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /**
     * Posts a Detailed CFS message to the XchangeCore core. A XchangeCore incident will be
     * created, updated, or closed/archived, based on the message's activity
     * status.
     * 
     * @param PostDetailedCFSMessageRequestDocument
     * 
     * @return PostDetailedCFSMessageResponseDocument
     * @throws XmlException
     * @throws DetailedCFSMessageException
     * @throws DetailedCFSMessageXMLException
     * @see <a href="../../services/LEITSC/0.1/LEITSCService.xsd">Appendix: LEITSCService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_LEITSCService, localPart = "PostDetailedCFSMessageRequest")
    public PostDetailedCFSMessageResponseDocument postDetailedCFSMessage(PostDetailedCFSMessageRequestDocument requestDoc)
        throws XmlException, DetailedCFSMessageException, DetailedCFSMessageXMLException,
        LEITSCIncidentPublicationException {

        log.debug("postDCFSMessage() called - data:");
        log.debug(requestDoc.toString());

        PostDetailedCFSMessageResponseDocument responseDoc = PostDetailedCFSMessageResponseDocument.Factory.newInstance();
        PostDetailedCFSMessageResponse response = responseDoc.addNewPostDetailedCFSMessageResponse();

        response.setLeitscIncidentID(leitscService.postDetailedCFSMessage(requestDoc.toString()));

        return responseDoc;

    }

    /**
     * Gets the LEITSC incident.
     * 
     * @param requestDoc the request doc
     * 
     * @return the LEITSC incident document
     */
    @PayloadRoot(namespace = NS_LEITSCService, localPart = "GetLEITSCIncidentRequest")
    public GetLEITSCIncidentResponseDocument getLEITSCIncident(GetLEITSCIncidentRequestDocument requestDoc) {

        // log.debug("getUICDSIncient() called - data:");
        // log.debug(requestDoc.toString());

        GetLEITSCIncidentResponseDocument responseDoc = GetLEITSCIncidentResponseDocument.Factory.newInstance();
        GetLEITSCIncidentResponse response = responseDoc.addNewGetLEITSCIncidentResponse();

        WorkProduct product = leitscService.getLEITSCIncident(requestDoc.getGetLEITSCIncidentRequest().getLeitscIncidentID());
        if (product != null) {
            response.setWorkProduct(product);
        }
        return responseDoc;

    }

    void setLEITSCService(LEITSCService leitscSvc) {

        leitscService = leitscSvc;
    }
}
