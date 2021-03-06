/*
 * Copyright 2011-2020 the original author or authors.
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
import org.appng.api.Platform;
import org.appng.api.ProcessingException;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.PropertyForm;
import org.appng.application.manager.form.SiteForm;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.domain.SiteImpl;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SitesTest extends AbstractTest {

	private static final String SITE_EVENT = "siteEvent";

	@Test
	public void testCreateSite() throws Exception {
		propertyRepository.save(new PropertyImpl("platform." + Platform.Property.MESSAGING_ENABLED, null, "false"));
		
		SiteImpl siteToCreate = new SiteImpl();
		SiteForm siteForm = new SiteForm(siteToCreate);
		siteToCreate.setName("localhost");
		siteToCreate.setHost("localhost");
		siteToCreate.setDomain("localhost");
		siteToCreate.setActive(true);

		//prepares using appNG >= 1.19.1
		PropertyForm form = new PropertyForm();
		form.getProperty().setName(Platform.Property.MESSAGING_ENABLED);
		form.getProperty().setDefaultString(Boolean.FALSE.toString());
		getAction("propertyEvent", "create-platform-property").withParam(FORM_ACTION, "create-platform-property")
				.getCallableAction(form).perform();

		CallableAction callableAction = getAction(SITE_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(siteForm);

		FieldProcessor fieldProcessor = callableAction.perform();
		validate(callableAction.getAction(), "-action");
		validate(fieldProcessor.getMessages(), "-messages");
	}

	@Test
	public void testCreateSiteValidationFail() throws Exception {
		SiteImpl siteToCreate = new SiteImpl();
		SiteForm siteForm = new SiteForm(siteToCreate);
		CallableAction callableAction = getAction(SITE_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(siteForm);
		callableAction.perform();
		validate(callableAction.getAction());
	}

	@Test
	public void testDeleteSite() throws ProcessingException, IOException {
		createSite();
		CallableAction callableAction = getAction(SITE_EVENT, "delete").withParam(FORM_ACTION, "delete")
				.withParam("siteid", "2").getCallableAction(null);
		FieldProcessor fp = callableAction.perform();
		validate(fp.getMessages());
	}

	@Test
	public void testReloadSite() throws Exception {
		CallableAction callableAction = getAction(SITE_EVENT, "reload").withParam(FORM_ACTION, "reload")
				.withParam("siteid", "1").getCallableAction(null);
		FieldProcessor fp = callableAction.perform();
		validate(fp.getMessages());
	}

	@Test
	public void testShowSite() throws Exception {
		CallableDataSource siteDatasource = getDataSource("site").withParam("siteid", "1").getCallableDataSource();
		siteDatasource.perform("test");
		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testShowSites() throws Exception {
		createSite();

		CallableDataSource siteDatasource = getDataSource("sites").getCallableDataSource();
		siteDatasource.perform("test");

		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testShowSitesFiltered() throws Exception {
		CallableDataSource siteDatasource = getDataSource("sites").withParam("name", "site")
				.withParam("domain", "example").getCallableDataSource();
		siteDatasource.perform("test");

		validate(siteDatasource.getDatasource());
	}

	private void createSite() {
		SiteImpl realSite = new SiteImpl();
		realSite.setName("site2");
		realSite.setHost("example.com");
		realSite.setDomain("example.com");
		realSite.setDescription("a description");
		realSite.setActive(true);
		siteRepository.save(realSite);
	}

}
