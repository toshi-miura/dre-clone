package jp.thisnor.dre.app;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

class PathInputViewer {
	private Messages messages;

	private Composite rootComp;
	private Text pathText;
	private Button openButton;

	PathInputViewer(DREFrame frame) {
		this.messages = frame.getMessages();
	}

	Control getControl() {
		return rootComp;
	}

	String getPath() {
		return pathText.getText();
	}

	void setPath(String path) {
		pathText.setText(path);
	}

	void addModifyListener(ModifyListener l) {
		pathText.addModifyListener(l);
	}

	void setEnabled(boolean enabled) {
		rootComp.setEnabled(enabled);
		pathText.setEnabled(enabled);
		openButton.setEnabled(enabled);
	}

	void createContents(Composite parent) {
		rootComp = new Composite(parent, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			rootComp.setLayout(l);
		}

		pathText = new Text(rootComp, SWT.BORDER);

		openButton = new Button(rootComp, SWT.PUSH);
		openButton.setText(messages.getString("PathInputViewer.OPEN_BUTTON_TEXT"));
		openButton.addSelectionListener(OPEN_BUTTON_SELECTED);

		pathText.setLayoutData(new FormDataBuilder().left(0).right(openButton).bottom(100).build());
		openButton.setLayoutData(new FormDataBuilder().right(100).bottom(100).build());
	}

	private final SelectionListener OPEN_BUTTON_SELECTED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			String currentPath = getPath();
			if (!new File(currentPath).exists()) {
				int posSep = currentPath.lastIndexOf(File.separatorChar);
				if (posSep < 0) {
//					currentPath = null;
				} else {
					currentPath = currentPath.substring(0, posSep);
				}
			}
			DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell());
			if (currentPath != null) dialog.setFilterPath(currentPath);
			String fpath = dialog.open();
			if (fpath != null) {
				pathText.setText(fpath);
			}
		}
	};
}
