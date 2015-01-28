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

import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts_Private;

public class SSearchBoxTag extends AbsTagInTemplate
{
    public SSearchBoxTag(AbsTagInTemplate parentTag)
    {
        super(parentTag);
    }

    public String getTagname()
    {
        return Consts_Private.TAGNAME_SEARCHBOX;
    }

    public String getDisplayValue(ReportRequest rrequest,AbsComponentType ownerComponentObj)
    {
        AbsComponentType displayComponentObj=this.getDisplayComponentObj(rrequest);
        if(displayComponentObj==null) displayComponentObj=ownerComponentObj;
        if(!(displayComponentObj instanceof AbsReportType))
        {
            throw new WabacusRuntimeException("组件"+displayComponentObj.getConfigBean().getPath()+"不是报表，不能调用<wx:navigate/>显示其翻页导航栏部分");
        }
        AbsReportType reportTypeObj=(AbsReportType)displayComponentObj;
        ReportBean rbean=reportTypeObj.getReportBean();
        String condition=null;
        String top=null;
        String iteratorindex=null;
        if(this.mTagAttributes!=null)
        {
            condition=this.mTagAttributes.get("condition");
            condition=condition==null?"":condition.trim();
            top=this.mTagAttributes.get("top");
            iteratorindex=this.mTagAttributes.get("iteratorindex");
        }
        if(condition==null||condition.trim().equals(""))
        {
            String resultStr=reportTypeObj.showSearchBox();
            if(resultStr==null||resultStr.trim().equals("")) return "";
            StringBuffer resultBuf=new StringBuffer();
            resultBuf.append(TagAssistant.getInstance().showTopSpace(top));
            resultBuf.append(resultStr);
            return resultBuf.toString();
        }else
        {
            ConditionBean conbean=rbean.getSbean().getConditionBeanByName(condition);
            if(conbean==null)
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"没有name属性为"+condition+"的查询条件，无法显示其输入框");
            }
            if(!conbean.isConditionWithInputbox())
            {
                throw new WabacusRuntimeException("报表"+rbean.getPath()+"的name属性为"+condition+"的查询条件是隐藏查询条件，不需显示输入框");
            }
            return TagAssistant.getInstance().showConditionBox(rrequest,conbean,iteratorindex,null);
        }
    }
}
