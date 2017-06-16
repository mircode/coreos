// mock init
var model=localStorage.getItem("model");
if(model=='debug'){
	$('[data-mock="true"]').show();
}else{
	$('[data-mock="true"]').hide();
}

// 假数据接口
window.debug=function(flag){
	if(flag==undefined||flag){
		window.model='debug';
		localStorage.setItem("model","debug");
		$('[data-mock="true"]').show();
	}else{
		localStorage.setItem("model","normal");
		window.model='normal';
		$('[data-mock="true"]').hide();
	}
}
$('[data-mock="true"]').on('click',function(){
	var table=$(this).attr('data-table');
	$.JsonRPC('mock/mock',{table:table}).done(function(result){
		window.mock.callback();
	});
});