package os.route.provider;


import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import os.core.conf.Config;

/**
 * 路由模块
 */
@Component(name = "os.route")
public class Route extends ZooKeeperServerMain {
	
	private Thread thread;
	private ServerConfig config;

	// 初始匿
	@Activate
	void start(BundleContext context) {
		
		System.out.println(Config.get(Config.ROUTE_URL));
		// 默认启动端口
		String port=Config.get(Config.ROUTE_URL);
		String ip=Config.get(Config.HOST_IP);
		if(port.split(":").length==2){
			port=port.split(":")[1];
		}
		
		// 获取系统临时目录,用于存放网络信息
		String tmp=System.getProperty("java.io.tmpdir");
		
		// 创建路由相关配置
		System.out.println(String.format("config:route:%s:%s", ip,port));
		System.out.println(String.format("config:tmp:%s",tmp));
		config = new ServerConfig();
		config.parse(new String[]{port,tmp});
		
		// 启动路由
		System.out.println("start:route");
		thread = new Thread(this::config_run,"os.route");
		thread.start();
	
	}
	
	// 注销
	@Deactivate
	void deactivate() {
		shutdown();
		thread.interrupt();
		System.out.println("stop:route");
	}
	
	// 根据配置启动
	public void config_run() {
		try {
			runFromConfig(config);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("config:error");
		}
	}
	
}
