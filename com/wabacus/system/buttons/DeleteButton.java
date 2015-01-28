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

import java.util.List;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.util.Consts_Private;

public class DeleteButton extends AbsEditableReportButton
{
    public DeleteButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    public String getButtonType()
    {
        return Consts_Private.DELETE_BUTTON;
    }

    protected String getClickEvent(ReportRequest rrequest,String paramsForGetUrl)
    {
        StringBuffer paramsBuf=new StringBuffer();
        paramsBuf.append("{pageid:'").append(ccbean.getPageBean().getId()).append("'");
        ReportBean rbean=(ReportBean)this.ccbean;
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
        if(ersqlbean==null) return "";
        paramsBuf.append(",savingReportIds:[");
        List<ReportBean> lstBindedReportBeans=(List<ReportBean>)rrequest.getAttribute(ccbean.getId()+"_DYN_BINDED_REPORTS");
        if(lstBindedReportBeans==null||lstBindedReportBeans.size()==0)
        {
            lstBindedReportBeans=ersqlbean.getLstDeleteBindingReportBeans();
        }
        if(lstBindedReportBeans!=null&&lstBindedReportBeans.size()>0)
        {
            for(ReportBean rbBindedTmp:lstBindedReportBeans)
            {
                paramsBuf.append("{reportid:'").append(rbBindedTmp.getId()).append("',updatetype:'delete'},");
            }
            if(paramsBuf.length()>0&&paramsBuf.charAt(paramsBuf.length()-1)==',') paramsBuf.deleteCharAt(paramsBuf.length()-1);
        }else
        {
            paramsBuf.append("{reportid:'").append(ccbean.getId()).append("',updatetype:'delete'}");
        }
        paramsBuf.append("]}");
        rrequest.removeAttribute(ccbean.getId()+"_DYN_BINDED_REPORTS");
        return "saveEditableReportData("+paramsBuf.toString()+")";
    }
}
