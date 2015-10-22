package com.leidos.xchangecore.core.em.endpoint;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.broadcastService.BroadcastMessageErrorType;
import org.uicds.broadcastService.BroadcastMessageRequestDocument;
import org.uicds.broadcastService.BroadcastMessageResponseDocument;
import org.uicds.broadcastService.BroadcastMessageResponseType;

import com.leidos.xchangecore.core.em.exceptions.SendMessageErrorException;
import com.leidos.xchangecore.core.em.service.BroadcastService;
import com.leidos.xchangecore.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.leidos.xchangecore.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareAgreementException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;

/**
 * The Broadcast Service provides a mechanism to send messages to a set of selected XchangeCore resource
 * instances or XMPP users. The message is represented as an EDXL-DE message and as such can contain
 * any content allowed by the EDXL-DE specification.
 * <p/>
 * An EDXL-DE message to be sent is represented by a BroadcastMessageType and is defined as the
 * following data structure:<br/>
 * <img src="doc-files/broadcast.png"/> <!-- NEWPAGE -->
 * <p/>
 * Resource instances are identified as individual explictAddress elements in the EDXL-DE header.
 * The messages will be delivered to the specified resource instances by adding the full EDXL-DE
 * message to the resource instance's notification queue. XMPP users are identified as individual
 * explicitAddress elements and the full EDXL-DE message will be delivered as an XML element in an
 * XMPP message.
 * <p/>
 * Delivery of messages to resource instances is based on the availability of the core of the
 * destination resource instance. A data sharing agreement must be in place between the two cores to
 * allow messages to be sent. The destination core must also be online for the message to be sent
 * and delivered. The response will contain an error message indicating if the message could not be
 * sent to one or more destination resource instances.
 * <p/>
 * The address scheme for the explictAddress for XchangeCore resources instances is "uicds:user" and the
 * address value is the XchangeCore identifier of a resource instance. An example of an explicitAddress
 * element is:
 * 
 * <pre>
 * &lt;de:explicitAddress&gt;
 *    &lt;de:explicitAddressScheme&gt;uicds:user&lt;/de:explicitAddressScheme&gt;
 *    &lt;de:explicitAddressValue&gt;user@core.otherdomain.com&lt;/de:explicitAddressValue&gt;
 * &lt;/de:explicitAddress&gt;
 * </pre>
 * 
 * The address scheme for the explictAddress for XMPP users is "xmpp" and the address value is the
 * Jabber ID (JID) of the XMPP user. An example of an explicitAddress element is:
 * 
 * <pre>
 * &lt;de:explicitAddress&gt;
 *    &lt;de:explicitAddressScheme&gt;xmpp&lt;/de:explicitAddressScheme&gt;
 *    &lt;de:explicitAddressValue&gt;user@xmpp.domain.com&lt;/de:explicitAddressValue&gt;
 * &lt;/de:explicitAddress&gt;
 * </pre>
 * 
 * The EDXL-DE message will be delivered to XMPP clients as and XMPP message. The body will be a
 * short summary of the sender id and date/time sent from the EDXL-DE header information. If each
 * ContentObject element contains a ContentDescription element then that text will be included also.
 * The full EDXL-DE message will be a sub-element of the XMPP message element.
 * 
 * @author Aruna Hau
 * @see <a href="../../wsdl/BroadcastService.wsdl">Appendix: BroadcastService.wsdl</a>
 * @see <a href="../../services/Broadcast/0.1/BroadcastService.xsd">Appendix: BroadcastService.xsd</a>
 * @idd
 */
@Endpoint
public class BroadcastServiceEndpoint
    implements ServiceNamespaces {

    @Autowired
    BroadcastService broadcastService;

    Logger log = LoggerFactory.getLogger(BroadcastServiceEndpoint.class);

    void setBroadcastService(BroadcastService service) {

        broadcastService = service;
    }

    /**
     * Allows the client to broadcast a message to XchangeCore resource instances and XMPP JIDs. The
     * target of the message can be either:
     * <ul>
     * <li>A particular XchangeCore resource instance by specifying their resource instance id in an
     * explictAddress element</li>
     * <li>A particular XMPP user by specifying their JID in an explicitAddress element</li>
     * </ul>
     * 
     * Any number of explictAddresses of the known schemas may be added. The message will be
     * delivered to the Notification queue of the targeted resource instance or as an XMPP message
     * to any JIDs.
     * 
     * @see <a href="../../services/Broadcast/0.1/BroadcastService.xsd">Appendix: BroadcastService.xsd</a>
     * 
     * @param BroadcastMessageRequestDocument
     * 
     * @return None
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_BroadcastService, localPart = "BroadcastMessageRequest")
    public BroadcastMessageResponseDocument BroadcastMessage(BroadcastMessageRequestDocument requestDoc) {

        log.debug("BroadcastMessage: ");

        BroadcastMessageResponseDocument responseDoc = BroadcastMessageResponseDocument.Factory.newInstance();
        BroadcastMessageResponseType response = responseDoc.addNewBroadcastMessageResponse();
        response.setErrorExists(false);

        try {
            broadcastService.broadcastMessage(requestDoc);
        } catch (IllegalArgumentException e) {
            response.setErrorExists(true);
            response.setErrorString(e.getMessage());
        } catch (EmptyCoreNameListException e) {
            response.setErrorExists(true);
            response.setErrorString("Empty Explicit Address List");
        } catch (SendMessageErrorException e) {
            response.setErrorExists(true);
            response.setErrorString("Failure to send message to one or more cores");
            Set<String> coresWithError = e.getErrors().keySet();
            for (String core : coresWithError) {
                BroadcastMessageErrorType error = response.addNewCoreError();
                error.setCoreName(core);
                error.setError(e.getErrors().get(core).toString());
            }
        } catch (LocalCoreNotOnlineException e) {
            response.setErrorExists(true);
            response.setErrorString("Local Core is not online");
        } catch (NoShareAgreementException e) {
            response.setErrorExists(true);
            response.setErrorString(e.getMessage());
        } catch (NoShareRuleInAgreementException e) {
            response.setErrorExists(true);
            response.setErrorString(e.getMessage());
        }

        return responseDoc;
    }
}
