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

import org.dom4j.Element;

import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.dataimport.filetype.AbsFileTypeProcessor;
import com.wabacus.system.dataimport.filetype.XlsFileProcessor;
import com.wabacus.util.Tools;

public class XlsDataImportBean extends AbsDataImportConfigBean
{
    private String sheet;

//    private String datalayout="horizontal";//如果取值为horizontal表示记录是一行一行，如果取值为vertical，则记录是一列一列，默认为horizontal

    private int colnamerowindex=-1;

    private int startdatarowindex=-1;
    
    public String getSheet()
    {
        return sheet;
    }

    public void setSheet(String sheet)
    {
        this.sheet=sheet;
    }

    public int getStartdatarowindex()
    {
        return startdatarowindex;
    }

    public void setStartdatarowindex(int startdatarowindex)
    {
        this.startdatarowindex=startdatarowindex;
    }

    public int getColnamerowindex()
    {
        return colnamerowindex;
    }

    public void setColnamerowindex(int colnamerowindex)
    {
        this.colnamerowindex=colnamerowindex;
    }

    public boolean hasColName()
    {
        if(this.colnamerowindex>=0) return true;
        return false;
    }

    public void loadConfig(Element eleDataImport)
    {
        super.loadConfig(eleDataImport);
        String sheet=eleDataImport.attributeValue("sheet");
        if(sheet!=null&&!sheet.trim().equals(""))
        {
            this.sheet=sheet.trim();
            if(Tools.isDefineKey("index",sheet))
            {
                int isheet=Integer.parseInt(Tools.getRealKeyByDefine("index",sheet));
                if(isheet<0)
                {
                    throw new WabacusConfigLoadingException("加载KEY为"+reskey
                            +"的数据导入资源项失败，配置的sheet不是有效序号");
                }
            }
        }
//            datalayout=datalayout.toLowerCase().trim();
//            }
        try
        {
            String colnamerowindex=eleDataImport.attributeValue("colnamerowindex");
            if(colnamerowindex!=null&&!colnamerowindex.trim().equals(""))
            {
                this.colnamerowindex=Integer.parseInt(colnamerowindex.trim());
            }
            String startdatarowindex=eleDataImport.attributeValue("startdatarowindex");
            if(startdatarowindex!=null&&!startdatarowindex.trim().equals(""))
            {
                this.startdatarowindex=Integer.parseInt(startdatarowindex.trim());
            }
        }catch(NumberFormatException e)
        {
            throw new WabacusConfigLoadingException("加载KEY为"+reskey+"的数据导入资源项失败，配置的数据文件位置信息不是合法数字",
                    e);
        }
        
        if(this.hasColName())
        {
            if(this.startdatarowindex<0)
            {
                this.startdatarowindex=this.colnamerowindex+1;
            }else if(this.startdatarowindex<=this.colnamerowindex)
            {
                throw new WabacusConfigLoadingException(
                        "加载KEY为"
                                +reskey
                                +"的数据导入资源项失败，对于Horizontal布局的Excel数据，startnamecolindex的配置值必须小于startdatarowindex配置值");
            }
        }else
        {
            if(this.startdatarowindex<0) this.startdatarowindex=0;
        }
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        if(this.colMapBean.getFileMapType().equals("name"))
        {
            if(!this.hasColName())
            {
                throw new WabacusConfigLoadingException("加载KEY为"+reskey
                        +"的数据导入资源项失败，此数据文件没有字段名，无法根据它的名称进行映射");
            }
        }
    }

    public AbsFileTypeProcessor createDataImportProcessor()
    {
        return new XlsFileProcessor(this);
    }

}
