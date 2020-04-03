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

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ActionProvider;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Property;
import org.appng.api.model.Property.Type;
import org.appng.api.model.SimpleProperty;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.form.PropertyForm;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.domain.PropertyImpl;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.FieldType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides CRUD-operations for a {@link PropertyImpl}.
 * 
 * @author Matthias Müller
 */

@Slf4j
@Component
public class Properties extends ServiceAware implements ActionProvider<PropertyForm>, DataProvider {

	private static final String PROPERTY = "property";
	private static final String PROPERTIES = "properties";

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Service service = getService();
		Integer siteId = options.getInteger(PROPERTIES, "siteId");
		Integer applicationId = options.getInteger(PROPERTIES, "applicationId");
		String propertyName = options.getString(PROPERTY, "id");
		DataContainer data = null;
		if (ACTION_CREATE.equals(getAction(options))) {
			data = service.getNewProperty(fp);
		} else {
			try {
				data = service.searchProperties(fp, siteId, applicationId, propertyName);
				if (null != data.getItem()) {
					PropertyForm propertyForm = (PropertyForm) data.getItem();
					PropertyWrapper propertyWrapper = new PropertyWrapper(propertyForm.getProperty());
					Type type = propertyWrapper.getType();
					FieldType expectedType = getFieldTypeForPropertyType(type);
					FieldDef field = fp.getField("property.value");
					field.setType(expectedType);
					if (FieldType.DECIMAL.equals(expectedType)) {
						String value = propertyWrapper.getActualString();
						int dotIdx = value.indexOf('.');
						int fraction = dotIdx > 0 ? value.substring(dotIdx + 1).length() : 3;
						field.setFormat("#." + StringUtils.repeat('#', fraction));
					}
					field.getLabel().setValue(propertyWrapper.getShortName());
					propertyForm.setProperty(propertyWrapper);
				} else {
					@SuppressWarnings("unchecked")
					Page<PropertyImpl> page = (Page<PropertyImpl>) data.getPage();
					data.setPage(page.map(p -> new PropertyWrapper(p, true)));
				}
			} catch (BusinessException ex) {
				String message = request.getMessage(ex.getMessageKey(), ex.getMessageArgs());
				log.error(message, ex);
				fp.addErrorMessage(message);
			}
		}
		return data;
	}

	private FieldType getFieldTypeForPropertyType(Type type) {
		switch (type) {
		case INT:
			return FieldType.INT;
		case DECIMAL:
			return FieldType.DECIMAL;
		case BOOLEAN:
			return FieldType.CHECKBOX;
		case PASSWORD:
			return FieldType.PASSWORD;
		case MULTILINE:
			return FieldType.LONGTEXT;
		default:
			return FieldType.TEXT;
		}
	}

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			PropertyForm propertyForm, FieldProcessor fp) {
		String action = getAction(options);
		String errorMessage = null;
		String okMessage = null;
		Service service = getService();
		String propertyName = options.getString(PROPERTY, ID);

		try {
			if (ACTION_CREATE.equals(action)) {
				errorMessage = MessageConstants.PROPERTY_CREATE_ERROR;
				Integer siteId = options.getInteger(PROPERTIES, "siteId");
				Integer applicationId = options.getInteger(PROPERTIES, "applicationId");
				service.createProperty(request, propertyForm, siteId, applicationId, fp);
				okMessage = MessageConstants.PROPERTY_CREATED;
			} else if (ACTION_UPDATE.equals(action)) {
				errorMessage = MessageConstants.PROPERTY_UPDATE_ERROR;
				propertyForm.getProperty().setName(propertyName);
				service.updateProperty(request, propertyForm, fp);
				okMessage = MessageConstants.PROPERTY_UPDATED;
			} else if (ACTION_DELETE.equals(action)) {
				errorMessage = MessageConstants.PROPERTY_DELETE_ERROR;
				service.deleteProperty(request, propertyName, fp);
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
		private boolean hidePassword;

		PropertyWrapper(SimpleProperty property) {
			this(property, true);
		}

		PropertyWrapper(SimpleProperty property, boolean hidePassword) {
			this.property = property;
			this.hidePassword = hidePassword;
		}

		public String getDisplayValue() {
			if (Property.Type.MULTILINE.equals(getType())) {
				return getClob();
			}
			return getActualString();
		}

		@Override
		public String getActualString() {
			String stringValue = getString();
			if (StringUtils.isNotBlank(stringValue) && hidePassword && Property.Type.PASSWORD.equals(getType())) {
				return stringValue.substring(0, 2) + StringUtils.repeat('*', stringValue.length() - 4)
						+ stringValue.substring(stringValue.length() - 2);
			}
			return stringValue;
		}

		@Override
		public Boolean getChangedValue() {
			return property.getChangedValue();
		}

		public String getShortName() {
			return getName().substring(getName().lastIndexOf('.') + 1);
		}

		@Override
		public String getString() {
			return property.getString();
		}

		@Override
		public Boolean getBoolean() {
			return property.getBoolean();
		}

		public void setBoolean(Boolean value) {
			property.setActualString(value.toString());
		}

		@Override
		public Integer getInteger() {
			return property.getInteger();
		}

		public void setInteger(Integer value) {
			property.setActualString(value.toString());
		}

		@Override
		public Float getFloat() {
			return property.getFloat();
		}

		@Override
		public Double getDouble() {
			return property.getDouble();
		}

		@Override
		public byte[] getBlob() {
			return property.getBlob();
		}

		@Override
		public String getClob() {
			return property.getClob();
		}

		@Override
		public String getName() {
			return property.getName();
		}

		@Override
		public boolean isMandatory() {
			return property.isMandatory();
		}

		@Override
		public String getDefaultString() {
			return property.getDefaultString();
		}

		@Override
		public Type getType() {
			return property.getType();
		}

		@Override
		public Object getValue() {
			return property.getValue();
		}

		@Override
		public void setValue(Object value) {
			property.setValue(value);
		}

		@Override
		public String getDescription() {
			return property.getDescription();
		}
	}
}
