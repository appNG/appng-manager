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
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

import org.appng.api.FieldProcessor;
import org.appng.api.Platform;
import org.appng.api.ProcessingException;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.PropertyForm;
import org.appng.application.manager.form.SiteForm;
import org.appng.application.manager.service.ManagerService;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.domain.SiteImpl;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.ContextConfiguration;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(inheritInitializers = false, initializers = SitesTest.class)
public class SitesTest extends AbstractTest {

	static {
		WritingXmlValidator.writeXml = false;
	}

	private static final String SITE_EVENT = "siteEvent";

	@Test
	public void testCreateSite01() throws Exception {
		propertyRepository.save(new PropertyImpl("platform." + Platform.Property.MESSAGING_ENABLED, null, "false"));

		SiteImpl siteToCreate = new SiteImpl();
		SiteForm siteForm = new SiteForm(siteToCreate);
		siteToCreate.setName("site1");
		siteToCreate.setHost("hostname1.domain.tld");
		siteToCreate.setHostAliases(new HashSet<>(Arrays.asList("alias1", "alias2", "alias3", "alias4-stays-unique")));
		siteToCreate.setDomain("https://hostname1.domain.tld");
		siteToCreate.setActive(true);

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
	public void testCreateSite02ValidationFail() throws Exception {
		SiteImpl siteToCreate = new SiteImpl();
		SiteForm siteForm = new SiteForm(siteToCreate);
		CallableAction callableAction = getAction(SITE_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(siteForm);
		callableAction.perform();
		validate(callableAction.getAction());
	}

	@Test
	public void testCreateSite03NameConflictFail() throws Exception {
		SiteImpl siteToCreate = new SiteImpl();
		SiteForm siteForm = new SiteForm(siteToCreate);
		siteToCreate.setName("site1");
		siteToCreate.setHost("other-hostname.domain.tld");
		siteToCreate.setDomain("https://other-hostname.domain.tld");
		siteToCreate.setActive(true);

		CallableAction callableAction = getAction(SITE_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(siteForm);
		callableAction.perform();
		validate(callableAction.getAction());
	}

	@Test
	public void testCreateSite041Host2HostConflictFail() throws Exception {
		SiteImpl siteH2HConflict = new SiteImpl();
		SiteForm siteFormH2HConflict = new SiteForm(siteH2HConflict);
		siteH2HConflict.setName("other-site");
		siteH2HConflict.setHost("hostname1.domain.tld");
		siteH2HConflict.setDomain("https://other-hostname.domain.tld");
		siteH2HConflict.setActive(true);

		CallableAction clbActionH2HConflict = getAction(SITE_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(siteFormH2HConflict);
		clbActionH2HConflict.perform();
		validate(clbActionH2HConflict.getAction());
	}

	@Test
	public void testCreateSite041Host2AliasConflictFail() throws Exception {
		SiteImpl siteH2AConflict = new SiteImpl();
		SiteForm siteFormH2AConflict = new SiteForm(siteH2AConflict);
		siteH2AConflict.setName("other-site");
		siteH2AConflict.setHost("alias1");
		siteH2AConflict.setDomain("https://other-hostname.domain.tld");
		siteH2AConflict.setActive(true);

		CallableAction clbActionH2AConflict = getAction(SITE_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(siteFormH2AConflict);
		clbActionH2AConflict.perform();
		validate(clbActionH2AConflict.getAction());
	}

	@Test
	public void testCreateSite051Alias2HostConflictFail() throws Exception {
		SiteImpl siteA2HConflict = new SiteImpl();
		SiteForm siteFormA2HConflict = new SiteForm(siteA2HConflict);
		siteA2HConflict.setName("other-site");
		siteA2HConflict.setHost("other-hostname.domain.tld");
		siteA2HConflict.setHostAliases(new HashSet<>(Arrays.asList("", "hostname1.domain.tld")));
		siteA2HConflict.setDomain("https://other-hostname.domain.tld");
		siteA2HConflict.setActive(true);

		CallableAction clbActionA2HConflict = getAction(SITE_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(siteFormA2HConflict);
		clbActionA2HConflict.perform();
		validate(clbActionA2HConflict.getAction());
	}

	@Test
	public void testCreateSite052Alias2AliasConflictFail() throws Exception {
		SiteImpl siteA2AConflict = new SiteImpl();
		SiteForm siteFormA2AConflict = new SiteForm(siteA2AConflict);
		siteA2AConflict.setName("other-site");
		siteA2AConflict.setHost("other-hostname.domain.tld");
		siteA2AConflict.setHostAliases(
				new HashSet<>(Arrays.asList("alias1", "alias2", "alias3"))
		);
		siteA2AConflict.setDomain("https://other-hostname.domain.tld");
		siteA2AConflict.setActive(true);

		CallableAction clbActionA2AConflict = getAction(SITE_EVENT, "create").withParam(FORM_ACTION, "create")
				.getCallableAction(siteFormA2AConflict);
		clbActionA2AConflict.perform();
		validate(clbActionA2AConflict.getAction());
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
		addParameter(ManagerService.FILTER_SITE_NAME, "site");
		addParameter(ManagerService.FILTER_SITE_DOMAIN, "example");
		addParameter(ManagerService.FILTER_SITE_ACTIVE, "true");
		initParameters();
		CallableDataSource siteDatasource = getDataSource("sites").getCallableDataSource();
		siteDatasource.perform("test");

		validate(siteDatasource.getDatasource());
	}

	private SiteForm getSiteForm(String name, String host, String domain, Set<String> aliases) {
		SiteImpl siteToCreate = new SiteImpl();
		siteToCreate.setName(name);
		siteToCreate.setHost(host);
		if (null != aliases) {
			siteToCreate.setHostAliases(aliases);
		}
		siteToCreate.setDomain(domain);
		siteToCreate.setActive(true);
		return new SiteForm(siteToCreate);
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
