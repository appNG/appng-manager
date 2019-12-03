/*
 * Copyright 2011-2019 the original author or authors.
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

import java.util.Properties;

import org.appng.api.support.CallableAction;
import org.appng.testsupport.TestBase;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = ManagerTestConfig.class, initializers = SqlExecutorTest.class)
public class SqlExecutorTest extends TestBase {

	public SqlExecutorTest() {
		super("appng-manager", APPLICATION_HOME);
		setUseFullClassname(false);
		setEntityPackage("org.appng.core.domain");
		setRepositoryBase("org.appng.core.repository");
	}
	
	@Test
	public void testExecute() throws Exception {
		String sql = "--comment\r\nselect id,name version from site;\r\nselect count(*) from application;\n--comment\n";
		CallableAction callableAction = getAction("databaseConnectionEvent", "executeSql")
				.withParam(FORM_ACTION, "executeSql").withParam("id", "1")
				.getCallableAction(new SqlExecutor.SqlStatement(sql, null, false));

		callableAction.perform();
		WritingXmlValidator.writeXml = true;
		validate(callableAction.getAction());
	}

	@Override
	protected Properties getProperties() {
		Properties properties = super.getProperties();
		properties.put("createDatabase", "true");
		return properties;
	}
}
