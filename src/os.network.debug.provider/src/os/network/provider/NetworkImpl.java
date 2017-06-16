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
	
	
	// 路由缓存
	private Set<Network> routes = new HashSet<>();
	
	// 输出流
	private PrintStream out=System.out;
	
	@Activate void start(){
		// hock
		add(this);
	}
	
	// 系统内核
	CoreOS coreos=null;
	@Reference void setCoreOS(CoreOS coreos){
		this.coreos=coreos;
	}
	// 连接路由
	@Reference void connect(ConfigurationAdmin cm) {
		HostInfo host=this.getHostInfo();
		String ip=host.ip;
		String port=host.port;// http端口
		RouteConnect route=new RouteConnect(ip,port);
		route.connect(cm);
		
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
		HostInfo host=network.getHostInfo();
		out.println(String.format("[%s]->[%s:%s]->[off]", time,host.ip,host.port));
	}
	
	@Override
	public HostInfo getHostInfo(){
		// 主机信息
		String ip=HostUtil.address();
		String hostname=HostUtil.hostname();
		String port=System.getProperty("org.osgi.service.http.port","8080");// http端口
		HostInfo host=new HostInfo();
		host.ip=ip;
		host.hostname=hostname;
		host.port=port;
		return host;
	}
	@Override
	public List<ServiceInfo> getServices() {
		// 主机信息
		HostInfo host=this.getHostInfo();
		String ip=host.ip;
		String port=host.port;
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
		// 主机信息
		HostInfo host=this.getHostInfo();
		String ip=host.ip;
		String port=host.port;
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
	
	@Override  
	public boolean equals(Object other) {
		HostInfo self=this.getHostInfo();
		Network o=null;
		if(other instanceof Network){
			o=(Network)(other);
		}
		HostInfo host=o.getHostInfo();
        return (self.ip+self.port).equals(host.ip+host.port);  
	}
	@Override  
    public int hashCode() {  
		HostInfo self=this.getHostInfo();
        return (self.ip+self.port).hashCode();  
    }
	@Override
	public String toString(){
		HostInfo self=this.getHostInfo();
		return self.ip+":"+self.port;
	}
}
