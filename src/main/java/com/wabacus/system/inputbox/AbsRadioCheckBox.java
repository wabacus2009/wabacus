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
import java.util.Map;

import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Tools;

public abstract class AbsRadioCheckBox extends AbsSelectBox
{
    protected int inline_count;
    
    public AbsRadioCheckBox(String typename)
    {
        super(typename);
    }
    
    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        StringBuilder resultBuf=new StringBuilder();
        String realinputboxid=getInputBoxId(rrequest);
        String optionLabelTmp,optionValueTmp,selected;
        resultBuf.append(this.getBeforedescription(rrequest));
        resultBuf.append("<span id=\""+realinputboxid+"_group\">");
        if(!this.isDependsOtherInputbox())
        {
            List<Map<String,String>> lstOptionsResult=getLstOptionsFromCache(rrequest);
            if(lstOptionsResult!=null&&lstOptionsResult.size()>0)
            {
                if(isReadonly) style_property=addReadonlyToStyleProperty2(style_property);//如果是只读，则将只读属性添加到styleproperty中
                value=getInputBoxValue(rrequest,value);
                int count=0;
                for(Map<String,String> mOptionTmp:lstOptionsResult)
                {
                    optionLabelTmp=mOptionTmp.get("label");
                    optionValueTmp=mOptionTmp.get("value");
                    if(this.inline_count>0&&count>0&&count%this.inline_count==0)
                    {
                        resultBuf.append("<br>");
                    }
                    selected=isSelectedValueOfSelectBox(value,this.isBelongtoUpdatecolSrcCol?optionLabelTmp:optionValueTmp)?" checked ":"";
                    optionValueTmp=optionValueTmp==null?"":optionValueTmp.trim();
                    resultBuf.append("<input type=\""+this.getSelectboxType()+"\"  name=\""+realinputboxid+"\" id=\""+realinputboxid+"\"");
                    resultBuf.append(" label=\"").append(optionLabelTmp).append("\" value=\""+optionValueTmp+"\" ").append(selected);
                    if(style_property!=null) resultBuf.append(" ").append(style_property);
                    resultBuf.append(">").append(optionLabelTmp).append("</input> ");
                    count++;
                }
            }
        } 
        resultBuf.append("</span>");
        resultBuf.append(this.getAfterdescription(rrequest));
        return resultBuf.toString().trim();
    }
    
    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        if(this.inline_count>0) resultBuf.append(" inline_count=\"").append(this.inline_count).append("\"");
        if(!isBelongToEditable2ReportCol(true)&&this.isDependsOtherInputbox())
        {
            String realstyle=this.getStyleproperty(rrequest);
            if(realstyle!=null&&!realstyle.trim().equals(""))
            {
                resultBuf.append(" styleproperty=\""+Tools.jsParamEncode(realstyle)+"\"");
            }
        }
        return resultBuf.toString();
    }

    public String getChangeStyleObjOnEdit()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("var optBoxName=boxObj.getAttribute('name');");
        resultBuf.append("boxObj=boxObj.parentNode;");//改变父标签背景色
        resultBuf.append("while(boxObj!=null){");
        resultBuf.append("  if(boxObj.tagName=='SPAN'&&boxObj.getAttribute('id')==optBoxName+'_group') break;");
        resultBuf.append("  boxObj=boxObj.parentNode;");
        resultBuf.append("}");
        return resultBuf.toString();
    } 
    
    public String filledInContainer()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("var parentIds=boxMetadataObj.getAttribute('parentids');");
        resultBuf.append("if(parentIds!=null&&parentIds!=''){");
        resultBuf.append("  if(displayonclick==='true'){");
        resultBuf.append("      wx_reloadChildSelectBoxOptions(realinputboxid,false);");
        resultBuf.append("  }else{");//对于listform报表类型，先显示一个外部的<span/>，以便填充
        resultBuf.append("      setColDisplayValueToEditable2Td(parentTdObj,\"<span id='\"+realinputboxid+\"_group'></span>\");");
        resultBuf.append("  }");
        resultBuf.append("}else{");
        resultBuf
                .append("  boxstr=getChkRadioBoxOptionsDisplayString(boxMetadataObj,getSelectBoxOptionsFromMetadata(boxMetadataObj),realinputboxid,boxValue,'"
                        +this.getSelectboxType()+"');");
        resultBuf.append("  setColDisplayValueToEditable2Td(parentTdObj,\"<span id='\"+realinputboxid+\"_group'>\"+boxstr+\"</span>\");");
        resultBuf.append("}");
        return resultBuf.toString();
    }
    
    public String doPostFilledInContainer()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("isCommonFlag=false;");
        resultBuf.append("var boxObjsArr=document.getElementsByName(realinputboxid);");
        resultBuf.append("if(boxObjsArr==null||boxObjsArr.length==0) return;");
        //如果不是点击时填充，比如是添加记录时填充，则不调这个，以避免在添加记录时触发onblur时的客户端校验，点击时填充必须调这个，因为这样才可以触发onblur事件
        resultBuf.append("if(displayonclick=='true'){for(var j=0;j<boxObjsArr.length;j++){boxObjsArr[j].dataObj=initInputBoxData(boxObjsArr[j],parentTdObj);}boxObjsArr[0].focus();}");
        return resultBuf.toString();
    }
    
    protected String getBoxValueAndLabelScript()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("var selectboxname=boxObj.getAttribute('name');");
        resultBuf.append("if(selectboxname==null||selectboxname=='') return;");
        resultBuf.append("var optionObjs=document.getElementsByName(selectboxname);");
        return resultBuf.toString();
    }
    
    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
        List<String[]> lstOptionsResult=(List<String[]>)specificDataObj;
        StringBuffer resultBuf=new StringBuffer();
        dynstyleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,dynstyleproperty,1);
        if(isReadonly) dynstyleproperty=addReadonlyToStyleProperty1(dynstyleproperty);
        if(lstOptionsResult!=null&&lstOptionsResult.size()>0)
        {
            String optionLabelTmp="";
            String optionValueTmp="";
            String selected="";
            for(String[] items:lstOptionsResult)
            {
                optionLabelTmp=items[0];
                optionValueTmp=items[1];
                optionValueTmp=optionValueTmp==null?"":optionValueTmp.trim();
                if(isSelectedValueOfSelectBox(value,optionValueTmp)) selected=" checked ";
                resultBuf.append("<input type=\""+this.getSelectboxType()+"\" value=\""+optionValueTmp+"\" ").append(selected);
                if(dynstyleproperty!=null) resultBuf.append(" ").append(dynstyleproperty);
                resultBuf.append(">").append(optionLabelTmp).append("</input> ");
                selected="";
            }
        }
        return resultBuf.toString();
    }

    protected String addPropValueToFillStyleproperty()
    {
        return "";
    }
    
    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(eleInputboxBean);
        String inlinecount=eleInputboxBean.attributeValue("inlinecount");
        if(inlinecount!=null&&!inlinecount.trim().equals(""))
        {
            this.inline_count=Integer.parseInt(inlinecount.trim());
        }
    }

    protected String getDefaultStylePropertyForDisplayMode2()
    {
        return "onkeypress='return onKeyEvent(event);'";
    }

    protected void processStylePropertyAfterMerged()
    {
        super.processStylePropertyAfterMerged();
        this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"onclick=\"this.focus();\"",1);
        if(this.isDisplayOnClick())
        {
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,
                    "onfocus=\"setGroupBoxStopFlag(this)\" onblur=\"fillGroupBoxValue(this)\"",1);
        }
    }
    
    protected boolean isChangeDisplayValueWhenFillValue()
    {
        return false;
    }
    
    protected String getRefreshChildboxDataEventName()
    {
        return "onclick";
    }
    
    protected String fillParentValueMethodName()
    {
        return "onclick";
    }
}
