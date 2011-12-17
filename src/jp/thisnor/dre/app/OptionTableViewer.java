package jp.thisnor.dre.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jp.thisnor.dre.core.MeasureOptionEntry;
import jp.thisnor.dre.core.Messages;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

class OptionTableViewer {
	private Map<String, MeasureOptionEntry> optionMap;

	private Messages messages;
	private TableViewer optionTableViewer;

	OptionTableViewer(DREFrame frame) {
		this.messages = frame.getMessages();
	}

	void setOptionMap(Map<String, MeasureOptionEntry> optionMap) {
		this.optionMap = optionMap;
		if (optionTableViewer != null) {
			optionTableViewer.setInput(optionMap);
			optionTableViewer.refresh();
		}
	}

	TableViewer getTableViewer() {
		return optionTableViewer;
	}

	void createContents(Composite parent) {
		optionTableViewer = new TableViewer(parent, SWT.BORDER | SWT.V_SCROLL);
		optionTableViewer.setContentProvider(OPTION_TABLE_CONTENT_PROVIDER);
		optionTableViewer.getTable().addMouseListener(OPTION_TABLE_CELL_SELECTED);
		{
			TableLayout l = new TableLayout();
			l.addColumnData(new ColumnWeightData(70, true));
			l.addColumnData(new ColumnWeightData(30, true));
			optionTableViewer.getTable().setLayout(l);
		}
		optionTableViewer.getTable().setHeaderVisible(true);

		TableViewerColumn keyColumn = new TableViewerColumn(optionTableViewer, SWT.NONE);
		keyColumn.getColumn().setText(messages.getString("OptionTableViewer.COLUMN.KEY"));
		keyColumn.setLabelProvider(OPTION_TABLE_KEY_LABEL_PROVIDER);

		TableViewerColumn valueColumn = new TableViewerColumn(optionTableViewer, SWT.NONE);
		valueColumn.getColumn().setText(messages.getString("OptionTableViewer.COLUMN.VALUE"));
		valueColumn.setLabelProvider(OPTION_TABLE_VALUE_LABEL_PROVIDER);
		valueColumn.setEditingSupport(new OptionTableEditingSupport(optionTableViewer));

		if (optionMap != null) {
			optionTableViewer.setInput(optionMap);
			optionTableViewer.refresh();
		}
	}

	private final IContentProvider OPTION_TABLE_CONTENT_PROVIDER = new ArrayContentProvider() {
		@Override
		public Object[] getElements(Object inputElement) {
			@SuppressWarnings("unchecked")
			Map<String, MeasureOptionEntry> optionMap = (Map<String, MeasureOptionEntry>)inputElement;
			List<MeasureOptionEntry> entryList = new ArrayList<MeasureOptionEntry>(optionMap.size());
			for (Iterator<MeasureOptionEntry> it = optionMap.values().iterator(); it.hasNext(); ) {
				MeasureOptionEntry e = it.next();
				if (!e.isHidden()) entryList.add(e);
			}
			return entryList.toArray();
		}
	};

	private final CellLabelProvider OPTION_TABLE_KEY_LABEL_PROVIDER = new CellLabelProvider() {
		@Override
		public void update(ViewerCell cell) {
			cell.setText(((MeasureOptionEntry)cell.getElement()).getName());
		}
	};

	private final CellLabelProvider OPTION_TABLE_VALUE_LABEL_PROVIDER = new CellLabelProvider() {
		@Override
		public void update(ViewerCell cell) {
			cell.setText(((MeasureOptionEntry)cell.getElement()).getValue());
		}
	};

	private static class OptionTableEditingSupport extends EditingSupport {
		private final TableViewer tableViewer;

		public OptionTableEditingSupport(TableViewer tableViewer) {
			super(tableViewer);
			this.tableViewer = tableViewer;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			MeasureOptionEntry entry = (MeasureOptionEntry)element;
			if (entry.getCandidateList() != null) {
				List<String> candList = entry.getCandidateList();
				String[] cands = candList.toArray(new String[candList.size()]);
				return new ComboBoxCellEditor(tableViewer.getTable(), cands, SWT.DROP_DOWN | SWT.READ_ONLY);
			} else {
				return new TextCellEditor(tableViewer.getTable());
			}
		}

		@Override
		protected Object getValue(Object element) {
			MeasureOptionEntry entry = (MeasureOptionEntry)element;
			if (entry.getCandidateList() != null) {
				return entry.getCandidateList().indexOf(entry.getValue());
			} else {
				return entry.getValue();
			}
		}

		@Override
		protected void setValue(Object element, Object value) {
			MeasureOptionEntry entry = (MeasureOptionEntry)element;
			if (entry.getCandidateList() != null) {
				entry.setValue(entry.getCandidateList().get((Integer)value));
			} else {
				entry.setValue(String.valueOf(value));
			}
			tableViewer.refresh();
		}
	};

	private final MouseListener OPTION_TABLE_CELL_SELECTED = new MouseAdapter() {
		@Override
		public void mouseDown(MouseEvent event) {
			Table t = optionTableViewer.getTable();
			for (int i = 0; i < t.getItemCount(); i++) {
				TableItem item = t.getItem(i);
				for (int j = 0; j < t.getColumnCount(); j++) {
					if (item.getBounds(j).contains(event.x, event.y)) {
						optionTableViewer.editElement(item.getData(), 1);
						return;
					}
				}
			}
		}
	};
}
