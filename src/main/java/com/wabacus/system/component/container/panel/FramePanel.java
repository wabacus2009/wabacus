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

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.panel.FramePanelBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.util.Consts;

public class FramePanel extends AbsPanelType
{
    public FramePanel(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(!rrequest.checkPermission(this.containerConfigBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            wresponse.println("&nbsp;");
            return;
        }
        StringBuffer tempBuf;
        String containerwidth=null;
        if(this.getParentContainerType()!=null)
        {
            containerwidth=this.getParentContainerType().getChildDisplayWidth(containerConfigBean);
            if(containerwidth==null||containerwidth.trim().equals("")) containerwidth="100%";
            if(containerConfigBean.getTop()!=null&&!containerConfigBean.getTop().trim().equals(""))
            {
                wresponse.println("<table  cellspacing=\"0\" cellpadding=\"0\" width=\""+containerwidth+"\" style=\"MARGIN:0;\">");
                wresponse.println("<tr><td height=\""+containerConfigBean.getTop()+"\"></td></tr></table>");
            }
            wresponse.println(getRealHeaderFooterDisplayValue(containerConfigBean.getOuterHeaderTplBean(),"outerheader"));
            wresponse.println("<table  cellspacing=\"0\" cellpadding=\"0\" width=\""+containerwidth+"\" id=\""+containerConfigBean.getGuid()+"\"><tr><td>");
        }
        wresponse.println("<div id=\"WX_CONTENT_"+containerConfigBean.getGuid()+"\">");
        wresponse.println(this.showHeader());//显示Header部分
        showButtonsOnTopBottomTitle(true);
        tempBuf=new StringBuffer();
        tempBuf.append("<FIELDSET class=\"cls-"+containerConfigBean.getTagname()+"-content\"");
        if(containerConfigBean.shouldShowContextMenu())
        {
            tempBuf.append(" oncontextmenu=\"try{showcontextmenu('contextmenu_"+containerConfigBean.getGuid()
                    +"',event);}catch(e){logErrorsAsJsFileLoad(e);}\"");
        }
        if(containerConfigBean.getBorder()!=0)
        {
            tempBuf.append(" style=\"border:solid "+containerConfigBean.getBorder()+"px ");
            if(containerConfigBean.getBordercolor()!=null&&!containerConfigBean.getBordercolor().trim().equals(""))
            {
                tempBuf.append(containerConfigBean.getBordercolor());
            }
        }else
        {
            tempBuf.append(" style=\"border:0");
        }
        tempBuf.append(";\">");
        wresponse.println(tempBuf.toString());
        wresponse.println("<LEGEND class=\"cls-title\">"
                +(rrequest.checkPermission(containerConfigBean.getId(),Consts.TITLE_PART,null,Consts.PERMISSION_TYPE_DISPLAY)?this.containerConfigBean.getTitle(rrequest):"")
                +"</LEGEND>");
        
        if(rrequest.checkPermission(this.containerConfigBean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            wresponse.println("<table cellspacing='0' cellpadding='0' width=\"100%\">");
            wresponse.println(showContainerScrollStartTag());
            if(containerConfigBean.getMargin_top()!=null&&!containerConfigBean.getMargin_top().trim().equals(""))
            {
                wresponse.println("<tr><td colspan=\""+containerConfigBean.getColspan_total()+"\" height=\""+
                        containerConfigBean.getMargin_top()+"\"></td></tr>");
            }
            wresponse.println("<tr>");
            if(containerConfigBean.getMargin_left()!=null&&!containerConfigBean.getMargin_left().trim().equals(""))
            {//如果有左边间隔
                wresponse.println("<td width=\""+containerConfigBean.getMargin_left()+"\"><span style=\"margin-left:"+containerConfigBean.getMargin_left()+"\"></span></td>");
            }
            wresponse.println("<td>");
            tempBuf=new StringBuffer();
            tempBuf.append("<table cellspacing='0' cellpadding='0' width=\"100%\"");
            if(!this.containerConfigBean.isScrollY()&&containerConfigBean.getHeight()!=null&&!containerConfigBean.getHeight().trim().equals(""))
            {//容器的高度配置必须放在最里层的<table/>中，否则没办法通过它的<td/>的valign控制子组件的垂直对齐方式
                tempBuf.append(" height=\""+containerConfigBean.getHeight()+"\" ");
            }
            tempBuf.append(">");
            wresponse.println(tempBuf.toString());
            IComponentType childObjTmp=this.mChildren.entrySet().iterator().next().getValue();
            wresponse.println("<tr>");
            showChildObj(childObjTmp,null);
            wresponse.println("</tr>");
            wresponse.println("</table></td>");
            if(containerConfigBean.getMargin_right()!=null&&!containerConfigBean.getMargin_right().trim().equals(""))
            {
                wresponse.println("<td width=\""+containerConfigBean.getMargin_right()+"\"><span style=\"margin-left:"+containerConfigBean.getMargin_right()+"\"></span></td>");
            }
            wresponse.println("</tr>");
            if(containerConfigBean.getMargin_bottom()!=null&&!containerConfigBean.getMargin_bottom().trim().equals(""))
            {
                wresponse.println("<tr><td colspan=\""+containerConfigBean.getColspan_total()+"\" height=\""+
                        containerConfigBean.getMargin_bottom()+"\"></td></tr>");
            }
            wresponse.println(showContainerScrollEndTag());
            wresponse.println("</table>");
        }
        wresponse.println("</FIELDSET>");
        showButtonsOnTopBottomTitle(false);
        wresponse.println(this.showFooter());
//        wresponse.println(this.showContextMenu());
        wresponse.println(this.showMetaData());
        wresponse.println("</div>");
        if(this.getParentContainerType()!=null)
        {
            wresponse.println("</td></tr></table>");
            wresponse.println(getRealHeaderFooterDisplayValue(containerConfigBean.getOuterFooterTplBean(),"outerfooter"));
            if(containerConfigBean.getBottom()!=null&&!containerConfigBean.getBottom().trim().equals(""))
            {
                wresponse.println("<table  cellspacing=\"0\" cellpadding=\"0\" width=\""+containerwidth+"\" style=\"MARGIN:0;\">");
                wresponse.println("<tr><td height=\""+containerConfigBean.getBottom()+"\"></td></tr></table>");
            }
        }
    }

    private void showButtonsOnTopBottomTitle(boolean isDisplayTopTitleBar)
    {
        String buttonsOnTitle=getContainerTopBottomButtonsDisplayValue(isDisplayTopTitleBar);
        if(buttonsOnTitle.trim().equals("")) return;
        String buttonalign=this.containerConfigBean.getButtonsBean().getAlign();
        buttonalign=buttonalign==null||buttonalign.equals("")?"right":buttonalign.trim();
        wresponse.println("<table  cellspacing=\"0\" cellpadding=\"0\" width=\"100%\"><tr>");
        wresponse.println("<td align=\""+buttonalign+"\">");
        wresponse.println(buttonsOnTitle);
        wresponse.println("</td></tr></table>");
    }

    
    
    public AbsContainerConfigBean loadConfig(XmlElementBean eleContainer,AbsContainerConfigBean parent,String tagname)
    {
        FramePanelBean fpanelbean=(FramePanelBean)super.loadConfig(eleContainer,parent,tagname);
        if(fpanelbean.getLstChildrenIDs().size()>1)
        {
            List<String> lstChildrenIds=new ArrayList<String>();
            lstChildrenIds.add(fpanelbean.getLstChildrenIDs().get(0));//只保留配置的第一个子元素
            for(int i=1;i<fpanelbean.getLstChildrenIDs().size();i++)
            {
                fpanelbean.getMChildren().remove(fpanelbean.getLstChildrenIDs().get(i));
            }
            fpanelbean.setLstChildrenIDs(lstChildrenIds);
        }
        return fpanelbean;
    }

    protected AbsContainerConfigBean createContainerConfigBean(AbsContainerConfigBean parentContainer,String tagname)
    {
        return new FramePanelBean(parentContainer,tagname);
    }
    
    protected String getComponentTypeName()
    {
        return "container.framepanel";
    }
}

