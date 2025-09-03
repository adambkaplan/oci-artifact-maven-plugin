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

A Maven plugin for deploying and resolving Maven artifacts from OCI container registries.

**This originates as a hard fork of the [Apache Maven Deploy Plugin](https://maven.apache.org/plugins/maven-deploy-plugin/)**

## Getting Started

This plugin is not published on Maven Central (yet). To test it out, do the following:

- Clone this repository
- Build and install the plugin locally by running the following:

```sh
mvn clean install
```

Please note that this plugin is a very crude proof of concept, and may introduce siginificant
and undocumented breaking changes in the future.

## Usage

Add the following to your Maven project's `pom.xml`:

```xml
<plugins>
  <plugin>
    <groupId>org.opencontainers.maven.plugins</groupId>
    <artifactId>oci-artifact-maven-plugin</artifactId>
    <executions>
      <execution>
        <goals>
          <goal>deploy</goal>
        </goals>
        <configuration>
          <imageRepo>docker.io/myuser/myrepo</imageRepo>
          <imageTag>${project.version}</imageTag>
        </configuration>
      </execution>
    </executions>
  </plugin>
</plugins>
```

### Configuration Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `imageRepo` | String | Yes | - | Container image repository to push the artifacts to. Must be a fully qualified image reference (e.g., `docker.io/myuser/myrepo`) |
| `imageTag` | String | No | `latest` | Container image tag to use when pushing the artifacts to the registry |
| `registryUsername` | String | No | - | Username to use when pushing the artifacts to the registry |
| `registryPassword` | String | No | - | Password to use when pushing the artifacts to the registry |
| `insecureTLSNoVerify` | Boolean | No | `false` | Whether to skip TLS verification when pushing the artifacts to the registry |

### Goals

- `deploy`: Deploys all artifacts for a Maven project in an OCI artifact.
- `deploy-file`: Deploys a specified file for a Maven project in an OCI artifact.

### Authentication

The plugin by default utilizes your system's local container runtime to authenticate to the target
container registry. You can provide basic authentication data through the `registryUsername` and
`registryPassword` configuration options.

### Examples

#### Basic Usage

```xml
<plugin>
  <groupId>org.opencontainers.maven.plugins</groupId>
  <artifactId>oci-artifact-maven-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>deploy</goal>
      </goals>
      <configuration>
        <imageRepo>ghcr.io/myorg/myproject</imageRepo>
        <imageTag>${project.version}</imageTag>
      </configuration>
    </execution>
  </executions>
</plugin>
```

#### With Custom Tag and Authentication

```xml
<plugin>
  <groupId>org.opencontainers.maven.plugins</groupId>
  <artifactId>oci-artifact-maven-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>deploy</goal>
      </goals>
      <configuration>
        <imageRepo>docker.io/myuser/myrepo</imageRepo>
        <imageTag>custom-tag</imageTag>
        <registryUsername>${env.REGISTRY_USERNAME}</registryUsername>
        <registryPassword>${env.REGISTRY_PASSWORD}</registryPassword>
      </configuration>
    </execution>
  </executions>
</plugin>
```

## Contributing to OCI Artifact Maven Plugin


<!--
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)][license]
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-deploy-plugin.svg?label=Maven%20Central&versionPrefix=3.)](https://search.maven.org/artifact/org.apache.maven.plugins/maven-deploy-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-deploy-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.apache.maven.plugins/maven-deploy-plugin)
[![Reproducible Builds](https://img.shields.io/badge/Reproducible_Builds-ok-green?labelColor=blue)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/maven/plugins/maven-deploy-plugin/README.md)
[![Jenkins Status](https://img.shields.io/jenkins/s/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven-deploy-plugin/job/master.svg?)][build]
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci-maven.apache.org/job/Maven/job/maven-box/job/maven-deploy-plugin/job/master.svg?)][test-results]
-->


You have found a bug or you have an idea for a cool new feature? Contributing
code is a great way to give something back to the open source community. Before
you dig right into the code, there are a few guidelines that we need
contributors to follow so that we can have a chance of keeping on top of
things.

### Getting Started

- Make sure you have a [GitHub account](https://github.com/signup/free).
- Fork the repository on GitHub.

### Making and Submitting Changes

We accept Pull Requests via GitHub.

There are some guidelines which will make applying PRs easier for us:

- Create a topic branch from where you want to base your work (this is usually the main branch).
  Push your changes to a topic branch in your fork of the repository.
- Make commits of logical units.
- Respect the original code style: by using the same [codestyle][code-style],
  patches should only highlight the actual difference, not being disturbed by any formatting issues:
  - Only use spaces for indentation.
  - Create minimal diffs - disable on save actions like reformat source code or organize imports. 
    If you feel the source code should be reformatted, create a separate PR for this change.
  - Check for unnecessary whitespace with `git diff --check` before committing.
- Make sure you have added the necessary tests (JUnit/IT) for your changes.
- Run all the tests with `mvn clean verify` to assure nothing else was accidentally broken.
- Submit a pull request to the repository in this organization.

### Additional Resources

- [Contributing patches](https://maven.apache.org/guides/development/guide-maven-development.html#Creating_and_submitting_a_patch)
- [General GitHub documentation](https://help.github.com/)
- [GitHub pull request documentation](https://help.github.com/send-pull-requests/)

[license]: https://www.apache.org/licenses/LICENSE-2.0
[code-style]: https://maven.apache.org/developers/conventions/code.html
[maven-wiki]: https://cwiki.apache.org/confluence/display/MAVEN/Index
[test-results]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven-deploy-plugin/job/master/lastCompletedBuild/testReport/
[build]: https://ci-maven.apache.org/job/Maven/job/maven-box/job/maven-deploy-plugin/job/master/
