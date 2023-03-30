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
package org.appng.application.manager.business;

import java.io.IOException;

import org.appng.api.FieldProcessor;
import org.appng.api.ProcessingException;
import org.appng.api.model.Property.Type;
import org.appng.api.support.CallableAction;
import org.appng.api.support.CallableDataSource;
import org.appng.application.manager.form.PropertyForm;
import org.appng.core.domain.PropertyImpl;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NodePropertiesTest extends AbstractTest {

	static {
		WritingXmlValidator.writeXml = false;
	}

	@Test
	public void testACreate() throws ProcessingException, IOException {
		ActionCall actionCall = getAction("propertyEvent", "create-node-property")
				.withParam(FORM_ACTION, "create-node-property").withParam("nodeId", "foobar");
		PropertyImpl property = new PropertyImpl("foo", "bar");
		CallableAction action = actionCall.getCallableAction(new PropertyForm(property));
		FieldProcessor perform = action.perform();
		validate(perform.getMessages());
	}

	@Test
	public void testUpdate() throws ProcessingException, IOException {
		PropertyImpl property = new PropertyImpl("foo", "rab");
		property.setType(Type.TEXT);
		PropertyForm propertyForm = new PropertyForm(property);
		CallableAction action = getAction("propertyEvent", "update-node-property")
				.withParam(FORM_ACTION, "update-node-property").withParam("propertyId", "platform.node.foobar.foo")
				.getCallableAction(propertyForm);
		FieldProcessor perform = action.perform();
		validate(perform.getMessages());
	}

	@Test
	public void testShowAll() throws ProcessingException, IOException {
		CallableDataSource dataSource = getDataSource("node-properties").withParam("nodeId", "foobar")
				.getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource());
	}

	@Test
	public void testShowOne() throws ProcessingException, IOException {
		CallableDataSource dataSource = getDataSource("property").withParam("id", "platform.node.foobar.foo")
				.getCallableDataSource();
		dataSource.perform("test");
		validate(dataSource.getDatasource());
	}

	@Test
	public void testZDelete() throws ProcessingException, IOException {
		CallableAction action = getAction("propertyEvent", "delete-node-property")
				.withParam(FORM_ACTION, "delete-node-property").withParam("propertyId", "platform.node.foobar.foo")
				.getCallableAction(new PropertyForm());
		FieldProcessor perform = action.perform();
		validate(perform.getMessages());
	}
}
