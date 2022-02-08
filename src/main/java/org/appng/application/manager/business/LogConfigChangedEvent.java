/*
 * Copyright 2011-2022 the original author or authors.
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
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;

public class LogConfigChangedEvent extends Event implements Serializable {

	private final String content;

	public LogConfigChangedEvent(String siteName, String content, String configFilePath) {
		super(siteName);
		this.content = content;
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException, BusinessException {
		try {
			Properties platformProps = environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
			String rootPath = platformProps.getString(Platform.Property.PLATFORM_ROOT_PATH);
			File configFilePath = new File(rootPath, LogConfig.LOG4J_PROPS);
			FileUtils.write(configFilePath, content, StandardCharsets.UTF_8);
			new PropertyConfigurator().doConfigure(configFilePath.getAbsolutePath(), LogManager.getLoggerRepository());
		} catch (IOException e) {
			throw new BusinessException("error while executing ReadloadConfigEvent", e);
		}
	}

}
