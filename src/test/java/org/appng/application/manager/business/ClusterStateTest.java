/*
 * Copyright 2011-2018 the original author or authors.
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

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Messaging;
import org.appng.api.support.CallableDataSource;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.junit.Test;
import org.mockito.Mockito;

public class ClusterStateTest extends AbstractTest {

	static {
		WritingXmlValidator.writeXml = false;
	}

	@Test
	public void testClusterState() throws Exception {
		environment.setAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG,
				Mockito.mock(org.appng.api.model.Properties.class));
		System.setProperty(Messaging.APPNG_NODE_ID, "node1");
		CallableDataSource siteDatasource = getDataSource("clusterState").getCallableDataSource();
		siteDatasource.perform("test");
		validate(siteDatasource.getDatasource().getConfig());
	}

}
