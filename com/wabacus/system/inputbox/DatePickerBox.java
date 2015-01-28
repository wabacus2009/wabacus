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

import com.wabacus.config.Config;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class DatePickerBox extends TextBox
{
    private String dateformat;

    public DatePickerBox(String typename)
    {
        super(typename);
    }

    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(eleInputboxBean);
        this.setTypePromptBean(null);
        if(eleInputboxBean!=null)
        {
            String dateformat=eleInputboxBean.attributeValue("dateformat");
            if(dateformat!=null) this.dateformat=dateformat.trim();
            
        }
        String dynparams="";
        if(this.dateformat!=null&&!this.dateformat.trim().equals(""))
        {
            dynparams=dynparams+"dateFmt:'"+this.dateformat+"',";
        }
        
        if(this.language!=null&&!this.language.trim().equals(""))
        {
            if(this.language.equals(Consts_Private.LANGUAGE_ZH))
            {
                dynparams=dynparams+"lang:'zh-cn',";
            }else
            {
                dynparams=dynparams+"lang:'en',";
            }
        }
        this.inputboxparams=Tools.mergeJsonValue(dynparams,this.inputboxparams);
        this.inputboxparams=this.inputboxparams==null?"":this.inputboxparams.trim();
        if(!this.inputboxparams.trim().equals(""))
        {
            this.inputboxparams="{"+this.inputboxparams+"}";
        }
    }
    
    protected String getDefaultStylePropertyForDisplayMode2()
    {
        String resultStr="onkeypress='return onKeyEvent(event);'";
        if(this.hasDescription())
        {
            resultStr+=" class='cls-inputbox2' ";
        }else
        {
            resultStr+=" class='cls-inputbox2-full' ";
        }
        return resultStr;
    }
    
    public void doPostLoad()
    {
        super.doPostLoad();
        String jspick=Config.webroot+"/webresources/component/My97DatePicker/WdatePicker.js";
        jspick=Tools.replaceAll(jspick,"//","/");
        owner.getReportBean().getPageBean().addMyJavascriptFile(jspick,0);
    }
    
    protected void processStylePropertyAfterMerged()
    {
        super.processStylePropertyAfterMerged();
        this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"onclick=\"showMy97DatepickerBox(this)\"",1);
    }
    
    protected void initDisplayMode()
    {
        this.displayOnClick=false;
    }
}
