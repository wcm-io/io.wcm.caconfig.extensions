/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caconfig.extensions.persistence.impl;

import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;
import static com.day.cq.commons.jcr.JcrConstants.NT_UNSTRUCTURED;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.commit;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.containsJcrContent;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.deleteChildrenNotInCollection;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.deletePageOrResource;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.ensureContainingPage;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.ensurePageIfNotContainingPage;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.getOrCreateResource;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.isItemModifiedOrNewlyAdded;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.replaceProperties;
import static io.wcm.caconfig.extensions.persistence.impl.PersistenceUtils.updatePageLastMod;

import java.util.Arrays;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.management.ConfigurationManagementSettings;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * AEM-specific persistence strategy that has higher precedence than the default strategy from Sling,
 * but lower precedence that the persistence strategy that is part of AEM since version 6.3.
 *
 * <p>
 * It supports reading configurations from cq:Page nodes in /conf, the configuration is read from the jcr:content child
 * node. Unlike the persistence strategy in AEM 6.3 this also supports writing configuration to /conf.
 * </p>
 */
@Component(service = ConfigurationPersistenceStrategy2.class)
@Designate(ocd = PagePersistenceStrategy.Config.class)
public class PagePersistenceStrategy implements ConfigurationPersistenceStrategy2 {

  @ObjectClassDefinition(name = "wcm.io Context-Aware Configuration Persistence Strategy: AEM Page",
      description = "Stores Context-Aware Configuration in AEM pages instead of simple resources.")
  @interface Config {

    @AttributeDefinition(name = "Enabled",
        description = "Enable this persistence strategy.")
    boolean enabled() default false;

    @AttributeDefinition(name = "Resource type",
        description = "Resource type for configuration pages.")
    String resourceType();

    @AttributeDefinition(name = "Collection: Mark all items updated",
        description = "When modifying a single collection item, mark all items in the collection as updated. This is a workaround for a problem publishing collections in AEMaaCS.")
    boolean collectionMarkAllItemsUpdated() default true;

    @AttributeDefinition(name = "Service Ranking",
        description = "Priority of persistence strategy (higher = higher priority).")
    int service_ranking() default 1500;

    @AttributeDefinition(name = "Config Name Deny List",
        description = "List of context-aware configuration names this persistence implementation should ignore.")
    String[] configNameDenyList() default {
        // ignore because AEM uses JCR query to fetch config values, assuming a different persistence structure
        "com.adobe.aem.wcm.site.manager.config.SiteConfig"
    };

  }

  private static final String DEFAULT_CONFIG_NODE_TYPE = NT_UNSTRUCTURED;

  @Reference
  private ConfigurationManagementSettings configurationManagementSettings;
  @Reference
  private PageManagerFactory pageManagerFactory;

  private boolean enabled;
  private String resourceType;
  private boolean collectionMarkAllItemsUpdated;
  private Set<String> configNameDenyList;

  @Activate
  void activate(Config config) {
    this.enabled = config.enabled();
    this.resourceType = config.resourceType();
    this.collectionMarkAllItemsUpdated = config.collectionMarkAllItemsUpdated();
    this.configNameDenyList = Set.copyOf(Arrays.asList(config.configNameDenyList()));
  }

  @Override
  public Resource getResource(@NotNull Resource resource) {
    if (!enabled) {
      return null;
    }
    if (containsJcrContent(resource.getPath())) {
      return resource;
    }
    return resource.getChild(JCR_CONTENT);
  }

  @Override
  public Resource getCollectionParentResource(@NotNull Resource resource) {
    if (!enabled) {
      return null;
    }
    return resource;
  }

  @Override
  public Resource getCollectionItemResource(@NotNull Resource resource) {
    return getResource(resource);
  }

  @Override
  public String getResourcePath(@NotNull String resourcePath) {
    if (!enabled) {
      return null;
    }
    if (containsJcrContent(resourcePath)) {
      return resourcePath;
    }
    return resourcePath + "/" + JCR_CONTENT;
  }

  @Override
  public String getCollectionParentResourcePath(@NotNull String resourcePath) {
    if (!enabled) {
      return null;
    }
    return resourcePath;
  }

  @Override
  public String getCollectionItemResourcePath(@NotNull String resourcePath) {
    return getResourcePath(resourcePath);
  }

  @Override
  public String getConfigName(@NotNull String configName, @Nullable String relatedConfigPath) {
    if (!enabled && isConfigNameDenied(configName)) {
      return null;
    }
    if (containsJcrContent(configName)) {
      return configName;
    }
    return configName + "/" + JCR_CONTENT;
  }

  @Override
  public String getCollectionParentConfigName(@NotNull String configName, @Nullable String relatedConfigPath) {
    if (!enabled && isConfigNameDenied(configName)) {
      return null;
    }
    return configName;
  }

  @Override
  public String getCollectionItemConfigName(@NotNull String configName, @Nullable String relatedConfigPath) {
    return getConfigName(configName, relatedConfigPath);
  }

  @Override
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public boolean persistConfiguration(@NotNull ResourceResolver resolver, @NotNull String configResourcePath, @NotNull ConfigurationPersistData data) {
    if (!enabled || isConfigResourcePathDenied(configResourcePath)) {
      return false;
    }
    String path = getResourcePath(configResourcePath);
    ensureContainingPage(resolver, path, resourceType, configurationManagementSettings);

    getOrCreateResource(resolver, path, DEFAULT_CONFIG_NODE_TYPE, data.getProperties(), configurationManagementSettings);

    PageManager pageManager = pageManagerFactory.getPageManager(resolver);
    updatePageLastMod(resolver, pageManager, path);
    commit(resolver, configResourcePath);
    return true;
  }

  @Override
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public boolean persistConfigurationCollection(@NotNull ResourceResolver resolver, @NotNull String configResourceCollectionParentPath,
      @NotNull ConfigurationCollectionPersistData data) {
    if (!enabled || isConfigResourcePathDenied(configResourceCollectionParentPath)) {
      return false;
    }

    PageManager pageManager = pageManagerFactory.getPageManager(resolver);

    // create page for collection parent
    String parentPath = getCollectionParentResourcePath(configResourceCollectionParentPath);
    ensurePageIfNotContainingPage(resolver, parentPath, resourceType, configurationManagementSettings);
    Resource configResourceParent = getOrCreateResource(resolver, parentPath, DEFAULT_CONFIG_NODE_TYPE, ValueMap.EMPTY, configurationManagementSettings);
    updatePageLastMod(resolver, pageManager, parentPath);

    // delete existing children no longer in the list
    deleteChildrenNotInCollection(configResourceParent, data);

    // create new or overwrite existing children
    for (ConfigurationPersistData item : data.getItems()) {
      String path = getCollectionItemResourcePath(parentPath + "/" + item.getCollectionItemName());
      if (collectionMarkAllItemsUpdated || isItemModifiedOrNewlyAdded(resolver, path, item, configurationManagementSettings)) {
        ensureContainingPage(resolver, path, resourceType, configurationManagementSettings);
        getOrCreateResource(resolver, path, DEFAULT_CONFIG_NODE_TYPE, item.getProperties(), configurationManagementSettings);
        updatePageLastMod(resolver, pageManager, path);
      }
    }

    // if resource collection parent properties are given replace them as well
    if (data.getProperties() != null) {
      Page parentPage = configResourceParent.adaptTo(Page.class);
      if (parentPage != null) {
        replaceProperties(parentPage.getContentResource(), data.getProperties(), configurationManagementSettings);
      }
      else {
        replaceProperties(configResourceParent, data.getProperties(), configurationManagementSettings);
      }
    }

    commit(resolver, configResourceCollectionParentPath);
    return true;
  }

  @Override
  public boolean deleteConfiguration(@NotNull ResourceResolver resolver, @NotNull String configResourcePath) {
    if (!enabled || isConfigResourcePathDenied(configResourcePath)) {
      return false;
    }
    Resource configResource = resolver.getResource(configResourcePath);
    if (configResource != null) {
      deletePageOrResource(configResource);
    }
    PageManager pageManager = pageManagerFactory.getPageManager(resolver);
    updatePageLastMod(resolver, pageManager, configResourcePath);
    commit(resolver, configResourcePath);
    return true;
  }

  private boolean isConfigNameDenied(@NotNull String configName) {
    return configNameDenyList.contains(configName);
  }

  private boolean isConfigResourcePathDenied(@NotNull String configResourcePath) {
    String resourceName = ResourceUtil.getName(configResourcePath);
    return isConfigNameDenied(resourceName);
  }

}
