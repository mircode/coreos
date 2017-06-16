package os.admin.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.admin.base.BaseCtrl;
import os.admin.mgr.ClusterMgr;
import os.admin.mgr.NetworkWrapper;
import os.core.api.CoreOS;
import os.core.model.BundleInfo;
import os.core.tools.HostUtil;
import os.core.tools.ReflectUtil;
import osgi.enroute.jsonrpc.api.JSONRPC;

//集群Web管理组件
@Component(name="os.admin",property=JSONRPC.ENDPOINT + "=admin")
@SuppressWarnings({"rawtypes","unchecked"})
public class AdminApp extends BaseCtrl implements JSONRPC  {
	
	
	// 依赖对象
	CoreOS coreos=null;
	@Reference void setCoreOS(CoreOS coreos){
		this.coreos=coreos;
	}
	// 系统信息
	public List infos(){
		List nodes=new ArrayList();
		ClusterMgr cluster=this.getManager();
		if(cluster!=null){
			nodes=sysInfo(cluster.getBundles(),true);
		}else{
			List<BundleInfo> list=this.coreos.getBundles();
			nodes=sysInfo(list,false);
		}
		return nodes;
	}
	// 组件仓库
	public List repertories(){
		List list=new ArrayList<>();
		list=coreos.getRepertories().stream().map(bundle->{
			Map<String,String> map=new HashMap<>();
			map.put("name",bundle.name);
			map.put("location",bundle.location);
			map.put("version",bundle.version);
			return map;
		}).collect(Collectors.toList());
		return list;
	}
	
	CmdApp cmd=null;
	@Reference void setCmd(CmdApp cmd){
		this.cmd=cmd;
	}
	// 安装组件
	public List execute(Map param){
		String method=(String)param.get("method");
		// 是否是集群环境
		boolean cluster_env=false;
		ClusterMgr cluster=this.getManager();
		if(cluster!=null){
			cluster_env=true;
		}
		boolean start=false;
		Object args=param.get("start");
		if(args!=null&&args.toString().equals("true")){
			start=true;
		}
		if(method.equals("install")){
			// 要安装的组件
			String location=param.get("location").toString();
			
			if(!cluster_env){
				cmd.install(location);
				if(start){
					cmd.start(location.replace(".jar", ""));
				}
			}else{
				// 指定主机安装
				Object addr=param.get("addr");
				if(addr!=null){
					cmd.install(addr.toString(),location);
					if(start){
						cmd.start(addr.toString(),location.replace(".jar", ""));
					}
				}
				// 指定数目安装
				Object num=param.get("num");
				if(num!=null){
					cmd.install(location,Long.parseLong(num.toString()));
					if(start){
						cmd.start(location.replace(".jar", ""));
					}
				}
			}
		}else if(method.equals("change")){
			String bundle=param.get("bundle").toString();
			Long num=Long.parseLong(param.get("num").toString());
			cmd.change(bundle,num);
		}else if(method.equals("move")){
			String bundle=param.get("bundle").toString();
			String from=param.get("from").toString();
			String to=param.get("to").toString();
			cmd.move(bundle,from,to);
		}else if(method.equals("update")){
			String bundle=param.get("bundle").toString();
			Object addr=param.get("addr");
			Object time=param.get("time");
			if(addr!=null){
				cmd.update(addr.toString(), bundle);
			}else{
				if(time!=null){
					cmd.update(bundle,Long.parseLong(time.toString()));
				}else{
					cmd.update(bundle);
				}
			}
		}else{
			// 启动 停止 卸载
			List<String> params=new ArrayList<>();
			String bundle=param.get("bundle").toString();
			Object addr=param.get("addr");
			if(addr!=null&&cluster_env){
				params.add(addr.toString());
			}
			params.add(bundle);
			try {
				ReflectUtil.invoke(cmd, method, params.toArray());
			} catch (Exception e) {
				
			}
			
		}
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return infos();
	}
	public List oneInstall(Map<String,Object> param){
		for (Map.Entry<String,Object> it : param.entrySet()) {
			String key=it.getKey();
			Object val=it.getValue();
			if(key!=null&&key.startsWith("os.")){
				String bundle=key.toString();
				Long num=Long.parseLong(val.toString());
				cmd.change(bundle,num);
			}
		}
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return infos();
	}
	public ClusterMgr getManager(){
		Object target=this.coreos.getService("os.network.api.Network");
		if(target!=null){
			NetworkWrapper network=new NetworkWrapper(target);
			ClusterMgr cluster=new ClusterMgr(network);
			return cluster;
		}
		return null;
	}
	private List sysInfo(List<BundleInfo> list,boolean cluster){
		Map cache=new HashMap();
		list.stream().filter(bdl->{
			return bdl.name.startsWith("os.");
		}).forEach(bdl->{
			Map bundle=new HashMap();
			bundle.put("name",bdl.name);
			bundle.put("status",bdl.status);
			bundle.put("version",bdl.version);
			
			List services=bdl.services.stream().filter(srv->{
				return srv.name.startsWith("os.");
			}).map(srv->{
				Map service=new HashMap();
				service.put("name", srv.name);
				service.put("status", srv.status);
				service.put("methods", srv.methods);
				return service;
			}).collect(Collectors.toList());
			bundle.put("services",services);
			
			String key="local";
			if(cluster==true){
				key=bdl.ip+":"+bdl.port;
			}
			if(cache.get(key)==null){
				String ip=bdl.ip;
				String port=bdl.port;
	
				Map node=new HashMap();
				node.put("ip",ip);
				node.put("port",port);
				node.put("bundles",new ArrayList());
				if(ip==null){
					ip=HostUtil.address();
					port=System.getProperty("org.osgi.service.http.port","8080");
					node.put("cluster",false);
					node.put("ip",ip);
					node.put("port",port);
				}
				cache.put(key,node);
			}
			Map node=(Map)cache.get(key);
			((List)node.get("bundles")).add(bundle);
		});
		return Arrays.asList(cache.values().toArray());
	}
	@Override
	public Object getDescriptor() throws Exception {
		return "admin";
	}
	@Override
	public CoreOS getCoreOS() {
		return this.coreos;
	}

}
