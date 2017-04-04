package org.duniter.elasticsearch.beans;

import org.duniter.core.beans.Bean;
import org.duniter.core.beans.BeanCreationException;
import org.duniter.core.beans.BeanFactory;
import org.elasticsearch.common.inject.Injector;

/**
 * Created by blavenie on 31/03/17.
 */
public class ESBeanFactory extends BeanFactory {

    private Injector injector = null;

    public void setInjector(Injector injector) {
        this.injector = injector;
    }

    @Override
    protected <S extends Bean> void initBean(S bean) {
        super.initBean(bean);
        if (injector != null) {
            injector.injectMembers(bean);
        }
    }

    @Override
    protected <S extends Bean> S newBean(Class<S> clazz) {
        try {
            return super.newBean(clazz);
        }
        catch(BeanCreationException e) {
            // try using injector, if exists
            if (injector != null) {
                return injector.getBinding(clazz).getProvider().get();
            }
            throw e;
        }
    }
}
