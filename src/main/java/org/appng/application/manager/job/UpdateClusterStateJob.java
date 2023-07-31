package org.appng.application.manager.job;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.appng.api.ScheduledJob;
import org.appng.api.Scope;
import org.appng.api.messaging.Event;
import org.appng.api.messaging.Messaging;
import org.appng.api.messaging.Sender;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.core.controller.messaging.NodeEvent;
import org.appng.core.controller.messaging.NodeEvent.NodeState;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Component
public class UpdateClusterStateJob implements ScheduledJob, InitializingBean {

	private static final String SHUTDOWN_EVENT = "org.appng.core.controller.messaging.ShutdownEvent";

	private String description = "Removes nodes from cluster state on inactivity.";
	private Map<String, Object> jobDataMap = new HashMap<>();
	private @Value("${clusterStateMaxAgeMinutes:2}") Integer clusterStateMaxAge;
	private Class<? extends Event> eventClass;

	@SuppressWarnings("unchecked")
	@Override
	public void afterPropertiesSet() throws Exception {
		boolean isShutdownPresent = ClassUtils.isPresent(SHUTDOWN_EVENT, getClass().getClassLoader());
		if (isShutdownPresent) {
			eventClass = (Class<? extends Event>) getClass().getClassLoader().loadClass(SHUTDOWN_EVENT);
		}
		jobDataMap.put("enabled", isShutdownPresent);
		jobDataMap.put("runOnce", true);
		jobDataMap.put("cronExpression", String.format("0 0/%s * 1/1 * ? *", clusterStateMaxAge));
	}

	@Override
	public void execute(Site site, Application application) throws Exception {
		if ((boolean) jobDataMap.getOrDefault("enabled", false)) {
			DefaultEnvironment env = DefaultEnvironment.getGlobal();
			Sender sender = Messaging.getMessageSender(env);
			Map<String, NodeState> nodeStates = env.getAttribute(Scope.PLATFORM, NodeEvent.NODE_STATE);
			long maxAge = TimeUnit.MINUTES.toMillis(clusterStateMaxAge);
			Date now = new Date();
			for (Entry<String, NodeState> node : nodeStates.entrySet()) {
				Date date = node.getValue().getDate();
				long age = now.getTime() - date.getTime();
				if (age > maxAge) {
					log.info("Removing {} from cluster, last message is {} seconds ago.", node.getKey(), age / 1000);
					sender.send(eventClass.newInstance());
				}
			}
		}

	}

}
