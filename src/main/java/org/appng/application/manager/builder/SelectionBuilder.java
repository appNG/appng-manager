package org.appng.application.manager.builder;

import java.util.Collection;

import org.appng.api.model.NameProvider;
import org.appng.api.support.OptionOwner;
import org.appng.api.support.OptionOwner.Selector;
import org.appng.xml.platform.Label;
import org.appng.xml.platform.Option;
import org.appng.xml.platform.OptionGroup;
import org.appng.xml.platform.SelectionType;

public class SelectionBuilder<T> extends OptionsBuilder<T, SelectionBuilder.Selection> {

	private Selection selection;

	public static class Selection extends org.appng.xml.platform.Selection implements OptionOwner {
		public void addOption(Option option) {
			getOptions().add(option);
		}

		public Option addOption(String name, String value, boolean selected) {
			Option o = new Option();
			getOptions().add(o);
			o.setName(name);
			o.setValue(value);
			o.setSelected(selected);
			return o;
		}
	};

	public SelectionBuilder(String id) {
		super();
		this.selection = new Selection();
		selection.setId(id);
		setOwner(selection);
	}

	public SelectionBuilder<T> label(String labelId) {
		Label label = new Label();
		label.setValue(labelId);
		selection.setTitle(label);
		return this;
	}

	public SelectionBuilder<T> type(SelectionType type) {
		selection.setType(type);
		return this;
	}

	public SelectionBuilder<T> addGroup(OptionGroup group) {
		this.selection.getOptionGroups().add(group);
		return this;
	}

	public SelectionBuilder<T> options(Iterable<T> values) {
		super.options(values);
		return this;
	}

	public SelectionBuilder<T> name(NameProvider<T> nameProvider) {
		super.name(nameProvider);
		return this;
	}

	public SelectionBuilder<T> selector(Selector selector) {
		super.selector(selector);
		return this;
	}

	public SelectionBuilder<T> select(Collection<T> selected) {
		super.select(selected);
		return this;
	}

	public SelectionBuilder<T> select(T selected) {
		super.select(selected);
		return this;
	}

	public SelectionBuilder<T> disable(Collection<T> selected) {
		super.disable(selected);
		return this;
	}

	public SelectionBuilder<T> disable(T selected) {
		super.disable(selected);
		return this;
	}

	public SelectionBuilder<T> defaultOption(String name, String value) {
		super.defaultOption(name, value);
		return this;
	}

	public Selection build() {
		super.build();
		return selection;
	}

}
