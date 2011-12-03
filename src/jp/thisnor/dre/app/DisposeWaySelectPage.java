package jp.thisnor.dre.app;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;

class DisposeWaySelectPage extends DREPage {
	private static final String
		PREFS_DISPOSE_KEY = DisposeWaySelectPage.class.getName() + ".dispose", //$NON-NLS-1$
		PREFS_DIRMOVETO_KEY = DisposeWaySelectPage.class.getName() + ".dirmoveto"; //$NON-NLS-1$

	private DREFrame frame;
	private Messages messages;

	private Composite rootComp;
	private Button[] disposeRadios;
	private PathInputViewer pathInput;

	DisposeWaySelectPage(DREFrame frame) {
		this.frame = frame;
		this.messages = frame.getMessages();
	}

	@Override
	void createContents(Composite parent) {
		PreferenceStore prefs = frame.getPreferences();
		prefs.setDefault(PREFS_DISPOSE_KEY, 0);
		prefs.setDefault(PREFS_DIRMOVETO_KEY, ""); //$NON-NLS-1$

		rootComp = new Composite(parent, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			l.spacing = 8;
			rootComp.setLayout(l);
		}

		Group disposeGroup = new Group(rootComp, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			l.spacing = 8;
			disposeGroup.setLayout(l);
		}

		disposeRadios = new Button[2];

		disposeRadios[0] = new Button(disposeGroup, SWT.RADIO);
		disposeRadios[0].setText(messages.getString("DisposeWaySelectPage.DISPOSE_MENUES.DELETE"));
		disposeRadios[0].addSelectionListener(UPDATE_CONTENTS_ENABLED);

		disposeRadios[1] = new Button(disposeGroup, SWT.RADIO);
		disposeRadios[1].setText(messages.getString("DisposeWaySelectPage.DISPOSE_MENUES.MOVE"));
		disposeRadios[1].addSelectionListener(UPDATE_CONTENTS_ENABLED);

		pathInput = new PathInputViewer(frame);
		pathInput.createContents(disposeGroup);

		disposeRadios[0].setLayoutData(new FormDataBuilder().left(0).right(100).top(0).build());
		disposeRadios[1].setLayoutData(new FormDataBuilder().left(0).right(100).top(disposeRadios[0]).build());
		pathInput.getControl().setLayoutData(new FormDataBuilder().left(0, 40).right(100).top(disposeRadios[1]).build());

		disposeGroup.setLayoutData(new FormDataBuilder().left(0).right(100).top(0).bottom(100).build());

		int selectIndex = prefs.getInt(PREFS_DISPOSE_KEY);
		if (selectIndex >= 0) disposeRadios[selectIndex].setSelection(true);
		pathInput.setPath(prefs.getString(PREFS_DIRMOVETO_KEY));
		updateContentsEnabled();
	}

	@Override
	void activated() {
		frame.setPageTitle(messages.getString("DisposeWaySelectPage.PAGE_TITLE"));
		frame.setPageDescription(messages.getString("DisposeWaySelectPage.PAGE_DESCRIPTION"));
		frame.setContent(rootComp);
		frame.setNextButtonEnabled(true);
		frame.setPreviousButtonEnabled(true);
	}

	@Override
	void hiddened() {
		PreferenceStore prefs = frame.getPreferences();
		prefs.setValue(PREFS_DISPOSE_KEY, getSelectedDisposeWay());
		prefs.setValue(PREFS_DIRMOVETO_KEY, pathInput.getPath());
	}

	@Override
	void nextRequested() {
		if (getSelectedDisposeWay() == 0) {
			MessageBox msgBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.YES | SWT.NO);
			msgBox.setText(messages.getString("DisposeWaySelectPage.CONFIRM_DELETE_TITLE"));
			msgBox.setMessage(messages.getString("DisposeWaySelectPage.CONFIRM_DELETE_MESSAGE"));
			if (msgBox.open() == SWT.NO) return;
		}
		frame.setActivePage(frame.getPage(DisposeExecutePage.class));
	}

	@Override
	void previousRequested() {
		frame.setActivePage(frame.getPage(SimilarEntrySelectPage.class));
	}

	@Override
	void dispose() {

	}

	int getSelectedDisposeWay() {
		for (int i = 0; i < disposeRadios.length; i++) {
			if (disposeRadios[i].getSelection()) return i;
		}
		return -1;
	}

	String getDirectoryPathMoveTo() {
		return pathInput.getPath();
	}

	private void updateContentsEnabled() {
		pathInput.setEnabled(disposeRadios[1].getSelection());
		frame.setNextButtonEnabled(getSelectedDisposeWay() >= 0);
	}

	private final SelectionListener UPDATE_CONTENTS_ENABLED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			updateContentsEnabled();
		}
	};
}
