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

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.xml.XmlElementBean;

public class PlainExcelExportBean extends AbsDataExportBean
{
    private String plainexceltitle="label";

    private int plainexcelsheetsize;

    public PlainExcelExportBean(IComponentConfigBean owner,String type)
    {
        super(owner,type);
    }

    public String getPlainexceltitle()
    {
        return plainexceltitle;
    }

    public void setPlainexceltitle(String plainexceltitle)
    {
        this.plainexceltitle=plainexceltitle;
    }

    public int getPlainexcelsheetsize()
    {
        return plainexcelsheetsize;
    }

    public void loadConfig(XmlElementBean eleDataExport)
    {
        super.loadConfig(eleDataExport);
        String plainexceltitle=eleDataExport.attributeValue("plainexceltitle");
        String sheetsize=eleDataExport.attributeValue("sheetsize");
        if(plainexceltitle!=null)
        {
            this.plainexceltitle=plainexceltitle.toLowerCase().trim();
        }
        if(sheetsize!=null&&!sheetsize.trim().equals(""))
        {
            this.plainexcelsheetsize=Integer.parseInt(sheetsize.trim());
        }else
        {
            this.plainexcelsheetsize=Config.getInstance().getPlainexcelSheetsize();
        }
    }

}
