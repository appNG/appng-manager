package org.appng.application.manager;

import java.nio.charset.StandardCharsets;

import org.appng.api.AppNgApplication;
import org.appng.api.ApplicationConfig;
import org.appng.api.support.OptionGroupFactory;
import org.appng.api.support.SelectionFactory;
import org.appng.application.manager.service.ManagerService;
import org.appng.application.manager.service.Service;
import org.appng.mail.MailTransport;
import org.appng.mail.impl.DefaultTransport;
import org.appng.persistence.repository.SearchRepositoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@AppNgApplication(entityPackage = "org.appng.core.domain")
@ComponentScan("org.appng.application.manager")
@EnableJpaRepositories(basePackages = "org.appng.core.repository", repositoryBaseClass = SearchRepositoryImpl.class)
public class ManagerConfig extends ApplicationConfig {

	@Bean
	public Service service(SelectionFactory selectionFactory,
			org.appng.api.support.OptionGroupFactory optionGroupFactory) {
		ManagerService service = new ManagerService(selectionFactory, optionGroupFactory);
		ResourceBundleMessageSource timezoneMessages = new ResourceBundleMessageSource();
		timezoneMessages.setAlwaysUseMessageFormat(true);
		timezoneMessages.setBasename("timetones");
		timezoneMessages.setDefaultEncoding(StandardCharsets.UTF_8.name());
		timezoneMessages.setUseCodeAsDefaultMessage(true);
		service.setTimezoneMessages(timezoneMessages);
		return service;
	}

	@Bean
	public OptionGroupFactory OptionGroupFactory() {
		return new OptionGroupFactory();
	}

	@Bean
	public MailTransport mailTransport(@Value("${site.mailHost:localhost}") String host,
			@Value("${site.mailPort:25}") int port, @Value("${site.mailDisabled:true}") boolean disableSend) {
		DefaultTransport mailTransport = new DefaultTransport(host, port);
		mailTransport.setDisableSend(disableSend);
		return mailTransport;
	}

}
