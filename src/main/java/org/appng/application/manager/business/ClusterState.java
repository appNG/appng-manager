/*
 * Copyright 2011-2017 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.core.controller.messaging.NodeEvent;
import org.appng.core.controller.messaging.NodeEvent.NodeState;
import org.appng.core.controller.messaging.RequestNodeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

@Service
@org.springframework.context.annotation.Scope("request")
public class ClusterState implements DataProvider, ActionProvider<Void> {

	private static final Logger log = LoggerFactory.getLogger(ClusterState.class);

	public DataContainer getData(Site site, Application application, org.appng.api.Environment environment,
			Options options, Request request, FieldProcessor fieldProcessor) {
		DataContainer dataContainer = new DataContainer(fieldProcessor);
		String mode = options.getOptionValue("mode", "value");
		String nodeId = options.getOptionValue("mode", "nodeId");
		Map<String, NodeState> nodeStates = environment.getAttribute(Scope.PLATFORM, NodeEvent.NODE_STATE);
		if (null == nodeStates) {
			dataContainer.setPage(new PageImpl<String>(new ArrayList<String>()));
		} else {
			List<NodeState> items = new ArrayList<NodeState>(nodeStates.values());
			if (StringUtils.isEmpty(mode)) {
				dataContainer.setPage(items, fieldProcessor.getPageable());
			} else if (StringUtils.isNotEmpty(nodeId)) {
				NodeState nodeState = nodeStates.get(nodeId);
				Map<?, ?> entries = null;
				if (null != nodeState) {
					if ("props".equals(mode)) {
						entries = nodeState.getProps();
					} else if ("env".equals(mode)) {
						entries = nodeState.getEnv();
					} else if ("siteState".equals(mode)) {
						entries = nodeState.getSiteStates();
					}
					if (null == entries) {
						entries = new HashMap<String, String>();
					}
					dataContainer.setPage(Environment.getSortedEntries(entries), fieldProcessor.getPageable());
				} else {
					log.warn("no node state for node {}!", nodeId);
				}
			}
		}
		return dataContainer;
	}

	public void perform(Site site, Application application, org.appng.api.Environment environment, Options options,
			Request request, Void formBean, FieldProcessor fieldProcessor) {
		site.sendEvent(new RequestNodeState(site.getName()));
	}

}
