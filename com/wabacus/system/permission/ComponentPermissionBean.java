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
package com.wabacus.system.permission;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.system.assistant.AuthorizationAssistant;
import com.wabacus.util.Consts;

public class ComponentPermissionBean extends AbsPermissionBean
{
    protected IComponentConfigBean ccbean; 
    
    public ComponentPermissionBean(IComponentConfigBean ccbean)
    {
        this.ccbean=ccbean;
    }

    public IComponentConfigBean getComponentConfigBean()
    {
        return ccbean;
    }

    public void authorize(String parttype,String partid,String permissiontype,String permissionvalue)
    {
        if(parttype==null||parttype.trim().equals(""))
        {
            addPermission(permissiontype,permissionvalue);
        }else
        {
            this.getChildPermissionBean(parttype,true).authorize(partid,permissiontype,permissionvalue);
        }
    }

    public int checkPermission(String parttype,String partid,String permissiontype,String permissionvalue)
    {
        if(!AuthorizationAssistant.getInstance().isExistPermissiontype(permissiontype)) return Consts.CHKPERMISSION_UNSUPPORTEDTYPE;
        if(!AuthorizationAssistant.getInstance().isExistValueOfPermissiontype(permissiontype,permissionvalue))
            return Consts.CHKPERMISSION_UNSUPPORTEDVALUE;
        //如果本次是判断当前组件的权限
        if(parttype==null||parttype.trim().equals(""))
        {
            permissiontype=permissiontype.trim();
            permissionvalue=permissionvalue.toLowerCase().trim();
            String myPermissionvalue=this.getPermission(permissiontype);
            if(myPermissionvalue==null||myPermissionvalue.trim().equals("")) return Consts.CHKPERMISSION_EMPTY;
            if(myPermissionvalue.toLowerCase().trim().equals(permissionvalue))
            {//显式授的权限值即为要判断的permissionvalue
                return Consts.CHKPERMISSION_YES;
            }else
            {
                return Consts.CHKPERMISSION_NO;
            }
        }else
        {
            ComponentPartPermissionBean cpauthBean=this.getChildPermissionBean(parttype,false);
            if(cpauthBean==null) return Consts.CHKPERMISSION_EMPTY;
            return cpauthBean.checkPermission(partid,permissiontype,permissionvalue);
        }
    }
}

