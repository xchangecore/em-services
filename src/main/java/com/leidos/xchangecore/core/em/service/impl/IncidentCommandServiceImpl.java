package com.leidos.xchangecore.core.em.service.impl;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.organizationElement.OrganizationElementDocument;

import com.leidos.xchangecore.core.em.dao.IncidentCommandDAO;
import com.leidos.xchangecore.core.em.dao.OrganizationElementDAO;
import com.leidos.xchangecore.core.em.service.IncidentCommandService;
import com.leidos.xchangecore.core.infrastructure.exceptions.InvalidXpathException;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.InfrastructureNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.UUIDUtil;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The IncidentCommandService implementation.
 *
 * @see com.leidos.xchangecore.core.em.model.IncidentCommandStructure IncidentCommandStructure Data
 *      Model
 * @see com.leidos.xchangecore.core.em.model.OrganizationElement OrganizationElement Data Model
 * @see com.leidos.xchangecore.core.em.model.OrganizationPositionType OrganizationPositionType Data
 *      Model
 * @see com.leidos.xchangecore.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
@Transactional
public class IncidentCommandServiceImpl
    implements IncidentCommandService, ServiceNamespaces, InfrastructureNamespaces {

    private final Logger logger = LoggerFactory.getLogger(IncidentCommandServiceImpl.class);

    private WorkProductService workProductService;
    private DirectoryService directoryService;
    private IncidentCommandDAO incidentCommandDAO;
    private OrganizationElementDAO organizationElementDAO;

    /**
     * Gets the command structure by incident. Looks for incidents associated with the given
     * incidentId. If there are none, then create a new ICS and associate the incident with it.
     *
     * @param incidentID
     *            the incident id
     *
     * @return the command structure by incident
     * @ssdd
     */
    @Override
    public WorkProduct getCommandStructureByIncident(String incidentID) {

        final WorkProduct wp = null;

        try {
            // try the type as ICSType
            List<WorkProduct> icsList = workProductService.getProductByTypeAndXQuery(ICSType, null,
                null);
            // if there is no ICSType, try the MACSType
            if (icsList.size() == 0)
                icsList = workProductService.getProductByTypeAndXQuery(MACSType, null, null);

            for (final WorkProduct ics : icsList) {
                final Set<String> idSet = ics.getAssociatedInterestGroupIDs();
                for (final String id : idSet)
                    if (id.equals(incidentID))
                        return ics;
            }
        } catch (final InvalidXpathException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return wp;
    }

    public DirectoryService getDirectoryService() {

        return directoryService;
    }

    public IncidentCommandDAO getIncidentCommandDAO() {

        return incidentCommandDAO;
    }

    public OrganizationElementDAO getOrganizationElementDAO() {

        return organizationElementDAO;
    }

    private String getWorkProductIDByOrg(OrganizationElementDocument org) {

        return UUIDUtil.getID(getWorkProductTypeByOrg(org));
    }

    private String getWorkProductTypeByOrg(OrganizationElementDocument org) {

        if (org == null || org.getOrganizationElement() == null ||
            org.getOrganizationElement().getPersonInCharge() == null ||
            org.getOrganizationElement().getPersonInCharge().getRoleProfileRef() == null)
            return "UnknownType";

        return org.getOrganizationElement().getPersonInCharge().getRoleProfileRef().startsWith(
            IncidentCommanderRole) ? ICSType : MACSType;
    }

    
    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    public void setIncidentCommandDAO(IncidentCommandDAO ic) {

        incidentCommandDAO = ic;
    }

    public void setOrganizationElementDAO(OrganizationElementDAO oe) {

        organizationElementDAO = oe;
    }

    public void setWorkProductService(WorkProductService wp) {

        workProductService = wp;
    }

    @Override
    public void systemInitializedHandler(String message) {

        logger.debug("systemInitializedHandler: ... start ...");
        final WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(ICSType);
        typeList.addProductType(MACSType);
        getDirectoryService().registerUICDSService(NS_IncidentCommandStructureService,
            ICS_SERVICE_NAME, typeList, typeList);
        logger.debug("systemInitializedHandler: ... done ...");
    }

    /*
     * private ProductPublicationStatus associateProfilesToICSIncident(OrganizationElement org,
     * IncidentInfoType incidentRecordType) {
     *
     * // Null check Organization ProductPublicationStatus status = new ProductPublicationStatus();
     * // need to initialize to to something????
     *
     * if (org != null && incidentRecordType != null) { // Organization is a hierarchy org chart. //
     * Each org has a person in charge and their respective staff // These are the positionType and
     * include personal profile and the role profile // The Role profile is what needs to be updated
     * with the current incident
     *
     * // Get the person in charge OrganizationPositionType boss = org.getPersonInCharge(); // Call
     * the profileService, associate incident if (boss != null) {
     * getProfileService().addIncident(boss.getRoleProfileRef(), incidentRecordType); } // Get the
     * staff of the organization Set<OrganizationPositionType> staff = org.getStaffs(); for
     * (OrganizationPositionType position : staff) { // Call the profileService, associate incident
     * if (position.getRoleProfileRef() != null) {
     * getProfileService().addIncident(position.getRoleProfileRef(), incidentRecordType); } }
     *
     * // Get organizations Set<String> orgs = org.getOrganizations(); for (String orgId : orgs) {
     * OrganizationElement organization = organizationElementDAO.findById(orgId); status =
     * associateProfilesToICSIncident(organization, incidentRecordType); } } return status; }
     */

    /**
     * Update command structure.
     *
     * @param pkgId
     *            the pkg id
     * @param org
     *            the org
     * @param incidentID
     *            the incident id
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateCommandStructure(IdentificationType pkgId,

    // Actually, we don't need to model the Command Structure anymore since we will take it as
    // product and save it

                                                           OrganizationElementDocument org,
                                                           String incidentID) {

        logger.debug("updateCommandStructure");
        if (incidentID != null)
            incidentID.replaceAll("\n\t ", "");

        if (org == null || org.getOrganizationElement() == null)
            return null;

        // Get the current work product if it exists
        WorkProduct wp = workProductService.getProduct(pkgId);

        // If it does not exist then setup the new work product
        if (wp == null) {

            wp = new WorkProduct();
            if (pkgId == null)
                pkgId = IdentificationType.Factory.newInstance();

            if (pkgId.getType() == null)
                pkgId.addNewType().setStringValue(getWorkProductTypeByOrg(org));

            WorkProductHelper.setWorkProductIdentification(wp, pkgId);

            if (wp.getProductID() == null)
                // we need to identify whether it is a ICS or MACS
                wp.setProductID(getWorkProductIDByOrg(org));

            if (incidentID != null) {
                logger.debug(" incidentID=" + incidentID);
                wp.associateInterestGroup(incidentID);
            }
        }

        // Put the payload into the work product
        wp.setProduct(org);

        final ProductPublicationStatus status = workProductService.publishProduct(wp);

        return status;

    }
}
