/*
 * Copyright 2011-2018 the original author or authors.
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

import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.DatabaseConnection;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Provides CRUD-operations for a {@link DatabaseConnection}.
 * 
 * @author Matthias Müller
 * 
 */

@Lazy
@Component
@Scope("request")
public class DatabaseConnections extends ServiceAware implements DataProvider, ActionProvider<DatabaseConnection> {

	private static final String ACTION_DELETE = "delete";
	private static final String ACTION_RESET = "reset";
	private static final String ACTION_TEST = "test";
	private static final String OPT_CONNECTION = "connection";
	private static final String OPT_SITE = "site";

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			DatabaseConnection databaseConnection, FieldProcessor fp) {
		String action = options.getOptionValue(ACTION, ID);
		Integer conId = request.convert(options.getOptionValue(OPT_CONNECTION, ID), Integer.class);
		if (ACTION_UPDATE.equals(action)) {
			databaseConnection.setId(conId);
			getService().updateDatabaseConnection(fp, databaseConnection);
		} else if (ACTION_DELETE.equals(action)) {
			getService().deleteDatabaseConnection(fp, conId);
		} else if (ACTION_TEST.equals(action)) {
			getService().testConnection(fp, conId);
		} else if (ACTION_RESET.equals(action)) {
			getService().resetConnection(conId);
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Integer dcId = request.convert(options.getOptionValue(ID, ID), Integer.class);
		Integer siteId = request.convert(options.getOptionValue(OPT_SITE, ID), Integer.class);
		DataContainer dataContainer = new DataContainer(fp);
		if (null == dcId) {
			Page<DatabaseConnection> connections = getService().getDatabaseConnections(siteId, fp);
			dataContainer.setPage(connections);
		} else {
			DatabaseConnection connection = getService().getDatabaseConnection(dcId, true);
			dataContainer.setItem(connection);
		}
		return dataContainer;
	}

}
