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
package com.wabacus.system.commoninterface;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;

public class DefaultReportPersonalizePersistenceImpl implements IReportPersonalizePersistence
{
    public String loadColOrderData(ReportRequest rrequest,ReportBean rbean)
    {
        return (String)rrequest.getRequest().getSession().getAttribute(rbean.getPath()+"_colorderdata");
    }

    public void storeColOrderData(ReportRequest rrequest,ReportBean rbean,String colOrder)
    {
        rrequest.getRequest().getSession().setAttribute(rbean.getPath()+"_colorderdata",colOrder);
    }

    public String loadColSelectedData(ReportRequest rrequest,ReportBean rbean)
    {
        return (String)rrequest.getRequest().getSession().getAttribute(rbean.getPath()+"_colselecteddata");
    }

    public void storeColSelectedData(ReportRequest rrequest,ReportBean rbean,String colSelectedData)
    {
        rrequest.getRequest().getSession().setAttribute(rbean.getPath()+"_colselecteddata",colSelectedData);
    }

    public String loadOrderByCol(ReportRequest rrequest,ReportBean rbean)
    {
        return (String)rrequest.getRequest().getSession().getAttribute(rbean.getPath()+"_orderbycol");
    }

    public void storeOrderByCol(ReportRequest rrequest,ReportBean rbean,String orderByCol)
    {
        rrequest.getRequest().getSession().setAttribute(rbean.getPath()+"_orderbycol",orderByCol);
    }    
}
