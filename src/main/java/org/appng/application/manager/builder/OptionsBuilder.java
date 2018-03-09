package org.appng.application.manager.builder;

import java.util.Arrays;
import java.util.Collection;

import org.appng.api.model.Identifiable;
import org.appng.api.model.NameProvider;
import org.appng.api.model.Nameable;
import org.appng.api.support.OptionOwner;
import org.appng.api.support.OptionOwner.Selector;
import org.appng.xml.platform.Option;

public class OptionsBuilder<T, R extends OptionOwner> {

	private R owner;
	private Iterable<T> values;
	private NameProvider<T> nameProvider;
	private Selector selector;
	private Collection<T> selected;
	private Collection<T> disabled;
	private Option defaultOption;

	public OptionsBuilder(R owner) {
		this.owner = owner;
	}
	
	public OptionsBuilder() {
	}

	protected void setOwner(R owner) {
		this.owner = owner;
	}

	public OptionsBuilder<T,R> options(Iterable<T> values) {
		this.values = values;
		return this;
	}

	public OptionsBuilder<T,R> name(NameProvider<T> nameProvider) {
		this.nameProvider = nameProvider;
		return this;
	}

	public OptionsBuilder<T,R> selector(Selector selector) {
		this.selector = selector;
		return this;
	}

	public OptionsBuilder<T,R> select(Collection<T> selected) {
		this.selected = selected;
		return this;
	}

	public OptionsBuilder<T,R> select(T selected) {
		this.selected = Arrays.asList(selected);
		return this;
	}

	public OptionsBuilder<T,R> disable(Collection<T> selected) {
		this.disabled = selected;
		return this;
	}

	public OptionsBuilder<T,R> disable(T selected) {
		this.disabled = Arrays.asList(selected);
		return this;
	}

	public OptionsBuilder<T,R> defaultOption(String name, String value) {
		this.defaultOption = new Option();
		defaultOption.setName(name);
		defaultOption.setValue(value);
		return this;
	}


	public R build() {
		if (null != defaultOption) {
			owner.getOptions().add(defaultOption);
		}
		if (null != values) {
			for (T t : values) {
				String name = t.toString();
				if (null != nameProvider) {
					name = nameProvider.getName(t);
				} else if (t instanceof Nameable) {
					name = Nameable.class.cast(t).getName();
				}

				String value = name;
				if (t instanceof Identifiable<?>) {
					value = Identifiable.class.cast(t).getId().toString();
				}


				boolean isSelected = null != selected && selected.contains(t);
				Option option = new Option();
				option.setName(name);
				option.setValue(value);
				option.setSelected(isSelected);
				if (null != selector) {
					selector.select(option);
				}
				if (null != disabled && disabled.contains(t)) {
					option.setDisabled(true);
				}

				owner.getOptions().add(option);
			}
		}
		return owner;
	}

}
