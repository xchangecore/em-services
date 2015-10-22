package com.leidos.xchangecore.core.em.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.sensorService.CreateSOIRequestDocument;
import org.uicds.sensorService.CreateSOIResponseDocument;
import org.uicds.sensorService.GetSOIListRequestDocument;
import org.uicds.sensorService.GetSOIListRequestDocument.GetSOIListRequest;
import org.uicds.sensorService.GetSOIListResponseDocument;
import org.uicds.sensorService.GetSOIRequestDocument;
import org.uicds.sensorService.GetSOIRequestDocument.GetSOIRequest;
import org.uicds.sensorService.GetSOIResponseDocument;
import org.uicds.sensorService.GetSOIResponseDocument.GetSOIResponse;
import org.uicds.sensorService.SensorObservationInfoDocument.SensorObservationInfo;
import org.uicds.sensorService.UpdateSOIRequestDocument;
import org.uicds.sensorService.UpdateSOIResponseDocument;

import com.leidos.xchangecore.core.em.service.SensorService;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * 
 * The XchangeCore Sensor Service allows clients to manage Open Geospatial Consortium
 * Sensor Observation Specification (OGC-SOS) GetObservation and Observation
 * work products. Observations or measurements made by these sensors are used to
 * support incident related activities. The discovery process to identify
 * sensors that are relevant to a particular XchangeCore incident involves
 * interactions between XchangeCore clients and the sensor systems directly at several
 * SOS levels. To reduce the need to repeat these steps, the information
 * required to retrieve sensor observations, and optionally an observation
 * itself, is stored as XchangeCore work products that are associated with the
 * incident. XchangeCore clients who are interested in retrieving the sensor
 * observations request these work products either from the Sensor Service or
 * the Work Product Service. The GetObservation element included in the work
 * product can be used to request observations directly from the sensor system
 * via the SOS interface in a more real-time manner.
 * <p>
 * The XchangeCore Sensor Service provides the ability for XchangeCore users to:
 * <ul>
 * <li>create an Sensor Observation Information (SOI) work product
 * <li>delete an SOI work product
 * <li>update an SOI work product
 * <li>get an SOI work product with a specified work product ID
 * <li>get a list of SOI work products associated with a specified incident
 * </ul>
 * <p>
 * The request to create an SOI work product is shown in the following diagram:
 * <img src="doc-files/CreateSOIRequest.png"/>
 * <p>
 * The create allows the client to create an SOI work product and optionally
 * associate it with an incident.
 * <p>
 * The SensorObservationInfo type is shown in the following data structure:<br/>
 * <img src="doc-files/SensorObservationInfo.png"/>
 * <p>
 * The sosURN should be the URN of the SOS interface for the sensor. The
 * SensorInfo contains descriptive data about the sensor or sensor suite. The
 * location in this element should be in WGS84 decimal degrees and indicate
 * where the sensor should be displayed on a map. The any element is used to
 * contain the actual SOS GetObservation and Observation elements.
 * <p>
 * Note that each SOI work product may contain any number of SensorInfo elements
 * and any number of GetObservation and Observation elements in the any element
 * for one or more external sensors. Best practice for creating SOI work
 * products is to include a SensorInfo and GetObservation element for each SOS
 * offering by the SOS that is to be associated to the incident. The
 * SensorObservationInfo.SensorInfo.name and GetObservation.offering values
 * should be used as the common value to match elements. If an Observation
 * element is included for a sensor its gml.name element should match the
 * SensorObservationInfo.SensorInfo.name and GetObservation.offering values.
 * <p>
 * The Sensor Service creates Digests for each SOI work products. Full details
 * of the mapping from the SOI work product to the Digests fields can be found
 * in the SOIDigest.xsl XSL Transformation file. The following table shows the
 * default mapping.
 * <p>
 * <b>SOI to UCore Digest Mapping:</b>
 * <p>
 * <table>
 * <tr>
 * <th>IncidentType Element</th>
 * <th>Digest Element</th>
 * </tr>
 * <tr>
 * <td>SensorObservationInfo.SensorInfo.id</td>
 * <td>Event.Descriptor</td>
 * </tr>
 * <tr>
 * <td>SensorObservationInfo.SensorInfo.name</td>
 * <td>Event.Identifier</td>
 * </tr>
 * <tr>
 * <td>IncidentType.ActivityCategoryText</td>
 * <td>Event.What (default "Event")</td>
 * </tr>
 * <tr>
 * <td>SensorObservationInfo.sosURN</td>
 * <td>Location.CyberAddress.virtualCoverage@address</td>
 * </tr>
 * <tr>
 * <td>SensorObservationInfo.SensorInfo.latitude
 * SensorObservationInfo.SensorInfo.longitude</td>
 * <td>Location.GeoLocation.Point.Point.pos</td>
 * </tr>
 * </table>
 * <p>
 * The Sensor Service manages XchangeCore work products of type "SOI". <BR>
 * <p>
 * 
 * @author Aruna Hau
 * @since 1.0
 * @see <a href="../../wsdl/SensorService.wsdl">Appendix: SensorService.wsdl</a>
 * @see <a href="../../services/Sensor/0.1/SensorService.xsd">Appendix:
 *      SensorService.xsd</a>
 * @see <a href="http://www.opengeospatial.org/standards/sos">OGC Sensor
 *      Observation Service</a>
 * @idd
 * 
 */
@Endpoint
public class SensorServiceEndpoint
    implements ServiceNamespaces {

    @Autowired
    SensorService sensorService;

    @Autowired
    WorkProductService workProductService;

    @Autowired
    DirectoryService directoryService;

    Logger log = LoggerFactory.getLogger(SensorServiceEndpoint.class);

    /**
     * Creates an Sensor Observation Info (SOI) work product and optionally
     * associate it to an incident.
     * 
     * @param CreateSOIRequestDocument
     * 
     * @return CreateSOIResponseDocument
     * @see <a href="../../services/Sensor/0.1/SensorService.xsd">Appendix:
     *      SensorService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_SensorService, localPart = "CreateSOIRequest")
    public CreateSOIResponseDocument CreateSOI(CreateSOIRequestDocument requestDoc) {

        CreateSOIResponseDocument responseDoc = CreateSOIResponseDocument.Factory.newInstance();
        SensorObservationInfo soi = requestDoc.getCreateSOIRequest().getSensorObservationInfo();
        String incidentID = requestDoc.getCreateSOIRequest().getIncidentID();
        System.out.println("CreateSOIResponseDocument: soi=[" + soi.toString() + "] incidentID=" +
                           incidentID);
        ProductPublicationStatus status = sensorService.createSOI(soi, incidentID);

        responseDoc.addNewCreateSOIResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));
        return responseDoc;
    }

    /**
     * Updates an Sensor Observation Info (SOI) work product.
     * 
     * @param UpdateSOIRequestDocument
     * 
     * @return UpdateSOIResponseDocument
     * @see <a href="../../services/Sensor/0.1/SensorService.xsd">Appendix:
     *      SensorService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_SensorService, localPart = "UpdateSOIRequest")
    public UpdateSOIResponseDocument UpdateSOI(UpdateSOIRequestDocument requestDoc) {

        UpdateSOIResponseDocument responseDoc = UpdateSOIResponseDocument.Factory.newInstance();
        ProductPublicationStatus status = sensorService.updateSOI(requestDoc.getUpdateSOIRequest().getSensorObservationInfo(),
            requestDoc.getUpdateSOIRequest().getWorkProductIdentification());
        responseDoc.addNewUpdateSOIResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));
        return responseDoc;
    }

    /**
     * Deletes an Sensor Observation Info (SOI) work product.
     * 
     * @param DeleteSOIRequestDocument
     * 
     * @see <a href="../../services/Sensor/0.1/SensorService.xsd">Appendix:
     *      SensorService.xsd</a>
     * @see DeleteSOIResponseDocument
     * @idd
     * @PayloadRoot(namespace = NS_SensorService, localPart =
     *                        "DeleteSOIRequest") public
     *                        DeleteSOIResponseDocument
     *                        DeleteSOI(DeleteSOIRequestDocument requestDoc) {
     *                        DeleteSOIResponseDocument responseDoc =
     *                        DeleteSOIResponseDocument.Factory.newInstance();
     *                        String productId = requestDoc
     *                        .getDeleteSOIRequest(
     *                        ).getWorkProductIdentification().getIdentifier
     *                        ().getStringValue(); ProductPublicationStatus
     *                        status = sensorService.deleteSOI(productId);
     *                        responseDoc.addNewDeleteSOIResponse
     *                        ().addNewWorkProductPublicationResponse().set(
     *                        WorkProductHelper
     *                        .toWorkProductPublicationResponse(status)); return
     *                        responseDoc; }
     */

    /**
     * Returns an Sensor Observation Info (SOI) work product for the input work
     * product identification.
     * 
     * @param GetSOIRequestDocument
     * 
     * @return GetSOIResponseDocument
     * @see <a href="../../services/Sensor/0.1/SensorService.xsd">Appendix:
     *      SensorService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_SensorService, localPart = "GetSOIRequest")
    public GetSOIResponseDocument GetSOI(GetSOIRequestDocument requestDoc) {

        GetSOIRequest request = requestDoc.getGetSOIRequest();
        IdentificationType workProductIdentification = request.getWorkProductIdentification();

        GetSOIResponseDocument responseDoc = GetSOIResponseDocument.Factory.newInstance();
        GetSOIResponse response = responseDoc.addNewGetSOIResponse();
        response.setWorkProduct(WorkProductHelper.toWorkProduct(sensorService.getSOI(workProductIdentification.getIdentifier().getStringValue())));
        return responseDoc;
    }

    /**
     * Returns a list of Sensor Observation Info (SOI) work products associated
     * with a given incident.
     * 
     * @param GetSOIListRequestDocument
     * 
     * @return GetSOIListResponseDocument
     * @see <a href="../../services/Sensor/0.1/SensorService.xsd">Appendix:
     *      SensorService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_SensorService, localPart = "GetSOIListRequest")
    public GetSOIListResponseDocument GetSOIList(GetSOIListRequestDocument requestDoc) {

        GetSOIListRequest getSOIListRequest = requestDoc.getGetSOIListRequest();

        WorkProduct[] products = sensorService.getSOIList(getSOIListRequest.getIncidentID());
        com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct[] workProductSummaryArray = null;
        if (products != null && products.length > 0) {
            workProductSummaryArray = new com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct[products.length];
            int i = 0;
            for (WorkProduct product : products) {
                workProductSummaryArray[i] = WorkProductHelper.toWorkProductSummary(product);
                i++;
            }
        }

        GetSOIListResponseDocument responseDoc = GetSOIListResponseDocument.Factory.newInstance();
        responseDoc.addNewGetSOIListResponse().addNewWorkProductList().setWorkProductArray(workProductSummaryArray);

        return responseDoc;
    }
}
