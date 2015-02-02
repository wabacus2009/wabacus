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
package com.wabacus.system.dataset.select.rationaldbassistant;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.dataset.select.common.SQLCommonDataSetValueProvider;
import com.wabacus.system.datatype.IDataType;

public class GetDataSetBySQL
{
    private static Log log=LogFactory.getLog(GetDataSetBySQL.class);

    protected List<String> lstConditions;

    protected List<IDataType> lstConditionsTypes;

    protected boolean isPreparedStmt;

    protected ReportRequest rrequest;

    protected ReportBean rbean;
    
    public GetDataSetBySQL(ReportRequest rrequest,ReportBean rbean,boolean isPreparedStmt)
    {
        this.rrequest=rrequest;
        this.rbean=rbean;
        this.isPreparedStmt=isPreparedStmt;
        if(isPreparedStmt)
        {
            lstConditions=new ArrayList<String>();
            lstConditionsTypes=new ArrayList<IDataType>();
        }
    }
    
    public Object getCommonDataSet(SQLCommonDataSetValueProvider provider,Object typeObj,String sql)
    {
        if(lstConditions!=null) lstConditions.clear();
        if(lstConditionsTypes!=null) lstConditionsTypes.clear();
        sql=ReportAssistant.getInstance().addDynamicConditionExpressionsToSql(rrequest,rbean,null,sql,
                provider.getLstConditions(),lstConditions,lstConditionsTypes);
        if(rbean.getInterceptor()!=null&&typeObj!=null)
        {
            Object obj=rbean.getInterceptor().beforeLoadData(rrequest,rbean,typeObj,sql);
            if(!(obj instanceof String)) return obj;
            sql=(String)obj;
        }
        try
        {
            return executeQuery(provider.getDatasource(),sql);
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("执行报表"+rbean.getPath()+"的SQL语句："+sql+"失败",e);
        }
    }
    
    protected ResultSet executeQuery(String datasource,String sql) throws SQLException
    {
        if(Config.show_sql) log.info("Execute sql: "+sql);
        if(this.isPreparedStmt)
        {
            PreparedStatement pstmt=rrequest.getConnection(datasource).prepareStatement(sql,ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            if(lstConditions.size()>0)
            {
                AbsDatabaseType dbtype=rrequest.getDbType(datasource);
                for(int i=0;i<lstConditions.size();i++)
                {
                    if(Config.show_sql) log.info("param"+(i+1)+"="+lstConditions.get(i));
                    lstConditionsTypes.get(i).setPreparedStatementValue(i+1,lstConditions.get(i),pstmt,dbtype);
                }

            }
            rrequest.addUsedStatement(pstmt);
            return pstmt.executeQuery();
        }else
        {
            Statement stmt=rrequest.getConnection(datasource).createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
            rrequest.addUsedStatement(stmt);
            return stmt.executeQuery(sql);
        }
    }
}
