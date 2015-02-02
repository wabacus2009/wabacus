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

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.panel.VerticalPanelBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.util.Consts;

public class VerticalPanel extends AbsPanelType
{
    public VerticalPanel(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(!rrequest.checkPermission(this.containerConfigBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            wresponse.println("&nbsp;");
            return;
        }
        wresponse.println(showContainerStartPart());
        wresponse.println(showContainerTableTag());
        if(rrequest.checkPermission(this.containerConfigBean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            IComponentType childObjTmp;
            for(String childIdTmp:lstChildrenIds)
            {
                wresponse.println("<tr>");
                childObjTmp=this.mChildren.get(childIdTmp);
                showChildObj(childObjTmp,null);
                wresponse.println("</tr>");
            }
        }
        wresponse.println("</table>");
        wresponse.println(showContainerEndPart());
    }

    protected AbsContainerConfigBean createContainerConfigBean(AbsContainerConfigBean parentContainer,String tagname)
    {
        return new VerticalPanelBean(parentContainer,tagname);
    }
    
    protected String getComponentTypeName()
    {
        return "container.vpanel";
    }
}
