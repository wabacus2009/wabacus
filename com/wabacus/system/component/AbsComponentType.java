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
package com.wabacus.system.component;

import java.util.List;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.WabacusResponse;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public abstract class AbsComponentType implements IComponentType
{
    protected ReportRequest rrequest;

    protected WabacusResponse wresponse;
    
    protected IComponentConfigBean comCfgBean;

    protected AbsContainerType parentContainerType;
    
    public AbsComponentType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        this.parentContainerType=parentContainerType;
        this.comCfgBean=comCfgBean;
        this.rrequest=rrequest;
        if(rrequest!=null) wresponse=rrequest.getWResponse();
    }
    
    public ReportRequest getReportRequest()
    {
        return rrequest;
    }
    
    public AbsContainerType getParentContainerType()
    {
        return parentContainerType;
    }

    public void setParentContainerType(AbsContainerType parentContainerType)
    {
        this.parentContainerType=parentContainerType;
    }
    
    public IComponentConfigBean getConfigBean()
    {
        return this.comCfgBean;
    }
    
    protected boolean shouldDisplayMe()
    {
        if(!rrequest.checkPermission(this.comCfgBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY)) return false;
        AbsContainerConfigBean parentConfigBean=this.comCfgBean.getParentContainer();
        while(parentConfigBean!=null)
        {
            if(!rrequest.checkPermission(parentConfigBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY)) return false;
            if(!rrequest.checkPermission(parentConfigBean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return false;
            parentConfigBean=parentConfigBean.getParentContainer();
        }
        return true;
    }
    
    protected String getTitleDisplayValue(String realtitle,String buttonsOnTitle)
    {
        realtitle=realtitle==null?"":realtitle.trim();
        buttonsOnTitle=buttonsOnTitle==null?"":buttonsOnTitle.trim();
        if(realtitle.equals("")&&buttonsOnTitle.equals("")) return "";
        String titlealign=null;//标题对齐方式
        if(!realtitle.trim().equals(""))
        {
            titlealign=comCfgBean.getTitlealign();
            titlealign=titlealign==null||titlealign.trim().equals("")?"left":titlealign.toLowerCase().trim();
        }
        String buttonalign=null;
        if(!buttonsOnTitle.trim().equals(""))
        {
            buttonsOnTitle="<span style=\"vertical-align:bottom;\">"+buttonsOnTitle+"</span>";
            buttonalign=comCfgBean.getButtonsBean().getAlign();
            buttonalign=buttonalign==null||buttonalign.trim().equals("")?"right":buttonalign.toLowerCase().trim();
        }
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<table class='cls-title-table' cellpadding='0' cellspacing='0' width='100%'>");
        resultBuf.append("<tr class='cls-title-tr'>");
        if(titlealign==null||buttonalign==null||titlealign.equals(buttonalign))
        {
            if(buttonalign==null)
            {
                resultBuf.append("<td align='"+titlealign+"'>");
                resultBuf.append(realtitle);
            }else if(titlealign==null)
            {
                resultBuf.append("<td align='"+buttonalign+"'>");
                resultBuf.append(buttonsOnTitle);
            }else
            {//本次同时显示两者，但对齐方式一致
                resultBuf.append("<td align='"+titlealign+"'>");
                if("left".equals(this.comCfgBean.getButtonsBean().getTitleposition()))
                {
                    resultBuf.append(buttonsOnTitle);
                    resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(this.comCfgBean.getButtonsBean().getButtonspacing()));
                }
                resultBuf.append(realtitle);
                if(!"left".equals(this.comCfgBean.getButtonsBean().getTitleposition()))
                {
                    resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(this.comCfgBean.getButtonsBean().getButtonspacing()));
                    resultBuf.append(buttonsOnTitle);
                }
            }
            resultBuf.append("</td>");
        }else
        {
            if(titlealign.equals("left"))
            {
                resultBuf.append("<td align='left' width='1%' nowrap>").append(realtitle).append("</td>");
                resultBuf.append("<td align='"+buttonalign+"' nowrap>").append(buttonsOnTitle).append("</td>");
            }else if(titlealign.equals("right"))
            {
                resultBuf.append("<td align='"+buttonalign+"' nowrap>").append(buttonsOnTitle).append("</td>");
                resultBuf.append("<td align='left' width='1%' nowrap>").append(realtitle).append("</td>");
            }else
            {
                if(buttonalign.equals("left")) resultBuf.append("<td align='left' width='1%' nowrap>").append(buttonsOnTitle).append("</td>");
                resultBuf.append("<td align='"+titlealign+"' nowrap>").append(realtitle).append("</td>");
                if(buttonalign.equals("right")) resultBuf.append("<td align='right' width='1%' nowrap>").append(buttonsOnTitle).append("</td>");
            }
        }
        resultBuf.append("</tr></table>");
        return resultBuf.toString();
    }
    
    protected String getDisplayRealTitleAndSubTitle()
    {
        StringBuffer resultBuf=new StringBuffer();
        if(rrequest.checkPermission(comCfgBean.getId(),Consts.TITLE_PART,"title",Consts.PERMISSION_TYPE_DISPLAY))
        {
            String title=comCfgBean.getTitle(rrequest);
            if(title!=null&&!title.trim().equals(""))
            {//需要显示title
                //resultBuf.append("<span class=\"cls-title\">").append(Tools.htmlEncode(title.trim())).append("</span>");
                resultBuf.append("<span class=\"cls-title\">").append(title.trim()).append("</span>");
            }
        }
        if(rrequest.checkPermission(comCfgBean.getId(),Consts.TITLE_PART,"subtitle",Consts.PERMISSION_TYPE_DISPLAY))
        {
            String subtitle=comCfgBean.getSubtitle(rrequest);
            if(subtitle!=null&&!subtitle.trim().equals(""))
            {
                if(resultBuf.length()>0)
                {
                    resultBuf.append("<span  style=\"margin-left:2px;\">&nbsp;</span>");
                }
                //resultBuf.append("<span class='cls-subtitle'>").append(Tools.htmlEncode(subtitle.trim())).append("</span>");
                resultBuf.append("<span class='cls-subtitle'>").append(subtitle.trim()).append("</span>");
            }
        }
        return resultBuf.toString();
    }
    
    public String showHeader()
    {
        if(!rrequest.checkPermission(this.comCfgBean.getId(),Consts.HEADER_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        StringBuffer resultBuf=new StringBuffer();
        if(this.comCfgBean.getHeaderTplBean()==null) return "";
        resultBuf.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\"");
        resultBuf.append("><tr><td>");
        String header=this.getRealHeaderFooterDisplayValue(this.comCfgBean.getHeaderTplBean(),"header");
        if(header==null||header.trim().equals("")) return "";
        resultBuf.append(header);
        resultBuf.append("</td></tr></table>");
        return resultBuf.toString();
    }
    
    public String showFooter()
    {
        if(!rrequest.checkPermission(this.comCfgBean.getId(),Consts.FOOTER_PART,null,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        StringBuffer resultBuf=new StringBuffer();
        if(this.comCfgBean.getFooterTplBean()==null) return "";
        resultBuf.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">");
        resultBuf.append("<tr><td>");
        String footer=getRealHeaderFooterDisplayValue(this.comCfgBean.getFooterTplBean(),"footer");
        if(footer==null||footer.trim().equals("")) return "";
        resultBuf.append(footer);
        resultBuf.append("</td></tr></table>");
        return resultBuf.toString();
    }
    
    public String getRealHeaderFooterDisplayValue(TemplateBean headerFooterTpl,String type)
    {
        if(headerFooterTpl==null) return "";
        if(headerFooterTpl.getLstTagChildren()!=null&&headerFooterTpl.getLstTagChildren().size()>0)
        {
            return headerFooterTpl.getDisplayValue(rrequest,this);
        }
        String tplcontent=headerFooterTpl.getContent();
        if(tplcontent==null||tplcontent.trim().indexOf("i18n")!=0) return tplcontent;
        if(Tools.isDefineKey("i18n",tplcontent.trim()))
        {
            Object obj=rrequest.getI18NObjectValue(tplcontent.trim());
            if(obj==null) return "";
            if(!(obj instanceof TemplateBean)) return obj.toString();
            if("footer".equals(type))
            {//当前是在显示footer
                ComponentAssistant.getInstance().validComponentFooterTpl(this.comCfgBean,(TemplateBean)obj);
            }else if("header".equals(type))
            {
                ComponentAssistant.getInstance().validComponentHeaderTpl(this.comCfgBean,(TemplateBean)obj);
            }
            return ((TemplateBean)obj).getDisplayValue(rrequest,this);
        }
        return tplcontent;
    }
    
    protected String showContextMenu()
    {
        if(!this.comCfgBean.shouldShowContextMenu()) return "";
        List<AbsButtonType> lstMenuButtons=this.comCfgBean.getButtonsBean().getButtonsByPosition(Consts.CONTEXTMENU_PART);
        if(lstMenuButtons==null||lstMenuButtons.size()==0) return "";
        String prevgroup=null;
        String menustr;
        StringBuffer menuBuffer=new StringBuffer();
        for(AbsButtonType menuButton:lstMenuButtons)
        {
            if(menuButton==null) continue;
            menustr=ComponentAssistant.getInstance().showButtonMenu(this.comCfgBean,menuButton,rrequest,null);
            if(menustr==null||menustr.trim().equals("")) continue;
            if(prevgroup!=null)
            {
                if(!prevgroup.equals(menuButton.getMenugroup()))
                {
                    menuBuffer.append("<hr/>");
                    prevgroup=menuButton.getMenugroup();
                }
            }else
            {
                prevgroup=menuButton.getMenugroup();
            }
            menuBuffer.append(menustr);
        }
        menustr=menuBuffer.toString();
        if(menustr.endsWith("<hr/>"))
        {
            menustr=menustr.substring(0,menustr.length()-5);
        }
        menuBuffer=new StringBuffer();
        menuBuffer.append("<DIV class=\"contextmenu\" id=\"contextmenu_"+this.comCfgBean.getGuid()+"\"");
        if(menustr.trim().equals(""))
        {
            menuBuffer.append(" isEmpty=\"true\"");
        }else
        {
            menuBuffer.append(" onmouseover=\"try{highlightmenuitem(event);}catch(e){}\"  onmouseout=\"try{lowlightmenuitem(event);}catch(e){}\"");
        }
        menuBuffer.append(">"+menustr+"</DIV>");
        return menuBuffer.toString();
    }
    
    protected String showMetaData()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(rrequest.getStringAttribute(this.comCfgBean.getGuid()+"_showMetaData","").equals("true")) return "";
        rrequest.setAttribute(this.comCfgBean.getGuid()+"_showMetaData","true");
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(showMetaDataDisplayStringStart()).append(">");
        resultBuf.append(showMetaDataContentDisplayString());
        resultBuf.append("</span>");
        resultBuf.append(showContextMenu());//显示右键菜单
        return resultBuf.toString();
    }
    
    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<span id=\"").append(this.comCfgBean.getGuid()).append("_metadata\" style=\"display:none;\"");
        resultBuf.append(" pageid=\"").append(this.comCfgBean.getPageBean().getId()).append("\"");
        resultBuf.append(" componentid=\"").append(this.comCfgBean.getId()).append("\"");
        resultBuf.append(" refreshComponentGuid=\"").append(this.comCfgBean.getRefreshGuid()).append("\"");
        resultBuf.append(" componentTypeName=\"").append(getComponentTypeName()).append("\"");
        return resultBuf.toString();
    }

    protected abstract String getComponentTypeName();
    
    protected String showMetaDataContentDisplayString()
    {
        return "";
    }
    
    public static IComponentType createComponentTypeObj(Class componentClass,AbsContainerType parentContainer,IComponentConfigBean ccbean,
            ReportRequest rrequest)
    {
        if(componentClass==null) return null;
        try
        {
            return (IComponentType)componentClass.getConstructor(
                    new Class[] { AbsContainerType.class, IComponentConfigBean.class, ReportRequest.class }).newInstance(
                    new Object[] { parentContainer, ccbean, rrequest });
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("创建组件"+componentClass.getName()+"对象失败",e);
        }
    }
}

