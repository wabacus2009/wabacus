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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.CellStyle;
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
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.component.application.report.extendconfig.IGroupExtendConfigLoad;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.StandardExcelAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportSubDisplayBean;
import com.wabacus.system.component.application.report.configbean.ColAndGroupTitlePositionBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportGroupBean;
import com.wabacus.system.component.application.report.configbean.crosslist.AbsCrossListReportColAndGroupBean;
import com.wabacus.system.component.application.report.configbean.crosslist.CrossListReportBean;
import com.wabacus.system.component.application.report.configbean.crosslist.CrossListReportColBean;
import com.wabacus.system.component.application.report.configbean.crosslist.CrossListReportGroupBean;
import com.wabacus.system.component.application.report.configbean.crosslist.CrossListReportStatiBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.dataset.select.common.AbsCommonDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class CrossListReportType extends UltraListReportType implements IGroupExtendConfigLoad
{
    private static Log log=LogFactory.getLog(CrossListReportType.class);

    public final static String KEY=CrossListReportType.class.getName();

    private int dynColGroupIndexId=10000;

    private List lstAllDisplayColAndGroupBeans;

    private Map<String,RuntimeDynamicDatasetBean> mDynamicDatasetBeans;

    private List<VerticalCrossStatisticColData> lstVerticalStatiColBeansAndValues;

    public CrossListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    private boolean hasLoadedData=false;

    public boolean isLoadedReportData()
    {
        return this.hasLoadedData;
    }

    public void setHasLoadedDataFlag(boolean hasLoadedDataFlag)
    {
        super.setHasLoadedDataFlag(hasLoadedDataFlag);
        this.hasLoadedData=hasLoadedDataFlag;
    }

    public int generateColGroupIdxId()
    {
        return dynColGroupIndexId++;
    }

    public void loadReportData(boolean shouldInvokePostaction)
    {
        if(this.hasLoadedData) return;
        this.hasLoadedData=true;
        CrossListReportBean csrbean=(CrossListReportBean)rbean.getExtendConfigDataForReportType(KEY);
        if(csrbean==null||!csrbean.isHasDynamicColGroupBean())
        {
            super.loadReportData(shouldInvokePostaction);
            return;
        }
        super.initLoadReportData();//不管要不要加载数据，先初始化一下，因为如果本报表是依赖细览报表的从报表，则要为后面的刷新在URL中准备数据，因为后面可能只刷新此报表
        List<ColBean> lstAllRuntimeColBeans=new ArrayList<ColBean>();
        this.cacheDataBean.setLstDynOrderColBeans(lstAllRuntimeColBeans);
        this.lstAllDisplayColAndGroupBeans=new ArrayList();
        List lstConfigChildren=getLstConfigChildren(rbean.getDbean());
        Map<String,Boolean> mDynamicColGroupDisplayType=getMDynamicColGroupDisplayType(lstConfigChildren);
        getAllRuntimeColGroupBeans(lstConfigChildren,this.lstAllDisplayColAndGroupBeans,lstAllRuntimeColBeans,mDynamicColGroupDisplayType);
        processFixedColsAndRows(rbean);//处理冻结行列标题的情况
        if(csrbean.isShouldCreateRowSelectCol()&&rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            super.insertRowSelectNewCols(alrbean,lstAllRuntimeColBeans);
        }
        super.loadReportData(shouldInvokePostaction);
        createVerticalStaticColBeansAndData(lstAllRuntimeColBeans);
    }

    public void getAllRuntimeColGroupBeans(List lstConfigChildren,List lstAllRuntimeDisplayChildren,List<ColBean> lstAllRuntimeColBeans,
            Map<String,Boolean> mDynamicColGroupDisplayType)
    {
        AbsCrossListReportColAndGroupBean clrcgbeanTmp;
        for(Object childColGroupBeanTmp:lstConfigChildren)
        {
            if(!(childColGroupBeanTmp instanceof ColBean)&&!(childColGroupBeanTmp instanceof UltraListReportGroupBean)) continue;
            clrcgbeanTmp=ListReportAssistant.getInstance().getCrossColAndGroupBean(childColGroupBeanTmp);
            if(clrcgbeanTmp==null||(!clrcgbeanTmp.isDynamicColGroup()&&!clrcgbeanTmp.hasDynamicColGroupChild()))
            {
                if(childColGroupBeanTmp instanceof ColBean)
                {
                    lstAllRuntimeColBeans.add((ColBean)childColGroupBeanTmp);
                    if(((ColBean)childColGroupBeanTmp).getDisplaytype(rrequest)!=Consts.COL_DISPLAYTYPE_HIDDEN)
                    {
                        lstAllRuntimeDisplayChildren.add(childColGroupBeanTmp);
                    }
                }else if(childColGroupBeanTmp instanceof UltraListReportGroupBean)
                {
                    ((UltraListReportGroupBean)childColGroupBeanTmp).getAllColBeans(lstAllRuntimeColBeans,null);
                    lstAllRuntimeDisplayChildren.add(childColGroupBeanTmp);
                }
            }else
            {
                clrcgbeanTmp.getRuntimeColGroupBeans(this,lstAllRuntimeDisplayChildren,lstAllRuntimeColBeans,mDynamicColGroupDisplayType);
            }
        }
    }

    private Map<String,Boolean> getMDynamicColGroupDisplayType(List lstConfigChildren)
    {
        Map<String,Boolean> mDynamicColGroupDisplayType=new HashMap<String,Boolean>();
        AbsCrossListReportColAndGroupBean clrcgbeanTmp;
        for(Object childColGroupBeanTmp:lstConfigChildren)
        {
            if(!(childColGroupBeanTmp instanceof ColBean)&&!(childColGroupBeanTmp instanceof UltraListReportGroupBean)) continue;
            clrcgbeanTmp=ListReportAssistant.getInstance().getCrossColAndGroupBean(childColGroupBeanTmp);
            if(clrcgbeanTmp==null||(!clrcgbeanTmp.isDynamicColGroup()&&!clrcgbeanTmp.hasDynamicColGroupChild()))
            {//如果当前列是普通列
                continue;
            }else
            {
                clrcgbeanTmp.getMDynamicColGroupDisplayType(rrequest,mDynamicColGroupDisplayType);
            }
        }
        return mDynamicColGroupDisplayType;
    }

    private void createVerticalStaticColBeansAndData(List<ColBean> lstAllRuntimeColBeans)
    {
        boolean hasVerticalStatiData=false;
        for(Entry<String,RuntimeDynamicDatasetBean> entryTmp:this.mDynamicDatasetBeans.entrySet())
        {
            hasVerticalStatiData|=entryTmp.getValue().loadVerticalStatiData();
        }
        if(!hasVerticalStatiData) return;
        this.lstVerticalStatiColBeansAndValues=new ArrayList<VerticalCrossStatisticColData>();
        int colspans=0;
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PDF&&rrequest.getShowtype()!=Consts.DISPLAY_ON_PLAINEXCEL&&alrdbean.getRowGroupColsNum()>0
                &&alrdbean.getRowgrouptype()==2)
        {
            colspans=1;
        }
        AbsCrossListReportColAndGroupBean crossColGroupBeanTmp;
        CrossListReportColBean crcbeanTmp;
        List<AbsCrossListReportColAndGroupBean> lstNotDisplayStatiLabelBeans=new ArrayList<AbsCrossListReportColAndGroupBean>();//存放还没有显示label的统计列
        ColBean cbTmp;
        ColAndGroupTitlePositionBean positionBeanTmp;
        AbsListReportColBean alrcbeanTmp;
        Boolean isPrevFixedCol=null;
        for(int i=0;i<lstAllRuntimeColBeans.size();i++)
        {
            cbTmp=lstAllRuntimeColBeans.get(i);
            positionBeanTmp=this.mRuntimeColGroupPositionBeans.get(cbTmp.getColid());
            if(positionBeanTmp==null||positionBeanTmp.getDisplaymode()<=0) continue;
            if("[DYN_COL_DATA]".equals(cbTmp.getProperty()))
            {
                isPrevFixedCol=false;
                crcbeanTmp=(CrossListReportColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                crossColGroupBeanTmp=crcbeanTmp.getBelongToRootOwner();
                if(crossColGroupBeanTmp.isCommonCrossColGroup()||!crossColGroupBeanTmp.getInnerDynamicColBean().isHasVerticalstatistic())
                {//对于普通动态列，或者不针对每列数据进行垂直统计的交叉统计列，则跟普通列是一样处理
                    colspans++;
                }else
                {
                    lstNotDisplayStatiLabelBeans.add(crossColGroupBeanTmp);
                    if(colspans>0)
                    {
                        createVerticalStatiLabelColBean(lstNotDisplayStatiLabelBeans,colspans);
                        lstNotDisplayStatiLabelBeans.clear();
                        colspans=0;
                    }
                    crossColGroupBeanTmp.getVerticalStatisticColBeanAndData(this,lstAllRuntimeColBeans);
                    String currentCrossStatiColGroupid=crossColGroupBeanTmp.getRootCrossColGroupId();
                    int j=i;
                    for(;j<lstAllRuntimeColBeans.size();j++)
                    {
                        cbTmp=lstAllRuntimeColBeans.get(j);
                        crcbeanTmp=(CrossListReportColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                        if(crcbeanTmp==null||crcbeanTmp.getBelongToRootOwner()==null
                                ||!currentCrossStatiColGroupid.equals(crcbeanTmp.getBelongToRootOwner().getRootCrossColGroupId()))
                        {
                            break;
                        }
                    }
                    i=j-1;
                }
            }else
            {
                alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PDF&&rrequest.getShowtype()!=Consts.DISPLAY_ON_PLAINEXCEL&&alrcbeanTmp.isRowgroup()
                        &&alrdbean.getRowgrouptype()==2)
                {//是树形分组报表的分组列，且当前不是显示在plainexcel和pdf中，则这里不colspan++，因为在最开始的时候已经加了（这种列必须会配置在最前面，所以isPrevFixedCol=null）
                    isPrevFixedCol=alrcbeanTmp!=null&&alrcbeanTmp.isFixedCol(rrequest);
                }else if(isPrevFixedCol==null)
                {
                    colspans++;
                    isPrevFixedCol=alrcbeanTmp!=null&&alrcbeanTmp.isFixedCol(rrequest);
                }else if(isPrevFixedCol)
                {
                    if(alrcbeanTmp!=null&&alrcbeanTmp.isFixedCol(rrequest))
                    {
                        colspans++;
                    }else
                    {//当前列不是冻结列，将冻结列和非冻结列分在不同的ColBean中
                        createVerticalStatiLabelColBean(lstNotDisplayStatiLabelBeans,colspans);
                        lstNotDisplayStatiLabelBeans.clear();
                        colspans=1;
                        isPrevFixedCol=false;
                    }
                }else
                {
                    colspans++;
                    //当前列不是冻结列，则后面所有列都视为非冻结列，所以不再为isPrevFixedCol变量赋值
                }
            }
        }
        if(colspans>0) createVerticalStatiLabelColBean(lstNotDisplayStatiLabelBeans,colspans);
        for(Entry<String,RuntimeDynamicDatasetBean> entryTmp:this.mDynamicDatasetBeans.entrySet())
        {
            entryTmp.getValue().closeResultSet();
        }
    }

    protected void getRuntimeColAndGroupPosition()
    {
        this.mRuntimeColGroupPositionBeans=calPosition(rbean,this.lstAllDisplayColAndGroupBeans,null,rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE);
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PLAINEXCEL)
        {
            calPositionForStandardExcel(lstAllDisplayColAndGroupBeans,null,mRuntimeColGroupPositionBeans);
        }
    }
    
    public void addVerticalCrossStatisticColData(ColBean cbean,Object displayvalue,int colspan)
    {
        if(displayvalue==null) displayvalue="";
        this.lstVerticalStatiColBeansAndValues.add(new VerticalCrossStatisticColData(cbean,String.valueOf(displayvalue),colspan));
    }

    private void createVerticalStatiLabelColBean(List<AbsCrossListReportColAndGroupBean> lstNotDisplayStatiLabelBeans,int colspan)
    {
        if(lstNotDisplayStatiLabelBeans==null||lstNotDisplayStatiLabelBeans.size()==0)
        {//到目前为止所有交叉统计列都已经显示了verticallabel，则此普通列只要显示空字符串
            ColBean cbTmp=new ColBean(rbean.getDbean());
            cbTmp.setValuestyleproperty(" colspan=\""+colspan+"\"",true);
            this.lstVerticalStatiColBeansAndValues.add(new VerticalCrossStatisticColData(cbTmp,"",colspan));
        }else
        {
            ColBean cbTmp=null;
            String displayvalue;
            for(int i=0;i<lstNotDisplayStatiLabelBeans.size();i++)
            {
                displayvalue=rrequest.getI18NStringValue(lstNotDisplayStatiLabelBeans.get(i).getInnerDynamicColBean().getVerticallabel(rrequest));
                if(colspan<=0)
                {//已经没有多余的单元格来显示剩下的统计标题了，则和前面的合并
                    this.lstVerticalStatiColBeansAndValues.get(this.lstVerticalStatiColBeansAndValues.size()-1).appendColValue(displayvalue);
                }else
                {
                    cbTmp=new ColBean(rbean.getDbean());
                    String valuestyleproperty=lstNotDisplayStatiLabelBeans.get(i).getInnerDynamicColBean().getVerticallabelstyleproperty(rrequest,false);
                    valuestyleproperty=valuestyleproperty==null?"":valuestyleproperty.trim();
                    String strcolspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,false);
                    if(strcolspan==null||strcolspan.trim().equals("")) strcolspan="1";
                    int icolspan=Integer.parseInt(strcolspan);
                    if(icolspan>colspan||icolspan<colspan&&i==lstNotDisplayStatiLabelBeans.size()-1)
                    {
                        icolspan=colspan;
                    }
                    valuestyleproperty=Tools.removePropertyValueByName("colspan",valuestyleproperty);
                    valuestyleproperty=valuestyleproperty+" colspan=\""+icolspan+"\"";
                    cbTmp.setValuestyleproperty(valuestyleproperty,true);
                    this.lstVerticalStatiColBeansAndValues.add(new VerticalCrossStatisticColData(cbTmp,displayvalue,icolspan));
                    colspan-=icolspan;
                }
            }

        }
    }

    public void addDynamicSelectCols(AbsCrossListReportColAndGroupBean crossColGroupBean,String dynSelectCols)
    {
        String datasetid=crossColGroupBean.getDatasetid();
        datasetid=datasetid==null||datasetid.trim().equals("")?Consts.DEFAULT_KEY:datasetid.trim();
        if(this.mDynamicDatasetBeans==null) this.mDynamicDatasetBeans=new HashMap<String,RuntimeDynamicDatasetBean>();
        RuntimeDynamicDatasetBean dynDatasetBean=this.mDynamicDatasetBeans.get(datasetid);
        if(dynDatasetBean==null)
        {
            dynDatasetBean=new RuntimeDynamicDatasetBean(crossColGroupBean.getDatasetBean());
            this.mDynamicDatasetBeans.put(datasetid,dynDatasetBean);
        }
        dynDatasetBean.addSelectColsOfCrossColGroup(crossColGroupBean.getRootCrossColGroupId(),dynSelectCols);
    }

    public Map<String,String> getMDynamicSelectCols(ReportDataSetValueBean dsvbean)
    {
        String datasetid=dsvbean.getId();
        datasetid=datasetid==null||datasetid.trim().equals("")?Consts.DEFAULT_KEY:datasetid.trim();
        if(this.mDynamicDatasetBeans==null||this.mDynamicDatasetBeans.get(datasetid)==null) return null;
        return this.mDynamicDatasetBeans.get(datasetid).mAllSelectCols;
    }
    
    public ResultSet getVerticalStatisticResultSet(AbsCrossListReportColAndGroupBean crossColGroupBean)
    {
        if(this.mDynamicDatasetBeans==null) return null;
        String datasetid=crossColGroupBean.getDatasetid();
        datasetid=datasetid==null||datasetid.trim().equals("")?Consts.DEFAULT_KEY:datasetid.trim();
        RuntimeDynamicDatasetBean dynDatasetBean=this.mDynamicDatasetBeans.get(datasetid);
        if(dynDatasetBean==null) return null;
        return dynDatasetBean.getVerticalStatiResultSet();
    }

    protected List getLstDisplayChildren(UltraListReportDisplayBean ulrdbean)
    {
        return this.lstAllDisplayColAndGroupBeans;
    }

    protected List<ColBean> getLstAllRealColBeans()
    {
        return this.cacheDataBean.getLstDynOrderColBeans();
    }
    
    protected String getLastDisplayColIdInFirstTitleRow(UltraListReportDisplayBean urldbean)
    {
        return "[CROSSLISTREPORT]";
    }

    protected String showSubRowDataForWholeReport(int position)
    {
        StringBuffer resultBuf=new StringBuffer();
        if(position==AbsListReportSubDisplayBean.SUBROW_POSITION_BOTTOM&&this.lstVerticalStatiColBeansAndValues!=null
                &&this.lstVerticalStatiColBeansAndValues.size()>0)
        {//对动态列的交叉统计只显示在下面
            resultBuf.append("<tr  class='cls-data-tr'>");
            for(VerticalCrossStatisticColData vcDataTmp:this.lstVerticalStatiColBeansAndValues)
            {
                resultBuf.append("<td class='cls-data-td-list' ");
                resultBuf.append(vcDataTmp.getCbean().getValuestyleproperty(rrequest,false)).append(">");
                resultBuf.append(vcDataTmp.getColValue());
                resultBuf.append("</td>");
            }
            resultBuf.append("</tr>");
        }
        resultBuf.append(super.showSubRowDataForWholeReport(position));
        return resultBuf.toString();
    }

    protected void showSubRowDataInPlainExcelForWholeReport(Workbook workbook,CellStyle dataCellStyle,int position)
    {
        if(position==AbsListReportSubDisplayBean.SUBROW_POSITION_BOTTOM&&this.lstVerticalStatiColBeansAndValues!=null
                &&this.lstVerticalStatiColBeansAndValues.size()>0)
        {
            String stativalue;
            int startcolidx=0;
            int endcolidx=-1;
            CellRangeAddress region;
            for(VerticalCrossStatisticColData vcDataTmp:this.lstVerticalStatiColBeansAndValues)
            {
                stativalue=vcDataTmp.getColValue();
                stativalue=Tools.replaceAll(stativalue,"&nbsp;"," ");
                stativalue=stativalue.replaceAll("<.*?\\>","");
                startcolidx=endcolidx+1;
                endcolidx=startcolidx+vcDataTmp.getColspan()-1;
                region=new CellRangeAddress(excelRowIdx,excelRowIdx,startcolidx,endcolidx);
                StandardExcelAssistant.getInstance().setRegionCellStringValue(workbook,excelSheet,region,dataCellStyle,stativalue);
            }
            excelRowIdx++;
        }
        super.showSubRowDataInPlainExcelForWholeReport(workbook,dataCellStyle,position);
    }

    protected void showSubRowDataOnPdfForWholeReport(int position)
    {
        if(position==AbsListReportSubDisplayBean.SUBROW_POSITION_BOTTOM&&this.lstVerticalStatiColBeansAndValues!=null
                &&this.lstVerticalStatiColBeansAndValues.size()>0)
        {
            String stativalue;
            int startcolidx=0;
            int endcolidx=-1;
            CellRangeAddress region;
            for(VerticalCrossStatisticColData vcDataTmp:this.lstVerticalStatiColBeansAndValues)
            {
                stativalue=vcDataTmp.getColValue();
                stativalue=Tools.replaceAll(stativalue,"&nbsp;"," ");
                stativalue=stativalue.replaceAll("<.*?\\>","");//替换掉html标签
                startcolidx=endcolidx+1;
                endcolidx=startcolidx+vcDataTmp.getColspan();
                addDataCell(vcDataTmp.getCbean(),stativalue,1,endcolidx-startcolidx,Element.ALIGN_LEFT);
            }
        }
        super.showSubRowDataOnPdfForWholeReport(position);
    }

    public String getColSelectedMetadata()
    {
        return "";
    }

    public List sortChildrenByDynColOrders(List lstChildren,List<String> lstDynColids,
            Map<String,ColAndGroupTitlePositionBean> colAndGroupTitlePostions)
    {
        return lstChildren;
    }

    public boolean isSupportHorizontalDataset(ReportBean reportbean)
    {
        return false;
    }
    
    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        XmlElementBean eleColBean=lstEleColBeans.get(0);
        CrossListReportColBean clrcbean=(CrossListReportColBean)colbean.getExtendConfigDataForReportType(KEY);
        if(clrcbean==null)
        {
            clrcbean=new CrossListReportColBean(colbean);
            colbean.setExtendConfigDataForReportType(KEY,clrcbean);
        }
        loadCrossColAndGroupCommonConfig(eleColBean,clrcbean);
        String verticalstatistic=eleColBean.attributeValue("verticalstatistic");
        String verticallabel=eleColBean.attributeValue("verticallabel");
        String verticallabelstyleproperty=eleColBean.attributeValue("verticallabelstyleproperty");
        clrcbean.setHasVerticalstatistic(verticalstatistic!=null&&verticalstatistic.trim().equalsIgnoreCase("true"));
        verticallabel=verticallabel==null?"":Config.getInstance().getResourceString(null,colbean.getPageBean(),verticallabel,true).trim();
        clrcbean.setVerticallabel(verticallabel);
        clrcbean.setVerticallabelstyleproperty(verticallabelstyleproperty==null?"":verticallabelstyleproperty.trim(),false);
        if(colbean.getLstDatasetValueids()!=null&&colbean.getLstDatasetValueids().size()>1)
        {
            throw new WabacusConfigLoadingException("加载交叉报表"+colbean.getReportBean().getPath()+"失败，不能为动态列"+colbean.getColumn()+"配置多个数据集<value/>");
        }
        clrcbean.setDatasetid(colbean.getLstDatasetValueids()==null?null:colbean.getLstDatasetValueids().get(0));
        //        {
        //            throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，为交叉<col/>配置column属性不能为"
        List<XmlElementBean> lstEleChildren=eleColBean.getLstChildElementsByName("statistic");
        if(lstEleChildren!=null&&lstEleChildren.size()>0)
        {
            List<CrossListReportStatiBean> lstStatisBeans=new ArrayList<CrossListReportStatiBean>();
            clrcbean.setLstStatisBeans(lstStatisBeans);
            CrossListReportStatiBean statisBean;
            List<String> lstExistIds=new ArrayList<String>();
            for(XmlElementBean eleStaticBeanTmp:lstEleChildren)
            {
                statisBean=new CrossListReportStatiBean(colbean);
                String id=eleStaticBeanTmp.attributeValue("id");
                if(id==null||id.trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载交叉报表"+colbean.getReportBean().getPath()+"失败，没有为<statistic/>配置id");
                }
                id=id.trim();
                if(lstExistIds.contains(id))
                {
                    throw new WabacusConfigLoadingException("加载交叉报表"+colbean.getReportBean().getPath()+"失败，<statistic/>的id值"+id+"存在重复");
                }
                lstExistIds.add(id);
                statisBean.setId(colbean.getColumn()+"."+id);//加上column前缀，方便保持唯一性，尤其是授权的时候，不会与其它<col/>的column有冲突
                String type=eleStaticBeanTmp.attributeValue("type");
                String column=eleStaticBeanTmp.attributeValue("column");
                String label=eleStaticBeanTmp.attributeValue("label");
                String labelstyleproperty=eleStaticBeanTmp.attributeValue("labelstyleproperty");
                String valuestyleproperty=eleStaticBeanTmp.attributeValue("valuestyleproperty");
                String statitems=eleStaticBeanTmp.attributeValue("statitems");
                statisBean.setType(type==null?"":type.toLowerCase().trim());
                statisBean.setColumn(column==null?"":column.trim());
                statisBean.setLstLabels(Tools
                        .parseAllStringToList(Config.getInstance().getResourceString(null,colbean.getPageBean(),label,false),"|"));
                statisBean.setLstLabelstyleproperties(Tools.parseAllStringToList(labelstyleproperty,"|"));
                statisBean.setLstValuestyleproperties(Tools.parseAllStringToList(valuestyleproperty,"|"));
                statisBean.setDatatypeObj(ConfigLoadAssistant.loadDataType(eleStaticBeanTmp));
                if(statitems!=null)
                {
                    statitems=statitems.trim();
                    if(statitems.equals(""))
                    {
                        statisBean.setLstStatitems(null);
                    }else
                    {
                        List<String> lstTemp=new UniqueArrayList<String>();
                        lstTemp.addAll(Tools.parseAllStringToList(statitems,"|"));
                        statisBean.setLstStatitems(lstTemp);
                    }
                }
                statisBean.validateConfig();
                lstStatisBeans.add(statisBean);
            }
        }
        super.afterColLoading(colbean,lstEleColBeans);
        return 1;
    }

    public int beforeGroupLoading(UltraListReportGroupBean groupbean,List<XmlElementBean> lstEleGroupBeans)
    {
        CrossListReportGroupBean clrgbean=(CrossListReportGroupBean)groupbean.getExtendConfigDataForReportType(KEY);
        if(clrgbean==null)
        {
            clrgbean=new CrossListReportGroupBean(groupbean);
            groupbean.setExtendConfigDataForReportType(KEY,clrgbean);
        }
        XmlElementBean eleGroupBean=lstEleGroupBeans.get(0);
        String column=eleGroupBean.attributeValue("column");
        if(column!=null)
        {
            column=column.trim();
            if(!column.equals(""))
            {
                int idx=column.indexOf(".");
                if(idx>0)
                {
                    clrgbean.setDatasetid(column.substring(0,idx).trim());
                    column=column.substring(idx+1).trim();
                }
            }
            clrgbean.setColumn(column.trim());
        }
        loadCrossColAndGroupCommonConfig(eleGroupBean,clrgbean);
        return 1;
    }

    private void loadCrossColAndGroupCommonConfig(XmlElementBean eleColGroupBean,AbsCrossListReportColAndGroupBean clrcgbean)
    {
        String staticondition=eleColGroupBean.attributeValue("staticondition"); 
        if(staticondition!=null&&!staticondition.trim().equals(""))
        {
            clrcgbean.setStaticondition(staticondition.trim());
            clrcgbean.setLstStatiConditions(ComponentConfigLoadManager.loadCommonDatasetConditios(clrcgbean.getOwner().getReportBean(),
                    eleColGroupBean.getChildElementByName("staticonditions")));
        }
        String realvalue=eleColGroupBean.attributeValue("realvalue");
        if(realvalue!=null) clrcgbean.setRealvalue(realvalue.trim());
        String dataset=eleColGroupBean.attributeValue("dataset");
        if(dataset!=null)
        {
            AbsCommonDataSetValueProvider providerObj=AbsCommonDataSetValueProvider.createCommonDataSetValueProviderObj(clrcgbean.getOwner().getReportBean(),dataset);
            clrcgbean.setTitleDatasetProvider(providerObj);
            if(providerObj==null) return;//此列不是动态列
            providerObj.setOwnerCrossReportColAndGroupBean(clrcgbean);
            providerObj.loadConfig(eleColGroupBean);
            XmlElementBean eleHeaderFormatBean=eleColGroupBean.getChildElementByName("format");
            if(eleHeaderFormatBean!=null)
            {
                XmlElementBean eleFormatValueBean=eleHeaderFormatBean.getChildElementByName("value");
                if(eleFormatValueBean!=null)
                {
                    String formatmethod=eleFormatValueBean.getContent();
                    if(formatmethod==null||formatmethod.trim().equals(""))
                    {//如果将<value/>配置为空
                        clrcgbean.setDataHeaderPojoClass(null);
                        clrcgbean.setDataheaderformatContent(null);
                    }else
                    {
                        List<XmlElementBean> lstEleFormatBeans=new ArrayList<XmlElementBean>();
                        lstEleFormatBeans.add(eleHeaderFormatBean);
                        //这里把要生成的format内容先存起来，等在doPostLoad()方法中生成类，因为如果在这里生成，则对于定义了<group/>的报表，定义在<group/>之外的动态<col/>会生成两次字节码（因为会加载两次）导致出错
                        clrcgbean.setLstDataHeaderFormatImports(ComponentConfigLoadManager.getListImportPackages(lstEleFormatBeans));
                        clrcgbean.setDataheaderformatContent(formatmethod.trim());
                    }
                }
            }
        }
    }

    public int afterGroupLoading(UltraListReportGroupBean groupbean,List<XmlElementBean> lstEleGroupBeans)
    {
        return 0;
    }

    public int afterReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        CrossListReportBean cslrbean=(CrossListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(cslrbean==null)
        {
            cslrbean=new CrossListReportBean(reportbean);
            reportbean.setExtendConfigDataForReportType(KEY,cslrbean);
        }
        reportbean.setCelldrag(0);
        return super.afterReportLoading(reportbean,lstEleReportBeans);
    }

    public int doPostLoad(ReportBean reportbean)
    {
        if(reportbean.getSbean().isHorizontalDataset())
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，横向数据集不能配置为交叉统计报表");
        }
        DisplayBean disbean=reportbean.getDbean();
        disbean.setPageColselect(false);
        disbean.setDataexportColselect(false);
        reportbean.setCelldrag(0);
        List lstChildren=getLstConfigChildren(disbean);
        for(Object childObjTmp:lstChildren)
        {
            processCrossColAndGroupBeansStart(childObjTmp,null);
        }
        CrossListReportBean cslrbean=(CrossListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(!cslrbean.isHasDynamicColGroupBean()) return super.doPostLoad(reportbean);
        for(Object childObjTmp:lstChildren)
        {
            processCrossColAndGroupBeansEnd(childObjTmp,null);
        }
        for(ReportDataSetBean dsbeanTmp:reportbean.getSbean().getLstDatasetBeans())
        {
            for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
            {
                dsvbeanTmp.getProvider().doPostLoadCrosslist();
            }
        }
        super.doPostLoad(reportbean);
        return 1;
    }

    private List getLstConfigChildren(DisplayBean disbean)
    {
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(UltraListReportType.KEY);
        List lstChildren=null;
        if(ulrdbean==null||!ulrdbean.isHasGroupConfig(null))
        {//如果不是列分组显示的报表
            lstChildren=disbean.getLstCols();
        }else
        {
            lstChildren=ulrdbean.getLstChildren();
        }
        return lstChildren;
    }

    private void processCrossColAndGroupBeansStart(Object colAndGroupBean,CrossListReportGroupBean parentGroupBean)
    {
        if(!(colAndGroupBean instanceof ColBean)&&!(colAndGroupBean instanceof UltraListReportGroupBean)) return;
        AbsCrossListReportColAndGroupBean clrcgbeanTmp=ListReportAssistant.getInstance().getCrossColAndGroupBean(colAndGroupBean);
        if(clrcgbeanTmp==null) return;
        clrcgbeanTmp.setParentCrossGroupBean(parentGroupBean);
        clrcgbeanTmp.processColGroupRelationStart();
        if(colAndGroupBean instanceof UltraListReportGroupBean)
        {
            for(Object childObjTmp:((UltraListReportGroupBean)colAndGroupBean).getLstChildren())
            {
                processCrossColAndGroupBeansStart(childObjTmp,(CrossListReportGroupBean)clrcgbeanTmp);
            }
        }
    }

    private void processCrossColAndGroupBeansEnd(Object colAndGroupBean,CrossListReportGroupBean parentGroupBean)
    {
        if(!(colAndGroupBean instanceof ColBean)&&!(colAndGroupBean instanceof UltraListReportGroupBean)) return;
        AbsCrossListReportColAndGroupBean clrcgbeanTmp=ListReportAssistant.getInstance().getCrossColAndGroupBean(colAndGroupBean);
        if(clrcgbeanTmp==null) return;
        clrcgbeanTmp.processColGroupRelationEnd();
        if(colAndGroupBean instanceof UltraListReportGroupBean)
        {
            for(Object childObjTmp:((UltraListReportGroupBean)colAndGroupBean).getLstChildren())
            {
                processCrossColAndGroupBeansEnd(childObjTmp,(CrossListReportGroupBean)clrcgbeanTmp);
            }
        }
    }

    protected void processFixedColsAndRows(ReportBean reportbean)
    {
        if(rrequest==null||rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return;//加载配置文件时不处理，在运行时根据生成好的列进行处理
        List<ColBean> lstRuntimeColBeans=this.cacheDataBean.getLstDynOrderColBeans();
        if(lstRuntimeColBeans==null||lstRuntimeColBeans.size()==0) return;
        int fixedcols=this.alrbean.getFixedcols(null);
        if(fixedcols>0)
        {
            int cnt=0;
            for(ColBean cbTmp:lstRuntimeColBeans)
            {
                if(cbTmp.getDisplaytype(true)==Consts.COL_DISPLAYTYPE_HIDDEN) continue;
                cnt++;
            }
            if(cnt<=fixedcols)
            {
                fixedcols=0;
                alrbean.setFixedcols(rrequest,0);
            }
        }
        if(fixedcols>0)
        {
            AbsListReportColBean alrcbeanTmp;
            int cnt=0;
            for(ColBean cbTmp:lstRuntimeColBeans)
            {
                if(cbTmp.getDisplaytype(true)==Consts.COL_DISPLAYTYPE_HIDDEN) continue;
                if(cbTmp.isRowSelectCol())
                {
                    throw new WabacusConfigLoadingException("对于冻结列标题的交叉显示报表，如果加载报表"+reportbean.getPath()
                            +"失败,在<report/>的fixedcols中配置的冻结列数包括了行选中列，这样不能正常选中行");
                }
                alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(alrcbeanTmp==null)
                {
                    alrcbeanTmp=new AbsListReportColBean(cbTmp);
                    cbTmp.setExtendConfigDataForReportType(AbsListReportType.KEY,alrcbeanTmp);
                }
                alrcbeanTmp.setFixedCol(rrequest,true);
                if(++cnt==fixedcols) break;
            }
        }
        if(fixedcols>0||alrbean.getFixedrows()>0)
        {
            if(alrdbean!=null&&alrdbean.getRowgrouptype()==2&&alrdbean.getRowGroupColsNum()>0)
            {//树形分组报表
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,树形分组报表不能冻结行列标题");
            }
        }
    }

    protected ColBean[] insertRowSelectNewCols(AbsListReportBean alrbean,List<ColBean> lstCols)
    {
        CrossListReportBean cslrbean=(CrossListReportBean)alrbean.getOwner().getReportBean().getExtendConfigDataForReportType(KEY);
        if(!cslrbean.isHasDynamicColGroupBean()) return super.insertRowSelectNewCols(alrbean,lstCols);
        cslrbean.setShouldCreateRowSelectCol(true);
        return null;
    }

    protected Map<String,ColAndGroupTitlePositionBean> calPosition(ReportBean reportbean,List lstChildren,List<String> lstDisplayColIds,boolean isForPage)
    {
        if(rrequest==null) return null;
        return super.calPosition(reportbean,lstChildren,lstDisplayColIds,isForPage);
    }

    protected void calPositionForStandardExcel(List lstChildren,List<String> lstDynColids,
            Map<String,ColAndGroupTitlePositionBean> mColAndGroupTitlePostions)
    {
        if(rrequest==null) return;//在加载的时候不计算位置，因为动态列是否显示，显示多少列都没有定，所以统一放在运行时计算
        super.calPositionForStandardExcel(lstChildren,lstDynColids,mColAndGroupTitlePostions);
    }

    private class RuntimeDynamicDatasetBean
    {
        private ReportDataSetValueBean dsvbean;

        private Map<String,String> mAllSelectCols;

        private ResultSet verticalStatiResultSet;

        public RuntimeDynamicDatasetBean(ReportDataSetValueBean dsvbean)
        {
            this.dsvbean=dsvbean;
        }

        public void addSelectColsOfCrossColGroup(String colgroupid,String selectcols)
        {
            if(mAllSelectCols==null) mAllSelectCols=new HashMap<String,String>();
            mAllSelectCols.put(colgroupid,selectcols);
        }

        public ResultSet getVerticalStatiResultSet()
        {
            return verticalStatiResultSet;
        }

        public boolean loadVerticalStatiData()
        {
            if(this.dsvbean.getProvider() instanceof SQLReportDataSetValueProvider)
            {
                verticalStatiResultSet=((SQLReportDataSetValueProvider)this.dsvbean.getProvider()).loadCrossListReportVerticalStatiResultSet(rrequest);
            }
            return verticalStatiResultSet!=null;
        }

        public void closeResultSet()
        {
            try
            {
                if(verticalStatiResultSet!=null) verticalStatiResultSet.close();
            }catch(SQLException e)
            {
                log.warn("关闭报表"+rbean.getPath()+"的针对每列数据进行垂直统计的记录集失败",e);
            }
        }
    }

    private class VerticalCrossStatisticColData
    {
        private ColBean cbean;

        private String colValue;

        private int colspan;//当前列占据多少个单元格，供导出到plainexcel和pdf时使用

        public VerticalCrossStatisticColData(ColBean cbean,String colValue,int colspan)
        {
            this.cbean=cbean;
            if(colValue==null) colValue="";
            this.colValue=colValue;
            this.colspan=colspan;
        }

        public ColBean getCbean()
        {
            return cbean;
        }

        public String getColValue()
        {
            return colValue;
        }

        public int getColspan()
        {
            return colspan;
        }

        public void appendColValue(String colValue)
        {
            if(this.colValue==null) this.colValue="";
            if(colValue==null||colValue.trim().equals("")) return;
            this.colValue+=" "+colValue;
        }
    }
}
