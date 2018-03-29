/*
 * Copyright 2011-2018 the original author or authors.
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
import org.appng.api.model.Application;
import org.appng.api.model.Identifier;
import org.appng.api.model.Permission;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Role;
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
import org.appng.core.controller.AppngCache;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.domain.RepositoryImpl;
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

	void createGroup(GroupForm groupForm, Site site, FieldProcessor fp) throws BusinessException;

	void createRepository(RepositoryImpl valueHolder, FieldProcessor fp) throws BusinessException;

	void reloadRepository(Integer repositoryId, FieldProcessor fp) throws BusinessException;

	void createRole(RoleForm roleForm, Integer appId, FieldProcessor fp) throws BusinessException;

	void createSite(SiteForm sitesForm, FieldProcessor fp) throws BusinessException;

	void createSubject(Locale locale, SubjectForm form, FieldProcessor fp) throws BusinessException;

	void createProperty(PropertyForm propertyForm, Integer siteId, Integer appId, FieldProcessor fp)
			throws BusinessException;

	void createPermission(PermissionImpl permission, Integer appId, FieldProcessor fp) throws BusinessException;

	DataContainer searchGroups(FieldProcessor fp, Site site, Integer siteId, Integer groupId) throws BusinessException;

	DataContainer searchApplications(FieldProcessor fp, Integer siteId, Integer appId, boolean assignedOnly)
			throws BusinessException;

	DataContainer searchRepositories(FieldProcessor fp, Integer repositoryId);

	DataContainer searchInstallablePackages(FieldProcessor fp, Integer repositoryId) throws BusinessException;

	DataContainer searchPackageVersions(FieldProcessor fp, Integer repositoryId, String packageName)
			throws BusinessException;

	Packages searchPackages(FieldProcessor fp, String repositoryName, String digest) throws BusinessException;

	PackageVersions searchPackageVersions(FieldProcessor fp, String repositoryName, String packageName, String digest)
			throws BusinessException;

	PackageArchive getPackageArchive(String repositoryName, String packageName, String packageVersion,
			String packageTimestamp, String diges) throws BusinessException;

	DataContainer searchResources(Site site, FieldProcessor fp, ResourceType type, Integer resourceId, Integer appId)
			throws BusinessException;

	DataContainer searchRole(FieldProcessor fp, Integer roleId, Integer appId) throws BusinessException;

	DataContainer searchSites(FieldProcessor fp, Integer siteId) throws BusinessException;

	DataContainer searchSubjects(Request request, FieldProcessor fp, Integer subjectId, String defaultTimezone,
			List<String> languages) throws BusinessException;

	DataContainer searchPermissions(FieldProcessor fp, Integer permissionId, Integer appId) throws BusinessException;

	DataContainer searchProperties(FieldProcessor fp, Integer siteId, Integer appId, String propertyName)
			throws BusinessException;

	void updateGroup(Site site, GroupForm groupForm, FieldProcessor fp) throws BusinessException;

	void updateApplication(Environment env, Application application, FieldProcessor fp) throws BusinessException;

	void updateRepository(RepositoryForm repositoryForm, FieldProcessor fp) throws BusinessException;

	void updateRole(RoleForm roleForm, FieldProcessor fp) throws BusinessException;

	void updateSite(SiteForm sitesForm, FieldProcessor fp) throws BusinessException;

	Boolean updateSubject(SubjectForm form, FieldProcessor fp) throws BusinessException;

	void updateProperty(PropertyForm propertyForm, FieldProcessor fp) throws BusinessException;

	void updatePermission(Permission permission, FieldProcessor fp) throws BusinessException;

	void assignGroupsToSubject(Integer subjectId, List<Integer> groupIds, FieldProcessor fp) throws BusinessException;

	MigrationStatus assignApplicationToSite(Integer siteId, Integer appId, FieldProcessor fp) throws BusinessException;

	MigrationStatus removeApplicationFromSite(Integer siteId, Integer appId, FieldProcessor fp)
			throws BusinessException;

	void deleteSubject(Subject currentSubject, Integer id, FieldProcessor fp) throws BusinessException;

	void deleteGroup(Integer id, FieldProcessor fp) throws BusinessException;

	void deletePermission(Integer id, FieldProcessor fp) throws BusinessException;

	void deleteRole(Integer id) throws BusinessException;

	void deleteApplication(Integer id, FieldProcessor fp) throws BusinessException;

	void deleteRepository(Integer repositoryId, FieldProcessor fp) throws BusinessException;

	void deletePackageVersion(Integer repositoryId, String packageName, String packageVersion, String packageTimestamp,
			FieldProcessor fp) throws BusinessException;

	String deleteResource(Integer appId, Integer resourceId, FieldProcessor fp) throws BusinessException;

	void deleteSite(String host, Integer siteId, FieldProcessor fp, Site currentSite) throws BusinessException;

	void deleteProperty(String id, FieldProcessor fp) throws BusinessException;

	void reloadSite(Application application, Integer siteId, FieldProcessor fp) throws BusinessException;

	DataContainer getNewSubject(FieldProcessor fp, String timezone, List<String> languages);

	DataContainer getNewPermission(FieldProcessor fp);

	DataContainer getNewGroup(Site site, FieldProcessor fp);

	DataContainer getNewSite(FieldProcessor fp);

	DataContainer getNewRepository(FieldProcessor fp);

	DataContainer getNewRole(FieldProcessor fp, Integer appId);

	DataContainer getNewProperty(FieldProcessor fp);

	List<JarInfo> getJars(Environment environment, Integer siteId);

	void installPackage(Integer repositoryId, String packageName, String packageVersion, String packageTimestamp,
			FieldProcessor fp) throws BusinessException;

	void setRequest(Request request);

	String updateResource(Site site, Integer appId, ResourceForm form, FieldProcessor fp) throws BusinessException;

	void createResource(Site site, Integer appId, UploadForm form, FieldProcessor fp) throws BusinessException;

	void updateDatabaseConnection(FieldProcessor fp, DatabaseConnection databaseConnection);

	void createDatabaseConnection(FieldProcessor fp, DatabaseConnection databaseConnection, Integer siteId);

	void deleteDatabaseConnection(FieldProcessor fp, Integer conId);

	void testConnection(FieldProcessor fp, Integer connectionId);

	void resetConnection(Integer conId);

	Page<DatabaseConnection> getDatabaseConnections(Integer siteId, FieldProcessor fp);

	DatabaseConnection getDatabaseConnection(Integer dcId, boolean clearPassword);

	Collection<? extends Role> findRolesForSite(Integer siteId);

	SiteApplication getSiteApplication(Integer siteId, Integer appId);

	List<Selection> getGrantedSelections(Integer siteId, Integer appId);

	void grantSites(Integer siteId, Integer appId, Set<Integer> grantedSiteIds);

	String addArchiveToRepository(Integer repositoryId, FormUpload archive, FieldProcessor fp);

	RepositoryImpl getRepository(Integer repositoryId);

	String getNameForSite(Integer siteId);

	List<AppngCache> getCacheEntries(Integer siteId);

	Map<String, String> getCacheStatistics(Integer siteId);

	void expireCacheElement(FieldProcessor fieldProcessor, Request request, Integer siteId, String cacheElement);

	void clearCacheStatistics(FieldProcessor fieldProcessor, Request request, Integer siteId);

	void clearCache(FieldProcessor fp, Request request, Integer siteId);

	void deleteTemplate(String name, FieldProcessor fp);

	List<Identifier> listTemplates();

	void createEvent(Type type, String message);

}
