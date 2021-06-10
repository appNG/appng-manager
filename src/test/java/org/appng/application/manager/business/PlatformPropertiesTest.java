/*
 * Copyright 2011-2021 the original author or authors.
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
import org.appng.api.model.Property.Type;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.PropertyForm;
import org.appng.core.domain.PropertyImpl;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.appng.xml.platform.FieldDef;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PlatformPropertiesTest extends AbstractTest {

	private static final String PROPERTY_EVENT = "propertyEvent";

	static {
		WritingXmlValidator.writeXml = false;
	}

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
		assertError(fp.getField("property.defaultString"), "Please set the value or the multiline value.");
		assertError(fp.getField("property.clob"), "Please set the value or the multiline value.");
	}

	@Test
	public void testCreateBoolean() throws ProcessingException, IOException {
		PropertyImpl booleanProp = new PropertyImpl("booleanProp", null);
		booleanProp.setDescription("this is bool, man!");
		PropertyForm form = new PropertyForm(booleanProp);
		form.getProperty().setDefaultString("true");
		CallableAction action = getCreateAction().getCallableAction(form);
		action.perform();

		CallableDataSource dataSource = getDataSource("property").withParam("id", "platform.booleanProp")
				.getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource());
	}

	@Test
	public void testCreateMultiline() throws ProcessingException, IOException {
		PropertyImpl multilineProp = new PropertyImpl("multilineProp", null);
		multilineProp.setDescription("Toto - Hold the line");
		PropertyForm form = new PropertyForm(multilineProp);
		form.getProperty().setClob("Hold the line,\nlove isn't always on time,\noh oh oh");
		CallableAction action = getCreateAction().getCallableAction(form);
		action.perform();

		CallableDataSource dataSource = getDataSource("property").withParam("id", "platform.multilineProp")
				.getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource());
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
		addParameter("sortPlatformProperties", "shortName:asc");
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
		property.setType(Type.INT);
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
		property.setType(Type.INT);
		property.setClob("aa");
		CallableAction action = getUpdateAction(new PropertyForm(property));
		FieldProcessor fp = action.perform();
		FieldDef value = fp.getField("property.value");
		Assert.assertNull(value.getMessages());
	}

	protected CallableAction getUpdateAction(PropertyForm form) throws ProcessingException {
		return getAction(PROPERTY_EVENT, "update-platform-property").withParam(FORM_ACTION, "update-platform-property")
				.withParam("propertyid", "platform.testproperty").getCallableAction(form);
	}

	@Override
	protected List<Property> getSiteProperties(String prefix) {
		List<Property> siteProperties = new ArrayList<>();
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.SERVICE_PATH, "/service"));
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.MANAGER_PATH, "ws"));
		siteProperties.add(new SimpleProperty(prefix + SiteProperties.DEFAULT_PAGE_SIZE, "10"));
		return siteProperties;
	}

}
