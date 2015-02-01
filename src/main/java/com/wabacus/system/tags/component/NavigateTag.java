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
import javax.servlet.jsp.tagext.BodyContent;

import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public class NavigateTag extends AbsComponentTag
{
    private static final long serialVersionUID=-7075710208765627815L;

    private String type;

    private String top;

    private String minlength;

    private String initcount;

    private String maxcount;//当type为sequence时，指定显示连续页码的最大个数，默认值与initcount相等，指定的值必须大于等于initcount值

    public void setType(String type)
    {
        this.type=type;
    }

    public void setMinlength(String minlength)
    {
        this.minlength=minlength;
    }

    public String getTop()
    {
        return top;
    }

    public void setTop(String top)
    {
        this.top=top;
    }

    public void setInitcount(String initcount)
    {
        this.initcount=initcount;
    }

    public void setMaxcount(String maxcount)
    {
        this.maxcount=maxcount;
    }

    public int doMyStartTag() throws JspException,IOException
    {
        if(this.displayComponentObj==null) return SKIP_BODY;
        if(!(this.displayComponentObj instanceof AbsReportType))
        {
            throw new WabacusRuntimeException("组件"+this.displayComponentObj.getConfigBean().getPath()+"不是报表，不能调用<wx:navigate/>显示其翻页导航栏部分");
        }
        type=type==null?"":type.toLowerCase().trim();
        return EVAL_BODY_BUFFERED;
    }

    public int doMyEndTag() throws JspException,IOException
    {
        if(this.displayComponentObj==null) return EVAL_PAGE;
        String label=null;
        BodyContent bc=this.getBodyContent();
        if(bc!=null)
        {
            label=bc.getString();
            label=label==null?"":label.trim();
        }
        label=label==null?"":label.trim();
        println(TagAssistant.getInstance()
                .getNavigateDisplayInfo((AbsReportType)this.displayComponentObj,type,minlength,initcount,maxcount,top,label));
        return EVAL_PAGE;
    }
}
