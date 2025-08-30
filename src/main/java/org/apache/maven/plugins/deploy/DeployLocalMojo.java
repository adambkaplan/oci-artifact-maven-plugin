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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Deploys an artifact to a local directory as if it were a remote Maven repository.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey (refactoring only)</a>
 */
@Mojo(name = "deploy-local", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class DeployLocalMojo extends AbstractDeployProjectMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", required = true, readonly = true)
    private List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "${plugin}", required = true, readonly = true)
    private PluginDescriptor pluginDescriptor;

    /**
     * Whether every project should be deployed during its own deploy-phase or at the end of the multimodule build. If
     * set to {@code true} and the build fails, none of the reactor projects is deployed.
     *
     * @since 2.8
     */
    @Parameter(defaultValue = "false", property = "deployAtEnd")
    private boolean deployAtEnd;

    /**
     * The local directory to deploy to. This will be treated as a Maven repository root.
     */
    @Parameter(property = "deployLocalDirectory", defaultValue = "${project.build.directory}/deploy-local")
    private File deployLocalDirectory;

    /**
     * Set this to 'true' to bypass artifact deploy
     * Since since 3.0.0-M2 it's not anymore a real boolean as it can have more than 2 values:
     * <ul>
     *     <li><code>true</code>: will skip as usual</li>
     *     <li><code>releases</code>: will skip if current version of the project is a release</li>
     *     <li><code>snapshots</code>: will skip if current version of the project is a snapshot</li>
     *     <li>any other values will be considered as <code>false</code></li>
     * </ul>
     * @since 2.4
     */
    @Parameter(property = "maven.deploy.skip", defaultValue = "false")
    private String skip = Boolean.FALSE.toString();

    private static final String DEPLOY_PROCESSED_MARKER = DeployLocalMojo.class.getName() + ".processed";

    private void putState(State state) {
        getPluginContext().put(DEPLOY_PROCESSED_MARKER, state.name());
    }

    private State getState(Map<String, Object> pluginContext) {
        return State.valueOf((String) pluginContext.get(DEPLOY_PROCESSED_MARKER));
    }

    private boolean hasState(MavenProject project) {
        Map<String, Object> pluginContext = session.getPluginContext(pluginDescriptor, project);
        return pluginContext.containsKey(DEPLOY_PROCESSED_MARKER);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        State state;
        if (Boolean.parseBoolean(skip)
                || ("releases".equals(skip) && !ArtifactUtils.isSnapshot(project.getVersion()))
                || ("snapshots".equals(skip) && ArtifactUtils.isSnapshot(project.getVersion()))) {
            getLog().info("Skipping artifact deployment");
            state = State.SKIPPED;
        } else {
            warnIfAffectedPackagingAndMaven(project.getPackaging());

            if (!deployAtEnd) {
                DeployRequest request = new DeployRequest();
                request.setRepository(createLocalRepository());
                processProject(project, request);
                deployLocal(request);
                state = State.DEPLOYED;
            } else {
                state = State.TO_BE_DEPLOYED;
            }
        }

        putState(state);

        List<MavenProject> allProjectsUsingPlugin = getAllProjectsUsingPlugin(reactorProjects, pluginDescriptor);

        if (allProjectsMarked(allProjectsUsingPlugin)) {
            deployAllAtOnce(allProjectsUsingPlugin);
        } else if (state == State.TO_BE_DEPLOYED) {
            getLog().info("Deferring deploy for " + project.getGroupId() + ":" + project.getArtifactId() + ":"
                    + project.getVersion() + " at end");
        }
    }

    private void deployAllAtOnce(List<MavenProject> allProjectsUsingPlugin) throws MojoExecutionException {
        DeployRequest request = new DeployRequest();
        request.setRepository(createLocalRepository());

        // collect all artifacts from all modules to deploy
        for (MavenProject reactorProject : allProjectsUsingPlugin) {
            Map<String, Object> pluginContext = session.getPluginContext(pluginDescriptor, reactorProject);
            State state = getState(pluginContext);
            if (state == State.TO_BE_DEPLOYED) {
                processProject(reactorProject, request);
            }
        }

        // execute deployment
        deployLocal(request);
    }

    private boolean allProjectsMarked(List<MavenProject> allProjectsUsingPlugin) {
        for (MavenProject reactorProject : allProjectsUsingPlugin) {
            if (!hasState(reactorProject)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a local repository for deployment.
     */
    private RemoteRepository createLocalRepository() {
        return new RemoteRepository.Builder(
                        "deploy-local", "default", deployLocalDirectory.toURI().toString())
                .build();
    }

    /**
     * Deploys artifacts to the local directory.
     */
    private void deployLocal(DeployRequest request) throws MojoExecutionException {
        try {
            // Ensure the base directory exists
            if (!deployLocalDirectory.exists()) {
                deployLocalDirectory.mkdirs();
            }

            // Deploy each artifact
            for (Artifact artifact : request.getArtifacts()) {
                deployArtifactToLocal(artifact);
            }

            getLog().info("Successfully deployed to local directory: " + deployLocalDirectory.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to deploy to local directory", e);
        }
    }

    /**
     * Deploys a single artifact to the local directory.
     */
    private void deployArtifactToLocal(Artifact artifact) throws IOException, MojoExecutionException {
        if (artifact.getFile() == null || !artifact.getFile().exists()) {
            getLog().warn("Skipping artifact with no file: " + artifact);
            return;
        }

        // Create the directory structure
        String relativePath = getArtifactPath(artifact);
        Path targetPath = deployLocalDirectory.toPath().resolve(relativePath);
        Path targetDir = targetPath.getParent();

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        // Copy the artifact file
        Files.copy(artifact.getFile().toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        getLog().debug("Deployed artifact: " + artifact + " to " + targetPath);

        // Generate checksums if the artifact is not a POM
        if (!"pom".equals(artifact.getExtension())) {
            generateChecksums(artifact.getFile().toPath(), targetPath);
        }
    }

    /**
     * Gets the relative path for an artifact in the repository structure.
     */
    private String getArtifactPath(Artifact artifact) {
        StringBuilder path = new StringBuilder();
        path.append(artifact.getGroupId().replace('.', '/'));
        path.append('/');
        path.append(artifact.getArtifactId());
        path.append('/');
        path.append(artifact.getVersion());
        path.append('/');
        path.append(artifact.getArtifactId());
        path.append('-');
        path.append(artifact.getVersion());

        if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
            path.append('-');
            path.append(artifact.getClassifier());
        }

        path.append('.');
        path.append(artifact.getExtension());

        return path.toString();
    }

    /**
     * Generates MD5 and SHA1 checksums for the artifact.
     */
    private void generateChecksums(Path sourcePath, Path targetPath) throws IOException {
        // Generate MD5 checksum
        Path md5Path = targetPath.resolveSibling(targetPath.getFileName() + ".md5");
        String md5 = calculateMD5(sourcePath);
        Files.write(md5Path, md5.getBytes());

        // Generate SHA1 checksum
        Path sha1Path = targetPath.resolveSibling(targetPath.getFileName() + ".sha1");
        String sha1 = calculateSHA1(sourcePath);
        Files.write(sha1Path, sha1.getBytes());
    }

    /**
     * Calculates MD5 checksum for a file.
     */
    private String calculateMD5(Path path) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(Files.readAllBytes(path));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }

    /**
     * Calculates SHA1 checksum for a file.
     */
    private String calculateSHA1(Path path) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(Files.readAllBytes(path));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 algorithm not available", e);
        }
    }

    private enum State {
        SKIPPED,
        TO_BE_DEPLOYED,
        DEPLOYED
    }
}
