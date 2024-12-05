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
package io.wcm.caconfig.extensions.persistence.impl;

import static io.wcm.caconfig.extensions.persistence.impl.PropertiesFilterUtil.removeIgnoredProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.caconfig.management.ConfigurationManagementSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertiesFilterUtilTest {

  @Mock
  private ConfigurationManagementSettings settings;

  @BeforeEach
  void setUp() {
    when(settings.getIgnoredPropertyNames(any())).thenReturn(Set.of("prop1", "prop3"));
  }

  @Test
  void testRemoveIgnoredPropertiesSet() {
    Set<String> names = new HashSet<>(Set.of("prop1", "prop2", "prop3", "prop4"));
    removeIgnoredProperties(names, settings);
    assertEquals(Set.of("prop2", "prop4"), names);
  }

  @Test
  void testRemoveIgnoredPropertiesMap() {
    Map<String, Object> properties = new HashMap<>(Map.of("prop1", 1, "prop2", 2, "prop3", 3, "prop4", 4));
    removeIgnoredProperties(properties, settings);
    assertEquals(Map.of("prop2", 2, "prop4", 4), properties);
  }

}
