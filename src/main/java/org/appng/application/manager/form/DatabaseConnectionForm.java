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
