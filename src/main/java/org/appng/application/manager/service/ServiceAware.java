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
package org.appng.application.manager.service;

import org.appng.api.ActionProvider;
import org.appng.api.DataProvider;
import org.appng.api.Options;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract baseclass used by the application's {@link ActionProvider}s and {@link DataProvider}s.
 * 
 * @author Matthias Müller
 */
public abstract class ServiceAware {

	protected static final String ID = "id";
	protected static final String MODE = "mode";
	protected static final String NAME = "name";
	protected static final String ACTION = "action";
	protected static final String ACTION_DELETE = "delete";
	protected static final String ACTION_UPDATE = "update";
	protected static final String ACTION_CREATE = "create";
	protected static final String ACTION_ASSIGN = "assign";
	protected static final String ACTION_RELOAD_TEMPLATE = "reloadTemplate";
	protected static final String ACTION_RELOAD = "reload";
	protected static final String ACTION_RELOAD_PLATFORM = "reloadPlatform";
	protected static final String ACTION_ASSIGN_PERMISSIONS = "assignPermissions";
	protected static final String ACTION_ASSIGN_ROLES = "assignRoles";
	protected static final String ACTION_INSTALL = "install";
	protected static final String ACTION_DELETE_PACKAGE = "delete-package";
	protected static final String CHUNK = "chunk";
	protected static final String SORT = "sort";
	protected static final String ORDER = "order";
	protected static final String KEY = "key";
	protected static final String APPLICATION = "application";

	@Autowired
	private Service service;

	public String getAction(Options options) {
		return options.getOptionValue(ACTION, ID);
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

}
