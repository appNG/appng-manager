/*
 * Copyright 2011-2017 the original author or authors.
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

import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Identifier;
import org.appng.api.model.Site;
import org.appng.application.manager.service.ServiceAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Provides CRUD-operations for a {@link Template}.
 * 
 * @author Matthias Müller
 * 
 */

@Lazy
@Component
@Scope("request")
public class Templates extends ServiceAware implements DataProvider, ActionProvider<Void> {

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			Void applicationBean, FieldProcessor fp) {
		if ("delete".equals(options.getOptionValue("template", "action"))) {
			getService().deleteTemplate(options.getOptionValue("template", "templateName"), fp);
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		List<Identifier> templates = getService().listTemplates();
		DataContainer dataContainer = new DataContainer(fp);
		dataContainer.setPage(templates, fp.getPageable());
		return dataContainer;
	}

}
