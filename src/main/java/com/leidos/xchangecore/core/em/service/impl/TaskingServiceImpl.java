package com.leidos.xchangecore.core.em.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.taskingService.TaskListDocument;
import org.uicds.taskingService.TaskListType;

import com.leidos.xchangecore.core.em.service.TaskingService;
import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The TaskingService implementation.
 *
 * @author ron
 * @see com.leidos.xchangecore.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class TaskingServiceImpl
    implements TaskingService, ServiceNamespaces {

    private final Logger logger = LoggerFactory.getLogger(TaskingServiceImpl.class);

    private WorkProductService workProductService;
    private DirectoryService directoryService;

    /**
     * Creates the task list.
     *
     * @param entityId
     *            the entity id
     * @param incidentId
     *            the incident id
     * @param taskList
     *            the task list
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createTaskList(String entityId,
                                                   String incidentId,
                                                   TaskListType taskList) {

        logger.debug("createTaskList");

        if (taskList.getEntityId() == null)
            taskList.setEntityId(entityId);
        logger.debug("taskList.toString=" + taskList.toString());

        final TaskListDocument taskListDocument = TaskListDocument.Factory.newInstance();
        taskListDocument.setTaskList(taskList);

        final WorkProduct product = new WorkProduct();
        product.setProductType(TaskingService.TASKING_PRODUCT_TYPE);
        product.setCreatedDate(new Date());
        product.setProduct(taskListDocument);

        if (incidentId != null)
            product.associateInterestGroup(incidentId);

        logger.debug("product=" + taskList.toString());
        final ProductPublicationStatus status = workProductService.publishProduct(product);

        logger.debug("createTaskList DONE");
        return status;
    }

    /**
     * Delete task list.
     *
     * @param wpId
     *            the wp id
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus deleteTaskList(String wpId) {

        logger.debug("deleteTaskList");
        final WorkProduct wp = workProductService.getProduct(wpId);

        ProductPublicationStatus status;

        if (wp == null) {
            status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(wpId + " doesn't existed");
            return status;
        }

        // if it's still not closed, we need to close it first
        if (wp.isActive() == true) {
            status = getWorkProductService().closeProduct(
                WorkProductHelper.getWorkProductIdentification(wp));
            if (status.getStatus().equals(ProductPublicationStatus.FailureStatus))
                return status;
        }

        return getWorkProductService().archiveProduct(
            WorkProductHelper.getWorkProductIdentification(wp));
    }

    /** {@inheritDoc} */
    @Override
    public DirectoryService getDirectoryService() {

        return directoryService;
    }

    /**
     * Gets the task list.
     *
     * @param wpId
     *            the wp id
     * @return the task list
     * @ssdd
     */
    @Override
    public WorkProduct getTaskList(String wpId) {

        return getWorkProductService().getProduct(wpId);
    }

    /**
     * Gets the task list by entity id and incident id.
     *
     * @param entityId
     *            the entity id
     * @param incidentId
     *            the incident id
     * @return the task list by entity id and incident id
     * @ssdd
     */
    @Override
    public WorkProduct getTaskListByEntityIdAndIncidentId(String entityId, String incidentId) {

        logger.debug("getTaskList");

        TaskListDocument taskListDocument = null;
        TaskListType taskList = null;

        // GetByType and IncidentID
        final List<WorkProduct> wpList = workProductService.findByInterestGroupAndType(incidentId,
            TaskingService.TASKING_PRODUCT_TYPE);
        for (final WorkProduct wp : wpList) {
            try {
                taskListDocument = (TaskListDocument) wp.getProduct();
                taskList = taskListDocument.getTaskList();
            } catch (final Exception e) {
                e.printStackTrace();
                return null;
            }

            if (taskList != null && taskList.getEntityId().equals(entityId)) {
                logger.debug("getTaskList taskList.toString:\n" + taskList.toString());
                return wp;
            }
        }
        return null;
    }

    /**
     * Gets the task list by incident id.
     *
     * @param entityId
     *            the entity id
     * @param incidentId
     *            the incident id
     * @return the task list by incident id
     * @ssdd
     */
    public WorkProduct getTaskListByIncidentId(String entityId, String incidentId) {

        TaskListDocument taskListDocument = null;
        TaskListType taskList = null;

        // GetByType and IncidentID
        final List<WorkProduct> wpList = workProductService.findByInterestGroupAndType(incidentId,
            TaskingService.TASKING_PRODUCT_TYPE);
        for (final WorkProduct wp : wpList) {
            try {
                taskListDocument = (TaskListDocument) wp.getProduct();
                taskList = taskListDocument.getTaskList();
            } catch (final Exception e) {
                e.printStackTrace();
                return null;
            }

            if (taskList != null && entityId != null)
                if (taskList.getEntityId().equals(entityId)) {
                    logger.debug("getTaskList taskList.toString:\n" + taskList.toString());
                    return wp;
                }
        }
        return null;

    }

    /** {@inheritDoc} */
    @Override
    public WorkProductService getWorkProductService() {

        return workProductService;
    }

    @PostConstruct
    public void init() {

    }

    /** {@inheritDoc} */
    @Override
    public void setDirectoryService(DirectoryService service) {

        directoryService = service;
    }

    /** {@inheritDoc} */
    @Override
    public void setWorkProductService(WorkProductService service) {

        workProductService = service;
    }

    @Override
    public void systemInitializedHandler(String messgae) {

        logger.debug("systemInitializedHandler: ... start ...");
        final WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(TASKING_PRODUCT_TYPE);
        directoryService.registerUICDSService(NS_TaskingService, TASKING_SERVICE_NAME, typeList,
            typeList);
        logger.debug("systemInitializedHandler: ... done ...");
    }

    /**
     * Update task list.
     *
     * @param taskList
     *            the task list
     * @param workProductIdentification
     *            the work product identification
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateTaskList(TaskListType taskList,
                                                   IdentificationType workProductIdentification) {

        WorkProduct wp = workProductService.getProduct(workProductIdentification.getIdentifier().getStringValue());

        final TaskListDocument taskListDoc = TaskListDocument.Factory.newInstance();
        taskListDoc.addNewTaskList().set(taskList);
        wp.setProduct(taskListDoc);
        wp = WorkProductHelper.setWorkProductIdentification(wp, workProductIdentification);

        final ProductPublicationStatus status = workProductService.publishProduct(wp);

        return status;
    }
}
