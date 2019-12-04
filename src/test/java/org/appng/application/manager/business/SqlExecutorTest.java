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

import org.appng.api.support.CallableAction;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class SqlExecutorTest extends AbstractTest {

	@Test
	public void testExecute() throws Exception {

		DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:hsqldb:mem:hsql-testdb", "sa", "");
		Flyway fw = new FluentConfiguration().dataSource(dataSource).table("schema_version")
				.locations("db/migration/hsql").load();
		fw.migrate();

		String sql = "--comment\r\nselect id,name version from site;\r\n"
				+ "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC';\r\n" + "--comment\n";
		CallableAction callableAction = getAction("databaseConnectionEvent", "executeSql")
				.withParam(FORM_ACTION, "executeSql").withParam("id", "1")
				.getCallableAction(new SqlExecutor.SqlStatement(sql, null, false));

		callableAction.perform();
		validate(callableAction.getAction());
	}

}
