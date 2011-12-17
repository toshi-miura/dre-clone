package jp.thisnor.dre.core;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private final Locale locale;
	private final ResourceBundle resBundle;

	public Messages(String baseName, Locale locale) {
		this(baseName, locale, Messages.class.getClassLoader());
	}

	public Messages(String baseName, Locale locale, ClassLoader clsLoader) {
		ResourceBundle resBundle = null;
		try {
			resBundle = ResourceBundle.getBundle(baseName + ".messages", locale, clsLoader);
		} catch (MissingResourceException e) {
			locale = Locale.ENGLISH;
			try {
				resBundle = ResourceBundle.getBundle(baseName + ".messages", locale, clsLoader);
			} catch (MissingResourceException e2) {
			}
		}
		this.locale = locale;
		this.resBundle = resBundle;
	}

	public Locale getLocale() {
		return locale;
	}

	public String getString(String key) {
		try {
			return resBundle.getString(key);
		} catch (Exception e) {
			return '!' + key + '!';
		}
	}
}
