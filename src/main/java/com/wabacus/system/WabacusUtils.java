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
package com.wabacus.system;

import java.util.HashMap;
import java.util.Map;

import com.wabacus.system.assistant.TagAssistant;

public class WabacusUtils
{
    private static Map<String,String> mEncodedFilePaths=new HashMap<String,String>();

    private static Map<String,String> mDecodedFilePaths=new HashMap<String,String>();

    public static String encodeFilePath(String filepath)
    {
        if(filepath==null||filepath.trim().equals("")) return "";
        if(mEncodedFilePaths.containsKey(filepath.toLowerCase().trim())) return mEncodedFilePaths.get(filepath.toLowerCase().trim());
        String filecode=(int)(Math.random()*10000)+"_"+(int)(Math.random()*10000);
        mEncodedFilePaths.put(filepath.toLowerCase().trim(),filecode);
        mDecodedFilePaths.put(filecode,filepath);
        return filecode;
    }

    public static String decodeFilePath(String filecode)
    {
        if(filecode==null||filecode.trim().equals("")) return "";
        return mDecodedFilePaths.get(filecode);
    }

    public static String createUploadLoadFileTag(ReportRequest rrequest,Map<String,String> mParams)
    {
        String label=null;
        String maxsize=null;//允许上传的最大文件大小，以KB为单位
        String allowtypes=null;
        String disallowtypes=null;
        String uploadcount="1";
        String newfilename=null;
        String savepath=null;//上传文件保存路径
        String rooturl=null;
        String popupparams=null;
        String beforepopup=null;
        String initsize=null;
        String interceptor=null;
        if(mParams!=null)
        {
            label=mParams.get("label");
            maxsize=mParams.get("maxsize");
            allowtypes=mParams.get("allowtypes");
            disallowtypes=mParams.get("disallowtypes");
            uploadcount=mParams.get("uploadcount");
            newfilename=mParams.get("newfilename");
            savepath=mParams.get("savepath");
            rooturl=mParams.get("rooturl");
            popupparams=mParams.get("popupparams");
            beforepopup=mParams.get("beforepopup");
            interceptor=mParams.get("interceptor");
            initsize=mParams.get("initsize");
        }
        return TagAssistant.getInstance().getFileUploadDisplayValue(maxsize,allowtypes,disallowtypes,uploadcount,newfilename,savepath,rooturl,
                popupparams,initsize,interceptor,label,beforepopup,null);//request中传入null，是因为不用导入弹出窗口的JS
    }
}
