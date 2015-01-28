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
import com.wabacus.util.Consts;

public class ComponentPartPermissionBean extends AbsPermissionBean
{
    protected String id;
    
    protected AbsPermissionBean parentPermissionBean;

    public ComponentPartPermissionBean(String id,AbsPermissionBean parentPermissionBean)
    {
        this.id=id;
        this.parentPermissionBean=parentPermissionBean;
    }

    public String getId()
    {
        return id;
    }

    public AbsPermissionBean getParentPermissionBean()
    {
        return parentPermissionBean;
    }

    protected IComponentConfigBean getComponentConfigBean()
    {
        if(parentPermissionBean instanceof ComponentPermissionBean)
        {
            return ((ComponentPermissionBean)parentPermissionBean).getComponentConfigBean();
        }
        return ((ComponentPartPermissionBean)parentPermissionBean).getComponentConfigBean();
    }
    
    public void authorize(String partid,String permissiontype,String permissionvalue)
    {
        if(partid==null||partid.trim().equals(""))
        {
            addPermission(permissiontype,permissionvalue);
        }else
        {
            this.getChildPermissionBean(partid,true).authorize(null,permissiontype,permissionvalue);
        }
    }
    
    int checkPermission(String partid,String permissiontype,String permissionvalue)
    {
        if(partid==null||partid.trim().equals(""))
        {
            String myPermissionvalue=this.getPermission(permissiontype);
            if(myPermissionvalue==null||myPermissionvalue.trim().equals("")) return Consts.CHKPERMISSION_EMPTY;
            if(myPermissionvalue.equalsIgnoreCase(permissionvalue))
            {
                return Consts.CHKPERMISSION_YES;
            }else
            {
                return Consts.CHKPERMISSION_NO;
            }
        }else
        {//当前需要判断子元素的子元素权限
            ComponentPartPermissionBean cpauthBean=this.getChildPermissionBean(partid,false);
            if(cpauthBean==null)
            {
                return Consts.CHKPERMISSION_EMPTY;
            }else
            {
                return cpauthBean.checkPermission(null,permissiontype,permissionvalue);
            }
        }
    }
}
