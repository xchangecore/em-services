package com.leidos.xchangecore.core.em.service;

import java.util.Collection;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;

import com.leidos.xchangecore.core.em.messages.IncidentStateNotificationMessage;
import com.leidos.xchangecore.core.infrastructure.messages.ProductChangeNotificationMessage;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;

/**
 * The Map Service manages Layer and MapViewContext type work products. It also can pull all the
 * features from a Shapefile and create Feature type work products for each of them. Each Feature
 * type work product is then available via an WMS layer that is automatically added to the default
 * Map that is created for every incident.
 * 
 * @author Patrick Neal - Image Matters, LLC
 * @created: Nov 7, 2008
 * @package: com.saic.dctd.uicds.core.service
 * @ssdd
 */
@Transactional
public interface MapService {

    public final static String MapType = "MapViewContext";
    public final static String LayerType = "LayerViewContext";
    public final static String FeatureType = "Feature";
    public final static String AlertType = "Alert";
    public static final String MAP_SERVICE_NAME = "MapService";

    /**
     * Get a ViewContext representation of a specific map.
     * 
     * @param packageId Package identification node
     * @return WorkProductDocument Node
     * @throws Exception
     * @ssdd
     */
    public Node getMap(Node packageId) throws Exception;

    /**
     * Get a ViewContext representation of the latest version of a map
     * 
     * @param workProductId work product id of the map to get
     * @return WorkProduct
     * @throws Exception
     * @ssdd
     */
    public WorkProduct getMapWP(String workProductId) throws Exception;

    /**
     * Get a ViewContext representation of a specific map.
     * 
     * @param packageId Package identification node
     * @return WorkProduct
     * @throws Exception
     * @ssdd
     */
    public WorkProduct getMapWP(Node packageId) throws Exception;

    /**
     * Get a ViewContext representation of the latest version of a map
     * 
     * @param workProductId work product id of the map to get
     * @return WorkProductDocument Node
     * @throws Exception
     * @ssdd
     */
    public Node getMap(String workProductId) throws Exception;

    /**
     * Get a ViewContext representation of a specific layer.
     * 
     * @param packageId Package identification node
     * @return WorkProductDocument Node
     * @throws Exception
     * @ssdd
     */
    public Node getLayer(Node packageId) throws Exception;

    /**
     * Get a ViewContext Layer representation of the latest version of a layer.
     * 
     * @param workProductId work product id of the map to get
     * @return WorkProduct
     * @throws Exception
     * @ssdd
     */

    public WorkProduct getLayerWP(String workProductId) throws Exception;

    /**
     * Get a ViewContext representation of a specific layer.
     * 
     * @param packageId Package identification node
     * @return WorkProduct
     * @throws Exception
     * @ssdd
     */
    public WorkProduct getLayerWP(Node packageId) throws Exception;

    /**
     * Get a ViewContext Layer representation of the latest version of a layer.
     * 
     * @param workProductId work product id of the map to get
     * @return WorkProductDocument Node
     * @throws Exception
     * @ssdd
     */

    public Node getLayer(String workProductId) throws Exception;

    /**
     * Get the set of maps work products associated with an incident.
     * 
     * @param incidentId
     * @return collection of WorkProduct objects
     * @throws Exception
     * @ssdd
     */
    public Collection<WorkProduct> getMaps(String incidentId) throws Exception;

    /**
     * Get the set of map layer work products associated with an incident.
     * 
     * @param incidentId
     * @return collection of WorkProduct objects
     * @throws Exception
     * @ssdd
     */
    public Collection<WorkProduct> getLayers(String incidentId) throws Exception;

    /**
     * Submit a ViewContext to create a new map for the specified incident.
     * 
     * @param incidentId
     * @param map
     * @return productPublicationStatus
     * @throws Exception
     * @ssdd
     */
    public ProductPublicationStatus createMap(String incidentId, Node map) throws Exception;

    /**
     * Mark map as deleted from the incident.
     * 
     * @param incidentId
     * @param mapId
     * @throws Exception
     * @ssdd
     */
    public ProductPublicationStatus deleteMap(String incidentId, String mapId) throws Exception;

    /**
     * Update an existing map with the specified ViewContext document.
     * 
     * @param pkgId
     * @param map
     * @throws Exception
     * @ssdd
     */
    public ProductPublicationStatus updateMap(Node packageId, Node map) throws Exception;

    /**
     * Submit a ViewContext Layer to create a new layer for the specified incident.
     * 
     * @param incidentId
     * @param layer
     * @return productPublicationStatus
     * @throws Exception
     * @ssdd
     */
    public ProductPublicationStatus createLayer(String incidentId, Node layer) throws Exception;

    /**
     * Mark layer as deleted from the incident.
     * 
     * @param incidentId
     * @param layerId
     * @throws Exception
     * @ssdd
     */
    public ProductPublicationStatus deleteLayer(String incidentId, String layerId) throws Exception;

    /**
     * Update an existing layer with the specified ViewContext Layer representation.
     * 
     * @param pkgId
     * @param layer
     * @throws Exception
     * @ssdd
     */
    public ProductPublicationStatus updateLayer(Node packageId, Node layer) throws Exception;

    /**
     * Submit an archive containing a shapefile and necessary supplement files to be used to create
     * Features within the specified incident
     * 
     * @param incidentId String identifier
     * @param bytes byte[] representing the .shz binary contents of the request
     * @return List of ProductPublicationStatus for 1) the shapefile product, 2) the list of
     *         features products, 3) the layer product associated with the features
     * @throws Exception
     * @ssdd
     */
    public List<ProductPublicationStatus> submitShapefile(String incidentId, byte[] bytes)
        throws Exception;

    /**
     * SystemIntialized Message Handler
     * 
     * @param message SystemInitialized message
     * @see applicationContext
     * @ssdd
     */
    public void systemInitializedHandler(String messgae);

    /**
     * Handles notifications when Work Products are changed. We're only interested in those of type
     * "Feature"
     * 
     * @param message ProductChangeNotificationMessage
     * @ssdd
     */
    public void productChangeNotificationHandler(ProductChangeNotificationMessage message);

    /**
     * Handles notifications when new Incidents have been created in the system. Creates a default
     * map for the incident
     * 
     * @param message Message of type IncidentStateNotificationMessage
     * @ssdd
     */
    public void handleIncidentState(IncidentStateNotificationMessage message);
}
