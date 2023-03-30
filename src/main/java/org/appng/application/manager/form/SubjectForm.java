/*
 * Copyright 2011-2023 the original author or authors.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.FormValidator;
import org.appng.api.MessageParam;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.auth.PasswordPolicy.ValidationResult;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.UserType;
import org.appng.application.manager.MessageConstants;
import org.appng.core.domain.SubjectImpl;
import org.appng.xml.platform.FieldDef;
import org.appng.xml.platform.Validation;

/**
 * Bindclass used for creating/updating a {@link SubjectImpl}.
 * 
 * @author Matthias Müller
 */
public class SubjectForm implements FormValidator, Serializable {
	private SubjectImpl subject;
	private List<Integer> groupIds = new ArrayList<Integer>();
	private String password;
	private String passwordConfirmation;

	public SubjectForm() {
		this(new SubjectImpl());
	}

	public SubjectForm(SubjectImpl subject) {
		this.subject = subject;
	}

	public void validate(Site site, Application application, Environment environment, Options options, Request request,
			FieldProcessor fp) {
		if (isLocalUser()) {

			if (StringUtils.isBlank(getSubject().getEmail())) {
				String message = request.getMessage(MessageConstants.SUBJECT_ENTER_EMAIL);
				fp.addErrorMessage(fp.getField("subject.email"), message);
			}

			String action = options.getOptionValue("action", "id");
			FieldDef passwordField = fp.getField("password");
			if ("create".equals(action)) {
				if (null == password) {
					String message = request.getMessage(MessageConstants.SUBJECT_ENTER_PASSWORD);
					fp.addErrorMessage(passwordField, message);
				}
				if (null == passwordConfirmation) {
					String message = request.getMessage(MessageConstants.SUBJECT_ENTER_PASSWORD_CONFIRMATION);
					fp.addErrorMessage(fp.getField("passwordConfirmation"), message);
				}
			}

			PasswordPolicy passwordPolicy = site.getPasswordPolicy();
			if (!StringUtils.isEmpty(password)) {
				ValidationResult validationResult = passwordPolicy.validatePassword(getSubject().getAuthName(), null,
						password.toCharArray());
				if (!StringUtils.equals(password, passwordConfirmation)) {
					String message = request.getMessage(MessageConstants.SUBJECT_PASSWORDS_NO_MATCH);
					passwordField.setValidation(new Validation());
					fp.addErrorMessage(fp.getField("passwordConfirmation"), message);
				} else if (!validationResult.isValid()) {
					passwordField.setValidation(new Validation());
					for (MessageParam m : validationResult.getMessages()) {
						String errMssg = request.getMessage(m.getMessageKey(), m.getMessageArgs());
						fp.addErrorMessage(passwordField, errMssg);
					}
				}
			}
		}

	}

	@Valid
	public SubjectImpl getSubject() {
		return subject;
	}

	public void setSubject(SubjectImpl subject) {
		this.subject = subject;
	}

	public List<Integer> getGroupIds() {
		return groupIds;
	}

	public void setGroupIds(List<Integer> groupIds) {
		this.groupIds = groupIds;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		if (!"".equals(password)) {
			this.password = password;
		}
	}

	public String getPasswordConfirmation() {
		return passwordConfirmation;
	}

	public void setPasswordConfirmation(String passwordConfirmation) {
		if (!"".equals(passwordConfirmation)) {
			this.passwordConfirmation = passwordConfirmation;
		}
	}

	public boolean isLocalUser() {
		return UserType.LOCAL_USER.equals(getSubject().getUserType());
	}

}
