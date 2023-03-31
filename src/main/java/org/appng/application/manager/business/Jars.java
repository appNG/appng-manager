/*
 * Copyright 2011-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appng.application.manager.business;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.model.JarInfo;
import org.appng.core.model.JarInfo.JarInfoBuilder;
import org.springframework.stereotype.Component;

/**
 * A {@link DataProvider} providing information about the JAR-files used by a site/the platform.
 * 
 * @author Matthias Herlitzius
 */

@Component
public class Jars extends ServiceAware implements DataProvider {

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Integer siteId = options.getInteger(Sites.SITE, ID);
		DataContainer data = new DataContainer(fp);
		List<JarInfo> jars;
		if (null == options.getOptionValue("tomcatJars", "show")) {
			jars = getService().getJars(environment, siteId);
		} else {
			jars = new ArrayList<JarInfo>();
			File baseFolder = new File(System.getProperty("catalina.base"), "lib");
			File homeFolder = new File(System.getProperty("catalina.home"), "lib");
			FilenameFilter jarFilter = (dir, name) -> name.endsWith(".jar");
			for (String jarfile : baseFolder.list(jarFilter)) {
				jars.add(JarInfoBuilder.getJarInfo(new File(baseFolder, jarfile)));
			}
			if (!baseFolder.equals(homeFolder)) {
				for (String jarfile : homeFolder.list(jarFilter)) {
					jars.add(JarInfoBuilder.getJarInfo(new File(homeFolder, jarfile)));
				}
			}
			Collections.sort(jars);
		}
		data.setPage(jars, fp.getPageable());
		return data;
	}
}
