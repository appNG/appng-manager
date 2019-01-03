/*
 * Copyright 2011-2019 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.appng.api.FieldProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.RepositoryForm;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.model.RepositoryMode;
import org.appng.core.model.RepositoryType;
import org.appng.testsupport.validation.XPathDifferenceHandler;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RepositoriesTest extends AbstractTest {

	private static final String REPO_PATH = new File("target").toPath().toUri().toString();
	private static final String REPO_EVENT = "repositoryEvent";

	@Test
	public void testCreateRepository() throws Exception {

		RepositoryForm repoForm = getRepository();
		CallableAction callableAction = getAction(REPO_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(repoForm);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action");
		validate(fieldProcessor.getMessages(), "-messages");
	}

	@Test
	public void testCreateRepositoryNameExists() throws Exception {
		RepositoryForm groupForm = getRepository();

		CallableAction callableAction = getAction(REPO_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(groupForm);

		callableAction.perform();
		validate(callableAction.getAction());
	}

	@Test
	public void testDeleteRepository() throws ProcessingException, IOException {
		createRepository();
		CallableAction callableAction = getAction(REPO_EVENT, "delete").withParam("repositoryid", "2")
				.withParam("form_action", "delete-repository").getCallableAction(null);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(fieldProcessor.getMessages());
	}

	@Test
	public void testShowRepository() throws Exception {
		CallableDataSource siteDatasource = getDataSource("repository").withParam("repositoryid", "1")
				.getCallableDataSource();
		siteDatasource.perform("test");
		
		XPathDifferenceHandler differenceListener = new XPathDifferenceHandler(false);
		differenceListener.ignoreDifference("/datasource[1]/data[1]/result[1]/field[4]/value[1]/text()[1]");
		differenceListener.ignoreDifference("/datasource[1]/data[1]/result[1]/field[5]/value[1]/text()[1]");
		validate(siteDatasource.getDatasource(), differenceListener);
	}

	@Test
	public void testShowRepositories() throws Exception {
		createRepository();
		addParameter("sortRepositories", "name:desc");
		initParameters();

		CallableDataSource siteDatasource = getDataSource("repositories").getCallableDataSource();
		siteDatasource.perform("test");

		XPathDifferenceHandler differenceListener = new XPathDifferenceHandler(false);
		differenceListener.ignoreDifference("/datasource[1]/data[1]/resultset[1]/result[1]/field[6]/value[1]/text()[1]");
		differenceListener.ignoreDifference("/datasource[1]/data[1]/resultset[1]/result[2]/field[6]/value[1]/text()[1]");
		validate(siteDatasource.getDatasource(), differenceListener);
	}

	@Test
	public void testUpdateRepository() throws Exception {
		RepositoryCacheFactory.init(null, null, null, null, false);
		RepositoryForm repoForm = getRepository();

		CallableAction callableAction = getAction(REPO_EVENT, "update").withParam("repositoryid", "1")
				.withParam(FORM_ACTION, "update").getCallableAction(repoForm);
		
		XPathDifferenceHandler differenceListener = new XPathDifferenceHandler(false);
		differenceListener.ignoreDifference("/action[1]/data[1]/result[1]/field[4]/value[1]/text()[1]");
		differenceListener.ignoreDifference("/action[1]/data[1]/result[1]/field[5]/value[1]/text()[1]");

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action", differenceListener);
		validate(fieldProcessor.getMessages(), "-messages");
	}

	private RepositoryForm getRepository() {
		RepositoryImpl repo = new RepositoryImpl();
		RepositoryForm repoForm = new RepositoryForm(repo);
		repo.setName("Local");
		repo.setDescription("local repo");
		repo.setRepositoryMode(RepositoryMode.ALL);
		repo.setRepositoryType(RepositoryType.LOCAL);
		try {
			repo.setUri(new URI(REPO_PATH));
		} catch (URISyntaxException e) {
			// ignore
		}
		return repoForm;
	}

	private void createRepository() {
		RepositoryImpl realRepository = new RepositoryImpl();
		realRepository.setName("Delete me");
		realRepository.setDescription("delete me");
		realRepository.setActive(true);
		realRepository.setRepositoryMode(RepositoryMode.ALL);
		realRepository.setRepositoryType(RepositoryType.LOCAL);
		try {
			realRepository.setUri(new URI(REPO_PATH));
		} catch (URISyntaxException e) {
			// ignore
		}
		repoRepository.save(realRepository);
	}
}
