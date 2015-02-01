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
package com.wabacus.system.dataset.select.report.value;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.dataexport.AbsDataExportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportRowGroupSubDisplayRowBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.statistic.StatisticItemBean;
import com.wabacus.system.dataset.select.rationaldbassistant.BatchStatisticItems;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class RelationalDBReportDataSetValueProvider extends AbsReportDataSetValueProvider
{
    private static Log log=LogFactory.getLog(RelationalDBReportDataSetValueProvider.class);

    public final static String STATISQL_PLACEHOLDER="%STATISTIC_SQL%";

    protected String value;

    protected String statiReportSqlWithoutCondition;

    protected List<StatisticItemBean> lstReportStatitemBeansWithCondition;

    protected List<StatisticItemBean> lstReportStatitemBeansWithoutCondition;//配置针对整个报表数据进行统计不带条件的统计项

    protected String statiPageSqlWithoutCondition;

    protected List<StatisticItemBean> lstPageStatitemBeansWithCondition;

    protected List<StatisticItemBean> lstPageStatitemBeansWithoutCondition;

    protected String statiSqlWithCondition;

    public String getValue()
    {
        return this.value;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public List<String> getColFilterDataSet(ReportRequest rrequest,ColBean filterColBean,boolean isGetSelectedOptions,int maxOptionCount)
    {
        return null;
    }

    public int getRecordcount(ReportRequest rrequest)
    {
        return 0;
    }

    public List<Map<String,Object>> getDataSet(ReportRequest rrequest,List<AbsReportDataPojo> lstReportData,int startRownum,int endRownum)
    {
        return null;
    }

    public Map<String,Object> getStatisticDataSet(ReportRequest rrequest,AbsListReportRowGroupSubDisplayRowBean rowGroupSubDisplayRowBean,
            Map<String,String> mRowGroupColumnValues)
    {
        Map<String,Object> mResultData=new HashMap<String,Object>();
        ReportBean rbean=this.getReportBean();
        String groupbyClause=null;//如果本次是统计某个行分组对应的数据时，这里存放group by
        if(rowGroupSubDisplayRowBean==null)
        {
            CacheDataBean cdb=rrequest.getCdb(rbean.getId());
            if(cdb.isLoadAllReportData()||this.ownerDataSetValueBean.isDependentDataSet())
            {
                mResultData.putAll(getStatitemDataSetWithoutCondition(rrequest,
                        new BatchStatisticItems(this.lstPageStatitemBeansWithoutCondition,true),this.statiPageSqlWithoutCondition,null,-1,-1));
                mResultData.putAll(getStatitemDataSetWithCondition(rrequest,new BatchStatisticItems(this.lstPageStatitemBeansWithCondition,true),
                        this.statiSqlWithCondition,null,-1,-1));
            }else
            {//当前是加载某一页报表数据
                Integer startRownum=cdb.getStartRownumOfDsvbean(this.ownerDataSetValueBean);;
                Integer endRownum=cdb.getEndRownumOfDsvbean(this.ownerDataSetValueBean);
                if(startRownum!=null&&endRownum!=null&&startRownum>=0&&endRownum>startRownum)
                {
                    mResultData.putAll(getStatitemDataSetWithoutCondition(rrequest,new BatchStatisticItems(this.lstPageStatitemBeansWithoutCondition,
                            true),this.statiPageSqlWithoutCondition,null,startRownum,endRownum));
                    mResultData.putAll(getStatitemDataSetWithCondition(rrequest,new BatchStatisticItems(this.lstPageStatitemBeansWithCondition,true),
                            this.statiSqlWithCondition,null,startRownum,endRownum));
                }
            }
        }else
        {
            groupbyClause=this.getRowGroupStatiGroupByClause(rowGroupSubDisplayRowBean,mRowGroupColumnValues);
        }
        mResultData.putAll(getStatitemDataSetWithoutCondition(rrequest,new BatchStatisticItems(this.lstReportStatitemBeansWithoutCondition,false),
                this.statiReportSqlWithoutCondition,groupbyClause,-1,-1));
        mResultData.putAll(getStatitemDataSetWithCondition(rrequest,new BatchStatisticItems(this.lstReportStatitemBeansWithCondition,false),
                this.statiSqlWithCondition,groupbyClause,-1,-1));
        return mResultData.size()==0?null:mResultData;
    }

    private Map<String,Object> getStatitemDataSetWithoutCondition(ReportRequest rrequest,BatchStatisticItems batStatitems,String statisql,
            String groupbyClause,int startRownum,int endRownum)
    {
        Map<String,Object> mResultData=new HashMap<String,Object>();
        if(batStatitems.getLstStatitemBeans()==null||batStatitems.getLstStatitemBeans().size()==0) return mResultData;
        if(statisql==null||statisql.trim().equals("")) return mResultData;
        if(groupbyClause!=null&&!groupbyClause.trim().equals("")&&this.isRowGroupColBelongsToMe())
        {//是针对某个分组的统计，且当前数据集就是查询分组数据的数据集（对于不是查询分组数据的数据集，其上的统计不进行分组统计，而是对整个报表的统计）
            statisql=statisql+"  "+groupbyClause;
        }
        parseStatisticData(getStatisticDataSet(rrequest,batStatitems,statisql,startRownum,endRownum),mResultData,batStatitems,rrequest
                .getDbType(this.ownerDataSetValueBean.getDatasource()));
        return mResultData;
    }

    private Map<String,Object> getStatitemDataSetWithCondition(ReportRequest rrequest,BatchStatisticItems batStatitems,String statisql,
            String groupbyClause,int startRownum,int endRownum)
    {
        Map<String,Object> mResultData=new HashMap<String,Object>();
        if(batStatitems.getLstStatitemBeans()==null||batStatitems.getLstStatitemBeans().size()==0) return mResultData;
        if(statisql==null||statisql.trim().equals("")) return mResultData;
        AbsDatabaseType dbtype=rrequest.getDbType(this.ownerDataSetValueBean.getDatasource());
        String sqlTmp;
        List<StatisticItemBean> lstStatitemBeansLocal=new ArrayList<StatisticItemBean>();
        BatchStatisticItems batStatitemsLocal;
        for(StatisticItemBean alrsibeanTmp:batStatitems.getLstStatitemBeans())
        {
            sqlTmp=Tools.replaceAll(statisql,"%SELECTEDCOLUMNS%",alrsibeanTmp.getValue()+" as "+alrsibeanTmp.getProperty());
            String realcondition=getRealStatisticItemConditionValues(rrequest,alrsibeanTmp.getLstConditions());
            if(realcondition.trim().equals(""))
            {
                sqlTmp=Tools.replaceAll(sqlTmp,"%CONDITION%","");
            }else
            {
                sqlTmp=Tools.replaceAll(sqlTmp,"%CONDITION%"," where "+realcondition);
            }
            if(groupbyClause!=null&&!groupbyClause.trim().equals("")&&this.isRowGroupColBelongsToMe())
            {
                sqlTmp=sqlTmp+"  "+groupbyClause;
            }
            lstStatitemBeansLocal.clear();
            lstStatitemBeansLocal.add(alrsibeanTmp);
            batStatitemsLocal=new BatchStatisticItems(lstStatitemBeansLocal,batStatitems.isStatisticForOnePage());
            parseStatisticData(getStatisticDataSet(rrequest,batStatitemsLocal,sqlTmp,startRownum,endRownum),mResultData,batStatitemsLocal,dbtype);
        }
        return mResultData;
    }
    
    private String getRealStatisticItemConditionValues(ReportRequest rrequest,List<ConditionBean> lstConditionBeans)
    {
        if(lstConditionBeans==null||lstConditionBeans.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        String conditionValTmp;
        for(ConditionBean cbeanTmp:lstConditionBeans)
        {
            if(cbeanTmp.isConstant())
            {//是常量查询条件表达式
                conditionValTmp=cbeanTmp.getConditionExpression().getValue();
            }else
            {
                conditionValTmp=cbeanTmp.getDynamicConditionvalueForSql(rrequest,-1);
            }
            if(conditionValTmp!=null&&!conditionValTmp.trim().equals("")) resultBuf.append(conditionValTmp).append(" and ");
        }
        conditionValTmp=resultBuf.toString().trim();
        if(conditionValTmp.endsWith(" and"))
        {
            conditionValTmp=conditionValTmp.substring(0,conditionValTmp.length()-4);
        }
        return conditionValTmp;
    }
    
    protected Object getStatisticDataSet(ReportRequest rrequest,BatchStatisticItems batStatitems,String statisticsql,int startRownum,int endRownum)
    {
        return null;
    }
    
    //private String statiSqlGroupby;//当前行分组统计SQL语句的group by部分

    protected String getRowGroupStatiGroupByClause(AbsListReportRowGroupSubDisplayRowBean rowGroupSubDisplayRowBean,
            Map<String,String> mRowGroupColumnValues)
    {
        if(rowGroupSubDisplayRowBean==null) return null;
        ReportBean rbean=this.getReportBean();
        String[] colsArr=rowGroupSubDisplayRowBean.getParentAndMyOwnRowGroupColumnsArray(rbean);
        StringBuffer groupbyBuf=new StringBuffer();
        for(int i=0;i<colsArr.length;i++)
        {
            groupbyBuf.append(colsArr[i]).append(",");
        }
        if(groupbyBuf.charAt(groupbyBuf.length()-1)==',')
        {
            groupbyBuf.deleteCharAt(groupbyBuf.length()-1);
        }
        String realGroupByClause=" group by "+groupbyBuf.toString()+" having "+getStatiRowGroupConditionExpression(rowGroupSubDisplayRowBean);
        String colvalTmp;
        for(int i=0;i<colsArr.length;i++)
        {
            colvalTmp=mRowGroupColumnValues.get(colsArr[i]);
            if(colvalTmp==null) colvalTmp="";
            realGroupByClause=Tools.replaceAll(realGroupByClause,"#"+colsArr[i]+"#",colvalTmp);
        }
        return realGroupByClause;
    }

    private String getStatiRowGroupConditionExpression(AbsListReportRowGroupSubDisplayRowBean rowGroupSubDisplayRowBean)
    {
        String condition;
        if(rowGroupSubDisplayRowBean.getCondition()!=null&&!rowGroupSubDisplayRowBean.getCondition().trim().equals(""))
        {
            //rowGroupSubDisplayRowBean.validateConditionConfig(rbean);//校验condition配置的合法性
            condition=rowGroupSubDisplayRowBean.getCondition();
        }else
        {
            ColBean[] cbeansArr=rowGroupSubDisplayRowBean.getParentAndMyOwnRowGroupColBeans(this.getReportBean());
            StringBuffer conditionBuf=new StringBuffer();
            ColBean cbTmp;
            String tmp;
            for(int i=0;i<cbeansArr.length;i++)
            {
                cbTmp=cbeansArr[i];
                if(cbTmp.getDatatypeObj()==null||cbTmp.getDatatypeObj() instanceof VarcharType||cbTmp.getDatatypeObj() instanceof AbsDateTimeType)
                {
                    tmp="'";
                }else
                {
                    tmp="";
                }
                conditionBuf.append(cbTmp.getColumn()).append("=").append(tmp).append("#").append(cbTmp.getColumn()).append("#").append(tmp);
                conditionBuf.append(" and ");
            }
            condition=conditionBuf.toString().trim();
            if(condition.endsWith(" and")) condition=condition.substring(0,condition.length()-4);
        }
        return condition;
    }
    
    public String getFilterConditionExpression(ReportRequest rrequest)
    {
        String[] filterColumnAndConditoinValues=super.getFilterColumnAndConditionValues(rrequest);
        if(filterColumnAndConditoinValues==null||filterColumnAndConditoinValues.length!=2) return "";
        return filterColumnAndConditoinValues[0]+" in ("+filterColumnAndConditoinValues[1]+") ";
    }

    public String getRowSelectValueConditionExpression(ReportRequest rrequest)
    {
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE||rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return null;
        ReportBean rbean=getReportBean();
        IComponentType typeObj=rrequest.getComponentTypeObj(rbean,null,false);
        if(!(typeObj instanceof AbsListReportType)) return null;
        AbsDataExportBean dataExportBean=rbean.getDataExportsBean()!=null?rbean.getDataExportsBean().getDataExportBean(rrequest.getShowtype()):null;
        if(dataExportBean==null) return null;//没有配置<dataexport/>，则不可能取行选中数据
        List<String> lstRowSelectColProperties=this.ownerDataSetValueBean.getLstRowSelectValueColProperties();
        if(Tools.isEmpty(lstRowSelectColProperties)) return null;
        List<Map<String,String>> lstSelectRowDatas=rrequest.getCdb(rbean.getId()).getLstRowSelectData();
        if(Tools.isEmpty(lstSelectRowDatas)) return null;//没有选中的数据，则导满足条件的所有数据
        StringBuilder resultBuf=new StringBuilder();
        String nameTmp, valueTmp;
        for(String colPropertyTmp:lstRowSelectColProperties)
        {
            nameTmp=dataExportBean.getRowSelectDataBean().getColExpression(colPropertyTmp);
            if(Tools.isEmpty(nameTmp)) continue;
            resultBuf.append(nameTmp).append(" in (");
            for(Map<String,String> mRowDataTmp:lstSelectRowDatas)
            {
                valueTmp=mRowDataTmp.get(colPropertyTmp);
                if(valueTmp==null) valueTmp="";
                resultBuf.append("'"+valueTmp+"',");
            }
            if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
            resultBuf.append(") and ");
        }
        String resultStr=resultBuf.toString().trim();
        if(resultStr.endsWith(" and")) resultStr=resultStr.substring(0,resultStr.length()-4);
        return resultStr.trim().equals("")?resultStr:" ("+resultStr+") ";
    }
    
    protected List<String> parseColFilterResultDataset(ReportRequest rrequest,ColBean cbean,Object colFilterDataSet,int maxOptionCount)
    {
        AbsDatabaseType dbtype=rrequest.getDbType(this.ownerDataSetValueBean.getDatasource());
        List<String> lstFilterDataResult=null;
        if(colFilterDataSet instanceof ResultSet)
        {
            lstFilterDataResult=new ArrayList<String>();
            ResultSet rs=(ResultSet)colFilterDataSet;
            Object valObj;
            String strvalue;
            try
            {
                while(rs.next())
                {
                    valObj=cbean.getDatatypeObj().getColumnValue(rs,cbean.getColumn(),dbtype);
                    strvalue=cbean.getDatatypeObj().value2label(valObj);
                    strvalue=strvalue==null?"":strvalue.trim();
                    if(strvalue.equals("")||lstFilterDataResult.contains(strvalue)) continue;
                    lstFilterDataResult.add(strvalue);
                    if(maxOptionCount>0&&lstFilterDataResult.size()==maxOptionCount)
                    {
                        break;
                    }
                }
                rs.close();
            }catch(SQLException e)
            {
                throw new WabacusRuntimeException("加载报表"+this.getReportBean().getPath()+"的列过滤数据失败",e);
            }
        }else if(colFilterDataSet instanceof List)
        {
            lstFilterDataResult=(List<String>)colFilterDataSet;
            if(maxOptionCount>0)
            {
                while(lstFilterDataResult.size()>maxOptionCount)
                    lstFilterDataResult.remove(lstFilterDataResult.size()-1);
            }
        }else
        {
            throw new WabacusRuntimeException("加载报表"+this.getReportBean().getPath()+"的列过滤数据失败，数据集返回的数据类型不合法，即不是ResultSet也不是List类型");
        }
        return lstFilterDataResult;
    }

    protected int parseRecordCount(Object recordCountDataSet)
    {
        if(recordCountDataSet==null) return 0;
        int recordcount=0;
        if(recordCountDataSet instanceof List)
        {
            List lst=(List)recordCountDataSet;
            if(lst.size()==0)
            {
                recordcount=0;
            }else
            {
                if(!(lst.get(0) instanceof Integer))
                {
                    throw new WabacusRuntimeException("报表"+this.getReportBean().getPath()+"拦截器在查询记录数时返回的记录数不是合法数字，必须返回Integer类型的数据");
                }
                recordcount=(Integer)lst.get(0);
                if(recordcount<0) recordcount=0;
            }
        }else if(recordCountDataSet instanceof ResultSet)
        {
            ResultSet rs=(ResultSet)recordCountDataSet;
            try
            {
                if(rs.next()) recordcount=rs.getInt(1);
            }catch(SQLException e)
            {
                throw new WabacusRuntimeException("报表"+this.getReportBean().getPath()+"拦截器在查询记录数失败",e);
            }finally
            {
                try
                {
                    rs.close();
                }catch(SQLException e)
                {
                   log.warn("关闭ResultSet失败",e);
                }
            }
        }else
        {
            throw new WabacusRuntimeException("报表"+this.getReportBean().getPath()+"拦截器在查询记录数时返回的记录数类型"+recordCountDataSet.getClass().getName()+"无效");
        }
        return recordcount;
    }
    
    protected List<Map<String,Object>> parseResultDataset(ReportRequest rrequest,Object reportDataSet)
    {
        if(reportDataSet==null) return null;
        List<Map<String,Object>> lstResult=new ArrayList<Map<String,Object>>();
        List<ColBean> lstColBeans=getLstRealColBeans(rrequest);
        if(lstColBeans==null||lstColBeans.size()==0) return null;
        if(reportDataSet instanceof List)
        {
            List lstData=(List)reportDataSet;
            if(lstData==null||lstData.size()==0) return null;
            while(lstData.size()>0&&lstData.get(0)==null)
            {
                lstData.remove(0);
            }
            if(lstData.size()==0) return null;
            if(lstData.get(0) instanceof Map) return lstData;
            if(!(lstData.get(0) instanceof AbsReportDataPojo))
            {
                throw new WabacusRuntimeException("获取报表"+this.ownerDataSetValueBean.getReportBean().getPath()+"数据时，加载数据前置动作返回的结果集类型："
                        +lstData.get(0).getClass().getName()+"为无效结果类型，没有继承"+AbsReportDataPojo.class.getName()+"类");
            }
            List<String> lstDependingParentColumns=null;
            if(this.ownerDataSetValueBean.isDependentDataSet())
            {//当前数据集是子数据集
                lstDependingParentColumns=this.ownerDataSetValueBean.getLstDependParentColumns();
                for(ColBean cbTmp:lstColBeans)
                {
                    if(lstDependingParentColumns.contains(cbTmp.getProperty())) lstDependingParentColumns.remove(cbTmp.getProperty());
                }
            }
            AbsReportDataPojo pojoDataTmp;
            Map<String,Object> mRowDataTmp;
            for(int i=0;i<lstData.size();i++)
            {
                pojoDataTmp=(AbsReportDataPojo)lstData.get(i);
                if(pojoDataTmp==null) continue;
                mRowDataTmp=new HashMap<String,Object>();
                for(ColBean cbTmp:lstColBeans)
                {
                    mRowDataTmp
                            .put("[DYN_COL_DATA]".equals(cbTmp.getProperty())?cbTmp.getColumn():cbTmp.getProperty(),pojoDataTmp.getColValue(cbTmp));
                }
                if(lstDependingParentColumns!=null)
                {
                    for(String colTmp:lstDependingParentColumns)
                    {
                        mRowDataTmp.put(colTmp,String.valueOf(pojoDataTmp.getColValue(colTmp)));//参与依赖的列字段全部强制以字符串类型获取，在进行匹配时还可以通过用户配置的格式化方法进行格式化后再比较
                    }
                }
                lstResult.add(mRowDataTmp);
            }
        }else if(reportDataSet instanceof ResultSet)
        {
            ResultSet rs=(ResultSet)reportDataSet;
            AbsDatabaseType dbtype=rrequest.getDbType(this.ownerDataSetValueBean.getDatasource());
            try
            {
                List<String> lstDependingColumns=null;
                if(this.ownerDataSetValueBean.isDependentDataSet())
                {
                    lstDependingColumns=this.ownerDataSetValueBean.getLstDependMyColumns();
                    for(ColBean cbTmp:lstColBeans)
                    {
                        if(lstDependingColumns.contains(cbTmp.getProperty())) lstDependingColumns.remove(cbTmp.getProperty());
                    }
                }
                Map<String,Object> mRowDataTmp;
                while(rs.next())
                {
                    mRowDataTmp=new HashMap<String,Object>();
                    for(ColBean cbTmp:lstColBeans)
                    {
                        mRowDataTmp.put("[DYN_COL_DATA]".equals(cbTmp.getProperty())?cbTmp.getColumn():cbTmp.getProperty(),getColumnValueFromRs(
                                rrequest,rs,dbtype,cbTmp));
                    }
                    if(lstDependingColumns!=null)
                    {//当前数据集是子数据集，则将子数据集中参与依赖的字段数据也全部取出来，以便后面比较
                        for(String colTmp:lstDependingColumns)
                        {
                            mRowDataTmp.put(colTmp,rs.getString(colTmp));
                        }
                    }
                    lstResult.add(mRowDataTmp);
                }
            }catch(SQLException e)
            {
                throw new WabacusRuntimeException("获取报表"+this.ownerDataSetValueBean.getReportBean().getPath()+"数据失败",e);
            }finally
            {
                try
                {
                    rs.close();
                }catch(SQLException e)
                {
                    log.warn("获取报表"+this.ownerDataSetValueBean.getReportBean().getPath()+"数据时关闭ResultSet失败",e);
                }
            }
        }else
        {
            throw new WabacusRuntimeException("获取报表"+this.ownerDataSetValueBean.getReportBean().getPath()+"数据时，返回的结果集："
                    +reportDataSet.getClass().getName()+"为无效结果类型");
        }
        return lstResult;
    }

    private Object getColumnValueFromRs(ReportRequest rrequest,ResultSet rs,AbsDatabaseType dbtype,ColBean colbean)
    {
        Object columnvalue=null;
        String column=colbean.getColumn();
        column=column==null?"":column.trim();
        if(!column.equals("")&&!colbean.isNonFromDbCol()&&!colbean.isNonValueCol()&&!colbean.isSequenceCol()&&!colbean.isControlCol())
        {
            try
            {
                if(colbean.isI18n()&&rrequest!=null&&!rrequest.getLocallanguage().trim().equals(""))
                {
                    column=column+"_"+rrequest.getLocallanguage();
                    try
                    {
                        columnvalue=colbean.getDatatypeObj().getColumnValue(rs,column,dbtype);
                    }catch(SQLException sqle)
                    {
                        log.warn("根据列名"+column+"获取数据失败，可能是数据表中不支持"+rrequest.getLocallanguage()+"语言",sqle);
                        columnvalue=colbean.getDatatypeObj().getColumnValue(rs,colbean.getColumn(),dbtype);
                    }
                }else
                {
                    columnvalue=colbean.getDatatypeObj().getColumnValue(rs,column,dbtype);
                }
            }catch(Exception e)
            {
                throw new WabacusRuntimeException("获取报表"+this.getReportBean().getPath()+"的列"+colbean.getColumn()+"数据失败",e);
            }
        }
        return columnvalue;
    }
    
    protected void parseStatisticData(Object statiDataObj,Map<String,Object> mStatiDatas,BatchStatisticItems batStatitems,
            AbsDatabaseType dbtype)
    {
        if(statiDataObj==null) return;
        if(statiDataObj instanceof Map)
        {
            mStatiDatas.putAll((Map)statiDataObj);
        }else if(statiDataObj instanceof ResultSet)
        {
            ResultSet rs=(ResultSet)statiDataObj;
            try
            {
                if(rs.next())
                {
                    for(StatisticItemBean alrsibeanTmp:batStatitems.getLstStatitemBeans())
                    {
                        Object colVal=alrsibeanTmp.getDatatypeObj().getColumnValue(rs,alrsibeanTmp.getProperty(),dbtype);
                        mStatiDatas.put(batStatitems.isStatisticForOnePage()?"page_"+alrsibeanTmp.getProperty():alrsibeanTmp.getProperty(),colVal);
                    }
                }
            }catch(Exception e)
            {
                throw new WabacusRuntimeException("加载报表"+getReportBean().getPath()+"统计数据失败",e);
            }finally
            {
                try
                {
                    rs.close();
                }catch(SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }else
        {
            throw new WabacusRuntimeException("加载报表"+getReportBean().getPath()
                    +"统计数据失败，在加载数据的前置动作中，如果是统计数据的SQL语句，则只能返回SQL语句、ResultSet、或Map<String,Object>类型");
        }
    }

    public void addStaticItemBean(StatisticItemBean staticitembean)
    {
        super.addStaticItemBean(staticitembean);
        if(staticitembean.getLstConditions()==null||staticitembean.getLstConditions().size()==0)
        {
            if(staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                    ||staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_REPORT)
            {
                if(this.lstReportStatitemBeansWithoutCondition==null) this.lstReportStatitemBeansWithoutCondition=new ArrayList<StatisticItemBean>();
                this.lstReportStatitemBeansWithoutCondition.add(staticitembean);
            }
            if(staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                    ||staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_PAGE)
            {
                if(this.lstPageStatitemBeansWithoutCondition==null) this.lstPageStatitemBeansWithoutCondition=new ArrayList<StatisticItemBean>();
                this.lstPageStatitemBeansWithoutCondition.add(staticitembean);
            }
        }else
        {
            if(staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                    ||staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_REPORT)
            {
                if(this.lstReportStatitemBeansWithCondition==null) this.lstReportStatitemBeansWithCondition=new ArrayList<StatisticItemBean>();
                this.lstReportStatitemBeansWithCondition.add(staticitembean);
            }
            if(staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                    ||staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_PAGE)
            {
                if(this.lstPageStatitemBeansWithCondition==null) this.lstPageStatitemBeansWithCondition=new ArrayList<StatisticItemBean>();
                this.lstPageStatitemBeansWithCondition.add(staticitembean);
            }

        }
    }

    public void doPostLoadStatistic()
    {
        if((this.lstReportStatitemBeans==null||this.lstReportStatitemBeans.size()==0)
                &&(this.lstPageStatitemBeans==null||this.lstPageStatitemBeans.size()==0))
        {//在此数据集中没有配置统计项
            return;
        }
        this.statiReportSqlWithoutCondition=parseStatiSqlWithoutCondition(this.lstReportStatitemBeansWithoutCondition,STATISQL_PLACEHOLDER);
        this.statiPageSqlWithoutCondition=parseStatiSqlWithoutCondition(this.lstPageStatitemBeansWithoutCondition,STATISQL_PLACEHOLDER);
        if((this.lstReportStatitemBeansWithCondition!=null&&this.lstReportStatitemBeansWithCondition.size()>0)
                ||(this.lstPageStatitemBeansWithCondition!=null&&this.lstPageStatitemBeansWithCondition.size()>0))
        {
            this.statiSqlWithCondition="select %SELECTEDCOLUMNS% from (select * from ("+STATISQL_PLACEHOLDER
                    +") wx_tableStati1 %CONDITION%) tableStati2";
        }
    }

    private String parseStatiSqlWithoutCondition(List<StatisticItemBean> lstStatitemBeansWithoutCondition,String sql)
    {
        if(lstStatitemBeansWithoutCondition==null||lstStatitemBeansWithoutCondition.size()==0) return "";
        StringBuffer statisticColumnsBuf=new StringBuffer();
        for(StatisticItemBean statItemBeanTmp:lstStatitemBeansWithoutCondition)
        {
            statisticColumnsBuf.append(statItemBeanTmp.getValue()).append(" as ").append(statItemBeanTmp.getProperty()).append(",");
        }
        if(statisticColumnsBuf.length()>0&&statisticColumnsBuf.charAt(statisticColumnsBuf.length()-1)==',')
        {
            statisticColumnsBuf.deleteCharAt(statisticColumnsBuf.length()-1);
        }
        String sqlStati="";
        if(statisticColumnsBuf.length()>0)
        {
            sqlStati="select "+statisticColumnsBuf.toString()+" from ("+sql+") wx_tableStati";
        }
        return sqlStati;
    }
}
