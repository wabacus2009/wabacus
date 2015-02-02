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
package com.wabacus.config.resource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class Resources
{
    private static Log log=LogFactory.getLog(Resources.class);

    private Map mBuiltInDefaultResources=new HashMap();

    private Map mGlobalResources=new HashMap();

    private Map<String,Map> mLocalResources=new HashMap<String,Map>();

    private Map<String,Map> mLocalDefineResources=new HashMap<String,Map>();

    private Map<String,Map<String,Object>> mI18NResources;

    public Map getMGlobalResources()
    {
        return mGlobalResources;
    }

    public Map getMLocalResources()
    {
        return mLocalResources;
    }

    public Map<String,Map> getMLocalDefineResources()
    {
        return mLocalDefineResources;
    }

    public Map getMBuiltInDefaultResources()
    {
        return mBuiltInDefaultResources;
    }

    public Map<String,Map<String,Object>> getMI18NResources()
    {
        return mI18NResources;
    }

    public void setMI18NResources(Map<String,Map<String,Object>> resources)
    {
        mI18NResources=resources;
    }

    public String getI18NStringValue(String key,ReportRequest rrequest)
    {
        Object objResult=getI18NObjectValue(key,rrequest);
        if(objResult==null) return key;
        if(!(objResult instanceof String))
        {
            throw new WabacusRuntimeException("获取key:"+key+"对应的资源项失败，它不是字符串类型");
        }
        return ((String)objResult).trim();
    }
    
    public String getI18NStringValue(String key,String localelanguage)
    {
        Object objResult=getI18NObjectValue(key,localelanguage);
        if(objResult==null) return key;
        if(!(objResult instanceof String))
        {
            throw new WabacusRuntimeException("获取key:"+key+"对应的资源项失败，它不是字符串类型");
        }
        return ((String)objResult).trim();
    }
    
    public Object getI18NObjectValue(String key,ReportRequest rrequest)
    {
        if(key==null) return null;
        if(this.mI18NResources==null)
        {
            log.warn("没有配置国际化资源文件，无法获取"+key+"对应的资源项");
            return null;
        }
        if(Tools.isDefineKey("classpath",Config.configpath))
        {
            return getI18nValueInClasspath(key,rrequest);
        }
        Object result=getI18NObjectValue(key,rrequest.getLocallanguage());
        if(result instanceof String)
        {
            result=Tools.replaceAll((String)result,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin());
        }
        return result;
    }
    
    public Object getI18NObjectValue(String key,String localelanguage)
    {
        if(key==null) return null;
        if(this.mI18NResources==null)
        {
            log.warn("没有配置国际化资源文件，无法获取"+key+"对应的资源项");
            return null;
        }
        if(Tools.isDefineKey("classpath",Config.configpath))
        {
            return getI18nValueInClasspath(key,localelanguage);
        }
        String filename=Config.i18n_filename;
        if(Config.encode.equalsIgnoreCase("UTF-8"))
        {
            filename=filename+"_"+localelanguage;
        }else
        {
            log.warn("当前应用不是UTF-8编码，不支持国际化显示");
        }
        Map<String,Object> mResources=this.mI18NResources.get(filename.toLowerCase());
        if(mResources==null)
        {//没取到相应语言的资源项，则取英文版的
            filename=Config.i18n_filename;
            mResources=this.mI18NResources.get(filename);
        }
        if(mResources==null)
        {
            log.warn("没有配置资源文件"+filename+".xml，无法获取其中的资源项");
            return null;
        }
        Object result=mResources.get(key);
        if(result==null)
        {
            log.warn("在资源文件"+filename+".xml中没有取到"+key+"对应的资源项");
        }
        return result;
    }
    
    private Object getI18nValueInClasspath(String key,ReportRequest rrequest)
    {
        if(key==null) return null;
        Object result=getI18nValueInClasspath(key,rrequest.getLocallanguage());
        if(result instanceof String)
        {
            result=Tools.replaceAll((String)result,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin());
        }
        return result;
    }
    
    private Object getI18nValueInClasspath(String key,String localelanguage)
    {
        if(key==null) return null;
        if(localelanguage.equals("")) localelanguage="en";
        Map<String,Object> mResources=this.mI18NResources.get(localelanguage);
        if(mResources==null)
        {
            ConfigLoadManager.loadI18nResourcesInClassPath(localelanguage);
            mResources=this.mI18NResources.get(localelanguage);
        }
        if(mResources.size()==0)
        {
            mResources=this.mI18NResources.get("en");
        }
        if(mResources==null||mResources.size()==0)
        {
            log.warn("没有配置"+localelanguage+"对应的国际化资源文件，无法获取其中的资源项");
            return null;
        }
        Object result=mResources.get(key);
        if(result==null)
        {
            log.warn("在"+localelanguage+"对应的国际化资源文件中没有取到"+key+"对应的资源项");
        }
        return result;
    }
    
    public boolean contains(String key)
    {
        return mGlobalResources.containsKey(key);
    }

    public boolean contains(String reportfile_key,String key)
    {
        Map mTemp1=mLocalDefineResources.get(reportfile_key);
        Map mTemp2=mLocalResources.get(reportfile_key);
        if(mTemp1!=null&&mTemp1.containsKey(key)) return true;
        if(mTemp2!=null&&mTemp2.containsKey(key)) return true;
        return false;
    }

    public Object get(ReportRequest rrequest,String key,boolean ismust)
    {
        Object o=mGlobalResources.get(key);
        if(o==null)
        {
            o=mBuiltInDefaultResources.get(key);
            if(o==null&&ismust)
            {
                throw new WabacusRuntimeException("在resources文件中没有找到KEY为"+key+"的资源项");
            }
        }
        if(rrequest!=null&&o instanceof String)
        {
            o=Tools.replaceAll((String)o,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin());
        }
        return o;
    }

    public String getString(ReportRequest rrequest,String key,boolean ismust)
    {
        Object o=mGlobalResources.get(key);
        if(o==null)
        {
            o=mBuiltInDefaultResources.get(key);
            if(o==null)
            {
                if(ismust)
                {
                    throw new WabacusRuntimeException("在resources文件中没有定找到KEY为"+key+"的字符串资源");
                }else
                {
                    return null;
                }
            }
        }
        if(o instanceof String)
        {
            if(rrequest!=null)
            {//如果是在运行时获取资源项
                o=Tools.replaceAll((String)o,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin());
            }
            return ((String)o).trim();
        }else
        {
            throw new WabacusRuntimeException("在resources文件中KEY为"+key
                    +"的资源项不是String类型，无法调用Resources的getString(String)方法获取资源");
        }
    }

    public Object get(ReportRequest rrequest,PageBean pbean,String key,boolean ismust)
    {
        if(pbean==null) return get(rrequest,key,ismust);
        Object o=null;
        Map mTemp=mLocalDefineResources.get(pbean.getReportfile_key());
        if(mTemp==null||!mTemp.containsKey(key))
            mTemp=mLocalResources.get(pbean.getReportfile_key());
        if(mTemp==null||!mTemp.containsKey(key))
        {
            o=get(rrequest,key,ismust);
        }else
        {
            o=mTemp.get(key);
            if(rrequest!=null&&o instanceof String)
            {
                o=Tools.replaceAll((String)o,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin());
            }
        }
        if(o==null&&ismust)
        {
            throw new WabacusRuntimeException("在resources文件中没有找到KEY为"+key+"的资源项");
        }
        return o;
    }

    public String getString(ReportRequest rrequest,PageBean pbean,String key,boolean ismust)
    {
        if(pbean==null) return getString(rrequest,key,ismust);
        Object o=null;
        Map mTemp=mLocalDefineResources.get(pbean.getReportfile_key());
        if(mTemp==null||!mTemp.containsKey(key))
            mTemp=mLocalResources.get(pbean.getReportfile_key());
        if(mTemp==null||!mTemp.containsKey(key))
        {
            o=get(rrequest,key,ismust);
        }else
        {
            o=mTemp.get(key);
            if(rrequest!=null&&o instanceof String)
            {
                o=Tools.replaceAll((String)o,Consts_Private.SKIN_PLACEHOLDER,rrequest.getPageskin());//替换掉资源项中的主题风格占位符
            }
        }
        if(o==null)
        {
            if(ismust)
            {
                throw new WabacusRuntimeException("在resources文件中没有找到KEY为"+key+"的资源项");
            }else
            {
                return null;
            }
        }
        if(o instanceof String)
        {
            return ((String)o).trim();
        }else
        {
            throw new WabacusRuntimeException("在resources文件中KEY为"+key
                    +"的资源项不是String类型，无法调用Resources的getString(String,String)方法获取资源");
        }
    }

    public boolean isEmpty()
    {
        return mGlobalResources==null||mGlobalResources.size()==0;
    }

    public boolean isEmpty(String reportfile_key)
    {
        Map mTemp1=mLocalDefineResources.get(reportfile_key);
        Map mTemp2=mLocalResources.get(reportfile_key);
        return (mTemp1==null||mTemp1.size()==0)&&(mTemp2==null||mTemp2.size()==0);
    }

    public void replacePlaceHolderInStringRes()
    {
        mBuiltInDefaultResources=replace(mBuiltInDefaultResources);
        mGlobalResources=replace(mGlobalResources);
        /*mLocalResources=replace2(mLocalResources);
        mLocalDefineResources=replace2(mLocalDefineResources);*/
        if(mI18NResources!=null)
        {
            Map<String,Map<String,Object>> mI18nTemp=new HashMap<String,Map<String,Object>>();
            for(Entry<String,Map<String,Object>> entry:mI18NResources.entrySet())
            {
                Map<String,Object> mTemp=entry.getValue();
                if(mTemp==null)
                {
                    mI18nTemp.put(entry.getKey(),null);
                    continue;
                }
                Map<String,Object> mResults=new HashMap<String,Object>();
                Object value;
                for(Entry<String,Object> entry2:mTemp.entrySet())
                {
                    value=entry2.getValue();
                    if(value==null||!(value instanceof String))
                    {
                        mResults.put(entry2.getKey(),value);
                        continue;
                    }
//                    strvalue=Tools.replaceAll(strvalue,"//","/");
                    mResults.put(entry2.getKey(),WabacusAssistant.getInstance().replaceSystemPlaceHolder((String)value));
                }
                mI18nTemp.put(entry.getKey(),mResults);
            }
            mI18NResources=mI18nTemp;
        }
    }

    public Map replace(Map mResources)
    {
        Map mResults=null;
        if(mResources!=null)
        {
            mResults=new HashMap();
            Iterator<Entry> itEntries=mResources.entrySet().iterator();
            Entry entry;
            String value;
            while(itEntries.hasNext())
            {
                entry=itEntries.next();
                if(entry.getValue() instanceof String)
                {
                    value=(String)entry.getValue();
//                    value=Tools.replaceAll(value,"//","/");
                    mResults.put(entry.getKey(),WabacusAssistant.getInstance().replaceSystemPlaceHolder(value));
                }else
                {
                    mResults.put(entry.getKey(),entry.getValue());
                }
            }
        }
        return mResults;
    }

    /* private Map<String,Map> replace2(Map<String,Map> mResources)
     {
         Map<String, Map> mResourcesTemp=null;
         if(mResources!=null)
         {
             mResourcesTemp=new HashMap<String, Map>();
             Iterator<Entry<String,Map>> itEntries=mResources.entrySet().iterator();
             Entry<String,Map> entry=null;
             while(itEntries.hasNext())
             {
                 entry=itEntries.next();
                 if(entry==null) continue;
                 mResourcesTemp.put(entry.getKey(),replace(entry.getValue()));
             }
         }
         return mResourcesTemp;
     }*/
}
