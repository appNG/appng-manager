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

import org.appng.api.ActionProvider;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.SelectionBuilder;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.SelectionType;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Matthias Herlitzius
 */

@Slf4j
@Component
public class Installation extends ServiceAware implements DataProvider, ActionProvider<Void> {

	public static final String REPOSITORY = "repository";
	private static final String PACKAGE_OPTION = "package";
	private static final String PACKAGE_FILTER = "packageFilter";
	private static final String PACKAGE_NAME = "packageName";
	private static final String PACKAGE_VERSION = "packageVersion";
	private static final String PACKAGE_TIMESTAMP = "packageTimestamp";

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			Void valueHolder, FieldProcessor fp) {
		String action = getAction(options);
		String errorMessage = null;
		String okMessage = null;
		Service service = getService();
		Integer repositoryId = options.getInteger(REPOSITORY, ID);
		String packageName = options.getString(PACKAGE_OPTION, PACKAGE_NAME);
		String packageVersion = options.getString(PACKAGE_OPTION, PACKAGE_VERSION);
		String packageTimestamp = options.getString(PACKAGE_OPTION, PACKAGE_TIMESTAMP);

		try {
			if (ACTION_INSTALL.equals(action)) {
				errorMessage = MessageConstants.PACKAGE_INSTALL_ERROR;
				service.installPackage(request, repositoryId, packageName, packageVersion, packageTimestamp, fp);
				okMessage = MessageConstants.PACKAGE_INSTALLED;
				String reloadMessage = request.getMessage(MessageConstants.RELOAD_SITE);
				fp.addNoticeMessage(reloadMessage);
			} else if (ACTION_DELETE_PACKAGE.equals(action)) {
				errorMessage = MessageConstants.PACKAGE_DELETE_ERROR;
				service.deletePackageVersion(request, repositoryId, packageName, packageVersion, packageTimestamp, fp);
				okMessage = MessageConstants.PACKAGE_DELETED;
			}
			String message = request.getMessage(okMessage, repositoryId, packageName, packageVersion);
			fp.addOkMessage(message);
		} catch (BusinessException ex) {
			String message = request.getMessage(errorMessage, repositoryId, packageName, packageVersion);
			log.error(message, ex);
			fp.addErrorMessage(message);
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Service service = getService();
		Integer repositoryId = options.getInteger(REPOSITORY, ID);
		String packageFilter = options.getString(REPOSITORY, PACKAGE_FILTER);
		String applicationName = options.getString(PACKAGE_OPTION, PACKAGE_NAME);
		DataContainer data = new DataContainer(fp);
		try {
			if (null != repositoryId && null == applicationName) {

				Selection packageFilterSelection = new SelectionBuilder<>("pf").defaultOption("pf", packageFilter)
						.title(MessageConstants.NAME).type(SelectionType.TEXT).select(packageFilter).build();
				SelectionGroup filter = new SelectionGroup();
				filter.getSelections().add(packageFilterSelection);
				data = service.searchInstallablePackages(request, fp, repositoryId, packageFilter);
				data.getSelectionGroups().add(filter);
			} else if (null != repositoryId && null != applicationName) {
				data = service.searchPackageVersions(request, fp, repositoryId, applicationName);
			}
		} catch (BusinessException ex) {
			data.setPage(new ArrayList<>(), fp.getPageable());
		}
		return data;
	}
}
