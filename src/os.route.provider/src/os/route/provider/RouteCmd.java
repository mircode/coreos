package os.route.provider;


import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.ZooKeeper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import osgi.enroute.debug.api.Debug;

/**
 * 通过Shell控制路由 
 */
@Component(name="os.routemgr",service = RouteCmd.class, 
			  property = { 
			    Debug.COMMAND_SCOPE + "=route",  
			    Debug.COMMAND_FUNCTION + "=list" 
			  },immediate=true)
public class RouteCmd {
	
	// 输出流
	private ThreadLocal<PrintStream> outMap=new ThreadLocal<>();
	public void setOut(PrintStream out){
		outMap.set(out);
	}
	public PrintStream getOut(){
		PrintStream out=outMap.get();
		if(out==null){
			return System.out;
		}else{
			return out;
		}
	}
	private ZooKeeper zk;

	@Activate
	void activate() throws IOException {
		this.zk = new ZooKeeper("localhost:6789",10000,null);
	}

	@Deactivate
	void deactivate() throws Exception {
		this.zk.close();
	}
	// 路由信息
	public void list() throws Exception {
		List<String> lists=zk.getChildren("/route", false);
		List<String> infos=new ArrayList<>();
		infos.add("ip|port|path");
		for(String path:lists){
			byte[] data = zk.getData("/route/"+path, false, null);
			String info=new String(data, "UTF-8");
			info=info.replace("http://","").replace("/table/","");
			infos.add(info.replace(":","|")+"|table");
		}
		print(infos);
	}
	void print(List<String> lines){
		if(lines==null||lines.size()==1){
			System.out.println("empty data to print");
			return;
		}
		List<Integer> maxlen=new ArrayList<>();
		for(String row:lines){
			String args[]=row.split("[|]");
			for(int i=0;i<args.length;i++){
				int len=args[i].length();
				if(maxlen.size()<args.length){
					maxlen.add(len);
				}else{
					Integer max=maxlen.get(i);
					if(max<len){
						maxlen.set(i,len+2);
					}
				}
			}
		}
		String header=lines.remove(0);
		String data_fmt="";
		String line_fmt="";
		List<String> chs=new ArrayList<>();
		for(int i=0;i<header.split("[|]").length;i++){
			data_fmt+="|"+"%-"+maxlen.get(i)+"s";
			line_fmt+="+"+"%-"+maxlen.get(i)+"s";
			String res="";
			for(int j=0;j<maxlen.get(i);j++){
				res+="-";
			}
			chs.add(res);
		}
		
		stdout(line_fmt+"+",chs.toArray());
		stdout(data_fmt+"|",header.split("[|]"));
		stdout(line_fmt+"+",chs.toArray());
		for(String line:lines){
			stdout(data_fmt+"|",line.split("[|]"));	
		}
		stdout(line_fmt+"+",chs.toArray());
	}
	void stdout(String format,Object[] args){
		getOut().println(String.format(format, args));
	}
}
