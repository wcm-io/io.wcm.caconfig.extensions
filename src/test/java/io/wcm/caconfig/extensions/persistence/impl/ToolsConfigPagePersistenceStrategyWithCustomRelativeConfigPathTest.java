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

import static io.wcm.caconfig.extensions.persistence.testcontext.PersistenceTestUtils.writeConfiguration;
import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;
import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Objects;

import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;

import io.wcm.caconfig.extensions.contextpath.impl.AbsoluteParentContextPathStrategy;
import io.wcm.caconfig.extensions.persistence.example.SimpleConfig;
import io.wcm.sling.commons.adapter.AdaptTo;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class ToolsConfigPagePersistenceStrategyWithCustomRelativeConfigPathTest {

  final AemContext context = new AemContextBuilder()
          .plugin(CACONFIG)
          .build();

  private Page contentPage;

  @BeforeEach
  void setUp() {
    context.registerInjectActivateService(AbsoluteParentContextPathStrategy.class,
            "levels", new int[] { 1, 3 },
            "contextPathRegex", "^/content(/.+)$",
            "configPathPatterns", new String[] { "/conf$1", "/content$1/tools/other-config/jcr:content" });
    context.registerInjectActivateService(ToolsConfigPagePersistenceStrategy.class,
            "enabled", true,
            "configPageTemplate", "/apps/app1/templates/configEditor",
            "structurePageTemplate", "/apps/app1/templates/structurePage",
            "relativeConfigPath", "/tools/other-config/jcr:content");

    context.create().resource("/apps/app1/templates/configEditor/jcr:content",
            PROPERTY_RESOURCE_TYPE, "app1/components/page/configEditor");

    context.create().page("/content/region1");
    context.create().page("/content/region1/site1");
    context.create().page("/content/region1/site1/en");
    contentPage = context.create().page("/content/region1/site1/en/page1");
  }

  @Test
  void testSimpleConfig() {
    // write config
    writeConfiguration(context, contentPage.getPath(), SimpleConfig.class.getName(),
            "stringParam", "value1",
            "intParam", 123);

    // assert storage in page in /content/*/tools/config
    Page configPage = context.pageManager().getPage("/content/region1/site1/en/tools/other-config");
    assertThat(configPage.getContentResource(), ResourceMatchers.props(
            NameConstants.PN_TEMPLATE, "/apps/app1/templates/configEditor",
            NameConstants.PN_TITLE, "other-config",
            PROPERTY_RESOURCE_TYPE, "app1/components/page/configEditor"));
    assertThat(configPage.getContentResource("sling:configs/" + SimpleConfig.class.getName()), ResourceMatchers.props(
            "stringParam", "value1",
            "intParam", 123));

    Page toolsPage = context.pageManager().getPage("/content/region1/site1/en/tools");
    assertThat(toolsPage.getContentResource(), ResourceMatchers.props(
            NameConstants.PN_TEMPLATE, "/apps/app1/templates/structurePage",
            NameConstants.PN_TITLE, "tools",
            PROPERTY_RESOURCE_TYPE, null));

    // read config
    SimpleConfig config = AdaptTo.notNull(contentPage.getContentResource(), ConfigurationBuilder.class).as(SimpleConfig.class);
    assertEquals("value1", config.stringParam());
    assertEquals(123, config.intParam());

    // delete
    ConfigurationManager configManager = context.getService(ConfigurationManager.class);
    Objects.requireNonNull(configManager).deleteConfiguration(contentPage.getContentResource(), SimpleConfig.class.getName());
    config = AdaptTo.notNull(contentPage.getContentResource(), ConfigurationBuilder.class).as(SimpleConfig.class);
    assertNull(config.stringParam());
    assertEquals(5, config.intParam());
  }

}
