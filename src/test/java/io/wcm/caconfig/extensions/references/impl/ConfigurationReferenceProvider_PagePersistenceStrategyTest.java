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
package io.wcm.caconfig.extensions.references.impl;

import static io.wcm.caconfig.extensions.references.impl.TestUtils.applyConfig;
import static io.wcm.caconfig.extensions.references.impl.TestUtils.assertReferences;
import static io.wcm.caconfig.extensions.references.impl.TestUtils.registerConfigurations;
import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;

import io.wcm.caconfig.extensions.persistence.impl.PagePersistenceStrategy;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextCallback;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import io.wcm.wcm.commons.contenttype.ContentType;

/**
 * Test the {@link ConfigurationReferenceProvider} with the {@link PagePersistenceStrategy}.
 */
@ExtendWith(AemContextExtension.class)
class ConfigurationReferenceProvider_PagePersistenceStrategyTest {

  private final AemContext context = new AemContextBuilder()
      .beforeSetUp(new AemContextCallback() {
        @Override
        public void execute(@NotNull AemContext ctx) {
          // also find sling:configRef props in cq:Page/jcr:content nodes
          MockOsgi.setConfigForPid(ctx.bundleContext(), "org.apache.sling.caconfig.resource.impl.def.DefaultContextPathStrategy",
              "configRefResourceNames", new String[] { "jcr:content", "." });
        }
      })
      .plugin(CACONFIG)
      .build();

  private static final ValueMap CONFIGURATION_A = new ValueMapDecorator(Map.of("key", "foo"));
  private static final ValueMap CONFIGURATION_B = new ValueMapDecorator(Map.of("key", "bar",
      "assetReference1", "/content/dam/test.jpg",
      "assetReference2", "/content/dam/test.jpg"));
  private static final Calendar TIMESTAMP = Calendar.getInstance();

  private Resource site1PageResource;
  private Resource site2PageResource;

  @BeforeEach
  void setup() {
    // enable AEM page persistence strategy
    context.registerInjectActivateService(PagePersistenceStrategy.class,
        "enabled", true);

    context.create().asset("/content/dam/test.jpg", 10, 10, ContentType.JPEG);

    context.create().resource("/conf");

    context.create().page("/content/region1", null, Map.of("sling:configRef", "/conf/region1"));
    context.create().page("/content/region1/site1", null, Map.of("sling:configRef", "/conf/region1/site1"));
    context.create().page("/content/region1/site2", null, Map.of("sling:configRef", "/conf/region1/site2"));
    Page region1Page = context.create().page("/content/region1/page");
    Page site1Page = context.create().page("/content/region1/site1/page");
    Page site2Page = context.create().page("/content/region1/site2/page");

    site1PageResource = site1Page.adaptTo(Resource.class);
    site2PageResource = site2Page.adaptTo(Resource.class);

    registerConfigurations(context, ConfigurationA.class, ConfigurationB.class);

    // store fallback config
    context.create().page("/conf/global/sling:configs/configB", null,
        Map.<String, Object>of("key", "fallback", NameConstants.PN_PAGE_LAST_MOD, TIMESTAMP));

    applyConfig(context, region1Page, "configA", CONFIGURATION_A); // 1 config for region
    applyConfig(context, site1Page, "configA", CONFIGURATION_A); // 1 config on page1 (+1 from region +1 from fallback)
    applyConfig(context, site2Page, "configA", CONFIGURATION_A); // 2 configs on page2 (+1 from region +1 from fallback)
    applyConfig(context, site2Page, "configB", CONFIGURATION_B);
  }

  @Test
  void testReferencesOfPage1() {
    ReferenceProvider referenceProvider = context.registerInjectActivateService(ConfigurationReferenceProvider.class);
    List<Reference> references = referenceProvider.findReferences(site1PageResource);
    assertReferences(references,
        "/conf/region1/site1/sling:configs/configA",
        "/conf/region1/sling:configs/configA",
        "/conf/global/sling:configs/configB");
  }

  @Test
  void testReferencesOfPage2() {
    ReferenceProvider referenceProvider = context.registerInjectActivateService(ConfigurationReferenceProvider.class);
    List<Reference> references = referenceProvider.findReferences(site2PageResource);
    assertReferences(references,
        "/conf/region1/site2/sling:configs/configA",
        "/conf/region1/sling:configs/configA",
        "/conf/region1/site2/sling:configs/configB",
        "/conf/global/sling:configs/configB");
  }

  @Test
  void testReferencesProperties() {
    ReferenceProvider referenceProvider = context.registerInjectActivateService(ConfigurationReferenceProvider.class);
    List<Reference> references = referenceProvider.findReferences(site1PageResource);

    // validate props of fallback config reference
    Reference ref = references.get(references.size() - 1);
    assertEquals("/conf/global/sling:configs/configB", ref.getResource().getPath());
    assertEquals(ConfigurationReferenceProvider.REFERENCE_TYPE, ref.getType());
    assertEquals("conf / global / Configuration B", ref.getName());
    assertEquals(TIMESTAMP.getTimeInMillis(), ref.getLastModified());
  }

  @Test
  void testReferencesOfPage2_assetReferences() {
    ReferenceProvider referenceProvider = context.registerInjectActivateService(ConfigurationReferenceProvider.class,
        "assetReferences", true);
    List<Reference> references = referenceProvider.findReferences(site2PageResource);

    assertReferences(references,
        "/conf/region1/site2/sling:configs/configA",
        "/conf/region1/sling:configs/configA",
        "/conf/region1/site2/sling:configs/configB",
        "/conf/global/sling:configs/configB",
        "/content/dam/test.jpg");
  }

  @Test
  void testDisabled() {
    ReferenceProvider referenceProvider = context.registerInjectActivateService(ConfigurationReferenceProvider.class,
        "enabled", false);
    List<Reference> references = referenceProvider.findReferences(site1PageResource);
    assertTrue(references.isEmpty(), "no references");
  }

}
