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
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.dataset.select.rationaldbassistant.report.AbsGetReportDataSetBySQL;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Consts_Private;

public class PartConvertSQLevel extends CompleteConvertSQLevel
{
    
    public PartConvertSQLevel(SQLReportDataSetValueProvider ownerProvider)
    {
        super(ownerProvider);
    }

    public String getFilterdata_sql(ReportRequest rrequest,AbsGetReportDataSetBySQL sqlDataSetObj)
    {
        if(kernelSql.indexOf(SQLReportDataSetValueProvider.filterConditionPlaceHolder)<0)
        {
            throw new WabacusRuntimeException("获取报表"+this.getReportBean().getPath()+"列过滤选项数据失败，此报表数据集的sqlconvertlevle配置为part时，必须在SQL语句中指定"
                    +SQLReportDataSetValueProvider.filterConditionPlaceHolder+"占位符");
        }
        return super.getFilterdata_sql(rrequest,sqlDataSetObj);
    }

    public void parseSql(String sql)
    {
        super.parseSql(sql);
        if(this.ownerProvider.getOwnerDataSetValueBean().hasRowSelectValueConditions()
                &&sql.indexOf(SQLReportDataSetValueProvider.rowselectvaluesConditionPlaceHolder)<0)
        {
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()
                    +"的数据集<value/>的sqlconvertlevle配置为part，而且此数据集需要取选中行的记录，因此必须为其指定"+SQLReportDataSetValueProvider.rowselectvaluesConditionPlaceHolder
                    +"占位符");
        }
        convertedSql=Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL+" "+SQLReportDataSetValueProvider.orderbyPlaceHolder;
        parseSqlKernelAndOrderBy();
        countSql="select count(*) from ("+Consts_Private.PLACEHOLDER_LISTREPORT_SQLKERNEL+")  wx_tabletemp ";
        AbsDatabaseType dbtype=Config.getInstance().getDataSource(this.getDatasource()).getDbType();
        if(dbtype==null) throw new WabacusConfigLoadingException("没有实现数据源"+this.getDatasource()+"对应数据库类型的相应实现类");
        this.pagesplitSql=dbtype.constructSplitPageSql(this);
    }
}

