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
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.system.component.application.report.chart.FusionChartsReportType;

public class HorizontalDatasetType extends AbsDatasetType
{
    public HorizontalDatasetType(FusionChartsReportType reportTypeObj)
    {
        super(reportTypeObj);
        this.lstDisplayedColBeans=rrequest.getCdb(rbean.getId()).getLstDynOrderColBeans();
    }

    protected String getColKey(ColBean cbean)
    {
        return cbean.getColumn();
    }

    protected boolean shouldDisplayThisRowData(AbsReportDataPojo rowDataObj)
    {
        if(rowDataObj==null||rowDataObj.getHdsDataColBean()==null) return false;
        if(this.reportTypeObj.isHiddenCol(rowDataObj.getHdsDataColBean())) return false;
        return true;
    }

    public void displayDualLayerDatasetDataPart(StringBuffer resultBuf)
    {
        if(this.lstReportData==null||this.lstReportData.size()==0) return;
        String dsGroupidTmp;
        StringBuffer bufTmp;
        for(List<ReportDataSetBean> lstDatasetBeansTmp:rbean.getSbean().getLstHdsDataDatasetGroupBeans())
        {
            if(lstDatasetBeansTmp==null||lstDatasetBeansTmp.size()==0) continue;
            dsGroupidTmp=lstDatasetBeansTmp.get(0).getGroupid();
            bufTmp=new StringBuffer();
            int i=0;
            for(AbsReportDataPojo dataObjTmp:this.lstReportData)
            {
                if(dataObjTmp.getWx_belongto_datasetid().equals(dsGroupidTmp))
                {//对于横向数据集，dataObj中存的datasetid就是<dataset/>的分组ID
                    bufTmp.append(showRowData(dataObjTmp,i));
                }
                i++;
            }
            if(bufTmp.length()>0)
            {
                resultBuf.append("<dataset ").append(lstDatasetBeansTmp.get(0).getDatasetstyleproperty(rrequest,false));
                resultBuf.append(">").append(bufTmp.toString()).append("</dataset>");
            }
        }
    }
}
