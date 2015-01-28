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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.CrossListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportFilterBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportRowGroupSubDisplayRowBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.statistic.StatisticItemBean;
import com.wabacus.system.component.application.report.configbean.crosslist.AbsCrossListReportColAndGroupBean;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;

public abstract class AbsReportDataSetValueProvider implements Cloneable
{
    protected ReportDataSetValueBean ownerDataSetValueBean;//所属的<value/>配置对象

    private List<ConditionBean> lstMyConditionBeans;//本数据集<value/>所对应的查询条件集合，在运行时构造
    
    protected List<StatisticItemBean> lstReportStatitemBeans;
    
    protected List<StatisticItemBean> lstPageStatitemBeans;
    
    protected CrossListReportDatasetValueBean clrdvbean;
    
    public void setOwnerDataSetValueBean(ReportDataSetValueBean ownerDataSetValueBean)
    {
        this.ownerDataSetValueBean=ownerDataSetValueBean;
    }

    public ReportDataSetValueBean getOwnerDataSetValueBean()
    {
        return ownerDataSetValueBean;
    }

    public ReportBean getReportBean()
    {
        return this.ownerDataSetValueBean.getReportBean();
    }
    
    public CrossListReportDatasetValueBean getClrdvbean()
    {
        return clrdvbean;
    }

    protected List<ColBean> getLstRealColBeans(ReportRequest rrequest)
    {
        ReportBean rbean=this.getReportBean();
        List<ColBean> lstColBeans=rrequest.getCdb(rbean.getId()).getLstDynOrderColBeans();
        if(lstColBeans==null||lstColBeans.size()==0) lstColBeans=rbean.getDbean().getLstCols();
        List<ColBean> lstResults=new ArrayList<ColBean>();
        for(ColBean cbTmp:lstColBeans)
        {
            if(!cbTmp.isMatchDataSet(this.ownerDataSetValueBean)) continue;//不是从这个数据集中取数据
            if(cbTmp.getColumn()==null||cbTmp.getColumn().trim().equals("")) continue;
            if(cbTmp.isNonFromDbCol()||cbTmp.isNonValueCol()||cbTmp.isSequenceCol()||cbTmp.isControlCol()) continue;
            lstResults.add(cbTmp);
        }
        return lstResults;
    }
    
    public List<ConditionBean> getLstMyConditionBeans(ReportRequest rrequest)
    {
        if(rrequest==null)
        {
            throw new WabacusConfigLoadingException("此方法只能在运行时调用");
        }
        if(lstMyConditionBeans!=null) return lstMyConditionBeans;
        List<ConditionBean> lstConditionBeans=this.getReportBean().getSbean().getLstConditions();
        if(lstConditionBeans==null||lstConditionBeans.size()==0) return null;
        List<ConditionBean> lstMyConditionBeansTmp=new ArrayList<ConditionBean>();
        for(ConditionBean conbean:lstConditionBeans)
        {
            if(conbean.isBelongTo(this.ownerDataSetValueBean))
            {
                lstMyConditionBeansTmp.add(conbean);
            }
        }
        this.lstMyConditionBeans=lstMyConditionBeansTmp;
        return this.lstMyConditionBeans;
    }
    
    public String[] getFilterColumnAndConditionValues(ReportRequest rrequest)
    {
        AbsListReportFilterBean filterBean=rrequest.getCdb(this.getReportBean().getId()).getFilteredBean();
        if(filterBean==null) return null;
        ColBean cbTmp=(ColBean)filterBean.getOwner();
        if(!cbTmp.isMatchDataSet(this.ownerDataSetValueBean)) return null;
        String filterval=rrequest.getStringAttribute(filterBean.getId(),"");
        if(filterval.equals("")) return null;
        if(cbTmp.getDatatypeObj()==null||cbTmp.getDatatypeObj() instanceof VarcharType||cbTmp.getDatatypeObj() instanceof AbsDateTimeType)
        {
            filterval=Tools.replaceAll(filterval,";;","','");
            if(!filterval.startsWith("'")) filterval="'"+filterval;
            if(filterval.endsWith("','")) filterval=filterval.substring(0,filterval.length()-3);
            if(!filterval.endsWith("'")) filterval=filterval+"'";
            if(filterval.equals("'")) filterval="";
        }else
        {
            filterval=Tools.replaceAll(filterval,";;",",");
            if(filterval.endsWith(",")) filterval=filterval.substring(0,filterval.length()-1);
        }
        String column=null;
        if(filterBean.getFilterColumnExpression()!=null&&!filterBean.getFilterColumnExpression().trim().equals(""))
        {//配置了字段表达式
            column=filterBean.getFilterColumnExpression();
        }else
        {
            column=cbTmp.getColumn();
        }
        return Tools.isEmpty(filterval)||Tools.isEmpty(column)?null:new String[] { column, filterval };
    }
    
    public ColBean getClickOrderByCbean(ReportRequest rrequest)
    {
        String[] orderbys=(String[])rrequest.getAttribute(this.getReportBean().getId(),"ORDERBYARRAY");
        if(orderbys==null||orderbys.length!=2) return null;
        ColBean cbeanClickOrderby=this.getReportBean().getDbean().getColBeanByColColumn(orderbys[0]);
        return cbeanClickOrderby!=null&&cbeanClickOrderby.isMatchDataSet(this.ownerDataSetValueBean)?cbeanClickOrderby:null;
    }
    
    public String[] getClickOrderByColumnAndOrder(ReportRequest rrequest)
    {
        ColBean cbean=getClickOrderByCbean(rrequest);
        if(cbean==null) return null;
        String[] orderbys=(String[])rrequest.getAttribute(this.getReportBean().getId(),"ORDERBYARRAY");
        if(orderbys==null||orderbys.length!=2||Tools.isEmpty(orderbys[0])||Tools.isEmpty(orderbys[1])) return null;
        return orderbys;
    }
    
    protected boolean isRowGroupColBelongsToMe()
    {
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)this.getReportBean().getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrdbean==null) return false;
        return this.ownerDataSetValueBean.isMatchDatasetid(alrdbean.getRowgroupDatasetId());
    }
    
    public String getDynamicSelectCols(ReportRequest rrequest)
    {
        if(this.clrdvbean==null) return "";
        return this.clrdvbean.getAllDynamicSelectCols(rrequest);
    }
    
    public String getDynamicCrossStatiSelectColsWithVerticalStati(ReportRequest rrequest)
    {
        if(this.clrdvbean==null) return "";
        return this.clrdvbean.getDynamicCrossStatiSelectColsWithVerticalStati(rrequest);
    }
    
    public abstract List<String> getColFilterDataSet(ReportRequest rrequest,ColBean filterColBean,boolean isGetSelectedOptions,int maxOptionCount);
    
    public abstract int getRecordcount(ReportRequest rrequest);

    public abstract List<Map<String,Object>> getDataSet(ReportRequest rrequest,List<AbsReportDataPojo> lstReportData,int startRownum,int endRownum);

    public abstract Map<String,Object> getStatisticDataSet(ReportRequest rrequest,
            AbsListReportRowGroupSubDisplayRowBean rowGroupSubDisplayRowBean,Map<String,String> mRowGroupColumnValues);
    
    public void addCrossColGroupBean(AbsCrossListReportColAndGroupBean crossColGroupBean)
    {
        if(this.clrdvbean==null)this.clrdvbean=createCrossListDataSetValueBean();
        this.clrdvbean.addCrossColGroupBean(crossColGroupBean);
    }
    
    protected CrossListReportDatasetValueBean createCrossListDataSetValueBean()
    {
        return new CrossListReportDatasetValueBean();
    }

    public void loadConfig(XmlElementBean eleValueBean)
    {}

    public void afterSqlLoad()
    {}
    
    public void doPostLoad()
    {}

    public void addStaticItemBean(StatisticItemBean staticitembean)
    {
        if(staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                ||staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_REPORT)
        {
            if(this.lstReportStatitemBeans==null) this.lstReportStatitemBeans=new ArrayList<StatisticItemBean>();
            this.lstReportStatitemBeans.add(staticitembean);
        }
        if(staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                ||staticitembean.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_PAGE)
        {
            if(this.lstPageStatitemBeans==null) this.lstPageStatitemBeans=new ArrayList<StatisticItemBean>();
            this.lstPageStatitemBeans.add(staticitembean);
        }
    }
    
    public void doPostLoadStatistic()
    {}
    
    public void doPostLoadCrosslist()
    {
        if(this.clrdvbean!=null) this.clrdvbean.doPostLoad();
    }
    
    protected Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    public AbsReportDataSetValueProvider clone(ReportDataSetValueBean newOwnerDataSetValueBean)
    {
        try
        {
            AbsReportDataSetValueProvider newValueTypeObj=(AbsReportDataSetValueProvider)this.clone();
            newValueTypeObj.setOwnerDataSetValueBean(newOwnerDataSetValueBean);
            return newValueTypeObj;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    protected class CrossListReportDatasetValueBean
    {
        protected List<AbsCrossListReportColAndGroupBean> lstIncludeCommonCrossColAndGroupBeans;

        protected List<AbsCrossListReportColAndGroupBean> lstIncludeCrossStatiColAndGroupBeans;
        
        protected void addCrossColGroupBean(AbsCrossListReportColAndGroupBean crossColGroupBean)
        {
            if(crossColGroupBean.isCommonCrossColGroup())
            {
                if(lstIncludeCommonCrossColAndGroupBeans==null) lstIncludeCommonCrossColAndGroupBeans=new ArrayList<AbsCrossListReportColAndGroupBean>();
                for(AbsCrossListReportColAndGroupBean cgBeanTmp:this.lstIncludeCommonCrossColAndGroupBeans)
                {
                    if(cgBeanTmp.getColumn().equals(crossColGroupBean.getColumn())) return;
                }
                this.lstIncludeCommonCrossColAndGroupBeans.add(crossColGroupBean);
            }else if(crossColGroupBean.isStatisticCrossColGroup())
            {
                if(lstIncludeCrossStatiColAndGroupBeans==null) lstIncludeCrossStatiColAndGroupBeans=new ArrayList<AbsCrossListReportColAndGroupBean>();
                for(AbsCrossListReportColAndGroupBean cgBeanTmp:this.lstIncludeCrossStatiColAndGroupBeans)
                {
                    if(cgBeanTmp.getColumn().equals(crossColGroupBean.getColumn())) return;
                }
                this.lstIncludeCrossStatiColAndGroupBeans.add(crossColGroupBean);
            }
        }
        
        public String getDynamicCommonSelectCols(ReportRequest rrequest)
        {
            return getDynamicSelectCols(this.lstIncludeCommonCrossColAndGroupBeans,getMDynamicSelectCols(rrequest));
        }

        public String getDynamicCrossStatiSelectCols(ReportRequest rrequest)
        {
            return getDynamicSelectCols(this.lstIncludeCrossStatiColAndGroupBeans,getMDynamicSelectCols(rrequest));
        }

        private Map<String,String> getMDynamicSelectCols(ReportRequest rrequest)
        {
            return ((CrossListReportType)rrequest.getDisplayReportTypeObj(getReportBean())).getMDynamicSelectCols(ownerDataSetValueBean);
        }
        
        private String getDynamicSelectCols(List<AbsCrossListReportColAndGroupBean> lstCrossColAndGroupBeans,Map<String,String> mAllSelectCols)
        {
            if(lstCrossColAndGroupBeans==null||lstCrossColAndGroupBeans.size()==0) return "";
            if(mAllSelectCols==null||mAllSelectCols.size()==0) return "";
            StringBuffer resultBuf=new StringBuffer();
            String selectColsTmp;
            for(AbsCrossListReportColAndGroupBean colgroupBeanTmp:lstCrossColAndGroupBeans)
            {
                selectColsTmp=mAllSelectCols.get(colgroupBeanTmp.getRootCrossColGroupId());
                if(selectColsTmp==null||selectColsTmp.trim().equals("")) continue;
                resultBuf.append(selectColsTmp).append(",");
            }
            if(resultBuf.length()>0&&resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
            return resultBuf.toString();
        }
        
        public String getAllDynamicSelectCols(ReportRequest rrequest)
        {
            String crossStatiDynCols=getDynamicCrossStatiSelectCols(rrequest).trim();
            String commonDynCols=getDynamicCommonSelectCols(rrequest).trim();
            if(crossStatiDynCols.equals("")) return commonDynCols;
            if(commonDynCols.equals("")) return crossStatiDynCols;
            return commonDynCols+","+crossStatiDynCols;
        }
        
        protected String getDynamicCrossStatiSelectColsWithVerticalStati(ReportRequest rrequest)
        {
            if(this.lstIncludeCrossStatiColAndGroupBeans==null||lstIncludeCrossStatiColAndGroupBeans.size()==0) return "";
            Map<String,String> mAllSelectCols=getMDynamicSelectCols(rrequest);
            if(mAllSelectCols==null||mAllSelectCols.size()==0) return "";
            StringBuffer resultBuf=new StringBuffer();
            String selectColsTmp;
            for(AbsCrossListReportColAndGroupBean colgroupBeanTmp:lstIncludeCrossStatiColAndGroupBeans)
            {
                if(!colgroupBeanTmp.getInnerDynamicColBean().isHasVerticalstatistic()) continue;//当前交叉统计不需要针对每个动态列进行统计
                selectColsTmp=mAllSelectCols.get(colgroupBeanTmp.getRootCrossColGroupId());
                if(selectColsTmp==null||selectColsTmp.trim().equals("")) continue;
                resultBuf.append(selectColsTmp).append(",");
            }
            if(resultBuf.length()>0&&resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
            return resultBuf.toString();
        }
        
        protected void doPostLoad()
        {}
    }
}
