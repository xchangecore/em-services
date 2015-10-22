package com.leidos.xchangecore.core.em.service.impl;

import gov.niem.niem.niemCore.x20.AddressFullTextDocument;
import gov.niem.niem.niemCore.x20.AreaType;
import gov.niem.niem.niemCore.x20.CircularRegionType;
import gov.niem.niem.niemCore.x20.LocationType;
import gov.niem.niem.niemCore.x20.OrganizationType;
import gov.niem.niem.niemCore.x20.TextType;
import gov.ucore.ucore.x20.DigestDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.IncidentInfoDocument;
import org.uicds.incidentManagementService.IncidentInfoType;
import org.uicds.incidentManagementService.IncidentListType;
import org.uicds.incidentManagementService.ShareIncidentRequestDocument.ShareIncidentRequest;

import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert.Info;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert.Info.Area;

import com.leidos.xchangecore.core.em.dao.IncidentDAO;
import com.leidos.xchangecore.core.em.messages.IncidentStateNotificationMessage;
import com.leidos.xchangecore.core.em.model.Incident;
import com.leidos.xchangecore.core.em.service.IncidentManagementService;
import com.leidos.xchangecore.core.em.util.AgreementMatcher;
import com.leidos.xchangecore.core.em.util.DigestGenerator;
import com.leidos.xchangecore.core.em.util.EMGeoUtil;
import com.leidos.xchangecore.core.em.util.IncidentUtil;
import com.leidos.xchangecore.core.infrastructure.dao.AgreementDAO;
import com.leidos.xchangecore.core.infrastructure.dao.InterestGroupDAO;
import com.leidos.xchangecore.core.infrastructure.dao.UserInterestGroupDAO;
import com.leidos.xchangecore.core.infrastructure.exceptions.InvalidInterestGroupIDException;
import com.leidos.xchangecore.core.infrastructure.exceptions.UICDSException;
import com.leidos.xchangecore.core.infrastructure.messages.DeleteJoinedInterestGroupMessage;
import com.leidos.xchangecore.core.infrastructure.messages.InterestGroupStateNotificationMessage;
import com.leidos.xchangecore.core.infrastructure.messages.JoinedInterestGroupNotificationMessage;
import com.leidos.xchangecore.core.infrastructure.messages.ProductChangeNotificationMessage;
import com.leidos.xchangecore.core.infrastructure.model.Agreement;
import com.leidos.xchangecore.core.infrastructure.model.ExtendedMetadata;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.ConfigurationService;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.InterestGroupManagementComponent;
import com.leidos.xchangecore.core.infrastructure.service.PubSubNotificationService;
import com.leidos.xchangecore.core.infrastructure.service.PubSubService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.InterestGroupInfo;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.LdapUtil;
import com.leidos.xchangecore.core.infrastructure.util.LogEntry;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.ServletUtil;
import com.leidos.xchangecore.core.infrastructure.util.UUIDUtil;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.IdentificationType;
import com.vividsolutions.jts.geom.Point;

/**
 * The IncidentManagementService implementation.
 *
 * @author Daphne Hurrell
 * @author Daniel Huang
 * @since 1.0
 * @see com.leidos.xchangecore.core.em.model.Incident Incident Data Model
 * @see com.leidos.xchangecore.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class IncidentManagementServiceImpl
    implements IncidentManagementService, PubSubNotificationService, ServiceNamespaces {

    Logger logger = LoggerFactory.getLogger(IncidentManagementServiceImpl.class);

    private LdapUtil ldapUtil;
    private AgreementDAO agreementDAO;
    private UserInterestGroupDAO userInterestGroupDAO;
    private InterestGroupDAO interestGroupDAO;
    private IncidentDAO incidentDAO;
    private WorkProductService workProductService;
    private DirectoryService directoryService;
    private PubSubService pubSubService;
    private ConfigurationService configurationService;
    private InterestGroupManagementComponent interestGroupManagementComponent;

    // TODO: temporary - this channel shall be removed once the messages
    // sent on it have been replaced with simple method invocations to other domain services.
    private MessageChannel incidentStateNotificationChannel;

    // remotely-owned work products pending update
    // key: productID
    // value: incident
    private final Map<String, String> pendingRemoteUpdateRequests = new HashMap<String, String>();

    private String xsltFilePath;

    private String iconConfigXmlFilePath;

    private javax.xml.transform.Source xsltSource;

    private ClassPathResource xsltResource;

    private javax.xml.transform.TransformerFactory transformerFactory;

    private javax.xml.transform.Transformer transformer;

    private DigestGenerator digestGenerator;

    private UICDSIncidentType alertToIncident(Alert alert) {

        // System.out.println("alertToIncident: alert=[" + alert.toString() + "]");
        final UICDSIncidentType theIncident = UICDSIncidentType.Factory.newInstance();

        // set activity name
        String incidentName = "No incident description.";
        if (alert.sizeOfInfoArray() > 0) {
            incidentName = alert.getInfoArray()[0].getEvent();
        } else {
            incidentName = alert.getIdentifier();
        }

        final TextType[] activityNameArray = new TextType[1];
        activityNameArray[0] = TextType.Factory.newInstance();
        activityNameArray[0].setStringValue(incidentName);
        theIncident.setActivityNameArray(activityNameArray);

        final TextType[] activityCategoryText = new TextType[1];
        activityCategoryText[0] = TextType.Factory.newInstance();

        final TextType[] activityDescriptionText = new TextType[1];
        activityDescriptionText[0] = TextType.Factory.newInstance();

        // process the FIRST alert.info
        final int sizeOfInfo = alert.sizeOfInfoArray();
        if (sizeOfInfo > 0) {
            final Info[] infos = alert.getInfoArray();
            // set activity type
            activityCategoryText[0].setStringValue(infos[0].getCategoryArray(0).toString());

            activityDescriptionText[0].setStringValue(infos[0].getDescription());

            if (infos[0].sizeOfAreaArray() > 0) {
                // set the area data into IncidentType.IncidentLocation.LocationArea
                final Area[] areas = infos[0].getAreaArray();
                for (final Area area : areas) {

                    final LocationType incidentLocation = theIncident.addNewIncidentLocation();
                    String[] stringValues = area.getPolygonArray();
                    // if there are polygon points then save them into
                    // IncidentLocation.LocationArea.AreaPolygonGeographicCoordinate
                    if (stringValues != null) {
                        for (final String polygonString : stringValues) {
                            // assign the polygon into
                            // IncidentType.IncidentLocation.LocationArea.Polygon
                            final AreaType polygonArea = EMGeoUtil.getPoloygon(polygonString);
                            incidentLocation.addNewLocationArea().set(polygonArea);
                        }
                    }
                    // if there are circle then save them into
                    // IncidentLocation.LocationArea.AreaCircleRegion
                    stringValues = area.getCircleArray();
                    if ((stringValues != null) && (stringValues.length > 0)) {
                        for (final String circleString : stringValues) {
                            // each point is a circle with center coordinate and radius
                            final CircularRegionType theCircle = EMGeoUtil.getCircle(circleString);
                            if ((theCircle != null) &&
                                (theCircle.getCircularRegionCenterCoordinateArray().length > 0)) {
                                incidentLocation.addNewLocationArea().addNewAreaCircularRegion().set(
                                    theCircle);
                            }
                        }
                    }
                }
            }
        } else {
            activityCategoryText[0].setStringValue(Alert.Info.Category.OTHER.toString());
        }

        theIncident.setActivityCategoryTextArray(activityCategoryText);

        theIncident.setActivityDescriptionTextArray(activityDescriptionText);

        // set the IncidentType.IncidentLocation.LocationAddress
        LocationType[] incidentLocationArray;
        if (theIncident.sizeOfIncidentLocationArray() == 0) {
            incidentLocationArray = new LocationType[1];
            incidentLocationArray[0] = LocationType.Factory.newInstance();
        } else {
            incidentLocationArray = theIncident.getIncidentLocationArray();
        }
        final AddressFullTextDocument ad = AddressFullTextDocument.Factory.newInstance();
        ad.addNewAddressFullText().setStringValue(alert.getAddresses());
        incidentLocationArray[0].addNewLocationAddress().set(ad);
        theIncident.setIncidentLocationArray(incidentLocationArray);

        // TRAC #272
        final XmlCursor cursor = theIncident.addNewActivityDateRepresentation().newCursor();
        cursor.setName(new QName("http://niem.gov/niem/niem-core/2.0", "ActivityDate"));
        cursor.setTextValue(alert.getSent().toString());
        cursor.dispose();

        final OrganizationType[] incidentJurisdictionalOrganizationArray = new OrganizationType[1];
        incidentJurisdictionalOrganizationArray[0] = OrganizationType.Factory.newInstance();

        final TextType[] organizationNameArray = new TextType[1];
        organizationNameArray[0] = TextType.Factory.newInstance();
        organizationNameArray[0].setStringValue("Organization Name");

        incidentJurisdictionalOrganizationArray[0].setOrganizationNameArray(organizationNameArray);
        theIncident.setIncidentJurisdictionalOrganizationArray(incidentJurisdictionalOrganizationArray);

        return theIncident;
    }

    /**
     * Archive incident. find the work products and deleted them. delete the interest group. find
     * the incident model and deleted it.
     *
     * @param incidentID
     *            the incident id
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus archiveIncident(String incidentID) {

        ProductPublicationStatus status = validateIncident(incidentID);
        if (status != null) {
            return status;
        }

        status = new ProductPublicationStatus();
        status.setStatus(ProductPublicationStatus.SuccessStatus);

        // find the work products, mark them to be deleted
        final WorkProduct[] products = getWorkProductService().getAssociatedWorkProductList(
            incidentID);
        for (final WorkProduct product : products) {
            if (product.isActive() == true) {
                return new ProductPublicationStatus(incidentID + " contains " +
                                                    product.getProductID() +
                                                    " which needs to be closed first");
            }
            logger.debug("delete " + product.getProductID() + ", Ver.: " +
                         product.getProductVersion());
            getWorkProductService().deleteWorkProduct(product.getProductID());
        }

        // we will delete the interest group now.
        try {
            logger.debug("ask InterestGroupManagementComponent to delete InterestGroup: " +
                         incidentID);
            getInterestGroupManagementComponent().deleteInterestGroup(incidentID);
            // TODO the UserInterestGroupDAO and IncidentDAO shall be linked
            getUserInterestGroupDAO().removeInterestGroup(incidentID);
        } catch (final InvalidInterestGroupIDException e) {
            e.printStackTrace();
            return new ProductPublicationStatus("delete interest group: " + incidentID + ": " +
                                                e.getMessage());
        }

        // find the incident model and mark it to be deleted
        logger.debug("DELETE " + incidentID + " ...");
        try {
            incidentDAO.delete(incidentID, true);
            logger.debug("DELETED " + incidentID + " ...");
        } catch (final HibernateException e) {
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(e.getMessage());
            logger.error("archiveIncident: HibernateException deleting incidentDAO: " +
                         e.getMessage() + " from " + e.toString());
        } catch (final Exception e) {
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(e.getMessage());
            logger.error("archiveIncident: Exception deleting incidentDAO: " + e.getMessage() +
                " from " + e.toString());
        }

        // make a performance log entry
        final LogEntry logEntry = new LogEntry();
        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
        logEntry.setAction(LogEntry.ACTION_INCIDENT_ARCHIVE);
        logEntry.setIncidentId(incidentID);
        logEntry.setUpdatedBy(ServletUtil.getPrincipalName());
        logger.info(logEntry.getLogEntry());

        return status;
    }

    /**
     * Close incident. Find the work products, mark them to be deleted. Find the incident model and
     * mark it to be deleted.
     *
     * @param incidentID
     *            the incident id
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus closeIncident(String incidentID) {

        ProductPublicationStatus status = validateIncident(incidentID);
        if (status != null) {
            return status;
        }

        status = new ProductPublicationStatus();
        status.setStatus(ProductPublicationStatus.SuccessStatus);

        // find the work products, mark them to be deleted
        final WorkProduct[] products = getWorkProductService().getAssociatedWorkProductList(
            incidentID);
        for (final WorkProduct product : products) {
            if ((product != null) && product.isActive()) {
                logger.debug("mark " + product.getProductID() + ", Ver.: " +
                             product.getProductVersion() + " as Deleted/InActive");
                getWorkProductService().closeProduct(
                    WorkProductHelper.getWorkProductIdentification(product));
            }
        }

        // find the incident model and mark it to be deleted
        logger.debug("mark " + incidentID + " as Deleted/InActive");
        try {
            incidentDAO.delete(incidentID, false);
        } catch (final HibernateException e) {
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(e.getMessage());
            logger.error("closeIncident: HibernateException deleting incidentDAO: " +
                         e.getMessage() + " from " + e.toString());
        } catch (final Exception e) {
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(e.getMessage());
            logger.error("closeIncident: Exception deleting incidentDAO: " + e.getMessage() +
                " from " + e.toString());
        }

        // make a performance log entry
        final LogEntry logEntry = new LogEntry();
        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
        logEntry.setAction(LogEntry.ACTION_INCIDENT_CLOSE);
        logEntry.setIncidentId(incidentID);
        logEntry.setUpdatedBy(ServletUtil.getPrincipalName());
        logger.info(logEntry.getLogEntry());

        return status;
    }

    /**
     * Creates the incident. Create interest group for the incident. Set the digest for the work
     * product. Persist the incident.
     *
     * @param incident
     *            the incident
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createIncident(UICDSIncidentType incident) {

        // set the incident ID first
        final String owningCore = getConfigurationService().getCoreName();

        // create interest group for the incident
        final InterestGroupInfo igInfo = new InterestGroupInfo();
        igInfo.setInterestGroupID(null);
        igInfo.setInterestGroupType(IncidentManagementService.InterestGroupType);
        igInfo.setName(getIncidentName(incident));
        igInfo.setDescription(getIncidentDescription(incident));
        igInfo.setOwningCore(owningCore);
        igInfo.setInterestGroupSubType(getIncidentActivityCategory(incident));

        // set the extended metadata elements for the interest group
        // iterate through the entries and build a hashset
        if (incident.sizeOfExtendedMetadataArray() > 0) {
            final HashSet set = new HashSet();
            final ExtendedMetadata data = new ExtendedMetadata();
            for (int i = 0; i < incident.getExtendedMetadataArray().length; i++) {

                data.setCodespace(incident.getExtendedMetadataArray(i).getCodespace());
                data.setCode(incident.getExtendedMetadataArray(i).getCode());
                data.setLabel(incident.getExtendedMetadataArray(i).getLabel());
                data.setValue(incident.getExtendedMetadataArray(i).getStringValue());

                set.add(data);
            }
            igInfo.setExtendedMetadata(set);

        }

        final String interestGroupID = interestGroupManagementComponent.createInterestGroup(igInfo);

        setIncidentID(incident, interestGroupID);

        // we will setup these administrative information, not user
        incident.setOwningCore(owningCore);
        incident.setSharedCoreNameArray(null);

        final IncidentDocument incidentDoc = IncidentDocument.Factory.newInstance();
        incidentDoc.addNewIncident().set(incident);

        // this is the time to generate userInterestGroup entries
        generateUserInterestGroupList(incidentDoc);

        final WorkProduct newWp = newWorkProduct(incidentDoc, null);

        // Create the digest if we have an XSLT .
        // test env doesn't set this via Spring:
        if (xsltFilePath == null) {
            xsltFilePath = "xslt/IncidentDigest.xsl";
        }
        if (iconConfigXmlFilePath == null) {
            iconConfigXmlFilePath = "xml/types_icons.xml";
        }
        digestGenerator = new DigestGenerator(xsltFilePath, iconConfigXmlFilePath);
        final DigestDocument digestDoc = digestGenerator.createDigest(incidentDoc);
        // log.info("digestDoc="+digestDoc);
        newWp.setDigest(digestDoc);

        // set the digest for the work product
        // newWp.setDigest(new EMDigestHelper(incidentDoc.getIncident()).getDigest());
        // log.info("EMDigestHelper Digest="+new
        // EMDigestHelper(incidentDoc.getIncident()).getDigest().toString());

        final ProductPublicationStatus status = getWorkProductService().publishProduct(newWp);

        WorkProduct wp = null;
        if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
            wp = status.getProduct();
            // set the incident work product ID to the incidentDoc
            // setIncidentWPID(incidentDoc.getIncident(), wp.getProductID());

            // persist the Incident model again to persist the work product ID

            persistIncident(incidentDoc.getIncident(), wp.getProductID());

            // send the IncidentStateNotificationMessage.State.NEW message
            sendIncidentStateChangeMessages(InterestGroupStateNotificationMessage.State.NEW,
                getIncidentDAO().findByIncidentID(getIncidentID(incidentDoc.getIncident())), igInfo);
        }

        // make a performance log entry
        final LogEntry logEntry = new LogEntry();
        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
        logEntry.setAction(LogEntry.ACTION_INCIDENT_CREATE);
        logEntry.setCoreName(owningCore);
        logEntry.setIncidentId(interestGroupID);
        logEntry.setCreatedBy(ServletUtil.getPrincipalName());
        if (incident.sizeOfActivityCategoryTextArray() > 0) {
            logEntry.setIncidentType(incident.getActivityCategoryTextArray(0).getStringValue());
        } else {
            logEntry.setIncidentType(wp.getProductType());
        }
        logger.info(logEntry.getLogEntry());

        return status;
    }

    /**
     * Creates the incident from cap. Convert the CAP alert to a UICDSIncidentType and then create
     * the incident.
     *
     * @param alert
     *            the alert
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createIncidentFromCap(Alert alert) {

        if (alert == null) {
            return null;
        }

        return createIncident(alertToIncident(alert));
    }

    /**
     * Delete incident.
     *
     * @param incidentID
     *            the incident id
     * @return true, if successful
     */
    @Override
    public boolean deleteIncident(String incidentID) {

        // TODO
        logger.debug("delete incident: " + incidentID + " ...");
        return false;
    }

    @Override
    public void deleteJoinedInterestGroupHandler(DeleteJoinedInterestGroupMessage message) {

        // the interest group has been deleted, what do i do next ???
        final String incidentID = message.getInterestGroupID();

        // find the work products, mark them to be deleted
        final WorkProduct[] products = getWorkProductService().getAssociatedWorkProductList(
            incidentID);
        for (final WorkProduct product : products) {
            logger.debug("deleteJoinedInterestGroupHandler: purge product: " +
                         product.getProductID());
            getWorkProductService().purgeWorkProduct(product.getProductID());
        }

        // find the incident model and mark it to be deleted
        logger.debug("Joined Core: delete incident: " + incidentID + " ...");
        try {
            incidentDAO.delete(incidentID, true);
        } catch (final HibernateException e) {
            logger.error("deleteJoinedInterestGroupHandler: HibernateException deleting incidentDAO: " +
                         e.getMessage() + " from " + e.toString());
        } catch (final Exception e) {
            logger.error("deleteJoinedInterestGroupHandler: Exception deleting incidentDAO: " +
                         e.getMessage() + " from " + e.toString());
        }
    }

    private boolean doShareIncident(ShareIncidentRequest shareIncidentRequest,
                                    boolean agreementChecked) throws UICDSException {

        final String IGID = shareIncidentRequest.getIncidentID();
        final String remoteCoreJID = shareIncidentRequest.getCoreName();
        logger.debug("doShareIncident: IGID: " + IGID + ", remoteCoreJID: " + remoteCoreJID);

        final IncidentInfoType incidentInfo = getIncidentInfo(IGID);
        final IncidentInfoDocument incidentInfoDoc = IncidentInfoDocument.Factory.newInstance();
        incidentInfoDoc.setIncidentInfo(incidentInfo);

        logger.debug("doShareIncident: incidentDoc's string=[" + incidentInfoDoc.toString() + "]");
        try {
            interestGroupManagementComponent.shareInterestGroup(IGID, remoteCoreJID,
                incidentInfoDoc.toString(), agreementChecked);

            // TODO: RDW - The shareInterestGroup call sends an asycn message to the joining core
            // We can't wait synchronously for this response so we should put the request in a
            // queue and return. handleJoinRequestResponse will be called when we get a
            // response from the core.

            handleJoinRequestResponse(incidentInfo, remoteCoreJID);
        } catch (final UICDSException e) {
            logger.error("Error sharing incident: " + e.getMessage());
            throw e;
        }

        return true;
    }

    private void generateUserInterestGroupList(IncidentDocument incidentDoc) {

        final String IGID = getIncidentID(incidentDoc.getIncident());
        logger.debug("generateUserInterestGroupList: IGID: " + IGID);
        final String localCoreJID = directoryService.getLocalCoreJid();
        logger.debug("generateUserInterestGroupList: local core JID: " + localCoreJID);
        final String creator = ServletUtil.getPrincipalName();
        logger.debug("generateUserInterestGroupList: add creator: " + creator + " for IGID: " +
            IGID);
        getUserInterestGroupDAO().addUser(creator, IGID);

        // Add xchangecore-admins memebers to whitelist regardless
        final List<String> adminMembers = getLdapUtil().getGroupMembersForAdmins();
        for (final String adminMember : adminMembers) {
            logger.debug("gernerateUserInterestGroupList: adding " + adminMember +
                " from admin groups for IGID: " + IGID);
            getUserInterestGroupDAO().addUser(adminMember, IGID);
        }

        final List<Agreement> agreementList = getAgreementDAO().findAll();
        for (final Agreement agreement : agreementList) {
            logger.debug("gernerateUserInterestGroupList: Matching Agreement ID: " +
                         agreement.getId());
            if (!agreement.isIntraCoreAgreement()) {
                logger.debug("generateUserInterestGroupList: inter-core: localCoreJID: " +
                    agreement.getLocalCorename() + " with remoteCoreJID: " +
                    agreement.getRemoteCorename() + " ... skip ...");

                continue;
            }

            boolean doShare = false;

            logger.debug("generateUserInterestGroupList: matched rules");

            // if the from part is empty which means no ID nor group specified
            if (agreement.getLocalJIDs().isEmpty() && agreement.getLocalGroups().isEmpty()) {
                logger.debug("generateUserInterestGroupList: share because from part is all.");
                doShare = true;
            }

            // if the list of ID contains the creator, then share
            if (!doShare && !agreement.getLocalJIDs().isEmpty() &&
                agreement.getLocalJIDs().contains(creator)) {
                logger.debug("generateUserInterestGroupList: share because fromJIDs contains " +
                    creator);
                doShare = true;
            }

            // if any member in the list of the groups contains the creator, then share
            if (!doShare && !agreement.getLocalGroups().isEmpty()) {
                final Set<String> groups = agreement.getLocalGroups();
                for (final String group : groups) {
                    if (getLdapUtil().groupContainsMember(group, creator)) {
                        logger.debug("generateUserInterestGroupList: share because fromGroups: " +
                            group + " contains " + creator);
                        doShare = true;
                        break;
                    }
                }
            }

            if (doShare) {

                if (agreement.getRemoteJIDs().isEmpty() && agreement.getRemoteGroups().isEmpty()) {
                    logger.debug("generateUserInterestGroupList: to part is all");
                    if (AgreementMatcher.isRuleMatchedIgnoreProximity(null,
                        agreement.getShareRules(), incidentDoc, true)) {

                        final List<String> members = getLdapUtil().listOfUsers();
                        for (final String member : members) {
                            logger.debug("generateUserInterestGroupList: adding " + member +
                                " as one of the member on the core for IGID: " + IGID);
                            getUserInterestGroupDAO().addUser(member, IGID);
                        }
                    }
                    continue;
                }

                if (agreement.getRemoteJIDs().isEmpty() == false) {

                    if (AgreementMatcher.isRuleMatchedIgnoreProximity(null,
                        agreement.getShareRules(), incidentDoc, true)) {

                        final Set<String> users = agreement.getRemoteJIDs();
                        for (final String user : users) {
                            logger.debug("generateUserInterestGroupList: adding " + user +
                                " as one of the remoteJIDs for IGID: " + IGID);
                            getUserInterestGroupDAO().addUser(user, IGID);
                        }
                    }
                }

                if (agreement.getRemoteGroups().isEmpty() == false) {
                    final Set<String> groups = agreement.getRemoteGroups();
                    for (final String group : groups) {
                        // check all the rules
                        String[] points = new String[2];
                        points = getLdapUtil().getCNLocation(group);
                        logger.debug("Checking Agreement Matcher for group: " + group);
                        if (AgreementMatcher.isRuleMatched(points, agreement.getShareRules(),
                            incidentDoc)) {
                            final List<String> members = getLdapUtil().getGroupMembers(group);
                            for (final String member : members) {
                                logger.debug("generateUserInterestGroupList: adding " + member +
                                    " as one of the remoteGroups for IGID: " + IGID);
                                getUserInterestGroupDAO().addUser(member, IGID);
                            }
                        }
                    }
                }
            }
        }
    }

    public AgreementDAO getAgreementDAO() {

        return agreementDAO;
    }

    /** {@inheritDoc} */
    public ConfigurationService getConfigurationService() {

        return configurationService;
    }

    /** {@inheritDoc} */
    public DirectoryService getDirectoryService() {

        return directoryService;
    }

    // private ArrayList<IncidentStateObserver> observers = new ArrayList<IncidentStateObserver>();

    /**
     * @return the getIconConfigXmlFilePath
     */
    public String getIconConfigXmlFilePath() {

        return iconConfigXmlFilePath;
    }

    /**
     * Gets the incident using the workProduct Id String.
     *
     * @param incidentWPID
     *            the incident wpid
     * @return the incident
     * @ssdd
     */
    @Override
    public WorkProduct getIncident(String incidentWPID) {

        return incidentWPID != null ? getWorkProductService().getProduct(incidentWPID) : null;
    }

    private String getIncidentActivityCategory(UICDSIncidentType incident) {

        String incidentActivityCategory = null;
        if (incident.sizeOfActivityCategoryTextArray() > 0) {
            incidentActivityCategory = incident.getActivityCategoryTextArray(0).getStringValue();
        }
        return incidentActivityCategory;
    }

    /** {@inheritDoc} */
    public IncidentDAO getIncidentDAO() {

        return incidentDAO;
    }

    private String getIncidentDateRepresentation(UICDSIncidentType incident) {

        String dateRepresentation = null;
        if (incident.sizeOfActivityDateRepresentationArray() > 0) {
            dateRepresentation = incident.getActivityDateRepresentationArray(0).toString();
        }
        return dateRepresentation;
    }

    private String getIncidentDescription(UICDSIncidentType incident) {

        String incidentDescription = null;
        if (incident.sizeOfActivityDescriptionTextArray() > 0) {
            incidentDescription = incident.getActivityDescriptionTextArray(0).getStringValue();
        }
        return incidentDescription;
    }

    private String getIncidentID(UICDSIncidentType incident) {

        String incidentID = null;
        if (incident.sizeOfActivityIdentificationArray() > 0) {
            if (incident.getActivityIdentificationArray(0).sizeOfIdentificationIDArray() > 0) {
                incidentID = incident.getActivityIdentificationArray(0).getIdentificationIDArray(0).getStringValue();
            }
        }
        return incidentID;
    }

    /**
     * Gets the incident description info from the incident identifier.
     *
     * @param incidentID
     *            the incident id
     * @return the incident info
     * @ssdd
     */
    @Override
    public IncidentInfoType getIncidentInfo(String incidentID) {

        final InterestGroupInfo igInfo = interestGroupManagementComponent.getInterestGroup(incidentID);
        final Incident incident = incidentDAO.findByIncidentID(incidentID);
        if (incident != null) {
            return toIncidentInfoType(incident, igInfo);
        }
        return null;
    }

    /**
     * Gets the list of incident work products.
     *
     * @return the list of incident work products
     * @ssdd
     */
    @Override
    public ArrayList<WorkProduct> getIncidentList() {

        final ArrayList<WorkProduct> workProducts = new ArrayList<WorkProduct>();

        final List<Incident> incidents = incidentDAO.findAll();
        if ((incidents != null) && (incidents.size() > 0)) {
            for (final Incident incident : incidents) {
                final WorkProduct wp = workProductService.getProduct(incident.getWorkProductID());
                if (wp != null) {
                    workProducts.add(wp);
                }
            }
        }

        return workProducts;
    }

    private String getIncidentName(UICDSIncidentType incident) {

        String incidentName = null;
        if (incident.sizeOfActivityNameArray() > 0) {
            incidentName = incident.getActivityNameArray(0).getStringValue();
        }
        return incidentName;
    }

    private String getIncidentWPID(UICDSIncidentType incident) {

        return incident.getId();
    }

    public InterestGroupDAO getInterestGroupDAO() {

        return interestGroupDAO;
    }

    public InterestGroupManagementComponent getInterestGroupManagementComponent() {

        return interestGroupManagementComponent;
    }

    public LdapUtil getLdapUtil() {

        return ldapUtil;
    }

    /**
     * Gets the list of closed incident.
     *
     * @return the list of closed incident
     * @ssdd
     */
    @Override
    public String[] getListOfClosedIncident() {

        final List<Incident> closedIncidents = incidentDAO.findAllClosedIncident();
        String[] incidentIDList = null;
        if (closedIncidents.size() > 0) {
            incidentIDList = new String[closedIncidents.size()];
            int i = 0;
            for (final Incident incident : closedIncidents) {
                incidentIDList[i++] = incident.getIncidentId();
            }
        }

        return incidentIDList;
    }

    /**
     * Gets the list of incidents.
     *
     * @return the list of incidents
     * @ssdd
     */
    @Override
    public IncidentListType getListOfIncidents() {

        final IncidentListType response = IncidentListType.Factory.newInstance();
        final List<Incident> incidents = incidentDAO.findAll();
        if ((incidents != null) && (incidents.size() > 0)) {
            final List<IncidentInfoType> infoList = new ArrayList<IncidentInfoType>();
            for (final Incident incident : incidents) {
                final InterestGroupInfo igInfo = interestGroupManagementComponent.getInterestGroup(incident.getIncidentId());
                if (igInfo != null) {
                    final IncidentInfoType incidentInfo = toIncidentInfoType(incident, igInfo);
                    if (incidentInfo != null) {
                        infoList.add(incidentInfo);
                    }
                }
            }
            if (infoList.size() > 0) {
                IncidentInfoType[] infos = new IncidentInfoType[infoList.size()];
                infos = infoList.toArray(infos);
                response.setIncidentInfoArray(infos);
            }
        }

        return response;
    }

    /**
     * Gets the service name.
     *
     * @return the service name
     * @ssdd
     */
    @Override
    public String getServiceName() {

        return IncidentManagementService.IMS_SERVICE_NAME;
    }

    public UserInterestGroupDAO getUserInterestGroupDAO() {

        return userInterestGroupDAO;
    }

    // end of temporary code
    /** {@inheritDoc} */
    public WorkProductService getWorkProductService() {

        return workProductService;
    }

    /**
     * @return the xsltFilePath
     */
    public String getXsltFilePath() {

        return xsltFilePath;
    }

    private void handleJoinRequestResponse(IncidentInfoType incidentInfo, String coreName)
        throws UICDSException {

        final String incidentID = incidentInfo.getId();

        // Notify listeners of a state change to the incident.
        final IncidentStateNotificationMessage mesg = new IncidentStateNotificationMessage();
        mesg.setState(InterestGroupStateNotificationMessage.State.SHARE);
        mesg.setIncidentInfo(incidentInfo);

        try {
            notifyOfIncidentStateChange(mesg);
        } catch (final Exception e) {
            logger.error("Share Incident: " + incidentID + " with " + coreName +
                " incident state listener failure: " + e.getMessage());
            e.printStackTrace();
        }

        // update the incident work product to include the name of the "joined" core
        final Incident theIncident = getIncidentDAO().findByIncidentID(incidentID);
        final WorkProduct wp = getWorkProductService().getProduct(theIncident.getWorkProductID());
        IncidentDocument incidentWPDoc = null;
        try {
            incidentWPDoc = (IncidentDocument) wp.getProduct();
        } catch (final Exception e) {
            e.printStackTrace();
            logger.error("Incident WP: " + wp.getProductID() + " not a valied incident document");
            throw new UICDSException("Internal Error: Incident WP: " + wp.getProductID() +
                                     " not a valied incident document");

        }

        String[] sharedCore = incidentWPDoc.getIncident().getSharedCoreNameArray();

        final HashSet<String> sharedCoreSet = new HashSet<String>(Arrays.asList(sharedCore));

        if (!sharedCoreSet.contains(coreName)) {
            sharedCoreSet.add(coreName);
            sharedCore = new String[sharedCoreSet.size()];
            sharedCore = sharedCoreSet.toArray(sharedCore);
            incidentWPDoc.getIncident().setSharedCoreNameArray(sharedCore);
            wp.setProduct(incidentWPDoc);

            final ProductPublicationStatus status = getWorkProductService().publishProduct(wp);
        }

        // make a performance log entry
        final LogEntry logEntry = new LogEntry();
        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
        logEntry.setAction(LogEntry.ACTION_INCIDENT_SHARE);
        logEntry.setCoreName(incidentInfo.getOwningCore());
        logEntry.setIncidentId(incidentID);
        logEntry.setShareCoreName(coreName);
        logger.info(logEntry.getLogEntry());
    }

    /*
     * Initialize the cache and send UPDATE message to Directory Service for each incidents
     */
    private void init() {

        final List<Incident> incidents = incidentDAO.findAll();
        if (incidents != null) {
            for (final Incident incident : incidents) {
                final InterestGroupInfo igInfo = getInterestGroupManagementComponent().getInterestGroup(
                    incident.getIncidentId());
                if (igInfo.getOwningCore().equalsIgnoreCase(getDirectoryService().getLocalCoreJid())) {
                    sendIncidentStateChangeMessages(
                        InterestGroupStateNotificationMessage.State.RESTORE, incident, igInfo);
                } else {
                    logger.debug("init: IGID: " + igInfo.getInterestGroupID() + " owningCoreJID: " +
                                 igInfo.getOwningCore() + " restore after remote core is online");
                }
            }
        }
    }

    /**
     * Invalid xpath notification.
     *
     * @param subscriptionId
     *            the subscription id
     * @param errorMessage
     *            the error message
     */
    @Override
    public void InvalidXpathNotification(Integer subscriptionId, String errorMessage) {

        logger.error(errorMessage + " for Subscriber: " + subscriptionId);
    }

    @Override
    public void newJoinedInterestGroupHandler(JoinedInterestGroupNotificationMessage message) {

        logger.info("newJoinedInterestGroupHandler: receive new joined incident notification incidentID=" +
                    message.interestGroupID +
                    " incidentType=" +
                    message.interestGroupType +
                    " owner=" + message.owner);

        if (message.interestGroupType.equals(IncidentManagementService.InterestGroupType)) {

            Incident incident = getIncidentDAO().findByIncidentID(message.interestGroupID);

            if (incident == null) {
                try {
                    final IncidentInfoDocument incidentInfoDocument = IncidentInfoDocument.Factory.parse(message.interestGroupInfo);

                    final IncidentInfoType incidentInfo = incidentInfoDocument.getIncidentInfo();

                    incident = new Incident();
                    incident.setIncidentId(incidentInfo.getId());
                    incident.setWorkProductID(incidentInfo.getWorkProductIdentification().getIdentifier().getStringValue());
                    incident.setLatitude(incidentInfo.getLatitude());
                    incident.setLongitude(incidentInfo.getLongitude());
                    try {
                        getIncidentDAO().makePersistent(incident);
                    } catch (final HibernateException e) {
                        logger.error("newJoinedInterestGroupHandler: HibernateException makePersistent incidentDAO: " +
                                     e.getMessage() + " from " + e.toString());
                    } catch (final Exception e) {
                        logger.error("newJoinedInterestGroupHandler: Exception makePersistent incidentDAO: " +
                                     e.getMessage() + " from " + e.toString());
                    }

                    final InterestGroupInfo igInfo = interestGroupManagementComponent.getInterestGroup(incident.getIncidentId());
                    if (igInfo != null) {
                        sendIncidentStateChangeMessages(
                            InterestGroupStateNotificationMessage.State.JOIN, incident, igInfo);

                        // make a performance log entry
                        final LogEntry logEntry = new LogEntry();
                        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
                        logEntry.setAction(LogEntry.ACTION_INCIDENT_JOIN);
                        logEntry.setCoreName(getDirectoryService().getCoreName());
                        logEntry.setIncidentId(incident.getIncidentId());
                        logEntry.setJoinCoreName(message.getOwner());
                        logger.info(logEntry.getLogEntry());

                    } else {
                        logger.error("newJoinedInterestGroupHandler: Failed to retrieve interest group info for  " +
                                     incident.getIncidentId() +
                                     "from interestGroupManagementComponent");
                    }
                } catch (final Throwable e) {
                    logger.error("newJoinedInterestGroupHandler: error parsing received incident info");
                    e.printStackTrace();
                }
            }
        }
    }

    private WorkProduct newWorkProduct(IncidentDocument incidentDoc, String productID) {

        final WorkProduct wp = new WorkProduct();
        final String incidentID = getIncidentID(incidentDoc.getIncident());
        if (incidentID == null) {
            logger.error("Missing incident ID from [" + incidentDoc.toString() + "]");
        }
        wp.associateInterestGroup(incidentID);
        wp.setProductType(Type);

        if (productID == null) {
            productID = UUIDUtil.getID(IncidentManagementService.Type);
            // addWPIDToIncident(incidentDoc.getIncident(), productID);
        }

        incidentDoc.getIncident().setId(productID);
        wp.setProductID(productID);
        // log.debug("newWorkProduct: incidentDoc's wp=[" + incidentDoc.toString() + "]");
        wp.setProduct(incidentDoc);
        return wp;
    }

    /**
     * New work product version. Checks if a subscription was made for a pending update and if so,
     * cancels the subscription, sends incident state change notifications and remove pending update
     * requests.
     *
     * When there is new version of Incident document, we need to update the Incident and Interest
     * Group Models.
     *
     * @param workProductID
     *            the work product id
     * @param subscriptionId
     *            the subscription id
     * @ssdd
     */
    @Override
    public void newWorkProductVersion(String workProductID, Integer subscriptionId) {

        // Verify the subscription was made for a pending update
        logger.debug("newWorkProductVersion: incident wpID=" + workProductID + " subscriptionId=" +
                     subscriptionId);

        // When the Incident document has updated on the remote core successfully, we need to
        // update the Incident & InterestGroup model
        final WorkProduct product = workProductService.getProduct(workProductID);
        if (product != null) {
            IncidentDocument theIncident = null;
            try {
                theIncident = (IncidentDocument) product.getProduct();
            } catch (final Exception e) {
                logger.error("the work product is not a valid Incident Document\n" +
                             e.getMessage() + "\n" + product.getProduct().xmlText());
                return;
            }
            // if owning core is not itself and there is no interest group created
            // then it's the first Incident document after the sharing, no update needed
            final String owningCore = theIncident.getIncident().getOwningCore();
            final String interestGroupId = getIncidentID(theIncident.getIncident());
            if (interestGroupDAO.findByInterestGroup(interestGroupId) == null) {
                if (owningCore.equals(directoryService.getCoreName())) {
                    logger.error(directoryService.getCoreName() +
                                 " is the owner, but no Interest Group exists");
                } else {
                    logger.debug("this is the first Incident Document update on the joined core");
                }
                return;
            } else if (!owningCore.equals(directoryService.getCoreName())) {
                logger.debug("newWorkProductVersion: Update: Incident:" + product.getProductID() +
                    ", Version: " + product.getProductVersion() + ", owned by core: " +
                    owningCore);
                updateIncidentModelAndInterestGroupInfo(theIncident.getIncident(), owningCore,
                    product.getProductID());
            }
        }
    }

    /**
     * Notify of incident state change.
     *
     * @param notification
     *            the notification
     * @ssdd
     */
    public void notifyOfIncidentStateChange(IncidentStateNotificationMessage notification) {

        logger.info("#### notifyOfIncidentStateChange() called with incident:" +
                    notification.getIncidentInfo().getId() + " state:" + notification.getState() +
                    " thead:" + Thread.currentThread().getName());
        final Message<IncidentStateNotificationMessage> message = new GenericMessage<IncidentStateNotificationMessage>(notification);
        incidentStateNotificationChannel.send(message);

    }

    /**
     * public void owningCoreProductNotificationHandler(PublishEDXLProductMessage message) {
     * log.debug("owningCoreProductNotificationHandler: receive product notification"); if
     * (message.getProductType().equals(Type)) { log .info(
     * "owningCoreProductNotificationHandler: receive notification of an incident work product published by core:"
     * + message.getOwningCore()); EDXLDistributionDocument doc = message.getEdxlProduct(); String
     * owningCore = message.getOwningCore(); ContentObjectType content =
     * doc.getEDXLDistribution().getContentObjectArray(0); if (content == null) {
     * log.error("No content: " + doc.toString()); return; } String productType =
     * EDXLDistributionHelper.getProductType(content); String productID =
     * EDXLDistributionHelper.getProductID(content); String incidentID = content.getIncidentID();
     * IncidentDocument incidentDoc = null; try { incidentDoc =
     * IncidentDocument.Factory.parse(content.getXmlContent()
     * .getEmbeddedXMLContentArray(0).toString()); } catch (Exception e) {
     * log.error("publishIncidentWorkProductHandler: cannot parse incident: " + e.getMessage());
     * return; } Incident incident = getIncidentDAO().findByIncidentID(incidentID); if (incident ==
     * null) { // persist the incident model incident = new Incident();
     * incident.setIncidentId(incidentID);
     * incident.setName(getIncidentName(incidentDoc.getIncident()));
     * incident.setOwningCore(owningCore); incident.setWorkProductID(productID);
     * getIncidentDAO().makePersistent(incident); } // persist the work product
     * getWorkProductService().publishProduct(doc.getEDXLDistribution());
     * sendIncidentStateChangeMessages(InterestGroupStateNotificationMessage.State.JOIN, incident);
     * } }
     */

    // persist the incident model
    private void persistIncident(UICDSIncidentType incident, String incidentWPID) {

        final String incidentID = getIncidentID(incident);

        Incident i = getIncidentDAO().findByIncidentID(incidentID);
        if (i == null) {
            i = new Incident();
            i.setIncidentId(incidentID);
            i.setWorkProductID(incidentWPID);
        }
        // After published the work product, we will do another persist and the work product Id will
        // be needed to persisted.
        final String incidentWPId = getIncidentWPID(incident);
        if (incidentWPId != null) {
            i.setWorkProductID(incidentWPId);
        }

        // Point point = EMGeoUtil.parsePoint(incident);
        final Point point = IncidentUtil.getIncidentLocation(incident);
        if (point != null) {
            i.setLatitude(point.getY());
            i.setLongitude(point.getX());
        } else {
            logger.debug("persistIncident: Location not found in incident");
        }

        // TODO: what is the DateRepresentation ?
        // setDate(getIncidentDateRepresentation(incident));

        try {
            // r is here only to make EasyMock happy in the unit tests
            Incident r = getIncidentDAO().makePersistent(i);
            r = null;
        } catch (final HibernateException e) {
            logger.error("persistIncident: HibernateException makePersistent incidentDAO: " +
                         e.getMessage() + " from " + e.toString());
        } catch (final Exception e) {
            logger.error("persistIncident: Exception makePersistent incidentDAO: " +
                         e.getMessage() + " from " + e.toString());
        }
    }

    private void sendIncidentStateChangeMessages(InterestGroupStateNotificationMessage.State state,
                                                 Incident incident,
                                                 InterestGroupInfo igInfo) {

        if (incident == null) {
            logger.error("sendIncidentStateChangeMessages: incident is null");
            return;
        }

        if (igInfo == null) {
            logger.error("sendIncidentStateChangeMessages: interest group info is null");
            return;
        }

        final IncidentInfoType incidentInfo = toIncidentInfoType(incident, igInfo);

        if (incidentInfo == null) {
            logger.error("sendIncidentStateChangeMessages: unable to retrieve incidentInfo");
            return;
        }

        // TODO: the sending of this message, IncidentStateChangeMessage, should be changed
        // to a simple method invocation
        final IncidentStateNotificationMessage mesg = new IncidentStateNotificationMessage();
        mesg.setState(state);
        mesg.setIncidentInfo(incidentInfo);

        logger.debug("sendIncidentStateChangeMessage: \nmessage= IncidentStateNotificationMessage \nstate= " +
                     mesg.getState() +
                     "\nwpID=" +
                     mesg.getIncidentInfo().getWorkProductIdentification().getIdentifier().getStringValue() +
                     "\nID=" +
                     mesg.getIncidentInfo().getId() +
                     "\ndesc:" +
                     mesg.getIncidentInfo().getDescription() +
                     "\nlat=" +
                     mesg.getIncidentInfo().getLatitude() +
                     "\nlong=" +
                     mesg.getIncidentInfo().getLongitude() +
                     "\nname:" +
                     mesg.getIncidentInfo().getName() +
                     " \nowningCore=" +
                     mesg.getIncidentInfo().getOwningCore());
        notifyOfIncidentStateChange(mesg);

    }

    public void setAgreementDAO(AgreementDAO agreementDAO) {

        this.agreementDAO = agreementDAO;
    }

    /** {@inheritDoc} */
    public void setConfigurationService(ConfigurationService configurationService) {

        this.configurationService = configurationService;
    }

    /** {@inheritDoc} */
    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    /**
     * @param getIconConfigXmlFilePath
     *            to set
     */
    public void setIconConfigXmlFilePath(String iconConfigXmlFilePath) {

        this.iconConfigXmlFilePath = iconConfigXmlFilePath;
    }

    /** {@inheritDoc} */
    public void setIncidentDAO(IncidentDAO incidentDAO) {

        this.incidentDAO = incidentDAO;
    }

    // make the ActivityIdentification to be immutable
    private void setIncidentID(UICDSIncidentType incident, String incidentID) {

        if (incident.sizeOfActivityIdentificationArray() > 0) {
            incident.removeActivityIdentification(0);
        }

        if ((incidentID != null) && (incidentID.length() > 0)) {
            incident.addNewActivityIdentification().addNewIdentificationID().setStringValue(
                incidentID);
        }
    }

    /**
     * @param channel
     */
    public void setIncidentStateNotificationChannel(MessageChannel channel) {

        incidentStateNotificationChannel = channel;
    }

    public void setInterestGroupDAO(InterestGroupDAO interestGroupDAO) {

        this.interestGroupDAO = interestGroupDAO;
    }

    public void setInterestGroupManagementComponent(InterestGroupManagementComponent interestGroupManagementComponent) {

        this.interestGroupManagementComponent = interestGroupManagementComponent;
    }

    public void setLdapUtil(LdapUtil ldapUtil) {

        this.ldapUtil = ldapUtil;
    }

    public void setPubSubService(PubSubService pubSubService) {

        this.pubSubService = pubSubService;
    }

    public void setUserInterestGroupDAO(UserInterestGroupDAO userInterestGroupDAO) {

        this.userInterestGroupDAO = userInterestGroupDAO;
    }

    /**
     * @param workProductService
     */
    public void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }

    /**
     * @param xsltFilePath
     *            the xsltFilePath to set
     */
    public void setXsltFilePath(String xsltFilePath) {

        this.xsltFilePath = xsltFilePath;
    }

    /**
     * Share incident. Adds the core to be shared with to the list of shared cores and re-publish
     * the incident work product This method does not set the agreement checked parameter so
     * interest group state notifications are not triggered.
     *
     * @param shareIncidentRequest
     *            the share incident request
     * @return true, if successful
     * @throws UICDSException
     *             the UICDS exception
     * @ssdd
     */
    @Override
    public boolean shareIncident(ShareIncidentRequest shareIncidentRequest) throws UICDSException {

        return doShareIncident(shareIncidentRequest, false);
    }

    /**
     * Share incident agreement checked. This method sets the agreement checked parameter which
     * triggers interest group state notification.
     *
     * @param shareIncidentRequest
     *            the share incident request
     * @return true, if successful
     * @throws UICDSException
     *             the UICDS exception
     * @ssdd
     */
    @Override
    public boolean shareIncidentAgreementChecked(ShareIncidentRequest shareIncidentRequest)
        throws UICDSException {

        return doShareIncident(shareIncidentRequest, true);
    }

    @Override
    public void systemInitializedHandler(String message) {

        logger.debug("systemInitializedHandler: ... start ...");
        // register with the directory service
        final WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(Type);
        getDirectoryService().registerUICDSService(NS_IncidentManagementService, IMS_SERVICE_NAME,
            typeList, typeList);

        // remove any existing subscriptions for this service (trac#602)
        final List<Integer> subscriptionIDs = pubSubService.getSubscriptionsByServiceName(getServiceName());
        for (final Integer subscriptionID : subscriptionIDs) {
            pubSubService.unsubscribeBySubscriptionID(subscriptionID);
        }

        // resubscribe to Incident work product updates
        try {
            // pubSubService.
            pubSubService.subscribeWorkProductType(IncidentManagementService.Type, null,
                new HashMap<String, String>(), this);
        } catch (final Exception e) {
            logger.error("Unable to subscribe to own product type");
            e.printStackTrace();
        }
        init();

        // log.info("calling DigestGenerator(xsltFilePath)");
        // digestGenerator = new DigestGenerator(xsltFilePath);
        logger.debug("systemInitializedHandler: ... done ...");
    }

    private IncidentInfoType toIncidentInfoType(Incident incident, InterestGroupInfo igInfo) {

        if (incident == null) {
            logger.error("toIncidentInfoType - incident is null");
            return null;
        }

        final IdentificationType identification = workProductService.getProductIdentification(incident.getWorkProductID());

        if (identification == null) {
            logger.error("toIncidentInfoType - unable to retrieve product identification for incident:" +
                         incident.getIncidentId() + ", wp: " + incident.getWorkProductID());
            return null;
        }

        final IncidentInfoType info = IncidentInfoType.Factory.newInstance();
        info.setId(incident.getIncidentId());
        info.setWorkProductIdentification(identification);
        info.setLatitude(incident.getLatitude());
        info.setLongitude(incident.getLongitude());
        info.setDate(new GregorianCalendar().getTime().toString());
        info.setName(igInfo.getName());
        info.setDescription(igInfo.getDescription());
        info.setOwningCore(igInfo.getOwningCore());

        return info;
    }

    /**
     * Update incident.
     *
     * @param incident
     *            the incident
     * @param pkgId
     *            the pkg id
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateIncident(UICDSIncidentType incident,
                                                   IdentificationType pkgId) {

        final String productID = pkgId.getIdentifier().getStringValue();

        final WorkProduct wp = getWorkProductService().getProduct(productID);
        if (incident.sizeOfActivityCategoryTextArray() > 0) {
            logger.debug("updateIncident: wpID=" + productID + "category:" +
                         incident.getActivityCategoryTextArray(0).toString());
        }

        if (incident.sizeOfIncidentLocationArray() > 0) {
            final Point point = IncidentUtil.getIncidentLocation(incident);
            if (point != null) {
                logger.debug("===> updateIncidet: lat=" + IncidentUtil.getLatitude(point) +
                    " long=" + IncidentUtil.getLongititude(point));
            } else {
                logger.debug("===> updateIncidet: - location not specified in the update");
            }
        }

        if (wp == null) {
            logger.error("updateIncident: unable to locate incident wp for productID=" + productID);
            final ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            if (workProductService.isDeleted(productID) == true) {
                status.setReasonForFailure(productID + " is inactive");
            } else {
                status.setReasonForFailure(productID + " cannot be located");
            }
            return status;
        } else {
            IncidentDocument persistedIncidentDoc = null;
            try {
                persistedIncidentDoc = (IncidentDocument) wp.getProduct();
            } catch (final Exception e) {
                e.printStackTrace();
                logger.error("updateIncident for " + productID + " failed: " + e.getMessage());
                return null;
            }

            // this may need to change when we allow ownership transfer
            incident.setOwningCore(persistedIncidentDoc.getIncident().getOwningCore());

            incident.setSharedCoreNameArray(persistedIncidentDoc.getIncident().getSharedCoreNameArray());
            // enforce the incident Id is immutable
            setIncidentID(incident, getIncidentID(persistedIncidentDoc.getIncident()));

            final IncidentDocument incidentDoc = IncidentDocument.Factory.newInstance();
            incidentDoc.setIncident(incident);

            WorkProduct newWp = newWorkProduct(incidentDoc, wp.getProductID());
            newWp = WorkProductHelper.setWorkProductIdentification(newWp, pkgId);

            // Create the digest if we have an XSLT
            digestGenerator = new DigestGenerator(xsltFilePath, iconConfigXmlFilePath);
            final DigestDocument digestDoc = digestGenerator.createDigest(incidentDoc);
            // log.info("digestDoc="+digestDoc);
            newWp.setDigest(digestDoc);

            // set the digest for the work product
            // newWp.setDigest(new EMDigestHelper(incidentDoc.getIncident()).getDigest());

            // publish the work product
            final ProductPublicationStatus status = getWorkProductService().publishProduct(newWp);
            final String owningCore = persistedIncidentDoc.getIncident().getOwningCore();

            if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
                final InterestGroupInfo igInfo = updateIncidentModelAndInterestGroupInfo(incident,
                    owningCore, status.getProduct().getProductID());

                if ((owningCore != null) && directoryService.getCoreName().equals(owningCore) &&
                    (igInfo != null)) {
                    // Only send notifications if it's an update by owning core
                    // create interest group for the incident
                    sendIncidentStateChangeMessages(
                        InterestGroupStateNotificationMessage.State.UPDATE,
                        getIncidentDAO().findByIncidentID(getIncidentID(incident)), igInfo);
                }
                // make a performance log entry
                final LogEntry logEntry = new LogEntry();
                logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
                logEntry.setAction(LogEntry.ACTION_INCIDENT_UPDATE);
                logEntry.setCoreName(owningCore);
                if (newWp != null) {
                    logEntry.setIncidentId(newWp.getProductID());
                    logEntry.setIncidentType(newWp.getProductType());
                }
                logEntry.setUpdatedBy(ServletUtil.getPrincipalName());
                logger.info(logEntry.getLogEntry());
            }
            // even we put subscription into pending hash, we cannot tell when the
            // newWorkProductVersion is invoked whether it's from original core or not
            /*
             * else if (status.getStatus().equals(ProductPublicationStatus.PendingStatus)) { try {
             * // subscribe to the work product ID so we can send notifications when the // update
             * is completed. Add the pending update request to map to be use to // check against
             * when receiving new product version notifications.
             * log.debug("updateIncident: workProduct: " + productID + " owned by core [" +
             * owningCore + "]"); Integer subscriptionID =
             * pubSubService.subscribeWorkProductIDNewVersions( productID, this);
             * log.debug("updateIncident: subcriptionID=" + subscriptionID);
             * pendingRemoteUpdateRequests.put(subscriptionID.toString(), productID);
             *
             * } catch (Exception e) { log.error(
             * "Error - updateIncident:  Unable to subscribe to a pending update worproduct; ID=" +
             * productID); e.printStackTrace(); } }
             */

            return status;
        }
    }

    private InterestGroupInfo updateIncidentModelAndInterestGroupInfo(UICDSIncidentType incident,
                                                                      String owningCore,
                                                                      String wpID) {

        // persist the updated incident model after the work product is published
        persistIncident(incident, wpID);

        // Determine who owns the work product being updated
        final InterestGroupInfo igInfo = new InterestGroupInfo();
        igInfo.setInterestGroupID(getIncidentID(incident));
        igInfo.setInterestGroupType(IncidentManagementService.InterestGroupType);
        igInfo.setInterestGroupSubType(getIncidentActivityCategory(incident));
        igInfo.setName(getIncidentName(incident));
        igInfo.setDescription(getIncidentDescription(incident));
        igInfo.setOwningCore(owningCore);
        try {
            interestGroupManagementComponent.updateInterestGroup(igInfo);
        } catch (final InvalidInterestGroupIDException e) {
            logger.error("Caught InvalidInterestGroupIDException while attempting to update interestGroup with ID=" +
                         getIncidentID(incident));
            return null;
        }
        return igInfo;
    }

    private ProductPublicationStatus validateIncident(String incidentID) {

        if (incidentID == null) {

            final ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("Empty Incident ID");
            return status;
        }

        if (getIncidentDAO().findByIncidentID(incidentID) == null) {
            return new ProductPublicationStatus("Incident: " + incidentID + " does not exist");
        }

        // if the core is not the owning core then return failure
        if (getInterestGroupDAO().ownedByCore(incidentID, getDirectoryService().getLocalCoreJid()) == false) {

            final ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(getDirectoryService().getCoreName() +
                " doesn't own incident: " + incidentID);
            return status;
        }
        return null;
    }

    /**
     * Work product deleted. Not yet implemented
     *
     * @param workProductID
     *            the work product id
     * @param workProductType
     *            the work product type
     * @param subscriptionId
     *            the subscription id
     */
    @Override
    public void workProductDeleted(ProductChangeNotificationMessage changedMessage,
                                   Integer subscriptionId) {

        // Nothing to do for now
    }

}
