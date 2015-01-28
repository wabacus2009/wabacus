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

import java.util.List;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.chart.FusionChartsReportType;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.system.intercept.RowDataBean;

public abstract class AbsDatasetType
{
    protected FusionChartsReportType reportTypeObj;

    protected ReportRequest rrequest;

    protected ReportBean rbean;

    protected List<AbsReportDataPojo> lstReportData;

    protected List<ColBean> lstDisplayedColBeans;

    public AbsDatasetType(FusionChartsReportType reportTypeObj)
    {
        this.reportTypeObj=reportTypeObj;
        this.rrequest=reportTypeObj.getReportRequest();
        this.rbean=reportTypeObj.getReportBean();
        this.lstReportData=reportTypeObj.getLstReportData();
    }

    public List<ColBean> getLstDisplayedColBeans()
    {
        return lstDisplayedColBeans;
    }

    public void displayCategoriesPart(StringBuffer resultBuf)
    {
        RowDataBean rowInterceptorObjTmp;
        ColDisplayData colDisplayDataTmp;
        String stylepropertyTmp=this.reportTypeObj.getRowLabelstyleproperty();
        if(rbean.getInterceptor()!=null)
        {
            rowInterceptorObjTmp=new RowDataBean(reportTypeObj,stylepropertyTmp,lstDisplayedColBeans,null,-1,lstDisplayedColBeans.size());
            rbean.getInterceptor().beforeDisplayReportDataPerRow(rrequest,rbean,rowInterceptorObjTmp);
            stylepropertyTmp=rowInterceptorObjTmp.getRowstyleproperty();
            if(!rowInterceptorObjTmp.isShouldDisplayThisRow()) return;
        }
        resultBuf.append("<categories ").append(stylepropertyTmp==null?"":stylepropertyTmp.trim()).append(">");
        for(ColBean cbTmp:lstDisplayedColBeans)
        {
            if(cbTmp.isNonFromDbCol())
            {//不是从数据库获取的数据，则直接显示在<col></col>中配置的内容
                resultBuf.append(cbTmp.getTagcontent(rrequest));
            }else if(!ColBean.NON_LABEL.equals(cbTmp.getLabel(null)))
            {//需要显示label，即<category/>
                colDisplayDataTmp=ColDisplayData.getColDataFromInterceptor(reportTypeObj,cbTmp,null,-1,this.reportTypeObj.getColLabelStyleproperty(
                        cbTmp,null),cbTmp.getLabel(rrequest));
                resultBuf.append("<category label='").append(colDisplayDataTmp.getValue()).append("' ");
                resultBuf.append(colDisplayDataTmp.getStyleproperty()).append("/>");
            }
        }
        resultBuf.append("</categories>");
    }

    public void displaySingleSeriesDataPart(StringBuffer resultBuf)
    {
        AbsReportDataPojo dataObj=null;
        for(AbsReportDataPojo dataObjTmp:lstReportData)
        {
            if(shouldDisplayThisRowData(dataObjTmp))
            {
                dataObj=dataObjTmp;
                break;
            }
        }
        if(dataObj==null) return;
        if(rbean.getInterceptor()!=null)
        {
            RowDataBean rowInterceptorObjTmp=new RowDataBean(reportTypeObj,null,lstDisplayedColBeans,dataObj,0,lstDisplayedColBeans.size());
            rbean.getInterceptor().beforeDisplayReportDataPerRow(rrequest,rbean,rowInterceptorObjTmp);
            if(!rowInterceptorObjTmp.isShouldDisplayThisRow()) return;
        }
        ColDisplayData colDisplayDataTmp;
        for(ColBean cbTmp:lstDisplayedColBeans)
        {
            if(cbTmp.isNonValueCol())
            {
                throw new WabacusRuntimeException("报表"+this.rbean.getPath()+"是单序列数据图表，不能配置column为{non-value}的列");
            }
            if(cbTmp.isNonFromDbCol())
            {//不是从数据库获取的数据，则直接显示在<col></col>中配置的内容
                resultBuf.append(cbTmp.getTagcontent(rrequest));
            }else
            {
                colDisplayDataTmp=ColDisplayData.getColDataFromInterceptor(reportTypeObj,cbTmp,dataObj,0,dataObj
                        .getColValuestyleproperty(getColKey(cbTmp)),dataObj.getColStringValue(cbTmp));
                resultBuf.append("<set label='").append(cbTmp.getLabel(rrequest)).append("'");
                if(colDisplayDataTmp.getValue()!=null&&!colDisplayDataTmp.getValue().equals(""))
                {
                    resultBuf.append(" value='").append(colDisplayDataTmp.getValue()).append("' ");
                    resultBuf.append(colDisplayDataTmp.getStyleproperty());
                }
                resultBuf.append("/>");
            }
        }
    }

    public void displaySingleLayerDatasetDataPart(StringBuffer resultBuf)
    {
        for(int i=0;i<lstReportData.size();i++)
        {
            resultBuf.append(showRowData(lstReportData.get(i),i));
        }
    }

    protected String showRowData(AbsReportDataPojo dataObj,int rowindex)
    {
        if(!shouldDisplayThisRowData(dataObj)) return "";
        String stylepropertyTmp=dataObj.getRowValuestyleproperty();
        if(rbean.getInterceptor()!=null)
        {
            RowDataBean rowInterceptorObjTmp=new RowDataBean(reportTypeObj,stylepropertyTmp,lstDisplayedColBeans,dataObj,rowindex,
                    lstDisplayedColBeans.size());
            rbean.getInterceptor().beforeDisplayReportDataPerRow(rrequest,rbean,rowInterceptorObjTmp);
            if(!rowInterceptorObjTmp.isShouldDisplayThisRow()) return "";
            stylepropertyTmp=rowInterceptorObjTmp.getRowstyleproperty();
        }
        if(stylepropertyTmp==null) stylepropertyTmp="";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<dataset ").append(stylepropertyTmp).append(">");
        ColDisplayData colDisplayDataTmp;
        for(ColBean cbTmp:lstDisplayedColBeans)
        {
            if(!cbTmp.isNonFromDbCol()&&!cbTmp.isNonValueCol())
            {//{non-fromdb}的列不在这里处理，而是在<categories/>中显示
                colDisplayDataTmp=ColDisplayData.getColDataFromInterceptor(reportTypeObj,cbTmp,dataObj,rowindex,dataObj
                        .getColValuestyleproperty(getColKey(cbTmp)),dataObj.getColStringValue(cbTmp));
                resultBuf.append("<set");
                if(colDisplayDataTmp.getValue()!=null&&!colDisplayDataTmp.getValue().equals(""))
                {
                    resultBuf.append(" value='").append(colDisplayDataTmp.getValue()).append("' ");
                    resultBuf.append(colDisplayDataTmp.getStyleproperty());
                }
                resultBuf.append("/>");
            }
        }
        resultBuf.append("</dataset>");
        return resultBuf.toString();
    }
    
    protected abstract String getColKey(ColBean cbean);
    
    protected abstract boolean shouldDisplayThisRowData(AbsReportDataPojo rowDataObj);
    
    public abstract void displayDualLayerDatasetDataPart(StringBuffer resultBuf);    
}
