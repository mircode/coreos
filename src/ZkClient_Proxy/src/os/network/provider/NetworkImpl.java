package os.network.provider;

import java.util.ArrayList;
import java.util.List;

import os.network.api.Network;

public class NetworkImpl implements Network{
	// 当前主机IP和端口
	private String addr=null;
	// 路由缓存
	public List<Network> routes = new ArrayList<>();
		
	public NetworkImpl(String addr){
		this.addr=addr;
	}
	
	public void connect() {

		// 路由地址
		String route_addr="localhost:6789";
		
		// 要上报的主机通讯信息
		String ip=addr.split(":")[0];
		String port=addr.split(":")[1];
		String path="table";
		
		// 连接路由
		try{
			RouteConnect client=new RouteConnect(this);
			// 连接路由—>启动监听服务—>上报通讯地址
			client.connect(route_addr).listen(port).report(ip, port, path);
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	@Override
	public String getHostInfo() {
		return addr;
	}

	@Override
	public String call(String namespace, String method, Object[] args) {
		
		System.out.println("\n---------远程调用------------");
		System.out.println("[  host] -> "+addr);
		System.out.println("[ class] -> "+namespace);
		System.out.println("[method] -> "+method);
		String params="";
		for(Object arg:args){
			params+=arg.toString()+",";
		}
		params=params.replaceAll(",$","");
		System.out.println("[  args] -> "+params);
		
		// 反射调用目标类,返回执行结果
		String res="I'm "+this.getHostInfo();
		System.out.println("[result] -> "+res);
		return res;
	}
	public void add(Network network){
		if(routes.contains(network.getHostInfo())){
			routes.remove(network.getHostInfo());
		}
		routes.add(network);
	}
	public void clear(){
		routes.clear();
	}
	public void remove(Network network){
		routes.remove(network);
	}
	
	@Override
	public String toString(){
		return addr;
	}

}
