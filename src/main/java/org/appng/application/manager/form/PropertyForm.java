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
package org.appng.application.manager.form;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.FormValidator;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.domain.PropertyImpl;

/**
 * Bindclass used for creating/updating a {@link PropertyImpl}.
 * 
 * @author Matthias Müller
 * 
 */
public class PropertyForm implements FormValidator {
	private PropertyImpl property;

	public PropertyForm() {
		this(new PropertyImpl());
	}

	public PropertyForm(PropertyImpl property) {
		this.property = property;
	}

	@Valid
	public PropertyImpl getProperty() {
		return property;
	}

	public void setProperty(PropertyImpl property) {
		this.property = property;
	}

	public void validate(Site site, Application application, Environment environment, Options options, Request request,
			FieldProcessor fp) {
		String fieldValue;
		String fieldName;
		boolean isCreate = fp.hasField("property.name");
		if (isCreate) {
			fieldValue = getProperty().getDefaultString();
			fieldName = "property.defaultString";
		} else {
			fieldValue = getProperty().getActualString();
			fieldName = "property.actualString";
		}
		int clobLength = StringUtils.length(getProperty().getClob());
		int stringLength = StringUtils.length(fieldValue);
		if ((clobLength > 0 && stringLength > 0) || (isCreate && (clobLength + stringLength == 0))) {
			String message = application.getMessage(environment.getLocale(), "property.stringOrClob");
			fp.addErrorMessage(fp.getField("property.clob"), message);
			fp.addErrorMessage(fp.getField(fieldName), message);
		}
	}

}
