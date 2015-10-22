package com.leidos.xchangecore.core.em.service;

import org.springframework.transaction.annotation.Transactional;
import org.uicds.taskingService.TaskListType;

import com.leidos.xchangecore.core.infrastructure.model.WorkProduct;
import com.leidos.xchangecore.core.infrastructure.service.DirectoryService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.precis.x2009.x06.base.CodespaceValueType;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * The TaskingService manages Tasking type work products.
 * 
 * @author Ron Ridgely
 * @since 1.0
 * @ssdd
 */
@Transactional
public interface TaskingService {

    public static final String TASKING_PRODUCT_TYPE = "Tasking";
    public static final String TASKING_SERVICE_NAME = "TaskingService";

    /**
     * Creates a new task list for an entity and associates it with an incident.
     * 
     * @param entityId
     * @param incidentId
     * @param taskList
     * @see TaskListType
     * @param type
     * @see CodespaceValueType
     * @return ProductPublicationStatus
     * @ssdd
     */
    public ProductPublicationStatus createTaskList(String entityId,
                                                   String incidentId,
                                                   TaskListType taskList);

    /**
     * Updates the incident task list for an entity and incident
     * 
     * @param taskList
     * @see TaskListType
     * @param workProductIdentification
     * @see IdentificationType
     * @return ProductPublicationStatus
     * @ssdd
     */
    public ProductPublicationStatus updateTaskList(TaskListType taskList,
                                                   IdentificationType workProductIdentification);

    /**
     * Deletes a task list for an entity and incident
     * 
     * @param workProductIdentification
     * @see IdentificationType
     * @return WorkProductSummaryDocument
     * @ssdd
     */
    public ProductPublicationStatus deleteTaskList(String wpId);

    /**
     * Gets the task list
     * 
     * @param workProductIdentification
     * @see IdentificationType
     * @return WorkProductDocument
     * @ssdd
     */
    public WorkProduct getTaskList(String wpId);

    /**
     * Gets the incident task list for an entity and incident
     * 
     * @param entityId
     * @param incidentId
     * @param workProductIdentification
     * @see IdentificationType
     * @return WorkProductDocument
     * @ssdd
     */
    public WorkProduct getTaskListByEntityIdAndIncidentId(String entityID, String incidentId);

    /**
     * SystemIntialized Message Handler
     * 
     * @param message - SystemInitialized message
     * @return void
     * @see applicationContext
     * @ssdd
     */
    public void systemInitializedHandler(String messgae);

    /**
     * Gets the DirectoryService dependency
     * 
     * @param None
     * @return DirectoryService
     * @see DirectoryService
     * @ssdd
     */
    public DirectoryService getDirectoryService();

    /**
     * Sets the DirectoryService dependency
     * 
     * @param service - DirectoryService
     * @return void
     * @see DirectoryService
     * @ssdd
     */
    public void setDirectoryService(DirectoryService service);

    /**
     * Gets the WorkProductService dependency
     * 
     * @param None
     * @return void
     * @see WorkProductService
     * @ssdd
     */
    public WorkProductService getWorkProductService();

    /**
     * Sets the WorkProductService dependency
     * 
     * @param service - WorkProductService
     * @return void
     * @see WorkProductService
     * @ssdd
     */
    public void setWorkProductService(WorkProductService service);
}
