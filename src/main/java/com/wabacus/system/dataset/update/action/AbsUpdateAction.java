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
package com.wabacus.system.dataset.update.action;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public abstract class AbsUpdateAction
{
    protected List<EditableReportParamBean> lstParamBeans;
    
    protected AbsEditableReportEditDataBean ownerUpdateBean;//本<value/>所属的<insert/>、<update/>、<delete/>、<button/>对象
    
    protected String objectId;
    
    public AbsUpdateAction(AbsEditableReportEditDataBean ownerUpdateBean)
    {
        this.ownerUpdateBean=ownerUpdateBean;
        objectId=Tools.generateObjectId(this.getClass());
    }

    public AbsEditableReportEditDataBean getOwnerUpdateBean()
    {
        return ownerUpdateBean;
    }
    
    public void setLstParamBeans(List<EditableReportParamBean> lstParamBeans)
    {
        this.lstParamBeans=lstParamBeans;
    }
    
    public List<EditableReportParamBean> getLstParamBeans()
    {
        return lstParamBeans;
    }
    
    public void beginTransaction(ReportRequest rrequest)
    {}

    public void commitTransaction(ReportRequest rrequest)
    {}

    public void rollbackTransaction(ReportRequest rrequest)
    {}
    
    public String getExecuteSql(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues)
    {
        return null;
    }
    
    public void setExecuteSql(ReportRequest rrequest,String sql)
    {}
    
    public abstract void updateData(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues) throws SQLException;
    
    public EditableReportParamBean createParamBeanByColbean(String configColProperty,String reportTypeKey,boolean enableHiddenCol,boolean isMust)
    {
        if(configColProperty==null||configColProperty.trim().equals("")) return null;
        String realColProperty=configColProperty.trim();
        if(realColProperty.endsWith("__old")) realColProperty=realColProperty.substring(0,realColProperty.length()-"__old".length());
        ReportBean rbean=this.ownerUpdateBean.getOwner().getReportBean();
        ColBean cbean=rbean.getDbean().getColBeanByColProperty(realColProperty);
        if(cbean==null) throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，column/property为"+realColProperty+"的列不存在，无法为其创建更新参数对象");
        if(cbean.isNonValueCol()||cbean.isSequenceCol()||cbean.isControlCol())
        {
            if(!isMust) return null;
            throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()+"失败，列"+cbean.getColumn()+"不是从数据库获取数据的列，不能做为更新列");
        }
        if(!isMust&&cbean.isNonFromDbCol()) return null;
        ColBean cbUpdateSrc=cbean;
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true)))
        {
            ColBean cbSrc=cbean.getUpdateColBeanSrc(false);
            if(cbSrc==null)
            {//没有被别的<col/>通过updatecol属性引用到
                if(!enableHiddenCol)
                {
                    if(!isMust) return null;
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()+"失败，列"+cbean.getColumn()+"为隐藏列，在这种情况不允许做为更新数据的字段");
                }
            }else
            {//被其它列通过updatecol引用进行更新
                cbUpdateSrc=cbSrc;
            }
        }
        EditableReportParamBean paramBean=new EditableReportParamBean();
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(reportTypeKey);
        if(ercbean!=null) paramBean.setDefaultvalue(ercbean.getDefaultvalue());
        paramBean.setOwner(cbean);
        this.ownerUpdateBean.setParamBeanInfoOfColBean(cbUpdateSrc,paramBean,configColProperty,reportTypeKey);
        return paramBean;
    }
}

