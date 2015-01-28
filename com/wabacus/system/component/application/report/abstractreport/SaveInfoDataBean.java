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
package com.wabacus.system.component.application.report.abstractreport;


public class SaveInfoDataBean
{
    protected String updatetype;
    
    protected boolean[] shouldDoSave;

    public SaveInfoDataBean()
    {
        shouldDoSave=new boolean[]{false,false,false,false};
    }
    
    public boolean[] getShouldDoSave()
    {
        return shouldDoSave;
    }

    public void setShouldDoSave(boolean[] shouldDoSave)
    {
        this.shouldDoSave=shouldDoSave;
    }

    public String getUpdatetype()
    {
        return updatetype;
    }

    public void setUpdatetype(String updatetype)
    {
        this.updatetype=updatetype;
    }

    public boolean hasDeleteData()
    {
        if(shouldDoSave==null||shouldDoSave.length!=4) return false;
        if(shouldDoSave[2]) return true;
        if(shouldDoSave[3]&&"delete".equals(this.updatetype)) return true;
        return false;
    }

    public boolean hasSavingData()
    {
        if(shouldDoSave==null||shouldDoSave.length!=4) return false;
        if(shouldDoSave[0]||shouldDoSave[1]) return true;
        if(shouldDoSave[3]&&(updatetype==null||!updatetype.equals("delete"))) return true;
        return false;
    }
}
