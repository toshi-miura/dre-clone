package jp.thisnor.dre.app;

import org.eclipse.swt.widgets.Composite;

abstract class DREPage {
	abstract void createContents(Composite parent);
	abstract void activated();
	abstract void hiddened();
	abstract void nextRequested();
	abstract void previousRequested();
	abstract void dispose();
}
