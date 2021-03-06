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
package org.appng.application.manager.business;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.business.SqlExecutor.SqlStatement;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.DatabaseConnection;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.internal.database.DatabaseFactory;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.resource.StringResource;
import org.flywaydb.core.internal.sqlscript.SqlScript;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Component;

@Component
public class SqlExecutor extends ServiceAware implements DataProvider, ActionProvider<SqlStatement> {

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fieldProcessor) {
		DataContainer dataContainer = new DataContainer(fieldProcessor);
		Map<String, String> sessionParams = application.getSessionParams(site, environment);
		String dcId = options.getOptionValue("connection", "id");
		SqlStatement statement = new SqlStatement(sessionParams.get("sql" + dcId),
				sessionParams.remove("result" + dcId), false);
		dataContainer.setItem(statement);
		return dataContainer;
	}

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			SqlStatement formBean, FieldProcessor fieldProcessor) {
		Integer dcId = options.getInteger("connection", "id");
		String sql = formBean.getContent();
		Map<String, String> sessionParams = application.getSessionParams(site, environment);
		sessionParams.put("sql" + dcId, sql);
		DatabaseConnection databaseConnection = getService().getDatabaseConnection(dcId, false);
		DataSource dataSource = new SingleConnectionDataSource(databaseConnection.getJdbcUrl(),
				databaseConnection.getUserName(), new String(databaseConnection.getPassword()), true);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		List<String> queries = getQueries(sql, databaseConnection);
		StringBuilder results = new StringBuilder();
		for (String query : queries) {
			SqlStatement statementResult = processSingleStatement(query, jdbcTemplate);
			results.append("<div style='background:#F0F0F0;border:1px solid grey'>");
			results.append(statementResult.getContent());
			results.append("</div>");
			results.append(statementResult.getResult());
			results.append("<p/>");
		}
		String result = results.toString();
		sessionParams.put("result" + dcId, result);
	}

	public SqlStatement processSingleStatement(String sql, JdbcTemplate jdbcTemplate) {
		String sqlResult;
		boolean hasError = false;
		try {
			sqlResult = jdbcTemplate.execute(sql, (PreparedStatementCallback<String>) (ps -> {
				String result = "";
				ps.execute();
				int up = ps.getUpdateCount();
				ResultSet rs = ps.getResultSet();
				if (null != rs) {
					result = buildResultSetTable(new ResultSetWrappingSqlRowSet(rs));
				} else if (up > -1) {
					result = "<div>" + up + " row(s) affected</div>";
				}
				return result;
			}));
		} catch (DataAccessException e) {
			sqlResult = "<div style='border:1px solid red'>" + e.getCause().getMessage() + "</div>";
			hasError = true;
		}
		return new SqlStatement(sql, sqlResult, hasError);
	}

	public String buildResultSetTable(SqlRowSet rowSet) {
		StringBuilder sb = new StringBuilder();
		int rows = 0;
		if (null != rowSet) {
			SqlRowSetMetaData metaData = rowSet.getMetaData();
			sb.append("<table border='1px solid grey' cellpadding='0' cellspacing='0'>");
			sb.append("<tr>");
			for (String col : metaData.getColumnNames()) {
				sb.append("<th>" + col + "</th>");
			}
			sb.append("</tr>");

			int columnCount = metaData.getColumnCount();
			while (rowSet.next()) {
				rows++;
				sb.append("<tr style='vertical-align:top'>");
				for (int i = 1; i <= columnCount; i++) {
					sb.append("<td>" + rowSet.getObject(i) + "</td>");
				}
				sb.append("</tr>");
			}
			sb.append("</table>");
		}
		sb.append("<div>" + rows + " row(s) selected</div>");
		return sb.toString();
	}

	public static class SqlStatement {
		private String content;
		private String result;
		private boolean hasError;

		public SqlStatement() {

		}

		public SqlStatement(String content, String result, boolean hasError) {
			this.content = content;
			this.result = result;
			this.hasError = hasError;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getResult() {
			return result;
		}

		public void setResult(String result) {
			this.result = result;
		}

		public boolean isHasError() {
			return hasError;
		}

		public void setHasError(boolean hasError) {
			this.hasError = hasError;
		}

	}

	public List<String> getQueries(String sql, DatabaseConnection connection) {
		List<String> queries = new ArrayList<>();
		try (Database<?> db = DatabaseFactory
				.createDatabase(new FluentConfiguration().dataSource(connection.getDataSource()), true)) {
			SqlScript sqlScript = new SqlScript(db.createSqlStatementBuilderFactory(), new StringResource(sql), false);
			sqlScript.getSqlStatements().forEach(query -> queries.add(query.getSql()));
		}
		return queries;
	}

}
