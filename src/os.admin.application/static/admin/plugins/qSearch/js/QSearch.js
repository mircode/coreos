/*
 * 用于查询用户和服务
 *
 * Author: weiguoxing
 *
 * Version:  1.0
 * 
 */

;(function($) {
    
  

    var QSearch = function(target,options) {
        this.target = target; // 输入框
        this.container = null; //插件容器
        this.resultct = null; //搜索结果容器
        this.isKeyslect = false; //是否在用上下键选择
        this.isContainerExit = false; // 插件容器是否已存在
        this.isDataInit=false;		// 插件数据是否已经初始化过	
        
        this.searchData=options;
        //构建城市分类字面量
        this.searchTag={
	        hot: {},
	        ABCDEFGH: {},
	        IJKLMNOP: {},
	        QRSTUVWXYZ: {}
	    }
        
    };

    QSearch.prototype = {
        constructor: QSearch,
        //初始化
        init: function() {
        	this.initRecentData();
        	this.initSearchData();
            this.creatItem();
            this.tabChange();
            this.searchSelect();
            this.inputSearch();
            this.keySelect();
            this.stopPropagation();
        },
        initRecentData:function(){
        	this.searchTag.hot['_recent'] = this.get()||[];
        },
        // 初始化用于搜索的数据集
        initSearchData:function(){
        	 
        	if(this.isDataInit) return;
        	
        	var reg_ah = /^[a-h]$/i, // 匹配首字母为 a-h
	        reg_ip = /^[i-p]/i, // 匹配首字母为 i-p
	        reg_qz = /^[q-z]/i; // 匹配首字母为 q-z

	   		 //城市按首字母分类，填充到分类字面量
	        for (var i = 0, len = this.searchData.length; i < len; i++) {
	            var part = this.searchData[i].split('|'),
	            	id = part[0], //id标识
	                en = part[1], //中文名
	                letter = part[2], //拼音
	                spletter = part[3], //拼音简写
	                first = letter[0].toUpperCase(), //拼音首字母
	                ltPart; //当前字母下的城市
	
	            if (reg_ah.test(first)) {
	                ltPart = 'ABCDEFGH';
	            } else if (reg_ip.test(first)) {
	                ltPart = 'IJKLMNOP';
	            } else if (reg_qz.test(first)) {
	                ltPart = 'QRSTUVWXYZ';
	            }
	            var item = {id:id,en:en}
	            this.searchTag[ltPart][first] ? this.searchTag[ltPart][first].push(item) : (this.searchTag[ltPart][first] = [], this.searchTag[ltPart][first].push(item));
	
	            //设置前16个城市为热门城市
	            if (i < 10) {
	                this.searchTag.hot['hot'] ? this.searchTag.hot['hot'].push(item) : (this.searchTag.hot['hot'] = [], this.searchTag.hot['hot'].push(item));
	            }
	            
	           
	        }
	        this.isDataInit=true;
        },
        //创建列表
        creatItem: function() {
            //if(this.isContainerExit) return;
            
            $('.qsearch').remove();
            var template = '<div class="qsearch"><div class="qsearchbox"><h3 class="qsearch_header">搜索(支持汉字/拼音搜索)</h3><ul class="qsearch_nav"><li class="active">最近/常用</li><li>ABCDEFGH</li><li>IJKLMNOP</li><li>QRSTUVWXYZ</li></ul><div class="qsearch_body"></div></div><ul class="result"></ul></div>';
            $('body').append(template);

            this.container = $('.qsearch');
            this.resultct = $('.result');

            for (var group in this.searchTag) {
                var itemKey = [];

                for (var item in this.searchTag[group]) {
                    itemKey.push(item);
                }
                itemKey.sort();
                var itembox = $('<div class="qsearch_item">');
                
                itembox.addClass(group);

                for (var i = 0, iLen = itemKey.length; i < iLen; i++) {
					
					
					var tagKey=itemKey[i];
					if(itemKey[i] == 'hot'){
						tagKey="常用";
					}else if(itemKey[i] == '_recent'){
						tagKey="最近";
					}
					
                    var dl = $('<dl>'),
                        dt = '<dt>' +tagKey+ '</dt>',
                        dd = $('<dd>'),
                        str = '';
					
                    for (var j = 0, jLen = this.searchTag[group][itemKey[i]].length; j < jLen; j++) {
                        str += '<span data-key="'+this.searchTag[group][itemKey[i]][j].id+'">' + this.searchTag[group][itemKey[i]][j].en + '</span>'
                    }
					
                    dd.append(str);
                    dl.append(dt).append(dd);
                    if(str!=''){itembox.append(dl);}
                    
                    
                }
                $('.qsearch_body').append(itembox);
                this.container.find('.hot').addClass('active');
            }
            //this.isContainerExit = true;
        },
        //创建搜索结果列表
        creatResult: function(re, value) {
            var result = re.result,
                len = result.length,
                str = '';
            if (!!len) {
                for (var i = 0; i < len; i++) {
                    str += '<li><span class="name">' + result[i].en + '</span><span class="letter" data-key="'+result[i].id+'">' + result[i].py + '</span></li>'
                }
                this.container.find('.result').html('').html(str).find('li').eq(0).addClass('active');
            } else {
                this.container.find('.result').html('<li>没有找到<span class="noresult">' + value + '</span>相关信息</li>');
            }
        },
        //列表切换
        tabChange: function() {
            $('.qsearch_nav').on('click', 'li', function(e) {
                var current = $(e.target),
                    index = current.index();

                current.addClass('active').siblings().removeClass('active');
                $('.qsearch_item').eq(index).addClass('active').siblings().removeClass('active');
                $('.qsearch_body').scrollTop(0);

            })
        },
        //选择查询项
        searchSelect: function() {
            var self = this;
            $('.qsearch_item dd').on('click', 'span', function(e) {
                self.target.val(($(e.target).text()));
                self.container.hide();
                
                var en=$(e.target).text();
                var id=$(e.target).attr("data-key");
                var item={id:id,en:en}
                
                self.target.attr("data-key",id);
                self.add(item);
            })
        },
        //上下键选择搜索结果
        keySelect: function() {
            var self = this;
            this.target.on('keydown', function(e){
                var current = self.resultct.find('.active').index();
                if(current !== -1){
                    switch(e.keyCode){
                        //上
                        case 38: 
                            keyActive(false);
                            break;
                        //下
                        case 40:
                            keyActive(true);
                            break;
                        //确定
                        case 13: 
                            self.isKeyslect = false;
                            self.target.val(self.resultct.find('.active .name').text());
                            self.triggleShow('all');
                            self.target.blur();
                            
                            var en=self.target.val();
			                var id=self.resultct.find('.active .letter').attr("data-key");
			                var item={id:id,en:en}
			                self.target.attr("data-key",id);
                            self.add(item);
                            break;
                        default: 
                            self.isKeyslect = false;
                            break;
                    }
                }
                function keyActive(isInorder) {
                        var max = self.resultct.find('li').length - 1;
                        if(isInorder){
                            current = current == max ? 0 : current + 1;
                        }else{
                            current = current == 0 ? max : current - 1;
                        }
                        self.resultct.find('li').eq(current).addClass('active').siblings().removeClass('active');
                        self.isKeyslect = true;
                }
            })
        },
        //搜索
        inputSearch: function() {
            var self = this;
            this.target.on('keyup', function(e) {
                if(!self.isKeyslect){
                    self.throttle(search, this);
                }
            })
            // 输入框搜索
            function search(e) {
                var container = self.container;
                self.triggleShow(false);
                var value = $(this).val();
                if (value) {
                   
                    var res=[];
                    for (var i = 0, len = self.searchData.length; i < len; i++) {
                    	var re={};
                    	
                    	var item=self.searchData[i];
                    	
                    	var part = item.split('|'),
                    	id = part[0],
		                en = part[1], //中文名
		                letter = part[2], //拼音
		                spletter = part[3], //拼音简写
		                first = letter[0].toUpperCase(), //拼音首字母
		                ltPart; //当前字母下的城市
	                
                    	if(item.toLowerCase().indexOf(value.toLowerCase())>=0){
                    		re.id=id;
	                    	re.en=en;
	                    	re.py=letter+" | "+id;
	                    	res.push(re);
                    	}
                    	
                    }
                    self.creatResult({result:res}, value);
                    
                } else {
                    self.triggleShow(true);
                }
            }
        },
        //列表，结果，整体 显示切换
        triggleShow: function(open) {
            var container = this.container;
            if (open === 'all') {
                container.hide()
            } else if (open) {
                container.find('.qsearchbox').show().end().find('.result').hide();
            } else {
                container.find('.qsearchbox').hide().end().find('.result').show();
            }
        },
        //函数节流
        throttle: function(fn, context) {
            clearTimeout(fn.tId);
            fn.tId = setTimeout(function(){
                fn.call(context);
            }, 100)
        },
        //阻止事件冒泡
        stopPropagation: function() {
            var self = this;
            //阻止事件冒泡
            this.container.on('click', stopPropagation);
            this.target.on('click', stopPropagation);
            //页面点击 隐藏
            $(document).on('click', function() {
                self.container.hide();
            })
            function stopPropagation(e) {
                e.stopPropagation();
            }
        },
        add:function(item){
        	var selector=$(this.target).attr("id");
        	var itemKey=item.id+"|"+item.en;
        	var recentSearch=localStorage.getItem("recentSearch"+selector)||"";
        	var recentSearchs=recentSearch.split(",");
        	var subChar=",";
        	
        	if(recentSearchs.length>=8){
        		recentSearch=recentSearch.substring(0,recentSearch.lastIndexOf(","));
        	}
        	if(recentSearch==""||recentSearch==itemKey){
        		subChar="";		
        	}
        	// 替换掉重复的元素
        	recentSearch=recentSearch.replace(itemKey+subChar,"").replace(subChar+itemKey,"");
        	recentSearch=itemKey+subChar+recentSearch;
        
        	localStorage.setItem("recentSearch"+selector,recentSearch)
        },
        get:function(){
        	var selector=$(this.target).attr("id");
        	var recentSearch=localStorage.getItem("recentSearch"+selector);
        	if(recentSearch){
	        	var recentSearchs=recentSearch.split(",");
	        	var length=recentSearchs.length;
	        	var arry=[];
        		for(var i=0;i<length;i++){
        			var id=recentSearchs[i].split("|")[0];
        			var en=recentSearchs[i].split("|")[1];
        			var item={id:id,en:en};
        			arry.push(item);
        		}
        		return arry;
        	}else{
        		return [];
        	}
        }
    };

    var qsearch = {};
    $.fn.QSearch = function(options) {
        var target = $(this);
        var selector=target.selector;
        target.on('focus', function(e) {
            var top = $(this).offset().top + $(this).outerHeight(),
                left = $(this).offset().left;
            qsearch[selector] = qsearch[selector] ? qsearch[selector] : new QSearch(target,options);
            qsearch[selector].target = $(e.target);
            qsearch[selector].init();
            qsearch[selector].container.show().offset({
                'top': top + 7,
                'left': left
            });
            qsearch[selector].triggleShow(true);
            qsearch[selector].resultct.on('click', 'li', function() {
                qsearch[selector].target.val($(this).find('.name').text());
                qsearch[selector].triggleShow('all');
                
                var en=$(this).find('.name').text();
                var id=$(this).find('.letter').attr("data-key");
                var item={id:id,en:en}
                
                qsearch[selector].target.attr("data-key",id);
                
                qsearch[selector].add(item);
            })
        });
        return this;
    };
})(jQuery)
