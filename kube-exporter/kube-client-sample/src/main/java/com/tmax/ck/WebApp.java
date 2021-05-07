package com.tmax.ck;

import java.io.IOException;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD;

public class WebApp extends NanoHTTPD {

	// public WebApp() {
	// 	this(8080);
	// }
	public WebApp(int port) throws IOException {
		super(port);
		System.out.println("Running WebApp, listening port = " + Integer.toString(port));
        start(0, false);
	}

	@Override
	public Response serve(IHTTPSession session) {
		Response response;
		if (session.getMethod() == Method.GET) {
			// String itemIdRequestParameter = session.getParameters().get("itemId").get(0);
			response = newFixedLengthResponse("Get : Hello, Nano World!");
		}
		else if (session.getMethod() == Method.POST) {
			try {
				final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
				final String data = map.get("postData");
				System.out.println("data : \n" + data);
				response = newFixedLengthResponse("data : \n" + data);
			} catch (IOException | ResponseException e) {
				// handle
				response = newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "The requested resource does not exist");
			}
		}
		else {
			response = newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "The requested resource does not exist");
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		return response;
	}
}