package org.duniter.elasticsearch.i18n;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.i18n.bundle.I18nBundle;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by blavenie on 10/01/17.
 */
public class I18nInitializer extends org.nuiton.i18n.init.I18nInitializer{
    private static final Log log = LogFactory.getLog(UserI18nInitializer.class);
    protected final File userDirectory;

    private String[] bundleNames;
    private String i18nPath;
    private List<UserI18nInitializer> delegates;


    public I18nInitializer(File userDirectory, String[] bundleNames) throws NullPointerException {
        this((String)null, userDirectory, bundleNames);
    }

    public I18nInitializer(String i18nPath, File userDirectory, String[] bundleNames) throws NullPointerException {
        super();

        this.i18nPath = i18nPath;
        this.bundleNames = bundleNames;
        this.userDirectory = userDirectory;
        this.delegates = createDelegates(userDirectory, bundleNames);

        if(userDirectory == null) {
            throw new NullPointerException("parameter \'userDirectory\' can not be null");
        }
    }

    public File getUserDirectory() {
        return this.userDirectory;
    }


    @Override
    public I18nBundle[] resolvBundles() throws Exception {

        List<I18nBundle> result = new ArrayList<>();
        for(DefaultI18nInitializer delegate: delegates) {
            I18nBundle[] bundles = delegate.resolvBundles();
            for(I18nBundle bundle: bundles) {
                result.add(bundle);
            }
        }

        return result.toArray(new I18nBundle[result.size()]);
    }

    /* -- private methods -- */

    private List<UserI18nInitializer> createDelegates(File userDirectory, String[] bundleNames) {
        List<UserI18nInitializer> result = new ArrayList<>();
        for(String bundleName: bundleNames) {
            UserI18nInitializer delegate = new UserI18nInitializer(userDirectory, new DefaultI18nInitializer(bundleName));
            result.add(delegate);
        }
        return result;
    }

}
