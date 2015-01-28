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
package com.wabacus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.config.dataexport.WordRichExcelExportBean;
import com.wabacus.config.other.JavascriptFileBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.exception.WabacusRuntimeTerminateException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.WabacusResponse;
import com.wabacus.system.WabacusUtils;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.assistant.PdfAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportFilterBean;
import com.wabacus.system.component.application.report.chart.FusionChartsReportType;
import com.wabacus.system.component.container.page.PageType;
import com.wabacus.system.fileupload.AbsFileUpload;
import com.wabacus.system.fileupload.DataImportReportUpload;
import com.wabacus.system.fileupload.DataImportTagUpload;
import com.wabacus.system.fileupload.FileInputBoxUpload;
import com.wabacus.system.fileupload.FileTagUpload;
import com.wabacus.system.inputbox.AbsSelectBox;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.inputbox.autocomplete.AutoCompleteBean;
import com.wabacus.system.inputbox.validate.ServerValidateBean;
import com.wabacus.system.print.AbsPrintProvider;
import com.wabacus.system.serveraction.IServerAction;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class WabacusFacade
{
    private static Log log=LogFactory.getLog(WabacusFacade.class);

    public static void displayReport(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=new ReportRequest(request,Consts.DISPLAY_ON_PAGE);
        WabacusResponse wresponse=new WabacusResponse(response);
        displayReport(rrequest,wresponse,rrequest.getStringAttribute("PAGEID",""));
    }

    public static String displayReport(String pageid,Map<String,String> mParams,Locale locale)
    {
        ReportRequest rrequest=new ReportRequest(pageid,Consts.DISPLAY_ON_PAGE,locale);
        if(mParams!=null)
        {
            for(Entry<String,String> entryTmp:mParams.entrySet())
            {
                rrequest.setAttribute(entryTmp.getKey(),entryTmp.getValue());
            }
        }
        WabacusResponse wresponse=new WabacusResponse(null);
        displayReport(rrequest,wresponse,pageid);
        StringBuilder resultBuf=wresponse.getOutBuf();
        if(resultBuf==null) return "";
        return resultBuf.toString();
    }

    private static void displayReport(ReportRequest rrequest,WabacusResponse wresponse,String pageid)
    {
        boolean success=true;
        String errorinfo=null;
        try
        {
            rrequest.setWResponse(wresponse);
            wresponse.setRRequest(rrequest);
            rrequest.init(pageid);
            if(rrequest.getSlaveReportTypeObj()!=null)
            {
                rrequest.getSlaveReportTypeObj().displayOnPage(null);
            }else
            {
                if(!rrequest.isLoadedByAjax()&&rrequest.getRefreshComponentTypeObj() instanceof PageType)
                {
                    wresponse.println(((PageType)rrequest.getRefreshComponentTypeObj()).showOuterHeader());
                }
                rrequest.getRefreshComponentTypeObj().displayOnPage(null);
            }
            log.debug(rrequest.getCurrentStatus());
        }catch(WabacusRuntimeTerminateException wrwe)
        {
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
                errorinfo=wresponse.assembleResultsInfo(wrwe);
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            log.error("显示页面"+pageid+"失败",wre);
            success=false;
            errorinfo=wresponse.assembleResultsInfo(wre);
        }finally
        {
            rrequest.destroy(success);
        }
        if(!rrequest.isLoadedByAjax())
        {
            wresponse.println(rrequest.getPageObj().showOuterHeader());
            wresponse.println(rrequest.getPageObj().showStartWebResources());
            wresponse.println(rrequest.getPageObj().showEndWebResources());
        }
        if(errorinfo!=null&&!errorinfo.trim().equals(""))
        {//如果出了错
            wresponse.println(errorinfo,true);
        }else
        {
            wresponse.println(rrequest.getWResponse().assembleResultsInfo(null));
        }
        if(!rrequest.isLoadedByAjax())
        {
            wresponse.println(rrequest.getPageObj().showOuterFooter());
        }
    }

    public static void exportReportDataOnWordRichexcel(HttpServletRequest request,HttpServletResponse response,int exporttype)
    {
        ReportRequest rrequest=new ReportRequest(request,exporttype);
        WabacusResponse wresponse=new WabacusResponse(response);
        exportReportDataOnWordRichexcel(rrequest.getStringAttribute("PAGEID",""),rrequest,wresponse,exporttype);
    }

    private static void exportReportDataOnWordRichexcel(String pageid,ReportRequest rrequest,WabacusResponse wresponse,int exporttype)
    {
        boolean success=true;
        String errorinfo=null;
        try
        {
            rrequest.setWResponse(wresponse);
            wresponse.setRRequest(rrequest);
            rrequest.init(pageid);
            wresponse.initOutput(rrequest.getDataExportFilename());
            IComponentType ccTypeObjTmp;
            Object dataExportTplObjTmp;
            WordRichExcelExportBean debeanTmp;
            for(IComponentConfigBean ccbeanTmp:rrequest.getLstComponentBeans())
            {
                ccTypeObjTmp=rrequest.getComponentTypeObj(ccbeanTmp,null,true);
                dataExportTplObjTmp=null;
                if(ccbeanTmp.getDataExportsBean()!=null)
                {//如果此组件配置了<dataexports/>
                    debeanTmp=(WordRichExcelExportBean)ccbeanTmp.getDataExportsBean().getDataExportBean(exporttype);//有在<dataexports/>中配置了type为此导出类型的<dataexport/>
                    if(debeanTmp!=null) dataExportTplObjTmp=debeanTmp.getDataExportTplObj();
                }
                ccTypeObjTmp.displayOnExportDataFile(dataExportTplObjTmp,true);
            }
        }catch(WabacusRuntimeTerminateException wrwe)
        {
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
                errorinfo=wresponse.assembleResultsInfo(wrwe);
                try
                {
                    if(!Tools.isEmpty(errorinfo)) wresponse.println(errorinfo,true);
                }catch(Exception e)
                {
                    log.error("导出页面"+pageid+"下的应用"+rrequest.getStringAttribute("INCLUDE_APPLICATIONIDS","")+"数据失败",e);
                }
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            log.error("导出页面"+rrequest.getPagebean().getId()+"下的报表失败，",wre);
            success=false;
            errorinfo=rrequest.getWResponse().assembleResultsInfo(wre);
            try
            {
                if(!Tools.isEmpty(errorinfo)) wresponse.println(errorinfo,true);
            }catch(Exception e)
            {
                log.error("导出页面"+pageid+"下的应用"+rrequest.getStringAttribute("INCLUDE_APPLICATIONIDS","")+"数据失败",e);
            }
        }finally
        {
            if(rrequest.isExportToLocalFile())
            {//放在这里处理主要是为了报表和页面后置动作中能取到导出的数据文件
                wresponse.writeBufDataToLocalFile();
                if(rrequest.isDataexport_localstroagezip())
                {
                    tarDataFile(rrequest);
                }
            }
            rrequest.destroy(success);
        }
        doPostDataExport(rrequest,wresponse);
    }

    public static void exportReportDataOnPlainExcel(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=new ReportRequest(request,Consts.DISPLAY_ON_PLAINEXCEL);
        WabacusResponse wresponse=new WabacusResponse(response);
        exportReportDataOnPlainExcel(rrequest.getStringAttribute("PAGEID",""),rrequest,wresponse);
    }

    public static void exportReportDataOnPlainExcel(String pageid,Locale locale)
    {
        ReportRequest rrequest=new ReportRequest(pageid,Consts.DISPLAY_ON_PLAINEXCEL,locale);
        WabacusResponse wresponse=new WabacusResponse(null);
        exportReportDataOnPlainExcel(pageid,rrequest,wresponse);
    }

    private static void exportReportDataOnPlainExcel(String pageid,ReportRequest rrequest,WabacusResponse wresponse)
    {
        boolean success=true;
        try
        {
            rrequest.setWResponse(wresponse);
            wresponse.setRRequest(rrequest);
            rrequest.init(pageid);
            if(rrequest.getLstAllReportBeans()==null||rrequest.getLstAllReportBeans().size()==0)
            {
                throw new WabacusRuntimeException("导出页面"+pageid+"上的数据失败，plainexcel导出方式只能导出报表，不能导出其它应用");
            }
            Workbook workbook=new HSSFWorkbook();
            AbsReportType reportTypeObjTmp;
            for(ReportBean rbTmp:rrequest.getLstAllReportBeans())
            {
                reportTypeObjTmp=(AbsReportType)rrequest.getComponentTypeObj(rbTmp,null,false);
                reportTypeObjTmp.displayOnPlainExcel(workbook);
            }
            BufferedOutputStream bos=null;
            if(rrequest.isExportToLocalFile())
            {
                bos=new BufferedOutputStream(new FileOutputStream(new File(rrequest.getDataExportFilepath())));
            }else
            {
                String title=WabacusAssistant.getInstance().encodeAttachFilename(rrequest.getRequest(),rrequest.getDataExportFilename());
                wresponse.getResponse().setHeader("Content-disposition","attachment;filename="+title+".xls");
                bos=new BufferedOutputStream(wresponse.getResponse().getOutputStream());
            }
            workbook.write(bos);
            bos.close();
            if(rrequest.isExportToLocalFile()&&rrequest.isDataexport_localstroagezip())
            {
                tarDataFile(rrequest);
            }
        }catch(WabacusRuntimeTerminateException wrwe)
        {
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            log.error("导出页面"+rrequest.getPagebean().getId()+"下的报表失败",wre);
            success=false;
        }finally
        {
            rrequest.destroy(success);
        }
        doPostDataExport(rrequest,wresponse);
    }

    public static void exportReportDataOnPDF(HttpServletRequest request,HttpServletResponse response,int showtype)
    {
        ReportRequest rrequest=new ReportRequest(request,Consts.DISPLAY_ON_PDF);
        WabacusResponse wresponse=new WabacusResponse(response);
        exportReportDataOnPDF(rrequest.getStringAttribute("PAGEID",""),rrequest,wresponse);
    }

    private static void exportReportDataOnPDF(String pageid,ReportRequest rrequest,WabacusResponse wresponse)
    {
        boolean success=true;
        try
        {
            rrequest.setWResponse(wresponse);
            wresponse.setRRequest(rrequest);
            rrequest.init(pageid);
            if(rrequest.getLstAllReportBeans()==null||rrequest.getLstAllReportBeans().size()==0)
            {
                throw new WabacusRuntimeException("导出页面"+pageid+"上的数据失败，plainexcel导出方式只能导出报表，不能导出其它应用");
            }
            Document document=new Document();
            ByteArrayOutputStream baosResult=new ByteArrayOutputStream();
            PdfCopy pdfCopy=new PdfCopy(document,baosResult);
            document.open();
            boolean ispdfprint=rrequest.isPdfPrintAction();
            for(IComponentConfigBean ccbeanTmp:rrequest.getLstComponentBeans())
            {//依次导出配置有PDF模板的组件
                PDFExportBean pdfbeanTmp=null;
                if(ispdfprint)
                {
                    pdfbeanTmp=ccbeanTmp.getPdfPrintBean();
                }else if(ccbeanTmp.getDataExportsBean()!=null)
                {
                    pdfbeanTmp=(PDFExportBean)ccbeanTmp.getDataExportsBean().getDataExportBean(Consts.DATAEXPORT_PDF);
                }
                if(pdfbeanTmp!=null&&pdfbeanTmp.getPdftemplate()!=null&&!pdfbeanTmp.getPdftemplate().trim().equals(""))
                {
                    PdfAssistant.getInstance()
                            .addPdfPageToDocument(pdfCopy,PdfAssistant.getInstance().showReportDataOnPdfWithTpl(rrequest,ccbeanTmp));
                }
            }
            AbsReportType reportTypeObjTmp;
            for(ReportBean rbTmp:rrequest.getLstAllReportBeans())
            {
                reportTypeObjTmp=(AbsReportType)rrequest.getComponentTypeObj(rbTmp,null,false);
                if(rrequest.isReportInPdfTemplate(rbTmp.getId())) continue;//如果当前报表是在某个有PDF模板的组件中导出，则不在这里导，而是在上面导出
                PdfAssistant.getInstance().addPdfPageToDocument(pdfCopy,reportTypeObjTmp.displayOnPdf());
            }
            document.close();
            BufferedOutputStream bos=null;
            if(rrequest.isExportToLocalFile())
            {
                bos=new BufferedOutputStream(new FileOutputStream(new File(rrequest.getDataExportFilepath())));
            }else
            {
                if(!ispdfprint)
                {
                    String title=WabacusAssistant.getInstance().encodeAttachFilename(rrequest.getRequest(),rrequest.getDataExportFilename());
                    wresponse.getResponse().setHeader("Content-disposition","attachment;filename="+title+".pdf");
                }
                wresponse.getResponse().setContentLength(baosResult.size());
                bos=new BufferedOutputStream(wresponse.getResponse().getOutputStream());
            }
            baosResult.writeTo(bos);
            bos.close();
            baosResult.close();
            if(rrequest.isExportToLocalFile()&&rrequest.isDataexport_localstroagezip())
            {
                tarDataFile(rrequest);
            }
        }catch(WabacusRuntimeTerminateException wrwe)
        {
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            log.error("导出页面"+rrequest.getPagebean().getId()+"下的报表失败",wre);
            success=false;
        }finally
        {
            rrequest.destroy(success);
        }
        doPostDataExport(rrequest,wresponse);
    }

    private static void doPostDataExport(ReportRequest rrequest,WabacusResponse wresponse)
    {
        if(!rrequest.isExportToLocalFile()) return;
        if(rrequest.isDataexport_localstroagedownload())
        {
            try
            {
                wresponse.getResponse().sendRedirect(rrequest.getDataExportFileurl());
            }catch(IOException e)
            {
                outputMessageToPage(rrequest,wresponse,"<br/></br><div id='div1' class='cls-dataexport-failed'>下载文件"+rrequest.getDataExportFileurl()+"失败，请尝试直接访问此URL进行下载</div>");
            }
        }else
        {
            outputMessageToPage(rrequest,wresponse,"<br/><br/><div id='div1' class='cls-dataexport-success'>导出数据文件成功</div><div id='div2' class='cls-dataexport-path'>存放路径："
                    +rrequest.getDataExportFilepath()+"</div>");
        }
    }
    
    private static void outputMessageToPage(ReportRequest rrequest,WabacusResponse wresponse,String content)
    {
        try
        {
            PrintWriter out=wresponse.getResponse().getWriter();
            out
                    .println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
            out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+Config.encode+"\">");
            List<String> lstCsses=ComponentAssistant.getInstance().initDisplayCss(rrequest);
            if(!Tools.isEmpty(lstCsses))
            {
                for(String cssTmp:lstCsses)
                {
                    out.println("<LINK rel=\"stylesheet\" type=\"text/css\" href=\""+cssTmp+"\"/>");
                }
            }
            out.println(content);
        }catch(IOException e)
        {
            throw new WabacusRuntimeException("初始化页面输出失败",e);
        }
    }
    
    private static void tarDataFile(ReportRequest rrequest)
    {
        String originFilePath=rrequest.getDataExportFilepath();
        if(Tools.isEmpty(originFilePath)||originFilePath.indexOf(".")<0) return;
        String fileurl=rrequest.getDataExportFileurl();
        String zipFilePath=originFilePath.substring(0,originFilePath.lastIndexOf("."))+".zip";
        if(!Tools.isEmpty(fileurl)&&fileurl.indexOf(".")>0)
        {
            fileurl=fileurl.substring(0,fileurl.lastIndexOf("."))+".zip";
        }
        rrequest.setDataExportFilepath(zipFilePath);
        rrequest.setDataExportFileurl(fileurl);
        tarFileToZip(originFilePath,zipFilePath);
    }
    
    private static void tarFileToZip(String originFilePath,String zipFilePath)
    {
        if(Tools.isEmpty(originFilePath)||Tools.isEmpty(zipFilePath)) return;
        int idx=originFilePath.lastIndexOf(File.separator);
        String fileName=idx>0?originFilePath.substring(idx+File.separator.length()):originFilePath;//取到文件名
        idx=fileName.lastIndexOf("_");
        if(idx>0) fileName=fileName.substring(idx+1).trim();
        try
        {
            FileOutputStream fout=new FileOutputStream(zipFilePath);
            ZipOutputStream zipout=new ZipOutputStream(fout);
            FileInputStream fis=new FileInputStream(originFilePath);
            zipout.putNextEntry(new ZipEntry(fileName));
            byte[] buffer=new byte[1024];
            int len;
            while((len=fis.read(buffer))!=-1)
            {
                zipout.write(buffer,0,len);
            }
            zipout.closeEntry(); 
            fis.close();
            zipout.close();
            fout.close();
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("压缩文件"+originFilePath+"到zip失败",e);
        }
    }
    
    public static void printComponents(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=new ReportRequest(request,Consts.DISPLAY_ON_PRINT);
        WabacusResponse wresponse=new WabacusResponse(response);
        rrequest.setWResponse(wresponse);
        wresponse.setRRequest(rrequest);
        String pageid=rrequest.getStringAttribute("PAGEID","");
        String printComid=rrequest.getStringAttribute("COMPONENTIDS","");
        boolean success=true;
        String errorinfo=null;
        try
        {
            rrequest.init(pageid);
            if(printComid.equals(""))
            {
                throw new WabacusRuntimeException("没有传入打印的组件ID");
            }
            if(rrequest.getLstComponentBeans()==null||rrequest.getLstComponentBeans().size()==0)
            {
                throw new WabacusRuntimeException("页面"+pageid+"不存在ID为"+printComid+"的组件");
            }
            if(rrequest.getLstComponentBeans().size()>1)
            {
                throw new WabacusRuntimeException("打印页面"+pageid+"上的组件"+printComid+"失败，一次只能打印一个组件");
            }
            AbsPrintProviderConfigBean printBean=rrequest.getLstComponentBeans().get(0).getPrintBean();
            if(printBean==null)
            {
                throw new WabacusRuntimeException("页面"+pageid+"ID为"+printComid+"的组件没有配置<print/>");
            }
            AbsPrintProvider printProvider=printBean.createPrintProvider(rrequest);
            printProvider.doPrint();
            wresponse.addOnloadMethod(rrequest.getLstComponentBeans().get(0).getPrintBean().getPrintJsMethodName(),"",true);
            wresponse.println(rrequest.getWResponse().assembleResultsInfo(null));
        }catch(WabacusRuntimeTerminateException wrwe)
        {
            if(wresponse.getStatecode()==Consts.STATECODE_FAILED)
            {
                success=false;
                errorinfo=wresponse.assembleResultsInfo(wrwe);
            }
        }catch(Exception wre)
        {
            wresponse.setStatecode(Consts.STATECODE_FAILED);
            log.error("打印页面"+pageid+"下的应用失败，",wre);
            success=false;
            errorinfo=rrequest.getWResponse().assembleResultsInfo(wre);
        }finally
        {
            rrequest.destroy(success);
        }
        if(errorinfo!=null&&!errorinfo.trim().equals(""))
        {
            try
            {
                wresponse.println(errorinfo,true);
            }catch(Exception e)
            {
                log.error("打印页面"+pageid+"下的组件"+printComid+"数据失败",e);
            }
        }
    }

    public static String getFilterDataList(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=null;
        ReportBean rbean=null;
        StringBuffer resultBuf=new StringBuffer();
        try
        {
            rrequest=new ReportRequest(request,-1);
            WabacusResponse wresponse=new WabacusResponse(response);
            wresponse.setRRequest(rrequest);
            rrequest.setWResponse(wresponse);
            rrequest.initGetFilterDataList();
            rbean=rrequest.getLstAllReportBeans().get(0);
            //            StringBuffer columnsBuf=new StringBuffer();
            String colproperty=rrequest.getStringAttribute("FILTER_COLPROP","");
            ColBean cbean=rbean.getDbean().getColBeanByColProperty(colproperty);
            if(cbean==null)
            {
                throw new WabacusRuntimeException("取过滤数据时，根据"+colproperty+"没有取到指定的<col/>配置信息");
            }
            List<ReportDataSetValueBean> lstDsvbeans=rbean.getSbean().getLstDatasetValueBeansOfCbean(cbean);
            AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            AbsListReportFilterBean alfbean=alrcbean.getFilterBean();
            if(rbean.getInterceptor()!=null)
            {
                rrequest.setAttribute(rbean.getId()+"_WABACUS_FILTERBEAN",alfbean);
            }
            AbsListReportFilterBean filterBean=rrequest.getCdb(rbean.getId()).getFilteredBean();
            ColBean cbeanFiltered=filterBean==null?null:(ColBean)filterBean.getOwner();
            String[][] allfilterOptionsArr=getFilterDataArray(rrequest,lstDsvbeans,cbean,null);
            if(allfilterOptionsArr==null||allfilterOptionsArr.length==0)
            {
                resultBuf.append("<item><value>[nodata]</value><label>无选项数据</label></item>");
                return resultBuf.toString();
            }
            List<String> lstSelectedData=new ArrayList<String>();
            if(!alfbean.isConditionRelate())
            {
                String[][] selectedOptionsArr=cbeanFiltered==null?allfilterOptionsArr:getFilterDataArray(rrequest,lstDsvbeans,cbean,cbeanFiltered);
                if(selectedOptionsArr!=null&&selectedOptionsArr.length>0)
                {
                    for(int i=0;i<selectedOptionsArr[0].length;i++)
                    {
                        lstSelectedData.add(selectedOptionsArr[0][i]);
                    }
                }
                log.debug(lstSelectedData);
            }else
            {//与查询条件相关联的列
                String filterval=rrequest.getStringAttribute(alfbean.getConditionname(),"");
                if(!filterval.equals(""))
                {
                    resultBuf.append("<item><value><![CDATA[(ALL_DATA)]]></value><label>(全部)</label></item>");
                }
            }
            for(int i=0;i<allfilterOptionsArr[0].length;i++)
            {
                resultBuf.append("<item>");
                resultBuf.append("<value");
                if(lstSelectedData.contains(allfilterOptionsArr[0][i]))
                {
                    resultBuf.append(" isChecked=\"true\"");
                }
                resultBuf.append("><![CDATA["+allfilterOptionsArr[0][i]+"]]></value>");
                if(allfilterOptionsArr[1]!=null&&!"[BLANK]".equals(allfilterOptionsArr[1][i]))
                {
                    resultBuf.append("<label><![CDATA["+allfilterOptionsArr[1][i]+"]]></label>");
                }
                resultBuf.append("</item>");
            }
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载报表"+rbean!=null?rbean.getPath():""+"的列过滤数据失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
        return resultBuf.toString();
    }

    private static String[][] getFilterDataArray(ReportRequest rrequest,List<ReportDataSetValueBean> lstDsvbeans,ColBean cbean,ColBean cbeanFiltered)
    {
        ReportBean rbean=cbean.getReportBean();
        int maxOptionsCount=Config.getInstance().getSystemConfigValue("colfilter-maxnum-options",-1);
        AbsListReportFilterBean alfbean=((AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY)).getFilterBean();
        List<String[]> lstFilterOptionsData=new ArrayList<String[]>();
        List<String> lstOneDatasetValueOptions=null;
        outer: for(ReportDataSetValueBean dsvbeanTmp:lstDsvbeans)
        {
            lstOneDatasetValueOptions=dsvbeanTmp.getProvider().getColFilterDataSet(rrequest,cbean,
                    cbeanFiltered!=null&&cbeanFiltered.isMatchDataSet(dsvbeanTmp),maxOptionsCount);
            if(rbean.getInterceptor()!=null)
            {
                lstOneDatasetValueOptions=(List)rbean.getInterceptor().afterLoadData(rrequest,rbean,alfbean,lstOneDatasetValueOptions);
            }
            if(lstOneDatasetValueOptions==null||lstOneDatasetValueOptions.size()==0) continue;
            String[] strValueArr=lstOneDatasetValueOptions.toArray(new String[lstOneDatasetValueOptions.size()]);
            String[] strLabelArr=null;
            if(alfbean.getFormatMethod()!=null&&alfbean.getFormatClass()!=null)
            {
                try
                {
                    strLabelArr=(String[])alfbean.getFormatMethod().invoke(alfbean.getFormatClass(),new Object[] { rbean, strValueArr });
                }catch(Exception e)
                {
                    log.warn("调用报表"+rbean.getPath()+"的列"+cbean.getColumn()+"上配置的针对列过滤数据进行格式化方法失败",e);
                }
            }
            if(strLabelArr==null||strLabelArr.length!=strValueArr.length)
            {
                strLabelArr=null;
            }
            String[] strArrayTmp;
            for(int i=0;i<strValueArr.length;i++)
            {
                strArrayTmp=new String[2];
                strArrayTmp[0]=strValueArr[i];
                if(strLabelArr!=null)
                {//用户在格式化方法中返回了专门用于显示的标题部分
                    strArrayTmp[1]=strLabelArr[i];
                }else
                {
                    strArrayTmp[1]="[BLANK]";
                }
                lstFilterOptionsData.add(strArrayTmp);
                if(maxOptionsCount>0&&maxOptionsCount<=lstFilterOptionsData.size()) break outer;
            }
        }
        String[][] strArrayResults=new String[2][lstFilterOptionsData.size()];
        int i=0;
        for(String[] optionArrTmp:lstFilterOptionsData)
        {
            strArrayResults[0][i]=optionArrTmp[0];
            strArrayResults[1][i]=optionArrTmp[1];
            i++;
        }
        return strArrayResults;
    }
    
    public static String getTypePromptDataList(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=null;
        StringBuffer resultBuf=new StringBuffer();
        try
        {
            rrequest=new ReportRequest(request,-1);
            WabacusResponse wresponse=new WabacusResponse(response);
            rrequest.setWResponse(wresponse);
            rrequest.initReportCommon();
            ReportBean rbean=rrequest.getLstAllReportBeans().get(0);
            String inputboxid=rrequest.getStringAttribute("INPUTBOXID","");
            if(inputboxid.equals(""))
            {
                throw new WabacusRuntimeException("没有取到输入框ID，无法获取输入提示数据");
            }
            int idx=inputboxid.lastIndexOf("__");
            if(idx>0)
            {//自动列表/列表表单的输入框
                inputboxid=inputboxid.substring(0,idx);
            }
            TextBox boxObj=rbean.getTextBoxWithingTypePrompt(inputboxid);
            if(boxObj==null)
            {
                throw new WabacusRuntimeException("没有取到相应输入框对象，无法获取提示数据");
            }
            TypePromptBean promptBean=boxObj.getTypePromptBean();
            if(promptBean==null)
            {
                throw new WabacusRuntimeException("输入框没有配置输入提示功能");
            }
            String inputvalue=rrequest.getStringAttribute("TYPE_PROMPT_TXTVALUE","");
            if(boxObj.getOwner() instanceof ConditionBean)
            {
                ConditionBean cbTmp=(ConditionBean)boxObj.getOwner();
                if(ConditionBean.LABELPOSITION_INNER.equals(cbTmp.getLabelposition())&&inputvalue.equals(cbTmp.getLabel(rrequest))) inputvalue="";
            }
            if(inputvalue.equals(""))
            {
                return "";
            }else
            {
                List<Map<String,String>> lstResults=promptBean.getLstRuntimeOptionsData(rrequest,rbean,inputvalue);
                if(lstResults==null||lstResults.size()==0) return "";
                int cnt=promptBean.getResultcount();
                if(cnt>lstResults.size()) cnt=lstResults.size();
                for(int i=0;i<cnt;i++)
                {
                    resultBuf.append("<item ");
                    for(Entry<String,String> entryTmp:lstResults.get(i).entrySet())
                    {
                        resultBuf.append(entryTmp.getKey()).append("=\"").append(entryTmp.getValue()).append("\" ");
                    }
                    resultBuf.append("/>");
                }
            }
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载输入联想数据失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
        return resultBuf.toString();
    }

    public static String getSelectBoxDataList(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=null;
        StringBuilder resultBuf=new StringBuilder();
        try
        {
            rrequest=new ReportRequest(request,-1);
            WabacusResponse wresponse=new WabacusResponse(response);
            rrequest.setWResponse(wresponse);
            rrequest.initGetSelectBoxDataList();
            resultBuf.append("pageid:\"").append(rrequest.getPagebean().getId()).append("\",");
            String selectboxParams=rrequest.getStringAttribute("SELECTBOXIDS_AND_PARENTVALUES","");
            if(selectboxParams.equals("")) return "";
            List<Map<String,String>> lstParams=EditableReportAssistant.getInstance().parseSaveDataStringToList(selectboxParams);
            Map<String,ReportBean> mHasInitReportBeans=new HashMap<String,ReportBean>();
            String realInputboxidTmp, inputboxidTmp;
            AbsSelectBox childSelectBoxTmp;
            ReportBean rbeanTmp;
            for(Map<String,String> mSelectBoxParamsTmp:lstParams)
            {
                realInputboxidTmp=mSelectBoxParamsTmp.get("conditionSelectboxIds");
                if(!Tools.isEmpty(realInputboxidTmp))
                {
                    List<String> lstSelectboxIds=Tools.parseStringToList(realInputboxidTmp,";",false);
                    for(String selectboxidTmp:lstSelectboxIds)
                    {
                        rbeanTmp=getReportBeanByInputboxid(rrequest,selectboxidTmp,mHasInitReportBeans);
                        if(rbeanTmp==null) continue;
                        childSelectBoxTmp=rbeanTmp.getChildSelectBoxInConditionById(selectboxidTmp);
                        if(childSelectBoxTmp==null)
                        {
                            throw new WabacusRuntimeException("报表"+rbeanTmp.getPath()+"不存在id为"+selectboxidTmp+"的子下拉框");
                        }
                        List<Map<String,String>> lstOptionsResult=childSelectBoxTmp.getOptionsList(rrequest,getMParentValuesOfConditionBox(rrequest,
                                rbeanTmp,childSelectBoxTmp));
                        resultBuf.append(assembleOptionsResult(selectboxidTmp,lstOptionsResult));
                    }
                }else
                {
                    realInputboxidTmp=mSelectBoxParamsTmp.get("wx_inputboxid");//取到下拉框ID
                    if(Tools.isEmpty(realInputboxidTmp)) continue;
                    inputboxidTmp=realInputboxidTmp;
                    int idx=inputboxidTmp.lastIndexOf("__");
                    if(idx>0) inputboxidTmp=inputboxidTmp.substring(0,idx);
                    rbeanTmp=getReportBeanByInputboxid(rrequest,realInputboxidTmp,mHasInitReportBeans);
                    if(rbeanTmp==null) continue;
                    childSelectBoxTmp=rbeanTmp.getChildSelectBoxInColById(inputboxidTmp);
                    if(childSelectBoxTmp==null)
                    {
                        throw new WabacusRuntimeException("报表"+rbeanTmp.getPath()+"不存在id为"+inputboxidTmp+"的子下拉框");
                    }
                    mSelectBoxParamsTmp.remove("wx_inputboxid");
                    List<Map<String,String>> lstOptionsResult=childSelectBoxTmp.getOptionsList(rrequest,mSelectBoxParamsTmp);
                    resultBuf.append(assembleOptionsResult(realInputboxidTmp,lstOptionsResult));
                }
            }
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载下拉框数据失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
        if(resultBuf.length()>0&&resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
        return resultBuf.length()>0?"{"+resultBuf.toString()+"}":"";
    }

    private static ReportBean getReportBeanByInputboxid(ReportRequest rrequest,String inputboxid,Map<String,ReportBean> mHasInitReportBeans)
    {
        String reportidTmp=WabacusAssistant.getInstance().getComponentIdByGuid(WabacusAssistant.getInstance().getReportGuidByInputboxId(inputboxid));
        if(Tools.isEmpty(reportidTmp)) return null;
        ReportBean rbeanTmp=mHasInitReportBeans.get(reportidTmp);
        if(rbeanTmp==null)
        {
            rbeanTmp=rrequest.getPagebean().getReportChild(reportidTmp,true);
            mHasInitReportBeans.put(reportidTmp,rbeanTmp);
        }
        return rbeanTmp;
    }
    
    private static Map<String,String> getMParentValuesOfConditionBox(ReportRequest rrequest,ReportBean rbean,AbsSelectBox childSelectBox)
    {
        Map<String,String> mResults=new HashMap<String,String>();
        ConditionBean cbTmp;
        String parentidTmp;
        for(Entry<String,Boolean> entryTmp:childSelectBox.getMParentIds().entrySet())
        {
            parentidTmp=entryTmp.getKey();
            cbTmp=rbean.getSbean().getConditionBeanByName(parentidTmp);
            mResults.put(parentidTmp,cbTmp.getConditionValue(rrequest,-1));
        }
        return mResults;
    }
    
    private static String assembleOptionsResult(String realSelectboxid,List<Map<String,String>> lstOptionsResult)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(realSelectboxid).append(":[");
        if(lstOptionsResult!=null&&lstOptionsResult.size()>0)
        {
            String labelTmp, valueTmp;
            for(Map<String,String> mItemsTmp:lstOptionsResult)
            {
                labelTmp=mItemsTmp.get("label");
                valueTmp=mItemsTmp.get("value");
                labelTmp=labelTmp==null?"":labelTmp.trim();
                valueTmp=valueTmp==null?"":valueTmp.trim();
                resultBuf.append("{label:\"").append(labelTmp).append("\",");
                resultBuf.append("value:\"").append(valueTmp).append("\"},");
            }
        }
        if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
        resultBuf.append("],");
        return resultBuf.toString();
    }

    public static String getAutoCompleteColValues(HttpServletRequest request,HttpServletResponse response)
    {
        StringBuilder resultBuf=new StringBuilder();
        ReportRequest rrequest=null;
        try
        {
            rrequest=new ReportRequest(request,-1);
            WabacusResponse wresponse=new WabacusResponse(response);
            rrequest.setWResponse(wresponse);
            rrequest.initGetAutoCompleteColValues();
            ReportBean rbean=rrequest.getLstAllReportBeans().get(0);
            String conditionparams=request.getParameter("AUTOCOMPLETE_COLCONDITION_VALUES");
            List<Map<String,String>> lstConditionParamsValue=EditableReportAssistant.getInstance().parseSaveDataStringToList(conditionparams);
            if(lstConditionParamsValue==null||lstConditionParamsValue.size()==0) return "";
            rrequest.setAttribute("COL_CONDITION_VALUES",lstConditionParamsValue.get(0));
            AutoCompleteBean accbean=rrequest.getAutoCompleteSourceInputBoxObj().getAutoCompleteBean();
            Map<String,String> mAutoCompleteData=accbean.getDatasetProvider().getAutoCompleteColumnsData(rrequest,lstConditionParamsValue.get(0));
            if(rbean.getInterceptor()!=null)
            {
                mAutoCompleteData=(Map<String,String>)rbean.getInterceptor().afterLoadData(rrequest,rbean,accbean,mAutoCompleteData);
            }
            if(mAutoCompleteData==null||mAutoCompleteData.size()==0) return "";
            resultBuf.append("{");
            String propTmp, valueTmp;
            for(ColBean cbTmp:accbean.getLstAutoCompleteColBeans())
            {
                propTmp=cbTmp.getProperty();
                valueTmp=mAutoCompleteData.get(propTmp);
                if(valueTmp==null) valueTmp="";
                resultBuf.append(propTmp).append(":\"").append(valueTmp).append("\",");
                mAutoCompleteData.remove(propTmp);
            }
            for(Entry<String,String> entryTmp:mAutoCompleteData.entrySet())
            {//mColData中还有上面没处理的键值对（可能是在开发人员在拦截器加载数据后置动作中加进去的）
                resultBuf.append(entryTmp.getKey()).append(":\"").append(entryTmp.getValue()).append("\",");
            }
            if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
            resultBuf.append("}");
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载自动填充的输入框数据失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
        return resultBuf.toString();
    }

    public static void showUploadFilePage(HttpServletRequest request,PrintWriter out)
    {
        String contentType=request.getHeader("Content-type");
        String fileuploadtype=null;
        if(contentType!=null&&contentType.startsWith("multipart/"))
        {
            fileuploadtype=(String)request.getAttribute("FILEUPLOADTYPE");
            fileuploadtype=fileuploadtype==null?"":fileuploadtype.trim();
        }else
        {
            fileuploadtype=Tools.getRequestValue(request,"FILEUPLOADTYPE","");
        }
        AbsFileUpload fileUpload=getFileUploadObj(request,fileuploadtype);
        if(fileUpload==null)
        {
            out.println("显示文件上传界面失败，未知的文件上传类型");
            return;
        }
        importWebresources(out);
        out.println("<form  action=\""+Config.showreport_url
                +"\" style=\"margin:0px\" method=\"post\" onsubmit=\"return doFileUploadAction()\" enctype=\"multipart/form-data\" name=\"fileuploadform\">");
        out.println("<input type='hidden' name='FILEUPLOADTYPE' value='"+fileuploadtype+"'/>");
        fileUpload.showUploadForm(out);
        out.println("</form>");
        out.println("<div id=\"LOADING_IMG_ID\" class=\"cls-loading-img\"></div>");
    }

    public static void uploadFile(HttpServletRequest request,HttpServletResponse response)
    {
        PrintWriter out=null;
        try
        {
            out=response.getWriter();
        }catch(IOException e1)
        {
            throw new WabacusRuntimeException("从response中获取PrintWriter对象失败",e1);
        }
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+Config.encode+"\">");
        importWebresources(out);
        if(Config.getInstance().getSystemConfigValue("prompt-dialog-type","artdialog").equals("artdialog"))
        {
            out.print("<script type=\"text/javascript\"  src=\""+Config.webroot+"webresources/component/artDialog/artDialog.js\"></script>");
            out.print("<script type=\"text/javascript\"  src=\""+Config.webroot+"webresources/component/artDialog/plugins/iframeTools.js\"></script>");
        }
        /**if(true)
        {
            out.print("<table style=\"margin:0px;\"><tr><td style='font-size:13px;'><font color='#ff0000'>");
            out.print("这里是公共演示，不允许上传文件，您可以在本地部署WabacusDemo演示项目，进行完全体验，只需几步即可部署完成\n\rWabacusDemo.war位于下载包的samples/目录中");
            out.print("</font></td></tr></table>");
            return;
        }*/
        DiskFileItemFactory factory=new DiskFileItemFactory();
        factory.setSizeThreshold(4096);
        String repositoryPath=FilePathAssistant.getInstance().standardFilePath(Config.webroot_abspath+File.separator+"wxtmpfiles"+File.separator+"upload"+File.separator);
        FilePathAssistant.getInstance().checkAndCreateDirIfNotExist(repositoryPath);
        factory.setRepository(new File(repositoryPath));
        ServletFileUpload fileUploadObj=new ServletFileUpload();
        fileUploadObj.setFileItemFactory(factory);
        fileUploadObj.setHeaderEncoding(Config.encode);
        List lstFieldItems=null;
        String errorinfo=null;
        try
        {
            lstFieldItems=fileUploadObj.parseRequest(request);
            if(lstFieldItems==null||lstFieldItems.size()==0)
            {
                errorinfo="上传失败，没有取到要上传的文件";
            }
        }catch(FileUploadException e)
        {
            log.error("获取上传文件失败",e);
            errorinfo="获取上传文件失败";
        }
        Map<String,String> mFormFieldValues=new HashMap<String,String>();
        Iterator itFieldItems=lstFieldItems.iterator();
        FileItem item;
        while(itFieldItems.hasNext())
        {//将所有普通表单字段先取出放入mFormFieldValues中供后面传文件时使用
            item=(FileItem)itFieldItems.next();
            if(item.isFormField())
            {
                try
                {
                    mFormFieldValues.put(item.getFieldName(),item.getString(Config.encode));
                    request.setAttribute(item.getFieldName(),item.getString(Config.encode));
                }catch(UnsupportedEncodingException e)
                {
                    log.warn("进行文件上传时获取的表单数据不能转换成"+Config.encode+"编码类型",e);
                }
            }
        }
        String fileuploadtype=mFormFieldValues.get("FILEUPLOADTYPE");
        AbsFileUpload fileUpload=getFileUploadObj(request,fileuploadtype);
        boolean isPromtAuto=true;
        if(fileUpload==null)
        {
            errorinfo="上传文件失败，未知的文件上传类型";
        }else if(errorinfo==null||errorinfo.trim().equals(""))
        {
            fileUpload.setMFormFieldValues(mFormFieldValues);
            errorinfo=fileUpload.doFileUpload(lstFieldItems,out);
            if(fileUpload.getInterceptorObj()!=null)
            {
                isPromtAuto=fileUpload.getInterceptorObj().beforeDisplayFileUploadPrompt(request,lstFieldItems,fileUpload.getMFormFieldValues(),
                        errorinfo,out);
            }
        }
        out.println("<script language='javascript'>");
        out.println("  try{hideLoadingMessage();}catch(e){}");
        out.println("</script>");
        if(isPromtAuto)
        {
            if(errorinfo==null||errorinfo.trim().equals(""))
            {
                out.println("<script language='javascript'>");
                fileUpload.promptSuccess(out,Config.getInstance().getSystemConfigValue("prompt-dialog-type","artdialog").equals("artdialog"));
                out.println("</script>");
            }else
            {
                out.println("<table style=\"margin:0px;\"><tr><td style='font-size:13px;'><font color='#ff0000'>"+errorinfo
                        +"</font></td></tr></table>");
            }
        }
        if(errorinfo!=null&&!errorinfo.trim().equals(""))
        {
            if(fileUpload!=null)
            {
                request.setAttribute("WX_FILE_UPLOAD_FIELDVALUES",fileUpload.getMFormFieldValues());
            }
            showUploadFilePage(request,out);
        }else if(!isPromtAuto)
        {//如果成功了，但是不自动提示，则将界面自动关闭掉（如果是自动提示，上面在提示的时候就已经关闭了）
            out.println("<script language='javascript'>");
            if(Config.getInstance().getSystemConfigValue("prompt-dialog-type","artdialog").equals("artdialog"))
            {
                out.println("art.dialog.close();");
            }else
            {
                out.println("parent.closePopupWin();");
            }
            out.println("</script>");
        }
    }

    private static void importWebresources(PrintWriter out)
    {
        List<JavascriptFileBean> lstResult=new UniqueArrayList<JavascriptFileBean>();
        List<JavascriptFileBean> lstJsTmp=Config.getInstance().getLstDefaultGlobalJavascriptFiles();
        if(lstJsTmp!=null) lstResult.addAll(lstJsTmp);
        lstJsTmp=Config.getInstance().getLstGlobalJavascriptFiles();
        if(lstJsTmp!=null) lstResult.addAll(lstJsTmp);
        Collections.sort(lstResult);
        for(JavascriptFileBean jsBeanTmp:lstResult)
        {
            out.println("<script type=\"text/javascript\"  src=\""+jsBeanTmp.getJsfileurl()+"\"></script>");
        }
        List<String> lstCss=Config.getInstance().getUlstGlobalCss();
        if(lstCss!=null)
        {
            for(String cssTmp:lstCss)
            {
                out.println("<LINK rel=\"stylesheet\" type=\"text/css\" href=\""+Tools.replaceAll(cssTmp,Consts_Private.SKIN_PLACEHOLDER,Config.skin)+"\"/>");
            }
        }
    }
    
    private static AbsFileUpload getFileUploadObj(HttpServletRequest request,String fileuploadtype)
    {
        fileuploadtype=fileuploadtype==null?"":fileuploadtype.trim();
        AbsFileUpload fileUpload=null;
        if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_FILEINPUTBOX))
        {
            fileUpload=new FileInputBoxUpload(request);
        }else if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_FILETAG))
        {
            fileUpload=new FileTagUpload(request);
        }else if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_DATAIMPORTREPORT))
        {
            fileUpload=new DataImportReportUpload(request);
        }else if(fileuploadtype.equalsIgnoreCase(Consts_Private.FILEUPLOADTYPE_DATAIMPORTTAG))
        {
            fileUpload=new DataImportTagUpload(request);
        }
        return fileUpload;
    }
    
    public static void downloadFile(HttpServletRequest request,HttpServletResponse response)
    {
        response.setContentType("application/x-msdownload;");
        BufferedInputStream bis=null;
        BufferedOutputStream bos=null;
        String realfilepath=null;
        try
        {
            bos=new BufferedOutputStream(response.getOutputStream());
            String serverfilename=request.getParameter("serverfilename");
            String serverfilepath=request.getParameter("serverfilepath");
            String newfilename=request.getParameter("newfilename");
            if(serverfilename==null||serverfilename.trim().equals(""))
            {
                bos.write("没有取到要下载的文件名".getBytes());
                return;
            }
            if(serverfilename.indexOf("/")>=0||serverfilename.indexOf("\\")>=0)
            {
                bos.write("指定要下载的文件名包含非法字符".getBytes());
                return;
            }
            if(serverfilepath==null||serverfilepath.trim().equals(""))
            {
                bos.write("没有取到要下载的文件路径".getBytes());
                return;
            }
            if(newfilename==null||newfilename.trim().equals("")) newfilename=serverfilename;
            newfilename=WabacusAssistant.getInstance().encodeAttachFilename(request,newfilename);
            response.setHeader("Content-disposition","attachment;filename="+newfilename);
            //response.setHeader("Content-disposition","inline;filename="+newfilename);
            String realserverfilepath=null;
            if(Tools.isDefineKey("$",serverfilepath))
            {
                realserverfilepath=Config.getInstance().getResourceString(null,null,serverfilepath,true);
            }else
            {
                realserverfilepath=WabacusUtils.decodeFilePath(serverfilepath);
            }
            if(realserverfilepath==null||realserverfilepath.trim().equals(""))
            {
                bos.write(("根据"+serverfilepath+"没有取到要下载的文件路径").getBytes());
            }
            realserverfilepath=WabacusAssistant.getInstance().parseConfigPathToRealPath(realserverfilepath,Config.webroot_abspath);
            if(Tools.isDefineKey("classpath",realserverfilepath))
            {
                realserverfilepath=Tools.getRealKeyByDefine("classpath",realserverfilepath);
                realserverfilepath=Tools.replaceAll(realserverfilepath+"/"+serverfilename,"//","/").trim();
                while(realserverfilepath.startsWith("/"))
                    realserverfilepath=realserverfilepath.substring(1);//因为这种配置方式是用ClassLoader进行加载，而不是Class，所以必须不能以/打头
                bis=new BufferedInputStream(ConfigLoadManager.currentDynClassLoader.getResourceAsStream(realserverfilepath));
                response.setContentLength(bis.available());
            }else
            {
                File downloadFileObj=new File(FilePathAssistant.getInstance().standardFilePath(realserverfilepath+File.separator+serverfilename));
                if(!downloadFileObj.exists()||downloadFileObj.isDirectory())
                {
                    bos.write(("没有找到要下载的文件"+serverfilename).getBytes());
                    return;
                }
                //response.setHeader("Content-Length", String.valueOf(downloadFileObj.length()));
                bis=new BufferedInputStream(new FileInputStream(downloadFileObj));
            }
            byte[] buff=new byte[1024];
            int bytesRead;
            while((bytesRead=bis.read(buff,0,buff.length))!=-1)
            {
                bos.write(buff,0,bytesRead);
            }
        }catch(IOException e)
        {
            throw new WabacusRuntimeException("下载文件"+realfilepath+"失败",e);
        }finally
        {
            try
            {
                if(bis!=null) bis.close();
            }catch(IOException e)
            {
                log.warn("下载文件"+realfilepath+"时，关闭输入流失败",e);
            }
            try
            {
                if(bos!=null) bos.close();
            }catch(IOException e)
            {
                log.warn("下载文件"+realfilepath+"时，关闭输出流失败",e);
            }
        }
    }

    public static String invokeServerAction(HttpServletRequest request,HttpServletResponse response)
    {
        String serverClassName=request.getParameter("WX_SERVERACTION_SERVERCLASS");
        if(serverClassName==null||serverClassName.trim().equals(""))
        {
            throw new WabacusRuntimeException("没有传入要调用的服务器端类");
        }
        String params=request.getParameter("WX_SERVERACTION_PARAMS");
        List<Map<String,String>> lstParamsValue=EditableReportAssistant.getInstance().parseSaveDataStringToList(params);
        try
        {
            Object obj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(serverClassName.trim()).newInstance();
            if(!(obj instanceof IServerAction))
            {
                throw new WabacusRuntimeException("调用的服务器端类"+serverClassName+"没有实现"+IServerAction.class.getName()+"接口");
            }
            return ((IServerAction)obj).executeServerAction(request,response,lstParamsValue);
        }catch(InstantiationException e)
        {
            throw new WabacusRuntimeException("调用的服务器端类"+serverClassName+"无法实例化",e);
        }catch(IllegalAccessException e)
        {
            throw new WabacusRuntimeException("调用的服务器端类"+serverClassName+"无法访问",e);
        }
    }
    
    public static String doServerValidateOnBlur(HttpServletRequest request,HttpServletResponse response)
    {
        String inputboxid=request.getParameter("INPUTBOXID");
        if(inputboxid==null||inputboxid.trim().equals(""))
        {
            throw new WabacusRuntimeException("没有传入要校验输入框的ID");
        }
        String boxvalue=request.getParameter("INPUTBOX_VALUE");
        String othervalues=request.getParameter("OTHER_VALUES");
        StringBuilder resultBuf=new StringBuilder();
        ReportRequest rrequest=null;
        try
        {
            rrequest=new ReportRequest(request,-1);
            WabacusResponse wresponse=new WabacusResponse(response);
            rrequest.setWResponse(wresponse);
            rrequest.initReportCommon();
            List<Map<String,String>> lstOthervalues=EditableReportAssistant.getInstance().parseSaveDataStringToList(othervalues);
            Map<String,String> mOtherValues=null;
            if(lstOthervalues!=null&&lstOthervalues.size()>0) mOtherValues=lstOthervalues.get(0);
            ReportBean rbean=rrequest.getLstAllReportBeans().get(0);
            ServerValidateBean  svb=rbean.getServerValidateBean(inputboxid);
            if(svb==null||Tools.isEmpty(svb.getLstValidateMethods()))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"上的输入框"+inputboxid+"没有配置失去焦点时进行服务器端校验，无法完成校验操作");
            }
            List<String> lstErrorMessages=new ArrayList<String>();
            StringBuilder errorPromptParamsBuf=new StringBuilder();
            boolean isSuccess=svb.validate(rrequest,boxvalue,mOtherValues,lstErrorMessages,errorPromptParamsBuf);
            resultBuf.append("<WX-SUCCESS-FLAG>").append(isSuccess).append("</WX-SUCCESS-FLAG>");
            if(lstErrorMessages.size()>0)
            {
                resultBuf.append("<WX-ERROR-MESSAGE>");
                for(String errormsgTmp:lstErrorMessages)
                {
                    resultBuf.append(errormsgTmp).append(";");
                }
                if(resultBuf.charAt(resultBuf.length()-1)==';') resultBuf.deleteCharAt(resultBuf.length()-1);
                resultBuf.append("</WX-ERROR-MESSAGE>");
            }
            if(errorPromptParamsBuf.length()>0)
            {
                resultBuf.append("<WX-ERRORPROMPT-PARAMS>");
                resultBuf.append(errorPromptParamsBuf.toString().trim());
                resultBuf.append("</WX-ERRORPROMPT-PARAMS>");
            }
            if(rrequest.getMServerValidateDatas()!=null&&rrequest.getMServerValidateDatas().size()>0)
            {
                resultBuf.append("<WX-SERVER-DATA>{");
                for(Entry<String,String> entryTmp:rrequest.getMServerValidateDatas().entrySet())
                {
                    resultBuf.append(entryTmp.getKey()+":\""+entryTmp.getValue()+"\",");
                }
                if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
                resultBuf.append("}</WX-SERVER-DATA>");
            }
        }catch(Exception e)
        {
            log.error("对输入框"+inputboxid+"进行服务器端校验时失败",e);
            throw new WabacusRuntimeException("对输入框"+inputboxid+"进行服务器端校验时失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
        return resultBuf.toString();        
    }

    public static String getChartDataString(HttpServletRequest request,HttpServletResponse response)
    {
        ReportRequest rrequest=null;
        try
        {
            rrequest=new ReportRequest(request,Consts.DISPLAY_ON_PAGE);
            WabacusResponse wresponse=new WabacusResponse(response);
            rrequest.setWResponse(wresponse);
            rrequest.initGetChartDataString();
            ReportBean rbean=rrequest.getLstAllReportBeans().get(0);
            AbsReportType reportTypeObj=(AbsReportType)rrequest.getComponentTypeObj(rbean,null,true);
            if(!(reportTypeObj instanceof FusionChartsReportType))
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"不是图表报表，不能加载其<chart/>数据");
            }
            ((FusionChartsReportType)reportTypeObj).init();
            ((FusionChartsReportType)reportTypeObj).loadReportData(true);
            return ((FusionChartsReportType)reportTypeObj).loadStringChartData(true);
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载图表报表数据失败",e);
        }finally
        {
            if(rrequest!=null) rrequest.destroy(false);
        }
    }

    public static String getChartDataStringFromLocalFile(HttpServletRequest request,HttpServletResponse response)
    {
        String xmlfile=request.getParameter("xmlfilename");
        if(xmlfile==null) return "";
        String filepath=FusionChartsReportType.chartXmlFileTempPath+File.separator+xmlfile;
        File f=new File(filepath);
        if(!f.exists()||!f.isFile()) return "";
        StringBuffer resultBuf=new StringBuffer();
        BufferedReader br=null;
        try
        {
            br=new BufferedReader(new InputStreamReader(new FileInputStream(f),Config.encode));
            String content=br.readLine();
            while(content!=null)
            {
                resultBuf.append(content);
                content=br.readLine();
            }
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("读取图表数据文件"+xmlfile+"失败",e);
        }finally
        {
            try
            {
                br.close();
            }catch(IOException e)
            {
                log.error("读取图表数据文件"+xmlfile+"时关闭失败",e);
            }
            f.delete();
        }
        return resultBuf.toString();
    }
}
