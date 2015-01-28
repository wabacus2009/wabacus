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
package com.wabacus.system.component.application.report;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.AddButton;
import com.wabacus.system.buttons.CancelButton;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.ResetButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;

public class EditableFormReportType extends EditableDetailReportType
{
    public final static String KEY=EditableFormReportType.class.getName();

    private final static List<String> LST_ALL_ACCESSMODE=new ArrayList<String>();
    static
    {
        LST_ALL_ACCESSMODE.add(Consts.ADD_MODE);
        LST_ALL_ACCESSMODE.add(Consts.READ_MODE);
        LST_ALL_ACCESSMODE.add(Consts.UPDATE_MODE);
        LST_ALL_ACCESSMODE.add(Consts.READONLY_MODE);
    }
    
    public EditableFormReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }
    
    public String getDefaultAccessMode()
    {
        return Consts.UPDATE_MODE;
    }
    public List<String> getLstAllAccessModes()
    {
        return LST_ALL_ACCESSMODE;
    }

    protected boolean getDefaultShowContextMenu()
    {
        return false;
    }
    
    protected void initRealAccessMode()
    {
        if(EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            realAccessMode=Consts.READONLY_MODE;
        }else
        {
            String accessmode=rrequest.getStringAttribute(rbean.getId(),"CURRENT_ACCESSMODE",getDefaultAccessMode());
            if(accessmode.equals(Consts.ADD_MODE))
            {
                if(ersqlbean.getInsertbean()==null)
                {
                    throw new WabacusRuntimeException("报表"+rbean.getPath()+"没有配置<insert/>，不能进行添加操作");
                }
                realAccessMode=Consts.ADD_MODE;
                rrequest.getWResponse().addOnloadMethod("addEditableDetailReportFoSaving","{reportguid:\""+this.rbean.getGuid()+"\"}",true);
            }else if(accessmode.equals(Consts.READ_MODE)||ersqlbean.getUpdatebean()==null)
            {
                realAccessMode=Consts.READ_MODE;
            }else
            {
                realAccessMode=Consts.UPDATE_MODE;
            }
        }
    }
    public boolean needCertainTypeButton(AbsButtonType buttonType)
    {
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return false;
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(EditableDetailReportType.KEY);
        if(buttonType instanceof AddButton)
        {
            if(ersqlbean.getInsertbean()==null) return false;
            if(this.realAccessMode.equals(Consts.UPDATE_MODE)) return true;
        }else if(buttonType instanceof DeleteButton)
        {
            if(this.lstReportData!=null&&this.lstReportData.size()>0&&(this.realAccessMode.equals(Consts.UPDATE_MODE)||this.realAccessMode.equals(Consts.READ_MODE)))
            {
                return true;
            }
        }else if(buttonType instanceof SaveButton)
        {
            if(ersqlbean.getInsertbean()!=null&&this.realAccessMode.equals(Consts.ADD_MODE))
            {
                return true;
            }
            if(ersqlbean.getUpdatebean()!=null&&this.realAccessMode.equals(Consts.UPDATE_MODE))
            {
                if(this.lstReportData!=null&&this.lstReportData.size()>0) return true;
            }
        }else if(buttonType instanceof ResetButton)
        {
            if(ersqlbean.getUpdatebean()!=null&&this.realAccessMode.equals(Consts.UPDATE_MODE))
            {
                if(this.lstReportData!=null&&this.lstReportData.size()>0) return true;
            }
            if(ersqlbean.getInsertbean()!=null&&this.realAccessMode.equals(Consts.ADD_MODE))
            {
                return true;
            }
        }else if(buttonType instanceof CancelButton)
        {
            if(ersqlbean.getInsertbean()!=null&&this.realAccessMode.equals(Consts.ADD_MODE)&&rbean.getSbean().getLstDatasetBeans()!=null
                    &&rbean.getSbean().getLstDatasetBeans().size()>0)
            {
                return true;
            }
        }
        return false;
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_FORM;
    }
}
