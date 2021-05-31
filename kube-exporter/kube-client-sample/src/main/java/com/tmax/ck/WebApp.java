package com.tmax.ck;

import java.util.ArrayList;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;

class WatchUrl {
    public String kubeApiServer;
    public String bearerToken;
    public String cacrt;
    public ArrayList<String> urlList;
}
public class WebApp extends NanoHTTPD {
	private Map<String, KubeExporterServer> exporterServerMap = new HashMap<>();
	
	public ArrayList<KubeExporterServer> kubeExporterServerList = new ArrayList<>();
	
	public WebApp(int port) throws IOException {
		super(port);
		System.out.println("Listening port " + Integer.toString(port));
        start(0, false);
	}

	@Override
	public Response serve(IHTTPSession session) {

		Method method = session.getMethod();
		String uri = session.getUri();
		// String itemIdRequestParameter = session.getParameters().get("itemId").get(0);
		// Map<String, String> parms = session.getParms();
        // if (parms.get("username") == null) {
        //     msg += "<form action='?' method='get'>\n" + "  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        // } else {
        //     msg += "<p>Hello, " + parms.get("username") + "!</p>";
        // }

		System.out.println("Method : " + method);
		System.out.println("URI : " + uri);
		String[] splitedUri = uri.split("/");
		System.out.println("splited URI length : " + splitedUri.length);
		System.out.println("splited URI : ");
		for (String splitedUriComponent : splitedUri) {
			System.out.println(splitedUriComponent);
		}

		Response response;
		if (Method.GET.equals(method)) {
			response = newFixedLengthResponse("Get : Hello!");
		}
		else if (Method.POST.equals(method)) {
			try {
				final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
				String data = map.get("postData");
				// System.out.println("data : \n" + data);
				response = newFixedLengthResponse("data : \n" + data);
				
				GsonBuilder builder = new GsonBuilder();
				builder.setPrettyPrinting();
				Gson gson = builder.create();
				WatchUrl watchUrl = gson.fromJson(data,WatchUrl.class);
				
				data = gson.toJson(watchUrl);
				// System.out.println("WatchUrl Object as string : "+data);
				
				if (!exporterServerMap.containsKey(watchUrl.kubeApiServer)) {
					System.out.println("New server");
					KubeExporterServer server = new KubeExporterServer(watchUrl.kubeApiServer, watchUrl.bearerToken, watchUrl.cacrt);
					server.init();
					server.start();
					for (String url : watchUrl.urlList) {
						server.addResource(url);
					}
					exporterServerMap.put(watchUrl.kubeApiServer, server);
				} else {
					System.out.println("Existing server");
						for (String url : watchUrl.urlList) {
							exporterServerMap.get(watchUrl.kubeApiServer).addResource(url);
						}
				}

			} catch (Throwable t){
					System.out.println("Exception : " + t.toString());
					response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, t.toString());
			}
		}
		else if (Method.DELETE.equals(method)) {
			if( (splitedUri.length == 3 && splitedUri[1].equalsIgnoreCase("server")) || (splitedUri.length == 5 && splitedUri[1].equalsIgnoreCase("server") && splitedUri[3].equalsIgnoreCase("uri")) ) {
				try {
					if (splitedUri.length == 3 && splitedUri[1].equalsIgnoreCase("server")){
						// server 찾아서 삭제
					}

					if (splitedUri.length == 5 && splitedUri[1].equalsIgnoreCase("server") && splitedUri[3].equalsIgnoreCase("uri")) {
						// server 하위에 uri 찾아서 삭제
					}

					response = newFixedLengthResponse("ok");
	
				} catch (Throwable t){
					System.out.println("Exception : " + t.toString());
					response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, t.toString());
				}
			} else {
				response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Request not formatted properly.");
			}

		}
		else {
			response = newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "The requested resource does not exist");
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		return response;
	}
}