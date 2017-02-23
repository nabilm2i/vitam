package fr.gouv.vitam.common.client;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.model.ItemStatus;


/**
 * 
 * OperationManagementClient include all common method between ProcessManagement and Ingest Internal
 */
public interface OperationManagementClient extends MockOrRestClient {

    /**
     * getOperationProcessStatus:
     * 
     * get operation process status**
     * 
     * @param id : operation identifier*
     * @return ItemStatus response containing message and status*
     * @throws VitamClientException
     * @throws InternalServerException
     * @throws BadRequestException
     */

    ItemStatus getOperationProcessStatus(String id)
        throws VitamClientException, InternalServerException, BadRequestException;

    /**
     * 
     * getOperationProcessExecutionDetails : get operation processing execution details
     * 
     * @param id : operation identifier
     * @param query : query identifier
     * @return Engine response containing message and status
     * @throws VitamClientException
     * @throws InternalServerException
     * @throws BadRequestException
     */

    ItemStatus getOperationProcessExecutionDetails(String id, JsonNode query)
        throws VitamClientException, InternalServerException, BadRequestException;

    /**
     * cancelOperationProcessExecution : cancel processing operation
     * 
     * @param id : operation identifier
     * @return ItemStatus response containing message and status
     * @throws VitamClientException
     * @throws InternalServerException
     * @throws BadRequestException
     */
    Response cancelOperationProcessExecution(String id)
        throws InternalServerException, BadRequestException, VitamClientException;

    /**
     * updateOperationActionProcess : update operation processing status
     * 
     * 
     * @param actionId : identify the action to be executed by the workflow(next , pause,resume)
     * @param operationId : operation identifier
     * @return Response response containing message and status
     * @throws InternalServerException
     * @throws BadRequestException
     * @throws VitamClientException
     */
    Response updateOperationActionProcess(String actionId, String operationId)
        throws InternalServerException, BadRequestException, VitamClientException;


    /**
     * executeOperationProcess : execute an operation processing
     * 
     * 
     * @param contextId :define the execution context of workflow
     * @param actionId : identify the action to be executed by the workflow(next , pause,resume)
     * @param container: name of the container
     * @param workflow : id of the workflow
     * @param id : operation identifier
     * @return ItemStatus response containing message and status
     * @throws VitamClientException
     * @throws InternalServerException
     * @throws BadRequestException
     * @throws ProcessingUnauthorizeException 
     * @throws WorkflowNotFoundException
     * 
     */
    Response executeOperationProcess(String operationId, String workflow, String contextId, String actionId)
        throws InternalServerException, BadRequestException, VitamClientException, WorkflowNotFoundException;

    /**
     * initWorkFlow : init workFlow Process
     * 
     * 
     * @param contextId :define the execution context of workflow
     * @return Response response containing message and status
     * @throws VitamClientException
     * @throws VitamException
     */
    Response initWorkFlow(String contextId) throws VitamException;

    /**
     * updateVitamProcess : update vitam process status
     * 
     * 
     * @param contextId
     * @param actionId
     * @param container
     * @param workflow
     * @return
     * @throws InternalServerException
     * @throws BadRequestException
     * @throws VitamClientException
     */

    ItemStatus updateVitamProcess(String contextId, String actionId, String container, String workflow)
        throws InternalServerException, BadRequestException, VitamClientException;

    /**
     * initVitamProcess
     * 
     * @param contextId
     * @return
     * @throws InternalServerException
     * @throws VitamClientException
     * @throws BadRequestException
     */
    Response initVitamProcess(String contextId, String container, String workflow)
        throws InternalServerException, VitamClientException, BadRequestException;

    /**
     * Retrieve all the workflow operations
     * 
     * @return All details of the operations
     * @throws VitamClientInternalException
     */
    Response listOperationsDetails() throws VitamClientException;


}
