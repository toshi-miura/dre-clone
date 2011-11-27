package jp.thisnor.dre.gui;

import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

class FormDataBuilder {
	private final FormData fd;

	FormDataBuilder() {
		fd = new FormData();
	}

	FormData build() {
		return fd;
	}

	FormDataBuilder left(Control control) {
		fd.left = new FormAttachment(control);
		return this;
	}

	FormDataBuilder left(Control control, int offset) {
		fd.left = new FormAttachment(control, offset);
		return this;
	}

	FormDataBuilder left(int numerator) {
		fd.left = new FormAttachment(numerator);
		return this;
	}

	FormDataBuilder left(int numerator, int offset) {
		fd.left = new FormAttachment(numerator, offset);
		return this;
	}

	FormDataBuilder right(Control control) {
		fd.right = new FormAttachment(control);
		return this;
	}

	FormDataBuilder right(Control control, int offset) {
		fd.right = new FormAttachment(control, offset);
		return this;
	}

	FormDataBuilder right(int numerator) {
		fd.right = new FormAttachment(numerator);
		return this;
	}

	FormDataBuilder right(int numerator, int offset) {
		fd.right = new FormAttachment(numerator, offset);
		return this;
	}

	FormDataBuilder top(Control control) {
		fd.top = new FormAttachment(control);
		return this;
	}

	FormDataBuilder top(Control control, int offset) {
		fd.top = new FormAttachment(control, offset);
		return this;
	}

	FormDataBuilder top(int numerator) {
		fd.top = new FormAttachment(numerator);
		return this;
	}

	FormDataBuilder top(int numerator, int offset) {
		fd.top = new FormAttachment(numerator, offset);
		return this;
	}

	FormDataBuilder bottom(Control control) {
		fd.bottom = new FormAttachment(control);
		return this;
	}

	FormDataBuilder bottom(Control control, int offset) {
		fd.bottom = new FormAttachment(control, offset);
		return this;
	}

	FormDataBuilder bottom(int numerator) {
		fd.bottom = new FormAttachment(numerator);
		return this;
	}

	FormDataBuilder bottom(int numerator, int offset) {
		fd.bottom = new FormAttachment(numerator, offset);
		return this;
	}

	FormDataBuilder width(int width) {
		fd.width = width;
		return this;
	}

	FormDataBuilder height(int height) {
		fd.height = height;
		return this;
	}
}
