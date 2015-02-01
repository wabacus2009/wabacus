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
import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.template.TemplateParser;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.html.HtmlTemplateApp;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.util.Tools;

public class HtmlComponentBean extends AbsJspHtmlComponentBean
{
    private TemplateBean tplBean;

    public HtmlComponentBean(AbsContainerConfigBean parentContainer)
    {
        super(parentContainer);
    }

    public TemplateBean getTplBean()
    {
        return tplBean;
    }

    public void setTplBean(TemplateBean tplBean)
    {
        this.tplBean=tplBean;
    }

    public IComponentType createComponentTypeObj(ReportRequest rrequest,AbsContainerType parentContainer)
    {
        return new HtmlTemplateApp(parentContainer,this,rrequest);
    }

    public void loadExtendConfig(XmlElementBean eleJspHtml,AbsContainerConfigBean parentConfigBean)
    {
        super.loadExtendConfig(eleJspHtml,parentConfigBean);
        String content=eleJspHtml.getContent();
        if(content==null||content.trim().equals(""))
        {
            this.tplBean=null;
            return;
        }
        if(ComponentConfigLoadAssistant.getInstance().isStaticTemplateResource(content))
        {
            if(Tools.isDefineKey("$",content))
            {
                Object obj=Config.getInstance().getResourceObject(null,parentConfigBean.getPageBean(),content,true);
                if(obj==null) obj="";
                if(obj instanceof TemplateBean)
                {
                    tplBean=(TemplateBean)obj;
                }else
                {
                    createTplBean(obj.toString());
                }
            }else
            {//取html/htm文件中的模板
                createTplBean(WabacusAssistant.getInstance().readFileContentByPath(content));
            }
        }else
        {//配置为普通字符串
            createTplBean(content.trim());
        }

    }

    private void createTplBean(String content)
    {
        if(this.belongto==null||this.belongto.trim().equals(""))
        {
            tplBean=new TemplateBean();
            tplBean.setContent(content);
        }else
        {
            tplBean=TemplateParser.parseTemplateByContent(content);
        }
    }
}
