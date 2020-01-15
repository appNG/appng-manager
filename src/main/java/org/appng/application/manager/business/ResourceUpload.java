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

import java.util.Arrays;

import org.appng.api.ActionProvider;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site;
import org.appng.api.support.SelectionBuilder;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.form.UploadForm;
import org.appng.application.manager.service.ServiceAware;
import org.appng.xml.platform.Selection;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Allows displaying and uploading a {@link Resource} for an {@link Application}.
 * 
 * @author Matthias Müller
 */

@Slf4j
@Component
public class ResourceUpload extends ServiceAware implements ActionProvider<UploadForm>, DataProvider {

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		DataContainer d = new DataContainer(fp);
		d.setItem(new UploadForm());

		Selection typeSelection = new SelectionBuilder<ResourceType>("type").title(MessageConstants.TYPE)
				.options(Arrays.asList(ResourceType.values())).defaultOption(null, null).build();
		d.getSelections().add(typeSelection);
		return d;
	}

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			UploadForm form, FieldProcessor fp) {
		try {
			Integer applicationId = options.getInteger(APPLICATION, ID);
			getService().createResource(request, site, applicationId, form, fp);
		} catch (BusinessException ex) {
			log.error("error while processing resource", ex);
		}
	}

}
