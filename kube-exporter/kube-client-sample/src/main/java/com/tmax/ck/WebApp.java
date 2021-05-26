package com.tmax.ck;

import java.util.ArrayList;
import java.io.IOException;
import java.util.HashMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;

class WatchUrl {
    public String kubeApiServer;
    public String bearerToken;
    public String cacrt;
    public ArrayList<String> urlList;
}
public class WebApp extends NanoHTTPD {
	public ArrayList<KubeExporterServer> kubeExporterServerList = new ArrayList<>();
	
	public WebApp(int port) throws IOException {
		super(port);
		System.out.println("Listening port " + Integer.toString(port));
        start(0, false);
	}

	@Override
	public Response serve(IHTTPSession session) {
		Response response;
		if (session.getMethod() == Method.GET) {
			// String itemIdRequestParameter = session.getParameters().get("itemId").get(0);
			response = newFixedLengthResponse("Get : Hello!");
		}
		else if (session.getMethod() == Method.POST) {
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
				
				System.out.println("kubeExporterServerList servers : ");
				for (KubeExporterServer existingServer : kubeExporterServerList) {
					System.out.println(existingServer.getKubeApiServer());
				}

				Boolean isExistingServer = false;

				// TODO: 기존 server list에 이미 있던거면 addresource만 하고
				// 새로운 거면 추가해주면서 server init start
				for (KubeExporterServer existingServer : kubeExporterServerList) {
					if (existingServer.getKubeApiServer().equals(watchUrl.kubeApiServer) && existingServer.getBearerToken().equals(watchUrl.bearerToken) && existingServer.getCacrt().equals(watchUrl.cacrt)) {
						isExistingServer = true;
						System.out.println("Existing server");
						for (String url : watchUrl.urlList) {
							existingServer.addResource(url);
						}
						break;
					}
				}
				if(!isExistingServer){
					System.out.println("New server");
					KubeExporterServer server = new KubeExporterServer(watchUrl.kubeApiServer, watchUrl.bearerToken, watchUrl.cacrt);
					kubeExporterServerList.add(server);
					System.out.println("server added to server list");
					server.init();
					server.start();
					for (String url : watchUrl.urlList) {
						server.addResource(url);
					}
				}
				// server.addResource("/api/v1/namespaces/kube-system/pods");
				// server.addResource("/api/v1/namespaces/kube-system/services");
				// server.addResource("/apis/apps/v1/namespaces/kube-system/deployments");
				// server.addResource("/apis/apiextensions.k8s.io/v1/namespaces/default/customresourcedefinitions");
			} catch (Throwable t){
					System.out.println("Exception : " + t.toString());
					response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, t.toString());
				if(t instanceof Exception){
					// if(t instanceof IOException){
					// 	// handle this exception type
					// } else if (t instanceof AnotherExceptionType){
					// 	//handle this one
					// } else {
					// 	// We didn't expect this Exception. What could it be? Let's log it, and let it bubble up the hierarchy.
					// }
				} else if (t instanceof Error){
					// if(t instanceof IOError){
					// 	// handle this Error
					// } else if (t instanceof AnotherError){
					// 	//handle different Error
					// } else {
					// 	// We didn't expect this Error. What could it be? Let's log it, and let it bubble up the hierarchy.
					// }
				} else {
					// // This should never be reached, unless you have subclassed Throwable for your own purposes.
					// throw t;
				}
			}
			// } catch(Exception e){
			// 	System.out.println("Exception : "+e.toString());
			// 	response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.toString());
			// }
			// } catch (IOException | ResponseException | JsonIOException | IllegalStateException e) {
			// 	// handle
			// 	response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.toString());
			// }
		}
		else {
			response = newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "The requested resource does not exist");
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		return response;
	}
}