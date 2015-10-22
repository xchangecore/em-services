package com.leidos.xchangecore.core.em.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.taskingService.CreateTaskListRequestDocument;
import org.uicds.taskingService.CreateTaskListRequestDocument.CreateTaskListRequest;
import org.uicds.taskingService.CreateTaskListResponseDocument;
import org.uicds.taskingService.GetTaskListByIdRequestDocument;
import org.uicds.taskingService.GetTaskListByIdRequestDocument.GetTaskListByIdRequest;
import org.uicds.taskingService.GetTaskListByIdResponseDocument;
import org.uicds.taskingService.GetTaskListByIdResponseDocument.GetTaskListByIdResponse;
import org.uicds.taskingService.GetTaskListRequestDocument;
import org.uicds.taskingService.GetTaskListResponseDocument;
import org.uicds.taskingService.TaskListType;
import org.uicds.taskingService.UpdateTaskListRequestDocument;
import org.uicds.taskingService.UpdateTaskListRequestDocument.UpdateTaskListRequest;
import org.uicds.taskingService.UpdateTaskListResponseDocument;

import com.leidos.xchangecore.core.em.service.TaskingService;
import com.leidos.xchangecore.core.infrastructure.service.WorkProductService;
import com.leidos.xchangecore.core.infrastructure.service.impl.ProductPublicationStatus;
import com.leidos.xchangecore.core.infrastructure.util.ServiceNamespaces;
import com.leidos.xchangecore.core.infrastructure.util.WorkProductHelper;
import com.saic.precis.x2009.x06.base.IdentificationType;

/**
 * 
 * 
 * The Tasking Service allows a client to create, update, query and delete a list of tasks for a
 * resource. The resource to be tasked must be represented by an identifier of a resource instance.
 * Each resource instance in the XchangeCore system can have a list of tasks for a given incident as a
 * work product associated with the incident. Task lists are managed as a whole. There are service
 * end points for the basic creation, retrieval, update and deletion of task lists.
 * 
 * A XchangeCore TaskList contains the following information:
 * <ul>
 * <li>entityId - the resource instance identifier the task list is assigned to
 * <li>task list - a list of tasks containing
 * <ul>
 * <li>taskId - the task identifier
 * <li>description - the task description
 * <li>priority - the numeric task priority
 * <li>assignedTo - to whom the task is assigned
 * <li>assignedBy - who assigned the task
 * <li>status - the current task status including numeric completion indicator and text comment
 * <li>dueDate - a datetime value
 * </ul>
 * </ul>
 * <p>
 * The TaskType is defined as the following data structure:
 * <p>
 * <img src="doc-files/TaskType.png"/>
 * <p>
 * <p>
 * The Tasking Service manages XchangeCore work products of type "Tasking". <BR>
 * <p>
 * <!-- NEWPAGE -->
 * <p>
 * 
 * @author Ron Ridgely
 * @see <a href="../../wsdl/TaskingService.wsdl">Appendix: TaskingService.wsdl</a>
 * @see <a href="../../services/Tasking/0.1/TaskingService.xsd">Appendix: TaskingService.xsd</a>
 * @idd
 */
@Endpoint
@Transactional
public class TaskingServiceEndpoint
    implements ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(TaskingServiceEndpoint.class);

    @Autowired
    private TaskingService taskingService;

    @Autowired
    WorkProductService workProductService;

    void setTaskingService(TaskingService p) {

        taskingService = p;
    }

    /**
     * Creates a new task list for an entity and associates it with an incident.
     * 
     * @param CreateTaskListRequestDocument
     * 
     * @return CreateTaskListResponseDocument
     * @see <a href="../../services/Tasking/0.1/TaskingService.xsd">Appendix: TaskingService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_TaskingService, localPart = "CreateTaskListRequest")
    public CreateTaskListResponseDocument createTaskList(CreateTaskListRequestDocument requestDoc) {

        CreateTaskListRequest request = requestDoc.getCreateTaskListRequest();
        String entityId = request.getEntityID();
        String incidentId = request.getIncidentId();
        TaskListType taskList = request.getTaskList();

        ProductPublicationStatus status = taskingService.createTaskList(entityId,
            incidentId,
            taskList);
        CreateTaskListResponseDocument responseDoc = CreateTaskListResponseDocument.Factory.newInstance();
        responseDoc.addNewCreateTaskListResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));
        return responseDoc;
    }

    /**
     * Updates the incident task list with the input work product identification.
     * 
     * @param UpdateTaskListRequestDocument
     * 
     * @return UpdateTaskListResponseDocument
     * @see <a href="../../services/Tasking/0.1/TaskingService.xsd">Appendix: TaskingService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_TaskingService, localPart = "UpdateTaskListRequest")
    public UpdateTaskListResponseDocument updateTaskList(UpdateTaskListRequestDocument requestDoc) {

        UpdateTaskListRequest request = requestDoc.getUpdateTaskListRequest();
        TaskListType taskList = request.getTaskList();
        IdentificationType workProductIdentification = request.getWorkProductIdentification();

        ProductPublicationStatus status = taskingService.updateTaskList(taskList,
            workProductIdentification);

        UpdateTaskListResponseDocument responseDoc = UpdateTaskListResponseDocument.Factory.newInstance();
        responseDoc.addNewUpdateTaskListResponse().addNewWorkProductPublicationResponse().set(WorkProductHelper.toWorkProductPublicationResponse(status));
        return responseDoc;
    }

    /**
     * Deletes a task list with the input work product identification.
     * 
     * @param DeleteTaskListRequestDocument
     * 
     * @return DeleteTaskListResponseDocument
     * @see <a href="../../services/Tasking/0.1/TaskingService.xsd">Appendix: TaskingService.xsd</a>
     * @idd
     * @PayloadRoot(namespace = NS_TaskingService, localPart = "DeleteTaskListRequest") public
     *                        DeleteTaskListResponseDocument
     *                        deleteTaskList(DeleteTaskListRequestDocument requestDoc) {
     *                        DeleteTaskListRequest request = requestDoc.getDeleteTaskListRequest();
     *                        IdentificationType workProductIdentification =
     *                        request.getWorkProductIdentification();
     * 
     *                        ProductPublicationStatus status = taskingService.deleteTaskList
     *                        (workProductIdentification .getIdentifier().getStringValue());
     * 
     *                        DeleteTaskListResponseDocument responseDoc =
     *                        DeleteTaskListResponseDocument .Factory.newInstance();
     *                        responseDoc.addNewDeleteTaskListResponse
     *                        ().addNewWorkProductPublicationResponse().set( WorkProductHelper
     *                        .toWorkProductPublicationResponse(status));
     * 
     *                        return responseDoc; }
     */

    /**
     * Gets the task list with the input work product identification.
     * 
     * @param GetTaskListRequestDocument
     * 
     * @return GetTaskListResponseDocument
     * @see <a href="../../services/Tasking/0.1/TaskingService.xsd">Appendix: TaskingService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_TaskingService, localPart = "GetTaskListRequest")
    public GetTaskListResponseDocument getTaskList(GetTaskListRequestDocument requestDoc) {

        GetTaskListResponseDocument responseDoc = GetTaskListResponseDocument.Factory.newInstance();
        responseDoc.addNewGetTaskListResponse().setWorkProduct(WorkProductHelper.toWorkProduct(workProductService.getProduct(requestDoc.getGetTaskListRequest().getWorkProductIdentification())));

        return responseDoc;
    }

    /**
     * Gets the incident task list for an entity and incident.
     * 
     * @param GetTaskListByIdRequestDocument
     * 
     * @return GetTaskListByIdResponseDocument
     * @see <a href="../../services/Tasking/0.1/TaskingService.xsd">Appendix: TaskingService.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_TaskingService, localPart = "GetTaskListByIdRequest")
    public GetTaskListByIdResponseDocument getTaskListByEntityIdAndIncidentId(GetTaskListByIdRequestDocument requestDoc) {

        GetTaskListByIdRequest request = requestDoc.getGetTaskListByIdRequest();
        String entityId = request.getEntityID();
        String incidentId = request.getIncidentId();

        GetTaskListByIdResponseDocument responseDoc = GetTaskListByIdResponseDocument.Factory.newInstance();
        GetTaskListByIdResponse response = responseDoc.addNewGetTaskListByIdResponse();

        response.setWorkProduct(WorkProductHelper.toWorkProduct(taskingService.getTaskListByEntityIdAndIncidentId(entityId,
            incidentId)));

        return responseDoc;
    }
}
