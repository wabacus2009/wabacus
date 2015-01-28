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
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class DatePickerBox2 extends TextBox
{
    private String dateformat="y-mm-dd";

    public DatePickerBox2(String typename)
    {
        super(typename);
    }

    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        if(this.dateformat==null) this.dateformat="y-mm-dd";
        resultBuf.append(" dateformat=\""+this.dateformat.trim()+"\"");
        return resultBuf.toString();
    }

    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
       return null;
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
        String jspick=Tools.replaceAll(Config.webroot+"/webresources/component/datepicker/js/calendar.js","//","/");
        owner.getReportBean().getPageBean().addMyJavascriptFile(jspick,0);
        jspick=Tools.replaceAll(Config.webroot+"/webresources/component/datepicker/js/calendar-setup.js","//","/");
        owner.getReportBean().getPageBean().addMyJavascriptFile(jspick,0);
        jspick=Config.webroot+"/webresources/component/datepicker/js/";
        if(this.language==null||this.language.trim().equals("")||this.language.trim().equals(Consts_Private.LANGUAGE_ZH))
        {
            jspick=jspick+"calendar-zh.js";
        }else
        {
            jspick=jspick+"calendar-en.js";
        }
        jspick=Tools.replaceAll(jspick,"//","/");
        owner.getReportBean().getPageBean().addMyJavascriptFile(jspick,0);
        String csspick=Config.webroot+"/webresources/component/datepicker/css/calendar.css";
        csspick=Tools.replaceAll(csspick,"//","/");
        owner.getReportBean().getPageBean().addMyCss(csspick);
    }
   
    protected void processStylePropertyAfterMerged()
    {
        super.processStylePropertyAfterMerged();
        this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"onclick=\"showDatepickerBox2(this)\"",1);
    }
    
    protected void initDisplayMode()
    {
        this.displayOnClick=false;
    }
}
