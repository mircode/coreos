package os.network.provider;

import java.net.Socket;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
/**
 * 
 * 路由连接对象
 * @author admin
 *
 */
public class RouteConnect {
	
	// OSGI配置管理对象
	ConfigurationAdmin cm=null;
	public RouteConnect(ConfigurationAdmin cm){
		this.cm=cm;
	}
	
	// 主机IP端口信息
	String ip=null;
	String port=null;
	
	// 链接请求
	public void connect(String route_url,String ip,String port){
		
		this.ip=ip;
		this.port=port;
		
		new Thread(new Runnable(){
			@Override
			public void run() {
				// 检测route_url是否可用
				ping(route_url);
				// 更新连接配置发起连接
				try{
					Configuration configuration = cm.getConfiguration("org.amdatu.remote.discovery.zookeeper","?");
					if(configuration!=null){
						Dictionary<String, Object> map = configuration.getProperties();
						if(map==null){map=new Hashtable<String, Object>();}
						
						// 一、zookeeper服务端地址
						map.put("org.amdatu.remote.discovery.zookeeper.connectstring",route_url);
						
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
	// 检测网络是否可用到达
	public void ping(String url){
		while(true){
			String route_ip=url.split(":")[0];
			String route_port=url.split(":")[1];
			try{
				new Socket(route_ip,Integer.parseInt(route_port));
				break;
			}catch (Exception e){
				System.out.println(String.format("network[%s:%s]->route[%s:%s] connect error",ip,port,route_ip,route_port));
			}
			try{
				Thread.sleep(3000);
			}catch(Exception e){};
		}
	}
	
}
