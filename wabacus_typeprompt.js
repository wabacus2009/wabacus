var TYPE_PROMPT_theTextBox;//当前正在输入的文本框
var TYPE_PROMPT_objLastActive;//最后一次激活的控件，用于判断是否是第一次进入当前输入框
var TYPE_PROMPT_selectSpanTitleStart="<span style='width:100%;display:block;' class='spanOutputTitleElement'>";
var TYPE_PROMPT_selectSpanStart="<span style='width:100%;display:block;' class='spanOutputNormalElement' onmouseover='setTypePromptHighColor(this)' ";
var TYPE_PROMPT_selectSpanEnd="</span>";

/**
 *textboxObj：需要输入前提示的文本框对象
 */
function initializeTypePromptProperties(textboxObj,params)
{
	if(textboxObj.typePromptObj==null)
	{//还没有初始化
		var props={
			 elem:textboxObj,
	       paramsObj:null,
			 currentRecordCount:0,//当前输入框提示条目的数量
			 spanOutputId:"",//输入提示框的ID
			 timer:false,//定时器
			 resultItemsXmlRoot:null,//从服务器端获取的所有输入提示选项的XML数据
			 resultItemsCount:0,//从服务器端获取的所有输入提示选项的数量
			 strLastValue:"",//文本框最后的值
			 bMadeRequest:false,//是否正在向服务器端请求数据
			 currentValueSelected:-1,//当前选中的输入提示项下标
			 bNoResults:false//是否从服务器端请求到与用户输入相关的提示结果
		};
		if(textboxObj.id)
		{//如果输入文本框有ID属性，则用其ID属性构造相应提示框的SPAN的ID
			props.spanOutputId="spanOutput_"+textboxObj.id;
		}else
		{//如果没有ID属性，则用它的name属性
			props.spanOutputId="spanOutput_"+textboxObj.name;
		}
		//alert(props.spanOutputId);
		params=paramdecode(params);
		props.paramsObj=eval('('+params+')');
		//创建输入前提示所用的span框
		var spanobj=document.getElementById(props.spanOutputId);
		if(spanobj==null)
		{
			var elemSpan=document.createElement("span");
			elemSpan.id=props.spanOutputId;
			elemSpan.className="spanOutputTextTypePrompt";
			document.body.appendChild(elemSpan);
		}
		//alert(props.paramsObj.spanOutputWidth);
		if(props.paramsObj.spanOutputWidth==null||props.paramsObj.spanOutputWidth<=0)
		{
			props.paramsObj.spanOutputWidth=textboxObj.offsetWidth;
		}
		if(props.paramsObj.resultCount==null||props.paramsObj.resultCount<=0)
		{
			props.paramsObj.resultCount=15;//如果没有输入显示的提示结果数量，则显示15条
		}
		//alert(props.spanOutputWidth);
		textboxObj.onkeyup=doKeyUpEvent;
		textboxObj.onkeydown=doKeyDownEvent;
		textboxObj.typePromptObj=props;
	}
	if(textboxObj.typePromptObj.paramsObj.isSelectBox===true) getTypeAheadDataOnFocus(textboxObj)
}

/**
 * 如果当前输入联想是模拟可输入下拉框的效果，则在onfocus时就要弹出联想选项
 */
function getTypeAheadDataOnFocus(textboxObj)
{
	TYPE_PROMPT_theTextBox=textboxObj;
	TYPE_PROMPT_objLastActive= TYPE_PROMPT_theTextBox;
	TYPE_PROMPT_theTextBox.typePromptObj.bMadeRequest=true;
	var boxVal=TYPE_PROMPT_theTextBox.value;
	if(boxVal==TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.conditionlabel) boxVal='';//如果当前输入框是查询条件的输入框，且label是显示在输入框里面
	getTypeAheadData(boxVal);
	TYPE_PROMPT_theTextBox.typePromptObj.strLastValue=boxVal;
}

/**
 * 特殊按键ascii码
 * Backspace 8 		Print Screen 44 		Tab 9					Home 36
 *	Delete 46 			Enter 13 				F1 112				F11 122
 * Shift 16				F2 113					Ctrl 17				F12 123
 *	F3 114				Alt 18					F4 115				F10 121
 * Pause/Break 19 	F5 116					Caps Lock 20		F9 120
 * F6 117				Esc 27					F7 118				F8 119
 * Page Up 33			Page Down 34									End 35
 *	Left Arrow 37		Up Arrow 38				Right Arrow 39		Down Arrow 40	
 */

function doKeyDownEvent(e)
{
	var event;
	var intKey;
	if(window.event)
	{
		event=window.event;
		intKey=window.event.keyCode;
	}else
	{
		event=e;
		intKey=e.which;
	}
	if(TYPE_PROMPT_objLastActive== TYPE_PROMPT_theTextBox)
	{//不是刚进入此文本输入框
		if(intKey==38)
		{//UP键
			//alert('2');
			moveTypePromptHighlight(-1);
		}else if(intKey==40)
		{//DOWN键
			//alert('1');
			moveTypePromptHighlight(1);
		}else if(intKey==27)
		{//ESC键
			TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected=-1;
			hideTypePromptTheBox(TYPE_PROMPT_theTextBox);
			TYPE_PROMPT_theTextBox.value=TYPE_PROMPT_theTextBox.typePromptObj.strLastValue;//将用户已经输入的字符串填到文本框中，这对IE有用，因为按ESC键时，系统会自动将以前的字符串填入，因此这里需要将用户输入的填入。
		}else if(intKey==13)
		{//ENTER键
			grabTypePromptHighlighted();
			TYPE_PROMPT_theTextBox.blur();
			//阻止onkeypress事件，比如form表单自动提交等动作
			event.returnValue = false;
			if (event.preventDefault) event.preventDefault();
		}
	}
}


/**
 *处理用户keyup按键
 */
function doKeyUpEvent(e)
{
	var intKey=-1;
	if(window.event)
	{
		intKey=window.event.keyCode;
		TYPE_PROMPT_theTextBox=window.event.srcElement;
	}else{
		intKey=e.which;
		TYPE_PROMPT_theTextBox=e.target;
	}
	//alert(intKey);
	if(TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.timeoutSecond>0)
	{
		if(TYPE_PROMPT_theTextBox.typePromptObj.timer)
		{
			eraseTypePromptTimeout();
		}
		startTypePromptTimeout();
	}
	if(TYPE_PROMPT_theTextBox.value.length==0&&TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.isSelectBox!==true)
	{//输入框中没有内容，且不是下拉框形式
		TYPE_PROMPT_theTextBox.typePromptObj.resultItemsXmlRoot=null;
		hideTypePromptTheBox(TYPE_PROMPT_theTextBox);
		TYPE_PROMPT_theTextBox.typePromptObj.strLastValue="";
		return false;
	}
	
	if(TYPE_PROMPT_objLastActive== TYPE_PROMPT_theTextBox)
	{//不是刚进入此文本输入框
		if((intKey< 32&&intKey!=8)||(intKey >= 33 && intKey < 46)|| (intKey >= 112 && intKey <= 123)) return false;
	}
	//alert(TYPE_PROMPT_strLastValue);
	//window.status=TYPE_PROMPT_theTextBox.typePromptObj.resultItemsCount+'  '+TYPE_PROMPT_theTextBox.typePromptObj.bNoResults;
	if(TYPE_PROMPT_objLastActive!= TYPE_PROMPT_theTextBox//第一次进入此输入框时
	   ||TYPE_PROMPT_theTextBox.value.indexOf(TYPE_PROMPT_theTextBox.typePromptObj.strLastValue)!=0//与之前的输入起始字符不同
	   ||((TYPE_PROMPT_theTextBox.typePromptObj.resultItemsCount==0||TYPE_PROMPT_theTextBox.typePromptObj.resultItemsCount==TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.resultCount)&&!TYPE_PROMPT_theTextBox.typePromptObj.bNoResults)//第一次进入，或之前从服务器中查询的输入提示项超过指定的最大条数(这种情况再次输入字符时也需要去服务器中查询)
  	   ||TYPE_PROMPT_theTextBox.value.length<TYPE_PROMPT_theTextBox.typePromptObj.strLastValue.length//在之前输入上删掉了字符
	   ||TYPE_PROMPT_theTextBox.typePromptObj.strLastValue=='')
	{//以上情况都需要从服务器中查询最新的输入提示项
		TYPE_PROMPT_objLastActive= TYPE_PROMPT_theTextBox;
		TYPE_PROMPT_theTextBox.typePromptObj.bMadeRequest=true;
		getTypeAheadData(TYPE_PROMPT_theTextBox.value);
	}else if(!TYPE_PROMPT_theTextBox.typePromptObj.bMadeRequest)
	{//当不需从服务器加载新输入前提示信息，且服务器端之前的加载都已完成时，只需从缓存中根据之前的输入前提示列表构造新的输入前提示框
		buildTypeAheadBox(TYPE_PROMPT_theTextBox.value);
	}
	TYPE_PROMPT_theTextBox.typePromptObj.strLastValue=TYPE_PROMPT_theTextBox.value;
}

/**
 *获取提示数据列表
 */
function getTypeAheadData(txtValue)
{
	//alert(txtValue);
	if((txtValue==null||txtValue=="")&&TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.isSelectBox!==true) return;
	if(txtValue==null) txtValue='';
	var pageid=TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.pageid;
	var reportid=TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.reportid;
	var inputboxid=TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.inputboxid;
	var reportguid=getComponentGuidById(pageid,reportid);
	var metadataObj=getReportMetadataObj(reportguid);
	var server_url=getComponentUrl(pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
	server_url=replaceUrlParamValue(server_url,"REPORTID",reportid);
	server_url=replaceUrlParamValue(server_url,"INPUTBOXID",inputboxid);
	server_url=replaceUrlParamValue(server_url,"ACTIONTYPE","GetTypePromptDataList");
	server_url=replaceUrlParamValue(server_url,"TYPE_PROMPT_TXTVALUE",txtValue);
	var urlparams=server_url.substring(server_url.indexOf('?')+1);
	server_url=server_url.substring(0,server_url.indexOf('?'));
	XMLHttpREPORT.sendReq('POST',server_url,urlparams,buildChoices,onErrorMethodTypeAhead,null);	
}
/**
 *回调函数
 */
function buildChoices(xmlHttpObj,dataObj)
{
	var tempxml=xmlHttpObj.responseXML;
	//alert(tempxml);
	TYPE_PROMPT_theTextBox.typePromptObj.resultItemsXmlRoot=tempxml.getElementsByTagName("items")[0];//取到XML文档的根<items/>
	if(TYPE_PROMPT_theTextBox.typePromptObj.resultItemsXmlRoot!=null)
	{
		TYPE_PROMPT_theTextBox.typePromptObj.resultItemsCount=TYPE_PROMPT_theTextBox.typePromptObj.resultItemsXmlRoot.childNodes.length;
	}else
	{
		TYPE_PROMPT_theTextBox.typePromptObj.resultItemsCount=0;
	}
    //alert(TYPE_PROMPT_theTextBox.typePromptObj.resultItemsCount);
	TYPE_PROMPT_theTextBox.typePromptObj.bMadeRequest=false;
	buildTypeAheadBox(TYPE_PROMPT_theTextBox.typePromptObj.strLastValue);
}

/**
 *出错处理
 */
function onErrorMethodTypeAhead(xmlHttpObj){
	TYPE_PROMPT_theTextBox.typePromptObj.bMadeRequest=false;
  	wx_warn('获取输入联想数据失败');
}

function buildTypeAheadBox(txtValue)
{
	var matchedResults=makeMatches(txtValue);
	//alert(matchedResults);
	if(matchedResults.length>0)
	{
		var doc=document;
		setTypePromptOutputSpanPosition(TYPE_PROMPT_theTextBox);
		var containerSpanObj=doc.getElementById(TYPE_PROMPT_theTextBox.typePromptObj.spanOutputId);
		containerSpanObj.innerHTML=matchedResults;
		var contentDivObj=doc.getElementById(TYPE_PROMPT_theTextBox.typePromptObj.spanOutputId+'_inner');
		if(contentDivObj!=null)
		{
			var maxheight=TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.spanOutputMaxheight;
			//alert(maxheight);
			if(maxheight==null||maxheight<15) maxheight=350;
			if(contentDivObj.offsetHeight<maxheight-10)
			{
				containerSpanObj.style.height=(contentDivObj.offsetHeight+10)+'px';
			}else
			{
				containerSpanObj.style.height=maxheight+'px';
			}
		}
		doc.getElementById(TYPE_PROMPT_theTextBox.typePromptObj.spanOutputId+"_0").className="spanOutputHighElement";
		TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected=0;
		TYPE_PROMPT_theTextBox.typePromptObj.bNoResults=false;
		EventTools.addEventHandler(window.document,"mousedown",hideTypePromptTheBoxEvent);
	}else
	{
		TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected=-1;
		TYPE_PROMPT_theTextBox.typePromptObj.bNoResults=true;
		hideTypePromptTheBox(TYPE_PROMPT_theTextBox);
	}

}

function hideTypePromptTheBoxEvent(event)
{
	var srcObj=window.event?window.event.srcElement:event.target;
	if(srcObj!=null)
	{
		if(srcObj.getAttribute('id')==TYPE_PROMPT_theTextBox.getAttribute('id')) return;
		if(isElementOrChildElement(srcObj,TYPE_PROMPT_theTextBox.typePromptObj.spanOutputId)) return;
	}
	hideTypePromptTheBox(TYPE_PROMPT_theTextBox);
}

function setTypePromptOutputSpanPosition(theTextBox)
{
	var pos=getElementAbsolutePosition(theTextBox);
   var spanOutput=document.getElementById(theTextBox.typePromptObj.spanOutputId);
	spanOutput.style.left=pos.left+'px';
	//alert(theTextBox.typePromptObj.matchTextBoxWidth);
	//alert(theTextBox.typePromptObj.paramsObj.spanOutputWidth);
	spanOutput.style.width=parseInt(theTextBox.typePromptObj.paramsObj.spanOutputWidth)+'px';
	spanOutput.style.top=(pos.top+pos.height)+'px';
	spanOutput.style.display="block";
	if(theTextBox.typePromptObj.paramsObj.timeoutSecond>0)
	{
		spanOutput.onmouseout=startTypePromptTimeout;
		spanOutput.onmouseover=eraseTypePromptTimeout;
	}else
	{
		spanOutput.onmouseout=null;
		spanOutput.onmouseover=null;
	}
}

function makeMatches(txtValue)
{
	TYPE_PROMPT_theTextBox.typePromptObj.currentRecordCount=0;
	if(TYPE_PROMPT_theTextBox.typePromptObj.resultItemsXmlRoot==null) return '';
	if(TYPE_PROMPT_theTextBox.typePromptObj.resultItemsCount==0) return '';
	var resultstr='';
	var colsArray=TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.colsArray;
	//alert(colsArray);
	//alert(TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.isShowlabel);
	var displaycolcount=0;//要显示多少个列（除了隐藏列）
	for(var i=0,len=colsArray.length;i<len;i++)
	{
		if(colsArray[i].hidden!==true) displaycolcount++;
	}
	if(displaycolcount==0) return '';
	if(TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.isShowTitle)
	{//需要显示标题部分
		resultstr=TYPE_PROMPT_selectSpanTitleStart;
		for(var i=0,len=colsArray.length;i<len;i=i+1)
		{//显示所有标题
			if(colsArray[i].hidden!==true) resultstr=resultstr+"<span style='width:"+99/displaycolcount+"%;display:inline-block;'>"+colsArray[i].coltitle+"</span>";
		}
		resultstr=resultstr+TYPE_PROMPT_selectSpanEnd;
	}
	if(txtValue==null) txtValue='';
	var isSelectBox=TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.isSelectBox;
	var isCasesensitive=TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.isCasesensitive;
	if(isCasesensitive!==true) txtValue=txtValue.toLowerCase();
	var clientMatcherMethod=TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.clientMatchMethod;//用户指定的客户端匹配方法
	//alert(resultstr);
	var allItems=TYPE_PROMPT_theTextBox.typePromptObj.resultItemsXmlRoot.childNodes;
	//alert(allItems.length);
	for(var i=0,len=allItems.length;i<len;i=i+1)
	{
		var eleChild=allItems.item(i);
		var attrs=eleChild.attributes;
		var labelstr='';
		var colParamsTmp='';//存放各列的label和value值，以便在选中回调函数中可以取到
		for(var j=0,len2=colsArray.length;j<len2;j=j+1)
		{//显示所有数据
			var matchmode=colsArray[j].matchmode;
			if(matchmode==null||matchmode=='') matchmode='0';
			var colabelTmp=attrs.getNamedItem(colsArray[j].collabel).value;
			if(colsArray[j].hidden!==true)
			{//参与显示
				if(txtValue!=''&&clientMatcherMethod==null) colabelTmp=formatMatchText(colabelTmp,txtValue,parseInt(matchmode),isCasesensitive);//如果用户自己指定了匹配方法，则不对匹配数据进行格式化显示，因为可能不准确
				labelstr=labelstr+"<span style='width:"+99/displaycolcount+"%;display:inline-block;'>"+colabelTmp+"</span>";
			}
			//拼凑参数以便传入回调函数
			colParamsTmp+=colabelTmp;
			if(colsArray[j].colvalue!=null) colParamsTmp+=';;;'+attrs.getNamedItem(colsArray[j].colvalue).value;
			colParamsTmp+='|||';
		}
		if(colParamsTmp.indexOf(colParamsTmp.length-3,colParamsTmp.length)=='|||') colParamsTmp=colParamsTmp.substring(0,colParamsTmp.length-3);
		//alert(labelstr);
		for(var j=0,len2=colsArray.length;j<len2;j=j+1)
		{//为每列数据加上点击或按回车键后给输入框设置相应数据的功能
			var matchmode=colsArray[j].matchmode;
			if(matchmode==null||matchmode=='') continue;
			var imatchmode=parseInt(matchmode);
			if(imatchmode<=0) continue;
			var label=attrs.getNamedItem(colsArray[j].collabel).value;
			var value=attrs.getNamedItem(colsArray[j].colvalue).value;
			if(label==null) label='';
			if(isCasesensitive!==true) label=label.toLowerCase();
			//alert(matchmode+' '+value+' '+txtValue);
			if(txtValue==''&&isSelectBox===true||clientMatcherMethod==null&&defaultMatcherMethod(label,txtValue,imatchmode)||clientMatcherMethod!=null&&clientMatcherMethod(label,txtValue,imatchmode))
			{//匹配的时候是用label，不是用value
				var selectSpanMid=" colparamvalues=\""+colParamsTmp+"\" onmousedown=\"setTypePromptText('"+value+"',this)\" id='"+TYPE_PROMPT_theTextBox.typePromptObj.spanOutputId+"_"+TYPE_PROMPT_theTextBox.typePromptObj.currentRecordCount+"' value='"+value+"'>"+labelstr;
				resultstr+=TYPE_PROMPT_selectSpanStart+selectSpanMid+TYPE_PROMPT_selectSpanEnd;//+"<br/>";
				TYPE_PROMPT_theTextBox.typePromptObj.currentRecordCount++;
				break;//只要有一列匹配就退出
			}
		}
	}
	if(TYPE_PROMPT_theTextBox.typePromptObj.currentRecordCount<=0) return '';
	//alert(resultstr);
	resultstr="<div id='"+TYPE_PROMPT_theTextBox.typePromptObj.spanOutputId+"_inner'>"+resultstr+"</div>";//外面再括一层<div/>，方便后面控制显示窗口大小（一定要用<div/>，不能用<span/>）
	return resultstr;
}

/**
 * 默认判断选项与输入值是否匹配的方法
 * @param optionvalue 被判断的选项值
 * @param boxvalue 输入框输入的值
 * @param matchmode 匹配模式，1：从头匹配；2：任意位置匹配
 */
function defaultMatcherMethod(optionvalue,boxvalue,matchmode)
{
	return (matchmode==1&&optionvalue.indexOf(boxvalue)==0)||(matchmode==2&&optionvalue.indexOf(boxvalue)>=0);
}

/**
 * 如果当前列参与匹配，则将匹配的子串进行格式化，显示一下划线
 */
function formatMatchText(optionlabel,txtValue,matchmode,isCasesensitive)
{
	if(matchmode!=1&&matchmode!=2) return optionlabel;//当前列不参与匹配
	var regExpObj=null;
	if(matchmode==1&&optionlabel.indexOf(txtValue)==0)
	{
		if(isCasesensitive==true)
		{
			regExpObj=new RegExp('^'+txtValue);
		}else
		{
			regExpObj=new RegExp('^'+txtValue,'i');
		}
	}else if(matchmode==2&&optionlabel.indexOf(txtValue)>=0)
	{
		if(isCasesensitive==true)
		{
			regExpObj=new RegExp(txtValue);
		}else
		{
			regExpObj=new RegExp(txtValue,'i');
		}
	}
	if(regExpObj==null) return optionlabel;
	var resultlabel='';
	var matchObj=splitTextValues(optionlabel,regExpObj,txtValue.length);
	if(matchObj!=null)
	{
		resultlabel=resultlabel+matchObj.start+'<u>'+matchObj.mid+'</u>';//给匹配子串加上下画线
		optionlabel=matchObj.end;
		//alert(matchObj.start+'|||'+matchObj.mid+'|||'+matchObj.end);
		//matchObj=splitTextValues(optionlabel,regExpObj,txtValue.length);
		//alert(matchObj);
	}
	resultlabel=resultlabel+optionlabel;
	return resultlabel;
}

function moveTypePromptHighlight(idx)
{
	//alert('selectedvalue:::'+TYPE_PROMPT_currentValueSelected);
	//alert(TYPE_PROMPT_theTextBox.typePromptObj.currentRecordCount);
	if(TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected>=0)
	{
		newValue=parseInt(TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected)+parseInt(idx);
		if(newValue==TYPE_PROMPT_theTextBox.typePromptObj.currentRecordCount) newValue=0;
		if(newValue<0) newValue=TYPE_PROMPT_theTextBox.typePromptObj.currentRecordCount-1;
		TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected=newValue;
		setTypePromptHighColor(null);
	}
}
function setTypePromptText(xTxtValue,spanObj)
{
	//alert(xTxtValue);
	var colParams=spanObj.getAttribute('colparamvalues');//label1;;;value1|||label2;;;value2...格式的各列值参数
	var allColValuesArr=new Array();
	if(colParams!=null&&colParams!='')
	{
		var colsArr=colParams.split('|||');
		var colObjTmp;
		for(var i=0,len=colsArr.length;i<len;i++)
		{
			if(colsArr[i]==null||colsArr[i]=='') continue;
			colObjTmp=new Object();
			var idx=colsArr[i].indexOf(';;;');
			if(idx>0)
			{
				colObjTmp.label=colsArr[i].substring(0,idx);
				colObjTmp.value=colsArr[i].substring(idx+3);
			}else
			{
				colObjTmp.label=colsArr[i];
				colObjTmp.value=null;
			}
			//alert('label:::'+colObjTmp.label+';;;value:::'+colObjTmp.value);
			allColValuesArr[allColValuesArr.length]=colObjTmp;
		}
	}
	TYPE_PROMPT_theTextBox.value=xTxtValue;
	hideTypePromptTheBox(TYPE_PROMPT_theTextBox);
	var callbackmethod=TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.callbackmethod;
	if(callbackmethod!=null)
	{//配置了选中选项后的回调函数，则执行
		callbackmethod(TYPE_PROMPT_theTextBox,allColValuesArr);//调用时传入文本框对象以及各列的label和value
	}
}
function setTypePromptHighColor(xOutputSpanItem)
{
	//alert(TYPE_PROMPT_theTextBox.typePromptObj.currentRecordCount);
	//debugger;
	if(xOutputSpanItem)
	{//从mouse点击过来
	    var arrayTemp=xOutputSpanItem.id.split("_");
		//alert(arrayTemp.length+"  "+arrayTemp[arrayTemp.length-1]);
		TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected=arrayTemp[arrayTemp.length-1];
	}
	for(var i=0;i<TYPE_PROMPT_theTextBox.typePromptObj.currentRecordCount;i++)
	{
		document.getElementById(TYPE_PROMPT_theTextBox.typePromptObj.spanOutputId+"_"+i).className='spanOutputNormalElement';
	}
	var spanTemp=document.getElementById(TYPE_PROMPT_theTextBox.typePromptObj.spanOutputId+"_"+TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected);
	//alert(TYPE_PROMPT_currentValueSelected);
	if(spanTemp) spanTemp.className='spanOutputHighElement';
}
/**
 *按了回车键，选中相应的输入提示项
 */
function grabTypePromptHighlighted()
{
	//alert(TYPE_PROMPT_currentValueSelected);
	if(TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected>=0)
	{
		var spanTemp=document.getElementById(TYPE_PROMPT_theTextBox.typePromptObj.spanOutputId+"_"+TYPE_PROMPT_theTextBox.typePromptObj.currentValueSelected);
		if(spanTemp)
		{
			var xval=spanTemp.getAttribute("value");
			setTypePromptText(xval,spanTemp);
		}
	}
}

function hideTypePromptTheBox(txtBox)
{
	//alert(txtBox.typePromptObj.spanOutputId);
	document.getElementById(txtBox.typePromptObj.spanOutputId).style.display='none';
	txtBox.typePromptObj.currentValueSelected=-1;
	txtBox.typePromptObj.bNoResults=false;
	clearTimeout(txtBox.typePromptObj.timer);
	txtBox.typePromptObj.timer=false;
	EventTools.removeEventHandler(window.document,"mousedown",hideTypePromptTheBoxEvent);
}

function startTypePromptTimeout()
{
	TYPE_PROMPT_theTextBox.typePromptObj.timer=setTimeout(function(){hideTypePromptTheBox(TYPE_PROMPT_theTextBox);},TYPE_PROMPT_theTextBox.typePromptObj.paramsObj.timeoutSecond*1000);
}

function eraseTypePromptTimeout()
{
	clearTimeout(TYPE_PROMPT_theTextBox.typePromptObj.timer);
	TYPE_PROMPT_theTextBox.typePromptObj.timer=false;
}

var WX_TYPEPROMPT_LOADED=true;//用于标识此js文件加载完