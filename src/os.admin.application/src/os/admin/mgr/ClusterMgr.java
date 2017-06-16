package os.admin.mgr;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

import os.core.model.BundleInfo;
import os.core.model.HostInfo;
import os.core.model.ServiceInfo;

/**
 * 集群管理-命令接口
 * @author admin
 *
 */
@SuppressWarnings({ "rawtypes" ,"unchecked"})
public class ClusterMgr {

	private NetworkWrapper network;
	private BundleMgr manager;
	
	// 输入流
	PrintStream out=System.out;
	public void setOut(PrintStream out){
		this.out=out;
		this.manager.setOut(out);
	}
	public ClusterMgr(NetworkWrapper network){
		this.network=network;
		// 组件管理:安装,卸载等操作
		// 节点管理:节点信息,组件信息,服务信息
		this.manager=new BundleMgr(network);
	}
	public void call(String addr,String namespace,String name,Object... args){
		Object res=this.exec(addr,namespace, name, args);
		if(res!=null){
			// 如果不是测试代码返回结果,则打印
			if(!res.toString().equals("DEBUG")){
				out.println("result:"+res);
			}
		}else{
			out.println("result:null");
		}
	}
	public void call(String namespace,String name,Object... args){
		Object res=this.exec(namespace, name, args);
		if(res!=null){
			// 如果不是测试代码返回结果,则打印
			if(!res.toString().equals("DEBUG")){
				out.println("result:"+res);
			}
		}else{
			out.println("result:null");
		}
	}
	
	
	public void services(){
		services("^os[.].*");
	}
	public void services(String filter){
		List<ServiceInfo> res=manager.getServices();
		if(res==null||res.size()==0){return;};
		List<String> infos=new ArrayList<>();
		infos.add("id|ip|port|name|status|methods");
		for(int i=0;i<res.size();i++){
			ServiceInfo srvInfo=res.get(i);
			String methods="[";
			if(srvInfo.methods!=null&&srvInfo.methods.size()>0){
				for(int j=0;j<4;j++){
					if(j<srvInfo.methods.size()){
						String m=srvInfo.methods.get(j);
						methods+=m+",";
					}
					
				}
			}
			methods=methods.replaceAll(",$","");
			methods+="]";
			if(filter.equals("all")){
				infos.add(String.format("%s|%s|%s|%s|%s|%s",i+1,srvInfo.ip,srvInfo.port,srvInfo.name,srvInfo.status,methods));
			}else{
				
				// 查找制定主机服务
				if(filter.contains(":")){
					String ip=filter;
					String port="8080";
					if(filter.contains(":")){
						ip=filter.split(":")[0];
						port=filter.split(":")[1];
					}
					if(srvInfo.ip.equals(ip)&&srvInfo.port.equals(port)&&srvInfo.name!=null&&srvInfo.name.matches("^os[.].*")){
						infos.add(String.format("%s|%s|%s|%s|%s|%s",i+1,srvInfo.ip,srvInfo.port,srvInfo.name,srvInfo.status,methods));
					}
				// 查找
				}else{
					if(srvInfo.name!=null&&srvInfo.name.matches(filter)){
						infos.add(String.format("%s|%s|%s|%s|%s|%s",i+1,srvInfo.ip,srvInfo.port,srvInfo.name,srvInfo.status,methods));
					}
				}
			}
		}
		print(infos);
	}
	public void inspect(String service) {
		List<ServiceInfo> res=manager.getServices();
		if(res==null||res.size()==0){return;};
		List<String> infos=new ArrayList<>();
		infos.add("id|ip|port|name|status|methods");
		for(int i=0;i<res.size();i++){
			ServiceInfo srvInfo=res.get(i);
			if(srvInfo.name!=null&&srvInfo.name.equals(service)){
				if(srvInfo.methods!=null&&srvInfo.methods.size()>0){
					for(int j=0;j<srvInfo.methods.size();j++){
						String m=srvInfo.methods.get(j);
						infos.add(String.format("%s|%s|%s|%s|%s|%s",i+1,srvInfo.ip,srvInfo.port,srvInfo.name,srvInfo.status,m));
					}
				}
				break;
			}
		}
		print(infos);
	}
	public void bundles(){
		bundles("^os[.].*");	
	}
	public void bundles(String filter){
		List<BundleInfo> res=manager.getBundles();
		if(res==null||res.size()==0){return;};
		List<String> infos=new ArrayList<>();
		infos.add("id|ip|port|name|version|status");
		for(int i=0;i<res.size();i++){
			BundleInfo bundle=res.get(i);
			int type=Integer.parseInt(bundle.status);
			 
			String status="STARTING";
			if(type==Bundle.INSTALLED){
				status="INSTALLED";
			}
			if(type==Bundle.UNINSTALLED){
				status="UNINSTALLED";
			}
			if(type==Bundle.RESOLVED){
				status="RESOLVED";
			}
			if(type==Bundle.STARTING){
				status="START...";
			}
			// 停止中
			if(type==Bundle.STOPPING){
				status="STOP...";
			}
			// 启动成功
			if(type==Bundle.ACTIVE){
				status="STARTING";
			}
			String name=bundle.name;
			String version=bundle.version;
			if(bundle.version.split("[.]").length>3){
				int index=bundle.version.lastIndexOf(".");
				version=bundle.version.substring(0, index);
			}
			if(filter.equals("all")){
				infos.add(String.format("%s|%s|%s|%s|%s|%s",bundle.id,bundle.ip,bundle.port,name,version,status));
			}else{
				// 查找制定主机组件
				if(filter.contains(":")){
					String ip=filter;
					String port="8080";
					if(filter.contains(":")){
						ip=filter.split(":")[0];
						port=filter.split(":")[1];
					}
					if(bundle.ip.equals(ip)&&bundle.port.equals(port)&&bundle.name.matches("^os[.].*")){
						if(!status.equals("UNINSTALLED")){
							infos.add(String.format("%s|%s|%s|%s|%s|%s",bundle.id,bundle.ip,bundle.port,name,version,status));
						}
					}
				// 查找
				}else{
					if(bundle.name!=null&&bundle.name.matches(filter)){
						if(!status.equals("UNINSTALLED")){
							infos.add(String.format("%s|%s|%s|%s|%s|%s",bundle.id,bundle.ip,bundle.port,name,version,status));
						}
					}
				}
			}
		}
		print(infos);
	}
	
	public void nodes(){
		//HostInfo host=net.getHostInfo();调用网卡对象主机信息函数
		List<HostInfo> res=manager.getNodes();
		if(res==null||res.size()==0){return;};
		List<String> infos=new ArrayList<>();
		infos.add("id|hostname|ip|port|status");
		for(int i=0;i<res.size();i++){
			HostInfo route=res.get(i);
			infos.add(String.format("%d|%s|%s|%s|%s",i+1,route.hostname,route.ip,route.port,"running"));
		}
		print(infos);
		
	}
	//设置打印的格式
	void print(List<String> lines){
		if(lines==null||lines.size()==1){
			System.out.println("empty data to print");
			return;
		}
		//设置每列的最大长度
		List<Integer> maxlen=new ArrayList<>();
		for(String row:lines){
			String args[]=row.split("[|]");
			for(int i=0;i<args.length;i++){
				int len=args[i].getBytes().length;
				//第一行没有maxlength
				if(maxlen.size()<args.length){
					maxlen.add(len);
				}else{
					//比上一行该列的长度长就修改maxlen的值，并加两个长度
					Integer max=maxlen.get(i);
					if(max<len){
						maxlen.set(i,len+2);
					}
				}
			}
		}
		//返回remove删除掉的第一行的字符串即表头
		String header=lines.remove(0);
		
		String line_fmt="";
		List<String> chs=new ArrayList<>();
		for(int i=0;i<header.split("[|]").length;i++){
			line_fmt+="+"+"%-"+maxlen.get(i)+"s";
			String res="";
			for(int j=0;j<maxlen.get(i);j++){
				res+="-";
			}
			chs.add(res);
		}
		
		// 打印
		stdout(line_fmt+"+",chs.toArray());
		String fields[]=header.split("[|]");
		String res="|";
		for(int i=0;i<fields.length;i++){
			
			int num=maxlen.get(i)-fields[i].getBytes().length;
			String blank="";
			for(int b=0;b<num;b++){
				blank+=" ";
			}
			res+=fields[i]+blank+"|";
		}
		stdout("%s",new Object[]{res});
		stdout(line_fmt+"+",chs.toArray());
		
		// 打印
		for(String line:lines){
			fields=line.split("[|]");
			res="|";
			for(int i=0;i<fields.length;i++){
				
				int num=maxlen.get(i)-fields[i].getBytes().length;
				String blank="";
				for(int b=0;b<num;b++){
					blank+=" ";
				}
				res+=fields[i]+blank+"|";
			}
			stdout("%s",new Object[]{res});
		}
		stdout(line_fmt+"+",chs.toArray());
	}
	
	void stdout(String format,Object[] args){
		out.println(String.format(format, args));
	}
	
	public void install(String addr,String location){
		this.manager.install(addr,location);
	}
	public void install(String location,long num){
		this.manager.install(location, num);
	}
	
	public void start(String addr,String nameVersion){
		this.manager.start(addr,nameVersion);
	}
	public void start(String nameVersion){
		this.manager.start(nameVersion);
	}
	
	public void stop(String addr,String nameVersion){
		this.manager.stop(addr,nameVersion);
	}
	public void stop(String nameVersion){
		this.manager.stop(nameVersion);
	}
	
	public void uninstall(String addr,String nameVersion){
		this.manager.uninstall(addr,nameVersion);
	}
	public void uninstall(String nameVersion){
		this.manager.uninstall(nameVersion);
	}
	
	public void update(String addr,String nameVersion){
		this.manager.update(addr,nameVersion);
	}
	public void update(String nameVersion){
		this.manager.update(nameVersion);
	}
	public void update(String nameVersion,Long time){
		this.manager.update(nameVersion,time);
	}
	public void change(String nameVersion,Long num){
		this.manager.change(nameVersion, num);
	}
	public void move(String nameVersion,String from,String to){
		this.manager.move(nameVersion, from,to);
	}
	public void check() {
		this.manager.check();
	}
	
	
	// 集群信息-Web接口
	public List<ServiceInfo> getServices(){
		return this.manager.getServices();
	}
	public List<BundleInfo> getBundles(){
		return this.manager.getBundles();
	}
	public List<HostInfo> getNodes(){
		return this.manager.getNodes();
	}
	
	
	public Object exec(String namespace,String name,Object... args){
		NetworkWrapper target=this.network;
		if(namespace.equals("os.moudel.person.provider.PersonService")&&name.equals("list")){
			List res=target.call(namespace,name,args);
			debug((List<Map<String,Object>>)res);
			return "DEBUG";
		}
		Object res=target.call(namespace,name,args);
		return res;
	}
	public Object exec(String addr,String namespace,String name,Object... args){
		NetworkWrapper target=this.getTarget(addr);
		if(target==null) return null;
		Object res=target.call(namespace,name,args);
		if(namespace.equals("os.moudel.person.provider.PersonService")&&name.equals("list")){
			debug((List<Map<String,Object>>)res);
			return "DEBUG";
		}
		return res;
	}
	
	public void debug(List<Map<String,Object>> res){
		// 最后一条记录信息为主机IP和端口信息
		String host=res.get(res.size()-1).get("ip:port").toString();
		System.out.println(String.format("%s->执行体脂查询->",host));
		// 删除最后一条信息
		res.remove(res.size()-1);
		
		
		// 获取表头信息
		StringBuilder header=new StringBuilder();
		if(res!=null&&res.size()>0){
			Map row=res.get(0);
			row.forEach((key,val)->{
				header.append(key+"|");
			});
		}
		// 添加表头信息
		List<String> lines=new ArrayList<>();
		lines.add("用户名|脂肪含量|BMI|基础代谢|体脂判断|体型判断|测量时间|报警|医生建议");
		
		String fields[]={"username","zfhl","bmi","jcdx","tzpd","txpd","time","alert","ysjy"};
		// 数据列
		res.forEach(row->{
			StringBuilder line=new StringBuilder();
			for(String f:fields){
				line.append(row.get(f).toString()+"|");
			}
			// 添加数据列
			lines.add(line.toString().replaceAll("[|]$",""));
		});
		print(lines);
	}
	
	NetworkWrapper getTarget(String addr){
		String ip=addr;
		String port="8080";
		if(addr.contains(":")){
			ip=addr.split(":")[0];
			port=addr.split(":")[1];
		}
		for(NetworkWrapper net:network.getRoutes()){
			HostInfo host=net.getHostInfo();
			if(host.ip.equals(ip)&&host.port.equals(port)){
				return net;
			}
		}
		return null;
	}
}
