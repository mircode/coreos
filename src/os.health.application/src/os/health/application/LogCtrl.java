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
 *
 */
@Component(name="os.log",property=JSONRPC.ENDPOINT + "=log")
@SuppressWarnings("rawtypes")
public class LogCtrl extends BaseCtrl implements JSONRPC  {

	// 日志记录类
	String LOG_CLASS="os.moudel.log.provider.LogDB";
	
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
	public List query(Map map){
		try{
			List res=this.coreos.call(LOG_CLASS,"query",map);
			return res;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public Object getDescriptor() throws Exception {
		return "log";
	}
}
