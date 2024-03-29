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
import org.appng.api.model.Subject;
import org.appng.api.model.UserType;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.SubjectForm;
import org.appng.application.manager.service.ManagerService;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.security.BCryptPasswordHandler;
import org.appng.testsupport.validation.DifferenceHandler;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SubjectsTest extends AbstractTest {

	private static final String SUBJECT_EVENT = "subjectEvent";

	private DifferenceListener differenceListener;

	@Autowired
	private ManagerService managerService;

	public SubjectsTest() {
		// since different JVMs may return a different amount of Timezones, ignore content
		// of /data[1]/selection[4]/optionGroup
		differenceListener = new DifferenceHandler() {

			@Override
			public int differenceFound(Difference difference) {
				String xpathLocation = difference.getControlNodeDetail().getXpathLocation();
				if (null == xpathLocation) {
					xpathLocation = difference.getTestNodeDetail().getXpathLocation();
				}
				if (xpathLocation.contains("/data[1]/selection[5]/optionGroup")) {
					return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
				}
				return RETURN_ACCEPT_DIFFERENCE;
			}
		};
	}

	@Test
	public void testCreate() throws Exception {

		GroupImpl group1 = new GroupImpl();
		group1.setName("admins");
		groupRepository.save(group1);

		GroupImpl group2 = new GroupImpl();
		group2.setName("users");
		groupRepository.save(group2);

		SubjectImpl newSubject = getSubject();
		SubjectForm subjectForm = new SubjectForm(newSubject);
		String password = site.getPasswordPolicy().generatePassword();
		subjectForm.setPassword(password);
		subjectForm.setPasswordConfirmation(password);
		subjectForm.getGroupIds().add(1);
		subjectForm.getGroupIds().add(2);

		CallableAction callableAction = getAction(SUBJECT_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(subjectForm);

		FieldProcessor fieldProcessor = callableAction.perform();

		validate(callableAction.getAction(), "-action", differenceListener);
		validate(fieldProcessor.getMessages(), "-messages");

		SubjectImpl anotherSubject = getSubject();
		anotherSubject.setName("user");
		SubjectForm anotherForm = new SubjectForm(anotherSubject);
		anotherForm.setPassword(password);
		anotherForm.setPasswordConfirmation(password);
		anotherForm.getGroupIds().add(1);
		callableAction = getAction(SUBJECT_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(anotherForm);

		callableAction.perform();
	}

	@Test
	public void testCreateValidationFail() throws Exception {
		SubjectForm form = new SubjectForm();
		form.setPassword("§");
		form.setPasswordConfirmation("§");
		form.getSubject().setUserType(UserType.LOCAL_USER);
		CallableAction callableAction = getAction(SUBJECT_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(form);
		callableAction.perform();
		validate(callableAction.getAction(), differenceListener);
	}

	@Test
	public void testCreateNameExists() throws Exception {
		SubjectForm form = new SubjectForm(getSubject());
		String password = site.getPasswordPolicy().generatePassword();
		form.setPassword(password);
		form.setPasswordConfirmation(password);
		CallableAction callableAction = getAction(SUBJECT_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(form);
		callableAction.perform();
		validate(callableAction.getAction(), differenceListener);
	}

	@Test
	public void testDelete() throws ProcessingException, IOException {
		CallableAction callableAction = getAction(SUBJECT_EVENT, "delete").withParam("userId", "2")
				.withParam(FORM_ACTION, "delete").getCallableAction(null);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(fieldProcessor.getMessages(), differenceListener);
	}

	@Test
	public void testShowOne() throws Exception {
		CallableDataSource siteDatasource = getDataSource("user").withParam("userId", "1").getCallableDataSource();
		siteDatasource.perform("test");

		validate(siteDatasource.getDatasource(), differenceListener);
	}

	@Test
	public void testShowAll() throws Exception {

		addParameter("sortSubjects", "name:desc");
		initParameters();

		CallableDataSource siteDatasource = getDataSource("users").getCallableDataSource();
		siteDatasource.perform("test");

		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testShowAllFilterGroup() throws Exception {
		addParameter("f_gid", "1");
		initParameters();
		CallableDataSource siteDatasource = getDataSource("users").getCallableDataSource();
		siteDatasource.perform("test");
		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testUpdate() throws Exception {
		SubjectImpl s = getSubject();
		s.setRealname("Jane Doe");
		s.setTimeZone("Europe/Zurich");
		SubjectForm form = new SubjectForm(s);
		String password = site.getPasswordPolicy().generatePassword();
		form.setPassword(password);
		form.setPasswordConfirmation(password);
		CallableAction callableAction = getAction(SUBJECT_EVENT, "update").withParam("userId", "1")
				.withParam(FORM_ACTION, "update").getCallableAction(form);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action", differenceListener);
		validate(fieldProcessor.getMessages(), "-messages");

		Subject subjectByEmail = managerService.getSubjectByEmail(s.getEmail());
		boolean validPassword = new BCryptPasswordHandler(subjectByEmail).isValidPassword(password);
		Assert.assertTrue(validPassword);
	}

	private SubjectImpl getSubject() {
		SubjectImpl newSubject = new SubjectImpl();
		newSubject.setName("admin");
		newSubject.setEmail("foo@example.com");
		newSubject.setRealname("John Doe");
		newSubject.setUserType(UserType.LOCAL_USER);
		newSubject.setLanguage("en");
		newSubject.setTimeZone("Europe/Berlin");
		return newSubject;
	}

}
