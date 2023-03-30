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
package org.appng.application.manager.form;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.appng.api.Environment;
import org.appng.api.FileUpload;
import org.appng.api.NotBlank;
import org.appng.api.Platform;
import org.appng.api.Scope;
import org.appng.forms.FormUpload;

import lombok.Data;

@Data
public class MailForm {

	private String mailHostAndPort;
	private @NotBlank String senderAddress;
	String senderName;
	private @NotBlank String receiverAddress;
	String receiverName;
	private @NotBlank String subject;
	private @NotBlank String textContent;
	String htmlContent;
	private @FileUpload(minCount = 0, fileTypes = "jpg, png, txt, pdf, xls, xlsx, doc, docx") FormUpload attachment;

	public void applyDefaults(Environment env, String mailHost, int mailPort) {
		String hostName = getHostName();
		String appngVersion = env.getAttribute(Scope.PLATFORM, Platform.Environment.APPNG_VERSION);
		setSubject(String.format("Testmail from %s", hostName));
		setSenderName(String.format("appNG %s on %s", appngVersion, hostName));
		setSenderAddress(String.format("appNG_%s@%s", appngVersion, hostName));
		setReceiverAddress(env.getSubject().getEmail());
		setReceiverName(env.getSubject().getRealname());
		setTextContent("A test.");
		setHtmlContent("<p>A <strong>test</strong>.</p>");
		setMailHostAndPort(String.format("%s:%s", mailHost, mailPort));
	}

	protected String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			//
		}
		return "<unknown>";
	}

}
