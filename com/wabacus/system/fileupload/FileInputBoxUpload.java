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

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.inputbox.FileBox;
import com.wabacus.system.intercept.AbsFileUploadInterceptor;
import com.wabacus.system.serveraction.CommonServerAction;
import com.wabacus.util.Tools;

public class FileInputBoxUpload extends AbsFileUpload
{
    public FileInputBoxUpload(HttpServletRequest request)
    {
        super(request);
    }

    public void showUploadForm(PrintWriter out)
    {
        String pageid=getRequestString("PAGEID","");
        String reportid=getRequestString("REPORTID","");
        String inputboxid=getRequestString("INPUTBOXID","");
        PageBean pbean=Config.getInstance().getPageBean(pageid);
        if(pbean==null)
        {
            throw new WabacusRuntimeException("页面ID："+pageid+"不存在");
        }
        ReportBean rbean=pbean.getReportChild(reportid,true);
        if(rbean==null)
        {
            throw new WabacusRuntimeException("ID为"+pageid+"的页面下不存在ID为"+reportid+"的报表");
        }
        String boxid=inputboxid;
        int idx=boxid.lastIndexOf("__");
        if(idx>0)
        {
            boxid=boxid.substring(0,idx);
        }
        FileBox fileboxObj=rbean.getUploadFileBox(boxid);
        if(fileboxObj==null)
        {
            throw new WabacusRuntimeException("报表"+rbean.getPath()+"下面不存在ID为"+boxid+"的文件上传输入框");
        }
        String parentWindowName;
        if(Config.getInstance().getSystemConfigValue("prompt-dialog-type","artdialog").equals("artdialog"))
        {
            out.print("<script type=\"text/javascript\"  src=\""+Config.webroot+"webresources/component/artDialog/artDialog.js\"></script>");
            out.print("<script type=\"text/javascript\"  src=\""+Config.webroot+"webresources/component/artDialog/plugins/iframeTools.js\"></script>");
            parentWindowName="artDialog.open.origin";
        }else
        {
            parentWindowName="parent";
        }
        out.print("<input type='hidden' name='INPUTBOXID' value='"+inputboxid+"'/>");
        out.print("<input type='hidden' name='PAGEID' value='"+pageid+"'/>");
        out.print("<input type='hidden' name='REPORTID' value='"+reportid+"'/>");
        Map<String,String> mFormFieldValues=(Map<String,String>)request.getAttribute("WX_FILE_UPLOAD_FIELDVALUES");
        showDynParamHiddenFields(mFormFieldValues,fileboxObj.getLstParamNamesInUrl(),out);
        boolean flag=true;
        if(fileboxObj.getInterceptor()!=null)
        {
            flag=fileboxObj.getInterceptor().beforeDisplayFileUploadInterface(request,mFormFieldValues,out);
        }
        if(flag)
        {
            out.print(displayFileUpload(fileboxObj.getUploadcount(),fileboxObj.getAllowTypes(),fileboxObj.getDisallowtypes()));
            String oldvalue=getRequestString("OLDVALUE","");
            StringBuilder buf=new StringBuilder();
            if(fileboxObj.getDeletetype()>0&&!Tools.isEmpty(oldvalue))
            {
                buf.append("&nbsp;&nbsp;<input type=\"button\" class=\"cls-button\" value=\"删除\"");
                buf.append(" onclick=\""+parentWindowName+".setPopUpBoxValueToParent('','");
                buf.append(inputboxid+"');");
                if(fileboxObj.getDeletetype()>1)
                {//删除文件
                    buf.append(parentWindowName+".invokeServerAction('"+CommonServerAction.class.getName()+"'");
                    buf.append(",{ACTIONTYPE:'deleteuploadfile',DELETEFILES:'"+oldvalue+"',INPUTBOXID:'"+inputboxid+"'");
                    buf.append(",PAGEID:'"+pageid+"',REPORTID:'"+reportid+"'}");
                    buf.append(",deleteUploadFilesInvokeCallback,null)");
                }
                buf.append("\"/>");
                buf.append("</td></tr></table>");
            }
            out.print(buf.toString());
        }
    }

    private void showDynParamHiddenFields(Map<String,String> mFormFieldValues,List<String> lstParamNamesInUrl,PrintWriter out)
    {
        String oldvalue=null;
        if(mFormFieldValues!=null)
        {
            oldvalue=mFormFieldValues.get("OLDVALUE");
        }else
        {
            oldvalue=request.getParameter("OLDVALUE");
        }
        if(oldvalue!=null&&!oldvalue.trim().equals(""))
        {
            out.print("<input type=\"hidden\" name=\"OLDVALUE\" value=\""+oldvalue.trim()+"\"/>");
        }
        if(lstParamNamesInUrl==null||lstParamNamesInUrl.size()==0) return;
        String paramvalueTmp;
        for(String paramnameTmp:lstParamNamesInUrl)
        {
            if(mFormFieldValues!=null)
            {
                paramvalueTmp=mFormFieldValues.get(paramnameTmp);
            }else
            {
                paramvalueTmp=request.getParameter(paramnameTmp);
            }
            if(paramvalueTmp!=null&&!paramvalueTmp.trim().equals(""))
            {
                out.print("<input type=\"hidden\" name=\""+paramnameTmp+"\" value=\""+paramvalueTmp+"\"/>");
            }
        }
    }

    public String doFileUpload(List lstFieldItems,PrintWriter out)
    {
        String pageid=mFormFieldValues.get("PAGEID");
        String reportid=mFormFieldValues.get("REPORTID");
        String inputboxid=mFormFieldValues.get("INPUTBOXID");
        pageid=pageid==null?"":pageid.trim();
        inputboxid=inputboxid==null?"":inputboxid.trim();
        PageBean pbean=Config.getInstance().getPageBean(pageid);
        if(pbean==null)
        {
            throw new WabacusRuntimeException("文件上传失败，页面ID："+pageid+"不存在");
        }
        ReportBean rbean=pbean.getReportChild(reportid,true);
        if(rbean==null)
        {
            throw new WabacusRuntimeException("ID为"+pageid+"的页面下不存在ID为"+reportid+"的报表");
        }
        mFormFieldValues.put(AbsFileUploadInterceptor.REPORTID_KEY,reportid);
        String boxid=inputboxid;
        int idx=boxid.lastIndexOf("__");
        if(idx>0)
        {
            boxid=boxid.substring(0,idx);
        }
        FileBox fileboxObj=rbean.getUploadFileBox(boxid);
        if(fileboxObj==null)
        {
            throw new WabacusRuntimeException("报表"+rbean.getPath()+"下面不存在ID为"+boxid+"的文件上传输入框");
        }
        this.interceptorObj=fileboxObj.getInterceptor();
        out.println(fileboxObj.createSelectOkFunction(inputboxid,false));
        String configAllowTypes=fileboxObj.getAllowTypes();
        if(configAllowTypes==null) configAllowTypes="";
        List<String> lstConfigAllowTypes=getFileSuffixList(configAllowTypes);
        String configDisallowTypes=fileboxObj.getDisallowtypes();
        if(configDisallowTypes==null) configDisallowTypes="";
        List<String> lstConfigDisallowTypes=getFileSuffixList(configDisallowTypes);
        String savepath=WabacusAssistant.getInstance().parseAndGetRealValue(request,fileboxObj.getSavePath(),"");
        String newfilename=WabacusAssistant.getInstance().parseAndGetRealValue(request,fileboxObj.getNewfilename(),"");
        String rooturl=WabacusAssistant.getInstance().parseAndGetRealValue(request,fileboxObj.getRooturl(),"");
        String seperator=fileboxObj.getSeperator();
        if(Tools.isEmpty(seperator)) seperator=";";
        String allSaveValues="",saveValueTmp;
        boolean existUploadFile=false;
        List<String> lstDestFileNames=new ArrayList<String>();
        FileItem item;
        for(Object itemObj:lstFieldItems)
        {
            item=(FileItem)itemObj;
            if(item.isFormField()) continue;
            String orginalFilename=item.getName();
            if((orginalFilename==null||orginalFilename.equals(""))) continue;
            orginalFilename=getFileNameFromAbsolutePath(orginalFilename);
            if(orginalFilename.equals("")) return "文件上传失败，文件路径不合法";//非法的文件路径格式
            mFormFieldValues.put(AbsFileUploadInterceptor.ALLOWTYPES_KEY,configAllowTypes);
            mFormFieldValues.put(AbsFileUploadInterceptor.DISALLOWTYPES_KEY,configDisallowTypes);
            mFormFieldValues.put(AbsFileUploadInterceptor.MAXSIZE_KEY,String.valueOf(fileboxObj.getMaxsize()));
            mFormFieldValues.put(AbsFileUploadInterceptor.FILENAME_KEY,getSaveFileName(orginalFilename,newfilename));
            mFormFieldValues.put(AbsFileUploadInterceptor.SAVEPATH_KEY,savepath);
            mFormFieldValues.put(AbsFileUploadInterceptor.ROOTURL_KEY,rooturl);
            boolean shouldUpload=interceptorObj!=null?interceptorObj.beforeFileUpload(request,item,mFormFieldValues,out):true;
            if(shouldUpload)
            {
                getRealUploadFileName(lstDestFileNames,orginalFilename);
                String errorMessage=doUploadFileAction(item,mFormFieldValues,orginalFilename,configAllowTypes,lstConfigAllowTypes,
                        configDisallowTypes,lstConfigDisallowTypes);
                if(errorMessage!=null&&!errorMessage.trim().equals("")) return errorMessage;
            }
            existUploadFile=true;
            saveValueTmp=getSaveValue();
            if(!Tools.isEmpty(saveValueTmp)) allSaveValues+=saveValueTmp+seperator;
        }
        if(!existUploadFile) return "请选择要上传的文件!";
        if(allSaveValues.endsWith(seperator)) allSaveValues=allSaveValues.substring(0,allSaveValues.length()-seperator.length());
        out.print("<script language='javascript'>");
        out.print("selectOK('"+allSaveValues+"',null,null,false);");
        out.print("</script>");
        return null;
    }
    
    private String getSaveValue()
    {
        String savevalue=mFormFieldValues.get(AbsFileUploadInterceptor.SAVEVALUE_KEY);
        if(savevalue==null)
        {
            String destfilenameTmp=mFormFieldValues.get(AbsFileUploadInterceptor.FILENAME_KEY);
            String rooturlTmp=mFormFieldValues.get(AbsFileUploadInterceptor.ROOTURL_KEY);
            String savepathTmp=mFormFieldValues.get(AbsFileUploadInterceptor.SAVEPATH_KEY);
            if(Tools.isEmpty(rooturlTmp)&&Tools.isEmpty(savepathTmp))
            {
                savevalue=destfilenameTmp;
            }else if(Tools.isEmpty(rooturlTmp))
            {
                savevalue=FilePathAssistant.getInstance().standardFilePath(savepathTmp+File.separator+destfilenameTmp);
            }else
            {
                if(!rooturlTmp.endsWith("/")) rooturlTmp+="/";
                savevalue=rooturlTmp+destfilenameTmp;
            }
        }
        if(savevalue==null) savevalue="";
        StringBuilder pathBuf=new StringBuilder();
        for(int i=0;i<savevalue.length();i++)
        {
            if(savevalue.charAt(i)=='\\')
            {
                pathBuf.append("\\\\");
            }else
            {
                pathBuf.append(savevalue.charAt(i));
            }
        }
        return pathBuf.toString();
    }
    
    public void promptSuccess(PrintWriter out,boolean isArtDialog)
    {
        if(isArtDialog)
        {//artdialog提示组件
            out.println("artDialog.open.origin.wx_success('上传文件成功');");
            out.println("art.dialog.close();");
        }else
        {
            out.println("parent.wx_success('上传文件成功');");
            out.println("parent.closePopupWin();");
        }
    }
}
