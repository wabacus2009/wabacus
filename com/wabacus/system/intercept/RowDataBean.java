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

import java.util.List;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public class RowDataBean
{
    private AbsReportType reportTypeObj;
    
    private String rowstyleproperty;//显示当前行时的<tr/>样式字符串，比如设置为 bgcolor='red' height='30px'。这样显示当前行的<tr/>时就会有<tr bgcolor='red' height='30px'.../>

    private int rowindex;
    
    private AbsReportDataPojo rowDataObj;
    
    private int colspans;
    
    private List lstColBeans;//对于列表报表，这里指定本报表显示的所有数据列配置对象；对于细览报表，这里指定这一行中显示的所有数据列的配置对象（不是所有<col/>，而是这一个<tr/>显示的列）
    
    private String insertDisplayRowHtml;
    
    private boolean shouldDisplayThisRow;//是否应该显示当前行

    private boolean readonly;

    private boolean isSelectedRow;
    
    private boolean disableSelectedRow;

    public RowDataBean(AbsReportType reportTypeObj,String rowstyleproperty,List lstColBeans,AbsReportDataPojo rowDataObj,int rowindex,int colspans)
    {
        this.reportTypeObj=reportTypeObj;
        this.rowstyleproperty=rowstyleproperty;
        this.lstColBeans=lstColBeans;
        this.rowDataObj=rowDataObj;
        this.rowindex=rowindex;
        this.colspans=colspans;
        this.shouldDisplayThisRow=true;
    }
    
    public AbsReportType getReportTypeObj()
    {
        return reportTypeObj;
    }

    public int getRowindex()
    {
        return rowindex;
    }

    public int getColspans()
    {
        return colspans;
    }

    public List getLstColBeans()
    {
        return lstColBeans;
    }

    public String getInsertDisplayRowHtml()
    {
        return insertDisplayRowHtml;
    }

    public void setInsertDisplayRowHtml(String insertDisplayRowHtml)
    {
        this.insertDisplayRowHtml=insertDisplayRowHtml;
    }

    public String getRowstyleproperty()
    {
        return rowstyleproperty;
    }

    public void setRowstyleproperty(String rowstyleproperty)
    {
        this.rowstyleproperty=rowstyleproperty;
    }

    public boolean isShouldDisplayThisRow()
    {
        return shouldDisplayThisRow;
    }

    public void setShouldDisplayThisRow(boolean shouldDisplayThisRow)
    {
        this.shouldDisplayThisRow=shouldDisplayThisRow;
    }

    public boolean isReadonly()
    {
        return readonly;
    }

    public void setReadonly(boolean readonly)
    {
        this.readonly=readonly;
    }

    public boolean isSelectedRow()
    {
        return isSelectedRow;
    }

    public void setSelectedRow(boolean isSelectedRow)
    {
        if(disableSelectedRow)
        {
            this.isSelectedRow=false;
        }else
        {
            this.isSelectedRow=isSelectedRow;
            reportTypeObj.getReportRequest().addListReportWithDefaultSelectedRows(reportTypeObj.getReportBean(),isSelectedRow);
        }
    }
    
    public Object getRowDataObj()
    {
        return rowDataObj;
    }

    public boolean isDisableSelectedRow()
    {
        return disableSelectedRow;
    }

    public void setDisableSelectedRow(boolean disableSelectedRow)
    {
        if(disableSelectedRow)
        {//如果是禁止选中
            this.isSelectedRow=false;
        }
        this.disableSelectedRow=disableSelectedRow;
    }

    public Object getColData(String property)
    {
        if(rowDataObj==null) return null;
        return rowDataObj.getColValue(property);
    }
    
    public boolean setColData(String property,Object valObj)
    {
        if(rowDataObj==null) return false;
        return rowDataObj.setColValue(property,valObj);
    }
}
