 //@ sourceURL=mn_doctor.js
+function(baseurl){
	
	// 查询表
	var action="mn_doctor";
	// 全部查询
	var allFlag=true;
	// 表格对象
	var table=null;
	// 初始化
	+function init(){
		search();
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
	// 查询参数
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
	// 查询
	function search(){
		
		//param
		var param=getParam();
		
		// query
		$.JsonRPC('guard/query',param).done(function(result){
			if(!result) {alert('查询列表失败');return;}
			result.forEach(function(ele){
						ele=html(ele);
					});
			var data=result;
			
			// 表头映射
			var header=[  
						  {title:'id',data:'id'},
				          {title:'姓名',data:'name'},
				          {title:'性别',data:'sex'},
				          {title:'年龄',data:'age'},
				          {title:'电话',data:'phone'},
				          {title:'地址',data:'address'},
				          {title:'城市',data:'city'},
				          {title:'医院',data:'hospital'},
				          {title:'主治',data:'ill'},
				          {title:'经验',data:'years'},
				          {title:'时间',data:'time'}
				        ];
				         
			table=$('#table_view').table(data,header,{check:true});
		});
	}
	// 格式化字段
	function html(item){
		return item;
	}
	
	// 添加
	$('#btn_add').on('click',function(){
		$('#editForm').clear();
		$('#lay_pop').pop({title:'添加医生'});
	});
	
	// 修改
	$('#btn_edit').on('click',function(){
		$('#editForm').clear();
		if(table!=null){
			var id=table.getSelectedId();
			if(id==null||id==""){
				alert("请选择要编辑的记录");
				return;
			}
			// 查询
			$.JsonRPC('guard/queryById',{table:action,id:id}).done(function(result){
				if(!result) { alert('获取编辑对象失败');return;}
				var item=result;
				$('#editForm').form(item);
				$('#lay_pop').pop({title:'修改医生'});	
			});
		}
	});
	
	// 删除
	$('#btn_remove').on('click',function(){
		if(table!=null){
			var id=table.getSelectedIds();
			if(id==null||id==""){
				alert("请选择要删除的记录");
				return;
			}
			// 删除
			$.JsonRPC('guard/remove',{table:action,ids:id}).done(function(result){
				search();
			});
		}
	});
	
	// 添加和更新表单提交
	$('#btn_submit').on('click',function(){
		// 检查
		if(!$('#editForm').check()){
			return;	
		}
		// 提交
		var param=$('#editForm').form();
		// 操作表单
		param.table=action;
		$.JsonRPC('guard/update',param).done(function(result){
			if(result){
				search();
				$('#lay_pop').close();
			}
		});
	});
	
	// mock 回调
	window.mock.callback=search;
}(baseurl);
