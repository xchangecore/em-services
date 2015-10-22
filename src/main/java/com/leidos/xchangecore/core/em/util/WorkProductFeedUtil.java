package com.leidos.xchangecore.core.em.util;

import gov.ucore.ucore.x20.CauseOfRelationshipType;
import gov.ucore.ucore.x20.DigestType;
import gov.ucore.ucore.x20.EntityLocationRelationshipType;
import gov.ucore.ucore.x20.EntityType;
import gov.ucore.ucore.x20.EventLocationRelationshipType;
import gov.ucore.ucore.x20.EventType;
import gov.ucore.ucore.x20.SimplePropertyType;
import gov.ucore.ucore.x20.ThingType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlObject;

import com.usersmarts.pub.atom.Category;
import com.usersmarts.pub.atom.Entry;
import com.usersmarts.pub.atom.Link;
import com.usersmarts.pub.georss.GEORSS;
import com.vividsolutions.jts.geom.Geometry;
import com.leidos.xchangecore.core.em.service.AlertService;
import com.leidos.xchangecore.core.em.service.IncidentManagementService;
import com.leidos.xchangecore.core.em.service.ResourceManagementService;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.util.DigestConstant;
import com.leidos.xchangecore.core.infrastructure.util.InfrastructureNamespaces;

public class WorkProductFeedUtil
    implements InfrastructureNamespaces, DigestConstant {

    // linkPrefix = "api/workProducts/"
    static public Set<Entry> getFeedEntries(WorkProduct workProduct, String linkPrefix) {

        // Create the entries based on the work product type
        String type = workProduct.getProductType();

        if (type.equals(IncidentManagementService.InterestGroupType)) {
            return getIncidentFeedEntries(workProduct, linkPrefix);
        } else if (type.equals(ResourceManagementService.COMMIT_RESOURCE_PRODUCT_TYPE)) {
            return getCommitResourceFeedEntries(workProduct, linkPrefix);
        } else if (type.equals(ResourceManagementService.REQUEST_RESOURCE_PRODUCT_TYPE)) {
            return getRequestResourceFeedEntries(workProduct, linkPrefix);
        } else if (type.equals(AlertService.Type)) {
            return getAlertFeedEntries(workProduct, linkPrefix);
        } else if (type.equals("Feature")) {
            return getFeatureFeedEntries(workProduct, linkPrefix);
        }

        Set<Entry> emptySet = Collections.emptySet();
        return emptySet;
    }

    private static Set<Entry> getRequestResourceFeedEntries(WorkProduct workProduct,
                                                            String linkPrefix) {

        DigestType digest = workProduct.getDigest().getDigest();

        HashSet<Entry> set = new HashSet<Entry>();

        // Get the CommunicationEvent events
        Set<ThingType> things = EMDigestHelper.getThingsByWhatType(digest,
            NS_UCORE_CODESPACE,
            S_CommunicationEvent);

        // Create feed entries for each CommunicationEvent
        if (things != null && things.size() > 0) {
            for (ThingType thing : things) {
                if (thing instanceof EventType) {
                    EventType event = (EventType) thing;
                    Entry entry = createWorkProductFeedEntry(workProduct, linkPrefix);
                    if (event.getDescriptor() != null) {
                        entry.setTitle(event.getDescriptor().getStringValue());
                    } else {
                        entry.setTitle("Resource Request");
                    }
                    SimplePropertyType resourceProperty = EMDigestHelper.getSimplePropertyFromThing(thing,
                        "http://nimsonline.org/2.0",
                        null,
                        "Resource",
                        null);
                    SimplePropertyType quantityProperty = EMDigestHelper.getSimplePropertyFromThing(thing,
                        "urn:oasis:names:tc:emergency:EDXL:RM:1.0",
                        null,
                        "Quantity",
                        null);
                    StringBuffer summary = new StringBuffer();
                    summary.append("Requested Resource: ");
                    if (quantityProperty != null) {
                        summary.append(quantityProperty.getStringValue());
                        summary.append(" ");
                    }
                    if (resourceProperty != null) {
                        summary.append(resourceProperty.getStringValue());
                        summary.append(" ");
                    }
                    entry.setSummary(summary.toString());

                    entry.addCategory(new Category(S_CommunicationEvent, NS_UCORE, ""));
                    Geometry geometry = getRequestLocation(digest, event.getId());
                    if (geometry != null)
                        entry.put(GEORSS.WHERE, geometry);
                    set.add(entry);
                }
            }
        }
        return set;
    }

    private static Geometry getRequestLocation(DigestType digest, String id) {

        // Find the cause of the request resource
        CauseOfRelationshipType cause = EMDigestHelper.getCauseByEffectID(digest, id);

        // Find the located at for the cause
        EventLocationRelationshipType locatedAt = EMDigestHelper.getLocationRelationshipByTypeAndEventID(digest,
            S_OccursAt,
            cause.getCause());

        // Find where the cause occurs at
        if (locatedAt != null && locatedAt.getLocationRef() != null &&
            locatedAt.getLocationRef().getRef().size() > 0) {
            return EMDigestHelper.getGeometryFromLocationByID(digest,
                (String) locatedAt.getLocationRef().getRef().get(0));
        }
        return null;
    }

    private static Set<Entry> getAlertFeedEntries(WorkProduct workProduct, String linkPrefix) {

        Entry entry = createWorkProductFeedEntry(workProduct, linkPrefix);

        DigestType digest = workProduct.getDigest().getDigest();

        if (digest.sizeOfThingAbstractArray() > 0) {
            ThingType thing = digest.getThingAbstractArray(0);
            if (thing != null) {
                if (thing.getDescriptor() != null) {
                    entry.setSummary(thing.getDescriptor().getStringValue());
                }
                if (thing.sizeOfIdentifierArray() > 0) {
                    entry.setTitle(thing.getIdentifierArray(0).getStringValue());
                }
            }
        }

        entry.put(GEORSS.WHERE, EMDigestHelper.getFirstGeometry(digest));

        // UCore taxonomy
        entry.addCategory(new Category(S_AlertEvent, NS_UCORE, ""));

        HashSet<Entry> set = new HashSet<Entry>();
        set.add(entry);

        return set;
    }

    private static Set<Entry> getFeatureFeedEntries(WorkProduct workProduct, String linkPrefix) {

        Entry entry = createWorkProductFeedEntry(workProduct, linkPrefix);

        DigestType digest = workProduct.getDigest().getDigest();

        if (digest.sizeOfThingAbstractArray() > 0) {
            // Find the Entity type and use it to set descriptor and title
            for (ThingType thing : digest.getThingAbstractArray()) {
                if (thing != null && thing instanceof EntityType) {
                    EntityType entity = (EntityType) thing;
                    if (entity.getDescriptor() != null) {
                        entry.setSummary(entity.getDescriptor().getStringValue());
                    }
                    if (entity.sizeOfIdentifierArray() > 0) {
                        entry.setTitle(entity.getIdentifierArray(0).getStringValue());
                    }
                }
            }
        }

        entry.put(GEORSS.WHERE, EMDigestHelper.getFirstGeometry(digest));

        // UCore taxonomy from what type of entity
        String what = EMDigestHelper.getUCoreWhatType(digest);
        entry.addCategory(new Category(what, NS_UCORE, ""));

        HashSet<Entry> set = new HashSet<Entry>();
        set.add(entry);

        return set;
    }

    private static Set<Entry> getCommitResourceFeedEntries(WorkProduct workProduct,
                                                           String linkPrefix) {

        DigestType digest = workProduct.getDigest().getDigest();

        // Get entity id for each HasDestinationOf relationship
        HashMap<String, String> entityIds = new HashMap<String, String>();
        XmlObject[] elements = digest.selectChildren(NS_UCORE, "HasDestinationOf");
        if (elements != null && elements.length > 0) {
            for (XmlObject element : elements) {
                if (element instanceof EntityLocationRelationshipType) {
                    EntityLocationRelationshipType rel = (EntityLocationRelationshipType) element;
                    entityIds.put(rel.getEntityRef().getRef().toString(),
                        rel.getLocationRef().getRef().toString());
                }
            }
        }

        // Create feed entries for each entity with a HasDestinationOf
        HashSet<Entry> set = new HashSet<Entry>();
        elements = digest.selectChildren(NS_UCORE, S_Entity);

        if (elements != null && elements.length > 0) {
            for (XmlObject element : elements) {
                if (element instanceof EntityType) {
                    EntityType entity = (EntityType) element;
                    if (entityIds.keySet().contains(entity.getId())) {
                        Entry entry = createWorkProductFeedEntry(workProduct, linkPrefix);
                        entry.setTitle(entity.getDescriptor().getStringValue());
                        entry.setSummary(entity.getDescriptor().getStringValue());
                        entry.getCategories().add(new Category(S_Entity, NS_UCORE, ""));
                        entry.put(GEORSS.WHERE, EMDigestHelper.getGeometryFromLocationByID(digest,
                            entityIds.get(entity.getId())));
                        set.add(entry);
                    }
                }
            }
        }
        return set;
    }

    private static Set<Entry> getIncidentFeedEntries(WorkProduct workProduct, String linkPrefix) {

        Entry entry = createWorkProductFeedEntry(workProduct, linkPrefix);

        DigestType digest = workProduct.getDigest().getDigest();

        if (digest.sizeOfThingAbstractArray() > 0) {
            ThingType thing = digest.getThingAbstractArray(0);
            if (thing != null) {
                if (thing.getDescriptor() != null) {
                    entry.setSummary(thing.getDescriptor().getStringValue());
                }
                if (thing.sizeOfIdentifierArray() > 0) {
                    entry.setTitle(thing.getIdentifierArray(0).getStringValue());
                }
            }
        }

        entry.put(GEORSS.WHERE, EMDigestHelper.getFirstGeometry(digest));

        // UCore taxonomy
        entry.addCategory(new Category(S_Event, NS_UCORE, ""));

        HashSet<Entry> set = new HashSet<Entry>();
        set.add(entry);

        return set;
    }

    private static Entry createWorkProductFeedEntry(WorkProduct workProduct, String linkPrefix) {

        Entry entry = new Entry();

        entry.setId(workProduct.getProductID());
        // entry.workProductType = workProduct.getProductType();
        entry.getLinks().add(new Link("WorkProduct",
                                      linkPrefix + workProduct.getProductID(),
                                      "rel",
                                      ""));
        entry.setUpdated(workProduct.getUpdatedDate());
        entry.setPublished(workProduct.getCreatedDate());

        if (workProduct.getAssociatedInterestGroupIDs() != null &&
            workProduct.getAssociatedInterestGroupIDs().size() > 0) {
            entry.add(new QName("http://www.saic.com/precis/2009/06/base", "AssociatedGroups"),
                workProduct.getAssociatedInterestGroupIDs().iterator().next());
        }

        return entry;
    }
}