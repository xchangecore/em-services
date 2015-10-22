package com.leidos.xchangecore.core.em.util;

import java.util.ArrayList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.incident.IncidentDocument;

import com.leidos.xchangecore.core.infrastructure.model.ExtendedMetadata;
import com.leidos.xchangecore.core.infrastructure.model.ShareRule;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class AgreementMatcher {

    private static final String INTEREST_GROUP_CODESPACE = "http://uicds.org/interestgroup#Incident";
    private static Logger logger = LoggerFactory.getLogger(AgreementMatcher.class);

    private static double calculateDistancekm(double lat1, double lon1, double lat2, double lon2) {

        final double earthRadius = 6371; // in km
        final double dLat = Math.toRadians(lat2 - lat1);
        final double dLng = Math.toRadians(lon2 - lon1);
        final double a = (Math.sin(dLat / 2) * Math.sin(dLat / 2)) +
            (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2));
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        final double dist = earthRadius * c;

        return dist;
    }

    public static boolean isRuleMatched(String[] points,
                                        Set<ShareRule> shareRules,
                                        IncidentDocument incidentDoc) {

        return isRuleMatchedIgnoreProximity(points, shareRules, incidentDoc, false);
    }

    public static boolean isRuleMatchedIgnoreProximity(String[] points,
                                                       Set<ShareRule> shareRules,
                                                       IncidentDocument incidentDoc,
                                                       boolean ignoreProximity) {

        if (shareRules.size() == 0) {
            return true;
        }

        // If all the rules are disabled then always share
        boolean evaluateRules = false;
        for (final ShareRule rule : shareRules) {
            if (rule.isEnabled()) {
                evaluateRules = true;
                break;
            }
        }

        if (!evaluateRules) {
            return true;
        }

        String coreLat = "";
        String coreLon = "";
        if (points != null) {
            coreLat = points[0];
            coreLon = points[1];
        }

        // Incident Type rule
        String incidentType = null;
        if (incidentDoc.getIncident().sizeOfActivityCategoryTextArray() > 0) {
            incidentType = incidentDoc.getIncident().getActivityCategoryTextArray(0).getStringValue();
        }
        if ((incidentType == null) || (incidentType.length() == 0)) {
            logger.error("isRuleMatched: Incident Type is empty");
            return false;
        }

        logger.debug("isRuleMatched: incidentType: " + incidentType);

        // Extended Metadata rule
        final ArrayList<ExtendedMetadata> incidentExtendedMetadataList = new ArrayList<ExtendedMetadata>();
        if (incidentDoc.getIncident().sizeOfExtendedMetadataArray() > 0) {
            logger.debug("isRuleMatched: incident.ExtendedMetadata size: " +
                incidentDoc.getIncident().sizeOfExtendedMetadataArray());
            final ExtendedMetadata extendedMetadata = new ExtendedMetadata();
            for (int i = 0; i < incidentDoc.getIncident().sizeOfExtendedMetadataArray(); i++) {

                extendedMetadata.setCodespace(incidentDoc.getIncident().getExtendedMetadataArray(i).getCodespace());
                extendedMetadata.setCode(incidentDoc.getIncident().getExtendedMetadataArray(i).getCode());
                extendedMetadata.setValue(incidentDoc.getIncident().getExtendedMetadataArray(i).getStringValue());

                incidentExtendedMetadataList.add(extendedMetadata);
            }
        }

        for (final ShareRule rule : shareRules) {

            boolean igMatches = false;
            boolean exMatches = false;
            boolean locMatches = false;

            // only process rule if it is enabled and has a condition
            if (!rule.isEnabled()) {
                continue;
            }

            logger.debug("isRuleMatched: Check Incident.Type");
            // validate the Incident Type
            if ((rule.getInterestGroup() != null) &&
                (rule.getInterestGroup().getCodeSpace() != null) &&
                (rule.getInterestGroup().getValue() != null)) {
                logger.debug("isRuleMatched: matching Rule.IncidentType: " +
                    rule.getInterestGroup().getValue());
                if (rule.getInterestGroup().getValue().equalsIgnoreCase("*") ||
                    (rule.getInterestGroup().getCodeSpace().equalsIgnoreCase(
                        INTEREST_GROUP_CODESPACE) && rule.getInterestGroup().getValue().equalsIgnoreCase(
                            incidentType))) {
                    igMatches = true;
                }
            } else {
                logger.debug("isRuleMatched: No IncidentType specified in Rule");
                igMatches = true;
            }
            logger.debug("isRuleMatched: igMatches: " + igMatches);
            if (!igMatches) {
                continue;
            }

            // now check Extended Metadata
            if ((rule.getExtendedMetadata() != null) && (rule.getExtendedMetadata().size() > 0)) {
                final ExtendedMetadata extendedMetadata = new ExtendedMetadata();

                logger.debug("isRuleMatched: Check extended metadata condition");
                for (final ExtendedMetadata data : rule.getExtendedMetadata()) {
                    extendedMetadata.setCodespace(data.getCodespace());
                    extendedMetadata.setCode(data.getCode());
                    extendedMetadata.setValue(data.getValue());
                    logger.debug("isRuleMatched: In Rule: " + extendedMetadata.toString());

                    for (final ExtendedMetadata incidentEM : incidentExtendedMetadataList) {
                        logger.debug("isRuleMatched: In Incident: " + incidentEM.toString());
                        if (incidentEM.getCode().equalsIgnoreCase(extendedMetadata.getCode()) &&
                            incidentEM.getCodespace().equalsIgnoreCase(
                                extendedMetadata.getCodespace()) &&
                                incidentEM.getValue().equalsIgnoreCase(extendedMetadata.getValue())) {
                            exMatches = true;
                            break;
                        }
                    }
                    // as long as one matchs then it's matched
                    if (exMatches) {
                        break;
                    }
                }
            } else {
                logger.debug("isRuleMatched: No Rule.ExtendedMatadata defined");
                exMatches = true;
            }
            logger.debug("isRuleMatched: exMatches: " + exMatches);
            if (!exMatches) {
                continue;
            }

            if (!ignoreProximity && (rule.getRemoteCoreProximity() != null)) {
                logger.debug("isRuleMatched: Check remote proximity condition");

                if (!("".equals(coreLat) || "".equals(coreLon))) {
                    // get the remote core's location as a point object
                    final GeometryFactory geoFactory = new GeometryFactory();
                    final Point remoteCorePoint = geoFactory.createPoint(new Coordinate(Double.parseDouble(coreLat),
                        Double.parseDouble(coreLon)));

                    if ((incidentDoc.getIncident().getIncidentLocationArray() != null) &&
                        (incidentDoc.getIncident().getIncidentLocationArray(0).getLocationAreaArray() != null)) {
                        // get the incident location (EMGeoUtil is naive, but works for most cases)
                        final Point incidentPoint = EMGeoUtil.parsePoint(incidentDoc.getIncident());
                        final double radiuskm = Double.parseDouble(rule.getRemoteCoreProximity());
                        final double lat1 = incidentPoint.getY();
                        final double lon1 = incidentPoint.getX();
                        final double lat2 = remoteCorePoint.getX();
                        final double lon2 = remoteCorePoint.getY();
                        logger.debug("isRuleMatched: incident: lat/lon: " + lat1 + "/" + lon1);
                        logger.debug("isRuleMatched: remote core: lat/lon " + lat2 + "/" + lon2);;
                        final double distancekm = calculateDistancekm(lat1, lon1, lat2, lon2);
                        logger.debug("isRuleMatched: distance: " + distancekm + ", proximity: " +
                            radiuskm);
                        if (distancekm <= radiuskm) {
                            locMatches = true;
                        }
                    }
                } else if (Boolean.valueOf(rule.getShareOnNoLoc())) {
                    locMatches = Boolean.valueOf(rule.getShareOnNoLoc());
                    logger.debug("isRuleMatched: No lat/lon specified and getShareOnNoLoc is set to " +
                        (locMatches ? "true" : "false"));
                }
            } else {
                // there was no location condition
                locMatches = true;
            }

            if (igMatches && exMatches && locMatches) {
                return true;
            }

        }

        return false;
    }
}
