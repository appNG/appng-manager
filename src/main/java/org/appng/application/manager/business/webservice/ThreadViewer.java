/*
 * Copyright 2011-2020 the original author or authors.
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

import java.util.Map;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.Request;
import org.appng.api.Webservice;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.springframework.stereotype.Component;

@Component
public class ThreadViewer implements Webservice {

	private static final String PERM_PLATFORM_THREADS = "platform.threads";

	public byte[] processRequest(Site site, Application application, Environment environment, Request request)
			throws BusinessException {

		String result = "";
		Subject subject = environment.getSubject();
		boolean hasPermission = request.getPermissionProcessor().hasPermission(PERM_PLATFORM_THREADS);
		if (subject != null && subject.isAuthenticated() && hasPermission) {
			String threadName = request.getParameter("name");
			String trace = request.getParameter("trace");
			result = getThreadDump(threadName, trace);
		}
		return result.getBytes();
	}

	String getThreadDump(String threadName, String trace) {
		StringBuilder result = new StringBuilder();
		Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
		for (Map.Entry<Thread, StackTraceElement[]> threadEntry : map.entrySet()) {
			Thread t = threadEntry.getKey();
			StringBuilder traceBuilder = new StringBuilder();
			if (null == threadName || threadName.equals(t.getName())) {
				traceBuilder.append("#" + t.getId() + " " + t.getName() + ":" + t.getState());
				traceBuilder.append(" (alive: " + t.isAlive() + ", ");
				traceBuilder.append("daemon: " + t.isDaemon() + ", ");
				traceBuilder.append("interrupted: " + t.isInterrupted());
				ClassLoader classLoader = t.getContextClassLoader();
				if (null != classLoader) {
					traceBuilder.append(
							", classloader: " + classLoader.getClass().getSimpleName() + "#" + classLoader.hashCode());
				}
				traceBuilder.append(")");
				traceBuilder.append(Constants.NEW_LINE);
				for (StackTraceElement element : threadEntry.getValue()) {
					traceBuilder.append("\t" + element);
					traceBuilder.append(Constants.NEW_LINE);
				}
				if (null == trace || traceBuilder.toString().contains(trace)) {
					result.append(traceBuilder.toString());
					result.append(Constants.NEW_LINE);
				}
			}
		}
		return result.toString();
	}

	public String getContentType() {
		return "text/plain";
	}

}
