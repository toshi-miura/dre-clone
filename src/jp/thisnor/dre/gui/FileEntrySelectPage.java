package jp.thisnor.dre.gui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

public class FileEntrySelectPage extends DREPage {
	private static final String
		PREFS_TARGET_FILES_KEY = FileEntrySelectPage.class.getName() + ".target.files", //$NON-NLS-1$
		PREFS_SOURCE_FILES_KEY = FileEntrySelectPage.class.getName() + ".source.files", //$NON-NLS-1$
		PREFS_SAME_TARGET_KEY = FileEntrySelectPage.class.getName() + ".sametarget", //$NON-NLS-1$
		PREFS_USE_EXTENSION_FILTER_KEY = FileEntrySelectPage.class.getName() + ".use.extfilter",
		PREFS_EXTENSION_FILTER_KEY = FileEntrySelectPage.class.getName() + ".extfilter"
		;

	private static final String
		DEFAULT_EXTENSION_FILTER = ".*\\.(?i:png|jpe?g|gif)";

	private DREFrame frame;
	private PreferenceStore prefs;
	private Messages messages;

	private SashForm rootComp;
	private FileDropListViewer targetFileListViewer, sourceFileListViewer;
	private Button sameTargetCheck;
	private Button extFilterCheck;
	private Text extFilterText;

	FileEntrySelectPage(DREFrame frame) {
		this.frame = frame;
		this.messages = frame.getMessages();
	}

	void createContents(Composite parent) {
		prefs = frame.getPreferences();
		prefs.setDefault(PREFS_TARGET_FILES_KEY, ""); //$NON-NLS-1$
		prefs.setDefault(PREFS_SOURCE_FILES_KEY, ""); //$NON-NLS-1$
		prefs.setDefault(PREFS_SAME_TARGET_KEY, true);
		prefs.setDefault(PREFS_USE_EXTENSION_FILTER_KEY, false);
		prefs.setDefault(PREFS_EXTENSION_FILTER_KEY, DEFAULT_EXTENSION_FILTER);
		prefs.setDefault(PREFS_TARGET_FILES_KEY + ".save", true); //$NON-NLS-1$
		prefs.setDefault(PREFS_SOURCE_FILES_KEY + ".save", true); //$NON-NLS-1$

		rootComp = new SashForm(parent, SWT.HORIZONTAL);
		rootComp.setSashWidth(8);
		rootComp.setLayout(new FillLayout());

		Group targetComp = new Group(rootComp, SWT.NONE);
		targetComp.setText(messages.getString("FileEntrySelectPage.TARGET_TABLE_CAPTION"));
		{
			FillLayout l = new FillLayout();
			l.marginWidth = l.marginHeight = 8;
			targetComp.setLayout(l);
		}

		targetFileListViewer = new FileDropListViewer(frame, targetComp);

		Composite rightComp = new Composite(rootComp, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			l.spacing = 8;
			rightComp.setLayout(l);
		}

		Group sourceComp = new Group(rightComp, SWT.NONE);
		sourceComp.setText(messages.getString("FileEntrySelectPage.SOURCE_TABLE_CAPTION"));
		{
			FormLayout l = new FormLayout();
			l.marginWidth = l.marginHeight = 8;
			l.spacing = 8;
			sourceComp.setLayout(l);
		}

		sourceFileListViewer = new FileDropListViewer(frame, sourceComp);

		sameTargetCheck = new Button(sourceComp, SWT.CHECK);
		sameTargetCheck.setText(messages.getString("FileEntrySelectPage.TARGET_AS_SOURCE_BUTTON_TEXT"));
		sameTargetCheck.addSelectionListener(UPDATE_CONTENTS_ENABLED);

		sourceFileListViewer.getControl().setLayoutData(new FormDataBuilder().left(0).right(100).top(0).bottom(sameTargetCheck).build());
		sameTargetCheck.setLayoutData(new FormDataBuilder().left(0).right(100).bottom(100).build());

		Composite extFilterComp = new Composite(rightComp, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			l.marginWidth = l.marginHeight = 8;
			l.spacing = 8;
			extFilterComp.setLayout(l);
		}
		extFilterCheck = new Button(extFilterComp, SWT.CHECK);
		extFilterCheck.setText(messages.getString("FileEntrySelectPage.TARGET_EXTENSION_LABEL_TEXT"));
		extFilterCheck.addSelectionListener(UPDATE_CONTENTS_ENABLED);
		extFilterText = new Text(extFilterComp, SWT.BORDER);
		extFilterCheck.setLayoutData(new FormDataBuilder().left(0).top(0).build());
		extFilterText.setLayoutData(new FormDataBuilder().left(extFilterCheck).right(100).top(0).build());

		sourceComp.setLayoutData(new FormDataBuilder().left(0).right(100).top(0).bottom(extFilterComp).build());
		extFilterComp.setLayoutData(new FormDataBuilder().left(0).right(100).bottom(100).build());

		sameTargetCheck.setSelection(prefs.getBoolean(PREFS_SAME_TARGET_KEY));
		extFilterCheck.setSelection(prefs.getBoolean(PREFS_USE_EXTENSION_FILTER_KEY));
		extFilterText.setText(prefs.getString(PREFS_EXTENSION_FILTER_KEY));
		if (!prefs.getString(PREFS_TARGET_FILES_KEY).isEmpty()) {
			for (String path : prefs.getString(PREFS_TARGET_FILES_KEY).split("\\" + File.pathSeparator)) { //$NON-NLS-1$
				targetFileListViewer.addFile(path);
			}
		}
		if (!prefs.getString(PREFS_SOURCE_FILES_KEY).isEmpty()) {
			for (String path : prefs.getString(PREFS_SOURCE_FILES_KEY).split("\\" + File.pathSeparator)) { //$NON-NLS-1$
				sourceFileListViewer.addFile(path);
			}
		}

		updateContentsEnabled();
	}

	void activated() {
		frame.setPageTitle(messages.getString("FileEntrySelectPage.PAGE_TITLE"));
		frame.setPageDescription(messages.getString("FileEntrySelectPage.PAGE_DESCRIPTION"));
		frame.setContent(rootComp);
		frame.setPreviousButtonEnabled(false);
		frame.setNextButtonEnabled(true);
	}

	void hiddened() {
		final String filterRegex = extFilterText.getText();
		FilenameFilter filter = extFilterCheck.getSelection() ?
				new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						boolean match = name.matches(filterRegex);
						return match;
					}
				} :
				new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return true;
					}
				};
		targetFileListViewer.computeFileEntryList(filter, true);
		sourceFileListViewer.computeFileEntryList(filter, true);

		PreferenceStore prefs = frame.getPreferences();
		prefs.setValue(PREFS_SAME_TARGET_KEY, sameTargetCheck.getSelection());
		prefs.setValue(PREFS_USE_EXTENSION_FILTER_KEY, extFilterCheck.getSelection());
		prefs.setValue(PREFS_EXTENSION_FILTER_KEY, extFilterText.getText());
		if (prefs.getBoolean(PREFS_TARGET_FILES_KEY + ".save")) //$NON-NLS-1$
			prefs.setValue(PREFS_TARGET_FILES_KEY, fileEntryListToText(targetFileListViewer.getRawFileEntryList()));
		if (prefs.getBoolean(PREFS_SOURCE_FILES_KEY + ".save")) //$NON-NLS-1$
			prefs.setValue(PREFS_SOURCE_FILES_KEY, fileEntryListToText(sourceFileListViewer.getRawFileEntryList()));
	}

	void nextRequested() {
		frame.setActivePage(frame.getPage(PackageSelectPage.class));
	}

	void previousRequested() {

	}

	@Override
	void dispose() {
		targetFileListViewer.dispose();
		sourceFileListViewer.dispose();
	}

	List<FileEntry> getTargetFileList() {
		return targetFileListViewer.getFileEntryList();
	}

	List<FileEntry> getSourceFileList() {
		return sameTargetCheck.getSelection() ?
				getTargetFileList() :
				sourceFileListViewer.getFileEntryList();
	}

	private void updateContentsEnabled() {
		sourceFileListViewer.setEnabled(!sameTargetCheck.getSelection());
		extFilterText.setEnabled(extFilterCheck.getSelection());
	}

	private String fileEntryListToText(List<String> filePathList) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String filePath : filePathList) {
			if (first) first = false; else sb.append(File.pathSeparatorChar);
			sb.append(filePath);
		}
		return sb.toString();
	}

	private final SelectionListener UPDATE_CONTENTS_ENABLED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			updateContentsEnabled();
		}
	};
}
