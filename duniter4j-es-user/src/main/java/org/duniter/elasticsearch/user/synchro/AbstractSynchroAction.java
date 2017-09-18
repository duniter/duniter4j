package org.duniter.elasticsearch.service.synchro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.HttpService;
import org.duniter.core.client.service.exception.HttpUnauthorizeException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.exception.DuniterElasticsearchException;
import org.duniter.elasticsearch.exception.InvalidFormatException;
import org.duniter.elasticsearch.exception.InvalidSignatureException;
import org.duniter.elasticsearch.model.SearchResponse;
import org.duniter.elasticsearch.model.SearchScrollResponse;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.model.UserEvent;
import org.duniter.elasticsearch.user.service.AbstractService;
import org.duniter.elasticsearch.user.service.UserEventService;
import org.duniter.elasticsearch.user.service.UserService;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.io.IOException;
import java.util.*;

public abstract class AbstractSynchroAction extends AbstractService implements SynchroAction {

    private static final String SCROLL_PARAM_VALUE = "1m";

    public interface InsertListener {
        void onInsert(String id, JsonNode source);
    }

    private String fromIndex;
    private String fromType;
    private String toIndex;
    private String toType;
    private String issuerFieldName = Record.PROPERTY_ISSUER;
    private String versionFieldName = Record.PROPERTY_TIME;

    private HttpService httpService;

    private boolean enableUpdate = false;
    private List<InsertListener> insertListeners;

    public AbstractSynchroAction(String index, String type,
                                 Duniter4jClient client,
                                 PluginSettings pluginSettings,
                                 CryptoService cryptoService,
                                 ThreadPool threadPool) {
        this(index, type, index, type, client, pluginSettings, cryptoService, threadPool);
    }

    public AbstractSynchroAction(String fromIndex, String fromType,
                                 String toIndex, String toType,
                                 Duniter4jClient client,
                                 PluginSettings pluginSettings,
                                 CryptoService cryptoService,
                                 ThreadPool threadPool) {
        super("duniter.synchro." + toIndex, client, pluginSettings, cryptoService);
        this.fromIndex = fromIndex;
        this.fromType = fromType;
        this.toIndex = toIndex;
        this.toType = toType;
        threadPool.scheduleOnStarted(() -> httpService = ServiceLocator.instance().getHttpService());
    }


    @Override
    public EndpointApi getEndPointApi() {
        return EndpointApi.ES_USER_API;
    }

    @Override
    public void handleSynchronize(Peer peer,
                                  long fromTime,
                                  SynchroResult result) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkArgument(fromTime >= 0);
        Preconditions.checkNotNull(result);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s] [%s/%s] Synchronizing where [%s > %s]...", peer, toIndex, toType, versionFieldName, fromTime));
        }

        try {
            QueryBuilder query = createQuery(fromTime);
            synchronize(peer, query, result);
        }
        catch(Exception e1) {
            // Log the first error
            if (logger.isDebugEnabled()) {
                logger.error(e1.getMessage(), e1);
            }
            else {
                logger.error(e1.getMessage());
            }
        }
    }

    public void addInsertListener(InsertListener listener) {
        if (insertListeners == null) {
            insertListeners = Lists.newArrayList();
        }
        insertListeners.add(listener);
    }

    /* -- protected methods -- */

    protected void notifyInsertListeners(final String id, final JsonNode source) {
        if (insertListeners == null) return;
        insertListeners.forEach(l -> l.onInsert(id, source));
    }

    protected QueryBuilder createQuery(long fromTime) {

        return QueryBuilders.boolQuery()
                .should(QueryBuilders.rangeQuery("time").gte(fromTime));
    }

    private HttpPost createScrollRequest(Peer peer,
                                         String fromIndex, String fromType,
                                         QueryBuilder query) {
        HttpPost httpPost = new HttpPost(httpService.getPath(peer, fromIndex, fromType, "_search?scroll=" + SCROLL_PARAM_VALUE));
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");

        try {
            // Query to String
            BytesStreamOutput bos = new BytesStreamOutput();
            XContentBuilder builder = new XContentBuilder(JsonXContent.jsonXContent, bos);
            query.toXContent(builder, null);
            builder.flush();

            // Sort on "_doc" - see https://www.elastic.co/guide/en/elasticsearch/reference/2.4/search-request-scroll.html
            String content = String.format("{\"query\":%s,\"size\":%s, \"sort\": [\"_doc\"]}",
                    bos.bytes().toUtf8(),
                    pluginSettings.getIndexBulkSize());
            httpPost.setEntity(new StringEntity(content, "UTF-8"));

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[%s] [%s] [%s/%s] Sending POST scroll request: %s", peer.getCurrency(), peer, fromIndex, fromType, content));
            }

        } catch (IOException e) {
            throw new TechnicalException("Error while preparing search query: " + e.getMessage(), e);
        }

        return httpPost;
    }

    private HttpPost createNextScrollRequest(Peer peer,
                                             String scrollId) {

        HttpPost httpPost = new HttpPost(httpService.getPath(peer, "_search", "scroll"));
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        httpPost.setEntity(new StringEntity(String.format("{\"scroll\": \"%s\", \"scroll_id\": \"%s\"}",
                SCROLL_PARAM_VALUE,
                scrollId), "UTF-8"));
        return httpPost;
    }

    private SearchScrollResponse executeAndParseRequest(Peer peer, HttpUriRequest request) {
        try {
            // Execute query & parse response
            JsonNode node = httpService.executeRequest(request, JsonNode.class, String.class);
            return node == null ? null : new SearchScrollResponse(node);
        } catch (HttpUnauthorizeException e) {
            throw new TechnicalException(String.format("[%s] [%s] [%s/%s] Unable to access (%s).", peer.getCurrency(), peer, fromIndex, fromType, e.getMessage()), e);
        } catch (TechnicalException e) {
            throw new TechnicalException(String.format("[%s] [%s] [%s/%s] Unable to synchronize: %s", peer.getCurrency(), peer, fromIndex, fromType, e.getMessage()), e);
        } catch (Exception e) {
            throw new TechnicalException(String.format("[%s] [%s] [%s/%s] Unable to parse response: ", peer.getCurrency(), peer, fromIndex, fromType, e.getMessage()), e);
        }
    }

    private void synchronize(Peer peer,
                             QueryBuilder query,
                             SynchroResult result) {

        if (!client.existsIndex(toIndex)) {
            throw new TechnicalException(String.format("Unable to import changes. Index [%s] not exists", toIndex));
        }

        ObjectMapper objectMapper = getObjectMapper();

        long counter = 0;
        boolean stop = false;
        String scrollId = null;
        int total = 0;
        while(!stop) {
            SearchScrollResponse response;
            if (scrollId == null) {
                HttpUriRequest request = createScrollRequest(peer, fromIndex, fromType, query);
                response = executeAndParseRequest(peer, request);
                if (response != null) {
                    scrollId = response.getScrollId();
                    total = response.getHits().getTotalHits();
                    if (total > 0 && logger.isDebugEnabled()) {
                        logger.debug(String.format("[%s] [%s] [%s/%s] %s docs to check...", peer.getCurrency(), peer, toIndex, toType, total));
                    }
                }
            }
            else {
                HttpUriRequest request = createNextScrollRequest(peer, scrollId);
                response =  executeAndParseRequest(peer, request);
            }

            if (response == null) {
                stop = true;
            }
            else {
                counter += fetchAndIndex(peer, response, objectMapper, result);
                stop = counter >= total;
            }
        }
    }

    private long fetchAndIndex(final Peer peer,
                               SearchScrollResponse response,
                               final ObjectMapper objectMapper,
                               SynchroResult result) {
        boolean debug = logger.isTraceEnabled();

        long counter = 0;

        long insertHits = 0;
        long updateHits = 0;
        long invalidSignatureHits = 0;

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        bulkRequest.setRefresh(true);

        for (Iterator<SearchResponse.SearchHit> hits = response.getHits(); hits.hasNext();){
            SearchResponse.SearchHit hit = hits.next();
            String id = hit.getId();
            JsonNode source = hit.getSource();

            if (source == null) {
                logger.error(String.format("[%s] [%s] [%s/%s/%s] No source found. Skipping.", peer.getCurrency(), peer,
                        toIndex, toType, id));
            }
            else {
                counter++;

                try {
                    String issuer = source.get(issuerFieldName).asText();
                    if (StringUtils.isBlank(issuer)) {
                        throw new InvalidFormatException(String.format("Invalid format: missing or null %s field.", issuerFieldName));
                    }
                    Long version = source.get(versionFieldName).asLong();
                    if (version == null) {
                        throw new InvalidFormatException(String.format("Invalid format: missing or null %s field.", versionFieldName));
                    }

                    Map<String, Object> existingFields = client.getFieldsById(toIndex, toType, id, versionFieldName, issuerFieldName);
                    boolean exists = existingFields != null;

                    // Insert (new doc)
                    if (!exists) {

                        if (debug) {
                            logger.trace(String.format("[%s] [%s] [%s/%s] insert _id=%s\n%s", peer.getCurrency(), peer, toIndex, toType, id, source.toString()));
                        }

                        // FIXME: some user/profile document failed ! - see issue #11
                        // Il semble que le format JSON ne soit pas le même que celui qui a été signé
                        try {
                            readAndVerifyIssuerSignature(source, issuerFieldName);
                        } catch (InvalidSignatureException e) {
                            invalidSignatureHits++;
                            // FIXME: should enable this log (after issue #11 resolution)
                            //logger.warn(String.format("[%s] [%s/%s/%s] %s.\n%s", peer, toIndex, toType, id, e.getMessage(), source.toString()));
                        }

                        bulkRequest.add(client.prepareIndex(toIndex, toType, id)
                                .setSource(objectMapper.writeValueAsBytes(source))
                        );

                        // Notify insert listeners
                        notifyInsertListeners(id, source);

                        insertHits++;
                    }

                    // Existing doc: do update (if enable)
                    else if (enableUpdate){

                        // Check same issuer
                        String existingIssuer = (String) existingFields.get(issuerFieldName);
                        if (!Objects.equals(issuer, existingIssuer)) {
                            throw new InvalidFormatException(String.format("Invalid document: not same [%s].", issuerFieldName));
                        }

                        // Check version
                        Number existingVersion = ((Number) existingFields.get(versionFieldName));
                        boolean doUpdate = (existingVersion == null || version > existingVersion.longValue());

                        if (doUpdate) {
                            if (debug) {
                                logger.trace(String.format("[%s] [%s] [%s/%s] update _id=%s\n%s", peer.getCurrency(), peer, toIndex, toType, id, source.toString()));
                            }

                            // FIXME: some user/profile document failed ! - see issue #11
                            // Il semble que le format JSON ne soit pas le même que celui qui a été signé
                            try {
                                readAndVerifyIssuerSignature(source, issuerFieldName);
                            } catch (InvalidSignatureException e) {
                                invalidSignatureHits++;
                                // FIXME: should enable this log (after issue #11 resolution)
                                //logger.warn(String.format("[%s] [%s/%s/%s] %s.\n%s", peer, toIndex, toType, id, e.getMessage(), source.toString()));
                            }

                            bulkRequest.add(client.prepareIndex(toIndex, toType, id)
                                    .setSource(objectMapper.writeValueAsBytes(source)));

                            updateHits++;
                        }
                    }

                } catch (DuniterElasticsearchException e) {
                    if (logger.isDebugEnabled()) {
                        logger.warn(String.format("[%s] [%s] [%s/%s/%s] %s. Skipping.\n%s", peer.getCurrency(), peer, toIndex, toType, id, e.getMessage(), source.toString()));
                    } else {
                        logger.warn(String.format("[%s] [%s] [%s/%s/%s] %s. Skipping.", peer.getCurrency(), peer, toIndex, toType, id, e.getMessage()));
                    }
                    // Skipping document (continue)
                } catch (Exception e) {
                    logger.error(String.format("[%s] [%s] [%s/%s/%s] %s. Skipping.", peer.getCurrency(), peer, toIndex, toType, id, e.getMessage()), e);
                    // Skipping document (continue)
                }
            }
        }

        if (bulkRequest.numberOfActions() > 0) {

            // Flush the bulk if not empty
            BulkResponse bulkResponse = bulkRequest.get();
            Set<String> missingDocIds = new LinkedHashSet<>();

            // If failures, continue but save missing blocks
            if (bulkResponse.hasFailures()) {
                // process failures by iterating through each bulk response item
                for (BulkItemResponse itemResponse : bulkResponse) {
                    boolean skip = !itemResponse.isFailed()
                            || missingDocIds.contains(itemResponse.getId());
                    if (!skip) {
                        if (debug) {
                            logger.debug(String.format("[%s] [%s] [%s/%s] could not process _id=%s: %s. Skipping.", peer.getCurrency(), peer, toIndex, toType, itemResponse.getId(), itemResponse.getFailureMessage()));
                        }
                        missingDocIds.add(itemResponse.getId());
                    }
                }
            }
        }

        // update result stats
        result.addInserts(toIndex, toType, insertHits);
        result.addUpdates(toIndex, toType, updateHits);
        result.addInvalidSignatures(toIndex, toType, invalidSignatureHits);

        return counter;
    }

    protected void setIssuerFieldName(String issuerFieldName) {
        this.issuerFieldName = issuerFieldName;
    }

    protected void setVersionFieldName(String versionFieldName) {
        this.versionFieldName = versionFieldName;
    }

    protected ObjectMapper getObjectMapper() {
        return JacksonUtils.getThreadObjectMapper();
    }

    protected void setEnableUpdate(boolean enableUpdate) {
        this.enableUpdate = enableUpdate;
    }

    private void synchronizeUserEventsByReference(Peer peer, UserEvent.Reference reference, long fromTime, SynchroResult result) {

        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .filter(QueryBuilders.rangeQuery(Record.PROPERTY_TIME).gte(fromTime));

        // Query = filter on reference
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (StringUtils.isNotBlank(reference.getIndex())) {
            boolQuery.filter(QueryBuilders.termQuery(UserEvent.PROPERTY_REFERENCE + "." + UserEvent.Reference.PROPERTY_INDEX, reference.getIndex()));
        }
        if (StringUtils.isNotBlank(reference.getType())) {
            boolQuery.filter(QueryBuilders.termQuery(UserEvent.PROPERTY_REFERENCE + "." + UserEvent.Reference.PROPERTY_TYPE, reference.getType()));
        }

        query.should(QueryBuilders.nestedQuery(UserEvent.PROPERTY_REFERENCE, QueryBuilders.constantScoreQuery(boolQuery)));

        //safeSynchronizeIndex(peer, UserService.INDEX, UserEventService.EVENT_TYPE, query, result);
    }

}
