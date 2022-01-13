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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

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
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.Scope;
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
import org.appng.xml.platform.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.hazelcast.cache.ICache;

/**
 * Provides methods to interact with the page cache. Elements are stored in the cache by the {@link PageCacheFilter}.
 * 
 * @author Matthias Herlitzius
 */
@Lazy
@Slf4j
@Component
public class Cache extends ServiceAware implements ActionProvider<Void>, DataProvider {

	private static final String F_ETR = "fEtr";
	private static final String F_CTYPE = "fCtpe";
	private static final String STATISTICS = "statistics";
	private static final String ENTRIES = "entries";
	private static final String ACTION_EXPIRE_CACHE_ELEMENT = "expireCacheElement";
	private static final String ACTION_CLEAR_CACHE = "clearCache";
	private @Value("${" + ManagerSettings.MAX_FILTERABLE_CACHE_ENTRIES + "}") Integer maxCacheEntries;

	@Autowired
	private SelectionFactory selectionFactory;

	public DataContainer getData(Site site, Application application, Environment env, Options options, Request request,
			FieldProcessor fp) {
		String mode = options.getString("mode", ID);
		Integer siteId = options.getInteger("site", ID);
		DataContainer dataContainer = new DataContainer(fp);

		Map<String, Site> siteMap = env.getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		Optional<Site> cacheSite = siteMap.values().stream().filter(s -> s.getId().equals(siteId)).findFirst();

		if (cacheSite.isPresent()) {
			if (STATISTICS.equals(mode)) {
				List<Entry<String, String>> result = new ArrayList<>();
				Map<String, String> stats = CacheService.getCacheStatistics(cacheSite.get());
				if (!stats.isEmpty()) {
					result.add(getStatEntry(request, stats, CacheService.STATS_NAME));
					result.add(getStatEntry(request, stats, CacheService.STATS_SIZE));
					result.add(getStatEntry(request, stats, CacheService.STATS_HITS));
					result.add(getStatEntry(request, stats, CacheService.STATS_HITS_PERCENT));
					result.add(getStatEntry(request, stats, CacheService.STATS_MISSES));
					result.add(getStatEntry(request, stats, CacheService.STATS_MISSES_PERCENT));
					result.add(getStatEntry(request, stats, CacheService.STATS_PUTS));
					result.add(getStatEntry(request, stats, CacheService.STATS_AVG_PUT_TIME));
					result.add(getStatEntry(request, stats, CacheService.STATS_GETS));
					result.add(getStatEntry(request, stats, CacheService.STATS_AVG_GET_TIME));
					result.add(getStatEntry(request, stats, CacheService.STATS_REMOVALS));
					result.add(getStatEntry(request, stats, CacheService.STATS_AVG_REMOVAL_TIME));
				}
				dataContainer.setItems(result);
			} else if (ENTRIES.equals(mode)) {
				Pageable pageable = fp.getPageable();
				List<CacheEntry> cacheEntries = new ArrayList<>();

				javax.cache.Cache<String, CachedResponse> cache = CacheService.getCache(cacheSite.get());
				int cacheSize = 0;
				if (null != cache) {
					cacheSize = cache.unwrap(ICache.class).size();

					if (cacheSize > maxCacheEntries) {
						Iterator<javax.cache.Cache.Entry<String, CachedResponse>> elements = cache.iterator();
						int idx = 0;
						int startIdx = pageable.getOffset();
						int endIdx = pageable.getOffset() + pageable.getPageSize();
						while (elements.hasNext()) {
							javax.cache.Cache.Entry<java.lang.String, CachedResponse> entry = elements.next();
							CachedResponse cachedResponse = entry.getValue();
							// entry may have been removed meanwhile
							if (null != cachedResponse) {
								if (idx >= startIdx && idx < endIdx) {
									cacheEntries.add(new CacheEntry(cachedResponse));
								}
								if (idx++ >= endIdx) {
									break;
								}
							} else {
								endIdx++;
							}
						}

						fp.getFields().stream().filter(f -> !"id".equals(f.getBinding())).forEach(f -> f.setSort(null));
						SortOrder idOrder = fp.getField("id").getSort().getOrder();
						if (null != idOrder) {
							Collections.sort(cacheEntries, (e1, e2) -> StringUtils.compare(e1.getId(), e2.getId()));
							if (SortOrder.DESC.equals(idOrder)) {
								Collections.reverse(cacheEntries);
							}
						}

					} else {
						String entryName = request.getParameter(F_ETR);
						String entryType = request.getParameter(F_CTYPE);
						boolean filterName = StringUtils.isNotBlank(entryName);
						boolean filterType = StringUtils.isNotBlank(entryType);
						for (javax.cache.Cache.Entry<String, CachedResponse> entry : cache) {
							String entryId = entry.getKey();
							CachedResponse cachedResponse = entry.getValue();
							// entry may have been removed meanwhile
							if (null != cachedResponse) {
								boolean nameMatches = !filterName || FilenameUtils.wildcardMatch(
										entryId.substring(entryId.indexOf('/')), entryName, IOCase.INSENSITIVE);
								boolean typeMatches = !filterType || FilenameUtils
										.wildcardMatch(cachedResponse.getContentType(), entryType, IOCase.INSENSITIVE);
								if (nameMatches && typeMatches) {
									cacheEntries.add(new CacheEntry(cachedResponse));
								}
							}
						}

						Selection nameSelection = selectionFactory.getTextSelection(F_ETR, MessageConstants.NAME,
								entryName);
						Selection typeSelection = selectionFactory.getTextSelection(F_CTYPE, MessageConstants.TYPE,
								entryType);
						SelectionGroup selectionGroup = new SelectionGroup();
						selectionGroup.getSelections().add(nameSelection);
						selectionGroup.getSelections().add(typeSelection);
						dataContainer.getSelectionGroups().add(selectionGroup);
					}
				}
				dataContainer.setPage(new PageImpl<>(cacheEntries, pageable, cacheSize));
			}
		}
		return dataContainer;
	}

	private boolean matches(String name, String matcher) {
		return FilenameUtils.wildcardMatch(name, matcher, IOCase.INSENSITIVE);
	}

	protected Optional<CacheEntry> getEntry(Site cacheSite, BlockingCache cache, Serializable key) {
		Element element = cache.getQuiet(key);
		if (null != element && null != element.getObjectValue()) {
			try {
				PageInfo pageInfo = (PageInfo) element.getObjectValue();
				return Optional.of(new CacheEntry(new AppngCache(key, cacheSite, pageInfo, element)));
			} catch (IOException e) {
				// ignore
			}
		}
		return Optional.empty();
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
