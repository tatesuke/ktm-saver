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

	private Request request;
	private boolean isReceiving = false;
	
	@OnMessage
	public void onMessage(String message, Session session) {
		synchronized (this) {
			try {
				if (request != null) {
					throw new IllegalStateException("想定外の順番でメッセージを受信した");
				}
				this.request = JSON.decode(message, Request.class);
				System.out.println(new Date() + "\treceive\t" + request);
			} catch (Exception e) {
				try {
					e.printStackTrace();
					Response response = new Response();
					response.result = Response.Result.ERROR;
					response.message = e.getMessage();
					String result = JSON.encode(response);
					System.out.println(new Date() + "\treturn\t" + result);
					session.getBasicRemote().sendText(result);
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					request = null;
				}
			}
		}
	}

	@OnMessage
	public void onMessage(byte[] data, boolean isLast, Session session) {
		synchronized (this) {
			try {
				Response response = new Response();
				if (!isReceiving) {
					isReceiving = true;
					switch (request.action) {
					case "SAVE_AS":
						service.startSaveAs(request);
						break;
					case "OVERWRITE":
						service.startOverwriteSave(request);
						break;
					default:
						response.result = Response.Result.ERROR;
						response.message = "unknown action " + request.action;
						String result = JSON.encode(response);
						System.out.println(new Date() + "\treturn\t" + result);
						session.getBasicRemote().sendText(result);
						session.close();
					}
				}

				service.append(data);

				if (isLast) {
					service.endSave(response);
					String result = JSON.encode(response);
					System.out.println(new Date() + "\treturn\t" + result);
					session.getBasicRemote().sendText(result);
					request = null;
					isReceiving = false;
				}
			} catch (Exception e) {
				try {
					e.printStackTrace();

					Response response = new Response();
					response.result = Response.Result.ERROR;
					response.message = e.getMessage();
					String result = JSON.encode(response);
					System.out.println(new Date() + "\treturn\t" + result);
					session.getBasicRemote().sendText(result);
					session.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					request = null;
					isReceiving = false;
				}
			}
		}
	}

	@OnError
	public void onWebSocketError(Throwable cause, Session session)
			throws IOException {
		cause.printStackTrace();
		if ((request != null) || !(cause instanceof SocketTimeoutException)) {
			Response response = new Response();
			response.result = Response.Result.ERROR;
			String result = JSON.encode(response);
			response.message = "unknown error\n" + cause.getMessage();
			System.out.println(new Date() + "\treturn\t" + result);
			session.getBasicRemote().sendText(result);
			session.close();
		}
	}

}