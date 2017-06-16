package os.moudel.db.api;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
@SuppressWarnings({"rawtypes"})
public interface  DBase {
	
	// 返回数据库连接对象
	public Connection getConnect();

	// 插入或更新
	public <T> T excute(String sql);
	public <T> T excute(String sql,List param);
	public <T> T excute(String table,Map param);
	
	// 查询多条
	public List<Map<String,Object>> query(String sql);
	public List<Map<String,Object>> query(String sql,List param);
	public List<Map<String,Object>> query(String table,Map param);
	public List<Map<String,Object>> query(String table,Map param,String order,String limit);
	
	// 根据ID查询
	public <T> T queryById(String table,Object id);
	// 查询多个对象
	public List<Map<String,Object>> queryByIds(String table,String ids);
	
	// 根据ID列表删除
	public void deleteByIds(String table,String ids);
	
	// 重新初始数据库
	public Object init(boolean reset);
}
