var SAVING_ROWDATA_SEPERATOR='_ROTAREPES_ATADWOR_GNIVAS_';//保存数据中每条记录间的分隔符
 
var SAVING_COLDATA_SEPERATOR='_ROTAREPES_ATADLOC_GNIVAS_';//保存数据中每列数据间的分隔符
 
var SAVING_NAMEVALUE_SEPERATOR='_ROTAREPES_EULAVEMAN_GNIVAS_';//保存数据中每个参数名和值之间的分隔符

var COL_NONDISPLAY_PERMISSION_PREX='[NOISSIMREP_YALPSIDNON]';//当某个列的数据不在页面展示，而是保存在session中时，放在其对应的参数名的前缀，比如被授权为不显示的列，或者密码框的oldvaluename

var ReportFamily=
	 {//定义框架支持的所有报表家庭，与Consts_Private类定义的一致
	 	LIST:'list',
		DETAIL:'detail',
		EDITABLELIST:'editablelist',
		EDITABLELIST2:'editablelist2',
		LISTFORM:'listform',
		EDITABLEDETAIL2:'editabledetail2',
		EDITABLEDETAIL:'editabledetail',
		FORM:'form'
	 };

var WX_ROWSELECT_TYPE=
	{//支持的行选中类型
		single:'single',
		multiple:'multiple',
		radiobox:'radiobox',
		checkbox:'checkbox',
		single_radiobox:'single-radiobox',
		multiple_checkbox:'multiple-checkbox',
		alltypes:{//所有类型
			'single':true,
			'multiple':true,
			'radiobox':true,
			'checkbox':true,
			'single-radiobox':true,
			'multiple-checkbox':true
		}
	};

/**
 * 存放同一页面中所有数据自动列表报表/表单中被选中的行对象
 * 此对象为一类似JAVA中的Map<reportGuid,Map<trid,trObj>>对象，其中reportGuid为报表的guid，Map<trid,trObj>为此报表所有被选中的<tr/>对象，以<tr/>的id为键，<tr/>对象为值。
 * 因此要获取某个报表所有被选中的行对象时，只需根据这个报表的<report/>的guid得到被选中行的<tr/>对象集合，循环它就可以得到所有被选中行的对象
 * 如果被选中的行对象是可编辑数据自动列表报表新增的<tr/>，则trObj.getAttribute('EDIT_TYPE')=='add'
 * 一般在行选中回调函数中可能会需要取用这些被选中行对象
 */
var WX_selectedTrObjs;

var ART_DIALOG_OBJ=null;//用winpage()弹出的弹出窗口界面对象



/**
 * 获取某个标签下的框架自动生成的输入框子标签对象（可以是直接子标签或间接子标签）
 * 框架自动生成的输入框对象必须有id属性，且id中包括_guid_，同时还必须有typename属性，表示相应输入框的类型名
 */
function getWXInputBoxChildNode(parentObj)
{
	var children=parentObj.childNodes;
 	if(!children||children.length==0) return null;//没有输入框
 	for(var i=0,len=children.length;i<len;i++)
 	{
 		if(children.item(i).nodeType!=1) continue;//说明不是Element
 		if(isWXInputBoxNode(children.item(i))) return children.item(i);
 		var boxObj=getWXInputBoxChildNode(children.item(i));
 		if(boxObj!=null) return boxObj;
 	}
 	return null;
}

/**
 * 判断某个标签是否是框架的输入框标签
 */
function isWXInputBoxNode(eleObj)
{
	if(eleObj==null||eleObj.nodeType!=1) return false;
	var id=eleObj.getAttribute('id');
 	if(id==null||id.indexOf('_guid_')<0) return false;
 	return id.indexOf('_wxcol_')>0||id.indexOf('_wxcondition_')>0;
}

/**
 * 获取某个标签下的输入框子标签
 */
function getInputBoxChildNode(parentObj)
{
	var children=parentObj.childNodes;
 	if(!children||children.length==0) return null;//没有输入框
 	for(var i=0,len=children.length;i<len;i++)
 	{
 		if(children.item(i).nodeType!=1) continue;//说明不是Element
 		return children.item(i);
 	}
 	return null;
}

/**
 * 获取存放某组件元数据的spanObj对象
 */
function getComponentMetadataObj(componentguid)
{
	if(componentguid==null||componentguid=='') return null;
	var metaDataSpanObj=document.getElementById(componentguid+'_metadata');
	if(metaDataSpanObj==null) return null;
	var resultObj=new Object();
	resultObj.pageid=metaDataSpanObj.getAttribute('pageid');
	resultObj.componentid=metaDataSpanObj.getAttribute('componentid');
	resultObj.componentguid=componentguid;
	resultObj.refreshComponentGuid=metaDataSpanObj.getAttribute('refreshComponentGuid');
	resultObj.componentTypeName=metaDataSpanObj.getAttribute('componentTypeName');//组件类型
	resultObj.metaDataSpanObj=metaDataSpanObj;
	return resultObj;
}

/**
 * 获取某个报表元数据的spanObj以及基本元数据
 */
function getReportMetadataObj(reportguid)
{
	if(reportguid==null||reportguid=='') return null;
	var resultObj=getComponentMetadataObj(reportguid);
	if(resultObj==null) return null;
	resultObj.reportid=resultObj.componentid;
	resultObj.reportguid=reportguid;
	resultObj.reportfamily=resultObj.metaDataSpanObj.getAttribute('reportfamily');
	var isSlaveReport=resultObj.metaDataSpanObj.getAttribute('isSlaveReport');
	if(isSlaveReport=='true') 
	{
		resultObj.slave_reportid=resultObj.reportid;
	}else
	{ 
		resultObj.slave_reportid=null;
	}
	return resultObj;
}

/**
 * 获取存放某个输入框的无数据的spanObj对象
 */
function getInputboxMetadataObj(inputboxid)
{
	if(inputboxid==null||inputboxid=='') return null;
	if(inputboxid.lastIndexOf('__')>0) inputboxid=inputboxid.substring(0,inputboxid.lastIndexOf('__'));
	return document.getElementById('span_'+inputboxid+'_span');
}

/**
 * 从页面上获取某个组件的更新URL，这里获取的只是当前页面状态下它的URL，可以在此基础上构造出它的搜索、翻页等URL
 * @param pageid 组件所在页面的id
 * @param refreshComponentGuid当前组件刷新时要更新的组件ID（可能是自己，也可能是其某层父容器的ID）
 * @param slave_reportid 如果当前是报表，且是从报表，这里存放从报表ID
 */
function getComponentUrl(pageid,refreshComponentGuid,slave_reportid)
{
	var pageurlSpanObj=document.getElementById(pageid+'_url_id');
	var resultUrl=pageurlSpanObj.getAttribute('value');
	if(slave_reportid!=null&&slave_reportid!='')
	{//当前是从报表
		//alert(getComponentGuidById(pageid,slave_reportid)+'_url_id');
		resultUrl=document.getElementById(getComponentGuidById(pageid,slave_reportid)+'_url_id').getAttribute('value');
	}
	if(!resultUrl||resultUrl=='')
	{
	 	wx_warn('获取组件URL失败，没有取到其原始url');
	 	return null;
	}
	resultUrl=paramdecode(resultUrl);
	var ancestorPageUrls=pageurlSpanObj.getAttribute('ancestorPageUrls');
	if(ancestorPageUrls!=null&&ancestorPageUrls!='')
	{//如果有祖先页面的URL，则带上，以便在服务器端可以判断是否要显示“返回”按钮，也可以避免刷新整个页面时生成存放URL的span时从rrequest中取不到而漏掉了。
	 //注意，如果本次是刷新从报表，也得加上，因为其上也可能显示“返回”按钮，没有传这个参数，则显示不了
		resultUrl=replaceUrlParamValue(resultUrl,'ancestorPageUrls',ancestorPageUrls);
	}
	if(refreshComponentGuid!=null&&refreshComponentGuid!='')
	{
		resultUrl=replaceUrlParamValue(resultUrl,'refreshComponentGuid',refreshComponentGuid);
	}
	if(slave_reportid!=null&&slave_reportid!='')
	{
		resultUrl=replaceUrlParamValue(resultUrl,'SLAVE_REPORTID',slave_reportid);
	}
	return resultUrl;
}

/**
 * 重置某个组件的访问URL，一般用于刷新此组件
 * @param url：原始URL
 * @param reportresetype 如果当前是在重置报表访问URL时，此参数表示重置哪一部分，
 *			目前可取值为navigate、navigate.false（表示保留当前显示的页码）、searchbox分别表示重置翻页导航信息和搜索条件，可以指定多个，用分号分隔。
 *			如果没有指定，则重置所有信息
 */
function resetComponentUrl(pageid,componentid,url,reportresetype)
{
	if(url==null||url=='') return url;
	var doc=document;
	var componentguid=getComponentGuidById(pageid,componentid);
	var cmetaDataObj=getComponentMetadataObj(componentguid);
	if(cmetaDataObj==null) return url;
	if(cmetaDataObj.componentTypeName=='application.report')
	{//当前组件是报表
		var reportMetaDataObj=getReportMetadataObj(componentguid);
		if(reportresetype==null||reportresetype==''||reportresetype.indexOf('navigate')>=0)
		{
			var currentPageno=null,navigate_reportid=null;;
			if(reportresetype!=null&&reportresetype.indexOf('navigate.false')>=0)
			{//要保留显示页码
				var navigate_reportid=reportMetaDataObj.metaDataSpanObj.getAttribute('navigate_reportid');
				if(navigate_reportid!=null&&navigate_reportid!='')
				{
					currentPageno=getParamValueFromUrl(url,navigate_reportid+'_PAGENO');//从URL中取到本报表目前的访问页码
				}
			}
			url=removeReportNavigateInfosFromUrl(url,reportMetaDataObj,null);//删除掉分页信息
			if(currentPageno!=null&&navigate_reportid!=null)
			{
				url=url+'&'+navigate_reportid+'_DYNPAGENO='+currentPageno;//保留页码
			}
		}
		if(reportresetype==null||reportresetype==''||reportresetype.indexOf('searchbox')>=0)
		{
			url=removeReportConditionBoxValuesFromUrl(url,reportMetaDataObj);//删除掉条件输入框中的信息
		}
	}else if(cmetaDataObj.componentTypeName=='container.tabspanel')
	{//当前组件提tabspanel
		url=replaceUrlParamValue(url,componentid+"_selectedIndex",null);//重置选中的tabitem下标
	}
	var childComponentIds=cmetaDataObj.metaDataSpanObj.getAttribute('childComponentIds');
	if(childComponentIds!=null&&childComponentIds!='')
	{//此组件有子组件，则依次初始化其子组件在URL中的参数，一般容器才会有子组件
		var childidsArr=childComponentIds.split(';');
		for(var i=0,len=childidsArr.length;i<len;i++)
		{
			if(childidsArr[i]==null||childidsArr[i]=='') continue;
			url=resetComponentUrl(pageid,childidsArr[i],url);
		}
	}
	return url;
}

/**
 * 根据输入框对象取到其父<font/>或父<td/>对象
 */
function getInputboxParentElementObjByTagName(inputboxObj,eleTagName)
{
	if(inputboxObj==null||eleTagName==null||eleTagName=='') return null;
	if(inputboxObj.dataObj!=null&&inputboxObj.dataObj.parentTdObj!=null) return inputboxObj.dataObj.parentTdObj;
	var parentEleObj=inputboxObj.parentNode;
	if(parentEleObj==null) return null;
	if(parentEleObj.tagName!=eleTagName) return getInputboxParentElementObjByTagName(parentEleObj,eleTagName);
	if(eleTagName=='FONT')
	{
		var fontid=parentEleObj.getAttribute('id');
		if(fontid==null||fontid.indexOf('font_')<0||fontid.indexOf('_guid_')<0)
		{
			return getInputboxParentElementObjByTagName(parentEleObj,eleTagName);
		}
	}else if(eleTagName=='TD')
	{
		var tdid=parentEleObj.getAttribute('id');
		if(tdid==null||tdid.indexOf('_td')<0||tdid.indexOf('_guid_')<0)
		{
			return getInputboxParentElementObjByTagName(parentEleObj,eleTagName);
		}
	}
	return parentEleObj;
}

/**
 * 获取输入框对象所在的父<font/>或父<td/>
 */
function getInputboxParentElementObj(inputboxObj)
{
	if(inputboxObj==null) return null;
	if(inputboxObj.dataObj!=null&&inputboxObj.dataObj.parentTdObj!=null) return inputboxObj.dataObj.parentTdObj;
	var parentEleObj=inputboxObj.parentNode;
	if(parentEleObj==null) return null;
	if(parentEleObj.tagName=='FONT')
	{
		var fontid=parentEleObj.getAttribute('id');
		if(fontid!=null&&fontid.indexOf('font_')==0&&fontid.indexOf('_guid_')>0) return parentEleObj;
	}else if(parentEleObj.tagName=='TD')
	{
		var tdid=parentEleObj.getAttribute('id');
		if(tdid!=null&&tdid.indexOf('_td')>0&&tdid.indexOf('_guid_')>0) return parentEleObj;
	}
	return getInputboxParentElementObj(parentEleObj);
}

/**
 * 根据列父标签获取报表GUID
 * @param 
 */
function getReportguidByParentElementObj(parentElementObj)
{
	if(parentElementObj==null) return null;
	var reportguid=null;
	if(parentElementObj.tagName=='FONT')
	{
		reportguid=parentElementObj.getAttribute('name');
		if(reportguid.indexOf('font_')!=0) return null;
		reportguid=reportguid.substring('font_'.length);
	}else
	{
		reportguid=getReportGuidByInputboxId(parentElementObj.getAttribute('id'));
	}
	return reportguid;
}

/**
 * 根据列父标签获取输入框ID
 * @param 
 */
function getInputboxIdByParentElementObj(parentElementObj)
{
	if(parentElementObj==null) return null;
	var reportguid=getReportguidByParentElementObj(parentElementObj);
	parentElementObj=getUpdateColSrcObj(parentElementObj,reportguid,parentElementObj);
	var inputboxid=null;
	if(parentElementObj.tagName=='FONT')
	{
		inputboxid=parentElementObj.getAttribute('inputboxid');
	}else if(parentElementObj.tagName=='TD')
	{
		var tdid=parentElementObj.getAttribute('id');
		var idx=tdid.lastIndexOf('__td');
		if(idx<=0) return null;
		if(idx==tdid.length-'__td'.length)
		{//以__td结尾，表示是可编辑细览报表的<td/>
			inputboxid=tdid.substring(0,idx);
		}else
		{//__td后面跟了行号，说明是可编辑列表报表的<td/>
			inputboxid=tdid.substring(0,idx)+'__'+tdid.substring(idx+'__td'.length);
		}
	}
	return inputboxid;
}

/**
 * 根据输入框的真正ID获取当前记录行序号，对于细览报表，返回-1，对于列表报表，返回大于等于0的数
 */
function getRowIndexByRealInputboxId(realinputboxid)
{
	var rowidx=-1;
	var idx=realinputboxid.lastIndexOf('__');
	if(idx>0)
	{
		var tmp=realinputboxid.substring(idx+2);
		if(tmp!=null&&tmp!='') rowidx=parseInt(tmp,10);
	}
	return rowidx;
}

/**
 * 获取标签元素对象数组（可能是<td/>的数组或<font/>的数组中所有value和oldvalue中的值对象）
 * @columnsObj 指定要获取哪些列的数据，指定格式为{col1:true,col2:true,....}，如果没有指定，即columnsObj为null，则获取所有列的新旧数据
 */
function wx_getAllColValueByParentElementObjs(parentElementObjsArr,columnsObj)
{
	if(parentElementObjsArr==null||parentElementObjsArr.length==0) return null;
	var resultDatasObj=null,eleObjTmp,dataObjTmp,value_name;
	for(var i=0,len=parentElementObjsArr.length;i<len;i=i+1)
	{
		eleObjTmp=parentElementObjsArr[i];
		value_name=eleObjTmp.getAttribute('value_name');
		if(value_name==null||value_name=='') continue;//不是有效数据列
		if(columnsObj!=null&&columnsObj[value_name]!='true'&&columnsObj[value_name]!==true) continue;//如果指定了要获取数据的列，但没有指定要获取此列的数据
		dataObjTmp=new Object();
		dataObjTmp.name=value_name;
		dataObjTmp.value=wx_getColValue(eleObjTmp);
		dataObjTmp.oldvalue=eleObjTmp.getAttribute('oldvalue');
		if(resultDatasObj==null) resultDatasObj=new Object();
		resultDatasObj[value_name]=dataObjTmp;
	}
	return resultDatasObj;
}

/**
 * 根据某列输入框ID获取到其对应的其它列的值
 * @param inputboxid 输入框ID
 * @param siblingColProperties 其它列的property组合字符串，以分号分隔
 */
function wx_getAllSiblingColValuesByInputboxid(inputboxid,siblingColProperties)
{
	if(inputboxid==null||inputboxid=='') return null;
	var rowidx=inputboxid.lastIndexOf('__')>0?inputboxid.substring(inputboxid.lastIndexOf('__')+2):'';
	var reportguid=getReportGuidByInputboxId(inputboxid);
	var trObj=rowidx!=''?document.getElementById(reportguid+'_tr_'+rowidx):null;
	var colvaluesObj;
	if(trObj!=null)
	{//是可编辑列表报表
		colvaluesObj=wx_getListReportColValuesInRow(trObj,createGetColDataObjByString(siblingColProperties));//取到本记录行中各参与查询自动填充报表数据条件的列的数据
	}else
	{//是可编辑细览报表
		colvaluesObj=getEditableReportColValues(getPageIdByComponentGuid(reportguid),getComponentIdByGuid(reportguid),createGetColDataObjByString(siblingColProperties),null);
	}
	return colvaluesObj;
}

/**
 * 根据用分号分隔的列property字符串，构造获取这些列数据的对象，例如colprops="col1;col2;"，则返回{col1:true;col2:true}
 * @param colprops 要获取数据的列的property字符串组合，如果有多个，用分号分隔
 */
function createGetColDataObjByString(colprops)
{
	if(colprops==null||colprops=='') return null;
	var colObj=new Object();//构造{col1:true,col2:true,...}格式的对象，用于传入获取报表数据的接口方法中指定要获取哪些列的数据
	var colpropArr=colprops.split(';');
	var hasValidCol=false;
	for(var i=0;i<colpropArr.length;i++)
	{
		if(colpropArr[i]==null||colpropArr[i]=='') continue;
		colObj[colpropArr[i]]=true;
		hasValidCol=true;
	}
	return hasValidCol?colObj:null;
}

/**
 * 获取某列数据（包括可编辑报表的列和不可编辑报表但因为某种原因（比如rowselectvalue为true的列）显示了value和value_name的列的数据）
 * @param colElementObj 列所在父标签对象（<font/>或<td/>）
 */
function wx_getColValue(colElementObj)
{
	if(colElementObj==null) return null;
	var valuename=colElementObj.getAttribute('value_name');
	if(valuename==null||valuename=='') return null; 
	var colvalue=colElementObj.getAttribute('value');
	if(colElementObj.tagName=='TD'&&colElementObj.parentNode.tagName=='TR'&&colElementObj.parentNode.getAttribute('wx_not_in_currentpage')==='true')
	{//当前行是跨页编辑时其它页的行
		return colvalue;
	}
	var reportguid=getReportguidByParentElementObj(colElementObj);
	if(reportguid!=null&&reportguid!='')
	{
		var metadataObj=getReportMetadataObj(reportguid);
		if(metadataObj!=null)
		{
			var realinputboxid=getInputboxIdByParentElementObj(colElementObj);
			if(realinputboxid!=null&&realinputboxid!='')
			{
				var boxMetadataObj=getInputboxMetadataObj(realinputboxid);
				if(boxMetadataObj!=null&&boxMetadataObj.getAttribute('displayonclick')!=='true')
				{//当前列是可编辑列，且是直接显示输入框（而不是点击后再显示输入框）
					var updateDestTdObj=getUpdateColDestObj(colElementObj,reportguid,null);
					if(updateDestTdObj==null)
					{
						colvalue=getInputBoxValueById(realinputboxid);
					}else
					{//当前列通过updatecol更新其它列
						colvalue=getInputBoxLabelById(realinputboxid);
					}
				}
			}
			var ongetvaluemethods=metadataObj.metaDataSpanObj.getAttribute(valuename+'_ongetvaluemethods');
			var ongetvaluemethodObjs=getObjectByJsonString(ongetvaluemethods);
			if(ongetvaluemethodObjs!=null&&ongetvaluemethodObjs.methods!=null)
			{//此列配置有设置列值的回调函数
				var methodObjsArr=ongetvaluemethodObjs.methods;
				if(methodObjsArr.length>0)
				{
					for(var i=0,len=methodObjsArr.length;i<len;i++)
					{
						colvalue=methodObjsArr[i].method(colElementObj,colvalue);
					}
				}
			}
		}
	}
	return colvalue;
}

/**
 * 获取某列通过updatecol更新的列的标签对象，如果没有配置updatecol，则返回defaultEleObj，一般是自己
 */
function getUpdateColDestObj(eleObj,reportguid,defaultEleObj)
{
	var updatecolDest=eleObj.getAttribute('updatecolDest');
	if(updatecolDest==null||updatecolDest=='') return defaultEleObj;
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj==null) return defaultEleObj;
	var reportfamily=metadataObj.reportfamily;
	var eleObjsArr=null;
	if(reportfamily==ReportFamily.EDITABLELIST2||reportfamily==ReportFamily.LISTFORM)
	{//此时eleObj为tdObj
		var trObj=eleObj.parentNode;
		eleObjsArr=trObj.getElementsByTagName('TD');
	}else if(reportfamily==ReportFamily.EDITABLEDETAIL2)
	{//此时eleObj为tdObj
		var tableObj=getParentElementObj(eleObj,'TABLE');
		eleObjsArr=tableObj.getElementsByTagName('TD');
	}else if(reportfamily==ReportFamily.EDITABLEDETAIL||reportfamily==ReportFamily.FORM)
	{
		eleObjsArr=document.getElementsByName('font_'+reportguid);
	}
	if(eleObjsArr==null) return defaultEleObj;
	for(var i=0;i<eleObjsArr.length;i++)
	{
		if(eleObjsArr[i].getAttribute('value_name')==updatecolDest) return eleObjsArr[i];
	}
	return defaultEleObj;
}

/**
 * 获取某列是被其它列通过updatecol引用更新，则返回更新此列的列
 */
function getUpdateColSrcObj(eleObj,reportguid,defaultEleObj)
{
	var updatecolSrc=eleObj.getAttribute('updatecolSrc');
	if(updatecolSrc==null||updatecolSrc=='') return defaultEleObj;
	var eleObjsArr=null;
	var metadataObj=getReportMetadataObj(reportguid);
	if(metadataObj==null) return defaultEleObj;
	var reportfamily=metadataObj.reportfamily;
	if(reportfamily==ReportFamily.EDITABLELIST2||reportfamily==ReportFamily.LISTFORM)
	{//此时eleObj为tdObj
		var trObj=eleObj.parentNode;
		eleObjsArr=trObj.getElementsByTagName('TD');
	}else if(reportfamily==ReportFamily.EDITABLEDETAIL2)
	{//此时eleObj为tdObj
		var tableObj=getParentElementObj(eleObj,'TABLE');
		eleObjsArr=tableObj.getElementsByTagName('TD');
	}else if(reportfamily==ReportFamily.EDITABLEDETAIL||reportfamily==ReportFamily.FORM)
	{
		eleObjsArr=document.getElementsByName('font_'+reportguid);
	}
	if(eleObjsArr==null) return defaultEleObj;
	for(var i=0;i<eleObjsArr.length;i++)
	{
		if(eleObjsArr[i].getAttribute('value_name')==updatecolSrc) return eleObjsArr[i];
	}
	return defaultEleObj;
}

/**
 * 根据组件id获取组件的guid
 * @param pageid 组件所在页面ID
 * @param componentId 组件ID
 */
function getComponentGuidById(pageid,componentId)
{
	if(componentId==null||componentId==''||componentId==pageid) return pageid;
	return pageid+WX_GUID_SEPERATOR+componentId;
}

/**
 * 根据组件GUID获取组件ID
 */
function getComponentIdByGuid(componentGuid)
{
	if(componentGuid==null||componentGuid=='') return null;
	var idx=componentGuid.lastIndexOf(WX_GUID_SEPERATOR);
	if(idx>0)
	{
		return componentGuid.substring(idx+WX_GUID_SEPERATOR.length);
	}
	return componentGuid;//没有包含WX_GUID_SEPERATOR，可能当前componentGuid就是页面ID
}

/**
 * 根据组件ID获取组件所在页面的ID
 */
function getPageIdByComponentGuid(componentGuid)
{
	if(componentGuid==null||componentGuid=='') return null;
	var idx=componentGuid.lastIndexOf(WX_GUID_SEPERATOR);
	if(idx>0)
	{
		return componentGuid.substring(0,idx);
	}
	return componentGuid;//没有包含WX_GUID_SEPERATOR，可能当前componentGuid就是页面ID
}

/**
 * 根据输入框ID获取报表的GUID
 */
function getReportGuidByInputboxId(inputboxid)
{
	if(inputboxid==null||inputboxid=='') return null;
	var idx=inputboxid.indexOf(WX_GUID_SEPERATOR);
	var pageid=inputboxid.substring(0,idx);
	var reportid=inputboxid.substring(idx+WX_GUID_SEPERATOR.length);
	idx=reportid.indexOf('_wxcol_');//编辑列上的输入框
	if(idx<=0) idx=reportid.indexOf('_wxcondition_');//查询条件上的输入框
	if(idx<=0)
	{
		wx_alert('输入框ID：'+inputboxid+'不合法，没有包含reportguid');
	 	return null;
	}
	reportid=reportid.substring(0,idx);
	return pageid+WX_GUID_SEPERATOR+reportid;
}

/***********************************************************************
 * 在某个标签上、下、左、右、内部显示提示信息
 */
/**
 * 为配置了失去焦点时进行客户端校验的输入框初始化出错信息提示窗口
 * @param ownerObj 输入框对象
 */
function createTipElementObj(ownerObj)
{
	var tooltip=
	{
		owner:ownerObj,
		myContainer:null,
		timer:null
	}
	var tipContainer =document.createElement("span");//创建信息提示窗口
	tipContainer.className='spanTipContainer';
	tipContainer.setAttribute("id",'id_'+parseInt((Math.random()*100000))+parseInt((Math.random()*100000)));
	tipContainer.onmouseover=function(){this.clearIntervalFlag=false;};
	tipContainer.onmouseout=function(){this.clearIntervalFlag=true;};
	var parentContainerObj=getNearestParentComonentContentDivObj(ownerObj);//取到最近的父组件<div/>容器，将提示框做为它的子节点，这样刷新页面时就能自动将这个提示框清掉
	if(parentContainerObj==null)
	{
		document.body.appendChild(tipContainer);
	}else 
	{
		parentContainerObj.appendChild(tipContainer);
	}
	tooltip.myContainer=tipContainer;
	tooltip.show = function (text,paramsObj) 
	{
		if (this.myContainer==null) return;
		this.myContainer.innerHTML = text;
		if(paramsObj==null) paramsObj=new Object();
		//将提示窗口定位在输入框之下
		var element=this.owner;
		if(element==null)	return;
		var pos=getElementAbsolutePosition(element);
		//alert(scrollHeight+'  '+scrollWidth);
		this.myContainer.style.display = "block";
		this.myContainer.style.position='absolute';
		if(paramsObj.width!=null&&paramsObj.width!='')
		{//通过参数指定了宽度
			if(parseInt(paramsObj.width)==0)
			{//与element相同宽度
				this.myContainer.style.width=pos.width+'px';
			}else
			{
				this.myContainer.style.width=paramsObj.width;
			}		
		}else
		{
			try
			{
				this.myContainer.style.width='';
			}catch(e)
			{}
		}
		if(paramsObj.height!=null&&paramsObj.height!='')
		{
			if(parseInt(paramsObj.height)==0)
			{//与element相同宽度
				this.myContainer.style.height=pos.height+'px';
			}else
			{
				this.myContainer.style.height=paramsObj.height;
			}	
		}else
		{
			try
			{
				this.myContainer.style.height='';
			}catch(e)
			{}
		}
		if(paramsObj.color!=null&&paramsObj.color!='')
		{
			this.myContainer.style.color=paramsObj.color;		
		}else
		{
			try
			{
				this.myContainer.style.color='';
			}catch(e)
			{}
		}
		if(paramsObj.bgcolor!=null&&paramsObj.bgcolor!='')
		{
			this.myContainer.style.backgroundColor=paramsObj.bgcolor;	
		}else
		{
			try
			{
				this.myContainer.style.backgroundColor='';
			}catch(e)
			{}
		}
		if(paramsObj.position=='left')
		{//显示在元素左边
			var left=pos.left-this.myContainer.offsetWidth;
			if(left<0) left=0;
			this.myContainer.style.left=left+'px';
			this.myContainer.style.top=pos.top+'px';
		}else if(paramsObj.position=='right')
		{//显示在元素右边
			var left=pos.left+pos.width;
			this.myContainer.style.left=left+'px';
			this.myContainer.style.top=pos.top+'px';
		}else if(paramsObj.position=='top')
		{//显示在元素上面
			this.myContainer.style.left=pos.left+'px';
			var top=pos.top-this.myContainer.offsetHeight;
			if(top<0) top=0;
			this.myContainer.style.top=top+'px';
		}else if(paramsObj.position=='inner')
		{//显示在元素内部
			this.myContainer.style.left=pos.left+'px';
			this.myContainer.style.top=pos.top+'px';
		}else
		{//bottom
			this.myContainer.style.left=pos.left+'px';
			this.myContainer.style.top=(pos.top+pos.height)+'px';
		}
		this.myContainer.clearIntervalFlag=true;//复位允许隐藏提示框的标识
		if(this.interval!=null)
		{//清除掉隐藏提示框的计时器
			clearInterval(this.interval);//清掉已经启动的计时器，重新计时
			this.interval=null;
		}
		if(paramsObj.hide==null||parseInt(paramsObj.hide,10)==0)
		{//点击鼠标时隐藏
			WX_tipObjs_hideOnclick[this.myContainer.id]=this.myContainer;
			document.body.onmousedown=checkAndHideTipOnClick;
		}else if(parseInt(paramsObj.hide,10)>0)
		{//定时隐藏掉
			var containerLocal=this.myContainer;
			this.interval=setInterval(function(){hideTipContainer(containerLocal);},parseInt(paramsObj.hide,10)*1000);
		}
		//else{不自动隐藏}
	}
	/*tooltip.hide = function () 
	{
		if (!this.myContainer) return;
		this.myContainer.innerHTML = "";
		this.myContainer.style.display = "none";
	}*/
	return tooltip;
}
var WX_tipObjs_hideOnclick=new Object();//当前正在显示的点击document时才隐藏的提示框
function checkAndHideTipOnClick(evt)
{
	var _target = evt ? evt.target : event.srcElement ;
	var existNotHideContainer=false;
	var container;
	for(var cid in WX_tipObjs_hideOnclick)
	{
		container=WX_tipObjs_hideOnclick[cid];
		if(container==null)
		{
			delete WX_tipObjs_hideOnclick[cid];
		}else if(isTipContainerOrChild(container,_target))
		{//点击的元素是本提示容器或其上的子元素
			existNotHideContainer=true;
		}else
		{
			hideTipContainer(container);
		}
	}
	if(existNotHideContainer)
	{//还有没有隐藏的提示框
		document.body.onmousedown=checkAndHideTipOnClick;
	}else
	{//所有提示框都隐藏了
		document.body.onmousedown=null;
	}
}

/**
 * 当前点击的元素是否是某个提示框容器或其子元素
 */
function isTipContainerOrChild(tipcontainer,clickTarget)
{
	if(tipcontainer==null) return false;
	while(clickTarget!=null)
	{
		try
		{
			if(clickTarget.getAttribute('id')==tipcontainer.getAttribute('id')) return true;
		}catch(e){}//因为如果点击到<body/>上面则没有getAttribute()方法，会抛出异常，所以这里要兼容这种情况
		clickTarget=clickTarget.parentNode;
	}
	return false;
}

/**
 * 隐藏提示信息容器对象
 */
function hideTipContainer(tipcontainer,milsec)
{
	if(tipcontainer==null) return;
	if(milsec!=null&&parseInt(milsec)>0)
	{//需要延迟隐藏
		tipcontainer.clearIntervalFlag=true;
		if(tipcontainer.interval!=null)
		{
			clearInterval(tipcontainer.interval);//清掉已经启动的计时器，重新计时
		}
		tipcontainer.interval=setInterval(function(){hideTipContainer(tipcontainer);},parseInt(milsec));
	}else
	{
		if(tipcontainer.clearIntervalFlag!==false)
		{
			tipcontainer.innerHTML = "";
			tipcontainer.style.display = "none";
			tipcontainer.enablehide=null;
			if(tipcontainer.getAttribute('id')!=null) delete WX_tipObjs_hideOnclick[tipcontainer.getAttribute('id')];
			if(tipcontainer.interval!=null) 
			{
				clearInterval(tipcontainer.interval);//清掉已经启动的计时器
				tipcontainer.interval=null;
			}
		}else
		{
			//还不能清除，等定时器下个周期来了再考虑清除，有可能此时鼠标还在提示框上，移出提示框时clearIntervalFlag就会变成true
		}
	}
}

/**
 * 获取某个HTML标签最近的父组件HTML容器
 */
function getNearestParentComonentContentDivObj(element)
{
	while(element!=null)
	{
		if(element==document.body||element.tagName=='DIV'&&element.id!=null&&element.id.indexOf('WX_CONTENT_'==0)) return element;
		element=element.parentNode;	
	}
	return null;
}
/****************************************************************************************
 * 显示“正在加载...”提示
 */
jQuery.fn.showLoading = function(options) {
		var indicatorID;
  		var settings = {
  			'addClass': '',
   		'beforeShow': '', 
  			'afterShow': '',
  			'hPos': 'center', 
   		'vPos': 'center',
  			'indicatorZIndex' : 5001, 
  			'overlayZIndex': 5000, 
   		'parent': '',
  			'marginTop': 0,
  			'marginLeft': 0,
   		'overlayWidth': null,
  			'overlayHeight': null
   	};
		jQuery.extend(settings, options);
      var loadingDiv = jQuery('<div></div>');
		var overlayDiv = jQuery('<div></div>');
		if ( settings.indicatorID ) 
		{
			indicatorID = settings.indicatorID;
		}else 
		{
			indicatorID = jQuery(this).attr('id');
		}
		jQuery(loadingDiv).attr('id', 'loading-indicator-' + indicatorID );
		jQuery(loadingDiv).addClass('loading-indicator');
		if ( settings.addClass )
		{
			jQuery(loadingDiv).addClass(settings.addClass);
		}
		jQuery(overlayDiv).css('display', 'none');//创建遮罩层
		jQuery(document.body).append(overlayDiv);
		//设置遮罩层的样式
		jQuery(overlayDiv).attr('id', 'loading-indicator-' + indicatorID + '-overlay');
		jQuery(overlayDiv).addClass('loading-indicator-overlay');
		if ( settings.addClass )
		{
			jQuery(overlayDiv).addClass(settings.addClass + '-overlay');
		}
		//设置样式
		var overlay_width,overlay_height;
		var border_top_width = jQuery(this).css('border-top-width');
		var border_left_width = jQuery(this).css('border-left-width');
		
		// IE will return values like 'medium' as the default border, but we need a number
		border_top_width = isNaN(parseInt(border_top_width)) ? 0 : border_top_width;
		border_left_width = isNaN(parseInt(border_left_width)) ? 0 : border_left_width;
		
		var overlay_left_pos = jQuery(this).offset().left + parseInt(border_left_width);
		var overlay_top_pos = jQuery(this).offset().top + parseInt(border_top_width);
		
		if ( settings.overlayWidth !== null ) 
		{
			overlay_width = settings.overlayWidth;
		}else 
		{
			overlay_width = parseInt(jQuery(this).width()) + parseInt(jQuery(this).css('padding-right')) + parseInt(jQuery(this).css('padding-left'));
		}

		if ( settings.overlayHeight !== null ) 
		{
			overlay_height = settings.overlayWidth;
		}else 
		{
			overlay_height = parseInt(jQuery(this).height()) + parseInt(jQuery(this).css('padding-top')) + parseInt(jQuery(this).css('padding-bottom'));
		}
		jQuery(overlayDiv).css('width', overlay_width.toString() + 'px');
		jQuery(overlayDiv).css('height', overlay_height.toString() + 'px');
		jQuery(overlayDiv).css('left', overlay_left_pos.toString() + 'px');
		jQuery(overlayDiv).css('position', 'absolute');
		jQuery(overlayDiv).css('top', overlay_top_pos.toString() + 'px' );
		jQuery(overlayDiv).css('z-index', settings.overlayZIndex);

      if ( settings.overlayCSS ) 
      {
       	jQuery(overlayDiv).css ( settings.overlayCSS );
      }

		jQuery(loadingDiv).css('display', 'none');
		jQuery(document.body).append(loadingDiv);
		
		jQuery(loadingDiv).css('position', 'absolute');
		jQuery(loadingDiv).css('z-index', settings.indicatorZIndex);

		var indicatorTop = overlay_top_pos;
		
		if ( settings.marginTop ) indicatorTop += parseInt(settings.marginTop);
		var indicatorLeft = overlay_left_pos;
		if ( settings.marginLeft ) indicatorLeft += parseInt(settings.marginTop);
		
		if ( settings.hPos.toString().toLowerCase() == 'center' ) 
		{
			jQuery(loadingDiv).css('left', (indicatorLeft + ((jQuery(overlayDiv).width() - parseInt(jQuery(loadingDiv).width())) / 2)).toString()  + 'px');
		}else if ( settings.hPos.toString().toLowerCase() == 'left' ) 
		{
			jQuery(loadingDiv).css('left', (indicatorLeft + parseInt(jQuery(overlayDiv).css('margin-left'))).toString() + 'px');
		}else if ( settings.hPos.toString().toLowerCase() == 'right' ) 
		{
			jQuery(loadingDiv).css('left', (indicatorLeft + (jQuery(overlayDiv).width() - parseInt(jQuery(loadingDiv).width()))).toString()  + 'px');
		}else 
		{
			jQuery(loadingDiv).css('left', (indicatorLeft + parseInt(settings.hPos)).toString() + 'px');
		}		
		
		if ( settings.vPos.toString().toLowerCase() == 'center' ) 
		{
			jQuery(loadingDiv).css('top', (indicatorTop + ((jQuery(overlayDiv).height() - parseInt(jQuery(loadingDiv).height())) / 2)).toString()  + 'px');
		}else if ( settings.vPos.toString().toLowerCase() == 'top' ) 
		{
			jQuery(loadingDiv).css('top', indicatorTop.toString() + 'px');
		}else if ( settings.vPos.toString().toLowerCase() == 'bottom' ) 
		{
			jQuery(loadingDiv).css('top', (indicatorTop + (jQuery(overlayDiv).height() - parseInt(jQuery(loadingDiv).height()))).toString()  + 'px');
		}else 
		{
			jQuery(loadingDiv).css('top', (indicatorTop + parseInt(settings.vPos)).toString() + 'px' );
		}		
      if ( settings.css ) jQuery(loadingDiv).css ( settings.css );

		var callback_options = 
		{
			'overlay': overlayDiv,
			'indicator': loadingDiv,
			'element': this
		};
	
		if ( typeof(settings.beforeShow) == 'function' ) settings.beforeShow( callback_options );
		
		jQuery(overlayDiv).show();
		
		jQuery(loadingDiv).show();

		if ( typeof(settings.afterShow) == 'function' ) settings.afterShow( callback_options );
		return this;
};

jQuery.fn.hideLoading = function(options){		
  		var settings = {};
  		jQuery.extend(settings, options);
		if ( settings.indicatorID ) 
		{
			indicatorID = settings.indicatorID;
		}else 
		{
			indicatorID = jQuery(this).attr('id');
		}
   	jQuery(document.body).find('#loading-indicator-' + indicatorID ).remove();
		jQuery(document.body).find('#loading-indicator-' + indicatorID + '-overlay' ).remove();
		return this;
};

var WX_UTIL_LOADED=true;//用于标识此js文件加载完