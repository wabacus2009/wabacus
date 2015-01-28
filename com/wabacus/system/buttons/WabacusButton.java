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
package com.wabacus.system.buttons;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class WabacusButton extends AbsButtonType
{
    private final static Log log=LogFactory.getLog(WabacusButton.class);

    public WabacusButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }
    
    public String showButton(ReportRequest rrequest,String dynclickevent)
    {
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return "";
        if(!checkDisplayPermission(rrequest)) return "";
        String clickevent="";
        StringBuffer resultBuf=new StringBuffer();
        boolean isDisabled=checkDisabledPermission(rrequest);
        if(!isDisabled)
        {
            if(dynclickevent!=null&&!dynclickevent.trim().equals(""))
            {
                clickevent=dynclickevent.trim();
            }else
            {
                clickevent=getClickEvent(rrequest);
            }
            if(clickevent==null||clickevent.trim().equals(""))
            {
                if(clickevent==null) clickevent="";
                log.warn("为组件"+this.ccbean.getPath()+"配置的按钮："+this.name+"没有配置点击事件");
            }
        }
        String labeltemp=label;
        if(Tools.isDefineKey("$",labeltemp))
        {
            labeltemp=Config.getInstance().getResourceString(rrequest,ccbean.getPageBean(),labeltemp,false);
        }
        labeltemp=rrequest.getI18NStringValue(labeltemp);
        String mystyleproperty=this.getStyleproperty(!isDisabled);//本按钮的显示样式
        if(Tools.isDefineKey("image",labeltemp)||Tools.isDefineKey("button",labeltemp))
        {
            if(!isDisabled)
            {
                resultBuf.append("<span  onMouseOver=\"this.style.cursor='pointer'\" ");
                resultBuf.append(" onclick=\""+getRealClickEvent(rrequest,clickevent)+"\">");
            }
            if(Tools.isDefineKey("image",labeltemp))
            {
                labeltemp=Tools.getRealKeyByDefine("image",labeltemp);
                if(!labeltemp.startsWith(Config.webroot)&&!labeltemp.toLowerCase().startsWith("http://"))
                {
                    labeltemp=Config.webroot+"/"+labeltemp;
                    labeltemp=Tools.replaceAll(labeltemp,"//","/");
                }
                resultBuf.append("<img src=\"").append(labeltemp).append("\" ").append(mystyleproperty);
                resultBuf.append(" align=\"absMiddle\" border=\"0\">");
            }else
            {
                resultBuf.append(Tools.getRealKeyByDefine("button",labeltemp));
            }
            if(!isDisabled)
            {
                resultBuf.append("</span>");
            }
        }else
        {
            resultBuf.append("<input type=\"button\" value=\""+labeltemp+"\" ");
            resultBuf.append(mystyleproperty);
            resultBuf.append(" onclick=\""+getRealClickEvent(rrequest,clickevent)+"\"").append(">");
        }
        return resultBuf.toString();
    }

    public String showButton(ReportRequest rrequest,String dynclickevent,String button)
    {
        //if(shouldStopDisplayAsRefered(rrequest)) return "";
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return "";
        if(button==null||button.trim().equals("")) return showButton(rrequest,dynclickevent);
        if(!checkDisplayPermission(rrequest)) return "";
        StringBuffer resultBuf=new StringBuffer();
        boolean isDisabled=checkDisabledPermission(rrequest);
        if(!isDisabled)
        {
            String clickevent=null;
            if(dynclickevent!=null&&!dynclickevent.trim().equals(""))
            {
                clickevent=dynclickevent.trim();
            }else
            {
                clickevent=getClickEvent(rrequest);
            }
            if(clickevent==null||clickevent.trim().equals(""))
            {
                if(clickevent==null) clickevent="";
                log.warn("为组件"+this.ccbean+"配置的按钮："+this.name+"没有配置点击事件");
            }
            resultBuf.append("<span  onMouseOver=\"this.style.cursor='pointer'\" ");
            resultBuf.append(" onclick=\""+getRealClickEvent(rrequest,clickevent)+"\">");
        }
        resultBuf.append(button);
        if(!isDisabled)
        {
            resultBuf.append("</span>");
        }
        return resultBuf.toString();
    }
    
    public String showMenu(ReportRequest rrequest,String dynclickevent)
    {
        if(menulabel==null||menulabel.trim().equals("")) return "";
        if(!checkDisplayPermission(rrequest)) return "";
        String clickevent="";
        String menuItemCls="contextmenuitems_disabled";//菜单项样式
        if(!checkDisabledPermission(rrequest))
        {
            if(dynclickevent!=null&&!dynclickevent.trim().equals(""))
            {
                clickevent=dynclickevent.trim();
            }else
            {
                clickevent=getClickEvent(rrequest);
            }
            if(clickevent==null||clickevent.trim().equals(""))
            {
                if(clickevent==null) clickevent="";
                log.warn("为报表："+this.ccbean.getPath()+"配置的按钮："+this.name+"没有配置点击事件");
            }
            menuItemCls="contextmenuitems_enabled";
        }
        StringBuffer menuBuffer=new StringBuffer();
        menuBuffer.append("<DIV class=\"").append(menuItemCls).append("\" onclick=\"").append(getRealClickEvent(rrequest,clickevent)).append("\">");
        menuBuffer.append(rrequest.getI18NStringValue(this.menulabel));
        menuBuffer.append("</DIV>");
        return menuBuffer.toString();
    }
    
    private String getRealClickEvent(ReportRequest rrequest,String clickevent)
    {
        if(clickevent==null||clickevent.trim().equals("")) return "";
        if(this.confirmessage!=null&&!this.confirmessage.trim().equals(""))
        {
            String realConfirmmess=rrequest.getI18NStringValue(this.confirmessage);
            if(realConfirmmess!=null&&!realConfirmmess.trim().equals(""))
            {
                if(!clickevent.trim().endsWith(";")) clickevent=clickevent+";";
                String realconfirmTitle=rrequest.getI18NStringValue(this.confirmtitle);
                realconfirmTitle=realconfirmTitle==null?"":realconfirmTitle.trim();
                return "wx_confirm('"+realConfirmmess+"','"+realconfirmTitle+"',null,null,function(input){if(wx_isOkConfirm(input)){try{"+clickevent
                        +"}catch(e){logErrorsAsJsFileLoad(e);}}},"+this.cancelmethod+");";
            }
        }
        return "try{"+clickevent+"}catch(e){logErrorsAsJsFileLoad(e);}";
    }
    
//        {//当前按钮被容器引用显示，且不在原位置显示
//            if(rrequest.getStringAttribute("DISPLAY_ON_SOURCEREPORT","").equals("false"))
//            {//不是在源报表中显示此按钮
//                rrequest.removeAttribute("DISPLAY_ON_SOURCEREPORT");//及时清除掉
//                return false;//如果当前不是在源报表上显示，则显示出来
//            return false;
    
    public void loadExtendConfig(XmlElementBean eleButtonBean)
    {
    }
}
