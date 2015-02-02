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
package com.wabacus.system.dataset.select.report.value;

import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.dataset.select.rationaldbassistant.BatchStatisticItems;
import com.wabacus.system.dataset.select.rationaldbassistant.ISPDataSetProvider;
import com.wabacus.system.dataset.select.rationaldbassistant.SPDataSetValueBean;
import com.wabacus.system.dataset.select.rationaldbassistant.report.GetReportDataSetBySP;

public class SPReportDataSetValueProvider extends RelationalDBReportDataSetValueProvider implements ISPDataSetProvider
{
    private SPDataSetValueBean spbean;

    public SPDataSetValueBean getSpbean()
    {
        return spbean;
    }

    public ConditionBean getConditionBeanByName(String name)
    {
        return this.getReportBean().getSbean().getConditionBeanByName(name);
    }

    public boolean isUseSystemParams()
    {
        return true;
    }
    
    public String getDatasource()
    {
        return this.ownerDataSetValueBean.getDatasource();
    }
    
    public List<String> getColFilterDataSet(ReportRequest rrequest,ColBean filterColBean,boolean isGetSelectedOptions,int maxOptionCount)
    {
        return parseColFilterResultDataset(rrequest,filterColBean,(new GetReportDataSetBySP(rrequest,filterColBean.getReportBean(),this))
                .getColFilterDataSet(filterColBean,isGetSelectedOptions),maxOptionCount);
    }

    public int getRecordcount(ReportRequest rrequest)
    {
        return parseRecordCount((new GetReportDataSetBySP(rrequest,this.getReportBean(),this)).getRecordcount());
    }

    public List<Map<String,Object>> getDataSet(ReportRequest rrequest,List<AbsReportDataPojo> lstReportData,int startRownum,int endRownum)
    {
        if(startRownum>=0&&endRownum>0&&startRownum>endRownum||endRownum==0) return null;
        GetReportDataSetBySP spDataSetObj=new GetReportDataSetBySP(rrequest,this.getReportBean(),this);
        if(startRownum>=0||endRownum>0)
        {
            if(startRownum<0) startRownum=0;
            if(endRownum<0) endRownum=Integer.MAX_VALUE;
            spDataSetObj.setStartRownum(startRownum);
            spDataSetObj.setEndRownum(endRownum);
        }
        return parseResultDataset(rrequest,spDataSetObj.getReportDataSet(lstReportData));
    }

    protected Object getStatisticDataSet(ReportRequest rrequest,BatchStatisticItems batStatitems,String statisticsql,int startRownum,int endRownum)
    {
        GetReportDataSetBySP spDataSetObj=new GetReportDataSetBySP(rrequest,this.getReportBean(),this);
        if(startRownum>=0&&endRownum>0)
        {
            if(startRownum>=endRownum) return null;
            spDataSetObj.setStartRownum(startRownum);
            spDataSetObj.setEndRownum(endRownum);
        }
        return spDataSetObj.getStatisticDataSet(batStatitems,statisticsql);
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        this.spbean=new SPDataSetValueBean(this);
        this.spbean.parseStoreProcedure(this.ownerDataSetValueBean.getReportBean(),this.getValue());
    }
}
