package jp.thisnor.dre.gui;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class InputStreamUtils {
	static InputStream open(String path) throws IOException {
		if (JarPackaged.jarPackaged) {
			return ClassLoader.getSystemResourceAsStream(path);
		} else {
			return new FileInputStream(path);
		}
	}

	static void close(InputStream in) {
		try {
			if (in != null) in.close();
		} catch (IOException e) {}
	}
}
