/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

# Maven Deploy Plugin - Deploy Local Goal

## Overview

The `deploy-local` goal is a new addition to the Maven Deploy Plugin that allows you to deploy Maven artifacts to a local directory as if it were a remote Maven repository. This is useful for:

- Testing deployment configurations locally
- Creating local repositories for development
- Verifying artifact structure before remote deployment
- CI/CD pipelines that need local artifact storage

## Goal Name

`deploy-local`

## Usage

### Basic Usage

```bash
mvn deploy:deploy-local
```

### With Custom Directory

```bash
mvn deploy:deploy-local -DdeployLocalDirectory=/path/to/local/repo
```

### In POM Configuration

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-deploy-plugin</artifactId>
    <version>3.1.1</version>
    <configuration>
        <deployLocalDirectory>${project.build.directory}/local-repo</deployLocalDirectory>
    </configuration>
</plugin>
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `deployLocalDirectory` | File | `${project.build.directory}/deploy-local` | The local directory to deploy to |
| `deployAtEnd` | boolean | `false` | Whether to deploy at the end of the multimodule build |
| `skip` | String | `false` | Skip deployment (can be `true`, `releases`, `snapshots`) |
| `allowIncompleteProjects` | boolean | `false` | Allow incomplete projects to be deployed |

## Features

### Repository Structure

The local deployment creates a standard Maven repository structure:

```
deploy-local/
├── org/
│   └── apache/
│       └── maven/
│           └── test/
│               └── maven-deploy-test/
│                   └── 1.0-SNAPSHOT/
│                       ├── maven-deploy-test-1.0-SNAPSHOT.jar
│                       ├── maven-deploy-test-1.0-SNAPSHOT.jar.md5
│                       ├── maven-deploy-test-1.0-SNAPSHOT.jar.sha1
│                       └── maven-deploy-test-1.0-SNAPSHOT.pom
```

### Checksum Generation

The goal automatically generates MD5 and SHA1 checksums for all artifacts (except POM files), matching the behavior of remote Maven repositories.

### Multimodule Support

Supports the same `deployAtEnd` functionality as the main `deploy` goal, allowing you to defer deployment until all modules are built.

### Skip Logic

Supports the same skip logic as the main `deploy` goal:
- `true`: Skip all deployments
- `releases`: Skip only release versions
- `snapshots`: Skip only snapshot versions

## Examples

### Deploy Current Project

```bash
mvn deploy:deploy-local
```

### Deploy to Custom Location

```bash
mvn deploy:deploy-local -DdeployLocalDirectory=/tmp/my-repo
```

### Deploy with Skip for Releases

```bash
mvn deploy:deploy-local -Dmaven.deploy.skip=releases
```

### Deploy at End of Multimodule Build

```bash
mvn deploy:deploy-local -DdeployAtEnd=true
```

## Implementation Details

The `DeployLocalMojo` extends `AbstractDeployProjectMojo` and reuses much of the existing deployment logic. Key differences from the main `deploy` goal:

1. **Local File System**: Deploys to local directories instead of remote repositories
2. **No Network**: No authentication, proxy, or network-related functionality
3. **Checksum Generation**: Manually generates MD5 and SHA1 checksums
4. **File Copying**: Uses Java NIO for efficient file operations

## Testing

The goal includes comprehensive tests that verify:
- Basic artifact deployment
- Skip functionality
- Attached artifacts handling
- Repository structure creation
- Checksum generation

## Compatibility

- **Maven Version**: 3.0+
- **Java Version**: 8+
- **Plugin Version**: 3.1.1+

## Migration from Deploy Goal

If you're currently using the `deploy` goal and want to switch to local deployment:

1. Change the goal from `deploy:deploy` to `deploy:deploy-local`
2. Add the `deployLocalDirectory` parameter if you want a custom location
3. Remove any remote repository configuration (not needed for local deployment)

## Troubleshooting

### Common Issues

1. **Permission Denied**: Ensure the target directory is writable
2. **Directory Not Created**: The goal will create directories automatically
3. **Artifacts Not Deployed**: Check that the project has been built (`mvn compile` or `mvn package`)

### Debug Information

Enable debug logging to see detailed deployment information:

```bash
mvn deploy:deploy-local -X
```

## Contributing

The `deploy-local` goal follows the same coding standards and patterns as the existing plugin code. When contributing:

1. Follow the existing code style
2. Add appropriate tests
3. Update documentation
4. Ensure backward compatibility
