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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.appng.api.model.Site;
import org.appng.api.support.OptionOwner;
import org.appng.api.support.SelectionFactory;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.service.ServiceAware;
import org.appng.core.controller.Controller;
import org.appng.core.controller.Session;
import org.appng.core.controller.SessionListener;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.Selection;
import org.appng.xml.platform.SelectionGroup;
import org.appng.xml.platform.SelectionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
@org.springframework.context.annotation.Scope("request")
public class Sessions extends ServiceAware implements ActionProvider<Void>, DataProvider {

	private static final String F_CR_AF = "fCrAf";
	private static final String F_CR_BF = "fCrBf";
	private static final String F_AGNT = "fAgnt";
	private static final String F_SESS = "fSess";
	private static final String F_DMN = "fDmn";
	private static final String F_USR = "fUsr";
	private static final String MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
	private final FastDateFormat hourMinutes = FastDateFormat.getInstance(MM_DD_HH_MM);

	@Autowired
	private SelectionFactory selectionFactory;

	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			Void formBean, FieldProcessor fieldProcessor) {
		String sessionId = options.getOptionValue("session", "id");
		List<Session> sessions = environment.getAttribute(Scope.PLATFORM, "sessions");
		String currentSession = environment.getAttribute(Scope.SESSION, org.appng.api.Session.Environment.SID);
		Integer siteId = request.convert(options.getOptionValue("site", "id"), Integer.class);
		String siteName = null == siteId ? null : getService().getNameForSite(siteId);
		if (null == sessionId) {
			for (Session session : sessions) {
				expire(currentSession, session, siteName);
			}
		} else {
			Session session = sessions.get(sessions.indexOf(new Session(sessionId)));
			expire(currentSession, session, siteName);
		}
		environment.setAttribute(Scope.SESSION, Controller.EXPIRE_SESSIONS, true);
	}

	public DataContainer getData(Site site, Application application, Environment environment, Options options,
			Request request, FieldProcessor fieldProcessor) {
		DataContainer dataContainer = new DataContainer(fieldProcessor);
		List<Session> imutableSessions = environment.getAttribute(Scope.PLATFORM, SessionListener.SESSIONS);

		String fDmn = request.getParameter(F_DMN);
		String fSess = request.getParameter(F_SESS);
		String fAgnt = request.getParameter(F_AGNT);
		String fCrBf = request.getParameter(F_CR_BF);
		String fCrAf = request.getParameter(F_CR_AF);
		String fUsr = request.getParameter(F_USR);

		Set<String> userAgents = new TreeSet<String>();
		userAgents.add("");
		Boolean currentSiteOnly = request.convert(options.getOptionValue("site", "currentSiteOnly"), Boolean.class);
		List<Session> sessions = getSessions(options, request, currentSiteOnly, imutableSessions, userAgents, fDmn,
				fSess, fAgnt, fUsr, getDate(fCrBf), getDate(fCrAf));

		addFilters(request, currentSiteOnly, dataContainer, userAgents, fAgnt, fDmn, fUsr);

		dataContainer.setPage(sessions, fieldProcessor.getPageable());
		return dataContainer;
	}

	protected List<Session> getSessions(Options options, Request request, Boolean currentSiteOnly,
			List<Session> imutableSessions, Set<String> userAgents, final String fSite, final String fSessid,
			final String fAgnt, String fUsr, final Date fcrBfDate, final Date fcrAfDate) {
		Integer siteId = request.convert(options.getOptionValue("site", "id"), Integer.class);
		String currentSiteName = null == siteId ? null : getService().getNameForSite(siteId);
		List<Session> sessions = new ArrayList<Session>();
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
			doAdd &= StringUtils.isBlank(fAgnt) || session.getUserAgent().startsWith(fAgnt);

			boolean nameMatches = FilenameUtils.wildcardMatch(session.getUser(), fUsr, IOCase.INSENSITIVE);
			doAdd &= StringUtils.isBlank(fUsr) || nameMatches;
			doAdd &= null == fcrAfDate || session.getCreationTime().after(fcrAfDate);
			doAdd &= null == fcrBfDate || session.getCreationTime().before(fcrBfDate);
			doAdd &= StringUtils.isBlank(fSite) || session.getSite().equals(fSite);

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
		Selection crAfFilter = selectionFactory.getDateSelection(F_CR_AF, MessageConstants.CREATED_AFTER,
				request.getParameter(F_CR_AF), MM_DD_HH_MM);
		selectionGroup.getSelections().add(crAfFilter);
		Selection crBfFilter = selectionFactory.getDateSelection(F_CR_BF, MessageConstants.CREATED_BEFORE,
				request.getParameter(F_CR_BF), MM_DD_HH_MM);
		selectionGroup.getSelections().add(crBfFilter);
		if (!currentSiteOnly) {
			selectionGroup.getSelections().add(getDomainFilter(request, fDmn));
		}
		selectionGroup.getSelections()
				.add(selectionFactory.getTextSelection(F_USR, MessageConstants.USER_NAME, request.getParameter(F_USR)));
	}

	private Date getDate(String dateString) {
		try {
			return StringUtils.isNotBlank(dateString) ? hourMinutes.parse(dateString) : null;
		} catch (ParseException e) {
			return null;
		}
	}

	protected Selection getDomainFilter(Request request, String fSite) {
		Selection domainFilter = new Selection();
		domainFilter.setId(F_DMN);
		domainFilter.setTitle(new Label());
		domainFilter.getTitle().setId(MessageConstants.DOMAIN);
		domainFilter.getOptions().add(new Option());
		domainFilter.setType(SelectionType.SELECT);
		Map<String, Site> siteMap = request.getEnvironment().getAttribute(Scope.PLATFORM, Platform.Environment.SITES);
		List<Site> sites = new ArrayList<Site>(siteMap.values());
		Collections.sort(sites, new Comparator<Site>() {
			public int compare(Site o1, Site o2) {
				return o1.getDomain().compareTo(o2.getDomain());
			}
		});
		for (Site s : sites) {
			Option o = new Option();
			domainFilter.getOptions().add(o);
			o.setValue(s.getName());
			o.setName(s.getDomain());
			o.setSelected(s.getName().equals(fSite));
		}
		return domainFilter;
	}

	private void expire(String currentSession, Session session, String siteName) {
		if (!session.getId().equals(currentSession) && (siteName == null || session.getSite().equals(siteName))) {
			session.expire();
		}
	}
}
