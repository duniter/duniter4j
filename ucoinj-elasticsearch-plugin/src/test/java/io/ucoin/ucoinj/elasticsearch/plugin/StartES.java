package io.ucoin.ucoinj.elasticsearch.plugin;

import com.google.common.collect.Lists;
import org.elasticsearch.bootstrap.Elasticsearch;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by blavenie on 02/02/16.
 */
public class StartES {

    public StartES() {

    }

    public static void main(String args[]) {
        List<String> argList = Lists.newArrayList();
        if (args != null && args.length > 0) {
            argList.addAll(Arrays.asList(args));
        }

        // path.data
        String pathData = null;
        if (argList.size() == 1) {
            pathData = argList.get(0);
            argList.remove(pathData);
        }
        else {
            pathData = System.getProperty("tmp.dir") + File.separator + "elasticsearch-plugin-unit-test";
        }

        System.setProperty("es.path.home", "src/test/es-home");
        System.setProperty("es.path.data", pathData + File.separator + "data");
        System.setProperty("es.http.enable", "true");

        argList.add("start");

        Elasticsearch.main(argList.toArray(new String[argList.size()]));

    }
}
