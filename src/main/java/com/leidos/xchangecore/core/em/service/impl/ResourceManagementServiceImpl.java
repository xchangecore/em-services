package com.leidos.xchangecore.core.em.service.impl;

import gov.ucore.ucore.x20.ContentMetadataType;
import gov.ucore.ucore.x20.DigestDocument;
import gov.ucore.ucore.x20.EntityRefType;
import gov.ucore.ucore.x20.EntityType;
import gov.ucore.ucore.x20.EventRefType;
import gov.ucore.ucore.x20.EventType;
import gov.ucore.ucore.x20.LocationType;
import gov.ucore.ucore.x20.OrganizationType;
import gov.ucore.ucore.x20.PointDocument;
import gov.ucore.ucore.x20.SimplePropertyType;
import gov.ucore.ucore.x20.StringType;
import gov.ucore.ucore.x20.ThingType;
import gov.ucore.ucore.x20.WhatType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import mil.dod.metadata.mdr.ns.ddms.x20.CompoundCountryCodeIdentifierType;
import mil.dod.metadata.mdr.ns.ddms.x20.PostalAddressDocument.PostalAddress;
import oasisNamesTcCiqXal3.AddressType;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsn.b2.NotificationMessageHolderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.resourceManagementService.EdxlDeResponseDocument;

import x0.oasisNamesTcEmergencyEDXLDE1.ContentObjectType;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;
import x0.oasisNamesTcEmergencyEDXLDE1.ValueSchemeType;
import x0.oasisNamesTcEmergencyEDXLRM1.ContactInformationType;
import x0.oasisNamesTcEmergencyEDXLRM1.IncidentInformationType;
import x0.oasisNamesTcEmergencyEDXLRM1.RecallTypeType;
import x0.oasisNamesTcEmergencyEDXLRM1.ResponseTypeType;
import x0.oasisNamesTcEmergencyEDXLRM1.ScheduleTypeType;
import x0.oasisNamesTcEmergencyEDXLRM1.TypeInfoType;
import x0.oasisNamesTcEmergencyEDXLRM1.ValueListURNType;
import x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument;
import x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument.CommitResource;
import x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument.CommitResource.ResourceInformation;
import x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument;
import x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument.RequestResource;
import x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument.RequestResource.ResourceInformation.ScheduleInformation;

import com.leidos.xchangecore.core.em.exceptions.SendMessageErrorException;
import com.leidos.xchangecore.core.em.service.ResourceManagementService;
import com.leidos.xchangecore.core.em.util.BroadcastUtil;
import com.leidos.xchangecore.core.em.util.DigestGenerator;
import com.leidos.xchangecore.core.em.util.EMDigestHelper;
import com.leidos.xchangecore.core.em.util.EMGeoUtil;
import com.leidos.xchangecore.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.leidos.xchangecore.core.infrastructure.exceptions.InvalidXpathException;
import com.leidos.xchangecore.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareAgreementException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.leidos.xchangecore.core.infrastructure.exceptions.RemoteCoreUnavailableException;
import com.leidos.xchangecore.core.infrastructure.exceptions.RemoteCoreUnknownException;
import com.leidos.xchangecore.core.infrastructure.messages.Core2CoreMessage;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.CommunicationsService;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.NotificationService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.DigestConstant;
import com.leidos.xchangecore.core.infrastructure.util.InfrastructureNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.StringUtil;
import com.leidos.xchangecore.core.infrastructure.util.UUIDUtil;

/**
 * The ResourceManagementService implementation.
 *
 * @author roger
 * @see com.leidos.xchangecore.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */

public class ResourceManagementServiceImpl
    implements ResourceManagementService, ServiceNamespaces, InfrastructureNamespaces {

    public static final QName REQUEST_RESOURCE_QNAME = RequestResourceDocument.type.getDocumentElementName();
    public static final QName COMMIT_RESOURCE_QNAME = CommitResourceDocument.type.getDocumentElementName();
    public static final QName VALUE_LIST_URN_QNAME = new QName(ValueListURNType.type.getName().getNamespaceURI(),
        "ValueListURN");
    public static final String NIMS_NS = "http://nimsonline.org/2.0";

    static final String NEW_LINE = System.getProperty("line.separator");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private WorkProductService workProductService;
    private DirectoryService directoryService;
    private CommunicationsService communicationsService;
    private NotificationService notificationService;

    private String xsltFilePath;
    private String iconConfigXmlFilePath;

    private DigestGenerator digestGenerator;

    private LocationType addDestinationLocation(x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument.CommitResource.ResourceInformation info) {

        // Create a digest location element of for the first schedule element of the correct type
        LocationType destination = null;
        if (info.sizeOfScheduleInformationArray() > 0)
            for (final x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument.CommitResource.ResourceInformation.ScheduleInformation sch : info.getScheduleInformationArray())
                if (sch.getScheduleType() == ScheduleInformation.ScheduleType.REQUESTED_ARRIVAL ||
                    sch.getScheduleType() == ScheduleTypeType.ESTIMATED_ARRIVAL ||
                    sch.getScheduleType() == ScheduleInformation.ScheduleType.REQUESTED_DEPARTURE ||
                    sch.getScheduleType() == ScheduleTypeType.ESTIMATED_DEPARTURE ||
                    sch.getScheduleType() == ScheduleTypeType.ACTUAL_DEPARTURE ||
                    sch.getScheduleType() == ScheduleTypeType.COMMITTED ||
                    sch.getScheduleType() == ScheduleInformation.ScheduleType.REPORT_TO ||
                    sch.getScheduleType() == ScheduleInformation.ScheduleType.ROUTE) {
                    if (sch.getLocation() != null) {
                        destination = LocationType.Factory.newInstance();
                        destination.setId(UUIDUtil.getID("Location"));
                        // TODO: handle more than just description
                        if (sch.getLocation().getLocationDescription() != null)
                            destination.addNewDescriptor().setStringValue(
                                sch.getLocation().getLocationDescription());
                        // Handle target area
                        if (sch.getLocation().getTargetArea() != null)
                            if (sch.getLocation().getTargetArea().getPoint() != null) {
                                final PointDocument point = PointDocument.Factory.newInstance();
                                point.addNewPoint().addNewPoint();
                                point.getPoint().getPoint().setId(UUIDUtil.getID("point"));
                                point.getPoint().getPoint().addNewPos().setStringValue(
                                    sch.getLocation().getTargetArea().getPoint().getPos().getStringValue());
                                point.getPoint().getPoint().getPos().setSrsName(EMGeoUtil.EPSG4326);
                                destination.addNewGeoLocation().set(point);
                            }
                        if (sch.getLocation().getAddress() != null) {
                            final AddressType a = sch.getLocation().getAddress();
                            final PostalAddress postalAddress = digestAddressType(sch.getLocation().getAddress());
                            destination.addNewPhysicalAddress().setPostalAddress(postalAddress);
                        }
                    }
                    break;
                }

        return destination;
    }

    private LocationType addDestinationLocation(x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument.RequestResource.ResourceInformation info) {

        // Create a digest location element of for the first schedule element of the correct type
        LocationType destination = null;
        if (info.sizeOfScheduleInformationArray() > 0)
            for (final ScheduleInformation sch : info.getScheduleInformationArray())
                if (sch.getScheduleType() == ScheduleInformation.ScheduleType.REQUESTED_ARRIVAL ||
                    sch.getScheduleType() == ScheduleInformation.ScheduleType.REQUESTED_DEPARTURE ||
                    sch.getScheduleType() == ScheduleInformation.ScheduleType.REPORT_TO) {
                    destination = LocationType.Factory.newInstance();
                    destination.setId(UUIDUtil.getID("Location"));
                    if (sch.getLocation() != null) {
                        // Add in location description if available
                        if (sch.getLocation().getLocationDescription() != null)
                            destination.addNewDescriptor().setStringValue(
                                sch.getLocation().getLocationDescription());
                        // Handle target area
                        if (sch.getLocation().getTargetArea() != null)
                            if (sch.getLocation().getTargetArea().getPoint() != null) {
                                final PointDocument point = PointDocument.Factory.newInstance();
                                point.addNewPoint().addNewPoint();
                                point.getPoint().getPoint().setId(UUIDUtil.getID("point"));
                                point.getPoint().getPoint().addNewPos().setStringValue(
                                    sch.getLocation().getTargetArea().getPoint().getPos().getStringValue());
                                point.getPoint().getPoint().getPos().setSrsName(EMGeoUtil.EPSG4326);
                                destination.addNewGeoLocation().set(point);
                            }
                        if (sch.getLocation().getAddress() != null) {
                            final AddressType a = sch.getLocation().getAddress();
                            final PostalAddress postalAddress = digestAddressType(sch.getLocation().getAddress());
                            destination.addNewPhysicalAddress().setPostalAddress(postalAddress);
                        }
                    }
                    break;
                }

        return destination;
    }

    private void addNIMSTypePropertiesFromCommit(EntityType event,
                                                 x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument.CommitResource.ResourceInformation info) {

        if (info.getResource().getTypeStructure() != null) {
            final XmlObject[] valueLists = info.getResource().getTypeStructure().selectChildren(
                VALUE_LIST_URN_QNAME);
            if (valueLists.length > 0) {
                final XmlCursor c = valueLists[0].newCursor();
                // make sure this is the type struct we are looking for
                if (c.getTextValue().equals(NIMS_NS)) {
                    addTypeInfoProperty(event, "Category", info.getResource().getTypeInfo());
                    addTypeInfoProperty(event, "Kind", info.getResource().getTypeInfo());
                    addTypeInfoProperty(event, "Resource", info.getResource().getTypeInfo());
                    addTypeInfoProperty(event, "MinimumCapabilities",
                        info.getResource().getTypeInfo());
                }
                c.dispose();
            }
        }
    }

    private void addNIMSTypePropertiesFromRequest(EventType event,
                                                  x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument.RequestResource.ResourceInformation info) {

        if (info.getResource().getTypeStructure() != null) {
            final XmlObject[] valueLists = info.getResource().getTypeStructure().selectChildren(
                VALUE_LIST_URN_QNAME);
            if (valueLists.length > 0) {
                final XmlCursor c = valueLists[0].newCursor();
                // make sure this is the type struct we are looking for
                if (c.getTextValue().equals(NIMS_NS)) {
                    addTypeInfoProperty(event, "Category", info.getResource().getTypeInfo());
                    addTypeInfoProperty(event, "Kind", info.getResource().getTypeInfo());
                    addTypeInfoProperty(event, "Resource", info.getResource().getTypeInfo());
                    addTypeInfoProperty(event, "MinimumCapabilities",
                        info.getResource().getTypeInfo());
                }
                c.dispose();
            }
        }
    }

    private void addQuantityProperty(EventType event,
                                     x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument.RequestResource.ResourceInformation resourceInfo) {

        if (resourceInfo.getAssignmentInformation() != null &&
            resourceInfo.getAssignmentInformation().getQuantity() != null &&
            resourceInfo.getAssignmentInformation().getQuantity().sizeOfQuantityTextArray() > 0) {
            final String codespace = resourceInfo.getAssignmentInformation().getQuantity().schemaType().getName().getNamespaceURI();
            final String label = "Quantity";
            final String code = "AssignmentInformation.Quantity";
            final String value = resourceInfo.getAssignmentInformation().getQuantity().getQuantityTextArray(
                0);

            addSimpleProperty(event, codespace, code, label, value);
        }
    }

    private void addSimpleProperty(ThingType thing,
                                   String codespace,
                                   String code,
                                   String label,
                                   String value) {

        final SimplePropertyType property = createSimpleProperty(codespace, code, label, value);
        thing.addNewSimpleProperty().set(property);
        // System.out.println(event);
    }

    private void addTypeInfoProperty(ThingType thing, String elementName, TypeInfoType typeInfo) {

        final XmlObject[] category = typeInfo.selectChildren(null, elementName);
        if (category.length > 0) {
            final XmlCursor c = category[0].newCursor();
            addSimpleProperty(thing, NIMS_NS, null, elementName, c.getTextValue());
            c.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    private DigestDocument createCommitResourceDigest(CommitResourceDocument commitDocument) {

        final EMDigestHelper digest = new EMDigestHelper();
        final CommitResource commitResource = commitDocument.getCommitResource();

        String label = null;
        // Try to label with the description of the sender first then just use the first description
        // found
        if (commitResource.sizeOfContactInformationArray() > 0) {
            for (final ContactInformationType contact : commitResource.getContactInformationArray())
                if (contact.getContactRole() != null &&
                    contact.getContactRole() == x0.oasisNamesTcEmergencyEDXLRM1.ContactRoleType.SENDER)
                    if (contact.getContactDescription() != null &&
                        !contact.getContactDescription().isEmpty())
                        label = contact.getContactDescription();
            if (label == null)
                for (final ContactInformationType contact : commitResource.getContactInformationArray())
                    if (contact.getContactDescription() != null &&
                        !contact.getContactDescription().isEmpty())
                        label = contact.getContactDescription();
        }

        // Create the Event element for the incident
        EventRefType incidentRef = null;
        if (commitResource.sizeOfIncidentInformationArray() > 0) {
            String msgDesc = null;
            if (commitResource.getMessageDescription() != null)
                msgDesc = commitResource.getMessageDescription().toString();
            incidentRef = digestIncidentInformation(digest,
                commitResource.getIncidentInformationArray(0), msgDesc,
                COMMIT_RESOURCE_PRODUCT_TYPE, label);
        }

        // List of all commit events created from this RequestResource
        final ArrayList<String> commitEvents = new ArrayList<String>();

        final Calendar now = Calendar.getInstance();

        // Create Entity elements for each resource
        if (commitResource.sizeOfResourceInformationArray() > 0)
            // Need to use fqn here because EDXL-RM overloads the
            // ResourceInformation element name
            for (final x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument.CommitResource.ResourceInformation info : commitResource.getResourceInformationArray()) {
                final EntityRefType ref = digestCommitResourceInformation(digest, info,
                    commitResource.getOriginatingMessageID(), getCommitStatusProperty(info));
                commitEvents.addAll(ref.getRef());
                // Involve the entity in the incident event
                if (incidentRef != null)
                    digest.setInvolvedIn((String) ref.getRef().get(0),
                        (String) incidentRef.getRef().get(0), now);
                final LocationType location = addDestinationLocation(info);
                if (location != null) {
                    digest.setLocation(location);
                    digest.setHasDestinationOf((String) ref.getRef().get(0), location.getId(), now);
                }
            }

        return digest.getDigest();
    }

    // TODO: need to deal with ContactInformation elements from requests. I
    // think they should
    // be Organization elements with relationships to the Events for the
    // requests.
    @SuppressWarnings("unchecked")
    private DigestDocument createRequestResourceDigest(RequestResourceDocument requestDocument) {

        final EMDigestHelper digest = new EMDigestHelper();
        final RequestResource req = requestDocument.getRequestResource();

        String label = null;
        // Try to label with the description of the requester first then just use the first
        // description
        // found
        if (req.sizeOfContactInformationArray() > 0) {
            for (final ContactInformationType contact : req.getContactInformationArray())
                if (contact.getContactRole() != null &&
                    contact.getContactRole() == x0.oasisNamesTcEmergencyEDXLRM1.ContactRoleType.REQUESTER)
                    if (contact.getContactDescription() != null &&
                        !contact.getContactDescription().isEmpty())
                        label = contact.getContactDescription();
            if (label == null)
                for (final ContactInformationType contact : req.getContactInformationArray())
                    if (contact.getContactDescription() != null &&
                        !contact.getContactDescription().isEmpty())
                        label = contact.getContactDescription();
        }

        // Create the Event element for the incident
        EventRefType incidentRef = null;
        if (req.sizeOfIncidentInformationArray() > 0) {
            String msgDesc = null;
            if (req.getMessageDescription() != null)
                msgDesc = req.getMessageDescription().toString();
            incidentRef = digestIncidentInformation(digest, req.getIncidentInformationArray(0),
                msgDesc, REQUEST_RESOURCE_PRODUCT_TYPE, label);
        }

        // List of all request events created from this RequestResource
        final ArrayList<String> requestEvents = new ArrayList<String>();

        final Calendar now = Calendar.getInstance();

        // Create Event elements for each resource request
        if (req.sizeOfResourceInformationArray() > 0)
            // Need to use fqn here because EDXL-RM overloads the
            // ResourceInformation element name
            for (final x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument.RequestResource.ResourceInformation info : req.getResourceInformationArray()) {
                final EventRefType ref = digestRequestResourceInformation(digest, info,
                    req.getOriginatingMessageID(), getRequestStatus(req));
                requestEvents.addAll(ref.getRef());
                final LocationType destinationLocation = addDestinationLocation(info);
                if (ref.getRef().size() > 0 && destinationLocation != null) {
                    digest.setLocation(destinationLocation);
                    if (incidentRef != null)
                        // Assume first entry is the correct identifier string
                        digest.setOccursAt((String) incidentRef.getRef().get(0),
                            destinationLocation.getId(), now);
                }
            }

        // Create a Event caused the request resource event
        if (incidentRef != null && requestEvents.size() > 0)
            for (final String eventId : requestEvents)
                digest.setCauseOf((String) incidentRef.getRef().get(0), eventId);

        // Create a digest element for the first "Requester" ContactInformation element
        LocationType requesterLocation = null;
        OrganizationType requesterOrganization = null;
        if (req.sizeOfContactInformationArray() > 0)
            for (final ContactInformationType contact : req.getContactInformationArray())
                if (contact.getContactRole() == x0.oasisNamesTcEmergencyEDXLRM1.ContactRoleType.REQUESTER &&
                    contact.getContactLocation() != null) {
                    // Create a location element for the requester
                    requesterLocation = getLocationFromContactInformation(digest,
                        contact.getContactLocation());
                    // Create an organization element for the requester
                    requesterOrganization = getOrganizationFromContactInformation(digest, contact);
                    digest.setLocatedAt(requesterOrganization.getId(), requesterLocation.getId(),
                        now);
                    // Create an InvolvedIn relationship to each request event
                    for (final String eventId : requestEvents)
                        digest.setInvolvedIn(requesterOrganization.getId(), eventId, now);
                    break;
                }

        return digest.getDigest();
    }

    private SimplePropertyType createSimpleProperty(String codespace,
                                                    String code,
                                                    String label,
                                                    String value) {

        final SimplePropertyType property = SimplePropertyType.Factory.newInstance();
        if (codespace != null)
            property.setCodespace(codespace);
        if (code != null)
            property.setCode(code);
        if (label != null)
            property.addNewLabel().setStringValue(label);
        if (value != null)
            property.setStringValue(value);
        return property;
    }

    private PostalAddress digestAddressType(AddressType address) {

        final PostalAddress postalAddress = PostalAddress.Factory.newInstance();
        // country codes should be ISO 3166-1
        // (http://www.iso.org/iso/english_country_names_and_code_elements)
        if (address.getCountry() != null && address.getCountry().sizeOfNameElementArray() > 0) {
            final CompoundCountryCodeIdentifierType countryCode = CompoundCountryCodeIdentifierType.Factory.newInstance();
            countryCode.setValue(address.getCountry().getNameElementArray(0).getNameCode());
            countryCode.setQualifier(address.getCountry().getNameElementArray(0).getNameCodeType());
            postalAddress.setCountryCode(countryCode);
        }
        if (address.getAdministrativeArea() != null &&
            address.getAdministrativeArea().sizeOfNameElementArray() > 0)
            postalAddress.setState(address.getAdministrativeArea().getNameElementArray(0).getNameCode());
        if (address.getLocality() != null && address.getLocality().sizeOfNameElementArray() > 0)
            postalAddress.setCity(address.getLocality().getNameElementArray(0).getStringValue());
        if (address.getThoroughfare() != null) {
            String number = "";
            if (address.getThoroughfare().sizeOfNumberArray() > 0)
                number = address.getThoroughfare().getNumberArray(0).getStringValue() + " ";
            if (address.getThoroughfare().sizeOfNameElementArray() > 0)
                postalAddress.addNewStreet().setStringValue(
                    number + address.getThoroughfare().getNameElementArray(0).getStringValue());
        }
        if (address.getPostCode() != null && address.getPostCode().sizeOfIdentifierArray() > 0)
            postalAddress.setPostalCode(address.getPostCode().getIdentifierArray(0).getStringValue());

        return postalAddress;
    }

    private EntityRefType digestCommitResourceInformation(EMDigestHelper digest,
                                                          x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument.CommitResource.ResourceInformation resourceInfo,
                                                          String originatingMessageID,
                                                          String status) {

        // set the entity
        final EntityType entity = EntityType.Factory.newInstance();
        final String entityID = getResourceID(resourceInfo, originatingMessageID);
        entity.setId(entityID);

        // Set descriptor as resource name
        if (resourceInfo.getResource().getName() != null) {
            final StringType t = StringType.Factory.newInstance();
            t.setStringValue(resourceInfo.getResource().getName());
            entity.setDescriptor(t);
        }

        // add the status based on MessageRecall value
        addSimpleProperty(entity, UICDS_EVENT_STATUS_CODESPACE, status, "Status", null);

        // Add properties
        // addContactLocationSimpleProperty(event,
        // commitDocument.getCommitResource());
        // addQuantityProperty(event, commitDocument.getCommitResource());
        addNIMSTypePropertiesFromCommit(entity, resourceInfo);

        // Set the event
        digest.setEntity(entity);

        final WhatType ucoreWhat = WhatType.Factory.newInstance();
        ucoreWhat.setCodespace(InfrastructureNamespaces.NS_UCORE_CODESPACE);
        ucoreWhat.setCode(DigestConstant.S_Equipment);
        digest.setWhatForEvent(ucoreWhat, entityID);

        final ArrayList<String> refs = new ArrayList<String>();
        refs.add(entityID);

        final EntityRefType ref = EntityRefType.Factory.newInstance();
        ref.setRef(refs);
        return ref;
    }

    private EventRefType digestIncidentInformation(EMDigestHelper digest,
                                                   IncidentInformationType incidentInfo,
                                                   String msgDesc,
                                                   String RMType,
                                                   String label) {

        // set the event
        final EventType event = EventType.Factory.newInstance();
        event.setId(incidentInfo.getIncidentID());

        // Add properties
        final SimplePropertyType property = createSimpleProperty(UICDS_EVENT_CODESPACE, "Incident",
            "Type", RMType);

        String descriptor = msgDesc;
        if (descriptor == null || descriptor.isEmpty())
            descriptor = incidentInfo.getIncidentDescription();
        final StringBuffer sb = new StringBuffer();
        sb.append(RMType);
        sb.append(" - ");
        sb.append(label);
        final String identifier = sb.toString();
        final String[] codespace = new String[0];
        final ContentMetadataType metadata = null;

        digest.setEvent(incidentInfo.getIncidentID(), descriptor, identifier, codespace, metadata,
            property);

        final WhatType ucoreWhat = WhatType.Factory.newInstance();
        ucoreWhat.setCodespace(InfrastructureNamespaces.NS_UCORE_CODESPACE);
        ucoreWhat.setCode(DigestConstant.S_Event);
        digest.setWhatForEvent(ucoreWhat, incidentInfo.getIncidentID());

        final ArrayList<String> refs = new ArrayList<String>();
        refs.add(incidentInfo.getIncidentID());

        final EventRefType ref = EventRefType.Factory.newInstance();
        ref.setRef(refs);
        return ref;
    }

    private EventRefType digestRequestResourceInformation(EMDigestHelper digest,
                                                          x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument.RequestResource.ResourceInformation resourceInfo,
                                                          String originatingMessageID,
                                                          String status) {

        // set the event
        final EventType event = EventType.Factory.newInstance();
        final String eventId = originatingMessageID + "-" + resourceInfo.getResourceInfoElementID();
        event.setId(eventId);

        // Set a simple descriptor
        final StringType t = StringType.Factory.newInstance();
        t.setStringValue("Resource Request");
        event.setDescriptor(t);

        // Add properties
        addSimpleProperty(event, UICDS_EVENT_STATUS_CODESPACE, status, "Status", null);
        addQuantityProperty(event, resourceInfo);
        addNIMSTypePropertiesFromRequest(event, resourceInfo);

        // Set the event
        digest.setEvent(event);

        final WhatType ucoreWhat = WhatType.Factory.newInstance();
        ucoreWhat.setCodespace(InfrastructureNamespaces.NS_UCORE_CODESPACE);
        ucoreWhat.setCode(DigestConstant.S_CommunicationEvent);
        digest.setWhatForEvent(ucoreWhat, eventId);

        final WhatType resourceWhat = WhatType.Factory.newInstance();
        resourceWhat.setCodespace("http://uicds.gov/1.0/codespace");
        resourceWhat.setCode(RequestResource.MessageContentType.REQUEST_RESOURCE.toString());
        digest.setWhatForEvent(resourceWhat, eventId);

        final ArrayList<String> refs = new ArrayList<String>();
        refs.add(eventId);

        final EventRefType ref = EventRefType.Factory.newInstance();
        ref.setRef(refs);
        return ref;
    }

    /**
     * Parse the content of the edxl message, determine the type of request, process the request
     * appropriately and send the message to each core that has a user in an explictAddress
     *
     * @param edxl
     *            the edxl
     *
     * @return the edxl de response document
     *
     * @throws IllegalArgumentException
     *             the illegal argument exception
     * @throws EmptyCoreNameListException
     *             the empty core name list exception
     * @throws SendMessageErrorException
     *             the send message error exception
     * @throws LocalCoreNotOnlineException
     *             the local core not online exception
     * @throws NoShareRuleInAgreementException
     * @throws NoShareAgreementException
     * @ssdd
     */
    @Override
    public EdxlDeResponseDocument edxldeRequest(EDXLDistribution edxl)
        throws IllegalArgumentException, EmptyCoreNameListException, SendMessageErrorException,
        LocalCoreNotOnlineException, NoShareAgreementException, NoShareRuleInAgreementException {

        final EdxlDeResponseDocument response = EdxlDeResponseDocument.Factory.newInstance();
        response.addNewEdxlDeResponse();

        // Check if we have the content we want else return null
        if (edxl.sizeOfContentObjectArray() > 0 &&
            edxl.getContentObjectArray(0).getXmlContent() != null &&
            edxl.getContentObjectArray(0).getXmlContent().sizeOfEmbeddedXMLContentArray() > 0) {

            // Determine what type of RM message is in the embedded xml content
            final XmlCursor c = edxl.getContentObjectArray(0).getXmlContent().getEmbeddedXMLContentArray(
                0).newCursor();
            if (c.toFirstChild()) {
                if (c.getObject().schemaType().getOuterType().getDocumentElementName() == REQUEST_RESOURCE_QNAME) {
                    logger.info("Processing: " + REQUEST_RESOURCE_QNAME);
                    final RequestResource r = (RequestResource) c.getObject();
                    final RequestResourceDocument requestDocument = RequestResourceDocument.Factory.newInstance();
                    requestDocument.addNewRequestResource().set(r);
                    response.getEdxlDeResponse().setDigest(
                        processRequestResource(requestDocument).getDigest());
                    response.getEdxlDeResponse().setErrorExists(false);
                } else if (c.getObject().schemaType().getOuterType().getDocumentElementName() == COMMIT_RESOURCE_QNAME) {
                    logger.info("Processing: " + COMMIT_RESOURCE_QNAME);
                    final CommitResource r = (CommitResource) c.getObject();
                    final CommitResourceDocument commitDocument = CommitResourceDocument.Factory.newInstance();
                    commitDocument.addNewCommitResource().set(r);
                    response.getEdxlDeResponse().setDigest(
                        processCommitResource(commitDocument).getDigest());
                    response.getEdxlDeResponse().setErrorExists(false);

                }
            } else {
                response.getEdxlDeResponse().setErrorExists(true);
                response.getEdxlDeResponse().setErrorString("EDXL-RM Content Missing");
            }

            c.dispose();

            // send the message
            sendEdxlDeMessage(edxl);

            return response;
        } else
            return null;
    }

    private String getCommitStatusProperty(x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument.CommitResource.ResourceInformation info) {

        if (info.getResponseInformation().getResponseType().equals(ResponseTypeType.ACCEPT))
            return "Comitted";
        else if (info.getResponseInformation().getResponseType().equals(ResponseTypeType.DECLINE))
            return "Declined";
        else if (info.getResponseInformation().getResponseType().equals(
            ResponseTypeType.PROVISIONAL))
            return "Provisional";
        return "Unknown";
    }

    /**
     * Gets the committed resources workProduct array.
     *
     * @param incidentID
     *            the incident id
     *
     * @return the committed resources
     * @ssdd
     */
    @Override
    public WorkProduct[] getCommittedResources(String incidentID) {

        /*
         * Get a list of WP by Type from Work Product
         */
        try {
            final List<WorkProduct> listOfProducts = getWorkProductService().getProductByTypeAndXQuery(
                COMMIT_RESOURCE_PRODUCT_TYPE, null, null);
            if (listOfProducts != null && listOfProducts.size() > 0) {
                final WorkProduct[] products = new WorkProduct[listOfProducts.size()];
                return listOfProducts.toArray(products);
            }
        } catch (final InvalidXpathException e) {
            logger.error("getCommittedResources: " + e.getMessage());
        }
        return null;
    }

    public CommunicationsService getCommunicationsService() {

        return communicationsService;
    }

    public DirectoryService getDirectoryService() {

        return directoryService;
    }

    /**
     * @return the getIconConfigXmlFilePath
     */
    public String getIconConfigXmlFilePath() {

        return iconConfigXmlFilePath;
    }

    private LocationType getLocationFromContactInformation(EMDigestHelper digest,
                                                           x0.oasisNamesTcEmergencyEDXLRM1.LocationType location) {

        final LocationType digestLoc = LocationType.Factory.newInstance();
        digestLoc.setId(UUIDUtil.getID("Location"));

        // Add any location description to the descriptor
        if (location.getLocationDescription() != null)
            digestLoc.addNewDescriptor().setStringValue(location.getLocationDescription());
        // Handle target area
        if (location.getTargetArea() != null) {
            if (location.getTargetArea().getPoint() != null) {
                final PointDocument point = PointDocument.Factory.newInstance();
                point.addNewPoint().addNewPoint();
                point.getPoint().getPoint().setId(UUIDUtil.getID("point"));
                point.getPoint().getPoint().addNewPos().setStringValue(
                    location.getTargetArea().getPoint().getPos().getStringValue());
                point.getPoint().getPoint().getPos().setSrsName(EMGeoUtil.EPSG4326);
                digestLoc.addNewGeoLocation().set(point);
            }
        } else if (location.getAddress() != null) {
            final AddressType a = location.getAddress();
            final PostalAddress postalAddress = digestAddressType(location.getAddress());
            digestLoc.addNewPhysicalAddress().setPostalAddress(postalAddress);
        }

        digest.setLocation(digestLoc);

        return digestLoc;
    }

    private String getMessageBody(EDXLDistributionDocument edxlDoc) {

        final StringBuffer body = new StringBuffer();
        if (edxlDoc.getEDXLDistribution() != null) {
            body.append("EDXL-RM message received from ");
            if (edxlDoc.getEDXLDistribution().getSenderID() != null)
                body.append(edxlDoc.getEDXLDistribution().getSenderID());
            else
                body.append("UICDS");
            body.append(NEW_LINE);
            if (edxlDoc.getEDXLDistribution().getDateTimeSent() != null) {
                body.append("Sent at ");
                body.append(edxlDoc.getEDXLDistribution().getDateTimeSent().toString());
                body.append(NEW_LINE);
            }
            if (edxlDoc.getEDXLDistribution().sizeOfContentObjectArray() > 0) {
                body.append("Content element descriptions: ");
                body.append(NEW_LINE);
                for (final ContentObjectType content : edxlDoc.getEDXLDistribution().getContentObjectArray())
                    if (content.getContentDescription() != null) {
                        body.append("Content Description: ");
                        body.append(content.getContentDescription());
                        body.append(NEW_LINE);
                    }
            }
        }
        return body.toString();
    }

    public NotificationService getNotificationService() {

        return notificationService;
    }

    private OrganizationType getOrganizationFromContactInformation(EMDigestHelper digest,
                                                                   ContactInformationType contact) {

        final OrganizationType org = OrganizationType.Factory.newInstance();
        final String orgID = UUIDUtil.getID("Organization");
        org.setId(orgID);

        if (contact.getContactDescription() != null)
            org.addNewDescriptor().setStringValue(contact.getContactDescription());

        digest.setOrganization(org);

        final WhatType ucoreWhat = WhatType.Factory.newInstance();
        ucoreWhat.setCodespace(InfrastructureNamespaces.NS_UCORE_CODESPACE);
        ucoreWhat.setCode(DigestConstant.S_Organization);
        digest.setWhatForEvent(ucoreWhat, orgID);

        return org;
    }

    private HashSet<String> getRecipientCores(EDXLDistribution edxl) {

        final HashSet<String> cores = new HashSet<String>();
        if (edxl.sizeOfExplicitAddressArray() > 0)
            // Find core name for each explicit address.
            for (final ValueSchemeType type : edxl.getExplicitAddressArray())
                if (type.getExplicitAddressScheme().equals(
                    CommunicationsService.UICDSCoreAddressScheme))
                    for (final String address : type.getExplicitAddressValueArray())
                        cores.add(address);
        return cores;
    }

    /**
     * Gets the requested resources workProduct array.
     *
     * @param incidentID
     *            the incident id
     *
     * @return the requested resources
     * @ssdd
     */
    @Override
    public WorkProduct[] getRequestedResources(String incidentID) {

        /*
         * Get a list of WP by Type from Work Product
         */
        try {
            final List<WorkProduct> listOfProducts = getWorkProductService().getProductByTypeAndXQuery(
                REQUEST_RESOURCE_PRODUCT_TYPE, null, null);
            if (listOfProducts != null && listOfProducts.size() > 0) {
                final WorkProduct[] products = new WorkProduct[listOfProducts.size()];
                return listOfProducts.toArray(products);
            }
        } catch (final InvalidXpathException e) {
            logger.error("getRequestResources: " + e.getMessage());
        }

        return null;
    }

    private String getRequestStatus(RequestResource requestResource) {

        String status = "Open";
        if (requestResource.getMessageRecall() != null)
            if (requestResource.getMessageRecall().getRecallType() == RecallTypeType.CANCEL)
                status = "Closed";
        return status;
    }

    private String getResourceID(ResourceInformation resourceInfo, String originatingMessageID) {

        final StringBuffer sb = new StringBuffer();
        sb.append(originatingMessageID);
        sb.append("-");
        sb.append(resourceInfo.getResourceInfoElementID());
        sb.append("-");
        if (resourceInfo.getResource() != null &&
            resourceInfo.getResource().getResourceID() != null)
            sb.append(resourceInfo.getResource().getResourceID());
        return sb.toString();
    }

    public WorkProductService getWorkProductService() {

        return workProductService;
    }

    /**
     * @return the xsltFilePath
     */
    public String getXsltFilePath() {

        return xsltFilePath;
    }

    // private void addContactLocationSimpleProperty(EventType event, RequestResource
    // requestResource) {
    //
    // String codespace =
    // requestResource.schemaType().getOuterType().getDocumentElementName().getNamespaceURI();
    // String label = "Requester Location";
    // String code = null;
    // String value = null;
    // if (requestResource.sizeOfContactInformationArray() > 0) {
    // ContactInformationType contact = requestResource.getContactInformationArray(0);
    // if (contact.getContactLocation().getAddress() != null) {
    // code = "ContactLocation.Address";
    // // TODO: create a location element and relationship for
    // // addresses
    // value = contact.getContactLocation().getAddress().getLabel();
    // } else if (contact.getContactLocation().getLocationDescription() != null) {
    // code = "ContactLocation.Description";
    // value = contact.getContactLocation().getLocationDescription();
    // } else if (contact.getContactLocation().getTargetArea() != null) {
    // code = "ContactLocation.TargetArea";
    // // TODO: create a location element and relationship for target
    // // areas
    // value = contact.getContactLocation().getTargetArea().getFeaturetypetag();
    // }
    // }
    // addSimpleProperty(event, codespace, code, label, value);
    // }

    @PostConstruct
    public void init() {

    }

    private DigestDocument processCommitResource(CommitResourceDocument commit)
        throws IllegalArgumentException, EmptyCoreNameListException, SendMessageErrorException,
        LocalCoreNotOnlineException {

        // Create the work product id from the message identifiers
        final StringBuffer wpIDBuffer = new StringBuffer();
        wpIDBuffer.append(commit.getCommitResource().getMessageContentType().toString());
        wpIDBuffer.append("-");
        wpIDBuffer.append(commit.getCommitResource().getOriginatingMessageID());

        // Create a work product
        final WorkProduct workProduct = new WorkProduct();
        workProduct.setProductType(commit.getCommitResource().getMessageContentType().toString());
        workProduct.setProduct(commit);

        if (commit.getCommitResource().sizeOfIncidentInformationArray() > 0)
            workProduct.associateInterestGroup(commit.getCommitResource().getIncidentInformationArray(
                0).getIncidentID());

        // System.out.println("commit resource WP="+commit);

        // Create the digest using XSLT
        // if (xsltFilePath == null)
        xsltFilePath = "xslt/CommitResourceDigest.xsl";
        if (iconConfigXmlFilePath == null)
            iconConfigXmlFilePath = "xml/types_icons.xml";
        digestGenerator = new DigestGenerator(xsltFilePath, iconConfigXmlFilePath);
        final DigestDocument digestDoc = digestGenerator.createDigest(commit);
        // System.out.println("CommitResourceDigest="+digestDoc);
        workProduct.setDigest(digestDoc);

        // create the digest
        // DigestDocument digest = createCommitResourceDigest(commit);
        // System.out.println("CommitResourceDigest="+digest);
        // workProduct.setDigest(digest);
        // System.out.println(WorkProductHelper.toWorkProductDocument(workProduct));

        // publish the work product
        final ProductPublicationStatus status = workProductService.publishProduct(workProduct);

        return digestDoc;
        // return digest;
    }

    private DigestDocument processRequestResource(RequestResourceDocument request)
        throws IllegalArgumentException, EmptyCoreNameListException, SendMessageErrorException,
        LocalCoreNotOnlineException {

        // Create the work product id from the message identifiers
        final StringBuffer wpIDBuffer = new StringBuffer();
        wpIDBuffer.append(request.getRequestResource().getMessageContentType().toString());
        wpIDBuffer.append("-");
        wpIDBuffer.append(request.getRequestResource().getOriginatingMessageID());

        // Create a work product
        final WorkProduct workProduct = new WorkProduct();
        workProduct.setProductType(request.getRequestResource().getMessageContentType().toString());
        workProduct.setProduct(request);

        if (request.getRequestResource().sizeOfIncidentInformationArray() > 0)
            workProduct.associateInterestGroup(request.getRequestResource().getIncidentInformationArray(
                0).getIncidentID());

        // Create the digest using XSLT
        // if (xsltFilePath == null)
        xsltFilePath = "xslt/RequestResourceDigest.xsl";
        if (iconConfigXmlFilePath == null)
            iconConfigXmlFilePath = "xml/types_icons.xml";
        digestGenerator = new DigestGenerator(xsltFilePath, iconConfigXmlFilePath);
        final DigestDocument digestDoc = digestGenerator.createDigest(request);
        workProduct.setDigest(digestDoc);

        // create the digest
        // DigestDocument digest = createRequestResourceDigest(request);
        // workProduct.setDigest(digest);
        // System.out.println(WorkProductHelper.toWorkProductDocument(workProduct));

        // publish the work product
        final ProductPublicationStatus status = workProductService.publishProduct(workProduct);

        // return digest;
        return digestDoc;
    }

    /**
     * Resource message notification handler.
     *
     * @param message
     *            the message
     * @ssdd
     */
    @Override
    public void resourceMessageNotificationHandler(Core2CoreMessage message) {

        logger.debug("resourceMessageNotificationHandler: received messagefrom " +
                     message.getFromCore());
        // =[" + message.getMessage()+ "]

        XmlObject xmlObj;
        try {

            final EDXLDistributionDocument edxlDoc = EDXLDistributionDocument.Factory.parse(message.getMessage());

            if (edxlDoc.getEDXLDistribution().sizeOfExplicitAddressArray() > 0)
                // Find core name for each explicit address.
                for (final ValueSchemeType type : edxlDoc.getEDXLDistribution().getExplicitAddressArray())
                    if (type.getExplicitAddressScheme().equals(
                        CommunicationsService.UICDSExplicitAddressScheme))
                        for (final String address : type.getExplicitAddressValueArray()) {
                            xmlObj = XmlObject.Factory.parse(edxlDoc.toString());
                            // log.debug("broadcastMessageNotificationHandler: sending notification ["
                            // + xmlObj.toString() + "]  to " + address);
                            sendMessageNotification(xmlObj, address);
                        }

        } catch (final Throwable e) {
            logger.error("resourceMessageNotificationHandler: Error parsing message - not a valid XML string");
            throw new IllegalArgumentException("Message is not a valid XML string");
        }
    }

    private void sendEdxlDeMessage(EDXLDistribution edxl) throws IllegalArgumentException,
        EmptyCoreNameListException, SendMessageErrorException, LocalCoreNotOnlineException,
        NoShareAgreementException, NoShareRuleInAgreementException {

        final HashSet<String> cores = BroadcastUtil.getCoreList(edxl);
        final HashSet<String> jids = BroadcastUtil.getJidList(edxl);

        // Send the message to each core that has a user in an explictAddress
        // element
        if (cores.size() == 0 && jids.size() == 0)
            return;
        else {
            SendMessageErrorException errorException = new SendMessageErrorException();
            final EDXLDistributionDocument edxlDoc = EDXLDistributionDocument.Factory.newInstance();
            edxlDoc.setEDXLDistribution(edxl);

            // log.error("in sendEdxlDeMessage() edxlDoc=" + edxlDoc.toString());
            // log.error("in sendEdxlDeMessage() edxl=" + edxl.toString());

            // Send the message to the cores as a Broadcast Service message
            errorException = sendMessageToCores(cores, errorException, edxlDoc.xmlText());

            // Send the message to any external XMPP addresses
            errorException = sendXMPPMessage(jids, errorException, edxlDoc);

            if (errorException.getErrors().size() > 0)
                throw errorException;
        }
    }

    private void sendMessageNotification(XmlObject xmlObj, String address) {

        final ArrayList<NotificationMessageHolderType> messages = new ArrayList<NotificationMessageHolderType>();

        final NotificationMessageHolderType t = NotificationMessageHolderType.Factory.newInstance();
        final NotificationMessageHolderType.Message m = t.addNewMessage();

        try {
            m.set(xmlObj);
            messages.add(t);

            NotificationMessageHolderType[] notification = new NotificationMessageHolderType[messages.size()];

            notification = messages.toArray(notification);
            logger.debug("===> sending Core2Core message: array size=" + notification.length);
            notificationService.notify(StringUtil.getSubmitterResourceInstanceName(address),
                notification);
        } catch (final Throwable e) {
            logger.error("productPublicationStatusNotificationHandler: error creating and sending  Core2Core message  notification to " +
                         address);
            e.printStackTrace();
        }
    }

    private SendMessageErrorException sendMessageToCores(HashSet<String> cores,
                                                         SendMessageErrorException errorException,
                                                         String msgStr) {

        for (final String core : cores) {
            logger.info("sendMessage to: " + core);
            try {

                communicationsService.sendMessage(msgStr,
                    CommunicationsService.CORE2CORE_MESSAGE_TYPE.RESOURCE_MESSAGE, core);
                logger.debug("called communicationsService.sendMessage");
            } catch (final RemoteCoreUnknownException e1) {
                errorException.getErrors().put(core,
                    SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.CORE_UNKNOWN);
            } catch (final RemoteCoreUnavailableException e2) {
                errorException.getErrors().put(core,
                    SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.CORE_UNAVAILABLE);
            } catch (final LocalCoreNotOnlineException e) {
                // TODO: this short circuit for the local core should be in the
                // CommunicationService
                logger.info("Sending to local core");
                final Core2CoreMessage message = new Core2CoreMessage();

                message.setFromCore(core);
                message.setToCore(core);
                message.setMessageType(CommunicationsService.CORE2CORE_MESSAGE_TYPE.RESOURCE_MESSAGE.toString());
                // Core2CoreMessageDocument doc =
                // Core2CoreMessageDocument.Factory.newInstance();
                // doc.addNewCore2CoreMessage().set(edxl);
                // message.setMessage(doc.toString());
                // EDXLDistributionDocument doc = EDXLDistributionDocument.Factory.newInstance();
                // doc.setEDXLDistribution(edxl);
                message.setMessage(msgStr);
                resourceMessageNotificationHandler(message);
                // communicationsService.core2CoreMessageNotificationHandler(message);
            } catch (final NoShareAgreementException e) {
                errorException.getErrors().put(core,
                    SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.NO_SHARE_AGREEMENT);
            } catch (final NoShareRuleInAgreementException e) {
                errorException.getErrors().put(core,
                    SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.NO_SHARE_RULE_IN_AGREEMENT);
            }
        }
        return errorException;
    }

    private SendMessageErrorException sendXMPPMessage(HashSet<String> jids,
                                                      SendMessageErrorException errorException,
                                                      EDXLDistributionDocument edxlDoc)
        throws NoShareAgreementException, NoShareRuleInAgreementException,
        LocalCoreNotOnlineException {

        for (final String jid : jids)
            // System.out.println("sending to " + jid);
            communicationsService.sendXMPPMessage(getMessageBody(edxlDoc), null, edxlDoc.xmlText(),
                jid);
        return errorException;
    }

    public void setCommunicationsService(CommunicationsService communicationsService) {

        this.communicationsService = communicationsService;
    }

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

    public void setNotificationService(NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    public void setWorkProductService(WorkProductService service) {

        workProductService = service;
    }

    /**
     * @param xsltFilePath
     *            the xsltFilePath to set
     */
    public void setXsltFilePath(String xsltFilePath) {

        this.xsltFilePath = xsltFilePath;
    }

    @Override
    public void systemInitializedHandler(String messgae) {

        logger.debug("systemInitializedHandler: ... start ...");
        final WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(COMMIT_RESOURCE_PRODUCT_TYPE);
        typeList.addProductType(REQUEST_RESOURCE_PRODUCT_TYPE);
        directoryService.registerUICDSService(NS_ResourceManagementService, RESOURCE_SERVICE_NAME,
            typeList, typeList);
        logger.debug("systemInitializedHandler: ... done ...");
    }

}
