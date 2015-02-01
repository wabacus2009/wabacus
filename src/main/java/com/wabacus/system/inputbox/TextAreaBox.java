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

import com.wabacus.system.ReportRequest;
import com.wabacus.util.Tools;

public class TextAreaBox extends AbsInputBox
{
    public TextAreaBox(String typename)
    {
        super(typename);
    }
    
    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        if(isReadonly) style_property=addReadonlyToStyleProperty1(style_property);
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(this.getBeforedescription(rrequest));
        String inputboxid=getInputBoxId(rrequest);
        resultBuf.append("<textarea typename='"+typename+"' name='"+inputboxid+"'  id='"+inputboxid+"' ");
        if(style_property!=null) resultBuf.append(" ").append(style_property);
        resultBuf.append(">").append(getInputBoxValue(rrequest,value)).append("</textarea>");
        resultBuf.append(this.getAfterdescription(rrequest));
        return resultBuf.toString();
    }

    public String filledInContainer()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("  boxstr=\"<textarea  \";");
        resultBuf.append(getInputBoxCommonFilledProperties());
        resultBuf.append("  boxstr=boxstr+\">\"+boxValue+\"</textarea>\";");
        resultBuf.append("if(displayonclick=='true'){");
        resultBuf.append("  var textAreaboxContainer=document.getElementById('WX_TEXTAREA_BOX_CONTAINER');");
        resultBuf.append("  if(textAreaboxContainer==null){");
        resultBuf.append("      textAreaboxContainer=document.createElement('span');");
        resultBuf.append("      textAreaboxContainer.setAttribute('id','WX_TEXTAREA_BOX_CONTAINER');");
        resultBuf.append("      document.body.appendChild(textAreaboxContainer);");
        resultBuf.append("  }");
        resultBuf.append("  textAreaboxContainer.innerHTML=boxstr;");
        resultBuf.append("  textAreaboxContainer.style.display='';");
        resultBuf.append("  var textAreabox=document.getElementById(realinputboxid);");
        resultBuf.append("  setTextAreaBoxPosition(textAreabox,parentTdObj);");
        resultBuf.append("  textAreabox.focus();");
        resultBuf.append("}else{");//显示时即填充输入框
        
        resultBuf.append("setColDisplayValueToEditable2Td(parentTdObj,boxstr);");
        resultBuf.append("}");
        return resultBuf.toString();
    }

    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
       return null;
    }

    public String fillBoxValueToParentElement()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.fillBoxValueToParentElement());
        resultBuf.append("boxObj.parentNode.style.display='none';boxObj.style.display='none';");
        return resultBuf.toString();
    }
    
    protected String getDefaultStylePropertyForDisplayMode2()
    {
        return "class='cls-inputbox-textareabox2'";
    }    
}
