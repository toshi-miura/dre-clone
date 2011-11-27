package jp.thisnor.dre.bindup.sample;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import jp.thisnor.dre.gui.FileEntry;
import jp.thisnor.dre.gui.FileEntryMeasurer;
import jp.thisnor.dre.gui.OptionEntry;

public class BinDupMeasurer implements FileEntryMeasurer {
	/**
	 * 初期化処理。convertが呼ばれる前に、メインスレッドから一度だけ呼び出される。
	 */
	@Override
	public void init(Map<String, OptionEntry> optionMap) {
	}

	/**
	 * データオブジェクト生成処理。複数のスレッドから並列して呼び出される。
	 * FileEntryのopen/closeメソッドを呼び出すことで、実ファイルかZIPアーカイブ化に関わらずストリームとして使用できる。
	 */
	@Override
	public Object convert(FileEntry entry) throws Exception {
		InputStream in = null;
		try {
			in = entry.open();
			byte[] buffer = new byte[in.available()];
			in.read(buffer);
			return new BinDupData(buffer);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
		}
	}

	/**
	 * 距離計算処理。複数のスレッドから並列して呼び出される。
	 * このメソッドは、距離関数の定義（とくに三角不等式）を満たしている必要がある。
	 * デフォルトのスレッショルドが100なので、一般的な判定の境目がこれに合うように、値をスケーリングするとよい。
	 * また、値が広くばらけるようにすると、比較処理のオーダーが線形に近づき、処理時間をより短くすることができる。
	 */
	@Override
	public int measure(Object data1, Object data2) {
		BinDupData bdData1 = (BinDupData)data1;
		BinDupData bdData2 = (BinDupData)data2;
		if (Arrays.equals(bdData1.buffer, bdData2.buffer)) {
			return 0;
		} else {
			return Math.abs(bdData2.buffer.length - bdData1.buffer.length) + 1;
		}
	}

	/**
	 * 後処理。すべてのmeasureが終わったあと、メインスレッドから一度だけ呼び出される。
	 */
	@Override
	public void dispose() {
	}
}
