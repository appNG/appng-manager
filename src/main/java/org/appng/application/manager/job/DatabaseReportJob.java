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
package org.appng.application.manager.job;

import java.util.Collection;
import java.util.List;

import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.application.manager.ManagerSettings;
import org.appng.application.manager.business.PlatformEvents.EventFilter;
import org.appng.application.manager.service.PlatformEventService;
import org.appng.application.manager.service.RoleService;
import org.appng.core.domain.SubjectImpl;
import org.appng.core.service.DatabaseService;
import org.appng.mail.Mail;
import org.appng.mail.Mail.RecipientType;
import org.appng.mail.MailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatabaseReportJob extends ReportJobBase {

	private static final String ROLE_DATABASE_REPORT_RECEIVER = "Database report receiver";

	@Autowired
	DatabaseReportJob(PlatformEventService service, DatabaseService databaseService, MailTransport mailTransport,
			MessageSource messageSource, RoleService roleService) {
		super(service, mailTransport, messageSource, roleService);
	}

	public void execute(Site site, Application application) throws Exception {

		Properties properties = application.getProperties();
		List<String> receivers = properties.getList(ManagerSettings.DATABASE_REPORT_RECEIVERS, ";");

		Collection<SubjectImpl> reportReceivers = roleService.getSubjectsForRole(application,
				ROLE_DATABASE_REPORT_RECEIVER);

		if (!(receivers.isEmpty() && reportReceivers.isEmpty())) {
			Mail mail = mailTransport.createMail();

			addReceiver(mail, properties, ManagerSettings.DATABASE_REPORT_RECEIVERS, RecipientType.BCC);
			reportReceivers.forEach(r -> mail.addReceiver(r.getEmail(), r.getRealname(), RecipientType.BCC));

			mail.setSubject(properties.getString(ManagerSettings.DATABASE_REPORT_SUBJECT));
			mail.setFrom(properties.getString(ManagerSettings.DATABASE_REPORT_SENDER));
			mail.setTextContent(properties.getString(ManagerSettings.DATABASE_REPORT_TEXT));

			EventFilter eventFilter = new EventFilter();
			eventFilter.setEA(getIntervalStart());
			eventFilter.setEX("jdbc:");

			addReport(mail, eventFilter);

			mailTransport.send(mail);
		} else {
			log.info("No report receivers defined, set {} accordingly!", ManagerSettings.DATABASE_REPORT_RECEIVERS);
		}
	}

}
