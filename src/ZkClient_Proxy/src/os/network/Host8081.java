package os.network;

import os.network.coreos.CoreOS;


public class Host8081 {
	public static void main(String args[]) throws Exception{
		// call localhost:8081 os.user query who
		CoreOS coreos=new CoreOS("localhost:8081");
		System.out.println("Ö÷»ú8081Æô¶¯");
		coreos.shell();
		System.out.println("exit");
	}
}
