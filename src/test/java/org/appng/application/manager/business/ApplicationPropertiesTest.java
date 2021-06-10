/*
 * Copyright 2011-2020 the original author or authors.
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

import org.appng.api.FieldProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.model.Property.Type;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.PropertyForm;
import org.appng.core.domain.ApplicationImpl;
import org.appng.core.domain.PropertyImpl;
import org.junit.Test;
import org.junit.runner.OrderWith;
import org.junit.runner.manipulation.Alphanumeric;

@OrderWith(Alphanumeric.class)
public class ApplicationPropertiesTest extends AbstractTest {

	private static final String PROPERTY_EVENT = "propertyEvent";

	private static final String TESTPROPERTY = "testproperty";
	private static final String PROPERTY_NAME = "platform.application.manager." + TESTPROPERTY;

	@Test
	public void testCreate() throws ProcessingException, IOException {

		ApplicationImpl application = new ApplicationImpl();
		application.setName("manager");
		applicationRepository.save(application);

		ActionCall actionCall = getAction(PROPERTY_EVENT, "create-application-property").withParam(FORM_ACTION,
				"create-application-property").withParam("appid", "1");
		CallableAction action = actionCall.getCallableAction(new PropertyForm(new PropertyImpl(TESTPROPERTY, "5")));
		FieldProcessor perform = action.perform();
		validate(perform.getMessages());

		actionCall.getCallableAction(new PropertyForm(new PropertyImpl("anotherproperty", "foobar"))).perform();
	}

	@Test
	public void testShowAll() throws ProcessingException, IOException {
		addParameter("sortApplication-properties", "shortName:asc");
		initParameters();
		CallableDataSource dataSource = getDataSource("application-properties").withParam("appid", "1")
				.getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource());
	}

	@Test
	public void testShowOne() throws ProcessingException, IOException {
		CallableDataSource dataSource = getDataSource("property").withParam("id", PROPERTY_NAME)
				.getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource());
	}

	@Test
	public void testUpdate() throws ProcessingException, IOException {
		ActionCall actionCall = getAction(PROPERTY_EVENT, "update-application-property")
				.withParam(FORM_ACTION, "update-application-property").withParam("propertyid", PROPERTY_NAME)
				.withParam("appid", "1");
		PropertyImpl property = new PropertyImpl(TESTPROPERTY, "7", "9");
		property.setType(Type.INT);
		property.setClob("");
		CallableAction action = actionCall
				.getCallableAction(new PropertyForm(property));
		FieldProcessor perform = action.perform();
		validate(perform.getMessages());

		CallableDataSource dataSource = getDataSource("property").withParam("appid", "1")
				.withParam("id", PROPERTY_NAME).getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource(), "-data");
	}
}
