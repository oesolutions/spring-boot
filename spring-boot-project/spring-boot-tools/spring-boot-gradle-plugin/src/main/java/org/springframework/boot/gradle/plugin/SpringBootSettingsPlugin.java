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

package org.springframework.boot.gradle.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.initialization.resolve.MutableVersionCatalogContainer;
import org.gradle.util.GradleVersion;

import org.springframework.boot.gradle.util.VersionExtractor;

public class SpringBootSettingsPlugin implements Plugin<Settings> {

	private static final String SPRING_BOOT_VERSION = VersionExtractor.forClass(DependencyManagementPluginAction.class);

	/**
	 * The coordinates {@code (group:name:version)} of the {@code spring-boot-catalog}
	 * version catalog.
	 */
	public static final String CATALOG_COORDINATES = "org.springframework.boot:spring-boot-catalog:"
			+ SPRING_BOOT_VERSION;

	@Override
	public void apply(Settings settings) {
		verifyGradleVersion();
		registerCatalog(settings.getDependencyResolutionManagement().getVersionCatalogs());
	}

	private void registerCatalog(MutableVersionCatalogContainer versionCatalogs) {
		versionCatalogs.register("spring", (versionCatalogBuilder) -> versionCatalogBuilder.from(CATALOG_COORDINATES));
	}

	private void verifyGradleVersion() {
		GradleVersion currentVersion = GradleVersion.current();
		if (currentVersion.compareTo(GradleVersion.version("7.4")) < 0) {
			throw new GradleException("Spring Boot plugin requires Gradle 7.x (7.4 or later). "
					+ "The current version is " + currentVersion);
		}
	}

}
