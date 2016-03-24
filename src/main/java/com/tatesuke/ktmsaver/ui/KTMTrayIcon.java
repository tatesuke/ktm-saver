package com.tatesuke.ktmsaver.ui;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * トレイアイコン。
 * ユーザに起動していることを知らせる、起動失敗を知らせる、アプリケーション終了させる役割。
 * @author tatesuke
 */
public class KTMTrayIcon {

	private TrayIcon trayIcon;
	private Observer observer;

	public KTMTrayIcon(Observer observer) {
		this.observer = observer;
	}

	public void onExit() {
		observer.onExit();
	}

	public void show() {
		try {
			Image image = ImageIO.read(Thread.currentThread()
					.getContextClassLoader()
					.getResourceAsStream("ktm.png"));
			this.trayIcon = new TrayIcon(image);
			
	        PopupMenu menu = new PopupMenu();
	        MenuItem exitItem = new MenuItem("終了");
	        exitItem.addActionListener((e)-> {
	        	this.onExit();
	        });
	        menu.add(exitItem);
	        trayIcon.setPopupMenu(menu);
			
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FunctionalInterface
	public static interface Observer {
		void onExit();
	}

	public void showErrorMessage(String message) {
		trayIcon.displayMessage("KTMSaver", message, TrayIcon.MessageType.ERROR);		
	}

}
