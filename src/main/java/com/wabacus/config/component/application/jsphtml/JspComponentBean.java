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
package com.wabacus.config.component.application.jsphtml;

import com.wabacus.config.Config;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.jsp.JspTemplateApp;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.util.Tools;

public class JspComponentBean extends AbsJspHtmlComponentBean
{
    private String url;

    private boolean isInIFrame;
    
    private String iframestyleproperty;//如果当前JSP是显示在iframe中，这里存放<iframe/>的属性字符串
    
    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url=url;
    }

    public boolean isInIFrame()
    {
        return isInIFrame;
    }

    public String getIframestyleproperty()
    {
        return iframestyleproperty;
    }

    public void setIframestyleproperty(String iframestyleproperty)
    {
        this.iframestyleproperty=iframestyleproperty;
    }

    public JspComponentBean(AbsContainerConfigBean parentContainer)
    {
        super(parentContainer);
    }

    public IComponentType createComponentTypeObj(ReportRequest rrequest,AbsContainerType parentContainer)
    {
        return new JspTemplateApp(parentContainer,this,rrequest);
    }

    public void loadExtendConfig(XmlElementBean eleJspHtml,AbsContainerConfigBean parentConfigBean)
    {
        super.loadExtendConfig(eleJspHtml,parentConfigBean);
        this.url=eleJspHtml.getContent();
        if(this.url==null||this.url.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载"+this.getPath()+"的<jsp/>标签失败，没有在标签内容中指定jsp文件访问URL");
        }
        this.url=this.url.trim();
        String iframe=eleJspHtml.attributeValue("iframe");
        this.isInIFrame=iframe!=null&&iframe.toLowerCase().trim().equals("true");
        if(this.isInIFrame)
        {
            if(this.belongto!=null&&!this.belongto.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载"+this.getPath()+"的<jsp/>标签失败，当其iframe属性为true时，不能配置其belongto属性");
            }
            if(!this.url.startsWith(Config.webroot)&&!this.url.toLowerCase().startsWith("http://"))
            {
                this.url=Config.webroot+"/"+this.url;
                this.url=Tools.replaceAll(this.url,"//","/");
            }
            this.iframestyleproperty=eleJspHtml.attributeValue("iframestyleproperty");
        }
    }    
}
