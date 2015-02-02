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
package com.wabacus.system.component.application.report.configbean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.UltraListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class UltraListReportGroupBean extends AbsConfigBean
{
    private static Log log=LogFactory.getLog(UltraListReportGroupBean.class);

    private String groupid;

    private String parentGroupid;

    private String childids;

    private String label;
    
    private Map<String,String> mDynLableParts;//显示<group/>对应<td/>的样式字符串

    private String labelstyleproperty;
    
    private List<String> lstDynLabelstylepropertyParts;//labelstyleproperty中的动态部分，key为此动态值的在labelstyleproperty中的占位符，值为request{xxx}、session{key}、url{key}等等形式，用于运行时得到真正值

    private int rowspan=1;

    private List lstChildren;

    public UltraListReportGroupBean(AbsConfigBean owner)
    {
        super(owner);
        this.groupid="group_"+((DisplayBean)owner).generate_childid();
    }

    public UltraListReportGroupBean(AbsConfigBean owner,int groupid)
    {
        super(owner);
        this.groupid="group_"+groupid;
    }

    public String getGroupid()
    {
        return groupid;
    }

    public void setGroupid(String groupid)
    {
        this.groupid=groupid;
    }

    public String getParentGroupid()
    {
        return parentGroupid;
    }

    public void setParentGroupid(String parentGroupid)
    {
        this.parentGroupid=parentGroupid;
    }

    public String getLabel(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.label,this.mDynLableParts,"");
    }

    public void setLabel(String label)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.getPageBean(),label);
        this.label=(String)objArr[0];
        this.mDynLableParts=(Map<String,String>)objArr[1];
    }

    public String getLabelstyleproperty(ReportRequest rrequest,boolean isStaticPart)
    {
        if(isStaticPart) return this.labelstyleproperty==null?"":this.labelstyleproperty;
        return WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.labelstyleproperty,this.lstDynLabelstylepropertyParts,"");
    }

    public void setLabelstyleproperty(String labelstyleproperty,boolean isStaticPart)
    {
        if(isStaticPart)
        {
            this.labelstyleproperty=labelstyleproperty;
        }else
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(labelstyleproperty);
            this.labelstyleproperty=(String)objArr[0];
            this.lstDynLabelstylepropertyParts=(List<String>)objArr[1];
        }
    }

    public int getRowspan()
    {
        return rowspan;
    }

    public void setRowspan(int rowspan)
    {
        if(rowspan<=0) rowspan=1;
        this.rowspan=rowspan;
    }

    public String getChildids()
    {
        return childids;
    }

    public void setChildids(String childids)
    {
        this.childids=childids;
    }

    public List getLstChildren()
    {
        return lstChildren;
    }

    public void setLstChildren(List lstChildren)
    {
        this.lstChildren=lstChildren;
    }

    public void createColAndGroupDisplayBeans(UltraListReportType reportTypeObj,Map<String,String> mDisplayRealColAndGroupLabels,
            ReportRequest rrequest,List<String> lstDynColids,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,
            List<ColAndGroupDisplayBean> lstColAndGroupDisplayBeans,boolean isForPage)
    {
        ColAndGroupDisplayBean cgDisplayBeanTmp;
        ColBean cbTmp;
        UltraListReportGroupBean ulgroupbeanTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        UltraListReportColBean ulcbTmp;
        List lstChildrenTmp=reportTypeObj.sortChildrenByDynColOrders(lstChildren,lstDynColids,mColAndGroupTitlePostions);
        AbsListReportColBean alrcbean=null;
        String labelTmp;
        for(Object objTmp:lstChildrenTmp)
        {
            cgDisplayBeanTmp=new ColAndGroupDisplayBean();
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(positionBeanTmp.getDisplaymode()<0) continue;//当前列没有显示权限
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbTmp.getDisplaytype(isForPage))) continue;
                cgDisplayBeanTmp.setId(cbTmp.getColid());
                cgDisplayBeanTmp.setControlCol(cbTmp.isControlCol());
                alrcbean=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                cgDisplayBeanTmp.setNonFixedCol(alrcbean==null||!alrcbean.isFixedCol(rrequest));
                cgDisplayBeanTmp.setAlways(positionBeanTmp.getDisplaymode()==2);
                cgDisplayBeanTmp.setChecked(positionBeanTmp.getDisplaymode()>0);
                ulcbTmp=(UltraListReportColBean)cbTmp.getExtendConfigDataForReportType(UltraListReportType.KEY);
                cgDisplayBeanTmp.setParentGroupId(ulcbTmp.getParentGroupid());
                cgDisplayBeanTmp.setLayer(positionBeanTmp.getLayer());
                labelTmp=mDisplayRealColAndGroupLabels.get(cbTmp.getColid());
                if(Tools.isEmpty(labelTmp)) labelTmp=cbTmp.getLabel(rrequest);
                cgDisplayBeanTmp.setTitle(labelTmp);
                lstColAndGroupDisplayBeans.add(cgDisplayBeanTmp);
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                ulgroupbeanTmp=(UltraListReportGroupBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(ulgroupbeanTmp.getGroupid());
                if(positionBeanTmp.getDisplaymode()<0) continue;
                cgDisplayBeanTmp.setId(ulgroupbeanTmp.getGroupid());
                cgDisplayBeanTmp.setChildIds(ulgroupbeanTmp.getChildids());
                cgDisplayBeanTmp.setAlways(positionBeanTmp.getDisplaymode()==2);
                cgDisplayBeanTmp.setChecked(positionBeanTmp.getDisplaymode()>0);
                cgDisplayBeanTmp.setChildIds(ulgroupbeanTmp.getChildids());
                cgDisplayBeanTmp.setParentGroupId(ulgroupbeanTmp.getParentGroupid());
                cgDisplayBeanTmp.setLayer(positionBeanTmp.getLayer());
                labelTmp=mDisplayRealColAndGroupLabels.get(ulgroupbeanTmp.getGroupid());
                if(Tools.isEmpty(labelTmp)) labelTmp=ulgroupbeanTmp.getLabel(rrequest);
                cgDisplayBeanTmp.setTitle(labelTmp);
                lstColAndGroupDisplayBeans.add(cgDisplayBeanTmp);
                ulgroupbeanTmp.createColAndGroupDisplayBeans(reportTypeObj,mDisplayRealColAndGroupLabels,rrequest,lstDynColids,
                        mColAndGroupTitlePostions,lstColAndGroupDisplayBeans,isForPage);
            }
        }
    }

    public boolean hasRowgroupChildCol()
    {
        Object childObj=lstChildren.get(0);
        boolean hasRowGroupCol=false;
        if(childObj instanceof ColBean)
        {
            ColBean cbeanTmp=((ColBean)childObj);
            AbsListReportColBean alrcbean=(AbsListReportColBean)cbeanTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
            hasRowGroupCol=alrcbean.isRowgroup();
        }else if(childObj instanceof UltraListReportGroupBean)
        {
            hasRowGroupCol=((UltraListReportGroupBean)childObj).hasRowgroupChildCol();
        }
        return hasRowGroupCol;
    }

    public boolean hasFixedChildCol(ReportRequest rrequest)
    {
        Object childObj=lstChildren.get(0);
        boolean result=false;
        if(childObj instanceof ColBean)
        {
            ColBean cbeanTmp=((ColBean)childObj);
            AbsListReportColBean alrcbean=(AbsListReportColBean)cbeanTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
            result=alrcbean!=null&&alrcbean.isFixedCol(rrequest);
        }else if(childObj instanceof UltraListReportGroupBean)
        {
            result=((UltraListReportGroupBean)childObj).hasFixedChildCol(rrequest);
        }
        return result;
    }

    public boolean isDragable(AbsListReportDisplayBean alrdbean)
    {
        if(this.hasFixedChildCol(null)) return false;
        if(alrdbean==null||alrdbean.getRowgrouptype()<=0||alrdbean.getRowGroupColsNum()<=0) return true;
        if(this.hasRowgroupChildCol()) return false;
        return true;
    }

    public void getAllChildColIdsInerit(List<String> lstColIds)
    {
        Object obj;
        for(int i=0;i<lstChildren.size();i++)
        {
            obj=lstChildren.get(i);
            if(obj==null) continue;
            if(obj instanceof ColBean)
            {
                lstColIds.add(((ColBean)obj).getColid());
            }else
            {
                ((UltraListReportGroupBean)obj).getAllChildColIdsInerit(lstColIds);
            }
        }
    }

    public boolean containsChild(String childid,boolean inherit)
    {
        if(childid==null||childid.trim().equals("")) return false;
        for(Object childTmp:lstChildren)
        {
            if(childTmp==null) continue;
            if(childTmp instanceof ColBean)
            {
                if(childid.equals((((ColBean)childTmp).getColid())))
                {
                    return true;
                }
            }else
            {
                if(childid.equals(((UltraListReportGroupBean)childTmp).getGroupid()))
                {
                    return true;
                }
                if(inherit)
                {
                    boolean isExist=((UltraListReportGroupBean)childTmp).containsChild(childid,inherit);
                    if(isExist) return true;
                }
            }
        }
        return false;
    }

    public UltraListReportGroupBean getGroupBeanById(String groupid)
    {
        if(groupid==null||groupid.trim().equals("")) return null;
        if(lstChildren!=null&&lstChildren.size()>0)
        {
            Object obj;
            for(int i=0;i<lstChildren.size();i++)
            {
                obj=lstChildren.get(i);
                if(obj==null||obj instanceof ColBean) continue;
                if(groupid.equals(((UltraListReportGroupBean)obj).getGroupid())) return (UltraListReportGroupBean)obj;
                obj=((UltraListReportGroupBean)obj).getGroupBeanById(groupid);
                if(obj!=null) return (UltraListReportGroupBean)obj;
            }
        }
        return null;
    }

    public boolean removeChildColBeanByColumn(String column,boolean inherit)
    {
        if(column==null||column.trim().equals("")) return false;
        if(lstChildren==null||lstChildren.size()==0) return false;
        boolean result=false;
        Object obj=null;
        for(int i=lstChildren.size()-1;i>=0;i--)
        {
            obj=lstChildren.get(i);
            if(obj==null) continue;
            if(obj instanceof ColBean)
            {
                if(((ColBean)obj).getColumn().equals(column))
                {
                    lstChildren.remove(i);
                    result=true;
                }
            }else if(obj instanceof UltraListReportGroupBean)
            {
                if(inherit)
                {
                    boolean flag=((UltraListReportGroupBean)obj).removeChildColBeanByColumn(column,true);
                    if(flag)
                    {//成功删除了column对应的列
                        if(((UltraListReportGroupBean)obj).getLstChildren()==null||((UltraListReportGroupBean)obj).getLstChildren().size()==0)
                        {
                            lstChildren.remove(i);
                        }
                        result=true;
                    }
                }
            }
        }
        return result;
    }

    public String getFirstColId(List<String> lstNewOrderChildCols)
    {
        if(lstNewOrderChildCols==null||lstNewOrderChildCols.size()==0)
        {
            Object childObj=lstChildren.get(0);
            if(childObj instanceof ColBean)
            {
                return ((ColBean)childObj).getColid();
            }else
            {
                return ((UltraListReportGroupBean)childObj).getFirstColId(null);
            }
        }else
        {
            List<String> lstAllChildColIds=new ArrayList<String>();
            this.getAllChildColIdsInerit(lstAllChildColIds);
            for(String colidTmp:lstNewOrderChildCols)
            {
                if(lstAllChildColIds.contains(colidTmp))
                {
                    return colidTmp;
                }
            }
        }
        return null;
    }

    public String getLastColId(List<String> lstNewOrderChildCols)
    {
        if(lstNewOrderChildCols==null||lstNewOrderChildCols.size()==0)
        {
            Object childObj=lstChildren.get(lstChildren.size()-1);
            if(childObj instanceof ColBean)
            {
                return ((ColBean)childObj).getColid();
            }else
            {
                return ((UltraListReportGroupBean)childObj).getLastColId(null);
            }
        }else
        {
            List<String> lstAllChildColIds=new ArrayList<String>();
            this.getAllChildColIdsInerit(lstAllChildColIds);
            String colidTmp;
            for(int i=lstNewOrderChildCols.size()-1;i>=0;i--)
            {
                colidTmp=lstNewOrderChildCols.get(i);
                if(lstAllChildColIds.contains(colidTmp))
                {
                    return colidTmp;
                }
            }
        }
        return null;
    }

    public void getAllColBeans(List<ColBean> lstColBeans,Map<String,ColAndGroupTitlePositionBean> mPositions)
    {
        ColAndGroupTitlePositionBean cgpositionBeanTmp;
        for(Object childObj:lstChildren)
        {
            if(childObj instanceof ColBean)
            {
                if(mPositions!=null)
                {
                    cgpositionBeanTmp=mPositions.get(((ColBean)childObj).getColid());
                    if(cgpositionBeanTmp!=null&&cgpositionBeanTmp.getDisplaymode()<=0) continue;
                }
                lstColBeans.add((ColBean)childObj);
            }else
            {
                ((UltraListReportGroupBean)childObj).getAllColBeans(lstColBeans,mPositions);
            }
        }
    }

    public int calColSpans()
    {
        int colspans=0;
        for(Object childObj:lstChildren)
        {
            if(childObj instanceof ColBean)
            {
                colspans++;
            }else
            {
                colspans+=((UltraListReportGroupBean)childObj).calColSpans();
            }
        }
        if(labelstyleproperty!=null&&!labelstyleproperty.trim().equals(""))
        {
            String value=Tools.getPropertyValueByName("colspan",labelstyleproperty,true);
            try
            {
                if(Integer.parseInt(value)>1)
                {
                    log.warn("对于复杂标题的报表，最好不要配置其colspan和rowspan，否则可能会干扰报表的正常显示，可采用width和height来控制宽度和高度");
                }
            }catch(Exception e)
            {
            }
        }
        if(colspans>1)
        {
            if(labelstyleproperty==null) labelstyleproperty="";
            labelstyleproperty="  colspan=\""+colspans+"\" "+labelstyleproperty;
        }
        return colspans;
    }

    public int[] calPositionStart(ReportRequest rrequest,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,
            List<String> lstDisplayColids,boolean isForPage)
    {
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(this.groupid);
        if(positionBean==null)
        {
            positionBean=new ColAndGroupTitlePositionBean();
            mColAndGroupTitlePostions.put(this.groupid,positionBean);
        }
        ColAndGroupTitlePositionBean positionBeanTmp;
        ColBean cbTmp;
        UltraListReportGroupBean groupBeanTmp;
        boolean hasGroupChild=false;
        boolean isAllChildNonDisplayPermission=true;
        boolean containsAlwaysCol=false;
        int maxrowspan=0,colspan=0;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(positionBeanTmp==null)
                {
                    positionBeanTmp=new ColAndGroupTitlePositionBean();
                    mColAndGroupTitlePostions.put(cbTmp.getColid(),positionBeanTmp);
                }
                positionBeanTmp.setDisplaymode(cbTmp.getDisplaymode(rrequest,lstDisplayColids,isForPage));
                if(positionBeanTmp.getDisplaymode()>=0)
                {
                    if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbTmp.getDisplaytype(isForPage))) isAllChildNonDisplayPermission=false;//说明此分组存在没有授权为不显示的列且不是displaytype为hidden的列
                    if(positionBeanTmp.getDisplaymode()>0) colspan++;
                    if(positionBeanTmp.getDisplaymode()==2) containsAlwaysCol=true;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                int[] spans=groupBeanTmp.calPositionStart(rrequest,mColAndGroupTitlePostions,lstDisplayColids,isForPage);
                positionBeanTmp=mColAndGroupTitlePostions.get(groupBeanTmp.getGroupid());
                if(positionBeanTmp.getDisplaymode()>=0)
                {
                    isAllChildNonDisplayPermission=false;
                    if(positionBeanTmp.getDisplaymode()>0)
                    {
                        colspan+=spans[0];
                        if(spans[1]>maxrowspan) maxrowspan=spans[1];
                        hasGroupChild=true;
                        if(positionBeanTmp.getDisplaymode()==2) containsAlwaysCol=true;
                    }
                }
            }
        }
        if(isAllChildNonDisplayPermission)
        {//所有子列都被授权为不显示，或者为hidden的列，则此分组列也不显示（即不出现在列选择选项框中）
            positionBean.setDisplaymode(-1);
        }else if(colspan==0)
        {
            positionBean.setDisplaymode(0);
        }else if(colspan>0)
        {
            if(containsAlwaysCol)
            {
                positionBean.setDisplaymode(2);
            }else
            {
                positionBean.setDisplaymode(1);
            }
        }
        if(positionBean.getDisplaymode()>0)
        {
            positionBean.setColspan(colspan);
            maxrowspan=maxrowspan+this.rowspan;
            if(!hasGroupChild)
            {//如果没有子分组节点，即子节点全部是普通列<col/>，则本分组节点总行数要再加1即包括其所有子<col/>所占的那一行
                maxrowspan=maxrowspan+1;
            }
        }else
        {
            colspan=0;
            maxrowspan=0;
        }
        return new int[] { colspan, maxrowspan };
    }

    public void calPositionEnd(Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,int[] position)
    {
        int totalrowspan=position[0];
        int layer=position[1];
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(this.groupid);
        if(positionBean.getDisplaymode()<0) return;
        positionBean.setLayer(layer);//不管当前列是否需要显示，都必须设置layer，因为在显示动态列选择框时，需要根据此layer组织各列的层级关系
        if(positionBean.getDisplaymode()>0)
        {
            positionBean.setRowspan(this.rowspan);
        }
        ColAndGroupTitlePositionBean positionBeanTmp;
        ColBean cbTmp;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                positionBeanTmp.setLayer(layer+1);
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    positionBeanTmp.setRowspan(totalrowspan-this.rowspan);
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                ((UltraListReportGroupBean)objTmp).calPositionEnd(mColAndGroupTitlePostions,new int[] { totalrowspan-this.rowspan, layer+1 });
            }
        }
    }

    public void calPositionForStandardExcel(UltraListReportType reportTypeObj,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,
            List<String> lstDynColids,int[] startcolrowidx)
    {
        ColAndGroupTitlePositionBean positionBean=mColAndGroupTitlePostions.get(this.groupid);
        if(positionBean.getDisplaymode()<=0||lstChildren==null||lstChildren.size()==0)
        {
            return;
        }
        int startrowidx=startcolrowidx[0];
        int startcolidx=startcolrowidx[1];//起始列号
        positionBean.setStartcolindex(startcolidx);
        positionBean.setStartrowindex(startrowidx);
        List lstChildrenTemp=reportTypeObj.sortChildrenByDynColOrders(lstChildren,lstDynColids,mColAndGroupTitlePostions);
        ColBean cbTmp;
        UltraListReportGroupBean groupBeanTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(Object objTmp:lstChildrenTemp)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    positionBeanTmp.setStartcolindex(startcolidx);
                    positionBeanTmp.setStartrowindex(startrowidx+this.rowspan);
                    startcolidx++;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(groupBeanTmp.getGroupid());
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    groupBeanTmp.calPositionForStandardExcel(reportTypeObj,mColAndGroupTitlePostions,lstDynColids,new int[] {
                            startrowidx+this.rowspan, startcolidx });
                    startcolidx+=positionBeanTmp.getColspan();
                }
            }
        }
    }

    public AbsConfigBean clone(AbsConfigBean parent)
    {
        DisplayBean disbean=(DisplayBean)parent;
        UltraListReportGroupBean groupBeanNew=(UltraListReportGroupBean)super.clone(parent);
        if(this.lstChildren!=null&&this.lstChildren.size()>0)
        {
            List lstTemp=new ArrayList();
            ColBean cbTmp;
            for(Object obj:this.lstChildren)
            {
                if(obj instanceof ColBean)
                {
                    cbTmp=(ColBean)obj;
                    cbTmp=disbean.getColBeanByColId(cbTmp.getColid());
                    lstTemp.add(cbTmp);
                }else if(obj instanceof UltraListReportGroupBean)
                {
                    lstTemp.add(((UltraListReportGroupBean)obj).clone(parent));
                }
            }
            groupBeanNew.setLstChildren(lstTemp);
        }
        cloneExtendConfig(groupBeanNew);
        return groupBeanNew;
    }

    public void doPostLoad()
    {
        if(this.lstChildren!=null&&this.lstChildren.size()>0)
        {
            AbsListReportBean alrbean=(AbsListReportBean)this.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
            ColBean cbeanTmp;
            for(Object childObj:lstChildren)
            {
                if(childObj==null) continue;
                if(childObj instanceof ColBean)
                {
                    cbeanTmp=((ColBean)childObj);
                    if(alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL)
                    {//报表配置了纵向滚动条,则不能在<group/>中的列配置width。
                        if(Tools.getPropertyValueByName("width",cbeanTmp.getValuestyleproperty(null,true),true)!=null
                                ||Tools.getPropertyValueByName("width",cbeanTmp.getLabelstyleproperty(null,true),true)!=null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+cbeanTmp.getReportBean().getPath()
                                    +"失败，此报表配置了scrollheight，因此不能在<group/>中的<col/>配置width属性");
                        }
                    }
                }else if(childObj instanceof UltraListReportGroupBean)
                {
                    ((UltraListReportGroupBean)childObj).doPostLoad();
                }
            }
        }
    }

    public int getHidden()
    {
        throw new RuntimeException("被删除");
    }
}
