 //@ sourceURL=login.js
!function(baseurl){
    // init
    var checked=$.cookie('icheck');
    if(checked){
    	 $('#remember').iCheck('check');
    	 var user=$.cookie('user');
    	 var password=$.cookie('password');
    	 $('#user').val(user);
    	 $('#password').val(password);
    }else{
    	 $('#remember').iCheck('uncheck');
    }
    
    // login
    $('#submit_btn').on('click',function(){
    	
		var user=$('#user').val();
    	var password=$('#password').val();
    	
    	user=user.replace(/^\s+|\s+$/ig,'');
    	password=password.replace(/^\s+|\s+$/ig,'');
    	
    	if(user==''){
    		error('用户名为空');
    		return;
    	}
    	if(password==''){
    		error('密码为空');
    		return;
    	}
    	if(checked){
	    	$.cookie('user',user);
	    	$.cookie('password',password);
	    	$.cookie('icheck',true);
    	}else{
    		$.removeCookie('user');
	    	$.removeCookie('password');
	    	$.removeCookie('icheck');
    	}
    	
    	var param={username:user,password:password};
    	$.JsonRPC('login/login',param).done(function(msg){
    		if(msg=='0'){
    			window.location.href='./index.html';
    		}else{
    			error(msg);
    		}
    	});
    });
  
    function error(info){
    	$('#msg').html(info);
    }
    
    
    // remember me
    $('#remember').iCheck({
		    checkboxClass: 'icheckbox_minimal-blue',
			radioClass: 'iradio_minimal-blue',
			increaseArea: '20%' // optional
		 });
    $('#remember').on('ifChecked', function(event){
		  checked=true;
    });
	$('#remember').on('ifUnchecked', function(event){
		  checked=false;
    });
    
 	// 回车
    $(document).keypress(function(e) {  
    	 if(e.which==13){ 
	  		$('#submit_btn').click();
	  	}
	}); 
    
}(baseurl);

