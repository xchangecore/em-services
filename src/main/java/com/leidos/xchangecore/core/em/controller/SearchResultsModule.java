package com.leidos.xchangecore.core.em.controller;

import gov.ucore.ucore.x20.DigestType;
import gov.ucore.ucore.x20.ThingType;

import java.util.Date;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import net.opengis.context.LayerType;
import net.opengis.context.ViewContextDocument;

import org.apache.xmlbeans.XmlException;

import com.leidos.xchangecore.core.em.service.MapService;
import com.leidos.xchangecore.core.em.util.WorkProductFeedUtil;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.util.DigestHelper;
import com.usersmarts.cx.Entity;
import com.usersmarts.cx.search.Results;
import com.usersmarts.geo.gml.GML2;
import com.usersmarts.geo.gml.GMLStaxFormatter;
import com.usersmarts.pub.atom.ATOM;
import com.usersmarts.pub.atom.Entry;
import com.usersmarts.pub.atom.OPENSEARCH;
import com.usersmarts.pub.georss.GEO;
import com.usersmarts.pub.georss.GEORSS;
import com.usersmarts.pub.kml.KML;
import com.usersmarts.pub.rss.RSS2;
import com.usersmarts.util.stax.ExtendedXMLStreamWriter;
import com.usersmarts.xmf2.Configuration;
import com.usersmarts.xmf2.MarshalContext;
import com.usersmarts.xmf2.MarshalException;
import com.usersmarts.xmf2.stax.StaxAdaptersModule;
import com.usersmarts.xmf2.stax.StaxCompositeModule;
import com.usersmarts.xmf2.stax.StaxFormatter;
import com.usersmarts.xmf2.stax.StaxFormatterFinder;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class SearchResultsModule
extends StaxCompositeModule {

    private static class ResultsAtomFeedFormatter
    extends StaxFormatter {

        @Override
        public Object format(MarshalContext context, Object input, ExtendedXMLStreamWriter writer)
            throws MarshalException, XMLStreamException {

            Results<?> results = (Results<?>) input;

            writer.writeStartElement(ATOM.FEED);
            writer.element(ATOM.TITLE, "Results Feed");
            writer.element(OPENSEARCH.TOTAL_RESULTS, results.getResultSize());
            if (results.isPaging()) {
                writer.element(OPENSEARCH.ITEMS_PER_PAGE, results.getPageSize());
            }

            // marshal entries
            for (Object result : results) {
                if (result instanceof Entity) {
                    Object delegate = ((Entity) result).getDelegate();
                    if (delegate instanceof WorkProduct) {
                        formatEntity(context, (WorkProduct) delegate, writer);
                    }
                } else {
                    context.marshal(result, writer);
                }
            }

            writer.writeEndElement();
            return writer;
        }

        void formatEntity(MarshalContext context, WorkProduct product, ExtendedXMLStreamWriter out)
            throws XMLStreamException {

            String id = product.getProductID();
            Date updated = product.getUpdatedDate();
            Date published = product.getCreatedDate();
            String title = "";
            String summary = "";

            Geometry geometry = null;
            if (product.getDigest() != null && product.getDigest().getDigest() != null) {
                DigestType digest = product.getDigest().getDigest();
                // first thing
                if (digest.sizeOfThingAbstractArray() > 0) {
                    ThingType thing = digest.getThingAbstractArray(0);
                    if (thing != null) {
                        if (thing.getDescriptor() != null) {
                            summary = thing.getDescriptor().getStringValue();
                        }
                        if (thing.sizeOfIdentifierArray() > 0) {
                            title = thing.getIdentifierArray(0).getStringValue();
                        }
                    }
                }
                geometry = DigestHelper.getFirstGeometry(digest);
            } else {
                title = id;
                summary = product.getProductType() + ":" + product.getMimeType();
            }

            // String id = entity.getId();
            String href = context.getProperty("baseURL") + "workProducts/" + id;

            out.element(ATOM.ENTRY);
            {
                out.element(ATOM.ID, id);
                out.element(ATOM.TITLE, title);
                out.element(ATOM.SUMMARY, summary);
                out.element(ATOM.PUBLISHED, published);
                out.element(ATOM.UPDATED, updated);

                out.element(ATOM.LINK);
                out.attribute("rel", "self");
                out.attribute("href", href);
                out.end();

                if (geometry != null) {
                    out.element("gml", "where", GML2.NAMESPACE);
                    new GMLStaxFormatter().format(context, geometry, out);
                    out.end();
                }
            }
            out.end();
        }
    }

    private static class ResultsKmlFeedFormatter
    extends StaxFormatter {

        public static final String YELLOW_PUSHPIN_NORMAL_STYLE_ID = "sn_ylw-pushpin";
        public static final String YELLOW_PUSHPIN_HIGHLIGHT_STYLE_ID = "sh_ylw-pushpin";
        public static final String YELLOW_PUSHPIN_STYLEMAP_ID = "msn_ylw-pushpin";
        public static final String YELLOW_PUSHPIN_STYLEURL = "#msn_ylw-pushpin";
        public static final String YELLOW_PUSHPIN_NORMAL_STYLEURL = "#sn_ylw-pushpin";
        public static final String YELLOW_PUSHPIN_HIGHLIGHT_STYLEURL = "#sh_ylw-pushpin";
        public static final String YELLOW_PUSHPIN_PNG_URL = "http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png";

        private void addDefaultStyles(ExtendedXMLStreamWriter out) throws XMLStreamException {

            out.element(KML.STYLE);
            out.attribute(KML.ID, YELLOW_PUSHPIN_NORMAL_STYLE_ID);
            {
                out.element(KML.ICON_STYLE);
                {
                    out.element(new QName(KML.NAMESPACE, "scale"), "1.1");
                    out.element(KML.ICON);
                    {
                        out.element(KML.HREF, YELLOW_PUSHPIN_PNG_URL);
                    }
                    out.end();
                    out.element(KML.HOT_SPOT);
                    out.attribute("x", "20");
                    out.attribute("y", "2");
                    out.attribute("xunits", "pixels");
                    out.attribute("yunits", "pixels");
                    out.end();
                }
                out.end();
            }
            out.end();
            out.element(KML.STYLE);
            out.attribute(KML.ID, YELLOW_PUSHPIN_HIGHLIGHT_STYLE_ID);
            {
                out.element(KML.ICON_STYLE);
                {
                    out.element(new QName(KML.NAMESPACE, "scale"), "1.3");
                    out.element(KML.ICON);
                    {
                        out.element(KML.HREF, YELLOW_PUSHPIN_PNG_URL);
                    }
                    out.end();
                    out.element(KML.HOT_SPOT);
                    out.attribute("x", "20");
                    out.attribute("y", "2");
                    out.attribute("xunits", "pixels");
                    out.attribute("yunits", "pixels");
                    out.end();
                }
                out.end();
            }
            out.end();
            out.element(KML.STYLE_MAP);
            {
                out.element(new QName(KML.NAMESPACE, "Pair"));
                {
                    out.element(new QName(KML.NAMESPACE, "key"), "normal");
                    out.element(new QName(KML.NAMESPACE, "styleUrl"),
                        YELLOW_PUSHPIN_NORMAL_STYLEURL);
                }
                out.end();
                out.element(new QName(KML.NAMESPACE, "Pair"));
                {
                    out.element(new QName(KML.NAMESPACE, "key"), "highlight");
                    out.element(new QName(KML.NAMESPACE, "styleUrl"),
                        YELLOW_PUSHPIN_HIGHLIGHT_STYLEURL);
                }
                out.end();
            }
            out.end();
        }

        private void addWMSElement(ExtendedXMLStreamWriter writer, WorkProduct map, String baseURL)
            throws XMLStreamException {

            try {
                // TODO do we still need this ???
                ViewContextDocument viewContext = ViewContextDocument.Factory.parse(new String(map.getProduct().xmlText()));
                if (viewContext.getViewContext() == null ||
                    viewContext.getViewContext().getLayerList() == null) {
                    return;
                }
                LayerType[] layers = viewContext.getViewContext().getLayerList().getLayerArray();
                int drawOrder = 1;
                for (LayerType layer : layers) {

                    // System.out.println("KML processing layer: " + layer.getTitle());
                    if (!layer.getTitle().equalsIgnoreCase("Base Map")) {
                        String incidentID = null;
                        if (map.getAssociatedInterestGroupIDs().size() > 0) {
                            incidentID = map.getAssociatedInterestGroupIDs().iterator().next();
                            if (incidentID == null) {
                                continue;
                            }
                        }

                        writer.element(KML.GROUND_OVERLAY);
                        {
                            writer.element(KML.NAME, layer.getTitle());
                            writer.element(KML.COLOR, "7fffffff");
                            writer.element(KML.DRAW_ORDER, drawOrder++);
                            writer.element(KML.ICON);
                            {
                                writer.element(KML.HREF,
                                    layer.getServer().getOnlineResource().getHref());
                                writer.element(KML.REFRESH_MODE, "onInterval");
                                writer.element(KML.REFRESH_INTERVAL, "30");
                                writer.element(KML.VIEW_REFRESH_MODE, "onStop");
                                writer.element(new QName(KML.NAMESPACE, "viewRefreshTime"), "7");
                                String getMapQueryString = getGetMapQueryString(layer);
                                writer.element(KML.VIEW_FORMAT, getMapQueryString);

                            }
                            writer.end();
                            writer.element(KML.LAT_LON_BOX);
                            {
                                writer.element(KML.NORTH, "[bboxNorth]");
                                writer.element(KML.SOUTH, "[bboxSouth]");
                                writer.element(KML.EAST, "[bboxEast]");
                                writer.element(KML.WEST, "[bboxWest]");
                            }
                            writer.end();
                            writer.end();
                        }
                    }
                }
            } catch (XmlException e) {
                return;
            }
        }

        private void addWMSElements(ExtendedXMLStreamWriter writer,
                                    Results<?> results,
                                    String baseURL) {

            for (Object result : results) {
                if (result instanceof Entity) {
                    Object delegate = ((Entity) result).getDelegate();
                    if (delegate instanceof WorkProduct) {
                        WorkProduct wp = (WorkProduct) delegate;
                        if (wp.getProductType().equals(MapService.MapType)) {
                            try {
                                addWMSElement(writer, wp, baseURL);
                            } catch (XMLStreamException e) {
                                System.err.println("Error adding WMS element: " + e.getMessage());
                                return;
                            }
                        }
                    }
                }
            }

        }

        @Override
        public Object format(MarshalContext context, Object input, ExtendedXMLStreamWriter writer)
            throws MarshalException, XMLStreamException {

            Results<?> results = (Results<?>) input;

            writer.element(KML.KML);
            writer.writeNamespace("kml", KML.NAMESPACE);
            {
                writer.element(KML.DOCUMENT);
                {
                    writer.element(KML.NAME, "UICDS KML Feed");
                    addDefaultStyles(writer);
                    // addWMSElements(writer, results, (String)context.getProperty("baseURL"));

                    // marshal entries
                    for (Object result : results) {
                        if (result instanceof Entity) {
                            Object delegate = ((Entity) result).getDelegate();
                            if (delegate instanceof WorkProduct) {
                                formatEntity(context, (WorkProduct) delegate, writer);
                            }
                        } else {
                            context.marshal(result, writer);
                        }
                    }
                }
                writer.end();
            }
            writer.end();
            return writer;
        }

        void formatEntity(MarshalContext context, WorkProduct product, ExtendedXMLStreamWriter out)
            throws XMLStreamException {

            // System.out.println("KML: Processing work product type: " + product.getProductType());
            Set<Entry> entries = WorkProductFeedUtil.getFeedEntries(product,
                context.getProperty("baseURL") + "api/workProducts/");

            // System.out.println("KML: got results: " + entries.size());
            for (Entry entry : entries) {
                out.element(KML.PLACEMARK);
                {
                    out.element(KML.NAME, entry.getTitle());
                    out.element(KML.DESCRIPTION, entry.getSummary());
                    out.element(KML.STYLE_URL, YELLOW_PUSHPIN_STYLEURL);

                    if (entry.get(GEORSS.WHERE) != null) {
                        Geometry geometry = (Geometry) entry.get(GEORSS.WHERE);
                        if (geometry != null) {
                            Point point = geometry.getCentroid();
                            double x = point.getX();
                            double y = point.getY();
                            out.element(KML.POINT);
                            {
                                out.element(KML.COORDINATES, x + "," + y);
                            }
                            out.end();
                        }
                    }
                }
                out.end();
            }
            // Map products are not currently parsed by the WorkProductFeedUtil.getFeedEntries
            // so create our own entry for it with GroundOverlays for each layer
            if (product.getProductType().equalsIgnoreCase(MapService.MapType)) {
                addWMSElement(out, product, (String) context.getProperty("baseURL"));
            }
        }

        // "REQUEST=GetMap&VERSION=1.1.0&SRS=EPSG:4326&WIDTH=[horizPixels]&HEIGHT=[vertPixels]&BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]");
        private String getGetMapQueryString(LayerType layer) {

            StringBuffer sb = new StringBuffer();
            sb.append("REQUEST=GetMap");
            if (layer.getServer().getVersion() != null) {
                sb.append("&VERSION=");
                sb.append(layer.getServer().getVersion());
            }
            if (layer.sizeOfSRSArray() > 0) {
                sb.append("&SRS=");
                sb.append(layer.getSRSArray(0));
            } else {
                sb.append("&SRS=EPSG:4326");
            }
            sb.append("&WIDTH=[horizPixels]&HEIGHT=[vertPixels]&BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]");
            String getMapQueryString = sb.toString();
            return getMapQueryString;
        }

        private String getIncidentIDFromResultsSet(Results<?> results) {

            for (Object result : results) {
                if (result instanceof Entity) {
                    Object delegate = ((Entity) result).getDelegate();
                    if (delegate instanceof WorkProduct) {
                        if (((WorkProduct) delegate).getAssociatedInterestGroupIDs().size() > 0) {
                            return ((WorkProduct) delegate).getAssociatedInterestGroupIDs().iterator().next();
                        }
                    }
                }
            }
            return null;
        }
    };

    private static class ResultsRssFeedFormatter
    extends StaxFormatter {

        @Override
        public Object format(MarshalContext context, Object input, ExtendedXMLStreamWriter writer)
            throws MarshalException, XMLStreamException {

            Results<?> results = (Results<?>) input;

            writer.element(RSS2.RSS);
            {
                writer.attribute("version", "2.0");
                writer.writeNamespace("geo", GEO.GEO_NS);
                writer.element(RSS2.CHANNEL);
                {
                    writer.element(RSS2.TITLE, "Results Feed");
                    writer.element(OPENSEARCH.TOTAL_RESULTS, results.getResultSize());
                    if (results.isPaging()) {
                        writer.element(OPENSEARCH.ITEMS_PER_PAGE, results.getPageSize());
                    }
                    // marshal entries
                    for (Object result : results) {
                        if (result instanceof Entity) {
                            Object delegate = ((Entity) result).getDelegate();
                            if (delegate instanceof WorkProduct) {
                                formatEntity(context, (WorkProduct) delegate, writer);
                            }
                        } else {
                            context.marshal(result, writer);
                        }
                    }
                }
                writer.end();
            }
            writer.end();
            return writer;
        }

        void formatEntity(MarshalContext context, WorkProduct product, ExtendedXMLStreamWriter out)
            throws XMLStreamException {

            Set<Entry> entries = WorkProductFeedUtil.getFeedEntries(product,
                context.getProperty("baseURL") + "api/workProducts/");

            for (Entry entry : entries) {
                out.element(RSS2.ITEM);
                {
                    out.element(RSS2.GUID, entry.getId());
                    // out.element(new
                    // QName("http://www.saic.com/precis/2009/06/base","AssociatedGroups"));
                    out.element("precisb",
                        "AssociatedGroups",
                        "http://www.saic.com/precis/2009/06/base");
                    out.writeNamespace("precisb", "http://www.saic.com/precis/2009/06/base");
                    out.element(new QName("http://www.saic.com/precis/2009/06/base", "Identifier"),
                        entry.getString(new QName("http://www.saic.com/precis/2009/06/base",
                            "AssociatedGroups"), null));
                    out.end();
                    out.element(RSS2.TITLE, entry.getTitle());
                    out.element(RSS2.DESCRIPTION, entry.getSummary());
                    out.element(RSS2.CATEGORY, entry.getCategory().getTerm());
                    out.element(RSS2.PUB_DATE, entry.getPublished());
                    out.element(RSS2.LAST_BUILD_DATE, entry.getUpdated());
                    // commented out until we can make the link work
                    // out.element(RSS2.LINK, entry.getLink("rel").getHref());

                    if (entry.get(GEORSS.WHERE) != null) {
                        Geometry geometry = (Geometry) entry.get(GEORSS.WHERE);
                        if (geometry != null) {
                            Point point = geometry.getCentroid();
                            double x = point.getX();
                            double y = point.getY();
                            out.element(GEO.LONG, x);
                            out.element(GEO.LAT, y);
                        }
                    }
                }
                out.end();
            }
        }
    };

    @Override
    public void configure(Configuration cfg) {

        super.configure(cfg);
        addFinder(new StaxFormatterFinder(new ResultsAtomFeedFormatter(), Results.class).property("format",
            "atom"));
        addFinder(new StaxFormatterFinder(new ResultsRssFeedFormatter(), Results.class).property("format",
            "rss"));
        addFinder(new StaxFormatterFinder(new ResultsKmlFeedFormatter(), Results.class).property("format",
            "kml"));
        cfg.require(StaxAdaptersModule.class);
    };

}
