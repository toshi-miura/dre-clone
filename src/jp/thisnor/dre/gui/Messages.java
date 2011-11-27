package jp.thisnor.dre.gui;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "lang.messages";

	private final Locale locale;
	private final ResourceBundle resBundle;

	public Messages(Locale locale) {
		this(locale, Messages.class.getClassLoader());
	}

	public Messages(Locale locale, ClassLoader clsLoader) {
		ResourceBundle resBundle = null;
		try {
			resBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, clsLoader);
		} catch (MissingResourceException e) {
			locale = Locale.JAPANESE;
			resBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, clsLoader);
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
