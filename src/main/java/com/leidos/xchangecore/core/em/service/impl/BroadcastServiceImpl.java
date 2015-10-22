package com.leidos.xchangecore.core.em.service.impl;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsn.b2.NotificationMessageHolderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.broadcastService.BroadcastMessageRequestDocument;
import org.uicds.directoryServiceData.WorkProductTypeListType;

import x0.oasisNamesTcEmergencyEDXLDE1.ContentObjectType;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;
import x0.oasisNamesTcEmergencyEDXLDE1.ValueSchemeType;

import com.leidos.xchangecore.core.em.endpoint.BroadcastServiceEndpoint;
import com.leidos.xchangecore.core.em.exceptions.SendMessageErrorException;
import com.leidos.xchangecore.core.em.service.BroadcastService;
import com.leidos.xchangecore.core.em.util.BroadcastUtil;
import com.leidos.xchangecore.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.leidos.xchangecore.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareAgreementException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.leidos.xchangecore.core.infrastructure.exceptions.RemoteCoreUnavailableException;
import com.leidos.xchangecore.core.infrastructure.exceptions.RemoteCoreUnknownException;
import com.leidos.xchangecore.core.infrastructure.messages.Core2CoreMessage;
import com.leidos.xchangecore.core.infrastructure.service.CommunicationsService;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.NotificationService;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.StringUtil;

/**
 * The broadcast service implementation distributes the message by the following process:
 * <ul>
 * <li>iterates through all the explicitAddress elements
 * <li>finds the cores that need to receive this message
 * <li>sends this message to the broadcast service on those cores through the communications service
 * </ul>
 *
 * This service also receives messages sent from broadcast services hosted on other cores and:
 * <ul>
 * <li>iterates through all the explictAddress elements
 * <li>delivers this message to Notification endpoint of the explictAddresses that are are on this
 * core
 * </ul>
 *
 * @version UICDS - alpha
 * @author roberta
 * @author Andre Bonner
 * @ssdd
 */
public class BroadcastServiceImpl
implements BroadcastService, ServiceNamespaces {

    static final String NEW_LINE = System.getProperty("line.separator");

    private final Logger logger = LoggerFactory.getLogger(BroadcastServiceEndpoint.class);

    private CommunicationsService communicationsService;
    private DirectoryService directoryService;
    private NotificationService notificationService;

    /**
     * Parses a broadcast request document for and edxl distribution message and sends the edxl
     * document message to all referenced cores
     *
     * @param message the broadcast request document
     *
     * @exception BroadcastFault
     * @ssdd
     */
    @Override
    public void broadcastMessage(BroadcastMessageRequestDocument message)
        throws IllegalArgumentException, EmptyCoreNameListException, SendMessageErrorException,
        LocalCoreNotOnlineException, NoShareAgreementException, NoShareRuleInAgreementException {

        // TODO Auto-generated method stub
        // log.debug("sendMessage: "+product.xmlText());
        logger.debug("sendMessage: " + message.xmlText());
        // Must have a distribution element
        final EDXLDistribution edxl = message.getBroadcastMessageRequest().getEDXLDistribution();
        if (edxl == null)
            throw new IllegalArgumentException("Empty EDXLDistribution element");
        else {
            // Find all the destination cores or JIDs from the explicit address fields
            final HashSet<String> cores = BroadcastUtil.getCoreList(edxl);
            final HashSet<String> jids = BroadcastUtil.getJidList(edxl);

            // Send the message to each core that has a user in an explictAddress element
            if (cores.size() == 0 && jids.size() == 0)
                throw new EmptyCoreNameListException();
            else {
                SendMessageErrorException errorException = new SendMessageErrorException();

                final EDXLDistributionDocument edxlDoc = EDXLDistributionDocument.Factory.newInstance();
                edxlDoc.setEDXLDistribution(edxl);

                // Send the message to the cores as a Broadcase Service message
                errorException = sendMessageToCore(cores, errorException, edxlDoc.xmlText());

                // Send the message to any external XMPP addresses
                errorException = sendXMPPMessage(jids, errorException, edxlDoc);

                if (errorException.getErrors().size() > 0)
                    throw errorException;
            }

        }

    }

    /**
     * Broadcast message notification handler dispatches received messages to the listeners
     * specified in the explicit address array
     *
     * @param message the message
     * @ssdd
     */
    @Override
    public void broadcastMessageNotificationHandler(Core2CoreMessage message) {

        logger.debug("broadcastMessageNotificationHandler: received message=[" +
                     message.getMessage() + "] from " + message.getFromCore());

        XmlObject xmlObj;
        try {

            final EDXLDistributionDocument edxlDoc = EDXLDistributionDocument.Factory.parse(message.getMessage());

            if (edxlDoc.getEDXLDistribution().sizeOfExplicitAddressArray() > 0)
                // Find core name for each explicit address.
                for (final ValueSchemeType type : edxlDoc.getEDXLDistribution().getExplicitAddressArray())
                    if (type.getExplicitAddressScheme().equals(
                        CommunicationsService.UICDSExplicitAddressScheme))
                        for (final String address : type.getExplicitAddressValueArray()) {
                            xmlObj = XmlObject.Factory.parse(edxlDoc.toString());
                            // log.debug("broadcastMessageNotificationHandler: sending notification ["
                            // + xmlObj.toString() + "]  to " + address);
                            sendMessageNotification(xmlObj, address);
                        }

        } catch (final Throwable e) {
            logger.error("broadcastMessageNotificationHandler: Error parsing message - not a valid XML string");
            throw new IllegalArgumentException("Message is not a valid XML string");
        }
    }

    @Override
    public CommunicationsService getCommunicationsService() {

        return communicationsService;
    }

    private String getMessageBody(EDXLDistributionDocument edxlDoc) {

        final StringBuffer body = new StringBuffer();
        if (edxlDoc.getEDXLDistribution() != null) {
            body.append("EDXL-DE message received from ");
            if (edxlDoc.getEDXLDistribution().getSenderID() != null)
                body.append(edxlDoc.getEDXLDistribution().getSenderID());
            else
                body.append("UICDS");
            body.append(NEW_LINE);
            if (edxlDoc.getEDXLDistribution().getDateTimeSent() != null) {
                body.append("Sent at ");
                body.append(edxlDoc.getEDXLDistribution().getDateTimeSent().toString());
                body.append(NEW_LINE);
            }
            if (edxlDoc.getEDXLDistribution().sizeOfContentObjectArray() > 0) {
                body.append("Content element descriptions: ");
                body.append(NEW_LINE);
                for (final ContentObjectType content : edxlDoc.getEDXLDistribution().getContentObjectArray())
                    if (content.getContentDescription() != null) {
                        body.append("Content Description: ");
                        body.append(content.getContentDescription());
                        body.append(NEW_LINE);
                    }
            }
        }
        return body.toString();
    }

    private void sendMessageNotification(XmlObject xmlObj, String address) {

        final ArrayList<NotificationMessageHolderType> messages = new ArrayList<NotificationMessageHolderType>();

        final NotificationMessageHolderType t = NotificationMessageHolderType.Factory.newInstance();
        final NotificationMessageHolderType.Message m = t.addNewMessage();

        try {
            m.set(xmlObj);
            messages.add(t);

            NotificationMessageHolderType[] notification = new NotificationMessageHolderType[messages.size()];

            notification = messages.toArray(notification);
            logger.debug("===> sending Core2Core message: array size=" + notification.length);
            notificationService.notify(StringUtil.getSubmitterResourceInstanceName(address),
                notification);
        } catch (final Throwable e) {
            logger.error("productPublicationStatusNotificationHandler: error creating and sending  Core2Core message  notification to " +
                         address);
            e.printStackTrace();
        }
    }

    private SendMessageErrorException sendMessageToCore(HashSet<String> cores,
                                                        SendMessageErrorException errorException,
                                                        String msgStr)
                                                            throws NoShareAgreementException, NoShareRuleInAgreementException,
                                                            LocalCoreNotOnlineException {

        for (final String core : cores)
            try {
                // log.debug("sendMessage:  Sending " + msgStr + " to: " + core);
                //		    	System.out.println("sending to " + core);
                communicationsService.sendMessage(msgStr,
                    CommunicationsService.CORE2CORE_MESSAGE_TYPE.BROADCAST_MESSAGE, core);
                logger.debug("called communicationsService.sendMessage");
            } catch (final RemoteCoreUnknownException e1) {
                errorException.getErrors().put(core,
                    SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.CORE_UNKNOWN);
            } catch (final RemoteCoreUnavailableException e2) {
                errorException.getErrors().put(core,
                    SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.CORE_UNAVAILABLE);
            } catch (final LocalCoreNotOnlineException e) {
                throw e;
            }
        return errorException;
    }

    private SendMessageErrorException sendXMPPMessage(HashSet<String> jids,
                                                      SendMessageErrorException errorException,
                                                      EDXLDistributionDocument edxlDoc)
                                                          throws NoShareAgreementException, NoShareRuleInAgreementException,
                                                          LocalCoreNotOnlineException {

        for (final String jid : jids)
            // log.debug("sendMessage:  Sending " + msgStr + " to: " + core);
            communicationsService.sendXMPPMessage(getMessageBody(edxlDoc), null, edxlDoc.xmlText(),
                jid);
        return errorException;
    }

    @Override
    public void setCommunicationsService(CommunicationsService service) {

        communicationsService = service;
    }

    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    public void setNotificationService(NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    /** {@inheritDoc} */
    @Override
    public void systemInitializedHandler(String message) {

        logger.debug("systemInitializedHandler: ... start ...");
        final WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        directoryService.registerUICDSService(NS_BroadcastService, BROADCAST_SERVICE_NAME,
            typeList, typeList);
        logger.debug("systemInitializedHandler: ... done ...");
    }

}
