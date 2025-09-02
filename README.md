<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
# OCI Artifact Maven Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)][license]
[![Maven Central](https://img.shields.io/maven-central/v/org.opencontainers.maven.plugins/oci-artifact-maven-plugin.svg?label=Maven%20Central&versionPrefix=3.)](https://search.maven.org/artifact/org.opencontainers.maven.plugins/oci-artifact-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/org.opencontainers.maven.plugins/oci-artifact-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.opencontainers.maven.plugins/oci-artifact-maven-plugin)
[![Reproducible Builds](https://img.shields.io/badge/Reproducible_Builds-ok-green?labelColor=blue)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/opencontainers/maven/plugins/oci-artifact-maven-plugin/README.md)
[![Jenkins Status](https://img.shields.io/jenkins/s/https/ci-maven.apache.org/job/Maven/job/maven-box/job/oci-artifact-maven-plugin/job/master.svg?)][build]
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci-maven.apache.org/job/Maven/job/maven-box/job/oci-artifact-maven-plugin/job/master.svg?)][test-results]

## Overview

The OCI Artifact Maven Plugin enables Maven projects to resolve and deploy artifacts using Open
Container Initiative (OCI) images. This plugin bridges the gap between traditional Maven artifact
repositories and modern container registries, allowing developers to leverage OCI-compliant
registries for storing and retrieving Maven artifacts.

**Note: this is a hard fork of the official [Maven Deploy Plugin](https://github.com/apache/maven-deploy-plugin/tree/maven-deploy-plugin-3.1.4)**
**This codebase owes a large debt of gratitude to the Apache Maven maintainers who developed the**
**Maven repository ecosystem.**

## Features

- **OCI Image Support**: Deploy and resolve Maven artifacts as OCI images
- **Container Registry Integration**: Work with any OCI-compliant container registry
- **Artifact Metadata**: Preserve Maven artifact metadata within OCI image annotations
- **Flexible Configuration**: Support for various OCI registry configurations
- **Maven Integration**: Seamless integration with existing Maven build processes

## Getting Started

### Prerequisites

- Maven 3.6.3 or higher
- Access to an OCI-compliant container registry

### Basic Usage

Add the plugin to your project's `pom.xml`:

```xml
<plugin>
  <groupId>org.opencontainers.maven.plugins</groupId>
  <artifactId>oci-artifact-maven-plugin</artifactId>
  <version>3.1.4</version>
  <configuration>
    <registryUrl>https://your-registry.com</registryUrl>
    <repository>your-org/your-repo</repository>
  </configuration>
</plugin>
```

### Deploy Artifacts

To deploy your project artifacts as OCI images:

```bash
mvn oci-artifact:deploy
```

### Resolve Artifacts

To resolve dependencies from OCI registries:

```bash
mvn oci-artifact:resolve
```

## Configuration

The plugin supports various configuration options for OCI registry integration:

- **Registry URL**: The base URL of your OCI registry
- **Repository**: The repository path within the registry
- **Authentication**: Support for various authentication methods
- **Image Tags**: Customizable tagging strategies for artifacts

## Contributing

You have found a bug or you have an idea for a cool new feature? Contributing
code is a great way to give something back to the open source community. Before
you dig right into the code, there are a few guidelines that we need
contributors to follow so that we can have a chance of keeping on top of
things.

### Getting Started

+ Make sure you have a [GitHub account](https://github.com/signup/free).
+ If you're planning to implement a new feature, it makes sense to discuss your changes 
  on the [dev list][ml-list]] first. 
  This way you can make sure you're not wasting your time on something that isn't 
  considered to be in the project's scope.
+ Submit a ticket for your issue, assuming one does not already exist.
  + Clearly describe the issue, including steps to reproduce when it is a bug.
  + Make sure you fill in the earliest version that you know has the issue.
+ Fork the repository on GitHub.

### Making and Submitting Changes

We accept Pull Requests via GitHub. The [developer mailing list][ml-list] is the
main channel of communication for contributors.  
There are some guidelines which will make applying PRs easier for us:
+ Create a topic branch from where you want to base your work (this is usually the master branch).
  Push your changes to a topic branch in your fork of the repository.
+ Make commits of logical units.
+ Respect the original code style: by using the same [codestyle][code-style],
  patches should only highlight the actual difference, not being disturbed by any formatting issues:
  + Only use spaces for indentation.
  + Create minimal diffs - disable on save actions like reformat source code or organize imports. 
    If you feel the source code should be reformatted, create a separate PR for this change.
  + Check for unnecessary whitespace with `git diff --check` before committing.
+ Make sure you have added the necessary tests (JUnit/IT) for your changes.
+ Run all the tests with `mvn -Prun-its verify` to assure nothing else was accidentally broken.
+ Submit a pull request to the repository in the Apache organization.

If you plan to contribute on a regular basis, please consider filing a [contributor license agreement][cla].

### Additional Resources

+ [Contributing patches](https://maven.apache.org/guides/development/guide-maven-development.html#Creating_and_submitting_a_patch)
+ [Contributor License Agreement][cla]
+ [General GitHub documentation](https://help.github.com/)
+ [GitHub pull request documentation](https://help.github.com/send-pull-requests/)
+ [Apache Maven Twitter Account](https://twitter.com/ASFMavenProject)

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.

## Links

- [Project Website](https://opencontainers.org/)
- [OCI Specification](https://github.com/opencontainers/image-spec)
- [Maven Plugin Development Guide](https://maven.apache.org/guides/plugin/guide-java-plugin-development.html)

[license]: https://www.apache.org/licenses/LICENSE-2.0
[ml-list]: https://maven.apache.org/mailing-lists.html
[code-style]: https://maven.apache.org/developers/conventions/code.html
[cla]: https://www.apache.org/licenses/#clas
[maven-wiki]: https://cwiki.apache.org/confluence/display/MAVEN/Index
[test-results]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/oci-artifact-maven-plugin/job/master/lastCompletedBuild/testReport/
[build]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/oci-artifact-maven-plugin/job/master/