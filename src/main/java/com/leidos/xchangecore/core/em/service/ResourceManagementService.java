package com.leidos.xchangecore.core.em.service;

import org.springframework.transaction.annotation.Transactional;
import org.uicds.resourceManagementService.EdxlDeResponseDocument;

import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;
import x0Msg.oasisNamesTcEmergencyEDXLRM1.CommitResourceDocument.CommitResource;
import x0Msg.oasisNamesTcEmergencyEDXLRM1.RequestResourceDocument.RequestResource;

import com.leidos.xchangecore.core.em.exceptions.SendMessageErrorException;
import com.leidos.xchangecore.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.leidos.xchangecore.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareAgreementException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.leidos.xchangecore.core.infrastructure.messages.Core2CoreMessage;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;

/**
 * The ResourceManagementService uses the CommunicationsService to send EDXL-DE messages to
 * ResourceInstances on other cores. It manages RequestResource and CommitResource work products and
 * creates a Digest for each of these work products. Incoming EDXL-DE messages are passed to the
 * NotificationService for delivery.
 * 
 * @author Roger
 * @since 1.0
 * @ssdd
 */
@Transactional
public interface ResourceManagementService {

    public static final String COMMIT_RESOURCE_PRODUCT_TYPE = CommitResource.MessageContentType.COMMIT_RESOURCE.toString();
    public static final String REQUEST_RESOURCE_PRODUCT_TYPE = RequestResource.MessageContentType.REQUEST_RESOURCE.toString();
    public static final String RESOURCE_SERVICE_NAME = "ResourceManagementService";

    /**
     * Allows the client to submit an EDXL-RM document wrapped in EDXL-DE
     * 
     * @param request - EDXLDistribution containing RM Message
     * @return workProductId - String
     * @throws LocalCoreNotOnlineException
     * @throws SendMessageErrorException
     * @throws EmptyCoreNameListException
     * @throws IllegalArgumentException
     * @see EDXLDistribution
     * @ssdd
     */
    public EdxlDeResponseDocument edxldeRequest(EDXLDistribution request)
        throws IllegalArgumentException, EmptyCoreNameListException, SendMessageErrorException,
        LocalCoreNotOnlineException, NoShareAgreementException, NoShareRuleInAgreementException;

    /**
     * Gets a list of RequestResource work products which will contain digest elements for all
     * requested resources.
     * 
     * @param incidentID
     * @return ResourceItemListDocument
     * @see ResourceItemListDocument
     * @ssdd
     */
    public WorkProduct[] getRequestedResources(String incidentID);

    /**
     * Gets a list of CommitResource work products which will contain digest elements for all
     * comitted resources.
     * 
     * @param incidentID
     * @return ResourceItemListDocument
     * @see ResourceItemListDocument
     * @ssdd
     */
    public WorkProduct[] getCommittedResources(String incidentID);

    /**
     * Handles EDXL-RM messages from other cores.
     * 
     * @param message - a core to core message from the communications framework
     * @return void
     * @see applicationContext
     * @ssdd
     */
    public void resourceMessageNotificationHandler(Core2CoreMessage message);

    /**
     * SystemIntialized Message Handler
     * 
     * @param message - SystemInitialized message
     * @return void
     * @see applicationContext
     * @ssdd
     */
    public void systemInitializedHandler(String messgae);

}
