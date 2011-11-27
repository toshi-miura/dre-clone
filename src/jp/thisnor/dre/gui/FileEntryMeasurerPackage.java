package jp.thisnor.dre.gui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class FileEntryMeasurerPackage {
	private final String key;
	private final String name;
	private final String version;
	private final String author;
	private final String caption;
	private final String description;
	private final Image image;
	private final FileEntryMeasurer handler;
	private final Map<String, OptionEntry> optionMap;

	private FileEntryMeasurerPackage(PackageDescription desc, ClassLoader clsLoader) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		key = desc.key;
		name = desc.name;
		version = desc.version;
		author = desc.author;
		caption = desc.caption;
		description = desc.description;
		image = (desc.image != null) ? new Image(Display.getCurrent(), clsLoader.getResourceAsStream(desc.image)) : null;
		handler = clsLoader.loadClass(desc.handlerClsPath).asSubclass(FileEntryMeasurer.class).newInstance();
		optionMap = Collections.unmodifiableMap(desc.optionMap);
	}

	String getKey() {
		return key;
	}

	String getName() {
		return name;
	}

	String getVersion() {
		return version;
	}

	String getAuthor() {
		return author;
	}

	String getCaption() {
		return caption;
	}

	String getDescription() {
		return description;
	}

	Image getImage() {
		return image;
	}

	FileEntryMeasurer getHandler() {
		return handler;
	}

	Map<String, OptionEntry> getOptionMap() {
		return optionMap;
	}

	static FileEntryMeasurerPackage importPackage(DREFrame frame, File packageFile) throws IOException {
		try {
			if (packageFile.isDirectory()) {
				return importDirPackage(frame, packageFile);
			} else {
				return importJarPackage(frame, packageFile);
			}
		} catch (Exception e) {
			throw new IOException(String.format("Failed in loading package: %s.", packageFile.getPath()), e);
		}
	}

	private static FileEntryMeasurerPackage importJarPackage(DREFrame frame, File jar) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		PackageDescription desc = null;
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(jar);
			desc = importPackageDescription(frame, jarFile.getInputStream(jarFile.getEntry("package.xml"))); //$NON-NLS-1$
		} finally {
			try {
				if (jarFile != null) jarFile.close();
			} catch (IOException e) {}
		}
		ClassLoader clsLoader = new JarPackageClassLoader(jar);
		return new FileEntryMeasurerPackage(desc, clsLoader);
	}

	private static FileEntryMeasurerPackage importDirPackage(DREFrame frame, File dir) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		PackageDescription desc = null;
		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(dir.getPath() + File.separator + "package.xml")); //$NON-NLS-1$
			desc = importPackageDescription(frame, in);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
		}
		ClassLoader clsLoader = new DirPackageClassLoader(dir);
		return new FileEntryMeasurerPackage(desc, clsLoader);
	}

	private static class PackageDescription {
		private String key;
		private String name;
		private String version;
		private String author;
		private String caption;
		private String description;
		private String image;
		private String handlerClsPath;
		private Map<String, OptionEntry> optionMap;
	}

	private static PackageDescription importPackageDescription(DREFrame frame, InputStream in) throws IOException {
		Locale locale = frame.getMessages().getLocale();
		try {
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = f.newDocumentBuilder();
			Document doc = db.parse(in);
			PackageDescription desc = new PackageDescription();
			Element infoElement = (Element)doc.getElementsByTagName("information").item(0); //$NON-NLS-1$
			desc.key = getNodeValue(infoElement, "key"); //$NON-NLS-1$
			desc.name = getNodeValue(infoElement, "name", locale); //$NON-NLS-1$
			desc.version = getNodeValue(infoElement, "version"); //$NON-NLS-1$
			desc.author = getNodeValue(infoElement, "author", locale); //$NON-NLS-1$
			desc.caption = getNodeValue(infoElement, "caption", locale); //$NON-NLS-1$
			desc.description = getNodeValue(infoElement, "description", locale); //$NON-NLS-1$
			desc.image = getNodeValue(infoElement, "icon"); //$NON-NLS-1$
			Element classElement = (Element)doc.getElementsByTagName("class").item(0); //$NON-NLS-1$
			desc.handlerClsPath = getNodeValue(classElement, "measurer"); //$NON-NLS-1$
			desc.optionMap = new HashMap<String, OptionEntry>(8);
			boolean hasThreshold = false;
			NodeList optionList = doc.getElementsByTagName("option"); //$NON-NLS-1$
			for (int i = 0; i < optionList.getLength(); i++) {
				Element optionElement = (Element)optionList.item(i);
				OptionEntry option = new OptionEntry(getNodeValue(optionElement, "key")); //$NON-NLS-1$
				option.setName(getNodeValue(optionElement, "name", locale)); //$NON-NLS-1$
				option.setDefaultValue(getNodeValue(optionElement, "default")); //$NON-NLS-1$
				NodeList selectNodes = optionElement.getElementsByTagName("select"); //$NON-NLS-1$
				if (selectNodes.getLength() > 0) {
					List<String> candList = new ArrayList<String>(4);
					NodeList valueNodes = ((Element)selectNodes.item(0)).getElementsByTagName("value"); //$NON-NLS-1$
					for (int j = 0; j < valueNodes.getLength(); j++) {
						candList.add(valueNodes.item(j).getFirstChild().getNodeValue());
					}
					option.setCandidateList(candList);
				}
				desc.optionMap.put(option.getKey(), option);
				if (option.getKey().equals("threshold")) hasThreshold = true; //$NON-NLS-1$
			}
			if (!hasThreshold) {
				OptionEntry thresholdOption = new OptionEntry("threshold"); //$NON-NLS-1$
				thresholdOption.setName(frame.getMessages().getString("FileEntryMeasurerPackage.THRESHOLD"));
				thresholdOption.setDefaultValue("100"); //$NON-NLS-1$
				desc.optionMap.put("threshold", thresholdOption); //$NON-NLS-1$
			}
			return desc;
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (SAXException e) {
			throw new IOException(e);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
 		}
	}

	private static String getNodeValue(Element element, String tagName) {
		NodeList l = element.getElementsByTagName(tagName);
		if (l.getLength() == 0) return null;
		Node n = l.item(0).getFirstChild();
		if (n.getNodeType() != Node.TEXT_NODE) return null;
		return n.getNodeValue();
	}

	private static String getNodeValue(Element element, String tagName, Locale locale) {
		NodeList l = element.getElementsByTagName(tagName);
		if (l.getLength() == 0) return null;
		Node properNode = null;
		for (int i = 0; i < l.getLength(); i++) {
			Node n = l.item(i);
			NamedNodeMap attrMap = n.getAttributes();
			Node langNode = attrMap.getNamedItem("lang");
			if (langNode != null && locale.getLanguage().equals(langNode.getNodeValue())) {
				properNode = n;
				break;
			} else if (properNode == null && langNode == null) {
				properNode = n;
			}
		}
		if (properNode == null) properNode = l.item(0);
		Node textNode = properNode.getFirstChild();
		if (textNode.getNodeType() != Node.TEXT_NODE) return null;
		return textNode.getNodeValue();
	}

	private static class JarPackageClassLoader extends ClassLoader {
		private File file;

		private JarPackageClassLoader(File file) {
			this.file = file;
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(file);
				ZipEntry zipEntry = jarFile.getEntry(name.replace('.', '/') + ".class"); //$NON-NLS-1$
				if (zipEntry == null) throw new IOException(name + ".class"); //$NON-NLS-1$
				InputStream in = jarFile.getInputStream(zipEntry);
				int len = in.available();
				byte[] buf = new byte[len];
				in.read(buf);
				return defineClass(name, buf, 0, buf.length);
			} catch (IOException e) {
				throw new ClassNotFoundException(name, e);
			} finally {
				try {
					if (jarFile != null) jarFile.close();
				} catch (IOException e) {}
			}
		}
	}

	private static class DirPackageClassLoader extends ClassLoader {
		private File dir;

		private DirPackageClassLoader(File dir) {
			this.dir = dir;
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			InputStream in = null;
			try {
				String path = dir.getPath() + File.separator + name.replace('.', File.separatorChar) + ".class"; //$NON-NLS-1$
				in = new BufferedInputStream(new FileInputStream(path));
				int len = in.available();
				byte[] buf = new byte[len];
				in.read(buf);
				return defineClass(name, buf, 0, buf.length);
			} catch (IOException e) {
				throw new ClassNotFoundException(name);
			} finally {
				try {
					if (in != null) in.close();
				} catch (IOException e) {}
			}
		}
	}
}
