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
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.database.type.AbsDatabaseType;

public class LongType extends AbsNumberType
{
    private final static Log log=LogFactory.getLog(LongType.class);

    private final static Map<String,AbsNumberType> mLongTypeObjects=new HashMap<String,AbsNumberType>();
    
    public Object getColumnValue(ResultSet rs,String column,AbsDatabaseType dbtype)
            throws SQLException
    {
        return Long.valueOf(rs.getLong(column));
    }

    public Object getColumnValue(ResultSet rs,int iindex,AbsDatabaseType dbtype)
            throws SQLException
    {
        return Long.valueOf(rs.getLong(iindex));
    }

    public void setPreparedStatementValue(int iindex,String value,PreparedStatement pstmt,
            AbsDatabaseType dbtype) throws SQLException
    {
        log.debug("setLong("+iindex+","+value+")");
        Object objTmp=label2value(value);
        if(objTmp==null)
        {
            pstmt.setObject(iindex,null,java.sql.Types.BIGINT);
        }else
        {
            pstmt.setLong(iindex,(Long)objTmp);
        }
    }


    public Class getJavaTypeClass()
    {
        //        return long.class;
        return Long.class;
    }

    public Object label2value(String label)
    {
        if(label==null||label.trim().equals("")) return null;
        if(this.numberformat!=null&&!this.numberformat.trim().equals(""))
        {
            return Long.valueOf(this.getNumber(label.trim()).longValue());
        }else
        {
            int idxdot=label.indexOf(".");
            if(idxdot==0)
            {
                label="0";
            }else if(idxdot>0)
            {
                label=label.substring(0,idxdot).trim();
                if(label.equals("")) label="0";
            }
            return Long.valueOf(label.trim());
        }
    }

    public String value2label(Object value)
    {
        if(value==null) return "";
        if(!(value instanceof Long)) return String.valueOf(value);
        if(this.numberformat!=null&&!this.numberformat.trim().equals(""))
        {
            DecimalFormat df=new DecimalFormat(this.numberformat);
            return df.format((Long)value);
        }else
        {
            return String.valueOf(value);
        }
    }

    protected Map<String,AbsNumberType> getAllMNumberTypeObjects()
    {
        return mLongTypeObjects;
    }
}
