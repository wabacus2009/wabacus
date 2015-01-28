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
package com.wabacus.system.tags.component;

import java.io.IOException;

import javax.servlet.jsp.JspException;

import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public class TitleTag extends AbsComponentTag
{
    private static final long serialVersionUID=-3266691866541279150L;

    private String type;

    private String top;

    public void setType(String type)
    {
        this.type=type;
    }

    public void setTop(String top)
    {
        this.top=top;
    }

    public int doMyStartTag() throws JspException,IOException
    {
        if(this.displayComponentObj==null) return SKIP_BODY;
        if(!(this.displayComponentObj instanceof AbsReportType))
        {
            throw new WabacusRuntimeException("组件"+this.displayComponentObj.getConfigBean().getPath()+"不是报表，不能调用<wx:title/>显示其标题部分");
        }
        if(type!=null)
        {
            type=type.toLowerCase().trim();
            if(!type.equals("")&&!type.equals("title")&&!type.equals("subtitle"))
            {
                throw new JspException("<title/>标签中type属性指定的值只能是title或subtitle");
            }
        }
        return EVAL_BODY_INCLUDE;
    }

    public int doMyEndTag() throws JspException,IOException
    {
        if(this.displayComponentObj==null) return EVAL_PAGE;
        println(TagAssistant.getInstance().getTitleDisplayValue((AbsReportType)this.displayComponentObj,type,top));
        return EVAL_PAGE;
    }
}
