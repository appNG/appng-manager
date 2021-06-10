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

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("env")
public class Environment implements DataProvider {

	@SuppressWarnings("unchecked")
	public DataContainer getData(Site site, Application application, org.appng.api.Environment environment,
			Options options, Request request, FieldProcessor fieldProcessor) {
		String action = options.getOptionValue("mode", "id");
		DataContainer dataContainer = new DataContainer(fieldProcessor);
		Map<?, ?> entryMap = null;
		if ("env".equals(action)) {
			entryMap = System.getenv();
		} else if ("props".equals(action)) {
			entryMap = System.getProperties();
		} else if ("jvm".equals(action)) {
			entryMap = new HashMap<String, String>();
			List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
			for (String arg : inputArguments) {
				int idx = arg.indexOf('=');
				String key = arg;
				String value = "";
				if (idx > 0) {
					key = arg.substring(0, idx);
					value = arg.substring(idx + 1);
				}
				((Map<String, String>) entryMap).put(key, value);
			}
		} else if ("mem".equals(action)) {
			entryMap = new HashMap<String, LeveledEntry>();
			Unit unit = Unit.KB;
			long factor = unit.getFactor();
			DecimalFormat format = new DecimalFormat("  #,###,000  " + unit.name());
			DecimalFormat percentFormat = new DecimalFormat("#0.00 %");

			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			addUsage((Map<String, LeveledEntry>) entryMap, factor, format, percentFormat, mBeanServer, "Heap", "Memory",
					null, "HeapMemoryUsage");
			addUsage((Map<String, LeveledEntry>) entryMap, factor, format, percentFormat, mBeanServer, "Metaspace",
					"MemoryPool", "Metaspace", "Usage");
		} else if ("proc".equals(action)) {
			entryMap = new HashMap<String, String>();
			OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
			int procs = osMxBean.getAvailableProcessors();
			double load = osMxBean.getSystemLoadAverage();
			((Map<String, String>) entryMap).put("Processors", Integer.toString(procs));
			((Map<String, String>) entryMap).put("Average Load", Double.toString(load));
		}

		List<Entry<String, ?>> entries = getSortedEntries(entryMap);
		dataContainer.setPage(entries, fieldProcessor.getPageable());
		return dataContainer;
	}

	protected void addUsage(Map<String, LeveledEntry> entryMap, long factor, DecimalFormat format,
			DecimalFormat percentFormat, MBeanServer mBeanServer, String entryName, String type, String name,
			String attributeName) {
		try {
			ObjectName objectName = new ObjectName("java.lang:type=" + type + (null == name ? "" : (",name=" + name)));
			CompositeData attribute = (CompositeData) mBeanServer.getAttribute(objectName, attributeName);
			long committed = (long) attribute.get("committed");
			long max = (long) attribute.get("max");
			long used = (long) attribute.get("used");
			entryMap.put(entryName + " Size", new LeveledEntry(format.format((double) committed / factor)));
			entryMap.put(entryName + " Max", new LeveledEntry(max < 0 ? "?" : format.format((double) max / factor)));
			entryMap.put(entryName + " Used", new LeveledEntry(format.format((double) used / factor)));
			if (max > 0) {
				double percentage = (double) used / (double) max;
				int level = percentage < 0.75d ? LeveledEntry.LOW
						: (percentage < 0.85d ? LeveledEntry.MED : LeveledEntry.HIGH);
				entryMap.put(entryName + " Used (%)", new LeveledEntry(percentFormat.format(percentage), level));
			}
		} catch (OperationsException | ReflectionException | MBeanException e) {
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

	public class LeveledEntry {
		public static final int LOW = 1;
		public static final int MED = 2;
		public static final int HIGH = 3;

		private String value;
		private int level = 0;

		public LeveledEntry(String value) {
			this.value = value;
		}

		public LeveledEntry(String value, int level) {
			this.value = value;
			this.level = level;
		}

		public String getValue() {
			return value;
		}

		public int getLevel() {
			return level;
		}
	}

}
