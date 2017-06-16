package os.admin.job;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.admin.application.CmdApp;

@Component
public class CheckJob{

	// Òì³£»Ö¸´
	public static Map<String,Long> checktab=new HashMap<>();
		
	private boolean finish=true;
	CmdApp cmdApp=null;
	@Reference void setCmdApp(CmdApp cmdApp){
		this.cmdApp=cmdApp;
	}
	@Activate void start() {
		new Thread(new Runnable(){
			public void run(){
				while(true){
					// Ë¯2s
					try{
						Thread.sleep(2000);
					}catch(Exception e){}
					if(finish==true){
						finish=false;
						try{
							cmdApp.check();
						}catch(Exception e){}
						finish=true;
					}
				}
			}
		}).start();
	}
}
