package com.leidos.xchangecore.core.em.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;

import com.vividsolutions.jts.geom.Point;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;

public class IncidentUtil {

    static Logger log = LoggerFactory.getLogger(IncidentUtil.class);

    public static UICDSIncidentType getUICDSIncident(WorkProduct wp) {

        IncidentDocument incidentDoc = null;
        try {
            incidentDoc = (IncidentDocument) wp.getProduct();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("getUICDSIncident - parsing failed ");
        }
        return incidentDoc.getIncident();

    }

    public static String getIncidentName(UICDSIncidentType incident) {

        String incidentName = null;
        if (incident.sizeOfActivityNameArray() > 0) {
            incidentName = incident.getActivityNameArray(0).getStringValue();
        }
        return incidentName;
    }

    public static String getIncidentDescription(UICDSIncidentType incident) {

        String incidentDescription = null;
        if (incident.sizeOfActivityDescriptionTextArray() > 0) {
            incidentDescription = incident.getActivityDescriptionTextArray(0).getStringValue();
        }
        return incidentDescription;
    }

    public static Point getIncidentLocation(UICDSIncidentType incident) {

        Point point = EMGeoUtil.parsePoint(incident);
        return point;
    }

    public static double getLatitude(Point point) {

        return point.getY();
    }

    public static double getLongititude(Point point) {

        return point.getX();
    }
}
