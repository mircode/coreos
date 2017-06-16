package os.health.application;

import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.core.api.CoreOS;
import os.health.base.BaseCtrl;
import osgi.enroute.jsonrpc.api.JSONRPC;

/**
 * 登陆模块
 * @author admin
 */
@SuppressWarnings({"rawtypes"})
@Component(name="os.login",property=JSONRPC.ENDPOINT + "=login")
public class LoginCtrl extends BaseCtrl implements JSONRPC  {

	// 用户访问类
	String USER_CLASS="os.moudel.user.provider.UserService";
	
	// 系统内核
	CoreOS coreos;
	@Reference
	void setCoreOS(CoreOS coreos){
		this.coreos=coreos;
	}
	@Override
	public CoreOS getCoreOS() {
		return this.coreos;
	}
	public String login(String username,String password){
		List res=this.coreos.call(USER_CLASS,"queryUser",username);
		if(res.size()>0){
			Map user=(Map)res.get(0);
			String passwd=(String)user.get("password");
			if(password.equals(passwd)){
				HttpSession session=this.getSession();
				
				session.setAttribute("user",user);
				String id=user.get("id").toString();
				String name=user.get("username").toString();
				String role=user.get("role").toString();
				Cookie user_token=new Cookie("user_token",id+":"+name+":"+role);
				user_token.setMaxAge(30*60);
				user_token.setPath(request.getContextPath());
				response.addCookie(user_token);
				 
				return "0";
			}else{
				return "密码错误";
			}
		}else{
			return "用户或邮箱不正确";
		}
	}
	public Object info(){
		HttpSession session=this.getSession();
		Object user=session.getAttribute("user");
		return user;
	}
	public boolean loginout(){
		HttpSession session=this.getSession();
		session.setAttribute("user",null);
		return true;
	}
	@Override
	public Object getDescriptor() throws Exception {
		return "login";
	}
}
