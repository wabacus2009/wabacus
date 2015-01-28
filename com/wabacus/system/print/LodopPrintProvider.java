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
import java.util.Map.Entry;

import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.print.LodopPrintProviderConfigBean;
import com.wabacus.config.print.PrintSubPageBean;
import com.wabacus.config.print.PrintTemplateElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class LodopPrintProvider extends AbsPrintProvider
{
    private LodopPrintProviderConfigBean lodopConfigBean;

    public LodopPrintProvider(ReportRequest rrequest,AbsPrintProviderConfigBean ppcbean)
    {
        super(rrequest,ppcbean);
        lodopConfigBean=(LodopPrintProviderConfigBean)ppcbean;
    }
    
    public void doPrint()
    {
        String pageid=this.lodopConfigBean.getOwner().getPageBean().getId();
        wresponse.println("<print-jobname-"+pageid+">"+this.lodopConfigBean.getJobname(rrequest)+"</print-jobname-"+pageid+">");
        super.doPrint();
    }

    protected void printSubPage(PrintSubPageBean pspagebeanTmp,int maxpagecnt)
    {
        if(pspagebeanTmp.getMPrintElements()==null||pspagebeanTmp.getMPrintElements().size()==0) return;
        if(pspagebeanTmp.isSplitPrintPage())
        {
            this.wresponse.println("<"+pspagebeanTmp.getPlaceholder()+"_pagecount>"+maxpagecnt+"</"+pspagebeanTmp.getPlaceholder()+"_pagecount>");
            for(int i=0;i<maxpagecnt;i++)
            {//依次打印每一页的记录
                setSubPagePageno(pspagebeanTmp,i);
                this.wresponse.print("<"+pspagebeanTmp.getPlaceholder()+"_"+i+">");
                for(Entry<String,PrintTemplateElementBean> entryTmp:pspagebeanTmp.getMPrintElements().entrySet())
                {
                    if(lodopConfigBean.isLodopCodePrintValue())
                    {//全部配置的是lodop代码，则一个<subpage/>中可能有多个动态元素，所以每个动态元素要根据自己的placeholder显示一个<placeholder/>标签
                        printElement(entryTmp.getKey(),entryTmp.getValue());
                    }else
                    {//如果打印内容全部是静态模板或动态模板，而不是lodop代码，则一个<subpage/>中只有一个动态元素，也就只有一个PrintTemplateElementBean对象，所以不用PrintTemplateElementBean的placeholder，而直接用页面的
                        printElement(null,entryTmp.getValue());//传入placeholder为null，说明不在里面显示一个<placeholder/>标签
                    }
                }
                this.wresponse.print("</"+pspagebeanTmp.getPlaceholder()+"_"+i+">");
            }
        }else
        {
            for(Entry<String,PrintTemplateElementBean> entryTmp:pspagebeanTmp.getMPrintElements().entrySet())
            {
                if(lodopConfigBean.isLodopCodePrintValue())
                {//是lodop代码，则一个<subpage/>中可能有多个动态元素，所以每个动态元素要根据自己的placeholder显示一个<placeholder/>标签
                    printElement(entryTmp.getKey(),entryTmp.getValue());
                }else
                {//如果打印内容全部是静态模板或动态模板，而不是lodop代码，则一个<subpage/>中只有一个动态元素，也就只有一个PrintTemplateElementBean对象，所以不用PrintTemplateElementBean的placeholder，而直接用页面的
                    printElement(pspagebeanTmp.getPlaceholder(),entryTmp.getValue());//传入pspagebeanTmp的placeholder生成<placeholder/>标签
                }
            }
        }
    }
    
    protected void printElement(String placeholder,PrintTemplateElementBean ptEleBean)
    {
        if(placeholder!=null) this.wresponse.print("<"+placeholder+">");
        if(ptEleBean.getType()==PrintTemplateElementBean.ELEMENT_TYPE_APPLICATION)
        {
            printApplicationElementValue(ptEleBean);
        }else if(ptEleBean.getType()==PrintTemplateElementBean.ELEMENT_TYPE_OTHER)
        {
            String value=(String)ptEleBean.getValueObj();
            if(WabacusAssistant.getInstance().isGetRequestContextValue(value))
            {
                this.wresponse.print(WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,value,""));
            }else
            {
                this.wresponse.print(value==null?"":value);
            }
        }else
        {
            super.printElement(placeholder,ptEleBean);
        }
        if(placeholder!=null) this.wresponse.print("</"+placeholder+">");
    }
    
    private void printApplicationElementValue(PrintTemplateElementBean ptElebean)
    {
        List<String> lstParts=(List<String>)ptElebean.getValueObj();
        if(lstParts==null||lstParts.size()==0) return;
        if(lstParts.size()==1)
        {//applicationid或者this，即打印某个应用整体
            printApplication(lstParts.get(0));
            return;
        }
        AbsReportType reportTypeObj=rrequest.getDisplayReportTypeObj(lstParts.get(0));
        String partname=lstParts.get(1);
        if(Consts.HEADER_PART.equals(partname))
        {
            printHeaderPart(reportTypeObj,ptElebean);
        }else if(Consts.SEARCH_PART.equals(partname))
        {
            printSearchBox(reportTypeObj,ptElebean);
        }else if(Consts.TITLE_PART.equals(partname))
        {
            printTitlePart(reportTypeObj,ptElebean);
        }else if(Consts.DATA_PART.equals(partname))
        {
            printDataPart(reportTypeObj,ptElebean);
        }else if(Consts.NAVIGATE_PART.equals(partname))
        {
            printNavigatePart(reportTypeObj,ptElebean);
        }else if(Consts.FOOTER_PART.equals(partname))
        {
            printFooterPart(reportTypeObj,ptElebean);
        }
    }

    protected void printTotalPageCount(int totalpagecount)
    {
        this.wresponse.println("<WX_PRINT_TOTAL_pagecount>"+totalpagecount+"</WX_PRINT_TOTAL_pagecount>");
    }
}
