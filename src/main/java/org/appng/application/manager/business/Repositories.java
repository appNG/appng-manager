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
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.form.PackageUploadForm;
import org.appng.application.manager.form.RepositoryForm;
import org.appng.application.manager.service.Service;
import org.appng.application.manager.service.ServiceAware;
import org.appng.forms.FormUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Provides CRUD-operations for a {@link ApplicationRepositoryImpl}.
 * 
 * @author Matthias Herlitzius
 * 
 */

@Lazy
@Component
@Scope("request")
public class Repositories extends ServiceAware implements DataProvider, ActionProvider<RepositoryForm> {

	private static final String UPLOAD = "upload";
	private static final Logger log = LoggerFactory.getLogger(Repositories.class);
	private static final String ACTION_UPLOAD_ARCHIVE = "uploadArchive";
	public static final String REPOSITORY = "repository";

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			RepositoryForm repositoryForm, FieldProcessor fp) {
		String action = getAction(options);
		String errorMessage = null;
		String okMessage = null;
		Service service = getService();
		Integer repositoryId = request.convert(options.getOptionValue(REPOSITORY, ID), Integer.class);
		String archiveName = null;
		try {
			if (ACTION_CREATE.equals(action)) {
				errorMessage = MessageConstants.REPOSITORY_CREATE_ERROR;
				service.createRepository(repositoryForm.getRepository(), fp);
				okMessage = MessageConstants.REPOSITORY_CREATED;
			} else if (ACTION_UPDATE.equals(action)) {
				repositoryForm.getRepository().setId(repositoryId);
				errorMessage = MessageConstants.REPOSITORY_UPDATE_ERROR;
				service.updateRepository(repositoryForm, fp);
				okMessage = MessageConstants.REPOSITORY_UPDATED;
			} else if (ACTION_DELETE.equals(action)) {
				errorMessage = MessageConstants.REPOSITORY_DELETE_ERROR;
				service.deleteRepository(repositoryId, fp);
				okMessage = MessageConstants.REPOSITORY_DELETED;
			} else if (ACTION_RELOAD.equals(action)) {
				errorMessage = MessageConstants.REPOSITORY_RELOAD_ERROR;
				service.reloadRepository(repositoryId, fp);
				okMessage = MessageConstants.REPOSITORY_RELOADED;
			} else if (ACTION_UPLOAD_ARCHIVE.equals(action)) {
				errorMessage = MessageConstants.REPOSITORY_ARCHIVE_UPLOAD_ERROR;
				FormUpload archive = ((PackageUploadForm) repositoryForm).getArchive();
				archiveName = archive.getOriginalFilename();
				service.addArchiveToRepository(repositoryId, archive, fp);
				okMessage = MessageConstants.REPOSITORY_ARCHIVE_UPLOADED;
			}
			String message = request.getMessage(okMessage, repositoryId, archiveName);
			fp.addOkMessage(message);
		} catch (BusinessException ex) {
			String message = request.getMessage(errorMessage, repositoryId, archiveName);
			log.error(message, ex);
			fp.addErrorMessage(message);
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		Service service = getService();
		Integer repositoryId = request.convert(options.getOptionValue(REPOSITORY, ID), Integer.class);
		DataContainer data = null;
		if (null == repositoryId && ACTION_CREATE.equals(getAction(options))) {
			data = service.getNewRepository(fp);
		} else if (UPLOAD.equals(options.getOptionValue(REPOSITORY, MODE))) {
			data = new DataContainer(fp);
			PackageUploadForm packageUploadForm = new PackageUploadForm();
			packageUploadForm.setRepository(service.getRepository(repositoryId));
			data.setItem(packageUploadForm);
		} else {
			data = service.searchRepositories(fp, repositoryId);
		}
		return data;
	}
}
