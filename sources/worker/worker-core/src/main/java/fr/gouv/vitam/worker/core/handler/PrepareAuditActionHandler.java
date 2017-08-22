/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuditWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.File;
import java.util.Map;

/**
 * PrepareAuditActionHandler
 */
public class PrepareAuditActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(PrepareAuditActionHandler.class);

    private static final String HANDLER_ID = "LIST_OBJECTGROUP_ID";
    private static final String RESULTS = "$results";
    private static final String ID = "#id";
    private boolean asyncIO = false;

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        ArrayNode ogIdList = JsonHandler.createArrayNode();

        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {

            SelectMultiQuery query = new SelectMultiQuery();
            Map<WorkerParameterName, String> mapParameters = param.getMapParameters();
            String auditType = mapParameters.get(WorkerParameterName.auditType);
            if (auditType.toLowerCase().equals("tenant")) {
                auditType = BuilderToken.PROJECTIONARGS.TENANT.exactToken();
            } else if (auditType.toLowerCase().equals("originatingagency")) {
                auditType = BuilderToken.PROJECTIONARGS.ORIGINATING_AGENCY.exactToken();
            }
            query.setQuery(QueryHelper.eq(auditType,
                mapParameters.get(WorkerParameterName.objectId)));
            query.setProjection(JsonHandler.createObjectNode().put(ID, 1));
            JsonNode searchResults = metadataClient.selectObjectGroups(query.getFinalSelect());
            if (searchResults.get(RESULTS) != null) {
                ArrayNode ogList = (ArrayNode) searchResults.get(RESULTS);
                for (JsonNode og : ogList) {
                    ogIdList.add(og.get(ID).asText());
                }
            }

            File file = handler.getNewLocalFile(AuditWorkflowConstants.AUDIT_FILE);
            JsonHandler.writeAsFile(ogIdList, file);
            workspaceClient.createContainer(param.getContainerName());
            handler.transferFileToWorkspace(AuditWorkflowConstants.AUDIT_FILE, file, true, asyncIO);
            itemStatus.increment(StatusCode.OK);
        } catch (InvalidParseOperationException | InvalidCreateOperationException| MetaDataException e) {
            LOGGER.error("Metadata errors : ", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (ContentAddressableStorageAlreadyExistException e) {
            LOGGER.error("Workspace errors : ",e);
            itemStatus.increment(StatusCode.FATAL);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
    }
}