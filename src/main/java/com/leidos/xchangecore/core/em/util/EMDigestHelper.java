package com.leidos.xchangecore.core.em.util;

import gov.niem.niem.iso3166.x20.CountryAlpha2CodeType;
import gov.niem.niem.niemCore.x20.AddressType;
import gov.niem.niem.niemCore.x20.AreaType;
import gov.niem.niem.niemCore.x20.ContactInformationType;
import gov.niem.niem.niemCore.x20.OrganizationType;
import gov.niem.niem.niemCore.x20.StreetType;
import gov.niem.niem.niemCore.x20.StructuredAddressType;
import gov.niem.niem.niemCore.x20.TextType;
import gov.niem.niem.niemCore.x20.TwoDimensionalGeographicCoordinateType;
import gov.niem.niem.uspsStates.x20.USStateCodeType;
import gov.ucore.ucore.x20.EntityType;
import gov.ucore.ucore.x20.LocationType;
import gov.ucore.ucore.x20.StringType;
import gov.ucore.ucore.x20.WhatType;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import mil.dod.metadata.mdr.ns.ddms.x20.CompoundCountryCodeIdentifierType;
import mil.dod.metadata.mdr.ns.ddms.x20.PostalAddressDocument.PostalAddress;
import mil.dod.metadata.mdr.ns.ddms.x20.VirtualCoverageType;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.iapService.IncidentActionPlanType;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.sensorService.SensorInfoDocument.SensorInfo;
import org.uicds.sensorService.SensorObservationInfoDocument.SensorObservationInfo;

import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert.Info;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert.Info.Area;

import com.leidos.xchangecore.core.infrastructure.util.DigestConstant;
import com.leidos.xchangecore.core.infrastructure.util.DigestHelper;
import com.leidos.xchangecore.core.infrastructure.util.InfrastructureNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.UUIDUtil;
import com.usersmarts.geo.gml.GMLDomModule;
import com.usersmarts.xmf2.Configuration;

public class EMDigestHelper
    extends DigestHelper {

    // private DigestDocument digest;

    private static Configuration gmlParseCfg = new Configuration(GMLDomModule.class);

    Logger log = LoggerFactory.getLogger(this.getClass());

    public EMDigestHelper() {

        super();
        // digest = DigestDocument.Factory.newInstance();
        // digest.addNewDigest();
    }

    public EMDigestHelper(SensorObservationInfo soi) {

        this();

        SensorInfo[] infos = soi.getSensorInfoArray();

        for (SensorInfo info : infos) {

            // set the event's description
            String eventId = UUIDUtil.getID(DigestConstant.S_ObservationEvent);

            // set the descriptor
            String description = info.getDescription();

            setEvent(eventId, description, info.getName(), new String[] {
                null, null, "Observation"
            }, null, null);

            WhatType what = WhatType.Factory.newInstance();
            what.setCodespace(InfrastructureNamespaces.NS_UCORE_CODESPACE);
            what.setCode(DigestConstant.S_ObservationEvent);
            setWhatForEvent(what, eventId);

            LocationType location = LocationType.Factory.newInstance();
            location.setId(UUIDUtil.getID(DigestConstant.S_Location));

            // set point location
            Double lat = new Double(info.getLatitude());
            Double lon = new Double(info.getLongitude());

            if ((!lat.isNaN()) && (!lon.isNaN())) {

                net.opengis.gml.x32.PointType point = net.opengis.gml.x32.PointType.Factory.newInstance();
                net.opengis.gml.x32.DirectPositionType pos = net.opengis.gml.x32.DirectPositionType.Factory.newInstance();

                // the id attribute is needed for OpenGIS/GML/Point
                point.setId(UUIDUtil.getID("Point"));

                BigInteger dimension = BigInteger.valueOf(2);
                pos.setSrsDimension(dimension);
                pos.setStringValue(info.getLatitude() + " " + info.getLongitude());
                point.setPos(pos);

                addPointToLocation(location, point);
            }

            // set the SOI URN
            VirtualCoverageType coverage = VirtualCoverageType.Factory.newInstance();
            coverage.setProtocol("IP");
            coverage.setAddress(soi.getSosURN());

            location.addNewCyberAddress().setVirtualCoverage(coverage);

            this.setLocation(location);
            setLocatedAt(eventId, location.getId(), null);
        }
    }

    public EMDigestHelper(Alert alert) {

        this();

        Info[] infos = alert.getInfoArray();

        for (Info info : infos) {
            // set the event's description
            String eventId = UUIDUtil.getID(DigestConstant.S_Event);

            // set the descriptor
            String description = info.getDescription();

            // How to determine the Digest.Event.Identifier
            // 1) Alert.Info.Event
            // 2) Alert.Info.Description
            // 3) "CAP"
            String id = alert.getInfoArray(0).getEvent();
            if (id == null) {
                id = alert.getInfoArray(0).getDescription();
                if (id == null) {
                    id = "CAP";
                }
            }
            log.debug("Event: identifier: Alert - " + id);

            // setEvent(eventId, description, alert.getIdentifier(), new String[] {
            setEvent(eventId, description, "Alert - " + id, new String[] {
                InfrastructureNamespaces.NS_CAP, "identifier"
            }, null, null);

            WhatType what = WhatType.Factory.newInstance();
            what.setCodespace(InfrastructureNamespaces.NS_UCORE_CODESPACE);
            what.setCode(DigestConstant.S_AlertEvent);
            setWhatForEvent(what, eventId);

            what = WhatType.Factory.newInstance();
            what.setCodespace(InfrastructureNamespaces.NS_CAP);
            what.setCode(info.getCategoryArray(0).toString());
            setWhatForEvent(what, eventId);

            // the time occured
            Calendar theTime = alert.getSent();
            if (theTime == null)
                theTime = Calendar.getInstance();

            Area[] areas = info.getAreaArray();
            for (Area area : areas) {

                LocationType location = LocationType.Factory.newInstance();
                location.setId(UUIDUtil.getID(DigestConstant.S_Location));

                if (area.sizeOfCircleArray() > 0) {
                    net.opengis.gml.x32.CircleByCenterPointType circle = EMGeoUtil.getCircle(null,
                        area.getCircleArray(0));
                    if (circle != null) {
                        setCircle(location, circle);
                        setOccursAt(eventId, location.getId(), theTime);
                    }
                }

                if (area.sizeOfPolygonArray() > 0) {
                    for (String polygonsString : area.getPolygonArray()) {
                        net.opengis.gml.x32.PolygonType polygon = EMGeoUtil.getPolygon(null,
                            polygonsString);
                        if (polygon != null) {
                            setPolygon(location, polygon);
                            setOccursAt(eventId, location.getId(), theTime);
                        }
                    }
                }
            }
        }

    }

    public EMDigestHelper(UICDSIncidentType incident) {

        this();

        if (incident.sizeOfIncidentJurisdictionalOrganizationArray() > 0) {
            // parse the organizations
            OrganizationType[] organizations = incident.getIncidentJurisdictionalOrganizationArray();
            for (OrganizationType organization : organizations) {
                // add the organization
                gov.ucore.ucore.x20.OrganizationType org = gov.ucore.ucore.x20.OrganizationType.Factory.newInstance();

                // everyone has to have an ID
                org.setId(UUIDUtil.getID(S_Organization));

                // setup what for Organization
                org.addNewWhat().setCode(S_Organization);
                org.getWhatArray(0).setCodespace(NS_UCORE_CODESPACE);

                // set the Name
                if (organization.sizeOfOrganizationNameArray() > 0) {
                    TextType[] names = organization.getOrganizationNameArray();
                    int i = 0;
                    for (TextType name : names) {
                        if (i == 0) {
                            org.addNewName().addNewValue().setStringValue(name.getStringValue());
                            i++;
                        } else {
                            org.addNewAlternateName().addNewValue().setStringValue(name.getStringValue());
                        }
                    }
                }

                // set ContactInfo
                if (organization.sizeOfOrganizationPrimaryContactInformationArray() > 0) {
                    org.addNewContactInfo();
                    ContactInformationType[] contacts = organization.getOrganizationPrimaryContactInformationArray();
                    for (ContactInformationType contact : contacts) {
                        String contactText = contact.xmlText(new XmlOptions().setSavePrettyPrint());
                        if (contactText.indexOf("ContactEmailID") != -1) {
                            org.getContactInfo().addEmail(contactText);
                        } else if (contactText.indexOf("ContactTelephoneNumber") != -1) {
                            org.getContactInfo().addPhone(contactText);
                        }
                    }
                }

                setOrganization(org);
            }
        }

        // set the event's description
        String eventId = UUIDUtil.getID(DigestConstant.S_Event);

        // use the ActivityDescription as the Event's descriptor
        String activityDescription = null;
        if (incident.sizeOfActivityDescriptionTextArray() > 0) {
            activityDescription = incident.getActivityDescriptionTextArray(0).getStringValue();
        } else {
            activityDescription = "UICDS Incident";
        }

        // use ActivityName as the incident identifier
        String activityName = null;
        if (incident.sizeOfActivityNameArray() > 0) {
            activityName = incident.getActivityNameArray(0).getStringValue();
        } else {
            activityName = "UICDS Incident";
        }

        setEvent(eventId, activityDescription, activityName, new String[] {
            InfrastructureNamespaces.NS_NIEM_CORE, "ActivityName"
        }, null, null);

        // map either the CAP's category or the UCore's event type to represent the Event's what
        WhatType what = WhatType.Factory.newInstance();
        what.setCodespace(InfrastructureNamespaces.NS_UCORE_CODESPACE);
        if (incident.sizeOfActivityCategoryTextArray() > 0 &&
            incident.getActivityCategoryTextArray(0) != null &&
            incident.getActivityCategoryTextArray(0).getStringValue() != null) {
            what.setCode(incident.getActivityCategoryTextArray(0).getStringValue());
        } else {
            what.setCode("Event");
        }
        setWhatForEvent(what, eventId);

        // use the ActivityDateRepresentation as the time
        Calendar theCalendar = Calendar.getInstance();
        if (incident.sizeOfActivityDateRepresentationArray() > 0) {
            String calendarString = incident.getActivityDateRepresentationArray(0).newCursor().getTextValue();
            if (calendarString != null && calendarString.length() > 0) {
                SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                TimeZone timeZone = TimeZone.getDefault();
                ISO8601Local.setTimeZone(timeZone);
                try {
                    Date dateTime = (Date) ISO8601Local.parse(calendarString.trim());
                    theCalendar.setTime(dateTime);
                } catch (ParseException e) {
                    log.error("Error parsing date string should be yyyy-MM-dd'T'HH:mm:ss format: " +
                              e.getMessage());
                }
            }
        }

        // create the OccursAt
        gov.niem.niem.niemCore.x20.LocationType[] locations = incident.getIncidentLocationArray();

        for (gov.niem.niem.niemCore.x20.LocationType location : locations) {

            gov.ucore.ucore.x20.LocationType uLocation = gov.ucore.ucore.x20.LocationType.Factory.newInstance();
            uLocation.setId(UUIDUtil.getID(DigestConstant.S_Location));

            // Process location Areas
            if (location.sizeOfLocationAreaArray() > 0) {
                AreaType[] areas = location.getLocationAreaArray();
                for (AreaType area : areas) {
                    if (area.sizeOfAreaPolygonGeographicCoordinateArray() > 0) {
                        TwoDimensionalGeographicCoordinateType[] coords = area.getAreaPolygonGeographicCoordinateArray();
                        net.opengis.gml.x32.PolygonType polygon = EMGeoUtil.getPolygon(coords);
                        addPolygonToLocation(uLocation, polygon);
                    }
                    if (area.sizeOfAreaCircularRegionArray() > 0) {
                        net.opengis.gml.x32.CircleByCenterPointType circle = EMGeoUtil.getCircle(area.getAreaCircularRegionArray(0));
                        addCircleToLocation(uLocation, circle);
                    }
                }
            }

            // Process location addresses
            if (location.sizeOfLocationAddressArray() > 0) {
                AddressType[] addresses = location.getLocationAddressArray();
                for (AddressType address : addresses) {
                    if (address.sizeOfAddressRepresentationArray() > 0) {
                        for (XmlObject add : address.getAddressRepresentationArray()) {
                            if (add instanceof StructuredAddressType) {
                                addAddressToLocation(uLocation, (StructuredAddressType) add);
                            } else if (add instanceof TextType) {
                                addAddressTextToLocation(uLocation, (TextType) add);
                            }
                        }
                    }
                }
            }
            if (uLocation.sizeOfGeoLocationArray() > 0 ||
                uLocation.sizeOfPhysicalAddressArray() > 0) {
                setLocation(uLocation);
                setOccursAt(eventId, uLocation.getId(), theCalendar);
            }
            // System.out.println(uLocation);
        }
    }

    private void addAddressTextToLocation(LocationType uLocation, TextType add) {

        uLocation.addNewPhysicalAddress().addNewPostalAddress().addStreet(add.getStringValue());
        log.debug("set PhysicalAddress.PostAddress.Streeet: " + add.getStringValue());
    }

    public EMDigestHelper(IncidentActionPlanType iap, String wpid, boolean activated) {

        this();

        // set the entity
        EntityType entity = EntityType.Factory.newInstance();
        entity.setId(wpid);

        // Set descriptor as resource name
        if (iap.getName() != null) {
            StringType t = StringType.Factory.newInstance();
            t.setStringValue(iap.getName());
            entity.setDescriptor(t);
        }

        // add the status based on MessageRecall value
        if (activated) {
            addSimplePropertyToThing(entity,
                UICDS_EVENT_STATUS_CODESPACE,
                "Approved",
                "Status",
                null);
        } else {
            addSimplePropertyToThing(entity, UICDS_EVENT_STATUS_CODESPACE, "Draft", "Status", null);
        }

        // Set the event
        setEntity(entity);

        WhatType ucoreWhat = WhatType.Factory.newInstance();
        ucoreWhat.setCodespace(InfrastructureNamespaces.NS_UCORE_CODESPACE);
        ucoreWhat.setCode(DigestConstant.S_Document);
        setWhatForEvent(ucoreWhat, wpid);
    }

    private void setAddress(LocationType uLocation, StructuredAddressType address) {

        addAddressToLocation(uLocation, address);
        setLocation(uLocation);
    }

    public void addAddressToLocation(LocationType uLocation, StructuredAddressType address) {

        PostalAddress postalAddress = PostalAddress.Factory.newInstance();

        // country codes should be ISO 3166-1
        // (http://www.iso.org/iso/english_country_names_and_code_elements)
        if (address.sizeOfLocationCountryArray() > 0) {
            XmlObject o = address.getLocationCountryArray(0);
            // System.out.println(address);
            if (o instanceof CountryAlpha2CodeType) {
                CountryAlpha2CodeType code = (CountryAlpha2CodeType) o;
                CompoundCountryCodeIdentifierType countryCode = CompoundCountryCodeIdentifierType.Factory.newInstance();
                countryCode.setValue(code.getStringValue());
                countryCode.setQualifier("ISO 3166-1");
                postalAddress.setCountryCode(countryCode);
            }
        }
        if (address.sizeOfLocationStateArray() > 0) {
            XmlObject o = address.getLocationStateArray(0);
            if (o instanceof USStateCodeType) {
                postalAddress.setState(((USStateCodeType) o).getStringValue());
            }
        }
        if (address.sizeOfLocationCityNameArray() > 0) {
            postalAddress.setCity(address.getLocationCityNameArray(0).getStringValue());
        }
        if (address.sizeOfAddressDeliveryPointArray() > 0) {
            String streetStr = "";
            if (address.getAddressDeliveryPointArray(0) instanceof StreetType) {
                StreetType street = (StreetType) address.getAddressDeliveryPointArray(0);
                if (street.sizeOfStreetNumberTextArray() > 0) {
                    streetStr = street.getStreetNumberTextArray(0).getStringValue() + " ";
                }
                if (street.sizeOfStreetNameArray() > 0) {
                    streetStr = streetStr + street.getStreetNameArray(0).getStringValue();
                }
                postalAddress.addStreet(streetStr);
            }
        }
        if (address.sizeOfLocationPostalCodeArray() > 0) {
            postalAddress.setPostalCode(address.getLocationPostalCodeArray(0).getStringValue());
        }

        uLocation.addNewPhysicalAddress().setPostalAddress(postalAddress);
    }
}
