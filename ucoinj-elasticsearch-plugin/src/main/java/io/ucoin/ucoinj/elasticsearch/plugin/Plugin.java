package io.ucoin.ucoinj.elasticsearch.plugin;

import com.google.common.collect.Lists;
import io.ucoin.ucoinj.elasticsearch.action.RestModule;
import io.ucoin.ucoinj.elasticsearch.security.SecurityModule;
import org.elasticsearch.common.inject.Module;

import java.util.Collection;

public class Plugin extends org.elasticsearch.plugins.Plugin {

    @Override
    public String name() {
        return "ucoinj-elasticsearch";
    }

    @Override
    public String description() {
        return "uCoinj ElasticSearch Plugin";
    }

    @Override
    public Collection<Module> nodeModules() {
        Collection<Module> modules = Lists.newArrayList();
        modules.add(new SecurityModule());
        modules.add(new RestModule());
        return modules;
    }
}