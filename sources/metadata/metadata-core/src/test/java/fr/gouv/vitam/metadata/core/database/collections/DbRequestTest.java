/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.core.database.collections;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.isNull;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lt;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.missing;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.ne;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.nin;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.path;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.range;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.size;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.term;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.all;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.tenant;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.add;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.inc;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.min;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.push;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.set;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.unset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.PathQuery;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.DeleteMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.parser.request.multiple.DeleteParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;


public class DbRequestTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DbRequestTest.class);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static final Integer TENANT_ID_0 = 0;
    private static final Integer TENANT_ID_1 = 1;
    private static final Integer TENANT_ID_2 = 2;
    private static final Integer TENANT_ID_3 = 3;
    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";
    private static ElasticsearchTestConfiguration config = null;

    private static ElasticsearchAccessMetadata esClient;
    private static ElasticsearchAccess esClientWithoutVitambBehavior;

    private static final boolean CREATE = false;
    private static final boolean DROP = false;
    private static final String MY_INT = "MyInt";
    private static final String CREATED_DATE = "CreatedDate";
    private static final String DESCRIPTION = "Description";
    private static final String TITLE = "Title";
    private static final String MY_BOOLEAN = "MyBoolean";
    private static final String MY_FLOAT = "MyFloat";
    private static final String UNKNOWN_VAR = "unknown";
    private static final String VALUE_MY_TITLE = "MyTitle";
    private static final String ARRAY_VAR = "ArrayVar";
    private static final String ARRAY2_VAR = "Array2Var";
    private static final String EMPTY_VAR = "EmptyVar";
    static final int tenantId = 0;

    static final List tenantList = Lists.newArrayList(TENANT_ID_0, TENANT_ID_1, TENANT_ID_2, TENANT_ID_3);

    static MongoDbAccessMetadataImpl mongoDbAccess;
    static MongoDbVarNameAdapter mongoDbVarNameAdapter;

    private static final String REQUEST_SELECT_TEST = "{$query: {$eq: {\"id\" : \"id\" }}, $projection : []}";
    private static final String UUID2 = "aebaaaaaaaaaaaabaahbcakzu2stfryaaaaq";


    private static final String REQUEST_INSERT_TEST_1 =
        "{ \"#id\": \"aebaaaaaaaaaaaabaahbcakzu2stfryabbaq\", \"id\": \"id\" }";
    private static final String REQUEST_INSERT_TEST_2 =
        "{ \"#id\": \"aeaqaaaaaaaaaaababid6akzxqwg6qqaaaaq\", \"id\": \"id\" }";
    private static final String REQUEST_SELECT_TEST_ES_1 =
        "{$query: { $match : { 'Description' : 'OK' , '$max_expansions' : 1  } }}";
    private static final String REQUEST_SELECT_TEST_ES_2 =
        "{$query: { $match : { 'Description' : 'dèscription OK' , '$max_expansions' : 1  } }}";
    private static final String REQUEST_SELECT_TEST_ES_3 =
        "{$query: { $match : { 'Description' : 'est OK description' , '$max_expansions' : 1  } }}";
    private static final String REQUEST_SELECT_TEST_ES_4 =
        "{$query: { $or : [ { $match : { 'Title' : 'Vitam' , '$max_expansions' : 1  } }, " +
            "{$match : { 'Description' : 'vitam' , '$max_expansions' : 1  } }" +
            "] } }";
    private static final String REQUEST_INSERT_TEST_ES_1_TENANT_1 =
        "{ \"#id\": \"aebaaaaaaaaaaaabaahbcakzu2stfryaabaq\", " +
            "\"#tenant\": 1, " +
            "\"Title\": \"title vitam\", " +
            "\"Description\": \"description est OK\"," +
            "\"#management\" : {\"ClassificationRule\" : [ {\"Rule\" : \"RuleId\"} ] } }";
    private static final String REQUEST_INSERT_TEST_ES =
        "{ \"#id\": \"" + UUID2 + "\", " + "\"Title\": \"title vitam\", " +
            "\"Description\": \"description est OK\"," +
            "\"#management\" : {\"ClassificationRule\" : [ {\"Rule\" : \"RuleId\"} ] } }";
    private static final String REQUEST_INSERT_TEST_ES_2 =
        "{ \"#id\": \"aeaqaaaaaet33ntwablhaaku6z67pzqaaaar\", \"Title\": \"title vitam\", \"Description\": \"description est OK\" }";
    private static final String REQUEST_INSERT_TEST_ES_3 =
        "{ \"#id\": \"aeaqaaaaaet33ntwablhaaku6z67pzqaaaat\", \"Title\": \"title vitam\", \"Description\": \"description est OK\" }";
    private static final String REQUEST_INSERT_TEST_ES_4 =
        "{ \"#id\": \"aeaqaaaaaet33ntwablhaaku6z67pzqaaaas\", \"Title\": \"title sociales test_abcd_underscore othervalue france.pdf\", \"Description\": \"description est OK\" }";
    private static final String UUID1 = "aeaqaaaaaaaaaaabab4roakztdjqziaaaaaq";
    private static final String REQUEST_UPDATE_INDEX_TEST =
        "{$roots:['" + UUID1 +
            "'],$query:[],$filter:{},$action:[{$set:{'date':'09/09/2015'}},{$set:{'Title':'ArchiveDoubleTest'}}]}";
    private static final String REQUEST_SELECT_TEST_ES_UPDATE =
        "{$query: { $eq : { '#id' : '" + UUID1 + "' } }}";
    private static final String REQUEST_INSERT_TEST_ES_UPDATE = "REQUEST_INSERT_TEST_ES_UPDATE.json";
    private static final String REQUEST_INSERT_TEST_ES_UPDATE_KO =
        "{ \"#id\": \"aeaqaaaaaagbcaacabg44ak45e54criaaaaq\", \"#tenant\": 0, \"Title\": \"Archive3\", " +
            "\"_mgt\": {\"OriginatingAgency\": \"XXXXXXX\"}," +
            " \"DescriptionLevel\": \"toto\" }";
    private static final String REQUEST_UPDATE_INDEX_TEST_KO =
        "{$roots:['aeaqaaaaaagbcaacabg44ak45e54criaaaaq'],$query:[],$filter:{},$action:[{$set:{'date':'09/09/2015'}},{$set:{'title':'Archive2'}}]}";

    @Rule
    public MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "vitam-test", "Unit", "ObjectGroup");

    private MongoClient mongoClient = mongoRule.getMongoClient();

    @BeforeClass
    public static void beforeClass() throws Exception {
        try {
            config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {


        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, config.getTcpPort()));

        esClient = new ElasticsearchAccessMetadata(CLUSTER_NAME, nodes);
        esClientWithoutVitambBehavior = new ElasticsearchAccess(CLUSTER_NAME, nodes);

        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", CREATE, esClient, tenantList);
        mongoDbVarNameAdapter = new MongoDbVarNameAdapter();

    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (config == null) {
            return;
        }
        if (DROP) {
            for (final MetadataCollections col : MetadataCollections.values()) {
                if (col.getCollection() != null) {
                    col.getCollection().drop();
                }
            }
        }

        JunitHelper.stopElasticsearchForTest(config);
        esClient.close();
    }

    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * .
     *
     * @throws MetaDataExecutionException
     */
    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void testExecRequest() throws MetaDataExecutionException {
        // input data
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        try {
            final DbRequest dbRequest = new DbRequest();
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
            try {
                insertParser.parse(insertRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("InsertParser: {}", insertParser);
            // Now execute the request
            executeRequest(dbRequest, insertParser);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
            try {
                selectParser.parse(selectRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", selectParser);
            // Now execute the request
            executeRequest(dbRequest, selectParser);

            // UPDATE
            final JsonNode updateRequest = createUpdateRequestWithUUID(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
            try {
                updateParser.parse(updateRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("UpdateParser: {}", updateParser);
            // Now execute the request
            executeRequest(dbRequest, updateParser);

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                selectParser.parse(selectRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", selectParser);
            // Now execute the request
            executeRequest(dbRequest, selectParser);

            // DELETE
            final JsonNode deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            final DeleteParserMultiple deleteParser = new DeleteParserMultiple(mongoDbVarNameAdapter);
            try {
                deleteParser.parse(deleteRequest);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + deleteParser.toString());
            // Now execute the request
            executeRequest(dbRequest, deleteParser);
        } catch (final InstantiationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (final IllegalAccessException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (final MetaDataAlreadyExistException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (final MetaDataNotFoundException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (final InvalidParseOperationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } finally {
            // clean
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }

    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * .
     */
    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void testExecRequestThroughRequestParserHelper() throws MetaDataExecutionException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // input data
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser = null;
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            try {
                requestParser =
                    RequestParserHelper.getParser(insertRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("InsertParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = createUpdateRequestWithUUID(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            try {
                requestParser =
                    RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("UpdateParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            final JsonNode deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            try {
                requestParser =
                    RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + requestParser.toString());

            executeRequest(dbRequest, requestParser);
        } catch (final InstantiationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (final IllegalAccessException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (final MetaDataAlreadyExistException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (final MetaDataNotFoundException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } catch (final InvalidParseOperationException e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } finally {
            // clean
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }


    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * .
     *
     * @throws InvalidParseOperationException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataExecutionException
     */
    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void testExecRequestThroughAllCommands()
        throws MetaDataExecutionException, MetaDataAlreadyExistException, MetaDataNotFoundException,
        InvalidParseOperationException, InstantiationException, IllegalAccessException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // input data
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser = null;
            // INSERT
            final JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            try {
                requestParser =
                    RequestParserHelper.getParser(insertRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("InsertParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            JsonNode selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = clientRichUpdateBuild(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            try {
                requestParser =
                    RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("UpdateParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT ALL
            selectRequest = clientRichSelectAllBuild(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            final JsonNode deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            try {
                requestParser =
                    RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + requestParser.toString());
            // Now execute the request
            executeRequest(dbRequest, requestParser);
        } finally {
            // clean
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
        }
    }

    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * .
     *
     * @throws Exception
     */
    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void testExecRequestMultiple() throws Exception {
        // input data
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        final GUID uuid2 = GUIDFactory.newUnitGUID(tenantId);
        try {
            final DbRequest dbRequest = new DbRequest();
            RequestParserMultiple requestParser = null;

            // INSERT
            JsonNode insertRequest = createInsertRequestWithUUID(uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            requestParser = RequestParserHelper.getParser(insertRequest, mongoDbVarNameAdapter);
            LOGGER.debug("InsertParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            insertRequest = createInsertChild2ParentRequest(uuid2, uuid);
            // Now considering insert request and parsing it as in Data Server (POST command)
            requestParser = RequestParserHelper.getParser(insertRequest, mongoDbVarNameAdapter);
            LOGGER.debug("InsertParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT based on UUID
            JsonNode selectRequest = createSelectRequestWithOnlyUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT based on UUID
            selectRequest = createSelectRequestWithOnlyUUID(uuid2);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            selectRequest = createSelectRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            selectRequest = clientSelect2Build(uuid2);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT
            selectRequest = clientSelectMultiQueryBuild(uuid, uuid2);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // UPDATE
            final JsonNode updateRequest = createUpdateRequestWithUUID(uuid);
            // Now considering update request and parsing it as in Data Server (PATCH command)
            try {
                requestParser =
                    RequestParserHelper.getParser(updateRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("UpdateParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // SELECT ALL
            selectRequest = createSelectAllRequestWithUUID(uuid);
            // Now considering select request and parsing it as in Data Server (GET command)
            try {
                requestParser =
                    RequestParserHelper.getParser(selectRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("SelectParser: {}", requestParser);
            // Now execute the request
            executeRequest(dbRequest, requestParser);

            // DELETE
            JsonNode deleteRequest = clientDelete2Build(uuid2);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            try {
                requestParser =
                    RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + requestParser.toString());
            // Now execute the request
            executeRequest(dbRequest, requestParser);
            deleteRequest = createDeleteRequestWithUUID(uuid);
            // Now considering delete request and parsing it as in Data Server (DELETE command)
            try {
                requestParser =
                    RequestParserHelper.getParser(deleteRequest, mongoDbVarNameAdapter);
            } catch (final InvalidParseOperationException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            LOGGER.debug("DeleteParser: " + requestParser.toString());
            // Now execute the request
            executeRequest(dbRequest, requestParser);
        } finally {
            // clean
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid.toString()));
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid2.toString()));
        }
    }


    /**
     * Test method for
     * {@link fr.gouv.vitam.database.collections.DbRequest#execRequest(fr.gouv.vitam.request.parser.RequestParser, fr.gouv.vitam.database.collections.Result)}
     * with sorts.
     */
    @Test
    @RunWithCustomExecutor
    public void testExecRequestWithSort() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID uuid1 = GUIDFactory.newUnitGUID(tenantId);
        final GUID uuid2 = GUIDFactory.newUnitGUID(tenantId);

        try {
            final DbRequest dbRequest = new DbRequest();
            final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
            final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
            // INSERT
            final ObjectNode insertRequest1 = (ObjectNode) createInsertRequestWithUUID(uuid1);
            ((ObjectNode) insertRequest1.get("$data")).put(TITLE, "mon titreA Complet");
            final ObjectNode insertRequest2 = (ObjectNode) createInsertRequestWithUUID(uuid2);
            ((ObjectNode) insertRequest2.get("$data")).put(TITLE, "mon titreB Complet");
            insertRequest2.putArray("_up").addAll((ArrayNode) JsonHandler.toJsonNode(Arrays.asList(uuid1)));
            // insert1
            insertParser.parse(insertRequest1);
            LOGGER.debug("InsertParser1: {}", insertParser);
            executeRequest(dbRequest, insertParser);
            // insert2
            insertParser.parse(insertRequest2);
            LOGGER.debug("InsertParser2: {}", insertParser);
            executeRequest(dbRequest, insertParser);

            // SELECT
            // select with desc sort on title and one query
            SelectMultiQuery selectRequest = new SelectMultiQuery();
            selectRequest.addUsedProjection(all());
            selectRequest
                .addQueries(or().add(eq(id(), uuid1.toString()))
                    .add(eq(id(), uuid2.toString()))
                    .setDepthLimit(2));
            selectRequest.addOrderByDescFilter(TITLE);
            selectParser.parse(selectRequest.getFinalSelect());
            LOGGER.debug("SelectParser: {}", selectParser);
            final Result result0 = dbRequest.execRequest(selectParser, null);
            assertEquals(2L, result0.getNbResult());
            final List<MetadataDocument<?>> list = result0.getFinal();
            LOGGER.warn(list.toString());
            assertEquals("mon titreB Complet", ((Document) list.get(0)).getString(TITLE));
            assertEquals("mon titreA Complet", ((Document) list.get(1)).getString(TITLE));

            // select with desc sort on title and two queries
            selectRequest = new SelectMultiQuery();
            selectRequest.addUsedProjection(all());
            selectRequest
                .addQueries(eq(MY_BOOLEAN, false),
                    or().add(eq(id(), uuid1.toString())).add(eq(id(), uuid2.toString())).setDepthLimit(0));
            selectRequest.addOrderByDescFilter(TITLE);
            selectParser.parse(selectRequest.getFinalSelect());
            LOGGER.debug("SelectParser: {}", selectParser);
            final Result result1 = dbRequest.execRequest(selectParser, null);
            assertEquals(2L, result1.getNbResult());
            final List<MetadataDocument<?>> list1 = result1.getFinal();
            assertEquals("mon titreB Complet", ((Document) list1.get(0)).getString(TITLE));
            assertEquals("mon titreA Complet", ((Document) list1.get(1)).getString(TITLE));

            // select with desc sort on title and two queries and elastic search
            selectRequest = new SelectMultiQuery();
            selectRequest.addUsedProjection(all());
            selectRequest.addQueries(match(TITLE, "mon Complet").setDepthLimit(0),
                or().add(eq(id(), uuid1.toString())).add(eq(id(), uuid2.toString())).setDepthLimit(0));
            selectRequest.addOrderByDescFilter(TITLE);
            selectParser.parse(selectRequest.getFinalSelect());
            LOGGER.debug("SelectParser: {}", selectParser);
            final Result result2 = dbRequest.execRequest(selectParser, null);
            assertEquals(2L, result2.getNbResult());
            final List<MetadataDocument<?>> list2 = result2.getFinal();
            assertEquals("mon titreB Complet", ((Document) list2.get(0)).getString(TITLE));
            assertEquals("mon titreA Complet", ((Document) list2.get(1)).getString(TITLE));

        } catch (final Exception e1) {
            e1.printStackTrace();
            fail(e1.getMessage());
        } finally {
            // clean
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid1.toString()));
            MetadataCollections.C_UNIT.getCollection().deleteOne(new Document(MetadataDocument.ID, uuid2.toString()));
        }
    }


    @Test
    @RunWithCustomExecutor
    public void testInsertUnitRequest() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        final GUID uuid2 = GUIDFactory.newUnitGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser = null;

        requestParser = RequestParserHelper.getParser(createInsertRequestWithUUID(uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        assertEquals(1, MetadataCollections.C_UNIT.getCollection().count());

        requestParser =
            RequestParserHelper.getParser(createInsertChild2ParentRequest(uuid2, uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        assertEquals(2, MetadataCollections.C_UNIT.getCollection().count());
    }

    @Test
    @RunWithCustomExecutor
    public void ShouldIndexElasticSearchWithGoodUnitSchema() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        final GUID uuid2 = GUIDFactory.newUnitGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final DbRequest dbRequest = new DbRequest();

        RequestParserMultiple requestParser = null;
        requestParser = RequestParserHelper.getParser(createInsertRequestWithUUID(uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        requestParser =
            RequestParserHelper.getParser(createInsertChild2ParentRequest(uuid2, uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);

        final QueryBuilder qb1 = QueryBuilders.matchPhrasePrefixQuery("_id", uuid);
        final QueryBuilder qb2 = QueryBuilders.matchPhrasePrefixQuery("_id", uuid2);

        final SearchRequestBuilder request =
            esClientWithoutVitambBehavior.getClient()
                .prepareSearch(getIndexName(MetadataCollections.C_UNIT, tenantId))
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setTypes(VitamCollection.getTypeunique()).setExplain(false)
                .setSize(GlobalDatas.LIMIT_LOAD);
        SearchResponse response;
        request.setQuery(qb1);
        response = request.get();
        assertEquals(1, response.getHits().totalHits());
        request.setQuery(qb2);
        response = request.get();
        assertEquals(1, response.getHits().totalHits());
    }

    /**
     * @param dbRequest
     * @param requestParser
     * @throws InvalidParseOperationException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataExecutionException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     */
    private void executeRequest(DbRequest dbRequest, RequestParserMultiple requestParser)
        throws MetaDataExecutionException, MetaDataAlreadyExistException, MetaDataNotFoundException,
        InvalidParseOperationException, InstantiationException, IllegalAccessException {

        final Result result = dbRequest.execRequest(requestParser, null);
        LOGGER.warn("XXXXXXXX " + requestParser.getClass().getSimpleName() + " Result XXXXXXXX: " + result);
        assertEquals("Must have 1 result", result.getNbResult(), 1);
        assertEquals("Must have 1 result", result.getCurrentIds().size(), 1);
        esClient.refreshIndex(MetadataCollections.C_UNIT, tenantId);
        esClient.refreshIndex(MetadataCollections.C_OBJECTGROUP, tenantId);
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createDeleteRequestWithUUID(GUID uuid) {
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        try {
            delete.addQueries(and().add(eq(id(), uuid.toString()), eq(TITLE, VALUE_MY_TITLE)));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("DeleteString: " + delete.getFinalDelete().toString());
        return delete.getFinalDelete();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createSelectAllRequestWithUUID(GUID uuid) {
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            select.addUsedProjection(all())
                .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // selectRequestString = select.getFinalSelect().toString();
        LOGGER.debug("SelectAllString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createUpdateRequestWithUUID(GUID uuid) {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        try {
            update.addActions(set("NewVar", false), inc(MY_INT, 2), set(DESCRIPTION, "New description"))
                .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("UpdateString: " + update.getFinalUpdate().toString());
        return update.getFinalUpdate();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createSelectRequestWithUUID(GUID uuid) {
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            select.addUsedProjection(id(), TITLE, DESCRIPTION)
                .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("SelectString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createSelectRequestWithOnlyUUID(GUID uuid) {
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            select.addQueries(eq(id(), uuid.toString()).setDepthLimit(10));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("SelectString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode createInsertRequestWithUUID(GUID uuid) {
        // INSERT
        final List<String> list = Arrays.asList("val1", "val2");
        final ObjectNode data = JsonHandler.createObjectNode().put(id(), uuid.toString())
            .put(TITLE, VALUE_MY_TITLE).put(DESCRIPTION, "Ma description est bien détaillée")
            .put(CREATED_DATE, "" + LocalDateUtil.now()).put(MY_INT, 20)
            .put(tenant(), tenantId)
            .put(MY_BOOLEAN, false).putNull(EMPTY_VAR).put(MY_FLOAT, 2.0);
        try {
            data.putArray(ARRAY_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            data.putArray(ARRAY2_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.addData(data);
        LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        return insert.getFinalInsert();
    }


    /**
     * @param uuid child
     * @param uuid2 parent
     * @return
     */
    private JsonNode createInsertChild2ParentRequest(GUID child, GUID parent) throws Exception {
        final ObjectNode data = JsonHandler.createObjectNode().put(id(), child.toString())
            .put(TITLE, VALUE_MY_TITLE + "2").put(DESCRIPTION, "Ma description2 vitam")
            .put(CREATED_DATE, "" + LocalDateUtil.now()).put(MY_INT, 10);
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.addData(data).addQueries(eq(VitamFieldsHelper.id(), parent.toString()));
        LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        return insert.getFinalInsert();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientRichSelectAllBuild(GUID uuid) {
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            select.addUsedProjection(all())
                .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE),
                    exists(CREATED_DATE), missing(UNKNOWN_VAR), isNull(EMPTY_VAR),
                    or().add(in(ARRAY_VAR, "val1"), nin(ARRAY_VAR, "val3")),
                    gt(MY_INT, 1), lt(MY_INT, 100),
                    ne(MY_BOOLEAN, true), range(MY_FLOAT, 1.0, false, 100.0, true),
                    term(TITLE, VALUE_MY_TITLE), size(ARRAY2_VAR, 2)));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("SelectAllString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientRichUpdateBuild(GUID uuid) {
        final UpdateMultiQuery update = new UpdateMultiQuery();
        try {
            update.addActions(set("NewVar", false), inc(MY_INT, 2), set(DESCRIPTION, "New description"),
                unset(UNKNOWN_VAR), push(ARRAY_VAR, "val2"), min(MY_FLOAT, 1.5),
                add(ARRAY2_VAR, "val2"))
                .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("UpdateString: " + update.getFinalUpdate().toString());
        return update.getFinalUpdate();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientSelect2Build(GUID uuid) {
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            select.addUsedProjection(id(), TITLE, DESCRIPTION)
                .addQueries(eq(id(), uuid.toString()).setDepthLimit(2));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("SelectString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid father
     * @param uuid2 son
     * @return
     */
    private JsonNode clientSelectMultiQueryBuild(GUID uuid, GUID uuid2) {
        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            select.addUsedProjection(id(), TITLE, DESCRIPTION)
                .addQueries(and().add(eq(id(), uuid.toString()), match(TITLE, VALUE_MY_TITLE)),
                    eq(id(), uuid2.toString()));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("SelectString: " + select.getFinalSelect().toString());
        return select.getFinalSelect();
    }

    /**
     * @param uuid
     * @return
     */
    private JsonNode clientDelete2Build(GUID uuid) {
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        try {
            delete.addQueries(path(uuid.toString()));
        } catch (final InvalidCreateOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        LOGGER.debug("DeleteString: " + delete.getFinalDelete().toString());
        return delete.getFinalDelete();
    }

    @Test
    @RunWithCustomExecutor
    public void testResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final DbRequest dbRequest = new DbRequest();
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        LOGGER.debug("InsertParser: {}", insertParser);
        final Result result = dbRequest.execRequest(insertParser, null);
        assertEquals("[]", result.getFinal().toString());

        final Bson projection = null;
        result.setFinal(projection);
        assertEquals(1, result.currentIds.size());
        assertEquals(1, (int) result.nbResult);

        result.putFrom(result);
        assertEquals(1, result.currentIds.size());
        assertEquals(1, (int) result.nbResult);
    }

    @Test
    public void testMongoDbAccess() {
        for (final MetadataCollections col : MetadataCollections.values()) {
            if (col.getCollection() != null) {
                col.getCollection().drop();
            }
        }
        final MongoDatabase db = mongoDbAccess.getMongoDatabase();
        assertEquals(0, db.getCollection("Unit").count());
        assertEquals(0, db.getCollection("Objectgroup").count());
        mongoDbAccess = new MongoDbAccessMetadataImpl(mongoClient, "vitam-test", CREATE, esClient, tenantList);
        assertNotNull(mongoDbAccess.toString());
    }


    private ObjectNode createInsertRequestGO(GUID uuid, GUID uuidParent) throws InvalidParseOperationException {
        // Create Insert command as in Internal Vitam Modules
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.resetFilter();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        final JsonNode json =
            JsonHandler
                .getFromString("{\"#id\":\"" +
                    uuid +
                    "\", \"#Originating_Agency\": \"FRAN_NP_050056\",  \"FileInfo\": { \"Filename\": \"Filename0\"}, \"#qualifiers\": [{ \"qualifier\": \"BinaryMaster\"}]}");
        System.out.println(json.get("_qualifiers"));
        // "OriginatingAgency": "FRAN_NP_050056"
        insert.addData((ObjectNode) json);
        insert.addRoots(uuidParent.getId());
        final ObjectNode insertRequest = insert.getFinalInsert();
        LOGGER.debug("InsertString: " + insertRequest);
        return insertRequest;
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertGORequest() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser = null;

        requestParser = RequestParserHelper.getParser(createInsertRequestWithUUID(uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        Result result = checkExistence(dbRequest, uuid, false);
        assertFalse(result.isError());

        final GUID uuid2 = GUIDFactory.newObjectGroupGUID(tenantId);
        requestParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser.parse(createInsertRequestGO(uuid2, uuid));

        executeRequest(dbRequest, requestParser);
        result = checkExistence(dbRequest, uuid2, true);
        assertFalse(result.isError());
    }

    @Test
    @RunWithCustomExecutor
    public void testOGElasticsearchIndex() throws Exception {
        final GUID uuid = GUIDFactory.newUnitGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser = null;

        requestParser = RequestParserHelper.getParser(createInsertRequestWithUUID(uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        Result result = checkExistence(dbRequest, uuid, false);
        assertFalse(result.isError());
        final GUID uuid2 = GUIDFactory.newObjectGroupGUID(tenantId);
        requestParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser.parse(createInsertRequestGO(uuid2, uuid));

        executeRequest(dbRequest, requestParser);
        result = checkExistence(dbRequest, uuid2, true);
        assertFalse(result.isError());

        final QueryBuilder qb = QueryBuilders.termQuery("_id", uuid2);

        // Use new esClient for have full elastic index and not just the id in the response.
        final SearchRequestBuilder request =
            esClientWithoutVitambBehavior.getClient()
                .prepareSearch(getIndexName(MetadataCollections.C_OBJECTGROUP, tenantId))
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setTypes(VitamCollection.getTypeunique()).setExplain(false)
                .setSize(GlobalDatas.LIMIT_LOAD);
        request.setQuery(qb);
        final SearchResponse response = request.get();
        assertTrue(response != null);
        checkElasticResponseField(response, uuid.getId());
        final JsonNode jsonNode = JsonHandler.getFromString(response.toString());
        final SearchHits hits = response.getHits();
        assertEquals(1, hits.getTotalHits());

        LOGGER.debug("Elasticsearch Index for objectGroup ", response);
    }

    /**
     * Check elastic Search Response field
     *
     * @param response ElasticSearch response
     * @param parentUuid the parentUuid
     */
    private void checkElasticResponseField(SearchResponse response, String parentUuid) {
        final Iterator<SearchHit> iterator = response.getHits().iterator();
        while (iterator.hasNext()) {
            final SearchHit searchHit = iterator.next();
            final Map<String, Object> source = searchHit.getSource();
            for (final String key : source.keySet()) {
                if ("_qualifiers".equals(key)) {
                    final List<Map<String, Object>> qualifiers = (List<Map<String, Object>>) source.get(key);
                    for (final Map qualifier : qualifiers) {
                        assertEquals("BinaryMaster", qualifier.get("qualifier"));
                    }
                } else if ("_Originating_Agency".equals(key)) {
                    assertEquals("FRAN_NP_050056", source.get(key));
                } else if ("FileInfo".equals(key)) {
                    final Map<String, Object> fileInfo = (Map<String, Object>) source.get(key);
                    assertEquals("Filename0", fileInfo.get("Filename"));
                } else if ("_up".equals(key)) {
                    final List<String> ups = (List<String>) source.get(key);
                    assertEquals(parentUuid, ups.get(0));
                }
            }

        }
    }


    private String getIndexName(final MetadataCollections collection, Integer tenantId) {
        return collection.getName().toLowerCase() + "_" + tenantId.toString();
    }

    private Result checkExistence(DbRequest dbRequest, GUID uuid, boolean isOG)
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataExecutionException,
        MetaDataAlreadyExistException, MetaDataNotFoundException, InstantiationException, IllegalAccessException {
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(eq(VitamFieldsHelper.id(), uuid.getId()));
        if (isOG) {
            select.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        }
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(select.getFinalSelect());
        return dbRequest.execRequest(selectParser, null);
    }

    @Test
    @RunWithCustomExecutor
    public void testUnitParentForlastInsertFilterProjection() throws Exception {
        final DbRequest dbRequest = new DbRequest();
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        final GUID uuid = GUIDFactory.newObjectGroupGUID(tenantId);
        final GUID uuidUnit = GUIDFactory.newUnitGUID(tenantId);


        final JsonNode insertRequest = createInsertRequestWithUUID(uuidUnit);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        try {
            insertParser.parse(insertRequest);
        } catch (final InvalidParseOperationException e) {
            fail(e.getMessage());
        }
        executeRequest(dbRequest, insertParser);

        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.resetFilter();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addRoots(uuidUnit.getId());
        final ObjectNode json =
            (ObjectNode) JsonHandler
                .getFromString("{\"#id\":\"" +
                    uuid +
                    "\", \"#qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"Title\":\"title1\"}");

        insert.addData(json);
        final ObjectNode insertNode = insert.getFinalInsert();

        final RequestParserMultiple requestParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser.parse(insertNode);
        executeRequest(dbRequest, requestParser);
        assertFalse(requestParser.getRequest().getRoots().isEmpty());
        // Check _og
        Result result = checkExistence(dbRequest, uuidUnit, false);
        assertFalse(result.isError());
        List<MetadataDocument<?>> list = result.getFinal();
        final Document unit = list.get(0);
        assertTrue(unit.getString("_og").equals(uuid.getId()));

        // Check _up is set as _og
        result = checkExistence(dbRequest, uuid, true);
        assertFalse(result.isError());
        list = result.getFinal();
        final Document og = list.get(0);
        System.err.println(og);
        System.err.println(og.get("_up"));
        System.err.println(uuidUnit.getId());
        assertTrue(((List<String>) og.get("_up")).contains(uuidUnit.getId()));

    }

    @Test
    @RunWithCustomExecutor
    public void testRequestWithObjectGroupQuery() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID uuid01 = GUIDFactory.newUnitGUID(tenantId);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser =
            RequestParserHelper.getParser(createInsertRequestWithUUID(uuid01), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        Result result = checkExistence(dbRequest, uuid01, false);
        assertFalse(result.isError());
        final GUID uuid02 = GUIDFactory.newUnitGUID(tenantId);
        requestParser = RequestParserHelper.getParser(createInsertRequestWithUUID(uuid02), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        result = checkExistence(dbRequest, uuid02, false);
        assertFalse(result.isError());

        final GUID uuid1 = GUIDFactory.newObjectGroupGUID(tenantId);
        final GUID uuid2 = GUIDFactory.newObjectGroupGUID(tenantId);
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());

        final ObjectNode json =
            (ObjectNode) JsonHandler
                .getFromString("{\"#id\":\"" +
                    uuid1 +
                    "\", \"#qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description\" : \"Test\"}}, \"Title\":\"title1\"}");
        final ObjectNode json1 =
            (ObjectNode) JsonHandler
                .getFromString("{\"#id\":\"" +
                    uuid2 +
                    "\", \"#qualifiers\" :{\"Physique Master\" : {\"PhysiqueOId\" : \"abceff\", \"Description1\" : \"Test\"}}, \"Title\":\"title1\"}");
        insert.addData(json).addRoots(uuid01.getId());
        ObjectNode insertRequestString = insert.getFinalInsert();
        requestParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser.parse(insertRequestString);
        executeRequest(dbRequest, requestParser);

        final PathQuery query = path(uuid02.getId());
        insert.reset().addQueries(query).addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addData(json1);
        insertRequestString = insert.getFinalInsert();
        final RequestParserMultiple requestParser1 = new InsertParserMultiple(mongoDbVarNameAdapter);
        requestParser1.parse(insertRequestString);
        executeRequest(dbRequest, requestParser1);
    }


    @Test
    @RunWithCustomExecutor
    public void testSelectResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final DbRequest dbRequest = new DbRequest();
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result = dbRequest.execRequest(selectParser, null);
        assertEquals("[]", result.getFinal().toString());

    }


    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResult() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_1);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, tenantId);
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result2 = dbRequest.execRequest(selectParser, null);
        assertEquals(1, result2.nbResult);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSelectNoResultSinceOtherTenantUsed() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(2);
        final DbRequest dbRequest = new DbRequest();
        // unit is insterted with tenantId = 0
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_2);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        VitamThreadUtils.getVitamSession().setTenantId(3);
        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        final Result result2 = dbRequest.execRequest(selectParser, null);
        assertEquals(0, result2.nbResult);
    }


    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResultWithES() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_2);

        // OG ???

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_ES);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_1);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser1.parse(selectRequest1);
        LOGGER.debug("SelectParser: {}", selectRequest1);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_2);
        final Result resultSelect1 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelect1.nbResult);

        final JsonNode selectRequest2 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_2);
        final SelectParserMultiple selectParser2 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser2.parse(selectRequest2);
        LOGGER.debug("SelectParser: {}", selectRequest2);
        final Result resultSelect2 = dbRequest.execRequest(selectParser2, null);
        assertEquals(1, resultSelect2.nbResult);

        final JsonNode selectRequest3 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_3);
        final SelectParserMultiple selectParser3 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser3.parse(selectRequest3);
        LOGGER.debug("SelectParser: {}", selectRequest3);
        final Result resultSelect3 = dbRequest.execRequest(selectParser3, null);
        assertEquals(1, resultSelect3.nbResult);

        final JsonNode selectRequest4 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_4);
        final SelectParserMultiple selectParser4 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser4.parse(selectRequest4);
        LOGGER.debug("SelectParser: {}", selectRequest4);
        final Result resultSelect4 = dbRequest.execRequest(selectParser4, null);
        assertEquals(1, resultSelect4.nbResult);

        InsertMultiQuery insert = new InsertMultiQuery();
        insert.parseData(REQUEST_INSERT_TEST_ES_2).addRoots(UUID2);
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_2);

        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(match("Description", "description OK").setDepthLimit(1))
            .addRoots(UUID2);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel0 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel0.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar",
            resultSelectRel0.getCurrentIds().iterator().next().toString());

        select = new SelectMultiQuery();
        select.addQueries(match("Description", "description OK").setDepthLimit(1))
            .addRoots(UUID2);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel1 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel1.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar",
            resultSelectRel1.getCurrentIds().iterator().next().toString());

        select = new SelectMultiQuery();
        select.addQueries(match("Description", "description OK").setDepthLimit(3))
            .addRoots(UUID2);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result<MetadataDocument<?>> resultSelectRel3 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel3.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar",
            resultSelectRel3.getCurrentIds().iterator().next().toString());

        insert = new InsertMultiQuery();
        insert.parseData(REQUEST_INSERT_TEST_ES_3).addRoots("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar");
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_2);

        final Result<MetadataDocument<?>> resultSelectRel4 = dbRequest.execRequest(selectParser1, null);
        assertEquals(2, resultSelectRel4.nbResult);
        for (final String root : resultSelectRel4.getCurrentIds()) {
            assertTrue(root.equalsIgnoreCase("aeaqaaaaaet33ntwablhaaku6z67pzqaaaat") ||
                root.equalsIgnoreCase("aeaqaaaaaet33ntwablhaaku6z67pzqaaaar"));
        }

        insert = new InsertMultiQuery();
        insert.parseData(REQUEST_INSERT_TEST_ES_4).addRoots(UUID2);
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_2);

        select = new SelectMultiQuery();
        select.addQueries(match("Title", "othervalue").setDepthLimit(1))
            .addRoots(UUID2);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result<MetadataDocument<?>> resultSelectRel5 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel5.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel5.getCurrentIds().iterator().next().toString());

        // Check for "France.pdf"
        select = new SelectMultiQuery();
        select.addRoots(UUID2).addQueries(match("Title", "Frânce").setDepthLimit(1));
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel6 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel6.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel6.getCurrentIds().iterator().next().toString());

        // Check for "social vs sociales"
        select = new SelectMultiQuery();
        select.addRoots(UUID2).addQueries(match("Title", "social").setDepthLimit(1));
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel7 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel7.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel7.getCurrentIds().iterator().next().toString());

        // Check for "name_with_underscore"
        select = new SelectMultiQuery();
        select
            .addQueries(match("Title", "abcd").setDepthLimit(1));
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectParser1.getRequest());
        final Result resultSelectRel8 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel8.nbResult);
        assertEquals("aeaqaaaaaet33ntwablhaaku6z67pzqaaaas",
            resultSelectRel8.getCurrentIds().iterator().next().toString());
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResultWithESTenant1() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_ES_1_TENANT_1);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_1);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser1.parse(selectRequest1);
        LOGGER.debug("SelectParser: {}", selectRequest1);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_1);
        final Result resultSelect1 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelect1.nbResult);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSelectUnitResultWithESTenant1ButTenant0() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);

        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_ES_1_TENANT_1);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_1);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser1.parse(selectRequest1);
        LOGGER.debug("SelectParser: {}", selectRequest1);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_1);
        final Result resultSelect1 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelect1.nbResult);
    }


    @Test
    @RunWithCustomExecutor
    public void testUpdateUnitResult() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        // insert title ARchive 3
        final DbRequest dbRequest = new DbRequest();
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        final InsertMultiQuery insert = new InsertMultiQuery();
        String requestInsertTestEsUpdate = IOUtils.toString(PropertiesUtils.getResourceAsStream(REQUEST_INSERT_TEST_ES_UPDATE), "UTF-8");
        insert.parseData(requestInsertTestEsUpdate);
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);

        // check value should exist in the collection
        SelectParserMultiple selectParser2 = new SelectParserMultiple(mongoDbVarNameAdapter);
        SelectMultiQuery select1 = new SelectMultiQuery();
        select1.addQueries(match("Title", "Archive").setDepthLimit(0)).addRoots(UUID1);
        selectParser2.parse(select1.getFinalSelect());
        Result resultSelectRel6 = dbRequest.execRequest(selectParser2, null);
        assertEquals(1, resultSelectRel6.nbResult);

        // update title Archive 3 to Archive 2
        final JsonNode updateRequest = JsonHandler.getFromString(REQUEST_UPDATE_INDEX_TEST);
        final UpdateParserMultiple updateParser = new UpdateParserMultiple(mongoDbVarNameAdapter);
        updateParser.parse(updateRequest);
        LOGGER.debug("UpdateParser: {}", updateParser.getRequest());
        final Result result2 = dbRequest.execRequest(updateParser, null);
        LOGGER.debug("result2", result2.getNbResult());
        assertEquals(1, result2.nbResult);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);

        // check old value should not exist in the collection
        selectParser2 = new SelectParserMultiple(mongoDbVarNameAdapter);
        select1 = new SelectMultiQuery();
        select1.addQueries(match("Title", "ArchiveTest").setDepthLimit(0)).addRoots(UUID1);
        selectParser2.parse(select1.getFinalSelect());
        resultSelectRel6 = dbRequest.execRequest(selectParser2, null);
        assertEquals(0, resultSelectRel6.nbResult);

        // check new value should exist in the collection
        final JsonNode selectRequest1 = JsonHandler.getFromString(REQUEST_SELECT_TEST_ES_UPDATE);
        final SelectParserMultiple selectParser1 = new SelectParserMultiple(mongoDbVarNameAdapter);
        final SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(match("Title", "ArchiveDoubleTest").setDepthLimit(0)).addRoots(UUID1);
        selectParser1.parse(select.getFinalSelect());
        LOGGER.debug("SelectParser: {}", selectRequest1);
        final Result resultSelectRel5 = dbRequest.execRequest(selectParser1, null);
        assertEquals(1, resultSelectRel5.nbResult);
        assertEquals(UUID1,
            resultSelectRel5.getCurrentIds().iterator().next().toString());
    }


    @Test(expected = MetaDataExecutionException.class)
    @RunWithCustomExecutor
    public void testUpdateKOSchemaUnitResultThrowsException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        // insert title ARchive 3
        final DbRequest dbRequest = new DbRequest();
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.parseData(REQUEST_INSERT_TEST_ES_UPDATE_KO).addRoots("aeaqaaaaaagbcaacabg44ak45e54criaaaaq");
        insertParser.parse(insert.getFinalInsert());
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);
        esClient.refreshIndex(MetadataCollections.C_UNIT, TENANT_ID_0);

        final JsonNode updateRequest = JsonHandler.getFromString(REQUEST_UPDATE_INDEX_TEST_KO);
        final UpdateParserMultiple updateParser = new UpdateParserMultiple();
        updateParser.parse(updateRequest);
        LOGGER.debug("UpdateParser: {}", updateParser.getRequest());
        dbRequest.execRequest(updateParser, null);
    }

    private static final JsonNode buildQueryJsonWithOptions(String query, String data)
        throws Exception {
        return JsonHandler.getFromString(new StringBuilder()
            .append("{ $roots : [ '' ], ")
            .append("$query : [ " + query + " ], ")
            .append("$data : " + data + " }")
            .toString());
    }


    @Test
    @RunWithCustomExecutor
    public void testInsertUnitWithTenant() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        final JsonNode insertRequest = buildQueryJsonWithOptions("", REQUEST_INSERT_TEST_1);
        final InsertParserMultiple insertParser = new InsertParserMultiple(mongoDbVarNameAdapter);
        insertParser.parse(insertRequest);
        LOGGER.debug("InsertParser: {}", insertParser);
        dbRequest.execRequest(insertParser, null);

        final JsonNode selectRequest = JsonHandler.getFromString(REQUEST_SELECT_TEST);
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(selectRequest);
        LOGGER.debug("SelectParser: {}", selectRequest);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);
        final Result result3 = dbRequest.execRequest(selectParser, null);
        assertEquals(0, result3.nbResult);
    }

    private JsonNode createInsertRequestGOTenant(GUID uuid) throws InvalidParseOperationException {
        // INSERT
        final List<String> list = Arrays.asList("val1", "val2");
        final ObjectNode data = JsonHandler.createObjectNode().put(id(), uuid.toString())
            .put(TITLE, VALUE_MY_TITLE).put(DESCRIPTION, "Ma description est bien détaillée")
            .put(CREATED_DATE, "" + LocalDateUtil.now()).put(MY_INT, 20)
            .put(tenant(), tenantId)
            .put(MY_BOOLEAN, false).putNull(EMPTY_VAR).put(MY_FLOAT, 2.0);
        try {
            data.putArray(ARRAY_VAR).addAll((ArrayNode) JsonHandler.toJsonNode(list));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            data.putArray("_up").addAll((ArrayNode) JsonHandler.toJsonNode(list));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addData(data);
        LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        return insert.getFinalInsert();
    }

    @Test
    @RunWithCustomExecutor
    public void testInsertGOWithTenant() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);

        final GUID uuid = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser = null;
        requestParser = RequestParserHelper.getParser(createInsertRequestGOTenant(uuid), mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);

        final SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(eq(VitamFieldsHelper.id(), uuid.getId()));
        select.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(select.getFinalSelect());
        final Result result = dbRequest.execRequest(selectParser, null);
        assertEquals(1, result.nbResult);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_1);
        final Result result2 = dbRequest.execRequest(selectParser, null);
        assertEquals(0, result2.nbResult);
    }

    @Test
    @RunWithCustomExecutor
    public void testOrAndMatch() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID_0);

        final GUID uuid = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        final DbRequest dbRequest = new DbRequest();
        RequestParserMultiple requestParser = null;
        // INSERT 1
        ObjectNode data = JsonHandler.createObjectNode().put(id(), uuid.toString())
            .put(TITLE, "Rectorat 1").put(DESCRIPTION, "Ma description public est bien détaillée")
            .put("DescriptionLevel", "Item")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(tenant(), tenantId);
        InsertMultiQuery insert = new InsertMultiQuery();
        insert.addHintFilter(BuilderToken.FILTERARGS.UNITS.exactToken());
        insert.addData(data);
        LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        ObjectNode insertNode = insert.getFinalInsert();
        requestParser = RequestParserHelper.getParser(insertNode, mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        //Insert 2
        final GUID uuid2 = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        ObjectNode data2 = JsonHandler.createObjectNode().put(id(), uuid2.toString())
            .put(TITLE, "Rectorat 2").put(DESCRIPTION, "Ma description privé est bien détaillée")
            .put("DescriptionLevel", "Item")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(tenant(), tenantId);
        insert.reset();
        insert.addHintFilter(BuilderToken.FILTERARGS.UNITS.exactToken());
        insert.addData(data2);
        LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        insertNode = insert.getFinalInsert();
        requestParser = RequestParserHelper.getParser(insertNode, mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        //Insert 3 false description
        final GUID uuid3 = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        ObjectNode data3 = JsonHandler.createObjectNode().put(id(), uuid3.toString())
            .put(TITLE, "Rectorat 3").put(DESCRIPTION, "Ma description est bien détaillée")
            .put("DescriptionLevel", "Item")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(tenant(), tenantId);
        insert.reset();
        insert.addHintFilter(BuilderToken.FILTERARGS.UNITS.exactToken());
        insert.addData(data3);
        LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        insertNode = insert.getFinalInsert();
        requestParser = RequestParserHelper.getParser(insertNode, mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);
        //Insert 4 false Title
        final GUID uuid4 = GUIDFactory.newObjectGroupGUID(TENANT_ID_0);
        ObjectNode data4 = JsonHandler.createObjectNode().put(id(), uuid4.toString())
            .put(TITLE, "Title 4").put(DESCRIPTION, "Ma description public est bien détaillée")
            .put("DescriptionLevel", "Item")
            .put(CREATED_DATE, "" + LocalDateUtil.now())
            .put(tenant(), tenantId);
        insert.reset();
        insert.addHintFilter(BuilderToken.FILTERARGS.UNITS.exactToken());
        insert.addData(data4);
        LOGGER.debug("InsertString: " + insert.getFinalInsert().toString());
        insertNode = insert.getFinalInsert();
        requestParser = RequestParserHelper.getParser(insertNode, mongoDbVarNameAdapter);
        executeRequest(dbRequest, requestParser);

        String query = "{\"$roots\": [],\"$query\": [{\"$or\": " +
            "[{\"$and\": [{\"$match\": {\"Title\": \"Rectorat\"}},{\"$match\": {\"Description\": \"public\"}}]}," +
            "{\"$and\": [{\"$match\": {\"Title\": \"Rectorat\"}},{\"$match\": {\"Description\": \"privé\"}}]}]," +
            "\"$depth\": 20}],\"$filter\": {\"$hint\": [\"units\"], \"$orderby\": {\"TransactedDate\": 1}}," +
            "\"$projection\": {\"$fields\": {\"TransactedDate\": 1,\"#id\": 1,\"Title\": 1,\"#object\": 1,\"Description\": 1}}}";
        final SelectParserMultiple selectParser = new SelectParserMultiple(mongoDbVarNameAdapter);
        selectParser.parse(JsonHandler.getFromString(query));
        final Result result = dbRequest.execRequest(selectParser, null);

        // Clean
        final DeleteMultiQuery delete = new DeleteMultiQuery();
        delete.addQueries(in(VitamFieldsHelper.id(), uuid.toString(), uuid2.toString(), uuid3.toString(), uuid4.toString()));
        delete.setMult(true);
        final DeleteParserMultiple deleteParser = new DeleteParserMultiple(mongoDbVarNameAdapter);
        deleteParser.parse(delete.getFinalDelete());
        dbRequest.execRequest(deleteParser, null);
        assertEquals(2, result.nbResult);
    }
}
