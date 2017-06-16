package os.admin.mgr;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.osgi.framework.Bundle;

import os.admin.job.CheckJob;
import os.core.model.BundleInfo;
import os.core.model.HostInfo;
import os.core.model.ServiceInfo;
import os.core.tools.BundleUtil;

/**
 * 组件管理对象
 * @author admin
 *
 */
public class BundleMgr {

	private NetworkWrapper network;
	private String NAMESPACE="os.core.provider.CoreShell";
	
	// 输入流
	PrintStream out=System.out;
	public void setOut(PrintStream out){
		this.out=out;
	}
	// 异常恢复
	public Map<String,Long> checktab=CheckJob.checktab;
	// 构造函数
	public BundleMgr(NetworkWrapper network){
		this.network=network;;
	}

	// 全量安装
	public void install(String location){
		install(location,-1L);
	}
	// 指定数目安装
	public void install(String location,Long num){
		
   	    // 组件标识
	  	BundleInfo bundle=BundleUtil.bundleInfo(location);
	    String bdlName=bundle.name;
	  	if(bdlName==null)return;
		// 安装bundle的节点
		List<NetworkWrapper> installNodes=new ArrayList<>();
		// 未安装bundle的节点
		List<NetworkWrapper> unstallNodes=new ArrayList<>();
		
		List<NetworkWrapper> routes=network.getRoutes();
		// 查询安装情况
		for(NetworkWrapper net:routes){
			List<BundleInfo> bdls=net.getBundles();
			int size=search(bdlName,bdls).size();
			if(size>0){
				installNodes.add(net);
			}else{
				unstallNodes.add(net);
			}
		}
		// 全部安装
		if(num<0){
			for(NetworkWrapper net:unstallNodes){
				 install(net,location);
			}
			return;
		}
		long size=installNodes.size();
		int unsize=unstallNodes.size();
		// 扩容
		if(num>size){
			Long len=(num-size)>unsize?unsize:(num-size);
			int old=-1;
			for(int i=0;i<len;i++){
				int index=random(unsize);
				if(index==old){
					index=random(unsize);
					i--;
					continue;
				}
				install(unstallNodes.get(index),location);
				old=index;
			}
		// 缩容
		}else if(num<size){
			long len=size-num;
			for(int i=0;i<len;i++){
				uninstall(installNodes.get(i),bdlName);
			}
		}
	}
	private int random(int range){
		return new Random().nextInt(range);
	}
	// 指定主机安装
	public void install(String addr,String location){
		// 查找目标主机
		NetworkWrapper net=search(addr);
		this.install(net,location);
	}
	// 全量卸载
	public void uninstall(String nameVersion){
		List<NetworkWrapper> routes=network.getRoutes();
		// 查询安装情况
		for(NetworkWrapper net:routes){
			List<BundleInfo> bdls=net.getBundles();
			int size=search(nameVersion,bdls).size();
			if(size>0){
				this.uninstall(net, nameVersion);
			}
		}
	}
	// 指定主机卸载
	public void uninstall(String addr,String nameVersion){
		NetworkWrapper net=search(addr);
		this.uninstall(net,nameVersion);
	}
	
	// 动态扩容
	public void change(String nameVersion,Long num){
		List<NetworkWrapper> routes=network.getRoutes();
		BundleInfo bundle=null;
		// 查询安装情况
		for(NetworkWrapper net:routes){
			List<BundleInfo> bdls=net.getBundles();
			List<BundleInfo> targets=search(nameVersion,bdls);
			int size=targets.size();
			if(size>0){
				bundle=targets.get(0);
				break;
			}
		}
		if(bundle!=null){
			String location=bundle.location;
			this.install(location, num);
			this.start(nameVersion);
			
			this.checktab.put(bundle.name, num);
		// hock
		}else{
			// 通过nameVersion获取jar包安装路径
			String location=BundleUtil.fullName(nameVersion)+".jar";
			this.install(location, num);
			this.start(nameVersion);
			
			bundle=BundleUtil.bundleInfo(location);
			this.checktab.put(bundle.name, num);
		}
	}
	
	// 动态迁移
	public void move(String nameVersion,String from,String to){
		String location=BundleUtil.bundlePath(nameVersion);
		this.install(to,location);
		this.start(to,nameVersion);
		this.uninstall(from,nameVersion);
	}
	
	// 全量启动
	public void start(String nameVersion){
		this.execute("start",nameVersion);
	}
	// 指定主机启动
	public void start(String addr,String nameVersion){
		this.execute("start",addr,nameVersion);
	}
	
	// 全量停止
	public void stop(String nameVersion){
		this.execute("stop",nameVersion);
	}
	// 指定主机停止
	public void stop(String addr,String nameVersion){
		this.execute("stop",addr,nameVersion);
	}

	// 全量更新
	public void update(String nameVersion){
		if(nameVersion.equals("stop")){
			status="stop";
			if(update_bundle!=null){
				System.out.println(String.format("%s->升级任务->终止成功",update_bundle));
			}
			while(thread.isAlive()){
				thread.interrupt();
				try {
					Thread.sleep(200);
				} catch (Exception e) {
				}
			}
			thread=null;
			
		}else if(nameVersion.equals("start")){
			status="start";
			if(update_bundle!=null){
				System.out.println(String.format("%s->升级任务->启动成功",update_bundle));
			}
		}else if(nameVersion.equals("pause")){
			status="pause";
			if(update_bundle!=null){
				System.out.println(String.format("%s->升级任务->暂停成功",update_bundle));
			}
		}else if(nameVersion.equals("status")){
			if(update_bundle!=null){
				if(status.equals("start")){
					System.out.println(String.format("%s->升级任务->进行中...",update_bundle));
				}
				if(status.equals("pause")){
					System.out.println(String.format("%s->升级任务->已暂停",update_bundle));
				}
			}else{
				System.out.println("暂无升级任务");
			}
		}else{
			this.update(nameVersion,10L);
		}
	}
	// 升级状态
	public static String status=null;
	// 升级线程
	public static Thread thread=null;
	// 正在升级的组件
	public static String update_bundle=null;
	public void update(String nameVersion,Long time){
		// 存在升级任务
		if(thread!=null&&thread.isAlive()){
			if(status.equals("start")){
				System.out.println(String.format("%s->升级任务->进行中...",update_bundle));
			}
			if(status.equals("pause")){
				System.out.println(String.format("%s->升级任务->已暂停",update_bundle));
			}
			return;
		}
		// 启动升级
		status="start";
		update_bundle=nameVersion;
		thread=new Thread(new Runnable(){
			public void run() {
				List<NetworkWrapper> routes=network.getRoutes();
				for(int i=0;i<routes.size();i++){
					NetworkWrapper net=routes.get(i);
					// 暂停
					while(status.equals("pause")){
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
							break;
						}
					}
					// 终止
					if(status.equals("stop")){
						break;
					}
					// 升级
					try{
						List<BundleInfo> bdls=net.getBundles();
						int size=search(nameVersion,bdls).size();
						if(size>0){
							execute("update",net,nameVersion);
						}
					}catch(Exception e){
						break;
					}
					// 升级完成
					if(i==(routes.size()-1)){
						break;
					}
					// 间隔
					try{
						Thread.sleep(time*1000);
					} catch (Exception e) {
						break;
					}
					
				}
				update_bundle=null;
			}
			
		});
		thread.start();
		
	}
	// 指定主机更新
	public void update(String addr,String nameVersion){
		this.execute("update",addr,nameVersion);
	}
	
	// 异常恢复
	public void check() {
		// 查询安装情况
		checktab.forEach((name,num)->{
			int count=0;
			List<NetworkWrapper> routes=network.getRoutes();
			for(NetworkWrapper net:routes){
				List<BundleInfo> bdls=net.getBundles();
				int size=search(name,bdls).size();
				count+=size;
			}
			if(count<num){
				System.out.println(String.format("%s->%s->%s","异常恢复:"+name,count,num));
				this.change(name,num);
			}
		});
	}
	
	// 查询接口
	public List<ServiceInfo> getServices(){
		List<ServiceInfo> res=new ArrayList<>();
		for(NetworkWrapper net:network.getRoutes()){
			List<ServiceInfo> services=net.getServices();
			if(services!=null){
				res.addAll(services);
			}
		}
		return res;
	}
	public List<BundleInfo> getBundles(){
		List<BundleInfo> res=new ArrayList<>();
		for(NetworkWrapper net:network.getRoutes()){
			List<BundleInfo> bundles=net.getBundles();
			if(bundles!=null){
				res.addAll(net.getBundles());
			}
		}
		return res;
	}
	public List<HostInfo> getNodes(){
		List<HostInfo> res=new ArrayList<>();
		for(NetworkWrapper net:network.getRoutes()){
			HostInfo host=net.getHostInfo();
			if(host!=null){
				res.add(host);
			}
		}
		return res;
	}
	
	// 工具函数
	private void install(NetworkWrapper net,String location){
		BundleInfo bundle=BundleUtil.bundleInfo(location);
		
		if(bundle==null)return;
		
		String name=bundle.name;
	    String version=bundle.version;
	    String nameVersion=name+":"+version;
	  	
		if(net!=null){
			List<BundleInfo> bdls=search(nameVersion,net.getBundles());
			if(bdls.size()<=0){
				net.call(NAMESPACE, "install", location);
				add(name,1L);
				
				// 打印非本机安装信息
				if(!network.equals(net)){
					DateFormat format=new SimpleDateFormat("yyyyMMdd HH:mm:ss");
					String time=format.format(new Date());
					out.println(String.format("[%s]->[%s:%s-%s]->[%s:%s]->[success]",time,net.getHostInfo().ip,net.getHostInfo().port,"install",bundle.name,bundle.version));
				}
			}
		}
	}
	private void uninstall(NetworkWrapper net,String nameVersion){
		if(net!=null){
			List<BundleInfo> bdls=search(nameVersion,net.getBundles());
			if(bdls.size()>0){
				net.call(NAMESPACE, "uninstall",nameVersion);
				String name=bdls.get(0).name;
				//String version=bdls.get(0).version;
				add(name,-1L);
				
				// 打印非本机安装信息
				if(!network.equals(net)){
					DateFormat format=new SimpleDateFormat("yyyyMMdd HH:mm:ss");
					String time=format.format(new Date());
					out.println(String.format("[%s]->[%s:%s-%s]->[%s]->[success]",time,net.getHostInfo().ip,net.getHostInfo().port,"uninstall",nameVersion));
				}
			}
		}
	}
	// 全量操作
	private void execute(String action,String nameVersion){
		List<NetworkWrapper> routes=network.getRoutes();
		// 查询安装情况
		for(NetworkWrapper net:routes){
			List<BundleInfo> bdls=net.getBundles();
			int size=search(nameVersion,bdls).size();
			if(size>0){
				this.execute(action,net,nameVersion);
			}
		}
	}
	// 指定主机操作
	private void execute(String action,String addr,String nameVersion){
		NetworkWrapper net=search(addr);
		this.execute(action,net,nameVersion);
	}
	// 执行操作
	private Bundle execute(String action,NetworkWrapper net,String nameVersion){
		if(net!=null){
			
			// hock
			List<BundleInfo> bdls=net.getBundles();
			List<BundleInfo> targets=search(nameVersion,bdls);
			if(targets.size()>0){
				// hock
				if(action.equals("start")){
					int type=Integer.parseInt(targets.get(0).status);
					// 如果是已启动状态则返回
					if(type==Bundle.ACTIVE){
						return null;
					}
				}
			}
			
			
			net.call(NAMESPACE,action, nameVersion);
			// 打印非本机安装信息
			if(!network.equals(net)){
				DateFormat format=new SimpleDateFormat("yyyyMMdd HH:mm:ss");
				String time=format.format(new Date());
				out.println(String.format("[%s]->[%s:%s-%s]->[%s]->[success]",time,net.getHostInfo().ip,net.getHostInfo().port,action,nameVersion));
			}
			return null;
		}
		return null;
	}
	// 调整实例数目
	private void add(String key,Long offset){
		key=BundleUtil.nameVersion(key);
		Long num=checktab.get(key);
		if(num!=null){
			checktab.put(key,num+offset);
		}else{
			checktab.put(key,offset);
		}
	}

	// 根据IP和端口查找对应NetworkWrapper对象
	private NetworkWrapper search(String addr){
		String ip=addr;
		String port="8080";
		if(addr.contains(":")){
			ip=addr.split(":")[0];
			port=addr.split(":")[1];
		}
		List<NetworkWrapper> routes=network.getRoutes();
		for(NetworkWrapper net:routes){
			HostInfo host=net.getHostInfo();
			if(host.ip.equals(ip)&&host.port.equals(port)){
				return net;
			}
		}
		return null;
	}
	// 过滤指定组件对象
	private List<BundleInfo> search(String nameVersion,List<BundleInfo> arry){
		String name=BundleUtil.name(nameVersion);
		String version=BundleUtil.version(nameVersion);
		
		List<BundleInfo> bundles=new ArrayList<>();
		for(BundleInfo bundle:arry){
			// 排查卸载的组件
			if(bundle.status.equals("1")){
				continue;
			}
			// 获取组件的简称和版本号
			String bdlName=BundleUtil.name(bundle.name);
			String bdlVerson=BundleUtil.version(bundle.version);
			
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
		return bundles;
	}
	
	
}

