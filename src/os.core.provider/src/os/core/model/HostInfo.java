package os.core.model;

import org.osgi.dto.DTO;

/**
 * 主机信息Model
 * @author admin
 *
 */
public class HostInfo extends DTO{

	// 主机名
	public String hostname;
	// 主机IP
	public String ip;
	// 主机通讯端口
	public String port;
	// 主机状态
	public String status;
	
	public HostInfo(){};
	public HostInfo(String ip,String port,String hostname){
		this.ip=ip;
		this.port=port;
		this.hostname=hostname;
	}
	
	@Override  
	public boolean equals(Object other) {
		HostInfo o=null;
		if(other instanceof HostInfo){
			o=(HostInfo)(other);
		}
		String key=ip+port;
		return key.equals(o.ip+o.port);
	}
	@Override  
    public int hashCode() {
		String key=ip+port;
		return key.hashCode();
    }  
	
}
