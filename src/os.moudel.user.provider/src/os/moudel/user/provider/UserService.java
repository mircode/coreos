package os.moudel.user.provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.core.api.CoreOS;
import os.core.tools.StringUtil;

/**
 * 用户管理
 */
@SuppressWarnings({"unchecked","rawtypes"})
@Component(name = "os.moudel.user",service=UserService.class)
public class UserService {
	
	// 数据库访问类
	String DB_CLASS="os.moudel.db.api.DBase";
	
	// 系统内核
	CoreOS coreos;
	@Reference
	void setCoreOS(CoreOS coreos){
		this.coreos=coreos;
	}
	// 更新添加用户
	public String update(Map map){
		
		String id=map.get("id").toString();
		String username=map.get("username").toString();
		String email=map.get("email").toString();
		
		if(!StringUtil.isEmpty(id)){
			boolean res=this.coreos.call(DB_CLASS,"excute","user",map);
			return res?"0":"更新失败";
		}else{
			int check=checkExits(username,email);
			if(check==1){
				return "用户名已经存在";
			}
			if(check==2){
				return "邮箱已经存在";
			}
			Long userid=-1L;
			if(check==0){
				// 添加时间戳
				DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");
				map.put("time",format.format(new Date()));
				userid=Long.parseLong(this.coreos.call(DB_CLASS,"excute","user",map).toString());
			}
			
			// 创建个人信息记录
			Map param=new HashMap();
			param.put("name",map.get("realname").toString());
			Long fkid=Long.parseLong(this.coreos.call(DB_CLASS,"excute","mn_info",param).toString());
			
			// 创建个人信息记录和登录用户关联关系
			param=new HashMap();
			param.put("userid",userid);
			param.put("fkid",fkid);
			param.put("type","mn_info");
			this.coreos.call(DB_CLASS,"excute","fk_user_mn",param);
			
			return "0";
		}
	}
	// 重置数据库
	public void reset(){
		this.coreos.call(DB_CLASS,"init",true);
	}
	public List query(){
		return this.coreos.call(DB_CLASS,"query","user",new HashMap());
	}
	public Object queryById(Object id){
		return this.coreos.call(DB_CLASS,"queryById","user",id);
	}
	public List queryUser(String name){
		String sql="select * from user where username=? or email=?";
		List param=new ArrayList();
		param.add(name);
		param.add(name);
		List res=this.coreos.call(DB_CLASS,"query",sql,param);
		return res;
	}
	public Object remove(String id){
		try{
			this.coreos.call(DB_CLASS,"deleteByIds","user",id);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	public void remove_user_fk(String id){
		// 清除用户数据
		String sql=String.format("select * from fk_user_mn where userid in (%s) and type='mn_info'",id);
		List<Map> res=this.coreos.call(DB_CLASS,"query",sql,new ArrayList());
		for(int i=0;i<res.size();i++){
			Map map=res.get(i);
			String fkid=map.get("fkid").toString();
			this.coreos.call(DB_CLASS,"deleteByIds","mn_info",fkid);
		}
		// 清除其他关联数据
		sql=String.format("delete from fk_user_mn where userid in (%s)",id);
		this.coreos.call(DB_CLASS,"excute",sql,new ArrayList());
		
		// 清除测量数据
		sql=String.format("delete from bld_fat where userid in (%s)",id);
		this.coreos.call(DB_CLASS,"excute",sql,new ArrayList());
		sql=String.format("delete from bld_oxygen where userid in (%s)",id);
		this.coreos.call(DB_CLASS,"excute",sql,new ArrayList());
		sql=String.format("delete from bld_press where userid in (%s)",id);
		this.coreos.call(DB_CLASS,"excute",sql,new ArrayList());
		sql=String.format("delete from bld_sugar where userid in (%s)",id);
		this.coreos.call(DB_CLASS,"excute",sql,new ArrayList());
		sql=String.format("delete from log where userid in (%s)",id);
		this.coreos.call(DB_CLASS,"excute",sql,new ArrayList());
		
	}
	private int checkExits(String username,String email){
		if(!StringUtil.isEmpty(username)){
			Map param=new HashMap();
			param.put("username",username);
			List res=this.coreos.call(DB_CLASS,"query","user",param);
			if(res.size()>0){
				return 1;
			}
		}
		if(!StringUtil.isEmpty(email)){
			Map param=new HashMap();
			param.put("email",email);
			List res=this.coreos.call(DB_CLASS,"query","user",param);
			if(res.size()>0){
				return 2;
			}
		}
		return 0;
	}
}
