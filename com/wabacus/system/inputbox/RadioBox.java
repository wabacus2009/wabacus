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

public class RadioBox extends AbsRadioCheckBox
{
    public RadioBox(String typename)
    {
        super(typename);
    }
    
    protected boolean isMultipleSelect()
    {
        return false;
    }

    public String createGetInputboxValueJs(boolean isGetLabel)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("var radioObjs=document.getElementsByName(boxId);");
        resultBuf.append("if(radioObjs==null||radioObjs.length==0) return null;");
        resultBuf.append("for(i=0;i<radioObjs.length;i=i+1){");
        resultBuf.append("  if(radioObjs[i].checked){");
        if(!isGetLabel)
        {
            resultBuf.append("   return radioObjs[i].value;");
        }else
        {
            resultBuf.append("   return radioObjs[i].getAttribute('label');");
        }
        resultBuf.append("   }");
        resultBuf.append("}");
        resultBuf.append("  return null;");
        return resultBuf.toString();
    }
    
    public String createSetInputboxValueJs(boolean isSetLabel)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("isCommonFlag=false;");
        resultBuf.append("var radioObjs=document.getElementsByName(boxId);");
        resultBuf.append("if(radioObjs!=null&&radioObjs.length>0){");
        resultBuf.append("  for(var i=0,len=radioObjs.length;i<len;i=i+1){");
        resultBuf.append("      if(radioObjs[i]."+(isSetLabel?"getAttribute('label')":"value")+"==newValue){radioObjs[i].checked=true;break;}");
        resultBuf.append("  }");
        resultBuf.append("}");
        return resultBuf.toString();
    }
    
    protected String getBoxValueAndLabelScript()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.getBoxValueAndLabelScript());
        resultBuf.append("if(optionObjs!=null&&optionObjs.length>0){");
        resultBuf.append("  for(i=0,len=optionObjs.length;i<len;i=i+1){");
        resultBuf.append("      if(optionObjs[i].checked){selectboxlabel=optionObjs[i].getAttribute('label');selectboxvalue=optionObjs[i].value; break;}");
        resultBuf.append("  }");
        resultBuf.append("}");
        return resultBuf.toString();
    }
    
    public String getSelectboxType()
    {
        return "radio";
    }
    
    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(eleInputboxBean);
        this.isMultiply=false;
        this.separator=null;
    }
}
