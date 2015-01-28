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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.CrossListReportType;
import com.wabacus.system.component.application.report.configbean.UltraListReportGroupBean;
import com.wabacus.util.Consts;

public class CrossListReportColBean extends AbsCrossListReportColAndGroupBean
{
    private static Log log=LogFactory.getLog(CrossListReportColBean.class);
    
    private boolean hasVerticalstatistic;

    private String verticallabel;

    private Map<String,String> mDynVerticallableParts;
    
    private String verticallabelstyleproperty;

    private List<String> lstDynVerticallabelstylepropertyParts;//verticallabelstyleproperty中的动态部分，key为此动态值的在verticallabelstyleproperty中的占位符，值为request{xxx}、session{key}、url{key}等等形式，用于运行时得到真正值
    
    private boolean shouldShowStaticLabel;
    
    private int rowspan=1;//当本统计<col/>下面的统计的标题需要单独显示一行时，此<col/>即相当于一个<group/>，此时可以通过labelstyleproperty指定它的标题占据的行数
    
    private List<CrossListReportStatiBean> lstStatisBeans;//配置的<statistic/>对象集合

    private AbsCrossListReportColAndGroupBean belongToRootOwner;
    
    public CrossListReportColBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public void setHasVerticalstatistic(boolean hasVerticalstatistic)
    {
        this.hasVerticalstatistic=hasVerticalstatistic;
    }

    public boolean isHasVerticalstatistic()
    {
        return hasVerticalstatistic;
    }

    public void setVerticallabel(String verticallabel)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.getOwner().getPageBean(),verticallabel);
        this.verticallabel=(String)objArr[0];
        this.mDynVerticallableParts=(Map<String,String>)objArr[1];
    }

    public String getVerticallabel(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.verticallabel,this.mDynVerticallableParts,"");
    }

    public String getVerticallabelstyleproperty(ReportRequest rrequest,boolean isStaticPart)
    {
        if(isStaticPart) return this.verticallabelstyleproperty==null?"":this.verticallabelstyleproperty;
        return WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.verticallabelstyleproperty,
                this.lstDynVerticallabelstylepropertyParts,"");
    }

    public void setVerticallabelstyleproperty(String verticallabelstyleproperty,boolean isStaticPart)
    {
        if(isStaticPart)
        {
            this.verticallabelstyleproperty=verticallabelstyleproperty;
        }else
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(verticallabelstyleproperty);
            this.verticallabelstyleproperty=(String)objArr[0];
            this.lstDynVerticallabelstylepropertyParts=(List<String>)objArr[1];
        }
    }

    public void setLstStatisBeans(List<CrossListReportStatiBean> lstStatisBeans)
    {
        this.lstStatisBeans=lstStatisBeans;
    }

    public void setLstDisplayStatisBeans(List<CrossListReportStatiDisplayBean> lstDisplayStatisBeans)
    {
        this.lstDisplayStatisBeans=lstDisplayStatisBeans;
    }

    public boolean isShouldShowStaticLabel()
    {
        return this.isStatisticCrossColGroup()&&shouldShowStaticLabel;
    }

    public void setShouldShowStaticLabel(boolean shouldShowStaticLabel)
    {
        this.shouldShowStaticLabel=shouldShowStaticLabel;
    }

    public int getRowspan()
    {
        return rowspan;
    }

    public void setRowspan(int rowspan)
    {
        this.rowspan=rowspan;
    }

    public String getColumn()
    {
        return ((ColBean)this.getOwner()).getColumn();
    }

    public String getLabel(ReportRequest rrequest)
    {
        return ((ColBean)this.getOwner()).getLabel(rrequest);
    }
    
    public CrossListReportColBean getInnerDynamicColBean()
    {
        return this;
    }

    public AbsCrossListReportColAndGroupBean getBelongToRootOwner()
    {
        return belongToRootOwner;
    }

    public void setBelongToRootOwner(AbsCrossListReportColAndGroupBean belongToRootOwner)
    {
        this.belongToRootOwner=belongToRootOwner;
    }

    public boolean hasDynamicColGroupChild()
    {
        if(isDynamicColGroup()) return true;
        return false;
    }
    
    protected List<CrossListReportStatiBean> getLstStatisBeans()
    {
        if(!this.isStatisticCrossColGroup()) return null;
        return this.lstStatisBeans;
    }
    
    private ColBean getCbeanOwner()
    {
        return (ColBean)this.getOwner();
    }
    
    public boolean getMDynamicColGroupDisplayType(ReportRequest rrequest,Map<String,Boolean> mDynamicColGroupDisplayType)
    {
        if(!this.isDynamicColGroup()) return true;
        ColBean cbeanOwner=(ColBean)this.getOwner();
        if(this.lstStatisBeans!=null&&this.lstStatisBeans.size()>0)
        {
            boolean isAllStatisticItemsHidden=true;
            for(CrossListReportStatiBean cslsbeanTmp:this.lstStatisBeans)
            {
                if(!rrequest.checkPermission(cbeanOwner.getReportBean().getId(),Consts.DATA_PART,cslsbeanTmp.getId(),Consts.PERMISSION_TYPE_DISPLAY))
                {//在运行时被授权为不显示
                    mDynamicColGroupDisplayType.put(cslsbeanTmp.getId(),false);
                }else
                {
                    mDynamicColGroupDisplayType.put(cslsbeanTmp.getId(),true);
                    isAllStatisticItemsHidden=false;
                }
            }
            if(isAllStatisticItemsHidden)
            {
                mDynamicColGroupDisplayType.put(cbeanOwner.getColid(),false);
                return false;
            }
        }
        if(!rrequest.checkPermission(cbeanOwner.getReportBean().getId(),Consts.DATA_PART,cbeanOwner.getColumn(),Consts.PERMISSION_TYPE_DISPLAY))
        {
            mDynamicColGroupDisplayType.put(cbeanOwner.getColid(),false);
            return false;
        }
        boolean shouldDisplay;
        if(this.isCommonCrossColGroup())
        {
            shouldDisplay=true;
        }else
        {
            shouldDisplay=hasDisplayStatisBeans(mDynamicColGroupDisplayType);
        }
        mDynamicColGroupDisplayType.put(cbeanOwner.getColid(),shouldDisplay);
        return shouldDisplay;
    }
    
    public void getRuntimeColGroupBeans(CrossListReportType crossListReportType,List lstAllRuntimeDisplayChildren,List<ColBean> lstAllRuntimeColBeans,
            Map<String,Boolean> mDynamicColGroupDisplayType)
    {
        ColBean cbOwner=(ColBean)this.getOwner();
        if(this.isDynamicColGroup())
        {//当前列是动态列
            if(!mDynamicColGroupDisplayType.get(cbOwner.getColid())
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
            List lstDynChildren=new ArrayList();
            StringBuffer allDynColConditonsBuf=new StringBuffer();
            StringBuffer dynSelectedColsBuf=new StringBuffer();//存放查询动态统计数据的字段列表
            StringBuffer conditionBuf;
            AbsReportDataPojo headDataObj=ReportAssistant.getInstance().getPojoClassInstance(crossListReportType.getReportRequest(),
                    crossListReportType.getReportBean(),this.dataHeaderPojoClass);
            for(Map<String,String> mRowDataTmp:lstDynamicColGroupLabelData)
            {
                conditionBuf=new StringBuffer();
                getRuntimeColBeans(crossListReportType,mRowDataTmp,dynSelectedColsBuf,conditionBuf,allDynColConditonsBuf,lstDynChildren,headDataObj,
                        null,mDynamicColGroupDisplayType);
            }
            afterGetRuntimeColGroups(crossListReportType,mDynamicColGroupDisplayType,allDynColConditonsBuf,dynSelectedColsBuf,lstAllRuntimeDisplayChildren,
                    lstAllRuntimeColBeans,lstDynChildren,headDataObj);
        }else
        {
            lstAllRuntimeColBeans.add(cbOwner);
            if(cbOwner.getDisplaytype(crossListReportType.getReportRequest().getShowtype()==Consts.DISPLAY_ON_PAGE)!=Consts.COL_DISPLAYTYPE_HIDDEN)
            {
                lstAllRuntimeDisplayChildren.add(cbOwner);
            }
        }
    }

    public void getRealLabelValueFromResultset(ResultSet rs,Map<String,String> mRowData) throws SQLException
    {
        String column=this.getCbeanOwner().getColumn();
        String myLabelValue=rs.getString(column);
        myLabelValue=myLabelValue==null?"":myLabelValue.trim();
        mRowData.put(column,myLabelValue);
        if(this.isCommonCrossColGroup()&&this.realvalue!=null&&!this.realvalue.trim().equals(""))
        {
            mRowData.put(this.realvalue,rs.getString(this.realvalue));
        }
    }
    
    protected void getRuntimeColBeans(CrossListReportType crossListReportType,Map<String,String> mRowDataTmp,StringBuffer dynselectedColsBuf,StringBuffer conditionBuf,
            StringBuffer allColConditionsBuf,List lstChildren,AbsReportDataPojo headDataObj,
            Map<String,String> mAllColConditionsInGroup,Map<String,Boolean> mDynamicColGroupDisplayType)
    {
        String column=this.getCbeanOwner().getColumn();
        String colLabelValue=mRowDataTmp.get(column);
        colLabelValue=colLabelValue==null?"":colLabelValue.trim();
        if(this.isStatisticCrossColGroup())
        {
            String realConditionTmp=getMyStatisticConditon(crossListReportType,colLabelValue);
            if(!realConditionTmp.trim().equals("")) conditionBuf.append(realConditionTmp);
            if(conditionBuf.length()>0)
            {
                if(allColConditionsBuf!=null)
                {//收集所有生成<col/>对应的条件，稍后针对整个报表的横向统计会用上
                    allColConditionsBuf.append("(").append(conditionBuf.toString()).append(") or ");
                }
                if(mAllColConditionsInGroup!=null)
                {//需要收集各分组包括的所有<col/>对应的条件
                    CrossListReportGroupBean parentGroupBean=this.parentCrossGroupBean;
                    while(parentGroupBean!=null&&parentGroupBean.isDynamicColGroup())
                    {//给所有包含此列的分组加上此列的条件，这样统计相应分组时，就可以精确包含要统计的条件
                        String colConditions=mAllColConditionsInGroup.get(parentGroupBean.getColumn());
                        if(colConditions==null) colConditions="";
                        colConditions=colConditions+"("+conditionBuf.toString()+") or ";
                        mAllColConditionsInGroup.put(parentGroupBean.getColumn(),colConditions);
                        parentGroupBean=parentGroupBean.parentCrossGroupBean;
                    }
                }
            }
        }
        if(!mDynamicColGroupDisplayType.get(this.getCbeanOwner().getColid())) return;//没有显示权限，则不用构造相应的col/group
        DisplayBean disbean=crossListReportType.getReportBean().getDbean();
        ReportRequest rrequest=crossListReportType.getReportRequest();
        if(this.isShouldShowStaticLabel())
        {
            int colidx=crossListReportType.generateColGroupIdxId();
            UltraListReportGroupBean groupBean=new UltraListReportGroupBean(disbean,colidx);
            groupBean.setLabel(getDynamicLabel(headDataObj,column,colLabelValue,colidx));
            groupBean.setLabelstyleproperty(this.getCbeanOwner().getLabelstyleproperty(crossListReportType.getReportRequest(),false),false);
            groupBean.setRowspan(this.rowspan);
            List lstColChildren=new ArrayList();
            groupBean.setLstChildren(lstColChildren);
            for(CrossListReportStatiDisplayBean statisdBeanTmp:this.lstDisplayStatisBeans)
            {
                if(!mDynamicColGroupDisplayType.get(statisdBeanTmp.getStatiBean().getId())) continue;
                colidx=crossListReportType.generateColGroupIdxId();
                lstColChildren.add(createDynamicCrossStatiColBean(disbean,rrequest.getI18NStringValue(statisdBeanTmp.getLabel()),statisdBeanTmp
                        .getLabelstyleproperty(rrequest),statisdBeanTmp.getValuestyleproperty(rrequest),statisdBeanTmp.getStatiBean().getDatatypeObj(),colidx));
                createStatisticColumn(dynselectedColsBuf,conditionBuf,statisdBeanTmp,colidx);
            }
            lstChildren.add(groupBean);
            rrequest.setAttribute(crossListReportType.getReportBean().getId(),"WX_IS_HAS_GROUP_CONFIG","true");//标识此报表有<group/>配置，（因为可能本报表配置时没有配置<group/>，但在这里新增了<group/>，所以要标识一下，方便显示标题时判断）
        }else
        {
            int colidx=crossListReportType.generateColGroupIdxId();
            if(this.isStatisticCrossColGroup())
            {//如果是统计数据，则用<statistic/>中配置的数据类型
                CrossListReportStatiDisplayBean statisdBeanTmp=this.lstDisplayStatisBeans.get(0);//不显示统计标题的情况下，只有一个统计项，且没有配置label
                createStatisticColumn(dynselectedColsBuf,conditionBuf,statisdBeanTmp,colidx);
                lstChildren.add(createDynamicCrossStatiColBean(disbean,getDynamicLabel(headDataObj,column,colLabelValue,colidx),statisdBeanTmp
                        .getLabelstyleproperty(rrequest),statisdBeanTmp.getValuestyleproperty(rrequest),statisdBeanTmp.getStatiBean().getDatatypeObj(),colidx));
            }else
            {//如果是普通动态列，则用<col/>中配置的数据类型
                String colcolumn=this.realvalue;
                if(colcolumn==null||colcolumn.trim().equals("")) colcolumn=this.getColumn();
                colcolumn=mRowDataTmp.get(colcolumn);
                dynselectedColsBuf.append(colcolumn).append(",");
                lstChildren.add(createDynamicCommonColBean(crossListReportType.getReportRequest(),disbean,getDynamicLabel(headDataObj,column,colLabelValue,colidx),colidx,colcolumn));
            }
        }
    }
    
    private ColBean createDynamicCommonColBean(ReportRequest rrequest,DisplayBean disbean,String label,int colidx,String colcolumn)
    {
        ColBean cbConfig=this.getCbeanOwner();
        ColBean cbResult=new ColBean(disbean,colidx);
        cbResult.setLabel(label);
        String belongDsid=this.getDatasetBean().getId();
        List<String> lstDsids=null;
        if(belongDsid!=null&&!belongDsid.trim().equals(""))
        {
            lstDsids=new ArrayList<String>();
            lstDsids.add(belongDsid);
        }
        cbResult.setLstDatasetValueids(lstDsids);
        cbResult.setLabelstyleproperty(cbConfig.getLabelstyleproperty(rrequest,false),true);
        cbResult.setValuestyleproperty(cbConfig.getValuestyleproperty(rrequest,false),true);
        cbResult.setDatatypeObj(cbConfig.getDatatypeObj());
        cbResult.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
        cbResult.setProperty("[DYN_COL_DATA]");
        cbResult.setColumn(colcolumn);
        CrossListReportColBean crcbeanTmp=new CrossListReportColBean(cbResult);
        crcbeanTmp.setBelongToRootOwner(this.getRootCrossColGroupBean());
        cbResult.setExtendConfigDataForReportType(CrossListReportType.KEY,crcbeanTmp);
        return cbResult;
    }

    private void createStatisticColumn(StringBuffer dynselectedColsBuf,StringBuffer conditionBuf,CrossListReportStatiDisplayBean statisdBeanTmp,
            int colidx)
    {
        dynselectedColsBuf.append(statisdBeanTmp.getStatiBean().getType()+"(");
        dynselectedColsBuf.append("case when ").append(conditionBuf.toString()).append(" then ")
                .append(statisdBeanTmp.getStatiBean().getColumn()).append("  end ");
        dynselectedColsBuf.append(") as ").append("column_"+colidx).append(",");
    }

    private String getDynamicLabel(AbsReportDataPojo headDataObj,String column,String colLabelValue,int colidx)
    {
        if(headDataObj!=null)
        {
            headDataObj.setDynamicColData("_"+column+"_"+colidx,colLabelValue);
            colLabelValue="_"+column+"_"+colidx;
        }
        return colLabelValue;
    }
    
    public void processColGroupRelationStart()
    {
        super.processColGroupRelationStart();
        ColBean colbean=(ColBean)this.getOwner();
        if(this.isDynamicColGroup())
        {
            if(colbean.isControlCol()||colbean.isNonFromDbCol()||colbean.isSequenceCol())
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()
                        +"的列"+this.getLabel(null)+"配置失败，此列不是从数据库取数据的列，不能将其配置为动态列");
            }
            colbean.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});//动态统计列的displaytype属性统一为永远显示
            if(this.lstStatisBeans!=null&&this.lstStatisBeans.size()>0)
            {
                this.setDynColGroupSpecificBean(new CrossStatisticColGroupBean());
            }else
            {
                this.setDynColGroupSpecificBean(new CommonCrossColGroupBean());
            }
        }else if(this.lstStatisBeans!=null&&this.lstStatisBeans.size()>0)
        {
            throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"的列"+this.getLabel(null)
                    +"配置失败，此列不是动态列，不能为其配置<statatic/>标签进行交叉统计");
        }
    }

    public void processColGroupRelationEnd()
    {
        if(this.isCommonCrossColGroup())
        {
            if(this.realvalue==null||this.realvalue.trim().equals(""))
            {
                log.warn("报表"+this.getOwner().getReportBean().getPath()+"的列"+this.getLabel(null)
                        +"为普通动态列，没有配置realvalue属性指定查询本动态列数据的字段名，将使用column属性指定的字段名");
                this.realvalue=this.getColumn();
            }else if(this.realvalue.indexOf(".")>0)
            {
                this.realvalue=this.realvalue.substring(this.realvalue.indexOf(".")+1).trim();
            }
        }
        super.processColGroupRelationEnd();
    }
    
    protected void initStatisDisplayBean(CrossListReportStatiBean statibean,List<String> lstStatitems)
    {
        String column=((ColBean)this.getOwner()).getColumn();
        if(statibean.getLstStatitems()==null||statibean.getLstStatitems().size()==0//没有配置statitems，则默认就统计最里层的<col/>
                ||statibean.getLstStatitems().contains(column))
        {//此统计需要针对此分组进行横向统计（这里必须用，不能用lstStatitems，因为lstStatitems会边处理边删除，所以如果用lstStatitems，则在这里判断lstStatitems==null||lstStatitems.size()==0不准确）
            if(statibean.getLstStatitems()==null||statibean.getLstStatitems().size()==0)
            {//没有配置statitems，则只统计最里层的<col/>，所以取label、labelstyleproperty、valuestyleproperty等时只取第一个，即使配置了多个
                column=null;
            }
            if(this.lstDisplayStatisBeans==null) this.lstDisplayStatisBeans=new ArrayList<CrossListReportStatiDisplayBean>();
            this.lstDisplayStatisBeans.add(createStatisticDisplayBean(statibean,lstStatitems,column));
        }
    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        CrossListReportColBean beanNew=(CrossListReportColBean)super.clone(owner);
        if(lstStatisBeans!=null)
        {
            List<CrossListReportStatiBean> lstStatisBeansNew=new ArrayList<CrossListReportStatiBean>();
            for(CrossListReportStatiBean beanTmp:lstStatisBeans)
            {
                lstStatisBeansNew.add((CrossListReportStatiBean)beanTmp.clone(owner));
            }
            beanNew.setLstStatisBeans(lstStatisBeansNew);
        }
        return beanNew;
    }   
}
