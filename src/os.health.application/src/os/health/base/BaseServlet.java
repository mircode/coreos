package os.health.base;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.log.LogService;

import os.core.tools.ReflectUtil;
import osgi.enroute.jsonrpc.api.JSONRPC;
import osgi.enroute.jsonrpc.dto.JSON.JSONRPCError;
import osgi.enroute.jsonrpc.dto.JSON.Response;
import aQute.lib.json.JSONCodec;

/**
 * JSONRCP入口类
 * 接受请求并将结果转发controller
 * 
 * @author admin
 */
@Component(
		service=Servlet.class,
		name="osgi.web.jsonrpc",
		property={
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/jsonrpc/*" // 拦截路径
		})
public class BaseServlet extends HttpServlet {
	
	static final long serialVersionUID = 1L;
	
	// 日志类
	@Reference
	LogService log;
		
	// JSON转换
	static JSONCodec codec=new JSONCodec();

	// 控制器集合
	ConcurrentHashMap<String, JSONRPC> controllers = new ConcurrentHashMap<>();
	
	// 请求入口类
	public void service(HttpServletRequest rq, HttpServletResponse rsp) throws IOException {
		
		String path = rq.getPathInfo();
		if (path == null) {
			rsp.getWriter().println("Missing Controller name in " + rq.getRequestURI());
			rsp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		path=path.replaceAll("^/|/$","").replace(".json","");
		
		try {
			// 通过请求URL定位类和方法
			String clz=path.split("/")[0];
			String method=path.split("/")[1];
			try {
				log.log(LogService.LOG_INFO,"Request " + rq);
				Object ctrl=controllers.get(clz);
				if (ctrl==null) {
					rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
					return;
				}
				// 请求参数
				Map<String,Object> params=new HashMap<>();
				rq.getParameterMap().forEach((key,value)->{
					if(value.length==1){
						params.put(key,value[0]);
					}else if(value.length>1){
						params.put(key,value);
					}else{
						params.put(key,null);
					}
				});
				
				// 初始化函数
				params.put("invoke_method",method);
				ReflectUtil.invoke(ctrl,"init",rq,rsp,params);
				
				// 执行目标方法
				Response result = execute(ctrl,method,params);
				
				log.log(LogService.LOG_INFO,"Result " + result);

				// 返回结果
				OutputStream out = rsp.getOutputStream();
				if(result!=null){
					rsp.setContentType("application/json;charset=UTF-8");
					codec.enc().writeDefaults().to(out).put(result);
				}

				out.close();
			} catch (Exception e) {
				rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
		} catch (Exception e) {
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	public Response execute(Object target,String method,Map<String,Object> params) throws Exception {
		Response response = new Response();
		try {
			Method m=ReflectUtil.search(target.getClass(),method, params);
			if(m!=null){
				response.result=ReflectUtil.invoke(target,m,params);
				return response;
			}else{
				response.error = new JSONRPCError();
				response.error.message = "No such method " + method;
				return response;
			}
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, "JSONRPC exec error on " + params.get("request").toString(), e);
			response.error = new JSONRPCError();
			response.error.message = e.getMessage();
			return response;
		}
	}
	
	// 监控Controller变化
	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	public synchronized void addEndpoint(osgi.enroute.jsonrpc.api.JSONRPC resourceManager, Map<String, Object> map) {
		String name = (String) map.get(JSONRPC.ENDPOINT);
		controllers.put(name, resourceManager);
	}
	public synchronized void removeEndpoint(JSONRPC resourceManager, Map<String, Object> map) {
		String name = (String) map.get(JSONRPC.ENDPOINT);
		controllers.remove(name);
	}
}
