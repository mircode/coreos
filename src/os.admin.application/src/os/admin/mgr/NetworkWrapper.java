package os.admin.mgr;

import java.util.List;
import java.util.stream.Collectors;

import os.core.model.BundleInfo;
import os.core.model.HostInfo;
import os.core.model.ServiceInfo;
import os.core.tools.ReflectUtil;

/**
 * 网卡包装类
 * @author admin
 *
 */
public class NetworkWrapper{

	// 真实网卡对象
	public Object network=null;
	
	public NetworkWrapper(Object network){
		this.network=network;
	}
	
	public <T> T call(String namespace, String method, Object... args) {
		try {
			return ReflectUtil.invoke(network, "call",namespace,method,args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<ServiceInfo> getServices() {
		try {
			return ReflectUtil.invoke(network, "getServices");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<BundleInfo> getBundles() {
		try {
			return ReflectUtil.invoke(network, "getBundles");
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return null;
	}

	public HostInfo getHostInfo() {
		try {
			return ReflectUtil.invoke(network, "getHostInfo");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<NetworkWrapper> getRoutes() {
		List<Object> list;
		try {
			list = ReflectUtil.invoke(network, "getRoutes");
			List<NetworkWrapper> routes=list.stream()
					.map(net->{
						return new NetworkWrapper(net);
					}).collect(Collectors.toList());
			return routes;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public Object getNetwork(){
		return this.network;
	}
	@Override  
	public boolean equals(Object other) {
		NetworkWrapper o=null;
		if(other instanceof NetworkWrapper){
			o=(NetworkWrapper)(other);
		}
		return network.equals(o.getNetwork());
	}
	@Override  
    public int hashCode() {  
		return network.hashCode();
    } 
}
