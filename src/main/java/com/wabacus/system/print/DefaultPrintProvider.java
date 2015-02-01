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
package com.wabacus.system.print;

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.print.DefaultPrintProviderConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.util.Tools;

public class DefaultPrintProvider extends AbsPrintProvider
{
    public DefaultPrintProvider(ReportRequest rrequest,AbsPrintProviderConfigBean ppcbean)
    {
        super(rrequest,ppcbean);
    }

    public void doPrint()
    {
        List<String> lstCsses=ComponentAssistant.getInstance().initDisplayCss(rrequest);
        if(lstCsses==null) return;
        StringBuffer buf=new StringBuffer();
        for(String cssTmp:lstCsses)
        {
            buf.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\""+cssTmp+"\"/>");
        }
        buf.append("<style media=\"print\">.Noprint { DISPLAY: none }.PageNext{ PAGE-BREAK-AFTER: always }</style>");
        buf.append("<div align=\"center\">");//显示最外层的<div/>，以便页面居中显示
        buf.append("<div id=\"WX_PRINT_FUNBAR_ID\" class=\"Noprint\" align=\"right\"");
        String paperwidth=((DefaultPrintProviderConfigBean)this.ppcbean).getPaperwidth();
        if(paperwidth!=null&&!paperwidth.trim().equals(""))
        {
            buf.append(" style=\"width:").append(paperwidth).append("\"");
        }
        buf.append(">");
        List<String> lstPagesizes=((DefaultPrintProviderConfigBean)this.ppcbean).getLstPrintPagesizes();
        if(lstPagesizes!=null&&lstPagesizes.size()>1)
        {
            buf.append("<select style=\"vertical-align:bottom;\" onchange=\"var widthtmp=this.options[this.options.selectedIndex].value;document.getElementById('WX_PRINT_FUNBAR_ID').style.width=widthtmp;document.getElementById('WX_PRINT_CONTENT_ID').style.width=widthtmp;\">");
            for(String pagesizeTmp:lstPagesizes)
            {
                buf.append("<option value=\""+DefaultPrintProviderConfigBean.mPrintPagesize.get(pagesizeTmp+"_WIDTH")+"\">");
                buf.append(pagesizeTmp).append("</option>");
            }
            buf.append("</select>").append(WabacusAssistant.getInstance().getSpacingDisplayString(3));
        }
        String serverName=rrequest.getRequest().getServerName();
        String serverPort=String.valueOf(rrequest.getRequest().getServerPort());
        String printimg=Config.webroot+"/webresources/skin/"+rrequest.getPageskin()+"/images/print.gif";
        printimg="http://"+serverName+":"+serverPort+Tools.replaceAll("/"+printimg,"//","/");
        buf.append("<img style=\"vertical-align:bottom\" src=\""+printimg+"\" onclick=\"window.print()\" onmouseover=\"this.style.cursor='pointer'\"/>");
        buf.append("</div>");
        buf.append("<div id=\"WX_PRINT_CONTENT_ID\" ");//显示打印内容所在<div/>
        if(((DefaultPrintProviderConfigBean)this.ppcbean).getPaperstyleproperty()!=null)
        {
            buf.append(((DefaultPrintProviderConfigBean)this.ppcbean).getPaperstyleproperty());
        }
        buf.append(">&nbsp;");
        buf.append("<div style='width:96%'>");
        this.wresponse.println(buf.toString());
        super.doPrint();
        this.wresponse.println("</div>&nbsp;</div></div>");
    }

}

