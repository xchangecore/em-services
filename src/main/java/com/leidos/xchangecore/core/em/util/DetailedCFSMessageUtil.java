package com.leidos.xchangecore.core.em.util;

import gov.niem.niem.niemCore.x20.ActivityType;
import gov.niem.niem.niemCore.x20.AddressFullTextDocument;
import gov.niem.niem.niemCore.x20.CircularRegionType;
import gov.niem.niem.niemCore.x20.LocationType;
import gov.niem.niem.niemCore.x20.MeasurePointValueType;
import gov.niem.niem.niemCore.x20.TwoDimensionalGeographicCoordinateType;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.incident.UICDSIncidentType;

import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageException;
import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageXMLException;
import com.saic.precis.x2009.x06.base.DateTimeType;
import com.saic.precis.x2009.x06.base.IdentificationType;

public class DetailedCFSMessageUtil {

    static Logger log = LoggerFactory.getLogger(DetailedCFSMessageUtil.class);

    public static UICDSIncidentType populateIncident(UICDSIncidentType uicdsIncident,
                                                     DetailedCFSMessageReader reader)
        throws DetailedCFSMessageXMLException, DetailedCFSMessageException, XmlException {

        if (uicdsIncident != null) {

            // set ActivityDescriptionText (LEITSC: Payload/ServiceCall/ActitivityDescriptionText)
            if ((uicdsIncident.getActivityDescriptionTextArray() != null) &&
                (uicdsIncident.getActivityDescriptionTextArray().length > 0)) {
                uicdsIncident.getActivityDescriptionTextArray(0).setStringValue(reader.read(DetailedCFSMessageReader.ACTIVITY_DESC_TEXT));
            }

            // Set ActivityCategoryText(LIETSC:
            // Payload/ServiceCall/ServiceCallAugmentation/CallTypeText)
            if ((uicdsIncident.getActivityCategoryTextArray() != null) &&
                (uicdsIncident.getActivityCategoryTextArray().length > 0)) {
                uicdsIncident.getActivityCategoryTextArray(0).setStringValue(reader.read(DetailedCFSMessageReader.CALL_TYPE_TEXT));
            }

            // Set ActivityDate (LEITSC:
            // Payload/ServiceCall/ServiceCallAugmentation/ServiceCallAririvedDate)
            DateTimeType dateTime = DateTimeType.Factory.newInstance();
            dateTime.setStringValue(reader.read(DetailedCFSMessageReader.ARRIVED_DATE_TIME));
            if (uicdsIncident.getActivityDateRepresentationArray() != null &&
                uicdsIncident.getActivityDateRepresentationArray().length > 0) {
                uicdsIncident.getActivityDateRepresentationArray(0).set(dateTime);
            }

            if (uicdsIncident.getIncidentLocationArray() != null &&
                uicdsIncident.getIncidentLocationArray().length > 0) {

                // Set Location Address (LEITSC: Payload/Location/LocationAddress)
                AddressFullTextDocument addressDoc = AddressFullTextDocument.Factory.newInstance();
                addressDoc.addNewAddressFullText().setStringValue(reader.read(DetailedCFSMessageReader.ADDRESS_FULLTEXT));
                if (uicdsIncident.getIncidentLocationArray(0).getLocationAddressArray() != null &&
                    uicdsIncident.getIncidentLocationArray(0).getLocationAddressArray().length > 0) {
                    uicdsIncident.getIncidentLocationArray(0).getLocationAddressArray(0).set(addressDoc.getAddressFullText());
                }

                // Set Geographical Coordinate
                // Latitude (LEITSC:
                // Playload/ServiceCallResponseLocation/LocationTwoDimensionalGeographicCoordinates/GeographicCoordinateLatitude)

                if (uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray() != null &&
                    uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray().length > 0 &&
                    uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray() != null &&
                    uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray().length > 0 &&
                    uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray() != null &&
                    uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray().length > 0) {

                    if (uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLatitude() != null) {
                        String latStr = reader.read(DetailedCFSMessageReader.GEO_COORDINATE_LATITUDE);
                        if (!latStr.isEmpty()) {
                            String latVals[] = EMGeoUtil.toDegMinSec(Double.parseDouble(latStr));
                            uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLatitude().addNewLatitudeDegreeValue().setStringValue(latVals[0]);
                            uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLatitude().addNewLatitudeMinuteValue().setStringValue(latVals[1]);
                            uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLatitude().addNewLatitudeSecondValue().setStringValue(latVals[2]);
                        }
                    }
                    // Longitude (LEITSC:
                    // Playload/ServiceCallResponseLocation/LocationTwoDimensionalGeographicCoordinates/GeographicCoordinateLongitude)
                    String longStr = reader.read(DetailedCFSMessageReader.GEO_COORDINATE_LONGITUDE);
                    if (uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLongitude() != null) {
                        if (!longStr.isEmpty()) {
                            String longVals[] = EMGeoUtil.toDegMinSec(Double.parseDouble(longStr));
                            uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLongitude().addNewLongitudeDegreeValue().setStringValue(longVals[0]);
                            uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLongitude().addNewLongitudeMinuteValue().setStringValue(longVals[1]);
                            uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLongitude().addNewLongitudeSecondValue().setStringValue(longVals[2]);
                        }
                    }
                    // RaduisLength
                    if (uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionRadiusLengthMeasureArray() != null &&
                        uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionRadiusLengthMeasureArray().length > 0) {
                        MeasurePointValueType measurePoint = MeasurePointValueType.Factory.newInstance();
                        measurePoint.setStringValue("0.0");
                        uicdsIncident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(0).getCircularRegionRadiusLengthMeasureArray(0).addNewMeasureValue().set(measurePoint);
                    }
                }
            }

            // Set Jurisdiction
            IdentificationType jurisdictionIdentification = IdentificationType.Factory.newInstance();
            jurisdictionIdentification.addNewIdentifier().setStringValue(reader.read(DetailedCFSMessageReader.ORG_IDENTIFICATION_ID));
            if (uicdsIncident.getIncidentJurisdictionalOrganizationArray() != null &&
                uicdsIncident.getIncidentJurisdictionalOrganizationArray().length > 0 &&
                uicdsIncident.getIncidentJurisdictionalOrganizationArray(0).getOrganizationIdentificationArray() != null &&
                uicdsIncident.getIncidentJurisdictionalOrganizationArray(0).getOrganizationIdentificationArray().length > 0) {
                uicdsIncident.getIncidentJurisdictionalOrganizationArray(0).getOrganizationIdentificationArray(0).set(jurisdictionIdentification);
            }
            // Set Incident Event
            if (uicdsIncident.getIncidentEventArray() != null &&
                uicdsIncident.getIncidentEventArray().length > 0) {
                ActivityType incidentEvent = uicdsIncident.getIncidentEventArray(0);
                if (incidentEvent.getActivityIdentificationArray() != null &&
                    incidentEvent.getActivityIdentificationArray().length > 0) {
                    incidentEvent.getActivityIdentificationArray(0).addNewIdentificationID().setStringValue(reader.read(DetailedCFSMessageReader.ACTIVITY_IDENTIFICATION));
                }
                if (incidentEvent.getActivityStatusArray() != null &
                    incidentEvent.getActivityStatusArray().length > 0) {
                    incidentEvent.getActivityStatusArray(0).addNewStatusDescriptionText().setStringValue(reader.read(DetailedCFSMessageReader.ACTIVITY_STATUS_TEXT));
                }
                if (incidentEvent.getActivityDateRepresentationArray() != null &&
                    incidentEvent.getActivityDateRepresentationArray().length > 0) {
                    incidentEvent.getActivityDateRepresentationArray(0).set(dateTime);
                }
                if (incidentEvent.getActivityNameArray() != null &&
                    incidentEvent.getActivityNameArray().length > 0) {
                    incidentEvent.getActivityNameArray(0).setStringValue("LEITSC Incident");
                }
                if (incidentEvent.getActivityDescriptionTextArray() != null &&
                    incidentEvent.getActivityDescriptionTextArray().length > 0) {
                    incidentEvent.getActivityDescriptionTextArray(0).setStringValue(jurisdictionIdentification.getIdentifier().getStringValue());
                }
            }

            // System.out.println("populateIncident: incident[\n" + uicdsIncident.xmlText() +
            // "\n]");

        } else {
            String reasonForFailure = "UICDS INternal error - Unable to pupulate incident data";
            log.error("populateIncident: " + reasonForFailure);
            throw new DetailedCFSMessageXMLException(reasonForFailure);
        }

        return uicdsIncident;
    }

    public static UICDSIncidentType createUICDSIncident() {

        UICDSIncidentType uicdsIncident = UICDSIncidentType.Factory.newInstance();

        // Add ActivityDescriptionText
        uicdsIncident.addNewActivityDescriptionText();

        // Add ActivityCategoryText
        uicdsIncident.addNewActivityCategoryText();

        // Add ActivityDate
        uicdsIncident.addNewActivityDateRepresentation();

        // Add location
        LocationType location = uicdsIncident.addNewIncidentLocation();

        // Add Location Address
        location.addNewLocationAddress();

        // Add Geographical Coordinate Latitude
        CircularRegionType circularRegion = location.addNewLocationArea().addNewAreaCircularRegion();
        TwoDimensionalGeographicCoordinateType circularCoordinate = circularRegion.addNewCircularRegionCenterCoordinate();
        circularCoordinate.addNewGeographicCoordinateLatitude();
        circularCoordinate.addNewGeographicCoordinateLongitude();
        circularRegion.addNewCircularRegionRadiusLengthMeasure().addNewLengthUnitCode().setStringValue("SMI");

        // Add Jurisdiction
        uicdsIncident.addNewIncidentJurisdictionalOrganization().addNewOrganizationIdentification();

        // Add Incident Event
        ActivityType incidentEvent = uicdsIncident.addNewIncidentEvent();
        incidentEvent.addNewActivityIdentification();
        incidentEvent.addNewActivityStatus();
        incidentEvent.addNewActivityDateRepresentation();
        incidentEvent.addNewActivityName();
        incidentEvent.addNewActivityDescriptionText();

        return uicdsIncident;
    }

}
