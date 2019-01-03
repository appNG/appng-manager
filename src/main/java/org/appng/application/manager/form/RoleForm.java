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
package org.appng.application.manager.form;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.appng.core.domain.RoleImpl;

/**
 * Bindclass used for creating/updating a {@link RoleImpl}.
 * 
 * @author Matthias Müller
 * 
 */
public class RoleForm {

	private RoleImpl role;
	private List<Integer> permissionIds = new ArrayList<Integer>();

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

	public void setRole(RoleImpl role) {
		this.role = role;
	}

	public List<Integer> getPermissionIds() {
		return permissionIds;
	}

	public void setPermissionIds(List<Integer> permissionIds) {
		this.permissionIds = permissionIds;
	}

}
