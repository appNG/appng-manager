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
package org.appng.application.manager.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang3.StringUtils;
import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.SelectionFactory;
import org.appng.api.support.SelectionFactory.Selection;
import org.appng.application.manager.ManagerSettings;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.controller.CachedResponse;
import org.appng.core.controller.filter.PageCacheFilter;
import org.appng.core.service.CacheService;
import org.appng.xml.platform.SelectionGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Provides methods to interact with the page cache. Elements are stored in the cache by the {@link PageCacheFilter}.
 * 
 * @author Matthias Herlitzius
 */
@Component
public class Cache extends ServiceAware implements ActionProvider<Void>, DataProvider {

	private static final String F_ETR = "fEtr";
	private static final String F_CTYPE = "fCtpe";
	private static final String STATISTICS = "statistics";
	private static final String ENTRIES = "entries";
	private static final String ACTION_EXPIRE_CACHE_ELEMENT = "expireCacheElement";
	private static final String ACTION_CLEAR_CACHE = "clearCache";

	@Autowired
	private SelectionFactory selectionFactory;

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		String mode = options.getOptionValue("mode", ID);
		Integer siteId = request.convert(options.getOptionValue("site", ID), Integer.class);
		DataContainer dataContainer = new DataContainer(fp);
		Pageable pageable = fp.getPageable();
		if (STATISTICS.equals(mode)) {
			List<Entry<String, String>> result = new ArrayList<>();
			Map<String, String> cacheStatistics = getService().getCacheStatistics(siteId);
			for (Entry<String, String> e : cacheStatistics.entrySet()) {
				result.add(e);
			}
			Collections.sort(result, new Comparator<Entry<String, String>>() {
				public int compare(Entry<String, String> o1, Entry<String, String> o2) {
					return o1.getKey().compareTo(o2.getKey());
				}
			});
			dataContainer.setPage(result, pageable);
		} else if (ENTRIES.equals(mode)) {
			Integer maxCacheEntries = application.getProperties()
					.getInteger(ManagerSettings.MAX_FILTERABLE_CACHE_ENTRIES);
			List<AppngCache> allCacheEntries = getService().getCacheEntries(siteId);
			List<CacheEntry> cacheEntries = new ArrayList<CacheEntry>();

			int cacheSize = allCacheEntries.size();
			if (cacheSize > maxCacheEntries) {
				fp.getFields().forEach(f -> f.setSort(null));
				for (int i = pageable.getOffset(); i < pageable.getOffset() + pageable.getPageSize(); i++) {
					if (i < cacheSize) {
						cacheEntries.add(new CacheEntry(allCacheEntries.get(i)));
					}
				}
				dataContainer.setPage(new PageImpl<>(cacheEntries, pageable, cacheSize));
			} else {
				String entryName = request.getParameter(F_ETR);
				String entryType = request.getParameter(F_CTYPE);
				boolean filterName = StringUtils.isNotBlank(entryName);
				boolean filterType = StringUtils.isNotBlank(entryType);

				for (AppngCache entry : allCacheEntries) {
					String entryId = entry.getId();
					boolean nameMatches = !filterName || FilenameUtils
							.wildcardMatch(entryId.substring(entryId.indexOf('/')), entryName, IOCase.INSENSITIVE);
					boolean typeMatches = !filterType
							|| FilenameUtils.wildcardMatch(entry.getContentType(), entryType, IOCase.INSENSITIVE);
					if (nameMatches && typeMatches) {
						cacheEntries.add(new CacheEntry(entry));
					}
				}

				Selection nameSelection = selectionFactory.getTextSelection(F_ETR, MessageConstants.NAME, entryName);
				Selection typeSelection = selectionFactory.getTextSelection(F_CTYPE, MessageConstants.TYPE, entryType);
				SelectionGroup selectionGroup = new SelectionGroup();
				selectionGroup.getSelections().add(nameSelection);
				selectionGroup.getSelections().add(typeSelection);
				dataContainer.getSelectionGroups().add(selectionGroup);
				dataContainer.setPage(cacheEntries, pageable);
			}

		}
		return dataContainer;
	}

	private Entry<String, String> getStatEntry(Request request, Map<String, String> statistics, String statKey) {
		return new DefaultMapEntry<String, String>(request.getMessage("cache.statistics." + statKey),
				statistics.get(statKey));
	}

	public class CacheEntry {
		private CachedResponse response;

		public CacheEntry(CachedResponse response) {
			this.response = response;
		}

		public String getPath() {
			return response.getDomain() + getId().substring(getId().indexOf('/'));
		}

		public String getType() {
			String contentType = response.getContentType();
			if (null == contentType) {
				return "<unknown>";
			}
			int idx = contentType.indexOf(';');
			return idx > 0 ? contentType.substring(0, idx) : contentType;
		}

		public String getId() {
			return response.getId();
		}

		public long getHits() {
			return response.getHitCount();
		}

		public Date getCreated() {
			return response.getCreationTime();
		}

		public Date getExpires() {
			return response.getExpirationTime();
		}

		public double getSize() {
			return (double) response.getContentLength() / FileUtils.ONE_KB;
		}
	}

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			Void formBean, FieldProcessor fieldProcessor) {
		String action = getAction(options);
		Integer siteId = options.getInteger("site", "id");
		if (ACTION_EXPIRE_CACHE_ELEMENT.equals(action)) {
			String cacheElement = options.getString("cacheElement", "id");
			getService().expireCacheElement(request, fieldProcessor, siteId, cacheElement);
		} else if (ACTION_CLEAR_CACHE.equals(action)) {
			getService().clearCache(request, fieldProcessor, siteId);
		}
	}
}
