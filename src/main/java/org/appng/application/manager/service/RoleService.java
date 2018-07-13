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
package org.appng.application.manager.service;

import java.util.Collection;

import org.appng.api.BusinessException;
import org.appng.api.model.Application;
import org.appng.api.model.Subject;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.repository.SubjectRepository;
import org.appng.persistence.repository.SearchQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = BusinessException.class)
public class RoleService {

	private SubjectRepository subjectRepository;

	@Autowired
	public RoleService(SubjectRepository subjectRepository) {
		this.subjectRepository = subjectRepository;
	}

	public Collection<? extends Subject> getSubjectsForRole(Application application, String roleName) {
		SearchQuery<SubjectImpl> query = subjectRepository.createSearchQuery();
		query.join("left join fetch e.roles r");
		query.equals("r.name", roleName).equals("r.application.id", application.getId());
		return subjectRepository.search(query, null).getContent();
	}

}
