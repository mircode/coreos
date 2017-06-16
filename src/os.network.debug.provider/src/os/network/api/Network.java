package os.network.api;

import java.util.List;

import os.core.model.BundleInfo;
import os.core.model.HostInfo;
import os.core.model.ServiceInfo;

/**
 * 调度组件接口
 * @author 尹行欣
 *
 */
public interface Network {
	/**
	 * 执行远程命令
	 * @param namespace
	 * @param method
	 * @param args
	 * @return
	 */
	public <T> T call(String namespace,String method,Object... args);
	/**
	 * 获取主机所能提供的功能
	 * @param service
	 * @return
	 */
	public List<ServiceInfo> getServices();
	/**
	 * 获取主机安装的组件
	 * @param service
	 * @return
	 */
	public List<BundleInfo> getBundles();
	/**
	 * 获取主机的IP和通讯端口等信息
	 * @param service
	 * @return
	 */
	public HostInfo getHostInfo();
	
	/**
	 * 获取网络中所有主机引用
	 * @return
	 */
	public List<Network> getRoutes();
}
