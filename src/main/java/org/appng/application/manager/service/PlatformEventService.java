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
package org.appng.application.manager.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.appng.application.manager.business.PlatformEvents.EventFilter;
import org.appng.core.domain.PlatformEvent;
import org.appng.core.repository.PlatformEventRepository;
import org.appng.persistence.repository.SearchQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlatformEventService {
	PlatformEventRepository platformEventRepository;

	@Autowired
	public PlatformEventService(PlatformEventRepository platformEventRepository) {
		this.platformEventRepository = platformEventRepository;
	}

	public Page<PlatformEvent> getEvents(Pageable pageable, EventFilter eventFilter) {
		SearchQuery<PlatformEvent> query = platformEventRepository.createSearchQuery();
		query.greaterEquals("created", eventFilter.getEA());
		query.lessEquals("created", eventFilter.getEB());
		query.equals("origin", StringUtils.trimToNull(eventFilter.getEH()));
		query.equals("hostName", StringUtils.trimToNull(eventFilter.getEN()));
		if (StringUtils.isNotBlank(eventFilter.getEU())) {
			query.contains("user", eventFilter.getEU());
		}
		query.equals("application", StringUtils.trimToNull(eventFilter.getEAp()));
		query.in("type", eventFilter.eventTypes());
		query.contains("event", StringUtils.trimToNull(eventFilter.getEX()));
		return platformEventRepository.search(query, pageable);
	}

	public List<PlatformEvent> getEvents(EventFilter eventFilter) {
		return getEvents(new PageRequest(0, Integer.MAX_VALUE), eventFilter).getContent();
	}

	public List<String> getUsers() {
		return platformEventRepository.findDistinctUsers();
	}

	public List<String> getApplications() {
		return platformEventRepository.findDistinctApplications();
	}

	public List<String> getHostNames() {
		return platformEventRepository.findDistinctHostNames();
	}

	public List<String> getOrigins() {
		return platformEventRepository.findDistinctOrigins();
	}
}
