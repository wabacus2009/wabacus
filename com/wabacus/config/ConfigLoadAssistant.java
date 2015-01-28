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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.FormatBean;
import com.wabacus.config.other.JavascriptFileBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.util.Tools;

public class ConfigLoadAssistant
{
    private final static ConfigLoadAssistant instance=new ConfigLoadAssistant();
    
    private ConfigLoadAssistant()
    {
    }
    
    public static ConfigLoadAssistant getInstance()
    {
        return instance;
    }
    
    public Map<String,String> assembleAllAttributes(List<XmlElementBean> lstElementBeans,
            String[] attributeNamesArr)
    {
        Map<String,String> mResults=lstElementBeans.get(0).getMPropertiesClone();
        if(mResults==null) mResults=new HashMap<String,String>();
        XmlElementBean eleDisBeanTmp;
        for(int i=1;i<lstElementBeans.size();i++)
        {
            eleDisBeanTmp=lstElementBeans.get(i);
            for(int j=0;j<attributeNamesArr.length;j++)
            {
                if(mResults.get(attributeNamesArr[j])!=null) continue;
                if(eleDisBeanTmp.attributeValue(attributeNamesArr[j])==null) continue;
                mResults.put(attributeNamesArr[j],eleDisBeanTmp
                        .attributeValue(attributeNamesArr[j]));
            }
        }
        return mResults;
    }
    
    public List<XmlElementBean> getRefElements(String refs,String nodename,List<String> lstReferedKeys,
            IComponentConfigBean ccbean)
    {
        List<XmlElementBean> lstEleBeans=new ArrayList<XmlElementBean>();
        if(refs==null||refs.trim().equals("")) return lstEleBeans;
        Object resObjTmp;
        XmlElementBean eleReferredTmp;
        if(lstReferedKeys==null) lstReferedKeys=new ArrayList<String>();
        List<String> lstRefs=Tools.parseStringToList(refs,"|",false);
        for(String refTmp:lstRefs)
        {
            refTmp=refTmp.trim();
            if(refTmp.equals("")) continue;
            if(!Tools.isDefineKey("$",refTmp))
            {
                throw new WabacusConfigLoadingException("加载报表"+ccbean.getPath()+"的"+nodename
                        +"失败，其引用的"+refs+"包含不是${key}格式的项");
            }
            if(lstReferedKeys.contains(Tools.getRealKeyByDefine("$",refTmp)))
            {
                throw new WabacusConfigLoadingException("加载报表"+ccbean.getPath()+"的"+nodename
                        +"失败，其多次循环引用"+refTmp+"资源项，造成死循环");
            }
            lstReferedKeys.add(Tools.getRealKeyByDefine("$",refTmp));
            resObjTmp=Config.getInstance().getResourceObject(null,ccbean.getPageBean(),refTmp,true);
            if(!(resObjTmp instanceof XmlElementBean))
            {
                throw new WabacusConfigLoadingException("加载报表"+ccbean.getPath()+"的"+nodename
                        +"失败，其引用的"+refTmp+"资源项不是XmlElementRes类型");
            }
            eleReferredTmp=(XmlElementBean)resObjTmp;
            if(!eleReferredTmp.getName().equals(nodename))
            {
                throw new WabacusConfigLoadingException("加载报表"+ccbean.getPath()+"的"+nodename
                        +"失败，其引用的"+refTmp+"资源项的顶层标签不是"+nodename);
            }
            lstEleBeans.add(eleReferredTmp);
            lstEleBeans.addAll(getRefElements(eleReferredTmp.attributeValue("ref"),nodename,
                    lstReferedKeys,ccbean));//这个被引用的资源项可以再引用其它资源项，无限引用
        }
        return lstEleBeans;
    }
    
    public  List<Class> convertStringToClassList(String strClasses)
    {
        if(strClasses==null||strClasses.trim().equals(""))
        {
            return null;
        }
        List<Class> lstClasses=new ArrayList<Class>();
        List<String> lstTemp=Tools.parseStringToList(strClasses,";",false);
        for(String strclassTmp:lstTemp)
        {
            if(strclassTmp==null||strclassTmp.trim().equals("")) continue;
            lstClasses.add(ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(strclassTmp.trim()));
        }
        if(lstClasses.size()==0) lstClasses=null;
        return lstClasses;
    }
    
    public static List<String> lstInvalidIdCharacters=new ArrayList<String>();
    
    static
    {
        lstInvalidIdCharacters.add("_");
        lstInvalidIdCharacters.add("|");
        lstInvalidIdCharacters.add("(");
        lstInvalidIdCharacters.add(")");
        lstInvalidIdCharacters.add("*");
        lstInvalidIdCharacters.add(".");
        lstInvalidIdCharacters.add("%");
        lstInvalidIdCharacters.add("#");
        lstInvalidIdCharacters.add("&");
        lstInvalidIdCharacters.add("-");
    }
    
    public static IDataType loadDataType(XmlElementBean eleBean)
    {
        String datatype=eleBean.attributeValue("datatype");
        String extrainfo=null;
        if(datatype!=null&&!datatype.trim().equals(""))
        {
            datatype=datatype.trim();
            int idxleft=datatype.indexOf("{");
            int idxright=datatype.indexOf("}");
            if(idxleft>0&&idxright==datatype.length()-1)
            {
                extrainfo=datatype.substring(idxleft+1,idxright).trim();
                datatype=datatype.substring(0,idxleft).trim();
            }
        }
        IDataType typeObj=Config.getInstance().getDataTypeByName(datatype);
        return typeObj.setUserConfigString(extrainfo);
    }
    
    public FormatBean loadFormatConfig(XmlElementBean eleFormatBean)
    {
        if(eleFormatBean==null) return null;
        FormatBean fbean=new FormatBean(null);
        fbean.setLstImports(loadImportsConfig(eleFormatBean));
        XmlElementBean eleFormatValueBean=eleFormatBean.getChildElementByName("value");
        if(eleFormatValueBean==null) return null;
        String format=eleFormatValueBean.getContent();
        format=format==null?"":format.trim();
        if(format.equals("")) return null;
        fbean.setFormatContent(format);
        return fbean;
    }
    
    public List<String> loadImportsConfig(XmlElementBean eleBean)
    {
        XmlElementBean eleImportsBean=eleBean.getChildElementByName("imports");
        if(eleImportsBean==null) return null;
        List<XmlElementBean> lstImportBeans=eleImportsBean.getLstChildElementsByName("import");
        if(lstImportBeans==null||lstImportBeans.size()==0) return null;
        String importTmp;
        List<String> lstImports=new ArrayList<String>();
        for(XmlElementBean eleImportBeanTmp:lstImportBeans)
        {
            importTmp=eleImportBeanTmp.getContent();
            if(importTmp==null||importTmp.trim().equals("")) continue;
            importTmp=importTmp.trim();
            if(importTmp.endsWith(".*"))
            {
                importTmp=importTmp.substring(0,importTmp.length()-2).trim();
            }else
            {
                int idx=importTmp.lastIndexOf(".");
                if(idx<=0)
                {
                    throw new WabacusConfigLoadingException("<import>"+importTmp+"</import>配置的导入外部包或JAVA类无效，不能导入");
                }
                importTmp=importTmp.substring(0,idx).trim();
            }
            if(importTmp.equals("")||lstImports.contains(importTmp)) continue;//重复的不加进来，不重复的都加进来。
            lstImports.add(importTmp);
        }
        return lstImports;
    }
    
    public List<String> loadImportsConfig(Element element)
    {
        Element eleImports=element.element("imports");
        List<String> lstImportPackages=new ArrayList<String>();
        if(eleImports!=null)
        {
            List lstImports=eleImports.elements("import");
            if(lstImports!=null&&lstImports.size()>0)
            {
                Iterator itImports=lstImports.iterator();
                while(itImports.hasNext())
                {
                    Element eleImport=(Element)itImports.next();
                    if(eleImport!=null)
                    {
                        String valueTemp=eleImport.getTextTrim();
                        if(valueTemp==null||valueTemp.trim().equals("")) continue;
                        if(valueTemp.lastIndexOf(".*")==valueTemp.length()-2)
                        {
                            valueTemp=valueTemp.substring(0,valueTemp.length()-2).trim();
                        }
                        if(valueTemp.equals("")||lstImportPackages.contains(valueTemp)) continue;
                        lstImportPackages.add(valueTemp);
                    }
                }
            }
        }
        return lstImportPackages;
    }
    
    public List<JavascriptFileBean> getLstPopupComponentJs()
    {
        List<JavascriptFileBean> lstResults=new ArrayList<JavascriptFileBean>();
        String promptDialogType=Config.getInstance().getSystemConfigValue("prompt-dialog-type","artdialog");
        if(promptDialogType.toLowerCase().equals("ymprompt"))
        {
            String jsTmp=Config.webroot+"/webresources/component/ymPrompt/ymPrompt.js";
            jsTmp=Tools.replaceAll(jsTmp,"\\","/");
            jsTmp=Tools.replaceAll(jsTmp,"//","/");
            lstResults.add(new JavascriptFileBean(jsTmp.trim(),0));
        }else
        {
            String jsTmp=Config.webroot+"/webresources/component/artDialog/artDialog.js";
            jsTmp=Tools.replaceAll(jsTmp,"\\","/");
            jsTmp=Tools.replaceAll(jsTmp,"//","/");
            lstResults.add(new JavascriptFileBean(jsTmp.trim(),0));
            jsTmp=Config.webroot+"/webresources/component/artDialog/plugins/iframeTools.js";
            jsTmp=Tools.replaceAll(jsTmp,"\\","/");
            jsTmp=Tools.replaceAll(jsTmp,"//","/");
            lstResults.add(new JavascriptFileBean(jsTmp.trim(),0));
        }
        return lstResults;
    }
    
    public List<String> getLstPopupComponentCss()
    {
        List<String> lstResults=new ArrayList<String>();
        String promptDialogType=Config.getInstance().getSystemConfigValue("prompt-dialog-type","artdialog");
        if(promptDialogType.toLowerCase().equals("ymprompt"))
        {
            String cssTmp=Config.webroot+"/webresources/skin/"+Config.skin+"/ymPrompt/ymPrompt.css";
            cssTmp=Tools.replaceAll(cssTmp,"\\","/");
            cssTmp=Tools.replaceAll(cssTmp,"//","/");
            lstResults.add(cssTmp);
        }else
        {
            String cssTmp=Config.webroot+"/webresources/skin/"+Config.skin+"/artDialog/artDialog.css";
            cssTmp=Tools.replaceAll(cssTmp,"\\","/");
            cssTmp=Tools.replaceAll(cssTmp,"//","/");
            lstResults.add(cssTmp);
        }
        return lstResults;
    }
}

