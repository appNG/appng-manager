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

import org.appng.api.ActionProvider;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.SimpleProperty;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.form.PropertyForm;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.PropertyImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides CRUD-operations for a {@link PropertyImpl}.
 * 
 * @author Matthias Müller
 * 
 */

@Slf4j
@Lazy
@Component
@Scope("request")
public class Properties extends ServiceAware implements ActionProvider<PropertyForm>, DataProvider {

	private static final String PROPERTY = "property";
	private static final String PROPERTIES = "properties";

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Service service = getService();
		Integer siteId = request.convert(options.getOptionValue(PROPERTIES, "siteId"), Integer.class);
		Integer applicationId = request.convert(options.getOptionValue(PROPERTIES, "applicationId"), Integer.class);
		String propertyName = request.convert(options.getOptionValue(PROPERTY, "id"), String.class);
		DataContainer data = null;
		if (ACTION_CREATE.equals(getAction(options))) {
			data = service.getNewProperty(fp);
		} else {
			try {
				data = service.searchProperties(fp, siteId, applicationId, propertyName);
				if (null != data.getItem()) {
					PropertyForm propertyForm = (PropertyForm) data.getItem();
					propertyForm.setProperty(new PropertyWrapper(propertyForm.getProperty()));
				} else {
					@SuppressWarnings("unchecked")
					Page<PropertyImpl> page = (Page<PropertyImpl>) data.getPage();
					data.setPage(page.map(new Converter<PropertyImpl, PropertyWrapper>() {
						public PropertyWrapper convert(PropertyImpl p) {
							return new PropertyWrapper(p);
						}
					}));
				}
			} catch (BusinessException ex) {
				String message = request.getMessage(ex.getMessageKey(), ex.getMessageArgs());
				log.error(message, ex);
				fp.addErrorMessage(message);
			}
		}
		return data;
	}

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			PropertyForm propertyForm, FieldProcessor fp) {
		String action = getAction(options);
		String errorMessage = null;
		String okMessage = null;
		Service service = getService();
		String propertyName = options.getOptionValue(PROPERTY, ID);
		try {
			if (ACTION_CREATE.equals(action)) {
				errorMessage = MessageConstants.PROPERTY_CREATE_ERROR;
				Integer siteId = request.convert(options.getOption(PROPERTIES).getString("siteId"), Integer.class);
				Integer applicationId = request.convert(options.getOption(PROPERTIES).getString("applicationId"),
						Integer.class);
				service.createProperty(propertyForm, siteId, applicationId, fp);
				okMessage = MessageConstants.PROPERTY_CREATED;
			} else if (ACTION_UPDATE.equals(action)) {
				errorMessage = MessageConstants.PROPERTY_UPDATE_ERROR;
				propertyForm.getProperty().setName(propertyName);
				service.updateProperty(propertyForm, fp);
				okMessage = MessageConstants.PROPERTY_UPDATED;
			} else if (ACTION_DELETE.equals(action)) {
				errorMessage = MessageConstants.PROPERTY_DELETE_ERROR;
				service.deleteProperty(propertyName, fp);
				okMessage = MessageConstants.PROPERTY_DELETED;
			}
			String message = request.getMessage(okMessage, propertyName);
			fp.addOkMessage(message);
		} catch (BusinessException ex) {
			String message = request.getMessage(errorMessage, propertyName);
			log.error(message, ex);
			fp.addErrorMessage(message);
		}
	}

	public class PropertyWrapper extends PropertyImpl {
		private SimpleProperty property;

		PropertyWrapper(SimpleProperty property) {
			this.property = property;
		}

		public String getActualString() {
			return property.getActualString();
		}

		public Boolean getChangedValue() {
			return property.getChangedValue();
		}

		public String getShortName() {
			return getName().substring(getName().lastIndexOf('.') + 1);
		}

		public String getString() {
			return property.getString();
		}

		public Boolean getBoolean() {
			return property.getBoolean();
		}

		public Integer getInteger() {
			return property.getInteger();
		}

		public Float getFloat() {
			return property.getFloat();
		}

		public Double getDouble() {
			return property.getDouble();
		}

		public byte[] getBlob() {
			return property.getBlob();
		}

		public String getClob() {
			return property.getClob();
		}

		public String getName() {
			return property.getName();
		}

		public boolean isMandatory() {
			return property.isMandatory();
		}

		public String getDefaultString() {
			return property.getDefaultString();
		}

		public String getDescription() {
			return property.getDescription();
		}
	}
}
