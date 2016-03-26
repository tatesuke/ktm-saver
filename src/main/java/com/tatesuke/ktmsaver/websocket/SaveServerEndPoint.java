package com.tatesuke.ktmsaver.websocket;

import java.io.IOException;

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
	private Response response;
	
	@OnMessage
	public void onMessage(String message, Session session) {
		try {
			this.request = JSON.decode(message, Request.class);
			System.out.println("receive\t" + request);
		} catch (Exception e) {
			try {
				e.printStackTrace();
				response = new Response();
				response.result = Response.Result.ERROR;
				response.message = e.getMessage();
				String result = JSON.encode(response);
				System.out.println("return\t" + result);
				session.getBasicRemote().sendText(result);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@OnMessage
	public void onMessage(byte[] data, boolean isLast, Session session) {
		try {
			if (response == null) {
				response = new Response();
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
					System.out.println("return\t" + result);
					session.getBasicRemote().sendText(result);
					session.close();
				}
			}

			service.append(data);

			if (isLast) {
				service.endSave(response);
				String result = JSON.encode(response);
				System.out.println(result);
				session.getBasicRemote().sendText(result);
				session.close();
			}
		} catch (Exception e) {
			try {
				e.printStackTrace();

				response = new Response();
				response.result = Response.Result.ERROR;
				response.message = e.getMessage();
				String result = JSON.encode(response);
				System.out.println("return\t" + result);
				session.getBasicRemote().sendText(result);
				session.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@OnError
	public void onWebSocketError(Throwable cause, Session session)
			throws IOException {
		cause.printStackTrace();
		Response response = new Response();
		response.result = Response.Result.ERROR;
		response.message = "unknown error\n" + cause.getMessage();
		session.getBasicRemote().sendText(JSON.encode(response));
		session.close();
	}

}