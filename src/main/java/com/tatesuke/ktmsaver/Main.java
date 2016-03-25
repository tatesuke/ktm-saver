package com.tatesuke.ktmsaver;

import java.util.Timer;
import java.util.TimerTask;

import javax.websocket.server.ServerContainer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import com.tatesuke.ktmsaver.ui.KTMTrayIcon;
import com.tatesuke.ktmsaver.websocket.SaveServerEndPoint;

/**
 * KTMSaverのメイン関数 サーバーを起動して、トレイアイコンを表示する。
 * 
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
			server = new Server();
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(Integer.parseInt(port));
			connector.setHost("127.0.0.1");
			server.addConnector(connector);

			ServletContextHandler context = new ServletContextHandler(
					ServletContextHandler.SESSIONS);
			context.setContextPath("/ktmsaver");
			server.setHandler(context);

			ServerContainer wscontainer = WebSocketServerContainerInitializer
					.configureContext(context);
			wscontainer.addEndpoint(SaveServerEndPoint.class);
			wscontainer.setDefaultMaxTextMessageBufferSize(Integer.MAX_VALUE);
			
			server.start();
			server.dump(System.err);
			
			trayIcon.setPort(port);
		} catch (Throwable e) {
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
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

}