package os.moudel.guard.provider;

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
 * 监护人管理
 */
@Component(name = "os.moudel.guard",service=GuardService.class)
@SuppressWarnings({"unchecked","rawtypes"})
public class GuardService {
	
	// 数据库访问类
	String DB_CLASS="os.moudel.db.api.DBase";
		
	CoreOS coreos=null;
	@Reference void setCoreOS(CoreOS coreos){
		this.coreos=coreos;
	}
	public List query(String table,Map args){
		// 查询表
		if(StringUtil.isEmpty(table)){
			return null;
		}
		// 查询参数
		String name=getParam(args,"name");
		String start=getParam(args,"start");
		String end=getParam(args,"end");
		String phone=getParam(args,"phone");
		String userid=getParam(args,"userid");
		
		// 创建查询SQL
		String sql="select t.* from "+table+" t left join fk_user_mn fk on fk.fkid=t.id and fk.type='"+table+"' where 1=1";
		List<String> param=new ArrayList<>();
		if(!StringUtil.isEmpty(name)){
			sql+=" and name=?";
			param.add(name);
		}
		if(!StringUtil.isEmpty(start)){
			sql+=" and time>=?";
			param.add(start);
		}
		if(!StringUtil.isEmpty(end)){
			sql+=" and time<=?";
			param.add(end);
		}
		if(!StringUtil.isEmpty(phone)){
			sql+=" and phone=?";
			param.add(phone);
		}
		if(!StringUtil.isEmpty(userid)){
			sql+=" and userid=?";
			param.add(userid);
		}
		sql+=" order by time desc";
		return this.coreos.call(DB_CLASS,"query",sql,param);
	}
	public Object userInfo(String table,Map args){
		
		Map info=new HashMap();
		String userid=args.get("userid").toString();
		List res=this.query(table, args);
		if(res!=null&&res.size()>0){
			info.putAll((Map)res.get(0));
		}
		
		// 最近一次血压,血糖,血氧,体脂值
		String sql=String.format("select * from %s where userid=%s order by time desc limit 0,1","bld_fat",userid);
		List r=this.coreos.call(DB_CLASS,"query",sql);
		if(r!=null&&r.size()>0){
			Map map=(Map)r.get(0);
			info.put("fat",map.get("zfhl"));
		}
					
		sql=String.format("select * from %s where userid=%s order by time desc limit 0,1","bld_oxygen",userid);
		r=this.coreos.call(DB_CLASS,"query",sql);
		if(r!=null&&r.size()>0){
			Map map=(Map)r.get(0);
			info.put("oxygen",map.get("zfhl"));
		}
		sql=String.format("select * from %s where userid=%s order by time desc limit 0,1","bld_press",userid);
		r=this.coreos.call(DB_CLASS,"query",sql);
		if(r!=null&&r.size()>0){
			Map map=(Map)r.get(0);
			info.put("press",map.get("zfhl"));
		}
		sql=String.format("select * from %s where userid=%s order by time desc limit 0,1","bld_sugar",userid);
		r=this.coreos.call(DB_CLASS,"query",sql);
		if(r!=null&&r.size()>0){
			Map map=(Map)r.get(0);
			info.put("sugar",map.get("zfhl"));
		}
		
		// 志愿者,亲属,医生数量
		sql=String.format("select count(*) as num from fk_user_mn where type='%s' and  userid=%s","mn_relatives",userid);
		r=this.coreos.call(DB_CLASS,"query",sql);
		if(r!=null&&r.size()>0){
			Map map=(Map)r.get(0);
			info.put("qn_num",map.get("num"));
		}
		sql=String.format("select count(*) as num from fk_user_mn where type='%s' and  userid=%s","mn_doctor",userid);
		r=this.coreos.call(DB_CLASS,"query",sql);
		if(r!=null&&r.size()>0){
			Map map=(Map)r.get(0);
			info.put("ys_num",map.get("num"));
		}
		sql=String.format("select count(*) as num from fk_user_mn where type='%s' and  userid=%s","mn_volunteer",userid);
		r=this.coreos.call(DB_CLASS,"query",sql);
		if(r!=null&&r.size()>0){
			Map map=(Map)r.get(0);
			info.put("zyz_num",map.get("num"));
		}
		return info;
	}
	public Object queryById(String table,Object id){
		return this.coreos.call(DB_CLASS,"queryById",table,id);
	}
	public Object update(String table,Map params){
		// 添加时间戳
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");
		params.put("time",format.format(new Date()));
		return this.coreos.call(DB_CLASS,"excute",table,params);
	}
	public Object remove(String table,String id){
		try{
			this.coreos.call(DB_CLASS,"deleteByIds",table,id);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	// 查询未添加的医生,亲属,志愿者
	public List query_noadd(String table,String userid){
		String sql=String.format(
				"select t.* from %s t  where t.id not in ( select fk.fkid from fk_user_mn fk where fk.userid=%s and fk.type='%s' ) order by time desc",
				table,userid,table);
		return this.coreos.call(DB_CLASS,"query",sql);
	}
	public boolean add_user_fk(String table,String userid,String ids){
		if(StringUtil.isEmpty(table)){
			return false;
		}
		try{
			for(String id : ids.split(",")){
				String sql="insert into fk_user_mn (userid,fkid,type) values(?,?,?)";
				List param=new ArrayList();
				param.add(userid);
				param.add(id);
				param.add(table);
				update(sql,param);
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public Object remove_user_fk(String table,String userid,String ids){
		if(StringUtil.isEmpty(table)){
			return false;
		}
		String sql="delete from fk_user_mn where type='"+table+"' and fkid in ("+ids+")";
		if(!StringUtil.isEmpty(userid)){
			 sql+=" and userid="+userid;
		}
		List param=new ArrayList();
		return update(sql,param);
	}
	
	private Object update(String sql,List param){
		return this.coreos.call(DB_CLASS,"excute",sql,param);
	}
	private String getParam(Map param,String name){
		return this.getParam(param, name,null);
	}
	private String getParam(Map param,String name,String def){
		if(param.get(name)==null){
			return def;
		}else{
 			return param.get(name).toString();
		}
	}
}
