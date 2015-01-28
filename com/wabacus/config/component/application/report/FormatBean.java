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
package com.wabacus.config.component.application.report;

import java.util.ArrayList;
import java.util.List;

public class FormatBean extends AbsConfigBean
{
    private String formatContent;

    private List<String> lstImports;

    public FormatBean(AbsConfigBean parent)
    {
        super(parent);
    }

    public String getFormatContent()
    {
        return formatContent;
    }

    public void setFormatContent(String formatContent)
    {
        this.formatContent=formatContent;
    }

    public List<String> getLstImports()
    {
        return lstImports;
    }

    public void setLstImports(List<String> lstImports)
    {
        this.lstImports=lstImports;
    }

    public AbsConfigBean clone(AbsConfigBean parent)
    {
        FormatBean fbTemp=(FormatBean)super.clone(parent);
        if(parent instanceof ReportBean) ((ReportBean)parent).setFbean(fbTemp);
        if(lstImports!=null)
        {
            fbTemp.setLstImports((List<String>)((ArrayList<String>)lstImports).clone());
        }
        cloneExtendConfig(fbTemp);
        return fbTemp;
    }
}
