package com.tatesuke.ktmsaver.websocket;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import net.arnx.jsonic.JSON;

import com.tatesuke.ktmsaver.dto.Request;
import com.tatesuke.ktmsaver.dto.Response;
import com.tatesuke.ktmsaver.service.SaveService;

/**
 * ファイル保存用のウェブソケットの受け口。
 * @author tatesuke
 */
@ServerEndpoint("/save")
public class SaveServerEndPoint {

	private SaveService service = getService();

	protected SaveService getService() {
		return new SaveService();
	}

	@OnMessage
	public String onMessage(String message, Session session) {
		try {
			Request request = JSON.decode(message, Request.class);
			
			Response response;
			switch (request.action) {
			case "SAVE_AS":
				response = service.saveAs(request);
				break;
			case "OVERWRITE":
				response = service.overwrite(request);
				break;
			default:
				response = new Response();
				response.result = Response.Result.ERROR;
				response.message = "unknown action " + request.action;
			}
	
			String result = JSON.encode(response);
			System.out.println(result);
			return result;
		} catch (Exception e) {
			Response response = new Response();
			response.result = Response.Result.ERROR;
			response.message = e.getMessage();
			
			String result = JSON.encode(response);
			System.out.println(result);
			return result;
		}
		
	}
}