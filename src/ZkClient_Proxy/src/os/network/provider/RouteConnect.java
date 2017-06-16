package os.network.provider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import os.network.api.Network;
import os.network.http.HttpClient;
import os.network.http.HttpServerExport;

public class RouteConnect {
	private ZooKeeper zk=null;
	private Network network=null;
	public RouteConnect(Network network){
		this.network=network;
	}
	// 连接路由,并注册监听事件从Zookeeper服务端获取最新的数据
	public RouteConnect connect(String route_addr) throws Exception{
		zk = new ZooKeeper(route_addr,10000,new Watcher(){
			// 链接的时候,通知注册监听事件,用于获取指定存储目录下,客户端连接存放的通讯地址信息
			@Override
			public void process(WatchedEvent event) {
				 if(event.getType()==Watcher.Event.EventType.NodeChildrenChanged)
					try{
						updateUrls();
					}catch(Exception e){
						e.printStackTrace();
					}
			}
		});
		
		// 如果route目录不存在,则创建route目录,
		if(zk.exists("/route",true)==null){
			zk.create("/route", null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		}
		return this;
	}
	// 上报自己IP和端口
	public RouteConnect report(String ip,String port,String url) throws Exception{
		// 创建route下的子目录,一个子目录记录一个主机。产生一个UUID作为key,对应的主机通讯IP和端口,作为数据value
		UUID uuid = UUID.randomUUID();
		
		zk.create("/route/"+uuid.toString().substring(0,8), ("http://"+ip+":"+port+"/"+url+"/").getBytes(),Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL);
		// 更新通讯地址
		updateUrls();
		return this;
	}
	public void updateUrls()throws Exception{
		// 主动获取一次
		List<String> lists=zk.getChildren("/route", true);
		System.out.println("\n--------最新节点列表----------");
		List<String> urls=new ArrayList<>();
		for(String path:lists){
			byte[] data = zk.getData("/route/"+path, false, null);
			String info=new String(data, "UTF-8");
			System.out.println(info);
			urls.add(info);
		}
		System.out.println("创建远程代理对象");
		create_proxy(urls);
	}
	public RouteConnect listen(String port){
		try{
			new HttpServerExport(network).start(Integer.parseInt(port));
		}catch(Exception e){
			e.printStackTrace();
		}
		return this;
	}
	public void create_proxy(List<String> urls){
		
		// 清空原有数据
		NetworkImpl net=(NetworkImpl)this.network;
		net.clear();
		
		for(String url:urls){
			// 参数:类加载器,Network接口,Hanlder函数
			Object network =Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{Network.class}, new ProxyHandler(url));
			// 向network添加远程代理对象
			net.add((Network)network);
		}
	}
	public class ProxyHandler implements InvocationHandler{
		private String url=null;
		public ProxyHandler(String url){
			this.url=url;
		}
		// 处理代理对象方法调用
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			
			// 通过HttpClient客户端发送远程请求
			return HttpClient.post(url, method.getName(),args);
		}
		
	}
		
	
}
