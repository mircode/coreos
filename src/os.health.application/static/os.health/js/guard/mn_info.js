 //@ sourceURL=mn_info.js
+function(baseurl){
	
	// 查询表
	var action="mn_relatives";
	
	// 页面表格对象
	var table_add=null;
	var table_noadd=null;
	
	
	// 初始化
	+function init(){
		info();// 查询用户信息
		setTimeout(function(){$('#mn_relatives_tag').click();},5)
	}();
	
	// 个人信息
	function info(){
		// query
		$.JsonRPC('guard/userInfo',{table:'mn_info'}).done(function(result){
			var item=result;
			// mock
			item.qn_num=item.qn_num||'0';
			item.ys_num=item.ys_num||'0';
			item.zyz_num=item.zyz_num||'0';
			
			item.press=item.press||'0';
			item.fat=item.fat||'0';
			item.sugar=item.sugar||'0';
			item.oxygen=item.oxygen||'0';
			
			// hock
			item.username=item.name;
			item.phonenum=item.phone;
			
			$('#mn_user_info').html('');
			$('#mn_user_info_tmpl').tmpl(item).appendTo('#mn_user_info');
		});
	}
	// 更新个人信息
	function update(){
		var user={};
		var user=$('#editForm').form();
		user.table="mn_info";
		$.JsonRPC('guard/update',user).done(function(result){
			alert("更新成功");
			info();
		});
	}
	// 页签切换
	$('#tag_search a').on('click',function(){
		action=$(this).attr('id').replace("_tag","");
		search(action);
	});
	$("#mn_user_info").on('click','.description-block',function(){
		var id=$(this).find('a.description-text').attr('data-href');
		$(id+"_tag").click();
	});
	
	
	
	//  查询
	function search(action){
		// param
		var param={};
		param.table=action;
		// query
		return $.JsonRPC('guard/query',param).done(function(result){
			
			if(!result) {alert('查询列表失败');return;}
			
			result.forEach(function(ele){
						ele=html(ele);
					});
			var data=result;
			
			var header=[
						  {title:'id',data:'id'},
				          {title:'姓名',data:'name'},
				          {title:'电话',data:'phone'},
				          {title:'城市',data:'city'},
				          {title:'住址',data:'address'},
				          {title:'时间',data:'time'}
				          ];
			
			table_add=$('#'+action).table(data,header,{print:false,check:true});
		});
	}
	function html(item){
		return item;
	}
	
	
	// 编辑
	var editFlag=true;
	$('#btn_edit').on('click',function(){
		$('#mn_user_info input').each(function(){
			if(editFlag){
				$(this).removeAttr('readonly');
				$(this).css({border:'1px solid #3c8dbc'});
			}else{
				$(this).attr('readonly',true);
				$(this).css({border:'1px solid transparent'});
			}
		});
		if(editFlag){
			$(this).html('<i class="fa fa-save">保存</i>');
		}else{
			// 更新用户信息
			update();
			$(this).html('<i class="fa fa-edit">编辑</i>');
		}
		editFlag=!editFlag;
		
	});
	// 添加
	$('#btn_add').on('click',function(){
		
		var href=$("#tag_search li.active").find("a").attr("href").replace("#","");
		var id=href+"_pop";
		
		var title="亲属列表";
		if(href=="mn_relatives"){
			title="亲属列表";	
		}
		if(href=="mn_doctor"){
			title="医生列表";
		}
		if(href=="mn_volunteer"){
			title="志愿列表";
		}
		$('#lay_pop div[data-role]').hide();
		search_noadd(href,id).done(function(){
			action=href;
			$('#'+id).show();
			$('#lay_pop').pop({title:title,width:'800px',height:'550px'});
		});
	});
	$('#btn_remove').on('click',function(){
		var ids=table_add.getSelectedIds();
		if(ids){
			var param={};
			param.table=action;
			param.ids=ids;
			// query
			$.JsonRPC('guard/remove_user_fk',param).done(function(result){
				search(action);
			});
		}
	});
	$('#btn_submit').on('click',function(){
		var ids=table_noadd.getSelectedIds();
		if(ids){
			var param={};
			param.table=action;
			param.ids=ids;
			// query
			$.JsonRPC('guard/add_user_fk',param).done(function(result){
				if(result){
					search(action);
					$('#lay_pop').close();
					$('#lay_pop div[data-role]').hide();
				}
			});
		}
		
	});
	
	
	function search_noadd(action,container){
		// 参数要保持顺序
		var param={};
		// create
		param.table=action;
		// query
		return $.JsonRPC('guard/query_noadd',param).done(function(result){
					if(!result) {alert('查询列表失败');return;}
					
					result.forEach(function(ele){
								ele=html(ele);
							});
					var data=result;
					
					var header=[
								  {title:'id',data:'id'},
						          {title:'姓名',data:'name'},
						          {title:'电话',data:'phone'},
						          {title:'城市',data:'city'},
						          {title:'住址',data:'address'},
						          {title:'时间',data:'time'}
						          ];
					
					table_noadd=$('#'+container).table(data,header,{print:false,check:true,search:true});
					
				});
	}
	
	// mock 回调
	window.mock.callback=info;
}(baseurl);
