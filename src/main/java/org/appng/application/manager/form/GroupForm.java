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
package org.appng.application.manager.form;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.appng.core.domain.GroupImpl;

import lombok.Getter;
import lombok.Setter;

/**
 * Bindclass used for creating/updating a {@link GroupImpl}.
 * 
 * @author Matthias Müller
 * 
 */
@Getter
@Setter
public class GroupForm {

	private GroupImpl group;

	private List<Integer> roleIds = new ArrayList<>();

	public GroupForm() {
		this(new GroupImpl());
	}

	public GroupForm(GroupImpl group) {
		this.group = group;
	}

	@Valid
	public GroupImpl getGroup() {
		return group;
	}

}
