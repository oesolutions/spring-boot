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

package org.springframework.boot.build.bom;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.tasks.TaskProvider;

public abstract class VersionCatalogPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {

		VersionCatalogExtension versionCatalogExt = project.getExtensions()
			.create("versionCatalog", VersionCatalogExtension.class);

		Configuration conf = project.getConfigurations().create("versionCatalogElements");
		conf.setDescription("Artifacts for the version catalog");
		conf.setCanBeResolved(false);
		conf.setCanBeConsumed(true);
		conf.setVisible(false);
		conf.attributes((attrs) -> {
			attrs.attribute(Category.CATEGORY_ATTRIBUTE,
					project.getObjects().named(Category.class, Category.REGULAR_PLATFORM));
			attrs.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.VERSION_CATALOG));
		});
		AdhocComponentWithVariants versionCatalog = getSoftwareComponentFactory().adhoc("versionCatalog");
		project.getComponents().add(versionCatalog);
		versionCatalog.addVariantsFromConfiguration(conf, (details) -> {
			details.mapToMavenScope("compile");
			details.mapToOptional();
		});

		project.getPlugins().withType(BomPlugin.class).all((bomPlugin) -> {
			BomExtension bomExt = project.getExtensions().getByType(BomExtension.class);
			TaskProvider<GenerateVersionCatalogToml> gen = project.getTasks()
				.register("generateCatalogToml", GenerateVersionCatalogToml.class, (t) -> {
					t.getBomExtension().convention(bomExt);
					t.getSpringBootVersion().convention(versionCatalogExt.getSpringBootVersion());
					t.getOutputFile()
						.convention(project.getLayout()
							.getBuildDirectory()
							.file("version-catalog/spring-boot-catalog.toml"));
				});
			conf.getOutgoing().artifact(gen);
		});
	}

	@Inject
	protected abstract SoftwareComponentFactory getSoftwareComponentFactory();

}
