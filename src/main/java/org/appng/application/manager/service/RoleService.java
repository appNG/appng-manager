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

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.appng.api.BusinessException;
import org.appng.api.model.Application;
import org.appng.api.model.Subject;
import org.appng.core.domain.SubjectImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = BusinessException.class)
public class RoleService {

	private EntityManager em;

	@Autowired
	public RoleService(@Qualifier("entityManager") EntityManager em) {
		this.em = em;
	}

	public Collection<? extends Subject> getSubjectsForRole(Application application, String roleName) {
		TypedQuery<SubjectImpl> query = em.createQuery(
				"select s from GroupImpl g join g.subjects s join g.roles r where r.name=:name and r.application.id=:applicationId",
				SubjectImpl.class);
		query.setParameter("name", roleName).setParameter("applicationId", application.getId());
		List<SubjectImpl> result = query.getResultList();
		return result;
	}

}
