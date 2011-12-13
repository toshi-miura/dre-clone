package jp.thisnor.dre.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

public class MeasurerPackage {
	private static final String PACKAGE_DIR = "packages";

	private final String key;
	private final String name;
	private final String version;
	private final String author;
	private final String caption;
	private final String description;
	private final Image image;
	private final Measurer handler;
	private final Map<String, MeasureOptionEntry> optionMap;

	private MeasurerPackage(PackageDescription desc, ClassLoader clsLoader) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		key = desc.key;
		name = desc.name;
		version = desc.version;
		author = desc.author;
		caption = desc.caption;
		description = desc.description;
		image = (desc.image != null) ? new Image(Display.getDefault(), clsLoader.getResourceAsStream(desc.image)) : null;
		handler = clsLoader.loadClass(desc.handlerClsPath).asSubclass(Measurer.class).newInstance();
		optionMap = desc.optionMap;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getAuthor() {
		return author;
	}

	public String getCaption() {
		return caption;
	}

	public String getDescription() {
		return description;
	}

	public Image getImage() {
		return image;
	}

	public Measurer getHandler() {
		return handler;
	}

	public Map<String, MeasureOptionEntry> getOptionMap() {
		return optionMap;
	}

	public static MeasurerPackage importPackage(String packageName) throws IOException {
		try {
			if (new File(PACKAGE_DIR, packageName).isDirectory()) {
				return importDirPackage(new File(PACKAGE_DIR, packageName));
			} else {
				return importJarPackage(new File(PACKAGE_DIR, packageName + ".jar"));
			}
		} catch (Exception e) {
			throw new IOException(String.format("Failed in loading package: %s.", packageName), e);
		}
	}

	public static MeasurerPackage importPackage(File packageFile) throws IOException {
		try {
			if (packageFile.isDirectory()) {
				return importDirPackage(packageFile);
			} else {
				return importJarPackage(packageFile);
			}
		} catch (Exception e) {
			throw new IOException(String.format("Failed in loading package: %s.", packageFile.getPath()), e);
		}
	}

	private static MeasurerPackage importJarPackage(File jar) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		PackageDescription desc = null;
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(jar);
			desc = importPackageDescription(jarFile.getInputStream(jarFile.getEntry("package.xml"))); //$NON-NLS-1$
		} finally {
			try {
				if (jarFile != null) jarFile.close();
			} catch (IOException e) {}
		}
		ClassLoader clsLoader = new JarPackageClassLoader(jar);
		return new MeasurerPackage(desc, clsLoader);
	}

	private static MeasurerPackage importDirPackage(File dir) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		PackageDescription desc = null;
		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(dir.getPath() + File.separator + "package.xml")); //$NON-NLS-1$
			desc = importPackageDescription(in);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
		}
		ClassLoader clsLoader = new DirPackageClassLoader(dir);
		return new MeasurerPackage(desc, clsLoader);
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
		private Map<String, MeasureOptionEntry> optionMap;
	}

	private static PackageDescription importPackageDescription(InputStream in) throws IOException {
		Locale locale = Locale.getDefault();
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
			desc.optionMap = new HashMap<String, MeasureOptionEntry>(8);
			NodeList optionList = doc.getElementsByTagName("option"); //$NON-NLS-1$
			for (int i = 0; i < optionList.getLength(); i++) {
				Element optionElement = (Element)optionList.item(i);
				MeasureOptionEntry option = new MeasureOptionEntry(getNodeValue(optionElement, "key")); //$NON-NLS-1$
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
