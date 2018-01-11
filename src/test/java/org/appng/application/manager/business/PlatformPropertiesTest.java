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
package org.appng.application.manager.business;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appng.api.FieldProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.SiteProperties;
import org.appng.api.model.Property;
import org.appng.api.model.SimpleProperty;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.PropertyForm;
import org.appng.core.domain.PropertyImpl;
import org.appng.xml.platform.FieldDef;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PlatformPropertiesTest extends AbstractTest {

	private static final String PROPERTY_EVENT = "propertyEvent";

	@Test
	public void testCreate() throws ProcessingException, IOException {
		PropertyForm form = new PropertyForm(new PropertyImpl("testproperty", "5"));
		ActionCall actionCall = getCreateAction();
		CallableAction action = actionCall.getCallableAction(form);
		FieldProcessor perform = action.perform();
		validate(perform.getMessages());

		actionCall.getCallableAction(new PropertyForm(new PropertyImpl("anotherproperty", "foobar"))).perform();
	}

	@Test
	public void testCreateStringOrClob() throws ProcessingException, IOException {
		PropertyForm form = new PropertyForm(new PropertyImpl("testproperty", "5"));
		form.getProperty().setClob("foo");
		CallableAction action = getCreateAction().getCallableAction(form);
		FieldProcessor fp = action.perform();
		assertError(fp.getField("property.defaultString"), "Please set the value or the multilined value.");
		assertError(fp.getField("property.clob"), "Please set the value or the multilined value.");
	}

	private void assertError(FieldDef field, String message) {
		String actual = field.getMessages().getMessageList().get(0).getContent();
		Assert.assertEquals(message, actual);
	}

	protected ActionCall getCreateAction() {
		return getAction(PROPERTY_EVENT, "create-platform-property").withParam(FORM_ACTION, "create-platform-property");
	}

	@Test
	public void testShowAll() throws ProcessingException, IOException {
		addParameter("sortPlatformProperties", "id:asc");
		initParameters();
		CallableDataSource dataSource = getDataSource("platformProperties").getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource());
	}

	@Test
	public void testShowOne() throws ProcessingException, IOException {
		CallableDataSource dataSource = getDataSource("property").withParam("id", "platform.testproperty")
				.getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource());
	}

	@Test
	public void testUpdate() throws ProcessingException, IOException {
		PropertyImpl property = new PropertyImpl("testproperty", "7", "9");
		property.setClob("");
		CallableAction action = getUpdateAction(new PropertyForm(property));
		FieldProcessor perform = action.perform();
		validate(perform.getMessages());

		CallableDataSource dataSource = getDataSource("property").withParam("id", "platform.testproperty")
				.getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource(), "-data");
	}

	@Test
	public void testUpdateStringOrClob() throws ProcessingException, IOException {
		PropertyImpl property = new PropertyImpl("testproperty", "7", "9");
		property.setClob("aa");
		CallableAction action = getUpdateAction(new PropertyForm(property));
		FieldProcessor fp = action.perform();
		assertError(fp.getField("property.actualString"), "Please set the value or the multilined value.");
		assertError(fp.getField("property.clob"), "Please set the value or the multilined value.");
	}

	protected CallableAction getUpdateAction(PropertyForm form) throws ProcessingException {
		ActionCall actionCall = getAction(PROPERTY_EVENT, "update-platform-property")
				.withParam(FORM_ACTION, "update-platform-property").withParam("propertyid", "platform.testproperty");
		CallableAction action = actionCall.getCallableAction(form);
		return action;
	}

	@Override
	protected List<Property> getSiteProperties(String prefix) {
		List<Property> siteProperties = new ArrayList<Property>();
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.SERVICE_PATH, "/service"));
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.MANAGER_PATH, "ws"));
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.DEFAULT_PAGE_SIZE, "10"));
		return siteProperties;
	}

}
