package os.network.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import os.network.provider.NetworkImpl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

// 将Network暴露在Http环境中
public class HttpServerExport {

	private Object network;
	public HttpServerExport(Object network){
		this.network=network;
	}
	
	public void start(int port) throws Exception {
		InetSocketAddress addr = new InetSocketAddress(port);
		HttpServer server = HttpServer.create(addr, 100);

		server.createContext("/table", new RequestHandler());
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		System.out.println("Server is listening on port " + port);
	}

	public class RequestHandler implements HttpHandler {
		public void handle(HttpExchange exchange) throws IOException {

			// 请求方式
			String method=exchange.getRequestMethod();
			if (method.equalsIgnoreCase("POST")){
				
				// 读取输入
				BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
				StringBuffer res = new StringBuffer();
				String temp = null;
				while ((temp = reader.readLine()) != null) {
					res.append(temp);
				}
				
				// 执行
				String msg=execute(res.toString());
				
				// 返回执行结果
				exchange.sendResponseHeaders(200, msg.length()); 
				OutputStream out = exchange.getResponseBody();
				out.write(msg.getBytes());
				out.flush();
				exchange.close();

			}
		}
	}

	public String execute(String request){
		// log
				
		Map<String,String> params=getParam(request);
		String method=params.get("method");
		Object[] args=toArray(params.get("params"));

		Method func=null;
		Method[] funcs=network.getClass().getMethods();
		for(Method m:funcs){
			if(m.getName().equals(method)){
				func=m;
				break;
			}
		}
		String response="not find the target function "+method;
		if(func!=null){
			Object res=null;
			try {
				if(!method.equals("call")){
					res = func.invoke(network,args);
				}else{
					// call方法
					if(args.length<2){
						res="error args nums";
					}else{
						if(args.length==2){
							res = func.invoke(network,args[0],args[1]);
						}else{
							List<Object> subArgs=new ArrayList<>();
							for(int i=2;i<args.length;i++){
								subArgs.add(args[i]);
							}
							res = func.invoke(network,args[0],args[1],subArgs.toArray());
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(res!=null) response=res.toString();
		}
		
		return response;
	}
	public static Map<String,String> getParam(String request){
		Map<String,String> params=new HashMap<>();
		String[] args=request.split("&");
		for(String arg:args){
			params.put(arg.split("=")[0],arg.split("=")[1]);
		}
		return params;
	}
	// 反序列化
	public static Object[] toArray(String res) {
		if(res==null) return null;
		return res.split(",");
	}
	
	public static void main(String args[]) throws Exception{
		new HttpServerExport(new NetworkImpl("localhost:8080")).start(8080);
	}
}
