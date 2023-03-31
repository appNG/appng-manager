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
package org.appng.application.manager.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.service.PlatformEventService;
import org.appng.application.manager.service.RoleService;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.DatabaseConnection.DatabaseType;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.service.CoreService;
import org.appng.core.service.DatabaseService;
import org.appng.mail.MailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

public class LogDatabaseSize extends ReportJobBase {

	private DatabaseService databaseService;
	private CoreService coreService;

	@Autowired
	LogDatabaseSize(PlatformEventService service, MailTransport mailTransport, MessageSource messageSource,
			RoleService roleService, CoreService coreService, DatabaseService databaseService) {
		super(service, mailTransport, messageSource, roleService);
		this.databaseService = databaseService;
		this.coreService = coreService;
	}

	public void execute(Site site, Application application) throws Exception {
		List<DatabaseType> databaseTypes = new ArrayList<>(Arrays.asList(DatabaseType.values()));
		databaseTypes.remove(DatabaseType.HSQL);
		for (DatabaseType databaseType : databaseTypes) {
			DatabaseConnection rootConnection = databaseService.getRootConnectionOfType(databaseType);
			if (null != rootConnection && rootConnection.testConnection(true)) {
				Object[] messageArgs = new Object[] { databaseType.name(), rootConnection.getJdbcUrl(),
						rootConnection.getDatabaseSize() };
				String message = messageSource.getMessage(MessageConstants.EVENT_DATABASE_ROOT_SIZE, messageArgs,
						Locale.ENGLISH);
				coreService.createEvent(Type.INFO, message);
			}
		}

		for (Site s : coreService.getSites()) {
			List<DatabaseConnection> dbcs = coreService.getDatabaseConnectionsForSite(s.getId());
			for (DatabaseConnection dbc : dbcs) {
				if (dbc.testConnection(true)) {
					Double databaseSize = dbc.getDatabaseSize();
					Object[] messageArgs = new Object[] { dbc.getType().name(), s.getName(), dbc.getJdbcUrl(),
							databaseSize };
					String message = messageSource.getMessage(MessageConstants.EVENT_DATABASE_SIZE, messageArgs,
							Locale.ENGLISH);
					coreService.createEvent(Type.INFO, message);
				} else {
					Object[] messageArgs = new Object[] { dbc.getType().name(), s.getName(), dbc.getJdbcUrl() };
					String message = messageSource.getMessage(MessageConstants.EVENT_DATABASE_NOTREACHABLE, messageArgs,
							Locale.ENGLISH);
					coreService.createEvent(Type.WARN, message);
				}
			}
		}

	}

}
