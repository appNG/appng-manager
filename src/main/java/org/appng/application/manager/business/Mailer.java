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

import org.appng.api.ActionProvider;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.appng.application.manager.MessageConstants;
import org.appng.application.manager.form.MailForm;
import org.appng.mail.Mail;
import org.appng.mail.Mail.RecipientType;
import org.appng.mail.MailException;
import org.appng.mail.MailTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Mailer implements DataProvider, ActionProvider<MailForm> {

	private final MailTransport mailTransport;
	private @Value("${site.mailHost}") String mailHost;
	private @Value("${site.mailPort}") int mailPort;

	@Override
	public DataContainer getData(Site site, Application application, Environment env, Options options, Request request,
			FieldProcessor fp) {
		DataContainer dataContainer = new DataContainer(fp);
		MailForm mailForm = new MailForm();
		mailForm.applyDefaults(env, mailHost, mailPort);
		dataContainer.setItem(mailForm);
		return dataContainer;
	}

	@Override
	public void perform(Site site, Application application, Environment environment, Options options, Request request,
			MailForm mailForm, FieldProcessor fp) {
		Mail mail = mailTransport.createMail();
		mail.setFrom(mailForm.getSenderAddress(), mailForm.getSenderName());
		mail.setSubject(mailForm.getSubject());
		mail.setTextContent(mailForm.getTextContent());
		mail.setHTMLContent(mailForm.getHtmlContent());
		mail.addReceiver(mailForm.getReceiverAddress(), mailForm.getReceiverName(), RecipientType.TO);
		try {
			if (null != mailForm.getAttachment()) {
				mail.addAttachment(mailForm.getAttachment().getFile(), mailForm.getAttachment().getContentType());
			}
			if (mailTransport.isDisableSend()) {
				fp.addNoticeMessage(request.getMessage(MessageConstants.MAIL_DISABLED));
			} else {
				mailTransport.send(mail);
				fp.addOkMessage(request.getMessage(MessageConstants.MAIL_SUCCESS, mailForm.getReceiverAddress()));
			}
		} catch (MailException m) {
			fp.addErrorMessage(request.getMessage(MessageConstants.MAIL_ERROR, mailForm.getReceiverAddress()));
			fp.addErrorMessage(m.getClass().getName() + ": " + m.getMessage());
			log.error("error sending mail", m);
		}

	}

}
