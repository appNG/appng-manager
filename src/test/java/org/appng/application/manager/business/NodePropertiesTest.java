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
		WritingXmlValidator.writeXml = true;
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
