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
package com.wabacus.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.dataexport.AbsDataExportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportFilterBean;
import com.wabacus.system.component.application.report.configbean.ListReportColPositionBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportDeleteDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportInsertDataBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class CacheDataBean
{
    private int pagecount=0;

    private int recordcount=0;
    
    private Map<String,Integer> mRecordcountOfDatasets;

    private int maxrecordcount=-1;
    
    private int dynpageno;//动态指定的页码，它的优先级比pageno高，用户可以在拦截器或者访问URL中指定
    
    private int pageno=1;

    private int pagesize=10;
    
    private int printPageno;
    
    private int printPagesize;
    
    private int printPagecount;//本报表打印的总页数
    
    private int printRecordcount;
    
    private int configDataexportRecordcount;
    
    private boolean isLoadAllReportData;
    
    private int refreshNavigateInfoType;

    private AbsListReportFilterBean filteredBean;//本次显示时被点击的过滤列（不包括与查询条件关联的过滤列）

    private List<ColBean> lstDynOrderColBeans;
    
    private List<String> lstDynOrderColids;
    
    private List<String> lstDynDisplayColids;
    
    private ListReportColPositionBean colPositionBean;
    
    private Map<String,ColBean> mHdsColBeans;//对于横向数据集，用于存放所有动态生成的colbean，以生成列所在标题列的value（也就是各<col/>的column值）为key
    
    /** ************************************* */

    //    private Map<String, String> mSearchValue = null;//用于缓存用户通过页面输入的查询条件，以便显示输入框时，能将用户刚才的输入显示出来
    //    private Map<String, String> mHiddenSearchValue = null;//存放隐藏条件的值
    private Map attributes=new HashMap();

    private ReportBean reportBean;

    private Map<String,List<Map<String,String>>> mEditedData;//存放所有增、删、改数据列表，每种操作类型的数据对应一个List，List中每个Map存放一条修改的记录。
    
    private Map<String,List<Map<String,String>>> mParamValuesForEditedData;
    
    private ReportRequest rrequest;
    
    private List<String> lstDynRowLabelStyleproperties;
    
    private Map<String,String> mDynRowValueStyleproperties;
    
    private Map<String,String> mDynColLabelStyleproperties;
    
    private Map<String,String> mDynColValueStyleproperties;
    
    public CacheDataBean(ReportRequest rrequest)
    {
        this.rrequest=rrequest;
    }
    
    public boolean isExportPrintPartData()
    {
        return false;
    }
    
    public int getPagecount()
    {
        if(!this.reportBean.getId().equals(this.reportBean.getNavigate_reportid()))
        {
            return rrequest.getCdb(this.reportBean.getNavigate_reportid()).getPagecount();
        }
        return pagecount;
    }

    public void setPagecount(int pagecount)
    {
        if(!this.reportBean.getId().equals(this.reportBean.getNavigate_reportid()))
        {
            if(this.reportBean.getNavigate_reportid()!=null&&!this.reportBean.getNavigate_reportid().trim().equals(""))
            {
                rrequest.getCdb(this.reportBean.getNavigate_reportid()).setPagecount(pagecount);
            }
        }else
        {
            if(pagecount>0)
            {
                rrequest.addParamToUrl(this.reportBean.getNavigate_reportid()+"_PAGECOUNT",String.valueOf(pagecount),true);
            }else
            {
                rrequest.addParamToUrl(this.reportBean.getNavigate_reportid()+"_PAGECOUNT",null,true);
            }
        }
        this.pagecount=pagecount;
    }

    public int getRecordcount()
    {
        return recordcount;
    }

    public void setRecordcount(int recordcount)
    {
        if(this.maxrecordcount>0&&maxrecordcount<recordcount) recordcount=maxrecordcount;
        if(recordcount>0)
        {
            rrequest.addParamToUrl(reportBean.getId()+"_RECORDCOUNT",String.valueOf(recordcount),true);
        }else
        {
            rrequest.addParamToUrl(reportBean.getId()+"_RECORDCOUNT",null,true);
        }
        this.recordcount=recordcount;
    }
    
    public void addRecordcount(String datasetid,int recordcount)
    {
        if(datasetid==null||datasetid.trim().equals("")) return;//没有数据集ID，则说明只有一个数据集
        if(this.maxrecordcount>0&&maxrecordcount<recordcount) recordcount=maxrecordcount;
        if(this.mRecordcountOfDatasets==null) this.mRecordcountOfDatasets=new HashMap<String,Integer>();
        this.mRecordcountOfDatasets.put(datasetid,recordcount);
        StringBuffer allRecordcountsBuf=new StringBuffer();
        for(Entry<String,Integer> entryTmp:this.mRecordcountOfDatasets.entrySet())
        {
            allRecordcountsBuf.append(entryTmp.getKey()+"="+entryTmp.getValue().intValue()).append(";");
        }
        rrequest.addParamToUrl(reportBean.getId()+"_ALLDATASETS_RECORDCOUNT",allRecordcountsBuf.toString(),true);
    }
    
    public int getRecordcountOfDataset(String datasetid)
    {
        if(datasetid==null||datasetid.trim().equals("")) return this.recordcount;
        if(this.mRecordcountOfDatasets==null||this.mRecordcountOfDatasets.get(datasetid)==null) return 0;
        return this.mRecordcountOfDatasets.get(datasetid).intValue();
    }
    
    public void setDynpageno(int dynpageno)
    {
        this.dynpageno=dynpageno;
        if(!this.reportBean.getId().equals(this.reportBean.getNavigate_reportid())&&this.reportBean.getNavigate_reportid()!=null
                &&!this.reportBean.getNavigate_reportid().trim().equals(""))
        {
            rrequest.getCdb(this.reportBean.getNavigate_reportid()).setDynpageno(dynpageno);
        }else
        {
            addPagenoToUrl();
        }
    }
    
    public void setPageno(int pageno)
    {
        this.pageno=pageno;
        if(!this.reportBean.getId().equals(this.reportBean.getNavigate_reportid()))
        {
            rrequest.getCdb(this.reportBean.getNavigate_reportid()).setPageno(pageno);
        }else
        {
            addPagenoToUrl();
        }
    }

    private void addPagenoToUrl()
    {
        int realpageno=this.getRealPageno();
        if(realpageno>0)
        {
            rrequest.addParamToUrl(this.reportBean.getNavigate_reportid()+"_PAGENO",String.valueOf(realpageno),true);
        }else
        {
            rrequest.addParamToUrl(this.reportBean.getNavigate_reportid()+"_PAGENO",null,true);
        }
    }
    
    public int getRealPageno()
    {
        if(!this.reportBean.getId().equals(this.reportBean.getNavigate_reportid()))
        {
            return rrequest.getCdb(this.reportBean.getNavigate_reportid()).getRealPageno();
        }
        if(this.dynpageno>0) return this.dynpageno;
        return pageno;
    }

    
    public int getFinalPageno()
    {
        if(this.pagesize<0) return 1;
        if(!this.reportBean.getId().equals(this.reportBean.getNavigate_reportid()))
        {
            return rrequest.getCdb(this.reportBean.getNavigate_reportid()).getFinalPageno();
        }
        if(this.pagecount<=0) return 1;
        int realpageno=this.pageno;
        if(this.dynpageno>0) realpageno=this.dynpageno;//传入了动态要显示的页码
        if(realpageno>this.pagecount) realpageno=this.pagecount;
        return realpageno<=0?1:realpageno;
    }
    
    public int getPagesize()
    {
        return pagesize;
    }

//        int myRecordcount=this.getRecordcountOfDataset(datasetid);
//        {//如果显示多于一页，且当前是显示最后一页（最后一页显示记录数可能小于pagesize）（如果myRecordcount%pagesize==0说明最后一页也是显示pagesize条记录）
//        }
    
    public void setPagesize(int pagesize)
    {
        if(pagesize<0&&pagesize!=-1||this.reportBean.isListReportType()&&pagesize==0)
        {
            pagesize=this.reportBean.getLstPagesize().get(0);
        }
        this.pagesize=pagesize;
    }
    
    public int getRefreshNavigateInfoType()
    {
        if(!this.reportBean.getId().equals(this.reportBean.getNavigate_reportid()))
        {
            return rrequest.getCdb(this.reportBean.getNavigate_reportid()).getRefreshNavigateInfoType();
        }
        return this.refreshNavigateInfoType;
    }

    public int getMaxrecordcount()
    {
        return maxrecordcount;
    }

    public void setMaxrecordcount(int maxrecordcount)
    {
        this.maxrecordcount=maxrecordcount;
    }

    public void setRefreshNavigateInfoType(int refreshNavigateInfoType)
    {
        if(refreshNavigateInfoType<-1||refreshNavigateInfoType>1) refreshNavigateInfoType=-1;
        if(!this.reportBean.getId().equals(this.reportBean.getNavigate_reportid()))
        {
            rrequest.getCdb(this.reportBean.getNavigate_reportid()).setRefreshNavigateInfoType(
                    refreshNavigateInfoType);
        }
        this.refreshNavigateInfoType=refreshNavigateInfoType;
    }

    public List<ColBean> getLstDynOrderColBeans()
    {
        return lstDynOrderColBeans;
    }

    public void setLstDynOrderColBeans(List<ColBean> lstDynColBeans)
    {
        this.lstDynOrderColBeans=lstDynColBeans;
        if(lstDynColBeans==null||lstDynColBeans.size()==0)
        {
            this.mHdsColBeans=null;
        }else
        {
            if(this.mHdsColBeans==null)
            {
                this.mHdsColBeans=new HashMap<String,ColBean>();
            }else
            {
                this.mHdsColBeans.clear();
            }
            for(ColBean cbTmp:this.lstDynOrderColBeans)
            {
                this.mHdsColBeans.put(cbTmp.getColumn(),cbTmp);
            }
        }
    }

    public Map<String,ColBean> getMHdsColBeans()
    {
        return mHdsColBeans;
    }

    public ColBean getHdsColBeanByColumn(String column)
    {
        if(this.mHdsColBeans==null) return null;
        return this.mHdsColBeans.get(column);
    }
    
    public ColBean getDynamicColBeanByColumn(String dyncolumn)
    {
        if(this.lstDynOrderColBeans==null||dyncolumn==null) return null;
        for(ColBean cbTmp:this.lstDynOrderColBeans)
        {
            if("[DYN_COL_DATA]".equals(cbTmp.getProperty())&&dyncolumn.equals(cbTmp.getColumn())) return cbTmp;
        }
        return null;
    }
    
    public List<String> getLstDynOrderColids()
    {
        if(lstDynOrderColids==null||lstDynOrderColids.size()==0)
        {
            if(lstDynOrderColBeans==null||lstDynOrderColBeans.size()==0) return null;
            lstDynOrderColids=new ArrayList<String>();
            for(ColBean cbTmp:lstDynOrderColBeans)
            {
                lstDynOrderColids.add(cbTmp.getColid());
            }
        }
        return lstDynOrderColids;
    }
    public List<String> getLstDynDisplayColids()
    {
        return lstDynDisplayColids;
    }

    public void setLstDynDisplayColids(List<String> dynDisplayColids)
    {
        this.lstDynDisplayColids=dynDisplayColids;
    }

    public int getPrintPageno()
    {
        return printPageno;
    }

    public void setPrintPageno(int printPageno)
    {
        this.printPageno=printPageno;
    }

    public int getPrintPagesize()
    {
        return printPagesize;
    }

    public void setPrintPagesize(int printPagesize)
    {
        this.printPagesize=printPagesize;
    }

    public int getPrintPagecount()
    {
        return printPagecount;
    }

    public void setPrintPagecount(int printPagecount)
    {
        this.printPagecount=printPagecount;
    }

    public int getPrintRecordcount()
    {
        return printRecordcount;
    }

    public void setPrintRecordcount(int printRecordcount)
    {
        this.printRecordcount=printRecordcount;
    }

    public void setConfigDataexportRecordcount(int configDataexportRecordcount)
    {
        this.configDataexportRecordcount=configDataexportRecordcount;
    }

    public void setStartRownumOfDsbean(ReportDataSetBean dsbean,int startrownum)
    {
       this.attributes.put(dsbean.getId()+"_START_ROWNUM",startrownum);
    }
    
    public void setEndRownumOfDsbean(ReportDataSetBean dsbean,int endrownum)
    {
        this.attributes.put(dsbean.getId()+"_END_ROWNUM",endrownum);
    }
    
    public void setStartRownumOfDsvbean(ReportDataSetValueBean dsvbean,int startrownum)
    {
        if(this.isLoadAllReportData) return;
        this.attributes.put(dsvbean.getGuid()+"_START_ROWNUM",startrownum);
    }
    
    public void setEndRownumOfDsvbean(ReportDataSetValueBean dsvbean,int endrownum)
    {
        if(this.isLoadAllReportData) return;
        this.attributes.put(dsvbean.getGuid()+"_END_ROWNUM",endrownum);
    }
    
    public Integer getStartRownumOfDsbean(ReportDataSetBean dsbean)
    {
        return (Integer)this.attributes.get(dsbean.getId()+"_START_ROWNUM");
    }
    
    public Integer getEndRownumOfDsbean(ReportDataSetBean dsbean)
    {
        return (Integer)this.attributes.get(dsbean.getId()+"_END_ROWNUM");
    }
    
    public Integer getStartRownumOfDsvbean(ReportDataSetValueBean dsvbean)
    {
        if(this.isLoadAllReportData) return null;
        return (Integer)this.attributes.get(dsvbean.getGuid()+"_START_ROWNUM");
    }
    
    public Integer getEndRownumOfDsvbean(ReportDataSetValueBean dsvbean)
    {
        if(this.isLoadAllReportData) return null;
        return (Integer)this.attributes.get(dsvbean.getGuid()+"_END_ROWNUM");
    }
    
    public ReportBean getReportBean()
    {
        return reportBean;
    }

    public void setReportBean(ReportBean reportBean)
    {
        this.reportBean=reportBean;
    }

    public String getFirstColId()
    {
        if(colPositionBean==null)
        {
            initColPosition();
        }
        return colPositionBean.getFirstColid();
    }
    
    public String getLastColId()
    {
        if(colPositionBean==null)
        {
            initColPosition();
        }
        return colPositionBean.getLastColid();
    }
    
    public int getTotalColCount()
    {
        if(colPositionBean==null)
        {
            initColPosition();
        }
        return colPositionBean.getTotalColCount();
    }
    
    private void initColPosition()
    {
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)reportBean.getDbean()
                .getExtendConfigDataForReportType(AbsListReportDisplayBean.class);
        if(alrdbean==null)
        {
            throw new WabacusRuntimeException("报表"+reportBean.getPath()+"不是数据自动列表报表，不能获取其列位置信息");
        }
        if((this.lstDynDisplayColids==null||this.lstDynDisplayColids.size()==0)
                &&(lstDynOrderColBeans==null||lstDynOrderColBeans.size()==0)
                &&(attributes.get("authroize_col_display")==null||!String.valueOf(attributes.get("authroize_col_display")).trim().equals("false")))
        {//没有从客户端动态选取要显示的列，也没有在客户端进行列拖动操作
            this.colPositionBean=rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE?alrdbean.getDefaultPageColPositionBean():alrdbean.getDefaultDataExportColPositionBean();
        }else
        {
            List<ColBean> lstCols=this.lstDynOrderColBeans;
            if(lstCols==null||lstCols.size()==0)
            {
                lstCols=reportBean.getDbean().getLstCols();
            }
            this.colPositionBean=ListReportAssistant.getInstance().calColPosition(rrequest,alrdbean,lstCols,this.lstDynDisplayColids,rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE);
        }
    }
    private List<Map<String,String>> lstRowSelectData;
    
    public List<Map<String,String>> getLstRowSelectData()
    {
        return lstRowSelectData;
    }

    public void initLoadReportDataType()
    {
        if(this.reportBean.isChartReportType()||this.reportBean.getSbean().isHorizontalDataset())
        {
            this.isLoadAllReportData=true;
        }else if(this.reportBean.isDetailReportType()&&this.pagesize==0)
        {
            this.maxrecordcount=1;
            this.isLoadAllReportData=true;
        }else if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            this.isLoadAllReportData=this.pagesize<=0;
        }else if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
        {
            if(this.printPagesize==0&&this.pagesize<=0)
            {
                this.isLoadAllReportData=true;
            }else
            {
                this.isLoadAllReportData=this.printPagesize!=0;//不是打印当前页，则说明打印所有页
            }
        }else
        {
            AbsDataExportBean dataExportBean=reportBean.getDataExportsBean()!=null?reportBean.getDataExportsBean().getDataExportBean(
                    rrequest.getShowtype()):null;
            if(dataExportBean!=null&&dataExportBean.getRowSelectDataBean()!=null)
            {
                lstRowSelectData=EditableReportAssistant.getInstance().parseSaveDataStringToList(
                        rrequest.getStringAttribute(this.getReportBean().getGuid()+"_allselectedatas"));
            }
            if(!Tools.isEmpty(lstRowSelectData))
            {
                this.isLoadAllReportData=true;
                this.maxrecordcount=-1;
            }else if(this.configDataexportRecordcount==0)
            {
                this.isLoadAllReportData=this.pagesize<=0;
            }else
            {//导出所有记录
                this.isLoadAllReportData=true;
                if(this.configDataexportRecordcount>0) this.maxrecordcount=this.configDataexportRecordcount;
            }
        }
    }
    
    public boolean isLoadAllReportData()
    {
        return this.isLoadAllReportData;
    }
    
    private Map<String,Integer> mColBeansDisplayModes=new HashMap<String,Integer>();

    public int getColDisplayModeAfterAuthorize(ColBean cbean,boolean isForPage)
    {
        Integer displaymode=mColBeansDisplayModes.get(cbean.getColid()+"_"+isForPage);
        if(displaymode==null)
        {
            if(isForPage||rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&reportBean.getDbean().isAllColDisplaytypesEquals()
                    ||rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
            {
                displaymode=cbean.getDisplaymode(rrequest,lstDynDisplayColids,isForPage);
            }else
            {
                displaymode=cbean.getDisplaymode(rrequest,null,isForPage);
            }
            mColBeansDisplayModes.put(cbean.getColid()+"_"+isForPage,displaymode);
        }
        return displaymode.intValue();
    }
    
    public Map getAttributes()
    {
        return attributes;
    }

    public void setAttributes(Map attributes)
    {
        this.attributes=attributes;
    }

    public AbsListReportFilterBean getFilteredBean()
    {
        return filteredBean;
    }

    public void setFilteredBean(AbsListReportFilterBean filteredBean)
    {
        this.filteredBean=filteredBean;
    }

    public List<Map<String,String>> getLstEditedData(AbsEditableReportEditDataBean editbean)
    {
        if(this.mEditedData==null) return null;
        return mEditedData.get(getEditypeKey(editbean));
    }

    public void setLstEditedData(AbsEditableReportEditDataBean editbean,List<Map<String,String>> lstEditedData)
    {
        if(mEditedData==null)
        {
            mEditedData=new HashMap<String,List<Map<String,String>>>();
        }
        mEditedData.put(getEditypeKey(editbean),lstEditedData);
    }

    public List<Map<String,String>> getLstEditedParamValues(AbsEditableReportEditDataBean editbean)
    {
        if(this.mParamValuesForEditedData==null) return null;
        return mParamValuesForEditedData.get(getEditypeKey(editbean));
    }

    public void setLstEditedParamValues(AbsEditableReportEditDataBean editbean,List<Map<String,String>> lstEditedExternalValues)
    {
        if(mParamValuesForEditedData==null)
        {
            mParamValuesForEditedData=new HashMap<String,List<Map<String,String>>>();
        }
        mParamValuesForEditedData.put(getEditypeKey(editbean),lstEditedExternalValues);
    }
    
    private String getEditypeKey(AbsEditableReportEditDataBean editbean)
    {
        String edittype="";
        if(editbean instanceof EditableReportDeleteDataBean)
        {
            edittype="delete";
        }else if(editbean instanceof EditableReportInsertDataBean)
        {
            edittype="insert";
        }else
        {
            edittype="update";
        }
        return this.reportBean.getId()+"_editype_"+edittype;
    }
    
    public void setRowLabelstyleproperty(String labelstyleproperty)
    {
        if(this.lstDynRowLabelStyleproperties==null) this.lstDynRowLabelStyleproperties=new ArrayList<String>();
        if(this.lstDynRowLabelStyleproperties.size()>0) this.lstDynRowLabelStyleproperties.clear();//一个报表只有一个rowlabelstyleproperty，所以之前设置过的话则清空掉
        this.lstDynRowLabelStyleproperties.add(labelstyleproperty);
    }
    
    public String getDynRowLabelstyleproperty()
    {
        if(this.lstDynRowLabelStyleproperties==null||this.lstDynRowLabelStyleproperties.size()==0) return null;
        return this.lstDynRowLabelStyleproperties.get(0);
    }
    
    public void setRowValuestyleproperty(String key,String valuestyleproperty)
    {
        if(this.mDynRowValueStyleproperties==null) this.mDynRowValueStyleproperties=new HashMap<String, String>();
        this.mDynRowValueStyleproperties.put(key,valuestyleproperty);
    }
    
    public String getDynRowValuestyleproperty(String key)
    {
        if(this.mDynRowValueStyleproperties==null) return null;
        return this.mDynRowValueStyleproperties.get(key);
    }
    
    public void setColLabelstyleproperty(String key,String labelstyleproperty)
    {
        if(this.mDynColLabelStyleproperties==null) this.mDynColLabelStyleproperties=new HashMap<String,String>();
        this.mDynColLabelStyleproperties.put(key,labelstyleproperty);
    }
    
    public String getDynColLabelstyleproperty(String key)
    {
        if(this.mDynColLabelStyleproperties==null) return null;
        return this.mDynColLabelStyleproperties.get(key);
    }
    
    public void setColValuestyleproperty(String key,String valuestyleproperty)
    {
        if(this.mDynColValueStyleproperties==null) this.mDynColValueStyleproperties=new HashMap<String,String>();
        this.mDynColValueStyleproperties.put(key,valuestyleproperty);
    }
    
    public String getDynColValuestyleproperty(String key)
    {
        if(this.mDynColValueStyleproperties==null) return null;
        return this.mDynColValueStyleproperties.get(key);
    }
}
