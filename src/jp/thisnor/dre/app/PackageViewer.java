package jp.thisnor.dre.app;

import jp.thisnor.dre.core.MeasurerPackage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

class PackageViewer {
	private static final String DEFAULT_PACKAGE_ICON_PATH = "res/icon/image_multi32.png"; //$NON-NLS-1$

	private DREFrame frame;
	private Messages messages;

	private Composite rootComp;
	private Composite descComp;
	private Label nameLabel;
	private Label infoLabel;
	private Text descText;
	private Label packageIcon;

	private OptionTableViewer optionTableViewer;

	private Font boldFont;
	private Image defaultPackageIcon;

	PackageViewer(DREFrame frame) {
		this.frame = frame;
		this.messages = frame.getMessages();
	}

	void createContents(Composite parent) {
		FontData systemFontData = Display.getCurrent().getSystemFont().getFontData()[0];
		boldFont = new Font(Display.getCurrent(), systemFontData.getName(), systemFontData.getHeight(), SWT.BOLD);
		defaultPackageIcon = ImageUtils.loadImage(DEFAULT_PACKAGE_ICON_PATH);

		rootComp = new Composite(parent, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			l.spacing = 8;
			rootComp.setLayout(l);
		}

		descComp = new Composite(rootComp, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			l.spacing = 8;
			descComp.setLayout(l);
		}

		packageIcon = new Label(descComp, SWT.NO_BACKGROUND);

		Composite descTextComp = new Composite(descComp, SWT.NONE);
		descTextComp.setLayout(new FormLayout());
		nameLabel = new Label(descTextComp, SWT.NONE);
		nameLabel.setFont(boldFont);
		infoLabel = new Label(descTextComp, SWT.NONE);
		descText = new Text(descTextComp, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);

		nameLabel.setLayoutData(new FormDataBuilder().left(0).right(100).top(0).build());
		infoLabel.setLayoutData(new FormDataBuilder().left(0).right(100).top(nameLabel).build());
		descText.setLayoutData(new FormDataBuilder().left(0).right(100).top(infoLabel).bottom(0, 100).build());

		packageIcon.setLayoutData(new FormDataBuilder().left(0).right(0, 32).top(0).bottom(0, 32).build());
		descTextComp.setLayoutData(new FormDataBuilder().left(packageIcon).right(100).top(0).bottom(100).build());

		Composite optionTableComp = new Composite(rootComp, SWT.NONE);
		optionTableComp.setLayout(new FormLayout());

		Label optionTableLabel = new Label(optionTableComp, SWT.NONE);
		optionTableLabel.setText(messages.getString("PackageViewer.OPTION_TABLE_CAPTION"));

		optionTableViewer = new OptionTableViewer(frame);
		optionTableViewer.createContents(optionTableComp);

		optionTableLabel.setLayoutData(new FormDataBuilder().left(0).right(100).top(0).build());
		optionTableViewer.getTableViewer().getTable().setLayoutData(new FormDataBuilder().left(0).right(100).top(optionTableLabel).bottom(100).build());

		descComp.setLayoutData(new FormDataBuilder().left(0).right(100).top(0).build());
		optionTableComp.setLayoutData(new FormDataBuilder().left(0).right(100).top(descComp).bottom(100).build());
	}

	Control getControl() {
		return rootComp;
	}

	void setActivePackage(MeasurerPackage handlerPackage) {
		if (handlerPackage == null) {
			nameLabel.setText(""); //$NON-NLS-1$
			infoLabel.setText(""); //$NON-NLS-1$
			descText.setText(""); //$NON-NLS-1$
			packageIcon.setImage(null);
		} else {
			nameLabel.setText(handlerPackage.getName() != null ? handlerPackage.getName() : "(Untitled handler)"); //$NON-NLS-1$
			infoLabel.setText(
					(handlerPackage.getVersion() != null ? "version " + handlerPackage.getVersion() : "") + //$NON-NLS-1$ //$NON-NLS-2$
					", " + //$NON-NLS-1$
					(handlerPackage.getAuthor() != null ? handlerPackage.getAuthor() : "(Unknown author)")); //$NON-NLS-1$
			descText.setText(handlerPackage.getDescription() != null ? handlerPackage.getDescription() : ""); //$NON-NLS-1$
			packageIcon.setImage(handlerPackage.getImage() != null ? handlerPackage.getImage() : defaultPackageIcon);

			optionTableViewer.setOptionMap(handlerPackage.getOptionMap());
		}
		descComp.layout();
	}
}
