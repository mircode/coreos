package os.core.model;

import java.util.ArrayList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * 组件信息Model
 * @author admin
 */
public class BundleInfo extends DTO{
	
	// 组件ID
	public String id;
	// 组件名称
	public String name;
	// 组件运行级别
	public String level;
	// 组件状态
	public String status;
	// 组件版本
	public String version;
	// 安装路径
	public String location;
	// 主机IP
	public String ip;
	// 主机端口
	public String port;
	
	// 组件所包含的服务信息
	public List<ServiceInfo> services=new ArrayList<>();
	
	@Override  
	public boolean equals(Object other) {
		BundleInfo o=null;
		if(other instanceof BundleInfo){
			o=(BundleInfo)(other);
		}
		if(id!=null){
			return id.equals(o.id);
		}else{
			String key=name+version;
			return key.equals(o.name+o.version);
		}
		
	}
	@Override  
    public int hashCode() {
		if(id!=null){
			return id.hashCode();
		}else{
			String key=name+version;
			return key.hashCode();
		}
		
    }  
	
}
