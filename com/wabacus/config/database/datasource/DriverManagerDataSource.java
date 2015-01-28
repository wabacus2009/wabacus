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

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.util.DesEncryptTools;

public class DriverManagerDataSource extends AbsDataSource
{
    private static Log log=LogFactory.getLog(DriverManagerDataSource.class);

    private String driver;

    private String url;

    private String user;

    private String password;

    public String getDriver()
    {
        return driver;
    }

    public void setDriver(String driver)
    {
        this.driver=driver;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url=url;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user=user;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password=password;
    }

    public Connection getConnection()
    {
        try
        {
            log.debug("从数据源"+this.getName()+"获取数据库连接");
            Class.forName(this.driver).newInstance();
            return DriverManager.getConnection(url,user,password);
        }catch(Exception e)
        {
            log.error("获取数据库连接失败",e);
        }
        return null;
    }

    public DataSource getDataSource()
    {
        throw new WabacusRuntimeException("不能从DriverManagerDataSource数据源类型中获取javax.sql.DataSource对象");
    }
    
    public void loadConfig(Element eleDataSource)
    {
        super.loadConfig(eleDataSource);
        List lstEleProperties=eleDataSource.elements("property");
        if(lstEleProperties==null||lstEleProperties.size()==0)
        {
            throw new WabacusConfigLoadingException("没有为数据源："+this.getName()+"配置driver、url、user等参数");
        }
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
            if(name.equals("driver"))
            {
                driver=value;
            }else if(name.equals("url"))
            {
                url=value;
            }else if(name.equals("user"))
            {
                user=value;
            }else if(name.equals("password"))
            {
                password=value;
            }
        }
        driver=driver==null?"":driver.trim();
        url=url==null?"":url.trim();
        user=user==null?"":user.trim();
        password=password==null?"":password.trim();
        if(password.startsWith("{3DES}"))
        {
            password=password.substring("{3DES}".length());
            if(DesEncryptTools.KEY_OBJ==null)
            {
                throw new WabacusConfigLoadingException("没有取到密钥文件，无法完成数据库密码解密操作");
            }
            password=DesEncryptTools.decrypt(password);
        }
        if(driver.equals("")||url.equals("")||user.equals(""))
        {
            throw new WabacusConfigLoadingException("数据源："+this.getName()+"配置的参数不完整，必须配置driver,url,user几个参数");
        }
    }

}
