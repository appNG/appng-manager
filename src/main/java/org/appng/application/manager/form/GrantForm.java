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
package org.appng.application.manager.form;

import java.util.HashSet;
import java.util.Set;

import org.appng.api.model.Site;
import org.appng.core.domain.SiteApplication;

public class GrantForm {

	private SiteApplication siteApplication;
	private Set<Integer> grantedSiteIds;
	private boolean showGrantedBy = false;

	public GrantForm() {
		this.grantedSiteIds = new HashSet<>();
	}

	public GrantForm(SiteApplication siteApplication) {
		this();
		this.siteApplication = siteApplication;
		Set<Site> grantedSites = siteApplication.getGrantedSites();
		if (null != grantedSites) {
			for (Site site : grantedSites) {
				grantedSiteIds.add(site.getId());
			}
		}
	}

	public SiteApplication getSiteApplication() {
		return siteApplication;
	}

	public void setSiteApplication(SiteApplication siteApplication) {
		this.siteApplication = siteApplication;
	}

	public Set<Integer> getGrantedSiteIds() {
		return grantedSiteIds;
	}

	public void setGrantedSiteIds(Set<Integer> grantedSitesIds) {
		this.grantedSiteIds = grantedSitesIds;
	}

	public boolean isShowGrantedBy() {
		return showGrantedBy;
	}

	public void setShowGrantedBy(boolean showGrantedBy) {
		this.showGrantedBy = showGrantedBy;
	}

}
