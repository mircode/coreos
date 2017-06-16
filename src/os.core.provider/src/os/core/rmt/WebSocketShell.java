package os.core.rmt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import os.core.api.CoreOS;
import os.core.zcode.BASE64Encoder;

// 远程交互Shell
public class WebSocketShell extends TelnetShell implements Runnable{

	// 输入流
	public InputStream in;
	// 输出流
	public PrintStream out;
	// 内核
	public CoreOS coreos;
		
	// 协议头
	public byte flag=-127;
	public WebSocketShell(InputStream in, OutputStream out, CoreOS coreos) {
		this.in=in;
		this.out=new WebSocketPrintStream(out);
		this.coreos=coreos;
		super.setCoreos(coreos);
		super.setOut(this.out);
	}
	public void run() {
		int count=-1;
		byte[] buff=new byte[3072];
		
		// 握手
		try{
			count=in.read(buff);
			String header=new String(buff,0,count);
		    ((WebSocketPrintStream)out).write(header(header).getBytes());
		}catch(Exception e){
			return;
		}
		
		this.out.print(version);
		this.out.println();
		// 如果当前线程未被中断
		while(!Thread.currentThread().isInterrupted()){
			this.out.print("$>> ");
			String cmd;
			try{
				count=in.read(buff);
	        	cmd=readLine(buff,count);
				if(cmd!=null){
					cmd=cmd.replaceAll(";$","").replaceAll("\\s+"," ").replaceAll("^\\s+|\\s+$","");
				}else{
					continue;
				}
			}catch(Exception ex){
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
	// 生成握手Header信息
	public String header(String req) throws Exception{
		StringBuffer header=new StringBuffer();
		header.append("HTTP/1.1 101 Switching Protocols\r\n");
		header.append("Upgrade: websocket\r\n");
		header.append("Connection: Upgrade\r\n");
		header.append("Sec-WebSocket-Accept: "+WebSocketAccept(req)+"\r\n\r\n");
        return header.toString();
	}
	public String readLine(byte[] buff,int count) throws Exception{
		for(int i=0;i<count-6;i++){
            buff[i+6]=(byte)(buff[i%4+2]^buff[i+6]);
        }
		flag=buff[0];
		return new String(buff,6,count-6,"UTF-8");
	}
	private String WebSocketAccept(String req) throws Exception{
    	Pattern p=Pattern.compile("^(Sec-WebSocket-Key:\\s*)(.+)",Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
        Matcher m=p.matcher(req);
        if(m.find()){
            String key=m.group(2);
            key+="258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            
            // SHA加密
            MessageDigest md=MessageDigest.getInstance("SHA-1");
            md.update(key.getBytes("ISO-8859-1"),0,key.length());
            byte[] sha=md.digest();
            
            // BASE64编码
            BASE64Encoder encoder=new BASE64Encoder();
            return encoder.encode(sha);
        }
        return null;
    }
	public class WebSocketPrintStream extends PrintStream{
		OutputStream out=null;
		public WebSocketPrintStream(OutputStream out) {
			super(out);
			this.out=out;
		}
		public void write(byte buf[]) throws IOException {
			this.out.write(buf);
		}
		public void println(){
			String str="\r\n";
			print(str);
		}
		public void println(String str) {
			str+="\r\n";
			print(str);
		}
		public void print(String str){
			int length=str.length();
			byte[] header=null;
			if(length<126){
			    header=new byte[2];
				header[0]=flag;  
				header[1]=(byte)length;
		    } else if(length>125 && length<65536){
		    	header=new byte[4];
		    	header[0]=flag;  
		    	header[1]=(byte)126;
		    	header[2]=(byte)((length>>8) & 0xFF);
		    	header[3]=(byte)(length & 0xFF);
		    }else if(length>65535){
		    	header=new byte[10];
		    	header[0]=flag;  
		        header[1]=(byte)127;
		        header[2]=(byte)((length>>56) & 0xFF);
		        header[3]=(byte)((length>>48) & 0xFF);
		        header[4]=(byte)((length>>40) & 0xFF);
		        header[5]=(byte)((length>>32) & 0xFF);
		        header[6]=(byte)((length>>24) & 0xFF);
		        header[7]=(byte)((length>>16) & 0xFF);
		        header[8]=(byte)((length>>8) & 0xFF);
		        header[9]=(byte)(length & 0xFF);
		    }
			try{
			 out.write(header);
	         out.write(str.getBytes("UTF-8"));
			}catch(Exception e){}
		}
	}
}
