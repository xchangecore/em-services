package com.leidos.xchangecore.core.em.service;

import java.util.ArrayList;

import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.IncidentInfoType;
import org.uicds.incidentManagementService.IncidentListType;
import org.uicds.incidentManagementService.ShareIncidentRequestDocument.ShareIncidentRequest;

import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert;

import com.leidos.xchangecore.core.infrastructure.exceptions.InvalidInterestGroupIDException;
import com.leidos.xchangecore.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareAgreementException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.leidos.xchangecore.core.infrastructure.exceptions.RemoteCoreUnavailableException;
import com.leidos.xchangecore.core.infrastructure.exceptions.UICDSException;
import com.leidos.xchangecore.core.infrastructure.exceptions.XMPPComponentException;
import com.leidos.xchangecore.core.infrastructure.messages.DeleteJoinedInterestGroupMessage;
import com.leidos.xchangecore.core.infrastructure.messages.JoinedInterestGroupNotificationMessage;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The Incident Management Service manages the lifecycle of an incident and the sharing of
 * incidents. It processes Incident type work products and creates and updates digests for those
 * work products.
 * 
 * @ssdd
 */
public interface IncidentManagementService {

    public static final String Type = "Incident";
    public static final String InterestGroupType = "Incident";
    public static final String IMS_SERVICE_NAME = "IncidentManagementService";

    /**
     * Archive incident.
     * 
     * @param incidentID the incident id
     * 
     * @return the product publication status
     * @ssdd
     */
    public ProductPublicationStatus archiveIncident(String incidentID);

    /**
     * Close incident.
     * 
     * @param incidentID the incident id
     * 
     * @return the product publication status
     * @ssdd
     */
    public ProductPublicationStatus closeIncident(String incidentID);

    /**
     * Allows the user to create an incident using the UICDSIncidentType type.
     * 
     * @param incident - the entered incident
     * @return status - status of the createIncident request
     * @see ProductPublicationStatus
     * 
     * @ssdd
     */
    public ProductPublicationStatus createIncident(UICDSIncidentType incident);

    /**
     * Allows the user to create an incident by passing in a UICDAlertAdapterType which is
     * cap/alert. All the related geometry information will be transferred into UICDSIncidentType.
     * 
     * @param incident - the entered incident
     * @return status - status of the createIncident request
     * @see ProductPublicationStatus
     * 
     * @ssdd
     */
    public ProductPublicationStatus createIncidentFromCap(Alert alert);

    /**
     * Delete incident.
     * 
     * @param incidentID the incident id
     * 
     * @return true, if successful
     * @ssdd
     */
    public boolean deleteIncident(String incidentID);

    /**
     * Delete joined interest group handler.
     * 
     * @param message the message
     * @ssdd
     */
    public void deleteJoinedInterestGroupHandler(DeleteJoinedInterestGroupMessage message);

    /**
     * Allows the user to retrieve an incident document by the incident's work product ID
     * 
     * @param incidentWPID
     * @return workProduct - work product for the specified incident
     * @ssdd
     */
    public WorkProduct getIncident(String incidentWPID);

    /**
     * Return the IncidentInfoType for the specified incident ID.
     * 
     * @param incidentID
     * @return incidentInfo - the IncidentInfoType will be returned.
     * @ssdd
     */
    public IncidentInfoType getIncidentInfo(String incidentID);

    /**
     * Gets the list of closed incident.
     * 
     * @return the list of closed incident
     * @ssdd
     */
    public String[] getListOfClosedIncident();

    /**
     * Returns a list of all incident's information data for this core
     * 
     * @return incidentList - list of the IncidentInfoType
     * @ssdd
     */
    public IncidentListType getListOfIncidents();

    /**
     * Returns a list of all incident work products for this core
     * 
     * @return incidentList - list of the incident work products
     * @ssdd
     */
    public ArrayList<WorkProduct> getIncidentList();

    /**
     * Notifies the IMS of a new joined incident
     * 
     * @param message
     * @ssdd
     */
    public void newJoinedInterestGroupHandler(JoinedInterestGroupNotificationMessage message);

    /**
     * Allows the user to share an incident with the specified core.
     * 
     * @param shareIncidentRequest - this includes the incident ID and the shared core name.
     * @return status - true if successful, otherwise false is return. The error log will be used to
     *         log the error message upon failure.
     * @throws NoShareRuleInAgreementException
     * @throws NoShareAgreementException
     * @throws XMPPComponentException
     * @throws RemoteCoreUnavailableException
     * @throws LocalCoreNotOnlineException
     * @throws InvalidInterestGroupIDException
     * @throws UICDSException
     * @ssdd
     */
    public boolean shareIncident(ShareIncidentRequest shareIncidentRequest)
        throws InvalidInterestGroupIDException, LocalCoreNotOnlineException,
        RemoteCoreUnavailableException, XMPPComponentException, NoShareAgreementException,
        NoShareRuleInAgreementException, UICDSException;

    /**
     * Allows the user to share an incident with the specified core. This a non-endpoint method and
     * is called by other UICDS services/entities (e.g. autoshare) where agreements rules have
     * already been checked and enforced.
     * 
     * @param shareIncidentRequest - this includes the incident ID and the shared core name.
     * @return status - true if successful, otherwise false is return. The error log will be used to
     *         log the error message upon failure.
     * @throws NoShareRuleInAgreementException
     * @throws NoShareAgreementException
     * @throws XMPPComponentException
     * @throws RemoteCoreUnavailableException
     * @throws LocalCoreNotOnlineException
     * @throws InvalidInterestGroupIDException
     * @throws UICDSException
     * @ssdd
     */
    public boolean shareIncidentAgreementChecked(ShareIncidentRequest shareIncidentRequest)
        throws InvalidInterestGroupIDException, LocalCoreNotOnlineException,
        RemoteCoreUnavailableException, XMPPComponentException, NoShareAgreementException,
        NoShareRuleInAgreementException, UICDSException;

    /**
     * Performs initialization operations.
     * 
     * @param message
     * @ssdd
     */
    public void systemInitializedHandler(String message);

    /**
     * Provides to user to update an existed incident.
     * 
     * @param incident - the updated incident
     * @return status - status of the createIncident request
     * @see ProductPublicationStatus
     * 
     * @ssdd
     */
    public ProductPublicationStatus updateIncident(UICDSIncidentType incident,
                                                   IdentificationType pkdId);
}
