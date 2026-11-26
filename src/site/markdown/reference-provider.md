## Reference Provider

### Overview

wcm.io Context-Aware Configuration Extensions provide an implementation of the AEM [ReferenceProvider][aem-referenceprovider] service interface. For each content page it provides references to the related configuration pages (e.g. stored below /conf).

If you activate an AEM page you will be asked if you want to publish references as well. If there are unpublished configuration changes in configuration pages related to the current configuration context (or higher hierarchy levels) they are listed as well. 

This reference provider only works if the configurations are stored as AEM pages, e.g. using the "AEM Page" persistence strategy, see [Persistence Strategies][persistence-strategies].


### Configuration

The reference provider can be disabled by configuration, but its enabled by default.

To disable it configure:

```
  io.wcm.caconfig.extensions.references.impl.ConfigurationReferenceProvider
    enabled=B"false"
```

Optionally, you can detect also asset references stored in the context-aware configuration pages, and include them in the publication check:

```
  io.wcm.caconfig.extensions.references.impl.ConfigurationReferenceProvider
    enabled=B"true"
    assetReferences=B"true"
```


### Logic

In order to find CAC resources associated with a content page this reference provider uses the following approach:

1. It retrieves all configuration names from `ConfigurationManager.getConfigurationNames()`
2. For each configuration name it tries to find the relevant resources (including inherited ones) from `ConfigurationResourceResolvingStrategyMultiplexer.getResourceInheritanceChain(...)` considering all buckets retrieved from `ConfigurationResourceResolverConfig.configBucketNames()`
3. All found resources are mapped to their container pages and the returned references refer to the pages and their respective modification date

This is just a *heuristic* as this will return all CACs potentially used on the content page. It doesn't necessarily mean the returned CAC resource is actually leveraged when rendering the page.

Also it doesn't necessarily find all CACs. The following ones are not found:

- CACs not using [`@Configuration` annotations][annotation-classes] (as this is the only source for configuration names)


### AEM Core WCM Components' Reference Provider

AEMs Core WCM Components ship with a [similar reference provider][wcm-core-reference-provider]. However, this has the following limitations compared to this one:

- It doesn't properly support the full inheritance chain of CACs (but only finds the deepest one)
- It only supports the default bucket name `sling:configs` ([#3114](https://github.com/adobe/aem-core-wcm-components/issues/3114))
- It doesn't find asset references inside CAC resources

Therefore, the Core WCM Components' reference provider is [automatically disabled][core-component-reference-provider-disabler] when this bundle is active. 


### Activating CACs via other means

Publishing the CACs alone (e.g. via the [Configuration Browser][configuration-browser]) does usually not have the desired effect as this won't invalidate the affected cached content pages in the dispatcher and also this is cumbersome as it doesn't consider the full inheritance hierarchy. Therefore relying on a reference provider and just activating CACs along with a content page is the most reliable way.


[aem-referenceprovider]: https://experienceleague.adobe.com/en/tools/aem-api-documentation/cloud-service/javadoc/com/day/cq/wcm/api/reference/ReferenceProvider.html
[persistence-strategies]: persistence-strategies.html
[annotation-classes]: https://sling.apache.org/documentation/bundles/context-aware-configuration/context-aware-configuration.html#describe-configurations-via-annotation-classes
[wcm-core-reference-provider]: https://github.com/adobe/aem-core-wcm-components/blob/main/bundles/core/src/main/java/com/adobe/cq/wcm/core/components/internal/services/CaConfigReferenceProvider.java
[configuration-browser]: https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/developing/configurations#using-configuration-browser
[core-component-reference-provider-disabler]: https://github.com/wcm-io/io.wcm.caconfig.extensions/blob/develop/src/main/java/io/wcm/caconfig/extensions/references/impl/CoreCompReferenceProviderDisabler.java
