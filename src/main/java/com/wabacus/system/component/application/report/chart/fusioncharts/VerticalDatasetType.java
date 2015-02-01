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
package com.wabacus.system.component.application.report.chart.fusioncharts;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.system.component.application.report.chart.FusionChartsReportType;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.system.intercept.RowDataBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class VerticalDatasetType extends AbsDatasetType
{
    public VerticalDatasetType(FusionChartsReportType reportTypeObj)
    {
        super(reportTypeObj);
        List<ColBean> lstColBeans=rbean.getDbean().getLstCols();
        this.lstDisplayedColBeans=new ArrayList<ColBean>();
        if(lstColBeans!=null)
        {
            for(ColBean cbTmp:lstColBeans)
            {
                if(reportTypeObj.getFcrbean().isXyPlotChart())
                {
                    if("x".equals(cbTmp.getProperty())||"y".equals(cbTmp.getProperty())||"z".equals(cbTmp.getProperty()))
                    {
                        this.lstDisplayedColBeans.add(cbTmp);
                        continue;
                    }
                }
                if(cbTmp.isControlCol()||reportTypeObj.isHiddenCol(cbTmp)) continue;
                this.lstDisplayedColBeans.add(cbTmp);
            }
        }
        if(this.lstDisplayedColBeans.size()==0) this.lstDisplayedColBeans=null;
    }

    protected String getColKey(ColBean cbean)
    {
        return cbean.getProperty();
    }

    protected boolean shouldDisplayThisRowData(AbsReportDataPojo rowDataObj)
    {
        return rowDataObj!=null;
    }
    
    public void displayDualLayerDatasetDataPart(StringBuffer resultBuf)
    {
        List<String> lstDatasetIdsInGroupTmp=new ArrayList<String>();
        List lstProcessedData=new ArrayList();//存放已经处理过的POJO对象，因为一行记录只会来自一个<dataset/>，所以某条记录是来自某个<dataset/>，则不可能再来自其它<dataset/>
        String currentDatasetidTmp;
        for(List<ReportDataSetBean> lstDatasetBeansTmp:rbean.getSbean().getLstDatasetGroupBeans())
        {
            lstDatasetIdsInGroupTmp.clear();//清空用于存放本<dataset/>分组中包含的datasetid，以便判断某条记录所在的<dataset/>的id是否属于此组数据集
            for(ReportDataSetBean dsbeanTmp:lstDatasetBeansTmp)
            {//记录下本数据集组中所有数据集<dataset/>的id
                lstDatasetIdsInGroupTmp.add(dsbeanTmp.getId()+";");
            }
            StringBuffer datasetGroupBuf=new StringBuffer();
            AbsReportDataPojo dataObjTmp;
            for(int i=0;i<lstReportData.size();i++)
            {
                dataObjTmp=this.lstReportData.get(i);
                if(lstProcessedData.contains(dataObjTmp)) continue;//此POJO已经在其它组的<dataset/>中处理过了，则肯定不属于此组<dataset/>，因为一个记录行只会来自一个<dataset/>
                currentDatasetidTmp=dataObjTmp.getWx_belongto_datasetid();
                if(!lstDatasetIdsInGroupTmp.contains(currentDatasetidTmp+";")) continue;//此POJO所在的<dataset/>的id不属于本次显示的<dataset/>组
                lstProcessedData.add(dataObjTmp);
                datasetGroupBuf.append(showRowData(dataObjTmp,i));
            }
            if(datasetGroupBuf.length()>0)
            {
                resultBuf.append("<dataset ").append(lstDatasetBeansTmp.get(0).getDatasetstyleproperty(rrequest,false));//只取第一个<dataset/>的styleproperty显示在外层<dataset/>中
                resultBuf.append(">").append(datasetGroupBuf.toString()).append("</dataset>");
            }
        }
    }
    
    public void displayXyPlotChartDataPart(StringBuffer resultBuf)
    {
        int rowindex=0;
        String prevDatasetidTmp=null, currentDatasetidTmp=null, stylepropertyTmp;
        boolean isMultiDatasetRows=rbean.getSbean().isMultiDatasetRows(false);
        StringBuffer dataBufTmp=new StringBuffer(),setBufTmp;
        for(AbsReportDataPojo dataObjTmp:this.lstReportData)
        {
            if(isMultiDatasetRows)
            {//有多个数据集<dataset/>
                currentDatasetidTmp=dataObjTmp.getWx_belongto_datasetid();
                currentDatasetidTmp=currentDatasetidTmp==null?Consts.DEFAULT_KEY:currentDatasetidTmp.trim();
                if(!currentDatasetidTmp.equals(prevDatasetidTmp))
                {//当前数据集显示完，则在图表中做为一个指标的<dataset/>
                    if(dataBufTmp.length()>0)
                    {
                        resultBuf.append("<dataset ").append(
                                rbean.getSbean().getDatasetBeanById(prevDatasetidTmp).getDatasetstyleproperty(rrequest,false)).append(">");
                        resultBuf.append(dataBufTmp.toString()).append("</dataset>");
                        dataBufTmp=new StringBuffer();
                    }
                    prevDatasetidTmp=currentDatasetidTmp;
                }
            }
            stylepropertyTmp=dataObjTmp.getRowValuestyleproperty();
            if(rbean.getInterceptor()!=null)
            {
                RowDataBean rowInterceptorObjTmp=new RowDataBean(reportTypeObj,stylepropertyTmp,lstDisplayedColBeans,dataObjTmp,rowindex,
                        lstDisplayedColBeans.size());
                rbean.getInterceptor().beforeDisplayReportDataPerRow(rrequest,rbean,rowInterceptorObjTmp);
                if(!rowInterceptorObjTmp.isShouldDisplayThisRow()) continue;
                stylepropertyTmp=rowInterceptorObjTmp.getRowstyleproperty();
            }
            if(stylepropertyTmp==null) stylepropertyTmp="";
            ColDisplayData colDisplayDataTmp;
            setBufTmp=new StringBuffer();
            for(ColBean cbTmp:lstDisplayedColBeans)
            {
                if("x".equals(cbTmp.getProperty())||"y".equals(cbTmp.getProperty())||"z".equals(cbTmp.getProperty()))
                {
                    colDisplayDataTmp=ColDisplayData.getColDataFromInterceptor(reportTypeObj,cbTmp,dataObjTmp,rowindex,dataObjTmp
                            .getColValuestyleproperty(cbTmp.getProperty()),dataObjTmp.getColStringValue(cbTmp));
                    if(Tools.isEmpty(colDisplayDataTmp.getValue()))
                    {
                        setBufTmp=new StringBuffer();
                        break;
                    }
                    setBufTmp.append(cbTmp.getProperty()+"='").append(colDisplayDataTmp.getValue()).append("' ");
                }
            }
            if(setBufTmp.length()>0)
            {
                dataBufTmp.append("<set ").append(stylepropertyTmp).append(" ");
                dataBufTmp.append(setBufTmp.toString()).append("/>");
            }
        }
        if(dataBufTmp.length()>0)
        {
            resultBuf.append("<dataset ").append(rbean.getSbean().getDatasetBeanById(currentDatasetidTmp).getDatasetstyleproperty(rrequest,false));
            resultBuf.append(">").append(dataBufTmp.toString()).append("</dataset>");
        }
    }
}
