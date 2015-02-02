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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.buttons.DataImportButton;

public class DataImportReportUpload extends AbsFileUpload
{
    private DataImportButton dataImportButtonObj;
    
    public DataImportReportUpload(HttpServletRequest request)
    {
        super(request);
    }

    public void showUploadForm(PrintWriter out)
    {
        String pageid=getRequestString("PAGEID","");
        String comid=getRequestString("COMPONENTID","");
        String buttonname=getRequestString("DATAIMPORT_BUTTONNAME","");
        this.dataImportButtonObj=getDataImportButtonObj(pageid,comid,buttonname);
        out.print("<input type='hidden' name='PAGEID' value='"+pageid+"'/>");
        out.print("<input type='hidden' name='COMPONENTID' value='"+comid+"'/>");
        out.print("<input type='hidden' name='DATAIMPORT_BUTTONNAME' value='"+buttonname+"'/>");
        boolean flag=true;
        if(this.dataImportButtonObj.getDataimportInterceptorObj()!=null)
        {
            interceptorObj=this.dataImportButtonObj.getDataimportInterceptorObj();
            Map<String,String> mFormFieldValues=(Map<String,String>)request.getAttribute("WX_FILE_UPLOAD_FIELDVALUES");
            request.setAttribute("LST_DATAIMPORT_CONFIGBEANS",this.dataImportButtonObj.getLstDataImportItems());
            flag=interceptorObj.beforeDisplayFileUploadInterface(request,mFormFieldValues,out);
        }
        if(flag)
        {
            out.print(showDataImportFileUpload(this.dataImportButtonObj.getLstDataImportFileNames()));
        }
    }

    public String doFileUpload(List lstFieldItems,PrintWriter out)
    {
        String pageid=mFormFieldValues.get("PAGEID");
        String comid=mFormFieldValues.get("COMPONENTID");
        String buttonname=mFormFieldValues.get("DATAIMPORT_BUTTONNAME");
        this.dataImportButtonObj=getDataImportButtonObj(pageid,comid,buttonname);
        this.interceptorObj=this.dataImportButtonObj.getDataimportInterceptorObj();
        return uploadDataImportFiles(lstFieldItems,this.dataImportButtonObj.getLstDataImportItems(),this.dataImportButtonObj.isDataImportAsyn(),out);
    }
    
    private DataImportButton getDataImportButtonObj(String pageid,String comid,String buttonname)
    {
        PageBean pbean=Config.getInstance().getPageBean(pageid);
        if(pbean==null)
        {
            throw new WabacusRuntimeException("页面ID："+pageid+"不存在");
        }
        IComponentConfigBean ccbeanTmp=null;
        if(comid.equals(pageid))
        {
            ccbeanTmp=pbean;
        }else
        {
            ccbeanTmp=pbean.getChildComponentBean(comid,true);
            if(ccbeanTmp==null)
            {
                throw new WabacusRuntimeException("ID为"+pageid+"的页面下不存在ID为"+comid+"的子组件");
            }
        }
        DataImportButton buttonObj=(DataImportButton)ccbeanTmp.getButtonsBean().getButtonByName(buttonname);
        if(buttonObj==null)
        {
            throw new WabacusRuntimeException("组件"+ccbeanTmp.getPath()+"下不存在name为"+buttonname+"的数据导入按钮");
        }
        return buttonObj;
    }
    
    public void promptSuccess(PrintWriter out,boolean isArtDialog)
    {
        String message="";
        if(this.dataImportButtonObj.isDataImportAsyn())
        {//异步导入
            message="数据文件上传成功";
        }else
        {
            message="数据文件导入成功!";
        }
        String parentRef=null;
        if(isArtDialog)
        {
            out.println("artDialog.open.origin.wx_success('"+message+"');");
            out.println("art.dialog.close();");
            parentRef="artDialog.open.origin";
        }else
        {
            out.println("parent.wx_success('"+message+"');");
            out.println("parent.closePopupWin();");
            parentRef="parent";
        }
        IComponentConfigBean ccbean=this.dataImportButtonObj.getCcbean();
        if(!this.dataImportButtonObj.isDataImportAsyn())
        {
            out.println(parentRef+".refreshComponentDisplay('"+ccbean.getPageBean().getId()+"','"+ccbean.getId()+"',true);");
        }
    }
}
