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
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.service.MigrationService.MigrationStatus;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides CRUD-operations for a {@link ApplicationImpl}.
 * 
 * @author Matthias Müller
 */

@Slf4j
@Component
public class Applications extends ServiceAware implements DataProvider, ActionProvider<ApplicationImpl> {

	private static final String FORM_ACTION = "form_action";
	private static final String DEACTIVATE_APPLICATION = "deactivateApplication";
	private static final String ACTIVATE_APPLICATION = "activateApplication";
	private static final String ACTION_CONFIGURE_APPLICATION = "configure-application";
	private static final String SITE = "site";

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			ApplicationImpl applicationBean, FieldProcessor fp) {
		String action = getAction(options);
		String errorMessage = null;
		String okMessage = null;
		Service service = getService();
		Integer applicationId = options.getInteger(APPLICATION, ID);
		try {
			if (ACTION_UPDATE.equals(action)) {
				applicationBean.setId(applicationId);
				errorMessage = MessageConstants.APPLICATION_UPDATE_ERROR;
				service.updateApplication(request, environment, applicationBean, fp);
				okMessage = MessageConstants.APPLICATION_UPDATED;
			} else if (ACTION_DELETE.equals(action)) {
				errorMessage = MessageConstants.APPLICATION_DELETE_ERROR;
				service.deleteApplication(request, applicationId, fp);
				okMessage = MessageConstants.APPLICATION_DELETED;
			} else if (ACTION_CONFIGURE_APPLICATION.equals(action)) {
				MigrationStatus migrationStatus = null;
				Integer siteId = options.getInteger(SITE, ID);
				String formAction = options.getOptionValue(ACTION, FORM_ACTION);
				if (ACTIVATE_APPLICATION.equals(formAction)) {
					errorMessage = MessageConstants.APPLICATION_UPDATE_ERROR;
					migrationStatus = service.assignApplicationToSite(request, siteId, applicationId, fp);
					okMessage = MessageConstants.APPLICATION_ACTIVATED;
				} else if (DEACTIVATE_APPLICATION.equals(formAction)) {
					errorMessage = MessageConstants.APPLICATION_UPDATE_ERROR;
					migrationStatus = service.removeApplicationFromSite(request, siteId, applicationId, fp);
					okMessage = MessageConstants.APPLICATION_DEACTIVATED;
				}
				if (migrationStatus.isErroneous()) {
					okMessage = null;
				} else {
					String reloadMessage = request.getMessage(MessageConstants.RELOAD_SITE);
					fp.addNoticeMessage(reloadMessage);
				}
			}
			if (null != okMessage) {
				String message = request.getMessage(okMessage, applicationId);
				fp.addOkMessage(message);
			}
		} catch (BusinessException ex) {
			String message = request.getMessage(errorMessage, applicationId);
			log.error(message, ex);
			fp.addErrorMessage(message);
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Service service = getService();
		Integer applicationId = options.getInteger(APPLICATION, ID);
		Integer siteId = options.getInteger(SITE, ID);

		DataContainer data = null;
		try {
			boolean assignedOnly = "select".equals(options.getOptionValue(SITE, "mode"));
			data = service.searchApplications(fp, siteId, applicationId, assignedOnly);
		} catch (BusinessException be) {
			String message = request.getMessage(be.getMessageKey(), be.getMessageArgs());
			log.error(message, be);
			fp.addErrorMessage(message);
		}
		return data;
	}

}
