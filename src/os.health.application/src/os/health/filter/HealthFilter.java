package os.health.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import osgi.enroute.http.capabilities.RequireHttpImplementation;
@RequireHttpImplementation
@Component(property = {
		HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX+"=.*jsonrpc.*"
	})
public class HealthFilter implements Filter {
	public static ThreadLocal<HttpServletRequest> local= new ThreadLocal<>();
	@Override
	public void destroy() {
		
	}
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		try {
			HttpServletRequest req = (HttpServletRequest) request;
			HttpServletResponse rep = (HttpServletResponse)response;
			rep.setHeader("Access-Control-Allow-Origin","*");  
			rep.setHeader("Access-Control-Allow-Methods","*");  
			rep.setHeader("Access-Control-Allow-Headers","*");  
			rep.setHeader("Access-Control-Allow-Credentials","true");  
			if(req.getMethod().equals("OPTIONS")){
				rep.setStatus(HttpServletResponse.SC_OK);
				return;
			}
			Object user=req.getSession().getAttribute("user");
			
			// hock
			if(user==null){
				// ≥¢ ‘¥”Cookie∂¡»°÷µ
				String user_token=null;
				Cookie cookies[]=req.getCookies();
				if(cookies!=null){
		           for(Cookie cookie:cookies){
	                 String name=cookie.getName();
	                 String value=cookie.getValue();
	                 if(name.endsWith("user_token")){
	                	 user_token=value;
	                	 break;
	                 }
		           }
		         }
				
				if(user_token!=null){
					Map<String,String> map=new HashMap<String,String>();
					map.put("id",user_token.split(":")[0]);
					map.put("username",user_token.split(":")[1]);
					map.put("role",user_token.split(":")[2]);
					req.getSession().setAttribute("user",map);
				}
			}
			user=req.getSession().getAttribute("user");
			String path=req.getRequestURI();
			if(path!=null&&path.endsWith(".json")){
				if(!path.endsWith("login.json")){
					if(user==null){
						response.getWriter().write("{status:-1}");  
					    return;
				    }
				}
			}else if(path!=null&&path.endsWith("index.html")){
				if(user==null){
					rep.sendRedirect(req.getContextPath()+"/os.health/login.html");
					return;
				}
			}
			
			local.set(req);
			chain.doFilter(request,response);
		}catch(Exception e){
			e.printStackTrace();
		} finally {
			local.set(null);
		}
	}
	@Override
	public void init(FilterConfig config) throws ServletException {
		
	}
}
