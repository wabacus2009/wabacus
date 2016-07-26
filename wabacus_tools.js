var EventTools = new Object;//处理事件的工具类

EventTools.addEventHandler = function (oTarget, sEventType, fnHandler) 
{
    if (oTarget.addEventListener) 
    {
        oTarget.addEventListener(sEventType, fnHandler, false);
    } else if (oTarget.attachEvent) 
    {
        oTarget.attachEvent("on" + sEventType, fnHandler);
    } else 
    {
        oTarget["on" + sEventType] = fnHandler;
    }
}
EventTools.removeEventHandler = function (oTarget, sEventType, fnHandler) {
    if (oTarget.removeEventListener) {
        oTarget.removeEventListener(sEventType, fnHandler, false);
    } else if (oTarget.detachEvent) {
        oTarget.detachEvent("on" + sEventType, fnHandler);
    } else { 
        oTarget["on" + sEventType] = null;
    }
}

EventTools.formatEvent = function (oEvent) 
{
    if (typeof oEvent.charCode == "undefined") 
    {
        oEvent.charCode = (oEvent.type == "keypress") ? oEvent.keyCode : 0;
        oEvent.isChar = (oEvent.charCode > 0);
    }
    if (oEvent.srcElement && !oEvent.target) 
    {
        oEvent.eventPhase = 2;
        var documentScrollSize=getDocumentScroll();
        oEvent.pageX = oEvent.clientX + documentScrollSize.scrollLeft- document.body.clientLeft;
        oEvent.pageY = oEvent.clientY + documentScrollSize.scrollTop- document.body.clientTop;
        if (!oEvent.preventDefault) 
        {
                oEvent.preventDefault = function () 
                {
                    this.returnValue = false;
                };
        }

        if (oEvent.type == "mouseout") {
            oEvent.relatedTarget = oEvent.toElement;
        } else if (oEvent.type == "mouseover") {
            oEvent.relatedTarget = oEvent.fromElement;
        }

        if (!oEvent.stopPropagation) {
                oEvent.stopPropagation = function () {
                    this.cancelBubble = true;
                };
        }
        if ( typeof oEvent.button == " undefined " ) {
            oEvent.button = oEvent.which;
        }
        oEvent.target = oEvent.srcElement;
        oEvent.time = (new Date).getTime();
    
    }
    
    return oEvent;
}

EventTools.getEvent = function() 
{
    if (window.event) 
    {
        return this.formatEvent(window.event);
    } else 
    {
        return EventTools.getEvent.caller.arguments[0];
    }
}
// 使用正则表达式，检测 s 是否满足模式 re
function checkExp( re, s )
{
	//return re.test( s );
	return s.match(re);
}

/**
 * 去除字符串左右空格
 */
String.prototype.trim = function() 
{ 
	return this.replace(/(^\s*)|(\s*$)/g, ""); 
}; 
// 验证是否 数字
function isWXNumber(strValue)
{
	if(strValue==null||strValue=='') return false;
	if( !checkExp( /^[+-]?\d+(\.\d+)?$/g, strValue ))
	{
		return false;
	}
	return true;
}
/**
 *返回某个字符串的字节数，一个中文为两个字节
 */
String.prototype.getBytesLength = function() 
{ 
	return this.replace(/[^\x00-\xff]/gi, "--").length; 
}; 


/********************************************************************************
 *处理ajax提交请求
 */
var XMLHttpREPORT = {
    _objPool: [],    
    _getInstance: function ()
    {
        for (var i = 0; i < this._objPool.length; i++)
        {
            if (this._objPool[i].readyState == 0 || this._objPool[i].readyState == 4)
            {
                return this._objPool[i];
            }
        }
        this._objPool[this._objPool.length] = this._createObj();

        return this._objPool[this._objPool.length - 1];
        //return this._createObj();
    },
    _createObj: function ()
    {
        if (window.XMLHttpRequest)
        {
            var objXMLHttp = new XMLHttpRequest();
        }
        else
        {
            /*var MSXML = ['MSXML2.XMLHTTP.5.0', 'MSXML2.XMLHTTP.4.0', 'MSXML2.XMLHTTP.3.0', 'MSXML2.XMLHTTP', 'Microsoft.XMLHTTP'];
            for(var n = 0; n < MSXML.length; n ++)
            {
                try
                {
                    var objXMLHttp = new ActiveXObject(MSXML[n]);        
                    break;
                }
                catch(e)
                {
                }
            }*/
            var objXMLHttp=new ActiveXObject("Microsoft.XMLHTTP");
         }          
        if (objXMLHttp.readyState == null)
        {
            objXMLHttp.readyState = 0;
            objXMLHttp.addEventListener("load", function ()
                {
                    objXMLHttp.readyState = 4;
                    
                    if (typeof objXMLHttp.onreadystatechange == "function")
                    {
                        objXMLHttp.onreadystatechange();
                    }
                },  false);
        }
        return objXMLHttp;
    },
    
   /**
    * dataObj 存放本次请求需要回传给回调函数的数据对象
    */
    sendReq: function (method, url, data, callbackmethod,onErrorMethod,dataObj)
    {
        var objXMLHttp =this._createObj();// this._getInstance();

        with(objXMLHttp)
        {
            try
            {
                if (url.indexOf("?") > 0)
                {
                    url += "&randnum=" + Math.random();
                }
                else
                {
                    url += "?randnum=" + Math.random();
                }
                open(method, url, true);
                setRequestHeader('Content-Type', 'application/x-www-form-urlencoded;charset=utf-8');
                send(data);
                onreadystatechange = function ()
                {  
                    if (objXMLHttp.readyState == 4)
                    {
                        if(objXMLHttp.status == 200 || objXMLHttp.status == 304)
                        {
                        	if(callbackmethod!=null) callbackmethod(objXMLHttp,dataObj);
                        }else
                        {
                        	if(onErrorMethod!=null) onErrorMethod(objXMLHttp,dataObj);
                        }
                    }
                }
            }
            catch(e)
            {
                alert(e);
            }finally
            {
            }
        }
    }
}; 

/**
 * 向后台发送异步请求
 * @param serverurl 发起请求的URL
 * @param successCallback 后台处理成功时的回调函数
 * @param failedCallback 请求失败时的回调函数
 * @param datasObj 请求成功时传给successCallback方法的参数对象
 */
function sendAsynRequestToServer(serverurl,successCallback,failedCallback,datasObj)
{
	if(serverurl==null||serverurl=='') return;
	var tmpArray = serverurl.split("?");
	if(tmpArray==null||tmpArray.length<=1)
	{
		tmpArray[1]='';	
	}else if(tmpArray.length>=2)
	{//有多个问号，则只取第一个问号的前面为URL，后面都是参数
	  	if(tmpArray.length>2)
		{
			for(var k=2;k<tmpArray.length;k=k+1)
			{
 				tmpArray[1]=tmpArray[1]+'?'+tmpArray[k];
			}
		}
 	}		
	XMLHttpREPORT.sendReq('POST',tmpArray[0],tmpArray[1],successCallback,failedCallback,datasObj);
}

/**
 * 获取浏览器窗口大小
 */
function getDocumentSize()
{  
 	if(document.compatMode == "BackCompat"&&document.body)
 	{  
 		return { width: document.body.clientWidth,height: document.body.clientHeight};
 	}else 
 	{  
 		return {width: document.documentElement.clientWidth,height: document.documentElement.clientHeight};
 	}
} 

/**
 * 获取网页滚动条位置信息
 */
function getDocumentScroll() 
{
	var t=0, l=0, w=0, h=0;
	if (document.compatMode == "BackCompat"&&document.body||isChrome)
	{
		t = document.body.scrollTop;
		l = document.body.scrollLeft;
		w = document.body.scrollWidth;
		h = document.body.scrollHeight;
	}else if (document.documentElement) 
	{
		t = document.documentElement.scrollTop;
		l = document.documentElement.scrollLeft;
		w = document.documentElement.scrollWidth;
		h = document.documentElement.scrollHeight;
	}
	return {scrollTop:t, scrollLeft:l, scrollWidth:w, scrollHeight:h};
}
/**
 * 获取所有父容器的滚动条滚动值（如果有滚动条的话）
 */
function getAllParentScrollOffsetValue(element) 
{
	var scrollX=0;
	var scrollY=0;
	element = element.offsetParent;
	while (element != null) 
	{
		if(element.scrollLeft)
		{
			scrollX += element.scrollLeft;
		}
		if(element.scrollTop)
		{
			scrollY += element.scrollTop;
		}
		element = element.parentNode;
	}
	return {scrollWidth:scrollX,scrollHeight:scrollY};
}
/**
 * 获取某个元素的绝对位置，考虑了document和父容器的滚动条滚动值。
 */
function getElementAbsolutePosition(element) 
{
	if (!element) 
	{
		return null;
	}
	var eleTmp=element;
	var theElemHeight = eleTmp.offsetHeight;
	var theElemWidth = eleTmp.offsetWidth;
	var txtboxPosX=0;
	var txtboxPosY=0;
	while (eleTmp != null) 
	{
		txtboxPosX += eleTmp.offsetLeft;
		if(eleTmp.className=='cls-fixed-headerInner'&&isIE)
		{//如果element是冻结行列标题中非冻结列上的子元素，则在IE中需要将标题向左移动（滚动条向右移动）的位置减掉，这是因为在IE中，没有将移动值包括在offsetLeft中，而其它浏览器会包含在其中，所以在IE中要手工减掉
			if(eleTmp.style.right!=null&&eleTmp.style.right!='') txtboxPosX=txtboxPosX-parseInt(eleTmp.style.right);
		}
		txtboxPosY += eleTmp.offsetTop;
		eleTmp = eleTmp.offsetParent;
	}
	
	//父容器滚动条
	var scroll=getAllParentScrollOffsetValue(element);
	if(scroll) 
	{
		//alert(scroll.scrollWidth);
		txtboxPosX=txtboxPosX-scroll.scrollWidth;
		txtboxPosY=txtboxPosY-scroll.scrollHeight;
	}
	scroll=getDocumentScroll();
	if(scroll)
	{
		txtboxPosX=txtboxPosX+scroll.scrollLeft;
		txtboxPosY=txtboxPosY+scroll.scrollTop;
	}
	return {top:txtboxPosY,left:txtboxPosX,width:theElemWidth,height:theElemHeight};
}

/**
 * 去除src字符串中，startstr与endstr之间的子串，并将startstr之前的子串与endstr之后的子串用link连接起来
 * 如果没有传入link，则直接拼凑起来返回
 * 如果src中不包括startstr，则直接返回src，不做任何处理
 * 如果src中不包括endstr，则返回src中startstr之前的所有内容，且不会拼上link
 */
function removeSubStr(src,startstr,endstr,link)
{
	if(src==null||src==''||startstr==null||startstr==''||endstr==null||endstr=='') return src;
	if(link==null) link='';
	var idx=src.indexOf(startstr);
	while(idx>=0)
	{
		var srcstart=src.substring(0,idx);
		src=src.substring(idx+startstr.length);
		idx=src.indexOf(endstr);
		if(idx<0)
		{ 
			src=srcstart;
			break;
		}
		var srcend=src.substring(idx+endstr.length);
		if(srcend=='')
		{
			src=srcstart;
			break;
		}
		src=srcstart+link+srcend;
		idx=src.indexOf(startstr);
	}
	return src;
}

/**
 *	获取src字符串中startstr与endstr之间的子串
 * 其中获取的结果字符串不包括startstr,endstr本身
 *	如果src中不包括startstr，则返回空，如果不包括endstr，则返回startstr后面的所有字符串
 */
function getSubStrValue(src,startstr,endstr)
{
	if(src==null||src==''||startstr==null||startstr==''||endstr==null||endstr=='') return '';
	var idxTmp=src.indexOf(startstr);
  	if(idxTmp<0) return '';
  	var result=src.substring(idxTmp+startstr.length);
  	idxTmp=result.indexOf(endstr);
  	if(idxTmp>=0)
  	{
  		result=result.substring(0,idxTmp);
  	}
  	return result;
}

/**
 * 判断某个字符串是否是正整数
 */
function isPositiveInteger(str)
{
	if(str==null||str=='') return false;
	var r='^[1-9][0-9]*$';
	return str.match(r);
}

/**
 * 有时候服务器端在传入某个字符串参数到javascript方法中时，这个字符串可能包括'、"、$等符号，
 * 而又不能对它们进行urlEncode编码，此时会在传入前替换掉这些特殊字符，然后在javascript方法
 * 中使用它们时，调用下面的方法进行解码，即替换回来。
 */
function paramdecode(paramvalue)
{
   if(paramvalue==null||paramvalue=='') return paramvalue;
   paramvalue=paramvalue.replace(/wx_QUOTE_wx/g,'\'');
   paramvalue=paramvalue.replace(/wx_DBLQUOTE_wx/g,'"');
   paramvalue=paramvalue.replace(/wx_DOLLAR_wx/g,'$');
   return paramvalue;
} 

/**
 * 为了能正常拼凑到json中，对paramvalue进行必要的编码
 * @param onlyNewline 是否只替换回车键，此方法一般只针对整个json字符串进行编码时用上，因为此时不能替换其中的单引号和双引号，因为会把json中表示字符串的单引号和双引号替换了，此时只能替换回车，因为回车会导致json转换成对象失败
 *			如果是对参数值进行替换，则可以替换所有特殊字符，此时不需传入此参数值，或传入false
 */
function jsonParamEncode(paramvalue,onlyNewline)
{
	if(paramvalue==null||paramvalue=='') return paramvalue;
	var reg=null;
	if(!onlyNewline)
	{//不是只替换回车符
		reg=new RegExp('\'','g');
		paramvalue=paramvalue.replace(reg,'wx_json_QUOTE_wx');
		reg=new RegExp('\"','g');
		paramvalue=paramvalue.replace(reg,'wx_json_DBLQUOTE_wx');
	}
	reg=new RegExp('\n','g');
	paramvalue=paramvalue.replace(reg,'wx_json_NEWLINE_wx');
	return paramvalue;
}

/**
 * 对jsonParamEncode方法编码的字符串进行解码
 */
function jsonParamDecode(paramvalue)
{
	if(paramvalue==null||paramvalue=='') return paramvalue;
	var reg=new RegExp('wx_json_QUOTE_wx','g');
	paramvalue=paramvalue.replace(reg,'\'');
	reg=new RegExp('wx_json_DBLQUOTE_wx','g');
	paramvalue=paramvalue.replace(reg,'\"');
	reg=new RegExp('wx_json_NEWLINE_wx','g');
	paramvalue=paramvalue.replace(reg,'\n');
	return paramvalue;
}

 /**
 *判断某个Map对象是否为空
 *@param mapDataObj 待判断的Map对象，类型为Object
 */
function isEmptyMap(mapDataObj)
{
	if(mapDataObj==null) return true;
	for(var key in mapDataObj)
	{
		//alert(key);
		return false;
	}
	return true;
}

/**
 * 获取某个html标签的背景色
 */
function getElementBgColor(element)
{
	var bgcolor=element.style.backgroundColor;
	if(bgcolor!=null&&bgcolor!='')
	{//直接在tag上配置了style="background-color:...;"，它的优先级最高
		return bgcolor;
	}
	if (document.defaultView && document.defaultView.getComputedStyle) 
	{//firefox
    	var style = document.defaultView.getComputedStyle(element, null);                        
	    if(style) bgcolor=style.backgroundColor;
    }else if (element.currentStyle) 
	{//ie,opera                                                   
      	bgcolor = element.currentStyle.backgroundColor;      
    }
    if(bgcolor!=null&&bgcolor!='')
    {//通过外部css文件控制了当前tag的背景色
    	return bgcolor;
    }  
	return element.bgColor;
}


/**
 * 获取某个标签的某个名称的父标签
 * @param element 标签对象
 * @param parentTagName 要获取的父标签名
 */
function getParentElementObj(element,parentTagName)
{
	if(!element||!parentTagName) return null;
	var parentElement=element.parentNode;
	if(!parentElement) return null;
	if(parentElement.tagName==parentTagName)
	{
		return parentElement;
	}else
	{
		return getParentElementObj(parentElement,parentTagName);
	}
}

/**
 * 当前结点元素是否是某个元素或其子元素
 * @param eleObj 要判断的元素对象
 * @param rootid 要判断的顶层元素的ID
 */
function isElementOrChildElement(eleObj,rootid)
{
	while(eleObj!=null)
	{
		try
		{
			if(eleObj.getAttribute('id')==rootid) return true;
		}catch(e)
		{//因为到document后就没有getAttribute()方法了，因此会抛出异常
			break;
		}
		eleObj=eleObj.parentNode;
	}
	return false;
}

/**
 * 获取json字符串对应的对象
 */
function getObjectByJsonString(jsonStr)
{
	if(jsonStr==null||jsonStr==''||jsonStr=='null') return null;
	jsonStr=paramdecode(jsonStr);
	jsonStr=jsonParamEncode(jsonStr,true);//去掉回车符，否则转换时会报错
	var obj=eval('('+jsonStr+')');
	return obj;
}

/**
 *采用post方式提交href，实现的功能类似于：
 *<a href="linkurl" method="post"/>
 */
function postlinkurl(url,newwindow)
{
	if(url==null||url=='') return;
	var doc=document;
   var frm=doc.createElement("Form");
	frm.method="post";
	if(newwindow===true) frm.setAttribute('target','_blank');
	/**
	 * 因为url是做为字符串类型的参数传递的，因此为了不让它破坏参数结构，在调用此方法前，
	 * 将url进行编码，下面将它进行解码，还原出来
	 */
	url=paramdecode(url);
	var urlArray=splitUrlAndParams(url,true);
	var posturl=urlArray[0];
	var urlParamsObj=urlArray[1];
  	if(urlParamsObj!=null)
  	{
  	 	var paramValueTmp;
  	   for(var paramNameTmp in urlParamsObj)
  	   {
  	   	if(paramNameTmp==null||paramNameTmp=='') continue;
  	   	paramValueTmp=urlParamsObj[paramNameTmp];
  	   	if(paramValueTmp==null) paramValueTmp='';
  	    	var hid=doc.createElement("input");
	  		hid.setAttribute("name",paramNameTmp);
	  		hid.setAttribute("value",paramValueTmp);
	  		hid.setAttribute("type","hidden");
	  		frm.appendChild(hid);
  	   }
  	}
   frm.action=posturl;
   doc.body.appendChild(frm);
   frm.submit();
   doc.body.removeChild(frm);
}

/**
 * 如果URL中存在paramname参数名对应的参数值，则将其替换为新值newvalue，
 * 如果不存在此参数名的参数，则将paramname=newvalue加入URL中
 * @param url 
 * @param paramname 参数名
 * @param newvalue 此参数名的新值，如果为null，则相当从URL中删除此参数（注意：如果newvalue为''也会添加到URL中）
 */
function replaceUrlParamValue(url,paramname,newvalue,ignoreEncode)
{
	if(url==null||url=='') return url;
	if(paramname==null||paramname=='') return url;
	url=removeSubStr(url,'?'+paramname+'=','&','?');
	url=removeSubStr(url,'&'+paramname+'=','&','&');
	if(newvalue!=null)
	{
		if(url.indexOf('?')>0)
		{
			url=url+'&';
		}else
		{
			url=url+'?';
		}
		if(ignoreEncode!==true) newvalue=encodeURIComponent(newvalue);
		url=url+paramname+'='+newvalue;
	}
	return url;
}

/**
 * 从URL中获取某个参数值
 * @param url URL
 * @param paramname 要获取参数值的参数名
 */
function getParamValueFromUrl(url,paramname)
{
	if(url==null||url=='') return '';
	if(paramname==null||paramname=='') return '';
	var paramvalue=getSubStrValue(url,'?'+paramname+'=','&');
	if(paramvalue=='')
	{
		paramvalue=getSubStrValue(url,'&'+paramname+'=','&');
	}
	return paramvalue;
}

/**
 * 分解URL中的URI及其参数，返回的是一个有两个元素的数组，第一个元素是不带参数的URL，第二个是存放参数的Object对象，其中key为参数名，value为参数值
 * @param urldeocde 是否需要对URL中的参数进行URL解码
 */
function splitUrlAndParams(url,urldecode)
{
	if(url==null||url=='') return null;
	var resultArray=new Array();
	var idx=url.indexOf('?');
	if(idx==0) return null;
	if(idx<0)
	{//没有参数
		resultArray[0]=url;
		resultArray[1]=null;
		return resultArray;
	}
	var urlParam=url.substring(idx+1);
	url=url.substring(0,idx);
	if(urlParam=='')
	{//没有参数
		resultArray[0]=url;
		resultArray[1]=null;
		return resultArray;
	}
  	var paramsObj=new Object();
  	var paramsArray=urlParam.split("&");
  	var paramTmp,paramNameTmp,paramValueTmp;
  	for(var i=0,len=paramsArray.length;i<len;i=i+1)
  	{
  		paramTmp=paramsArray[i];
  	  	if(paramTmp==null||paramTmp=='') continue;
  	   idx=paramTmp.indexOf('=');
  	   if(idx==0) continue;
  	   if(idx>0)
  	   {//有等号，说明有参数值
  	   	paramNameTmp=paramTmp.substring(0,idx);
  	   	paramValueTmp=paramTmp.substring(idx+1);
  	   	if(urldecode) paramValueTmp=decodeURIComponent(paramValueTmp);//需要进行url解码
  	   }else
  	   {
  	   	paramNameTmp=paramTmp;
  	   	paramValueTmp='';
  	   }
  	   paramsObj[paramNameTmp]=paramValueTmp;
  	}
   resultArray[0]=url;
	resultArray[1]=paramsObj;
	return resultArray;
}

/**
 * 去掉字符串str左边所有token字符，如果没有传入token，或传入''，则去掉所有左边的空格符号
 * @param str 处理的字符串
 * @param token 要去掉的字符，默认为空格
 */
function wx_ltrim(str,token)
{
	if(str==null||str=='') return str;
	if(token==null||token=='') token=' ';
	var idx=str.indexOf(token);
	while(idx==0)
	{
		str=str.substring(token.length);
		idx=str.indexOf(token);
	}
	return str;
}

/**
 * 去掉字符串str右边所有token字符，如果没有传入token，或传入''，则去掉所有右边的空格符号
 * @param str 处理的字符串
 * @param token 要去掉的字符，默认为空格
 */
function wx_rtrim(str,token)
{
	if(str==null||str=='') return str;
	if(token==null||token=='') token=' ';
	var idx=str.lastIndexOf(token);
	while(idx>=0&&idx==str.length-token.length)
	{
		str=str.substring(0,str.length-token.length);
		idx=str.lastIndexOf(token);
	}
	return str;
}


/**
 * 去掉字符串str左右所有token字符，如果没有传入token，或传入''，则去掉左右所有空格符号
 * @param str 处理的字符串
 * @param token 要去掉的字符，默认为空格
 */
function wx_trim(str,token)
{
	str=wx_ltrim(str,token);
	str=wx_rtrim(str,token);
	return str;
}

/**
 * 获取html某个标签属性字符串中指定属性名的属性值
 * @param htmlProperties 例如：title="测试tip" style='background-color:red;border:solid 1px #ff00ff' onmouseover="this.style.borderColor='#999900';"
 * @param propname 要获取属性值的属性名 例如style
 */
function getPropertyValueFromHtmlProperties(htmlProperties,propname)
{
	var result=new Array();
	result[0]=htmlProperties;
	result[1]='';
	if(propname==null||propname==''||htmlProperties==null||htmlProperties=='') return result;
	htmlProperties=' '+htmlProperties;//方便判断.indexOf(' '+propname+'=')
	var idx=htmlProperties.toLowerCase().indexOf(' '+propname+'=');
	if(idx<0) return result;//不包含这个属性
	var propvalue='';
	var str1=htmlProperties.substring(0,idx);
	var str2=wx_trim(htmlProperties.substring(idx+(' '+propname+'=').length));
	if(str2.length>0)
	{
		var quote=str2.substring(0,1);
		if(quote!='\''&&quote!='"') 
		{//如果propname=后面不是单引号也不是双引号，则以空格做为与其它属性的分隔符
		  quote=' ';
		  str2=wx_trim(str2);
		}else 
		{
		  str2=str2.substring(1);//去掉第一个单引号或双引号
		}
		idx=str2.indexOf(quote);
		if(idx<0)
		{//没有取到结束字符，则到最后
			propvalue=str2.substring(0,str2.length);
			str2='';
		}else
		{
			propvalue=str2.substring(0,idx);
			str2=str2.substring(idx+1);
		}
	}
	result[0]=str1+" "+str2;
	result[1]=propvalue;
	return result;
}


/**
 * 判断某个对象是否是数组
 */
function isArray(obj)
{
	return Object.prototype.toString.call(obj)=='[object Array]';
}

/**
 * 将一个对象转变为数组，包括两种情况：
 *		如果obj本身就是数组，则直接返回；
 *		如果obj不是一个数组，而是一个不为null的其它对象，则建立一个数组存放它，然后返回此数组
 * 此方法一般用来转化某个即可以接受一个数组，又可以接受一个普通对象的场合，用这个方法转换成数组后可以统一处理
 */
function convertToArray(obj)
{
	if(obj==null) return null;
	if(isArray(obj)) return obj;//已经是一个数组
	var resultArr=new Array();
	resultArr[resultArr.length]=obj;
	return resultArr;
}

/**
 * 找到text中符合正则表达式regExp的第一个匹配子串，并将其转换为一个包括此匹配子串前面部分、匹配子串部分、匹配子串后面部分三个属性的对象返回
 * 如果没有找到匹配的子串，则返回null
 * @param text 要查找的源字符串
 * @param regExp 正则表达式RegExp对象
 * @param len 要找的目标字符串的长度
 */
function splitTextValues(text,regExp,len)
{
	var startPos=text.search(regExp);
	if(startPos<0) return null;//没有找到
	var matchText=text.substring(startPos,startPos+len);//取到匹配子串
	var startText='';
	if(startPos>0) startText=text.substring(0,startPos);//取到匹配子串的前面部分
	var endText=text.substring(startPos+len);//匹配子串的后面部分
	return {start:startText,mid:matchText,end:endText};
}

/**
 * 将以token分隔的字符串str解析成数组，不包括空字符串部分
 * @param includeBlank 空字符串是否做为一个有效的子串放入数组中
 */
function parseStringToArray(str,token,includeBlank)
{
	if(str==null||str==''||token==null) return null;
	if(token=='') return str;
	var resultArr=new Array();
	var arrTmp=str.split(token);
	for(var i=0;i<arrTmp.length;i++)
	{
		if(arrTmp[i]==null||arrTmp[i]=='') continue;
		if(includeBlank===false&&wx_trim(arrTmp[i])=='') continue; 
		resultArr[resultArr.length]=arrTmp[i];
	}
	return resultArr;
}

/**
 *
 * 解析content中某个起始标签和结束标签的内容，返回一个数组，数组第一个元素为标签中间的内容，第二个元素为删掉此标签及内容后剩下的部分
 */
function parseTagContent(content,startTag,endTag)
{
	if(content==null||content.indexOf(startTag)<0||content.indexOf(endTag)<0) return null;
	var idx=content.indexOf(startTag);
	var str1=content.substring(0,idx);//起始标签前面部分
	content=content.substring(idx+startTag.length);
	idx=content.indexOf(endTag);
	if(idx<0) return null;
	var str2=content.substring(0,idx);//标签内容部分
	var str3=content.substring(idx+endTag.length);//结束标签后面部分
	var resultArr=new Array();
	resultArr[0]=str2;
	resultArr[1]=str1+str3;
	return resultArr;
}

/**
 * 去除掉字符串中所有<>括住的标签，只保留普通字符串
 */
function removeAllHtmlTag(content)
{
	if(content==null) return null;
	return content.replace(/<.*?>/gi, '');
}

var WX_TOOLS_LOADED=true;//用于标识此js文件加载完