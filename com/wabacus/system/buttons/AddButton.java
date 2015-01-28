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

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.report.EditableDetailReportType;
import com.wabacus.system.component.application.report.EditableListReportType;
import com.wabacus.system.component.application.report.EditableListReportType2;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;

public class AddButton extends AbsEditableReportButton
{
    public AddButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    public String getButtonType()
    {
        return Consts_Private.ADD_BUTTON;
    }

    protected String getClickEvent(ReportRequest rrequest,String paramsForGetUrl)
    {
        IComponentType reportObj=rrequest.getComponentTypeObj(ccbean,null,true);
        if(reportObj instanceof EditableListReportType)
        {
            return ((EditableListReportType)reportObj).getAddEvent();
        }else if(reportObj instanceof EditableListReportType2)
        {
            return "addNewDataRow('"+ccbean.getPageBean().getId()+"','"+ccbean.getGuid()+"')";
        }else if(reportObj instanceof EditableDetailReportType)
        {
            return "changeReportAccessMode('"+ccbean.getGuid()+"','"+Consts.ADD_MODE+"')";
        }
        return "";
    }
}
