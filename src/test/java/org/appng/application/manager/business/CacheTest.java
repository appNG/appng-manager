/*
 * Copyright 2011-2019 the original author or authors.
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

import org.appng.api.Platform;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.PropertyForm;
import org.appng.application.manager.form.SiteForm;
import org.appng.core.domain.SiteImpl;
import org.appng.testsupport.validation.XPathDifferenceHandler;
import org.junit.Test;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.web.PageInfo;

public class CacheTest extends AbstractTest {

	@Test
	public void testCacheElements() throws Exception {
		// prepares using appNG >= 1.19.1
		PropertyForm form = new PropertyForm();
		form.getProperty().setName(Platform.Property.MESSAGING_ENABLED);
		form.getProperty().setDefaultString(Boolean.FALSE.toString());
		getAction("propertyEvent", "create-platform-property").withParam(FORM_ACTION, "create-platform-property")
				.getCallableAction(form).perform();

		SiteImpl siteToCreate = new SiteImpl();
		SiteForm siteForm = new SiteForm(siteToCreate);
		siteToCreate.setName("localhost");
		siteToCreate.setHost("localhost");
		siteToCreate.setDomain("localhost");
		siteToCreate.setActive(true);

		getAction("siteEvent", "create").withParam(FORM_ACTION, "create").getCallableAction(siteForm).perform();

		CacheManager cacheManager = CacheManager.getInstance();
		Ehcache cache = cacheManager.addCacheIfAbsent("pageCache-localhost");
		BlockingCache blockingCache = new BlockingCache(cache);
		String cacheKey = "GET/foo/bar.txt";
		PageInfo pageInfo = new PageInfo(200, "text/plain", null, "appNG rocks!".getBytes(), false, 120, null);
		for (int i = 0; i < 1001; i++) {
			blockingCache.put(new Element("GET/foo/bar" + i + ".txt", pageInfo));
		}
		cacheManager.replaceCacheWithDecoratedCache(cache, blockingCache);

		CallableDataSource callableDataSource = getDataSource("cacheElements").withParam("siteid", "1")
				.getCallableDataSource();
		callableDataSource.perform("");
		XPathDifferenceHandler diffHandler = new XPathDifferenceHandler();
		diffHandler.ignoreDifference("/datasource/data/resultset/result/field/value/text()");
		validate(callableDataSource.getDatasource(), diffHandler);
	}
}
