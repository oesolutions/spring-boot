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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

import org.springframework.boot.build.bom.Library.Group;
import org.springframework.boot.build.bom.Library.Module;

@UntrackedTask(
		because = "the bom extension is not serializable at this time and therefore cannot properly be used as an input property")
public abstract class GenerateVersionCatalogToml extends DefaultTask {

	@Internal
	public abstract Property<BomExtension> getBomExtension();

	@Input
	public abstract Property<String> getSpringBootVersion();

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@TaskAction
	protected void generateTomlFile() throws IOException {
		Path outFile = getOutputFile().get().getAsFile().toPath();
		Files.createDirectories(outFile.getParent());
		try (var writer = Files.newBufferedWriter(outFile)) {
			writer.write("[metadata]\n");
			writer.write("format.version = \"1.1\"\n\n");
			writer.write("[versions]\n");
			writer.write("springBoot = \"" + getSpringBootVersion().get() + "\"\n\n");
			writer.write("[plugins]\n");
			writer.write("springBoot = { id = \"org.springframework.boot\", version.ref = \"springBoot\" }\n\n");
			writer.write("[libraries]\n");
			for (Library library : getBomExtension().get().getLibraries()) {
				for (Group group : library.getGroups()) {
					for (Module module : group.getModules()) {
						if (module.isIncludedInCatalog()) {
							writer.write(String.format("%s = {group = \"%s\", name = \"%s\", version = \"\" }%n",
									module.getName(), group.getId(), module.getName()));
						}
					}
				}
			}
		}
	}

}
