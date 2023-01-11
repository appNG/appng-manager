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
package org.appng.application.manager.form;

import java.sql.Connection;
import java.sql.SQLException;

import org.appng.core.domain.DatabaseConnection;
import org.flywaydb.core.api.MigrationInfoService;

import lombok.Getter;
import lombok.Setter;

public class DatabaseConnectionForm {

	private static final String UNKNOWN = "UNKNOWN";
	@Getter
	@Setter
	private DatabaseConnection item;

	public DatabaseConnectionForm(DatabaseConnection item) {
		this.item = item;
	}

	public boolean isPendingMigrations() {
		MigrationInfoService migrationInfoService = item.getMigrationInfoService();
		return null == migrationInfoService ? false : migrationInfoService.pending().length > 0;
	}

	public String getState() {
		MigrationInfoService migrationInfoService = item.getMigrationInfoService();
		return null == migrationInfoService || null == migrationInfoService.current() ? UNKNOWN
				: migrationInfoService.current().getState().name();
	}

	public String getVersion() {
		MigrationInfoService migrationInfoService = item.getMigrationInfoService();
		return null == migrationInfoService || null == migrationInfoService.current() ? UNKNOWN
				: migrationInfoService.current().getVersion().getVersion();
	}

	public String getProductName() {
		try (Connection connection = item.getConnection()) {
			return connection.getMetaData().getDatabaseProductName();
		} catch (SQLException e) {
		}
		return UNKNOWN;
	}

	public String getProductVersion() {
		try (Connection connection = item.getConnection()) {
			return connection.getMetaData().getDatabaseProductVersion();
		} catch (SQLException e) {
		}
		return UNKNOWN;
	}

}
