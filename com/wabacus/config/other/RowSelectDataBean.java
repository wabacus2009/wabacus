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
package com.wabacus.config.other;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.util.Tools;

public class RowSelectDataBean implements Cloneable
{
    private ReportBean rbean;

    private List<ColBean> lstColBeans;//要用到的所有列<col/>对象

    private Map<String,String> mColAndExpressions;

    public void setReportBean(ReportBean rbean)
    {
        this.rbean=rbean;
    }

    public ReportBean getReportBean()
    {
        return rbean;
    }

    public List<ColBean> getLstColBeans()
    {
        return lstColBeans;
    }

    public Map<String,String> getMColAndExpressions()
    {
        return mColAndExpressions;
    }

    public String getColExpression(String property)
    {
        if(mColAndExpressions==null) return null;
        return mColAndExpressions.get(property);
    }

    public void setConfigColsExpression(String configSelectedColProperties)
    {
        this.lstColBeans=new ArrayList<ColBean>();
        this.mColAndExpressions=new HashMap<String,String>();
        List<String> lstCols=Tools.parseStringToList(configSelectedColProperties,";",false);
        String str1, propertyTmp, str2;
        ColBean cbTmp;
        for(String proptmp:lstCols)
        {
            int idx=proptmp.indexOf("#");
            if(idx<0)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，其配置的获取选中记录行数据的列格式不正确，需要配置包含#property#指定引用数据的列");
            }
            str1=proptmp.substring(0,idx);
            proptmp=proptmp.substring(idx+1);
            idx=proptmp.indexOf("#");
            if(idx<=0)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，其配置的获取选中记录行数据的列格式不正确，需要配置包含#property#指定引用数据的列");
            }
            propertyTmp=proptmp.substring(0,idx).trim();
            str2=proptmp.substring(idx+1);
            cbTmp=rbean.getDbean().getColBeanByColProperty(propertyTmp);
            if(cbTmp==null||cbTmp.isControlCol()||cbTmp.isSequenceCol()||cbTmp.isNonFromDbCol()||cbTmp.isNonValueCol())
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，其配置的获取选中记录行数据的列"+propertyTmp+"对应的列不存在");
            }
            AbsListReportColBean alrcbean=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrcbean==null)
            {
                alrcbean=new AbsListReportColBean(cbTmp);
                cbTmp.setExtendConfigDataForReportType(AbsListReportType.KEY,alrcbean);
            }
            cbTmp.setDisplayNameValueProperty(true);
            setHasRowselectValueConditions(cbTmp);
            this.lstColBeans.add(cbTmp);
            mColAndExpressions.put(cbTmp.getProperty(),str1+cbTmp.getColumn()+str2);
        }
    }

    private void setHasRowselectValueConditions(ColBean cbean)
    {
        List<String> lstDatasetValueIds=cbean.getLstDatasetValueids();
        if(lstDatasetValueIds==null||lstDatasetValueIds.size()==0)
        {//没有指定数据集，则说明会从所有数据集中取数据，因此要加到所有数据集<value/>中
            for(ReportDataSetBean dsbeanTmp:rbean.getSbean().getLstDatasetBeans())
            {
                for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
                {
                    dsvbeanTmp.addRowSelectValueColProperty(cbean.getProperty());
                }
            }
        }else
        {
            ReportDataSetValueBean dsvbeanTmp;
            for(String dsvidTmp:lstDatasetValueIds)
            {
                for(ReportDataSetBean dsbeanTmp:rbean.getSbean().getLstDatasetBeans())
                {
                    dsvbeanTmp=dsbeanTmp.getDatasetValueBeanById(dsvidTmp);
                    if(dsvbeanTmp!=null) dsvbeanTmp.addRowSelectValueColProperty(cbean.getProperty());
                }
            }
        }
    }

    public RowSelectDataBean clone()
    {
        try
        {
            return (RowSelectDataBean)super.clone();
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
