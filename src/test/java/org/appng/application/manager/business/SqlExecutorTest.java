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
package org.appng.application.manager.business;

import org.appng.api.support.CallableAction;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.internal.parser.Parser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "classpath:/beans-test.xml" })
public class SqlExecutorTest extends AbstractTest {

	@Test
	public void testExecute() throws Exception {

		String sql = "-- comment\r\nselect id,name version from site;\r\nselect /*inline comment*/ count(*) from application;\r\n-- comment\r\n";
		CallableAction callableAction = getAction("databaseConnectionEvent", "executeSql")
				.withParam(FORM_ACTION, "executeSql").withParam("id", "1")
				.getCallableAction(new SqlExecutor.SqlStatement(sql, null, false));

		callableAction.perform();
		validate(callableAction.getAction());
	}

	@Test
	public void testParser() throws ReflectiveOperationException {
		assertType("MySQLParser", "jdbc:mysql://localhost", DatabaseType.MYSQL);
		assertType("MariaDBParser", "jdbc:mariadb://localhost", DatabaseType.MYSQL);
		assertType("SQLServerParser", "jdbc:sqlserver://localhost", DatabaseType.MSSQL);
		assertType("PostgreSQLParser", "jdbc:postgresql://localhost", DatabaseType.POSTGRESQL);
		assertType("HSQLDBParser", "jdbc:hsqldb:hsql://localhost", DatabaseType.HSQL);
	}

	private void assertType(String className, String url, DatabaseType type) throws ReflectiveOperationException {
		ClassicConfiguration cfg = new ClassicConfiguration();
		cfg.setDataSource(url, null, null);
		Parser parser = new SqlExecutor().getParser(type, cfg);
		Assert.assertNotNull(parser);
		Assert.assertEquals(className, parser.getClass().getSimpleName());
	}

}
