package os.core.api;

import java.io.InputStream;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import os.core.model.BundleInfo;
import os.core.model.ConfigInfo;
import os.core.model.ServiceInfo;

/**
 * 内核接口
 */
public interface CoreOS {
	
	// 管理接口
	/**
	 * 安装组件
	 * @param location 组件路径
	 * @return         组件实例
	 * @throws BundleException
	 */
	public Bundle install(String location) throws BundleException;
	/**
	 * 卸载组件
	 * @param  id or name:version
	 * @return 组件实例
	 * @throws BundleException
	 */
	public Bundle uninstall(String nameVersion) throws BundleException;
	/**
	 * 启动组件
	 * @param  id or name:version
	 * @return 组件实例
	 * @throws BundleException
	 */
	public Bundle start(String nameVersion) throws BundleException;
	/**
	 * 停止组件
	 * @param  id or name:version
	 * @return 组件实例
	 * @throws BundleException
	 */
	public Bundle stop(String nameVersion) throws BundleException;
	/**
	 * 更新组件
	 * @param  id or name:version
	 * @return 组件实例
	 * @throws BundleException
	 */
	public Bundle update(String nameVersion) throws BundleException;
	
	// 查询接口
	/**
	 * 组件列表
	 * @return
	 */
	public List<BundleInfo> getBundles();
	/**
	 * 仓库组件列表
	 * @return
	 */
	public List<BundleInfo> getRepertories();
	/**
	 * 服务列表
	 * @return
	 */
	public List<ServiceInfo> getServices();
	
	// 组件调用接口
	/**
	 * 组件调用
	 * @param namespace 类路径
	 * @param method    方法名
	 * @param args      参数
	 * @return          返回值
	 */
	public <T> T call(String namespace, String method, Object... args);
	
	// 其他接口
	/**
	 * 获取当前上下文环境
	 * @return
	 */
	public BundleContext getContext();
	/**
	 * 获取系统配置接口
	 * @param key
	 * @param def
	 * @return
	 */
	public String setConf(String key,String val);
	public String getConf(String key,String def);
	public List<ConfigInfo> listConf();
	
	/**
	 * 获取某个服务
	 * @return
	 */
	public Object getService(String namespace);
	
	/**
	 * 刷新组件
	 * eg:
	 * refresh
	 * refresh name:verison name:verison
	 * @return
	 */
	public List<Bundle> refresh(String... args);
	/**
	 * 解析组件依赖
	 * eg:
	 * resolve
	 * resolve name:verison name:verison
	 */
	public List<Bundle> resolve(String... args);
	/**
	 * 获取和设置系统启动级别
	 * eg:
	 * startLevel
	 * startLevel 0
	 */
	public String startLevel(String... args);
	/** 
	 * 获取和设置组件的运行级别
	 * eg:
	 * bundleLevel id
 	 * bundleLevel name:version
	 * bundleLevel name:version 2
	 * bundleLevel -i  2
	 */
	public String bundleLevel(String... args);
	/**
	 * 安装组件
	 * @param location 组件位置
	 * @param input    输入流
	 * @return         组件实例
	 * @throws BundleException
	 */
	public Bundle install(String location, InputStream input)throws BundleException;
}
