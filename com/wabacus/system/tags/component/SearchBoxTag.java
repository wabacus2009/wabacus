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

import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.inputbox.CustomizedBox;

public class SearchBoxTag extends AbsComponentTag
{
    private static final long serialVersionUID=-8285863411043887498L;

    private String condition;

    private String styleproperty=null;

    private String top;

    private String iteratorindex;
    
    private AbsReportType displayReportTypeObj;
    
    public void setCondition(String condition)
    {
        this.condition=condition;
    }

    public void setStyleproperty(String styleproperty)
    {
        this.styleproperty=styleproperty;
    }

    public void setTop(String top)
    {
        this.top=top;
    }

    public void setIteratorindex(String iteratorindex)
    {
        this.iteratorindex=iteratorindex;
    }

    public int doMyStartTag() throws JspException,IOException
    {
        if(this.displayComponentObj==null) return SKIP_BODY;
        if(!(this.displayComponentObj instanceof AbsReportType))
        {
            throw new WabacusRuntimeException("组件"+this.displayComponentObj.getConfigBean().getPath()+"不是报表，不能调用<wx:searchbox/>显示其搜索部分");
        }
        displayReportTypeObj=(AbsReportType)this.displayComponentObj;
        if(condition!=null&&!condition.trim().equals(""))
        {
            ConditionBean cbean=getConditionBean();
            if(showInputboxSelf(cbean))
            {
                printlnConditionInputBox(displayReportTypeObj,cbean);
                return EVAL_BODY_INCLUDE;
            }
        }
        return SKIP_BODY;
    }

    public int doMyEndTag() throws JspException,IOException
    {
        if(displayReportTypeObj==null) return EVAL_PAGE;
        if(condition!=null&&!condition.trim().equals(""))
        {//显示某个查询条件的输入框
            ConditionBean cbean=getConditionBean();
            if(!showInputboxSelf(cbean))
            {
                printlnConditionInputBox(displayReportTypeObj,cbean);
            }else
            {
                out.println("</font>");
            }
        }else
        {
            String resultStr=displayReportTypeObj.showSearchBox();
            if(resultStr==null) resultStr="";
            if(!resultStr.trim().equals(""))
            {
                StringBuffer resultBuf=new StringBuffer();
                resultBuf.append(TagAssistant.getInstance().showTopSpace(top));
                resultBuf.append(resultStr);
                resultStr=resultBuf.toString();
            }
            println(resultStr);
        }
        return EVAL_PAGE;
    }

    private ConditionBean getConditionBean()
    {
        ConditionBean cbean=displayReportTypeObj.getReportBean().getSbean().getConditionBeanByName(condition);
        if(cbean==null)
        {
            throw new WabacusRuntimeException("报表"+displayReportTypeObj.getReportBean().getPath()+"没有name属性为"+condition+"的查询条件，无法显示其输入框");
        }
        if(!cbean.isConditionWithInputbox())
        {
            throw new WabacusRuntimeException("报表"+displayReportTypeObj.getReportBean().getPath()+"的name属性为"+condition+"的查询条件是隐藏查询条件，不需显示输入框");
        }
        return cbean;
    }
    
    private boolean showInputboxSelf(ConditionBean cbean)
    {
        return cbean.getInputbox() instanceof CustomizedBox;
    }
    
    private void printlnConditionInputBox(AbsReportType reportObj,ConditionBean cbean) throws IOException
    {
        println(TagAssistant.getInstance().showConditionBox(rrequest,cbean,iteratorindex,styleproperty));
    }
}
