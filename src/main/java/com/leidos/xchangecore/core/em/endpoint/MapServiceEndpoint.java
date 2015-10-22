package com.leidos.xchangecore.core.em.endpoint;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ws.server.endpoint.AbstractDomPayloadEndpoint;
import org.uicds.mapService.GetLayerResponseDocument;
import org.uicds.mapService.GetLayersResponseDocument;
import org.uicds.mapService.GetMapResponseDocument;
import org.uicds.mapService.GetMapsResponseDocument;
import org.uicds.mapService.SubmitLayerResponseDocument;
import org.uicds.mapService.SubmitMapResponseDocument;
import org.uicds.mapService.SubmitShapefileResponseDocument;
import org.uicds.mapService.SubmitShapefileResponseDocument.SubmitShapefileResponse;
import org.uicds.mapService.UpdateLayerResponseDocument;
import org.uicds.mapService.UpdateMapResponseDocument;
import org.uicds.workProductService.WorkProductPublicationResponseType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.leidos.xchangecore.core.em.service.MapService;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.usersmarts.util.DOMUtils;

/**
 * The XchangeCore Map Service provides a means for interacting with an XchangeCore core to manage map related
 * resources. The service defines Layer and Map resources and collections of these resources. This
 * model is based on the OGC Web Map Context specification. The operations consist of creating,
 * retrieving, updating, and deleting these resources. <BR>
 * <p>
 * The Map Service manages XchangeCore work products of type:
 * <ul>
 * <li>"MapViewContext" (OGC ViewContext)
 * <li>"LayerViewContext" (OGC Layer)
 * </ul>
 * <p>
 * An example MapViewContext work product is shown in the following example. This example shows the
 * default map that is created for each incident. It contains two layers, one is a base map (title:
 * Metacarta) and one is a WMS layer that contains location markers for each work product that is
 * submitted as a "Feature" type through the work product service (title: XchangeCore Core Map Service). A
 * layer work product is just a single OGC Layer element.
 * <p>
 * EPSG 4326 is the recommended SRS for layers that are used within XchangeCore. The XchangeCore core does not
 * access these layers so any type of Service or SRS that is valid for the OGC MapViewContext
 * specification can be added by clients. It is up to the clients that receive the MapViewContext
 * work products to be able to interpret and render the maps and layers.
 * <p>
 * As clients add layers to a MapViewContext work product they should set the BoundingBox value to
 * properly cover the extent of features that are renderable in the layers or to the correct
 * bounding box for the incident.
 * <p>
 * 
 * <pre>
 * &lt;wmc:ViewContext id=&quot;MapViewContext-6e8061e3-abb2-417f-9a1e-5074bc549e89&quot; version=&quot;1.1.0&quot; xmlns:wmc=&quot;http://www.opengis.net/context&quot;&gt;
 *   &lt;wmc:General&gt;
 *     &lt;wmc:Window height=&quot;500&quot; width=&quot;600&quot;/&gt;
 *     &lt;wmc:BoundingBox SRS=&quot;EPSG:4326&quot; maxx=&quot;-77.430125&quot; maxy=&quot;37.53350277777778&quot; minx=&quot;-77.430125&quot; miny=&quot;37.53350277777778&quot;/&gt;
 *     &lt;wmc:Title&gt;Default Map for Incident '1acb8113-b6eb-4bdb-9522-b8cf93d9dfaa'&lt;/wmc:Title&gt;
 *     &lt;wmc:KeywordList&gt;
 *       &lt;wmc:Keyword&gt;Default Map&lt;/wmc:Keyword&gt;
 *       &lt;wmc:Keyword&gt;1acb8113-b6eb-4bdb-9522-b8cf93d9dfaa&lt;/wmc:Keyword&gt;
 *       &lt;wmc:Keyword&gt;UICDS&lt;/wmc:Keyword&gt;
 *     &lt;/wmc:KeywordList&gt;
 *     &lt;wmc:Abstract&gt;Default map for incident with id '1acb8113-b6eb-4bdb-9522-b8cf93d9dfaa'&lt;/wmc:Abstract&gt;
 *   &lt;/wmc:General&gt;
 *   &lt;wmc:LayerList&gt;
 *     &lt;wmc:Layer hidden=&quot;false&quot; queryable=&quot;false&quot;&gt;
 *       &lt;wmc:Server service=&quot;OGC:WMS&quot; title=&quot;Metacarta&quot; version=&quot;1.1.0&quot;&gt;
 *         &lt;wmc:OnlineResource xlink:href=&quot;http://labs.metacarta.com/wms/vmap0&quot; xmlns:xlink=&quot;http://www.w3.org/1999/xlink&quot;/&gt;
 *       &lt;/wmc:Server&gt;
 *       &lt;wmc:Name&gt;basic&lt;/wmc:Name&gt;
 *       &lt;wmc:Title&gt;Base Map&lt;/wmc:Title&gt;
 *       &lt;wmc:SRS&gt;EPSG:4326&lt;/wmc:SRS&gt;
 *     &lt;/wmc:Layer&gt;
 *     &lt;wmc:Layer hidden=&quot;false&quot; queryable=&quot;false&quot;&gt;
 *       &lt;wmc:Server service=&quot;OGC:WMS&quot; title=&quot;UICDS Core Map Service&quot; version=&quot;1.1.0&quot;&gt;
 *         &lt;wmc:OnlineResource xlink:href=&quot;https://uicds-test3.saic.com/uicds/api/1acb8113-b6eb-4bdb-9522-b8cf93d9dfaa/features?&quot; xmlns:xlink=&quot;http://www.w3.org/1999/xlink&quot;/&gt;
 *       &lt;/wmc:Server&gt;
 *       &lt;wmc:Name/&gt;
 *       &lt;wmc:Title&gt;Incident Features&lt;/wmc:Title&gt;
 *       &lt;wmc:SRS&gt;EPSG:4326&lt;/wmc:SRS&gt;
 *     &lt;/wmc:Layer&gt;
 *   &lt;/wmc:LayerList&gt;
 * &lt;/wmc:ViewContext&gt;
 * </pre>
 * 
 * @author Christopher Lakey
 * @see <a href="../../wsdl/MapService.wsdl">Appendix: MapService.wsdl</a>
 * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
 * @idd
 */
// @Endpoint
@Transactional
public class MapServiceEndpoint
    extends AbstractDomPayloadEndpoint
    implements ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(MapServiceEndpoint.class);

    @Autowired
    private MapService mapService;

    private static final String NS = NS_MapService;
    private static final QName GET_MAP = new QName(NS, "GetMapRequest");
    private static final QName GET_MAPS = new QName(NS, "GetMapsRequest");
    private static final QName CREATE_MAP = new QName(NS, "SubmitMapRequest");
    private static final QName UPDATE_MAP = new QName(NS, "UpdateMapRequest");
    private static final QName DELETE_MAP = new QName(NS, "DeleteMapRequest");
    private static final QName GET_LAYER = new QName(NS, "GetLayerRequest");
    private static final QName GET_LAYERS = new QName(NS, "GetLayersRequest");

    private static final QName CREATE_LAYER = new QName(NS, "SubmitLayerRequest");
    private static final QName UPDATE_LAYER = new QName(NS, "UpdateLayerRequest");
    private static final QName DELETE_LAYER = new QName(NS, "DeleteLayerRequest");
    private static final QName SUBMIT_SHAPEFILE = new QName(NS, "SubmitShapefileRequest");

    @Override
    protected Element invokeInternal(Element root, Document document) throws Exception {

        String ns = root.getNamespaceURI();
        String local = root.getLocalName();
        QName name = new QName(ns, local);

        Element response = null;
        if (GET_MAP.equals(name)) {
            response = getMap(root, document);
        } else if (GET_MAPS.equals(name)) {
            response = getMaps(root, document);
        } else if (CREATE_MAP.equals(name)) {
            response = createMap(root, document);
        } else if (UPDATE_MAP.equals(name)) {
            response = updateMap(root, document);
            /*
             * } else if (DELETE_MAP.equals(name)) { response = deleteMap(root, document);
             */
        } else if (GET_LAYER.equals(name)) {
            response = getLayer(root, document);
        } else if (GET_LAYERS.equals(name)) {
            response = getLayers(root, document);
        } else if (CREATE_LAYER.equals(name)) {
            response = createLayer(root, document);
        } else if (UPDATE_LAYER.equals(name)) {
            response = updateLayer(root, document);
            /*
             * } else if (DELETE_LAYER.equals(name)) { response = deleteLayer(root, document);
             */
        } else if (SUBMIT_SHAPEFILE.equals(name)) {
            response = submitShapefile(root, document);
        }
        return response;
    }

    /**
     * Allows the client to retrieve a map work product with a specific Work Product Identification.
     * 
     * @param GetMapRequest
     * 
     * @return GetMapResponse
     * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
     * 
     * @idd
     */
    protected Element getMap(Element request, Document document) throws Exception {

        Node packageIdNode = DOMUtils.getChild(request, "WorkProductIdentification");

        WorkProduct mapProduct = getMapService().getMapWP(packageIdNode);

        GetMapResponseDocument response = GetMapResponseDocument.Factory.newInstance();
        response.addNewGetMapResponse();

        response.getGetMapResponse().addNewWorkProduct().set(WorkProductHelper.toWorkProductDocument(mapProduct).getWorkProduct());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(response.newInputStream());

        return doc.getDocumentElement();
    }

    /**
     * Allows the client to retrieve the collection of map work products associated with an
     * incident.
     * 
     * @param GetMapsRequest
     * 
     * @return GetMapsResponse
     * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
     * 
     * @idd
     */
    protected Element getMaps(Element request, Document document) throws Exception {

        Node incidentIdNode = DOMUtils.getChild(request, "IncidentId");
        String incidentId = incidentIdNode.getTextContent();

        Collection<WorkProduct> mapProducts = getMapService().getMaps(incidentId);

        GetMapsResponseDocument response = GetMapsResponseDocument.Factory.newInstance();
        response.addNewGetMapsResponse();

        for (WorkProduct mapProduct : mapProducts) {
            response.getGetMapsResponse().addNewWorkProduct().set(WorkProductHelper.toWorkProductDocument(mapProduct).getWorkProduct());
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(response.newInputStream());

        return doc.getDocumentElement();
    }

    /**
     * Allows the client to create a new Map work product and optionally associated it with an
     * incident.
     * 
     * @param SubmitMapRequest
     * 
     * @return SubmitMapResponse
     * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
     * 
     * @idd
     */
    protected Element createMap(Element request, Document document) throws Exception {

        Node incidentIdNode = DOMUtils.getChild(request, "IncidentId");
        String incidentId = null;
        if (incidentIdNode != null) {
            incidentId = incidentIdNode.getTextContent();
        }

        Node map = DOMUtils.getChild(request, "ViewContext");

        ProductPublicationStatus status = getMapService().createMap(incidentId, map);

        WorkProductPublicationResponseType returnStatus = WorkProductHelper.toWorkProductPublicationResponse(status);

        SubmitMapResponseDocument responseDoc = SubmitMapResponseDocument.Factory.newInstance();
        responseDoc.addNewSubmitMapResponse().addNewWorkProductPublicationResponse().set(returnStatus);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(responseDoc.newInputStream());

        return doc.getDocumentElement();
    }

    /**
     * Allows the client to update an existing Map work product.
     * 
     * @param UpdateMapRequest
     * 
     * @return UpdateMapResponse
     * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
     * 
     * @idd
     */
    protected Element updateMap(Element request, Document document) throws Exception {

        // Node incidentIdNode = DOMUtils.getChild(request, "incidentId");
        // String incidentId = incidentIdNode.getTextContent();
        // Node mapIdNode = DOMUtils.getChild(request, "mapId");
        // String mapId = mapIdNode.getTextContent();
        // Node mapNode = DOMUtils.getChild(request, "map");
        // Node map = DOMUtils.getChild(mapNode, "ViewContext");

        Node packageIdNode = DOMUtils.getChild(request, "WorkProductIdentification");
        Node map = request.getElementsByTagNameNS("http://www.opengis.net/context", "ViewContext").item(0);

        ProductPublicationStatus status = getMapService().updateMap(packageIdNode, map);

        WorkProductPublicationResponseType returnStatus = WorkProductHelper.toWorkProductPublicationResponse(status);

        UpdateMapResponseDocument responseDoc = UpdateMapResponseDocument.Factory.newInstance();
        responseDoc.addNewUpdateMapResponse().addNewWorkProductPublicationResponse().set(returnStatus);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(responseDoc.newInputStream());

        return doc.getDocumentElement();
    }

    /**
     * Allows the client to delete an existing Map work product.
     * 
     * @param DeleteMapRequest
     * 
     * @return DeleteMapResponse
     * 
     * @idd protected Element deleteMap(Element request, Document document) throws Exception {
     * 
     *      Node incidentIdNode = DOMUtils.getChild(request, "incidentId"); String incidentId =
     *      incidentIdNode.getTextContent(); Node mapIdNode = DOMUtils.getChild(request, "mapId");
     *      String mapId = mapIdNode.getTextContent();
     * 
     *      ProductPublicationStatus status = getMapService().deleteMap(incidentId, mapId);
     * 
     *      WorkProductPublicationResponseType returnStatus =
     *      WorkProductHelper.toWorkProductPublicationResponse(status);
     * 
     *      DeleteMapResponseDocument responseDoc = DeleteMapResponseDocument.Factory.newInstance();
     *      responseDoc .addNewDeleteMapResponse
     *      ().addNewWorkProductPublicationResponse().set(returnStatus);
     * 
     *      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
     *      dbf.setNamespaceAware(true); DocumentBuilder db = dbf.newDocumentBuilder(); Document doc
     *      = db.parse(responseDoc.newInputStream());
     * 
     *      return doc.getDocumentElement(); }
     */

    /**
     * Allows the client to retrieve a layer work product with a specific Work Product
     * Identification.
     * 
     * @param GetLayerRequest
     * 
     * @return GetLayerResponse
     * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
     * 
     * @idd
     */
    protected Element getLayer(Element request, Document document) throws Exception {

        // Node incidentIdNode = DOMUtils.getChild(request, "IncidentId");
        // String incidentId = incidentIdNode.getTextContent();
        // Node layerIdNode = DOMUtils.getChild(request, "LayerId");
        // String layerId = layerIdNode.getTextContent();

        Node packageIdNode = DOMUtils.getChild(request, "WorkProductIdentification");

        WorkProduct layerProduct = getMapService().getLayerWP(packageIdNode);

        if (layerProduct != null) {
            GetLayerResponseDocument response = GetLayerResponseDocument.Factory.newInstance();
            response.addNewGetLayerResponse();

            response.getGetLayerResponse().addNewWorkProduct().set(WorkProductHelper.toWorkProductDocument(layerProduct).getWorkProduct());

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(response.newInputStream());

            return doc.getDocumentElement();
        } else {
            throw new Exception("Layer Not Found ");
        }

        // Element newRoot = document.createElementNS(NS, "GetLayerResponse");
        // if (node != null) {
        // newRoot.appendChild(document.importNode(node, true));
        // }
        // return newRoot;
    }

    /**
     * Allows the client to retrieve the collection of layer work products associated with an
     * incident.
     * 
     * @param GetLayersRequest
     * 
     * @return GetLayersResponse
     * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
     * 
     * @idd
     */
    protected Element getLayers(Element request, Document document) throws Exception {

        Node incidentIdNode = DOMUtils.getChild(request, "IncidentId");
        String incidentId = incidentIdNode.getTextContent();

        Collection<WorkProduct> layerProducts = getMapService().getLayers(incidentId);

        GetLayersResponseDocument response = GetLayersResponseDocument.Factory.newInstance();
        response.addNewGetLayersResponse();

        for (WorkProduct layerProduct : layerProducts) {
            response.getGetLayersResponse().addNewWorkProduct().set(WorkProductHelper.toWorkProductDocument(layerProduct).getWorkProduct());
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(response.newInputStream());

        return doc.getDocumentElement();

        // for (String layerId : layerIds) {
        // Node layer = getMapService().getLayer(incidentId, layerId);
        // Element layerNode = document.createElementNS(NS, "layer");
        // layerNode.appendChild(document.importNode(incidentIdNode, true));
        // Element layerIdNode = document.createElementNS(NS, "layerId");
        // layerIdNode.setTextContent(layerId);
        // layerNode.appendChild(layerIdNode);
        // layerNode.appendChild(document.importNode(layer, true));
        // newRoot.appendChild(layerNode);
        // }
        // return newRoot;
    }

    /**
     * Allows the client to create a new Layer work product.
     * 
     * @param SubmitLayerRequest
     * 
     * @return SubmitLayerResponse
     * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
     * 
     * @idd
     */
    protected Element createLayer(Element request, Document document) throws Exception {

        Node incidentIdNode = DOMUtils.getChild(request, "IncidentId");
        String incidentId = null;
        if (incidentIdNode != null) {
            incidentId = incidentIdNode.getTextContent();
        }

        Node layer = DOMUtils.getChild(request, "Layer");

        // String layerId =
        ProductPublicationStatus status = getMapService().createLayer(incidentId, layer);

        WorkProductPublicationResponseType returnStatus = WorkProductHelper.toWorkProductPublicationResponse(status);

        SubmitLayerResponseDocument responseDoc = SubmitLayerResponseDocument.Factory.newInstance();
        responseDoc.addNewSubmitLayerResponse().addNewWorkProductPublicationResponse().set(returnStatus);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(responseDoc.newInputStream());

        return doc.getDocumentElement();
    }

    /**
     * Allows the client to update an existing Layer work product.
     * 
     * @param UpdateLayerRequest
     * 
     * @return UpdateLayerResponse
     * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
     * 
     * @idd
     */
    protected Element updateLayer(Element request, Document document) throws Exception {

        // Node incidentIdNode = DOMUtils.getChild(request, "IncidentId");
        // String incidentId = incidentIdNode.getTextContent();
        // Node layerIdNode = DOMUtils.getChild(request, "LayerId");
        // String layerId = layerIdNode.getTextContent();
        // Node layerNode = DOMUtils.getChild(request, "Layer");

        Node packageIdNode = DOMUtils.getChild(request, "WorkProductIdentification");
        Node layer = DOMUtils.getChild(request, "Layer");

        ProductPublicationStatus status = getMapService().updateLayer(packageIdNode, layer);

        // layer = getMapService().getLayer(incidentId, layerId);
        //
        // Element newRoot = document.createElementNS(NS,
        // "UpdateLayerResponse");
        // layerNode = document.createElementNS(NS, "layer");
        // layerNode.appendChild(document.importNode(incidentIdNode, true));
        // layerNode.appendChild(document.importNode(layerIdNode, true));
        // layerNode.appendChild(document.importNode(layer, true));
        // newRoot.appendChild(layerNode);

        WorkProductPublicationResponseType returnStatus = WorkProductHelper.toWorkProductPublicationResponse(status);

        UpdateLayerResponseDocument responseDoc = UpdateLayerResponseDocument.Factory.newInstance();
        responseDoc.addNewUpdateLayerResponse().addNewWorkProductPublicationResponse().set(returnStatus);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(responseDoc.newInputStream());

        return doc.getDocumentElement();
    }

    /**
     * Allows the client to delete an existing Layer work product.
     * 
     * @param DeleteLayerRequest
     * 
     * @return DeleteLayerResponse
     * 
     * @idd protected Element deleteLayer(Element request, Document document) throws Exception {
     * 
     *      Node incidentIdNode = DOMUtils.getChild(request, "incidentId"); String incidentId =
     *      incidentIdNode.getTextContent(); Node layerIdNode = DOMUtils.getChild(request,
     *      "layerId"); String layerId = layerIdNode.getTextContent();
     * 
     *      ProductPublicationStatus status = getMapService().deleteLayer(incidentId, layerId);
     * 
     *      WorkProductPublicationResponseType returnStatus =
     *      WorkProductHelper.toWorkProductPublicationResponse(status);
     * 
     *      DeleteLayerResponseDocument responseDoc =
     *      DeleteLayerResponseDocument.Factory.newInstance(); responseDoc.addNewDeleteLayerResponse
     *      ().addNewWorkProductPublicationResponse().set( returnStatus);
     * 
     *      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
     *      dbf.setNamespaceAware(true); DocumentBuilder db = dbf.newDocumentBuilder(); Document doc
     *      = db.parse(responseDoc.newInputStream());
     * 
     *      return doc.getDocumentElement(); }
     */

    /**
     * Allows the client to create a new Shapefile, Layer, and Feature work products from the
     * supplied shapefile and optionally associated it to an incident. The submitted shapefile must
     * be a zip file that contains .shp, .shx, .prj, dbf, and .avl files and base64 encoded in the
     * ContentData parameter. A UCore Digest should be supplied with a shapefile that contains an
     * Event that describes the shapefile, a Location with a boundary of the shapefile, along with a
     * LocatedAt element to associatd the Event and Location elements.
     * 
     * @param SubmitShapefileRequest
     * 
     * @return SubmitShapefileResponse
     * @see <a href="../../services/Map/0.1/MapService.xsd">Appendix: MapService.xsd</a>
     * 
     * @idd
     */
    protected Element submitShapefile(Element request, Document document) throws Exception {

        Node incidentIdNode = DOMUtils.getChild(request, "IncidentId");
        String incidentId = null;
        if (incidentIdNode != null) {
            incidentId = incidentIdNode.getTextContent();
        }

        Node contentNode = DOMUtils.getChild(request, "ContentData");
        byte[] bytes = contentNode.getTextContent().getBytes();
        bytes = Base64.decodeBase64(bytes);

        List<ProductPublicationStatus> statusObjs = getMapService().submitShapefile(incidentId,
            bytes);

        SubmitShapefileResponseDocument responseDoc = SubmitShapefileResponseDocument.Factory.newInstance();
        SubmitShapefileResponse response = responseDoc.addNewSubmitShapefileResponse();
        for (ProductPublicationStatus statusObj : statusObjs) {
            WorkProductPublicationResponseType returnStatus = WorkProductHelper.toWorkProductPublicationResponse(statusObj);
            response.addNewWorkProductPublicationResponse().set(returnStatus);
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(responseDoc.newInputStream());

        return doc.getDocumentElement();
    }

    void setMapService(MapService mapService) {

        this.mapService = mapService;
    }

    MapService getMapService() {

        return this.mapService;
    }
}
