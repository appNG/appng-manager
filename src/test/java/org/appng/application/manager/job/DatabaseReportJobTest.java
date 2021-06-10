package org.appng.application.manager.job;

import java.io.InputStream;
import java.util.Properties;

import org.appng.application.manager.ManagerSettings;
import org.appng.application.manager.business.AbstractTest;
import org.appng.mail.Mail;
import org.appng.mail.Mail.RecipientType;
import org.appng.mail.MailTransport;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.net.MediaType;

@Ignore
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
