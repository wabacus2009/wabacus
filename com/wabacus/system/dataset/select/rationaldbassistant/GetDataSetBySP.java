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

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.jdbc.driver.OracleTypes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.database.type.Oracle;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.dataset.select.common.SPCommonDataSetValueProvider;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public class GetDataSetBySP
{
    private static Log log=LogFactory.getLog(GetDataSetBySP.class);

    protected ReportBean rbean;

    protected ReportRequest rrequest;

    public GetDataSetBySP(ReportRequest rrequest,ReportBean rbean)
    {
        this.rrequest=rrequest;
        this.rbean=rbean;
    }
    
    public Object getCommonDataSet(SPCommonDataSetValueProvider provider,Object typeObj,StringBuffer systemParamsBuf)
    {
        return doGetResultSet(provider.getSpbean(),typeObj,systemParamsBuf);
    }
    
    protected Object doGetResultSet(SPDataSetValueBean spBean,Object typeObj,StringBuffer systemParamsBuf)
    {
        log.debug(systemParamsBuf.toString());
        String procedure=spBean.getProcedure();
        if(rbean.getInterceptor()!=null)
        {
            Object obj=rbean.getInterceptor().beforeLoadData(rrequest,rbean,typeObj,procedure);
            if(!(obj instanceof String))
            {
                return obj;
            }
            procedure=(String)obj;
        }
        if(Config.show_sql) log.info("Execute sql: "+procedure);
        CallableStatement cstmt=null;
        try
        {
            cstmt=rrequest.getConnection(spBean.getOwnerSpProvider().getDatasource()).prepareCall(procedure);
            AbsDatabaseType dbtype=rrequest.getDbType(spBean.getOwnerSpProvider().getDatasource());
            VarcharType varcharObj=(VarcharType)Config.getInstance().getDataTypeByClass(VarcharType.class);
            int idx=1;
            if(spBean.getLstStoreProcedureParams()!=null&&spBean.getLstStoreProcedureParams().size()>0)
            {
                for(String paramTmp:spBean.getLstStoreProcedureParams())
                {
                    if(WabacusAssistant.getInstance().isGetRequestContextValue(paramTmp))
                    {//从request/session中取值
                        varcharObj.setPreparedStatementValue(idx,WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,paramTmp,""),
                                cstmt,dbtype);
                    }else if(Tools.isDefineKey("condition",paramTmp))
                    {
                        setConditionValue(rrequest,spBean,cstmt,dbtype,idx,Tools.getRealKeyByDefine("condition",paramTmp),varcharObj);
                    }else
                    {
                        varcharObj.setPreparedStatementValue(idx,paramTmp,cstmt,dbtype);
                    }
                    idx++;
                }
            }
            if(spBean.getOwnerSpProvider().isUseSystemParams())
            {
                if(systemParamsBuf==null) systemParamsBuf=new StringBuffer();
                cstmt.setString(idx++,systemParamsBuf.toString());//如果是查询报表数据，将系统参数传入
            }
            if(dbtype instanceof Oracle)
            {
                cstmt.registerOutParameter(idx,OracleTypes.CURSOR);
            }
            rrequest.addUsedStatement(cstmt);
            cstmt.executeQuery();
            ResultSet rs=null;
            if(dbtype instanceof Oracle)
            {
                rs=(ResultSet)cstmt.getObject(idx);
            }else
            {
                rs=cstmt.getResultSet();
            }
            return rs;
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("从数据库取报表"+rbean.getPath()+"数据时执行SQL："+procedure+"失败",e);
        }
    }
    
    protected void setConditionValue(ReportRequest rrequest,SPDataSetValueBean spBean,CallableStatement cstmt,AbsDatabaseType dbtype,int index,
            String conname,VarcharType varcharObj) throws SQLException
    {
        ConditionBean cbean=spBean.getOwnerSpProvider().getConditionBeanByName(conname);
        IDataType dataTypeObj=cbean.getDatatypeObj()==null?varcharObj:cbean.getDatatypeObj();
        dataTypeObj.setPreparedStatementValue(index,cbean.getConditionValueForSP(rrequest),cstmt,dbtype);
    }
}
