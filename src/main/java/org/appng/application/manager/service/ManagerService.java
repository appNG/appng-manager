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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.ApplicationException;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.InvalidConfigurationException;
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Group;
import org.appng.api.model.Identifier;
import org.appng.api.model.NameProvider;
import org.appng.api.model.Nameable;
import org.appng.api.model.Permission;
import org.appng.api.model.Properties;
import org.appng.api.model.Resource;
import org.appng.api.model.ResourceType;
import org.appng.api.model.Resources;
import org.appng.api.model.Role;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.api.model.Subject;
import org.appng.api.model.UserType;
import org.appng.api.support.OptionGroupFactory;
import org.appng.api.support.OptionGroupFactory.OptionGroup;
import org.appng.api.support.OptionOwner.Selector;
import org.appng.api.support.SelectionFactory;
import org.appng.api.support.environment.EnvironmentKeys;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.form.GroupForm;
import org.appng.application.manager.form.PropertyForm;
import org.appng.application.manager.form.RepositoryForm;
import org.appng.application.manager.form.ResourceForm;
import org.appng.application.manager.form.RoleForm;
import org.appng.application.manager.form.SiteForm;
import org.appng.application.manager.form.SubjectForm;
import org.appng.application.manager.form.UploadForm;
import org.appng.application.manager.soap.endpoint.RepositoryService;
import org.appng.core.controller.AppngCache;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.DatabaseConnection;
import org.appng.core.domain.GroupImpl;
import org.appng.core.domain.PermissionImpl;
import org.appng.core.domain.PlatformEvent.Type;
import org.appng.core.domain.PropertyImpl;
import org.appng.core.domain.RepositoryImpl;
import org.appng.core.domain.ResourceImpl;
import org.appng.core.domain.RoleImpl;
import org.appng.core.domain.SiteApplication;
import org.appng.core.domain.SiteApplicationPK;
import org.appng.core.domain.SiteImpl;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.model.InstallablePackage;
import org.appng.core.model.JarInfo;
import org.appng.core.model.PackageArchive;
import org.appng.core.model.PackageVersion;
import org.appng.core.model.Repository;
import org.appng.core.model.RepositoryCacheFactory;
import org.appng.core.model.RepositoryMode;
import org.appng.core.model.RepositoryType;
import org.appng.core.model.RepositoryUtils;
import org.appng.core.service.CoreService;
import org.appng.core.service.InitializerService;
import org.appng.core.service.MigrationService.MigrationStatus;
import org.appng.core.service.PropertySupport;
import org.appng.core.xml.repository.PackageVersions;
import org.appng.core.xml.repository.Packages;
import org.appng.forms.FormUpload;
import org.appng.persistence.repository.SearchQuery;
import org.appng.xml.application.PackageInfo;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.SelectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.annotation.Transactional;

/**
 * The {@link Service}-implementation extending {@link CoreService}.
 * 
 * @author Matthias Müller
 * 
 */
@Transactional(rollbackFor = BusinessException.class)
public class ManagerService extends CoreService implements Service {

	private Logger logger = LoggerFactory.getLogger(ManagerService.class);

	@Autowired
	private Request request;

	@Autowired
	private Environment environment;

	@Autowired
	private SelectionFactory selectionFactory;

	@Autowired
	private OptionGroupFactory optionGroupFactory;

	private MessageSource timezoneMessages;

	public void deleteSubject(Subject currentSubject, Integer subjectId, FieldProcessor fp) throws BusinessException {
		try {
			if (currentSubject.getId().equals(subjectId)) {
				throw new BusinessException("can not delete currently used subject!");
			}
			SubjectImpl subject = subjectRepository.findOne(subjectId);
			if (null != subject) {
				subjectRepository.delete(subject);
			} else {
				throw new BusinessException("No such subject " + subjectId, MessageConstants.SUBJECT_NOT_EXISTS);
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void deletePermission(Integer permissionId, FieldProcessor fp) throws BusinessException {
		try {
			PermissionImpl permission = permissionRepository.findOne(permissionId);
			if (null != permission) {
				Application application = permission.getApplication();
				if (application.getPermissions().remove(permission)) {
					logger.debug("removed permission '" + permission.getName() + "' from Application "
							+ application.getName());
				}

				for (Role role : application.getRoles()) {
					if (role.getPermissions().remove(permission)) {
						logger.debug("removed permission '" + permission.getName() + "' from Role " + role.getName());
					}
				}
				permissionRepository.delete(permission);
			} else {
				throw new BusinessException("No such permission " + permissionId,
						MessageConstants.PERMISSION_NOT_EXISTS);
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void deleteRole(Integer roleId) throws BusinessException {
		String roleDeleteError = MessageConstants.ROLE_DELETE_ERROR;
		String roleErrorInvalid = MessageConstants.ROLE_NOT_EXISTS;
		deleteRole(roleId, roleDeleteError, roleErrorInvalid);
	}

	public void deleteApplication(Integer appId, FieldProcessor fp) throws BusinessException {
		String deleteErrorWithCause = MessageConstants.APPLICATION_DELETE_ERROR_WITH_CAUSE;
		String removedFromSite = MessageConstants.APPLICATION_REMOVED_FROM_SITE;
		String errorInvalid = MessageConstants.APPLICATION_NOT_EXISTS;
		String roleDeleteError = MessageConstants.ROLE_DELETE_ERROR;
		String roleErrorInvalid = MessageConstants.ROLE_NOT_EXISTS;
		deleteApplication(environment, request, appId, fp, deleteErrorWithCause, removedFromSite, errorInvalid,
				roleDeleteError, roleErrorInvalid);
	}

	public void deleteRepository(Integer repositoryId, FieldProcessor fp) throws BusinessException {
		try {
			RepositoryImpl repository = repoRepository.findOne(repositoryId);
			if (null != repository) {
				repoRepository.delete(repository);
			} else {
				throw new BusinessException("no such repository " + repositoryId,
						MessageConstants.REPOSITORY_NOT_EXISTS);
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public String deleteResource(Integer applicationId, Integer resourceId, FieldProcessor fp)
			throws BusinessException {
		try {
			if (null != applicationId) {
				String resourceName = deleteResource(request.getEnvironment(), applicationId, resourceId);
				String message = request.getMessage(MessageConstants.RESOURCE_DELETED, resourceName);
				fp.addOkMessage(message);
				return resourceName;
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
		return null;
	}

	public void deleteSite(String host, Integer siteId, final FieldProcessor fp, Site currentSite)
			throws BusinessException {
		try {
			SiteImpl site = siteRepository.findOne(siteId);
			if (null != site) {
				if (!site.equals(currentSite) && !site.getHost().equals(host)) {
					deleteSite(request.getEnvironment(), site);
				} else {
					throw new BusinessException("Can not delete current site " + site.getName(),
							MessageConstants.SITE_CURRENT_DELETE_ERROR, site.getName());
				}
			} else {
				throw new BusinessException("No such site " + siteId, MessageConstants.SITE_NOT_EXISTS);
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void deleteGroup(Integer id, FieldProcessor fp) throws BusinessException {
		try {
			GroupImpl group = groupRepository.findOne(id);
			if (null != group) {
				deleteGroup(group);
			} else {
				throw new BusinessException("No such group " + id);
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void createGroup(GroupForm groupForm, Site site, FieldProcessor fp) throws BusinessException {
		GroupImpl group = groupForm.getGroup();
		checkUniqueGroupName(group, fp);
		groupRepository.save(group);
		assignRolesToGroup(group, site, groupForm.getRoleIds());
	}

	public void updateGroup(Site site, GroupForm form, FieldProcessor fp) throws BusinessException {
		GroupImpl group = form.getGroup();
		checkUniqueGroupName(group, fp);
		GroupImpl currentGroup = groupRepository.findOne(group.getId());
		if (null == currentGroup) {
			throw new BusinessException("no such group");
		}
		getRequest().setPropertyValues(form, new GroupForm(currentGroup), fp.getMetaData());
		assignRolesToGroup(currentGroup, site, form.getRoleIds());
	}

	private void checkUniqueGroupName(Group group, FieldProcessor fp) throws BusinessException {
		boolean isUnique = groupRepository.isUnique(group.getId(), "name", group.getName());
		if (!isUnique) {
			fp.addErrorMessage(fp.getField("group.name"), request.getMessage(MessageConstants.GROUP_EXISTS));
			throw new BusinessException("group named " + group.getName() + " already exists!");
		}
	}

	public DataContainer searchGroups(FieldProcessor fp, Site site, Integer siteId, Integer groupId)
			throws BusinessException {
		DataContainer data = new DataContainer(fp);
		if (groupId != null) {
			GroupImpl group = groupRepository.findOne(groupId);
			if (null == group) {
				throw new BusinessException("no such group : " + groupId, MessageConstants.GROUP_NOT_EXISTS);
			}
			Selection selection = getRoleSelection(group, site.getId());

			data.getSelections().add(selection);
			data.setItem(new GroupForm(group));
		} else {
			Page<GroupImpl> groups = groupRepository.search(fp.getPageable());
			data.setPage(groups);
		}
		return data;
	}

	private Selection getRoleSelection(Group group, Integer siteId) {
		SiteImpl site = siteRepository.findOne(siteId);
		Selection selection = selectionFactory.fromObjects("roles", "roles", new Object[0], (Selector) null);
		for (Application application : sortByName(new ArrayList<Application>(site.getApplications()))) {
			String name = application.getName();
			List<Role> roles = new ArrayList<Role>(application.getRoles());
			OptionGroup roleGroup = optionGroupFactory.fromNamed(name, name, sortByName(roles), group.getRoles());
			selection.getOptionGroups().add(roleGroup);
		}
		selection.setType(SelectionType.SELECT_MULTIPLE);
		return selection;
	}

	private <T extends Nameable> List<T> sortByName(List<T> items) {
		Comparator<Nameable> nameComparator = new Comparator<Nameable>() {
			public int compare(Nameable n1, Nameable n2) {
				return n1.getName().compareTo(n2.getName());
			}
		};
		Collections.sort(items, nameComparator);
		return items;
	}

	public DataContainer searchRepositories(FieldProcessor fp, Integer repositoryId) {
		DataContainer data = new DataContainer(fp);
		if (repositoryId == null) {
			Page<RepositoryImpl> repositories = repoRepository.search(fp.getPageable());
			data.setPage(repositories);
		} else {
			RepositoryImpl repository = repoRepository.findOne(repositoryId);
			data.setItem(new RepositoryForm(repository));
			Selection repositoryTypeSelection = getRepositoryTypeSelection(repository.getRepositoryType());
			if (null != repositoryTypeSelection) {
				data.getSelections().add(repositoryTypeSelection);
			}
			Selection repositoryModeSelection = getRepositoryModeSelection(repository.getRepositoryMode());
			if (null != repositoryModeSelection) {
				data.getSelections().add(repositoryModeSelection);
			}
		}
		return data;
	}

	public DataContainer searchInstallablePackages(FieldProcessor fp, Integer repositoryId) throws BusinessException {
		DataContainer data = new DataContainer(fp);
		if (null != repositoryId) {
			RepositoryImpl repository = repoRepository.findOne(repositoryId);
			try {
				if (null != repository) {
					Page<ApplicationImpl> applications = applicationRepository.search(fp.getPageable());
					List<Identifier> identifiers = new ArrayList<Identifier>(applications.getContent());

					List<Identifier> templates = getInstalledTemplates();
					identifiers.addAll(templates);

					List<InstallablePackage> packages = repository.getInstallablePackages(identifiers);
					data.setPage(packages, fp.getPageable());
				}
			} catch (Exception e) {
				handleRepositoryException(fp, repository, e);
			}
		}
		return data;
	}

	protected void handleRepositoryException(FieldProcessor fp, Repository repository, Exception e)
			throws BusinessException {
		Exception handled = e;
		if (!(e instanceof BusinessException)) {
			handled = new BusinessException(null, e, MessageConstants.REPOSITORY_ERROR, repository.getName(),
					repository.getUri());
		}
		request.handleException(fp, handled);
	}

	public DataContainer searchPackageVersions(FieldProcessor fp, Integer repositoryId, String packageName)
			throws BusinessException {
		DataContainer data = new DataContainer(fp);
		try {
			if ((null != repositoryId) && (StringUtils.isNotBlank(packageName))) {
				RepositoryImpl repository = repoRepository.findOne(repositoryId);
				if (null != repository) {
					List<Identifier> packages = new ArrayList<Identifier>(applicationRepository.findAll());

					List<Identifier> templates = getInstalledTemplates();
					packages.addAll(templates);

					List<PackageVersion> packageVersions = repository.getPackageVersions(packages, packageName);
					Collections.reverse(packageVersions);
					data.setPage(packageVersions, fp.getPageable());
				}
			}
		} catch (Exception e) {
			request.handleException(fp, e);
		}
		return data;
	}

	/**
	 * Returns a {@link Packages}-object from a certain repository. {@link Packages} are made available to other appNG
	 * instances via the {@link RepositoryService}.
	 */
	public Packages searchPackages(FieldProcessor fp, String repositoryName, String digest) throws BusinessException {
		try {
			Repository repository = getRepository(repositoryName, digest);
			return repository.getPackages();
		} catch (Exception e) {
			request.handleException(fp, e);
		}
		return null;
	}

	/**
	 * Returns a {@link PackageVersions}-object from a certain repository. {@link PackageVersions} are made available to
	 * other appNG instances via the {@link RepositoryService}.
	 */
	public PackageVersions searchPackageVersions(FieldProcessor fp, String repositoryName, String packageName,
			String digest) throws BusinessException {
		try {
			Repository repository = getRepository(repositoryName, digest);
			return repository.getPackageVersions(packageName);
		} catch (Exception e) {
			request.handleException(fp, e);
		}
		return null;
	}

	public PackageArchive getPackageArchive(String repositoryName, String name, String version, String timestamp,
			String digest) throws BusinessException {
		Repository repository = getRepository(repositoryName, digest);
		return repository.getPackageArchive(name, version, timestamp);
	}

	private Repository getRepository(String repositoryName, String digest) throws BusinessException {
		if (StringUtils.isNotBlank(repositoryName)) {
			Repository repository = repoRepository.findByName(repositoryName);
			if (null != repository && repository.isPublished()) {
				boolean digestOk = false;
				if (StringUtils.isBlank(repository.getDigest())) {
					String defaultDigest = getPlatformConfig().getString(Platform.Property.REPOSITORY_DEFAULT_DIGEST);
					digestOk = StringUtils.equals(StringUtils.trimToEmpty(defaultDigest),
							StringUtils.trimToEmpty(digest));
				} else {
					digestOk = StringUtils.equals(repository.getDigest(), digest);
				}
				if (digestOk) {
					return repository;
				}
			}
		}
		throw new BusinessException("Repository not found or repository is not published: " + repositoryName);
	}

	protected Properties getPlatformConfig() {
		return environment.getAttribute(Scope.PLATFORM, Platform.Environment.PLATFORM_CONFIG);
	}

	/**
	 * Installs a {@link PackageArchive} in order to use it within the platform.
	 */
	public void installPackage(Integer repositoryId, String name, String version, String timestamp, FieldProcessor fp)
			throws BusinessException {
		try {
			Boolean isFilebased = getPlatformConfig().getBoolean(Platform.Property.FILEBASED_DEPLOYMENT);
			PackageVersions packageVersions = getRepository(repositoryId).getPackageVersions(name);
			List<PackageInfo> versions = packageVersions.getPackage().stream()
					.filter(p -> p.getVersion().equals(version) && p.getTimestamp().equals(timestamp)).limit(1)
					.collect(Collectors.toList());
			String appngVer = environment.getAttribute(Scope.PLATFORM, Platform.Environment.APPNG_VERSION);
			String pkAppngVer = versions.get(0).getAppngVersion();
			if (pkAppngVer.compareTo(appngVer) > 0) {
				String versionMismatch = request.getMessage(MessageConstants.PACKAGE_APP_NG_VERSION_MISMATCH,
						pkAppngVer, appngVer);
				fp.addNoticeMessage(versionMismatch);
			}
			installPackage(repositoryId, name, version, timestamp, false, false, isFilebased);
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	/**
	 * Deletes a {@link PackageArchive} in the {@link Repository}.
	 */
	public void deletePackageVersion(Integer repositoryId, String packageName, String packageVersion,
			String packageTimestamp, FieldProcessor fp) throws BusinessException {
		try {
			deletePackageVersion(repositoryId, packageName, packageVersion, packageTimestamp);
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	@SuppressWarnings("unchecked")
	public DataContainer searchResources(Site site, FieldProcessor fp, ResourceType type, Integer resourceId,
			Integer applicationId) throws BusinessException {
		DataContainer data = new DataContainer(fp);
		Application application = applicationRepository.findOne(applicationId);
		Resources resourceHolder;
		try {
			File appRootFolder = getApplicationRootFolder(environment);
			resourceHolder = getResources(application, null, appRootFolder);

			if (null == resourceId) {
				boolean isAsc = true;
				ComparatorChain comparatorChain = new ComparatorChain();
				comparatorChain.addComparator(new PropertyComparator<Resource>("resourceType", true, isAsc));
				comparatorChain.addComparator(new PropertyComparator<Resource>("name", true, isAsc));
				List<Resource> resources = null;
				if (null == type) {
					resources = new ArrayList<Resource>(resourceHolder.getResources());
				} else {
					resources = new ArrayList<Resource>(resourceHolder.getResources(type));
				}
				Collections.sort(resources, comparatorChain);
				data.setPage(resources, fp.getPageable());
				Selection typeSelection = selectionFactory.fromEnum("resourceType", MessageConstants.TYPE,
						ResourceType.values(), type);

				typeSelection.getOptions().add(0, new Option());
				SelectionGroup selectionGroup = new SelectionGroup();
				data.getSelectionGroups().add(selectionGroup);
				selectionGroup.getSelections().add(typeSelection);
			} else {
				Resource resource = resourceHolder.getResource(resourceId);
				data.setItem(new ResourceForm(resource));
			}
		} catch (InvalidConfigurationException e) {
			request.handleException(fp, e);
		}
		return data;
	}

	public String updateResource(Site site, Integer appId, ResourceForm form, FieldProcessor fp)
			throws BusinessException {
		String errorMessage = null;
		String okMessage = null;
		try {
			Integer id = form.getId();
			Application application = applicationRepository.findOne(appId);
			boolean fileBased = application.isFileBased();
			String reloadMessage = request.getMessage(MessageConstants.RELOAD_SITE);
			File appRootFolder = getApplicationRootFolder(environment);
			Resources resourceHolder = getResources(application, null, appRootFolder);
			Resource resource = resourceHolder.getResource(id);
			String fileName = resource.getName();
			ResourceType type = resource.getResourceType();
			if (fileBased) {
				errorMessage = request.getMessage(MessageConstants.RESOURCE_UPDATED_FILEBASED_ERROR, fileName);
				File resourceFolder = getResourceFolder(application.getName(), type);
				File original = new File(resourceFolder, fileName);
				FileUtils.write(original, form.getContent(), ResourceForm.ENCODING, false);
				okMessage = request.getMessage(MessageConstants.RESOURCE_UPDATED_FILEBASED, fileName,
						FileUtils.sizeOf(original));
				createEvent(Type.UPDATE,
						String.format("Resource %s of Application %s", fileName, application.getName()));
			} else {
				errorMessage = request.getMessage(MessageConstants.RESOURCE_UPDATED_DATABASED_ERROR, fileName);
				ResourceImpl dbResource = resourceRepository.findOne(resource.getId());
				dbResource.setBytes(form.getContent().getBytes());
				dbResource.calculateChecksum();
				okMessage = request.getMessage(MessageConstants.RESOURCE_UPDATED_DATABASED, fileName,
						resource.getSize());
			}
			fp.addNoticeMessage(reloadMessage);
			fp.addOkMessage(okMessage);
			return fileName;
		} catch (Exception e) {
			fp.addErrorMessage(errorMessage);
			request.handleException(fp, e);
		}
		return null;
	}

	private File getResourceFolder(String appName, ResourceType type) {
		File appFolder = getApplicationFolder(environment, appName);
		String typeFolder = type.getFolder();
		File resourceFolder = new File(appFolder, typeFolder);
		return resourceFolder;
	}

	public void createResource(Site site, Integer appId, UploadForm form, FieldProcessor fp) throws BusinessException {
		Application application = applicationRepository.findOne(appId);
		String okMessage = null;
		String errorMessage = null;
		try {
			ResourceType type = form.getType();
			FormUpload uploadFile = form.getFile();
			File file = uploadFile.getFile();
			String originalFilename = uploadFile.getOriginalFilename();
			okMessage = request.getMessage(MessageConstants.RESOURCE_UPLOADED, originalFilename,
					FileUtils.sizeOf(file));

			errorMessage = request.getMessage(MessageConstants.RESOURCE_UPLOAD_ERROR, originalFilename);
			if (type.isValidFileName(originalFilename)) {
				if (application.isFileBased()) {
					File appFolder = getResourceFolder(application.getName(), type);
					File targetFile = new File(appFolder, originalFilename);
					if (targetFile.exists()) {
						String overriddenMssg = request.getMessage(MessageConstants.RESOURCE_OVERRIDDEN,
								originalFilename);
						fp.addNoticeMessage(overriddenMssg);
					}
					FileUtils.copyFile(file, targetFile);
				} else {
					ResourceImpl resource = resourceRepository.findByNameAndApplicationId(originalFilename, appId);
					if (null != resource) {
						String overriddenMssg = request.getMessage(MessageConstants.RESOURCE_OVERRIDDEN,
								originalFilename);
						fp.addNoticeMessage(overriddenMssg);
					} else {
						resource = new ResourceImpl();
					}

					resource.setApplication(application);
					resource.setBytes(uploadFile.getBytes());
					resource.setName(originalFilename);
					resource.setResourceType(type);
					resource.setDescription("uploaded on " + new Date());
					resource.calculateChecksum();
					resourceRepository.save(resource);
				}
				String reloadMessage = request.getMessage(MessageConstants.RELOAD_SITE);
				fp.addNoticeMessage(reloadMessage);
				fp.addOkMessage(okMessage);
			} else {
				String invalidTypeMessage = application.getMessage(environment.getLocale(),
						MessageConstants.RESOURCE_WRONG_TYPE, type.toString(),
						StringUtils.join(type.getAllowedFileEndings(), ","));
				fp.addErrorMessage(invalidTypeMessage);
			}

		} catch (Exception e) {
			fp.addErrorMessage(errorMessage);
			request.handleException(fp, e);
		}
	}

	public DataContainer searchRole(FieldProcessor fp, Integer roleId, Integer appId) throws BusinessException {
		DataContainer data = new DataContainer(fp);
		if (roleId != null) {
			RoleImpl role = roleRepository.findOne(roleId);
			if (null == role) {
				throw new BusinessException("no such Role " + roleId, MessageConstants.ROLE_NOT_EXISTS);
			}
			Selection selection = getPermissionSelection(role.getApplication().getId(), role);
			data.getSelections().add(selection);
			RoleForm form = new RoleForm(role);
			data.setItem(form);
		} else {
			Page<RoleImpl> roles;
			if (null == appId) {
				roles = roleRepository.search(fp.getPageable());
			} else {
				SearchQuery<RoleImpl> query = new SearchQuery<RoleImpl>(RoleImpl.class).equals("application.id", appId);
				roles = roleRepository.search(query, fp.getPageable());
			}
			data.setPage(roles);
		}
		return data;
	}

	private Selection getPermissionSelection(Integer appId, RoleImpl role) {
		Set<Permission> permissionsFromRole = role.getPermissions();
		List<PermissionImpl> allPermissions = permissionRepository.findByApplicationId(appId,
				new Sort(Direction.ASC, "name"));
		Map<String, List<Permission>> permissionGroups = new HashMap<String, List<Permission>>();
		Pattern pattern = Pattern.compile("([^\\.]+)((.)*)");
		for (Permission permission : allPermissions) {
			Matcher matcher = pattern.matcher(permission.getName());
			if (matcher.matches()) {
				String group = matcher.group(1);
				if (!permissionGroups.containsKey(group)) {
					permissionGroups.put(group, new ArrayList<Permission>());
				}
				permissionGroups.get(group).add(permission);
			}
		}

		Selection permissionSelection = selectionFactory.fromObjects("permissions", "permissions", new Object[0],
				(Selector) null);
		List<String> groupNames = new ArrayList<String>(permissionGroups.keySet());
		Collections.sort(groupNames);
		for (String permissionGroup : groupNames) {
			List<Permission> permissions = sortByName(permissionGroups.get(permissionGroup));
			if (permissions.size() > 1) {
				OptionGroup group = optionGroupFactory.fromNamed(permissionGroup, permissionGroup, permissions,
						permissionsFromRole);
				permissionSelection.getOptionGroups().add(group);
			} else {
				List<Option> options = selectionFactory
						.fromNamed(permissionGroup, permissionGroup, permissions, permissionsFromRole).getOptions();
				permissionSelection.getOptions().addAll(options);
			}
		}
		permissionSelection.setType(SelectionType.SELECT_MULTIPLE);
		return permissionSelection;
	}

	public void createRole(RoleForm roleForm, Integer appId, FieldProcessor fp) throws BusinessException {
		if (null != appId) {
			RoleImpl role = roleForm.getRole();
			Application application = applicationRepository.findOne(appId);
			role.setApplication(application);
			checkUniqueRoleName(role, role.getName(), fp);
			roleRepository.save(role);
			assignPermissionsToRole(role, roleForm.getPermissionIds(), fp);
		} else {
			throw new BusinessException("no application id given", MessageConstants.ROLE_CREATE_ERROR);
		}
	}

	public void updateRole(RoleForm form, FieldProcessor fp) throws BusinessException {
		RoleImpl role = form.getRole();
		RoleImpl currentRole = roleRepository.findOne(role.getId());
		if (null == currentRole) {
			throw new BusinessException("no such role", MessageConstants.ROLE_NOT_EXISTS);
		}
		checkUniqueRoleName(currentRole, role.getName(), fp);
		getRequest().setPropertyValues(form, new RoleForm(currentRole), fp.getMetaData());
		assignPermissionsToRole(currentRole, form.getPermissionIds(), fp);
	}

	private void checkUniqueRoleName(Role role, String name, FieldProcessor fp) throws BusinessException {
		boolean isUnique = roleRepository.isUnique(role.getId(), new String[] { "name", "application.id" },
				new Object[] { name, role.getApplication().getId() });
		if (!isUnique) {
			fp.addErrorMessage(fp.getField("role.name"), request.getMessage(MessageConstants.ROLE_EXISTS));
			throw new BusinessException(
					"a role " + role.getName() + " already exists for application " + role.getApplication().getName());
		}
	}

	public DataContainer searchApplications(FieldProcessor fp, Integer siteId, Integer appId, boolean assignedOnly)
			throws BusinessException {
		DataContainer data = new DataContainer(fp);

		if (null == siteId) {
			if (null == appId) {
				Page<ApplicationImpl> applications = applicationRepository.search(fp.getPageable());
				data.setPage(applications);
			} else {
				ApplicationImpl application = applicationRepository.findOne(appId);
				if (null == application) {
					throw new BusinessException("no such application: " + appId,
							MessageConstants.APPLICATION_NOT_EXISTS);
				}
				data.setItem(application);
			}
		} else if (null == appId) {
			Pageable pageable = fp.getPageable();
			if (assignedOnly) {
				SearchQuery<SiteApplication> searchQuery = new SearchQuery<SiteApplication>(SiteApplication.class);
				searchQuery.equals("site.id", siteId);
				Page<SiteApplication> applications = siteApplicationRepository.search(searchQuery, pageable);
				data.setPage(applications);
			} else {
				// page uses wrong property path application.x
				Page<ApplicationImpl> allApplications = applicationRepository.search(pageable);
				List<SiteApplication> applications = new ArrayList<SiteApplication>();
				Site site = siteRepository.findOne(siteId);
				for (Application application : allApplications) {
					SiteApplication siteApplication = ((SiteImpl) site).getSiteApplication(application.getName());
					if (siteApplication == null) {
						siteApplication = new SiteApplication();
						siteApplication.setApplication(application);
						siteApplication.setActive(false);
						siteApplication.setReloadRequired(false);
						siteApplication.setMarkedForDeletion(false);
					}
					applications.add(siteApplication);
				}
				Pageable currentPage = new PageRequest(allApplications.getNumber(), allApplications.getSize(),
						allApplications.getSort());
				Page<SiteApplication> page = new PageImpl<SiteApplication>(applications, currentPage,
						allApplications.getTotalElements());
				data.setPage(page);
			}
		}
		return data;
	}

	public void createRepository(RepositoryImpl repository, FieldProcessor fp) throws BusinessException {
		try {
			validateRepository(repository, new RepositoryImpl(), fp);
			createRepository(repository);
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void reloadRepository(Integer repositoryId, FieldProcessor fp) throws BusinessException {
		reloadRepository(fp, repoRepository.findOne(repositoryId));
	}

	protected void reloadRepository(FieldProcessor fp, RepositoryImpl repository) throws BusinessException {
		if (null != repository) {
			try {
				repository.reload();
			} catch (Exception e) {
				handleRepositoryException(fp, repository, e);
			}
		}
	}

	public void updateApplication(Environment env, Application application, FieldProcessor fp)
			throws BusinessException {
		try {
			if (application.getId() != null) {
				ApplicationImpl currentApplication = applicationRepository.findOne(application.getId());
				if (null == currentApplication) {
					fp.addErrorMessage(request.getMessage(MessageConstants.APPLICATION_NOT_EXISTS));
					return;
				}
				synchronizeApplicationResources(env, currentApplication, application.isFileBased());
				getRequest().setPropertyValues(application, currentApplication, fp.getMetaData());
			} else {
				throw new BusinessException("No such application");
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void updateRepository(RepositoryForm repositoryForm, FieldProcessor fp) throws BusinessException {
		RepositoryImpl currentRepository = repoRepository.findOne(repositoryForm.getRepository().getId());
		try {
			if (null == currentRepository) {
				throw new BusinessException("no such repository");
			}
			validateRepository(repositoryForm.getRepository(), currentRepository, fp);
			getRequest().setPropertyValues(repositoryForm, new RepositoryForm(currentRepository), fp.getMetaData());
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
		reloadRepository(fp, currentRepository);
	}

	private void validateRepository(RepositoryImpl repository, RepositoryImpl currentRepository, FieldProcessor fp)
			throws BusinessException {
		if (null == repository.getRepositoryType()) {
			if (null == currentRepository.getRepositoryType()) {
				repository.setRepositoryType(RepositoryType.getDefault());
			} else {
				repository.setRepositoryType(currentRepository.getRepositoryType());
			}
		}
		if (null == repository.getRepositoryMode()) {
			if (null == currentRepository.getRepositoryMode()) {
				repository.setRepositoryMode(RepositoryMode.getDefault());
			} else {
				repository.setRepositoryMode(currentRepository.getRepositoryMode());
			}
		}
		checkUniqueRepositoryName(repository, fp);
		try {
			RepositoryCacheFactory.validateRepositoryURI(repository);
		} catch (BusinessException e) {
			String message = e.getMessage();
			fp.addErrorMessage(message);
			throw e;
		}
	}

	private void checkUniqueRepositoryName(Repository repository, FieldProcessor fp) throws BusinessException {
		boolean isUnique = repoRepository.isUnique(repository.getId(), "name", repository.getName());
		if (!isUnique) {
			fp.addErrorMessage(fp.getField("repository.name"), request.getMessage(MessageConstants.REPOSITORY_EXISTS));
			throw new BusinessException("repository named " + repository.getName() + " already exists!");
		}
	}

	public DataContainer searchSites(FieldProcessor fp, Integer siteId) throws BusinessException {
		DataContainer data = new DataContainer(fp);
		if (siteId != null) {
			SiteImpl site = siteRepository.findOne(siteId);
			if (null == site) {
				throw new BusinessException("no such site: " + siteId, MessageConstants.SITE_NOT_EXISTS);
			}
			addSelectionsForSite(site, data);
			data.setItem(new SiteForm(site));
		} else {
			Map<String, Site> siteMap = environment.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
			Page<SiteImpl> sites = siteRepository.search(fp.getPageable());
			for (SiteImpl siteImpl : sites) {
				Site site = siteMap.get(siteImpl.getName());
				if (null != site) {
					siteImpl.setRunning(SiteState.STARTED.equals(site.getState()));
					siteImpl.setStartupTime(site.getStartupTime());
				}
			}
			data.setPage(sites);
		}
		return data;
	}

	private void addSelectionsForSite(final SiteImpl site, DataContainer data) {
		initSiteProperties(site);
		List<String> templateNames = new ArrayList<String>();
		for (Identifier identifier : getInstalledTemplates()) {
			templateNames.add(identifier.getDisplayName());
		}
		Collections.sort(templateNames);
		String activeTemplate = site.getProperties().getString(SiteProperties.TEMPLATE);
		Selection templateSelection = selectionFactory.fromObjects("template", "template",
				templateNames.toArray(new String[templateNames.size()]), activeTemplate);
		data.getSelections().add(templateSelection);
	}

	private List<Identifier> getInstalledTemplates() {
		return templateService.getInstalledTemplates();
	}

	public void createSite(SiteForm siteForm, FieldProcessor fp) throws BusinessException {
		try {
			SiteImpl site = siteForm.getSite();
			checkSite(site, fp, site);
			createSite(site, environment);
			updateSiteTemplate(site, siteForm.getTemplate());
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void updateSite(SiteForm form, FieldProcessor fp) throws BusinessException {
		try {
			SiteImpl site = form.getSite();
			boolean isActive = site.isActive();
			SiteImpl currentSite = siteRepository.findOne(site.getId());
			if (null == currentSite) {
				throw new BusinessException("no such site:" + site.getId());
			}
			checkSite(site, fp, currentSite);

			boolean wasActiveBefore = currentSite.isActive();
			getRequest().setPropertyValues(form, new SiteForm(currentSite), fp.getMetaData());

			if (isActive ^ wasActiveBefore) {
				String message = request.getMessage(MessageConstants.RELOAD_SITE);
				fp.addNoticeMessage(message);
			}

			updateSiteTemplate(currentSite, form.getTemplate());
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	private void updateSiteTemplate(SiteImpl currentSite, String template) {
		String propertyName = PropertySupport.getPropertyName(currentSite, null, SiteProperties.TEMPLATE);
		propertyRepository.findByName(propertyName).setString(template);
	}

	private void checkSite(Site site, FieldProcessor fp, Site currentSite) throws BusinessException {
		if (fp.hasField("site.name")) {
			if (!siteRepository.isUnique(site.getId(), "name", site.getName())) {
				fp.addErrorMessage(fp.getField("site.name"), request.getMessage(MessageConstants.SITE_NAME_EXISTS));
			}
		}
		if (!siteRepository.isUnique(site.getId(), "host", site.getHost())) {
			fp.addErrorMessage(fp.getField("site.host"), request.getMessage(MessageConstants.SITE_HOST_EXISTS));
		}
		if (!siteRepository.isUnique(site.getId(), "domain", site.getDomain())) {
			fp.addErrorMessage(fp.getField("site.domain"), request.getMessage(MessageConstants.SITE_DOMAIN_EXISTS));
		}
		if (fp.hasErrors()) {
			throw new BusinessException("invalid name, host or domain");
		}
	}

	public DataContainer searchSubjects(Request request, FieldProcessor fp, Integer subjectId, String defaultTimezone,
			List<String> languages) throws BusinessException {
		DataContainer data = new DataContainer(fp);
		if (subjectId != null) {
			SubjectImpl subject = subjectRepository.findOne(subjectId);
			if (null == subject) {
				fp.addErrorMessage(request.getMessage(MessageConstants.SUBJECT_NOT_EXISTS));
				throw new BusinessException("no such subject: " + subjectId, MessageConstants.SUBJECT_NOT_EXISTS);
			}
			SubjectForm subjectsForm = new SubjectForm(subject);
			data.setItem(subjectsForm);
			String timeZone = subject.getTimeZone();
			addSelectionsForSubject(data, subject, timeZone == null ? defaultTimezone : timeZone, languages);
		} else {
			String filterParamType = "f_type";
			String filterParamName = "f_name";
			String typeFormRequest = request.getParameter(filterParamType);
			UserType userType = null != typeFormRequest && UserType.names().contains(typeFormRequest)
					? UserType.valueOf(typeFormRequest) : null;
			String name = request.getParameter(filterParamName);

			SearchQuery<SubjectImpl> searchQuery = subjectRepository.createSearchQuery();
			if (StringUtils.isNotBlank(name)) {
				searchQuery.like("name", "%" + name + "%");
			} else {
				name = "";
			}
			if (null != userType) {
				searchQuery.equals("userType", userType);
			}

			Page<SubjectImpl> subjects = subjectRepository.search(searchQuery, fp.getPageable());
			for (SubjectImpl subject : subjects) {
				subject.setTypeName(getUserTypeNameProvider().getName(subject.getUserType()));
			}

			Selection userTypes = selectionFactory.fromObjects(filterParamType, MessageConstants.TYPE,
					UserType.values(), getUserTypeNameProvider(), userType);
			userTypes.getOptions().add(0, new Option());
			userTypes.setType(SelectionType.SELECT);
			Selection userName = selectionFactory.fromObjects(filterParamName, MessageConstants.NAME,
					new String[] { name }, new String[] { name });
			userName.setType(SelectionType.TEXT);
			SelectionGroup filterGroup = new SelectionGroup();
			filterGroup.getSelections().add(userName);
			filterGroup.getSelections().add(userTypes);
			data.getSelectionGroups().add(filterGroup);
			data.setPage(subjects);
		}
		return data;
	}

	private void addSelectionsForSubject(DataContainer data, SubjectImpl subject, String timezone,
			List<String> languages) {
		List<? extends Group> allGroups = groupRepository.findAll(new Sort(Direction.ASC, "name"));
		Selection selection = selectionFactory.fromNamed("groups", MessageConstants.GROUPS, allGroups,
				subject.getGroups());
		data.getSelections().add(selection);
		NameProvider<UserType> nameProvider = getUserTypeNameProvider();
		Selection userTypeSelection = selectionFactory.fromEnum("userType", MessageConstants.TYPE, UserType.values(),
				subject.getUserType(), nameProvider);

		data.getSelections().add(userTypeSelection);
		Selection localeSelection = selectionFactory.fromObjects("language", MessageConstants.LANGUAGE,
				languages.toArray(), subject.getLanguage());
		data.getSelections().add(localeSelection);

		Selection timezoneSelection = getTimezoneSelection(timezone);
		data.getSelections().add(timezoneSelection);
	}

	private Selection getTimezoneSelection(String timeZone) {

		Selection timezoneSelection = new Selection();
		timezoneSelection.setId("timeZone");
		Label value = new Label();
		value.setId(MessageConstants.TIMEZONE);
		timezoneSelection.setTitle(value);
		timezoneSelection.setType(SelectionType.SELECT);
		List<String> ids = Arrays.asList(TimeZone.getAvailableIDs());
		Collections.sort(ids);
		List<TimeZone> timeZones = new ArrayList<TimeZone>();
		for (String id : ids) {
			if (id.matches("(Africa|America|Antarctica|Asia|Atlantic|Australia|Europe|Indian|Pacific).*")) {
				timeZones.add(TimeZone.getTimeZone(id));
			}
		}

		Locale locale = environment.getLocale();
		String groupName = "";
		for (TimeZone tz : timeZones) {
			String id = tz.getID();
			int separatorIdx = id.indexOf('/');
			String area = id.substring(0, separatorIdx);
			String location = id.substring(id.indexOf('/') + 1);
			String areaKey = "timezone." + area;
			List<org.appng.xml.platform.OptionGroup> optionGroups = timezoneSelection.getOptionGroups();
			org.appng.xml.platform.OptionGroup group;
			if (!groupName.equals(area)) {
				group = new org.appng.xml.platform.OptionGroup();
				String areaName = timezoneMessages.getMessage(areaKey, new Object[0], locale);
				Label optLabel = new Label();
				group.setLabel(optLabel);
				optLabel.setValue(areaName);
				optionGroups.add(group);
			} else {
				group = optionGroups.get(optionGroups.size() - 1);
			}

			groupName = area;
			Option opt = new Option();
			String locationName = timezoneMessages.getMessage(areaKey + "." + location, new Object[0], locale);
			double offset = (double) tz.getRawOffset() / 1000 / 60 / 60;
			int hours = (int) offset;
			int minutes = Math.abs((int) (60 * (double) (offset - hours)));
			String gmtTimezone = "GMT" + (offset >= 0 ? "+" : "") + StringUtils.leftPad(String.valueOf(hours), 2, "0")
					+ ":" + StringUtils.leftPad(String.valueOf(minutes), 2, "0");
			opt.setName(locationName + " (" + gmtTimezone + ")");
			opt.setValue(id);
			opt.setSelected(id.equals(timeZone));
			group.getOptions().add(opt);

			Collections.sort(group.getOptions(), new Comparator<Option>() {
				public int compare(Option o1, Option o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		}
		return timezoneSelection;
	}

	private NameProvider<UserType> getUserTypeNameProvider() {
		return new NameProvider<UserType>() {
			public String getName(UserType instance) {
				return request.getMessage(UserType.class.getSimpleName() + "." + instance.name());
			}
		};
	}

	public void createSubject(Locale locale, SubjectForm form, FieldProcessor fp) throws BusinessException {
		try {
			SubjectImpl subject = form.getSubject();
			SubjectImpl subjectByName = getSubjectByName(subject.getName(), false);
			if (null != subjectByName) {
				fp.addErrorMessage(fp.getField("subject.name"), request.getMessage(MessageConstants.SUBJECT_EXISTS));
				throw new BusinessException("subject '" + subject.getName() + "' already exists");
			}

			if (form.isLocalUser()) {
				updatePassword(form.getPassword().toCharArray(), form.getPasswordConfirmation().toCharArray(), subject);
			}
			subjectRepository.save(subject);
			assignGroupsToSubject(subject.getId(), form.getGroupIds(), fp);
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public Boolean updateSubject(SubjectForm subjectForm, FieldProcessor fp) throws BusinessException {
		Boolean updated = false;
		SubjectImpl subject = subjectForm.getSubject();
		try {
			if (subject.getId() != null) {
				SubjectImpl currentSubject = subjectRepository.findOne(subject.getId());
				if (null == currentSubject) {
					fp.addErrorMessage(request.getMessage(MessageConstants.SUBJECT_NOT_EXISTS));
				}
				if (!StringUtils.isEmpty(subjectForm.getPassword())
						&& !StringUtils.isEmpty(subjectForm.getPasswordConfirmation())) {
					updated = updatePassword(subjectForm.getPassword().toCharArray(),
							subjectForm.getPasswordConfirmation().toCharArray(), currentSubject);
				}
				assignGroupsToSubject(subject.getId(), subjectForm.getGroupIds(), fp);
				getRequest().setPropertyValues(subjectForm, new SubjectForm(currentSubject), fp.getMetaData());
			} else {
				throw new BusinessException("No such user exists");
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
		return updated;
	}

	public void assignGroupsToSubject(Integer subjectId, List<Integer> groupIds, FieldProcessor fp)
			throws BusinessException {
		try {
			assignGroupsToSubject(subjectId, groupIds, true);
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void assignPermissionsToRole(Role role, List<Integer> permissionsIds, FieldProcessor fp)
			throws BusinessException {
		try {
			role.getPermissions().clear();
			for (Integer id : permissionsIds) {
				Permission permission = permissionRepository.findOne(id);
				role.getPermissions().add(permission);
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public MigrationStatus assignApplicationToSite(Integer siteId, Integer appId, FieldProcessor fp)
			throws BusinessException {
		return assignApplicationToSite(environment, siteId, appId, fp, true);
	}

	public MigrationStatus removeApplicationFromSite(Integer siteId, Integer appId, FieldProcessor fp)
			throws BusinessException {
		return assignApplicationToSite(environment, siteId, appId, fp, false);
	}

	private MigrationStatus assignApplicationToSite(Environment environment, Integer siteId, Integer appId,
			FieldProcessor fp, boolean assign) throws BusinessException {

		MigrationStatus migrationStatus = null;
		SiteImpl site = siteRepository.findOne(siteId);
		Application application = applicationRepository.findOne(appId);
		SiteApplication siteApplication = siteApplicationRepository.findOne(new SiteApplicationPK(siteId, appId));
		boolean isAssigned = null != siteApplication;
		if (isAssigned) {
			boolean hasConnection = null != siteApplication.getDatabaseConnection();
			migrationStatus = hasConnection ? MigrationStatus.DB_SUPPORTED : MigrationStatus.NO_DB_SUPPORTED;
		}
		Site liveSite = RequestUtil.getSiteByName(environment, site.getName());
		SiteApplication liveApplication = null;
		if (null != liveSite) {
			liveApplication = ((SiteImpl) liveSite).getSiteApplication(application.getName());
		}
		boolean isLive = null != liveApplication;
		if (assign) {
			if (isAssigned) {
				siteApplication.setActive(true);
				siteApplication.setMarkedForDeletion(false);
				siteApplication.setReloadRequired(!siteApplication.isReloadRequired());
				auditableListener.createEvent(Type.INFO,
						String.format("Assigned application %s to site %s", application.getName(), site.getName()));
			} else {
				migrationStatus = assignApplicationToSite(site, application, true);
				switch (migrationStatus) {
				case ERROR:
					fp.addErrorMessage(request.getMessage(MessageConstants.MIGRATION_FAILED));
					break;
				case DB_MIGRATED:
					fp.addOkMessage(request.getMessage(MessageConstants.MIGRATION_SUCCESS));
					break;
				case NO_DB_SUPPORTED:
					fp.addOkMessage(request.getMessage(MessageConstants.MIGRATION_NO_DB_SUPPORTED));
					break;
				case DB_SUPPORTED:
					fp.addNoticeMessage(request.getMessage(MessageConstants.MIGRATION_DB_SUPPORTED));
					break;
				case DB_NOT_AVAILABLE:
					fp.addErrorMessage(request.getMessage(MessageConstants.MIGRATION_DB_NOT_AVAILABLE));
				}
			}
		} else if (isAssigned) {
			logger.debug("removing application '" + application.getName() + "' from site '" + site.getName() + "'");
			if (!isLive) {
				migrationStatus = unlinkApplicationFromSite(siteApplication);
				DatabaseConnection connection = siteApplication.getDatabaseConnection();
				switch (migrationStatus) {
				case ERROR:
					fp.addOkMessage(
							request.getMessage(MessageConstants.MIGRATION_DB_DELETE_FAILED, connection.getJdbcUrl()));
					break;
				case DB_MIGRATED:
					fp.addOkMessage(request.getMessage(MessageConstants.MIGRATION_DB_DELETED, connection.getJdbcUrl()));
					break;
				case DB_SUPPORTED:
					fp.addOkMessage(
							request.getMessage(MessageConstants.MIGRATION_DB_NOT_DELETED, connection.getJdbcUrl()));
					break;
				default:
					break;
				}
			} else {
				auditableListener.createEvent(Type.INFO, String.format("Removed application %s from site %s",
						siteApplication.getApplication().getName(), site.getName()));
				siteApplication.setActive(false);
				siteApplication.setMarkedForDeletion(true);
				siteApplication.setReloadRequired(!siteApplication.isReloadRequired());
			}
		}
		return migrationStatus;
	}

	public void createPermission(PermissionImpl permission, Integer appId, FieldProcessor fp) throws BusinessException {
		if (null != appId) {
			ApplicationImpl application = applicationRepository.findOne(appId);
			permission.setApplication(application);
			checkUniquePermissionName(permission, permission.getName(), fp);
			permissionRepository.save(permission);
		} else {
			throw new BusinessException("no application ID provided");
		}
	}

	public void updatePermission(Permission permission, FieldProcessor fp) throws BusinessException {
		if (permission.getId() != null) {
			PermissionImpl currentPermission = permissionRepository.findOne(permission.getId());
			if (null == currentPermission) {
				throw new BusinessException("No such permission!", MessageConstants.PERMISSION_NOT_EXISTS);
			}
			checkUniquePermissionName(currentPermission, permission.getName(), fp);
			getRequest().setPropertyValues(permission, currentPermission, fp.getMetaData());
		} else {
			throw new BusinessException("No such permission!", MessageConstants.PERMISSION_NOT_EXISTS);
		}
	}

	private void checkUniquePermissionName(Permission permission, String name, FieldProcessor fp)
			throws BusinessException {
		boolean isUnique = permissionRepository.isUnique(permission.getId(), new String[] { "name", "application.id" },
				new Object[] { name, permission.getApplication().getId() });
		if (!isUnique) {
			fp.addErrorMessage(fp.getField("name"), request.getMessage(MessageConstants.PERMISSION_EXISTS));
			throw new BusinessException("a permission named '" + permission.getName()
					+ "' already exists for application '" + permission.getApplication().getName() + "'");
		}
	}

	public DataContainer searchPermissions(FieldProcessor fp, Integer permissionId, Integer appId)
			throws BusinessException {
		DataContainer data = new DataContainer(fp);
		if (permissionId != null) {
			PermissionImpl permission = permissionRepository.findOne(permissionId);
			if (null == permission) {
				throw new BusinessException("no such permission " + permissionId,
						MessageConstants.PERMISSION_NOT_EXISTS);
			}
			data.setItem(permission);
		} else {
			SearchQuery<PermissionImpl> query = new SearchQuery<PermissionImpl>(PermissionImpl.class)
					.equals("application.id", appId);
			Page<PermissionImpl> permissions = permissionRepository.search(query, fp.getPageable());
			data.setPage(permissions);
		}
		return data;
	}

	public DataContainer searchProperties(FieldProcessor fp, Integer siteId, Integer appId, String propertyName)
			throws BusinessException {
		DataContainer data = new DataContainer(fp);
		if (propertyName != null && propertyName.length() > 0) {
			PropertyImpl property = propertyRepository.findOne(propertyName);
			if (null == property) {
				throw new BusinessException("no such property " + propertyName, MessageConstants.PROPERTY_NOT_EXISTS);
			}
			data.setItem(new PropertyForm(property));
		} else {
			Page<PropertyImpl> properties = getProperties(siteId, appId, fp.getPageable());
			data.setPage(properties);
		}
		return data;
	}

	public void createProperty(PropertyForm propertyForm, Integer siteId, Integer appId, FieldProcessor fp)
			throws BusinessException {
		try {
			PropertyImpl property = propertyForm.getProperty();
			if (checkPropertyExists(siteId, appId, property)) {
				fp.addErrorMessage(request.getMessage(MessageConstants.PROPERTY_EXISTS));
				throw new BusinessException("property already exists!");
			} else {
				createProperty(siteId, appId, property);
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void updateProperty(PropertyForm propertyForm, FieldProcessor fp) throws BusinessException {
		try {
			PropertyImpl property = propertyForm.getProperty();
			if (property.getName() != null) {
				PropertyImpl currentProperty = propertyRepository.findOne(property.getName());
				if (null == currentProperty) {
					throw new BusinessException("no such property");
				}
				getRequest().setPropertyValues(propertyForm, new PropertyForm(currentProperty), fp.getMetaData());
			} else {
				throw new BusinessException("no propertyname given");
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void deleteProperty(String id, FieldProcessor fp) throws BusinessException {
		PropertyImpl prop = propertyRepository.findOne(id);
		try {
			if (null != prop) {
				deleteProperty(prop);
			} else {
				throw new BusinessException("No such property " + id);
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public void reloadSite(Application application, Integer siteId, FieldProcessor fp) throws BusinessException {
		try {
			InitializerService initializerService = application.getBean(InitializerService.class);
			SiteImpl site = getSite(siteId);
			if (null != site) {
				String siteName = site.getName();
				if (site.isActive()) {
					try {
						initializerService.loadSite(environment, site, fp);
						logger.info("Site reloaded: " + siteName);
					} catch (InvalidConfigurationException e) {
						throw new BusinessException("Invalid configuration for site: " + siteName, e);
					}
				} else {
					shutdownSite(environment, siteName);
				}
			}
		} catch (Exception e) {
			getRequest().handleException(fp, e);
		}
	}

	public DataContainer getNewSubject(FieldProcessor fp, String timezone, List<String> languages) {
		DataContainer data = new DataContainer(fp);
		SubjectForm subjectsForm = new SubjectForm();
		SubjectImpl subject = subjectsForm.getSubject();
		subject.setGroups(new ArrayList<Group>());
		subject.setLanguage("");
		subject.setRealname("");
		subject.setName("");
		subject.setEmail("");
		subject.setUserType(UserType.LOCAL_USER);
		subject.setTimeZone(timezone);
		data.setItem(subjectsForm);
		addSelectionsForSubject(data, subject, timezone, languages);
		return data;
	}

	public DataContainer getNewPermission(FieldProcessor fp) {
		DataContainer data = new DataContainer(fp);
		PermissionImpl permission = new PermissionImpl();
		permission.setDescription("");
		permission.setName("");
		data.setItem(permission);
		return data;
	}

	public DataContainer getNewGroup(Site site, FieldProcessor fp) {
		DataContainer data = new DataContainer(fp);
		GroupForm groupForm = new GroupForm();
		GroupImpl group = groupForm.getGroup();
		group.setDescription("");
		group.setName("");
		group.setSubjects(new HashSet<Subject>());
		data.setItem(groupForm);

		Selection roleSelection = getRoleSelection(group, site.getId());
		data.getSelections().add(roleSelection);
		return data;
	}

	public DataContainer getNewSite(FieldProcessor fp) {
		DataContainer data = new DataContainer(fp);
		SiteForm sitesForm = new SiteForm();
		SiteImpl site = sitesForm.getSite();
		site.setActive(false);
		site.setCreateRepository(false);
		site.setDescription("");
		site.setHost("");
		site.setName("");
		site.setDomain("");
		site.setProperties(null);
		data.setItem(sitesForm);
		addSelectionsForSite(site, data);
		return data;
	}

	public DataContainer getNewRepository(FieldProcessor fp) {
		DataContainer data = new DataContainer(fp);
		try {
			RepositoryImpl repository = new RepositoryImpl();
			repository.setActive(false);
			repository.setDescription("");
			repository.setName("");
			repository.setRepositoryType(RepositoryType.getDefault());
			repository.setUri(new URI(""));
			repository.setPublished(false);
			repository.setRepositoryMode(RepositoryMode.getDefault());

			data.setItem(new RepositoryForm(repository));
			Selection repositoryTypeSelection = getRepositoryTypeSelection(null);
			if (null != repositoryTypeSelection) {
				data.getSelections().add(repositoryTypeSelection);
			}
			Selection repositoryModeSelection = getRepositoryModeSelection(null);
			if (null != repositoryModeSelection) {
				data.getSelections().add(repositoryModeSelection);
			}
		} catch (URISyntaxException e) {
			// may occur very unlikely while using infinite improbability drive
			logger.error("", e);
		}
		return data;
	}

	private Selection getRepositoryTypeSelection(RepositoryType repositoryType) {
		return getTypeSelection(RepositoryType.class, repositoryType, "repositoryType", "repositoryType");
	}

	private Selection getRepositoryModeSelection(RepositoryMode repositoryMode) {
		return getTypeSelection(RepositoryMode.class, repositoryMode, "repositoryMode", "repositoryMode");
	}

	private <E extends Enum<E>> Selection getTypeSelection(final Class<E> clazz, final E type, String id,
			String title) {
		NameProvider<E> nameProvider = new NameProvider<E>() {
			public String getName(E instance) {
				return request.getMessage(clazz.getSimpleName() + "." + instance.name());
			}
		};
		return selectionFactory.fromEnum(id, title, clazz.getEnumConstants(), type, nameProvider);
	}

	public DataContainer getNewRole(FieldProcessor fp, Integer appId) {
		DataContainer data = new DataContainer(fp);
		RoleForm rolesForm = new RoleForm();
		RoleImpl role = rolesForm.getRole();
		data.setItem(rolesForm);
		data.getSelections().add(getPermissionSelection(appId, role));
		return data;
	}

	public DataContainer getNewProperty(FieldProcessor fp) {
		DataContainer data = new DataContainer(fp);
		PropertyForm propertiesForm = new PropertyForm();
		data.setItem(propertiesForm);
		return data;
	}

	public List<JarInfo> getJars(Environment environment, Integer siteId) {
		List<JarInfo> jarInfos;
		if (null != siteId) {
			SiteImpl site = siteRepository.findOne(siteId);
			jarInfos = environment.getAttribute(Scope.PLATFORM, site.getName() + "." + EnvironmentKeys.JAR_INFO_MAP);
		} else {
			jarInfos = environment.getAttribute(Scope.PLATFORM,
					Platform.Environment.PLATFORM_CONFIG + "." + EnvironmentKeys.JAR_INFO_MAP);
		}
		if (null != jarInfos) {
			Collections.sort(jarInfos);
		}
		return jarInfos;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public void updateDatabaseConnection(FieldProcessor fp, DatabaseConnection databaseConnection) {
		DatabaseConnection current = databaseConnectionRepository.findOne(databaseConnection.getId());
		byte[] currentPassword = current.getPassword();
		request.setPropertyValues(databaseConnection, current, fp.getMetaData());
		if (StringUtils.isBlank(current.getPasswordPlain())) {
			current.setPassword(currentPassword);
		}
		boolean isConnectionWorking = testConnection(fp, current, false);
		if (current.isActive() && !isConnectionWorking) {
			fp.addNoticeMessage(request.getMessage(MessageConstants.CONNECTION_NOT_ACTIVE, current.getName()));
			current.setActive(false);
		} else if (!current.isRootConnection()) {
			current.setActive(isConnectionWorking);
		}
		fp.addOkMessage(request.getMessage(MessageConstants.CONNECTION_UPDATED));
	}

	public void createDatabaseConnection(FieldProcessor fp, DatabaseConnection databaseConnection, Integer siteId) {
		if (null != siteId) {
			Site site = getSite(siteId);
			databaseConnection.setSite(site);
		}
		createDatabaseConnection(databaseConnection, false);
		fp.addOkMessage(request.getMessage(MessageConstants.CONNECTION_CREATED));
		testConnection(fp, databaseConnection, true);
	}

	private boolean testConnection(FieldProcessor fp, DatabaseConnection current, boolean addError) {
		StringBuilder dbInfo = new StringBuilder();
		if (current.testConnection(dbInfo)) {
			fp.addOkMessage(request.getMessage(MessageConstants.CONNECTION_SUCCESSFULL, dbInfo.toString()));
			return true;
		} else if (addError) {
			fp.addErrorMessage(request.getMessage(MessageConstants.CONNECTION_FAILED));
		}
		return false;
	}

	public void testConnection(FieldProcessor fp, Integer connectionId) {
		DatabaseConnection databaseConnection = getDatabaseConnection(connectionId, false);
		testConnection(fp, databaseConnection, true);
	}

	public void deleteDatabaseConnection(FieldProcessor fp, Integer conId) {
		DatabaseConnection current = databaseConnectionRepository.findOne(conId);
		if (null != current) {
			// TODO check if in use
			databaseConnectionRepository.delete(current);
			fp.addOkMessage(request.getMessage(MessageConstants.CONNECTION_DELETED));
		} else {
			fp.addErrorMessage(request.getMessage(MessageConstants.CONNECTION_NO_SUCH_ID));
		}
	}

	public Collection<? extends Role> findRolesForSite(Integer siteId) {
		return roleRepository.findRolesForSite(siteId);
	}

	public void resetConnection(Integer conId) {
		resetConnection(null, conId);
	}

	public SiteApplication getSiteApplication(Integer siteId, Integer appId) {
		SiteApplication siteApplication = siteApplicationRepository.findOne(new SiteApplicationPK(siteId, appId));
		siteApplication.getGrantedSites().size();
		return siteApplication;
	}

	public List<Selection> getGrantedSelections(Integer siteId, Integer appId) {

		List<SiteImpl> grantedBy = new ArrayList<SiteImpl>();

		SiteApplication siteApplication = getSiteApplication(siteId, appId);
		Site grantedSite = siteApplication.getSite();
		Application application = siteApplication.getApplication();
		List<SiteImpl> allSites = siteRepository.findAll(new Sort("name"));
		allSites.remove(grantedSite);
		for (SiteImpl site : new ArrayList<SiteImpl>(allSites)) {
			SiteApplication granted = siteApplicationRepository
					.findByApplicationNameAndGrantedSitesName(application.getName(), site.getName());
			if (null != granted && !granted.equals(siteApplication)) {
				allSites.remove(site);
				grantedBy.add(site);
			}
		}

		Selection granted = selectionFactory.fromNamed("siteApplication.grantedSites", "sites", allSites,
				siteApplication.getGrantedSites());
		Selection grantedSitesBy = selectionFactory.fromNamed("grantedBy", "grantedBy", grantedBy, grantedBy);
		List<Selection> selections = new ArrayList<Selection>();
		selections.add(granted);
		selections.add(grantedSitesBy);
		return selections;
	}

	public void grantSites(Integer siteId, Integer appId, Set<Integer> grantedSiteIds) {
		SiteApplication siteApplication = getSiteApplication(siteId, appId);
		siteApplication.getGrantedSites().clear();
		List<SiteImpl> sites = siteRepository.findAll(grantedSiteIds);
		siteApplication.getGrantedSites().addAll(sites);
	}

	public String addArchiveToRepository(Integer repositoryId, FormUpload archive, FieldProcessor fp) {
		String name = null;
		File file = archive.getFile();
		String originalFilename = archive.getOriginalFilename();
		RepositoryImpl repo = repoRepository.findOne(repositoryId);
		RepositoryMode repositoryMode = repo.getRepositoryMode();
		PackageArchive packageArchive = RepositoryUtils.getPackage(repo, file, originalFilename);
		if (null != packageArchive && RepositoryType.LOCAL.equals(repo.getRepositoryType())) {
			File targetFolder = new File(repo.getUri().getPath());
			try {
				File targetArchive = new File(targetFolder, originalFilename);
				boolean existedBefore = targetArchive.exists();
				FileUtils.copyFile(file, targetArchive);
				if (existedBefore) {
					String message = request.getMessage(MessageConstants.REPOSITORY_ARCHIVE_REPLACED, originalFilename);
					fp.addNoticeMessage(message);
				}
				logger.info("copied archive from {} to {}", file, targetArchive);
				name = packageArchive.getPackageInfo().getName();
			} catch (IOException e) {
				throw new ApplicationException("error while copying archive to " + targetFolder, e);
			}
		} else {
			String message = request.getMessage(MessageConstants.REPOSITORY_ARCHIVE_WRONG_MODE, repositoryMode,
					(repositoryMode.equals(RepositoryMode.STABLE) ? "snapshot" : "stable"));
			fp.addErrorMessage(fp.getField("archive"), message);
		}
		FileUtils.deleteQuietly(file);
		return name;
	}

	public RepositoryImpl getRepository(Integer repositoryId) {
		return repoRepository.findOne(repositoryId);
	}

	public String getNameForSite(Integer siteId) {
		Site site = siteRepository.findOne(siteId);
		return site == null ? null : site.getName();
	}

	public MessageSource getTimezoneMessages() {
		return timezoneMessages;
	}

	public void setTimezoneMessages(MessageSource timezoneMessages) {
		this.timezoneMessages = timezoneMessages;
	}

	public List<AppngCache> getCacheEntries(Integer siteId) {
		return super.getCacheEntries(siteId);
	}

	public Map<String, String> getCacheStatistics(Integer siteId) {
		return super.getCacheStatistics(siteId);
	}

	public void expireCacheElement(FieldProcessor fp, Request request, Integer siteId, String cacheElement) {
		String cacheElementNotExists = MessageConstants.CACHE_ELEMENT_NOT_EXISTS;
		String cacheElementDeleted = MessageConstants.CACHE_ELEMENT_DELETED;
		try {
			expireCacheElement(siteId, cacheElement);
			fp.addNoticeMessage(request.getMessage(cacheElementDeleted));
		} catch (BusinessException e) {
			fp.addErrorMessage(request.getMessage(cacheElementNotExists));
		}
	}

	public void clearCacheStatistics(FieldProcessor fp, Request request, Integer siteId) {
		String cacheStatisticsCleared = MessageConstants.CACHE_STATISTICS_CLEARED;
		clearCacheStatistics(siteId);
		fp.addNoticeMessage(request.getMessage(cacheStatisticsCleared));
	}

	public void clearCache(FieldProcessor fp, Request request, Integer siteId) {
		String cacheCleared = MessageConstants.CACHE_CLEARED;
		clearCache(siteId);
		fp.addNoticeMessage(request.getMessage(cacheCleared));
	}

	public void deleteTemplate(String name, FieldProcessor fp) {
		Integer code = deleteTemplate(name);
		String message = request.getMessage("template.delete.status." + code, name);
		if (0 == code) {
			fp.addOkMessage(message);
		} else {
			fp.addErrorMessage(message);
		}
	}

	public List<Identifier> listTemplates() {
		return templateService.getInstalledTemplates();
	}

	public void createEvent(Type type, String message) {
		auditableListener.createEvent(type, message);
	}

}
