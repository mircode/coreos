package os.core.rmt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import os.core.api.CoreOS;

// 远程交互Shell
@SuppressWarnings({"rawtypes","unchecked"})
public class TelnetShell implements Runnable {
	
	// 输入流
	public BufferedReader in;
	// 输出流
	public PrintStream out;
	// 内核
	public CoreOS coreos;
	
	// 版本
	public static String version;
	static{
		version=version();
	}
	public static String version(){
	    InputStream input = TelnetShell.class.getResourceAsStream("version.txt");
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder builder = new StringBuilder();
        String line = null;
        try{
            while((line = reader.readLine()) != null){
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
                //builder.append("\n");
            }
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            try{
            	input.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        return builder.toString();
	}
	public TelnetShell(){}
	public TelnetShell(InputStream in,OutputStream out,CoreOS coreos){
		this.in=new BufferedReader(new InputStreamReader(in));
		this.out= new PrintStream(out);
		this.coreos=coreos;
	} 
	
	public void run() {
		this.out.print(version);
		this.out.println();
		// 如果当前线程未被中断
		while(!Thread.currentThread().isInterrupted()){
			this.out.print("$>> ");
			String cmd;
			try{
				cmd=this.in.readLine();
				if(cmd!=null){
					cmd=cmd.replaceAll(";$","").replaceAll("\\s+"," ").replaceAll("^\\s+|\\s+$","");
				}else{
					continue;
				}
			}catch(IOException ex){
				if (!Thread.currentThread().isInterrupted()){
					ex.printStackTrace(out);
					out.println("Unable to read from stdin - exiting now");
				}
				return;
			}
			if (cmd.equals("exit")){
				out.println("exit");
				out.close();
				return;
			}
			try{
				execute(cmd);
			}catch(Exception t) {
				t.printStackTrace(out);
			}
		}
	}
	public void execute(String cmd) throws Exception{
		String args[]=cmd.split("\\s+");
		// 获取目标shell
		Object shell=null;
		String scope=null;
		String method=args[0];
		if(args[0].contains(":")){
			scope=args[0].split(":")[0];
			method=args[0].split(":")[1];
			shell=getService(scope);
		}else{
			shell=getService(null);
		}
		
		Class<?>[] types=new Class<?>[args.length>1?args.length-1:0];
		List<Object> params=new ArrayList<>();
		if(args!=null&&args.length>1){
			if(method.equals("call")){
				int j=0;
				types=new Class<?>[3];
				types[j++]=args[1].getClass();
				types[j++]=args[2].getClass();
				types[j++]=Object[].class;
				
				params.add(args[1]);
				params.add(args[2]);
				List<Object> others=new ArrayList<>();
				for(int i=3;i<args.length;i++){
					others.add(args[i]);
				}
				params.add(others.toArray());
				
			}else{
				int j=0;
				for(int i=1;i<args.length;i++){
					types[j++] = args[i].getClass();
					params.add(args[i]);
				}
			}
		}
		
		// 调用CoreShell方法
		Method func=null;
		try{
			func=shell.getClass().getMethod(method,types);
		}catch(Exception e){}
		
		if(func!=null){
			func.invoke(shell, params.toArray());
		}
	}
	public Object getService(String scope){
		try {
			BundleContext context=this.getCoreos().getContext();
			ServiceReference[] refs=context.getAllServiceReferences(null, "(osgi.command.scope=*)");
			
			ServiceReference target=null;
			ServiceReference core_ref=null;
			ServiceReference root_ref=null;
			for(ServiceReference ref:refs){
				String value=ref.getProperty("osgi.command.scope").toString();
				if(scope!=null&&value.equals(scope)){
					target=ref;
					break;
				}else{
					if(value.equals("root")){
						root_ref=ref;
					}
					if(value.equals("core")){
						core_ref=ref;
					}
				}
			}
			if(target==null){
				if(core_ref!=null){
					target=core_ref;
				}
				if(root_ref!=null){
					target=root_ref;
				}
			}
			if(target!=null){
				Object obj=context.getService(target);
				if(obj!=null){
					Method m=obj.getClass().getMethod("setOut",PrintStream.class);
					m.invoke(obj,this.getOut());
				}
				return obj;
			}else{
				return null;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
	}
	
	public PrintStream getOut() {
		return out;
	}
	public void setOut(PrintStream out) {
		this.out = out;
	}
	public CoreOS getCoreos() {
		return coreos;
	}
	public void setCoreos(CoreOS coreos) {
		this.coreos = coreos;
	}
	public static void main(String args[]) throws Exception{
		new Thread(new TelnetShell(System.in,System.out,null)).start();
	}
}
