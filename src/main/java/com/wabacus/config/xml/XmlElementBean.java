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
package com.wabacus.config.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.exception.WabacusConfigLoadingException;

public class XmlElementBean implements Cloneable
{
    private String name;

    private String content;

    private XmlElementBean parentElement;

    private List<XmlElementBean> lstChildElements;

    private Map<String,String> mProperties;//属性集合

    public XmlElementBean(String name)
    {
        if(name==null||name.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("初始化节点失败，没有传入节点名");
        }
        this.name=name;
    }

    public String getName()
    {
        return name;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content=content;
    }

    public XmlElementBean getParentElement()
    {
        return parentElement;
    }

    public void setParentElement(XmlElementBean parentElement)
    {
        this.parentElement=parentElement;
    }

    public List<XmlElementBean> getLstChildElements()
    {
        return lstChildElements;
    }

    public List<XmlElementBean> getLstChildElementsClone(XmlElementBean parentElementNew)
    {
        if(lstChildElements==null||lstChildElements.size()==0) return lstChildElements;
        if(parentElementNew==null) parentElementNew=this.parentElement;
        List<XmlElementBean> lstChildElementsNew=new ArrayList<XmlElementBean>();
        for(XmlElementBean eleChildTmp:lstChildElementsNew)
        {
            lstChildElementsNew.add(eleChildTmp.clone(parentElementNew));
        }
        return lstChildElementsNew;
    }
    
    public void setLstChildElements(List<XmlElementBean> lstChildElements)
    {
        this.lstChildElements=lstChildElements;
    }

    public XmlElementBean getChildElementByName(String name)
    {
        if(lstChildElements==null) return null;
        for(XmlElementBean eleBeanTmp:lstChildElements)
        {
            if(eleBeanTmp==null) continue;
            if(eleBeanTmp.getName().equals(name))
            {
                return eleBeanTmp;
            }
        }
        return null;
    }

    public List<XmlElementBean> getLstChildElementsByName(String name)
    {
        if(lstChildElements==null) return null;
        List<XmlElementBean> lstElements=new ArrayList<XmlElementBean>();
        for(XmlElementBean eleBeanTmp:lstChildElements)
        {
            if(eleBeanTmp==null) continue;
            if(eleBeanTmp.getName().equals(name))
            {
                lstElements.add(eleBeanTmp);
            }
        }
        return lstElements;
    }
    
    public Map<String,String> getMPropertiesClone()
    {
        if(mProperties==null) return null;
        return (Map<String,String>)((HashMap<String,String>)mProperties).clone();
    }
    
    public Map<String,String> attributes()
    {
        return mProperties;
    }

    public void setMProperties(Map<String,String> properties)
    {
        mProperties=properties;
    }

    public void setAttributeValue(String propname,String propvalue)
    {
        if(mProperties==null) mProperties=new HashMap<String,String>();
        mProperties.put(propname,propvalue);
    }
    
    public String attributeValue(String propname)
    {
        if(mProperties==null) return null;
        return mProperties.get(propname);
    }

    public String attributeValue(String propname,String defaultvalue)
    {
        if(mProperties==null) return defaultvalue;
        String value=mProperties.get(propname);
        if(value==null||value.trim().equals(""))
        {
            return defaultvalue;
        }
        return value.trim();
    }

    public XmlElementBean clone(XmlElementBean parentEleBean)
    {
        try
        {
            XmlElementBean eleBeanNew=(XmlElementBean)super.clone();
            eleBeanNew.setParentElement(parentEleBean);
            if(this.lstChildElements!=null)
            {
                List<XmlElementBean> lstChildElementsNew=new ArrayList<XmlElementBean>();
                for(XmlElementBean eleChildTmp:lstChildElementsNew)
                {
                    lstChildElementsNew.add(eleChildTmp.clone(eleBeanNew));
                }
                eleBeanNew.setLstChildElements(lstChildElementsNew);
            }
            return eleBeanNew;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
