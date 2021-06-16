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
package org.appng.application.manager.service;

import org.appng.api.Environment;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.model.Site;
import org.appng.core.service.InitializerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class StopSiteEvent extends Event {

	private static final long serialVersionUID = 8053808333634879840L;

	public StopSiteEvent(String siteName) {
		super(siteName);
	}

	public void perform(Environment environment, Site site) throws InvalidConfigurationException {
		Logger logger = LoggerFactory.getLogger(StopSiteEvent.class);
		logger.info("about to stop site: {}", getSiteName());
		ApplicationContext platformContext = environment.getAttribute(Scope.PLATFORM,
				Platform.Environment.CORE_PLATFORM_CONTEXT);
		platformContext.getBean(InitializerService.class).shutDownSite(environment, site);
	}
}
