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
package com.wabacus.config.database.datasource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.configuration.JAXPConfigurator;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.xml.XmlAssistant;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.util.DesEncryptTools;
import com.wabacus.util.Tools;

public class ProxoolDataSource extends AbsDataSource
{
    private static Log log=LogFactory.getLog(ProxoolDataSource.class);

    private String alias;

    private boolean isClosedPool=false;

    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias=alias;
    }

    public Connection getConnection()
    {
        try
        {
            log.debug("从数据源"+this.getName()+"获取数据库连接");
            return DriverManager.getConnection(alias);
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("获取数据库连接失败",e);
        }
    }

    public DataSource getDataSource()
    {
        throw new WabacusRuntimeException("不能从ProxoolDataSource数据源类型中获取javax.sql.DataSource对象");
    }
    
    public void closePool()
    {
        super.closePool();
        if(!isClosedPool)
        {
            log.debug("正在关闭Proxool连接池......................................");
            ProxoolFacade.shutdown(0);
            isClosedPool=true;
            alias=null;
        }
    }

    public void loadConfig(Element eleDataSource)
    {
        super.loadConfig(eleDataSource);
        List lstEleProperties=eleDataSource.elements("property");
        if(lstEleProperties==null||lstEleProperties.size()==0)
        {
            throw new WabacusConfigLoadingException("没有为数据源："+this.getName()+"配置alias、configfile等参数");
        }
        String configfile=null;
        Element eleChild;
        String name;
        String value;
        for(int i=0;i<lstEleProperties.size();i++)
        {
            eleChild=(Element)lstEleProperties.get(i);
            name=eleChild.attributeValue("name");
            value=eleChild.getText();
            name=name==null?"":name.trim();
            value=value==null?"":value.trim();
            if(name.equals("alias"))
            {
                alias=value;
            }else if(name.equals("configfile"))
            {
                configfile=value;
            }
        }
        if(alias==null||alias.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("没有为数据源"+this.getName()+"配置alias");
        }
        alias="proxool."+alias.trim();
        if(configfile==null||configfile.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("没有为数据源"+this.getName()+"配置configfile");
        }
        changePassword(configfile,false);
        BufferedInputStream bis=null;
        try
        {
            if(Tools.isDefineKey("classpath",Config.configpath))
            {
                if(configfile.startsWith("/"))
                {
                    configfile=configfile.substring(1);
                }
                bis=new BufferedInputStream(ConfigLoadManager.currentDynClassLoader.getResourceAsStream(WabacusAssistant.getInstance()
                        .getRealFilePath(Config.configpath,configfile)));
            }else
            {
                bis=new BufferedInputStream(new FileInputStream(
                        new File(WabacusAssistant.getInstance().getRealFilePath(Config.configpath,configfile))));
            }
            if(!configfile.toLowerCase().endsWith(".xml"))
            {
                throw new WabacusConfigLoadingException("加载配置文件"+configfile+"失败，不合法的文件格式，wabacus只支持使用.xml配置proxool数据源");
            }
            JAXPConfigurator.configure(new InputStreamReader(bis),false);
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("加载配置文件"+configfile+"失败",e);
        }finally
        {
            if(bis!=null)
            {
                try
                {
                    bis.close();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
                bis=null;
            }
        }
        changePassword(configfile,true);
    }

    private void changePassword(String configfile,boolean actiontype)
    {
        if(!configfile.toLowerCase().endsWith(".xml"))
        {
            throw new WabacusConfigLoadingException("加载配置文件"+configfile+"失败，不合法的文件格式，wabacus只支持使用.xml配置proxool数据源");
        }
        Document doc=XmlAssistant.getInstance().loadXmlDocument(configfile);
        Element root=doc.getRootElement();
        Element eleDriverProp=root.element("driver-properties");
        if(eleDriverProp==null) return;
        List lstProps=eleDriverProp.elements("property");
        if(lstProps==null||lstProps.size()==0)
        {
            return;
        }
        Element elePasswordProp=null;
        for(int i=0;i<lstProps.size();i++)
        {
            if("password".equalsIgnoreCase(((Element)lstProps.get(i)).attributeValue("name").trim()))
            {
                elePasswordProp=(Element)lstProps.get(i);
            }
        }
        if(elePasswordProp==null) return;
        String password=elePasswordProp.attributeValue("value");
        password=password==null?"":password.trim();
        if(password.equals("")) return;
        boolean shouldsave=false;
        if(actiontype)
        {
            if(DesEncryptTools.KEY_OBJ==null) return;//没有在wabacus.cfg.xml中指定密钥，则不加密
            if(password.startsWith("{3DES}"))
            {
                if(DesEncryptTools.IS_NEWKEY)
                {
                    throw new WabacusConfigLoadingException("密钥文件已经改变，但"+configfile+"中已有用旧密钥加密好的密码，它们将无法解密，请将它们先置成明文再换密钥文件");
                }
            }else if(!password.equals(""))
            {
                password="{3DES}"+DesEncryptTools.encrypt(password);
                shouldsave=true;
            }
        }else
        {
            if(password.startsWith("{3DES}"))
            {
                if(DesEncryptTools.IS_NEWKEY)
                {
                    throw new WabacusConfigLoadingException("密钥文件已经改变，但"+configfile+"中已有用旧密钥加密好的密码，它们将无法解密，请将它们先置成明文再换密钥文件");
                }
                if(DesEncryptTools.KEY_OBJ==null)
                {
                    throw new WabacusConfigLoadingException("没有在wabacus.cfg.xml中指定密钥文件,"+configfile+"中已加密的密码无法解密");
                }
                password=DesEncryptTools.decrypt(password.substring("{3DES}".length()));
                shouldsave=true;
            }
        }
        if(shouldsave)
        {
            Attribute attrPass=elePasswordProp.attribute("value");
            attrPass.setValue(password);
            try
            {
                XmlAssistant.getInstance().saveDocumentToXmlFile(configfile,doc);
            }catch(IOException e)
            {
                log.warn(configfile+"中的数据源密码加密失败，将存放明文的密码",e);
            }
        }
    }

    protected void finalize() throws Throwable
    {
        closePool();
        super.finalize();
    }
}
