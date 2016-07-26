var WX_GUID_SEPERATOR='_guid_';//guid的分隔符

var ISOPERA=(navigator.userAgent.toLowerCase().indexOf("opera")!=-1);//是否是opera浏览器

var isChrome =(navigator.userAgent.indexOf("Chrome") !== -1);//是否是GOOGLE浏览器

var isIE=(navigator.userAgent.toLowerCase().indexOf("msie")!=-1);//IE浏览器

/**
 *只用于EditableListReportType2和EditableDetailReportType2两种报表类型
 *对于EditableListReportType2报表类型，这个对象是一个HashMap，以报表ID为键，值为一ArrayList对象，在ArrayList中存放的是被编辑行的<tr/>对象。
 *对于EditableDetailReportType2报表类型，这个对象是一个HashMap，以报表ID为键，值为true，表示当前报表数据被编辑过。
 */
var WX_UPDATE_ALLDATA;
var WX_showProcessingBar=true;//当前加载操作是否需要显示正在加载的进度条，一般情况下都需要，但有时候不想让用户感觉到是经过服务器端操作，则不显示，比如列拖动操作。
var WX_serverUrl;
var WX_refreshActionParamsMap;//刷新页面时
/**
 *	向指定URL发送异步请求
 *	@param serverUrl:要加载的url
 * @param mSaveReportIds：本次是增、改数据操作时指定本次要保存的报表ID
 */ 
function refreshComponent(serverUrl,mSaveReportIds,refreshActionParamsMap)
{ 
	if(serverUrl==null||serverUrl=='') return;
	var idx=serverUrl.indexOf('&WX_ISSERVERCONFIRM=true');
	if(idx>0)
	{//当前是用户在服务器端的“确认”提示信息中点了“是”或“否”时的加载，这个时候不做任何判断，直接重新刷新即可
		serverUrl=serverUrl.substring(0,idx)+serverUrl.substring(idx+'&WX_ISSERVERCONFIRM=true'.length);
		WX_serverUrl=serverUrl;
		loadPage('ok');
	}else
	{
		WX_refreshActionParamsMap=refreshActionParamsMap;
	  	WX_serverUrl=paramdecode(serverUrl);
	  	WX_serverUrl=replaceUrlParamValue(WX_serverUrl,'WX_ISAJAXLOAD','true');//加上本次是ajax加载的标识
		/*var pageid=getParamValueFromUrl(serverUrl,'PAGEID');
		var refreshComponentGuid=getParamValueFromUrl(serverUrl,'refreshComponentGuid');
		if(refreshComponentGuid==null||refreshComponentGuid=='')
	  	{
	  	 	refreshComponentGuid=pageid;
	  	}else if(refreshComponentGuid.indexOf('[OUTERPAGE]')==0)
	  	{//本次是要加载另一个页面（因为跳转或返回操作），也就是说本次刷新的不是当前页面或其上的部分，而是新的页面（可能是报表页面，也可能是其它页面）
	  		refreshComponentGuid=pageid;
	  	}*/
	  	if(!isHasIgnoreReportsSavingData(WX_serverUrl,mSaveReportIds))
	  	//if(!isHasIgnoreReportsSavingData())
	  	{//如果当前是在做保存动作，或者不是做保存，但用户没有编辑数据，则直接刷新页面
	  		loadPage('ok');
	  	}else
	  	{//当前不是保存动作且用户更新或删除了数据
	  		wx_confirm('是否放弃对报表数据的修改？',null,null,null,loadPage);
	  	}
  	}
}

function loadPage(input)
{
	if(!wx_isOkConfirm(input)) return false;
	var serverUrl=WX_serverUrl;
	/**
  	 * 将下面数据封装到一个对象中，以便ajax回调函数能使用
  	 */
  	var dataObj=new Object();
  	dataObj['serverUrl']=serverUrl;
  	dataObj['refreshActionParamsMap']=WX_refreshActionParamsMap==null?new Object():WX_refreshActionParamsMap;
  	//alert(serverUrl);
  	var pageid=getParamValueFromUrl(serverUrl,'PAGEID');
  	if(pageid=='')
  	{
  		wx_warn('在URL中没有取到PAGEID，无法加载');
  		return;
  	}
  	dataObj['pageid']=pageid;
  	var refreshComponentGuid=getParamValueFromUrl(serverUrl,'refreshComponentGuid');
  	if(refreshComponentGuid==null||refreshComponentGuid=='')
  	{
  	 	refreshComponentGuid=pageid;
  	}else if(refreshComponentGuid.indexOf('[OUTERPAGE]')==0)
  	{//本次是要加载另一个页面（因为跳转或返回操作），也就是说本次刷新的不是当前页面或其上的部分，而是新的页面（可能是报表页面，也可能是其它页面）
  		serverUrl=replaceUrlParamValue(serverUrl,'refreshComponentGuid',null);//此时肯定是加载完整的目标页面，所以去掉里面的refreshComponentGuid参数
  		serverUrl=replaceUrlParamValue(serverUrl,'WX_ISOUTERPAGE','true');//标识一下当前请求是更新整个页面到另一个页面，即整个<page/>替换成其它页面，在服务器端需要用到此标识
  	}
  	dataObj['refreshComponentGuid']=refreshComponentGuid;
  	var slave_reportid=getParamValueFromUrl(serverUrl,'SLAVE_REPORTID');
  	dataObj['slave_reportid']=slave_reportid;
	var tmpArray = serverUrl.split("?");
  	if(tmpArray==null||tmpArray.length<=1)
  	{
    	tmpArray[1]='';	
  	}else if(tmpArray.length>2)
  	{//有多个问号，则只取第一个问号的前面为URL，后面都是参数
  		for(var k=2;k<tmpArray.length;k=k+1)
  		{
  	  		tmpArray[1]=tmpArray[1]+'?'+tmpArray[k];
  		}		
  	}
  	if(WX_showProcessingBar) 
   {
   	displayLoadingMessage(getRealRefreshPageId(pageid,refreshComponentGuid));
   }
	WX_showProcessingBar=true;
	XMLHttpREPORT.sendReq('POST',tmpArray[0],tmpArray[1],callBack,onErrorMethod,dataObj);
	closeAllColFilterResultSpan();//关闭所有列过滤提示窗口
}

var responseStateCode={NONREFRESHPAGE:0,FAILED:-1,SUCCESS:1};//返回的响应状态码

var wx_request_responseResultData=null;//请求时的数据和请求返回的结果数据
var wx_isSuccessResponse;//本次请求是否成功
function callBack(xmlHttpObj,dataObj)
{
	var pageid=dataObj['pageid'];
	var refreshComponentGuid=dataObj['refreshComponentGuid'];
	hideLoadingMessage(getRealRefreshPageId(pageid,refreshComponentGuid));
	WX_showProcessingBar=true;
	var mess=xmlHttpObj.responseText;
	var serverUrl=dataObj['serverUrl'];
	var strArr=parseTagContent(mess,'<RESULTS_INFO-'+pageid+'>','</RESULTS_INFO-'+pageid+'>');
	var jsonResultStr=null;
	if(strArr!=null&&strArr.length==2)
	{
		jsonResultStr=strArr[0];
		mess=strArr[1];
	}
	var callback_jsonResultsObj=getObjectByJsonString(jsonResultStr);//根据返回字符串生成的结果对象
	if(callback_jsonResultsObj==null)
	{
		wx_error('更新页面数据失败');
		return;
	}
	if(callback_jsonResultsObj.confirmessage!=null&&callback_jsonResultsObj.confirmessage!='')
	{//服务器端有确认提示信息
		WX_serverconfirm_key=callback_jsonResultsObj.confirmkey;
		WX_serverconfirm_url=callback_jsonResultsObj.confirmurl;
		wx_confirm(callback_jsonResultsObj.confirmessage,null,null,null,okServerConfirm,cancelServerConfirm);
		return;
	}
	var slave_reportid=null;
	if(refreshComponentGuid.indexOf('[DYNAMIC]')==0)
	{//是动态生成的刷新组件ID
		refreshComponentGuid=callback_jsonResultsObj.dynamicRefreshComponentGuid;
		slave_reportid=callback_jsonResultsObj.dynamicSlaveReportId;
	}else
	{
		slave_reportid=dataObj['slave_reportid'];
	}
	if(slave_reportid!=null&&slave_reportid!='')
	{//当前是刷新从报表
		refreshComponentGuid=getComponentGuidById(pageid,slave_reportid);
	}
	//alert(mess);
	var oldRefreshedComponentGuid=refreshComponentGuid;
	if(oldRefreshedComponentGuid.indexOf('[OUTERPAGE]')==0)
	{//当前加载的页面不是本页面或本页面的某一部分，而是进入新页面（即跳转或返回操作）
		oldRefreshedComponentGuid=oldRefreshedComponentGuid.substring('[OUTERPAGE]'.length);
		refreshComponentGuid=pageid;//肯定是刷新整个页面
	}
	wx_isSuccessResponse=callback_jsonResultsObj.statecode!=responseStateCode.FAILED&&(callback_jsonResultsObj.errormess==null||callback_jsonResultsObj.errormess=='');
  	if(wx_isSuccessResponse===true&&callback_jsonResultsObj.statecode!=responseStateCode.NONREFRESHPAGE)
	{//没有报错且没有被阻止刷新页面
		var ele=document.getElementById('WX_CONTENT_'+oldRefreshedComponentGuid);
 		if(ele==null)
 		{
 			wx_error('更新页面失败');
 			return;
 		}
 		processAllListReportDataRowsBeforeRefreshPage(dataObj.serverUrl,dataObj.refreshActionParamsMap);//本次刷新要保持支持翻页行选中报表所选中的行
 		if(oldRefreshedComponentGuid!=refreshComponentGuid)
 		{//如果当前是跳转到其它页面，即不是刷新本页面或本页面的某一个部分
 			if(ele.outerHTML!=null&&ele.outerHTML!=undefined)
 			{
 				ele.outerHTML=mess;
 			}else
 			{//firefox不支持outerHTML，用下面代码进行替换
 				ele.innerHTML=mess;
 			}
 		}else
 		{
 			var startflag='<div id="WX_CONTENT_'+refreshComponentGuid+'">';
 			var endflag='</div>';
 			//被startflag和endflag括住的部分才是本组件所有显示的内容，这两个标识之外的内容不属于本组件的显示内容，所以上面代码就是把它们去掉
 			var idx=mess.indexOf(startflag);
 			if(idx>=0) 
 			{
 				mess=mess.substring(idx+startflag.length);
 				idx=mess.lastIndexOf(endflag); 
 				if(idx>0) mess=mess.substring(0,idx);
 			}
 			ele.innerHTML=mess; 
 		}
 		if(slave_reportid==null||slave_reportid=='')
		{//当前不是加载从报表
			var pageurlSpanObj=document.getElementById(pageid+'_url_id');
			pageurlSpanObj.setAttribute('value',callback_jsonResultsObj.pageurl);
			if(callback_jsonResultsObj.pageEncodeUrl!=null&&callback_jsonResultsObj.pageEncodeUrl!='')
			{//存在本页面ascii编码后的URL，则存入此<span/>的encodevalue属性中，供跳转后返回使用
				pageurlSpanObj.setAttribute('encodevalue',callback_jsonResultsObj.pageEncodeUrl);
			}
		}
	}
	wx_request_responseResultData=new Object;
	wx_request_responseResultData.requestDataObj=dataObj;
	wx_request_responseResultData.responseDataObj=callback_jsonResultsObj;
	callback_alert();
}

/**
 * 获取本次刷新时真正原始pageid
 */
function getRealRefreshPageId(pageid,refreshComponentGuid)
{
	if(refreshComponentGuid!=null&&refreshComponentGuid.indexOf('[OUTERPAGE]')==0) return refreshComponentGuid.substring('[OUTERPAGE]'.length);
	return pageid;
}

var WX_serverconfirm_key=null;//服务器端确认提示的KEY
var WX_serverconfirm_url=null;//服务器端确认提示时正在加载的URL
/**
 * 在服务器端确认提示时用户点击“确定”时的处理函数
 */
function okServerConfirm(input)
{
	if(!wx_isOkConfirm(input)) 
	{
		cancelServerConfirm();
	}else
	{
		refreshComponent(WX_serverconfirm_url+'&'+WX_serverconfirm_key+'=true&WX_ISSERVERCONFIRM=true');
	}
}

/**
 * 在服务器端确认提示时用户点击“取消”时的处理函数
 */
function cancelServerConfirm()
{
	refreshComponent(WX_serverconfirm_url+'&'+WX_serverconfirm_key+'=false&WX_ISSERVERCONFIRM=true');
}

function callback_alert()
{
	var alertsArr=wx_request_responseResultData.responseDataObj.alert;
	if(alertsArr==null||alertsArr.length==0)
	{
		callback_success();
	}else
	{
		var messageTmp,paramsObjTmp;
		for(var i=0,len=alertsArr.length;i<len;i++)
		{
			messageTmp=alertsArr[i].message;
			paramsObjTmp=alertsArr[i].popupparams;
			if(i==len-1)
			{//提示最后一条信息
				if(WXConfig.prompt_dialog_type=='ymprompt')
				{
					if(paramsObjTmp==null) paramsObjTmp=new Object();
					paramsObjTmp.handler=callback_success;
					wx_alert(messageTmp,paramsObjTmp);
				}else
				{
					wx_alert(messageTmp,paramsObjTmp);
					callback_success();
				}
			}else
			{
				wx_alert(messageTmp,paramsObjTmp);
			}
		}
	}
}

function callback_success()
{
	var successArr=wx_request_responseResultData.responseDataObj.success;
	if(successArr==null||successArr.length==0)
	{
		callback_warn();
	}else 
	{
		var messageTmp,paramsObjTmp;
		for(var i=0,len=successArr.length;i<len;i++)
		{
			messageTmp=successArr[i].message;
			paramsObjTmp=successArr[i].popupparams;
			if(i==len-1)
			{//提示最后一条信息
				if(WXConfig.prompt_dialog_type=='ymprompt')
				{
					if(paramsObjTmp==null) paramsObjTmp=new Object();
					paramsObjTmp.handler=callback_warn;
					wx_success(messageTmp,paramsObjTmp);
				}else
				{
					wx_success(messageTmp,paramsObjTmp);
					callback_warn();
				}
			}else
			{
				wx_success(messageTmp,paramsObjTmp);
			}
		}
	}
}

function callback_warn()
{
	var warnsArr=wx_request_responseResultData.responseDataObj.warn;
	if(warnsArr==null||warnsArr.length==0)
	{
		callback_error();
	}else
	{
		var messageTmp,paramsObjTmp;
		for(var i=0,len=warnsArr.length;i<len;i++)
		{
			messageTmp=warnsArr[i].message;
			paramsObjTmp=warnsArr[i].popupparams;
			if(i==len-1)
			{//提示最后一条信息
				if(WXConfig.prompt_dialog_type=='ymprompt')
				{
					if(paramsObjTmp==null) paramsObjTmp=new Object();
					paramsObjTmp.handler=callback_error;
					wx_warn(messageTmp,paramsObjTmp);
				}else
				{
					wx_warn(messageTmp,paramsObjTmp);
					callback_error();
				}
			}else
			{
				wx_warn(messageTmp,paramsObjTmp);
			}
		}
	}
}

function callback_error()
{
	var errorsArr=wx_request_responseResultData.responseDataObj.error;
	if(errorsArr==null||errorsArr.length==0)
	{
		doPostCallback();//如果没有出错
	}else
	{
		var messageTmp,paramsObjTmp;
		for(var i=0,len=errorsArr.length;i<len;i++)
		{
			messageTmp=errorsArr[i].message;
			paramsObjTmp=errorsArr[i].popupparams;
			if(i==len-1)
			{//提示最后一条信息
				if(WXConfig.prompt_dialog_type=='ymprompt')
				{
					if(paramsObjTmp==null) paramsObjTmp=new Object();
					paramsObjTmp.handler=doPostCallback;
					wx_error(messageTmp,paramsObjTmp);
				}else
				{
					wx_error(messageTmp,paramsObjTmp);
					doPostCallback();
				}
			}else
			{
				wx_error(messageTmp,paramsObjTmp);
			}
		}
	}
}

//响应成功时执行回调函数，并且初始化现场
function doPostCallback()
{
	var callback_jsonResultsObj=wx_request_responseResultData.responseDataObj;
	var updateReportGuids=callback_jsonResultsObj.updateReportGuids;
	if(wx_isSuccessResponse===true)
	{
		processAllListReportDataRowsAfterRefreshPage(updateReportGuids);
	}
	/**
	 *因为成功和失败都有可能要执行onload回调函数，所以这里不做区分，而是在服务器端根据成功还是失败的状态添加相应的onload函数到客户端，在这里直接调用
	 */
	var onloadMethods=callback_jsonResultsObj.onloadMethods;
	if(onloadMethods&&onloadMethods!='')
	{
   	var onloadMethodTmp;
   	for(var i=0;i<onloadMethods.length;i=i+1)
   	{//依次执行每个onload方法
   		onloadMethodTmp=onloadMethods[i];
   		if(!onloadMethodTmp||!onloadMethodTmp.methodname) continue;
   		if(onloadMethodTmp.methodparams)
   		{//如果这个onload方法有参数
   			onloadMethodTmp.methodname(onloadMethodTmp.methodparams);
   		}else
   		{
   			onloadMethodTmp.methodname();
   		}
   	}
	}
	/**
	 * 下面全局变量的数据可能需要在onload方法中使用，所以放在onload方法之后调用。
	 */
	if(wx_isSuccessResponse===true&&updateReportGuids!=null&&updateReportGuids!='')
	{
	 	for(var i=0;i<updateReportGuids.length;i=i+1)
	 	{
	 		reportguidTmp=updateReportGuids[i].value;
	    	if(WX_ALL_SAVEING_DATA&&WX_ALL_SAVEING_DATA[reportguidTmp])
	    	{
	    		delete WX_ALL_SAVEING_DATA[reportguidTmp];
	    	}
	   }
   }
}

/**
 * 在刷新页面前处理支持跨页行选中的报表的选中记录行
 */
function processAllListReportDataRowsBeforeRefreshPage(serverUrl,refreshActionParamsMap)
{
	var refreshOldReportidsMap=getAllRefreshReportIdsByRefreshUrl(serverUrl,false);//存放本次刷新要更新的报表ID
	if(refreshOldReportidsMap==null) return;
	var pageid=getParamValueFromUrl(serverUrl,'PAGEID');
	var metadataObjTmp,reportguidTmp,allSelectedRowsTmp;
 	for(var oldReportidTmp in refreshOldReportidsMap)
 	{//循环所有本次刷新要更新的报表
 		if(oldReportidTmp==null||oldReportidTmp=='') continue;
 		reportguidTmp=getComponentGuidById(pageid,oldReportidTmp);
 		metadataObjTmp=getReportMetadataObj(reportguidTmp);
 		if(metadataObjTmp==null) continue;
 		if(WX_Current_Slave_TrObj!=null&&WX_Current_Slave_TrObj[reportguidTmp]!=null)
    	{
    		WX_Current_Slave_TrObj[reportguidTmp]=WX_Current_Slave_TrObj[reportguidTmp].cloneNode(true);
    		WX_Current_Slave_TrObj[reportguidTmp].setAttribute('wx_not_in_currentpage','true');//当前主报表的记录行已经不显示在页面上了
    	}
 		if(refreshActionParamsMap.keepSelectedRowsAction===true&&metadataObjTmp.metaDataSpanObj.getAttribute('isSelectRowCrossPages')==='true'&&WX_selectedTrObjs!=null)
 		{//当前报表需要保持选中的记录行
	 		allSelectedRowsTmp=WX_selectedTrObjs[reportguidTmp];
	 		if(allSelectedRowsTmp==null) continue;
	 		var deleteTrObjKeysArr=new Array();
	 		for(var keyTmp in allSelectedRowsTmp)
	 		{
	 			if(allSelectedRowsTmp[keyTmp]==null||allSelectedRowsTmp[keyTmp].getAttribute('EDIT_TYPE')=='add')
	 			{//如果是新增的行，则不再保留
	 				deleteTrObjKeysArr[deleteTrObjKeysArr.length]=keyTmp;
	 			}else if(allSelectedRowsTmp[keyTmp].getAttribute('wx_not_in_currentpage')!=='true')
	 			{//如果当前选中的记录行是在本页显示，则存放clone行（因为当前页将会替换掉）
	 			   addAllColValuesToValueProperty(allSelectedRowsTmp[keyTmp]);
	 				allSelectedRowsTmp[keyTmp]=allSelectedRowsTmp[keyTmp].cloneNode(true);
	 				allSelectedRowsTmp[keyTmp].setAttribute('wx_not_in_currentpage','true');//标识已不在当前页面显示了此选中行
	 			}
	 		}
	 		for(var i=0;i<deleteTrObjKeysArr.length;i++)
	 		{
	 			delete allSelectedRowsTmp[deleteTrObjKeysArr[i]];
	 		}
 		}
 		if(refreshActionParamsMap.keepSavingRowsAction===true
 			&&WX_UPDATE_ALLDATA!=null&&WX_UPDATE_ALLDATA[reportguidTmp]!=null
 			&&metadataObjTmp.metaDataSpanObj.getAttribute('isEnableCrossPageEdit')==='true')
  		{//需要跨页编辑功能，且当前是允许跨页编辑的刷新操作
			var updateTrObjsForSaving=WX_UPDATE_ALLDATA[reportguidTmp];
			for(var i=0;i<updateTrObjsForSaving.length;i++)
			{
				trObjTmp=updateTrObjsForSaving[i];
				if(trObjTmp==null||trObjTmp.getAttribute('EDIT_TYPE')=='add')
				{
					updateTrObjsForSaving.splice(i,1);//新添加的列清空
				}else if(trObjTmp.getAttribute('wx_not_in_currentpage')!=='true')
				{//本编辑行显示在此旧页面上
					addAllColValuesToValueProperty(trObjTmp);
					updateTrObjsForSaving[i]=trObjTmp.cloneNode(true);
					updateTrObjsForSaving[i].setAttribute('wx_not_in_currentpage','true');
				}
			}
  		}
   }
}

/**
 * 将<tr/>上各列输入框的数据（如果有的话）全部存到其value属性中，以便翻到下一页时能正常取此行上各列的数据
 */
function addAllColValuesToValueProperty(trObj)
{
	var tdObjsArr=trObj.getElementsByTagName('TD');
	if(tdObjsArr==null||tdObjsArr.length==0) return;
	for(var i=0,len=tdObjsArr.length;i<len;i++)
	{
		tdObjsArr[i].setAttribute('value',wx_getColValue(tdObjsArr[i]));
	}
}

/**
 * 在刷新页面后处理支持跨页行选中的报表的选中记录行
 */
function processAllListReportDataRowsAfterRefreshPage(updatereportGuids)
{
	if(updatereportGuids==null||updatereportGuids=='') return;
	var reportguidTmp,metadataObjTmp,trObjTmp;
	var doc=document;
	var refreshActionParamsMap=wx_request_responseResultData.requestDataObj.refreshActionParamsMap;
 	for(var i=0;i<updatereportGuids.length;i=i+1)
 	{//下面全局变量中的数据需要在onload方法之前删除，因为onload方法还可能向其中写值，不能被删除，比如刷新从报表
 		reportguidTmp=updatereportGuids[i].value;
    	metadataObjTmp=getReportMetadataObj(reportguidTmp);
    	if(WX_UPDATE_ALLDATA!=null&&WX_UPDATE_ALLDATA[reportguidTmp]!=null)
    	{
	    	if(refreshActionParamsMap.keepSavingRowsAction===true&&metadataObjTmp.metaDataSpanObj.getAttribute('isEnableCrossPageEdit')==='true')
	  		{//需要跨页编辑功能，且当前是允许跨页编辑的刷新操作
	    		var updateTrObjsForSaving=WX_UPDATE_ALLDATA[reportguidTmp];
				for(var j=0;j<updateTrObjsForSaving.length;j++)
				{
					trObjTmp=updateTrObjsForSaving[j];
					if(trObjTmp==null||trObjTmp.getAttribute('EDIT_TYPE')=='add')
					{
						updateTrObjsForSaving.splice(j,1);//新添加的列清空
					}else
					{
						var realTrObjTmp=doc.getElementById(trObjTmp.getAttribute('id'));
						if(realTrObjTmp!=null&&realTrObjTmp.getAttribute('global_rowindex')==trObjTmp.getAttribute('global_rowindex'))
						{//当前行修改了数据，则将它替换成要显示的行，这样就能让用户看到修改了的数据，并在此基础上做修改
							trObjTmp.setAttribute('wx_not_in_currentpage',null);//当前列已经要参与显示了
							realTrObjTmp.parentNode.replaceChild(trObjTmp,realTrObjTmp);
						}
					}
				}
	    	}else 
	    	{//当前报表有编辑数据，则清空
	    		delete WX_UPDATE_ALLDATA[reportguidTmp];
	    	}
    	}
    	if(WX_selectedTrObjs==null||WX_selectedTrObjs[reportguidTmp]==null)
    	{
    		selectDefaultSelectedDataRows(metadataObjTmp);//选中在服务器端指定要选中的行
    	}else
    	{
	    	if(refreshActionParamsMap.keepSelectedRowsAction===true&&metadataObjTmp.metaDataSpanObj.getAttribute('isSelectRowCrossPages')==='true')
	 		{//当前报表需要保持选中的记录行
	    		selectDefaultSelectedDataRows(metadataObjTmp);//选中在服务器端指定要选中的行
	    		var selectedTrObjsMap=WX_selectedTrObjs[reportguidTmp];
    			for(var trguidTmp in selectedTrObjsMap)
    			{
    				trObjTmp=selectedTrObjsMap[trguidTmp];
    				if(trObjTmp==null||trObjTmp.getAttribute('wx_not_in_currentpage')!=='true') continue;
    				var realTrObjTmp=doc.getElementById(trObjTmp.getAttribute('id'));
					if(realTrObjTmp!=null&&realTrObjTmp.getAttribute('global_rowindex')==trObjTmp.getAttribute('global_rowindex'))
					{//本选中行在当前页需要显示出来，则重新选中它
						doSelectListReportDataRow(metadataObjTmp,realTrObjTmp,false,true);//重新选中这一行（一定要用这个方法，不能直接selectDataRow()，因为要考虑到树形分组节点选中其父节点的情况）
					}
    			}
	    	}else
	    	{
	    		delete WX_selectedTrObjs[reportguidTmp];
	    		selectDefaultSelectedDataRows(metadataObjTmp);//选中在服务器端指定要选中的行
	    	}
    	}
   }
}

/**
 * 选中在服务器端指定要选中的行
 */
function selectDefaultSelectedDataRows(metadataObj)
{
	if(metadataObj==null) return;
	var reportfamily=metadataObj.metaDataSpanObj.getAttribute('reportfamily');
	if(reportfamily==null||reportfamily.indexOf('list')<0) return;//不是列表报表
	var tableObj=document.getElementById(metadataObj.reportguid+'_data');
	if(tableObj==null) return;
	var rowselecttype=metadataObj.metaDataSpanObj.getAttribute('rowselecttype');
	var trObjTmp;
	if(rowselecttype==WX_ROWSELECT_TYPE.single||rowselecttype==WX_ROWSELECT_TYPE.radiobox||rowselecttype==WX_ROWSELECT_TYPE.single_radiobox)
	{//此报表支持单选
		for(var i=tableObj.rows.length-1;i>=0;i--)
		{
			trObjTmp=tableObj.rows[i];
			if(trObjTmp!=null&&trObjTmp.getAttribute('default_rowselected')==='true')
			{//在服务器端指定了要默认选中
				doSelectListReportDataRow(metadataObj,trObjTmp,true,true);
				break;//选中了最后一行即可
			}
		}
	}else if(rowselecttype==WX_ROWSELECT_TYPE.multiple||rowselecttype==WX_ROWSELECT_TYPE.checkbox||rowselecttype==WX_ROWSELECT_TYPE.multiple_checkbox)
	{//此报表支持多选
		for(var i=0,len=tableObj.rows.length;i<len;i++)
		{
			trObjTmp=tableObj.rows[i];
			if(trObjTmp!=null&&trObjTmp.getAttribute('default_rowselected')==='true')
			{//在服务器端指定了要默认选中
				doSelectListReportDataRow(metadataObj,trObjTmp,true,true);
			}
		}
	}	
}

/**
 *出错处理
 */
function onErrorMethod(xmlHttpObj,dataObj)
{
  	 hideLoadingMessage();
  	 if(true && !window.onbeforeunload)
  	 {//解决report ajax方式查询未返回时，此时点击其他链接时会报错：“系统忙，请稍后再试！"
		 window.onbeforeunload=function(){WXConfig.load_error_message = null;};
	 }
  	 if(WXConfig.load_error_message!=null&&WXConfig.load_error_message!='')
  	 {
  	 	 wx_error(WXConfig.load_error_message);
  	 }
}

/**
 * 判断本次页面刷新是否有还没保存的数据会被忽略掉
 * @param refreshComponentGuid 本次要刷新的组件GUID
 * @param mSaveReportIds 如果本次刷新是做保存操作，这里存放要保存的报表ID集合
 */
function isHasIgnoreReportsSavingData(serverUrl,mSaveReportIds)
{
	if(WX_UPDATE_ALLDATA==null||isEmptyMap(WX_UPDATE_ALLDATA)) return false;//当前页面没有编辑任何报表的数据
	var pageid=getParamValueFromUrl(serverUrl,'PAGEID');
	if(pageid==null||pageid=='') return true;//当前是整个页面跳转到其它非报表页面的页面
	var refreshComponentGuid=getParamValueFromUrl(serverUrl,'refreshComponentGuid');
	var slave_reportid=getParamValueFromUrl(serverUrl,'SLAVE_REPORTID');
	var isRefreshByMaster=getParamValueFromUrl(serverUrl,'WX_ISREFRESH_BY_MASTER');
	if(isRefreshByMaster==='true') return false;//如果当前是被主报表的事件刷新的从报表，则不用判断，因为在加载主报表时已经判断过了
	var refreshReportidsMap=getAllRefreshReportIdsByRefreshUrl(serverUrl,true);//存放本次刷新要更新的所有报表ID
  	if(mSaveReportIds==null) mSaveReportIds=new Object();
	var nonSaveReportGuids=new Array();//存放所有要刷新的报表中没有出现在mSaveReportIds的报表的GUID
	for(var reportidTmp in refreshReportidsMap)
	{
		if(mSaveReportIds[reportidTmp]=='true') continue;
		nonSaveReportGuids[nonSaveReportGuids.length]=getComponentGuidById(pageid,reportidTmp);
	}
	if(nonSaveReportGuids.length==0) return false;//本次受刷新影响到的报表，如果有编辑数据的都会被保存到
	for(var i=0;i<nonSaveReportGuids.length;i++)
	{
		var savingDatas=WX_UPDATE_ALLDATA[nonSaveReportGuids[i]];
		if(savingDatas==null||savingDatas=='') continue;//此报表没有数据要保存
		var metadataObj=getComponentMetadataObj(nonSaveReportGuids[i]);
		if(metadataObj!=null&&metadataObj.metaDataSpanObj.getAttribute('checkdirtydata')=='false') continue;//当前报表不需要校验是否有保存数据
		if(WX_refreshActionParamsMap==null||WX_refreshActionParamsMap.keepSavingRowsAction!==true) return true;//当前的操作是不保存跨页编辑数据的动作
		if(metadataObj==null||metadataObj.metaDataSpanObj.getAttribute('isEnableCrossPageEdit')!=='true') return true;//如果当前报表不是editablelist2/listform或者不允许跨页编辑数据
		try
		{//是editablelist2/listform报表类型，且允许跨页编辑数据
			for(var j=0;j<savingDatas.length;j++)
			{
				if(savingDatas[j].getAttribute('EDIT_TYPE')=='add') return true;//有新添加的记录行，则还是要提醒是否放弃修改
			}
		}catch(e)
		{}
	}
	return false;
}

/**
 * 从刷新URL的参数中获取到本次刷新会刷新哪些报表的ID集合
 * @param isGetAllReferenceReports 是否要获取本次刷新时引起的连锁刷新所影响的报表的ID（常见的就是刷新主报表为列表报表，这里就指定是否要获取其从报表ID，因为其从报表不是同时刷新）
 */
function getAllRefreshReportIdsByRefreshUrl(serverUrl,isGetAllReferenceReports)
{
	var pageid=getParamValueFromUrl(serverUrl,'PAGEID');
	if(pageid==null||pageid=='') return null;//当前是刷新到其它非报表页面的页面
	var refreshComponentGuid=getParamValueFromUrl(serverUrl,'refreshComponentGuid');
	var slave_reportid=getParamValueFromUrl(serverUrl,'SLAVE_REPORTID');
	var refreshComponentGuidsArr=new Array();//所有刷新组件的GUID集合
	var refreshReportidsMap=new Object();//存放本次刷新要更新的所有报表ID
	if(slave_reportid!=null&&slave_reportid!='')
	{//当前是加载从报表
		refreshReportidsMap[slave_reportid]='true';
	}else
	{
		if(refreshComponentGuid.indexOf('[DYNAMIC]')==0)
		{//\指定要刷新的所有组件ID，然后动态根据这些组件ID确定要刷新范围
			var componentidsTmp=refreshComponentGuid.substring('[DYNAMIC]'.length);
			var componentidsArrTmp=componentidsTmp.split(';');
			for(var i=0;i<componentidsArrTmp.length;i++)
			{
				if(componentidsArrTmp[i]==null||componentidsArrTmp[i]=='') continue;
				var metadataObjTmp=getComponentMetadataObj(getComponentGuidById(pageid,componentidsArrTmp[i]));
				if(metadataObjTmp!=null)
				{
					if(metadataObjTmp.refreshComponentGuid==null||metadataObjTmp.refreshComponentGuid=='')
					{
						refreshComponentGuidsArr[refreshComponentGuidsArr.length]=getComponentGuidById(pageid,componentidsArrTmp[i]);
					}else
					{
						refreshComponentGuidsArr[refreshComponentGuidsArr.length]=metadataObjTmp.refreshComponentGuid;
					}
				}
			}
		}else if(refreshComponentGuid==null||refreshComponentGuid==''||refreshComponentGuid.indexOf('[OUTERPAGE]')==0)
	  	{//本次是要加载另一个页面（因为跳转或返回操作），也就是说本次刷新的不是当前页面或其上的部分，而是新的页面（可能是报表页面，也可能是其它页面）
	  	 	refreshComponentGuidsArr[refreshComponentGuidsArr.length]=pageid;
	  	}else
		{
			refreshComponentGuidsArr[refreshComponentGuidsArr.length]=refreshComponentGuid;
		}
		getAllRefreshReportids(pageid,refreshComponentGuidsArr,refreshReportidsMap);
	}
	return refreshReportidsMap;
}

function getAllRefreshReportids(pageid,refreshComponentGuidsArr,refreshReportidsMap,isGetAllReferenceReports)
{
	if(refreshComponentGuidsArr==null||refreshComponentGuidsArr.length==0) return;
	var refreshComponentGuidTmp;
	for(var i=0;i<refreshComponentGuidsArr.length;i++)
	{
		refreshComponentGuidTmp=refreshComponentGuidsArr[i];
		var cmetaDataObj=getComponentMetadataObj(refreshComponentGuidTmp);
		if(cmetaDataObj==null) continue;
		if(cmetaDataObj.componentTypeName=='application.report')
		{//当前组件是报表
			refreshReportidsMap[cmetaDataObj.componentid]='true';
			var dependingChildReportIds=cmetaDataObj.metaDataSpanObj.getAttribute('dependingChildReportIds');
			if(isGetAllReferenceReports===true&&dependingChildReportIds!=null&&dependingChildReportIds!='')
			{//有从报表依赖本报表，则要判断一下从报表是否有没保存的数据
				var dependingChildReportIdsArr=dependingChildReportIds.split(';');
				for(var j=0;j<dependingChildReportIdsArr.length;j++)
				{
					if(dependingChildReportIdsArr[j]==null||dependingChildReportIdsArr[j]=='') continue;
					refreshReportidsMap[dependingChildReportIdsArr[j]]='true';
					getAllInheritDependingChildReportIds(pageid,dependingChildReportIdsArr[j],refreshReportidsMap);
				}
			}
		}else
		{
			var childComponentIds=cmetaDataObj.metaDataSpanObj.getAttribute('childComponentIds');
			if(childComponentIds!=null&&childComponentIds!='')
			{//此组件有子组件，则依次初始化其子组件在URL中的参数，一般容器才会有子组件
				var childidsArr=childComponentIds.split(';');
				for(var j=0,len=childidsArr.length;j<len;j++)
				{
					if(childidsArr[j]==null||childidsArr[j]=='') continue;
					var refreshComponentGuidsArrTmp=new Array();
					refreshComponentGuidsArrTmp[refreshComponentGuidsArrTmp.length]=getComponentGuidById(pageid,childidsArr[j]);
					getAllRefreshReportids(pageid,refreshComponentGuidsArrTmp,refreshReportidsMap,isGetAllReferenceReports);
				}
			}
		}
	}
}

/**
 * 判断当前主报表是否有没保存数据的从报表，这个方法专被主报表为列表报表，在选中主报表记录行刷新从报表时调用
 */
function isHasIgnoreSlaveReportsSavingData(masterReportguid)
{
	if(WX_UPDATE_ALLDATA==null||isEmptyMap(WX_UPDATE_ALLDATA)) return false;//当前页面没有编辑任何报表的数据
	var pageid=getPageIdByComponentGuid(masterReportguid);
	var masterReportid=getComponentIdByGuid(masterReportguid);
	var slaveReportidsMap=new Object();//存放本次刷新要更新的所有报表ID
	getAllInheritDependingChildReportIds(pageid,masterReportid,slaveReportidsMap);
	var slaveReportGuid;
	for(var slaveReportidTmp in slaveReportidsMap)
	{
		slaveReportGuid=getComponentGuidById(pageid,slaveReportidTmp);
		if(WX_UPDATE_ALLDATA[slaveReportGuid]!=null&&WX_UPDATE_ALLDATA[slaveReportGuid]!='')
		{//此报表有保存数据没有保存
			return true;
		}
	}
	return false;
}

/**
 * 获取某个所有依赖某个报表ID的子报表ID，存放在参数reportidsMap中
 */
function getAllInheritDependingChildReportIds(pageid,reportid,reportidsMap)
{
	var reportguid=getComponentGuidById(pageid,reportid);
	var reportMetadataObj=getReportMetadataObj(reportguid);
	if(reportMetadataObj==null) return;
	var dependingChildReportIds=reportMetadataObj.metaDataSpanObj.getAttribute('dependingChildReportIds');
	if(dependingChildReportIds==null||dependingChildReportIds=='') return;
	var dependingChildReportIdsArr=dependingChildReportIds.split(';');
	for(var i=0;i<dependingChildReportIdsArr.length;i++)
	{
		if(dependingChildReportIdsArr[i]==null||dependingChildReportIdsArr[i]=='') continue;
		reportidsMap[dependingChildReportIdsArr[i]]='true';
		getAllInheritDependingChildReportIds(pageid,dependingChildReportIdsArr[i],reportidsMap);//获取子子报表的ID集合
	}
}

/**
 * 查询某个报表数据
 *		
 */
function searchReportData(pageid,reportid,paramsObj)
{
	var reportguid=getComponentGuidById(pageid,reportid);
	var metadataObj=getReportMetadataObj(reportguid);
	var url=getComponentUrl(pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
	if(url==null) return;
	var fontConditionObjs=document.getElementsByName('font_'+reportguid+'_conditions');//取到所有查询条件的<font/>对象
	if(fontConditionObjs!=null)
	{
		var fontObjTmp,convalueTmp,connameTmp,conIteratoridxTmp,validateMethodTmp;
		var datasObj=getAllConditionValues(reportguid);
		for(var i=0,len=fontConditionObjs.length;i<len;i=i+1)
		{
			fontObjTmp=fontConditionObjs[i];
			connameTmp=fontObjTmp.getAttribute('value_name');//<condition/>的name属性
			if(connameTmp==null||connameTmp=='') continue;
			if(!validateConditionBoxValue(metadataObj,datasObj,fontObjTmp,false)) return;//客户端校验没通过
			conIteratoridxTmp=fontObjTmp.getAttribute('iteratorindex');//如果显示多套输入框，这里指定下标
			if(conIteratoridxTmp!=null&&conIteratoridxTmp!=''&&parseInt(conIteratoridxTmp,10)>=0)
			{
				connameTmp=connameTmp+'_'+parseInt(conIteratoridxTmp,10);//加上下标
			}
			convalueTmp=datasObj[connameTmp];//根据父<font/>标签对象，获取到此条件输入框的值
			if(convalueTmp==null||convalueTmp=='') convalueTmp=null;//置成null以便能从URL中删除这个参数
			url=replaceUrlParamValue(url,connameTmp,convalueTmp);
			var innerlogicid=fontObjTmp.getAttribute('innerlogicid');
			if(innerlogicid!=null&&innerlogicid!='')
			{//有逻辑关系选择框，则取出它的值放入URL中传入后台
				url=replaceUrlParamValue(url,innerlogicid,getConditionSelectedValue(innerlogicid,fontObjTmp.getAttribute('innerlogicinputboxtype')));
			}
			var columnid=fontObjTmp.getAttribute('columnid');
			if(columnid!=null&&columnid!='')
			{//有比较字段选择框，则取出选中值放入URL中传入后台
				url=replaceUrlParamValue(url,columnid,getConditionSelectedValue(columnid,fontObjTmp.getAttribute('columninputboxtype')));
			}
			var valueid=fontObjTmp.getAttribute('valueid');
			if(valueid!=null&&valueid!='')
			{//有条件表达式选择框，则取出选中值放入URL中传入后台
				url=replaceUrlParamValue(url,valueid,getConditionSelectedValue(valueid,fontObjTmp.getAttribute('valueinputboxtype')));
			}
		}
	}
	url=removeReportNavigateInfosFromUrl(url,metadataObj,null);//从URL中删除掉本报表及与本报表有查询条件相关联的分页报表的所有翻页导航信息
	url=removeLazyLoadParamsFromUrl(url,metadataObj,true);//删除掉本报表及与其有查询条件关联的报表的延迟加载参数
	var accessmode=metadataObj.metaDataSpanObj.getAttribute('current_accessmode');
	if(accessmode===WX_ACCESSMODE_ADD)
	{//如果当前报表是添加模式，则搜索后不能再是添加模式
		url=replaceUrlParamValue(url,reportid+'_ACCESSMODE',null);
	}
	url=replaceUrlParamValue(url,'SEARCHREPORT_ID',metadataObj.reportid);//加上正在查询这个报表操作的标识，稍后要根据此参数判断当前是在对某个报表做查询操作（查询关联的报表不在这里加，在服务器端）
	if(paramsObj!=null)
	{
		for(var key in paramsObj)
		{
			if(key==null) continue;
			url=replaceUrlParamValue(url,key,paramsObj[key]);
		}
	}
	//alert(url);
	var beforeSearchMethod=metadataObj.metaDataSpanObj.getAttribute('beforeSearchMethod');
	if(beforeSearchMethod!=null&&beforeSearchMethod!='')
	{
		var beforeMethodObj=getObjectByJsonString(beforeSearchMethod);
		url=beforeMethodObj.method(pageid,reportid,url);
		if(url==null||url=='') return;//被回调函数阻了搜索操作
	}
	refreshComponent(url);
}

function removeReportConditionBoxValuesFromUrl(url,metadataObj)
{
	var fontConditionObjs=document.getElementsByName('font_'+metadataObj.reportguid+'_conditions');//取到所有查询条件的<font/>对象
	if(fontConditionObjs==null||fontConditionObjs.length==0) return url;
	var fontObjTmp=null;
	var convalueTmp=null;
	var connameTmp=null;
	var conIteratoridxTmp=null;
	for(var i=0,len=fontConditionObjs.length;i<len;i=i+1)
	{
		fontObjTmp=fontConditionObjs[i];
		connameTmp=fontObjTmp.getAttribute('value_name');//<condition/>的name属性
		if(connameTmp==null||connameTmp=='') continue;
		conIteratoridxTmp=fontObjTmp.getAttribute('iteratorindex');//如果显示多套输入框，这里指定下标
		if(conIteratoridxTmp!=null&&conIteratoridxTmp!=''&&parseInt(conIteratoridxTmp,10)>=0)
		{
			connameTmp=connameTmp+'_'+parseInt(conIteratoridxTmp,10);//加上下标
		}
		url=replaceUrlParamValue(url,connameTmp,null);
		var innerlogicid=fontObjTmp.getAttribute('innerlogicid');
		if(innerlogicid!=null&&innerlogicid!='')
		{//有逻辑关系选择框
			url=replaceUrlParamValue(url,innerlogicid,null);
		}
		var columnid=fontObjTmp.getAttribute('columnid');
		if(columnid!=null&&columnid!='')
		{//有比较字段选择框
			url=replaceUrlParamValue(url,columnid,null);
		}
		var valueid=fontObjTmp.getAttribute('valueid');
		if(valueid!=null&&valueid!='')
		{//有条件表达式选择框
			url=replaceUrlParamValue(url,valueid,null);
		}
	}
	return url;
}

/**
 * 获取查询条件的“逻辑关系选择框”、“条件表达式选择框”、“比较字段选择框”之一的选中值
 * @param selectedboxid 选择框ID
 * @param inputboxtype 选择框类型
 */
function getConditionSelectedValue(selectedboxid,inputboxtype)
{
	if(inputboxtype=='radiobox')
	{//是单选框
		var radioboxObjs=document.getElementsByName(selectedboxid);
		if(radioboxObjs!=null)
		{
			for(var i=0;i<radioboxObjs.length;i++)
			{
				if(radioboxObjs[i].checked) return radioboxObjs[i].value;
			}
		}
	}else
	{//是下拉框
		var selectboxObj=document.getElementById(selectedboxid);
		if(selectboxObj!=null)
		{
			return selectboxObj.options[selectboxObj.options.selectedIndex].value;
		}
	}
	return null;
}

/**
 * 获取所有查询条件的数据
 */
function getAllConditionValues(reportguid)
{
	var fontChilds=document.getElementsByName('font_'+reportguid+'_conditions');
	if(fontChilds==null||fontChilds.length==0) return null;
	var fontObjTmp,conIteratoridxTmp,value_name,valueTmp;
	var dataObj=new Object();
	for(var i=0,len=fontChilds.length;i<len;i++)
	{
		fontObjTmp=fontChilds[i];
		value_name=fontObjTmp.getAttribute('value_name');//<condition/>的name属性
		if(value_name==null||value_name=='') continue;
		conIteratoridxTmp=fontObjTmp.getAttribute('iteratorindex');//如果显示多套输入框，这里指定下标
		if(conIteratoridxTmp!=null&&conIteratoridxTmp!=''&&parseInt(conIteratoridxTmp,10)>=0)
		{
			value_name=value_name+'_'+parseInt(conIteratoridxTmp,10);//加上下标
		}
		valueTmp=wx_getConditionValue(fontObjTmp);//根据父<font/>标签对象，获取到此条件输入框的值
		if(valueTmp==null) valueTmp='';
		dataObj[value_name]=valueTmp;
	}
	return dataObj;
}

/**
 * 设置查询条件的值
 * @param parentEleObj 查询条件所在的父<font/>对象
 * @param newValue 设置值
 */
function wx_setConditionValue(parentEleObj,newValue)
{
	var valuename=parentEleObj.getAttribute('value_name');
	if(valuename==null||valuename=='') return;
	parentEleObj.setAttribute('value',newValue);
	var inputBoxObj=getWXInputBoxChildNode(parentEleObj);//取到<font/>下的框架自动提供的输入框对象
	if(inputBoxObj!=null) setInputBoxValue(inputBoxObj,newValue);
	var reportguid=getReportGuidByParentConditionFontObj(parentEleObj);
	if(reportguid!=null&&reportguid!='')
	{
		var metadataObj=getReportMetadataObj(reportguid);
		if(metadataObj==null) return;
		var onsetvaluemethods=metadataObj.metaDataSpanObj.getAttribute(valuename+'_onsetvaluemethods');
		var onsetvaluemethodObjs=getObjectByJsonString(onsetvaluemethods);
		if(onsetvaluemethodObjs!=null&&onsetvaluemethodObjs.methods!=null)
		{//此列配置有设置列值的回调函数
			var methodObjsArr=onsetvaluemethodObjs.methods;
			if(methodObjsArr.length>0)
			{
				for(var i=0,len=methodObjsArr.length;i<len;i++)
				{
					methodObjsArr[i].method(parentEleObj,newValue,null);
				}
			}
		}
	}
}

/**
 * 获取查询条件的值
 * @param parentEleObj 查询条件所在的父<font/>对象
 */
function wx_getConditionValue(parentEleObj)
{
	if(parentEleObj==null) return null;
	var reportguid=getReportGuidByParentConditionFontObj(parentEleObj);
	if(reportguid==null||reportguid=='') return null;
	var conname=parentEleObj.getAttribute('value_name');
	if(conname==null||conname=='') return null; 
	var inputBoxObj=getWXInputBoxChildNode(parentEleObj);//取到<font/>下的框架自动提供的输入框对象
	var convalue=inputBoxObj==null?parentEleObj.getAttribute('value'):getInputBoxValue(inputBoxObj);
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj!=null)
	{
		var ongetvaluemethods=metadataObj.metaDataSpanObj.getAttribute(conname+'_ongetvaluemethods');
		var ongetvaluemethodObjs=getObjectByJsonString(ongetvaluemethods);
		if(ongetvaluemethodObjs!=null&&ongetvaluemethodObjs.methods!=null)
		{//此列配置有设置列值的回调函数
			var methodObjsArr=ongetvaluemethodObjs.methods;
			if(methodObjsArr.length>0)
			{
				for(var i=0,len=methodObjsArr.length;i<len;i++)
				{
					convalue=methodObjsArr[i].method(parentEleObj,convalue);
				}
			}
		}
	}
	return convalue;
}

function getReportGuidByParentConditionFontObj(parentFontObj)
{
	if(parentFontObj==null) return null;
	var fontid=parentFontObj.getAttribute('id');
	if(fontid==null||fontid.indexOf('font_')!=0||fontid.indexOf('_conditions')<=0) return null;
	return fontid.substring('font_'.length,fontid.lastIndexOf('_conditions'));
}

/**
 *
 * 对报表进行翻页操作
 */
function navigateReportPage(pageid,reportid,newpageno)
{
	var reportMetaDataObj=getReportMetadataObj(getComponentGuidById(pageid,reportid));
	var navigate_reportid=reportMetaDataObj.metaDataSpanObj.getAttribute('navigate_reportid');
	var url=getComponentUrl(pageid,reportMetaDataObj.refreshComponentGuid,reportMetaDataObj.slave_reportid);
	url=replaceUrlParamValue(url,navigate_reportid+"_PAGENO",newpageno);
	var dependingChildReportIds=reportMetaDataObj.metaDataSpanObj.getAttribute('dependingChildReportIds');
	var reportfamily=reportMetaDataObj.metaDataSpanObj.getAttribute('reportfamily');
	if(dependingChildReportIds!=null&&dependingChildReportIds!=''&&(reportfamily==null||reportfamily.indexOf('list')<0))
	{//如果当前报表是被其它从报表依赖的细览报表，则清除掉子报表翻页导航信息，包括间接依赖此报表的从报表（因为此时的从报表数据会重新加载数据）
		var childIdsArr=dependingChildReportIds.split(';');
		var childIdTmp;
		for(var i=0,len=childIdsArr.length;i<len;i=i+1)
		{
			childIdTmp=childIdsArr[i];
			if(childIdTmp==null||childIdTmp=='') continue;
			var childMetadataObj=getReportMetadataObj(getComponentGuidById(pageid,childIdTmp));
			if(childMetadataObj==null) continue;
			url=removeReportNavigateInfosFromUrl(url,childMetadataObj,null);
		}
	}
	var flag=reportfamily==null||reportfamily.indexOf('list')>=0;
	refreshComponent(url,null,{keepSelectedRowsAction:flag,keepSavingRowsAction:flag});//如果是翻页细览报表，则不保存选中的行，如果是列表报表，则保存选中的行
}

/**
 * 从URL中删除掉此报表相关的所有翻页导航信息，包括自己的翻页导航信息和存在查询条件相关联的分页报表翻页导航信息
 * @param url
 * @param metadataObj 本报表的元数据对象
 * @param removetype 删除类型：1：只删除自己的翻页导航ID；2：只删除查询条件关联报表的翻页导航ID；其它值则删除所有翻页导航ID
 */
function removeReportNavigateInfosFromUrl(url,metadataObj,removetype)
{
	if(removetype!=2)
	{//不是只删除查询条件关联报表的翻页导航ID
		var navigate_reportid=metadataObj.metaDataSpanObj.getAttribute('navigate_reportid');
		if(navigate_reportid!=null&&navigate_reportid!='')
		{//本报表本身就有翻页导航ID
			url=replaceUrlParamValue(url,navigate_reportid+"_PAGENO",null);
      	url=replaceUrlParamValue(url,navigate_reportid+"_PAGECOUNT",null);
      	url=replaceUrlParamValue(url,navigate_reportid+"_RECORDCOUNT",null);
		}
	}
	if(removetype!=1)
	{//不是只删除自己的翻页导航ID
		var relateConditionReportNavigateIds=metadataObj.metaDataSpanObj.getAttribute('relateConditionReportNavigateIds');
		if(relateConditionReportNavigateIds!=null&&relateConditionReportNavigateIds!='')
		{//有查询条件关联的分页显示报表
			var navigateIdsArr=relateConditionReportNavigateIds.split(';');
			var navigateIdTmp;
			for(var i=0;i<navigateIdsArr.length;i=i+1)
			{
				navigateIdTmp=navigateIdsArr[i];
				if(navigateIdTmp==null||navigateIdTmp=='') continue;
				url=replaceUrlParamValue(url,navigateIdTmp+"_PAGENO",null);
      		url=replaceUrlParamValue(url,navigateIdTmp+"_PAGECOUNT",null);
      		url=replaceUrlParamValue(url,navigateIdTmp+"_RECORDCOUNT",null);
			}
		}
		var dependingChildReportIds=metadataObj.metaDataSpanObj.getAttribute('dependingChildReportIds');
		var reportfamily=metadataObj.metaDataSpanObj.getAttribute('reportfamily');
		if(dependingChildReportIds!=null&&dependingChildReportIds!=''&&(reportfamily==null||reportfamily.indexOf('list')<0))
		{//如果当前报表是被其它从报表依赖的细览报表，则清除掉子报表翻页导航信息，包括间接依赖此报表的从报表
			var childIdsArr=dependingChildReportIds.split(';');
			var childIdTmp;
			for(var i=0;i<childIdsArr.length;i=i+1)
			{
				childIdTmp=childIdsArr[i];
				if(childIdTmp==null||childIdTmp=='') continue;
				var childMetadataObj=getReportMetadataObj(getComponentGuidById(metadataObj.pageid,childIdTmp));
				if(childMetadataObj==null) continue;
				url=removeReportNavigateInfosFromUrl(url,childMetadataObj,removetype);
			}
		}
	}
	return url;
}
var WX_SCROLL_DELAYTIME=50;//以前是200
/**
 * 为组件显示图片滚动条
 */
function showComponentScroll(componentGuid,scrollheight,type)
{//在显示图片滚动条时，需要先等待一下，等其它内容都完全显示好后再开始显示滚动条，否则后面取scrollheight时会有问题导致不能正常显示滚动条
	if(scrollheight!=null&&isWXNumber(scrollheight))
	{//如果只指定了高度值，没有指定单位，则加上像素作为单位，否则会显示不出滚动条
		scrollheight=scrollheight+'px';
	}
	if(typeof(fleXenv)=='undefined')
	{
		wx_error('框架没有为此页面导入/webresources/scroll/scroll.js文件，请在＜page/＞的js属性中显式导入');
		return false;
	}
	if(type==11)
	{//为数据自动列表报表显示垂直滚动条
		setTimeout(function(){showComponentVerticalScroll('vscroll_'+componentGuid,scrollheight);},WX_SCROLL_DELAYTIME);
		//showComponentVerticalScroll('vscroll_'+componentGuid,scrollheight);
	}else if(type==12)
	{//为数据自动列表报表显示横向滚动条
		setTimeout(function(){showComponentHorizontalScroll('hscroll_'+componentGuid);},WX_SCROLL_DELAYTIME);
		//showComponentHorizontalScroll('hscroll_'+componentGuid);
	}else if(type==13)
	{//为数据自动列表报表显示纵横滚动条（现在不采用这种方式了，而是改为23方式）
		//setTimeout(function(){showComponentVerticalScroll('vscroll_'+componentGuid,scrollheight);showComponentHorizontalScroll('hscroll_'+componentGuid);},WX_SCROLL_DELAYTIME);
		showComponentVerticalScroll('vscroll_'+componentGuid,scrollheight);
		showComponentHorizontalScroll('hscroll_'+componentGuid);
	}else if(type==21)
	{//为其它组件显示垂直滚动条
		setTimeout(function(){showComponentVerticalScroll('scroll_'+componentGuid,scrollheight);},WX_SCROLL_DELAYTIME);
		//showComponentVerticalScroll('scroll_'+componentGuid,scrollheight);
	}else if(type==22)
	{//为其它组件显示横向滚动条
		setTimeout(function(){showComponentHorizontalScroll('scroll_'+componentGuid);},WX_SCROLL_DELAYTIME);
		//showComponentHorizontalScroll('scroll_'+componentGuid);
	}else if(type==23)
	{//为其它组件显示纵横滚动条
		setTimeout(function(){showComponentAllScroll('scroll_'+componentGuid,scrollheight);},WX_SCROLL_DELAYTIME);
		//showComponentAllScroll('scroll_'+componentGuid,scrollheight);
	}
}

/**
 * 为组件显示垂直滚动条
 */
function showComponentVerticalScroll(scrollid,scrollheight)
{
	if(!scrollheight||parseInt(scrollheight)<=0) return false;
	//alert('1');
	var divobj=document.getElementById(scrollid);
	if(!divobj) return false;
	//setTimeout(function(){alert('1');},300);
	if(divobj.scrollHeight>parseInt(scrollheight))
	{
		divobj.style.height=scrollheight;
		fleXenv.fleXcrollMain(divobj);//显示图片滚动条
		/**
	 	 * 将横向滚动条隐藏
	 	 */
		document.getElementById(scrollid+'_hscrollerbase').className='hscrollerbase_hidden';
		//document.getElementById(scrollid+'_mcontentwrapper').style.border='0';
		//document.getElementById(scrollid+'_contentwrapper').style.border='0';
		divobj.fleXcroll.updateScrollBars();
	}else
	{
		divobj.style.height=divobj.scrollHeight+'px';
	}
	//fleXenv.updateScrollBars();
}

/**
 * 为列表报表显示横向滚动条
 */
function showComponentHorizontalScroll(scrollid)
{
	var divobj=document.getElementById(scrollid);
	if(!divobj) return false;
	divobj.style.height=(divobj.scrollHeight+15)+'px';//加上滚动条的高度，以免内容被滚动条遮住
	fleXenv.fleXcrollMain(divobj);
	document.getElementById(scrollid+'_vscrollerbase').className='vscrollerbase_hidden';
	//fleXenv.updateScrollBars();
	divobj.fleXcroll.updateScrollBars();
}

/**
 * 为组件显示纵横滚动条
 */
function showComponentAllScroll(scrollid,scrollheight)
{
	if(!scrollheight||parseInt(scrollheight)<=0) return false;
	var divobj=document.getElementById(scrollid);
	if(!divobj) return false;
	if(divobj.scrollHeight>parseInt(scrollheight))
	{
		divobj.style.height=scrollheight;
	}else
	{
		divobj.style.height=(divobj.scrollHeight+15)+'px';//加上滚动条的高度，以免内容被滚动条遮住
	}
	fleXenv.fleXcrollMain(divobj);
	divobj.fleXcroll.updateScrollBars(); 
}

/*******************************选中数据自动列表报表的行**********************************/

/**
 * 初始化行选择
 */
function initRowSelect()
{
	if(WX_selectedTrObjs==null) WX_selectedTrObjs=new Object();
}

var WX_selectedTrObj_temp;//被选中的<tr/>对象
var WX_shouldInvokeOnloadMethod_temp;//是否需要执行行选中的回调函数（包括配置的或框架自动生成的，比如刷新从报表等），默认为不执行，传入true则执行

/**
 * 执行由客户端选中记录行接口方法选中行对象操作
 */
function doSelectReportDataRowImpl(input)//(trObj,shouldInvokeOnloadMethod)
{
	if(!wx_isOkConfirm(input)) return;
	var trObj=WX_selectedTrObj_temp;
	var shouldInvokeOnloadMethod=WX_shouldInvokeOnloadMethod_temp;
	WX_selectedTrObj_temp=null;
	WX_shouldInvokeOnloadMethod_temp=null;
	if(trObj==null||trObj.getAttribute('disabled_rowselected')=='true') return;
	var trid=trObj.getAttribute("id");
	if(trid==null||trid=='') return;
  	var reportguid=trid.substring(0,trid.lastIndexOf('_tr_'));//报表的guid
  	if(reportguid=='') return;
  	var metadataObj=getReportMetadataObj(reportguid);
  	if(metadataObj==null) return;
  	doSelectListReportDataRow(metadataObj,trObj,shouldInvokeOnloadMethod,true);
}

/**
 * 选中某记录行对象
 */
function doSelectListReportDataRow(metadataObj,trObj,shouldInvokeOnloadMethod,ignoreCheckHasSavingdata)
{
	if(metadataObj==null||trObj.getAttribute('disabled_rowselected')=='true') return;
  	var rowselecttype=metadataObj.metaDataSpanObj.getAttribute('rowselecttype');
  	if(rowselecttype==null||WX_ROWSELECT_TYPE.alltypes[rowselecttype.toLowerCase()]!==true) return;
	if(rowselecttype==WX_ROWSELECT_TYPE.checkbox||rowselecttype==WX_ROWSELECT_TYPE.radiobox||
		rowselecttype==WX_ROWSELECT_TYPE.multiple_checkbox||rowselecttype==WX_ROWSELECT_TYPE.single_radiobox)
	{//通过单选框或复选框进行行选中
		var selectBoxObjTmp=null;
		if(rowselecttype==WX_ROWSELECT_TYPE.checkbox||rowselecttype==WX_ROWSELECT_TYPE.multiple_checkbox)
		{
			selectBoxObjTmp=getSelectCheckBoxObj(trObj);
		}else
		{
			selectBoxObjTmp=getSelectRadioBoxObj(trObj);
		}
		if(selectBoxObjTmp==null) return;
		selectBoxObjTmp.checked=true;
		doSelectedDataRowChkRadio(selectBoxObjTmp,shouldInvokeOnloadMethod==true?'true':'false',ignoreCheckHasSavingdata);
	}else
	{//普通行选中
		if(!isListReportDataTrObj(trObj)) return;//不是可选中的行
		initRowSelect();
		var deselectedTrObjsArr=null;
		if(rowselecttype==WX_ROWSELECT_TYPE.single)
		{//如果只能单行选中
			deselectedTrObjsArr=deselectAllDataRow(metadataObj);
		}
		selectDataRow(metadataObj,trObj);
		if(shouldInvokeOnloadMethod==true)
		{
			var selectedTrObjArr=new Array();
			selectedTrObjArr[0]=trObj;
			deselectedTrObjsArr=getRealDeselectedTrObjs(selectedTrObjArr,deselectedTrObjsArr);
			invokeRowSelectedMethods(metadataObj,selectedTrObjArr,deselectedTrObjsArr);
		}
	}
}

/**
 * 执行由客户端取消选中记录行接口方法选中行对象操作
 */
function doDeselectReportDataRowImpl(input)
{
	if(!wx_isOkConfirm(input)) return;
	var trObj=WX_selectedTrObj_temp;
	var shouldInvokeOnloadMethod=WX_shouldInvokeOnloadMethod_temp;
	WX_selectedTrObj_temp=null;
	WX_shouldInvokeOnloadMethod_temp=null;
	if(trObj==null||trObj.getAttribute('disabled_rowselected')=='true') return;
	var trid=trObj.getAttribute("id");
	if(trid==null||trid=='') return;
  	var reportguid=trid.substring(0,trid.lastIndexOf('_tr_'));//报表的guid
  	var metadataObj=getReportMetadataObj(reportguid);
  	if(metadataObj==null) return;
  	if(!isSelectedRowImpl(reportguid,trObj)) return;
  	var rowselecttype=getRowSelectType(reportguid);
  	if(rowselecttype==null||WX_ROWSELECT_TYPE.alltypes[rowselecttype.toLowerCase()]!==true) return;
	if(rowselecttype==WX_ROWSELECT_TYPE.checkbox||rowselecttype==WX_ROWSELECT_TYPE.multiple_checkbox)
	{
		var selectBoxObjTmp=getSelectCheckBoxObj(trObj);
		if(selectBoxObjTmp==null) return;
		selectBoxObjTmp.checked=false;//将此复选框置为不选中状态
		doSelectedDataRowChkRadio(selectBoxObjTmp,shouldInvokeOnloadMethod==true?'true':'false',true);
	}else
	{
		deselectDataRow(metadataObj,trObj);
		if(rowselecttype==WX_ROWSELECT_TYPE.radiobox||rowselecttype==WX_ROWSELECT_TYPE.single_radiobox)
		{
			var selectBoxObjTmp=getSelectRadioBoxObj(trObj);
			if(selectBoxObjTmp!=null) selectBoxObjTmp.checked=false;
		}
		if(shouldInvokeOnloadMethod==true)
		{
			var deselectedTrObjsArr=new Array();
			deselectedTrObjsArr[0]=trObj;
			invokeRowSelectedMethods(metadataObj,null,deselectedTrObjsArr);
		}
	}
}


var WX_selected_selecttype_temp;
/**
 * 选中行对象
 */
function doSelectDataRowEvent(evt)
{
	var event=evt || window.event;
	var element=event.srcElement || event.target;
	var trObj=getSelectedTrParent(element);//获取其父级节点-表格行
	if(trObj==null||trObj.getAttribute('disabled_rowselected')=='true') return;
  	var trid=trObj.getAttribute("id");
  	var reportguid=trid.substring(0,trid.lastIndexOf('_tr_'));//报表的guid
  	var metadataObj=getReportMetadataObj(reportguid);
  	if(metadataObj==null) return;
  	var selecttype=metadataObj.metaDataSpanObj.getAttribute('rowselecttype');
	if(selecttype==WX_ROWSELECT_TYPE.multiple_checkbox||selecttype==WX_ROWSELECT_TYPE.single_radiobox)
  	{
  		if(element.getAttribute('name')==reportguid+'_rowselectbox_col') return;//本次是点击在行选择的单选框或复选框上，则不在这里进行行选中，因为此选择框的onclick事件自己会选中
  		var selectBoxObjTmp=null;
		if(selecttype==WX_ROWSELECT_TYPE.multiple_checkbox)
		{
			selectBoxObjTmp=getSelectCheckBoxObj(trObj);
			if(selectBoxObjTmp==null) return;
			if(selectBoxObjTmp.checked==true) selectBoxObjTmp.checked=false;
  			else selectBoxObjTmp.checked=true;
		}else
		{
			selectBoxObjTmp=getSelectRadioBoxObj(trObj);
			if(selectBoxObjTmp==null||selectBoxObjTmp.checked==true) return;//如果是单选框，且当前是选中，则再点击仍然是选中，所以不用做任何处理
			selectBoxObjTmp.checked=true;
		}
  		doSelectedDataRowChkRadio(selectBoxObjTmp,true,false);
  	}else
  	{
	  	WX_selectedTrObj_temp=trObj;
	  	if(isHasIgnoreSlaveReportsSavingData(reportguid))
	  	{
	  		wx_confirm('本操作可能会丢失对从报表数据的修改，是否继续？',null,null,null,doSelectDataRowEventImpl);
	  	}else
	  	{
	  		doSelectDataRowEventImpl('ok');
	  	}
  	}
}

/**
 * 选中行对象
 */
function doSelectDataRowEventImpl(input)
{
	if(!wx_isOkConfirm(input)) return;
	var selectedTrObj=WX_selectedTrObj_temp;
	WX_selectedTrObj_temp=null;
	if(selectedTrObj.getAttribute('disabled_rowselected')=='true') return;
  	var trid=selectedTrObj.getAttribute("id");
  	var reportguid=trid.substring(0,trid.lastIndexOf('_tr_'));//报表的guid
  	var metadataObj=getReportMetadataObj(reportguid);
  	if(metadataObj==null) return;
  	var rowselecttype=metadataObj.metaDataSpanObj.getAttribute('rowselecttype');
  	initRowSelect();
  	var selectedTrObjArr=new Array();//本次选中的行对象数组，稍后要传给行选中回调函数
  	var deselectedTrObjsArr=new Array();//本次操作被取消选中的所有行对象数组，稍后要传给行选中回调函数
  	if(rowselecttype==WX_ROWSELECT_TYPE.single)
  	{//如果只能单选则取消所有选中行，只选中当前行
		deselectedTrObjsArr=deselectAllDataRow(metadataObj);//将所有已选中的行取消选中
		selectDataRow(metadataObj,selectedTrObj);
		selectedTrObjArr[selectedTrObjArr.length]=selectedTrObj;
		deselectedTrObjsArr=getRealDeselectedTrObjs(selectedTrObjArr,deselectedTrObjsArr);
  }else
  {//支持多选
	  	var allSelecteTrObjs=WX_selectedTrObjs[reportguid];
	  	var trguid=getSelectedTrObjGuid(metadataObj,selectedTrObj);
	  	if(trguid==null) return;
	  	if(allSelecteTrObjs!=null&&allSelecteTrObjs[trguid]!=null)
	  	{//当前行已被选中，则取消选中
		 	deselectDataRow(metadataObj,selectedTrObj);
		 	deselectedTrObjsArr[deselectedTrObjsArr.length]=selectedTrObj;
	  	}else
	  	{//还没选中，则选中它
	  		selectDataRow(metadataObj,selectedTrObj);
		 	selectedTrObjArr[selectedTrObjArr.length]=selectedTrObj;
	  	}
  }
  invokeRowSelectedMethods(metadataObj,selectedTrObjArr,deselectedTrObjsArr);//调用行选中/取消选中的回调函数
}

var WX_selected_selectBoxObj_temp;

/**
 * 点击单选框/复选框选中记录行
 * @param selectBoxObj 单选框/复选框对象
 *	@param shouldInvokeOnloadMethod 是否需要执行行选中回调函数，只有显式指定为'false'时，才不执行，其它情况都执行。
 * @param ignoreCheckHasSavingdata是否需要忽略检查从报表是否有没保存的数据
 */
function doSelectedDataRowChkRadio(selectBoxObj,shouldInvokeOnloadMethod,ignoreCheckHasSavingdata)
{
	var selecttype=selectBoxObj.type;
	var isChecked=selectBoxObj.checked;//取到点击后此行选择框的状态是选中还是不选中（注意：这里取的不是点击前的状态）
	//alert(selecttype);
	if(selecttype!='radio'&&selecttype!='RADIO'&&selecttype!='checkbox'&&selecttype!='CHECKBOX') return;
	var boxname=selectBoxObj.getAttribute('name');
	var idx=boxname.lastIndexOf('_rowselectbox_col');
	if(idx<=0) return;
	var reportguid=boxname.substring(0,idx);
	if(reportguid==null||reportguid=='') return;
	WX_selected_selectBoxObj_temp=selectBoxObj;
	WX_shouldInvokeOnloadMethod_temp=shouldInvokeOnloadMethod;
	if(ignoreCheckHasSavingdata!==true&&isHasIgnoreSlaveReportsSavingData(reportguid))
  	{
  		wx_confirm('本操作可能会丢失对从报表数据的修改，是否继续？',null,null,null,doSelectedDataRowChkRadioImpl,cancelSelectDeselectChkRadio);
  	}else
  	{
  		doSelectedDataRowChkRadioImpl('ok');
  	}
}

/**
 * 如果因为不放弃从报表数据的更新而不进行本次的选中或不选中操作，则调用如下方法恢复当前单选框/复选框的状态
 */
function cancelSelectDeselectChkRadio()
{
	if(WX_selected_selectBoxObj_temp==null) return;
	if(WX_selected_selectBoxObj_temp.checked)
	{//点击后变成选中，说明之前是非选中的
		WX_selected_selectBoxObj_temp.checked=false;
	}else
	{
		WX_selected_selectBoxObj_temp.checked=true;
	}
	WX_selected_selectBoxObj_temp=null;
}


/**
 * 点击单选框/复选框选中记录行
 */
function doSelectedDataRowChkRadioImpl(input)
{
	if(!wx_isOkConfirm(input)) 
	{
		wx_callCancelEvent();
		return;
	}
	var selectBoxObj=WX_selected_selectBoxObj_temp;
	var shouldInvokeOnloadMethod=WX_shouldInvokeOnloadMethod_temp;
	WX_selected_selectBoxObj_temp=null;
	WX_shouldInvokeOnloadMethod_temp=null;
	var selecttype=selectBoxObj.type;
	var isChecked=selectBoxObj.checked;//取到点击后此行选择框的状态是选中还是不选中（注意：这里取的不是点击前的状态）
	//alert(selecttype);
	initRowSelect();
	var selectedTrObjArr=new Array();//本次选中的行对象数组，稍后要传给行选中回调函数
	var deselectedTrObjsArr=new Array();//本次被取消选中的行对象数组
	var boxname=selectBoxObj.getAttribute('name');
	var idx=boxname.lastIndexOf('_rowselectbox_col');
	if(idx<=0) return;
	var reportguid=boxname.substring(0,idx);
	if(reportguid==null||reportguid=='') return;
	var metadataObj=getReportMetadataObj(reportguid);
  	if(metadataObj==null) return;
	var tridPrex;//当前报表数据行<tr/>的id前缀
	var parentTridSuffix;//如果当前行对象是树形节点，存放其父分组所在行的后缀
	var rowgroup=selectBoxObj.getAttribute('rowgroup');
	if(rowgroup=='true')
	{//当前点击是分组节点上的复选框
		/**
		 * 取到所在的行分组<tr/>对象
		 */
		var trGroupObj=getTreeGroupRowObj(reportguid,selectBoxObj);
		if(trGroupObj==null) return;
		selectChildDataRows(metadataObj,trGroupObj,selectedTrObjArr,deselectedTrObjsArr,isChecked);
		var trGroupid=trGroupObj.getAttribute('id');
		var idx=trGroupid.lastIndexOf('trgroup_');
		tridPrex=trGroupid.substring(0,idx);//取到<tr/>的id前缀
		parentTridSuffix=trGroupObj.getAttribute('parenttridsuffix');
	}else
	{//点击的是普通数据行上的复选框/单选框
		var trObj=getSelectedTrParent(selectBoxObj);//获取其父级节点-表格行
  		if(trObj==null||trObj.getAttribute('disabled_rowselected')=='true') return;
  		var trid=trObj.getAttribute("id");
		if(selecttype=='radio'||selecttype=='RADIO')
		{//单选
			deselectedTrObjsArr=deselectAllDataRow(metadataObj);//将所有已选中的行取消选中
			selectDataRow(metadataObj,trObj);
			selectedTrObjArr[selectedTrObjArr.length]=trObj;
		}else
		{//复选
  			if(isChecked)
  			{//如果本次是选中记录行
  				selectDataRow(metadataObj,trObj);
  				selectedTrObjArr[selectedTrObjArr.length]=trObj;
  			}else
  			{
  				deselectDataRow(metadataObj,trObj);//取消此行的选中
  				deselectedTrObjsArr[deselectedTrObjsArr.length]=trObj;
  			}
  			var idx=trid.lastIndexOf('tr_');
			tridPrex=trid.substring(0,idx);
			parentTridSuffix=trObj.getAttribute('parenttridsuffix');
		}
	}
	if(selecttype=='checkbox'||selecttype=='CHECKBOX')
	{//复选框
		/**
  	 	 * 取到标题栏的选中/取消选中所有复选框的父复选框对象
  	 	 * 记录中的单选框/复选框都有一个name属性，值为reportguid_rowselectbox_col
  	 	 * 标题栏中取消/选中所有记录行的复选框的name属性为reportguid_rowselectbox
  	 	 */
  		var boxname=selectBoxObj.getAttribute('name');
  		var idx=boxname.lastIndexOf('_col');
  		if(idx<=0) return;
  		var rootboxname=boxname.substring(0,idx);
  		var rootSelectBoxObjArr=document.getElementsByName(rootboxname);
  		if(rootSelectBoxObjArr&&rootSelectBoxObjArr.length>0)
  		{
  			var rootSelectBoxObj=rootSelectBoxObjArr[0];
  			if(rootSelectBoxObj!=null)
	  		{//在标题行存在复选框对象，则设置它的选中与否的状态
	  			var allSelecteTrObjs=WX_selectedTrObjs[reportguid];
	  			if(allSelecteTrObjs==null||isEmptyMap(allSelecteTrObjs))
				{//当前报表已经没有选中的行
					rootSelectBoxObj.checked=false;//如果标题行有复选框，则设置为非选中状态
				}else
				{
					rootSelectBoxObj.checked=true;
				}
	  		}
  		}
  		if(parentTridSuffix!=null&&parentTridSuffix!='')
  		{//如果当前点击的数据行是在树形分组报表中，则设置其所有父节点的状态
  			selectParentDataRows(tridPrex,parentTridSuffix,isChecked);
  		}
	}
	//alert(selectedTrObjArr.length);
	if(!shouldInvokeOnloadMethod||shouldInvokeOnloadMethod!='false')
  	{//本次操作选中了行，则执行行选中回调函数
  		deselectedTrObjsArr=getRealDeselectedTrObjs(selectedTrObjArr,deselectedTrObjsArr);
  		invokeRowSelectedMethods(metadataObj,selectedTrObjArr,deselectedTrObjsArr);
  	}
}

/**
 * 根据树形分组报表树枝节点上的子元素获取到其树枝节点所在行对象
 * @param childObj 树枝节点上的子元素对象
 */
function getTreeGroupRowObj(reportguid,childObj)
{
	var trGroupObj=childObj.parentNode;
	while(trGroupObj!=null)
	{
		if(trGroupObj.tagName=='TR')
		{
			var trgroupid=trGroupObj.getAttribute('id');
			if(trgroupid!=null&&trgroupid.indexOf(reportguid)>=0&&trgroupid.indexOf('_trgroup_')>0)
			{
				break;
			}
		}
		trGroupObj=trGroupObj.parentNode;
	}
	return trGroupObj;
}

/**
 * 选中某个分组列下面所有子数据行和子分组行对象
 */
function selectChildDataRows(metadataObj,trGroupObj,selectedTrObjArr,deselectedTrObjsArr,isChecked)
{
	var trGroupid=trGroupObj.getAttribute('id');
	//alert(trGroupid);
	var idx=trGroupid.lastIndexOf('trgroup_');
	var tridPrex=trGroupid.substring(0,idx);
	var childDataidSuffixes=trGroupObj.getAttribute('childdataidsuffixes');//所有子数据行的id后缀
	//alert(childDataidSuffixes);
	var doc=document;
	if(childDataidSuffixes&&childDataidSuffixes!='')
	{
		var dataidsArr=childDataidSuffixes.split(';');
		//alert(dataidsArr.length);
		for(var i=0,len=dataidsArr.length;i<len;i=i+1)
		{
			var idSuffixTmp=dataidsArr[i];
			if(idSuffixTmp==null||idSuffixTmp=='') continue;
			var trChildTmp=doc.getElementById(tridPrex+idSuffixTmp);//取到子数据行对象
			//alert(tridPrex+idSuffixTmp);
			if(trChildTmp==null||trChildTmp.getAttribute('disabled_rowselected')=='true') continue;
			var chkObjTmp=getSelectCheckBoxObj(trChildTmp);
			var mychecked=chkObjTmp.checked;//本记录之前的选中状态
			//alert(chkObjTmp);
			if(isChecked)
			{
				chkObjTmp.checked=true;
				selectDataRow(metadataObj,trChildTmp);
				if(!mychecked) selectedTrObjArr[selectedTrObjArr.length]=trChildTmp;//只有当本记录之前是非选中状态，才将它放入selectedTrObjsArr以便回调函数能取到
			}else
			{
				chkObjTmp.checked=false;
				deselectDataRow(metadataObj,trChildTmp);//取消此行的选中
				if(mychecked) deselectedTrObjsArr[deselectedTrObjsArr.length]=trChildTmp;//只有当本记录之前是选中状态，才将它放入deselectedTrObjsArr以便回调函数能取到
			}
		}
	}
	var childGroupidSuffixes=trGroupObj.getAttribute('childgroupidsuffixes');//所有子分组行的id后缀
	if(childGroupidSuffixes!=null&&childGroupidSuffixes!='')
	{
		var groupidsArr=childGroupidSuffixes.split(';');
		for(var i=0;i<groupidsArr.length;i=i+1)
		{
			var idSuffixTmp=groupidsArr[i];
			if(idSuffixTmp==null||idSuffixTmp=='') continue;
			var trChildTmp=doc.getElementById(tridPrex+idSuffixTmp);//取到子分组行对象
			if(trChildTmp==null) continue;
			var chkObjTmp=getSelectCheckBoxObj(trChildTmp);
			if(isChecked)
			{
				chkObjTmp.checked=true;
			}else
			{
				chkObjTmp.checked=false;
			}
		}
	}
	//alert(selectedTrObjArr.length);
}

/**
 * 设置父分组节点上的行选中复选框的状态
 *	@param tridPrex 所有数据行所在<tr/>的id前缀
 * @param parentTridSuffix 父分组节点行对应<tr/>的id后缀
 *	@param isChecked 子节点的选中状态
 */
function selectParentDataRows(tridPrex,parentTridSuffix,isChecked)
{
	if(parentTridSuffix==null||parentTridSuffix=='') return;
	var doc=document;
	var parentTrObj=doc.getElementById(tridPrex+parentTridSuffix);
	if(parentTrObj==null) return;
	var chkObj=getSelectCheckBoxObj(parentTrObj);
	if(chkObj==null) return;
	if(isChecked)
	{//子节点是选中状态，则当前父分组节点也是选中状态
		chkObj.checked=true;
	}else
	{//子节点是非选中状态
		var childGroupidSuffixes=parentTrObj.getAttribute('childgroupidsuffixes');//所有子分组行的id后缀
		if(childGroupidSuffixes&&childGroupidSuffixes!='')
		{//当前分组节点有子分组节点，则判断它的所有子分组节点，是否有选中的（如果有子分组，判断子分组而不是直接判断子数据行可以提高性能）
			var groupidsArr=childGroupidSuffixes.split(';');
			for(var i=0;i<groupidsArr.length;i=i+1)
			{
				var idSuffixTmp=groupidsArr[i];
				if(idSuffixTmp==null||idSuffixTmp=='') continue;
				var trChildTmp=doc.getElementById(tridPrex+idSuffixTmp);//取到子分组行对象
				if(trChildTmp==null) continue;
				var chkObjTmp=getSelectCheckBoxObj(trChildTmp);
				if(chkObjTmp.checked)
				{//存在选中的子分组
					isChecked=true;
					break;
				}
			}
		}else
		{//当前分组节点没有子分组，全部是子数据节点，则判断所有子数据是否存在被选中的
			var childDataidSuffixes=parentTrObj.getAttribute('childdataidsuffixes');//所有子数据行的id后缀
			if(childDataidSuffixes!=null&&childDataidSuffixes!='')
			{
				var dataidsArr=childDataidSuffixes.split(';');
				for(var i=0;i<dataidsArr.length;i=i+1)
				{
					var idSuffixTmp=dataidsArr[i];
					if(!idSuffixTmp||idSuffixTmp=='') continue;
					var trChildTmp=doc.getElementById(tridPrex+idSuffixTmp);//取到子数据行对象
					if(trChildTmp==null) continue;
					var chkObjTmp=getSelectCheckBoxObj(trChildTmp);
					if(chkObjTmp.checked)
					{//存在选中的子数据行
						isChecked=true;
						break;
					}
				}
			}
		}
		if(isChecked)
			chkObj.checked=true;
		else
			chkObj.checked=false;
	}
	selectParentDataRows(tridPrex,parentTrObj.getAttribute('parenttridsuffix'),isChecked);
}

/**
 * 获取数据行上用于行选中的复选框对象
 */
function getSelectCheckBoxObj(trObj)
{
	var childInputObjs=trObj.getElementsByTagName('INPUT');
	var selectChkObj=null;
	for(var i=0,len=childInputObjs.length;i<len;i=i+1)
	{
		var name=childInputObjs[i].getAttribute('name');
		var type=childInputObjs[i].getAttribute('type');
		if(!type||type!='checkbox') continue;
		if(!name||name.indexOf('_rowselectbox_col')<=0) continue;
		selectChkObj=childInputObjs[i];
		break;
	}
	return selectChkObj;
}

/**
 * 获取数据行上用于行选中的单选框对象
 */
function getSelectRadioBoxObj(trObj)
{
	var childInputObjs=trObj.getElementsByTagName('INPUT');
	var selectRadioObj=null;
	for(var i=0,len=childInputObjs.length;i<len;i=i+1)
	{
		var name=childInputObjs[i].getAttribute('name');
		var type=childInputObjs[i].getAttribute('type');
		if(!type||type!='radio') continue;
		if(!name||name.indexOf('_rowselectbox_col')<=0) continue;
		selectRadioObj=childInputObjs[i];
		break;
	}
	return selectRadioObj;
}

/**
 * 点中标题栏中全选/全不选所有记录行的复选框对象
 */
function doSelectedAllDataRowChkRadio(selectBoxObj)
{
	var boxname=selectBoxObj.getAttribute('name');
	var idx=boxname.lastIndexOf('_rowselectbox');
	if(idx<=0) return;
	var reportguid=boxname.substring(0,idx);
	WX_selected_selectBoxObj_temp=selectBoxObj;
	if(isHasIgnoreSlaveReportsSavingData(reportguid))
  	{
  		wx_confirm('本操作可能会丢失对从报表数据的修改，是否继续？',null,null,null,doSelectedAllDataRowChkRadioImpl,cancelSelectDeselectChkRadio);
  	}else
  	{
  		doSelectedAllDataRowChkRadioImpl('ok');
  	}
}

/**
 * 点中标题栏中全选/全不选所有记录行的复选框对象
 */
function doSelectedAllDataRowChkRadioImpl(input)
{
	if(!wx_isOkConfirm(input)) 
	{
		wx_callCancelEvent();
		return;
	}
	var selectBoxObj=WX_selected_selectBoxObj_temp;
	WX_selected_selectBoxObj_temp=null;
	initRowSelect();
	var selectedTrObjArr=new Array();//本次选中的行对象数组，稍后要传给行选中回调函数
	var deselectedTrObjsArr=new Array();//本次取消选中的所有行对象
	var boxname=selectBoxObj.getAttribute('name');
	var idx=boxname.lastIndexOf('_rowselectbox');
	if(idx<=0) return;
	var reportguid=boxname.substring(0,idx);
	var metadataObj=getReportMetadataObj(reportguid);
  	if(metadataObj==null) return;
	//var parentFixedDataObj=getParentFixedDataObj(reportguid,selectBoxObj);//如果当前是冻结行列报表，则取到在客户端显示数据的表格父<div/>对象
	//if(parentFixedDataObj==null) parentFixedDataObj=document;
	var allChildNamesArr=document.getElementsByName(boxname+'_col');
	if(allChildNamesArr==null||allChildNamesArr.length==0) return;//没有记录
	//alert(allChildNamesArr.length);
	var isChecked=selectBoxObj.checked;//标题复选框是选中，则选中所有行
	var childBoxObjTmp,trObj,mychecked;
	var mStoredTridsTmp=new Object();//用于保存已经放进selectedTrObjArr/deselectedTrObjsArr的行对象ID，以防止同一个记录行被多次存放（比如行列固定的报表，同一个行对象有多套，因此不判断一下会放多份进去）
	for(var i=0,len=allChildNamesArr.length;i<len;i=i+1)
	{
		childBoxObjTmp=allChildNamesArr[i];
		if(childBoxObjTmp==null) continue;
		var rowgroup=childBoxObjTmp.getAttribute('rowgroup');
		if(rowgroup=='true')
		{//当前点击是分组节点上的复选框
			childBoxObjTmp.checked=isChecked;
			continue;
		}
		trObj=getSelectedTrParent(childBoxObjTmp);//获取其父级节点-表格行
		if(trObj==null||trObj.getAttribute('disabled_rowselected')=='true') continue;
		mychecked=childBoxObjTmp.checked;//本选择框之前的选中状态
		if(isChecked)
		{//标题复选框是选中，则选中所有行
			selectDataRow(metadataObj,trObj);
			childBoxObjTmp.checked=true;
			if(!mychecked&&mStoredTridsTmp[trObj.getAttribute('id')]!='true') 
			{
				selectedTrObjArr[selectedTrObjArr.length]=trObj;//只有当本记录之前是非选中状态，才将它放入selectedTrObjsArr以便回调函数能取到
				mStoredTridsTmp[trObj.getAttribute('id')]='true';
			}
		}else
		{
			deselectDataRow(metadataObj,trObj);
			childBoxObjTmp.checked=false;
			if(mychecked&&mStoredTridsTmp[trObj.getAttribute('id')]!='true') 
			{
				deselectedTrObjsArr[deselectedTrObjsArr.length]=trObj;//只有当本记录之前是选中状态，才将它放入deselectedTrObjsArr以便回调函数能取到
				mStoredTridsTmp[trObj.getAttribute('id')]='true';
			}
		}
	}
	invokeRowSelectedMethods(metadataObj,selectedTrObjArr,deselectedTrObjsArr);
}

function getSelectedTrObjGuid(metadataObj,trObj)
{
	if(metadataObj==null) return null;
	var growidx=trObj.getAttribute('global_rowindex');
  	if(growidx==null||growidx=='') return null;
  	return metadataObj.reportguid+'_tr_'+growidx;
}

/**
 * 判断当前行是否选中
 */
function isSelectedRowImpl(reportguid,trObj)
{
	if(reportguid==null||reportguid=='') return false;
	var allSelectedTrObjs=WX_selectedTrObjs[reportguid];
	var growidx=trObj.getAttribute('global_rowindex');
  	if(growidx==null||growidx=='') return false;
	return allSelectedTrObjs!=null&&allSelectedTrObjs[reportguid+'_tr_'+growidx]!=null;
}
/**
 * 选中报表trObj对应的行
 * @param trObj行对象
 */
function selectDataRow(metadataObj,trObj)
{
	var reportguid=metadataObj.reportguid;
	var trguid=getSelectedTrObjGuid(metadataObj,trObj);
	if(trguid==null||trguid=='') return;
  	var allSelecteTrObjs=WX_selectedTrObjs[reportguid];
  	if(allSelecteTrObjs==null)
  	{
  		allSelecteTrObjs=new Object();
  		WX_selectedTrObjs[reportguid]=allSelecteTrObjs;
  	}
  	allSelecteTrObjs[trguid]=trObj;
	setTrBgcolorInSelect(trObj,reportguid);//设置背景色
}

/**
 *
 * 取消某行的选中
 */
function deselectDataRow(metadataObj,trObj)
{
	if(isTrObjInCurrentPage(metadataObj,trObj)) resetTrBgcolorInSelect(trObj);//恢复背景色（不管有没有被选中，都要恢复它的背景色，因为对于行列固定的报表，可能会有多个同样的记录行被选中）
	if(WX_selectedTrObjs==null) return;
	var allSelecteTrObjs=WX_selectedTrObjs[metadataObj.reportguid];
	var trguid=getSelectedTrObjGuid(metadataObj,trObj);
	if(trguid==null||trguid=='') return;
  	if(allSelecteTrObjs!=null&&allSelecteTrObjs[trguid]!=null) delete allSelecteTrObjs[trguid];
}

/*
 * 取消某个表格中所有选中行，都变为不选中状态。
 * @param reportguid 要取消的报表guid
 * @param 返回被取消行选中行对象数组 
 */
function deselectAllDataRow(metadataObj)
{
	var allSelectedTrObjs=getListReportSelectedTrObjsImpl(metadataObj.pageid,metadataObj.reportid,false,false);//获取选中的所有行对象
	if(allSelectedTrObjs!=null&&allSelectedTrObjs.length>0)
	{//只将当前页面的行选中对象恢复一下背景色，其它页的则不用恢复，因为都没显示出来
		for(var i=0,len=allSelectedTrObjs.length;i<len;i++)
		{
			if(isTrObjInCurrentPage(metadataObj,allSelectedTrObjs[i])) resetTrBgcolorInSelect(allSelectedTrObjs[i]);//恢复背景色
		}
	}
	if(WX_selectedTrObjs!=null) delete WX_selectedTrObjs[metadataObj.reportguid];
	return allSelectedTrObjs;//返回所有选中的行对象，因为它们都被取消选中了
}

/**
 * 获取到本次真正取消选中的行对象数组
 * @param selectedTrObjsArr 本次操作选中的所有行对象数组
 * @param deselectedTrObjsArr 本次操作取消选中的所有行对象数组
 */
function getRealDeselectedTrObjs(selectedTrObjsArr,deselectedTrObjsArr)
{
	if(selectedTrObjsArr==null||selectedTrObjsArr.length==0) return deselectedTrObjsArr;
	if(deselectedTrObjsArr==null||deselectedTrObjsArr.length==0) return deselectedTrObjsArr;
	var selectedTrIds=new Object();
	for(var i=0,len=selectedTrObjsArr.length;i<len;i++)
	{//取到所有本次选中的行对象
		selectedTrIds[selectedTrObjsArr[i].getAttribute('global_rowindex')]='true';
	}
	var resultArr=new Array();
	for(var i=0,len=deselectedTrObjsArr.length;i<len;i++)
	{
		if(selectedTrIds[deselectedTrObjsArr[i].getAttribute('global_rowindex')]=='true') continue;//此被取消的行对象又被选中了（比如点击某个已被选中的行）
		resultArr[resultArr.length]=deselectedTrObjsArr[i];
	}
	return resultArr;
}

/**
 * 获取dom元素element所在的数据自动列表报表的数据行的<tr/>对象，它的id必须包含_tr_，且_后面是大于等于0的数字
 */
function getSelectedTrParent(element)
{
	if(!element) return null;
	while(element!=null)
	{
		if(isListReportDataTrObj(element))
		{
			return element;
		}
		element=element.parentNode;
	}
	return null;
}
/**
 * 判断当前<tr/>对象是否是数据自动列表报表数据部分的<tr/>对象，且不是树形分组树枝节点所在的行，即不包括trgoup_
 * 只有这种报表类型的行才可以进行行选中，鼠标滑过时改变背景色等操作
 * 主要是根据它的id特征进行判断
 */
function isListReportDataTrObj(trObj)
{
	if(trObj.tagName!='TR') return false;
	var trid=trObj.getAttribute("id");
	if(!trid||trid=='') return false;
	var idx=trid.lastIndexOf('_tr_');
	if(idx<=0) return false;
	/*if(trid.indexOf('_trhead_')>=0) return false;//或数据标题所在的行*/
	if(trid.indexOf('trgoup_')>0) return false;//树形分组报表树枝节点所在行
	if(trid.substring(0,idx).indexOf(WX_GUID_SEPERATOR)>0&&parseInt(trid.substring(idx+4))>=0)
	{//前缀要包括WX_GUID_SEPERATOR变量值(因为是当前<report/>的guid)，后缀是大于等于0的数字(行号)
		return true;
	}
	return false;
}

/**
 * 获取某个报表的行选中类型
 */
function getRowSelectType(reportguid)
{
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj==null) return '';
	return metadataObj.metaDataSpanObj.getAttribute('rowselecttype');
}

/**
 * 执行行选中回调函数
 * 行选中回调函数存放在数据表格的rowSelectMethods属性中，值为JSON字符串，格式为：rowSelectMethods="{rowSelectMethods:[{value:method1},{value:method2},...]}"
 * @param selectedTrObjArr 本次选中的行对象数组（注意：不是所有选中行对象，只是本次选中的行对象）
 * @param deselectedTrObjArr本次操作取消选中的行对象数组
 */
function invokeRowSelectedMethods(metadataObj,selectedTrObjArr,deselectedTrObjArr)
{
	if((selectedTrObjArr==null||selectedTrObjArr.length==0)&&(deselectedTrObjArr==null||deselectedTrObjArr.length==0)) return;//没有选中行也没有取消选中的行
	var methodsStr=metadataObj.metaDataSpanObj.getAttribute('rowSelectMethods');//取到配置的所有行选中回调函数
	var methodsObj=getObjectByJsonString(methodsStr);
	if(methodsObj==null) return;//没有行选中回调方法
	var allMethodsObj=methodsObj.rowSelectMethods;
	if(allMethodsObj==null||allMethodsObj.length==0) return;
	for(var i=0;i<allMethodsObj.length;i=i+1)
	{//依次调用每个行选中回调函数
		allMethodsObj[i].value(metadataObj.pageid,metadataObj.reportid,selectedTrObjArr,deselectedTrObjArr);
	}
}

/**
 * 获取某个列表报表所有选中行或当前页显示的选中行的<tr/>对象
 * @isOnlyCurrentPage 是否只获取当前页的选中行对象
 * @isNeedOrder 是否需要排序返回，如果为true，则按global_rowindex降序进行排序
 */
function getListReportSelectedTrObjsImpl(pageid,reportid,isOnlyCurrentPage,isNeedOrder)
{
	if(WX_selectedTrObjs==null) return null; 
	var reportguid=getComponentGuidById(pageid,reportid);
	var mSelectTrObjs=WX_selectedTrObjs[reportguid];
	if(mSelectTrObjs==null) return null;
	var metadataObj=getReportMetadataObj(reportguid);
	var selectedTrObjsArr=new Array();
	var trObjTmp;
	for(var key in mSelectTrObjs)
	{
		trObjTmp=mSelectTrObjs[key];
		if(trObjTmp==null) continue;
		if(isOnlyCurrentPage===true&&!isTrObjInCurrentPage(metadataObj,trObjTmp)) continue;//允许跨页选中记录行，且只获取当前页的选中记录但当前行不在本页
		selectedTrObjsArr[selectedTrObjsArr.length]=trObjTmp;
	}
	if(selectedTrObjsArr.length>0&&isNeedOrder===true) orderSelectedTrObjsArr(selectedTrObjsArr);//需要按global_rowindex排序输出
	return selectedTrObjsArr;
}

/**
 * 判断某个选中行是否显示在当前页面上
 */
function isTrObjInCurrentPage(metadataObj,trObj)
{
	if(trObj==null) return false;
	if(metadataObj==null||metadataObj.metaDataSpanObj.getAttribute('isSelectRowCrossPages')!=='true') return true;//如果不允许跨页选中，则肯定在当前页上
	return trObj.getAttribute('wx_not_in_currentpage')!='true';
}

/**
 * 将先中记录行根据global_rowindex属性值按升序排序
 */
function orderSelectedTrObjsArr(selectedTrObjsArr)
{
	if(selectedTrObjsArr==null||selectedTrObjsArr.length<2) return;
	var rowidx1,rowidx2,shouldChangeOrder;
	var len=selectedTrObjsArr.length;
	for(var i=0;i<len-1;i++)
   {
       for(var j=i+1;j<len;j++)
       {
       	shouldChangeOrder=false;
       	rowidx1=selectedTrObjsArr[i].getAttribute('global_rowindex');
       	rowidx2=selectedTrObjsArr[j].getAttribute('global_rowindex');
       	if(rowidx1.indexOf('new_')==0&&rowidx2.indexOf('new_')==0)
       	{//都是新增记录行
       		shouldChangeOrder=parseInt(rowidx1.substring('new_'.length))>parseInt(rowidx2.substring('new_'.length));
       	}else if(rowidx1.indexOf('new_')<0&&rowidx2.indexOf('new_')<0)
       	{//都是已有记录行
       		shouldChangeOrder=parseInt(rowidx1)>parseInt(rowidx2);
       	}else
       	{
       		shouldChangeOrder=rowidx1.indexOf('new_')==0;//如果前面记录是新增行，后面记录是修改行，则新增行在下面，因此需要交换位置
       	}
         if(shouldChangeOrder)
         {
             var trTmp=selectedTrObjsArr[i];
             selectedTrObjsArr[i]=selectedTrObjsArr[j];
             selectedTrObjsArr[j]=trTmp;
         }
       }
   }
}

/**
 * 获取所有选中记录行上各列的数据，对于不可编辑的列表报表，只能获取到配置了rowselectvalue="true"的列的数据
 */
function getListReportSelectedTrDatasImpl(pageid,reportid,isOnlyCurrentPage,isNeedOrder,includeNewTr)
{
	var selectedTrObjsArr=getListReportSelectedTrObjsImpl(pageid,reportid,isOnlyCurrentPage,isNeedOrder);
	if(selectedTrObjsArr==null||selectedTrObjsArr.length==0) return null;
	var resultDataArr=new Array();
	var trObjTmp,rowDataObjTmp;
	for(var i=0,len=selectedTrObjsArr.length;i<len;i++)
	{
		trObjTmp=selectedTrObjsArr[i];
		if(trObjTmp==null||(includeNewTr!==true&&trObjTmp.getAttribute('EDIT_TYPE')=='add')) continue;
		rowDataObjTmp=wx_getListReportColValuesInRow(trObjTmp);
		if(rowDataObjTmp!=null) resultDataArr[resultDataArr.length]=rowDataObjTmp;
	}
	return resultDataArr;
}

/**
 * 获取存放在WX_selectedTrObjs中某个行选中对象的真正行选中对象，
 *		如果此行对象是本页面上某个行对象，则返回动态获取的行对象，
 *		如果不是，则返回defaultTrObj
 * 	如果不是有效的列表报表行对象，则返回null
 */
/*function getRealSelectedTrObj(selectedTrObj,defaultTrObj)
{
	if(selectedTrObj==null) return null;
	var trid=selectedTrObj.getAttribute('id');
	var growidx=selectedTrObj.getAttribute('global_rowindex');
	if(trid==null||trid==''||growidx==null||growidx=='') return null;
	var realSelectedTrObj=document.getElementById(trid);
	if(realSelectedTrObj==null) return defaultTrObj;
	var realGrowidx=realSelectedTrObj.getAttribute('global_rowindex');
	if(realGrowidx==null||realGrowidx=='') return defaultTrObj;
	try
	{
		if(parseInt(realGrowidx)==parseInt(growidx)) return realSelectedTrObj;//在当前页面上存在ID相同且global_rowindex也相同的<tr/>，则用页面上显示的<tr/>
	}catch(e)
	{}
	return defaultTrObj;
}*/

/*
 *将某个<tr/>的背景色设置为选中状态
 */
function setTrBgcolorInSelect(trObj,reportguid)
{
	if(trObj==null) return;
	storeTdsOriginalBgColorInTr(trObj);//保存原始背景色
	setTdsBgColorInTr(trObj,WX_selectedRowBgcolor);
}
/*
 *将某个<tr/>的背景色恢复成原始状态，即没选中时的状态
 */
function resetTrBgcolorInSelect(trObj)
{
	if(trObj==null) return;
	resetTdsBgColorInTr(trObj);
}

/*
 * 鼠标滑过当前行时，改变当前行的背景色
 * @param trObj 当前滑过的行对象
 * @param mouseoverbgcolor 需要换成的新的背景色
 */
function changeRowBgcolorOnMouseOver(trObj,mouseoverbgcolor)
{
	if(trObj==null||!isListReportDataTrObj(trObj)) return;
	storeTdsOriginalBgColorInTr(trObj);
	setTdsBgColorInTr(trObj,mouseoverbgcolor);
}

/**
 * 鼠标移出当前行时，恢复当前行的背景色
 * @param trObj 鼠标移动的行对象
 */
function resetRowBgcolorOnMouseOver(trObj)
{
	if(isSelectedRow(trObj))
	{//如果当前行被选中，则将它的背景色恢复成选中颜色
		setTdsBgColorInTr(trObj,WX_selectedRowBgcolor);
	}else
	{
		resetTdsBgColorInTr(trObj);
	}
}

/**
 * 保存某记录行上所有单元格的原始背景色，以便恢复时使用
 * 这里的保存会忽略掉分组报表的分组单元格，因为不管是行选中还是onmouseover，都不会改变这种列的颜色，所以没必要保存
 */
function storeTdsOriginalBgColorInTr(trObj)
{
	if(trObj==null) return;
	if(isSetTrBgColorOnly(trObj)===true)
	{//只要设置<tr/>本身的背景色即可
		var oldbgcolor=trObj.getAttribute('tr_original_bgcolor');
		if(oldbgcolor!=null&&oldbgcolor!='') return;//已经保存过了
		oldbgcolor=getElementBgColor(trObj);//取到此<tr/>的原始背景色
		if(oldbgcolor==null||oldbgcolor=='') oldbgcolor='#ffffff';
		trObj.setAttribute('tr_original_bgcolor',oldbgcolor);//保存到tr对象的属性中
	}else
	{//必须设置<tr/>中所有<td/>的背景色
		//var trid=trObj.getAttribute('id');
		//var reportguid=trid.substring(0,trid.lastIndexOf('_tr_'));//报表的guid
	  	//if(reportguid=='') return;
		var isgroupcol,isFixedCol,oldbgcolor,tdObjTmp;
		var tdObjs=trObj.cells;
		var isCheckedOriginalColorSaved=false;
		for(var i=0,len=tdObjs.length;i<len;i++)
		{
			tdObjTmp=tdObjs[i];
			isgroupcol=tdObjTmp.getAttribute('groupcol');
			if(isgroupcol=='true') continue;//是分组报表的分组列，不改变背景色，所以不存它的原始背景色
			isFixedCol=tdObjTmp.getAttribute('isFixedCol');
			if(isFixedCol=='true') continue;//如果是冻结列，则视为标题，不改变其背景色
			if(!isCheckedOriginalColorSaved)
			{//还没有检查是否保存过背景色，则检查一下
				oldbgcolor=tdObjTmp.getAttribute('td_original_bgcolor');
				if(oldbgcolor!=null&&oldbgcolor!='') return;//已经保存过此记录行的原始背景色（只要判断到一个td保存了，则整行都保存了）
				isCheckedOriginalColorSaved=true;
			}
			oldbgcolor=getElementBgColor(tdObjTmp);//取到此<td/>的原始背景色
			if(oldbgcolor==null||oldbgcolor=='') oldbgcolor='#ffffff';
			tdObjTmp.setAttribute('td_original_bgcolor',oldbgcolor);//保存到td对象的属性中
		}
	}
}

/**
 * 设置某记录行上所有单元格的颜色为newcolor，但分组报表的分组列上的单元格除外
 */
function setTdsBgColorInTr(trObj,newcolor)
{
	if(trObj==null) return;
	if(isSetTrBgColorOnly(trObj)===true)
	{//只要设置<tr/>本身的背景色即可
		trObj.style.backgroundColor=newcolor;
	}else
	{//必须设置<tr/>中所有<td/>的背景色
		var tdObjs=trObj.cells;
		var isgroupcol,isFixedCol,tdObjTmp;
		for(var i=0,len=tdObjs.length;i<len;i++)
		{
			tdObjTmp=tdObjs[i];
			isgroupcol=tdObjTmp.getAttribute('groupcol');
			if(isgroupcol=='true') continue;//是分组报表的分组列，则不改变背景色
			isFixedCol=tdObjTmp.getAttribute('isFixedCol');
			if(isFixedCol=='true') continue;//如果是冻结列，则视为标题，不改变其背景色
			tdObjTmp.style.backgroundColor=newcolor;
		}
	}
}

/**
 * 重置某记录行上所有单元格的背景色为原始背景色
 */
function resetTdsBgColorInTr(trObj)
{
	//var trid=trObj.getAttribute('id');
	//var reportguid=trid.substring(0,trid.lastIndexOf('_tr_'));//报表的guid
	if(isSetTrBgColorOnly(trObj)===true)
	{//只要设置<tr/>本身的背景色即可
		var originalBgcolorTmp=trObj.getAttribute('tr_original_bgcolor');
		if(originalBgcolorTmp==null||originalBgcolorTmp=='') return;
		trObj.style.backgroundColor=originalBgcolorTmp;
	}else
	{//必须设置<tr/>中所有<td/>的背景色
		var isgroupcol,isFixedCol,idx=0,originalBgcolorTmp;
		var tdObjs=trObj.cells,tdObjTmp;
		for(var i=0,len=tdObjs.length;i<len;i++)
		{
			tdObjTmp=tdObjs[i];
			isgroupcol=tdObjTmp.getAttribute('groupcol');
			if(isgroupcol=='true') continue;//是分组报表的分组列，则不会改变背景色
			isFixedCol=tdObjTmp.getAttribute('isFixedCol');
			if(isFixedCol=='true') continue;//如果是冻结列，则视为标题，不改变其背景色
			originalBgcolorTmp=tdObjTmp.getAttribute('td_original_bgcolor');
			if(originalBgcolorTmp==null||originalBgcolorTmp=='') return;//没有改变过此行的背景色（只要判断到一个<td/>没有，则肯定整行没有）
			tdObjTmp.style.backgroundColor=originalBgcolorTmp;
		}
	}
}

/**
 * 在设置此<tr/>背景色时，是否只要设置<tr/>的背景色即可
 * @return true：只要设置<tr/>本身的背景色；false：必须设置<tr/>中符合要求的<td/>的背景色（这种情况一般是有行分组列的报表或冻结行列标题的报表）
 */
function isSetTrBgColorOnly(trObj)
{
	if(trObj==null) return true;
	var result=trObj.getAttribute('wx_isSetTrBgColorOnly_flag');
	if(result!=null&&result!='')return result=='true';//已经判断过
	var tdObjs=trObj.cells,tdObjTmp;
	for(var i=0,len=tdObjs.length;i<len;i++)
	{
		tdObjTmp=tdObjs[i];
		isgroupcol=tdObjTmp.getAttribute('groupcol');
		isFixedCol=tdObjTmp.getAttribute('isFixedCol');
		if(isgroupcol=='true'||isFixedCol=='true') 
		{//是分组报表的分组列，或者是冻结列，则不能简单的设置<tr/>的背景色，而需要设置每个<td/>的背景色，并且排除掉这些列
			trObj.setAttribute('wx_isSetTrBgColorOnly_flag','false');
			return false;
		}
	}
	trObj.setAttribute('wx_isSetTrBgColorOnly_flag','true');
	return true;
}

/**
 * 存放每个主从报表中，当前显示的从报表对应主报表的哪一行<tr/>对象。
 * 此对象为Map类型，每个主从报表的主报表在此存放一个数据，键为报表ID，值为选中的<tr/>的id
 * 此对象的作用为防止点击当前从报表对应的数据行时，再去刷新从报表。
 */
var WX_Current_Slave_TrObj;

/**
 * 清除掉某个主报表当前刷新从报表的行对象
 * 一般是当前选中的行是新增记录行时，此时会把从报表清除，也就是说此时此主报表没有任意行刷新从报表
 */
function clearCurrentSlaveTrObjOfReport(reportguid)
{
	if(WX_Current_Slave_TrObj==null) return;
	delete WX_Current_Slave_TrObj[reportguid];
}

/**
 * 获取某个主报表显示从报表的记录行
 */
function getRealCurrentSlaveTrObjForReport(reportguid)
{
	if(WX_Current_Slave_TrObj==null) return null;
	var oldTrObj=WX_Current_Slave_TrObj[reportguid];
	if(oldTrObj==null) return null;
	if(oldTrObj.getAttribute('wx_not_in_currentpage')!=='true') return oldTrObj;//这个已经是显示在当前页面上的列
	var tableObj=document.getElementById(reportguid + '_data');
	if(tableObj==null) return null;
	var trChilds = tableObj.getElementsByTagName('TR');
	var trObjTmp;
	for (var i = 0, len = trChilds.length; i < len; i++)
	{
		trObjTmp=trChilds[i];
		if(isTheSameSlaveTrObjOfReport(reportguid,oldTrObj,trObjTmp)) return trObjTmp;
	}
	return null;
}

function setCurrentSlaveTrObjForReport(reportguid,currentSlaveTrObj)
{
	if(WX_Current_Slave_TrObj==null) WX_Current_Slave_TrObj=new Object();
	WX_Current_Slave_TrObj[reportguid]=currentSlaveTrObj;
}

/**
 *
 * 判断某条记录行是否是本报表当前显示从报表的记录行
 */
function isCurrentSlaveTrObjOfReport(reportguid,trObj)
{
	if(trObj==null||WX_Current_Slave_TrObj==null) return false;
	var currentSlaveTrObj=WX_Current_Slave_TrObj[reportguid];
	if(currentSlaveTrObj==null||currentSlaveTrObj.getAttribute('wx_not_in_currentpage')==='true') return false;
	return isTheSameSlaveTrObjOfReport(reportguid,trObj,currentSlaveTrObj);
}

function isTheSameSlaveTrObjOfReport(reportguid,trObj1,trObj2)
{
	if(trObj1==null||trObj2==null) return false;
	var paramsMap1=getRefreshSlaveReportsHrefParamsMap(trObj1);
	var paramsMap2=getRefreshSlaveReportsHrefParamsMap(trObj2);
	if(paramsMap1==null||paramsMap2==null) return false;
	var valtmp1,valtmp2;
	for(var key in paramsMap1)
	{
		valtmp1=paramsMap1[key];
		valtmp2=paramsMap2[key];
		if(valtmp1==null) valtmp1='';
		if(valtmp2==null) valtmp2='';
		if(valtmp1!=valtmp2) return false;
		delete paramsMap2[key];//从这里删掉，以便后面判断当paramsMap1都循环完后，paramsMap2是否还有值
	}
	for(var key in paramsMap2)
	{
		if(paramsMap2[key]!=null&&paramsMap2[key]!='') return false;//如果paramsMap1都循环完后，paramsMap2是否还有值，则说明不是同一条记录
	}
	return true;
}

/**
 * 获取被选中的记录中所有参与刷新从报表的动态参数的所有参数名和参数值，
 * 并拼凑成URL参数字符串形式返回
 * @param trObj 被选中行的<tr/>对象
 */
function getRefreshSlaveReportsHrefParams(trObj)
{
	if(trObj==null) return '';
	var paramsMap=getRefreshSlaveReportsHrefParamsMap(trObj);
	if(paramsMap==null) return '';
	var linkparams='';
	for(var paramname in paramsMap)
	{
		if(linkparams!='') linkparams=linkparams+'&';
		linkparams=linkparams+paramname+'='+encodeURIComponent(paramsMap[paramname]);
	}
	return linkparams;
}

function getRefreshSlaveReportsHrefParamsMap(trObj)
{
	var tdChilds=trObj.getElementsByTagName('TD');
	var resultMap=new Object();
	var paramname,paramvalue;
	var hasValidParamname=false;
	for(var i=0,len=tdChilds.length;i<len;i=i+1)
	{
		paramname=tdChilds[i].getAttribute('slave_paramname');
		if(paramname==null||paramname=='') continue;
		paramvalue=tdChilds[i].getAttribute('value');
		if(paramvalue==null) paramvalue='';
		resultMap[paramname]=paramvalue;
		hasValidParamname=true;
	}
	if(hasValidParamname===false) return null;
	return resultMap;
}

/******************************************************************************
 *单元格左右移动
 */
function initdrag(o,type)
{ 
   o.isInit=true;
   //获取某个单元格的宽度
   o.getTdWidthByIndex=function(index)
					   { 
					     var obj=o.parentNode.parentNode.cells[o.parentNode.cellIndex+index];
						  var width=obj.style.width;
						  if(width&&width!=''&&width.indexOf('%')<0) return parseInt(width);//配置了宽度，且不是百分比
						  if(window.ActiveXObject||isChrome||ISOPERA)
						  { //IE,google等浏览器
							  //return o.parentNode.parentNode.cells[o.parentNode.cellIndex+index].offsetWidth;
  						      return obj.offsetWidth;  
						  }else
						  { 
						      var padding=o.parentNode.parentNode.parentNode.parentNode.cellPadding;
							   if(!padding)
							   {
							       padding=1;
							   }
							   var border=o.parentNode.parentNode.parentNode.parentNode.border;
							   if(!border)
							   {
							      border=1;
							   }else if(parseInt(border)>=1)
							   {
							      //border=2;
							   }
							  return parseInt(o.parentNode.parentNode.cells[o.parentNode.cellIndex+index].offsetWidth)- 
								     parseInt(padding)*2-parseInt(border); 
						  } 
					   } 
   //设置某个单元格的宽度
   o.setTdWidthByIndex=function(index,newwidth)
                       { 
                          /**for(var i=0;i<o.parentNode.parentNode.parentNode.parentNode.rows.length;i++) 
						  { 
                              o.parentNode.parentNode.parentNode.parentNode.rows[0].cells[index].style.width=newwidth; 
                          }*/
                          o.parentNode.parentNode.cells[index].style.width=newwidth+'px'; 
                       } 

   o.firstChild.onmousedown=function(){return false;}; 
   o.onmousedown=function(a)
                 { 
                    var d=document;
						  if(!a)a=window.event; 
                    var lastX=a.clientX; 
                    var watch_dog=0;
						  if(type) watch_dog=o.getTdWidthByIndex(0)+o.getTdWidthByIndex(1);
                    if(o.setCapture) o.setCapture(); 
                    else if(window.captureEvents) window.captureEvents(Event.MOUSEMOVE|Event.MOUSEUP); 
                    d.onmousemove=function(a)
					              { 
                                 if(!a)a=window.event; 
                                 if(type&&o.getTdWidthByIndex(0)+o.getTdWidthByIndex(1)>watch_dog)
									 		{ 
										 		o.setTdWidthByIndex(o.parentNode.cellIndex+1,watch_dog-o.getTdWidthByIndex(0)); 
										 		return; 
								     		} 
                                 var t=a.clientX-lastX; 
									 		if(t>0) 
									 		{//向右移动 
									     		if(type&&parseInt(o.parentNode.parentNode.cells[o.parentNode.cellIndex+1].style.width)-t<10) 
										    		return; 
										 		o.setTdWidthByIndex(o.parentNode.cellIndex,o.getTdWidthByIndex(0)+t); 
										 		if(type)
										 		{//改变相临单元格宽度
													o.setTdWidthByIndex(o.parentNode.cellIndex+1,o.getTdWidthByIndex(1)-t); 
                                    }else
										 		{//改变表格宽度
										   		o.parentNode.parentNode.parentNode.parentNode.style.width=
											                   (o.parentNode.parentNode.parentNode.parentNode.offsetWidth+t)+'px';
										 		}
									 		} else 
									 		{//向左移动 此时t为负数
										 		if(parseInt(o.parentNode.parentNode.cells[o.parentNode.cellIndex].style.width)+t<10) 
									        		return; 
										 		o.setTdWidthByIndex(o.parentNode.cellIndex,o.getTdWidthByIndex(0)+t);
										 		if(type)
										 		{//改变相临单元格宽度
													o.setTdWidthByIndex(o.parentNode.cellIndex+1,o.getTdWidthByIndex(1)-t); 
                                    }else
										 		{//改变表格宽度
										    		o.parentNode.parentNode.parentNode.parentNode.style.width=
										                       (o.parentNode.parentNode.parentNode.parentNode.offsetWidth+t)+'px';
                                    } 
									 		} 
								     		lastX=a.clientX; 
                               }; 
							d.onmouseup=function()
					            	 { 
								   		if(o.releaseCapture) o.releaseCapture(); 
								   		else if(window.captureEvents) window.captureEvents(Event.MOUSEMOVE|Event.MOUSEUP); 
								   		d.onmousemove=null; 
                                 d.onmouseup=null; 
                               }; 
                 }; 
}
/******************************************************************************
 * 上下拖动记录行进行排序
 */
var dragrow_table;
var dragrow_pageid;//当前拖动的报表所在页面ID
var dragrow_reportid;//当前拖动的报表ID
var _tempDragrowTarget;//被拖动的记录行的影子table
var _fromDragrowTarget;//被拖动的起始行
var _toDroprowTarget;//拖动到的目标位置行
var _isDragRow = false;//当前是否正在拖动行操作
var dragrow_enabled=true;//当前是否允许行拖动操作（比如滑过单元格上的输入框时，就不允许做行拖动操作，否则影响输入框编辑）
/**
 * 点击了某个可拖动的行
 * @param trObj 被拖动的行对象
 */
function handleRowDragMouseDown(trObj,pageid,reportid)
{    
	if(trObj.getAttribute('EDIT_TYPE')=='add') return;//拖动的是新添加的行，则不做处理
	if(_tempDragrowTarget!=null)
	{//如果上一次行移动还没有完成（可能是因为子组件的onmousemove()事件阻止了，导致行对象的移动还没有完成，比如对于可编辑报表，如果移动的是单元格的选中的文本，则会出现这处情况）
		handleRowMouseUp(trObj);//把上次的行移动到本次点击的行上
		return;
	}
	if(!dragrow_enabled) return;
   _isDragRow = true;//设置当前正在拖动的行操作
   //设置当前拖动的参数
   _fromDragrowTarget = trObj;
   _toDroprowTarget = trObj;
   dragrow_pageid=pageid;
   dragrow_reportid=reportid;
   dragrow_table= getParentElementObj(trObj,'TABLE');
   if(dragrow_table==null) return false;
   document.body.onselectstart = function() {return false;}//取消可选中，以免拖动时，将表格中的文本选中了
   document.body.style.MozUserSelect="none";
   //如果当前单元格所属表格尚没有初始化，则进行处始化
   if(!dragrow_table.isInitRowDrag)
   {
   	dragrow_table.isInitRowDrag=true;
   	//为当前数据表格所有记录行加上几个鼠标事件，以便拖到表格任意行时都能完成拖动操作
   	var trObjTmp;
   	var trObjs=dragrow_table.tBodies[0].rows;
   	for(var i=0,len=trObjs.length;i<len;i++) 
    	{
       	trObjTmp=trObjs[i];
       	//不加上如下两个事件，因为行滑动时，框架要用它们来改变被滑过行的背景色，即这两个方法已经框架使用了，所以下面不能修改它
       	//trObjTmp.onmouseover = function () 
       	//{
           	//handleRowMouseOver(this);
       	//};
       	//trObjTmp.onmouseout = function () 
       	//{
           	//handleRowMouseOut();
       	//};
       	trObjTmp.onmouseup = function () 
       	{
           	handleRowMouseUp(this);
       	};
    	}
   }
   document.body.style.cursor="move";
   EventTools.addEventHandler(window.document.body,"mousemove",handleRowMouseMove);
   EventTools.addEventHandler(window.document.body,"mouseup",handleBodyRowMouseUp);
   /**
    * 创建被拖动行的影子元素对象
    */
   _tempDragrowTarget=document.createElement('TABLE');
   _tempDragrowTarget.className=dragrow_table.className;
   var tbodyObj= document.createElement("TBODY");//必须要有tbody，否则IE中无法正常显示
   tbodyObj.appendChild(trObj.cloneNode(true));//将被拖动的行对象完整的复制一份出来放入这个影子对象中
   _tempDragrowTarget.appendChild(tbodyObj);
   with(_tempDragrowTarget.style) 
   {
       display = "none";
       position='absolute';
       zIndex=101;
       filter="Alpha(style=0,opacity=50)";
       opacity=0.5;
       width=dragrow_table.clientWidth+'px';
       left=getElementAbsolutePosition(dragrow_table).left;
   }
   if(dragrow_table.style.tableLayout!=null) _tempDragrowTarget.style.tableLayout=dragrow_table.style.tableLayout;//有可能是fixed
   document.body.appendChild(_tempDragrowTarget);
}

/**
 * 不是在表格中释放鼠标，则将拖动状态还原，且不完成拖动操作
 */
function handleBodyRowMouseUp() 
{
	 document.body.style.cursor="";
	 document.body.onselectstart = function() {return true;}////恢复可选中
    document.body.style.MozUserSelect="";
    _isDragRow = false;
    removeTempDragrowTarget();
    EventTools.removeEventHandler(window.document.body,"mousemove",handleRowMouseMove);
    EventTools.removeEventHandler(window.document.body,"mouseup",handleBodyRowMouseUp);
    _toDroprowTarget = null;
}

/**
 * 在某个记录行上释放鼠标
 */
function handleRowMouseUp(trObj) 
{
	document.body.style.cursor="";
	removeTempDragrowTarget();//一定要放在下面代码的上面，因为mouseup时，无论如何都要删掉此对象
   if(!_isDragRow) return false;
   _isDragRow = false;
   EventTools.removeEventHandler(window.document.body,"mousemove",handleRowMouseMove);
   EventTools.removeEventHandler(window.document.body,"mouseup",handleBodyRowMouseUp);
   if(_toDroprowTarget!=null) 
   {
       var tableObjTmp=getParentElementObj(trObj,'TABLE');
       if(tableObjTmp==dragrow_table)
       {//释放鼠标的行与拖动的行是在同一个表格，则完成拖动操作
       	if(trObj.getAttribute('EDIT_TYPE')=='add')
       	{//目标位置的记录行是添加行
       		var trObjs=dragrow_table.tBodies[0].rows;
       		for(var i=trObjs.length-1;i>=0;i--)
       		{//从下往上循环表格所有记录行，找出第一个有效的且不是添加数据的记录行做为目标位置，即将当前记录行移到最后位置
       			if(trObjs[i].getAttribute('EDIT_TYPE')=='add') continue;
       			if(!isListReportDataTrObj(trObjs[i])) continue;
       			trObj=trObjs[i];
       			break;
       		}
       	}
       	_toDroprowTarget=trObj;
        	changeListReportRoworderByDrag();
       }
       _toDroprowTarget = null;
    }
    document.body.onselectstart = function() {return true;}//恢复可选中
    document.body.style.MozUserSelect="";
}

/**
 * 清除掉被移动行的影子元素
 */
function removeTempDragrowTarget()
{
	if(_tempDragrowTarget!=null)
	{
		if(_tempDragrowTarget.parentNode!=null)
		{
			_tempDragrowTarget.parentNode.removeChild(_tempDragrowTarget);
		}else
		{
			_tempDragrowTarget.style.display='none';
		}
		_tempDragrowTarget=null;
	}
}

/**
 * 鼠标滑过记录行，不再使用
 */
/*function handleRowMouseOver(trObj) 
{    
    if(!_isDragRow) { return false; }
    if(!isListReportDataTrObj(trObj))
    {//如果移到非数据行上面（比如标题行上），则改变鼠标状态为不允许
    		document.body.style.cursor="not-allowed";
    		return false;
    }
    var tableObjTmp=getParentElementObj(trObj,'TABLE');
    if(tableObjTmp==dragrow_table)
    {//鼠标滑过的行与拖动的行是在同一个表格
       	document.body.style.cursor="move";
       	_toDroprowTarget=trObj;
    }
}*/
/**
 * 鼠标滑出记录行，不再使用
 */
/*function handleRowMouseOut() 
{
    if(_toDroprowTarget==null) { return false; }    
    if(_isDragRow) document.body.style.cursor="not-allowed";
    _toDroprowTarget = null;
}*/

/**
 * 鼠标正在移动
 */
function handleRowMouseMove(oEvent) 
{
    oEvent = EventTools.getEvent(oEvent);
    if(oEvent.type.indexOf("mousemove") == -1 ) 
    {
        EventTools.removeEventHandler(window.document.body,"mousemove",handleRowMouseMove);
        EventTools.removeEventHandler(window.document.body,"mouseup",handleBodyRowMouseUp);
        return false;
    }
    with(_tempDragrowTarget.style) 
    {
        top = (oEvent.pageY+5) + "px";
        display = "";
    }    
}

/**
 * 通过行移动改变记录行顺序
 */
function changeListReportRoworderByDrag() 
{
	if(_fromDragrowTarget==null||_toDroprowTarget==null||_fromDragrowTarget==_toDroprowTarget) return false;
	if(!isListReportDataTrObj(_toDroprowTarget)) return false;
	var trFromId=_fromDragrowTarget.getAttribute('id');
	var trToId=_toDroprowTarget.getAttribute('id');
	//alert(trFromId+'|||'+trToId);
	if(trFromId==trToId) return false;
	var fromrowidx=parseInt(trFromId.substring(trFromId.lastIndexOf('_tr_')+'_tr_'.length),10);
	var torowidx=parseInt(trToId.substring(trToId.lastIndexOf('_tr_')+'_tr_'.length),10);
	var direct=fromrowidx>torowidx;//如果被拖动的记录行在下面，则移动时，它要放到目标记录行的上面，此时direct为true；反之则放在目标记录行的下面
	var url=getRoworderUrl(dragrow_pageid,dragrow_reportid,_fromDragrowTarget);
	if(url==null||url=='') return false;
   var destTrParams=getRoworderParamsInTr(_toDroprowTarget);//目标位置记录行的参数
   url=url+'&'+dragrow_reportid+'_ROWORDERTYPE=drag';
   url=url+'&'+dragrow_reportid+'_DESTROWPARAMS='+destTrParams;
   url=url+'&'+dragrow_reportid+'_ROWORDERDIRECT='+direct;
   refreshComponent(url);
}

/**
 * 通过点击上、下箭头改变行顺序
 */
function changeListReportRoworderByArrow(pageid,reportid,element,direct)
{
	var trObj=getValidParentTrObj(element);
	if(trObj==null) return false;
	var url=getRoworderUrl(pageid,reportid,trObj);
	var trid=trObj.getAttribute('id');
	var tridx=parseInt(trid.substring(trid.lastIndexOf('_tr_')+'_tr_'.length),10);
	var destTrObj=null;
	if(direct&&tridx>0)
	{//向上移且当前行不是第一行
		destTrObj=document.getElementById(trid.substring(0,trid.lastIndexOf('_tr_')+'_tr_'.length)+(tridx-1));
	}else if(!direct)
	{//向下移
		destTrObj=document.getElementById(trid.substring(0,trid.lastIndexOf('_tr_')+'_tr_'.length)+(tridx+1));
	}
	if(destTrObj!=null&&destTrObj.getAttribute('EDIT_TYPE')!='add')
	{
		var destTrParams=getRoworderParamsInTr(destTrObj);
		url=url+'&'+reportid+'_DESTROWPARAMS='+destTrParams;
	}
	url=url+'&'+reportid+'_ROWORDERTYPE=arrow';
   url=url+'&'+reportid+'_ROWORDERDIRECT='+direct;
   refreshComponent(url);
}

/**
 * 通过修改行排序输入框的值改变行的顺序
 */
var WX_Roworder_inputbox_pageid;
var WX_Roworder_inputbox_reportid;
var WX_Roworder_inputbox_trObj;
var WX_Roworder_inputbox_newordervalue;
function changeListReportRoworderByInputbox(pageid,reportid,inputboxObj,oldvalue)
{
	if(inputboxObj==null||inputboxObj.value==null||inputboxObj.value==''||inputboxObj.value==oldvalue) return;
	var trObj=getValidParentTrObj(inputboxObj);
	if(trObj==null) return;
	WX_Roworder_inputbox_pageid=pageid;
	WX_Roworder_inputbox_reportid=reportid;
	WX_Roworder_inputbox_trObj=trObj;
	WX_Roworder_inputbox_newordervalue=inputboxObj.value;
	wx_confirm('确认修改此行排序值为'+inputboxObj.value+'?','排序',null,null,doChangeListReportRoworderByInputbox);
}

function doChangeListReportRoworderByInputbox(input)
{
	if(wx_isOkConfirm(input))
	{
		var url=getRoworderUrl(WX_Roworder_inputbox_pageid,WX_Roworder_inputbox_reportid,WX_Roworder_inputbox_trObj);
		if(url==null||url=='')
		{
			wx_warn('没有取到行排序所需的参数');
			return;
		}
		url=url+'&'+WX_Roworder_inputbox_reportid+'_ROWORDERTYPE=inputbox';
		url=url+'&'+WX_Roworder_inputbox_reportid+'_ROWORDERVALUE='+WX_Roworder_inputbox_newordervalue;
		refreshComponent(url);
	}
	WX_Roworder_inputbox_pageid=null;
	WX_Roworder_inputbox_reportid=null;
	WX_Roworder_inputbox_trObj=null;
	WX_Roworder_inputbox_newordervalue=null;
}

/**
 * 通过点击置顶按钮改变行顺序
 */
function changeListReportRoworderByTop(pageid,reportid,element)
{
	var trObj=getValidParentTrObj(element);
	var url=getRoworderUrl(pageid,reportid,trObj);
	if(url==null||url=='') return;
	url=url+'&'+reportid+'_ROWORDERTYPE=top';
	refreshComponent(url);
}

/**
 * 获取到element所在的父数据行对象
 */
function getValidParentTrObj(element)
{
	while(element!=null)
	{
		if(element.tagName=='TR'&&isListReportDataTrObj(element)&&element.getAttribute('EDIT_TYPE')!='add')
		{
			return element;
		}
		element=getParentElementObj(element,'TR');
	}
	return null;
}

/**
 * 获取排序某记录行的URL，并带上此记录行中参与行排序的参数
 */
function getRoworderUrl(pageid,reportid,trObj)
{
	var reportguid=getComponentGuidById(pageid,reportid);
   var reportMetadataObj=getReportMetadataObj(reportguid);
   var url=getComponentUrl(pageid,reportMetadataObj.refreshComponentGuid,reportMetadataObj.slave_reportid);
   var roworderparams=getRoworderParamsInTr(trObj);
	if(roworderparams==null||roworderparams=='') return null;
	url=url+'&'+reportid+'_ROWORDERPARAMS='+encodeURIComponent(roworderparams);
	return url;
}

/**
 * 获取记录行中所有参与行排序的列的参数值
 */
function getRoworderParamsInTr(trObj)
{
	var roworderparams='';
	var tdObjTmp;
   var name,value;
   for(var i=0,len=trObj.cells.length;i<len;i++) 
   {
   	tdObjTmp=trObj.cells[i];
   	name=tdObjTmp.getAttribute('value_name');
   	if(name==null||name=='') continue;
   	value=tdObjTmp.getAttribute('value');
   	if(value==null) value='';
   	roworderparams=roworderparams+name+SAVING_NAMEVALUE_SEPERATOR+value+SAVING_COLDATA_SEPERATOR;
   }
   if(roworderparams.lastIndexOf(SAVING_COLDATA_SEPERATOR)==roworderparams.length-SAVING_COLDATA_SEPERATOR.length)
	{
		roworderparams=roworderparams.substring(0,roworderparams.length-SAVING_COLDATA_SEPERATOR.length);
	}
	return roworderparams;
}

/******************************************************************************
 *单元格左右拖动
 */
var drag_table;
var drag_pageid;//当前拖动的报表所在页面ID
var drag_reportid;//当前拖动的报表ID
var _tempDragTarget;//被拖动的单元格的影子div
var _fromDragTarget;//被拖动的起始单元格
var _toDropTarget;//拖动的目标单元格
var _isDrag = false;//当前正在拖动单元格
var drag_enabled=true;//当前是否在做不允许列拖动的操作，比如正在调整单元格大小时，不能拖动单元格。
//var whenCanDropToTargStyle ='';// "_toDropTarget.style.borderLeft = 'thin solid #00FFFF'";
//var whenCannotDropToTargStyle ='';// "_toDropTarget.style.borderLeft = '1'";

function insertAfter(newElement,targetElement) 
{
	var parent = targetElement.parentNode;
	if (parent.lastChild == targetElement) 
	{
    	parent.appendChild(newElement);
	} else 
	{
    	parent.insertBefore(newElement,targetElement.nextSibling);
	}
}

function setTempDragTarget(elem) 
{
    _tempDragTarget.innerHTML = elem.innerHTML;
    _tempDragTarget.className = "dragShadowOnMove";
    _tempDragTarget.style.display = "none";
    _tempDragTarget.style.height= elem.clientHeight+'px';//.style.height;
    _tempDragTarget.style.width = elem.clientWidth+'px';//.style.width;     
}
/**
 * 点击了某个可拖动的单元格
 * @param elem 拖动的元素
 *		
 */
function handleCellDragMouseDown(elem,pageid,reportid)
{    
	if(!drag_enabled) return false;
    _isDrag = true;//设置当前正在拖动的单元格
    //处理被拖动单元格的影子div
    _tempDragTarget=document.getElementById('dragShadowObjId');
    if(!_tempDragTarget)
    {
    	_tempDragTarget = document.createElement("DIV");    
    	_tempDragTarget.style.display = "none";
    	_tempDragTarget.id='dragShadowObjId';
    	/*if(window.document.body) 
    	{
        	window.document.body.insertBefore(_tempDragTarget,window.document.body.firstChild);
    	}*/
    	document.body.appendChild(_tempDragTarget);
    }
    setTempDragTarget(elem);
    //设置当前拖动的参数
    _fromDragTarget = elem;
    _toDropTarget = elem;
    drag_pageid=pageid;
    drag_reportid=reportid;
    drag_table= getParentElementObj(elem,'TABLE');
    if(!drag_table) return false;
    //drag_table.onselectstart = function() {return false;}//取消可选中，以免拖动时，将表格中的文本选中了
    document.body.onselectstart = function() {return false;}//取消可选中，以免拖动时，将表格中的文本选中了
    document.body.style.MozUserSelect="none";
    //如果当前单元格所属表格尚没有初始化，则进行处始化
    if(!drag_table.isInitCellDrag)
    {
    	drag_table.isInitCellDrag=true;
    	//为当前数据表格所有单元格加上几个鼠标事件，以便拖到表格任意单元格都能完成拖动操作，而不仅仅是标题部分的单元格
    	for(var i=0,len=drag_table.tBodies[0].rows.length;i<len;i++) 
    	{
        	var cells = drag_table.tBodies[0].rows[i].cells;
        	for(var j=0,len2=cells.length;j<len2;j++) 
        	{
            	var cell = cells[j];
            	cell.onmouseover = function () 
            	{
                	handleMouseOver(this);
            	};
            	cell.onmouseout = function () 
            	{
                	handleMouseOut();
            	};
            	cell.onmouseup = function () 
            	{
                	handleMouseUp(this);
            	};
        	}
    	}
    }
    document.body.style.cursor="move";
    EventTools.addEventHandler(window.document.body,"mousemove",handleMouseMove);
    EventTools.addEventHandler(window.document.body,"mouseup",handleBodyMouseUp);
}

/**
 * 不是在表格中释放鼠标，则将拖动状态还原，且不完成拖动操作
 */
function handleBodyMouseUp() 
{
	 document.body.style.cursor="";
	 document.body.onselectstart = function() {return true;}////恢复可选中
    document.body.style.MozUserSelect="";
    //if(_isDrag){ _isDrag = false;}
    _isDrag = false;
    EventTools.removeEventHandler(window.document.body,"mousemove",handleMouseMove);
    EventTools.removeEventHandler(window.document.body,"mouseup",handleBodyMouseUp);
    _tempDragTarget.style.display = "none";
    if(_toDropTarget) 
    {
        _toDropTarget = null;
    }
}

/**
 * 在某个单元格上释放鼠标
 */
function handleMouseUp(elem) 
{
	document.body.style.cursor="";
    if(!_isDrag) 
    {
        return false;
    }
    _isDrag = false;
    EventTools.removeEventHandler(window.document.body,"mousemove",handleMouseMove);
    EventTools.removeEventHandler(window.document.body,"mouseup",handleBodyMouseUp);
    _tempDragTarget.style.display = "none";
    if(_toDropTarget) 
    {
        var tableObjTmp=getParentElementObj(elem,'TABLE');
        if(tableObjTmp==drag_table)
        {//释放鼠标的单元格与拖动的单元格是同一个表格，则完成拖动操作
        	_toDropTarget=elem;
        	moveTargetToByServer();
        }
        _toDropTarget = null;
    }
    document.body.onselectstart = function() {return true;}//恢复可选中
    document.body.style.MozUserSelect="";
}
/**
 * 鼠标经过单元格
 */
function handleMouseOver(elem) 
{    
   if(!_isDrag) { return false; }
   var tableObjTmp=getParentElementObj(elem,'TABLE');
   if(tableObjTmp==drag_table)
   {//释放鼠标的单元格与拖动的单元格是同一个表格，则表示可以拖动
       document.body.style.cursor="move";
       _toDropTarget = elem;
   }
}
/**
 * 鼠标移出单元格
 */
function handleMouseOut() 
{
    if(!_toDropTarget) { return false; }    
    if(_isDrag) document.body.style.cursor="not-allowed";
    _toDropTarget = null;
}

function handleMouseMove(oEvent) 
{
    oEvent = EventTools.getEvent(oEvent);
    if(oEvent.type.indexOf("mousemove") == -1 ) 
    {
        EventTools.removeEventHandler(window.document.body,"mousemove",handleMouseMove);
        EventTools.removeEventHandler(window.document.body,"mouseup",handleBodyMouseUp);
        return false;
    }
    var x = oEvent.pageX + 10;
    var y = oEvent.pageY + 10;
    with(_tempDragTarget.style) 
    {
        left = x + "px";
        top = y + "px";
        display = "";
    }    
}
/**
 * 在客户端进行两列单元格的拖动动作
 * 这种方式只能对单行数据标题的报表进行拖动操作
 */
function moveTargetToByClient() 
{
	if(!_fromDragTarget||!_toDropTarget||_fromDragTarget==_toDropTarget) return false;
	var fromIndex=_fromDragTarget.cellIndex;
	var toIndex=_toDropTarget.cellIndex;
	if(fromIndex==toIndex) return;
	var isforward=true;//向前（右）移
	if(fromIndex>toIndex) isforward=false;//向后（左）移
    for (var i=0;i<drag_table.tBodies[ 0 ].rows.length;i++ ) 
    {
        var row = drag_table.tBodies[0].rows[i];
        var fTag = row.cells[fromIndex];
        var tTag = row.cells[toIndex];
        //row.insertBefore(fTag,tTag);
		if(!isforward)
		{//向左移
			row.insertBefore(fTag,tTag);
		}else
		{//向右移
			if(tTag.cellIndex==row.cells.length-1)
			{//目标位置是最后一列
				row.appendChild(fTag);
			}else
			{
				row.insertBefore(fTag,row.cells[toIndex+1]);
			}
		}
    }
}
/**
 * 在服务器端进行两列单元格的移动动作
 */
function moveTargetToByServer() 
{
	if(!_fromDragTarget||!_toDropTarget||_fromDragTarget==_toDropTarget) return false;
	/*if(!drag_isUltraReport)
	{//单行数据标题的报表
		var fromIndex=_fromDragTarget.cellIndex;
		var toIndex=_toDropTarget.cellIndex;
		if(fromIndex==toIndex) return;
		_toDropTarget=drag_table.tBodies[0].rows[0].cells[toIndex];//根据释放拖动的单元格取到其标题行的单元格
    }else
    {//复杂标题的报表
    	_toDropTarget=getThOfDropTargetTd();
    }*/
    /**
     * 统一采用getThOfDropTargetTd()方法获取释放拖动的单元格的标题单元格
     * 这样可以兼容复杂表头，在此方法中是根据坐标和宽度来获取标题单元格
     */
    var direct=getThOfDropTargetTd();
    if(direct==0) return;
    var fromcolid=_fromDragTarget.getAttribute('dragcolid');
    var tocolid=_toDropTarget.getAttribute('dragcolid');
    if(!fromcolid||!tocolid||fromcolid==tocolid) return;
    
    var reportguid=getComponentGuidById(drag_pageid,drag_reportid);
    var reportMetadataObj=getReportMetadataObj(reportguid);
    var url=getComponentUrl(drag_pageid,reportMetadataObj.refreshComponentGuid,reportMetadataObj.slave_reportid);
    url=replaceUrlParamValue(url,drag_reportid+"_DRAGCOLS",fromcolid+";"+tocolid);
    url=url+"&"+drag_reportid+"_DRAGDIRECT="+direct;
    //url=addLazyLoadParamsToUrl(reportMetadataObj,url);
    WX_showProcessingBar=false;//不显示正在加载的进度条
    refreshComponent(url,null,{keepSelectedRowsAction:true,keepSavingRowsAction:false});//这里之所以不保存编辑数据，是因为顺序变了后再将编辑行替换回去会与新的顺序不符
}
/**
 * 在复杂标题的报表中，获取释放拖动的单元格所在数据标题单元格
 * 因为在这种复杂标题的报表中，不能像单行数据标题的报表中一样，根据cellIndex获取当前单元格对应的标题行的单元格
 * 只能根据横坐标和宽度一致来判断
 */
function getThOfDropTargetTd()
{
    var cellObj;
    var cellDragColId;
	 var row=_fromDragTarget.parentNode;//取到起始单元格所在的行对象，如果释放拖动的单元格与起始单元格之间可以拖动，则它们的标题单元格必须是同一行的。
    for(var i=0,len=row.cells.length;i<len;i++)
    {
    	cellObj=row.cells[i];
    	cellDragColId=cellObj.getAttribute('dragcolid');
    	if(cellDragColId==null||cellDragColId=='') continue;//当前标题单元格不允许拖动，则它也不能做为拖动的目的地
    	var fromThLeft=getElementAbsolutePosition(_fromDragTarget).left;
    	var targetWidth=_toDropTarget.offsetWidth;
    	var targetLeft=getElementAbsolutePosition(_toDropTarget).left;
    	var thWidth=cellObj.offsetWidth;
    	var thLeft=getElementAbsolutePosition(cellObj).left;
    	
    	if(_fromDragTarget==cellObj||fromThLeft==thLeft) continue;
    	if(thWidth<targetWidth)
    	{//当前标题单元格宽度释放拖动的目标单格宽度小，则肯定不是它的标题单元格
    		continue;
    	}
    	if(thLeft>targetLeft)
    	{//当前标题单元格左边距大于释放拖动的目标单元格左边距则不可能是它的标题单元格
    		continue;
    	}
    	if(thLeft<=targetLeft&&thLeft+thWidth>=targetLeft+targetWidth)
    	{//释放拖动的目标单元格在当前标题单元格之间（即前者的左边距大于等于后者左边距且右边距小于等于后者右边距），则说明当前标题单元格是它的标题单元格
    		var fromgroupid=_fromDragTarget.getAttribute('parentGroupid');
    		var togroupid=cellObj.getAttribute('parentGroupid');
    		if(!fromgroupid&&!togroupid||fromgroupid==togroupid)
    		{//源与目标单元格都不在列分组下面或在同一个分组下面，则可以移动。
    			_toDropTarget=cellObj;//返回释放拖动单元格对应的标题行的单元格
    			if(fromThLeft==thLeft) return 0;
    			if(fromThLeft>thLeft) return -1;//向左移
    			if(fromThLeft<thLeft) return 1; //向右移
    		}
    	}
    }
    return 0;
}

/********************************************************************************
 * 点击标题进行列排序的事件
 */
 function clickorderby(spanObj,orderbyparam)
 {
 	var tableObjTmp=getParentElementObj(spanObj,'TABLE');
 	if(!tableObjTmp) return false;
 	var pageid=tableObjTmp.getAttribute('pageid');
   var reportid=tableObjTmp.getAttribute('reportid');
   var refreshComponentGuid=tableObjTmp.getAttribute('refreshComponentGuid');
   var isSlave=tableObjTmp.getAttribute('isSlave');
   var slave_reportid=null;
   if(isSlave&&isSlave=='true') slave_reportid=reportid;
   var url=getComponentUrl(pageid,refreshComponentGuid,slave_reportid);
   url=replaceUrlParamValue(url,reportid+'ORDERBY',orderbyparam);
   url=replaceUrlParamValue(url,reportid+'ORDERBY_ACTION','true');
 	refreshComponent(url);
 }
 
 /*******************************************************************************
 *过滤
 */
var COL_FILTER_btnObj;//当前点中的列过滤按钮对象
var COL_FILTER_selectSpanTitleStart="<span style='width:100%;display:block;' class='spanOutputTitleElement'>";
var COL_FILTER_selectSpanStart="<span style='width:100%;display:block;'  class='spanOutputNormalElement' onmouseover='setHighColor(this)' ";
var COL_FILTER_selectSpanEnd="</span>";

/**
 * 关闭所有已经打开的列过滤结果提示窗口
 */
function closeAllColFilterResultSpan()
{
	if(COL_FILTER_btnObj)
	{
		var spanObj=document.getElementById(COL_FILTER_btnObj.obj.spanOutputId);
		if(spanObj) spanObj.style.display='none';
		COL_FILTER_btnObj.obj.currentValueSelected=-1;
	}
	COL_FILTER_btnObj=null;
	var treeContainerObj=document.getElementById('wx_titletree_container');
	if(treeContainerObj!=null) closeSelectedTree();
}

/**
 *
 * @param filterBtnObj
 * @param params 列过滤所需参数，格式为： 
 *	   reportguid:reportguid,
 *		webroot:webroot,//应用的根URL
 *		skin:skin,//皮肤配置
 *		property:property,//过滤列对应<col/>的property属性值
 *		urlParamName:urlParamName,//当前过滤列对应url的参数名
 *		multiply:multiply,当前列是否允许多选
 *		filterwidth:filterwidth,列过滤选项显示的宽度
 *		navigateReportIds:navigateReportIds 如果当前报表是翻页报表，存放翻页的id
 */
function getFilterDataList(filterBtnObj,params)
{
	closeAllColFilterResultSpan();//关闭所有已经打开的列过滤结果提示窗口
	COL_FILTER_btnObj=filterBtnObj;
	var paramsObj=getObjectByJsonString(params);
	var spanwidth=paramsObj.filterwidth;
	var tdObj=getParentElementObj(filterBtnObj,'TD');
	if(filterBtnObj.obj==null||((spanwidth==null||spanwidth<=0)&&filterBtnObj.obj.spanOutputWidth!=tdObj.offsetWidth))
	{//没有初始化或显示结果的<span/>的宽度与相应<td/>宽度相同，且初始化后改变过td的宽度
		filterBtnObj.obj=initializeFilter(filterBtnObj,paramsObj,-1);
	}
	filterBtnObj.obj.paramsObj=paramsObj;
	var metadataObj=getReportMetadataObj(paramsObj.reportguid);
	filterBtnObj.obj.metadataObj=metadataObj;
	var pageid=metadataObj.pageid;
   var reportid=metadataObj.reportid;
   var refreshComponentGuid=metadataObj.refreshComponentGuid;
   var slave_reportid=metadataObj.slave_reportid;
   var url=getComponentUrl(pageid,refreshComponentGuid,slave_reportid);
   url=replaceUrlParamValue(url,'REPORTID',reportid);
   url=replaceUrlParamValue(url,'ACTIONTYPE','GetFilterDataList');
   url=replaceUrlParamValue(url,'FILTER_COLPROP',paramsObj.property);
	if(url!=null&&url!='')
	{
		var tmpArray = url.split("?");
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
		 XMLHttpREPORT.sendReq('POST',tmpArray[0],tmpArray[1],buildFilterItems,onGetDataErrorMethod,'');
	}
}

/**
 *xElem：点中的<td/>对象
 *xOutputCount：输入提示显示的结果数量
 */
function initializeFilter(xElem,paramsObj,xOutputCount)
{
	var props={
		 elem:xElem,
		 paramsObj:null,//列过滤时客户端传过来的json参数字符串
		 metadataObj:null,//本报表的元数据对象
		 spanOutputWidth:null,
		 resultItemsXmlRoot:null,//从服务器端获取的所有输入提示选项的XML数据
		 recordCount:0,//过滤选项数量
		 treeNodesArr:null,//当允许多选时，存放所有选项树节点集合
		 //下面变量只对单选列过滤有效
		 spanOutputId:"",//输入提示框的ID
		 currentValueSelected:-1,//当前选中的输入提示项下标
		 prevValueSelected:-1//上一次选中的提示项下标
	};
	if(xElem.id)
	{
		props.spanOutputId="spanOutput_"+xElem.id;//过滤提示框的按钮ID
	}else
	{
	   alert("必须给用于点击的按钮对象分配一id属性");
	}
	//alert(props.spanOutputId);
	
	var tdObj=getParentElementObj(xElem,'TD');
	var spanwidth=paramsObj.filterwidth;
	if(spanwidth!=null&&spanwidth<=0)
	{//没有传入结果窗口的宽度或传入的宽度小于等于0，则显示为与相应<td/>相同宽度
		props.spanOutputWidth=tdObj.offsetWidth;
	}else
	{
		props.spanOutputWidth=spanwidth;
	}
	//alert('props.spanOutputWidth');
	if(paramsObj.multiply&&paramsObj.multiply=='false')
	{//如果当前列过滤只能单选（一般是因为与查询条件存在关联）
		//创建输入前提示所用的span框
		var spanobj=document.getElementById(props.spanOutputId);
		if(spanobj==null)
		{
			//alert('create span');
			var elemSpan=document.createElement("span");
			elemSpan.id=props.spanOutputId;
			elemSpan.className="spanOutputTextDropdown";
			document.body.appendChild(elemSpan);
			spanobj=elemSpan;
		}
	}
	props.paramsObj=paramsObj;
	return props;
}

function setHighColor(xOutputSpanItem)
{
	//debugger;
	if(xOutputSpanItem)
	{//从mouse点击过来
	    var arrayTemp=xOutputSpanItem.id.split("_");
		//alert(arrayTemp.length+"  "+arrayTemp[arrayTemp.length-1]);
		COL_FILTER_btnObj.obj.prevValueSelected=COL_FILTER_btnObj.obj.currentValueSelected;
		COL_FILTER_btnObj.obj.currentValueSelected=arrayTemp[arrayTemp.length-1];
	}
	if(parseInt(COL_FILTER_btnObj.obj.prevValueSelected)>=0)
		document.getElementById(COL_FILTER_btnObj.obj.spanOutputId+"_"+COL_FILTER_btnObj.obj.prevValueSelected).className='spanOutputNormalElement';
	var spanTemp=document.getElementById(COL_FILTER_btnObj.obj.spanOutputId+"_"+COL_FILTER_btnObj.obj.currentValueSelected);
	//alert(TYPE_AHEAD_currentValueSelected);
	if(spanTemp)
	  spanTemp.className='spanOutputHighElement';
}

function onGetDataErrorMethod(xmlHttpObj)
{
	if(WXConfig.load_error_message!=null&&WXConfig.load_error_message!='')
	{
		wx_error(WXConfig.load_error_message);
	}
	//document.getElementById(COL_FILTER_btnObj.obj.spanOutputId).innerHTML="<font color='red'><b>获取数据失败</b></font>";
	COL_FILTER_btnObj.obj.currentValueSelected=-1;
}
/**
 *回调函数
 */
function buildFilterItems(xmlHttpObj)
{
	var tempxml=xmlHttpObj.responseXML;
	COL_FILTER_btnObj.obj.resultItemsXmlRoot=tempxml.getElementsByTagName("items")[0];//取到XML文档的根<items/>
	if(COL_FILTER_btnObj.obj.resultItemsXmlRoot)
		COL_FILTER_btnObj.obj.recordCount=COL_FILTER_btnObj.obj.resultItemsXmlRoot.childNodes.length;
	else
		COL_FILTER_btnObj.obj.recordCount=0;
	buildSelectListBox();
}

function buildSelectListBox()
{
	var pos=getElementAbsolutePosition(getParentElementObj(COL_FILTER_btnObj,'TD'));//取<td/>位置，之所以不放在setOutputPosition()方法中，是为了加快定位的速度，因为定位前已经将结果span显示出来了
	var doc=document;
	if(COL_FILTER_btnObj.obj.paramsObj.multiply=='false')
	{//如果当前列过滤只能单选（一般是因为与查询条件存在关联）
		var matchedResults=makeFilterSelectList();
		//alert(matchedResults);
		if(matchedResults.length>0)
		{
			var spanOutput=doc.getElementById(COL_FILTER_btnObj.obj.spanOutputId);
			spanOutput.innerHTML=matchedResults;
			doc.getElementById(COL_FILTER_btnObj.obj.spanOutputId+"_0").className="spanOutputHighElement";
			COL_FILTER_btnObj.obj.currentValueSelected=0;
			spanOutput.style.display="block";
			setOutputPosition(pos,spanOutput,spanOutput,doc.getElementById(COL_FILTER_btnObj.obj.spanOutputId+"_inner"));
			EventTools.addEventHandler(window.document,"mousedown",handleDocumentMouseDownForSingleColFilter);
		}else
		{
			hideSingleColFilterSelectBox();
		}
	}else
	{//可以多选
		var cnt=COL_FILTER_btnObj.obj.recordCount;
		if(cnt<=0) return;
		var paramsObj=COL_FILTER_btnObj.obj.paramsObj;
		var metadataObj=getReportMetadataObj(paramsObj.reportguid);
		var isLazydisplaydata=metadataObj.metaDataSpanObj.getAttribute('lazydisplaydata')=='true';//本报表是否是延迟加载，如果是的话，则所有数据都不选中
		var webroot=paramsObj.webroot;
		var skin=paramsObj.skin;
		var img_rooturl=webroot+'webresources/skin/'+skin+'/images/coltitle_selected/';//所用到图片的路径
		var treeParams="{img_rooturl:\""+img_rooturl+"\"";
		treeParams=treeParams+",checkbox:\"true\"";//显示复选框
		treeParams=treeParams+",treenodeimg:\"false\"";//不显示节点前面的前导图片
		treeParams=treeParams+",nodes:[";
		var treeNodesArr=new Array();//存放所有树节点
		var treeNodeObjTmp;
		var childNodes=COL_FILTER_btnObj.obj.resultItemsXmlRoot.childNodes;
		var value='';
		var label='';
		for(var i=0;i<cnt;i=i+1)
		{
			var eleItem=childNodes.item(i);
			if(eleItem.childNodes.length<=0) continue;
			//alert(eleItem.childNodes.length);
			value=eleItem.firstChild.childNodes[0].nodeValue;
			var isChecked=eleItem.firstChild.getAttribute('isChecked');//当前节点是否选中
			if(isLazydisplaydata||isChecked==null||isChecked=='') isChecked='false'; 
			if(eleItem.childNodes.length==1)
			{
		   	label=value;
			}else
			{
		   	label=eleItem.lastChild.childNodes[0].nodeValue;
			}
			if(value&&(value=='[nodata]'||value=='[error]'))
			{//没有取到数据，或取数据失败
				treeParams=label;
				break;
			}
			var nodeid='col_filter_'+i;
			treeNodeObjTmp=new Object();
			treeNodeObjTmp['nodeid']=nodeid;//节点id
			treeNodeObjTmp['nodevalue']=value;//节点值
			treeNodesArr[treeNodesArr.length]=treeNodeObjTmp;
			treeParams=treeParams+"{nodeid:\""+nodeid+"\"";
			treeParams=treeParams+",title:\""+label+"\"";
			treeParams=treeParams+",checked:\""+isChecked+"\"";
			treeParams=treeParams+"},";
		}
		if(treeParams.lastIndexOf(',')==treeParams.length-1)
		{
			treeParams=treeParams.substring(0,treeParams.length-1);
		}
		COL_FILTER_btnObj.obj.treeNodesArr=treeNodesArr;
		/**
		 * 显示列过滤选项树
		 */
		var treeStr="<ul class=\"bbit-tree-root bbit-tree-lines\">";
		if(treeParams.indexOf('nodes:[')<0&&treeParams.indexOf('{img_rooturl:')<0)
		{//说明取过滤数据时没取到或出错（即存在[nodata]或[error]）
			treeStr=treeStr+treeParams;//直接显示无记录或出错提示
		}else
		{
			treeParams=treeParams+"]}";
			treeStr=treeStr+showTreeNodes(treeParams);
			//treeStr=treeStr+"<p align='center'><img src=\""+img_rooturl+"submit.gif\" onclick=\"okSelectedColFilter()\"></p>"
		}
		treeStr=treeStr+"</ul>";
		var treeContentDivObj=doc.getElementById('wx_titletree_content');
   	treeContentDivObj.innerHTML=treeStr;
   	var treeContainerObj=doc.getElementById('wx_titletree_container');
   	treeContainerObj.style.display = '';   	
   	doc.getElementById('wx_titletree_buttoncontainer').innerHTML='<img src=\"'+img_rooturl+'submit.gif\" onclick=\"okSelectedColFilter()\">';
   	setOutputPosition(pos,treeContainerObj,doc.getElementById('titletree_container_inner'),treeContentDivObj);
   	EventTools.addEventHandler(window.document,"mousedown",handleDocumentMouseDownForSelectedTree);
	}
}

/**
 *	定位列过滤结果窗口的位置
 * @param pos <td/>单元格位置信息对象
 * @param eleOuterContainerObj 结果输出窗口对象
 */
function setOutputPosition(pos,eleOuterContainerObj,eleInnerContainerObj,eleContentObj)
{
	eleOuterContainerObj.style.width=COL_FILTER_btnObj.obj.spanOutputWidth+'px';
	eleOuterContainerObj.style.top=(pos.top+pos.height)+'px';
	/**
    * 将过滤选项提示span显示与单元格右端对齐，这是因为列过滤图标一般是显示在右侧，所以这样显示更好看
    */
   eleOuterContainerObj.style.left=(pos.left+pos.width-eleOuterContainerObj.offsetWidth)+'px';//这个offsetWidth，必须在显示完内容之后取用
   var maxheight=COL_FILTER_btnObj.obj.paramsObj.filtermaxheight;
  	if(maxheight==null||maxheight<15) maxheight=350;
  	if(eleContentObj.offsetHeight<maxheight-10) 
  	{
  		eleInnerContainerObj.style.height=(eleContentObj.offsetHeight+10)+'px';
  	}else
  	{
  		eleInnerContainerObj.style.height=maxheight+'px';//这里必须设置，因为整个页面都共这一个<div/>对象，可能在其它列过滤中已经设置了一个比较小的值
  	}
}

function okSelectedColFilter()
{
	var treeNodesArr=COL_FILTER_btnObj.obj.treeNodesArr;//得到列选项树节点集合
	var resultStr='';
	if(treeNodesArr&&treeNodesArr.length>0)
	{
		var treeNodeObjTmp;
		var treeNodeDivTmp;
		var selectedCount=0;//选中的记录数
		for(var i=0;i<treeNodesArr.length;i++)
		{
			treeNodeObjTmp=treeNodesArr[i];
			treeNodeDivTmp=document.getElementById(treeNodeObjTmp['nodeid']);//取到此节点对应的树节点的<div/>对象
			var checkedTmp=treeNodeDivTmp.getAttribute('checked');
			if(checkedTmp&&checkedTmp=='true')
			{
				resultStr=resultStr+treeNodeObjTmp['nodevalue']+';;';
				selectedCount++;
			}
		}
		if(selectedCount==treeNodesArr.length)
		{//全部选中
			resultStr='';//则表示不在此列过滤
		}else
		{
			if(resultStr.lastIndexOf(';;')==resultStr.length-2)
			{
				resultStr=resultStr.substring(0,resultStr.length-2);//去掉最后多余的;;号
			}
			if(resultStr=='')
			{//全部没选中
				wx_warn('请选择要过滤的数据');
				return false;
			}
		}
		//alert(resultStr);
		filterReportData(resultStr);//进行过滤操作
	}
	closeSelectedTree();
}

function makeFilterSelectList()
{
	var matchArray=new Array();
	if(!COL_FILTER_btnObj.obj.resultItemsXmlRoot) return '';
	var resultstr='';
	var value='';
	var label='';
	var cnt=COL_FILTER_btnObj.obj.recordCount;
	if(cnt<=0) return '';
	var childNodes=COL_FILTER_btnObj.obj.resultItemsXmlRoot.childNodes;
	for(var i=0;i<cnt;i=i+1)
	{
		var eleItem=childNodes.item(i);
		if(eleItem.childNodes.length<=0)
		{
			COL_FILTER_btnObj.obj.recordCount--;
		 	continue;
		}
		value=eleItem.firstChild.childNodes[0].nodeValue;
		if(eleItem.childNodes.length==1)
		{
		   label=value;
		}else
		{
		   label=eleItem.lastChild.childNodes[0].nodeValue;
		}
		var selectSpanMid=" id='"+COL_FILTER_btnObj.obj.spanOutputId+"_"+i+"'";
		if(value&&value!=''&&value!='[nodata]'&&value!='[error]')
		{
			selectSpanMid=selectSpanMid+" onmousedown=\"filterReportData('"+value+"')\"";
		}
		selectSpanMid=selectSpanMid+">"+label;
		resultstr+=COL_FILTER_selectSpanStart+selectSpanMid+COL_FILTER_selectSpanEnd;//+"<br/>";
	}
	if(COL_FILTER_btnObj.obj.recordCount<=0) return '';
	resultstr="<div id='"+COL_FILTER_btnObj.obj.spanOutputId+"_inner'>"+resultstr+"</div>";//外面再括一层<div/>，方便后面控制显示窗口大小（一定要用<div/>，不能用<span/>）
	//alert(resultstr);
	return resultstr;
}
/**
 * 根据用户点击的过滤项进行报表数据过滤
 */
function filterReportData(value)
{
	var metaDataObj=COL_FILTER_btnObj.obj.metadataObj;	
   var url=getComponentUrl(metaDataObj.pageid,metaDataObj.refreshComponentGuid,metaDataObj.slave_reportid);
	var urlparamname=COL_FILTER_btnObj.obj.paramsObj.urlParamName;
	url=replaceUrlParamValue(url,urlparamname,value);
	if(COL_FILTER_btnObj.obj.paramsObj.multiply!='false')
	{//如果当前列过滤是多选（即与查询条件不存在关联）
		url=removeReportNavigateInfosFromUrl(url,metaDataObj,1);//只删除本报表的翻页导航ID
		url=replaceUrlParamValue(url,metaDataObj.reportid+'_COL_FILTERID',urlparamname);
	}else
	{//与查询条件关联
		url=removeReportNavigateInfosFromUrl(url,metaDataObj,null);//删除掉本报表的翻页导航ID和所有与此报表存在查询条件关联的分页报表翻页导航ID
		hideSingleColFilterSelectBox();
	}
	url=removeLazyLoadParamsFromUrl(url,metaDataObj,null);
	refreshComponent(url);
}

/**
 * 处理当前是单选列过滤时，单击页面的处理事件，是隐藏还是继续显示列选择框
 */
function handleDocumentMouseDownForSingleColFilter(event)
{
	var srcObj=window.event?window.event.srcElement:event.target;
	if(srcObj==null)
	{
		hideSingleColFilterSelectBox();
	}else
	{
		while(srcObj!=null)
		{
			try
			{
				if(srcObj.getAttribute('id')==COL_FILTER_btnObj.obj.spanOutputId) return;//当前点击的是过滤框或其子元素，则不隐藏
				srcObj=srcObj.parentNode;
			}catch(e)
			{
				break;
			}
		}
		hideSingleColFilterSelectBox();
	}
}

/**
 * 隐藏单选列过滤选择框
 */
function hideSingleColFilterSelectBox()
{
	if(COL_FILTER_btnObj!=null)
	{
		document.getElementById(COL_FILTER_btnObj.obj.spanOutputId).style.display='none';
		COL_FILTER_btnObj.obj.currentValueSelected=-1;
	}
	EventTools.removeEventHandler(window.document,"mousedown",handleDocumentMouseDownForSingleColFilter);
}

 /**
  *
  * 创建某个报表的标题选择树
  * @param obj 被点击的对象，比如“下载Excel”，这个对象是方便定位列选择框显示位置
  * @param params  参数格式分两种：
  *				{skin:skin,	//皮肤
  *				 webroot:webroot,//WEB根URL
  *				 width:width 窗口宽度
  *			    reportguid:reportguid,当前报表的guid
  *             //下面两个参数只有选择导出数据的列时需要，选择页面上的显示列时不需要
  *				 showreport_onpage_url:showreport_onpage_url,显示报表到页面时的URL前缀    
  *				 showreport_dataexport_url:showreport_dataexport_url,导出报表的URL前缀，根据导出类型，分别有不同的前缀  这两个参数用来将页面URL替换成导出数据的URL
  *				 
  */
var WX_colSeletedParamsObj=null;//存放每个报表列选择时在“确定”后要使用的参数
function createTreeObjHtml(obj,params,e)
{
	var doc=document;
	var paramsObj=getObjectByJsonString(params);
	var skin=paramsObj.skin;
	var webroot=paramsObj.webroot;
	var reportguid=paramsObj.reportguid;
	var metadataObj=getReportMetadataObj(reportguid);//取到元数据对象
	var newParamsObj=
	{
		metadataObj:metadataObj,
		paramsObj:paramsObj
	}
	if(WX_colSeletedParamsObj==null) WX_colSeletedParamsObj=new Object();
	WX_colSeletedParamsObj[reportguid]=newParamsObj;//存放在这里供okSelected()方法使用
	/*var showreport_onpage_url=paramsObj.showreport_onpage_url;
	var showreport_dataexport_url=paramsObj.showreport_dataexport_url;
	var isSelectedForPage=false;//当前是选择显示在页面上的列
	if(showreport_onpage_url==null||showreport_dataexport_url==null||showreport_dataexport_url==''||showreport_onpage_url==showreport_dataexport_url)
		isSelectedForPage=true;*/
	var showreport_onpage_url=paramsObj.showreport_onpage_url;
	var showreport_dataexport_url=paramsObj.showreport_dataexport_url;
	var rootObj=null;
	if(showreport_onpage_url==null||showreport_dataexport_url==null||showreport_dataexport_url==''||showreport_onpage_url==showreport_dataexport_url)
	{//当前是在为刷新页面而动态选择列
		rootObj=doc.getElementById(reportguid+'_page_col_titlelist');
	}else
	{//当前是选择导出文件的列
		rootObj=doc.getElementById(reportguid+'_dataexport_col_titlelist');
	}
	if(rootObj==null) return;
	var nodesArr=rootObj.getElementsByTagName('ITEM');
	var nodeItemObj;
	var title;
	var id;
	var parentgroupid;
	var childids;
	var layer;
	var checked;
	var isControlCol;
	var isalway;
	var img_rooturl=webroot+'webresources/skin/'+skin+'/images/coltitle_selected/';//所用到图片的路径
	var treeParams="{img_rooturl:\""+img_rooturl+"\"";
	treeParams=treeParams+",checkbox:\"true\"";//显示复选框
	treeParams=treeParams+",treenodeimg:\"true\"";//显示节点前面的前导图片
	treeParams=treeParams+',nodes:[';
	var parentidsMap='';//存放每个节点的parentid，这样可以在后面根据每个节点的id很容易找到其父节点id
	for(var i=0,len=nodesArr.length;i<len;i=i+1)
	{
		nodeItemObj=nodesArr[i];
		id=nodeItemObj.getAttribute('nodeid');
		treeParams=treeParams+"{nodeid:\""+id+"\"";
		treeParams=treeParams+",title:\""+nodeItemObj.getAttribute('title')+"\"";
		parentgroupid=nodeItemObj.getAttribute('parentgroupid');
		if(parentgroupid!=null&&parentgroupid!='')
		{
			treeParams=treeParams+",parentgroupid:\""+parentgroupid+"\"";
			parentidsMap=parentidsMap+id+":\""+parentgroupid+"\",";//记下当前节点的父节点id，以便显示树时使用
		} 
		childids=nodeItemObj.getAttribute('childids');
		if(childids&&childids!='')
		{
			treeParams=treeParams+",childids:\""+childids+"\"";
		}
		layer=parseInt(nodeItemObj.getAttribute('layer'),10);
		if(!layer) layer=0;
		treeParams=treeParams+",layer:\""+layer+"\"";
		checked=nodeItemObj.getAttribute('checked');
		if(!checked) checked='false';
		treeParams=treeParams+",checked:\""+checked+"\"";
		isControlCol=nodeItemObj.getAttribute('isControlCol');
		if(isControlCol=='true')
		{//本列是控制列，比如是行选中的列，则不出现在选择列表中
			isalway='hidden';
		}else
		{
			isalway=nodeItemObj.getAttribute('always');
			if(isalway==null) isalway='false';
		}
		treeParams=treeParams+",isalway:\""+isalway+"\"";
		treeParams=treeParams+"},";
	}
	if(treeParams.lastIndexOf(',')==treeParams.length-1)
	{
		treeParams=treeParams.substring(0,treeParams.length-1);
	}
	treeParams=treeParams+']';
	if(parentidsMap.lastIndexOf(',')==parentidsMap.length-1)
	{
		parentidsMap=parentidsMap.substring(0,parentidsMap.length-1);
	}
	treeParams=treeParams+",parentidsMap:{"+parentidsMap+"}";
	treeParams=treeParams+'}';
	var treeStr="<ul class=\"bbit-tree-root bbit-tree-lines\">";
	treeStr=treeStr+showTreeNodes(treeParams);
	//treeStr=treeStr+"<p align='center'><img src=\""+img_rooturl+"submit.gif\" onclick=\"okSelected('"+reportguid+"')\"></p>";
	treeStr=treeStr+"</ul>";
	var treeContentDivObj=doc.getElementById('wx_titletree_content');
	treeContentDivObj.innerHTML=treeStr;
   doc.getElementById('wx_titletree_buttoncontainer').innerHTML="<img src=\""+img_rooturl+"submit.gif\" onclick=\"okSelected('"+reportguid+"')\">";
	var pos=getElementAbsolutePosition(obj);
	var treeContainerObj=doc.getElementById('wx_titletree_container');
   if(paramsObj.width!=null&&paramsObj.width>0)
   {
   	treeContainerObj.style.width=paramsObj.width+'px';
   }
   treeContainerObj.style.display = '';
   var maxheight=paramsObj.maxheight;
   if(maxheight==null||maxheight<15) maxheight=350;
   //alert(treeContentDivObj.offsetHeight);
   if(treeContentDivObj.offsetHeight<maxheight-10) 
   {
   	doc.getElementById('titletree_container_inner').style.height=(treeContentDivObj.offsetHeight+10)+'px';
   }else
   {
   	doc.getElementById('titletree_container_inner').style.height=maxheight+'px';//这里必须设置，因为整个页面都共这一个<div/>对象，可能在其它报表的列选择中已经设置了一个比较小的值
   }
   //alert(treeContentDivObj.offsetHeight);
   var event = e || window.event;
   var documentSize=getDocumentSize();
	var rightedge = documentSize.width - event.clientX;
	var bottomedge =documentSize.height - event.clientY;
   if(rightedge<treeContainerObj.offsetWidth)
   {//显示在被点击对象的左边
   	treeContainerObj.style.left =(pos.left-treeContainerObj.offsetWidth)+'px';
   }else
   {//显示在其右边
   	treeContainerObj.style.left =(pos.left+pos.width)+'px';
   }
   if(bottomedge<treeContainerObj.offsetHeight&&event.clientY>treeContainerObj.offsetHeight)
   {//如果被点击对象的底部边距小于提示框的高度，且顶部边距大于它的高度，则向上显示提示窗口
   	treeContainerObj.style.top = (pos.top-treeContainerObj.offsetHeight+pos.height)+'px'; 
   }else
   {
   	treeContainerObj.style.top = (pos.top)+'px'; 
   }
   EventTools.addEventHandler(window.document,"mousedown",handleDocumentMouseDownForSelectedTree);
}

/**
 *	获取所有选中节点的值
 * 
 */	
function okSelected(reportguid)
{
	var doc=document;
	var paramsObj=WX_colSeletedParamsObj[reportguid];
	var resultStr='';
	var checkedTmp;
	var treenode;
	var nodeid;
	var hasSelectedNonFixedCol=false;//本次选择是否选中了非冻结列
	var showreport_onpage_url=paramsObj.paramsObj.showreport_onpage_url;
	var showreport_dataexport_url=paramsObj.paramsObj.showreport_dataexport_url;
	var isColSelectForPage=showreport_onpage_url==null||showreport_dataexport_url==null||showreport_dataexport_url==''||showreport_onpage_url==showreport_dataexport_url;
	var rootObj=null;
	if(isColSelectForPage)
	{//当前是在为刷新页面而动态选择列
		rootObj=doc.getElementById(reportguid+'_page_col_titlelist');
	}else
	{//当前是选择导出文件的列
		rootObj=doc.getElementById(reportguid+'_dataexport_col_titlelist');
	}
	var nodesArr=rootObj.getElementsByTagName('ITEM');
	for(var i=0,len=nodesArr.length;i<len;i++)
	{
		nodeItemObj=nodesArr[i];
		nodeid=nodeItemObj.getAttribute('nodeid');
		treenode=doc.getElementById(nodeid);//取到此节点对应的树节点的<div/>对象
		if(treenode==null) continue;
		checkedTmp=treenode.getAttribute('checked');
		if(checkedTmp&&checkedTmp=='true')
		{
			resultStr=resultStr+nodeid+';';
			if(!hasSelectedNonFixedCol&&nodeItemObj.getAttribute('isNonFixedCol')=='true')
			{//还没有选中非冻结列，且当前列是非冻结列
				hasSelectedNonFixedCol=true;
			}
		}
	}
	if(!hasSelectedNonFixedCol)
	{
		wx_warn('至少选中一个非冻结数据列');
		return false;
	}
	if(resultStr.lastIndexOf(';')==resultStr.length-1)
	{
		resultStr=resultStr.substring(0,resultStr.length-1);//去掉最后多余的;号
	}
	if(resultStr=='')
	{
		wx_warn('请选择要显示/下载的列');
		return false;
	}
	var metadataObj=paramsObj.metadataObj;
	var url=getComponentUrl(metadataObj.pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
	url=replaceUrlParamValue(url,metadataObj.reportid+'_DYNDISPLAY_COLIDS',resultStr);
	url=replaceUrlParamValue(url,metadataObj.reportid+'_DYNDISPLAY_COLIDS_ACTION','true');//标识本次是在做列选择操作
	//url=addLazyLoadParamsToUrl(metadataObj,url);
	//alert(url);
	if(isColSelectForPage)
	{//当前是在为刷新页面而动态选择列
		refreshComponent(url,null,{keepSelectedRowsAction:true,keepSavingRowsAction:false});//不保存动态数据，因为列选择后列数不同恢复编辑行会导致显示混乱
	}else
	{//当前是为下载Excel而动态选择列
		url=addSelectedRowDataToUrl(metadataObj.pageid,metadataObj.reportid,url);
		exportData(metadataObj.pageid,metadataObj.reportid,paramsObj.paramsObj.includeApplicationids,showreport_onpage_url,showreport_dataexport_url,url);
		//window.location.href=url;
	}
	closeSelectedTree();
}

var WX_rootTreeNodeId='root_treenode_id';//树根节点的ID
/**
 * 显示所有树结点
 * @params 参数结构:
 *		{img_rooturl:img_rooturl, //树节点所需图片存放路径
 *		 checkbox:true/false,//是否需要显示复选框
 *		 treenodeimg:true/false,是否需要显示树节点的前导图片（即树枝节点和树叶节点的标识图片）
 *		 nodes[{nodeid:nodeid,title:title,layer:layer,....},{},{}]存放所有节点
 *		 parentids:{id:parentid,....}//存放每个节点的父节点
 */
function showTreeNodes(params)
{
	//alert(params);
	var paramsObj=eval("("+params+")");
	var img_rooturl=paramsObj.img_rooturl;
	var nodes=paramsObj.nodes;
	//alert(params);
	if(!nodes||nodes.length==0) return '';
	var parentidsMap=paramsObj.parentidsMap;//每个节点的父节点
	var nodeItemObj;
	var str='';
	var hasAlwaysChild=false;//是否有永远选中的树节点，方便后面显示根节点的状态
	var hasCheckedChild=false;//是否有选中树节点
	var rootChilds='';
	var endGroupNodesObj=new Object();//存放当前已经显示完的树枝节点
	var showCheckBox=paramsObj.checkbox;//是否需要显示复选框
	var showTreeNodeImg=paramsObj.treenodeimg;//是否需要显示树节点前导图片
	for(var i=0;i<nodes.length;i=i+1)
	{
		nodeItemObj=nodes[i];
		id=nodeItemObj.nodeid;
		title=nodeItemObj.title;
		parentgroupid=nodeItemObj.parentgroupid;
		if(!parentgroupid||parentgroupid==''||parentgroupid==WX_rootTreeNodeId) 
		{//没有父层节点，或父节点ID为WX_rootTreeNodeId，则说明当前节点是第一级节点，即顶级节点的子节点
			parentgroupid=WX_rootTreeNodeId;//根节点id
			rootChilds=rootChilds+id+',';//这个节点是根节点的子节点
		}
		childids=nodeItemObj.childids;
		if(!childids) childids='';
		layer=parseInt(nodeItemObj.layer);
		if(!layer) layer=0;
		checked=nodeItemObj.checked;
		if(!checked) checked='false';
		if(checked=='true') hasCheckedChild=true;
		isalway=nodeItemObj.isalway;
		if(isalway=='hidden') continue;//如果是永远不显示，则不显示它
		if(isalway==null) isalway='false';
		if(isalway=='true') hasAlwaysChild=true;
		str=str+"<div ";
		///str=str+"\"";
		str=str+" title=\""+title+"\" id=\""+id+"\" parentgroupid=\""+parentgroupid+"\" childids=\""+childids+"\" layer=\""+layer+"\" always=\""+isalway+"\" checked=\""+checked+"\">";//显示结点上的数据
		var imgname='';
		/**
		 * 如果当前节点的层级数大于2，则显示前面的竖线用于分层
		 * 显示时，从父父节点那一层开始显示
		 */
		var layerLine='';
		if(parentidsMap&&parentgroupid!=WX_rootTreeNodeId)
		{//父节点不是顶层节点，因为要显示竖线必须从父父节点开始
			var parentid=parentidsMap[parentgroupid];
			while(true)
			{
				if(!parentid||parentid=='') parentid=WX_rootTreeNodeId;
				var isEnd=endGroupNodesObj[parentid];
				if(!isEnd||isEnd!='true')
				{//这一层父节点还没有结束，即当前节点不是或不属于它的最后一个子节点。
					imgname="elbow-line.gif";//显示一根竖线
				}else
				{//当前节点是或属于这层父节点的最后一个子节点，则不显示竖线。
					imgname="elbow-line-none.gif";
				}
				layerLine="<img style='vertical-align:middle;' src=\""+img_rooturl+imgname+"\">"+layerLine;
				//alert(parentid);
				if(parentid==WX_rootTreeNodeId) break;
				parentid=parentidsMap[parentid];
			}
		}
		str=str+layerLine;
		str=str+"<img ";//显示前导虚线图片
		if(isLastNodeInThisLayer(parentgroupid,i,nodes))
		{//是当前分组的最后一个节点
			imgname='elbow-end.gif';
			endGroupNodesObj[parentgroupid]='true';//记录它对应的父树枝节点已经显示完了
		}else
		{//中间节点
			imgname='elbow.gif';
		}
		str=str+" style='vertical-align:middle;' src=\""+img_rooturl+imgname+"\">";
		if(showTreeNodeImg&&showTreeNodeImg=='true')
		{//要显示树节点前面的前导图片
			if(childids&&childids!='')
			{//有子节点，说明当前节点是树枝节点
				imgname='folder-open.gif';
			}else
			{//没有子节点，说明是树叶节点
				imgname='leaf.gif';
			}
			str=str+"<img  style='vertical-align:middle;' src=\""+img_rooturl+imgname+"\">";//显示前导树枝/树叶节点图片
		}
		if(showCheckBox&&showCheckBox=='true')
		{//需要显示复选框
			if(isalway&&isalway=='true')
			{//必须永远显示，不能取消选中
				imgname="checkbox_3.gif";//选中的复选框
			}else if(checked&&checked=='true')
			{
				imgname="checkbox_1.gif";//选中的复选框
			}else
			{
				imgname="checkbox_0.gif";//没选中的复选框
			}
			str=str+"<img id=\""+id+"_cb\" style='vertical-align:middle;'";
			if(!isalway||isalway=='false')
			{
				str=str+" onclick=\"processTreeNodeselected(this.id,'"+img_rooturl+"')\"";
			}
			str=str+" src=\""+img_rooturl+imgname+"\">";//显示复选框图片！！！注意：这里只能传入this.id，不能传入this，否则读写checked属性不成功！！！
		}
		str=str+title;//显示标题
		str=str+"</div>";
	}
	/**
	 *
	 * 显示树根节点
	 */
	if(rootChilds.lastIndexOf(',')==rootChilds.length-1)
	{
		rootChilds=rootChilds.substring(0,rootChilds.length-1);
	}
	var rootstr="<div id=\""+WX_rootTreeNodeId+"\" childids=\""+rootChilds+"\" always=\""+hasAlwaysChild+"\" checked=\""+hasCheckedChild+"\"><img style='vertical-align:middle;' src=\""+img_rooturl+"root.gif\">";//根节点
	if(showCheckBox&&showCheckBox=='true')
	{//需要显示复选框
		var rootnodecheckimg='';
		if(hasAlwaysChild==true)
		{
			rootnodecheckimg="checkbox_3.gif";//选中的复选框
		}else if(hasCheckedChild==true)
		{
			rootnodecheckimg="checkbox_1.gif";//选中的复选框
		}else
		{//没有一个树节点是选中状态
			rootnodecheckimg="checkbox_0.gif";//没选中的复选框
		}
		rootstr=rootstr+"<img id=\""+WX_rootTreeNodeId+"_cb\" style='vertical-align:middle;'";
		if(hasAlwaysChild!=true)
		{
			rootstr=rootstr+" onclick=\"processTreeNodeselected(this.id,'"+img_rooturl+"')\"";
		}
		rootstr=rootstr+" src=\""+img_rooturl+rootnodecheckimg+"\">";//显示复选框图片
	}
	rootstr=rootstr+"</div>";
	str=rootstr+str;
	return str;
}

/**
 * 打开选择树后，处理document上的点击事件
 */
function handleDocumentMouseDownForSelectedTree(event)
{
	var srcObj=window.event?window.event.srcElement:event.target;
	if(srcObj==null||!isElementOrChildElement(srcObj,'wx_titletree_container'))
	{
		closeSelectedTree();
	}
}

/**
 * 隐藏树
 */	
function closeSelectedTree()
{
	document.getElementById('wx_titletree_container').style.display='none';
	EventTools.removeEventHandler(window.document,"mousedown",handleDocumentMouseDownForSelectedTree);
}

/**
 * 判断当前节点是否是其父分组节点中最后一个节点
 * @param parentgroupid 当前节点的父节点ID
 * @param nodeidx 当前节点在所有节点中的下标
 * @param nodesArr 存放所有节点对象
 */
function isLastNodeInThisLayer(parentgroupid,nodeidx,nodes)
{
	if(nodeidx==nodes.length-1) return true;//最后一个节点
	if(!parentgroupid) parentgroupid='';
	var nextNodeParentgroupid;
	for(var i=nodeidx+1;i<nodes.length;i++)
	{//从当前节点的下一个节点开始循环，看一下有没有同一层的节点
		nextNodeParentgroupid=nodes[i].parentgroupid;
		var isalway=nodes[i].isalway;
		if(isalway=='hidden') continue;//如果是永远不显示，比如是控制列
		if(!nextNodeParentgroupid||nextNodeParentgroupid=='') nextNodeParentgroupid=WX_rootTreeNodeId;
		if(nextNodeParentgroupid!=parentgroupid) continue;
		return false;//找到了与当前节点同一个父节点的节点，说明当前节点不是此分组的最后一个节点
	}
	return true;
}

/**
 *
 * 点击某个树节点复选框时，处理选中/取消选中逻辑
 * @param imgid 注意：这里只能传入id，不能传入复选框对象，否则读写checked属性失败
 */
function processTreeNodeselected(imgid,img_rooturl)
{
	var chkImgObj=document.getElementById(imgid);
	//var parentDivObj=chkImgObj.parentNode;//取到父<div/>节点
	var parentDivObj=document.getElementById(imgid.substring(0,imgid.indexOf('_cb')));
	var newstate;//点击当前节点前状态
	var checked=parentDivObj.getAttribute('checked');
	//alert(imgid+' '+checked)
	if(checked&&checked=='true')
	{//如果当前节点是选中的，则让它不选中
		newstate=false;
		chkImgObj.src=img_rooturl+"checkbox_0.gif";
		parentDivObj.setAttribute('checked','false');
	}else
	{//如果当前节点是不选中的，则让它选中
		newstate=true;
		chkImgObj.src=img_rooturl+"checkbox_1.gif";
		parentDivObj.setAttribute('checked','true');
	}
	//alert(parentDivObj.getAttribute('checked')+'  '+document.getElementById('4').getAttribute('checked'));
	processChildNodesSelected(parentDivObj,newstate,img_rooturl);//处理子节点的选中情况
	//alert(document.getElementById('4').getAttribute('checked'));
	processParentNodesSelected(parentDivObj,newstate,img_rooturl);//处理父节点的选中情况
	//alert(document.getElementById('4').getAttribute('checked'));
}
	
/**
 * 当选中/取消选中某个树枝节点时，处理它的所有子节点。
 * @param parentDivObj
 * @param newstate 父节点点击后新的选中状态，true：选中；false：不选中
 */
function processChildNodesSelected(parentDivObj,newstate,img_rooturl)
{
	var childids=parentDivObj.getAttribute('childids');//取出当前所有子节点ID
	if(childids&&childids!='') 
	{
		var childArr=childids.split(',');
		var childDivObj;
		var childImgObj;
		for(var i=0;i<childArr.length;i++)
		{
			if(childArr[i]=='') continue;
			childDivObj=document.getElementById(childArr[i]);//取到当前子节点的<div/>对象
			if(!childDivObj) continue;
			childImgObj=document.getElementById(childArr[i]+'_cb');//取到当前子节点对应的复选框的<img/>对象
			if(!childImgObj) continue;
			if(newstate)
			{//如果父节点新状态是选中的，则让它选中
				childImgObj.src=img_rooturl+"checkbox_1.gif";
				childDivObj.setAttribute('checked','true');//不能放在上面用childDivObj.setAttribute('checked',newstate);设置值，否则在IE中无效，因为newstate是boolean类型，IE会区分类型，只能显式地设置字符串类型的true和false
			}else
			{//如果父节点新状态是选中的，则让它不选中
				childImgObj.src=img_rooturl+"checkbox_0.gif";
				childDivObj.setAttribute('checked','false');
			}
			processChildNodesSelected(childDivObj,newstate,img_rooturl);
		}
	}
}

/**
 * 当点击某个节点上的复选框后，处理其父节点的选中情况
 *	
 */	
function processParentNodesSelected(parentDivObj,newstate,img_rooturl)
{
	var parentgroupid=parentDivObj.getAttribute('parentgroupid');
	if(parentgroupid&&parentgroupid!='')
	{
		var parentNodeDivObj=document.getElementById(parentgroupid);
		if(!parentNodeDivObj) return;
		var isAlway=parentNodeDivObj.getAttribute('always');
		if(isAlway&&isAlway=='true')
		{//当前父节点不允许取消选中
			return;
		}
		if(newstate)
		{//如果子节点新状态是选中，则将父节点选中即可
			parentNodeDivObj.setAttribute('checked','true');
			var parentNodeImgObj=document.getElementById(parentgroupid+'_cb');
			parentNodeImgObj.src=img_rooturl+"checkbox_1.gif";
		}else
		{//子节点新状态是不选中（说明老状态是选中状态，所以父节点老状态一定是选中状态，下面就要判断是否要将父节点设为不选中状态）
			var childids=parentNodeDivObj.getAttribute('childids');//取出父节点的所有子节点ID
			var childArr=childids.split(',');
			var childDivObj;
			var hasCheckedChild=false;
			var checkedTmp;
			for(var i=0;i<childArr.length;i++)
			{
				if(childArr[i]=='') continue;
				childDivObj=document.getElementById(childArr[i]);//取到当前子节点的<div/>对象
				if(!childDivObj) continue;
				checkedTmp=childDivObj.getAttribute('checked');
				//alert(childArr[i]+'  '+checkedTmp+' '+childDivObj.getAttribute('title'));
				if(checkedTmp&&checkedTmp=='true')
				{//此父节点有一个子节点是选中状态
					hasCheckedChild=true;
					break;
				}
			}
			//alert(hasCheckedChild);
			if(!hasCheckedChild)
			{//父节点已经没有一个子节点是选中状态
				parentNodeDivObj.setAttribute('checked','false');
				var parentNodeImgObj=document.getElementById(parentgroupid+'_cb');
				parentNodeImgObj.src=img_rooturl+"checkbox_0.gif";
			}
		}
		processParentNodesSelected(parentNodeDivObj,newstate,img_rooturl);
	}
}


/**
 * 展开或收缩树形表格的树枝节点
 * @param webroot 当前应用的根URL，以便显示+或-号图片
 * @param skin 皮肤类型
 * @param imgobj 当前点击的树枝节点对应的图片对象
 * @param tridPrex 当前报表所有<tr/>的id的前缀
 * @param verticalscrollid 如果当前报表显示了表头固定的垂直滚动条，此参数为滚动条ID
 */
function expandOrCloseTreeNode(webroot,skin,imgobj,tridPrex,verticalscrollid)
{
	var trObj=getTrObjOfTreeGroupRow(imgobj);
	var tridSuffixes;//当前树枝节点包括的所有子树枝节点和树叶节点的id后缀，以分号分隔
	var trdataidSuffixes=trObj.getAttribute('childDataIdSuffixes');//所有数据子节点后缀
	var trgroupidSuffixes=trObj.getAttribute('childGroupIdSuffixes');//所有分组子节点后缀
	if(!trdataidSuffixes||trdataidSuffixes=='')
	{
		tridSuffixes=trgroupidSuffixes;
	}else if(!trgroupidSuffixes||trgroupidSuffixes=='')
	{
		tridSuffixes=trdataidSuffixes;
	}else
	{
		tridSuffixes=trgroupidSuffixes+';'+trdataidSuffixes;
	}
	if(tridSuffixes==null||tridSuffixes=='') return false;
	var state=trObj.getAttribute('state');//当前点击的树枝节点的状态
	if(!state||state=='') state='open';
	var stateimgObj=document.getElementById(trObj.getAttribute('id')+'_img');
	if(state=='open') 
	{
		trObj.setAttribute('state','close');//如果当前是展开状态，则设置为收缩状态
		stateimgObj.src=webroot+'webresources/skin/'+skin+'/images/nodeclosed.gif';
	}
	else
	{
	 	trObj.setAttribute('state','open');//如果当前是收缩状态，则设置为展开状态
	 	stateimgObj.src=webroot+'webresources/skin/'+skin+'/images/nodeopen.gif';
	}
	var mHasClosedParentnode=new Object();//缓存当前点击操作所牵涉到的每个树枝节点及其对应的所有父节点是否有关闭状态。这样可以减少下面的重复递归判断的次数。
	var tridSuffixArr=tridSuffixes.split(';');
	for(var i=0;i<tridSuffixArr.length;i=i+1)
	{//处理当前树枝节点的所有子节点的显示状态
		if(tridSuffixArr[i]=='') continue;
		var trObjTmp=document.getElementById(tridPrex+tridSuffixArr[i]);
		//alert(tridPrex+tridSuffixArr[i]);
		if(!trObjTmp) continue;
		var hasClosedParent=isExistParentStateClosed(tridPrex,trObjTmp,mHasClosedParentnode);//判断当前子节点所在的所有层级父节点是否有一个为收缩状态
		if(!hasClosedParent)
		{//如果没有一个为收缩状态，即全部为展开状态，则显示当前子节点
			trObjTmp.style.display='';
		}else
		{//有任意一层的父节点为收缩状态，则将当前子节点不显示
			trObjTmp.style.display='none';
		}
	}
	if(verticalscrollid&&verticalscrollid!='')
	{
		try
		{
			var vertical_scrolldivobj=document.getElementById(verticalscrollid);
			if(vertical_scrolldivobj) vertical_scrolldivobj.fleXcroll.updateScrollBars();
		}catch(e){}
	}
}

/**
 * 判断某个树节点中存不存在状态为close的父节点
 */
function isExistParentStateClosed(tridPrex,trObj,mHasClosedParentnode)
{
	var parentTridSuffix=trObj.getAttribute('parentTridSuffix');
	if(!parentTridSuffix||parentTridSuffix=='')
	{//说明当前是顶层分组，则表示没有找到为收缩状态的父分组节点对象，全部为展开状态
		return false;
	}
	//alert(parentTridSuffix);
	if(mHasClosedParentnode[parentTridSuffix])
	{//已经判断过此父节点及其所有层级的父节点是否有close状态
		//alert(mHasClosedParentnode[parentTridSuffix]=='1');
		return mHasClosedParentnode[parentTridSuffix]=='1';
	}
	var parentTrObj=document.getElementById(tridPrex+parentTridSuffix);
	var stateParent=parentTrObj.getAttribute('state');//判断当前父节点对象的状态
	if(stateParent&&stateParent=='close')
	{//当前父节点对象本身就是收缩状态 
		mHasClosedParentnode[parentTridSuffix]='1';
		return true;//找到了一个为收缩状态的父节点对象
	}
	var flag= isExistParentStateClosed(tridPrex,parentTrObj,mHasClosedParentnode);
	if(flag)
	{//当前节点的父节点存在close状态的父节点，则把这个信息保存下来，以便稍后判断同一层级的节点时直接用上
		mHasClosedParentnode[parentTridSuffix]='1';
	}else 
	{
		mHasClosedParentnode[parentTridSuffix]='0';
	}
	return mHasClosedParentnode[parentTridSuffix]=='1';
}
/*
 *	根据树形分组报表中某树枝节点内部的某个元素获取它所在的<tr/>对象
 *  比如在点击折叠图片时，根据被点击的折叠图片对象获取到相应树枝节点所在<tr/>的对象
 */
function getTrObjOfTreeGroupRow(obj)
{
	var parentObj=obj.parentNode;
	if(!parentObj) return null;
	if(parentObj.tagName=='TR')
	{
		var id=parentObj.getAttribute('id');
		if(id&&id.indexOf('trgroup_')>0)
		{//是树枝节点的<tr/>对象
			return parentObj;
		}
	} 
	return getTrObjOfTreeGroupRow(parentObj);
}

/**
 * 导出数据
 * @param pageid：页面id
 * @param reportids：本次导出的所有报表ID组合，如果有多个，用;号分隔
 * @param showreport_onpage_baseurl：显示报表到页面时配置的URL
 * @param showreport_dataexport_baseurl 导出为数据到当前类型文件的配置的URL
 * @param url 客户端已经取好的URL，此时在此方法中不用再取URL，比如列选择后再导出数据的情况，此时的URL在列选择时就已经构造好，不用再在这里构造，否则不会包括当前选择的列的信息
 * @param shouldRemoveColSelectIds:如果导出数据时不要列选择功能，则是否所有列配置的显示模式在页面和导出文件中完全一致，如果完全一致，则导出页面中当前正在显示的列，此时保留URL中的reportid_DYNDISPLAY_COLIDS参数，否则删除掉，以便重新获取导出文件时各列的显示信息组织显示
 */
function exportData(pageid,componentids,includeApplicationids,showreport_onpage_baseurl,showreport_dataexport_baseurl,url,shouldRemoveColSelectIds)
{
	if(includeApplicationids==null||includeApplicationids=='') return;
	if(url==null||url=='')
	{//客户端没有传入URL
		var appidsArr=new Array();
		var appidsArrTmp=includeApplicationids.split(';');
		var mObj=new Object();
		for(var i=0,len=appidsArrTmp.length;i<len;i++)
		{
			if(appidsArrTmp[i]!=null&&appidsArrTmp[i]!=''&&mObj[appidsArrTmp]!='true')
			{
				mObj[appidsArrTmp[i]]='true';
			 	appidsArr[appidsArr.length]=appidsArrTmp[i];
			}
		}
		if(appidsArr!=null&&appidsArr.length==1)
		{//当前是导出一个报表的数据，则取出此报表的URL（之所以不取页面级URL，是因为如果此报表是从报表，则它有单独的URL，里面包括它自己的查询条件，所以不能取页面级URL）
			var appguid=getComponentGuidById(pageid,appidsArr[0]);
			var metadataObj=getReportMetadataObj(appguid);
			if(metadataObj==null)
			{//此组件不是报表，或是没有参与本次显示的报表，则取出页面级的URL做为此报表的导出URL，之所以不重新构造，是因为可能此报表用到了URL中的查询条件（条件关联所致）
				url=getComponentUrl(pageid,null,null);
			}else
			{
				url=getComponentUrl(pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
			}
			if(shouldRemoveColSelectIds===true)
			{//如果要删除掉URL中选择列信息（数据导出不需要列选择功能，且某些列在页面和导出文件中的显示模式不同，则要清除掉URL中页面的列选择信息，让导出时重新判断要导出哪些列）
				url=replaceUrlParamValue(url,metadataObj.reportid+'_DYNDISPLAY_COLIDS','');
			}
			url=addSelectedRowDataToUrl(pageid,appidsArr[0],url);
		}else
		{//如果本次是导出多个应用，或只导出一个非报表的组件，则取页面级URL
			url=getComponentUrl(pageid,null,null);
			/**
			 * 如果下载的多个报表中有参与本次显示的从报表，因为它们有自己独立的URL，所以下面代码把它们URL中的参数加到本次导出数据的URL中，以便下载它们与当前显示相符数据
			 */
			var metaDataTmp=null;
			var appguidTmp=null;
			var urlTmp=null;
			for(var i=0,len=appidsArr.length;i<len;i++)
			{
				appguidTmp=getComponentGuidById(pageid,appidsArr[i]);
				metaDataTmp=getReportMetadataObj(appguidTmp);
				if(metaDataTmp==null||metaDataTmp.slave_reportid==null||metaDataTmp.slave_reportid=='') continue; //此报表没有参与本次显示，或者不是从报表
				urlTmp=getComponentUrl(pageid,metaDataTmp.refreshComponentGuid,metaDataTmp.slave_reportid);
				url=mergeUrlParams(url,urlTmp,false);//将从报表的URL中的参数添加到导出数据的URL中
				url=addSelectedRowDataToUrl(pageid,appidsArr[i],url);
			}
		}
	}
	url=replaceUrlParamValue(url,'COMPONENTIDS',componentids);
	url=replaceUrlParamValue(url,'INCLUDE_APPLICATIONIDS',includeApplicationids);
	/**
	 * 因为showreport_onpage_baseurl和showreport_dataexport_baseurl可能跟的参数不一致（比如一个已经有参数，一个没有参数）
	 * 为了确保将它们替换到url中后还是一个合法的url，则将它们后面跟的?或&号也做为一个整体进行替换
	 */
	if(showreport_onpage_baseurl.indexOf('?')>0) 
	{
		if(showreport_onpage_baseurl.lastIndexOf('&')!=showreport_onpage_baseurl.length-1) showreport_onpage_baseurl=showreport_onpage_baseurl+'&';
	}else
	{
		if(showreport_onpage_baseurl.lastIndexOf('?')!=showreport_onpage_baseurl.length-1) showreport_onpage_baseurl=showreport_onpage_baseurl+'?';
	}
	if(showreport_dataexport_baseurl.indexOf('?')>0) 
	{
		if(showreport_dataexport_baseurl.lastIndexOf('&')!=showreport_dataexport_baseurl.length-1) showreport_dataexport_baseurl=showreport_dataexport_baseurl+'&';
	}else
	{
		if(showreport_dataexport_baseurl.lastIndexOf('?')!=showreport_dataexport_baseurl.length-1) showreport_dataexport_baseurl=showreport_dataexport_baseurl+'?';
	}
	var idx=url.indexOf(showreport_onpage_baseurl);
	url=url.substring(0,idx)+showreport_dataexport_baseurl+url.substring(idx+showreport_onpage_baseurl.length);
	postlinkurl(url,true);
}

/**
 * 将此报表所有选中的数据添加到URL中
 */
function addSelectedRowDataToUrl(pageid,reportid,url)
{
	var selectedRowDatasArr=getListReportSelectedTrDatas(pageid,reportid,false,false,false);
	var resultParams=null;
	if(selectedRowDatasArr!=null&&selectedRowDatasArr.length>0)
	{//有选中记录行
		resultParams='';
		var rowDataObjTmp;
		for(var i=0,len=selectedRowDatasArr.length;i<len;i++)
		{
			rowDataObjTmp=selectedRowDatasArr[i];
			var params='';
			for(var key in rowDataObjTmp)
			{
				if(rowDataObjTmp[key]==null||rowDataObjTmp[key]=='') continue;
				if(rowDataObjTmp[key].value==null) rowDataObjTmp[key].value='';
				params+=key+SAVING_NAMEVALUE_SEPERATOR+rowDataObjTmp[key].value+SAVING_COLDATA_SEPERATOR;
			}
			if(params.lastIndexOf(SAVING_COLDATA_SEPERATOR)==params.length-SAVING_COLDATA_SEPERATOR.length)
			{
				params=params.substring(0,params.length-SAVING_COLDATA_SEPERATOR.length);
			}
			if(params!='') resultParams+=params+SAVING_ROWDATA_SEPERATOR;
		}
		if(resultParams.lastIndexOf(SAVING_ROWDATA_SEPERATOR)==resultParams.length-SAVING_ROWDATA_SEPERATOR.length)
		{
			resultParams=resultParams.substring(0,resultParams.length-SAVING_ROWDATA_SEPERATOR.length);
		}
	}
	url=replaceUrlParamValue(url,getComponentGuidById(pageid,reportid)+'_allselectedatas',resultParams);
	return url;
}

/**
 * 将urlMerged的参数合并到urlSrc中
 * @param overwrite 在合并的时候碰到同名的，是否覆盖掉urlSrc中的参数值
 */
function mergeUrlParams(urlSrc,urlMerged,overwrite)
{
	if(urlSrc==null||urlSrc=='') return urlMerged;
	if(urlMerged==null||urlMerged=='') return urlSrc;
	var urlSrcArray=splitUrlAndParams(urlSrc,false);
	var urlMergedArray=splitUrlAndParams(urlMerged,false);
	var urlSrcBase=urlSrcArray[0];//基本URL
	var paramsSrcObj=urlSrcArray[1];//源URL中的参数对象
	var paramsMergedObj=urlMergedArray[1];//被合并的URL中的参数对象
	if(paramsMergedObj==null) return urlSrc;//如果被合并的URL中没有参数
	var paramsObj1=null;//优先级高的参数
	var paramsObj2=null;//优先级低的参数
	if(overwrite)
	{//如果urlMerged中的参数要覆盖掉urlSrc中同名的参数
		paramsObj1=paramsMergedObj;
		paramsObj2=paramsSrcObj;
	}else
	{
		paramsObj1=paramsSrcObj;
		paramsObj2=paramsMergedObj;
	}
	var link='?';
	if(paramsObj1!=null)
	{
		for(var key in paramsObj1)
		{
			urlSrcBase=urlSrcBase+link+key+'='+paramsObj1[key];
			link='&';
			if(paramsObj2!=null) delete paramsObj2[key];//删掉优先级低的同名参数
		}
	}
	if(paramsObj2!=null)
	{
		for(var key in paramsObj2)
		{
			urlSrcBase=urlSrcBase+link+key+'='+paramsObj2[key];
			link='&';
		}
	}
	return urlSrcBase;
}

/*******************************************************/

/**
 * 在onload函数中更新第一级子选择框选项列表
 */
function wx_showChildSelectboxOptionsOnload(paramsObj)
{
	if(paramsObj==null||paramsObj.childids==null||paramsObj.childids=='') return;
	wx_reloadChildSelectBoxOptions(paramsObj.childids,true);
}

/**
 * 输入框数据变化时更新子选择框选项数据
 * @param parentInputboxObj 父输入框对象
 */
function wx_reloadChildSelectBoxOptionsByParentInputbox(parentInputboxObj)
{
	var inputboxid=getInputboxIdByParentElementObj(getInputboxParentElementObj(parentInputboxObj));
	var boxMetadataObj=getInputboxMetadataObj(inputboxid);
	if(boxMetadataObj==null) return;
	var childSelectboxIds=boxMetadataObj.getAttribute('childboxids');//当前父输入框要刷新的所有子下拉框ID（对于列表报表，不包括它们的rowindex）及此下拉框所依赖的所有父输入框ID（对于查询条件上的输入框，则是<condition/>的name，对于编辑列上的输入框，则是column）
	if(childSelectboxIds==null||childSelectboxIds=='') return;
	var rowidx=getRowIndexByRealInputboxId(inputboxid);
	if(rowidx>=0) childSelectboxIds=changeToRealInputBoxids(childSelectboxIds,rowidx);//如果是列表报表上的输入框，则为输入框ID加上rowid，变成真正的输入框ID
	wx_reloadChildSelectBoxOptions(childSelectboxIds,false);//更新所有子选择框选项
}

/**
 * 如果是列表报表上的输入框，则为输入框ID加上rowid，变成真正的输入框ID
 */
function changeToRealInputBoxids(inputboxids,rowidx)
{
	if(inputboxids==null||inputboxids==''||rowidx<0) return inputboxids;
	var resultSelectboxIds='';
	var selectboxIdsArr=inputboxids.split(';');
	var childSelectboxIdTmp,idxTmp;
	for(var i=0,len=selectboxIdsArr.length;i<len;i++)
	{
		childSelectboxIdTmp=selectboxIdsArr[i];
		if(childSelectboxIdTmp==null||childSelectboxIdTmp=='') continue;
		idxTmp=childSelectboxIdTmp.lastIndexOf('__');
		if(idxTmp<=0) childSelectboxIdTmp+='__'+rowidx;
		resultSelectboxIds+=childSelectboxIdTmp+';';
	}
	return resultSelectboxIds;
}

/**
 * 更新所有子选择框选项列表
 * @param childselectboxIdsArr 要更新的所有子选择框ID组合，以分号分隔，这里存放的是真正的输入框ID（列表报表包括行号）
 */
function wx_reloadChildSelectBoxOptions(childselectboxIds,isRefreshOptionInitially)
{
	if(childselectboxIds==null||childselectboxIds=='') return;
	var childselectboxIdsArr=childselectboxIds.split(';');
	if(childselectboxIdsArr==null||childselectboxIdsArr.length==0) return;
	var reportguidTmp,metadataObjTmp,childSelectboxIdTmp;
	var boxMetadataObjTmp,isConditionBoxTmp,parentidsTmp,parentidsObjTmp;
	var params='';
	var url=null;
	var pageid=null;
	var conditionSelectboxIds='';//存放所有查询条件下拉框的ID，以便在回调函数中更新它们的选项
	for(var i=0,len=childselectboxIdsArr.length;i<len;i++)
	{
		childSelectboxIdTmp=childselectboxIdsArr[i];
		if(childSelectboxIdTmp==null||childSelectboxIdTmp=='') continue;
		reportguidTmp=getReportGuidByInputboxId(childSelectboxIdTmp);
		metadataObjTmp=getReportMetadataObj(reportguidTmp);
		if(metadataObjTmp==null) continue;
		if(url==null)
		{//还没有获取刷新URL（URL只需获取一次，因为主从报表的选择框不会一起刷新，所以它们肯定在各自的URL中）
			pageid=metadataObjTmp.pageid;
			url=getComponentUrl(pageid,metadataObjTmp.refreshComponentGuid,metadataObjTmp.slave_reportid);
			if(url==null||url=='') continue;
		}
		boxMetadataObjTmp=getInputboxMetadataObj(childSelectboxIdTmp);
		if(boxMetadataObjTmp==null) continue;
		parentidsTmp=boxMetadataObjTmp.getAttribute('parentids');//得到子下拉框的所有父输入框ID
		if(parentidsTmp==null||parentidsTmp=='') continue;
		isConditionBoxTmp=boxMetadataObjTmp.getAttribute('isconditionbox')==='true';
		if(isConditionBoxTmp===true)
		{//是查询条件的输入框
			conditionSelectboxIds+=childSelectboxIdTmp+';';//对于查询条件上的输入框，只要记录下查询条件的name，以便在回调函数中替换它的选项
			var parentidsWithInputBoxArr=parentidsTmp.split(';');
			var parentBoxObjTmp,parentConNameTmp;
			for(var j=0,len2=parentidsWithInputBoxArr.length;j<len2;j++)
			{//将所有输入框的父条件的值更新到URL中
				parentConNameTmp=parentidsWithInputBoxArr[j];
				parentBoxObjTmp=document.getElementById(reportguidTmp+'_wxcondition_'+parentConNameTmp);
				if(parentBoxObjTmp==null) continue;
				url=replaceUrlParamValue(url,parentConNameTmp,wx_getConditionValue(getInputboxParentElementObj(parentBoxObjTmp)));//这里要将URL中本父输入框对应的条件替换掉，依赖的无输入框的父条件值都在URL中，不需替换			
			}
		}else
		{//是编辑列的输入框
			var paramsTmp=createEditableSelectBoxParams(metadataObjTmp,childSelectboxIdTmp,parentidsTmp);
			if(paramsTmp!='') params+=paramsTmp+SAVING_ROWDATA_SEPERATOR;
		}
	}
	if(conditionSelectboxIds!='') params+="conditionSelectboxIds"+SAVING_NAMEVALUE_SEPERATOR+conditionSelectboxIds;//如果有查询条件输入框，则传所有查询条件输入框ID，以便在回调函数中替换
	if(params.indexOf(SAVING_ROWDATA_SEPERATOR)>0&&params.lastIndexOf(SAVING_ROWDATA_SEPERATOR)==params.length-SAVING_ROWDATA_SEPERATOR.length)
	{
		params=params.substring(0,params.length-SAVING_ROWDATA_SEPERATOR.length);
	}
	url=replaceUrlParamValue(url,'SELECTBOXIDS_AND_PARENTVALUES',params);
	url=replaceUrlParamValue(url,'ACTIONTYPE','GetSelectBoxDataList');
	sendAsynRequestToServer(url,refreshSelectBoxData,onRefreshSelectBoxDataErrorMethod,isRefreshOptionInitially);
}

/**
 * 创建某个父选择框对应的所有父输入框的值组成的URL参数
 */
function createEditableSelectBoxParams(metadataObj,childSelectboxId,parentids)
{
	var mParentValuesObjTmp=wx_getAllSiblingColValuesByInputboxid(childSelectboxId,parentids);
	var strConparams='';
	if(mParentValuesObjTmp!=null)
	{
		var dataObjTmp,valueTmp;
		for(var key in mParentValuesObjTmp)
		{
			dataObjTmp=mParentValuesObjTmp[key];
			if(dataObjTmp==null) continue;
			valueTmp=dataObjTmp.value;
			if(valueTmp==null) valueTmp='';
			strConparams+=key+SAVING_NAMEVALUE_SEPERATOR+valueTmp+SAVING_COLDATA_SEPERATOR;
		}
		if(strConparams.lastIndexOf(SAVING_COLDATA_SEPERATOR)==strConparams.length-SAVING_COLDATA_SEPERATOR.length)
		{
			strConparams=strConparams.substring(0,strConparams.length-SAVING_COLDATA_SEPERATOR.length);
		}
	}
	if(strConparams==null||strConparams=='') return '';
	//下面组成“带rowidx的下拉框ID=本下拉框各父列的值”格式拼凑到将要传给服务器端的参数中
	var params='wx_inputboxid'+SAVING_NAMEVALUE_SEPERATOR+childSelectboxId;
	//if(rowidx!='') params+='__'+rowidx;
	params+=SAVING_COLDATA_SEPERATOR+strConparams;
	return params;
}

/**
 * 从服务器中取到所有更新下拉框选项的数据后更新相应下拉框的下拉选项数据
 */
function refreshSelectBoxData(xmlHttpObj,isRefreshOptionInitially)
{
	var resultData=xmlHttpObj.responseText;
	//alert(WX_selectboxids);
	if(resultData==null||resultData==' '||resultData=='') return;
	var resultDataObj=null;
	try
	{
		resultDataObj=eval('('+resultData+')');
	}catch(e)
	{
		wx_error('获取子选择框选项失败');
		throw e;
	}
	var pageid=resultDataObj.pageid;
	delete resultDataObj['pageid'];
	var metadataObjTmp=null;
	var mRefreshChildIds=new Object();//所有要刷新的子下拉框ID
	var optionsArrTmp,boxMetadataObjTmp;
	var refreshedComboxObjs=new Object();
	for(var selectboxid in resultDataObj)
	{//依次处理每个需要更新选项的子下拉框
		optionsArrTmp=resultDataObj[selectboxid];
		boxMetadataObjTmp=getInputboxMetadataObj(selectboxid);
		if(boxMetadataObjTmp==null) continue;
		metadataObjTmp=getReportMetadataObj(getReportGuidByInputboxId(selectboxid));
		if(metadataObjTmp==null) continue;
		var selectboxtype=boxMetadataObjTmp.getAttribute('selectboxtype');
		if(selectboxtype=='combox')
		{
			refreshedComboxObjs[selectboxid]={"options":optionsArrTmp,"metadataObj":metadataObjTmp};//先记下来等最后更新，因为其它选择框（尤其是单选框和复选框）在后面更新时可能会改变单元格的大小导致先更新的combox显示有问题，所以放在最后更新
		}else if(selectboxtype=='selectbox')
		{
			filledChildSelectboxOptions(metadataObjTmp,selectboxid,optionsArrTmp,selectboxtype,isRefreshOptionInitially);
		}else if(selectboxtype=='checkbox'||selectboxtype=='radio')
		{
			filledChildChkRadioboxOptions(metadataObjTmp,selectboxid,optionsArrTmp,selectboxtype,isRefreshOptionInitially);
		}else
		{
			continue;
		}
		if(boxMetadataObjTmp.getAttribute('displayonclick')!=='true')
		{//如果是直接显示的输入框，则级联刷新它的子选择框
			addRefreshedChildBoxIds(mRefreshChildIds,boxMetadataObjTmp.getAttribute('childboxids'),getRowIndexByRealInputboxId(selectboxid));
		}
	}
	for(var selectboxid in refreshedComboxObjs)
	{//在最后依次更新所有combox输入框的选项
		filledChildSelectboxOptions(refreshedComboxObjs[selectboxid].metadataObj,selectboxid,refreshedComboxObjs[selectboxid].options,'combox',isRefreshOptionInitially);
	}
	refreshAllChildSelectboxs(mRefreshChildIds,isRefreshOptionInitially);
}

/**
 * 添加要刷新的子选择框ID
 * @param mRefreshChildIds 存放添加的子选择框ID集合
 * @param childids要添加的子选择框ID组合的字符串，用分号分隔，对于列表报表这里不包含__rowindex部分
 * @param rowidx对于列表报表，这里存放本行的rowindex，对于细览报表，这里传入-1
 */
function addRefreshedChildBoxIds(mRefreshChildIds,childids,rowidx)
{
	if(childids==null||childids=='') return;
	var childidsArrTmp=childids.split(';');
	var childidTmp;
	for(var i=0,len=childidsArrTmp.length;i<len;i++)
	{
		childidTmp=childidsArrTmp[i];
		if(childidTmp==null||childidTmp=='') continue;
		if(rowidx>=0) childidTmp+='__'+rowidx;
		if(mRefreshChildIds[childidTmp]===true) continue;//已经加进去过
		mRefreshChildIds[childidTmp]=true;
	}
}

/**
 * 刷新所有子选择框
 * @param isRefreshOptionInitially 是否在onload函数中刷新（如果是的话，则要从父标签中取本输入框的值，否则直接从选择框中获取）
 */
function refreshAllChildSelectboxs(mRefreshChildIds,isRefreshOptionInitially)
{
	if(mRefreshChildIds==null) return;
	var childIds='';
	for(var key in mRefreshChildIds)
	{
		if(mRefreshChildIds[key]===true) childIds+=key+';';
	}
	if(childIds!=''&&childIds.length>0) wx_reloadChildSelectBoxOptions(childIds,isRefreshOptionInitially);
}

function filledChildSelectboxOptions(metadataObj,selectboxid,optionsArr,selectboxtype,isRefreshOptionInitially)
{
	var selectboxObjTmp=document.getElementById(selectboxid);
	if(selectboxtype=='combox') selectboxObjTmp=getSelectBoxObjOfCombox(selectboxObjTmp);
	if(selectboxObjTmp==null) return;
	var boxMetadataObj=getInputboxMetadataObj(selectboxObjTmp.getAttribute('id'));
	var isDisplayOnClick=boxMetadataObj.getAttribute('displayonclick')==='true';
	var selectedValueTmp=null;
	if(isDisplayOnClick===true||isRefreshOptionInitially===true)
	{//是由本下拉框的onclick事件触发，或者是在onload函数中刷新此选择框选项（这个时候需要从父标签的value属性中取值，因为输入框还是空选项，其它情况都需要从输入框中取，因为后续操作不会将新值回填到父标签中）
		var parentEleObj=getInputboxParentElementObj(selectboxObjTmp);
		parentEleObj=getUpdateColDestObj(parentEleObj,metadataObj.reportguid,parentEleObj);
		selectedValueTmp=parentEleObj.getAttribute('value');
	}else
	{
		selectedValueTmp=getInputBoxValue(selectboxObjTmp);
	}
	if(selectboxObjTmp.options.length>0)
	{//删除掉所有老的选项数据
		 for(var i=0,length=selectboxObjTmp.options.length;i<length;i++)
		 {
				selectboxObjTmp.remove(selectboxObjTmp.options[i]);
		 }
	}
	var isExistSelectedOption=false;//所有新选项列表中是否存在选中值
	if(optionsArr!=null&&optionsArr.length>0)
	{
		var optTmp;
		for(var i=0;i<optionsArr.length;i++)
		{//添加每个选项数据到下拉框中
			//if(optionsArr[i].value==selectedValueTmp) selectedindex=i;//记下之前选中的数据在新下拉选项列表中的下标，以便后面将它设置为选中
			optTmp=new Option(optionsArr[i].label,optionsArr[i].value);
			selectboxObjTmp.options.add(optTmp);
			if(isSelectedValueForSelectedBox(selectedValueTmp,optionsArr[i].value,selectboxObjTmp)) 
			{
				optTmp.selected=true;
				isExistSelectedOption=true;
			}
		}
	}
	if(selectboxtype=='combox') 
	{
		$('#'+selectboxObjTmp.getAttribute('id')).refreshSelectbox();//刷新它的显示，以便上面的文本框能变成与新下拉框相应的大小
		if(isExistSelectedOption!==true)
		{
			if(selectedValueTmp==null) selectedValueTmp='';
			getTextBoxObjOfCombox(selectboxObjTmp).value=selectedValueTmp;
		}
	}
	//下面代码必须放在refreshSelectbox()方法后面，否则无法改变组合选择框样式
	if(isExistSelectedOption!==true&&!isDisplayOnClick&&!isRefreshOptionInitially&&boxMetadataObj.getAttribute('isconditionbox')!=='true')
	{//如果新选项列表中不存在以前的选中值，且是直接显示输入框（不是点击单元格后再显示输入框），且不是第一次显示输入框的时候刷新选项列表（因为第一次显示输入框用户没有操作，所以不改变输入框的背景色），则将此列加入待保存队列中，并改变它的背景色
		addInputboxDataForSaving(metadataObj.reportguid,selectboxObjTmp);
	}
}

function filledChildChkRadioboxOptions(metadataObj,selectboxid,optionsArr,selectboxtype,isRefreshOptionInitially)
{
	var boxMetadataObj=getInputboxMetadataObj(selectboxid);
	var isDisplayOnClick=boxMetadataObj.getAttribute('displayonclick')==='true';
	if(isDisplayOnClick===true)
	{
		var parentdid=null;
		var idx=selectboxid.lastIndexOf('__');
		if(idx>0)
		{//是列表报表的
			parentdid=selectboxid.substring(0,idx)+'__td'+selectboxid.substring(idx+2);
		}else
		{
			parentdid=selectboxid+'__td';
		}
		var parentTdObj=document.getElementById(parentdid);
		var selectedValue=getUpdateColDestObj(parentTdObj,metadataObj.reportguid,parentTdObj).getAttribute('value');
		setColDisplayValueToEditable2Td(parentTdObj,"<span id='"+selectboxid+"_group'>"+getChkRadioBoxOptionsDisplayString(boxMetadataObj,optionsArr,selectboxid,selectedValue,selectboxtype)+"</span>");
		doPostFilledInContainer(parentTdObj);
	}else
	{
		var parentSpanObj=document.getElementById(selectboxid+'_group');
		var selectedValue=null;
		if(isRefreshOptionInitially===true)
		{
			var parentEleObj=getInputboxParentElementObj(parentSpanObj);
			parentEleObj=getUpdateColDestObj(parentEleObj,metadataObj.reportguid,parentEleObj);
			selectedValue=parentEleObj.getAttribute('value');
		}else
		{
			selectedValue=getInputBoxValueById(selectboxid);
		}
		parentSpanObj.innerHTML=getChkRadioBoxOptionsDisplayString(boxMetadataObj,optionsArr,selectboxid,selectedValue,selectboxtype);
		if(!isRefreshOptionInitially&&boxMetadataObj.getAttribute('isconditionbox')!=='true')
		{
			var optionvalue=null,isExistSelectedOption=false;
			for(var i=0,len=optionsArr.length;i<len;i++)
			{
				optionvalue=optionsArr[i].value;
				if(isSelectedValueForSelectedBox(selectedValue,optionvalue,boxMetadataObj))
				{
					isExistSelectedOption=true;
					break;
				}
			}
			if(isExistSelectedOption!=true)
			{//不存在选中值对应的选项，则要改变此输入框的背景色以便表示被编辑过
				addDataForSaving(metadataObj.reportguid,parentSpanObj);
			}
		}
	}
}

/**
 * 获取单选/复选框的显示列表字符串
 * @param boxType这里是在<input 中指定的类型，而不是在wabacus.cfg.xml中注册的类型，所以不能从boxMetadataObj的typename属性中取
 */
function getChkRadioBoxOptionsDisplayString(boxMetadataObj,optionsArr,realinputboxid,selectedValue,boxType)
{
	var styleproperty=boxMetadataObj.getAttribute('styleproperty');
	styleproperty=styleproperty==null?'':paramdecode(styleproperty);
	var inline_count=boxMetadataObj.getAttribute('inline_count');
	var iinlinecount=(inline_count!=null&&inline_count!='')?parseInt(inline_count,10):0;//取到是否指定了每行显示的选项数
	var resultStr='';
	if(optionsArr==null||optionsArr.length==0)
	{
		if(boxMetadataObj.getAttribute('displayonclick')==='true')
		{//如果是点击时填充的输入框，则显示一个空的选择框，以便onblur时将空值填充给填充到父标签中
			resultStr+="<input type='"+boxType+"'";
			resultStr+=" id= '"+realinputboxid+"' name='"+realinputboxid+"'";
			resultStr+=' '+styleproperty;
			resultStr+='></input>';
		}
	}else
	{
		var optionlabel=null,optionvalue=null;
		for(var i=0,len=optionsArr.length;i<len;i++)
		{
			optionlabel=optionsArr[i].label;
			optionvalue=optionsArr[i].value;
			if(optionlabel==null) optionlabel='';
			if(optionvalue==null) optionvalue='';
			if(optionlabel==''&&optionvalue=='') continue;
			resultStr+="<input type='"+boxType+"'  value='"+optionvalue+"' label='"+optionlabel+"'";
			resultStr+=" id= '"+realinputboxid+"' name='"+realinputboxid+"'";
			resultStr+=' '+styleproperty;
			if(isSelectedValueForSelectedBox(selectedValue,optionvalue,boxMetadataObj)) resultStr+=' checked';
			resultStr+='>'+optionlabel+'</input>';
			if(iinlinecount>0&&i>0&&i%iinlinecount==0) resultStr+='<br>';
		}
	}
	return resultStr;
}

/**
 * 获取某个选择框的所有选项列表数组返回
 */
function getSelectBoxOptionsFromMetadata(boxMetadataObj)
{
	if(boxMetadataObj==null) return null;
	var childOptions=boxMetadataObj.getElementsByTagName('span');//取到所有选项
	if(childOptions==null||childOptions.length==0) return null;
	var optionlabel=null,optionvalue=null;
	var resultArr=new Array();
	for(var i=0,len=childOptions.length;i<len;i++)
	{
		optionlabel=childOptions[i].getAttribute('label');
		optionvalue=childOptions[i].getAttribute('value');
		if(optionlabel==null) optionlabel='';
		if(optionvalue==null) optionvalue='';
		resultArr[resultArr.length]={label:optionlabel,value:optionvalue};
	}
	return resultArr;
}

function onRefreshSelectBoxDataErrorMethod(xmlHttpObj)
{
	wx_error('获取子选择框选项数据失败');
}

/**
 * 判断某个可选择多个选项的输入框的选项值，比如复选框、可复选的下拉框都是这种类型的输入框
 *	@param selectedvalues 所有复选输入框选中的值（以separator分隔）
 * @param optionvalue 被判断的复选输入框某个选项的值
 * @param eleObj 存放有separator属性的标签元素对象
 */
function isSelectedValueForSelectedBox(selectedvalues,optionvalue,eleObj)
{
	if(selectedvalues==null||optionvalue==null) return false;
	var separator=eleObj.getAttribute('separator');
	if(separator==null||separator=='')
	{//单选选择框
		return selectedvalues==optionvalue;
	}else
	{//允许多选的选择框
		if(selectedvalues==optionvalue) return true;
		if(separator!=' ') selectedvalues=wx_trim(selectedvalues);//去掉左右的空格
		//selectedvalues=wx_trim(selectedvalues,separator);
		var tmpArr=selectedvalues.split(separator);
		for(var i=0;i<tmpArr.length;i++)
		{
			if(tmpArr[i]==optionvalue) return true;
		}
	   return false;
   }
}
/*******************************************************/ 

/**
 * 点击弹出窗口输入框弹出新窗口，注意，查询条件上的弹出输入框不使用此JS方法弹出，而是在服务器端构造
 * @param realinputboxid 弹出输入框的源输入框完整id，如果是editablelist2/listform，这里的id包括行号
 * @param params 获取当前弹出输入框的弹出窗口URL所需的参数，包括pageid，reportid，reporttype以及需要从哪些列中获取动态参数信息
 */
function popupPageByPopupInputbox(popupBoxObj)
{
	var oldvalue=popupBoxObj.value;//取到弹出输入框的现在的值
	if(oldvalue==null) oldvalue='';
	var realinputboxid=popupBoxObj.getAttribute('id');
	if(realinputboxid==null||realinputboxid=='') return;
	var boxMetadataObj=getInputboxMetadataObj(realinputboxid);
	if(boxMetadataObj==null) return;
	var paramsOfGetPageUrl=boxMetadataObj.getAttribute('paramsOfGetPageUrl');
	var paramsObj=getObjectByJsonString(paramsOfGetPageUrl);
	if(paramsObj==null)
	{
		wx_warn('没有取到id为'+realinputboxid+'的弹出输入框参数，无法弹出窗口');
		return;
	}
	var pageid=paramsObj.pageid;
	var reportid=paramsObj.reportid;
	var pageurl=paramsObj.popupPageUrl;//为此弹出输入框配置的原始URL
	var reportguid=getComponentGuidById(pageid,reportid);
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj==null) return;
	var urlParamsObj=paramsObj.popupPageUrlParams;
	if(urlParamsObj!=null)
	{//有需要从客户端取列数据的动态参数需要替换
		pageurl=parseDynColParamsInPopupUrl(metadataObj,popupBoxObj,pageurl,urlParamsObj);
	}
	var linkstr=(pageurl.indexOf('?')>0)?'&':'?';
	pageurl+=linkstr+'SRC_PAGEID='+pageid;
	pageurl+='&SRC_REPORTID='+reportid;
	pageurl+='&INPUTBOXID='+realinputboxid;
	pageurl+='&OLDVALUE='+encodeURIComponent(oldvalue);
	wx_winpage(pageurl,getObjectByJsonString(paramsObj.popupparams),paramsObj.beforePopupMethod,popupBoxObj);
}

/**
 * 点击文件上传输入框弹出上传文件界面
 * @param realinputboxid 弹出输入框的源输入框完整id，如果是editablelist2/listform，这里的id包括行号
 * @param params 获取当前弹出输入框的弹出窗口URL所需的参数，包括pageid，reportid，reporttype以及需要从哪些列中获取动态参数信息
 * @param displaytype 显示上传文件的源输入框类型，是图片还是文本框
 */
function popupPageByFileUploadInputbox(fileBoxObj)
{
	var realinputboxid=fileBoxObj.getAttribute('id');
	if(realinputboxid==null||realinputboxid=='') return;
	var boxMetadataObj=getInputboxMetadataObj(realinputboxid);
	if(boxMetadataObj==null) return;
	var paramsOfGetPageUrl=boxMetadataObj.getAttribute('paramsOfGetPageUrl');
	var displaytype=boxMetadataObj.getAttribute('displaytype');
	if(displaytype==null||displaytype=='') displaytype='textbox';
	var oldvalue=null;//文件上传现在的值
	if(displaytype=='image')
	{
		oldvalue=fileBoxObj.getAttribute('srcpath');
	}else
	{
		oldvalue=fileBoxObj.value;
	}
	if(oldvalue==null) oldvalue='';
	var paramsObj=getObjectByJsonString(paramsOfGetPageUrl);
	if(paramsObj==null)
	{
		wx_warn('没有取到id为'+realinputboxid+'的文件上传输入框参数，无法弹出窗口');
		return;
	}
	var pageid=paramsObj.pageid;
	var reportid=paramsObj.reportid;
	var urlparams=paramsObj.popupPageUrl;//为此弹出输入框配置的参数
	var reportguid=getComponentGuidById(pageid,reportid);
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj==null) return;
	var pageurl=WXConfig.showreport_url;
	var linkstr=pageurl.indexOf('?')>0?'&':'?';
	if(urlparams!=null&&urlparams!='') 
	{
		var popurlParamsObj=paramsObj.popupPageUrlParams;
		if(popurlParamsObj!=null)
		{//有需要从客户端取列数据的动态参数需要替换
			urlparams=parseDynColParamsInPopupUrl(metadataObj,fileBoxObj,urlparams,popurlParamsObj);
		}
		pageurl+=linkstr+urlparams;
		linkstr='&';
	}
	pageurl+=linkstr+"PAGEID="+pageid+"&REPORTID="+reportid+"&INPUTBOXID="+realinputboxid+"&ACTIONTYPE=ShowUploadFilePage&FILEUPLOADTYPE=fileinputbox";
	pageurl+='&OLDVALUE='+encodeURIComponent(oldvalue);
	wx_winpage(pageurl,getObjectByJsonString(paramsObj.popupparams),paramsObj.beforePopupMethod,fileBoxObj);
}

/**
 * 替换掉弹出页面pageurl中动态列数据
 */
function parseDynColParamsInPopupUrl(metadataObj,boxObj,pageurl,urlParamsObj)
{
	var colObj=new Object();//构造{col1:true,col2:true,...}格式的对象，用于传入获取报表数据的接口方法中指定要获取哪些列的数据
	var idxTmp;
	for(var colTmp in urlParamsObj)
	{
		if(colTmp==null||colTmp=='') continue;
		idxTmp=colTmp.lastIndexOf('__old');
		if(idxTmp>0&&idxTmp==colTmp.length-'__old'.length) colTmp=colTmp.substring(0,idxTmp);
		colObj[colTmp]=true;
	}
	var reportfamily=metadataObj.reportfamily;
	var mColValuesObjTmp=null;
	if(reportfamily==ReportFamily.EDITABLELIST2||reportfamily==ReportFamily.LISTFORM)
	{//如果是这些报表类型，则肯定是它们编辑列上的弹出窗口输入框（因为如果是查询条件输入框，则不管什么列表报表类型，都会在服务器端替换掉动态列数据）
		var trObj=getTrDataObj(metadataObj.reportguid,boxObj);
		if(trObj!=null) mColValuesObjTmp=wx_getListReportColValuesInRow(trObj,colObj);
	}else
	{
		mColValuesObjTmp=getEditableReportColValues(metadataObj.pageid,metadataObj.reportid,colObj,null);
	}
	var realColTmp,colDataTmp;
	for(var colTmp in urlParamsObj)
	{
		if(colTmp==null||colTmp=='') continue;
		idxTmp=colTmp.lastIndexOf('__old');
		realColTmp=(idxTmp>0&&idxTmp==colTmp.length-'__old'.length)?colTmp.substring(0,idxTmp):colTmp;
		colDataTmp=null;
		if(mColValuesObjTmp!=null&&mColValuesObjTmp[realColTmp]!=null)
		{
			if(idxTmp>0&&idxTmp==colTmp.length-'__old'.length)
			{//如果当前是取本列的旧数据
				colDataTmp=mColValuesObjTmp[realColTmp].oldvalue;
			}else
			{
				colDataTmp=mColValuesObjTmp[realColTmp].value;
			}
		}
		if(colDataTmp==null) colDataTmp='';
		pageurl=pageurl.replace(urlParamsObj[colTmp],colDataTmp);//将占位符替换成真正值
	}
	return pageurl;
}

function getTrDataObj(reportguid,inputboxObj)
{
	if(inputboxObj==null) return null;
	var trObj=getParentElementObj(inputboxObj,'TR');
	while(trObj!=null)
	{
		if(isEditableListReportTr(reportguid,trObj)) return trObj;
		trObj=getParentElementObj(trObj,'TR');
	}
	return null;
}

var WX_PARENT_INPUTBOXID='';//弹出窗口输入框所对应的父窗口输入框ID

/**
 * 将用户在弹出窗口输入框中输入（或选择）的数据设置到父窗口的输入框中
 * 目前有两种类型的弹出输入框，供选择数据的弹出输入框和文件上传弹出输入框
 */
function setPopUpBoxValueToParent(value,realinputboxid)
{
	var parentbox=document.getElementById(realinputboxid);
	if(parentbox==null)
	{
		wx_warn('没有取到id为'+realinputboxid+'的弹出源输入框');
		return false;
	}
	WX_PARENT_INPUTBOXID=realinputboxid;
	if(parentbox.tagName=='IMG')
	{
		parentbox.src=value;
		parentbox.setAttribute('srcpath',value);
	}else
	{
		parentbox.value=value;
	}
	var reportguid=getReportGuidByInputboxId(realinputboxid);
	if(realinputboxid.indexOf(reportguid+'_wxcondition_')<0)
	{//当前弹出窗口输入框不是查询条件输入框
		addInputboxDataForSaving(reportguid,parentbox);
	}
	parentbox=document.getElementById(realinputboxid);
	if(parentbox!=null) parentbox.focus();
}

/**
 * 关闭弹出输入框界面事件
 * 主要是将弹出源输入框设置为选中状态
 */
function closePopUpPageEvent(type)
{
	if(WX_PARENT_INPUTBOXID&&WX_PARENT_INPUTBOXID!='')
	{
		var parentbox=document.getElementById(WX_PARENT_INPUTBOXID);
		if(parentbox) parentbox.focus();
	}
	WX_PARENT_INPUTBOXID='';
}

/**
 * 删除文件上传输入框中的服务器端文件的回调函数
 */
function deleteUploadFilesInvokeCallback(xmlHttpObj,datasObj)
{
	var rtnVal=xmlHttpObj.responseText;//服务器端返回的字符串
	if(rtnVal==null||rtnVal=='') return;
	var idx=rtnVal.indexOf("|");
	var prompttype='warn';
	if(idx>0)
	{
		prompttype=rtnVal.substring(0,idx);//提示类型
		rtnVal=rtnVal.substring(idx+1);
	}
	if(prompttype=='alert')
	{
		wx_alert(rtnVal);
	}else if(prompttype=='warn')
	{
		wx_warn(rtnVal);
	}else if(prompttype=='success')
	{
		wx_success(rtnVal);
	}else if(prompttype=='error')
	{
		wx_error(rtnVal);
	}   
}

/*************************显示右键菜单，目前只有可编辑报表类型用上************************************/
var WX_contextmenuObj=null;
function showcontextmenu(menuid,e) 
{   
	if(WX_contextmenuObj!=null)
	{//如果同一页面上点击了其它报表的右键按钮，则先将其隐藏
		WX_contextmenuObj.style.visibility = "hidden";
	}
	WX_contextmenuObj=document.getElementById(menuid);
	if(WX_contextmenuObj==null) return;
	var isEmpty=WX_contextmenuObj.getAttribute('isEmpty');
	if(isEmpty=='true') return;//如果当前菜单没有菜单项，则不显示出来
	//获取当前鼠标右键按下后的位置，据此定义菜单显示的位置
	var event = e || window.event;
	var documentSize=getDocumentSize();
	var rightedge = documentSize.width - event.clientX;
	var bottomedge = documentSize.height - event.clientY;
  	var documentScroll=getDocumentScroll();
  
	//如果从鼠标位置到窗口右边的空间小于菜单的宽度，就定位菜单的左坐标（Left）为当前鼠标位置向左一个菜单宽度   
	if (rightedge < WX_contextmenuObj.offsetWidth) 
	{
		WX_contextmenuObj.style.left = (documentScroll.scrollLeft + event.clientX - WX_contextmenuObj.offsetWidth)+'px';
	} else 
	{//否则，就定位菜单的左坐标为当前鼠标位置  
		WX_contextmenuObj.style.left = (documentScroll.scrollLeft + event.clientX)+'px'; 
	}
  
	//如果从鼠标位置到窗口下边的空间小于菜单的高度，就定位菜单的上坐标（Top）为当前鼠标位置向上一个菜单高度   
	if (bottomedge < WX_contextmenuObj.offsetHeight) 
	{
		WX_contextmenuObj.style.top = (documentScroll.scrollTop + event.clientY - WX_contextmenuObj.offsetHeight)+'px';
	} else 
	{//否则，就定位菜单的上坐标为当前鼠标位置  
   	WX_contextmenuObj.style.top = (documentScroll.scrollTop + event.clientY)+'px';
	}
  
	//设置菜单可见   
	WX_contextmenuObj.style.visibility = "visible";
	//阻止浏览器自己的右键菜单弹出来
	if (window.event) 
	{
		event.returnValue = false;
	} else 
	{
		event.preventDefault();
	}
	document.onclick = hidecontextmenu;
	return false;
}
function hidecontextmenu() 
{//隐藏菜单 
	WX_contextmenuObj.style.visibility = "hidden";
}
function highlightmenuitem(evt) 
{   
//高亮度鼠标经过的菜单条项目
//如果鼠标经过的对象是menuitems，就重新设置背景色与前景色   
//event.srcElement.className表示事件来自对象的名称，必须首先判断这个值，这很重要！   
	var event = evt || window.event;
	var element = event.srcElement || event.target;
	if (element.className == "contextmenuitems_enabled") 
	{
		element.style.backgroundColor = "highlight";
		element.style.color = "white";
	} else 
	{
		if (element.className == "contextmenuitems_disabled") 
		{
			element.style.backgroundColor = "highlight";
		}
	}
}
function lowlightmenuitem(evt) 
{//恢复菜单条项目的正常显示   
	var event = evt || window.event;
	var element = event.srcElement || event.target;
	if (element.className == "contextmenuitems_enabled") 
	{
		element.style.backgroundColor = "";
		element.style.color = "black";   
	} else 
	{
		if (element.className == "contextmenuitems_disabled") 
		{
			element.style.backgroundColor = "";
		}
	}
}

/**
 * 跳转到目标页面，并在目标页面提供返回功能精确返回到本页面
 * @param pageid 当前源页面的id
 * @param desturl 目标页面的URL
 */
function forwardPageWithBack(pageid,desturl,beforeCallBack)
{
	desturl=paramdecode(desturl);
	var urlSpanObj=document.getElementById(pageid+'_url_id');
	var srcurl=urlSpanObj.getAttribute('encodevalue');//当前页面经过Ascii编码的URL
	if(srcurl==null||srcurl=='')
	{
		wx_error('没有取到本页面的URL，跳转失败');
		return;
	}
	var ancestorPageUrls=urlSpanObj.getAttribute('ancestorPageUrls');//当前页面的祖先页面URL，以便能逐层返回
	if(ancestorPageUrls==null||ancestorPageUrls=='')
	{
		ancestorPageUrls=srcurl;
	}else
	{
		ancestorPageUrls=srcurl+'||'+ancestorPageUrls;//将当前页面的URL放在最前面，以便返回时先返回到本页面（采用堆栈的形式组织）
	}
	desturl=replaceUrlParamValue(desturl,'ancestorPageUrls',ancestorPageUrls);
	
	/**
	 * 跳转必须刷新整个页面
	 */
	desturl=replaceUrlParamValue(desturl,'refreshComponentGuid','[OUTERPAGE]'+pageid);//加上[OUTERPAGE]表示刷新的refreshComponentGuid不属于desturl要加载的页面的一部分
	desturl=replaceUrlParamValue(desturl,'SLAVE_REPORTID',null);
	//alert(desturl);
	if(beforeCallBack!=null&&beforeCallBack!='') desturl=beforeCallBack(desturl);
	if(desturl!=null&&desturl!='') refreshComponent(desturl,null,{keepSelectedRowsAction:true,keepSavingRowsAction:true});
}

/**
 * 删除掉本报表及与其存在查询条件关联报表的延迟加载参数
 *	@param removeConditionRelateReports：true：删除本报表及与其有查询条件关联报表的延迟加载参数；false：只删除本报表延迟加载参数
 */
function removeLazyLoadParamsFromUrl(url,reportMetadataObj,removeConditionRelateReports)
{//
	url=replaceUrlParamValue(url,reportMetadataObj.reportid+"_lazydisplaydata",null);
	url=replaceUrlParamValue(url,reportMetadataObj.reportid+"_lazydisplaydata_prompt",null);
	if(removeConditionRelateReports)
	{
		var relateConditionReportIds=reportMetadataObj.metaDataSpanObj.getAttribute('relateConditionReportIds');
		if(relateConditionReportIds!=null&&relateConditionReportIds!='')
		{//有查询条件关联的报表
			var reportIdsArr=relateConditionReportIds.split(';');
			var reportIdTmp;
			for(var i=0;i<reportIdsArr.length;i=i+1)
			{
				reportIdTmp=reportIdsArr[i];
				if(reportIdTmp==null||reportIdTmp=='') continue;
				url=replaceUrlParamValue(url,reportIdTmp+"_lazydisplaydata",null);
				url=replaceUrlParamValue(url,reportIdTmp+"_lazydisplaydata_prompt",null);
			}
		}
	}
	return url;
}

/*********************************客户端校验*****************************************************/

/**
 * 当输入框失去焦点时进行的客户端/服务器端校验
 */
function wx_onblurValidate(reportguid,inputboxObj,isConditionBox,isServerValidate,serverValidateCallbackMethod)
{
 	var metadataObj=getReportMetadataObj(reportguid);
 	var parentElementObj=getInputboxParentElementObj(inputboxObj);
 	if(parentElementObj==null)
 	{
 		wx_alert('传入的标签元素对象不属于这一列标签对象');//当用户为自定义输入框校验调用此方法时，传入的元素有可能不是输入框，而是很远的一个标签对象
 		return true;
 	}
 	var validateResult=false;
 	if(isConditionBox===true)
 	{//是查询条件输入框
 		validateResult=validateConditionBoxValue(metadataObj,getAllConditionValues(reportguid),parentElementObj,true,isServerValidate,serverValidateCallbackMethod);
 	}else
 	{//编辑输入框
 		var datasObj=null;
 		if(metadataObj.reportfamily==ReportFamily.EDITABLELIST2||metadataObj.reportfamily==ReportFamily.LISTFORM)
 		{
 			datasObj=wx_getListReportColValuesInRow(parentElementObj.parentNode,null);
 		}else
 		{
			datasObj=getEditableReportColValues(getPageIdByComponentGuid(reportguid),getComponentIdByGuid(reportguid),null,null);
 		}
 		var rowDatasObj=new Object();
		if(datasObj!=null)
		{
			for(var key in datasObj)
			{
				if(datasObj[key].name==null||datasObj[key].name=='') continue;
				rowDatasObj[datasObj[key].name]=datasObj[key].value;
				rowDatasObj[datasObj[key].name+'__old']=datasObj[key].oldvalue;
			}
		}
 		validateResult=validateEditColBoxValue(metadataObj,rowDatasObj,parentElementObj,true,isServerValidate,serverValidateCallbackMethod);
 	}
 	//if(validateResult===true)
 	//{
 		//changeEditedInputboxDisplayStyle(parentElementObj);
 	//}
 	return validateResult;
}

/**
 * 校验查询条件输入框，配置不合法及校验通过都返回true，否则返回false
 */
function validateConditionBoxValue(metadataObj,conditionDatasObj,parentFontObj,isOnblur,isServerValidate,serverValidateCallbackMethod)
{
	if(parentFontObj==null) return true;
	var connameTmp=parentFontObj.getAttribute('value_name');
 	if(connameTmp==null||connameTmp=='') return true;
 	var convalueTmp=wx_getConditionValue(parentFontObj);//根据父<font/>标签对象，获取到此条件输入框的值
 	if(isServerValidate===true)
 	{
 		return doServerValidateInputBoxValue(metadataObj,metadataObj.reportguid+'_wxcondition_'+connameTmp,parentFontObj,convalueTmp,conditionDatasObj,isOnblur,serverValidateCallbackMethod);
 	}else
 	{
 		return doJsValidateInputBoxValue(metadataObj,metadataObj.reportguid+'_wxcondition_'+connameTmp,parentFontObj,convalueTmp,conditionDatasObj,isOnblur);
 	}
}

/**
 * 校验列编辑输入框，配置不合法及校验通过都返回true，否则返回false
 */
function validateEditColBoxValue(metadataObj,rowDatasObj,parentElementObj,isOnblur,isServerValidate,serverValidateCallbackMethod)
{
	if(parentElementObj==null) return true;
	var colnameTmp=parentElementObj.getAttribute('value_name');
 	if(colnameTmp==null||colnameTmp=='') return true;//不可编辑，说明没有输入框，则不进行校验
 	var colvalue=wx_getColValue(getUpdateColDestObj(parentElementObj,metadataObj.reportguid,parentElementObj));//根据父标签对象，获取到此输入框的值
 	if(isServerValidate===true)
 	{
 		return doServerValidateInputBoxValue(metadataObj,metadataObj.reportguid+'_wxcol_'+colnameTmp,parentElementObj,colvalue,rowDatasObj,isOnblur,serverValidateCallbackMethod);
 	}else
 	{
 		return doJsValidateInputBoxValue(metadataObj,metadataObj.reportguid+'_wxcol_'+colnameTmp,parentElementObj,colvalue,rowDatasObj,isOnblur);
 	}
}

/**
 * 进行客户端校验
 */
function doJsValidateInputBoxValue(metadataObj,inputboxid,parentElementObj,value,datasObj,isOnblur)
{
	var validatetype=metadataObj.metaDataSpanObj.getAttribute('validateType_'+inputboxid);
	if((isOnblur&&validatetype=='onsubmit')||(!isOnblur&&validatetype=='onblur')) return true;//不需要进行校验
	var validateMethodTmp=getObjectByJsonString(metadataObj.metaDataSpanObj.getAttribute('validateMethod_'+inputboxid));//校验方法
 	if(validateMethodTmp==null||validateMethodTmp.method==null) return true;
 	var paramsObj=getObjectByJsonString(metadataObj.metaDataSpanObj.getAttribute('jsValidateParamsObj_'+inputboxid));//校验时所需用到的参数
	if(paramsObj==null) paramsObj=new Object();
	paramsObj.datasObj=datasObj;
	return validateMethodTmp.method(metadataObj,getInputboxMetadataObj(inputboxid),value,parentElementObj,paramsObj,isOnblur);
}

/**
 * 进行服务器端校验
 */
function doServerValidateInputBoxValue(metadataObj,inputboxid,parentElementObj,value,datasObj,isOnblur,serverValidateCallbackMethod)
{
	var server_url=getComponentUrl(metadataObj.pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
	server_url=replaceUrlParamValue(server_url,"INPUTBOXID",inputboxid);
	server_url=replaceUrlParamValue(server_url,"ACTIONTYPE","ServerValidateOnBlur");
	server_url=replaceUrlParamValue(server_url,"INPUTBOX_VALUE",value);
	server_url=replaceUrlParamValue(server_url,"PAGEID",metadataObj.pageid);
	server_url=replaceUrlParamValue(server_url,"REPORTID",metadataObj.reportid);
	var otherValues='';
	if(datasObj!=null)
	{
		for(var key in datasObj)
		{
			if(datasObj[key]==null) continue;
			otherValues+=key+SAVING_NAMEVALUE_SEPERATOR+datasObj[key]+SAVING_COLDATA_SEPERATOR;
		}
		if(otherValues.lastIndexOf(SAVING_COLDATA_SEPERATOR)==otherValues.length-SAVING_COLDATA_SEPERATOR.length)
		{
			otherValues=otherValues.substring(0,otherValues.length-SAVING_COLDATA_SEPERATOR.length);
		}
	}
	server_url=replaceUrlParamValue(server_url,"OTHER_VALUES",otherValues);
	var urlparams=server_url.substring(server_url.indexOf('?')+1);
	server_url=server_url.substring(0,server_url.indexOf('?'));
	var datasObj=new Object();
	datasObj.inputboxid=inputboxid;
	datasObj.parentElementObj=parentElementObj;
	datasObj.inputboxvalue=value;
	datasObj.metadataObj=metadataObj;
	datasObj.serverValidateCallbackMethod=serverValidateCallbackMethod;
	XMLHttpREPORT.sendReq('POST',server_url,urlparams,onSuccessServerValidate,onFailedServerValidate,datasObj);
}

function onSuccessServerValidate(xmlHttpObj,datasObj)
{
	var mess=xmlHttpObj.responseText;
	var paramsObj=new Object();
	var strArr=parseTagContent(mess,'<WX-SUCCESS-FLAG>','</WX-SUCCESS-FLAG>');
	paramsObj.isSuccess=strArr!=null&&strArr[0]=='true';//校验成功与失败的标识
	strArr=parseTagContent(mess,'<WX-ERROR-MESSAGE>','</WX-ERROR-MESSAGE>');
	paramsObj.errormess=strArr==null||strArr.length==0?'':strArr[0];
	strArr=parseTagContent(mess,'<WX-SERVER-DATA>','</WX-SERVER-DATA>');
	paramsObj.serverDataObj=strArr==null||strArr.length==0?null:getObjectByJsonString(strArr[0]);
	paramsObj.validatetype='onblur';
	paramsObj.inputboxid=datasObj.inputboxid;
	paramsObj.parentElementObj=datasObj.parentElementObj;
	paramsObj.metadataObj=datasObj.metadataObj;
	paramsObj.value=datasObj.inputboxvalue;
	if(!paramsObj.isSuccess&&paramsObj.errormess!=null&&paramsObj.errormess!='')
	{
		var errorPromptParamsObj=null;//出错提示参数
		strArr=parseTagContent(mess,'<WX-ERRORPROMPT-PARAMS>','</WX-ERRORPROMPT-PARAMS>');
		if(strArr!=null&&strArr.length==2)
		{
			errorPromptParamsObj=getObjectByJsonString(strArr[0]);
		}
		if(errorPromptParamsObj==null)
		{//没有在校验方法中指定出错提示参数
			var boxMetadataObj=getInputboxMetadataObj(paramsObj.inputboxid);
			if(boxMetadataObj!=null)
			{
				errorPromptParamsObj=getObjectByJsonString(boxMetadataObj.getAttribute('errorpromptparamsonblur'));//取本输入框配置的校验出错提示参数，或者全局默认出错提示参数
			}
		}
		wx_serverPromptErrorOnblur(paramsObj.metadataObj,paramsObj.parentElementObj,paramsObj.errormess,errorPromptParamsObj);
	}else if(paramsObj.isSuccess)
	{
		wx_hideServerPromptErrorOnblur(paramsObj.metadataObj,paramsObj.parentElementObj);
	}
	if(datasObj.serverValidateCallbackMethod!=null)
	{//调用校验回调函数（成功和失败都会调用）
		datasObj.serverValidateCallbackMethod(paramsObj);
	}
}

function onFailedServerValidate()
{
	wx_warn('进行服务器端校验失败');
}

/**
 * 根据父标签对象获取更新时要改变样式的标签对象
 */
function getChangeStyleObjByParentElementOnEdit(parentElement)
{
	if(parentElement.changeStyleObjByInputBoxObjOnEdit!=null) return parentElement.changeStyleObjByInputBoxObjOnEdit;//已经设置过（比如自定义输入框就会在校验的时候设置一下）
	var boxId=getInputboxIdByParentElementObj(parentElement);
	if(boxId==null||boxId=='') return null;
	if(boxId.indexOf('_wxcol_')>0)
	{//是编辑列输入框
		var metadataObj=getReportMetadataObj(getReportGuidByInputboxId(boxId));
		if(metadataObj==null) return null;
		if(metadataObj.reportfamily==ReportFamily.EDITABLELIST2||metadataObj.reportfamily==ReportFamily.EDITABLEDETAIL2)
		{//eleObj即为<td/>对象，也是改变<td/>对象的背景色
			return parentElement;
		}
	}
	var changeStyleEleObj=getWXInputBoxChildNode(parentElement);
	if(changeStyleEleObj==null) changeStyleEleObj=getInputBoxChildNode(parentElement);
	changeStyleEleObj=getChangeStyleObjByInputBoxObjOnEdit(changeStyleEleObj);
	if(changeStyleEleObj==null) changeStyleEleObj=parentElement;
	return changeStyleEleObj;
}

/**********************************************************************************************/

/*********************************类似DWR的客户端调用服务器端JAVA代码的方法***************************/

/**
 * 被wabacus_api.js中的invokeServerActionForReportData()方法调用
 */
function invokeServerActionForReportDataImpl(pageid,reportid,serverClassName,conditionsObj,paramsObj,shouldRefreshPage,beforeCallbackMethod,afterCallbackMethod)
{
	var reportguid=getComponentGuidById(pageid,reportid);
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj==null) return;
	if(beforeCallbackMethod=='') beforeCallbackMethod=null;
	if(beforeCallbackMethod!=null&&typeof beforeCallbackMethod!='function')
	{
		wx_warn('传入的beforeCallbackMethod参数不是函数对象');
		return;
	}
	if(afterCallbackMethod=='') afterCallbackMethod=null;
	if(afterCallbackMethod!=null&&typeof afterCallbackMethod!='function')
	{
		wx_warn('传入的afterCallbackMethod参数不是函数对象');
		return;
	}
	var datasArr=null;
	if(serverClassName.indexOf('button{')==0)
	{//如果是在invokeComponentSqlActionButton()方法中调用此方法
		datasArr=conditionsObj;
	}else
	{
		if(serverClassName.indexOf('button_autoreportdata{')==0)
		{//自动获取报表数据传到后台进行操作的<button/>配置，把button_autoreportdata{替换为button{，方便后台解析
			var idx=serverClassName.indexOf('button_autoreportdata{');
			serverClassName=serverClassName.substring(0,idx)+'button{'+serverClassName.substring(idx+'button_autoreportdata{'.length);
		}
		datasArr=new Array();
		var datasObjArr=convertToArray(getEditableReportColValues(pageid,reportid,null,conditionsObj));//如果是可编辑报表类型，根据条件获取到报表数据
		if(datasObjArr!=null&&datasObjArr.length>0)
		{//取到报表数据
			var datasObj=null;
			var paramDataObjTmp=null;
			for(var i=0,len=datasObjArr.length;i<len;i++)
			{//循环第一条记录（如果是细览报表，则只有一条记录）
				datasObj=new Object();
				var hasData=false;
				for(var key in datasObjArr[i])
				{//循环当前记录中每列数据
					paramDataObjTmp=datasObjArr[i][key];
					if(paramDataObjTmp==null||paramDataObjTmp.name==null||paramDataObjTmp.name=='') continue;//没有name属性
					hasData=true;
					if(paramDataObjTmp.value==null) datasObjArr[i].value='';
					datasObj[paramDataObjTmp.name]=paramDataObjTmp.value;
					if(paramDataObjTmp.oldname==null||paramDataObjTmp.oldname==''||paramDataObjTmp.oldname==paramDataObjTmp.name) continue;//如果此列没有oldname，或者与name相同，则不将它加入参数列表中
					if(paramDataObjTmp.oldvalue==null) paramDataObjTmp.oldvalue='';
					datasObj[paramDataObjTmp.oldname]=paramDataObjTmp.oldvalue;
				}
				if(hasData) datasArr[datasArr.length]=datasObj;
			}
		}
		if(datasArr.length==0)
		{
			wx_warn('没有取到要操作的报表数据！');
			return;
		}
	}
	if(beforeCallbackMethod!=null)
	{//指定了调用前回调函数
		if(datasArr==null) datasArr=new Array();//方便在拦截器前置动作中开发人员加入其它要传入后台的值
		if(paramsObj==null) paramsObj=new Object();//方便在拦截器前置动作中开发人员加入其它要传入后台的值
		var rtnVal=beforeCallbackMethod(datasArr,paramsObj);
		if(rtnVal!==true) return;//不需要进行后续调用操作
	}
	var paramKey=null;
	if(afterCallbackMethod!=null) paramKey=reportguid;//如果有回调函数，则可能会用到全局变量WX_ALL_SAVEING_DATA中的值，所以以reportguid做为KEY存放数据到其中 
	var paramsStr=assempleServerActionDataParams(paramKey,datasArr);//构造参数
	var url=getComponentUrl(metadataObj.pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
	if(shouldRefreshPage) 
	{//需要刷新报表显示
		url=removeReportNavigateInfosFromUrl(url,metadataObj,null);//删除本报表的翻页导航ID以及查询条件关联的报表的翻页导航ID，重新计算页码
		url=url+'&WX_SERVERACTION_SHOULDREFRESH=true';
	}
	if(paramsStr!=null&&paramsStr!='') url=url+'&WX_SERVERACTION_PARAMS='+paramsStr;
	paramsStr=getCustomizedParamsObjAsString(paramsObj);
	if(paramsStr!=null&&paramsStr!='') url=url+'&WX_SERVERACTION_CUSTOMIZEDPARAMS='+paramsStr;
	if(afterCallbackMethod!=null)
	{//指定了回调后回调函数
		url=url+'&WX_SERVERACTION_CALLBACKMETHOD='+getFunctionNameByFunctionObj(afterCallbackMethod);
	}
	url=url+'&WX_SERVERACTION_COMPONENTID='+reportid;
	url=url+'&WX_SERVERACTION_SERVERCLASS='+serverClassName;
	refreshComponent(url);
}

function getCustomizedParamsObjAsString(paramsObj)
{
	if(paramsObj==null||paramsObj=='') return '';
	var resultStr='';
	for(var key in paramsObj)
	{
		if(paramsObj[key]==null||paramsObj[key]=='') continue;
		resultStr+=key+SAVING_NAMEVALUE_SEPERATOR+encodeURIComponent(paramsObj[key])+SAVING_COLDATA_SEPERATOR;
	}
	if(resultStr.lastIndexOf(SAVING_COLDATA_SEPERATOR)==resultStr.length-SAVING_COLDATA_SEPERATOR.length)
	{
		resultStr=resultStr.substring(0,resultStr.length-SAVING_COLDATA_SEPERATOR.length);
	}
	return resultStr;
}

/**
 * 被wabacus_api.js中的invokeServerActionForComponent()方法调用
 */
function invokeServerActionForComponentImpl(pageid,componentid,serverClassName,datas,shouldRefreshPage,callbackMethod)
{
	if(callbackMethod!=null&&typeof callbackMethod!='function')
	{
		wx_warn('传入的回调函数不是函数对象');
		return;
	}
	var componentguid=getComponentGuidById(pageid,componentid);
	var cmetaDataObj=getComponentMetadataObj(componentguid);
	if(cmetaDataObj==null) return;
	var url=getComponentUrl(pageid,cmetaDataObj.refreshComponentGuid,cmetaDataObj.metaDataSpanObj.getAttribute('slave_reportid'));
	if(url==null||url=='') return;
	if(shouldRefreshPage) 
	{
		url=resetComponentUrl(pageid,componentid,url,'navigate.false');//对于报表，只重置翻页导航栏，重新计算记录数和页数，但不重置显示页码，即保留当前页码，也不重置查询条件
		url=url+'&WX_SERVERACTION_SHOULDREFRESH=true';
	}
	if(componentid==null||componentid=='') componentid=pageid;
	url=url+'&WX_SERVERACTION_COMPONENTID='+componentid;
	var paramKey=null;
	if(callbackMethod!=null)
	{
		url=url+'&WX_SERVERACTION_CALLBACKMETHOD='+getFunctionNameByFunctionObj(callbackMethod);
		paramKey=componentguid;//只有配置了客户端回调函数时才需要将数据存入全局变量中供回调函数使用
	}
	var paramsStr=assempleServerActionDataParams(paramKey,datas);//构造参数
	if(paramsStr!=null&&paramsStr!='') url=url+'&WX_SERVERACTION_PARAMS='+paramsStr;
	url=url+'&WX_SERVERACTION_SERVERCLASS='+serverClassName;
	refreshComponent(url);
}

/**
 * 根据函数对象取到函数名
 */
function getFunctionNameByFunctionObj(functionObj)
{
	if(functionObj==null||typeof functionObj!='function') return '';
	var functionStr=wx_trim(functionObj.toString());
	var idx=functionStr.indexOf('function');
	if(idx<0) return '';//不是有效js函数定义
	functionStr=functionStr.substring(idx+'function'.length);
	functionStr=functionStr.substring(0,functionStr.indexOf('('));
	return wx_trim(functionStr);
}

/**
 * 被wabacus_api.js中的invokeServerAction()方法调用
 */
function invokeServerActionImpl(serverClassName,datas,callbackMethod,onErrorMethod)
{
	if(callbackMethod!=null&&typeof callbackMethod!='function')
	{
		wx_warn('传入的回调函数不是函数对象');
		return;
	}
	if(onErrorMethod!=null&&typeof onErrorMethod!='function')
	{
		wx_warn('传入的出错处理函数不是函数对象');
		return;
	}
	var url=WXConfig.showreport_url;
	var token='?';
	if(url.indexOf('?')>0) token='&';
	url=url+token+'ACTIONTYPE=invokeServerAction';
	var paramsStr=assempleServerActionDataParams(null,datas);//构造参数
	if(paramsStr!=null&&paramsStr!='')
	{
		url=url+'&WX_SERVERACTION_PARAMS='+paramsStr;
	}
	url=url+'&WX_SERVERACTION_SERVERCLASS='+serverClassName;
	var urlparams=url.substring(url.indexOf('?')+1);
	url=url.substring(0,url.indexOf('?'));
	XMLHttpREPORT.sendReq('POST',url,urlparams,callbackMethod,onErrorMethod,datas);
}

/**
 * 根据传入的参数对象组装传入URL中的参数字符串，并把参数放入WX_ALL_SAVEING_DATA中以便回调函数来使用
 * @param paramsKey 在全局变量WX_ALL_SAVEING_DATA中存放回调函数要使用的参数的KEY，如果没有传入此参数，则不会在WX_ALL_SAVEING_DATA中存放本次传入服务器端的参数
 * @param datasObj 参数对象或参数对象数组
 */
function assempleServerActionDataParams(paramsKey,datasObj)
{
	if(datasObj==null||datasObj=='') return '';
	var datasObjArr=convertToArray(datasObj);
	var resultParamsStr='';//存放所有记录上的数据组成的参数字符串
	for(var i=0,len=datasObjArr.length;i<len;i++)
	{//循环第一条记录（如果是细览报表，则只有一条记录）
		var rowParamsStr='';//存放当前记录上所有列数据的参数字符串
		for(var key in datasObjArr[i])
		{//循环当前记录中每列数据
			if(key==null||key=='') continue;
			if(datasObjArr[i][key]==null) datasObjArr[i][key]='';
			rowParamsStr=rowParamsStr+key+SAVING_NAMEVALUE_SEPERATOR+encodeURIComponent(datasObjArr[i][key])+SAVING_COLDATA_SEPERATOR;
		}
		if(rowParamsStr.lastIndexOf(SAVING_COLDATA_SEPERATOR)==rowParamsStr.length-SAVING_COLDATA_SEPERATOR.length)
		{
			rowParamsStr=rowParamsStr.substring(0,rowParamsStr.length-SAVING_COLDATA_SEPERATOR.length);
		}
		if(rowParamsStr=='') continue;//此条记录没有数据
		resultParamsStr=resultParamsStr+rowParamsStr+SAVING_ROWDATA_SEPERATOR;
	}
	if(resultParamsStr.lastIndexOf(SAVING_ROWDATA_SEPERATOR)==resultParamsStr.length-SAVING_ROWDATA_SEPERATOR.length)
	{
		resultParamsStr=resultParamsStr.substring(0,resultParamsStr.length-SAVING_ROWDATA_SEPERATOR.length);
	}
	if(resultParamsStr=='') return '';
	if(paramsKey!=null&&paramsKey!='')
	{//需要将数据存放到WX_ALL_SAVEING_DATA中供回调函数使用
		if(WX_ALL_SAVEING_DATA==null) WX_ALL_SAVEING_DATA=new Object();
		WX_ALL_SAVEING_DATA[paramsKey]=datasObjArr;//以便保存回调函数想使用时可以取到
	}
	return resultParamsStr;
}

/*******************************行列固定的报表**********************************************/
/**
 * 定义冻结行列标题的表格对象
 */
function fixedRowColTable(paramsObj)
{
	new fixedRowColTableObj(paramsObj);//一定要采用这种方式，如果直接调用fixedRowColTableObj(paramsObj)，则当一个页面中存在多个固定行列的报表时不能正常运行
}

var WX_M_FIXEDCOLSWIDTH;//存放某个报表中每个冻结列的宽度，这样可以不过如何列选择，都能保证冻结列的宽度不变

function fixedRowColTableObj(paramsObj)
{
	var pageid=paramsObj.pageid;
	var reportid=paramsObj.reportid;
	var reportguid=getComponentGuidById(pageid,reportid);
	var reportMetadataObj=getReportMetadataObj(reportguid);
	var _fixedRowsCount=reportMetadataObj.metaDataSpanObj.getAttribute('ft_fixedRowsCount');//固定行数
	var _fixedColids=reportMetadataObj.metaDataSpanObj.getAttribute('ft_fixedColids');//固定列的colid集合
	var _totalColCount=reportMetadataObj.metaDataSpanObj.getAttribute('ft_totalColCount');//总列数（包括隐藏列、显示列、冻结列和普通列）
	if((_fixedRowsCount==null||_fixedRowsCount=='')&&(_fixedColids==null||_fixedColids=='')) return;
	if(_totalColCount==null||_totalColCount=='') return;
	this.fixedRowsCount=parseInt(_fixedRowsCount);
	if(_fixedColids!=null&&_fixedColids!='')
	{//有冻结列
		var colidsArr=new Array();
		var tmpArr=_fixedColids.split(';');
		for(var i=0,len=tmpArr.length;i<len;i++)
		{
			if(tmpArr[i]==null||tmpArr[i]=='') continue;
			colidsArr[colidsArr.length]=tmpArr[i];
		}
		this.fixedColidsArr=colidsArr;//存放所有冻结列的colid的数组
		this.fixedColsCount=colidsArr.length;//冻结列数
	}else
	{
		this.fixedColsCount=0;
		this.fixedColidsArr=null;
	}
	this.colcnt=parseInt(_totalColCount);
	if(this.fixedRowsCount<0) this.fixedRowsCount=0;
	if(this.fixedColsCount<0) this.fixedColsCount=0;
	if(this.colcnt<=0) return;
	this.tableObj = document.getElementById(reportguid+'_data');
	//建立<colgroup/>及其所有子<col/>
	var trObjs = this.tableObj.tBodies[0].rows;
	if(trObjs==null||trObjs.length<=0) return;//没有记录行
	if(this.fixedRowsCount>trObjs.length) this.fixedRowsCount=trObjs.length;
	/**
	 * 创建包裹各部分<table/>的<div/>标签并为表格创建一个<colgroup/>
	 */
	this.divContainerObj = document.createElement("DIV");
	this.divFixedHeaderObj = this.divContainerObj.cloneNode(false);//左上角固定标题部分
	this.divHeaderObj = this.divContainerObj.cloneNode(false);//整个标题行部分的外层标签
	this.divHeaderInnerObj = this.divContainerObj.cloneNode(false);//整个标题行部分的内层标签
	this.divFixedDataObj = this.divContainerObj.cloneNode(false);//整个数据部分的外层标签
	this.divFixedDataInnerObj = this.divContainerObj.cloneNode(false);//整个数据部分的内存标签
	this.divDataObj = this.divContainerObj.cloneNode(false);//显示的数据部分
	this.colGroupObj = document.createElement("COLGROUP");
	
	this.tableObj.style.margin = "0px";
	if (this.tableObj.getElementsByTagName("COLGROUP").length > 0) 
	{
		this.tableObj.removeChild(this.tableObj.getElementsByTagName("COLGROUP")[0]);//删除掉已有的<colgroup/>
	}
	this.parentDivObj = this.tableObj.parentNode;//表格外层的<div/>
	this.tableParentDivHeight = this.parentDivObj.offsetHeight;
	this.tableParentDivWidth = this.parentDivObj.offsetWidth;
	if(this.parentDivObj.style.height==null||this.parentDivObj.style.height=='')
	{//没有为外层<div/>配置高度，则加上高度，否则显示不出滚动条
		this.parentDivObj.style.height=this.tableParentDivHeight+'px';
	}
	//加上样式
	this.divContainerObj.className = "cls-fixed-divcontainer";
	//this.divContainerObj.id=reportguid+'_fixeddata_container';//标识最外层
	this.divFixedHeaderObj.className = "cls-fixed-fixedHeader";
	this.divHeaderObj.className = "cls-fixed-header";
	this.divHeaderInnerObj.className = "cls-fixed-headerInner";
	this.divFixedDataObj.className = "cls-fixed-fixeddata";
	this.divFixedDataInnerObj.className = "cls-fixed-fixeddataInner";
	this.divDataObj.className = "cls-fixed-data";
	this.divDataObj.id=reportguid+'_fixeddata';//标识显示的数据部分，后面对数据的操作都是针对此<div/>下面的<table/>进行，比如点击标题上的“全选”复选框，选中的就是这里的表格数据，不能选中其它几个<div/>中的表格数据
	
	//将表格复制几份进行显示
	var alpha, beta;
	this.headerTableObj = this.tableObj.cloneNode(false);
	if(this.tableObj.tHead) 
	{//如果数据表格有<thead/>
		alpha = this.tableObj.tHead;
		this.headerTableObj.appendChild(alpha.cloneNode(false));
		beta = this.headerTableObj.tHead;
	}else 
	{
		alpha = this.tableObj.tBodies[0];
		this.headerTableObj.appendChild(alpha.cloneNode(false));
		beta = this.headerTableObj.tBodies[0];
	}
	alpha = alpha.rows;
	for (var i=0; i<this.fixedRowsCount; i++) 
	{
		beta.appendChild(alpha[i].cloneNode(true));
	}
	this.divHeaderInnerObj.appendChild(this.headerTableObj);
	
	if (this.fixedColsCount > 0) 
	{//指定了固定的列数
		this.fixedHeaderTableObj = this.headerTableObj.cloneNode(true);
		this.divFixedHeaderObj.appendChild(this.fixedHeaderTableObj);
		this.sFDataTable = this.tableObj.cloneNode(true);
		this.divFixedDataInnerObj.appendChild(this.sFDataTable);
	}
	var validTrObj=getValidRowidx(trObjs,this.colcnt);
	var cellidx=0;
	for (var i=0,len=validTrObj.cells.length; i<len; i++) 
	{
		var colspans=validTrObj.cells[i].colSpan;
		var isHiddenCol=validTrObj.cells[i].style.display=='none';//当前列是否是隐藏列
		var colwidth=validTrObj.cells[i].offsetWidth;
		if(colspans > 1)
		{//如果当前列colspan大于1，即占据多个单元格，则为每个单元格创建一个<col/>
			if(!isHiddenCol) colwidth=colwidth/colspans;
		}else
		{
			colspans=1;
		}
		for (var k=0;k<colspans;k++) 
		{//为每个单元格创建一个<col/>
			var colObjTmp=document.createElement("COL");
			if(isHiddenCol)
			{//如果是隐藏<td/>，则将此<col/>也隐藏起来，否则会干扰显示
				if(!isIE)
				{//IE的话，不能加进去，加进去后设置display=none无效
					this.colGroupObj.appendChild(colObjTmp);
					this.colGroupObj.lastChild.style.display='none';
				}
			}else
			{
				this.colGroupObj.appendChild(colObjTmp);
				if(cellidx<this.fixedColsCount)
				{//当前显示列是冻结列
					if(WX_M_FIXEDCOLSWIDTH==null) WX_M_FIXEDCOLSWIDTH=new Object();
					var wid=WX_M_FIXEDCOLSWIDTH[reportguid+this.fixedColidsArr[cellidx]];
					if(wid==null||wid=='')
					{//缓存中还没有缓存它的宽度，则先缓存起来
						WX_M_FIXEDCOLSWIDTH[reportguid+this.fixedColidsArr[cellidx]]=colwidth;
						wid=colwidth;
					}
					this.colGroupObj.lastChild.setAttribute("width",wid);
				}else
				{//普通列
					this.colGroupObj.lastChild.setAttribute("width",colwidth);
				}
				cellidx++;
			}
		}
	}
	
	//将创建的<colgroup/>插到每个部分数据表格最前面，做为它们的第一个子标签
	this.tableObj.insertBefore(this.colGroupObj.cloneNode(true), this.tableObj.firstChild);
	this.headerTableObj.insertBefore(this.colGroupObj.cloneNode(true), this.headerTableObj.firstChild);
	if (this.fixedColsCount > 0) 
	{
		this.sFDataTable.insertBefore(this.colGroupObj.cloneNode(true), this.sFDataTable.firstChild);
		this.fixedHeaderTableObj.insertBefore(this.colGroupObj.cloneNode(true), this.fixedHeaderTableObj.firstChild);
	}
	
	if (this.fixedColsCount > 0) this.sFDataTable.className += " cls-fixed-cols";//被固定列的样式
	
	/**
	 * 将所有新创建的<div/>及其<table/>加入最外层的<div/>中
	 */
	if (this.fixedColsCount > 0) 
	{
		this.divContainerObj.appendChild(this.divFixedHeaderObj);
	}
	this.divHeaderObj.appendChild(this.divHeaderInnerObj);
	this.divContainerObj.appendChild(this.divHeaderObj);
	if (this.fixedColsCount > 0) 
	{
		this.divFixedDataObj.appendChild(this.divFixedDataInnerObj);
		this.divContainerObj.appendChild(this.divFixedDataObj);
	}
	this.divContainerObj.appendChild(this.divDataObj);
	this.parentDivObj.insertBefore(this.divContainerObj, this.tableObj);
	this.divDataObj.appendChild(this.tableObj);
	
	/**
	 * 对齐所有<table/>，以便显示为一个整体
	 */
	var sDataStyles, sDataTableStyles;
	this.sHeaderHeight = this.tableObj.tBodies[0].rows[(this.tableObj.tHead) ? 0 : this.fixedRowsCount].offsetTop;
	sDataTableStyles = "margin-top: " + (this.sHeaderHeight * -1) + "px;";
	sDataStyles = "margin-top: " + this.sHeaderHeight + "px;";
	sDataStyles += "height: " + (this.tableParentDivHeight - this.sHeaderHeight) + "px;";
	if (this.fixedColsCount > 0) 
	{
		//找到被固定列的下一列，以便取到下一列的左边距
		var firstNonFixedColObj=null;
		for(var i=0,len=trObjs.length;i<len;i++)
		{
			for(var j=0,len2=trObjs[i].cells.length;j<len2;j++)
			{
				if(trObjs[i].cells[j].getAttribute('first_nonfixed_col')=='true')
				{
					firstNonFixedColObj=trObjs[i].cells[j];
					break;
				}
			}
			if(firstNonFixedColObj!=null) break;
		}
		this.sFHeaderWidth=-1;
		if(firstNonFixedColObj!=null) this.sFHeaderWidth = firstNonFixedColObj.offsetLeft;
		if (window.getComputedStyle) 
		{
			alpha = document.defaultView;
			beta = this.tableObj.tBodies[0].rows[0].cells[0];
			if (navigator.taintEnabled) 
			{ /* If not Safari */
				this.sFHeaderWidth += Math.ceil(parseInt(alpha.getComputedStyle(beta, null).getPropertyValue("border-right-width")) / 2);
			} else 
			{
				this.sFHeaderWidth += parseInt(alpha.getComputedStyle(beta, null).getPropertyValue("border-right-width"));
			}
		} else if (isIE) 
		{ /* Internet Explorer */
			alpha = this.tableObj.tBodies[0].rows[0].cells[0];
			beta = [alpha.currentStyle["borderRightWidth"], alpha.currentStyle["borderLeftWidth"]];
			if(/px/i.test(beta[0]) && /px/i.test(beta[1])) 
			{
				beta = [parseInt(beta[0]), parseInt(beta[1])].sort();
				this.sFHeaderWidth += Math.ceil(parseInt(beta[1]) / 2);
			}
		}
		if (window.opera) this.divFixedDataObj.style.height = this.tableParentDivHeight + "px";
		if(this.sFHeaderWidth>=0) 
		{
			this.divFixedHeaderObj.style.width = this.sFHeaderWidth + "px";
			sDataTableStyles += "margin-left: " + (this.sFHeaderWidth * -1) + "px;";
			sDataStyles += "margin-left: " + this.sFHeaderWidth + "px;";
			sDataStyles += "width: " + (this.tableParentDivWidth - this.sFHeaderWidth) + "px;";
		}
	} else 
	{
		sDataStyles += "width: " + this.tableParentDivWidth + "px;";
	}
	this.divDataObj.style.cssText = sDataStyles;
	this.tableObj.style.cssText = sDataTableStyles;
	/**
	 * 加上滚动条事件，以便拉动滚动条时，各table位置能相应变化，保持一个整体
	 */
	(function (obj) 
	 {
		if (obj.fixedColsCount > 0) 
		{
			obj.divDataObj.onscroll = function () {
				obj.divHeaderInnerObj.style.right = obj.divDataObj.scrollLeft + "px";
				obj.divFixedDataInnerObj.style.top = (obj.divDataObj.scrollTop * -1) + "px";
			};
		} else 
		{
			obj.divDataObj.onscroll = function () {
				obj.divHeaderInnerObj.style.right = obj.divDataObj.scrollLeft + "px";
			};
		}
		if (isIE) 
		{ //下面代码为了垃圾回收
			window.attachEvent("onunload", function () {
				obj.divDataObj.onscroll = null;
				obj = null;
			});
		}
	})(this);
};

/**
 * 获取有效记录行用于参考建立<colgroup/>中的<col/>标签
 * 首先找
 */
function getValidRowidx(trObjs,totalcolcnt)
{
	var maxcolcntRowidx=-1;//如果本次所有记录行中的列数都小于totalcolcnt，则这里存放最大列数的行号，稍后就返回此行（一般是本报表有很多隐藏列，且本次只显示标题不显示数据时，则可能出现这种情况）
	var trObjTmp;
	for (var i=0,len=trObjs.length; i<len; i++) 
	{
		trObjTmp=trObjs[i];
		if(maxcolcntRowidx<0||trObjTmp.cells.length>trObjs[maxcolcntRowidx].cells.length) maxcolcntRowidx=i;
		var maxrowspan=1;//本记录行所有列中最大的rowspan数
		var hasColspan2=false;//本记录行中是否存在colspan大于1的列
		for (var j=0,len2=trObjTmp.cells.length;j<len2; j++) 
		{
			if(trObjTmp.cells[j].rowSpan>maxrowspan)
			{//如果当前列的rowspan数大于maxrowspan
				maxrowspan=trObjTmp.cells[j].rowSpan;
			}
			if(hasColspan2) continue;
			if (trObjTmp.cells[j].colSpan>1) hasColspan2=true;
		}
		if(!hasColspan2&&totalcolcnt==trObjTmp.cells.length) return trObjTmp;//如果当前行中没有colspan不为1的列，且列数就是totalcolcnt
		i+=maxrowspan-1;//因为后面还有i++，所以这里要减1
	}
	return trObjs[maxcolcntRowidx];//如果没有取到，则说明没有数据，只有标题部分，因此返回列数最大的行
}

/**
 * 根据报表上某个子元素，判断其所在的报表是否是冻结行列标题的报表，并且返回真正向用户显示数据（即className为cls-fixed-data）的<div/>对象
 * @param reportguid 报表的guid
 * @param elementObj 报表上的元素对象
 */
function getParentFixedDataObj(reportguid,elementObj)
{
	while(true)
	{
		if(elementObj==null) return null;
		if(elementObj.getAttribute('id')=='WX_CONTENT_'+reportguid) return null;//找到此报表最外层的<span/>了还没有找到固定表头的<div/>
		if(elementObj.className=='cls-fixed-divcontainer')
		{//当前报表是冻结行列标题的报表
			return document.getElementById(reportguid+'_fixeddata');
		}
		elementObj=elementObj.parentNode;
	}
}
/***************************************容器相关的方法********************************************/

/**
 * 异步切换tabpanel的标签
 * @param tabpanelid Tab容器的id
 * @param refreshComponentGuid 此Tab容器的刷新GUID
 * @param newitemidx 新Tab项的下标
 */
function shiftTabPanelItemAsyn(pageid,tabpanelid,refreshComponentGuid,newitemidx,beforecallback)
{
	var url=getComponentUrl(pageid,refreshComponentGuid,null);
	url=replaceUrlParamValue(url,tabpanelid+'_selectedIndex',newitemidx);
	//alert(url);
	if(beforecallback!=null&&beforecallback!='')
	{//指定了切换前的回调函数
		url=beforecallback(pageid,tabpanelid,url);
		if(url==null||url=='') return;
	}
	refreshComponent(url,null,{keepSelectedRowsAction:true,keepSavingRowsAction:true});
}

/**
 * 切换tabpanel的标签
 * @param tabpanelid Tab容器的id
 * @param refreshComponentGuid 此Tab容器的刷新GUID
 * @param newitemidx 新Tab项的下标
 */
function shiftTabPanelItemSyn(pageid,tabpanelid,refreshComponentGuid,newitemidx)
{
	var panelguid=getComponentGuidById(pageid,tabpanelid);
	var newTitleObj=document.getElementById(panelguid+'_'+newitemidx+'_title');//取到标题所在<td/>的对象
	var titleTableObj=getParentElementObj(newTitleObj,'TABLE');//取到标题所在<table/>对象
	var selectedItemIndex=titleTableObj.getAttribute('selectedItemIndex');//取到当前被选中的标签页下标
	if(selectedItemIndex==null||selectedItemIndex=='') selectedItemIndex='0';
	if(newitemidx==selectedItemIndex) return;//当前点中的标签页是已被选中的标签页
	//将已被选中的标签页内容隐藏、将本次被选中显示的标签页内容显示
	var oldItemObj=document.getElementById(panelguid+'_'+selectedItemIndex+'_content');//取到上次被选中的标签页内容<table/>
	oldItemObj.style.display='none';
	var newItemObj=document.getElementById(panelguid+'_'+newitemidx+'_content');
	newItemObj.style.display='';
	//修改标签页标题样式
	var oldTitleObj=document.getElementById(panelguid+'_'+selectedItemIndex+'_title');
	var newTitleObj=document.getElementById(panelguid+'_'+newitemidx+'_title');
	var oldClassName=oldTitleObj.className;
	oldTitleObj.className=newTitleObj.className;
	newTitleObj.className=oldClassName;
	var newtabitem_position_type=newTitleObj.getAttribute('tabitem_position_type');
	var oldtabitem_position_type=oldTitleObj.getAttribute('tabitem_position_type');
	if(newtabitem_position_type=='first'||newtabitem_position_type=='middle'||newtabitem_position_type=='last')
	{//是显示在顶部的titlestyle为2的标题，则除了要更新标题样式，还要更新其图片
		changeImgForTabItemTitle(panelguid,newitemidx,selectedItemIndex,newtabitem_position_type,oldtabitem_position_type);
	}
	titleTableObj.setAttribute('selectedItemIndex',newitemidx);//将新选中的标签页下标保存起来
	//更新页面URL中此tab标签的选中标签页下标
	var url=getComponentUrl(pageid,refreshComponentGuid,null);
	url=replaceUrlParamValue(url,tabpanelid+'_selectedIndex',newitemidx);
	var pageurlSpanObj=document.getElementById(pageid+'_url_id');
	pageurlSpanObj.setAttribute('value',url);
}

/**
 * 
 * @param panelguid tabpanel的guid
 * @param newitemidx 新标签页的下标
 * @param oldItemIndex 旧标签页的下标
 * @param newtabitem_position_type 新选中标签页的位置类型
 * @param oldtabitem_position_type 旧标签页的位置类型
 */
function changeImgForTabItemTitle(panelguid,newitemidx,olditemidx,newtabitem_position_type,oldtabitem_position_type)
{
	var doc=document;
	var oldItemImgObj=doc.getElementById(panelguid+'_'+olditemidx+'_rightimg');//旧标签页对应的右边的图片
	var newItemImgObj=doc.getElementById(panelguid+'_'+newitemidx+'_rightimg');//新选中标签页对应的右边的图片
	//取到标题上的图片存放路径
	var imgsrc=oldItemImgObj.src;
	var idx=imgsrc.lastIndexOf('/');
	var imgpath=imgsrc.substring(0,idx)+'/';//存放标题部分图片的路径
	
	var inewitemidx=parseInt(newitemidx,10);
	var iolditemidx=parseInt(olditemidx,10);
	if(inewitemidx-iolditemidx==1)
	{//如果新标签是旧标签的下一个标签
		oldItemImgObj.src=imgpath+'title2_deselected_selected.gif';
		if(oldtabitem_position_type!='first')
		{
			doc.getElementById(panelguid+'_'+(iolditemidx-1)+'_rightimg').src=imgpath+'title2_deselected_deselected.gif';//本标签的上一个标题后面的图片也要改变
		}
		if(newtabitem_position_type=='last')
		{//如果新选中的标签是最后一个标签页
			newItemImgObj.src=imgpath+'title2_selected.gif';
		}else
		{
			newItemImgObj.src=imgpath+'title2_selected_deselected.gif';
		}
	}else if(iolditemidx-inewitemidx==1)
	{//如果旧标签是新标签的下一个标签
		newItemImgObj.src=imgpath+'title2_selected_deselected.gif';
		if(newtabitem_position_type!='first')
		{
			doc.getElementById(panelguid+'_'+(inewitemidx-1)+'_rightimg').src=imgpath+'title2_deselected_selected.gif';//本标签的上一个标题后面的图片也要改变
		}
		if(oldtabitem_position_type=='last')
		{//如果旧标签是最后一个标签页
			oldItemImgObj.src=imgpath+'title2_deselected.gif';
		}else
		{
			oldItemImgObj.src=imgpath+'title2_deselected_deselected.gif';
		}
	}else
	{//新旧标签没有挨在一起
		if(oldtabitem_position_type=='first')
		{
			oldItemImgObj.src=imgpath+'title2_deselected_deselected.gif';
		}else if(oldtabitem_position_type=='middle')
		{
			oldItemImgObj.src=imgpath+'title2_deselected_deselected.gif';
			doc.getElementById(panelguid+'_'+(iolditemidx-1)+'_rightimg').src=imgpath+'title2_deselected_deselected.gif';//本标签的上一个标题后面的图片也要改变
		}else if(oldtabitem_position_type=='last')
		{
			oldItemImgObj.src=imgpath+'title2_deselected.gif';
			doc.getElementById(panelguid+'_'+(iolditemidx-1)+'_rightimg').src=imgpath+'title2_deselected_deselected.gif';//本标签的上一个标题后面的图片也要改变
		}
		if(newtabitem_position_type=='first')
		{
			newItemImgObj.src=imgpath+'title2_selected_deselected.gif';
		}else if(newtabitem_position_type=='middle')
		{
			newItemImgObj.src=imgpath+'title2_selected_deselected.gif';
			doc.getElementById(panelguid+'_'+(inewitemidx-1)+'_rightimg').src=imgpath+'title2_deselected_selected.gif';//本标签的上一个标题后面的图片也要改变
		}else if(newtabitem_position_type=='last')
		{
			newItemImgObj.src=imgpath+'title2_selected.gif';
			doc.getElementById(panelguid+'_'+(inewitemidx-1)+'_rightimg').src=imgpath+'title2_deselected_selected.gif';//本标签的上一个标题后面的图片也要改变
		}
	}
}

/**
 * 当标题显示在顶部，且titlestyle=2时，调整图片的高度与所在<td/>的高度保持一致
 */
function adjustTabItemTitleImgHeight(paramsObj)
{
	if(paramsObj==null||paramsObj.tabpanelguid==null||paramsObj.tabpanelguid==''||paramsObj.tabitemcount==null) return;
	var doc=document;
	var imgObjTmp;
	for(var i=0;i<paramsObj.tabitemcount;i++)
	{
		imgObjTmp=doc.getElementById(paramsObj.tabpanelguid+'_'+i+'_rightimg');
		if(imgObjTmp!=null) imgObjTmp.style.height=imgObjTmp.parentNode.clientHeight+'px';
	}
}
/********************************************打印方法*******************************************/
var LODOP_OBJ;//lodop打印全局对象
/**
 * 打印某个组件
 * @param includeApplicationIds：本次打印需要包含的应用ID
 * @param printtype：打印类型，可取值包括：print(直接打印)、printpreview(打印预览)、printsetting(打印设置)
 */
function printComponentsData(pageid,componentid,includeApplicationIds,printtype)
{
	if(includeApplicationIds==null||includeApplicationIds=='') return;
	var appidsArr=parseStringToArray(includeApplicationIds,';',false);
	if(appidsArr==null||appidsArr.length==0) return;
	var url='';
	if(appidsArr.length==1)
	{//只有一个应用
		var appguid=getComponentGuidById(pageid,appidsArr[0]);
		var metadataObj=getReportMetadataObj(appguid);
		if(metadataObj==null)
		{//说明不是报表，是其它类型的应用（比如树、html、jsp等）
			url=getComponentUrl(pageid,null,null);
		}else
		{//是报表
			url=getComponentUrl(pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
		}
	}else
	{//有多个应用
		url=getComponentUrl(pageid,null,null);
		var metadataObjTmp=null;
		for(var i=0,len=appidsArr.length;i<len;i++)
		{
			metadataObjTmp=getReportMetadataObj(getComponentGuidById(pageid,appidsArr[i]));
			if(metadataObjTmp==null) continue;//不是报表应用
			if(metadataObjTmp.slave_reportid==appidsArr[i])
			{//是从报表，将它自己的URL合并到页面URL（因为要带上它的条件参数或者翻页参数等信息）
				var myurl=getComponentUrl(pageid,null,appidsArr[i]);
				url=mergeUrlParams(url,myurl,false);
				url=replaceUrlParamValue(url,'SLAVE_REPORTID',null);
			}
		}
	}
	url=replaceUrlParamValue(url,'COMPONENTIDS',componentid);
	url=replaceUrlParamValue(url,'INCLUDE_APPLICATIONIDS',includeApplicationIds);
	url=replaceUrlParamValue(url,'DISPLAY_TYPE','6');//表示正在打印
	url=replaceUrlParamValue(url,'WX_ISAJAXLOAD','true');//加上本次是ajax加载的标识
	//alert(url);
	var urlparams=url.substring(url.indexOf('?')+1);
	url=url.substring(0,url.indexOf('?'));
	var dataObj=new Object();
	dataObj['pageid']=pageid;
	dataObj['printtype']=printtype;
	XMLHttpREPORT.sendReq('POST',url,urlparams,printCallBack,onPrintErrorMethod,dataObj);
	
}

function printCallBack(xmlHttpObj,dataObj)
{
	var mess=xmlHttpObj.responseText;
	//alert(mess);
	var pageid=dataObj.pageid;
	var printtype=dataObj.printtype;
	var idx1=mess.indexOf('<RESULTS_INFO-'+pageid+'>');
	var idx2=mess.indexOf('</RESULTS_INFO-'+pageid+'>');
	var jsonResultStr=null;
	if(idx1>=0&&idx2>=0&&idx2>idx1)
	{
		jsonResultStr=mess.substring(idx1+('<RESULTS_INFO-'+pageid+'>').length,idx2);
		mess=mess.substring(0,idx1)+mess.substring(idx2+('</RESULTS_INFO-'+pageid+'>').length);
	}
	var jsonResultsObj=getObjectByJsonString(jsonResultStr);
	var onloadMethods=jsonResultsObj.onloadMethods;
   if(onloadMethods&&onloadMethods!='')
   {
   	var jobname='';
   	idx1=mess.indexOf('<print-jobname-'+pageid+'>');
   	idx2=mess.indexOf('</print-jobname-'+pageid+'>');
   	if(idx1>=0&&idx2>idx1)
   	{
   		jobname=mess.substring(idx1+('<print-jobname-'+pageid+'>').length,idx2);
			mess=mess.substring(0,idx1)+mess.substring(idx2+('</print-jobname-'+pageid+'>').length);
   	}
   	onloadMethods[0].methodname(jobname,mess,printtype);
   }
}

function onPrintErrorMethod(xmlHttpObj)
{
	wx_error('打印失败');
}

/**
 * 根据传入的打印字符串解析出指定位置的打印内容，默认的window.print()打印方式不会用到此方法，因为它是弹出一个页面，然后对整个<body/>内容进行打印
 * @param printtext 传入的完整打印字符串
 * @param placeholder 在完整打印字符串中用<placeholder></placeholder>括住要打印的内容
 */
function getAdvancedPrintRealValue(printtext,placeholder)
{
	if(printtext==null||printtext==''||placeholder==null||placeholder=='') return '';
	var starttag='<'+placeholder+'>';
	var endtag='</'+placeholder+'>';
	var idx=printtext.indexOf(starttag);
	if(idx<0) return '';
	printtext=printtext.substring(idx+starttag.length);
	idx=printtext.indexOf(endtag);
	if(idx<0) return printtext;
	return printtext.substring(0,idx);
}

/**
 * 根据传入的打印字符串解析出指定页指定位置的打印内容
 * @param printtext 传入的完整打印字符串
 * @param pageplaceholder 指定页的占位符
 * @param placeholder 此页指定元素的占位符，如果传入空，则说明此页只有一个动态元素，比如一个<subpage/>下面就是一个动态或静态模板，则没有子元素
 */
function getAdvancedPrintRealValueForPage(printtext,pageplaceholder,placeholder)
{
	if(printtext==null||printtext==''||pageplaceholder==null||pageplaceholder=='') return '';
	//先根据此页占位符，取到此页的所有打印内容
	var starttag='<'+pageplaceholder+'>';
	var endtag='</'+pageplaceholder+'>';
	var idx=printtext.indexOf(starttag);
	if(idx<0) return '';
	printtext=printtext.substring(idx+starttag.length);
	idx=printtext.indexOf(endtag);
	if(idx>=0) printtext=printtext.substring(0,idx);
	if(placeholder==null||placeholder=='') return printtext;//如果一页就是一个动态元素，比如一个<subpage/>下面就是一个动态或静态模板，则没有子元素
	//再取到此页中此动态元素打印内容
	starttag='<'+placeholder+'>';
	endtag='</'+placeholder+'>';
	idx=printtext.indexOf(starttag);
	if(idx<0) return '';
	printtext=printtext.substring(idx+starttag.length);
	idx=printtext.indexOf(endtag);
	if(idx<0) return printtext;
	return printtext.substring(0,idx);
}

/**
 * 获取总页数
 */
function getPrintPageCount(printtext,placeholder)
{
	if(printtext==null||printtext==''||placeholder==null||placeholder=='') return 0;
	var starttag='<'+placeholder+'_pagecount>';
	var endtag='</'+placeholder+'_pagecount>';
	var idx=printtext.indexOf(starttag);
	if(idx<0) return 0;
	printtext=printtext.substring(idx+starttag.length);
	idx=printtext.indexOf(endtag);
	if(idx<0) return 0;
	return parseInt(printtext.substring(0,idx),10);
}
/********************************************打印方法*******************************************/
/**
 * 跳转到目标页面
 */
function wx_sendRedirect(paramsObj)
{
	if(paramsObj==null||paramsObj.url==null||paramsObj.url=='') return;
	window.location.href=paramsObj.url;
}

/**
 * 提交文件上传表单进行文件上传操作
 */
function doFileUploadAction()
{
	//document.wx_fileupload_form.submit();
	displayLoadingMessage();
	return true;
}

/**********************************************************************************************/
/************************************图表报表方法************************************************/
/**
 * 显示fusioncharts图表
 */
function displayFusionChartsData(paramsObj)
{
	var chartid=paramsObj.pageid+paramsObj.reportid+parseInt(Math.random()*10000000);
	var chartObj=new FusionCharts(paramsObj.swfileurl,chartid,paramsObj.width, paramsObj.height,paramsObj.debugMode,paramsObj.registerWithJS);
	if(paramsObj.datatype=='xml')
	{
		chartObj.setXMLData(paramdecode(paramsObj.data));
	}else if(paramsObj.datatype=='xmlurl')
	{
		chartObj.setXMLUrl(WXConfig.webroot+'wxtmpfiles/chartdata/'+paramsObj.data);
	}else
	{//xmlurl-servlet
		var url=WXConfig.showreport_url;
		var link=url.indexOf('?')>0?'&':'?';
		url=url+link+'ACTIONTYPE=loadChartXmlFile&xmlfilename='+paramsObj.data;
		chartObj.setXMLUrl(url);
	}
	chartObj.render(getComponentGuidById(paramsObj.pageid,paramsObj.reportid)+"_data");
	var onloadMethods=paramsObj.chartOnloadMethods;
	paramsObj.chartOnloadMethods=null;//清空以便给onload函数使用
	paramsObj.chartid=chartid;
	paramsObj.chartObj=chartObj;
	if(onloadMethods!=null&&onloadMethods.length>0)
	{
		for(var i=0;i<onloadMethods.length;i++)
		{
			paramsObj.customizeData=onloadMethods[i].methodparams;
			onloadMethods[i].method(paramsObj);
		}
	}
}

/**********************************************************************************************/
var WX_ISSYSTEM_LOADED=true;//用于标识此js文件加载完