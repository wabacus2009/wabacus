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
package com.wabacus.system.dataset.select.report.value.sqlconvertor;

import com.wabacus.config.Config;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.dataset.select.rationaldbassistant.report.AbsGetReportDataSetBySQL;
import com.wabacus.system.dataset.select.rationaldbassistant.report.GetReportAllDataSetBySQL;
import com.wabacus.system.dataset.select.rationaldbassistant.report.GetReportPartDataSetBySQL;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class CompleteConvertSQLevel extends AbsConvertSQLevel
{
    public CompleteConvertSQLevel(SQLReportDataSetValueProvider ownerProvider)
    {
        super(ownerProvider);
    }
    
    public String getFilterdata_sql(ReportRequest rrequest,AbsGetReportDataSetBySQL sqlDataSetObj)
    {
        if(filterSql!=null) return filterSql;
        if(!this.isListReportType||ownerProvider.getOwnerDataSetValueBean().isDependentDataSet()) return null;
        String filtersqlTmp="select distinct "+filterColumnPlaceholder+"  from ("+kernelSql+") wx_tblfilter";
        if(kernelSql.indexOf(SQLReportDataSetValueProvider.filterConditionPlaceHolder)<0)
        {
            filtersqlTmp+=" where "+SQLReportDataSetValueProvider.filterConditionPlaceHolder;
        }
        filtersqlTmp+=" order by  "+filterColumnPlaceholder;
        filtersqlTmp=replaceDynColPlaceHolder(filtersqlTmp,"","[#dynamic-columns#]");
        filtersqlTmp=replaceDynColPlaceHolder(filtersqlTmp,"","(#dynamic-columns#)");
        filterSql=filtersqlTmp;
        return filterSql;
    }
    
    public String getRecordcountSql(ReportRequest rrequest,AbsGetReportDataSetBySQL sqlDataSetObj)
    {
        String sqlKernel=this.ownerProvider.getDynamicSql(rrequest);
        if("[NONE]".equals(sqlKernel)) return null;
        if(sqlKernel==null||sqlKernel.equals("")) sqlKernel=this.kernelSql;
        return Tools.replaceAll(countSql,Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,sqlKernel);
    }
    
    public String getReportDataSetSql(ReportRequest rrequest,GetReportAllDataSetBySQL sqlDataSetObj)
    {
        String sql=this.originalSql;
        String sqlDynamic=this.ownerProvider.getDynamicSql(rrequest);
        if("[NONE]".equals(sqlDynamic)) return null;
        if(this.isListReportType())
        {
            if(sqlDynamic==null||sqlDynamic.equals("")) sqlDynamic=this.kernelSql;//如果没有动态构造的SQL语句，则用静态配置的
            sql=Tools.replaceAll(this.convertedSql,Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,sqlDynamic);
            String[] orderbys=this.ownerProvider.getClickOrderByColumnAndOrder(rrequest);
            if(orderbys!=null&&orderbys.length==2)
            {
                String dynorderby=this.mixDynorderbyAndConfigOrderbyCols(orderbys[0]+" "+orderbys[1]);
                if(sql.indexOf(SQLReportDataSetValueProvider.orderbyPlaceHolder)<0)
                {
                    sql=sql+" order by "+dynorderby;
                }else
                {
                    sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.orderbyPlaceHolder," order by "+dynorderby);
                }
            }else
            {
                String ordertmp=this.getOrderby();
                if(ordertmp==null) ordertmp="";
                if(!ordertmp.trim().equals("")) ordertmp=" order by "+ordertmp;
                sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.orderbyPlaceHolder,ordertmp);
            }
        }else if(!Tools.isEmpty(sqlDynamic))
        {
            sql=sqlDynamic;
        }
        return sql;
    }
    
    public String getReportDataSetSql(ReportRequest rrequest,GetReportPartDataSetBySQL sqlDataSetObj)
    {
        return getOnePageDataSql(rrequest,sqlDataSetObj);
    }
    
    public String getStatisticDataSetSql(ReportRequest rrequest,GetReportAllDataSetBySQL sqlDataSetObj)
    {
        String sqlTmp=this.convertedSql;
        sqlTmp=Tools.replaceAll(sqlTmp,SQLReportDataSetValueProvider.orderbyPlaceHolder,"");
        String sqlDynamic=this.ownerProvider.getDynamicSql(rrequest);
        if("[NONE]".equals(sqlDynamic)) return null;
        if(sqlDynamic==null||sqlDynamic.equals("")) sqlDynamic=this.kernelSql;
        sqlTmp=Tools.replaceAll(sqlTmp,Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,sqlDynamic);
        return sqlTmp;
    }
    
    public String getStatisticDataSetSql(ReportRequest rrequest,GetReportPartDataSetBySQL sqlDataSetObj)
    {
        return getOnePageDataSql(rrequest,sqlDataSetObj);
    }
    
    private String getOnePageDataSql(ReportRequest rrequest,GetReportPartDataSetBySQL sqlDataSetObj)
    {
        String sqlKernel=this.ownerProvider.getDynamicSql(rrequest);
        if("[NONE]".equals(sqlKernel)) return null;//表示不执行此数据集的数据（目前在交叉动态列中有使用，当没有查询到动态列时，用户可能不需要执行对应的SQL语句查询它们的数据）
        if(sqlKernel==null||sqlKernel.equals("")) sqlKernel=this.kernelSql;
        String sql=this.pagesplitSql;
        String[] orderbys=this.ownerProvider.getClickOrderByColumnAndOrder(rrequest);
        if(orderbys!=null&&orderbys.length==2)
        {
            sql=rrequest.getDbType(this.getDatasource()).constructSplitPageSql((AbsConvertSQLevel)this,orderbys[0]+" "+orderbys[1]);
        }
        return Tools.replaceAll(sql,Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL,sqlKernel);
    }
    
    public final static String sqlprex="select * from (";

    public final static String sqlsuffix=") wabacus_temp_tbl";

    public void parseSql(String sql)
    {
        super.parseSql(sql);
        convertedSql=sqlprex+Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL+sqlsuffix;
        if(isListReportType)
        {
            String link=" where ";
            if(sql.indexOf(SQLReportDataSetValueProvider.filterConditionPlaceHolder)<0)
            {
                convertedSql+=link+SQLReportDataSetValueProvider.filterConditionPlaceHolder;
                link=" and ";
            }
            if(this.ownerProvider.getOwnerDataSetValueBean().hasRowSelectValueConditions()
                    &&sql.indexOf(SQLReportDataSetValueProvider.rowselectvaluesConditionPlaceHolder)<0)
            {
                convertedSql+=link+SQLReportDataSetValueProvider.rowselectvaluesConditionPlaceHolder;
            }
        }
        convertedSql+=" "+SQLReportDataSetValueProvider.orderbyPlaceHolder;//加上order by占位符
        parseSqlKernelAndOrderBy();
        countSql="select count(*) from ("+Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL+")  wx_tabletemp ";
        if(isListReportType)
        {
            String link=" where ";
            if(!ownerProvider.getOwnerDataSetValueBean().isDependentDataSet()&&sql.indexOf(SQLReportDataSetValueProvider.filterConditionPlaceHolder)<0)
            {
                countSql+=link+SQLReportDataSetValueProvider.filterConditionPlaceHolder;
                link=" and ";
            }
            if(this.ownerProvider.getOwnerDataSetValueBean().hasRowSelectValueConditions()
                    &&sql.indexOf(SQLReportDataSetValueProvider.rowselectvaluesConditionPlaceHolder)<0)
            {
                countSql+=link+SQLReportDataSetValueProvider.rowselectvaluesConditionPlaceHolder;
            }
        }
        AbsDatabaseType dbtype=Config.getInstance().getDataSource(this.getDatasource()).getDbType();
        if(dbtype==null) throw new WabacusConfigLoadingException("没有实现数据源"+this.getDatasource()+"对应数据库类型的相应实现类");
        this.pagesplitSql=dbtype.constructSplitPageSql(this);
    }
}

