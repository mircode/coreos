package os.core.conf;


import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import os.core.tools.StringUtil;

/**
 * 系统配置类
 * @author admin
 *
 */
public class Config {
	
	// 主机配置信息
	public static String HOST_IP="os.host.ip";
	public static String HOST_PORT="os.host.port";
	public static String HOST_NAME="os.host.name";
	
	// 路由组件信息
	public static String ROUTE_URL="os.route.url";
	
	// 数据库连接信息
	public static String DB_URL="os.db.url";
	public static String DB_DATABASE="os.db.database";
	public static String DB_USERNAME="os.db.username";
	public static String DB_PASSWORD="os.db.password";
	
	// 组件仓库地址
	public static String REPERTORY_PATH="os.repertory.path";
	
	// 系统临时目录
	public static String COREOS_TMP="os.coreos.tmp";
	
	public static Properties config = new Properties();
	// 配置文件所在路径
	private static String defualt="config.properties";
	static{
		try{
			
			// 从启动参数中读取项目家目录
			String home=System.getProperty("os.home");
			
			// 从环境变量中读取项目家目录
			if(StringUtil.isEmpty(home)){
				home=System.getenv().get("OS_HOME");
			}
			
			// 启动参数中读取配置文件
			String conf=System.getProperty("os.conf");
			if(conf!=null){
				config.load(new FileInputStream(conf));
			
			// 尝试从家目录下读取配置文件
			}else{
				// 读取配置文件
				if(!StringUtil.isEmpty(home)){
					config.load(new FileInputStream(home+"/conf/config.properties"));
				// 从当前类路径下读取
				}else{
					config.load(Config.class.getResourceAsStream(defualt));
				}
			}
			
			
			config.putAll(System.getProperties());
			
			String port=get("org.osgi.service.http.port");
			if(port!=null){
				config.put(Config.HOST_PORT,port);
			}
			String path=config.getProperty(Config.REPERTORY_PATH);
			if(!Paths.get(path).isAbsolute()){
				config.setProperty(Config.REPERTORY_PATH, Paths.get(home,path).toString());
			}
			
		}catch(Exception e){}
	}
	
	public static String get(String key){
		return config.getProperty(key);
	}
	public static String get(String key,String def){
		return config.getProperty(key, def);
	}
	public static void set(String key,String val){
		config.setProperty(key, val);
	}
	
	public static void update(ConfigurationAdmin cm) {
		// 重置Jetty服务器通讯端口
		try{
			Configuration conf=cm.getConfiguration("org.apache.felix.http",null);
			if(conf!=null){
				Dictionary<String,Object> param=conf.getProperties();
				if(param==null){
					param=new Hashtable<String,Object>();
				}
				param.put("org.osgi.service.http.port",get(Config.HOST_PORT,"8080"));
				conf.update(param);
			}
		}catch(Exception e){}
	}
	
}
