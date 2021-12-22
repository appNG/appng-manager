/*
 * Copyright 2011-2021 the original author or authors.
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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.FieldProcessor;
import org.appng.api.FileUpload.Unit;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.tools.ui.StringNormalizer;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("env")
public class Environment implements DataProvider {

	private static final double HIGH_TRESHOLD = 0.85d;
	private static final double MEDIUM_TRESHOLD = 0.75d;

	public DataContainer getData(Site site, Application app, org.appng.api.Environment env, Options opts,
			Request request, FieldProcessor fp) {
		String action = opts.getString("mode", "id");
		Map<?, ?> entryMap = null;
		boolean paginate = true;

		switch (action) {
		case "env":
			entryMap = System.getenv();
			break;
		case "props":
			entryMap = System.getProperties();
			break;
		case "jvm":
			Map<String, String> jvm = new HashMap<String, String>();
			List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
			for (String arg : inputArguments) {
				int idx = arg.indexOf('=');
				String key = idx > 0 ? arg.substring(0, idx) : arg;
				String value = idx > 0 ? arg.substring(idx + 1) : StringUtils.EMPTY;
				if (!jvm.containsKey(key)) {
					jvm.put(key, value);
				} else {
					jvm.replace(key, jvm.get(key) + StringUtils.LF + value);
				}
			}
			entryMap = jvm;
			break;
		case "mem":
			Map<String, LeveledEntry> memory = new HashMap<String, LeveledEntry>();
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			addUsage(memory, mBeanServer, "Heap", "Memory", null, "HeapMemoryUsage");
			addUsage(memory, mBeanServer, "Metaspace", "MemoryPool", "Metaspace", "Usage");
			entryMap = memory;
			paginate = false;
			break;
		case "proc":
			Map<String, String> proc = new HashMap<String, String>();
			OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
			proc.put("Processors", Integer.toString(osMxBean.getAvailableProcessors()));
			proc.put("Average Load", Double.toString(osMxBean.getSystemLoadAverage()));
			entryMap = proc;
			paginate = false;
			break;
		}

		DataContainer dataContainer = new DataContainer(fp);
		List<Entry<String, ?>> sortedEntries = getSortedEntries(entryMap);
		if (paginate) {
			dataContainer.setPage(sortedEntries, fp.getPageable());
		} else {
			dataContainer.setItems(sortedEntries);
		}
		return dataContainer;
	}

	protected void addUsage(Map<String, LeveledEntry> entryMap, MBeanServer mBeanServer, String entryName, String type,
			String name, String attributeName) {
		try {
			DecimalFormat format = new DecimalFormat("  #,###,000  " + Unit.KB.name());
			ObjectName objectName = new ObjectName("java.lang:type=" + type + (null == name ? "" : (",name=" + name)));
			CompositeData attribute = (CompositeData) mBeanServer.getAttribute(objectName, attributeName);
			long committed = (long) attribute.get("committed");
			long max = (long) attribute.get("max");
			long used = (long) attribute.get("used");
			long factor = Unit.KB.getFactor();
			entryMap.put(entryName + " Size", new LeveledEntry(format.format((double) committed / factor)));
			entryMap.put(entryName + " Max", new LeveledEntry(max < 0 ? "?" : format.format((double) max / factor)));
			entryMap.put(entryName + " Used", new LeveledEntry(format.format((double) used / factor)));
			if (max > 0) {
				double percentage = (double) used / (double) max;
				int level = percentage < MEDIUM_TRESHOLD ? LeveledEntry.LOW
						: (percentage < HIGH_TRESHOLD ? LeveledEntry.MED : LeveledEntry.HIGH);
				entryMap.put(entryName + " Used (%)",
						new LeveledEntry(new DecimalFormat("#0.00 %").format(percentage), level));
			}
		} catch (JMException e) {
			log.error("error adding memory usage", e);
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Entry<String, ?>> getSortedEntries(Map<?, ?> entryMap) {
		List<String> keys = new ArrayList<>();
		for (Object key : entryMap.keySet()) {
			keys.add((String) key);
		}
		List<Entry<String, ?>> entries = new ArrayList<>();
		Collections.sort(keys);
		String qm = "?";
		for (String key : keys) {
			String entryKey = StringNormalizer.replaceNonPrintableCharacters(key, qm);
			Object value = entryMap.get(key);
			Object entryValue = (value instanceof String)
					? StringNormalizer.replaceNonPrintableCharacters((String) value, qm)
					: value;
			entries.add(new DefaultMapEntry(entryKey, entryValue));
		}
		return entries;
	}

	@Getter
	@AllArgsConstructor
	@RequiredArgsConstructor
	public class LeveledEntry {
		public static final int LOW = 1;
		public static final int MED = 2;
		public static final int HIGH = 3;

		private final String value;
		private int level = 0;
	}

}
