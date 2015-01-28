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
package com.wabacus.system.dataset.update.precondition;

import java.util.List;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;

public abstract class AbsConcreteExpressionBean extends AbsExpressionBean
{
    protected AbsEditableReportEditDataBean ownerEditbean;
    
    protected String datasource;//如果是采用SQL语句取条件表达式的值，这里存放它所在<value/>的datasource
    
    protected String reportTypeKey;
    
    protected List<String> lstParams;

    public void setOwnerEditbean(AbsEditableReportEditDataBean ownerEditbean)
    {
        this.ownerEditbean=ownerEditbean;
    }

    public void setDatasource(String datasource)
    {
        this.datasource=datasource;
    }

    public void setReportTypeKey(String reportTypeKey)
    {
        this.reportTypeKey=reportTypeKey;
    }

    public void setLstParams(List<String> lstParams)
    {
        this.lstParams=lstParams;
    }

    protected ReportBean getReportBean()
    {
        return this.ownerEditbean.getOwner().getReportBean();
    }
    
    public void parseParams()
    {}

}
