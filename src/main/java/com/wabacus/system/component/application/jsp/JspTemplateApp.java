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
package com.wabacus.system.component.application.jsp;

import java.util.List;

import javax.servlet.RequestDispatcher;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.jsphtml.JspComponentBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.print.PrintSubPageBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.AbsApplicationType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class JspTemplateApp extends AbsApplicationType
{
    private JspComponentBean jspConfigBean;

    public JspTemplateApp(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        jspConfigBean=(JspComponentBean)comCfgBean;
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {}

    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(jspConfigBean.getUrl()==null||jspConfigBean.getUrl().trim().equals(""))
        {
            wresponse.println("&nbsp;");
            return;
        }
        if(!rrequest.checkPermission(this.jspConfigBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            wresponse.println("&nbsp;");
            return;
        }
        String width=null;
        if(this.getParentContainerType()!=null)
        {
            width=this.getParentContainerType().getChildDisplayWidth(this.jspConfigBean);
            if(width==null||width.trim().equals("")) width="100%";
            if(jspConfigBean.getTop()!=null&&!jspConfigBean.getTop().trim().equals(""))
            {
                wresponse.println("<table  cellspacing='0' cellpadding='0' width=\""+width+"\" style=\"MARGIN:0;\">");
                wresponse.println("<tr><td height=\""+jspConfigBean.getTop()+"\">&nbsp;</td></tr></table>");
            }
            wresponse.println("<table  cellspacing='0' cellpadding='0' width=\""+width+"\" id=\""+jspConfigBean.getGuid()+"\"");
            if(jspConfigBean.getHeight()!=null&&!jspConfigBean.getHeight().trim().equals(""))
            {
                wresponse.println(" height=\""+jspConfigBean.getHeight()+"\" ");
            }
            wresponse.println("><tr><td valign=\"top\">");
        }
        wresponse.println("<div id=\"WX_CONTENT_"+jspConfigBean.getGuid()+"\">");
        if(jspConfigBean.isInIFrame())
        {
            String jspContent="<iframe src=\""+jspConfigBean.getUrl()+"\"";
            if(jspConfigBean.getIframestyleproperty()!=null)
            {
                jspContent=jspContent+" "+jspConfigBean.getIframestyleproperty();
            }
            jspContent=jspContent+"></iframe>";
            wresponse.println(jspContent);
        }else
        {
            rrequest.getRequest().setAttribute("WX_JSPCOMPONENTBEAN",this.jspConfigBean);
            if(this.jspConfigBean.getBelongToCcbean() instanceof ReportBean)
            {
                AbsReportType reportTypeObj=rrequest.getDisplayReportTypeObj(this.jspConfigBean.getBelongToCcbean().getId());
                rrequest.getRequest().setAttribute("WX_COMPONENT_OBJ",reportTypeObj);//存进去，以便JSP中的自定义标签能正常使用
            }
            try
            {
                RequestDispatcher rd=rrequest.getRequest().getRequestDispatcher(jspConfigBean.getUrl().trim());
                rd.include(rrequest.getRequest(),rrequest.getWResponse().getResponse());
            }catch(Exception e)
            {
                throw new WabacusRuntimeException("显示JSP"+jspConfigBean.getUrl()+"失败",e);
            }
        }
        wresponse.println("</div>");
        if(this.getParentContainerType()!=null)
        {
            wresponse.println("</td></tr></table>");
            if(this.jspConfigBean.getBottom()!=null&&!this.jspConfigBean.getBottom().trim().equals(""))
            {
                wresponse.println("<table  cellspacing='0' cellpadding='0' width=\""+width+"\" style=\"MARGIN:0;\">");
                wresponse.println("<tr><td height=\""+this.jspConfigBean.getBottom()+"\">&nbsp;</td></tr></table>");
            }
        }
    }

    public String getRealParenttitle()
    {
        String parenttitle=jspConfigBean.getParenttitle(rrequest);
        if(parenttitle==null) return "";
        return parenttitle.trim();
    }

    protected String getComponentTypeName()
    {
        return "application.jsp";
    }

    public void displayOnExportDataFile(Object templateObj,boolean isFirstime)
    {
        displayOnPage(null);
    }
    
    public void printApplication(List<PrintSubPageBean> lstPrintPagebeans)
    {
        if(this.jspConfigBean.getPrintwidth()!=null&&!this.jspConfigBean.getPrintwidth().trim().equals(""))
        {
            this.wresponse.println("<div width=\""+this.jspConfigBean.getPrintwidth()+"\">");
        }
        String jspUrl=null;
        if(jspConfigBean.isInIFrame())
        {
            jspUrl=jspConfigBean.getUrl();
            if(jspUrl.toLowerCase().trim().startsWith("http://"))
            {
                jspUrl=null;
            }else if(!jspUrl.trim().startsWith(Config.webroot))
            {
                jspUrl=Tools.replaceAll(Config.webroot+"/"+jspUrl,"//","/");
            }
        }else
        {
            rrequest.getRequest().setAttribute("WX_JSPCOMPONENTBEAN",this.jspConfigBean);
            if(this.jspConfigBean.getBelongToCcbean() instanceof ReportBean)
            {
                AbsReportType reportTypeObj=rrequest.getDisplayReportTypeObj(this.jspConfigBean.getBelongToCcbean().getId());
                rrequest.getRequest().setAttribute("WX_COMPONENT_OBJ",reportTypeObj);//存进去，以便JSP中的自定义标签能正常使用
            }
            jspUrl=jspConfigBean.getUrl().trim();
        }
        if(jspUrl!=null&&!jspUrl.trim().equals(""))
        {
            try
            {
                RequestDispatcher rd=rrequest.getRequest().getRequestDispatcher(jspUrl);
                rd.include(rrequest.getRequest(),rrequest.getWResponse().getResponse());
            }catch(Exception e)
            {
                throw new WabacusRuntimeException("打印JSP"+jspUrl+"失败",e);
            }
        }
        if(this.jspConfigBean.getPrintwidth()!=null&&!this.jspConfigBean.getPrintwidth().trim().equals(""))
        {
            this.wresponse.println("</div>");
        }
    }
}
