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

import java.io.IOException;

import org.appng.api.FieldProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.RoleImpl;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PermissionsTest extends AbstractTest {

	private static final String PERMISSION_EVENT = "permissionEvent";

	@Test
	public void testCreate() throws Exception {

		ApplicationImpl application = new ApplicationImpl();
		application.setName("application");
		applicationRepository.save(application);

		RoleImpl role1 = new RoleImpl();
		role1.setApplication(application);
		role1.setName("role1");
		roleRepository.save(role1);

		PermissionImpl p1 = new PermissionImpl();
		p1.setName("do.this");
		p1.setDescription("description");
		p1.setApplication(application);
		permissionRepository.save(p1);
		PermissionImpl p2 = new PermissionImpl();
		p2.setName("do.that");
		p2.setDescription("description");
		p2.setApplication(application);
		permissionRepository.save(p2);

		role1.getPermissions().add(p1);
		role1.getPermissions().add(p2);

		PermissionImpl permission = new PermissionImpl();
		permission.setName("do.something");

		permission.setDescription("foobar");
		CallableAction callableAction = getAction(PERMISSION_EVENT, "create-permission")
				.withParam(FORM_ACTION, "create-permission").withParam("appid", "1").getCallableAction(permission);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action");
		validate(fieldProcessor.getMessages(), "-messages");
	}

	@Test
	public void testCreateNameExists() throws Exception {
		PermissionImpl permission = new PermissionImpl();
		permission.setName("do.this");
		permission.setDescription("foobar");

		CallableAction callableAction = getAction(PERMISSION_EVENT, "create-permission")
				.withParam(FORM_ACTION, "create-permission").withParam("appid", "1").getCallableAction(permission);

		callableAction.perform();
		validate(callableAction.getAction());
	}

	@Test
	public void testDeletePermission() throws ProcessingException, IOException {
		CallableAction callableAction = getAction(PERMISSION_EVENT, "delete-permission").withParam("id", "3")
				.withParam("appid", "1").withParam(FORM_ACTION, "delete-permission").getCallableAction(null);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(fieldProcessor.getMessages());
	}

	@Test
	public void testShowPermission() throws Exception {
		CallableDataSource siteDatasource = getDataSource("permission").withParam("permissionid", "1")
				.getCallableDataSource();
		siteDatasource.perform("test");
		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testShowPermissions() throws Exception {
		addParameter("sortGroups", "name:desc");
		initParameters();
		CallableDataSource siteDatasource = getDataSource("permissions").withParam("appid", "1")
				.getCallableDataSource();
		siteDatasource.perform("test");
		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testUpdate() throws Exception {
		PermissionImpl form = new PermissionImpl();
		form.setName("do.somethingelse");
		form.setDescription("new description");
		CallableAction callableAction = getAction(PERMISSION_EVENT, "update-permission").withParam("permissionid", "1")
				.withParam("appid", "1").withParam(FORM_ACTION, "update-permission").getCallableAction(form);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action");
		validate(fieldProcessor.getMessages(), "-messages");
	}

}
