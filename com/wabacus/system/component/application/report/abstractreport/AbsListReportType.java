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
package com.wabacus.system.component.application.report.abstractreport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.FormatBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.ListReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.StandardExcelAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.commoninterface.IListReportRoworderPersistence;
import com.wabacus.system.component.application.report.UltraListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportDisplayBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportFilterBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportRowGroupSubDisplayRowBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportSubDisplayBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportSubDisplayColBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportSubDisplayRowBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.statistic.StatisticItemBean;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.system.component.application.report.configbean.UltraListReportColBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportGroupBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public abstract class AbsListReportType extends AbsReportType
{
    public final static String KEY=AbsListReportType.class.getName();

    private final static Log log=LogFactory.getLog(AbsListReportType.class);

    protected AbsListReportBean alrbean;

    protected AbsListReportDisplayBean alrdbean;

    protected Object subDisplayDataObj;

    protected Map<String,Object> mRowGroupSubDisplayDataObj;

    public AbsListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            alrbean=(AbsListReportBean)((ReportBean)comCfgBean).getExtendConfigDataForReportType(KEY);
            alrdbean=(AbsListReportDisplayBean)((ReportBean)comCfgBean).getDbean().getExtendConfigDataForReportType(KEY);
        }
    }

    public Object getSubDisplayDataObj()
    {
        return subDisplayDataObj;
    }

    public Map<String,Object> getMRowGroupSubDisplayDataObj()
    {
        return mRowGroupSubDisplayDataObj;
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        ReportBean reportbean=(ReportBean)applicationConfigBean;
        super.initUrl(reportbean,rrequest);
        String colFilterId=rrequest.getStringAttribute(reportbean.getId()+"_COL_FILTERID","");
        if(!colFilterId.equals(""))
        {
            AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
            AbsListReportFilterBean filterbean=alrdbean.getFilterBeanById(colFilterId);
            if(filterbean!=null&&!filterbean.isConditionRelate())
            {//是不与查询条件关联的过滤列(与查询条件关联的列过滤放在查询条件中处理)
                String filterVal=rrequest.getStringAttribute(colFilterId,"");
                if(!filterVal.trim().equals(""))
                {
                    rrequest.addParamToUrl(colFilterId,filterVal,true);
                    rrequest.addParamToUrl(reportbean.getId()+"_COL_FILTERID",colFilterId,true);
                }
            }
        }
        String reportid=reportbean.getId();
        rrequest.addParamToUrl(reportid+"ORDERBY","rrequest{"+reportid+"ORDERBY}",true);

        rrequest.addParamToUrl(reportid+"_DYNCOLUMNORDER","rrequest{"+reportid+"_DYNCOLUMNORDER}",true);
    }

    public void initReportBeforeDoStart()
    {
        super.initReportBeforeDoStart();
        rrequest.setFilterCondition(rbean);
        String orderby=null;
        String orderbyAction=rrequest.getStringAttribute(rbean.getId()+"ORDERBY_ACTION","");
        if(orderbyAction.equals("true"))
        {
            orderby=rrequest.getStringAttribute(rbean.getId()+"ORDERBY","");
            if(rbean.getPersonalizeObj()!=null)
            {
                rbean.getPersonalizeObj().storeOrderByCol(rrequest,rbean,orderby);
            }
        }else
        {
            if(rbean.getPersonalizeObj()!=null)
            {
                orderby=rbean.getPersonalizeObj().loadOrderByCol(rrequest,rbean);
            }
            if(orderby==null||orderby.trim().equals(""))
            {
                orderby=rrequest.getStringAttribute(rbean.getId()+"ORDERBY","");
            }
        }
        if(!orderby.equals(""))
        {
            List<String> lstTemp=Tools.parseStringToList(orderby,"||",false);
            if(lstTemp==null||lstTemp.size()!=2)
            {
                log.error("URL中传入的排序字段"+orderby+"不合法，必须为字段名+||+(asc|desc)格式");
            }else
            {
                String[] str=new String[2];
                str[0]=lstTemp.get(0);
                str[1]=lstTemp.get(1);
                if(str[0]==null) str[0]="";
                if(str[1]==null||str[1].trim().equals("")||(!str[1].equalsIgnoreCase("desc")&&!str[1].equalsIgnoreCase("asc")))
                {
                    str[1]="asc";
                }
                rrequest.setAttribute(rbean.getId(),"ORDERBYARRAY",str);
            }
        }
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        List<String> lstColIds=null;
        String dyncolumnorder=null;
        if(rbean.getPersonalizeObj()!=null)
        {
            dyncolumnorder=rbean.getPersonalizeObj().loadColOrderData(rrequest,rbean);
        }
        if(dyncolumnorder==null||dyncolumnorder.trim().equals(""))
        {
            dyncolumnorder=rrequest.getStringAttribute(rbean.getId()+"_DYNCOLUMNORDER","");
        }
        if(!dyncolumnorder.equals(""))
        {
            lstColIds=Tools.parseStringToList(dyncolumnorder,";",false);
        }
        String dragcols=rrequest.getStringAttribute(rbean.getId()+"_DRAGCOLS","");
        if(!dragcols.equals(""))
        {
            if(lstColIds==null)
            {
                lstColIds=new ArrayList<String>();
                for(ColBean cbTmp:rbean.getDbean().getLstCols())
                {
                    lstColIds.add(cbTmp.getColid());
                }
            }
            lstColIds=processDragCols(dragcols,lstColIds);
        }
        if(lstColIds!=null&&lstColIds.size()>0)
        {//需要使用动态顺序
            List<ColBean> lstColBeansDyn=new ArrayList<ColBean>();
            ColBean cbTmp;
            StringBuffer dynColOrderBuf=new StringBuffer();
            for(String colidTmp:lstColIds)
            {
                cbTmp=rbean.getDbean().getColBeanByColId(colidTmp);
                if(cbTmp==null)
                {
                    throw new WabacusRuntimeException("在报表"+rbean.getPath()+"中没有取到colid为"+colidTmp+"的ColBean对象");
                }
                dynColOrderBuf.append(colidTmp).append(";");
                lstColBeansDyn.add(cbTmp);
            }
            cdb.setLstDynOrderColBeans(lstColBeansDyn);
            if(!dragcols.equals(""))
            {
                rrequest.addParamToUrl(rbean.getId()+"_DYNCOLUMNORDER",dynColOrderBuf.toString(),true);
                if(rbean.getPersonalizeObj()!=null) rbean.getPersonalizeObj().storeColOrderData(rrequest,rbean,dynColOrderBuf.toString());
            }
        }
    }

    private List<String> processDragCols(String dragcols,List<String> lstColIds)
    {
        String dragdirect=rrequest.getStringAttribute(rbean.getId()+"_DRAGDIRECT","1");
        if(!dragdirect.equals("1")&&!dragdirect.equals("-1")) dragdirect="1";
        String[] dragcolsArr=dragcols.split(";");
        if(dragcolsArr==null||dragcolsArr.length!=2)
        {
            log.warn("传入的移动列数据不合法，移动报表列失败");
            return lstColIds;
        }
        List<String> lstFromColids=new ArrayList<String>();
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(UltraListReportType.KEY);
        if(dragcolsArr[0].indexOf("group_")==0)
        {//被拖动的列是个分组列
            if(ulrdbean==null)
            {
                log.warn("当前报表没有配置列分组，但传入的移动列ID为分组ID，移动报表列失败");
                return lstColIds;
            }
            UltraListReportGroupBean groupBean=ulrdbean.getGroupBeanById(dragcolsArr[0]);
            if(groupBean==null)
            {
                log.warn("没有取到id为"+dragcolsArr[0]+"的列分组，移动报表列失败");
                return lstColIds;
            }
            groupBean.getAllChildColIdsInerit(lstFromColids);
        }else
        {
            lstFromColids.add(dragcolsArr[0]);
        }
        String targetColid=dragcolsArr[1];
        if(targetColid.indexOf("group_")==0)
        {
            UltraListReportGroupBean groupBean=ulrdbean.getGroupBeanById(targetColid);
            if(groupBean==null)
            {
                log.warn("没有取到id为"+targetColid+"的列分组，移动报表列失败");
                return lstColIds;
            }
            if(dragdirect.equals("1"))
            {
                targetColid=groupBean.getLastColId(lstColIds);
            }else
            {
                targetColid=groupBean.getFirstColId(lstColIds);
            }
        }
        List<String> lstColIdsNew=new ArrayList<String>();
        for(String colidTmp:lstColIds)
        {
            if(lstFromColids.contains(colidTmp)) continue;//被移动的列稍后加进去
            if(targetColid.equals(colidTmp))
            {
                if(dragdirect.equals("1"))
                {
                    lstColIdsNew.add(colidTmp);
                    lstColIdsNew.addAll(lstFromColids);
                }else
                {
                    lstColIdsNew.addAll(lstFromColids);
                    lstColIdsNew.add(colidTmp);
                }
            }else
            {
                lstColIdsNew.add(colidTmp);
            }
        }
        return lstColIdsNew;
    }

    protected void initReportAfterDoStart()
    {
        super.initReportAfterDoStart();
        if(rrequest.getSaveSlaveReportIdsSet().contains(rbean.getId()))
        {
            EditableReportAssistant.getInstance().doAllReportsSaveAction(rrequest);
        }
    }

    public void loadLazyReportData(int startRownum,int endRownum)
    {
        this.lstReportData=ListReportAssistant.getInstance().loadLazyReportDataSet(rrequest,this,startRownum,endRownum);
        if(this.lstReportData==null) this.lstReportData=new ArrayList<AbsReportDataPojo>();
        super.doLoadReportDataPostAction();
    }
    
    protected void doLoadReportDataPostAction()
    {
        loadSubDisplayDataForWholeReport();
        super.doLoadReportDataPostAction();
//        if(this.rbean.getSbean().isHorizontalDataset()&&alrbean.getFixedcols(null)>0)
//        {//配置了冻结列标题
    }

    protected boolean isHiddenCol(ColBean cbean)
    {
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE))) return true;
        int displaymodeTmp=cacheDataBean.getColDisplayModeAfterAuthorize(cbean,rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE);
        return displaymodeTmp<=0;
    }
    
    private void loadSubDisplayDataForWholeReport()
    {
        AbsListReportSubDisplayBean subdisplayBean=this.alrbean.getSubdisplaybean();
        if(subdisplayBean==null) return;
        //                &&cacheDataBean.getFinalPageno()!=cacheDataBean.getPagecount()&&!alrdbean.getStatibean().isExistDisplayPerpageStatiRow())
        //        {//如果当前报表是分页显示，且不存在每页都显示的统计行，且当前不是显示最后一页
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&cacheDataBean.getPagesize()<0||rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE
                &&!this.cacheDataBean.isExportPrintPartData())
        {
            if(subdisplayBean.isAllDisplayPerpageDataRows()) return;
        }
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&this.cacheDataBean.isExportPrintPartData()||rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE
                &&cacheDataBean.getPagesize()>0)
        {//当前是打印/导出或在页面上显示分页报表的一页数据
            if(cacheDataBean.getFinalPageno()!=cacheDataBean.getPagecount()&&subdisplayBean.isAllDisplayWholeReportDataRows()) return;//如果所有统计行都是显示在分页报表的最后一行，且当前不是最后一行
        }
        this.subDisplayDataObj=subdisplayBean.getPojoObject();
        loadSubDisplayDataObj(subdisplayBean,this.subDisplayDataObj,null,null);
    }

    protected void loadSubDisplayDataObj(AbsListReportSubDisplayBean subdisplayBean,Object subdisplayDataObj,
            Map<String,String> mGroupColAndParentColValues,AbsListReportRowGroupSubDisplayRowBean srgbean)
    {
        try
        {
            if(subdisplayBean.getLstStatitemBeans()!=null)
            {
                Map<String,Object> mStatitemValues=new HashMap<String,Object>();
                for(ReportDataSetBean dsbeanTmp:rbean.getSbean().getLstDatasetBeans())
                {
                    for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
                    {
                        Map<String,Object> mStatitemValuesLocal=dsvbeanTmp.getProvider().getStatisticDataSet(rrequest,srgbean,mGroupColAndParentColValues);//存放当前<value/>中加载出的统计数据
                        if(mStatitemValuesLocal!=null) mStatitemValues.putAll(mStatitemValuesLocal);
                    }
                }
                for(StatisticItemBean sItemBeanTmp:subdisplayBean.getLstStatitemBeans())
                {
                    if(sItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                            ||sItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_REPORT)
                    {
                        sItemBeanTmp.getSetMethod().invoke(subdisplayDataObj,new Object[] { mStatitemValues.get(sItemBeanTmp.getProperty()) });
                    }
                    if(sItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                            ||sItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_PAGE)
                    {
                        sItemBeanTmp.getPageStatiSetMethod().invoke(subdisplayDataObj,
                                new Object[] { mStatitemValues.get("page_"+sItemBeanTmp.getProperty()) });
                    }
                }
            }
            Method formatMethod=subdisplayBean.getPojoclass().getMethod("format",new Class[] { ReportRequest.class, ReportBean.class, String.class });
            formatMethod.invoke(subdisplayDataObj,new Object[] { rrequest, rbean, srgbean==null?"":srgbean.getRowgroupcolumn() });
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("设置报表"+rbean.getPath()+"的辅助显示数据失败",e);
        }
    }

    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        String rowselecttype=this.alrbean.getRowSelectType();
        if(rowselecttype!=null&&!rowselecttype.trim().equals(""))
        {//行选中类型
            resultBuf.append(" rowselecttype=\"").append(rowselecttype).append("\"");
            resultBuf.append(" isSelectRowCrossPages=\"").append(this.alrbean.isSelectRowCrossPages()).append("\"");
            if(!rowselecttype.trim().equalsIgnoreCase(Consts.ROWSELECT_NONE))
            {
                List<String> lstRowSelectCallBackFuns=this.alrbean.getLstRowSelectCallBackFuncs();
                if(lstRowSelectCallBackFuns!=null&&lstRowSelectCallBackFuns.size()>0)
                {
                    StringBuffer rowSelectMethodBuf=new StringBuffer();
                    rowSelectMethodBuf.append("{rowSelectMethods:[");
                    for(String callbackFunc:lstRowSelectCallBackFuns)
                    {
                        if(callbackFunc!=null&&!callbackFunc.trim().equals(""))
                        {
                            rowSelectMethodBuf.append("{value:").append(callbackFunc).append("},");
                        }
                    }
                    if(rowSelectMethodBuf.charAt(rowSelectMethodBuf.length()-1)==',') rowSelectMethodBuf.deleteCharAt(rowSelectMethodBuf.length()-1);
                    rowSelectMethodBuf.append("]}");
                    resultBuf.append(" rowSelectMethods=\"").append(rowSelectMethodBuf.toString()).append("\"");
                }
            }
        }
        return resultBuf.toString();
    }

    protected String showScrollStartTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        int scrolltype=this.alrbean.getScrollType();
        StringBuffer resultBuf=new StringBuffer();
        if(scrolltype==AbsListReportBean.SCROLLTYPE_ALL)
        {
            return ComponentAssistant.getInstance().showComponentScrollStartPart(rbean,true,true,rbean.getScrollwidth(),rbean.getScrollheight(),
                    rbean.getScrollstyle());
        }else if(scrolltype==AbsListReportBean.SCROLLTYPE_FIXED)
        {
            resultBuf.append("<div style=\"overflow:hidden;");
            if(rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals(""))
            {
                resultBuf.append("width:").append(rbean.getScrollwidth()).append(";");
            }
            if(rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals(""))
            {
                resultBuf.append("height:").append(rbean.getScrollheight()).append(";");
            }
            resultBuf.append("\">");

        }else if(scrolltype==AbsListReportBean.SCROLLTYPE_HORIZONTAL)
        {//只显示横向滚动条
            if(Consts_Private.SCROLLSTYLE_NORMAL.equals(rbean.getScrollstyle()))
            {
                resultBuf.append("<div><div onmouseover=\"this.style.height='100%'\" style=\"width:").append(rbean.getScrollwidth()).append(";");
                resultBuf.append("overflow-x:auto;overflow-y:hidden;height:expression(this.scrollHeight+15);\"");
            }else if(Consts_Private.SCROLLSTYLE_IMAGE.equals(rbean.getScrollstyle()))
            {
                resultBuf.append("<div  style=\"width:").append(rbean.getScrollwidth()).append(";");
                resultBuf.append("overflow-x:hidden;overflow-y:hidden;\"");
                resultBuf.append(" id=\"hscroll_"+rbean.getGuid()+"\"");
            }
            resultBuf.append(">");
        }
        return resultBuf.toString();
    }

    protected String showScrollEndTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        int scrolltype=this.alrbean.getScrollType();
        if(scrolltype==AbsListReportBean.SCROLLTYPE_ALL)
        {//显示普通纵横滚动条
            return ComponentAssistant.getInstance().showComponentScrollEndPart(rbean.getScrollstyle(),true,true);
        }else if(scrolltype==AbsListReportBean.SCROLLTYPE_FIXED)
        {
            return "</div>";
        }else if(scrolltype==AbsListReportBean.SCROLLTYPE_HORIZONTAL)
        {
            if(Consts_Private.SCROLLSTYLE_NORMAL.equals(rbean.getScrollstyle()))
            {//如果是只显示横向普通滚动条，因为为了兼容IE9在外面套了一层<div>，所以这里要返回两个<div/>
                return "</div></div>";
            }else
            {
                return "</div>";
            }
        }
        return "";
    }

    protected StringBuilder dataPartStringBuffer;
    
    protected void checkAndPrintBufferData(int rowindex)
    {
        if(this.alrbean.getBatchouputcount()>0&&rowindex>0&&rowindex%this.alrbean.getBatchouputcount()==0)
        {
            this.wresponse.print(dataPartStringBuffer.toString());
            dataPartStringBuffer.delete(0,dataPartStringBuffer.length());
        }
    }
    
    public abstract void showReportData(boolean showtype,StringBuilder resultBuf);

    protected String getDefaultNavigateKey()
    {
        return Consts.LISTREPORT_NAVIGATE_DEFAULT;
    }

    //    public String showNavigateBox()
    //        int pagesize=cdb.getPagesize();
    //        {//不分页或依赖其它报表的翻页导航栏
    //        }
    //        {//从数据库中没有取到数据，注意不能用recordcount==0判断，因为在可编辑列表报表中，recordcount为0时可能还要显示一页或多页的添加的行。
    //                .getString(rbean.getPageBean(),Consts.NAVIGATE_COUNT_INFO,true),rrequest);
    //
    //        navigate_count_info=Tools.replaceAll(navigate_count_info,"#totalpage#",String
    //        rtnStr.append("<table border='0'");
    //        }
    //        rtnStr.append("</td>");
    //            String buttonStr=rbean.getButtonsBean().showButtons(rrequest,Consts.NAVIGATE_PART);
    //                        "</td>");
    //        }
    //        rtnStr.append("<td>&nbsp;</td>");
    //            rtnStr.append("              "+navigate_prevpage_label+"&nbsp; "
    //            rtnStr.append("  <select name=\"SELEPAGENUM\"><option>1</option></select>");
    //            {//当前为第一页
    //                rtnStr.append(navigate_prevpage_label
    //            {//当前为最后一页
    //                rtnStr.append(WabacusUtil.getNavigateInfo(pagenum-1,navigate_prevpage_label,rbean,
    //            {//中间页
    //                        rrequest)
    //
    //        rtnStr.append("</td></tr></table>");

    public List<ColBean> getLstDisplayColBeans()
    {
        List<ColBean> lstColBeans=this.cacheDataBean.getLstDynOrderColBeans();
        if(lstColBeans==null||lstColBeans.size()==0) lstColBeans=rbean.getDbean().getLstCols();
        return lstColBeans;
    }

    //    /**
    //     * 从拦截器中获取某一行的样式字符串信息
    //     */
    //        if(rbean.getInterceptor()!=null)
    //            {//拦截器中返回了<tr/>的样式字符串
    //            }

    protected String showSubRowDataForWholeReport(int position)
    {
        AbsListReportSubDisplayBean subdisplayBean=this.alrbean.getSubdisplaybean();
        if(subdisplayBean==null) return "";
        List<AbsListReportSubDisplayRowBean> lstSubDisplayRowBeans=subdisplayBean.getLstSubDisplayRowBeans();
        if(lstSubDisplayRowBeans==null||lstSubDisplayRowBeans.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        for(AbsListReportSubDisplayRowBean sRowBeanTmp:lstSubDisplayRowBeans)
        {
            if(sRowBeanTmp.getDisplaytype()==AbsListReportSubDisplayBean.SUBROW_DISPLAYTYPE_PAGE)
            {
                if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&!this.cacheDataBean.isExportPrintPartData()) continue;//当前是打印或导出，且导出或打印全部数据
                if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&cacheDataBean.getPagesize()<0) continue;
            }else if(sRowBeanTmp.getDisplaytype()==AbsListReportSubDisplayBean.SUBROW_DISPLAYTYPE_REPORT)
            {
                if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&this.cacheDataBean.isExportPrintPartData()
                        ||rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&cacheDataBean.getPagesize()>0)
                {
                    if(cacheDataBean.getFinalPageno()!=cacheDataBean.getPagecount()) continue;
                }
            }
            if(sRowBeanTmp.getDisplayposition()!=AbsListReportSubDisplayBean.SUBROW_POSITION_BOTH&&sRowBeanTmp.getDisplayposition()!=position)
                continue;
            resultBuf.append(showOneSubRowData(sRowBeanTmp));
        }
        return resultBuf.toString();
    }

    private String showOneSubRowData(AbsListReportSubDisplayRowBean sRowBean)
    {
        List<AbsListReportSubDisplayColBean> lstStatiColBeans=sRowBean.getLstSubColBeans();
        if(lstStatiColBeans==null||lstStatiColBeans.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<tr  class='cls-data-tr'>");
        ColDisplayData colDisplayData;
        String stativalue=null;
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&rbean.getDbean().isPageColselect()
                ||rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&rbean.getDbean().isDataexportColselect()
                ||lstStatiColBeans.size()==1
                ||(cacheDataBean.getAttributes().get("authroize_col_display")!=null&&String.valueOf(
                        cacheDataBean.getAttributes().get("authroize_col_display")).trim().equals("false"))
                ||(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&alrbean.hasControllCol()))
        {//如果提供了列选择功能，或者只有一个统计列，或者对列进行了授权，则所有统计项显示在一列中，这一列占据了整行
            resultBuf.append("<td class='cls-data-td-list' ");
            int colspan=cacheDataBean.getTotalColCount();
            if(colspan<=0) return "";
            resultBuf.append(" colspan='").append(colspan).append("' ");
            StringBuffer statiContentBuf=new StringBuffer();
            String dyntdstyleproperty=null;
            for(AbsListReportSubDisplayColBean scbean:lstStatiColBeans)
            {
                if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,scbean.getProperty(),Consts.PERMISSION_TYPE_DISPLAY)) continue;
                stativalue=getSubColDisplayValue(this.subDisplayDataObj,scbean);
                colDisplayData=ColDisplayData.getColDataFromInterceptor(this,scbean,null,0,scbean.getValuestyleproperty(rrequest,false),stativalue);
                statiContentBuf.append(colDisplayData.getValue()).append("&nbsp;&nbsp;");
                if(dyntdstyleproperty==null)
                {
                    dyntdstyleproperty=Tools.removePropertyValueByName("colspan",colDisplayData.getStyleproperty());
                }
            }
            stativalue=statiContentBuf.toString().trim();
            if(stativalue.endsWith("&nbsp;&nbsp;")) stativalue=stativalue.substring(0,stativalue.length()-"&nbsp;&nbsp;".length()).trim();
            if(stativalue.equals("")) return "";//当前没有要显示的统计项
            if(dyntdstyleproperty!=null) resultBuf.append(dyntdstyleproperty);
            resultBuf.append(">").append(stativalue).append("</td>");
        }else
        {
            for(AbsListReportSubDisplayColBean scbean:lstStatiColBeans)
            {
                //                if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,scbean.getProperty(),Consts.PERMISSION_TYPE_DISPLAY)) continue;//当前统计项没有显示权限
                stativalue=getSubColDisplayValue(this.subDisplayDataObj,scbean);
                colDisplayData=ColDisplayData.getColDataFromInterceptor(this,scbean,null,0,scbean.getValuestyleproperty(rrequest,false),stativalue);
                resultBuf.append("<td class='cls-data-td-list' ");
                resultBuf.append(colDisplayData.getStyleproperty());
                resultBuf.append(">").append(colDisplayData.getValue()).append("</td>");
            }
        }
        resultBuf.append("</tr>");
        return resultBuf.toString();
    }

    protected String getSubColDisplayValue(Object statiDataObj,AbsListReportSubDisplayColBean scbean)
    {
        if(statiDataObj==null) return "";
        String stativalue;
        try
        {
            Object objTmp=scbean.getGetMethod().invoke(statiDataObj,new Object[] {});
            if(objTmp==null)
            {
                stativalue="";
            }else
            {
                stativalue=String.valueOf(objTmp);
            }
        }catch(Exception e)
        {
            log.error("获取报表"+rbean.getPath()+"统计数据失败",e);
            stativalue="";
        }
        return stativalue;
    }

    public String showColData(ColBean cbean,int rowidx)
    {
        if(this.lstReportData==null||this.lstReportData.size()==0) return "";
        if(rowidx==-1)
        {
            rowidx=this.lstReportData.size()-1;
        }else if(rowidx==-2)
        {
            int[] displayrowinfo=this.getDisplayRowInfo();
            if(displayrowinfo[1]<=0) return "";
            rowidx=displayrowinfo[0];
        }
        if(lstReportData.size()<=rowidx) return "";
        AbsReportDataPojo dataObj=this.lstReportData.get(rowidx);
        String strvalue=dataObj.getColStringValue(cbean);
        if(strvalue==null) strvalue="";
        return strvalue;
    }

    public void showReportOnPlainExcel(Workbook workbook)
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        if(this.cacheDataBean.getTotalColCount()==0) return;
        createNewSheet(workbook,10);
        showReportTitleOnPlainExcel(workbook);
        (new ExportDataToPlainExcelFile(workbook)).exportReportDataToFile();
    }

    protected void createNewSheet(Workbook workbook,int defaultcolumnwidth)
    {
        super.createNewSheet(workbook,defaultcolumnwidth);
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        int i=0;
        for(ColBean cbean:lstColBeans)
        {
            if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean,false)<=0) continue;
            //this.excelSheet.autoSizeColumn(i);
            if(cbean.getPlainexcelwidth()>1.0f)
            {
                this.excelSheet.setColumnWidth(i,(int)cbean.getPlainexcelwidth());
            }
            i++;
        }
    }

    protected void showReportTitleOnPlainExcel(Workbook workbook)
    {
        SqlBean sbean=rbean.getSbean();
        if(sbean.isHorizontalDataset()&&this.cacheDataBean.getColDisplayModeAfterAuthorize(sbean.getHdsTitleLabelCbean(),false)<=0)
        {
            return;
        }
        String plainexceltitle=null;
        if(this.pedebean!=null) plainexceltitle=this.pedebean.getPlainexceltitle();
        if("none".equals(plainexceltitle)) return;
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        CellStyle titleCellStyle=StandardExcelAssistant.getInstance().getTitleCellStyleForStandardExcel(workbook);
        Row dataTitleRow=excelSheet.createRow(excelRowIdx++);
        ColDisplayData colDisplayData;
        int cellidx=0;
        Cell cell;
        if(sbean.isHorizontalDataset()&&isShouldDisplayHdsLabelCol())
        {//当前是横向数据集，且需要显示标题列
            cell=dataTitleRow.createCell(cellidx++);
            cell.setCellType(Cell.CELL_TYPE_STRING);
            cell.setCellValue(sbean.getHdsTitleLabelCbean().getLabel(rrequest));
            cell.setCellStyle(StandardExcelAssistant.getInstance().setCellAlign(titleCellStyle,sbean.getHdsTitleLabelCbean().getLabelalign()));
        }
        String labelTmp;
        for(ColBean cbean:lstColBeans)
        {
            if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean,false)<=0) continue;
            if("column".equals(plainexceltitle))
            {
                labelTmp=cbean.getColumn();
            }else
            {
                labelTmp=cbean.getLabel(rrequest);
                colDisplayData=ColDisplayData.getColDataFromInterceptor(this,cbean,null,-1,null,labelTmp);
                labelTmp=colDisplayData.getValue();
            }
            cell=dataTitleRow.createCell(cellidx++);
            cell.setCellType(Cell.CELL_TYPE_STRING);
            cell.setCellValue(labelTmp);
            cell.setCellStyle(StandardExcelAssistant.getInstance().setCellAlign(titleCellStyle,cbean.getLabelalign()));
        }
        titleRowCount=1;
        //        System.out.println(csTmp.getUserStyleName());
    }
    
    protected void showSubRowDataInPlainExcelForWholeReport(Workbook workbook,CellStyle dataCellStyle,int position)
    {
        AbsListReportSubDisplayBean subDisplayBean=this.alrbean.getSubdisplaybean();
        if(subDisplayBean==null) return;
        List<AbsListReportSubDisplayRowBean> lstStatiDisplayRowBeans=subDisplayBean.getLstSubDisplayRowBeans();
        if(lstStatiDisplayRowBeans==null||lstStatiDisplayRowBeans.size()==0) return;

        List<AbsListReportSubDisplayColBean> lstStatiColBeans=null;
        for(AbsListReportSubDisplayRowBean sRowBeanTmp:lstStatiDisplayRowBeans)
        {
            /****if(sRowBeanTmp.getDisplaytype()==AbsListReportStatiBean.STATIROW_DISPLAYTYPE_PAGE)
            {
                if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&!this.cacheDataBean.isExportPrintPartData()) continue;
                if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&cacheDataBean.getPagesize()<0) continue;//当前是显示在页面上，且显示全部数据
            }else if(sRowBeanTmp.getDisplaytype()==AbsListReportStatiBean.STATIROW_DISPLAYTYPE_REPORT)
            {
                if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&this.cacheDataBean.isExportPrintPartData()
                        ||rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&cacheDataBean.getPagesize()>0)
                {
                    if(cacheDataBean.getFinalPageno()!=cacheDataBean.getPagecount()) continue;
                }
            }*/
            if(sRowBeanTmp.getDisplayposition()!=AbsListReportSubDisplayBean.SUBROW_POSITION_BOTH&&sRowBeanTmp.getDisplayposition()!=position)
                continue;
            lstStatiColBeans=sRowBeanTmp.getLstSubColBeans();
            if(lstStatiColBeans==null||lstStatiColBeans.size()==0) continue;
            String stativalue;
            int startcolidx=0;
            int endcolidx=-1;
            CellRangeAddress region;
            for(AbsListReportSubDisplayColBean scbean:lstStatiColBeans)
            {
                stativalue=getSubColDisplayValue(this.subDisplayDataObj,scbean);
                stativalue=Tools.replaceAll(stativalue,"&nbsp;"," ");
                stativalue=stativalue.replaceAll("<.*?\\>","");//替换掉html标签
                if(rbean.getDbean().isDataexportColselect()
                        ||lstStatiColBeans.size()==1
                        ||(cacheDataBean.getAttributes().get("authroize_col_display")!=null&&String.valueOf(
                                cacheDataBean.getAttributes().get("authroize_col_display")).trim().equals("false"))||alrbean.hasControllCol())
                {
                    startcolidx=0;
                    endcolidx=cacheDataBean.getTotalColCount()-1;
                    int deltaCount=0;
                    if(alrdbean.getRowGroupColsNum()>0&&alrdbean.getRowgrouptype()==2)
                    {
                        deltaCount=alrdbean.getRowGroupColsNum()-1;
                    }
                    endcolidx=endcolidx+deltaCount;
                }else
                {
                    startcolidx=endcolidx+1;
                    endcolidx=startcolidx+scbean.getPlainexcel_colspan()-1;
                }
                region=new CellRangeAddress(excelRowIdx,excelRowIdx,startcolidx,endcolidx);
                StandardExcelAssistant.getInstance().setRegionCellStringValue(workbook,excelSheet,region,dataCellStyle,stativalue);
            }
            excelRowIdx++;
        }
    }

    protected void showReportOnPdfWithoutTpl()
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        if(this.cacheDataBean.getTotalColCount()==0) return;
        createNewPdfPage();
        showDataHeaderOnPdf();
        (new ExportDataToPdfFile()).exportReportDataToFile();
    }

    private float[] colwidthArr;//存放导出PDF时各列的宽度值

    protected void createNewPdfPage()
    {
        super.createNewPdfPage();
        if(this.totalcolcount<=0) return;
        if(colwidthArr==null)
        {
            colwidthArr=new float[this.totalcolcount];
            float totalconfigwidth=0f;
            int nonconfigwidthColcnt=0;
            List<ColBean> lstColBeans=this.getLstDisplayColBeans();
            for(ColBean cbean:lstColBeans)
            {
                if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean,false)<=0) continue;
                if(cbean.getPdfwidth()>0.1f)
                {
                    totalconfigwidth+=cbean.getPdfwidth();
                }else
                {//没有在<col/>中配置dataexportwidth
                    nonconfigwidthColcnt++;
                }
            }
            float nonconfigcolwidth=0f;//存放没有配置宽度的列的宽度
            if(nonconfigwidthColcnt==0)
            {
                pdfwidth=totalconfigwidth;
                this.pdfDataTable.setTotalWidth(totalconfigwidth);
            }else
            {
                if(pdfwidth<=totalconfigwidth)
                {
                    nonconfigcolwidth=50f;//没有配置宽度的列设置默认宽度值为50
                    pdfwidth=totalconfigwidth+nonconfigcolwidth*nonconfigwidthColcnt;
                    this.pdfDataTable.setTotalWidth(pdfwidth);
                }else
                {
                    nonconfigcolwidth=(pdfwidth-totalconfigwidth)/nonconfigwidthColcnt;
                }
            }
            int i=0;
            for(ColBean cbean:lstColBeans)
            {
                if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean,false)<=0) continue;
                if(cbean.getPdfwidth()>0.1f)
                {
                    colwidthArr[i]=cbean.getPdfwidth();
                }else
                {
                    colwidthArr[i]=nonconfigcolwidth;
                }
                i++;
            }
        }
        try
        {
            this.pdfDataTable.setWidths(colwidthArr);
        }catch(DocumentException e)
        {
            throw new WabacusRuntimeException("导出报表"+rbean.getPath()+"的数据到PDF文件失败",e);
        }
    }

    protected void showDataHeaderOnPdf()
    {
        SqlBean sbean=rbean.getSbean();
        if(sbean.isHorizontalDataset()&&this.cacheDataBean.getColDisplayModeAfterAuthorize(sbean.getHdsTitleLabelCbean(),false)<=0)
        {
            return;
        }
        if(sbean.isHorizontalDataset()&&isShouldDisplayHdsLabelCol())
        {
            addDataHeaderCell(sbean.getHdsTitleLabelCbean(),sbean.getHdsTitleLabelCbean().getLabel(rrequest),1,1,getPdfCellAlign(sbean
                    .getHdsTitleLabelCbean().getLabelalign(),Element.ALIGN_CENTER));
        }
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        ColDisplayData colDisplayData;
        String labelTmp;
        for(ColBean cbean:lstColBeans)
        {
            if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean,false)<=0) continue;
            labelTmp=cbean.getLabel(rrequest);
            colDisplayData=ColDisplayData.getColDataFromInterceptor(this,cbean,null,-1,null,labelTmp);
            addDataHeaderCell(cbean,colDisplayData.getValue(),1,1,getPdfCellAlign(cbean.getLabelalign(),Element.ALIGN_CENTER));
        }
    }
    
    protected int getTotalColCount()
    {
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        int cnt=0;
        for(ColBean cbean:lstColBeans)
        {
            if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean,rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)<=0) continue;
            if(cbean.isControlCol()) continue;
            cnt++;
        }
        return cnt;
    }

    protected void showSubRowDataOnPdfForWholeReport(int position)
    {
        AbsListReportSubDisplayBean subDisplayBean=this.alrbean.getSubdisplaybean();
        if(subDisplayBean==null) return;
        List<AbsListReportSubDisplayRowBean> lstStatiDisplayRowBeans=subDisplayBean.getLstSubDisplayRowBeans();
        if(lstStatiDisplayRowBeans==null||lstStatiDisplayRowBeans.size()==0) return;
        //        Object subdisplayDataObj=subDisplayBean.getPojoObject();
        List<AbsListReportSubDisplayColBean> lstStatiColBeans=null;
        for(AbsListReportSubDisplayRowBean sRowBeanTmp:lstStatiDisplayRowBeans)
        {
            /****if(sRowBeanTmp.getDisplaytype()==AbsListReportStatiBean.STATIROW_DISPLAYTYPE_PAGE)
            {
                if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&!this.cacheDataBean.isExportPrintPartData()) continue;
                if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&cacheDataBean.getPagesize()<0) continue;
            }else if(sRowBeanTmp.getDisplaytype()==AbsListReportStatiBean.STATIROW_DISPLAYTYPE_REPORT)
            {
                if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&this.cacheDataBean.isExportPrintPartData()
                        ||rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&cacheDataBean.getPagesize()>0)
                {
                    if(cacheDataBean.getFinalPageno()!=cacheDataBean.getPagecount()) continue;//如果不是显示最后一页
                }
            }*/
            if(sRowBeanTmp.getDisplayposition()!=AbsListReportSubDisplayBean.SUBROW_POSITION_BOTH&&sRowBeanTmp.getDisplayposition()!=position)
                continue;
            lstStatiColBeans=sRowBeanTmp.getLstSubColBeans();
            if(lstStatiColBeans==null||lstStatiColBeans.size()==0) continue;
            String stativalue;
            int startcolidx=0;
            int endcolidx=-1;
            for(AbsListReportSubDisplayColBean scbean:lstStatiColBeans)
            {
                stativalue=getSubColDisplayValue(this.subDisplayDataObj,scbean);
                stativalue=Tools.replaceAll(stativalue,"&nbsp;"," ");
                stativalue=stativalue.replaceAll("<.*?\\>","");
                if(rbean.getDbean().isDataexportColselect()
                        ||lstStatiColBeans.size()==1
                        ||(cacheDataBean.getAttributes().get("authroize_col_display")!=null&&String.valueOf(
                                cacheDataBean.getAttributes().get("authroize_col_display")).trim().equals("false"))||alrbean.hasControllCol())
                {
                    startcolidx=0;
                    endcolidx=cacheDataBean.getTotalColCount();//取到当前参与显示的总列数
                    int deltaCount=0;
                    if(alrdbean.getRowGroupColsNum()>0&&alrdbean.getRowgrouptype()==2)
                    {
                        deltaCount=alrdbean.getRowGroupColsNum()-1;
                    }
                    endcolidx=endcolidx+deltaCount;
                }else
                {
                    startcolidx=endcolidx+1;
                    endcolidx=startcolidx+scbean.getPlainexcel_colspan();
                }
                addDataCell(scbean,stativalue,1,endcolidx-startcolidx,Element.ALIGN_LEFT);
            }
        }
    }

    protected boolean isShouldDisplayHdsLabelCol()
    {
        if(!rbean.getSbean().isHorizontalDataset()) return false;
        ColBean cbDataBeanTmp;
        for(int i=0;i<this.lstReportData.size();i++)
        {
            cbDataBeanTmp=this.lstReportData.get(i).getHdsDataColBean();
            if(cbDataBeanTmp!=null&&this.cacheDataBean.getColDisplayModeAfterAuthorize(cbDataBeanTmp,rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)>0
                    &&!Tools.isEmpty(cbDataBeanTmp.getLabel(rrequest)))
            {
                return true;
            }
        }
        return false;
    }
    
    private abstract class AbsExportDataToFile
    {
        protected List<ColBean> lstColBeans;

        protected void exportReportDataToFile()
        {
            boolean isHorizontalDataset=rbean.getSbean().isHorizontalDataset();
            boolean isShouldDisplayHdsLabelCol=isShouldDisplayHdsLabelCol();
            this.lstColBeans=getLstDisplayColBeans();
            int[] displayrowinfo=getDisplayRowInfo();
            if(displayrowinfo[1]<=0)
            {//没有数据，则导出一条空数据行
                exportOneRowDataToFile(ReportAssistant.getInstance().getPojoClassInstance(rrequest,rbean,rbean.getPojoClassObj()),
                        isHorizontalDataset,isShouldDisplayHdsLabelCol);
            }else
            {
                boolean isLazyLoadata=rbean.isLazyLoadReportData(rrequest);
                int lazyloadataCount=rbean.getLazyLoadDataCount(rrequest);
                int recordidx=-1, startRownum=0;
                if(isLazyLoadata&&!cacheDataBean.isLoadAllReportData())
                {
                    startRownum=(cacheDataBean.getFinalPageno()-1)*cacheDataBean.getPagesize();
                }
                if(!isHorizontalDataset)
                {
                    exportSubRowDataToFileForWholeReport(AbsListReportSubDisplayBean.SUBROW_POSITION_TOP);
                }
                AbsReportDataPojo rowDataObjTmp;
                for(int i=displayrowinfo[0];i<displayrowinfo[1];i++)
                {
                    if(isLazyLoadata&&i%lazyloadataCount==0)
                    {
                        loadLazyReportData(startRownum+i,startRownum+i+lazyloadataCount);
                        if(lstReportData.size()==0) break;
                        recordidx=-1;
                    }
                    recordidx=isLazyLoadata?(recordidx+1):i;
                    if(recordidx>=lstReportData.size()) break;
                    rowDataObjTmp=lstReportData.get(recordidx);
                    exportOneRowDataToFile(rowDataObjTmp,isHorizontalDataset,isShouldDisplayHdsLabelCol);
                }
                if(!isHorizontalDataset)
                {
                    exportSubRowDataToFileForWholeReport(AbsListReportSubDisplayBean.SUBROW_POSITION_BOTTOM);
                }
            }
        }

        protected abstract void exportOneRowDataToFile(AbsReportDataPojo rowDataObj,boolean isHorizontalDataset,
                boolean isShouldDisplayHdsLabelCol);

        protected abstract void exportSubRowDataToFileForWholeReport(int position);
    }
    
    private class ExportDataToPlainExcelFile extends AbsExportDataToFile
    {
        private Workbook workbook;

        private CellStyle dataCellStyle;

        private CellStyle dataCellStyleWithFormat;

        public ExportDataToPlainExcelFile(Workbook workbook)
        {
            this.workbook=workbook;
        }

        protected void exportReportDataToFile()
        {
            dataCellStyle=StandardExcelAssistant.getInstance().getDataCellStyleForStandardExcel(workbook);//获取数据行的样式对象
            dataCellStyleWithFormat=StandardExcelAssistant.getInstance().getDataCellStyleForStandardExcel(workbook);
            super.exportReportDataToFile();
        }

        protected void exportOneRowDataToFile(AbsReportDataPojo rowDataObj,boolean isHorizontalDataset,boolean isShouldDisplayHdsLabelCol)
        {
            if(isHorizontalDataset&&rowDataObj.getHdsDataColBean()!=null
                    &&cacheDataBean.getColDisplayModeAfterAuthorize(rowDataObj.getHdsDataColBean(),false)<=0)
            {//横向数据集，当前数据行对应的配置<col/>被授权为不显示
                return;
            }
            if(sheetsize>0&&(excelRowIdx-titleRowCount)>=sheetsize)
            {
                createNewSheet(workbook,10);
                showReportTitleOnPlainExcel(workbook);
            }
            Row dataRow=excelSheet.createRow(excelRowIdx++);
            Cell cell;
            int cellidx=0;
            if(isShouldDisplayHdsLabelCol)
            {//是横向数据集且需要在左侧显示一列各配置<col/>的label
                cell=dataRow.createCell(cellidx++);
                cell.setCellValue(rowDataObj.getHdsDataColBean().getLabel(rrequest));
                cell.setCellStyle(StandardExcelAssistant.getInstance().setCellAlign(dataCellStyle,rowDataObj.getHdsDataColBean().getValuealign()));
            }
            AbsListReportColBean alrcbeanTmp;
            Object objvalueTmp;
            ColBean cbeanOriginal;
            for(ColBean cbean:lstColBeans)
            {
                if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean,false)<=0) continue;
                cbeanOriginal=isHorizontalDataset&&rowDataObj.getHdsDataColBean()!=null&&!cbean.isSequenceCol()&&!cbean.isNonFromDbCol()
                        &&!cbean.isNonValueCol()&&!cbean.isControlCol()?rowDataObj.getHdsDataColBean():cbean;
                alrcbeanTmp=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(KEY);
                cell=dataRow.createCell(cellidx++);
                boolean flag=false;
                if(cbean.isSequenceCol())
                {
                    cell.setCellValue(excelGlobalRowIdx+alrcbeanTmp.getSequenceStartNum());
                }else if(!cbean.isControlCol())
                {
                    objvalueTmp=rowDataObj.getColValue(cbean);
                    flag=StandardExcelAssistant.getInstance().setCellValue(workbook,cbeanOriginal.getValuealign(),cell,objvalueTmp,
                            cbeanOriginal.getDatatypeObj(),dataCellStyleWithFormat);
                }
                if(!flag) cell.setCellStyle(StandardExcelAssistant.getInstance().setCellAlign(dataCellStyle,cbeanOriginal.getValuealign()));
            }
            excelGlobalRowIdx++;
        }

        protected void exportSubRowDataToFileForWholeReport(int position)
        {
            showSubRowDataInPlainExcelForWholeReport(workbook,dataCellStyle,position);
        }

    }
    
    private class ExportDataToPdfFile extends AbsExportDataToFile
    {
        protected void exportOneRowDataToFile(AbsReportDataPojo rowDataObj,boolean isHorizontalDataset,boolean isShouldDisplayHdsLabelCol)
        {
            if(isHorizontalDataset&&rowDataObj.getHdsDataColBean()!=null
                    &&cacheDataBean.getColDisplayModeAfterAuthorize(rowDataObj.getHdsDataColBean(),false)<=0)
            {//横向数据集，当前数据行对应的配置<col/>被授权为不显示
                return;
            }
            if(pdfpagesize>0&&pdfrowindex!=0&&pdfrowindex%pdfpagesize==0)
            {
                createNewPdfPage();
                if(isFullpagesplit) showDataHeaderOnPdf();//每页要显示所有内容，则显示一下标题
            }
            if(isShouldDisplayHdsLabelCol)
            {//是横向数据集且需要在左侧显示一列各配置<col/>的label
                addDataCell(rowDataObj.getHdsDataColBean(),rowDataObj.getHdsDataColBean().getLabel(rrequest),1,1,getPdfCellAlign(rowDataObj
                        .getHdsDataColBean().getValuealign(),Element.ALIGN_CENTER));
            }
            ColBean cbeanOriginal;
            AbsListReportColBean alrcbeanTmp;
            ColDisplayData colDisplayData;
            String valueTmp;
            for(ColBean cbean:lstColBeans)
            {
                if(cacheDataBean.getColDisplayModeAfterAuthorize(cbean,false)<=0) continue;
                cbeanOriginal=isHorizontalDataset&&rowDataObj.getHdsDataColBean()!=null&&!cbean.isSequenceCol()&&!cbean.isNonFromDbCol()
                        &&!cbean.isNonValueCol()&&!cbean.isControlCol()?rowDataObj.getHdsDataColBean():cbean;
                if(cbean.isSequenceCol())
                {
                    alrcbeanTmp=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
                    valueTmp=String.valueOf(pdfrowindex+alrcbeanTmp.getSequenceStartNum());
                }else if(!cbean.isControlCol())
                {
                    colDisplayData=ColDisplayData.getColDataFromInterceptor(AbsListReportType.this,cbean,null,pdfrowindex,null,rowDataObj.getColStringValue(cbean));
                    valueTmp=colDisplayData.getValue();
                }else
                {
                    valueTmp="";
                }
                addDataCell(cbean,valueTmp,1,1,getPdfCellAlign(cbeanOriginal.getValuealign(),Element.ALIGN_CENTER));
            }
            pdfrowindex++;
        }

        protected void exportSubRowDataToFileForWholeReport(int position)
        {
            showSubRowDataOnPdfForWholeReport(position);
        }
        
    }
    
    public int beforeReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        XmlElementBean eleReportBean=lstEleReportBeans.get(0);
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(alrbean==null)
        {
            alrbean=new AbsListReportBean(reportbean);
            reportbean.setExtendConfigDataForReportType(KEY,alrbean);
        }
        String fixedcols=eleReportBean.attributeValue("fixedcols");
        if(fixedcols!=null)
        {
            fixedcols=fixedcols.trim();
            if(fixedcols.equals(""))
            {
                alrbean.setFixedcols(null,0);
            }else
            {
                try
                {
                    alrbean.setFixedcols(null,Integer.parseInt(fixedcols));
                }catch(NumberFormatException e)
                {
                    log.warn("报表"+reportbean.getPath()+"的<report/>标签上fixedcols属性配置的值"+fixedcols+"为无效数字，"+e.toString());
                    alrbean.setFixedcols(null,0);
                }
            }
        }
        if(alrbean.getFixedcols(null)<0) alrbean.setFixedcols(null,0);
        String fixedrows=eleReportBean.attributeValue("fixedrows");
        if(fixedrows!=null)
        {
            fixedrows=fixedrows.trim();
            if(fixedrows.equals(""))
            {
                alrbean.setFixedrows(0);
            }else if(fixedrows.toLowerCase().equals("title"))
            {
                alrbean.setFixedrows(Integer.MAX_VALUE);
            }else
            {
                try
                {
                    alrbean.setFixedrows(Integer.parseInt(fixedrows));
                }catch(NumberFormatException e)
                {
                    log.warn("报表"+reportbean.getPath()+"的<report/>标签上fixedrows属性配置的值"+fixedrows+"为无效数字，"+e.toString());
                    alrbean.setFixedrows(0);
                }
            }
        }
        if(alrbean.getFixedrows()<0) alrbean.setFixedrows(0);
        String rowselect=eleReportBean.attributeValue("rowselect");
        if(rowselect!=null)
        {
            rowselect=rowselect.toLowerCase().trim();
            boolean isSelectRowCrossPages=false;
            int idx=rowselect.indexOf("|");
            if(idx>0)
            {
                isSelectRowCrossPages=rowselect.substring(idx+1).trim().equals("true");
                rowselect=rowselect.substring(0,idx).trim();
            }
            if(rowselect.equals(""))
            {
                alrbean.setRowSelectType(null);
            }else
            {
                if(!Consts.lstAllRowSelectTypes.contains(rowselect))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，配置的rowselect属性"+rowselect+"不合法");
                }
                alrbean.setRowSelectType(rowselect);
                alrbean.setSelectRowCrossPages(isSelectRowCrossPages);
            }
        }
        String rowselectcallback=eleReportBean.attributeValue("selectcallback");
        if(rowselectcallback!=null)
        {
            rowselectcallback=rowselectcallback.trim();
            if(rowselectcallback.equals(""))
            {
                alrbean.setLstRowSelectCallBackFuncs(null);
            }else
            {
                List<String> lstTemp=Tools.parseStringToList(rowselectcallback,";",false);
                for(String strfun:lstTemp)
                {
                    if(strfun==null||strfun.trim().equals("")) continue;
                    alrbean.addRowSelectCallBackFunc(strfun.trim());
                }
            }
        }
        String rowordertype=eleReportBean.attributeValue("rowordertype");
        if(rowordertype!=null)
        {
            rowordertype=rowordertype.toLowerCase().trim();
            if(rowordertype.equals(""))
            {
                alrbean.setLoadStoreRoworderObject(null);
                alrbean.setLstRoworderTypes(null);
            }else
            {
                List<String> lstRoworderTypes=new ArrayList<String>();
                List<String> lstTmp=Tools.parseStringToList(rowordertype,"|",false);
                for(String roworderTmp:lstTmp)
                {
                    if(roworderTmp==null||roworderTmp.trim().equals("")) continue;
                    roworderTmp=roworderTmp.trim();
                    if(!Consts.lstAllRoworderTypes.contains(roworderTmp))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，配置的rowordertype属性"+roworderTmp+"不支持");
                    }
                    if(!lstRoworderTypes.contains(roworderTmp)) lstRoworderTypes.add(roworderTmp);
                }
                if(lstRoworderTypes.size()==0)
                {
                    alrbean.setLoadStoreRoworderObject(null);
                    alrbean.setLstRoworderTypes(null);
                }else
                {
                    alrbean.setLstRoworderTypes(lstRoworderTypes);
                    String roworderclass=eleReportBean.attributeValue("roworderclass");
                    if(roworderclass!=null)
                    {
                        roworderclass=roworderclass.trim();
                        if(roworderclass.equals(""))
                        {
                            alrbean.setLoadStoreRoworderObject(null);
                        }else
                        {
                            Object obj=null;
                            try
                            {
                                obj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(roworderclass).newInstance();
                            }catch(Exception e)
                            {
                                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，无法实例化"+roworderclass+"类对象",e);
                            }
                            if(!(obj instanceof IListReportRoworderPersistence))
                            {
                                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，roworderclass属性配置的类"+roworderclass+"没有实现"
                                        +IListReportRoworderPersistence.class.getName()+"接口");
                            }
                            alrbean.setLoadStoreRoworderObject((IListReportRoworderPersistence)obj);
                        }
                    }
                    if(alrbean.getLoadStoreRoworderObject()==null&&Config.default_roworder_object==null)
                    {//没有配置局默认的处理行排序的对象
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()
                                +"失败，没有在wabacus.cfg.xml通过default-roworderclass配置项配置全局默认处理行排序的类，因此必须在其<report/>中配置roworderclass属性指定处理本报表行排序的类");
                    }
                }
            }
        }
        return 1;
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        XmlElementBean eleColBean=lstEleColBeans.get(0);
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)colbean.getParent().getExtendConfigDataForReportType(KEY);
        if(alrdbean==null)
        {
            alrdbean=new AbsListReportDisplayBean(colbean.getParent());
            colbean.getParent().setExtendConfigDataForReportType(KEY,alrdbean);
        }
        AbsListReportColBean alrcbean=(AbsListReportColBean)colbean.getExtendConfigDataForReportType(KEY);
        if(alrcbean==null)
        {
            alrcbean=new AbsListReportColBean(colbean);
            colbean.setExtendConfigDataForReportType(KEY,alrcbean);
        }
        ReportBean reportbean=colbean.getReportBean();
        String column=colbean.getColumn().trim();
        if(colbean.isNonValueCol())
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"为数据自动列表报表，不允许<col/>标签的column属性配置为"+Consts_Private.NON_VALUE);
        }
        if(colbean.isSequenceCol())
        {
            String sequence=column.substring(1,column.length()-1);
            int start=1;
            int idx=sequence.indexOf(":");
            if(idx>0)
            {
                sequence=sequence.substring(idx+1);
                try
                {
                    if(!sequence.trim().equals("")) start=Integer.parseInt(sequence);
                }catch(NumberFormatException e)
                {
                    log.warn("报表"+reportbean.getPath()+"配置的序号列"+colbean.getColumn()+"中的起始序号不是合法数字",e);
                    start=1;
                }
            }
            alrcbean.setSequenceStartNum(start);
        }
        if(!colbean.isSequenceCol()&&!colbean.isControlCol()&&(colbean.getProperty()==null||colbean.getProperty().trim().equals("")))
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"为数据自动列表报表，不允许<col/>标签的property属性为空");
        }
        String width=eleColBean.attributeValue("width");
        String align=eleColBean.attributeValue("align");
        String clickorderby=eleColBean.attributeValue("clickorderby");
        String filter=eleColBean.attributeValue("filter");

        String rowselectvalue=eleColBean.attributeValue("rowselectvalue");//当前<col/>是否需要在行选中的javascript回调函数中使用，如果设置为true，则在显示当前<col/>时，会在<td/>中显示一个名为value属性，值为当前列的值
        String rowgroup=eleColBean.attributeValue("rowgroup");//是否参与普通行分组
        String treerowgroup=eleColBean.attributeValue("treerowgroup");

        if(filter!=null)
        {
            filter=filter.trim();
            if(filter.equals("")||filter.equalsIgnoreCase("false"))
            {
                alrcbean.setFilterBean(null);
            }else
            {
                if(colbean.isSequenceCol()||colbean.isNonFromDbCol()||colbean.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，不能在column为非数据的<col/>中配置过滤列，即不能配置filter属性");
                }
                AbsListReportFilterBean filterBean=new AbsListReportFilterBean(colbean);
                if(Tools.isDefineKey("condition",filter))
                {
                    filterBean.setConditionname(Tools.getRealKeyByDefine("condition",filter).trim());
                }else if(!filter.toLowerCase().equals("true"))
                {
                    filterBean.setFilterColumnExpression(filter);
                }
                String filterwidth=eleColBean.attributeValue("filterwidth");
                if(filterwidth!=null) filterBean.setFilterwidth(Tools.getWidthHeightIntValue(filterwidth.trim()));
                String filtermaxheight=eleColBean.attributeValue("filtermaxheight");
                if(filtermaxheight!=null) filterBean.setFiltermaxheight(Tools.getWidthHeightIntValue(filtermaxheight.trim()));
                String filterformat=eleColBean.attributeValue("filterformat");
                if(filterformat!=null&&!filterformat.trim().equals(""))
                {
                    filterformat=filterformat.trim();
                    filterBean.setFilterformat(filterformat);
                    filterBean.setFormatClass(reportbean.getFormatMethodClass(filterformat,new Class[] { ReportBean.class, String[].class }));
                    try
                    {
                        filterBean.setFormatMethod(filterBean.getFormatClass().getMethod(filterformat,
                                new Class[] { ReportBean.class, String[].class }));
                    }catch(Exception e)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，无法得到"+filterformat+"格式化方法对象",e);
                    }
                }
                alrcbean.setFilterBean(filterBean);
            }
        }

        if(width!=null&&!width.trim().equals(""))
        {
            if(Tools.getPropertyValueByName("width",colbean.getValuestyleproperty(null,true),true)==null)
                colbean.setValuestyleproperty(colbean.getValuestyleproperty(null,true)+" width='"+width+"' ",true);
            if(Tools.getPropertyValueByName("width",colbean.getLabelstyleproperty(null,true),true)==null)
                colbean.setLabelstyleproperty(colbean.getLabelstyleproperty(null,true)+" width='"+width+"' ",true);
        }
        if(colbean.getValuealign()==null||colbean.getValuealign().trim().equals(""))
        {//如果没有在valuestyleproperty中指定align，则使用<col/>中配置的align属性
            if(align==null)
            {//在<col/>中没有配置align
                if(treerowgroup!=null&&treerowgroup.trim().equals("true"))
                {
                    align="left";
                }else
                {
                    align="center";//默认为center
                }
            }
            colbean.setValuestyleproperty(colbean.getValuestyleproperty(null,true)+" align='"+align+"' ",true);
            colbean.setValuealign(align);
        }
        if(colbean.getPrintlabelstyleproperty(null,false)==null||colbean.getPrintlabelstyleproperty(null,false).trim().equals(""))
        {
            colbean.setPrintlabelstyleproperty(colbean.getLabelstyleproperty(null,false),false);
        }
        if(colbean.getPrintvaluestyleproperty(null,false)==null||colbean.getPrintvaluestyleproperty(null,false).trim().equals(""))
        {
            colbean.setPrintvaluestyleproperty(colbean.getValuestyleproperty(null,false),false);
        }
        String printwidth=colbean.getPrintwidth();
        if(printwidth!=null&&!printwidth.trim().equals(""))
        {
            String printlabelstyleproperty=colbean.getPrintlabelstyleproperty(null,true);
            if(printlabelstyleproperty==null) printlabelstyleproperty="";
            printlabelstyleproperty=Tools.removePropertyValueByName("width",printlabelstyleproperty);
            printlabelstyleproperty=printlabelstyleproperty+" width=\""+printwidth+"\"";
            colbean.setPrintlabelstyleproperty(printlabelstyleproperty,true);
            String printvaluestyleproperty=colbean.getPrintvaluestyleproperty(null,true);
            if(printvaluestyleproperty==null) printvaluestyleproperty="";
            printvaluestyleproperty=Tools.removePropertyValueByName("width",printvaluestyleproperty);
            printvaluestyleproperty=printvaluestyleproperty+" width=\""+printwidth+"\"";
            colbean.setPrintvaluestyleproperty(printvaluestyleproperty,true);
        }
        if(clickorderby!=null)
        {
            clickorderby=clickorderby.toLowerCase().trim();
            if(!clickorderby.equals("true")&&!clickorderby.equals("false"))
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，column为"+colbean.getColumn()
                        +"的<col/>的clickorderby属性配置不合法，必须配置为true或false");
            }
            if(clickorderby.equals("true"))
            {
                if(colbean.getColumn()==null||colbean.getColumn().trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()
                            +"失败，存在在没有配置column属性的<col/>中配置clickorderby为true的情况");
                }
                if(colbean.isSequenceCol()||colbean.isNonFromDbCol()||colbean.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，不能在非数据列的<col/>中配置排序功能");
                }
                alrcbean.setRequireClickOrderby(true);
            }else
            {
                alrcbean.setRequireClickOrderby(false);
            }
        }
        if(rowselectvalue!=null)
        {
            colbean.setDisplayNameValueProperty(rowselectvalue.toLowerCase().trim().equals("true"));
        }
        if(rowgroup!=null&&treerowgroup!=null)
        {
            throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，不能在同一个<col/>中同时配置treerowgroup和rowgroup属性");
        }else if(rowgroup!=null)
        {//加载参与普通行分组的<col/>的信息
            rowgroup=rowgroup.toLowerCase().trim();
            if(!rowgroup.equals("true")&&!rowgroup.equals("false"))
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，参与分组的<col/>的rowgroup属性配置值不合法，只能配置true或false");
            }
            alrcbean.setRowgroup(Boolean.parseBoolean(rowgroup));
            if(alrcbean.isRowgroup())
            {
                if(colbean.isSequenceCol()||colbean.isNonFromDbCol()||colbean.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()
                            +"失败，不能将显示sequence或不从数据库某字段获取数据(即column为sequence或non-fromdb)的<col/>配置rowgroup为true");
                }
                if(alrdbean.getRowgrouptype()==2)
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，不能为同一个报表同时配置普通分组和行分组功能");
                }
                List<String> lstBelongDsids=colbean.getLstDatasetValueids();
                if(lstBelongDsids!=null&&lstBelongDsids.size()>1)
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"的列"+colbean.getColumn()+"失败，参与行分组的列不能指定多个数据集ID");
                }
                alrdbean.setRowgroupDatasetId(lstBelongDsids==null||lstBelongDsids.size()==0?null:lstBelongDsids.get(0));
                colbean.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
                alrdbean.addRowgroupCol(colbean);
                alrdbean.setRowgrouptype(1);
            }
        }else if(treerowgroup!=null)
        {//加载参与树形行分组的<col/>的信息
            treerowgroup=treerowgroup.toLowerCase().trim();
            if(!treerowgroup.equals("true")&&!treerowgroup.equals("false"))
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，参与分组的<col/>的treerowgroup属性配置值不合法，只能配置true或false");
            }
            alrcbean.setRowgroup(Boolean.parseBoolean(treerowgroup));
            if(alrcbean.isRowgroup())
            {
                if(colbean.isSequenceCol()||colbean.isNonFromDbCol()||colbean.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()
                            +"失败，不能将显示sequence或不从数据库某字段获取数据(即column为sequence或non-fromdb)的<col/>配置treerowgroup为true");
                }
                if(alrdbean.getRowgrouptype()==1)
                {//已经有列配置了treerowgroup属性为true，即已经配置了树形分组，则不能再配置普通分组
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，不能为同一个报表同时配置普通分组和行分组功能");
                }
                List<String> lstBelongDsids=colbean.getLstDatasetValueids();
                if(lstBelongDsids!=null&&lstBelongDsids.size()>1)
                {
                    throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"的列"+colbean.getColumn()+"失败，参与行分组的列不能指定多个数据集ID");
                }
                alrdbean.setRowgroupDatasetId(lstBelongDsids==null||lstBelongDsids.size()==0?null:lstBelongDsids.get(0));
                colbean.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
                alrdbean.addRowgroupCol(colbean);
                alrdbean.setRowgrouptype(2);
            }
        }
        if((alrdbean.getRowgrouptype()==1||alrdbean.getRowgrouptype()==2))
        {
            if(alrdbean.getTreenodeid()!=null&&!alrdbean.getTreenodeid().trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+colbean.getReportBean().getPath()+"失败，此报表已经配置为不限层级的树形分组报表，不能再配置普通行分组或树形行分组功能");
            }
            if(!alrcbean.isRowgroup()) alrdbean.addRowgroupCol(null);//当前列不参与行/树形分组，则后面的列都不能再参与行分组/树形分组，因为参与行分组/树形分组的列必须配置在最前面
        }
        String rowordervalue=eleColBean.attributeValue("rowordervalue");
        if(rowordervalue!=null)
        {
            alrcbean.setRoworderValue(rowordervalue.trim().toLowerCase().equals("true"));
            if(alrcbean.isRoworderValue()) colbean.setDisplayNameValueProperty(true);
        }
        if(colbean.isRoworderInputboxCol())
        {
            String inputboxstyleproperty=eleColBean.attributeValue("inputboxstyleproperty");
            if(inputboxstyleproperty!=null)
            {
                alrcbean.setRoworder_inputboxstyleproperty(inputboxstyleproperty.trim());
            }
        }
        //        if(colbean.getHidden().equals("0"))
        //        {//只有hidden为0的列才能参与折线标题列
        //            String curvelabelup=eleCol.attributeValue("curvelabelup");//折线标题列的上部标题
        //            String curvelabeldown=eleCol.attributeValue("curvelabeldown");//折线标题列的上部标题
        //            String curvelabel=eleCol.attributeValue("curvelabel");//是否参与了折线标题
        //            String curvecolor=eleCol.attributeValue("curvecolor");//是否参与了折线标题
        //                bean.setCurvelabeldown(curvelabeldown==null?"":curvelabeldown.trim());
        return 1;
    }

    public int beforeDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        super.beforeDisplayLoading(disbean,lstEleDisplayBeans);
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(alrdbean==null)
        {
            alrdbean=new AbsListReportDisplayBean(disbean);
            disbean.setExtendConfigDataForReportType(KEY,alrdbean);
        }
        alrdbean.setRowgroupDatasetId(null);
        Map<String,String> mDisplayProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleDisplayBeans,
                new String[] { "treenodeid", "treenodename", "treenodeparentid" });
        String treenodeid=mDisplayProperties.get("treenodeid");
        String treenodename=mDisplayProperties.get("treenodename");
        String treenodeparentid=mDisplayProperties.get("treenodeparentid");
        if(treenodeid!=null) alrdbean.setTreenodeid(treenodeid.trim());
        if(treenodename!=null) alrdbean.setTreenodename(treenodename.trim());
        if(treenodeparentid!=null) alrdbean.setTreenodeparentid(treenodeparentid.trim());
        return 1;
    }

    public int afterDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        Map<String,String> mDisplayProperties=ConfigLoadAssistant.getInstance().assembleAllAttributes(
                lstEleDisplayBeans,
                new String[] { "treeborder", "treecloseable", "treexpandlayer", "treeasyn", "treexpandimg", "treeclosedimg", "treeleafimg",
                        "mouseoverbgcolor" });
        String treeborder=mDisplayProperties.get("treeborder");//树形行分组边框显示类型
        String treecloseable=mDisplayProperties.get("treecloseable");
        //        String treecheckbox=eleDisplay.attributeValue("treecheckbox");//是否需要为树形行分组树枝节点显示复选框
        String treexpandlayer=mDisplayProperties.get("treexpandlayer");
        String treeasyn=mDisplayProperties.get("treeasyn");
        String treexpandimg=mDisplayProperties.get("treexpandimg");
        String treeclosedimg=mDisplayProperties.get("treeclosedimg");
        String treeleafimg=mDisplayProperties.get("treeleafimg");
        String mouseoverbgcolor=mDisplayProperties.get("mouseoverbgcolor");
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(mouseoverbgcolor==null)
        {
            if(alrdbean.getMouseoverbgcolor()==null)
            {
                alrdbean.setMouseoverbgcolor(Config.getInstance().getSystemConfigValue("default-mouseoverbgcolor",""));
            }
        }else
        {
            alrdbean.setMouseoverbgcolor(mouseoverbgcolor.trim());
        }
        if(treeborder!=null)
        {
            treeborder=treeborder.trim();
            if(treeborder.equals("")) treeborder="2";
            if(!treeborder.equals("0")&&!treeborder.equals("1")&&!treeborder.equals("2")&&!treeborder.equals("3"))
            {
                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，<display>的treeborder属性只能配置为0、1、2、3");
            }
            alrdbean.setTreeborder(Integer.parseInt(treeborder));
        }
        if(treecloseable!=null)
        {
            treecloseable=treecloseable.toLowerCase().trim();
            if(treecloseable.equals("")) treecloseable="true";
            if(!treecloseable.equals("true")&&!treecloseable.equals("false"))
            {
                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，<display>的treecloseable属性只能配置为true或false");
            }
            alrdbean.setTreecloseable(Boolean.parseBoolean(treecloseable));
        }
        if(treexpandlayer!=null)
        {
            treexpandlayer=treexpandlayer.trim();
            if(treexpandlayer.equals("")) treexpandlayer="-1";//默认是全部展开
            alrdbean.setTreexpandlayer(Integer.parseInt(treexpandlayer));
        }
        if(!alrdbean.isTreecloseable()) alrdbean.setTreexpandlayer(-1);
        if(treeasyn!=null)
        {
            alrdbean.setTreeAsynLoad(treeasyn.toLowerCase().trim().equals("true"));
        }
        if(treexpandimg!=null)
        {
            treexpandimg=treexpandimg.trim();
            if(treexpandimg.equals(""))
            {
                alrdbean.setLstTreexpandimgs(null);
            }else
            {
                alrdbean.setLstTreexpandimgs(Tools.parseStringToList(treexpandimg,";",new String[] { "'", "'" },false));
            }
        }
        if(treeclosedimg!=null)
        {
            treeclosedimg=treeclosedimg.trim();
            if(treeclosedimg.equals(""))
            {
                alrdbean.setLstTreeclosedimgs(null);
            }else
            {
                alrdbean.setLstTreeclosedimgs(Tools.parseStringToList(treeclosedimg,";",new String[] { "'", "'" },false));
            }
        }
        if(treeleafimg!=null) alrdbean.setTreeleafimg(treeleafimg.trim());

        //        processRowSelectCol(disbean);//处理提供行选中的列，只有报表行选中类型为Consts.ROWSELECT_CHECKBOX或Consts.ROWSELECT_RADIOBOX类型时，才会有行选中的列
        //            if(!treecheckbox.equals("true")&&!treecheckbox.equals("false"))
        //            bean.setTreecheckbox(Boolean.parseBoolean(treecheckbox));
        //        boolean isAllAlwayOrNeverCol=true;//是否全部是hidden为1或3的<col/>（如果<col/>全部是1或3，则<group/>的hidden不可能出现0和2的情况，所以不用判断它）
        List<ColBean> lstColBeans=disbean.getLstCols();
        boolean bolContainsClickOrderby=false;
        if(lstColBeans!=null&&lstColBeans.size()>0)
        {
            AbsListReportColBean alcbeanTemp;
            for(ColBean cbeanTmp:lstColBeans)
            {
                if(cbeanTmp==null) continue;
                alcbeanTemp=(AbsListReportColBean)cbeanTmp.getExtendConfigDataForReportType(KEY);
                if(alcbeanTemp==null) continue;
                if(alcbeanTemp.isRequireClickOrderby())
                {
                    bolContainsClickOrderby=true;
                }
                //                if(alcbeanTemp.getFilterBean()!=null
            }
        }
        //        if(isAllAlwayOrNeverCol) bean.setColselected(false);//全部是hidden为1或3的<col/>，后面显示时将不为它提供列选择框
        alrdbean.setContainsClickOrderBy(bolContainsClickOrderby);//此报表包括列排序功能
        //        bean.setContainsNonConditionFilter(bolContainsNonConditionFilter);//此报表包括与条件无关的列过滤功能
        //        if(eleStatisticBean!=null) loadStatisticConfig(eleStatisticBean,disbean,alrdbean);//加载统计信息的配置
        return 1;
    }

    public int afterReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        XmlElementBean eleReportBean=lstEleReportBeans.get(0);
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(alrbean==null)
        {
            alrbean=new AbsListReportBean(reportbean);
            reportbean.setExtendConfigDataForReportType(KEY,alrbean);
        }
        String batchouputcount=eleReportBean.attributeValue("batchouputcount");
        if(batchouputcount!=null)
        {
            if(batchouputcount.trim().equals(""))
            {
                alrbean.setBatchouputcount(null);
            }else
            {
                try
                {
                    alrbean.setBatchouputcount(Integer.parseInt(batchouputcount.trim()));
                }catch(NumberFormatException e)
                {
                    log.warn("报表"+reportbean.getPath()+"配置的batchoutputcount属性值："+batchouputcount.trim()+"不是有效数字");
                    alrbean.setBatchouputcount(null);
                }
            }
        }
        
        XmlElementBean eleSubdisplayBean=eleReportBean.getChildElementByName("subdisplay");
        if(eleSubdisplayBean!=null) alrbean.setSubdisplaybean(loadSubDisplayConfig(eleSubdisplayBean,reportbean));
        return 1;
    }

    private AbsListReportSubDisplayBean loadSubDisplayConfig(XmlElementBean eleSubDisplayBean,ReportBean reportbean)
    {
        AbsListReportSubDisplayBean subDisplayBean=new AbsListReportSubDisplayBean(reportbean);
        boolean hasSubDisplayChild=false;
        List<XmlElementBean> lstEleRowBeans=eleSubDisplayBean.getLstChildElementsByName("subrow");
        if(lstEleRowBeans!=null&&lstEleRowBeans.size()>0)
        {
            List<AbsListReportSubDisplayRowBean> lstSDisRowBeans=new ArrayList<AbsListReportSubDisplayRowBean>();
            subDisplayBean.setLstSubDisplayRowBeans(lstSDisRowBeans);
            AbsListReportSubDisplayRowBean sRowBeanTmp;
            for(XmlElementBean eleRowTmp:lstEleRowBeans)
            {
                if(eleRowTmp==null) continue;
                sRowBeanTmp=new AbsListReportSubDisplayRowBean();
                String displaytype=eleRowTmp.attributeValue("displaytype");
                String displayposition=eleRowTmp.attributeValue("displayposition");
                if(displaytype!=null) sRowBeanTmp.setDisplaytype(reportbean,displaytype.trim());
                if(displayposition!=null) sRowBeanTmp.setDisplayposition(reportbean,displayposition.trim());
                List<XmlElementBean> lstEleColBeans=eleRowTmp.getLstChildElementsByName("subcol");
                List<AbsListReportSubDisplayColBean> lstSubColBeans=new ArrayList<AbsListReportSubDisplayColBean>();
                sRowBeanTmp.setLstSubColBeans(lstSubColBeans);
                if(lstEleColBeans!=null&&lstEleColBeans.size()>0)
                {
                    for(XmlElementBean objTmp:lstEleColBeans)
                    {
                        lstSubColBeans.add(loadSubColConfig(reportbean,objTmp));
                    }
                }
                if(lstSubColBeans.size()>0) lstSDisRowBeans.add(sRowBeanTmp);
            }
            hasSubDisplayChild=lstSDisRowBeans.size()>0;
        }

        List<XmlElementBean> lstRowGroupStatistics=eleSubDisplayBean.getLstChildElementsByName("rowgroup-subrow");
        List<XmlElementBean> lstTreeRowGroupStatistics=eleSubDisplayBean.getLstChildElementsByName("treerowgroup-subrow");
        if(lstRowGroupStatistics!=null&&lstRowGroupStatistics.size()>0&&lstTreeRowGroupStatistics!=null&&lstTreeRowGroupStatistics.size()>0)
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，不能在<subdisplay/>同时配置<rowgroup-subrow/>和<treerowgroup-subrow/>");
        }
        AbsListReportRowGroupSubDisplayRowBean subRowbeanTmp;
        if(lstRowGroupStatistics!=null&&lstRowGroupStatistics.size()>0)
        {
            for(XmlElementBean objTmp:lstRowGroupStatistics)
            {
                subRowbeanTmp=loadRowGroupSubRowConfig(reportbean,objTmp);
                if(subRowbeanTmp!=null) subDisplayBean.addRowGroupSubDisplayRowBean(subRowbeanTmp);
            }
            hasSubDisplayChild=subDisplayBean.getMRowGroupSubDisplayRowBeans()!=null&&subDisplayBean.getMRowGroupSubDisplayRowBeans().size()>0;
        }
        if(lstTreeRowGroupStatistics!=null&&lstTreeRowGroupStatistics.size()>0)
        {
            for(XmlElementBean objTmp:lstTreeRowGroupStatistics)
            {
                subRowbeanTmp=loadRowGroupSubRowConfig(reportbean,objTmp);
                if(subRowbeanTmp!=null) subDisplayBean.addRowGroupSubDisplayRowBean(subRowbeanTmp);
            }
            hasSubDisplayChild=subDisplayBean.getMRowGroupSubDisplayRowBeans()!=null&&subDisplayBean.getMRowGroupSubDisplayRowBeans().size()>0;
        }
        if(!hasSubDisplayChild) return null;
        XmlElementBean eleStatitemsBean=eleSubDisplayBean.getChildElementByName("statitems");
        if(eleStatitemsBean!=null)
        {
            List<XmlElementBean> lstEleStatitemBeans=eleStatitemsBean.getLstChildElementsByName("statitem");
            if(lstEleStatitemBeans!=null&&lstEleStatitemBeans.size()>0)
            {
                List<StatisticItemBean> lstStatitemBeans=new ArrayList<StatisticItemBean>();
                for(XmlElementBean eleBeanTmp:lstEleStatitemBeans)
                {
                    lstStatitemBeans.add(loadStatitemConfig(reportbean,eleBeanTmp));
                }
                subDisplayBean.setLstStatitemBeans(lstStatitemBeans);
            }
        }
        FormatBean fbean=ConfigLoadAssistant.getInstance().loadFormatConfig(eleSubDisplayBean.getChildElementByName("format"));
        if(fbean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的<subdisplay/>配置失败，没有配置格式化显示<format/>标签");
        }
        subDisplayBean.setFbean(fbean);
        return subDisplayBean;
    }

    private AbsListReportRowGroupSubDisplayRowBean loadRowGroupSubRowConfig(ReportBean reportbean,XmlElementBean eleRowGroupBean)
    {
        String column=eleRowGroupBean.attributeValue("column");
        if(column==null||column.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，在<subdisplay/>配置<rowgroup-subrow/>时，column属性不能为空");
        }
        String condition=eleRowGroupBean.attributeValue("condition");
        AbsListReportRowGroupSubDisplayRowBean srgbean=new AbsListReportRowGroupSubDisplayRowBean();
        srgbean.setRowgroupcolumn(column.trim());
        if(condition!=null) srgbean.setCondition(condition.trim());
        List<XmlElementBean> lstEleCols=eleRowGroupBean.getLstChildElementsByName("subcol");
        List<AbsListReportSubDisplayColBean> lstSubColBeans=new ArrayList<AbsListReportSubDisplayColBean>();
        srgbean.setLstSubColBeans(lstSubColBeans);
        if(lstEleCols!=null&&lstEleCols.size()>0)
        {
            for(XmlElementBean colobjTmp:lstEleCols)
            {
                lstSubColBeans.add(loadSubColConfig(reportbean,colobjTmp));
            }
        }
        return lstSubColBeans.size()==0?null:srgbean;
    }

    private AbsListReportSubDisplayColBean loadSubColConfig(ReportBean reportbean,XmlElementBean eleScolBean)
    {
        AbsListReportSubDisplayColBean subColBean=new AbsListReportSubDisplayColBean();
        String property=eleScolBean.attributeValue("property");
        if(property==null||property.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项<subcol/>失败，property属性不能为空");
        }
        subColBean.setProperty(property.trim());
        String colspan=eleScolBean.attributeValue("colspan");
        String valuestyleproperty=eleScolBean.attributeValue("valuestyleproperty");
        if(valuestyleproperty==null) valuestyleproperty="";
        String colspanInStyleprop=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
        if(colspan!=null&&!colspan.trim().equals("")&&colspanInStyleprop!=null&&!colspanInStyleprop.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，不能同时在<scol/>的colspan和valuestyleproperty中同时配置colspan值");
        }
        if(colspan!=null&&!colspan.trim().equals(""))
        {
            valuestyleproperty=valuestyleproperty+" colspan=\""+colspan+"\"";
        }
        subColBean.setValuestyleproperty(valuestyleproperty.trim(),false);
        return subColBean;
    }

    private StatisticItemBean loadStatitemConfig(ReportBean reportbean,XmlElementBean eleStatitemBean)
    {
        StatisticItemBean statitemBean=new StatisticItemBean();
        String property=eleStatitemBean.attributeValue("property");
        String value=eleStatitemBean.attributeValue("value");
        String statiscope=eleStatitemBean.attributeValue("statiscope");
        String dataset=eleStatitemBean.attributeValue("dataset");
        if(property==null||property.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项<statitems/>失败，property属性不能为空");
        }
        statitemBean.setProperty(property.trim());
        if(value==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+property+"失败，必须配置<statitem/>的value属性");
        }
        value=value.trim();
        int idxl=value.indexOf("(");
        int idxr=value.lastIndexOf(")");
        if(idxl<=0||idxr!=value.length()-1)
        {//不合要求
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+property+"失败，"+value+"不是有效的统计");
        }
        String statitype=value.substring(0,idxl).trim();
        String column=value.substring(idxl+1,idxr).trim();
        if(!Consts.lstStatisticsType.contains(statitype))
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+property+"失败，"+statitype+"不是有效的统计类型");
        }
        if(column.equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+property+"失败，"+value+"中统计字段为空");
        }
        statitemBean.setValue(value);
        statitemBean.setDatatypeObj(ConfigLoadAssistant.loadDataType(eleStatitemBean));
        statitemBean.setLstConditions(ComponentConfigLoadManager.loadConditionsInOtherPlace(eleStatitemBean,reportbean));
        if(statiscope!=null) statitemBean.setStatiscope(reportbean,statiscope.trim());
        if(dataset!=null) statitemBean.setDatasetid(dataset.trim());
        return statitemBean;
    }
    
    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        SqlBean sbean=reportbean.getSbean();
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(alrbean==null) return 1;
        if(sbean==null||!sbean.isHorizontalDataset())
        {
            processFixedColsAndRows(reportbean);
        }
        processReportScrollConfig(reportbean);
        if(alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_VERTICAL||alrbean.getScrollType()==AbsListReportBean.SCROLLTYPE_FIXED
                ||(sbean!=null&&sbean.isHorizontalDataset()))
        {
            reportbean.setCellresize(0);
        }
        DisplayBean dbean=reportbean.getDbean();
        if(sbean!=null&&sbean.isHorizontalDataset())
        {
            dbean.setPageColselect(false);
            dbean.setDataexportColselect(false);
            reportbean.setCelldrag(0);
        }else
        {
            AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrdbean!=null&&alrdbean.getRowGroupColsNum()>0)
            {
                reportbean.setPageLazyloadataCount(-1);//行分组或树形分组报表不能延迟加载，否则无法取到父数据集中共有哪些数据
                reportbean.setDataexportLazyloadataCount(-1);
                if(sbean!=null&&sbean.isMultiDataSetCols()&&Tools.isEmpty(alrdbean.getRowgroupDatasetId()))
                {
                    throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"配置了多个独立数据集，必须为其行分组列指定来自哪个数据集");
                }
            }
        }
        processRowSelectCol(dbean);
        processRoworderCol(dbean);
        ((AbsListReportDisplayBean)dbean.getExtendConfigDataForReportType(KEY)).doPostLoad();
        if(alrbean.getSubdisplaybean()!=null)
        {
            if(sbean!=null&&sbean.isHorizontalDataset())
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，横向数据集不能配置辅助显示行");
            }
            alrbean.getSubdisplaybean().doPostLoad();
        }
        return 1;
    }

    protected void processFixedColsAndRows(ReportBean reportbean)
    {
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        if(alrbean.getFixedcols(null)>0)
        {
            int cnt=0;
            for(ColBean cbTmp:reportbean.getDbean().getLstCols())
            {
                if(cbTmp.getDisplaytype(true)==Consts.COL_DISPLAYTYPE_HIDDEN) continue;
                cnt++;
            }
            if(cnt<=alrbean.getFixedcols(null)) alrbean.setFixedcols(null,0);
        }
        if(alrbean.getFixedcols(null)>0)
        {
            boolean isChkRadioRowselectReport=Consts.ROWSELECT_CHECKBOX.equals(alrbean.getRowSelectType())
                    ||Consts.ROWSELECT_RADIOBOX.equals(alrbean.getRowSelectType())
                    ||Consts.ROWSELECT_MULTIPLE_CHECKBOX.equals(alrbean.getRowSelectType())
                    ||Consts.ROWSELECT_SINGLE_RADIOBOX.equals(alrbean.getRowSelectType());
            AbsListReportColBean alrcbeanTmp;
            int cnt=0;
            for(ColBean cbTmp:reportbean.getDbean().getLstCols())
            {
                if(cbTmp.getDisplaytype(true)==Consts.COL_DISPLAYTYPE_HIDDEN) continue;
                if(cbTmp.isRowSelectCol())
                {
                    if(!isChkRadioRowselectReport) continue;//如果当前报表不是单选框或复选框选中方式的报表，则这两种行选中列会在稍后processRowSelectCol()方法中删除掉，所以这里不对它计数
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,在<report/>的fixedcols中配置的冻结列数包括了行选中列，这样不能正常选中行");
                }
                alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(KEY);
                if(alrcbeanTmp==null)
                {
                    alrcbeanTmp=new AbsListReportColBean(cbTmp);
                    cbTmp.setExtendConfigDataForReportType(KEY,alrcbeanTmp);
                }
                alrcbeanTmp.setFixedCol(null,true);
                if(++cnt==alrbean.getFixedcols(null)) break;
            }
        }
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
        if(alrbean.getFixedcols(null)>0||alrbean.getFixedrows()>0)
        {
            if(alrdbean!=null&&alrdbean.getRowgrouptype()==2&&alrdbean.getRowGroupColsNum()>0)
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,树形分组报表不能冻结行列标题");
            }
        }
    }

    protected void processReportScrollConfig(ReportBean reportbean)
    {
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(KEY);
        int scrolltype=alrbean.getScrollType();
        if(scrolltype==AbsListReportBean.SCROLLTYPE_NONE||scrolltype==AbsListReportBean.SCROLLTYPE_FIXED) return;
        if(scrolltype==AbsListReportBean.SCROLLTYPE_ALL)
        {
            ComponentAssistant.getInstance().doPostLoadForComponentScroll(reportbean,true,true,reportbean.getScrollwidth(),
                    reportbean.getScrollheight(),reportbean.getScrollstyle());
        }else
        {
            if(scrolltype==AbsListReportBean.SCROLLTYPE_VERTICAL)
            {//只显示垂直滚动条的数据自动列表报表必须以像素的形式配置宽度，否则不能保证标题列和数据列对齐
                String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(reportbean.getWidth());
                if(htmlsizeArr==null||htmlsizeArr[0].equals("")||htmlsizeArr[0].equals("0"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,此报表只显示垂直滚动条，因此必须为其配置width属性指定报表宽度");
                }else
                {
                    if(htmlsizeArr[1]!=null&&htmlsizeArr[1].equals("%"))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败,只显示垂直滚动条时，不能将报表的width属性配置为百分比");
                    }
                    reportbean.setWidth(htmlsizeArr[0]+htmlsizeArr[1]);
                }
                reportbean.setShowContextMenu(false);
            }
            if(Consts_Private.SCROLLSTYLE_IMAGE.equals(reportbean.getScrollstyle()))
            {
                String scrolljs="/webresources/script/wabacus_scroll.js";
                //                if(Config.encode.toLowerCase().trim().equals("utf-8"))
                //                    scrolljs="/webresources/script/wabacus_scroll.js";
                //                    if(encode.trim().equalsIgnoreCase("gb2312"))
                //                    scrolljs="/webresources/script/"+encode.toLowerCase()+"/wabacus_scroll.js";
                scrolljs=Tools.replaceAll(Config.webroot+"/"+scrolljs,"//","/");
                reportbean.getPageBean().addMyJavascriptFile(scrolljs,-1);
                String css=Config.webroot+"/webresources/skin/"+Consts_Private.SKIN_PLACEHOLDER+"/wabacus_scroll.css";
                css=Tools.replaceAll(css,"//","/");
                reportbean.getPageBean().addMyCss(css);
                if(scrolltype==AbsListReportBean.SCROLLTYPE_HORIZONTAL)
                {//只显示横向滚动条
                    reportbean.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONlOAD_IMGSCROLL,"showComponentScroll('"+reportbean.getGuid()
                            +"','-1',12)"));
                }else if(scrolltype==AbsListReportBean.SCROLLTYPE_VERTICAL)
                {
                    reportbean.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONlOAD_IMGSCROLL,"showComponentScroll('"+reportbean.getGuid()
                            +"','"+reportbean.getScrollheight()+"',11)"));
                }
            }
        }
    }

    protected ColBean[] processRowSelectCol(DisplayBean disbean)
    {
        List<ColBean> lstCols=disbean.getLstCols();
        ColBean cbRowSelect=null;
        for(ColBean cbTmp:lstCols)
        {
            if(cbTmp.isRowSelectCol())
            {
                if(cbRowSelect!=null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，不能配置多个行选择的单选框或复选框列");
                }
                cbRowSelect=cbTmp;
            }
        }
        ReportBean reportbean=disbean.getReportBean();
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrbean.getRowSelectType()==null
                ||(!alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_CHECKBOX)
                        &&!alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_RADIOBOX)
                        &&!alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_MULTIPLE_CHECKBOX)&&!alrbean.getRowSelectType().trim().equals(
                        Consts.ROWSELECT_SINGLE_RADIOBOX)))
        {//当前报表要么没有提供行选中功能，要么提供的不是复选框/单选框的行选择功能
            if(cbRowSelect==null) return null;
            for(int i=0,len=lstCols.size();i<len;i++)
            {
                if(lstCols.get(i).isRowSelectCol())
                {
                    lstCols.remove(i);
                    break;
                }
            }
            return null;
        }
        //提供了复选框/单选框的行选择功能
        if(cbRowSelect==null) return insertRowSelectNewCols(alrbean,lstCols);
        if(Consts.ROWSELECT_CHECKBOX.equals(alrbean.getRowSelectType())||Consts.ROWSELECT_MULTIPLE_CHECKBOX.equals(alrbean.getRowSelectType()))
        {//如果是复选框，则在标题部分显示一个复选框，支持全选和全不选
            String label=cbRowSelect.getLabel(rrequest);
            label=label==null?"":label.trim();
            if(label.indexOf("<input")<0||label.indexOf("type")<0||label.indexOf("checkbox")<0)
            {
                label=label
                        +"<input type=\"checkbox\" onclick=\"try{doSelectedAllDataRowChkRadio(this);}catch(e){logErrorsAsJsFileLoad(e);}\" name=\""
                        +reportbean.getGuid()+"_rowselectbox\">";
            }
            cbRowSelect.setLabel(label);
        }
        cbRowSelect.setLabelstyleproperty(Tools.addPropertyValueToStylePropertyIfNotExist(cbRowSelect.getLabelstyleproperty(null,true),"align","center"),true);
        cbRowSelect.setLabelstyleproperty(Tools.addPropertyValueToStylePropertyIfNotExist(cbRowSelect.getLabelstyleproperty(null,true),"valign","middle"),true);
        cbRowSelect.setValuestyleproperty(Tools.addPropertyValueToStylePropertyIfNotExist(cbRowSelect.getValuestyleproperty(null,true),"align","center"),true);
        cbRowSelect.setValuestyleproperty(Tools.addPropertyValueToStylePropertyIfNotExist(cbRowSelect.getValuestyleproperty(null,true),"valign","middle"),true);
        cbRowSelect.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
        return null;
    }

    protected ColBean[] insertRowSelectNewCols(AbsListReportBean alrbean,List<ColBean> lstCols)
    {
        ReportBean reportbean=(ReportBean)alrbean.getOwner();
        ColBean[] cbResult=new ColBean[2];
        ColBean cbNewRowSelect=new ColBean(reportbean.getDbean());
        cbNewRowSelect.setColumn(Consts_Private.COL_ROWSELECT);
        cbNewRowSelect.setProperty(Consts_Private.COL_ROWSELECT);
        cbResult[0]=cbNewRowSelect;
        AbsListReportColBean alrcbean=new AbsListReportColBean(cbNewRowSelect);
        cbNewRowSelect.setExtendConfigDataForReportType(KEY,alrcbean);
        if(Consts.ROWSELECT_CHECKBOX.equals(alrbean.getRowSelectType())||Consts.ROWSELECT_MULTIPLE_CHECKBOX.equals(alrbean.getRowSelectType()))
        {
            cbNewRowSelect
                    .setLabel("<input type=\"checkbox\" onclick=\"try{doSelectedAllDataRowChkRadio(this);}catch(e){logErrorsAsJsFileLoad(e);}\" name=\""
                            +reportbean.getGuid()+"_rowselectbox\">");
        }else
        {
            cbNewRowSelect.setLabel("");
        }
        cbNewRowSelect.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
        cbNewRowSelect.setLabelstyleproperty("style=\"text-align:center;vertical-align:middle;\"",true);
        cbNewRowSelect.setValuestyleproperty("style=\"text-align:center;vertical-align:middle;\"",true);
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(
                UltraListReportType.KEY);
        ColBean cbTmp;
        for(int i=0,len=lstCols.size();i<len;i++)
        {
            cbTmp=lstCols.get(i);
            if(cbTmp.getDisplaytype(true).equals(Consts.COL_DISPLAYTYPE_HIDDEN))
            {
                if(i==len-1) lstCols.add(cbNewRowSelect);
                continue;
            }
            AbsListReportColBean alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(KEY);
            if(alrcbeanTmp!=null&&(alrcbeanTmp.isRowgroup()||alrcbeanTmp.isFixedCol(rrequest)))
            {//如果当前是行分组或树形分组列或者是冻结列
                if(i==len-1) lstCols.add(cbNewRowSelect);
                continue;
            }
            UltraListReportColBean ulrcbeanTmp=(UltraListReportColBean)cbTmp.getExtendConfigDataForReportType(UltraListReportType.KEY);
            if(ulrcbeanTmp!=null&&ulrcbeanTmp.getParentGroupid()!=null&&!ulrcbeanTmp.getParentGroupid().trim().equals(""))
            {//当前列是在<group/>中
                String parentgroupid=ulrcbeanTmp.getParentGroupid();
                if(ulrdbean!=null&&(hasRowgroupColSibling(parentgroupid,ulrdbean)||hasFixedColSibling(parentgroupid,ulrdbean)))
                {//如果此列所在的<group/>或任意层父<group/>中有行分组列或被冻结的列，则新生成的行选择列不能在它的前面
                    if(i==len-1) lstCols.add(cbNewRowSelect);
                    continue;
                }
            }
            lstCols.add(i,cbNewRowSelect);
            cbResult[1]=cbTmp;
            break;
        }
        return cbResult;
    }

    protected List<ColBean> processRoworderCol(DisplayBean disbean)
    {
        List<ColBean> lstCols=disbean.getLstCols();
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrbean.getLstRoworderTypes()!=null&&alrbean.getLstRoworderTypes().size()>0)
        {
            if(disbean.getReportBean().getSbean().isHorizontalDataset())
            {
                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()
                        +"失败，横向数据集不能配置行排序功能");
            }
            List<ColBean> lstRoworderValueCols=new ArrayList<ColBean>();
            AbsListReportColBean alrcbeanTmp=null;
            for(ColBean cbTmp:lstCols)
            {
                alrcbeanTmp=(AbsListReportColBean)cbTmp.getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(alrcbeanTmp==null||!alrcbeanTmp.isRoworderValue()) continue;
                lstRoworderValueCols.add(cbTmp);
            }
            if(lstRoworderValueCols.size()==0)
            {
                throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()
                        +"失败，为它配置了行排序功能，但没有一个<col/>的rowordervalue属性配置为true，这样无法完成行排序功能");
            }
            AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
            alrdbean.setLstRoworderValueCols(lstRoworderValueCols);
        }
        Map<String,ColBean> mCreatedRoworderCols=new HashMap<String,ColBean>();
        for(String rowordertypeTmp:Consts.lstAllRoworderTypes)
        {
            if(rowordertypeTmp.equals(Consts.ROWORDER_DRAG)) continue;
            if(alrbean.getLstRoworderTypes()==null||!alrbean.getLstRoworderTypes().contains(rowordertypeTmp))
            {
                for(int i=lstCols.size()-1;i>=0;i--)
                {//删除掉所有这种行排序类型的列
                    if(lstCols.get(i).isRoworderCol(getRoworderColColumnByRoworderType(rowordertypeTmp)))
                    {
                        lstCols.remove(i);
                    }
                }
            }else
            {
                boolean isExistCol=false;
                for(int i=lstCols.size()-1;i>=0;i--)
                {
                    if(lstCols.get(i).isRoworderCol(getRoworderColColumnByRoworderType(rowordertypeTmp)))
                    {
                        isExistCol=true;
                        lstCols.get(i).setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
                    }
                }
                if(!isExistCol)
                {
                    ColBean cbNewRoworder=new ColBean(disbean);
                    cbNewRoworder.setColumn(getRoworderColColumnByRoworderType(rowordertypeTmp));
                    cbNewRoworder.setProperty(getRoworderColColumnByRoworderType(rowordertypeTmp));
                    AbsListReportColBean alrcbean=new AbsListReportColBean(cbNewRoworder);
                    cbNewRoworder.setExtendConfigDataForReportType(KEY,alrcbean);
                    if(rowordertypeTmp.equals(Consts.ROWORDER_ARROW))
                    {
                        cbNewRoworder.setLabel(Config.getInstance().getResourceString(null,disbean.getPageBean(),"${roworder.arrow.label}",false));
                    }else if(rowordertypeTmp.equals(Consts.ROWORDER_INPUTBOX))
                    {
                        cbNewRoworder.setLabel(Config.getInstance().getResourceString(null,disbean.getPageBean(),"${roworder.inputbox.label}",false));
                    }else if(rowordertypeTmp.equals(Consts.ROWORDER_TOP))
                    {
                        cbNewRoworder.setLabel(Config.getInstance().getResourceString(null,disbean.getPageBean(),"${roworder.top.label}",false));
                    }
                    cbNewRoworder.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
                    cbNewRoworder.setLabelstyleproperty("style=\"text-align:center;vertical-align:middle;\"",true);
                    cbNewRoworder.setValuestyleproperty("style=\"text-align:center;vertical-align:middle;\"",true);
                    mCreatedRoworderCols.put(rowordertypeTmp,cbNewRoworder);
                }
            }
        }
        List<ColBean> lstCreatedColBeans=new ArrayList<ColBean>();
        if(mCreatedRoworderCols.size()>0)
        {//本次有动态生成列，则把它加入disbean中，且放在最后的位置
            for(String roworderTmp:alrbean.getLstRoworderTypes())
            {
                if(!mCreatedRoworderCols.containsKey(roworderTmp)) continue;
                lstCols.add(mCreatedRoworderCols.get(roworderTmp));
                lstCreatedColBeans.add(mCreatedRoworderCols.get(roworderTmp));
            }
        }
        return lstCreatedColBeans;
    }

    protected String getRoworderColColumnByRoworderType(String rowordertype)
    {
        if(rowordertype==null) return null;
        if(rowordertype.equals(Consts.ROWORDER_ARROW))
        {
            return Consts_Private.COL_ROWORDER_ARROW;
        }else if(rowordertype.equals(Consts.ROWORDER_INPUTBOX))
        {
            return Consts_Private.COL_ROWORDER_INPUTBOX;
        }else if(rowordertype.equals(Consts.ROWORDER_TOP))
        {
            return Consts_Private.COL_ROWORDER_TOP;
        }
        return "";
    }

    private boolean hasRowgroupColSibling(String parentgroupid,UltraListReportDisplayBean ulrdbean)
    {
        if(parentgroupid==null||parentgroupid.trim().equals("")) return false;
        UltraListReportGroupBean ulrgbean=ulrdbean.getGroupBeanById(parentgroupid);
        if(ulrgbean==null) return false;
        if(ulrgbean.hasRowgroupChildCol()) return true;
        return hasRowgroupColSibling(ulrgbean.getParentGroupid(),ulrdbean);
    }

    private boolean hasFixedColSibling(String parentgroupid,UltraListReportDisplayBean ulrdbean)
    {
        if(parentgroupid==null||parentgroupid.trim().equals("")) return false;
        UltraListReportGroupBean ulrgbean=ulrdbean.getGroupBeanById(parentgroupid);
        if(ulrgbean==null) return false;
        if(ulrgbean.hasFixedChildCol(rrequest)) return true;
        return hasFixedColSibling(ulrgbean.getParentGroupid(),ulrdbean);
    }

    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_LIST;
    }
}
