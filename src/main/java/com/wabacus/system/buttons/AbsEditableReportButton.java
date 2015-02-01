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
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;

public abstract class AbsEditableReportButton extends WabacusButton
{

    public AbsEditableReportButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    protected boolean checkDisplayPermission(ReportRequest rrequest)
    {
        AbsReportType reportObj=(AbsReportType)rrequest.getComponentTypeObj(ccbean,null,true);
        if(reportObj!=null)
        {
            if(!(reportObj instanceof IEditableReportType)) return false;
            if(EditableReportAssistant.getInstance().isReadonlyAccessMode(((IEditableReportType)reportObj))) return false;
            if(!Consts_Private.DELETE_BUTTON.equals(this.getButtonType()))
            {
                if(rrequest.checkPermission(reportObj.getReportBean().getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_READONLY)) return false;
            }
        }
        return super.checkDisplayPermission(rrequest);
    }

    public String showButton(ReportRequest rrequest,String params)
    {
        if(!shouldShowThisEditButton(rrequest)) return "";
        return super.showButton(rrequest,getClickEvent(rrequest,params));
    }

    public String showButton(ReportRequest rrequest,String params,String button)
    {
        if(!shouldShowThisEditButton(rrequest)) return "";
        return super.showButton(rrequest,getClickEvent(rrequest,params),button);
    }

    public String showMenu(ReportRequest rrequest,String params)
    {
        if(!shouldShowThisEditButton(rrequest)) return "";
        return super.showMenu(rrequest,getClickEvent(rrequest,params));
    }
    
    private boolean shouldShowThisEditButton(ReportRequest rrequest)
    {
        AbsReportType reportObj=(AbsReportType)rrequest.getComponentTypeObj(ccbean,null,true);
        if(!(reportObj instanceof IEditableReportType)) return false;
        if(!((IEditableReportType)reportObj).needCertainTypeButton(this)) return false;//在此可编辑报表中不需要显示此按钮
        return true;
    }
    
    protected abstract String getClickEvent(ReportRequest rrequest,String params);
}

