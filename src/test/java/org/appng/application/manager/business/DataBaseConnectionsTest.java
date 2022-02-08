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

import org.appng.api.support.CallableDataSource;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.appng.xml.platform.Action;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "classpath:/beans-test.xml" })
public class DataBaseConnectionsTest extends AbstractTest {

	static {
		WritingXmlValidator.writeXml = false;
	}

	@Test
	public void testOverview() throws Exception {
		CallableDataSource connections = getDataSource("databaseConnections").getCallableDataSource();
		connections.perform("");
		validate(connections.getDatasource());
	}

	@Test
	public void testUpdate() throws Exception {
		Action action = getAction("databaseConnectionEvent", "updateConnection")
				.withParam(FORM_ACTION, "updateConnection").withParam("id", "1").initialize();
		validate(action);
	}

	@Test
	public void testMigrations() throws Exception {
		CallableDataSource connections = getDataSource("migrations").withParam("id", "1").getCallableDataSource();
		connections.perform("");
		validate(connections.getDatasource());
	}

}
