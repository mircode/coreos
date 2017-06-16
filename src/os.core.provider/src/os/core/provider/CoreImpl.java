package os.core.provider;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

import os.core.api.CoreOS;
import os.core.conf.Config;
import os.core.model.BundleInfo;
import os.core.model.ConfigInfo;
import os.core.model.ServiceInfo;
import os.core.tools.BundleUtil;
import os.core.tools.ReflectUtil;

/**
 * 软件内核
 * 提供基础组件安装卸载等操作
 */
@Component(name="os.core",immediate=true)
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CoreImpl implements CoreOS{
	//组件的实体，jar相关的操作接口，包括安装卸载启动等接口
	BundleContext context=null;
	
	// 本地服务
	//HashSet<>存放对象，set是集合接口，HashSet进行了实现
	Set<ServiceInfo> services=new HashSet<>();
	// 本地组件
	Set<BundleInfo>  bundles=new HashSet<>();
		
	// 启动方法,参数component和context由OSGI容器注入，@Active初始化，@Reference解决对象依赖，OSGi注入
	@Activate void start(ComponentContext componet,BundleContext context) {
		
		this.context=context;
		// 监听本地节点组件的安装和卸载
		this.context.addBundleListener(new BundleListener(){
			@Override
			public void bundleChanged(BundleEvent event) {
				BundleInfo bleInfo=BleInfo(event.getBundle());
				if(bleInfo!=null){
					// update
					if(bundles.contains(bleInfo)){
						bundles.remove(bleInfo);
					}
					bundles.add(bleInfo);
				}
			}
		});
		//查询当前bundle，加入bundles变量里面
		for(Bundle bundle:context.getBundles()){
			BundleInfo bleInfo=BleInfo(bundle);
			if(bleInfo!=null){
				// update
				if(bundles.contains(bleInfo)){
					bundles.remove(bleInfo);
				}
				bundles.add(bleInfo);
			}
		};
		
		// 监听本地服务的注册和注销  
		this.context.addServiceListener(new ServiceListener(){
			@Override
			public void serviceChanged(ServiceEvent event) {
				ServiceInfo srvInfo=SrvInfo(event.getServiceReference());
				if(srvInfo!=null){
					if(event.getType()==ServiceEvent.UNREGISTERING){ 
						services.remove(srvInfo);
					}else{
						if(services.contains(srvInfo)){
							services.remove(srvInfo);
						}
						services.add(srvInfo);
					}
				}
			}
		});
		//查询当前服务加入services变量里面
		try{
			ServiceReference[] refs = context.getAllServiceReferences(null,null);
	        if(refs != null){
	        	for(ServiceReference ref : refs){
	        		ServiceInfo srvInfo=SrvInfo(ref);
	        		if(srvInfo!=null){
		        		if(services.contains(srvInfo)){
							services.remove(srvInfo);
						}
		            	services.add(srvInfo);
	        		}
	            }
	        }
	        
	        //把core的方法服务加进去，是个bug
	        String id="0";
	        try{
	        	id=componet.getServiceReference().getProperty("service.id").toString();
	        }catch(Exception e){};
	        services.add(SrvInfo(CoreOS.class,id));
	        
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	// 管理接口
	@Override
	public Bundle install(String location) throws BundleException{
		location=BundleUtil.getRepPath(location);
		return context.installBundle(location);
	}
	@Override
	public Bundle uninstall(String nameVersion) throws BundleException{
		Bundle bundle=null;
		List<Bundle> bundles=this.search(nameVersion);
		if(bundles!=null&&bundles.size()>0){
			for(Bundle bde:bundles){
				bundle=bde;
				bde.uninstall();
			}
		}
		return bundle;
	}
	@Override
	public Bundle start(String nameVersion)throws BundleException{
		Bundle bundle=null;
		List<Bundle> bundles=this.search(nameVersion);
		if(bundles!=null&&bundles.size()>0){
			for(Bundle bde:bundles){
				bundle=bde;
				bde.start();
			}
		}
		return bundle;
	}
	@Override
	public Bundle stop(String nameVersion)throws BundleException{
		Bundle bundle=null;
		List<Bundle> bundles=this.search(nameVersion);
		if(bundles!=null&&bundles.size()>0){
			for(Bundle bde:bundles){
				bundle=bde;
				bde.stop();
			}
		}
		return bundle;
	}
	@Override
	public Bundle update(String nameVersion)throws BundleException{
		Bundle bundle=null;
		List<Bundle> bundles=this.search(nameVersion);
		if(bundles!=null&&bundles.size()>0){
			for(Bundle bde:bundles){
				bundle=bde;
				bde.update();
			}
		}
		return bundle;
	}
	
	// 查询接口
	//当前的所有组件的信息
	@Override
	public List<BundleInfo> getBundles(){
		List list=Arrays.asList(bundles.toArray());
		return list;
	}
	@Override
	//当前所有服务的信息
	public List<ServiceInfo> getServices() {
		List list=Arrays.asList(services.toArray());
		return list;
	}
	//组件仓库的组件信息
	@Override
	public List<BundleInfo> getRepertories() {
		return BundleUtil.getRepList();
	}
	//根据当前环境组件信息和类路径找到该服务
	@Override
	public Object getService(String namespace) {
		// 本地查找
		if(context!=null){
			ServiceTracker tracker = new ServiceTracker(context,namespace,null);tracker.open();
			Object service=tracker.getService();
			return service;
		}
		return null;
	}
	
	// 组件调用接口
	@Override
	public <T> T call(String namespace,String method,Object... args){
		// 本地查找
		if(context!=null){
			// 根据类名查找对应的服务
			ServiceTracker tracker = new ServiceTracker(context,namespace,null);tracker.open();
			//获取该服务或者说component的全局对象
			Object service =tracker.getService();
				// 反射调用服务
				if(service!=null){
					// 目标类,clazz是类对象
					Class clazz=service.getClass();
					// 目标参数
			    	List params=new ArrayList();
			    	for(Object obj:args){
			    		params.add(obj);
			    	}
			    	// 查询目标方法
			    	Method func=ReflectUtil.search(clazz,method, params);
					
					// 本地调用
					if(func!=null){
						try{
							return (T)ReflectUtil.invoke(service, method, params);
						}catch (Exception e) {
							tracker.close();
							throw new RuntimeException("本地调用错误",e);
						}
					}
				}else{
					return (T)rmtCall(namespace,method,args);
				}
		}
	
		return null;
	}
	Object rmtCall(String namespace,String method,Object... args) {
		// 获取网卡
		ServiceTracker tracker = new ServiceTracker(context,"os.network.api.Network",null);tracker.open();
		//获取网卡的全局对象
		Object network =tracker.getService();
		// 通过网卡调用服务
		if(network!=null){
			try{
				//network.getClass()获取类对象
				Method func=network.getClass().getMethod("call",new Class[]{String.class,String.class,Object[].class});
				return func.invoke(network, namespace, method, args);
			}catch(Exception e){
				throw new RuntimeException("远程调用错误",e);
			}
		}
		return null;
	}
	
	
	// 工具函数
	// 根据组件名称和版本检索组件
	List<Bundle> search(String nameVersion){
		List<Bundle> bundles=new ArrayList<>();
		// 如果nameVersion是由数字组成的串,这表明nameVersion为组件ID,则根据ID查找组件
		if(nameVersion.matches("\\d+")){
			Bundle bundle=context.getBundle(Long.parseLong(nameVersion));
			bundles.add(bundle);
		// 否则根据组件名称和组件版本,搜索匹配的组件
		}else{
			// name:version截串
			String name=BundleUtil.name(nameVersion);
			String version=BundleUtil.version(nameVersion);
			for(Bundle bundle:context.getBundles()){
				// 获取组件的简称和版本号
				String bdlName=BundleUtil.name(bundle.getSymbolicName());
				String bdlVerson=BundleUtil.version(bundle.getVersion().toString());
				
				// 比较搜索串和目标组件
				if(version!=null){
					if(bdlName.equals(name)&&bdlVerson.equals(version)){
						bundles.add(bundle);
					}
				}else{
					if(bdlName.equals(name)){
						bundles.add(bundle);
					}
				}
			}
		}
		// 返回搜索结果
		return bundles.size()==0?null:bundles;
	}
	// 通过反射获取一个类的服务信息
	ServiceInfo SrvInfo(Class clazz,String id){
		try{
	    	// 类方法
	    	List<String> methods=new ArrayList<>();
	    	for(Method m:clazz.getDeclaredMethods()){
	    		methods.add(m.getName());
			}
	    	// save
	    	ServiceInfo srvInfo=new ServiceInfo();
			srvInfo.id=id;
			///服务的名字就是类路径就是类的名字
	    	srvInfo.name=clazz.getName();
	    	srvInfo.status="RUNNING";
	    	srvInfo.methods=methods;
	    	// hock
	    	if(srvInfo.name==null){return null;};
	    	return srvInfo;
	    	
		}catch(Exception e){ 
			new RuntimeException("获取服务信息失败",e);
		}
    	return null;
	}
	// 通过服务引用对象获取服务信息
	ServiceInfo SrvInfo(ServiceReference ref){
		
		Map<String,String> props=getProps(ref.toString());
		
		String clz=props.get("clazz");
		
		try{
			Object service=context.getService(ref);
			if(service==null){
				return null;
			}
			
			// 查找服务接口类
	    	Class clazz=service.getClass();
	    	for(Class<?> inter:clazz.getInterfaces()){
	    		if(inter.getName().equals(clz)){
	    			clazz=inter;
	    		}
	    	}
	    	
	    	// 通过反射获取接口方法名
	    	Method ms[]=null;
	    	List<String> methods=new ArrayList<>();
	    	try{
	    		// hock
	    		if(clazz.getName().equals("org.apache.felix.gogo.command.OBR")){
	    			return null;
	    		}
	    		if(clazz.getName().equals("org.apache.felix.gogo.runtime.threadio.ThreadIOImpl")){
	    			return null;
	    		}
	    		ms=clazz.getDeclaredMethods( );
	    	}catch(Exception e){
	    		ms=clazz.getMethods();
	    	}finally{}
	    	for(Method m:ms){
	    		methods.add(m.getName());
			}
	    	
	    	// save
	    	ServiceInfo srvInfo=new ServiceInfo();
	    	srvInfo.name=clz;
	    	srvInfo.methods=methods;
	    	srvInfo.status="RUNNING";
	    	srvInfo.id=props.get("service.id");
	    	srvInfo.bundle=ref.getBundle().getBundleId()+"";
	    	
	    	// hock
	    	if(srvInfo.name==null){return null;};
	    	
	    	return srvInfo;
			
		}catch(Exception e){ 
			e.printStackTrace();
		}
    	return null;
	 }
 	// 通过服务引用获取服务属性信息
	Map<String,String> getProps(String json){
		Pattern pattern = Pattern.compile("\\{(.*)\\}=\\{(.*)\\}");
	    Matcher matcher = pattern.matcher(json);
	    Map<String,String> props=new HashMap<>();
    	while(matcher.find()){
    		String clz=matcher.group(1);
    		props.put("clazz",clz);
    		for(String item:matcher.group(2).split(",\\s")){
    			try{
    				props.put(item.split("=")[0],item.split("=")[1]);
    			}catch(Exception e){};
    		}
    	}
    	return props;
	}
	// 通过组件对象获取组件信息
	BundleInfo BleInfo(Bundle bundle){
		
		try{
			BundleInfo bleInfo=new BundleInfo();
			
			bleInfo.id=bundle.getBundleId()+"";
			bleInfo.name=BundleUtil.name(bundle.getSymbolicName());
			bleInfo.version=BundleUtil.version(bundle.getVersion().toString());
			bleInfo.status=bundle.getState()+"";
			bleInfo.location=bundle.getLocation();
			
			ServiceReference[] list=null;
			try{
				// 获取组件中的由@component标志 的类，即是service
				list=bundle.getRegisteredServices();
			}catch(Exception e){};
			if(list!=null){
				// 添加组件对应的服务信息
				for(ServiceReference srv :list){
					ServiceInfo service=SrvInfo(srv);
					if(service!=null){
						bleInfo.services.add(service);
					}
				}
			}
			return bleInfo;
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	 }
	
	// 其他接口
	@Override
	public BundleContext getContext(){
		return context;
	}
	ConfigurationAdmin cm=null;
	@Reference void config(ConfigurationAdmin cm) {
		this.cm=cm;
		//Config.update(cm);
	}
	// 读取系统配置
	@Override
	public String getConf(String key,String def){
		return Config.get(key,def);
	}
	// 设置系统配置
	@Override
	public String setConf(String key,String val){
		String old=Config.get(key);
		Config.set(key,val);
		return old;
	}
	// 显示系统设置
	public List<ConfigInfo> listConf(){
		List<ConfigInfo> list=new ArrayList<>();
		Config.config.forEach((key,val)->{
			if(key.toString().startsWith("os.")){
				list.add(new ConfigInfo(key.toString(),val.toString()));
			}
		});
		list.sort((o1,o2)->{
			return o1.key.compareTo(o2.key);
		});
		return list;
	}
	
	// 其他接口
	StartLevel startLevel=null;
	PackageAdmin pageAdmin=null;
	// 注入
	@Reference void setStartLevel(StartLevel startLevel){
		this.startLevel=startLevel;
	}
	// 注入
	@Reference void setPackageAdmin(PackageAdmin pageAdmin){
		this.pageAdmin=pageAdmin;
	}
	@Override
	public List<Bundle> refresh(String...args){
		if(args.length==0){
			pageAdmin.refreshPackages(null);
			return null;
		}else{
			List<Bundle> bundles = new ArrayList<Bundle>();
			for(String arg:args){
				bundles.addAll(search(arg));
			}
			pageAdmin.refreshPackages(bundles.toArray(new Bundle[bundles.size()]));
			return bundles;
		}
	}
	@Override
	public List<Bundle> resolve(String...args){
		if(args.length==0){
			pageAdmin.resolveBundles(null);
			return null;
		}else{
			List<Bundle> bundles = new ArrayList<Bundle>();
			for(String arg:args){
				bundles.addAll(search(arg));
			}
			pageAdmin.resolveBundles(bundles.toArray(new Bundle[bundles.size()]));
			return bundles;
		}
	}
	@Override
	public String startLevel(String... args){
		if(args==null||args.length==0){
			int level=startLevel.getStartLevel();
			return level+"";
		}else{
			startLevel.setStartLevel(Integer.parseInt(args[0]));
			return null;
		}
		
	}
	@Override
	public String bundleLevel(String... args){
		
		if(args==null||args.length==0) return null;
		if(args.length==1){
			String args1=args[0].toString();
			Bundle bundle=search(args1).get(0);
			int level=startLevel.getBundleStartLevel(bundle);
			return level+"";
		}
		if(args.length==2){
			String args1=args[0].toString();
			String args2=args[1].toString();
			if(args1.equals("-i")){
				startLevel.setInitialBundleStartLevel(Integer.parseInt(args2));
				return null;
			}else{
				Bundle bundle=search(args1).get(0);
				startLevel.setBundleStartLevel(bundle,Integer.parseInt(args2));
				return null;
			}
		}
		return null;
	}
	@Override
	public Bundle install(String location,InputStream input) throws BundleException{
		return context.installBundle(location, input);
	}
	
}
