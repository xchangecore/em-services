package com.leidos.xchangecore.core.em.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.iapService.AttachWorkProductToIAPRequestDocument;
import org.uicds.iapService.AttachWorkProductToIAPRequestDocument.AttachWorkProductToIAPRequest;
import org.uicds.iapService.AttachWorkProductToIAPResponseDocument;
import org.uicds.iapService.CreateIAPRequestDocument;
import org.uicds.iapService.CreateIAPResponseDocument;
import org.uicds.iapService.CreateICSFormRequestDocument;
import org.uicds.iapService.CreateICSFormResponseDocument;
import org.uicds.iapService.GetApprovedIAPRequestDocument;
import org.uicds.iapService.GetApprovedIAPResponseDocument;
import org.uicds.iapService.GetIAPRequestDocument;
import org.uicds.iapService.GetIAPResponseDocument;
import org.uicds.iapService.GetICSFormListRequestDocument;
import org.uicds.iapService.GetICSFormListResponseDocument;
import org.uicds.iapService.GetICSFormRequestDocument;
import org.uicds.iapService.GetICSFormResponseDocument;
import org.uicds.iapService.IncidentActionPlanType;
import org.uicds.iapService.SetApprovedIAPRequestDocument;
import org.uicds.iapService.SetApprovedIAPResponseDocument;
import org.uicds.iapService.UpdateIAPRequestDocument;
import org.uicds.iapService.UpdateIAPRequestDocument.UpdateIAPRequest;
import org.uicds.iapService.UpdateIAPResponseDocument;
import org.uicds.iapService.UpdateICSFormRequestDocument;
import org.uicds.iapService.UpdateICSFormRequestDocument.UpdateICSFormRequest;
import org.uicds.iapService.UpdateICSFormResponseDocument;
import org.uicds.icsFormCommon.ICSFormDocumentType;
import org.uicds.workProductService.WorkProductPublicationResponseType;

import com.leidos.xchangecore.core.em.service.IAPService;
import com.leidos.xchangecore.core.infrastructure.exceptions.InvalidProductIDException;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The Incident Action Plan (IAP) Service allows XchangeCore compatible clients to manage an IAP and the
 * related ICS form work products that are associated with a XchangeCore incident. It includes services
 * to:
 * <ul>
 * <li>create, update, or retrieve an IAP
 * <li>create, update, or retrieve ICS Forms
 * <li>attach any work product to a particular IAP
 * <li>set and get the approved version of the IAP work product for an incident
 * </ul>
 * <p>
 * The Incident Action Plan work product is defined as the following data structure:<br/>
 * <img src="doc-files/iap.png"/>
 * <p>
 * Work products associated to an IAP as Components can be any work product the client wishes to be
 * part of the Incident Action Plan for example maps, image files, ICS, etc. Most of the time the
 * associated work product will be ICS Forms that are represented by XML documents defined as part
 * of this service. Components of the IAP may also have a XchangeCore task associated with them that
 * represents a task to complete the work product. Each component also has a status that can be a
 * string value. Currently the IAP Service defines XML schemas for the following ICS forms:
 * <ul>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm201.xsd">ICS 201</a></li>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm202.xsd">ICS 202</a></li>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm203.xsd">ICS 203</a></li>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm204.xsd">ICS 204</a></li>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm205.xsd">ICS 205</a></li>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm206.xsd">ICS 206</a></li>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm207.xsd">ICS 207</a></li>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm209.xsd">ICS 209</a></li>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm210.xsd">ICS 210</a></li>
 * <li><a href="../../services/IncidentForms/0.1/ICSForm211.xsd">ICS 211</a></li>
 * </ul>
 * <p>
 * The IAP Service manages work products of type "IAP" and "ICSForm". <BR>
 * <p>
 * 
 * 
 * @author Andre Bonner
 * @see <a href="../../wsdl/IAPService.wsdl">Appendix: IAPService.wsdl</a>
 * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm201.xsd">Appendix: ICSForm201.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm202.xsd">Appendix: ICSForm202.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm203.xsd">Appendix: ICSForm203.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm204.xsd">Appendix: ICSForm204.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm205.xsd">Appendix: ICSForm205.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm206.xsd">Appendix: ICSForm206.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm207.xsd">Appendix: ICSForm207.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm209.xsd">Appendix: ICSForm209.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm210.xsd">Appendix: ICSForm210.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm211.xsd">Appendix: ICSForm211.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSForm.xsd">Appendix: ICSForm.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/ICSFormCommon.xsd">Appendix: ICSFormCommon.xsd</a>
 * @see <a href="../../services/IncidentForms/0.1/UICDSCommon.xsd">Appendix: UICDSCommon.xsd</a>
 * @idd
 */
@Endpoint
public class IAPServiceEndpoint
    implements ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(IAPServiceEndpoint.class);

    @Autowired
    private IAPService iapService;

    @Autowired
    private WorkProductService productService;

    /**
     * Set the IAP Service object.
     * 
     * @param service
     */
    void setIAPService(IAPService service) {

        iapService = service;
        log.debug("IAPService set.");
    }

    /**
     * Create an IAP work product.
     * 
     * @param CreateIAPRequestDocument
     * 
     * @return CreateIAPResponseDocument
     * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSFormCommon.xsd">Appendix: ICSFormCommon.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "CreateIAPRequest")
    public CreateIAPResponseDocument createIAP(CreateIAPRequestDocument requestDoc) {

        log.debug("createIAP: id=" +
                  requestDoc.getCreateIAPRequest().getIncidentActionPlan().getIncidentID());

        // return doc
        CreateIAPResponseDocument response = CreateIAPResponseDocument.Factory.newInstance();
        ProductPublicationStatus status = iapService.createIAP(requestDoc.getCreateIAPRequest().getIncidentActionPlan());
        response.addNewCreateIAPResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));
        return response;

    }

    /**
     * Create an ICSForm work product.
     * 
     * @param CreateICSFormRequestDocument
     * 
     * @return CreateICSFormResponseDocument
     * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm201.xsd">Appendix: ICSForm201.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm202.xsd">Appendix: ICSForm202.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm203.xsd">Appendix: ICSForm203.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm204.xsd">Appendix: ICSForm204.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm205.xsd">Appendix: ICSForm205.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm206.xsd">Appendix: ICSForm206.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm207.xsd">Appendix: ICSForm207.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm209.xsd">Appendix: ICSForm209.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm210.xsd">Appendix: ICSForm210.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm211.xsd">Appendix: ICSForm211.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm.xsd">Appendix: ICSForm.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSFormCommon.xsd">Appendix: ICSFormCommon.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/UICDSCommon.xsd">Appendix: UICDSCommon.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "CreateICSFormRequest")
    public CreateICSFormResponseDocument createICSForm(CreateICSFormRequestDocument requestDoc) {

        log.debug("createICSForm: id=" + requestDoc.getCreateICSFormRequest().getICSForm());

        // returned doc
        CreateICSFormResponseDocument response = CreateICSFormResponseDocument.Factory.newInstance();

        ProductPublicationStatus status = iapService.createICSForm(requestDoc.getCreateICSFormRequest().getICSForm());
        response.addNewCreateICSFormResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));
        return response;

    }

    /**
     * Get an existing IAP work product.
     * 
     * @param GetIAPRequestDocument
     * 
     * @return GetIAPResponseDocument
     * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "GetIAPRequest")
    public GetIAPResponseDocument getIAP(GetIAPRequestDocument requestDoc) {

        String wpId = requestDoc.getGetIAPRequest().getWorkProductIdentification().getIdentifier().getStringValue();
        log.debug("getIAP: id=" + wpId);

        // return doc

        GetIAPResponseDocument response = GetIAPResponseDocument.Factory.newInstance();
        response.addNewGetIAPResponse().setWorkProduct(WorkProductHelper.toWorkProduct(productService.getProduct(requestDoc.getGetIAPRequest().getWorkProductIdentification())));

        return response;

    }

    /**
     * Get an existing ICSForm work product.
     * 
     * @param GetICSFormRequestDocument
     * 
     * @return GetICSFormResponseDocument
     * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "GetICSFormRequest")
    public GetICSFormResponseDocument getICSForm(GetICSFormRequestDocument requestDoc) {

        String wpId = requestDoc.getGetICSFormRequest().getWorkProductIdentification().getIdentifier().getStringValue();
        log.debug("getICSForm: workProductID=" + wpId);

        // return doc
        GetICSFormResponseDocument response = GetICSFormResponseDocument.Factory.newInstance();

        response.addNewGetICSFormResponse().setWorkProduct(WorkProductHelper.toWorkProduct(productService.getProduct(requestDoc.getGetICSFormRequest().getWorkProductIdentification())));

        return response;
    }

    /**
     * Update an IAP work product.
     * 
     * @param UpdateIAPRequestDocument
     * 
     * @return UpdateIAPResponseDocument
     * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "UpdateIAPRequest")
    public UpdateIAPResponseDocument updateIAP(UpdateIAPRequestDocument requestDoc) {

        log.debug("updateIAPRequest: iapID=" +
                  requestDoc.getUpdateIAPRequest().getIncidentActionPlan().getId());

        UpdateIAPRequest request = requestDoc.getUpdateIAPRequest();
        IncidentActionPlanType iap = request.getIncidentActionPlan();
        IdentificationType workProductIdentification = request.getWorkProductIdentification();

        UpdateIAPResponseDocument responseDoc = UpdateIAPResponseDocument.Factory.newInstance();

        ProductPublicationStatus status = iapService.updateIAP(iap,
            workProductIdentification,
            false);

        responseDoc.addNewUpdateIAPResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));

        return responseDoc;
    }

    /**
     * Update an ICSForm work product.
     * 
     * @param UpdateICSFormRequestDocument
     * 
     * @return UpdateICSFormResponseDocument
     * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm201.xsd">Appendix: ICSForm201.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm202.xsd">Appendix: ICSForm202.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm203.xsd">Appendix: ICSForm203.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm204.xsd">Appendix: ICSForm204.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm205.xsd">Appendix: ICSForm205.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm206.xsd">Appendix: ICSForm206.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm207.xsd">Appendix: ICSForm207.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm209.xsd">Appendix: ICSForm209.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm210.xsd">Appendix: ICSForm210.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm211.xsd">Appendix: ICSForm211.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSForm.xsd">Appendix: ICSForm.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/ICSFormCommon.xsd">Appendix: ICSFormCommon.xsd</a>
     * @see <a href="../../services/IncidentForms/0.1/UICDSCommon.xsd">Appendix: UICDSCommon.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "UpdateICSFormRequest")
    public UpdateICSFormResponseDocument updateICSForm(UpdateICSFormRequestDocument requestDoc) {

        log.debug("updateICSForm: formID=" +
                  requestDoc.getUpdateICSFormRequest().getICSForm().getId());

        UpdateICSFormRequest request = requestDoc.getUpdateICSFormRequest();
        ICSFormDocumentType icsForm = request.getICSForm();
        IdentificationType workProductIdentification = request.getWorkProductIdentification();

        UpdateICSFormResponseDocument responseDoc = UpdateICSFormResponseDocument.Factory.newInstance();

        ProductPublicationStatus status = iapService.updateICSForm(icsForm,
            workProductIdentification);

        responseDoc.addNewUpdateICSFormResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));

        return responseDoc;
    }

    /**
     * Designate this IAP work product as the active IAP for this particular incident.
     * 
     * @param ActivateIAPRequestDocument
     * 
     * @return None
     * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "SetApprovedIAPRequest")
    public SetApprovedIAPResponseDocument setApprovedIAP(SetApprovedIAPRequestDocument requestDoc) {

        // call service method
        ProductPublicationStatus status = iapService.setApprovedIAP(requestDoc.getSetApprovedIAPRequest().getWorkProductIdentification(),
            requestDoc.getSetApprovedIAPRequest().getIncidentId());

        // WorkProduct wpIAP =
        // productService.getProduct(requestDoc.getActivateIAPRequest().getWorkProductIdentification());

        SetApprovedIAPResponseDocument response = SetApprovedIAPResponseDocument.Factory.newInstance();

        WorkProductPublicationResponseType statusXML = WorkProductHelper.toWorkProductPublicationResponse(status);

        response.addNewSetApprovedIAPResponse().addNewWorkProductPublicationResponse().set(statusXML);

        return response;
    }

    /**
     * Gets the IAP work product that is currently active. The active IAP is set by the activateIAP
     * operation.
     * 
     * @param requestDoc
     * @return
     * @throws InvalidProductIDException
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "GetApprovedIAPRequest")
    public GetApprovedIAPResponseDocument getApprovedIAP(GetApprovedIAPRequestDocument requestDoc)
        throws InvalidProductIDException {

        WorkProduct workProduct = iapService.getApprovedIAP(requestDoc.getGetApprovedIAPRequest().getIncidentId());

        if (workProduct != null) {

            GetApprovedIAPResponseDocument response = GetApprovedIAPResponseDocument.Factory.newInstance();
            response.addNewGetApprovedIAPResponse();
            response.getGetApprovedIAPResponse().addNewWorkProduct().set(WorkProductHelper.toWorkProduct(workProduct));
            return response;

        } else {
            throw new InvalidProductIDException();
        }

    }

    /**
     * Associate any XchangeCore Work Product to an IAP work product. This could be an ICS form that was
     * created with this service, a work product created with other XchangeCore work product services, or
     * a work product that was created through the Work Product Service.
     * 
     * @param AttachICSFormToIAPRquestDocument
     * 
     * @return AttachWorkProductToIAPResponseDocument
     * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "AttachWorkProductToIAPRequest")
    public AttachWorkProductToIAPResponseDocument attachWorkProductToIAP(AttachWorkProductToIAPRequestDocument requestDoc) {

        AttachWorkProductToIAPRequest data = requestDoc.getAttachWorkProductToIAPRequest();
        String iapWpId = data.getIAP().getWorkProductIdentification().getIdentifier().getStringValue();
        log.debug("attachWorkProductToIAP: workProductID=" + iapWpId);

        AttachWorkProductToIAPResponseDocument response = AttachWorkProductToIAPResponseDocument.Factory.newInstance();
        ProductPublicationStatus status = iapService.attachWorkProductToIAP(requestDoc.getAttachWorkProductToIAPRequest().getWorkProductList().getWorkProductIdentificationArray(),
            iapWpId);
        response.getAttachWorkProductToIAPResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));

        status.setStatus("OK");
        return response;
    }

    /**
     * Get a list of the existing ICS Form work products for a particular incident.
     * 
     * @param GetICSFormListRequestDocument
     * 
     * @return GetICSFormListResponseDocument
     * @see <a href="../../services/IncidentForms/0.1/IAPService.xsd">Appendix: IAPService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_IAPService, localPart = "GetICSFormListRequest")
    public GetICSFormListResponseDocument getICSFormList(GetICSFormListRequestDocument requestDoc) {

        log.debug("getICSFormList: incidentID=" +
                  requestDoc.getGetICSFormListRequest().getIncidentID());

        // return doc
        WorkProduct wps[] = iapService.getICSFormList(requestDoc.getGetICSFormListRequest().getIncidentID());
        GetICSFormListResponseDocument response = GetICSFormListResponseDocument.Factory.newInstance();
        response.addNewGetICSFormListResponse();
        for (WorkProduct wp : wps) {
            response.getGetICSFormListResponse().addNewWorkProduct().set(WorkProductHelper.toWorkProductDocument(wp));
        }
        return response;
    }

}
