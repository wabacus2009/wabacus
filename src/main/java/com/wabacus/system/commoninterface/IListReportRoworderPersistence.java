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

import java.util.Map;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;

public interface IListReportRoworderPersistence
{
    public String loadRoworder(Map<String,String> mColValuesInRow);

    public void storeRoworderByDrag(ReportRequest rrequest,ReportBean rbean,Map<String,String> mColValuesInRow,
            Map<String,String> mColValuesInDestRow,boolean direct);

    public void storeRoworderByArrow(ReportRequest rrequest,ReportBean rbean,Map<String,String> mColValuesInRow,
            Map<String,String> mColValuesInDestRow,boolean direct);

    public void storeRoworderByInputbox(ReportRequest rrequest,ReportBean rbean,Map<String,String> mColValuesInRow,String newrowordervalue);

    public void storeRoworderByTop(ReportRequest rrequest,ReportBean rbean,Map<String,String> mColValuesInRow);
}
