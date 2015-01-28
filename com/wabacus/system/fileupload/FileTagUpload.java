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
package com.wabacus.system.fileupload;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;

import com.wabacus.config.Config;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.intercept.AbsFileUploadInterceptor;
import com.wabacus.util.Tools;

public class FileTagUpload extends AbsFileUpload
{
    public FileTagUpload(HttpServletRequest request)
    {
        super(request);
    }

    public void showUploadForm(PrintWriter out)
    {
        String savepath=urlDecode(getRequestString("SAVEPATH",""));
        String rooturl=urlDecode(getRequestString("ROOTURL",""));
        String uploadcount=getRequestString("UPLOADCOUNT","");
        String newfilename=urlDecode(getRequestString("NEWFILENAME",""));
        String maxsize=getRequestString("MAXSIZE","");
        String allowtypes=urlDecode(getRequestString("ALLOWTYPES",""));
        String disallowtypes=urlDecode(getRequestString("DISALLOWTYPES",""));
        String interceptor=urlDecode(getRequestString("INTERCEPTOR",""));
        out.print("<input type='hidden' name='SAVEPATH' value='"+savepath+"'/>");
        out.print("<input type='hidden' name='ROOTURL' value='"+rooturl+"'/>");
        out.print("<input type='hidden' name='UPLOADCOUNT' value='"+uploadcount+"'/>");
        out.print("<input type='hidden' name='NEWFILENAME' value='"+newfilename+"'/>");
        out.print("<input type='hidden' name='MAXSIZE' value='"+maxsize+"'/>");
        out.print("<input type='hidden' name='ALLOWTYPES' value='"+allowtypes+"'/>");
        out.print("<input type='hidden' name='DISALLOWTYPES' value='"+disallowtypes+"'/>");
        out.print("<input type='hidden' name='INTERCEPTOR' value='"+interceptor+"'/>");
        boolean flag=true;
        if(interceptor!=null&&!interceptor.trim().equals(""))
        {
            AbsFileUploadInterceptor interceptorObj=AbsFileUploadInterceptor.createInterceptorObj(interceptor.trim());
            Map<String,String> mFormFieldValues=(Map<String,String>)request.getAttribute("WX_FILE_UPLOAD_FIELDVALUES");
            flag=interceptorObj.beforeDisplayFileUploadInterface(request,mFormFieldValues,out);
        }
        if(flag)
        {
            int iuploadcount=Integer.parseInt(uploadcount.trim());
            if(iuploadcount<=0) iuploadcount=1;
            out.print(displayFileUpload(iuploadcount,allowtypes,disallowtypes));
            out.print("</td></tr></table>");
        }
    }

    private String urlDecode(String urlparam)
    {
        if(urlparam==null||urlparam.trim().equals("")) return urlparam;
        try
        {
            return URLDecoder.decode(urlparam,"utf-8");
        }catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return urlparam;
        }
    }

    private  List<String> lstAllRootUrls;
    
    public String doFileUpload(List lstFieldItems,PrintWriter out)
    {
        String savepath=mFormFieldValues.get("SAVEPATH");
        String rooturl=mFormFieldValues.get("ROOTURL");
        String newfilename=mFormFieldValues.get("NEWFILENAME");
        String maxsize=mFormFieldValues.get("MAXSIZE");
        String configAllowTypes=mFormFieldValues.get("ALLOWTYPES");
        String configDisallowTypes=mFormFieldValues.get("DISALLOWTYPES");
        String interceptor=mFormFieldValues.get("INTERCEPTOR");
        long imaxsize=-1L;
        if(maxsize!=null&&!maxsize.trim().equals(""))
        {
            imaxsize=Long.parseLong(maxsize.trim())*1024;
        }
        if(Tools.isDefineKey("classpath",savepath))
        {
            throw new WabacusConfigLoadingException("显示文件上传标签失败，不能将文件上传标签的savepath属性指定为classpath{}格式");
        }
        if(!Tools.isDefineKey("absolute",savepath)&&!Tools.isDefineKey("relative",savepath))
        {
            throw new WabacusConfigLoadingException("显示文件上传标签失败，必须将上传文件的保存路径配置为absolute{}或relative{}格式");
        }
        savepath=WabacusAssistant.getInstance().parseAndGetRealValue(request,savepath,"");
        newfilename=WabacusAssistant.getInstance().parseAndGetRealValue(request,newfilename,"");
        rooturl=WabacusAssistant.getInstance().parseAndGetRealValue(request,rooturl,"");
        savepath=WabacusAssistant.getInstance().parseConfigPathToRealPath(savepath,Config.webroot_abspath);
        if(interceptor!=null&&!interceptor.trim().equals(""))
        {
            interceptorObj=AbsFileUploadInterceptor.createInterceptorObj(interceptor.trim());
        }
        List<String> lstConfigAllowTypes=getFileSuffixList(configAllowTypes);
        List<String> lstConfigDisallowTypes=getFileSuffixList(configDisallowTypes);
        FileItem item;
        List<String> lstDestFileNames=new ArrayList<String>();
        lstAllRootUrls=new ArrayList<String>();
        boolean existUploadFile=false;
        for(Object itemObj:lstFieldItems)
        {
            item=(FileItem)itemObj;
            if(item.isFormField()) continue;
            String originalFilename=item.getName();
            if((originalFilename==null||originalFilename.equals(""))) continue;
            originalFilename=getFileNameFromAbsolutePath(originalFilename);
            if((originalFilename==null||originalFilename.equals(""))) continue;
            mFormFieldValues.put(AbsFileUploadInterceptor.ALLOWTYPES_KEY,configAllowTypes);
            mFormFieldValues.put(AbsFileUploadInterceptor.DISALLOWTYPES_KEY,configDisallowTypes);
            mFormFieldValues.put(AbsFileUploadInterceptor.MAXSIZE_KEY,String.valueOf(String.valueOf(imaxsize)));
            mFormFieldValues.put(AbsFileUploadInterceptor.FILENAME_KEY,getSaveFileName(originalFilename,newfilename));
            mFormFieldValues.put(AbsFileUploadInterceptor.SAVEPATH_KEY,savepath);
            mFormFieldValues.put(AbsFileUploadInterceptor.ROOTURL_KEY,rooturl);
            existUploadFile=true;
            boolean shouldUpload=interceptorObj!=null?interceptorObj.beforeFileUpload(request,item,mFormFieldValues,out):true;
            if(shouldUpload)
            {
                getRealUploadFileName(lstDestFileNames,originalFilename);
                String errorMessage=doUploadFileAction(item,mFormFieldValues,originalFilename,configAllowTypes,lstConfigAllowTypes,
                        configDisallowTypes,lstConfigDisallowTypes);
                if(errorMessage!=null&&!errorMessage.trim().equals("")) return errorMessage;
            }
            String destfilenameTmp=mFormFieldValues.get(AbsFileUploadInterceptor.FILENAME_KEY);
            String rooturlTmp=mFormFieldValues.get(AbsFileUploadInterceptor.ROOTURL_KEY);
            if(!Tools.isEmpty(rooturlTmp)&&!Tools.isEmpty(destfilenameTmp))
            {
                if(!rooturlTmp.endsWith("/")) rooturlTmp+="/";
                lstAllRootUrls.add(rooturlTmp+destfilenameTmp);
            }
        }
        return existUploadFile?null:"请选择要上传的文件!";
    }

    public void promptSuccess(PrintWriter out,boolean isArtDialog)
    {
        if(lstAllRootUrls!=null&&lstAllRootUrls.size()>0)
        {//需要提示返回的访问URL
            promptByWinDialog(out,isArtDialog);
        }else
        {
            promptBySuccessDialog(out,isArtDialog);
        }
    }
    
    private void promptByWinDialog(PrintWriter out,boolean isArtDialog)
    {
        StringBuilder buf=new StringBuilder();
        buf.append("<p align='center'><b>文件上传成功!</b></p>");
        buf.append("上传文件URL：");
        if(lstAllRootUrls.size()==1)
        {
            buf.append(lstAllRootUrls.get(0));
        }else
        {
            for(String fileUrlTmp:lstAllRootUrls)
            {
                if(Tools.isEmpty(fileUrlTmp)) continue;
                buf.append("<br/>&nbsp;&nbsp;&nbsp;"+fileUrlTmp);
            }
        }
        if(isArtDialog)
        {
            out.println("artDialog.open.origin.wx_win(\""+buf.toString()+"\");");
            out.println("art.dialog.close();");
        }else
        {
            out.println("parent.wx_win(\""+buf.toString()+"\");");
            out.println("parent.closePopupWin();");
        }
    }
    
    public void promptBySuccessDialog(PrintWriter out,boolean isArtDialog)
    {
        if(isArtDialog)
        {
            out.println("artDialog.open.origin.wx_success('文件上传成功');");
            out.println("art.dialog.close();");
        }else
        {
            out.println("parent.wx_success('文件上传成功');");
            out.println("parent.closePopupWin();");
        }
    }
}
