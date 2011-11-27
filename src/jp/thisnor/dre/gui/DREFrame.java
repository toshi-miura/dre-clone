package jp.thisnor.dre.gui;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

class DREFrame {
	private static final String
		SOFTWARE_TITLE = "DeadRingerEraser",
		SOFTWARE_VERSION = "0.3.0";

	private static final String PREFERENCES_PATH = "prefs"; //$NON-NLS-1$

	private static final String
		PREFS_LANGUAGE_KEY = DREFrame.class.getName() + ".lang",
		PREFS_SHELL_WIDTH_KEY = DREFrame.class.getName() + ".width",
		PREFS_SHELL_HEIGHT_KEY = DREFrame.class.getName() + ".height";

	private PreferenceStore prefs;
	private Messages messages;

	private Map<Class<? extends DREPage>, DREPage> pageMap;

	private DREPage currentPage;

	private Shell shell;
	private Composite titleBar;
	private Label pageTitleLabel;
	private Label pageDescLabel;
	private Composite contentComp;
	private StackLayout contentStack;
	private Button prefsButton, prevButton, nextButton, closeButton;

	PreferenceStore getPreferences() {
		return prefs;
	}

	Messages getMessages() {
		return messages;
	}

	<T extends DREPage> T getPage(Class<T> cls) {
		return cls.cast(pageMap.get(cls));
	}

	void setActivePage(DREPage page) {
		if (currentPage != null) {
			currentPage.hiddened();
		}
		currentPage = page;
		if (page != null) {
			page.activated();
		}
	}

	void setPageTitle(String title) {
		pageTitleLabel.setText(title);
		titleBar.layout();
	}

	void setPageDescription(String desc) {
		pageDescLabel.setText(desc);
		titleBar.layout();
	}

	void setContent(Control cont) {
		contentStack.topControl = cont;
		contentComp.layout();
		contentComp.redraw();
	}

	void setPreviousButtonEnabled(boolean enabled) {
		prevButton.setEnabled(enabled);
	}

	void setNextButtonEnabled(boolean enabled) {
		nextButton.setEnabled(enabled);
	}

	void setCancelButtonEnabled(boolean enabled) {
		closeButton.setEnabled(enabled);
	}

	void open() {
		prefs = new PreferenceStore(PREFERENCES_PATH);
		try {
			prefs.load();
		} catch (IOException e) {
		}

		prefs.setDefault(PREFS_LANGUAGE_KEY, Locale.getDefault().getLanguage());
		prefs.setDefault(PREFS_SHELL_WIDTH_KEY, 840);
		prefs.setDefault(PREFS_SHELL_HEIGHT_KEY, 540);

		messages = new Messages(new Locale(prefs.getString(PREFS_LANGUAGE_KEY)), this.getClass().getClassLoader());

		pageMap = new HashMap<Class<? extends DREPage>, DREPage>();
		putPage(FileEntrySelectPage.class, new FileEntrySelectPage(this));
		putPage(PackageSelectPage.class, new PackageSelectPage(this));
		putPage(MeasureExecutePage.class, new MeasureExecutePage(this));
		putPage(SimilarEntrySelectPage.class, new SimilarEntrySelectPage(this));
		putPage(DisposeWaySelectPage.class, new DisposeWaySelectPage(this));
		putPage(DisposeExecutePage.class, new DisposeExecutePage(this));

		Display display = new Display();
		shell = new Shell(display);
		shell.setText(SOFTWARE_TITLE + " " + SOFTWARE_VERSION);
		shell.setSize(prefs.getInt(PREFS_SHELL_WIDTH_KEY), prefs.getInt(PREFS_SHELL_HEIGHT_KEY));
		{
			FormLayout l = new FormLayout();
			shell.setLayout(l);
		}
		shell.addShellListener(SHELL_CLOSED);
		createContents(shell);

		setActivePage(getPage(FileEntrySelectPage.class));

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		try {
			prefs.save();
		} catch (IOException e) {
		}

		for (DREPage page : pageMap.values()) {
			page.dispose();
		}
		display.dispose();
	}

	private <T extends DREPage> void putPage(Class<T> cls, T obj) {
		pageMap.put(cls, obj);
	}

	private void createContents(Composite parent) {
		titleBar = new Composite(parent, SWT.NONE);
		titleBar.setBackground(new Color(Display.getCurrent(), 255, 255, 255));
		{
			RowLayout l = new RowLayout(SWT.VERTICAL);
			l.marginWidth = l.marginHeight = 8;
			titleBar.setLayout(l);
		}
		pageTitleLabel = new Label(titleBar, SWT.NONE);
		pageTitleLabel.setBackground(new Color(Display.getCurrent(), 255, 255, 255));
		pageTitleLabel.setFont(new Font(Display.getCurrent(),
				Display.getCurrent().getSystemFont().getFontData()[0].getName(),
				Display.getCurrent().getSystemFont().getFontData()[0].getHeight(),
				SWT.BOLD));
		pageDescLabel = new Label(titleBar, SWT.NONE);
		pageDescLabel.setBackground(new Color(Display.getCurrent(), 255, 255, 255));

		Group contentGroup = new Group(parent, SWT.NONE);
		{
			FillLayout l = new FillLayout();
			l.marginWidth = l.marginHeight = 8;
			contentGroup.setLayout(l);
		}
		contentComp = new Composite(contentGroup, SWT.NONE);
		contentComp.setLayout(contentStack = new StackLayout());

		Composite buttonBar = new Composite(parent, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			l.marginWidth = l.marginHeight = 8;
			l.spacing = 8;
			buttonBar.setLayout(l);
		}
		prefsButton = new Button(buttonBar, SWT.PUSH);
		prefsButton.setText(messages.getString("DREFrame.CONFIG_BUTTON_TEXT"));
		prefsButton.addSelectionListener(PREFS_BUTTON_SELECTED);
		prevButton = new Button(buttonBar, SWT.PUSH);
		prevButton.setText(messages.getString("DREFrame.BACK_BUTTON_TEXT"));
		prevButton.addSelectionListener(PREV_BUTTON_SELECTED);
		nextButton = new Button(buttonBar, SWT.PUSH);
		nextButton.setText(messages.getString("DREFrame.NEXT_BUTTON_TEXT"));
		nextButton.addSelectionListener(NEXT_BUTTON_SELECTED);
		closeButton = new Button(buttonBar, SWT.PUSH);
		closeButton.setText(messages.getString("DREFrame.CLOSE_BUTTON_TEXT"));
		closeButton.addSelectionListener(CLOSE_BUTTON_SELECTED);

		prefsButton.setLayoutData(new FormDataBuilder().left(0).width(120).height(30).build());
		prevButton.setLayoutData(new FormDataBuilder().width(120).right(nextButton).height(30).build());
		nextButton.setLayoutData(new FormDataBuilder().width(120).right(closeButton).height(30).build());
		closeButton.setLayoutData(new FormDataBuilder().width(80).right(100).height(30).build());

		{
			FormData fd = new FormData();
			fd.top = new FormAttachment(0, 0);
			fd.left = new FormAttachment(0, 0);
			fd.right = new FormAttachment(100, 0);
			titleBar.setLayoutData(fd);
		}
		{
			FormData fd = new FormData();
			fd.top = new FormAttachment(titleBar);
			fd.bottom = new FormAttachment(buttonBar);
			fd.left = new FormAttachment(0, 0);
			fd.right = new FormAttachment(100, 0);
			contentGroup.setLayoutData(fd);
		}
		{
			FormData fd = new FormData();
			fd.bottom = new FormAttachment(100, 0);
			fd.left = new FormAttachment(0, 0);
			fd.right = new FormAttachment(100, 0);
			buttonBar.setLayoutData(fd);
		}

		for (DREPage page : pageMap.values()) {
			page.createContents(contentComp);
		}
	}

	Shell getShell() {
		return shell;
	}

	private final SelectionListener PREFS_BUTTON_SELECTED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			PrefsDialog dialog = new PrefsDialog(DREFrame.this);
			dialog.open();
		}
	};

	private final SelectionListener PREV_BUTTON_SELECTED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			currentPage.previousRequested();
		}
	};

	private final SelectionListener NEXT_BUTTON_SELECTED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			currentPage.nextRequested();
		}
	};

	private final SelectionListener CLOSE_BUTTON_SELECTED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			shell.close();
		}
	};

	private final ShellListener SHELL_CLOSED = new ShellAdapter() {
		@Override
		public void shellClosed(ShellEvent event) {
			MessageBox msgBox = new MessageBox(shell, SWT.YES | SWT.NO);
			msgBox.setText(messages.getString("DREFrame.CONFIRM_CLOSE_TITLE"));
			msgBox.setMessage(messages.getString("DREFrame.CONFIRM_CLOSE_MESSAGE"));
			int ret = msgBox.open();
			event.doit = (ret == SWT.YES);
			if (event.doit) {
				setActivePage(null);
				prefs.setValue(PREFS_SHELL_WIDTH_KEY, shell.getSize().x);
				prefs.setValue(PREFS_SHELL_HEIGHT_KEY, shell.getSize().y);
			}
		}
	};

	public static void main(String[] args) {
		PrintStream out = null;
		PrintStream err = null;
		try {
			if (JarPackaged.jarPackaged) {
				out = new PrintStream(new FileOutputStream("stdout")); //$NON-NLS-1$
				err = new PrintStream(new FileOutputStream("stderr")); //$NON-NLS-1$
				System.setOut(out);
				System.setErr(err);
			}
			new DREFrame().open();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) { out.flush(); out.close(); }
			if (err != null) { err.flush(); err.close(); }
		}
	}
}
