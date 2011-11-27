package jp.thisnor.dre.gui;

import java.io.File;
import java.util.Set;
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

class DisposeExecutePage extends DREPage {
	private DREFrame frame;
	private Messages messages;

	private Composite rootComp;
	private Label disposeProgressLabel;
	private ProgressBar disposeProgress;
	private Text logText;

	private Set<FileEntry> targetFileSet;
	private int disposeWay;
	private File dirMoveTo;

	private ExecutorService executor;
	private CountDownLatch disposeLatch;

	DisposeExecutePage(DREFrame frame) {
		this.frame = frame;
		this.messages = frame.getMessages();
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

		disposeProgressLabel = new Label(progressComp, SWT.NONE);

		disposeProgress = new ProgressBar(progressComp, SWT.NONE);
		disposeProgress.setMinimum(0);

		disposeProgressLabel.setLayoutData(new FormDataBuilder().left(0).right(0, 160).top(0).build());
		disposeProgress.setLayoutData(new FormDataBuilder().left(disposeProgressLabel).right(100).top(0).build());

		logText = new Text(rootComp, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
	}

	@Override
	void activated() {
		frame.setPageTitle(messages.getString("DisposeExecutePage.PAGE_TITLE"));
		frame.setPageDescription(messages.getString("DisposeExecutePage.PAGE_DESCRIPTION"));
		frame.setContent(rootComp);
		frame.setPreviousButtonEnabled(true);
		frame.setNextButtonEnabled(false);

		disposeProgressLabel.setText(messages.getString("DisposeExecutePage.DISPOSE_WAY_TEXT." + (disposeWay == 0 ? "DELETE" : "MOVE")));

		targetFileSet = frame.getPage(SimilarEntrySelectPage.class).getCheckedFileSet();
		DisposeWaySelectPage disposeWayPage = frame.getPage(DisposeWaySelectPage.class);
		disposeWay = disposeWayPage.getSelectedDisposeWay();
		dirMoveTo = new File(disposeWayPage.getDirectoryPathMoveTo());

		disposeProgress.setMaximum(targetFileSet.size());
		disposeProgress.setSelection(0);
		logText.setText(""); //$NON-NLS-1$

		int numThreads = frame.getPage(PackageSelectPage.class).getNumThreads();
		executor = Executors.newFixedThreadPool(numThreads + 1);
		disposeLatch = new CountDownLatch(targetFileSet.size());

		executor.execute(new WaitFinishTask());
		switch (disposeWay) {
		case 0:
			for (FileEntry file : targetFileSet) {
				executor.execute(new FileDeleteTask(file));
			}
			break;
		case 1:
			if (!dirMoveTo.exists()) {
				MessageBox msgBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.YES | SWT.NO);
				msgBox.setText(messages.getString("DisposeExecutePage.CONFIRM_MKDIR_TITLE"));
				msgBox.setMessage(messages.getString("DisposeExecutePage.CONFIRM_MKDIR_MESSAGE"));
				if (msgBox.open() == SWT.NO) {
					executor.shutdownNow();
					return;
				}
				if (!dirMoveTo.mkdirs()) {
					logText.setText(String.format(messages.getString("DisposeExecutePage.REPORT_FAILED_MKDIR_MESSAGE"), dirMoveTo.getPath()));
					executor.shutdownNow();
					return;
				}
			}
			for (FileEntry file : targetFileSet) {
				executor.execute(new FileMoveTask(file));
			}
			break;
		}
		executor.shutdown();
	}

	@Override
	void hiddened() {
	}

	@Override
	void nextRequested() {
	}

	@Override
	void previousRequested() {
		if (!executor.isTerminated()) {
			MessageBox msgBox = new MessageBox(Display.getCurrent().getActiveShell(), SWT.YES | SWT.NO);
			msgBox.setText(messages.getString("DisposeExecutePage.CONFIRM_ABORT_TITLE"));
			msgBox.setMessage(messages.getString("DisposeExecutePage.CONFIRM_ABORT_MESSAGE"));
			if (msgBox.open() == SWT.NO) return;
			executor.shutdownNow();
			while (!executor.isTerminated()) {
				try {
					Thread.sleep(33);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		frame.setActivePage(frame.getPage(DisposeWaySelectPage.class));
	}

	@Override
	void dispose() {

	}

	private void incProgress() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (!disposeProgress.isDisposed())
					disposeProgress.setSelection(disposeProgress.getSelection() + 1);
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

	private class FileDeleteTask implements Runnable {
		private final FileEntry file;
		private FileDeleteTask(FileEntry file) {
			this.file = file;
		}
		public void run() {
			if (file instanceof NormalFileEntry) {
				File f = new File(((NormalFileEntry)file).getPath());
				if (!f.delete()) {
					log(String.format(messages.getString("DisposeExecutePage.REPORT_FAILED_DELETE_MESSAGE"), file.getPath()));
				}
			}
			disposeLatch.countDown();
			incProgress();
		}
	}

	private class FileMoveTask implements Runnable {
		private final FileEntry file;
		private FileMoveTask(FileEntry file) {
			this.file = file;
		}
		public void run() {
			if (file instanceof NormalFileEntry) {
				File f = new File(((NormalFileEntry)file).getPath());
				File newF = new File(dirMoveTo, f.getName());
				if (!f.renameTo(newF)) {
					int dotPos = newF.getName().lastIndexOf('.');
					String name = (dotPos >= 0) ? newF.getName().substring(0, dotPos) : newF.getName();
					String ext = (dotPos >= 0) ? newF.getName().substring(dotPos) : ""; //$NON-NLS-1$
					boolean succeeded = false;
					for (int i = 1; i < 1000; i++) {
						newF = new File(dirMoveTo, String.format("%s(%3d)%s", name, i, ext)); //$NON-NLS-1$
						if (f.renameTo(newF)) {
							succeeded = true;
							break;
						}
					}
					if (!succeeded) {
						log(String.format(messages.getString("DisposeExecutePage.REPORT_FAILED_MOVE_MESSAGE"), file.getPath()));
					}
				}
			}
			disposeLatch.countDown();
			incProgress();
		}
	}

	private class WaitFinishTask implements Runnable {
		public void run() {
			try {
				disposeLatch.await();
				log(messages.getString("DisposeExecutePage.REPORT_COMPLETED_MESSAGE"));
			} catch (InterruptedException e) {
				log(messages.getString("DisposeExecutePage.REPORT_ABORTED_MESSAGE"));
			}
		}
	}
}
