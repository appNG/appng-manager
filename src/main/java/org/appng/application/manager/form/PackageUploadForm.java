/*
 * Copyright 2011-2022 the original author or authors.
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

import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.FileUpload;
import org.appng.api.FormValidator;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.FileUpload.Unit;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.core.domain.PackageArchiveImpl;
import org.appng.core.model.PackageArchive;
import org.appng.forms.FormUpload;
import org.appng.xml.platform.FieldDef;

public class PackageUploadForm extends RepositoryForm implements FormValidator {

	private FormUpload archive;

	@Override
	public void validate(Site site, Application application, Environment environment, Options options, Request request,
			FieldProcessor fp) {
		FieldDef field = fp.getField("archive");
		if (null != archive) {
			PackageArchive packageArchive = new PackageArchiveImpl(archive.getFile(), archive.getOriginalFilename());
			if (!packageArchive.isValid()) {
				fp.addErrorMessage(field, request.getMessage(MessageConstants.PACKAGE_ARCHIVE_INVALID));
			}
		} else {
			fp.addErrorMessage(request.getMessage(MessageConstants.PACKAGE_ARCHIVE_NO_FILE));
		}
	}

	@FileUpload(fileTypes = "zip", maxSize = 50, unit = Unit.MB)
	public FormUpload getArchive() {
		return archive;
	}

	public void setArchive(FormUpload archive) {
		this.archive = archive;
	}

}
