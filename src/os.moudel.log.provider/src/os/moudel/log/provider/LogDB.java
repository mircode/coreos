package os.moudel.log.provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.core.api.CoreOS;

/**
 * 日志记录类
 */
@Component(name = "os.moudel.log",service=LogDB.class)
@SuppressWarnings({"rawtypes","unchecked"})
public class LogDB{
	CoreOS coreos=null;
	@Reference void setCoreOS(CoreOS coreos){
		this.coreos=coreos;
	}
	public List query(Map map) {
		return this.coreos.call("os.moudel.db.api.DBase","query","log",map);
	}
	public void info(String level, String user, String ip,String msg) {
		List param=new ArrayList<>();
		param.add(level);
		param.add(user.split(":")[0]);
		param.add(user.split(":")[1]);
		param.add(ip);
		param.add(msg);
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");
		param.add(format.format(new Date()));
		String sql="insert into log(level,userid,username,ip,msg,time) values(?,?,?,?,?,?)";
		update(sql,param);
	}
	
	private void update(String sql,List param){
		this.coreos.call("os.moudel.db.api.DBase","excute",sql,param);
	}
	
}
