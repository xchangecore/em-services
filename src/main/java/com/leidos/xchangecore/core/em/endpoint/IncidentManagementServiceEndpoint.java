package com.leidos.xchangecore.core.em.endpoint;

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.incidentManagementService.ArchiveIncidentRequestDocument;
import org.uicds.incidentManagementService.ArchiveIncidentResponseDocument;
import org.uicds.incidentManagementService.CloseIncidentRequestDocument;
import org.uicds.incidentManagementService.CloseIncidentResponseDocument;
import org.uicds.incidentManagementService.CreateIncidentFromCapRequestDocument;
import org.uicds.incidentManagementService.CreateIncidentFromCapResponseDocument;
import org.uicds.incidentManagementService.CreateIncidentRequestDocument;
import org.uicds.incidentManagementService.CreateIncidentResponseDocument;
import org.uicds.incidentManagementService.GetIncidentCurrentVersionRequestDocument.GetIncidentCurrentVersionRequest;
import org.uicds.incidentManagementService.GetIncidentCurrentVersionResponseDocument;
import org.uicds.incidentManagementService.GetIncidentListRequestDocument;
import org.uicds.incidentManagementService.GetIncidentListResponseDocument;
import org.uicds.incidentManagementService.GetIncidentListResponseDocument.GetIncidentListResponse;
import org.uicds.incidentManagementService.GetIncidentRequestDocument;
import org.uicds.incidentManagementService.GetIncidentResponseDocument;
import org.uicds.incidentManagementService.GetListOfClosedIncidentRequestDocument;
import org.uicds.incidentManagementService.GetListOfClosedIncidentResponseDocument;
import org.uicds.incidentManagementService.ShareIncidentRequestDocument;
import org.uicds.incidentManagementService.ShareIncidentResponseDocument;
import org.uicds.incidentManagementService.ShareIncidentResponseDocument.ShareIncidentResponse;
import org.uicds.incidentManagementService.UpdateIncidentRequestDocument;
import org.uicds.incidentManagementService.UpdateIncidentResponseDocument;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import com.leidos.xchangecore.core.em.service.IncidentManagementService;
import com.leidos.xchangecore.core.infrastructure.dao.UserInterestGroupDAO;
import com.leidos.xchangecore.core.infrastructure.exceptions.InvalidProductIDException;
import com.leidos.xchangecore.core.infrastructure.exceptions.PermissionDeniedException;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.ConfigurationService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.ServletUtil;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The Incident Management Service allows clients to manage XchangeCore incidents. It
 * includes services to:
 * <ul>
 * <li>create an incident from a UICDSIncidentType</li>
 * <li>create an incident from a CAP version 1.1 alert document</li>
 * <li>get the incident document (UICDSIncidentType)</li>
 * <li>update information about an incident</li>
 * <li>share the incident with other XchangeCore cores</li>
 * <li>close an incident</li>
 * <li>archive an incident</li>
 * </ul>
 * <p>
 * An incident is defined as the following data structure:<br/>
 * <img src="doc-files/UICDSIncidentType.png"/>
 * <p>
 * <!-- NEWPAGE -->
 * <p>
 * The UICDSIncidentType is derived from the NIEM IncidentType and extended with
 * elements to represent XchangeCore specific items which are essentially read-only
 * for clients. The following elements are the only elements in the NIEM
 * IncidentType that XchangeCore needs to have values for. All other elements in this
 * structure can be used in accordance with NIEM as needed by the clients.
 * <ul>
 * <li>ActivityCategoryText</li>
 * <li>ActivityDateRepresentation</li>
 * <li>ActivityName</li>
 * <li>IncidentLocation</li>
 * <li>IncidentJurisdictionOrganization
 * </ul>
 * To make an incident's location available to other clients one of the
 * following IncidentLocation types is required:
 * <ul>
 * <li>AreaPolygonGeographicCoordinate</li>
 * <li>AreaCircularRegion</li>
 * </ul>
 * with the latitude and longitude values expressed in WGS84 coordinate
 * reference system. NIEM defines the latitude and longitude values as:
 * <p>
 * <b>LatitudeCoordinateType Definition:</b> A measurement of the angular
 * distance between a point on the Earth and the Equator.
 * <p>
 * <b>LatitudeCoordinateType Usage:</b> Information Values range from -90
 * degrees (inclusive) at the South Pole to +90 degrees (inclusive) at the North
 * Pole. The value is 0 at the Equator.
 * <p>
 * <p>
 * <b>LongitudeCoordinateType Definition:</b> A measurement of the angular
 * distance between a point on the Earth and the Prime Meridian.
 * <p>
 * <b>LongitudeCoordinateType Usage:</b> Values range from -180 degrees
 * (inclusive) at the International Date Line to +180 (exclusive) just west of
 * the International Date Line. The value is 0 at the Prime Meridian.
 * <p>
 * The value of the ActivityCategoryText be consistent with the local incident
 * typing conventions or should be one of the standard Event What types that are
 * defined in the UCore 2.0 specification at the <a
 * href="https://uicds.gov">UCore site</a> or in the <a
 * href="../../ucore/2.0/ucore/2.0/codespace/UCoreTaxonomy.owl/">UCore
 * Taxonomy</a>. The following is the current list of UCore Event types:
 * <p>
 * <ul>
 * <li>Event</li>
 * <li>AlertEvent</li>
 * <li>CriminalEvent</li>
 * <li>CommunicationEvent</li>
 * <li>CyberSpaceEvent</li>
 * <li>DisasterEvent</li>
 * <li>EconomicEvent</li>
 * <li>EmergencyEvent</li>
 * <li>EnvironmentalEvent</li>
 * <li>EvacuationEvent</li>
 * <li>ExerciseEvent</li>
 * <li>FinancialEvent</li>
 * <li>HazardousEvent</li>
 * <li>HumanitarianAssistanceEvent</li>
 * <li>InfrastructureEvent</li>
 * <li>LawEnforcementEvent</li>
 * <li>MigrationEvent</li>
 * <li>MilitaryEvent</li>
 * <li>NaturalEvent</li>
 * <li>ObservationEvent</li>
 * <li>PlannedEvent</li>
 * <li>PoliticalEvent</li>
 * <li>PublicHealthEvent</li>
 * <li>SecurityEvent</li>
 * <li>SocialEvent</li>
 * <li>TerroristEvent</li>
 * <li>TransportationEvent</li>
 * <li>WeatherEvent</li>
 * </ul>
 * If more specific types are required for the incident the IncidentEvent array
 * can contain an element with organization, jurisdiction, or application
 * identification along with more typing information. The following example XML
 * shows that the Fire Department Report Manager application has typed the
 * incident as FIRE/BUILDING and the local incident identifier is F01103070100.
 * 
 * <pre>
 *  &lt;IncidentEvent&gt;
 *   &lt;ActivityIdentification&gt;
 *     &lt;IdentificationID&gt;F01103070100&lt;/IdentificationID&gt;
 *     &lt;IdentificationCategoryDescriptionText&gt;http://fire.dept.us/ReportManager#incident&lt;/IdentificationCategoryDescriptionText&gt;
 *     &lt;IdentificationJurisdictionText&gt;Fire Department&lt;/IdentificationJurisdictionText&gt;
 *   &lt;/ActivityIdentification&gt;
 *   &lt;ActivityCategoryText&gt;FIRE/BUILDING&lt;/ActivityCategoryText&gt;
 *   &lt;ActivityReasonText&gt;INCIDENT_TYPE&lt;/ActivityReasonText&gt;
 *  &lt;/IncidentEvent&gt;
 * </pre>
 * 
 * The IncidentEvent array can also be used to simply designate that an
 * application has received the incident and report its local incident
 * identifier as in the following example. In this example the combination of
 * ActivityCategoryText and ActivityReasonText indicates that this event
 * captures the fact that Fire Report Manager application received this
 * incident. The data in the ActivityIdentification element indicates that the
 * Fire Report Manager server at http://fire.dept.us/ReportManager received the
 * incident and created an entry with the local identifier F01103070100 in the
 * incident database or collection.
 * 
 * <pre>
 * &lt;IncidentEvent&gt; 
 *   &lt;ActivityIdentification&gt; 
 *     &lt;IdentificationID&gt;F01103070100&lt;/IdentificationID&gt;
 *     &lt;IdentificationCategoryDescriptionText&gt;http://fire.dept.us/ReportManager#incident&lt;/IdentificationCategoryDescriptionText&gt;
 *     &lt;IdentificationJurisdictionText&gt;Fire Department&lt;/IdentificationJurisdictionText&gt;
 *   &lt;/ActivityIdentification&gt;
 *   &lt;ActivityCategoryText&gt;Fire Report Manager&lt;/ActivityCategoryText&gt;
 *   &lt;ActivityReasonText&gt;RECEIVED&lt;/ActivityReasonText&gt;
 * &lt;/IncidentEvent&gt;
 * </pre>
 * 
 * <p>
 * For the IncidentJurisdictionOrganization it is recommended that the following
 * elements contain values:
 * <ul>
 * <li>OrganizationIdentification</li>
 * <li>OrganziationLocation (using same types as IncidentLocation)</li>
 * </ul>
 * <p>
 * The Incident Management Service creates a UCore digest for each incident when
 * it is created and updates the digest each time the incident is updated. The
 * main components of the digest are shown in the following diagram: <img
 * src="doc-files/IncidentDigest.png"/>
 * <p>
 * <!-- NEWPAGE -->
 * <p>
 * The UCore.Event element is populated from data in the main part of the
 * IncidentType. The UCore.Location will contain a GeoLocation element for each
 * LocationArea in the IncidentLocation and a PhysicalAddress for each
 * LocationAddress. The following table shows a mapping of data elements. Full
 * details of the mapping can be found in the IncidentDigest.xsl XSL
 * Transformation file.
 * <p>
 * <b>IncidentType to UCore Digest Mapping:</b>
 * <p>
 * <table>
 * <tr>
 * <th>IncidentType Element</th>
 * <th>Digest Element</th>
 * </tr>
 * <tr>
 * <td>IncidentType.ActivityDescriptionText</td>
 * <td>Event.Descriptor</td>
 * </tr>
 * <tr>
 * <td>IncidentType.ActivityName</td>
 * <td>Event.Identifier</td>
 * </tr>
 * <tr>
 * <td>IncidentType.ActivityCategoryText</td>
 * <td>Event.What (default "Event")</td>
 * </tr>
 * <tr>
 * <td>Current date/time</td>
 * <td>OccursAt.TimeInstant</td>
 * </tr>
 * <tr>
 * <td>IncidentType..AreaPolygonGeographicCoordinate</td>
 * <td>Location.GeoLocation.Polygon LinearyRing</td>
 * </tr>
 * <tr>
 * <td>IncidentType..AreaCircularRegion</td>
 * <td>Location.GeoLocation.Polygon CircleByCenterPoint</td>
 * </tr>
 * <tr>
 * <td>IncidentType..StructuredAddress.LocationAddress.StreetNumberText +
 * StreetName</td>
 * <td>Location.PhysicalAddress.postalAddress.street</td>
 * </tr>
 * <tr>
 * <td>IncidentType..StructuredAddress.LocationAddress.LocationCityName</td>
 * <td>Location.PhysicalAddress.postalAddress.city</td>
 * </tr>
 * <tr>
 * <td>IncidentType..StructuredAddress.LocationAddress.
 * LocationStateUSPostalServiceCode</td>
 * <td>Location.PhysicalAddress.postalAddress.state</td>
 * </tr>
 * <tr>
 * <td>IncidentType..StructuredAddress.LocationAddress.LocationPostalCode</td>
 * <td>Location.PhysicalAddress.postalAddress.postalCode</td>
 * </tr>
 * <tr>
 * <td>IncidentType..StructuredAddress.LocationAddress.
 * LocationCountryISO3166Alpha2Code</td>
 * <td>Location.PhysicalAddress.postalAddress.countryCode</td>
 * </tr>
 * <tr>
 * <td>IncidentType.IncidentJurisdictionalOrganization.OrganizationName</td>
 * <td>Organization.Name</td>
 * </tr>
 * </table>
 * <p>
 * When incidents are created from CAP messages values are copied from the CAP
 * message to instantiate a new UICDSIncidentType. The following table shows
 * which CAP elements are copied to the new UICDSIncidentType structure. The CAP
 * message used to create the incident is not saved as a work product when
 * creating an incident.
 * <p>
 * <b>CAP to Incident Element Mapping:</b>
 * <p>
 * <table>
 * <tr>
 * <th>CAP Element</th>
 * <th>Incident Element</th>
 * </tr>
 * <tr>
 * <td>alert.event (if no event then alert.identifier)</td>
 * <td>ActivityName</td>
 * </tr>
 * <tr>
 * <td>alert.info:category</td>
 * <td>ActivityCategoryText</td>
 * </tr>
 * <tr>
 * <td>alert.info:description</td>
 * <td>ActivityDescriptionText</td>
 * </tr>
 * <tr>
 * <td>alert.sent</td>
 * <td>ActivityDateRepresentation</td>
 * </tr>
 * <tr>
 * <td>alert.info.area.addresses</td>
 * <td>IncidentLocation.LocationAddress.AddressFullText</td>
 * </tr>
 * <tr>
 * <td>alert.info.area:polygon</td>
 * <td>IncidentLocation.LocationArea:AreaPolygonGeographicCoordinate (array of)</td>
 * </tr>
 * <tr>
 * <td>alert.info.area.circle</td>
 * <td>IncidentLocation.LocationArea.AreaCircleRegion (array of)</td>
 * </tr>
 * </table>
 * 
 * <p>
 * The Incident Management Service manages XchangeCore work products of type
 * "Incident". <BR>
 * 
 * <p>
 * 
 * @author Daphne Hurrell
 * @author Daniel Huang
 * @since 1.0
 * @see <a href="../../wsdl/IncidentManagementService.wsdl">Appendix:
 *      IncidentManagementService.wsdl</a>
 * @see <a href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
 *      Incident.xsd</a>
 * @see <a
 *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
 *      IncidentManagementService.xsd</a>
 * @see <a
 *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
 *      IncidentManagementServiceData.xsd</a>
 * 
 * @idd
 * 
 */
@Endpoint
public class IncidentManagementServiceEndpoint
    implements ServiceNamespaces {

    private static Logger logger = LoggerFactory.getLogger(IncidentManagementServiceEndpoint.class);

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    IncidentManagementService incidentManagementService;
    @Autowired
    UserInterestGroupDAO userInterestGroupDAO;
    @Autowired
    WorkProductService workProductService;

    /**
     * Archives an incident by removing it and all of the associated work
     * products. The incident must be closed before it is archived.
     * 
     * @param ArchiveIncidentRequestDocument
     * 
     * @return ArchiveIncidentResponseDocument
     * @throws PermissionDeniedException 
     * @throws InvalidProductIDException 
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "ArchiveIncidentRequest")
    public ArchiveIncidentResponseDocument archiveIncident(ArchiveIncidentRequestDocument request)
        throws DatatypeConfigurationException, InvalidProductIDException, PermissionDeniedException {

        String igID = request.getArchiveIncidentRequest().getIncidentID();

        //		  2014/05/28 - TTHURSTON - removed since only the incident owner or an admin can close
        //        if (userInterestGroupDAO.isEligible(ServletUtil.getPrincipalName(), igID) == false) {
        //        	logger.debug("user: " + ServletUtil.getPrincipalName() + " is not eligible." );
        //        	throw new PermissionDeniedException();
        //        }

        // 2014/04/14 - TTHURSTON
        // not the owner!

        //2014/05/28 - TTHURSTON
        // also validate not admin because new management console now uses these services also
        if (!workProductService.isAuthenticatedUserTheOwner(ServletUtil.getPrincipalName(), igID) &&
            (!workProductService.isAuthenticatedUserAnAdmin(ServletUtil.getPrincipalName()))) {
            logger.debug("user: " + ServletUtil.getPrincipalName() +
                         " is not the owner nor an admin.");
            throw new PermissionDeniedException();
        }
        // END CHANGES

        ArchiveIncidentResponseDocument response = ArchiveIncidentResponseDocument.Factory.newInstance();
        response.addNewArchiveIncidentResponse().set(WorkProductHelper.toWorkProductProcessingStatus(incidentManagementService.archiveIncident(igID)));
        return response;
    }

    /**
     * Closes an incident by making all of the associated work products
     * inactive.
     * 
     * @param CloseIncidentRequestDocument
     * 
     * @return CloseIncidentResponseDocument
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * 
     * @throws DatatypeConfigurationException
     * @throws InvalidProductIDException 
     * @throws PermissionDeniedException 
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "CloseIncidentRequest")
    public CloseIncidentResponseDocument closeIncident(CloseIncidentRequestDocument request)
        throws DatatypeConfigurationException, InvalidProductIDException, PermissionDeniedException {

        String igID = request.getCloseIncidentRequest().getIncidentID();

        // 		  2014/05/28 - TTHURSTON - removed since only the incident owner or an admin can close
        //        if (userInterestGroupDAO.isEligible(ServletUtil.getPrincipalName(), igID) == false) {
        //        	logger.debug("user: " + ServletUtil.getPrincipalName() + " is not eligible." );
        //        	throw new PermissionDeniedException();
        //
        //        }

        // 2014/04/14 - TTHURSTON
        // not the owner!

        //2014/05/28 - TTHURSTON
        // also validate not admin - new management console now uses these services also
        if (!workProductService.isAuthenticatedUserTheOwner(ServletUtil.getPrincipalName(), igID) &&
            (!workProductService.isAuthenticatedUserAnAdmin(ServletUtil.getPrincipalName()))) {
            logger.debug("user: " + ServletUtil.getPrincipalName() +
                         " is not the owner nor an admin.");
            throw new PermissionDeniedException();
        }
        // END CHANGES

        CloseIncidentResponseDocument response = CloseIncidentResponseDocument.Factory.newInstance();
        response.addNewCloseIncidentResponse().set(WorkProductHelper.toWorkProductProcessingStatus(incidentManagementService.closeIncident(igID)));
        return response;
    }

    /**
     * Allows the client to create an incident using the UICDSIncidentType as an
     * input type.
     * 
     * @param CreateIncidentRequestDocument
     * 
     * @return CreateIncidentResponseDocument
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "CreateIncidentRequest")
    public CreateIncidentResponseDocument createIncident(CreateIncidentRequestDocument request)
        throws DatatypeConfigurationException {

        CreateIncidentResponseDocument response = CreateIncidentResponseDocument.Factory.newInstance();
        response.addNewCreateIncidentResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(incidentManagementService.createIncident(request.getCreateIncidentRequest().getIncident())));
        // log.debug("CreateIncidentResponse: [ " + response.toString() + " ]");
        return response;
    }

    /**
     * Allows the client to create an incident from a CAP version 1.1 element.
     * 
     * @param CreateIncidentFromCapRequestDocument
     * 
     * @return CreateIncidentFromCapResponseDocument
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "CreateIncidentFromCapRequest")
    public CreateIncidentFromCapResponseDocument createIncidentFromCap(CreateIncidentFromCapRequestDocument request)
        throws DatatypeConfigurationException {

        ProductPublicationStatus status = incidentManagementService.createIncidentFromCap(request.getCreateIncidentFromCapRequest().getAlert());

        CreateIncidentFromCapResponseDocument response = CreateIncidentFromCapResponseDocument.Factory.newInstance();
        response.addNewCreateIncidentFromCapResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));

        logger.debug("CreateIncidentFromCap: [ " + response.toString() + " ]");
        return response;

    }

    private ArrayList<WorkProduct> getEligibleListOfIncident(List<WorkProduct> incidentWPs) {

        ArrayList<WorkProduct> incidentList = new ArrayList<WorkProduct>();

        for (WorkProduct wp : incidentWPs) {
            String igID = wp.getFirstAssociatedInterestGroupID();
            if (userInterestGroupDAO.isEligible(ServletUtil.getPrincipalName(), igID))
                incidentList.add(wp);
        }
        return incidentList;
    }

    /**
     * Allows the client to retrieve the incident work product by incident
     * WorkProductIdentfication. The request must designate a specific version
     * of the work product when using this method.
     * 
     * @param GetIncidentRequestDocument
     * 
     * @return GetIncidentResponseDocument
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * @throws InvalidProductIDException
     *             The work product for the given work product identifier does
     *             not exist.
     * @throws SOAPException 
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "GetIncidentRequest")
    public GetIncidentResponseDocument getIncident(GetIncidentRequestDocument request)
        throws InvalidProductIDException, DatatypeConfigurationException, SOAPException {

        WorkProduct wp = getWorkProduct(request.getGetIncidentRequest().getWorkProductIdentification());

        GetIncidentResponseDocument response = GetIncidentResponseDocument.Factory.newInstance();
        response.addNewGetIncidentResponse().setWorkProduct(WorkProductHelper.toWorkProduct(wp));

        return response;
    }

    /**
     * Allows the client to retrieve the incident work product by incident work
     * product ID. The request can specify just the Identification element from
     * the WorkProductIdentification element and teh latest version of the
     * matching Incident work product will be returned.
     * 
     * @param GetIncidentCurrentVersionRequestDocument
     * 
     * @return GetIncidentCurrentVersionResponseDocument
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * @throws InvalidProductIDException
     *             The work product for the given work product identifier does
     *             not exist.
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "GetIncidentCurrentVersionRequest")
    public GetIncidentCurrentVersionResponseDocument getIncidentCurrentVersion(GetIncidentCurrentVersionRequest request)
        throws InvalidProductIDException, DatatypeConfigurationException {

        GetIncidentCurrentVersionResponseDocument response = GetIncidentCurrentVersionResponseDocument.Factory.newInstance();
        WorkProduct workProduct = workProductService.getProduct(request.getIdentifier().getStringValue());
        if (workProduct != null)
            response.addNewGetIncidentCurrentVersionResponse().setWorkProduct(WorkProductHelper.toWorkProduct(workProduct));
        else
            throw new InvalidProductIDException();
        return response;
    }

    /**
     * Get a list of all the incidents on the core. The returned list contains
     * the Work Product Identification, Work Product Properties, and Digest of
     * each incident.
     * 
     * @param GetIncidentListRequestDocument
     * 
     * @return GetIncidentListResponseDocument
     * 
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * 
     * @idd
     */

    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "GetIncidentListRequest")
    public GetIncidentListResponseDocument getIncidentList(GetIncidentListRequestDocument request)
        throws DatatypeConfigurationException {

        GetIncidentListResponseDocument response = GetIncidentListResponseDocument.Factory.newInstance();
        String username = ServletUtil.getPrincipalName();
        logger.debug("getIncidentList for user: " + username);
        ArrayList<WorkProduct> incidentWPs = incidentManagementService.getIncidentList();
        ArrayList<WorkProduct> incidentList = getEligibleListOfIncident(incidentWPs);
        GetIncidentListResponse rsp = response.addNewGetIncidentListResponse();
        WorkProductList productList = populateSummary(incidentList);

        rsp.setWorkProductList(productList);

        return response;
    }

    /**
     * Get a list of incidents that have been closed with the CloseIncident
     * operation.
     * 
     * @param GetListOfClosedIncidentRequestDocument
     * 
     * @return GetListOfClosedIncidentResponseDocument
     * 
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "GetListOfClosedIncidentRequest")
    public GetListOfClosedIncidentResponseDocument getListOfClosedIncident(GetListOfClosedIncidentRequestDocument request)
        throws DatatypeConfigurationException {

        GetListOfClosedIncidentResponseDocument response = GetListOfClosedIncidentResponseDocument.Factory.newInstance();
        String[] incidentIDs = incidentManagementService.getListOfClosedIncident();
        if (incidentIDs.length > 0)
            for (String incidentID : incidentIDs)
                if (userInterestGroupDAO.isEligible(ServletUtil.getPrincipalName(), incidentID))
                    response.addNewGetListOfClosedIncidentResponse().addNewIdentifier().setStringValue(incidentID);
        return response;
    }

    private WorkProduct getWorkProduct(IdentificationType identification)
        throws InvalidProductIDException, PermissionDeniedException {

        WorkProduct wp = workProductService.getProduct(identification);
        if (wp == null)
            throw new InvalidProductIDException();

        if (userInterestGroupDAO.isEligible(ServletUtil.getPrincipalName(),
            wp.getFirstAssociatedInterestGroupID()) == false)
            throw new PermissionDeniedException();

        return wp;
    }

    private WorkProductList populateSummary(ArrayList<WorkProduct> products) {

        WorkProductList productList = WorkProductList.Factory.newInstance();

        for (WorkProduct product : products)
            if (product != null)
                productList.addNewWorkProduct().set(WorkProductHelper.toWorkProductSummary(product));

        return productList;
    }

    void setIncidentManagementService(IncidentManagementService ims) {

        incidentManagementService = ims;
    }

    void setUserInterestGroupDAO(UserInterestGroupDAO userInterestGroupDAO) {

        this.userInterestGroupDAO = userInterestGroupDAO;
    }

    void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }

    /**
     * Allows the client to share an with another core. The agreements are
     * checked before the share is allowed to take place.
     * 
     * @param ShareIncidentRequestDocument
     * 
     * @return ShareIncidentResponseDocument
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "ShareIncidentRequest")
    public ShareIncidentResponseDocument shareIncident(ShareIncidentRequestDocument request)
        throws DatatypeConfigurationException {

        ShareIncidentResponseDocument response = ShareIncidentResponseDocument.Factory.newInstance();
        ShareIncidentResponse rsp = response.addNewShareIncidentResponse();
        boolean shared;
        try {
            shared = incidentManagementService.shareIncident(request.getShareIncidentRequest());
            rsp.setIncidentShareSucessful(shared);
        } catch (Exception e) {

            rsp.setIncidentShareSucessful(false);
            rsp.setErrorString(e.getMessage());
        }

        return response;
    }

    /**
     * Allows the client to update the incident work product.
     * 
     * @param UpdateIncidentRequestDocument
     * 
     * @return UpdateIncidentResponseDocument
     * @throws SOAPException 
     * @throws InvalidProductIDException 
     * @see <a
     *      href="../../services/IncidentManagement/0.1/Incident.xsd">Appendix:
     *      Incident.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementService.xsd">Appendix:
     *      IncidentManagementService.xsd</a>
     * @see <a
     *      href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_IncidentManagementService, localPart = "UpdateIncidentRequest")
    public UpdateIncidentResponseDocument updateIncident(UpdateIncidentRequestDocument request)
        throws DatatypeConfigurationException, SOAPException, InvalidProductIDException {

        WorkProduct wp = getWorkProduct(request.getUpdateIncidentRequest().getWorkProductIdentification());

        UpdateIncidentResponseDocument response = UpdateIncidentResponseDocument.Factory.newInstance();
        ProductPublicationStatus status = incidentManagementService.updateIncident(request.getUpdateIncidentRequest().getIncident(),
            request.getUpdateIncidentRequest().getWorkProductIdentification());
        response.addNewUpdateIncidentResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));

        return response;
    }
}
