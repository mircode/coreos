package os.network.provider;

import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class RouteConnect {
	
	// 路由地址
	private String addr=null;
	
	// 本机IP地址和端口
	private String ip=null;
	private String port=null;
	
	public RouteConnect(String ip,String port){
		this.ip=ip;
		this.port=port;
		
		 String addr=System.getenv().get("OS_ROUTE");
		 if(addr==null||addr.equals("")){
			 addr=System.getProperty("os.route");
		 }
		 if(addr==null||addr.equals("")){
			 addr="localhost:6789";
		 }
		 this.addr=addr;
	}
	// 链接请求
	public void connect(ConfigurationAdmin cm){
		new Thread(new Runnable(){
			@Override
			public void run() {
				
				// 检测路由连接直至可用
				String route_ip=addr.split(":")[0];
				String route_port=addr.split(":")[1];
				while(true){
					try{
						new Socket(route_ip,Integer.parseInt(route_port));
						break;
					} catch (Exception e){
						System.out.println("connect route error");
						try {
							Thread.sleep(3000);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				}
				
				// 更新连接配置发起连接
				try{
					Configuration configuration = cm.getConfiguration("org.amdatu.remote.discovery.zookeeper","?");
					if(configuration!=null){
						Dictionary<String, Object> map = configuration.getProperties();
						if(map==null){map=new Hashtable<String, Object>();}
						
						// 一、zookeeper服务端地址
						map.put("org.amdatu.remote.discovery.zookeeper.connectstring",addr);
						
						// 二、需要存储的信息
						// 需要往zookeeper服务端存储的信息,这些信息包含了本网卡的通讯所需的基本信息
						// 包括：网卡所代表的IP和PROT
						// 包括：因为通讯协议是基于HTTP的,所以执行了一个table路径
						// 这几条信息会在zookeeper服务端,以http://ip:port/path的形式进行存储
						map.put("org.amdatu.remote.discovery.zookeeper.host", ip);
						map.put("org.amdatu.remote.discovery.zookeeper.port", port);
						map.put("org.amdatu.remote.discovery.zookeeper.path","table");
						// 三、存储的路径
						// 设置zookeeper存储这些信息的路径,可以理解为key
						// key:为route value：http://ip:port/path 组成的url
						map.put("org.amdatu.remote.discovery.zookeeper.rootpath", "/route");
						
						// 四、心跳检测的频率
						map.put("org.amdatu.remote.discovery.zookeeper.schedule", 3);
						configuration.update(map);
					}
				} catch (Exception e) {}
			
				try{
					// 更新远程管理信息
					Configuration configuration = cm.getConfiguration("org.amdatu.remote.admin.http","?");
					if(configuration!=null){
						Dictionary<String, Object> map=configuration.getProperties();
						if(map==null){map=new Hashtable<String, Object>();}
						
						map.put("org.amdatu.remote.admin.http.host",ip);
						map.put("org.amdatu.remote.admin.http.port",port);
						map.put("org.amdatu.remote.admin.http.path","services");
						
						configuration.update(map);
					}
				} catch (Exception e) {}
			}
		}).start();
	}
		
	
}
