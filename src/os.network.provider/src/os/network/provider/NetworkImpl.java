package os.network.provider;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import os.core.api.CoreOS;
import os.core.conf.Config;
import os.core.model.BundleInfo;
import os.core.model.HostInfo;
import os.core.model.ServiceInfo;
import os.core.tools.HostUtil;
import os.network.api.Network;
import osgi.enroute.webserver.capabilities.RequireWebServerExtender;

/**
 * 网卡模块
 */
//WEBService注解，这个引入Jetty的jar包，Jetty是嵌入式的HTTP服务器，添加这个注解，在resolve的时候，
//会自动引入Jetty的jar包,否则需要自己写入
@RequireWebServerExtender
@Component(name = "os.network",property={"service.exported.interfaces=*"},immediate=true)
//@SuppressWarnings设置JDK编译器注解，去掉代码中类型转换的警告，不影响代码执行
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NetworkImpl implements Network{
	
	// 依赖成员变量
	// 由OSGI容器注入
	@Reference
	CoreOS coreos=null;
	@Reference
	ConfigurationAdmin cm=null;
	
	// 通讯配置相关变量
	String ip=HostUtil.address();
	String port=HostUtil.port();
	String hostname=HostUtil.hostname();
	String route_url=Config.get(Config.ROUTE_URL);
	
	// 路由缓存
	private Set<Network> routes = new HashSet<>();
	
	// 输出流
	private PrintStream out=System.out;
	public void setOut(PrintStream out){this.out=out;}
		
	// 连接路由对象
	RouteConnect route=null;
	
	@Activate void start(){
		if(this.route==null){
			this.route=new RouteConnect(this.cm);
			this.connect(route_url);
		}
		// hock
		if(this.routes.size()==0){
			this.add(this);
		}
	}
	
	// 连接指定url的路由组件
	public void connect(String url){
		route.connect(url,ip,port);
	}
	
	
	// 添加网卡
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void add(Network network) {
		routes.add(network);
		
		DateFormat format=new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		String time=format.format(new Date());
		HostInfo host=network.getHostInfo();
		out.println(String.format("[%s]->[%s:%s]->[on]", time,host.ip,host.port));
	}
	// 移除网卡
	void remove(Network network) {
		routes.remove(network);
		
		DateFormat format=new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		String time=format.format(new Date());
		String host=network.toString();
		int index=host.replaceAll("http://","").indexOf("/");
		host=host.replaceAll("http://","").substring(0,index);
		out.println(String.format("[%s]->[%s]->[off]", time,host));
	}
	
	@Override
	public HostInfo getHostInfo(){
		// 主机信息
		String ip=this.ip;
		String hostname=this.hostname;
		String port=this.port;
		HostInfo host=new HostInfo(ip,port,hostname);
		return host;
	}
	@Override
	public List<ServiceInfo> getServices() {
		// 服务信息
		List<ServiceInfo> services=new ArrayList<>();
		// 服务信息
		this.coreos.getServices().forEach(service->{
			service.ip=ip;
			service.port=port;
			services.add(service);
		});
		return services;
	}
	@Override
	public List<BundleInfo> getBundles(){
		// 组件信息
		List<BundleInfo> bundles=new ArrayList<>();
		// 组件信息
		this.coreos.getBundles().forEach(bundle->{
			bundle.ip=ip;
			bundle.port=port;
			bundles.add(bundle);
		});
		return bundles;
		
	}
	@Override
	public List<Network> getRoutes() {
		List list=Arrays.asList(routes.toArray());
		return list;
	}
	
	@Override
	public <T> T call(String namespace,String method,Object... args){
		
		// 服务查找
		List<Network> targets=new ArrayList<>();
		//当前的所有network，即网卡对象
		List<Network> routes=this.getRoutes();
		for(Network net:routes){
			//获取当前环境所有服务
			List<ServiceInfo> list=net.getServices();
			for(ServiceInfo srv:list){
				if(srv.name.equals(namespace)){
					targets.add(net);
				}
			}
		}
		// 当前网络中无该服务
		if(targets.size()<=0){
			return null;
		}else{
			// 本机调用
			for(Network net:targets){
				if(net.equals(this)){
					try{
						log(namespace,method);
						return this.coreos.call(namespace, method, args);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
				
			// 远程负载均衡调用
			T res=null;
			for(int i=0;i<targets.size();i++){
				try{
					// 随机负载均衡
					Random random=new Random();
					int index=random.nextInt(targets.size());
					res=(T)targets.get(index).call(namespace, method, args);
					break;
				}catch(Exception e){
					e.printStackTrace();
				}finally{}
			}
			return res;
		}
	}
	public void log(String namespace,String method){
		if(namespace.matches("^(os.core|os.network|os.admin|os.route).*")){
			return;
		}
		DateFormat format=new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		String time=format.format(new Date());
		HostInfo host=this.getHostInfo();
		out.println(String.format("[%s]->[%s:%s]->[%s:%s]", time,host.ip,host.port,namespace,method));
		
	}
	@Override  
	public boolean equals(Object other) {
		Network o=null;
		if(other instanceof Network){
			o=(Network)(other);
		}
        return this.getHostInfo().equals(o.getHostInfo());  
	}
	@Override  
    public int hashCode() {  
        return ("network"+ip+port).hashCode();  
    }
	@Override
	public String toString(){
		return "network:"+ip+":"+port;
	}


}
