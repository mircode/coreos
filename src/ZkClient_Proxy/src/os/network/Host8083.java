package os.network;

import os.network.coreos.CoreOS;
import os.network.provider.NetworkImpl;


public class Host8083 {
	public void start(String addr){
		new NetworkImpl(addr);
	}
	public static void main(String args[]) throws Exception{
		CoreOS coreos=new CoreOS("localhost:8083");
		System.out.println("Ö÷»ú8083Æô¶¯");
		coreos.shell();
	}
}
