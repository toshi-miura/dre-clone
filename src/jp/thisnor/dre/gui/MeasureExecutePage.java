package jp.thisnor.dre.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

class MeasureExecutePage extends DREPage {
	private DREFrame frame;
	private Messages messages;

	private Composite rootComp;
	private ProgressBar
		loadProgress, measureProgress;
//	private Button pauseButton;
	private Text logText;

	private List<FileEntry> targetFileEntryList;
	private List<FileEntry> sourceFileEntryList;
	private FileEntryMeasurer handler;
	private int numThreads;
	private int threshold;

	private ExecutorService masterExecutor;
	private ExecutorService workerExecutor;
	private volatile List<MeasureEntry> measureEntryList;
	private volatile Object firstEntryData;
	private volatile Map<FileEntry, List<SimilarEntry>> similarMap;
	private volatile CountDownLatch latch;
	private volatile boolean paused;

	MeasureExecutePage(DREFrame frame) {
		this.frame = frame;
		this.messages = frame.getMessages();
	}

	Map<FileEntry, List<SimilarEntry>> getSimilarMap() {
		synchronized (similarMap) {
			return similarMap;
		}
	}

	@Override
	void createContents(Composite parent) {
		rootComp = new SashForm(parent, SWT.VERTICAL);

		Composite progressComp = new Composite(rootComp, SWT.NONE);
		{
			FormLayout l = new FormLayout();
			l.spacing = 8;
			progressComp.setLayout(l);
		}

		Label loadProgressLabel = new Label(progressComp, SWT.NONE);
		loadProgressLabel.setText(messages.getString("MeasureExecutePage.LOAD_PROGRESS"));

		loadProgress = new ProgressBar(progressComp, SWT.NONE);
		loadProgress.setMinimum(0);

		Label measureProgressLabel = new Label(progressComp, SWT.NONE);
		measureProgressLabel.setText(messages.getString("MeasureExecutePage.MEASURE_PROGRESS"));

		measureProgress = new ProgressBar(progressComp, SWT.NONE);
		measureProgress.setMinimum(0);

//		pauseButton = new Button(progressComp, SWT.PUSH);
//		pauseButton.addSelectionListener(PAUSE_BUTTON_SELECTION_LISTENER);

		loadProgressLabel.setLayoutData(new FormDataBuilder().left(0).right(0, 160).top(0).build());
		loadProgress.setLayoutData(new FormDataBuilder().left(loadProgressLabel).right(100).top(0).build());
		measureProgressLabel.setLayoutData(new FormDataBuilder().left(0).right(0, 160).top(loadProgressLabel).build());
		measureProgress.setLayoutData(new FormDataBuilder().left(measureProgressLabel).right(100).top(loadProgressLabel).build());
//		pauseButton.setLayoutData(new FormDataBuilder().left(100, -80).right(100).bottom(100).build());

		logText = new Text(rootComp, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
	}

	@Override
	void activated() {
		frame.setPageTitle(messages.getString("MeasureExecutePage.PAGE_TITLE"));
		frame.setPageDescription(messages.getString("MeasureExecutePage.PAGE_DESCRIPTION"));
		frame.setContent(rootComp);
		frame.setPreviousButtonEnabled(true);
		frame.setNextButtonEnabled(false);

		FileEntrySelectPage fileSelectPage = frame.getPage(FileEntrySelectPage.class);
		targetFileEntryList = fileSelectPage.getTargetFileList();
		sourceFileEntryList = fileSelectPage.getSourceFileList();

		PackageSelectPage packageSelectPage = frame.getPage(PackageSelectPage.class);
		handler = packageSelectPage.getSelectedPackage().getHandler();
		Map<String, OptionEntry> optionMap = new HashMap<String, OptionEntry>(packageSelectPage.getSelectedPackage().getOptionMap());
		numThreads = packageSelectPage.getNumThreads();
		threshold = Integer.valueOf(optionMap.get("threshold").getValue()); //$NON-NLS-1$

		OptionEntry langOption = new OptionEntry("lang");
		langOption.setDefaultValue(frame.getMessages().getLocale().getLanguage());
		optionMap.put("lang", langOption);

		handler.init(optionMap);

		loadProgress.setSelection(0);
		measureProgress.setSelection(0);
		logText.setText("");
//		pauseButton.setText(PAUSE_BUTTON_TEXTS[0]);
//		pauseButton.setEnabled(true);
		paused = false;

		if (targetFileEntryList.size() == 1 && targetFileEntryList.get(0) == null) {
			log(messages.getString("MeasureExecutePage.REPORT_FAILED_NOTARGET_MESSAGE"));
			return;
		}
		if (sourceFileEntryList.size() == 1 && sourceFileEntryList.get(0) == null) {
			log(messages.getString("MeasureExecutePage.REPORT_FAILED_NOSOURCE_MESSAGE"));
			return;
		}

		workerExecutor = Executors.newFixedThreadPool(numThreads);
		masterExecutor = Executors.newSingleThreadExecutor();
		masterExecutor.execute(new MasterTask());
		masterExecutor.shutdown();
	}

	@Override
	void hiddened() {
		if (masterExecutor != null) {
			masterExecutor.shutdownNow();
			masterExecutor = null;
		}
	}

	@Override
	void nextRequested() {
		frame.setActivePage(frame.getPage(SimilarEntrySelectPage.class));
	}

	@Override
	void previousRequested() {
		if (workerExecutor != null) {
			MessageBox msgBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.YES | SWT.NO);
			msgBox.setText(messages.getString("MeasureExecutePage.CONFIRM_ABORT_TITLE"));
			msgBox.setMessage(messages.getString("MeasureExecutePage.CONFIRM_ABORT_MESSAGE"));
			if (msgBox.open() == SWT.NO) return;
		}
		frame.setActivePage(frame.getPage(PackageSelectPage.class));
	}

	@Override
	void dispose() {

	}

	private void incProgress(final ProgressBar bar) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (!bar.isDisposed())
					bar.setSelection(bar.getSelection() + 1);
			}
		});
	}

	private void log(final String text) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (!logText.isDisposed())
					logText.append(text + "\n"); //$NON-NLS-1$
			}
		});
	}

	private class MasterTask implements Runnable {
		public void run() {
			try {
				long t0 = System.nanoTime();
				// ロード準備
				measureEntryList = Collections.synchronizedList(new ArrayList<MeasureEntry>(targetFileEntryList != sourceFileEntryList ? targetFileEntryList.size() + sourceFileEntryList.size() : targetFileEntryList.size()));
				final Set<FileEntry> fileEntrySet = new TreeSet<FileEntry>(FileEntryHashComparator.INSTANCE);
				for (int i = 0; ; i++) {
					while (i >= targetFileEntryList.size()) {
						Thread.sleep(33);
					}
					FileEntry fileEntry = targetFileEntryList.get(i);
					if (fileEntry == null) break;
					MeasureEntry mEntry = new MeasureEntry();
					mEntry.fileEntry = fileEntry;
					mEntry.target = true;
					measureEntryList.add(mEntry);
					fileEntrySet.add(fileEntry);
				}
				if (targetFileEntryList != sourceFileEntryList) {
					for (int i = 0; ; i++) {
						while (i >= sourceFileEntryList.size()) {
							Thread.sleep(33);
						}
						FileEntry fileEntry = sourceFileEntryList.get(i);
						if (fileEntry == null) break;
						if (!fileEntrySet.contains(fileEntry)) {
							MeasureEntry mEntry = new MeasureEntry();
							mEntry.fileEntry = fileEntry;
							mEntry.target = false;
							measureEntryList.add(mEntry);
						}
					}
				}

				// ロード実行
				latch = new CountDownLatch(measureEntryList.size());
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						loadProgress.setMaximum(measureEntryList.size());
					}
				});
				for (int i = 0; i < measureEntryList.size(); i++) {
					workerExecutor.execute(new LoadTask(measureEntryList.get(i)));
				}

				// ウェイト
				latch.await();
				System.out.println(System.nanoTime() - t0);
				t0 = System.nanoTime();

				// ファースト計測準備
				for (int i = 0; i < measureEntryList.size() && firstEntryData == null; i++) {
					firstEntryData = measureEntryList.get(i).data;
				}
				if (firstEntryData == null) {
					throw new RuntimeException(messages.getString("MeasureExecutePage.REPORT_FAILED_NOENTRY_MESSAGE"));
				}

				// ファースト計測実行
				latch = new CountDownLatch(measureEntryList.size());
				for (int i = 0; i < measureEntryList.size(); i++) {
					workerExecutor.execute(new FirstMeasureTask(measureEntryList.get(i)));
				}

				// ウェイト
				latch.await();

				// 全体計測準備
				synchronized (measureEntryList) {
					Collections.sort(measureEntryList, MEASURE_ENTRY_DISTANCE_COMPARATOR);
				}
				similarMap = Collections.synchronizedMap(new HashMap<FileEntry, List<SimilarEntry>>(targetFileEntryList.size() / 2));

				// 全体計測実行
				latch = new CountDownLatch(measureEntryList.size());
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						measureProgress.setMaximum(measureEntryList.size());
					}
				});
				for (int i = 0; i < measureEntryList.size(); i++) {
					if (measureEntryList.get(i).target) {
						workerExecutor.execute(new WholeMeasureTask(i));
					} else if (targetFileEntryList != sourceFileEntryList) {
						latch.countDown();
						incProgress(measureProgress);
					}
				}

				// ウェイト
				latch.await();
				System.out.println(System.nanoTime() - t0);

				log(messages.getString("MeasureExecutePage.REPORT_COMPLETED_MESSAGE"));
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
//						pauseButton.setEnabled(false);
						frame.setNextButtonEnabled(true);
					}
				});
			} catch (Exception e) {
				log(e.getClass().getName() + ": " + e.getMessage()); //$NON-NLS-1$
				log(messages.getString("MeasureExecutePage.REPORT_ABORTED_MESSAGE"));
			}

			workerExecutor.shutdownNow();
			workerExecutor = null;
			measureEntryList = null;
			firstEntryData = null;
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					handler.dispose();
				}
			});
		}
	}

	private class LoadTask implements Runnable {
		private final MeasureEntry mEntry;

		private LoadTask(MeasureEntry mEntry) {
			this.mEntry = mEntry;
		}

		public void run() {
			try {
				while (paused) Thread.sleep(33);
			} catch (InterruptedException e) {
				return;
			}
			try {
				mEntry.data = handler.convert(mEntry.fileEntry);
			} catch (Exception e) {
//				e.printStackTrace();
				log(e.getClass().getName() + ": " + e.getMessage() + " (" + mEntry.fileEntry.getPath() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			incProgress(loadProgress);
			latch.countDown();
		}
	}

	private class FirstMeasureTask implements Runnable {
		private final MeasureEntry mEntry;

		private FirstMeasureTask(MeasureEntry mEntry) {
			this.mEntry = mEntry;
		}

		public void run() {
			if (mEntry.data != null) {
				mEntry.firstDistance = handler.measure(firstEntryData, mEntry.data);
			} else {
				mEntry.firstDistance = Integer.MIN_VALUE;
			}
			latch.countDown();
		}
	}

	private class WholeMeasureTask implements Runnable {
		private final MeasureEntry mEntry;
		private final int mEntryIndex;

		private WholeMeasureTask(int mEntryIndex) {
			this.mEntry = measureEntryList.get(mEntryIndex);
			this.mEntryIndex = mEntryIndex;
		}

		public void run() {
			try {
				while (paused) Thread.sleep(33);
			} catch (InterruptedException e) {
				return;
			}
			if (mEntry.firstDistance >= 0) {
				Object data = mEntry.data;
				List<SimilarEntry> similarList = new ArrayList<SimilarEntry>();
				for (int i = mEntryIndex + 1; i < measureEntryList.size(); i++) {
					MeasureEntry mEntry2 = measureEntryList.get(i);
					if (mEntry2.firstDistance - mEntry.firstDistance >= threshold) break;
					addListIfSimilar(data, mEntry2, similarList);
				}
				for (int i = mEntryIndex - 1; i >= 0; i--) {
					MeasureEntry mEntry2 = measureEntryList.get(i);
					if (mEntry.firstDistance - mEntry2.firstDistance >= threshold) break;
					addListIfSimilar(data, mEntry2, similarList);
				}
				if (!similarList.isEmpty()) {
					Collections.sort(similarList, SIMILAR_ENTRY_COMPARATOR);
					similarMap.put(mEntry.fileEntry, similarList);
				}
			}
			incProgress(measureProgress);
			latch.countDown();
		}

		private void addListIfSimilar(Object myData, MeasureEntry target, List<SimilarEntry> l) {
			FileEntry tarEntry = target.fileEntry;
			Object tarData = target.data;
			if (tarData == null) return;
			int d = handler.measure(myData, tarData);
			if (d < threshold) {
				l.add(new SimilarEntry(tarEntry, d));
			}
		}
	}
/*
	private final SelectionListener PAUSE_BUTTON_SELECTION_LISTENER = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			paused = !paused;
			pauseButton.setText(PAUSE_BUTTON_TEXTS[paused ? 1 : 0]);
			frame.setNextButtonEnabled(paused);
		}
	};
*/
	private static final Comparator<MeasureEntry> MEASURE_ENTRY_DISTANCE_COMPARATOR = new Comparator<MeasureEntry>() {
		public int compare(MeasureEntry o1, MeasureEntry o2) {
			return (o1.firstDistance > o2.firstDistance) ? 1 : -1;
		}
	};

	private static final Comparator<SimilarEntry> SIMILAR_ENTRY_COMPARATOR = new Comparator<SimilarEntry>() {
		public int compare(SimilarEntry o1, SimilarEntry o2) {
			return (o1.distance > o2.distance) ? 1 : -1;
		}
	};

	private static class MeasureEntry {
		private volatile FileEntry fileEntry;
		private volatile boolean target;
		private volatile Object data;
		private volatile int firstDistance;
		public String toString() { return fileEntry.getPath(); }
	}
}
