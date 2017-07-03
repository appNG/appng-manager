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
package org.appng.application.manager.soap.endpoint;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.SoapService;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.FieldProcessorImpl;
import org.appng.application.manager.service.Service;
import org.appng.core.domain.PackageArchiveImpl;
import org.appng.core.model.PackageArchive;
import org.appng.core.model.Repository;
import org.appng.core.model.RepositoryUtils;
import org.appng.core.xml.repository.GetPackageRequest;
import org.appng.core.xml.repository.GetPackageResponse;
import org.appng.core.xml.repository.GetPackageVersionsRequest;
import org.appng.core.xml.repository.GetPackageVersionsResponse;
import org.appng.core.xml.repository.GetPackagesRequest;
import org.appng.core.xml.repository.GetPackagesResponse;
import org.appng.core.xml.repository.Package;
import org.appng.core.xml.repository.PackageVersions;
import org.appng.core.xml.repository.Packages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * 
 * A {@link SoapService} allowing remote listing and transferring of the available {@link Package}s from a
 * {@link Repository}.
 * 
 * @author Matthias Herlitzius
 * 
 */
@Endpoint
@Scope("request")
@org.springframework.stereotype.Service
public class RepositoryService implements SoapService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryService.class);
	private static final String SCHEMA_LOCATION = "appng-repository.xsd";
	private static final String REPOSITORY = "repository";
	private static final String NAMESPACE = "http://www.appng.org/schema/repository";
	private static final String GET_PACKAGES = "getPackagesRequest";
	private static final String GET_PACKAGE_VERSIONS = "getPackageVersionsRequest";
	private static final String GET_PACKAGE = "getPackageRequest";

	@Autowired
	private Service service;

	@PayloadRoot(localPart = GET_PACKAGES, namespace = NAMESPACE)
	public @ResponsePayload GetPackagesResponse getPackages(@RequestPayload GetPackagesRequest request)
			throws BusinessException {
		String repositoryName = request.getRepositoryName();
		try {
			GetPackagesResponse response = new GetPackagesResponse();
			FieldProcessor fp = new FieldProcessorImpl(REPOSITORY);
			Packages packages = service.searchPackages(fp, repositoryName, request.getDigest());
			response.setPackages(packages);
			return response;
		} catch (BusinessException e) {
			LOGGER.error("error while retrieving packages from repository: " + repositoryName, e);
			throw e;
		}
	}

	@PayloadRoot(localPart = GET_PACKAGE_VERSIONS, namespace = NAMESPACE)
	public @ResponsePayload GetPackageVersionsResponse getPackageVersions(
			@RequestPayload GetPackageVersionsRequest request) throws BusinessException {
		String repositoryName = request.getRepositoryName();
		String packageName = request.getPackageName();
		try {
			GetPackageVersionsResponse response = new GetPackageVersionsResponse();
			FieldProcessor fp = new FieldProcessorImpl(REPOSITORY);
			PackageVersions packageVersions = service.searchPackageVersions(fp, repositoryName, packageName,
					request.getDigest());
			response.setPackageVersions(packageVersions);
			return response;
		} catch (BusinessException e) {
			LOGGER.error("error while retrieving package versions from repository: " + repositoryName + ", package: "
					+ packageName, e);
			throw e;
		}
	}

	@PayloadRoot(localPart = GET_PACKAGE, namespace = NAMESPACE)
	public @ResponsePayload GetPackageResponse getPackage(@RequestPayload GetPackageRequest request)
			throws BusinessException {
		String repositoryName = request.getRepositoryName();
		String packageName = request.getPackageName();
		String version = request.getPackageVersion();
		String timestamp = request.getPackageTimestamp();
		try {
			PackageArchive archive = service.getPackageArchive(repositoryName, packageName, version, timestamp,
					request.getDigest());
			try {
				if (StringUtils.isBlank(timestamp)) {
					timestamp = archive.getPackageInfo().getTimestamp();
				}
				GetPackageResponse response = new GetPackageResponse();
				response.setChecksum(archive.getChecksum());
				response.setData(archive.getBytes());
				response.setFileName(PackageArchiveImpl.getFileName(packageName, version, timestamp));
				return response;
			} catch (IOException e) {
				throw new BusinessException("error getting bytes from " + archive, e);
			}
		} catch (BusinessException e) {
			LOGGER.error("error while retrieving package archive from remote repository: " + repositoryName
					+ ", package: " + packageName + ", version: " + version + ", timestamp: " + timestamp, e);
			throw e;
		}
	}

	public String getContextPath() {
		return RepositoryUtils.getContextPath();
	}

	public String getSchemaLocation() {
		return SCHEMA_LOCATION;
	}

	public void setApplication(Application application) {
	}

	public void setSite(Site site) {
	}

	public void setEnvironment(Environment environment) {
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

}
