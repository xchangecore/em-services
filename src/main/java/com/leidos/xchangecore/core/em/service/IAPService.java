package com.leidos.xchangecore.core.em.service;

import org.uicds.iapService.IncidentActionPlanDocument;
import org.uicds.iapService.IncidentActionPlanType;
import org.uicds.icsForm.ICSFormDocument;
import org.uicds.icsFormCommon.ICSFormDocumentType;
import org.uicds.uicdsCommon.DocumentType;

import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The IAP Service manages Incident Action Plan (IAP) and ICS form work products.
 * 
 * @author Roger Wuerfel
 * @ssdd
 * 
 */
public interface IAPService {

    public final static String IAP_WORKPRODUCT_TYPE = "IAP";
    public final static String ICSFORM_WORKPRODUCT_TYPE = "ICSForm";

    public static final String IAP_SERVICE_NAME = "IAPService";

    /**
     * Create an Incident Action Plan
     * 
     * @param iap
     * @return status of the create request
     * @see ProductPublicationStatus
     * @ssdd
     */
    public ProductPublicationStatus createIAP(IncidentActionPlanType plan);

    /**
     * Create an Incident Command Structure Form
     * 
     * @param icsForm
     * @return status of the create request
     * @see ProductPublicationStatus
     * @ssdd
     */
    public ProductPublicationStatus createICSForm(ICSFormDocumentType icsForm);

    /**
     * Lookup an Incident Action Plan according to the specified workProductID
     * 
     * @param iapID
     * @return incidentActionPlanDocument
     * @ssdd
     */
    public IncidentActionPlanDocument getIAP(String workProductID);

    /**
     * Lookup an ICSFrom according to the specified workProductID
     * 
     * @return ICSFormDocument
     * @ssdd
     */
    public ICSFormDocument getICSForm(String workProductID);

    /**
     * Set the specified Incident Action Plan as the approved IAP for the incident.
     * 
     * @param workProductID
     * @param incidentID
     * @return
     * @ssdd
     */
    public ProductPublicationStatus setApprovedIAP(IdentificationType workProductID,
                                                   String incidentID);

    /**
     * Get the approved IAP for the specified Incident
     * 
     * @param incidentID
     * @return
     * @ssdd
     */
    public WorkProduct getApprovedIAP(String incidentID);

    /**
     * Attach a List of ICSForms to an Incident Action Plan with the specified workProductID
     * 
     * @param form
     * @param iapID
     * @ssdd
     */
    public ProductPublicationStatus attachWorkProductToIAP(IdentificationType[] workProductIdentificationArray,
                                                           String iapWpId);

    /**
     * Get the ICSFormList for the specified incident
     * 
     * @return workProduct[]
     * @ssdd
     */
    public WorkProduct[] getICSFormList(String incidentID);

    /**
     * Update the specified Incident Action Plan
     * 
     * @param plan
     * @param workProductIdentification
     * @param activate
     * @return status of the create request
     * @see ProductPublicationStatus
     * @ssdd
     */
    ProductPublicationStatus updateIAP(IncidentActionPlanType plan,
                                       IdentificationType workProductIdentification,
                                       boolean activate);

    /**
     * Update the ICSForm
     * 
     * @param form @return status of the create request
     * @see ProductPublicationStatus
     * @ssdd
     */
    public ProductPublicationStatus updateICSForm(ICSFormDocumentType form,
                                                  IdentificationType workProductIdentification);

    /**
     * Update the document
     * 
     * @param document
     * @return status of the create request
     * @see ProductPublicationStatus
     * @ssdd
     */
    public ProductPublicationStatus updateDocument(DocumentType document,
                                                   IdentificationType workProductIdentification);

    // public void publishIAP(String iapID);
}
