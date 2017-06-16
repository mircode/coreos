package os.network.provider;

import java.util.List;
import java.util.UUID;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class RouteConnect {
	private ZooKeeper zk=null;
	
	// 连接路由,并注册监听事件从Zookeeper服务端获取最新的数据
	public RouteConnect connect(String route_addr) throws Exception{
		zk = new ZooKeeper(route_addr,10000,new Watcher(){
			
			// 链接的时候,通知注册监听事件,用于获取指定存储目录下,客户端连接存放的通讯地址信息
			@Override
			public void process(WatchedEvent event) {
				 if(event.getType()==Watcher.Event.EventType.NodeChildrenChanged)
					try{
						List<String> lists=zk.getChildren("/route", true);
						System.out.println("\n--------最新节点列表----------");
						for(String path:lists){
							byte[] data = zk.getData("/route/"+path, false, null);
							String info=new String(data, "UTF-8");
							System.out.println(info);
						}
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
	public void report(String ip,String port,String url) throws Exception{
		// 创建route下的子目录,一个子目录记录一个主机。产生一个UUID作为key,对应的主机通讯IP和端口,作为数据value
		UUID uuid = UUID.randomUUID();
		zk.create("/route/"+uuid.toString().substring(0,8), ("http://"+ip+":"+port+"/"+url+"/").getBytes(),Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL);
		
		// 主动获取一次
		List<String> lists=zk.getChildren("/route", true);
		System.out.println("\n--------最新节点列表----------");
		for(String path:lists){
			byte[] data = zk.getData("/route/"+path, false, null);
			String info=new String(data, "UTF-8");
			System.out.println(info);
		}
	}
	
		
	
}
