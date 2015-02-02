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
package com.wabacus.config.component.container.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.other.JavascriptFileBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.commoninterface.IPagePersonalizePersistence;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.component.container.page.PageType;
import com.wabacus.system.intercept.AbsPageInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class PageBean extends AbsContainerConfigBean
{
    private boolean isCheckPermission;
    
    private List<String> ulstMyCss=null;

    private List<JavascriptFileBean> lstMyJavascriptFiles=null;

    private JavascriptFileBean jsFileForConfigFile;

    private String jsFilePath="";//为此页面上所有报表自动生成的javascript函数所在js文件的绝对路径（即物理路径）

    private String reportfile_key;

    private List<ReportBean> lstRelateReports;

    private Map<ReportBean,ReportBean> mRelateReports;

    //    private PageTemplate templateObj;//当前页面所用的模板对象
    
    private List<AbsPageInterceptor> lstInterceptors;

    private boolean shouldProvideEncodePageUrl;//在运行时决定本页面是否需要提供编码后的URL，目前只有当本页面通过rrequest.forwardPageWithBack()方法跳转到其它页面后需要返回时，才需要编码本页面的URL，以便返回
    
    private boolean shouldIncludeAutoCreatedJs;
    
    private IPagePersonalizePersistence personalizeObj=null;
    
    private List<AbsPrintProviderConfigBean> lstPrintBeans;
    
    public PageBean(AbsContainerConfigBean parent,String tagname)
    {//<page/>一定是最顶层的容器
        super(null,tagname);
    }

    public List<String> getUlstMyCss()
    {
        return ulstMyCss;
    }

    public List<String> getUlstSystemCss()
    {
        List<String> lstResults=new UniqueArrayList<String>();
        List<String> lstCss=Config.getInstance().getUlstGlobalCss();
        if(lstCss!=null) lstResults.addAll(lstCss);
        lstCss=Config.getInstance().getUlstLocalCss(this);
        if(lstCss!=null) lstResults.addAll(lstCss);
        return lstResults;
    }

    public List<JavascriptFileBean> getLstMyJavascriptFiles()
    {
        return this.lstMyJavascriptFiles;
    }

    public List<JavascriptFileBean> getLstSystemJavascriptFiles()
    {
        List<JavascriptFileBean> lstResult=new UniqueArrayList<JavascriptFileBean>();
        List<JavascriptFileBean> lstJsTmp=Config.getInstance().getLstDefaultGlobalJavascriptFiles();
        if(lstJsTmp!=null) lstResult.addAll(lstJsTmp);
        lstJsTmp=Config.getInstance().getLstGlobalJavascriptFiles();
        if(lstJsTmp!=null) lstResult.addAll(lstJsTmp);
        lstJsTmp=Config.getInstance().getLstLocalJavascript(this);
        if(lstJsTmp!=null) lstResult.addAll(lstJsTmp);
        if(shouldIncludeAutoCreatedJs&&this.jsFileForConfigFile!=null) lstResult.add(this.jsFileForConfigFile);
        return lstResult;
    }

    public void addMyCss(String css)
    {
        if(ulstMyCss==null) ulstMyCss=new UniqueArrayList<String>();
        ulstMyCss.add(css);
    }

    public void addMyCss(List<String> lstcss)
    {
        if(ulstMyCss==null) ulstMyCss=new UniqueArrayList<String>();
        ulstMyCss.addAll(lstcss);
    }

    public void addMyJavascriptFile(String jsfileUrl,int loadorder)
    {
        if(this.lstMyJavascriptFiles==null) this.lstMyJavascriptFiles=new UniqueArrayList<JavascriptFileBean>();
        this.lstMyJavascriptFiles.add(new JavascriptFileBean(jsfileUrl,loadorder));
    }
    
    public void setShouldIncludeAutoCreatedJs(boolean shouldIncludeAutoCreatedJs)
    {
        this.shouldIncludeAutoCreatedJs=shouldIncludeAutoCreatedJs;
    }

    public boolean isCheckPermission()
    {
        return isCheckPermission;
    }

    public void setCheckPermission(boolean isCheckPermission)
    {
        this.isCheckPermission=isCheckPermission;
    }

    public void setJsFileUrl(String jsFileUrl)
    {
        if(jsFileUrl==null||jsFileUrl.trim().equals(""))
        {
            this.jsFileForConfigFile=null;
        }else
        {
            this.jsFileForConfigFile=new JavascriptFileBean(jsFileUrl.trim(),0);
        }
    }

    public String getJsFilePath()
    {
        return this.jsFilePath;
    }

    public void setJsFilePath(String jsFilePath)
    {
        this.jsFilePath=jsFilePath;
    }

    public String getReportfile_key()
    {
        return reportfile_key;
    }

    public void setReportfile_key(String reportfile_key)
    {
        this.reportfile_key=reportfile_key;
    }

    public void addInterceptor(AbsPageInterceptor interceptor)
    {
        if(interceptor==null) return;
        if(this.lstInterceptors==null) this.lstInterceptors=new ArrayList<AbsPageInterceptor>();
        this.lstInterceptors.add(interceptor);
    }
    
    public List<AbsPageInterceptor> getLstInterceptors()
    {
        if(lstInterceptors==null) return new ArrayList<AbsPageInterceptor>();
        return lstInterceptors;
    }

    public void setLstRelateReports(List<ReportBean> lstRelateReports)
    {
        this.lstRelateReports=lstRelateReports;
    }

    public void setMRelateReports(Map<ReportBean,ReportBean> relateReports)
    {
        mRelateReports=relateReports;
    }

    public void addRelateReports(ReportBean rbean)
    {
        if(lstRelateReports==null)
        {
            lstRelateReports=new ArrayList<ReportBean>();
        }
        lstRelateReports.add(rbean);
    }

    public Map<ReportBean,ReportBean> getMRelateReports()
    {
        return mRelateReports;
    }

    public ReportBean getDependParentReportBean(ReportBean rbean)
    {
        if(this.mRelateReports==null) return null;
        return this.mRelateReports.get(rbean);
    }

    public boolean isShouldProvideEncodePageUrl()
    {
        return shouldProvideEncodePageUrl;
    }

    public void setShouldProvideEncodePageUrl(boolean shouldProvideEncodePageUrl)
    {
        this.shouldProvideEncodePageUrl=shouldProvideEncodePageUrl;
    }

    public IPagePersonalizePersistence getPersonalizeObj()
    {
        return personalizeObj;
    }

    public void setPersonalizeObj(IPagePersonalizePersistence personalizeObj)
    {
        this.personalizeObj=personalizeObj;
    }

    public List<AbsPrintProviderConfigBean> getLstPrintBeans()
    {
        return lstPrintBeans;
    }

    public void addPrintBean(AbsPrintProviderConfigBean printBean)
    {
        if(this.lstPrintBeans==null) this.lstPrintBeans=new ArrayList<AbsPrintProviderConfigBean>();
        this.lstPrintBeans.add(printBean);
    }

    public ReportBean getSlaveReportBean(String reportid)
    {
        if(reportid==null||reportid.equals("")) return null;
        if(this.mRelateReports==null||this.mRelateReports.size()==0) return null;
        for(ReportBean rbean:this.mRelateReports.keySet())
        {
            if(reportid.equals(rbean.getId()))
            {
                return rbean;
            }
        }
        return null;
    }

    public PageBean clone(AbsContainerConfigBean parent)
    {
        PageBean pbNew=(PageBean)super.clone(parent);
        if(this.lstRelateReports!=null)
        {
            pbNew.setLstRelateReports(new ArrayList<ReportBean>());
        }
        if(this.mRelateReports!=null)
        {
            pbNew.setMRelateReports(new HashMap<ReportBean,ReportBean>());
        }
        return pbNew;
    }

    public void doPostLoad()
    {
        this.refreshid=this.id;
        processRelateReports();
        processRelateConditions();
        if(ulstMyCss!=null&&ulstMyCss.size()==0) ulstMyCss=null;
        if(this.lstMyJavascriptFiles!=null&&lstMyJavascriptFiles.size()==0) lstMyJavascriptFiles=null;
        super.doPostLoad();
    }
    
    private void processRelateReports()
    {
        if(this.lstRelateReports==null||this.lstRelateReports.size()==0) return;
        this.mRelateReports=new HashMap<ReportBean,ReportBean>();
        for(ReportBean rbeanSlave:lstRelateReports)
        {
            ReportBean rbeanMaster=this.getReportChild(rbeanSlave.getDependParentId(),true);
            if(rbeanMaster==null)
            {
                throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，其下的报表"
                        +rbeanSlave.getId()+"依赖的报表"+rbeanSlave.getDependParentId()+"不存在");
            }
            if(rbeanMaster.isListReportType())
            {//如果主报表是列表报表，则子报表不能配置refreshid为其它容器，因为它们是在客户端单独加载的，不能和其它报表绑定加载
                if(rbeanSlave.getRefreshid()!=null&&!rbeanSlave.getRefreshid().trim().equals("")
                        &&!rbeanSlave.getRefreshid().trim().equals(rbeanSlave.getId()))
                {
                    throw new WabacusConfigLoadingException("报表"+rbeanSlave.getPath()+"失败，它是依赖其它报表的从报表，因此不能为它配置refreshid属性");
                }
            }
            processDependParams(rbeanSlave,rbeanMaster);
            if(rbeanMaster.isListReportType())
            {
                AbsListReportBean alrbean=(AbsListReportBean)rbeanMaster.getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(alrbean.getRowSelectType()==null||alrbean.getRowSelectType().trim().equals("")
                        ||alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_NONE))
                {
                    alrbean.setRowSelectType(Consts.ROWSELECT_SINGLE);
                }
            }
            this.mRelateReports.put(rbeanSlave,rbeanMaster);
        }
        validRelateReports();
        ReportBean rbeanTempMaster;
        AbsListReportBean alrbean;
        List<ReportBean> lstCreatedListReportBeans=new ArrayList<ReportBean>();
        for(Entry<ReportBean,ReportBean> rbeanEntries:this.mRelateReports.entrySet())
        {
            rbeanTempMaster=rbeanEntries.getValue();
            if(rbeanTempMaster.isListReportType())
            {
                if(lstCreatedListReportBeans.contains(rbeanTempMaster)) continue;//已经为此报表生成过js
                lstCreatedListReportBeans.add(rbeanTempMaster);
                alrbean=(AbsListReportBean)rbeanTempMaster.getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(alrbean==null)
                {
                    alrbean=new AbsListReportBean(rbeanTempMaster);
                    rbeanTempMaster.setExtendConfigDataForReportType(AbsListReportType.KEY,alrbean);
                }

                alrbean.addRowSelectCallBackFunc(rbeanTempMaster.getRefreshSlaveReportsCallBackMethodName());
                rbeanTempMaster.addOnloadMethod(new OnloadMethodBean(Consts_Private.ONLOAD_REFRESHSLAVE,rbeanTempMaster
                        .getRefreshSlaveReportsCallBackMethodName()));
            }else
            {
                String masterReportRefreshId=rbeanTempMaster.getRefreshid();
                if(masterReportRefreshId==null||masterReportRefreshId.trim().equals(""))
                {
                    masterReportRefreshId=rbeanTempMaster.getId();
                }
                rbeanTempMaster.setRefreshid(getParentContainerObjOfComponents(masterReportRefreshId,rbeanEntries.getKey().getId()).getId());
            }
        }
        for(ReportBean rbTmp:lstCreatedListReportBeans)
        {
            JavaScriptAssistant.getInstance().createRefreshSlaveReportsDataScript(rbTmp);
        }
        this.lstRelateReports=null;
    }

    private void processDependParams(ReportBean rbeanSlave,ReportBean rbeanMaster)
    {
        Map<String,Map<String,String>> mDependParams=rbeanMaster.getMDependChilds();
        if(mDependParams==null)
        {
            mDependParams=new HashMap<String,Map<String,String>>();
            rbeanMaster.setMDependChilds(mDependParams);
        }
        if(rbeanSlave.getDependparams()==null||rbeanSlave.getDependparams().trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，其下的报表"
                    +rbeanSlave.getId()+"依赖其它报表，但不存在依赖的动态参数，无法完成依赖操作");
        }
        List<String> lstParams=Tools.parseStringToList(rbeanSlave.getDependparams(),";",false);
        List<String> lstTemp;
        Map<String,String> mParams=new HashMap<String,String>();
        mDependParams.put(rbeanSlave.getId(),mParams);
        if(rbeanMaster.isDetailReportType()) rbeanSlave.setMDependsDetailReportParams(mParams);//如果主报表是细览报表，则存放其所用到的参数，以便加载此从报表数据时可以知道要取哪些参数
        for(String param:lstParams)
        {
            lstTemp=Tools.parseStringToList(param,"=",false);
            String paramname=lstTemp.get(0).trim();
            String paramvalue="";
            if(lstTemp.size()==2)
            {
                paramvalue=lstTemp.get(1).trim();
            }else if(lstTemp.size()>2)
            {
                throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，其下报表"
                        +rbeanSlave.getId()+"配置的dependsparams:"+rbeanSlave.getDependparams()+"不合法，出现多个=号");
            }
            if(paramname.equals(""))
            {//每个从报表的动态参数必须定义成@{<col/>的property}格式
                throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，其下报表"+rbeanSlave.getId()+"配置的dependsparams:"
                        +rbeanSlave.getDependparams()+"不合法，参数名不能为空");
            }
            ConditionBean conbeanTmp=rbeanSlave.getSbean().getConditionBeanByName(paramname);
            if(conbeanTmp!=null)
            {
                if(!conbeanTmp.isConditionValueFromUrl())
                {
                    throw new WabacusConfigLoadingException("加载从报表"+rbeanSlave.getPath()+"失败，在dependsparams中配置的"+paramname
                            +"对应的查询条件<condition/>不是从URL中获取条件数据");
                }
            }
            mParams.put(paramname,paramvalue);
            if(Tools.isDefineKey("@",paramvalue))
            {
                paramvalue=Tools.getRealKeyByDefine("@",paramvalue);//父报表的某个<col/>的property
                ColBean cb=rbeanMaster.getDbean().getColBeanByColProperty(paramvalue);
                if(cb==null)
                {
                    throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，其下报表"
                            +rbeanSlave.getId()+"配置的dependsparams:"+rbeanSlave.getDependparams()+"中动态参数"
                            +paramvalue+"在被依赖的父报表中不存在property为此值的<col/>");
                }
                if(rbeanMaster.isListReportType())
                {
                    AbsListReportColBean alrcean=(AbsListReportColBean)cb.getExtendConfigDataForReportType(AbsListReportType.KEY);
                    if(alrcean==null)
                    {
                        alrcean=new AbsListReportColBean(cb);
                        cb.setExtendConfigDataForReportType(AbsListReportType.KEY,alrcean);
                    }
                    alrcean.setSlaveReportParamName(paramname);//将此ColBean标识为参与刷新从报表的参数，并记下对应的参数名
                    rbeanSlave.addParamNameFromURL(paramname);
                    cb.setDisplayNameValueProperty(true);
                }
            }
        }
    }

    private void validRelateReports()
    {
        List<ReportBean> lstValidedMasterReport=new ArrayList<ReportBean>();
        List<String> lstDetailReportidDependingListReports=new ArrayList<String>();
        for(ReportBean rbeanSlave:this.mRelateReports.keySet())
        {
            if(rbeanSlave==null||!rbeanSlave.isSlaveReport()) continue;
            ReportBean rbeanMaster=this.mRelateReports.get(rbeanSlave);
            if(!lstValidedMasterReport.contains(rbeanMaster))
            {
                Map<String,Map<String,String>> mDependChildsTmp=rbeanMaster.getMDependChilds();
                Map<String,String> mDynParamsValueAndName=new HashMap<String,String>();
                for(Entry<String,Map<String,String>> entryTmp:mDependChildsTmp.entrySet())
                {//循环所有依赖此主报表的从报表
                    Map<String,String> mParams=entryTmp.getValue();
                    if(mParams.size()==0)
                    {
                        throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，依赖主报表"+rbeanMaster.getPath()+"的从报表"+entryTmp.getKey()
                                +"没有配置dependsparams参数");
                    }
                    boolean existDynParams=false;
                    for(Entry<String,String> entryParams:mParams.entrySet())
                    {
                        if(Tools.isDefineKey("@",entryParams.getValue()))
                        {
                            existDynParams=true;
                            if(mDynParamsValueAndName.containsKey(entryParams.getValue())
                                    &&!entryParams.getKey().equals(mDynParamsValueAndName.get(entryParams.getValue())))
                            {
                                throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，依赖主报表"+rbeanMaster.getPath()+"的从报表存在多个参数名从本主报表的列"
                                        +entryParams.getValue()+"中取参数值，必须保持相同的参数名");
                            }
                            mDynParamsValueAndName.put(entryParams.getValue(),entryParams.getKey());
                        }
                    }
                    if(!existDynParams)
                    {
                        throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，依赖主报表"+rbeanMaster.getPath()+"的从报表"+entryTmp.getKey()
                                +"在dependsparams中没有配置动态参数");
                    }
                }
                lstValidedMasterReport.add(rbeanMaster);
            }
            if(rbeanMaster.isListReportType()&&rbeanSlave.isDetailReportType())
            {
                lstDetailReportidDependingListReports.add(rbeanSlave.getId());
            }
            while(rbeanMaster!=null)
            {
                if(rbeanMaster.getId().equals(rbeanSlave.getId()))
                {//存在循环依赖
                    throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，其下报表存在循环依赖");
                }
                rbeanMaster=this.mRelateReports.get(rbeanMaster);
            }
        }
        for(ReportBean rbeanSlave:this.mRelateReports.keySet())
        {
            if(rbeanSlave==null||!rbeanSlave.isSlaveReport()) continue;
            ReportBean rbeanMaster=this.mRelateReports.get(rbeanSlave);
            if(lstDetailReportidDependingListReports.contains(rbeanMaster.getId()))
            {
                throw new WabacusConfigLoadingException("加载页面"+this.getPath()+"失败，细览报表"+rbeanMaster.getPath()+"依赖于数据自动列表报表，因此不能再被其它报表依赖");
            }
        }
    }
    
    private void processRelateConditions()
    {
        List<String> lstAllConditionNames=new ArrayList<String>();
        Set<String> sRelateConditionNames=new HashSet<String>();
        getAllRelateConditions(this,lstAllConditionNames,sRelateConditionNames);
        if(sRelateConditionNames.size()==0) return;
        Map<String,List<ReportBean>> mRelateConReportBeans=new HashMap<String,List<ReportBean>>();
        getAllRelateConditionReportBeans(this,sRelateConditionNames,mRelateConReportBeans);
        for(Entry<String,List<ReportBean>> entryTmp:mRelateConReportBeans.entrySet())
        {//处理每个关联查询条件对应的报表
            updateRefreshContaineridForReportBeans(entryTmp.getValue());
            updateRelateReportidsForReportBeans(entryTmp.getValue());
        }
    }

    private void getAllRelateConditions(AbsContainerConfigBean containerBean,
            List<String> lstAllConditionNames,Set<String> sRelateConditionNames)
    {
        List<String> lstConNames;
        for(Entry<String,IComponentConfigBean> entryTmp:containerBean.getMChildren().entrySet())
        {
            if(entryTmp.getValue() instanceof ReportBean)
            {
                ReportBean rbeanTmp=(ReportBean)entryTmp.getValue();
                if(rbeanTmp.getSbean()==null||rbeanTmp.getSbean().getLstConditions()==null)
                    continue;
                List<ConditionBean> lstConditions=rbeanTmp.getSbean().getLstConditions();
                lstConNames=new ArrayList<String>();
                for(ConditionBean cbean:lstConditions)
                {
                    if(cbean==null) continue;
                    if(lstConNames.contains(cbean.getName()))
                    {
                        throw new WabacusConfigLoadingException("报表"+rbeanTmp.getPath()+"中存在多个name属性为"+cbean.getName()
                                +"的查询条件，同一个报表下的所有<condition/>的name属性必须唯一");
                    }
                    lstConNames.add(cbean.getName());
                    if(!cbean.isConditionValueFromUrl()) continue;
                    if(lstAllConditionNames.contains(cbean.getName()))
                    {
                        sRelateConditionNames.add(cbean.getName());
                    }else
                    {
                        lstAllConditionNames.add(cbean.getName());
                    }
                }
            }else if(entryTmp.getValue() instanceof AbsContainerConfigBean)
            {
                getAllRelateConditions((AbsContainerConfigBean)entryTmp.getValue(),
                        lstAllConditionNames,sRelateConditionNames);
            }
        }
    }

    private void getAllRelateConditionReportBeans(AbsContainerConfigBean containerBean,
            Set<String> sRelateConditionNames,Map<String,List<ReportBean>> mRelateConReportBeans)
    {
        IComponentConfigBean childBeanTmp;
        for(Entry<String,IComponentConfigBean> entryTmp:containerBean.getMChildren().entrySet())
        {
            childBeanTmp=entryTmp.getValue();
            if(childBeanTmp instanceof ReportBean)
            {
                for(String relateconname:sRelateConditionNames)
                {
                    ReportBean rbeanTmp=(ReportBean)entryTmp.getValue();
                    if(rbeanTmp.getSbean()==null||rbeanTmp.getSbean().getLstConditions()==null) continue;
                    if(rbeanTmp.isSlaveReportDependsonListReport()) continue;//从报表不存在查询条件关联的情况
                    List<ConditionBean> lstConditions=rbeanTmp.getSbean().getLstConditions();
                    for(ConditionBean cbean:lstConditions)
                    {
                        if(cbean==null||!cbean.isConditionValueFromUrl()) continue;
                        if(relateconname.equals(cbean.getName()))
                        {
                            List<ReportBean> lst=mRelateConReportBeans.get(relateconname);
                            if(lst==null)
                            {
                                lst=new ArrayList<ReportBean>();
                                mRelateConReportBeans.put(relateconname,lst);
                            }
                            lst.add(rbeanTmp);
                        }
                    }
                }
            }else if(childBeanTmp instanceof AbsContainerConfigBean)
            {
                getAllRelateConditionReportBeans((AbsContainerConfigBean)childBeanTmp,sRelateConditionNames,mRelateConReportBeans);
            }
        }
    }

    private void updateRefreshContaineridForReportBeans(List<ReportBean> lstReportBeans)
    {
        if(lstReportBeans==null||lstReportBeans.size()<2) return;
        ReportBean rbean=lstReportBeans.get(0);
        String containerid=null;
        AbsContainerConfigBean parentContainer=rbean.getParentContainer();
        while(parentContainer!=null)
        {
            int i=1;
            for(;i<lstReportBeans.size();i++)
            {
                if(parentContainer.getReportChild(lstReportBeans.get(i).getId(),true)==null)
                {
                    parentContainer=parentContainer.getParentContainer();
                    break;
                }
            }
            if(i==lstReportBeans.size())
            {
                containerid=parentContainer.getId();
                break;
            }
        }
        for(ReportBean rbeanTmp:lstReportBeans)
        {
            if(rbeanTmp.getRefreshid()==null||rbeanTmp.getRefreshid().trim().equals(""))
            {
                rbeanTmp.setRefreshid(containerid);
            }else
            {
                rbeanTmp.setRefreshid(getCommonRefreshIdOfComponents(containerid,rbeanTmp.getRefreshid()));
            }
        }
    }

    public String getCommonRefreshIdOfComponents(String componentId1,String componentId2)
    {
        componentId1=componentId1==null?"":componentId1.trim();
        componentId2=componentId2==null?"":componentId2.trim();
        if(componentId2.equals("")||componentId1.equals("")) return null;
        if(componentId1.equals(componentId2))
        {//两个id是同一个组件的ID，则refreshid就是它们的id
            if(!this.id.equals(componentId1)&&getChildComponentBean(componentId1,true)==null)
            {
                throw new WabacusConfigLoadingException("页面"+this.id+"下面不存在id属性为"+componentId1+"的子组件");
            }
            return componentId1;
        }
        AbsContainerConfigBean commonParentContainerObj=getParentContainerObjOfComponents(componentId1,componentId2);
        if(commonParentContainerObj==null) return null;
        return commonParentContainerObj.getId();
    }
    
    public AbsContainerConfigBean getParentContainerObjOfComponents(String componentId1,String componentId2)
    {
        componentId1=componentId1==null?"":componentId1.trim();
        componentId2=componentId2==null?"":componentId2.trim();
        if(componentId1.equals("")||componentId2.equals("")) return null;
        if(id.equals(componentId1)&&id.equals(componentId2))
        {//两个都是<page/>的id
            return this;
        }
        IComponentConfigBean obj1=null;
        IComponentConfigBean obj2=null;
        if(id.equals(componentId1))
        {
            obj1=this;
        }else
        {
            obj1=this.getChildComponentBean(componentId1,true);
        }
        if(id.equals(componentId2))
        {
            obj2=this;
        }else
        {
            obj2=this.getChildComponentBean(componentId2,true);
        }
        if(obj1==null)
        {
            throw new WabacusConfigLoadingException("页面"+this.id+"下面不存在id属性为"+componentId1+"的子组件");
        }
        if(obj2==null)
        {
            throw new WabacusConfigLoadingException("页面"+this.id+"下面不存在id属性为"+componentId2+"的子组件");
        }
        if(componentId1.equals(componentId2))
        {
            if(obj1 instanceof AbsContainerConfigBean)
            {
                return (AbsContainerConfigBean)obj1;
            }else
            {
                return ((IComponentConfigBean)obj1).getParentContainer();
            }
        }
        if(obj1 instanceof AbsContainerConfigBean)
        {
            if(((AbsContainerConfigBean)obj1).getChildComponentBean(componentId2,true)!=null)
            {
                return (AbsContainerConfigBean)obj1;
            }
        }
        if(obj2 instanceof AbsContainerConfigBean)
        {
            if(((AbsContainerConfigBean)obj2).getChildComponentBean(componentId1,true)!=null)
            {
                return (AbsContainerConfigBean)obj2;
            }
        }
        AbsContainerConfigBean parentContainer1=((IComponentConfigBean)obj1).getParentContainer();
        while(parentContainer1!=null)
        {
            if(parentContainer1.getChildComponentBean(componentId2,true)!=null)
            {
                return parentContainer1;
            }
            parentContainer1=parentContainer1.getParentContainer();
        }
        return null;
    }

    private void updateRelateReportidsForReportBeans(List<ReportBean> lstReportBeans)
    {
        for(ReportBean rbeanTmp:lstReportBeans)
        {
            for(ReportBean rbeanTmp2:lstReportBeans)
            {
                rbeanTmp.addRelateConditionReportid(rbeanTmp2.getId());
            }
        }
    }
    
    public IComponentType createComponentTypeObj(ReportRequest rrequest,AbsContainerType parentContainer)
    {
        return new PageType(parentContainer,this,rrequest);
    }
}
