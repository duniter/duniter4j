package io.ucoin.ucoinj.elasticsearch.plugin;

/*
 * #%L
 * ucoinj-elasticsearch-plugin
 * %%
 * Copyright (C) 2014 - 2016 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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
