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
package com.wabacus.config.resource.dataimport;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.resource.AbsResource;
import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.config.resource.dataimport.configbean.ColumnMapBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.dataimport.interceptor.IDataImportInterceptor;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class DataImportRes extends AbsResource
{
    public Object getValue(Element itemElement)
    {
        String key=itemElement.attributeValue("key");
        Element eleDataImport=itemElement.element("dataimport");
        if(eleDataImport==null)
        {
            throw new WabacusConfigLoadingException("在资源文件中配置的资源项"+key
                    +"不是有效的数据导入资源项，必须以<dataimport/>做为其顶层标签");
        }
        String filetype=eleDataImport.attributeValue("filetype");
        AbsDataImportConfigBean dataimportcbean=AbsDataImportConfigBean.createDataImportConfigBean(
                key,filetype);
        String filename=eleDataImport.attributeValue("filename");
        if(filename!=null)
        {
            dataimportcbean.setFilename(filename.trim());
        }
        String tablename=eleDataImport.attributeValue("tablename");
        if(tablename==null||tablename.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载KEY为"+key+"的数据导入资源项失败，必须配置tablename属性");
        }
        dataimportcbean.setTablename(tablename.trim());

        String importtype=eleDataImport.attributeValue("importtype");
        if(importtype==null||importtype.trim().equals(""))
            importtype=Consts_Private.DATAIMPORTTYPE_OVERWRITE;
        importtype=importtype.toLowerCase().trim();
        if(!importtype.equals(Consts_Private.DATAIMPORTTYPE_APPEND)
                &&!importtype.equals(Consts_Private.DATAIMPORTTYPE_OVERWRITE))
        {
            throw new WabacusConfigLoadingException("加载KEY为"+key+"的数据导入资源项失败，配置的importtype属性无效");
        }
        dataimportcbean.setImporttype(importtype);
        String keyfields=eleDataImport.attributeValue("keyfields");
        if(keyfields!=null&&!keyfields.trim().equals(""))
        {
            dataimportcbean.setLstKeyfields(Tools.parseStringToList(keyfields.toUpperCase(),";",false));
        }
        String filepath=eleDataImport.attributeValue("filepath");
        if(filepath==null||filepath.trim().equals(""))
        {
            filepath=Config.getInstance().getSystemConfigValue("default-dataimport-filepath","");
            if(filepath.equals(""))
                throw new WabacusConfigLoadingException(
                        "加载KEY为"
                                +key
                                +"的数据导入资源项失败，因为没有在wabacus.cfg.xml中配置default-dataimoprt-filepath，所以必须为此数据导入项配置filepath属性");
        }
        dataimportcbean.setFilepath(FilePathAssistant.getInstance().standardFilePath(filepath.trim()));
        String datasource=eleDataImport.attributeValue("datasource");
        if(datasource!=null)
        {
            dataimportcbean.setDatasource(datasource.trim());
        }
        String interceptor=eleDataImport.attributeValue("interceptor");
        if(interceptor!=null&&!interceptor.trim().equals(""))
        {
            try
            {
                Object o=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(interceptor.trim()).newInstance();
                if(!(o instanceof IDataImportInterceptor))
                {
                    throw new WabacusConfigLoadingException("加载KEY为"+key+"的数据导入资源项失败，配置的拦截器不是"
                            +IDataImportInterceptor.class.getName()+"类型");
                }
                dataimportcbean.setInterceptor((IDataImportInterceptor)o);
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("加载KEY为"+key+"的数据导入资源项失败，生成拦截器类"
                        +interceptor+"的对象失败",e);
            }
        }
        String autodetect=eleDataImport.attributeValue("autodetect");
        if(autodetect==null||!autodetect.trim().equals("false"))
        {
            Config.getInstance().addAutoDetectedDataImportBean(filepath,dataimportcbean);
        }
        
        Element eleColumnmap=eleDataImport.element("columnmap");
        if(eleColumnmap==null)
        {
            throw new WabacusConfigLoadingException("加载KEY为"+key+"的数据导入资源项失败，没有配置<columnmap/>标签");
        }
        ColumnMapBean cmbean=new ColumnMapBean(dataimportcbean);
        String matchmode=eleColumnmap.attributeValue("matchmode");
        if(matchmode!=null)
        {
            matchmode=matchmode.toLowerCase().trim();
            if(matchmode.equals("")) matchmode=Consts_Private.DATAIMPORT_MATCHMODE_INITIAL;
            if(!Consts_Private.LST_DATAIMPORT_MATCHMODES.contains(matchmode))
            {
                throw new WabacusConfigLoadingException("加载KEY为"+key+"的数据导入资源项失败，配置的matchmode："+matchmode+"不支持");
            }
            cmbean.setMatchmode(matchmode);
        }
        String type=eleColumnmap.attributeValue("type");
        if(type==null||type.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载KEY为"+key
                    +"的数据导入资源项失败，没有为<columnmap/>标签配置type属性");
        }
        type=type.trim().toLowerCase();
        List<String> lstTypes=Tools.parseStringToList(type,"=",false);
        if(lstTypes.size()==1)
        {
            String exclusive=eleColumnmap.attributeValue("exclusive");
            List<String> lstExclusives=null;
            if(exclusive!=null&&!exclusive.trim().equals(""))
            {
                lstExclusives=Tools.parseStringToList(exclusive.toUpperCase(),";",false);
            }
            if(lstTypes.get(0).equals("name"))
            {
                cmbean.setMaptype(ColumnMapBean.MAPTYPE_NAME);
                cmbean.setLstExclusiveColumns(lstExclusives);
            }else if(lstTypes.get(0).equals("index"))
            {
                cmbean.setMaptype(ColumnMapBean.MAPTYPE_INDEX);
                if(lstExclusives!=null)
                {
                    List lstTemp=new ArrayList();
                    for(String colTmp:lstExclusives)
                    {
                        if(colTmp.trim().equals("")) continue;
                        lstTemp.add(Integer.parseInt(colTmp.trim()));
                    }
                    if(lstTemp.size()>0)
                    {
                        cmbean.setLstExclusiveColumns(lstTemp);
                    }
                }
            }else
            {
                throw new WabacusConfigLoadingException("加载KEY为"+key
                        +"的数据导入资源项失败，为<columnmap/>标签配置type属性"+type+"不合法");
            }
        }else if(lstTypes.size()==2)
        {
            String dbcoltype=lstTypes.get(0).trim();
            String filecoltype=lstTypes.get(1).trim();
            String columnmaps=eleColumnmap.getText();
            if(columnmaps==null||columnmaps.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载KEY为"+key
                        +"的数据导入资源项失败，当<columnmap/>标签配置type属性为"+type+"时，没有配置映射字段");
            }
            columnmaps=Tools.formatStringBlank(columnmaps);
            if(dbcoltype.equals("name")&&filecoltype.equals("name"))
            {
                cmbean.setMaptype(ColumnMapBean.MAPTYPE_NAME_NAME);
            }else if(dbcoltype.equals("name")&&filecoltype.equals("index"))
            {
                cmbean.setMaptype(ColumnMapBean.MAPTYPE_NAME_INDEX);
            }else if(dbcoltype.equals("index")&&filecoltype.equals("name"))
            {
                cmbean.setMaptype(ColumnMapBean.MAPTYPE_INDEX_NAME);
            }else if(dbcoltype.equals("index")&&filecoltype.equals("index"))
            {
                cmbean.setMaptype(ColumnMapBean.MAPTYPE_INDEX_INDEX);
            }else
            {
                throw new WabacusConfigLoadingException("加载KEY为"+key
                        +"的数据导入资源项失败，为<columnmap/>标签配置type属性"+type+"不合法");
            }
            cmbean.parseColMaps(key,columnmaps);
        }else
        {
            throw new WabacusConfigLoadingException("加载KEY为"+key
                    +"的数据导入资源项失败，为<columnmap/>标签配置type属性不合法");
        }
        dataimportcbean.setColMapBean(cmbean);
        dataimportcbean.loadConfig(eleDataImport);
        dataimportcbean.doPostLoad();
        dataimportcbean.buildImportSqls();
        return dataimportcbean;
    }
}
