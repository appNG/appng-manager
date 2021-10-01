/*
 * Copyright 2011-2021 the original author or authors.
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
import java.net.URISyntaxException;

import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.PermissionProcessor;
import org.appng.api.Request;
import org.appng.api.model.Subject;
import org.appng.application.manager.business.webservice.LogViewer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class LogViewerTest extends LogViewer {

	@Test
	public void test() throws BusinessException {
		Environment environment = Mockito.mock(Environment.class);
		Request request = Mockito.mock(Request.class);
		Subject subject = Mockito.mock(Subject.class);
		Mockito.when(environment.getSubject()).thenReturn(subject);
		Mockito.when(subject.isAuthenticated()).thenReturn(true);
		PermissionProcessor permissionProcessor = Mockito.mock(PermissionProcessor.class);
		Mockito.when(request.getPermissionProcessor()).thenReturn(permissionProcessor);
		Mockito.when(request.getParameter("lines")).thenReturn("2");
		Mockito.when(permissionProcessor.hasPermission(PERM_LOG_VIEWER)).thenReturn(true);
		byte[] processRequest = processRequest(null, null, environment, request);
		String result = new String(processRequest);
		Assert.assertEquals("log4j.appender.appng.File = ${webapp.root}/appNG.log", result);
	}

	@Override
	File getLogfile() {
		try {
			return new File(getClass().getClassLoader().getResource("log4j.properties").toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException("file not found", e);
		}
	}

}
