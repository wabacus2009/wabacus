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

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.EditableListFormReportType;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.util.Tools;

public class Combox extends AbsSelectBox
{
    private boolean isAutoComplete;
    
    private String autocompleteparams;
    
    private String onGetNonExistValueByLabelMethod;
    
    private String onGetNonExistLabelByValueMethod;
    
    public Combox(String typename)
    {
        super(typename);
    }

    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(this.getBeforedescription(rrequest));//显示描述信息
        if(isReadonly) style_property=addReadonlyToStyleProperty2(style_property);
        String realinputboxid=getInputBoxId(rrequest);
        resultBuf.append("<select name='"+realinputboxid+"' id='"+realinputboxid+"'");
        if(style_property!=null) resultBuf.append(" ").append(style_property);
        resultBuf.append(">");
        resultBuf.append("</select>");
        resultBuf.append(this.getAfterdescription(rrequest));
        rrequest.getWResponse().addDisplayInputbox(this.owner.getOwnerId(),realinputboxid,this.mParentIds==null?null:this.mParentIds.keySet());
        return resultBuf.toString();
    }
    
    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        if(!Tools.isEmpty(this.onGetNonExistValueByLabelMethod))
        {
            resultBuf.append(" onGetNonExistValueByLabelMethod=\"{method:"+Tools.jsParamEncode(this.onGetNonExistValueByLabelMethod)+"}\"");
        }
        if(!Tools.isEmpty(this.onGetNonExistLabelByValueMethod))
        {
            resultBuf.append(" onGetNonExistLabelByValueMethod=\"{method:"+Tools.jsParamEncode(this.onGetNonExistLabelByValueMethod)+"}\"");
        }
        if(this.isAutoComplete)
        {
            resultBuf.append(" autocomplete=\"").append(this.isAutoComplete).append("\"");
            if(!Tools.isEmpty(this.autocompleteparams))
            {
                resultBuf.append(" autocompleteParams=\"").append(this.autocompleteparams).append("\"");
            }
        }
        return resultBuf.toString();
    }

    public String filledInContainer()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("boxstr=\"<select \";").append(getInputBoxCommonFilledProperties());
        resultBuf.append("boxstr+=\">\";");
        resultBuf.append("boxstr+=\"</select>\";");
        resultBuf.append("setColDisplayValueToEditable2Td(parentTdObj,boxstr);");
        resultBuf.append("showComboxAddOptionsById(realinputboxid);");
        resultBuf.append("if(displayonclick==='true'){");
        resultBuf.append("  var parentIds=boxMetadataObj.getAttribute('parentids');");
        resultBuf.append("  if(parentIds!=null&&parentIds!=''){wx_reloadChildSelectBoxOptions(realinputboxid,false);}");//当前选择框依赖其它输入框
        resultBuf.append("}");
        return resultBuf.toString();
    
    }
    
    protected String getBoxValueAndLabelScript()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("var dataObjTmp=getComboxLabelValue(boxObj);");
        resultBuf.append("if(dataObjTmp!=null){");
        resultBuf.append("  selectboxlabel=dataObjTmp.label==null?'':dataObjTmp.label;");
        resultBuf.append("  selectboxvalue=dataObjTmp.value==null?'':dataObjTmp.value;");
        resultBuf.append("}");
        return resultBuf.toString();
    }

    public String createGetInputboxValueJs(boolean isGetLabel)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("if(boxObj==null) return null;");
        resultBuf.append("var valObjTmp=getComboxLabelValue(boxObj);");
        resultBuf.append("if(valObjTmp==null) return null;");
        if(isGetLabel)
        {
            resultBuf.append("return valObjTmp.label;");
        }else
        {
            resultBuf.append("return valObjTmp.value;");
        }
        return resultBuf.toString();
    }
    
    public String createSetInputboxValueJs(boolean isSetLabel)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("isCommonFlag=false;");
        resultBuf.append("if(boxObj==null) return;");
        if(isSetLabel)
        {
            resultBuf.append("setComboxLabel(boxObj,newValue);");
        }else
        {
            resultBuf.append("setComboxValue(boxObj,newValue,newValue);");
        }
        return resultBuf.toString();
    }
    
    public String getChangeStyleObjOnEdit()
    {
       return "boxObj=getTextBoxObjOfCombox(boxObj);";
    } 
    
    public String getSelectboxType()
    {
        return "combox";
    }

    protected boolean isMultipleSelect()
    {
        return false;
    }

    protected String getDefaultStylePropertyForDisplayMode2()
    {
        String resultStr="onkeypress='return onKeyEvent(event);'";
        if(this.hasDescription())
        {
            resultStr+=" class='cls-inputbox2-selectbox' ";
        }else
        {
            resultStr+=" class='cls-inputbox2-selectbox-full' ";
        }
        return resultStr;
    }

    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(eleInputboxBean);
        String autocomplete=eleInputboxBean.attributeValue("autocomplete");
        autocomplete=autocomplete==null?"":autocomplete.trim();
        this.isAutoComplete=!"false".equalsIgnoreCase(autocomplete);
        if(this.isAutoComplete)
        {
            String autocompleteParams=eleInputboxBean.attributeValue("autocompleteparams");
            if(!Tools.isEmpty(autocompleteParams))
            {
                this.autocompleteparams=Tools.jsParamEncode(autocompleteParams.trim());
                if(!this.autocompleteparams.startsWith("{")||!this.autocompleteparams.endsWith("}"))
                {
                    this.autocompleteparams="{"+this.autocompleteparams+"}";
                }
            }
        }
        String ongetnonexistvaluebylabel=eleInputboxBean.attributeValue("ongetnonexistvaluebylabel");
        if(ongetnonexistvaluebylabel!=null) this.onGetNonExistValueByLabelMethod=ongetnonexistvaluebylabel.trim();
        String  ongetnonexistlabelbyvalue=eleInputboxBean.attributeValue("ongetnonexistlabelbyvalue");
        if(ongetnonexistlabelbyvalue!=null) this.onGetNonExistLabelByValueMethod=ongetnonexistlabelbyvalue.trim();
        addJsFile("jquery.autocomplete.js");
        addJsFile("jquery.bgiframe.js");
        addJsFile("jquery.selectbox.js");
        String cssFile=Config.webroot+"/webresources/component/combox/jquery.autocomplete.css";
        cssFile=Tools.replaceAll(cssFile,"//","/");
        owner.getReportBean().getPageBean().addMyCss(cssFile);
    }

    private void addJsFile(String jsfilename)
    {
        String jsFile=Config.webroot+"/webresources/component/combox/"+jsfilename;
        jsFile=Tools.replaceAll(jsFile,"//","/");
        owner.getReportBean().getPageBean().addMyJavascriptFile(jsFile,0);
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        if((owner instanceof EditableReportColBean)
                &&(Config.getInstance().getReportType(this.owner.getReportBean().getType()) instanceof EditableListFormReportType))
        {
            this.owner.getReportBean().setCellresize(0);
        }
    }
    
    protected boolean isChangeDisplayValueWhenFillValue()
    {
        return false;
    }
    
    protected void processStylePropertyAfterMerged()
    {
        super.processStylePropertyAfterMerged();
        this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"onchange=\"setSelectBoxLabelToTextBoxOnChange(this)\"",1);
        if(this.isDisplayOnClick())
        {
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,
                    "onfocus=\"setGroupBoxStopFlag(getSelectBoxObjOfCombox(this))\" onblur=\"fillGroupBoxValue(getSelectBoxObjOfCombox(this))\"",1);
        }
    }

    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
        List<String[]> lstOptionsResult=(List<String[]>)specificDataObj;
        StringBuilder resultBuf=new StringBuilder();
        dynstyleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,dynstyleproperty,1);
        if(isReadonly) dynstyleproperty=addReadonlyToStyleProperty1(dynstyleproperty);//如果是只读，则将只读属性添加到styleproperty中
        String boxid=Tools.getPropertyValueByName("id",dynstyleproperty,false);
        if(Tools.isEmpty(boxid)) boxid=Tools.getRandomString(10);
        resultBuf.append("<select ").append(dynstyleproperty);
        resultBuf.append(" id=\""+boxid+"\">");
        if(lstOptionsResult!=null&&lstOptionsResult.size()>0)
        {
            String name_temp, value_temp, selected;
            for(String[] items:lstOptionsResult)
            {
                name_temp=items[0];
                value_temp=items[1];
                value_temp=value_temp==null?"":value_temp.trim();
                selected=value_temp.equals(value)?"selected":"";
                resultBuf.append("<option value='"+value_temp+"' "+selected+">"+name_temp+"</option>");
            }
        }
        resultBuf.append("</select>");
        rrequest.getWResponse().addOnloadMethod("showComboxOnload","{id:'"+boxid+"',selectedvalue:'"+value+"'}",true);
        return resultBuf.toString();
    }
}

