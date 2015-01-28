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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class ColumnMapBean
{
    public final static int MAPTYPE_NAME=1;

    public final static int MAPTYPE_INDEX=2;

    public final static int MAPTYPE_NAME_NAME=3;//字段名/字段名映射，左边为数据库，右边为数据文件

    public final static int MAPTYPE_NAME_INDEX=4;//字段名/字段位置映射，左边为数据库，右边为数据文件

    public final static int MAPTYPE_INDEX_NAME=5;//字段位置/字段名映射，左边为数据库，右边为数据文件

    public final static int MAPTYPE_INDEX_INDEX=6;//字段位置/字段位置映射，左边为数据库，右边为数据文件

    private int maptype;

    private String matchmode=Consts_Private.DATAIMPORT_MATCHMODE_INITIAL;
    
    private List lstExclusiveColumns;

    private List<Map> lstMapColumns;//当映射类型为3、4、5、6时，匹配字段名或字段序号的映射关系

    private AbsDataImportConfigBean diconfigbean;
    
    public ColumnMapBean(AbsDataImportConfigBean diconfigbean)
    {
        this.diconfigbean=diconfigbean;
    }
    
    public int getMaptype()
    {
        return maptype;
    }

    public void setMaptype(int maptype)
    {
        this.maptype=maptype;
    }

    public String getMatchmode()
    {
        return matchmode;
    }

    public void setMatchmode(String matchmode)
    {
        this.matchmode=matchmode;
    }

    public List getLstExclusiveColumns()
    {
        return lstExclusiveColumns;
    }

    public void setLstExclusiveColumns(List lstExclusiveColumns)
    {
        this.lstExclusiveColumns=lstExclusiveColumns;
    }

    public List<Map> getLstMapColumns()
    {
        return lstMapColumns;
    }

    public void setLstMapColumns(List<Map> lstMapColumns)
    {
        this.lstMapColumns=lstMapColumns;
    }

    public void parseColMaps(String key,String colMaps)
    {
        if(colMaps==null||colMaps.trim().equals("")) return;
        List<String> lstColumnmaps=Tools.parseStringToList(colMaps,";",false);
        this.lstMapColumns=new ArrayList<Map>();
        Map mColTmp;
        Map<String,String> mColMapTmp=new HashMap<String,String>();
        for(String colmapTmp:lstColumnmaps)
        {
            List<String> lstTmp=Tools.parseStringToList(colmapTmp,"=",false);
            if(lstTmp.size()!=2)
            {
                throw new WabacusConfigLoadingException("加载KEY为"+key
                        +"的数据导入资源项失败，为<columnmap/>配置的映射字段"+colMaps+"不合法");
            }
            String dbcol=lstTmp.get(0).toUpperCase().trim();
            String filecol=lstTmp.get(1).trim();
            if(dbcol.equals("")||filecol.equals(""))
            {
                throw new WabacusConfigLoadingException("加载KEY为"+key
                        +"的数据导入资源项失败，为<columnmap/>配置的映射字段"+colMaps+"不合法");
            }
            if(!Tools.isDefineKey("request",filecol)&&!Tools.isDefineKey("session",filecol))
            {
                filecol=filecol.toUpperCase();
            }
            if(mColMapTmp.containsKey(dbcol))
            {
                if(filecol.equals(mColMapTmp.get(dbcol)))
                {
                    continue;
                }else
                {
                    throw new WabacusConfigLoadingException("加载KEY为"+key
                            +"的数据导入资源项失败，为<columnmap/>标签配置的映射字段"+colMaps+"不合法，将数据库字段"+dbcol
                            +"映射到了数据文件的多个不同字段");
                }
            }
            mColTmp=new HashMap();
            
            if(maptype==ColumnMapBean.MAPTYPE_INDEX_INDEX)
            {
                mColTmp.put(Integer.parseInt(dbcol),Tools.isDefineKey("request",filecol)||Tools.isDefineKey("session",filecol)?filecol:Integer
                        .parseInt(filecol));
            }else if(maptype==ColumnMapBean.MAPTYPE_INDEX_NAME)
            {
                mColTmp.put(Integer.parseInt(dbcol),filecol);
            }else if(maptype==ColumnMapBean.MAPTYPE_NAME_INDEX)
            {
                mColTmp.put(dbcol,Tools.isDefineKey("request",filecol)||Tools.isDefineKey("session",filecol)?filecol:Integer.parseInt(filecol));
            }else
            {
                mColTmp.put(dbcol,filecol);
            }
            lstMapColumns.add(mColTmp);
        }
    }

    public List<DataImportSqlBean> createImportDataSqls(String dynimporttype)
    {
        Connection conn=Config.getInstance().getDataSource(diconfigbean.getDatasource()).getConnection();
        AbsDatabaseType dbtype=Config.getInstance().getDataSource(diconfigbean.getDatasource()).getDbType();
        Statement stmt=null;
        try
        {
            stmt=conn.createStatement();
            ResultSet rs=stmt.executeQuery("select * from "+diconfigbean.getTablename());
            ResultSetMetaData rsm=rs.getMetaData();
            int colcount=rsm.getColumnCount();
            if(colcount<=0)
            {
                rs.close();
                throw new WabacusConfigLoadingException("表"+diconfigbean.getTablename()+"没有列，不能对其进行传输");
            }
            Map<String,String> mAllColAndTypes=new HashMap<String,String>();
            List<Map<String,String>> lstAllColsAndTypes=new ArrayList<Map<String,String>>();//存放所有字段及类型，每个字段存在一个Map中，键为字段名，值为字段类型。
            Map<String,String> mTmp;
            for(int i=1;i<=colcount;i++)
            {
                mAllColAndTypes.put(rsm.getColumnName(i).toUpperCase(),rsm.getColumnTypeName(i));
                mTmp=new HashMap<String,String>();
                mTmp.put(rsm.getColumnName(i).toUpperCase(),rsm.getColumnTypeName(i));
                lstAllColsAndTypes.add(mTmp);
            }
            rs.close();

            List<DataImportSqlBean> lstImportSqlObjs=new ArrayList<DataImportSqlBean>();
            if(dynimporttype!=null&&dynimporttype.trim().equals(Consts_Private.DATAIMPORTTYPE_DELETE))
            {
                DataImportSqlBean importSbean=new DataImportSqlBean();
                if(dynimporttype.equals(Consts_Private.DATAIMPORTTYPE_OVERWRITE)
                        ||diconfigbean.getLstKeyfields()==null
                        ||diconfigbean.getLstKeyfields().size()==0)
                {
                    importSbean.setSql("delete from "+diconfigbean.getTablename());
                }else
                {//append模式，且配置了keyfields，且数据文件中有数据，则删除数据文件中对应数据的记录（数据文件没数据时，客户端不会调用这个方法来构造删除SQL语句）
                    importSbean=createDelOldRecordsSql(mAllColAndTypes,lstAllColsAndTypes,dbtype,
                            Consts_Private.DATAIMPORTTYPE_APPEND);
                }
                lstImportSqlObjs.add(importSbean);
            }else
            {
                DataImportSqlBean disqlBean_delete=createDelOldRecordsSql(mAllColAndTypes,
                        lstAllColsAndTypes,dbtype,dynimporttype);
                if(disqlBean_delete!=null)
                {
                    lstImportSqlObjs.add(disqlBean_delete);
                }
                lstImportSqlObjs.add(createInsertRecordsSql(mAllColAndTypes,lstAllColsAndTypes,
                        dbtype));
            }
            return lstImportSqlObjs;
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("建立数据导入SQL语句失败",e);
        }finally
        {
            WabacusAssistant.getInstance().release(conn,stmt);
        }
    }
    
    private DataImportSqlBean createDelOldRecordsSql(Map<String,String> mAllColAndTypes,
            List<Map<String,String>> lstAllColsAndTypes,AbsDatabaseType dbtype,String dynimporttype)
    {
        DataImportSqlBean importSbean=new DataImportSqlBean();
        if(dynimporttype==null||dynimporttype.trim().equals(""))
        {
            dynimporttype=this.diconfigbean.getImporttype();
        }
        if(dynimporttype.equals(Consts_Private.DATAIMPORTTYPE_OVERWRITE))
        {
            importSbean.setSql("delete from "+diconfigbean.getTablename());
        }else
        {//append模式或者是append模式下动态指定的delete模式
            List<String> lstKeyfields=diconfigbean.getLstKeyfields();
            if(lstKeyfields==null||lstKeyfields.size()==0)
            {
                return null;
            }
            StringBuffer sqlTmpBuf=new StringBuffer();
            sqlTmpBuf.append("delete from "+diconfigbean.getTablename()+" where ");
            List<IDataType> lstParamTypes=new ArrayList<IDataType>();
            List lstParamColsInFile=new ArrayList();
            String fieldTypeTmp;
            IDataType dtypeTmp;
            Object objTmp;
            for(String keyFieldTmp:lstKeyfields)
            {
                sqlTmpBuf.append(keyFieldTmp).append("=? and ");
                fieldTypeTmp=mAllColAndTypes.get(keyFieldTmp);
                if(fieldTypeTmp==null)
                {
                    throw new WabacusConfigLoadingException("数据导入项"+diconfigbean.getReskey()+"对应的表"
                            +diconfigbean.getTablename()+"不存在keyfields属性中配置的"+keyFieldTmp+"字段");
                }
                dtypeTmp=dbtype.getWabacusDataTypeByColumnType(fieldTypeTmp);
                if(dtypeTmp==null) dtypeTmp=Config.getInstance().getDataTypeByClass(VarcharType.class);
                lstParamTypes.add(dtypeTmp);
                objTmp=getFileColByDbCol(keyFieldTmp,lstAllColsAndTypes);
                if(objTmp==null)
                {
                    throw new WabacusConfigLoadingException("加载数据导入项"+diconfigbean.getReskey()
                            +"失败，在数据文件中没有取到数据库关键字段"+keyFieldTmp+"对应的字段");
                }
                lstParamColsInFile.add(objTmp);
            }
            String sql=sqlTmpBuf.toString().trim();
            if(sql.endsWith("and")) sql=sql.substring(0,sql.length()-3);
            importSbean.setSql(sql);
            importSbean.setLstParamTypes(lstParamTypes);
            importSbean.setLstParamColsInFile(lstParamColsInFile);
        }
        return importSbean;
    }
    
    public DataImportSqlBean createInsertRecordsSql(Map<String,String> mAllColsAndTypes,
            List<Map<String,String>> lstAllColsAndTypes,AbsDatabaseType dbtype)
    {
        switch (maptype)
        {
            case MAPTYPE_NAME:
                return createSql_automatch(lstAllColsAndTypes,dbtype,"name");
            case MAPTYPE_INDEX:
                return createSql_automatch(lstAllColsAndTypes,dbtype,"index");
            case MAPTYPE_NAME_NAME:
                return createSql_db_file(mAllColsAndTypes,lstAllColsAndTypes,dbtype,
                        "name","name");
            case MAPTYPE_NAME_INDEX:
                return createSql_db_file(mAllColsAndTypes,lstAllColsAndTypes,dbtype,
                        "name","index");
            case MAPTYPE_INDEX_NAME:
                return createSql_db_file(mAllColsAndTypes,lstAllColsAndTypes,dbtype,
                        "index","name");
            case MAPTYPE_INDEX_INDEX:
                return createSql_db_file(mAllColsAndTypes,lstAllColsAndTypes,dbtype,
                        "index","index");
            default:
                return null;
        }
    }

    private DataImportSqlBean createSql_automatch(List<Map<String,String>> lstAllColsAndTypes,
            AbsDatabaseType dbtype,String colmaptype)
    {
        StringBuffer sqlBuf=new StringBuffer();
        sqlBuf.append("insert into ").append(this.diconfigbean.getTablename()).append("(");
        StringBuffer colsBuf=new StringBuffer();
        List<IDataType> lstParamTypes=new ArrayList<IDataType>();
        List lstParamColsInFile=new ArrayList();
        IDataType dtypeTmp;
        Map<String,String> mColTmp;
        for(int i=0;i<lstAllColsAndTypes.size();i++)
        {
            if(colmaptype.equals("index")&&lstExclusiveColumns!=null&&lstExclusiveColumns.contains(i)) continue;
            mColTmp=lstAllColsAndTypes.get(i);
            String colNameTmp=mColTmp.keySet().iterator().next();
            if(colmaptype.equals("name")&&lstExclusiveColumns!=null&&lstExclusiveColumns.contains(colNameTmp)) continue;
            sqlBuf.append(colNameTmp).append(",");
            colsBuf.append("?,");
            dtypeTmp=dbtype.getWabacusDataTypeByColumnType(mColTmp.get(colNameTmp));
            if(dtypeTmp==null) dtypeTmp=Config.getInstance().getDataTypeByClass(VarcharType.class);
            lstParamTypes.add(dtypeTmp);
            if(colmaptype.equals("index"))
            {
                lstParamColsInFile.add(i);
            }else
            {
                lstParamColsInFile.add(colNameTmp);
            }
        }
        if(sqlBuf.charAt(sqlBuf.length()-1)==',')
        {
            sqlBuf.deleteCharAt(sqlBuf.length()-1);
        }
        if(colsBuf.charAt(colsBuf.length()-1)==',')
        {
            colsBuf.deleteCharAt(colsBuf.length()-1);
        }
        sqlBuf.append(") values(").append(colsBuf.toString()).append(")");
        DataImportSqlBean importSqlBean=new DataImportSqlBean();
        importSqlBean.setSql(sqlBuf.toString());
        importSqlBean.setLstParamColsInFile(lstParamColsInFile);
        importSqlBean.setLstParamTypes(lstParamTypes);
        return importSqlBean;
    }

    private DataImportSqlBean createSql_db_file(Map<String,String> mAllColsAndTypes,
            List<Map<String,String>> lstAllColsAndTypes,AbsDatabaseType dbtype,String dbcoltype,
            String filecoltype)
    {
        if(lstMapColumns==null||lstMapColumns.size()==0)
        {
            throw new WabacusConfigLoadingException("数据导入项："+diconfigbean.getReskey()
                    +"没有需要导入的字段");
        }
        StringBuffer sqlBuf=new StringBuffer();
        sqlBuf.append("insert into ").append(diconfigbean.getTablename()).append("(");
        StringBuffer colsBuf=new StringBuffer();
        List<IDataType> lstParamTypes=new ArrayList<IDataType>();
        List lstParamColsInFile=new ArrayList<String>();
        IDataType dtypeTmp;
        for(Map mColTmp:lstMapColumns)
        {
            if(mColTmp.size()==0) continue;
            Entry entry=(Entry)mColTmp.entrySet().iterator().next();
            Object dbcol=entry.getKey();//数据库端的字段配置
            Object filecol=entry.getValue();
            String dbcolname=null;
            if(dbcoltype.equals("index"))
            {
                int colidx=(Integer)dbcol;
                if(colidx>=lstAllColsAndTypes.size())
                {
                    throw new WabacusConfigLoadingException("数据导入项："
                            +diconfigbean.getReskey()+"配置的字段序号"+colidx+"超过表"
                            +diconfigbean.getTablename()+"的字段数");
                }
                dbcolname=lstAllColsAndTypes.get(colidx).keySet().iterator().next();
            }else
            {
                dbcolname=(String)dbcol;
            }
            String typename=mAllColsAndTypes.get(dbcolname);
            if(typename==null)
            {
                throw new WabacusConfigLoadingException("数据导入项："+diconfigbean.getReskey()
                        +"配置的字段"+dbcol+"在表"+diconfigbean.getTablename()+"中不存在");
            }
            dtypeTmp=dbtype.getWabacusDataTypeByColumnType(typename);
            if(dtypeTmp==null) dtypeTmp=Config.getInstance().getDataTypeByClass(VarcharType.class);
            lstParamTypes.add(dtypeTmp);
            sqlBuf.append(dbcolname).append(",");
            colsBuf.append("?,");
            lstParamColsInFile.add(filecol);
        }
        if(sqlBuf.charAt(sqlBuf.length()-1)==',') sqlBuf.deleteCharAt(sqlBuf.length()-1);
        if(colsBuf.charAt(colsBuf.length()-1)==',')  colsBuf.deleteCharAt(colsBuf.length()-1);
        sqlBuf.append(") values(").append(colsBuf.toString()).append(")");
        DataImportSqlBean importSqlBean=new DataImportSqlBean();
        importSqlBean.setSql(sqlBuf.toString());
        importSqlBean.setLstParamColsInFile(lstParamColsInFile);
        importSqlBean.setLstParamTypes(lstParamTypes);
        return importSqlBean;
    }

    public Object getFileColByDbCol(String dbColName,List<Map<String,String>> lstAllColsAndTypes)
    {
        switch (this.maptype)
        {
            case MAPTYPE_NAME:
                return dbColName;
            case MAPTYPE_INDEX:
                for(int i=0;i<lstAllColsAndTypes.size();i++)
                {
                    if(lstAllColsAndTypes.get(i).containsKey(dbColName))
                    {
                        return i;
                    }
                }
                return null;
            case MAPTYPE_NAME_NAME:
            case MAPTYPE_NAME_INDEX:
                for(Map mColTmp:lstMapColumns)
                {
                    if(mColTmp.containsKey(dbColName))
                    {
                        return mColTmp.get(dbColName);
                    }
                }
                return null;
            case MAPTYPE_INDEX_NAME:
            case MAPTYPE_INDEX_INDEX:
                int dbcolidx=-1;
                for(int i=0;i<lstAllColsAndTypes.size();i++)
                {
                    if(lstAllColsAndTypes.get(i).containsKey(dbColName))
                    {
                        dbcolidx=i;
                        break;
                    }
                }
                if(dbcolidx<0) return null;
                for(Map mColTmp:lstMapColumns)
                {
                    if(mColTmp.containsKey(dbcolidx))
                    {
                        return mColTmp.get(dbcolidx);
                    }
                }
                return null;
            default:
                return null;
        }
    }
    
    public String getFileMapType()
    {
        switch (this.maptype)
        {
            case MAPTYPE_INDEX:
            case MAPTYPE_NAME_INDEX:
            case MAPTYPE_INDEX_INDEX:
                return "index";
            default:
                return "name";
        }
    }
}
