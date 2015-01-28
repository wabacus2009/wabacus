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
package com.wabacus.system.tags;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.inputbox.PopUpBox;

public class PopUpInputBoxTag extends TagSupport
{
    private static final long serialVersionUID=-7292339076480586578L;

    public int doStartTag() throws JspException
    {
        HttpServletRequest request=(HttpServletRequest)pageContext.getRequest();
        JspWriter out=pageContext.getOut();
        String pageid=request.getParameter("SRC_PAGEID");
        String reportid=request.getParameter("SRC_REPORTID");
        String inputboxid=request.getParameter("INPUTBOXID");
        pageid=pageid==null?"":pageid.trim();
        inputboxid=inputboxid==null?"":inputboxid.trim();
        reportid=reportid==null?"":reportid.trim();
        PageBean pbean=Config.getInstance().getPageBean(pageid);
        if(pbean==null)
        {
            throw new WabacusRuntimeException("页面ID："+pageid+"对应的页面不存在");
        }
        ReportBean rbean=pbean.getReportChild(reportid,true);
        if(rbean==null)
        {
            throw new WabacusRuntimeException("ID为"+pageid+"的页面下不存在ID为"+rbean.getId()+"的报表");
        }
        
        String boxid=inputboxid;
//        String boxidSuffix=null;//输入框后缀，只对可编辑数据自动列表报表有效，即以__rowidx为后缀
        int idx=boxid.lastIndexOf("__");
        if(idx>0)
        {
            boxid=boxid.substring(0,idx);
        }
        PopUpBox popupboxObj=rbean.getPopUpBox(boxid);
        if(popupboxObj==null)
        {
            throw new JspException("报表"+rbean.getPath()+"下面不存在ID为"+boxid+"的弹出输入框");
        }
        try
        {
            out.println(popupboxObj.createSelectOkFunction(inputboxid,true));
        }catch(IOException e)
        {
            throw new WabacusRuntimeException("初始化报表"+rbean.getPath()+"的弹出输入框失败",e);
        }
        return EVAL_BODY_INCLUDE;
    }
}
