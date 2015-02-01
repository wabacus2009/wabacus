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

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public abstract class AbsDetailReportType extends AbsReportType
{
    public final static String KEY=AbsDetailReportType.class.getName();
    
    public AbsDetailReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    protected String showReportScrollStartTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        boolean isShowScrollX=rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("");
        return ComponentAssistant.getInstance().showComponentScrollStartPart(rbean,isShowScrollX,isShowScrollY,rbean.getScrollwidth(),
                rbean.getScrollheight(),rbean.getScrollstyle());
    }

    protected String showReportScrollEndTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        boolean isShowScrollX=rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("");
        return ComponentAssistant.getInstance().showComponentScrollEndPart(rbean.getScrollstyle(),isShowScrollX,isShowScrollY);
    }

    protected String showColLabel(ColBean cbean,AbsReportDataPojo rowDataObj)
    {
        String label=cbean.getLabel(rrequest);
        if(label==null||ColBean.NON_LABEL.equals(label)) return "";//<col/>的label没有配置时，不为它显示标题列
        StringBuffer resultBuf=new StringBuffer();
        String labelstyleproperty=this.getColLabelStyleproperty(cbean,rowDataObj);
        ColDisplayData colDisplayData=ColDisplayData.getColDataFromInterceptor(this,cbean,null,-1,labelstyleproperty,label);
        label=colDisplayData.getValue();
        resultBuf.append("<td class='cls-data-th-detail'");
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE)
        {
            String dataheaderbgcolor=Config.getInstance().getSkinConfigValue(rrequest.getPageskin(),"table.dataheader.bgcolor");
            if(dataheaderbgcolor==null) dataheaderbgcolor="";
            resultBuf.append(" bgcolor='"+dataheaderbgcolor+"'");
        }
        resultBuf.append(" ").append(colDisplayData.getStyleproperty());
        if(label.equals("")) label="&nbsp;";
        resultBuf.append(">"+label+"</td>");
        return resultBuf.toString();
    }
    
    protected  String getDefaultNavigateKey()
    {
        return Consts.DETAILREPORT_NAVIGATE_DEFAULT;
    }

    public boolean isSupportHorizontalDataset(ReportBean reportbean)
    {
        return false;
    }

    public int afterReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        super.afterReportLoading(reportbean,lstEleReportBeans);
        reportbean.setCellresize(0);
        return 1;
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        super.afterColLoading(colbean,lstEleColBeans);
        if(colbean.isSequenceCol())
        {
            throw new WabacusConfigLoadingException("报表"+colbean.getReportBean().getPath()+"为数据细览报表，不允许<col/>标签的column属性配置为自动增长列");
        }
        if(colbean.isRowSelectCol())
        {
            throw new WabacusConfigLoadingException("报表"+colbean.getReportBean().getPath()+"为数据细览报表，不允许<col/>标签的column属性配置为行选中列");
        }
        if(colbean.isRoworderCol())
        {
            throw new WabacusConfigLoadingException("报表"+colbean.getReportBean().getPath()+"为数据细览报表，不允许<col/>标签的column属性配置为行排序列");
        }
        return 1;
    }

    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        DisplayBean dbean=reportbean.getDbean();
        List<ColBean> lstColBeans=dbean.getLstCols();
        if(Consts_Private.REPORT_BORDER_VERTICAL.equals(reportbean.getBorder())||Consts_Private.REPORT_BORDER_HORIZONTAL2.equals(reportbean.getBorder()))
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，细览报表不支持"
                    +Consts_Private.REPORT_BORDER_VERTICAL+"和"+Consts_Private.REPORT_BORDER_HORIZONTAL2+"两种边框类型");
        }
        if(lstColBeans!=null&&lstColBeans.size()>0)
        {
            for(ColBean cbean:lstColBeans)
            {
                String borderstyle=cbean.getBorderStylePropertyOnColBean();
                if(borderstyle!=null&&!borderstyle.trim().equals(""))
                {
                    cbean.setValuestyleproperty(Tools.mergeHtmlTagPropertyString(cbean.getValuestyleproperty(null,true),"style=\""+borderstyle+"\"",1),true);
                    cbean.setLabelstyleproperty(Tools.mergeHtmlTagPropertyString(cbean.getLabelstyleproperty(null,true),"style=\""+borderstyle+"\"",1),true);
                }
            }
        }
        
        boolean isShowScrollX=reportbean.getScrollwidth()!=null&&!reportbean.getScrollwidth().trim().equals("");
        boolean isShowScrollY=reportbean.getScrollheight()!=null&&!reportbean.getScrollheight().trim().equals("");
        ComponentAssistant.getInstance().doPostLoadForComponentScroll(reportbean,isShowScrollX,isShowScrollY,reportbean.getScrollwidth(),
                reportbean.getScrollheight(),reportbean.getScrollstyle());
        return 1;
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_DETAIL;
    }
    
    public abstract String showColData(ColBean cbean,boolean showpart,String dynstyleproperty);
}
