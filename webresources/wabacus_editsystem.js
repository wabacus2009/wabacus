 /**
  * 定义保存前客户端回调函数返回值
  */
 var WX_SAVE_IGNORE=0;//表示不进行本报表的保存/删除操作，但不影响本次一起保存或删除的其它报表
 
 var WX_SAVE_TERMINAT=-1;//表示中止本次保存操作，这里中止的就不仅是当前报表的保存/删除操作，而对本次一起保存的其它报表也有影响，也就是本次保存/删除操作取消
 
 var WX_SAVE_CONTINUE=1;//表示继续后续的保存操作

 /*******************************************************************************
 * 用户自定义传入的待保存数据
 * 数据结构：这是一个Map对象，key为报表id，value为一Map对象，存放用户在此报表中加入的待保存到后台的所有自定义数据列表。
 *										其中name为参数名；value为参数值
 ******************************************************************************/
 var WX_CUSTOMIZE_DATAS;

 /**
  *当输入框为点击后填充的方式时，必须将输入框初始化一份如下的数据
  *在此数据中存放有输入框所在td的对象，以便onblur时，能取到它向其中回填最新值。
  *@param xInputBoxObj 输入框对象
  *@param xParentTdObj 输入框所在的父td对象
  */
 function initInputBoxData(xInputBoxObj,xParentTdObj)
 {
 	var props={
 		me:xInputBoxObj,
 		parentTdObj:xParentTdObj
 	};
 	return props;
 }
 
 /**
  * 点击某个单元格时，将输入框填充到其中的js函数
  */
 function fillInputBoxOnClick(evt)
 {
 	var event=evt||window.event;
 	if(event.ctrlKey||event.shiftKey) return false;//按住Ctrl或Shift键进行点击，此时可能是为了选择，所以不允许编辑单元格数据
 	var element=event.srcElement || event.target;
 	if(element.tagName!='TD')
 	{//点击到<td/>上的其它子标签，则取到父td对象
 		if(isWXInputBoxNode(element)) return false;
 		element=getParentElementObj(element,'TD');
 		if(element==null) return false;
 	}
 	if(getWXInputBoxChildNode(element)!=null) return false;//当前单元格已经填充了框架的输入框，则不用再填充
 	fillInputBoxToTd(element);
 }
 
 /**
 * 对于可编辑报表类型2，更新主下拉框数据时，复原从下拉框所在td的数据
 * @param parentSelectboxObj 父下框对象
 */
function resetChildSelectBoxData(parentBoxObj)
{
	/**
	 * 说明：
	 * 	如果是列表报表的下拉框，下拉框id格式为：report13province__0，其所在<td/>的id格式为report13province__td0，其中0为行号
    * 	如果是细览报表的下拉框，其id格式为：report13province，其所在<td/>的id格式为report13province__td
	 */
	var inputboxid=parentBoxObj.getAttribute('id');
	if(inputboxid==null||inputboxid=='') return;
	var reportguid=getReportGuidByInputboxId(inputboxid);
	var metadataObj=getReportMetadataObj(reportguid);
	var tdObj=getInputboxParentElementObjByTagName(parentBoxObj,'TD');
	var tdUpdatecolDestObj=getUpdateColDestObj(tdObj,metadataObj.reportguid,tdObj);
	var oldvalue=tdUpdatecolDestObj.getAttribute('oldvalue');//当前父下拉框的旧值，即第一次显示时的值
	var newvalue=getInputBoxValue(parentBoxObj);//当前父输入框的新值
	if(oldvalue==null) oldvalue='';
	if(newvalue==null) newvalue='';
	resetTdValue(tdObj,newvalue==oldvalue);//newvalue==oldvalue为true表示是恢复旧值，则稍后从下拉框也全部恢复旧值
}

function resetTdValue(tdObj,isResetOldValue)
{
	var tdid=tdObj.getAttribute('id');
	var inputboxid=getInputboxIdByParentElementObj(tdObj);
	if(inputboxid==null||inputboxid=='') return;
	var reportguid=getReportGuidByInputboxId(inputboxid);
	var metadataObj=getReportMetadataObj(reportguid);
	var boxMetadataObj=getInputboxMetadataObj(inputboxid);
	if(boxMetadataObj==null) return;
	var childSelectboxIds=boxMetadataObj.getAttribute('childboxids');//所有子下拉框id
	if(childSelectboxIds==null||childSelectboxId=='') return;
	var childSelectboxIdsArr=childSelectboxIds.split(';');
	for(var n=0;n<childSelectboxIdsArr.length;n++)
	{
		var childSelectboxId=childSelectboxIdsArr[n];
		if(childSelectboxId==null||childSelectboxId=='') continue;
		var childTdId=childSelectboxId+'__td';//得到子下拉框所在<td/>的id
		var idxTmp=tdid.lastIndexOf('__td');
		if(idxTmp>0)
		{
			var rowindex=tdid.substring(idxTmp+4);//对于数据自动列表报表，__td后面跟的是行号，这里就是要取这个行号
			if(rowindex!=null&&rowindex!='') childTdId=childTdId+rowindex;
		}
		var childTdObj=document.getElementById(childTdId);
		if(childTdObj!=null) resetChildTdValue(metadataObj,childTdObj,isResetOldValue);
	}
}

/*
 * 刷新子下拉框所在<td/>的数据
 * @param tdObj 子下拉框所在单元格的<td/>对象
 * @param isResetOldValue 是否是恢复旧值，还是直接置空。如果主下拉框的数据是恢复旧值，则从下拉框所在<td/>也恢复旧值；如果主下拉框当前是新值，则从下拉框所在<td/>置空
 */
function resetChildTdValue(metadataObj,tdObj,isResetOldValue)
{
	var tdUpdatecolSrcObj=getUpdateColSrcObj(tdObj,metadataObj.reportguid,tdObj);
	var tdUpdatecolDestObj=getUpdateColDestObj(tdObj,metadataObj.reportguid,tdObj);
	var olderInnerValue=tdUpdatecolSrcObj.getAttribute('oldInnerHTMLValue');
	if(olderInnerValue==null)
	{//如果还没有将此<td/>的显示值保存起来，说明是第一次刷新此<td/>的内容，则保存起来
		tdUpdatecolSrcObj.setAttribute('oldInnerHTMLValue',tdUpdatecolSrcObj.innerHTML);
	}
	if(isResetOldValue)
	{
		if(olderInnerValue==null) return;//如果父输入框新旧值相同，且本输入框还没有存放旧的InnerHtml，说明是由于父输入框的onblur引起的，且并没有改变值
		tdUpdatecolSrcObj.setAttribute('value',tdUpdatecolSrcObj.getAttribute('oldvalue'));
		tdUpdatecolSrcObj.innerHTML=olderInnerValue;//恢复<td/>显示值为原始值
		tdUpdatecolDestObj.setAttribute('value',tdUpdatecolDestObj.getAttribute('oldvalue'));
	}else
	{
		tdUpdatecolSrcObj.setAttribute('value','');
		setColDisplayValueToEditable2Td(tdUpdatecolSrcObj,'&nbsp;');//这里一定用&nbsp;，因为在树形分组报表中，如果直接用''，则会导致树形节点显示变形
		tdUpdatecolDestObj.setAttribute('value','');
	}
	resetTdValue(tdObj,isResetOldValue);
}

/**
 *将编辑框中的数据放入待保存数据队列
 *@param dataobj 对于细览报表，这里只是一个'true'字符串，表示此列数据被更新过
 *               对于数据自动列表报表，这里存放被更新数据的<tr/>对象，保存时此tr下的所有数据都将被传到后台保存
 */
function addInputboxDataForSaving(reportguid,inputboxObj)
{
	var metadataObj=getReportMetadataObj(reportguid);
	var oldvalueEleObj=getInputboxParentElementObj(inputboxObj);//存放旧值的标签元素对象
	if(oldvalueEleObj==null) return;
	var changeBgcolorEleObj=null;//需要标识被修改的标签元素对象
	if(metadataObj.reportfamily==ReportFamily.EDITABLELIST2||metadataObj.reportfamily==ReportFamily.EDITABLEDETAIL2)
	{
		changeBgcolorEleObj=oldvalueEleObj;//改变整个<td/>的背景色
	}else
	{
		changeBgcolorEleObj=getChangeStyleObjByInputBoxObjOnEdit(inputboxObj);
	}
	if(changeBgcolorEleObj==null) changeBgcolorEleObj=inputboxObj;
	oldvalueEleObj=getUpdateColDestObj(oldvalueEleObj,reportguid,oldvalueEleObj);//当前列有可能通过updatecol属性更新其它列数据（之所以这里用updatecol更新列的值，是因为后面从输入框取新值时也是取的是真正值）
	var oldvalue=oldvalueEleObj.getAttribute('oldvalue');
	if(oldvalue==null) oldvalue='';
	var newvalue=wx_getColValue(oldvalueEleObj);
	if(newvalue==null) newvalue='';
	if(newvalue==oldvalue) return;//本次没有更新编辑列的值
	addDataForSaving(reportguid,changeBgcolorEleObj);
}

/**
 * 根据输入框所在的父标签添加相应的待保存数据
 * @param eleObj 对于editablelist2/listform/editabledetail2报表类型，此对象为<td/>对象，对于editabledetail/form报表类型，此对象为<font/>对象
 */
function addElementDataForSaving(reportguid,eleObj)
{
	eleObj=getUpdateColSrcObj(eleObj,reportguid,eleObj);
	addDataForSaving(reportguid,getChangeStyleObjByParentElementOnEdit(eleObj));
}

function addDataForSaving(reportguid,changeBgcolorEleObj)
{
	changeEditedInputboxDisplayStyle(changeBgcolorEleObj);
	var metadataObj=getReportMetadataObj(reportguid);
	var dataObj=null;
	if(metadataObj.reportfamily==ReportFamily.EDITABLELIST2||metadataObj.reportfamily==ReportFamily.LISTFORM)
	{//这两种报表类型在这里存放要保存的tr对象
		dataObj=getParentElementObj(changeBgcolorEleObj,'TR');
	}else
	{//细览报表只要存放true
		dataObj='true';
	}
	doAddDataForSaving(reportguid,dataObj);
}

function doAddDataForSaving(reportguid,savedataObj)
{
	if(WX_UPDATE_ALLDATA==null) WX_UPDATE_ALLDATA=new Object();
	var tmpDataList=WX_UPDATE_ALLDATA[reportguid];
	if(tmpDataList==null) 
	{
		tmpDataList=new Array();
		WX_UPDATE_ALLDATA[reportguid]=tmpDataList;
	}
	var i=0;
	for(;i<tmpDataList.length;i=i+1)
	{
		if(tmpDataList[i]==savedataObj) break;
	}
	//alert(i+'  '+savedataObj);
	if(i==tmpDataList.length) tmpDataList[tmpDataList.length]=savedataObj;
	//alert(tmpDataList);
}

/**
 * 向某个表格添加记录
 *	@param dynDefaultValuesObj：添加时指定某个或某些列的默认值，对于可编辑列表报表类型的下拉框/单选框，还可以指定默认值在<td/>中的默认显示label（因为它们是点击时才会显示输入框，所以要先显示默认值对应的label）。
 *									指定方式为json字符串，格式如下所示:
 *				{col1:"value1",col1$label:"label1",col3:"value3",...}，col1、col3为相应列的column属性配置值，对于可编辑列表报表的下拉框或单选框，如果要指定某个列默认值对应的显示值，则通过键：column$label
 */
function addNewDataRow(pageid,reportguid,dynDefaultValuesObj)
{
	var doc=document;
	var tableObj=doc.getElementById(reportguid+'_data');
	if(tableObj==null)
	{
		wx_warn('此报表所在表格的ID不合法，无法增加新行');
		return false;
	}
	var metadataObj=getReportMetadataObj(reportguid);
	var newRowCols=metadataObj.metaDataSpanObj.getAttribute('newRowCols');//取到新增行中各列显示信息
	if(newRowCols==null||newRowCols=='')
	{
		wx_error('没有取到报表添加记录信息，可能没有为此报表配置添加功能');
		return false;
	}
	var colsInfoObj=getObjectByJsonString(newRowCols);
	var currentRecordCount=colsInfoObj.currentRecordCount;//当前已有的记录数
	var maxRecordCount=colsInfoObj.maxRecordCount;//配置的最大记录数，如果为-1，则不限制记录数（通过<display/>的maxrownum配置）
	//alert(currentRecordCount+'  '+maxRecordCount);
	if(maxRecordCount!=null&&maxRecordCount>0)
	{//限制了最大记录数
		var changedRowNumInClient=tableObj.getAttribute('wx_MyChangedRowNumInClient');//获取当前报表在客户端增、删的行数
		if(changedRowNumInClient==null) changedRowNumInClient='0';
		var iChangedRowNumInClient=parseInt(changedRowNumInClient);
		if(currentRecordCount+iChangedRowNumInClient>=maxRecordCount)
		{
			wx_warn('此报表限制最大记录数：'+maxRecordCount);
			return false;
		}
	}
	var nodataTrObj=doc.getElementById(reportguid+"_nodata_tr");
	if(nodataTrObj!=null)
	{//当前报表添加数据前没有符合条件的记录（可能是因为搜索导致），则将提示没有记录的行删掉，因为现在要添加一条记录，必须在添加记录前删除，否则下面取tableObj.rows.length时会出现第一次第二次添加的时候相同的情况
		nodataTrObj.parentNode.removeChild(nodataTrObj);
	}
	var rownum=tableObj.rows.length;//当前记录数
	rownum=rownum+1;
	var newtrid=reportguid+'_tr_'+rownum;//新记录行的id
	var mFirstLevelChildSelectBoxIds=new Object();//存放所有第一层子选择框ID组合，这样在显示完后自动刷新这些选择框的选项。对于其它层级的子选择框，则通过级联进行刷新
	var trObj=doc.createElement('tr');
	trObj.className='cls-data-tr';
	trObj.setAttribute('id',newtrid);
	trObj.setAttribute('EDIT_TYPE','add');
	trObj.setAttribute('global_rowindex','new_'+rownum);//加上new_前缀，这样可以与已有行保存唯一
	var colsObj=colsInfoObj.cols;
	if(colsObj==null||colsObj.length==0) return false;
	var fillInputboxTdObjsArr=new Array();//存放要立即填充输入框的单元格（因为填充时要取它们的updatecol对应的列，所以需要在所有列都添加到<tr/>中后才能填充输入框，所以先缓存到此数组变量中）
	var defaultvalue,defaultlabel;
	var boxMetadataObjTmp,displayonclickTmp,colObjTmp;
	for(var i=0;i<colsObj.length;i=i+1)
	{
		colObjTmp=colsObj[i];
		var tdObj=doc.createElement('td');
		tdObj.appendChild(doc.createTextNode(' '));
		trObj.appendChild(tdObj);
		if(metadataObj.reportfamily==ReportFamily.LISTFORM)
		{
			tdObj.className='cls-data-td-listform';
		}else
		{
			tdObj.className='cls-data-td-editlist';
		}
		//alert(colObjTmp.coltype+'  '+colObjTmp.value_name);
		if(colObjTmp.updatecolSrc!=null&&colObjTmp.updatecolSrc!='') tdObj.setAttribute('updatecolSrc',colObjTmp.updatecolSrc);
		if(colObjTmp.updatecolDest!=null&&colObjTmp.updatecolDest!='') tdObj.setAttribute('updatecolDest',colObjTmp.updatecolDest);
		if(colObjTmp.hidden=='true') tdObj.style.display='none';//不参与本次显示(可能是hidden配置为1或者因为列选择后导致不参与本次显示)
		if(colObjTmp.colWrapStart!=null&&colObjTmp.colWrapStart!=''&&colObjTmp.colWrapEnd!=null&&colObjTmp.colWrapEnd!='')
		{//如果单元格内容需要包裹起来
			tdObj.innerHTML=colObjTmp.colWrapStart+colObjTmp.colWrapEnd;
		}
		if(colObjTmp.coltype=='EMPTY') continue;
		if(colObjTmp.coltype=='ROWSELECTED-RADIOBOX')
		{//当前列是选中行的单选框列
			setColDisplayValueToEditable2Td(tdObj,"<input type=\"radio\" onclick=\"doSelectedDataRowChkRadio(this)\" name=\""+reportguid+"_rowselectbox_col\">");
			continue;
		}else if(colObjTmp.coltype=='ROWSELECTED-CHECKBOX')
		{//当前列是选中行的复选框列
			setColDisplayValueToEditable2Td(tdObj,"<input type=\"checkbox\" onclick=\"doSelectedDataRowChkRadio(this)\" name=\""+reportguid+"_rowselectbox_col\">");
			continue;
		}
		if(colObjTmp.value_name!=null&&colObjTmp.value_name!='')
		{
			tdObj.setAttribute('value_name',colObjTmp.value_name);
		}
		var inputboxid=null;//输入框id
		boxMetadataObjTmp=null;
		displayonclickTmp=null;
		if(colObjTmp.boxid!=null&&colObjTmp.boxid!='')
		{
			tdObj.setAttribute('id',colObjTmp.boxid+'__td'+rownum);
			inputboxid=colObjTmp.boxid+'__'+rownum;
			boxMetadataObjTmp=getInputboxMetadataObj(colObjTmp.boxid);
			if(boxMetadataObjTmp!=null) displayonclickTmp=boxMetadataObjTmp.getAttribute('displayonclick');
		}
		defaultvalue='';
		defaultlabel='';
		//alert(dynDefaultValuesObj[colObjTmp.value_name]+'   '+colObjTmp.value_name);
		if(dynDefaultValuesObj!=null&&dynDefaultValuesObj[colObjTmp.value_name]!=null)//json数组也可以这样访问
		{//用户动态指定了此列输入框的默认值
			defaultvalue=jsonParamDecode(dynDefaultValuesObj[colObjTmp.value_name]);
			defaultlabel=jsonParamDecode(dynDefaultValuesObj[colObjTmp.value_name+'$label']);
			if(defaultlabel==null) defaultlabel=defaultvalue;
		}else if(colObjTmp.defaultvalue)
		{//用户静态配置了此列输入框的默认值
			defaultvalue=colObjTmp.defaultvalue;
			defaultlabel=colObjTmp.defaultvaluelabel;
			if(defaultlabel==null) defaultlabel=defaultvalue;
		}
		if(defaultvalue!=null&&defaultvalue!='') tdObj.setAttribute('value',defaultvalue);//有默认值
		if(colObjTmp.coltype=='NONE-EDITABLE'||colObjTmp.hidden=='true')
		{
			if(defaultlabel!=null&&defaultlabel!='') setColDisplayValueToEditable2Td(tdObj,defaultlabel);
			continue;//当前列是不可编辑列（可能是配置时不可编辑或者是授权为readonly），或虽是可编辑列，但不参与本次显示
		}
		if(displayonclickTmp==='true') 
		{//点击单元格时才显示输入框
			if(defaultlabel!=null&&defaultlabel!='')
			{//有默认值，则在<td/>中显示默认值对应的显示label
				setColDisplayValueToEditable2Td(tdObj,defaultlabel);
			}
         if(window.event)
         {
            tdObj.onclick=function(boxid)
            				{ 
            					return function(){fillInputBoxOnClick(null);};
            				}(inputboxid);
         }else
         {
				tdObj.setAttribute("onclick","fillInputBoxOnClick(event)");
    	   }
    	}else
    	{//显示td时就直接填充输入框
    		if(boxMetadataObjTmp!=null&&(boxMetadataObjTmp.getAttribute('parentids')==null||boxMetadataObjTmp.getAttribute('parentids')=='')) 
    		{//如果当前列上的输入框不是子选择框
    			addRefreshedChildBoxIds(mFirstLevelChildSelectBoxIds,boxMetadataObjTmp.getAttribute('childboxids'),rownum);
    		}
    		if(boxMetadataObjTmp.getAttribute('selectboxtype')=='combox') tdObj.setAttribute('wx_tmp_selectboxtype','combox');//以便后面填充时放在最后显示这个输入框，这样可以避免其它输入框显示时改变单元格大小导致开始显示的combox有问题
			fillInputboxTdObjsArr[fillInputboxTdObjsArr.length]=tdObj;
    	}
		tdObj=null;
	}
	if(tableObj.rows.length==0)
	{//如果数据表格没有记录行
		if(tableObj.childNodes.length==0)
		{//如果也没有<tbody/>等标签
			tableObj.appendChild(trObj);
		}else
		{//如果有<tbody/>标签
			tableObj.childNodes[0].appendChild(trObj);
		}
	}else if(metadataObj.metaDataSpanObj.getAttribute('addposition')!=='top')
	{//追加到后面
		tableObj.rows[0].parentNode.appendChild(trObj);
	}else
	{//新增行显示在前面
		var trDataObj=null;//存放第一个数据行
		var trClassNameTmp;
		for(var i=0;i<tableObj.rows.length;i++)
		{
			trClassNameTmp=tableObj.rows[i].className;
			if(trClassNameTmp!=null&&trClassNameTmp.indexOf('cls-data-tr')==0&&trClassNameTmp!='cls-data-tr-head-list')
			{//是第一个数据行
				trDataObj=tableObj.rows[i];
				break;
			}
		}
		if(trDataObj==null)
		{//没有数据行
			tableObj.rows[0].parentNode.appendChild(trObj);
		}else
		{
			trDataObj.parentNode.insertBefore(trObj,trDataObj);
		}
	}
	//要放在显示完<tr/>的后面，因为有的输入框（比如combox）在显示时要依赖从页面上找这个输入框对象才能正确显示，或者依赖它的实际显示位置和大小
	for(var i=0,len=fillInputboxTdObjsArr.length;i<len;i++)
	{//填充所有要立即填充输入框的单元格
		if(fillInputboxTdObjsArr[i].getAttribute('wx_tmp_selectboxtype')!=='combox')
		{
			fillInputBoxToTd(fillInputboxTdObjsArr[i]);
		}
	}
	for(var i=0,len=fillInputboxTdObjsArr.length;i<len;i++)
	{//填充所有组合框
		if(fillInputboxTdObjsArr[i].getAttribute('wx_tmp_selectboxtype')==='combox')
		{
			fillInputBoxToTd(fillInputboxTdObjsArr[i]);
			fillInputboxTdObjsArr[i].removeAttribute('wx_tmp_selectboxtype');
		}
	}
	doAddDataForSaving(reportguid,trObj);
	refreshAllChildSelectboxs(mFirstLevelChildSelectBoxIds,true);//刷新所有第一层子选择框选项
	if(maxRecordCount!=null&&maxRecordCount>0) setListReportChangedRowNumInClient(reportguid,1,true);//限制了最大记录数，将客户端改变的记录数加1
	var callbackmethodStr=metadataObj.metaDataSpanObj.getAttribute('addCallbackMethod');//配置了添加记录行的回调函数
	var callbackmethod=getObjectByJsonString(callbackmethodStr);
	if(callbackmethod!=null&&callbackmethod.method!=null)
	{
		callbackmethod.method(trObj);
	}
	/**
	 * 这里用到了闭包，所以清除掉下面的变量，节省内存
	 */
	doc=null;
	tableObj=null;
	trObj=null;
	metadataObj=null;
}

/**
 * 对于限制了最大记录数的列表报表，在新增和删除新增的记录时，改变存放在<table/>中的客户端变更记录数
 * @param deltacount 如果当前是新增记录，则值为1，如果是删除新增的记录，则值为-1
 * @param createNew 如果<table/>中没有此属性，是否要强制加上此属性，对于限制了最大记录数的报表，在添加时，必须加上此属性，在删除新增记录时，如果限制了最大记录数，则<table/>中肯定有此属性，如果没有限制，则肯定没有，也不用加制加。
 */
function setListReportChangedRowNumInClient(reportguid,deltacount,createNew)
{
	var tableObj=document.getElementById(reportguid+'_data');
	var changedRowNumInClient=tableObj.getAttribute('wx_MyChangedRowNumInClient');//获取当前报表在客户端增、删的行数
	if(changedRowNumInClient==null||changedRowNumInClient=='')
	{//如果没有wx_MyChangedRowNumInClient属性
		if(!createNew) return;//如果不需要创建新的
		changedRowNumInClient='0';
	}
	var iChangedRowNumInClient=parseInt(changedRowNumInClient)+deltacount;
	if(iChangedRowNumInClient<0) iChangedRowNumInClient=0;
	tableObj.setAttribute('wx_MyChangedRowNumInClient',iChangedRowNumInClient);
}

 /**
  * 输入框键盘事件
  */
 function onKeyEvent(event)
 {
 	var intKey=-1;
	var srcObj;
	if(window.event)
	{
		intKey=window.event.keyCode;
		srcObj=window.event.srcElement;
	}else{
		intKey=event.which;
		srcObj=event.target;
	}
	if(intKey==13)
	{
		srcObj.blur();
		return false;
	}
	return true;
 }
 
 /**
 *设置textarea类型文本框的位置
 * textBoxObj：文本框对象
 * ownerObj:td对象，文本框的位置要以它为标准。
 */
function setTextAreaBoxPosition(textBoxObj,ownerObj)
{
	var txtboxPosX=0;
	var txtboxPosY=0;
	if(!ownerObj)	return;
	var pos=getElementAbsolutePosition(ownerObj);
	textBoxObj.style.left=pos.left+'px';
	//alert(theTextBox.obj.matchTextBoxWidth);
	//textBoxObj.style.width=TABLE_FILTER_imgObj.obj.spanOutputWidth;
	textBoxObj.style.top=pos.top+'px';
	textBoxObj.style.display="block";
	if(!pos.width||pos.width<100)
	{
		textBoxObj.style.width='200px';
	}else
	{
		textBoxObj.style.width=pos.width+'px';
	}
	/**var textAreabox=document.getElementById('WX_TEXTAREA_BOX');
     if(!textAreabox)
     { 
     	textAreabox=document.createElement("textarea");
     	textAreabox.className='cls-inputbox-textareabox2';
     	textAreabox.value='Hello';
     	textAreabox.setAttribute('id','WX_TEXTAREA_BOX');
     	textAreabox.style.display='none';
     	textAreabox.onblur=function(){this.style.display='none';};
     	//alert(textAreabox);
     	document.body.appendChild(textAreabox);
     }
     setTextAreaBoxPosition(textAreabox,element);
     textAreabox.focus();*/
}
 
/**
 * 获取可编辑数据自动列表报表指定列的数据
 */
function getEditableListReportColValues(pageid,reportguid,columnsObj,conditionsObj)
{
	var trObjsArr=getAllEditableList2DataTrObjs(reportguid,conditionsObj);//获取到满足条件的记录行，如果没传conditions，则取所有数据行对象
	if(trObjsArr==null||trObjsArr.length==0) return null;
	var resultsArr=new Array();
	for(var i=0,len=trObjsArr.length;i<len;i=i+1)
	{
		var rowDataObj=wx_getAllColValueByParentElementObjs(trObjsArr[i].getElementsByTagName('TD'),columnsObj);//存放当前记录行上所有列数据
		if(rowDataObj!=null) resultsArr[resultsArr.length]=rowDataObj;
	}
	return resultsArr;
}

/**
 * 设置editablelist2/editabledetail2两种报表类型上某些或所有记录上的某些列上输入框的值。
 * 注意：如果设置的是树形分组列或普通行分组列的上的输入框上的值，则不会刷新分组节点上输入框或<td/>上的显示值，这是因为它们是公共的，不能因为改几条记录而把分组中其它记录的此列数据更改了。
 * @param newValuesObj 传入要设置哪些列及设置给它们的新值，如果是设置editablelist2报表类型上的输入框或不可编辑列上的值时，显示值和真正的值可能不同
 *	@param conditionsObj 指定修改哪些行上这些列的值，如果没有传入此参数，则默认设置所有行上这些列的值
 */
function setEditableListReportColValue(reportguid,newValuesObj,conditionsObj)
{
	if(newValuesObj==null||isEmptyMap(newValuesObj)) return false;
	var trObjsArr=getAllEditableList2DataTrObjs(reportguid,conditionsObj);
	if(trObjsArr==null||trObjsArr.length==0) return false;
	//alert(trObjsArr.length);
	for(var i=0,len=trObjsArr.length;i<len;i=i+1)
	{//依次设置每行中各列的值
		setBatchEditableColValues(reportguid,trObjsArr[i].getElementsByTagName('TD'),newValuesObj);
	}
	return true;
}

/**
 * 获取editablelist2/listform两种报表类型中所有有效数据行
 * @param conditions 指定要获取的数据记录行要满足的条件，默认为获取当前页面所有数据行
 */
function getAllEditableList2DataTrObjs(reportguid,conditionsObj)
{
	var tableObj=document.getElementById(reportguid+'_data');
	if(tableObj==null||tableObj.rows.length<=0) return null;
	var trObjsArr=new Array();//存放本次要设置值的<tr/>对象集合
	var trObjTmp=null;
	//alert(reportguid+'_data tableObj.rows.length  '+tableObj.rows.length);
	for(var i=0,len=tableObj.rows.length;i<len;i=i+1)
	{//取到所有有效数据行对象，存入trObjsArr数组中待处理
		trObjTmp=tableObj.rows[i];
		if(!isEditableListReportTr(reportguid,trObjTmp)) continue;
		trObjsArr[trObjsArr.length]=trObjTmp;
	}
	if(trObjsArr.length==0) return null;
	if(conditionsObj==null) return trObjsArr;//没有指定条件，说明要获取所有记录行对象
	var conditionsArrObj=convertToArray(conditionsObj);
	var isSelectedRowCondition=null;
	if(conditionsArrObj.length==1&&conditionsArrObj[0].name=='SELECTEDROW')
	{//如果指定的条件是指选中行或非选中行
		if(conditionsArrObj[0].value==true||conditionsArrObj[0].value=='true')
		{//获取所有选中行
			isSelectedRowCondition='true';
		}else
		{//获取所有非选中行
			isSelectedRowCondition='false';
		}
	}
	var trArrTmp=new Array();
	for(var i=0,len=trObjsArr.length;i<len;i=i+1)
	{
		if((isSelectedRowCondition=='true'&&isSelectedRow(trObjsArr[i]))||
			(isSelectedRowCondition=='false'&&!isSelectedRow(trObjsArr[i]))||
			isMatchAllOldValues(trObjsArr[i],conditionsArrObj))
		{//满足用户指定的条件
			trArrTmp[trArrTmp.length]=trObjsArr[i];
		}
	}
	return trArrTmp.length==0?null:trArrTmp;
}

/**
 * 判断当前行是否是可编辑列表报表或列表表单的数据行
 */
function isEditableListReportTr(reportguid,trObj)
{
	var trid=trObj.getAttribute('id');
	if(trid==null||trid.indexOf(reportguid)<0||trid.indexOf('_tr_')<0) return false;
	var value_name;
	for(var i=0;i<trObj.cells.length;i=i+1)
	{
		value_name=trObj.cells[i].getAttribute('value_name');
		if(value_name!=null&&value_name!='') return true;
	}
	return false;
}

/**
 * 判断某个数据行中的相应列的值是否与条件值相同
 */
function isMatchAllOldValues(trObj,conditionsArrObj)
{
	var tdObjTmp=null;
	var conditionObjTmp=null;
	for(var i=0,len=conditionsArrObj.length;i<len;i=i+1)
	{//依次循环用户指定的每个条件
		conditionObjTmp=conditionsArrObj[i];
		var name=conditionObjTmp.name;//条件所用的列的column属性值
		if(name==null||name=='') continue;
		var matchmode=conditionObjTmp.matchmode;//此条件的比较模式
		if(matchmode==null||matchmode=='') matchmode='equals';//默认为相等比较
		var j=0;
		for(var len2=trObj.cells.length;j<len2;j=j+1)
		{//判断当前行中所有列中是否有一列与当前条件匹配
			tdObjTmp=trObj.cells[j];
			var valueNameTmp=tdObjTmp.getAttribute('value_name');
			if(valueNameTmp==null||valueNameTmp==''||valueNameTmp!=name) continue;
			if(conditionObjTmp.oldvalue)
			{//本条件中指定的是原始数据做为条件
				if(!matchOldValue(conditionObjTmp.oldvalue,tdObjTmp.getAttribute('oldvalue'),matchmode)) return false;
			}
			if(conditionObjTmp.value)
			{//本条件中指定了现在的列数据做为条件
				if(!matchOldValue(conditionObjTmp.value,wx_getColValue(tdObjTmp),matchmode)) return false;
			}
			break;
		}
		if(j==trObj.cells.length) return false;//在此行中没有找到此条件对应的列，则此<tr/>肯定不满足要求
	}
	return true;
}

/**
 * 比较valueDest通过matchmode方式是否匹配valueSrc
 * @param valueSrc 用户在条件中指定的值
 * @param valueDest 当前要判断的列的数据
 * @param matchmode 比较模式
 */
function matchOldValue(valueSrc,valueDest,matchmode)
{
	if(valueSrc==null) valueSrc='';
	if(valueDest==null) valueDest='';
	if(valueSrc==''&&valueDest=='') return true;
	if(matchmode=='include'&&valueDest.indexOf(valueSrc)>=0) return true;
	if(matchmode=='exclude'&&valueDest.indexOf(valueSrc)<0) return true;
	if(matchmode=='start'&&valueDest.indexOf(valueSrc)==0) return true;
	if(matchmode=='end'&&valueDest.lastIndexOf(valueSrc)==valueDest.length-valueSrc.length) return true;
	if(matchmode=='regex'&&valueDest.match(valueSrc)) return true;
	return valueSrc==valueDest;//默认是equals比较
}

/*********保存editablelist2/editabledetail2报表类型的数据***********/
/**
 * 保存editablelist2/listform报表类型的数据
 */
function preSaveEditableListReportTypeData(metadataObj)
{
	if(WX_UPDATE_ALLDATA==null) return WX_SAVE_IGNORE;
	var datasArray=new Array();
	var datasObj;
	var savedatatype=metadataObj.metaDataSpanObj.getAttribute('savedatatype');
	var updatedataForSaving=null;
	if(savedatatype=='changed')
	{//本报表只保存有修改的数据
		updatedataForSaving=WX_UPDATE_ALLDATA[metadataObj.reportguid];
	}else
	{//保存所有数据
		updatedataForSaving=getAllEditableList2DataTrObjs(metadataObj.reportguid,null);
	}
	if(updatedataForSaving==null||updatedataForSaving.length==0) return WX_SAVE_IGNORE;
	var trObj,edittype,hasSaveData=false;
	for(var i=0,len=updatedataForSaving.length;i<len;i=i+1)
	{
		trObj=updatedataForSaving[i];
		//if(!hasEditDataForSavingRow(trObj)) continue;//当前行记录没有列被更新，不需保存到后台
		var tdChilds=trObj.getElementsByTagName('TD');
		datasObj=new Object();
		edittype=trObj.getAttribute('EDIT_TYPE');
		if(edittype=='add')
		{//新增记录
			datasObj['WX_TYPE']='add';//表示当前记录是被做添加操作
		}else
		{//修改记录
			datasObj['WX_TYPE']='update';//表示当前记录是被做修改操作
		}
		var rtnVal=getAllSavingData(metadataObj,tdChilds,datasObj);
		if(rtnVal===WX_SAVE_TERMINAT) return rtnVal;
		if(rtnVal===false) continue;
		datasArray[datasArray.length]=datasObj;
		hasSaveData=true;
	}
	storeSavingData(metadataObj.reportguid,datasArray);
	return hasSaveData==true?WX_SAVE_CONTINUE:WX_SAVE_IGNORE;
}

/**
 * 删除editablelist/editablelist2/listform报表类型的数据
 * @param updatetype：删除类型，一般为delete，表示删除当前被选中的数据，如果指定格式为delete|all，则删除本报表当前显示的所有记录
 * 
 */
function deleteListReportTypeData(metadataObj,updatetype)
{
	var deletetype=updatetype.lastIndexOf('|')>0?updatetype.substring(updatetype.lastIndexOf('|')+1):'';
	var trObjArr=null;
	if(deletetype=='all')
	{//当前是删除本次显示的所有数据
		trObjArr=getAllEditableList2DataTrObjs(metadataObj.reportguid,null);
	}else
	{//默认删除选中行的数据
		if(WX_selectedTrObjs==null) return WX_SAVE_IGNORE;
		var trObjs=WX_selectedTrObjs[metadataObj.reportguid];
		trObjArr=new Array();
		for(var key in trObjs)
		{
			trObjArr[trObjArr.length]=trObjs[key];
		}
	}
	return preDeleteListReportTrObjs(metadataObj,trObjArr);
}

/**
 * 删除editablelist2/listform中指定行对象的数据，可以是新增行，也可以是已有行
 * 供wabacus_api.js中deleteEditableListReportRows()方法调用
 */
function deleteEditableListReportRowsImpl(reportguid,trObjArray)
{
	if(trObjArray==null||trObjArray.length==0) return;
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj==null)
	{
		wx_warn('删除数据失败，没有取到guid为'+reportguid+'的元数据');
		return;
	}
	if(!preDeleteListReportTrObjs(metadataObj,trObjArray)) return;
	var saveParams=addEditableReportSaveDataParams(metadataObj);
	if(saveParams=='') return;
	var url=getComponentUrl(metadataObj.pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
	if(url==null||url=='')
	{
		wx_warn('保存数据失败，没有取到guid为'+metadataObj.reportguid+'的URL');
		return;
	}
	url=removeReportNavigateInfosFromUrl(url,metadataObj,null);//删除本报表的翻页导航ID以及查询条件关联的报表的翻页导航ID，重新计算页码
	url=url+saveParams;
	WX_saveWithDeleteUrl=url;
	if(getDeleteConfirmMessageAsString()=='')
	{//没有删除确认提示信息，则直接删除
		doSaveEditableWithDelete('ok');
	}else
	{
		wx_confirm(getDeleteConfirmMessageAsString(),'删除',null,null,doSaveEditableWithDelete);
	}
}

function preDeleteListReportTrObjs(metadataObj,trObjArr)
{
	if(trObjArr==null||trObjArr.length==0) return WX_SAVE_IGNORE;
	var deleteconfirmmessage=metadataObj.metaDataSpanObj.getAttribute('deleteconfirmmessage');//获取到删除报表确认提示信息
	addDeleteConfirmMessage(getEditableReportDeleteConfirmMessage(trObjArr,deleteconfirmmessage));//得到实际提示信息（去替换掉动态内容部分）
	var delNewTrObjArr=new Array();//存放本次删除的新增的行对象
	var datasArrayOfOldTrObjs=new Array();
	var edittype,trObjTmp;
	var hasDeleteData=false;
	for(var i=0;i<trObjArr.length;i=i+1)
	{
		trObjTmp=trObjArr[i];
		if(trObjTmp==null) continue;
		hasDeleteData=true;
		edittype=trObjTmp.getAttribute('EDIT_TYPE');
		if(edittype=='add')
		{
			delNewTrObjArr[delNewTrObjArr.length]=trObjTmp;
		}else
		{
			var datasObj=new Object();
			datasObj['WX_TYPE']='delete';//表示当前记录是被做修改操作
			var rtnVal=getAllSavingData(metadataObj,trObjTmp.getElementsByTagName('TD'),datasObj);
			if(rtnVal===WX_SAVE_TERMINAT) return rtnVal;
			if(rtnVal===false) continue;
			datasArrayOfOldTrObjs[datasArrayOfOldTrObjs.length]=datasObj;
		}
	}
	if(hasDeleteData==false) return WX_SAVE_IGNORE;
	/**
	 * 将要删除本报表新增的tr对象数组及元数据放入WX_listReportDeleteInfo中，等后面用户确认后再完成删除操作。
	 */
	var delObj=new Object();
	delObj.metadataObj=metadataObj;
	if(delNewTrObjArr.length>0) delObj.delNewTrObjArr=delNewTrObjArr;
	if(WX_listReportDeleteInfo==null) WX_listReportDeleteInfo=new Array();
	WX_listReportDeleteInfo[WX_listReportDeleteInfo.length]=delObj;
	storeSavingData(metadataObj.reportguid,datasArrayOfOldTrObjs);
	return WX_SAVE_CONTINUE;
}

/**
 * 预保存editabledetail2报表类型的数据，此时将报表取出组装好存放到全局变量中
 * 如果返回null，则没有保存数据，否则返回datasObj
 */
function preSaveEditableDetail2ReportData(metadataObj,realUpdatetype)
{
	var tableObj=document.getElementById(metadataObj.reportguid+'_data');
	if(tableObj==null)
	{
		wx_warn('没有取到需要保存/删除数据的报表对象');
		return WX_SAVE_IGNORE;
	}
	var reportguid=metadataObj.reportguid;
	var datasObj=new Object();
	var tdChilds=tableObj.getElementsByTagName('TD');//取到<table/>下的所有<td/>
	if(tdChilds==null||tdChilds.length==0) return WX_SAVE_IGNORE;
	if(realUpdatetype==WX_SAVETYPE_DELETE)
	{//当前是删除操作
		var deleteconfirmmessage=metadataObj.metaDataSpanObj.getAttribute('deleteconfirmmessage');//获取到删除报表确认提示信息
		addDeleteConfirmMessage(getEditableReportDeleteConfirmMessage(tdChilds,deleteconfirmmessage));//得到实际提示信息（去替换掉动态内容部分）
		datasObj['WX_TYPE']='delete';//表示当前记录是被做删除操作
	}else
	{
		var savedatatype=metadataObj.metaDataSpanObj.getAttribute('savedatatype');
		if(savedatatype=='changed'&&(WX_UPDATE_ALLDATA==null||WX_UPDATE_ALLDATA[reportguid]==null||WX_UPDATE_ALLDATA[reportguid]==''))
		{//本报表只在有数据修改时才进行保存操作，且本报表没有修改数据
			return WX_SAVE_IGNORE;;
		}
		datasObj['WX_TYPE']='update';//表示当前记录是被做修改操作
	}
	var rtnVal=getAllSavingData(metadataObj,tdChilds,datasObj);
	if(rtnVal===false) return WX_SAVE_IGNORE;
	if(rtnVal===WX_SAVE_TERMINAT) return rtnVal;
	var datasArray=new Array();
	datasArray[datasArray.length]=datasObj;
	storeSavingData(metadataObj.reportguid,datasArray);
	return datasObj;
}

/**
 * 设置editable2编辑类型报表上<td/>单元格上的显示值，这里考虑到了如果<td/>中的内容需要被某些标签包裹时，则将显示内容设置到包裹标签中，而不是直接设置在<td/>标签中
 */
function setColDisplayValueToEditable2Td(tdObj,col_displayvalue)
{
	var colInnerWrapEle=getColInnerWrapElement(tdObj);
	if(colInnerWrapEle==null)
	{//如果本单元格内容没有被其它标签包裹，则直接设置在<td/>中
		tdObj.innerHTML=col_displayvalue;
	}else
	{//设置在包裹标签中
		colInnerWrapEle.innerHTML=col_displayvalue;
	}
}

/**
 * 获取单元格上的包裹单元格内容的标签元素对象
 */
function getColInnerWrapElement(tdObj)
{
	var children=tdObj.childNodes;
 	if(children==null||children.length==0) return null;
 	var childNodeTmp;
 	for(var i=0,len=children.length;i<len;i++)
 	{
 		childNodeTmp=children.item(i);
 		if(childNodeTmp.nodeType!=1) continue;
 		if(childNodeTmp.getAttribute('tagtype')=='COL_CONTENT_WRAP') return childNodeTmp;
 		childNodeTmp=getColInnerWrapElement(childNodeTmp);
 		if(childNodeTmp!=null) return childNodeTmp;
 	}
 	return null;
}

/**
 * 取到在<col/>中配置了formatemplate的列真正的显示值
 * @param formatemplate 配置的formatemplate值，里面的url/request{}等动态值已经在服务器端替换了，这里只要替换各列的新旧值
 * @param formatemplate_dyncols 在formatemplate各动态列的占位符及property/property__old的值
 * @param col_displayvalue 此列的值显示
 */
function getEditable2ColRealValueByFormatemplate(parentTdObj,reportguid,formatemplate,formatemplate_dyncols,col_displayvalue)
{
	if((col_displayvalue==null||col_displayvalue=='')&&(formatemplate.indexOf('[')!=0||formatemplate.lastIndexOf(']')!=formatemplate.length-1)) return col_displayvalue;
	if(formatemplate.indexOf('[')==0&&formatemplate.lastIndexOf(']')==formatemplate.length-1)
	{
		formatemplate=formatemplate.substring(1,formatemplate.length-1);
	}
	var col_valuename=parentTdObj.getAttribute('value_name');
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj.reportfamily==ReportFamily.EDITABLELIST2)
	{
		datasObj=wx_getListReportColValuesInRow(parentTdObj.parentNode,null);
	}else if(metadataObj.reportfamily==ReportFamily.EDITABLEDETAIL2)
	{
		datasObj=getEditableReportColValues(metadataObj.pageid,metadataObj.reportid,null,null);
	}else
	{
		return col_displayvalue;
	}
	if(formatemplate_dyncols==null||formatemplate_dyncols=='') return formatemplate;//没有动态列变量值需要替换
	var dyncolsArr=formatemplate_dyncols.split(';');
	var colplacehoderTmp,colnameTmp,colRealnameTmp,colvalueTmp;
	for(var i=0;i<dyncolsArr.length;i++)
	{//循环所有动态列
		if(dyncolsArr[i]==null||dyncolsArr[i]=='') continue;
		var idx=dyncolsArr[i].indexOf('=');
		if(idx<=0) continue;
		colplacehoderTmp=dyncolsArr[i].substring(0,idx);//动态列在formatemplate中的占位符
		if(colplacehoderTmp==null||colplacehoderTmp==''||formatemplate.indexOf(colplacehoderTmp)<0) continue;//已经没有此占位符了
		colnameTmp=dyncolsArr[i].substring(idx+1);//动态列名
		colRealnameTmp=colnameTmp.lastIndexOf('__old')>0?colnameTmp.substring(0,colnameTmp.lastIndexOf('__old')):colnameTmp;
		if(datasObj[colRealnameTmp]==null)
		{//没有取到此列的值
			colvalueTmp='';
		}else if(colRealnameTmp!=colnameTmp&&datasObj[colRealnameTmp].oldname!=null&&datasObj[colRealnameTmp].oldname!='')
		{//动态列名中有__old
			colvalueTmp=datasObj[colRealnameTmp].oldvalue;
		}else
		{//取用此列的新值
			if(col_valuename==colRealnameTmp)
			{
				colvalueTmp=col_displayvalue;
			}else
			{
				colvalueTmp=datasObj[colRealnameTmp].value;
			}
		}
		if(colvalueTmp==null) colvalueTmp='';
		idx=formatemplate.indexOf(colplacehoderTmp);
		while(idx>=0)
		{//将formatemplate中此列的占位符替换成此列的值
			formatemplate=formatemplate.substring(0,idx)+colvalueTmp+formatemplate.substring(idx+colplacehoderTmp.length);
			idx=formatemplate.indexOf(colplacehoderTmp);
		}
	}
	return formatemplate;
}

/****************************************************over*******************************************************/

/*********************************下面方法函数用于EditableDetailReportType及其子类型*******************************/

/**
 * 预保存editabledetail/form报表类型的数据，此时将报表取出组装好存放到全局变量中
 * 如果返回null，则没有保存数据，否则返回datasObj
 */
function preSaveEditableDetailReportData(metadataObj,realUpdatetype)
{
	var reportguid=metadataObj.reportguid;
	var fontChilds=document.getElementsByName('font_'+reportguid);//获取到所有name为'font_'+reportguid的<font/>数组
	if(fontChilds==null||fontChilds.length==0) return null;
	var datasObj=new Object();
	datasObj['WX_TYPE']=getEditableDetailRealUpdateType(metadataObj,realUpdatetype);
	if(datasObj['WX_TYPE']==null) return WX_SAVE_IGNORE;
	if(realUpdatetype==WX_SAVETYPE_DELETE)
	{//如果当前是删除操作
		var deleteconfirmmessage=metadataObj.metaDataSpanObj.getAttribute('deleteconfirmmessage');//获取到删除报表确认提示信息
		addDeleteConfirmMessage(getEditableReportDeleteConfirmMessage(fontChilds,deleteconfirmmessage));//得到实际提示信息（去替换掉动态内容部分）
	}else
	{
		var savedatatype=metadataObj.metaDataSpanObj.getAttribute('savedatatype');
		if(savedatatype=='changed'&&(WX_UPDATE_ALLDATA==null||WX_UPDATE_ALLDATA[reportguid]==null||WX_UPDATE_ALLDATA[reportguid]==''))
		{//本报表只在有数据修改时才进行保存操作，且本报表没有修改数据
			return WX_SAVE_IGNORE;
		}
	}
	var rtnVal=getAllSavingData(metadataObj,fontChilds,datasObj);
	if(rtnVal===WX_SAVE_TERMINAT) return rtnVal;
	if(rtnVal===false) return WX_SAVE_IGNORE;//没有保存数据
	var datasArray=new Array();
	datasArray[datasArray.length]=datasObj;
	storeSavingData(reportguid,datasArray);
	return datasObj;
}

/**
 * 获取editabledetail/form报表类型的真正保存类型
 * @param updatetype 按钮传入的类型，包括save和delete
 * @return 返回的值为add、update、delete和null，如果是null表示无效的保存操作
 */
function getEditableDetailRealUpdateType(metadataObj,updatetype)
{
	var realUpdatetype=null;
	if(updatetype==WX_SAVETYPE_DELETE)
	{//如果当前是删除操作
		realUpdatetype='delete';
	}else
	{
		var accessmode=metadataObj.metaDataSpanObj.getAttribute('current_accessmode');
		if(accessmode==WX_ACCESSMODE_ADD)
		{//当前报表是添加模式，则是保存添加数据
			realUpdatetype='add';
		}else if(accessmode==WX_ACCESSMODE_UPDATE)
		{//当前报表是修改模式，则是保存修改数据
			realUpdatetype='update';
		}
	}
	return realUpdatetype;
}

/**
 * 切换editabledetail/form访问模式
 */
function changeReportAccessMode(reportguid,newaccessmode)
{
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj==null) return;
	var url=getComponentUrl(metadataObj.pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
	if(url==null||url=='') return;
	if(newaccessmode=='') newaccessmode=null;
	var currentAccessmode=metadataObj.metaDataSpanObj.getAttribute('current_accessmode');
	if(newaccessmode==null)
	{//新的访问模式是默认访问模式，一般是点击“取消”按钮
		if(currentAccessmode==WX_ACCESSMODE_ADD)
		{//当前是在做添加操作，则取消后要重新计算页码
			url=removeReportNavigateInfosFromUrl(url,metadataObj,null);
		}
	}else
	{
		//如果新旧模式相等，也要调用refreshComponent(url)重新加载页面，不能中断，因为重置就是这种情况。
		if(newaccessmode!=currentAccessmode&&(currentAccessmode==WX_ACCESSMODE_ADD||newaccessmode==WX_ACCESSMODE_ADD))
		{//新旧模式不一致，且有一个模式是添加，则需重新计算页码
			url=removeReportNavigateInfosFromUrl(url,metadataObj,null);
		}
	}
	url=replaceUrlParamValue(url,metadataObj.reportid+'_ACCESSMODE',newaccessmode);
	refreshComponent(url);
}

/**
 * 当用户以添加模式进入可编辑细览报表时，则不管有没有录入数据都要添加到待保存容器中
 */
function addEditableDetailReportFoSaving(paramsObj)
{
	if(paramsObj==null||paramsObj.reportguid==null||paramsObj.reportguid=='') return;
	doAddDataForSaving(paramsObj.reportguid,'true');
}

/*******************************************over****************************************/

/*********************************下面方法函数用于EditableListReportType类型*******************************/

/**
 * 弹出添加/修改报表的编辑页面
 * @param strParamsObj 包含弹出URL及URL中参数值的json字符串
 * @param strPopupParamsObj 包含弹出窗口参数值的json字符串
 */
function popupEditReportPage(strParamsObj,strPopupParamsObj)
{
	var paramsObj=getObjectByJsonString(strParamsObj);
	var popupParamsObj=getObjectByJsonString(strPopupParamsObj);
	if(paramsObj==null) return;
	var pageurl=paramsObj.pageurl;
	if(pageurl==null||pageurl=='') return;
	var params=paramsObj.params;
	if(params!=null)
	{
		for(var key in params)
		{//将URL中所有动态参数加到URL中
			pageurl=pageurl+'&'+key+'='+encodeURIComponent(params[key]);
		}
	}
	wx_winpage(pageurl,popupParamsObj,paramsObj.beforePopupMethod);
}

/**
 * 被editabledetail报表类型保存完后调用，关闭弹出页面，并刷新源报表
 */
function closeMeAndRefreshParentReport(paramsObj)
{
	if(paramsObj==null) return;
	var shouldCloseme=paramsObj.closeme!='false';
	if(WXConfig.prompt_dialog_type=='ymprompt')
	{
		parent.refreshParentEditableListReport(paramsObj.pageid,paramsObj.reportid,paramsObj.edittype,shouldCloseme);
	}else
	{
		artDialog.open.origin.refreshParentEditableListReport(paramsObj.pageid,paramsObj.reportid,paramsObj.edittype,shouldCloseme);
	}
}

/**
 * 被弹出的编辑editablelist某记录行的页面调用，关闭弹出页面，并刷新源editablelist报表
 */
function refreshParentEditableListReport(pageid,reportid,edittype,closeme)
{
	if(pageid==null||pageid==''||reportid==null||reportid=='') return;
	var reportguid=getComponentGuidById(pageid,reportid);
	var reportMetadataObj=getReportMetadataObj(reportguid);
	if(reportMetadataObj==null) return;
	if(closeme===true) closePopupWin(1);
	//wx_success('保存成功');
	if(reportMetadataObj.slave_reportid==reportMetadataObj.reportid)
	{//如果当前报表是从报表，则判断是否要刷新其主报表
		var refreshParentReportidOnSave=reportMetadataObj.metaDataSpanObj.getAttribute('refreshParentReportidOnSave');//当前从报表保存时，需要刷新的主报表ID
		if(refreshParentReportidOnSave!=null&&refreshParentReportidOnSave!='')
		{
			var parentMetadataObj=getReportMetadataObj(getComponentGuidById(pageid,refreshParentReportidOnSave));
			var url=getComponentUrl(pageid,parentMetadataObj.refreshComponentGuid,parentMetadataObj.slave_reportid);
			if(reportMetadataObj.metaDataSpanObj.getAttribute('refreshParentReportTypeOnSave')=='true')
			{//需要重新计算主报表页码
				url=removeReportNavigateInfosFromUrl(url,parentMetadataObj,null);//删除顶层主报表的翻页导航ID以及查询条件关联的报表的翻页导航ID，重新计算其页码
			}
			refreshComponent(url);
			return;//刷新了主报表，则不用执行后面的刷新从报表操作，因为会自动刷新
		}
	}
	refreshComponentDisplay(pageid,reportid,false);
}
/*******************************************over****************************************/

/*********************************所有可编辑报表类型公用函数*******************************/

/**
 * 在调用setEditableReportColValue()方法设置列值时批量设置某列值的方法
 */
function setBatchEditableColValues(reportguid,colElementObjsArr,newValuesObj)
{
	if(newValuesObj==null||isEmptyMap(newValuesObj)) return;
	if(colElementObjsArr==null||colElementObjsArr.length==0) return;
	var colElementObj;
	for(var i=0,len=colElementObjsArr.length;i<len;i++)
	{
		colElementObj=colElementObjsArr[i];
		var valueName=colElementObj.getAttribute('value_name');
		var newValue=jsonParamDecode(newValuesObj[valueName]);
		if(newValue==null) continue;//当前列没有设置新值
		var newLabel=jsonParamDecode(newValuesObj[valueName+'$label']);
		if(newLabel==null) newLabel=newValue;
		setEditableColValue(colElementObj,newValue,newLabel);
	}
}

/**
 * 设置某列数据
 * @param colElementObj 列所在父标签对象（<font/>或<td/>）
 * @param newValue 要设置的新值
 * @param newLabel 新显示值，如果为null，则它的显示值为newValue
 */
function setEditableColValue(colElementObj,newValue,newLabel)
{
	if(colElementObj==null) return;
	var valuename=colElementObj.getAttribute('value_name');
	if(valuename==null||valuename=='') return;
	var realinputboxid=getInputboxIdByParentElementObj(colElementObj);
	if(realinputboxid==null||realinputboxid=='') return;
	var boxMetadataObj=getInputboxMetadataObj(realinputboxid);
	var reportguid=getReportGuidByInputboxId(realinputboxid);
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj==null) return;
	colElementObj.setAttribute('value',newValue);
	if(boxMetadataObj!=null&&boxMetadataObj.getAttribute('displayonclick')!=='true')
	{//当前列是可编辑列，且是直接显示输入框（而不是点击后再显示输入框）
		var updateDestTdObj=getUpdateColDestObj(colElementObj,reportguid,null);
		if(updateDestTdObj==null)
		{
			setInputBoxValueById(realinputboxid,newValue);
		}else
		{//当前列通过updatecol更新其它列
			setInputBoxLabelById(realinputboxid,newValue);
		}
	}else
	{//没有直接显示输入框的，则修改它的显示值
		if(newLabel==null) newLabel=newValue;
		if(newLabel==null) newLabel='';
		if(colElementObj.tagName=='FONT')
		{
			colElementObj.innerHTML=newLabel;
		}else
		{
			setColDisplayValueToEditable2Td(colElementObj,newLabel);
		}
	}
	var accessmode=metadataObj.metaDataSpanObj.getAttribute('current_accessmode');
	if(accessmode!=WX_ACCESSMODE_READONLY)
	{
		var oldvalue=colElementObj.getAttribute('oldvalue');
		if(oldvalue!=newValue) addElementDataForSaving(reportguid,colElementObj);
	}
	var onsetvaluemethods=metadataObj.metaDataSpanObj.getAttribute(valuename+'_onsetvaluemethods');
	var onsetvaluemethodObjs=getObjectByJsonString(onsetvaluemethods);
	if(onsetvaluemethodObjs!=null&&onsetvaluemethodObjs.methods!=null)
	{//此列配置有设置列值的回调函数
		var methodObjsArr=onsetvaluemethodObjs.methods;
		if(methodObjsArr.length>0)
		{
			for(var i=0,len=methodObjsArr.length;i<len;i++)
			{
				methodObjsArr[i].method(colElementObj,newValue,newLabel);
			}
		}
	}
	if(boxMetadataObj!=null&&boxMetadataObj.getAttribute('displayonclick')==='true')
	{
		var childboxids=boxMetadataObj.getAttribute('childboxids');
		if(childboxids!=null&&childboxids!='')
		{//有依赖当前下拉框的子下拉框，则更新子下拉框所在<td/>的值
			resetTdValue(colElementObj,colElementObj.getAttribute('oldvalue')==wx_getColValue(colElementObj));
		}
	}
}

/**
 * 保存一个或多个可编辑报表的数据
 * params结构为
 *		pageid:"pageid",
 *		savingReportIds:[{reportid:"reportid1",updatetype:"save"},{reportid:"reportid2",reporttype:"reportype2",updatetype:"delete|all"},...]
 *		updatetype有save、delete两种取值，
 *			对于editablelist2/listform两种数据自动列表报表，在delete时后面可以跟上|all表示删除本页面上此报表所有数据，如果没指定，则只删除选中的行。
 *			对于editabledetail/form两种报表类型，如果updatetype为save，则会从它的元数据中取出当前是add还是update
 */
function saveEditableReportDataImpl(paramsObj)
{
	if(paramsObj==null) return;
	WX_deleteConfirmessage=null;
	var url='';
	var pageid=paramsObj.pageid;
	var isExistDeleteAction=false;//本次保存是否存在删除数据的操作
	var isExistValidDeleteAction=false;//本次保存是否存在有效的删除数据的操作
	var isExistSaveAction=false;//本次保存是否有要保存的数据
	var isExistValidSaveAction=false;//本次保存是否有效的保存的数据
	var reportid,realUpdatetype,reportguid,metadataObj,saveReportParamsObjTmp;
	var savingReports=paramsObj.savingReportIds;//所有保存/删除的报表
	if(savingReports==null||savingReports.length==0) return;
	var parentReportidsOfSaveSlaveReportArr=new Array();//存放被从报表保存刷新的主报表ID集合
	var mSaveReportIds=new Object();//存放本次保存操作（不包括删除操作）的所有报表ID，以便保存时可以用于判断是否会因为刷新页面而忽略掉没保存的报表数据
	var reportids='';//存放本次需要保存和删除的所有报表ID，在保存时要将它们传到后台动态决定本次刷新的组件
	var saveParams=null;
	var savingReportsCnt=0;//需保存数据到后台的报表个数
	for(var i=0,len=savingReports.length;i<len;i=i+1)
	{
		if(savingReports[i]==null||!isValidSaveReportParamObj(savingReports[i])) continue;
		reportid=savingReports[i].reportid;
		realUpdatetype=savingReports[i].updatetype;
		if(realUpdatetype!=null&&realUpdatetype.indexOf('|')>0) realUpdatetype=realUpdatetype.substring(0,realUpdatetype.indexOf('|'));//因为更新类型为delete时，可能为delete|all格式表示删除所有记录
		reportguid=getComponentGuidById(pageid,reportid);
		metadataObj=getReportMetadataObj(reportguid);
		if(metadataObj==null)
		{//当前报表可能隐藏了
			wabacus_info('没有取到页面ID为'+pageid+',报表ID为'+reportid+'的元数据，无法对其进行保存操作，可能此报表没有显示出来');
			continue;
		}
		if(!isValidUpdateType(metadataObj,realUpdatetype)) continue;
		saveParams=doSaveEditableReportData(metadataObj,savingReports[i].updatetype);
		if(realUpdatetype==WX_SAVETYPE_DELETE) 
		{
			isExistDeleteAction=true;//对此报表是做删除操作
		}else
		{
			isExistSaveAction=true;
		}
		if(saveParams===WX_SAVE_IGNORE) continue;
		if(saveParams===WX_SAVE_TERMINAT) return;
		if(saveParams!=null&&saveParams!='')
		{//当前报表有需要更新到后台的保存数据
			mSaveReportIds[reportid]='true';
			if(realUpdatetype==WX_SAVETYPE_DELETE)
			{
				isExistValidDeleteAction=true;
			}else  
			{
				isExistValidSaveAction=true;
			}
			reportids+=reportid+';';
			url=getRealSavingUrl(metadataObj,url,parentReportidsOfSaveSlaveReportArr);
			url+=saveParams+'&'+reportid+'_SAVE_ORDER='+i;//加上此报表的保存顺序
			savingReportsCnt++;
		}else if(WX_listReportDeleteInfo!=null&&WX_listReportDeleteInfo.length>0)
		{
			isExistValidDeleteAction=true;
		}
	}
	if(savingReportsCnt>1&&reportids!='')
	{//本次需要保存多个报表的数据
		//因为可能保存多个，因此需要在这里指定其refreshComponentGuid为[DYNAMIC]reportid1;reportid2;...，这样动态确定本次刷新的guid，如果本次只刷新一个从报表，也会动态确定只更新这个从报表
		url=replaceUrlParamValue(url,'refreshComponentGuid','[DYNAMIC]'+reportids,true);//指定true，因为在loadpage()中要从URL中解析这个参数，所以不能对它编码，否则得到的编码值不好在客户端使用
	}
	if(!hasSaveData(url)&&(WX_listReportDeleteInfo==null||WX_listReportDeleteInfo.length==0))
	{
		if(isExistSaveAction===true&&isExistDeleteAction===true)
		{
		 	wx_warn('没有要保存和删除的数据');
		}else if(isExistSaveAction===true)
		{
			wx_warn('没有要保存的数据');
		}else
		{
			wx_warn('没有要删除的数据');
		}
		return;
	}
	var parentReportidsOfSaveSlaveReport='';
	for(var i=0;i<parentReportidsOfSaveSlaveReportArr.length;i++)
	{
		parentReportidsOfSaveSlaveReport+=parentReportidsOfSaveSlaveReportArr[i]+';';
	}
	if(parentReportidsOfSaveSlaveReport=='') parentReportidsOfSaveSlaveReport=null;
	url=replaceUrlParamValue(url,'SAVEDSLAVEREPORT_ROOTREPORT_IDS',parentReportidsOfSaveSlaveReport);
	if(isExistValidDeleteAction)
	{
		WX_saveWithDeleteUrl=url;
		WX_mSaveReportIds=mSaveReportIds;
		if(getDeleteConfirmMessageAsString()=='')
		{//没有删除确认提示信息，则直接删除
			doSaveEditableWithDelete('ok');
		}else
		{
			wx_confirm(getDeleteConfirmMessageAsString(),'删除',null,null,doSaveEditableWithDelete);
		}
	}else if(hasSaveData(url))
	{
		refreshComponent(url,mSaveReportIds);
	}
	WX_deleteConfirmessage=null;
}

/**
 * 获取保存数据的URL
 */
function getRealSavingUrl(metadataObj,savingurl,parentReportidsOfSaveSlaveReportArr)
{
	var updatetypesObj=getReportUpdateTypes(metadataObj);
	if(updatetypesObj==null) return savingurl;
	var resultSavingUrl=getComponentUrl(metadataObj.pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
	resultSavingUrl=mergeUrlParams(savingurl,resultSavingUrl,false);//合并到之前的URL中，因为可能同时保存多个报表数据
	if(metadataObj.slave_reportid==metadataObj.reportid||metadataObj.metaDataSpanObj.getAttribute('isSlaveDetailReport')=='true')
	{//如果当前报表是从报表，看一下保存时是否要刷新其主报表
		if(updatetypesObj['add']==true)
		{
			resultSavingUrl=addParentReportParamsToSavingUrl(metadataObj,parentReportidsOfSaveSlaveReportArr,resultSavingUrl,'OnInsert');
		}
		if(updatetypesObj['update']==true)
		{
			resultSavingUrl=addParentReportParamsToSavingUrl(metadataObj,parentReportidsOfSaveSlaveReportArr,resultSavingUrl,'OnUpdate');
		}
		if(updatetypesObj['delete']==true)
		{
			resultSavingUrl=addParentReportParamsToSavingUrl(metadataObj,parentReportidsOfSaveSlaveReportArr,resultSavingUrl,'OnDelete');
		}
	}
	if(updatetypesObj['add']===true||updatetypesObj['delete']===true)
	{//本次是对独立报表进行添加或删除操作，则重置翻页信息
		resultSavingUrl=removeNavigateInfoBecauseOfSave(metadataObj,resultSavingUrl);//删除本报表的翻页导航ID以及查询条件关联的报表的翻页导航ID，重新计算翻页信息
	}
	if(savingurl!=null&&savingurl!='') resultSavingUrl=replaceUrlParamValue(resultSavingUrl,'SLAVE_REPORTID',null);//已经有保存报表，说明本次保存多个报表，因此不能只刷新这一个从报表
	resultSavingUrl=removeLazyLoadParamsFromUrl(resultSavingUrl,metadataObj,false);//只删除本报表的延迟加载参数，不删除与其有查询条件关联的报表的延迟加载参数
	return resultSavingUrl;
}

/**
 * 判断某个从报表本次操作时是否要刷新某个主报表
 */
function addParentReportParamsToSavingUrl(metadataObj,parentReportidsOfSaveSlaveReportArr,savingurl,propertySuffix)
{
	var parentid=metadataObj.metaDataSpanObj.getAttribute('refreshParentReportid'+propertySuffix);
	if(parentid==null||parentid=='') return savingurl;
	if(metadataObj.slave_reportid==metadataObj.reportid)
	{//如果当前是依赖列表报表的从报表，要绑定保存主报表，则需要记下主报表ID，以便当主报表不是可编辑报表时也能通知它去保存从报表的数据
		parentReportidsOfSaveSlaveReportArr[parentReportidsOfSaveSlaveReportArr.length]=parentid;
	}
	var parentMetadataObj=getReportMetadataObj(getComponentGuidById(metadataObj.pageid,parentid));
	if(parentMetadataObj==null) return savingurl;
	var parenturl=getComponentUrl(metadataObj.pageid,parentMetadataObj.refreshComponentGuid,parentMetadataObj.slave_reportid);
	if(metadataObj.metaDataSpanObj.getAttribute('refreshParentReportType'+propertySuffix)=='true')
	{//需要重新计算主报表页码
		parenturl=removeReportNavigateInfosFromUrl(parenturl,parentMetadataObj,null);//删除指定主报表的翻页导航ID以及查询条件关联的报表的翻页导航ID，重新计算其页码
	}
	savingurl=mergeUrlParams(savingurl,parenturl,true);
	savingurl=replaceUrlParamValue(savingurl,'SLAVE_REPORTID',null);//已经有保存报表，说明本次保存多个报表，因此不能只刷新这一个从报表
	savingurl=removeNavigateInfoBecauseOfSave(metadataObj,savingurl);//删除本报表的翻页导航ID以及查询条件关联的报表的翻页导航ID，重新计算翻页信息
	return savingurl;
}

/**
 * 获取某个报表本次更新数据的类型，包括add、update、delete
 */
function getReportUpdateTypes(metadataObj)
{
	if(WX_ALL_SAVEING_DATA==null) return null;
	var savingDatasArr=WX_ALL_SAVEING_DATA[metadataObj.reportguid];
	if(savingDatasArr==null||savingDatasArr.length==0) return null;
	var updatetypesObj=new Object();
	var datasObjTmp;
	for(var i=0,len=savingDatasArr.length;i<len;i++)
	{
		datasObjTmp=savingDatasArr[i];
		if(datasObjTmp==null) continue;
		updatetypesObj[datasObjTmp['WX_TYPE']]=true;
	}
	return updatetypesObj;
}

var WX_mSaveReportIds;//本次保存操作要保存哪些报表ID，传入refreshComponent()使用
var WX_deleteConfirmessage;//删除报表数据时的确认提示信息
var WX_saveWithDeleteUrl;
var WX_listReportDeleteInfo;//删除的editablelist2/listform报表的新增记录行信息

function doSaveEditableWithDelete(input)
{
	if(wx_isOkConfirm(input)) 
	{
		if(WX_listReportDeleteInfo!=null&&WX_listReportDeleteInfo.length>0)
		{//本次删除了editablelist2/listform报表的新增记录行
			var delObjTmp;
			for(var i=0,len1=WX_listReportDeleteInfo.length;i<len1;i=i+1)
			{
				delObjTmp=WX_listReportDeleteInfo[i];
				if(delObjTmp==null||delObjTmp.metadataObj==null) continue;
				var updatedataForSaving=null;//此报表的待保存数据列表
				if(WX_UPDATE_ALLDATA!=null) updatedataForSaving=WX_UPDATE_ALLDATA[delObjTmp.metadataObj.reportguid];
				if(updatedataForSaving!=null&&updatedataForSaving.length==0) updatedataForSaving=null;
				//alert(updatedataForSaving);
				if(delObjTmp.delNewTrObjArr!=null&&delObjTmp.delNewTrObjArr.length>0)
				{//本报表有删除的新增记录行，则在此进行删除
					var newTrObjTmp;
					for(var j=0,len2=delObjTmp.delNewTrObjArr.length;j<len2;j=j+1)
					{
						newTrObjTmp=delObjTmp.delNewTrObjArr[j];
						if(updatedataForSaving!=null)
						{
							for(var k=0,len3=updatedataForSaving.length;k<len3;k=k+1)
							{//如果当前删除行在待保存队列中，则删除它，以免还对它进行保存操作或者在删除后进行其它操作时再提示“是否放弃修改”
								if(updatedataForSaving[k]==newTrObjTmp) 
								{
									updatedataForSaving.splice(k,1);
									if(updatedataForSaving.length==0)
									{
										if(WX_UPDATE_ALLDATA!=null) delete WX_UPDATE_ALLDATA[delObjTmp.metadataObj.reportguid];//从这里删除掉，以便在进行其它操作时判断是否有保存数据需要放弃
										break;
									}
								}
							}
						}
						newTrObjTmp.parentNode.removeChild(newTrObjTmp);
						setListReportChangedRowNumInClient(delObjTmp.metadataObj.reportguid,-1,false);//如果限制了最大记录数（即<table/>中在添加时增加了wx_MyChangedRowNumInClient属性），则改变客户端更改记录数
					}
				}
				if(WX_selectedTrObjs!=null) delete WX_selectedTrObjs[delObjTmp.metadataObj.reportguid];//将已删除的记录从选中记录中去掉
			}
		}
		if(hasSaveData(WX_saveWithDeleteUrl))
		{//如果需要保存或删除后台的数据（除了删除新增行的情况，其它情况都要更新后台数据）
			refreshComponent(WX_saveWithDeleteUrl,WX_mSaveReportIds);
		}
	}
	WX_saveWithDeleteUrl=null;
	WX_listReportDeleteInfo=null;
	WX_deleteConfirmessage=null;
	WX_mSaveReportIds=null;
}

/**
 * 校验传入的保存报表数据的参数是否有效
 */
function isValidSaveReportParamObj(saveReportParamObj)
{
	var reportid=saveReportParamObj.reportid;
	var updatetype=saveReportParamObj.updatetype;
	if(reportid==null||reportid=='')
	{
		wx_warn('保存数据失败,参数中没有取到报表ID');
		return false;
	}
	if(updatetype==null||updatetype=='')
	{
		wx_warn('保存数据失败，没有取到保存类型');
		return false;
	}
	return true;
}

/**
 * 校验针对指定类型的报表进行updatetype更新操作是否合法
 * 这里不校验metadataObj、updatetype为空的情况，在isValidSaveReportParamObj()方法中已经校验了
 */
function isValidUpdateType(metadataObj,updatetype)
{
	var reportfamily=metadataObj.reportfamily;
	if(reportfamily!=ReportFamily.EDITABLELIST2&&reportfamily!=ReportFamily.LISTFORM
		&&reportfamily!=ReportFamily.EDITABLEDETAIL2&&reportfamily!=ReportFamily.EDITABLELIST
		&&reportfamily!=ReportFamily.EDITABLEDETAIL&&reportfamily!=ReportFamily.FORM)
	{
		wx_warn(metadataObj.reportid+'对应的报表类型不可编辑，不能保存其数据');
		return false;
	}
	var accessmode=metadataObj.metaDataSpanObj.getAttribute('current_accessmode');
	if(accessmode==WX_ACCESSMODE_READONLY)
	{
		//wx_warn(metadataObj.reportid+'是只读访问模式，不能保存其数据');
		return false;
	}
	if(reportfamily==ReportFamily.EDITABLEDETAIL||reportfamily==ReportFamily.FORM)
	{
		if(updatetype==WX_SAVETYPE_DELETE)
		{//如果当前是做删除操作
			if(accessmode==WX_ACCESSMODE_ADD) return false;//如果当前是添加模式，则返回false，即不允许删除
		}else
		{//当前是做保存操作
			if(accessmode!=WX_ACCESSMODE_ADD&&accessmode!=WX_ACCESSMODE_UPDATE) return false;//如果不是编辑访问模式，则不允许做保存操作
		}
	}
	if(reportfamily==ReportFamily.EDITABLELIST&&updatetype!=WX_SAVETYPE_DELETE)
	{//editablelist报表类型，且不是对它做删除操作
		wx_warn(metadataObj.reportid+'报表为editablelist类型，不能直接对其做添加修改操作');
		return false;
	}
	return true;
}

function removeNavigateInfoBecauseOfSave(metadataObj,url)
{
	var reportfamily=metadataObj.reportfamily;
	var navigate_reportid=metadataObj.metaDataSpanObj.getAttribute('navigate_reportid');
	if(navigate_reportid==null||navigate_reportid=='') return url;//当前报表没有分页
	var accessmode=metadataObj.metaDataSpanObj.getAttribute('current_accessmode');
	if(accessmode==WX_ACCESSMODE_READONLY) return url;
	var currentPageno=getParamValueFromUrl(url,navigate_reportid+'_PAGENO');//从URL中取到本报表目前的访问页码
	url=removeReportNavigateInfosFromUrl(url,metadataObj,null);//删除掉本报表的所有分页信息，因为要重新计算它们的分页信息
	if(currentPageno!=null&&currentPageno!='')
	{//以前的URL已经有访问页码，则仍定位到这个页码
		url=url+'&'+navigate_reportid+'_DYNPAGENO='+currentPageno;//注意是用DYNPAGENO，而不是用PAGENO，因为重新计算翻页信息后，PAGENO参数指定无效，必须指定DYNPAGENO才有效。
	}
	return url;
}

/**
 * 执行报表保存操作
 */
function doSaveEditableReportData(metadataObj,updatetype)
{
	var realUpdatetype=(updatetype!=null&&updatetype.indexOf('|')>0)?updatetype=updatetype.substring(0,updatetype.indexOf('|')):updatetype;//因为更新类型为delete时，可能为delete|all格式表示删除所有记录
	var reportfamily=metadataObj.reportfamily;
	var reportguid=metadataObj.reportguid;
	var rtnVal=null;
	if(reportfamily==ReportFamily.EDITABLEDETAIL||reportfamily==ReportFamily.FORM)
	{
		rtnVal=preSaveEditableDetailReportData(metadataObj,realUpdatetype);
		if(rtnVal===WX_SAVE_TERMINAT||rtnVal===WX_SAVE_IGNORE) return rtnVal;//客户端校验没通过或没有数据需要保存
		realUpdatetype=getEditableDetailRealUpdateType(metadataObj,updatetype);
		//这里没有报表的保存数据也要继续执行，因为有可能本表单全部是自定义的数据if(datasObj===false||datasObj==null) return '';//没有保存数据
	}else if(reportfamily==ReportFamily.EDITABLELIST2||reportfamily==ReportFamily.LISTFORM)
	{
		if(realUpdatetype==WX_SAVETYPE_DELETE)
		{
			rtnVal=deleteListReportTypeData(metadataObj,updatetype);//这种报表类型的删除功能是在点击确定后再删除的
			if(rtnVal===WX_SAVE_TERMINAT||rtnVal===WX_SAVE_IGNORE) return rtnVal;
		}else
		{
			rtnVal=preSaveEditableListReportTypeData(metadataObj);
			if(rtnVal===WX_SAVE_TERMINAT||rtnVal===WX_SAVE_IGNORE) return rtnVal;//客户端校验没通过或没有数据需要保存
			realUpdatetype='';
		}
	}else if(reportfamily==ReportFamily.EDITABLEDETAIL2)
	{
		rtnVal=preSaveEditableDetail2ReportData(metadataObj,realUpdatetype);
		if(rtnVal===WX_SAVE_IGNORE||rtnVal===WX_SAVE_TERMINAT) return rtnVal;//没有保存数据
		realUpdatetype=rtnVal['WX_TYPE'];
	}else if(reportfamily==ReportFamily.EDITABLELIST)
	{//这种报表类型只有删除操作，添加修改操作都是通过editabledetail完成
		rtnVal=deleteListReportTypeData(metadataObj,updatetype);//这种报表类型的删除功能是在点击确定后再删除的
		if(rtnVal===WX_SAVE_TERMINAT||rtnVal===WX_SAVE_IGNORE) return rtnVal;
	}else
	{
		return WX_SAVE_IGNORE;
	}
	var beforeSaveActionStr=metadataObj.metaDataSpanObj.getAttribute('beforeSaveAction');//保存前客户端回调函数
	var beforeSaveActionMethod=getObjectByJsonString(beforeSaveActionStr);
	//alert(beforeSaveActionMethod);
	if(WX_ALL_SAVEING_DATA==null) WX_ALL_SAVEING_DATA=new Object();
	if(beforeSaveActionMethod!=null&&beforeSaveActionMethod.method!=null)
	{//如果有保存前客户端回调函数，则执行它
		var result=beforeSaveActionMethod.method(metadataObj.pageid,metadataObj.reportid,WX_ALL_SAVEING_DATA[metadataObj.reportguid]);
		if(result===WX_SAVE_IGNORE||result===WX_SAVE_TERMINAT) return result;//如果保存前客户端回调函数返回这两个值，则不进行此报表或此次的保存操作
	}
	var saveParams=addEditableReportSaveDataParams(metadataObj);
	/**
	 * 因为用户可能在保存前置动作中为此报表加上自定义数据
	 * 因此自定义数据需要在执行完保存前客户端回调函数执行完后再组装
	 */
	var customizedParams=addAllCustomizeParamValues(metadataObj,WX_ALL_SAVEING_DATA[metadataObj.reportguid],realUpdatetype);//加入用户自定义的待保存数据
	if(customizedParams!=null&&customizedParams!='') saveParams=saveParams+'&'+customizedParams;
	return saveParams;
}

/**
 * 从WX_ALL_SAVEING_DATA中取某个报表的编辑数据拼凑到URL参数中进行保存
 */
function addEditableReportSaveDataParams(metadataObj)
{
	var datasArray=WX_ALL_SAVEING_DATA[metadataObj.reportguid];
	if(datasArray==null||datasArray.length==0) return '';
	var datasObjTmp;
	var insertdatastr='',updatedatastr='',deletedatastr='';
	for(var i=0,len=datasArray.length;i<len;i++)
	{
		datasObjTmp=datasArray[i];
		if(datasObjTmp==null) continue;
		var edittype='',editparams='';
		for(var key in datasObjTmp)
		{
			if(key=='WX_TYPE') 
			{
				edittype=datasObjTmp[key];
			}else
			{
				editparams+=key+SAVING_NAMEVALUE_SEPERATOR+(datasObjTmp[key]==null?'[W-X-N-U-L-L]':datasObjTmp[key])+SAVING_COLDATA_SEPERATOR;
			}
		}
		if(editparams.lastIndexOf(SAVING_COLDATA_SEPERATOR)==editparams.length-SAVING_COLDATA_SEPERATOR.length)
		{
			editparams=editparams.substring(0,editparams.length-SAVING_COLDATA_SEPERATOR.length);
		}
		if(editparams==''||edittype==''||(edittype!='add'&&edittype!='update'&&edittype!='delete')) continue;
		if(edittype=='add')
		{
			insertdatastr+=editparams+SAVING_ROWDATA_SEPERATOR;
		}else if(edittype=='delete')
		{
			deletedatastr+=editparams+SAVING_ROWDATA_SEPERATOR;
		}else
		{//update
			updatedatastr+=editparams+SAVING_ROWDATA_SEPERATOR;
		}
	}
	if(insertdatastr.lastIndexOf(SAVING_ROWDATA_SEPERATOR)==insertdatastr.length-SAVING_ROWDATA_SEPERATOR.length)
	{
		insertdatastr=insertdatastr.substring(0,insertdatastr.length-SAVING_ROWDATA_SEPERATOR.length);
	}
	if(updatedatastr.lastIndexOf(SAVING_ROWDATA_SEPERATOR)==updatedatastr.length-SAVING_ROWDATA_SEPERATOR.length)
	{
		updatedatastr=updatedatastr.substring(0,updatedatastr.length-SAVING_ROWDATA_SEPERATOR.length);
	}
	if(deletedatastr.lastIndexOf(SAVING_ROWDATA_SEPERATOR)==deletedatastr.length-SAVING_ROWDATA_SEPERATOR.length)
	{
		deletedatastr=deletedatastr.substring(0,deletedatastr.length-SAVING_ROWDATA_SEPERATOR.length);
	}
	var resultStr='';
	if(insertdatastr!='') resultStr+='&'+metadataObj.reportid+'_INSERTDATAS='+encodeURIComponent(insertdatastr);
	if(updatedatastr!='') resultStr+='&'+metadataObj.reportid+'_UPDATEDATAS='+encodeURIComponent(updatedatastr);
	if(deletedatastr!='') resultStr+='&'+metadataObj.reportid+'_DELETEDATAS='+encodeURIComponent(deletedatastr);
	return resultStr;
}

/**
 * 如果某个报表有用户自定义的待保存数据，保存时将它组织好以便传入后台
 * @param updatetype ：当前保存操作的更新类型，可取值为add、update、delete，分别表示当前是在做增、改、删操作。
 *					   只有editablelist2/listform/editabledetail2三种报表类型的添加、修改数据时不用传入updatetype，其它情况都要根据情况传入add/update/delete
 * @return 加入自定义数据后的URL
 */
function addAllCustomizeParamValues(metadataObj,datasArray,updatetype)
{
	if(WX_CUSTOMIZE_DATAS==null) return '';
	var datasObj=WX_CUSTOMIZE_DATAS[metadataObj.reportguid];
	if(datasObj==null) return ''; 
	var datalist='';
	for(var key in datasObj)
	{
		if(datasObj[key]==null||datasObj[key]=='') continue;
		datalist=datalist+key+SAVING_NAMEVALUE_SEPERATOR+datasObj[key]+SAVING_COLDATA_SEPERATOR;
	}
	if(datalist.lastIndexOf(SAVING_COLDATA_SEPERATOR)==datalist.length-SAVING_COLDATA_SEPERATOR.length)
	{
		datalist=datalist.substring(0,datalist.length-SAVING_COLDATA_SEPERATOR.length);
	}
	if(datalist=='') return '';//没有用户定制的待保存数据
	if(updatetype!=null&&updatetype!='')
	{//如果指定了更新类型，只有editablelist2/listform/editabledetail2三种报表类型的添加、修改数据时不用传入updatetype，其它情况都要根据情况传入add/update/delete
		datalist=datalist+SAVING_COLDATA_SEPERATOR+'WX_UPDATETYPE'+SAVING_NAMEVALUE_SEPERATOR+updatetype;
		datasObj['WX_TYPE']='customize.'+updatetype;//表示此记录类型为用户自定义的
	}else
	{
		datasObj['WX_TYPE']='customize';//表示此记录类型为用户自定义的
	}
	if(datasArray==null) 
	{
		datasArray=new Array();
		storeSavingData(metadataObj.reportguid,datasArray);
	}
	datasArray[datasArray.length]=datasObj;//放进去以便保存回调函数需要时能用上
	delete WX_CUSTOMIZE_DATAS[metadataObj.reportguid];
	return metadataObj.reportid+'_CUSTOMIZEDATAS='+encodeURIComponent(datalist);
}

/**
 * 供用户调用添加要保存的自定义参数数据
 */
function setCustomizeParamValue(pageid,reportid,paramname,paramvalue)
{
	if(WX_CUSTOMIZE_DATAS==null) WX_CUSTOMIZE_DATAS=new Object();
	var reportguid=getComponentGuidById(pageid,reportid);
	var datasObj=WX_CUSTOMIZE_DATAS[reportguid];
	if(datasObj==null) 
	{
		datasObj=new Object();
		WX_CUSTOMIZE_DATAS[reportguid]=datasObj;
	}
	datasObj[paramname]=paramvalue;
	doAddDataForSaving(reportguid,'true');
}

/**
 * 判断某个URL中是否有需要保存的数据
 */
function hasSaveData(baseurl)
{
	if(baseurl==null||baseurl=='') return false;
	if(baseurl.indexOf('_INSERTDATAS=')<0&&baseurl.indexOf('_UPDATEDATAS=')<0&&baseurl.indexOf('_DELETEDATAS=')<0&&baseurl.indexOf('_CUSTOMIZEDATAS=')<0)
	{
		return false;
	}
	return true;
}

/**
 * 获取所有保存数据，没有取到保存数据时返回false，否则返回true
 */
function getAllSavingData(metadataObj,eleObjsArr,datasObj)
{
	if(eleObjsArr==null||eleObjsArr.length==0) return false;
	var eleObjTmp;
	var hasSavingData=false;
	for(var i=0,len=eleObjsArr.length;i<len;i++)
	{
		eleObjTmp=eleObjsArr[i];
		var valuename=eleObjTmp.getAttribute('value_name');
		if(valuename==null||valuename=='') continue;
		datasObj[valuename]=wx_getColValue(eleObjTmp);
		hasSavingData=true;
		if(datasObj['WX_TYPE']!=='add')
		{//当前记录不是添加操作，是修改或删除操作
			datasObj[valuename+'__old']=eleObjTmp.getAttribute('oldvalue');
		}
	}
	if(hasSavingData&&(datasObj==null||datasObj['WX_TYPE']!=='delete'))
	{//如果当前不是做删除操作，且有保存数据，则进行客户端校验
		for(var i=0,len=eleObjsArr.length;i<len;i++)
		{
			if(!validateEditColBoxValue(metadataObj,datasObj,eleObjsArr[i],false)) return WX_SAVE_TERMINAT;
		}
	}
	return hasSavingData;
}

/**
 * 将某个报表的保存数据存放到全局变量中
 */
function storeSavingData(reportguid,datasArray)
{
	if(WX_ALL_SAVEING_DATA==null) WX_ALL_SAVEING_DATA=new Object();
	WX_ALL_SAVEING_DATA[reportguid]=datasArray;//存进去以便保存回调函数需要时可以使用上
}

/**
 * 获取删除可编辑报表数据时的确认提示信息
 */
function getEditableReportDeleteConfirmMessage(eleObjsArr,deleteconfirmmessage)
{
	if(deleteconfirmmessage==null||deleteconfirmmessage.indexOf('@{')<0) return deleteconfirmmessage;//没有动态信息
	var mDynObj=new Object();//用于存放从deleteconfirmmessage中解析的所有动态数据的占位符和其对应的column
	deleteconfirmmessage=parseDeleteConfirmInfo(deleteconfirmmessage,mDynObj);
	var rowColDataObjs=wx_getAllColValueByParentElementObjs(eleObjsArr,null);//所有列的新旧数据
	if(rowColDataObjs==null) return deleteconfirmmessage;
	var paramnameTmp,paramvalueTmp;
	for(var key in mDynObj)
	{//依次循环配置的每个动态数据部分
		paramnameTmp=mDynObj[key];
		paramvalueTmp='';
		if(paramnameTmp!=null)
		{
			if(paramnameTmp.lastIndexOf('__old')==paramnameTmp.length-'__old'.length)
			{//引用的是此列的旧数据
				paramnameTmp=paramnameTmp.substring(0,paramnameTmp.lastIndexOf('__old'));
				if(rowColDataObjs[paramnameTmp]!=null) paramvalueTmp=rowColDataObjs[paramnameTmp].oldvalue;
			}else
			{
				if(rowColDataObjs[paramnameTmp]!=null) paramvalueTmp=rowColDataObjs[paramnameTmp].value;
			}
			if(paramvalueTmp==null) paramvalueTmp='';
		}
		deleteconfirmmessage=deleteconfirmmessage.replace(key,paramvalueTmp);
	}
	return deleteconfirmmessage;
}

/**
 * 解析配置的删除确认提示信息，将其中用@{}括住的动态字段名抽取出来
 * @param deleteconfirmmessage 配置的删除确认信息
 * @param mDynObj 存放动态内容部分，key为动态内容的在deleteconfirmmessage中的占位符，值为对应的column
 * @return 返回解析后包括占位符的删除确认信息
 */
function parseDeleteConfirmInfo(deleteconfirmmessage,mDynObj)
{
	if(deleteconfirmmessage==null||deleteconfirmmessage.indexOf('@{')<0) return deleteconfirmmessage;//没有动态信息
	var idx=deleteconfirmmessage.indexOf('@{');
	var str,column;
	var placeholder_idx=0;
	while(idx>=0)
	{
		str=deleteconfirmmessage.substring(0,idx);
		deleteconfirmmessage=deleteconfirmmessage.substring(idx+2);
		idx=deleteconfirmmessage.indexOf('}');
		if(idx<0) break;
		column=deleteconfirmmessage.substring(0,idx);
		deleteconfirmmessage=deleteconfirmmessage.substring(idx+1);
		mDynObj['PLACE_HOLDER_'+placeholder_idx]=column;
		deleteconfirmmessage=str+'PLACE_HOLDER_'+placeholder_idx+deleteconfirmmessage;//将占位符放入deleteconfirmmessage中以便后面替换
		placeholder_idx++;
		idx=deleteconfirmmessage.indexOf('@{');
	}
	return deleteconfirmmessage;
}


/**
 * 将某个报表的删除提示信息拼凑到全局删除确认提示信息中（因为可能一次删除多个报表）
 */
function addDeleteConfirmMessage(deleteconfirmmessage)
{
	if(deleteconfirmmessage==null||deleteconfirmmessage==''||deleteconfirmmessage=='none') return;
	if(WX_deleteConfirmessage==null) WX_deleteConfirmessage=new Array();
	for(var i=0;i<WX_deleteConfirmessage.length;i++)
	{
		if(WX_deleteConfirmessage[i]==deleteconfirmmessage) return;//已经存在
	}
	WX_deleteConfirmessage[WX_deleteConfirmessage.length]=deleteconfirmmessage;
}

/**
 * 获取删除确认提示信息字符串
 */
function getDeleteConfirmMessageAsString()
{
	if(WX_deleteConfirmessage==null) return '';
	var resultStr='';
	for(var i=0;i<WX_deleteConfirmessage.length;i++)
	{
		if(WX_deleteConfirmessage[i]==null||WX_deleteConfirmessage[i]==''||WX_deleteConfirmessage[i]=='none') return;//已经存在
		resultStr+=WX_deleteConfirmessage[i]+'\n';
	}
	return resultStr;
}

var WX_MGROUP_NAME_FLAG=new Object();

/**
 *对于某个输入是通过一组(name属性相同)输入框来完成的输入框类型，比如radiobox和checkbox等，
 *它们的onblur事件在获取输入框值时，必须通过下面这个函数。而不能直接调用fillInputBoxValueToParentTd
 *因为当是由于点击同一组的其它输入框失去焦点时，不能调用fillInputBoxValueToParentTd函数。
 */
function fillGroupBoxValue(boxObj)
{
	WX_MGROUP_NAME_FLAG[boxObj.getAttribute('id')]='';
	setTimeout(function(){doFillGroupBoxValue(boxObj);},130);//有130ms的延迟
}

function doFillGroupBoxValue(boxObj)
{
	var stop=WX_MGROUP_NAME_FLAG[boxObj.getAttribute('id')];
	//alert(stop);
	if(stop!=='true')
	{//新获取焦点的不是同一组的输入框（因为如果是同一组的输入框的话，它的onfocus事件会调用setGroupBoxStopFlag将标识设置true），此时回填新数据到td中。
		var inputboxid=boxObj.getAttribute('id');if(inputboxid.lastIndexOf('__')>0) inputboxid=inputboxid.substring(0,inputboxid.lastIndexOf('__'));
		fillBoxValueToParentElement(boxObj,inputboxid,true);
	} 
}

/**
 * 供被同组的输入框获得焦点时调用，阻止td调用fillInputBoxValueToParentTd方法
 */
function setGroupBoxStopFlag(boxObj)
{
	//alert('focus');
	WX_MGROUP_NAME_FLAG[boxObj.getAttribute('id')]='true';
}

/**
 * 加载自坛填充其它输入框的数据
 * @param inputboxObj 输入框对象
 * @param conditionprops 参与查询自动填充数据的条件的所有列的property组成的字符串
 */
function loadAutoCompleteInputboxData(inputboxObj,conditionprops)
{
	var parentElementObj=getInputboxParentElementObj(inputboxObj);
	var inputboxid=getInputboxIdByParentElementObj(parentElementObj);
	var reportguid=getReportGuidByInputboxId(inputboxid);
	var metadataObj=getReportMetadataObj(reportguid);
	var boxNewvalue=wx_getColValue(parentElementObj);
	if(boxNewvalue==inputboxObj.autoComplete_oldData) return;//此输入框数据没有变
	var conditionValuesObj=wx_getAllSiblingColValuesByInputboxid(inputboxid,conditionprops);
	if(conditionValuesObj==null) return;
	//下面组装所有参与条件的列的值传到后台查询数据时用
	var strConparams='';
	var dataObjTmp;
	var valueTmp;
	for(var key in conditionValuesObj)
	{
		dataObjTmp=conditionValuesObj[key];
		if(dataObjTmp==null) continue;
		valueTmp=dataObjTmp.value;
		if(valueTmp==null) valueTmp='';
		strConparams+=key+SAVING_NAMEVALUE_SEPERATOR+valueTmp+SAVING_COLDATA_SEPERATOR;
	}
	if(strConparams.lastIndexOf(SAVING_COLDATA_SEPERATOR)==strConparams.length-SAVING_COLDATA_SEPERATOR.length)
	{
		strConparams=strConparams.substring(0,strConparams.length-SAVING_COLDATA_SEPERATOR.length);
	}
	var url=getComponentUrl(metadataObj.pageid,metadataObj.refreshComponentGuid,metadataObj.slave_reportid);
   url=replaceUrlParamValue(url,'REPORTID',metadataObj.reportid);
   url=replaceUrlParamValue(url,'ACTIONTYPE','GetAutoCompleteFormData');
   url=replaceUrlParamValue(url,'INPUTBOXID',inputboxid);
   url=replaceUrlParamValue(url,'AUTOCOMPLETE_COLCONDITION_VALUES',strConparams);
   var paramsDataObj=new Object();//这个对象用于存放基本信息供回调函数使用
   paramsDataObj.pageid=metadataObj.pageid;
   paramsDataObj.reportid=metadataObj.reportid;
   paramsDataObj.inputboxid=inputboxid;
   sendAsynRequestToServer(url,fillAutoCompleteColsMethod,onGetAutoCompleteDataErrorMethod,paramsDataObj);
}

function fillAutoCompleteColsMethod(xmlHttpObj,paramsDataObj)
{
	var resultData=xmlHttpObj.responseText;
	if(resultData==null||resultData==' '||resultData=='') return;
	var resultDataObj=getObjectByJsonString(resultData);
	var inputboxid=paramsDataObj.inputboxid;
	var rowidx=inputboxid.lastIndexOf('__')>0?inputboxid.substring(inputboxid.lastIndexOf('__')+2):'';
	var trObj=rowidx!=''?document.getElementById(getReportGuidByInputboxId(inputboxid)+'_tr_'+rowidx):null;	
	if(trObj!=null)
	{//是可编辑列表报表
		var reportguid=getComponentGuidById(paramsDataObj.pageid,paramsDataObj.reportid);
		setEditableListReportColValueInRow(paramsDataObj.pageid,paramsDataObj.reportid,trObj,resultDataObj);
	}else
	{
		setEditableReportColValue(paramsDataObj.pageid,paramsDataObj.reportid,resultDataObj,null);
	}
}

function onGetAutoCompleteDataErrorMethod(xmlHttpObj)
{
	wx_error('获取自动填充表单域数据失败');
}

/**
 * 下面方法是用于公共场合演示时，不允许用户保存数据，并给出提示
 */
function stopSaveForDemo()
{
	wx_win("<div style='font-size:15px;color:#CC3399'><br>这里是公共演示，不允许保存数据到后台<br><br>您可以在本地部署WabacusDemo演示项目，进行完全体验，只需几步即可部署完成<br><br>WabacusDemo.war位于下载包的samples/目录中</div>",{lock:true});
	return false;
}

var WX_EDITSYSTEM_LOADED=true;//用于标识此js文件加载完