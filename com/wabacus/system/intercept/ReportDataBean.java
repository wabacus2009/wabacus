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

import java.util.List;

import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public class ReportDataBean
{
    private AbsReportType reportTypeObj;
    
    private List lstColBeans;//本报表所有<col/>列对象，包括静态配置的、动态生成的、显示的、隐藏的等
    
    private String beforeDisplayString;

    private boolean shouldDisplayReportData;

    private String afterDisplayString;

    private String chartDataString;//对于图形报表有效，由用户自己构造图表的数据字符串，即<chart ....></chart>，如果是null，则由框架自动构造
    
    private boolean stopAutoDisplayChart;//对于图形报表有效，是否中止框架自动用JS显示图形报表
    
    public ReportDataBean(AbsReportType reportTypeObj,List lstColBeans)
    {
        this.reportTypeObj=reportTypeObj;
        this.lstColBeans=lstColBeans;
        this.shouldDisplayReportData=true;
    }

    public AbsReportType getReportTypeObj()
    {
        return reportTypeObj;
    }

    public List getLstColBeans()
    {
        return lstColBeans;
    }

    public boolean isShouldDisplayReportData()
    {
        return shouldDisplayReportData;
    }

    public void setShouldDisplayReportData(boolean shouldDisplayReportData)
    {
        this.shouldDisplayReportData=shouldDisplayReportData;
    }

    public String getBeforeDisplayString()
    {
        return beforeDisplayString;
    }

    public void setBeforeDisplayString(String beforeDisplayString)
    {
        this.beforeDisplayString=beforeDisplayString;
    }

    public String getAfterDisplayString()
    {
        return afterDisplayString;
    }

    public void setAfterDisplayString(String afterDisplayString)
    {
        this.afterDisplayString=afterDisplayString;
    }

    public String getChartDataString()
    {
        return chartDataString;
    }

    public void setChartDataString(String chartDataString)
    {
        this.chartDataString=chartDataString;
    }

    public boolean isStopAutoDisplayChart()
    {
        return stopAutoDisplayChart;
    }

    public void setStopAutoDisplayChart(boolean stopAutoDisplayChart)
    {
        this.stopAutoDisplayChart=stopAutoDisplayChart;
    }
}
