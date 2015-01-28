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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import COM.ibm.db2.app.Blob;
import COM.ibm.db2.app.Clob;

import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.AbsConvertSQLevel;
import com.wabacus.system.datatype.BigdecimalType;
import com.wabacus.system.datatype.BlobType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.system.datatype.DateType;
import com.wabacus.system.datatype.DoubleType;
import com.wabacus.system.datatype.FloatType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.IntType;
import com.wabacus.system.datatype.LongType;
import com.wabacus.system.datatype.ShortType;
import com.wabacus.system.datatype.TimeType;
import com.wabacus.system.datatype.TimestampType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public class DB2 extends AbsDatabaseType
{
    private static Log log=LogFactory.getLog(DB2.class);

    public String constructSplitPageSql(AbsConvertSQLevel convertSqlObj)
    {
        String sql=convertSqlObj.getConvertedSql();
        String orderby="";
        if(sql.indexOf(SQLReportDataSetValueProvider.orderbyPlaceHolder)>0)
        {
            sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.orderbyPlaceHolder,"");
            orderby="order by "+convertSqlObj.getOrderby();
        }
        sql="SELECT * FROM(SELECT jd_temp_tbl1.*, rownumber() OVER("+orderby+") as ROWID FROM("+sql
                +") as jd_temp_tbl1) as jd_temp_tbl2 WHERE jd_temp_tbl2.ROWID<="+SQLReportDataSetValueProvider.endRowNumPlaceHolder+" and jd_temp_tbl2.ROWID>"+SQLReportDataSetValueProvider.startRowNumPlaceHolder;
        return sql;
    }

    public String constructSplitPageSql(AbsConvertSQLevel convertSqlObj,String dynorderby)
    {
        String sql=convertSqlObj.getConvertedSql();
        dynorderby=convertSqlObj.mixDynorderbyAndConfigOrderbyCols(dynorderby);
        sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.orderbyPlaceHolder,"");
        sql="select * from (select rownumber() over(order by "+dynorderby+") as ROWID,jd_temp_tbl1.* from("+sql
                +") as jd_temp_tbl1) as jd_temp_tbl2 where ROWID > "+SQLReportDataSetValueProvider.startRowNumPlaceHolder+" AND ROWID<= "+SQLReportDataSetValueProvider.endRowNumPlaceHolder;
        return sql;
    }

    public String getSequenceValueByName(String sequencename)
    {
        return "nextval for "+sequencename;
    }

    public String getSequenceValueSql(String sequencename)
    {
        return "select  nextval for "+sequencename+" from sysibm.sysdummy1";
    }

    public String getClobValue(ResultSet rs,String column) throws SQLException
    {
        Clob clob=(Clob)rs.getClob(column);
        if(clob==null) return "";
        BufferedReader in=null;
        try
        {
            in=new BufferedReader(clob.getReader());
            StringBuffer sbuffer=new StringBuffer();
            String str=in.readLine();
            while(str!=null)
            {
                sbuffer.append(str).append("\n");
                str=in.readLine();
            }
            return sbuffer.toString();
        }catch(Exception e)
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
        Clob clob=(Clob)rs.getClob(iindex);
        if(clob==null) return "";
        BufferedReader in=null;
        try
        {
            in=new BufferedReader(clob.getReader());
            StringBuffer sbuffer=new StringBuffer();
            String str=in.readLine();
            while(str!=null)
            {
                sbuffer.append(str).append("\n");
                str=in.readLine();
            }
            return sbuffer.toString();
        }catch(Exception e)
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

    public byte[] getBlobValue(ResultSet rs,String column) throws SQLException
    {
        Blob blob=(Blob)rs.getBlob(column);
        if(blob==null) return null;
        BufferedInputStream bin=null;
        try
        {
            bin=new BufferedInputStream(blob.getInputStream());
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
        Blob blob=(Blob)rs.getBlob(iindex);
        if(blob==null) return null;
        BufferedInputStream bin=null;
        try
        {
            bin=new BufferedInputStream(blob.getInputStream());
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

    public void setClobValue(int iindex,String value,PreparedStatement pstmt) throws SQLException
    {
        if(value==null) value="";
        StringBufferInputStream sbis=new StringBufferInputStream(value);
        pstmt.setAsciiStream(iindex,sbis,sbis.available());
    }

    public IDataType getWabacusDataTypeByColumnType(String columntype)
    {
        if(columntype==null||columntype.trim().equals("")) return null;
        columntype=columntype.toLowerCase().trim();
        IDataType dataTypeObj=null;
        if(columntype.indexOf("varchar")>=0||columntype.equals("character")||columntype.equals("char")||columntype.indexOf("graphic")>=0)
        {
            dataTypeObj=new VarcharType();
        }else if(columntype.equals("smallint"))
        {
            dataTypeObj=new ShortType();
        }else if(columntype.equals("int"))
        {
            dataTypeObj=new IntType();
        }else if(columntype.equals("bigint"))
        {
            dataTypeObj=new LongType();
        }else if(columntype.equals("blob"))
        {
            dataTypeObj=new BlobType();
        }else if(columntype.equals("date"))
        {
            dataTypeObj=new DateType();
        }else if(columntype.equals("time"))
        {
            dataTypeObj=new TimeType();
        }else if(columntype.equals("decimal")||columntype.equals("numeric"))
        {
            dataTypeObj=new BigdecimalType();
        }else if(columntype.equals("real"))
        {
            dataTypeObj=new FloatType();
        }else if(columntype.equals("double"))
        {
            dataTypeObj=new DoubleType();
        }else if(columntype.equals("timestamp"))
        {
            dataTypeObj=new TimestampType();
        }else if(columntype.equals("clob")||columntype.equals("dbclob"))
        {
            dataTypeObj=new ClobType();
        }else
        {
            log.warn("数据类型："+columntype+"不支持，将当做varchar类型");
            dataTypeObj=new VarcharType();
        }
        return dataTypeObj;
    }
}
