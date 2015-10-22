package com.leidos.xchangecore.core.em.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.directoryServiceData.WorkProductTypeListType;

import x1.oasisNamesTcEmergencyCap1.AlertDocument;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert;

import com.leidos.xchangecore.core.em.service.AlertService;
import com.leidos.xchangecore.core.em.util.EMDigestHelper;
import com.leidos.xchangecore.core.infrastructure.exceptions.InvalidXpathException;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.NamespaceMapItemType;
import com.saic.precis.x2009.x06.base.NamespaceMapType;

/**
 * The AlertService implementation.
 *
 * @since 1.0
 * @see com.leidos.xchangecore.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class AlertServiceImpl
implements AlertService, ServiceNamespaces {

    private final Logger logger = LoggerFactory.getLogger(AlertServiceImpl.class);

    private DirectoryService directoryService;

    private WorkProductService workProductService;

    /**
     * Cancel alert deletes the work product identified by the workproduct id string
     *
     * @param workProductId
     *            the work product id
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus cancelAlert(String workProductId) {

        logger.info("work product id to cancel: " + workProductId);

        final WorkProduct wp = getWorkProductService().getProduct(workProductId);

        ProductPublicationStatus status;

        if (wp == null) {
            status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(workProductId + " doesn't existed");
            return status;
        }

        // if it's still not closed, we need to close it first
        if (wp.isActive() == true) {
            status = getWorkProductService().closeProduct(
                WorkProductHelper.getWorkProductIdentification(wp));
            if (status.getStatus().equals(ProductPublicationStatus.FailureStatus))
                return status;
        }

        return getWorkProductService().archiveProduct(
            WorkProductHelper.getWorkProductIdentification(wp));
    }

    /**
     * Creates a workproduct of alert type. Adds the supplied incident id to the set of associated
     * interest groups
     *
     * @param incidentId
     *            the incident id
     * @param alert
     *            the alert
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createAlert(String incidentId, Alert alert) {

        // log.info("Received New Alert Message:\n" + alert.toString());
        // Check for msgType of incoming Alert
        // If it is Cancel then look in the References data to find one or more
        // WorkProductIdentifers
        // In references - one or more Cap Alerts are identified with the attributes
        // sender,identifier,sent for each Cap Alert separated by spaces
        //
        final AlertDocument alertDoc = AlertDocument.Factory.newInstance();
        alertDoc.setAlert(alert);
        final String msgType = alertDoc.getAlert().getMsgType().toString();

        if (msgType.equalsIgnoreCase("Cancel")) {
            ProductPublicationStatus status = new ProductPublicationStatus();

            logger.info("New Alert is of type Cancel, ");

            // log.info("References Data: " + alertDoc.getAlert().getReferences());

            // get info from references and parse for Alert ID's
            final String references = alertDoc.getAlert().getReferences();
            final String prefix = "";
            final String[] alerts = references.substring(prefix.length()).split(" ");
            final String[] alertIDs = new String[alerts.length];

            for (int i = 0; i < alerts.length; i++) {
                logger.info("Alert Record: " + alerts[i]);
                final String[] eachAlert = alerts[i].split(",");
                for (int j = 0; j < eachAlert.length; j++)
                    if (j == 1) { // second item
                        logger.info("Alert ID: " + eachAlert[j]);
                        alertIDs[i] = eachAlert[j];
                    }

            }

            // use the alert ids to cancel each alert
            logger.info("looping to cancel each alert id");
            for (final String z : alertIDs) {
                final String wpIdentifier = z;
                final WorkProduct wp = getAlertByAlertId(wpIdentifier);
                if (wp != null) {
                    logger.info("calling cancel alert for: ");
                    logger.info("wp ID: " + wp.getProductID());
                    status = cancelAlert(wp.getProductID());
                    logger.info("Status: " + status.getStatus());

                } else
                    logger.info("Alert: " + wpIdentifier + " not found on core to delete");
            }
            return status;
        }

        final WorkProduct wp = new WorkProduct();
        wp.setProductType(AlertService.Type);
        wp.setProduct(alertDoc);

        if (incidentId != null)
            wp.associateInterestGroup(incidentId);

        // get digest byte array
        wp.setDigest(new EMDigestHelper(alert).getDigest());

        final ProductPublicationStatus status = workProductService.publishProduct(wp);

        return status;

    }

    private WorkProduct findAlertWP(String alertID) {

        final List<WorkProduct> productList = getWorkProductService().listByProductType(
            AlertService.Type);
        for (final WorkProduct product : productList)
            try {
                final AlertDocument alertDocument = (AlertDocument) product.getProduct();

                if (alertDocument.getAlert().getIdentifier().equals(alertID))
                    return product;
            } catch (final Exception e) {
                logger.error("Not Valid Alert Document:\n" + product.getProduct().xmlText());
            }
        return null;
    }

    /**
     * Gets the alert using the workproduct id string
     *
     * @param wpID
     *            the wp id
     * @return the alert
     * @ssdd
     */
    @Override
    public WorkProduct getAlert(String wpID) {

        return getWorkProductService().getProduct(wpID);
    }

    /**
     * Gets the alert by alert id.
     *
     * @param alertId
     *            the alert id
     * @return the alert by alert id
     * @ssdd
     */
    @Override
    public WorkProduct getAlertByAlertId(String alertId) {

        return findAlertWP(alertId);
    }

    public DirectoryService getDirectoryService() {

        return directoryService;
    }

    /**
     * Gets the list of alerts.
     *
     * @param queryType
     *            the query type
     * @param namespaceMap
     *            the namespace map
     * @return the list of alerts
     * @ssdd
     */
    @Override
    public WorkProduct[] getListOfAlerts(String queryType, NamespaceMapType namespaceMap)
        throws InvalidXpathException {

        final Map<String, String> mapNamespaces = new HashMap<String, String>();
        if (namespaceMap != null)
            for (final NamespaceMapItemType ns : namespaceMap.getItemArray())
                mapNamespaces.put(ns.getPrefix(), ns.getURI());

        /*
         * Get a list of WP by Type from Work Product and use that list to match the alertID
         */
        final List<WorkProduct> listOfProducts = getWorkProductService().getProductByTypeAndXQuery(
            AlertService.Type, queryType, mapNamespaces);
        if (listOfProducts != null && listOfProducts.size() > 0) {
            final WorkProduct[] products = new WorkProduct[listOfProducts.size()];
            return listOfProducts.toArray(products);
        } else
            return null;
    }

    public WorkProductService getWorkProductService() {

        return workProductService;
    }

    public void setDirectoryService(DirectoryService service) {

        directoryService = service;
    }

    public void setWorkProductService(WorkProductService service) {

        workProductService = service;
    }

    /**
     * System initialized handler.
     *
     * @param message
     *            the message
     */
    @Override
    public void systemInitializedHandler(String message) {

        logger.debug("systemInitializedHandler: ... start ...");
        final WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(AlertService.Type);
        directoryService.registerUICDSService(NS_AgreementService, ALERT_SERVICE_NAME, typeList,
            typeList);
        logger.debug("systemInitializedHandler: ... done ...");
    }
}
