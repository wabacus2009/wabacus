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
package com.wabacus.system.serveraction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.inputbox.FileBox;
import com.wabacus.util.Tools;

public class CommonServerAction implements IServerAction
{
    private final static Log log=LogFactory.getLog(CommonServerAction.class);
    
    public String executeServerAction(HttpServletRequest request,HttpServletResponse response,List<Map<String,String>> lstData)
    {
        if(Tools.isEmpty(lstData)) return null;
        Map<String,String> mParamValues=lstData.get(0);
        String actiontype=mParamValues.get("ACTIONTYPE");
        if("deleteuploadfile".equalsIgnoreCase(actiontype))
        {
           return deleteUploadFiles(request,response,mParamValues); 
        }
        return null;
    }

    private String deleteUploadFiles(HttpServletRequest request,HttpServletResponse response,Map<String,String> mParamValues)
    {
        String pageid=mParamValues.get("PAGEID");
        String reportid=mParamValues.get("REPORTID");
        PageBean pbean=Config.getInstance().getPageBean(pageid);
        if(pbean==null)
        {
            log.debug("删除文件失败，页面ID："+pageid+"不存在");
            return "error|删除文件失败，没有取到页面ID";
        }
        ReportBean rbean=pbean.getReportChild(reportid,true);
        if(rbean==null)
        {
            log.debug("删除文件失败，ID为"+pageid+"的页面下不存在ID为"+reportid+"的报表");
            return "error|删除文件失败，没有取到报表ID";
        }
        String filepaths=mParamValues.get("DELETEFILES");
        String inputboxid=mParamValues.get("INPUTBOXID");
        if(Tools.isEmpty(filepaths)) return "error|没有取到要删除的文件";
        if(Tools.isEmpty(inputboxid)) return "error|没有取到要删除的文件上传输入框对象";
        String boxid=inputboxid;
        int idx=boxid.lastIndexOf("__");
        if(idx>0) boxid=boxid.substring(0,idx);
        FileBox fileboxObj=rbean.getUploadFileBox(boxid);
        if(fileboxObj==null)
        {
            log.debug("报表"+rbean.getPath()+"下面不存在ID为"+boxid+"的文件上传输入框");
            return "error|删除失败，没有取到文件上传输入框对象";
        }
        String savepath=fileboxObj.getSavePath();
        String seperator=fileboxObj.getSeperator();
        if(seperator==null||seperator.equals("")) seperator=";";
        List<String> lstDeleteFiles=Tools.parseStringToList(filepaths,seperator,false);
        String fileNameTmp;
        File fTmp;
        List<String> lstSuccessFiles=new ArrayList<String>();
        List<String> lstFailedFiles=new ArrayList<String>();
        for(String fileTmp:lstDeleteFiles)
        {
            if(Tools.isEmpty(fileTmp)) continue;
            fileTmp=FilePathAssistant.getInstance().standardFilePath(fileTmp);
            idx=fileTmp.lastIndexOf(File.separator);
            fileNameTmp=idx>0?fileTmp.substring(idx+File.separator.length()):fileTmp;
            try
            {
                fTmp=new File(FilePathAssistant.getInstance().standardFilePath(savepath+File.separator+fileNameTmp));
                if(!fTmp.exists()||!fTmp.isFile()) fTmp=new File(fileTmp);
                if(fTmp.exists()&&fTmp.isFile())
                {
                    fTmp.delete();
                    lstSuccessFiles.add(fileTmp);
                }else
                {
                    lstFailedFiles.add(fileTmp);
                }
            }catch(Exception e)
            {
                log.error("删除文件"+fileTmp+"失败",e);
                lstFailedFiles.add(fileTmp);
            }
        }
        if(lstSuccessFiles.size()==0&&lstFailedFiles.size()==0) return "warn|没有取到要删除的文件";
        StringBuilder promptBuf=new StringBuilder();
        if(lstSuccessFiles.size()>0)
        {
            promptBuf.append("删除文件[");
            for(String fileTmp:lstSuccessFiles)
            {
                promptBuf.append(fileTmp+";");
            }
            promptBuf.append("]成功!");
        }
        if(lstFailedFiles.size()>0)
        {
            promptBuf.append("\n删除文件[");
            for(String fileTmp:lstFailedFiles)
            {
                promptBuf.append(fileTmp+";");
            }
            promptBuf.append("]失败!");
        }
        String promptType;
        if(lstSuccessFiles.size()>0&&lstFailedFiles.size()>0)
        {//即有删除成功又有删除失败的文件，则警告提示
            promptType="warn";
        }else if(lstSuccessFiles.size()>0)
        {
            promptType="success";
        }else
        {
            promptType="error";
        }
        return promptType+"|"+promptBuf.toString();
    }
    
    public String executeSeverAction(ReportRequest rrequest,IComponentConfigBean ccbean,List<Map<String,String>> lstData,
            Map<String,String> customizedData)
    {
        return null;
    }

}

