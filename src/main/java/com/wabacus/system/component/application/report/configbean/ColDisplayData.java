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

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.intercept.ColDataBean;

public class ColDisplayData
{
    private String value;

    private String styleproperty;

    private ColDataBean coldataByInterceptor;

    public void setValue(String value)
    {
        this.value=value;
    }
    
    public String getValue()
    {
        return value;
    }

    public String getStyleproperty()
    {
        return styleproperty;
    }

    public ColDataBean getColdataByInterceptor()
    {
        return coldataByInterceptor;
    }

    public static ColDisplayData getColDataFromInterceptor(AbsReportType reportTypeObj,Object colGroupBean,AbsReportDataPojo rowDataObj,int rowindex,
            String styleproperty,String value)
    {
        ColDisplayData cdd=new ColDisplayData();
        if(reportTypeObj.getReportBean().getInterceptor()!=null)
        {
            if(rowindex<0&&rowindex!=-1) rowindex=-1;
            cdd.coldataByInterceptor=new ColDataBean(reportTypeObj,colGroupBean,rowDataObj,value,styleproperty,rowindex);
            reportTypeObj.getReportBean().getInterceptor().beforeDisplayReportDataPerCol(reportTypeObj.getReportRequest(),
                    reportTypeObj.getReportBean(),cdd.coldataByInterceptor);
            value=cdd.coldataByInterceptor.getValue();
            styleproperty=cdd.coldataByInterceptor.getStyleproperty();
        }
        cdd.value=value==null?"":value;
        cdd.styleproperty=styleproperty==null?"":styleproperty;
        return cdd;
    }
}
