/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2024 wcm.io
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import io.wcm.wcm.commons.contenttype.ContentType;

@ExtendWith(AemContextExtension.class)
class AssetRefereneDetectorTest {

  private static final String ASSET_1 = "/content/dam/asset1.jpg";
  private static final String ASSET_2 = "/content/dam/asset2.jpg";
  private static final String ASSET_3 = "/content/dam/asset3.jpg";

  final AemContext context = new AemContext();

  @BeforeEach
  void setUp() {
    context.create().asset(ASSET_1, 10, 10, ContentType.JPEG);
    context.create().asset(ASSET_2, 10, 10, ContentType.JPEG);
    context.create().asset(ASSET_3, 10, 10, ContentType.JPEG);
  }

  @Test
  void testNoReferences() {
    Page page = context.create().page("/content/test", null,
        "prop1", "value1", "prop2", 5);
    assertTrue(getReferences(page).isEmpty());
  }

  @Test
  void testSimpleProperties() {
    Page page = context.create().page("/content/test", null,
        "prop1", "value1", "prop2", 5,
        "ref1", ASSET_1, "ref2", ASSET_2);
    assertEquals(Set.of(ASSET_1, ASSET_2), getReferences(page));
  }

  @Test
  void testArrayProperty() {
    Page page = context.create().page("/content/test", null,
        "prop1", "value1", "prop2", 5,
        "ref", ASSET_1, "refs", new String[] { ASSET_2, ASSET_3 });
    assertEquals(Set.of(ASSET_1, ASSET_2, ASSET_3), getReferences(page));
  }

  @Test
  void testNested() {
    Page page = context.create().page("/content/test", null,
        "prop1", "value1", "prop2", 5,
        "ref", ASSET_1);
    context.create().resource(page, "sub1",
        "ref", ASSET_2);
    context.create().resource(page, "sub2/sub21/sub211",
        "ref", ASSET_3);
    assertEquals(Set.of(ASSET_1, ASSET_2, ASSET_3), getReferences(page));
  }

  static Set<String> getReferences(@NotNull Page page) {
    return new AssetRefereneDetector(page).getReferencedAssets().stream()
        .map(Asset::getPath)
        .collect(Collectors.toSet());
  }

}
