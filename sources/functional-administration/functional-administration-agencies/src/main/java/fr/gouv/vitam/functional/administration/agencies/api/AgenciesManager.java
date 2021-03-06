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
package fr.gouv.vitam.functional.administration.agencies.api;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;


import static fr.gouv.vitam.functional.administration.agencies.api.AgenciesService.AGENCIES_IMPORT_EVENT;

/**
 * Agency validator and logBook manager
 */
class AgenciesManager {
    public static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AgenciesManager.class);

    private final GUID eip;
    private final LogbookOperationsClient logBookclient;
    private boolean warning = false;



    public AgenciesManager(LogbookOperationsClient logBookclient, GUID eip) {
        this.logBookclient = logBookclient;
        this.eip = eip;
    }

    @VisibleForTesting AgenciesManager(LogbookOperationsClient logBookclient, GUID eip, boolean warning) {
        this.logBookclient = logBookclient;
        this.eip = eip;
        this.warning = warning;
    }

    /**
     * log start process
     *
     * @throws VitamException
     * @param eventType
     */
    public void logStarted(String eventType) throws VitamException {

        final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

        final LogbookOperationParameters logbookParameters;
        logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eipUsage, eventType, eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.STARTED), eipUsage);
        logBookclient.create(logbookParameters);
    }

    /**
     * log end success process
     *
     * @throws VitamException
     */
    public void logFinish() throws VitamException {
        if (warning) {
            logEventWarning(AGENCIES_IMPORT_EVENT);
            return;
        }
        logSuccess();
    }
    /**
     * log end success process
     *
     * @throws VitamException
     */
    public void logSuccess() throws VitamException {
        logEventSuccess(AGENCIES_IMPORT_EVENT);

    }

    /**
     * log end success process
     *
     * @throws VitamException
     */
    public void logEventSuccess(String eventType) throws VitamException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eipId, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.OK), eipId);
        logBookclient.update(logbookParameters);

    }

    /**
     * log end warnig
     *
     * @throws VitamException
     */
    public void logEventWarning(String eventType) throws VitamException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        if (!warning) {
            warning = true;
        }
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eipId, eventType, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.WARNING,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.WARNING), eipId);
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(evDetData));
        logBookclient.update(logbookParameters);

    }



    /**
     * log fatal error (system or technical error)
     *
     * @param errorsDetails
     * @throws VitamException
     */
    public void logError(String errorsDetails) throws VitamException {


        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, AGENCIES_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(AGENCIES_IMPORT_EVENT, StatusCode.KO), eip);
        logbookMessageError(errorsDetails, logbookParameters);

        logBookclient.update(logbookParameters);
    }


    private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
        if (null != errorsDetails && !errorsDetails.isEmpty()) {
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put("agencyCheck", errorsDetails);

                final String wellFormedJson = SanityChecker.sanitizeJson(object);
                logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (final InvalidParseOperationException e) {
                // Do nothing
            }
        }
    }


    public void setEvDetData(ObjectNode evDetData) {
        this.evDetData = evDetData;
    }

    private ObjectNode evDetData = JsonHandler.createObjectNode();


}
