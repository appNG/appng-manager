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
package org.appng.application.manager.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.ApplicationException;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.SelectionFactory;
import org.appng.application.manager.service.PlatformEventService;
import org.appng.core.domain.PlatformEvent;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.SelectionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import lombok.Data;

@Service
public class PlatformEvents implements DataProvider {

	private static final FastDateFormat FDF = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

	private PlatformEventService platformEventEventService;
	private EventFilter filter;
	private SelectionFactory selectionFactory;

	@Autowired
	public PlatformEvents(PlatformEventService platformEventEventService, SelectionFactory selectionFactory,
			EventFilter filter) {
		this.platformEventEventService = platformEventEventService;
		this.filter = filter;
		this.selectionFactory = selectionFactory;
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fieldProcessor) {
		SelectionGroup group = new SelectionGroup();
		List<Selection> selections = group.getSelections();

		selections.add(selectionFactory.getDateSelection("fAf", "createdAfter", filter.getFAf(), FDF));
		selections.add(selectionFactory.getDateSelection("fBf", "createdBefore", filter.getFBf(), FDF));

		Selection typeFilter = selectionFactory.fromEnum("fTps", "type", PlatformEvent.Type.values(),
				filter.eventTypes());
		typeFilter.setType(SelectionType.CHECKBOX);
		selections.add(typeFilter);

		selections.add(selectionFactory.getTextSelection("fTxt", "event", filter.getFTxt()));
		selections.add(selectionFactory.getTextSelection("fUsr", "user", filter.getFUsr()));
		selections.add(selectionFactory.getTextSelection("fHst", "host", filter.getFHst()));
		selections.add(selectionFactory.getTextSelection("fHstNm", "hostName", filter.getFHstNm()));

		DataContainer dataContainer = new DataContainer(fieldProcessor);
		dataContainer.getSelectionGroups().add(group);
		Page<PlatformEvent> events = platformEventEventService.getEvents(fieldProcessor.getPageable(), filter);
		dataContainer.setPage(events);
		return dataContainer;
	}

	@Data
	@Component("eventFilter")
	@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	public class EventFilter implements DataProvider {

		private Date fBf;
		private Date fAf;
		private String fTxt;
		private String fUsr;
		private String fHst;
		private String fHstNm;
		private List<String> fTps = new ArrayList<>();

		public DataContainer getData(Site site, Application application, Environment environment, Options options,
				Request request, FieldProcessor fp) {
			try {
				request.fillBindObject(this, fp, request, site.getSiteClassLoader());
				DataContainer dataContainer = new DataContainer(fp);
				dataContainer.setItem(this);
				return dataContainer;
			} catch (BusinessException e) {
				throw new ApplicationException("error filling filter", e);
			}
		}

		public List<PlatformEvent.Type> eventTypes() {
			return fTps.stream().map(t -> PlatformEvent.Type.valueOf(t)).collect(Collectors.toList());
		}

	}

}