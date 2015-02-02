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
package com.wabacus.system.inputbox;

import java.util.ArrayList;
import java.util.HashMap;

import com.wabacus.config.Config;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Tools;

public class PopUpBox extends AbsPopUpBox
{
    private String sourcebox;
    
    public PopUpBox(String typename)
    {
        super(typename);
    }
    
    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(this.getBeforedescription(rrequest));
        String inputboxid=getInputBoxId(rrequest);
        if("textareabox".equals(this.sourcebox))
        {
            resultBuf.append("<textarea");
        }else
        {
            resultBuf.append("<input  type='text'");
            resultBuf.append(" value=\""+getInputBoxValue(rrequest,value)+"\"");
        }
        resultBuf.append(" name='"+inputboxid+"' id='"+inputboxid+"' ");
        if(isReadonly)
        {
            style_property=Tools.replaceAll(style_property,"popupPageByPopupInputbox(this)","");
        }
        if(style_property!=null) resultBuf.append(" ").append(style_property);
        resultBuf.append(">");
        if("textareabox".equals(this.sourcebox))
        {
            resultBuf.append(getInputBoxValue(rrequest,value)).append("</textarea>");
        }
        resultBuf.append(this.getAfterdescription(rrequest));
        return resultBuf.toString();
    }

    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        if("textareabox".equals(this.sourcebox)) resultBuf.append(" sourcebox=\"textareabox\"");
        return resultBuf.toString();
    }

    public String filledInContainer()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("if(boxMetadataObj.getAttribute('sourcebox')=='textareabox'){boxstr=\"<textarea\";}else{boxstr=\"<input type='text' value=\\\"\"+boxValue+\"\\\"\";}");
        resultBuf.append(getInputBoxCommonFilledProperties());
        resultBuf.append("boxstr=boxstr+\">\";");
        resultBuf.append("if(boxMetadataObj.getAttribute('sourcebox')=='textareabox'){boxstr=boxstr+boxValue+\"</textarea>\";}");
        resultBuf.append("setColDisplayValueToEditable2Td(parentTdObj,boxstr);");
        return resultBuf.toString();
    }

    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        poppageurl=eleInputboxBean.getContent();
        if(poppageurl==null||poppageurl.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("报表"+owner.getReportBean().getPath()+"配置的弹出输入框没有配置poppageurl属性");
        }
        poppageurl=poppageurl.trim();
        if(!poppageurl.startsWith(Config.webroot))
        {
            poppageurl=Config.webroot+poppageurl;
        }
        poppageurl=Tools.replaceAll(poppageurl,"//","/");
        this.sourcebox=eleInputboxBean.attributeValue("sourcebox");
        super.loadInputBoxConfig(eleInputboxBean);//这个必须放在最后，因为加载了上面的信息后，再在父类中进行一下加载后处理
        owner.getReportBean().addPopUpBox(this);
    }

    protected void parsePopupPageUrl()
    {
        if(this.poppageurl==null||this.poppageurl.trim().equals("")) return;
        int idx=this.poppageurl.indexOf('?');
        if(idx>0)
        {
            String urlparams=this.poppageurl.substring(idx+1);
            this.poppageurl=this.poppageurl.substring(0,idx);
            urlparams=Tools.isEmpty(urlparams)?null:parseDynParamsInUrl(urlparams);
            if(!Tools.isEmpty(urlparams)) this.poppageurl+="?"+urlparams;
        }
    }
    
    protected String getDefaultWidth()
    {
        return "500";
    }
    
    protected String getDefaultHeight()
    {
        return "300";
    }
    
    protected void processStylePropertyAfterMerged()
    {
        super.processStylePropertyAfterMerged();
        this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"onclick=\"popupPageByPopupInputbox(this)\"",1);
    }
    
    public Object clone(IInputBoxOwnerBean owner)
    {
        PopUpBox popbNew=(PopUpBox)super.clone(owner);
        if(owner!=null&&owner.getReportBean()!=null)
        {
            owner.getReportBean().addPopUpBox(popbNew);
        }
        return popbNew;
    }
}
