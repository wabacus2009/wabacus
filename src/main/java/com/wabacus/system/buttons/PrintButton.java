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
package com.wabacus.system.buttons;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Consts;

public class PrintButton extends WabacusButton
{
    private String printtype;

    private boolean isPdfPrint;
    
    public PrintButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    public String getPrinttype()
    {
        return printtype;
    }

    public void setPrinttype(String printtype)
    {
        this.printtype=printtype;
    }

    public String getButtonType()
    {
        return printtype;
    }

    public void setPdfPrint(boolean isPdfPrint)
    {
        this.isPdfPrint=isPdfPrint;
    }

    public String showButton(ReportRequest rrequest,String dynclickevent)
    {
        String clickevent=getPrintEvent();
        if(clickevent==null||clickevent.trim().equals("")) return "";
        return super.showButton(rrequest,clickevent);
    }

    public String showButton(ReportRequest rrequest,String dynclickevent,String button)
    {
        String clickevent=getPrintEvent();
        if(clickevent==null||clickevent.trim().equals("")) return "";
        return super.showButton(rrequest,clickevent,button);
    }

    public String showMenu(ReportRequest rrequest,String dynclickevent)
    {
        String clickevent=getPrintEvent();
        if(clickevent==null||clickevent.trim().equals("")) return "";
        return super.showMenu(rrequest,clickevent);
    }

    private String getPrintEvent()
    {
        StringBuffer resultBuf=new StringBuffer();
        if(!this.isPdfPrint)
        {
            if(this.ccbean.getPrintBean()==null) return "";
            resultBuf.append("printComponentsData('").append(this.ccbean.getPageBean().getId()).append("','").append(this.ccbean.getId()).append(
                    "','");
            resultBuf.append(this.ccbean.getPrintBean().getIncludeApplicationids()).append("','");
            resultBuf.append(this.printtype).append("')");
        }else
        {
            if(this.ccbean.getPdfPrintBean()==null) return "";
            String pdfdataexporturl=Config.showreport_onpdf_url;
            String token="?";
            if(pdfdataexporturl.indexOf("?")>0) token="&";
            pdfdataexporturl=pdfdataexporturl+token+"WX_IS_PDFPRINT_ACTION=true";//在URL后面加上WX_IS_PDFPRINT_ACTION=true表示不是PDF导出，而是PDF打印
            resultBuf.append("exportData('"+this.ccbean.getPageBean().getId()+"','"+this.ccbean.getId()+"','"
                    +this.ccbean.getPdfPrintBean().getIncludeApplicationids()+"','"+Config.showreport_onpage_url+"','"+pdfdataexporturl+"');");
        }
        return resultBuf.toString();
    }

    public void loadExtendConfig(XmlElementBean eleButtonBean)
    {
        super.loadExtendConfig(eleButtonBean);
        String printtype=eleButtonBean.attributeValue("type");
        if(printtype==null||printtype.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"上的按钮"+this.name+"失败，此按钮为打印按钮，必须配置其printtype属性，指定本按钮类型");
        }
        printtype=printtype.trim();
        if(!printtype.equals(Consts.PRINTTYPE_PRINT)&&!printtype.equals(Consts.PRINTTYPE_PRINTPREVIEW)
                &&!printtype.equals(Consts.PRINTTYPE_PRINTSETTING))
        {
            throw new WabacusConfigLoadingException("加载报表"+ccbean.getPath()+"上的按钮"+this.name+"失败，为此打印按钮配置的printtype："+printtype+"无效");
        }
        this.printtype=printtype;
    }
}
