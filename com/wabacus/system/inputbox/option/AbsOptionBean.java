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
package com.wabacus.system.inputbox.option;

import com.wabacus.system.dataset.select.common.AbsCommonDataSetValueProvider;
import com.wabacus.system.inputbox.AbsInputBox;

public abstract class AbsOptionBean implements Cloneable
{
    protected AbsCommonDataSetValueProvider datasetProvider;

    protected AbsInputBox ownerInputboxObj;
    
    public AbsOptionBean(AbsInputBox ownerInputboxObj)
    {
        this.ownerInputboxObj=ownerInputboxObj;
    }
    
    public AbsInputBox getOwnerInputboxObj()
    {
        return ownerInputboxObj;
    }
    
    public AbsCommonDataSetValueProvider getDatasetProvider()
    {
        return datasetProvider;
    }

    public void setDatasetProvider(AbsCommonDataSetValueProvider datasetProvider)
    {
        this.datasetProvider=datasetProvider;
    }

    public void doPostLoad()
    {
        if(this.datasetProvider!=null) this.datasetProvider.doPostLoad();
    }
    
    public AbsOptionBean clone(AbsInputBox newOwnerInputboxObj) 
    {
        AbsOptionBean newOptionBean=null;
        try
        {
            newOptionBean=(AbsOptionBean)super.clone();
            newOptionBean.ownerInputboxObj=newOwnerInputboxObj;
            if(datasetProvider!=null)
            {
                newOptionBean.datasetProvider=this.datasetProvider.clone(newOptionBean);
            }
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        return newOptionBean;
    }
    
    
}

