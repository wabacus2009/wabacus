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

import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts_Private;

public class SDataTag extends AbsTagInTemplate
{
    public SDataTag(AbsTagInTemplate parentTag)
    {
        super(parentTag);
    }
    
    public String getTagname()
    {
        return Consts_Private.TAGNAME_DATA;
    }
    
    public String getDisplayValue(ReportRequest rrequest,AbsComponentType ownerComponentObj)
    {
        AbsComponentType displayComponentObj=this.getDisplayComponentObj(rrequest);
        if(displayComponentObj==null) displayComponentObj=ownerComponentObj;
        if(!(displayComponentObj instanceof AbsReportType))
        {
            throw new WabacusRuntimeException("组件"+displayComponentObj.getConfigBean().getPath()+"不是报表，不能调用<wx:data/>显示其数据部分");
        }
        return TagAssistant.getInstance().getReportDataPartDisplayValue((AbsReportType)displayComponentObj,this.mTagAttributes);
    }
}

