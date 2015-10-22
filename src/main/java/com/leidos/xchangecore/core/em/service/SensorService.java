package com.leidos.xchangecore.core.em.service;

import org.springframework.transaction.annotation.Transactional;
import org.uicds.sensorService.SensorObservationInfoDocument.SensorObservationInfo;

import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The SensorService manages SOS type work products that are OGC-SOS documents.
 * 
 * @author Aruna Hau
 * @since 1.0
 * @ssdd
 * 
 */
@Transactional
public interface SensorService {

    public final static String Type = "SOI";
    public static final String SENSOR_SERVICE_NAME = "SensorService";

    /**
     * Creates an Sensor Observation Info (SOI) work product .
     * 
     * @param soi
     * @see SensorObservationInfo
     * @param incidentID (optional)
     * 
     * @return status of the create request
     * @see ProductPublicationStatus
     * @ssdd
     */
    public ProductPublicationStatus createSOI(SensorObservationInfo soi, String incidentID);

    /**
     * Deletes an Sensor Observation Info (SOI) work product .
     * 
     * @param productID : String
     * 
     * @return work product ID : String
     * @ssdd
     */
    public ProductPublicationStatus deleteSOI(String productID);

    /**
     * Updates an Sensor Observation Info (SOI) work product .
     * 
     * @param productID : String
     * @param soi
     * @see SensorObservationInfo
     * 
     * @return status of the create request
     * @see ProductPublicationStatus
     * @ssdd
     */
    public ProductPublicationStatus updateSOI(SensorObservationInfo soi, IdentificationType pkgId);

    /**
     * Returns a list of Sensor Observation Info (SOI) work products associated with a given
     * incident .
     * 
     * @param inciden ID : String
     * 
     * @return list of of SOI work products
     * @see WorkProduct
     * @ssdd
     */
    public WorkProduct[] getSOIList(String incidentID);

    /**
     * Returns an Sensor Observation Info (SOI) work product .
     * 
     * @param productID : String
     * 
     * @return work productID
     * @ssdd
     */
    public WorkProduct getSOI(String productID);

    /**
     * SystemIntialized Message Handler
     * 
     * @param message SystemInitialized message
     * @return void
     * @see applicationContext
     * @ssdd
     */
    public void systemInitializedHandler(String messgae);

}
