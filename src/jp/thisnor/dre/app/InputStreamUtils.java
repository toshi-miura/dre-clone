package jp.thisnor.dre.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class InputStreamUtils {
	static InputStream open(String path) throws IOException {
		if (Application.isInJar()) {
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
