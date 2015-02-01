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
package com.wabacus.system.dataset.update.action.rationaldb;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public class SPUpdateAction extends AbsRationalDBUpdateAction
{
    private static Log log=LogFactory.getLog(SPUpdateAction.class);
    
    private List lstParams;
    
    public SPUpdateAction(AbsEditableReportEditDataBean ownerUpdateBean)
    {
        super(ownerUpdateBean);
    }

    public String getExecuteSql(ReportRequest rrequest,Map<String,String> rowData,Map<String,String> paramValues)
    {
        String dynamicsql=getDynExecuteSql(rrequest,null);
        if(!Tools.isEmpty(dynamicsql)) return dynamicsql;
        return this.sqlsp;
    }

    public void updateData(ReportRequest rrequest,Map<String,String> mRowData,
            Map<String,String> mParamValues) throws SQLException
    {
        String realsql=getExecuteSql(rrequest,mRowData,mParamValues);
        AbsDatabaseType dbtype=rrequest.getDbType(this.datasource);
        Connection conn=rrequest.getConnection(this.datasource);
        CallableStatement cstmt=null;
        try
        {
            ReportBean rbean=this.ownerUpdateBean.getOwner().getReportBean();
            if(Config.show_sql) log.info("Execute sql:"+realsql);
            cstmt=conn.prepareCall(realsql);
            if(lstParams!=null&&lstParams.size()>0)
            {
                int idx=1;
                IDataType varcharTypeObj=Config.getInstance().getDataTypeByClass(VarcharType.class);
                EditableReportParamBean paramBeanTmp;
                for(Object paramObjTmp:this.lstParams)
                {
                    if(paramObjTmp instanceof EditableReportParamBean)
                    {
                        paramBeanTmp=(EditableReportParamBean)paramObjTmp;
                        paramBeanTmp.getDataTypeObj().setPreparedStatementValue(
                                idx++,
                                paramBeanTmp.getRuntimeParamValue(rrequest,rbean,mRowData,mParamValues,this.datasource,ownerUpdateBean
                                        .isAutoReportdata()),cstmt,dbtype);
                    }else
                    {
                        varcharTypeObj.setPreparedStatementValue(idx++,paramObjTmp==null?"":String.valueOf(paramObjTmp),cstmt,dbtype);
                    }
                }
            }
            int outputindex=-1;
            if(this.returnValueParamname!=null&&!this.returnValueParamname.trim().equals(""))
            {//有返回值
                outputindex=this.lstParams==null?1:this.lstParams.size()+1;
                cstmt.registerOutParameter(outputindex,java.sql.Types.VARCHAR);
            }
            cstmt.execute();
            if(outputindex>0)
            {
                String rtnVal=cstmt.getString(outputindex);
                storeReturnValue(rrequest,mParamValues,rtnVal);
            }
        }finally
        {
            WabacusAssistant.getInstance().release(null,cstmt);
        }
    }

    public void parseActionScript(String sp,List<AbsUpdateAction> lstUpdateActions,String reportTypeKey)
    {
        ReportBean rbean=this.ownerUpdateBean.getOwner().getReportBean();
        if(sp.startsWith("{")&&sp.endsWith("}")) sp=sp.substring(1,sp.length()-1).trim();
        String procedure=sp.toLowerCase().startsWith("call ")?sp.substring("call ".length()).trim():sp.trim();
        if(procedure.equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的更新语句"+sp+"失败，没有指定要调用的存储过程名");
        }
        String procname=procedure;
        List lstProcedureParams=new ArrayList();
        int idxLeft=procedure.indexOf("(");
        if(idxLeft==0) throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的更新语句"+sp+"失败，配置的要调用的存储过程格式不对");
        if(idxLeft>0)
        {
            int idxRight=procedure.lastIndexOf(")");
            if(idxRight!=procedure.length()-1) throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的更新语句"+sp+"失败，配置的要调用的存储过程格式不对");
            procname=procedure.substring(0,idxLeft).trim();
            String params=procedure.substring(idxLeft+1,idxRight).trim();//存储过程参数
            if(!params.equals(""))
            {
                List<String> lstParamsTmp=Tools.parseStringToList(params,",",new String[]{"'","'"},false);
                Object paramObjTmp;
                for(String paramTmp:lstParamsTmp)
                {
                    paramObjTmp=createEditParams(paramTmp,reportTypeKey);
                    if(paramObjTmp instanceof String)
                    {
                        String strParamTmp=((String)paramObjTmp);
                        if(strParamTmp.startsWith("'")&&strParamTmp.endsWith("'")) strParamTmp=strParamTmp.substring(1,strParamTmp.length()-1);
                        if(strParamTmp.startsWith("\"")&&strParamTmp.endsWith("\"")) strParamTmp=strParamTmp.substring(1,strParamTmp.length()-1);
                        paramObjTmp=strParamTmp;
                    }
                    lstProcedureParams.add(paramObjTmp);
                }
            }
        }
        StringBuilder tmpBuf=new StringBuilder("{call "+procname+"(");
        for(int i=0,len=lstProcedureParams.size();i<len;i++)
        {
            tmpBuf.append("?,");
        }
        if(this.returnValueParamname!=null&&!this.returnValueParamname.trim().equals("")) tmpBuf.append("?");
        if(tmpBuf.charAt(tmpBuf.length()-1)==',') tmpBuf.deleteCharAt(tmpBuf.length()-1);
        tmpBuf.append(")}");
        this.sqlsp=tmpBuf.toString();
        this.lstParams=lstProcedureParams;
        lstUpdateActions.add(this);
    }
}

