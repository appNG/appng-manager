/*
 * Copyright 2011-2018 the original author or authors.
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
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site;
import org.appng.api.support.SelectionFactory;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.form.UploadForm;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.Selection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Allows displaying and uploading resources for a {@link Application}.
 * 
 * @author Matthias Müller
 * 
 */

@Slf4j
@Lazy
@Component
@Scope("request")
public class Upload extends ServiceAware implements ActionProvider<UploadForm>, DataProvider {

	@Autowired
	SelectionFactory selectionFactory;

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		DataContainer d = new DataContainer(fp);
		UploadForm form = new UploadForm();
		d.setItem(form);
		ResourceType[] types = ResourceType.values();
		Selection selectionFromEnum = selectionFactory.fromEnum("type", MessageConstants.TYPE, types,
				new ArrayList<ResourceType>());
		selectionFromEnum.getOptions().add(0, new Option());
		d.getSelections().add(selectionFromEnum);
		return d;
	}

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			UploadForm form, FieldProcessor fp) {

		Service service = getService();
		Integer applicationId = request.convert(options.getOptionValue(APPLICATION, ID), Integer.class);

		try {
			service.createResource(site, applicationId, form, fp);
		} catch (BusinessException ex) {
			log.error("error while processing resource", ex);

		}
	}

}
