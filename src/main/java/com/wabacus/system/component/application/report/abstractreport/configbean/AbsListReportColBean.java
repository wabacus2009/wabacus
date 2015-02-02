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

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.system.ReportRequest;

public class AbsListReportColBean extends AbsExtendConfigBean
{
    private int sequenceStartNum;
    
    private boolean rowgroup=false;

    private boolean requireClickOrderby=false;//是否需要点击标题进行排序功能，配置此属性的<col/>必须配置有column属性

    private String slaveReportParamName;

//    private String curvelabelup;//如果当前列标题是显示成折线，则此处存放当前列的折线上面的标题。
//    private String curvelabeldown;//如果当前列标题是显示成折线，则此处存放当前列的折线上面的标题。
//    
//    private String curvecolor;//折线标题中折线的颜色
//    private boolean isCurveLabel;//当前列是否参与了显示折线标题
    
    private boolean isRoworderValue=false;//当前<col/>是否需要在行排序时传入后台，此时会在其所属的<td/>中显示name和value值
    
    private String roworder_inputboxstyleproperty;
    
    private boolean isFixedCol;
    
    private AbsListReportFilterBean filterBean;

    public AbsListReportColBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public int getSequenceStartNum()
    {
        return sequenceStartNum;
    }

    public void setSequenceStartNum(int sequenceStartNum)
    {
        this.sequenceStartNum=sequenceStartNum;
    }

    public boolean isRequireClickOrderby()
    {
        return requireClickOrderby;
    }

    public void setRequireClickOrderby(boolean requireClickOrderby)
    {
        this.requireClickOrderby=requireClickOrderby;
    }

    public boolean isRowgroup()
    {
        return rowgroup;
    }

    public void setRowgroup(boolean rowgroup)
    {
        this.rowgroup=rowgroup;
    }

    public String getSlaveReportParamName()
    {
        return slaveReportParamName;
    }

    public void setSlaveReportParamName(String slaveReportParamName)
    {
        this.slaveReportParamName=slaveReportParamName;
    }

//    {
//    {
//    {
//    {
//    {
//    {
//    {
//    {

    public boolean isFixedCol(ReportRequest rrequest)
    {
        if(rrequest==null) return isFixedCol;
        Object objVal=rrequest.getAttribute(this.getOwner().getReportBean().getId(),((ColBean)this.getOwner()).getColid()+"_IS_FIXED");
        if(objVal==null) return this.isFixedCol;
        return ((Boolean)objVal).booleanValue();
    }

    public void setFixedCol(ReportRequest rrequest,boolean isFixedCol)
    {
        if(rrequest==null)
        {
            this.isFixedCol=isFixedCol;
        }else
        {
            rrequest.setAttribute(this.getOwner().getReportBean().getId(),((ColBean)this.getOwner()).getColid()+"_IS_FIXED",isFixedCol);
        }
    }

    public boolean isRoworderValue()
    {
        return isRoworderValue;
    }

    public void setRoworderValue(boolean isRoworderValue)
    {
        this.isRoworderValue=isRoworderValue;
    }

    public String getRoworder_inputboxstyleproperty()
    {
        return roworder_inputboxstyleproperty;
    }

    public void setRoworder_inputboxstyleproperty(String roworder_inputboxstyleproperty)
    {
        this.roworder_inputboxstyleproperty=roworder_inputboxstyleproperty;
    }

    public AbsListReportFilterBean getFilterBean()
    {
        return filterBean;
    }

    public void setFilterBean(AbsListReportFilterBean filterBean)
    {
        this.filterBean=filterBean;
    }

    public boolean isDragable(AbsListReportDisplayBean alrdbean)
    {
        if(this.isFixedCol) return false;
        if(alrdbean==null||alrdbean.getRowgrouptype()<=0||alrdbean.getRowGroupColsNum()<=0) return true;//不是行分组或树形分组报表
        if(this.isRowgroup()) return false;
        return true;
    }
    
    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        AbsListReportColBean bean=(AbsListReportColBean)super.clone(owner);
        if(this.filterBean!=null)
        {
            bean.setFilterBean((AbsListReportFilterBean)this.filterBean.clone(owner));
        }
        return bean;
    }
}
