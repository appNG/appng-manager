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

import java.io.UnsupportedEncodingException;

import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;

/**
 * Bindclass used for creating/updating a {@link Resource}.
 * 
 * @author Matthias Müller
 * 
 */
public class ResourceForm {

	public static final String ENCODING = "UTF-8";

	private Integer id;
	private String name;
	private ResourceType type;
	private String content;

	public ResourceForm(Resource resource) {
		if (!ResourceType.JAR.equals(resource.getResourceType())) {
			try {
				this.content = new String(resource.getBytes(), ENCODING);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		this.id = resource.getId();
		this.name = resource.getName();
		this.type = resource.getResourceType();
	}

	public ResourceForm() {

	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public ResourceType getType() {
		return type;
	}

}
