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

import java.util.List;

import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;
import org.appng.api.support.CallableDataSource;
import org.appng.core.service.LdapService;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LdapUsersTest extends AbstractTest {

	static {
		WritingXmlValidator.writeXml = false;
	}

	@Test
	public void testUsers() throws Exception {
		CallableDataSource ldapUsers = getDataSource("ldapUsers").getCallableDataSource();
		ldapUsers.perform("");
		validate(ldapUsers.getDatasource());
	}

	@Test
	public void testSettings() throws Exception {
		CallableDataSource ldapUsers = getDataSource("ldapSettings").getCallableDataSource();
		ldapUsers.perform("");
		validate(ldapUsers.getDatasource());
	}

	@Override
	protected List<Property> getSiteProperties(String prefix) {
		List<Property> siteProperties = super.getSiteProperties(prefix);
		siteProperties.add(new SimpleProperty(prefix + LdapService.LDAP_START_TLS, "false"));
		siteProperties.add(new SimpleProperty(prefix + LdapService.LDAP_HOST, "ldap://localhost:389"));
		siteProperties.add(new SimpleProperty(prefix + LdapService.LDAP_PRINCIPAL_SCHEME, "DN"));
		return siteProperties;
	}

}
