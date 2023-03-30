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
package org.appng.application.manager.business;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.apache.catalina.Manager;
import org.apache.catalina.Store;
import org.apache.catalina.StoreManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Platform;
import org.appng.api.Request;
import org.appng.api.Scope;
import org.appng.api.model.Application;
import org.appng.api.model.Named;
import org.appng.api.model.Site;
import org.appng.api.support.OptionOwner;
import org.appng.api.support.SelectionBuilder;
import org.appng.api.support.SelectionFactory;
import org.appng.api.support.environment.DefaultEnvironment;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.controller.Session;
import org.appng.core.controller.SessionListener;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.SelectionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Sessions extends ServiceAware implements ActionProvider<Void>, DataProvider {

	private static final String F_CR_AF = "fCrAf";
	private static final String F_CR_BF = "fCrBf";
	private static final String F_AGNT = "fAgnt";
	private static final String F_SESS = "fSess";
	private static final String F_DMN = "fDmn";
	private static final String F_USR = "fUsr";
	private static final String F_LGN = "fLgn";
	private static final String MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
	private final FastDateFormat hourMinutes = FastDateFormat.getInstance(MM_DD_HH_MM);
	@Value("${maxSessions:250}")
	private int maxSessions;

	@Autowired
	private SelectionFactory selectionFactory;

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			Void formBean, FieldProcessor fp) {
		String sessionId = options.getOptionValue("session", "id");
		String currentSession = environment.getAttribute(Scope.SESSION, org.appng.api.Session.Environment.SID);
		Integer siteId = options.getInteger("site", "id");
		String siteName = null == siteId ? null : getService().getNameForSite(siteId);
		if (null == sessionId) {
			List<Session> sessions = getSessionsFromManager(request, fp);
			int cnt = 0;
			for (Session session : sessions) {
				if (expire(currentSession, session.getId(), siteName, getManager(environment))) {
					cnt++;
				}
			}
			if (null == siteName) {
				fp.addOkMessage(request.getMessage(MessageConstants.SESSIONS_EXPIRED_GLOBAL, cnt));
			} else {
				fp.addOkMessage(request.getMessage(MessageConstants.SESSIONS_EXPIRED_SITE, cnt, siteName));
			}
		} else {
			boolean expired = expire(currentSession, sessionId, siteName, getManager(environment));
			String shortId = new Session(sessionId).getShortId();
			if (expired) {
				fp.addOkMessage(request.getMessage(MessageConstants.SESSION_EXPIRED, shortId));
			} else {
				fp.addErrorMessage(request.getMessage(MessageConstants.SESSION_EXPIRED_FAIL, shortId));
			}
		}
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fp) {
		DataContainer dataContainer = new DataContainer(fp);
		List<Session> immutableSessions = getSessionsFromManager(request, fp);

		String fDmn = request.getParameter(F_DMN);
		String fSess = request.getParameter(F_SESS);
		String fAgnt = request.getParameter(F_AGNT);
		String fCrBf = request.getParameter(F_CR_BF);
		String fCrAf = request.getParameter(F_CR_AF);
		String fUsr = request.getParameter(F_USR);
		String fLgn = request.getParameter(F_LGN);

		Set<String> userAgents = new TreeSet<>();
		userAgents.add(StringUtils.EMPTY);
		Boolean currentSiteOnly = options.getBoolean("site", "currentSiteOnly");
		List<Session> sessions = getSessions(options, request, currentSiteOnly, immutableSessions, userAgents, fDmn,
				fSess, fAgnt, fUsr, getDate(fCrBf), getDate(fCrAf), fLgn);
		if (getManager(environment).getActiveSessions() <= maxSessions) {
			addFilters(request, currentSiteOnly, dataContainer, userAgents, fAgnt, fDmn, fUsr);
		}

		dataContainer.setPage(sessions, fp.getPageable());
		return dataContainer;
	}

	protected List<Session> getSessionsFromManager(Request request, FieldProcessor fp) {
		List<Session> immutableSessions = new ArrayList<>();
		Manager manager = getManager(request.getEnvironment());
		try {
			if (manager instanceof StoreManager) {
				Store store = StoreManager.class.cast(manager).getStore();
				int sessionCount = store.getSize();
				String[] sessionsKeys = store.keys();
				if (sessionCount > maxSessions) {
					sessionCount = maxSessions;
					fp.addNoticeMessage(request.getMessage(MessageConstants.SESSIONS_SHOWING_MAX, maxSessions,
							sessionsKeys.length));
				}
				for (int i = 0; i < sessionCount && i < sessionsKeys.length;) {
					Optional<Session> sessionMetaData = getSessionMetaData(manager.findSession(sessionsKeys[i]));
					if (sessionMetaData.isPresent()) {
						immutableSessions.add(sessionMetaData.get());
						i++;
					}
				}

			} else if (maxSessions >= manager.getActiveSessions()) {
				for (org.apache.catalina.Session session : manager.findSessions()) {
					Optional<Session> sessionMetaData = getSessionMetaData(session);
					if (sessionMetaData.isPresent()) {
						immutableSessions.add(sessionMetaData.get());
					}
				}
			} else {
				fp.addErrorMessage(request.getMessage(MessageConstants.SESSIONS_TOO_MANY, manager.getActiveSessions(),
						maxSessions));
			}
		} catch (IOException e) {
			log.error("error while retrieving sessions keys from store", e);
			fp.addErrorMessage(request.getMessage(MessageConstants.SESSIONS_READ_ERROR));
		}
		return immutableSessions;
	}

	private Optional<Session> getSessionMetaData(org.apache.catalina.Session session) {
		Session metaData = null;
		if (null != session) {
			metaData = (Session) session.getSession().getAttribute(SessionListener.META_DATA);
			if (null == metaData) {
				metaData = new Session(session.getId());
			}
		}
		return Optional.ofNullable(metaData);
	}

	private Manager getManager(Environment environment) {
		ServletContext servletCtx = ((DefaultEnvironment) environment).getServletContext();
		return (Manager) servletCtx.getAttribute(SessionListener.SESSION_MANAGER);
	}

	protected List<Session> getSessions(Options options, Request request, Boolean currentSiteOnly,
			List<Session> imutableSessions, Set<String> userAgents, final String fSite, final String fSessid,
			final String fAgnt, String fUsr, final Date fcrBfDate, final Date fcrAfDate, final String fLgn) {
		Integer siteId = options.getInteger("site", "id");
		String currentSiteName = null == siteId ? null : getService().getNameForSite(siteId);
		List<Session> sessions = new ArrayList<>();
		for (Session session : imutableSessions) {
			String userAgent = session.getUserAgent();
			if (StringUtils.isBlank(userAgent)) {
				userAgent = request.getMessage(MessageConstants.USER_AGENT_NONE);
			}
			int idx = userAgent.indexOf('(');
			if (idx > 0) {
				userAgents.add(userAgent.substring(0, idx));
			} else {
				userAgents.add(userAgent);
			}

			boolean doAdd = true;
			if (Boolean.TRUE.equals(currentSiteOnly) && null != currentSiteName) {
				doAdd = currentSiteName.equals(session.getSite());
			}

			doAdd &= StringUtils.isBlank(fSessid) || session.getId().toLowerCase().startsWith(fSessid.toLowerCase());
			doAdd &= StringUtils.isBlank(fAgnt) || StringUtils.startsWith(session.getUserAgent(), fAgnt);

			boolean nameMatches = FilenameUtils.wildcardMatch(session.getUser(), fUsr, IOCase.INSENSITIVE);
			doAdd &= StringUtils.isBlank(fUsr) || nameMatches;
			doAdd &= null == fcrAfDate || session.getCreationTime().after(fcrAfDate);
			doAdd &= null == fcrBfDate || session.getCreationTime().before(fcrBfDate);
			doAdd &= StringUtils.isBlank(fSite) || session.getSite().equals(fSite);
			doAdd &= StringUtils.isBlank(fLgn)
					|| (StringUtils.isNotBlank(session.getUser()) && Boolean.TRUE.toString().equalsIgnoreCase(fLgn));

			if (doAdd) {
				Session cloned = session.clone();
				sessions.add(cloned);

				String currentSession = request.getEnvironment().getAttribute(Scope.SESSION,
						org.appng.api.Session.Environment.SID);
				if (cloned.getId().equals(currentSession)) {
					cloned.setAllowExpire(false);
				}
			}
		}
		return sessions;
	}

	protected void addFilters(Request request, Boolean currentSiteOnly, DataContainer dataContainer,
			Set<String> userAgents, final String fAgnt, String fDmn, String fUsr) {
		SelectionGroup selectionGroup = new SelectionGroup();
		dataContainer.getSelectionGroups().add(selectionGroup);
		selectionGroup.getSelections()
				.add(selectionFactory.getTextSelection(F_SESS, MessageConstants.ID, request.getParameter(F_SESS)));

		Selection userAgentFilter = selectionFactory.fromObjects(F_AGNT, MessageConstants.USER_AGENT,
				userAgents.toArray(), new OptionOwner.Selector() {
					public void select(Option o) {
						if (o.getValue().equals(fAgnt)) {
							o.setSelected(true);
						}
					}
				});
		selectionGroup.getSelections().add(userAgentFilter);

		selectionGroup.getSelections()
				.add(selectionFactory.getTextSelection(F_USR, MessageConstants.USER_NAME, request.getParameter(F_USR)));

		if (!currentSiteOnly) {
			selectionGroup.getSelections().add(getDomainFilter(request, fDmn));
		}

		Selection loggedinFilter = selectionFactory.fromObjects(F_LGN, MessageConstants.USER_LOGGED_IN,
				new String[] { Boolean.TRUE.toString() }, o -> StringUtils.EMPTY, request.getParameter(F_LGN));
		loggedinFilter.setType(SelectionType.CHECKBOX);
		selectionGroup.getSelections().add(loggedinFilter);

		Selection crAfFilter = selectionFactory.getDateSelection(F_CR_AF, MessageConstants.CREATED_AFTER,
				request.getParameter(F_CR_AF), MM_DD_HH_MM);
		selectionGroup.getSelections().add(crAfFilter);

		Selection crBfFilter = selectionFactory.getDateSelection(F_CR_BF, MessageConstants.CREATED_BEFORE,
				request.getParameter(F_CR_BF), MM_DD_HH_MM);
		selectionGroup.getSelections().add(crBfFilter);
	}

	private Date getDate(String dateString) {
		try {
			return StringUtils.isNotBlank(dateString) ? hourMinutes.parse(dateString) : null;
		} catch (ParseException e) {
			return null;
		}
	}

	protected Selection getDomainFilter(Request request, String fSite) {
		Map<String, Site> siteMap = request.getEnvironment().getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		List<NamedSite> sites = siteMap.values().stream().sorted((s1, s2) -> s1.getDomain().compareTo(s2.getDomain()))
				.map(s -> NamedSite.forSite(s)).collect(Collectors.toList());

		return new SelectionBuilder<NamedSite>(F_DMN).title(MessageConstants.DOMAIN).defaultOption(null, null)
				.type(SelectionType.SELECT).options(sites).selector(o -> o.getValue().equals(fSite)).build();
	}

	@Data
	@RequiredArgsConstructor
	@SuppressWarnings("serial")
	static class NamedSite implements Named<String> {
		private final String id;
		private final String name;
		private String description;

		static NamedSite forSite(Site s) {
			return new NamedSite(s.getName(), s.getDomain());
		}

	}

	protected boolean expire(String currentSession, String sessionId, String siteName, Manager manager) {
		try {
			org.apache.catalina.Session session = manager.findSession(sessionId);
			if (null != session) {
				Optional<Session> sessionMetaData = getSessionMetaData(session);
				if (sessionMetaData.isPresent() && !sessionMetaData.get().getId().equals(currentSession)
						&& (siteName == null || sessionMetaData.get().getSite().equals(siteName))) {
					session.expire();
					return true;
				}
			}
		} catch (IOException e) {
			log.error("error expiring session", e);
		}
		return false;
	}

}

