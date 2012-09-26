package jp.thisnor.dre.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import jp.thisnor.dre.core.FileEntry;
import jp.thisnor.dre.core.Messages;
import jp.thisnor.dre.core.NormalFileEntry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

class FileDropListViewer {
	private static final int SUBCHECK_COLUMN_WIDTH = 48;

	private static final String
		FILE_ICON_PATH = "res/icon/page32.png", //$NON-NLS-1$
		IMAGE_FILE_ICON_PATH = "res/icon/image32.png", //$NON-NLS-1$
		NO_FILE_ICON_PATH = "res/icon/page_remove32.png", //$NON-NLS-1$
//		FOLDER_ICON_PATH = "res/icon/folder_full32.png",
		EMPTY_FOLDER_ICON_PATH = "res/icon/folder32.png", //$NON-NLS-1$
		APPEND_FILE_ICON_PATH = "res/icon/page_add_inactive32.png", //$NON-NLS-1$
		APPEND_FOLDER_ICON_PATH = "res/icon/folder_add_inactive32.png"; //$NON-NLS-1$

	private static final String
		TABLE_EDITOR_KEY = "tableEditor",
		TABLE_PROPS_KEY = "props",
		TABLE_FUTURE_KEY = "future"; //$NON-NLS-1$

	private Messages messages;

	private Composite rootComp;
	private Table fileTable;
	private TableItem appendDirItem, appendFileItem;

	private Image fileIconImage, imageFileIconImage, noFileIconImage, emptyFolderIconImage; // folderIconImage
	private Image appendFileIconImage, appendFolderIconImage;

	private ExecutorService computePropertiesExecutor;

	private List<String> pathList;
	private boolean listChanged;

	FileDropListViewer(DREFrame frame, Composite parentComp) {
		messages = frame.getMessages();
		computePropertiesExecutor = Executors.newSingleThreadExecutor();
		listChanged = true;

		fileIconImage = ImageUtils.loadImage(FILE_ICON_PATH);
		imageFileIconImage = ImageUtils.loadImage(IMAGE_FILE_ICON_PATH);
		noFileIconImage = ImageUtils.loadImage(NO_FILE_ICON_PATH);
//		folderIconImage = ImageUtils.loadImage(FOLDER_ICON_PATH);
		emptyFolderIconImage = ImageUtils.loadImage(EMPTY_FOLDER_ICON_PATH);
		appendFileIconImage = ImageUtils.loadImage(APPEND_FILE_ICON_PATH);
		appendFolderIconImage = ImageUtils.loadImage(APPEND_FOLDER_ICON_PATH);

		rootComp = new Composite(parentComp, SWT.NONE);
		rootComp.setLayout(newFormLayout(8));

		fileTable = new Table(rootComp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		fileTable.setLinesVisible(false);
		fileTable.addSelectionListener(FILE_TABLE_SELECTION_LISTENER);
		fileTable.addKeyListener(FILE_TABLE_KEY_LISTENER);
		fileTable.addControlListener(FILE_TABLE_RESIZE_LISTENER);
		fileTable.addPaintListener(FILE_TABLE_PAINT_LISTENER);
		fileTable.addListener(SWT.MeasureItem, FILE_TABLE_MEASURE_ITEM_LISTENER);
		fileTable.addListener(SWT.EraseItem, FILE_TABLE_ERASE_ITEM_LISTENER);
		fileTable.addListener(SWT.PaintItem, FILE_TABLE_PAINT_ITEM_LISTENER);

		TableColumn fileColumn = new TableColumn(fileTable, SWT.NONE);
		fileColumn.setText(messages.getString("FileDropListViewer.COLUMNS.FILE"));
		fileColumn.setWidth(0);

		TableColumn subCheckColumn = new TableColumn(fileTable, SWT.NONE);
		subCheckColumn.setText(messages.getString("FileDropListViewer.COLUMNS.SUBENTRY"));
		subCheckColumn.setWidth(SUBCHECK_COLUMN_WIDTH);

		appendDirItem = new TableItem(fileTable, SWT.NONE);
		appendDirItem.setImage(appendFolderIconImage);
		appendDirItem.setText(messages.getString("FileDropListViewer.ITEMS.APPEND_DIR"));

		appendFileItem = new TableItem(fileTable, SWT.NONE);
		appendFileItem.setImage(appendFileIconImage);
		appendFileItem.setText(messages.getString("FileDropListViewer.ITEMS.APPEND_FILE"));

		Menu fileTableMenu = new Menu(fileTable.getShell(), SWT.POP_UP);
		MenuItem changeToDirMenuItem = new MenuItem(fileTableMenu, SWT.PUSH);
		changeToDirMenuItem.setText(messages.getString("FileDropListViewer.CONTEXT_MENU.OPEN_DIR"));
		changeToDirMenuItem.addSelectionListener(FILE_TABLE_CHANGE_TO_DIR_LISTENER);
		MenuItem changeToFileMenuItem = new MenuItem(fileTableMenu, SWT.PUSH);
		changeToFileMenuItem.setText(messages.getString("FileDropListViewer.CONTEXT_MENU.OPEN_FILE"));
		changeToFileMenuItem.addSelectionListener(FILE_TABLE_CHANGE_TO_FILE_LISTENER);
		MenuItem removeMenuItem = new MenuItem(fileTableMenu, SWT.PUSH);
		removeMenuItem.setText(messages.getString("FileDropListViewer.CONTEXT_MENU.REMOVE"));
		removeMenuItem.addSelectionListener(FILE_TABLE_REMOVE_ITEM_LISTENER);
		removeMenuItem.setAccelerator(SWT.DEL);
		fileTable.setMenu(fileTableMenu);

		fileTable.setLayoutData(new FormDataBuilder().left(0).right(100).top(0).bottom(100).build());

		DropTarget dropTarget = new DropTarget(fileTable, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		dropTarget.setTransfer(new Transfer[]{ FileTransfer.getInstance() });
		dropTarget.addDropListener(FILE_TABLE_DROP_LISTENER);

		DragSource dragSource = new DragSource(fileTable, DND.DROP_MOVE | DND.DROP_COPY);
		dragSource.setTransfer(new Transfer[]{ FileTransfer.getInstance() });
		dragSource.addDragListener(FILE_TABLE_DRAG_LISTENER);
	}

	Control getControl() {
		return rootComp;
	}

	void setEnabled(boolean enabled) {
		fileTable.setEnabled(enabled);
		for (TableItem item : fileTable.getItems()) {
			TableEditor editor = (TableEditor)item.getData(TABLE_EDITOR_KEY);
			if (editor != null) editor.getEditor().setEnabled(enabled);
		}
	}

	void addFile(String path) {
		addFile(path, fileTable.getItemCount() - 2);
	}
	void addFile(String path, int insertPos) {
		addFiles(new String[] { path }, insertPos);
	}
	void addFiles(String[] paths) {
		addFiles(paths, fileTable.getItemCount() - 2);
	}
	void addFiles(String[] paths, int insertPos) {
		addFilesRaw(paths, insertPos);
		updateCheckboxLocation(0);
	}

	List<String> getExtPathList() {
		if (listChanged) {
			pathList = new ArrayList<String>(fileTable.getItemCount() - 2);
			for (TableItem item : fileTable.getItems()) {
				if (item == appendDirItem || item == appendFileItem) continue;
				StringBuilder sb = new StringBuilder();
				sb.append(((FileEntry)item.getData()).getPath());
				TableEditor editor = (TableEditor)item.getData(TABLE_EDITOR_KEY);
				if (editor != null && ((Button)editor.getEditor()).getSelection()) {
					sb.append("//");
				}
				pathList.add(sb.toString());
			}
			listChanged = false;
		}
		return pathList;
	}

	void dispose() {
		computePropertiesExecutor.shutdownNow();
	}

	private FormLayout newFormLayout(int spacing) {
		FormLayout l = new FormLayout();
		l.spacing = 8;
		return l;
	}

	private String toRawPath(String exPath) {
		return exPath.endsWith("//") ? exPath.substring(0, exPath.length() - 2) : exPath;
	}

	private void addFilesRaw(String[] exPaths, int insertPos) {
		for (String exPath : exPaths) {
			boolean preSub = false;
			String preProps = null;
			for (int i = 0; i < fileTable.getItemCount() - 2; i++) {
				TableItem item = fileTable.getItem(i);
				FileEntry fe = (FileEntry)item.getData();
				if (fe != null && fe.getPath().equals(toRawPath(exPath))) {
					preSub = item.getData(TABLE_EDITOR_KEY) != null ?
							((Button)((TableEditor)item.getData(TABLE_EDITOR_KEY)).getEditor()).getSelection() : false;
					preProps = (String)item.getData(TABLE_PROPS_KEY);
					removeItemsRaw(new int[] {i});
					if (insertPos > i) insertPos--;
					break;
				}
			}
			TableItem item = newTableItem(exPath, insertPos++);
			if (preSub) {
				TableEditor editorCtrl = (TableEditor)item.getData(TABLE_EDITOR_KEY);
				if (editorCtrl != null)
					((Button)editorCtrl.getEditor()).setSelection(preSub);
			}
			if (preProps != null) {
				item.setData(TABLE_PROPS_KEY, preProps);
			} else {
				item.setData(
						TABLE_FUTURE_KEY,
						computePropertiesExecutor.submit(new ComputeFilePropertiesTask(item)));
			}
		}
		listChanged = true;
	}

	private TableItem newTableItem(String pathex, int insertPos) {
		if (insertPos < 0 || insertPos >= fileTable.getItemCount())
			insertPos = fileTable.getItemCount();

		String path = pathex.endsWith("//") ? pathex.substring(0, pathex.length() - 2) : pathex;

		File file = new File(path);
		Image icon = getIcon(file);

		TableItem item = new TableItem(fileTable, SWT.NONE, insertPos);
		item.setImage(0, icon);
		item.setText(0, path);

		if (file.isDirectory()) {
			attachSubCheckEditor(item);
			Button bt = (Button)((TableEditor)item.getData(TABLE_EDITOR_KEY)).getEditor();
			if (pathex.endsWith("//")) {
				bt.setSelection(true);
			}
		}

		item.setData(new NormalFileEntry(file));

		return item;
	}

	private void attachSubCheckEditor(TableItem item) {
		TableEditor editor = new TableEditor(fileTable);
		Button subButton = new Button(fileTable, SWT.CHECK);
		subButton.setText(messages.getString("FileDropListViewer.SUBENTRY_BUTTON_TEXT"));
		subButton.setSelection(false);
		subButton.pack();
		subButton.addSelectionListener(FILE_TABLE_CHECKBOX_LISTENER);
		subButton.setData(item);
		editor.minimumWidth = subButton.getSize().x;
		editor.horizontalAlignment = SWT.CENTER;
		editor.setEditor(subButton, item, 1);
		item.setData(TABLE_EDITOR_KEY, editor);
	}

	private void removeItems(int[] indices) {
		removeItemsRaw(indices);
		fileTable.setSelection(indices[0]);
		updateCheckboxLocation(indices[0]);
	}
	private void removeItemsRaw(int[] indices) {
		for (int i : indices) {
			TableEditor editor = (TableEditor)fileTable.getItem(i).getData(TABLE_EDITOR_KEY);
			if (editor != null)
				editor.getEditor().dispose();
		}
		fileTable.remove(indices);
		listChanged = true;
	}

	private void updateCheckboxLocation(int offset) {
		for (int i = offset; i < fileTable.getItemCount(); i++) {
			TableItem item = fileTable.getItem(i);
			TableEditor editor = (TableEditor)item.getData(TABLE_EDITOR_KEY);
			if (editor != null) {
				Control c = editor.getEditor();
				c.setLocation(c.getLocation().x, i * item.getBounds().height);
			}
		}
	}

	private boolean isImageFile(String path) {
		int dotPos = path.lastIndexOf('.');
		if (dotPos < 0) return false;
		String suffix = path.substring(dotPos + 1);
		return ImageIO.getImageReadersBySuffix(suffix).hasNext();
	}

	private Image getIcon(File file) {
		Image icon = null;
		if (!file.exists()) {
			icon = noFileIconImage;
		} else if (file.isDirectory()) {
			icon = emptyFolderIconImage;
		} else if (isImageFile(file.getPath())) {
			icon = imageFileIconImage;
		} else {
			icon = fileIconImage;
		}
		return icon;
	}

	private final SelectionListener FILE_TABLE_SELECTION_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			TableItem item = (TableItem)event.item;
			if (item == appendDirItem) {
				DirectoryDialog dialog = new DirectoryDialog(Display.getDefault().getActiveShell(), SWT.MULTI);
				String path = dialog.open();
				if (path != null) addFile(path);
			} else if (item == appendFileItem) {
				FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.MULTI);
				dialog.open();
				if (dialog.getFileNames() != null)
					addFiles(dialog.getFileNames());
			}
		}
	};

	private final KeyListener FILE_TABLE_KEY_LISTENER = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent event) {
			switch (event.keyCode) {
			case SWT.INSERT:
				DirectoryDialog dialog = new DirectoryDialog(rootComp.getShell());
				String path = dialog.open();
				if (path != null) addFile(path);
				break;
			case SWT.DEL:
				removeItems(fileTable.getSelectionIndices());
				break;
			}
		}
	};

	private final ControlListener FILE_TABLE_RESIZE_LISTENER = new ControlAdapter() {
		@Override
		public void controlResized(ControlEvent event) {
			fileTable.getColumn(0).setWidth(fileTable.getClientArea().width - SUBCHECK_COLUMN_WIDTH);
			fileTable.redraw();
		}
	};

	private final PaintListener FILE_TABLE_PAINT_LISTENER = new PaintListener() {
		@Override
		public void paintControl(PaintEvent event) {
			for (int i = 0; i < fileTable.getItemCount() - 2; i++) {
				TableItem item = fileTable.getItem(i);
				TableEditor editorCtrl = (TableEditor)item.getData(TABLE_EDITOR_KEY);
				if (editorCtrl != null)
					editorCtrl.getEditor().redraw();
			}
		}
	};

	private final Listener FILE_TABLE_MEASURE_ITEM_LISTENER = new Listener() {
		public void handleEvent(Event event) {
			event.height = Math.max(event.height, 40);
		}
	};

	private final Listener FILE_TABLE_ERASE_ITEM_LISTENER = new Listener() {
		public void handleEvent(Event event) {
			event.detail &= ~SWT.FOREGROUND;
		}
	};

	private final Listener FILE_TABLE_PAINT_ITEM_LISTENER = new Listener() {
		public void handleEvent(Event event) {
			if (event.index != 0) return;
			TableItem item = (TableItem)event.item;
			if (item.getImage() == null) return;
			if (item == appendDirItem || item == appendFileItem) {
				GC gc = event.gc;
				gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
				gc.drawImage(item.getImage(), event.x + 4, event.y + 4);
				gc.drawText(item.getText(), event.x + 48, event.y + 20, true);
				return;
			}
			FileEntry fileEntry = (FileEntry)item.getData();
			String props = (String)item.getData(TABLE_PROPS_KEY);
			GC gc = event.gc;
			gc.drawImage(item.getImage(), event.x + 4, event.y + 4);
			gc.drawText(fileEntry.getPath(), event.x + 48, event.y, true);
			gc.drawText(props != null ? props : messages.getString("FileDropListViewer.REPORT_CALCULATING_MESSAGE"), event.x + 48, event.y + 20, true);
		}
	};

	private final DropTargetListener FILE_TABLE_DROP_LISTENER = new DropTargetAdapter() {
		@Override
		public void dragEnter(DropTargetEvent event) {
//			event.detail = DND.DROP_COPY;
		}

		@Override
		public void dragOver(DropTargetEvent event) {
			event.feedback = DND.FEEDBACK_INSERT_BEFORE | DND.FEEDBACK_SCROLL;
		}

		public void drop(DropTargetEvent event) {
			int insertPos = event.item != null && event.item.getData() != null ?
				fileTable.indexOf((TableItem)event.item) :
				fileTable.getItemCount() - 2;
			addFiles((String[])event.data, insertPos);
		}
	};

	private final DragSourceListener FILE_TABLE_DRAG_LISTENER = new DragSourceAdapter() {
		@Override
		public void dragSetData(DragSourceEvent event) {
			TableItem[] items = fileTable.getSelection();
			String[] pathes = new String[items.length];
			for (int i = 0; i < items.length; i++) {
				pathes[i] = new File(((FileEntry)items[i].getData()).getPath()).getAbsolutePath();
			}
			event.data = pathes;
		}
	};

	private final SelectionListener FILE_TABLE_CHANGE_TO_DIR_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			TableItem item = fileTable.getSelection()[0];
			FileEntry entry = (FileEntry)item.getData();
			DirectoryDialog dialog = new DirectoryDialog(rootComp.getShell());
			dialog.setFilterPath(entry.getPath());
			String path = dialog.open();
			if (path != null) {
				File file = new File(path);
				item.setImage(getIcon(file));
				item.setText(path);
				item.setData(new NormalFileEntry(file));
				if (item.getData(TABLE_EDITOR_KEY) == null) {
					attachSubCheckEditor(item);
				}
				Future<?> future = (Future<?>)item.getData(TABLE_FUTURE_KEY);
				if (future != null) future.cancel(true);
				item.setData(
						TABLE_FUTURE_KEY,
						computePropertiesExecutor.submit(new ComputeFilePropertiesTask(item)));
				listChanged = true;
			}
		}
	};

	private final SelectionListener FILE_TABLE_CHANGE_TO_FILE_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			TableItem item = fileTable.getSelection()[0];
			FileEntry entry = (FileEntry)item.getData();
			FileDialog dialog = new FileDialog(rootComp.getShell());
			dialog.setFilterPath(entry.getPath());
			String path = dialog.open();
			if (path != null) {
				File file = new File(path);
				item.setImage(getIcon(file));
				item.setText(path);
				item.setData(new NormalFileEntry(file));
				if (item.getData(TABLE_EDITOR_KEY) != null) {
					TableEditor editor = (TableEditor)item.getData(TABLE_EDITOR_KEY);
					editor.getEditor().dispose();
					editor.dispose();
				}
				Future<?> future = (Future<?>)item.getData(TABLE_FUTURE_KEY);
				if (future != null) future.cancel(true);
				item.setData(
						TABLE_FUTURE_KEY,
						computePropertiesExecutor.submit(new ComputeFilePropertiesTask(item)));
				listChanged = true;
			}
		}
	};

	private final SelectionListener FILE_TABLE_REMOVE_ITEM_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			removeItems(fileTable.getSelectionIndices());
		}
	};

	private final SelectionListener FILE_TABLE_CHECKBOX_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			TableItem item = (TableItem)event.widget.getData();
			Future<?> future = (Future<?>)item.getData(TABLE_FUTURE_KEY);
			if (future != null) future.cancel(true);
			item.setData(
					TABLE_FUTURE_KEY,
					computePropertiesExecutor.submit(new ComputeFilePropertiesTask(item)));
			listChanged = true;
		}
	};

	private class ComputeFilePropertiesTask implements Runnable {
		private final TableItem item;
		private final FileEntry fileEntry;
		private final boolean recursive;

		private ComputeFilePropertiesTask(TableItem item) {
			this.item = item;
			this.fileEntry = (FileEntry)item.getData();
			this.recursive = item.getData(TABLE_EDITOR_KEY) != null ?
					((Button)((TableEditor)item.getData(TABLE_EDITOR_KEY)).getEditor()).getSelection() : false;
		}

		public void run() {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					if (!item.isDisposed()) item.setData(TABLE_PROPS_KEY, null);
				}
			});
			Display.getDefault().syncExec(FILE_TABLE_REDRAW_TASK);
			String text = null;
			try {
				if (fileEntry.isDirectory()) {
					text = computeDirProperties();
				} else if (isImageFile(fileEntry.getPath())) {
					text = computeImageFileProperties();
				} else {
					text = computeFileProperties();
				}
			} catch (Exception e) {
				text = messages.getString("FileDropListViewer.FAILED_CALCULATING_MESSAGE");
			}
			final String text2 = text;
			if (Thread.interrupted()) return;
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					if (!item.isDisposed()) item.setData(TABLE_PROPS_KEY, text2);
				}
			});
			Display.getDefault().syncExec(FILE_TABLE_REDRAW_TASK);
		}

		private String computeFileProperties() {
			File file = new File(fileEntry.getPath());
			if (!file.exists()) return "";
			return getFileSizeDescription(file.length());
		}

		private String computeImageFileProperties() {
			File file = new File(fileEntry.getPath());
			if (!file.exists()) return "";
			Image image = ImageUtils.loadImage(fileEntry);
			StringBuilder sb = new StringBuilder();
			if (image != null) {
				ImageData data = image.getImageData();
				sb.append(data.width).append('x').append(data.height).append(' ');
				sb.append(data.depth).append("bpp").append(' '); //$NON-NLS-1$
			}
			sb.append(getFileSizeDescription(file.length()));
			return sb.toString();
		}

		private String computeDirProperties() {
			File dir = new File(fileEntry.getPath());
			if (!dir.exists()) return "";
			DirProps dirProps = new DirProps();
			for (File child : dir.listFiles()) {
				if (child.isDirectory()) {
					dirProps.numDirs++;
					if (recursive) computeDirProperties(child, dirProps);
				} else {
					dirProps.numFiles++;
					dirProps.length += child.length();
				}
			}
			String text = String.format(
					messages.getString("FileDropListViewer.REPORT_DIRINFO_MESSAGE"),
					dirProps.numDirs, dirProps.numFiles, getFileSizeDescription(dirProps.length));
			return text;
		}

		private void computeDirProperties(File dir, DirProps dirProps) {
			for (File child : dir.listFiles()) {
				if (child.isDirectory()) {
					computeDirProperties(child, dirProps);
				} else {
					dirProps.numFiles++;
					dirProps.length += child.length();
				}
			}
		}
	}

	private static class DirProps {
		int numDirs, numFiles;
		long length;
	}

	private final Runnable FILE_TABLE_REDRAW_TASK = new Runnable() {
		public void run() {
			if (!fileTable.isDisposed()) {
				fileTable.redraw();
			}
		}
	};

	private static final String[] FILE_SIZE_UNIT = {
		"byte", "KB", "MB", "GB" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	};

	private String getFileSizeDescription(long byteSize) {
		int curUnitId = 0;
		if (byteSize < 1000) {
			return byteSize + FILE_SIZE_UNIT[0];
		}
		long viewByteSize = byteSize * 10;
		while (byteSize >> 10 > 0 && curUnitId < FILE_SIZE_UNIT.length - 1) {
			++curUnitId;
			byteSize >>= 10;
			viewByteSize >>= 10;
		}
		return String.format("%d.%d%s", viewByteSize / 10, viewByteSize % 10, FILE_SIZE_UNIT[curUnitId]); //$NON-NLS-1$
	}
}
