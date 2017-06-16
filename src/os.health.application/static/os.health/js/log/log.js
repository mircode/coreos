 //@ sourceURL=log.js
+function(baseurl){
	// 全部查询
	var allFlag=true;
	
	// 初始化
	+function init(){
		search();
		var types=[
			{id:'',text:'全部'},
			{id:'info',text:'正常'},
			{id:'warn',text:'警告'},
			{id:'error',text:'错误'},
			{id:'debug',text:'调试'}
		];
		$("#type").select2({data: types});
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
		var param={};
		// 查询全部
		if(allFlag==true){
			return param;
		}
		//  合并查询表单参数		
		var form=$('#queryForm').form();
		param=$.extend(param,form)
		
		return param;
	}
	function search(){
		
		// 查询参数
		var param=getParam();
		
		// 查询
		$.JsonRPC('log/query',param).done(function(result){
			if(!result) {alert('查询列表失败');return;}
			
			result.forEach(function(ele){
						ele=html(ele);
					});
			
			var data=result;
			
			var header=[
						  {title:'id',data:'id'},
				          {title:'级别',data:'level'},
				          {title:'用户',data:'username'},
				          {title:'信息',data:'msg'},
				          {title:'记录时间',data:'time'},
				          {title:'IP',data:'ip'}
				          ];
			$('#table_view').table(data,header);
		});
	}
	function html(item){
		item=level_html(item);
		item=name_html(item);
		return item;
	}
	function name_html(item){
		item.username='<a href="#">'+item.username+'</a>';
		return item;
	}
	function level_html(item){
		var level='bg-green';
				
		if(item.level=='error'){
			level='bg-red';
		}
		if(item.level=='warn'){
			level='bg-yellow';
		}
		if(item.level=='debug'){
			level='bg-light-blue';
		}
		if(item.level=='info'){
			level='bg-green';
		}
		item.level='<span class="label '+level+'">'+item.level+'</span>';
		return item;
	}
}(baseurl);
