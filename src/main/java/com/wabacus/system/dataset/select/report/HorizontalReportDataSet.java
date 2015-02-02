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
package com.wabacus.system.dataset.select.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.chart.FusionChartsReportType;
import com.wabacus.util.Consts;

public class HorizontalReportDataSet extends AbsReportDataSetType
{
    private ColBean hdsTitleValueCbean;//查询标题行的各列数据的<col/>

    private ColBean hdsTitleLabelCbean;//查询标题行的各列label的<col/>

    public HorizontalReportDataSet(AbsReportType reportTypeObj)
    {
        super(reportTypeObj);
        hdsTitleValueCbean=rbean.getSbean().getHdsTitleValueCbean();
        hdsTitleLabelCbean=rbean.getSbean().getHdsTitleLabelCbean();
    }

    public List<AbsReportDataPojo> loadReportAllRowDatas(boolean isLoadAllDataMandatory)
    {
        List<ReportDataSetBean> lstDsBeans=rbean.getSbean().getLstDatasetBeans();
        if(lstDsBeans==null||lstDsBeans.size()==0) return null;
        Map<String,List<AbsReportDataPojo>> mDatasetAndLstDataObjs=loadAllDatasetDataObjs();//取到所有的<dataset/>的记录集
        if(mDatasetAndLstDataObjs.size()==0) return null;
        Map<String,Map<String,AbsReportDataPojo>> mDatasetAndMapDataObjs=new HashMap<String,Map<String,AbsReportDataPojo>>();//存放每个<dataset/>的ID及它们加载的记录行数据，其中每个数据行以标题行对应的列数据为KEY，行数据的POJO为值
        Map<String,AbsReportDataPojo> mOneDatasetDataObjs;
        for(Entry<String,List<AbsReportDataPojo>> entryTmp:mDatasetAndLstDataObjs.entrySet())
        {
            mOneDatasetDataObjs=new HashMap<String,AbsReportDataPojo>();
            for(AbsReportDataPojo dataObjTmp:entryTmp.getValue())
            {//将本<dataset/>中加载的各条记录POJO对象以标题列的数据为KEY进行存放，以便后面可以根据标题列数据取到它对应的这POJO对象
                mOneDatasetDataObjs.put(dataObjTmp.getColStringValue(hdsTitleValueCbean),dataObjTmp);
            }
            mDatasetAndMapDataObjs.put(entryTmp.getKey(),mOneDatasetDataObjs);
        }
        List<String[]> lstTitleColValueLabels=getLstTitleColumnValues(mDatasetAndLstDataObjs);
        if(lstTitleColValueLabels==null||lstTitleColValueLabels.size()==0) return null;
        if((reportTypeObj instanceof FusionChartsReportType)&&((FusionChartsReportType)reportTypeObj).getFcrbean().isXyPlotChart())
        {
            return createXyPlotChartResultRowDataObjs(mDatasetAndMapDataObjs,lstTitleColValueLabels);
        }else
        {
            return createHorizontalResultRowDataObjs(mDatasetAndMapDataObjs,lstTitleColValueLabels);
        }
    }

    private Map<String,List<AbsReportDataPojo>> loadAllDatasetDataObjs()
    {
        Map<String,List<AbsReportDataPojo>> mDatasetAndLstDataObjs=new HashMap<String,List<AbsReportDataPojo>>();//存放每个<dataset/>的ID及它们加载的记录行数据列表
        List<AbsReportDataPojo> lstOneDatasetDataObjs;
        List<Map<String,Object>> lstOneDatasetValueDataTmp;//一个<value/>加载的数据集合
        int maxrecordcount=cdb.getMaxrecordcount();
        if(maxrecordcount<=0) maxrecordcount=-1;
        String dsidTmp;
        for(ReportDataSetBean dsbeanTmp:rbean.getSbean().getLstDatasetBeans())
        {//循环每个<dataset/>，加载每个<dataset/>的数据
            lstOneDatasetDataObjs=new ArrayList<AbsReportDataPojo>();
            for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
            {
                if((lstOneDatasetDataObjs.size()==0)&&dsvbeanTmp.isDependentDataSet()) continue;
                lstOneDatasetValueDataTmp=dsvbeanTmp.getProvider().getDataSet(rrequest,lstOneDatasetDataObjs,-1,-1);
                if(lstOneDatasetValueDataTmp==null||lstOneDatasetValueDataTmp.size()==0) continue;
                if(dsvbeanTmp.isDependentDataSet())
                {
                    parseDependentReportData(dsvbeanTmp,lstOneDatasetValueDataTmp,lstOneDatasetDataObjs);
                }else
                {//加载所有数据
                    copyListDataToLstResultsData(dsvbeanTmp,lstOneDatasetValueDataTmp,lstOneDatasetDataObjs,maxrecordcount);
                }
            }
            if(lstOneDatasetDataObjs.size()>0)
            {
                dsidTmp=dsbeanTmp.getId()==null||dsbeanTmp.getId().trim().equals("")?Consts.DEFAULT_KEY:dsbeanTmp.getId();
                mDatasetAndLstDataObjs.put(dsidTmp,lstOneDatasetDataObjs);
            }
        }
        return mDatasetAndLstDataObjs;
    }

    private List<String[]> getLstTitleColumnValues(Map<String,List<AbsReportDataPojo>> mDatasetAndDataObjs)
    {
        String titlecolumndsid=rbean.getSbean().getTitlecolumndatasetid();
        if(titlecolumndsid==null||titlecolumndsid.trim().equals(""))
        {
            int size=-1;
            for(Entry<String,List<AbsReportDataPojo>> entryTmp:mDatasetAndDataObjs.entrySet())
            {
                if(entryTmp.getValue().size()>size)
                {
                    size=entryTmp.getValue().size();
                    titlecolumndsid=entryTmp.getKey();
                }
            }
            if(size<=0) return null;
        }
        titlecolumndsid=titlecolumndsid==null||titlecolumndsid.trim().equals("")?Consts.DEFAULT_KEY:titlecolumndsid;
        List<AbsReportDataPojo> lstDataObjsOfTitleColumn=mDatasetAndDataObjs.get(titlecolumndsid);
        if(lstDataObjsOfTitleColumn==null||lstDataObjsOfTitleColumn.size()==0) return null;
        List<String[]> lstTitleValues=new ArrayList<String[]>();
        for(AbsReportDataPojo dataObjTmp:lstDataObjsOfTitleColumn)
        {
            lstTitleValues.add(new String[] { dataObjTmp.getColStringValue(hdsTitleValueCbean), dataObjTmp.getColStringValue(hdsTitleLabelCbean) });
        }
        return lstTitleValues;
    }

    private List<AbsReportDataPojo> createXyPlotChartResultRowDataObjs(Map<String,Map<String,AbsReportDataPojo>> mDatasetAndMapDataObjs,
            List<String[]> lstTitleColValueLabels)
    {
        List<ColBean> lstConfigColBeans=rbean.getDbean().getLstCols();
        List<AbsReportDataPojo> lstDataResult=new ArrayList<AbsReportDataPojo>();
        AbsReportDataPojo rowDataObjResultTmp, rowDataObjTmp;
        String dsidTmp;
        Object colDataObj;
        for(List<ReportDataSetBean> lstDsBeansTmp:rbean.getSbean().getLstHdsDataDatasetGroupBeans())
        {//循环每个<dataset/>分组，一个<dataset/>分组合成图表数据中的一个<dataset/>（通过<dataset/>的mergetop进行分组）
            if(lstDsBeansTmp==null||lstDsBeansTmp.size()==0) continue;
            for(String[] titleColTmp:lstTitleColValueLabels)
            {//每个titleColTmp对应一条新构造的记录（即一个点的x、y、z等各列值）
                rowDataObjResultTmp=ReportAssistant.getInstance().getPojoClassInstance(rrequest,rbean,rbean.getPojoClassObj());
                for(ColBean cbTmp:lstConfigColBeans)
                {
                    if(cbTmp.isControlCol()||cbTmp.isNonValueCol()||cbTmp.isNonFromDbCol()||cbTmp.isSequenceCol()) continue;
                    colDataObj=null;
                    if(hdsTitleValueCbean.getProperty().equals(cbTmp.getProperty()))
                    {
                        colDataObj=titleColTmp[0];
                    }else if(hdsTitleLabelCbean.getProperty().equals(cbTmp.getProperty()))
                    {
                        colDataObj=titleColTmp[1];
                    }else
                    {
                        for(int i=lstDsBeansTmp.size()-1;i>=0;i--)
                        {//从后向前取，因为后面的<dataset/>会覆盖同一组的其它<dataset/>的列值
                            if(lstDsBeansTmp.get(i).getDatasetValueBeanOfCbean(cbTmp)!=null)
                            {//此<dataset/>会查询此列的数据
                                dsidTmp=lstDsBeansTmp.get(i).getId();
                                dsidTmp=dsidTmp==null||dsidTmp.trim().equals("")?Consts.DEFAULT_KEY:dsidTmp;
                                if(mDatasetAndMapDataObjs.get(dsidTmp)!=null)
                                {
                                    rowDataObjTmp=mDatasetAndMapDataObjs.get(dsidTmp).get(titleColTmp[0]);
                                    if(rowDataObjTmp!=null) colDataObj=rowDataObjTmp.getColValue(cbTmp);
                                }
                                break;//因为后面的<dataset/>会覆盖前面的，所以查到后面的值后就不用再去查前面的<dataset/>的值
                            }
                        }
                    }
                    rowDataObjResultTmp.setColValue(cbTmp,colDataObj);
                }
                rowDataObjResultTmp.setLstAllDataObjs(lstDataResult);
                rowDataObjResultTmp.setWx_belongto_datasetid(lstDsBeansTmp.get(0).getGroupid());//这里存放<dataset/>的组id
                lstDataResult.add(rowDataObjResultTmp);
            }
        }
        return lstDataResult;
    }
    
    private List<AbsReportDataPojo> createHorizontalResultRowDataObjs(Map<String,Map<String,AbsReportDataPojo>> mDatasetAndMapDataObjs,
            List<String[]> lstTitleColValueLabels)
    {
        List<ColBean> lstConfigColBeans=rbean.getDbean().getLstCols();
        List<ColBean> lstDisplayColBeans=new ArrayList<ColBean>();//动态构造的各显示列
        if(this.reportTypeObj instanceof AbsListReportType)
        {
            for(ColBean cbTmp:lstConfigColBeans)
            {
                if(cbTmp.isControlCol()||cbTmp.isNonValueCol()||cbTmp.isNonFromDbCol()||cbTmp.isSequenceCol())
                {
                    lstDisplayColBeans.add(cbTmp);
                }
            }
        }
        ColBean cbeanTmp;
        for(String[] titleColTmp:lstTitleColValueLabels)
        {
            cbeanTmp=new ColBean(rbean.getDbean());
            cbeanTmp.setProperty("[DYN_COL_DATA]");
            cbeanTmp.setColumn(titleColTmp[0]);
            cbeanTmp.setLabel(titleColTmp[1]);
            cbeanTmp.setLabelstyleproperty(this.hdsTitleLabelCbean.getLabelstyleproperty(rrequest,false),true);
            lstDisplayColBeans.add(cbeanTmp);
        }
        this.cdb.setLstDynOrderColBeans(lstDisplayColBeans);
        List<AbsReportDataPojo> lstDataResult=new ArrayList<AbsReportDataPojo>();//存放返回的数据集POJO对象列表
        AbsReportDataPojo rowDataObjResultTmp, rowDataObjTmp;
        String dsidTmp;
        boolean hasAddHdsTitleValueColBeanRow=false;
        for(List<ReportDataSetBean> lstDsBeansTmp:rbean.getSbean().getLstHdsDataDatasetGroupBeans())
        {//循环每个<dataset/>分组，一个<dataset/>分组合成一条记录（通过<dataset/>的mergetop进行分组）
            if(lstDsBeansTmp==null||lstDsBeansTmp.size()==0) continue;
            for(ColBean cbTmp:lstConfigColBeans)
            {//一个数据<col/>就是一条记录
                if(hdsTitleLabelCbean.getProperty().equals(cbTmp.getProperty())
                        ||(hasAddHdsTitleValueColBeanRow&&hdsTitleValueCbean.getProperty().equals(cbTmp.getProperty())))
                {
                    continue;
                }
                if(cbTmp.isControlCol()||cbTmp.isNonValueCol()||cbTmp.isNonFromDbCol()||cbTmp.isSequenceCol()) continue;
                boolean hasValidData=false;//在这个<dataset/>组中，是否存在有效记录
                rowDataObjResultTmp=ReportAssistant.getInstance().getPojoClassInstance(rrequest,rbean,rbean.getPojoClassObj());
                for(String[] titleColTmp:lstTitleColValueLabels)
                {
                    if(hdsTitleValueCbean.getProperty().equals(cbTmp.getProperty()))
                    {
                        rowDataObjResultTmp.setDynamicColData(titleColTmp[0],titleColTmp[0]);
                        hasValidData=true;
                        continue;
                    }
                    for(int i=lstDsBeansTmp.size()-1;i>=0;i--)
                    {//从后向前取，因为后面的<dataset/>会覆盖同一组的其它<dataset/>的列值
                        if(lstDsBeansTmp.get(i).getDatasetValueBeanOfCbean(cbTmp)!=null)
                        {//此<dataset/>会查询此列的数据
                            dsidTmp=lstDsBeansTmp.get(i).getId();
                            dsidTmp=dsidTmp==null||dsidTmp.trim().equals("")?Consts.DEFAULT_KEY:dsidTmp;
                            if(mDatasetAndMapDataObjs.get(dsidTmp)!=null)
                            {
                                rowDataObjTmp=mDatasetAndMapDataObjs.get(dsidTmp).get(titleColTmp[0]);//取到此条记录上此标题列数据对应的值
                                if(rowDataObjTmp!=null)
                                {
                                    rowDataObjResultTmp.setDynamicColData(titleColTmp[0],rowDataObjTmp.getColValue(cbTmp));
                                    hasValidData=true;
                                }
                            }
                            break;//因为后面的<dataset/>会覆盖前面的，所以查到后面的值后就不用再去查前面的<dataset/>的值
                        }
                    }
                }
                if(hasValidData)
                {
                    rowDataObjResultTmp.setHdsDataColBean(cbTmp);
                    rowDataObjResultTmp.setLstAllDataObjs(lstDataResult);
                    rowDataObjResultTmp.setWx_belongto_datasetid(lstDsBeansTmp.get(0).getGroupid());//这里存放<dataset/>的组id
                    lstDataResult.add(rowDataObjResultTmp);
                }
            }
            hasAddHdsTitleValueColBeanRow=true;
        }
        return lstDataResult;
    }
}
