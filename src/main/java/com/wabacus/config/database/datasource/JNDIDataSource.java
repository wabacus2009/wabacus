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
import java.sql.SQLException;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import com.wabacus.exception.WabacusConfigLoadingException;

public class JNDIDataSource extends AbsDataSource
{
    private static Log log=LogFactory.getLog(JNDIDataSource.class);

    private String jndi;

    private DataSource ds;

    public String getJndi()
    {
        return jndi;
    }

    public void setJndi(String jndi)
    {
        this.jndi=jndi;
    }

    public Connection getConnection()
    {
        Connection conn=null;
        try
        {
            log.debug("从数据源"+this.getName()+"获取数据库连接");
            conn=getDataSource().getConnection();
        }catch(SQLException e)
        {
            log.error("获取数据源发生异常",e);
        }
        return conn;
    }

    public DataSource getDataSource()
    {
        Context context=null;
        try
        {
            if(ds!=null)
            {
                return ds;
            }
            context=new InitialContext();
            ds=(DataSource)context.lookup(jndi);
            return ds;
        }catch(Exception e)
        {
            log.error("获取数据源发生异常",e);
            return null;
        }finally
        {
            try
            {
                if(context!=null)
                {
                    context.close();
                }
            }catch(Exception ex)
            {
                log.error("获取数据源发生异常",ex);
            }
        }

    }
    
    public void loadConfig(Element eleDataSource)
    {
        super.loadConfig(eleDataSource);
        List lstEleProperties=eleDataSource.elements("property");
        if(lstEleProperties==null||lstEleProperties.size()==0)
        {
            throw new WabacusConfigLoadingException("没有为数据源："+this.getName()
                    +"配置jndi参数");
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
            if(name.equals("jndi"))
            {
                jndi=value;
                break;
            }
        }
        if(jndi==null||jndi.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("数据源："+this.getName()+"配置的jndi值为空");
        }
    }
}
