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

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.configbean.BlockListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.intercept.ReportDataBean;
import com.wabacus.system.intercept.RowDataBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class BlockListReportType extends AbsListReportType
{
    private final static String KEY=BlockListReportType.class.getName();

    public BlockListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    public void showReportData(StringBuilder resultBuf)
    {
        if(!rrequest.checkPermission(rbean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        dataPartStringBuffer=resultBuf;
        ReportDataBean reportDataObjFromInterceptor=null;
        if(rbean.getInterceptor()!=null)
        {
            reportDataObjFromInterceptor=new ReportDataBean(this,rbean.getDbean().getLstCols());
            rbean.getInterceptor().beforeDisplayReportData(rrequest,rbean,reportDataObjFromInterceptor);
        }
        if(reportDataObjFromInterceptor!=null&&reportDataObjFromInterceptor.getBeforeDisplayString()!=null)
        {
            dataPartStringBuffer.append(reportDataObjFromInterceptor.getBeforeDisplayString());
        }
        if(reportDataObjFromInterceptor==null||reportDataObjFromInterceptor.isShouldDisplayReportData())
        {
            dataPartStringBuffer.append(showReportScrollStartTag());
            dataPartStringBuffer.append("<ul id=\""+rbean.getGuid()+"_data\" class=\"cls-blocklist\"");
            dataPartStringBuffer.append(" style=\"");
            dataPartStringBuffer.append(" width:"+getReportDataWidthOnPage()+";");
            if(rbean.getHeight()!=null&&!rbean.getHeight().trim().equals(""))
            {
                dataPartStringBuffer.append("height:").append(rbean.getHeight()).append(";");
            }
            dataPartStringBuffer.append("\"");
            if(rbean.shouldShowContextMenu())
            {
                dataPartStringBuffer.append(" oncontextmenu=\"try{showcontextmenu('contextmenu_"+rbean.getGuid()
                        +"',event);}catch(e){logErrorsAsJsFileLoad(e);}\"");
            }
            dataPartStringBuffer.append(">");
            if(getDisplayRowInfo()[1]<=0)
            {
                dataPartStringBuffer.append("<li class=\"cls-blocklist-block\"><div class=\"cls-blocklist-item\">");
                if(this.isLazyDisplayData()&&rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
                {
                    dataPartStringBuffer.append(rrequest.getStringAttribute(rbean.getId()+"_lazydisplaydata_prompt",""));
                }else
                {
                    dataPartStringBuffer.append(rrequest.getI18NStringValue((Config.getInstance().getResources().getString(rrequest,rbean.getPageBean(),
                            Consts.NODATA_PROMPT_KEY,true))));
                }
                dataPartStringBuffer.append("</div></li></ul>");
                return;
            }
            if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
            {
                dataPartStringBuffer.append("<table border='1'");
                if(rrequest.getShowtype()==Consts.DISPLAY_ON_WORD)
                {
                    dataPartStringBuffer.append(" style=\"");
                    if(!rbean.getWidth().trim().equals(""))
                    {
                        dataPartStringBuffer.append(" width:"+rbean.getWidth()+";");
                    }else
                    {
                        dataPartStringBuffer.append("width:100%;");
                    }
                    dataPartStringBuffer
                            .append("border-collapse:collapse;border:none;mso-border-alt:solid windowtext .25pt;mso-border-insideh:.5pt solid windowtext;mso-border-insidev:.5pt solid windowtext");
                    dataPartStringBuffer.append("\"");
                }else
                {
                    dataPartStringBuffer.append(" width=\"100%\"");
                }
                dataPartStringBuffer.append(">");
            }
            showDataPart();
            if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
            {
                dataPartStringBuffer.append("</table>");
            }
            dataPartStringBuffer.append("</ul>");
            dataPartStringBuffer.append(showReportScrollEndTag());
        }
        if(reportDataObjFromInterceptor!=null&&reportDataObjFromInterceptor.getAfterDisplayString()!=null)
        {
            dataPartStringBuffer.append(reportDataObjFromInterceptor.getAfterDisplayString());
        }
    }

    private void showDataPart()
    {
        int startNum=this.cacheDataBean.isLoadAllReportData()?0:(this.cacheDataBean.getFinalPageno()-1)*this.cacheDataBean.getPagesize();
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) dataPartStringBuffer.append("<tr>");
        List<ColBean> lstColBeans=rbean.getDbean().getLstCols();
        BlockListReportDisplayBean blrdbean=(BlockListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(KEY);
        RowDataBean rowInterceptorObjTmp;
        ColDisplayData colDisplayData;
        String col_displayvalue, trstylepropertyTmp;
        AbsReportDataPojo rowDataObjTmp;
        int colsinexportfile=blrdbean.getColsinexportfile();
        int n=1;
        boolean isLazyLoadata=rbean.isLazyLoadReportData(rrequest);
        int lazyloadataCount=rbean.getLazyLoadDataCount(rrequest);
        int recordidx=-1;
        int[] displayrowinfo=getDisplayRowInfo();
        for(int i=displayrowinfo[0];i<displayrowinfo[1];i++)
        {
            if(isLazyLoadata&&i%lazyloadataCount==0)
            {//是延迟加载数据，且要开始加载新的一批数据
                loadLazyReportData(startNum+i,startNum+i+lazyloadataCount);
                if(lstReportData.size()==0) break;
                recordidx=-1;
            }
            recordidx=isLazyLoadata?(recordidx+1):i;
            if(recordidx>=lstReportData.size()) break;
            rowDataObjTmp=lstReportData.get(recordidx);
            trstylepropertyTmp=rowDataObjTmp.getRowValuestyleproperty();
            if(rbean.getInterceptor()!=null)
            {
                rowInterceptorObjTmp=new RowDataBean(this,trstylepropertyTmp,lstColBeans,rowDataObjTmp,i,-1);
                rbean.getInterceptor().beforeDisplayReportDataPerRow(this.rrequest,this.rbean,rowInterceptorObjTmp);
                if(rowInterceptorObjTmp.getInsertDisplayRowHtml()!=null) dataPartStringBuffer.append(rowInterceptorObjTmp.getInsertDisplayRowHtml());
                if(!rowInterceptorObjTmp.isShouldDisplayThisRow()) continue;
                trstylepropertyTmp=rowInterceptorObjTmp.getRowstyleproperty();
            }
            if(trstylepropertyTmp==null) trstylepropertyTmp="";
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                dataPartStringBuffer.append("<li class=\"cls-blocklist-block\" "+trstylepropertyTmp+">");
            }else
            {
                dataPartStringBuffer.append("<td "+trstylepropertyTmp+">");
            }
            for(ColBean cbean:lstColBeans)
            {
                if(this.isHiddenCol(cbean)) continue;
                col_displayvalue=getColDisplayValue(cbean,rowDataObjTmp,startNum);
                dataPartStringBuffer.append("<div class=\"cls-blocklist-item\"");
                colDisplayData=ColDisplayData.getColDataFromInterceptor(this,cbean,rowDataObjTmp,i,rowDataObjTmp.getColValuestyleproperty(cbean
                        .getProperty()),col_displayvalue);
                dataPartStringBuffer.append(colDisplayData.getStyleproperty());
                dataPartStringBuffer.append(">").append(colDisplayData.getValue());
                dataPartStringBuffer.append("</div>");
            }
            if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
            {
                dataPartStringBuffer.append("</li>");
            }else
            {
                dataPartStringBuffer.append("</td>");
                if(colsinexportfile>0&&n++%colsinexportfile==0) dataPartStringBuffer.append("</tr><tr>");
            }
            this.checkAndPrintBufferData(i);
        }
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
        {
            dataPartStringBuffer.append("</tr>");
        }
        if(rbean.getInterceptor()!=null)
        {
            rowInterceptorObjTmp=new RowDataBean(this,rbean.getDbean().getValuestyleproperty(rrequest,false),lstColBeans,null,Integer.MAX_VALUE,-1);
            rbean.getInterceptor().beforeDisplayReportDataPerRow(this.rrequest,this.rbean,rowInterceptorObjTmp);
            if(rowInterceptorObjTmp.getInsertDisplayRowHtml()!=null) dataPartStringBuffer.append(rowInterceptorObjTmp.getInsertDisplayRowHtml());
        }
    }

    private String getColDisplayValue(ColBean cbean,AbsReportDataPojo dataObj,int startNum)
    {
        String col_displayvalue;
        if(cbean.isControlCol())
        {
            throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，此报表不能配置行选中列或行排序列");
        }else if(cbean.isSequenceCol())
        {
            AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            col_displayvalue=String.valueOf(startNum+alrcbean.getSequenceStartNum());
        }else
        {
            col_displayvalue=dataObj.getColStringValue(cbean);
            if(col_displayvalue==null) col_displayvalue="";
        }
        return col_displayvalue;
    }

    private String showReportScrollStartTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        boolean isShowScrollX=rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("");
        return ComponentAssistant.getInstance().showComponentScrollStartPart(rbean,isShowScrollX,isShowScrollY,rbean.getScrollwidth(),
                rbean.getScrollheight(),rbean.getScrollstyle());
    }

    private String showReportScrollEndTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        boolean isShowScrollX=rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("");
        return ComponentAssistant.getInstance().showComponentScrollEndPart(rbean.getScrollstyle(),isShowScrollX,isShowScrollY);
    }

    public void showReportData(boolean showtype,StringBuilder resultBuf)
    {
        if(showtype) showReportData(resultBuf);
    }

    public String getColSelectedMetadata()
    {
        return "";
    }

    public boolean isSupportHorizontalDataset(ReportBean reportbean)
    {
        return false;
    }
    
    public int afterDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        super.beforeDisplayLoading(disbean,lstEleDisplayBeans);
        BlockListReportDisplayBean blrdbean=new BlockListReportDisplayBean(disbean);
        disbean.setExtendConfigDataForReportType(KEY,blrdbean);
        XmlElementBean eleDisplayBean=lstEleDisplayBeans.get(0);
        String blockwidth=eleDisplayBean.attributeValue("blockwidth");
        String blockheight=eleDisplayBean.attributeValue("blockheight");
        String colsinexportfile=eleDisplayBean.attributeValue("colsinexportfile");
        if(colsinexportfile==null||colsinexportfile.trim().equals("")) colsinexportfile="5";
        try
        {
            blrdbean.setColsinexportfile(Integer.parseInt(colsinexportfile.trim()));
        }catch(NumberFormatException e)
        {
            throw new WabacusConfigLoadingException("加载报表"+disbean.getReportBean().getPath()+"失败，为<display/>配置的colsinexportfile属性值"+colsinexportfile
                    +"不是有效数字",e);
        }
        String blockstyleproperty=disbean.getValuestyleproperty(null,true);
        if(blockstyleproperty==null) blockstyleproperty="";
        String style=Tools.getPropertyValueByName("style",blockstyleproperty,false);
        style=style==null?"":style.trim();
        String widthinstyle=Tools.getPropertyValueFromStyle("width",style);
        if((widthinstyle==null||widthinstyle.trim().equals(""))&&blockwidth!=null&&!blockwidth.trim().equals(""))
        {//没有在blockstyleproperty的style中指定width，但在<display/>中配置了blockwidth
            if(!style.equals("")&&!style.endsWith(";")) style=style+";";
            style=style+"width:"+blockwidth+";";
        }
        String heightinstyle=Tools.getPropertyValueFromStyle("height",style);//从style中取到height属性值
        if((heightinstyle==null||heightinstyle.trim().equals(""))&&blockheight!=null&&!blockheight.trim().equals(""))
        {//没有在blockstyleproperty的style中指定height，但在<display/>中配置了blockheight
            if(!style.equals("")&&!style.endsWith(";")) style=style+";";
            style=style+"height:"+blockheight+";";
        }
        blockstyleproperty=Tools.removePropertyValueByName("style",blockstyleproperty);
        if(style!=null&&!style.trim().equals(""))
        {
            blockstyleproperty=blockstyleproperty+" style=\""+style+"\"";
        }
        disbean.setValuestyleproperty(blockstyleproperty.trim(),true);
        return 1;
    }

    public int doPostLoad(ReportBean reportbean)
    {
        DisplayBean disbean=reportbean.getDbean();
        disbean.setPageColselect(false);
        disbean.setDataexportColselect(false);
        AbsListReportBean alrbean=(AbsListReportBean)disbean.getReportBean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        alrbean.setRowSelectType(Consts.ROWSELECT_NONE);
        return super.doPostLoad(reportbean);
    }

    protected void processReportScrollConfig(ReportBean reportbean)
    {
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        alrbean.setFixedcols(null,0);
        alrbean.setFixedrows(0);
        boolean isShowScrollX=reportbean.getScrollwidth()!=null&&!reportbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=reportbean.getScrollheight()!=null&&!reportbean.getScrollheight().trim().equals("");
        if(Consts_Private.SCROLLSTYLE_IMAGE.equals(reportbean.getScrollstyle())&&isShowScrollY)
        {//要显示图片垂直滚动条
            String[] htmlsizeArr=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(reportbean.getHeight());
            if(htmlsizeArr==null||htmlsizeArr[0].equals("")||htmlsizeArr[0].equals("0"))
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()
                        +"失败,blocklist报表类型要显示图片垂直滚动条时，必须配置height属性，否则在firefox/chrome等浏览器上显示不出垂直滚动条");
            }else
            {
                if(htmlsizeArr[1]!=null&&htmlsizeArr[1].equals("%"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()
                            +"失败,blocklist报表类型要显示图片垂直滚动条时，必须配置height属性，且不能配置为百分比，否则在firefox/chrome等浏览器上显示不出垂直滚动条");
                }
            }
        }
        ComponentAssistant.getInstance().doPostLoadForComponentScroll(reportbean,isShowScrollX,isShowScrollY,reportbean.getScrollwidth(),
                reportbean.getScrollheight(),reportbean.getScrollstyle());
    }
}
