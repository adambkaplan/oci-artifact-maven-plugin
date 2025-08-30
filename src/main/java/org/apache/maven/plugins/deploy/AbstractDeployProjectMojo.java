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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

public abstract class AbstractDeployProjectMojo extends AbstractDeployMojo {

    private static final Pattern ALT_LEGACY_REPO_SYNTAX_PATTERN = Pattern.compile("(.+?)::(.+?)::(.+)");
    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile("(.+?)::(.+)");

    /**
     * Set this to <code>true</code> to allow incomplete project processing. By default, such projects are forbidden
     * and Mojo will fail to process them. Incomplete project is a Maven Project that has any other packaging than
     * "pom" and has no main artifact packaged. In the majority of cases, what user really wants here is a project
     * with "pom" packaging and some classified artifact attached (typical example is some assembly being packaged
     * and attached with classifier).
     *
     * @since 3.1.1
     */
    @Parameter(defaultValue = "false", property = "allowIncompleteProjects")
    private boolean allowIncompleteProjects;

    public boolean isAllowIncompleteProjects() {
        return this.allowIncompleteProjects;
    }

    boolean isFile(File file) {
        return file != null && file.isFile();
    }

    void processProject(final MavenProject project, DeployRequest request) throws MojoExecutionException {
        // always exists, as project exists
        Artifact pomArtifact = RepositoryUtils.toArtifact(new ProjectArtifact(project));
        // always exists, but at "init" is w/o file (packaging plugin assigns file to this when packaged)
        Artifact projectArtifact = RepositoryUtils.toArtifact(project.getArtifact());

        // pom project: pomArtifact and projectArtifact are SAME
        // jar project: pomArtifact and projectArtifact are DIFFERENT
        // incomplete project: is not pom project and projectArtifact has no file

        // we must compare coordinates ONLY (as projectArtifact may not have file, and Artifact.equals factors it in)
        // BUT if projectArtifact has file set, use that one
        if (ArtifactIdUtils.equalsId(pomArtifact, projectArtifact)) {
            if (isFile(projectArtifact.getFile())) {
                pomArtifact = projectArtifact;
            }
            projectArtifact = null;
        }

        if (isFile(pomArtifact.getFile())) {
            request.addArtifact(pomArtifact);
        } else {
            throw new MojoExecutionException(
                    "The POM for project " + project.getArtifactId() + " could not be attached");
        }

        // is not packaged, is "incomplete"
        boolean isIncomplete = projectArtifact != null && !isFile(projectArtifact.getFile());
        if (projectArtifact != null) {
            if (!isIncomplete) {
                request.addArtifact(projectArtifact);
            } else if (!project.getAttachedArtifacts().isEmpty()) {
                if (allowIncompleteProjects) {
                    getLog().warn("");
                    getLog().warn("The packaging plugin for project " + project.getArtifactId() + " did not assign");
                    getLog().warn("a main file to the project but it has attachments. Change packaging to 'pom'.");
                    getLog().warn("");
                    getLog().warn("Incomplete projects like this will fail in future Maven versions!");
                    getLog().warn("");
                } else {
                    throw new MojoExecutionException("The packaging plugin for project " + project.getArtifactId()
                            + " did not assign a main file to the project but it has attachments. Change packaging"
                            + " to 'pom'.");
                }
            } else {
                throw new MojoExecutionException("The packaging plugin for project " + project.getArtifactId()
                        + " did not assign a file to the build artifact");
            }
        }

        for (org.apache.maven.artifact.Artifact attached : project.getAttachedArtifacts()) {
            getLog().debug("Attaching for deploy: " + attached.getId());
            request.addArtifact(RepositoryUtils.toArtifact(attached));
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

    /**
     * Gets the deployment repository for a project, considering alternative repositories.
     */
    protected RemoteRepository getDeploymentRepository(
            final MavenProject project,
            final String altSnapshotDeploymentRepository,
            final String altReleaseDeploymentRepository,
            final String altDeploymentRepository)
            throws MojoExecutionException {
        RemoteRepository repo = null;

        String altDeploymentRepo;
        if (ArtifactUtils.isSnapshot(project.getVersion()) && altSnapshotDeploymentRepository != null) {
            altDeploymentRepo = altSnapshotDeploymentRepository;
        } else if (!ArtifactUtils.isSnapshot(project.getVersion()) && altReleaseDeploymentRepository != null) {
            altDeploymentRepo = altReleaseDeploymentRepository;
        } else {
            altDeploymentRepo = altDeploymentRepository;
        }

        if (altDeploymentRepo != null) {
            getLog().info("Using alternate deployment repository " + altDeploymentRepo);

            Matcher matcher = ALT_LEGACY_REPO_SYNTAX_PATTERN.matcher(altDeploymentRepo);

            if (matcher.matches()) {
                String id = matcher.group(1).trim();
                String layout = matcher.group(2).trim();
                String url = matcher.group(3).trim();

                if ("default".equals(layout)) {
                    getLog().warn("Using legacy syntax for alternative repository. " + "Use \"" + id + "::" + url
                            + "\" instead.");
                    repo = getRemoteRepository(id, url);
                } else {
                    throw new MojoExecutionException("Invalid legacy syntax and layout for alternative repository: \""
                            + altDeploymentRepo + "\". Use \"" + id + "::" + url
                            + "\" instead, and only default layout is supported.");
                }
            } else {
                matcher = ALT_REPO_SYNTAX_PATTERN.matcher(altDeploymentRepo);

                if (!matcher.matches()) {
                    throw new MojoExecutionException("Invalid syntax for alternative repository: \"" + altDeploymentRepo
                            + "\". Use \"id::url\".");
                } else {
                    String id = matcher.group(1).trim();
                    String url = matcher.group(2).trim();

                    repo = getRemoteRepository(id, url);
                }
            }
        }

        if (repo == null) {
            repo = RepositoryUtils.toRepo(project.getDistributionManagementArtifactRepository());
        }

        if (repo == null) {
            String msg = "Deployment failed: repository element was not specified in the POM inside"
                    + " distributionManagement element or in -DaltDeploymentRepository=id::url parameter";

            throw new MojoExecutionException(msg);
        }

        return repo;
    }

    /**
     * Gets all projects that use this plugin from the reactor.
     */
    protected List<MavenProject> getAllProjectsUsingPlugin(
            List<MavenProject> reactorProjects, org.apache.maven.plugin.descriptor.PluginDescriptor pluginDescriptor) {
        ArrayList<MavenProject> result = new ArrayList<>();
        for (MavenProject reactorProject : reactorProjects) {
            if (reactorProject.getBuild() != null
                    && hasExecution(reactorProject.getPlugin("org.apache.maven.plugins:maven-deploy-plugin"))) {
                result.add(reactorProject);
            }
        }
        return result;
    }

    /**
     * Checks if a plugin has any executions.
     */
    protected boolean hasExecution(Plugin plugin) {
        if (plugin == null) {
            return false;
        }

        for (PluginExecution execution : plugin.getExecutions()) {
            if (!execution.getGoals().isEmpty() && !"none".equalsIgnoreCase(execution.getPhase())) {
                return true;
            }
        }
        return false;
    }
}
