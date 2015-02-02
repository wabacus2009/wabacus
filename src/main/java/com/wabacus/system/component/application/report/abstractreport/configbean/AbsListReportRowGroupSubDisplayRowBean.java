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
package com.wabacus.system.component.application.report.abstractreport.configbean;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class AbsListReportRowGroupSubDisplayRowBean extends AbsListReportSubDisplayRowBean
{
    private String rowgroupcolumn;

    private String condition;

    public String getRowgroupcolumn()
    {
        return rowgroupcolumn;
    }

    public void setRowgroupcolumn(String rowgroupcolumn)
    {
        this.rowgroupcolumn=rowgroupcolumn;
    }

    public void setCondition(String condition)
    {
        this.condition=condition;
    }

    public String getCondition()
    {
        return condition;
    }

    public void validateRowGroupSubDisplayColsConfig(AbsListReportSubDisplayBean subdisplayBean,DisplayBean dbean)
    {
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)dbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        ReportBean rbean=dbean.getReportBean();
        if(this.lstSubColBeans.size()==1)
        {
            subdisplayBean.validateSubDisplayColsConfig(this.lstSubColBeans,alrdbean);
        }else
        {//不需提供列选择功能，则所有不是配置为永久隐藏的列都要显示出来，此时可以配置多个<scol/>
            int idxGroupCol=0;
            AbsListReportColBean alrcbean;
            if(alrdbean.getRowgrouptype()==1)
            {
                boolean isExist=false;
                for(ColBean cbean:dbean.getLstCols())
                {
                    idxGroupCol++;
                    if(rowgroupcolumn.equals(cbean.getColumn()))
                    {
                        alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                        if(!alrcbean.isRowgroup())
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，列"+rowgroupcolumn+"没有参与行分组，不能为它配置行分组统计");
                        }
                        isExist=true;
                        break;
                    }
                }
                if(!isExist)
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，分组列"+rowgroupcolumn+"不存在，没有column属性为此值的<col/>");
                }
            }else if(alrdbean.getRowgrouptype()==2)
            {
                idxGroupCol=1;
            }
            subdisplayBean.calColSpanAndStartColIdx(this.lstSubColBeans,alrdbean,alrdbean.getDefaultPageColPositionBean().getTotalColCount()-idxGroupCol);//这里只要校验每个统计列的colspan数是否合法，不用计算startcolidx，因为下载纯数据时，不会将分组统计的数据下载到Excel文件中
        }
    }

    public void validateConditionConfig(ReportBean rbean)
    {
        if(this.condition==null||this.condition.trim().equals("")) return;
        String[] colsArr=getParentAndMyOwnRowGroupColumnsArray(rbean);
        String tmp;
        for(int i=0;i<colsArr.length-1;i++)
        {
            for(int j=i+1;j<colsArr.length;j++)
            {
                if(colsArr[i].length()<colsArr[j].length())
                {
                    tmp=colsArr[i];
                    colsArr[i]=colsArr[j];
                    colsArr[j]=tmp;
                }
            }
        }
        List<String> lstConditions=Tools.parseStringToList(this.condition," ",false);
        for(int i=0;i<colsArr.length;i++)
        {
            boolean hasCon=false;
            String con;
            for(int j=lstConditions.size()-1;j>=0;j--)
            {
                con=lstConditions.get(j);
                if(con.indexOf(colsArr[i])>=0&&con.indexOf("#"+colsArr[i]+"#")>0)
                {
                    hasCon=true;
                    lstConditions.remove(j);
                }
            }
            if(!hasCon)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，分组"+this.rowgroupcolumn+"的统计配置没有为父分组"+colsArr[i]
                        +"列在condition属性中配置查询条件");
            }
        }
    }

    public String[] getParentAndMyOwnRowGroupColumnsArray(ReportBean rbean)
    {
       ColBean[] cbeansArr=getParentAndMyOwnRowGroupColBeans(rbean);
       String[] resultsArr=new String[cbeansArr.length];
       for(int i=0;i<cbeansArr.length;i++)
       {
           resultsArr[i]=cbeansArr[i].getColumn();
       }
       return resultsArr;
    }
    
    public ColBean[] getParentAndMyOwnRowGroupColBeans(ReportBean rbean)
    {
        List<ColBean> lstCbeans=new ArrayList<ColBean>();
        for(ColBean cbean:rbean.getDbean().getLstCols())
        {
            if(cbean.isControlCol()||cbean.isNonFromDbCol()||cbean.isNonValueCol()) continue;
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true))||Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(false))) continue;
            lstCbeans.add(cbean);
            if(rowgroupcolumn.equals(cbean.getColumn()))
            {//循环到了当前分组，则后面再有分组列也是当前分组列的子分组
                break;
            }
        }
        if(lstCbeans==null||lstCbeans.size()==0)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，没有column为"+rowgroupcolumn+"的分组列，无法将其配置分组统计");
        }
        return lstCbeans.toArray(new ColBean[lstCbeans.size()]);
    }
}
