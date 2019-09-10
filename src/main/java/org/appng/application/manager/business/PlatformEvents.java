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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.ApplicationException;
import org.appng.api.BusinessException;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.SelectionBuilder;
import org.appng.api.support.SelectionFactory;
import org.appng.application.manager.MessageConstants;
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

		Selection typeFilter = selectionFactory.fromEnum("eT", MessageConstants.TYPE, PlatformEvent.Type.values(),
				filter.eventTypes());
		typeFilter.setType(SelectionType.CHECKBOX);
		selections.add(typeFilter);

		selections.add(selectionFactory.getTextSelection("eX", MessageConstants.EVENT, filter.getEX()));

		Collection<String> users = platformEventEventService.getUsers();
		Selection userSelection = getStringSelection("eU", users, filter.getEU(), MessageConstants.USER);
		selections.add(userSelection);

		Collection<String> applications = platformEventEventService.getApplications();
		Selection applicationSelection = getStringSelection("eAp", applications, filter.getEAp(),
				MessageConstants.APPLICATION);
		selections.add(applicationSelection);

		Collection<String> hostNames = platformEventEventService.getOrigins();
		Selection hostSelection = getStringSelection("eH", hostNames, filter.getEH(), MessageConstants.HOST);
		selections.add(hostSelection);

		Collection<String> hosts = platformEventEventService.getHostNames();
		Selection hostNameSelection = getStringSelection("eN", hosts, filter.getEN(), MessageConstants.HOST_NAME);
		selections.add(hostNameSelection);

		selections.add(selectionFactory.getDateSelection("eA", MessageConstants.CREATED_AFTER, filter.getEA(), FDF));
		selections.add(selectionFactory.getDateSelection("eB", MessageConstants.CREATED_BEFORE, filter.getEB(), FDF));

		DataContainer dataContainer = new DataContainer(fieldProcessor);
		dataContainer.getSelectionGroups().add(group);
		environment.setAttribute(Scope.SESSION, "eventFilter", filter.copy());
		Page<PlatformEvent> events = platformEventEventService.getEvents(fieldProcessor.getPageable(), filter);
		dataContainer.setPage(events);
		return dataContainer;
	}

	private Selection getStringSelection(String id, Collection<String> values, String selected, String label) {
		SelectionBuilder<String> builder = new SelectionBuilder<String>(id);
		return builder.title(label).options(values).select(selected).type(SelectionType.SELECT)
				.defaultOption(StringUtils.EMPTY, StringUtils.EMPTY).build();
	}

	@Data
	@Component("eventFilter")
	@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	public static class EventFilter implements DataProvider, Serializable {
		/** before */
		private Date eB;
		/** after */
		private Date eA;
		/** text */
		private String eX;
		/** user */
		private String eU;
		/** host */
		private String eH;
		/** hostnames */
		private String eN;
		/** application */
		private String eAp;
		/** types */
		private List<String> eT = new ArrayList<>();

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

		public EventFilter copy() {
			EventFilter copy = new EventFilter();
			copy.setEA(eA);
			copy.setEB(eB);
			copy.setEH(eH);
			copy.setEN(eN);
			copy.setET(eT);
			copy.setEU(eU);
			copy.setEX(eX);
			return copy;
		}

		public List<PlatformEvent.Type> eventTypes() {
			return eT.stream().filter(t -> StringUtils.isNotBlank(t)).map(t -> PlatformEvent.Type.valueOf(t))
					.collect(Collectors.toList());
		}

	}
}
