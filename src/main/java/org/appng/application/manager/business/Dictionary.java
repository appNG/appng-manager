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
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;

import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Property;
import org.appng.api.model.Site;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.PropertyImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

/**
 * A {@link DataProvider} providing the available messages from an {@link ResourceBundle} for a {@link Application} or
 * the platform.
 * 
 * @author Matthias Müller
 * 
 */

@Lazy
@Component
@Scope("request")
public class Dictionary extends ServiceAware implements DataProvider {

	private static final String MESSAGES_CORE = "messages-core";

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		ResourceBundle bundle = null;
		List<Property> properties = new ArrayList<Property>();

		Integer applicationId = request.convert(options.getOptionValue("application", "id"), Integer.class);
		Locale locale = environment.getLocale();
		if (null != applicationId) {
			ResourceBundleMessageSource messageSource = getBundleForApplication(site, environment, applicationId);
			// for (String key : messageSource.getKeys(locale)) {
			// properties.add(new PropertyImpl(key,
			// messageSource.getMessage(key, new Object[0], locale)));
			// }
		} else {
			ClassLoader parent = site.getSiteClassLoader().getParent();
			bundle = ResourceBundle.getBundle(MESSAGES_CORE, locale, parent);
			properties.addAll(getPropertiesFromBundle(bundle));
		}
		DataContainer data = new DataContainer(fp);
		data.setPage(properties, fp.getPageable());
		return data;
	}

	private ResourceBundleMessageSource getBundleForApplication(Site site, Environment environment,
			Integer applicationId) {
		for (Application p : site.getApplications()) {
			if (p.getId().equals(applicationId)) {
				return p.getBean(ResourceBundleMessageSource.class);
			}
		}
		return null;
	}

	private Collection<Property> getPropertiesFromBundle(ResourceBundle bundle) {
		SortedSet<Property> properties = new TreeSet<Property>();
		if (null != bundle) {
			Enumeration<String> keys = bundle.getKeys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				properties.add(new PropertyImpl(key, bundle.getString(key)));
			}
		}
		return properties;
	}
}
