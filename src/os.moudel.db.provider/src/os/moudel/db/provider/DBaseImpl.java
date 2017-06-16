package os.moudel.db.provider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import os.moudel.db.api.DBase;

/**
 * 数据库连接
 */
@Component(name = "os.moudel.db")
@SuppressWarnings({"unchecked","rawtypes"})
public class DBaseImpl implements DBase {

	// 数据源
	DataSource db=null;
	
	// 数据连接对象
	public Connection getConnect(){
		Connection conn=null;
		try{
			conn=db.getConnection();
		}catch(SQLException e1){
			e1.printStackTrace();
		}
		return conn;
	}
	@Activate
	public void start(){
		this.init(false);
		if(db==null){
			db=new DBPool();
		}
		DateFormat format=new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
		String time=format.format(new Date());
		System.out.println("start:"+time);
	}
	
	public List<Map<String,Object>> query(String sql) {
		return this.query(sql, new ArrayList());
	}
	public <T> T excute(String sql){
		List param=new ArrayList();
		return this.excute(sql, param);
	}
	public <T> T excute(String sql, List param) {
		Connection conn=this.getConnect();
		if(conn==null) return null;
		return this.excute(conn, sql, param);
	}
	public List<Map<String,Object>> query(String sql,List param) {
		// 获取数据库连接对象
		Connection conn=this.getConnect();
		if(conn==null) return null;
		return this.query(conn,sql, param);
	}
	@Override
	public <T> T excute(String table,Map param) {
		
		Object id=param.get("id");
		
		String sql=null;
		List args=new ArrayList();

		// 查询表中含有字段
		Connection conn=this.getConnect();
		List<String> colums=colums(conn,table);
				
		// 更新
		if(id!=null&&!id.toString().isEmpty()){
			String updates="";
			for(Object key : param.keySet()){ 
				if(!key.toString().equals("id")&&colums.contains(key.toString())){
				    Object value=param.get(key);
				    updates+=" "+key+"=?,";
					args.add(value);
				}
			}
			args.add(id);
			updates=updates.replaceAll(",$","");
			sql=String.format("update %s set %s where id=?",table,updates);
			
			return (T)this.excute(conn,sql.toString(),args);
			
		// 插入操作
		}else{
			String fields="";
			String values="";
			for(Object key : param.keySet()){  
				if(!key.toString().equals("id")&&colums.contains(key.toString())){
				    Object value=param.get(key);
				    fields+=key+",";
					values+="?,";
					args.add(value);
				}
			}
			fields=fields.replaceAll(",$","");
			values=values.replaceAll(",$","");
			
			sql=String.format("insert into %s (%s) values(%s)",table,fields,values);
			return (T)this.excute(conn,sql,args);
		}
	
	}
	@Override
	public List<Map<String,Object>> query(String table,Map param){
		return this.query(table,param, "time desc","100");
	}
	@Override
	public List<Map<String,Object>> query(String table,Map param,String order,String limit){

		// 查询表中含有字段
		Connection conn=this.getConnect();
		List<String> colums=colums(conn,table);
		
		String where="";
		List args=new ArrayList();
		for(Object key : param.keySet()){  
			if(colums.contains(key.toString())){
			    Object value=param.get(key);
			    if(value!=null){
			    	if(value instanceof String){
			    		if(value.toString().equals("")){
			    			continue;
			    		}
			    	}
				    where+=" and "+key+"=?";
				    args.add(value);
			    }
			}
		}
		// 时间范围
		Object start=param.get("start");
		Object end=param.get("end");
		if(start!=null&&!start.toString().equals("")){
			where+=" and time>=?";
			args.add(start);
		}
		if(end!=null&&!end.toString().equals("")){
			where+=" and time<=?";
			args.add(end);
		}
		String sql=String.format("select * from %s where 1=1 %s order by %s limit 0,%s", table,where,order,limit);
		return this.query(conn,sql, args);
	}
	@Override
	public void deleteByIds(String table, String ids) {
		String sql=String.format("delete from %s where id in(%s)",table,ids);
		this.excute(sql, new ArrayList());
	}
	@Override
	public List<Map<String,Object>> queryByIds(String table, String ids) {
		String sql=String.format("select * from %s where id in(%s)",table,ids);
		return this.query(sql,new ArrayList());
	}
	@Override
	public <T> T queryById(String table, Object id) {
		String sql=String.format("select * from %s where id=?",table);
		List param=new ArrayList();
		param.add(id);
		List list=this.query(sql,param);
		if(list.size()>0){
			return (T) list.get(0);
		}else{
			return null;
		}
	}
	// 增删改
	public <T> T excute(Connection conn,String sql, List param) {
		
		// 打印
		String info=sql.replace("?","%s");
		System.out.println("[excute]: "+String.format(info,param==null?new Object[]{}:param.toArray()));
			
		Object res=false;
		PreparedStatement state=null;
			
		try{
			// 准备
			state=conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			// 参数
			setParam(state,param);
			// 查询
			int row=state.executeUpdate();
			
			// 处理返回结果
			if(sql.contains("insert")){
				try{
					ResultSet rs=state.getGeneratedKeys();  
					if(rs.next()){
						res=rs.getObject(1);
					}
					return (T)res;
				}catch(Exception e){}
			}
			
			if(row>0){
				res=true;
			}else{
				res=false;
			}
			return (T)res;
           
		}catch(SQLException e){
			e.printStackTrace();
		}finally{
			try{
				if(state!= null){
					state.close();
				}
				if(conn!=null){
					conn.close();
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
		}
		return (T)res;
	}
	// 查询
	private List<Map<String,Object>> query(Connection conn,String sql,List param) {
		
		// 打印
		String info=sql.replace("?","%s");
		System.out.println("[ query]: "+String.format(info,param==null?new Object[]{}:param.toArray()));
		
		
		ResultSet set=null;
		PreparedStatement state=null;
		List<Map<String,Object>> res=null;
		
		ResultSetMetaData meta=null;
		
		try{
			// 准备
			state=conn.prepareStatement(sql);
			// 参数
			setParam(state,param);
			// 查询
			set=state.executeQuery();
			
			// 处理返回结果
			meta=set.getMetaData();
			res=new ArrayList<Map<String,Object>>();
			while(set.next()){
				Map<String,Object> map=new HashMap<>();
				for(int i=0;i<meta.getColumnCount();i++) {
					map.put(meta.getColumnName(i+1),set.getObject(i+1));
				}
				res.add(map);
			}
		}catch(SQLException e){
			e.printStackTrace();
		}finally{
			try{
				if(set!=null){
					set.close();
				}
				if(state!= null){
					state.close();
				}
				if(conn!=null){
					conn.close();
				}
			}catch(SQLException e){
				e.printStackTrace();
			}
		}
		return res;
	}
	private List<String> colums(Connection conn,String table){
		List<String> list=new ArrayList<>();
		try{
			DatabaseMetaData dbmd=conn.getMetaData();
	        ResultSet set=dbmd.getTables(null,"%",table,new String[]{"TABLE"});
	        while(set.next()){
	            ResultSet rs=dbmd.getColumns(null,"%",table,"%");
	            while(rs.next()){
	            	list.add(rs.getString("COLUMN_NAME"));
	            }
	        }
		}catch(Exception e){}
		return list;
	}
	private void setParam(PreparedStatement state,List params){
		if(params!=null){
			for(int i=0;i<params.size();i++){
				try{
				state.setObject(i+1,params.get(i));
				}catch(Exception e){}
			}
		}
	}
	
	// 初始化数据库
	public Object init(boolean reset){
		
		// 数据库连接
		Connection conn=getMgrConn();
		
		if(reset==false){
			try{
				conn.prepareStatement("use coreos").executeUpdate();
			}catch(Exception e){
				reset=true;
			}
		}
		// 读取SQL文件
		List<String> lines=new ArrayList<>();
		try{
			InputStream input=DBaseImpl.class.getResourceAsStream("schema.sql");
	        BufferedReader read= new BufferedReader(new InputStreamReader(input));
	        String line;
	         while((line=read.readLine())!=null){
	        	 lines.add(line);
	         } 
	         read.close();
	         input.close();
		}catch(Exception e){
			return false;
		}
		
		// 初始化数据库
		StringBuilder sql=new StringBuilder();
		for(String line:lines){
			if(!line.equals("")&&!line.startsWith("/*")){
				sql.append(line);
				if(line.endsWith(";")){
					try{
						if((!line.startsWith("DROP TABLE")&&!line.startsWith("INSERT INTO"))||reset){
							System.out.println("[excute]: "+sql);
							conn.prepareStatement(sql.toString()).executeUpdate();
						}
					}catch(Exception e){
						e.printStackTrace();
						System.out.println("执行失败:"+line);
						return false;
					}
					sql.setLength(0);
				}
			}
		}
		return true;
	}
	// 数据库管理
	private Connection mrgConn=null;
	private Connection getMgrConn(){
		if(mrgConn==null){
			try {
				String url=DBConfig.rooturl();
				String driver=DBConfig.driver();
				String username=DBConfig.username();
				String password=DBConfig.password();
				
				Class.forName(driver);
				this.mrgConn =  DriverManager.getConnection(url,username,password);
		    } catch (Exception e) {
		    	e.printStackTrace();
		        throw new ExceptionInInitializerError();
		    }
		}
		return mrgConn;
	}
}
