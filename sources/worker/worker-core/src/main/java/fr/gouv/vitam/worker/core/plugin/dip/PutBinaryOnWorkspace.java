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
package fr.gouv.vitam.worker.core.plugin.dip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

public class PutBinaryOnWorkspace extends ActionHandler {

    private static final String PUT_BINARY_ON_WORKSPACE = "PUT_BINARY_ON_WORKSPACE";
    private static final String DEFAULT_STORAGE_STRATEGY = "default";

    private static final int GUID_TO_PATH_RANK = 0;


    private StorageClientFactory storageClientFactory;

    public PutBinaryOnWorkspace() {
        this.storageClientFactory = StorageClientFactory.getInstance();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        final ItemStatus itemStatus = new ItemStatus(PUT_BINARY_ON_WORKSPACE);

        try (
            InputStream inputStream = new FileInputStream((File) handler.getInput(GUID_TO_PATH_RANK));
            StorageClient storageClient = storageClientFactory.getClient()) {

            Map<String, Object> guidToPath = JsonHandler.getMapFromInputStream(inputStream);

            Response response = storageClient
                .getContainerAsync(DEFAULT_STORAGE_STRATEGY, param.getObjectName(), StorageCollectionType.OBJECTS);

            handler.transferInputStreamToWorkspace((String) guidToPath.get(param.getObjectName()),
                (InputStream) response.getEntity(),
                null, false);
            itemStatus.increment(StatusCode.OK);
            return new ItemStatus(PUT_BINARY_ON_WORKSPACE).setItemsStatus(PUT_BINARY_ON_WORKSPACE, itemStatus);

        } catch (StorageServerClientException | IOException | InvalidParseOperationException |
            StorageNotFoundException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {

    }
}
