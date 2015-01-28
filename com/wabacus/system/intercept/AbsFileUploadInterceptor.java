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
package com.wabacus.system.intercept;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;

import com.wabacus.config.ConfigLoadManager;
import com.wabacus.exception.WabacusConfigLoadingException;

public abstract class AbsFileUploadInterceptor
{
    public final static String SAVEPATH_KEY="SAVEPATH";
    
    public final static String ROOTURL_KEY="ROOTURL";
    
    public final static String FILENAME_KEY="FILENAME";
    
    public final static String MAXSIZE_KEY="MAXSIZE";
    
    public final static String ALLOWTYPES_KEY="ALLOWTYPES";//允许上传的文件类型
    
    public final static String DISALLOWTYPES_KEY="DISALLOWTYPES";
    
    public final static String PAGEID_KEY="PAGEID";
    
    public final static String REPORTID_KEY="REPORTID";
    
    public final static String INPUTBOXID_KEY="INPUTBOXID";
    
    public final static String SAVEVALUE_KEY="SAVEVALUE";//上传输入框上传后要保存的值
    
    public boolean beforeDisplayFileUploadInterface(HttpServletRequest request,Map<String,String> mFormAndConfigValues,PrintWriter out)
    {
        return true;
    }

    public boolean beforeFileUpload(HttpServletRequest request,FileItem fileitemObj,Map<String,String> mFormAndConfigValues,PrintWriter out)
    {
        return true;
    }
    
    public boolean beforeDisplayFileUploadPrompt(HttpServletRequest request,List lstFieldItems,Map<String,String> mFormAndConfigValues,
            String failedMessage,PrintWriter out)
    {
        return true;
    }
    
    public static AbsFileUploadInterceptor createInterceptorObj(String interceptor)
    {
        if(interceptor==null||interceptor.trim().equals("")) return null;
        Object interObj=null;
        try
        {
            interObj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(interceptor.trim()).newInstance();
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("文件上传拦截器"+interceptor+"类无法实例化",e);
        }
        if(!(interObj instanceof AbsFileUploadInterceptor))
        {
            throw new WabacusConfigLoadingException("文件上传拦截器"+interceptor+"类没有继承框架父类"+AbsFileUploadInterceptor.class.getName());
        }
        return (AbsFileUploadInterceptor)interObj;
    }
}
