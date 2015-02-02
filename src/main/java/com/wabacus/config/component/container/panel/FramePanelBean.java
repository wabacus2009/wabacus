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
package com.wabacus.config.component.container.panel;

import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.component.container.panel.FramePanel;

public class FramePanelBean extends AbsContainerConfigBean
{

    public FramePanelBean(AbsContainerConfigBean parentContainer,String tagname)
    {
        super(parentContainer,tagname);
    }

    public IComponentType createComponentTypeObj(ReportRequest rrequest,AbsContainerType parentContainer)
    {
        return new FramePanel(parentContainer,this,rrequest);
    }

}
