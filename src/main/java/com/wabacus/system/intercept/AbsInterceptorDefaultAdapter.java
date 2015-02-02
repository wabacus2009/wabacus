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

import java.util.Map;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;

public abstract class AbsInterceptorDefaultAdapter implements IInterceptor
{
    public void doStart(ReportRequest rrequest,ReportBean rbean)
    {}

    public int doSave(ReportRequest rrequest,ReportBean rbean,AbsEditableReportEditDataBean editbean)
    {
        return EditableReportAssistant.getInstance().doSaveReport(rrequest,rbean,editbean);
    }

    public int doSavePerRow(ReportRequest rrequest,ReportBean rbean,Map<String,String> mRowData,Map<String,String> mParamValues,
            AbsEditableReportEditDataBean editbean)
    {
        return EditableReportAssistant.getInstance().doSaveRow(rrequest,rbean,mRowData,mParamValues,editbean);
    }

    public int doSavePerAction(ReportRequest rrequest,ReportBean rbean,Map<String,String> mRowData,Map<String,String> mParamValues,
            AbsUpdateAction action,AbsEditableReportEditDataBean editbean)
    {
        return EditableReportAssistant.getInstance().doSavePerAction(rrequest,rbean,mRowData,mParamValues,action,editbean);
    }

    public Object beforeLoadData(ReportRequest rrequest,ReportBean rbean,Object typeObj,String sql)
    {
        return sql;
    }

    public Object afterLoadData(ReportRequest rrequest,ReportBean rbean,Object typeObj,Object dataObj)
    {
        return dataObj;
    }

    public void beforeDisplayReportData(ReportRequest rrequest,ReportBean rbean,ReportDataBean reportDataBean)
    {}

    public void beforeDisplayReportDataPerRow(ReportRequest rrequest,ReportBean rbean,RowDataBean rowDataBean)
    {}

    public void beforeDisplayReportDataPerCol(ReportRequest rrequest,ReportBean rbean,ColDataBean colDataBean)
    {}

    public void doEnd(ReportRequest rrequest,ReportBean rbean)
    {}
}
