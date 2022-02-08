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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.appng.api.ActionProvider;
import org.appng.api.AttachmentWebservice;
import org.appng.api.DataContainer;
import org.appng.api.DataProvider;
import org.appng.api.Environment;
import org.appng.api.FieldProcessor;
import org.appng.api.GlobalTaglet;
import org.appng.api.Options;
import org.appng.api.Request;
import org.appng.api.RequestUtil;
import org.appng.api.Taglet;
import org.appng.api.Webservice;
import org.appng.api.XMLTaglet;
import org.appng.api.model.Application;
import org.appng.api.model.Site;
import org.springframework.stereotype.Component;

@Component
public class Beans implements DataProvider {

	@Override
	public DataContainer getData(Site site, Application app, Environment env, Options opts, Request req,
			FieldProcessor fp) {
		String siteName = opts.getOptionValue("application", "siteName");
		String applicationName = opts.getOptionValue("application", "appName");
		Site selectedSite = RequestUtil.getSiteByName(env, siteName);
		Set<BeanInfo> beanInfos = new TreeSet<>();
		if (null != selectedSite) {
			Application application = selectedSite.getApplication(applicationName);
			if (null != application) {
				addBeans(application, ActionProvider.class, beanInfos);
				addBeans(application, DataProvider.class, beanInfos);
				addBeans(application, Webservice.class, beanInfos);
				addBeans(application, AttachmentWebservice.class, beanInfos);
				addBeans(application, Taglet.class, beanInfos);
				addBeans(application, GlobalTaglet.class, beanInfos);
				addBeans(application, XMLTaglet.class, beanInfos);
			}
		}
		DataContainer dataContainer = new DataContainer(fp);
		dataContainer.setItems(beanInfos);
		return dataContainer;
	}

	private void addBeans(Application application, Class<?> type, Collection<BeanInfo> beanInfos) {
		String[] names = application.getBeanNames(type);
		for (String name : names) {
			Object bean = application.getBean(name, type);
			if (null != bean) {
				beanInfos.add(new BeanInfo(name, bean.getClass()));
			}
		}
	}

	public class BeanInfo implements Comparable<BeanInfo> {

		private String name;
		private Class<?> type;

		public BeanInfo(String name, Class<?> type) {
			super();
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Class<?> getType() {
			return type;
		}

		public void setType(Class<?> type) {
			this.type = type;
		}

		public String getInterfaces() {
			StringBuilder sb = new StringBuilder();
			int i = 0;
			Class<?>[] interfaces = type.getInterfaces();
			if (interfaces.length == 0) {
				Class<?> superclass = type.getSuperclass();
				interfaces = superclass.getInterfaces();
			}
			for (Class<?> c : interfaces) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(c.getSimpleName());
				i++;
			}
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BeanInfo other = (BeanInfo) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

		private Beans getOuterType() {
			return Beans.this;
		}

		public int compareTo(BeanInfo o) {
			return getName().compareTo(o.getName());
		}

	}
}
