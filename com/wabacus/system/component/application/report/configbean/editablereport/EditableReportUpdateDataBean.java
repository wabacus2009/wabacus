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
package com.wabacus.system.component.application.report.configbean.editablereport;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.util.Consts;

public class EditableReportUpdateDataBean extends AbsEditableReportEditDataBean
{
    private boolean isEnableCrossPageEdit;//是否允许跨页编辑数据，只对editablelist2/listform报表类型有效

    public EditableReportUpdateDataBean(IEditableReportEditGroupOwnerBean owner)
    {
        super(owner);
    }

    public boolean isEnableCrossPageEdit()
    {
        return isEnableCrossPageEdit;
    }

    public void setEnableCrossPageEdit(boolean isEnableCrossPageEdit)
    {
        this.isEnableCrossPageEdit=isEnableCrossPageEdit;
    }

    public void setParamBeanInfoOfColBean(ColBean cbUpdateSrc,EditableReportParamBean paramBean,String configColProperty,String reportTypeKey)
    {
        /*if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbUpdateSrc.getDisplaytype(true)))
        {
            if(configColProperty.endsWith("__old")) configColProperty=configColProperty.substring(0,configColProperty.length()-"__old".length());
        }else if(!configColProperty.endsWith("__old"))
        {
            EditableReportColBean ercbeanUpdateSrc=(EditableReportColBean)cbUpdateSrc.getExtendConfigDataForReportType(reportTypeKey);
            ercbeanUpdateSrc.setEditableWhenUpdate(2);
        }*/
        if(!configColProperty.endsWith("__old")&&!Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbUpdateSrc.getDisplaytype(true)))
        {
            EditableReportColBean ercbeanUpdateSrc=(EditableReportColBean)cbUpdateSrc.getExtendConfigDataForReportType(reportTypeKey);
            ercbeanUpdateSrc.setEditableWhenUpdate(2);
        }
       super.setParamBeanInfoOfColBean(cbUpdateSrc,paramBean,configColProperty,reportTypeKey);
    }
}
