/*
 * Copyright 2011-2023 the original author or authors.
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

import java.util.List;

import javax.cache.Cache;

import org.appng.api.ProcessingException;
import org.appng.api.support.CallableDataSource;
import org.appng.core.controller.CachedResponse;
import org.appng.core.service.CacheService;
import org.appng.testsupport.validation.DateFieldDifferenceHandler;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.appng.testsupport.validation.XPathDifferenceHandler;
import org.appng.xml.platform.Datasource;
import org.appng.xml.platform.Result;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.MediaType;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CacheTest extends AbstractTest {

	static HazelcastInstance hz;

	static {
		WritingXmlValidator.writeXml = false;
		hz = Hazelcast.getOrCreateHazelcastInstance(new ClasspathXmlConfig("hazelcast-test.xml"));
		CacheService.createCacheManager(hz, false);
	}

	@Test
	public void testCache() throws Exception {
		Cache<String, CachedResponse> cache = CacheService.createCache(site);
		fillCache(cache, 0, 500);
		addParameter("fEtr", "/element/*");
		addParameter("fCtpe", "text/plain");
		Datasource datasource = getCacheDataSource();
		validate(datasource, new DateFieldDifferenceHandler());
	}

	@Test
	public void testCacheNoFilter() throws Exception {
		Cache<String, CachedResponse> cache = CacheService.getCache(site);
		fillCache(cache, 500, 700);
		Datasource datasource = getCacheDataSource();
		validate(datasource, new DateFieldDifferenceHandler());

	}

	private void fillCache(Cache<String, CachedResponse> cache, int start, int end) {
		for (int i = start; i < end; i++) {
			String key = "GET/element/" + i;
			cache.put(key, new CachedResponse(key, site, servletRequest, 200, MediaType.TEXT_PLAIN_VALUE, new byte[0],
					null, 3600));
		}
	}

	public Datasource getCacheDataSource() throws ProcessingException {
		addParameter("sortCacheElements", "pageSize:10;page:4");
		initParameters();
		CallableDataSource ds = getDataSource("cacheElements").withParam("siteid", "1").getCallableDataSource();
		ds.perform("");

		Datasource datasource = ds.getDatasource();
		List<Result> results = datasource.getData().getResultset().getResults();

		// we can not predict order
		for (int i = 0; i < 10;) {
			results.get(i).getFields().get(0).setValue("/element/" + String.valueOf(++i));
		}
		return datasource;
	}

	@Test
	public void testCacheNoSite() throws Exception {
		addParameter("sortCacheElements", "pageSize:25;page:0");
		initParameters();
		CallableDataSource ds = getDataSource("cacheElements").withParam("siteid", "42").getCallableDataSource();
		ds.perform("");
		validate(ds.getDatasource());
	}

	@Test
	public void testCacheStatistics() throws Exception {
		CallableDataSource ds = getDataSource("cacheStatistics").withParam("siteid", "1").getCallableDataSource();
		ds.perform("");
		Datasource datasource = ds.getDatasource();
		XPathDifferenceHandler dh = new XPathDifferenceHandler(false);
		// avg put/get times
		dh.ignoreDifference("/datasource[1]/data[1]/resultset[1]/result[8]/field[2]/value[1]/text()[1]");
		dh.ignoreDifference("/datasource[1]/data[1]/resultset[1]/result[10]/field[2]/value[1]/text()[1]");
		validate(datasource, dh);
	}

}
