package com.tatesuke.ktmsaver.websocket;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Date;

import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import net.arnx.jsonic.JSON;

import com.tatesuke.ktmsaver.dto.Request;
import com.tatesuke.ktmsaver.dto.Response;
import com.tatesuke.ktmsaver.service.SaveService;

/**
 * ファイル保存用のウェブソケットの受け口。
 * 
 * @author tatesuke
 */
@ServerEndpoint("/save")
public class SaveServerEndPoint {

	private SaveService service = getService();

	protected SaveService getService() {
		return new SaveService();
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		synchronized (this) {
			try {
				Request request = JSON.decode(message, Request.class);
				System.out.println(new Date() + "\treceive\t" + request);

				switch (request.action) {
				case "SAVE_AS":
					service.startSaveAs(request);
					break;
				case "OVERWRITE":
					service.startOverwriteSave(request);
					break;
				case "CLOSE":
					Response response = new Response();
					service.close(response);

					String result = JSON.encode(response);
					System.out.println(new Date() + "\treturn\t" + result);
					session.getBasicRemote().sendText(result);
					session.close();
					break;
				default:
					response = new Response();
					response.result = Response.Result.ERROR;
					response.message = "unknown action " + request.action;
					result = JSON.encode(response);
					System.out.println(new Date() + "\treturn\t" + result);
					session.getBasicRemote().sendText(result);
					session.close();
				}

			} catch (Exception e) {
				try {
					e.printStackTrace();
					service.close(null);
					Response response = new Response();
					response.result = Response.Result.ERROR;
					response.message = e.getMessage();
					String result = JSON.encode(response);
					System.out.println(new Date() + "\treturn\t" + result);
					session.getBasicRemote().sendText(result);
					session.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	@OnMessage
	public void onMessage(byte[] data, Session session) {
		synchronized (this) {
			try {
				System.out.println(new Date() + "\treceive\t" + data.length + "byte");
				service.append(data);
			} catch (Exception e) {
				try {
					e.printStackTrace();
					service.close(null);
					Response response = new Response();
					response.result = Response.Result.ERROR;
					response.message = e.getMessage();
					String result = JSON.encode(response);
					System.out.println(new Date() + "\treturn\t" + result);
					session.getBasicRemote().sendText(result);
					session.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	@OnError
	public void onWebSocketError(Throwable cause, Session session)
			throws IOException {
		if ((!service.isClosed()) || !(cause instanceof SocketTimeoutException)) {
			cause.printStackTrace();

			service.close(null);

			Response response = new Response();
			response.result = Response.Result.ERROR;
			response.message = "unknown error\n" + cause.getMessage();
			String result = JSON.encode(response);
			System.out.println(new Date() + "\treturn\t" + result);
			session.getBasicRemote().sendText(result);
			session.close();
		}
	}

}