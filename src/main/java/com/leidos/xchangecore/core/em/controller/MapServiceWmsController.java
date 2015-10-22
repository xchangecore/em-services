package com.leidos.xchangecore.core.em.controller;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.AbstractView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.leidos.xchangecore.core.em.service.MapService;
import com.leidos.xchangecore.core.em.util.GetCapabilitiesHandler;
import com.leidos.xchangecore.core.em.util.MapRenderer;
import com.leidos.xchangecore.core.em.util.XmfView;
import com.usersmarts.cx.ows.api.GetCapabilitiesRequest;
import com.usersmarts.cx.ows.api.GetCapabilitiesResponse;
import com.usersmarts.cx.ows.api.OwsRequest;
import com.usersmarts.cx.ows.api.ServiceMetadata;
import com.usersmarts.cx.wms.xml.WmsModule;
import com.usersmarts.util.Coerce;
import com.usersmarts.util.DOMUtils;
import com.usersmarts.xmf2.MarshalContext;
import com.vividsolutions.jts.geom.Envelope;

/**
 * MapServiceWmsController
 * 
 * @author Patrick Neal - Image Matters, LLC
 * @package com.saic.dctd.uicds.core.controller
 * @created Nov 24, 2008
 */
@Component(value = "mapServiceWmsController")
public class MapServiceWmsController
    extends AbstractController {

    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MapService mapService;

    MapRenderer renderer = new MapRenderer();

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
                                                 HttpServletResponse response) throws Exception {

        Map<String, String> params = request.getParameterMap();
        String requestType = "GetMap";
        for (String param : params.keySet()) {
            if (param.equalsIgnoreCase("request")) {
                requestType = request.getParameter(param);
            }
        }
        ModelAndView modelAndView = null;
        if (requestType.equals("GetCapabilities")) {
            modelAndView = handleGetCapabilities(request, response);
        } else {
            modelAndView = handleGetMap(request, response);
        }
        return modelAndView;
    }

    private ModelAndView handleGetMap(HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        ModelAndView modelAndView = new ModelAndView(new MapView());
        // parse out map identifier from url
        // format: .../api/maps/[mapId]?...
        String path = request.getPathInfo();
        String mapId = path.substring(path.lastIndexOf("/") + 1, path.length());

        if (StringUtils.isEmpty(mapId)) {
            response.sendError(400, "Must specify a map id to render as a part of the request path");
            return modelAndView;
        }

        // parse out layer names from request
        String[] layerNames = getLayerNames(request);

        // load map based on id or layer names
        Node viewContext = ("layers".equals(mapId)) ? getLayerBasedViewContext(layerNames)
                                                   : getMapService().getMap(mapId);

        if (viewContext == null) {
            response.sendError(404, "Map not found with id " + mapId);
            return modelAndView;
        }
        if (!viewContext.getNodeName().equalsIgnoreCase("ViewContext")) {
            Element stPayload = DOMUtils.getChild(viewContext, "StructuredPayload");
            viewContext = DOMUtils.getChild(stPayload, "ViewContext");
        }

        // parse out remaining necessary map request parameters
        String format = getParameter("format", request, "image/png");
        int height = getParameter("height", request, getDefaultDimension(viewContext, false));
        int width = getParameter("width", request, getDefaultDimension(viewContext, true));
        Envelope envelope = getBoundingBox(request, viewContext);

        // render the map
        Image image = renderer.render((Element) viewContext,
            layerNames,
            envelope,
            width,
            height,
            format);
        modelAndView.addObject("output", image);
        modelAndView.addObject("outputFormat", format);
        return modelAndView;
    }

    private ModelAndView handleGetCapabilities(HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {

        XmfView view = new XmfView();
        view.setMarshalContext(new MarshalContext(new WmsModule()));
        ModelAndView result = new ModelAndView(view);

        String path = request.getPathInfo();
        String mapId = path.substring(path.lastIndexOf("/") + 1, path.length());

        if (StringUtils.isEmpty(mapId)) {
            response.sendError(400, "Must specify a map id to render as a part of the request path");
            return result;
        }

        // parse out layer names from request
        String[] layerNames = getLayerNames(request);

        // load map based on id or layer names
        Node viewContext = ("layers".equals(mapId)) ? getLayerBasedViewContext(layerNames)
                                                   : getMapService().getMap(mapId);

        if (viewContext == null) {
            response.sendError(404, "Map not found with id " + mapId);
            return result;
        }
        if (!viewContext.getNodeName().equalsIgnoreCase("ViewContext")) {
            Element stPayload = DOMUtils.getChild(viewContext, "StructuredPayload");
            viewContext = DOMUtils.getChild(stPayload, "ViewContext");
        }

        GetCapabilitiesHandler handler = new GetCapabilitiesHandler();
        OwsRequest owsRequest = handler.parseQueryParameters(request, response);
        GetCapabilitiesResponse capResponse = new GetCapabilitiesResponse();
        ServiceMetadata capabilities = handler.computeMetadata((Element) viewContext,
            (GetCapabilitiesRequest) owsRequest,
            request);
        capResponse.setCapabilities(capabilities);
        result.addObject("output", capResponse);
        result.addObject("outputFormat", "application/xml");
        return result;
    }

    /**
     * Parses the WMS image bounding box parameter from the request. If the request does not specify
     * a bounding box via the "bbox" parameter, the map specified in the request will be used to
     * supply a bounding box (as defined within it)
     * 
     * @param request HttpServletRequest
     * @param viewContext Node
     * @return Envelope
     */
    private Envelope getBoundingBox(HttpServletRequest request, Node viewContext) {

        Envelope envelope = null;
        String bboxStr = getParameter("bbox", request, "");
        if (StringUtils.isNotEmpty(bboxStr)) {
            String[] bbox = bboxStr.split(",");
            envelope = new Envelope(Double.parseDouble(bbox[0]),
                                    Double.parseDouble(bbox[2]),
                                    Double.parseDouble(bbox[1]),
                                    Double.parseDouble(bbox[3]));
        } else {
            envelope = getDefaultBBOX(viewContext);
        }
        return envelope;
    }

    /**
     * @param request HttpServletRequest
     * @return String[]
     */
    private String[] getLayerNames(HttpServletRequest request) {

        String layers = getParameter("layers", request, "");
        String[] layerNames = (StringUtils.isNotEmpty(layers)) ? layers.split(",") : new String[0];
        return layerNames;
    }

    /**
     * Creates a sample ViewContext document by finding the layers stored within the system via the
     * MapService and appending them to the list of layers
     * 
     * @param layerNames String[]
     * @return Node root of the ViewContext
     * @throws Exception
     */
    private Node getLayerBasedViewContext(String[] layerNames) throws Exception {

        Node result = null;
        Document document = DOMUtils.newDocument();
        result = document.createElement("ViewContext");
        document.appendChild(result);

        Element general = document.createElement("General");
        result.appendChild(general);

        Element window = document.createElement("Window");
        window.setAttribute("width", "600");
        window.setAttribute("height", "500");
        general.appendChild(window);

        Element bbox = document.createElement("BoundingBox");
        bbox.setAttribute("minx", "-125.0");
        bbox.setAttribute("miny", "20.0");
        bbox.setAttribute("maxx", "-66.0");
        bbox.setAttribute("maxy", "50.0");
        general.appendChild(bbox);

        Element layerList = document.createElement("LayerList");
        result.appendChild(layerList);

        for (int i = 0; i < layerNames.length; ++i) {
            String layerId = layerNames[i];
            Node layer = getMapService().getLayer(layerId);
            if (layer != null) {
                layerList.appendChild(document.importNode(layer, true));
                layerNames[i] = DOMUtils.getChild(layer, "Name").getTextContent();
            } else {
                log.debug("Layer not found with id: " + layerId);
            }
        }

        return result;
    }

    /**
     * @param node Node root node in ViewContext
     * @param width boolean flag specifying whether to return the width or height (true=width,
     *            false=height)
     * @return Integer value of the specified dimension from the ViewContext
     */
    private Integer getDefaultDimension(Node node, boolean width) {

        Element general = DOMUtils.getChild(node, "General");
        Element window = DOMUtils.getChild(general, "Window");
        return Coerce.toInteger(window.getAttribute((width ? "width" : "height")), 500);
    }

    /**
     * @param node Node root node in ViewContext
     * @return Envelope
     */
    private Envelope getDefaultBBOX(Node node) {

        Element general = DOMUtils.getChild(node, "General");
        Element bbox = DOMUtils.getChild(general, "BoundingBox");
        return new Envelope(Coerce.toDouble(bbox.getAttribute("minx")),
                            Coerce.toDouble(bbox.getAttribute("maxx")),
                            Coerce.toDouble(bbox.getAttribute("miny")),
                            Coerce.toDouble(bbox.getAttribute("maxy")));
    }

    /**
     * @param name String
     * @param request HttpServletRequest
     * @param fallback String
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
     * @param name String
     * @param request HttpServletRequest
     * @param fallback Integer
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
     * @param name String
     * @param request HttpServletRequest
     * @param fallback Boolean
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

    /**
     * @param mapService
     */
    public void setMapService(MapService mapService) {

        this.mapService = mapService;
    }

    /**
     * @return
     */
    public MapService getMapService() {

        return mapService;
    }

    /**
     * MapView
     * 
     * @author Patrick Neal - Image Matters, LLC
     * @package com.saic.dctd.uicds.core.controller
     * @created Nov 24, 2008
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
