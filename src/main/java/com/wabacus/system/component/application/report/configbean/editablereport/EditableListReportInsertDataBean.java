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
import java.util.Map;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.EditableListReportType;
import com.wabacus.util.Tools;

public class EditableListReportInsertDataBean extends EditableReportInsertDataBean
{
    private EditableListReportInsertUpdateBean realInsertBean;

    public EditableListReportInsertDataBean(IEditableReportEditGroupOwnerBean owner)
    {
        super(owner);
    }

    public EditableListReportInsertUpdateBean getRealInsertBean()
    {
        return realInsertBean;
    }

    public void setRealInsertBean(EditableListReportInsertUpdateBean realInsertBean)
    {
        this.realInsertBean=realInsertBean;
    }

    public int parseActionscripts(String reportTypeKey)
    {
        return 1;
    }

    public String parseUpdateWhereClause(ReportBean rbean,String reportKey,List<EditableReportParamBean> lstDynParams,String whereclause)
    {
        return "";
    }

    public String getPopupPageUrlAndParams(ReportRequest rrequest,EditableListReportType reportTypeObj,AbsReportDataPojo dataObj)
    {
        return this.realInsertBean.getPopupPageUrlAndParams(rrequest,reportTypeObj,dataObj);
    }

    public void doPostLoadFinally()
    {
        if(this.realInsertBean.getLstUrlParams()!=null)
        {
            String paramValueTmp;
            for(Map<String,String> mParamTmp:this.realInsertBean.getLstUrlParams())
            {
                if(mParamTmp==null||mParamTmp.size()==0) continue;
                paramValueTmp=mParamTmp.entrySet().iterator().next().getValue();
                if(Tools.isDefineKey("url",paramValueTmp))
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()
                            +"失败，editablelist报表类型的<insert/>中urlparams参数不能配置为url{name}，可以配置为request/session{key}格式");
                }
                if(Tools.isDefineKey("@",paramValueTmp))
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()
                            +"失败，editablelist报表类型的<insert/>中urlparams参数不能配置为@{name}，即不能从某列获取参数值，可以配置为request/session{key}格式");
                }
            }
        }
        this.realInsertBean.doPostLoadFinally();
    }
    
    public Object clone(IEditableReportEditGroupOwnerBean newowner)
    {
        EditableListReportInsertDataBean newbean=(EditableListReportInsertDataBean)super.clone(newowner);
        if(this.realInsertBean!=null)
        {
            newbean.setRealInsertBean((EditableListReportInsertUpdateBean)realInsertBean.clone(newbean));
        }
        return newbean;
    }
}
