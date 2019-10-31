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

import java.util.ArrayList;
import java.util.List;

import org.appng.core.controller.Session;
import org.mockito.Mockito;

public class SessionsTestBean extends Sessions {

	public static List<Session> SESSIONS;

	public List<Session> getSessions() {
		return SESSIONS;
	}

	public static void setSessions(List<Session> sessions) {
		SESSIONS = sessions;
	}

	public static Session getSession(String id) {
		return SESSIONS.stream().filter(s -> s.getId().equals(id)).findFirst().get();
	}

	protected void expire(String currentSession, Session session, String siteName) {
		SESSIONS = new ArrayList<>(SESSIONS);
		SESSIONS.remove(session);
		Session mocked = Mockito.mock(Session.class);
		String id = session.getId();
		Mockito.when(mocked.getId()).thenReturn(id);
		Mockito.when(mocked.isExpired()).thenReturn(!id.equals(currentSession));
		SESSIONS.add(mocked);
	}

}
