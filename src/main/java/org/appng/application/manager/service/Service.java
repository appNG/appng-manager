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
package org.appng.application.manager.service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Request;
import org.appng.api.auth.PasswordPolicy;
import org.appng.api.model.Application;
import org.appng.api.model.Identifier;
import org.appng.api.model.Permission;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Site;
import org.appng.api.model.Subject;
import org.appng.application.manager.form.GroupForm;
import org.appng.application.manager.form.PropertyForm;
import org.appng.application.manager.form.RepositoryForm;
import org.appng.application.manager.form.ResourceForm;
import org.appng.application.manager.form.RoleForm;
import org.appng.application.manager.form.SiteForm;
import org.appng.application.manager.form.SubjectForm;
import org.appng.application.manager.form.UploadForm;
import org.appng.core.controller.CachedResponse;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.model.JarInfo;
import org.appng.core.model.PackageArchive;
import org.appng.core.service.MigrationService.MigrationStatus;
import org.appng.core.xml.repository.PackageVersions;
import org.appng.core.xml.repository.Packages;
import org.appng.forms.FormUpload;
import org.appng.xml.platform.Selection;
import org.springframework.data.domain.Page;

/**
 * Service-interface for the manager application.
 * 
 * @author Matthias Müller
 * 
 */
public interface Service {

	void createGroup(Request request, GroupForm groupForm, Site site, FieldProcessor fp) throws BusinessException;

	void createRepository(Request request, RepositoryImpl valueHolder, FieldProcessor fp) throws BusinessException;

	void reloadRepository(Request request, Integer repositoryId, FieldProcessor fp) throws BusinessException;

	void createRole(Request request, RoleForm roleForm, Integer appId, FieldProcessor fp) throws BusinessException;

	void createSite(Request request, SiteForm sitesForm, FieldProcessor fp) throws BusinessException;

	void createSubject(Request request, Locale locale, SubjectForm form, FieldProcessor fp, PasswordPolicy policy) throws BusinessException;

	void createProperty(Request request, PropertyForm propertyForm, Integer siteId, Integer appId, FieldProcessor fp)
			throws BusinessException;

	void createPermission(Request request, PermissionImpl permission, Integer appId, FieldProcessor fp)
			throws BusinessException;

	DataContainer searchGroups(FieldProcessor fp, Site site, Integer siteId, Integer groupId, String groupName) throws BusinessException;

	DataContainer searchApplications(FieldProcessor fp, Integer siteId, Integer appId, boolean assignedOnly)
			throws BusinessException;

	DataContainer searchRepositories(Request request, FieldProcessor fp, Integer repositoryId);

	DataContainer searchInstallablePackages(Request request, FieldProcessor fp, Integer repositoryId, String filter)
			throws BusinessException;

	DataContainer searchPackageVersions(Request request, FieldProcessor fp, Integer repositoryId, String packageName)
			throws BusinessException;

	Packages searchPackages(Environment env, FieldProcessor fp, String repositoryName, String digest, String packageName)
			throws BusinessException;

	PackageVersions searchPackageVersions(Environment environment, FieldProcessor fp, String repositoryName, String packageName,
			String digest) throws BusinessException;

	PackageArchive getPackageArchive(Environment environment, String repositoryName, String packageName,
			String packageVersion, String packageTimestamp, String diges) throws BusinessException;

	DataContainer searchResources(Request request, Site site, FieldProcessor fp, ResourceType type, Integer resourceId,
			Integer appId) throws BusinessException;

	DataContainer searchRole(FieldProcessor fp, Integer roleId, Integer appId) throws BusinessException;

	DataContainer searchSites(Environment environment, FieldProcessor fp, Integer siteId, String name, String domain) throws BusinessException;

	DataContainer searchSubjects(Request request, FieldProcessor fp, Integer subjectId, String defaultTimezone,
			List<String> languages, Integer groupId) throws BusinessException;

	DataContainer searchPermissions(FieldProcessor fp, Integer permissionId, Integer appId) throws BusinessException;

	DataContainer searchProperties(FieldProcessor fp, Integer siteId, Integer appId, String propertyName)
			throws BusinessException;

	void updateGroup(Request request, Site site, GroupForm groupForm, FieldProcessor fp) throws BusinessException;

	void updateApplication(Request request, Environment env, Application application, FieldProcessor fp)
			throws BusinessException;

	void updateRepository(Request request, RepositoryForm repositoryForm, FieldProcessor fp) throws BusinessException;

	void updateRole(Request request, RoleForm roleForm, FieldProcessor fp) throws BusinessException;

	void updateSite(Request request, SiteForm sitesForm, FieldProcessor fp) throws BusinessException;

	Boolean updateSubject(Request request, SubjectForm form, FieldProcessor fp, PasswordPolicy policy) throws BusinessException;

	void updateProperty(Request request, PropertyForm propertyForm, FieldProcessor fp) throws BusinessException;

	void updatePermission(Request request, Permission permission, FieldProcessor fp) throws BusinessException;

	void assignGroupsToSubject(Request request, Integer subjectId, List<Integer> groupIds, FieldProcessor fp)
			throws BusinessException;

	MigrationStatus assignApplicationToSite(Request request, Integer siteId, Integer appId, FieldProcessor fp)
			throws BusinessException;

	MigrationStatus removeApplicationFromSite(Request request, Integer siteId, Integer appId, FieldProcessor fp)
			throws BusinessException;

	void deleteSubject(Request request, Subject currentSubject, Integer id, FieldProcessor fp) throws BusinessException;

	void deleteGroup(Request request, Integer id, FieldProcessor fp) throws BusinessException;

	void deletePermission(Request request, Integer id, FieldProcessor fp) throws BusinessException;

	void deleteRole(Integer id) throws BusinessException;

	void deleteApplication(Request request, Integer id, FieldProcessor fp) throws BusinessException;

	void deleteRepository(Request request, Integer repositoryId, FieldProcessor fp) throws BusinessException;

	void deletePackageVersion(Request request, Integer repositoryId, String packageName, String packageVersion,
			String packageTimestamp, FieldProcessor fp) throws BusinessException;

	String deleteResource(Request request, Integer appId, Integer resourceId, FieldProcessor fp)
			throws BusinessException;

	void deleteSite(Request request, String host, Integer siteId, FieldProcessor fp, Site currentSite)
			throws BusinessException;

	void deleteProperty(Request request, String id, FieldProcessor fp) throws BusinessException;

	void reloadSite(Request request, Application application, Integer siteId, FieldProcessor fp)
			throws BusinessException;

	DataContainer getNewSubject(Request request, FieldProcessor fp, String timezone, List<String> languages);

	DataContainer getNewPermission(FieldProcessor fp);

	DataContainer getNewGroup(Site site, FieldProcessor fp);

	DataContainer getNewSite(FieldProcessor fp);

	DataContainer getNewRepository(Request request, FieldProcessor fp);

	DataContainer getNewRole(FieldProcessor fp, Integer appId);

	DataContainer getNewProperty(FieldProcessor fp);

	List<JarInfo> getJars(Environment environment, Integer siteId);

	void installPackage(Request request, Integer repositoryId, String packageName, String packageVersion,
			String packageTimestamp, FieldProcessor fp) throws BusinessException;

	String updateResource(Request request, Site site, Integer appId, ResourceForm form, FieldProcessor fp)
			throws BusinessException;

	void createResource(Request request, Site site, Integer appId, UploadForm form, FieldProcessor fp)
			throws BusinessException;

	void updateDatabaseConnection(Request request, FieldProcessor fp, DatabaseConnection databaseConnection);

	void createDatabaseConnection(Request request, FieldProcessor fp, DatabaseConnection databaseConnection,
			Integer siteId);

	void deleteDatabaseConnection(Request request, FieldProcessor fp, Integer conId);

	void testConnection(Request request, FieldProcessor fp, Integer connectionId);

	void resetConnection(Integer conId);

	Page<DatabaseConnection> getDatabaseConnections(Integer siteId, FieldProcessor fp);

	DatabaseConnection getDatabaseConnection(Integer dcId, boolean clearPassword);

	Collection<RoleImpl> findRolesForSite(Integer siteId);

	SiteApplication getSiteApplication(Integer siteId, Integer appId);

	List<Selection> getGrantedSelections(Integer siteId, Integer appId);

	void grantSites(Integer siteId, Integer appId, Set<Integer> grantedSiteIds);

	String addArchiveToRepository(Request request, Integer repositoryId, FormUpload archive, FieldProcessor fp) throws BusinessException;

	RepositoryImpl getRepository(Integer repositoryId);

	String getNameForSite(Integer siteId);

	List<CachedResponse> getCacheEntries(Integer siteId);

	Map<String, String> getCacheStatistics(Integer siteId);

	void expireCacheElement(Request request, FieldProcessor fieldProcessor, Integer siteId, String cacheElement);

	void clearCache(Request request, FieldProcessor fp, Integer siteId);

	void deleteTemplate(Request request, String name, FieldProcessor fp);

	List<Identifier> listTemplates();

	void createEvent(Type type, String message);

	Site getSite(Integer siteId);

}
