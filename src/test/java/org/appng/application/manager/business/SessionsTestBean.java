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
		if (!id.equals(currentSession)) {
			Mockito.when(mocked.isExpired()).thenReturn(true);
		}
		Mockito.when(mocked.getId()).thenReturn(id);
		SESSIONS.add(mocked);
	}

}
