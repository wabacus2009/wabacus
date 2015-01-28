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
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.database.type.SQLSERVER2K;
import com.wabacus.config.database.type.SQLSERVER2K5;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.dataset.select.rationaldbassistant.BatchStatisticItems;
import com.wabacus.system.dataset.select.report.value.RelationalDBReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Tools;

public class GetReportPartDataSetBySQL extends AbsGetReportDataSetBySQL
{
    private int startRownum;
    
    private int endRownum;
    
    public GetReportPartDataSetBySQL(ReportRequest rrequest,ReportBean rbean,SQLReportDataSetValueProvider provider,boolean isPreparedStmt)
    {
        super(rrequest,rbean,provider,isPreparedStmt);
    }
    
    public void setStartRownum(int startNum)
    {
        this.startRownum=startNum;
    }

    public void setEndRownum(int endNum)
    {
        this.endRownum=endNum;
    }

    public Object getRecordcount()
    {
        String sqlCount=this.provider.getSqlConvertObj().getRecordcountSql(rrequest,this);
        if(Tools.isEmpty(sqlCount)) return null;
        return getDataSet(this.provider.getOwnerDataSetValueBean(),sqlCount);
    }

    public Object getReportDataSet(List<AbsReportDataPojo> lstReportData)
    {
        if(this.startRownum<0||this.endRownum<=0||this.startRownum>=this.endRownum) return null;
        String sql=this.provider.getSqlConvertObj().getReportDataSetSql(rrequest,this);
        if(Tools.isEmpty(sql)) return null;
        sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.startRowNumPlaceHolder,String.valueOf(this.startRownum));
        sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.endRowNumPlaceHolder,String.valueOf(this.endRownum));
        sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.pagesizePlaceHolder,String.valueOf(this.endRownum-this.startRownum));
        return getDataSet(this.provider.getOwnerDataSetValueBean(),sql);
    }
    
    public Object getStatisticDataSet(BatchStatisticItems batStatitems,String sql)
    {
        if(this.startRownum<0||this.endRownum<=0||this.startRownum>=this.endRownum) return null;
        String sqlTmp=this.provider.getSqlConvertObj().getStatisticDataSetSql(rrequest,this);
        AbsDatabaseType dbType=rrequest.getDbType(this.provider.getOwnerDataSetValueBean().getDatasource());
        if(dbType instanceof SQLSERVER2K||dbType instanceof SQLSERVER2K5)
        {
            String sqlTmp2=Tools.removeBracketAndContentInside(sqlTmp,true);
            sqlTmp2=Tools.replaceAll(sqlTmp2,"  "," ");//将所有空格替换成只有一个空格
            if(sqlTmp2.toLowerCase().indexOf("order by")>0)
            {
                sqlTmp=sqlTmp.substring(0,sqlTmp.toLowerCase().lastIndexOf("order by"));
            }
        }
        sql=Tools.replaceAll(sql,RelationalDBReportDataSetValueProvider.STATISQL_PLACEHOLDER,sqlTmp);
        sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.startRowNumPlaceHolder,String.valueOf(this.startRownum));
        sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.endRowNumPlaceHolder,String.valueOf(this.endRownum));
        sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.pagesizePlaceHolder,String.valueOf(this.endRownum-this.startRownum));
        return getDataSet(batStatitems,sql);
    }    
}
