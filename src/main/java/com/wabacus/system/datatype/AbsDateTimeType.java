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
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import org.dom4j.Element;

public abstract class AbsDateTimeType extends AbsDataType
{
    protected String dateformat;

    public String getDateformat()
    {
        return dateformat;
    }

    public void loadTypeConfig(Element eleDataType)
    {
        if(eleDataType==null) return;
        dateformat=eleDataType.attributeValue("defaultformat");
        if(dateformat!=null) dateformat=dateformat.trim();
    }

    public IDataType setUserConfigString(String configstring)
    {
        if(configstring==null||configstring.trim().equals("")) return this;
        configstring=configstring.trim();
        if(getMDatetimeTypeObjects().containsKey(configstring))
        {
            return getMDatetimeTypeObjects().get(configstring);
        }
        AbsDateTimeType newDateType=null;
        try
        {
            newDateType=(AbsDateTimeType)super.clone();
            newDateType.dateformat=configstring;
            getMDatetimeTypeObjects().put(configstring,newDateType);
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        return newDateType;
    }
    
    protected abstract Map<String,AbsDateTimeType> getMDatetimeTypeObjects();
    
    public abstract void setPreparedStatementValue(int index,Date dateValue,PreparedStatement pstmt)
            throws SQLException;
}
