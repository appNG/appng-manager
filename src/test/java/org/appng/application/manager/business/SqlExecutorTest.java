/*
 * Copyright 2011-2017 the original author or authors.
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

import org.appng.api.support.CallableAction;
import org.appng.testsupport.TestBase;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.ContextConfiguration;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(locations = { "classpath:/beans-test.xml", TestBase.TESTCONTEXT_CORE, TestBase.TESTCONTEXT_JPA })
public class SqlExecutorTest extends AbstractTest {

	@Test
	public void testExecute() throws Exception {

		String sql = "--comment\r\nselect id,name version from site;\r\nselect count(*) from application;\n--comment";
		CallableAction callableAction = getAction("databaseConnectionEvent", "executeSql")
				.withParam(FORM_ACTION, "executeSql").withParam("id", "1")
				.getCallableAction(new SqlExecutor.SqlStatement(sql, null, false));

		callableAction.perform();
		validate(callableAction.getAction());
	}

}