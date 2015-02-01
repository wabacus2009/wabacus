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
package com.wabacus.system.component.application.report.configbean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.system.ReportRequest;

public class UltraListReportDisplayBean extends AbsExtendConfigBean
{
    private boolean hasGroupConfig;//是否有<group/>的配置
    
    private List lstChildren;
    
    private Map<String,ColAndGroupTitlePositionBean> mChildrenDefaultPagePositions;
    
    private Map<String,ColAndGroupTitlePositionBean> mChildrenDefaultDataExportPositions;

    public UltraListReportDisplayBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public Map<String,ColAndGroupTitlePositionBean> getMChildrenDefaultPagePositions()
    {
        return mChildrenDefaultPagePositions;
    }

    public void setMChildrenDefaultPagePositions(Map<String,ColAndGroupTitlePositionBean> childrenDefaultPagePositions)
    {
        mChildrenDefaultPagePositions=childrenDefaultPagePositions;
    }

    public Map<String,ColAndGroupTitlePositionBean> getMChildrenDefaultDataExportPositions()
    {
        return mChildrenDefaultDataExportPositions;
    }

    public void setMChildrenDefaultDataExportPositions(Map<String,ColAndGroupTitlePositionBean> childrenDefaultDataExportPositions)
    {
        mChildrenDefaultDataExportPositions=childrenDefaultDataExportPositions;
    }

    public boolean isHasGroupConfig(ReportRequest rrequest)
    {
        if(rrequest==null) return hasGroupConfig;
        String flag=rrequest.getStringAttribute(this.getOwner().getReportBean().getId(),"WX_IS_HAS_GROUP_CONFIG",null);
        if(flag==null||flag.trim().equals("")) return hasGroupConfig;
        return flag.toLowerCase().trim().equals("true");
    }

    public void setHasGroupConfig(boolean hasGroupConfig)
    {
        this.hasGroupConfig=hasGroupConfig;
    }

    public List getLstChildren()
    {
        return lstChildren;
    }
    
    public void setLstChildren(List lstChildren)
    {
        this.lstChildren=lstChildren;
    }

    public UltraListReportGroupBean getGroupBeanById(String groupid)
    {
        if(groupid==null||groupid.trim().equals("")) return null;
        if(lstChildren==null||lstChildren.size()==0) return null;
        for(Object obj:lstChildren)
        {
            if(obj==null||obj instanceof ColBean) continue;
            if(groupid.equals(((UltraListReportGroupBean)obj).getGroupid()))
                return (UltraListReportGroupBean)obj;
            obj=((UltraListReportGroupBean)obj).getGroupBeanById(groupid);
            if(obj!=null) return (UltraListReportGroupBean)obj;
        }
        return null;
    }
    
    public boolean removeChildColBeanByColumn(String column,boolean inherit)
    {
        if(column==null||column.trim().equals("")) return false;
        if(lstChildren==null||lstChildren.size()==0) return false;
        boolean result=false;
        Object obj=null;
        for(int i=lstChildren.size()-1;i>=0;i--)
        {
            obj=lstChildren.get(i);
            if(obj==null) continue;
            if(obj instanceof ColBean)
            {
                if(((ColBean)obj).getColumn().equals(column))
                {
                    lstChildren.remove(i);
                    result=true;
                }
            }else if(obj instanceof UltraListReportGroupBean)
            {
                if(inherit)
                {
                    boolean flag=((UltraListReportGroupBean)obj).removeChildColBeanByColumn(column,true);
                    if(flag)
                    {
                        if(((UltraListReportGroupBean)obj).getLstChildren()==null||((UltraListReportGroupBean)obj).getLstChildren().size()==0)
                        {
                            lstChildren.remove(i);
                        }
                        result=true;
                    }
                }
            }
        }
        return result;
    }
    
    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        DisplayBean disbean=(DisplayBean)owner;
        UltraListReportDisplayBean beanNew=(UltraListReportDisplayBean)super.clone(owner);
        if(beanNew.getLstChildren()!=null&&beanNew.getLstChildren().size()>0)
        {
            List lstTemp=new ArrayList();
            ColBean cbTmp;
            for(int i=0;i<beanNew.getLstChildren().size();i++)
            {
                Object obj=beanNew.getLstChildren().get(i);
                if(obj==null) continue;
                if(obj instanceof ColBean)
                {
                    cbTmp=(ColBean)obj;
                    cbTmp=disbean.getColBeanByColId(cbTmp.getColid());
                    lstTemp.add(cbTmp);
                }else if(obj instanceof AbsExtendConfigBean)
                {
                    lstTemp.add(((AbsExtendConfigBean)obj).clone(owner));
                }else if(obj instanceof AbsConfigBean)
                {
                    lstTemp.add(((AbsConfigBean)obj).clone(owner));
                }
            }
            beanNew.setLstChildren(lstTemp);
            beanNew.mChildrenDefaultPagePositions=cloneDefaultPositions(this.mChildrenDefaultPagePositions);
            beanNew.mChildrenDefaultDataExportPositions=cloneDefaultPositions(this.mChildrenDefaultDataExportPositions);
        }
        
        return beanNew;
    }
    
    private Map<String,ColAndGroupTitlePositionBean> cloneDefaultPositions(Map<String,ColAndGroupTitlePositionBean> mChildrenDefaultPositions)
    {
        try
        {
            if(mChildrenDefaultPositions!=null)
            {
                Map<String,ColAndGroupTitlePositionBean> mChildrenPositionsNew=new HashMap<String,ColAndGroupTitlePositionBean>();
                for(Entry<String,ColAndGroupTitlePositionBean> entry:mChildrenDefaultPositions.entrySet())
                {
                    mChildrenPositionsNew.put(entry.getKey(),(ColAndGroupTitlePositionBean)entry
                            .getValue().clone());
                }
                return mChildrenPositionsNew;
            }
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
