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
package com.wabacus.system.dataset.select.rationaldbassistant.report;

import java.util.List;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.dataset.select.rationaldbassistant.BatchStatisticItems;
import com.wabacus.system.dataset.select.report.value.RelationalDBReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Tools;

public class GetReportAllDataSetBySQL extends AbsGetReportDataSetBySQL
{
    public GetReportAllDataSetBySQL(ReportRequest rrequest,ReportBean rbean,SQLReportDataSetValueProvider provider,boolean isPreparedStmt)
    {
        super(rrequest,rbean,provider,isPreparedStmt);
    }

    public Object getRecordcount()
    {
        return null;
    }
    
    public Object getReportDataSet(List<AbsReportDataPojo> lstReportData)
    {
        String sql=this.provider.getSqlConvertObj().getReportDataSetSql(rrequest,this);
        if(Tools.isEmpty(sql)) return null;
        if(this.provider.getOwnerDataSetValueBean().isDependentDataSet())
        {
            sql=ReportAssistant.getInstance().replaceSQLConditionPlaceHolderByRealValue(rbean,sql,
                    SQLReportDataSetValueProvider.dependsConditionPlaceHolder,
                    this.provider.getOwnerDataSetValueBean().getRealDependsConditionExpression(lstReportData));
        }
        return getDataSet(this.provider.getOwnerDataSetValueBean(),sql);
    }
    
    public Object getStatisticDataSet(BatchStatisticItems batStatitems,String sql)
    {
        String sqlTmp=this.provider.getSqlConvertObj().getStatisticDataSetSql(rrequest,this);
        if(Tools.isEmpty(sqlTmp)) return null;
        if(this.provider.getOwnerDataSetValueBean().isDependentDataSet())
        {
            List<AbsReportDataPojo> lstReportData=null;
            if(!rrequest.getCdb(rbean.getId()).isLoadAllReportData()&&!batStatitems.isStatisticForOnePage())
            {
                //查询子数据集数据不管分不分页都是用此类查询，因此当针对一页数据进行统计时也是用此代码，不过此时父数据集数据只能用当前页的，而不能用所有父数据集数据，所以不需进行如下查询，而是直接调用getLstReportData()得到当前页的父数据集数据
                lstReportData=(List<AbsReportDataPojo>)rrequest.getAttribute(rbean.getId()+"wx_all_data_tempory");
                if(lstReportData==null)
                {
                    lstReportData=ReportAssistant.getInstance().loadReportDataSet(rrequest,rrequest.getDisplayReportTypeObj(rbean),true);
                    rrequest.setAttribute(rbean.getId()+"wx_all_data_tempory",lstReportData);
                }
            }else
            {
                lstReportData=rrequest.getDisplayReportTypeObj(rbean).getLstReportData();
            }
            sqlTmp=ReportAssistant.getInstance().replaceSQLConditionPlaceHolderByRealValue(rbean,sqlTmp,
                    SQLReportDataSetValueProvider.dependsConditionPlaceHolder,
                    this.provider.getOwnerDataSetValueBean().getRealDependsConditionExpression(lstReportData));
        }
        sql=Tools.replaceAll(sql,RelationalDBReportDataSetValueProvider.STATISQL_PLACEHOLDER,sqlTmp);
        return getDataSet(batStatitems,sql);
    }
}

