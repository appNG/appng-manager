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
package org.appng.application.manager.business;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.appng.api.Scope;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.core.controller.Session;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SessionsTest extends AbstractTest {

	@Test
	public void testShowSessions() throws Exception {
		setSessions();
		environment.setAttribute(Scope.SESSION, org.appng.api.Session.Environment.SID, "47124712");
		CallableDataSource siteDatasource = getDataSource("sessions")
				.withParam("deleteLink", "/system?action=expire&sessid=").getCallableDataSource();
		siteDatasource.perform("test");
		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testShowSessionsFiltered() throws Exception {
		addParameter("fAgnt", "Mozilla");
		initParameters();
		Session first = setSessions().get(0);
		Mockito.when(first.getUserAgent()).thenReturn(null);
		environment.setAttribute(Scope.SESSION, org.appng.api.Session.Environment.SID, "47124712");
		CallableDataSource siteDatasource = getDataSource("sessions")
				.withParam("deleteLink", "/system?action=expire&sessid=").getCallableDataSource();
		siteDatasource.perform("test");
		validate(siteDatasource.getDatasource());
	}

	@Test
	public void testExpire() throws Exception {
		Session session = new Session("47124712");
		environment.setAttribute(Scope.PLATFORM, "sessions", Arrays.asList(session));
		CallableAction callableAction = getAction("sessionEvent", "expire").withParam("action", "expire")
				.withParam("sessid", "47124712").getCallableAction(null);
		callableAction.perform();
		Assert.assertTrue(session.isExpired());
	}

	@Test
	public void testExpireAll() throws Exception {
		Session sessionA = new Session("47114711");
		Session sessionB = new Session("47124712");
		Session sessionC = Mockito.spy(new Session("47134713"));
		Mockito.when(sessionC.isAllowExpire()).thenReturn(false);
		environment.setAttribute(Scope.SESSION, org.appng.api.Session.Environment.SID, sessionC.getId());
		environment.setAttribute(Scope.PLATFORM, "sessions", Arrays.asList(sessionA, sessionB, sessionC));
		CallableAction callableAction = getAction("sessionEvent", "expireAll").withParam("action", "expireAll")
				.getCallableAction(null);
		callableAction.perform();
		Assert.assertTrue(sessionA.isExpired());
		Assert.assertTrue(sessionB.isExpired());
		Assert.assertFalse(sessionC.isExpired());
	}

	private List<Session> setSessions() throws ParseException {
		List<Session> sessions = new ArrayList<Session>();
		Session sessionA = Mockito.mock(Session.class);
		Mockito.when(sessionA.getId()).thenReturn("47114711");
		Mockito.when(sessionA.getShortId()).thenReturn("47114711");
		Mockito.when(sessionA.getIp()).thenReturn("127.0.0.1");
		Mockito.when(sessionA.getCreationTime()).thenReturn(DateUtils.parseDate("1400", "HHmm"));
		Mockito.when(sessionA.getLastAccessedTime()).thenReturn(DateUtils.parseDate("1407", "HHmm"));
		Mockito.when(sessionA.getDomain()).thenReturn("http://www.example.com");
		Mockito.when(sessionA.getExpiryDate()).thenReturn(DateUtils.parseDate("1430", "HHmm"));
		Mockito.when(sessionA.getMaxInactiveInterval()).thenReturn(1800);
		Mockito.when(sessionA.getRequests()).thenReturn(100);
		Mockito.when(sessionA.getSite()).thenReturn("somesite");
		Mockito.when(sessionA.getUser()).thenReturn("johndoe");
		Mockito.when(sessionA.getUserAgent()).thenReturn("Mozilla");
		Mockito.when(sessionA.isAllowExpire()).thenReturn(true);
		Mockito.when(sessionA.clone()).thenReturn(sessionA);
		sessions.add(sessionA);

		Session sessionB = Mockito.mock(Session.class);
		Mockito.when(sessionB.getId()).thenReturn("47124712");
		Mockito.when(sessionB.getShortId()).thenReturn("47124712");
		Mockito.when(sessionB.getIp()).thenReturn("127.0.0.1");
		Mockito.when(sessionB.getCreationTime()).thenReturn(DateUtils.parseDate("1500", "HHmm"));
		Mockito.when(sessionA.getLastAccessedTime()).thenReturn(DateUtils.parseDate("1508", "HHmm"));
		Mockito.when(sessionB.getDomain()).thenReturn("http://www.example.com");
		Mockito.when(sessionB.getExpiryDate()).thenReturn(DateUtils.parseDate("1530", "HHmm"));
		Mockito.when(sessionB.getMaxInactiveInterval()).thenReturn(1800);
		Mockito.when(sessionB.getRequests()).thenReturn(1000);
		Mockito.when(sessionB.getSite()).thenReturn("somesite");
		Mockito.when(sessionB.getUser()).thenReturn("johndoe");
		Mockito.when(sessionB.getUserAgent()).thenReturn("Mozilla");
		Mockito.when(sessionB.isAllowExpire()).thenReturn(false);
		Mockito.when(sessionB.clone()).thenReturn(sessionB);
		sessions.add(sessionB);
		environment.setAttribute(Scope.PLATFORM, "sessions", sessions);
		return sessions;
	}
}
