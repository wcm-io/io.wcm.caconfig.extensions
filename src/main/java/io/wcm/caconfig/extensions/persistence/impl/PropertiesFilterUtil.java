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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.caconfig.management.ConfigurationManagementSettings;

/**
 * Filter internal properties from ConfigManager API output.
 */
final class PropertiesFilterUtil {

  private PropertiesFilterUtil() {
    // static methods only
  }

  public static void removeIgnoredProperties(Set<String> propertyNames, ConfigurationManagementSettings settings) {
    Set<String> ignoredProperties = settings.getIgnoredPropertyNames(propertyNames);
    propertyNames.removeAll(ignoredProperties);
  }

  public static void removeIgnoredProperties(Map<String, Object> properties, ConfigurationManagementSettings settings) {
    Set<String> propertyNames = new HashSet<>(properties.keySet());
    removeIgnoredProperties(propertyNames, settings);
    properties.keySet().retainAll(propertyNames);
  }

}
