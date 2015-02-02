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
package com.wabacus.system.component.application.report.configbean.crosslist;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.util.Consts;

public class CrossListReportStatiBean extends AbsExtendConfigBean
{
    public final static String STATICS_FOR_WHOLEREPORT="report";

    private String id;//标识此统计信息，其值由<col/>的column+“.”+<statistic/>的id属性配置值

    private String type;

    private String column;

    private List<String> lstLabels;

    private List<String> lstLabelstyleproperties;//显示统计的列的标题样式

    private List<String> lstValuestyleproperties;

    private List<String> lstStatitems;
    
    private IDataType datatypeObj;
    
    public CrossListReportStatiBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id=id;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type=type;
    }

    public List<String> getLstStatitems()
    {
        return lstStatitems;
    }

    public void setLstStatitems(List<String> lstStatitems)
    {
        this.lstStatitems=lstStatitems;
    }

    public String getColumn()
    {
        return column;
    }

    public void setColumn(String column)
    {
        this.column=column;
    }

    public List<String> getLstLabels()
    {
        return lstLabels;
    }

    public void setLstLabels(List<String> lstLabels)
    {
        this.lstLabels=lstLabels;
    }

    public List<String> getLstLabelstyleproperties()
    {
        return lstLabelstyleproperties;
    }

    public void setLstLabelstyleproperties(List<String> lstLabelstyleproperties)
    {
        this.lstLabelstyleproperties=lstLabelstyleproperties;
    }

    public List<String> getLstValuestyleproperties()
    {
        return lstValuestyleproperties;
    }

    public void setLstValuestyleproperties(List<String> lstValuestyleproperties)
    {
        this.lstValuestyleproperties=lstValuestyleproperties;
    }

    public IDataType getDatatypeObj()
    {
        return datatypeObj;
    }

    public void setDatatypeObj(IDataType datatypeObj)
    {
        this.datatypeObj=datatypeObj;
    }

    public String getLabel(String column)
    {
        if(this.lstLabels==null||this.lstLabels.size()==0) return "";
        if(column==null||column.trim().equals(""))
        {
            return this.lstLabels.get(0);
        }
        int idx=getIndex(column);
        if(idx<0) return "";
        if(idx>this.lstLabels.size()-1) return this.lstLabels.get(this.lstLabels.size()-1);
        return this.lstLabels.get(idx);
    }

    public String getLabelstyleproperty(String column)
    {
        if(this.lstLabelstyleproperties==null||this.lstLabelstyleproperties.size()==0) return "";
        if(column==null||column.trim().equals(""))
        {
            return this.lstLabelstyleproperties.get(0);
        }
        int idx=getIndex(column);
        if(idx<0) return "";
        if(idx>this.lstLabelstyleproperties.size()-1) return this.lstLabelstyleproperties.get(this.lstLabelstyleproperties.size()-1);
        return this.lstLabelstyleproperties.get(idx);
    }

    public String getValuestyleproperty(String column)
    {
        if(this.lstValuestyleproperties==null||this.lstValuestyleproperties.size()==0) return "";
        if(column==null||column.trim().equals(""))
        {
            return this.lstValuestyleproperties.get(0);
        }
        int idx=getIndex(column);
        if(idx<0) return "";
        if(idx>this.lstValuestyleproperties.size()-1) return this.lstValuestyleproperties.get(this.lstValuestyleproperties.size()-1);
        return this.lstValuestyleproperties.get(idx);
    }

    private int getIndex(String column)
    {
        int i=0;
        for(String itemTmp:this.lstStatitems)
        {
            if(itemTmp.equals(column)) return i;
            i++;
        }
        return -1;
    }

    public void validateConfig()
    {
        ReportBean rbean=this.getOwner().getReportBean();
        if(type==null||type.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有为<statistic/>配置type属性指定统计类型");
        }
        if(!Consts.lstStatisticsType.contains(type))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有为<statistic/>配置type属性"+type+"是不支持的统计类型");
        }
        if(column==null||column.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有为<statistic/>配置column属性指定统计字段");
        }
    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        CrossListReportStatiBean beanNew=(CrossListReportStatiBean)super.clone(owner);
        if(this.lstLabels!=null) beanNew.setLstLabels((List<String>)((ArrayList<String>)lstLabels).clone());
        if(this.lstLabelstyleproperties!=null)
            beanNew.setLstLabelstyleproperties((List<String>)((ArrayList<String>)lstLabelstyleproperties).clone());
        if(this.lstStatitems!=null) beanNew.setLstStatitems((List<String>)((ArrayList<String>)lstStatitems).clone());
        if(this.lstValuestyleproperties!=null)
            beanNew.setLstValuestyleproperties((List<String>)((ArrayList<String>)lstValuestyleproperties).clone());
        return beanNew;
    }
}
