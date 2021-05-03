package com.tmax.ck;

import java.io.IOException;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD;

public class WebApp extends NanoHTTPD {

	public WebApp(int port) {
		super(8080);
		System.out.println("Run simple stress application : listen port = 8080");
	}

	@Override
	public Response serve(IHTTPSession session) {
		String responseString = "initial response";
		if (session.getUri().startsWith("/activeThread")) {
			int num = Integer.valueOf(session.getParameters().get("num").get(0));
			int sleep = Integer.valueOf(session.getParameters().get("sleep").get(0));

			for (int i = 0; i < num; i++) {
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(sleep);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				});
				t.start();
			}
			responseString = "active thread";
		} else if (session.getUri().startsWith("/cpu")) {
			int num = Integer.valueOf(session.getParameters().get("num").get(0));
			int loop = Integer.valueOf(session.getParameters().get("loop").get(0));
			String target = session.getParameters().get("target").get(0);

			for (int i = 0; i < num; i++) {
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						for (int i = 0; i < loop; i++) {
							try {
								// // encrypt and decrypt
								// Cipher cipher = Cipher.getInstance("AES");
								// SecretKeySpec secretKeySpec = new SecretKeySpec("happyprogrammer!".getBytes(), "AES");
								// cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
								// byte[] encryptBytes = cipher.doFinal(target.getBytes("UTF-8"));
								// System.out.println(new String(encryptBytes));
								// cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
								// byte[] decryptBytes = cipher.doFinal(encryptBytes);
								// System.out.println(new String(decryptBytes, "UTF-8"));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});
				t.start();
			}
		} else if (session.getUri().startsWith("/heap")) {
			responseString = "heap(not implemented)";
		} else if (session.getUri().startsWith("/stat")) {
			StringBuilder responseBuilder = new StringBuilder();
			responseBuilder.append("active thread count : ").append(Thread.activeCount());
			responseString = responseBuilder.toString();
		} else if (session.getUri().startsWith("/help")) {
			responseString = "not implemented";
		} else {
			responseString = "invalid request, try GET /help";
		}
		return newFixedLengthResponse(responseString);
	}
}