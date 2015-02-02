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

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.database.type.SQLSERVER2K;
import com.wabacus.config.database.type.SQLSERVER2K5;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.crosslist.AbsCrossListReportColAndGroupBean;
import com.wabacus.system.dataset.select.rationaldbassistant.report.AbsGetReportDataSetBySQL;
import com.wabacus.system.dataset.select.rationaldbassistant.report.GetReportAllDataSetBySQL;
import com.wabacus.system.dataset.select.rationaldbassistant.report.GetReportPartDataSetBySQL;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Tools;

public class NonConvertSQLevel extends AbsReportSQLConvertLevel
{
    public NonConvertSQLevel(SQLReportDataSetValueProvider ownerProvider)
    {
        super(ownerProvider);
    }

    public String getFilterdata_sql(ReportRequest rrequest,AbsGetReportDataSetBySQL sqlDataSetObj)
    {
        if(!this.isListReportType||ownerProvider.getOwnerDataSetValueBean().isDependentDataSet()) return null;
        if(this.originalSql.indexOf(SQLReportDataSetValueProvider.filterConditionPlaceHolder)<0)
        {
            throw new WabacusRuntimeException("获取报表"+this.getReportBean().getPath()+"列过滤选项数据失败，此报表数据集的sqlconvertlevle配置为none时，必须在SQL语句中指定"
                    +SQLReportDataSetValueProvider.filterConditionPlaceHolder+"占位符");
        }
        String convertsqlTmp=getAllDataSql(rrequest,true,null);
        if(Tools.isEmpty(convertsqlTmp)) return null;
        String filtersqlTmp="select distinct "+filterColumnPlaceholder+"  from ("+convertsqlTmp+") wx_tblfilter";
        filtersqlTmp+=" order by  "+filterColumnPlaceholder;
        return filtersqlTmp;
    }

    public String getRecordcountSql(ReportRequest rrequest,AbsGetReportDataSetBySQL sqlDataSetObj)
    {
        String convertsqlTmp=getAllDataSql(rrequest,true,null);
        if(Tools.isEmpty(convertsqlTmp)) return null;
        return "select count(*) from ("+convertsqlTmp+") wx_tblcount";
    }

    public String getReportDataSetSql(ReportRequest rrequest,GetReportAllDataSetBySQL sqlDataSetObj)
    {
        return getAllDataSql(rrequest,false,getRealOrderby(rrequest));
    }

    public String getReportDataSetSql(ReportRequest rrequest,GetReportPartDataSetBySQL sqlDataSetObj)
    {
        return getOnePageDataSql(rrequest,sqlDataSetObj);
    }

    public String getStatisticDataSetSql(ReportRequest rrequest,GetReportAllDataSetBySQL sqlDataSetObj)
    {
        return getAllDataSql(rrequest,true,null);
    }

    public String getStatisticDataSetSql(ReportRequest rrequest,GetReportPartDataSetBySQL sqlDataSetObj)
    {
        return getOnePageDataSql(rrequest,sqlDataSetObj);
    }

    private String getOnePageDataSql(ReportRequest rrequest,GetReportPartDataSetBySQL sqlDataSetObj)
    {
        String resultSql=this.ownerProvider.getDynamicSql(rrequest);
        if("[NONE]".equals(resultSql)) return null;
        if(Tools.isEmpty(resultSql)) resultSql=this.originalSql;
        return replaceOrderbyPlaceholder(resultSql,getRealOrderby(rrequest));
    }
    
    private String getAllDataSql(ReportRequest rrequest,boolean isAsInnerSql,String dynorderby)
    {
        String resultSql=this.ownerProvider.getDynamicSql(rrequest);
        if("[NONE]".equals(resultSql)) return null;
        if(Tools.isEmpty(resultSql)) resultSql=this.originalSql;
        resultSql=Tools.replaceAll(resultSql,SQLReportDataSetValueProvider.startRowNumPlaceHolder,"0");
        resultSql=Tools.replaceAll(resultSql,SQLReportDataSetValueProvider.endRowNumPlaceHolder,String.valueOf(Integer.MAX_VALUE));
        resultSql=Tools.replaceAll(resultSql,SQLReportDataSetValueProvider.pagesizePlaceHolder,String.valueOf(Integer.MAX_VALUE));
        if(dynorderby==null) dynorderby=this.orderby;
        resultSql=replaceOrderbyPlaceholder(resultSql,dynorderby);
        if(isAsInnerSql)
        {
            AbsDatabaseType dbtype=Config.getInstance().getDataSource(this.getDatasource()).getDbType();
            if(dbtype instanceof SQLSERVER2K||dbtype instanceof SQLSERVER2K5)
            {
                resultSql=Tools.replaceAll(resultSql,"  "," ");//将所有空格替换成只有一个空格
                int idx1=resultSql.lastIndexOf(")");
                int idx2=resultSql.lastIndexOf(" order by ");
                if(idx2>idx1)
                {
                    resultSql=resultSql.substring(0,idx2);
                }
            }
        }
        return resultSql;
    }

    private String getRealOrderby(ReportRequest rrequest)
    {
        String orderby=this.orderby;
        String[] orderbys=this.ownerProvider.getClickOrderByColumnAndOrder(rrequest);
        if(orderbys!=null&&orderbys.length==2)
        {
            orderby=this.mixDynorderbyAndConfigOrderbyCols(orderbys[0]+" "+orderbys[1]);
            if(this.originalSql.indexOf(SQLReportDataSetValueProvider.orderbyPlaceHolder)<0)
            {
                throw new WabacusRuntimeException("获取报表"+this.getReportBean().getPath()
                        +"列过滤选项数据失败，此报表数据集的sqlconvertlevle配置为none且支持点击列标题进行排序功能，必须在SQL语句中指定"+SQLReportDataSetValueProvider.orderbyPlaceHolder+"占位符");
            }
        }
        return orderby;
    }
    
    private String replaceOrderbyPlaceholder(String resultSql,String realOrderby)
    {
        if(resultSql.indexOf(SQLReportDataSetValueProvider.orderbyInversePlaceHolder)>0)
        {
            String[] orderbys=this.getOrderByAndInverseArray(realOrderby);
            if(orderbys==null||orderbys.length!=2)
            {
                resultSql=Tools.replaceAll(resultSql,SQLReportDataSetValueProvider.orderbyPlaceHolder,"");
                resultSql=Tools.replaceAll(resultSql,SQLReportDataSetValueProvider.orderbyInversePlaceHolder,"");
            }else
            {
                resultSql=Tools.replaceAll(resultSql,SQLReportDataSetValueProvider.orderbyPlaceHolder,orderbys[0]);
                resultSql=Tools.replaceAll(resultSql,SQLReportDataSetValueProvider.orderbyInversePlaceHolder,orderbys[1]);
            }
        }else if(Tools.isEmpty(realOrderby))
        {
            resultSql=Tools.replaceAll(resultSql,SQLReportDataSetValueProvider.orderbyPlaceHolder,"");
        }else
        {
            resultSql=Tools.replaceAll(resultSql,SQLReportDataSetValueProvider.orderbyPlaceHolder," order by "+realOrderby);
        }
        return resultSql;
    }
    
    public String getCrossListDynamicSql(ReportRequest rrequest,String commonDynCols,String crossStatiDynCols)
    {
        if(!Tools.isEmpty(crossStatiDynCols))
        {
            throw new WabacusRuntimeException("报表"+this.getReportBean().getPath()+"是交叉统计报表，不能将数据集<value/>的sqlconvertlevle配置为none");
        }
        if(Tools.isEmpty(commonDynCols)&&this.dynamicColsPlaceholder.equals("(#dynamic-columns#)")) return "[NONE]";
        return replaceDynColPlaceHolder(this.originalSql,commonDynCols,this.dynamicColsPlaceholder);
    }
    
    public String getGetVerticalStatisDataSql()
    {
        return null;
    }
    
    public void loadConfig(XmlElementBean eleValueBean)
    {
        super.loadConfig(eleValueBean);
        orderby=eleValueBean.attributeValue("orderby");
    }

    public void parseSql(String originalsql)
    {
        super.parseSql(originalsql);
        if(this.ownerProvider.getOwnerDataSetValueBean().hasRowSelectValueConditions()
                &&originalSql.indexOf(SQLReportDataSetValueProvider.rowselectvaluesConditionPlaceHolder)<0)
        {
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()
                    +"的数据集<value/>的sqlconvertlevle配置为none，而且此数据集需要取选中行的记录，因此必须为其指定"+SQLReportDataSetValueProvider.rowselectvaluesConditionPlaceHolder
                    +"占位符");
        }
    }
    
    public void doPostLoadCrossList(List<AbsCrossListReportColAndGroupBean> lstIncludeCommonCrossColAndGroupBeans,
            List<AbsCrossListReportColAndGroupBean> lstIncludeCrossStatiColAndGroupBeans)
    {
        super.doPostLoadCrossList(lstIncludeCommonCrossColAndGroupBeans,lstIncludeCrossStatiColAndGroupBeans);
        if(lstIncludeCrossStatiColAndGroupBeans!=null&&lstIncludeCrossStatiColAndGroupBeans.size()>0)
        {
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"是交叉统计报表，不能将数据集<value/>的sqlconvertlevle配置为none");
        }
    }
}

