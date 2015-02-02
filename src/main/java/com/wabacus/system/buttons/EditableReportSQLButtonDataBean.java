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
package com.wabacus.system.buttons;

import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportDeleteDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.IEditableReportEditGroupOwnerBean;

public class EditableReportSQLButtonDataBean extends EditableReportDeleteDataBean
{
    private boolean isAutoReportdata;

    private boolean  isHasReportDataParams;
    
    public boolean isAutoReportdata()
    {
        return isAutoReportdata;
    }

    public void setAutoReportdata(boolean isAutoReportdata)
    {
        this.isAutoReportdata=isAutoReportdata;
    }

    public boolean isHasReportDataParams()
    {
        return isHasReportDataParams;
    }

    public void setHasReportDataParams(boolean isHasReportDataParams)
    {
        this.isHasReportDataParams=isHasReportDataParams;
    }

    public EditableReportSQLButtonDataBean(IEditableReportEditGroupOwnerBean owner)
    {
        super(owner);
    }
}
