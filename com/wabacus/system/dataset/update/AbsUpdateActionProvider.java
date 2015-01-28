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
package com.wabacus.system.dataset.update;

import java.util.List;

import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.dataset.update.precondition.AbsExpressionBean;
import com.wabacus.util.Tools;

public abstract class AbsUpdateActionProvider implements Cloneable
{
    private String precondition;

    private String datasource;
    
    protected AbsEditableReportEditDataBean ownerUpdateBean;//本<value/>所属的<insert/>、<update/>、<delete/>、<button/>对象

    public void setOwnerUpdateBean(AbsEditableReportEditDataBean ownerUpdateBean)
    {
        this.ownerUpdateBean=ownerUpdateBean;
    }

    public void setPrecondition(String precondition)
    {
        this.precondition=precondition;
    }

    public String getPrecondition()
    {
        return precondition;
    }

    public String getDatasource()
    {
        if(Tools.isEmpty(datasource)) datasource=ownerUpdateBean.getDatasource();
        return datasource;
    }
    
    public boolean loadConfig(XmlElementBean eleValueBean)
    {
        datasource=eleValueBean.attributeValue("datasource");
        return true;
    }

    public abstract List<AbsUpdateAction> parseAllUpdateActions(String reportTypeKey);

    protected Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    public Object clone(AbsEditableReportEditDataBean newowner)
    {
        try
        {
            AbsUpdateActionProvider newbean=(AbsUpdateActionProvider)clone();
            newbean.ownerUpdateBean=newowner;
            return newbean;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
