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
package com.wabacus.system.tags.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.util.Consts;

public class ButtonTag extends AbsComponentTag
{
    private static final long serialVersionUID=-8123624007139208727L;

    private final static Log log=LogFactory.getLog(ButtonTag.class);

    private String type;

    private String name;//<button/>的name属性
    
    private String savebinding;
    
    private String deletebinding;

    private String pageurl;
    
    private String beforecallback;//当type为forwardwithback，即显示带返回功能的跳转按钮时，用于指定跳转到目标页面时执行的JS回调函数名
    
    private String dataexportcomponentids;
    
    private String localstroage;
    
    private String download;
    
    private String autodelete;
    
    private String zip;
    
    private String directorydateformat;
    
    public void setType(String type)
    {
        this.type=type;
    }

    public void setName(String name)
    {
        this.name=name;
    }

    public void setSavebinding(String savebinding)
    {
        this.savebinding=savebinding;
    }

    public void setDeletebinding(String deletebinding)
    {
        this.deletebinding=deletebinding;
    }

    public void setPageurl(String pageurl)
    {
        this.pageurl=pageurl;
    }

    public void setBeforecallback(String beforecallback)
    {
        this.beforecallback=beforecallback;
    }

    public void setLocalstroage(String localstroage)
    {
        this.localstroage=localstroage;
    }

    public void setDownload(String download)
    {
        this.download=download;
    }

    public void setAutodelete(String autodelete)
    {
        this.autodelete=autodelete;
    }

    public void setZip(String zip)
    {
        this.zip=zip;
    }

    public void setDirectorydateformat(String directorydateformat)
    {
        this.directorydateformat=directorydateformat;
    }

    public int doStartTag() throws JspException
    {
        if(Consts.lstDataExportTypes.contains(type))
        {
            this.dataexportcomponentids=this.componentid;
            this.componentid=null;
        }
        return super.doStartTag();
    }

    public int doMyStartTag() throws JspException,IOException
    {
        type=type==null?"":type.trim().toLowerCase();
        name=name==null?"":name.trim();
        savebinding=savebinding==null?"":savebinding.trim();
        deletebinding=deletebinding==null?"":deletebinding.trim();
        if(!name.equals("")&&!type.equals(""))
        {
            log.warn("在<wx:button/>中，同时指定name属性和type属性时，只有name属性有效");
        }else if(name.equals("")&&type.equals(""))
        {
            throw new JspException("当不是循环显示所有按钮时，必须通过type属性或name属性指定要显示的按钮");
        }
        return EVAL_BODY_BUFFERED;
    }

    public int doMyEndTag() throws JspException,IOException
    {
        BodyContent bc=getBodyContent();
        String button=null;
        if(bc!=null) button=bc.getString();
        button=button==null?"":button.trim();
        Map<String,String> attributes=new HashMap<String,String>();
        attributes.put("type",type);
        attributes.put("name",name);
        attributes.put("savebinding",savebinding);
        attributes.put("deletebinding",deletebinding);
        attributes.put("label",button);
        attributes.put("componentids",this.dataexportcomponentids);
        attributes.put("pageurl",pageurl);
        attributes.put("beforecallback",beforecallback);
        attributes.put("directorydateformat",this.directorydateformat);
        attributes.put("download",this.download);
        attributes.put("autodelete",this.autodelete);
        attributes.put("localstroage",this.localstroage);
        attributes.put("zip",this.zip);
        if(Consts.lstDataExportTypes.contains(type))
        {//如果是导出数据的按钮，则不用初始化displayReportObj，因为reportid可以传入多个报表的id，即使某个报表当前没显示，也可以提供导出它数据的链接，所以跟displayReportObj无关
            println(TagAssistant.getInstance().getButtonDisplayValue(this.ownerComponentObj,attributes));
        }else
        {
            if(this.displayComponentObj==null) return EVAL_PAGE;
            println(TagAssistant.getInstance().getButtonDisplayValue(this.displayComponentObj,attributes));
        }
        return EVAL_PAGE;
    }
}
