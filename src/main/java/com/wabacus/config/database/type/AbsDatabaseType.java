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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.AbsConvertSQLevel;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.dataset.update.action.rationaldb.SQLInsertAction;
import com.wabacus.system.dataset.update.action.rationaldb.SQLUpdateAction;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public abstract class AbsDatabaseType
{
    public abstract String constructSplitPageSql(AbsConvertSQLevel convertSqlObj);

    public abstract String constructSplitPageSql(AbsConvertSQLevel convertSqlObj,String dynorderby);

    public abstract String getSequenceValueByName(String sequencename);
    
    public abstract String getSequenceValueSql(String sequencename);
    
    public String getLowerMethodname()
    {
        return "lower";
    }
    
    public String getClobValue(ResultSet rs,String column) throws SQLException
    {
        return rs.getString(column);
    }

    public String getClobValue(ResultSet rs,int iindex) throws SQLException
    {
        return rs.getString(iindex);
    }

    public byte[] getBlobValue(ResultSet rs,String column) throws SQLException
    {
        InputStream in=null;
        try
        {
            in=rs.getBinaryStream(column);
            if(in==null) return null;
            return Tools.getBytesArrayFromInputStream(in);
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

    public byte[] getBlobValue(ResultSet rs,int iindex) throws SQLException
    {
        InputStream in=null;
        try
        {
            in=rs.getBinaryStream(iindex);
            if(in==null) return null;
            return Tools.getBytesArrayFromInputStream(in);
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
        pstmt.setString(iindex,value);
    }

    public void setBlobValue(int iindex,byte[] value,PreparedStatement pstmt) throws SQLException
    {
        if(value==null)
        {
            pstmt.setBinaryStream(iindex,null,0);
        }else
        {
            try
            {
                ByteArrayInputStream in=Tools.getInputStreamFromBytesArray(value);
                pstmt.setBinaryStream(iindex,in,in.available());
                in.close();
            }catch(IOException e)
            {
                throw new WabacusRuntimeException("将字节流写入数据库失败",e);
            }
        }
    }

    public void setBlobValue(int iindex,InputStream in,PreparedStatement pstmt) throws SQLException
    {
        if(in==null)
        {
            pstmt.setBinaryStream(iindex,null,0);
        }else
        {
            try
            {
                pstmt.setBinaryStream(iindex,in,in.available());
            }catch(IOException e)
            {
                throw new WabacusRuntimeException("将字节流写入数据库失败",e);
            }
        }
    }

    public void constructInsertSql(String configInsertSql,ReportBean rbean,String reportTypeKey,SQLInsertAction insertSqlAction,List<AbsUpdateAction> lstActionsResult)
    {
        insertSqlAction.constructInsertSql(configInsertSql,rbean,reportTypeKey,lstActionsResult);
    }

    public List<SQLUpdateAction> constructUpdateSql(String configUpdateSql,ReportBean rbean,String reportTypeKey,SQLUpdateAction updateSqlAction)
    {
        return updateSqlAction.constructUpdateSql(configUpdateSql,rbean,reportTypeKey);
    }
    
    public abstract IDataType getWabacusDataTypeByColumnType(String columntype);

    public String getStatementValue(IDataType dataTypeObj,String paramValue)
    {
        if(paramValue==null)
        {
            paramValue="null";
        }else if(paramValue.trim().equals(""))
        {
            if(dataTypeObj instanceof VarcharType||dataTypeObj instanceof ClobType)
            {
                paramValue="'"+paramValue+"'";
            }else
            {
                paramValue="null";
            }
        }else if(dataTypeObj instanceof VarcharType||dataTypeObj instanceof ClobType||dataTypeObj instanceof AbsDateTimeType)
        {
            paramValue="'"+paramValue+"'";
        }
        return paramValue;
    }

    /*
        public void setPreparedStatementValue(int iindex,Object value,int type,PreparedStatement pstmt)
                throws SQLException
        {
            if((value instanceof String)&&type!=DBColumnType.VARCHAR&&type!=DBColumnType.CLOB)
            {
                value=DBColumnType.convertStringValueToRealDBTypeValue((String)value,type);
            }
            switch (type)
            {
                case DBColumnType.VARCHAR:
                    String str=(String)value;
                    if(str==null) str="";
                    log.debug("setString("+iindex+","+str+")");
                    pstmt.setString(iindex,str);
                    break;
                case DBColumnType.BYTE:
                    byte b=0;
                    if(value!=null)
                    {
                        b=(Byte)value;
                    }
                    log.debug("setByte("+iindex+","+b+")");
                    pstmt.setByte(iindex,b);
                    break;
                case DBColumnType.BOOLEAN:
                    boolean bl=false;
                    if(value!=null)
                    {
                        bl=(Boolean)value;
                    }
                    log.debug("setBoolean("+iindex+","+bl+")");
                    pstmt.setBoolean(iindex,bl);
                    break;
                case DBColumnType.SHORT:
                    short s=0;
                    if(value!=null)
                    {
                        s=(Short)value;
                    }
                    log.debug("setShort("+iindex+","+s+")");
                    pstmt.setShort(iindex,s);
                    break;
                case DBColumnType.INT:
                    int i=0;
                    if(value!=null)
                    {
                        i=(Integer)value;
                    }
                    log.debug("setInt("+iindex+","+i+")");
                    pstmt.setInt(iindex,i);
                    break;
                case DBColumnType.LONG:
                    long l=0L;
                    if(value!=null)
                    {
                        l=(Long)value;
                    }
                    log.debug("setLong("+iindex+","+l+")");
                    pstmt.setLong(iindex,l);
                    break;
                case DBColumnType.FLOAT:
                    float f=0.0F;
                    if(value!=null)
                    {
                        f=(Float)value;
                    }
                    log.debug("setFloat("+iindex+","+f+")");
                    pstmt.setFloat(iindex,f);
                    break;
                case DBColumnType.DOUBLE:
                    double d=0.0D;
                    if(value!=null)
                    {
                        d=(Double)value;
                    }
                    log.debug("setDouble("+iindex+","+d+")");
                    pstmt.setDouble(iindex,d);
                    break;
                case DBColumnType.BIGDECIMAL:
                    BigDecimal bd=null;
                    if(value!=null)
                    {
                        bd=(BigDecimal)value;
                    }else
                    {
                        bd=new BigDecimal(0);
                    }
                    log.debug("setBigDecimal("+iindex+","+bd+")");
                    pstmt.setBigDecimal(iindex,bd);
                    break;
                case DBColumnType.BLOB:
                    log.debug("setBlob("+iindex+","+value+")");
                    if(value==null||(value instanceof byte[]))
                    {
                        setBlobValue(iindex,(byte[])value,pstmt);
                    }else if(value instanceof InputStream)
                    {
                        setBlobValue(iindex,(InputStream)value,pstmt);
                    }else
                    {
                        throw new WabacusUpdateReportDataException("将"+value
                                +"写入BLOB字段失败，不是byte[]类型或InputStream类型");
                    }
                    break;
                case DBColumnType.CLOB:
                    log.debug("setClob("+iindex+","+value+")");
                    setClobValue(iindex,(String)value,pstmt);
                    break;
                case DBColumnType.DATE:
                case DBColumnType.CDATE:
                    log.debug("setDate("+iindex+","+value+")");
                    pstmt.setDate(iindex,(java.sql.Date)value);
                    break;
                case DBColumnType.TIME:
                case DBColumnType.CTIME:
                    log.debug("setTime("+iindex+","+value+")");
                    pstmt.setTime(iindex,(java.sql.Time)value);
                    break;
                case DBColumnType.TIMESTAMP:
                case DBColumnType.CTIMESTAMP:
                    log.debug("setTimestamp("+iindex+","+value+")");
                    pstmt.setTimestamp(iindex,(java.sql.Timestamp)value);
                    break;
                default:
                    String str2=null;
                    if(value!=null)
                    {
                        str2=String.valueOf(value);
                    }else
                    {
                        str2="";
                    }
                    log.debug("setString("+iindex+","+str2+")");
                    pstmt.setString(iindex,str2);
            }
        }*/
}
