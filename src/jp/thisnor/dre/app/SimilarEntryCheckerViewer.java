package jp.thisnor.dre.app;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.thisnor.dre.core.FileEntry;
import jp.thisnor.dre.core.SimilarEntry;
import jp.thisnor.dre.core.SimilarGroup;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

class SimilarEntryCheckerViewer {
	private String[]
		CONDITION_KEY = {
			"ALL", "TARGET", "SOURCE", "RANDOM_ONE", "FILE_NAME", "COMPRESS", "PIXEL_SIZE", "FILE_SIZE", "LAST_MODIFIED", "DISTANCE", "DIRECTORY"
		},
		MAX_MIN_KEY = {
			"MAX", "MIN"
		},
		GEQ_LEQ_KEY = {
			"GEQ", "LEQ"
		},
		CHECK_STATE_KEY = {
			"ON", "OFF", "TOGGLE"
		};

	private static final String
		PREFS_COND_KEY = SimilarEntryCheckerViewer.class.getName() + ".cond", //$NON-NLS-1$
		PREFS_FILENAME_KEY = SimilarEntryCheckerViewer.class.getName() + ".filename",
		PREFS_COMPRESS_KEY = SimilarEntryCheckerViewer.class.getName() + ".compress", //$NON-NLS-1$
		PREFS_SIZE_KEY = SimilarEntryCheckerViewer.class.getName() + ".size", //$NON-NLS-1$
		PREFS_FILESIZE_KEY = SimilarEntryCheckerViewer.class.getName() + ".filesize", //$NON-NLS-1$
		PREFS_LASTMODIFIED_KEY = SimilarEntryCheckerViewer.class.getName() + ".lastmodified", //$NON-NLS-1$
		PREFS_DISTANCE_VALUE_KEY = SimilarEntryCheckerViewer.class.getName() + ".distvalue", //$NON-NLS-1$
		PREFS_DISTANCE_DIR_KEY = SimilarEntryCheckerViewer.class.getName() + ".distdir", //$NON-NLS-1$
		PREFS_PATH_KEY = SimilarEntryCheckerViewer.class.getName() + ".path", //$NON-NLS-1$
		PREFS_INVERSE_KEY = SimilarEntryCheckerViewer.class.getName() + ".inverse", //$NON-NLS-1$
		PREFS_CHECK_KEY = SimilarEntryCheckerViewer.class.getName() + ".check"; //$NON-NLS-1$

	private DREFrame frame;
	private Messages messages;

	private Composite rootComp;
	private ScrolledComposite condScComp;
	private Composite condComp;
	private Button[] condRadios;
	private Button[] filenameRadios;
	private Button[] compressRadios;
	private Button[] sizeRadios;
	private Button[] fileSizeRadios;
	private Button[] lastModifiedRadios;
	private Spinner distanceSpinner;
	private Button[] distanceRadios;
	private PathInputViewer pathInput;
	private Button inverseCheck;
	private Button[] checkRadios;
	private Button invokeButton;

	SimilarEntryCheckerViewer(DREFrame frame) {
		this.frame = frame;
		this.messages = frame.getMessages();
	}

	void createContents(Composite parent) {
		PreferenceStore prefs = frame.getPreferences();
		prefs.setDefault(PREFS_COND_KEY, 0);
		prefs.setDefault(PREFS_FILENAME_KEY, 0);
		prefs.setDefault(PREFS_COMPRESS_KEY, 0);
		prefs.setDefault(PREFS_SIZE_KEY, 0);
		prefs.setDefault(PREFS_FILESIZE_KEY, 0);
		prefs.setDefault(PREFS_LASTMODIFIED_KEY, 0);
		prefs.setDefault(PREFS_DISTANCE_VALUE_KEY, 100);
		prefs.setDefault(PREFS_DISTANCE_DIR_KEY, 0);
		prefs.setDefault(PREFS_PATH_KEY, ""); //$NON-NLS-1$
		prefs.setDefault(PREFS_INVERSE_KEY, false);
		prefs.setDefault(PREFS_CHECK_KEY, 0);

		rootComp = new Composite(parent, SWT.NONE);
		rootComp.setLayout(new FormLayout());

		Group condBorderGroup = new Group(rootComp, SWT.NONE);
		condBorderGroup.setLayout(new FillLayout());

		condScComp = new ScrolledComposite(condBorderGroup, SWT.V_SCROLL | SWT.H_SCROLL);
		condScComp.setMinHeight(10000);
		condScComp.setExpandHorizontal(true);
		condScComp.setExpandVertical(true);
//		condScComp.setAlwaysShowScrollBars(true);
		condScComp.addControlListener(COND_SCCOMP_RESIZED);

		condComp = new Composite(condScComp, SWT.NONE);
		condScComp.setContent(condComp);
		{
			GridLayout l = new GridLayout();
			l.numColumns = 2;
			l.marginWidth = l.marginHeight = 8;
			l.horizontalSpacing = 8;
			condComp.setLayout(l);
		}

		condRadios = new Button[CONDITION_KEY.length];
		for (int i = 0; i < CONDITION_KEY.length; i++) {
			condRadios[i] = new Button(condComp, SWT.RADIO);
			condRadios[i].setText(messages.getString("SimilarEntryCheckerViewer.CONDITION." + CONDITION_KEY[i]));
			condRadios[i].addSelectionListener(UPDATE_CONTENTS_ENABLED);
			GridData condRadioGridData = new GridData();
			condRadios[i].setLayoutData(condRadioGridData);

			if (i == 10) new Label(condComp, SWT.NONE);

			Composite condItemComp = new Composite(condComp, SWT.NONE);
			condItemComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			switch (i) {
			case 4:
				Group filenameGroup = new Group(condItemComp, SWT.NONE);
				filenameGroup.setLayout(createSpacedHorizontalLayout());
				filenameRadios = new Button[MAX_MIN_KEY.length];
				for (int j = 0; j < MAX_MIN_KEY.length; j++) {
					filenameRadios[j] = new Button(filenameGroup, SWT.RADIO);
					filenameRadios[j].setText(messages.getString("SimilarEntryCheckerViewer.FILE_NAME." + MAX_MIN_KEY[j]));
				}
				break;
			case 5:
				Group compressGroup = new Group(condItemComp, SWT.NONE);
				compressGroup.setLayout(createSpacedHorizontalLayout());
				compressRadios = new Button[MAX_MIN_KEY.length];
				for (int j = 0; j < MAX_MIN_KEY.length; j++) {
					compressRadios[j] = new Button(compressGroup, SWT.RADIO);
					compressRadios[j].setText(messages.getString("SimilarEntryCheckerViewer.COMPRESS." + MAX_MIN_KEY[j]));
				}
				break;
			case 6:
				Group sizeGroup = new Group(condItemComp, SWT.NONE);
				sizeGroup.setLayout(createSpacedHorizontalLayout());
				sizeRadios = new Button[MAX_MIN_KEY.length];
				for (int j = 0; j < MAX_MIN_KEY.length; j++) {
					sizeRadios[j] = new Button(sizeGroup, SWT.RADIO);
					sizeRadios[j].setText(messages.getString("SimilarEntryCheckerViewer.PIXEL_SIZE." + MAX_MIN_KEY[j]));
				}
				break;
			case 7:
				Group fileSizeGroup = new Group(condItemComp, SWT.NONE);
				fileSizeGroup.setLayout(createSpacedHorizontalLayout());
				fileSizeRadios = new Button[MAX_MIN_KEY.length];
				for (int j = 0; j < MAX_MIN_KEY.length; j++) {
					fileSizeRadios[j] = new Button(fileSizeGroup, SWT.RADIO);
					fileSizeRadios[j].setText(messages.getString("SimilarEntryCheckerViewer.FILE_SIZE." + MAX_MIN_KEY[j]));
				}
				break;
			case 8:
				Group lastModifiedGroup = new Group(condItemComp, SWT.NONE);
				lastModifiedGroup.setLayout(createSpacedHorizontalLayout());
				lastModifiedRadios = new Button[MAX_MIN_KEY.length];
				for (int j = 0; j < MAX_MIN_KEY.length; j++) {
					lastModifiedRadios[j] = new Button(lastModifiedGroup, SWT.RADIO);
					lastModifiedRadios[j].setText(messages.getString("SimilarEntryCheckerViewer.LAST_MODIFIED." + MAX_MIN_KEY[j]));
				}
				break;
			case 9:
				distanceSpinner = new Spinner(condItemComp, SWT.BORDER);
				distanceSpinner.setMinimum(0);
				distanceSpinner.setMaximum(Integer.MAX_VALUE);
				Group distanceGroup = new Group(condItemComp, SWT.NONE);
				distanceGroup.setLayout(createSpacedHorizontalLayout());
				distanceRadios = new Button[GEQ_LEQ_KEY.length];
				for (int j = 0; j < GEQ_LEQ_KEY.length; j++) {
					distanceRadios[j] = new Button(distanceGroup, SWT.RADIO);
					distanceRadios[j].setText(messages.getString("SimilarEntryCheckerViewer.DISTANCE." + GEQ_LEQ_KEY[j]));
				}
				break;
			case 10:
				condRadioGridData.horizontalSpan = 2;
				pathInput = new PathInputViewer(frame);
				pathInput.createContents(condItemComp);
				pathInput.getControl().setLayoutData(new FormDataBuilder().left(0).right(100).build());
				break;
			}
			if (i == 10) {
				FormLayout l = new FormLayout();
				condItemComp.setLayout(l);
			} else {
				RowLayout l = new RowLayout(SWT.HORIZONTAL);
				l.spacing = 8;
				l.center = true;
				condItemComp.setLayout(l);
			}
		}

		Group inverseGroup = new Group(rootComp, SWT.NONE);
		inverseGroup.setLayout(createSpacedHorizontalLayout());
		inverseCheck = new Button(inverseGroup, SWT.CHECK);
		inverseCheck.setText(messages.getString("SimilarEntryCheckerViewer.INVERSE"));

		Group checkGroup = new Group(rootComp, SWT.NONE);
		checkGroup.setLayout(createSpacedHorizontalLayout());
		checkRadios = new Button[CHECK_STATE_KEY.length];
		for (int i = 0; i < CHECK_STATE_KEY.length; i++) {
			checkRadios[i] = new Button(checkGroup, SWT.RADIO);
			checkRadios[i].setText(messages.getString("SimilarEntryCheckerViewer.CHECK." + CHECK_STATE_KEY[i]));
		}

		Composite invokeComp = new Composite(rootComp, SWT.NONE);
		invokeComp.setLayout(new FormLayout());

		invokeButton = new Button(invokeComp, SWT.PUSH);
		invokeButton.setText(messages.getString("SimilarEntryCheckerViewer.INVOKE"));
		invokeButton.addSelectionListener(CHECKER_INVOKE_BUTTON_SELECTED);

		invokeButton.setLayoutData(new FormDataBuilder().left(100, -80).right(100).bottom(100).build());

		condBorderGroup.setLayoutData(new FormDataBuilder().left(0).right(100).top(0).bottom(inverseGroup).build());
		inverseGroup.setLayoutData(new FormDataBuilder().left(0).right(100).bottom(checkGroup).build());
		checkGroup.setLayoutData(new FormDataBuilder().left(0).right(100).bottom(invokeComp).build());
		invokeComp.setLayoutData(new FormDataBuilder().left(0).right(100).bottom(100).build());

		select(condRadios, prefs.getInt(PREFS_COND_KEY));
		select(filenameRadios, prefs.getInt(PREFS_FILENAME_KEY));
		select(compressRadios, prefs.getInt(PREFS_COMPRESS_KEY));
		select(sizeRadios, prefs.getInt(PREFS_SIZE_KEY));
		select(fileSizeRadios, prefs.getInt(PREFS_FILESIZE_KEY));
		select(lastModifiedRadios, prefs.getInt(PREFS_LASTMODIFIED_KEY));
		distanceSpinner.setSelection(prefs.getInt(PREFS_DISTANCE_VALUE_KEY));
		select(distanceRadios, prefs.getInt(PREFS_DISTANCE_DIR_KEY));
		pathInput.setPath(prefs.getString(PREFS_PATH_KEY));
		inverseCheck.setSelection(prefs.getBoolean(PREFS_INVERSE_KEY));
		select(checkRadios, prefs.getInt(PREFS_CHECK_KEY));
		updateContentsEnabled();
	}

	void hiddened() {
		PreferenceStore prefs = frame.getPreferences();
		prefs.setValue(PREFS_COND_KEY, getSelectedCondition());
		prefs.setValue(PREFS_FILENAME_KEY, getSelectedFilename());
		prefs.setValue(PREFS_COMPRESS_KEY, getSelectedCompress());
		prefs.setValue(PREFS_SIZE_KEY, getSelectedSize());
		prefs.setValue(PREFS_FILESIZE_KEY, getSelectedFileSize());
		prefs.setValue(PREFS_LASTMODIFIED_KEY, getSelectedLastModified());
		prefs.setValue(PREFS_DISTANCE_VALUE_KEY, distanceSpinner.getSelection());
		prefs.setValue(PREFS_DISTANCE_DIR_KEY, getSelectedDistanceDir());
		prefs.setValue(PREFS_PATH_KEY, pathInput.getPath());
		prefs.setValue(PREFS_INVERSE_KEY, inverseCheck.getSelection());
		prefs.setValue(PREFS_CHECK_KEY, getSelectedCheck());
	}

	void updateContentsEnabled() {
		for (Button b : filenameRadios) b.setEnabled(condRadios[4].getSelection());
		for (Button b : compressRadios) b.setEnabled(condRadios[5].getSelection());
		for (Button b : sizeRadios) b.setEnabled(condRadios[6].getSelection());
		for (Button b : fileSizeRadios) b.setEnabled(condRadios[7].getSelection());
		for (Button b : lastModifiedRadios) b.setEnabled(condRadios[8].getSelection());
		distanceSpinner.setEnabled(condRadios[9].getSelection());
		for (Button b : distanceRadios) b.setEnabled(condRadios[9].getSelection());
		pathInput.setEnabled(condRadios[10].getSelection());
	}

	private void select(Button[] radios, int index) {
		if (index < 0) return;
		radios[index].setSelection(true);
	}
	private int getSelectedFilename() {
		for (int i = 0; i < filenameRadios.length; i++) {
			if (filenameRadios[i].getSelection()) return i;
		}
		return -1;
	}
	private int getSelectedCondition() {
		for (int i = 0; i < condRadios.length; i++) {
			if (condRadios[i].getSelection()) return i;
		}
		return -1;
	}
	private int getSelectedCompress() {
		for (int i = 0; i < compressRadios.length; i++) {
			if (compressRadios[i].getSelection()) return i;
		}
		return -1;
	}
	private int getSelectedSize() {
		for (int i = 0; i < sizeRadios.length; i++) {
			if (sizeRadios[i].getSelection()) return i;
		}
		return -1;
	}
	private int getSelectedFileSize() {
		for (int i = 0; i < fileSizeRadios.length; i++) {
			if (fileSizeRadios[i].getSelection()) return i;
		}
		return -1;
	}
	private int getSelectedLastModified() {
		for (int i = 0; i < lastModifiedRadios.length; i++) {
			if (lastModifiedRadios[i].getSelection()) return i;
		}
		return -1;
	}
	private int getSelectedDistanceDir() {
		for (int i = 0; i < distanceRadios.length; i++) {
			if (distanceRadios[i].getSelection()) return i;
		}
		return -1;
	}
	private int getSelectedCheck() {
		for (int i = 0; i < checkRadios.length; i++) {
			if (checkRadios[i].getSelection()) return i;
		}
		return -1;
	}

	private RowLayout createSpacedHorizontalLayout() {
		RowLayout l = new RowLayout(SWT.HORIZONTAL);
		l.spacing = 8;
		l.center = true;
		return l;
	}

	private final ControlListener COND_SCCOMP_RESIZED = new ControlAdapter() {
		@Override
		public void controlResized(ControlEvent event) {
			Rectangle clientRect = condScComp.getClientArea();
			condScComp.setMinSize(condComp.computeSize(clientRect.width, SWT.DEFAULT));
		}
	};

	private final SelectionListener UPDATE_CONTENTS_ENABLED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			updateContentsEnabled();
		}
	};

	private final SelectionListener CHECKER_INVOKE_BUTTON_SELECTED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			final List<SimilarGroup> simGroupList = frame.getPage(MeasureExecutePage.class).getSimilarGroupList();

			EntrySelecter selecter0 = null;
			switch (getSelectedCondition()) {
			case 0: selecter0 = AllEntrySelecter.INSTANCE; break;
			case 1: selecter0 = TargetEntrySelecter.INSTANCE; break;
			case 2: selecter0 = StorageEntrySelecter.INSTANCE; break;
			case 3: selecter0 = PickOneEntrySelecter.INSTANCE; break;
			case 4: selecter0 = (getSelectedFilename() == 0) ? FilenameEntrySelecter.LAST_SELECTER : FilenameEntrySelecter.EARLIEST_SELECTER; break;
			case 5: selecter0 = (getSelectedCompress() == 0) ? CompressEntrySelecter.MAX_SELECTER : CompressEntrySelecter.MIN_SELECTER; break;
			case 6: selecter0 = (getSelectedSize() == 0) ? SizeEntrySelecter.MAX_SELECTER : SizeEntrySelecter.MIN_SELECTER; break;
			case 7: selecter0 = (getSelectedFileSize() == 0) ? FileSizeEntrySelecter.MAX_SELECTER : FileSizeEntrySelecter.MIN_SELECTER; break;
			case 8: selecter0 = (getSelectedLastModified() == 0) ? LastModifiedEntrySelecter.LAST_SELECTER : LastModifiedEntrySelecter.EARLIEST_SELECTER; break;
			case 9: selecter0 = (getSelectedDistanceDir() == 0) ? new LargerDistanceEntrySelecter(distanceSpinner.getSelection()) : new SmallerDistanceEntrySelecter(distanceSpinner.getSelection()); break;
			case 10: selecter0 = new PathFilterEntrySelecter(pathInput.getPath()); break;
			}
			final EntrySelecter selecter = selecter0;

			final boolean inverse = inverseCheck.getSelection();

			ExecutorService executor = Executors.newSingleThreadExecutor();
			invokeButton.setEnabled(false);
			executor.execute(new Runnable() {
				public void run() {
					Set<FileEntry> selectedSet0 = selecter.select(simGroupList);
					if (inverse) {
						Set<FileEntry> wholeSet = new HashSet<FileEntry>();
						for (SimilarGroup simGroup : simGroupList) {
							wholeSet.add(simGroup.getFileEntry());
							for (SimilarEntry similar : simGroup.getSimilarList()) {
								wholeSet.add(similar.getFileEntry());
							}
						}
						wholeSet.removeAll(selectedSet0);
						selectedSet0 = wholeSet;
					}
					final Set<FileEntry> selectedSet = selectedSet0;

					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							if (frame.getShell().isDisposed()) return;
							SimilarEntrySelectPage selectPage = frame.getPage(SimilarEntrySelectPage.class);
							switch (getSelectedCheck()) {
							case 0:
								selectPage.setFileChecked(selectedSet, 1);
								break;
							case 1:
								selectPage.setFileChecked(selectedSet, 0);
								break;
							case 2:
								selectPage.setFileChecked(selectedSet, -1);
								break;
							}
							invokeButton.setEnabled(true);
						}
					});
				}
			});
			executor.shutdown();
		}
	};
}
