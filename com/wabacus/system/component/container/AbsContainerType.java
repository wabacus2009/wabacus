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
package com.wabacus.system.component.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.dataexport.WordRichExcelExportBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.AbsApplicationType;
import com.wabacus.system.component.application.html.HtmlTemplateApp;
import com.wabacus.system.component.application.jsp.JspTemplateApp;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts;

public abstract class AbsContainerType extends AbsComponentType
{
    private static Log log=LogFactory.getLog(AbsContainerType.class);
    
    protected AbsContainerConfigBean containerConfigBean;
    
    protected Map<String,IComponentType> mChildren;

    protected List<String> lstChildrenIds;
    
    public AbsContainerType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        this.containerConfigBean=(AbsContainerConfigBean)comCfgBean;
    }
    
    public AbsContainerConfigBean getContainerConfigBean()
    {
        return containerConfigBean;
    }

    public Map<String,IComponentType> getMChildren()
    {
        return mChildren;
    }

    public List<String> getLstChildrenIds()
    {
        return lstChildrenIds;
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        IComponentType childTypeObj;
        IComponentConfigBean childConfigBean;
        AbsContainerConfigBean containerConfigBean=(AbsContainerConfigBean)applicationConfigBean;
        for(String childid:containerConfigBean.getLstChildrenIDs())
        {
            childConfigBean=containerConfigBean.getMChildren().get(childid);
            if(childConfigBean instanceof ReportBean)
            {
                if(((ReportBean)childConfigBean).isSlaveReportDependsonListReport()) continue; 
                childTypeObj=Config.getInstance().getReportType(((ReportBean)childConfigBean).getType());
                ((AbsReportType)childTypeObj).initUrl((ReportBean)childConfigBean,rrequest);
            }else if(childConfigBean instanceof AbsContainerConfigBean)
            {
                Config.getInstance().getContainerType(((AbsContainerConfigBean)childConfigBean).getTagname()).initUrl(childConfigBean,rrequest);
            }
        }
    }

    
    public List<ReportBean> initDisplayOnPage()
    {
        List<ReportBean> lstReportbeans=new ArrayList<ReportBean>();
        mChildren=new HashMap<String,IComponentType>();
        IComponentConfigBean childConfigBeanTmp;
        IComponentType childTypeObjTmp;
        AbsReportType reportObj;
        lstChildrenIds=this.getDisplayChildIds();
        if(lstChildrenIds==null||lstChildrenIds.size()==0) return lstReportbeans;
        for(String childIDTmp:lstChildrenIds)
        {
            childConfigBeanTmp=containerConfigBean.getMChildren().get(childIDTmp);
            if(childConfigBeanTmp==null) continue;
            childTypeObjTmp=rrequest.getComponentTypeObj(childConfigBeanTmp,this,true);
            if(childTypeObjTmp instanceof AbsReportType)
            {
                ReportBean rbTmp=(ReportBean)childConfigBeanTmp;
                reportObj=(AbsReportType)childTypeObjTmp;
                if(!rbTmp.isSlaveReportDependsonListReport())
                {//如果当前报表不是依赖列表报表的从报表（从报表的初始化不会经过所属容器，而是直接在rrequest的初始化方法中调用）
                    lstReportbeans.add(rbTmp);
                    reportObj.init();
                    reportObj.loadReportData(true);
                }
                mChildren.put(childIDTmp,reportObj);
            }else if(childTypeObjTmp instanceof AbsContainerType)
            {
                AbsContainerType childTypeObj=(AbsContainerType)childTypeObjTmp;
                lstReportbeans.addAll(childTypeObj.initDisplayOnPage());
                mChildren.put(childIDTmp,childTypeObj);
            }else if(childTypeObjTmp instanceof HtmlTemplateApp||childTypeObjTmp instanceof JspTemplateApp)
            {
                mChildren.put(childIDTmp,childTypeObjTmp);
            }
        }
        if(this.containerConfigBean.getLstOnloadMethods()!=null&&this.containerConfigBean.getLstOnloadMethods().size()>0)
        {
            rrequest.getWResponse().addOnloadMethod(this.containerConfigBean.getOnloadMethodName(),"",false);
        }
        if(rrequest.getServerActionBean()!=null) rrequest.getServerActionBean().executeServerAction(this.comCfgBean);
        return lstReportbeans;
    }
    
    protected boolean shouldShowThisContainer()
    {
        if(rrequest.getSlaveReportBean()!=null) return false;
        if(!(rrequest.getRefreshComponentBean() instanceof AbsContainerConfigBean)) 
            return false;
        if(rrequest.getRefreshComponentBean()==this.containerConfigBean) return true;
        if(((AbsContainerConfigBean)rrequest.getRefreshComponentBean()).isExistChildId(this.containerConfigBean.getId(),true,true))
        {//当前容器属于本次要刷新的容器的一部分
            return true;
        }
        return false;
    }
   
    protected List<String> getDisplayChildIds()
    {
        return this.containerConfigBean.getLstChildrenIDs();
    }

    public String getRealParenttitle()
    {
        String realparenttitle=containerConfigBean.getParenttitle(rrequest);
        if(realparenttitle!=null&&!realparenttitle.trim().equals("")) return realparenttitle.trim();
        realparenttitle=containerConfigBean.getTitle(rrequest);
        if(realparenttitle!=null&&!realparenttitle.trim().equals("")) return realparenttitle.trim();
        IComponentType childTypeObjTmp;
        for(String childidTmp:containerConfigBean.getLstChildrenIDs())
        {
            childTypeObjTmp=mChildren.get(childidTmp);
            if(childTypeObjTmp==null) continue;
            realparenttitle=childTypeObjTmp.getRealParenttitle();
            if(realparenttitle!=null&&!realparenttitle.trim().equals("")) break;
        }
        realparenttitle=realparenttitle==null?"":realparenttitle.trim();
        return realparenttitle;
    }
    
    public String getChildDisplayWidth(IComponentConfigBean childBean)
    {
        if(childBean instanceof ReportBean)
        {
            return ((ReportBean)childBean).getDisplayWidth();
        }else
        {
            return childBean.getWidth();
        }
    }
    
    protected String showContainerStartPart()
    {
        StringBuilder resultBuf=new StringBuilder();
        if((containerConfigBean instanceof PageBean)||this.getParentContainerType()!=null)
        {//如果当前是在显示顶层<page/>，或者本次不是单独刷新此容器，而是刷新其某层父容器，则要完整地更新此容器
            String containerwidth=null;
            if(containerConfigBean.getParentContainer()==null)
            {//如果当前容器是<page/>
                containerwidth=containerConfigBean.getWidth();
            }else
            {
                containerwidth=this.getParentContainerType().getChildDisplayWidth(containerConfigBean);
            }
            if(containerwidth==null||containerwidth.trim().equals(""))
            {
                containerwidth="100%";
            }
            if(containerConfigBean.getTop()!=null&&!containerConfigBean.getTop().trim().equals(""))
            {
                resultBuf.append("<table  cellspacing='0' cellpadding='0' width=\""+containerwidth+"\" style=\"MARGIN:0;\">");
                resultBuf.append("<tr><td height=\""+containerConfigBean.getTop()+"\"></td></tr></table>");
            }
            if(this.getParentContainerType()!=null)
            {//是子容器，不是<page/>（如果是<page/>，则自己显示了<outerheader/>）
                resultBuf.append(getRealHeaderFooterDisplayValue(containerConfigBean.getOuterHeaderTplBean(),"outerheader"));
            }
            resultBuf.append("<table  cellspacing='0' cellpadding='0' width=\""+containerwidth+"\" id=\""+containerConfigBean.getGuid()+"\"><tr><td>");
        }
        if(!(containerConfigBean instanceof PageBean))
        {//<page/>的<span/>在PageType类中生成
            resultBuf.append("<div id=\"WX_CONTENT_"+containerConfigBean.getGuid()+"\">");//后面的操作需要刷新当前容器时，从这里开始刷新，这样不需用到父容器来获取它的宽度
        }
        resultBuf.append(this.showHeader());
        resultBuf.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin:0;\">");
        if(containerConfigBean.isTitleInLeft()||containerConfigBean.isTitleInRight())
        {
            String leftrighttitle=showLeftRightTitlePart();
            resultBuf.append(showTopBottomButtonsWhenTitleInLeftRight(leftrighttitle,true));
            resultBuf.append("<tr>");
            if(containerConfigBean.isTitleInLeft()) resultBuf.append(leftrighttitle);//显示左侧标题
        }else
        {
            resultBuf.append(showTopBottomTitlePart(true));
            resultBuf.append("<tr>");
        }
        resultBuf.append("<td>");
        resultBuf.append("<table cellspacing='0' cellpadding='0' class=\"cls-"+containerConfigBean.getTagname()+"-content\" width=\"100%\"");
        if(containerConfigBean.shouldShowContextMenu())
        {
            resultBuf.append(" oncontextmenu=\"try{showcontextmenu('contextmenu_"+containerConfigBean.getGuid()
                    +"',event);}catch(e){logErrorsAsJsFileLoad(e);}\"");
        }
        if(containerConfigBean.getBorder()!=0)
        {
            resultBuf.append(" style=\"border:solid ").append(containerConfigBean.getBorder()).append("px ");
            if(containerConfigBean.getBordercolor()!=null&&!containerConfigBean.getBordercolor().trim().equals(""))
            {
                resultBuf.append(containerConfigBean.getBordercolor());
            }
            resultBuf.append(";\"");
        }else
        {//配置为不显示边框
            resultBuf.append(" style=\"border:0px;\"");
        }
        resultBuf.append(">");
        resultBuf.append(showContainerScrollStartTag());
        if(containerConfigBean.getMargin_top()!=null&&!containerConfigBean.getMargin_top().trim().equals(""))
        {
            resultBuf.append("<tr><td colspan=\""+containerConfigBean.getColspan_total()+"\" height=\"").append(containerConfigBean.getMargin_top())
                    .append("\"></td></tr>");
        }
        resultBuf.append("<tr>");
        if(containerConfigBean.getMargin_left()!=null&&!containerConfigBean.getMargin_left().trim().equals(""))
        {
            resultBuf.append("<td width=\"").append(containerConfigBean.getMargin_left()).append("\"><span style=\"margin-left:"+containerConfigBean.getMargin_left()+"\"></span></td>");
        }
        resultBuf.append("<td>");
        return resultBuf.toString();
    }
    
    protected String showContainerTableTag()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<table cellspacing='0' cellpadding='0' width=\"100%\"");
        if(!this.containerConfigBean.isScrollY()&&containerConfigBean.getHeight()!=null&&!containerConfigBean.getHeight().trim().equals(""))
        {//容器的高度配置必须放在最里层的<table/>中，否则没办法通过它的<td/>的valign控制子组件的垂直对齐方式（如果有垂直滚动条，则不在这里指定高度）
            resultBuf.append(" height=\""+containerConfigBean.getHeight()+"\"");
        }
        resultBuf.append(">");
        return resultBuf.toString();
    }
    
    private String showTopBottomButtonsWhenTitleInLeftRight(String leftrighttitle,boolean isTopPosition)
    {
        String buttonsOnTitle=showTopBottomTitlePart(isTopPosition);
        if(buttonsOnTitle.trim().equals("")) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<tr>");
        if(!leftrighttitle.trim().equals(""))
        {//在左侧或右侧有标题要显示（则有两个<td/>，一个显示容器标题，一个显示容器内容）
            if(containerConfigBean.isTitleInLeft())
            {//如果标题显示在左边
                resultBuf.append("<td>&nbsp;</td>");
                resultBuf.append("<td><table cellspacing='0' cellpadding='0'  width=\"100%\">").append(buttonsOnTitle).append("</table></td>");
            }else
            {
                resultBuf.append("<td><table cellspacing='0' cellpadding='0'  width=\"100%\">").append(buttonsOnTitle).append("</table></td>");
                resultBuf.append("<td>&nbsp;</td>");
            }
        }else
        {//不用显示左右标题（可能是没配置，或被授权为不显示），则只有一个<td/>用于显示容器内容
            resultBuf.append("<td><table cellspacing='0' cellpadding='0'  width=\"100%\">").append(buttonsOnTitle).append("</table></td>");
        }
        resultBuf.append("</tr>");
        return resultBuf.toString();
    }

    protected String showLeftRightTitlePart()
    {
        if(!rrequest.checkPermission(containerConfigBean.getId(),Consts.TITLE_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        String realtitle=containerConfigBean.getTitle(rrequest);
        if(realtitle==null||realtitle.trim().equals("")) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<td width=\"20\"");
        String titlealign=containerConfigBean.getTitlealign();
        if(titlealign==null||titlealign.trim().equals("")) titlealign="top";
        resultBuf.append(" valign=\"").append(titlealign).append("\">");
        resultBuf.append("<table cellpadding=\"0\" width=\"100%\" cellspacing=\"0\" class=\"cls-title-table\"");
        //        {//标题显示在底部
        //        {//标题显示在顶部
        //            resultBuf.append(" class=\"cls-title-left-table\"");
        resultBuf.append(">");
        resultBuf.append("<tr><TD class=\"cls-title\">").append(
                containerConfigBean.getTitle(rrequest)).append("</TD></tr>");
        String subtitle=this.containerConfigBean.getSubtitle(rrequest);
        if(subtitle!=null&&!subtitle.trim().equals(""))
        {
            resultBuf.append("<tr><TD class=\"cls-subtitle\" ");
//            {//标题显示在右边
//            }else
            resultBuf.append(">").append(subtitle.trim()).append("</TD></tr>");
        }
        resultBuf.append("</table></td>");
        return resultBuf.toString();
    }

    protected String showTopBottomTitlePart(boolean isDisplayTopTitleBar)
    {
        String realtitle="";
        if((isDisplayTopTitleBar&&this.containerConfigBean.isTitleInTop())||(!isDisplayTopTitleBar&&this.containerConfigBean.isTitleInBottom()))
        {
            if(rrequest.checkPermission(containerConfigBean.getId(),Consts.TITLE_PART,null,Consts.PERMISSION_TYPE_DISPLAY))
            {//授权为显示标题栏（则要显示标题和副标题）
                realtitle=this.getDisplayRealTitleAndSubTitle();
            }
        }
        String buttonsOnTitle=getContainerTopBottomButtonsDisplayValue(isDisplayTopTitleBar);
        if(realtitle.trim().equals("")&&buttonsOnTitle.trim().equals("")) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<TR><TD>");
        resultBuf.append(getTitleDisplayValue(realtitle,buttonsOnTitle));
        resultBuf.append("</TD></TR>");
        return resultBuf.toString();
    }

    protected String getContainerTopBottomButtonsDisplayValue(boolean isDisplayTopTitleBar)
    {
        String buttonsOnTitle="";
        if(this.containerConfigBean.getButtonsBean()!=null)
        {
            if(isDisplayTopTitleBar)
            {
                buttonsOnTitle=this.containerConfigBean.getButtonsBean().showButtons(rrequest,"top");
            }else
            {
                buttonsOnTitle=this.containerConfigBean.getButtonsBean().showButtons(rrequest,"bottom");
            }
        }
        return buttonsOnTitle.trim();
    }

    protected String showContainerScrollStartTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        StringBuffer resultBuf=new StringBuffer();
        if((!this.containerConfigBean.isScrollX())&&!this.containerConfigBean.isScrollY()) return "";
        resultBuf.append("<tr><td>");
        resultBuf.append(ComponentAssistant.getInstance().showComponentScrollStartPart(this.containerConfigBean,containerConfigBean.isScrollX(),
                containerConfigBean.isScrollY(),containerConfigBean.getWidth(),containerConfigBean.getHeight(),containerConfigBean.getScrollstyle()));
        resultBuf.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin:0;\">");
        return resultBuf.toString();
    }
    
    protected String showContainerScrollEndTag()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if((!this.containerConfigBean.isScrollX())&&!this.containerConfigBean.isScrollY()) return "";//不需显示滚动条
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("</table>");
        resultBuf.append(ComponentAssistant.getInstance().showComponentScrollEndPart(containerConfigBean.getScrollstyle(),this.containerConfigBean.isScrollX(),
                this.containerConfigBean.isScrollY()));
        resultBuf.append("</td></tr>");
        return resultBuf.toString();
    }
    
    protected void showChildObj(IComponentType childObj,String tdwidth)
    {
        IComponentConfigBean childConfigBean=childObj.getConfigBean();
        boolean hasLeftRight=false;
        if((childConfigBean.getLeft()!=null&&!childConfigBean.getLeft().trim().equals(""))
                ||(childConfigBean.getRight()!=null&&!childConfigBean.getRight().trim().equals("")))
        {
            hasLeftRight=true;
        }
        StringBuffer tempBuf=new StringBuffer();
        tempBuf.append("<td");
        if(tdwidth!=null&&!tdwidth.trim().equals(""))
        {
            tempBuf.append(" width=\""+tdwidth+"\"");
        }
        if(childConfigBean.getValign()!=null&&!childConfigBean.getValign().trim().equals(""))
        {
            tempBuf.append(" valign=\""+childConfigBean.getValign()+"\"");
        }
        if(childConfigBean.getAlign()!=null&&!childConfigBean.getAlign().trim().equals(""))
        {
            tempBuf.append(" align=\""+childConfigBean.getAlign()+"\"");
        }
        wresponse.println(tempBuf.toString()+">");
        if(hasLeftRight)
        {
            wresponse.println("<table cellspacing='0' cellpadding='0' width=\"100%\" border=\"0\" style=\"MARGIN:0;\"><tr>");
            if(childConfigBean.getLeft()!=null&&!childConfigBean.getLeft().trim().equals(""))
            {
                wresponse.println("<td width=\""+childConfigBean.getLeft()+"\"></td>");
            }
            tempBuf=new StringBuffer();
            tempBuf.append("<td");
            if(childConfigBean.getValign()!=null&&!childConfigBean.getValign().trim().equals(""))
            {
                tempBuf.append(" valign=\""+childConfigBean.getValign()+"\"");
            }
            if(childConfigBean.getAlign()!=null&&!childConfigBean.getAlign().trim().equals(""))
            {
                tempBuf.append(" align=\""+childConfigBean.getAlign()+"\"");
            }
            wresponse.println(tempBuf.toString()+">");
        }
        childObj.displayOnPage(null);
        if(childConfigBean.getRight()!=null&&!childConfigBean.getRight().trim().equals(""))
        {//配置了右边距
            wresponse.println("<td width=\""+childConfigBean.getRight()+"\"><span style=\"margin-left:"+childConfigBean.getRight()+"\"></span></td>");
        }
        if(hasLeftRight)
        {
            wresponse.println("</tr></table>");
        }
        wresponse.println("</td>");
    }

    protected String showContainerEndPart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("</td>");
        if(containerConfigBean.getMargin_right()!=null&&!containerConfigBean.getMargin_right().trim().equals(""))
        {
            resultBuf.append("<td width=\"").append(containerConfigBean.getMargin_right()).append("\"><span style=\"margin-left:"+containerConfigBean.getMargin_right()+"\"></span></td>");
        }
        resultBuf.append("</tr>");
        if(containerConfigBean.getMargin_bottom()!=null&&!containerConfigBean.getMargin_bottom().trim().equals(""))
        {
            resultBuf.append("<tr><td colspan=\""+containerConfigBean.getColspan_total()+"\" height=\"").append(
                    containerConfigBean.getMargin_bottom()).append("\"></td></tr>");
        }
        resultBuf.append(showContainerScrollEndTag());
        resultBuf.append("</table>");
//        resultBuf.append(showContainerScrollEndTag());//显示滚动条结束部分
        resultBuf.append("</td>");
        if(containerConfigBean.isTitleInLeft()||containerConfigBean.isTitleInRight())
        {
            String leftrighttitle=showLeftRightTitlePart();
            if(containerConfigBean.isTitleInRight()) resultBuf.append(leftrighttitle);//显示右侧标题
            resultBuf.append("</tr>");
            resultBuf.append(showTopBottomButtonsWhenTitleInLeftRight(leftrighttitle,false));
        }else
        {
            resultBuf.append("</tr>");
            resultBuf.append(showTopBottomTitlePart(false));
        }
        resultBuf.append("</table>");
        resultBuf.append(this.showFooter());
//        resultBuf.append(this.showContextMenu());
        resultBuf.append(this.showMetaData());
        if(!(containerConfigBean instanceof PageBean)) resultBuf.append("</div>");
        if((containerConfigBean instanceof PageBean)||this.getParentContainerType()!=null)
        {//如果当前是在显示顶层<page/>，或者本次不是单独刷新此容器，而是刷新其某层父容器
            resultBuf.append("</td></tr></table>");
            if(this.getParentContainerType()!=null)
            {//是子容器，不是<page/>（如果是<page/>，则自己显示了<outerheader/>）
                resultBuf.append(getRealHeaderFooterDisplayValue(containerConfigBean.getOuterFooterTplBean(),"outerfooter"));
            }
            if(containerConfigBean.getBottom()!=null&&!containerConfigBean.getBottom().trim().equals(""))
            {
                String containerwidth=null;
                if(containerConfigBean.getParentContainer()==null)
                {//如果当前容器是<page/>
                    containerwidth=containerConfigBean.getWidth();
                }else
                {
                    containerwidth=this.getParentContainerType().getChildDisplayWidth(containerConfigBean);
                }
                if(containerwidth==null||containerwidth.trim().equals(""))
                {
                    containerwidth="100%";
                }
                resultBuf.append("<table  cellspacing=\"0\" cellpadding=\"0\" width=\""+containerwidth+"\" style=\"MARGIN:0;\">");
                resultBuf.append("<tr><td height=\""+containerConfigBean.getBottom()+"\"></td></tr></table>");
            }
        }
        return resultBuf.toString();
    }

    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(" childComponentIds=\"");
        for(String childidTmp:this.containerConfigBean.getLstChildrenIDs())
        {
            resultBuf.append(childidTmp).append(";");
        }
        if(resultBuf.charAt(resultBuf.length()-1)==';')
        {
            resultBuf.deleteCharAt(resultBuf.length()-1);
        }
        resultBuf.append("\"");
        return resultBuf.toString();
    }
    
    public Object getChildObjById(String id,boolean inherit)
    {
        if(mChildren==null||mChildren.size()==0) return null;
        Object obj=mChildren.get(id);
        if(obj!=null)
        {
            return obj;
        }else if(!inherit)
        {
            return null;
        }
        Iterator<String> itKeys=mChildren.keySet().iterator();
        String idTemp;
        while(itKeys.hasNext())
        {
            idTemp=itKeys.next();
            obj=mChildren.get(idTemp);
            if(obj==null||!(obj instanceof AbsContainerType)) continue;
            obj=((AbsContainerType)obj).getChildObjById(id,inherit);
            if(obj!=null) return obj;
        }
        return null;
    }

    public void displayOnExportDataFile(Object templateObj,boolean isFirstime)
    {
        if(!rrequest.checkPermission(this.comCfgBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL
                &&(rrequest.checkPermission(comCfgBean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_RICHEXCEL+"}",Consts.PERMISSION_TYPE_DISABLED)||!rrequest
                        .checkPermission(comCfgBean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_RICHEXCEL+"}",Consts.PERMISSION_TYPE_DISPLAY)))
        {//如果当前是导出richexcel，但没有这个权限，则返回空
            return;
        }
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_WORD
                &&(rrequest.checkPermission(comCfgBean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_WORD+"}",Consts.PERMISSION_TYPE_DISABLED)||!rrequest
                        .checkPermission(comCfgBean.getId(),Consts.BUTTON_PART,"type{"+Consts.DATAEXPORT_WORD+"}",Consts.PERMISSION_TYPE_DISPLAY)))
        {
            return;
        }
        if(templateObj instanceof TemplateBean)
        {
            ((TemplateBean)templateObj).printDisplayValue(this.rrequest,this);
        }else if(templateObj!=null&&!templateObj.toString().trim().equals(""))
        {
            WabacusAssistant.getInstance().includeDynTpl(rrequest,this,templateObj.toString().trim());
        }else
        {
            List<String> lstApplicationids=this.containerConfigBean.getLstAllChildApplicationIds(true);
            AbsApplicationType appTypeObjTmp;
            WordRichExcelExportBean debeanTmp;
            boolean hasExportChild=false;//本次是否导出了此容器的有效子应用
            for(String appidTmp:lstApplicationids)
            {
                if(!rrequest.getLstApplicationIds().contains(appidTmp)) continue;
                hasExportChild=true;
                appTypeObjTmp=(AbsApplicationType)rrequest.getComponentTypeObj(appidTmp,null,true);
                Object tplObjTmp=null;
                if(appTypeObjTmp.getConfigBean().getDataExportsBean()!=null)
                {
                    debeanTmp=(WordRichExcelExportBean)appTypeObjTmp.getConfigBean().getDataExportsBean().getDataExportBean(rrequest.getShowtype());
                    if(debeanTmp!=null) tplObjTmp=debeanTmp.getDataExportTplObj();
                }
                appTypeObjTmp.displayOnExportDataFile(tplObjTmp,true);
            }
            if(!hasExportChild)
            {
                log.warn("没有导出容器"+this.comCfgBean.getPath()+"中任何子应用");
            }
        }
    }

    public abstract AbsContainerConfigBean loadConfig(XmlElementBean eleContainer,AbsContainerConfigBean parent,String tagname);

}
