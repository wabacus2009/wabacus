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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.dataexport.DataExportLocalStroageBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class DataExportButton extends WabacusButton
{
    private String dataexporttype;

    private DataExportLocalStroageBean localStroageBean;//如果导出文件要在本地落地，在这里存放相关信息，只有在<button></button>中指定了导出组件时，才能在<button/>中指定是否落地，以及落地方式

    public DataExportButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    public String getDataexporttype()
    {
        return dataexporttype;
    }

    public void setDataexporttype(String dataexporttype)
    {
        this.dataexporttype=dataexporttype;
    }

    public String getButtonType()
    {
        return dataexporttype;
    }

    public boolean isExportBySpecifyApplicationids()
    {
        if(this.clickhandler==null) return false;
        if(this.clickhandler instanceof IButtonClickeventGenerate) return true;//在<button/>标签内容中指定了要导出的应用
        return !this.clickhandler.toString().trim().equals("");
    }

    public String showButton(ReportRequest rrequest,String dynclickevent)
    {
        return super.showButton(rrequest,getDataExportClickEvent(rrequest,null,this.dataexporttype,null));
    }

    public String showButton(ReportRequest rrequest,String dynclickevent,String button)
    {
        return super.showButton(rrequest,getDataExportClickEvent(rrequest,null,this.dataexporttype,null),button);
    }

    public String showMenu(ReportRequest rrequest,String dynclickevent)
    {
        return super.showMenu(rrequest,getDataExportClickEvent(rrequest,null,this.dataexporttype,null));
    }

    public String showButtonTag(ReportRequest rrequest,String dataexporttype,String exportComponentIds,String button,
            DataExportLocalStroageBean localStroageBean)
    {
        return super.showButton(rrequest,getDataExportClickEvent(rrequest,exportComponentIds,dataexporttype,localStroageBean),button);
    }

    private String getDataExportClickEvent(ReportRequest rrequest,String exportComponentIds,String dataexporttype,
            DataExportLocalStroageBean localStroageBean)
    {
        if(exportComponentIds==null||exportComponentIds.trim().equals(""))
        {
            exportComponentIds=this.getClickEvent(rrequest);//取到在<button></button>中指定的要导出的应用
        }
        if(exportComponentIds==null||exportComponentIds.trim().equals(""))
        {//如果即没有动态传入要导出的应用，也没有在<button/>中指定要导出的应用，则导出本组件
            if(this.ccbean==null) return "";
            exportComponentIds=this.ccbean.getId();
        }
        List<String> lstTmp=Tools.parseStringToList(exportComponentIds,";",false);
        List<IComponentConfigBean> lstDataExportComBeans=new ArrayList<IComponentConfigBean>();
        IComponentConfigBean ccbeanTmp;
        for(String comidTmp:lstTmp)
        {
            if(comidTmp==null||comidTmp.trim().equals("")) continue;
            if(!rrequest.checkPermission(comidTmp,Consts.BUTTON_PART,"type{"+dataexporttype+"}",Consts.PERMISSION_TYPE_DISPLAY)) continue;
            if(rrequest.checkPermission(comidTmp,Consts.BUTTON_PART,"type{"+dataexporttype+"}",Consts.PERMISSION_TYPE_DISABLED)) continue;//此组件的数据导入功能禁止使用
            if(comidTmp.equals(rrequest.getPagebean().getId()))
            {
                ccbeanTmp=rrequest.getPagebean();
            }else
            {
                ccbeanTmp=rrequest.getPagebean().getChildComponentBean(comidTmp,true);
            }
            if(ccbeanTmp==null) throw new WabacusRuntimeException("在页面"+rrequest.getPagebean().getId()+"中不存在id为"+comidTmp+"的组件，无法导出其数据");
            lstDataExportComBeans.add(ccbeanTmp);
        }
        if(lstDataExportComBeans.size()==0) return "";
        ReportBean rbean=null;
        String componentids="";
        String includeApplicationids="";
        if(lstDataExportComBeans.size()==1&&lstDataExportComBeans.get(0) instanceof ReportBean)
        {
            rbean=(ReportBean)lstDataExportComBeans.get(0);
            componentids=rbean.getId();
            if(rbean.getDataExportsBean()==null)
            {
                includeApplicationids=rbean.getId();
            }else
            {
                includeApplicationids=rbean.getDataExportsBean().getIncludeApplicationids(dataexporttype);
                if(includeApplicationids==null||includeApplicationids.trim().equals("")) includeApplicationids=rbean.getId();
            }
        }else
        {
            String includeAppidsTmp;
            for(IComponentConfigBean ccbean:lstDataExportComBeans)
            {
                componentids+=ccbean.getId()+";";
                includeAppidsTmp=null;
                if(ccbean.getDataExportsBean()!=null)
                {//配置了数据导出功能
                    includeAppidsTmp=ccbean.getDataExportsBean().getIncludeApplicationids(dataexporttype);
                }
                if(includeAppidsTmp==null||includeAppidsTmp.trim().equals(""))
                {
                    includeAppidsTmp="";
                    if(ccbean instanceof AbsContainerConfigBean)
                    {
                        List<String> lstApplicationids=((AbsContainerConfigBean)ccbean).getLstAllChildApplicationIds(true);
                        for(String appidTmp:lstApplicationids)
                        {
                            includeAppidsTmp+=appidTmp+";";
                        }
                    }else
                    {
                        includeAppidsTmp=ccbean.getId()+";";
                    }
                }
                includeApplicationids+=includeAppidsTmp;
                if(!includeApplicationids.endsWith(";")) includeApplicationids=includeApplicationids+";";
            }
        }
        return getDataExportEvent(rrequest,rbean,componentids,includeApplicationids,dataexporttype,localStroageBean);
    }

    private String getDataExportEvent(ReportRequest rrequest,ReportBean rbean,String componentids,String includeApplicationids,String exporttype,
            DataExportLocalStroageBean localStroageBean)
    {
        String clickevent=null;
        String exporturl=null;
        if(Consts.DATAEXPORT_PLAINEXCEL.equals(exporttype.toLowerCase().trim()))
        {
            exporturl=Config.showreport_onplainexcel_url;
        }else if(Consts.DATAEXPORT_WORD.equals(exporttype.toLowerCase().trim()))
        {
            exporturl=Config.showreport_onword_url;
        }else if(Consts.DATAEXPORT_PDF.equals(exporttype.toLowerCase().trim()))
        {
            exporturl=Config.showreport_onpdf_url;
        }else
        {//Consts.DATAEXPORT_RICHEXCEL
            exporturl=Config.showreport_onrichexcel_url;
        }
        if(localStroageBean==null) localStroageBean=this.localStroageBean;
        if(localStroageBean!=null)
        {
            exporturl+=(exporturl.indexOf("?")>0?"&":"?")+"dataexport_localstroage=true";
            exporturl+="&dataexport_localstroageautodelete="+localStroageBean.isAutodelete();
            exporturl+="&dataexport_localstroagedownload="+localStroageBean.isDownload();
            exporturl+="&dataexport_localstroagezip="+localStroageBean.isZip();
            try
            {
                exporturl+="&dataexport_localstroagedirectorydateformat="+URLEncoder.encode(localStroageBean.getRealDirectorydateformat(),"utf-8");
            }catch(UnsupportedEncodingException e)
            {
                e.printStackTrace();
                exporturl+="&dataexport_localstroagedirectorydateformat="+localStroageBean.getRealDirectorydateformat();
            }
        }
        if(rbean!=null&&rbean.getDbean().isDataexportColselect())
        {
            StringBuffer paramsBuf=new StringBuffer();
            paramsBuf.append("{reportguid:\"").append(rbean.getGuid()).append("\"");
            paramsBuf.append(",includeApplicationids:\"").append(includeApplicationids).append("\"");
            paramsBuf.append(",skin:\"").append(rrequest.getPageskin()).append("\"");
            paramsBuf.append(",webroot:\"").append(Config.webroot).append("\"");
            paramsBuf.append(",width:").append(rbean.getDbean().getColselectwidth());
            paramsBuf.append(",maxheight:").append(rbean.getDbean().getColselectmaxheight());
            paramsBuf.append(",showreport_onpage_url:\"").append(Config.showreport_onpage_url).append("\"");
            paramsBuf.append(",showreport_dataexport_url:\"").append(exporturl).append("\"");
            paramsBuf.append("}");
            clickevent="createTreeObjHtml(this,'"+Tools.jsParamEncode(paramsBuf.toString())+"',event);";
        }else
        {
            //clickevent="postlinkurl('"+url+"',true);";
            clickevent="exportData('"+rrequest.getPagebean().getId()+"','"+componentids+"','"+includeApplicationids+"','"
                    +Config.showreport_onpage_url+"','"+exporturl+"',null";
            clickevent+=","+(rbean==null||!rbean.getDbean().isAllColDisplaytypesEquals())+")";
        }
        return clickevent;
    }

    public void loadExtendConfig(XmlElementBean eleButtonBean)
    {
        super.loadExtendConfig(eleButtonBean);
        String dataexporttype=eleButtonBean.attributeValue("type");
        if(dataexporttype==null||dataexporttype.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+ccbean.getPath()+"上的按钮"+this.name+"失败，此按钮为数据导出按钮，必须配置其dataexporttype属性，指定本按钮的数据导出类型");
        }
        dataexporttype=dataexporttype.trim();
        if(!Consts.lstDataExportTypes.contains(dataexporttype))
        {
            throw new WabacusConfigLoadingException("加载报表"+ccbean.getPath()+"上的按钮"+this.name+"失败，为此数据导出按钮配置的dataexporttype："+dataexporttype+"无效");
        }
        this.dataexporttype=dataexporttype;
        String localstorage=eleButtonBean.attributeValue("localstorage");
        if("true".equalsIgnoreCase(localstorage))
        {
            this.localStroageBean=new DataExportLocalStroageBean();
            this.localStroageBean.setDownload(!"false".equalsIgnoreCase(eleButtonBean.attributeValue("download")));
            this.localStroageBean.setAutodelete(!"false".equalsIgnoreCase(eleButtonBean.attributeValue("autodelete")));
            this.localStroageBean.setZip("true".equalsIgnoreCase(eleButtonBean.attributeValue("zip")));
            this.localStroageBean.setDirectorydateformat(eleButtonBean.attributeValue("directorydateformat"));
        }else
        {
            this.localStroageBean=null;
        }
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        if(this.localStroageBean!=null)
        {
            this.localStroageBean.doPostLoad();
        }
    }

}
