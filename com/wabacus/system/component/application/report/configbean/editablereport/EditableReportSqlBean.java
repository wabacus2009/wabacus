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

import java.util.List;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;

public final class EditableReportSqlBean extends AbsExtendConfigBean implements IEditableReportEditGroupOwnerBean
{
    private EditableReportUpdateDataBean updatebean;

    private EditableReportInsertDataBean insertbean;

    private EditableReportDeleteDataBean deletebean;

    private String beforeSaveAction;
    
    private String[] afterSaveAction;

    private List<String> lstSaveBindingReportIds;
    
    private List<ReportBean> lstSaveBindingReportBeans;
    
    private List<String> lstDeleteBindingReportIds;
    
    private List<ReportBean> lstDeleteBindingReportBeans;
    
    public EditableReportSqlBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public EditableReportUpdateDataBean getUpdatebean()
    {
        return updatebean;
    }

    public void setUpdatebean(EditableReportUpdateDataBean updatebean)
    {
        this.updatebean=updatebean;
    }

    public EditableReportInsertDataBean getInsertbean()
    {
        return insertbean;
    }

    public void setInsertbean(EditableReportInsertDataBean insertbean)
    {
        this.insertbean=insertbean;
    }

    public EditableReportDeleteDataBean getDeletebean()
    {
        return deletebean;
    }

    public void setDeletebean(EditableReportDeleteDataBean deletebean)
    {
        this.deletebean=deletebean;
    }

    public List<String> getLstSaveBindingReportIds()
    {
        return lstSaveBindingReportIds;
    }

    public void setLstSaveBindingReportIds(List<String> lstSaveBindingReportIds)
    {
        this.lstSaveBindingReportIds=lstSaveBindingReportIds;
    }

    public List<ReportBean> getLstSaveBindingReportBeans()
    {
        return lstSaveBindingReportBeans;
    }

    public void setLstSaveBindingReportBeans(List<ReportBean> lstSaveBindingReportBeans)
    {
        if(lstSaveBindingReportBeans!=null&&lstSaveBindingReportBeans.size()==0) lstSaveBindingReportBeans=null;
        this.lstSaveBindingReportBeans=lstSaveBindingReportBeans;
    }

    public List<String> getLstDeleteBindingReportIds()
    {
        return lstDeleteBindingReportIds;
    }

    public void setLstDeleteBindingReportIds(List<String> lstDeleteBindingReportIds)
    {
        this.lstDeleteBindingReportIds=lstDeleteBindingReportIds;
    }

    public List<ReportBean> getLstDeleteBindingReportBeans()
    {
        return lstDeleteBindingReportBeans;
    }

    public void setLstDeleteBindingReportBeans(List<ReportBean> lstDeleteBindingReportBeans)
    {
        if(lstDeleteBindingReportBeans!=null&&lstDeleteBindingReportBeans.size()==0) lstDeleteBindingReportBeans=null;
        this.lstDeleteBindingReportBeans=lstDeleteBindingReportBeans;
    }

    public String getBeforeSaveAction()
    {
        return beforeSaveAction;
    }

    public void setBeforeSaveAction(String beforeSaveAction)
    {
        this.beforeSaveAction=beforeSaveAction;
    }

    public String[] getAfterSaveAction()
    {
        return afterSaveAction;
    }
    
    public String getAfterSaveActionMethod()
    {
        if(afterSaveAction==null||afterSaveAction.length==0) return "";
        return afterSaveAction[0];
    }
    
    public void setAfterSaveAction(String[] afterSaveAction)
    {
        this.afterSaveAction=afterSaveAction;
    }

    public ReportBean getReportBean()
    {
        return this.getOwner().getReportBean();
    }
    
    public String getBeforeSaveActionString(String actiontype)
    {
        if(this.beforeSaveAction==null||this.beforeSaveAction.trim().equals("")) return "";
        if(actiontype==null) actiontype="";
        StringBuffer buf=new StringBuffer();
        buf.append("if(!").append(this.beforeSaveAction).append("('").append(
                this.getOwner().getReportBean().getPageBean().getId()).append("','")
                .append(this.getOwner().getReportBean().getId()).append("','").append(actiontype)
                .append("'))");
        buf.append("return;");
        return buf.toString();
    }
    
    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        EditableReportSqlBean newsqlbean=(EditableReportSqlBean)super.clone(owner);
        if(this.insertbean!=null)
        {
            newsqlbean.setInsertbean((EditableReportInsertDataBean)insertbean.clone(newsqlbean));
        }
        if(this.updatebean!=null)
        {
            newsqlbean.setUpdatebean((EditableReportUpdateDataBean)updatebean.clone(newsqlbean));
        }
        if(this.deletebean!=null)
        {
            newsqlbean.setDeletebean((EditableReportDeleteDataBean)deletebean.clone(newsqlbean));
        }
        return newsqlbean;
    }

    public int hashCode()
    {
        final int prime=31;
        int result=1;
        result=prime*result+((this.getOwner()==null)?0:this.getOwner().getReportBean().hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if(this==obj) return true;
        if(obj==null) return false;
        if(getClass()!=obj.getClass()) return false;
        final EditableReportSqlBean other=(EditableReportSqlBean)obj;
        if(this.getOwner()==null)
        {
            if(other.getOwner()!=null) return false;
        }else if(!this.getOwner().getReportBean().equals(other.getOwner().getReportBean()))
        {
            return false;
        }
        return true;
    }
}
