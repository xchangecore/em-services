package com.leidos.xchangecore.core.em.endpoint;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.resourceManagementService.EdxlDeMessageErrorType;
import org.uicds.resourceManagementService.EdxlDeRequestDocument;
import org.uicds.resourceManagementService.EdxlDeResponseDocument;
import org.uicds.resourceManagementService.GetCommittedResourcesRequestDocument;
import org.uicds.resourceManagementService.GetCommittedResourcesRequestDocument.GetCommittedResourcesRequest;
import org.uicds.resourceManagementService.GetCommittedResourcesResponseDocument;
import org.uicds.resourceManagementService.GetCommittedResourcesResponseDocument.GetCommittedResourcesResponse;
import org.uicds.resourceManagementService.GetRequestedResourcesRequestDocument;
import org.uicds.resourceManagementService.GetRequestedResourcesRequestDocument.GetRequestedResourcesRequest;
import org.uicds.resourceManagementService.GetRequestedResourcesResponseDocument;
import org.uicds.resourceManagementService.GetRequestedResourcesResponseDocument.GetRequestedResourcesResponse;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;

import com.leidos.xchangecore.core.em.exceptions.SendMessageErrorException;
import com.leidos.xchangecore.core.em.service.ResourceManagementService;
import com.leidos.xchangecore.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.leidos.xchangecore.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareAgreementException;
import com.leidos.xchangecore.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;

/**
 * The Resource Management Service provides XchangeCore clients with services to exchange EDXL-RM messages
 * with other XchangeCore clients and send EDXL-RM messages to XMPP users. The clients can exchange any of
 * the EDXL-RM messages from any of the EDXL-RM messaging phases: Discovery, Ordering and
 * Deployment. It is up to the clients to implement the actions necessary to support these message
 * exchanges according to the EDXL-RM specification. The XchangeCore Resource Management Service merely
 * routes these messages to explicitly targeted clients and delivers the message on the clients
 * notification queue or XMPP message queue while maintaining a minimal summary of the message
 * exchanges. The Resource Management service will maintain the following summary information about
 * resources for each incident:
 * <ul>
 * <li>Requested resources</li>
 * <li>Committed resources</li>
 * </ul>
 * A XchangeCore core will support the following:
 * <ul>
 * <li>Routing any EDXL-RM message to specific XchangeCore clients based on the EDXL-DE Explicit Address
 * element(s). See use of explicit address usage in
 * {@link com.leidos.xchangecore.core.em.endpoint.BroadcastServiceEndpoint}</li>
 * <li>Routing any EDXL-RM message to specific XMPP JID based on the EDXL-DE Explicit Address
 * element(s). See use of explicit address usage in
 * {@link com.leidos.xchangecore.core.em.endpoint.BroadcastServiceEndpoint}</li>
 * <li>Restricting routing of EDXL-RM messages to only those cores that have valid agreements</li>
 * <li>Creating work products for RequestResource and CommitResource messages</li>
 * <ul>
 * <li>Other messages are not parsed or stored by the XchangeCore core</li>
 * </ul>
 * <li>Providing a summary of requested and committed resources for an incident</li> </ul>
 * <p>
 * An example of the interaction of two XchangeCore clients, in this example two CAD systems, with an
 * XchangeCore core to support EDXL-RM messaging can be seen in the following sequence diagram. <br/>
 * <img src="doc-files/rm_slide3.jpg"/> <br/>
 * <p>
 * <!-- NEWPAGE -->
 * <p>
 * The main points about working with the EDXL-RM schemas when interacting with XchangeCore are:
 * <ul>
 * <li>All elements required by the EDXL-RM specification must be represented</li>
 * <li>Where EDXL-RM allows a choice of required elements XchangeCore may specify a required or preferred
 * element</li>
 * <li>Where EDXL-RM allows multiple representations XchangeCore may designate a preferred representation</li>
 * <li>XchangeCore will not change any EDXL-RM message and will preserve all EDXL-RM message content (i.e.
 * optional elements)</li>
 * </ul>
 * <p>
 * Since XchangeCore only parses the Request and Commit Resource messages it only places requirements on
 * those messages. The following diagrams show details of data requirements for these two messages.
 * The full details of what data is required beyond that required for XchangeCore needs to be negotiated
 * between the systems that are exchanging EDXL-RM messages. <br/>
 * <img src="doc-files/rm_slide8.jpg"/> <br/>
 * <p>
 * <!-- NEWPAGE --> <img src="doc-files/rm_slide9.jpg"/>
 * <p>
 * <!-- NEWPAGE --> <img src="doc-files/rm_slide10.jpg"/>
 * <p>
 * <!-- NEWPAGE --> <img src="doc-files/rm_slide11.jpg"/>
 * <p>
 * <!-- NEWPAGE --> <img src="doc-files/rm_slide12.jpg"/>
 * <p>
 * <!-- NEWPAGE --> <img src="doc-files/rm_slide19.jpg"/>
 * <p>
 * <!-- NEWPAGE --> <img src="doc-files/rm_slide20.jpg"/>
 * <p>
 * <!-- NEWPAGE --> <img src="doc-files/rm_slide21.jpg"/>
 * <p>
 * <!-- NEWPAGE -->
 * <p>
 * The Request and Commit Resource message are used within XchangeCore to create a work product and digest
 * so that these data can be searched and provided as outputs. The following diagrams show the
 * digests that are created for the Request and Commit Resource messages within XchangeCore. Full details
 * of the mapping from the EDXL-RM message to the Digests fields can be found in the
 * CommitResourceDigest.xsl and RequestResourceDigest.xsl XSL Transformation files. The following
 * tables shows the default mappings.
 * <p>
 * <img src="doc-files/rm_slide15.jpg"/>
 * <p>
 * <b>CommitResource to UCore Digest Mapping:</b>
 * <p>
 * <table>
 * <tr>
 * <th>CommitResource Element</th>
 * <th>Digest Element</th>
 * </tr>
 * <tr>
 * <td>CommitResource.ContactInformation.ContactDescription</td>
 * <td>Event.Identifier</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.Resource.Name</td>
 * <td>Entity.Descriptor</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.Resource.TypeInfo.Category</td>
 * <td>Entity.SimpleProperty</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.Resource.TypeInfo.Kind</td>
 * <td>Entity.SimpleProperty</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.Resource.TypeInfo.Resource</td>
 * <td>Entity.SimpleProperty</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.Resource.TypeInfo.MinimumCapabilities</td>
 * <td>Entity.SimpleProperty</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.ScheduleInformation.Location. LocationDescription</td>
 * <td>Location.Descriptor</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.ScheduleInformation.Location.Address. Thoroughfare.Number
 * CommitResource.ResourceInformation.ScheduleInformation.Location .Address.Thoroughfare.NameElement
 * </td>
 * <td>Location.PhysicalAddress.postalAddress.street</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.ScheduleInformation.Location.Address. Locality.NameElement
 * </td>
 * <td>Location.PhysicalAddress.postalAddress.city</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.ScheduleInformation.Location.Address.
 * AdministrativeArea.NameElement@NameCode</td>
 * <td>Location.PhysicalAddress.postalAddress.state</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.ScheduleInformation.Location.Address. PostCode.Identifier</td>
 * <td>Location.PhysicalAddress.postalAddress.postalCode</td>
 * </tr>
 * <tr>
 * <td>CommitResource.ResourceInformation.ScheduleInformation.Location.Address. Country.NameElement@NameCode
 * </td>
 * <td>Location.PhysicalAddress.postalAddress.countryCode</td>
 * </tr>
 * <tr>
 * <td>
 * CommitResource.ResourceInformation.ScheduleInformation.Location.TargetArea .Point.pos</td>
 * <td>Location.GeoLocation.Point.Point.pos</td>
 * </tr>
 * </table>
 * <p>
 * <!-- NEWPAGE --> <img src="doc-files/rm_slide26.jpg"/>
 * <p>
 * <b>RequestResource to UCore Digest Mapping:</b>
 * <p>
 * <table>
 * <tr>
 * <th>RequestResource Element</th>
 * <th>Digest Element</th>
 * </tr>
 * <tr>
 * <td>Resource Request</td>
 * <td>Entity.Descriptor</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.AssignmentInformation.Quantity. QuantityText</td>
 * <td>Entity.SimpleProperty</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.Resource.TypeInfo.Kind</td>
 * <td>Entity.SimpleProperty</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.Resource.TypeInfo.Resource</td>
 * <td>Entity.SimpleProperty</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.Resource.TypeInfo.MinimumCapabilities</td>
 * <td>Entity.SimpleProperty</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.Resource.TypeInfo.Resource</td>
 * <td>Entity.What</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.ScheduleInformation.Location. LocationDescription</td>
 * <td>Location.Descriptor</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.ScheduleInformation.Location.Address. Thoroughfare.Number
 * CommitResource.ResourceInformation.ScheduleInformation.Location .Address.Thoroughfare.NameElement
 * </td>
 * <td>Location.PhysicalAddress.postalAddress.street</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.ScheduleInformation.Location.Address.
 * Locality.NameElement</td>
 * <td>Location.PhysicalAddress.postalAddress.city</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.ScheduleInformation.Location.Address.
 * AdministrativeArea.NameElement@NameCode</td>
 * <td>Location.PhysicalAddress.postalAddress.state</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.ScheduleInformation.Location.Address. PostCode.Identifier
 * </td>
 * <td>Location.PhysicalAddress.postalAddress.postalCode</td>
 * </tr>
 * <tr>
 * <td>RequestResource.ResourceInformation.ScheduleInformation.Location.Address. Country.NameElement@NameCode
 * </td>
 * <td>Location.PhysicalAddress.postalAddress.countryCode</td>
 * </tr>
 * <tr>
 * <td>
 * RequestResource.ResourceInformation.ScheduleInformation.Location.TargetArea .Point.pos</td>
 * <td>Location.GeoLocation.Point.Point.pos</td>
 * </tr>
 * <tr>
 * <td>
 * RequestResource.ContactInformation.ContactDescription</td>
 * <td>Organization.Descriptor</td>
 * </tr>
 * </table>
 * <p>
 * <!-- NEWPAGE -->
 * <p>
 * As stated in the EDXL-RM specification an EDXL-RM message posted to XchangeCore must be encapsulated in
 * an EDXL-DE message. Currently XchangeCore only routes EDXL-DE messages based on values in the explicit
 * address element. The values in this element must have a type of uicds:user with a value of a
 * resource instance identifier.
 * 
 * An example explicit address for RMApplication@core1 to send an EDXL-RM message to
 * RMApplication@core2 would be:
 *
 * <pre>
 * &lt;di:explicitAddress&gt; &lt;de:explicitAddressScheme&gt;uicds:user&lt;/de:explicitAddressScheme&gt; &lt;de:explicitAddressValue&gt;RMApplication2@core2&lt;/de:explicitAddressValue&gt; &lt;/de:explicitAddress&gt;
 * </pre>
 * 
 * Note that the explicitAddressSchema should be set to "uicds:user" and the explicitAddressValue
 * set to the resource instance identifier that is listed in the agreements and is also the actual
 * identifier that RMApplication2 is using to get notification messages from XchangeCore. <br/>
 * 
 * An example explicit address to send an EDXL-RM message to an XMPP user would be:
 *
 * <pre>
 * &lt;di:explicitAddress&gt; &lt;de:explicitAddressScheme&gt;xmpp&lt;/de:explicitAddressScheme&gt; &lt;de:explicitAddressValue&gt;user@xmpp.domain.com&lt;/de:explicitAddressValue&gt; &lt;/de:explicitAddress&gt;
 * </pre>
 * 
 * The EDXL-DE message will be delivered to XMPP clients as and XMPP message. The body will be a
 * short summary of the sender id and date/time sent from the EDXL-DE header information. If each
 * ContentObject element contains a ContentDescription element then that text will be included also.
 * The full EDXL-DE message will be a sub-element of the XMPP message element.
 * 
 * Note that the explicitAddressSchema should be set to "xmpp" and the explicitAddressValue set to
 * the Jabber identifier (JID) of the XMPP user. <br/>
 * <p>
 *
 * @see <a href="../../wsdl/ResourceManagementService.wsdl">Appendix:
 *      ResourceManagementService.wsdl</a>
 * @see <a href="../../services/ResourceManagement/0.1/ResourceManagementService.xsd">Appendix:
 *      ResourceManagementService.xsd</a>
 * @see <a href="../../services/ResourceManagement/0.1/ResourceManagementServiceData.xsd">Appendix:
 *      ResourceManagementServiceData.xsd</a>
 * @see <a href="http://docs.oasis-open.org/emergency/edxl-de/v1.0/EDXL-DE_Spec_v1.0.pdf">OASIS
 *      EDXL-DE Specification</a>
 * @see <a href="http://docs.oasis-open.org/emergency/edxl-rm/v1.0/EDXL-RM-SPEC-V1.0.pdf">OASIS
 *      EDXL-RM Specification</a>
 * @see com.leidos.xchangecore.core.em.endpoint.BroadcastServiceEndpoint
 * @see com.leidos.xchangecore.core.em.endpoint.NotificationServiceEndpoint
 * @see com.leidos.xchangecore.core.em.endpoint.ResourceInstanceServiceEndpoint
 * @author roger
 * @author Ron Ridgely
 * @idd
 */
@Endpoint
@Transactional
public class ResourceManagementServiceEndpoint implements ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(ResourceManagementServiceEndpoint.class);

    @Autowired
    ResourceManagementService resourceManagementService;

    @Autowired
    WorkProductService workProductService;

    /**
     * Allows the client to submit an EDXL-RM document wrapped in EDXL-DE for delivery to explicit
     * addresses or as information only if it is a CommitResource or RequestResource. Submitting a
     * CommitResource or RequestResource without an explicit address will create the appropriate
     * work products and associate them with the incident.
     *
     * @param EdxlDeRequestDocument
     *
     * @return WorkProductIdResponseDocument
     * @see <a href="../../services/ResourceManagement/0.1/ResourceManagementService.xsd">Appendix:
     *      ResourceManagementService.xsd</a>
     * @see <a
     *      href="../../services/ResourceManagement/0.1/ResourceManagementServiceData.xsd">Appendix:
     *      ResourceManagementServiceData.xsd</a>
     * @see <a href="http://docs.oasis-open.org/emergency/edxl-de/v1.0/EDXL-DE_Spec_v1.0.pdf">OASIS
     *      EDXL-DE Specification</a>
     * @see <a href="http://docs.oasis-open.org/emergency/edxl-rm/v1.0/EDXL-RM-SPEC-V1.0.pdf">OASIS
     *      EDXL-RM Specification</a>
     * @idd
     */
    // TODO: translate exceptions into the response
    @PayloadRoot(namespace = NS_ResourceManagementService, localPart = "EdxlDeRequest")
    public EdxlDeResponseDocument edxldeRequest(EdxlDeRequestDocument requestDoc) {

        EDXLDistribution request = requestDoc.getEdxlDeRequest().getEDXLDistribution();
        EdxlDeResponseDocument response = EdxlDeResponseDocument.Factory.newInstance();
        response.addNewEdxlDeResponse();
        try {
            response = resourceManagementService.edxldeRequest(request);
        } catch (IllegalArgumentException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString(e.getMessage());
        } catch (EmptyCoreNameListException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString("Empty Explicit Address List");
        } catch (SendMessageErrorException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString(
                    "Failure to send message to one or more cores");
            Set<String> coresWithError = e.getErrors().keySet();
            for (String core : coresWithError) {
                EdxlDeMessageErrorType error = response.getEdxlDeResponse().addNewCoreError();
                error.setCoreName(core);
                error.setError(e.getErrors().get(core).toString());
            }
        } catch (LocalCoreNotOnlineException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString("Local Core is not online");
        } catch (NoShareAgreementException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString(e.getMessage());
        } catch (NoShareRuleInAgreementException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString(e.getMessage());
        }

        return response;
    }

    /**
     * Gets a list of CommitResource work product digests. Parsing the digest elements will yield a
     * list of resources committed to the incident.
     *
     * @param GetCommittedResourcesRequestDocument
     *
     * @return GetCommittedResourcesResponseDocument
     * @see <a href="../../services/ResourceManagement/0.1/ResourceManagementService.xsd">Appendix:
     *      ResourceManagementService.xsd</a>
     * @see <a
     *      href="../../services/ResourceManagement/0.1/ResourceManagementServiceData.xsd">Appendix:
     *      ResourceManagementServiceData.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_ResourceManagementService, localPart = "GetCommittedResourcesRequest")
    public GetCommittedResourcesResponseDocument getCommittedResources(
            GetCommittedResourcesRequestDocument requestDoc) {

        GetCommittedResourcesRequest request = requestDoc.getGetCommittedResourcesRequest();
        WorkProduct[] workProducts = resourceManagementService.getCommittedResources(request
                .getIncidentID());
        GetCommittedResourcesResponseDocument responseDoc = GetCommittedResourcesResponseDocument.Factory
                .newInstance();
        GetCommittedResourcesResponse responseList = responseDoc
                .addNewGetCommittedResourcesResponse();

        WorkProductList productList = populateSummary(workProducts);

        responseList.setWorkProductList(productList);

        return responseDoc;
    }

    /**
     * Gets a list of RequestResource work product digests. Parsing the digest elements will yield a
     * list of resources requested for the incident.
     *
     * @param GetRequestedResourcesRequestDocument
     *
     * @return GetRequestedResourcesResponseDocument
     * @see <a href="../../services/ResourceManagement/0.1/ResourceManagementService.xsd">Appendix:
     *      ResourceManagementService.xsd</a>
     * @see <a
     *      href="../../services/ResourceManagement/0.1/ResourceManagementServiceData.xsd">Appendix:
     *      ResourceManagementServiceData.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_ResourceManagementService, localPart = "GetRequestedResourcesRequest")
    public GetRequestedResourcesResponseDocument getRequestedResources(
            GetRequestedResourcesRequestDocument requestDoc) {

        GetRequestedResourcesRequest request = requestDoc.getGetRequestedResourcesRequest();
        WorkProduct[] workProducts = resourceManagementService.getRequestedResources(request
                .getIncidentID());
        GetRequestedResourcesResponseDocument responseDoc = GetRequestedResourcesResponseDocument.Factory
                .newInstance();
        GetRequestedResourcesResponse responseList = responseDoc
                .addNewGetRequestedResourcesResponse();

        WorkProductList productList = populateSummary(workProducts);

        responseList.setWorkProductList(productList);

        return responseDoc;
    }

    ResourceManagementService getResourceManagementService() {

        return resourceManagementService;
    }

    WorkProductService getWorkProductService() {

        return workProductService;
    }

    private WorkProductList populateSummary(WorkProduct[] products) {

        WorkProductList productList = WorkProductList.Factory.newInstance();

        if (products != null && products.length > 0) {
            for (WorkProduct product : products) {
                productList.addNewWorkProduct()
                        .set(WorkProductHelper.toWorkProductSummary(product));
            }
        }

        return productList;
    }

    void setResourceManagementService(ResourceManagementService resourceManagementService) {

        this.resourceManagementService = resourceManagementService;
    }

    void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }
}
