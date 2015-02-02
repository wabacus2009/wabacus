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
package com.wabacus.config.database.type;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import oracle.sql.BLOB;
import oracle.sql.CLOB;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.buttons.EditableReportSQLButtonDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportExternalValueBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.AbsConvertSQLevel;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.dataset.update.action.rationaldb.SQLInsertAction;
import com.wabacus.system.dataset.update.action.rationaldb.SQLUpdateAction;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.BigdecimalType;
import com.wabacus.system.datatype.BlobType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.system.datatype.DateType;
import com.wabacus.system.datatype.DoubleType;
import com.wabacus.system.datatype.FloatType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.IntType;
import com.wabacus.system.datatype.TimestampType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public class Oracle extends AbsDatabaseType
{
    private final static Log log=LogFactory.getLog(Oracle.class);

    public String constructSplitPageSql(AbsConvertSQLevel convertSqlObj)
    {
        String sql=convertSqlObj.getConvertedSql();
        if(sql.indexOf(SQLReportDataSetValueProvider.orderbyPlaceHolder)>0)
        {
            sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.orderbyPlaceHolder," order by "+convertSqlObj.getOrderby());
        }
        StringBuffer sqlBuffer=new StringBuffer("SELECT * FROM(SELECT wx_temp_tbl1.*, ROWNUM row_num FROM ");
        sqlBuffer.append("("+sql+")  wx_temp_tbl1 WHERE ROWNUM<="+SQLReportDataSetValueProvider.endRowNumPlaceHolder+")  wx_temp_tbl2");
        sqlBuffer.append(" WHERE row_num>"+SQLReportDataSetValueProvider.startRowNumPlaceHolder);
        return sqlBuffer.toString();
    }

    public String constructSplitPageSql(AbsConvertSQLevel convertSqlObj,String dynorderby)
    {
        dynorderby=convertSqlObj.mixDynorderbyAndConfigOrderbyCols(dynorderby);
        dynorderby=" ORDER BY "+dynorderby;
        String sql=convertSqlObj.getConvertedSql();
        if(sql.indexOf(SQLReportDataSetValueProvider.orderbyPlaceHolder)<0)
        {
            sql=sql+dynorderby;
        }else
        {
            sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.orderbyPlaceHolder,dynorderby);
        }
        StringBuilder sqlBuffer=new StringBuilder("SELECT * FROM(SELECT wx_temp_tbl1.*, ROWNUM row_num FROM ");
        sqlBuffer.append("("+sql+")  wx_temp_tbl1 WHERE ROWNUM<="+SQLReportDataSetValueProvider.endRowNumPlaceHolder+")  wx_temp_tbl2");
        sqlBuffer.append(" WHERE row_num>"+SQLReportDataSetValueProvider.startRowNumPlaceHolder);
        return sqlBuffer.toString();
    }

    public void constructInsertSql(String configInsertSql,ReportBean rbean,String reportTypeKey,SQLInsertAction insertSqlAction,
            List<AbsUpdateAction> lstActionsResult)
    {
        int idxwhere=configInsertSql.toLowerCase().indexOf(" where ");
        if(idxwhere<0)
        {
            super.constructInsertSql(configInsertSql,rbean,reportTypeKey,insertSqlAction,lstActionsResult);
            return;
        }
        String whereclause=configInsertSql.substring(idxwhere).trim();
        if(whereclause.equals(""))
        {
            super.constructInsertSql(configInsertSql,rbean,reportTypeKey,insertSqlAction,lstActionsResult);
            return;
        }
        insertSqlAction.setPreparedStatement(true);
        configInsertSql=configInsertSql.substring(0,idxwhere).trim();
        List lstParams=new ArrayList();
        StringBuilder sqlBuffer=new StringBuilder();
        sqlBuffer.append("insert into ");
        configInsertSql=configInsertSql.substring("insert".length()).trim();
        if(configInsertSql.toLowerCase().indexOf("into ")==0)
        {
            configInsertSql=configInsertSql.substring("into".length()).trim();
        }
        boolean isAutoReportData=insertSqlAction.getOwnerUpdateBean().isAutoReportdata();
        Map<String,EditableReportParamBean> mLobParamsBean=new HashMap<String,EditableReportParamBean>();
        String tablename=null;
        int idxleft=configInsertSql.indexOf("(");
        if(idxleft<0||configInsertSql.endsWith("()"))
        {//没有指定要更新的字段，则将所有符合要求的从数据库取数据的<col/>全部更新到表中
            if(!isAutoReportData)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                        +"失败，在autoreportdata属性为false的<button/>中，不能配置insert into table这种格式的SQL语句");
            }
            if(configInsertSql.endsWith("()")) configInsertSql=configInsertSql.substring(0,configInsertSql.length()-2).trim();
            tablename=configInsertSql;
            sqlBuffer.append(tablename).append("(");
            for(ColBean cbean:rbean.getDbean().getLstCols())
            {
                EditableReportParamBean paramBean=insertSqlAction.createParamBeanByColbean(cbean.getProperty(),reportTypeKey,false,false);
                if(paramBean==null) continue;
                sqlBuffer.append(cbean.getColumn()+",");
                lstParams.add(processLobTypeInsert(mLobParamsBean,cbean.getColumn(),paramBean,cbean.getDatatypeObj()));
            }
        }else
        {
            tablename=configInsertSql.substring(0,idxleft);
            sqlBuffer.append(tablename).append("(");
            int idxright=configInsertSql.lastIndexOf(")");
            if(idxright!=configInsertSql.length()-1)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configInsertSql+"不合法");
            }
            String cols=configInsertSql.substring(idxleft+1,idxright);
            List<String> lstInsertCols=Tools.parseStringToList(cols,",",new String[] { "'", "'" },false);
            String columnname=null;
            String columnvalue=null;
            ColBean cb;
            for(String configColPropertyTmp:lstInsertCols)
            {
                if(configColPropertyTmp==null||configColPropertyTmp.trim().equals("")) continue;
                int idxequals=configColPropertyTmp.indexOf("=");
                if(idxequals>0)
                {
                    columnname=configColPropertyTmp.substring(0,idxequals).trim();
                    columnvalue=configColPropertyTmp.substring(idxequals+1).trim();
                    Object paramObjTmp=insertSqlAction.createEditParams(columnvalue,reportTypeKey);
                    if(paramObjTmp==null) continue;
                    sqlBuffer.append(columnname+",");
                    IDataType datatypeObj=null;
                    if(Tools.isDefineKey("#",columnvalue))
                    {//是从<params/>中定义的变量中取值
                        datatypeObj=((EditableReportExternalValueBean)((EditableReportParamBean)paramObjTmp).getOwner()).getTypeObj();
                    }else if(isAutoReportData&&Tools.isDefineKey("@",columnvalue))
                    {
                        datatypeObj=((ColBean)((EditableReportParamBean)paramObjTmp).getOwner()).getDatatypeObj();
                    }
                    lstParams.add(processLobTypeInsert(mLobParamsBean,columnname,paramObjTmp,datatypeObj));
                }else
                {
                    if(!Tools.isDefineKey("@",configColPropertyTmp))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的添加数据SQL语句"+configInsertSql+"不合法");
                    }
                    if(isAutoReportData)
                    {
                        configColPropertyTmp=Tools.getRealKeyByDefine("@",configColPropertyTmp);
                        String realColProperty=configColPropertyTmp.trim();
                        if(realColProperty.endsWith("__old")) realColProperty=realColProperty.substring(0,realColProperty.length()-"__old".length());
                        cb=rbean.getDbean().getColBeanByColProperty(realColProperty);
                        if(cb==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+configColPropertyTmp+"不合法，没有取到其值对应的<col/>");
                        }
                        sqlBuffer.append(cb.getColumn()+",");
                        lstParams.add(processLobTypeInsert(mLobParamsBean,cb.getColumn(),insertSqlAction.createParamBeanByColbean(
                                configColPropertyTmp,reportTypeKey,true,true),cb.getDatatypeObj()));
                    }else
                    {
                        ((EditableReportSQLButtonDataBean)insertSqlAction.getOwnerUpdateBean()).setHasReportDataParams(true);
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(configColPropertyTmp);
                        sqlBuffer.append(Tools.getRealKeyByDefine("@",configColPropertyTmp)+",");
                        lstParams.add(paramBean);
                    }
                }
            }
        }
        if(lstParams.size()==0)
        {
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的sql语句："+configInsertSql+"失败，SQL语句格式不对");
        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',') sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        sqlBuffer.append(") values(");
        List<EditableReportParamBean> lstCommonTypeParamsBean=new ArrayList<EditableReportParamBean>();//普通字段类型的动态参数列表（即不是clob、blog类型的参数）
        for(int j=0;j<lstParams.size();j++)
        {
            if(lstParams.get(j) instanceof EditableReportParamBean)
            {
                sqlBuffer.append("?,");
                lstCommonTypeParamsBean.add((EditableReportParamBean)lstParams.get(j));
            }else
            {
                sqlBuffer.append(lstParams.get(j)).append(",");
            }
        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',') sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        sqlBuffer.append(")");
        insertSqlAction.setSqlsp(sqlBuffer.toString());
        insertSqlAction.setLstParamBeans(lstCommonTypeParamsBean);
        lstActionsResult.add(insertSqlAction);
        if(mLobParamsBean.size()>0)
        {
            List<EditableReportParamBean> lstParamsBean2=new ArrayList<EditableReportParamBean>();
            sqlBuffer=new StringBuilder("select ");
            for(Entry<String,EditableReportParamBean> entry:mLobParamsBean.entrySet())
            {
                sqlBuffer.append(entry.getKey()).append(",");
                lstParamsBean2.add(entry.getValue());
            }
            if(sqlBuffer.charAt(sqlBuffer.length()-1)==',') sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
            sqlBuffer.append(" from ").append(tablename);
            List<EditableReportParamBean> lstDynParamsInWhereClause=new ArrayList<EditableReportParamBean>();
            whereclause=insertSqlAction.getOwnerUpdateBean().parseStandardEditSql(whereclause,lstDynParamsInWhereClause,reportTypeKey,true,false);
            lstParamsBean2.addAll(lstDynParamsInWhereClause);
            sqlBuffer.append(" ").append(whereclause).append(" for update");
            insertSqlAction=insertSqlAction.cloneWithAllDefaultValues();
            insertSqlAction.setSqlsp(sqlBuffer.toString());
            insertSqlAction.setLstParamBeans(lstParamsBean2);
            insertSqlAction.setReturnValueParamname(null);
            lstActionsResult.add(insertSqlAction);
        }
    }

    private Object processLobTypeInsert(Map<String,EditableReportParamBean> mLobParamsBean,String columnname,Object paramObj,IDataType datatypeObj)
    {
        if(datatypeObj instanceof ClobType)
        {
            mLobParamsBean.put(columnname,(EditableReportParamBean)paramObj);
            paramObj="EMPTY_CLOB()";
        }else if(datatypeObj instanceof BlobType)
        {
            mLobParamsBean.put(columnname,(EditableReportParamBean)paramObj);
            paramObj="EMPTY_BLOB()";
        }
        return paramObj;
    }

    public List<SQLUpdateAction> constructUpdateSql(String configUpdateSql,ReportBean rbean,String reportTypeKey,SQLUpdateAction updateSqlAction)
    {
        if(!hasClobBlobColumn(configUpdateSql,rbean,reportTypeKey,updateSqlAction))
        {
            return super.constructUpdateSql(configUpdateSql,rbean,reportTypeKey,updateSqlAction);
        }
        updateSqlAction.setPreparedStatement(true);
        StringBuilder sqlBuffer=new StringBuilder();
        List<EditableReportParamBean> lstParamsBean=new ArrayList<EditableReportParamBean>();
        Map<String,EditableReportParamBean> mLobParamsBean=new HashMap<String,EditableReportParamBean>();
        boolean isAutoReportData=updateSqlAction.getOwnerUpdateBean().isAutoReportdata();//当前脚本是否是配置在自动获取报表数据进行操作的地方
        int idxleft=configUpdateSql.indexOf("(");
        boolean hasNonLobTypeUpdateParams=false;
        if(idxleft<0||configUpdateSql.endsWith("()"))
        {//没有指定要更新的字段，则将所有从数据库取数据的<col/>（不包括displaytype为hidden的<col/>）全部更新到表中
            if(!isAutoReportData)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                        +"失败，在autoreportdata属性为false的<button/>中，不能配置insert into table这种格式的SQL语句");
            }
            if(configUpdateSql.endsWith("()")) configUpdateSql=configUpdateSql.substring(0,configUpdateSql.length()-2);
            sqlBuffer.append(configUpdateSql).append(" set ");
            for(ColBean cbean:rbean.getDbean().getLstCols())
            {
                EditableReportParamBean paramBean=updateSqlAction.createParamBeanByColbean(cbean.getProperty(),reportTypeKey,false,false);
                if(paramBean==null) continue;
                if(cbean.getDatatypeObj() instanceof ClobType||cbean.getDatatypeObj() instanceof BlobType)
                {
                    mLobParamsBean.put(cbean.getColumn(),paramBean);
                }else
                {
                    hasNonLobTypeUpdateParams=true;
                    sqlBuffer.append(cbean.getColumn()+"=?,");
                    lstParamsBean.add(paramBean);
                }
            }
        }else
        {
            sqlBuffer.append(configUpdateSql.substring(0,idxleft)).append(" set ");
            int idxright=configUpdateSql.lastIndexOf(")");
            if(idxright!=configUpdateSql.length()-1)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configUpdateSql+"不合法");
            }
            String cols=configUpdateSql.substring(idxleft+1,idxright);
            List<String> lstUpdateCols=Tools.parseStringToList(cols,",",new String[] { "'", "'" },false);
            String columnname=null;
            String columnvalue=null;
            ColBean cb;
            for(String updatecol:lstUpdateCols)
            {
                if(updatecol==null||updatecol.trim().equals("")) continue;
                int idxequals=updatecol.indexOf("=");
                if(idxequals>0)
                {
                    columnname=updatecol.substring(0,idxequals).trim();
                    columnvalue=updatecol.substring(idxequals+1).trim();
                    Object paramObjTmp=updateSqlAction.createEditParams(columnvalue,reportTypeKey);
                    if(paramObjTmp==null) continue;
                    IDataType datatypeObj=null;
                    if(Tools.isDefineKey("#",columnvalue))
                    {//是从<params/>中定义的变量中取值
                        datatypeObj=((EditableReportExternalValueBean)((EditableReportParamBean)paramObjTmp).getOwner()).getTypeObj();
                    }else if(isAutoReportData&&Tools.isDefineKey("@",columnvalue))
                    {
                        datatypeObj=((ColBean)((EditableReportParamBean)paramObjTmp).getOwner()).getDatatypeObj();
                    }
                    if(datatypeObj instanceof ClobType||datatypeObj instanceof BlobType)
                    {
                        mLobParamsBean.put(columnname,(EditableReportParamBean)paramObjTmp);
                    }else
                    {
                        hasNonLobTypeUpdateParams=true;
                        sqlBuffer.append(columnname+"=");
                        if(paramObjTmp instanceof EditableReportParamBean)
                        {
                            sqlBuffer.append("?");
                            lstParamsBean.add((EditableReportParamBean)paramObjTmp);
                        }else
                        {
                            sqlBuffer.append(paramObjTmp);
                        }
                        sqlBuffer.append(",");
                    }
                }else
                {
                    if(!Tools.isDefineKey("@",updatecol))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configUpdateSql+"不合法，更新的字段值必须采用@{}括住");
                    }
                    if(isAutoReportData)
                    {
                        updatecol=Tools.getRealKeyByDefine("@",updatecol);
                        String realColProperty=updatecol.trim();
                        if(realColProperty.endsWith("__old")) realColProperty=realColProperty.substring(0,realColProperty.length()-"__old".length());
                        cb=rbean.getDbean().getColBeanByColProperty(realColProperty);
                        if(cb==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+updatecol+"不合法，没有取到其值对应的<col/>");
                        }
                        EditableReportParamBean paramBean=updateSqlAction.createParamBeanByColbean(cb.getProperty(),reportTypeKey,true,true);
                        if(cb.getDatatypeObj() instanceof ClobType||cb.getDatatypeObj() instanceof BlobType)
                        {
                            mLobParamsBean.put(cb.getColumn(),paramBean);
                        }else
                        {
                            hasNonLobTypeUpdateParams=true;
                            sqlBuffer.append(cb.getColumn()+"=?,");
                            lstParamsBean.add(paramBean);
                        }
                    }else
                    {
                        ((EditableReportSQLButtonDataBean)updateSqlAction.getOwnerUpdateBean()).setHasReportDataParams(true);
                        EditableReportParamBean paramBean=new EditableReportParamBean();
                        paramBean.setParamname(updatecol);
                        sqlBuffer.append(Tools.getRealKeyByDefine("@",updatecol)+"=?,");
                        lstParamsBean.add(paramBean);
                    }
                }
            }
        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',') sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        List<SQLUpdateAction> lstUpdateSqlActions=new ArrayList<SQLUpdateAction>();
        if(mLobParamsBean!=null&&mLobParamsBean.size()>0)
        {
            String tablename=configUpdateSql.substring("update".length()+1).trim();
            if(tablename.indexOf("(")>0) tablename=tablename.substring(0,tablename.indexOf("(")).trim();
            List<EditableReportParamBean> lstParamsBean2=new ArrayList<EditableReportParamBean>();
            StringBuilder sqlSelectLob=new StringBuilder("select ");
            StringBuilder sqlUpdateLob=new StringBuilder("update ");
            sqlUpdateLob.append(tablename).append(" set ");
            for(Entry<String,EditableReportParamBean> entry:mLobParamsBean.entrySet())
            {
                sqlSelectLob.append(entry.getKey()).append(",");
                lstParamsBean2.add(entry.getValue());
                sqlUpdateLob.append(entry.getKey()).append("=");
                if(entry.getValue().getDataTypeObj() instanceof ClobType)
                {
                    sqlUpdateLob.append("EMPTY_CLOB(),");
                }else
                {//BLOB
                    sqlUpdateLob.append("EMPTY_BLOB(),");
                }
            }
            if(sqlUpdateLob.charAt(sqlUpdateLob.length()-1)==',') sqlUpdateLob.deleteCharAt(sqlUpdateLob.length()-1);
            if(sqlSelectLob.charAt(sqlSelectLob.length()-1)==',') sqlSelectLob.deleteCharAt(sqlSelectLob.length()-1);
            sqlSelectLob.append(" from ");
            sqlSelectLob.append(tablename).append(" %where% for update");
            //先执行update 大字段=EMPTY_C/BLOB()
            SQLUpdateAction updateSqlActionNew=updateSqlAction.cloneWithAllDefaultValues();
            updateSqlActionNew.setSqlsp(sqlUpdateLob.toString());
            updateSqlActionNew.setLstParamBeans(new ArrayList<EditableReportParamBean>());
            updateSqlActionNew.setReturnValueParamname(null);
            lstUpdateSqlActions.add(updateSqlActionNew);
            updateSqlActionNew=updateSqlAction.cloneWithAllDefaultValues();
            updateSqlActionNew.setSqlsp(sqlSelectLob.toString());
            updateSqlActionNew.setLstParamBeans(lstParamsBean2);
            updateSqlActionNew.setReturnValueParamname(null);
            lstUpdateSqlActions.add(updateSqlActionNew);
        }
        if(hasNonLobTypeUpdateParams)
        {
            updateSqlAction.setSqlsp(sqlBuffer.toString());
            updateSqlAction.setLstParamBeans(lstParamsBean);
            lstUpdateSqlActions.add(updateSqlAction);
        }
        return lstUpdateSqlActions;
    }

    private boolean hasClobBlobColumn(String configUpdateSql,ReportBean rbean,String reportTypeKey,SQLUpdateAction updateSqlAction)
    {
        boolean isAutoReportData=updateSqlAction.getOwnerUpdateBean().isAutoReportdata();
        int idxleft=configUpdateSql.indexOf("(");
        if(idxleft<0||configUpdateSql.endsWith("()"))
        {//没有指定要更新的字段，则将所有从数据库取数据的<col/>（不包括displaytype为hidden的<col/>）全部更新到表中
            if(!isAutoReportData)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()
                        +"失败，在autoreportdata属性为false的<button/>中，不能配置insert into table这种格式的SQL语句");
            }
            for(ColBean cbean:rbean.getDbean().getLstCols())
            {
                if(cbean.getDatatypeObj() instanceof ClobType||cbean.getDatatypeObj() instanceof BlobType) return true;
            }
        }else
        {
            int idxright=configUpdateSql.lastIndexOf(")");
            if(idxright!=configUpdateSql.length()-1)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configUpdateSql+"不合法");
            }
            String cols=configUpdateSql.substring(idxleft+1,idxright);
            List<String> lstUpdateCols=Tools.parseStringToList(cols,",",new String[] { "'", "'" },false);
            String columnvalue=null;
            ColBean cb;
            for(String updatecol:lstUpdateCols)
            {
                if(updatecol==null||updatecol.trim().equals("")) continue;
                int idxequals=updatecol.indexOf("=");
                if(idxequals>0)
                {
                    columnvalue=updatecol.substring(idxequals+1).trim();
                    Object paramObjTmp=updateSqlAction.createEditParams(columnvalue,reportTypeKey);
                    if(paramObjTmp==null) continue;
                    IDataType datatypeObj=null;
                    if(Tools.isDefineKey("#",columnvalue))
                    {//是从<params/>中定义的变量中取值
                        datatypeObj=((EditableReportExternalValueBean)((EditableReportParamBean)paramObjTmp).getOwner()).getTypeObj();
                    }else if(isAutoReportData&&Tools.isDefineKey("@",columnvalue))
                    {
                        datatypeObj=((ColBean)((EditableReportParamBean)paramObjTmp).getOwner()).getDatatypeObj();
                    }
                    if(datatypeObj instanceof ClobType||datatypeObj instanceof BlobType) return true;
                }else
                {
                    if(!Tools.isDefineKey("@",updatecol))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configUpdateSql+"不合法，更新的字段值必须采用@{}括住");
                    }
                    if(isAutoReportData)
                    {
                        updatecol=Tools.getRealKeyByDefine("@",updatecol);
                        String realColProperty=updatecol.trim();
                        if(realColProperty.endsWith("__old")) realColProperty=realColProperty.substring(0,realColProperty.length()-"__old".length());
                        cb=rbean.getDbean().getColBeanByColProperty(realColProperty);
                        if(cb==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+updatecol+"不合法，没有取到其值对应的<col/>");
                        }
                        if(cb.getDatatypeObj() instanceof ClobType||cb.getDatatypeObj() instanceof BlobType) return true;
                    }
                }
            }
        }
        return false;
    }

    public String getSequenceValueByName(String sequencename)
    {
        return sequencename+".nextval";
    }

    public String getSequenceValueSql(String sequencename)
    {
        return "select "+sequencename+".nextval from dual";
    }

    public byte[] getBlobValue(ResultSet rs,String column) throws SQLException
    {
        BLOB blob=(BLOB)rs.getBlob(column);
        if(blob==null) return null;
        BufferedInputStream bin=null;
        try
        {
            bin=new BufferedInputStream(blob.getBinaryStream());
            return Tools.getBytesArrayFromInputStream(bin);
        }catch(Exception e)
        {
            log.error("读取二进制字段"+column+"失败",e);
            return null;
        }finally
        {
            if(bin!=null)
            {
                try
                {
                    bin.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] getBlobValue(ResultSet rs,int iindex) throws SQLException
    {
        BLOB blob=(BLOB)rs.getBlob(iindex);
        if(blob==null) return null;
        BufferedInputStream bin=null;
        try
        {
            bin=new BufferedInputStream(blob.getBinaryStream());
            return Tools.getBytesArrayFromInputStream(bin);
        }catch(Exception e)
        {
            log.error("读取二进制字段失败",e);
            return null;
        }finally
        {
            if(bin!=null)
            {
                try
                {
                    bin.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getClobValue(ResultSet rs,String column) throws SQLException
    {
        CLOB clob=(CLOB)rs.getClob(column);
        BufferedReader in=null;
        try
        {
            if(clob==null) return "";
            in=new BufferedReader(clob.getCharacterStream());
            StringBuffer sbuffer=new StringBuffer();
            String str=in.readLine();
            while(str!=null)
            {
                sbuffer.append(str).append("\n");
                str=in.readLine();
            }
            return sbuffer.toString();
        }catch(IOException e)
        {
            log.error("读取大字符串字段"+column+"失败",e);
            return null;
        }finally
        {
            if(in!=null)
            {
                try
                {
                    in.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getClobValue(ResultSet rs,int iindex) throws SQLException
    {
        CLOB clob=(CLOB)rs.getClob(iindex);
        BufferedReader in=null;
        try
        {
            if(clob==null) return "";
            in=new BufferedReader(clob.getCharacterStream());
            StringBuffer sbuffer=new StringBuffer();
            String str=in.readLine();
            while(str!=null)
            {
                sbuffer.append(str).append("\n");
                str=in.readLine();
            }
            return sbuffer.toString();
        }catch(IOException e)
        {
            log.error("读取大字符串字段失败",e);
            return null;
        }finally
        {
            if(in!=null)
            {
                try
                {
                    in.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setClobValue(int iindex,String value,PreparedStatement pstmt) throws SQLException
    {
        if(value==null) value="";
        BufferedReader reader=new BufferedReader(new StringReader(value));
        pstmt.setCharacterStream(iindex,reader,value.length());
    }

    public void setClobValueInSelectMode(Object value,oracle.sql.CLOB clob) throws SQLException
    {
        if(clob==null) return;
        String strvalue="";
        if(value!=null) strvalue=String.valueOf(value);
        BufferedWriter out=new BufferedWriter(clob.getCharacterOutputStream());
        BufferedReader in=new BufferedReader(new StringReader(strvalue));
        try
        {
            int c;
            while((c=in.read())!=-1)
            {
                out.write(c);
            }
            in.close();
            out.flush();
            out.close();
        }catch(IOException e)
        {
            throw new WabacusRuntimeException("将"+value+"写入CLOB字段失败",e);
        }
    }

    public void setBlobValueInSelectMode(Object value,oracle.sql.BLOB blob) throws SQLException
    {
        if(blob==null) return;
        InputStream in=null;
        if(value==null)
        {
            blob=null;
            return;
        }else if(value instanceof byte[])
        {
            in=Tools.getInputStreamFromBytesArray((byte[])value);
        }else if(value instanceof InputStream)
        {
            in=(InputStream)value;
        }else
        {
            throw new WabacusRuntimeException("将"+value+"写入BLOB字段失败，不是byte[]类型或InputStream类型");
        }
        BufferedOutputStream out=new BufferedOutputStream(blob.getBinaryOutputStream());
        try
        {
            int c;
            while((c=in.read())!=-1)
            {
                out.write(c);
            }
            in.close();
            out.close();
        }catch(IOException e)
        {
            throw new WabacusRuntimeException("将流"+value+"写入BLOB字段失败",e);
        }
    }

    public IDataType getWabacusDataTypeByColumnType(String columntype)
    {
        if(columntype==null||columntype.trim().equals("")) return null;
        columntype=columntype.toLowerCase().trim();
        IDataType dataTypeObj=null;
        if(columntype.indexOf("varchar")>=0||columntype.equals("char")||columntype.equals("nchar"))
        {
            dataTypeObj=new VarcharType();
        }else if(columntype.equals("integer"))
        {
            dataTypeObj=new IntType();
        }else if(columntype.equals("long raw")||columntype.equals("raw")||columntype.equals("blob"))
        {
            dataTypeObj=new BlobType();
        }else if(columntype.indexOf("date")>=0)
        {
            dataTypeObj=new DateType();
        }else if(columntype.equals("decimal")||columntype.equals("number"))
        {
            dataTypeObj=new BigdecimalType();
        }else if(columntype.equals("float"))
        {
            dataTypeObj=new FloatType();
        }else if(columntype.equals("real"))
        {
            dataTypeObj=new DoubleType();
        }else if(columntype.indexOf("timestamp")>=0)
        {
            dataTypeObj=new TimestampType();
        }else if(columntype.equals("clob")||columntype.equals("long")||columntype.equals("nclob"))
        {
            dataTypeObj=new ClobType();
        }else
        {
            log.warn("数据类型："+columntype+"不支持，将当做varchar类型");
            dataTypeObj=new VarcharType();
        }
        return dataTypeObj;
    }

    public String getStatementValue(IDataType dataTypeObj,String paramValue)
    {
        if(dataTypeObj instanceof AbsDateTimeType&&!Tools.isEmpty(paramValue))
        {
            return "to_date('"+paramValue+"','"+((AbsDateTimeType)dataTypeObj).getDateformat()+"')";
        }
        return super.getStatementValue(dataTypeObj,paramValue);
    }
    
}
