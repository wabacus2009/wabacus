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
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.util.Consts;

public interface IInterceptor
{
    public final int WX_RETURNVAL_SUCCESS=2;

    public final int WX_RETURNVAL_SUCCESS_NOTREFRESH=1;

    public final int WX_RETURNVAL_SKIP=0;

    public final int WX_RETURNVAL_TERMINATE=-1;

    public final int WX_INSERT=Consts.UPDATETYPE_INSERT;//添加记录的操作

    public final int WX_UPDATE=Consts.UPDATETYPE_UPDATE;

    public final int WX_DELETE=Consts.UPDATETYPE_DELETE;

    public void doStart(ReportRequest rrequest,ReportBean rbean);

    public int doSave(ReportRequest rrequest,ReportBean rbean,AbsEditableReportEditDataBean editbean);

    public int doSavePerRow(ReportRequest rrequest,ReportBean rbean,Map<String,String> mRowData,Map<String,String> mParamValues,
            AbsEditableReportEditDataBean editbean);

    public int doSavePerAction(ReportRequest rrequest,ReportBean rbean,Map<String,String> mRowData,Map<String,String> mParamValues,
            AbsUpdateAction action,AbsEditableReportEditDataBean editbean);

    public Object beforeLoadData(ReportRequest rrequest,ReportBean rbean,Object typeObj,String sql);

    public Object afterLoadData(ReportRequest rrequest,ReportBean rbean,Object typeObj,Object dataObj);

    public void beforeDisplayReportData(ReportRequest rrequest,ReportBean rbean,ReportDataBean reportDataBean);

    public void beforeDisplayReportDataPerRow(ReportRequest rrequest,ReportBean rbean,RowDataBean rowDataBean);

    public void beforeDisplayReportDataPerCol(ReportRequest rrequest,ReportBean rbean,ColDataBean colDataBean);

    public void doEnd(ReportRequest rrequest,ReportBean rbean);
}
