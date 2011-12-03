package jp.thisnor.dre.core;

public interface PathFilter {
	boolean accept(String path);

	static final PathFilter DEFAULT = new PathFilter() {
		@Override
		public boolean accept(String path) {
			return true;
		}
	};
}
