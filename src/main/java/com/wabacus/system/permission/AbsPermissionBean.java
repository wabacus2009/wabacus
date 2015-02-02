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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.AuthorizationAssistant;

public abstract class AbsPermissionBean
{
    private static Log log=LogFactory.getLog(AbsPermissionBean.class);
    
    protected ReportRequest rrequest;//如果当前bean是存放组件/元素的默认权限，则此对象为空
    
    protected Map<String,String> mPermissions;

    protected Map<String,ComponentPartPermissionBean> mChildrenPermissionBeans;

    public Map<String,String> getMPermissions()
    {
        return mPermissions;
    }

    public void setMPermissions(Map<String,String> mPermissions)
    {
        this.mPermissions=mPermissions;
    }

    public void setRRequest(ReportRequest rrequest)
    {
        this.rrequest=rrequest;
    }

    public Map<String,ComponentPartPermissionBean> getMChildrenPermissionBeans()
    {
        return mChildrenPermissionBeans;
    }

    protected ComponentPartPermissionBean getChildPermissionBean(String childid,boolean shouldCreate)
    {
        ComponentPartPermissionBean cpabean=null;
        if(mChildrenPermissionBeans!=null)  cpabean=mChildrenPermissionBeans.get(childid);
        if(cpabean!=null||!shouldCreate) return cpabean;
        if(mChildrenPermissionBeans==null) mChildrenPermissionBeans=new HashMap<String,ComponentPartPermissionBean>();
        cpabean=new ComponentPartPermissionBean(childid,this);
        cpabean.setRRequest(rrequest);
        mChildrenPermissionBeans.put(childid,cpabean);
        return cpabean;
    }
    
    protected void addPermission(String permissiontype,String permissionvalue)
    {
        if(!AuthorizationAssistant.getInstance().isExistPermissiontype(permissiontype))
        {
            log.error("给组件"+getComponentConfigBean().getPath()+"授权失败，权限类型为空，或框架不支持"+permissiontype+"权限类型");
            return;
        }
        if(!AuthorizationAssistant.getInstance().isExistValueOfPermissiontype(permissiontype,permissionvalue.trim()))
        {
            log.error("为组件"+this.getComponentConfigBean().getPath()+"或其上元素授权时，传入的权限值"+permissionvalue+"为空或在权限类型"+permissiontype+"中不支持");
            return;
        }
        if(mPermissions==null) mPermissions=new HashMap<String,String>();
        mPermissions.put(permissiontype,permissionvalue);
    }
    
    protected String getPermission(String permissiontype)
    {
        if(!AuthorizationAssistant.getInstance().isExistPermissiontype(permissiontype))
        {//框架不支持这个权限类型
            return null;
        }
        if(mPermissions==null) return null;
        return mPermissions.get(permissiontype);
    }
    
    protected abstract IComponentConfigBean getComponentConfigBean();    
}
