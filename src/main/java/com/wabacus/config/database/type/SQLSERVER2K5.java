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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.AbsConvertSQLevel;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.CompleteConvertSQLevel;
import com.wabacus.system.datatype.BigdecimalType;
import com.wabacus.system.datatype.BlobType;
import com.wabacus.system.datatype.BooleanType;
import com.wabacus.system.datatype.ByteType;
import com.wabacus.system.datatype.ClobType;
import com.wabacus.system.datatype.DateType;
import com.wabacus.system.datatype.DoubleType;
import com.wabacus.system.datatype.FloatType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.IntType;
import com.wabacus.system.datatype.LongType;
import com.wabacus.system.datatype.ShortType;
import com.wabacus.system.datatype.TimestampType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public class SQLSERVER2K5 extends AbsDatabaseType
{
    private static Log log=LogFactory.getLog(SQLSERVER2K5.class);

    public String constructSplitPageSql(AbsConvertSQLevel convertSqlObj)
    {
        String sql=convertSqlObj.getConvertedSql();
        String orderby=convertSqlObj.getOrderby();
        if(Tools.isEmpty(orderby))
        {
            orderby="(select 0)";
        }
        sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.orderbyPlaceHolder,"");
        boolean hasFilterCondition=false;
        if(sql.indexOf(SQLReportDataSetValueProvider.filterConditionPlaceHolder)>0)
        {
            hasFilterCondition=true;
        }
        sql=removeOuterWrap(sql);
        sql="select * from (select row_number() over(order by "+orderby+") as ROWID,* from ("+sql+") as jd_temp_tbl1";
        if(hasFilterCondition)
        {
            sql=sql+" where "+SQLReportDataSetValueProvider.filterConditionPlaceHolder;
        }
        sql=sql+") as jd_temp_tbl2 where ROWID > "+SQLReportDataSetValueProvider.startRowNumPlaceHolder+" AND ROWID<= "+SQLReportDataSetValueProvider.endRowNumPlaceHolder;
        return sql;
    }

    public String constructSplitPageSql(AbsConvertSQLevel convertSqlObj,String dynorderby)
    {
        dynorderby=convertSqlObj.mixDynorderbyAndConfigOrderbyCols(dynorderby);
        String sql=convertSqlObj.getConvertedSql();
        sql=Tools.replaceAll(sql,SQLReportDataSetValueProvider.orderbyPlaceHolder,"");
        boolean hasFilterCondition=false;
        if(sql.indexOf(SQLReportDataSetValueProvider.filterConditionPlaceHolder)>0)
        {
            hasFilterCondition=true;
        }
        sql=removeOuterWrap(sql);
        sql="select * from (select row_number() over(order by "+dynorderby+") as ROWID,* from ("+sql+") as jd_temp_tbl1";
        if(hasFilterCondition)
        {
            sql=sql+" where "+SQLReportDataSetValueProvider.filterConditionPlaceHolder;
        }
        sql=sql+") as jd_temp_tbl2 where ROWID > "+SQLReportDataSetValueProvider.startRowNumPlaceHolder+" AND ROWID<= "+SQLReportDataSetValueProvider.endRowNumPlaceHolder;
        return sql;
    }

    private String removeOuterWrap(String sql)
    {
        int idxprex=sql.indexOf(CompleteConvertSQLevel.sqlprex);
        int idxpostsuffix=sql.indexOf(CompleteConvertSQLevel.sqlsuffix);
        if(idxprex==0&&idxpostsuffix>0)
        {
            sql=sql.substring(CompleteConvertSQLevel.sqlprex.length(),idxpostsuffix);
        }
        return sql;
    }

    public String getSequenceValueByName(String sequencename)
    {
        log.warn("SqlServer数据库不支持序列（sequence）的配置，只有支持sequence的数据库才支持从序列中取值，比如Oracle、DB2等");
        return "";
    }
    
    public String getSequenceValueSql(String sequencename)
    {
       throw new WabacusRuntimeException("SqlServer数据库不支持序列（sequence）的配置，配置为从sequence取数据的报表将无法正常保存");
    }
    
    public IDataType getWabacusDataTypeByColumnType(String columntype)
    {
        if(columntype==null||columntype.trim().equals("")) return null;
        columntype=columntype.toLowerCase().trim();
        IDataType dataTypeObj=null;
        if(columntype.equals("varchar")||columntype.equals("nvarchar")||columntype.equals("char")||columntype.equals("nchar"))
        {
            dataTypeObj=new VarcharType();
        }else if(columntype.equals("bit"))
        {
            dataTypeObj=new BooleanType();
        }else if(columntype.equals("tinyint"))
        {
            dataTypeObj=new ByteType();
        }else if(columntype.equals("smallint"))
        {
            dataTypeObj=new ShortType();
        }else if(columntype.equals("int"))
        {
            dataTypeObj=new IntType();
        }else if(columntype.equals("bigint"))
        {
            dataTypeObj=new LongType();
        }else if(columntype.equals("binary")||columntype.equals("varbinary")||columntype.equals("image"))
        {
            dataTypeObj=new BlobType();
        }else if(columntype.indexOf("datetime")>=0)
        {
            dataTypeObj=new DateType();
        }else if(columntype.equals("decimal")||columntype.equals("numeric")||columntype.equals("money")||columntype.equals("smallmoney"))
        {
            dataTypeObj=new BigdecimalType();
        }else if(columntype.equals("float"))
        {
            dataTypeObj=new FloatType();
        }else if(columntype.equals("double")||columntype.equals("real"))
        {
            dataTypeObj=new DoubleType();
        }else if(columntype.equals("timestamp"))
        {
            dataTypeObj=new TimestampType();
        }else if(columntype.equals("text")||columntype.equals("ntext"))
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
