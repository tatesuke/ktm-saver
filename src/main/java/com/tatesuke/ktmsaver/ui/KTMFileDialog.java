package com.tatesuke.ktmsaver.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

/**
 * ファイル保存ダイアログ
 * 
 * @author tatesuke
 */
public class KTMFileDialog {

	private JFrame frame;
	private JFileChooser dialog;
	private int selected;

	public KTMFileDialog() {
		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
		} catch (Exception e) {
			// 握りつぶし
		}
		try {
			UIManager.setLookAndFeel("apple.laf.AquaLookAndFeel");
		} catch (Exception e) {
			// 握りつぶし
		}
		try {
			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
			// 握りつぶし
		}

		frame = new JFrame();
		frame.setTitle("KTMSaver Save As...");
		JLabel label = new JLabel("showing KTM savedialog...");
		frame.add(label);
		frame.setUndecorated(true);
		frame.pack();

		dialog = new JFileChooser();
		dialog.setAcceptAllFileFilterUsed(false);
		dialog.addChoosableFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "KTM HTML File(.html)";
			}

			@Override
			public boolean accept(File f) {
				return true;
			}
		});
	}

	/**
	 * ファイル保存ダイアログを表示する。
	 * <p>
	 * ユーザが拡張子を指定しなかった場合、".html"を補う。
	 * </p>
	 * 
	 * @param baseDir
	 *            初期ディレクトリ名(null可能)
	 * @param name
	 *            初期ファイル名（null不可）
	 * @return ユーザが指定したファイル。キャンセルした場合は場合null
	 * @throws InvocationTargetException
	 *             　ダイアログの表示に失敗した場合
	 * @throws InterruptedException
	 *             ダイアログの表示に失敗した場合
	 */
	public File getFile(File baseDir, String name)
			throws InvocationTargetException, InterruptedException {

		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				if (baseDir != null) {
					dialog.setCurrentDirectory(baseDir);
				}

				frame.setVisible(true);
				frame.setAlwaysOnTop(true);
				selected = dialog.showSaveDialog(frame);
				frame.setVisible(false);
			}
		});

		if (selected == JFileChooser.APPROVE_OPTION) {
			File file = dialog.getSelectedFile();
			String ext = getExtension(file.getName());
			if (ext == null) {
				file = new File(file.getParentFile(), file.getName() + ".html");
			}
			return file;
		} else {
			return null;
		}
	}

	private String getExtension(String fileName) {
		String ext = null;
		int dotIndex = fileName.lastIndexOf('.');

		if ((0 < dotIndex) && (dotIndex < fileName.length() - 1)) {
			ext = fileName.substring(dotIndex + 1).toLowerCase();
		}

		return ext;
	}

}
