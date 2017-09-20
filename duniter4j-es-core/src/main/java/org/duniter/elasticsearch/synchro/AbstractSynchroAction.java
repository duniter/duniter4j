package org.duniter.elasticsearch.synchro;

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
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.PrimitiveIterators;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.exception.DuniterElasticsearchException;
import org.duniter.elasticsearch.exception.InvalidFormatException;
import org.duniter.elasticsearch.exception.InvalidSignatureException;
import org.duniter.elasticsearch.model.SearchResponse;
import org.duniter.elasticsearch.model.SearchScrollResponse;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.service.changes.ChangeEvent;
import org.duniter.elasticsearch.service.changes.ChangeEvents;
import org.duniter.elasticsearch.service.changes.ChangeSource;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.util.bytes.BytesJsonNode;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.io.IOException;
import java.util.*;

public abstract class AbstractSynchroAction extends AbstractService implements SynchroAction {

    private static final String SCROLL_PARAM_VALUE = "1m";

    public interface SourceConsumer {
        void accept(String id, JsonNode source) throws Exception;
    }

    private String fromIndex;
    private String fromType;
    private String toIndex;
    private String toType;
    private String issuerFieldName = Record.PROPERTY_ISSUER;
    private String versionFieldName = Record.PROPERTY_TIME;
    private ChangeSource changeSource;

    private HttpService httpService;

    private boolean enableUpdate = false;
    private boolean enableSignatureValidation = true;
    private List<SourceConsumer> insertionListeners;
    private List<SourceConsumer> updateListeners;
    private List<SourceConsumer> validationListeners;

    private boolean trace = false;

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
        super("duniter.p2p." + toIndex, client, pluginSettings, cryptoService);
        this.fromIndex = fromIndex;
        this.fromType = fromType;
        this.toIndex = toIndex;
        this.toType = toType;
        this.changeSource = new ChangeSource()
                .addIndex(fromIndex)
                .addType(fromType);
        this.trace = logger.isTraceEnabled();
        threadPool.scheduleOnStarted(() -> httpService = ServiceLocator.instance().getHttpService());
    }


    @Override
    public EndpointApi getEndPointApi() {
        return EndpointApi.ES_USER_API;
    }

    @Override
    public ChangeSource getChangeSource() {
        return changeSource;
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

    @Override
    public void handleChange(Peer peer, ChangeEvent changeEvent) {

        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(changeEvent);
        Preconditions.checkNotNull(changeEvent.getOperation());

        String id = changeEvent.getId();
        String logPrefix = String.format("[%s] [%s] [%s/%s/%s] [WS]", peer.getCurrency(), peer, toIndex, toType, id);

        boolean skip = changeEvent.getOperation() == ChangeEvent.Operation.DELETE ||
                !enableUpdate && changeEvent.getOperation() == ChangeEvent.Operation.INDEX ||
                !changeEvent.hasSource();
        if (skip) {
            if (trace) {
                logger.trace(String.format("%s Ignoring change event of type [%s]", logPrefix, changeEvent.getOperation().name()));
            }
            return;
        }
        try {
            if (trace) {
                logger.trace(String.format("%s Processing new change event...", logPrefix));
            }

            JsonNode source = ChangeEvents.readTree(changeEvent.getSource());

            // Save doc
            save(changeEvent.getId(), source, logPrefix);
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

    public void addInsertionListener(SourceConsumer listener) {
        if (insertionListeners == null) {
            insertionListeners = Lists.newArrayList();
        }
        insertionListeners.add(listener);
    }

    public void addUpdateListener(SourceConsumer listener) {
        if (updateListeners == null) {
            updateListeners = Lists.newArrayList();
        }
        updateListeners.add(listener);
    }

    public void addValidationListener(SourceConsumer listener) {
        if (validationListeners == null) {
            validationListeners = Lists.newArrayList();
        }
        validationListeners.add(listener);
    }

    /* -- protected methods -- */

    protected void notifyInsertion(final String id, final JsonNode source) throws Exception {
        if (CollectionUtils.isNotEmpty(insertionListeners)) {
            for (SourceConsumer listener: insertionListeners) {
                listener.accept(id, source);
            }
        }
    }

    protected void notifyUpdate(final String id, final JsonNode source) throws Exception {
        if (CollectionUtils.isNotEmpty(updateListeners)) {
            for (SourceConsumer listener: updateListeners) {
                listener.accept(id, source);
            }
        }
    }

    protected void notifyValidation(final String id,
                                    final JsonNode source,
                                    final Iterator<Long> invalidSignatureHits,
                                    final String logPrefix) throws Exception {
        if (enableSignatureValidation) {
            try {
                readAndVerifyIssuerSignature(source, issuerFieldName);
            } catch (InvalidSignatureException e) {
                // FIXME: some user/profile document failed ! - see issue #11
                // Il semble que le format JSON ne soit pas le même que celui qui a été signé
                invalidSignatureHits.next();
                if (trace) {
                    logger.warn(String.format("%s %s.\n%s", logPrefix, e.getMessage(), source.toString()));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(validationListeners)) {
            for (SourceConsumer listener : validationListeners) {
                listener.accept(id, source);
            }
        }
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

            if (trace) {
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
            throw new TechnicalException(String.format("[%s] [%s] [%s/%s] Unable to scroll request: %s", peer.getCurrency(), peer, fromIndex, fromType, e.getMessage()), e);
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
                counter += fetchAndSave(peer, response, objectMapper, result);
                stop = counter >= total;
            }
        }
    }

    private long fetchAndSave(final Peer peer,
                              SearchScrollResponse response,
                              final ObjectMapper objectMapper,
                              SynchroResult result) {


        long counter = 0;


        PrimitiveIterators.OfLong insertHits = PrimitiveIterators.newLongSequence();
        PrimitiveIterators.OfLong updateHits = PrimitiveIterators.newLongSequence();
        PrimitiveIterators.OfLong invalidSignatureHits = PrimitiveIterators.newLongSequence();

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        bulkRequest.setRefresh(true);

        for (Iterator<SearchResponse.SearchHit> hits = response.getHits(); hits.hasNext();){
            SearchResponse.SearchHit hit = hits.next();
            String id = hit.getId();
            JsonNode source = hit.getSource();

            String logPrefix = String.format("[%s] [%s] [%s/%s/%s]", peer.getCurrency(), peer, toIndex, toType, id);

            if (source == null) {
                logger.error(String.format("%s No source found. Skipping.", logPrefix));
            }
            else {
                counter++;

                // Save (create or update)
                save(id, source,
                     objectMapper,
                     bulkRequest,
                     insertHits,
                     updateHits,
                     invalidSignatureHits,
                     logPrefix);
            }
        }

        if (bulkRequest.numberOfActions() > 0) {

            // Flush the bulk if not empty
            BulkResponse bulkResponse = bulkRequest.get();
            Set<String> missingDocIds = new LinkedHashSet<>();

            // If failures, continue but saveInBulk missing blocks
            if (bulkResponse.hasFailures()) {
                // process failures by iterating through each bulk response item
                for (BulkItemResponse itemResponse : bulkResponse) {
                    boolean skip = !itemResponse.isFailed()
                            || missingDocIds.contains(itemResponse.getId());
                    if (!skip) {
                        if (trace) {
                            logger.debug(String.format("[%s] [%s] [%s/%s] could not process _id=%s: %s. Skipping.", peer.getCurrency(), peer, toIndex, toType, itemResponse.getId(), itemResponse.getFailureMessage()));
                        }
                        missingDocIds.add(itemResponse.getId());
                    }
                }
            }
        }

        // update result stats
        result.addInserts(toIndex, toType, insertHits.current());
        result.addUpdates(toIndex, toType, updateHits.current());
        result.addInvalidSignatures(toIndex, toType, invalidSignatureHits.current());

        return counter;
    }

    protected void save(String id, JsonNode source, String logPrefix) {
        Iterator<Long> nullSeq = PrimitiveIterators.nullLongSequence();
        save(id, source, getObjectMapper(), null, nullSeq, nullSeq, nullSeq, logPrefix);
    }

    protected void save(final String id,
                        final JsonNode source,
                        final ObjectMapper objectMapper,
                        final BulkRequestBuilder bulkRequest,
                        final Iterator<Long> insertHits,
                        final Iterator<Long> updateHits,
                        final Iterator<Long> invalidSignatureHits,
                        final String logPrefix) {

        try {
            String issuer = source.get(issuerFieldName).asText();
            if (StringUtils.isBlank(issuer)) {
                throw new InvalidFormatException(String.format("Invalid format: missing or null %s field.", issuerFieldName));
            }
            long version = source.get(versionFieldName).asLong(-1);
            if (version == -1) {
                throw new InvalidFormatException(String.format("Invalid format: missing or null %s field.", versionFieldName));
            }

            Map<String, Object> existingFields = client.getFieldsById(toIndex, toType, id, versionFieldName, issuerFieldName);
            boolean exists = existingFields != null;

            // Insert (new doc)
            if (!exists) {

                if (trace) {
                    logger.trace(String.format("%s insert found\n%s", logPrefix, source.toString()));
                }

                // Validate doc
                notifyValidation(id, source, invalidSignatureHits, logPrefix);

                // Execute insertion
                IndexRequestBuilder request = client.prepareIndex(toIndex, toType, id)
                        .setSource(objectMapper.writeValueAsBytes(source));
                if (bulkRequest != null) {
                    bulkRequest.add(request);
                }
                else {
                    client.safeExecuteRequest(request, false);
                }

                // Notify insert listeners
                notifyInsertion(id, source);

                insertHits.next();
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
                    if (trace) {
                        logger.trace(String.format("%s found update\n%s", logPrefix, source.toString()));
                    }

                    // Validate source
                    notifyValidation(id, source, invalidSignatureHits, logPrefix);

                    // Execute update
                    UpdateRequestBuilder request = client.prepareUpdate(toIndex, toType, id)
                            .setRefresh(true)
                            .setSource(objectMapper.writeValueAsBytes(source));
                    if (bulkRequest != null) {
                        bulkRequest.add(request);
                    }
                    else {
                        client.safeExecuteRequest(request, false);
                    }

                    // Notify insert listeners
                    notifyUpdate(id, source);

                    updateHits.next();
                }
            }

        } catch (DuniterElasticsearchException e) {
            // Skipping document: log, then continue
            if (logger.isDebugEnabled()) {
                logger.warn(String.format("%s %s. Skipping.\n%s", logPrefix, e.getMessage(), source.toString()));
            } else {
                logger.warn(String.format("%s %s. Skipping.", logPrefix, e.getMessage()));
            }
        } catch (Exception e) {
            // Skipping document: log, then continue
            logger.error(String.format("%s %s. Skipping.", logPrefix, e.getMessage()), e);
        }
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

    protected void setEnableSignatureValidation(boolean enableSignatureValidation) {
        this.enableSignatureValidation = enableSignatureValidation;
    }


}
