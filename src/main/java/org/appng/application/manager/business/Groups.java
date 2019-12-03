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
import org.appng.application.manager.form.GroupForm;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.GroupImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides CRUD-operations for a {@link GroupImpl}.
 * 
 * @author Matthias Müller
 */

@Slf4j
@Lazy
@Component
public class Groups extends ServiceAware implements ActionProvider<GroupForm>, DataProvider {

	private static final String GROUP = "group";
	
	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			GroupForm groupForm, FieldProcessor fp) {
		String action = getAction(options);
		String errorMessage = null;
		String okMessage = null;
		Service service = getService();
		Integer groupId = options.getInteger(GROUP, ID);
		try {
			if (ACTION_CREATE.equals(action)) {
				service.createGroup(request, groupForm, site, fp);
				okMessage = MessageConstants.GROUP_CREATED;
			} else if (ACTION_UPDATE.equals(action)) {
				groupForm.getGroup().setId(groupId);
				service.updateGroup(request, site, groupForm, fp);
				okMessage = MessageConstants.GROUP_UPDATED;
			} else if (ACTION_DELETE.equals(action)) {
				errorMessage = MessageConstants.GROUP_DELETE_ERROR;
				service.deleteGroup(request, groupId, fp);
				okMessage = MessageConstants.GROUP_DELETED;
			}
			String message = request.getMessage(okMessage, groupId);
			fp.addOkMessage(message);
		} catch (BusinessException ex) {
			if (null != errorMessage) {
				String message = request.getMessage(errorMessage, groupId);
				log.error(message, ex);
				fp.addErrorMessage(message);
			}
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Service service = getService();
		Integer groupId = options.getInteger(GROUP, ID);
		DataContainer data = null;
		if (null == groupId && ACTION_CREATE.equals(getAction(options))) {
			data = service.getNewGroup(site, fp);
		} else {
			try {
				String groupName = options.getString(GROUP, "groupName");
				data = service.searchGroups(fp, site, null, groupId, groupName);
			} catch (BusinessException ex) {
				String message = request.getMessage(ex.getMessageKey(), ex.getMessageArgs());
				log.error(message, ex);
				fp.addErrorMessage(message);
			}
		}
		return data;
	}
}
