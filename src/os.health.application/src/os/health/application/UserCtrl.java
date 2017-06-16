package os.health.application;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.core.api.CoreOS;
import os.health.base.BaseCtrl;
import osgi.enroute.jsonrpc.api.JSONRPC;

/**
 * 日志查询模块-控制器
 * @author admin
 */
@SuppressWarnings({"rawtypes"})
@Component(name="os.user",property=JSONRPC.ENDPOINT + "=user")
public class UserCtrl extends BaseCtrl implements JSONRPC  {

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
	// 列表
	public List query(){
		return this.coreos.call(USER_CLASS,"query");
	}
	// 更加ID查询
	public Object queryById(String id){
		return this.coreos.call(USER_CLASS,"queryById",id);
	}
	// 更新
	public Object update(Map param){
		return this.coreos.call(USER_CLASS,"update",param);
	}
	// 重置数据库
	public Object reset(){
		this.coreos.call(USER_CLASS,"reset");
		return true;
	}
	// 删除
	public Object remove(Map param){
		String ids=param.get("ids").toString();
		try{
			for(String id:ids.split(",")){
				// 删除用户
				this.coreos.call(USER_CLASS,"remove",id);
				// 级联删除
				Object cascade=param.get("cascade");
				if(cascade==null||cascade.equals("yes")){
					this.coreos.call(USER_CLASS,"remove_user_fk",id);
				}
			}
			this.log("info","删除用户成功");
			return 0;
		}catch(Exception e){
			e.printStackTrace();
			this.log("error","删除成功失败");
		}
		return 1;
	}

	@Override
	public Object getDescriptor() throws Exception {
		return "user";
	}
}
