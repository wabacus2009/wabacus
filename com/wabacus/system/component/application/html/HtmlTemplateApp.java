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
package com.wabacus.system.component.application.html;

import java.util.List;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.jsphtml.HtmlComponentBean;
import com.wabacus.config.print.PrintSubPageBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.application.AbsApplicationType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class HtmlTemplateApp extends AbsApplicationType
{
    private HtmlComponentBean htmlConfigBean;

    public HtmlTemplateApp(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        htmlConfigBean=(HtmlComponentBean)comCfgBean;
    }
    
    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {}

    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(!rrequest.checkPermission(this.htmlConfigBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            wresponse.println("&nbsp;");
            return;
        }
        if(htmlConfigBean.getTplBean()==null)
        {
            wresponse.println("&nbsp;");
            return;
        }
        StringBuffer tempBuf=new StringBuffer();
        String width=null;
        if(this.getParentContainerType()!=null)
        {
            width=this.getParentContainerType().getChildDisplayWidth(this.htmlConfigBean);
            if(width==null||width.trim().equals("")) width="100%";
            if(htmlConfigBean.getTop()!=null&&!htmlConfigBean.getTop().trim().equals(""))
            {
                wresponse.println("<table  cellspacing='0' cellpadding='0' width=\""+width+"\" style=\"MARGIN:0;\">");
                wresponse.println("<tr><td height=\""+htmlConfigBean.getTop()+"\">&nbsp;</td></tr></table>");
            }
            tempBuf.append("<table  cellspacing='0' cellpadding='0' width=\""+width+"\" id=\""+htmlConfigBean.getGuid()+"\"");
            if(htmlConfigBean.getHeight()!=null&&!htmlConfigBean.getHeight().trim().equals(""))
            {
                tempBuf.append(" height=\""+htmlConfigBean.getHeight()+"\" ");
            }
            tempBuf.append("><tr><td valign=\"top\">");
            wresponse.println(tempBuf.toString());
        }
        wresponse.println("<div id=\"WX_CONTENT_"+htmlConfigBean.getGuid()+"\">");
        printlnHtmReallValue();
        wresponse.println("</div>");
        if(this.getParentContainerType()!=null)
        {
            wresponse.println("</td></tr></table>");
            if(this.htmlConfigBean.getBottom()!=null&&!this.htmlConfigBean.getBottom().trim().equals(""))
            {
                wresponse.println("<table  cellspacing='0' cellpadding='0' width=\""+width+"\" style=\"MARGIN:0;\">");
                wresponse.println("<tr><td height=\""+this.htmlConfigBean.getBottom()+"\">&nbsp;</td></tr></table>");
            }
        }
    }

    public String getRealParenttitle()
    {
        String parenttitle=htmlConfigBean.getParenttitle(rrequest);
        if(parenttitle==null) return "";
        return parenttitle.trim();//这种组件没有title属性，所以不调用htmlConfigBean.getTitle(rrequest);
    }

    protected String getComponentTypeName()
    {
        return "application.html";
    }

    public void displayOnExportDataFile(Object templateObj,boolean isFirstime)
    {
        displayOnPage(null);
    }
    
    public void printApplication(List<PrintSubPageBean> lstPrintPagebeans)
    {
        if(!rrequest.checkPermission(this.htmlConfigBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            wresponse.println("&nbsp;");
            return;
        }
        if(this.htmlConfigBean.getPrintwidth()!=null&&!this.htmlConfigBean.getPrintwidth().trim().equals(""))
        {
            this.wresponse.println("<div width=\""+this.htmlConfigBean.getPrintwidth()+"\">");
        }
        printlnHtmReallValue();
        if(this.htmlConfigBean.getPrintwidth()!=null&&!this.htmlConfigBean.getPrintwidth().trim().equals(""))
        {
            this.wresponse.println("</div>");
        }
    }
    
    private void printlnHtmReallValue()
    {
        AbsComponentType realDisplayComTypeObj=this;
        if(this.htmlConfigBean.getBelongToCcbean()!=null)
        {//当前<html/>属于一个组件，则取到真正的组件类型对象，稍后可能会用自定义标签显示它们的内容
            realDisplayComTypeObj=(AbsComponentType)rrequest.getComponentTypeObj(this.htmlConfigBean.getBelongToCcbean(),null,false);
        }
        if(htmlConfigBean.getTplBean().getLstTagChildren()!=null&&htmlConfigBean.getTplBean().getLstTagChildren().size()>0)
        {
            htmlConfigBean.getTplBean().printDisplayValue(rrequest,realDisplayComTypeObj);
        }else
        {
            String tplcontent=this.htmlConfigBean.getTplBean().getContent();
            if(tplcontent==null||!Tools.isDefineKey("i18n",tplcontent.trim()))
            {
                wresponse.println(tplcontent);
            }else
            {
                Object obj=rrequest.getI18NObjectValue(tplcontent.trim());
                if(obj==null)
                {
                    wresponse.println("&nbsp;");
                }else
                {
                    if(!(obj instanceof TemplateBean))
                    {
                        wresponse.println(obj.toString());
                    }else
                    {
                        ((TemplateBean)obj).printDisplayValue(rrequest,realDisplayComTypeObj);
                    }
                }
            }
        }
    }
}
