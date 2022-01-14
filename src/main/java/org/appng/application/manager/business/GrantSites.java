/*
 * Copyright 2011-2022 the original author or authors.
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
import java.util.Set;

import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.form.GrantForm;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.SiteApplication;
import org.appng.xml.platform.Selection;
import org.springframework.stereotype.Component;

@Component
public class GrantSites extends ServiceAware implements ActionProvider<GrantForm>, DataProvider {

	private static final String APPLICATION_ID = "applicationId";
	private static final String SITE_ID = "siteId";
	private static final String IDS = "ids";

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fieldProcessor) {
		DataContainer dataContainer = new DataContainer(fieldProcessor);

		Integer siteId = options.getInteger(IDS, SITE_ID);
		Integer applicationId = options.getInteger(IDS, APPLICATION_ID);

		SiteApplication siteApplication = getService().getSiteApplication(siteId, applicationId);

		List<Selection> siteSelections = getService().getGrantedSelections(siteId, applicationId);

		GrantForm grantForm = new GrantForm(siteApplication);
		boolean showGrantedBy = siteSelections.get(1).getOptions().size() > 0;
		if (!showGrantedBy) {
			siteSelections.remove(1);
		}
		grantForm.setShowGrantedBy(showGrantedBy);

		dataContainer.getSelections().addAll(siteSelections);
		dataContainer.setItem(grantForm);
		return dataContainer;
	}

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			GrantForm grantForm, FieldProcessor fieldProcessor) {

		Integer siteId = options.getInteger(IDS, SITE_ID);
		Integer applicationId = options.getInteger(IDS, APPLICATION_ID);
		Set<Integer> grantedSiteIds = grantForm.getGrantedSiteIds();
		getService().grantSites(siteId, applicationId, grantedSiteIds);

	}

}
