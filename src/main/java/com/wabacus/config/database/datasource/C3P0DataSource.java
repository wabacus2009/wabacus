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
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import com.mchange.v2.c3p0.DataSources;
import com.mchange.v2.c3p0.PoolConfig;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.util.DesEncryptTools;

public class C3P0DataSource extends AbsDataSource
{
    private static Log log=LogFactory.getLog(C3P0DataSource.class);
    
    private DataSource ds;

    public Connection getConnection()
    {
        try
        {
            log.debug("从数据源"+this.getName()+"获取数据库连接");
            return ds.getConnection();
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("获取"+this.getName()+"数据源的数据库连接失败",e);
        }
    }

    public DataSource getDataSource()
    {
        return this.ds;
    }
    
    public void closePool()
    {
        super.closePool();
        try
        {
            if(this.ds!=null)
            {
                log.debug("正在关闭C3P0连接池....................................................");
                DataSources.destroy(this.ds);
            }
            this.ds=null;
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("关闭c3p0数据源失败",e);
        }
    }

    public void loadConfig(Element eleDataSource)
    {
        super.loadConfig(eleDataSource);
        List lstEleProperties=eleDataSource.elements("property");
        if(lstEleProperties==null||lstEleProperties.size()==0)
        {
            throw new WabacusConfigLoadingException("没有为数据源："+this.getName()
                    +"配置alias、configfile等参数");
        }
        String driver=null;
        String url=null;
        String user=null;
        String password=null;

        int minPoolSize=1;
        int maxPoolSize=100;
        int maxIdleTime=0;
        int maxStatements=0;
        int acquireIncrement=1;
        int idleTestPeriod=0;
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
            if(value.equals(""))
            {
                continue;
            }
            if(name.equals("max_size"))
            {
                maxPoolSize=Integer.parseInt(value);
            }else if(name.equals("min_size"))
            {
                minPoolSize=Integer.parseInt(value);
            }else if(name.equals("timeout"))
            {
                maxIdleTime=Integer.parseInt(value);
            }else if(name.equals("max_statements"))
            {
                maxStatements=Integer.parseInt(value);
            }else if(name.equals("idle_test_period"))
            {
                idleTestPeriod=Integer.parseInt(value);
            }else if(name.equals("acquire_increment"))
            {
                acquireIncrement=Integer.parseInt(value);
            }else if(name.equals("driver"))
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
        if(driver.equals("")||url.equals("")||user.equals(""))
        {
            throw new WabacusConfigLoadingException("数据源："+this.getName()
                    +"配置的参数不完整，必须配置driver，url，user几个参数");
        }
        PoolConfig pcfg=new PoolConfig();
        pcfg.setInitialPoolSize(minPoolSize);
        pcfg.setMinPoolSize(minPoolSize);
        pcfg.setMaxPoolSize(maxPoolSize);
        pcfg.setAcquireIncrement(acquireIncrement);
        pcfg.setMaxIdleTime(maxIdleTime);
        pcfg.setMaxStatements(maxStatements);
        pcfg.setIdleConnectionTestPeriod(idleTestPeriod);

        Properties connectionProps=new Properties();
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
        connectionProps.setProperty("user",user);
        connectionProps.setProperty("password",password);
        try
        {
            Class.forName(driver);
            DataSource unpooled=DataSources.unpooledDataSource(url,connectionProps);
            this.ds=DataSources.pooledDataSource(unpooled,pcfg);
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("数据源："+this.getName()+"对象无法创建",e);
        }
    }

    protected void finalize() throws Throwable
    {
        closePool();
        super.finalize();
    }
}
