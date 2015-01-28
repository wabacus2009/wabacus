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
package com.wabacus.system.component.application.report.configbean;

import java.util.Map;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;

public class DetailReportDisplayBean extends AbsExtendConfigBean
{
    private String labeltdwidth=null;

    private String valuetdwidth=null;

    private String printlabelwidth;
    
    private String printvaluewidth;
    
    private String labelbgcolor=null;

    private String valuebgcolor=null;

    private String labelalign=null;//标题列的对齐方式

    private String valuealign=null;

    private Map<String,DetailReportColPositionBean> mColDefaultPagePositions;
    
    private Map<String,DetailReportColPositionBean> mColDefaultDataExportPositions;
    
    public DetailReportDisplayBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getLabeltdwidth()
    {
        return labeltdwidth;
    }

    public void setLabeltdwidth(String labeltdwidth)
    {
        this.labeltdwidth=labeltdwidth;
    }

    public String getValuetdwidth()
    {
        return valuetdwidth;
    }

    public void setValuetdwidth(String valuetdwidth)
    {
        this.valuetdwidth=valuetdwidth;
    }

    public String getValuebgcolor()
    {
        return valuebgcolor;
    }

    public void setValuebgcolor(String valuebgcolor)
    {
        this.valuebgcolor=valuebgcolor;
    }

    public String getLabelalign()
    {
        return labelalign;
    }

    public void setLabelalign(String labelalign)
    {
        this.labelalign=labelalign;
    }

    public String getValuealign()
    {
        return valuealign;
    }

    public void setValuealign(String valuealign)
    {
        this.valuealign=valuealign;
    }

    public String getLabelbgcolor()
    {
        return labelbgcolor;
    }

    public void setLabelbgcolor(String labelbgcolor)
    {
        this.labelbgcolor=labelbgcolor;
    }

    public String getPrintlabelwidth()
    {
        return printlabelwidth;
    }

    public void setPrintlabelwidth(String printlabelwidth)
    {
        this.printlabelwidth=printlabelwidth;
    }

    public String getPrintvaluewidth()
    {
        return printvaluewidth;
    }

    public void setPrintvaluewidth(String printvaluewidth)
    {
        this.printvaluewidth=printvaluewidth;
    }

    public Map<String,DetailReportColPositionBean> getMColDefaultPagePositions()
    {
        return mColDefaultPagePositions;
    }

    public void setMColDefaultPagePositions(Map<String,DetailReportColPositionBean> colDefaultPagePositions)
    {
        mColDefaultPagePositions=colDefaultPagePositions;
    }

    public Map<String,DetailReportColPositionBean> getMColDefaultDataExportPositions()
    {
        return mColDefaultDataExportPositions;
    }

    public void setMColDefaultDataExportPositions(Map<String,DetailReportColPositionBean> colDefaultDataExportPositions)
    {
        mColDefaultDataExportPositions=colDefaultDataExportPositions;
    }
}
