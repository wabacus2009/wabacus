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
package com.wabacus.config.dataexport;

import java.io.File;

import com.wabacus.config.Config;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.task.TimingThread;
import com.wabacus.util.Tools;

public class DataExportLocalStroageBean
{
    public static String default_dataexport_directorydateformat;
    
    public final static String ROOTDIRECTORY="{root-dir}";
    
    public static String dataExportFileRootPath;
    
    public static String dataExportFileRootUrl;
    
    private String directorydateformat;
    
    private boolean download;//在本地落地后是否要提供下载URL

    private boolean autodelete;
    
    private boolean zip;
    
    public boolean isDownload()
    {
        return download;
    }

    public boolean isAutodelete()
    {
        return autodelete;
    }

    public void setDownload(boolean download)
    {
        this.download=download;
    }

    public void setAutodelete(boolean autodelete)
    {
        this.autodelete=autodelete;
    }
    
    public boolean isZip()
    {
        return zip;
    }

    public void setZip(boolean zip)
    {
        this.zip=zip;
    }

    public void setDirectorydateformat(String directorydateformat)
    {
        this.directorydateformat=directorydateformat;
    }
    
    public String getRealDirectorydateformat()
    {
        if(Tools.isEmpty(directorydateformat))
        {
            if(Tools.isEmpty(default_dataexport_directorydateformat))
            {
                directorydateformat=ROOTDIRECTORY;
            }else
            {
                directorydateformat=default_dataexport_directorydateformat;
            }
        }
        return directorydateformat;
    }

    public void doPostLoad()
    {
        if(default_dataexport_directorydateformat==null) default_dataexport_directorydateformat=Config.getInstance().getSystemConfigValue("default-dataexport-directorydateformat","");
        if(dataExportFileRootPath==null)
        {
            String dataExportTmpPath=Config.webroot_abspath;
            if(!dataExportTmpPath.endsWith(File.separator)) dataExportTmpPath=dataExportTmpPath+File.separator;
            dataExportTmpPath+="wxtmpfiles"+File.separator+"dataexport"+File.separator;
            FilePathAssistant.getInstance().checkAndCreateDirIfNotExist(dataExportTmpPath);
            dataExportFileRootPath=dataExportTmpPath;
            dataExportFileRootUrl=Config.webroot+"wxtmpfiles/dataexport/";
        }
        if(this.autodelete)
        {//需要自动删除
            if(!TimingThread.getInstance().isExistTask(dataExportFileRootPath))
            {
                DeleteDataExportTempFileTask taskObj=new DeleteDataExportTempFileTask();
                taskObj.parseInterval(Config.getInstance().getSystemConfigValue("dataexport-autodelete-interval",""));
                taskObj.setId(dataExportFileRootPath);
                taskObj.setFilepath(dataExportFileRootPath+"temp"+File.separator);//自动删除的数据文件统一放在dataexport/temp/目录下面
                TimingThread.getInstance().addTask(taskObj);
            }
        }
    }
}
