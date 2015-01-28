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

import com.wabacus.config.xml.XmlElementBean;


public class CheckBox extends AbsRadioCheckBox
{
    public CheckBox(String typename)
    {
        super(typename);
    }
    
    protected boolean isMultipleSelect()
    {
        return true;
    }
    
    public String createGetInputboxValueJs(boolean isGetLabel)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("var chkObjs=document.getElementsByName(boxId);");
        resultBuf.append("if(chkObjs==null||chkObjs.length==0) return '';");
        resultBuf.append("var value=''; var separator=boxMetadataObj.getAttribute('separator');if(separator==null||separator=='') separator=' ';");
        resultBuf.append("for(i=0,len=chkObjs.length;i<len;i=i+1){");
        resultBuf.append("    if(chkObjs[i].checked){");
        if(!isGetLabel)
        {
            resultBuf.append("        value+=chkObjs[i].value+separator;");
        }else
        {
            resultBuf.append("        value+=chkObjs[i].getAttribute('label')+separator;");
        }
        resultBuf.append("    }");
        resultBuf.append("}");
        resultBuf.append("return wx_rtrim(value,separator);");
        return resultBuf.toString();
    }
    
    public String createSetInputboxValueJs(boolean isSetLabel)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("isCommonFlag=false;");
        resultBuf.append("var chkObjs=document.getElementsByName(boxId);");
        resultBuf.append("if(chkObjs==null||chkObjs.length==0) return;");
        resultBuf.append("for(var i=0,len=chkObjs.length;i<len;i=i+1){");
        resultBuf.append("  if(isSelectedValueForSelectedBox(newValue,chkObjs[i].");
        resultBuf.append(isSetLabel?"getAttribute('label')":"value");
        resultBuf.append(",boxMetadataObj)){chkObjs[i].checked=true;}");
        resultBuf.append("}");
        return resultBuf.toString();
    }
    
    protected String getBoxValueAndLabelScript()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.getBoxValueAndLabelScript());
        resultBuf.append("if(optionObjs!=null&&optionObjs.length>0){");
        resultBuf.append("  var separator=boxMetadataObj.getAttribute('separator');if(separator==null||separator=='') separator=' ';");
        resultBuf.append("  for(i=0,len=optionObjs.length;i<len;i=i+1){");
        resultBuf.append("      if(optionObjs[i].checked){selectboxlabel+=optionObjs[i].getAttribute('label')+separator;selectboxvalue+=optionObjs[i].value+separator;}");
        resultBuf.append("  }");
        resultBuf.append("  selectboxlabel=wx_rtrim(selectboxlabel,separator);");//去掉label结尾部分的separator
        resultBuf.append("  selectboxvalue=wx_rtrim(selectboxvalue,separator);");
        resultBuf.append("}");
        return resultBuf.toString();
    }
    
    public String getSelectboxType()
    {
        return "checkbox";
    }
    
    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        this.isMultiply=true;
        this.separator=eleInputboxBean.attributeValue("separator");
        if(this.separator==null||this.separator.equals("")) this.separator=" ";
        super.loadInputBoxConfig(eleInputboxBean);
    }
}
