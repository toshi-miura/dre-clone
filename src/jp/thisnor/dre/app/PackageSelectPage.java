package jp.thisnor.dre.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.thisnor.dre.core.DREException;
import jp.thisnor.dre.core.MeasureOptionEntry;
import jp.thisnor.dre.core.MeasurerPackage;
import jp.thisnor.dre.core.Messages;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TableItem;

public class PackageSelectPage extends DREPage {
	private static final File PACKAGE_DIR = new File("packages");

	private static final String PREFS_PACKAGE_KEY = "jp.thisnor.dre.core.PackageSelectPage.selectedpackage"; //$NON-NLS-1$
	private static final String PREFS_NUM_THREAD_KEY = "jp.thisnor.dre.core.PackageSelectPage.numthread"; //$NON-NLS-1$

	private MeasurerPackage[] packages;

	private DREFrame frame;
	private PreferenceStore prefs;
	private Messages messages;

	private SashForm rootComp;
	private PackageListViewer packageListViewer;
	private PackageViewer packageViewer;
	private Spinner numThreadsSpinner;

	PackageSelectPage(DREFrame frame) {
		this.frame = frame;
		this.messages = frame.getMessages();
	}

	MeasurerPackage getSelectedPackage() {
		return packageListViewer.getActivePackage();
	}

	int getNumThreads() {
		return numThreadsSpinner.getSelection();
	}

	void createContents(Composite parent) {
		List<MeasurerPackage> packageList = new ArrayList<MeasurerPackage>();
		for (File packageFile : PACKAGE_DIR.listFiles()) {
			try {
				packageList.add(MeasurerPackage.importPackage(packageFile, frame.getMessages().getLocale()));
			} catch (DREException e) {
				e.printStackTrace();
			}
		}
		packages = packageList.toArray(new MeasurerPackage[packageList.size()]);

		prefs = frame.getPreferences();
		prefs.setDefault(PREFS_PACKAGE_KEY, "jp.thisnor.dre.simimg"); //$NON-NLS-1$
		prefs.setDefault(PREFS_NUM_THREAD_KEY, 2);

		rootComp = new SashForm(parent, SWT.HORIZONTAL);
		rootComp.setSashWidth(8);
		rootComp.setLayout(new FillLayout());

		packageListViewer = new PackageListViewer(packages);
		packageListViewer.createContents(rootComp);
		packageListViewer.getTable().addSelectionListener(PACKAGE_LIST_SELECTION_LISTENER);

		Composite rightComp = new Composite(rootComp, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			l.spacing = 8;
			rightComp.setLayout(l);
		}

		Composite packageViewerGroup = new Group(rightComp, SWT.NONE);
		{
			FillLayout l = new FillLayout();
			l.marginWidth = l.marginHeight = 8;
			packageViewerGroup.setLayout(l);
		}
		packageViewer = new PackageViewer(frame);
		packageViewer.createContents(packageViewerGroup);

		Composite systemOptionComp = new Composite(rightComp, SWT.NONE);
		{
			RowLayout l = new RowLayout(SWT.HORIZONTAL);
			l.spacing = 8;
			systemOptionComp.setLayout(l);
		}
		Label numThreadLabel = new Label(systemOptionComp, SWT.NONE);
		numThreadLabel.setText(messages.getString("PackageSelectPage.NUM_THREADS_SPINNER_TEXT"));
		numThreadsSpinner = new Spinner(systemOptionComp, SWT.BORDER);
		numThreadsSpinner.setMinimum(1);

		packageViewerGroup.setLayoutData(new FormDataBuilder().left(0).right(100).top(0).bottom(systemOptionComp).build());
		systemOptionComp.setLayoutData(new FormDataBuilder().left(0).right(100).bottom(100).build());

		for (MeasurerPackage e : packages) {
			String className = e.getKey();
			for (MeasureOptionEntry option : e.getOptionMap().values()) {
				String prefsKey = className + "." + option.getKey(); //$NON-NLS-1$
				if (prefs.contains(prefsKey)) {
					option.setValue(prefs.getString(prefsKey));
				}
			}
		}
		String defaultPackagePath = prefs.getString(PREFS_PACKAGE_KEY);
		for (TableItem item : packageListViewer.getTable().getItems()) {
			MeasurerPackage pack = (MeasurerPackage)item.getData();
			if (pack.getKey().equals(defaultPackagePath)) {
				packageListViewer.getTable().setSelection(item);
				setActivePackage(pack);
				break;
			}
		}

		numThreadsSpinner.setSelection(prefs.getInt(PREFS_NUM_THREAD_KEY));
	}

	void setActivePackage(MeasurerPackage pack) {
		packageViewer.setActivePackage(pack);
		frame.setNextButtonEnabled(pack != null);
	}

	void activated() {
		frame.setPageTitle(messages.getString("PackageSelectPage.PAGE_TITLE"));
		frame.setPageDescription(messages.getString("PackageSelectPage.PAGE_DESCRIPTION"));
		frame.setContent(rootComp);
		frame.setPreviousButtonEnabled(true);
		frame.setNextButtonEnabled(packageListViewer.getActivePackage() != null);
	}

	void hiddened() {
		if (getSelectedPackage() != null) {
			MeasurerPackage selectedPackage = getSelectedPackage();
			prefs.setValue(PREFS_PACKAGE_KEY, selectedPackage.getKey());
		}
		prefs.setValue(PREFS_NUM_THREAD_KEY, getNumThreads());
		for (MeasurerPackage e : packages) {
			String className = e.getKey();
			for (MeasureOptionEntry option : e.getOptionMap().values()) {
				String prefsKey = className + "." + option.getKey(); //$NON-NLS-1$
				prefs.setValue(prefsKey, option.getValue());
			}
		}
	}

	void nextRequested() {
		if (getSelectedPackage() == null) {
			MessageBox msgBox = new MessageBox(rootComp.getShell(), SWT.OK);
			msgBox.setText(messages.getString("PackageSelectPage.REPORT_FAILED_NOSELECTION_TITLE"));
			msgBox.setMessage(messages.getString("PackageSelectPage.REPORT_FAILED_NOSELECTION_MESSAGE"));
			msgBox.open();
			return;
		}
		frame.setActivePage(frame.getPage(MeasureExecutePage.class));
	}

	void previousRequested() {
		frame.setActivePage(frame.getPage(FileEntrySelectPage.class));
	}

	@Override
	void dispose() {

	}

	private final SelectionListener PACKAGE_LIST_SELECTION_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			setActivePackage(packageListViewer.getActivePackage());
		}
	};
}
