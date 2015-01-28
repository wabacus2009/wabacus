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

import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.util.Consts_Private;

public class SFileUploadTag extends AbsTagInTemplate
{
    public SFileUploadTag(AbsTagInTemplate parentTag)
    {
        super(parentTag);
    }

    public String getTagname()
    {
        return Consts_Private.TAGNAME_FILEUPLOAD;
    }

    public String getDisplayValue(ReportRequest rrequest,AbsComponentType ownerComponentObj)
    {
        String maxsize=null;
        String allowtypes=null;
        String disallowtypes=null;
        String uploadcount="1";
        String newfilename=null;//文件上传的新文件名，如果没有配置，则不改变上传文件的文件名，保留原名，只有uploadcount为1时才能指定此属性
        String savepath=null;
        String rooturl=null;
        String popupparams=null;
        String beforepopup=null;
        String initsize=null;
        String interceptor=null;//拦截器
        if(this.mTagAttributes!=null)
        {
            maxsize=this.mTagAttributes.get("maxsize");
            allowtypes=this.mTagAttributes.get("allowtypes");
            disallowtypes=this.mTagAttributes.get("disallowtypes");
            uploadcount=this.mTagAttributes.get("uploadcount");
            newfilename=this.mTagAttributes.get("newfilename");
            savepath=this.mTagAttributes.get("savepath");
            rooturl=this.mTagAttributes.get("rooturl");
            popupparams=this.mTagAttributes.get("popupparams");
            beforepopup=this.mTagAttributes.get("beforepopup");
            interceptor=this.mTagAttributes.get("interceptor");
            initsize=this.mTagAttributes.get("initsize");
        }
        return TagAssistant.getInstance().getFileUploadDisplayValue(maxsize,allowtypes,disallowtypes,uploadcount,
                newfilename,savepath,rooturl,popupparams,initsize,interceptor,this.tagContent,beforepopup,rrequest.getRequest());
    }
}
