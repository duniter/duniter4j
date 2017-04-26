package org.duniter.elasticsearch.script;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.script.AbstractFloatSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import java.util.List;
import java.util.Map;

public class BlockchainTxCountScriptFactory implements NativeScriptFactory {

    @Override
    public ExecutableScript newScript(@Nullable Map<String, Object> params) {
        return new BlockchainTxCountScript();
    }

    @Override
    public boolean needsScores() {
        return false;
    }

    public class BlockchainTxCountScript extends AbstractFloatSearchScript {

        @Override
        public float runAsFloat() {
            Object a = source().get("transactions");
            return a != null ? ((List)a).size() : 0;
        }
    }
}