package com.leidos.xchangecore.core.em.service.impl;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.incident.UICDSIncidentType;

import com.leidos.xchangecore.core.em.dao.LEITSCIncidentDAO;
import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageException;
import com.leidos.xchangecore.core.em.exceptions.DetailedCFSMessageXMLException;
import com.leidos.xchangecore.core.em.exceptions.LEITSCIncidentPublicationException;
import com.leidos.xchangecore.core.em.model.LEITSCIncident;
import com.leidos.xchangecore.core.em.service.IncidentManagementService;
import com.leidos.xchangecore.core.em.service.LEITSCService;
import com.leidos.xchangecore.core.em.util.DetailedCFSMessageReader;
import com.leidos.xchangecore.core.em.util.DetailedCFSMessageUtil;
import com.leidos.xchangecore.core.em.util.IncidentUtil;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The LEITSCService implementation
 *
 * @see com.leidos.xchangecore.core.em.model.LEITSCIncident LEITSCIncident Data Model
 * @see com.leidos.xchangecore.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class LEITSCServiceImpl implements LEITSCService {

    Logger log = LoggerFactory.getLogger(LEITSCServiceImpl.class);

    private LEITSCIncidentDAO leitscIncidentDAO;

    private IncidentManagementService incidentManagementService;

    private String clearLEITSCIncident(String leitscIncidentID, DetailedCFSMessageReader reader)
            throws LEITSCIncidentPublicationException {

        LEITSCIncident leitscIncident = leitscIncidentDAO.findByLEITSCIncident(leitscIncidentID);
        if (leitscIncident != null) {
            log.debug("===> clearLEITSCIncident: close uicds incident ID="
                    + leitscIncident.getIncidentID());
            ProductPublicationStatus status = incidentManagementService
                    .closeIncident(leitscIncident.getIncidentID());
            if ((status != null)
                    && status.getStatus().equalsIgnoreCase(ProductPublicationStatus.SuccessStatus)) {
                log.debug("===> clearLEITSCIncident: archive uicds incident ID="
                        + leitscIncident.getIncidentID());
                status = incidentManagementService.archiveIncident(leitscIncident.getIncidentID());
                if ((status != null)
                        && status.getStatus().equalsIgnoreCase(
                                ProductPublicationStatus.SuccessStatus)) {
                    log.debug("===> clearLEITSCIncident: remove LEITSC incident , with  LEITSC incident ID="
                            + leitscIncident
                            + " and uicds incident ID="
                            + leitscIncident.getIncidentID());
                    leitscIncidentDAO.makeTransient(leitscIncident);
                    return leitscIncidentID;
                } else {
                    String reasonForFailure = (status == null) ? "Unkown" : status
                            .getReasonForFailure();
                    log.error("postDetailedCFSMessage - failed to retire leitsc incident "
                            + leitscIncidentID + " reason=" + reasonForFailure);
                    throw new LEITSCIncidentPublicationException(leitscIncidentID, "retire",
                            reasonForFailure);
                }
            } else {
                // do something here
                String reasonForFailure = (status == null) ? "Unkown" : status
                        .getReasonForFailure();
                log.error("postDetailedCFSMessage - failed to retire leitsc incident "
                        + leitscIncidentID + " reason=" + reasonForFailure);
                throw new LEITSCIncidentPublicationException(leitscIncidentID, "retire",
                        reasonForFailure);
            }
        } else {
            String reasonForFailure = " leitscIncident with ID" + leitscIncidentID
                    + " does NOT exist";
            log.error("postDetailedCFSMessage - failed to retire leitsc incident "
                    + leitscIncidentID + " reason=" + reasonForFailure);
            throw new LEITSCIncidentPublicationException(leitscIncidentID, "retire",
                    reasonForFailure);
        }
    }

    private String createLEITSCIncident(String leitscIncidentID, DetailedCFSMessageReader reader)
            throws LEITSCIncidentPublicationException, DetailedCFSMessageXMLException,
            DetailedCFSMessageException, XmlException {

        LEITSCIncident existingLeitscIncident = leitscIncidentDAO
                .findByLEITSCIncident(leitscIncidentID);
        if (existingLeitscIncident == null) {
            UICDSIncidentType incident = DetailedCFSMessageUtil.createUICDSIncident();
            incident = DetailedCFSMessageUtil.populateIncident(incident, reader);
            ProductPublicationStatus status = incidentManagementService.createIncident(incident);
            if ((status != null)
                    && status.getStatus().equalsIgnoreCase(ProductPublicationStatus.SuccessStatus)) {
                LEITSCIncident leitscIncident = new LEITSCIncident();
                leitscIncident.setLeitscIncidentID(leitscIncidentID);
                leitscIncident.setIncidentID(status.getProduct()
                        .getFirstAssociatedInterestGroupID());
                leitscIncident.setIncidentWPID(status.getProduct().getProductID());
                log.debug("===> createLEITSCIncident:  uicds incident ID="
                        + status.getProduct().getFirstAssociatedInterestGroupID() + " wpID="
                        + status.getProduct().getProductID()
                        + " was created for LEITSC incident ID=" + leitscIncidentID);
                leitscIncidentDAO.makePersistent(leitscIncident);

            } else {
                // do something here
                String reasonForFailure = (status == null) ? "Unkown" : status
                        .getReasonForFailure();
                log.error("postDetailedCFSMessage - failed to create leitsc incident "
                        + leitscIncidentID + " reason=" + reasonForFailure);
                throw new LEITSCIncidentPublicationException(leitscIncidentID, "create",
                        reasonForFailure);
            }
            return leitscIncidentID;
        } else {
            String reasonForFailure = " LEITSC incident ID=" + leitscIncidentID + "already exists";
            log.error("postDetailedCFSMessage - failed to create leitsc incident "
                    + leitscIncidentID + " reason=" + reasonForFailure);
            throw new LEITSCIncidentPublicationException(leitscIncidentID, "create",
                    reasonForFailure);
        }

    }

    public IncidentManagementService getIncidentManagementService() {

        return incidentManagementService;
    }

    /**
     * Gets the LEITSC incident.
     *
     * @param leitscIncidentID
     *            the LEITSC incident id
     *
     * @return the LEITSC incident
     * @ssdd
     */
    @Override
    public com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct getLEITSCIncident(
            String leitscIncidentID) {

        com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct product = null;
        LEITSCIncident leitscIncident = leitscIncidentDAO.findByLEITSCIncident(leitscIncidentID);
        if (leitscIncident != null) {
            WorkProduct wp = incidentManagementService
                    .getIncident(leitscIncident.getIncidentWPID());
            if (wp != null) {
                product = WorkProductHelper.toWorkProduct(wp);
            }
        }
        return product;
    }

    public LEITSCIncidentDAO getLeitscIncidentDAO() {

        return leitscIncidentDAO;
    }

    /**
     * Post detailed CFS message.
     *
     * @param message
     *            the message
     *
     * @return the string
     *
     * @throws DetailedCFSMessageException
     *             the detailed cfs message exception
     * @throws DetailedCFSMessageXMLException
     *             the detailed cfs message xml exception
     * @throws LEITSCIncidentPublicationException
     *             the LEITSC incident publication exception
     * @throws XmlException
     *             the xml exception
     * @ssdd
     */
    @Override
    public String postDetailedCFSMessage(String message) throws DetailedCFSMessageException,
            DetailedCFSMessageXMLException, LEITSCIncidentPublicationException, XmlException {

        log.debug("postDetailedCFSMessage - received meesage:[" + message + "]");

        String result = null;

        DetailedCFSMessageReader reader = new DetailedCFSMessageReader();

        reader.init(message);

        // make sure the LEITSC's Identification ID is defined
        String leitscIncidentID = reader.read(DetailedCFSMessageReader.ACTIVITY_IDENTIFICATION);

        String activityStatus = reader.read(DetailedCFSMessageReader.ACTIVITY_STATUS_TEXT);

        if (activityStatus.equalsIgnoreCase(DetailedCFSMessageReader.ActivityStatusText.CREATED
                .toString())) {
            // create a new incident
            result = createLEITSCIncident(leitscIncidentID, reader);

        } else if (activityStatus
                .equalsIgnoreCase(DetailedCFSMessageReader.ActivityStatusText.CLEARED.toString())) {
            // close and archive incident
            result = clearLEITSCIncident(leitscIncidentID, reader);

        } else {
            // for everything else, update an existing incident
            result = updateLEITSCIncident(leitscIncidentID, reader);

        }
        return result;
    }

    public void setIncidentManagementService(IncidentManagementService incidentManagementService) {

        this.incidentManagementService = incidentManagementService;
    }

    public void setLeitscIncidentDAO(LEITSCIncidentDAO leitscIncidentDAO) {

        this.leitscIncidentDAO = leitscIncidentDAO;
    }

    private String updateLEITSCIncident(String leitscIncidentID, DetailedCFSMessageReader reader)
            throws LEITSCIncidentPublicationException, DetailedCFSMessageXMLException,
            DetailedCFSMessageException, XmlException {

        LEITSCIncident leitscIncident = leitscIncidentDAO.findByLEITSCIncident(leitscIncidentID);
        if (leitscIncident != null) {
            log.debug("updateLEITSCIncident - get work product for uicds incident ID="
                    + leitscIncident.getIncidentID());
            WorkProduct wp = incidentManagementService
                    .getIncident(leitscIncident.getIncidentWPID());
            if (wp != null) {
                log.debug("updateLEITSCIncident - found work product ID=" + wp.getProductID()
                        + " associated with uicds incident ID=" + leitscIncident.getIncidentID());
                UICDSIncidentType incident = IncidentUtil.getUICDSIncident(wp);
                if (incident != null) {
                    incident = DetailedCFSMessageUtil.populateIncident(incident, reader);
                    IdentificationType pkdId = WorkProductHelper.getWorkProductIdentification(wp);
                    log.debug("updateLEITSCIncident - updater uicds incident ID="
                            + leitscIncident.getIncidentID() + " for LEITSC incident ID="
                            + leitscIncident.getLeitscIncidentID());
                    ProductPublicationStatus status = incidentManagementService.updateIncident(
                            incident, pkdId);
                    if ((status != null)
                            && status.getStatus().equalsIgnoreCase(
                                    ProductPublicationStatus.SuccessStatus)) {
                        return leitscIncidentID;
                    } else {
                        // do something here
                        String reasonForFailure = (status == null) ? "Unkown" : status
                                .getReasonForFailure();
                        log.error("postDetailedCFSMessage - failed to open leitsc incident "
                                + leitscIncidentID + " reason=" + reasonForFailure);
                        throw new LEITSCIncidentPublicationException(leitscIncidentID, "open",
                                reasonForFailure);
                    }
                } else {
                    String reasonForFailure = " leitscIncident with ID" + leitscIncidentID
                            + " whose UICDS incident with incident ID="
                            + leitscIncident.getIncidentID()
                            + " failes to be generated from the work product wpID="
                            + wp.getProductID();
                    log.error("postDetailedCFSMessage - failed to open leitsc incident "
                            + leitscIncidentID + " reason=" + reasonForFailure);
                    throw new LEITSCIncidentPublicationException(leitscIncidentID, "open",
                            reasonForFailure);
                }
            } else {
                String reasonForFailure = " leitscIncident with ID" + leitscIncidentID
                        + " whose UICDS incident with incident ID="
                        + leitscIncident.getIncidentID() + " does NOT have a work product";
                log.error("postDetailedCFSMessage - failed to open leitsc incident "
                        + leitscIncidentID + " reason=" + reasonForFailure);
                throw new LEITSCIncidentPublicationException(leitscIncidentID, "open",
                        reasonForFailure);
            }

        } else {
            String reasonForFailure = " leitscIncident with ID" + leitscIncidentID
                    + " does NOT exist";
            log.error("postDetailedCFSMessage - failed to open leitsc incident " + leitscIncidentID
                    + " reason=" + reasonForFailure);
            throw new LEITSCIncidentPublicationException(leitscIncidentID, "open", reasonForFailure);
        }
    }

}
