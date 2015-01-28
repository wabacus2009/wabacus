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
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.system.assistant.TagAssistant;

public class DataImportTag extends BodyTagSupport
{
    private static final long serialVersionUID=2884967750630773439L;

    private final static Log log=LogFactory.getLog(DataImportTag.class);

    private String ref;

    private String asyn;
    
    private String dataimportinitsize;//初始大小，可配置值包括max/normal，分别表示最大化、最小化、正常窗口大小（即上面pagewidth/pageheight配置的大小）
    
    private String popupparams;
    
    private String interceptor;
    
    public String getRef()
    {
        return ref;
    }

    public void setRef(String ref)
    {
        this.ref=ref;
    }

    public void setAsyn(String asyn)
    {
        this.asyn=asyn;
    }

    public void setDataimportinitsize(String dataimportinitsize)
    {
        this.dataimportinitsize=dataimportinitsize;
    }

    public String getPopupparams()
    {
        return popupparams;
    }

    public void setPopupparams(String popupparams)
    {
        this.popupparams=popupparams;
    }

    public void setInterceptor(String interceptor)
    {
        this.interceptor=interceptor;
    }

    public int doStartTag() throws JspException
    {
        return EVAL_BODY_BUFFERED;
    }

    public int doEndTag() throws JspException
    {
        if(ref==null||ref.trim().equals(""))
        {
            throw new JspException("必须指定<wx:dataimport/>的ref属性");
        }
        BodyContent bc=getBodyContent();
        String label=null;
        if(bc!=null) label=bc.getString();
        JspWriter out=pageContext.getOut();
        try
        {
            out.println(TagAssistant.getInstance().getDataImportDisplayValue(ref,this.asyn,this.popupparams,this.dataimportinitsize,label,this.interceptor,
                    (HttpServletRequest)pageContext.getRequest()));
        }catch(IOException e)
        {
            log.error("显示数据导入标签到页面失败",e);
        }
        return EVAL_PAGE;
    }
}
