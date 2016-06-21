package org.duniter.elasticsearch.node;

import org.duniter.core.client.model.local.Peer;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.service.BlockchainService;
import org.duniter.elasticsearch.service.MarketService;
import org.duniter.elasticsearch.service.RegistryService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;

/**
 * Created by blavenie on 17/06/16.
 */
public class DuniterNode extends AbstractLifecycleComponent<DuniterNode> {

    private final PluginSettings pluginSettings;
    private final ThreadPool threadPool;
    private final Injector injector;

    @Inject
    public DuniterNode(Settings settings, PluginSettings pluginSettings, ThreadPool threadPool, final Injector injector) {
        super(settings);
        this.pluginSettings = pluginSettings;
        this.threadPool = threadPool;
        this.injector = injector;

    }

    @Override
    protected void doStart() {
        threadPool.scheduleOnStarted(() -> {
            createIndices();
        });
    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() {

    }

    protected void createIndices() {

        boolean reloadIndices = pluginSettings.reloadIndices();
        Peer peer = pluginSettings.checkAndGetPeer();
        if (reloadIndices) {
            if (logger.isInfoEnabled()) {
                logger.info("Reloading all Duniter indices...");
            }
            injector.getInstance(RegistryService.class)
                    .deleteIndex()
                    .createIndexIfNotExists()
                    .fillRecordCategories()
                    .indexCurrencyFromPeer(peer);
            injector.getInstance(MarketService.class)
                    .deleteIndex()
                    .createIndexIfNotExists()
                    .fillRecordCategories();

            injector.getInstance(BlockchainService.class)
                    .indexLastBlocks(peer);
        }
        else {
            if (logger.isInfoEnabled()) {
                logger.info("Checking Duniter indices...");
            }

            injector.getInstance(RegistryService.class).createIndexIfNotExists();

            injector.getInstance(MarketService.class).createIndexIfNotExists();
        }

        if (logger.isInfoEnabled()) {
            logger.info("Duniter indices created.");
        }
    }
}
