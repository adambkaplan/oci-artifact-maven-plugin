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
package org.apache.maven.plugins.deploy;

import java.nio.file.Path;
import java.nio.file.Paths;

import land.oras.ContainerRef;
import land.oras.LocalPath;
import land.oras.Manifest;
import land.oras.Registry;
import land.oras.auth.UsernamePasswordProvider;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

/**
 * Abstract class for Deploy mojo's.
 */
public abstract class AbstractDeployMojo extends AbstractMojo {
    /**
     * Flag whether Maven is currently in online/offline mode.
     */
    @Parameter(defaultValue = "${settings.offline}", readonly = true)
    private boolean offline;

    /**
     * Parameter used to control how many times a failed deployment will be retried before giving up and failing. If a
     * value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
     *
     * @since 2.7
     */
    @Parameter(property = "retryFailedDeploymentCount", defaultValue = "1")
    private int retryFailedDeploymentCount;

    /**
     * Container image repository to push the artifacts to. Must be a fully qualified image reference.
     * Example: docker.io/myuser/myrepo
     */
    @Parameter(property = "imageRepo", required = true)
    private String imageRepo;

    /**
     * Container image tag to use when pushing the artifacts to the registry.
     * Default: latest
     */
    @Parameter(property = "imageTag", defaultValue = "latest")
    private String imageTag;

    /**
     * Username to use when pushing the artifacts to the registry.
     * Optional.
     */
    @Parameter(property = "registryUsername")
    private String registryUsername;

    /**
     * Password to use when pushing the artifacts to the registry.
     * Optional.
     */
    @Parameter(property = "registryPassword")
    private String registryPassword;

    /**
     * Whether to skip TLS verification when pushing the artifacts to the registry.
     * Default: false
     */
    @Parameter(property = "insecureTLSNoVerify", defaultValue = "false")
    private boolean insecureTLSNoVerify;

    @Component
    private RuntimeInformation runtimeInformation;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Component
    protected RepositorySystem repositorySystem;

    private static final String AFFECTED_MAVEN_PACKAGING = "maven-plugin";

    private static final String FIXED_MAVEN_VERSION = "3.9.0";

    /* Setters and Getters */

    void failIfOffline() throws MojoFailureException {
        if (offline) {
            throw new MojoFailureException("Cannot deploy artifacts when Maven is in offline mode");
        }
    }

    /**
     * If this plugin used in pre-3.9.0 Maven, the packaging {@code maven-plugin} will not deploy G level metadata.
     */
    protected void warnIfAffectedPackagingAndMaven(final String packaging) {
        if (AFFECTED_MAVEN_PACKAGING.equals(packaging)) {
            try {
                GenericVersionScheme versionScheme = new GenericVersionScheme();
                Version fixedMavenVersion = versionScheme.parseVersion(FIXED_MAVEN_VERSION);
                Version currentMavenVersion = versionScheme.parseVersion(runtimeInformation.getMavenVersion());
                if (fixedMavenVersion.compareTo(currentMavenVersion) > 0) {
                    getLog().warn("");
                    getLog().warn("You are about to deploy a maven-plugin using Maven " + currentMavenVersion + ".");
                    getLog().warn("This plugin should be used ONLY with Maven 3.9.0 and newer, as MNG-7055");
                    getLog().warn("is fixed in those versions of Maven only!");
                    getLog().warn("");
                }
            } catch (InvalidVersionSpecificationException e) {
                // skip it: Generic does not throw, only API contains this exception
            }
        }
    }

    /**
     * Creates resolver {@link RemoteRepository} equipped with needed whistles and bells.
     */
    protected RemoteRepository getRemoteRepository(final String repositoryId, final String url) {
        // TODO: RepositorySystem#newDeploymentRepository does this very same thing!
        RemoteRepository result = new RemoteRepository.Builder(repositoryId, "default", url).build();

        if (result.getAuthentication() == null || result.getProxy() == null) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder(result);

            if (result.getAuthentication() == null) {
                builder.setAuthentication(session.getRepositorySession()
                        .getAuthenticationSelector()
                        .getAuthentication(result));
            }

            if (result.getProxy() == null) {
                builder.setProxy(
                        session.getRepositorySession().getProxySelector().getProxy(result));
            }

            result = builder.build();
        }

        return result;
    }

    // I'm not sure if retries will work with deploying on client level ...
    // Most repository managers block a duplicate artifacts.

    // Eg, when we have an artifact list, even simple pom and jar in one request with released version,
    // next try can fail due to duplicate.

    protected void deploy(DeployRequest deployRequest) throws MojoExecutionException {
        deployOCI(deployRequest);
    }

    /**
     * Deploy the artifacts to the local build target directory.
     */
    private void deployOCI(DeployRequest deployRequest) throws MojoExecutionException {
        DeployRequest deployRequestCopy = new DeployRequest();
        deployRequestCopy.setArtifacts(deployRequest.getArtifacts());
        deployRequestCopy.setMetadata(deployRequest.getMetadata());
        deployRequestCopy.setTrace(deployRequest.getTrace());

        deployRequestCopy.setRepository(getRemoteRepository("build-target", "file://" + getLocalRepositoryPath()));
        try {
            repositorySystem.deploy(session.getRepositorySession(), deployRequestCopy);
        } catch (DeploymentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        pushOCIArtifact(deployRequest);
    }

    protected String getLocalRepositoryPath() {
        return session.getExecutionRootDirectory() + "/target/oci-artifacts";
    }

    private void pushOCIArtifact(DeployRequest deployRequest) throws MojoExecutionException {
        // HACK - this just packages the whole contents of the build-target repository
        // We could do better and have individual layers by GAV.
        Path artifactPath = Paths.get(getLocalRepositoryPath());
        // oras-java-sdk only supports a limited number of MIME types.
        // use the default one for packaging a directory. Ideally maven can provide its own MIME type.
        LocalPath localPath = LocalPath.of(artifactPath);
        Registry.Builder builder = Registry.builder().defaults().withInsecure(insecureTLSNoVerify);
        if (registryUsername != null
                && registryUsername.length() > 0
                && registryPassword != null
                && registryPassword.length() > 0) {
            builder = builder.withAuthProvider(new UsernamePasswordProvider(registryUsername, registryPassword));
        }
        Registry reg = builder.build();
        getLog().info("Pushing artifact to registry: " + imageRepo + ":" + imageTag);
        Manifest manifest = reg.pushArtifact(ContainerRef.parse(imageRepo + ":" + imageTag), localPath);
        getLog().info("Pushed digest: " + manifest.getDigest());
    }
}
