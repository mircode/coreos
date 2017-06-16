package os.core.tools;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes","unchecked"})
public class ReflectUtil {

	public static <T> T invoke(Object target,String m,Map<String,Object> params) throws Exception{
		Method method=search(target.getClass(),m,params);
		if(method==null) return null;
		return (T)invoke(target,method,params);
	}
	public static <T> T invoke(Object target,String m,List<Object> params) throws Exception{
		Method method=search(target.getClass(),m,params);
		if(method==null) return null;
		return (T)invoke(target,method,params);
	}
	public static <T> T invoke(Object target,String m,Object... params) throws Exception{
		List args=new ArrayList();
		for(Object obj:params){
			args.add(obj);
		}
		Method method=search(target.getClass(),m,args);
		if(method==null) return null;
		return (T)invoke(target,method,params);
	}
	public static <T> T invoke(Object target,Method m,Map<String,Object> params) throws Exception{
		List args=new ArrayList();
	    for(Parameter p : m.getParameters()){
			 // 目标参数类型
			 Class<?> pType=p.getType();
			 
			 Object obj=null;
			 // 目标参数名称
			 String pName=p.isNamePresent()?p.getName():null;
			 if(pName!=null){
				 // 名称匹配
				 obj=params.get(pName);
			 }
			 // 参数类型为Map类型
			 if(obj==null){
				 try{
					 if(pType.getName().equals(Map.class.getName())){
						 obj=params;
					 }
				 }catch(Exception e){};
			 }
			 // 参数类型为Model类型
			 if(obj==null){
				 try{
					obj=pType.newInstance();
		            Field[] fields=pType.getDeclaredFields();
		            for(Field field:fields){
		            	try{
			            	field.setAccessible(true);
			            	Object val=params.get(field.getName());
			            	val=translate(val,field.getType());
			            	field.set(obj,val);
		            	}catch(Exception e){};
		            }
				 }catch(Exception e){};
				 
			 }
			 // 参数类型为复杂类型
			 if(obj==null){
				 for(Object o:params.values()){
					 if(o.getClass().equals(pName)){
						 obj=o;
						 break;
					 }
				 }
			 }
			 // 调用setModel方法
			 if(obj==null){
				 try{
					 obj=pType.newInstance();
					 Method setmodel=pType.getMethod("setModel",Map.class);
					 if(setmodel!=null){
						 setmodel.invoke(obj, params);
					 }
				 }catch(Exception e){}
			 }
			 args.add(obj);
        }
		return (T)invoke(target,m,args);
	}
	public static <T> T invoke(Object target,Method m,List<Object> params) throws Exception{
		List args=new ArrayList();
		Class<?>[] types=m.getParameterTypes();
		for(int i=0;i<types.length;i++){
			args.add(translate(params.get(i),types[i]));
		}
		if(args.size()==1){
			return (T)m.invoke(target,args.get(0));
		}else{
			return (T)m.invoke(target,args.toArray());
		}
	}
	public static <T> T invoke(Object target,Method m,Object... params) throws Exception{
		List args=new ArrayList();
		for(int i=0;i<params.length;i++){
			args.add(params[i]);
		}
		if(args.size()==1){
			return (T)m.invoke(target,args.get(0));
		}else{
			return (T)m.invoke(target,args.toArray());
		}
	}
	private static Object translate(Object obj, Class<?> clz) {
		String name=clz.getName();
		try{
			// 目标类型为数字
			if(name.equals("int")||name.equals(Integer.class.getName())){
				return Integer.parseInt(obj.toString());
			}
			if(name.equals("long")||name.equals(Long.class.getName())){
				return Long.parseLong(obj.toString());
			}
			if(name.equals("float")||name.equals(Float.class.getName())){
				return Float.parseFloat(obj.toString());
			}
			if(name.equals("double")||name.equals(Double.class.getName())){
				return Double.parseDouble(obj.toString());
			}
			if(name.equals("boolean")||name.equals(Boolean.class.getName())){
				return Boolean.parseBoolean(obj.toString());
			}
			// 目标类型为字符串
			if(name.equals(String.class.getName())){
				return obj.toString();
			}
		}catch(Exception e){ return null;}
		return obj;
	}
	private static boolean compare(Class clz1,Object obj) { 
		Class clz2=obj.getClass();
		try{
			 if(clz1.getField("TYPE")!=null){
				 clz1=(Class)clz1.getField("TYPE").get(null);
			 }
		}catch(Exception e){}
		try{
			 if(clz2.getField("TYPE")!=null){
				 clz2=(Class)clz2.getField("TYPE").get(null);
			 }
		}catch(Exception e){}
		
		return clz1.getName().equals(clz2.getName())||clz1.isInstance(obj);
	} 
	public static Method search(Class clazz,String m,Map<String,Object> params){
		// 目标方法
		Method res=null;
		// 完全比配
		List<Method> targets=new ArrayList();
		Method[] methods=clazz.getMethods();
		for(int i=0;i<methods.length;i++){
			Method mt=methods[i];
			if(mt.getName().equals(m)){
				targets.add(mt);
			}
		}
		if(targets.size()<=0){
			return null;
		}else if(targets.size()==1){
			res=(Method)targets.get(0);
		}else{
			// 完全比配
			int match=0;
			for(Method mt:targets){
				boolean flag=true;
				int count=0;
				for(Parameter p:mt.getParameters()){
					Class<?> clz=p.getType();
					String name=p.isNamePresent()?p.getName():null;
					Object t=params.get(name);
					if(t==null||!compare(clz,t)){
						flag=false;
						break;
					}else{
						count++;
					}
				}
				if(flag&&count>match){
					match=count;
					res=mt;
				}
			}
			// 转换匹配
			if(res==null){
				match=0;
				for(Method mt:targets){
					boolean flag=true;
					int count=0;
					for(Parameter p:mt.getParameters()){
						Class<?> clz=p.getType();
						String name=p.isNamePresent()?p.getName():null;
						Object t=params.get(name);
						t=translate(t,clz);
						if(t==null||!compare(clz,t)){
							flag=false;
							break;
						}else{
							count++;
						}
					}
					if(flag&&count>match){
						match=count;
						res=mt;
					}
				}
			}
		}
		if(res==null){
			return null;
		}
		return res;
	}
	public static Method search(Class clazz,String m,List<Object> params){
		// 目标方法
		Method res=null;
		
		// 完全比配
		List<Method> targets=new ArrayList();
		Method[] methods=clazz.getMethods();
		for(int i=0;i<methods.length;i++){
			Method mt=methods[i];
			if(mt.getName().equals(m)&&mt.getParameterCount()==params.size()){
				targets.add(mt);
			}
		}
		if(targets.size()<=0){
			return null;
		}else if(targets.size()==1){
			res=(Method)targets.get(0);
		}else{
			// 完全比配
			for(Method mt:targets){
				Class<?> clz[]=mt.getParameterTypes();
				Object clls[]=params.toArray();
				boolean flag=true;
				for(int j=0;j<clz.length;j++){
					if(!compare(clz[j],clls[j])){
						flag=false;
						break;
					}
				}
				if(flag){
					res=mt;
					break;
				}
			}
			// 转换匹配
			if(res==null){
				for(Method mt:targets){
					Class<?> clz[]=mt.getParameterTypes();
					Object clls[]=params.toArray();
					boolean flag=true;
					for(int j=0;j<clz.length;j++){
						Object t=translate(clls[j],clz[j]);
						if(!compare(clz[j],t)){
							flag=false;
							break;
						}
					}
					if(flag){
						res=mt;
						break;
					}
				}
			}
		}
		if(res==null){
			return null;
		}
		return res;
	}
	
}
