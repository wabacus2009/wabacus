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

public class SelectBox extends AbsSelectBox
{
    public SelectBox(String typename)
    {
        super(typename);
    }

    protected boolean isMultipleSelect()
    {
        return this.isMultiply;
    }

    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(this.getBeforedescription(rrequest));
        if(isReadonly) style_property=addReadonlyToStyleProperty2(style_property);
        String realinputboxid=getInputBoxId(rrequest);
        resultBuf.append("<select name='"+realinputboxid+"' id='"+realinputboxid+"'");
        if(style_property!=null) resultBuf.append(" ").append(style_property);
        resultBuf.append(">");
        if(!this.isDependsOtherInputbox())
        {
            value=getInputBoxValue(rrequest,value);
            List<Map<String,String>> lstOptionsResult=getLstOptionsFromCache(rrequest);
            if(lstOptionsResult!=null)
            {
                String optionLabelTmp, optionValueTmp, selected;
                for(Map<String,String> mItems:lstOptionsResult)
                {
                    optionLabelTmp=mItems.get("label");
                    optionValueTmp=mItems.get("value");
                    selected=isSelectedValueOfSelectBox(value,this.isBelongtoUpdatecolSrcCol?optionLabelTmp:optionValueTmp)?"selected":"";
                    optionValueTmp=optionValueTmp==null?"":optionValueTmp.trim();
                    resultBuf.append("<option value='"+optionValueTmp+"' "+selected+">"+optionLabelTmp+"</option>");
                }
            }
        }
        resultBuf.append("</select>");
        resultBuf.append(this.getAfterdescription(rrequest));
        return resultBuf.toString();
    }
    
    public String filledInContainer()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("boxstr=\"<select \";").append(getInputBoxCommonFilledProperties());
        resultBuf.append("boxstr+=\">\";");
        //显示下拉选项（依赖其它下拉框的子下拉框也有可能需要显示选项）
        resultBuf.append("var optionSpans=boxMetadataObj.getElementsByTagName(\"span\");");
        resultBuf.append("if(optionSpans!=null&&optionSpans.length>0){ ");
        resultBuf.append("var optionlabel=null;var optionvalue=null;");
        resultBuf.append("  for(var i=0,len=optionSpans.length;i<len;i++){");
        resultBuf.append("      optionlabel=optionSpans[i].getAttribute('label'); optionvalue=optionSpans[i].getAttribute('value');");
        resultBuf.append("      boxstr=boxstr+\"<option value='\"+optionvalue+\"'\";");
        resultBuf.append("      if(isSelectedValueForSelectedBox(boxValue,optionvalue,boxMetadataObj)) boxstr=boxstr+\" selected\";");
        resultBuf.append("      boxstr=boxstr+\">\"+optionlabel+\"</option>\";");
        resultBuf.append("  }");
        resultBuf.append("}");
        resultBuf.append("boxstr+=\"</select>\";");
        resultBuf.append("setColDisplayValueToEditable2Td(parentTdObj,boxstr);");
        resultBuf.append("if(displayonclick==='true'){");
        resultBuf.append("  var parentIds=boxMetadataObj.getAttribute('parentids');if(parentIds!=null&&parentIds!='') wx_reloadChildSelectBoxOptions(realinputboxid,false);");
        resultBuf.append("}");
        return resultBuf.toString();
    }

//        {//如果是点击时再填充
//            return super.getDefaultvalue(rrequest);
//        //下面处理显示时就直接填充的情况，此时如果没有配置默认值，则以第一个下拉选项做为默认值

    protected String getBoxValueAndLabelScript()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("if(boxObj.options!=null&&boxObj.options.length>0){");
        resultBuf.append("  var separator=boxMetadataObj.getAttribute('separator');");
        resultBuf.append("  if(separator==null||separator==''){");
        resultBuf.append("      selectboxvalue=boxObj.options[boxObj.options.selectedIndex].value;");
        resultBuf.append("      selectboxlabel=boxObj.options[boxObj.options.selectedIndex].text;");
        resultBuf.append("  }else{");//复选下拉框
        resultBuf.append("      for(var i=0,len=boxObj.options.length;i<len;i++){");
        resultBuf.append("          if(boxObj.options[i].selected){");
        resultBuf.append("              selectboxvalue+=boxObj.options[i].value+separator;");
        resultBuf.append("              selectboxlabel+=boxObj.options[i].text+separator;");
        resultBuf.append("          }");
        resultBuf.append("      }");
        resultBuf.append("      selectboxvalue=wx_rtrim(selectboxvalue,separator);selectboxlabel=wx_rtrim(selectboxlabel,separator);");
        resultBuf.append("  }");
        resultBuf.append("}");
        return resultBuf.toString();
    }

    public String createGetInputboxValueJs(boolean isGetLabel)
    {
        String contenttype=isGetLabel?"text":"value";
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("if(boxObj==null) return null;");
        resultBuf.append("if(boxObj.options.length==0) return null;");
        resultBuf.append("var separator=boxMetadataObj.getAttribute('separator');");
        resultBuf.append("if(separator==null||separator=='') return boxObj.options[boxObj.options.selectedIndex]."+contenttype+";");
        resultBuf.append("var resultVal='';");
        resultBuf.append("for(var i=0,len=boxObj.options.length;i<len;i++){");
        resultBuf.append("  if(boxObj.options[i].selected){resultVal=resultVal+boxObj.options[i]."+contenttype+"+separator;}");
        resultBuf.append("}");
        resultBuf.append("return wx_rtrim(resultVal,separator);");
        return resultBuf.toString();
    }
    
    public String createSetInputboxValueJs(boolean isSetLabel)
    {
        String contenttype=isSetLabel?"text":"value";
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("isCommonFlag=false;");
        resultBuf.append("if(boxObj==null||boxObj.options.length==0) return;");
        resultBuf.append("var separator=boxMetadataObj.getAttribute('separator');");
        resultBuf.append("if(separator!=null&&separator!=''){");
        resultBuf.append("  for(var j=0,len=boxObj.options.length;j<len;j++){");
        resultBuf.append("      if(boxObj.options[j].selected&&boxObj.options[j]."+contenttype+"==newValue) return;");//要设置的新值已选中，则不用设置
        resultBuf.append("  }");
        resultBuf.append("}else{");
        resultBuf.append("  var oldvalue=boxObj.options[boxObj.selectedIndex]."+contenttype+";");
        resultBuf.append("  if(oldvalue&&oldvalue==newValue) return;");
        resultBuf.append("}");
        resultBuf.append("var i=0;");
        resultBuf.append("for(len=boxObj.options.length;i<len;i=i+1){");
        resultBuf.append("  if(boxObj.options[i]."+contenttype+"==newValue){boxObj.options[i].selected=true;break;}");
        resultBuf.append("}");
        resultBuf.append("if(i!=boxObj.options.length&&boxObj.onchange){boxObj.onchange();}");
        return resultBuf.toString();
    }
    
    public String getSelectboxType()
    {
        return "selectbox";
    }
    
    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        String multiply=eleInputboxBean.attributeValue("multiply");
        this.isMultiply=multiply!=null&&multiply.toLowerCase().trim().equals("true");
        if(this.isMultiply)
        {
            this.separator=eleInputboxBean.attributeValue("separator");
            if(this.separator==null||this.separator.equals("")) this.separator=" ";
        }else
        {
            this.separator="";
        }
        super.loadInputBoxConfig(eleInputboxBean);
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
    
    protected String getRefreshChildboxDataEventName()
    {
        return "onchange";
    }

    protected void processStylePropertyAfterMerged()
    {
        super.processStylePropertyAfterMerged();
        if(this.isMultiply) this.styleproperty=this.styleproperty+" multiple ";
    }
    
    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
        List<String[]> lstOptionsResult=(List<String[]>)specificDataObj;
        StringBuffer resultBuf=new StringBuffer();
        dynstyleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,dynstyleproperty,1);
        if(isReadonly) dynstyleproperty=addReadonlyToStyleProperty1(dynstyleproperty);
        resultBuf.append("<select ").append(dynstyleproperty).append(">");
        if(lstOptionsResult!=null&&lstOptionsResult.size()>0)
        {
            String name_temp,value_temp,selected;
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
        return resultBuf.toString();
    }
}
