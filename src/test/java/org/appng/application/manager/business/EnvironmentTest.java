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

import org.apache.commons.lang3.StringUtils;
import org.appng.api.support.CallableDataSource;
import org.appng.testsupport.validation.WritingXmlValidator;
import org.appng.testsupport.validation.XPathDifferenceHandler;
import org.junit.Test;

public class EnvironmentTest extends AbstractTest {

	static {
		WritingXmlValidator.writeXml = false;
	}

	@Test
	public void testSystemEnv() throws Exception {
		validate("systemEnv");
	}

	@Test
	public void testSystemProps() throws Exception {
		validate("systemProps");
	}

	@Test
	public void testJvmArguments() throws Exception {
		validate("jvmArguments");
	}

	@Test
	public void testProcessor() throws Exception {
		validate("processor");
	}

	@Test
	public void testMemory() throws Exception {
		validate("memory");
	}

	protected void validate(String dataSource) throws Exception {
		CallableDataSource ds = getDataSource(dataSource).getCallableDataSource();
		ds.perform("");
		ds.getDatasource().getData().setResultset(null);
		XPathDifferenceHandler dh = new XPathDifferenceHandler(true);		
		validate(ds.getDatasource(), StringUtils.capitalize(dataSource), dh);
	}
}
