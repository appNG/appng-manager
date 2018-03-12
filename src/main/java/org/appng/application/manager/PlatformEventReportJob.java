/*
 * Copyright 2011-2018 the original author or authors.
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
package org.appng.application.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.appng.api.ScheduledJob;
import org.appng.api.model.Application;
import org.appng.api.model.Properties;
import org.appng.api.model.Site;
import org.appng.application.manager.business.PlatformEvents.EventFilter;
import org.appng.application.manager.business.webservice.PlatformEventExport;
import org.appng.application.manager.service.PlatformEventService;
import org.appng.core.domain.PlatformEvent;
import org.appng.mail.Mail;
import org.appng.mail.Mail.RecipientType;
import org.appng.mail.MailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.google.common.net.MediaType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PlatformEventReportJob implements ScheduledJob {

	private static final int MINUS_1 = -1;
	private static final String INTERVAL = "interval";
	private String description;
	private Map<String, Object> jobDataMap;
	private MailTransport mailTransport;
	private PlatformEventService service;
	private MessageSource messageSource;
	private Interval interval = Interval.DAY;

	enum Interval {
		HOUR, DAY, WEEK, MONTH;
	}

	@Autowired
	public PlatformEventReportJob(PlatformEventService service, MailTransport mailTransport,
			MessageSource messageSource) {
		this.mailTransport = mailTransport;
		this.service = service;
		this.messageSource = messageSource;
	}

	public void execute(Site site, Application application) throws Exception {
		Mail mail = mailTransport.createMail();

		Properties properties = application.getProperties();
		List<String> receivers = properties.getList(ManagerSettings.EVENT_REPORT_RECEIVERS, ";");
		if (!receivers.isEmpty()) {
			receivers.forEach(r -> mail.addReceiver(StringUtils.trim(r), RecipientType.TO));

			mail.setSubject(properties.getString(ManagerSettings.EVENT_REPORT_SUBJECT));
			mail.setFrom(properties.getString(ManagerSettings.EVENT_REPORT_SENDER));
			mail.setTextContent(properties.getString(ManagerSettings.EVENT_REPORT_TEXT));

			EventFilter eventFilter = new EventFilter();
			String eventTypes = properties.getString(ManagerSettings.EVENT_REPORT_TYPES);
			if (null != eventTypes) {
				Arrays.asList(eventTypes.split(StringUtils.SPACE)).forEach(t -> eventFilter.getET().add(t));
			}

			if (jobDataMap.containsKey(INTERVAL)) {
				interval = Interval.valueOf((String) jobDataMap.get(INTERVAL));
			}
			Date createdAfter = new Date();
			switch (interval) {
			case HOUR:
				createdAfter = DateUtils.addHours(createdAfter, MINUS_1);
				break;
			case DAY:
				createdAfter = DateUtils.addDays(createdAfter, MINUS_1);
				break;
			case WEEK:
				createdAfter = DateUtils.addWeeks(createdAfter, MINUS_1);
				break;
			case MONTH:
				createdAfter = DateUtils.addMonths(createdAfter, MINUS_1);
			}

			eventFilter.setEA(createdAfter);
			List<PlatformEvent> events = service.getEvents(eventFilter);
			ByteArrayOutputStream out = PlatformEventExport.getEventReport(events, messageSource);

			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			String fileName = PlatformEventExport.getAttachmentName();
			mail.addAttachment(in, fileName, MediaType.OPENDOCUMENT_SPREADSHEET.toString());
			mailTransport.send(mail);
		} else {
			log.info("No report receivers defined, set {} accordingly!", ManagerSettings.EVENT_REPORT_RECEIVERS);
		}
	}

}
