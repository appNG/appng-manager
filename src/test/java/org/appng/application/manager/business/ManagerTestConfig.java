package org.appng.application.manager.business;

import javax.sql.DataSource;

import org.appng.application.manager.ManagerConfig;
import org.appng.core.domain.PlatformEventListener;
import org.appng.core.service.CoreService;
import org.appng.core.service.DatabaseService;
import org.appng.core.service.InitializerService;
import org.appng.core.service.LdapService;
import org.appng.core.service.TemplateService;
import org.appng.persistence.ApplicationConfigJPA;
import org.appng.testsupport.ApplicationTestConfig;
import org.appng.testsupport.TestBase;
import org.appng.testsupport.persistence.ApplicationConfigDataSource;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@ImportResource(TestBase.BEANS_PATH)
@Import({ ManagerConfig.class, ApplicationConfigJPA.class, ApplicationTestConfig.class })
public class ManagerTestConfig extends ApplicationConfigDataSource {

	@Bean
	@Primary
	@Override
	@Qualifier("dataSource")
	public FactoryBean<DataSource> datasource(String database) {
		return super.datasource("appng-manager");
	}

	@Bean
	@Primary
	Sessions sessions() {
		return new SessionsTestBean();
	}

	@Bean
	public DatabasePopulator databasePopulator(DataSource dataSource) {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("sql/testdata.sql"));
		DatabasePopulatorUtils.execute(databasePopulator, dataSource);
		return databasePopulator;
	}

	@Bean
	public PlatformEventListener platformEventListener() {
		return new PlatformEventListener();
	}

	@Bean
	public DatabaseService databaseService() {
		return new DatabaseService();
	}

	@Bean
	public LdapService ldapService() {
		return new LdapService();
	}

	@Bean
	public CoreService coreService() {
		return new CoreService();
	}

	@Bean
	public InitializerService initializerService() {
		return new InitializerService();
	}

	@Bean
	public TemplateService templateService() {
		return new TemplateService();
	}

}
