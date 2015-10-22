/**
 * 
 */
package com.leidos.xchangecore.core.em.util;

import gov.ucore.ucore.x20.LocationType;
import gov.ucore.ucore.x20.ThingType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import net.opengis.gml.x32.DirectPositionType;
import net.opengis.gml.x32.LineStringType;
import net.opengis.gml.x32.LinearRingType;
import net.opengis.gml.x32.PointType;
import net.opengis.gml.x32.PolygonType;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlObject;

import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.util.DigestConstant;
import com.leidos.xchangecore.core.infrastructure.util.DigestHelper;
import com.leidos.xchangecore.core.infrastructure.util.InfrastructureNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.UUIDUtil;
import com.leidos.xchangecore.core.infrastructure.util.XmlUtil;
import com.usersmarts.cx.AttributeType;
import com.usersmarts.cx.Collection;
import com.usersmarts.cx.Entity;
import com.usersmarts.cx.fs.FileSystemWorkspace;
import com.usersmarts.cx.search.Query;
import com.usersmarts.cx.search.Results;
import com.usersmarts.pub.atom.ATOM;
import com.usersmarts.pub.atom.Entry;
import com.usersmarts.pub.georss.GEORSS;
import com.usersmarts.util.Coerce;
import com.usersmarts.util.FileUtils;
import com.usersmarts.util.JarUtils;
import com.usersmarts.xmf2.MarshalContext;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * ShapefileIngester
 * 
 * @author Christopher Lakey
 * 
 * @created Dec 16, 2008
 * 
 */
public class ShapefileIngester {

    private final MarshalContext context = new MarshalContext(AtomModule.class);
    private final String DEFAULT_SCALAR = "1";
    private final String STYLE_CODESPACE = "http://uicds.us/style";

    private String getColorAsABGR(String colorsProperty) {

        String colorString = null;
        if (colorsProperty != null) {
            try {
                colorString = "";
                String[] colors = colorsProperty.split(",");
                for (String color : colors) {
                    if (Integer.parseInt(color) < 16) {
                        colorString += "0";
                    }
                    colorString += Integer.toHexString(Integer.parseInt(color));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return colorString;
    }

    public List<WorkProduct> parseFeatures(String incidentId, InputStream in) throws Exception {

        File file = writeTempFile(in);
        File tempDir = FileUtils.createTempFile("shapefile", "", null);
        JarUtils.extract(file, tempDir);
        file.delete();

        FileSystemWorkspace ws = new FileSystemWorkspace(tempDir);
        Collection collection = ws.getCollections().get(0);
        Query query = new Query();
        Results<Entity> results = collection.search(query);
        // LINE_ID UNITS COLORS DESC

        List<WorkProduct> products = new ArrayList<WorkProduct>();
        for (Entity e : results.list()) {
            String id = Coerce.toString(e.getValue(new QName("LINE_ID")));
            AttributeType[] atts = e.getEntityType().getAttributeTypes();
            String desc = Coerce.toString(e.getValue(new QName("DESC")));
            Geometry where = (Geometry) e.getValue(new QName("where"));

            Entry entry = new Entry();
            entry.put(ATOM.ID, id);
            entry.put(ATOM.TITLE, "Shapefile Feature");
            entry.put(ATOM.SUMMARY, desc);
            entry.put(GEORSS.WHERE, where);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            context.marshal(entry, out);
            WorkProduct feature = new WorkProduct();
            feature.setProductType("Feature");
            if (incidentId != null) {
                feature.associateInterestGroup(incidentId);
            }
            feature.setCreatedDate(new Date());
            feature.setMimeType("application/atom+xml");
            // TODO do we have better way to do this ???
            feature.setProduct(XmlObject.Factory.parse(new String(out.toByteArray())));
            out.close();

            DigestHelper digest = new DigestHelper();

            if (where instanceof Point) {
                setPointDigest(digest, where);
            } else if (where instanceof MultiLineString) {
                setMultiLineStringDigest(digest, where);
            } else if (where instanceof Polygon) {
                setPolygonDigest(digest, where);
            }
            feature.setDigest(digest.getDigest());
            products.add(feature);

            ThingType thing = digest.getDigest().getDigest().getThingAbstractArray()[0];

            String colors = null;
            String icon = null;
            String scalar = DEFAULT_SCALAR;

            for (AttributeType att : atts) {
                QName name = att.getName();
                if (name.getLocalPart().equalsIgnoreCase("colors")) {
                    colors = Coerce.toString(e.getValue(att.getName()));
                } else if (name.getLocalPart().equalsIgnoreCase("icon")) {
                    icon = Coerce.toString(e.getValue(att.getName()));
                } else if (name.getLocalPart().equalsIgnoreCase("scalar")) {
                    scalar = Coerce.toString(e.getValue(att.getName()));
                }
            }
            // Color Simple Property
            String abgr = getColorAsABGR(colors);
            if (abgr != null) {
                digest.addSimplePropertyToThing(thing, STYLE_CODESPACE, "color", "Colors", "#" +
                                                                                           abgr);
            }

            // Scalar Simple Property
            digest.addSimplePropertyToThing(thing, STYLE_CODESPACE, "scalar", "Scalar", scalar);
            if (where instanceof Point) {
                // Icon Simple Property
                if (icon != null) {
                    digest.addSimplePropertyToThing(thing, STYLE_CODESPACE, "icon", "Icon", icon);
                }
            }
        }
        collection.close();
        if (tempDir.exists()) {
            tempDir.delete();
        }
        return products;
    }

    private void setMultiLineStringDigest(DigestHelper digest, Geometry where) {

        LineStringType line = LineStringType.Factory.newInstance();
        line.setId(UUIDUtil.getID("MultiLineString"));

        Coordinate[] coordinates = where.getCoordinates();
        LocationType location = LocationType.Factory.newInstance();

        DirectPositionType[] dpa = new DirectPositionType[coordinates.length];
        int i = 0;
        for (Coordinate coordinate : coordinates) {
            DirectPositionType pos = DirectPositionType.Factory.newInstance();
            BigInteger dimension = BigInteger.valueOf(2);
            pos.setSrsDimension(dimension);
            pos.setStringValue(coordinate.y + " " + coordinate.x);
            dpa[i] = pos;
            i++;
        }
        line.setPosArray(dpa);
        location.setId(UUIDUtil.getID("Location"));
        digest.setLineString(location, line);
    }

    private void setPointDigest(DigestHelper digest, Geometry where) {

        Point pointGeom = where.getCentroid();

        PointType point = PointType.Factory.newInstance();
        point.setId(UUIDUtil.getID("Point"));
        DirectPositionType pos = DirectPositionType.Factory.newInstance();
        BigInteger dimension = BigInteger.valueOf(2);
        pos.setSrsDimension(dimension);
        pos.setStringValue(pointGeom.getCoordinate().y + " " + pointGeom.getCoordinate().x);
        point.setPos(pos);

        LocationType location = LocationType.Factory.newInstance();
        location.setId(UUIDUtil.getID("Location"));
        digest.setPoint(location, point);
    }

    private void setPolygonDigest(DigestHelper digest, Geometry where) {

        LocationType location = LocationType.Factory.newInstance();
        Polygon polygon = (Polygon) where;
        LinearRing lr = (LinearRing) polygon.getExteriorRing();
        Coordinate[] coordinates = lr.getCoordinates();
        LinearRingType linearRing = LinearRingType.Factory.newInstance();
        for (Coordinate coordinate : coordinates) {
            List<String> pointList = new ArrayList<String>();
            pointList.add(Double.toString(coordinate.y));
            pointList.add(Double.toString(coordinate.x));
            linearRing.addNewPos().setListValue(pointList);
        }
        PolygonType gPolygon = PolygonType.Factory.newInstance();
        gPolygon.setId(UUIDUtil.getID(DigestConstant.S_Polygon));
        gPolygon.setSrsName("EPSG:4326");
        if (linearRing != null) {
            XmlUtil.substitute(gPolygon.addNewExterior().addNewAbstractRing(),
                InfrastructureNamespaces.NS_GML,
                DigestConstant.S_LinearRing,
                LinearRingType.type,
                linearRing);
        }
        location.setId(UUIDUtil.getID("Location"));
        digest.setPolygon(location, gPolygon);
    }

    protected File writeTempFile(InputStream in) throws Exception {

        // copy inputstream to file
        File file = FileUtils.createTempFile("shapefile", ".zip", null);
        if (!file.exists()) {
            file.createNewFile();
        }
        OutputStream out = new FileOutputStream(file);
        IOUtils.copy(in, out);
        out.close();
        in.close();
        return file;
    }

}