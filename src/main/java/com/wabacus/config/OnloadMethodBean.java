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
package com.wabacus.config;

import java.util.HashMap;
import java.util.Map;

import com.wabacus.util.Consts_Private;

public class OnloadMethodBean implements Comparable<OnloadMethodBean>,Cloneable
{
    private String type;

    private String method;

    public OnloadMethodBean(String type,String method)
    {
        this.type=type;
        this.method=method;
    }
    
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type=type;
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method=method;
    }

    private final static Map<String,Integer> mOnloadTypeInvokeOrder=new HashMap<String,Integer>();

    static
    {
        mOnloadTypeInvokeOrder.put(Consts_Private.ONlOAD_CURVETITLE,2);
        mOnloadTypeInvokeOrder.put(Consts_Private.ONlOAD_IMGSCROLL,2);
//        mOnloadTypeInvokeOrder.put(Consts_Private.ONLOAD_AFTERSAVE,4);
        mOnloadTypeInvokeOrder.put(Consts_Private.ONLOAD_CONFIG,4);
        mOnloadTypeInvokeOrder.put(Consts_Private.ONLOAD_REFRESHSLAVE,5);
    }

    public int compareTo(OnloadMethodBean comparedObj)
    {
        if(!mOnloadTypeInvokeOrder.containsKey(this.type)||!mOnloadTypeInvokeOrder.containsKey(comparedObj.getType()))
        {
            return 1;
        }
        if(mOnloadTypeInvokeOrder.get(this.type)>mOnloadTypeInvokeOrder.get(comparedObj.getType()))
        {
            return 1;
        }else if(mOnloadTypeInvokeOrder.get(this.type)<mOnloadTypeInvokeOrder.get(comparedObj.getType()))
        {
            return -1;
        }
        return 0;
    }

    public Object clone()
    {
        try
        {
            return super.clone();
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return this;
        }
    }
}
