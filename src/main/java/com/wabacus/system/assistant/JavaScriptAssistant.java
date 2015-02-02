/* 
 * Copyright (C) 2010---2014 星星(wuweixing)<349446658@qq.com>
 * 
 * This file is part of Wabacus 
 * 
 * Wabacus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wabacus.system.assistant;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.IInputBoxOwnerBean;
import com.wabacus.system.inputbox.validate.JavascriptValidateBean;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class JavaScriptAssistant
{
    private final static JavaScriptAssistant instance=new JavaScriptAssistant();

    protected JavaScriptAssistant()
    {}

    public static JavaScriptAssistant getInstance()
    {
        return instance;
    }

    public void createRefreshSlaveReportsDataScript(ReportBean rbean)
    {
        if(rbean.getMDependChilds()==null||rbean.getMDependChilds().size()==0)
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"没有依赖它的从报表，不需生成刷新从报表的javascript函数");
        }
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function "+rbean.getRefreshSlaveReportsCallBackMethodName()+"(pageid,reportid,selectedTrObjArr,deselectedTrObjArr){");
        resultBuf.append(getRefreshSlaveReportsScriptString(rbean));
        resultBuf.append("}");
        writeJsMethodToJsFiles(rbean.getPageBean(),resultBuf.toString());
    }
    
    private String getRefreshSlaveReportsScriptString(ReportBean rbean)
    {
        StringBuffer scriptBuf=new StringBuffer();
        scriptBuf.append("var reportguid=getComponentGuidById(pageid, reportid);");
        scriptBuf.append("var metadataObj=getReportMetadataObj(reportguid);");
        scriptBuf.append("var oldSlaveTrObj=getRealCurrentSlaveTrObjForReport(reportguid);");
        scriptBuf.append("if(oldSlaveTrObj!=null&&(selectedTrObjArr==null||selectedTrObjArr.length==0)&&(deselectedTrObjArr==null||deselectedTrObjArr.length==0))");
        scriptBuf.append("{");
        scriptBuf.append("  doSelectListReportDataRow(metadataObj,oldSlaveTrObj,true,false);");
        scriptBuf.append("  return;");
        scriptBuf.append("}");
        scriptBuf.append("var currentSlaveTrObj=null;");
        scriptBuf.append("if (selectedTrObjArr == null || selectedTrObjArr.length <= 0) selectedTrObjArr = getListReportSelectedTrObjs(pageid, reportid, false, true);");
        scriptBuf.append("if (selectedTrObjArr == null || selectedTrObjArr.length <= 0)");
        scriptBuf.append("{");
        scriptBuf.append("  if(deselectedTrObjArr==null||deselectedTrObjArr.length==0)");
        scriptBuf.append("  {");
        scriptBuf.append("    var tableObj=document.getElementById(reportguid + '_data');");
        scriptBuf.append("    if(tableObj==null) return;");
        scriptBuf.append("    var trChilds = tableObj.getElementsByTagName('TR');");
        scriptBuf.append("    if(trChilds==null||trChilds.length==0) return;");
        scriptBuf.append("    var trObjTmp;");
        scriptBuf.append("    for (var i = 0, len = trChilds.length; i < len; i++) ");
        scriptBuf.append("    {");
        scriptBuf.append("      trObjTmp = trChilds[i];");
        scriptBuf.append("      if (!isListReportDataTrObj(trObjTmp)) continue;");
        scriptBuf.append("      var trtype = trObjTmp.getAttribute('EDIT_TYPE');");
        scriptBuf.append("      if (trtype == null || trtype != 'add')");
        scriptBuf.append("      {");
        scriptBuf.append("        doSelectListReportDataRow(metadataObj,trObjTmp,true,false);");
        scriptBuf.append("        return;");//因为在上面的选中函数中会执行行选中回调函数，因此会重新调用此方法刷新从报表
        scriptBuf.append("      }");
        scriptBuf.append("    }");
        scriptBuf.append("  }");
        scriptBuf.append("}else");
        scriptBuf.append("{");
        scriptBuf.append("  var trObjTmp;");
        scriptBuf.append("  for (var i = selectedTrObjArr.length - 1; i >= 0; i--)");
        scriptBuf.append("  {");
        scriptBuf.append("    trObjTmp = selectedTrObjArr[i];");
        scriptBuf.append("    var trtype = trObjTmp.getAttribute('EDIT_TYPE');");
        scriptBuf.append("    if (trtype == null || trtype != 'add')");
        scriptBuf.append("    {");
        scriptBuf.append("      if(isCurrentSlaveTrObjOfReport(reportguid,trObjTmp)) return;");
        scriptBuf.append("      currentSlaveTrObj = trObjTmp;");
        scriptBuf.append("      break;");
        scriptBuf.append("    }");
        scriptBuf.append("  }");
        scriptBuf.append("}");
        scriptBuf.append("setCurrentSlaveTrObjForReport(reportguid,currentSlaveTrObj);");
        scriptBuf.append("if(currentSlaveTrObj==null)");
        scriptBuf.append("{");
        scriptBuf.append("  var slaveReportSpanObjTmp=null;var staticlinkparams='';");
        scriptBuf.append(   hideAllSlaveReports(rbean,null));
        scriptBuf.append("}else");
        scriptBuf.append("{");
        scriptBuf.append(   refreshAllSlaveReports(rbean));//刷新所有从报表
        scriptBuf.append("}");
        return scriptBuf.toString();
    }

    private String hideAllSlaveReports(ReportBean rbean,Boolean parentDisplayWhenNoData)
    {
        StringBuffer scriptBuf=new StringBuffer();
        ReportBean rbeanTmp;
        String slaveidTmp;
        scriptBuf.append("var serverurl='"+Config.showreport_onpage_url+"&PAGEID='+pageid;");
        for(Entry<String,Map<String,String>> reportEntries:rbean.getMDependChilds().entrySet())
        {
            slaveidTmp=reportEntries.getKey();
            rbeanTmp=rbean.getPageBean().getReportChild(slaveidTmp,true);
            boolean mydisplayWhenNoData=rbeanTmp.isDisplayOnParentNoData();
            if(parentDisplayWhenNoData!=null&&parentDisplayWhenNoData==false)
            {
                mydisplayWhenNoData=false;
            }
            if(!mydisplayWhenNoData)
            {
                scriptBuf.append("slaveReportSpanObjTmp=document.getElementById('WX_CONTENT_").append(rbeanTmp.getGuid()).append("');");
                scriptBuf.append("if(slaveReportSpanObjTmp!=null) slaveReportSpanObjTmp.innerHTML='&nbsp;';");
            }else
            {
                Map<String,String> mParamsTmp=reportEntries.getValue();
                scriptBuf.append("staticlinkparams='';");//存放刷新每个从报表所配置的常量参数
                for(Entry<String,String> paramEntry:mParamsTmp.entrySet())
                {
                    if(!Tools.isDefineKey("@",paramEntry.getValue()))
                    {
                        scriptBuf
                                .append("staticlinkparams=staticlinkparams+'&"+paramEntry.getKey()+"='+encodeURIComponent('"+paramEntry.getValue()+"');");
                    }
                }
                scriptBuf.append("refreshComponent(serverurl+'&WX_ISREFRESH_BY_MASTER=true&"+slaveidTmp+"_PARENTREPORT_NODATA=true&SLAVE_REPORTID="+slaveidTmp+"'+staticlinkparams);");
            }
            if(rbeanTmp.getMDependChilds()!=null) scriptBuf.append(hideAllSlaveReports(rbeanTmp,mydisplayWhenNoData));
        }
        return scriptBuf.toString();
    }
    
    private String refreshAllSlaveReports(ReportBean rbean)
    {
        StringBuffer scriptBuf=new StringBuffer();
        scriptBuf.append("var linkparams=getRefreshSlaveReportsHrefParams(currentSlaveTrObj);");
        scriptBuf.append("if(linkparams==null||linkparams==''){wx_warn('没有取到刷新从报表数据的动态参数，刷新失败');return false;}");
        scriptBuf.append("var serverurl='"+Config.showreport_onpage_url+"&'+linkparams+'&PAGEID='+pageid;");
        scriptBuf.append("var staticlinkparams;");
        for(Entry<String,Map<String,String>> reportEntries:rbean.getMDependChilds().entrySet())
        {
            String slaveid=reportEntries.getKey();
            Map<String,String> mParamsTmp=reportEntries.getValue();
            scriptBuf.append("staticlinkparams='';");
            for(Entry<String,String> paramEntry:mParamsTmp.entrySet())
            {
                if(!Tools.isDefineKey("@",paramEntry.getValue()))
                {//当前参数是常量参数
                    scriptBuf
                            .append("staticlinkparams=staticlinkparams+'&"+paramEntry.getKey()+"='+encodeURIComponent('"+paramEntry.getValue()+"');");
                }
            }
            ReportBean slaverbean=rbean.getPageBean().getSlaveReportBean(slaveid);
            if(slaverbean==null)
            {
                throw new WabacusConfigLoadingException("为报表"+rbean.getPath()+"生成刷新从报表数据的javascript函数失败，没有取到"+slaveid+"对应的从报表");
            }
            scriptBuf.append("refreshComponent(serverurl+'&WX_ISREFRESH_BY_MASTER=true&SLAVE_REPORTID="+slaveid+"'+staticlinkparams);");
        }
        return scriptBuf.toString();
    }

    public void createComponentOnloadScript(IComponentConfigBean componentBean)
    {
        List<OnloadMethodBean> lstOnloadMethods=componentBean.getLstOnloadMethods();
        if(lstOnloadMethods==null||lstOnloadMethods.size()==0) return;
        Collections.sort(lstOnloadMethods);
        StringBuffer scriptBuf=new StringBuffer();
        for(OnloadMethodBean methodBeanTmp:lstOnloadMethods)
        {
            if(Consts_Private.ONLOAD_CONFIG.equalsIgnoreCase(methodBeanTmp.getType()))
            {
                scriptBuf.append(methodBeanTmp.getMethod()).append("('").append(componentBean.getPageBean().getId()).append("','");
                scriptBuf.append(componentBean.getId()).append("');");
            }else if(Consts_Private.ONlOAD_IMGSCROLL.equalsIgnoreCase(methodBeanTmp.getType())
                    ||Consts_Private.ONlOAD_CURVETITLE.equalsIgnoreCase(methodBeanTmp.getType()))
            {
                scriptBuf.append(methodBeanTmp.getMethod()).append(";");
            }else if(Consts_Private.ONLOAD_REFRESHSLAVE.equalsIgnoreCase(methodBeanTmp.getType()))
            {//是刷新从报表的js函数，则不传入pageid和reportid两个参数
                scriptBuf.append(methodBeanTmp.getMethod()).append("('").append(componentBean.getPageBean().getId()).append("','").append(
                        componentBean.getId()).append("',null,null);");
            }
        }
        if(scriptBuf.toString().trim().equals("")) return;
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("function "+componentBean.getOnloadMethodName()+"(){");
        resultBuf.append(scriptBuf.toString());
        resultBuf.append("}");
        writeJsMethodToJsFiles(componentBean.getPageBean(),resultBuf.toString());
    }
    
    public void createInputBoxValidateMethod(AbsInputBox inputboxObj)
    {
        String jsvalidate=inputboxObj.getJsvalidate();
        if(jsvalidate==null||jsvalidate.trim().equals("")) return;
        List<String> lstJsMethods=Tools.parseStringToList(jsvalidate.trim(),";",new String[] { "'", "'" },false);
        if(lstJsMethods==null||lstJsMethods.size()==0) return;
        StringBuilder scriptBuf=new StringBuilder();
        scriptBuf.append("function validate_"+inputboxObj.getOwner().getInputBoxId()+"(metadataObj,boxMetadataObj,boxValue,boxObj,paramsObj,isOnblur){");
        JavascriptValidateBean jsvalidateBean=new JavascriptValidateBean(inputboxObj);
        jsvalidateBean.setValidatetype(inputboxObj.getJsvalidatetype());
        IInputBoxOwnerBean ownerBean=inputboxObj.getOwner();
        jsvalidateBean.addParamBean("inputbox_label",ownerBean.getLabel(null),true);
        if(ownerBean instanceof ConditionBean)
        {
            ConditionBean cbTmp=(ConditionBean)ownerBean;
            if(cbTmp.getLabel()!=null&&!cbTmp.getLabel().trim().equals("")&&ConditionBean.LABELPOSITION_INNER.equals(cbTmp.getLabelposition()))
            {
                scriptBuf.append("if(boxValue==paramsObj.inputbox_label) boxValue='';");
            }
        }
        scriptBuf.append("var errorpromptparamsObj=null;");
        scriptBuf
                .append("if(boxMetadataObj!=null) errorpromptparamsObj=getObjectByJsonString(boxMetadataObj.getAttribute('errorpromptparamsonblur'));");
        for(String jsMethodTmp:lstJsMethods)
        {
            if(jsMethodTmp==null||jsMethodTmp.trim().equals("")) continue;
            jsMethodTmp=jsMethodTmp.trim();
            String methodname=jsMethodTmp;
            int lidx=jsMethodTmp.indexOf("(");
            int ridx=jsMethodTmp.lastIndexOf(")");
            if(lidx>0&&lidx<ridx)
            {//自己提供出错信息提示
                methodname=jsMethodTmp.substring(0,lidx);
                if(methodname.equals("")) continue;
                jsvalidateBean.parseConfigValidateMethodParams(methodname,jsMethodTmp.substring(lidx+1,ridx).trim());
            }
            scriptBuf.append("if(!"+methodname+"(boxValue,boxObj,paramsObj))");
            scriptBuf.append("{");
            scriptBuf.append("  var errormess=paramsObj."+methodname+"_errormessage;");
            scriptBuf.append("  if(errormess!=null&&errormess!='')");
            scriptBuf.append("  {");
            scriptBuf.append("      errormess=errormess.replace(/#label#/g,paramsObj.inputbox_label);");
            scriptBuf.append("      errormess=errormess.replace(/#data#/g,boxValue);");
            scriptBuf.append("      var tiparamsObj=paramsObj."+methodname+"_tiparams;if(tiparamsObj==null) tiparamsObj=errorpromptparamsObj;");
            scriptBuf.append("      if(errormess!=null&&errormess!='')");
            scriptBuf.append("      {");
            scriptBuf.append("          if(isOnblur===true){wx_jsPromptErrorOnblur(metadataObj,boxObj,errormess,tiparamsObj);}else{wx_warn(errormess);}");
            scriptBuf.append("      }");
            scriptBuf.append("  }");
            scriptBuf.append("  return false;");
            scriptBuf.append("}else{");
            scriptBuf.append("  wx_hideJsPromptErrorOnblur(metadataObj,boxObj);");//隐藏掉出错提示框
            scriptBuf.append("}");
        }
        scriptBuf.append("return true;}");
        writeJsMethodToJsFiles(ownerBean.getReportBean().getPageBean(),scriptBuf.toString());
        ownerBean.getReportBean().addInputboxJsValidateBean(ownerBean.getInputBoxId(),jsvalidateBean);
    }

    public void writeJsMethodToJsFiles(PageBean pagebean,String jsMethodContent)
    {
        if(jsMethodContent!=null&&!jsMethodContent.trim().equals(""))
        {
            writeJsMethodToJsFiles(pagebean.getJsFilePath(),jsMethodContent);
            pagebean.setShouldIncludeAutoCreatedJs(true);
        }
    }
    
    public void writeJsMethodToJsFiles(String jsPath,String jsMethodContent)
    {
        if(!Config.should_createjs) return;
        if(jsMethodContent==null||jsMethodContent.trim().equals("")) return;
        if(jsPath==null||jsPath.trim().equals("")) return;
        FilePathAssistant.getInstance().writeFileContentToDisk(jsPath,jsMethodContent,true);
    }
}
