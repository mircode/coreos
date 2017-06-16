package os.moudel.db.provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import os.moudel.db.api.DBase;
import junit.framework.TestCase;

@SuppressWarnings({"unchecked","rawtypes"})
public class DBaseTest extends TestCase {
	
	public static void test1(){
		String table="bld_fat";
		// 添加
		DBase base=new DBaseImpl();
		
		Map param=new HashMap();
		param.put("userid", 1);
		param.put("username", "wgx");
		param.put("zfhl", "12.4");
		param.put("bmi", "18.3");
		param.put("jcdx", "1803");
		param.put("tzpd", "偏低");
		param.put("txpd", "消瘦");
		param.put("AAC", "消瘦");
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");
		String time=format.format(new Date());
		param.put("time",time);
		param.put("alert","否");
		param.put("ysjy","无");
		Long id=base.excute(table, param);
		
		// 查询
		Map res=base.queryById(table, id);
		System.out.println(res);
		
		// 修改
		param.put("id",id);
		param.put("username","wgx2");
		boolean b=base.excute(table, param);
		System.out.println(b?"更新成功":"更新失败");
		
		// 查询
		res=base.queryById(table, id);
		System.out.println(res);
		
		// 查询
		Map where=new HashMap();
		where.put("username","wgx");
		where.put("zfhc","12.4");
		List list=base.query(table,where);
		System.out.println(list);
				
		// 删除
		base.deleteByIds(table, id+"");
	}
	public static void test2(){
		// 添加
		DBase base=new DBaseImpl();
		base.init(false);
	}
	public static void main(String args[]){
		test2();
	}

	
}
