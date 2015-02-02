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
package com.wabacus.config.component.application.report;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;

public abstract class AbsConfigBean implements Cloneable
{
    private Map<String,AbsExtendConfigBean> mExtendConfigForReportType=new HashMap<String,AbsExtendConfigBean>();

    private AbsConfigBean parent;

    public AbsConfigBean(AbsConfigBean parent)
    {
        this.parent=parent;
    }

    public void setMExtendConfigForReportType(Map<String,AbsExtendConfigBean> extendConfigForReportType)
    {
        mExtendConfigForReportType=extendConfigForReportType;
    }

    public void setExtendConfigDataForReportType(String key,AbsExtendConfigBean extendConfigDataForReportType)
    {
        mExtendConfigForReportType.put(key,extendConfigDataForReportType);
    }

    public AbsExtendConfigBean getExtendConfigDataForReportType(String key)
    {
        return mExtendConfigForReportType.get(key);
    }

    public AbsExtendConfigBean getExtendConfigDataForReportType(Class extendConfigBeanClass)
    {
        if(mExtendConfigForReportType==null||extendConfigBeanClass==null) return null;
        for(Entry<String,AbsExtendConfigBean> entry:mExtendConfigForReportType.entrySet())
        {
            if(extendConfigBeanClass.isInstance(entry.getValue())) return entry.getValue();
        }
        return null;
    }

    public AbsConfigBean getParent()
    {
        return parent;
    }

    public void setParent(AbsConfigBean parent)
    {
        this.parent=parent;
    }

    public ReportBean getReportBean()
    {
        if(this instanceof ReportBean)
        {
            return (ReportBean)this;
        }
        AbsConfigBean bean=this.getParent();
        if(bean==null) return null;
        while(!(bean instanceof ReportBean))
        {
            bean=bean.getParent();
            if(bean==null) return null;
        }
        return (ReportBean)bean;
    }

    public AbsContainerConfigBean getParentContainer()
    {
        return getReportBean().getParentContainer();
    }

    public PageBean getPageBean()
    {
        return getParentContainer().getPageBean();
    }

    public XmlElementBean getElementBean()
    {
        if(ConfigLoadManager.mAllXmlTagObjects==null) return null;
        return ConfigLoadManager.mAllXmlTagObjects.get(this.toString());
    }
    
    public void setElementBean(XmlElementBean eleBean)
    {
        if(ConfigLoadManager.mAllXmlTagObjects==null) return;
        ConfigLoadManager.mAllXmlTagObjects.put(this.toString(),eleBean);
    }
    
    public AbsConfigBean clone(AbsConfigBean parent)
    {
        try
        {
            AbsConfigBean newbean=(AbsConfigBean)super.clone();
            newbean.setParent(parent);
            newbean.setElementBean(this.getElementBean());
            return newbean;
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("加载报表配置文件失败",e);
        }
    }

    protected void cloneExtendConfig(AbsConfigBean newConfigBean)
    {
        if(mExtendConfigForReportType!=null)
        {
            Map<String,AbsExtendConfigBean> mTemp=new HashMap<String,AbsExtendConfigBean>();
            newConfigBean.setMExtendConfigForReportType(mTemp);
            for(Entry<String,AbsExtendConfigBean> entryTmp:mExtendConfigForReportType.entrySet())
            {
                if(entryTmp.getValue()==null) continue;
                mTemp.put(entryTmp.getKey(),entryTmp.getValue().clone(newConfigBean));
            }
        }
    }
}
