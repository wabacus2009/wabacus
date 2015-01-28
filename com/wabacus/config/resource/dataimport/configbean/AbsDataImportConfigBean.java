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
package com.wabacus.config.resource.dataimport.configbean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.dataimport.filetype.AbsFileTypeProcessor;
import com.wabacus.system.dataimport.interceptor.IDataImportInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.RegexTools;
import com.wabacus.util.Tools;

public abstract class AbsDataImportConfigBean
{
    private static Log log=LogFactory.getLog(AbsDataImportConfigBean.class);
    
    public final static String FILE_TYPE_EXCEL="excel";

    protected String reskey;

    protected String filename;

    protected String filepath;

    protected String importtype=Consts_Private.DATAIMPORTTYPE_OVERWRITE;//导入类型，可取值为overwrite/append，如果是append，则必须配置keyfields 

    protected String tablename;//导入的表名

    protected List<String> lstKeyfields;

    protected IDataImportInterceptor interceptor;

    protected ColumnMapBean colMapBean;

    private String datasource;

    private Map<String,List<DataImportSqlBean>> mImportSqlObjs;//存放每条要执行的sql以及相应的参数类型，加载时根据用户配置生成，key为导入类型，这里主要是考虑动态导入指定导入类型的情况，这样每次动态指定导入类型时，就不用每次去构造导入SQL语句

    public String getReskey()
    {
        return reskey;
    }

    public void setReskey(String reskey)
    {
        this.reskey=reskey;
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename=filename;
    }

    public String getFilepath()
    {
        return filepath;
    }

    public void setFilepath(String filepath)
    {
        this.filepath=filepath;
    }

    public String getImporttype()
    {
        return importtype;
    }

    public void setImporttype(String importtype)
    {
        this.importtype=importtype;
    }

    public String getTablename()
    {
        return tablename;
    }

    public void setTablename(String tablename)
    {
        this.tablename=tablename;
    }

    public String getDatasource()
    {
        return datasource;
    }

    public void setDatasource(String datasource)
    {
        this.datasource=datasource;
    }

    public List<String> getLstKeyfields()
    {
        return lstKeyfields;
    }

    public void setLstKeyfields(List<String> lstKeyfields)
    {
        this.lstKeyfields=lstKeyfields;
    }

    public IDataImportInterceptor getInterceptor()
    {
        return interceptor;
    }

    public void setInterceptor(IDataImportInterceptor interceptor)
    {
        this.interceptor=interceptor;
    }

    public ColumnMapBean getColMapBean()
    {
        return colMapBean;
    }

    public void setColMapBean(ColumnMapBean colMapBean)
    {
        this.colMapBean=colMapBean;
    }

    public List<DataImportSqlBean> getLstImportSqlObjs(String dynimporttype)
    {
        if(dynimporttype==null||dynimporttype.trim().equals(""))
        {
            if(colMapBean.getMatchmode().equals(Consts_Private.DATAIMPORT_MATCHMODE_INITIAL))
            {
                return this.mImportSqlObjs.get(Consts.DEFAULT_KEY);
            }else if(colMapBean.getMatchmode().equals(Consts_Private.DATAIMPORT_MATCHMODE_LAZY))
            {
                List<DataImportSqlBean> lstDataImportDataSqls=this.mImportSqlObjs.get(Consts.DEFAULT_KEY);
                if(lstDataImportDataSqls==null)
                {
                    log.debug("正在为数据导入资源项"+this.getReskey()+"建立数据文件和数据表映射...");
                    lstDataImportDataSqls=colMapBean.createImportDataSqls(null);
                    this.mImportSqlObjs.put(Consts.DEFAULT_KEY,lstDataImportDataSqls);
                }
                return lstDataImportDataSqls;
            }else if(colMapBean.getMatchmode().equals(Consts_Private.DATAIMPORT_MATCHMODE_EVERYTIME))
            {//每次导数据时建立SQL语句
                log.debug("正在为数据导入资源项"+this.getReskey()+"建立数据文件和数据表映射...");
                return colMapBean.createImportDataSqls(null);
            }else
            {
                return null;
            }
        }else
        {
            List<DataImportSqlBean> lstDataImportDataSqls=this.mImportSqlObjs.get(dynimporttype);
            if(lstDataImportDataSqls==null)
            {
                log.debug("正在为数据导入资源项"+this.getReskey()+"建立数据文件和数据表映射...");
                lstDataImportDataSqls=colMapBean.createImportDataSqls(dynimporttype);
                this.mImportSqlObjs.put(dynimporttype,lstDataImportDataSqls);
            }
            return lstDataImportDataSqls;
        }
    }

    public void loadConfig(Element eleDataImport)
    {}

    public void buildImportSqls()
    {
        if(this.mImportSqlObjs==null) this.mImportSqlObjs=new HashMap<String,List<DataImportSqlBean>>();
        if(Consts_Private.DATAIMPORT_MATCHMODE_INITIAL.equals(colMapBean.getMatchmode()))
        {
            this.mImportSqlObjs.put(Consts.DEFAULT_KEY,colMapBean.createImportDataSqls(null));
        }
    }

    public static AbsDataImportConfigBean createDataImportConfigBean(String key,String filetype)
    {
        if(filetype==null||filetype.trim().equals(""))
        {
            filetype=FILE_TYPE_EXCEL;
        }
        AbsDataImportConfigBean dicbean=null;
        if(filetype.equals(FILE_TYPE_EXCEL))
        {
            dicbean=new XlsDataImportBean();
        }else
        {
            throw new WabacusConfigLoadingException("加载数据导入资源项"+key+"失败，配置的数据文件类型"+filetype+"不支持");
        }
        dicbean.setReskey(key);
        return dicbean;
    }

    public abstract AbsFileTypeProcessor createDataImportProcessor();

    public void doPostLoad()
    {
        if(filename==null||filename.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("数据导入项"+reskey+"没有配置filename属性");
        }
        if(Tools.isDefineKey("pattern",filename))
        {
            if(Tools.getRealKeyByDefine("pattern",filename).trim().equals(""))
            {
                throw new WabacusConfigLoadingException("数据导入项"+reskey+"没有配置filename属性");
            }
        }
    }

    public boolean isMatch(String realfilename)
    {
        if(realfilename==null||realfilename.trim().equals("")) return false;
        if(Tools.isDefineKey("pattern",this.filename))
        {
            String filepattern=Tools.getRealKeyByDefine("pattern",this.filename);
            return RegexTools.isMatch(realfilename,filepattern);
        }else
        {
            return realfilename.trim().equals(this.filename);
        }
    }
}
