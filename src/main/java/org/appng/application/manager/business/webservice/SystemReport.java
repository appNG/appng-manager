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
package org.appng.application.manager.business.webservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.appng.api.ApplicationException;
import org.appng.api.AttachmentWebservice;
import org.appng.api.BusinessException;
import org.appng.api.Environment;
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.RequestUtil;
import org.appng.api.Scope;
import org.appng.api.SiteProperties;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.business.LogConfig;
import org.appng.application.manager.service.Service;
import org.appng.tools.os.Command;
import org.appng.tools.os.OperatingSystem;
import org.appng.tools.os.StringConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
@org.springframework.context.annotation.Scope("request")
public class SystemReport implements AttachmentWebservice {

	private static final String EXT_TXT = ".txt";
	private static final String EXT_PROPERTIES = ".properties";
	private static final String EXT_ZIP = ".zip";
	private static final String DATE_PATTERN = "yyyy-MM-dd-HH-mm";
	private static final String PERM_REPORT = "platform.report";

	@Autowired
	private LogConfig logConfig;

	@Autowired
	private ThreadViewer threadViewer;

	@Autowired
	private LogViewer logViewer;

	@Autowired
	private Service service;

	@Value("${platform." + Platform.Property.PLATFORM_ROOT_PATH + "}")
	private String rootPath;

	public byte[] processRequest(Site site, Application application, Environment environment, Request request)
			throws BusinessException {
		if (environment.isSubjectAuthenticated() && request.getPermissionProcessor().hasPermission(PERM_REPORT)) {
			try (
					final ByteArrayOutputStream out = new ByteArrayOutputStream();
					final ArchiveOutputStream os = new ArchiveStreamFactory()
							.createArchiveOutputStream(ArchiveStreamFactory.ZIP, out);) {
				((ZipArchiveOutputStream) os).setLevel(Deflater.BEST_COMPRESSION);

				addProperties(os, "system-properties" + EXT_PROPERTIES, System.getProperties());
				addProperties(os, "system-environment" + EXT_PROPERTIES, System.getenv());

				org.appng.api.model.Properties platformProps = environment.getAttribute(Scope.PLATFORM,
						Platform.Environment.PLATFORM_CONFIG);
				addProperties(os, "platform" + EXT_PROPERTIES, platformProps.getPlainProperties());

				for (String siteName : RequestUtil.getSiteNames(environment)) {
					Site s = RequestUtil.getSiteByName(environment, siteName);
					String sitePropsName = s.getName() + "/" + siteName + EXT_PROPERTIES;
					addProperties(os, sitePropsName, s.getProperties().getPlainProperties());
					String rootDir = s.getProperties().getString(SiteProperties.SITE_ROOT_DIR);
					String indexDir = s.getProperties().getString(SiteProperties.INDEX_DIR);
					addFile(os, new File(rootDir, indexDir), s.getName() + indexDir);
				}

				String jars = StringUtils.join(service.getJars(environment, null), Constants.NEW_LINE);
				addArchiveEntry(os, "lib" + EXT_TXT, jars);
				addArchiveEntry(os, "directories" + EXT_TXT, listDirectory(rootPath));
				addArchiveEntry(os, "threads" + EXT_TXT, threadViewer.getThreadDump(null, null));
				addArchiveEntry(os, logViewer.getLogfile());
				addArchiveEntry(os, logConfig.getConfigFile());
				addArchiveEntry(os, new File(rootPath, "WEB-INF/conf/appNG" + EXT_PROPERTIES));
				return out.toByteArray();
			} catch (IOException e) {
				throw new ApplicationException(e);
			} catch (ArchiveException e) {
				throw new ApplicationException(e);
			}
		}
		return new byte[0];
	}

	private void addFile(ArchiveOutputStream os, File srcFile, String currentFolder)
			throws FileNotFoundException, IOException {
		if (null != srcFile && srcFile.exists()) {
			for (File file : srcFile.listFiles()) {
				String path = currentFolder + "/" + file.getName();
				if (file.isDirectory()) {
					addFile(os, file, path);
				} else {
					addArchiveEntry(os, path, file);
				}
			}
		}
	}

	private void addProperties(ArchiveOutputStream os, String name, Map<?, ?> map) throws IOException {
		ByteArrayOutputStream propsOut = new ByteArrayOutputStream();
		for (Entry<String, ?> entry : org.appng.application.manager.business.Environment.getSortedEntries(map)) {
			propsOut.write((entry.getKey() + "=" + entry.getValue() + Constants.NEW_LINE).getBytes());
		}
		addArchiveEntry(os, name, new ByteArrayInputStream(propsOut.toByteArray()), null);
	}

	private void addArchiveEntry(ArchiveOutputStream os, File file) throws FileNotFoundException, IOException {
		addArchiveEntry(os, file.getName(), file);
	}

	private void addArchiveEntry(ArchiveOutputStream os, String name, File file)
			throws FileNotFoundException, IOException {
		if (file.exists()) {
			addArchiveEntry(os, name, new FileInputStream(file), file.lastModified());
		}
	}

	private void addArchiveEntry(ArchiveOutputStream os, String name, String content) throws IOException {
		addArchiveEntry(os, name, new ByteArrayInputStream(content.getBytes()), null);
	}

	private void addArchiveEntry(ArchiveOutputStream os, String name, InputStream is, Long lastModified)
			throws IOException {
		try {
			ZipArchiveEntry entry = new ZipArchiveEntry(name);
			if (null != lastModified) {
				entry.setTime(lastModified);
			}
			os.putArchiveEntry(entry);
			IOUtils.copy(is, os);
			os.closeArchiveEntry();
		} catch (IOException e) {
			is.close();
			throw e;
		}
	}

	public String getContentType() {
		return "application/zip";
	}

	public String getFileName() {
		String hostname = null;
		try {
			InetAddress addr = InetAddress.getLocalHost();
			hostname = addr.getCanonicalHostName();
		} catch (UnknownHostException e) {
			// ignore
		}
		return "systemreport" + (null == hostname ? "" : "-" + hostname) + "-"
				+ DateFormatUtils.format(new Date(), DATE_PATTERN) + EXT_ZIP;
	}

	public boolean isAttachment() {
		return true;
	}

	private String listDirectory(String root) {
		StringBuilder sb = new StringBuilder();
		StringConsumer outputConsumer = new StringConsumer();
		if (OperatingSystem.isWindows()) {
			Command.execute("cmd /c dir /q /s " + root, outputConsumer, null);
		} else {
			Command.execute("ls -lR " + root, outputConsumer, null);
		}
		List<String> lines = outputConsumer.getResult();
		if (null != lines) {
			sb.append(StringUtils.join(lines, Constants.NEW_LINE));
		}
		return sb.toString();
	}

}
