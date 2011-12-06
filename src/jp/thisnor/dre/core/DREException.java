package jp.thisnor.dre.core;

public class DREException extends Exception {
	private static final long serialVersionUID = 3351567726724637798L;

	public DREException() {
		super();
	}

	public DREException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public DREException(String arg0) {
		super(arg0);
	}

	public DREException(Throwable arg0) {
		super(arg0);
	}
}
