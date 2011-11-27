package jp.thisnor.dre.gui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ImageUtils {
	private ImageUtils() {}

	public static Image loadImage(String path) {
		if (JarPackaged.jarPackaged) {
			InputStream in = null;
			try {
				in = ClassLoader.getSystemResourceAsStream(path);
				if (in == null) return null;
				return new Image(Display.getDefault(), in);
			} catch (SWTException e) {
				return null;
			} finally {
				try {
					if (in != null) in.close();
				} catch (IOException e) {}
			}
		} else {
			try {
				return new Image(Display.getCurrent(), path);
			} catch (SWTException e) {
				return null;
			}
		}
	}

	public static Image loadImage(FileEntry entry) {
		InputStream in = null;
		try {
			in = entry.open();
			return new Image(Display.getCurrent(), in);
		} catch (IOException e) {
			return null;
		} catch (SWTException e) {
			return null;
		} finally {
			InputStreamUtils.close(in);
		}
	}

	public static ImageSize getImageSize(FileEntry file) {
		InputStream in = null;
		try {
			in = file.open();
			ImageInputStream iin = ImageIO.createImageInputStream(in);
			Iterator<ImageReader> it = ImageIO.getImageReaders(iin);
			if (!it.hasNext()) return null;
			ImageReader reader = it.next();
			reader.setInput(iin);
			return new ImageSize(reader.getWidth(0), reader.getHeight(0));
		} catch (IOException e) {
			return null;
		} finally {
			InputStreamUtils.close(in);
		}
	}

	public static class ImageSize {
		public final int width, height;

		public ImageSize(int width, int height) {
			this.width = width;
			this.height = height;
		}
	}
}
