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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.CrossListReportType;
import com.wabacus.system.component.application.report.configbean.UltraListReportGroupBean;
import com.wabacus.util.Consts;

public class CrossListReportGroupBean extends AbsCrossListReportColAndGroupBean
{
    private static Log log=LogFactory.getLog(CrossListReportGroupBean.class);
    
    private String column;
    
    private boolean hasCommonCrossColGroupChild;
    
    private boolean hasStatisticCrossColGroupChild;
    
    private AbsCrossListReportColAndGroupBean childCrossColGroupBeans;
    
    public CrossListReportGroupBean(AbsConfigBean owner)
    {
        super(owner);
    }
    
    public void setColumn(String column)
    {
        this.column=column;
    }

    public String getColumn()
    {
        return column;
    }

    public String getLabel(ReportRequest rrequest)
    {
        return ((UltraListReportGroupBean)this.getOwner()).getLabel(rrequest);
    }
    
    public void setDynColGroupSpecificBean(AbsDynamicColGroupBean dynColGroupSpecificBean)
    {
        this.hasCommonCrossColGroupChild=dynColGroupSpecificBean.isCommonCrossColGroup();
        this.hasStatisticCrossColGroupChild=dynColGroupSpecificBean.isStatisticCrossColGroup();
        super.setDynColGroupSpecificBean(dynColGroupSpecificBean);
    }

    public boolean hasDynamicColGroupChild()
    {
        if(isDynamicColGroup()) return true;
        return hasCommonCrossColGroupChild||hasStatisticCrossColGroupChild;
    }

    public boolean hasCommonCrossColGroupChild()
    {
        if(this.isCommonCrossColGroup()) return true;
        return hasCommonCrossColGroupChild;
    }

    public boolean hasStatisticCrossColGroupChild()
    {
        if(this.isStatisticCrossColGroup()) return true;
        return hasStatisticCrossColGroupChild;
    }

    protected List<CrossListReportStatiBean> getLstStatisBeans()
    {
        if(!this.isStatisticCrossColGroup()) return null;
        return this.childCrossColGroupBeans.getLstStatisBeans();
    }

    public CrossListReportColBean getInnerDynamicColBean()
    {
        return this.childCrossColGroupBeans.getInnerDynamicColBean();
    }

    public UltraListReportGroupBean getGroupBeanOwner()
    {
        return (UltraListReportGroupBean)this.getOwner();
    }
    
    public boolean getMDynamicColGroupDisplayType(ReportRequest rrequest,Map<String,Boolean> mDynamicColGroupDisplayType)
    {
        if(!this.isDynamicColGroup()&&!this.hasDynamicColGroupChild()) return true;
        boolean shouldDisplay=false;
        UltraListReportGroupBean groupBean=(UltraListReportGroupBean)this.getOwner();
        if(this.isDynamicColGroup())
        {//当前分组列是动态分组列
            shouldDisplay=this.childCrossColGroupBeans.getMDynamicColGroupDisplayType(rrequest,mDynamicColGroupDisplayType);
            if(!shouldDisplay) shouldDisplay=hasDisplayStatisBeans(mDynamicColGroupDisplayType);
        }else
        {
            AbsCrossListReportColAndGroupBean clrcgbeanTmp;
            for(Object childColGroupBeanTmp:groupBean.getLstChildren())
            {
                if(!(childColGroupBeanTmp instanceof ColBean)&&!(childColGroupBeanTmp instanceof UltraListReportGroupBean)) continue;
                clrcgbeanTmp=ListReportAssistant.getInstance().getCrossColAndGroupBean(childColGroupBeanTmp);
                if(clrcgbeanTmp==null||(!clrcgbeanTmp.isDynamicColGroup()&&!clrcgbeanTmp.hasDynamicColGroupChild()))
                {
                    if(!shouldDisplay
                            &&(childColGroupBeanTmp instanceof UltraListReportGroupBean||((ColBean)childColGroupBeanTmp).getDisplaytype(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)!=Consts.COL_DISPLAYTYPE_HIDDEN))
                    {
                        shouldDisplay=true;
                    }
                }else
                {
                    shouldDisplay|=clrcgbeanTmp.getMDynamicColGroupDisplayType(rrequest,mDynamicColGroupDisplayType);
                }
            }
        }
        mDynamicColGroupDisplayType.put(groupBean.getGroupid(),shouldDisplay);
        return shouldDisplay;
    }

    public void getRuntimeColGroupBeans(CrossListReportType crossListReportType,List lstAllRuntimeChildren,List<ColBean> lstAllRuntimeColBeans,
            Map<String,Boolean> mDynamicColGroupDisplayType)
    {
        UltraListReportGroupBean groupBean=(UltraListReportGroupBean)this.getOwner();
        ReportBean rbean=crossListReportType.getReportBean();
        ReportRequest rrequest=crossListReportType.getReportRequest();
        if(this.isDynamicColGroup())
        {//当前分组列是动态分组列
            if(!mDynamicColGroupDisplayType.get(groupBean.getGroupid())
                    &&(!this.hasDisplayStatisBeansOfReport(mDynamicColGroupDisplayType)||this.titleDatasetProvider.getLstConditions()==null||this.titleDatasetProvider
                            .getLstConditions().size()==0))
            {
                crossListReportType.addDynamicSelectCols(this,"");
                return;
            }
            List<Map<String,String>> lstDynamicColGroupLabelData=this.getDynColGroupLabelData(crossListReportType);
            if(lstDynamicColGroupLabelData==null)
            {
                crossListReportType.addDynamicSelectCols(this,"");
                return;
            }
            StringBuffer allDynColConditonsBuf=new StringBuffer();//所有动态标题列的条件字符串，用于横向针对整个报表进行统计时使用
            StringBuffer dynSelectedColsBuf=new StringBuffer();
            StringBuffer conditionBuf;
            Map<String,String> mCurrentGroupValues=new HashMap<String,String>();//用于存放每个<group/>上一个行的显示值，以便它们决定是否要新开一个GroupBean
            Map<String,String> mAllColConditionsInGroup=new HashMap<String,String>();//存放当前正在处理的每个分组包括的所有<col/>对应的条件,并用or拼凑在一起,以便对整个分组进行统计时能取到此分组的条件
            List lstDynChildren=new ArrayList();
            AbsReportDataPojo headDataObj=ReportAssistant.getInstance().getPojoClassInstance(rrequest,rbean,this.dataHeaderPojoClass);
            for(Map<String,String> mRowDataTmp:lstDynamicColGroupLabelData)
            {
                conditionBuf=new StringBuffer();
                getRuntimeGroupBeans(crossListReportType,mRowDataTmp,mCurrentGroupValues,mAllColConditionsInGroup,dynSelectedColsBuf,conditionBuf,
                        allDynColConditonsBuf,lstDynChildren,headDataObj,mDynamicColGroupDisplayType);
            }
            if(lstDynChildren.size()>0&&this.isStatisticCrossColGroup())
            {
                createStatisticForLastWholeGroup(crossListReportType,dynSelectedColsBuf,(UltraListReportGroupBean)lstDynChildren.get(lstDynChildren
                        .size()-1),mAllColConditionsInGroup,mDynamicColGroupDisplayType);
                
            }
            afterGetRuntimeColGroups(crossListReportType,mDynamicColGroupDisplayType,allDynColConditonsBuf,dynSelectedColsBuf,lstAllRuntimeChildren,
                    lstAllRuntimeColBeans,lstDynChildren,headDataObj);
        }else
        {//当前分组列是普通列（有动态子列）
            UltraListReportGroupBean groupBeanTmp=(UltraListReportGroupBean)groupBean.clone(rbean.getDbean());
            List lstChildrenTmp=new ArrayList();
            crossListReportType.getAllRuntimeColGroupBeans(groupBean.getLstChildren(),lstChildrenTmp,lstAllRuntimeColBeans,mDynamicColGroupDisplayType);
            if(lstChildrenTmp.size()>0)
            {
                groupBeanTmp.setLstChildren(lstChildrenTmp);
                lstAllRuntimeChildren.add(groupBeanTmp);
            }
        }
        
    }

    private void createStatisticForLastWholeGroup(CrossListReportType crossListReportType,StringBuffer dynSelectedColsBuf,
            UltraListReportGroupBean lastGroupBean,Map<String,String> mAllColConditionsInGroup,Map<String,Boolean> mDynamicColGroupDisplayType)
    {
        createStatisticForPrevWholeGroup(crossListReportType,dynSelectedColsBuf,lastGroupBean,mAllColConditionsInGroup,mDynamicColGroupDisplayType);
        if(this.childCrossColGroupBeans instanceof CrossListReportGroupBean)
        {
            List lstChildren=lastGroupBean.getLstChildren();
            for(int i=lstChildren.size()-1;i>=0;i--)
            {//从后到前取到最后一个子分组（这里要排除掉上面createStatisticForPrevWholeGroup()添加的针对本分组的整个分组进行统计的<col/>），构造对它整个分组的统计，中间分组列的整列统计在加载完它们时已经构造好不用在这里构造
                if(lstChildren.get(i) instanceof UltraListReportGroupBean)
                {
                    ((CrossListReportGroupBean)this.childCrossColGroupBeans).createStatisticForLastWholeGroup(crossListReportType,dynSelectedColsBuf,
                            (UltraListReportGroupBean)lstChildren.get(i),mAllColConditionsInGroup,mDynamicColGroupDisplayType);
                    break;
                }
            }
        }
    }
    
    public void getRealLabelValueFromResultset(ResultSet rs,Map<String,String> mRowData) throws SQLException
    {
        String myLabelValue=rs.getString(column);
        myLabelValue=myLabelValue==null?"":myLabelValue.trim();
        mRowData.put(column,myLabelValue);
        this.childCrossColGroupBeans.getRealLabelValueFromResultset(rs,mRowData);
    }
    
    private void getRuntimeGroupBeans(CrossListReportType crossListReportType,Map<String,String> mRowDataTmp,Map<String,String> mCurrentGroupValues,Map<String,String> mAllColConditionsInGroup,
            StringBuffer dynSelectedColsBuf,StringBuffer conditionBuf,StringBuffer allDynColConditonsBuf,List lstDynChildren,AbsReportDataPojo headDataObj,
            Map<String,Boolean> mDynamicColGroupDisplayType)
    {
        String myLabelValue=mRowDataTmp.get(column);
        myLabelValue=myLabelValue==null?"":myLabelValue.trim();
        List lstGroupChildren=null;
        if(myLabelValue.equals("")&&this.isCommonCrossColGroup())
        {//如果是普通动态列，且当前<group/>的label为空，则不生成这一级的<group/>
            mCurrentGroupValues.remove(column);//移掉这一个分组列的值，如果后面还有相同的group的label值，也要新建一个<group/>，因为这里插入了一个其它不属于此分组的列
            lstGroupChildren=lstDynChildren;
        }else
        {
            if(mDynamicColGroupDisplayType.get(this.getGroupBeanOwner().getGroupid()))
            {//此分组参与本次显示
                UltraListReportGroupBean groupBean=null;
                if(myLabelValue.equals(mCurrentGroupValues.get(column)))
                {
                    groupBean=(UltraListReportGroupBean)lstDynChildren.get(lstDynChildren.size()-1);
                }else
                {
                    mCurrentGroupValues.put(column,myLabelValue);
                    if(this.hasDisplayStatisBeans(mDynamicColGroupDisplayType))
                    {
                        if(lstDynChildren.size()>0)
                        {//对前一个分组的整个分组数据进行统计
                            groupBean=(UltraListReportGroupBean)lstDynChildren.get(lstDynChildren.size()-1);
                            createStatisticForPrevWholeGroup(crossListReportType,dynSelectedColsBuf,groupBean,mAllColConditionsInGroup,
                                    mDynamicColGroupDisplayType);
                        }
                    }
                    if(mAllColConditionsInGroup!=null) mAllColConditionsInGroup.remove(this.column);
                    int colidx=crossListReportType.generateColGroupIdxId();
                    groupBean=new UltraListReportGroupBean(crossListReportType.getReportBean().getDbean(),colidx);
                    if(headDataObj!=null)
                    {
                        groupBean.setLabel("_"+column+"_"+colidx);
                        headDataObj.setDynamicColData("_"+column+"_"+colidx,myLabelValue);
                    }else
                    {
                        groupBean.setLabel(myLabelValue);
                    }
                    groupBean.setLabelstyleproperty(this.getGroupBeanOwner().getLabelstyleproperty(crossListReportType.getReportRequest(),false),false);
                    groupBean.setLstChildren(new ArrayList());
                    groupBean.setRowspan(this.getGroupBeanOwner().getRowspan());
                    lstDynChildren.add(groupBean);
                }
                lstGroupChildren=groupBean.getLstChildren();
            }
            if(this.isStatisticCrossColGroup())
            {
                String realConditionTmp=getMyStatisticConditon(crossListReportType,myLabelValue);
                if(!realConditionTmp.trim().equals("")) conditionBuf.append(realConditionTmp).append(" and ");
            }
        }
        if(this.childCrossColGroupBeans instanceof CrossListReportGroupBean)
        {
            ((CrossListReportGroupBean)this.childCrossColGroupBeans).getRuntimeGroupBeans(crossListReportType,mRowDataTmp,mCurrentGroupValues,
                    mAllColConditionsInGroup,dynSelectedColsBuf,conditionBuf,allDynColConditonsBuf,lstGroupChildren,headDataObj,
                    mDynamicColGroupDisplayType);
        }else
        {
            ((CrossListReportColBean)this.childCrossColGroupBeans).getRuntimeColBeans(crossListReportType,mRowDataTmp,dynSelectedColsBuf,conditionBuf,
                    allDynColConditonsBuf,lstGroupChildren,headDataObj,mAllColConditionsInGroup,mDynamicColGroupDisplayType);
        }
    }

    private void createStatisticForPrevWholeGroup(CrossListReportType crossListReportType,StringBuffer dynSelectedColsBuf,
            UltraListReportGroupBean groupBean,Map<String,String> mAllColConditionsInGroup,Map<String,Boolean> mDynamicColGroupDisplayType)
    {
        if(this.lstDisplayStatisBeans==null||this.lstDisplayStatisBeans.size()==0) return;
        String groupConditions=mAllColConditionsInGroup.get(column);//此分组包括的所有生成的<col/>组合的条件
        if(groupConditions==null||groupConditions.trim().equals("")) return;
        groupConditions=groupConditions.trim();
        if(groupConditions.endsWith("or")) groupConditions=groupConditions.substring(0,groupConditions.length()-2);
        for(CrossListReportStatiDisplayBean statisdBeanTmp:this.lstDisplayStatisBeans)
        {//为每一个统计显示一个标题列，并且为它拼凑上查询的字段
            if(!mDynamicColGroupDisplayType.get(statisdBeanTmp.getStatiBean().getId())) continue;
            int colidx=crossListReportType.generateColGroupIdxId();
            groupBean.getLstChildren().add(
                    createDynamicCrossStatiColBean(crossListReportType.getReportBean().getDbean(),crossListReportType.getReportRequest()
                            .getI18NStringValue(statisdBeanTmp.getLabel()),statisdBeanTmp.getLabelstyleproperty(crossListReportType
                            .getReportRequest()),statisdBeanTmp.getValuestyleproperty(crossListReportType.getReportRequest()),statisdBeanTmp
                            .getStatiBean().getDatatypeObj(),colidx));
            dynSelectedColsBuf.append(statisdBeanTmp.getStatiBean().getType()+"(");
            dynSelectedColsBuf.append("case when ").append(groupConditions).append(" then ").append(statisdBeanTmp.getStatiBean().getColumn())
                    .append("  end ");
            dynSelectedColsBuf.append(") as ").append("column_"+colidx).append(",");
        }
    }
    
    
    public void processColGroupRelationStart()
    {
        super.processColGroupRelationStart();
        UltraListReportGroupBean groupBean=(UltraListReportGroupBean)this.getOwner();
        if(parentCrossGroupBean!=null&&parentCrossGroupBean.isDynamicColGroup())
        {
            if(this.column==null||this.column.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"的分组列"+groupBean.getLabel(null)
                        +"配置失败，其父分组列为动态列，则此分组列也必须是动态分组列，必须为其配置column属性，指定从哪个字段中取此分组的动态列头数据");
            }
        }
        if(this.isDynamicColGroup())
        {
            if(groupBean.getLstChildren().size()!=1)
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"的分组列"+groupBean.getLabel(null)
                        +"配置失败，此分组列是动态分组列，其下子能配置一个动态子分组列或动态子列");
            }
            this.childCrossColGroupBeans=ListReportAssistant.getInstance().getCrossColAndGroupBean(groupBean.getLstChildren().get(0));
        }
    }
    
    public void processColGroupRelationEnd()
    {
        if(this.isCommonCrossColGroup())
        {
            if(this.realvalue!=null&&!this.realvalue.trim().equals(""))
            {
                log.warn("报表"+this.getOwner().getReportBean().getPath()+"的列"+this.getLabel(null)+"为普通动态分组列，因为不需要查询它的显示数据，因此不需要配置realvalue属性");
                this.realvalue=null;
            }
        }
        super.processColGroupRelationEnd();
    }
    
    protected void getDynCols(List<Map<String,String>> lstDynCols)
    {
        super.getDynCols(lstDynCols);
        this.childCrossColGroupBeans.getDynCols(lstDynCols);
    }

    protected void initStatisDisplayBean(CrossListReportStatiBean statibean,List<String> lstStatitems)
    {
        if(statibean.getLstStatitems()!=null&&statibean.getLstStatitems().contains(this.column))
        {//此统计需要针对此分组进行横向统计
            if(this.lstDisplayStatisBeans==null) this.lstDisplayStatisBeans=new ArrayList<CrossListReportStatiDisplayBean>();
            this.lstDisplayStatisBeans.add(createStatisticDisplayBean(statibean,lstStatitems,this.column));
        }
        this.childCrossColGroupBeans.initStatisDisplayBean(statibean,lstStatitems);
    }
    
    
}
