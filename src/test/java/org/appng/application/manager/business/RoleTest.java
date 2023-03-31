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
import org.appng.application.manager.form.RoleForm;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.RoleImpl;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RoleTest extends AbstractTest {

	private static final String ROLE_EVENT = "roleEvent";
	private static final String CREATE_ROLE = "create-role";

	@Test
	public void testCreate() throws Exception {

		ApplicationImpl application = new ApplicationImpl();
		application.setName("application");
		applicationRepository.save(application);

		RoleImpl role1 = new RoleImpl();
		role1.setApplication(application);
		role1.setName("role1");

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
		roleRepository.save(role1);

		RoleImpl role = new RoleImpl();
		role.setName("role2");
		RoleForm roleForm = new RoleForm(role);
		roleForm.getPermissionIds().add(1);
		roleForm.getPermissionIds().add(2);
		CallableAction callableAction = getAction(ROLE_EVENT, CREATE_ROLE).withParam(FORM_ACTION, CREATE_ROLE)
				.withParam("appid", "1").getCallableAction(roleForm);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action");
		validate(fieldProcessor.getMessages(), "-messages");
	}

	@Test
	public void testCreateNameExists() throws Exception {
		RoleImpl role = new RoleImpl();
		role.setName("role2");
		RoleForm roleForm = new RoleForm(role);

		CallableAction callableAction = getAction(ROLE_EVENT, CREATE_ROLE).withParam(FORM_ACTION, CREATE_ROLE)
				.withParam("appid", "1").getCallableAction(roleForm);

		callableAction.perform();
		validate(callableAction.getAction());
	}

	@Test
	public void testDelete() throws ProcessingException, IOException {
		CallableAction callableAction = getAction(ROLE_EVENT, "delete-role").withParam("roleid", "2")
				.withParam("appid", "1").withParam(FORM_ACTION, "delete-role").getCallableAction(null);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(fieldProcessor.getMessages());
	}

	@Test
	public void testShowOne() throws Exception {
		CallableDataSource siteDatasource = getDataSource("role").withParam("id", "1").getCallableDataSource();
		siteDatasource.perform("test");
		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testShowAll() throws Exception {
		addParameter("sortRoles", "name:asc");
		initParameters();
		CallableDataSource siteDatasource = getDataSource("roles").withParam("appid", "1").getCallableDataSource();
		siteDatasource.perform("test");
		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testUpdate() throws Exception {
		RoleImpl role = new RoleImpl();
		role.setName("role.updated");
		role.setDescription("new description");
		RoleForm form = new RoleForm(role);
		form.getPermissionIds().add(1);
		CallableAction callableAction = getAction(ROLE_EVENT, "update-role").withParam("roleid", "1")
				.withParam("appid", "1").withParam(FORM_ACTION, "update-role").getCallableAction(form);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action");
		validate(fieldProcessor.getMessages(), "-messages");
	}

}
