package com.tatesuke.ktmsaver.ui;

import java.awt.AWTException;
import java.awt.FileDialog;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * ファイル保存ダイアログ
 * 
 * @author tatesuke
 */
public class KTMFileDialog {

	private Robot robot;
	private PointerInfo pointerInfo;
	private JFrame frame;
	private FileDialog dialog;

	public KTMFileDialog() {
		frame = new JFrame();
		frame.setTitle("KTMSaver Save As...");
		JLabel label = new JLabel("showing KTM savedialog...");
		frame.add(label);
		 frame.setUndecorated(true);
		frame.pack();
		dialog = new FileDialog(frame, "Save As...", FileDialog.SAVE);
		
		pointerInfo = MouseInfo.getPointerInfo();
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
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
					dialog.setDirectory(baseDir.getAbsolutePath());
				}
				
				dialog.setFile(name);
				
				frame.addWindowListener(new WindowAdapter() {
					@Override
					public void windowOpened(WindowEvent e) {
						if (robot == null) {
							return;
						}
						
						int orgX = pointerInfo.getLocation().x;
						int orgY = pointerInfo.getLocation().y;
						
						Rectangle r = e.getWindow().getBounds();
						int x = r.x + (r.width / 2);
						int y = r.y + (r.height / 2);
						
						robot.mouseMove(x, y);
						robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
						robot.mouseMove(orgX, orgY);
					}
					
					@Override
					public void windowActivated(WindowEvent e) {
						dialog.setVisible(true);
						frame.setVisible(false);
					}
				});
				frame.setVisible(true);
				frame.setAlwaysOnTop(true);
			}
		});
		
		while (frame.isVisible()) {
			Thread.sleep(50);
		}
		
		String dir = dialog.getDirectory();
		String file = dialog.getFile();

		if (file != null) {
			String ext = getExtension(file);
			if (ext == null) {
				file = file + ".html";
			}
		}

		if (dir == null) {
			return null;
		} else {
			return new File(dir, file);
		}
	}

	private String getExtension(String fileName) {
		String ext = null;
		int dotIndex = fileName.lastIndexOf('.');

		if ((dotIndex > 0) && (dotIndex < fileName.length() - 1)) {
			ext = fileName.substring(dotIndex + 1).toLowerCase();
		}

		return ext;
	}

}
