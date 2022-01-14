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
package org.appng.application.manager.business;

import java.io.IOException;

import org.appng.api.FieldProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.GroupForm;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteImpl;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GroupsTest extends AbstractTest {

	private static final String GROUP_EVENT = "groupEvent";

	static {
		WritingXmlValidator.writeXml = false;
	}

	@Test
	public void testCreateGroup() throws Exception {

		ApplicationImpl application = new ApplicationImpl();
		application.setName("application");
		applicationRepository.save(application);

		RoleImpl role1 = new RoleImpl();
		role1.setApplication(application);
		role1.setName("role1");
		roleRepository.save(role1);

		RoleImpl role2 = new RoleImpl();
		role2.setApplication(application);
		role2.setName("role2");
		roleRepository.save(role2);

		SiteImpl site = new SiteImpl();
		site.setName("localhost");
		site.setHost("localhost");
		site.setDomain("localhost");
		siteRepository.save(site);

		SiteApplication siteApplication = new SiteApplication(site, application);
		siteApplicationRepository.save(siteApplication);
		site.getSiteApplications().add(siteApplication);

		GroupForm groupForm = getAdminGroup();
		groupForm.getRoleIds().add(1);
		CallableAction callableAction = getAction(GROUP_EVENT, "createGroup").withParam(FORM_ACTION, "createGroup")
				.getCallableAction(groupForm);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action");
		validate(fieldProcessor.getMessages(), "-messages");
	}

	@Test
	public void testCreateGroupNameExists() throws Exception {
		GroupForm groupForm = getAdminGroup();

		CallableAction callableAction = getAction(GROUP_EVENT, "createGroup").withParam(FORM_ACTION, "createGroup")
				.getCallableAction(groupForm);

		callableAction.perform();
		validate(callableAction.getAction());
	}

	@Test
	public void testDeleteGroup() throws ProcessingException, IOException {
		createGroup();
		CallableAction callableAction = getAction(GROUP_EVENT, "deleteGroup").withParam("groupid", "2")
				.withParam("form_action", "deleteGroup").getCallableAction(null);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(fieldProcessor.getMessages());
	}

	@Test
	public void testShowGroup() throws Exception {
		CallableDataSource groupDatasource = getDataSource("group").withParam("groupid", "1").getCallableDataSource();
		groupDatasource.perform("test");

		validate(groupDatasource.getDatasource());
	}

	@Test
	public void testShowGroups() throws Exception {
		createGroup();
		// create default admin group to validate that it does not has an delete action
		createDefaultAdminGroup();

		addParameter("sortGroups", "name:desc");
		initParameters();

		CallableDataSource groupDatasource = getDataSource("groups").getCallableDataSource();
		groupDatasource.perform("test");

		validate(groupDatasource.getDatasource());
	}

	@Test
	public void testShowGroupsFilterName() throws Exception {
		CallableDataSource groupDatasource = getDataSource("groups").withParam("groupName", "admin")
				.getCallableDataSource();
		groupDatasource.perform("test");

		validate(groupDatasource.getDatasource());
	}

	@Test
	public void testUpdateAdminGroup() throws Exception {
		GroupForm groupForm = getAdminGroup();

		CallableAction callableAction = getAction(GROUP_EVENT, "updateGroup").withParam("groupid", "4")
				.withParam(FORM_ACTION, "updateGroup").getCallableAction(groupForm);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action");
		validate(fieldProcessor.getMessages(), "-messages");
	}

	@Test
	public void testUpdateGroup() throws Exception {
		GroupForm groupForm = getAdminGroup();

		CallableAction callableAction = getAction(GROUP_EVENT, "updateGroup").withParam("groupid", "1")
				.withParam(FORM_ACTION, "updateGroup").getCallableAction(groupForm);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action");
		validate(fieldProcessor.getMessages(), "-messages");
	}

	private GroupForm getAdminGroup() {
		GroupImpl group = new GroupImpl();
		GroupForm groupForm = new GroupForm(group);
		group.setName("admin");
		group.setDescription("admins");
		return groupForm;
	}

	private void createGroup() {
		GroupImpl realGroup = new GroupImpl();
		realGroup.setName("users");
		realGroup.setDescription("all the users");
		groupRepository.save(realGroup);
	}

	private void createDefaultAdminGroup() {
		GroupImpl realGroup = new GroupImpl();
		realGroup.setName("Administrators");
		realGroup.setDescription("appNG Administrators Group");
		realGroup.setDefaultAdmin(true);
		groupRepository.save(realGroup);
	}

}
