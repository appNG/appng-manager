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
import java.nio.charset.StandardCharsets;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.appng.api.ActionProvider;
import org.appng.api.ApplicationException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.business.LogConfig.LogFile;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.PlatformEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogConfig extends ServiceAware implements DataProvider, ActionProvider<LogFile> {

	public static final String LOG4J_PROPS = "WEB-INF/conf/log4j.properties";

	@Value("${platform." + Platform.Property.PLATFORM_ROOT_PATH + "}")
	private String rootPath;

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		DataContainer container = new DataContainer(fp);
		LogFile logFile = new LogFile();
		container.setItem(logFile);
		if (!"location".equals(options.getOptionValue("mode", "value"))) {
			try {
				File configFile = getConfigFile();
				String content = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
				logFile.setContent(content);
				getService().createEvent(PlatformEvent.Type.INFO, "Changed " + LOG4J_PROPS);
			} catch (IOException e) {
				throw new ApplicationException(e);
			}
		}
		return container;
	}

	public File getConfigFile() {
		return new File(rootPath, LOG4J_PROPS);
	}

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			LogFile formBean, FieldProcessor fieldProcessor) {
		File configFile = getConfigFile();
		try {
			String currentContent = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
			String newContent = formBean.getContent();
			String separator = System.lineSeparator();
			newContent = newContent.replaceAll("\r\n", separator);

			LogConfigChangedEvent logConfigChangedEvent = new LogConfigChangedEvent(site.getName(), newContent,
					configFile.getAbsolutePath());

			if (!currentContent.equals(newContent)) {
				logConfigChangedEvent.perform(environment, site);
				if (formBean.clusterWide) {
					site.sendEvent(logConfigChangedEvent);
				}
				fieldProcessor.addOkMessage(request.getMessage(MessageConstants.LOGCONFIG_RELOADED));
			} else {
				fieldProcessor.addOkMessage(request.getMessage(MessageConstants.LOGCONFIG_NOCHANGE));
			}
		} catch (Exception e) {
			throw new ApplicationException(e);
		}
	}

	public static class LogFile {
		private String content;
		private boolean clusterWide = false;

		@NotNull
		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public boolean isClusterWide() {
			return clusterWide;
		}

		public void setClusterWide(boolean clusterWide) {
			this.clusterWide = clusterWide;
		}

	}

}
