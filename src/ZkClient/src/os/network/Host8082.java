package os.network;

import os.network.provider.RouteConnect;

public class Host8082 {
	public static void main(String args[]) throws Exception{
		
		// 路由地址
		String route_addr="localhost:6789";
		
		// 要上报的主机通讯信息
		String ip="localhost";
		String port="8082";
		String path="table";
		
		// 连接上报
		RouteConnect client=new RouteConnect();
		client.connect(route_addr).report(ip, port, path);
		Thread.sleep(Long.MAX_VALUE);
	}
}
