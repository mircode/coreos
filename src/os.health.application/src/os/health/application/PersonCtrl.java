	package os.health.application;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.core.api.CoreOS;
import os.health.base.BaseCtrl;
import osgi.enroute.jsonrpc.api.JSONRPC;

/**
 * 个人体征模块
 * @author admin
 */
@SuppressWarnings("rawtypes")
@Component(name="os.person",property=JSONRPC.ENDPOINT+"=person")
public class PersonCtrl extends BaseCtrl implements JSONRPC  {

	// 个人体征类
	String PERSON_CLASS="os.moudel.person.provider.PersonService";
		
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
	// 个人体征查询
	public List query(Map param){
		
		// 查询
		String table=(String)param.get("table");
		try {
			List res = this.coreos.call(PERSON_CLASS,"query",table,param);
			this.log("info", getDesc(table)+"列表查询成功");
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			this.log("info", getDesc(table)+"列表查询失败");
		}
		return null;
	}
	// 添加数据
	public Object update(String table,Map param){
		return this.coreos.call(PERSON_CLASS,"update",table,param);
	}
	private String getDesc(String table){
		String info="个人体征";
		if(table.equals("bld_fat")){
			info="体脂";
		}else if(table.equals("bld_oxygen")){
			info="血氧";
		}else if(table.equals("bld_press")){
			info="血压";
		}else if(table.equals("bld_sugar")){
			info="血糖";
		}
		return info;
	}
	@Override
	public Object getDescriptor() throws Exception {
		return "person";
	}
}
