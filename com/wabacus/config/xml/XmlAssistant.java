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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class XmlAssistant
{
    private final static XmlAssistant instance=new XmlAssistant();
    
    private XmlAssistant(){}
    
    public static XmlAssistant getInstance()
    {
        return instance;
    }
    
    public  Document loadXmlDocument(String configfilepath)
    {
        if(Tools.isDefineKey("classpath",Config.configpath))
        {
            if(configfilepath.startsWith("/"))
            {
                configfilepath=configfilepath.substring(1);
            }
            BufferedInputStream bis=null;
            try
            {
                bis=new BufferedInputStream(ConfigLoadManager.currentDynClassLoader
                        .getResourceAsStream(WabacusAssistant.getInstance().getRealFilePath(
                                Config.configpath,configfilepath)));
                Document doc=loadXmlDocument(bis);
                return doc;
            }catch(DocumentException e)
            {
                throw new WabacusConfigLoadingException("加载配置文件"+configfilepath
                        +"失败，可能此配置文件不存在或不是合法的XML文档",e);
            }finally
            {
                try
                {
                    if(bis!=null) bis.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }else
        {
            return loadXmlDocument(new File(WabacusAssistant.getInstance().getRealFilePath(
                    Config.configpath,configfilepath)));
        }
    }
    
    public  Document loadXmlDocument(File file)
    {
        BufferedInputStream bis=null;
        try
        {
            SAXReader saxReader=new SAXReader();
            Map map=new HashMap();
            map.put(Consts.XML_NAMESPACE_KEY,Consts.XML_NAMESPACE_VALUE);
            saxReader.getDocumentFactory().setXPathNamespaceURIs(map);
            bis=new BufferedInputStream(new FileInputStream(file));
//            ConfigLoadAssistant.getInstance().addOpenedInputFileObj(inputstreamKey,bis);//加进去以便后面的关闭
            Document doc=saxReader.read(bis);
            return doc;
        }catch(FileNotFoundException fnfe)
        {
            throw new WabacusConfigLoadingException("加载配置文件"+file.getName()+"失败，没有找到此配置文件",fnfe);
        }catch(DocumentException de)
        {
            throw new WabacusConfigLoadingException("加载配置文件"+file.getName()+"失败，不是合法的XML格式",de);
        }finally
        {
            try
            {
                if(bis!=null)bis.close();
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public  Document loadXmlDocument(InputStream in) throws DocumentException
    {
        SAXReader saxReader=new SAXReader();
        Map map=new HashMap();
        map.put(Consts.XML_NAMESPACE_KEY,Consts.XML_NAMESPACE_VALUE);
        saxReader.getDocumentFactory().setXPathNamespaceURIs(map);
        return saxReader.read(new BufferedInputStream(in));
    }

    public  List getElementsByName(Element parent,String nodename)
    {
        if(parent==null||nodename==null||nodename.trim().equals("")) return null;
        return parent.selectNodes(Consts.XML_NAMESPACE_KEY+":"+nodename);
    }

    public  Element getSingleElementByName(Element parent,String nodename)
    {
        if(parent==null||nodename==null||nodename.trim().equals("")) return null;
        List lstChildren=parent.selectNodes(Consts.XML_NAMESPACE_KEY+":"+nodename);
        if(lstChildren==null||lstChildren.size()==0) return null;
        Element ele=(Element)lstChildren.get(0);
        if(ele==null) return null;
        if(!isLegalNamespaceElement(ele))
        {
            throw new WabacusConfigLoadingException("namespace："+ele.getNamespacePrefix()
                    +"不合法，无法加载其配置");
        }
        return ele;
    }

    public String addNamespaceToXpath(String xpath)
    {
        if(xpath==null||xpath.trim().equals("")) return xpath;
        if(Consts.XML_NAMESPACE_KEY==null||Consts.XML_NAMESPACE_KEY.trim().equals("")) return xpath;
        StringBuffer resultBuf=new StringBuffer();
        for(int i=0;i<xpath.length();i++)
        {
            if(xpath.charAt(i)=='/')
            {
                resultBuf.append("/");
                for(i=i+1;i<xpath.length();i++)
                {
                    if(xpath.charAt(i)==' ') continue;
                    if(xpath.charAt(i)=='/')
                    {
                        resultBuf.append("/");
                    }else
                    {
                        break;
                    }
                }
                if(i<xpath.length())
                {
                    resultBuf.append(Consts.XML_NAMESPACE_KEY).append(":");
                    resultBuf.append(xpath.charAt(i));
                }
            }else
            {
                resultBuf.append(xpath.charAt(i));
            }
        }
        return resultBuf.toString();
    }

    public  boolean isLegalNamespaceElement(Element element)
    {
        if(element==null) return false;
        String prex=element.getNamespacePrefix();
        if(prex==null) return false;
        prex=prex.trim();
        if(!prex.equals("")&&!prex.equals("wx"))
        {
            return false;
        }
        return true;
    }
    
    public void saveDocumentToXmlFile(String configfilepath,Document doc) throws IOException
    {
        XMLWriter xwriter=null;
        try
        {
            File f=null;
            if(Tools.isDefineKey("classpath",Config.configpath))
            {
                f=WabacusAssistant.getInstance().getFileObjByPathInClasspath(
                        Tools.getRealKeyByDefine("classpath",Config.configpath),configfilepath);
            }else
            {
                f=new File(WabacusAssistant.getInstance().getRealFilePath(Config.configpath,
                        configfilepath));
            }
            OutputFormat format=OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            xwriter=new XMLWriter(new OutputStreamWriter(new FileOutputStream(f,false),"UTF-8"));
            xwriter.write(doc);
        }finally
        {
                if(xwriter!=null) xwriter.close();
        }
    }
    
    public  XmlElementBean parseXmlValueToXmlBean(Element element)
    {
        if(element==null) return null;
        if(!isLegalNamespaceElement(element))
        {
            throw new WabacusConfigLoadingException("namespace："
                    +element.getNamespacePrefix()+"不合法");
        }
        XmlElementBean xebean=new XmlElementBean(element.getName());
        String tagContent=element.getText();
        xebean.setContent(tagContent==null?"":tagContent.trim());
        Iterator itAttributes=element.attributeIterator();
        Map<String,String> mProps=new HashMap<String,String>();
        xebean.setMProperties(mProps);
        Attribute eleProps;
        while(itAttributes.hasNext())
        {
            eleProps=(Attribute)itAttributes.next();
            mProps.put(eleProps.getName(),eleProps.getValue());
        }
        List<XmlElementBean> lstChildren=null;
        List lstChildElements=element.elements();
        if(lstChildElements!=null&&lstChildElements.size()>0)
        {
            lstChildren=new ArrayList<XmlElementBean>();
            XmlElementBean eleTmp;
            for(Object eleObj:lstChildElements)
            {
                if(eleObj==null) continue;
                eleTmp=parseXmlValueToXmlBean((Element)eleObj);
                if(eleTmp==null) continue;
                eleTmp.setParentElement(xebean);
                lstChildren.add(eleTmp);
            }
            xebean.setLstChildElements(lstChildren);
        }
        return xebean;
    }
    
    public void mergeXmlElementBeans(XmlElementBean xebean1,XmlElementBean xebean2)
    {
        if(xebean1==null||xebean2==null) return;
        Map<String,String> mAttributes2=xebean2.attributes();
        if(mAttributes2!=null&&mAttributes2.size()>0)
        {
            for(Entry<String,String> entryAtt2:mAttributes2.entrySet())
            {
                if(entryAtt2.getKey()==null||entryAtt2.getValue()==null) continue;
                if(xebean1.attributeValue(entryAtt2.getKey())==null)
                {
                    xebean1.setAttributeValue(entryAtt2.getKey(),entryAtt2.getValue());
                }
            }
        }
        if((xebean1.getContent()==null||xebean1.getContent().trim().equals(""))
                &&(xebean1.getLstChildElements()==null||xebean1.getLstChildElements().size()==0))
        {//如果没有在xebean1中配置标签内容，且没有配置子标签
            xebean1.setContent(xebean2.getContent());
            xebean1.setLstChildElements(xebean2.getLstChildElementsClone(xebean1));
        }
    }
}
