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
package org.appng.application.manager.business.webservice;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.Webservice;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A {@link Webservice} for displaying the current logfile.<br/>
 * Usage:<br/>
 * http://localhost:8080/service/appng/appng-manager/webservice/logViewer<br/>
 * limit lines:<br/>
 * http://localhost:8080/service/appng/appng-manager/webservice/logViewer?lines=100
 * 
 * @author Matthias Müller
 */

@Component
public class LogViewer implements Webservice {

	protected static final String PERM_LOG_VIEWER = "platform.logfile";
	private @Value("${loggingFile:WEB-INF/log/appNG.log}") String logPath;
	private @Value("${platform." + Platform.Property.PLATFORM_ROOT_PATH + "}") String rootPath;

	public byte[] processRequest(Site site, Application application, Environment environment, Request request)
			throws BusinessException {

		StringBuilder result = new StringBuilder();
		Subject subject = environment.getSubject();
		if (subject != null && subject.isAuthenticated()
				&& request.getPermissionProcessor().hasPermission(PERM_LOG_VIEWER)) {
			File logFile = getLogfile();
			int maxLines = 1000;
			String parameter = request.getParameter("lines");
			String find = request.getParameter("find");
			Pattern pattern = StringUtils.isBlank(find) ? null : Pattern.compile(find);
			if (null != parameter && parameter.matches("\\d+")) {
				maxLines = Integer.parseInt(parameter);
			}

			try {
				Iterator<String> lines = new FastAccessFile(logFile).tail(maxLines - 1);
				while (lines.hasNext()) {
					String line = lines.next();
					if (null == pattern || pattern.matcher(line).find()) {
						result.append(line);
						if (lines.hasNext()) {
							result.append(FastAccessFile.LF);
						}
					}
				}
			} catch (IOException e) {
				throw new BusinessException(e);
			}
		}
		return result.toString().getBytes();

	}

	File getLogfile() {
		return new File(rootPath, logPath);
	}

	public String getContentType() {
		return "text/plain";
	}

}
