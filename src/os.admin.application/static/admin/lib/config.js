// 服务器基地址
window.baseurl='/jsonrpc/';

//Mock模式
window.mock={callback:function(){}};

//登录拦截
$(document).ajaxComplete(function(event,xhr,settings){
	var url=window.location.href;
	if(xhr.responseText=='{status:-1}'){
		if(url.indexOf('login.html')<0){
			window.location.href='login.html';
		}
	}
});

// Alert封装
window.alert=function(msg){
	layer.msg(msg);
}
window.confirm=function(msg,ok,cancel){
	layer.confirm(msg,{btn:['确定','取消']}
		,function(){
			ok&&ok();
		}
		,function(){
			cancel&&cancal();
		});
}

// Ajax封装
$.JsonRPC=function(url,param){
	var url=window.baseurl+url+'.json';
	param=param||{}
	var deferred=$.Deferred();
	$.ajax({
		    url: url,
		    type:'POST',
		    data:param,
		    dataType: 'json',
		    success:function(data){
		    	deferred.resolve(data.result);
			}
	});
	return deferred;
}
// 组件管理
$.adminRPC=function(url,param){
	var baseurl='/coreos/';
	var url=baseurl+url+'.json';
	param=param||{}
	var deferred=$.Deferred();
	$.ajax({
		    url: url,
		    type:'POST',
		    data:param,
		    dataType: 'json',
		    success:function(data){
		    	deferred.resolve(data.result);
			}
	});
	return deferred;
}
	

// 弹出层分支
$.fn.pop=function(option){
	var title=option.title||'对话框';
	var width=option.width||'600px';
	var height=option.height||'440px';
	layer.open({
			  type: 1,
			  title:title,
			  area: [width,height],
			  shadeClose: true, //点击遮罩关闭
			  content: $(this)
		  });
}
$.fn.close=function(){
	layer.closeAll();
}

// 表单封装
$.fn.form=function(model){
	if(model){
		for(var key in model){
			var value=model[key];
			var $input=$(this).find('input[name="'+key+'"]');
			if($input.attr('type')=='radio'){
				$input.each(function(){
					if($(this).val()==value){
						$(this).prop('checked',true);
					}else{
						$(this).prop('checked',false);
					}
				});
				
			}else{
				$input.val(value);
			}
		}
	}else{
		model={};
		$(this).find('input').each(function(){
			var key=$(this).attr('name');
			var value=$(this).val();
			model[key]=value;
		});
		return model;
	}
}
$.fn.clear=function(){
	$(this).find('.form-group').removeClass("has-error");
	$(this).find('input:hidden').val('');
	$(this)[0].reset();
}
$.fn.check=function(){
	var inputs=$(this).find('input');
	for(var i=0;i<inputs.length;i++){
		$this=$(inputs[i]);
		var id=$this.attr("id");
		var name=$this.attr("name");
		// 非ID字段校验
		if(!(id=="id"||name=="id")){
			if(!$this.val()){
				$this.parents('.form-group').addClass("has-error");
				var html=$this.parents('.form-group').find('label').html();
				if(!html){
					html=$this.attr('placeholder');
				}
				alert(html+'不能为空');
				return false;
			}else{
				$this.parents('.form-group').removeClass("has-error");
			}
		}
	}
	return true;
}

// DataTable封装
var gindex=0;
$.fn.table=function(data,header,options){
	
	var options=options||{};
	
	// 处理数据
	var columns=header;
	if(options.order!==false){
		if(data.length==0||data[0].id){
			// 替换
			columns.splice(0,1,{title:'序号',data:'id'});
		}else{
			// 插入
			columns.splice(0,0,{title:'序号',data:'id'});			
		}
	}
	if(options.check===true){
		if(options.single){
			columns.splice(0,0,{title:'',data:'checkbox'});
		}else{
			columns.splice(0,0,{title:'<input type="checkbox" class="check-all"/>',data:'checkbox'});
		}
	}
	for(var i=0;i<data.length;i++){
		if(options.order!==false){
			data[i].id='<span data-id="'+(data[i].id||(i-0+1))+'">'+(i-0+1)+'</span>';
		}
		if(options.check===true){
			data[i].checkbox='<input type="checkbox"/>';
		}
	}
	
	// 创建DOM元素
	var header='<thead><tr>';
	for(var i=0;i<columns.length;i++){
		header+='<th>'+columns[i].title+'</th>';
	}
	header+='</tr></thead>';
	$(this).html('').html('<table cellpadding="0" cellspacing="0" border="0" class="table table-bordered  table-striped table-hover" id="datatable'+gindex+'" >'+header+'</table>' );
	
	
	// 开启搜索
	var f=options.search===true?'f':'';
	
	// 导出配置
	var exportOptions={
		    format: {
		        header: function (data, columnIdx) {
		            index_num = 0;
		            return data;
		        },
		        body: function (data, columnIdx) {
		            if (columnIdx == 0) {
		                return ++index_num;
		            } else {
		            	return $(data).text()||data;
		            }
		        }
		    }
	};
	// 开启导出和打印
	var buttons=[{text: '导出',extend: 'excel',className: 'btn-export',exportOptions: exportOptions},
		         {text:'打印',title:'',extend: 'print',className: 'btn-export',autoPrint: false}];
		if(options.print===false){
			buttons=[];
		}
	
		
	// 其他配置
	var columnDefs=[];
	var aoColumnDefs=[];
	if(options.check===true){
		columnDefs=[{
           targets: 0,
           searchable: false,
           orderable: false
        }];
        aoColumnDefs=[{sWidth:'1em',aTargets:[0]}]
	}
	
	
	// 创建表格
	var table=$('#datatable'+gindex).DataTable({
			language: {
				sProcessing: "处理中...",
		        sLengthMenu: "显示 _MENU_ 项结果",
		        sZeroRecords: "没有匹配结果",
		        sInfo: "显示第 _START_ 至 _END_ 项结果，共 _TOTAL_ 项",
		        sInfoEmpty: "显示第 0 至 0 项结果，共 0 项",
		        sInfoFiltered: "(由 _MAX_ 项结果过滤)",
		        sInfoPostFix: "",
		        sSearch: "搜索:",
		        sUrl: "",
		        sEmptyTable: "表中数据为空",
		        sLoadingRecords: "载入中...",
		        sInfoThousands: ",",
		        oPaginate: {
		            sFirst: "首页",
		            sPrevious: "上页",
		            sNext: "下页",
		            sLast: "末页"
		        }
		    },
	        buttons:buttons,
	        bAutoWidth:false,
		    sDom: '<"top" B'+f+'>rt<"row" <"col-md-3" l><"col-md-4"  i><"col-md-5"  p>><"clear">',
	        data: data,
	        columns: columns,
	        columnDefs:columnDefs,
	        aoColumnDefs:aoColumnDefs
     					
	});
	
	// 绑定事件
	var $container=$('#datatable'+gindex);
	$container.on('click','tr',function(event){
		var check=$(this).find('input[type="checkbox"]');
		var flag=check.prop('checked');
		
		if(table.rowClick){
	    	var row=table.row($(this)).data();
	    	for(var k in row){
	    		if(row[k].match('s*<.*>')){
	    			if(k=='id'){
	    				row[k]=$(row[k]).attr('data-id')||row[k];
	    			}else{
	    				row[k]=$(row[k]).text()||row[k];
	    			}
	    		}
	    	}
	    	table.rowClick(row);
	    }
		
		 
	    if($(event.target).attr('type')=='checkbox'){
			return;
		}
	    
	    if(options.single){
			$container.find('input[type="checkbox"]').prop('checked',false);
		}
	    
	    if(!check.hasClass('check-all')){
	    	if(flag){
				check.prop('checked',false);
			}else{
				check.prop('checked',true);
			}
	    }
	    
	   
	});
	$container.on('click','.check-all',function(){
		if($(this).prop('checked')){
			$container.find('input[type="checkbox"]').prop('checked',true);
		}else{
			$container.find('input[type="checkbox"]').prop('checked',false);
		}
	});
	
	
	// 搜索
	if($('#search_btn').length>0){
		$('#search_btn').on('click',function(event){
			event.stopPropagation();
		});
		$("#search_btn").keyup(function() {
		   table.search(this.value).draw();
		});  
	}
	
	// 获取选中的行
	table.getSelectedIds=function(){
		var ids=[];
		$container.find('td input:checkbox:checked').each(function(){
			var ele=$(this).parents('tr').find('[data-id]');
			var id=$(ele).attr('data-id');
			ids.push(id);
		});
		return ids.join(',');
	}
	table.getSelectedId=function(){
		var ids=[];
		$container.find('td input:checkbox:checked').each(function(){
			var ele=$(this).parents('tr').find('[data-id]');
			var id=$(ele).attr('data-id');
			ids.push(id);
		});
		return ids[0];
	}
	gindex++;
	return table;
	
}

// 拖拽
$.fn.sortable=function(options){
	$(this).each(function(i,obj){
		Sortable.create(obj,options)
	})
}

// 封装localstorage
window.StoreDB=function(key){this.key=key;}
StoreDB.prototype={
	// 集合存储  key:[]
	add:function(ele){
		var list=this.list();
		if(list.indexOf(ele)<0){
			list.push(ele);
			localStorage.setItem(this.key,list.join(','));
		}
	},
	remove:function(ele){
		var list=this.list();
		var index=list.indexOf(ele);
		if(index>=0){
			list.splice(index,1);
			localStorage.setItem(this.key,list.join(','));
		}
	},
	list:function(){
		var list=localStorage.getItem(this.key);
		return list?list.split(','):[];
	},
	clear:function(){
		localStorage.setItem(this.key,'');
	},
	// 单值存储 key:val
	set:function(ele){
		localStorage.setItem(this.key,ele);
	},
	get:function(){
		return localStorage.getItem(this.key);
	}
}
	

