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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Collectors;
import javax.validation.Valid;

import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.FormValidator;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.ValidationPatterns;

/**
 * Bindclass used for creating/updating a {@link SiteImpl}.
 * 
 * @author Matthias Müller
 * 
 */
public class SiteForm implements FormValidator {

	private SiteImpl site;
	private String template;

	public SiteForm() {
		this.site = new SiteImpl();
	}

	public SiteForm(SiteImpl site) {
		this.site = site;
	}

	@Valid
	public SiteImpl getSite() {
		return site;
	}

	public void setSite(SiteImpl site) {
		this.site = site;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getHostAliases() {
		return site.getHostAliases().stream().sorted().collect(Collectors.joining(System.lineSeparator()));
	}

	public void setHostAliases(String hostAliases) {
		Set<String> hostnames = new HashSet<>();
		Pattern splitPattern = Pattern.compile("^[ \t]*(.+?)[ \t]*$", Pattern.MULTILINE);
		Matcher splitMatcher = splitPattern.matcher(hostAliases);
		while (splitMatcher.find()) {
			hostnames.add(splitMatcher.group(1));
		}
		site.setHostAliases(hostnames);
	}

	public void validate(Site site, Application application, Environment environment, Options options, Request request,
			FieldProcessor fp) {
		for (String name : this.site.getHostAliases()) {
			if (!Pattern.matches(ValidationPatterns.HOST_PATTERN, name))
				fp.addErrorMessage(fp.getField("hostAliases"),
						request.getMessage(MessageConstants.SITE_HOSTALIAS_INVALID, name));
		}
	}

}
