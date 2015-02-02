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
package com.wabacus.system.component.container.panel;

import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.container.AbsContainerType;

public abstract class AbsPanelType extends AbsContainerType
{
    public AbsPanelType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    public AbsContainerConfigBean loadConfig(XmlElementBean eleContainer,AbsContainerConfigBean parent,String tagname)
    {
        AbsContainerConfigBean configBean=createContainerConfigBean(parent,tagname);
        ComponentConfigLoadManager.loadComponentCommonConfig(eleContainer,configBean);
        ComponentConfigLoadManager.loadContainerCommonConfig(eleContainer,configBean);
        return configBean;
    }
    
    protected abstract AbsContainerConfigBean createContainerConfigBean(AbsContainerConfigBean parentContainer,String tagname);
}
