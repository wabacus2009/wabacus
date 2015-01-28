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
package com.wabacus.system.component.application.report.configbean.editablereport;

import java.util.Map;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;


public class EditableReportDeleteDataBean extends AbsEditableReportEditDataBean
{
    private String deleteConfirmMessage=null;

    private Map<String,String> mDynDeleteConfirmMessageParts;
    
    public EditableReportDeleteDataBean(IEditableReportEditGroupOwnerBean owner)
    {
        super(owner);
    }

    public String getDeleteConfirmMessage(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,deleteConfirmMessage,mDynDeleteConfirmMessageParts,"");
    }

    public void setDeleteConfirmMessage(String deleteConfirmMessage)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.getOwner().getReportBean().getPageBean(),deleteConfirmMessage);
        this.deleteConfirmMessage=(String)objArr[0];
        this.mDynDeleteConfirmMessageParts=(Map<String,String>)objArr[1];
    }
}
