package os.health.application;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import os.core.api.CoreOS;
import os.health.base.BaseCtrl;
import osgi.enroute.jsonrpc.api.JSONRPC;

/**
 * 假数据
 * @author admin
 */
@Component(name="os.mock",property=JSONRPC.ENDPOINT + "=mock")
@SuppressWarnings({"rawtypes","unchecked"})
public class MockCtrl extends BaseCtrl implements JSONRPC  {
	
	// 系统内核
	CoreOS coreos;
	@Reference
	void setCoreOS(CoreOS coreos){
		this.coreos=coreos;
	}
	@Override
	public CoreOS getCoreOS() {
		return this.coreos;
	}
	// 数据库访问类
	String DB_CLASS="os.moudel.db.api.DBase";
	
	
	// mock
	public Object mock(Map param){
		String table=param.get("table").toString();
		param=model(table,param);
		Object id=excute(table,param);
		// 创建管理关系
		if(table.startsWith("mn")&&!table.equals("mn_info")){
			Map kf=new HashMap();
			kf.put("userid",param.get("userid"));
			kf.put("fkid",id);
			kf.put("type",table);
			excute("fk_user_mn",kf);
		}
		return true;
	}
	public Map model(String table,Map param){
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");
		String time=format.format(new Date());
		if(table.startsWith("bld_")){
			Object user=this.getSession().getAttribute("user");
			String realname=null;
			if(user!=null){
				realname=((Map)user).get("realname").toString();
			}
			param.put("username",realname);
			param.put("zfhl",random(100)+"");
			param.put("bmi",random(100)+"");
			param.put("jcdx",random(100)+"");
			String tzpd=tzpd();
			String txpd=txpd();
			String alert="否";
			if(tzpd.equals("过高")||tzpd.equals("过低")){
				alert="是";
			}
			if(txpd.equals("肥胖")||txpd.equals("消瘦")){
				alert="是";
			}
			param.put("tzpd",tzpd);
			param.put("txpd",txpd);
			param.put("time",time);
			param.put("alert",alert);
			param.put("ysjy",ysjy());
			
			
		}
		
		if(table.startsWith("mn_")){
			
			param.put("name",name());
			param.put("sex",sex());
			param.put("age",random(100)+"");
			param.put("phone",phone());
			param.put("address",address());
			param.put("city",city());
			param.put("time",time);
			
			if(table.equals("mn_doctor")){
				param.put("hospital",hospital());
				param.put("ill",ill());
				param.put("years",random(30));
			}
			if(table.equals("mn_relatives")){
				param.put("profession",profession());
				param.put("relatives",relatives());
			}
			if(table.equals("mn_volunteer")){
				param.put("school",school());
				param.put("grade",grade());
			}
			if(table.equals("mn_info")){
				Object user=this.getSession().getAttribute("user");
				String id=((Map)user).get("id").toString();
				List args=new ArrayList();
				args.add(id);
				args.add("mn_info");
				List r=query("select * from fk_user_mn where userid=? and type=?",args);
				if(r!=null&&r.size()>0){
					id=((Map)r.get(0)).get("fkid")+"";
					param.put("id",id);
				}
				
				param.put("profession",profession());
				param.put("ill",ill());
			}
		}
		
		return param;
	}
	public Object excute(String table,Map param){
		return this.coreos.call(DB_CLASS,"excute",table,param);
	}
	public List query(String sql,List param){
		return this.coreos.call(DB_CLASS,"query",sql,param);
	}
	public int random(int range){
		return new Random().nextInt(range);
	}
	public String txpd(){
		String str="肥胖,微胖,正常,微瘦,消瘦";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String tzpd(){
		String str="过高,偏高,合格,偏低,过低";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String ysjy(){
		String str="多喝水,多吃药,动手术,打针,打点滴,多吃饭,多吃水果,多吃蔬菜,少吃肉";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String sex(){
		String str="男,女";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String relatives(){
		String str="爷爷,奶奶,姑姑,姑父,婶婶,叔叔,表兄弟,表兄妹,堂兄弟,堂姐妹,爸爸,妈妈,姥姥,姥爷,舅舅,舅妈,姨妈,姨夫,侄子,侄女,媳妇,儿子,女儿,外甥,外甥女,孙子,孙女,外孙,外孙女";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String grade(){
		String str="大一,大二,大三,大四";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String city(){
		String str="北京,杭州,天津,合肥,上海,福州,重庆,南昌,香港,济南,澳门,郑州,呼和浩特,武汉,乌鲁木齐,长沙,银川,广州,拉萨,海口,南宁,成都,石家庄,贵阳,太原,昆明,沈阳,西安,长春,兰州,哈尔滨,西宁,南京,台北";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String address(){
		String str="重庆大厦,黑龙江路,十梅庵街,遵义路,湘潭街,瑞金广场,仙山街,仙山东路,仙山西大厦,白沙河路,赵红广场,机场路,民航街,长城南路,流亭立交桥,虹桥广场,长城大厦,礼阳路,风岗街,中川路,白塔广场,兴阳路,文阳街,绣城路,河城大厦,锦城广场,崇阳街,华城路,康城街,正阳路,和阳广场,中城路,江城大厦,顺城路,安城街,山城广场,春城街,国城路,泰城街,德阳路,明阳大厦,春阳路,艳阳街,秋阳路,硕阳街,青威高速,瑞阳街,丰海路,双元大厦,惜福镇街道,夏庄街道,古庙工业园,中山街,太平路,广西街,潍县广场,博山大厦,湖南路,济宁街,芝罘路,易州广场,荷泽四路,荷泽二街,荷泽一路,荷泽三大厦,观海二广场,广西支街,观海一路,济宁支街,莒县路,平度广场,明水路,蒙阴大厦,青岛路,湖北街,江宁广场,郯城街,天津路,保定街,安徽路,河北大厦,黄岛路,北京街,莘县路,济南街,宁阳广场,日照街,德县路,新泰大厦,荷泽路,山西广场,沂水路,肥城街,兰山路,四方街,平原广场,泗水大厦,浙江路,曲阜街,寿康路,河南广场,泰安路,大沽街,红山峡支路,西陵峡一大厦,台西纬一广场,台西纬四街,台西纬二路,西陵峡二街,西陵峡三路,台西纬三广场,台西纬五路,明月峡大厦,青铜峡路,台西二街,观音峡广场,瞿塘峡街,团岛二路,团岛一街,台西三路,台西一大厦,郓城南路,团岛三街,刘家峡路,西藏二街,西藏一广场,台西四街,三门峡路,城武支大厦,红山峡路,郓城北广场,龙羊峡路,西陵峡街,台西五路,团岛四街,石村广场,巫峡大厦,四川路,寿张街,嘉祥路,南村广场,范县路,西康街,云南路,巨野大厦,西江广场,鱼台街,单县路,定陶街,滕县路,钜野广场,观城路,汶上大厦,朝城路,滋阳街,邹县广场,濮县街,磁山路,汶水街,西藏路,城武大厦,团岛路,南阳街,广州路,东平街,枣庄广场,贵州街,费县路,南海大厦,登州路,文登广场,信号山支路,延安一街,信号山路,兴安支街,福山支广场,红岛支大厦,莱芜二路,吴县一街,金口三路,金口一广场,伏龙山路,鱼山支街,观象二路,吴县二大厦,莱芜一广场,金口二街,海阳路,龙口街,恒山路,鱼山广场,掖县路,福山大厦,红岛路,常州街,大学广场,龙华街,齐河路,莱阳街,黄县路,张店大厦,祚山路,苏州街,华山路,伏龙街,江苏广场,龙江街,王村路,琴屿大厦,齐东路,京山广场,龙山路,牟平街,延安三路,延吉街,南京广场,东海东大厦,银川西路,海口街,山东路,绍兴广场,芝泉路,东海中街,宁夏路,香港西大厦,隆德广场,扬州街,郧阳路,太平角一街,宁国二支路,太平角二广场,天台东一路,太平角三大厦,漳州路一路,漳州街二街,宁国一支广场,太平角六街,太平角四路,天台东二街,太平角五路,宁国三大厦,澳门三路,江西支街,澳门二路,宁国四街,大尧一广场,咸阳支街,洪泽湖路,吴兴二大厦,澄海三路,天台一广场,新湛二路,三明北街,新湛支路,湛山五街,泰州三广场,湛山四大厦,闽江三路,澳门四街,南海支路,吴兴三广场,三明南路,湛山二街,二轻新村镇,江南大厦,吴兴一广场,珠海二街,嘉峪关路,高邮湖街,湛山三路,澳门六广场,泰州二路,东海一大厦,天台二路,微山湖街,洞庭湖广场,珠海支街,福州南路,澄海二街,泰州四路,香港中大厦,澳门五路,新湛三街,澳门一路,正阳关街,宁武关广场,闽江四街,新湛一路,宁国一大厦,王家麦岛,澳门七广场,泰州一路,泰州六街,大尧二路,青大一街,闽江二广场,闽江一大厦,屏东支路,湛山一街,东海西路,徐家麦岛函谷关广场,大尧三路,晓望支街,秀湛二路,逍遥三大厦,澳门九广场,泰州五街,澄海一路,澳门八街,福州北路,珠海一广场,宁国二路,临淮关大厦,燕儿岛路,紫荆关街,武胜关广场,逍遥一街,秀湛四路,居庸关街,山海关路,鄱阳湖大厦,新湛路,漳州街,仙游路,花莲街,乐清广场,巢湖街,台南路,吴兴大厦,新田路,福清广场,澄海路,莆田街,海游路,镇江街,石岛广场,宜兴大厦,三明路,仰口街,沛县路,漳浦广场,大麦岛,台湾街,天台路,金湖大厦,高雄广场,海江街,岳阳路,善化街,荣成路,澳门广场,武昌路,闽江大厦,台北路,龙岩街,咸阳广场,宁德街,龙泉路,丽水街,海川路,彰化大厦,金田路,泰州街,太湖路,江西街,泰兴广场,青大街,金门路,南通大厦,旌德路,汇泉广场,宁国路,泉州街,如东路,奉化街,鹊山广场,莲岛大厦,华严路,嘉义街,古田路,南平广场,秀湛路,长汀街,湛山路,徐州大厦,丰县广场,汕头街,新竹路,黄海街,安庆路,基隆广场,韶关路,云霄大厦,新安路,仙居街,屏东广场,晓望街,海门路,珠海街,上杭路,永嘉大厦,漳平路,盐城街,新浦路,新昌街,高田广场,市场三街,金乡东路,市场二大厦,上海支路,李村支广场,惠民南路,市场纬街,长安南路,陵县支街,冠县支广场,小港一大厦,市场一路,小港二街,清平路,广东广场,新疆路,博平街,港通路,小港沿,福建广场,高唐街,茌平路,港青街,高密路,阳谷广场,平阴路,夏津大厦,邱县路,渤海街,恩县广场,旅顺街,堂邑路,李村街,即墨路,港华大厦,港环路,馆陶街,普集路,朝阳街,甘肃广场,港夏街,港联路,陵县大厦,上海路,宝山广场,武定路,长清街,长安路,惠民街,武城广场,聊城大厦,海泊路,沧口街,宁波路,胶州广场,莱州路,招远街,冠县路,六码头,金乡广场,禹城街,临清路,东阿街,吴淞路,大港沿,辽宁路,棣纬二大厦,大港纬一路,贮水山支街,无棣纬一广场,大港纬三街,大港纬五路,大港纬四街,大港纬二路,无棣二大厦,吉林支路,大港四街,普集支路,无棣三街,黄台支广场,大港三街,无棣一路,贮水山大厦,泰山支路,大港一广场,无棣四路,大连支街,大港二路,锦州支街,德平广场,高苑大厦,长山路,乐陵街,临邑路,嫩江广场,合江路,大连街,博兴路,蒲台大厦,黄台广场,城阳街,临淄路,安邱街,临朐路,青城广场,商河路,热河大厦,济阳路,承德街,淄川广场,辽北街,阳信路,益都街,松江路,流亭大厦,吉林路,恒台街,包头路,无棣街,铁山广场,锦州街,桓台路,兴安大厦,邹平路,胶东广场,章丘路,丹东街,华阳路,青海街,泰山广场,周村大厦,四平路,台东西七街,台东东二路,台东东七广场,台东西二路,东五街,云门二路,芙蓉山村,延安二广场,云门一街,台东四路,台东一街,台东二路,杭州支广场,内蒙古路,台东七大厦,台东六路,广饶支街,台东八广场,台东三街,四平支路,郭口东街,青海支路,沈阳支大厦,菜市二路,菜市一街,北仲三路,瑞云街,滨县广场,庆祥街,万寿路,大成大厦,芙蓉路,历城广场,大名路,昌平街,平定路,长兴街,浦口广场,诸城大厦,和兴路,德盛街,宁海路,威海广场,东山路,清和街,姜沟路,雒口大厦,松山广场,长春街,昆明路,顺兴街,利津路,阳明广场,人和路,郭口大厦,营口路,昌邑街,孟庄广场,丰盛街,埕口路,丹阳街,汉口路,洮南大厦,桑梓路,沾化街,山口路,沈阳街,南口广场,振兴街,通化路,福寺大厦,峄县路,寿光广场,曹县路,昌乐街,道口路,南九水街,台湛广场,东光大厦,驼峰路,太平山,标山路,云溪广场,太清路";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String name(){
		String f1="赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜戚谢邹喻柏水窦章云苏潘葛奚范彭郎鲁韦昌马苗凤花方俞任袁柳酆鲍史唐费廉岑薛雷贺倪汤滕殷罗毕郝邬安常乐于时傅皮卞齐康伍余元卜顾孟平黄和穆萧尹姚邵湛汪祁毛禹狄米贝明臧计伏成戴谈宋茅庞熊纪舒屈项祝董梁杜阮蓝闵席季麻强贾路娄危江童颜郭梅盛林刁钟徐邱骆高夏蔡田樊胡凌霍虞万支柯咎管卢莫经房裘缪干解应宗宣丁贲邓郁单杭洪包诸左石崔吉钮龚程嵇邢滑裴陆荣翁荀羊於惠甄魏加封芮羿储靳汲邴糜松井段富巫乌焦巴弓牧隗山谷车侯宓蓬全郗班仰秋仲伊宫宁仇栾暴甘钭厉戎祖武符刘姜詹束龙叶幸司韶郜黎蓟薄印宿白怀蒲台从鄂索咸籍赖卓蔺屠蒙池乔阴郁胥能苍双闻莘党翟谭贡劳逄姬申扶堵冉宰郦雍却璩桑桂濮牛寿通边扈燕冀郏浦尚农温别庄晏柴瞿阎充慕连茹习宦艾鱼容向古易慎戈廖庚终暨居衡步都耿满弘匡国文寇广禄阙东殴殳沃利蔚越夔隆师巩厍聂晁勾敖融冷訾辛阚那简饶空曾毋沙乜养鞠须丰巢关蒯相查后江红游竺权逯盖益桓公万俟司马上官欧阳夏侯诸葛闻人东方赫连皇甫尉迟公羊澹台公冶宗政濮阳淳于仲孙太叔申屠公孙乐正轩辕令狐钟离闾丘长孙慕容鲜于宇文司徒司空亓官司寇仉督子车颛孙端木巫马公西漆雕乐正壤驷公良拓拔夹谷宰父谷粱晋楚阎法汝鄢涂钦段干百里东郭南门呼延归海羊舌微生岳帅缑亢况后有琴梁丘左丘东门西门商牟佘佴伯赏南宫墨哈谯笪年爱阳佟第五言福百家姓续";  
	    String f2="秀娟英华慧巧美娜静淑惠珠翠雅芝玉萍红娥玲芬芳燕彩春菊兰凤洁梅琳素云莲真环雪荣爱妹霞香月莺媛艳瑞凡佳嘉琼勤珍贞莉桂娣叶璧璐娅琦晶妍茜秋珊莎锦黛青倩婷姣婉娴瑾颖露瑶怡婵雁蓓纨仪荷丹蓉眉君琴蕊薇菁梦岚苑婕馨瑗琰韵融园艺咏卿聪澜纯毓悦昭冰爽琬茗羽希宁欣飘育滢馥筠柔竹霭凝晓欢霄枫芸菲寒伊亚宜可姬舒影荔枝思丽 "; 
	    String f3="伟刚勇毅俊峰强军平保东文辉力明永健世广志义兴良海山仁波宁贵福生龙元全国胜学祥才发武新利清飞彬富顺信子杰涛昌成康星光天达安岩中茂进林有坚和彪博诚先敬震振壮会思群豪心邦承乐绍功松善厚庆磊民友裕河哲江超浩亮政谦亨奇固之轮翰朗伯宏言若鸣朋斌梁栋维启克伦翔旭鹏泽晨辰士以建家致树炎德行时泰盛雄琛钧冠策腾楠榕风航弘";
	    int index1=random(f1.length());
	    int index2=random(f2.length());
	    int index3=random(f3.length());
		return ""+f1.charAt(index1)+f2.charAt(index2)+f3.charAt(index3);
	}
	public String phone(){
		String arry[]="134,135,136,137,138,139,150,151,152,157,158,159,130,131,132,155,156,133,153".split(",");  
		int index=random(arry.length);
        String first=arry[index]; 
        String second=String.valueOf(random(888)+10000).substring(1);  
        String thrid=String.valueOf(random(9100)+10000).substring(1);  
        return first+second+thrid;  
	}
	public String ill(){
		String str="糖尿病,营养缺乏病,痛风,骨质疏松,阻塞性肺气肿,哮喘 ,肺心病,呼吸衰竭,矽肺,肺纤维化,心力衰竭,冠心病,先天性心脏病,高血压,心脏瓣膜病,感染性心内膜炎,心肌疾病,心包炎,胃炎,消化性溃疡,肠结核,肠炎,腹泻,肝炎,肝硬化,胰腺炎,胆囊炎,肾炎,肾衰,泌尿系炎症,贫血,粒细胞白血病,淋巴细胞白血病,淋巴瘤,淋巴细胞性甲状腺炎,甲亢,甲减";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String hospital(){
		String str="北京大学第三医院,北京466医院,中国中医科学院西苑医院,北京肿瘤医院,解放军总医院第二附属医院,中国人民解放军空军总医院,海军总医院,北京世纪坛医院,北京大学口腔医院,北京大学第六医院,解放军总医院第一附属医院,北京老年医院,北京长青肛肠医院,北京市海淀区北方肿瘤医院,中国人民解放军总医院,北京解放军309医院,武警总医院";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String school(){
		String str="清华大学,北京大学,中国人民大学,北京邮电大学,北京航空航天大学,北京科技大学,北京化工大学,首都经贸大学,北京理工大学,北京交通大学,北京工业大学,北方工业大学,北京师范大学,首都师范大学,北京外国语大学,对外经贸大学,北京语言大学,中国农业大学,北京电影学院,中国石油大学(北京),北京大学医学部,中国协和医科大学,首都医科大学,北京中医药大学,中国地质大学,外交学院,中国青年政治学院,中央财经大学,中国传媒大学,中央音乐学院,北京体育大学,中国矿业大学,中央美术学院,中国人民公安大学,北京印刷学院,中国戏曲学院,北京林业大学,中央民族大学,中国政法大学,华北电力大学,北京第二外国语学院,北京信息科技大学,北京建筑工程学院,北京科技职业学院,中国音乐学院,中央广播电视大学,北京联合大学,北京石油化工学院,北京电子科技学院,北京教育学院,北京服装学院,中央戏剧学院,北京信息职业技术学院,首钢工学院,北京联合大学应用文理学院,北京舞蹈学院,北京农学院,北京农业职业学院,北京交通职业技术学院,中国防卫科技学院,北京物资学院,北京东方大学,北京城市学院,中国科学院研究生院,首都体育学院,中国劳动关系学院,北京北大方正软件技术学院,,北京财贸职业学院,北京电子科技职业学院,中华女子学院,北京吉利大学,北京工业职业技术学院,北京工商大学,北京信息职业技术学院,国际关系学院,北京电大,中国信息大学";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	public String profession(){
		String str="程序员,美工,运维,测试,销售,工人,学生,大学教授,厨师,理发师,服务员,快递员,送餐员,个体,公务员,总监,主管,教练,司机,助理";
		String[] arry=str.split(",");
		int index=random(arry.length);
		return arry[index];
	}
	
	
	@Override
	public Object getDescriptor() throws Exception {
		return "mock";
	}
	
}
