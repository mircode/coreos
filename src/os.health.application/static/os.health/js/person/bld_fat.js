 //@ sourceURL=bld_fat.js
+function(baseurl){
	
	// 查询表
	var action="bld_fat";
	// 全部查询
	var allFlag=true;
	
	// 初始化
	+function init(){
		search();
		var types=[
			{id:'',text:'全部'},
			{id:'过高',text:'过高'},
			{id:'偏高',text:'偏高'},
			{id:'合格',text:'合格'},
			{id:'偏低',text:'偏低'},
			{id:'过低',text:'过低'}
		];
		$("input[name='tzpd']").select2({data: types});
	}();
	// 绑定事件
	$('#searchQuery').on('click',function(){
		allFlag=false;
		search();
	});
	$('#searchAll').on('click',function(){
		allFlag=true;
		search();
	});
	
	function getParam(){
		// 参数要保持顺序
		var param={};
		
		// 查询必要信息
		param.table=action;
		if(allFlag==true){
			return param;
		}
		//  合并查询表单参数		
		var form=$('#queryForm').form();
		param=$.extend(param,form)
		return param;
	}
	function search(){
		
		//param
		var param=getParam();
		
		// query
		$.JsonRPC('person/query',param).done(function(result){
			
			if(!result) {alert('查询列表失败');return;}
			result.forEach(function(ele){
						ele=html(ele);
					});
			var data=result;
			
			// 表头映射
			var header=[  
						  {title:'id',data:'id'},
				          {title:'用户名',data:'username'},
				          {title:'脂肪含量',data:'zfhl'},
				          {title:'BMI',data:'bmi'},
				          {title:'基础代谢',data:'jcdx'},
				          {title:'体质判断',data:'tzpd'},
				          {title:'体型判定',data:'txpd'},
				          {title:'测量时间',data:'time'},
				          {title:'报警',data:'alert'},
				          {title:'医生建议',data:'ysjy'}
				        ];
				         
			$('#table_view').table(data,header);
			
		});
	}
	function html(item){
		item=alert_html(item);
		item=tzpd_html(item);
		item=name_html(item);
		return item;
	}
	function name_html(item){
		item.username='<a href="#">'+item.username+'</a>';
		return item;
	}
	function alert_html(item){
		var level='bg-green';
		if(item.alert=='否'){
			level='bg-green';
		}
		if(item.alert==='是'){
			level='bg-red';
		}
		item.alert='<span class="label '+level+'">'+item.alert+'</span>';
		return item;
	}
	function tzpd_html(item){
		var level='bg-green';
				
		if(item.tzpd=='过高'){
			level='bg-red';
		}
		if(item.tzpd=='偏高'){
			level='bg-yellow';
		}
		if(item.tzpd=='正常'){
			level='bg-green';
		}
		if(item.tzpd=='偏低'){
			level='bg-yellow';
		}
		if(item.tzpd=='过低'){
			level='bg-red';
		}
		item.tzpd='<span class="label '+level+'">'+item.tzpd+'</span>';
		return item;
	}
	
	// mock 回调
	window.mock.callback=search;
}(baseurl);
