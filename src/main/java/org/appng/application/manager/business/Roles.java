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
import org.appng.application.manager.form.RoleForm;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides CRUD-operations for a {@link org.appng.core.domain.RoleImpl}.
 * 
 * @author Matthias Müller
 */

@Slf4j
@Component
public class Roles extends ServiceAware implements DataProvider, ActionProvider<RoleForm> {

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			RoleForm roleForm, FieldProcessor fp) {
		String action = getAction(options);

		String okMessage = null;
		Service service = getService();
		Integer applicationroleId = options.getInteger(ID, ID);

		try {
			if (ACTION_CREATE.equals(action)) {
				Integer applicationId = options.getInteger(APPLICATION, ID);
				service.createRole(request, roleForm, applicationId, fp);
				okMessage = MessageConstants.ROLE_CREATED;
			} else if (ACTION_UPDATE.equals(action)) {
				Integer roleId = options.getInteger(ID, ID);
				roleForm.getRole().setId(roleId);
				service.updateRole(request, roleForm, fp);
				okMessage = MessageConstants.ROLE_UPDATED;
			} else if (ACTION_DELETE.equals(action)) {
				service.deleteRole(applicationroleId);
				okMessage = MessageConstants.ROLE_DELETED;
			}
			String message = request.getMessage(okMessage, applicationroleId);
			fp.addOkMessage(message);
		} catch (BusinessException e) {
			log.error("error while performing action", e);
			request.addErrorMessage(fp, e);
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Service service = getService();
		Integer applicationRoleId = options.getInteger(ID, ID);
		Integer applicationId = options.getInteger(APPLICATION, ID);
		DataContainer data = null;
		if (null == applicationRoleId && ACTION_CREATE.equals(getAction(options))) {
			data = service.getNewRole(fp, applicationId);
		} else {
			try {
				data = service.searchRole(fp, applicationRoleId, applicationId);
			} catch (BusinessException ex) {
				String message = request.getMessage(ex.getMessageKey(), ex.getMessageArgs());
				log.error(message, ex);
				fp.addErrorMessage(message);
			}
		}
		return data;
	}
}
