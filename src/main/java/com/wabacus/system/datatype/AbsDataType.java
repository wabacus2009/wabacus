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

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import org.dom4j.Element;

import com.wabacus.exception.WabacusConfigLoadingException;

public abstract class AbsDataType implements IDataType
{

    public void loadTypeConfig(Element eleDataType)
    {

    }

    public IDataType setUserConfigString(String configstring)
    {
        return this;
    }

    public String value2label(Object value)
    {
        if(value==null) return "";
        return String.valueOf(value);
    }

    public CtClass getCreatedClass(ClassPool pool)
    {
        try
        {
            return pool.get(getJavaTypeClass().getName());
        }catch(NotFoundException e)
        {
            throw new WabacusConfigLoadingException("没有找到"+getJavaTypeClass().getName()
                    +"类型，无法创建此类型的成员变量",e);
        }
    }
}
