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
package com.wabacus.system.component.application.report.configbean.crosslist;

import java.util.Map;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;

public class CrossListReportStatiDisplayBean
{
    private String label;

    private String labelstyleproperty;

    private Map<String,String> mDynLabelstylepropertyParts;

    private String valuestyleproperty;

    private Map<String,String> mDynValuestylepropertyParts;//标题valuestyleproperty中的动态部分，key为此动态值的在valuestyleproperty中的占位符，值为request{xxx}、session{key}、url{key}等等形式，用于运行时得到真正值

    private CrossListReportStatiBean statiBean;

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label=label==null?"":label.trim();
    }

    public String getLabelstyleproperty(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.labelstyleproperty,this.mDynLabelstylepropertyParts,"");
    }

    public void setLabelstyleproperty(String labelstyleproperty)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(labelstyleproperty);
        this.labelstyleproperty=(String)objArr[0];
        this.mDynLabelstylepropertyParts=(Map<String,String>)objArr[1];
    }

    public String getValuestyleproperty(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.valuestyleproperty,this.mDynValuestylepropertyParts,"");
    }

    public void setValuestyleproperty(String valuestyleproperty)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(valuestyleproperty);
        this.valuestyleproperty=(String)objArr[0];
        this.mDynValuestylepropertyParts=(Map<String,String>)objArr[1];
    }

    public CrossListReportStatiBean getStatiBean()
    {
        return statiBean;
    }

    public void setStatiBean(CrossListReportStatiBean statiBean)
    {
        this.statiBean=statiBean;
    }
}
