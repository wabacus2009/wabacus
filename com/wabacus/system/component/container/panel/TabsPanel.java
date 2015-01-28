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
package com.wabacus.system.component.container.panel;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.panel.TabsPanelBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.util.Consts;

public class TabsPanel extends AbsPanelType
{
    private static Log log=LogFactory.getLog(TabsPanel.class);

    private TabItemDisplayBean currentSelectedTabItemDisplayBean=null;
    
    private List<TabItemDisplayBean> lstDisplayedChildren;

    private TabsPanelBean tabspanelBean;
    
    public TabsPanel(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        tabspanelBean=(TabsPanelBean)comCfgBean;
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        super.initUrl(applicationConfigBean,rrequest);
        rrequest.addParamToUrl(applicationConfigBean.getId()+"_selectedIndex","rrequest{"+applicationConfigBean.getId()+"_selectedIndex}",true);
    }

    protected List<String> getDisplayChildIds()
    {
        initDisplayTabItems();
        List<String> lstResults=new ArrayList<String>();
        if(currentSelectedTabItemDisplayBean==null) return lstResults;
        if(tabspanelBean.isAsyn())
        {
            lstResults.add(currentSelectedTabItemDisplayBean.getChildid());
        }else
        {
            for(TabItemDisplayBean tidbeanTmp:lstDisplayedChildren)
            {//所有显示了标题的，且不是禁用的，都显示出来，切换时只要在客户端切换即可
                if(!tidbeanTmp.isDisabled()) lstResults.add(tidbeanTmp.getChildid());
            }
        }
        return lstResults;
    }

    private void initDisplayTabItems()
    {
        lstDisplayedChildren=new ArrayList<TabItemDisplayBean>();
        if(!rrequest.checkPermission(this.containerConfigBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY)) return;
        int childscount=containerConfigBean.getLstChildrenIDs().size();
        if(childscount<=0) return;
        int currentSelectedTabItemIdx=-1;
        String selectedIndex=rrequest.getStringAttribute(this.containerConfigBean.getId()+"_selectedIndex");
        if(selectedIndex!=null&&!selectedIndex.trim().equals(""))
        {
            try
            {
                currentSelectedTabItemIdx=Integer.parseInt(selectedIndex.trim());
            }catch(NumberFormatException e)
            {
                log.warn("传入的"+this.containerConfigBean.getId()+"_selectedIndex不是有效序号",e);
            }
        }
        if(currentSelectedTabItemIdx<0||currentSelectedTabItemIdx>=childscount)
        {
            currentSelectedTabItemIdx=0;
            changeSelectedTabItemIdx(0);
        }
        
        String childidTmp;
        TabItemDisplayBean tdbeanTmp;
        for(int i=0;i<childscount;i++)
        {
            childidTmp=containerConfigBean.getLstChildrenIDs().get(i);
            if(!rrequest.checkPermission(containerConfigBean.getId(),Consts.DATA_PART,String.valueOf(i),Consts.PERMISSION_TYPE_DISPLAY))  continue;
            tdbeanTmp=new TabItemDisplayBean();
            tdbeanTmp.setIndex(i);//位置下标
            tdbeanTmp.setChildid(childidTmp);
            if(rrequest.checkPermission(containerConfigBean.getId(),Consts.DATA_PART,String.valueOf(i),Consts.PERMISSION_TYPE_DISABLED))
            {
                tdbeanTmp.setDisabled(true);
            }else if(i==currentSelectedTabItemIdx)
            {
                currentSelectedTabItemDisplayBean=tdbeanTmp;
            }
            lstDisplayedChildren.add(tdbeanTmp);
        }
        if(lstDisplayedChildren.size()==0) return;
        if(currentSelectedTabItemDisplayBean==null)
        {
            for(TabItemDisplayBean tabItemTmp:lstDisplayedChildren)
            {
                if(!tabItemTmp.isDisabled())
                {
                    currentSelectedTabItemDisplayBean=tabItemTmp;
                    changeSelectedTabItemIdx(currentSelectedTabItemDisplayBean.getIndex());
                    break;
                }
            }
        }
    }
    
    private void changeSelectedTabItemIdx(int newtabitemIdx)
    {
        rrequest.setAttribute(this.containerConfigBean.getId()+"_selectedIndex",newtabitemIdx);
        rrequest.addParamToUrl(containerConfigBean.getId()+"_selectedIndex",String.valueOf(newtabitemIdx),true);
    }

    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(currentSelectedTabItemDisplayBean==null)
        {//如果没有要显示的子标签
            wresponse.println("&nbsp;");
            return;
        }
        wresponse.println(showContainerStartPart());
        if(mChildren!=null&&mChildren.size()>0)
        {
            IComponentType childObjTmp;
            if(this.tabspanelBean.isAsyn())
            {
                wresponse.println(showContainerTableTag());
                childObjTmp=this.mChildren.entrySet().iterator().next().getValue();
                wresponse.println("<TR>");
                showChildObj(childObjTmp,null);
                wresponse.println("</TR>");
                wresponse.println("</table>");
            }else
            {
                showChildTabItem(this.currentSelectedTabItemDisplayBean,true);
                for(TabItemDisplayBean tidbeanTmp:lstDisplayedChildren)
                {//所有显示了标题的，且不是禁用的，都显示出来，切换时只要在客户端切换即可
                    if(tidbeanTmp.isDisabled()) continue;
                    if(tidbeanTmp.getIndex()==this.currentSelectedTabItemDisplayBean.getIndex()) continue;
                    showChildTabItem(tidbeanTmp,false);
                }
            }
        }
        wresponse.println(showContainerEndPart());
    }

    
    private void showChildTabItem(TabItemDisplayBean tidbean,boolean isDisplay)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<table cellspacing='0' cellpadding='0' width=\"100%\"");
        if(!this.containerConfigBean.isScrollY()&&containerConfigBean.getHeight()!=null&&!containerConfigBean.getHeight().trim().equals(""))
        {//容器的高度配置必须放在最里层的<table/>中，否则没办法通过它的<td/>的valign控制子组件的垂直对齐方式
            resultBuf.append(" height=\""+containerConfigBean.getHeight()+"\"");
        }
        resultBuf.append(" id=\""+this.containerConfigBean.getGuid()+"_"+tidbean.getIndex()+"_content\"");//分配一个ID属性，以便在客户端切换时能取到此<table/>对象
        if(!isDisplay)
        {
            resultBuf.append(" style=\"display:none;\"");
        }
        resultBuf.append(">");
        wresponse.println(resultBuf.toString());
        wresponse.println("<TR>");
        showChildObj(this.mChildren.get(tidbean.getChildid()),null);
        wresponse.println("</TR>");
        wresponse.println("</table>");
        
    }

    protected String showLeftRightTitlePart()
    {
        if(currentSelectedTabItemDisplayBean==null) return "";
        if(!rrequest.checkPermission(containerConfigBean.getId(),Consts.TITLE_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<td width=\"20\"");
        if(containerConfigBean.isTitleInRight())
        {//标题显示在右侧
            resultBuf.append(" class=\"cls-tabtitle-right-parenttd\"");
        }else
        {
            resultBuf.append(" class=\"cls-tabtitle-left-parenttd\"");
        }
        String titlealign=containerConfigBean.getTitlealign();
        if(titlealign==null||titlealign.trim().equals("")) titlealign="top";
        resultBuf.append(" valign=\"").append(titlealign).append("\">");
        
        resultBuf.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"");
        if(containerConfigBean.isTitleInRight())
        {
            resultBuf.append(" class=\"cls-tabtitle-right-table\"");
        }else
        {
            resultBuf.append(" class=\"cls-tabtitle-left-table\"");
        }
        if(!this.tabspanelBean.isAsyn())
        {
            resultBuf.append(" selectedItemIndex=\"").append(currentSelectedTabItemDisplayBean.getIndex()).append("\"");//记录当前选中的标签页下标，以便切换时用上
        }
        resultBuf.append(">");
        String titlewidth=((TabsPanelBean)containerConfigBean).getTitlewidth();
        String titlestyle=((TabsPanelBean)containerConfigBean).getTitlestyle();
        TabItemDisplayBean tdbeanTmp;
        for(int i=0,len=lstDisplayedChildren.size();i<len;i++)
        {
            tdbeanTmp=lstDisplayedChildren.get(i);
            resultBuf.append("<tr><TD ");
            String titleclassname=null;
            if(titlestyle!=null&&titlestyle.equals("2"))
            {
                resultBuf.append(" nowrap ");
                if(titlewidth!=null&&!titlewidth.trim().equals("")) resultBuf.append(" style=\"width:").append(titlewidth).append("\"");
                titleclassname="cls-tabtitle-leftright2";
            }else
            {
                if(titlewidth!=null&&!titlewidth.trim().equals("")) resultBuf.append("style=\"height:").append(titlewidth).append("\"");
                titleclassname="cls-tabtitle-leftright";
            }
            resultBuf.append(" id=\""+this.containerConfigBean.getGuid()+"_"+tdbeanTmp.getIndex()+"_title\"");
            if(tdbeanTmp.getIndex()==currentSelectedTabItemDisplayBean.getIndex())
            {
                resultBuf.append(" class=\""+titleclassname+"-selected\"");
                if(!this.tabspanelBean.isAsyn()) resultBuf.append(" onclick=\"try{"+getTabItemClickEvent(tdbeanTmp.getIndex())+"}catch(e){logErrorsAsJsFileLoad(e);}\"");
            }else
            {
                if(tdbeanTmp.isDisabled())
                {//如果当前标签页不可点击
                    resultBuf.append(" class=\""+titleclassname+"-disabled\"");
                }else
                {
                    resultBuf.append(" class=\""+titleclassname+"-deselected\"");
                    resultBuf.append(" onclick=\"try{"+getTabItemClickEvent(tdbeanTmp.getIndex())+"}catch(e){logErrorsAsJsFileLoad(e);}\"");
                }
            }
            resultBuf.append(">"+getTabItemTitle(tdbeanTmp.getChildid())).append("</TD></tr>");
            if(i!=len-1) resultBuf.append("<tr><td height=\"2px\"></td></tr>");
        }
        String subtitle=this.getMChildren().get(currentSelectedTabItemDisplayBean.getChildid()).getConfigBean().getParentSubtitle(rrequest);
        if(subtitle!=null&&!subtitle.trim().equals(""))
        {
            resultBuf.append("<tr><TD class=\"cls-subtitle\"");
//            {//标题显示在右边
//                resultBuf.append(" align=\"right\"");
            resultBuf.append(">").append(subtitle.trim()).append("</TD></tr>");
        }
        resultBuf.append("</table></td>");
        return resultBuf.toString();
    }

    protected String showTopBottomTitlePart(boolean isDisplayTopTitleBar)
    {
        if(currentSelectedTabItemDisplayBean==null) return "";
        if(this.containerConfigBean.isTitleInLeft()||this.containerConfigBean.isTitleInRight())
        {
            return super.showTopBottomTitlePart(isDisplayTopTitleBar);
        }
        if((isDisplayTopTitleBar&&this.containerConfigBean.isTitleInBottom())||(!isDisplayTopTitleBar&&this.containerConfigBean.isTitleInTop()))
        {
            return super.showTopBottomTitlePart(isDisplayTopTitleBar);
        }
        if(!rrequest.checkPermission(containerConfigBean.getId(),Consts.TITLE_PART,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            return super.showTopBottomTitlePart(isDisplayTopTitleBar);
        }
        String realtabtitle=getTabsTitleInTopAndBottomDisplayValue();
        if(realtabtitle.trim().equals("")) return super.showTopBottomTitlePart(isDisplayTopTitleBar);//没有标题需要显示，则只考虑按钮的显示
        String buttonsOnTitle=getContainerTopBottomButtonsDisplayValue(isDisplayTopTitleBar);
        String buttonalign=null;
        if(!buttonsOnTitle.trim().equals(""))
        {
            buttonalign=this.containerConfigBean.getButtonsBean().getAlign();
            buttonalign=buttonalign==null||buttonalign.trim().equals("")?"right":buttonalign.toLowerCase().trim();
        }
        String titlealign=containerConfigBean.getTitlealign();
        titlealign=titlealign==null?"left":titlealign.toLowerCase().trim();
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<TR><TD>");
        resultBuf.append("<TABLE cellSpacing=0 cellPadding=0 style=\"width:100%\"");
        if(containerConfigBean.isTitleInBottom())
        {
            resultBuf.append(" class=\"cls-tabtitle-bottom-table\"");
        }else
        {
            resultBuf.append(" class=\"cls-tabtitle-top-table\"");
        }
        if(buttonsOnTitle.equals("")||titlealign.equalsIgnoreCase(buttonalign))
        {
            if(!this.tabspanelBean.isAsyn())
            {//如果不是异步切换tabitem
                resultBuf.append(" selectedItemIndex=\"").append(currentSelectedTabItemDisplayBean.getIndex()).append("\"");
            }
            resultBuf.append("><TBODY><TR>");
            if(!titlealign.equals("left")) resultBuf.append("<td>&nbsp;</td>");
            if(!buttonsOnTitle.equals("")&&"left".equals(this.containerConfigBean.getButtonsBean().getTitleposition()))
            {
                resultBuf.append("<td width='1%' nowrap>");
                resultBuf.append(buttonsOnTitle);
                resultBuf
                        .append(WabacusAssistant.getInstance().getSpacingDisplayString(this.containerConfigBean.getButtonsBean().getButtonspacing()));
                resultBuf.append("</td>");
            }
            resultBuf.append(realtabtitle);
            if(!buttonsOnTitle.equals("")&&!"left".equals(this.containerConfigBean.getButtonsBean().getTitleposition()))
            {
                resultBuf.append("<td width='1%' nowrap>");
                resultBuf
                        .append(WabacusAssistant.getInstance().getSpacingDisplayString(this.containerConfigBean.getButtonsBean().getButtonspacing()));
                resultBuf.append(buttonsOnTitle);
                resultBuf.append("</td>");
            }
            if(!titlealign.equals("right")) resultBuf.append("<td>&nbsp;</td>");
        }else
        {//要显示按钮，且标题与按钮的对齐方式不一致，则要分两个<td/>显示
            resultBuf.append("><TBODY><TR>");
            if(titlealign.equals("left"))
            {
                resultBuf.append(showRealTitlePartInTopBottom(realtabtitle,titlealign));
                resultBuf.append(showRealButtonPartInTopBottom(buttonsOnTitle,buttonalign.equals("center")?"align=\"left\" width=\"50%\""
                        :"align=\"right\" width=\"1%\""));
            }else if(titlealign.equals("center"))
            {
                if(buttonalign.equals("left"))
                {
                    resultBuf.append(showRealButtonPartInTopBottom(buttonsOnTitle,"align=\"left\" width=\"1%\""));
                    resultBuf.append(showRealTitlePartInTopBottom(realtabtitle,titlealign));
                }else
                {//right
                    resultBuf.append(showRealTitlePartInTopBottom(realtabtitle,titlealign));
                    resultBuf.append(showRealButtonPartInTopBottom(buttonsOnTitle,"width=\"1%\" align=\"right\""));
                }
            }else
            {
                resultBuf.append(showRealButtonPartInTopBottom(buttonsOnTitle,buttonalign.equals("center")?"align=\"right\" width=\"50%\""
                        :"align=\"left\" width=\"1%\""));
                resultBuf.append(showRealTitlePartInTopBottom(realtabtitle,titlealign));
            }
        }
        resultBuf.append("</TR></TBODY></TABLE>");
        resultBuf.append("</TD></TR>");        
        return resultBuf.toString();
    }

    private String showRealTitlePartInTopBottom(String realtabtitle,String titlealign)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<td align=\""+titlealign+"\" valign=\""+(containerConfigBean.isTitleInBottom()?"top":"bottom")+"\">");
        resultBuf.append("<TABLE cellSpacing=0 cellPadding=0");
        if(!this.tabspanelBean.isAsyn())
        {
            resultBuf.append(" selectedItemIndex=\"").append(currentSelectedTabItemDisplayBean.getIndex()).append("\"");
        }
        resultBuf.append("><TBODY><TR>");
        resultBuf.append(realtabtitle);
        resultBuf.append("</TR></TBODY></TABLE>");
        resultBuf.append("</td>");
        return resultBuf.toString();
    }

    private String showRealButtonPartInTopBottom(String buttonsOnTitle,String tdStyleproperty)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<td nowrap "+tdStyleproperty);
        resultBuf.append(" valign=\""+(containerConfigBean.isTitleInBottom()?"top":"bottom")+"\"");
        resultBuf.append(">").append(buttonsOnTitle).append("</td>");
        return resultBuf.toString();
    }
    
//    protected String showTopBottomTitlePart(boolean isDisplayTopTitleBar)
//        {//如果当前容器的标题是显示在左右两侧，即不在上下显示标题，则只要考虑显示在上下的按钮
//        }
//        {//如果当前是在显示容器顶部，但标题是显示在底部；或者当前是在显示容器底部，但标题是显示在容器顶部
//        {//标题栏授权为不显示，则只显示上面的功能按钮
//            return super.showTopBottomTitlePart(isDisplayTopTitleBar);
//        if(realtabtitle.trim().equals("")) return super.showTopBottomTitlePart(isDisplayTopTitleBar);//没有标题需要显示，则只考虑按钮的显示
//        String buttonsOnTitle=getContainerTopBottomButtonsDisplayValue(isDisplayTopTitleBar);//要显示在顶部或底部标题栏上的功能按钮
//        {
//        String titlealign=containerConfigBean.getTitlealign();
//        if(containerConfigBean.isTitleInBottom())
//        {//标题显示在底部
//        {//标题显示在顶部
//        if(!this.tabspanelBean.isAsyn())
//        {//如果不是异步切换tabitem
//            resultBuf.append(" selectedItemIndex=\"").append(currentSelectedTabItemDisplayBean.getIndex()).append("\"");//记录当前选中的标签页下标
//        {//没有按钮只显示标题部分
//            {
//                resultBuf.append("<td width='50%'>&nbsp;</td>");
//                resultBuf.append("<td width='50%'>&nbsp;</td>");
//                resultBuf.append("<td>&nbsp;</td>");
//            }else
//            {//left
//                resultBuf.append("<td>&nbsp;</td>");
//        {//需要显示标题和功能按钮
//            {
//                    resultBuf.append("<td width=\"50%\" nowrap align=\"left\">").append(buttonsOnTitle).append("</td>");
//                    resultBuf.append("<td width=\"50%\">&nbsp;</td>");
//                {
//                    resultBuf.append("<td width='50%'>&nbsp;</td>");
//                    resultBuf.append("<td width=\"2px\">&nbsp;</td>");
//                    resultBuf.append("<td align=\"left\" width=\"1%\" nowrap>"+buttonsOnTitle+"</td>");
//                    resultBuf.append("<td width='50%'>&nbsp;</td>");
//                {//right
//                    resultBuf.append("<td width='50%'>&nbsp;</td>");
//                    resultBuf.append("<td align=\"right\" nowrap>"+buttonsOnTitle+"</td>");
//            }else if(titlealign.equals("right"))
//                    resultBuf.append("<td align=\"left\" nowrap>"+buttonsOnTitle+"</td>");
//                }else if(buttonalign.equals("center"))
//                    resultBuf.append("<td width='50%'>&nbsp;</td>");
//                    resultBuf.append("<td align=\"left\" nowrap>"+buttonsOnTitle+"</td>");
//                {//right
//                    resultBuf.append("<td>&nbsp;</td>");
//                    resultBuf.append("<td width=\"2px\">&nbsp;</td>");
//                    resultBuf.append("<td align=\"left\" width=\"1%\" nowrap>"+buttonsOnTitle+"</td>");
//                }
//            {//left
//                if(buttonalign.equals("left")) resultBuf.append("<td width=\"2px\">&nbsp;</td>");
//                resultBuf.append("<td align=\""+buttonalign+"\" nowrap>"+buttonsOnTitle+"</td>");
//        resultBuf.append("</TR></TBODY></TABLE>");
//        resultBuf.append("</TD></TR>");
//        return resultBuf.toString();
    
    private String getTabsTitleInTopAndBottomDisplayValue()
    {
        StringBuffer resultBuf=new StringBuffer();
        String titlewidth=((TabsPanelBean)containerConfigBean).getTitlewidth();
        if(titlewidth==null||titlewidth.trim().equals("")) titlewidth=Config.getInstance().getSystemConfigValue("default-tabpanel-titlewidth","120px");
        String titlestyle=((TabsPanelBean)containerConfigBean).getTitlestyle();
        String img_sel=null;
        String img_sel_desel=null;
        String img_desel=null;
        String img_desel_sel=null;
        String img_desel_desel=null;
        if(titlestyle!=null&&titlestyle.trim().equals("2")&&containerConfigBean.isTitleInTop())
        {
            img_sel=Config.webroot+"webresources/skin/"+rrequest.getPageskin()+"/images/title2_selected.gif";
            img_sel_desel=Config.webroot+"webresources/skin/"+rrequest.getPageskin()+"/images/title2_selected_deselected.gif";
            img_desel=Config.webroot+"webresources/skin/"+rrequest.getPageskin()+"/images/title2_deselected.gif";
            img_desel_sel=Config.webroot+"webresources/skin/"+rrequest.getPageskin()+"/images/title2_deselected_selected.gif";
            img_desel_desel=Config.webroot+"webresources/skin/"+rrequest.getPageskin()+"/images/title2_deselected_deselected.gif";
            StringBuffer paramsBuf=new StringBuffer();
            paramsBuf.append("{tabpanelguid:\""+this.containerConfigBean.getGuid()+"\"");
            paramsBuf.append(",tabitemcount:"+lstDisplayedChildren.size()+"}");
            rrequest.getWResponse().addOnloadMethod("adjustTabItemTitleImgHeight",paramsBuf.toString(),true);
        }
        String titleTmp;
        TabItemDisplayBean tdbeanTmp;
        for(int i=0,len=lstDisplayedChildren.size();i<len;i++)
        {
            tdbeanTmp=lstDisplayedChildren.get(i);
            titleTmp=getTabItemTitle(tdbeanTmp.getChildid());
            resultBuf.append("<TD noWrap style=\"width:").append(titlewidth).append("\"");
            resultBuf.append(" id=\""+this.containerConfigBean.getGuid()+"_"+tdbeanTmp.getIndex()+"_title\"");
            if(titlestyle!=null&&titlestyle.trim().equals("2")&&containerConfigBean.isTitleInTop())
            {
                if(!this.tabspanelBean.isAsyn()&&len>1)
                {
                    if(i==0)
                    {
                        resultBuf.append("tabitem_position_type=\"first\"");
                    }else if(i==len-1)
                    {//如果是最后一个标签页
                        resultBuf.append("tabitem_position_type=\"last\"");
                    }else
                    {
                        resultBuf.append("tabitem_position_type=\"middle\"");
                    }
                }
                boolean isSelected=false;
                if(tdbeanTmp.getIndex()==currentSelectedTabItemDisplayBean.getIndex())
                {
                    isSelected=true;
                    if(!this.tabspanelBean.isAsyn()) resultBuf.append(" onclick=\"try{"+getTabItemClickEvent(tdbeanTmp.getIndex())+"}catch(e){logErrorsAsJsFileLoad(e);}\"");
                    resultBuf.append(" class='cls-tabtitle-top2-selected'>");
                }else
                {
                    if(tdbeanTmp.isDisabled())
                    {
                        resultBuf.append(" class='cls-tabtitle-top2-disabled'>");
                    }else
                    {
                        resultBuf.append(" class='cls-tabtitle-top2-deselected' onclick=\"try{"+getTabItemClickEvent(tdbeanTmp.getIndex())+"}catch(e){logErrorsAsJsFileLoad(e);}\">");
                    }
                }
                resultBuf.append(titleTmp+"</td>");
                resultBuf.append("<TD width=\"21px\"><IMG width=\"21px\"");
                resultBuf.append(" id=\""+this.containerConfigBean.getGuid()+"_"+tdbeanTmp.getIndex()+"_rightimg\"");
                resultBuf.append(" src=\"");
                if(i==len-1)
                {
                    if(isSelected)
                    {
                        resultBuf.append(img_sel);
                    }else
                    {
                        resultBuf.append(img_desel);
                    }
                }else
                {
                    if(isSelected)
                    {
                        resultBuf.append(img_sel_desel);
                    }else
                    {
                        if(lstDisplayedChildren.get(i+1).getIndex()==currentSelectedTabItemDisplayBean.getIndex())
                        {//如果当前tabitem的下一个tabitem就是本次选中显示的tabitem
                            resultBuf.append(img_desel_sel);
                        }else
                        {
                            resultBuf.append(img_desel_desel);
                        }
                    }
                }
                resultBuf.append("\"></td>");
            }else
            {
                if(containerConfigBean.isTitleInBottom())
                {
                    if(tdbeanTmp.getIndex()==currentSelectedTabItemDisplayBean.getIndex())
                    {
                        resultBuf.append(" class=\"cls-tabtitle-bottom-selected\"");
                        if(!this.tabspanelBean.isAsyn()) resultBuf.append(" onclick=\"try{"+getTabItemClickEvent(tdbeanTmp.getIndex())+"}catch(e){logErrorsAsJsFileLoad(e);}\"");
                    }else
                    {
                        if(tdbeanTmp.isDisabled())
                        {
                            resultBuf.append(" class='cls-tabtitle-bottom-disabled'");
                        }else
                        {
                            resultBuf.append(" class='cls-tabtitle-bottom-deselected' onclick=\"try{"+getTabItemClickEvent(tdbeanTmp.getIndex())+"}catch(e){logErrorsAsJsFileLoad(e);}\"");
                        }
                    }
                }else
                {
                    if(tdbeanTmp.getIndex()==currentSelectedTabItemDisplayBean.getIndex())
                    {
                        resultBuf.append(" class=\"cls-tabtitle-top-selected\"");
                        if(!this.tabspanelBean.isAsyn()) resultBuf.append(" onclick=\"try{"+getTabItemClickEvent(tdbeanTmp.getIndex())+"}catch(e){logErrorsAsJsFileLoad(e);}\"");
                    }else
                    {
                        if(tdbeanTmp.isDisabled())
                        {
                            resultBuf.append(" class='cls-tabtitle-top-disabled'");
                        }else
                        {
                            resultBuf.append(" class='cls-tabtitle-top-deselected' onclick=\"try{"+getTabItemClickEvent(tdbeanTmp.getIndex())+"}catch(e){logErrorsAsJsFileLoad(e);}\"");
                        }
                    }
                }
                resultBuf.append(">").append(titleTmp).append("</TD>");
                if(i!=len-1) resultBuf.append("<TD width=\"1px\">&nbsp;</TD>");
            }
        }
        String subtitle=this.getMChildren().get(currentSelectedTabItemDisplayBean.getChildid()).getConfigBean().getParentSubtitle(rrequest);
        if(subtitle!=null&&!subtitle.trim().equals(""))
        {//有副标题
            resultBuf.append("<td width=\"2px\">&nbsp;</td>");
            resultBuf.append("<td class='cls-subtitle' align='left' nowrap");
//            {//标题显示在底部
//                resultBuf.append(" valign='bottom'");
            resultBuf.append(">").append(subtitle.trim()).append("</td>");
        }
        return resultBuf.toString();
    }

    private String getTabItemClickEvent(int index)
    {
        StringBuffer resultBuf=new StringBuffer();
        if(this.tabspanelBean.isAsyn())
        {
            resultBuf.append("shiftTabPanelItemAsyn('");
            resultBuf.append(containerConfigBean.getPageBean().getId()).append("','");
            resultBuf.append(containerConfigBean.getId()).append("','");
            resultBuf.append(((TabsPanelBean)containerConfigBean).getRefreshGuid(index)).append("','");
            resultBuf.append(index).append("',");
            resultBuf
                    .append(this.tabspanelBean.getSwitchbeforecallback()==null||this.tabspanelBean.getSwitchbeforecallback().trim().equals("")?"null"
                            :this.tabspanelBean.getSwitchbeforecallback().trim());
            resultBuf.append(")");
        }else
        {
            resultBuf.append("shiftTabPanelItemSyn('");
            resultBuf.append(containerConfigBean.getPageBean().getId()).append("','");
            resultBuf.append(containerConfigBean.getId()).append("','");
            resultBuf.append(((TabsPanelBean)containerConfigBean).getRefreshGuid(index)).append("','");
            resultBuf.append(index).append("')");
        }
        return resultBuf.toString();
    }
    
    private String getTabItemTitle(String childid)
    {
        if(this.mChildren.containsKey(childid))
        {
            return this.mChildren.get(childid).getRealParenttitle();
        }
        IComponentConfigBean childBean=containerConfigBean.getMChildren().get(childid).getConfigBeanWithValidParentTitle();
        if(childBean==null) return "";
        String title=childBean.getParenttitle(rrequest);
        if(title==null||title.trim().equals(""))
        {
            title=childBean.getTitle(rrequest);
        }
        return title==null?"":title.trim();
    }

    public AbsContainerConfigBean loadConfig(XmlElementBean eleContainer,AbsContainerConfigBean parent,String tagname)
    {
        TabsPanelBean tabsConfigBean=(TabsPanelBean)super.loadConfig(eleContainer,parent,tagname);
        String asyn=eleContainer.attributeValue("asyn");
        String titlewidth=eleContainer.attributeValue("titlewidth");
        String titlestyle=eleContainer.attributeValue("titlestyle");
        String switchbeforecallback=eleContainer.attributeValue("switchbeforecallback");
        if(asyn!=null)
        {
            tabsConfigBean.setAsyn(!asyn.trim().toLowerCase().equals("false"));//因为默认是true，所以这里这样判断
        }
        if(titlewidth!=null) tabsConfigBean.setTitlewidth(titlewidth.trim());
        if(titlestyle!=null) tabsConfigBean.setTitlestyle(titlestyle.trim());
        String displaycount=eleContainer.attributeValue("displaycount");
        if(displaycount!=null&&!displaycount.trim().equals(""))
        {
            try
            {
                tabsConfigBean.setDisplaycount(Integer.parseInt(displaycount.trim()));
            }catch(NumberFormatException e)
            {
                log.warn("tab容器"+tabsConfigBean.getPath()+"的displaycount属性配置值不是合法数字",e);
            }
        }
        if(switchbeforecallback!=null) tabsConfigBean.setSwitchbeforecallback(switchbeforecallback.trim());
        return tabsConfigBean;
    }

    protected AbsContainerConfigBean createContainerConfigBean(AbsContainerConfigBean parentContainer,String tagname)
    {
        return new TabsPanelBean(parentContainer,tagname);
    }
    
    private class TabItemDisplayBean
    {
        private int index;

        private String childid;

        private boolean isDisabled;

        public int getIndex()
        {
            return index;
        }

        public void setIndex(int index)
        {
            this.index=index;
        }

        public String getChildid()
        {
            return childid;
        }

        public void setChildid(String childid)
        {
            this.childid=childid;
        }

        public boolean isDisabled()
        {
            return isDisabled;
        }

        public void setDisabled(boolean isDisabled)
        {
            this.isDisabled=isDisabled;
        }
    }
    
    protected String getComponentTypeName()
    {
        return "container.tabspanel";
    }
}
