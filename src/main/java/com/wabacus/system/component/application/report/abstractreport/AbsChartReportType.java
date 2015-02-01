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
package com.wabacus.system.component.application.report.abstractreport;

import java.util.List;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsChartReportBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.util.Consts;

public abstract class AbsChartReportType extends AbsReportType
{
    public final static String KEY=AbsChartReportType.class.getName();

    protected AbsChartReportBean acrbean;
    
    public AbsChartReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            acrbean=(AbsChartReportBean)((ReportBean)comCfgBean).getExtendConfigDataForReportType(KEY);
        }
    }

    public AbsChartReportBean getAcrbean()
    {
        return acrbean;
    }

    public abstract String loadStringChartData(boolean invokeInterceptor);
    
    public boolean isHiddenCol(ColBean cbean)
    {
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE))) return true;
        return !cbean.checkDisplayPermission(rrequest);
    }

    public String getColSelectedMetadata()
    {
        return null;
    }

    protected String getDefaultNavigateKey()
    {
        return null;
    }

    protected int getTotalColCount()
    {
        return 0;
    }

    public int afterReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        super.afterReportLoading(reportbean,lstEleReportBeans);
        AbsChartReportBean acrbean=(AbsChartReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(acrbean==null)
        {
            acrbean=new AbsChartReportBean(reportbean);
            reportbean.setExtendConfigDataForReportType(KEY,acrbean);
        }
        XmlElementBean eleReportBean=lstEleReportBeans.get(0);
        String chartype=eleReportBean.attributeValue("chartype");
        if(chartype!=null) acrbean.setChartype(chartype.trim());
        if(acrbean.getChartype()==null||acrbean.getChartype().trim().equals(""))
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"必须指定图表类型");
        }
        String datatype=eleReportBean.attributeValue("datatype");
        if(datatype!=null) acrbean.setDatatype(datatype.trim());
        String chartstyleproperty=eleReportBean.attributeValue("chartstyleproperty");
        if(chartstyleproperty!=null)
        {
            acrbean.setChartstyleproperty(chartstyleproperty.trim(),false);
        }
        return 1;
    }
}
