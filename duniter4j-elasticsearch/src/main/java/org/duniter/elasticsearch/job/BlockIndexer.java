package org.duniter.elasticsearch.job;

import org.duniter.core.client.model.bma.BlockchainParameters;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.service.blockchain.BlockBlockchainService;
import org.duniter.elasticsearch.service.market.CategoryMarketService;
import org.duniter.elasticsearch.service.market.RecordMarketService;
import org.duniter.elasticsearch.service.registry.CategoryRegistryService;
import org.duniter.elasticsearch.service.registry.CitiesRegistryService;
import org.duniter.elasticsearch.service.registry.CurrencyRegistryService;
import org.duniter.elasticsearch.service.registry.RecordRegistryService;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;

/**
 * Created by eis on 17/06/16.
 */
public class BlockIndexer implements LifecycleComponent<BlockIndexer> {
    private static final ESLogger log = ESLoggerFactory.getLogger(BlockIndexer.class.getName());

    private Lifecycle.State state;

    private ServiceLocator serviceLocator;
    private PluginSettings pluginSettings;
    private RecordMarketService recordMarketService;
    private CurrencyRegistryService currencyRegistryService;
    private BlockBlockchainService blockBlockchainService;
    private CategoryMarketService categoryMarketService;
    private RecordRegistryService recordRegistryService;
    private CategoryRegistryService categoryRegistryService;
    private CitiesRegistryService citiesRegistryService;

    @Inject
    public BlockIndexer(ServiceLocator serviceLocator,
                        PluginSettings pluginSettings,
                        RecordMarketService recordMarketService,
                        CurrencyRegistryService currencyRegistryService,
                        BlockBlockchainService blockBlockchainService,
                        CategoryMarketService categoryMarketService,
                        RecordRegistryService recordRegistryService,
                        CategoryRegistryService categoryRegistryService,
                        CitiesRegistryService citiesRegistryService
                        ) {
        this.serviceLocator = serviceLocator;
        this.pluginSettings = pluginSettings;
        this.recordMarketService = recordMarketService;
        this.currencyRegistryService = currencyRegistryService;
        this.blockBlockchainService = blockBlockchainService;
        this.categoryMarketService = categoryMarketService;
        this.recordRegistryService = recordRegistryService;
        this.categoryRegistryService = categoryRegistryService;
        this.citiesRegistryService = citiesRegistryService;
        this.state = Lifecycle.State.INITIALIZED;
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return state;
    }

    @Override
    public void addLifecycleListener(LifecycleListener var1){
        // TODO
    }

    @Override
    public void removeLifecycleListener(LifecycleListener var1){
        // TODO
    }

    @Override
    public BlockIndexer start(){
        state = Lifecycle.State.STARTED;
        if (log.isDebugEnabled()) {
            log.debug(String.format("Starting indexing blocks from node [%s:%s]...",
                    pluginSettings.getNodeBmaHost(), pluginSettings.getNodeBmaPort()));
        }

        //resetAllData();
        return this;
    }

    @Override
    public BlockIndexer stop(){
        state = Lifecycle.State.STOPPED;
        return this;
    }

    @Override
    public void close() {
        state = Lifecycle.State.STOPPED;
    }

    /* -- protected methods  -- */

    protected void setState(Lifecycle.State state) {
        this.state = state;
    }

    public void resetAllData() {
        resetAllCurrencies();
        //resetDataBlocks();
        //resetMarketRecords();
        //resetRegistry();
    }

    public void resetAllCurrencies() {
        currencyRegistryService.deleteAllCurrencies();
    }

    public void resetDataBlocks() {
        BlockchainRemoteService blockchainService = serviceLocator.getBlockchainRemoteService();
        Peer peer = checkConfigAndGetPeer(pluginSettings);

        try {
            // Get the blockchain name from node
            BlockchainParameters parameter = blockchainService.getParameters(peer);
            if (parameter == null) {
                log.error(String.format("Could not connect to node [%s:%s]",
                        pluginSettings.getNodeBmaHost(), pluginSettings.getNodeBmaPort()));
                return;
            }
            String currencyName = parameter.getCurrency();

            log.info(String.format("Reset data for index [%s]", currencyName));

            // Delete then create index on blockchain
            boolean indexExists = blockBlockchainService.existsIndex(currencyName);
            if (indexExists) {
                blockBlockchainService.deleteIndex(currencyName);
                blockBlockchainService.createIndex(currencyName);
            }


            log.info(String.format("Successfully reset data for index [%s]", currencyName));
        } catch(Exception e) {
            log.error("Error during reset data: " + e.getMessage(), e);
        }
    }

    public void resetMarketRecords() {
        try {
            // Delete then create index on records
            boolean indexExists = recordMarketService.existsIndex();
            if (indexExists) {
                recordMarketService.deleteIndex();
            }
            log.info(String.format("Successfully reset market records"));

            categoryMarketService.createIndex();
            categoryMarketService.initCategories();
            log.info(String.format("Successfully re-initialized market categories data"));

        } catch(Exception e) {
            log.error("Error during reset market records: " + e.getMessage(), e);
        }
    }

    public void resetRegistry() {
        try {
            // Delete then create index on records
            if (recordRegistryService.existsIndex()) {
                recordRegistryService.deleteIndex();
            }
            recordRegistryService.createIndex();
            log.info(String.format("Successfully reset registry records"));


            if (categoryRegistryService.existsIndex()) {
                categoryRegistryService.deleteIndex();
            }
            categoryRegistryService.createIndex();
            categoryRegistryService.initCategories();
            log.info(String.format("Successfully re-initialized registry categories"));

            if (citiesRegistryService.existsIndex()) {
                citiesRegistryService.deleteIndex();
            }
            citiesRegistryService.initCities();
            log.info(String.format("Successfully re-initialized registry cities"));

        } catch(Exception e) {
            log.error("Error during reset registry records: " + e.getMessage(), e);
        }
    }

    /* -- internal methods -- */

    protected Peer checkConfigAndGetPeer(PluginSettings pluginSettings) {
        if (StringUtils.isBlank(pluginSettings.getNodeBmaHost())) {
            log.error("ERROR: node host is required");
            System.exit(-1);
            return null;
        }
        if (pluginSettings.getNodeBmaPort() <= 0) {
            log.error("ERROR: node port is required");
            System.exit(-1);
            return null;
        }

        Peer peer = new Peer(pluginSettings.getNodeBmaHost(), pluginSettings.getNodeBmaPort());
        return peer;
    }
}
