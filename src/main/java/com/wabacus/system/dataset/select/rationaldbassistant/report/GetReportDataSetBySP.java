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
package com.wabacus.system.dataset.select.rationaldbassistant.report;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.dataset.select.rationaldbassistant.BatchStatisticItems;
import com.wabacus.system.dataset.select.rationaldbassistant.GetDataSetBySP;
import com.wabacus.system.dataset.select.rationaldbassistant.SPDataSetValueBean;
import com.wabacus.system.dataset.select.report.value.SPReportDataSetValueProvider;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public class GetReportDataSetBySP extends GetDataSetBySP
{
    private static Log log=LogFactory.getLog(GetReportDataSetBySP.class);

    private SPReportDataSetValueProvider provider;

    private int startRownum;

    private int endRownum;

    public GetReportDataSetBySP(ReportRequest rrequest,ReportBean rbean,SPReportDataSetValueProvider provider)
    {
        super(rrequest,rbean);
        this.provider=provider;
    }

    public void setStartRownum(int startRownum)
    {
        this.startRownum=startRownum;
    }

    public void setEndRownum(int endRownum)
    {
        this.endRownum=endRownum;
    }

    public Object getColFilterDataSet(ColBean filterColBean,boolean isGetSelectedOptions)
    {
        StringBuffer systemParamsBuf=new StringBuffer();
        systemParamsBuf.append("{[(<filter_column:"+filterColBean.getColumn()+">)]}");
        if(isGetSelectedOptions)
        {
            String filterwhere=this.provider.getFilterConditionExpression(rrequest);
            if(filterwhere!=null&&!filterwhere.trim().equals(""))
            {
                systemParamsBuf.append("{[(<filter_condition:"+filterwhere+">)]}");
            }
        }
        return doGetResultSet(this.provider.getSpbean(),rrequest.getAttribute(rbean.getId()+"_WABACUS_FILTERBEAN"),systemParamsBuf);
    }
    
    public Object getRecordcount()
    {
        StringBuffer systemParamsBuf=new StringBuffer();
        String filterwhere=this.provider.getFilterConditionExpression(rrequest);
        if(filterwhere!=null&&!filterwhere.trim().equals(""))
        {//有列过滤条件
            systemParamsBuf.append("{[(<filter_condition:"+filterwhere+">)]}");
        }
        String rowSelectCondition=this.provider.getRowSelectValueConditionExpression(rrequest);
        if(!Tools.isEmpty(rowSelectCondition))
        {
            systemParamsBuf.append("{[(<rowselectvalues_condition:"+rowSelectCondition.trim()+">)]}");
        }
        systemParamsBuf.append("{[(<get_recordcount:true>)]}");
        systemParamsBuf.append("{[(<dynamic_selectcols:"+this.provider.getDynamicSelectCols(rrequest)+">)]}");
        return doGetResultSet(this.provider.getSpbean(),this.provider.getOwnerDataSetValueBean(),systemParamsBuf);
    }

    public Object getReportDataSet(List<AbsReportDataPojo> lstReportData)
    {
        StringBuffer systemParamsBuf=new StringBuffer();
        if(rrequest.getDisplayReportTypeObj(rbean) instanceof AbsListReportType&&!rbean.getSbean().isHorizontalDataset())
        {
            addRowgroupColsToParams(systemParamsBuf);
            String[] orderbys=this.provider.getClickOrderByColumnAndOrder(rrequest);
            if(orderbys!=null&&orderbys.length==2)
            {
                systemParamsBuf.append("{[(<dynamic_orderby:"+orderbys[0]+" "+orderbys[1]+">)]}");
            }
            String filterwhere=this.provider.getFilterConditionExpression(rrequest);
            if(!Tools.isEmpty(filterwhere))
            {
                systemParamsBuf.append("{[(<filter_condition:"+filterwhere+">)]}");
            }
            systemParamsBuf.append("{[(<dynamic_selectcols:"+this.provider.getDynamicSelectCols(rrequest)+">)]}");
        }
        String rowSelectCondition=this.provider.getRowSelectValueConditionExpression(rrequest);
        if(!Tools.isEmpty(rowSelectCondition))
        {
            systemParamsBuf.append("{[(<rowselectvalues_condition:"+rowSelectCondition.trim()+">)]}");
        }
        if(this.startRownum>=0&&this.endRownum>0)
        {
            if(this.startRownum>=this.endRownum) return null;
            systemParamsBuf.append("{[(<pagesize:"+(this.endRownum-this.startRownum)+">)]}");
            systemParamsBuf.append("{[(<startrownum:"+this.startRownum+">)]}");//本次要查询的起始记录号
            systemParamsBuf.append("{[(<endrownum:"+this.endRownum+">)]}");
        }else
        {
            systemParamsBuf.append("{[(<pagesize:-1>)]}");
            if(this.provider.getOwnerDataSetValueBean().isDependentDataSet())
            {
                String realConExpress=this.provider.getOwnerDataSetValueBean().getRealDependsConditionExpression(lstReportData);
                systemParamsBuf.append("{[(<parentdataset_conditions:").append(realConExpress).append(">)]}");
            }
        }
        return doGetResultSet(this.provider.getSpbean(),this.provider.getOwnerDataSetValueBean(),systemParamsBuf);
    }

    public Object getStatisticDataSet(BatchStatisticItems batStatitems,String statisticsql)
    {
        if(statisticsql==null||statisticsql.trim().equals(""))
        {
            throw new WabacusRuntimeException("调用报表"+rbean.getPath()+"的存储过程失败，没有传入在存储过程中要执行的SQL语句");
        }
        StringBuffer systemParamsBuf=new StringBuffer();
        systemParamsBuf.append("{[(<statistic_sql:"+statisticsql+">)]}");
        String filterwhere=this.provider.getFilterConditionExpression(rrequest);
        if(!Tools.isEmpty(filterwhere))
        {
            systemParamsBuf.append("{[(<filter_condition:"+filterwhere+">)]}");
        }
        String rowSelectCondition=this.provider.getRowSelectValueConditionExpression(rrequest);
        if(!Tools.isEmpty(rowSelectCondition))
        {
            systemParamsBuf.append("{[(<rowselectvalues_condition:"+rowSelectCondition.trim()+">)]}");
        }
        if(this.startRownum>=0&&this.endRownum>0)
        {
            if(this.startRownum>=this.endRownum) return null;
            systemParamsBuf.append("{[(<pagesize:"+(this.endRownum-this.startRownum)+">)]}");
            systemParamsBuf.append("{[(<startrownum:"+this.startRownum+">)]}");//本次要查询的起始记录号
            systemParamsBuf.append("{[(<endrownum:"+this.endRownum+">)]}");
            addRowgroupColsToParams(systemParamsBuf);
            String[] orderbys=this.provider.getClickOrderByColumnAndOrder(rrequest);
            if(orderbys!=null&&orderbys.length==2)
            {
                systemParamsBuf.append("{[(<dynamic_orderby:"+orderbys[0]+" "+orderbys[1]+">)]}");
            }
        }else
        {
            systemParamsBuf.append("{[(<pagesize:-1>)]}");
            if(this.provider.getOwnerDataSetValueBean().isDependentDataSet())
            {//如果当前数据集是依赖其它数据集的子数据集（分页报表不用考虑子数据集的情况）
                CacheDataBean cdb=rrequest.getCdb(rbean.getId());
                List lstReportData=null;
                if(!cdb.isLoadAllReportData())
                {
                    lstReportData=(List)rrequest.getAttribute(rbean.getId()+"wx_all_data_tempory");
                    if(lstReportData==null)
                    {
                        lstReportData=ReportAssistant.getInstance().loadReportDataSet(rrequest,rrequest.getDisplayReportTypeObj(rbean),true);
                        rrequest.setAttribute(rbean.getId()+"wx_all_data_tempory",lstReportData);
                    }
                }else
                {
                    lstReportData=rrequest.getDisplayReportTypeObj(rbean).getLstReportData();
                }
                String realConExpress=this.provider.getOwnerDataSetValueBean().getRealDependsConditionExpression(lstReportData);
                systemParamsBuf.append("{[(<parentdataset_conditions:").append(realConExpress).append(">)]}");
            }
        }
        return doGetResultSet(this.provider.getSpbean(),this.provider.getOwnerDataSetValueBean(),systemParamsBuf);
    }
    
    private void addRowgroupColsToParams(StringBuffer systemParamsBuf)
    {
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrdbean!=null&&alrdbean.getLstRowgroupColsColumn()!=null
                &&this.provider.getOwnerDataSetValueBean().isMatchDatasetid(alrdbean.getRowgroupDatasetId()))
        {
            StringBuffer rowGroupColsBuf=new StringBuffer();
            for(String rowgroupColTmp:alrdbean.getLstRowgroupColsColumn())
            {
                if(rowgroupColTmp!=null&&!rowgroupColTmp.trim().equals("")) rowGroupColsBuf.append(rowgroupColTmp).append(",");
            }
            if(rowGroupColsBuf.length()>0)
            {
                if(rowGroupColsBuf.charAt(rowGroupColsBuf.length()-1)==',') rowGroupColsBuf.deleteCharAt(rowGroupColsBuf.length()-1);
                systemParamsBuf.append("{[(<rowgroup_cols:"+rowGroupColsBuf.toString()+">)]}");
            }
        }
    }

    protected void setConditionValue(ReportRequest rrequest,SPDataSetValueBean spBean,CallableStatement cstmt,AbsDatabaseType dbtype,int index,
            String conname,VarcharType varcharObj) throws SQLException
    {
        ConditionBean cbean=rbean.getSbean().getConditionBeanByName(conname);
        if(cbean.getIterator()>1||cbean.getCcolumnsbean()!=null||cbean.getCvaluesbean()!=null)
        {
            varcharObj.setPreparedStatementValue(index,cbean.getConditionValueForSP(rrequest),cstmt,dbtype);
        }else
        {
            cbean.getDatatypeObj().setPreparedStatementValue(index,cbean.getConditionValueForSP(rrequest),cstmt,dbtype);
        }
    }
    
}
