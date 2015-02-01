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
package com.wabacus.system.component.application.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.itextpdf.text.Element;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.extendconfig.LoadExtendConfigManager;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.StandardExcelAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.ColAndGroupDisplayBean;
import com.wabacus.system.component.application.report.configbean.ColAndGroupTitlePositionBean;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.system.component.application.report.configbean.UltraListReportColBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportGroupBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.RowDataBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class UltraListReportType extends ListReportType
{
    public UltraListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    private static Log log=LogFactory.getLog(UltraListReportType.class);

    public final static String KEY=UltraListReportType.class.getName();

    protected final static String MAX_TITLE_ROWSPANS="MAX_TITLE_ROWSPANS";

    protected Map<String,ColAndGroupTitlePositionBean> mRuntimeColGroupPositionBeans;

    public void loadReportData(boolean shouldInvokePostaction)
    {
        super.loadReportData(shouldInvokePostaction);
        getRuntimeColAndGroupPosition();
    }

    protected void getRuntimeColAndGroupPosition()
    {
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
        if(this.isSimpleListReport()) return;//如果没有<group/>配置，即没有列分组，则只需按普通报表显示头部
        List<String> lstDynOrderColids=this.cacheDataBean.getLstDynOrderColids();
        if((rrequest.getShowtype()==Consts.DISPLAY_ON_PLAINEXCEL&&lstDynOrderColids!=null&&lstDynOrderColids.size()>0)
                ||(this.cacheDataBean.getLstDynDisplayColids()!=null&&this.cacheDataBean.getLstDynDisplayColids().size()>0)
                ||"false".equals(String.valueOf(this.cacheDataBean.getAttributes().get("authroize_col_display")).trim()))
        {//注意：对于显示在非plainexcel时，只有改变了显示列的数量才需要重新计算各列的位置。如果只是改变了各列的显示顺序，则不用计算它们的位置，只要后面排一下序就可以了
            this.mRuntimeColGroupPositionBeans=calPosition(rbean,ulrdbean.getLstChildren(),this.cacheDataBean.getLstDynDisplayColids(),rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE);
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PLAINEXCEL)
            {
                calPositionForStandardExcel(ulrdbean.getLstChildren(),lstDynOrderColids,mRuntimeColGroupPositionBeans);
            }
        }else
        {
            mRuntimeColGroupPositionBeans=rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE?ulrdbean.getMChildrenDefaultPagePositions():ulrdbean
                    .getMChildrenDefaultDataExportPositions();
            if(mRuntimeColGroupPositionBeans==null)
            {
                throw new WabacusRuntimeException("显示报表"+this.rbean.getPath()+"失败，没有取到各普通列和分组列的位置信息，可能启动时抛了异常导致启动失败");
            }
        }
    }
    
    protected String showDataHeaderPart()
    {
        DisplayBean dbean=rbean.getDbean();
        StringBuilder resultBuf=new StringBuilder();
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)dbean.getExtendConfigDataForReportType(KEY);
        if(isSimpleListReport()) return super.showDataHeaderPart(); //如果没有<group/>配置，即没有列分组，则只需按普通报表显示头部
        ColAndGroupTitlePositionBean positionBean=mRuntimeColGroupPositionBeans.get(MAX_TITLE_ROWSPANS);
        int dataheader_rowcount=positionBean.getRowspan();
        List<String> lstDynColids=this.cacheDataBean.getLstDynOrderColids();
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE) mDisplayRealColAndGroupLabels=new HashMap<String,String>();
        String thstyleproperty=this.getRowLabelstyleproperty();
        if(this.rbean.getInterceptor()!=null)
        {
            RowDataBean rowdataObj=new RowDataBean(this,thstyleproperty,this.getLstDisplayColBeans(),null,-1,this.cacheDataBean.getTotalColCount());
            this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this.rrequest,this.rbean,rowdataObj);
            if(rowdataObj.getInsertDisplayRowHtml()!=null) resultBuf.append(rowdataObj.getInsertDisplayRowHtml());
            thstyleproperty=rowdataObj.getRowstyleproperty();
            if(!rowdataObj.isShouldDisplayThisRow()) return resultBuf.toString();
        }
        if(thstyleproperty==null) thstyleproperty="";
        if(thstyleproperty.toLowerCase().indexOf("class=")<0) thstyleproperty+=" class='"+getDataHeaderTrClassName()+"'";
        resultBuf.append("<tr ").append(thstyleproperty).append(">");
        List lstChildren=sortChildrenByDynColOrders(getLstDisplayChildren(ulrdbean),lstDynColids,mRuntimeColGroupPositionBeans);
        String lastDisplayColIdInFirstTitleRow=getLastDisplayColIdInFirstTitleRow(ulrdbean);//获取本次显示标题中第一行的最后一列ID，有可能是普通列，也有可能是分组列。因为在显示最后一列标题时，下面会有一些特殊处理。
        resultBuf.append(showLabel(lstChildren,mRuntimeColGroupPositionBeans,lastDisplayColIdInFirstTitleRow));
        resultBuf.append("</tr>");
        UltraListReportGroupBean groupBean;
        //Map<String,Integer> mGroupLayers=new HashMap<String,Integer>();//存放每个顶层分组当前显示的层级数
        List<String> lstHasDisplayColGroupIds=new ArrayList<String>();//存放已经显示了的<col/>和<group/>的id列表
        ColAndGroupTitlePositionBean positionBeanTmp;
        String groupidTmp;
        for(int i=1;i<dataheader_rowcount;i++)
        {
            resultBuf.append("<tr ").append(thstyleproperty).append(">");
            for(Object obj:lstChildren)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBean=(UltraListReportGroupBean)obj;
                groupidTmp=groupBean.getGroupid();
                positionBeanTmp=mRuntimeColGroupPositionBeans.get(groupidTmp);
                if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
                //                Integer layer=mGroupLayers.get(groupidTmp);//取到当前分组当前要显示的层级数
                List lstChildrenLocal=getDisplayChildrenOfGroupBean(groupBean.getLstChildren(),mRuntimeColGroupPositionBeans,lstHasDisplayColGroupIds,
                        i+1,groupBean.getRowspan());
                if(lstChildrenLocal==null||lstChildrenLocal.size()==0) continue;
                //                mGroupLayers.put(groupidTmp,layer+1);//显示了一层，则将层级数加1，并存放起来
                lstChildrenLocal=sortChildrenByDynColOrders(lstChildrenLocal,lstDynColids,mRuntimeColGroupPositionBeans);
                resultBuf.append(showLabel(lstChildrenLocal,mRuntimeColGroupPositionBeans,null));
            }
            resultBuf.append("</tr>");
        }
        if(this.fixedDataBean!=null&&this.fixedDataBean.getFixedrowscount()==Integer.MAX_VALUE)
        {
            this.fixedDataBean.setFixedrowscount(dataheader_rowcount);//显示了标题行，则设置冻结行数为dataheader_rowcount
        }
        return resultBuf.toString();
    }

    protected List getLstDisplayChildren(UltraListReportDisplayBean ulrdbean)
    {
        return ulrdbean.getLstChildren();
    }

    protected String getLastDisplayColIdInFirstTitleRow(UltraListReportDisplayBean urldbean)
    {
        String lastColId=this.cacheDataBean.getLastColId();
        ColBean cb=rbean.getDbean().getColBeanByColId(lastColId);
        UltraListReportColBean urlcbean=(UltraListReportColBean)cb.getExtendConfigDataForReportType(KEY);
        String parentGroupid=urlcbean.getParentGroupid();
        UltraListReportGroupBean urlgroupbean;
        while(parentGroupid!=null&&!parentGroupid.trim().equals(""))
        {
            urlgroupbean=urldbean.getGroupBeanById(parentGroupid);
            lastColId=urlgroupbean.getGroupid();
            parentGroupid=urlgroupbean.getParentGroupid();
        }
        return lastColId;
    }

    private List getDisplayChildrenOfGroupBean(List lstChildrens,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,
            List<String> lstHasDisplayColGroupIds,int currentrowcount,int parentrowspan)
    {
        if(lstChildrens==null||lstChildrens.size()==0) return null;
        if(currentrowcount-parentrowspan<=0) return null;//如果父分组标题所占的rowspan还没有显示完，则在当前<tr/>中先不显示其子分组或子列
        List lstDisplayChildren=new ArrayList();
        String idTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        UltraListReportGroupBean groupBeanTmp;
        for(Object objTmp:lstChildrens)
        {
            idTmp=null;
            if(objTmp instanceof ColBean)
            {
                idTmp=((ColBean)objTmp).getColid();
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                idTmp=((UltraListReportGroupBean)objTmp).getGroupid();
            }
            if(idTmp==null) continue;
            positionBeanTmp=mColAndGroupTitlePostions.get(idTmp);
            if(positionBeanTmp!=null&&positionBeanTmp.getDisplaymode()>0&&!lstHasDisplayColGroupIds.contains(idTmp))
            {//当前（分组）列需要显示，且还没有显示，则将它加入显示
                lstHasDisplayColGroupIds.add(idTmp);
                lstDisplayChildren.add(objTmp);
            }else if(objTmp instanceof UltraListReportGroupBean&&lstHasDisplayColGroupIds.contains(idTmp))
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                List lstDisplayChildrenTmp=getDisplayChildrenOfGroupBean(groupBeanTmp.getLstChildren(),mColAndGroupTitlePostions,
                        lstHasDisplayColGroupIds,currentrowcount,parentrowspan+groupBeanTmp.getRowspan());
                if(lstDisplayChildrenTmp!=null) lstDisplayChildren.addAll(lstDisplayChildrenTmp);
            }
        }
        return lstDisplayChildren;
    }

    protected String showLabel(List lstChildren,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,
            String lastDisplayColIdInFirstTitleRow)
    {
        StringBuilder resultBuf=new StringBuilder();
        String labelstyleproperty=null;
        String label=null;
        String parentGroupid=null;
        AbsListReportColBean alrcbean;
        UltraListReportColBean ulrcbean;
        ColBean colbean;
        UltraListReportGroupBean groupBean;
        ColDisplayData colDisplayData;
        ColAndGroupTitlePositionBean positionBeanTmp;
        String id;
        boolean isFirstCol=true;
        for(Object obj:lstChildren)
        {
            colbean=null;
            alrcbean=null;
            groupBean=null;
            positionBeanTmp=null;
            id=null;
            if(obj instanceof ColBean)
            {
                colbean=(ColBean)obj;
                if(alrdbean!=null&&alrdbean.getRowgrouptype()==2&&alrdbean.getRowGroupColsNum()>0)
                {
                    if(alrdbean.getLstRowgroupColsColumn().contains(colbean.getColumn())
                            &&!colbean.getColumn().equals(alrdbean.getLstRowgroupColsColumn().get(0)))
                    {//如果当前cbean是树形行分组的列，但不是第一列，则不显示为一独立列，所以这里就不为它显示一个<td/>
                        continue;
                    }
                }
                alrcbean=(AbsListReportColBean)colbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                ulrcbean=(UltraListReportColBean)colbean.getExtendConfigDataForReportType(KEY);
                if(ulrcbean!=null) parentGroupid=ulrcbean.getParentGroupid();
                positionBeanTmp=mColAndGroupTitlePostions.get(colbean.getColid());
                labelstyleproperty=getColLabelStyleproperty(colbean,null);
                label=colbean.getLabel(rrequest);
                id=colbean.getColid();
            }else if(obj instanceof UltraListReportGroupBean)
            {
                groupBean=((UltraListReportGroupBean)obj);
                labelstyleproperty=groupBean.getLabelstyleproperty(rrequest,false);
                label=groupBean.getLabel(rrequest);
                parentGroupid=groupBean.getParentGroupid();
                positionBeanTmp=mColAndGroupTitlePostions.get(groupBean.getGroupid());
                id=groupBean.getGroupid();
            }
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
            if(positionBeanTmp.getColspan()>1)
            {
                labelstyleproperty=" colspan='"+positionBeanTmp.getColspan()+"' "+labelstyleproperty;
            }
            if(positionBeanTmp.getRowspan()>1)
            {
                labelstyleproperty=" rowspan='"+positionBeanTmp.getRowspan()+"' "+labelstyleproperty;
            }
            colDisplayData=ColDisplayData.getColDataFromInterceptor(this,obj,null,-1,labelstyleproperty,label);
            label=colDisplayData.getValue();
            if(mDisplayRealColAndGroupLabels!=null) mDisplayRealColAndGroupLabels.put(id,label);
            resultBuf.append("<td class='"+getDataHeaderThClassName()+"' ");
            resultBuf.append(colDisplayData.getStyleproperty());
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                if(rbean.getCelldrag()>0)
                {
                    String dragcolid=null;
                    if(colbean!=null)
                    {//如果当前报表需要列拖动，且当前单元格不参与普通行分组和树形分组
                        if(alrcbean==null||alrcbean.isDragable(alrdbean)) dragcolid=colbean.getColid();
                    }else if(groupBean.isDragable(alrdbean))
                    {
                        dragcolid=groupBean.getGroupid();
                    }
                    if(dragcolid!=null)
                    {
                        resultBuf.append(" onmousedown=\"try{handleCellDragMouseDown(this,'"+rbean.getPageBean().getId()+"','"+rbean.getId()
                                +"');}catch(e){logErrorsAsJsFileLoad(e);}\"");
                        resultBuf.append(" dragcolid=\"").append(dragcolid).append("\"");
                        if(parentGroupid!=null&&!parentGroupid.trim().equals(""))
                        {
                            resultBuf.append(" parentGroupid=\"").append(parentGroupid).append("\"");
                        }
                    }
                }
                if(this.fixedDataBean!=null&&obj instanceof ColBean)
                {
                    resultBuf.append(this.fixedDataBean.showFirstNonFixedColFlag((ColBean)obj));
                }
                resultBuf.append(">");
                if(rbean.getCellresize()>0&&lastDisplayColIdInFirstTitleRow!=null)
                {
                    if(rbean.getCellresize()==1&&!id.equals(lastDisplayColIdInFirstTitleRow))
                    {
                        resultBuf.append(ListReportAssistant.getInstance().appendCellResizeFunction(true));
                    }else
                    {
                        resultBuf.append(ListReportAssistant.getInstance().appendCellResizeFunction(false));
                    }
                }
                if(rbean.getDbean().isPageColselect()&&lastDisplayColIdInFirstTitleRow!=null)
                {
                    if(rbean.getDbean().isDisplayColSelectLabelLeft()&&isFirstCol)
                    {
                        resultBuf.append(ReportAssistant.getInstance().getColSelectedLabelAndEvent(rrequest,rbean,true,"left"));
                    }
                    if(rbean.getDbean().isDisplayColSelectLabelRight()&&id.equals(lastDisplayColIdInFirstTitleRow))
                    {//当前是显示第一行标题的最后一列，且需要提供动态列选择功能
                        resultBuf.append(ReportAssistant.getInstance().getColSelectedLabelAndEvent(rrequest,rbean,true,"right"));
                    }
                }
                if(colbean!=null&&alrcbean!=null)
                {
                    if(this.getLstReportData()!=null&&this.getLstReportData().size()>0)
                    {
                        if(alrcbean.isRequireClickOrderby())
                        {
                            label=ListReportAssistant.getInstance().getColLabelWithOrderBy(colbean,rrequest,label);
                        }
                    }
                    if(alrcbean.getFilterBean()!=null)
                    {
                        resultBuf.append(ListReportAssistant.getInstance().createColumnFilter(rrequest,alrcbean));
                    }
                }
                isFirstCol=false;
            }else
            {
                String dataheaderbgcolor=Config.getInstance().getSkinConfigValue(rrequest.getPageskin(),"table.dataheader.bgcolor");
                if(dataheaderbgcolor==null) dataheaderbgcolor="";
                resultBuf.append(" bgcolor='"+dataheaderbgcolor+"'>");
            }
            resultBuf.append(label+"</td>");
        }
        return resultBuf.toString();
    }

    protected List<ColAndGroupDisplayBean> getColAndGroupDisplayBeans(boolean isForPage)
    {
        if(isSimpleListReport()) return super.getColAndGroupDisplayBeans(isForPage); //如果没有<group/>配置，即没有列分组，则只需按普通报表显示头部
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return null;
        List<ColAndGroupDisplayBean> lstColAndGroupDisplayBeans=new ArrayList<ColAndGroupDisplayBean>();
        ColAndGroupDisplayBean cgDisplayBeanTmp;
        ColBean cbTmp;
        AbsListReportColBean alrcbean=null;
        UltraListReportGroupBean ulgroupbeanTmp;
        Map<String,ColAndGroupTitlePositionBean> mRuntimeColGroupPositionBeansTmp=getRuntimeColAndGroupDisplayBeansForColSelect(isForPage);
        List lstChildren=sortChildrenByDynColOrders(getLstDisplayChildren((UltraListReportDisplayBean)rbean.getDbean()
                .getExtendConfigDataForReportType(KEY)),cacheDataBean.getLstDynOrderColids(),mRuntimeColGroupPositionBeansTmp);
        ColAndGroupTitlePositionBean cgpositionBeanTmp;
        String labelTmp;
        for(Object objTmp:lstChildren)
        {
            cgDisplayBeanTmp=new ColAndGroupDisplayBean();
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbTmp.getDisplaytype(isForPage))) continue;
                if(alrdbean!=null&&alrdbean.getRowgrouptype()==2&&alrdbean.getRowGroupColsNum()>0)
                {//当前报表是树形分组报表
                    if(alrdbean.getLstRowgroupColsColumn().contains(cbTmp.getColumn())
                            &&!cbTmp.getColumn().equals(alrdbean.getLstRowgroupColsColumn().get(0)))
                    {
                        continue;
                    }
                }
                cgpositionBeanTmp=mRuntimeColGroupPositionBeansTmp.get(cbTmp.getColid());
                if(cgpositionBeanTmp.getDisplaymode()<0) continue;
                alrcbean=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                cgDisplayBeanTmp.setNonFixedCol(alrcbean==null||!alrcbean.isFixedCol(rrequest));
                cgDisplayBeanTmp.setId(cbTmp.getColid());
                cgDisplayBeanTmp.setControlCol(cbTmp.isControlCol());
                cgDisplayBeanTmp.setAlways(cgpositionBeanTmp.getDisplaymode()==2);
                cgDisplayBeanTmp.setChecked(cgpositionBeanTmp.getDisplaymode()>0);
                labelTmp=this.mDisplayRealColAndGroupLabels.get(cbTmp.getColid());
                if(Tools.isEmpty(labelTmp)) labelTmp=cbTmp.getLabel(rrequest);
                cgDisplayBeanTmp.setTitle(labelTmp);
                lstColAndGroupDisplayBeans.add(cgDisplayBeanTmp);
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                ulgroupbeanTmp=(UltraListReportGroupBean)objTmp;
                cgpositionBeanTmp=mRuntimeColGroupPositionBeansTmp.get(ulgroupbeanTmp.getGroupid());
                if(cgpositionBeanTmp.getDisplaymode()<0) continue;
                cgDisplayBeanTmp.setId(ulgroupbeanTmp.getGroupid());
                cgDisplayBeanTmp.setChildIds(ulgroupbeanTmp.getChildids());
                cgDisplayBeanTmp.setAlways(cgpositionBeanTmp.getDisplaymode()==2);
                cgDisplayBeanTmp.setChecked(cgpositionBeanTmp.getDisplaymode()>0);
                labelTmp=this.mDisplayRealColAndGroupLabels.get(ulgroupbeanTmp.getGroupid());
                if(Tools.isEmpty(labelTmp)) labelTmp=ulgroupbeanTmp.getLabel(rrequest);
                cgDisplayBeanTmp.setTitle(labelTmp);
                lstColAndGroupDisplayBeans.add(cgDisplayBeanTmp);
                ulgroupbeanTmp.createColAndGroupDisplayBeans(this,mDisplayRealColAndGroupLabels,rrequest,cacheDataBean.getLstDynOrderColids(),
                        mRuntimeColGroupPositionBeansTmp,lstColAndGroupDisplayBeans,isForPage);
            }
        }
        return lstColAndGroupDisplayBeans;
    }

    private Map<String,ColAndGroupTitlePositionBean> getRuntimeColAndGroupDisplayBeansForColSelect(boolean isForPage)
    {
        Map<String, ColAndGroupTitlePositionBean> mRuntimeColGroupPositionBeansTmp=null;
        if(isForPage||!isForPage&&rbean.getDbean().isAllColDisplaytypesEquals())
        {
            mRuntimeColGroupPositionBeansTmp=this.mRuntimeColGroupPositionBeans;
        }else
        {//当前是获取导出数据文件时各列位置信息，且导出数据文件时有自己的列选择信息
            UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
            if("false".equals(String.valueOf(this.cacheDataBean.getAttributes().get("authroize_col_display")).trim()))
            {
                mRuntimeColGroupPositionBeansTmp=calPosition(rbean,ulrdbean.getLstChildren(),null,false);
            }else
            {
                mRuntimeColGroupPositionBeansTmp=ulrdbean.getMChildrenDefaultDataExportPositions();
            }
        }
        return mRuntimeColGroupPositionBeansTmp;
    }

    protected void showReportTitleOnPlainExcel(Workbook workbook)
    {
        String plainexceltitle=null;
        if(this.pedebean!=null) plainexceltitle=this.pedebean.getPlainexceltitle();
        if("none".equals(plainexceltitle)) return;
        if("column".equals(plainexceltitle)||rbean.getSbean().isHorizontalDataset())
        {
            super.showReportTitleOnPlainExcel(workbook);
            return;
        }
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
        if(isSimpleListReport())
        {//如果没有<group/>配置，即没有列分组，则只需按普通报表显示头部
            super.showReportTitleOnPlainExcel(workbook);
            return;
        }
        List<String> lstDynOrderColids=cacheDataBean.getLstDynOrderColids();
        ColAndGroupTitlePositionBean positionBean=this.mRuntimeColGroupPositionBeans.get(MAX_TITLE_ROWSPANS);
        int dataheader_rowcount=positionBean.getRowspan();
        List lstChildren=sortChildrenByDynColOrders(getLstDisplayChildren(ulrdbean),lstDynOrderColids,mRuntimeColGroupPositionBeans);
        showLabelInPlainExcel(workbook,excelSheet,lstChildren,mRuntimeColGroupPositionBeans);
        UltraListReportGroupBean groupBean;
        ColAndGroupTitlePositionBean positionBeanTmp;
        String groupidTmp;
        List<String> lstHasDisplayColGroupIds=new ArrayList<String>();//存放已经显示了的<col/>和<group/>的id列表
        for(int i=1;i<dataheader_rowcount;i++)
        {
            for(Object obj:lstChildren)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBean=(UltraListReportGroupBean)obj;
                groupidTmp=groupBean.getGroupid();
                positionBeanTmp=mRuntimeColGroupPositionBeans.get(groupidTmp);
                if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
                List lstChildrenLocal=getDisplayChildrenOfGroupBean(groupBean.getLstChildren(),mRuntimeColGroupPositionBeans,lstHasDisplayColGroupIds,
                        i+1,groupBean.getRowspan());
                if(lstChildrenLocal==null||lstChildrenLocal.size()==0) continue;
                lstChildrenLocal=sortChildrenByDynColOrders(lstChildrenLocal,lstDynOrderColids,mRuntimeColGroupPositionBeans);
                showLabelInPlainExcel(workbook,excelSheet,lstChildrenLocal,mRuntimeColGroupPositionBeans);
            }
        }
        excelRowIdx+=dataheader_rowcount;
        titleRowCount=dataheader_rowcount;
    }

    protected void showLabelInPlainExcel(Workbook workbook,Sheet sheet,List lstChildren,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        CellStyle titleCellStyle=StandardExcelAssistant.getInstance().getTitleCellStyleForStandardExcel(workbook);
        ColBean colbean;
        UltraListReportGroupBean groupBean;
        ColDisplayData colDisplayData;
        ColAndGroupTitlePositionBean positionBeanTmp;
        String id;
        String label=null;
        String align=null;
        CellRangeAddress region;
        for(Object obj:lstChildren)
        {
            colbean=null;
            groupBean=null;
            positionBeanTmp=null;
            id=null;
            align=null;
            if(obj instanceof ColBean)
            {
                colbean=(ColBean)obj;
                id=colbean.getColid();
                label=colbean.getLabel(rrequest);
                align=colbean.getLabelalign();
            }else if(obj instanceof UltraListReportGroupBean)
            {
                groupBean=((UltraListReportGroupBean)obj);
                label=groupBean.getLabel(rrequest);
                id=groupBean.getGroupid();
            }
            positionBeanTmp=mColAndGroupTitlePostions.get(id);
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
            colDisplayData=ColDisplayData.getColDataFromInterceptor(this,obj,null,-1,null,label);
            region=new CellRangeAddress(positionBeanTmp.getStartrowindex(),positionBeanTmp.getStartrowindex()+positionBeanTmp.getRowspan()-1,
                    positionBeanTmp.getStartcolindex(),positionBeanTmp.getStartcolindex()+positionBeanTmp.getColspan()-1);//参数依次为firstrow、lastrow、firstcol、lastcol
            StandardExcelAssistant.getInstance().setRegionCellStringValue(workbook,sheet,region,
                    StandardExcelAssistant.getInstance().setCellAlign(titleCellStyle,align),colDisplayData.getValue());
        }
    }

    protected void showDataHeaderOnPdf()
    {
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
        if(isSimpleListReport())
        {//如果没有<group/>配置，即没有列分组，则只需按普通报表显示头部
            super.showDataHeaderOnPdf();
            return;
        }
        List<String> lstDynOrderColids=cacheDataBean.getLstDynOrderColids();
        ColAndGroupTitlePositionBean positionBean=this.mRuntimeColGroupPositionBeans.get(MAX_TITLE_ROWSPANS);
        int dataheader_rowcount=positionBean.getRowspan();
        List lstChildren=sortChildrenByDynColOrders(getLstDisplayChildren(ulrdbean),lstDynOrderColids,mRuntimeColGroupPositionBeans);
        showLabelInPdf(lstChildren,mRuntimeColGroupPositionBeans);
        UltraListReportGroupBean groupBean;
        List<String> lstHasDisplayColGroupIds=new ArrayList<String>();//存放已经显示了的<col/>和<group/>的id列表
        ColAndGroupTitlePositionBean positionBeanTmp;
        String groupidTmp;
        for(int i=1;i<dataheader_rowcount;i++)
        {
            for(Object obj:lstChildren)
            {
                if(obj==null||obj instanceof ColBean) continue;
                groupBean=(UltraListReportGroupBean)obj;
                groupidTmp=groupBean.getGroupid();
                positionBeanTmp=mRuntimeColGroupPositionBeans.get(groupidTmp);
                if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
                List lstChildrenLocal=getDisplayChildrenOfGroupBean(groupBean.getLstChildren(),mRuntimeColGroupPositionBeans,lstHasDisplayColGroupIds,
                        i+1,groupBean.getRowspan());
                if(lstChildrenLocal==null||lstChildrenLocal.size()==0) continue;
                lstChildrenLocal=sortChildrenByDynColOrders(lstChildrenLocal,lstDynOrderColids,mRuntimeColGroupPositionBeans);
                showLabelInPdf(lstChildrenLocal,mRuntimeColGroupPositionBeans);
            }
        }
    }

    protected void showLabelInPdf(List lstChildren,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        ColBean colbean;
        UltraListReportGroupBean groupBean;
        ColDisplayData colDisplayData;
        ColAndGroupTitlePositionBean positionBeanTmp;
        String id;
        String label=null;
        String align=null;
        for(Object obj:lstChildren)
        {
            colbean=null;
            groupBean=null;
            positionBeanTmp=null;
            align=null;
            id=null;
            if(obj instanceof ColBean)
            {
                colbean=(ColBean)obj;
                id=colbean.getColid();
                label=colbean.getLabel(rrequest);
                align=colbean.getLabelalign();
            }else if(obj instanceof UltraListReportGroupBean)
            {
                groupBean=((UltraListReportGroupBean)obj);
                label=groupBean.getLabel(rrequest);
                id=groupBean.getGroupid();
            }
            positionBeanTmp=mColAndGroupTitlePostions.get(id);
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
            colDisplayData=ColDisplayData.getColDataFromInterceptor(this,obj,null,-1,null,label);
            this.addDataHeaderCell(obj,colDisplayData.getValue(),positionBeanTmp.getRowspan(),positionBeanTmp.getColspan(),this.getPdfCellAlign(
                    align,Element.ALIGN_CENTER));
        }
    }

    public List sortChildrenByDynColOrders(List lstChildren,List<String> lstDynColids,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        if(lstChildren.size()==1||lstDynColids==null||lstDynColids.size()==0)
        {
            return lstChildren;
        }
        Object[] objs=lstChildren.toArray();
        Object objTmp;
        int tmp;
        int n=objs.length-1;
        while(n>0)
        {
            int index=n;
            n=0;
            for(int i=0;i<index;i++)
            {
                tmp=comparePosition(objs[i],objs[i+1],lstDynColids,mColAndGroupTitlePostions);
                if(tmp>0)
                {
                    objTmp=objs[i];
                    objs[i]=objs[i+1];
                    objs[i+1]=objTmp;
                    n=i;
                }else if(tmp==-2)
                {
                    continue;
                }else if(tmp==-3)
                {
                    int j=i+2;
                    for(;j<=index;j++)
                    {//这里用<=，是因为objs[j]是做为被objs[i]的比较列，所以要循环完
                        tmp=comparePosition(objs[i],objs[j],lstDynColids,mColAndGroupTitlePostions);
                        if(tmp==-3)
                        {
                            continue;
                        }
                        if(tmp>0)
                        {
                            objTmp=objs[i];
                            objs[i]=objs[j];
                            objs[j]=objTmp;
                            n=i;
                        }
                        break;
                    }
                    if(j>index) break;
                }
            }
        }
        List lstChildrenNew=new ArrayList();
        for(int i=0;i<objs.length;i++)
        {
            lstChildrenNew.add(objs[i]);
        }
        return lstChildrenNew;
    }

    private int comparePosition(Object obj1,Object obj2,List<String> lstDynColids,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        String colid1=null;
        String colid2=null;
        if(obj1 instanceof ColBean)
        {
            colid1=((ColBean)obj1).getColid();
            if(mColAndGroupTitlePostions.get(colid1).getDisplaymode()<=0) return -2;//被比较的当前普通列不显示，则不比较
        }else
        {
            if(mColAndGroupTitlePostions.get(((UltraListReportGroupBean)obj1).getGroupid()).getDisplaymode()<=0) return -2;
            colid1=((UltraListReportGroupBean)obj1).getFirstColId(lstDynColids);
            //            label1=((UltraReportListGroupBean)obj1).getLabel();
        }
        if(obj2 instanceof ColBean)
        {
            colid2=((ColBean)obj2).getColid();
            if(mColAndGroupTitlePostions.get(colid2).getDisplaymode()<=0) return -3;
        }else
        {
            if(mColAndGroupTitlePostions.get(((UltraListReportGroupBean)obj2).getGroupid()).getDisplaymode()<=0) return -3;//被比较的当前分组列不显示，则不比较
            colid2=((UltraListReportGroupBean)obj2).getFirstColId(lstDynColids);
        }
        int idx1=-1;
        int idx2=-1;
        for(int i=0;i<lstDynColids.size();i++)
        {
            if(lstDynColids.get(i).equals(colid1))
            {
                idx1=i;
            }else if(lstDynColids.get(i).equals(colid2))
            {
                idx2=i;
            }
        }
        if(idx1==-1)
        {
            //log.warn("没有找到colid为"+colid1+"的<col/>，拖动列失败");
            return 0;
        }
        if(idx2==-1)
        {
            //log.warn("没有找到colid为"+colid2+"的<col/>，拖动列失败");
            return 0;
        }
        if(idx1>idx2) return 1;
        if(idx1<idx2) return -1;
        return 0;
    }

    private boolean isSimpleListReport()
    {
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
        return ulrdbean==null||!ulrdbean.isHasGroupConfig(rrequest)||rbean.getSbean().isHorizontalDataset();
    }
    
    public int afterDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        if(lstEleDisplayBeans==null) return 0;
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null)
        {
            ulrdbean=new UltraListReportDisplayBean(disbean);
            disbean.setExtendConfigDataForReportType(KEY,ulrdbean);
        }
        List<XmlElementBean> lstColAndGroups=new ArrayList<XmlElementBean>();
        boolean hasGroupConfig=joinedAllColAndGroupElement(lstEleDisplayBeans,lstColAndGroups,disbean.getReportBean());//取到所有要显示的直接子<col/>和<group/>标签对象
        if(!hasGroupConfig)
        {//没有<group/>配置
            ulrdbean.setHasGroupConfig(false);
            return super.afterDisplayLoading(disbean,lstEleDisplayBeans);
        }
        ulrdbean.setHasGroupConfig(true);
        disbean.clearChildrenInfo();
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrdbean!=null) alrdbean.clearChildrenInfo();
        if(lstColAndGroups==null||lstColAndGroups.size()==0) return 0;
        List lstChildren=new ArrayList();
        ulrdbean.setLstChildren(lstChildren);
        for(XmlElementBean eleChildBeanTmp:lstColAndGroups)
        {
            if(eleChildBeanTmp.getName().equalsIgnoreCase("col"))
            {
                ColBean colbean=ComponentConfigLoadManager.loadColConfig(eleChildBeanTmp,disbean);
                UltraListReportColBean ulrcbean=(UltraListReportColBean)colbean.getExtendConfigDataForReportType(KEY);
                if(ulrcbean==null)
                {
                    ulrcbean=new UltraListReportColBean(colbean);
                    colbean.setExtendConfigDataForReportType(KEY,ulrcbean);
                }
                disbean.getLstCols().add(colbean);
                lstChildren.add(colbean);
            }else if(eleChildBeanTmp.getName().equalsIgnoreCase("group"))
            {
                UltraListReportGroupBean groupBean=new UltraListReportGroupBean(disbean);
                lstChildren.add(groupBean);
                loadGroupConfig(groupBean,eleChildBeanTmp,disbean,null);
            }
        }
        if(lstChildren.size()==0) throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，没有配置要显示的列");
        int flag=super.afterDisplayLoading(disbean,lstEleDisplayBeans);
        if(flag<0) return -1;
        return 1;
    }

    protected boolean joinedAllColAndGroupElement(List<XmlElementBean> lstEleDisplayBeans,List<XmlElementBean> lstResults,ReportBean rbean)
    {
        boolean hasGroupConfig=false;
        List<XmlElementBean> lstChildrenTmp;
        for(XmlElementBean eleBeanTmp:lstEleDisplayBeans)
        {
            lstChildrenTmp=eleBeanTmp.getLstChildElements();
            if(lstChildrenTmp==null||lstChildrenTmp.size()==0) continue;
            for(XmlElementBean childTmp:lstChildrenTmp)
            {
                if("group".equals(childTmp.getName())||"col".equals(childTmp.getName()))
                {
                    lstResults.add(childTmp);
                    if(childTmp.getName().equals("group")) hasGroupConfig=true;
                }else if("ref".equals(childTmp.getName()))
                {//引用其它资源项中的配置
                    hasGroupConfig=joinedAllColAndGroupElement(ConfigLoadAssistant.getInstance().getRefElements(childTmp.attributeValue("key"),
                            "display",null,rbean),lstResults,rbean);
                }
            }
        }
        return hasGroupConfig;
    }

    protected void loadGroupConfig(UltraListReportGroupBean groupBean,XmlElementBean eleGroupBean,DisplayBean disbean,
            UltraListReportGroupBean parentGroupBean)
    {
        String label=eleGroupBean.attributeValue("label");
        label=label==null?"":label.trim();
        String labelstyleproperty=eleGroupBean.attributeValue("labelstyleproperty");
        labelstyleproperty=labelstyleproperty==null?"":labelstyleproperty.trim();
        groupBean.setLabelstyleproperty(labelstyleproperty,false);
        labelstyleproperty=groupBean.getLabelstyleproperty(null,true);
        labelstyleproperty=Tools.addPropertyValueToStylePropertyIfNotExist(labelstyleproperty,"align","center");
        labelstyleproperty=Tools.addPropertyValueToStylePropertyIfNotExist(labelstyleproperty,"valign","middle");
        groupBean.setLabelstyleproperty(labelstyleproperty,true);
        if(label!=null)
        {
            label=label.trim();
            label=Config.getInstance().getResourceString(null,disbean.getPageBean(),label,true);
        }
        groupBean.setLabel(label);
        String rowspan=Tools.getPropertyValueByName("rowspan",labelstyleproperty,true);
        if(rowspan!=null&&!rowspan.trim().equals(""))
        {
            try
            {
                groupBean.setRowspan(Integer.parseInt(rowspan));
            }catch(NumberFormatException e)
            {
                log.warn("报表"+disbean.getReportBean().getPath()+"配置的<group/>的labelstyleproperty中的rowspan不是合法数字",e);
                groupBean.setRowspan(1);
            }
        }
        List<XmlElementBean> lstEleGroupBeans=new ArrayList<XmlElementBean>();
        lstEleGroupBeans.add(eleGroupBean);
        LoadExtendConfigManager.loadBeforeExtendConfigForReporttype(groupBean,lstEleGroupBeans);
        List lstGroupChildren=new ArrayList();
        groupBean.setLstChildren(lstGroupChildren);
        StringBuffer childIdsBuf=new StringBuffer();
        List<XmlElementBean> lstChildrenElements=eleGroupBean.getLstChildElements();
        if(lstChildrenElements==null||lstChildrenElements.size()==0)
        {
            throw new WabacusConfigLoadingException("报表"+disbean.getReportBean().getPath()+"配置的group"+label+"没有配置子标签");
        }
        for(XmlElementBean eleChildBeanTmp:lstChildrenElements)
        {
            if(eleChildBeanTmp.getName().equalsIgnoreCase("col"))
            {
                ColBean colbean=ComponentConfigLoadManager.loadColConfig(eleChildBeanTmp,disbean);
                //                {//如果当前普通列是永远显示，则其所有层级的父分组也必须设置为永远显示
                //                    setAlwaysDisplayTypeToGroupBean(ulrdbean,groupBean);
                AbsListReportColBean alrcbean=(AbsListReportColBean)colbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                AbsListReportDisplayBean alrdbeanTmp=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(alrdbeanTmp!=null&&alrdbeanTmp.getRowgrouptype()==2&&alrcbean!=null&&alrcbean.isRowgroup())
                {
                    throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，<group/>下的<col/>不能参与树形分组");
                }
                UltraListReportColBean ulrcbeanTmp=(UltraListReportColBean)colbean.getExtendConfigDataForReportType(KEY);
                if(ulrcbeanTmp==null)
                {
                    ulrcbeanTmp=new UltraListReportColBean(colbean);
                    colbean.setExtendConfigDataForReportType(KEY,ulrcbeanTmp);
                }
                ulrcbeanTmp.setParentGroupid(groupBean.getGroupid());
                disbean.getLstCols().add(colbean);
                lstGroupChildren.add(colbean);
                childIdsBuf.append(colbean.getColid()).append(",");
            }else if(eleChildBeanTmp.getName().equalsIgnoreCase("group"))
            {
                UltraListReportGroupBean groupBeanChild=new UltraListReportGroupBean(disbean);
                groupBeanChild.setParentGroupid(groupBean.getGroupid());
                lstGroupChildren.add(groupBeanChild);
                loadGroupConfig(groupBeanChild,eleChildBeanTmp,disbean,groupBean);
                childIdsBuf.append(groupBeanChild.getGroupid()).append(",");
            }
        }
        if(lstGroupChildren.size()==0)
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"的分组"+label+"失败，没有为它配置有效的显示子标签");
        }
        if(childIdsBuf.charAt(childIdsBuf.length()-1)==',') childIdsBuf.deleteCharAt(childIdsBuf.length()-1);
        groupBean.setChildids(childIdsBuf.toString());
        LoadExtendConfigManager.loadAfterExtendConfigForReporttype(groupBean,lstEleGroupBeans);
    }

    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
        if(ulrdbean!=null)
        {
            List lstChildren=ulrdbean.getLstChildren();
            if(lstChildren==null||lstChildren.size()==0) return 0;
            for(Object childObj:lstChildren)
            {
                if(childObj==null) continue;
                if(childObj instanceof UltraListReportGroupBean)
                {
                    ((UltraListReportGroupBean)childObj).doPostLoad();
                }
            }
            ulrdbean.setMChildrenDefaultPagePositions(calPosition(reportbean,ulrdbean.getLstChildren(),null,true));
            calPositionForStandardExcel(ulrdbean.getLstChildren(),null,ulrdbean.getMChildrenDefaultPagePositions());
            ulrdbean.setMChildrenDefaultDataExportPositions(calPosition(reportbean,ulrdbean.getLstChildren(),null,false));
            calPositionForStandardExcel(ulrdbean.getLstChildren(),null,ulrdbean.getMChildrenDefaultDataExportPositions());
        }
        return 1;
    }

    protected ColBean[] processRowSelectCol(DisplayBean disbean)
    {
        ColBean[] cbResults=super.processRowSelectCol(disbean);
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null) return cbResults;
        if(alrbean.getRowSelectType()==null
                ||(!alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_CHECKBOX)
                        &&!alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_RADIOBOX)
                        &&!alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_MULTIPLE_CHECKBOX)&&!alrbean.getRowSelectType().trim().equals(
                        Consts.ROWSELECT_SINGLE_RADIOBOX)))
        {//当前报表要么没有提供行选中功能，要么提供的不是复选框/单选框的行选择功能
            ulrdbean.removeChildColBeanByColumn(Consts_Private.COL_ROWSELECT,true);
        }
        return cbResults;
    }

    protected ColBean[] insertRowSelectNewCols(AbsListReportBean alrbean,List<ColBean> lstCols)
    {
        ReportBean reportbean=(ReportBean)alrbean.getOwner();
        ColBean[] cbResults=super.insertRowSelectNewCols(alrbean,lstCols);
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null||cbResults==null||ulrdbean.getLstChildren()==null||ulrdbean.getLstChildren().size()==0) return cbResults;
        UltraListReportColBean ulrcbean=new UltraListReportColBean(cbResults[0]);
        cbResults[0].setExtendConfigDataForReportType(KEY,ulrcbean);
        List lstChildren=this.getLstDisplayChildren(ulrdbean);
        if(cbResults[1]==null)
        {//生成的行选择列是放在最后一列
            lstChildren.add(cbResults[0]);
            return cbResults;
        }
        Object objTmp;
        ColBean cbTmp;
        for(int i=0;i<lstChildren.size();i++)
        {
            objTmp=lstChildren.get(i);
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                if(cbTmp.getColid().equals(cbResults[1].getColid()))
                {
                    lstChildren.add(i,cbResults[0]);
                    break;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                if(((UltraListReportGroupBean)objTmp).containsChild(cbResults[1].getColid(),true))
                {
                    lstChildren.add(i,cbResults[0]);
                    break;
                }
            }
        }
        return cbResults;
    }

    protected List<ColBean> processRoworderCol(DisplayBean disbean)
    {
        List<ColBean> lstCreatedColBeans=super.processRoworderCol(disbean);
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(ulrdbean==null) return lstCreatedColBeans;
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        for(String rowordertypeTmp:Consts.lstAllRoworderTypes)
        {
            if(rowordertypeTmp.equals(Consts.ROWORDER_DRAG)) continue;
            if(alrbean.getLstRoworderTypes()==null||!alrbean.getLstRoworderTypes().contains(rowordertypeTmp))
            {
                ulrdbean.removeChildColBeanByColumn(getRoworderColColumnByRoworderType(rowordertypeTmp),true);
            }
        }
        if(lstCreatedColBeans!=null&&ulrdbean.getLstChildren()!=null&&ulrdbean.getLstChildren().size()>0)
        {
            for(ColBean cbCreatedTmp:lstCreatedColBeans)
            {
                UltraListReportColBean ulrcbean=new UltraListReportColBean(cbCreatedTmp);
                cbCreatedTmp.setExtendConfigDataForReportType(KEY,ulrcbean);
                ulrdbean.getLstChildren().add(cbCreatedTmp);
            }
        }
        return lstCreatedColBeans;
    }

    protected Map<String,ColAndGroupTitlePositionBean> calPosition(ReportBean reportbean,List lstChildren,List<String> lstDisplayColIds,boolean isForPage)
    {
        Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions=new HashMap<String,ColAndGroupTitlePositionBean>();
        int maxrowspan=calPositionStart(lstChildren,lstDisplayColIds,mColAndGroupTitlePostions,isForPage);
        if(maxrowspan==0) return new HashMap<String,ColAndGroupTitlePositionBean>();
        calPositionEnd(lstChildren,mColAndGroupTitlePostions,maxrowspan);
        //将本次要显示的标题部分最大行数存在一个特殊键MAX_TITLE_ROWSPANS中
        ColAndGroupTitlePositionBean positionTmp=new ColAndGroupTitlePositionBean();
        positionTmp.setRowspan(maxrowspan);
        mColAndGroupTitlePostions.put(MAX_TITLE_ROWSPANS,positionTmp);
        return mColAndGroupTitlePostions;
    }

    private int calPositionStart(List lstChildren,List<String> lstDisplayColIds,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,boolean isForPage)
    {
        ColBean cbTmp;
        UltraListReportGroupBean groupBeanTmp;
        int maxrowspan=0;
        ColAndGroupTitlePositionBean positionBeanTmp;
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
                positionBeanTmp.setDisplaymode(cbTmp.getDisplaymode(rrequest,lstDisplayColIds,isForPage));
                if(maxrowspan==0&&positionBeanTmp.getDisplaymode()>0)
                {
                    maxrowspan=1;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                int[] spans=groupBeanTmp.calPositionStart(rrequest,mColAndGroupTitlePostions,lstDisplayColIds,isForPage);
                if(mColAndGroupTitlePostions.get(groupBeanTmp.getGroupid()).getDisplaymode()>0&&spans[1]>maxrowspan)
                {
                    maxrowspan=spans[1];
                }
            }
        }
        return maxrowspan;
    }

    private void calPositionEnd(List lstChildren,Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions,int maxrowspan)
    {
        ColBean cbTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                positionBeanTmp.setLayer(0);
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    positionBeanTmp.setRowspan(maxrowspan);
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                ((UltraListReportGroupBean)objTmp).calPositionEnd(mColAndGroupTitlePostions,new int[] { maxrowspan, 0 });
            }
        }
    }

    protected void calPositionForStandardExcel(List lstChildren,List<String> lstDynColids,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        if(lstChildren==null||lstChildren.size()==0) return;
        lstChildren=sortChildrenByDynColOrders(lstChildren,lstDynColids,mColAndGroupTitlePostions);//如果用户进行了列拖动的话，先按要求排好序
        ColBean cbTmp;
        UltraListReportGroupBean groupBeanTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        int startcolidx=0;
        for(Object objTmp:lstChildren)
        {
            if(objTmp instanceof ColBean)
            {
                cbTmp=(ColBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(cbTmp.getColid());
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    positionBeanTmp.setStartcolindex(startcolidx);
                    positionBeanTmp.setStartrowindex(0);
                    startcolidx++;
                }
            }else if(objTmp instanceof UltraListReportGroupBean)
            {
                groupBeanTmp=(UltraListReportGroupBean)objTmp;
                positionBeanTmp=mColAndGroupTitlePostions.get(groupBeanTmp.getGroupid());
                if(positionBeanTmp.getDisplaymode()>0)
                {
                    groupBeanTmp.calPositionForStandardExcel(this,mColAndGroupTitlePostions,lstDynColids,new int[] { 0, startcolidx });
                    startcolidx+=positionBeanTmp.getColspan();
                }
            }
        }
    }
}
