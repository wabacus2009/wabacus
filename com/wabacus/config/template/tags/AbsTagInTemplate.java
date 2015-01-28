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
package com.wabacus.config.template.tags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.template.TemplateParser;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.util.Consts_Private;

public abstract class AbsTagInTemplate
{
    protected String tagContent="";

    protected int startposition;

    protected int endposition;

    protected Map<String,String> mTagAttributes;

    protected AbsTagInTemplate parentTag;//当前标签对应的父标签，如果当前标签是顶层标签，父标签为空

    protected List<AbsTagInTemplate> lstTagChildren;

    public AbsTagInTemplate(AbsTagInTemplate parentTag)
    {
        this.parentTag=parentTag;
        mTagAttributes=new HashMap<String,String>();
    }

    public String getTagContent()
    {
        return tagContent;
    }

    public void setTagContent(String tagContent)
    {
        this.tagContent=tagContent;
    }

    public int getStartposition()
    {
        return startposition;
    }

    public void setStartposition(int startposition)
    {
        this.startposition=startposition;
    }

    public int getEndposition()
    {
        return endposition;
    }

    public void setEndposition(int endposition)
    {
        this.endposition=endposition;
    }

    public Map<String,String> getMTagAttributes()
    {
        return mTagAttributes;
    }

    public void setMTagAttributes(Map<String,String> tagAttributes)
    {
        mTagAttributes=tagAttributes;
    }

    public List<AbsTagInTemplate> getLstTagChildren()
    {
        return lstTagChildren;
    }

    public void setLstTagChildren(List<AbsTagInTemplate> lstTagChildren)
    {
        this.lstTagChildren=lstTagChildren;
    }

    public void parseTagValue()
    {
        if(this.tagContent!=null&&!this.tagContent.trim().equals(""))
        {
            this.lstTagChildren=TemplateParser.parseHtmlContent(this,this.tagContent);
            if(this.lstTagChildren!=null&&this.lstTagChildren.size()>0)
            {
                for(AbsTagInTemplate tagbeanTmp:lstTagChildren)
                {
                    tagbeanTmp.parseTagValue();
                }
            }
        }
    }

    protected AbsComponentType getDisplayComponentObj(ReportRequest rrequest)
    {
        if(this.mTagAttributes==null) return null;
        String comid=this.mTagAttributes.get("componentid");
        if(comid==null||comid.trim().equals("")) return null;
        AbsComponentType comObj=(AbsComponentType)rrequest.getComponentTypeObj(comid,null,false);
        if(comObj==null)
        {
            throw new WabacusRuntimeException("没有取到要显示ID为"+comid+"的组件");
        }
        return comObj;
    }
    
    public abstract String getTagname();
    
    public abstract String getDisplayValue(ReportRequest rrequest,AbsComponentType ownerComponentObj);
    
    public static AbsTagInTemplate createTagObj(AbsTagInTemplate parentTag,
            String tagname,boolean ismust)
    {
        if(Consts_Private.TAGNAME_DATA.equals(tagname))
        {
            return new SDataTag(parentTag);
        }else if(Consts_Private.TAGNAME_NAVIGATE.equals(tagname))
        {
            return new SNavigateTag(parentTag);
        }else if(Consts_Private.TAGNAME_BUTTON.equals(tagname))
        {
            return new SButtonTag(parentTag);
        }else if(Consts_Private.TAGNAME_HEADER.equals(tagname))
        {
            return new SHeaderTag(parentTag);
        }else if(Consts_Private.TAGNAME_FOOTER.equals(tagname))
        {
            return new SFooterTag(parentTag);
        }else if(Consts_Private.TAGNAME_TITLE.equals(tagname))
        {
            return new STitleTag(parentTag);
        }else if(Consts_Private.TAGNAME_SEARCHBOX.equals(tagname))
        {
            return new SSearchBoxTag(parentTag);
        }else if(Consts_Private.TAGNAME_DATAIMPORT.equals(tagname))
        {
            return new SDataImportTag(parentTag);
        }else if(Consts_Private.TAGNAME_FILEUPLOAD.equals(tagname))
        {
            return new SFileUploadTag(parentTag);
        }else if(Consts_Private.TAGNAME_OUTPUT.equals(tagname))
        {
            return new SOutputTag(parentTag);
        }else if(ismust)
        {
            throw new WabacusConfigLoadingException("无效的标签名"+tagname);
        }
        return null;
    }
}
