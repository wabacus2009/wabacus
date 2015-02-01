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
package com.wabacus.system.component.application.report.chart.configbean;

import java.util.Map;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;

public class FunsionChartsReportBean extends AbsExtendConfigBean
{
    private String chartwidth;
    
    private String chartheight;
    
    private boolean debugmode=false;
    
    private boolean registerwithjs=true;
    
    private String subdisplayvalue;//配置在<subdisplay/>中的内容，解析后这里存放带占位符的subdisplayvalue字符串
    
    private Map<String,String> mDynSubdisplayvalueParts;//subdisplayvalue中的动态部分，key为此动态值的在subdisplayvalue中的占位符，值为request{xxx}、session{key}、url{key}等等形式，用于运行时得到真正值
    
    private boolean isSingleSeriesChart;
    
    private boolean isDualLayerDatasetTag;//是否需要显示两层<dataset/>，在afterReportLoad中构造
    
    private boolean isXyPlotChart;
    
    private boolean isLinkChart;
    
    public FunsionChartsReportBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getChartwidth()
    {
        return chartwidth;
    }

    public void setChartwidth(String chartwidth)
    {
        this.chartwidth=chartwidth;
    }

    public String getChartheight()
    {
        return chartheight;
    }

    public void setChartheight(String chartheight)
    {
        this.chartheight=chartheight;
    }

    public boolean isDebugmode()
    {
        return debugmode;
    }

    public void setDebugmode(boolean debugmode)
    {
        this.debugmode=debugmode;
    }

    public boolean isRegisterwithjs()
    {
        return registerwithjs;
    }

    public void setRegisterwithjs(boolean registerwithjs)
    {
        this.registerwithjs=registerwithjs;
    }

    public boolean isDualLayerDatasetTag()
    {
        return isDualLayerDatasetTag;
    }

    public void setDualLayerDatasetTag(boolean isDualDatasetTag)
    {
        this.isDualLayerDatasetTag=isDualDatasetTag;
    }

    public String getSubdisplayvalue(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.subdisplayvalue,this.mDynSubdisplayvalueParts,"");
    }

    public void setSubdisplayvalue(String subdisplayvalue)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.getOwner().getPageBean(),subdisplayvalue);
        this.subdisplayvalue=(String)objArr[0];
        this.mDynSubdisplayvalueParts=(Map<String,String>)objArr[1];
    }

    public boolean isSingleSeriesChart()
    {
        return isSingleSeriesChart;
    }

    public void setSingleSeriesChart(boolean isSingleSeriesChart)
    {
        this.isSingleSeriesChart=isSingleSeriesChart;
    }

    public boolean isXyPlotChart()
    {
        return isXyPlotChart;
    }

    public void setXyPlotChart(boolean isXyPlotChart)
    {
        this.isXyPlotChart=isXyPlotChart;
    }

    public boolean isLinkChart()
    {
        return isLinkChart;
    }

    public void setLinkChart(boolean isLinkChart)
    {
        this.isLinkChart=isLinkChart;
    }
}
