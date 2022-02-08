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
package org.appng.application.manager.job;

import java.io.InputStream;
import java.util.Properties;

import org.appng.application.manager.ManagerSettings;
import org.appng.application.manager.business.AbstractTest;
import org.appng.mail.Mail;
import org.appng.mail.Mail.RecipientType;
import org.appng.mail.MailTransport;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.net.MediaType;

public class DatabaseReportJobTest extends AbstractTest {

	@Autowired
	DatabaseReportJob job;

	public DatabaseReportJobTest() {
	}

	@Test
	public void testJob() throws Exception {
		MailTransport mailTransport = Mockito.mock(MailTransport.class);
		Mail mail = Mockito.mock(Mail.class);
		Mockito.when(mailTransport.createMail()).thenReturn(mail);
		job.setMailTransport(mailTransport);
		job.execute(site, application);

		org.appng.api.model.Properties properties = application.getProperties();
		Mockito.verify(mail).addReceiver(properties.getString(ManagerSettings.DATABASE_REPORT_RECEIVERS),
				RecipientType.BCC);
		Mockito.verify(mail).setFrom(properties.getString(ManagerSettings.DATABASE_REPORT_SENDER));
		Mockito.verify(mail).setSubject(properties.getString(ManagerSettings.DATABASE_REPORT_SUBJECT));
		Mockito.verify(mail).setTextContent(properties.getString(ManagerSettings.DATABASE_REPORT_TEXT));
		Mockito.verify(mail).addAttachment(Mockito.any(InputStream.class), Mockito.any(),
				Mockito.eq(MediaType.OPENDOCUMENT_SPREADSHEET.toString()));
		Mockito.verify(mailTransport).send(mail);
	}

	@Override
	protected Properties getProperties() {
		Properties properties = super.getProperties();
		properties.put(ManagerSettings.DATABASE_REPORT_RECEIVERS, "john@doe.org");
		return properties;
	}
}
