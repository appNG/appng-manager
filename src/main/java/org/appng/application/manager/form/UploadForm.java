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

import org.appng.api.FileUpload;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.forms.FormUpload;

/**
 * Bindclass used for creating/updating a {@link Resource}.
 * 
 * @author Matthias Müller
 * 
 */
public class UploadForm {

	private FormUpload file;
	private ResourceType type;

	@Valid
	@FileUpload(maxCount = 1, minCount = 1, fileTypes = "jar, xml, properties")
	public FormUpload getFile() {
		return file;
	}

	public void setFile(FormUpload file) {
		this.file = file;
	}

	public ResourceType getType() {
		return type;
	}

	public void setType(ResourceType type) {
		this.type = type;
	}

}
