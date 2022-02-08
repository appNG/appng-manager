/*
 * Copyright 2011-2022 the original author or authors.
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

import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.api.Session;
import org.appng.api.support.CallableAction;
import org.appng.application.manager.form.MailForm;
import org.appng.core.domain.SubjectImpl;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.appng.testsupport.validation.XPathDifferenceHandler;
import org.junit.Test;

public class MailTest extends AbstractTest {

	static {
		WritingXmlValidator.writeXml = false;
	}

	@Test
	public void testSendMail() throws Exception {
		SubjectImpl user = new SubjectImpl();
		user.setRealname("John Doe");
		user.setEmail("john@appng.org");
		environment.setAttribute(Scope.SESSION, Session.Environment.SUBJECT, user);

		MailForm form = new MailForm();
		form.applyDefaults(environment, "localhost", 25);

		environment.setAttribute(Scope.PLATFORM, Platform.Environment.APPNG_VERSION, "1.23");

		CallableAction sendMail = getAction("mailEvent", "sendMail").withParam(FORM_ACTION, "sendMail")
				.getCallableAction(form);
		sendMail.perform();
		XPathDifferenceHandler dh = new XPathDifferenceHandler(false);
		String fieldValue = "/action[1]/data[1]/result[1]/field[%s]/value[1]/text()[1]";
		dh.ignoreDifference(String.format(fieldValue, "2"));
		dh.ignoreDifference(String.format(fieldValue, "3"));
		dh.ignoreDifference(String.format(fieldValue, "6"));
		validate(sendMail.getAction(), dh);
	}

}
