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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.messaging.Messaging;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.model.Site.SiteState;
import org.appng.core.controller.messaging.NodeEvent;
import org.appng.core.controller.messaging.NodeEvent.MemoryUsage;
import org.appng.core.controller.messaging.NodeEvent.NodeState;
import org.appng.core.controller.messaging.RequestNodeState;
import org.appng.core.controller.messaging.SiteStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

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

		LocalNodeState localNode = getLocalNode(site, environment);
		LocalNodeState currentNode = localNode;
		Set<LocalNodeState> items = new HashSet<LocalNodeState>();
		items.add(localNode);

		boolean clusterAvailable = null != nodeStates;
		if (!clusterAvailable) {
			// TODO use Messaging.isEnabled(environment) from APPNG-2034
			org.appng.api.model.Properties platformConfig = environment.getAttribute(Scope.PLATFORM,
					Platform.Environment.PLATFORM_CONFIG);
			if (platformConfig.getBoolean(Platform.Property.MESSAGING_ENABLED)) {
				fieldProcessor.addErrorMessage(request.getMessage("cluster.notAvailable"));
			} else {
				fieldProcessor.addNoticeMessage(request.getMessage("cluster.disabled"));
			}
		} else if (StringUtils.isNotBlank(nodeId)) {
			NodeState nodeState = nodeStates.get(nodeId);
			if (null != nodeState) {
				currentNode = new LocalNodeState(nodeState);
			}
		}

		if (StringUtils.isBlank(mode)) {
			if (clusterAvailable) {
				nodeStates.values().forEach(ns -> items.add(new LocalNodeState(ns)));
			}
			dataContainer.setPage(items, fieldProcessor.getPageable());
		} else if (null != currentNode) {
			Map<?, ?> entries = null;
			if ("props".equals(mode)) {
				entries = currentNode.getProps();
			} else if ("env".equals(mode)) {
				entries = currentNode.getEnv();
			} else if ("siteState".equals(mode)) {
				entries = currentNode.getSiteStates();
			}
			List<Entry<String, ?>> sortedEntries = null == entries ? new ArrayList<>()
					: Environment.getSortedEntries(entries);
			dataContainer.setPage(sortedEntries, fieldProcessor.getPageable());
		}
		return dataContainer;
	}

	private LocalNodeState getLocalNode(Site site, org.appng.api.Environment environment) {
		NodeEvent nodeEvent = new NodeEvent(environment, site.getName());
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heap = nodeEvent.new MemoryUsage(memoryMXBean.getHeapMemoryUsage());
		MemoryUsage nonHeap = nodeEvent.new MemoryUsage(memoryMXBean.getNonHeapMemoryUsage());
		Map<String, SiteState> siteStates = environment.getAttribute(Scope.PLATFORM, SiteStateEvent.SITE_STATE);

		String localNodeId = System.getProperty(Messaging.APPNG_NODE_ID);
		// TODO use Messaging.getNodeId(environment) from APPNG-2034
		LocalNodeState localNode = new LocalNodeState(localNodeId, new Date(), heap, nonHeap, System.getProperties(),
				System.getenv(), siteStates);
		if (log.isDebugEnabled()) {
			log.debug("local node is {}", localNode);
		}
		localNode.setCurrent(true);
		return localNode;
	}

	public void perform(Site site, Application application, org.appng.api.Environment environment, Options options,
			Request request, Void formBean, FieldProcessor fieldProcessor) {
		site.sendEvent(new RequestNodeState(site.getName()));
	}

	@Data
	@RequiredArgsConstructor
	@EqualsAndHashCode(of = { "nodeId" })
	public class LocalNodeState {

		private final String nodeId;
		private final Date date;
		private final MemoryUsage heap;
		private final MemoryUsage nonHeap;
		private final Properties props;
		private final Map<String, String> env;
		private final Map<String, SiteState> siteStates;
		private boolean current = false;

		public LocalNodeState(NodeState nodeState) {
			this(nodeState.getNodeId(), nodeState.getDate(), nodeState.getHeap(), nodeState.getNonHeap(),
					nodeState.getProps(), nodeState.getEnv(), nodeState.getSiteStates());
		}

	}
}
