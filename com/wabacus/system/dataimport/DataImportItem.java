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
package com.wabacus.system.dataimport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.config.resource.dataimport.configbean.DataImportSqlBean;
import com.wabacus.exception.WabacusDataImportException;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.dataimport.filetype.AbsFileTypeProcessor;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class DataImportItem
{
    private static Log log=LogFactory.getLog(DataImportItem.class);

    private AbsDataImportConfigBean configBean;

    AbsFileTypeProcessor fileProcessor;
    
    private File datafileObj;

    private String dynimportype;//通过数据文件名动态指定的导入模式，可指定append/overwrite/delete三种之一，如果没有指定，则使用静态配置的导入类型进行导入

    private int batchupdatesize=0;
    
    private List<String> lstColNamesTrace;
    
    private List lstErrorColValuesTrace;
    
    private String errorSqlTrace;
    
    private HttpServletRequest request;
    
    private HttpSession session;
    
    public HttpServletRequest getRequest()
    {
        return request;
    }

    public void setRequest(HttpServletRequest request)
    {
        this.request=request;
    }

    public HttpSession getSession()
    {
        return session;
    }

    public void setSession(HttpSession session)
    {
        this.session=session;
    }

    public DataImportItem(AbsDataImportConfigBean configBean,File datafileObj)
    {
        this.configBean=configBean;
        this.datafileObj=datafileObj;
        if(datafileObj==null)
        {
            throw new WabacusDataImportException("初始化传输项失败，没有传入数据文件对象");
        }
        String strbatchupdatesize=Config.getInstance().getSystemConfigValue("dataimport-batchupdate-size","10");
        try
        {
            this.batchupdatesize=Integer.parseInt(strbatchupdatesize);
        }catch(NumberFormatException e)
        {
            log.warn("在wabacus.cfg.xml中配置的dataimport-batchupdate-size："+strbatchupdatesize+"不是有效数字");
            this.batchupdatesize=10;
        }
    }

    public String getDynimportype()
    {
        return dynimportype;
    }

    public void setDynimportype(String dynimportype)
    {
        this.dynimportype=dynimportype;
    }

    public AbsDataImportConfigBean getConfigBean()
    {
        return configBean;
    }

    public AbsFileTypeProcessor getFileProcessor()
    {
        return fileProcessor;
    }

    public File getDatafileObj()
    {
        return datafileObj;
    }

    public void doImportData()
    {
        fileProcessor=configBean.createDataImportProcessor();
        if(datafileObj==null)
        {
            throw new WabacusDataImportException("导入数据导入项"+configBean.getReskey()+"失败，没有取到数据文件");
        }
        if(!datafileObj.exists()||datafileObj.isDirectory())
        {
            throw new WabacusDataImportException("导入数据导入项"+configBean.getReskey()+"失败，数据文件"+datafileObj.getAbsolutePath()+"文件不存在或是目录");
        }
        Connection conn=null;
        try
        {
            fileProcessor.init(datafileObj);
            if(configBean.getInterceptor()!=null)
            {
                boolean flag=configBean.getInterceptor().doImportStart(this);
                if(!flag) return;
            }
            conn=Config.getInstance().getDataSource(configBean.getDatasource()).getConnection();
            AbsDatabaseType dbtype=Config.getInstance().getDataSource(configBean.getDatasource()).getDbType();
            if(dynimportype==null||dynimportype.trim().equals("")||dynimportype.trim().equals(configBean.getImporttype()))
            {//如果没有动态指定导入类型，或者动态指定的与静态配置的一致
                doUpdateData(conn,dbtype,configBean.getLstImportSqlObjs(null));
            }else if(dynimportype.trim().equals(Consts_Private.DATAIMPORTTYPE_APPEND)
                    ||dynimportype.trim().equals(Consts_Private.DATAIMPORTTYPE_OVERWRITE))
            {
                doUpdateData(conn,dbtype,configBean.getLstImportSqlObjs(dynimportype));
            }else if(dynimportype.trim().equals(Consts_Private.DATAIMPORTTYPE_DELETE))
            {
                doDeleteData(conn,dbtype);
            }else
            {
                log.warn("为数据文件"+this.datafileObj.getAbsolutePath()+"动态指定的导入类型无效，将采用静态配置的导入类型对其进行数据导入");
                doUpdateData(conn,dbtype,configBean.getLstImportSqlObjs(null));
            }
            log.info("导入数据文件"+datafileObj.getAbsolutePath()+"到导入项"+configBean.getReskey()+"成功");
            try
            {
                if(configBean.getInterceptor()!=null)
                {
                    configBean.getInterceptor().doImportEnd(1,this,null);
                }
            }catch(Exception e)
            {
                throw new WabacusDataImportException("导入数据文件"+datafileObj.getAbsolutePath()+"时，执行后置动作失败",e);
            }
            if(!conn.getAutoCommit()) conn.commit();
        }catch(Exception e)
        {
            if(configBean.getInterceptor()!=null)
            {
                configBean.getInterceptor().doImportEnd(-1,this,e);
            }
            StringBuffer errorBuf=new StringBuffer();
            if(this.errorSqlTrace!=null&&!this.errorSqlTrace.trim().equals(""))
            {
                errorBuf.append("导入数据时执行SQL："+this.errorSqlTrace+"出错，");
            }
            if(this.lstErrorColValuesTrace!=null&&this.lstErrorColValuesTrace.size()>0)
            {
                errorBuf.append("此时正在导入数据：");
                for(int i=0;i<this.lstErrorColValuesTrace.size();i++)
                {
                    errorBuf.append("[");
                    if(this.lstColNamesTrace!=null&&this.lstColNamesTrace.size()==this.lstErrorColValuesTrace.size())
                    {
                        errorBuf.append(this.lstColNamesTrace.get(i)).append(":");
                    }
                    errorBuf.append(this.lstErrorColValuesTrace.get(i)).append("]");
                }
            }else if(this.batchupdatesize!=1)
            {
                errorBuf.append("如果希望准确了解是哪一条记录导致出错，请将wabacus.cfg.xml的dataimport-batchupdate-size参数配置为1");
            }
            log.error(errorBuf.toString(),e);
            throw new WabacusDataImportException("导入数据文件"+datafileObj.getAbsolutePath()+"失败");
        }finally
        {
            fileProcessor.destroy();
            WabacusAssistant.getInstance().release(conn,null);
        }
    }

    private void doDeleteData(Connection conn,AbsDatabaseType dbtype)
            throws SQLException
    {
        PreparedStatement pstmtDelete=null;
        try
        {
            DataImportSqlBean disqlbean=null;
            if(fileProcessor.isEmpty())
            {//如果没有数据，则不论是哪种导入类型，都将清空表中所有数据
                disqlbean=new DataImportSqlBean();
                disqlbean.setSql("delete from "+configBean.getTablename());
            }else
            {
                disqlbean=configBean.getLstImportSqlObjs(dynimportype).get(0);
            }
            log.debug(disqlbean.getSql());
            this.errorSqlTrace=disqlbean.getSql();
            pstmtDelete=conn.prepareStatement(disqlbean.getSql());
            if(disqlbean.getLstParamColsInFile()==null||disqlbean.getLstParamColsInFile().size()==0)
            {
                pstmtDelete.executeUpdate();
            }else
            {
                boolean matchFileIndex=configBean.getColMapBean().getFileMapType().equals("index");
                List<String> lstColNames=getLstColNames(matchFileIndex);
                this.lstColNamesTrace=lstColNames;
                boolean hasUnCommitData=false;//是否存在没有提交的数据
                List lstDataColValuesPerRow;
                Map<String,Object> mDataColValues=null;
                int i=fileProcessor.getStartrecordindex();
                for(int len=fileProcessor.getStartrecordindex()+fileProcessor.getRecordcount();i<len;i++)
                {
                    lstDataColValuesPerRow=fileProcessor.getRowData(i);
                    if(lstDataColValuesPerRow==null||lstDataColValuesPerRow.size()==0) continue;
                    if(configBean.getInterceptor()!=null)
                    {
                        boolean flag=configBean.getInterceptor().beforeImportRow(conn,this,lstColNames,lstDataColValuesPerRow);
                        if(!flag) continue;
                    }
                    if(!matchFileIndex)
                    {
                        mDataColValues=getAllColTitleAndValueMap(lstColNames,lstDataColValuesPerRow);
                    }
                    updateDBRowData(pstmtDelete,dbtype,disqlbean.getLstParamColsInFile(),disqlbean.getLstParamTypes(),matchFileIndex,
                            lstDataColValuesPerRow,mDataColValues);
                    if(configBean.getInterceptor()!=null)
                    {
                        configBean.getInterceptor().afterImportRow(conn,this,lstColNames,lstDataColValuesPerRow);
                    }
                    hasUnCommitData=true;
                    if(shouldBatchCommit(i))
                    {
                        pstmtDelete.executeBatch();
                        hasUnCommitData=false;
                    }
                }
                if(hasUnCommitData)
                {
                    pstmtDelete.executeBatch();
                }
            }
        }finally
        {
            fileProcessor.destroy();
            WabacusAssistant.getInstance().release(null,pstmtDelete);
        }
    }

    private boolean shouldBatchCommit(int rownum)
    {
        if(this.batchupdatesize==1) return true;//如果是1，则表示每条都单独提交
        if(this.batchupdatesize>0&&rownum!=0&&rownum%this.batchupdatesize==0) return true;
        return false;
    }
    
    private void doUpdateData(Connection conn,AbsDatabaseType dbtype,List<DataImportSqlBean> lstDisqlbean)
            throws SQLException
    {
        PreparedStatement pstmtDelete=null;
        PreparedStatement pstmtInsert=null;
        try
        {
            String importype=null;
            if(this.dynimportype!=null
                    &&(this.dynimportype.trim().equals(Consts_Private.DATAIMPORTTYPE_APPEND)||this.dynimportype.trim().equals(
                            Consts_Private.DATAIMPORTTYPE_OVERWRITE)))
            {
                importype=this.dynimportype.trim();
            }else
            {
                importype=configBean.getImporttype();
            }
            if(fileProcessor.isEmpty())  return;
            if(importype.equals(Consts_Private.DATAIMPORTTYPE_OVERWRITE))
            {
                DataImportSqlBean disqlbean=lstDisqlbean.get(0);
                log.debug(disqlbean.getSql());
                this.errorSqlTrace=disqlbean.getSql();
                pstmtDelete=conn.prepareStatement(disqlbean.getSql());
                pstmtDelete.executeUpdate();
                disqlbean=lstDisqlbean.get(1);
                log.debug(disqlbean.getSql());
                pstmtInsert=conn.prepareStatement(disqlbean.getSql());
                updateRowDataToDB(dbtype,conn,null,pstmtInsert,null,disqlbean);
            }else
            {
                DataImportSqlBean disqlbeanDelete=null;
                DataImportSqlBean disqlbeanInsert=null;
                if(lstDisqlbean.size()==2)
                {//配置了keyfields，需要先删除旧数据
                    disqlbeanDelete=lstDisqlbean.get(0);
                    log.debug(disqlbeanDelete.getSql());
                    pstmtDelete=conn.prepareStatement(disqlbeanDelete.getSql());
                    disqlbeanInsert=lstDisqlbean.get(1);
                }else
                {
                    disqlbeanInsert=lstDisqlbean.get(0);
                }
                log.debug(disqlbeanInsert.getSql());
                pstmtInsert=conn.prepareStatement(disqlbeanInsert.getSql());
                updateRowDataToDB(dbtype,conn,pstmtDelete,pstmtInsert,disqlbeanDelete,disqlbeanInsert);
            }
        }finally
        {
            WabacusAssistant.getInstance().release(null,pstmtDelete);
            WabacusAssistant.getInstance().release(null,pstmtInsert);
        }
    }

    private void updateRowDataToDB(AbsDatabaseType dbtype,Connection conn,PreparedStatement pstmtDelete,PreparedStatement pstmtInsert,
            DataImportSqlBean disqlbeanDelete,DataImportSqlBean disqlbeanInsert) throws SQLException
    {
        List lstColsInFileDelete=null;
        List<IDataType> lstColTypeDelete=null;
        if(disqlbeanDelete!=null)
        {
            lstColsInFileDelete=disqlbeanDelete.getLstParamColsInFile();
            lstColTypeDelete=disqlbeanDelete.getLstParamTypes();
        }
        List lstColsInFileInsert=disqlbeanInsert.getLstParamColsInFile();
        List<IDataType> lstColTypeInsert=disqlbeanInsert.getLstParamTypes();
        boolean matchFileIndex=configBean.getColMapBean().getFileMapType().equals("index");
        List<String> lstColNames=this.getLstColNames(matchFileIndex);//存放数据文件中所有字段名列表
        boolean hasUnCommitData=false;
        List lstDataColValuesPerRow;
        Map<String,Object> mDataColValues=null;
        int i=fileProcessor.getStartrecordindex();
        for(int len=fileProcessor.getStartrecordindex()+fileProcessor.getRecordcount();i<len;i++)
        {
            lstDataColValuesPerRow=fileProcessor.getRowData(i);
            if(lstDataColValuesPerRow==null||lstDataColValuesPerRow.size()==0) continue;
            if(configBean.getInterceptor()!=null)
            {
                boolean flag=configBean.getInterceptor().beforeImportRow(conn,this,lstColNames,lstDataColValuesPerRow);
                if(!flag) continue;
            }
            if(!matchFileIndex)
            {
                mDataColValues=getAllColTitleAndValueMap(lstColNames,lstDataColValuesPerRow);
            }
            if(pstmtDelete!=null)
            {
                this.errorSqlTrace=disqlbeanDelete.getSql();
                updateDBRowData(pstmtDelete,dbtype,lstColsInFileDelete,lstColTypeDelete,matchFileIndex,lstDataColValuesPerRow,mDataColValues);
                if(this.shouldBatchCommit(i))
                {
                    pstmtDelete.executeBatch();
                }
            }
            this.errorSqlTrace=disqlbeanInsert.getSql();
            updateDBRowData(pstmtInsert,dbtype,lstColsInFileInsert,lstColTypeInsert,matchFileIndex,lstDataColValuesPerRow,mDataColValues);
            hasUnCommitData=true;
            if(this.shouldBatchCommit(i))
            {
                hasUnCommitData=false;
                pstmtInsert.executeBatch();
            }
            if(configBean.getInterceptor()!=null)
            {
                configBean.getInterceptor().afterImportRow(conn,this,lstColNames,lstDataColValuesPerRow);
            }
        }
        if(hasUnCommitData)
        {//上面循环中还存在没有提交的pstmt
            if(pstmtDelete!=null)
            {
                this.errorSqlTrace=disqlbeanDelete.getSql();
                pstmtDelete.executeBatch();
            }
            this.errorSqlTrace=disqlbeanInsert.getSql();
            pstmtInsert.executeBatch();
        }
    }

    private List<String> getLstColNames(boolean matchFileIndex)
    {
        List<String> lstColNames=fileProcessor.getLstColnameData();
        if(matchFileIndex||lstColNames==null||lstColNames.size()==0)
        {
            return lstColNames;
        }
        List<String> lstColNameResults=new ArrayList<String>();
        for(String colNameTmp:lstColNames)
        {
            if(colNameTmp==null)
            {
                lstColNameResults.add(null);
            }else
            {
                lstColNameResults.add(colNameTmp.trim().toUpperCase());
            }
        }
        return lstColNameResults;
    }

    public void backupOrDeleteDataFile()
    {
        backupOrDeleteDataFile(datafileObj);
        datafileObj=null;
    }

    public static void backupOrDeleteDataFile(File fileObj)
    {
        try
        {
            if(fileObj==null||!fileObj.exists())
            {
                return;
            }
            String backuppath=Config.getInstance().getSystemConfigValue("dataimport-backuppath","");
            if(backuppath.equals(""))
            {
                log.debug("正在删除数据文件"+fileObj.getAbsolutePath());
            }else
            {//配置了备份路径，则移到备份目录中
                String filename=fileObj.getName();
                int idxdot=filename.lastIndexOf(".");
                if(idxdot>0)
                {
                    filename=filename.substring(0,idxdot)+"_"+Tools.getStrDatetime("yyyyMMddHHmmss",new Date())+filename.substring(idxdot);
                }else
                {
                    filename=filename+"_"+Tools.getStrDatetime("yyyyMMddHHmmss",new Date());
                }
                File destFile=new File(FilePathAssistant.getInstance().standardFilePath(backuppath+File.separator+filename));
                log.debug("正在将数据文件"+fileObj.getAbsolutePath()+"备份到"+backuppath+"目录中");
                BufferedInputStream bis=null;
                BufferedOutputStream bos=null;
                try
                {
                    bis=new BufferedInputStream(new FileInputStream(fileObj));
                    bos=new BufferedOutputStream(new FileOutputStream(destFile));
                    byte[] btmp=new byte[1024];
                    while(bis.read(btmp)!=-1)
                    {
                        bos.write(btmp);
                    }
                }catch(Exception e)
                {
                    log.error("备份数据文件"+fileObj.getAbsolutePath()+"失败",e);
                }finally
                {
                    try
                    {
                        if(bis!=null) bis.close();
                    }catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                    try
                    {
                        if(bos!=null) bos.close();
                    }catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            fileObj.delete();
        }catch(Exception e)
        {
            log.error("备份或删除数据文件"+fileObj.getAbsolutePath()+"失败",e);
        }
    }

    private Map<String,Object> getAllColTitleAndValueMap(List<String> lstColNames,List lstDataColValues)
    {
        Map<String,Object> mDataColValues=null;
        mDataColValues=new HashMap<String,Object>();
        for(int k=0;k<lstColNames.size();k++)
        {
            mDataColValues.put(lstColNames.get(k),lstDataColValues.get(k));
        }
        return mDataColValues;
    }

    private void updateDBRowData(PreparedStatement pstmt,AbsDatabaseType dbtype,List lstColsInFile,List<IDataType> lstColType,boolean matchFileIndex,
            List lstDataColValuesPerRow,Map<String,Object> mDataColValues) throws SQLException
    {
        Object objDataVal,objCol;
        IDataType varcharTypeObj=Config.getInstance().getDataTypeByClass(VarcharType.class);
        if(this.batchupdatesize==1)this.lstErrorColValuesTrace=lstDataColValuesPerRow;
        for(int i=0;i<lstColsInFile.size();i++)
        {
            objCol=lstColsInFile.get(i);
            objDataVal=null;
            if(Tools.isDefineKey("request",String.valueOf(objCol)))
            {
                if(request==null)
                {
                    log.warn("导入数据时需要从request中取"+objCol+"对应的数据，但request对象为null，无法获取其中的导入数据");
                }else
                {
                    objDataVal=WabacusAssistant.getInstance().getValueFromRequest(request,String.valueOf(objCol));
                }
            }else if(Tools.isDefineKey("session",String.valueOf(objCol)))
            {
                if(session==null)
                {
                    log.warn("导入数据时需要从session中取"+objCol+"对应的数据，但session对象为null，无法获取其中的导入数据");
                }else
                {
                    objDataVal=WabacusAssistant.getInstance().getValueFromSession(session,String.valueOf(objCol));
                }
            }else if(matchFileIndex)
            {
                if((Integer)objCol<lstDataColValuesPerRow.size()) objDataVal=lstDataColValuesPerRow.get((Integer)objCol);
            }else
            {
                objDataVal=mDataColValues.get(objCol);
            }
            if(lstColType.get(i) instanceof AbsDateTimeType)
            {
                if(objDataVal instanceof Date)
                {
                    ((AbsDateTimeType)lstColType.get(i)).setPreparedStatementValue(i+1,(Date)objDataVal,pstmt);
                }else
                {//当作字符串处理
                    varcharTypeObj.setPreparedStatementValue(i+1,String.valueOf(objDataVal),pstmt,dbtype);
                }
            }else
            {
                lstColType.get(i).setPreparedStatementValue(i+1,(String)objDataVal,pstmt,dbtype);
            }
        }
        pstmt.addBatch();
    }
}
