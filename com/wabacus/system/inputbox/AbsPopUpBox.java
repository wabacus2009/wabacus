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
package com.wabacus.system.inputbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.EditableListReportType2;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.util.Tools;

public abstract class AbsPopUpBox extends AbsInputBox
{
    protected String poppageurl;

    protected Map<String,String> mDynParamParts;

    private List<String> lstParamNamesInUrl;
    
    protected String popupparams;

    protected String initsize;//初始大小，可配置值包括min/max/normal，分别表示最大化、最小化、正常窗口大小（即上面pagewidth/pageheight配置的大小）

    protected String beforepopup;

    public AbsPopUpBox(String typename)
    {
        super(typename);
    }

    public List<String> getLstParamNamesInUrl()
    {
        return lstParamNamesInUrl;
    }

    public String getPopupparams()
    {
        return popupparams;
    }

    public void setPopupparams(String popupparams)
    {
        this.popupparams=popupparams;
    }

    protected String getDefaultStylePropertyForDisplayMode2()
    {
        String resultStr="onfocus='this.select();' onkeypress='return onKeyEvent(event);' onmouseover=\"this.style.cursor='pointer';\" readonly";
        if(this.hasDescription())
        {
            resultStr+=" class='cls-inputbox2' ";
        }else
        {
            resultStr+=" class='cls-inputbox2-full' ";
        }
        return resultStr;
    }

    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        resultBuf.append(" paramsOfGetPageUrl=\"").append(getPopupUrlJsonString(rrequest)).append("\"");
        return resultBuf.toString();
    }
    
    private String getPopupUrlJsonString(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("{pageid:\"").append(owner.getReportBean().getPageBean().getId()).append("\",");
        resultBuf.append("reportid:\"").append(owner.getReportBean().getId()).append("\",");
        String popupageurlTmp=this.poppageurl;
        StringBuilder paramsBuf=new StringBuilder();
        boolean isReplaceColValueServerside=isReplaceColValueServerside();
        if(this.mDynParamParts!=null&&this.mDynParamParts.size()>0)
        {
            AbsReportDataPojo dataObj=isReplaceColValueServerside?rrequest.getReportDataObj(owner.getReportBean().getId(),0):null;
            String placeholderTmp, valueTmp;
            for(Entry<String,String> entryTmp:this.mDynParamParts.entrySet())
            {
                placeholderTmp=entryTmp.getKey();
                valueTmp=entryTmp.getValue();
                if(Tools.isDefineKey("@",valueTmp))
                {
                    valueTmp=Tools.getRealKeyByDefine("@",valueTmp);
                    if(isReplaceColValueServerside)
                    {
                        if(dataObj==null)
                        {//没有数据对象，则所有动态参数都替换成空字符串
                            valueTmp="";
                        }else
                        {
                            if(valueTmp.endsWith("__old")) valueTmp=valueTmp.substring(0,valueTmp.length()-"__old".length());
                            valueTmp=dataObj.getColStringValue(valueTmp);
                        }
                    }else
                    {
                        paramsBuf.append(valueTmp+":\""+placeholderTmp+"\","); 
                        continue;
                    }
                }else if(Tools.isDefineKey("url",valueTmp)||Tools.isDefineKey("request",valueTmp)||Tools.isDefineKey("rrequest",valueTmp)
                        ||Tools.isDefineKey("session",valueTmp))
                {
                    valueTmp=WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,valueTmp,"");
                }
                if(valueTmp==null) valueTmp="";
                popupageurlTmp=Tools.replaceAll(popupageurlTmp,placeholderTmp,valueTmp);
            }
        }
        resultBuf.append("popupPageUrl:\"").append(popupageurlTmp).append("\",");
        if(paramsBuf.length()>0&&paramsBuf.charAt(paramsBuf.length()-1)==',')
        {
            paramsBuf.deleteCharAt(paramsBuf.length()-1);
        }
        if(paramsBuf.length()>0) resultBuf.append(" popupPageUrlParams:{"+paramsBuf.toString()+"},");
        resultBuf.append("popupparams:\"").append(this.popupparams).append("\",");
        if(!Tools.isEmpty(this.beforepopup)) resultBuf.append("beforePopupMethod:").append(this.beforepopup);
        if(resultBuf.length()>0&&resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
        resultBuf.append("}");
        return Tools.jsParamEncode(resultBuf.toString());
    }

    private boolean isReplaceColValueServerside()
    {
        AbsReportType reportTypeObj=Config.getInstance().getReportType(this.owner.getReportBean().getType());
        if((this.owner instanceof ConditionBean)&&reportTypeObj instanceof AbsListReportType)
        {
            return true;
        }
        if(reportTypeObj instanceof IEditableReportType) return false;
        return true;
    }
    
    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(eleInputboxBean);
        String popupparams=eleInputboxBean.attributeValue("popupparams");
        if(popupparams!=null) this.popupparams=popupparams.trim();
        String initsize=eleInputboxBean.attributeValue("initsize");
        if(initsize!=null) this.initsize=initsize.trim().toLowerCase();
        String beforepopup=eleInputboxBean.attributeValue("beforepopup");
        if(beforepopup!=null) this.beforepopup=beforepopup.trim();
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        parsePopupPageUrl();
        popupparams=WabacusAssistant.getInstance().addDefaultPopupParams(popupparams,this.initsize,getDefaultWidth(),getDefaultHeight(),
                "closePopUpPageEvent");
    }
    
    protected abstract void parsePopupPageUrl();
    
    protected String parseDynParamsInUrl(String urlparams)
    {
        if(Tools.isEmpty(urlparams)) return null;
        this.mDynParamParts=new HashMap<String,String>();
        this.lstParamNamesInUrl=new ArrayList<String>();
        List<String> lstParams=Tools.parseStringToList(urlparams,"&",false);
        DisplayBean dbean=this.owner.getReportBean().getDbean();
        StringBuilder paramsBuf=new StringBuilder();
        String paramNameTmp, paramValueTmp,placeholderTmp;
        int idxTmp;
        ColBean cbTmp;
        String placeholderPrex="[PLACE_HOLDER_";
        int index=0;
        for(String paramTmp:lstParams)
        {
            if(Tools.isEmpty(paramTmp)) continue;
            idxTmp=paramTmp.indexOf("=");
            if(idxTmp<=0) continue;
            paramNameTmp=paramTmp.substring(0,idxTmp).trim();
            paramValueTmp=paramTmp.substring(idxTmp+1).trim();
            if(!this.lstParamNamesInUrl.contains(paramNameTmp)) this.lstParamNamesInUrl.add(paramNameTmp);
            paramsBuf.append(paramNameTmp+"=");
            if(Tools.isDefineKey("@",paramValueTmp)||Tools.isDefineKey("url",paramValueTmp)||Tools.isDefineKey("request",paramValueTmp)
                    ||Tools.isDefineKey("rrequest",paramValueTmp)||Tools.isDefineKey("session",paramValueTmp))
            {
                placeholderTmp=placeholderPrex+index+"]";
                this.mDynParamParts.put(placeholderTmp,paramValueTmp);
                paramsBuf.append(placeholderTmp);
                if(Tools.isDefineKey("@",paramValueTmp))
                {
                    paramValueTmp=Tools.getRealKeyByDefine("@",paramValueTmp);
                    if(paramValueTmp.endsWith("__old")) paramValueTmp=paramValueTmp.substring(0,paramValueTmp.length()-"__old".length());
                    cbTmp=dbean.getColBeanByColProperty(paramValueTmp);
                    if(cbTmp==null||cbTmp.isControlCol()||cbTmp.isSequenceCol()||cbTmp.isNonValueCol())
                    {
                        throw new WabacusConfigLoadingException("加载报表"+this.owner.getReportBean().getPath()+"的输入框"+this.owner.getInputBoxId()
                                +"失败，在弹出窗口URL的参数中指定的column为"+paramValueTmp+"的列不存在或不是有效数据列");
                    }
                    cbTmp.setDisplayNameValueProperty(true);
                }else if(Tools.isDefineKey("url",paramValueTmp))
                {//从查询条件中取数据的动态参数
                    this.owner.getReportBean().addParamNameFromURL(Tools.getRealKeyByDefine("url",paramValueTmp));
                }
                index++;
            }else
            {
                paramsBuf.append(paramValueTmp);
            }
            paramsBuf.append("&");
        }
        if(paramsBuf.length()>0&&paramsBuf.charAt(paramsBuf.length()-1)=='&') paramsBuf.deleteCharAt(paramsBuf.length()-1);
        return paramsBuf.toString();
    }
    
    protected void initDisplayMode()
    {
        this.displayOnClick=false;
    }

    protected void processStylePropertyAfterMerged()
    {
        super.processStylePropertyAfterMerged();
        if(this.styleproperty.toLowerCase().indexOf("readonly")<0) this.styleproperty=this.styleproperty+" readonly ";
    }

    public String createSelectOkFunction(String realinputboxid,boolean isAutoincludejs)
    {
        boolean isConditionBox=false;
        String paramname=null;
        if(this.getOwner() instanceof ConditionBean)
        {
            isConditionBox=true;
            paramname=((ConditionBean)this.getOwner()).getName();
        }else
        {
            isConditionBox=false;
            paramname=EditableReportAssistant.getInstance().getColParamName((ColBean)((EditableReportColBean)this.getOwner()).getOwner());
        }
        StringBuilder resultBuf=new StringBuilder();
        String parentWindowName, closeMeCodeString;
        if(Config.getInstance().getSystemConfigValue("prompt-dialog-type","artdialog").equals("artdialog"))
        {
            if(isAutoincludejs)
            {
                resultBuf.append("<script type=\"text/javascript\"  src=\""+Config.webroot
                        +"webresources/component/artDialog/artDialog.js\"></script>");
                resultBuf.append("<script type=\"text/javascript\"  src=\""+Config.webroot
                        +"webresources/component/artDialog/plugins/iframeTools.js\"></script>");
            }
            parentWindowName="artDialog.open.origin";
            closeMeCodeString="art.dialog.close();";
        }else
        {
            parentWindowName="parent";
            closeMeCodeString="parent.closePopupWin();";
        }
        resultBuf.append("<script language=\"javascript\">");
        resultBuf.append("function selectOK(value,name,label,closeme){");
        resultBuf.append("if(name==null||name==''||name=='"+paramname+"'){");
        resultBuf.append(parentWindowName+".setPopUpBoxValueToParent(value,'").append(realinputboxid).append("');");
        resultBuf.append("}else{");//指定了要设置的参数名
        String pageid=this.owner.getReportBean().getPageBean().getId();
        String reportid=this.owner.getReportBean().getId();
        if(isConditionBox)
        {
            resultBuf.append(parentWindowName+".setInputboxValueForCondition('"+pageid+"','"+reportid+"',name,value));");
        }else
        {
            resultBuf.append("var newvalues=\"{\"+name+\":\\\"\"+value+\"\\\"\";");
            resultBuf.append("if(label!=null){newvalues+=\",\"+name+\"$label:\\\"\"+label+\"\\\"\";}");
            resultBuf.append("newvalues=newvalues+\"}\";");
            AbsReportType reportTypeObj=Config.getInstance().getReportType(this.owner.getReportBean().getType());
            if(reportTypeObj instanceof EditableListReportType2)
            {
                resultBuf.append("var srcboxObj="+parentWindowName+".document.getElementById('"+realinputboxid+"');");//取到弹出窗口对应的源输入框对象，以便下面设置其它列的值时，可以取到其<tr/>对象
                resultBuf.append(parentWindowName+".setEditableListReportColValueInRow(\""+pageid+"\",\""+reportid+"\","+parentWindowName
                        +".getParentElementObj(srcboxObj,'TR'),"+parentWindowName+".getObjectByJsonString(newvalues));");
            }else
            {
                resultBuf.append(parentWindowName+".setEditableReportColValue(\""+pageid+"\",\""+reportid+"\","+parentWindowName
                        +".getObjectByJsonString(newvalues),null);");
            }
        }
        resultBuf.append("}");
        resultBuf.append("if(closeme!==false) "+closeMeCodeString+"}");
        resultBuf.append("</script>");
        return resultBuf.toString();
    }
    
    protected abstract String getDefaultWidth();

    protected abstract String getDefaultHeight();
    
    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
        return null;
    }
}
