package com.leidos.xchangecore.core.em.service;

import org.springframework.transaction.annotation.Transactional;

import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert;

import com.leidos.xchangecore.core.infrastructure.exceptions.InvalidXpathException;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.precis.x2009.x06.base.NamespaceMapType;

/**
 * The Alert service manages Alert work products that conform to the CAP version 1.1 specification.
 * It creates a Digest a digest for the input work product.
 * 
 * @since 1.0
 * @ssdd
 * 
 */
@Transactional
public interface AlertService {

    public final static String Type = "Alert";
    public static final String ALERT_SERVICE_NAME = "AlertService";

    /**
     * Allows the client to cancel an existing alert
     * 
     * @param alertID
     * @ssdd
     */
    public ProductPublicationStatus cancelAlert(String wpId);

    /**
     * Allows the client to create an alert
     * 
     * @param alert
     * @see Alert
     * @return status of the create request
     * @see ProductPublicationStatus
     * @ssdd
     */
    public ProductPublicationStatus createAlert(String incidentId, Alert alert);

    /**
     * Allows the client to get an alert
     * 
     * @param wpId unique identifier of alert to be retrieved
     * @return alert - the alert that is retrieved
     * @see WorkProduct
     * @ssdd
     */
    public WorkProduct getAlert(String wpId);

    /**
     * Allows the client to get an alert
     * 
     * @param alertID unique identifier of alert to be retrieved
     * @return alert - the alert that is retrieved
     * @see WorkProduct
     * @ssdd
     */
    public WorkProduct getAlertByAlertId(String alertId);

    /**
     * Allows the client to get a list of alerts by the query if specified
     * 
     * @param query type for all the alerts
     * @return list
     * @see Alert
     * @ssdd
     */
    public WorkProduct[] getListOfAlerts(String queryType, NamespaceMapType namespaceMap)
        throws InvalidXpathException;

    /**
     * SystemIntialized Message Handler
     * 
     * @param message SystemInitialized message
     * @return void
     * @see applicationContext
     * @ssdd
     */
    public void systemInitializedHandler(String message);

}
