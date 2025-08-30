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

import org.apache.maven.RepositoryUtils;
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

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public boolean isAllowIncompleteProjects() {
        return this.allowIncompleteProjects;
    }

    boolean isFile(File file) {
        return file != null && file.isFile();
    }

    protected String getSkip() {
        return this.skip;
    }

    protected MavenProject getProject() {
        return this.project;
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

    protected abstract RemoteRepository getDeploymentRepository() throws MojoExecutionException;
}
