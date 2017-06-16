 //@ sourceURL=user.js
+function(baseurl){
	
	var table=null;
	var target='table_user';
	
	// 初始函数
	+function init(){
		// 初始化图表		
		search();
		// 选择框
		$('input[name="role"]').iCheck({
		    checkboxClass: 'icheckbox_minimal-blue',
			radioClass: 'iradio_minimal-blue',
			increaseArea: '20%' 
		});
		// hock
		$('input[name="password"]').attr('type','password');
		$('input[name="retypepassword"]').attr('type','password');
	}();
	
	// 添加用户
	$('#add_btn').on('click',function(){
		if($('#editForm').check()){
			var user=$('#editForm').form();
			if(user.retypepassword!=user.password){
				alert("确认密码和密码不一致");
				return;
			}	
			user.role=$('input[name="role"]:checked').val();
			$.JsonRPC("user/update",user).done(function(msg){
				if(msg!="0"){
					alert(msg);
				}else{
					clear();
					search();
				}
			});
		}
	});
	
	// 删除
	function remove(id){
		$.JsonRPC("user/remove",{ids:id}).done(function(msg){
			if(msg!="0"){
				alert(msg);
			}else{
				clear();
				search();
			}
		});
	}

	function search(){
		var param={};
		// query
		table=$.JsonRPC('user/query',param).done(function(result){
			
			if(!result) {alert('查询列表失败');return;}
			result.forEach(function(ele){
						ele=html(ele);
					});
			var data=result;
			
			var header=[
						  {title:'id',data:'id'},
				          {title:'用户名',data:'username'},
				          {title:'姓名',data:'realname'},
				          {title:'邮箱',data:'email'},
				          {title:'角色',data:'role'},
				          {title:'注册时间',data:'time'},
				          {title:'操作',data:'opt'}
				          ];
			table=$('#table_user').table(data,header,{print:false,check:true,single:true,search:true});
			
			var rowid=null;
			table.rowClick=function(model){
				if(model&&model.id){
			        if(rowid==null||rowid!=model.id){
			        	// hock
			        	$('input[name="role"]').each(function(){
							var val=$(this).val();
							if(val==model.role){
								$(this).iCheck('check');
							}else{
								$(this).iCheck('uncheck');
							}
						});
						
						$('#editForm').form(model);
				        rowid=model.id;
				        $('#add_btn').html('修改');
			        }else{
			        	clear();
			        	rowid=null;
				        $('#add_btn').html('添加');
			        }
		        }
			}
		});
		
		$("#table_user").on('click','a[data-action="remove"]',function(event){
			var id=$(this).attr("data-id");
			event.preventDefault();
			event.stopPropagation();
			remove(id);
		});
		
	}
	function clear(){
		$('#editForm').clear();
		$('input[name="role"]').each(function(){
			var val=$(this).val();
			if(val=='user'){
				$(this).iCheck('check');
			}else{
				$(this).iCheck('uncheck');
			}
		});
	}
	function html(item){
		item=opt_html(item);
		return item;
	}
	function opt_html(item){
		item.opt="<a href='javascript:void(0)' data-action='remove' data-id='"+item.id+"'>删除</a>";
		return item;
	}
}(baseurl);
