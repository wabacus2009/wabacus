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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts;

public abstract class AbsComponentTag extends BodyTagSupport
{
    private static final long serialVersionUID=-1473494329815755344L;
 
    private final static Log log=LogFactory.getLog(AbsComponentTag.class);

    protected HttpServletRequest request;
    
    protected ReportRequest rrequest;
    
    protected AbsComponentType ownerComponentObj;
    
    protected AbsComponentType displayComponentObj;
    
    protected JspWriter out;

    private boolean includeChild;
    protected String componentid;
    
    public boolean isIncludeChild()
    {
        return includeChild;
    }
    public void setIncludeChild(boolean includeChild)
    {
        this.includeChild=includeChild;
    }
    public String getComponentid()
    {
        return componentid;
    }
    public void setComponentid(String componentid)
    {
        this.componentid=componentid;
    }
    
    protected AbsComponentTag getMyComponentParentTag()
    {
        Tag tagParent=this.getParent();
        while(tagParent!=null)
        {
            if(tagParent instanceof AbsComponentTag)
            {//如果当前父标签是框架的自定义标签
                return (AbsComponentTag)tagParent;
            }
            tagParent=tagParent.getParent();
        }
        return null;
    }

    protected boolean isRootTag()
    {
        if(getMyComponentParentTag()==null) return true;
        return false;
    }
    
    public int doStartTag() throws JspException
    {
        initTagState();
        request=(HttpServletRequest)pageContext.getRequest();
        out=pageContext.getOut();
        rrequest=(ReportRequest)request.getAttribute("WX_REPORTREQUEST");
        this.ownerComponentObj=(AbsReportType)request.getAttribute("WX_COMPONENT_OBJ");
        rrequest.getWResponse().setJspout(this.out);
        if(isRootTag())
        {
            if(rrequest==null) throw new JspException("没有取到ReportRequest对象，无法显示报表");
            if(this.ownerComponentObj==null) throw new JspException("没有取到组件对象，无法显示组件");
        }
        initDisplayComponentObj();
        try
        {
            return doMyStartTag();
        }catch(Exception e)
        {
            log.error("显示页面"+rrequest.getPagebean().getId()+"失败",e);
            rrequest.getWResponse().getMessageCollector().error("显示页面失败",null,true);
            return SKIP_BODY;
        }
    }
    
    private void initTagState()
    {
        this.id=null;
        this.out=null;
        this.displayComponentObj=null;
        this.ownerComponentObj=null;
        this.request=null;
        this.rrequest=null;
        this.includeChild=false;
    }
    
    private void initDisplayComponentObj()
    {
        if(this.componentid==null||this.componentid.trim().equals("")||this.componentid.equals(this.ownerComponentObj.getConfigBean().getId()))
        {//如果指定的是显示本组件的相应部分
            this.displayComponentObj=this.ownerComponentObj;
            return;
        }
        IComponentType componentObj=rrequest.getComponentTypeObj(this.componentid.trim(),null,false);
        if(componentObj==null)
        {
            throw new WabacusRuntimeException("页面"+rrequest.getPagebean().getId()+"下不存在id为"+componentid+"的组件");
        }
        this.displayComponentObj=(AbsComponentType)componentObj;
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            if(this.displayComponentObj instanceof AbsReportType)
            {
                ReportBean rbDisplay=(ReportBean)this.displayComponentObj.getConfigBean();
                if(rbDisplay.isSlaveReportDependsonListReport())
                {
                    throw new WabacusRuntimeException(rbDisplay.getPath()+"是从报表，不能在id为"+this.ownerComponentObj.getConfigBean().getPath()
                            +"的组件的动态模板中显示它的内容");
                }
            }
            if(this.ownerComponentObj instanceof AbsReportType)
            {
                ReportBean rbOwner=(ReportBean)this.ownerComponentObj.getConfigBean();
                if(rbOwner.isSlaveReportDependsonListReport())
                {
                    throw new WabacusRuntimeException(rbOwner.getPath()+"是从报表，不能其动态模板中显示其它组件的内容");
                }
            }
        }
    }
    
    public int doEndTag() throws JspException
    {
        try
        {
            int flag= doMyEndTag();
            rrequest.getWResponse().setJspout(null);
            return flag;
        }catch(Exception e)
        {
            log.error("显示页面"+rrequest.getPagebean().getId()+"失败",e);
            rrequest.getWResponse().getMessageCollector().error("显示页面失败",null,true);
            return EVAL_PAGE;
        }
    }

    protected void println(String content)
    {
        rrequest.getWResponse().println(content);
    }
    
    public boolean isDisplayByMySelf()
    {
        if(this.componentid==null||this.componentid.trim().equals("")||this.componentid.equals(this.ownerComponentObj.getConfigBean().getId()))
        {
            return true;
        }else
        {
            return false;
        }
    }
    
    public abstract int doMyStartTag() throws JspException,IOException;

    public abstract int doMyEndTag() throws JspException,IOException;
}

