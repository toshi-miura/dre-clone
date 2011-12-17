package jp.thisnor.dre.app;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import jp.thisnor.dre.core.FileEntry;
import jp.thisnor.dre.core.Messages;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class ImagePreviewer {
	private static final String
		PREFS_LOUPE_ZOOM_KEY = "jp.thisnor.dre.core.ImagePreviewer.loupeZoom", //$NON-NLS-1$
		PREFS_LOUPE_RADIUS_KEY = "jp.thisnor.dre.core.ImagePreviewer.loupeRadius"; //$NON-NLS-1$

	private Image image;

	private DREFrame frame;
	private PreferenceStore prefs;
	private Messages messages;

	private Composite rootComp;
	private Canvas canvas;
	private Composite descComp;
	private Label
		titleLabel,
		infoLabel;
	private Button loupeButton;
	private Spinner loupeZoomSpinner, loupeRadiusSpinner;

	private Image viewImage;
	private int viewWidth, viewHeight;
	private int clientWidth, clientHeight;
	private boolean loupeEnabled;
	private int loupeX, loupeY;
	private int loupeWidth, loupeHeight;
	private double loupeZoom;
	private boolean mouseDown;

	private static final int MAX_NUM_CACHED_IMAGES = 16;
	private static class ImageCache {
		private final FileEntry fileEntry;
		private final Image image;
		private ImageCache(FileEntry fileEntry, Image image) {
			this.fileEntry = fileEntry;
			this.image = image;
		}
	}
	private Queue<ImageCache> cachedImageQueue = new LinkedList<ImageCache>();

	ImagePreviewer(DREFrame frame) {
		this.frame = frame;
		this.messages = frame.getMessages();
	}

	void createContents(Composite parent) {
		prefs = frame.getPreferences();
		prefs.setDefault(PREFS_LOUPE_ZOOM_KEY, 2.0);
		prefs.setDefault(PREFS_LOUPE_RADIUS_KEY, 100);

		rootComp = new Composite(parent, SWT.NONE);
		rootComp.setLayout(new FormLayout());

		canvas = new Canvas(rootComp, SWT.NO_BACKGROUND);
		canvas.addPaintListener(PREVIEW_IMAGE_PAINTER);
		canvas.addMouseListener(MOUSE_CLICKED_ON_CANVAS);
		canvas.addMouseListener(MOUSE_DRAGGED_ON_CANVAS);
		canvas.addMouseMoveListener(MOUSE_DRAGGED_ON_CANVAS);

		Composite controlComp = new Composite(rootComp, SWT.NONE);
		controlComp.setLayout(new FormLayout());

		descComp = new Composite(controlComp, SWT.NONE);
		descComp.setLayout(new RowLayout(SWT.VERTICAL));

		titleLabel = new Label(descComp, SWT.NONE);
		infoLabel = new Label(descComp, SWT.NONE);

		Composite loupeComp = new Composite(controlComp, SWT.NONE);
		{
			RowLayout l = new RowLayout(SWT.HORIZONTAL);
			l.spacing = 8;
			l.center = true;
			loupeComp.setLayout(l);
		}

		loupeButton = new Button(loupeComp, SWT.CHECK);
		loupeButton.setText(messages.getString("ImagePreviewer.LOUPE_BUTTON_TEXT"));
		loupeButton.addSelectionListener(LOUPE_SELECTED);

		Composite loupeZoomComp = new Composite(loupeComp, SWT.NONE);
		{
			RowLayout l = new RowLayout(SWT.HORIZONTAL);
			l.center = true;
			loupeZoomComp.setLayout(l);
		}

		Label loupeZoomLabel = new Label(loupeZoomComp, SWT.NONE);
		loupeZoomLabel.setText("x"); //$NON-NLS-1$

		loupeZoomSpinner = new Spinner(loupeZoomComp, SWT.BORDER);
		loupeZoomSpinner.setMinimum(1);
		loupeZoomSpinner.setMaximum(8);
		loupeZoomSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				loupeZoom = ((Spinner)event.widget).getSelection();
				prefs.setValue(PREFS_LOUPE_ZOOM_KEY, loupeZoom);
				canvas.redraw();
			}
		});

		Composite loupeRadiusComp = new Composite(loupeComp, SWT.NONE);
		{
			RowLayout l = new RowLayout(SWT.HORIZONTAL);
			l.center = true;
			loupeRadiusComp.setLayout(l);
		}

		Label loupeRadiusLabel = new Label(loupeRadiusComp, SWT.NONE);
		loupeRadiusLabel.setText("R"); //$NON-NLS-1$

		loupeRadiusSpinner = new Spinner(loupeRadiusComp, SWT.BORDER);
		loupeRadiusSpinner.setMinimum(10);
		loupeRadiusSpinner.setMaximum(999);
		loupeRadiusSpinner.setIncrement(10);
		loupeRadiusSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				int value = ((Spinner)event.widget).getSelection();
				loupeWidth = value * 2;
				loupeHeight = value * 2;
				prefs.setValue(PREFS_LOUPE_RADIUS_KEY, value);
				canvas.redraw();
			}
		});

		descComp.setLayoutData(new FormDataBuilder().left(0).right(loupeComp).top(0).bottom(100).build());
		loupeComp.setLayoutData(new FormDataBuilder().right(100).top(0).bottom(100).build());

		canvas.setLayoutData(new FormDataBuilder().left(0).right(100).top(0).bottom(controlComp).build());
		controlComp.setLayoutData(new FormDataBuilder().left(0).right(100).bottom(100).build());

		setLoupeZoom(prefs.getDouble(PREFS_LOUPE_ZOOM_KEY));
		setLoupeRadius(prefs.getInt(PREFS_LOUPE_RADIUS_KEY));
	}

	void setFileEntry(final FileEntry entry) {
		image = null;
		if (entry == null) {
			titleLabel.setText(""); //$NON-NLS-1$
			infoLabel.setText(""); //$NON-NLS-1$
			canvas.redraw();
			descComp.layout();
		} else {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					image = getImage(entry);
					titleLabel.setText(entry.getPath());
					titleLabel.setToolTipText(entry.getPath());
					StringBuilder infoText = new StringBuilder();
					if (image != null) {
						ImageData data = image.getImageData();
						infoText.append(data.width).append('x').append(data.height).append(' ');
						infoText.append(data.depth).append("bpp").append(' '); //$NON-NLS-1$
					}
					if (!entry.isDirectory()) {
						infoText.append(getFileSizeDescription(entry.getSize()));
					}
					infoLabel.setText(infoText.toString());
					canvas.redraw();
					descComp.layout();
				}
			});
		}
	}

	void preloadImage(FileEntry fileEntry) {
		getImage(fileEntry);
	}

	private Image getImage(FileEntry fileEntry) {
		ImageCache cache = null;
		for (Iterator<ImageCache> it = cachedImageQueue.iterator(); it.hasNext(); ) {
			ImageCache curCache = it.next();
			if (fileEntry.equals(curCache.fileEntry)) {
				it.remove();
				cache = curCache;
				break;
			}
		}
		if (cache == null) {
			if (cachedImageQueue.size() >= MAX_NUM_CACHED_IMAGES) {
				ImageCache disposing = cachedImageQueue.poll();
				if (disposing.image == image) return null;
				disposing.image.dispose();
			}
			Image image = ImageUtils.loadImage(fileEntry);
			cache = new ImageCache(fileEntry, image);
		}
		cachedImageQueue.offer(cache);
		return cache.image;
	}

	void setLoupeEnabled(boolean enabled) {
		loupeEnabled = enabled;
		loupeButton.setSelection(enabled);
		canvas.redraw();
	}

	void setMouseDown(boolean down) {
		mouseDown = down;
	}

	boolean getLoupeEnabled() {
		return loupeEnabled;
	}

	void setLoupePosition(int x, int y) {
		int dirtyLeft = Math.min(loupeX, x) - loupeWidth / 2;
		int dirtyTop = Math.min(loupeY, y) - loupeHeight / 2;
		int dirtyWidth = Math.abs(loupeX - x) + loupeWidth;
		int dirtyHeight = Math.abs(loupeY - y) + loupeHeight;
		if (x >= 0 && x < viewWidth) loupeX = x;
		if (y >= 0 && y < viewHeight) loupeY = y;
		canvas.redraw(dirtyLeft, dirtyTop, dirtyWidth, dirtyHeight, false);
	}

	int getLoupePositionX() {
		return loupeX;
	}

	int getLoupePositionY() {
		return loupeY;
	}

	void setLoupeZoom(double zoom) {
		loupeZoomSpinner.setSelection((int)zoom);
	}

	void setLoupeRadius(int radius) {
		loupeRadiusSpinner.setSelection(radius);
	}

	Control getControl() {
		return rootComp;
	}

	private final PaintListener PREVIEW_IMAGE_PAINTER = new PaintListener() {
		@Override
		public void paintControl(PaintEvent event) {
			GC gc = event.gc;
			Rectangle clientRect = canvas.getClientArea();
			if (image == null || image.isDisposed()) {
				image = null;
				gc.fillRectangle(clientRect.x, clientRect.y, clientRect.width, clientRect.height);
				return;
			}
			if (clientRect.width != clientWidth || clientRect.height != clientHeight) {
				if (viewImage != null) viewImage.dispose();
				clientWidth = clientRect.width;
				clientHeight = clientRect.height;
				viewImage = new Image(Display.getDefault(), clientWidth, clientHeight);
			}

			GC gc2 = new GC(viewImage);
			gc2.fillRectangle(0, 0, clientWidth, clientHeight);

			int imageWidth = image.getImageData().width;
			int imageHeight = image.getImageData().height;
			double clientAspect = (double)clientRect.width / clientRect.height;
			double imageAspect = (double)imageWidth / imageHeight;
			if (clientAspect > imageAspect) {
				viewHeight = clientRect.height;
				viewWidth = (int)(clientRect.height * imageAspect);
			} else {
				viewWidth = clientRect.width;
				viewHeight = (int)(clientRect.width / imageAspect);
			}
			gc2.drawImage(image, 0, 0, imageWidth, imageHeight, 0, 0, viewWidth, viewHeight);

			if (loupeEnabled) {
				double scaleRatio = (double)imageWidth / viewWidth;
				int loupeXOnSrc = (int)(loupeX * scaleRatio);
				int loupeYOnSrc = (int)(loupeY * scaleRatio);
				int widthOnSrc = (int)(loupeWidth / loupeZoom);
				int heightOnSrc = (int)(loupeHeight / loupeZoom);
				int leftOnSrc = loupeXOnSrc - widthOnSrc / 2;
				int topOnSrc = loupeYOnSrc - heightOnSrc / 2;
				if (leftOnSrc < 0) { widthOnSrc -= 0 - leftOnSrc; leftOnSrc = 0; }
				if (topOnSrc < 0) { heightOnSrc -= 0 - topOnSrc; topOnSrc = 0; }
				if (leftOnSrc + widthOnSrc >= imageWidth) widthOnSrc = imageWidth - leftOnSrc;
				if (topOnSrc + heightOnSrc >= imageHeight) heightOnSrc = imageHeight - topOnSrc;
				if (widthOnSrc > 0 && heightOnSrc > 0) {
					int widthOnView = (int)(widthOnSrc * loupeZoom);
					int heightOnView = (int)(heightOnSrc * loupeZoom);
					int leftOnView = (int)((leftOnSrc - loupeXOnSrc) * loupeZoom) + loupeX;
					int topOnView = (int)((topOnSrc - loupeYOnSrc) * loupeZoom) + loupeY;
					Path clippingPath = new Path(Display.getDefault());
					clippingPath.addArc(loupeX - loupeWidth / 2, loupeY - loupeHeight / 2, loupeWidth, loupeHeight, 0, 360);
					gc2.setClipping(clippingPath);
					gc2.fillRectangle(leftOnView, topOnView, widthOnView, heightOnView);
					gc2.drawImage(image, leftOnSrc, topOnSrc, widthOnSrc, heightOnSrc, leftOnView, topOnView, widthOnView, heightOnView);
					clippingPath.dispose();
				}
			}
			gc2.dispose();

			gc.drawImage(viewImage, 0, 0);
		}
	};

	private final MouseListener MOUSE_CLICKED_ON_CANVAS = new MouseAdapter() {
		@Override
		public void mouseDown(MouseEvent event) {
			if (loupeEnabled) {
				setLoupePosition(event.x, event.y);
			}
		}
	};

	private final MouseDragOnCanvasListener MOUSE_DRAGGED_ON_CANVAS = new MouseDragOnCanvasListener();

	private class MouseDragOnCanvasListener implements MouseListener, MouseMoveListener {
		@Override
		public void mouseDoubleClick(MouseEvent event) {
		}

		@Override
		public void mouseDown(MouseEvent event) {
			if (event.button == 1) {
				mouseDown = true;
			}
		}

		@Override
		public void mouseUp(MouseEvent event) {
			if (event.button == 1) {
				mouseDown = false;
			}
		}

		@Override
		public void mouseMove(MouseEvent event) {
			if (loupeEnabled && mouseDown) {
				setLoupePosition(event.x, event.y);
			}
		}
	}

	private final SelectionListener LOUPE_SELECTED = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			setLoupeEnabled(loupeButton.getSelection());
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
