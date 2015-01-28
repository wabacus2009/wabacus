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

import java.util.HashMap;
import java.util.Map;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.util.Consts;

public class EditableReportInsertDataBean extends AbsEditableReportEditDataBean
{

    private Map<String,String> mUpdateConditions;

    private String insertstyle;
    
    private String addposition;//对于editablelist2/listform报表类型，指定添加记录的位置，可配置值为top和bottom，默认是bottom
    
    private String callbackmethod;//对于editablelist2/listform报表类型，指定添加记录时的回调函数
    
    public EditableReportInsertDataBean(IEditableReportEditGroupOwnerBean owner)
    {
        super(owner);
    }

    public Map<String,String> getMUpdateConditions()
    {
        return mUpdateConditions;
    }

    public void setMUpdateConditions(Map<String,String> updateConditions)
    {
        mUpdateConditions=updateConditions;
    }

    public String getInsertstyle()
    {
        return insertstyle;
    }

    public void setInsertstyle(String insertstyle)
    {
        this.insertstyle=insertstyle;
    }

    public String getAddposition()
    {
        return addposition;
    }

    public void setAddposition(String addposition)
    {
        this.addposition=addposition;
    }

    public String getCallbackmethod()
    {
        return callbackmethod;
    }

    public void setCallbackmethod(String callbackmethod)
    {
        this.callbackmethod=callbackmethod;
    }

    public void setParamBeanInfoOfColBean(ColBean cbUpdateSrc,EditableReportParamBean paramBean,String configColProperty,String reportTypeKey)
    {
        if(configColProperty.endsWith("__old"))
        {
            throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"的<insert/>标签失败，添加时各列没有旧数据，不能配置@{column__old}格式的编辑数据");
        }
        if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbUpdateSrc.getDisplaytype(true)))
        {
            EditableReportColBean ercbeanUpdated=(EditableReportColBean)cbUpdateSrc.getExtendConfigDataForReportType(reportTypeKey);
            ercbeanUpdated.setEditableWhenInsert(2);
        }
        super.setParamBeanInfoOfColBean(cbUpdateSrc,paramBean,configColProperty,reportTypeKey);
    }
    
    public Object clone(IEditableReportEditGroupOwnerBean newowner)
    {
        EditableReportInsertDataBean newbean=(EditableReportInsertDataBean)super.clone(newowner);
        if(mUpdateConditions!=null)
        {
            newbean.setMUpdateConditions((Map)((HashMap)mUpdateConditions).clone());
        }
        return newbean;
    }
}
