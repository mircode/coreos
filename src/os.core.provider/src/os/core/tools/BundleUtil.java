package os.core.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import os.core.conf.Config;
import os.core.model.BundleInfo;

/**
 * 
 * 用于解析Bundle Jar文件
 * @author admin
 *
 */
public class BundleUtil {
	
	
	public static String name(String nameVersion){
		String name=nameVersion;
		if(name.indexOf(":")>-1){
			name=nameVersion.split(":")[0];
		}
		return name.replaceAll("(.application$|.provider$|.api$)","");
	}
	public static String version(String nameVersion){
		String version=nameVersion;
		if(nameVersion.indexOf(":")>-1){
			version=nameVersion.split(":")[1];
		}
		// 以数字开头
		if(version.matches("^\\d.*")){
			if(version.length()>5){
				return version.substring(0,5);
			}else{
				return version;
			}
		}
		return null;
	}
	public static String nameVersion(String str){
		String name=name(str);
		String version=version(str);
		if(version!=null){
			return name+":"+version;
		}else{
			return name;
		}
	}
	public static String nameVersion(BundleInfo bundle){
		return bundle.name+":"+bundle.version;
	}
	public static String bundlePath(String nameVersion){
		String name=nameVersion;
		if(nameVersion.indexOf(":")>-1){
			name=nameVersion.split(":")[0];
		}
		return fullName(name)+".jar";
	}
	// 返回组件全称
	public static String fullName(String simple){
		if(simple.indexOf(":")>-1){
			simple=simple.split(":")[0];
		}
		if(simple.matches("(.application$|.provider$|.api$)")){
			return simple;
		}else{
			if(simple.contains("os.moudel")||simple.matches("(os.core|os.network|os.route)")){
				return simple+".provider";
			}
			if(simple.contains("os.api")){
				return simple+".api";
			}
			return simple+".application";
		}
	}

	// 根据存储路径返回组件信息
	public static BundleInfo bundleInfo(String location){
		String path=getRepPath(location);
		try {
			return bundleInfo(new File(new URL(path).toURI()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	// 根据组件文件返回组件信息
	public static BundleInfo bundleInfo(File bundlejar){
		final StringBuilder name=new StringBuilder();
		final StringBuilder version=new StringBuilder();
		JarFile jar=null;
		try{
			jar=new JarFile(bundlejar); 
		    Manifest manifest = jar.getManifest();
		    manifest.getMainAttributes().forEach((key,value)->{
		    	if(key.toString().equals("Bundle-SymbolicName")){
		    		name.append(value.toString());
		    	}
		    	 if(key.toString().equals("Bundle-Version")){
		    		 version.append(value.toString());
		    	 }
		    });
		    if(jar!=null) jar.close();
		}catch(Exception e){
			if(jar!=null)
				try {
					jar.close();
				}catch(IOException e1){}
			return null;
		}
		
		BundleInfo bundle=new BundleInfo();
		bundle.name=name(name.toString());
		bundle.location=bundlejar.getName();
		bundle.version=version(version.toString());
		return bundle;
	}
	// 组件仓库列表
	public static List<BundleInfo> getRepList(){
		 List<BundleInfo> list=new ArrayList<>();
		 //从环境变量中读取REPERTOTY_PATH,组建仓库路径
		 //ConfigUtil.get中读取顺序，环境变量，启动参数，配置文件
		 Path path=Paths.get(Config.get(Config.REPERTORY_PATH));
		 try{
			 Files.list(path).filter(file->{
				 String name=file.getFileName().toString();
				 if(name.endsWith(".jar")){
					 return true;
				 }else{
					 return false;
				 }
			 }).forEach(file->{
				 list.add(bundleInfo(file.toFile()));
			 });
		 }catch(Exception e){
			 e.printStackTrace();
		 }
		 return list;
	}
	// 获取组件对应的仓库地址
	public static String getRepPath(String location){
		
		// 检测location对应的路径是否存在组件
		boolean local=true;
		try{
			//检测location是否是一个网络中的地址，网络不存在，抛出异常
			new URL(location).openStream();
			local=false;
			return location;
		}catch(Exception e){
			local=true;
		}
		
		// 检测仓库下是否存在相应组件
		String bath=Config.get(Config.REPERTORY_PATH);
		if(local){
			File file=Paths.get(bath,location).toFile();
			if(file.exists()){
				try{
					location=file.toURI().toURL().toString();
					return location;
				}catch(Exception e){
					return bath+"/"+location;
				}
			}
		}
		return bath+"/"+location;
	}
	
	public static void main(String args[]) throws Exception{
		 List<BundleInfo> list=BundleUtil.getRepList();
		 list.forEach(row->{
			 System.out.println(row);
		 });
	}
}
