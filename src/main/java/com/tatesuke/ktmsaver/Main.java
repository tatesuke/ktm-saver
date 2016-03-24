package com.tatesuke.ktmsaver;

import java.util.Timer;
import java.util.TimerTask;

import org.glassfish.tyrus.server.Server;

import com.tatesuke.ktmsaver.ui.KTMTrayIcon;
import com.tatesuke.ktmsaver.websocket.SaveServerEndPoint;

/**
 * KTMSaverのメイン関数
 * サーバーを起動して、トレイアイコンを表示する。
 * @author tatesuke
 */
public class Main implements KTMTrayIcon.Observer {

	public static void main(String[] args) throws Exception {
		new Main().launch(args);
	}

	private Server server;

	private void launch(String[] args) {
		String port = (1 <= args.length) ? args[0] : "56565";

		KTMTrayIcon trayIcon = new KTMTrayIcon(this);
		trayIcon.show();

		try {
			server = new Server("localhost", Integer.parseInt(port), "/", null,
					SaveServerEndPoint.class);
			server.start();
			trayIcon.setPort(port);
		} catch (Exception e) {
			e.printStackTrace();

			trayIcon.showErrorMessage("The server has not started.\n"
					+ e.getMessage());
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					onExit();
				}
			}, 3000);
		}
	}

	@Override
	public void onExit() {
		server.stop();
		System.exit(0);
	};

}