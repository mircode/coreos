package os.admin.job;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.admin.application.CmdApp;
import osgi.enroute.scheduler.api.CronJob;

@Component(property=CronJob.CRON+"=*/20 * * * * ?")
public class CheckJob implements CronJob<Object> {

	// 异常恢复
	public static Map<String,Long> checktab=new HashMap<>();
		
	private boolean finish=true;
	CmdApp cmdApp=null;
	@Reference void setCmdApp(CmdApp cmdApp){
		this.cmdApp=cmdApp;
	}
	public void run(Object object) {
		// 通过finish控制检测频率,如果上一次恢复任务未完成,则不进行下一次恢复操作
		if(finish==true){
			finish=false;
			cmdApp.check();
			finish=true;
		}
	}
}
