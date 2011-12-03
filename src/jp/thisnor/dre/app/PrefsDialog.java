package jp.thisnor.dre.app;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

class PrefsDialog extends Dialog {
	private static final String
		DRE_CORE_PACKAGE_PATH = "jp.thisnor.dre.core"; //$NON-NLS-1$

	private DREFrame mainFrame;
	private PreferenceStore prefs;
	private Messages messages;

	private Shell shell;
	private TableViewer prefsTableViewer;
	private Button closeButton;

	PrefsDialog(DREFrame mainFrame) {
		super(mainFrame.getShell(), SWT.NONE);
		this.mainFrame = mainFrame;
		this.prefs = mainFrame.getPreferences();
		this.messages = mainFrame.getMessages();
	}

	void open() {
		Display display = mainFrame.getShell().getDisplay();

		createContents();

		shell.open();

		while (!shell.isDisposed()) {
			if (display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private void createContents() {
		shell = new Shell(mainFrame.getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setText(messages.getString("PrefsDialog.TITLE"));
		{
			FormLayout l = new FormLayout();
			l.marginWidth = l.marginHeight = 8;
			l.spacing = 8;
			shell.setLayout(l);
		}

		prefsTableViewer = new TableViewer(shell, SWT.BORDER | SWT.H_SCROLL | SWT.VIRTUAL);
		prefsTableViewer.getTable().setHeaderVisible(true);
		prefsTableViewer.setContentProvider(PREFS_TABLE_CONTENT_PROVIDER);
		prefsTableViewer.setSorter(PREFS_TABLE_VIEWER_SORTER);
		prefsTableViewer.getTable().addMouseListener(PREFS_TABLE_CELL_CLICKED_LISTENER);
		prefsTableViewer.setInput(prefs);
		{
			TableLayout l = new TableLayout();
			l.addColumnData(new ColumnWeightData(50, true));
			l.addColumnData(new ColumnWeightData(50, true));
			prefsTableViewer.getTable().setLayout(l);
		}

		TableViewerColumn keyColumn = new TableViewerColumn(prefsTableViewer, SWT.NONE);
		keyColumn.getColumn().setText(messages.getString("PrefsDialog.COLUMN.KEY"));
		keyColumn.setLabelProvider(PREFS_TABLE_KEY_LABEL_PROVIDER);

		TableViewerColumn valueColumn = new TableViewerColumn(prefsTableViewer, SWT.NONE);
		valueColumn.getColumn().setText(messages.getString("PrefsDialog.COLUMN.VALUE"));
		valueColumn.setLabelProvider(PREFS_TABLE_VALUE_LABEL_PROVIDER);
		valueColumn.setEditingSupport(new PrefsTableEditingSupport());

		closeButton = new Button(shell, SWT.PUSH);
		closeButton.setText(messages.getString("PrefsDialog.CLOSE_BUTTON_TEXT"));
		closeButton.addSelectionListener(CLOSE_BUTTON_SELECTION_LISTENER);

		prefsTableViewer.getTable().setLayoutData(new FormDataBuilder().left(0).right(100).top(0).bottom(closeButton).build());
		closeButton.setLayoutData(new FormDataBuilder().width(120).right(100).height(30).bottom(100).build());
	}

	private final IContentProvider PREFS_TABLE_CONTENT_PROVIDER = new ArrayContentProvider() {
		@Override
		public Object[] getElements(Object inputElement) {
			String[] prefKeys = prefs.preferenceNames();
			return prefKeys;
		}
	};

	private final CellLabelProvider PREFS_TABLE_KEY_LABEL_PROVIDER = new CellLabelProvider() {
		@Override
		public void update(ViewerCell cell) {
			String key = (String)cell.getElement();
			if (key.startsWith(DRE_CORE_PACKAGE_PATH)) {
				key = key.substring(DRE_CORE_PACKAGE_PATH.length() + 1);
			}
			cell.setText(key);
		}
	};

	private final CellLabelProvider PREFS_TABLE_VALUE_LABEL_PROVIDER = new CellLabelProvider() {
		@Override
		public void update(ViewerCell cell) {
			String key = (String)cell.getElement();
			cell.setText(prefs.getString(key));
		}
	};

	private class PrefsTableEditingSupport extends EditingSupport {
		private PrefsTableEditingSupport() {
			super(prefsTableViewer);
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(prefsTableViewer.getTable());
		}

		@Override
		protected Object getValue(Object element) {
			return prefs.getString((String)element);
		}

		@Override
		protected void setValue(Object element, Object value) {
			prefs.setValue((String)element, String.valueOf(value));
			prefsTableViewer.refresh(element);
		}
	};

	private final MouseListener PREFS_TABLE_CELL_CLICKED_LISTENER = new MouseAdapter() {
		@Override
		public void mouseDown(MouseEvent event) {
			Table t = prefsTableViewer.getTable();
			for (int i = 0; i < t.getItemCount(); i++) {
				TableItem item = t.getItem(i);
				for (int j = 0; j < t.getColumnCount(); j++) {
					if (item.getBounds(j).contains(event.x, event.y)) {
						prefsTableViewer.editElement(item.getData(), 1);
						return;
					}
				}
			}
		}
	};

	private final ViewerSorter PREFS_TABLE_VIEWER_SORTER = new ViewerSorter() {
		@Override
		public int category(Object element) {
			return ((String)element).startsWith(DRE_CORE_PACKAGE_PATH) ? 0 : 1;
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int cat1 = category(e1), cat2 = category(e2);
			if (cat1 == cat2) {
				return ((String)e1).compareTo((String)e2);
			} else {
				return (cat1 < cat2) ? -1 : 1;
			}
		}
	};

	private final SelectionListener CLOSE_BUTTON_SELECTION_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			shell.close();
		}
	};
}
