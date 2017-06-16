package os.debug.application;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
public class DebugApplication  {

	public static void main(String args[]) throws Exception{
		// E:\osgi5\coreos\os.debug.application
		String cur=System.getProperty("user.dir");
		
		Path target1=Paths.get(cur,"dist/folder/share");
		Path lib=Paths.get(cur,"dist/folder/core/lib");
		
		Path target2=Paths.get(cur,"dist/jar/master/share");
		Path target3=Paths.get(cur,"dist/jar/slave/share");
		
		System.out.println("clear share dir");
		Files.list(target1).forEach(file->{
			file.toFile().deleteOnExit();
		});
		Files.list(target2).forEach(file->{
			file.toFile().deleteOnExit();
		});
		Files.list(target3).forEach(file->{
			file.toFile().deleteOnExit();
		});
		
		System.out.println("clear lib dir");
		Files.list(lib).forEach(file->{
			file.toFile().deleteOnExit();
		});		
		
		
		// 获取工作空间路径
		Path workspace=Paths.get(cur).getParent();
		
		List<File> jarFiles=new ArrayList<>();
		try{
			// 递归搜索generated文件夹下的jar文件
	        Files.walkFileTree(workspace, new SimpleFileVisitor<Path>() {
	            @Override
	            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	            	String path=file.toString();
	            	if (path.contains("generated")&&path.endsWith(".jar")&&!path.contains(cur)) {  
	            		 jarFiles.add(file.toFile());  
	                }  
	                return FileVisitResult.CONTINUE;
	            }
	        });
		}catch(Exception e){}
	    for(File file:jarFiles){  
        	System.out.println("copy file:"+file.toString());
        	String name=file.getName();
        	
        	// copy to share
        	Files.copy(file.toPath(),Paths.get(target1.toString(),name));
        	Files.copy(file.toPath(),Paths.get(target2.toString(),name));
        	Files.copy(file.toPath(),Paths.get(target3.toString(),name));
        	
			if(name.startsWith("os.admin")||name.startsWith("os.core")||name.startsWith("os.network")||name.startsWith("os.route")){
				Files.copy(file.toPath(),Paths.get(lib.toString(),name)); 	
			}
	     }  
		System.out.println("finish");
		
	}
	
}
