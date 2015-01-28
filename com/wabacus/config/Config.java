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
package com.wabacus.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.database.datasource.AbsDataSource;
import com.wabacus.config.other.JavascriptFileBean;
import com.wabacus.config.resource.Resources;
import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.AuthorizationAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.commoninterface.IListReportRoworderPersistence;
import com.wabacus.system.commoninterface.IPagePersonalizePersistence;
import com.wabacus.system.commoninterface.IReportPersonalizePersistence;
import com.wabacus.system.component.AbsComponentType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IReportType;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.dataimport.thread.TimingDataImportTask;
import com.wabacus.system.dataset.select.common.AbsCommonDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.AbsReportDataSetValueProvider;
import com.wabacus.system.dataset.update.AbsUpdateActionProvider;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.intercept.AbsPageInterceptor;
import com.wabacus.system.permission.ComponentPermissionBean;
import com.wabacus.system.task.TimingThread;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;
import com.wabacus.util.UniqueArrayList;

public class Config
{
    private static Log log=LogFactory.getLog(Config.class);
    
    public static String homeAbsPath;//通过getRealPath("/")获取的绝对路径
    
    public static String webroot;//应用的URL根路径，比如/Wabacus/
    
    public static String webroot_abspath="";

    public static boolean should_createjs=true;

    public static boolean show_sql=false;

    public static String configpath;

    public static String skin;

    public static String showreport_url;//显示报表的URL

    public static String showreport_onpage_url;

    public static String showreport_onrichexcel_url;

    public static String showreport_onplainexcel_url;

    public static String showreport_onword_url;
    
    public static String showreport_onpdf_url;//显示报表到pdf上的URL

    public static String encode;

    public static String default_errorpromptparams_onblur;
    
    public static IListReportRoworderPersistence default_roworder_object;
    
    public static IPagePersonalizePersistence default_pagepersonalize_object;
    
    public static IReportPersonalizePersistence default_reportpersonalize_object;//在wabacus.cfg.xml中配置的全局默认持久化报表、表单个性化信息的对象
    
    public static String i18n_filename;

    public TemplateBean report_template_defaultbean;
    
    public TemplateBean dataexport_template_defaultbean;
    
    public TemplateBean print_template_defaultbean;
    
    private List<Class> lstServerValidateClasses;

    private List<Class> lstFormatClasses;//用户配置的所有格式化类

    private Map<String,PageBean> mReportStructureInfo; 

    private Map<String,String> mSystemConfig;

    private Map<String,AbsContainerType> mContainertypes;

    private Map<String,IReportType> mReporttypes;

    private List<String> ulstGlobalCss;//存放整个项目的报表页面都需要包含的css文件路径

    private Map<String,List<String>> mLocalCss;

    private List<JavascriptFileBean> lstDefaultGlobalJavascriptFiles;
    
    private List<JavascriptFileBean> lstGlobalJavascriptFiles;

    private Map<String,List<JavascriptFileBean>> mLocalJavascriptFiles;

    private Map<String,AbsInputBox> mInputBoxTypes;//存放注册的所有输入框类型

    private Map<String,IDataType> mDataTypes;

    private Map<String,AbsReportDataSetValueProvider> mReportDatasetValueProviders;
    
    private Map<String,AbsCommonDataSetValueProvider> mCommonDatasetValueProviders;
    
    private Map<String,AbsUpdateActionProvider> mUpdateDatasetActionProviders;
    
    //    private Map<String,Class> mClasses;//为那些用CommonDataBean存放中间数据的报表生成的子类字节码集合，只有当将生成的字节码配置成缓存在内存中，才会存在这里

    private Resources resources;//存放项目中所有资源项

    private String default_datasourcename;
    
    private Map<String,AbsDataSource> mDataSources;
    
    private Map<String,TemplateBean> mFileTemplates;//所有存放在html/htm文件中的模板
    
    private List<AbsPageInterceptor> lstGlobalPageInterceptors;

    private Map<String,List<AbsDataImportConfigBean>> mAutoDetectedDataImportBeans;
    
    private Map<String,ComponentPermissionBean> mComponentsDefaultPermissions;
    
    private Map<String,Map<String,String>> mSkinConfigProperties;
    
    private int dataexport_plainexcel_sheetsize=Integer.MIN_VALUE;//导出到plainexcel时，一个sheet显示的记录数，超过了则自动新增sheet
    
    private final static Config INSTANCE=new Config();

    private Config()
    {}

    public static Config getInstance()
    {
        return INSTANCE;
    }

    void setMReportStructureInfo(Map<String,PageBean> reportStructureInfo)
    {
        mReportStructureInfo=reportStructureInfo;
    }

    public Map<String,PageBean> getMAllPageBeans()
    {
        return this.mReportStructureInfo;
    }
    
    void setMReporttypes(Map<String,IReportType> reporttypes)
    {
        mReporttypes=reporttypes;
    }

    Map<String,AbsContainerType> getMContainertypes()
    {
        return mContainertypes;
    }

    void setMContainertypes(Map<String,AbsContainerType> containertypes)
    {
        mContainertypes=containertypes;
    }

    Map<String,IReportType> getMReporttypes()
    {
        return mReporttypes;
    }

    public Map<String,AbsDataSource> getMDataSources()
    {
        return mDataSources;
    }

    void setMDataSources(Map<String,AbsDataSource> dataSources)
    {
        mDataSources=dataSources;
    }

    public List<Class> getLstFormatClasses()
    {
        return lstFormatClasses;
    }

    void setLstFormatClasses(List<Class> lstFormatClasses)
    {
        this.lstFormatClasses=lstFormatClasses;
    }

    public String getDefault_datasourcename()
    {
        return default_datasourcename;
    }

    void setDefault_datasourcename(String default_datasourcename)
    {
        this.default_datasourcename=default_datasourcename;
    }

    public void initConfigLoad()
    {
        this.report_template_defaultbean=null;
        this.dataexport_template_defaultbean=null;
        this.lstGlobalPageInterceptors=null;
        this.ulstGlobalCss=null;
        if(mAutoDetectedDataImportBeans!=null)
        {
            mAutoDetectedDataImportBeans.clear();
        }
        if(this.mDataSources!=null)
        {
            for(Entry<String,AbsDataSource> entryDsTmp:mDataSources.entrySet())
            {
                entryDsTmp.getValue().closePool();
            }
            this.mDataSources=null;
        }
        dataexport_plainexcel_sheetsize=Integer.MIN_VALUE;
        this.mReportDatasetValueProviders=null;
        this.mCommonDatasetValueProviders=null;
    }
    
    public Object getResourceObject(ReportRequest rrequest,PageBean pbean,String key,boolean ismust)
    {
        if(key==null) return null;
        if(Tools.isDefineKey("$",key))
        {
            key=Tools.getRealKeyByDefine("$",key);
            return resources.get(rrequest,pbean,key,ismust);
        }
        return null;
    }

    public String getResourceString(ReportRequest rrequest,PageBean pbean,String key,boolean ismust)
    {
        if(key==null) return null;
        if(Tools.isDefineKey("$",key))
        {
            key=Tools.getRealKeyByDefine("$",key);
            return resources.getString(rrequest,pbean,key,ismust);
        }else
        {
            return key;
        }
    }
    
    public AbsButtonType getResourceButton(ReportRequest rrequest,IComponentConfigBean ccbean,String key,Class buttonType)
    {
        if(key==null) return null;
        if(Tools.isDefineKey("$",key))
        {
            key=Tools.getRealKeyByDefine("$",key);
        }
        XmlElementBean eleBean=(XmlElementBean)resources.get(rrequest,ccbean.getPageBean(),key,true);
        if(eleBean==null)
        {
            throw new WabacusConfigLoadingException("没有找到"+key+"对应的按钮资源项");
        }
        AbsButtonType buttonObj=ComponentConfigLoadManager.loadButtonConfig(ccbean,eleBean);
        if(buttonType!=null&&!buttonType.isInstance(buttonObj))
        {
            throw new WabacusConfigLoadingException("通过KEY为"+key+"配置的按钮不是"+buttonType.getName()+"类型");
        }
        buttonObj.setLabel(getResourceString(rrequest,ccbean.getPageBean(),buttonObj.getLabel(),true));
        return buttonObj;
    }
    
    public Resources getResources()
    {
        if(resources==null) resources=new Resources();
        return resources;
    }

    public void setResources(Resources resources)
    {
        this.resources=resources;
    }

    void setMSystemConfig(Map<String,String> systemConfig)
    {
        mSystemConfig=systemConfig;
    }

    public Map<String,List<AbsDataImportConfigBean>> getMAutoDetectedDataImportBeans()
    {
        return mAutoDetectedDataImportBeans;
    }

    void setLstDefaultGlobalJavascriptFiles(List<JavascriptFileBean> lstDefaultGlobalJavascriptFiles)
    {
        this.lstDefaultGlobalJavascriptFiles=lstDefaultGlobalJavascriptFiles;
    }

    void setLstGlobalJavascriptFiles(List<JavascriptFileBean> lstGlobalJavascriptFiles)
    {
        this.lstGlobalJavascriptFiles=lstGlobalJavascriptFiles;
    }

    void setMLocalJavascriptFiles(Map<String,List<JavascriptFileBean>> localJavascriptFiles)
    {
        mLocalJavascriptFiles=localJavascriptFiles;
    }

    public List<JavascriptFileBean> getLstDefaultGlobalJavascriptFiles()
    {
        return lstDefaultGlobalJavascriptFiles;
    }

    public List<JavascriptFileBean> getLstGlobalJavascriptFiles()
    {
        return lstGlobalJavascriptFiles;
    }

    public void addAutoDetectedDataImportBean(String filepath,
            AbsDataImportConfigBean autoDetectedDataImportBean)
    {
        if(filepath==null||filepath.trim().equals("")||autoDetectedDataImportBean==null)
        {
            return;
        }
        filepath=filepath.trim();
        if(mAutoDetectedDataImportBeans==null)
        {
            mAutoDetectedDataImportBeans=new HashMap<String,List<AbsDataImportConfigBean>>();
        }
        List<AbsDataImportConfigBean> lstAutoDetectedDataImportBeans=mAutoDetectedDataImportBeans
                .get(filepath);
        if(lstAutoDetectedDataImportBeans==null)
        {
            lstAutoDetectedDataImportBeans=new ArrayList<AbsDataImportConfigBean>();
            mAutoDetectedDataImportBeans.put(filepath,lstAutoDetectedDataImportBeans);
        }
        lstAutoDetectedDataImportBeans.add(autoDetectedDataImportBean);
        TimingThread.getInstance().addTask(new TimingDataImportTask());
    }

    public String getSystemConfigValue(String key,String defaultValue)
    {
        String temp=getSystemConfigValue(key);
        if(temp.equals(""))
        {
            return defaultValue;
        }else
        {
            return temp;
        }
    }

    public int getSystemConfigValue(String key,int defaultValue)
    {
        String temp=getSystemConfigValue(key);
        if(temp.equals(""))
        {
            return defaultValue;
        }else
        {
            try
            {
                return Integer.parseInt(temp);
            }catch(Exception e)
            {
                e.printStackTrace();
                return defaultValue;
            }
        }
    }

    public long getSystemConfigValue(String key,long defaultValue)
    {
        String temp=getSystemConfigValue(key);
        if(temp.equals(""))
        {
            return defaultValue;
        }else
        {
            try
            {
                return Long.parseLong(temp);
            }catch(Exception e)
            {
                e.printStackTrace();
                return defaultValue;
            }
        }
    }

    public boolean getSystemConfigValue(String key,boolean defaultValue)
    {
        String temp=getSystemConfigValue(key);
        if(temp.equals(""))
        {
            return defaultValue;
        }else
        {
            try
            {
                return Boolean.valueOf(temp).booleanValue();
            }catch(Exception e)
            {
                e.printStackTrace();
                return defaultValue;
            }
        }
    }

    private String getSystemConfigValue(String key)
    {
        if(mSystemConfig==null)
        {
            return "";
        }
        String temp=mSystemConfig.get(key);
        if(temp==null||temp.trim().equals("")) return "";
        return temp;
    }

    public PageBean getPageBean(String pageid)
    {
        if(mReportStructureInfo==null) return null;
        return mReportStructureInfo.get(pageid);
    }

    public List<String> getLstAllPageIds()
    {
        if(this.mReportStructureInfo==null) return null;
        List<String> lstResults=new ArrayList<String>();//重新建一个list对象存放返回的值，以免用户在外面修改到了本身的keySet容器对象
        lstResults.addAll(this.mReportStructureInfo.keySet());
        return lstResults;
    }

    public List<PageBean> getLstAllPageBeans()
    {
        if(this.mReportStructureInfo==null) return null;
        List<PageBean> lstResults=new ArrayList<PageBean>();
        for(Entry<String,PageBean> entryTmp:this.mReportStructureInfo.entrySet())
        {
            lstResults.add(entryTmp.getValue());
        }
        return lstResults;
    }
    
    public List<Class> getLstServerValidateClasses()
    {
        return lstServerValidateClasses;
    }

    public void setLstServerValidateClasses(List<Class> lstServerValidateClasses)
    {
        this.lstServerValidateClasses=lstServerValidateClasses;
    }

    public AbsContainerType getContainerType(String containername)
    {
        if(containername==null||containername.trim().equals("")) return null;
        containername=containername.trim();
        AbsContainerType containerObj=null;
        if(mContainertypes!=null&&mContainertypes.size()>0)
        {
            containerObj=mContainertypes.get(containername);
        }
        if(containerObj==null)
        {
            Class c=this.getTypeClass(containername);
            if(c!=null)
            {
                Object obj=AbsComponentType.createComponentTypeObj(c,null,null,null);
                if(!(obj instanceof AbsContainerType))
                {
                    throw new WabacusRuntimeException("配置的容器类型类："+containername+"没有继承"+AbsContainerType.class.getName()+"类");
                }
                containerObj=(AbsContainerType)obj;
                if(mContainertypes==null) mContainertypes=new HashMap<String,AbsContainerType>();
                mContainertypes.put(containername,containerObj);
            }
        }
        return containerObj;
    }

    public AbsReportType getReportType(String typename)
    {
        if(typename==null||typename.trim().equals("")) typename=Consts.DEFAULT_KEY;
        typename=typename.trim();
        IReportType reportObj=null;
        if(mReporttypes!=null&&mReporttypes.size()>0)
        {
            reportObj=mReporttypes.get(typename);
        }
        if(reportObj==null)
        {
            Class c=this.getTypeClass(typename);
            if(c!=null)
            {
                Object obj=AbsComponentType.createComponentTypeObj(c,null,null,null);
                if(!(obj instanceof AbsReportType))
                {
                    throw new WabacusRuntimeException("配置的报表类型类："+typename+"没有继承AbsReportType类");
                }
                reportObj=(IReportType)obj;
                if(mReporttypes==null) mReporttypes=new HashMap<String,IReportType>();
                mReporttypes.put(typename,reportObj);
            }
        }
        if(reportObj==null)
        {
            throw new WabacusRuntimeException("没有取到"+typename+"对应的报表类型");
        }
        return (AbsReportType)reportObj;
    }

    private Class getTypeClass(String configclassname)
    {
        configclassname=configclassname==null?"":configclassname.trim();
        if(configclassname.equals("")) return null;
        if(Tools.isDefineKey("class",configclassname))
        {
            String realclass=Tools.getRealKeyByDefine("class",configclassname).trim();
            if(realclass.equals("")) return null;
            return ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(realclass);
        }
        return null;
    }

    public AbsDataSource getDataSource(String datasource_name)
    {
        if(datasource_name==null||datasource_name.trim().equals(""))
        {
            datasource_name=Consts.DEFAULT_KEY;
        }
        datasource_name=datasource_name.trim();
        AbsDataSource datasource=mDataSources.get(datasource_name);
        if(datasource==null)
        {
            throw new WabacusRuntimeException("没有在wabacus.cfg.xml中配置name为"+datasource_name+"的数据源");
        }
        return datasource;
    }

    public List<String> getUlstLocalCss(PageBean pbean)
    {
        if(this.mLocalCss==null) return null;
        return mLocalCss.get(pbean.getReportfile_key());
    }

    public void setMLocalCss(Map<String,List<String>> localCss)
    {
        mLocalCss=localCss;
    }

    public Map<String,List<String>> getMLocalCss()
    {
        return mLocalCss;
    }

    public Map<String,List<JavascriptFileBean>> getMLocalJavascriptFiles()
    {
        return mLocalJavascriptFiles;
    }

    public List<JavascriptFileBean> getLstLocalJavascript(PageBean pbean)
    {
        if(this.mLocalJavascriptFiles==null) return null;
        return mLocalJavascriptFiles.get(pbean.getReportfile_key());
    }

    public AbsInputBox getInputBoxTypeByName(String name)
    {
        if(mInputBoxTypes==null) return null;
        AbsInputBox typeObj=null;
        if(name==null||name.trim().equals(""))
        {
            typeObj=mInputBoxTypes.get(Consts.DEFAULT_KEY);
            if(typeObj==null)
            {
                throw new WabacusRuntimeException("没有配置默认输入框类型，不能获取name属性为空的输入框");
            }
        }else
        {
            typeObj=mInputBoxTypes.get(name.trim());
            if(typeObj==null)
            {
                throw new WabacusRuntimeException("没有获取到name属性为"+name+"的输入框");
            }
        }
        return typeObj;
    }

    public AbsInputBox getInputBoxByType(Class boxTypeClass)
    {
        if(mInputBoxTypes==null||boxTypeClass==null) return null;
        for(Entry<String,AbsInputBox> entryInputBox:this.mInputBoxTypes.entrySet())
        {
            if(entryInputBox.getValue().getClass().getName().equals(boxTypeClass.getName())) return entryInputBox.getValue();
        }
        return null;
    }
    
    public void setInputBoxType(String name,AbsInputBox inputbox)
    {
        mInputBoxTypes.put(name,inputbox);
    }

    public void setMInputBoxTypes(Map<String,AbsInputBox> inputBoxTypes)
    {
        mInputBoxTypes=inputBoxTypes;
    }

    public Map<String,AbsInputBox> getMInputBoxTypes()
    {
        return mInputBoxTypes;
    }

    public void setMReportDatasetValueProviders(Map<String,AbsReportDataSetValueProvider> mReportDatasetValueTypes)
    {
       this.mReportDatasetValueProviders=mReportDatasetValueTypes;
    }
    
    Map<String,AbsReportDataSetValueProvider> getMReportDatasetValueProviders()
    {
        return mReportDatasetValueProviders;
    }

    public AbsReportDataSetValueProvider getReportDatasetValueProvider(String name)
    {
        if(this.mReportDatasetValueProviders==null) return null;
        AbsReportDataSetValueProvider typeObj;
        if(name==null||name.trim().equals(""))
        {
            typeObj=mReportDatasetValueProviders.get(Consts.DEFAULT_KEY);
            if(typeObj==null)
            {
                throw new WabacusRuntimeException("没有配置默认报表数据集脚本类型，不能获取name属性为空的报表数据集脚本类型");
            }
        }else
        {
            typeObj=mReportDatasetValueProviders.get(name.trim());
            if(typeObj==null)
            {
                throw new WabacusRuntimeException("没有获取到name属性为"+name+"的报表数据集脚本类型");
            }
        }
        return typeObj;
    }

    public void setMCommonDatasetValueProviders(Map<String,AbsCommonDataSetValueProvider> commonDatasetValueTypes)
    {
        this.mCommonDatasetValueProviders=commonDatasetValueTypes;
    }
    
    Map<String,AbsCommonDataSetValueProvider> getMCommonDatasetValueProviders()
    {
        return mCommonDatasetValueProviders;
    }
    
    public AbsCommonDataSetValueProvider getCommonDatasetValueProvider(String name)
    {
        if(this.mCommonDatasetValueProviders==null) return null;
        AbsCommonDataSetValueProvider typeObj;
        if(name==null||name.trim().equals(""))
        {
            typeObj=mCommonDatasetValueProviders.get(Consts.DEFAULT_KEY);
            if(typeObj==null)
            {
                throw new WabacusRuntimeException("没有配置默认报表数据集脚本类型，不能获取name属性为空的其它数据集脚本类型");
            }
        }else
        {
            typeObj=mCommonDatasetValueProviders.get(name.trim());
            if(typeObj==null)
            {
                throw new WabacusRuntimeException("没有获取到name属性为"+name+"的其它数据集脚本类型");
            }
        }
        return typeObj;
    }
    
    public void setMUpdateDatasetActionProviders(Map<String,AbsUpdateActionProvider> mUpdateDatasetActionProviders)
    {
        this.mUpdateDatasetActionProviders=mUpdateDatasetActionProviders;
    }
    
    Map<String,AbsUpdateActionProvider> getMUpdateDatasetActionProviders()
    {
        return mUpdateDatasetActionProviders;
    }

    public AbsUpdateActionProvider getUpdateDatasetActionProvider(String name)
    {
        if(this.mUpdateDatasetActionProviders==null) return null;
        AbsUpdateActionProvider typeObj;
        if(name==null||name.trim().equals(""))
        {
            typeObj=mUpdateDatasetActionProviders.get(Consts.DEFAULT_KEY);
        }else
        {
            typeObj=mUpdateDatasetActionProviders.get(name.trim());
        }
        return typeObj;
    }
    
    public IDataType getDataTypeByName(String name)
    {
        if(mDataTypes==null) return null;
        IDataType typeObj=null;
        if(name==null||name.trim().equals(""))
        {
            typeObj=mDataTypes.get(Consts.DEFAULT_KEY);
            if(typeObj==null)
            {
                throw new WabacusRuntimeException("没有配置默认数据类型，不能获取name属性为空的数据类型");
            }
        }else
        {
            typeObj=mDataTypes.get(name.trim());
            if(typeObj==null)
            {
                throw new WabacusRuntimeException("没有获取到name属性为"+name+"的数据类型");
            }
        }
        return typeObj;
    }
    
    public IDataType getDataTypeByClass(Class typeClass)
    {
        if(mDataTypes==null) return null;
        for(Entry<String,IDataType> entryTypesTmp:mDataTypes.entrySet())
        {
            if(entryTypesTmp.getValue().getClass().getName().equals(typeClass.getName()))
            {
                return entryTypesTmp.getValue();
            }
        }
        return null;
    }

    public Map<String,IDataType> getMDataTypes()
    {
        return mDataTypes;
    }

    public void setMDataTypes(Map<String,IDataType> dataTypes)
    {
        mDataTypes=dataTypes;
    }

    public void addGlobalInterceptor(AbsPageInterceptor interceptorObj)
    {
        if(this.lstGlobalPageInterceptors==null) this.lstGlobalPageInterceptors=new ArrayList<AbsPageInterceptor>();
        this.lstGlobalPageInterceptors.add(interceptorObj);
    }
    
    public List<AbsPageInterceptor> getLstGlobalPageInterceptors(String pageid)
    {
        List<AbsPageInterceptor> lstResults=new ArrayList<AbsPageInterceptor>();
        if(lstGlobalPageInterceptors==null||lstGlobalPageInterceptors.size()==0) return lstResults;
        for(AbsPageInterceptor tmpInterceptor:lstGlobalPageInterceptors)
        {
            if(tmpInterceptor.isMatch(pageid)) lstResults.add(tmpInterceptor);
        }
        return lstResults;
    }
    
    public List<String> getUlstGlobalCss()
    {
        return ulstGlobalCss;
    }

    public void addGlobalCss(String css)
    {
        if(ulstGlobalCss==null) ulstGlobalCss=new UniqueArrayList<String>();
        ulstGlobalCss.add(css);
    }
    
    public void addGlobalCss(List<String> lstcss)
    {
        if(ulstGlobalCss==null) ulstGlobalCss=new UniqueArrayList<String>();
        ulstGlobalCss.addAll(lstcss);
    }
    
    public void addFileTemplate(String filepath,TemplateBean tplBean)
    {
        if(this.mFileTemplates==null) this.mFileTemplates=new HashMap<String,TemplateBean>();
        mFileTemplates.put(filepath,tplBean);
    }
    
    public TemplateBean getFileTemplate(String filepath)
    {
        if(mFileTemplates==null) return null;
        return mFileTemplates.get(filepath);
    }
    
    public TemplateBean getDefaultReportTplBean()
    {
        if(report_template_defaultbean==null)
        {//还没加载全局默认模板资源项
            report_template_defaultbean=ComponentConfigLoadAssistant.getInstance().getStaticTemplateBeanByConfig(null,
                    "${"+Consts.REPORT_TEMPLATE_DEFAULT+"}");
            if(report_template_defaultbean==null)
            {
                throw new WabacusConfigLoadingException("没有配置显示在页面中的全局静态模板资源项");
            }
        }
        return report_template_defaultbean;
    }
    
    public TemplateBean getDefaultDataExportTplBean()
    {
        if(dataexport_template_defaultbean==null)
        {
            dataexport_template_defaultbean=ComponentConfigLoadAssistant.getInstance().getStaticTemplateBeanByConfig(null,
                    "${"+Consts.DATAEXPORT_TEMPLATE_DEFAULT+"}");
            if(dataexport_template_defaultbean==null)
            {
                throw new WabacusConfigLoadingException("没有配置显示在导出文件中的全局静态模板资源项");
            }
        }
        return dataexport_template_defaultbean;
    }
    
    public TemplateBean getDefaultReportPrintTplBean()
    {
        if(print_template_defaultbean==null)
        {
            print_template_defaultbean=ComponentConfigLoadAssistant.getInstance().getStaticTemplateBeanByConfig(null,
                    "${"+Consts.PRINT_TEMPLATE_DEFAULT+"}");
            if(print_template_defaultbean==null)
            {
                throw new WabacusConfigLoadingException("没有配置报表打印的全局静态模板资源项");
            }
        }
        return print_template_defaultbean;
    }

    public int getPlainexcelSheetsize()
    {
        if(dataexport_plainexcel_sheetsize==Integer.MIN_VALUE)
        {
            dataexport_plainexcel_sheetsize=Config.getInstance().getSystemConfigValue("dataexport-plainexcel-sheetsize",10000);
        }
        return dataexport_plainexcel_sheetsize;
    }
    
    public IComponentConfigBean getComponentConfigBeanByGuid(String componentGuid)
    {
        if(componentGuid==null||componentGuid.trim().equals("")) return null;
        componentGuid=componentGuid.trim();
        int idx=componentGuid.lastIndexOf(Consts_Private.GUID_SEPERATOR);
        String pageid=null;
        String componentid=null;
        if(idx<=0||componentGuid.endsWith(Consts_Private.GUID_SEPERATOR))
        {
            pageid=componentGuid;
        }else
        {
            pageid=componentGuid.substring(0,idx).trim();
            componentid=componentGuid.substring(idx+Consts_Private.GUID_SEPERATOR.length()).trim();
        }
        if(pageid==null||pageid.trim().equals("")) return null;
        PageBean pagebean=getPageBean(pageid);
        if(componentid==null||componentid.trim().equals("")) return pagebean;
        return pagebean.getChildComponentBean(componentid,true);
    }
    
    public ComponentPermissionBean getComponentDefaultPermissionBean(String componentGuid)
    {
        if(mComponentsDefaultPermissions==null) return null;
        if(componentGuid==null||componentGuid.trim().equals("")) return null;
        return mComponentsDefaultPermissions.get(componentGuid);
    }

    public void authorize(String componentGuid,String partname,String partid,String permissiontype,String permissionvalue)
    {
        if(mComponentsDefaultPermissions==null) mComponentsDefaultPermissions=new HashMap<String,ComponentPermissionBean>();
        ComponentPermissionBean cabean=this.mComponentsDefaultPermissions.get(componentGuid);
        if(cabean==null)
        {
            IComponentConfigBean ccbean=getComponentConfigBeanByGuid(componentGuid);
            if(ccbean==null) throw new WabacusConfigLoadingException("没有找到guid为"+componentGuid+"的组件，无法对其授权");
            cabean=new ComponentPermissionBean(ccbean);
            mComponentsDefaultPermissions.put(componentGuid,cabean);
        }
        cabean.authorize(partname,partid,permissiontype,permissionvalue);
    }
    
    public int checkDefaultPermission(String componentGuid,String partname,String partid,String permissiontype)
    {
        return checkDefaultPermission(componentGuid,partname,partid,permissiontype,"true");
    }    
    
    public int checkDefaultPermission(String componentGuid,String partname,String partid,String permissiontype,String permissionvalue)
    {
        if(!AuthorizationAssistant.getInstance().isExistPermissiontype(permissiontype)) return Consts.CHKPERMISSION_UNSUPPORTEDTYPE;
        if(!AuthorizationAssistant.getInstance().isExistValueOfPermissiontype(permissiontype,permissionvalue))
            return Consts.CHKPERMISSION_UNSUPPORTEDVALUE;//此权限类型不支持此权限值
        if(componentGuid==null||componentGuid.trim().equals("")) return Consts.CHKPERMISSION_EMPTY;
        if(mComponentsDefaultPermissions==null||mComponentsDefaultPermissions.get(componentGuid)==null) return Consts.CHKPERMISSION_EMPTY;
        return mComponentsDefaultPermissions.get(componentGuid).checkPermission(partname,partid,permissiontype,permissionvalue);
    }
    
    public void setSkin(HttpServletRequest request,PageBean pbean,String skin)
    {
        if(pbean!=null&&pbean.getPersonalizeObj()!=null)
        {
            pbean.getPersonalizeObj().storeSkin(request,pbean,skin);
        }else if(Config.default_pagepersonalize_object!=null)
        {
            Config.default_pagepersonalize_object.storeSkin(request,pbean,skin);
        }else
        {
            log.warn("没有配置持久化页面个性化信息的类，无法完成主题风格保存操作");
        }
    }
    
    public void setSkin(HttpServletRequest request,String pageid,String skin)
    {
        PageBean pbean=null;
        if(pageid!=null&&!pageid.trim().equals(""))
        {
            pbean=this.getPageBean(pageid);
            if(pbean==null)
            {
                throw new WabacusRuntimeException("设置主题风格失败，没有配置id为"+pageid+"的页面");
            }
        }
        setSkin(request,pbean,skin);
    }
    
    public String getSkin(HttpServletRequest request,PageBean pbean)
    {
        String resultSkin=null;
        if(pbean!=null&&pbean.getPersonalizeObj()!=null)
        {
            resultSkin=pbean.getPersonalizeObj().loadSkin(request,pbean);
            if(resultSkin!=null&&!resultSkin.trim().equals("")) return resultSkin;
            resultSkin=pbean.getPersonalizeObj().loadSkin(request,null);
            if(resultSkin!=null&&!resultSkin.trim().equals("")) return resultSkin;
        }
        if(Config.default_pagepersonalize_object!=null)
        {
            resultSkin=Config.default_pagepersonalize_object.loadSkin(request,pbean);
            if(resultSkin!=null&&!resultSkin.trim().equals("")) return resultSkin;
            if(pbean!=null)
            {
                resultSkin=Config.default_pagepersonalize_object.loadSkin(request,null);
                if(resultSkin!=null&&!resultSkin.trim().equals("")) return resultSkin;
            }
        }
        return Config.skin;
    }
    
    public String getSkin(HttpServletRequest request,String pageid)
    {
        PageBean pbean=null;
        if(pageid!=null&&!pageid.trim().equals(""))
        {
            pbean=this.getPageBean(pageid);
            if(pbean==null)
            {
                throw new WabacusRuntimeException("获取主题风格失败，没有配置id为"+pageid+"的页面");
            }
        }
        return getSkin(request,pbean);
    }
    
    public void addSkinConfigProperties(String skinname,Map<String,String> mConfigProperties)
    {
        if(this.mSkinConfigProperties==null) this.mSkinConfigProperties=new HashMap<String,Map<String,String>>();
        this.mSkinConfigProperties.put(skinname,mConfigProperties);
    }
    
    public String getSkinConfigValue(String skinname,String propertyname)
    {
        if(this.mSkinConfigProperties==null) return null;
        Map<String,String> mSkinConfigProperties=this.mSkinConfigProperties.get(skinname);
        if(mSkinConfigProperties==null) return null;
        return mSkinConfigProperties.get(propertyname);
    }
}
