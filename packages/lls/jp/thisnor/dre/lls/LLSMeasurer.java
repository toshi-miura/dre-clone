package jp.thisnor.dre.lls;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import jp.thisnor.dre.core.FileEntry;
import jp.thisnor.dre.core.Measurer;
import jp.thisnor.dre.core.MeasureOptionEntry;

public class LLSMeasurer implements Measurer {
	/* キャッシュ保管場所 */
	private static final File CACHE_DIR = new File("cache");
	private static final File CACHE_FILE = new File("cache/jp.thisnor.dre.lls.cache.sqlite3");
	private static final String TABLE_NAME = "tb_lls_cache";

//	private Messages messages;

	private volatile boolean useCache;
	private volatile int distType;
	private static final int
		DISTTYPE_MANHATTAN = 1,
		DISTTYPE_EUCLIDEAN = 2,
		DISTTYPE_CHEBYSHEV = 0;

	private Queue<CacheEntry> storeCacheQueue;
	private static final int PREF_QUEUE_SIZE = 64;

	private long readTime, grayTime, scaleTime, normalTime;
	private int numImages;

	@Override
	public void init(Map<String, MeasureOptionEntry> optionMap) {
//		messages = new Messages(new Locale(optionMap.get("lang").getValue()), this.getClass().getClassLoader());

		useCache = optionMap.get("useCache").getValue().equals("true");
		if (useCache) {
			initDB();
			storeCacheQueue = new ConcurrentLinkedQueue<CacheEntry>();
		}

		String distTypeStr = optionMap.get("distType").getValue();
		distType = "Manhattan".equals(distTypeStr) ? 1 : "Euclidean".equals(distTypeStr) ? 2 : 0;

		readTime = grayTime = scaleTime = normalTime = 0;
		numImages = 0;
	}

	@Override
	public Object convert(FileEntry fileEntry) throws Exception {
		long t0, t1;

		if (useCache) {
			/* キャッシュが存在すれば、それを利用する */
			LLSData data = loadCache(fileEntry);
			if (data != null) return data;
		}

		/* イメージファイル読み込み */
		t0 = System.nanoTime();
		BufferedImage srcImage = null;
		InputStream in = null;
		try {
			in = fileEntry.open();
			srcImage = ImageIO.read(in);
		} catch (IOException e) {
			throw new IOException(String.format("Failed in reading: %s", fileEntry.getPath()), e);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
		}
		if (srcImage == null) {
			new IOException(String.format("Failed in reading: %s", fileEntry.getPath()));
		}
		t1 = System.nanoTime();
		synchronized (this) { readTime += t1 - t0; }

		int srcWidth = srcImage.getWidth();
		int srcHeight = srcImage.getHeight();

		/* グレースケール化 */
		t0 = System.nanoTime();
		BufferedImage dstImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g2 = dstImage.createGraphics();
		g2.drawImage(srcImage, 0, 0, null);
		g2.dispose();
		t1 = System.nanoTime();
		synchronized (this) { grayTime += t1 - t0; }

		byte[] pixels = new byte[srcWidth * srcHeight];
		dstImage.getRaster().getDataElements(0, 0, srcWidth, srcHeight, pixels);

		/* スケーリング */
		t0 = System.nanoTime();
		/* 幅圧縮 */
		final int wssp = 8, wdsp = srcWidth;
		for (int y = 0; y < srcHeight; y++) {
			int sx = 0, nssdx = wssp, sval = pixels[y * srcWidth] & 0xff;
			int dx = 0, ndsdx = wdsp, dval = 0;
			for (int sdx = 0; sdx < srcWidth * 8; ) {
				if (sdx == nssdx) {
					++sx;
					nssdx += wssp;
					sval = pixels[y * srcWidth + sx] & 0xff;
				}
				dval += sval;
				++sdx;
				if (sdx == ndsdx) {
					pixels[y * srcWidth + dx] = (byte)(dval / wdsp);
					++dx;
					ndsdx += wdsp;
					dval = 0;
				}
			}
		}

		/* 高さ圧縮 */
		final int hssp = 8, hdsp = srcHeight;
		for (int x = 0; x < 8; x++) {
			int sy = 0, nssdy = hssp, sval = pixels[x] & 0xff;
			int dy = 0, ndsdy = hdsp, dval = 0;
			for (int sdy = 0; sdy < srcHeight * 8; ) {
				if (sdy == nssdy) {
					++sy;
					nssdy += hssp;
					sval = pixels[sy * srcWidth + x] & 0xff;
				}
				dval += sval;
				++sdy;
				if (sdy == ndsdy) {
					pixels[dy * 8 + x] = (byte)(dval / hdsp);
					++dy;
					ndsdy += hdsp;
					dval = 0;
				}
			}
		}
		t1 = System.nanoTime();
		synchronized (this) { scaleTime += t1 - t0; }

		/* 正規化 */
		t0 = System.nanoTime();
		int sum = 0, sum2 = 0;
		for (int i = 0; i < 64; i++) {
			int srcPixel = pixels[i] & 0xff;
			sum += srcPixel;
			sum2 += srcPixel * srcPixel;
		}
		int mean = sum / 64;
		int var = (int)Math.sqrt(sum2 / 64 - sum * sum / 4096);
		if (var == 0) var = 1;
		for (int i = 0; i < 64; i++) {
			int dstPixel = ((pixels[i] & 0xff) - mean) * 64 / var;
			if (dstPixel > 127) dstPixel = 127;
			if (dstPixel < -127) dstPixel = -127;
			pixels[i] = (byte)dstPixel;
		}
		t1 = System.nanoTime();
		synchronized (this) { normalTime += t1 - t0; }
		synchronized (this) { numImages ++; }

		/* データオブジェクトを生成 */
		LLSData data = new LLSData(Arrays.copyOf(pixels, 64));

		if (useCache) {
			/* キャッシュに保存する */
//			storeCache(entry, data);
			storeCacheQueue.offer(new CacheEntry(fileEntry, data));
			if (storeCacheQueue.size() >= PREF_QUEUE_SIZE) {
				storeCaches();
			}
		}

		return data;
	}

	@Override
	public int measure(Object o1, Object o2) {
		LLSData data1 = (LLSData)o1;
		LLSData data2 = (LLSData)o2;
		int sum = 0;
		switch (distType) {
		case DISTTYPE_EUCLIDEAN:
			for (int i = 0; i < 64; i++) {
				int d = (int)data1.pixels[i] - (int)data2.pixels[i];
				sum += d * d;
			}
			return (int)Math.sqrt(sum);
		case DISTTYPE_MANHATTAN:
			for (int i = 0; i < 64; i++) {
				sum += Math.abs(data1.pixels[i] - data2.pixels[i]);
			}
			return sum;
		case DISTTYPE_CHEBYSHEV:
			int max = 0;
			for (int i = 0; i < 64; i++) {
				int d = Math.abs(data1.pixels[i] - data2.pixels[i]);
				if (d > max) max = d;
			}
			return max;
		default:
			throw new IllegalStateException();
		}
	}

	@Override
	public void dispose() {
		if (useCache) {
			storeCaches();
		}
		System.out.println("Read time: " + readTime / numImages + " - " + readTime);
		System.out.println("Gray time: " + grayTime / numImages + " - " + grayTime);
		System.out.println("Scale time: " + scaleTime / numImages + " - " + scaleTime);
		System.out.println("Normal time: " + normalTime / numImages + " - " + normalTime);
	}

	private void initDB() {
		try {
			Class.forName("org.sqlite.JDBC");

			if (!CACHE_DIR.exists()) {
				CACHE_DIR.mkdirs();
			}
			if (!CACHE_FILE.exists()) {
				Connection conn = null;
				try {
					conn = DriverManager.getConnection("jdbc:sqlite:" + CACHE_FILE.getPath());
					Statement stat = conn.createStatement();
					stat.execute("CREATE TABLE " + TABLE_NAME + "(date INTEGER, size INTEGER, data BLOB);");
					stat.execute("CREATE INDEX id_date_size on " + TABLE_NAME + "(date, size);");
					conn.close();
				} finally {
					try {
						if (conn != null) conn.close();
					} catch (SQLException e) {}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			useCache = false;
		}
	}

	private LLSData loadCache(FileEntry entry) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + CACHE_FILE.getPath());
			Statement stat = conn.createStatement();
			ResultSet res = stat.executeQuery("SELECT data FROM " + TABLE_NAME + " WHERE date = " + entry.getLastModified() + " AND size = " + entry.getSize() + ";");
			if (!res.next()) return null;
			byte[] data = res.getBytes(1);
			if (res.next()) return null;
			return new LLSData(data);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException e) {}
		}
	}
/*
	private void storeCache(FileEntry entry, SimilarImageData data) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + CACHE_FILE.getPath());
			PreparedStatement stat = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " VALUES(" + entry.getLastModified() + ", " + entry.getSize() + ", ?);");
			stat.setBytes(1, data.pixels);
			stat.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException e) {}
		}
	}
*/
	private void storeCaches() {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + CACHE_FILE.getPath());
			conn.setAutoCommit(false);
			CacheEntry cacheEntry = null;
			while ((cacheEntry = storeCacheQueue.poll()) != null) {
				PreparedStatement stat = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " VALUES(" + cacheEntry.fileEntry.getLastModified() + ", " + cacheEntry.fileEntry.getSize() + ", ?);");
				stat.setBytes(1, cacheEntry.data.pixels);
				stat.executeUpdate();
			}
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (SQLException e) {}
		}
	}

	private static class CacheEntry {
		private final FileEntry fileEntry;
		private final LLSData data;
		private CacheEntry(FileEntry fileEntry, LLSData data) {
			this.fileEntry = fileEntry;
			this.data = data;
		}
	}
}
