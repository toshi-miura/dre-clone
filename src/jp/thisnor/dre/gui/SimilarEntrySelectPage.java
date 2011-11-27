package jp.thisnor.dre.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

class SimilarEntrySelectPage extends DREPage {
	private final DREFrame frame;
	private Messages messages;

	private SashForm rootComp;
	private Composite fileEntryComp;
	private Label fileEntryLabel;
	private Table fileEntryTable;
	private Composite similarEntryComp;
	private Label similarEntryLabel;
	private TableViewer similarEntryTableViewer;
	private ImagePreviewer previewer;
	private SimilarEntryCheckerViewer checkerViewer;

	private Map<FileEntry, List<SimilarEntry>> similarMap;
	private Set<FileEntry> checkedFileSet;

	private ExecutorService appendEntryExecutor;

	SimilarEntrySelectPage(DREFrame frame) {
		this.frame = frame;
		this.messages = frame.getMessages();
		this.checkedFileSet = Collections.synchronizedSet(new HashSet<FileEntry>());
	}

	@Override
	void createContents(Composite parent) {
		rootComp = new SashForm(parent, SWT.HORIZONTAL);
		rootComp.setSashWidth(8);

		SashForm entryTableComp = new SashForm(rootComp, SWT.VERTICAL);
		entryTableComp.setSashWidth(8);

		fileEntryComp = new Composite(entryTableComp, SWT.NONE);
		fileEntryComp.setLayout(new FormLayout());

		fileEntryLabel = new Label(fileEntryComp, SWT.NONE);
		fileEntryLabel.setText(messages.getString("SimilarEntrySelectPage.FILE_TABLE_CAPTION"));

		Label fileEntryControlLabel = new Label(fileEntryComp, SWT.NONE);
		fileEntryControlLabel.setText(messages.getString("SimilarEntrySelectPage.FILE_TABLE_CONTROL_CAPTION"));
		fileEntryControlLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));

		fileEntryTable = new Table(fileEntryComp, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
		fileEntryTable.addSelectionListener(FILE_TABLE_SELECTION_LISTENER);
		fileEntryTable.addSelectionListener(FILE_TABLE_CHECK_LISTENER);
		fileEntryTable.addKeyListener(FILE_TABLE_SHIFT_LISTENER);
		fileEntryTable.addKeyListener(COMMON_TABLE_KEY_LISTENER);
		{
			TableLayout l = new TableLayout();
			l.addColumnData(new ColumnWeightData(80, true));
			l.addColumnData(new ColumnWeightData(20, true));
			fileEntryTable.setLayout(l);
		}
		{
			TableColumn entryColumn = new TableColumn(fileEntryTable, SWT.NONE);
			entryColumn.setText(messages.getString("SimilarEntrySelectPage.FILE_TABLE.COLUMN.FILE"));
			TableColumn distColumn = new TableColumn(fileEntryTable, SWT.NONE);
			distColumn.setText(messages.getString("SimilarEntrySelectPage.FILE_TABLE.COLUMN.DISTANCE"));
		}
		fileEntryTable.setHeaderVisible(true);

		fileEntryLabel.setLayoutData(new FormDataBuilder().left(0).top(0).build());
		fileEntryControlLabel.setLayoutData(new FormDataBuilder().right(100).top(0).build());
		fileEntryTable.setLayoutData(new FormDataBuilder().left(0).right(100).top(fileEntryLabel).bottom(100).build());

		similarEntryComp = new Composite(entryTableComp, SWT.NONE);
		similarEntryComp.setLayout(new FormLayout());

		similarEntryLabel = new Label(similarEntryComp, SWT.NONE);
		similarEntryLabel.setText(messages.getString("SimilarEntrySelectPage.SIMILAR_TABLE_CAPTION"));

		Label similarEntryControlLabel = new Label(similarEntryComp, SWT.NONE);
		similarEntryControlLabel.setText(messages.getString("SimilarEntrySelectPage.SIMILAR_TABLE_CONTROL_CAPTION"));
		similarEntryControlLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));

		Table similarEntryTable = new Table(similarEntryComp, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
		{
			TableColumn entryColumn = new TableColumn(similarEntryTable, SWT.NONE);
			entryColumn.setText(messages.getString("SimilarEntrySelectPage.SIMILAR_TABLE.COLUMN.FILE"));
			TableColumn distColumn = new TableColumn(similarEntryTable, SWT.NONE);
			distColumn.setText(messages.getString("SimilarEntrySelectPage.SIMILAR_TABLE.COLUMN.DISTANCE"));
		}
		similarEntryTable.setHeaderVisible(true);
		similarEntryTableViewer = new TableViewer(similarEntryTable);
		similarEntryTableViewer.setContentProvider(SIMILAR_TABLE_CONTENT_PROVIDER);
		similarEntryTableViewer.setLabelProvider(SIMILAR_TABLE_LABEL_PROVIDER);
		similarEntryTableViewer.addSelectionChangedListener(SIMILAR_TABLE_SELECTED);
		similarEntryTable.addSelectionListener(SIMILAR_TABLE_CHECK_LISTENER);
		similarEntryTable.addKeyListener(SIMILAR_TABLE_SHIFT_LISTENER);
		similarEntryTable.addKeyListener(COMMON_TABLE_KEY_LISTENER);
		{
			TableLayout l = new TableLayout();
			l.addColumnData(new ColumnWeightData(80, true));
			l.addColumnData(new ColumnWeightData(20, true));
			similarEntryTable.setLayout(l);
		}

		similarEntryLabel.setLayoutData(new FormDataBuilder().left(0).top(0).build());
		similarEntryControlLabel.setLayoutData(new FormDataBuilder().right(100).top(0).build());
		similarEntryTable.setLayoutData(new FormDataBuilder().left(0).right(100).top(similarEntryLabel).bottom(100).build());

		TabFolder tabFolder = new TabFolder(rootComp, SWT.NONE);

		Composite previewerComp = new Composite(tabFolder, SWT.NONE);
		{
			FillLayout l = new FillLayout();
			l.marginWidth = l.marginHeight = 8;
			previewerComp.setLayout(l);
		}

		previewer = new ImagePreviewer(frame);
		previewer.createContents(previewerComp);

		TabItem previewerTab = new TabItem(tabFolder, SWT.NONE);
		previewerTab.setText(messages.getString("SimilarEntrySelectPage.TAB.PREVIEWER"));
		previewerTab.setControl(previewerComp);

		Composite checkerComp = new Composite(tabFolder, SWT.NONE);
		{
			FillLayout l = new FillLayout();
			l.marginWidth = l.marginHeight = 8;
			checkerComp.setLayout(l);
		}

		checkerViewer = new SimilarEntryCheckerViewer(frame);
		checkerViewer.createContents(checkerComp);

		TabItem checkerTab = new TabItem(tabFolder, SWT.NONE);
		checkerTab.setText(messages.getString("SimilarEntrySelectPage.TAB.CHECKER"));
		checkerTab.setControl(checkerComp);
	}

	Set<FileEntry> getCheckedFileSet() {
		return checkedFileSet;
	}

	void setFileChecked(Set<FileEntry> fileEntrySet, int mode) {
		switch (mode) {
		case 0: // check off
			checkedFileSet.removeAll(fileEntrySet);
			break;
		case 1: // check on
			checkedFileSet.addAll(fileEntrySet);
			break;
		case -1: // check turn
			checkedFileSet = new HashSet<FileEntry>(similarMap.keySet());
			for (List<SimilarEntry> l : similarMap.values()) {
				for (SimilarEntry similar : l) {
					checkedFileSet.add(similar.fileEntry);
				}
			}
			checkedFileSet.removeAll(fileEntrySet);
			break;
		}
		updateFileTableCheckState();
		updateSimilarTableCheckState();
	}

	void setFileChecked(FileEntry file, boolean checked) {
		if (checked) {
			checkedFileSet.add(file);
		} else {
			checkedFileSet.remove(file);
		}
		updateFileTableCheckState();
		updateSimilarTableCheckState();
	}

	void updateFileTableCheckState() {
		for (TableItem item : fileEntryTable.getItems()) {
			@SuppressWarnings("unchecked")
			Entry<FileEntry, List<SimilarEntry>> entry = (Entry<FileEntry, List<SimilarEntry>>)item.getData();
			item.setChecked(checkedFileSet.contains(entry.getKey()));
		}
	}

	void updateSimilarTableCheckState() {
		for (TableItem item : similarEntryTableViewer.getTable().getItems()) {
			SimilarEntry similar = (SimilarEntry)item.getData();
			item.setChecked(checkedFileSet.contains(similar.fileEntry));
		}
	}

	private void setActiveFileEntryItem(TableItem item) {
		@SuppressWarnings("unchecked")
		Entry<FileEntry, List<SimilarEntry>> entry = (Entry<FileEntry, List<SimilarEntry>>)item.getData();
		similarEntryTableViewer.setInput(entry);
		similarEntryTableViewer.getTable().setSelection(0);
		for (TableItem simItem : similarEntryTableViewer.getTable().getItems()) {
			SimilarEntry similar = (SimilarEntry)simItem.getData();
			if (checkedFileSet.contains(similar.fileEntry)) simItem.setChecked(true);
		}
		previewer.setFileEntry(entry.getKey());
		similarEntryLabel.setText(messages.getString("SimilarEntrySelectPage.SIMILAR_TABLE_CAPTION") + " (" + (entry.getValue().size() + 1) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		similarEntryComp.layout();
	}

	@Override
	void activated() {
		frame.setPageTitle(messages.getString("SimilarEntrySelectPage.PAGE_TITLE"));
		frame.setPageDescription(messages.getString("SimilarEntrySelectPage.PAGE_DESCRIPTION"));
		frame.setContent(rootComp);
		frame.setNextButtonEnabled(true);
		frame.setPreviousButtonEnabled(true);

		similarMap = frame.getPage(MeasureExecutePage.class).getSimilarMap();
		for (Iterator<Entry<FileEntry, List<SimilarEntry>>> it = similarMap.entrySet().iterator(); it.hasNext(); ) {
			Entry<FileEntry, List<SimilarEntry>> entry = it.next();
			FileEntry fileEntry = entry.getKey();
			if (fileEntry instanceof NormalFileEntry) {
				if (!new File(fileEntry.getPath()).exists()) {
					it.remove();
					continue;
				}
			}
			List<SimilarEntry> similarList = entry.getValue();
			for (Iterator<SimilarEntry> it2 = similarList.iterator(); it2.hasNext(); ) {
				FileEntry simFileEntry = it2.next().fileEntry;
				if (simFileEntry instanceof NormalFileEntry) {
					if (!new File(simFileEntry.getPath()).exists()) {
						it2.remove();
					}
				}
			}
			if (similarList.isEmpty()) {
				it.remove();
			}
		}

		fileEntryTable.removeAll();
		similarEntryTableViewer.getTable().removeAll();
		previewer.setFileEntry(null);
		checkedFileSet.clear();

		fileEntryLabel.setText(messages.getString("SimilarEntrySelectPage.FILE_TABLE_CAPTION") + " (" + similarMap.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		fileEntryComp.layout();
		similarEntryLabel.setText(messages.getString("SimilarEntrySelectPage.SIMILAR_TABLE_CAPTION"));
		similarEntryComp.layout();

		appendEntryExecutor = Executors.newSingleThreadExecutor();
		appendEntryExecutor.execute(APPEND_ENTRIES_TASK);
	}

	@Override
	void hiddened() {
		if (!appendEntryExecutor.isShutdown()) {
			appendEntryExecutor.shutdownNow();
		}

		checkerViewer.hiddened();
	}

	@Override
	void nextRequested() {
		frame.setActivePage(frame.getPage(DisposeWaySelectPage.class));
	}

	@Override
	void previousRequested() {
		MessageBox msgBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.YES | SWT.NO);
		msgBox.setText(messages.getString("SimilarEntrySelectPage.CONFIRM_BACK_TITLE"));
		msgBox.setMessage(messages.getString("SimilarEntrySelectPage.CONFIRM_BACK_MESSAGE"));
		if (msgBox.open() == SWT.NO) return;
		frame.setActivePage(frame.getPage(PackageSelectPage.class));
	}

	@Override
	void dispose() {

	}

	private final Runnable APPEND_ENTRIES_TASK = new Runnable() {
		public void run() {
			List<Entry<FileEntry, List<SimilarEntry>>> list = new ArrayList<Entry<FileEntry, List<SimilarEntry>>>(similarMap.entrySet());
			Collections.sort(list, SIMILAR_MAP_COMPARATOR);
			boolean first = true;
			int waitCounter = 32;
			for (final Entry<FileEntry, List<SimilarEntry>> entry : list) {
				final boolean ffirst = first;
				first = false;
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						if (fileEntryTable.isDisposed()) return;
						TableItem item = new TableItem(fileEntryTable, SWT.NONE);
						item.setText(0, entry.getKey().getName());
						item.setText(1,	Integer.toString(entry.getValue().get(0).distance));
						item.setData(entry);
						if (checkedFileSet.contains(entry.getKey())) {
							item.setChecked(true);
						}
						if (ffirst) {
							setActiveFileEntryItem(item);
							fileEntryTable.setSelection(0);
						}
					}
				});
				if (--waitCounter == 0) {
					try {
						Thread.sleep(16);
					} catch (InterruptedException e) {
						break;
					}
					waitCounter = 32;
				}
			}
		}
	};

	private final Comparator<Entry<FileEntry, List<SimilarEntry>>> SIMILAR_MAP_COMPARATOR = new Comparator<Entry<FileEntry, List<SimilarEntry>>>() {
		@Override
		public int compare(Entry<FileEntry, List<SimilarEntry>> o1, Entry<FileEntry, List<SimilarEntry>> o2) {
			int mind1 = o1.getValue().get(0).distance;
			int mind2 = o2.getValue().get(0).distance;
			if (mind1 != mind2) {
				return mind1 > mind2 ? 1 : -1;
			}
			int minh1 = o1.getKey().hashCode();
			for (SimilarEntry entry : o1.getValue()) {
				if (entry.fileEntry.hashCode() < minh1) minh1 = entry.fileEntry.hashCode();
			}
			int minh2 = o2.getKey().hashCode();
			for (SimilarEntry entry : o2.getValue()) {
				if (entry.fileEntry.hashCode() < minh2) minh2 = entry.fileEntry.hashCode();
			}
			return minh1 > minh2 ? 1 : (minh1 == minh2 ? 0 : -1);
		}
	};

	private final SelectionListener FILE_TABLE_SELECTION_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			TableItem item = (TableItem)event.item;
			setActiveFileEntryItem(item);
		}
	};

	private final KeyListener FILE_TABLE_SHIFT_LISTENER = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent event) {
			if (event.keyCode == SWT.SHIFT) {
				similarEntryTableViewer.getTable().forceFocus();
			}
		}
	};

	private final IContentProvider SIMILAR_TABLE_CONTENT_PROVIDER = new ArrayContentProvider() {
		@Override
		public Object[] getElements(Object inputElement) {
			@SuppressWarnings("unchecked")
			Entry<FileEntry, List<SimilarEntry>> entry = (Entry<FileEntry, List<SimilarEntry>>)inputElement;
			Object[] array = new Object[entry.getValue().size() + 1];
			array[0] = new SimilarEntry(entry.getKey(), 0);
			for (int i = 1; i < array.length; i++) {
				array[i] = entry.getValue().get(i - 1);
			}
			return array;
		}
	};

	private final ITableLabelProvider SIMILAR_TABLE_LABEL_PROVIDER = new ITableLabelProvider() {
		@Override
		public void addListener(ILabelProviderListener arg0) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {
		}

		@Override
		public Image getColumnImage(Object arg0, int arg1) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int index) {
			SimilarEntry similar = (SimilarEntry)element;
			return (index == 0) ? similar.fileEntry.getName() : Integer.toString(similar.distance);
		}
	};

	private final ISelectionChangedListener SIMILAR_TABLE_SELECTED = new ISelectionChangedListener() {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			SimilarEntry similar = (SimilarEntry)((IStructuredSelection)event.getSelection()).getFirstElement();
			if (similar != null) {
				previewer.setFileEntry(similar.fileEntry);
			}
		}
	};

	private final KeyListener SIMILAR_TABLE_SHIFT_LISTENER = new KeyAdapter() {
		@Override
		public void keyReleased(KeyEvent event) {
			if (event.keyCode == SWT.SHIFT) {
				fileEntryTable.forceFocus();
			}
		}
	};

	private final SelectionListener FILE_TABLE_CHECK_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			if (event.detail != SWT.CHECK) return;
			TableItem item = (TableItem)event.item;
			@SuppressWarnings("unchecked")
			FileEntry file = ((Entry<FileEntry, List<SimilarEntry>>)item.getData()).getKey();
			setFileChecked(file, item.getChecked());
		}
	};

	private final SelectionListener SIMILAR_TABLE_CHECK_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			if (event.detail != SWT.CHECK) return;
			TableItem item = (TableItem)event.item;
			FileEntry file = ((SimilarEntry)item.getData()).fileEntry;
			setFileChecked(file, item.getChecked());
		}
	};

	private final KeyListener COMMON_TABLE_KEY_LISTENER = new KeyListener() {
		@Override
		public void keyPressed(KeyEvent event) {
			switch (event.keyCode) {
			case SWT.CTRL:
				previewer.setLoupeEnabled(true);
				previewer.setMouseDown(true);
				break;
/*
			case SWT.ARROW_UP:
				if (previewer.getLoupeEnabled()) {
					previewer.setLoupePosition(previewer.getLoupePositionX(), previewer.getLoupePositionY() - 10);
				}
				break;
			case SWT.ARROW_DOWN:
				if (previewer.getLoupeEnabled())
					previewer.setLoupePosition(previewer.getLoupePositionX(), previewer.getLoupePositionY() + 10);
				break;
			case SWT.ARROW_LEFT:
				if (previewer.getLoupeEnabled())
					previewer.setLoupePosition(previewer.getLoupePositionX() - 10, previewer.getLoupePositionY());
				break;
			case SWT.ARROW_RIGHT:
				if (previewer.getLoupeEnabled())
					previewer.setLoupePosition(previewer.getLoupePositionX() + 10, previewer.getLoupePositionY());
				break;
*/
			}
		}

		@Override
		public void keyReleased(KeyEvent event) {
			switch (event.keyCode) {
			case SWT.CTRL:
				previewer.setLoupeEnabled(false);
				previewer.setMouseDown(false);
				break;
			}
		}
	};
}
