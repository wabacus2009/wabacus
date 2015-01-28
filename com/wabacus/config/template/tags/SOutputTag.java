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
package com.wabacus.config.template.tags;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class SOutputTag extends AbsTagInTemplate
{
    public SOutputTag(AbsTagInTemplate parentTag)
    {
        super(parentTag);
    }

    public String getDisplayValue(ReportRequest rrequest,AbsComponentType ownerComponentObj)
    {
        String value=this.mTagAttributes.get("value");
        if(value==null) return "";
        if(WabacusAssistant.getInstance().isGetRequestContextValue(value))
        {//如果是定义从request/session中取动态值显示
            if(Tools.isDefineKey("url",value))
            {
                String urlname=Tools.getRealKeyByDefine("url",value);
                if(ownerComponentObj instanceof AbsReportType)
                {
                    (((AbsReportType)ownerComponentObj).getReportBean()).addParamNameFromURL(urlname);
                }
                rrequest.addParamToUrl(urlname,rrequest.getStringAttribute(urlname,""),false);
            }
            return WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,value,"");
        }
        return value;
    }

    public String getTagname()
    {
        return Consts_Private.TAGNAME_OUTPUT;
    }
}

