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
package com.wabacus.system.assistant;

import java.util.HashMap;
import java.util.Map;

import com.wabacus.system.permission.permissiontype.AbsPermissionType;
import com.wabacus.system.permission.permissiontype.DisabledPermissionType;
import com.wabacus.system.permission.permissiontype.DisplayPermissionType;
import com.wabacus.system.permission.permissiontype.ReadonlyPermissionType;

public class AuthorizationAssistant
{
    private final static AuthorizationAssistant instance=new AuthorizationAssistant();
    
    private Map<String,AbsPermissionType> mPermissionTypes;
    
    private AuthorizationAssistant()
    {
        mPermissionTypes=new HashMap<String,AbsPermissionType>();
        AbsPermissionType typeObj=new ReadonlyPermissionType();
        mPermissionTypes.put(typeObj.getTypeName(),typeObj);
        typeObj=new DisabledPermissionType();
        mPermissionTypes.put(typeObj.getTypeName(),typeObj);
        typeObj=new DisplayPermissionType();
        mPermissionTypes.put(typeObj.getTypeName(),typeObj);
    }
    
    public static AuthorizationAssistant getInstance()
    {
        return instance;
    }
    
    public boolean isExistPermissiontype(String permissiontype)
    {
        if(permissiontype==null||permissiontype.trim().equals("")) return false;
        return mPermissionTypes.containsKey(permissiontype.trim());
    }
    
    public String getPermissionTypeDefaultvalue(String permissiontype)
    {
        if(!isExistPermissiontype(permissiontype)) return null;
        return mPermissionTypes.get(permissiontype).getDefaultvalue();
    }
    
    public boolean checkDefaultPermissionTypeValue(String permissiontype)
    {
        return checkDefaultPermissionTypeValue(permissiontype,"true");
    }
    
    public boolean checkDefaultPermissionTypeValue(String permissiontype,String permissionvalue)
    {
        if(!isExistPermissiontype(permissiontype)) return false;
        if(!isExistValueOfPermissiontype(permissiontype,permissionvalue)) return false;
        return mPermissionTypes.get(permissiontype).checkDefaultPermissionValue(permissionvalue);
    }
    
    public boolean isExistValueOfPermissiontype(String permissiontype,String permissionvalue)
    {
        if(permissionvalue==null||permissionvalue.trim().equals("")) return false;
        if(!isExistPermissiontype(permissiontype)) return false;
        permissionvalue=permissionvalue.toLowerCase().trim();
        return mPermissionTypes.get(permissiontype).getLstPermissionValues().contains(permissionvalue);
    }
    
    public boolean isConsistentWithParentPermission(String permissiontype,String permissionvalue,boolean parentPermission)
    {
        if(!isExistPermissiontype(permissiontype)) return false;
        return mPermissionTypes.get(permissiontype).isConsistentWithParentPermission(permissionvalue,parentPermission);
    }
}

