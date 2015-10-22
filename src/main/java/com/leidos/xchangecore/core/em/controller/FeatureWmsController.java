package com.leidos.xchangecore.core.em.controller;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.AbstractView;

import com.usersmarts.geo.render.GeometryRenderer;
import com.usersmarts.geo.render.ProjectionUtility;
import com.usersmarts.pub.atom.ATOM;
import com.usersmarts.pub.atom.Entry;
import com.usersmarts.pub.georss.GEORSS;
import com.usersmarts.util.Coerce;
import com.usersmarts.util.URLTemplate;
import com.usersmarts.xmf2.MarshalContext;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.leidos.xchangecore.core.em.util.AtomModule;
import com.leidos.xchangecore.core.em.util.MapRenderer;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;

/**
 * FeatureWmsController
 * 
 * @author Patrick Neal - Image Matters, LLC
 * @package com.saic.dctd.uicds.core.controller
 * @created Jan 7, 2009
 */
@Component(value = "featureWmsController")
public class FeatureWmsController
    extends AbstractController {

    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private WorkProductService workProductService;

    private final static Color BACKGROUND_COLOR = new Color(0.1F, 0.2F, 0.3F, 0.0F);
    private final static Color DRAW_COLOR = new Color(0.F, 1.F, 0.F, 1.F);
    private final static Color FILL_COLOR = new Color(0.F, 1.F, 0.F, 0.5F);;

    private final static Color LEVEL3_FILL_COLOR = new Color(1.0F, 1.0F, 0.0F, 0.3F);
    private final static Color LEVEL2_FILL_COLOR = new Color(1.0F, 0.78F, 0.0F, 0.5F);
    private final static Color LEVEL1_FILL_COLOR = new Color(1.0F, 0.0F, 0.0F, 0.5F);
    private final static Color LEVEL3_DRAW_COLOR = Color.YELLOW;
    private final static Color LEVEL2_DRAW_COLOR = Color.ORANGE;
    private final static Color LEVEL1_DRAW_COLOR = Color.RED;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {

        ModelAndView modelAndView = new ModelAndView(new MapView());

        // parse out map identifier from url
        // format: .../api/[incidentId]/features?...

        Map<QName, String> params = null;
        try {
            URLTemplate urlTemplate = new URLTemplate("api/{atom:id}/features", ATOM.ID);
            params = urlTemplate.parse(request);
        } catch (ParseException e) {
            params = new HashMap<QName, String>();
        }
        String incidentId = params.get(ATOM.ID);
        if (StringUtils.isEmpty(incidentId)) {
            response.sendError(400, "Must specify an incidentId id to render "
                                    + "as a part of the request path");
            return modelAndView;
        }

        String srs = getParameter("srs", request, "EPSG:4326");
        if (!("EPSG:4326".equals(srs))) {
            throw new Exception("Invalid SRS specified '" + srs + "'");
        }

        // parse out remaining necessary map request parameters
        String format = getParameter("format", request, "image/png");
        Dimension2D window = getDimensions(request);
        Envelope envelope = getBoundingBox(request);

        List<WorkProduct> features = null;
        String layerNames = getParameter("layers", request, "");
        if ("feature".equalsIgnoreCase(layerNames)) {
            layerNames = null;
        }
        if (StringUtils.isEmpty(layerNames)) {
            // find all features associated with the incident
            features = getWorkProductService().findByInterestGroupAndType(incidentId, "Feature");
        } else {
            // walk list of feature ids and add their WPs to the results
            features = new ArrayList<WorkProduct>();
            String[] featIds = layerNames.split(",");
            for (String featId : featIds) {
                WorkProduct feature = getWorkProductService().getProduct(featId);
                if (feature != null) {
                    features.add(feature);
                }
            }
        }

        String selected = getParameter("selected", request, getParameter("feature.selection",
            request,
            ""));

        // render features
        Image image = null;
        try {
            image = render(features, envelope, window, format, selected);
        } catch (Exception e) {
            response.sendError(500, "Error rendering features " + e.toString());
        }
        modelAndView.addObject("output", image);
        modelAndView.addObject("outputFormat", format);
        return modelAndView;
    }

    /**
     * @param entities
     * @param bbox
     * @param window
     * @param format
     * @param selected
     * @param selections
     * @return
     * @throws Exception
     */
    protected Image render(List<WorkProduct> features,
                           Envelope bbox,
                           Dimension2D window,
                           String format,
                           String selected) throws Exception {

        format = format.substring(format.indexOf("/") + 1, format.length());
        BufferedImage image = createImage(format,
            (int) window.getWidth(),
            (int) window.getHeight(),
            true);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        AffineTransform t = ProjectionUtility.total(window, bbox);
        graphics.setTransform(t);
        renderFeatures(features, selected, graphics);
        return image;
    }

    /**
     * @param features
     * @param selected
     * @param selections
     * @param graphics
     * @throws Exception
     */
    protected void renderFeatures(List<WorkProduct> features, String selected, Graphics2D graphics)
        throws Exception {

        float scale = (float) (-1.0F * graphics.getTransform().getScaleY());
        Stroke normal = new BasicStroke(0.5f / scale);
        Stroke bold = new BasicStroke(3.0f / scale);

        GeometryRenderer renderer = new GeometryRenderer();
        renderer.setPointSize(0.025f);

        // The following code assumes that features are represented as
        // GeoRSS-enabled Atom Entries.
        MarshalContext ctx = new MarshalContext(AtomModule.class);
        for (WorkProduct feature : features) {
            Geometry geometry = null;
            // TODO do we need this class ???
            InputStream in = new ByteArrayInputStream(new String(feature.getProduct().xmlText()).getBytes("UTF-8"));
            Object entry = ctx.marshal(in);
            if (entry instanceof Entry) {
                Object geom = ((Entry) entry).get(GEORSS.WHERE);
                if (geom != null && (geom instanceof Geometry || geom instanceof Envelope)) {
                    geometry = (Geometry) geom;
                    Shape shape = renderer.render(geometry);

                    if (Coerce.toString(feature.getProductID()).equals(selected)) {
                        graphics.setStroke(bold);
                    } else {
                        graphics.setStroke(normal);
                    }

                    Color fillColor = FILL_COLOR;
                    Color drawColor = DRAW_COLOR;
                    String title = ((Entry) entry).getTitle();
                    if ("Level001".equals(title)) {
                        fillColor = LEVEL1_FILL_COLOR;
                        drawColor = LEVEL1_DRAW_COLOR;
                    } else if ("Level002".equals(title)) {
                        fillColor = LEVEL2_FILL_COLOR;
                        drawColor = LEVEL2_DRAW_COLOR;
                    } else if ("Level003".equals(title)) {
                        fillColor = LEVEL3_FILL_COLOR;
                        drawColor = LEVEL3_DRAW_COLOR;
                    }
                    renderShape(shape, drawColor, fillColor, graphics);
                }
            }
        }
    }

    /**
     * @param shape
     * @param fillColor
     * @param drawColor
     * @param graphics
     */
    protected void renderShape(Shape shape, Color drawColor, Color fillColor, Graphics2D graphics) {

        if (shape == null)
            return;
        graphics.setColor(fillColor);
        graphics.fill(shape);
        graphics.setColor(drawColor);
        graphics.draw(shape);
    }

    protected BufferedImage createImage(String format, int width, int height, boolean transparent) {

        BufferedImage image;
        int imageType = transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        image = new BufferedImage(width, height, imageType);
        if ("gif".equals(format)) {
            // clear the background with transparent black.
            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setBackground(BACKGROUND_COLOR);
            graphics.clearRect(0, 0, width, height);
        }
        return image;
    }

    /**
     * @param request
     * @return
     */
    private Dimension2D getDimensions(HttpServletRequest request) {

        int height = Coerce.toInteger(getParameter("height", request, "600"));
        int width = Coerce.toInteger(getParameter("height", request, "500"));
        Dimension2D window = new Dimension(width, height);
        return window;
    }

    /**
     * Parses the WMS image bounding box parameter from the request. If the request does not specify
     * a bounding box via the "bbox" parameter, the map specified in the request will be used to
     * supply a bounding box (as defined within it)
     * 
     * @param request
     *            HttpServletRequest
     * @param viewContext
     *            Node
     * @return Envelope
     */
    private Envelope getBoundingBox(HttpServletRequest request) throws Exception {

        Envelope envelope = null;
        String bboxStr = getParameter("bbox", request, "");
        if (StringUtils.isNotEmpty(bboxStr)) {
            String[] bbox = bboxStr.split(",");
            envelope = new Envelope(Double.parseDouble(bbox[0]),
                                    Double.parseDouble(bbox[2]),
                                    Double.parseDouble(bbox[1]),
                                    Double.parseDouble(bbox[3]));
        } else {
            throw new Exception("Must include a BBOX parameter in request");
        }
        return envelope;
    }

    /**
     * @param name
     *            String
     * @param request
     *            HttpServletRequest
     * @param fallback
     *            String
     * @return String
     */
    protected String getParameter(String name, HttpServletRequest request, String fallback) {

        for (Object key : request.getParameterMap().keySet()) {
            if (((String) key).equalsIgnoreCase(name)) {
                return request.getParameter((String) key);
            }
        }
        return fallback;
    }

    /**
     * @param name
     *            String
     * @param request
     *            HttpServletRequest
     * @param fallback
     *            Integer
     * @return Integer
     */
    protected Integer getParameter(String name, HttpServletRequest request, Integer fallback) {

        for (Object key : request.getParameterMap().keySet()) {
            if (((String) key).equalsIgnoreCase(name)) {
                return Coerce.toInteger(request.getParameter((String) key), fallback);
            }
        }
        return fallback;
    }

    /**
     * @param name
     *            String
     * @param request
     *            HttpServletRequest
     * @param fallback
     *            Boolean
     * @return Boolean
     */
    protected Boolean getParameter(String name, HttpServletRequest request, Boolean fallback) {

        for (Object key : request.getParameterMap().keySet()) {
            if (((String) key).equalsIgnoreCase(name)) {
                return Coerce.toBoolean(request.getParameter((String) key), fallback);
            }
        }
        return fallback;
    }

    public void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }

    public WorkProductService getWorkProductService() {

        return workProductService;
    }

    /**
     * MapView
     * 
     * @author Patrick Neal - Image Matters, LLC
     * @package com.saic.dctd.uicds.core.controller
     * @created Jan 7, 2009
     */
    class MapView
        extends AbstractView {

        @SuppressWarnings("unchecked")
        @Override
        protected void renderMergedOutputModel(Map model,
                                               HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {

            String imageFormat = Coerce.toString(model.get("outputFormat"), "image/png");
            imageFormat = imageFormat.substring(imageFormat.indexOf("/") + 1, imageFormat.length());
            response.setContentType(imageFormat);
            OutputStream out = response.getOutputStream();

            Object output = model.get("output");
            BufferedImage result = null;
            if (output instanceof BufferedImage) {
                result = (BufferedImage) output;
            } else if (output instanceof Image) {
                Image image = (Image) output;
                result = MapRenderer.toBufferedImage(image);
            }

            if (result != null) {
                ImageIO.write(result, imageFormat, out);
            }
        }
    }

}
