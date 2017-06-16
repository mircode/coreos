package os.core.provider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import os.core.api.CoreOS;
import os.core.rmt.TelnetServer;
import os.core.tools.HostUtil;

/**
 * 远程管理接口
 */
@Component(name="os.rmt",service=CoreRmt.class,immediate=true)
public class CoreRmt {
	
	CoreOS coreos=null;
	TelnetServer server=null;
	
	@Reference void setCoreOS(CoreOS coreos){
		this.coreos=coreos;
	}
	
	@Activate void start() {
		int telnet_port=Integer.parseInt(HostUtil.socket_port());
		this.server=new TelnetServer(telnet_port,coreos);
		this.server.start();
	}

	@Deactivate void close() {
		try {
			this.server.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
