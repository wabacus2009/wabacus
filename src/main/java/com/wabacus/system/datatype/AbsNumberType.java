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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

public abstract class AbsNumberType extends AbsDataType
{
    private final static Log log=LogFactory.getLog(AbsNumberType.class);

    protected String numberformat;

    public void loadTypeConfig(Element eleDataType)
    {
        if(eleDataType==null) return;
        numberformat=eleDataType.attributeValue("defaultformat");
        numberformat=numberformat==null?"":numberformat.trim();
        if(numberformat.equals("")) numberformat=null;
    }

    public IDataType setUserConfigString(String configstring)
    {
        if(configstring==null||configstring.trim().equals("")) return this;
        configstring=configstring.trim();
        if(getAllMNumberTypeObjects().containsKey(configstring))
        {
            return getAllMNumberTypeObjects().get(configstring);
        }
        AbsNumberType newNumberType=null;
        try
        {
            newNumberType=(AbsNumberType)super.clone();
            newNumberType.numberformat=configstring;
            getAllMNumberTypeObjects().put(configstring,newNumberType);
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        return newNumberType;
    }

    protected Number getNumber(String strvalue)
    {
        if(strvalue==null||strvalue.trim().equals("")) strvalue="0";
        if(this.numberformat!=null&&!this.numberformat.trim().equals(""))
        {
            DecimalFormat df=new DecimalFormat(this.numberformat);
            Number n=0;
            try
            {
                n=df.parse(strvalue.trim());
            }catch(ParseException e)
            {
                log.error(strvalue+"不是合法的"+this.numberformat+"格式的字节",e);
                n=0;
            }
            return n;
        }else
        {
            return null;
        }
    }
    
    protected abstract Map<String,AbsNumberType> getAllMNumberTypeObjects();
}
