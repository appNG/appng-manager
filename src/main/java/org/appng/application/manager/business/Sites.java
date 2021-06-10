/*
 * Copyright 2011-2021 the original author or authors.
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

import org.appng.api.ActionProvider;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.form.SiteForm;
import org.appng.application.manager.form.SubjectForm;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.SiteImpl;
import org.appng.core.service.InitializerService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides CRUD-operations for a {@link SiteImpl}, furthermore supports reloading a site.
 * 
 * @author Matthias Müller
 */

@Slf4j
@Component
public class Sites extends ServiceAware implements DataProvider, ActionProvider<SiteForm> {

	public static final String SITE = "site";

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			SiteForm siteForm, FieldProcessor fp) {
		String action = getAction(options);
		String errorMessage = null;
		String okMessage = null;
		Service service = getService();
		Integer siteId = options.getInteger(SITE, ID);

		try {
			if (ACTION_CREATE.equals(action)) {
				errorMessage = MessageConstants.SITE_CREATE_ERROR;
				service.createSite(request, siteForm, fp);
				okMessage = MessageConstants.SITE_CREATED;
			} else if (ACTION_UPDATE.equals(action)) {
				SiteImpl siteBean = siteForm.getSite();
				siteBean.setId(siteId);
				errorMessage = MessageConstants.SITE_UPDATE_ERROR;
				service.updateSite(request, siteForm, fp);
				okMessage = MessageConstants.SITE_UPDATED;
			} else if (ACTION_DELETE.equals(action)) {
				errorMessage = MessageConstants.SITE_DELETE_ERROR;
				String host = request.getHost();
				service.deleteSite(request, host, siteId, fp, site);
				okMessage = MessageConstants.SITE_DELETED;
			} else if (ACTION_RELOAD.equals(action)) {
				errorMessage = MessageConstants.SITE_RELOADED_ERROR;
				service.reloadSite(request, application, siteId, fp);
				okMessage = MessageConstants.SITE_RELOADED;
			} else if (ACTION_RELOAD_PLATFORM.equals(action)) {
				errorMessage = MessageConstants.PLATFORM_RELOAD_ERROR;
				reloadPlatform(site, application, request, fp);
				okMessage = MessageConstants.PLATFORM_RELOADED;
			}
			String message = request.getMessage(okMessage, siteId);
			fp.addOkMessage(message);
		} catch (BusinessException ex) {
			String message = request.getMessage(errorMessage, siteId);
			log.error("error during action '" + action + "': " + message, ex);
			fp.addErrorMessage(message);
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		environment.setAttribute(Scope.SESSION, "subjectForm", new SubjectForm());
		Service service = getService();
		Integer siteId = options.getInteger(SITE, ID);
		DataContainer data = null;
		if (null == siteId && ACTION_CREATE.equals(getAction(options))) {
			data = service.getNewSite(fp);
		} else {
			try {
				String name = options.getString(SITE, "siteName");
				String domain = options.getString(SITE, "siteDomain");
				data = service.searchSites(environment, fp, siteId, name, domain);
			} catch (BusinessException e) {
				String message = request.getMessage(e.getMessageKey(), e.getMessageArgs());
				log.error(message, e);
				fp.addErrorMessage(message);
			}
		}
		return data;
	}

	public void reloadPlatform(Site site, Application application, Request request, FieldProcessor fp)
			throws BusinessException {
		InitializerService initializerService = application.getBean(InitializerService.class);
		try {
			try {
				initializerService.reloadPlatform(new java.util.Properties(), request.getEnvironment(), site.getName(),
						null, null);
			} catch (InvalidConfigurationException e) {
				throw new BusinessException("Invalid configuration", e);
			}
		} catch (Exception e) {
			request.handleException(fp, e);
		}
	}
}
