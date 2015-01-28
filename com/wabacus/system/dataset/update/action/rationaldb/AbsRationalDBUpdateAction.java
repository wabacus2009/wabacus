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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.database.type.Oracle;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.EditableReportSQLButtonDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportExternalValueBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.datatype.BlobType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public abstract class AbsRationalDBUpdateAction extends AbsUpdateAction
{
    private static Log log=LogFactory.getLog(AbsRationalDBUpdateAction.class);
    
    protected boolean isPreparedStatement;
    
    protected String datasource;
    
    protected boolean isOriginalParams;
    
    protected boolean isStandardSql;
    
    protected String sqlsp;//更新数据的SQL语句或存储过程

    protected String returnValueParamname;//用于保存此SQL语句或存储过程返回值的变量名，可以是定义在<params/>的变量的name属性或rrequset的key
    
    public AbsRationalDBUpdateAction(AbsEditableReportEditDataBean ownerUpdateBean)
    {
        super(ownerUpdateBean);
    }

    public void setPreparedStatement(boolean isPreparedStatement)
    {
        this.isPreparedStatement=isPreparedStatement;
    }

    public void setDatasource(String datasource)
    {
        this.datasource=datasource;
    }

    public void setOriginalParams(boolean isOriginalParams)
    {
        this.isOriginalParams=isOriginalParams;
    }

    public void setSqlsp(String sqlsp)
    {
        this.sqlsp=sqlsp;
    }

    public String getSqlsp()
    {
        return sqlsp;
    }

    public String getReturnValueParamname()
    {
        return returnValueParamname;
    }

    public void setReturnValueParamname(String returnValueParamname)
    {
        this.returnValueParamname=returnValueParamname;
    }

    public void beginTransaction(ReportRequest rrequest)
    {
        Connection conn=rrequest.getConnection(this.datasource);
        String dsLevel=rrequest.getTransactionLevel(this.datasource);
        if(dsLevel!=null&&!dsLevel.trim().equals(""))
        {
            if(!Consts_Private.M_ALL_TRANSACTION_LEVELS.containsKey(dsLevel))
            {
                throw new WabacusRuntimeException("为页面"+rrequest.getPagebean().getId()+"的数据源"+this.datasource+"设置事务隔离级别："+dsLevel+"不合法，不支持这个事务隔离级别");
            }
            if(dsLevel.equals(Consts.TRANS_NONE)) return;
        }
        try
        {
            if(!conn.getAutoCommit()) return;
            conn.setAutoCommit(false);
            if(!Tools.isEmpty(dsLevel)) conn.setTransactionIsolation(Consts_Private.M_ALL_TRANSACTION_LEVELS.get(dsLevel));
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("启动报表"+this.ownerUpdateBean.getOwner().getReportBean().getPath()+"的数据源"+this.datasource+"事件失败",e);
        }
    }

    public void commitTransaction(ReportRequest rrequest)
    {
        Connection conn=rrequest.getConnection(this.datasource);
        try
        {
            if(conn.getAutoCommit()) return;
            conn.commit();
            conn.setAutoCommit(true);
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("提交报表"+this.ownerUpdateBean.getOwner().getReportBean().getPath()+"的数据源"+this.datasource+"事件失败",e);
        }
    }

    public void rollbackTransaction(ReportRequest rrequest)
    {
        Connection conn=rrequest.getConnection(this.datasource);
        try
        {
            if(conn.getAutoCommit()) return;
            conn.rollback();
            conn.setAutoCommit(true);
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("回滚报表"+this.ownerUpdateBean.getOwner().getReportBean().getPath()+"的数据源"+this.datasource+"事件失败",e);
        }
    }
    
    public String getExecuteSql(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues)
    {
        String dynamicsql=getDynExecuteSql(rrequest,null);
        if(!Tools.isEmpty(dynamicsql)) return dynamicsql;
        if(this.isPreparedStatement) return this.sqlsp;//如果是preparedstatement方式执行，则直接返回带?的SQL语句
        dynamicsql=getStatementExecuteSql(rrequest, mRowData,mParamValues);
        setExecuteSql(rrequest,dynamicsql);
        return dynamicsql;
    }
    
    protected String getDynExecuteSql(ReportRequest rrequest,String defaultsql)
    {
        String dynamicsql=(String)rrequest.getAttribute(this.ownerUpdateBean.getOwner().getReportBean().getId(),getExecuteSqlKey(rrequest));
        return Tools.isEmpty(dynamicsql)?defaultsql:dynamicsql;
    }

    public void setExecuteSql(ReportRequest rrequest,String sql)
    {
        rrequest.setAttribute(this.ownerUpdateBean.getOwner().getReportBean().getId(),getExecuteSqlKey(rrequest),sql);
    }

    private String getExecuteSqlKey(ReportRequest rrequest)
    {
        String reportid=this.ownerUpdateBean.getOwner().getReportBean().getId();
        String updateRowIdx=(String)rrequest.getAttribute(reportid,"update_datarow_index");
        if(updateRowIdx==null) updateRowIdx="";
        return "dynamicalsql_"+updateRowIdx+"_"+this.objectId;
    }
    
    public void updateData(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues) throws SQLException
    {
        String realsql=getExecuteSql(rrequest,mRowData,mParamValues);
        if(Config.show_sql) log.info("Execute sql:"+realsql);
        Connection conn=rrequest.getConnection(this.datasource);
        Statement stmt=null;
        try
        {
            int rtnVal;
            if(this.isPreparedStatement)
            {
                stmt=conn.prepareStatement(realsql);
                rtnVal=updateDataByPreparedstatement(rrequest,mRowData,mParamValues,(PreparedStatement)stmt,realsql);
            }else
            {
                stmt=conn.createStatement();
                rtnVal=stmt.executeUpdate(realsql);
            }
            storeReturnValue(rrequest,mParamValues,String.valueOf(rtnVal));
        }finally
        {
            WabacusAssistant.getInstance().release(null,stmt);
        }
    }
    
    private String getStatementExecuteSql(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues)
    {
        ReportBean rbean=this.ownerUpdateBean.getOwner().getReportBean();
        String realsql=this.sqlsp;
        if(lstParamBeans!=null&&lstParamBeans.size()>0)
        {
            AbsDatabaseType dbtype=rrequest.getDbType(this.datasource);
            String paramValueTmp;
            for(EditableReportParamBean paramBean:lstParamBeans)
            {
                paramValueTmp=paramBean.getRuntimeParamValue(rrequest,rbean,mRowData,mParamValues,this.datasource,ownerUpdateBean.isAutoReportdata());
                if(this.isOriginalParams&&this.isStandardSql)
                {
                    if(paramValueTmp==null) paramValueTmp="";
                }else
                {
                    paramValueTmp=dbtype.getStatementValue(paramBean.getDataTypeObj(),paramValueTmp);
                    if(paramValueTmp==null) paramValueTmp="null";
                }
                realsql=Tools.replaceAll(realsql,paramBean.getPlaceholder(),paramValueTmp);
            }
        }
        return realsql;
    }
    
    private int updateDataByPreparedstatement(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues,PreparedStatement pstmt,String sql)
            throws SQLException
    {
        AbsDatabaseType dbtype=rrequest.getDbType(this.datasource);
        Oracle oracleType=null;
        ReportBean rbean=this.ownerUpdateBean.getOwner().getReportBean();
        int rtnVal=1;
        if(sql.trim().toLowerCase().startsWith("select ")&&(dbtype instanceof Oracle))
        {//当前在执行更新大字段的SQL语句
            oracleType=(Oracle)dbtype;
            if(lstParamBeans!=null&&lstParamBeans.size()>0)
            {
                int colidx=1;
                for(EditableReportParamBean paramBean:lstParamBeans)
                {
                    if((paramBean.getDataTypeObj() instanceof ClobType)||(paramBean.getDataTypeObj() instanceof BlobType)) continue;
                    paramBean.getDataTypeObj().setPreparedStatementValue(colidx++,
                            paramBean.getRuntimeParamValue(rrequest,rbean,mRowData,mParamValues,this.datasource,ownerUpdateBean.isAutoReportdata()),
                            pstmt,dbtype);
                }
            }
            ResultSet rs=pstmt.executeQuery();
            while(rs.next())
            {
                if(lstParamBeans!=null&&lstParamBeans.size()>0)
                {
                    int colidx=1;
                    for(EditableReportParamBean paramBean:lstParamBeans)
                    {
                        if(!(paramBean.getDataTypeObj() instanceof ClobType)&&!(paramBean.getDataTypeObj() instanceof BlobType)) continue;
                        String paramvalue=paramBean.getRuntimeParamValue(rrequest,rbean,mRowData,mParamValues,this.datasource,ownerUpdateBean
                                .isAutoReportdata());
                        if(paramBean.getDataTypeObj() instanceof ClobType)
                        {
                            oracleType.setClobValueInSelectMode(paramvalue,(oracle.sql.CLOB)rs.getClob(colidx++));
                        }else
                        {
                            oracleType.setBlobValueInSelectMode(paramBean.getDataTypeObj().label2value(paramvalue),(oracle.sql.BLOB)rs
                                    .getBlob(colidx++));
                        }
                    }
                }
            }
            rs.close();
        }else
        {
            if(lstParamBeans!=null&&lstParamBeans.size()>0)
            {
                int idx=1;
                for(EditableReportParamBean paramBean:lstParamBeans)
                {
                    paramBean.getDataTypeObj().setPreparedStatementValue(idx++,
                            paramBean.getRuntimeParamValue(rrequest,rbean,mRowData,mParamValues,this.datasource,ownerUpdateBean.isAutoReportdata()),
                            pstmt,dbtype);
                }
            }
            rtnVal=pstmt.executeUpdate();
        }
        return rtnVal;
    }
    
    protected void storeReturnValue(ReportRequest rrequest,Map<String,String> mExternalParamsValue,String rtnVal)
    {
        if(this.returnValueParamname==null||this.returnValueParamname.trim().equals("")) return;
        if(Tools.isDefineKey("#",this.returnValueParamname))
        {
            if(mExternalParamsValue!=null)
            {
                mExternalParamsValue.put(Tools.getRealKeyByDefine("#",this.returnValueParamname),rtnVal);
            }
        }else if(Tools.isDefineKey("rrequest",this.returnValueParamname))
        {
            rrequest.setAttribute(Tools.getRealKeyByDefine("rrequest",this.returnValueParamname),rtnVal);
        }
    }
    
    /*private String getExternalValueOfReferedCol(ReportBean rbean,ReportRequest rrequest,EditableReportParamBean paramBean,String paramvalue)
    {
        ColBean referredColBean=(ColBean)((EditableReportExternalValueBean)paramBean.getOwner()).getRefObj();//取到被引用的其它报表的列对象
        String colParamname=referredColBean.getReportBean().getId()+referredColBean.getProperty();
        if(paramvalue.indexOf(".insert.")>0)
        {
            List<Map<String,String>> lstInsertedCValues=rrequest.getLstInsertedData(referredColBean.getReportBean());
            if(lstInsertedCValues!=null&&lstInsertedCValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstInsertedCValues.get(0).get(colParamname),rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }else if(paramvalue.indexOf(".update.")>0)
        {
            List<Map<String,String>> lstUpdatedCValues=rrequest.getLstUpdatedData(referredColBean.getReportBean());//取到被引用的报表本次保存时所有变量的数据
            if(lstUpdatedCValues!=null&&lstUpdatedCValues.size()>0)
            {
                paramvalue=Tools.getRealKeyByDefine("@",paramvalue).trim();
                if(paramvalue.endsWith(".old"))
                {
                    paramvalue=lstUpdatedCValues.get(0).get(colParamname+"_old");
                    if(paramvalue==null)
                    {
                        paramvalue=lstUpdatedCValues.get(0).get(colParamname);
                    }
                    paramvalue=paramBean.getParamValue(paramvalue,rrequest,rbean);
                }else
                {
                    paramvalue=paramBean.getParamValue(lstUpdatedCValues.get(0).get(colParamname),rrequest,rbean);
                }
            }else
            {
                paramvalue="";
            }
        }else if(paramvalue.indexOf(".delete.")>0)
        {
            List<Map<String,String>> lstDeletedCValues=rrequest.getLstDeletedData(referredColBean.getReportBean());//取到被引用的报表本次保存时所有变量的数据
            if(lstDeletedCValues!=null&&lstDeletedCValues.size()>0)
            {
                paramvalue=lstDeletedCValues.get(0).get(colParamname+"_old");
                if(paramvalue==null)
                {
                    paramvalue=lstDeletedCValues.get(0).get(colParamname);
                }
                paramvalue=paramBean.getParamValue(paramvalue,rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }else
        {
            List<Map<String,String>> lstInsertedCValues=rrequest.getLstInsertedData(referredColBean.getReportBean());
            if(lstInsertedCValues!=null&&lstInsertedCValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstInsertedCValues.get(0).get(colParamname),rrequest,rbean);
            }else
            {
                List<Map<String,String>> lstUpdatedCValues=rrequest.getLstUpdatedData(referredColBean.getReportBean());
                if(lstUpdatedCValues!=null&&lstUpdatedCValues.size()>0)
                {
                    paramvalue=paramBean.getParamValue(lstUpdatedCValues.get(0).get(colParamname),rrequest,rbean);
                }else
                {
                    List<Map<String,String>> lstDeletedCValues=rrequest.getLstDeletedData(referredColBean.getReportBean());
                    if(lstDeletedCValues!=null&&lstDeletedCValues.size()>0)
                    {
                        paramvalue=lstDeletedCValues.get(0).get(colParamname+"_old");
                        if(paramvalue==null)
                        {
                            paramvalue=lstDeletedCValues.get(0).get(colParamname);
                        }
                        paramvalue=paramBean.getParamValue(paramvalue,rrequest,rbean);
                    }else
                    {
                        paramvalue="";
                    }
                }
            }
        }
        return paramvalue;
    }*/

    /**private String getReferedOtherExternalValue(ReportBean rbean,ReportRequest rrequest,EditableReportParamBean paramBean,String paramvalue)
    {
        EditableReportExternalValueBean referredEValueBean=(EditableReportExternalValueBean)((EditableReportExternalValueBean)paramBean.getOwner())
                .getRefObj();
        ReportBean rbeanRefered=referredEValueBean.getOwner().getOwner().getOwner().getReportBean();
        if(paramvalue.indexOf(".insert.")>0)
        {//是引用其它报表在<insert/>中定义的变量的值
            List<Map<String,String>> lstInsertedEValues=rrequest.getLstInsertedExternalValues(rbeanRefered);//取到被引用的报表本次保存时所有变量的数据
            if(lstInsertedEValues!=null&&lstInsertedEValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstInsertedEValues.get(0).get(referredEValueBean.getName()),rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }else if(paramvalue.indexOf(".update.")>0)
        {//是引用其它报表在<update/>中定义的变量的值
            List<Map<String,String>> lstUpdatedEValues=rrequest.getLstUpdatedExternalValues(rbeanRefered);
            if(lstUpdatedEValues!=null&&lstUpdatedEValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstUpdatedEValues.get(0).get(referredEValueBean.getName()),rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }else if(paramvalue.indexOf(".delete.")>0)
        {//是引用其它报表在<delete/>中定义的变量的值
            List<Map<String,String>> lstDeletedEValues=rrequest.getLstDeletedExternalValues(rbeanRefered);
            if(lstDeletedEValues!=null&&lstDeletedEValues.size()>0)
            {
                paramvalue=paramBean.getParamValue(lstDeletedEValues.get(0).get(referredEValueBean.getName()),rrequest,rbean);
            }else
            {
                paramvalue="";
            }
        }
        return paramvalue;
    }*/    
    
    public Object createEditParams(String paramname,String reportTypeKey)
    {
        if(paramname==null) return null;
        Object objResult=null;
        EditableReportParamBean paramBean=new EditableReportParamBean();
        if(Tools.isDefineKey("sequence",paramname))
        {
            objResult=Config.getInstance().getDataSource(this.datasource).getDbType().getSequenceValueByName(
                    Tools.getRealKeyByDefine("sequence",paramname));
        }else if(Tools.isDefineKey("#",paramname))
        {//是从<params/>中定义的变量中取值
            paramname=Tools.getRealKeyByDefine("#",paramname);
            EditableReportExternalValueBean editparamsbean=this.ownerUpdateBean.getExternalValueBeanByName(paramname,true);
            paramBean.setParamname(paramname);
            paramBean.setOwner(editparamsbean);
            objResult=paramBean;
        }else if(Tools.isDefineKey("@",paramname))
        {
            if(this.ownerUpdateBean.isAutoReportdata())
            {
                objResult=createParamBeanByColbean(Tools.getRealKeyByDefine("@",paramname),reportTypeKey,true,true);
            }else
            {
                ((EditableReportSQLButtonDataBean)this.ownerUpdateBean).setHasReportDataParams(true);
                paramBean.setParamname(paramname);
                objResult=paramBean;
            }
        }else if(WabacusAssistant.getInstance().isGetRequestContextValue(paramname)
                ||Tools.isDefineKey("!",paramname)||paramname.equals("uuid{}")||Tools.isDefineKey("increment",paramname))
        {
            paramBean.setParamname(paramname);
            objResult=paramBean;
            if(Tools.isDefineKey("url",paramname))
            {
                this.ownerUpdateBean.getOwner().getReportBean().addParamNameFromURL(Tools.getRealKeyByDefine("url",paramname));
            }
        }else
        {
            objResult=paramname;
        }
        return objResult;
    }
    
//    /**
//     * @param paramBean
//     */
//        if(((EditableReportParamBean)paramBean).getOwner() instanceof EditableReportExternalValueBean)
//        {//是从<params/>中定义的变量中取值
//        }
    
    public abstract void parseActionScript(String script,List<AbsUpdateAction> lstUpdateActions,String reportTypeKey);
    
}

