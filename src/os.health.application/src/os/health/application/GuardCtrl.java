package os.health.application;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.core.api.CoreOS;
import os.core.tools.StringUtil;
import os.health.base.BaseCtrl;
import osgi.enroute.jsonrpc.api.JSONRPC;

/**
 * 监护人模块
 * @author admin
 *
 */
@SuppressWarnings("rawtypes")
@Component(name="os.guard",property=JSONRPC.ENDPOINT + "=guard")
public class GuardCtrl extends BaseCtrl implements JSONRPC  {
	// 监护人类
	String GUARD_CLASS="os.moudel.guard.provider.GuardService";
	
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
	// 列表查询
	public List query(Map param){
		// 查询
		String table=(String)param.get("table");
		try{
			List res=this.coreos.call(GUARD_CLASS,"query",table,param);
			this.log("info", getDesc(table)+"列表查询成功");
			return res;
		}catch(Exception e){
			e.printStackTrace();
			this.log("error", getDesc(table)+"列表查询失败");
		}
		return null;
		
	}
	public Object userInfo(Map param){
		String table=(String)param.get("table");
		return this.coreos.call(GUARD_CLASS,"userInfo",table,param);
	}
	
	// 编辑查询
	public Object queryById(String table,String id){
		Object res=null;
		try{
			res=this.coreos.call(GUARD_CLASS,"queryById",table,id);
			this.log("info", getDesc(table)+"对象查询成功");
		}catch(Exception e){
			e.printStackTrace();
			this.log("error", getDesc(table)+"对象查询失败");
		}
		return res;
	}
	// 编辑或添加
	public Object update(Map params){
		String table=params.get("table").toString();
		String id=params.get("id").toString();
		String info=!StringUtil.isEmpty(id)?"添加":"编辑";
		Object res=null;
		try{
			res=this.coreos.call(GUARD_CLASS,"update",table,params);
			
			// 添加操作 创建关联关系
			if(StringUtil.isEmpty(id)){
				Object userid=params.get("userid");
				if(userid!=null){
					id=res.toString();
					add_user_fk(table,userid.toString(),id);	
				}
			}
			this.log("info", getDesc(table)+info+"成功");
		}catch(Exception e){
			e.printStackTrace();
			this.log("error", getDesc(table)+info+"失败");
		}
		return res;
	}
	// 删除
	public Object remove(Map params){
		String table=params.get("table").toString();
		String ids=params.get("ids").toString();
		Object res=null;
		try{
			for(String id:ids.split(",")){
				Object cascade=params.get("cascade");
				// 级联删除
				if(cascade!=null&&cascade.equals("yes")){
					res=this.coreos.call(GUARD_CLASS,"remove",table,id);
					remove_user_fk(table,null,id);
				}else{
					String userid=params.get("userid").toString();
					remove_user_fk(table,userid,id);
				}
			}
			this.log("info", getDesc(table)+"删除对象成功");
		}catch(Exception e){
			e.printStackTrace();
			this.log("error", getDesc(table)+"删除对象失败");
		}
		return res;
	}
	// 查询未添加列表
	public List query_noadd(String table,String userid){
		List res=null;
		try{
			res=this.coreos.call(GUARD_CLASS,"query_noadd",table,userid);
			this.log("info", getDesc(table)+"查询未添加列表成功");
		}catch(Exception e){
			e.printStackTrace();
			this.log("error", getDesc(table)+"查询未添加列表失败");
		}
		return res;
	}
	// 添加关联关系
	public boolean add_user_fk(String table,String userid,String ids){
		boolean res=false;
		try{
			res=this.coreos.call(GUARD_CLASS,"add_user_fk",table,userid,ids);
			this.log("info", getDesc(table)+"添加关联关系成功");
		}catch(Exception e){
			e.printStackTrace();
			this.log("error", getDesc(table)+"添加关联关系失败");
		}
		return res;
	}
	// 删除广联关系
	public boolean remove_user_fk(String table,String userid,String ids){
		boolean res=false;
		try{
			res=this.coreos.call(GUARD_CLASS,"remove_user_fk",table,userid,ids);
		this.log("info", getDesc(table)+"删除关联关系成功");
		}catch(Exception e){
			e.printStackTrace();
			this.log("error", getDesc(table)+"删除关联关系失败");
		}
		return res;
	}

	// 描述信息
	private String getDesc(String table){
		String info="监护人";
		if(table.equals("mn_info")){
			info="个人";
		}else if(table.equals("mn_doctor")){
			info="医生";
		}else if(table.equals("mn_relatives")){
			info="亲属";
		}else if(table.equals("mn_volunteer")){
			info="志愿者";
		}
		return info;
	}
	@Override
	public Object getDescriptor() throws Exception {
		return "guard";
	}
	

}
