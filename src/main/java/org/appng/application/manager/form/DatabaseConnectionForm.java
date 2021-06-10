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
package org.appng.application.manager.form;

import org.appng.core.domain.DatabaseConnection;
import org.flywaydb.core.api.MigrationInfoService;

import lombok.Getter;
import lombok.Setter;

public class DatabaseConnectionForm {

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
		return null == migrationInfoService || null == migrationInfoService.current() ? "UNKNOWN"
				: migrationInfoService.current().getState().name();
	}

}
