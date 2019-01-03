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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.RandomStringUtils;
import org.appng.api.Platform;
import org.appng.api.SiteProperties;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.repository.ApplicationRepository;
import org.appng.core.repository.GroupRepository;
import org.appng.core.repository.PermissionRepository;
import org.appng.core.repository.RepoRepository;
import org.appng.core.repository.RoleRepository;
import org.appng.core.repository.SiteApplicationRepository;
import org.appng.core.repository.SiteRepository;
import org.appng.testsupport.TestBase;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.junit.Before;
import org.junit.Ignore;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@Ignore
@ContextConfiguration(locations = { TestBase.TESTCONTEXT_CORE, TestBase.TESTCONTEXT_JPA }, initializers = AbstractTest.class)
public class AbstractTest extends TestBase {

	@Autowired
	SiteRepository siteRepository;

	@Autowired
	ApplicationRepository applicationRepository;

	@Autowired
	SiteApplicationRepository siteApplicationRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	GroupRepository groupRepository;

	@Autowired
	RepoRepository repoRepository;

	@Autowired
	PermissionRepository permissionRepository;

	@Autowired
	@Qualifier("entityManager")
	EntityManager em;

	static {
		WritingXmlValidator.writeXml = false;
	}

	protected AbstractTest() {
		super("appng-manager", APPLICATION_HOME);
		setUseFullClassname(false);
		setEntityPackage("org.appng.core.domain");
		setRepositoryBase("org.appng.core.repository");
	}

	@Before
	public void setSiteId() {
		((DefaultEnvironment) environment).setSubject(subject);
		Mockito.when(subject.getId()).thenReturn(42);
		Mockito.when(site.getId()).thenReturn(1);
		Mockito.when(site.getPasswordPolicy()).thenReturn(new PasswordPolicy() {

			public boolean isValidPassword(char[] password) {
				return true;
			}

			public String getErrorMessageKey() {
				return null;
			}

			public String generatePassword() {
				return RandomStringUtils.random(6);
			}
		});
	}

	@Override
	protected Properties getProperties() {
		Properties properties = super.getProperties();
		properties.put("hibernate.show_sql", "false");
		properties.put("hibernate.format_sql", "false");
		return properties;
	}

	@Override
	protected List<Property> getPlatformProperties(String prefix) {
		List<Property> platformProperties = super.getPlatformProperties(prefix);
		SimpleProperty templates = new SimpleProperty(prefix + Platform.Property.TEMPLATE_FOLDER, "templates");
		platformProperties.add(templates);
		return platformProperties;
	}

	@Override
	protected List<Property> getSiteProperties(String prefix) {
		List<Property> siteProperties = new ArrayList<Property>();
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.SERVICE_PATH, "services"));
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.MANAGER_PATH, "ws"));
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.DEFAULT_PAGE_SIZE, "10"));
		return siteProperties;
	}

}
