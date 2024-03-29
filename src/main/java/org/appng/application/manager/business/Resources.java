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

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ActionProvider;
import org.appng.api.ApplicationException;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.form.ResourceForm;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * ApplicationResources action class reads all resources of a application
 * 
 * @author Matthias Herlitzius
 */

@Slf4j
@Component
public class Resources extends ServiceAware implements ActionProvider<ResourceForm>, DataProvider {

	private static final String TYPE = "type";
	private static final String RESOURCE = "resource";

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			ResourceForm form, FieldProcessor fp) {
		String action = getAction(options);
		String errorMessage = null;
		Service service = getService();
		Integer applicationId = options.getInteger(APPLICATION, ID);
		Integer resourceId = options.getInteger(RESOURCE, ID);
		String resourceName = null;
		try {
			if (ACTION_DELETE.equals(action)) {
				errorMessage = MessageConstants.RESOURCE_DELETE_ERROR;
				resourceName = service.deleteResource(request, applicationId, resourceId, fp);

			} else if (ACTION_UPDATE.equals(action)) {
				form.setId(resourceId);
				resourceName = service.updateResource(request, site, applicationId, form, fp);
			}

		} catch (BusinessException ex) {
			String message = request.getMessage(errorMessage, resourceName);
			log.error(message, ex);
			fp.addErrorMessage(message);
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Service service = getService();
		Integer applicationId = options.getInteger(APPLICATION, ID);

		String resourceType = options.getOptionValue(RESOURCE, TYPE);
		Integer resourceId = options.getInteger(RESOURCE, ID);
		ResourceType type = null;
		if (StringUtils.isNotBlank(resourceType)) {
			type = ResourceType.valueOf(resourceType);
		}

		try {
			return service.searchResources(request, site, fp, type, resourceId, applicationId);
		} catch (BusinessException e) {
			throw new ApplicationException("error while loading resources for application", e);
		}
	}
}
