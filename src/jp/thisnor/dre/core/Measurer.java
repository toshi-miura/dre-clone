package jp.thisnor.dre.core;




public interface Measurer {
	void init(MeasurerPackage mpack);
	Object convert(FileEntry entry) throws Exception;
	int measure(Object data1, Object data2, int threshold);
	void dispose();
}
