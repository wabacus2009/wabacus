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
package com.wabacus.config.dataexport;

import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.xml.XmlElementBean;

public class WordRichExcelExportBean extends AbsDataExportBean
{
    private String dynDataExportTplPath;//如果当前报表显示到Excel/Word是用动态类型的模板（比如jsp、servlet），则在这里指定其访问URI，如果值为Consts_Private.REPORT_TEMPLATE_NONE，则没有显示模板，也不会用框架内置的全局模板

    private TemplateBean dataExportTplBean;
    
    public WordRichExcelExportBean(IComponentConfigBean owner,String type)
    {
        super(owner,type);
    }

    public String getDynDataExportTplPath()
    {
        return dynDataExportTplPath;
    }

    public void setDynDataExportTplPath(String dynDataExportTplPath)
    {
        this.dynDataExportTplPath=dynDataExportTplPath;
    }

    public TemplateBean getDataExportTplBean()
    {
        return dataExportTplBean;
    }

    public void setDataExportTplBean(TemplateBean dataExportTplBean)
    {
        this.dataExportTplBean=dataExportTplBean;
    }

    public Object getDataExportTplObj()
    {
        if(dynDataExportTplPath!=null&&!dynDataExportTplPath.trim().equals("")) return dynDataExportTplPath.trim();
        return dataExportTplBean;
    }
    
    public void loadConfig(XmlElementBean eleDataExport)
    {
        super.loadConfig(eleDataExport);
        String dataexporttemplate=eleDataExport.attributeValue("template");
        if(dataexporttemplate!=null)
        {
            if(dataexporttemplate.trim().equals(""))
            {
                this.dataExportTplBean=null;
                this.dynDataExportTplPath=null;
            }else
            {
                if(ComponentConfigLoadAssistant.getInstance().isStaticTemplateResource(dataexporttemplate))
                {
                    this.dataExportTplBean=ComponentConfigLoadAssistant.getInstance().getStaticTemplateBeanByConfig(this.owner.getPageBean(),dataexporttemplate);
                }else
                {
                    this.dynDataExportTplPath=dataexporttemplate;
                }
            }
        }
    }
}

