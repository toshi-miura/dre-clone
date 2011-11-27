package jp.thisnor.dre.gui;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

class PackageListViewer {
	private static final String DEFAULT_PACKAGE_ICON_PATH = "res/icon/image_multi32.png"; //$NON-NLS-1$

	private FileEntryMeasurerPackage[] packages;

	private Table packageList;

	private Font systemFont, boldFont;
	private Image defaultPackageIcon;

	PackageListViewer(FileEntryMeasurerPackage[] packages) {
		this.packages = packages;
	}

	void createContents(Composite parent) {
		systemFont = Display.getCurrent().getSystemFont();
		FontData systemFontData = systemFont.getFontData()[0];
		boldFont = new Font(Display.getCurrent(), systemFontData.getName(), systemFontData.getHeight(), SWT.BOLD);
		defaultPackageIcon = ImageUtils.loadImage(DEFAULT_PACKAGE_ICON_PATH);

		packageList = new Table(parent, SWT.BORDER | SWT.V_SCROLL | SWT.VIRTUAL);
		packageList.addListener(SWT.MeasureItem, PACKAGE_LIST_MEASURE_ITEM_LISTENER);
		packageList.addListener(SWT.EraseItem, PACKAGE_LIST_ERASE_ITEM_LISTENER);
		packageList.addListener(SWT.PaintItem, PACKAGE_LIST_PAINT_ITEM_LISTENER);
		{
			TableLayout l = new TableLayout();
			l.addColumnData(new ColumnWeightData(100));
			packageList.setLayout(l);
		}

//		TableColumn packageColumn = new TableColumn(packageList, SWT.NONE);
//		packageColumn.setText(PACKAGE_COLUMN_TEXT);
		new TableColumn(packageList, SWT.NONE);

		for (FileEntryMeasurerPackage pack : packages) {
			TableItem item = new TableItem(packageList, SWT.NONE);
			item.setData(pack);
		}
	}

	Table getTable() {
		return packageList;
	}

	FileEntryMeasurerPackage getActivePackage() {
		return (packageList.getSelectionCount() > 0) ? (FileEntryMeasurerPackage)packageList.getSelection()[0].getData() : null;
	}

	private final Listener PACKAGE_LIST_MEASURE_ITEM_LISTENER = new Listener() {
		public void handleEvent(Event event) {
			event.height = Math.max(event.height, 40);
		}
	};

	private final Listener PACKAGE_LIST_ERASE_ITEM_LISTENER = new Listener() {
		public void handleEvent(Event event) {
			event.detail &= ~SWT.FOREGROUND;
		}
	};

	private final Listener PACKAGE_LIST_PAINT_ITEM_LISTENER = new Listener() {
		public void handleEvent(Event event) {
			TableItem item = (TableItem)event.item;
			FileEntryMeasurerPackage pack = (FileEntryMeasurerPackage)item.getData();
			Image icon = pack.getImage() != null ? pack.getImage() : defaultPackageIcon;
			String name = pack.getName();
			String caption = pack.getCaption();
			GC gc = event.gc;
			gc.drawImage(icon, 0, 0, icon.getImageData().width, icon.getImageData().height, event.x + 4, event.y + 4, 32, 32);
			gc.setFont(boldFont);
			gc.drawText(name, event.x + 48, event.y + 0, true);
			gc.setFont(systemFont);
			gc.drawText(caption, event.x + 48, event.y + 20, true);
		}
	};
}
