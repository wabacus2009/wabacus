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

import com.wabacus.config.component.application.IApplicationConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.print.PrintSubPageBean;
import com.wabacus.config.print.PrintTemplateElementBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.WabacusResponse;
import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.AbsApplicationType;
import com.wabacus.system.component.application.report.abstractreport.AbsDetailReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Tools;

public abstract class AbsPrintProvider
{
    protected ReportRequest rrequest;

    protected WabacusResponse wresponse;

    protected AbsPrintProviderConfigBean ppcbean;

    public AbsPrintProvider(ReportRequest rrequest,AbsPrintProviderConfigBean ppcbean)
    {
        this.rrequest=rrequest;
        this.wresponse=rrequest.getWResponse();
        this.ppcbean=ppcbean;
    }
    
    public void doPrint()
    {
//        {//如果本次打印有打印报表，则依次记录各报表打印的页大小
//                {//是报表
//                    rrequest.getCdb(appidTmp).setPrintPagesize(this.ppcbean.getPrintPageSize(appidTmp));
        CacheDataBean cdbTmp;
        int totalpagecnt=0;
        for(PrintSubPageBean pspagebeanTmp:this.ppcbean.getLstPrintPageBeans())
        {
            int maxpagecnt=0;//存放当前子页的实际页数
            if(pspagebeanTmp.isSplitPrintPage())
            {
                int recordcntTmp, pagecntTmp;
                AbsReportType typeObj;
                for(String reportidTmp:pspagebeanTmp.getLstIncludeSplitPrintReportIds())
                {
                    typeObj=rrequest.getDisplayReportTypeObj(reportidTmp);
                    if(typeObj.getLstReportData()==null||typeObj.getLstReportData().size()==0) continue;
                    cdbTmp=rrequest.getCdb(reportidTmp);
                    recordcntTmp=typeObj.getLstReportData().size();
                    cdbTmp.setPrintRecordcount(recordcntTmp);
                    pagecntTmp=recordcntTmp/cdbTmp.getPrintPagesize();
                    if(recordcntTmp%cdbTmp.getPrintPagesize()>0) pagecntTmp++;
                    cdbTmp.setPrintPagecount(pagecntTmp);
                    if(pagecntTmp>maxpagecnt) maxpagecnt=pagecntTmp;
                }
                if(maxpagecnt<pspagebeanTmp.getMinpagecount()) maxpagecnt=pspagebeanTmp.getMinpagecount();
                if(pspagebeanTmp.getMaxpagecount()>0&&maxpagecnt>pspagebeanTmp.getMaxpagecount()) maxpagecnt=pspagebeanTmp.getMaxpagecount();//大于本页面配置的最大页数
                if(maxpagecnt==0) continue;
                if(pspagebeanTmp.isMergeUp()&&totalpagecnt>0)
                {
                    totalpagecnt+=maxpagecnt-1;
                }else
                {
                    totalpagecnt+=maxpagecnt;
                }
            }else
            {
                if(!pspagebeanTmp.isMergeUp()||totalpagecnt==0)   totalpagecnt++;
                maxpagecnt=1;
            }
            printSubPage(pspagebeanTmp,maxpagecnt);
        }
        printTotalPageCount(totalpagecnt);
    }
    
    protected void printSubPage(PrintSubPageBean pspagebeanTmp,int maxpagecnt)
    {
        for(int i=0;i<maxpagecnt;i++)
        {
            setSubPagePageno(pspagebeanTmp,i);
            for(Entry<String,PrintTemplateElementBean> entryTmp:pspagebeanTmp.getMPrintElements().entrySet())
            {//依次打印每一页中各动态元素的内容
                printElement(entryTmp.getKey(),entryTmp.getValue());
            }
        }
    }
    
    protected void setSubPagePageno(PrintSubPageBean pspagebean,int pageno)
    {
        if(pspagebean.getLstIncludeSplitPrintReportIds()==null) return;
        for(String reportidTmp:pspagebean.getLstIncludeSplitPrintReportIds())
        {
            rrequest.getCdb(reportidTmp).setPrintPageno(pageno);
        }
    }
    
    protected void printElement(String placeholder,PrintTemplateElementBean ptEleBean)
    {
        if(ptEleBean.getType()==PrintTemplateElementBean.ELEMENT_TYPE_APPLICATIONID)
        {//此<print/>没有配置打印内容，因此依次打印其include属性指定的应用
            printApplication((String)ptEleBean.getValueObj());
        }else
        {
            AbsReportType reportTypeObj=null;
            if(this.ppcbean.getOwner() instanceof ReportBean)
            {
                reportTypeObj=rrequest.getDisplayReportTypeObj(this.ppcbean.getOwner().getId());
            }
            if(ptEleBean.getType()==PrintTemplateElementBean.ELEMENT_TYPE_STATICTPL)
            {
                TemplateBean tplbean=(TemplateBean)ptEleBean.getValueObj();
                tplbean.printDisplayValue(rrequest,reportTypeObj);
            }else if(ptEleBean.getType()==PrintTemplateElementBean.ELEMENT_TYPE_DYNTPL)
            {
                String jspfile=(String)ptEleBean.getValueObj();
                WabacusAssistant.getInstance().includeDynTpl(rrequest,reportTypeObj,jspfile);
            }
        }
    }

    protected void printApplication(String appid)
    {
        IApplicationConfigBean appConfigBean=this.ppcbean.getOwner().getPageBean().getApplicationChild(appid,true);//要打印内容所属的应用对象
        AbsApplicationType appTypeObjTmp=(AbsApplicationType)rrequest.getComponentTypeObj(appConfigBean,null,true);
        List<PrintSubPageBean> lstPrintPagebean=null;
        if(appTypeObjTmp instanceof AbsReportType&&appConfigBean.getPrintBean()!=null
                &&!appConfigBean.getPrintBean().isUseGlobalDefaultPrintTemplate()&&appConfigBean.getPrintBean().isTemplatePrintValue())
        {//如果当前应用是报表，且配置了自己的<print/>，且里面的内容是模板，而不是其它代码，则用其配置的<print/>打印内容进行打印
            lstPrintPagebean=appConfigBean.getPrintBean().getLstPrintPageBeans();
        }
        appTypeObjTmp.printApplication(lstPrintPagebean);
    }
    
    protected void printTotalPageCount(int totalpagecount)
    {}
    
    protected void printHeaderPart(AbsReportType reportTypeObj,PrintTemplateElementBean ptElebean)
    {
        this.wresponse.print(reportTypeObj.showHeader());
    }

    protected void printSearchBox(AbsReportType reportTypeObj,PrintTemplateElementBean ptElebean)
    {
        List<String> lstParts=(List<String>)ptElebean.getValueObj();
        if(lstParts.size()==2)
        {
            this.wresponse.print(reportTypeObj.showSearchBox());
        }else
        {
            ConditionBean cbean=reportTypeObj.getReportBean().getSbean().getConditionBeanByName(lstParts.get(2));
            if(cbean==null)
            {
                throw new WabacusRuntimeException("打印报表"+reportTypeObj.getReportBean().getPath()+"的查询条件失败，没有取到"+lstParts.get(2)+"对应的查询条件");
            }
            this.wresponse.print(TagAssistant.getInstance().showConditionBox(rrequest,cbean,"-1",null));
        }
    }

    protected void printTitlePart(AbsReportType reportTypeObj,PrintTemplateElementBean ptElebean)
    {
        List<String> lstParts=(List<String>)ptElebean.getValueObj();
        if(lstParts.size()==2)
        {
            this.wresponse.print(reportTypeObj.showTitle());
        }else
        {//reportid.title.title/subtitle
            String type=lstParts.get(2).trim();
            if(type.equals("title")||type.equals(""))
            {//只显示标题部分
                this.wresponse.print(Tools.htmlEncode(reportTypeObj.getReportBean().getTitle(rrequest)));
            }else if(type.equals("subtitle"))
            {
                this.wresponse.print(Tools.htmlEncode(reportTypeObj.getReportBean().getSubtitle(rrequest)));
            }else
            {
                throw new WabacusRuntimeException("打印报表"+reportTypeObj.getReportBean().getPath()+"的标题部分失败，指定的"+type+"不合法，只能指定为空、title、subtitle三个值之一");
            }
        }
    }

    protected void printDataPart(AbsReportType reportTypeObj,PrintTemplateElementBean ptElebean)
    {
        List<String> lstParts=(List<String>)ptElebean.getValueObj();
        StringBuilder resultBuf=new StringBuilder();
        if(lstParts.size()==2)
        {
            reportTypeObj.showReportData(resultBuf);
            this.wresponse.print(resultBuf.toString());
        }else
        {
            if(lstParts.get(2).equals("[title]"))
            {
                ((AbsListReportType)reportTypeObj).showReportData(false,resultBuf);
                this.wresponse.print(resultBuf.toString());
            }else if(lstParts.get(2).equals("[data]"))
            {
                ((AbsListReportType)reportTypeObj).showReportData(true,resultBuf);
                this.wresponse.print(resultBuf.toString());
            }else
            {//reportid.data.col.xxx格式
                ColBean cbean=reportTypeObj.getReportBean().getDbean().getColBeanByColProperty(lstParts.get(2));
                if(lstParts.size()==3)
                {
                    if(reportTypeObj instanceof AbsDetailReportType)
                    {
                        this.wresponse.print(((AbsDetailReportType)reportTypeObj).showColData(cbean,false,null)+"&nbsp;&nbsp;");
                        this.wresponse.print(((AbsDetailReportType)reportTypeObj).showColData(cbean,true,null));
                    }else
                    {
                        this.wresponse.print(((AbsListReportType)reportTypeObj).showColData(cbean,-2));
                    }
                }else
                {
                    if(lstParts.get(3).equals("label"))
                    {
                        if(reportTypeObj instanceof AbsDetailReportType)
                        {
                            this.wresponse.print(((AbsDetailReportType)reportTypeObj).showColData(cbean,false,null));
                        }else
                        {//list
                            this.wresponse.print(cbean.getLabel(rrequest));
                        }
                    }else
                    {
                        if(reportTypeObj instanceof AbsDetailReportType)
                        {
                            this.wresponse.print(((AbsDetailReportType)reportTypeObj).showColData(cbean,true,null));
                        }else
                        {
                            this.wresponse.print(((AbsListReportType)reportTypeObj).showColData(cbean,-2));
                        }
                    }
                }
            }
        }
    }

    protected void printNavigatePart(AbsReportType reportTypeObj,PrintTemplateElementBean ptElebean)
    {
        List<String> lstParts=(List<String>)ptElebean.getValueObj();
        if(lstParts.size()==2)
        {
            this.wresponse.print(reportTypeObj.showNavigateBox());
        }else
        {
            this.wresponse.print(TagAssistant.getInstance().getNavigateDisplayInfo(reportTypeObj,lstParts.get(2),null,null,null,null,null));
        }
    }

    protected void printFooterPart(AbsReportType reportTypeObj,PrintTemplateElementBean ltelebean)
    {
        this.wresponse.print(reportTypeObj.showFooter());
    }
}
