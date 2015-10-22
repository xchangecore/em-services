package com.leidos.xchangecore.core.em.endpoint;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.soap.SOAPException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.alertService.CancelAlertRequestDocument;
import org.uicds.alertService.CancelAlertRequestDocument.CancelAlertRequest;
import org.uicds.alertService.CancelAlertResponseDocument;
import org.uicds.alertService.CreateAlertRequestDocument;
import org.uicds.alertService.CreateAlertResponseDocument;
import org.uicds.alertService.GetAlertByAlertIdRequestDocument;
import org.uicds.alertService.GetAlertByAlertIdResponseDocument;
import org.uicds.alertService.GetAlertByAlertIdResponseDocument.GetAlertByAlertIdResponse;
import org.uicds.alertService.GetAlertRequestDocument;
import org.uicds.alertService.GetAlertRequestDocument.GetAlertRequest;
import org.uicds.alertService.GetAlertResponseDocument;
import org.uicds.alertService.GetAlertResponseDocument.GetAlertResponse;
import org.uicds.alertService.GetListOfAlertsRequestDocument;
import org.uicds.alertService.GetListOfAlertsRequestDocument.GetListOfAlertsRequest;
import org.uicds.alertService.GetListOfAlertsResponseDocument;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import com.leidos.xchangecore.core.em.service.AlertService;
import com.leidos.xchangecore.core.infrastructure.exceptions.InvalidXpathException;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.NamespaceMapItemType;
import com.saic.precis.x2009.x06.base.NamespaceMapType;

import x1.oasisNamesTcEmergencyCap1.AlertDocument;

import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The Alert service allows XchangeCore compatible clients to create, cancel and get CAP alert work
 * products that conform to the CAP version 1.1 specification. Alerts may or may not be associated
 * with a particular incident using the optional incidentId parameter. Submitted alerts are not
 * validated against a particular CAP profile. A representation of the create request is shown in
 * the following diagram.
 * <p>
 * <img src="doc-files/alertType.png"/> <BR>
 * <p>
 * The Alert Service manages alerts as XchangeCore work products of type "Alert". <BR>
 * <p>
 * 
 * @author Daphne Hurrell
 * @since 1.0
 * @see <a href="../../wsdl/AlertService.wsdl">Appendix: AlertService.wsdl</a>
 * @see <a href="../../services/Alert/0.1/AlertService.xsd">Appendix: AlertService.xsd</a>
 * @idd
 * 
 */
@Endpoint
public class AlertServiceEndpoint
    implements ServiceNamespaces {

    @Autowired
    AlertService alertService;

    @Autowired
    WorkProductService productService;

    @Autowired
    DirectoryService directoryService;

    private Logger log = LoggerFactory.getLogger(AlertServiceEndpoint.class);

    /**
     * Allows the client to create an alert work product and optionally associate it to an incident.
     * 
     * @see <a href="../../services/Alert/0.1/AlertService.xsd">Appendix: AlertService.xsd</a>
     * 
     * @param CreateAlertRequestDocument
     * 
     * @return CreateAlertResponseDocument
     * @throws DatatypeConfigurationException
     * @idd
     */
    @PayloadRoot(namespace = NS_AlertService, localPart = "CreateAlertRequest")
    public CreateAlertResponseDocument createAlert(CreateAlertRequestDocument request)
        throws DatatypeConfigurationException {

        CreateAlertResponseDocument response = CreateAlertResponseDocument.Factory.newInstance();
        ProductPublicationStatus status = alertService.createAlert(request.getCreateAlertRequest().getIncidentId(),
            request.getCreateAlertRequest().getAlert());
        response.addNewCreateAlertResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));

        log.debug("CreateAlertResponse: [ " + response + " ]");
        return response;

    }

    /**
     * Allows the client to create an alert work product from a raw CAP XML element.
     * 
     * @see <a href="../../services/Alert/0.1/AlertService.xsd">Appendix: AlertService.xsd</a>
     * 
     * @param CreateAlertRequestDocument
     * 
     * @return CreateAlertResponseDocument
     * @throws DatatypeConfigurationException
     * @idd
     */
    @PayloadRoot(namespace = NS_OasisCAP, localPart = "alert")
    public void createAlertRawCap(AlertDocument alertDoc) throws DatatypeConfigurationException {

        ProductPublicationStatus status = alertService.createAlert(null, alertDoc.getAlert());

        log.debug("createAlertRawCap: [ " + status + " ]");

    }

    /**
     * Allows the client to get an alert work product using the CAP identifier value.
     * 
     * @see <a href="../../services/Alert/0.1/AlertService.xsd">Appendix: AlertService.xsd</a>
     * 
     * @param GetAlertByAlertIdRequestDocument
     * 
     * @return GetAlertByAlertIdResponseDocument
     * 
     * @throws DatatypeConfigurationException
     * @idd
     */
    @PayloadRoot(namespace = NS_AlertService, localPart = "GetAlertByAlertIdRequest")
    public GetAlertByAlertIdResponseDocument getAlertByAlertId(GetAlertByAlertIdRequestDocument requestDoc)
        throws DatatypeConfigurationException {

        GetAlertByAlertIdResponseDocument responseDoc = GetAlertByAlertIdResponseDocument.Factory.newInstance();
        WorkProduct wp = alertService.getAlertByAlertId(requestDoc.getGetAlertByAlertIdRequest().getAlertID());

        GetAlertByAlertIdResponse getAlertByAlertIdResponse = responseDoc.addNewGetAlertByAlertIdResponse();
        if (wp != null) {
            getAlertByAlertIdResponse.setWorkProduct(WorkProductHelper.toWorkProduct(wp));
        } else {
            getAlertByAlertIdResponse.setNil();
        }
        return responseDoc;
    }

    /**
     * Allows the client to get an alert work product using the XchangeCore Work Product Identification.
     * 
     * @see <a href="../../services/Alert/0.1/AlertService.xsd">Appendix: AlertService.xsd</a>
     * 
     * @param GetAlertRequestDocument
     * 
     * @return GetAlertResponseDocument
     * 
     * @throws DatatypeConfigurationException
     * @idd
     */
    @PayloadRoot(namespace = NS_AlertService, localPart = "GetAlertRequest")
    public GetAlertResponseDocument getAlert(GetAlertRequestDocument requestDoc)
        throws DatatypeConfigurationException {

        GetAlertRequest request = requestDoc.getGetAlertRequest();
        IdentificationType workProductIdentification = request.getWorkProductIdentification();

        GetAlertResponseDocument responseDoc = GetAlertResponseDocument.Factory.newInstance();
        GetAlertResponse response = responseDoc.addNewGetAlertResponse();
        response.setWorkProduct(WorkProductHelper.toWorkProduct(alertService.getAlert(workProductIdentification.getIdentifier().getStringValue())));

        return responseDoc;
    }

    /**
     * Allows the client to get a list of all alert work products or a limited list by using the
     * QueryString. The QueryString is an XPath statement rooted at the alert element with the
     * appropriate namespaces used in the XPath statement contained in the NamespaceMap.
     * 
     * @see <a href="../../services/Alert/0.1/AlertService.xsd">Appendix: AlertService.xsd</a>
     * 
     * @param GetListOfAlertsRequestDocument
     * 
     * @return GetListOfAlertsResponseDocument
     * 
     * @throws DatatypeConfigurationException
     * @idd
     */
    @PayloadRoot(namespace = NS_AlertService, localPart = "GetListOfAlertsRequest")
    public GetListOfAlertsResponseDocument getListOfAlerts(GetListOfAlertsRequestDocument requestDoc)
        throws javax.xml.soap.SOAPException {

        GetListOfAlertsRequest getListOfAlertsRequest = requestDoc.getGetListOfAlertsRequest();

        WorkProductList productList = null;

        String queryString = "";
        NamespaceMapType namespaceMap = null;

        //        NamespaceMapItemType namespace = NamespaceMapItemType.Factory.newInstance();
        //        request.addNewGetListOfAlertsRequest().addNewNamespaceMap().addNewItem().set(namespace);

        if (getListOfAlertsRequest != null) {
            queryString = getListOfAlertsRequest.getQueryString();
            namespaceMap = getListOfAlertsRequest.getNamespaceMap();
        }

        try {

            WorkProduct[] products = alertService.getListOfAlerts(queryString, namespaceMap);

            if (products != null && products.length > 0) {
                productList = WorkProductList.Factory.newInstance();
                for (WorkProduct product : products) {
                    productList.addNewWorkProduct().set(WorkProductHelper.toWorkProductSummary(product));
                }
            }

            GetListOfAlertsResponseDocument responseDoc = GetListOfAlertsResponseDocument.Factory.newInstance();
            responseDoc.addNewGetListOfAlertsResponse().addNewWorkProductList();
            if (productList != null) {
                responseDoc.getGetListOfAlertsResponse().getWorkProductList().set(productList);
            }

            return responseDoc;
        } catch (InvalidXpathException e) {
            throw new SOAPException(e.getMessage());
        }
    }

    /**
     * Allows the client to cancel an existing alert work product. This operation will close and
     * archive the alert work product specified by the WorkProductIdentification parameter.
     * 
     * @see <a href="../../services/Alert/0.1/AlertService.xsd">Appendix: AlertService.xsd</a>
     * 
     * @param CancelAlertRequestDocument
     * 
     * @return CancelAlertResponseDocument
     * 
     * @throws DatatypeConfigurationException
     * @idd
     */
    @PayloadRoot(namespace = NS_AlertService, localPart = "CancelAlertRequest")
    public CancelAlertResponseDocument cancelAlert(CancelAlertRequestDocument requestDoc)
        throws DatatypeConfigurationException {

        CancelAlertRequest request = requestDoc.getCancelAlertRequest();
        IdentificationType workProductIdentification = request.getWorkProductIdentification();

        CancelAlertResponseDocument responseDoc = CancelAlertResponseDocument.Factory.newInstance();
        ProductPublicationStatus status = alertService.cancelAlert(workProductIdentification.getIdentifier().getStringValue());
        responseDoc.addNewCancelAlertResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));
        return responseDoc;
    }
}
