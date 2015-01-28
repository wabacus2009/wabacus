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
package com.wabacus.system.component.application.report.configbean.editablereport;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;

public class EditableReportBean extends AbsExtendConfigBean
{
    private Boolean checkdirtydata;

    private String configSaveDatatype;//在<report/>中配置的保存数据类型
    
    private String savedatatype;
    
    public EditableReportBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public boolean isCheckdirtydata()
    {
        if(checkdirtydata==null)
        {
            this.checkdirtydata=Config.getInstance().getSystemConfigValue("default-checkdirtydata",true);
        }
        return checkdirtydata.booleanValue();
    }

    public void setCheckdirtydata(Boolean checkdirtydata)
    {
        this.checkdirtydata=checkdirtydata;
    }

    public String getConfigSavedatatype()
    {
        return this.configSaveDatatype;
    }
    
    public String getSavedatatype()
    {
        if(savedatatype==null||savedatatype.trim().equals(""))
        {
            savedatatype=Config.getInstance().getSystemConfigValue("default-savedatatype","realchanged");
        }
        return savedatatype;
    }

    public void setSavedatatype(String savedatatype)
    {
        this.configSaveDatatype=savedatatype;
        this.savedatatype=savedatatype;
    }
}
