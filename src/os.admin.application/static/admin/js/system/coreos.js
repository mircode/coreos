 //@ sourceURL=coreos.js
+function(baseurl){
	
	
	
	// 初始化
	+function init(){
		// 查询组件仓库
		repertories();
		// 查询网络拓扑
		infos();
	}();
	
	// 请求函数
	function repertories(){
		// 组件仓库信息
		$.adminRPC('admin/repertories').done(function(bundles){
			$('#bundle-chooser').html('');
			$('#bundle_info').tmpl(repertories_translate(bundles)).appendTo($('#bundle-chooser'));
			repertory_bind();
		});
	}
	// 查询网络拓扑
	function infos(){
		// 系统信息
		$.adminRPC('admin/infos').done(function(nodes){
			$('#nodes-container .node[data-type="normal"]').remove();
			$('#coreos_info').tmpl(bundles_translate(nodes)).prependTo($('#nodes-container'));
			coreos_bind();
		});
	}
	
	
	// 执行系统命令
	function execute(action,param){
		param=param||{};
		param.method=action;
		// 系统信息
		$.adminRPC('admin/execute',param).done(function(nodes){
			$('#nodes-container .node[data-type="normal"]').remove();
			$('#coreos_info').tmpl(bundles_translate(nodes)).prependTo($('#nodes-container'));
			coreos_bind();
		});
	}
	
	// 拖拽：安装
	function install(to,bundle){
		
		var action='install';
		var param={addr:to,location:bundle,start:true};
		console.info('%s->%s->%s',action,to,bundle);
		execute(action,param);
	}
	function repertory_install(bundle,num){
		var action='install';
		var param={location:bundle,start:true,num:num};
		console.info('%s->%s->%s',action,bundle,num);
		execute(action,param);
	}
	// 拖拽：迁移
	function move(from,to,bundle,status){
		
		var action='move';
		var param={bundle:bundle,from:from,to:to};
		if(status=='32'){
			param.start=true;
		}
		console.info('%s->%s->%s->%s',action,bundle,from,to);
		execute(action,param);
		
	}
	// 单机：组件启动,停止,卸载,更新
	function bundle_cmd(action,node,bundle){
		var action=action;
		var param={addr:node,bundle:bundle};
		console.info('%s->%s->%s',action,node,bundle);
		execute(action,param);
	}
	// 全局：组件启动,停止,卸载,更新
	function repertory_cmd(action,bundle,num){
		var action=action;
		var param={bundle:bundle};
		if(action=='update'){
			param.timer=num;
		}else if(num){
			param.num=num;
		}
		console.info('%s->%s',action,bundle);
		execute(action,param);
	}
	// 内核
	function coreos_cmd(action,node){
		console.info(action);
		console.info(node);
		if(action=='cmd'){
			command(node);
		}else{
			alert('暂不支持改功能');
		}
	}
	
	//###########
	// 事件绑定
	//###########
	function repertory_bind(){
		// 仓库拖拽
	    $('#bundle-chooser').sortable({
				sort: false,
				filter:'.nodrag',
				group:{
					name: 'advanced',
					pull: 'clone',
					put: false
				},
				animation: 150
		});
		$('#bundle-chooser li').smartMenu(repertory_menu,{name:'repertory'});
	}
	function coreos_bind(){
		// 节点之间拖拽
		$('.bundles-container').sortable({
				sort: true,
				filter:'.nodrag',
				group: {
					name: 'advanced',
					pull: true,
					put: true
				},
				animation: 150,
				onAdd:function(evt){
					var clone=evt.clone;
					var from=evt.from;
					var bundle=evt.item;  
					var to=evt.to;
					
					var lis=$(to).find('li:visible');
					if(lis.length>9){
						alert('超出目标主机最大组件安装个数');
						$(bundle).remove();
						$(clone).smartMenu(repertory_menu,{name:'repertory'});
						var f=$(from).data('role')||$(from).data('node');
						if(f!='repertory'){
							$(from).append(bundle);
						}
						return;
					}
					var installnum=0;
					for(var i=0;i<lis.length;i++){
						var trgt=lis[i];
						
						var src_bdl=$(bundle).data('bundle');
						var src_name=src_bdl.split(':')[0];
						var src_version=src_bdl.split(':')[1];
						
						var tgt_bdl=$(trgt).data('bundle');
						var tgt_name=tgt_bdl.split(':')[0];
						var tgt_version=tgt_bdl.split(':')[1];
						if(src_name.indexOf(tgt_name)>-1&&src_version==tgt_version){
							installnum++;
						}
					}
					if(installnum>=2){
						alert('目标主机已安装改组件');
						$(bundle).remove();
						$(clone).smartMenu(repertory_menu,{name:'repertory'});
						return;
					}
					$(clone).smartMenu(repertory_menu,{name:'repertory'});
					$(bundle).smartMenu(bundle_menu,{name:'bundle'});
					
					var from=$(from).data('role')||$(from).data('node');
					var location=$(bundle).data('location');
					var status=$(bundle).data('status');
					var bundle=$(bundle).data('bundle');
					var to=$(to).data('node');
					if(from=='repertory'){
						install(to,location);
					}else{
						move(from,to,bundle,status);
					}
					
				}
		});
		
		// 双击打开命令行接口
		$('.node').off('dblclick').on('dblclick', function(){ 
			var node=$(this).find('.coreos').data('node');
			coreos_cmd('cmd',node);
		});
		
		$('.bundles-container li').smartMenu(bundle_menu,{name:'bundle'});
	}
	//###########
	// 右键菜单
	//###########
	// 仓库菜单
	var repertory_menu=[
			[{
		        text:'安装',
		        func:function()
		        {
		        	var location=$(this).data('location');
		        	layer.prompt({
						formType:2,
						title:'实例数目',
						value:'1',
						area: ['200px', '30px']
					}, 
					function(value,index){
						layer.close(index);
						repertory_install(location,value);
					});
		        	
		        }
		    },{
		        text:'扩容',
		        func:function()
		        {
		        	var bundle=$(this).data('bundle');
		        	layer.prompt({
						formType:2,
						title:'实例数目',
						value:'1',
						area: ['200px', '30px']
					}, 
					function(value,index){
						layer.close(index);
						repertory_cmd('change',bundle,value);
					});
		        	
		        }
		    },{
		        text:'启动',
		        func:function()
		        {
		        	var bundle=$(this).data('bundle');
		            repertory_cmd('start',bundle);
		        }
		    },{
		        text:'升级',
		        func:function()
		        {
		        	var bundle=$(this).data('bundle');
		            var bundle=$(this).data('bundle');
		        	layer.prompt({
						formType:2,
						title:'时间间隔',
						value:'10',
						area: ['200px', '30px']
					}, 
					function(value,index){
						layer.close(index);
						repertory_cmd('update',bundle,value);
					});
		        }
		    },{
		        text:'重启',
		        func:function()
		        {
		        	var bundle=$(this).data('bundle');
		            repertory_cmd('restart',bundle);
		        }
		    },{
		        text:'停止',
		        func:function()
		        {
		        	var bundle=$(this).data('bundle');
		            repertory_cmd('stop',bundle);
		        }
		    },{
		        text:'卸载',
		        func:function()
		        {
		        	var bundle=$(this).data('bundle');
		            repertory_cmd('uninstall',bundle);
		        }
		    }]
	];
	// 组件菜单
	var bundle_menu=[
			    [{
			        text:'启动',
			        func:function()
			        {
			            var bundle=$(this).data('bundle');
			            var nodes=$(this).parents('ul').data('node');
			            bundle_cmd('start',nodes,bundle);
			        }
			    },{
			        text:'重启',
			        func:function()
			        {
			        	var bundle=$(this).data('bundle');
			            var nodes=$(this).parents('ul').data('node');
			        	bundle_cmd('restart',nodes,bundle);
			        }
			    },{
			        text:'停止',
			        func:function()
			        {
			        	var bundle=$(this).data('bundle');
			            var nodes=$(this).parents('ul').data('node');
			        	bundle_cmd('stop',nodes,bundle);
			        }
			    },{
			        text:'升级',
			        func:function()
			        {
			        	var bundle=$(this).data('bundle');
			            var nodes=$(this).parents('ul').data('node');
			        	bundle_cmd('update',nodes,bundle);
			        }
			    },{
			        text:'卸载',
			        func:function()
			        {
			        	var bundle=$(this).data('bundle');
			            var nodes=$(this).parents('ul').data('node');
			        	bundle_cmd('uninstall',nodes,bundle);
			        }
			    }]
    ];
	
	function command(node){
		
		// 主机端口号减去1000为telnet端口
		var ip=node.split(':')[0];
		var port=node.split(':')[1];
		if(port<=1000){
			port=port-0+1000;
		}else{
			port=port-1000;
		}
		node=ip+':'+port;
		
		layer.open({
				  type: 2,
				  shade: 0,
				  zIndex: layer.zIndex,
				  title: '命令行 '+node,
				  maxmin: true,
				  shadeClose: true,
				  area : ['735px' , '500px'],
				  content: 'pages/system/telnet.html',
				  success: function(layero, index){
					    var body = layer.getChildFrame('body', index);
					    var iframeWin = window[layero.find('iframe')[0]['name']];
					    if(node){
					    	iframeWin.telnet(node);
					    	iframeWin.refresh=infos;
					    	iframeWin.setTitle=function(title){
								parent.layer.title(title,index);
							}
					    	iframeWin.exit=function(){
					    		layer.close(index);
					    	}
					    }
				  },
				  cancel:function(index, layero){ 
					   var body = layer.getChildFrame('body', index);
					   var iframeWin = window[layero.find('iframe')[0]['name']];
					   iframeWin.cancel();
				  }    
		 });
		
	}
	
	//###########
	// 私有函数
	//###########
	// 排序
	var order={
		'os.core':1,
		'os.network':2,
		'os.route':3,
		'os.admin':4,
		'os.moudel.db':5,
		'os.moudel.log':6,
		'os.moudel.person':7,
		'os.moudel.guard':8,
		'os.moudel.user':9,
		'os.health':10,
		'os.other':11
	}
	function repertories_translate(bundles){
		for(var i=0;i<bundles.length;i++){
			translate(bundles[i]);
		}
		bundles.sort(function(obj1,obj2){
			var order1=0;
			var order2=0;
			for(var key in order){
				if(obj1.name.indexOf(key)>-1){
					order1=order[key];
				}
				if(obj2.name.indexOf(key)>-1){
					order2=order[key];
				}
			}
			return order1-order2;
		});
		return {bundles:bundles}
	}
	function bundles_translate(nodes){
		nodes.sort(function(obj1,obj2){
			return (obj1.ip+':'+obj1.port).localeCompare(obj2.ip+':'+obj2.port);
		});
		for(var i=0;i<nodes.length;i++){
			
			var filter=[];
			for(var j=0;j<nodes[i].bundles.length;j++){
				var status=nodes[i].bundles[j].status;
				if(status!='1'){
					filter.push(translate(nodes[i].bundles[j]));	
				}
				
			}
			nodes[i].type='normal';
			nodes[i].color='blue';
			nodes[i].bundles=filter;
			nodes[i].bundles.sort(function(obj1,obj2){
				return order[obj1.name]-order[obj2.name];
			});
		}
		return {nodes:nodes}
	}
	function translate(bundle){
		// 核心组件
		if(bundle.name.indexOf('os.core')>-1){
			bundle.icon='coreos';
			bundle.color='blue';
			bundle.text='系统内核';
			bundle.hidden=true;
		}
		else if(bundle.name.indexOf('os.network')>-1){
			bundle.icon='network';
			bundle.color='blue';
			bundle.text='网卡组件';
		}
		else if(bundle.name.indexOf('os.route')>-1){
			bundle.icon='route';
			bundle.color='blue';
			bundle.text='路由组件';
		}
		else if(bundle.name.indexOf('os.admin')>-1){
			bundle.icon='admin';
			bundle.color='blue';
			bundle.text='管理组件';
		}
		
		// 基础组件
		else if(bundle.name.indexOf('os.moudel.db')>-1){
			bundle.icon='db';
			bundle.color='orange';
			bundle.text='数据组件';
		}
		else if(bundle.name.indexOf('os.moudel.log')>-1){
			bundle.icon='log';
			bundle.color='orange';
			bundle.text='日志组件';
		}
		
		// 业务组件
		else if(bundle.name.indexOf('os.moudel.person')>-1){
			bundle.icon='person';
			bundle.color='orange';
			bundle.text='个人体征';
		}
		else if(bundle.name.indexOf('os.moudel.guard')>-1){
			bundle.icon='guard';
			bundle.color='orange';
			bundle.text='监控组件';
		}
		else if(bundle.name.indexOf('os.moudel.user')>-1){
			bundle.icon='user';
			bundle.color='orange';
			bundle.text='用户管理';
		}
		
		// 应用
		else if(bundle.name.indexOf('os.health')>-1){
			bundle.icon='health';
			bundle.color='green';
			bundle.text='医疗监护';
		}else{
			bundle.icon='other';
			bundle.color='purple';
			bundle.text='其他组件';
		}
		
		if(bundle.status=='2'||bundle.status=='4'){
			bundle.color='gray';
		}
		
		return bundle;
	}
    


	// 主机探测
	var nodes=[];
	var count=0;
	var timer=null;
	
	// 存储
	var store_ky=new StoreDB('ping_key');
	var store_ls=new StoreDB('ping_list');
	
	
	function ping_list(){
		
		// 探测主机列表
		var ping_nodes=store_ls.list();
		$('#ping-chooser').html('');
		$('#ping_nodes').tmpl({ping_nodes:ping_nodes}).appendTo($('#ping-chooser'));
		
		// 探测网段
		if(store_ky.get()){
			$('#new-node').val(store_ky.get().replace(',','\n'));
		}
				
	}
	
	// 初始化
	ping_list(); 
	
	$('#add-new-node').on('click',function(){
		
		infos();
		
		nodes=[];
		count=0;
		timer=null;
		
		var pings=$('#new-node').val();
		if(!pings){return;}
		
		// 根据IP段获取对应的地址
		var addrs=[];
		var arr=pings.split('\n');
		for(var i in arr){
			addrs=addrs.concat(getAddr(arr[i]));
		}
		
		// 探测
		for(var i in addrs){
			count++;
			ping(addrs[i],nodes);
		}
		
		store_ky.set(pings.replace(/\n{2,}/ig,'\n').replace('\n',','));
		
		// 保存结果
		timer=setInterval(function(){
			if(count<=0){
				clearInterval(timer);
				if(nodes.length>0){
					store_ls.clear();
					for(var i in nodes){
						store_ls.add(nodes[i]);
					}
					ping_list();
				}else{
					alert('指定IP段暂未可用主机');
				}
				infos();
			}
		},50);
		
	});
	$('#ping-chooser').on('dblclick','a[data-role="ping_select"]',function(){
		var node=$(this).parents('li').data('node');
		command(node);
	});
	$('#ping-chooser').on('click','a[data-role="ping_rm"]',function(){ 
		var node=$(this).parents('li').data('node');
		store_ls.remove(node);
		ping_list();
	});
	
	function getAddr(addr){
		var ip=addr.split(':')[0];
		var port=addr.split(':')[1];
		
		var addrs=$('#new-node').val();
		if(!addrs){return;}
		
		var ip=addr.split(':')[0];
		var port=addr.split(':')[1];
		
		// 符合规则的IP
		var ips=[];
		if(ip.indexOf('.'>-1)){
			var last=ip.split('.')[3];
			if(last==='*'){
				for(var i=0;i<255;i++){
					ips.push(ip.replace(last,i));
				}
			}else if(last.indexOf('~')>-1){
				var start=last.split('~')[0];
				var end=last.split('~')[1];
				for(var i=start;i<=end;i++){
					ips.push(ip.replace(last,i));
				}
			}else{
				ips.push(ip);
			}
		}else{
			ips.push(ip);
		}
		
		// 符合规则的PORT
		var ports=[];
		if(port.indexOf('~')>-1){
			var start=port.split('~')[0];
			var end=port.split('~')[1];
			if(start>end){
				alert('端口返回错误');
				return;
			}
			for(var i=start;i<=end;i++){
				ports.push(i);
			}
		}else{
			ports.push(port);
		}
		
		// 符合规则的IP:PROT串
		var addrs=[];
		for(var i in ips){
			for(var j in ports){
				addrs.push(ips[i]+':'+ports[j]);
			}
		}
		return addrs;
	}
	// 端口探测
	function ping(addr,nodes){
		var ip=addr.split(':')[0];
		var port=addr.split(':')[1];
		if(port<=1000){
			port=port-0+1000;
		}else{
			port=port-1000;
		}
		var socket=new WebSocket('ws://'+ip+':'+port+'/');
		socket.onopen=function(event){
			console.info('%s coreos running',addr);
			nodes.push(addr);
			count--;
		}
		socket.onerror=function(event){
			console.info('%s no coreos',addr);
			count--;
		}
	}
	// 一键安装
	$('#one_install').on('click',function(){
		var len=$('#bundle-chooser').find(' .node').length;
		var num=len%3+1;
		var param={};
		param['os.health']=num;
		param['os.moudel.user']=num;
		param['os.moudel.person']=num;
		param['os.moudel.guard']=num;
		param['os.moudel.log']=num;
		param['os.moudel.db']=num;
		
		$('#editForm').form(param);
		$('#lay_pop').pop({title:'一键部署',height:'300px',width:'490px'});
	});
	// 添加和更新表单提交
	$('#btn_submit').on('click',function(){
		// 检查
		if(!$('#editForm').check()){
			return;	
		}
		// 提交
		var param=$('#editForm').form();
		// 系统信息
		$.adminRPC('admin/oneInstall',param).done(function(nodes){
			$('#nodes-container .node[data-type="normal"]').remove();
			$('#coreos_info').tmpl(bundles_translate(nodes)).prependTo($('#nodes-container'));
			coreos_bind();
			$('#lay_pop').close();
		});
		
		
	});
}(baseurl);
