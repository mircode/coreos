package os.core.rmt;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import os.core.api.CoreOS;
public class TelnetServer extends Thread{

	private int port=7070;
	private int max_conn=100;
	private ServerSocket server=null;
	private List<TelnetClient> connects=new ArrayList<>();

	private CoreOS coreos;
	public TelnetServer(){}
	public TelnetServer(int port){
		this.port=port;
	}
	public TelnetServer(int port,CoreOS coreos){
		this.port=port;
		this.coreos=coreos;
	}
	public TelnetServer(int port,int max_coun,CoreOS coreos){
		this.port=port;
		this.max_conn=max_coun;
		this.coreos=coreos;
	}
	public void run(){
		try{
			this.listen();// 启动监听
		}catch(Exception ex){}
	}
	public void shutdown() throws InterruptedException {
	    this.interrupt();
	    try {
	      server.close();
	    } catch (IOException e) {
	    }
	    this.join();
	}
	public void listen() throws Exception{
		server=new ServerSocket(port);
		while(!Thread.interrupted()){
			  Socket client=null;
			  try{
		    	  client = server.accept();
		      }catch (IOException ex){
		        if(Thread.interrupted()){
		          break;
		        }
		        throw new RuntimeException(ex);
		      }
		      synchronized(connects){
			      if (connects.size()>=max_conn) {
			    	  client.close();
			          continue;
			        }
			  }
		      // 添加到连接队列
		      try{
		    	InputStream input=client.getInputStream();
			    OutputStream output=client.getOutputStream();
			    Thread.sleep(100);
			    if(input.available()!=0){
			    	new TelnetClient(new WebSocketShell(input,output,coreos),client).start();
			    }else{
			    	new TelnetClient(new TelnetShell(input,output,coreos),client).start();
			    }
		      }catch(Exception e){
		        e.printStackTrace();
		        try {
		          client.close();
		        } catch (IOException ex) {
		        }
		        continue;
		      }
		}
		
		//　关闭所有连接
		List<TelnetClient> threads;
	    synchronized(connects){
	      threads = new ArrayList<TelnetClient>(connects);
	    }
	    for(TelnetClient thread : threads){
	      try{
	    	  thread.stopAndWait();
	      }catch(Exception e){}
	    }
	    Thread.currentThread().interrupt();
	}
	// 内部类
	class TelnetClient extends Thread {
	    private Socket client;
	    public TelnetClient(TelnetShell shell,Socket client) {
	    	super(shell);
	    	this.client= client;
	    }
	    public void start() {
	      synchronized(connects) {
	    	  connects.add(this);
	      }
	      super.start();
	    }
	    
	    public void run(){
	      try{
	        super.run();
	      }finally{
	        synchronized(connects) {
	        	try{
					client.close();
				}catch (IOException e){}
	        	connects.remove(this);
	        }
	      }
	    }
	    // 关闭客户端连接
	    public void stopAndWait() throws InterruptedException {
	      if(!isAlive()) return;
	      interrupt();
	      try{
	    	  client.close();
	      }catch(IOException e){}
	      join();
	    }
	}
	
	public static void main(String args[]) throws InterruptedException{
		TelnetServer server=new TelnetServer(8090);
		server.start();
		while(true);
		//System.out.println("启动监听");
		//Thread.sleep(30000);
		//server.shutdown();
		//System.out.println("终止监听");
	}
}
