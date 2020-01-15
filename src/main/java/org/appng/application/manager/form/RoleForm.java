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
package org.appng.application.manager.form;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.appng.core.domain.RoleImpl;

import lombok.Getter;
import lombok.Setter;

/**
 * Bindclass used for creating/updating a {@link RoleImpl}.
 * 
 * @author Matthias Müller
 * 
 */
@Getter
@Setter
public class RoleForm {

	private RoleImpl role;
	private List<Integer> permissionIds = new ArrayList<Integer>();
	private List<Integer> groupIds = new ArrayList<Integer>();
	private List<Integer> userIds = new ArrayList<Integer>();

	public RoleForm() {
		this(new RoleImpl());
	}

	public RoleForm(RoleImpl role) {
		this.role = role;
	}

	@Valid
	public RoleImpl getRole() {
		return role;
	}
}
