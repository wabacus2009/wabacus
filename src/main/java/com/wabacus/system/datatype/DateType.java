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
package com.wabacus.system.datatype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.database.type.AbsDatabaseType;

public class DateType extends AbsDateTimeType
{
    private final static Log log=LogFactory.getLog(DateType.class);

    private final static Map<String,AbsDateTimeType> mDatetimeTypeObjects=new HashMap<String,AbsDateTimeType>();
    
    public Object label2value(String label)
    {
        if(label==null||label.trim().equals("")) return null;
        try
        {
            Date date=new SimpleDateFormat(dateformat).parse(label.trim());
            return new java.sql.Date(date.getTime());
        }catch(ParseException e)
        {
            log.error(label+"非法的日期格式,不能格式化为"+dateformat+"形式的日期类型",e);
            return null;
        }
    }

    public String value2label(Object value)
    {
        if(value==null) return "";
        if(!(value instanceof Date))
        {
            return String.valueOf(value);
        }
        return new SimpleDateFormat(dateformat).format((Date)value);
    }

    public Object getColumnValue(ResultSet rs,String column,AbsDatabaseType dbtype)
            throws SQLException
    {
        java.sql.Date dd=rs.getDate(column);
        if(dd==null) return null;
        return new Date(dd.getTime());
    }

    public Object getColumnValue(ResultSet rs,int iindex,AbsDatabaseType dbtype)
            throws SQLException
    {
        java.sql.Date dd=rs.getDate(iindex);
        if(dd==null) return null;
        return new Date(dd.getTime());
    }

    public void setPreparedStatementValue(int iindex,String value,PreparedStatement pstmt,
            AbsDatabaseType dbtype) throws SQLException
    {
        log.debug("setDate("+iindex+","+value+")");
        pstmt.setDate(iindex,(java.sql.Date)label2value(value));
    }

    public void setPreparedStatementValue(int index,Date dateValue,PreparedStatement pstmt)
            throws SQLException
    {
        log.debug("setDate("+index+","+dateValue+")");
        if(dateValue==null)
        {
            pstmt.setDate(index,null);
        }else
        {
            pstmt.setDate(index,new java.sql.Date(dateValue.getTime()));
        }
    }
    
    public Class getJavaTypeClass()
    {
        return Date.class;
    }
    
    protected Map<String,AbsDateTimeType> getMDatetimeTypeObjects()
    {
        return mDatetimeTypeObjects;
    }
}
