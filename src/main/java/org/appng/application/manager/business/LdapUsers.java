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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;
import org.appng.api.model.Site;
import org.appng.api.model.UserType;
import org.appng.application.manager.MessageConstants;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.service.CoreService;
import org.appng.core.service.LdapService;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;

@Component
public class LdapUsers implements DataProvider {

	private CoreService coreService;

	private LdapUsers(CoreService coreService) {
		this.coreService = coreService;
	}

	@Override
	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {

		DataContainer dataContainer = new DataContainer(fp);
		String mode = options.getString("mode", "value");
		LdapService ldapService = new LdapService();

		Properties siteProps = site.getProperties();
		if ("settings".equals(mode)) {
			List<Property> ldapProps = new ArrayList<>();
			SimpleProperty ldapHost = getProperty(siteProps, LdapService.LDAP_HOST);
			ldapProps.add(ldapHost);
			ldapProps.add(getProperty(siteProps, LdapService.LDAP_DOMAIN));
			ldapProps.add(getProperty(siteProps, LdapService.LDAP_GROUP_BASE_DN));
			SimpleProperty ldapUser = getProperty(siteProps, LdapService.LDAP_USER);
			ldapProps.add(ldapUser);
			ldapProps.add(getProperty(siteProps, LdapService.LDAP_ID_ATTRIBUTE));
			ldapProps.add(getProperty(siteProps, LdapService.LDAP_PRINCIPAL_SCHEME));
			ldapProps.add(getProperty(siteProps, LdapService.LDAP_START_TLS));
			dataContainer.setItems(ldapProps);
			char[] ldapPw = siteProps.getString(LdapService.LDAP_PASSWORD, StringUtils.EMPTY).toCharArray();
			boolean adminLoginOk = ldapService.loginUser(site, ldapUser.getString(), ldapPw);
			if (!adminLoginOk) {
				fp.addErrorMessage(request.getMessage(MessageConstants.LDAP_NOT_WORKING, ldapHost.getString()));
			}
		} else {

			List<LdapUser> users = new ArrayList<>();

			List<SubjectImpl> globalGroups = coreService.getSubjectsByType(UserType.GLOBAL_GROUP);
			for (SubjectImpl globalGroup : globalGroups) {
				List<SubjectImpl> membersOfGroup = ldapService.getMembersOfGroup(site, globalGroup.getName());
				users.addAll(membersOfGroup.stream().map(s -> new LdapUser(s.getName(), s.getEmail(), s.getRealname(),
						UserType.GLOBAL_GROUP, globalGroup.getName())).collect(Collectors.toList()));
			}
			List<SubjectImpl> globalUsers = coreService.getSubjectsByType(UserType.GLOBAL_USER);
			String userDn = siteProps.getString(LdapService.LDAP_ID_ATTRIBUTE) + "=%s,"
					+ siteProps.getString(LdapService.LDAP_USER_BASE_DN);
			users.addAll(globalUsers.stream().map(s -> {
				return new LdapUser(s.getName(), s.getEmail(), s.getRealname(), s.getUserType(),
						String.format(userDn, s.getName()));
			}).collect(Collectors.toList()));

			dataContainer.setPage(users, fp.getPageable(), true);
		}
		return dataContainer;
	}

	private SimpleProperty getProperty(Properties siteProps, String propName) {
		SimpleProperty prop = new SimpleProperty(propName, siteProps.getString(propName));
		prop.determineType();
		return prop;
	}

	@Data
	@AllArgsConstructor
	public class LdapUser {
		private String name;
		private String email;
		private String realname;
		private UserType type;
		private String groupOrDn;
	}

}
