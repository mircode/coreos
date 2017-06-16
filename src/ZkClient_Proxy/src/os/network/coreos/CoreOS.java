package os.network.coreos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import os.network.api.Network;
import os.network.provider.NetworkImpl;


public class CoreOS {
	Network network=null;
	public CoreOS(String addr){
		this.network=new NetworkImpl(addr);
		((NetworkImpl)network).connect();
	}
	@SuppressWarnings("unchecked")
	public <T> T call(String addr,String namespace,String method,Object[] args){
		String local=network.getHostInfo();
		// 本地查找
		if(addr.equals(local)){
			return (T)this.network.call(namespace, method, args);
		}
		// 远程调用
		NetworkImpl localnetwork=(NetworkImpl)network;
		
		for(Network net:localnetwork.routes){
			String rtm=net.getHostInfo();
			if(rtm.equals(addr)){
				return (T)net.call(namespace, method, args);
			}
		}
		return null;
	}
	public void shell() throws Exception{
		PrintStream out=System.out;
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
		while(true){
			out.print("$>> ");
			String cmd=in.readLine();
			cmd=cmd.replaceAll(";$","").replaceAll("\\s+"," ").replaceAll("^\\s+|\\s+$","");
			if(cmd==null) continue;
			if (cmd.equals("exit")){
				break;
			}
			Object res=execute(cmd);
			if(res!=null)
				out.println(res);
		}
	}
	public Object execute(String cmd){
		Object obj=null;
		String args[]=cmd.split("\\s+");
		if(args.length>0&&args[0].equals("call")){
			List<Object> params=new ArrayList<>();
			for(int i=4;i<args.length;i++){
				params.add(args[i]);
			}
			obj=call(args[1],args[2],args[3],params.toArray());
		}
		return obj;
	}
}
