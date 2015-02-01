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
package com.wabacus.system.assistant;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.AcroFields.Item;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class PdfAssistant
{
    private final static PdfAssistant instance=new PdfAssistant();
    
    private final static Log log=LogFactory.getLog(PdfAssistant.class);
    
    private BaseFont bfChinese=null;
    
    private PdfAssistant()
    {
        try
        {
            bfChinese=BaseFont.createFont("STSong-Light","UniGB-UCS2-H",BaseFont.NOT_EMBEDDED);
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("获取PDF中文字体失败",e);
        }
    }
    
    public static PdfAssistant getInstance()
    {
        return instance;
    }
    
    public BaseFont getBfChinese()
    {
        return bfChinese;
    }

    public ByteArrayOutputStream showReportDataOnPdfWithTpl(ReportRequest rrequest,IComponentConfigBean ccbean)
    {
        boolean ispdfprint=rrequest.isPdfPrintAction();
        PDFExportBean pdfbean=null;
        if(ispdfprint)
        {
            pdfbean=ccbean.getPdfPrintBean();
        }else if(ccbean.getDataExportsBean()!=null)
        {//是PDF导出，且配置了PDF的<dataexport/>
            pdfbean=(PDFExportBean)ccbean.getDataExportsBean().getDataExportBean(Consts.DATAEXPORT_PDF);
        }
        if(pdfbean==null||pdfbean.getPdftemplate()==null||pdfbean.getPdftemplate().trim().equals("")) return null;
        if(pdfbean.getLstIncludeApplicationids()==null||pdfbean.getLstIncludeApplicationids().size()==0) return null;
        Map<String,AbsReportType> mReportTypeObjs=new HashMap<String,AbsReportType>();
        IComponentType cctypeObj;
        AbsReportType reportTypeObjTmp;
        int maxrowcount=0;
        for(String appidTmp:pdfbean.getLstIncludeApplicationids())
        {//取到本组件导出PDF时所需的所有报表类型对象
            if(mReportTypeObjs.containsKey(appidTmp)) continue;
            cctypeObj=rrequest.getComponentTypeObj(appidTmp,null,false);
            if(cctypeObj==null||!(cctypeObj instanceof AbsReportType)) continue;
            reportTypeObjTmp=(AbsReportType)cctypeObj;
            mReportTypeObjs.put(appidTmp,reportTypeObjTmp);
            if(reportTypeObjTmp.getLstReportData()!=null&&reportTypeObjTmp.getLstReportData().size()>maxrowcount)
            {
                maxrowcount=reportTypeObjTmp.getLstReportData().size();
            }
        }
        if(mReportTypeObjs.size()==0||maxrowcount==0) return null;
        try
        {
            Document document=new Document();
            ByteArrayOutputStream baosResult=new ByteArrayOutputStream();
            PdfCopy pdfCopy=new PdfCopy(document,baosResult);
            document.open();
            for(int i=0;i<maxrowcount;i++)
            {
                addPdfPageToDocument(pdfCopy,showReportOneRowDataOnPdf(rrequest,mReportTypeObjs,ccbean,pdfbean,i));
            }
            document.close();
            return baosResult;
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("导出"+ccbean.getPath()+"数据到pdf文件失败",e);
        }
    }
    
    
    private ByteArrayOutputStream showReportOneRowDataOnPdf(ReportRequest rrequest,Map<String,AbsReportType> mReportTypeObjs,
            IComponentConfigBean ccbean,PDFExportBean pdfbean,int rowidx) throws Exception
    {
        PdfReader pdfTplReader=null;
        InputStream istream=null;
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        try
        {
            istream=WabacusAssistant.getInstance().readFile(pdfbean.getPdftemplate());
            if(istream==null) return null;
            pdfTplReader=new PdfReader(new BufferedInputStream(istream));
            PdfStamper stamp=new PdfStamper(pdfTplReader,baos);
            /* 取出报表模板中的所有字段 */
            AcroFields form=stamp.getAcroFields();
            form.addSubstitutionFont(bfChinese);
            boolean flag=true;
            if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
            {
                flag=pdfbean.getInterceptorObj().beforeDisplayPdfPageWithTemplate(ccbean,mReportTypeObjs,rowidx,stamp);
            }
            if(flag)
            {
                Map<String,Item> mFields=form.getFields();
                if(mFields!=null)
                {
                    String fieldValueTmp=null;
                    for(String fieldNameTmp:mFields.keySet())
                    {
                        fieldValueTmp=getPdfFieldValueByName(rrequest,mReportTypeObjs,ccbean,fieldNameTmp,rowidx);//取出此域的真正内容
                        if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
                        {
                            fieldValueTmp=pdfbean.getInterceptorObj().beforeDisplayFieldWithTemplate(ccbean,mReportTypeObjs,rowidx,stamp,
                                    fieldNameTmp,fieldValueTmp);
                        }
                        if(fieldValueTmp!=null) form.setField(fieldNameTmp,fieldValueTmp);
                    }
                }
            }
            if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
            {
                pdfbean.getInterceptorObj().afterDisplayPdfPageWithTemplate(ccbean,mReportTypeObjs,rowidx,stamp);
            }
            stamp.setFormFlattening(true);
            stamp.close();
        }finally
        {
            try
            {
                if(istream!=null) istream.close();
            }catch(IOException e)
            {
                e.printStackTrace();
            }
            if(pdfTplReader!=null) pdfTplReader.close();
        }
        return baos;
    }
    
    private String getPdfFieldValueByName(ReportRequest rrequest,Map<String,AbsReportType> mReportTypeObjs,IComponentConfigBean ccbean,
            String fieldname,int rowidx)
    {
        if(fieldname==null||fieldname.trim().equals("")) return null;
        if(WabacusAssistant.getInstance().isGetRequestContextValue(fieldname))
        {//从request/session中取数据
            return WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,fieldname,"");
        }
        if(fieldname.indexOf(".")<=0) return null;
        List<String> lstParts=Tools.parseStringToList(fieldname,".",false);
        if(lstParts.size()==1) return null;
        String reportid=lstParts.get(0);
        AbsReportDataPojo dataObjTmp=null;
        if(reportid.equals("this"))
        {
            if(!(ccbean instanceof ReportBean))
            {
                throw new WabacusRuntimeException("导出组件"+ccbean.getPath()+"到PDF失败，此组件不是报表，因此不能在其PDF模板中使用this");
            }
            reportid=ccbean.getId();
        }
        AbsReportType reportTypeObjTmp=mReportTypeObjs.get(reportid);
        if(reportTypeObjTmp==null)
        {
            throw new WabacusRuntimeException("导出组件"+ccbean.getPath()+"到PDF文件失败，在其PDF模板中指定的ID为"+reportid+"的报表没有出现在其<dataexport/>的include属性中");
        }
        if(reportTypeObjTmp.getLstReportData()!=null&&reportTypeObjTmp.getLstReportData().size()>rowidx)
        {
            dataObjTmp=reportTypeObjTmp.getLstReportData().get(rowidx);
        }
        String part=lstParts.get(1);
        if(part==null||part.trim().equals("")) return null;
        if(part.equals("title"))
        {
            if(!rrequest.checkPermission(reportid,Consts.TITLE_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return null;//如果本报表不显示标题
            if(lstParts.size()==2||!"subtitle".equals(lstParts.get(2)))
            {
                return reportTypeObjTmp.getReportBean().getTitle(rrequest);
            }else
            {
                return reportTypeObjTmp.getReportBean().getSubtitle(rrequest);
            }
        }else if(part.equals("data"))
        {
            if(lstParts.size()==2)
            {
                log.warn("导出组件"+ccbean.getPath()+"到PDF文件失败，在其PDF模板中指定的"+fieldname+"不合法");
                return null;
            }
            String colproperty=lstParts.get(2);
            if(colproperty==null||colproperty.trim().equals(""))
            {
                log.warn("导出组件"+ccbean.getPath()+"到PDF文件失败，在其PDF模板中指定的"+fieldname+"不合法");
                return null;
            }
            ColBean cbean=reportTypeObjTmp.getReportBean().getDbean().getColBeanByColProperty(colproperty);
            if(cbean==null)
            {
                throw new WabacusRuntimeException("导出报表"+reportTypeObjTmp.getReportBean().getPath()+"到PDF文件失败，此报表不存在property为"+colproperty+"的<col/>");
            }
            if(rrequest.getCdb(reportid).getColDisplayModeAfterAuthorize(cbean,false)<=0) return null;
            ColDisplayData colDisplayData=null;
            if(lstParts.size()==3||!"label".equals(lstParts.get(3)))
            {//显示此列的数据部分
                String col_displayvalue=dataObjTmp.getColStringValue(cbean);
                colDisplayData=ColDisplayData.getColDataFromInterceptor(reportTypeObjTmp,cbean,dataObjTmp,rowidx,null,col_displayvalue);
            }else
            {
                colDisplayData=ColDisplayData.getColDataFromInterceptor(reportTypeObjTmp,cbean,null,-1,null,cbean.getLabel(rrequest));
            }
            return colDisplayData.getValue();
        }else
        {
            log.warn("导出组件"+ccbean.getPath()+"到PDF文件失败，在其PDF模板中指定的"+fieldname+"不合法");
            return null;
        }
    }
    
    public void addPdfPageToDocument(PdfCopy pdfCopy,ByteArrayOutputStream baos)
    {
        if(baos==null) return;
        PdfReader readerTmp=null;
        try
        {
            readerTmp=new PdfReader(baos.toByteArray());
            for(int i=1,len=readerTmp.getNumberOfPages();i<=len;i++)
            {
                pdfCopy.addPage(pdfCopy.getImportedPage(readerTmp,i));
            }
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("写PDF文件失败",e);
        }finally
        {
            try
            {
                if(baos!=null) baos.close();
            }catch(IOException e)
            {
                e.printStackTrace();
            }
            readerTmp.close();
        }
    }
}

