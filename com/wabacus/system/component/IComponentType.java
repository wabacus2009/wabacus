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
package com.wabacus.system.component;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.tags.component.AbsComponentTag;

public interface IComponentType
{
    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest);
    
    public String getRealParenttitle();
    
    public AbsContainerType getParentContainerType();
    
    public void setParentContainerType(AbsContainerType parentContainer);
    
    public IComponentConfigBean getConfigBean();
    
    public void displayOnPage(AbsComponentTag displayTag);
    
    public void displayOnExportDataFile(Object templateObj,boolean isFirstime);

    public ReportRequest getReportRequest();
}
