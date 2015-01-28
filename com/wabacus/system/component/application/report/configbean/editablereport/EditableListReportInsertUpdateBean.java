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
package com.wabacus.system.component.application.report.configbean.editablereport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.EditableDetailReportType;
import com.wabacus.system.component.application.report.EditableListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class EditableListReportInsertUpdateBean implements Cloneable
{
    private String pageurl;//如果编辑页面是用户自己开发的jsp/servlet，则这里指定它们的访问URL

    private String pageid;//如果编辑页面是配置的另一个报表，则这里存放编辑报表所在的<page/>的id

    private String reportid;//如果编辑页面是配置的另一个报表，则这里存放编辑报表所在的<report/>的id

    private List<Map<String,String>> lstUrlParams;

    private String pageinitsize;//初始大小，可配置值包括min/max/normal，分别表示最大化、最小化、正常窗口大小（即上面pagewidth/pageheight配置的大小）

    private String popupparams;

    private String beforepopup;
    
    private AbsEditableReportEditDataBean owner;

    public EditableListReportInsertUpdateBean(AbsEditableReportEditDataBean owner)
    {
        this.owner=owner;
    }

    public String getPageurl()
    {
        return pageurl;
    }

    public void setPageurl(String pageurl)
    {
        this.pageurl=pageurl;
    }

    public String getPageid()
    {
        return pageid;
    }

    public void setPageid(String pageid)
    {
        this.pageid=pageid;
    }

    public String getReportid()
    {
        return reportid;
    }

    public void setReportid(String reportid)
    {
        this.reportid=reportid;
    }

    public List<Map<String,String>> getLstUrlParams()
    {
        return lstUrlParams;
    }

    public void setLstUrlParams(List<Map<String,String>> lstUrlParams)
    {
        this.lstUrlParams=lstUrlParams;
    }

    public String getPageinitsize()
    {
        return pageinitsize;
    }

    public void setPageinitsize(String pageinitsize)
    {
        this.pageinitsize=pageinitsize;
    }

    public String getPopupparams()
    {
        return popupparams;
    }

    public void setPopupparams(String popupparams)
    {
        this.popupparams=popupparams;
    }

    public String getBeforepopup()
    {
        return beforepopup;
    }

    public void setBeforepopup(String beforepopup)
    {
        this.beforepopup=beforepopup;
    }

    public AbsEditableReportEditDataBean getOwner()
    {
        return owner;
    }

    public void setOwner(AbsEditableReportEditDataBean owner)
    {
        this.owner=owner;
    }

    public boolean isBelongtoInsert()
    {
        return this.owner instanceof EditableListReportInsertDataBean;
    }
    
    public boolean isBelongtoUpdate()
    {
        return this.owner instanceof EditableListReportUpdateDataBean;
    }
    
    public String getPopupPageUrlAndParams(ReportRequest rrequest,EditableListReportType reportTypeObj,AbsReportDataPojo dataObj)
    {
        StringBuilder resultBuf=new StringBuilder();
        ReportBean rbean=this.getOwner().getOwner().getReportBean();
        boolean isBelongtoInsert=isBelongtoInsert();
        String pageurl=null;
        if(this.pageurl!=null&&!this.pageurl.trim().equals(""))
        {
            pageurl=this.pageurl;
        }else
        {
            pageurl=Config.showreport_onpage_url+"&PAGEID="+this.pageid;
            pageurl=pageurl+"&WX_REFEREDREPORTID="+this.reportid;
            pageurl=pageurl+"&"+this.reportid+"_ACCESSMODE="+(isBelongtoInsert?Consts.ADD_MODE:Consts.UPDATE_MODE);
        }
        pageurl=pageurl+"&WX_SRCPAGEID="+rbean.getPageBean().getId();
        pageurl=pageurl+"&WX_SRCREPORTID="+rbean.getId();
        pageurl=pageurl+"&WX_EDITTYPE="+(isBelongtoInsert?"add":"update");
        resultBuf.append("{pageurl:\"").append(pageurl).append("\"");
        StringBuffer urlParamsBuf=new StringBuffer();
        if(this.lstUrlParams!=null&&this.lstUrlParams.size()>0)
        {
            String paramnameTmp;
            String paramvalueTmp;
            for(Map<String,String> mParamTmp:this.lstUrlParams)
            {
                paramnameTmp=mParamTmp.keySet().iterator().next();
                paramvalueTmp=mParamTmp.get(paramnameTmp);
                if(paramnameTmp==null||paramnameTmp.trim().equals("")||paramvalueTmp==null||paramvalueTmp.trim().equals("")) continue;
                if(Tools.isDefineKey("@",paramvalueTmp))
                {
                    if(isBelongtoInsert) continue;//如果是新增数据，因此不可能取到某列上的数据，这种参数一律为空
                    paramvalueTmp=Tools.getRealKeyByDefine("@",paramvalueTmp);
                    if(paramvalueTmp.trim().equals("")) continue;
                    ColBean cbTmp=rbean.getDbean().getColBeanByColProperty(paramvalueTmp);
                    if(cbTmp==null)
                    {
                        throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，为其配置的urlparams属性中指定的@{"+paramvalueTmp+"}没有对应的<col/>");
                    }
                    paramvalueTmp=reportTypeObj.getColOriginalValue(dataObj,cbTmp);
                }else if(WabacusAssistant.getInstance().isGetRequestContextValue(paramvalueTmp))
                {//是从request/session中取数据
                    paramvalueTmp=WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,paramvalueTmp,null);
                }
                if(paramvalueTmp==null||paramvalueTmp.trim().equals("")) continue;
                urlParamsBuf.append(paramnameTmp).append(":\"").append(paramvalueTmp).append("\",");
            }
            if(urlParamsBuf.length()>0)
            {
                if(urlParamsBuf.charAt(urlParamsBuf.length()-1)==',') urlParamsBuf.deleteCharAt(urlParamsBuf.length()-1);
                resultBuf.append(",params:{").append(urlParamsBuf.toString()).append("}");
            }
        }
        if(!Tools.isEmpty(this.beforepopup)) resultBuf.append(",beforePopupMethod:").append(this.beforepopup);
        resultBuf.append("}");
        return Tools.jsParamEncode(resultBuf.toString());
    }
    
    public void doPostLoadFinally()
    {
        ReportBean rbean=this.getOwner().getOwner().getReportBean();
        this.pageid=this.pageid==null?"":this.pageid.trim();
        this.reportid=this.reportid==null?"":this.reportid.trim();
        if(!this.pageid.equals("")&&!this.reportid.equals(""))
        {
            PageBean pagebean=Config.getInstance().getPageBean(this.pageid);
            if(pagebean==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在pageurl中指定的pageid"+this.pageid+"不存在");
            }
            IComponentConfigBean ccbean=pagebean.getChildComponentBean(this.reportid,true);
            if(ccbean==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在pageurl中指定的pageid"+this.pageid+"中不存在id为"+this.reportid+"的报表");
            }
            if(!(ccbean instanceof ReportBean))
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在pageurl中指定的pageid"+this.pageid+"中id为"+this.reportid+"子组件不是报表");
            }
            ReportBean rbTmp=(ReportBean)ccbean;
            AbsReportType reportTypeObj=Config.getInstance().getReportType(rbTmp.getType());
            if(!(reportTypeObj instanceof EditableDetailReportType))
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在pageurl中指定的pageid"+this.pageid+"中id为"+this.reportid
                        +"报表不是editabledetail和form报表类型");
            }
            EditableReportSqlBean ersbeanTmp=(EditableReportSqlBean)rbTmp.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
            if(ersbeanTmp==null||(this.isBelongtoInsert()&&ersbeanTmp.getInsertbean()==null)
                    ||(this.isBelongtoUpdate()&&ersbeanTmp.getUpdatebean()==null))
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在pageurl中指定的pageid"+this.pageid+"中id为"+this.reportid
                        +"报表没有配置相应的编辑功能");
            }
        }else if(this.pageurl==null||this.pageurl.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，必须为其<insert/>和<update/>指定pageurl属性");
        }
        if(this.isBelongtoUpdate()&&(this.lstUrlParams==null||this.lstUrlParams.size()==0))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<update/>中必须通过urlparams参数配置进入编辑报表的参数");
        }
        popupparams=WabacusAssistant.getInstance().addDefaultPopupParams(popupparams,this.pageinitsize,"800","600","closePopUpPageEvent");
    }
    
    public Object clone(AbsEditableReportEditDataBean newowner)
    {
        try
        {
            EditableListReportInsertUpdateBean newbean=(EditableListReportInsertUpdateBean)super.clone();
            newbean.setOwner(newowner);
            if(this.lstUrlParams!=null)
            {
                List<Map<String,String>> lstUrlParamsNew=new ArrayList<Map<String,String>>();
                for(Map<String,String> mUrlParamTmp:this.lstUrlParams)
                {
                    lstUrlParamsNew.add((Map<String,String>)((HashMap<String,String>)mUrlParamTmp).clone());
                }
                newbean.setLstUrlParams(lstUrlParamsNew);
            }
            return newbean;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
