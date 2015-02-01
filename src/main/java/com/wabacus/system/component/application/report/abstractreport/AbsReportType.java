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
package com.wabacus.system.component.application.report.abstractreport;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.wabacus.config.Config;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.extendconfig.IAfterConfigLoadForReportType;
import com.wabacus.config.component.application.report.extendconfig.IColExtendConfigLoad;
import com.wabacus.config.component.application.report.extendconfig.IConditionExtendConfigLoad;
import com.wabacus.config.component.application.report.extendconfig.IDisplayExtendConfigLoad;
import com.wabacus.config.component.application.report.extendconfig.IReportExtendConfigLoad;
import com.wabacus.config.component.application.report.extendconfig.ISqlExtendConfigLoad;
import com.wabacus.config.dataexport.AbsDataExportBean;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.config.dataexport.PlainExcelExportBean;
import com.wabacus.config.other.ButtonsBean;
import com.wabacus.config.print.PrintSubPageBean;
import com.wabacus.config.print.PrintTemplateElementBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.PdfAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.AbsApplicationType;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSecretColValueBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.inputbox.validate.JavascriptValidateBean;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public abstract class AbsReportType extends AbsApplicationType implements IReportType,IReportExtendConfigLoad,IDisplayExtendConfigLoad,ISqlExtendConfigLoad,
        IColExtendConfigLoad,IConditionExtendConfigLoad,IAfterConfigLoadForReportType
{
    protected ReportBean rbean;

    protected List<AbsReportDataPojo> lstReportData;

    protected EditableReportSecretColValueBean currentSecretColValuesBean;
    
    protected CacheDataBean cacheDataBean;
    
    protected AbsDataExportBean currentDataExportBean;//本次如果是做数据导出，这里存放相应本次导出类型的导出本报表的<dataexport/>配置对象
    
    public AbsReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        this.rbean=(ReportBean)comCfgBean;
        if(rrequest!=null&&comCfgBean!=null)
        {
            this.cacheDataBean=rrequest.getCdb(rbean.getId());
        }
    }
    
    public ReportBean getReportBean()
    {
        return rbean;
    }

    public List<AbsReportDataPojo> getLstReportData()
    {
        return lstReportData;
    }

    public void setLstReportData(List<AbsReportDataPojo> lstReportData)
    {
        this.lstReportData=lstReportData;
    }

    public AbsDataExportBean getCurrentDataExportBean()
    {
        return currentDataExportBean;
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        ReportBean reportbean=(ReportBean)applicationConfigBean;
        initAllOtherParamsInUrl(rrequest,reportbean);
        String navi_reportid=reportbean.getNavigate_reportid();
        String reportid=reportbean.getId();
        if(navi_reportid==null||navi_reportid.trim().equals("")) navi_reportid=reportid;
        if(navi_reportid.equals(reportid))
        {
            rrequest.addParamToUrl(navi_reportid+"_PAGENO","rrequest{"+navi_reportid+"_PAGENO}",true);
            rrequest.addParamToUrl(navi_reportid+"_PAGECOUNT","rrequest{"+navi_reportid+"_PAGECOUNT}",true);
        }
        rrequest.addParamToUrl(reportid+"_RECORDCOUNT","rrequest{"+reportid+"_RECORDCOUNT}",true);
        rrequest.addParamToUrl(reportid+"_PAGESIZE","rrequest{"+reportid+"_PAGESIZE}",true);
        rrequest.addParamToUrl(reportid+"_MAXRECORDCOUNT","rrequest{"+reportid+"_MAXRECORDCOUNT}",true);
        rrequest.addParamToUrl(reportid+"_ALLDATASETS_RECORDCOUNT","rrequest{"+reportid+"_ALLDATASETS_RECORDCOUNT}",true);
        rrequest.addParamToUrl(reportbean.getId()+"_DYNDISPLAY_COLIDS","rrequest{"+reportbean.getId()+"_DYNDISPLAY_COLIDS}",true);
        rrequest.addParamToUrl(reportbean.getId()+"_lazydisplaydata","rrequest{"+reportbean.getId()+"_lazydisplaydata}",true);
        rrequest.addParamToUrl(reportbean.getId()+"_lazydisplaydata_prompt","rrequest{"+reportbean.getId()+"_lazydisplaydata_prompt}",true);
        SqlBean sbean=reportbean.getSbean();
        if(sbean==null) return;
        List<ConditionBean> lstConditions=sbean.getLstConditions();
        if(lstConditions==null) return;
        for(ConditionBean cbean:lstConditions)
        {
            cbean.initConditionValueByInitUrlMethod(rrequest);
        }
    }
    
    public void init()
    {
        wresponse=rrequest.getWResponse();
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            initAllOtherParamsInUrl(rrequest,rbean);//这里调用一次，以最新访问的报表参数值为准
            rrequest.getWResponse().addUpdateReportGuid(rbean.getGuid());
            rrequest.addShouldAddToUrlAttributeName(rbean.getId(),rbean.getId()+"_lazydisplaydata");
            rrequest.addShouldAddToUrlAttributeName(rbean.getId(),rbean.getId()+"_lazydisplaydata_prompt");
        }
        String dynDisplayColIdsAction=rrequest.getStringAttribute(rbean.getId()+"_DYNDISPLAY_COLIDS_ACTION","");
        String dynDisplayColIds="";
        if(dynDisplayColIdsAction.toLowerCase().equals("true"))
        {
            dynDisplayColIds=rrequest.getStringAttribute(rbean.getId()+"_DYNDISPLAY_COLIDS","");
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&rbean.getPersonalizeObj()!=null)
            {
                rbean.getPersonalizeObj().storeColSelectedData(rrequest,rbean,dynDisplayColIds);
            }
        }else
        {
            if(rbean.getPersonalizeObj()!=null)
            {
                dynDisplayColIds=rbean.getPersonalizeObj().loadColSelectedData(rrequest,rbean);
            }
            if(dynDisplayColIds==null||dynDisplayColIds.trim().equals(""))
            {
                dynDisplayColIds=rrequest.getStringAttribute(rbean.getId()+"_DYNDISPLAY_COLIDS","");
            }
        }
        if(!dynDisplayColIds.equals(""))
        {
            this.cacheDataBean.setLstDynDisplayColids(Tools.parseStringToList(dynDisplayColIds,";",false));
        }
        if(rbean.getSbean()!=null)
        {
            rrequest.addShouldAddToUrlAttributeName(rbean.getId(),rbean.getSbean().getLstConditionFromUrlNames());
            rbean.getSbean().initConditionValues(rrequest);
        }
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&rrequest.getShowtype()!=Consts.DISPLAY_ON_PRINT)
        {
            currentDataExportBean=this.getDataExportBeanOfMe();
        }
        initReportBeforeDoStart();//调用在拦截器的doStart()方法调用前的初始化方法
        if(rbean.getInterceptor()!=null)
        {
            rbean.getInterceptor().doStart(this.rrequest,this.rbean);
        }
        initNavigateInfoFromRequest();
        initReportAfterDoStart();
        if(rbean.isSlaveReport())
        {
            String parentReportNoData=rrequest.getStringAttribute(rbean.getId()+"_PARENTREPORT_NODATA","");
            if(parentReportNoData.toLowerCase().equals("true"))
            {//如果父报表没有数据
                rrequest.addParamToUrl(rbean.getId()+"_PARENTREPORT_NODATA","true",true);
                //return;//主报表没有数据，仍要调用下面的loadReportData()方法，比如crosslist、editdetail、form报表类型需要在此方法中做其它处理
            }
        }
        currentSecretColValuesBean=new EditableReportSecretColValueBean();
        if(rrequest.getServerActionBean()!=null) rrequest.getServerActionBean().executeServerAction(this.getReportBean());
        rrequest.releaseDBResources();
    }

    private void initAllOtherParamsInUrl(ReportRequest rrequest,ReportBean reportbean)
    {
        if(reportbean.getSParamNamesFromURL()!=null)
        {
            for(String paramnameTmp:reportbean.getSParamNamesFromURL())
            {
                if(paramnameTmp==null||paramnameTmp.trim().equals("")) continue;
                rrequest.addParamToUrl(paramnameTmp,"rrequest{"+paramnameTmp+"}",true);
            }
        }
    }
    
    protected void initReportBeforeDoStart()
    {}
    
    protected void initReportAfterDoStart()
    {}
    
    private void initNavigateInfoFromRequest()
    {
        String dynpageno=rrequest.getStringAttribute(rbean.getNavigate_reportid()+"_DYNPAGENO","");
        String pageno=rrequest.getStringAttribute(rbean.getNavigate_reportid()+"_PAGENO","1");
        String pagecount=rrequest.getStringAttribute(rbean.getNavigate_reportid()+"_PAGECOUNT","");
        String recordcount=rrequest.getStringAttribute(rbean.getId()+"_RECORDCOUNT","");
        String pagesize=rrequest.getStringAttribute(rbean.getId()+"_PAGESIZE","");
        String prevpagesize=rrequest.getStringAttribute(rbean.getId()+"_PREV_PAGESIZE","");
        if(!pagesize.equals(""))
        {
            int ipagesize=0;
            try
            {
                ipagesize=Integer.parseInt(pagesize);
            }catch(NumberFormatException e)
            {
                ipagesize=rbean.getLstPagesize().get(0);
            }
            cacheDataBean.setPagesize(ipagesize);
        }else
        {
            cacheDataBean.setPagesize(rbean.getLstPagesize().get(0));//没有传入pagesize，则用配置的第一个pagesize
        }
        int iprevpagesize=cacheDataBean.getPagesize();
        if(!prevpagesize.equals(""))
        {
            try
            {
                iprevpagesize=Integer.parseInt(prevpagesize);
            }catch(NumberFormatException e)
            {
                iprevpagesize=cacheDataBean.getPagesize();
            }
            if(iprevpagesize<-1) iprevpagesize=cacheDataBean.getPagesize();
        }
        int idynpageno=-1;
        if(!dynpageno.equals(""))
        {
            try
            {
                idynpageno=Integer.parseInt(dynpageno.trim());
            }catch(NumberFormatException e)
            {
                idynpageno=-1;
            }
        }
        cacheDataBean.setDynpageno(idynpageno);
        int ipagecount=-1;
        if(!pagecount.equals(""))
        {
            try
            {
                ipagecount=Integer.parseInt(pagecount);
            }catch(NumberFormatException e)
            {
                ipagecount=-1;
            }
        }
        if(ipagecount<0)
        {
            cacheDataBean.setRefreshNavigateInfoType(-1);
            cacheDataBean.setPagecount(0);
            cacheDataBean.setRecordcount(0);
            cacheDataBean.setPageno(1);
        }else if(iprevpagesize!=cacheDataBean.getPagesize())
        {
            cacheDataBean.setRefreshNavigateInfoType(0);
            if(recordcount.equals("")) recordcount="0";//recordcount有可能为空，比如在可编辑报表中配置了minrowspan，当没有记录时，其recordcount即为空，但此时有可能显示多个添加记录行。
            cacheDataBean.setRecordcount(Integer.parseInt(recordcount));
            setDataSetRecordcount();
            cacheDataBean.setPageno(1);
            cacheDataBean.setPagecount(0);
        }else
        {
            if(recordcount.equals("")) recordcount="0";
            cacheDataBean.setRefreshNavigateInfoType(1);
            cacheDataBean.setPagecount(ipagecount);
            cacheDataBean.setRecordcount(Integer.parseInt(recordcount));
            setDataSetRecordcount();
            try
            {
                cacheDataBean.setPageno(Integer.parseInt(pageno));
            }catch(NumberFormatException e)
            {
                cacheDataBean.setPageno(1);
            }
        }
        String maxrecordcount=rrequest.getStringAttribute(rbean.getId()+"_MAXRECORDCOUNT","-1");
        try
        {
            cacheDataBean.setMaxrecordcount(Integer.parseInt(maxrecordcount));
        }catch(NumberFormatException e)
        {
            cacheDataBean.setMaxrecordcount(-1);
        }
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
        {
            cacheDataBean.setPrintPagesize(rrequest.getLstComponentBeans().get(0).getPrintBean().getPrintPageSize(rbean.getId()));
        }else if(rrequest.isPdfPrintAction())
        {
            cacheDataBean.setRefreshNavigateInfoType(-1);//强制重新查询记录数及计算页码，因为这里的页大小和显示在页面的页大小可能不同
            this.cacheDataBean.setConfigDataexportRecordcount(rrequest.getLstComponentBeans().get(0).getPdfPrintBean().getDataExportRecordcount(
                    rbean.getId()));
        }else if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
        {
            cacheDataBean.setRefreshNavigateInfoType(-1);
            this.cacheDataBean.setConfigDataexportRecordcount(currentDataExportBean==null?-1:currentDataExportBean.getDataExportRecordcount(rbean.getId()));
        }
        this.cacheDataBean.initLoadReportDataType();
    }
    
    private void setDataSetRecordcount()
    {
        String alldataset_recordcount=rrequest.getStringAttribute(rbean.getId()+"_ALLDATASETS_RECORDCOUNT","");
        if(alldataset_recordcount!=null&&!alldataset_recordcount.trim().equals(""))
        {
            List<String> lstTmp=Tools.parseStringToList(alldataset_recordcount,";",true);
            for(String datasetidAndCountTmp:lstTmp)
            {
                int idxequals=datasetidAndCountTmp.indexOf("=");
                if(idxequals<=0) continue;
                this.cacheDataBean.addRecordcount(datasetidAndCountTmp.substring(0,idxequals).trim(),Integer.parseInt(datasetidAndCountTmp.substring(
                        idxequals+1).trim()));
            }
        }
    }

    private boolean hasInitLoadedData=false;
    
    protected void initLoadReportData()
    {
        if(hasInitLoadedData) return;
        hasInitLoadedData=true;
        if(this.rbean.isSlaveReportDependsonDetailReport()&&rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {//如果当前报表是依赖于细览报表，且当前是显示在页面上
            String paramnameTmp;
            String paramvalueTmp;
            AbsReportType parentReportTypeObj=(AbsReportType)rrequest.getComponentTypeObj(this.rbean.getDependParentId(),null,false);
            if(parentReportTypeObj==null||parentReportTypeObj.getParentContainerType()==null) return;
            if(!parentReportTypeObj.isLoadedReportData()) parentReportTypeObj.loadReportData(true);
            if(isEmptyParentData(parentReportTypeObj.getLstReportData()))
            {
                rrequest.addParamToUrl(rbean.getId()+"_PARENTREPORT_NODATA","true",true);
                rrequest.setAttribute(rbean.getId()+"_PARENTREPORT_NODATA","true");
                return;
            }
            //如果父报表有数据，则清除掉下面两个值
            rrequest.addParamToUrl(rbean.getId()+"_PARENTREPORT_NODATA",null,true);
            rrequest.setAttribute(rbean.getId()+"_PARENTREPORT_NODATA","");
            
            AbsReportDataPojo dataObj=parentReportTypeObj.getLstReportData().get(0);
            for(Entry<String,String> entryTmp:this.rbean.getMDependsDetailReportParams().entrySet())
            {
                paramnameTmp=entryTmp.getKey();
                paramvalueTmp=entryTmp.getValue();
                if(Tools.isDefineKey("@",paramvalueTmp))
                {
                    ColBean cb=parentReportTypeObj.getReportBean().getDbean().getColBeanByColProperty(Tools.getRealKeyByDefine("@",paramvalueTmp));
                    paramvalueTmp=dataObj.getColStringValue(cb);
                }
                if("".equals(paramvalueTmp))
                {
                    rrequest.addParamToUrl(rbean.getId()+"_PARENTREPORT_NODATA","true",true);
                    rrequest.setAttribute(rbean.getId()+"_PARENTREPORT_NODATA","true");
                    return;
                }
                rrequest.addParamToUrl(paramnameTmp,paramvalueTmp,true);
                rrequest.setAttribute(paramnameTmp,paramvalueTmp);//存入rrequest中以便后面取报表数据时能取到做为条件
            }
        }
    }

    private boolean isEmptyParentData(List<AbsReportDataPojo> lstParentData)
    {
        if(lstParentData==null||lstParentData.size()==0) return true;
        if(lstParentData.size()==1)
        {
            if(lstParentData.get(0)==null) return true;
            if("[default_empty_dataobject]".equals(lstParentData.get(0).getWx_belongto_datasetid())) return true;
        }
        return false;
    }
    
    private boolean hasLoadedData=false;
    
    public  boolean isLoadedReportData()
    {
        return this.hasLoadedData;
    }
    
    public void setHasLoadedDataFlag(boolean hasLoadedDataFlag)
    {
        this.hasLoadedData=hasLoadedDataFlag;
    }
    
    public void loadReportData(boolean shouldInvokePostaction)
    {
        if(this.hasLoadedData) return;
        this.hasLoadedData=true;
        initLoadReportData();
        if(rbean.isSlaveReport()&&rrequest.getStringAttribute(rbean.getId()+"_PARENTREPORT_NODATA","").toLowerCase().equals("true"))
        {
            return;
        }
        if(isLazyDisplayData()) return;
        this.lstReportData=ReportAssistant.getInstance().loadReportDataSet(rrequest,this,false);
        if(this.lstReportData==null) this.lstReportData=new ArrayList<AbsReportDataPojo>();
        if(shouldInvokePostaction) doLoadReportDataPostAction();
        rrequest.releaseDBResources();
    }
    
    protected void doLoadReportDataPostAction()
    {
        if(rbean.getInterceptor()!=null)
        {
            this.lstReportData=(List<AbsReportDataPojo>)rbean.getInterceptor().afterLoadData(this.rrequest,this.rbean,this,this.lstReportData);
        }
    }
    
    protected boolean isLazyDisplayData()
    {
        return rrequest.getStringAttribute(rbean.getId()+"_lazydisplaydata","").toLowerCase().trim().equals("true");
    }
    
    public String getRowLabelstyleproperty()
    {
        if(this.cacheDataBean.getDynRowLabelstyleproperty()!=null) return this.cacheDataBean.getDynRowLabelstyleproperty();//如果动态设置了，返回动态设置的
        return rbean.getDbean().getLabelstyleproperty(rrequest,false);
    }
    
    public String getColLabelStyleproperty(ColBean cbean,AbsReportDataPojo rowDataObj)
    {
        String key=cbean.getProperty();
        if(key==null||key.trim().equals("")) return "";
        if("[DYN_COL_DATA]".equals(key)) key=cbean.getColumn();
        if(rowDataObj!=null) return rowDataObj.getColLabelstyleproperty(key);
        String labelstyleproperty=this.cacheDataBean.getDynColLabelstyleproperty(key);
        if(labelstyleproperty==null) labelstyleproperty=cbean.getLabelstyleproperty(rrequest,false);
        return labelstyleproperty==null?"":labelstyleproperty;
    }
    
    protected String showMetaData()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.showMetaData());
        if(resultBuf.toString().trim().equals("")) return "";//父类没有显示元数据，说明本次不需显示
        resultBuf.append(getHiddenSearchBoxDisplayString());
        if(rbean.isSlaveReportDependsonListReport())
        {
            resultBuf.append("<span id=\"").append(this.rbean.getGuid()).append("_url_id\" style=\"display:none;\" value=\"").append(
                    Tools.jsParamEncode(rrequest.getUrl())).append("\"").append("></span>");
        }
        resultBuf.append(getColSelectedMetadata());//显示列选择所需的列信息<span/>
        return resultBuf.toString();
    }
    
    private String getHiddenSearchBoxDisplayString()
    {
        SqlBean sbean=rbean.getSbean();
        List<ConditionBean> lstConditions=sbean.getLstConditions();
        if(Tools.isEmpty(lstConditions)) return "";
        StringBuilder resultBuf=new StringBuilder();
        for(ConditionBean conbeanTmp:lstConditions)
        {
            if(conbeanTmp.isConditionValueFromUrl()&&conbeanTmp.isHidden())
            {
                resultBuf.append("<font id=\"font_").append(rbean.getGuid()+"_conditions\"");
                resultBuf.append(" name=\"font_").append(rbean.getGuid()+"_conditions\"");
                resultBuf.append(" value_name=\"").append(conbeanTmp.getName()).append("\"");
                resultBuf.append(" value=\"").append(rrequest.getStringAttribute(conbeanTmp.getName(),"")).append("\"");
                resultBuf.append(" style=\"display:none\"></font>");
            }
        }
        return resultBuf.toString();
    }
    
    protected String showMetaDataDisplayStringStart()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(" reportid=\"").append(rbean.getId()).append("\"");
        resultBuf.append(" reportfamily=\"").append(this.getReportFamily()).append("\"");
        if(rbean.isSlaveReport())
        {
            if(rbean.isSlaveReportDependsonListReport())
            {
                resultBuf.append(" isSlaveReport=\"true\"");
            }else
            {
                resultBuf.append(" isSlaveDetailReport=\"true\"");
            }
        }
        if(rbean.getMDependChilds()!=null&&rbean.getMDependChilds().size()>0)
        {//有从报表依赖本报表，则记录下所有从报表ID
            StringBuffer childReportidsBuf=new StringBuffer();
            for(String childReportidTmp:rbean.getMDependChilds().keySet())
            {
                childReportidsBuf.append(childReportidTmp).append(";");
            }
            resultBuf.append(" dependingChildReportIds=\"").append(childReportidsBuf.toString()).append("\"");
        }
        if(rbean.isPageSplitReport())
        {
            resultBuf.append(" navigate_reportid=\"").append(rbean.getNavigate_reportid()).append("\"");
        }
        resultBuf.append(showConditionOnGetSetValueMethods());
        StringBuffer relateConditionsReportIdsBuf=new StringBuffer();
        StringBuffer relateConditionReportNavigateIdsBuf=new StringBuffer();
        if(rbean.getSRelateConditionReportids()!=null)
        {//存在查询条件关联的报表
            List<String> lstConditionRelateReportids=new ArrayList<String>(); 
            lstConditionRelateReportids.add(rbean.getId());
            List<String> lstNavigateReportIds=new ArrayList<String>();
            String myNavigateId=rbean.getNavigate_reportid();
            if(myNavigateId==null||myNavigateId.trim().equals("")) myNavigateId=rbean.getId();
            lstNavigateReportIds.add(myNavigateId);
            String navigateIdTmp;
            ReportBean rbTmp;
            for(String rbIdTmp:rbean.getSRelateConditionReportids())
            {
                if(!lstConditionRelateReportids.contains(rbIdTmp))
                {
                    lstConditionRelateReportids.add(rbIdTmp);
                    relateConditionsReportIdsBuf.append(rbIdTmp).append(";");
                }
                rbTmp=(ReportBean)rbean.getPageBean().getChildComponentBean(rbIdTmp,true);
                if(rbTmp==null||!rbTmp.isPageSplitReport()) continue;
                navigateIdTmp=rbTmp.getNavigate_reportid();
                if(navigateIdTmp.equals("")||lstNavigateReportIds.contains(navigateIdTmp)) continue;
                lstNavigateReportIds.add(navigateIdTmp);
                relateConditionReportNavigateIdsBuf.append(navigateIdTmp).append(";");
            }
        }
        if(relateConditionsReportIdsBuf.length()>0)
        {
            if(relateConditionsReportIdsBuf.charAt(relateConditionsReportIdsBuf.length()-1)==';')
            {
                relateConditionsReportIdsBuf.deleteCharAt(relateConditionsReportIdsBuf.length()-1);
            }
            resultBuf.append(" relateConditionReportIds=\"").append(relateConditionsReportIdsBuf.toString()).append("\"");
        }
        if(relateConditionReportNavigateIdsBuf.length()>0)
        {
            if(relateConditionReportNavigateIdsBuf.charAt(relateConditionReportNavigateIdsBuf.length()-1)==';')
            {
                relateConditionReportNavigateIdsBuf.deleteCharAt(relateConditionReportNavigateIdsBuf.length()-1);
            }
            resultBuf.append(" relateConditionReportNavigateIds=\"").append(relateConditionReportNavigateIdsBuf.toString()).append("\"");
        }
        if(rbean.getSbean().getBeforeSearchMethod()!=null&&!rbean.getSbean().getBeforeSearchMethod().trim().equals(""))
        {//配置有搜索客户端回调函数
            resultBuf.append(" beforeSearchMethod=\"{method:").append(rbean.getSbean().getBeforeSearchMethod().trim()).append("}\"");
        }
        if(this.isLazyDisplayData())
        {
            resultBuf.append(" lazydisplaydata=\"true\"");
            String lazydataloadmessage=rrequest.getStringAttribute(rbean.getId()+"_lazydisplaydata_prompt","");
            if(!lazydataloadmessage.equals(""))
            {
                resultBuf.append(" lazydisplaydata_prompt=\""+lazydataloadmessage+"\"");
            }
        }
        if(this.rbean.getMInputboxJsValidateBeans()!=null)
        {
            for(Entry<String,JavascriptValidateBean> entryTmp:this.rbean.getMInputboxJsValidateBeans().entrySet())
            {
                resultBuf.append(entryTmp.getValue().displayValidateInfoOnMetadata(rrequest,entryTmp.getKey()));
            }
        }
        return resultBuf.toString();
    }
    
    private String showConditionOnGetSetValueMethods()
    {
        SqlBean sbean=rbean.getSbean();
        List<ConditionBean> lstConditions=sbean.getLstConditions();
        if(Tools.isEmpty(lstConditions)) return "";
        StringBuilder resultBuf=new StringBuilder();
        for(ConditionBean conbeanTmp:lstConditions)
        {
            if(conbeanTmp.isConditionValueFromUrl())
            {
                if(!Tools.isEmpty(conbeanTmp.getOngetvalueMethod()))
                {
                    resultBuf.append(" "+conbeanTmp.getName()+"_ongetvaluemethods=\"{methods:"+conbeanTmp.getOngetvalueMethod()+"}\"");
                }
                if(!Tools.isEmpty(conbeanTmp.getOnsetvalueMethod()))
                {
                    resultBuf.append(" "+conbeanTmp.getName()+"_onsetvaluemethods=\"{methods:"+conbeanTmp.getOnsetvalueMethod()+"}\"");
                }
            }
        }
        return resultBuf.toString();
    }
    
    protected String showMetaDataContentDisplayString()
    {
        StringBuilder resultBuf=new StringBuilder();
        initInputBox(resultBuf);
        return resultBuf.toString();
    }
    
    protected void initInputBox(StringBuilder resultBuf)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return;
        List<ConditionBean> lstConditions=rbean.getSbean().getLstConditions();
        if(Tools.isEmpty(lstConditions)) return;
        for(ConditionBean conbeanTmp:lstConditions)
        {//初始化所有查询条件输入框，为它们的显示做准备
            if(conbeanTmp.isConditionWithInputbox()&&conbeanTmp.getInputbox()!=null)
            {
                resultBuf.append(conbeanTmp.getInputbox().initDisplay(rrequest));
            }
        }
    }
    
    protected abstract String getColSelectedMetadata();
    
    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(!this.shouldDisplayMe())
        {
            wresponse.println("&nbsp;");
            return;
        }
        if(rbean.isSlaveReportDependsonDetailReport()&&!rbean.shouldDisplaySlaveReportDependsonDetailReport(rrequest))
        {
            wresponse.println("&nbsp;");
            return;
        }
        if(displayTag==null&&rbean.getDynTplPath()!=null&&rbean.getDynTplPath().trim().equals(Consts_Private.REPORT_TEMPLATE_NONE))
        {//如果指定的template为none，且当前不是在别的报表的动态模板中通过<wx:report/>进行显示本报表，则不显示此报表出来
            return;
        }
        String width=null;
        if(displayTag==null||!displayTag.isDisplayByMySelf())
        {//如果当前不是被动态模板显示，或者是被别的报表的动态模板通过<wx:component/>显示（如果是被自己的动态模板显示，则不显示下面部分，因为已经显示过）
            if(this.getParentContainerType()!=null)
            {
                width=this.getParentContainerType().getChildDisplayWidth(rbean);
                if(width==null||width.trim().equals("")) width="100%";
                if(rbean.getTop()!=null&&!rbean.getTop().trim().equals(""))
                {
                    wresponse.println("<table  cellspacing='0' cellpadding='0' width=\""+width+"\" style=\"MARGIN:0;\">");
                    wresponse.println("<tr><td height=\""+rbean.getTop()+"\">&nbsp;</td></tr></table>");
                }
                wresponse.println(getRealHeaderFooterDisplayValue(rbean.getOuterHeaderTplBean(),"outerheader"));
                wresponse.println("<table  cellspacing='0' cellpadding='0' width=\""+width+"\" id=\""+rbean.getGuid()+"\"");
                if(rbean.getHeight()!=null&&!rbean.getHeight().trim().equals(""))
                {
                    wresponse.println(" height=\""+rbean.getHeight()+"\" ");
                }
                wresponse.println("><tr><td valign=\"top\">");
            }
            wresponse.println("<div id=\"WX_CONTENT_"+rbean.getGuid()+"\">");
            wresponse.println("<form method=\"post\" onsubmit=\"return false\" AUTOCOMPLETE=\"off\"  name=\"frm_"+rbean.getGuid()
                    +"\"  style=\"margin:0px\">");
        }
        if((rrequest.getSlaveReportBean()==null&&!rbean.isSlaveReportDependsonListReport())
                ||(rrequest.getSlaveReportBean()!=null&&rbean.isSlaveReportDependsonListReport()&&rrequest.getSlaveReportBean().getId().equals(
                        rbean.getId())))
        {//(当前是在加载不依赖列表报表的报表，而且要判断的报表也没有依赖其它列表报表)或(当前是在加载从报表数据，且要判断的报表是本次要加载的从报表)
            if(rbean.getLstOnloadMethods()!=null&&rbean.getLstOnloadMethods().size()>0)
            {
                rrequest.getWResponse().addOnloadMethod(rbean.getOnloadMethodName(),"",false);
            }
            if(displayTag==null)
            {//如果不是被动态模板中的<wx:report/>调用显示，则根据此模板的实际配置进行显示
                if(rbean.getDynTplPath()!=null&&!rbean.getDynTplPath().trim().equals(""))
                {
                    WabacusAssistant.getInstance().includeDynTpl(rrequest,this,rbean.getDynTplPath().trim());
                }else
                {
                    rbean.getTplBean().printDisplayValue(rrequest,this);
                }
            }else
            {//被动态模板的<wx:report/>调用显示
                if(displayTag.isDisplayByMySelf())
                {//如果被自己的动态模板中的<wx:report/>调用显示
                    Config.getInstance().getDefaultReportTplBean().printDisplayValue(this.rrequest,this);
                    //wresponse.println(Config.getInstance().getDefaultReportTplBean().getDisplayValue(this.rrequest,this));//因为配置了动态模板，则不可能再配置有自己的静态模板，所以用全局默认静态模板
                }else
                {//被其它报表的动态模板的<wx:report/>调用显示
                    if(rbean.getDynTplPath()!=null&&!rbean.getDynTplPath().trim().equals(""))
                    {
                        if(rbean.getDynTplPath().trim().equals(Consts_Private.REPORT_TEMPLATE_NONE))
                        {//如果此报表的template配置为none，则用全局静态模板组织布局
                            Config.getInstance().getDefaultReportTplBean().printDisplayValue(this.rrequest,this);
                        }else
                        {
                            WabacusAssistant.getInstance().includeDynTpl(rrequest,this,rbean.getDynTplPath().trim());
                        }
                    }else
                    {
                        rbean.getTplBean().printDisplayValue(this.rrequest,this);
                    }
                }
            }
            wresponse.println(this.showMetaData());
            currentSecretColValuesBean.storeToSession(rrequest,rbean);//如果此列有授权为不显示或密码列，则将存放了这些数据的此bean存放session中
        }
        if(displayTag==null||!displayTag.isDisplayByMySelf())
        {//如果当前不是被动态模板显示，或者是被别的报表的动态模板通过<wx:report/>显示
            wresponse.println("</form></div>");
            if(this.getParentContainerType()!=null)
            {
                wresponse.println("</td></tr></table>");
                wresponse.println(getRealHeaderFooterDisplayValue(rbean.getOuterFooterTplBean(),"outerfooter"));
                if(rbean.getBottom()!=null&&!rbean.getBottom().trim().equals(""))
                {
                    wresponse.println("<table  cellspacing='0' cellpadding='0' width=\""+width+"\" style=\"MARGIN:0;\">");
                    wresponse.println("<tr><td height=\""+rbean.getBottom()+"\">&nbsp;</td></tr></table>");
                }
            }
        }
        rrequest.releaseDBResources();
    }

    protected String getReportDataWidthOnPage()
    {
        String width=rbean.getWidth();
        if(width!=null&&!width.trim().equals("")&&!width.trim().endsWith("%"))
        {
            return width;
        }
//        return width;
        return "100%";
    }
    
    public void displayOnExportDataFile(Object templateObj,boolean isFirstime)
    {
        if(isFirstime)
        {
            if(!rrequest.checkPermission(rbean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL
                    &&(rrequest.checkPermission(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_RICHEXCEL+"}",Consts.PERMISSION_TYPE_DISABLED)||!rrequest
                            .checkPermission(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_RICHEXCEL+"}",Consts.PERMISSION_TYPE_DISPLAY)))
            {
                return;
            }
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_WORD
                    &&(rrequest.checkPermission(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_WORD+"}",Consts.PERMISSION_TYPE_DISABLED)||!rrequest
                            .checkPermission(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_WORD+"}",Consts.PERMISSION_TYPE_DISPLAY)))
            {
                return;
            }
            String width=rbean.getWidth();
            if(width==null||width.trim().equals("")) width="100%";
            wresponse.println("<table  cellspacing='0' cellpadding='0' width=\""+width+"\">");
            wresponse.println("<tr><td valign=\"top\">");
        }
        if(templateObj instanceof TemplateBean)
        {//静态模板
            ((TemplateBean)templateObj).printDisplayValue(this.rrequest,this);
        }else if(templateObj!=null&&!templateObj.toString().trim().equals(""))
        {
            WabacusAssistant.getInstance().includeDynTpl(rrequest,this,templateObj.toString().trim());
        }else
        {
            Config.getInstance().getDefaultDataExportTplBean().printDisplayValue(this.rrequest,this);
        }
        if(isFirstime)
        {
            wresponse.println("</td></tr></table>");
        }
        rrequest.releaseDBResources();
    }
    
    public String getReportDataInJsonString()
    {
        StringBuffer resultBuf=new StringBuffer();
        if(this.lstReportData==null||this.lstReportData.size()==0) return "";
        List<ReportDataSetBean> lstDatasetBeans=rbean.getSbean().getLstDatasetBeans();
        if(lstDatasetBeans==null||lstDatasetBeans.size()==0) return "";
        List<ColBean> lstAllColBeans=getLstAllRealColBeans();
        if(lstDatasetBeans.size()>1)
        {//如果配置了多个<dataset/>
            List lstProcessedData=new ArrayList();
            resultBuf.append("{");
            for(ReportDataSetBean dsbeanTmp:lstDatasetBeans)
            {
                resultBuf.append("\""+dsbeanTmp.getId()).append("\":");
                resultBuf.append(getOneDatasetReportDataInJson(dsbeanTmp,lstAllColBeans,lstProcessedData,true));
                resultBuf.append(",");
            }
            if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
            resultBuf.append("}");
        }else
        {
            resultBuf.append(getOneDatasetReportDataInJson(lstDatasetBeans.get(0),lstAllColBeans,null,false));
        }
        return resultBuf.toString();
    }
    
    private String getOneDatasetReportDataInJson(ReportDataSetBean dsbean,List<ColBean> lstAllColBeans,List lstProcessedData,boolean hasMultiDataset)
    {
        StringBuffer resultBuf=new StringBuffer();
        String currentDatasetidTmp;
        resultBuf.append("[");
        for(AbsReportDataPojo dataObjTmp:this.lstReportData)
        {
            if(lstProcessedData!=null&&lstProcessedData.contains(dataObjTmp)) continue;//此记录行属于其它<dataset/>，且已经处理过了
            if(hasMultiDataset)
            {//当前配置了多个<dataset/>，因此每个POJO对象中都存放了此记录行所属的<dataset/>的id
                currentDatasetidTmp=dataObjTmp.getWx_belongto_datasetid();
                if(!dsbean.getId().equals(currentDatasetidTmp)) continue;//当前记录不是这个<dataset/>查出来的
            }
            if(lstProcessedData!=null) lstProcessedData.add(dataObjTmp);
            resultBuf.append("{");
            for(ColBean cbeanTmp:lstAllColBeans)
            {
                if(cbeanTmp.isControlCol()||cbeanTmp.getProperty()==null||cbeanTmp.getProperty().trim().equals("")) continue;
                if(dsbean.getDatasetValueBeanOfCbean(cbeanTmp)==null) continue;//当前<dataset/>不查本<col/>的数据
                resultBuf.append("\""+cbeanTmp.getProperty()).append("\":{");
                resultBuf.append("\"label\":\"").append(Tools.jsParamEncode(cbeanTmp.getLabel(rrequest))).append("\",");
                resultBuf.append("\"column\":\"").append(cbeanTmp.getColumn()).append("\",");
                resultBuf.append("\"value\":\"").append(Tools.jsParamEncode(dataObjTmp.getColStringValue(cbeanTmp))).append("\",");
                resultBuf.append("\"hidden\":").append(isHiddenCol(cbeanTmp));
                resultBuf.append("},");
            }
            if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
            resultBuf.append("},");
        }
        if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
        resultBuf.append("]");
        return resultBuf.toString();
    }
    
    protected List<ColBean> getLstAllRealColBeans()
    {
        return rbean.getDbean().getLstCols();
    }
    
    private AbsDataExportBean getDataExportBeanOfMe()
    {
        List<String> lstTmp;
        AbsDataExportBean exportBeanTmp;
        for(IComponentConfigBean ccbeanTmp:rrequest.getLstComponentBeans())
        {
            if(ccbeanTmp.getDataExportsBean()==null) continue;
            lstTmp=ccbeanTmp.getDataExportsBean().getLstIncludeApplicationids(rrequest.getShowtype());
            if(lstTmp==null||!lstTmp.contains(rbean.getId())) continue;
            exportBeanTmp=ccbeanTmp.getDataExportsBean().getDataExportBean(rrequest.getShowtype());
            if(exportBeanTmp!=null) return exportBeanTmp;
        }
        //如果导出本报表的所有组件都没有配置<dataexport/>控制它的导出行为，则用本报表自己的<dataexport/>配置的信息控制
        return rbean.getDataExportsBean()!=null?rbean.getDataExportsBean().getDataExportBean(rrequest.getShowtype()):null;
    }
    
    protected abstract boolean isHiddenCol(ColBean cbean);
    
    protected PlainExcelExportBean pedebean;
    
    protected int sheetsize;//每个sheet显示的记录数
    
    protected Sheet excelSheet=null;
    
    protected int sheetIdx=1;
    
    protected int excelRowIdx=0;
    
    protected int excelGlobalRowIdx=0;
    
    protected int titleRowCount=0;//标题行所占的行数，以便计算总行数时减掉它
    
    public void displayOnPlainExcel(Workbook workbook)
    {
        if(!rrequest.checkPermission(rbean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        if(rrequest.checkPermission(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_PLAINEXCEL+"}",Consts.PERMISSION_TYPE_DISABLED)
                ||!rrequest.checkPermission(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_PLAINEXCEL+"}",Consts.PERMISSION_TYPE_DISPLAY))
            return;
        pedebean=(PlainExcelExportBean)this.currentDataExportBean;
        if(pedebean!=null)
        {
            this.sheetsize=pedebean.getPlainexcelsheetsize();
        }else
        {
            this.sheetsize=Config.getInstance().getPlainexcelSheetsize();
        }
        showReportOnPlainExcel(workbook);
        rrequest.releaseDBResources();
    }
    
    protected void createNewSheet(Workbook workbook,int defaultcolumnwidth)
    {
        String title=rbean.getTitle(rrequest);
        if(sheetIdx>1) title=title+"_"+sheetIdx;
        if(title==null||title.trim().equals("")) title="Sheet_"+(sheetIdx+100);
        sheetIdx++;
        excelSheet=workbook.createSheet(title);
        excelSheet.setDefaultColumnWidth(defaultcolumnwidth);
        excelRowIdx=0;
    }
    
    public abstract void showReportOnPlainExcel(Workbook workbook);

    protected PDFExportBean pdfbean;
    
    protected Document document;
    
    protected float pdfwidth;
    
    protected int pdfpagesize;
    
    protected boolean isFullpagesplit;
    
    protected int pdfrowindex=0;//当前导出到记录编号
    
    protected PdfPTable pdfDataTable;
    
    public ByteArrayOutputStream displayOnPdf()
    {
        if(!rrequest.checkPermission(rbean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY)) return null;
        if(rrequest.checkPermission(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_PDF+"}",Consts.PERMISSION_TYPE_DISABLED)) return null;
        if(rrequest.isPdfPrintAction())
        {
            pdfbean=rbean.getPdfPrintBean();
        }else if(rbean.getDataExportsBean()!=null)
        {//是PDF导出，且配置有PDF的<dataexport/>
            pdfbean=(PDFExportBean)this.currentDataExportBean;
        }
        document=new Document();
        if(pdfbean!=null)
        {//此报表配置了pdf的<dataexport/>
            document.setPageSize(pdfbean.getPdfpagesizeObj());//PDF页的尺寸
            pdfpagesize=pdfbean.getPagesize();
            pdfwidth=pdfbean.getWidth();
            isFullpagesplit=pdfbean.isFullpagesplit();
        }else
        {//如果没有配置pdf的<dataexport/>，则用报表的页大小
            pdfpagesize=rbean.getLstPagesize().get(0);
            isFullpagesplit=true;
        }
        if(pdfwidth<=10f) pdfwidth=535f;
        try
        {
            ByteArrayOutputStream baosResult=new ByteArrayOutputStream();
            PdfWriter.getInstance(document,baosResult);
            document.open();
            boolean flag=true;
            if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
            {//如果配置了拦截器
                flag=pdfbean.getInterceptorObj().beforeDisplayReportWithoutTemplate(document,this);
            }
            if(flag)
            {
                showReportOnPdfWithoutTpl();
                if(pdfDataTable!=null)
                {
                    document.add(pdfDataTable);
                    if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
                    {
                        pdfbean.getInterceptorObj().afterDisplayPdfPageWithoutTemplate(document,this);
                    }
                }
            }
            if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
            {
                pdfbean.getInterceptorObj().afterDisplayReportWithoutTemplate(document,this);
            }
            document.close();
            rrequest.releaseDBResources();
            return baosResult;
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("导出报表"+rbean.getPath()+"到PDF文档中失败",e);
        }
    }
    
    protected void showTitleOnPdf() throws Exception
    {
        PdfPTable tableTitle=new PdfPTable(1);
        tableTitle.setTotalWidth(pdfwidth);
        tableTitle.setLockedWidth(true);//设置表格的宽度固定
        int titlefontsize=0;
        if(this.pdfbean!=null) titlefontsize=this.pdfbean.getTitlefontsize();
        if(titlefontsize<=0) titlefontsize=10;
        Font headFont=new Font(PdfAssistant.getInstance().getBfChinese(),titlefontsize,Font.BOLD);
        PdfPCell cell=new PdfPCell(new Paragraph(rbean.getTitle(rrequest)+"  "+rbean.getSubtitle(rrequest),headFont));
        cell.setColspan(1);
        cell.setBorder(0);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        tableTitle.addCell(cell);
        document.add(tableTitle);
    }
    
    protected abstract void showReportOnPdfWithoutTpl();
    
    protected abstract int getTotalColCount();
    
    protected int totalcolcount=-1;
    
    protected void createNewPdfPage()
    {
        try
        {
            if(this.pdfrowindex==0)
            {
                if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
                {
                    pdfbean.getInterceptorObj().beforeDisplayPdfPageWithoutTemplate(document,this);
                }
                showTitleOnPdf();
            }else
            {
                if(pdfDataTable!=null)
                {
                    document.add(pdfDataTable);//将前面的页加到document中
                    if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
                    {
                        pdfbean.getInterceptorObj().afterDisplayPdfPageWithoutTemplate(document,this);
                    }
                }
                if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
                {
                    pdfbean.getInterceptorObj().beforeDisplayPdfPageWithoutTemplate(document,this);
                }
                document.newPage();
                if(isFullpagesplit) showTitleOnPdf();
            }
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("导出报表"+rbean.getPath()+"数据到PDF时，新建页面失败",e);
        }
        if(totalcolcount<0) totalcolcount=getTotalColCount();
        pdfDataTable=new PdfPTable(totalcolcount);
        pdfDataTable.setTotalWidth(pdfwidth);
    }
    
    private Font dataheadFont=null;
    
    private Font dataFont=null;
    
    protected void addDataHeaderCell(Object configbean,String value,int rowspan,int colspan,int align)
    {
        if(dataheadFont==null)
        {
            int dataheaderfontsize=0;
            if(this.pdfbean!=null) dataheaderfontsize=this.pdfbean.getDataheaderfontsize();
            if(dataheaderfontsize<=0) dataheaderfontsize=6;
            dataheadFont=new Font(PdfAssistant.getInstance().getBfChinese(),dataheaderfontsize,Font.BOLD);//数据标题的样式
        }
        PdfPCell cell=new PdfPCell(new Paragraph(value,dataheadFont));
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
//            cell.setImage(img);
//        cell.addElement(new Paragraph(value+"2222",dataheadFont));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
        {
            pdfbean.getInterceptorObj().displayPerColDataWithoutTemplate(this,configbean,-1,value,cell);
        }
        pdfDataTable.addCell(cell);
    }
    
    protected void addDataCell(Object configbean,String value,int rowspan,int colspan,int align)
    {
        if(dataFont==null)
        {
            int datafontsize=0;
            if(this.pdfbean!=null) datafontsize=this.pdfbean.getDatafontsize();
            if(datafontsize<=0) datafontsize=6;
            dataFont=new Font(PdfAssistant.getInstance().getBfChinese(),datafontsize,Font.NORMAL);
        }
        PdfPCell cell=new PdfPCell(new Paragraph(value,dataFont));
        cell.setColspan(colspan);//设置合并单元格的列数
        cell.setRowspan(rowspan);
//        }catch(Exception e)
        cell.setHorizontalAlignment(align);//设置对齐方式
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if(pdfbean!=null&&pdfbean.getInterceptorObj()!=null)
        {
            pdfbean.getInterceptorObj().displayPerColDataWithoutTemplate(this,configbean,rowspan,value,cell);
        }
        pdfDataTable.addCell(cell);
    }
    
    protected int getPdfCellAlign(String configalign,int defaultalign)
    {
        if(configalign==null||configalign.trim().equals("")) return defaultalign;
        configalign=configalign==null?"":configalign.toLowerCase().trim();
        if(configalign.equals("left")) return Element.ALIGN_LEFT;
        if(configalign.equals("center")) return Element.ALIGN_CENTER;
        if(configalign.equals("right")) return Element.ALIGN_RIGHT;
        return defaultalign;
    }
    
    public void printApplication(List<PrintSubPageBean> lstPrintPagebeans)
    {
        if(!rrequest.checkPermission(rbean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        if(rbean.getPrintwidth()!=null&&!rbean.getPrintwidth().trim().equals(""))
        {
            this.wresponse.println("<div width=\""+rbean.getPrintwidth()+"\">");
        }
        if(lstPrintPagebeans==null||lstPrintPagebeans.size()==0)
        {//没有传入在<print></print>中配置的模板，则用全局默认打印模板
            Config.getInstance().getDefaultReportPrintTplBean().printDisplayValue(rrequest,this);
        }else
        {
            PrintTemplateElementBean ptEleBean;
            for(PrintSubPageBean pspagebeanTmp:lstPrintPagebeans)
            {
                for(Entry<String,PrintTemplateElementBean> entryTmp:pspagebeanTmp.getMPrintElements().entrySet())
                {
                    ptEleBean=entryTmp.getValue();
                    if(ptEleBean.getType()==PrintTemplateElementBean.ELEMENT_TYPE_STATICTPL)
                    {
                        ((TemplateBean)ptEleBean.getValueObj()).printDisplayValue(rrequest,this);
                    }else if(ptEleBean.getType()==PrintTemplateElementBean.ELEMENT_TYPE_DYNTPL)
                    {
                        WabacusAssistant.getInstance().includeDynTpl(rrequest,this,(String)ptEleBean.getValueObj());
                    }
                }
            }
        }
        if(rbean.getPrintwidth()!=null&&!rbean.getPrintwidth().trim().equals(""))
        {
            this.wresponse.println("</div>");
        }
    }
   
    protected int[] getDisplayRowInfo()
    {
        if(lstReportData==null) return new int[]{0,0};
        int startidx=0,displayRowcount=this.lstReportData.size();
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT&&this.cacheDataBean.getPrintPagesize()>0)
        {//当前是在做分页打印
            startidx=this.cacheDataBean.getPrintPageno()*this.cacheDataBean.getPrintPagesize();
            if(startidx>=displayRowcount) return new int[] { 0, 0 };
            displayRowcount=startidx+this.cacheDataBean.getPrintPagesize();
            if(displayRowcount>this.lstReportData.size()) displayRowcount=this.lstReportData.size();
        }else if(rbean.isLazyLoadReportData(rrequest))
        {
            if(this.cacheDataBean.getRecordcount()<=0)
            {
                displayRowcount=0;
            }else if(this.cacheDataBean.isLoadAllReportData())
            {
                displayRowcount=this.cacheDataBean.getRecordcount();
            }else
            {
                displayRowcount=this.cacheDataBean.getPagesize();
            }
        }
        return new int[]{startidx,displayRowcount};
    }
    
    protected String showReportTablePropsForNonOnPage()
    {
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<table border=\"1\"");
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_WORD)
        {
            resultBuf.append(" style=\"");
            if(!rbean.getWidth().trim().equals(""))
            {
                resultBuf.append(" width:"+rbean.getWidth()+";");
            }else
            {
                resultBuf.append("width:100.0%;");
            }
            resultBuf
                    .append("border-collapse:collapse;border:none;mso-border-alt:solid windowtext .25pt;mso-border-insideh:.5pt solid windowtext;mso-border-insidev:.5pt solid windowtext");
            resultBuf.append("\"");
        }else if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
        {
            String printwidth=rbean.getPrintwidth();
            if(printwidth==null||printwidth.trim().equals("")) printwidth="100%";
            resultBuf.append(" width=\"").append(printwidth).append("\"");
            resultBuf.append(" style=\"border-collapse:collapse;border:1px solid #000000;\"");
        }else
        {
            if(rbean.getWidth()!=null&&!rbean.getWidth().trim().equals(""))
            {
                resultBuf.append(" width=\"").append(rbean.getWidth()).append("\"");
            }else
            {
                resultBuf.append(" width=\"100.0%\"");
            }
        }
        return resultBuf.toString();
    }
    
    public String getRealParenttitle()
    {
        String parenttitle=rbean.getParenttitle(rrequest);
        if(parenttitle!=null&&!parenttitle.trim().equals("")) return parenttitle.trim();
        return rbean.getTitle(rrequest);
    }

    public String showSearchBox()
    {
        String searchBoxContent=getSearchBoxContent();//获取搜索栏上的内容
        if(Tools.isEmpty(searchBoxContent)) return "";
        String searchbox="<table border=\"0\" cellspacing=\"2\" cellpadding=\"2\" width=\"100%\">"+searchBoxContent+"</table>";
        String searchbox_outer_style=Config.getInstance().getResources()
                .getString(rrequest,rbean.getPageBean(),Consts.SEARCHBOX_OUTERSTYLE_KEY,false);
        if(searchbox_outer_style!=null&&!searchbox_outer_style.trim().equals(""))
        {
            searchbox=Tools.replaceAll(searchbox_outer_style,"%SEARCHBOX_CONTENT%",searchbox);
        }
        return searchbox;
    }

    private String getSearchBoxContent()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("<tr><td align='left'>");
        SqlBean sbean=rbean.getSbean();
        List<ConditionBean> lstConditions=sbean.getLstConditions();
        boolean hasDisplayConditionInThisRow=false;
        if(lstConditions!=null&&lstConditions.size()>0)
        {
            String conDisplayTmp=null;
            for(ConditionBean conbeanTmp:lstConditions)
            {
                if(conbeanTmp.getIterator()>1)
                {
                    StringBuilder tmpBuf=new StringBuilder();
                    for(int i=0;i<conbeanTmp.getIterator();i++)
                    {
                        conDisplayTmp=conbeanTmp.getDisplayString(rrequest,null,i);
                        if(conDisplayTmp==null||conDisplayTmp.trim().equals("")) break;//因为这种条件是统一授权，因此只要有一个不显示，其它的肯定也不显示
                        tmpBuf.append("<tr><td align='left'>");
                        tmpBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(conbeanTmp.getLeft()));
                        tmpBuf.append(conDisplayTmp);
                        tmpBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(conbeanTmp.getRight()));
                        tmpBuf.append("</td></tr>");
                    }
                    if(!tmpBuf.toString().trim().equals(""))
                    {
                        if(hasDisplayConditionInThisRow)
                        {
                            resultBuf.append("</td></tr>");
                        }
                        resultBuf.append(tmpBuf.toString());
                        //后面的查询条件再新起一行
                        resultBuf.append("<tr><td align='left'>");
                        hasDisplayConditionInThisRow=false;
                    }else if(conbeanTmp.isBr()&&hasDisplayConditionInThisRow)
                    {
                        resultBuf.append("</td></tr><tr><td align='left'>");
                        hasDisplayConditionInThisRow=false;
                    }
                }else
                {
                    conDisplayTmp=conbeanTmp.getDisplayString(rrequest,null,-1);
                    if(conDisplayTmp!=null&&!conDisplayTmp.trim().equals(""))
                    {
                        hasDisplayConditionInThisRow=true;
                        resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(conbeanTmp.getLeft()));
                        resultBuf.append(conDisplayTmp);
                        resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(conbeanTmp.getRight()));//显示右边距
                    }
                    if(conbeanTmp.isBr()&&hasDisplayConditionInThisRow)
                    {
                        resultBuf.append("</td></tr><tr><td align='left'>");
                        hasDisplayConditionInThisRow=false;
                    }
                }
            }
        }
        if(resultBuf.toString().endsWith("<tr><td align='left'>"))
        {
            resultBuf.delete(resultBuf.length()-"<tr><td align='left'>".length(),resultBuf.length());
        }
        if(resultBuf.toString().endsWith("</td></tr>"))
        {//把最后的</td></tr/>去掉，以便按钮与最后一个输入框显示在同一行
            resultBuf.delete(resultBuf.length()-"</td></tr>".length(),resultBuf.length());
        }
        ButtonsBean bbeans=rbean.getButtonsBean();
        if(bbeans!=null)
        {
            String buttonStr=bbeans.showButtons(rrequest,Consts.SEARCH_PART);
            if(!buttonStr.equals(""))
            {
                if(resultBuf.length()==0||resultBuf.toString().endsWith("</td></tr>"))
                {
                    resultBuf.append("<tr><td align='left'>");
                }
                resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(bbeans.getButtonspacing()));
                resultBuf.append(buttonStr);
            }
        }
        if(resultBuf.length()>0&&!resultBuf.toString().endsWith("</td></tr>")) resultBuf.append("</td></tr>");
        return resultBuf.toString();
    }
    
    public String showTitle()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return showTitleInAttachFile();
        String realtitle="";
        String buttonsontitle=showButtonsOnTitleBar();
        if(rrequest.checkPermission(rbean.getId(),Consts.TITLE_PART,null,Consts.PERMISSION_TYPE_DISPLAY))
        {//显示标题栏，则显示标题和副标题
            realtitle=getDisplayRealTitleAndSubTitle();
        }
        if(realtitle.trim().equals("")&&buttonsontitle.trim().equals("")) return "";
        return getTitleDisplayValue(realtitle,buttonsontitle);
    }

    protected String showButtonsOnTitleBar()
    {
        if(rbean.getButtonsBean()==null) return "";
        return rbean.getButtonsBean().showButtons(rrequest,Consts.TITLE_PART);
    }

    protected String showTitleInAttachFile()
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.TITLE_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        StringBuffer resultBuf=new StringBuffer();
        String title="";
        if(rrequest.checkPermission(rbean.getId(),Consts.TITLE_PART,"title",Consts.PERMISSION_TYPE_DISPLAY))
        {
            title=rbean.getTitle(rrequest);
        }
        String subtitle="";
        if(rrequest.checkPermission(rbean.getId(),Consts.TITLE_PART,"subtitle",Consts.PERMISSION_TYPE_DISPLAY))
        {
            subtitle=rbean.getSubtitle(rrequest);
        }
        if(title.trim().equals("")&&subtitle.trim().equals("")) return "";
        String titlealign=rbean.getTitlealign();
        titlealign=titlealign==null?"center":titlealign.trim();
        resultBuf.append("<div align='"+titlealign+"'>");
        if(!title.trim().equals(""))
        {
            resultBuf.append("<font size='3'><b>").append(Tools.htmlEncode(title)).append("</b></font>");
        }
        if(!subtitle.trim().equals(""))
        {
            resultBuf.append("  ").append(Tools.htmlEncode(subtitle));
        }
        resultBuf.append("</div>");
        return resultBuf.toString();
    }

    public String showNavigateBox()
    {
        if(!this.shouldDisplayNavigateBox()) return "";
        Object navigateObj=null;
        if(rbean.getNavigateObj()!=null)
        {
            navigateObj=rbean.getNavigateObj();
        }else
        {
            navigateObj=Config.getInstance().getResources().get(rrequest,rbean.getPageBean(),getDefaultNavigateKey(),true);
        }
        if(navigateObj==null) return "";
        String result=null;
        if(navigateObj instanceof String)
        {
            result=((String)navigateObj).trim();
            if(result.equals("")) return "";
            if(Tools.isDefineKey("i18n",result))
            {
                Object obj=rrequest.getI18NObjectValue(result);
                if(obj==null) return "";
                if(!ComponentConfigLoadManager.isValidNavigateObj(rbean,obj)) return "";
                if(obj instanceof String)
                {
                    result=(String)obj;
                }else
                {
                    result=((TemplateBean)obj).getDisplayValue(rrequest,this);
                }
            }
        }else if(navigateObj instanceof TemplateBean)
        {
            result=((TemplateBean)navigateObj).getDisplayValue(rrequest,this);
        }
        if(result==null) result="";
        return result;
    }

    public boolean shouldDisplayNavigateBox()
    {
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return false;
        if(!rrequest.checkPermission(this.rbean.getId(),Consts.NAVIGATE_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return false;
        if(!rbean.getId().equals(rbean.getNavigate_reportid())) return false;
        if(!rbean.isPageSplitReport()&&this.cacheDataBean.getPagesize()<=0) return false;
        if(this.cacheDataBean.getPagecount()<=0) return false;//从数据库中没有取到数据（注意不能用recordcount==0判断，因为在可编辑列表报表中，recordcount为0时可能还要显示一页或多页的添加的行）
        return true;
    }
    
    protected abstract String getDefaultNavigateKey();

    public boolean isSupportHorizontalDataset(ReportBean reportbean)
    {
        return true;
    }
    
    public String getReportFamily()
    {
        return "";
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        return 0;
    }

    public int beforeColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        return 0;
    }

    public int afterConditionLoading(ConditionBean conbean,List<XmlElementBean> lstEleConditionBeans)
    {
        return 0;
    }

    public int beforeConditionLoading(ConditionBean conbean,List<XmlElementBean> lstEleConditionBeans)
    {
        return 0;
    }

    public int afterSqlLoading(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans)
    {
        return 0;
    }

    public int beforeSqlLoading(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans)
    {
        return 0;
    }

    public int afterReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        return 0;
    }

    public int beforeReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        return 0;
    }

    public int afterButtonsLoading(ButtonsBean buttonsbean,List<XmlElementBean> lstEleButtonsBeans)
    {
        return 0;
    }

    public int beforeButtonsLoading(ButtonsBean buttonsbean,List<XmlElementBean> lstEleButtonsBeans)
    {
        return 0;
    }

    public int afterDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        return 0;
    }

    public int beforeDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        return 0;
    }
    
    protected String getComponentTypeName()
    {
        return "application.report";
    }
    
    public int doPostLoad(ReportBean reportbean)
    {
        if(reportbean.isPageSplitReport()&&reportbean.getNavigateObj()==null)
        {
            Object navigateObj=Config.getInstance().getResources().get(null,reportbean.getPageBean(),this.getDefaultNavigateKey(),true);
            if(!ComponentConfigLoadManager.isValidNavigateObj(reportbean,navigateObj))
            {
                throw new WabacusConfigLoadingException("KEY为"+this.getDefaultNavigateKey()+"的翻页导航栏配置不合法");
            }
        }
        DisplayBean dbean=reportbean.getDbean();
        if(dbean!=null)
        {
            List<ColBean> lstColBeans=dbean.getLstCols();
            List<String> lstProperties=new ArrayList<String>();
            if(lstColBeans!=null&&lstColBeans.size()>0)
            {
                boolean isPageAllAlwayOrNeverCol=true,isDataexportAllAlwayOrNeverCol=true;//是否全部是displaytype为always或never的<col/>
                for(ColBean cbean:lstColBeans)
                {
                    if(Consts.COL_DISPLAYTYPE_INITIAL.equals(cbean.getDisplaytype(true))||Consts.COL_DISPLAYTYPE_OPTIONAL.equals(cbean.getDisplaytype(true)))
                    {
                        isPageAllAlwayOrNeverCol=false;
                    }
                    if(Consts.COL_DISPLAYTYPE_INITIAL.equals(cbean.getDisplaytype(false))||Consts.COL_DISPLAYTYPE_OPTIONAL.equals(cbean.getDisplaytype(false)))
                    {
                        isDataexportAllAlwayOrNeverCol=false;
                    }
                    if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) continue;
                    if(cbean.isNonValueCol()||cbean.isSequenceCol()||cbean.isControlCol()) continue;
                    if(lstProperties.contains(cbean.getProperty()))
                    {
                        throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"中有多个<col/>的property属性值为"+cbean.getProperty());
                    }
                    lstProperties.add(cbean.getProperty());
                }
                if(isPageAllAlwayOrNeverCol) dbean.setPageColselect(false);
                if(isDataexportAllAlwayOrNeverCol) dbean.setDataexportColselect(false);
            }
        }
        return 1;
    }

    public int doPostLoadFinally(ReportBean reportbean)
    {
        return 1;
    }
    
}
