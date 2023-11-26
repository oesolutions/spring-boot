/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.build;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.Delete;

/**
 * A plugin to make a project's {@code deployment} publication available as a Maven
 * repository. The repository can be consumed by depending upon the project using the
 * {@code mavenRepository} configuration.
 *
 * @author Andy Wilkinson
 */
public class MavenRepositoryPlugin implements Plugin<Project> {

	/**
	 * Name of the {@code mavenRepository} configuration.
	 */
	public static final String MAVEN_REPOSITORY_CONFIGURATION_NAME = "mavenRepository";

	/**
	 * Name of the task that publishes to the project repository.
	 */
	public static final String PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME = "publishToProjectRepository";

	@Override
	public void apply(Project project) {
		Task publishToProjectRepo = project.getTasks()
			.create(PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME, (t) -> t.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP));
		Configuration projectRepository = project.getConfigurations().create(MAVEN_REPOSITORY_CONFIGURATION_NAME);
		project.getPlugins().apply(MavenPublishPlugin.class);
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		File repositoryLocation = new File(project.getBuildDir(), "maven-repository");
		Delete cleanTask = project.getTasks()
			.create("cleanMavenRepository", Delete.class, (t) -> t.setDelete(repositoryLocation));
		publishing.getRepositories().maven((mavenRepository) -> {
			mavenRepository.setName("project");
			mavenRepository.setUrl(repositoryLocation.toURI());
		});
		project.getTasks()
			.matching((task) -> task.getName().matches("publish.*PublicationToProjectRepository"))
			.all((task) -> {
				publishToProjectRepo.dependsOn(task);
				setUpProjectRepository(project, projectRepository, task, cleanTask, repositoryLocation);
			});
		project.getTasks()
			.matching((task) -> task.getName().equals("publishPluginMavenPublicationToProjectRepository"))
			.all((task) -> setUpProjectRepository(project, projectRepository, task, cleanTask, repositoryLocation));
	}

	private void setUpProjectRepository(Project project, Configuration projectRepository, Task publishTask,
			Delete cleanTask, File repositoryLocation) {
		publishTask.dependsOn(cleanTask);
		project.getArtifacts()
			.add(projectRepository.getName(), repositoryLocation, (artifact) -> artifact.builtBy(publishTask));
		DependencySet target = projectRepository.getDependencies();
		project.getPlugins()
			.withType(JavaPlugin.class)
			.all((javaPlugin) -> addMavenRepositoryDependencies(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
					target));
		project.getPlugins()
			.withType(JavaLibraryPlugin.class)
			.all((javaLibraryPlugin) -> addMavenRepositoryDependencies(project, JavaPlugin.API_CONFIGURATION_NAME,
					target));
		project.getPlugins()
			.withType(JavaPlatformPlugin.class)
			.all((javaPlugin) -> addMavenRepositoryDependencies(project, JavaPlatformPlugin.API_CONFIGURATION_NAME,
					target));
	}

	private void addMavenRepositoryDependencies(Project project, String sourceConfigurationName, DependencySet target) {
		project.getConfigurations()
			.getByName(sourceConfigurationName)
			.getDependencies()
			.withType(ProjectDependency.class)
			.all((dependency) -> {
				Map<String, String> dependencyDescriptor = new HashMap<>();
				dependencyDescriptor.put("path", dependency.getDependencyProject().getPath());
				dependencyDescriptor.put("configuration", MAVEN_REPOSITORY_CONFIGURATION_NAME);
				target.add(project.getDependencies().project(dependencyDescriptor));
			});
	}

}
