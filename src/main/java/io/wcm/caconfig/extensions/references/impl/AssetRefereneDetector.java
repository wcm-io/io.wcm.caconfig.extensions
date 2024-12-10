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

import static com.day.cq.dam.api.DamConstants.MOUNTPOINT_ASSETS;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;

/**
 * Recursively scans all string and string array properties in all resources of the given configuration page
 * to check for asset references.
 */
class AssetRefereneDetector {

  private final Page configPage;
  private final Resource configResource;
  private final ResourceResolver resourceResolver;
  private final List<Asset> assets = new ArrayList<>();

  private static final Pattern ASSET_PATH = Pattern.compile("^" + MOUNTPOINT_ASSETS + "/.*$");
  private static final Logger log = LoggerFactory.getLogger(AssetRefereneDetector.class);

  /**
   * @param configPage Configuration page (must have a content resource).
   */
  AssetRefereneDetector(@NotNull Page configPage) {
    this.configPage = configPage;
    this.configResource = configPage.getContentResource();
    this.resourceResolver = configResource.getResourceResolver();
  }

  /**
   * @return List of all assets referenced in the configuration page.
   */
  List<Asset> getReferencedAssets() {
    assets.clear();
    findAssetReferencesRecursively(configResource);
    return assets;
  }

  /**
   * Recurse through all child resources of the given resource.
   * @param resource Resource
   */
  private void findAssetReferencesRecursively(@NotNull Resource resource) {
    findAssetReferences(resource);
    resource.getChildren().forEach(this::findAssetReferencesRecursively);
  }

  /**
   * Find asset references in all properties of the given resource.
   * @param resource Resource
   */
  private void findAssetReferences(@NotNull Resource resource) {
    ValueMap props = resource.getValueMap();
    assets.addAll(props.values().stream()
        .flatMap(this::getAssetsIfAssetReference)
        .collect(Collectors.toList()));
  }

  /**
   * Checks if the value is string which might be asset reference, or an array containing a string asset reference.
   * @param value Value
   * @return Found referenced assets
   */
  private Stream<Asset> getAssetsIfAssetReference(@Nullable Object value) {
    List<Asset> result = new ArrayList<>();
    if (value instanceof String) {
      getAssetIfAssetReference((String)value).ifPresent(result::add);
    }
    else if (value != null && value.getClass().isArray()) {
      int length = Array.getLength(value);
      for (int i = 0; i < length; i++) {
        Object itemValue = Array.get(value, i);
        if (itemValue instanceof String) {
          getAssetIfAssetReference((String)itemValue).ifPresent(result::add);
        }
      }
    }
    return result.stream();
  }

  /**
   * Checks if the given string points to an asset.
   * @param value String value
   * @return Asset if string is a valid asset reference.
   */
  private Optional<Asset> getAssetIfAssetReference(@NotNull String value) {
    if (isAssetReference(value)) {
      Resource resource = resourceResolver.getResource(value);
      if (resource != null) {
        Asset asset = resource.adaptTo(Asset.class);
        if (asset != null) {
          log.trace("Found asset reference {} for resource {}", configPage.getPath(), resource.getPath());
          return Optional.of(asset);
        }
      }
    }
    return Optional.empty();
  }

  static boolean isAssetReference(@NotNull String value) {
    return ASSET_PATH.matcher(value).matches();
  }

}
