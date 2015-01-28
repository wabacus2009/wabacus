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
package com.wabacus.system.component.application.report.abstractreport.configbean;

import java.lang.reflect.Method;
import java.util.List;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;

public class AbsListReportSubDisplayColBean implements Cloneable
{
    private String property;
    private int plainexcel_startcolidx;
    
    private int plainexcel_colspan;

    private String valuestyleproperty;
    
    private List<String> lstDynValuestylepropertyParts;
    
    private Method getMethod;//此列对应的pojo类中的get方法对象
    
    public int getPlainexcel_startcolidx()
    {
        return plainexcel_startcolidx;
    }

    public void setPlainexcel_startcolidx(int plainexcel_startcolidx)
    {
        this.plainexcel_startcolidx=plainexcel_startcolidx;
    }

    public int getPlainexcel_colspan()
    {
        return plainexcel_colspan;
    }

    public void setPlainexcel_colspan(int plainexcel_colspan)
    {
        this.plainexcel_colspan=plainexcel_colspan;
    }
    
    public String getValuestyleproperty(ReportRequest rrequest,boolean isStaticPart)
    {
        if(isStaticPart) return this.valuestyleproperty;
        return WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.valuestyleproperty,this.lstDynValuestylepropertyParts,"");
    }

    public void setValuestyleproperty(String valuestyleproperty,boolean isStaticPart)
    {
        if(isStaticPart)
        {
            this.valuestyleproperty=valuestyleproperty;
        }else
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(valuestyleproperty);
            this.valuestyleproperty=(String)objArr[0];
            this.lstDynValuestylepropertyParts=(List<String>)objArr[1];
        }
    }

    public String getProperty()
    {
        return property;
    }

    public void setProperty(String property)
    {
        this.property=property;
    }
   
    public Method getGetMethod()
    {
        return getMethod;
    }

    public void setGetMethod(Method getMethod)
    {
        this.getMethod=getMethod;
    }

    public Object clone()
    {
        try
        {
            AbsListReportSubDisplayColBean newBean=(AbsListReportSubDisplayColBean)super.clone();
            return newBean;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
