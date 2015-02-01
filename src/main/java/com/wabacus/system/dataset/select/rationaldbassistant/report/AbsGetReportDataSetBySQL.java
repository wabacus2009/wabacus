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

import java.sql.SQLException;
import java.util.List;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.condition.ConditionInSqlBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.dataset.select.rationaldbassistant.BatchStatisticItems;
import com.wabacus.system.dataset.select.rationaldbassistant.GetDataSetBySQL;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.AbsReportSQLConvertLevel;
import com.wabacus.util.Tools;

public abstract class AbsGetReportDataSetBySQL extends GetDataSetBySQL
{
    protected SQLReportDataSetValueProvider provider;
    
    public AbsGetReportDataSetBySQL(ReportRequest rrequest,ReportBean rbean,SQLReportDataSetValueProvider provider,boolean isPreparedStmt)
    {
        super(rrequest,rbean,isPreparedStmt);
        this.provider=provider;
    }
    
    public Object getColFilterDataSet(ColBean filterColBean,boolean isGetSelectedOptions)
    {
        String sql=this.provider.getSqlConvertObj().getFilterdata_sql(rrequest,this);
        if(sql==null||sql.trim().equals("")) return null;
        if(filterColBean.getColumn()==null||filterColBean.getColumn().trim().equals("")) return null;
        sql=Tools.replaceAll(sql,AbsReportSQLConvertLevel.filterColumnPlaceholder,filterColBean.getColumn());
        if(isGetSelectedOptions)
        {
            sql=ReportAssistant.getInstance().replaceSQLConditionPlaceHolderByRealValue(rbean,sql,
                    SQLReportDataSetValueProvider.filterConditionPlaceHolder,this.provider.getFilterConditionExpression(rrequest));
        }else
        {
            sql=ReportAssistant.getInstance().replaceSQLConditionPlaceHolderByRealValue(rbean,sql,
                    SQLReportDataSetValueProvider.filterConditionPlaceHolder,null);
        }
        return getDataSet(rrequest.getAttribute(rbean.getId()+"_WABACUS_FILTERBEAN"),sql);
    }    
    
    public Object getDataSet(Object typeObj,String sql)
    {
        try
        {
            if(lstConditions!=null) lstConditions.clear();
            if(lstConditionsTypes!=null) lstConditionsTypes.clear();
            sql=parseRuntimeSqlAndCondition(sql);
            sql=ReportAssistant.getInstance().replaceSQLConditionPlaceHolderByRealValue(rbean,sql,
                    SQLReportDataSetValueProvider.filterConditionPlaceHolder,this.provider.getFilterConditionExpression(rrequest));
            sql=ReportAssistant.getInstance().replaceSQLConditionPlaceHolderByRealValue(rbean,sql,
                    SQLReportDataSetValueProvider.rowselectvaluesConditionPlaceHolder,this.provider.getRowSelectValueConditionExpression(rrequest));
            if(rbean.getInterceptor()!=null&&typeObj!=null)
            {
                Object obj=rbean.getInterceptor().beforeLoadData(rrequest,rbean,typeObj,sql);
                if(!(obj instanceof String)) return obj;
                sql=(String)obj;
            }
            return executeQuery(this.provider.getOwnerDataSetValueBean().getDatasource(),sql);
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("从数据库取数据失败，执行SQL："+sql+"抛出异常",e);
        }
    }
    
    protected String parseRuntimeSqlAndCondition(String sql)
    {
        List<ConditionBean> lstConditionBeans=rbean.getSbean().getLstConditions();
        if(lstConditionBeans==null||lstConditionBeans.size()==0)
        {
            if(sql!=null&&sql.indexOf("{#condition#}")>0)
            {
                sql=ReportAssistant.getInstance().replaceSQLConditionPlaceHolderByRealValue(rbean,sql,"{#condition#}",null);
            }
        }else
        {
            List<ConditionInSqlBean> lstConditionInSqlBeans=provider.getLstConditionInSqlBeans();
            if(lstConditionInSqlBeans==null||lstConditionInSqlBeans.size()==0)
            {//没有在sql语句中通过#name#方式直接指定动态条件
                sql=ReportAssistant.getInstance().addDynamicConditionExpressionsToSql(rrequest,rbean,this.provider.getOwnerDataSetValueBean(),sql,
                        this.provider.getLstMyConditionBeans(rrequest),this.lstConditions,this.lstConditionsTypes);
            }else
            {
                for(ConditionInSqlBean conbeanInSqlTmp:lstConditionInSqlBeans)
                {
                    sql=conbeanInSqlTmp.parseConditionInSql(rrequest,sql,this.lstConditions,this.lstConditionsTypes);
                }
            }
        }
        return sql;
    }
    
    public abstract Object getRecordcount();
    
    public abstract Object getReportDataSet(List<AbsReportDataPojo> lstReportData);
    
    public abstract Object getStatisticDataSet(BatchStatisticItems batStatitems,String sql);
}

