package com.leidos.xchangecore.core.em.util;

import gov.niem.niem.niemCore.x20.AreaType;
import gov.niem.niem.niemCore.x20.CircularRegionType;
import gov.niem.niem.niemCore.x20.LatitudeCoordinateType;
import gov.niem.niem.niemCore.x20.LengthMeasureType;
import gov.niem.niem.niemCore.x20.LocationTwoDimensionalGeographicCoordinateDocument;
import gov.niem.niem.niemCore.x20.LocationType;
import gov.niem.niem.niemCore.x20.LongitudeCoordinateType;
import gov.niem.niem.niemCore.x20.MeasurePointValueDocument;
import gov.niem.niem.niemCore.x20.MeasurePointValueType;
import gov.niem.niem.niemCore.x20.TwoDimensionalGeographicCoordinateType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import net.opengis.gml.x32.CircleByCenterPointType;
import net.opengis.gml.x32.LinearRingType;
import net.opengis.gml.x32.PolygonType;

import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.util.DigestConstant;
import com.leidos.xchangecore.core.infrastructure.util.GeoUtil;
import com.leidos.xchangecore.core.infrastructure.util.InfrastructureNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.UUIDUtil;
import com.leidos.xchangecore.core.infrastructure.util.XmlUtil;
import com.usersmarts.jts.GeoGeometryFactory;
import com.usersmarts.util.Coerce;
import com.usersmarts.util.DOMUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class EMGeoUtil
    extends GeoUtil {

    static Logger log = LoggerFactory.getLogger(EMGeoUtil.class);

    private static GeoGeometryFactory geometryFactory = new GeoGeometryFactory();

    public static final String EPSG4326 = "EPSG:4326";

    public static CircularRegionType getCircle(String circleString) {

        CircularRegionType circular = CircularRegionType.Factory.newInstance();

        // each point is a circle with center coordinate and radius
        String[] values = circleString.split(" ");
        // it shall be point and radius
        if (values.length != 2) {
            return circular;
        }

        LocationTwoDimensionalGeographicCoordinateDocument loc = toCoordinate(values[0]);
        if (loc != null) {
            String radiusStr = "0.0";
            if (!values[1].isEmpty()) {
                radiusStr = values[1];
            }
            circular.addNewCircularRegionCenterCoordinate().set(loc.getLocationTwoDimensionalGeographicCoordinate());

            LengthMeasureType radius = circular.addNewCircularRegionRadiusLengthMeasure();
            MeasurePointValueDocument value = MeasurePointValueDocument.Factory.newInstance();
            value.addNewMeasurePointValue().setBigDecimalValue(new BigDecimal(radiusStr));
            radius.set(value);

        }
        return circular;
    }

    public static CircleByCenterPointType getCircle(CircularRegionType circle) {

        CircleByCenterPointType gCircle = CircleByCenterPointType.Factory.newInstance();
        gCircle.setNumArc(new BigInteger("1"));

        if (circle.sizeOfCircularRegionCenterCoordinateArray() > 0 &&
            circle.sizeOfCircularRegionRadiusLengthMeasureArray() > 0) {
            LatitudeCoordinateType lat = circle.getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLatitude();
            LongitudeCoordinateType lon = circle.getCircularRegionCenterCoordinateArray(0).getGeographicCoordinateLongitude();

            if (lat != null && lat.sizeOfLatitudeDegreeValueArray() > 0 && lon != null &&
                lon.sizeOfLongitudeDegreeValueArray() > 0) {
                String min = "0";
                String sec = "0";
                if (lat.sizeOfLatitudeMinuteValueArray() > 0) {
                    min = lat.getLatitudeMinuteValueArray(0).getStringValue();
                }
                if (lat.sizeOfLatitudeSecondValueArray() > 0) {
                    sec = lat.getLatitudeSecondValueArray(0).getStringValue();
                }

                String latStr = fromDegMinSec(lat.getLatitudeDegreeValueArray(0).getStringValue(),
                    min,
                    sec);

                min = "0";
                sec = "0";
                if (lon.sizeOfLongitudeMinuteValueArray() > 0) {
                    min = lon.getLongitudeMinuteValueArray(0).getStringValue();
                }
                if (lon.sizeOfLongitudeSecondValueArray() > 0) {
                    sec = lon.getLongitudeSecondValueArray(0).getStringValue();
                }

                String lonStr = fromDegMinSec(lon.getLongitudeDegreeValueArray(0).getStringValue(),
                    min,
                    sec);

                List<String> pointList = new ArrayList<String>();
                pointList.add(latStr);
                pointList.add(lonStr);

                gCircle.addNewPos().setListValue(pointList);

                // set the radius
                if (circle.getCircularRegionRadiusLengthMeasureArray(0).sizeOfMeasureValueArray() > 0) {
                    // System.out.println(circle.getCircularRegionRadiusLengthMeasureArray(0));
                    // System.out.println(MeasurePointValueType.type.getName());
                    XmlObject[] objects = circle.getCircularRegionRadiusLengthMeasureArray(0).selectChildren(new QName(MeasurePointValueType.type.getName().getNamespaceURI(),
                                                                                                                       "MeasurePointValue"));
                    if (objects.length > 0) {
                        MeasurePointValueType value = (MeasurePointValueType) objects[0];
                        gCircle.addNewRadius().setStringValue(value.getStringValue());
                        if (circle.getCircularRegionRadiusLengthMeasureArray(0).getLengthUnitCode() != null) {
                            gCircle.getRadius().setUom(circle.getCircularRegionRadiusLengthMeasureArray(0).getLengthUnitCode().getStringValue());
                        } else {
                            // Default to miles
                            gCircle.getRadius().setUom("SMI");
                        }
                    } else {
                        gCircle.addNewRadius().setStringValue("0.0");
                    }
                } else {
                    gCircle.addNewRadius().setStringValue("0.0");
                }
                // set default UOM
                if (gCircle.getRadius().getUom() == null) {
                    gCircle.getRadius().setUom("SMI");
                }
            }
        }

        return gCircle;
    }

    public static AreaType getPoloygon(String polygonString) {

        AreaType area = AreaType.Factory.newInstance();

        String[] coords = polygonString.split(" ");
        if (coords.length >= 3) {
            int i = 0;
            for (String coord : coords) {
                LocationTwoDimensionalGeographicCoordinateDocument loc = toCoordinate(coord);
                if (loc != null) {
                    area.addNewAreaPolygonGeographicCoordinate().set(loc.getLocationTwoDimensionalGeographicCoordinate());
                }
            }
        }

        return area;
    }

    public static PolygonType getPolygon(TwoDimensionalGeographicCoordinateType[] coords) {

        PolygonType gPolygon = PolygonType.Factory.newInstance();
        gPolygon.setId(UUIDUtil.getID(DigestConstant.S_Polygon));

        LinearRingType linearRing = LinearRingType.Factory.newInstance();
        for (TwoDimensionalGeographicCoordinateType coord : coords) {
            linearRing.addNewPos().setStringValue(toCoordinateString(coord));
        }

        XmlUtil.substitute(gPolygon.addNewExterior().addNewAbstractRing(),
            InfrastructureNamespaces.NS_GML,
            DigestConstant.S_LinearRing,
            LinearRingType.type,
            linearRing);

        return gPolygon;
    }

    /**
     * @param incident UICDSIncidentType
     * @return Geometry or null
     */
    public static Geometry parseGeometry(UICDSIncidentType incident) {

        if (incident.sizeOfIncidentLocationArray() > 0) {
            LocationType location = incident.getIncidentLocationArray(0);
            AreaType[] areas = location.getLocationAreaArray();
            if (areas.length > 0) {
                AreaType area = location.getLocationAreaArray(0);
                try {
                    // return parseGeometryUsingDOM(area);
                    return parseGeometryUsingAreaType(area);
                } catch (Throwable t) {
                    log.error("Error parsing spatial extent from Incident work product", t);
                }
            } else {
                log.warn("Unable to find LocationArea within Incident, cannot create default map");
            }
        }
        return null;
    }

    protected static Coordinate parseCoordinateUsingTwoDimensionalGeographicCoordinateType(TwoDimensionalGeographicCoordinateType coord) {

        String lonDegStr = "0.0";
        String lonMinStr = "0.0";
        String lonSecStr = "0.0";
        if (coord.getGeographicCoordinateLongitude() != null) {
            if (coord.getGeographicCoordinateLongitude().sizeOfLongitudeDegreeValueArray() > 0) {
                lonDegStr = coord.getGeographicCoordinateLongitude().getLongitudeDegreeValueArray(0).getStringValue();
            }
            if (coord.getGeographicCoordinateLongitude().sizeOfLongitudeMinuteValueArray() > 0) {
                lonMinStr = coord.getGeographicCoordinateLongitude().getLongitudeMinuteValueArray(0).getStringValue();
            }
            if (coord.getGeographicCoordinateLongitude().sizeOfLongitudeSecondValueArray() > 0) {
                lonSecStr = coord.getGeographicCoordinateLongitude().getLongitudeSecondValueArray(0).getStringValue();
            }
        }
        Double lonDegree = Coerce.toDouble(lonDegStr, null);
        Double lonMinute = Coerce.toDouble(lonMinStr, 0.0);
        Double lonSecond = Coerce.toDouble(lonSecStr, 0.0);

        String latDegStr = "0.0";
        String latMinStr = "0.0";
        String latSecStr = "0.0";
        if (coord.getGeographicCoordinateLatitude() != null) {
            if (coord.getGeographicCoordinateLatitude().sizeOfLatitudeDegreeValueArray() > 0) {
                latDegStr = coord.getGeographicCoordinateLatitude().getLatitudeDegreeValueArray(0).getStringValue();
            }
            if (coord.getGeographicCoordinateLatitude().sizeOfLatitudeMinuteValueArray() > 0) {
                latMinStr = coord.getGeographicCoordinateLatitude().getLatitudeMinuteValueArray(0).getStringValue();
            }
            if (coord.getGeographicCoordinateLatitude().sizeOfLatitudeSecondValueArray() > 0) {
                latSecStr = coord.getGeographicCoordinateLatitude().getLatitudeSecondValueArray(0).getStringValue();
            }
        }
        Double latDegree = Coerce.toDouble(latDegStr, null);
        Double latMinute = Coerce.toDouble(latMinStr, 0.0);
        Double latSecond = Coerce.toDouble(latSecStr, 0.0);

        // didn't specify coordinates properly
        if (lonDegree == null || latDegree == null) {
            log.warn("Coordinates were not specified properly in Incident work product, "
                     + "unable to determine location for default map");
            return null;
        }

        // convert to decimal degrees
        int sign = (int) (lonDegree / Math.abs(lonDegree));
        lonDegree = sign * (Math.abs(lonDegree) + (lonMinute / 60.0) + (lonSecond / 3600.0));

        sign = (int) (latDegree / Math.abs(latDegree));
        latDegree = sign * (Math.abs(latDegree) + (latMinute / 60.0) + (latSecond / 3600.0));

        return new Coordinate(lonDegree, latDegree);
    }

    /**
     * Parses the geometry of the incident by traversing it's xml representation
     * 
     * @param area AreaType
     * @return Geometry or null
     * @throws Exception
     */
    protected static Geometry parseGeometryUsingAreaType(AreaType area) {

        if (area.sizeOfAreaPolygonGeographicCoordinateArray() > 0) {
            TwoDimensionalGeographicCoordinateType[] polyCoords = area.getAreaPolygonGeographicCoordinateArray();
            if (polyCoords != null && polyCoords.length > 0) {

                int i = 0;
                Coordinate[] carr = new Coordinate[polyCoords.length];
                for (TwoDimensionalGeographicCoordinateType polyCoord : polyCoords) {
                    carr[i++] = parseCoordinateUsingTwoDimensionalGeographicCoordinateType(polyCoord);
                }

                // make sure last point is same as first point for valid polygon
                if (!carr[carr.length - 1].equals(carr[0])) {
                    Coordinate[] temp = new Coordinate[carr.length + 1];
                    for (i = 0; i < carr.length; ++i)
                        temp[i] = carr[i];
                    temp[i] = carr[0];
                    carr = temp;
                }

                // create polygon so we can get the bounding box for the area
                LinearRing shell = geometryFactory.createLinearRing(carr);
                Polygon poly = geometryFactory.createPolygon(shell, null);
                return poly;

            }
        } else if (area.sizeOfAreaCircularRegionArray() > 0) {
            // look for circle

            CircularRegionType circle = area.getAreaCircularRegionArray(0);
            if (circle != null) {
                Coordinate coord = null;
                if (circle.sizeOfCircularRegionCenterCoordinateArray() > 0) {
                    TwoDimensionalGeographicCoordinateType center = circle.getCircularRegionCenterCoordinateArray(0);
                    coord = parseCoordinateUsingTwoDimensionalGeographicCoordinateType(center);
                }
                Double radius = 0.0;
                if (circle.sizeOfCircularRegionCenterCoordinateArray() > 0 &&
                    circle.getCircularRegionRadiusLengthMeasureArray(0).sizeOfMeasureUnitTextArray() > 0) {
                    radius = Coerce.toDouble(circle.getCircularRegionRadiusLengthMeasureArray(0).getMeasureValueArray(0),
                        1.0);
                }
                if (coord != null) {
                    Point point = geometryFactory.createPoint(coord);
                    if (radius > 0.0) {
                        Geometry geometry = geometryFactory.createCircle(point, radius);
                        return geometry;
                    } else if (radius == 0.0) {
                        return point;
                    } else {
                        log.warn("Invalid radius specified for incident circle.  Must be non-negative");
                    }
                }
            }
        }

        return null;
    }

    /**
     * Parses the geometry of the incident by traversing it's xml representation
     * 
     * @param area AreaType
     * @return Geometry or null
     * @throws Exception
     */
    protected static Geometry parseGeometryUsingDOM(AreaType area) {

        Document doc1 = DOMUtils.getDocument(area.getDomNode());
        Node areaNode = doc1;

        if (area.sizeOfAreaPolygonGeographicCoordinateArray() > 0) {
            List<Element> polyCoords = DOMUtils.getChildren(areaNode,
                "AreaPolygonGeographicCoordinate");
            if (!polyCoords.isEmpty()) {

                int i = 0;
                Coordinate[] carr = new Coordinate[polyCoords.size()];
                for (Element polyCoord : polyCoords) {
                    carr[i++] = parseCoordinateUsingDOM(polyCoord);
                }

                // make sure last point is same as first point for valid polygon
                if (!carr[carr.length - 1].equals(carr[0])) {
                    Coordinate[] temp = new Coordinate[carr.length + 1];
                    for (i = 0; i < carr.length; ++i)
                        temp[i] = carr[i];
                    temp[i] = carr[0];
                    carr = temp;
                }

                // create polygon so we can get the bounding box for the area
                LinearRing shell = geometryFactory.createLinearRing(carr);
                Polygon poly = geometryFactory.createPolygon(shell, null);
                return poly;

            }
        } else if (area.sizeOfAreaCircularRegionArray() > 0) {
            // look for circle

            // TODO: the areaNode here is actually the AreaCircularRegion element so it will
            // never have an AreaCircularRegion child. It already is the correct element.
            Element c = DOMUtils.getChild(areaNode, "AreaCircularRegion");
            Node circle = areaNode;
            if (circle != null) {
                Element center = DOMUtils.getChild(circle, "CircularRegionCenterCoordinate");
                Element rad = DOMUtils.getChild(circle, "CircularRegionRadiusLengthMeasure");
                Double radius = Coerce.toDouble(DOMUtils.getChildText(rad, "LengthUnitCode"), 1.0);
                Coordinate coord = parseCoordinateUsingDOM(center);
                if (coord != null) {
                    Point point = geometryFactory.createPoint(coord);
                    if (radius > 0.0) {
                        Geometry geometry = geometryFactory.createCircle(point, radius);
                        return geometry;
                    } else if (radius == 0.0) {
                        return point;
                    } else {
                        log.warn("Invalid radius specified for incident circle.  Must be non-negative");
                    }
                }
            }
        }

        return null;
    }

    public static Point parsePoint(UICDSIncidentType incident) {

        Point point = null;
        try {
            Geometry geometry = parseGeometry(incident);

            if (geometry instanceof Point) {
                point = (Point) geometry;
            } else if (geometry != null) {
                point = geometry.getCentroid();
            }
        } catch (Throwable t) {
            System.out.println("parsePoint: Error! - unable to parse location (point) from incident");
            t.printStackTrace();
        }
        return point;
    }

    public static Point parsePointFromIncidentWP(WorkProduct product) {

        // parse the incident from the work product
        IncidentDocument doc = null;
        try {
            doc = (IncidentDocument) product.getProduct();
        } catch (Exception e) {
            log.error("Error parsing Incident work product", e);
        }

        Point point = null;
        try {
            Geometry geometry = parseGeometry(doc.getIncident());

            if (geometry instanceof Point) {
                point = (Point) geometry;
            } else if (geometry != null) {
                point = geometry.getCentroid();
            }
        } catch (Throwable t) {
            System.out.println("parsePoint: Error! - unable to parse location (point) from incident");
            t.printStackTrace();
        }
        return point;
    }

    /*
     * the latLon is in the format of (latitude, longitude) which is WGS 84 format for coordinates
     */
    private static LocationTwoDimensionalGeographicCoordinateDocument toCoordinate(String latLon) {

        String[] points = latLon.split(",");

        if (points.length != 2) {
            return null;
        }

        LocationTwoDimensionalGeographicCoordinateDocument loc = LocationTwoDimensionalGeographicCoordinateDocument.Factory.newInstance();

        try {
            String[] values = toDegMinSec(toDouble(points[0]));
            loc.addNewLocationTwoDimensionalGeographicCoordinate().addNewGeographicCoordinateLatitude().addNewLatitudeDegreeValue().setStringValue(values[0]);
            loc.getLocationTwoDimensionalGeographicCoordinate().getGeographicCoordinateLatitude().addNewLatitudeMinuteValue().setStringValue(values[1]);
            loc.getLocationTwoDimensionalGeographicCoordinate().getGeographicCoordinateLatitude().addNewLatitudeSecondValue().setStringValue(values[2]);
            values = toDegMinSec(toDouble(points[1]));
            loc.getLocationTwoDimensionalGeographicCoordinate().addNewGeographicCoordinateLongitude().addNewLongitudeDegreeValue().setStringValue(values[0]);
            loc.getLocationTwoDimensionalGeographicCoordinate().getGeographicCoordinateLongitude().addNewLongitudeMinuteValue().setStringValue(values[1]);
            loc.getLocationTwoDimensionalGeographicCoordinate().getGeographicCoordinateLongitude().addNewLongitudeSecondValue().setStringValue(values[2]);
        } catch (NumberFormatException e) {
            log.error(points[0] + " or " + points[1] + " not valid double");
            return null;
        }

        return loc;
    }

    private static String toCoordinateString(TwoDimensionalGeographicCoordinateType coord) {

        StringBuffer coordBuffer = new StringBuffer();

        coordBuffer.append(fromDegMinSec(coord.getGeographicCoordinateLatitude().getLatitudeDegreeValueArray(0).getStringValue(),
            coord.getGeographicCoordinateLatitude().getLatitudeMinuteValueArray(0).getStringValue(),
            coord.getGeographicCoordinateLatitude().getLatitudeSecondValueArray(0).getStringValue()));

        // coordBuffer.append(",");
        // TRAC #367
        // changed comman to space
        coordBuffer.append(" ");

        coordBuffer.append(fromDegMinSec(coord.getGeographicCoordinateLongitude().getLongitudeDegreeValueArray(0).getStringValue(),
            coord.getGeographicCoordinateLongitude().getLongitudeMinuteValueArray(0).getStringValue(),
            coord.getGeographicCoordinateLongitude().getLongitudeSecondValueArray(0).getStringValue()));

        return coordBuffer.toString();
    }

}
