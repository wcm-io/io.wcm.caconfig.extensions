## About Context-Aware Configuration Extensions for AEM

AEM-specific extensions for Apache Sling Context-Aware Configuration.

[![Maven Central](https://img.shields.io/maven-central/v/io.wcm/io.wcm.caconfig.extensions)](https://repo1.maven.org/maven2/io/wcm/io.wcm.caconfig.extensions/)


### Documentation

* [Usage][usage]
* [Changelog][changelog]


### Overview

The following extensions are provided:

* Configure [Context Path Strategies][context-path-strategies]: without the need for `sling:configRef` attributes based on hierarchy levels or root templates
* AEM-specific [Persistence Strategies][persistence-strategies] to store configuration in `cq:Page` nodes either in `/conf` or in `tools/config` pages together with the content
* Configuration [Override Provider][override-providers] based on request headers (e.g. for QA instances - disabled by default)
* A [Reference Provider][reference-provider] implementation for context-aware configurations


### AEM Version Support Matrix

|Context-Aware Configuration Extensions for AEM version |AEM version supported
|-------------------------------------------------------|----------------------
|1.9.6 or higher                                        |AEM 6.5.17+ *), AEMaaCS
|1.9.2 - 1.9.4                                          |AEM 6.5.7+ *), AEMaaCS
|1.9.0                                                  |AEM 6.5+ *), AEMaaCS
|1.8.x                                                  |AEM 6.4+, AEMaaCS
|1.7.x                                                  |AEM 6.3+
|1.6.x                                                  |AEM 6.2+
|1.0.x - 1.5.x                                          |AEM 6.1+

### Dependencies

*) For version 1.9.0 and up, the bundle depends on `org.apache.sling.caconfig.spi` 1.4.0 and `org.apache.sling.caconfig.impl` 1.6.0 - but AEM 6.5 contains older versions of these bundles. It is recommended to update latest versions of these bundles and deploy them together with your application:

|---|---|---|
| [Apache Sling Context-Aware Configuration API](https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.caconfig.api/) | [![Maven Central](https://img.shields.io/maven-central/v/org.apache.sling/org.apache.sling.caconfig.api)](https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.caconfig.api/) |
| [Apache Sling Context-Aware Configuration SPI](https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.caconfig.spi/) | [![Maven Central](https://img.shields.io/maven-central/v/org.apache.sling/org.apache.sling.caconfig.spi)](https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.caconfig.spi/) |
| [Apache Sling Context-Aware Configuration Implementation](https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.caconfig.impl/) | [![Maven Central](https://img.shields.io/maven-central/v/org.apache.sling/org.apache.sling.caconfig.impl)](https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.caconfig.impl/) |

AEMaaCS includes recent enough versions of these dependencies.


### GitHub Repository

Sources: https://github.com/wcm-io/io.wcm.caconfig.extensions


[usage]: usage.html
[changelog]: changes-report.html
[context-path-strategies]: context-path-strategies.html
[persistence-strategies]: persistence-strategies.html
[override-providers]: override-providers.html
[reference-provider]: reference-provider.html
