package jp.thisnor.dre.app;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.thisnor.dre.core.FileEntry;
import jp.thisnor.dre.core.Messages;
import jp.thisnor.dre.core.NormalFileEntry;
import jp.thisnor.dre.core.SimilarEntry;
import jp.thisnor.dre.core.SimilarGroup;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
	private Table similarEntryTable;
	private ImagePreviewer previewer;
	private SimilarEntryCheckerViewer checkerViewer;

	private List<SimilarGroup> simGroupList;
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
		fileEntryTable.addKeyListener(FILE_TABLE_KEY_LISTENER);
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

		similarEntryTable = new Table(similarEntryComp, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
		{
			TableColumn entryColumn = new TableColumn(similarEntryTable, SWT.NONE);
			entryColumn.setText(messages.getString("SimilarEntrySelectPage.SIMILAR_TABLE.COLUMN.FILE"));
			TableColumn distColumn = new TableColumn(similarEntryTable, SWT.NONE);
			distColumn.setText(messages.getString("SimilarEntrySelectPage.SIMILAR_TABLE.COLUMN.DISTANCE"));
		}
		similarEntryTable.setHeaderVisible(true);
		similarEntryTable.addSelectionListener(SIMILAR_TABLE_SELECTION_LISTENER);
		similarEntryTable.addKeyListener(SIMILAR_TABLE_KEY_LISTENER);
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
			for (FileEntry entry : fileEntrySet) {
				if (checkedFileSet.contains(entry)) {
					checkedFileSet.remove(entry);
				} else {
					checkedFileSet.add(entry);
				}
			}
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
			SimilarGroup simGroup = (SimilarGroup)item.getData();
			item.setChecked(checkedFileSet.contains(simGroup.getFileEntry()));
		}
	}

	void updateSimilarTableCheckState() {
		for (TableItem item : similarEntryTable.getItems()) {
			FileEntry fileEntry = (FileEntry)item.getData();
			item.setChecked(checkedFileSet.contains(fileEntry));
		}
	}

	private void setActiveFileEntryItem(TableItem item) {
		SimilarGroup simGroup = (SimilarGroup)item.getData();
		similarEntryTable.removeAll();
		{
			TableItem simItem = new TableItem(similarEntryTable, SWT.NONE);
			simItem.setText(0, simGroup.getFileEntry().getName());
			simItem.setText(1, "0");
			simItem.setData(simGroup.getFileEntry());
		}
		for (SimilarEntry e : simGroup.getSimilarList()) {
			TableItem simItem = new TableItem(similarEntryTable, SWT.NONE);
			simItem.setText(0, e.getFileEntry().getName());
			simItem.setText(1, Integer.toString(e.getDistance()));
			simItem.setData(e.getFileEntry());
		}
		updateSimilarTableCheckState();
		previewer.setFileEntry(simGroup.getFileEntry());
//		for (SimilarEntry e : simGroup.getSimilarList()) {
//			previewer.preloadImage(e.getFileEntry());
//		}
		similarEntryLabel.setText(messages.getString("SimilarEntrySelectPage.SIMILAR_TABLE_CAPTION") + " (" + (simGroup.getSimilarList().size() + 1) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		similarEntryComp.layout();
	}

	@Override
	void activated() {
		frame.setPageTitle(messages.getString("SimilarEntrySelectPage.PAGE_TITLE"));
		frame.setPageDescription(messages.getString("SimilarEntrySelectPage.PAGE_DESCRIPTION"));
		frame.setContent(rootComp);
		frame.setNextButtonEnabled(true);
		frame.setPreviousButtonEnabled(true);

		simGroupList = frame.getPage(MeasureExecutePage.class).getSimilarGroupList();
		for (Iterator<SimilarGroup> it = simGroupList.iterator(); it.hasNext(); ) {
			SimilarGroup simGroup = it.next();
			FileEntry fileEntry = simGroup.getFileEntry();
			if (fileEntry instanceof NormalFileEntry && !new File(fileEntry.getPath()).exists()) {
				it.remove();
				continue;
			}
			List<SimilarEntry> simList = simGroup.getSimilarList();
			for (Iterator<SimilarEntry> it2 = simList.iterator(); it2.hasNext(); ) {
				FileEntry simFileEntry = it2.next().getFileEntry();
				if (simFileEntry instanceof NormalFileEntry && !new File(simFileEntry.getPath()).exists()) {
					it2.remove();
				}
			}
			if (simList.isEmpty()) {
				it.remove();
			}
		}

		fileEntryTable.removeAll();
		similarEntryTable.removeAll();
		previewer.setFileEntry(null);
		checkedFileSet.clear();

		fileEntryLabel.setText(messages.getString("SimilarEntrySelectPage.FILE_TABLE_CAPTION") + " (" + simGroupList.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
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
		MessageBox msgBox = new MessageBox(Display.getDefault().getActiveShell(), SWT.YES | SWT.NO);
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
			boolean first = true;
			int waitCounter = 32;
			for (final SimilarGroup entry : simGroupList) {
				final boolean ffirst = first;
				first = false;
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						if (fileEntryTable.isDisposed()) return;
						TableItem item = new TableItem(fileEntryTable, SWT.NONE);
						item.setText(0, entry.getFileEntry().getName());
						item.setText(1,	Integer.toString(entry.getSimilarList().get(0).getDistance()));
						item.setData(entry);
						if (checkedFileSet.contains(entry.getFileEntry())) {
							item.setChecked(true);
						}
						if (ffirst) {
							setActiveFileEntryItem(item);
							fileEntryTable.setSelection(0);
							similarEntryTable.setSelection(0);
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

	private final SelectionListener FILE_TABLE_SELECTION_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			if (event.detail == SWT.CHECK) {
				TableItem item = (TableItem)event.item;
				FileEntry fileEntry = ((SimilarGroup)item.getData()).getFileEntry();
				setFileChecked(fileEntry, item.getChecked());
			} else {
				setActiveFileEntryItem((TableItem)event.item);
				similarEntryTable.setSelection(0);
			}
		}
	};

	private final KeyListener FILE_TABLE_KEY_LISTENER = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent event) {
			if (event.keyCode == SWT.SHIFT) {
				similarEntryTable.forceFocus();
			}
		}
	};

	private final SelectionListener SIMILAR_TABLE_SELECTION_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			if (event.detail == SWT.CHECK) {
				TableItem item = (TableItem)event.item;
				FileEntry fileEntry = (FileEntry)item.getData();
				setFileChecked(fileEntry, item.getChecked());
			} else {
				previewer.setFileEntry((FileEntry)((TableItem)event.item).getData());
			}
		}
	};

	private final KeyListener SIMILAR_TABLE_KEY_LISTENER = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent event) {
			if (event.keyCode == SWT.ARROW_DOWN && event.stateMask == SWT.SHIFT &&
					similarEntryTable.getSelectionIndex() == similarEntryTable.getItemCount() - 1 &&
					fileEntryTable.getSelectionIndex() < fileEntryTable.getItemCount() - 1) {
				int newSelectedIndex = fileEntryTable.getSelectionIndex() + 1;
				setActiveFileEntryItem(fileEntryTable.getItem(newSelectedIndex));
				fileEntryTable.setSelection(newSelectedIndex);
				similarEntryTable.setSelection(0);
				event.doit = false;
			} else if (event.keyCode == SWT.ARROW_UP && event.stateMask == SWT.SHIFT &&
					similarEntryTable.getSelectionIndex() == 0 &&
					fileEntryTable.getSelectionIndex() > 0) {
				int newSelectedIndex = fileEntryTable.getSelectionIndex() - 1;
				setActiveFileEntryItem(fileEntryTable.getItem(newSelectedIndex));
				fileEntryTable.setSelection(newSelectedIndex);
				similarEntryTable.setSelection(similarEntryTable.getItemCount() - 1);
				event.doit = false;
			}
		}

		@Override
		public void keyReleased(KeyEvent event) {
			if (event.keyCode == SWT.SHIFT) {
				fileEntryTable.forceFocus();
			}
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
