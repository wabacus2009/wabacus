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
package com.wabacus.system.intercept;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public class ColDataBean
{
    private AbsReportType reportTypeObj;
    
    private String value;
    
    private Object displayColBean;
    
    private AbsReportDataPojo rowDataObj;
    
    private int rowindex;
    
    private String styleproperty;
    
    private boolean readonly;
    
    public ColDataBean(AbsReportType reportTypeObj,Object displayColBean,AbsReportDataPojo rowDataObj,String value,String styleproperty,int rowindex)
    {
        this.reportTypeObj=reportTypeObj;
        this.displayColBean=displayColBean;
        this.rowDataObj=rowDataObj;
        this.value=value;
        this.styleproperty=styleproperty;
        this.rowindex=rowindex;
    }
    
    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public String getStyleproperty()
    {
        return styleproperty;
    }

    public void setStyleproperty(String styleproperty)
    {
        this.styleproperty=styleproperty;
    }

    public Object getDisplayColBean()
    {
        return displayColBean;
    }

    public int getRowindex()
    {
        return rowindex;
    }

    public boolean isReadonly()
    {
        return readonly;
    }

    public void setReadonly(boolean readonly)
    {
        this.readonly=readonly;
    }

    public Object getRowDataObj()
    {
        return rowDataObj;
    }

    public Object getColData(String property)
    {
        if(rowDataObj==null) return null;
        return rowDataObj.getColValue(property);
    }
    
    public boolean setColData(String property,Object valObj)
    {
        if(rowDataObj==null) return false;
        if(displayColBean instanceof ColBean)
        {
            if(property==null) property="";
            if(property.equals(((ColBean)this.displayColBean).getProperty()))
            {
                this.value=valObj==null?null:valObj.toString();
            }
        }
        return rowDataObj.setColValue(property,valObj);
    }
    
    public AbsReportType getReportTypeObj()
    {
        return reportTypeObj;
    }
}

