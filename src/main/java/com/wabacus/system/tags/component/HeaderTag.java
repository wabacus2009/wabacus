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

import com.wabacus.system.assistant.TagAssistant;

public class HeaderTag extends AbsComponentTag
{
    private static final long serialVersionUID=4032346797072244038L;

    private String top;

    public void setTop(String top)
    {
        this.top=top;
    }

    public int doMyStartTag() throws JspException,IOException
    {
        if(this.displayComponentObj==null) return SKIP_BODY;
        return EVAL_BODY_INCLUDE;
    }

    public int doMyEndTag() throws JspException,IOException
    {
        if(this.displayComponentObj==null) return EVAL_PAGE;
        println(TagAssistant.getInstance().getHeaderFooterDisplayValue(this.displayComponentObj,top,true));
        return EVAL_PAGE;
    }

}
