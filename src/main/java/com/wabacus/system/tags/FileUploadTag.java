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

public class FileUploadTag extends BodyTagSupport
{
    private static final long serialVersionUID=-6877305901882687761L;
    
    private final static Log log=LogFactory.getLog(FileUploadTag.class);
    
    private String maxsize;
    
    private String allowtypes;
    
    private String disallowtypes;
    
    private String uploadcount="1";
    
    private String newfilename;//文件上传的新文件名，如果没有配置，则不改变上传文件的文件名，保留原名，只有uploadcount为1时才能指定此属性
    
    private String savepath;
    
    private String rooturl;
    
    private String initsize;//初始大小，可配置值包括min/max/normal，分别表示最大化、最小化、正常窗口大小（即上面pagewidth/pageheight配置的大小）
    
    private String popupparams;
    
    private String beforepopup=null;
    
    private String interceptor;//拦截器类
    
    public void setMaxsize(String maxsize)
    {
        this.maxsize=maxsize;
    }

    public void setAllowtypes(String allowtypes)
    {
        this.allowtypes=allowtypes;
    }

    public void setDisallowtypes(String disallowtypes)
    {
        this.disallowtypes=disallowtypes;
    }

    public void setUploadcount(String uploadcount)
    {
        this.uploadcount=uploadcount;
    }

    public void setNewfilename(String newfilename)
    {
        this.newfilename=newfilename;
    }

    public void setSavepath(String savepath)
    {
        this.savepath=savepath;
    }

    public void setRooturl(String rooturl)
    {
        this.rooturl=rooturl;
    }

    public void setInitsize(String initsize)
    {
        this.initsize=initsize;
    }

    public void setPopupparams(String popupparams)
    {
        this.popupparams=popupparams;
    }

    public void setBeforepopup(String beforepopup)
    {
        this.beforepopup=beforepopup;
    }

    public String getInterceptor()
    {
        return interceptor;
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
        BodyContent bc=getBodyContent();
        String label=null;
        if(bc!=null) label=bc.getString();
        JspWriter out=pageContext.getOut();
        try
        {
            out.println(TagAssistant.getInstance().getFileUploadDisplayValue(maxsize,allowtypes,disallowtypes,uploadcount,newfilename,savepath,rooturl,popupparams,
                    this.initsize,this.interceptor,label,this.beforepopup,(HttpServletRequest)pageContext.getRequest()));
        }catch(IOException e)
        {
            log.error("显示文件上传标签失败",e);
        }
        return EVAL_PAGE;
    }
}
