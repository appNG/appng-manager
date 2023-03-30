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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.appng.api.ScheduledJob;
import org.appng.api.model.Properties;
import org.appng.application.manager.business.PlatformEvents.EventFilter;
import org.appng.application.manager.business.webservice.PlatformEventExport;
import org.appng.application.manager.service.PlatformEventService;
import org.appng.application.manager.service.RoleService;
import org.appng.core.domain.PlatformEvent;
import org.appng.mail.Mail;
import org.appng.mail.Mail.RecipientType;
import org.appng.mail.MailException;
import org.appng.mail.MailTransport;
import org.springframework.context.MessageSource;

import com.google.common.net.MediaType;

import lombok.Data;

@Data
abstract class ReportJobBase implements ScheduledJob {

	protected static final int MINUS_1 = -1;
	private static final String INTERVAL = "interval";
	protected String description;
	protected Map<String, Object> jobDataMap = new HashMap<>();
	protected MailTransport mailTransport;
	protected PlatformEventService service;
	protected MessageSource messageSource;
	protected ReportIntervall interval = ReportIntervall.DAY;
	protected RoleService roleService;

	ReportJobBase(PlatformEventService service, MailTransport mailTransport, MessageSource messageSource,
			RoleService roleService) {
		this.mailTransport = mailTransport;
		this.service = service;
		this.messageSource = messageSource;
		this.roleService = roleService;
	}

	protected void addReceiver(Mail mail, Properties properties, String propertyName, RecipientType recipientType) {
		List<String> receivers = properties.getList(propertyName, ";");
		receivers.forEach(r -> mail.addReceiver(StringUtils.trim(r), recipientType));
	}

	protected Date getIntervalStart() {
		if (jobDataMap.containsKey(INTERVAL)) {
			interval = ReportIntervall.valueOf((String) jobDataMap.get(INTERVAL));
		}
		return interval.getIntervalStart();
	}

	protected void addReport(Mail mail, EventFilter eventFilter) throws IOException, MailException {
		List<PlatformEvent> events = service.getEvents(eventFilter);
		ByteArrayOutputStream out = PlatformEventExport.getEventReport(events, messageSource);

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		String fileName = PlatformEventExport.getAttachmentName();
		mail.addAttachment(in, fileName, MediaType.OPENDOCUMENT_SPREADSHEET.toString());
	}

	enum ReportIntervall {

		HOUR, DAY, WEEK, MONTH;

		Date getIntervalStart() {
			Date now = new Date();
			switch (this) {
			case HOUR:
				return DateUtils.addHours(now, -1);
			case DAY:
				return DateUtils.addDays(now, -1);
			case WEEK:
				return DateUtils.addWeeks(now, -1);
			case MONTH:
				return DateUtils.addMonths(now, -1);
			}
			return null;
		}
	}

}
