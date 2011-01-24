/*
 * Milyn - Copyright (C) 2006 - 2011
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (version 2.1) as published by the Free Software
 * Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details:
 * http://www.gnu.org/licenses/lgpl.txt
 */

package org.milyn.javabean.binding.model;

import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.cdr.SmooksResourceConfiguration;
import org.milyn.cdr.SmooksResourceConfigurationList;
import org.milyn.javabean.BeanInstanceCreator;
import org.milyn.javabean.BeanInstancePopulator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class Model {

    /**
     * Model base beans.
     * <p/>
     * A Smooks configuration can have multiple <jb:baseBeans> that can be wired together
     * in all sorts of ways to create models.  This is a Map of these baseBeans.  These
     * baseBeans are used (cloned) to create all possible models (with baseBeans all wired together).
     */
    private Map<String, Bean> baseBeans = new LinkedHashMap<String, Bean>();
    /**
     * Models.
     * <p/>
     * Should contain clones of the same baseBeans as in the baseBeans property (above), but
     * with their full graphs expanded i.e. all the bean wirings resolved and wired into
     * parent baseBeans etc.
     */
    private Map<String, Bean> models = new LinkedHashMap<String, Bean>();

    public Model(SmooksResourceConfigurationList userConfigList) throws SmooksConfigurationException {
        createBaseBeanMap(userConfigList);
        createExpandedModels();
    }

    public Bean getModel(String beanId) {
        return models.get(beanId);
    }

    public Bean getModel(Class<?> beanType) {
        for(Bean model : models.values()) {
            if(model.getCreator().getBeanRuntimeInfo().getPopulateType() == beanType) {
                return model;
            }
        }
        return null;
    }

    public Map<String, Bean> getModels() {
        return models;
    }

    private void createBaseBeanMap(SmooksResourceConfigurationList userConfigList) {
        for(int i = 0; i < userConfigList.size(); i++) {
            SmooksResourceConfiguration config = userConfigList.get(i);
            Object javaResource = config.getJavaResourceObject();

            if(javaResource instanceof BeanInstanceCreator) {
                BeanInstanceCreator beanCreator = (BeanInstanceCreator) javaResource;
                Bean bean = new Bean(beanCreator).setCloneable(true);

                baseBeans.put(bean.getBeanId(), bean);
            } else if(javaResource instanceof BeanInstancePopulator) {
                BeanInstancePopulator beanPopulator = (BeanInstancePopulator) javaResource;
                Bean bean = baseBeans.get(beanPopulator.getBeanId());

                if(bean == null) {
                    throw new SmooksConfigurationException("Unexpected binding configuration exception.  Unknown parent beanId '' for binding configuration.");
                }

                if(beanPopulator.isBeanWiring()) {
                    bean.getBindings().add(new WiredBinding(beanPopulator));
                } else {
                    bean.getBindings().add(new DataBinding(beanPopulator));
                }
            }
        }
    }

    private void createExpandedModels() {
        for(Bean bean : baseBeans.values()) {
            models.put(bean.getBeanId(), bean.clone(baseBeans, null));
        }
    }
}