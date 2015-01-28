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
package com.wabacus.system.buttons;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.intercept.AbsFileUploadInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class DataImportButton extends WabacusButton
{
    private DataImportBean dataimportBean;
    
    public DataImportButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    public String getButtonType()
    {
        return Consts.IMPORT_DATA;
    }

    public String showButton(ReportRequest rrequest,String dynclickevent)
    {
        return super.showButton(rrequest,getDataImportEvent());
    }

    public String showButton(ReportRequest rrequest,String dynclickevent,String button)
    {
        return super.showButton(rrequest,getDataImportEvent(),button);
    }

    public AbsFileUploadInterceptor getDataimportInterceptorObj()
    {
        return this.dataimportBean.getDataimportInterceptorObj();
    }
    
    public boolean isDataImportAsyn()
    {
        return this.dataimportBean.isDataImportAsyn();
    }
    
    public List<String> getLstDataImportFileNames()
    {
        return this.dataimportBean.getLstDataImportFileNames();
    }
    
    public List<AbsDataImportConfigBean> getLstDataImportItems()
    {
        return this.dataimportBean.getLstDataImportItems();
    }
    
    private String getDataImportEvent()
    {
        String token="?";
        if(Config.showreport_url.indexOf("?")>0) token="&";
        String serverurl=Config.showreport_url+token+"PAGEID="+ccbean.getPageBean().getId()+"&COMPONENTID="+ccbean.getId()
                +"&ACTIONTYPE=ShowUploadFilePage&FILEUPLOADTYPE="+Consts_Private.FILEUPLOADTYPE_DATAIMPORTREPORT;
        serverurl+="&DATAIMPORT_BUTTONNAME="+this.getName();
        return "wx_winpage('"+serverurl+"',"+this.dataimportBean.getDataimportpopupparams()+");";
    }

    public void loadExtendConfig(XmlElementBean eleButtonBean)
    {
        super.loadExtendConfig(eleButtonBean);
        XmlElementBean eleDataImportBean=eleButtonBean.getChildElementByName("dataimport");
        if(eleDataImportBean==null)
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"上的数据导入按钮失败，没有配置<dataimport/>子标签");
        }
        String ref=eleDataImportBean.attributeValue("ref");
        if(ref==null||ref.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"上的数据导入按钮失败，<dataimport/>标签的ref属性不能为空");
        }
        String interceptor=eleDataImportBean.attributeValue("interceptor");
        String asyn=eleDataImportBean.attributeValue("asyn");
        String popupparams=eleDataImportBean.attributeValue("popupparams");
        String popupinitsize=eleDataImportBean.attributeValue("popupinitsize");
        this.dataimportBean=new DataImportBean();
        List<AbsDataImportConfigBean> lstDataImports=new ArrayList<AbsDataImportConfigBean>();
        List<String> lst=Tools.parseStringToList(ref,";",false);
        for(String strTmp:lst)
        {
            if(strTmp.equals("")) continue;
            if(!Tools.isDefineKey("$",strTmp))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"失败，配置的数据导出项"+strTmp+"不是从资源文件中获取");
            }
            Object obj=Config.getInstance().getResourceObject(null,this.ccbean.getPageBean(),strTmp,true);
            if(!(obj instanceof AbsDataImportConfigBean))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.ccbean.getPath()+"失败，配置的数据导出项"+strTmp+"对应的资源项不是数据导出项资源类型");
            }
            lstDataImports.add((AbsDataImportConfigBean)obj);
        }
        this.dataimportBean.setLstDataImportItems(lstDataImports);
        popupparams=WabacusAssistant.getInstance().addDefaultPopupParams(popupparams,popupinitsize,"300","160",null);
        if(popupparams!=null) this.dataimportBean.setDataimportpopupparams(popupparams.trim());
        if(interceptor!=null&&!interceptor.trim().equals(""))
        {
            this.dataimportBean.setDataimportInterceptorObj(AbsFileUploadInterceptor.createInterceptorObj(interceptor.trim()));
        }
        this.dataimportBean.setDataImportAsyn("true".equalsIgnoreCase(asyn));
    }

    private class DataImportBean
    {
        private List<AbsDataImportConfigBean> lstDataImportItems;

        private AbsFileUploadInterceptor dataimportInterceptorObj;

        private boolean isDataImportAsyn;

        private List<String> lstDataImportFileNames;

        private String dataimportpopupparams;//弹出数据导入窗口参数，以json形式传入弹出组件所能接受的参数

        private List<AbsDataImportConfigBean> getLstDataImportItems()
        {
            return lstDataImportItems;
        }

        public void setLstDataImportItems(List<AbsDataImportConfigBean> lstDataImportItems)
        {
            lstDataImportFileNames=new ArrayList<String>();
            if(lstDataImportItems!=null&&lstDataImportItems.size()>0)
            {
                String fileuploadpath=null;
                for(AbsDataImportConfigBean dataImportTmp:lstDataImportItems)
                {
                    if(!lstDataImportFileNames.contains(dataImportTmp.getFilename())) lstDataImportFileNames.add(dataImportTmp.getFilename());
                    if(fileuploadpath==null)
                    {
                        fileuploadpath=dataImportTmp.getFilepath();
                    }else if(!fileuploadpath.equals(dataImportTmp.getFilepath()))
                    {
                        throw new WabacusConfigLoadingException("加载组件"+ccbean.getPath()+"数据导入按钮失败，被同一个报表引用的所有数据导入项"+dataImportTmp.getReskey()+"的导入路径必须一致");
                    }
                }
            }
            this.lstDataImportItems=lstDataImportItems;
        }

        private AbsFileUploadInterceptor getDataimportInterceptorObj()
        {
            return dataimportInterceptorObj;
        }

        private void setDataimportInterceptorObj(AbsFileUploadInterceptor dataimportInterceptorObj)
        {
            this.dataimportInterceptorObj=dataimportInterceptorObj;
        }

        private boolean isDataImportAsyn()
        {
            return isDataImportAsyn;
        }

        private void setDataImportAsyn(boolean isDataImportAsyn)
        {
            this.isDataImportAsyn=isDataImportAsyn;
        }

        private List<String> getLstDataImportFileNames()
        {
            return lstDataImportFileNames;
        }

        private void setLstDataImportFileNames(List<String> lstDataImportFileNames)
        {
            this.lstDataImportFileNames=lstDataImportFileNames;
        }

        private String getDataimportpopupparams()
        {
            if(dataimportpopupparams==null||dataimportpopupparams.trim().equals("")) return "null";
            return dataimportpopupparams;
        }

        private void setDataimportpopupparams(String dataimportpopupparams)
        {
            this.dataimportpopupparams=dataimportpopupparams;
        }
    }
}
