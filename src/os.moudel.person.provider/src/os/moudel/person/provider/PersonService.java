package os.moudel.person.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.core.api.CoreOS;
import os.core.tools.HostUtil;

/**
 * 个人体征模块
 */
@Component(name = "os.moudel.person",service=PersonService.class)
@SuppressWarnings({"rawtypes","unchecked"})
public class PersonService {
	
	// 数据库访问类
	String DB_CLASS="os.moudel.db.api.DBase";
	
	CoreOS coreos=null;
	@Reference void setCoreOS(CoreOS coreos){
		this.coreos=coreos;
	}
	public List query(String table,Map param) {
		return this.coreos.call(DB_CLASS,"query",table,param);
	}
	public Object update(String table,Map param){
		return this.coreos.call(DB_CLASS,"excute",table,param);
	}
	// 实验测试使用
	public List list(String cmd){
		if(cmd.equals("cmd:tz")){
			List res=debug("bld_fat");
			return res;
		}
		return null;
	}
	public List debug(String table){
		Map where=new HashMap<>();
		List list=this.coreos.call(DB_CLASS,"query",table,where,"time desc","10");
		
		
		// 追加主机信息
		if(list==null){
			list=new ArrayList<>();
		}
		Map<String,String> host=new HashMap<>();
		host.put("ip:port",HostUtil.address()+":"+HostUtil.port());
		list.add(host);
		
		return list;
	}
	
}
