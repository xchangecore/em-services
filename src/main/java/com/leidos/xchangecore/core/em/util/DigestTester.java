package com.leidos.xchangecore.core.em.util;

import gov.ucore.ucore.x20.AgentEventRelationshipType;
import gov.ucore.ucore.x20.CauseOfRelationshipType;
import gov.ucore.ucore.x20.CircleByCenterPointType;
import gov.ucore.ucore.x20.DigestType;
import gov.ucore.ucore.x20.EntityLocationExtendedRelationshipType;
import gov.ucore.ucore.x20.EntityLocationRelationshipType;
import gov.ucore.ucore.x20.EntityType;
import gov.ucore.ucore.x20.EventLocationRelationshipType;
import gov.ucore.ucore.x20.EventType;
import gov.ucore.ucore.x20.GeoLocationType;
import gov.ucore.ucore.x20.LocationType;
import gov.ucore.ucore.x20.OrganizationType;
import gov.ucore.ucore.x20.PhysicalAddressType;
import gov.ucore.ucore.x20.PolygonType;
import gov.ucore.ucore.x20.SimplePropertyType;
import gov.ucore.ucore.x20.ThingType;

import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;

import com.leidos.xchangecore.core.infrastructure.util.InfrastructureNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.XmlUtil;

public class DigestTester
    implements InfrastructureNamespaces {

    public static final String OCCURS_AT_TYPE = "OccursAt";
    public static final String ENTITY_THING_TYPE = "Entity";
    public static final String EVENT_THING_TYPE = "Event";
    public static final String ORGANIZATION_THING_TYPE = "Organization";
    public static final String HAS_DESTINATION_TYPE = "HasDestinationOf";
    public static final String LOCATED_AT_TYPE = "LocatedAt";
    public static final String CAUSE_OF_TYPE = "CauseOf";
    public static final String INVOLVED_IN_TYPE = "InvolvedIn";

    public static EventType getEventElementFromDigest(DigestType digest, Map<String, String> codes) {

        XmlObject object = null;
        XmlObject[] events = digest.selectChildren(EventType.type.getName().getNamespaceURI(),
            EVENT_THING_TYPE);
        for (XmlObject obj : events) {
            boolean found = false;
            for (String codespace : codes.keySet()) {
                if (EMDigestHelper.objectHasWhatType(codespace,
                    codes.get(codespace),
                    null,
                    null,
                    obj)) {
                    object = obj;
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }

        return (EventType) object;
    }

    public static boolean checkLocationGMLPoint(DigestType digest, String pos) {

        boolean foundPointLocation = false;
        List<LocationType> locationElements = EMDigestHelper.getLocationElements(digest);
        for (LocationType location : locationElements) {
            XmlObject[] geoLocations = location.selectChildren(gov.ucore.ucore.x20.GeoLocationType.type.getName().getNamespaceURI(),
                "GeoLocation");
            if (geoLocations != null && geoLocations.length > 0) {
                for (XmlObject object : geoLocations) {
                    GeoLocationType geo = (GeoLocationType) object;
                    XmlObject[] UcorePoint = geo.selectChildren(gov.ucore.ucore.x20.PointType.type.getName().getNamespaceURI(),
                        "Point");
                    if (UcorePoint.length > 0) {
                        gov.ucore.ucore.x20.PointType point = (gov.ucore.ucore.x20.PointType) UcorePoint[0];
                        if (point.getPoint().getPos().getStringValue().equals(pos)) {
                            foundPointLocation = true;
                        }
                    }
                }
            }
        }
        return foundPointLocation;
    }

    public static boolean checkLocationGMLPolygon(DigestType digest, List<String> points) {

        boolean foundPolygon = false;
        List<LocationType> locationElements = EMDigestHelper.getLocationElements(digest);
        for (LocationType location : locationElements) {
            XmlObject[] geoLocations = location.selectChildren(gov.ucore.ucore.x20.GeoLocationType.type.getName().getNamespaceURI(),
                "GeoLocation");
            if (geoLocations != null && geoLocations.length > 0) {
                for (XmlObject object : geoLocations) {
                    GeoLocationType geo = (GeoLocationType) object;
                    XmlObject[] ucorePolygons = geo.selectChildren(gov.ucore.ucore.x20.PointType.type.getName().getNamespaceURI(),
                        "Polygon");
                    if (ucorePolygons.length > 0) {
                        for (XmlObject poly : ucorePolygons) {
                            if (poly instanceof PolygonType) {
                                net.opengis.gml.x32.PolygonType polygon = ((PolygonType) poly).getPolygon();
                                if (polygon.getExterior() != null &&
                                    polygon.getExterior().getAbstractRing() != null) {
                                    if (polygon.getExterior().getAbstractRing() instanceof net.opengis.gml.x32.LinearRingType) {
                                        net.opengis.gml.x32.LinearRingType linearRing = (net.opengis.gml.x32.LinearRingType) polygon.getExterior().getAbstractRing();
                                        if (linearRing.sizeOfPosArray() == points.size()) {
                                            foundPolygon = true;
                                            // TODO: check the values
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return foundPolygon;
    }

    public static boolean checkLocationGMLCircle(DigestType digest, String radius, String UOM) {

        boolean foundCircle = false;
        List<LocationType> locationElements = EMDigestHelper.getLocationElements(digest);
        for (LocationType location : locationElements) {
            XmlObject[] geoLocations = location.selectChildren(gov.ucore.ucore.x20.GeoLocationType.type.getName().getNamespaceURI(),
                "GeoLocation");
            if (geoLocations != null && geoLocations.length > 0) {
                for (XmlObject object : geoLocations) {
                    GeoLocationType geo = (GeoLocationType) object;
                    XmlObject[] ucoreCircles = geo.selectChildren(CircleByCenterPointType.type.getName().getNamespaceURI(),
                        "CircleByCenterPoint");
                    if (ucoreCircles.length > 0) {
                        // TODO: should check point value and UOM
                        CircleByCenterPointType circle = (CircleByCenterPointType) ucoreCircles[0];
                        if (circle.getCircleByCenterPoint().getRadius().getStringValue().equals(radius) &&
                            circle.getCircleByCenterPoint().getRadius().getUom().equals(UOM) &&
                            circle.getCircleByCenterPoint().getNumArc() != null) {
                            foundCircle = true;
                        }
                    }
                }
            }
        }
        return foundCircle;
    }

    public static boolean hasAOccursAtElement(DigestType digest, String eventID, String reqLocID) {

        boolean found = false;
        XmlObject[] occursAts = digest.selectChildren(EventLocationRelationshipType.type.getName().getNamespaceURI(),
            DigestTester.OCCURS_AT_TYPE);
        if (occursAts.length > 0) {
            for (XmlObject object : occursAts) {
                EventLocationRelationshipType occursAt = (EventLocationRelationshipType) object;
                if (occursAt.getLocationRef().getRef().size() > 0 &&
                    occursAt.getLocationRef().getRef().size() > 0) {
                    String eventRef = (String) occursAt.getEventRef().getRef().get(0);
                    String locationRef = (String) occursAt.getLocationRef().getRef().get(0);
                    if (eventRef.equals(eventID) && locationRef.equals(reqLocID)) {
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    public static String checkLocationDescriptor(DigestType digest, String locationDescription) {

        String id = null;
        XmlObject[] locations = digest.selectChildren(gov.ucore.ucore.x20.LocationType.type.getName().getNamespaceURI(),
            "Location");
        if (locations.length > 0) {
            for (XmlObject object : locations) {
                LocationType location = (LocationType) object;
                if (location.getDescriptor().getStringValue().equals(locationDescription)) {
                    id = location.getId();
                }
            }
        }
        return id;
    }

    public static boolean checkLocationAddresses(DigestType digest) {

        boolean foundPointLocation = false;
        List<LocationType> locationElements = EMDigestHelper.getLocationElements(digest);
        for (LocationType location : locationElements) {
            XmlObject[] addresses = location.selectChildren(gov.ucore.ucore.x20.PhysicalAddressType.type.getName().getNamespaceURI(),
                "PhysicalAddress");
            if (addresses != null && addresses.length > 0) {
                for (XmlObject object : addresses) {
                    PhysicalAddressType address = (PhysicalAddressType) object;
                    if (address.getPostalAddress() != null) {
                        if (address.getPostalAddress().getCountryCode() != null &&
                            address.getPostalAddress().getCountryCode().getQualifier() != null &&
                            address.getPostalAddress().getCountryCode().getValue() != null &&
                            address.getPostalAddress().getState() != null &&
                            address.getPostalAddress().getCity() != null &&
                            address.getPostalAddress().getPostalCode() != null &&
                            address.getPostalAddress().sizeOfStreetArray() > 0) {
                            foundPointLocation = true;
                        }
                    }
                }
            }
        }
        return foundPointLocation;
    }

    public static OrganizationType getOrganizationElementFromDigest(DigestType digest,
                                                                    String contactDescriptor) {

        OrganizationType organization = null;
        XmlObject[] organizations = digest.selectChildren(OrganizationType.type.getName().getNamespaceURI(),
            ORGANIZATION_THING_TYPE);
        for (XmlObject obj : organizations) {
            OrganizationType org = (OrganizationType) obj;
            if (org.getDescriptor().getStringValue().equals(contactDescriptor)) {
                organization = org;
                break;
            }
        }
        return organization;
    }

    public static OrganizationType getOrganizationElementFromDigestByOrganizationName(DigestType digest,
                                                                                      String organizationName) {

        OrganizationType organization = null;
        XmlObject[] organizations = digest.selectChildren(OrganizationType.type.getName().getNamespaceURI(),
            ORGANIZATION_THING_TYPE);
        for (XmlObject obj : organizations) {
            OrganizationType org = (OrganizationType) obj;
            if (org.getName().getValue().getStringValue().equals(organizationName)) {
                organization = org;
                break;
            }
        }
        return organization;
    }

    public static boolean hasLocatedAtElement(DigestType digest, String id, String locationID) {

        boolean found = false;
        XmlObject[] locatedAts = digest.selectChildren(EntityLocationExtendedRelationshipType.type.getName().getNamespaceURI(),
            LOCATED_AT_TYPE);
        if (locatedAts.length > 0) {
            for (XmlObject object : locatedAts) {
                EntityLocationExtendedRelationshipType locatedAt = (EntityLocationExtendedRelationshipType) object;
                if (locatedAt.getLocationRef().getRef().size() > 0 &&
                    locatedAt.getLocationRef().getRef().size() > 0) {
                    String entityRef = (String) locatedAt.getEntityRef().getRef().get(0);
                    String locationRef = (String) locatedAt.getLocationRef().getRef().get(0);
                    if (entityRef.equals(id) && locationRef.equals(locationID)) {
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    public static boolean hasAHasDestinationOfElement(DigestType digest, String entityID) {

        boolean found = false;
        XmlObject[] hasDestinationOfs = digest.selectChildren(EntityLocationRelationshipType.type.getName().getNamespaceURI(),
            HAS_DESTINATION_TYPE);
        if (hasDestinationOfs.length > 0) {
            for (XmlObject object : hasDestinationOfs) {
                EntityLocationRelationshipType hasDestinationOf = (EntityLocationRelationshipType) object;
                if (hasDestinationOf.getLocationRef().getRef().size() > 0 &&
                    hasDestinationOf.getLocationRef().getRef().size() > 0) {
                    String entityRef = (String) hasDestinationOf.getEntityRef().getRef().get(0);
                    String locationRef = (String) hasDestinationOf.getLocationRef().getRef().get(0);
                    List<LocationType> locationElements = EMDigestHelper.getLocationElements(digest);
                    for (LocationType location : locationElements) {
                        if (entityRef.equals(entityID) && locationRef.equals(location.getId())) {
                            found = true;
                            break;
                        }
                    }
                    if (found)
                        break;
                }
            }
        }
        return found;
    }

    public static boolean hasACauseOfElement(DigestType digest, String causeID, String effectID) {

        boolean found = false;
        XmlObject[] causeOfs = digest.selectChildren(CauseOfRelationshipType.type.getName().getNamespaceURI(),
            CAUSE_OF_TYPE);
        if (causeOfs.length > 0) {
            for (XmlObject object : causeOfs) {
                CauseOfRelationshipType causeOf = (CauseOfRelationshipType) object;
                if (causeOf.getCause().getRef().size() > 0 &&
                    causeOf.getEffect().getRef().size() > 0) {
                    String causeRef = (String) causeOf.getCause().getRef().get(0);
                    String effectRef = (String) causeOf.getEffect().getRef().get(0);
                    if (causeRef.equals(causeID) && effectRef.equals(effectID)) {
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    public static boolean hasInvolvedInElement(DigestType digest, String agentID, String eventID) {

        boolean found = false;
        XmlObject[] involvedIns = digest.selectChildren(AgentEventRelationshipType.type.getName().getNamespaceURI(),
            INVOLVED_IN_TYPE);
        if (involvedIns.length > 0) {
            for (XmlObject object : involvedIns) {
                AgentEventRelationshipType involvedIn = (AgentEventRelationshipType) object;
                if (involvedIn.getAgentRef().getRef().size() > 0 &&
                    involvedIn.getEventRef().getRef().size() > 0) {
                    String agentRef = (String) involvedIn.getAgentRef().getRef().get(0);
                    String eventRef = (String) involvedIn.getEventRef().getRef().get(0);
                    if (agentRef.equals(agentID) && eventRef.equals(eventID)) {
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    public static boolean checkSimpleProperty(DigestType digest,
                                              String thingType,
                                              String eventID,
                                              String codespace,
                                              String code,
                                              String label,
                                              String value) {

        boolean ok = false;
        boolean codespaceOk = false;
        boolean codeOk = false;
        boolean labelOk = false;
        boolean valueOk = false;
        XmlObject[] things = digest.selectChildren(ThingType.type.getName().getNamespaceURI(),
            thingType);
        if (things.length > 0) {
            ThingType thing = null;
            for (XmlObject obj : things) {
                if (((ThingType) obj).getId().equals(eventID)) {
                    thing = (ThingType) obj;
                }
            }
            if (thing != null) {
                XmlObject[] props = thing.selectChildren(SimplePropertyType.type.getName().getNamespaceURI(),
                    "SimpleProperty");
                for (XmlObject prop : props) {
                    // System.out.println(prop);
                    // Find the SimpleProperty with the correct codespace (there may be more than
                    // one)
                    XmlObject codespaceAttr = prop.selectAttribute(SimplePropertyType.type.getName().getNamespaceURI(),
                        "codespace");
                    if (codespaceAttr != null) {
                        // System.out.println(getTextFromAny(codespaceAttr) + " " + codespace);
                        if (XmlUtil.getTextFromAny(codespaceAttr).equals(codespace)) {
                            codespaceOk = true;
                            // Check if it has the correct code value
                            if (code != null) {
                                XmlObject codeAttr = prop.selectAttribute(SimplePropertyType.type.getName().getNamespaceURI(),
                                    "code");
                                if (codeAttr != null &&
                                    XmlUtil.getTextFromAny(codeAttr).equals(code)) {
                                    codeOk = true;
                                }
                            } else {
                                codeOk = true;
                            }
                            // Check if it has the correct label
                            if (label != null) {
                                XmlObject labelAttr = prop.selectAttribute(SimplePropertyType.type.getName().getNamespaceURI(),
                                    "label");
                                if (labelAttr != null &&
                                    XmlUtil.getTextFromAny(labelAttr).equals(label)) {
                                    labelOk = true;
                                }
                            } else {
                                labelOk = true;
                            }

                            // Check if it has the correct value
                            if (value != null) {
                                if (XmlUtil.getTextFromAny(prop).equals(value)) {
                                    valueOk = true;
                                }
                            } else {
                                valueOk = true;
                            }
                        }
                    }
                    ok = codespaceOk && codeOk && labelOk && valueOk;
                    if (ok) {
                        break;
                    } else {
                        // System.out.println(codespaceOk);
                        // System.out.println(codeOk);
                        // System.out.println(labelOk);
                        // System.out.println(valueOk);
                        ok = codespaceOk = codeOk = labelOk = valueOk = false;
                    }
                }
            }
        }
        return ok;
    }

    public static boolean checkEventWhatElement(DigestType digest,
                                                String eventID,
                                                String codespace,
                                                String code,
                                                String label,
                                                String value) {

        boolean ok = false;
        XmlObject[] events = digest.selectChildren(EventType.type.getName().getNamespaceURI(),
            EVENT_THING_TYPE);
        if (events.length > 0) {
            EventType event = null;
            for (XmlObject obj : events) {
                if (((EventType) obj).getId().equals(eventID)) {
                    event = (EventType) obj;
                }
            }
            if (event != null) {
                // XmlObject event = events[0];
                ok = EMDigestHelper.objectHasWhatType(codespace, code, label, value, event);
            }
        }
        return ok;
    }

    public static boolean checkEntityWhatElement(DigestType digest,
                                                 String entityID,
                                                 String codespace,
                                                 String code,
                                                 String label,
                                                 String value) {

        boolean ok = false;
        XmlObject[] entities = digest.selectChildren(EntityType.type.getName().getNamespaceURI(),
            ENTITY_THING_TYPE);
        if (entities.length > 0) {
            EntityType entity = null;
            for (XmlObject obj : entities) {
                if (((EntityType) obj).getId().equals(entityID)) {
                    entity = (EntityType) obj;
                }
            }
            if (entity != null) {
                // XmlObject event = events[0];
                ok = EMDigestHelper.objectHasWhatType(codespace, code, label, value, entity);
            }
        }
        return ok;
    }

    public static boolean checkOrganizationWhatElement(DigestType digest,
                                                       String entityID,
                                                       String codespace,
                                                       String code,
                                                       String label,
                                                       String value) {

        boolean ok = false;
        XmlObject[] entities = digest.selectChildren(OrganizationType.type.getName().getNamespaceURI(),
            ORGANIZATION_THING_TYPE);
        if (entities.length > 0) {
            EntityType entity = null;
            for (XmlObject obj : entities) {
                if (((EntityType) obj).getId().equals(entityID)) {
                    entity = (EntityType) obj;
                }
            }
            if (entity != null) {
                // XmlObject event = events[0];
                ok = EMDigestHelper.objectHasWhatType(codespace, code, label, value, entity);
            }
        }
        return ok;
    }

    public static boolean hasALocationElement(DigestType digest) {

        XmlObject[] locations = digest.selectChildren(gov.ucore.ucore.x20.LocationType.type.getName().getNamespaceURI(),
            "Location");
        return (locations.length > 0);
    }
}
