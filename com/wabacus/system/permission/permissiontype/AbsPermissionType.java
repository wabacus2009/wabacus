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
package com.wabacus.system.permission.permissiontype;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.util.Consts;

public abstract class AbsPermissionType
{
    protected String typeName;
    
    protected String defaultvalue;
    
    private List<String> lstPermissionValues;

    public AbsPermissionType()
    {
        this.lstPermissionValues=new ArrayList<String>();
    }
    
    public String getTypeName()
    {
        return typeName;
    }

    public String getDefaultvalue()
    {
        return defaultvalue;
    }

    public List<String> getLstPermissionValues()
    {
        return lstPermissionValues;
    }

    protected void registerPermissionValue(String value)
    {
        if(value==null||value.trim().equals("")) return;
        value=value.toLowerCase().trim();
        if(!lstPermissionValues.contains(value))
        {
            lstPermissionValues.add(value);
        }
    }

    public abstract boolean isConsistentWithParentPermission(String permissionvalue,boolean parentPermission);
    
    public boolean checkDefaultPermissionValue(String permissionvalue)
    {
        if(defaultvalue==null||defaultvalue.trim().equals("")) return false;
        if(!this.lstPermissionValues.contains(permissionvalue)) return false;
        return permissionvalue.equalsIgnoreCase(defaultvalue);
    }
}

