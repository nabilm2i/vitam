/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.workspace.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.workspace.api.ContentAddressableStorage;
import fr.gouv.vitam.workspace.api.exception.*;
import fr.gouv.vitam.workspace.api.model.ContainerInformation;
import fr.gouv.vitam.workspace.common.Entry;
import fr.gouv.vitam.workspace.common.ErrorMessage;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import javax.ws.rs.client.*;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * Workspace client which calls rest services
 */
// FIXME REVIEW Since Factory => class and constructors as package protected
public class WorkspaceClient implements ContentAddressableStorage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceClient.class);
    private static final String RESOURCE_PATH = "/workspace/v1";

    public static final String X_DIGEST_ALGORITHM = "X-digest-algorithm";
    public static final String X_DIGEST = "X-digest";

    private final String serviceUrl;
    private final Client client;

    /**
     * Instantiates a workspace client with an url parameter
     *
     * @param serviceUrl
     */
    // FIXME REVIEW User should not specify such url: the factory should handle it (see Logbook client)
    public WorkspaceClient(String serviceUrl) {
        this.serviceUrl = serviceUrl + RESOURCE_PATH;

        final ClientConfig config = new ClientConfig();
        config.register(JacksonJsonProvider.class);
        config.register(JacksonFeature.class);
        config.register(MultiPartFeature.class);
        client = ClientBuilder.newClient(config);
    }

    @Override
    public void createContainer(String containerName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);

        Response response = null;
        try {
            response = client.target(serviceUrl).path("/containers").request()
                .post(Entity.json(new Entry(containerName)));
            if (Status.CREATED.getStatusCode() == response.getStatus()) {
                LOGGER.info(containerName + ": " + Response.Status.CREATED.getReasonPhrase());
            } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage());
                throw new ContentAddressableStorageAlreadyExistException(
                    ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage());
            } else {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }

    }

    @Override
    public void purgeContainer(String containerName) {
        // TODO
    }

    @Override
    public void deleteContainer(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);

        Response response = null;
        try {
            response = client.target(serviceUrl).path("/containers/" + containerName).request().delete();

            if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
                LOGGER.info(containerName + ": " + Response.Status.NO_CONTENT.getReasonPhrase());
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public void deleteContainer(String containerName, boolean recursive) {
        // TODO
    }

    @Override
    public boolean isExistingContainer(String containerName) {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        final Response response = client.target(serviceUrl).path("/containers/" + containerName).request().head();
        return Response.Status.OK.getStatusCode() == response.getStatus();
    }

    @Override
    public void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        final Response response = client.target(serviceUrl).path("/containers/" + containerName + "/folders").request()
            .post(Entity.json(new Entry(folderName)));
        if (Status.CREATED.getStatusCode() == response.getStatus()) {
            LOGGER.info(containerName + "/" + folderName + ": " + Response.Status.CREATED.getReasonPhrase());
        } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
            LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
            throw new ContentAddressableStorageAlreadyExistException(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
        } else {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    @Override
    public void deleteFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        Response response = null;
        try {
            response =
                client.target(serviceUrl).path("/containers/" + containerName + "/folders/" + folderName)
                    .request().delete();

            if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
                LOGGER.info(containerName + "/" + folderName + ": " + Response.Status.NO_CONTENT.getReasonPhrase());
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.FOLDER_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public boolean isExistingFolder(String containerName, String folderName) {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        final Response response =
            client.target(serviceUrl).path("/containers/" + containerName + "/folders/" + folderName)
                .request().head();
        return Response.Status.OK.getStatusCode() == response.getStatus();
    }

    // FIXME REVIEW change the contract of the implementation later on (POST on /objects/name directly in
    // order to prevent multipart)
    @Override
    public void putObject(String containerName, String objectName, InputStream stream)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);


        Response response = null;
        try (final FormDataMultiPart multiPart = new FormDataMultiPart()) {

            multiPart.bodyPart(new FormDataBodyPart("objectName", objectName, MediaType.TEXT_PLAIN_TYPE));
            multiPart.bodyPart(
                new StreamDataBodyPart("object", stream, objectName, MediaType.APPLICATION_OCTET_STREAM_TYPE));
            response = client.target(serviceUrl).path("/containers/" + containerName + "/objects").request()
                .post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE));

            if (Status.CREATED.getStatusCode() == response.getStatus()) {
                LOGGER.info(containerName + "/" + objectName + ": " + Response.Status.CREATED.getReasonPhrase());
            } else {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (IOException exc) { // NOSONAR no log to do
            // Do nothing since FormDataMultiPart#close() cannot throw IOException based on its implementation
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }

    }

    @Override
    public InputStream getObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        InputStream stream = null;
        try {
            WebTarget webTarget = client.target(serviceUrl);
            webTarget = webTarget.path("/containers/" + containerName + "/objects/" + objectName);
            Invocation.Builder builder = webTarget.request(MediaType.MULTIPART_FORM_DATA);
            builder = builder.accept(MediaType.APPLICATION_OCTET_STREAM);
            response = builder.get();


            /*response =
                client.target(serviceUrl).path("/containers/" + containerName + "/objects/" + objectName)
                    .request(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_OCTET_STREAM).get();*/

            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                // TODO : this is ugly but necessarily in order to close the response and avoid concurrent issues
                // to be improved
                InputStream streamClosedAutomatically = response.readEntity(InputStream.class);
                try {
                    stream = new ByteArrayInputStream(IOUtils.toByteArray(streamClosedAutomatically));
                } catch (IOException e) {
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                    throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                }
                return stream;
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }

    }

    @Override
    public void deleteObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response =
                client.target(serviceUrl).path("/containers/" + containerName + "/objects/" + objectName)
                    .request().delete();

            if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
                LOGGER.info(containerName + "/" + objectName + ": " + Response.Status.NO_CONTENT.getReasonPhrase());
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }

    }

    @Override
    public boolean isExistingObject(String containerName, String objectName) {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        final Response response =
            client.target(serviceUrl).path("/containers/" + containerName + "/objects/" + objectName)
                .request().head();
        return Response.Status.OK.getStatusCode() == response.getStatus();
    }

    @Override
    public List<URI> getListUriDigitalObjectFromFolder(String containerName, String folderName) {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        final Response response =
            client.target(serviceUrl).path("/containers/" + containerName + "/folders/" + folderName)
                .request().get();

        if (response != null && Response.Status.OK.getStatusCode() == response.getStatus()) {
            return response.readEntity(new GenericType<List<URI>>() {
            });
        } else {
            return Collections.<URI>emptyList();
        }
    }

    @Override
    public void unzipObject(String containerName, String folderName, InputStream inputStreamObject)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageAlreadyExistException, ContentAddressableStorageZipException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);

        if (isExistingContainer(containerName)) {
            if (!isExistingFolder(containerName, folderName)) {

                final Response response =
                    client.target(serviceUrl).path("/containers/" + containerName + "/folders/" + folderName).request()
                        .put(Entity.entity(inputStreamObject, MediaType.APPLICATION_OCTET_STREAM));

                if (Response.Status.CREATED.getStatusCode() == response.getStatus()) {
                    LOGGER.info(containerName + File.separator + folderName + " : " +
                        Response.Status.CREATED.getReasonPhrase());
                } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                    LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
                    throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
                    LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                    throw new ContentAddressableStorageAlreadyExistException(
                        ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                } else if (Status.BAD_REQUEST.getStatusCode() == response.getStatus() &&
                    "application/json".equals(response.getHeaderString("Content-Type"))) {
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                    throw new ContentAddressableStorageZipException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                } else {
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                    throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                }

            } else {
                LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                throw new ContentAddressableStorageAlreadyExistException(
                    ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
            }

        } else {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
        }

    }

    @Override
    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName, algo);

        final Response response =
            client.target(serviceUrl).path("/containers/" + containerName + "/objects/" + objectName)
                .request()
                .header(X_DIGEST_ALGORITHM, algo.getName())
                .head();

        if (Response.Status.OK.getStatusCode() == response.getStatus()) {
            return response.getHeaderString(X_DIGEST);
        } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
        } else {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    @Override
    public ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            response =
                client.target(serviceUrl).path("/containers/" + containerName).request().get();
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return response.readEntity(ContainerInformation.class);
            } else {
                throw new ContentAddressableStorageNotFoundException(response.getStatusInfo().getReasonPhrase());
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    @Override
    public JsonNode getObjectInformation(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response =
                client.target(serviceUrl).path("/containers/" + containerName + "/objects/" + objectName)
                    .request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).get();

            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return response.readEntity(JsonNode.class);
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }
}
