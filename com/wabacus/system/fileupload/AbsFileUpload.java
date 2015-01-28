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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.system.assistant.DataImportAssistant;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.dataimport.DataImportItem;
import com.wabacus.system.dataimport.queue.UploadFilesQueue;
import com.wabacus.system.dataimport.thread.FileUpDataImportThread;
import com.wabacus.system.intercept.AbsFileUploadInterceptor;
import com.wabacus.util.Tools;

public abstract class AbsFileUpload
{
    private final static Log log=LogFactory.getLog(AbsFileUpload.class);

    protected HttpServletRequest request;

    protected String contentType;
    
    protected Map<String,String> mFormFieldValues;
    
    protected AbsFileUploadInterceptor interceptorObj;

    public AbsFileUpload(HttpServletRequest request)
    {
        this.request=request;
        this.contentType=request.getHeader("Content-type");
    }

    public Map<String,String> getMFormFieldValues()
    {
        return mFormFieldValues;
    }

    public void setMFormFieldValues(Map<String,String> formFieldValues)
    {
        mFormFieldValues=formFieldValues;
    }

    public AbsFileUploadInterceptor getInterceptorObj()
    {
        return interceptorObj;
    }

    protected String getRequestString(String paramname,String defaultvalue)
    {
        String paramvalue=null;
        if(contentType!=null&&contentType.startsWith("multipart/"))
        {
            paramvalue=(String)request.getAttribute(paramname);
            if(paramvalue==null||paramvalue.trim().equals(""))
                paramvalue=defaultvalue;
            else
                paramvalue=paramvalue.trim();
        }else
        {
            paramvalue=Tools.getRequestValue(request,paramname,defaultvalue);
        }
        return paramvalue;
    }

    protected static String getFileNameFromAbsolutePath(String filepath)
    {
        String filename=filepath;
        if(File.separator.equals("/"))
        {
            filepath=Tools.replaceAll(filepath,"\\","/");
            int idxsep=filepath.lastIndexOf("/");
            if(idxsep>=0) filename=filepath.substring(idxsep+1);
        }else
        {
            filepath=Tools.replaceAll(filepath,"/","\\");
            int idxsep=filepath.lastIndexOf("\\");
            if(idxsep>=0) filename=filepath.substring(idxsep+1);
        }
        return filename.trim();
    }

    protected String showDataImportFileUpload(List<String> lstDataImportFileNames)
    {
        if(lstDataImportFileNames==null||lstDataImportFileNames.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("<table border=0 cellspacing=1 cellpadding=2  style=\"margin:0px\" width=\"98%\" ID=\"Table1\" align=\"center\">");
        resultBuf.append("<tr class=filetitle><td style='font-size:13px;'>数据文件上传</td></tr>");
        StringBuffer fileNameBuf=new StringBuffer();
        int idx=0;
        for(String filenameTmp:lstDataImportFileNames)
        {
            fileNameBuf.append(filenameTmp).append("; ");
            resultBuf.append("<tr><td style='font-size:13px;'><input type=\"file\" contentEditable=\"false\" name=\"uploadfile"+(idx++)
                    +"\"></td></tr>");
        }
        if(fileNameBuf.length()>2&&fileNameBuf.charAt(fileNameBuf.length()-2)==';')
        {
            fileNameBuf.deleteCharAt(fileNameBuf.length()-2);
        }
        resultBuf.append("<tr class=filetitle><td style='font-size:13px;'>[上传文件名："+fileNameBuf.toString().trim()+"]</td></tr>");
        resultBuf.append("<tr><td style='font-size:13px;'><input type=\"submit\" class=\"cls-button\" name=\"submit\" value=\"上传\">");
        resultBuf.append("</td></tr></table>");
        return resultBuf.toString();
    }

    protected String uploadDataImportFiles(List lstFieldItems,List<AbsDataImportConfigBean> lstDiBeans,boolean isAsyn,PrintWriter out)
    {
        if(lstDiBeans==null||lstDiBeans.size()==0)
        {
            return "没有提供数据导入功能，不能上传数据导入文件";
        }
        String filepath=lstDiBeans.get(0).getFilepath();//同一次上传的所有数据导入项的上传文件路径必须是一致的，所以这里只取第一个。
        File f=new File(FilePathAssistant.getInstance().standardFilePath(filepath));
        if(!f.exists()||!f.isDirectory())
        {
            return "数据导入项"+lstDiBeans.get(0).getReskey()+"配置的filepath不存在或不是目录";
        }
        Iterator itFieldItems=lstFieldItems.iterator();
        FileItem itemTmp;
        Map<String,FileItem> mUploadFiles=new HashMap<String,FileItem>();
        //        List<String> lstAllFileNameAndPatterns=new ArrayList<String>();//存放本次上传对应的所有数据导入项不重复的filename属性值
        List<String> lstFileNams=new ArrayList<String>();
        String filepathTmp;
        while(itFieldItems.hasNext())
        {
            itemTmp=(FileItem)itFieldItems.next();
            if(itemTmp.isFormField()) continue;
            filepathTmp=itemTmp.getName();
            if((filepathTmp==null||filepathTmp.equals(""))) continue;
            String filename=getFileNameFromAbsolutePath(filepathTmp);
            if(filename.equals(""))
            {
                return "文件上传失败，文件路径不合法";
            }
            if(lstFileNams.contains(filename))
            {
                return "文件"+filename+"不能重复上传";
            }
            lstFileNams.add(filename);
            boolean shouldUpload=true;
            if(interceptorObj!=null)
            {
                shouldUpload=interceptorObj.beforeFileUpload(request,itemTmp,mFormFieldValues,out);
            }
            if(shouldUpload)
            {
                mUploadFiles.put(filename,itemTmp);
            }
        }
        if(mUploadFiles.size()==0)
        {
            return "没有取到要导入的数据文件";
        }
        Map<List<DataImportItem>,Map<File,FileItem>> uploadFiles=new HashMap<List<DataImportItem>,Map<File,FileItem>>();
        String errorinfo=generateUploadFilesAndImportItems(lstDiBeans,mUploadFiles,uploadFiles,filepath);//校验上传的数据文件与配置的数据导入项的关系是否正确
        if(errorinfo!=null&&!errorinfo.trim().equals("")) return errorinfo;
        if(isAsyn)
        {
            UploadFilesQueue.getInstance().addUploadFile(uploadFiles);
            return null;
        }else
        {
            Entry<List<DataImportItem>,Map<File,FileItem>> entry=uploadFiles.entrySet().iterator().next();
            return FileUpDataImportThread.getInstance().doDataImport(entry.getKey(),entry.getValue());
        }
    }

    private String generateUploadFilesAndImportItems(List<AbsDataImportConfigBean> lstDiBeans,Map<String,FileItem> mUploadFiles,
            Map<List<DataImportItem>,Map<File,FileItem>> mResults,String filepath)
    {
        List<DataImportItem> lstDataImportItems=new ArrayList<DataImportItem>();
        Map<File,FileItem> mUploadFileItems=new HashMap<File,FileItem>();//为了数据文件上传
        mResults.put(lstDataImportItems,mUploadFileItems);
        List<String> lstTmpFile=new ArrayList<String>();
        File fTmp;
        Map<String,File> mFileTmp=new HashMap<String,File>();
        String[] strArrTmp;
        for(AbsDataImportConfigBean dibeanTmp:lstDiBeans)
        {
            lstTmpFile.clear();
            for(String filenameTmp:mUploadFiles.keySet())
            {
                strArrTmp=DataImportAssistant.getInstance().getRealFileNameAndImportType(filenameTmp);
                if(dibeanTmp.isMatch(strArrTmp[0]))
                {
                    String file=FilePathAssistant.getInstance().standardFilePath(filepath+File.separator+filenameTmp);
                    fTmp=mFileTmp.get(file);
                    if(fTmp==null)
                    {
                        fTmp=new File(file);
                        mFileTmp.put(file,fTmp);
                        mUploadFileItems.put(fTmp,mUploadFiles.get(filenameTmp));
                    }
                    DataImportItem diitem=new DataImportItem(dibeanTmp,fTmp);
                    diitem.setRequest(request);
                    diitem.setSession(request.getSession());
                    diitem.setDynimportype(strArrTmp[1]);
                    lstDataImportItems.add(diitem);
                    lstTmpFile.add(filenameTmp);
                }
            }
            if(lstTmpFile.size()==0)
            {
                log.warn("本次上传没有上传与数据导入项"+dibeanTmp.getReskey()+"匹配的数据文件");
            }else if(lstTmpFile.size()>1)
            {
                log.warn("数据文件上传失败，数据导入项"+dibeanTmp.getReskey()+"与本次上传的多个数据文件名"+lstTmpFile+"匹配");
                return "上传失败，同时上传了多个与"+dibeanTmp.getFilename()+"匹配的数据文件";
            }
        }
        if(lstDataImportItems.size()==0)
        {
            return "没有上传有效的数据导入文件";
        }
        return null;
    }

    protected String getSaveFileName(String name,String newfilename)
    {
        if(Tools.isEmpty(newfilename)) return name;
        String suffix=null;
        int idx=name.lastIndexOf(".");
        if(idx>0) suffix=name.substring(idx);
        suffix=suffix==null?"":suffix.trim();
        name=newfilename;
        if(name.startsWith("{")&&name.endsWith("}"))
        {
            name=name.substring(1,name.length()-1).trim();
            if(name.equalsIgnoreCase("date"))
            {
                name=Tools.getStrDatetime("yyyy-MM-dd",new Date());
            }else if(name.equalsIgnoreCase("time"))
            {
                name=Tools.getStrDatetime("HH:mm:ss",new Date());
            }else if(name.equalsIgnoreCase("timestamp"))
            {
                name=String.valueOf(System.currentTimeMillis());
            }else
            {
                name=newfilename;
            }
        }
        return name+suffix;
    }

    protected String displayFileUpload(int uploadcount,String allowtypes,String disallowtypes)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("<table border=0 cellspacing=1 cellpadding=2  style=\"margin:0px\" width=\"98%\" ID=\"Table1\" align=\"center\">");
        resultBuf.append("<tr class=filetitle><td style='font-size:13px;'>文件上传</td></tr>");
        for(int i=0;i<uploadcount;i++)
        {//为每一种数据文件生成一个上传输入框
            resultBuf.append("<tr><td style='font-size:13px;'><input type=\"file\" contentEditable=\"false\" name=\"uploadfile"+i+"\"></td></tr>");
        }
        resultBuf.append(getAllowedFileSuffixPrompt(allowtypes,disallowtypes));
        resultBuf.append("<tr><td style='font-size:13px;'><input type=\"submit\" class=\"cls-button\" name=\"submit\" value=\"上传\">");
        return resultBuf.toString();
    }
    
    private String getAllowedFileSuffixPrompt(String allowtypes,String disallowtypes)
    {
        StringBuilder resultBuf=new StringBuilder();
        if(!Tools.isEmpty(allowtypes)||!Tools.isEmpty(disallowtypes))
        {
            resultBuf.append("<tr class=filetitle><td style='font-size:13px;'>[");
            if(!Tools.isEmpty(allowtypes)) resultBuf.append(stardardFileSuffixString(allowtypes));
            if(!Tools.isEmpty(disallowtypes))
            {
                resultBuf.append("&nbsp;&nbsp;disallowed：<font color='red'>"+stardardFileSuffixString(disallowtypes)+"</font>");
            }
            resultBuf.append("]</td></tr>");
        }
        return resultBuf.toString();
    }
    
    protected String stardardFileSuffixString(String filesuffixes)
    {
        if(filesuffixes==null||filesuffixes.trim().equals("")) return "";
        List<String> lstSuffixes=Tools.parseStringToList(filesuffixes.trim(),";",false);
        StringBuilder suffixBuf=new StringBuilder();
        for(String typeTmp:lstSuffixes)
        {
            if(typeTmp==null||typeTmp.trim().equals("")||typeTmp.trim().equals(".")) continue;
            typeTmp=typeTmp.trim();
            if(typeTmp.startsWith("."))
            {
                typeTmp=typeTmp.substring(1).trim();
            }
            suffixBuf.append(typeTmp.toLowerCase()).append(";");
        }
        return suffixBuf.toString();
    }

    protected List<String> getFileSuffixList(String filesuffixes)
    {
        if(filesuffixes==null||filesuffixes.trim().equals("")) return null;
        filesuffixes=filesuffixes.trim();
        List<String> lstResults=new ArrayList<String>();
        List<String> lstTemp=Tools.parseStringToList(filesuffixes,";",false);
        for(String filetype:lstTemp)
        {
            if(filetype==null||filetype.trim().equals("")) continue;
            filetype=filetype.trim();
            if(filetype.startsWith("."))
            {
                filetype=filetype.substring(1).trim();
                if(filetype.equals("")) continue;
            }
            lstResults.add(filetype.toLowerCase().trim());
        }
        return lstResults;
    }

    protected void getRealUploadFileName(List<String> lstDestFileNames,String originalFilename)
    {
        String destfilename=mFormFieldValues.get(AbsFileUploadInterceptor.FILENAME_KEY);
        if(Tools.isEmpty(destfilename)) destfilename=originalFilename;
        if(lstDestFileNames.contains(destfilename))
        {
            int idx=destfilename.lastIndexOf(".");
            String nameTmp=idx>0?destfilename.substring(0,idx):destfilename;
            String suffix=idx>0?destfilename.substring(idx):"";
            idx=1;
            while(true)
            {
                if(!lstDestFileNames.contains(nameTmp+"("+(++idx)+")"+suffix)) break;
            }
            destfilename=nameTmp+"("+idx+")"+suffix;
            mFormFieldValues.put(AbsFileUploadInterceptor.FILENAME_KEY,destfilename);
        }
        lstDestFileNames.add(destfilename);
    }
    
    protected String doUploadFileAction(FileItem item,Map<String,String> mFormFieldValues,String orginalFilename,String configAllowTypes,
            List<String> lstConfigAllowTypes,String configDisallowTypes,
            List<String> lstConfigDisallowTypes)
    {
        String strmaxsize=mFormFieldValues.get(AbsFileUploadInterceptor.MAXSIZE_KEY);
        if(strmaxsize!=null&&!strmaxsize.trim().equals(""))
        {
            long lmaxsize=Long.parseLong(strmaxsize.trim());
            if(lmaxsize>0&&lmaxsize<item.getSize()) return "上传失败，上传文件太大";
        }
        if(!isInvalidUploadFileType(orginalFilename,configAllowTypes,lstConfigAllowTypes,configDisallowTypes,lstConfigDisallowTypes))
        {
            return "文件上传失败，文件类型不合法";
        }
        String savepathTmp=mFormFieldValues.get(AbsFileUploadInterceptor.SAVEPATH_KEY);
        String destfilenameTmp=mFormFieldValues.get(AbsFileUploadInterceptor.FILENAME_KEY);
        if(!Tools.isEmpty(savepathTmp)&&!Tools.isEmpty(destfilenameTmp))
        {
            try
            {
                savepathTmp=FilePathAssistant.getInstance().standardFilePath(savepathTmp+File.separator);
                FilePathAssistant.getInstance().checkAndCreateDirIfNotExist(savepathTmp);
                item.write(new File(savepathTmp+destfilenameTmp));
            }catch(Exception e)
            {
                log.error("上传文件"+orginalFilename+"到路径"+savepathTmp+"失败",e);
                return "上传文件"+orginalFilename+"到路径"+savepathTmp+"失败";
            }
        }
        return null;
    }
    
    private boolean isInvalidUploadFileType(String orginalFilename,String configAllowTypes,List<String> lstConfigAllowTypes,
            String configDisallowTypes,List<String> lstConfigDisallowTypes)
    {
        String suffix="";
        int idxdot=orginalFilename.lastIndexOf(".");//这里要用orginalFilename，不能用destfilename，因为是判断上传的原始文件名，而不是上传到服务器后的文件名
        if(idxdot>0&&idxdot!=orginalFilename.length()-1) suffix=orginalFilename.substring(idxdot+1).toLowerCase().trim();
        String allowTypesTmp=mFormFieldValues.get(AbsFileUploadInterceptor.ALLOWTYPES_KEY);
        if(!Tools.isEmpty(allowTypesTmp))
        {
            List<String> lstAllowTypesTmp=allowTypesTmp.equalsIgnoreCase(configAllowTypes)?lstConfigAllowTypes:getFileSuffixList(allowTypesTmp);
            if(!Tools.isEmpty(lstAllowTypesTmp)&&!lstAllowTypesTmp.contains(suffix)) return false;
        }
        String disallowTypesTmp=mFormFieldValues.get(AbsFileUploadInterceptor.DISALLOWTYPES_KEY);
        if(!Tools.isEmpty(disallowTypesTmp))
        {
            List<String> lstDisallowTypesTmp=disallowTypesTmp.equalsIgnoreCase(configDisallowTypes)?lstConfigDisallowTypes
                    :getFileSuffixList(disallowTypesTmp);
            if(!Tools.isEmpty(lstDisallowTypesTmp)&&lstDisallowTypesTmp.contains(suffix)) return false;
        }
        return true;
    }
    
    public abstract void showUploadForm(PrintWriter out);

    public abstract String doFileUpload(List lstFieldItems,PrintWriter out);
    
    public abstract void promptSuccess(PrintWriter out,boolean isArtDialog);
}
