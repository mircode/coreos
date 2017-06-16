/**
 * 简化图标调用接口
 * 
 * 
 * @author weiguoxing
 */

(function(baseurl){
	
	var baseurl=baseurl||'/hydrant-view/';
	
	// 简单图标对象	
	var SimpleChart={};
	
	
	// 动态图标
	SimpleChart.Chart=function(options,callback){
		
		// 图表对象
		var chart=null;

		// 变量

		// 图表container容器
		var target=options.target;
		
		// 图表标题
		var title=options.title;

		// 图表类型
		var type=options.type;

		// 图表Y轴标题
		var ytitle=options.ytitle;
		
		// 图表X轴类型
		var xtype=options.xtype||'datetime';
		
		// 图标数据排序规则
		var stype=options.stype;
		
		// 滚动条
		var scrollnum=options.scrollnum;
		var scrollbar=false;
		
		if(scrollnum && type=='column'){
			scrollbar=true;
		}
		
		// 报表导出
		var exporting=options.exporting||false;
		
		// 图标数据项
		var series=options.series;
		
		// 排序
		if(stype){
			sort(series,stype);
		}else{
			if(xtype=='datetime'){
				sort(series,"xasc");
			}else{
				sort(series,"ydesc");
			}
		}
		
		var categories=null;
		// 更具X轴的生成对应的目录
		if(xtype=='category'){
			categories=[];
			Highcharts.each(series[0].data,function(point){
				categories.push(point[0]);
			})
		}
		// 事件
		
		// 初始化成功后回调事件
		var onLoad=callback||options.events.onLoad;
		
		// 鼠标悬停事件
		var onPointHover=options.events.onPointHover;
		
		// 鼠标点击事件
		var onPointClick=options.events.onPointClick;
		
		var defualt = {
				chart: {
					renderTo: target, 	 // 图表Dom选择器
					type: type,          // 图表类型
					zoomType: 'x'
				},
				title:{
					text:title,			 // 图表标题
				},
				xAxis:{
					type: xtype,         // 图表x轴类型 datetime category line
					max:scrollnum,       // 一屏数量
					categories:categories
				},
				yAxis: {
					title: {
						text: ytitle     // 图表Y轴标题
					}
				},
				series:series,           // 图表数据项
				
				tooltip: {			      // 鼠标悬停事件
		            formatter: function() {
		                    return onPointHover(this);
		            }
		        },
				plotOptions: {
		            series: {
		                cursor: 'pointer',
		                point: {
		                    events: {    // 鼠标点击事件
		                        click: function () {
		                        	onPointClick&&onPointClick(this);
		                        }
		                    }
		                },
		                dataLabels: {
		                    align: 'center',
		                    enabled: true
		                }
		            },
		            pie: {
		                allowPointSelect: true,
		                cursor: 'pointer',
		                dataLabels: {
		                    enabled: true,
		                    color: '#000000',
		                    connectorColor: '#000000',
		                    format: '<b>{point.name}</b>: {point.percentage:.1f} %'
		                }
		            }
		        },
				credits: {
					enabled: false
				},
				scrollbar: {
		            enabled: scrollbar
		             
		        },
		        exporting: {
		            enabled: exporting,
		            buttons: {
		                contextButton: {
		                	 text: 'Excel',
		                     menuItems: null,
		                     onclick: function () {
		                        this.exportCSV();// 导出excel表格
		                     }
		                }
		            }
		        }
			};
		
		
		// 根据配置项和回掉函数构造图标
		function drawChart(options,callback){
			
			// 全局配置
			Highcharts.setOptions({                                                     
				global: {                                                               
					useUTC: false                                                       
				}			
			}); 
			
			var chart=new Highcharts.Chart(options,callback);	
			
			return chart;
		}
		
		// 绘制图标
		chart=drawChart(defualt,onLoad);
		return chart;
	}
	// 时序图
	SimpleChart.StackChart=function(options,callback){
		
		
		// 图表对象
		var chart=null;

		// 变量

		// 图表container容器
		var target=options.target;
		
		// 图表标题
		var title=options.title;
		
		// 图表Y轴标题
		var ytitle=options.ytitle;
		
		// 图表X轴类型
		var xtype=options.xtype||'datetime';
		
		// 报表导出
		var exporting=options.exporting||false;
		
		// 图标数据项
		var series=options.series;
		
		// 初始范围
		var selected=options.selected;
		
		// 排序
		if(xtype=='datetime'){
			sort(series,"xasc");
		}else{
			sort(series,"ydesc");
		}
		
		// 事件
		
		// 初始化成功后回调事件
		var onLoad=callback||options.events.onLoad;
		
		// 鼠标悬停事件
		var onPointHover=options.events.onPointHover;
		
		// 鼠标点击事件
		var onPointClick=options.events.onPointClick;
		
		var trange=options.trange||'day';
		
		var rangeSelector=null;
		var dateTimeLabelFormats=null;
		var tickPixelInterval=null;
		if(trange=='day'){
			rangeSelector={
					selected : 3,
		            buttons: [{
		                count: 1,
		                type: 'minute',
		                text: '1分'
		            }, {
		                count: 5,
		                type: 'minute',
		                text: '5分'
		            }, {
		                count: 10,
		                type: 'minute',
		                text: '10分'
		            }, {
		                type: 'all',
		                text: 'All'
		            }],
		            inputEnabled: false,
		            selected: 0
		    };
		    tickPixelInterval=100;
		    dateTimeLabelFormats={
				millisecond: '%H:%M:%S',
				second: '%H:%M:%S',
				minute: '%H:%M',
				hour: '%H:%M',
				day:'%Y-%m-%d',
				week:'%Y-%m-%d',
				month:'%Y-%m',
				year:'%Y'
			}
		   
		}else if(trange=='year'){
			rangeSelector={
					selected : 0,
					inputBoxWidth:90,
					inputDateFormat:'%Y-%m-%d',
					buttons: [{
						type: 'month',
						count: 1,
						text: '1月'
					}, {
						type: 'month',
						count: 3,
						text: '3月'
					}, {
						type: 'month',
						count: 6,
						text: '6月'
					}, {
						type: 'year',
						count: 1,
						text: '1年'
					}, {
						type: 'all',
						text: '全部'
					}]
				};
				
				dateTimeLabelFormats={
						second:'%Y-%m-%d',
						minute:'%Y-%m-%d',
						hour:'%Y-%m-%d',
						day:'%Y-%m-%d',
						week:'%Y-%m-%d',
						month:'%Y-%m',
						year:'%Y'
				};
				
				tickPixelInterval=140;
		}
		 
		if(selected==false){
			delete rangeSelector.selected;
		}
		// 图表配置项
		var defualt={
				chart: {
					renderTo: target,    // 图表Dom选择器
					zoomType: 'x'
		        },
				title : {
					text : title 		// 图表标题
				},
				xAxis: {
		            type: xtype, 		// 图标X轴类型
		            tickPixelInterval:tickPixelInterval,
		            dateTimeLabelFormats:dateTimeLabelFormats
		        },
		        yAxis : {    
		        	title: {
		                text: ytitle 	// 图标Y轴标题
		            }
		        },
		        series:series,          // 图表数据项
		        
		        rangeSelector: rangeSelector,
		        tooltip: {
		        	formatter: function(){  // 鼠标悬浮事件
		                return onPointHover(this); 
		            }
		        },
		        plotOptions: {
		            series: {
		                cursor: 'pointer',
		                point: {           // 鼠标点击事件
		                    events: {     
		                        click: function () {
		                        	onPointClick&&onPointClick(this);
		                        }
		                    }
		                }
		            }
		        },
		        
				credits:{
		            enabled: false
		        },
		        exporting: {
		            enabled: exporting,
		            buttons: {
		                contextButton: {
		                	 text: 'Excel',
		                     menuItems: null,
		                     onclick: function () {
		                        this.exportCSV();// 导出excel表格
		                     }
		                }
		            }
		        }
				
		}
		
		// 根据配置项和回掉函数构造图标
		function drawChart(options,callback){
			
			// 全局配置
			Highcharts.setOptions({
				global:{
					useUTC:false
				},
				lang:{
					rangeSelectorFrom:'统计日期',
					rangeSelectorTo:'至',
					rangeSelectorZoom:'范围',
					weekdays:["星期天", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
					shortMonths:['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
				}
			});
			
			
			var chart=new Highcharts.StockChart(options,callback);
			return chart;
		}
		
		// 绘制图标
		chart=drawChart(defualt,onLoad);
		
		return chart;
	}
	
	
	
	function sort(series,type){
		// 对数据进行排序
		var SORT={
			XDESC:"xdesc",
			XASC:"xasc",
			YDESC:"ydesc",
			YASC:"yasc",
		}
	
		for(var i in series){
			series[i].data.sort(function(obj1,obj2){
				
				var x1=parseInt(obj1[0],10);
				var y1=parseInt(obj1[1],10);
				var x2=parseInt(obj2[0],10);
				var y2=parseInt(obj2[1],10);
				
				if(type==SORT.XASC){
					return x1-x2;
				}else if(type==SORT.XDESC){
					return x2-x1;
				}else if(type==SORT.YASC){
					return y1-y2;
				}else if(type==SORT.YDESC){
					return y2-y1;
				}else{
					return 0;
				}
				
			});
		}
		
		return series;
	}
	
	
	window.SimpleChart=SimpleChart;
})(baseurl);
