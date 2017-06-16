package os.moudel.db.provider;

import java.io.IOException;
import java.util.Properties;

import os.core.conf.Config;
/**
 * 数据库配置类
 * @author admin
 */
public class DBConfig {
	private static Properties config = new Properties();
	static{
		try {
			config.load(DBConfig.class.getResourceAsStream("db.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// 用户名
	public static String username(){
		String username=Config.get(Config.DB_USERNAME);
		if(username==null){
			username=config.getProperty("jdbc.username");
		}
		return username;
	}
	// 密码
	public static String password(){
		String password=Config.get(Config.DB_PASSWORD);
		if(password==null){
			password=config.getProperty("jdbc.password");
		}
		return password;
	}
	// 驱动包
	public static String driver(){
		String driver=config.getProperty("jdbc.driver");
		return driver;
	}
	// 连接串
	public static String url(){
		String url=Config.get(Config.DB_URL);
		if(url!=null){
			url="jdbc:mysql://"+url+"/";
		}
		if(url==null){
			url=config.getProperty("jdbc.url");
		}
		String db=Config.get(Config.DB_DATABASE);
		if(db==null){
			db=config.getProperty("jdbc.db");
		}
		// 数据编码
		String code=config.getProperty("jdbc.code");
		return url+db+code;
	}
	// 最大连接串
	public static String maxconnects(){
		return config.getProperty("jdbc.maxconnects");
	}
	// root权限连接串
	public static String rooturl(){
		String url=Config.get(Config.DB_URL);
		if(url!=null){
			url="jdbc:mysql://"+url+"/";
		}
		if(url==null){
			url=config.getProperty("jdbc.url");
		}
		// 数据编码
		String code=config.getProperty("jdbc.code");
		return url+code;
	}
}
