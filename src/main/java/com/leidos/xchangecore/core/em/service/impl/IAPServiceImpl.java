package com.leidos.xchangecore.core.em.service.impl;

import gov.ucore.ucore.x20.DigestType;
import gov.ucore.ucore.x20.EntityType;
import gov.ucore.ucore.x20.SimplePropertyType;
import gov.ucore.ucore.x20.ThingType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.iapService.IAPComponentType;
import org.uicds.iapService.IncidentActionPlanDocument;
import org.uicds.iapService.IncidentActionPlanType;
import org.uicds.icsForm.ICSFormDocument;
import org.uicds.icsFormCommon.ICSFormDocumentType;
import org.uicds.uicdsCommon.DocumentType;
import org.uicds.uicdsCommon.IAPDocumentDocument1;

import com.leidos.xchangecore.core.em.service.IAPService;
import com.leidos.xchangecore.core.em.util.EMDigestHelper;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.DigestHelper;
import com.leidos.xchangecore.core.infrastructure.util.InfrastructureNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.UUIDUtil;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The IAPService implementation.
 *
 * @author wuerfelr
 * @see com.leidos.xchangecore.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class IAPServiceImpl
    implements IAPService, ServiceNamespaces {

    private static final boolean APPROVED = true;

    private static final boolean DRAFT = false;

    private final Logger logger = LoggerFactory.getLogger(IAPServiceImpl.class);

    private WorkProductService workProductService;
    private DirectoryService directoryService;

    /**
     * Attach ics forms to iap. If any supplied component Ids are already attached to the IAP (as
     * identifed by Id string, they are removed and added back with the new componentId
     * identification types and the IAP workProduct is re-published
     *
     * @param componentIds
     *            the component ids
     * @param workProductID
     *            the work product id
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus attachWorkProductToIAP(IdentificationType[] componentIds,
                                                           String workProductID) {

        // Check if IAP exists
        final WorkProduct workProduct = workProductService.getProduct(workProductID);
        // IncidentActionPlanDocument planDoc = getIAP(workProductID);

        ProductPublicationStatus status = null;

        // Check if IAP exists
        if (workProduct != null) {

            // Get the IAP
            final IncidentActionPlanDocument iapDoc = getIAPFromWorkProduct(workProduct);

            // List<IdentificationType> componentsToAdd =
            // Arrays.asList(componentIds);

            final ArrayList<IdentificationType> componentsToAdd = new ArrayList<IdentificationType>(Arrays.asList(componentIds));

            if (componentsToAdd.size() == 0)
                return status;

            // If alreay attached then remove it from the list of components to add
            if (iapDoc.getIncidentActionPlan().getComponents() != null &&
                iapDoc.getIncidentActionPlan().getComponents().sizeOfComponentArray() > 0)
                for (final IAPComponentType comp : iapDoc.getIncidentActionPlan().getComponents().getComponentArray()) {
                    final IdentificationType id = comp.getComponentIdentifier().getWorkProductIdentification();
                    for (final IdentificationType cid : componentIds)
                        if (id.getIdentifier().getStringValue().equalsIgnoreCase(
                            cid.getIdentifier().getStringValue()))
                            componentsToAdd.remove(cid);
                }

            // Add any that are not already in the IAP
            for (final IdentificationType comp : componentsToAdd)
                iapDoc.getIncidentActionPlan().addNewComponents().addNewComponent().addNewComponentIdentifier().setWorkProductIdentification(
                    comp);

            // Update the work product if there were any new components added
            if (componentsToAdd.size() > 0)
                status = workProductService.publishProduct(newWorkProductVersion(workProduct,
                    iapDoc));
            else {
                status = new ProductPublicationStatus();
                status.setStatus(ProductPublicationStatus.SuccessStatus);
                status.setProduct(workProduct);
            }
        }

        return status;
    }

    /**
     * Creates and publishes a workProduct of IAP service type.
     *
     * @param plan
     *            the plan
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createIAP(IncidentActionPlanType plan) {

        final WorkProduct wp = createIAPWorkProduct(plan, DRAFT);

        final ProductPublicationStatus status = workProductService.publishProduct(wp);
        if (status.getStatus().equals(ProductPublicationStatus.FailureStatus))
            logger.error("createIAP: Error publishing new IAP");

        return status;
    }

    private WorkProduct createIAPWorkProduct(IncidentActionPlanType plan, boolean approved) {

        final String planWPID = UUIDUtil.getID(IAPService.IAP_WORKPRODUCT_TYPE);
        plan.setId(planWPID);
        // Create WorkProduct
        final IncidentActionPlanDocument iap = IncidentActionPlanDocument.Factory.newInstance();
        iap.setIncidentActionPlan(plan);
        final WorkProduct wp = new WorkProduct();
        wp.setProductType(IAPService.IAP_WORKPRODUCT_TYPE);
        wp.setProduct(iap);
        wp.setProductID(planWPID);
        wp.setDigest(new EMDigestHelper(iap.getIncidentActionPlan(), planWPID, approved).getDigest());

        // Add interest group associated if requested.
        if (plan.getIncidentID() != null)
            wp.associateInterestGroup(plan.getIncidentID());
        return wp;
    }

    /**
     * Creates and publishes a workProduct of IAP service form type
     *
     * @param form
     *            the form
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createICSForm(ICSFormDocumentType form) {

        final String formWPID = UUIDUtil.getID(ICSFORM_WORKPRODUCT_TYPE);
        form.setId(formWPID);
        final ICSFormDocument icsForm = ICSFormDocument.Factory.newInstance();
        icsForm.addNewICSForm().set(form);
        final WorkProduct wp = new WorkProduct();
        wp.setProductType(IAPService.ICSFORM_WORKPRODUCT_TYPE);
        wp.setProduct(form);
        wp.setProductID(formWPID);

        // Add interest group associated if requested.
        if (form.getIncidentID() != null)
            wp.associateInterestGroup(form.getIncidentID());

        final ProductPublicationStatus status = workProductService.publishProduct(wp);
        if (status.getStatus().equals(ProductPublicationStatus.FailureStatus))
            logger.error("Error publishing ICS Form");

        return status;
    }

    private WorkProduct findApprovedIAP(String incidentID) {

        final List<WorkProduct> workProducts = workProductService.findByInterestGroupAndType(
            incidentID, IAP_WORKPRODUCT_TYPE);

        final List<WorkProduct> approvedIAPs = new ArrayList<WorkProduct>();

        for (final WorkProduct product : workProducts) {
            if (product.getAssociatedInterestGroupIDs().size() > 0) {
                final Iterator<String> it = product.getAssociatedInterestGroupIDs().iterator();
                if (!it.next().equals(incidentID))
                    continue;
            }

            if (product.getDigest() != null && product.getDigest().getDigest() != null) {
                final DigestType digest = product.getDigest().getDigest();
                if (digest.sizeOfThingAbstractArray() > 0)
                    for (final ThingType thing : digest.getThingAbstractArray())
                        if (thing instanceof EntityType) {
                            final SimplePropertyType status = DigestHelper.getSimplePropertyFromThing(
                                thing, InfrastructureNamespaces.UICDS_EVENT_STATUS_CODESPACE, null,
                                "Status", null);
                            if (status.getCode().equals("Approved"))
                                approvedIAPs.add(product);
                        }
            }
        }

        if (approvedIAPs.size() > 1) {
            logger.warn("Found more than one Approved IAP work product for incident: " + incidentID);
            return approvedIAPs.get(0);
        } else if (approvedIAPs.size() == 1)
            return approvedIAPs.get(0);
        return null;
    }

    @Override
    public WorkProduct getApprovedIAP(String incidentID) {

        return findApprovedIAP(incidentID);

    }

    /**
     * Gets the IAP using the supplied workProduct Id string
     *
     * @param workProductID
     *            the work product id
     *
     * @return the iAP
     * @ssdd
     */
    @Override
    public IncidentActionPlanDocument getIAP(String workProductID) {

        final WorkProduct wp = workProductService.getProduct(workProductID);
        try {
            return (IncidentActionPlanDocument) wp.getProduct();
        } catch (final Exception e) {
            logger.error("Error parsing IAP work product: " + e.getMessage());
            return null;
        }
    }

    private IncidentActionPlanDocument getIAPFromWorkProduct(WorkProduct workProduct) {

        IncidentActionPlanDocument plan = null;
        try {
            plan = (IncidentActionPlanDocument) workProduct.getProduct();
        } catch (final Exception e) {
            logger.error("Error parsing IAP work product: " + e.getMessage());
        }
        return plan;
    }

    /**
     * Gets the ICS form using the supplied workProduct Id string
     *
     * @param workProductID
     *            the work product id
     *
     * @return the iCS form
     * @ssdd
     */
    @Override
    public ICSFormDocument getICSForm(String workProductID) {

        final WorkProduct wp = workProductService.getProduct(workProductID);
        ICSFormDocument form = null;
        try {
            form = (ICSFormDocument) wp.getProduct();
        } catch (final Exception e) {
            logger.error("Error parsing IAP work product: " + e.getMessage());
        }
        return form;
    }

    /**
     * Gets the list of ICS form workProducts for the given incidentId
     *
     * @param incidentID
     *            the incident id
     *
     * @return the iCS form list
     * @ssdd
     */
    @Override
    public WorkProduct[] getICSFormList(String incidentID) {

        // GetByType and IncidentID
        final List<WorkProduct> wpList = workProductService.findByInterestGroupAndType(incidentID,
            IAPService.ICSFORM_WORKPRODUCT_TYPE);
        if (wpList == null || wpList.size() <= 0)
            return null;
        final WorkProduct[] products = wpList.toArray(new WorkProduct[wpList.size()]);

        return products;
    }

    private WorkProduct newWorkProductVersion(WorkProduct workProduct,
                                              IncidentActionPlanDocument iapDoc) {

        workProduct.setProduct(iapDoc);
        return workProduct;
    }

    /**
     * This takes an IAP from the list of IAP's for this incident and turns it into the 'active'
     * IAP.
     *
     * @param workProductID
     *            the work product id
     */
    @Override
    public ProductPublicationStatus setApprovedIAP(IdentificationType workProductID,
                                                   String incidentID) {

        ProductPublicationStatus status = new ProductPublicationStatus();
        status.setStatus(ProductPublicationStatus.FailureStatus);

        // get the requested IAP work product
        final WorkProduct requestedIAP = workProductService.getProduct(workProductID);
        if (requestedIAP == null) {
            status.setReasonForFailure("Work Product does not exist");
            return status;
        }

        // Get the IAP that is requested to be approved
        final IncidentActionPlanDocument iap = getIAPFromWorkProduct(requestedIAP);

        // see if this incident already has an active IAP
        final WorkProduct approvedIAP = findApprovedIAP(incidentID);

        // Create a new IAP work product that is marked as approved
        if (approvedIAP == null) {
            if (incidentID != null && !incidentID.isEmpty())
                if (iap.getIncidentActionPlan().getIncidentID() == null ||
                    iap.getIncidentActionPlan().getIncidentID().isEmpty())
                    iap.getIncidentActionPlan().setIncidentID(incidentID);

            final WorkProduct wp = createIAPWorkProduct(iap.getIncidentActionPlan(), APPROVED);

            status = workProductService.publishProduct(wp);
            if (status.getStatus().equals(ProductPublicationStatus.FailureStatus))
                logger.error("setApprovedIAP: Error publishing the new approved IAP");
        }
        // Update the current approved work product
        else {
            final IdentificationType workProductIdentification = WorkProductHelper.getWorkProductIdentification(approvedIAP);
            status = updateIAPWorkProduct(iap.getIncidentActionPlan(), workProductIdentification,
                true, approvedIAP);
        }

        return status;
    }

    /** {@inheritDoc} */
    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    /**
     *
     * @param workProductService
     */
    public void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }

    public void systemInitializedHandler(String messgae) {

        logger.debug("systemInitializedHandler: ... start ...");
        final WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(IAP_WORKPRODUCT_TYPE);
        typeList.addProductType(ICSFORM_WORKPRODUCT_TYPE);
        directoryService.registerUICDSService(NS_IAPService, IAP_SERVICE_NAME, typeList, typeList);
        logger.debug("systemInitializedHandler: ... done ...");
    }

    /**
     * Update an IAP workProduct given the workProduct identification.
     *
     * @param document
     *            the document
     * @param workProductIdentification
     *            the work product identification
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateDocument(DocumentType document,
                                                   IdentificationType workProductIdentification) {

        WorkProduct wp = workProductService.getProduct(document.getId());
        if (wp != null) {
            final IAPDocumentDocument1 iapDoc = IAPDocumentDocument1.Factory.newInstance();
            iapDoc.addNewIAPDocument().set(document);
            wp.setProduct(iapDoc);
            wp = WorkProductHelper.setWorkProductIdentification(wp, workProductIdentification);
            final ProductPublicationStatus status = workProductService.publishProduct(wp);
            return status;
        } else {
            final ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("Specified Document does not exist");
            return status;
        }
    }

    /**
     * Publishes an updated IAP workProduct if the product already exists or publishes a new IAP
     * workProduct if it does not.
     *
     * @param plan
     *            the plan
     * @param workProductIdentification
     *            the work product identification
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateIAP(IncidentActionPlanType plan,
                                              IdentificationType workProductIdentification,
                                              boolean activate) {

        final WorkProduct wp = workProductService.getProduct(workProductIdentification);

        return updateIAPWorkProduct(plan, workProductIdentification, activate, wp);
    }

    private ProductPublicationStatus updateIAPWorkProduct(IncidentActionPlanType plan,
                                                          IdentificationType workProductIdentification,
                                                          boolean activate,
                                                          WorkProduct wp) {

        // if updating to activate the IAP then use the current IAP payload
        if (plan == null)
            plan = getIAPFromWorkProduct(wp).getIncidentActionPlan();
        if (wp != null) {
            final IncidentActionPlanDocument iap = IncidentActionPlanDocument.Factory.newInstance();
            iap.addNewIncidentActionPlan().set(plan);
            // WorkProduct newWP = new WorkProduct(wp);
            wp.setProduct(iap);
            wp.setDigest(new EMDigestHelper(iap.getIncidentActionPlan(),
                                            workProductIdentification.getIdentifier().getStringValue(),
                                            activate).getDigest());
            final ProductPublicationStatus status = workProductService.publishProduct(wp);
            return status;
        } else {
            final ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("Specified Incident Action Plan does not exist");
            return status;
        }
    }

    /**
     * Publishes an updated ICS form workProduct if the product already exists or publishes a new
     * ICS form workProduct if it does not.
     *
     * @param form
     *            the form
     * @param workProductIdentification
     *            the work product identification
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateICSForm(ICSFormDocumentType form,
                                                  IdentificationType workProductIdentification) {

        final WorkProduct wp = workProductService.getProduct(workProductIdentification);
        if (wp != null) {
            final ICSFormDocument ics = ICSFormDocument.Factory.newInstance();
            ics.addNewICSForm().set(form);
            wp.setProduct(ics);
            final ProductPublicationStatus status = workProductService.publishProduct(wp);
            return status;
        } else {
            final ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("Specified ICS Form does not exist");
            return status;
        }
    }
}
