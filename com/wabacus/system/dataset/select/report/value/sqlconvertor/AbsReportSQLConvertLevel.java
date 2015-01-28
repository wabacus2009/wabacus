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
package com.wabacus.system.dataset.select.report.value.sqlconvertor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.crosslist.AbsCrossListReportColAndGroupBean;
import com.wabacus.system.dataset.select.rationaldbassistant.report.AbsGetReportDataSetBySQL;
import com.wabacus.system.dataset.select.rationaldbassistant.report.GetReportAllDataSetBySQL;
import com.wabacus.system.dataset.select.rationaldbassistant.report.GetReportPartDataSetBySQL;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Tools;

public abstract class AbsReportSQLConvertLevel implements Cloneable
{
    protected final static String GROUPBY_DYNAMICOLUMNS_PLACEHOLDER="#GROUPBY_DYNAMICOLUMNS_PLACEHOLDER#";

    public final static String filterColumnPlaceholder="{%FILTERCOLUMN%}";

    protected SQLReportDataSetValueProvider ownerProvider;

    protected String originalSql;
    
    protected String orderby;

    protected String dynamicColsPlaceholder;//如果本SQL是查询交叉报表的数据集，这里存放SQL语句中查询动态字段的占位符

    protected boolean isListReportType;
    
    //protected String selectcolumns;//对于partconvert/nonconvert转换类型中配置的SQL语句，在<value/>中配置的查询字段列表
    
    public AbsReportSQLConvertLevel(SQLReportDataSetValueProvider ownerProvider)
    {
        this.ownerProvider=ownerProvider;
    }

    public ReportBean getReportBean()
    {
        return this.ownerProvider.getReportBean();
    }

    public String getDatasource()
    {
        return this.ownerProvider.getOwnerDataSetValueBean().getDatasource();
    }

    public abstract String getRecordcountSql(ReportRequest rrequest,AbsGetReportDataSetBySQL sqlDataSetObj);
    
    public abstract String getFilterdata_sql(ReportRequest rrequest,AbsGetReportDataSetBySQL sqlDataSetObj);
    
    public abstract String getReportDataSetSql(ReportRequest rrequest,GetReportAllDataSetBySQL sqlDataSetObj);
    
    public abstract String getReportDataSetSql(ReportRequest rrequest,GetReportPartDataSetBySQL sqlDataSetObj);
    
    public abstract String getStatisticDataSetSql(ReportRequest rrequest,GetReportAllDataSetBySQL sqlDataSetObj);
    
    public abstract String getStatisticDataSetSql(ReportRequest rrequest,GetReportPartDataSetBySQL sqlDataSetObj);
    
    public String getOriginalSql()
    {
        return originalSql;
    }

    public String getOrderby()
    {
        return orderby;
    }

    public String getDynamicColsPlaceholder()
    {
        return dynamicColsPlaceholder;
    }

    public boolean isListReportType()
    {
        return isListReportType;
    }

    public abstract String getGetVerticalStatisDataSql();

    public abstract String getCrossListDynamicSql(ReportRequest rrequest,String commonDynCols,String crossStatiDynCols);


    public String mixDynorderbyAndRowgroupCols(String dynorderby)
    {
        List<String> lstTemp=Tools.parseStringToList(dynorderby," ",false);
        Map<String,String> mOldDynOrderBy=new HashMap<String,String>();
        if(lstTemp.size()!=2)
        {
            throw new WabacusRuntimeException("查询报表"+this.getReportBean().getPath()+"数据失败，传入的动态排序子句"+dynorderby+"不合法");
        }else
        {
            mOldDynOrderBy.put(lstTemp.get(0).trim(),lstTemp.get(1).trim());
        }
        StringBuffer orderbybuf=new StringBuffer();
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)this.getReportBean().getDbean().getExtendConfigDataForReportType(
                AbsListReportType.KEY);
        if(alrdbean!=null&&alrdbean.getRowGroupColsNum()>0)
        {
            List<Map<String,String>> lstRowgroupColsAndOrders=alrdbean.getLstRowgroupColsAndOrders();
            if(lstRowgroupColsAndOrders!=null&&lstRowgroupColsAndOrders.size()>0)
            {
                String ordercol;
                for(Map<String,String> mOrderCols:lstRowgroupColsAndOrders)
                {
                    if(mOrderCols==null||mOrderCols.size()==0) continue;
                    ordercol=mOrderCols.keySet().iterator().next();
                    if(mOldDynOrderBy!=null&&mOldDynOrderBy.containsKey(ordercol))
                    {
                        orderbybuf.append(ordercol).append(" ").append(mOldDynOrderBy.get(ordercol)).append(",");
                        mOldDynOrderBy=null;
                    }else
                    {
                        orderbybuf.append(ordercol).append(" ").append(mOrderCols.get(ordercol)).append(",");
                    }
                }
            }
        }
        if(mOldDynOrderBy!=null)
        {
            orderbybuf.append(dynorderby);
        }
        if(orderbybuf.charAt(orderbybuf.length()-1)==',')
        {
            orderbybuf.deleteCharAt(orderbybuf.length()-1);
        }
        return orderbybuf.toString();
    }

    public String mixDynorderbyAndConfigOrderbyCols(String dynorderby)
    {
        if(Tools.isEmpty(this.orderby)) return dynorderby;
        if(Tools.isEmpty(dynorderby)) return this.orderby;
        StringBuffer orderbybuf=new StringBuffer();
        orderbybuf.append(dynorderby);
        dynorderby=dynorderby.trim();
        int idx=dynorderby.indexOf(" ");
        if(idx<0) throw new WabacusRuntimeException("查询报表"+this.getReportBean().getPath()+"数据失败，传入的动态排序子句"+dynorderby+"不合法");
        String dynorderbyColumn=dynorderby.substring(0,idx).trim();//动态排序的字段名
        if(this.orderby.toLowerCase().indexOf(dynorderbyColumn.toLowerCase())<0)
        {
            orderbybuf.append(",").append(this.orderby);
        }else
        {
            List<String> lstTmp=Tools.parseStringToList(this.orderby," ",false);
            String columnTmp;
            for(String orderbyTmp:lstTmp)
            {
                if(Tools.isEmpty(orderbyTmp)) continue;
                columnTmp=orderbyTmp.trim();
                idx=columnTmp.indexOf(" ");
                if(idx>0) columnTmp=columnTmp.substring(0,idx);
                if(columnTmp.trim().toLowerCase().equals(dynorderbyColumn.trim().toLowerCase())) continue;
                orderbybuf.append(",").append(orderbyTmp);
            }
        }
        return orderbybuf.toString();
    }
    
    public String[] getOrderByAndInverseArray(String orderby)
    {
        if(Tools.isEmpty(orderby)) return null;
        List<String> lstOrderByColumns=Tools.parseStringToList(orderby,",",false);
        StringBuffer sbufferOrder=new StringBuffer();
        StringBuffer sbufferOrder_reverse=new StringBuffer();
        for(String orderbyTmp:lstOrderByColumns)
        {
            if(orderbyTmp==null||orderbyTmp.trim().equals("")) continue;
            orderbyTmp=orderbyTmp.trim();
            List<String> lstTemp=Tools.parseStringToList(orderbyTmp," ",false);
            if(sbufferOrder.length()>0&&sbufferOrder_reverse.length()>0)
            {
                sbufferOrder.append(",");
                sbufferOrder_reverse.append(",");
            }
            if(lstTemp.size()==1)
            {
                sbufferOrder.append(lstTemp.get(0)).append(" asc");
                sbufferOrder_reverse.append(lstTemp.get(0)).append(" desc");
            }else if(lstTemp.size()==2)
            {
                String ordertype=lstTemp.get(1).trim().toLowerCase();
                if(ordertype.equals("desc"))
                {
                    sbufferOrder.append(lstTemp.get(0)).append(" desc");
                    sbufferOrder_reverse.append(lstTemp.get(0)).append(" asc");
                }else
                {
                    sbufferOrder.append(lstTemp.get(0)).append(" asc");
                    sbufferOrder_reverse.append(lstTemp.get(0)).append(" desc");
                }
            }else
            {
                throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"配置的SQL语句中order by子句"+orderby+"不合法");
            }
        }
        return new String[] { "order by "+sbufferOrder.toString(), "order by "+sbufferOrder_reverse.toString() };
    }
    
    public void loadConfig(XmlElementBean eleValueBean)
    {}

    public void parseSql(String originalsql)
    {
        this.originalSql=originalsql;
        this.isListReportType=Config.getInstance().getReportType(getReportBean().getType()) instanceof AbsListReportType;
    }

    public void doPostLoadCrossList(List<AbsCrossListReportColAndGroupBean> lstIncludeCommonCrossColAndGroupBeans,
            List<AbsCrossListReportColAndGroupBean> lstIncludeCrossStatiColAndGroupBeans)
    {
        String tmpstr=this.originalSql;
        if(tmpstr.indexOf("[#dynamic-columns#]")>0)
        {
            this.dynamicColsPlaceholder="[#dynamic-columns#]";
        }else if(tmpstr.indexOf("(#dynamic-columns#)")>0)
        {
            this.dynamicColsPlaceholder="(#dynamic-columns#)";
        }else
        {
            throw new WabacusConfigLoadingException("加载报表"+getReportBean().getPath()
                    +"失败，查询动态列的数据集的SQL语句中没有指定[#dynamic-columns#]或(#dynamic-columns#)做为动态字段占位符");
        }
    }

    protected String replaceDynColPlaceHolder(String sql,String realSelectCols,String dyncols_placeholder)
    {
        if(realSelectCols==null||realSelectCols.trim().equals(""))
        {//删除掉SQL语句中的%dyncols%占位符
            int idx=sql.indexOf(dyncols_placeholder);
            while(idx>0)
            {
                String sql1=sql.substring(0,idx).trim();
                String sql2=sql.substring(idx+dyncols_placeholder.length());
                while(sql1.endsWith(","))
                    sql1=sql1.substring(0,sql1.length()-1).trim();
                sql=sql1+" "+sql2;
                idx=sql.indexOf(dyncols_placeholder);
            }
        }else
        {
            sql=Tools.replaceAll(sql,dyncols_placeholder,realSelectCols);
        }
        return sql;
    }

    public AbsReportSQLConvertLevel clone(SQLReportDataSetValueProvider newOwnerProvider)
    {
        try
        {
            AbsReportSQLConvertLevel newObj=(AbsReportSQLConvertLevel)super.clone();
            newObj.ownerProvider=newOwnerProvider;
            return newObj;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }

    }
}
