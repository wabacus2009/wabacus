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
package com.wabacus.system.dataimport.filetype;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.config.resource.dataimport.configbean.XlsDataImportBean;
import com.wabacus.exception.WabacusDataImportException;
import com.wabacus.util.Tools;

public class XlsFileProcessor extends AbsFileTypeProcessor
{
    private static Log log=LogFactory.getLog(XlsFileProcessor.class);

    private Workbook workbook;
    
    private BufferedInputStream bis;

    private Sheet sheetObj;

    private IXlsDataLayout layoutObj;

    private XlsDataImportBean xlsConfigBean;

    public XlsFileProcessor(AbsDataImportConfigBean configBean)
    {
        super(configBean);
        xlsConfigBean=(XlsDataImportBean)configBean;
        //        {
        layoutObj=new HorizontalDataLayout();
    }

    public Workbook getWorkbook()
    {
        return workbook;
    }

    public void init(File datafile)
    {
        try
        {
            bis=new BufferedInputStream(new FileInputStream(datafile));
            workbook=WorkbookFactory.create(bis);
            String sheet=xlsConfigBean.getSheet();
            if(sheet==null||sheet.trim().equals(""))
            {
                sheetObj=workbook.getSheetAt(0);
            }else if(Tools.isDefineKey("index",sheet))
            {
                sheetObj=workbook.getSheetAt(Integer.parseInt(Tools.getRealKeyByDefine("index",
                        sheet)));
            }else
            {
                sheetObj=workbook.getSheet(sheet);
            }
            if(sheetObj==null)
            {
                throw new WabacusDataImportException("在数据文件"+datafile.getAbsolutePath()
                        +"中没有取到所需的sheet");
            }
            layoutObj.init();
        }catch(FileNotFoundException e)
        {
            throw new WabacusDataImportException("数据导入失败，没有找到数据文件"+datafile.getAbsolutePath(),e);
        }catch(IOException ioe)
        {
            throw new WabacusDataImportException("导入数据文件"+datafile.getAbsolutePath()+"失败",ioe);
        }catch(InvalidFormatException e)
        {
            throw new WabacusDataImportException("数据导入失败，数据文件"+datafile.getAbsolutePath()+"格式不对",e);
        }
    }

    public boolean isEmpty()
    {
        if(this.recordcount<=0) return true;
        List lstRowDataTmp;
        for(int i=startrecordindex;i<startrecordindex+recordcount;i++)
        {
            lstRowDataTmp=this.getRowData(i);
            if(lstRowDataTmp!=null&&lstRowDataTmp.size()>0) return false;
        }
        return true;
    }

    public List<String> getLstColnameData()
    {
        return layoutObj.getLstColnameData();
    }

    public List getRowData(int rowidx)
    {
        if(rowidx<startrecordindex||rowidx>=startrecordindex+recordcount) return null;
        return layoutObj.getRowData(rowidx);
    }

    public void destroy()
    {
        try
        {
            if(bis!=null) bis.close();
        }catch(IOException e)
        {
            log.warn("关闭文件流"+configBean.getFilepath()+"/"+configBean.getFilename()+"失败",e);
        }
    }

    private interface IXlsDataLayout
    {
        public void init();

        public List<String> getLstColnameData();

        public List getRowData(int recordidx);
    }

    private class HorizontalDataLayout implements IXlsDataLayout
    {
        public void init()
        {
//            /**
//             */
//            startrecordindex=xlsConfigBean.getStartdatarowindex()-sheetObj.getFirstRowNum();//得到真正的相对getFirstRowNum()的起始行
//            if(startrecordindex<0) startrecordindex=0;
            startrecordindex=xlsConfigBean.getStartdatarowindex();
            recordcount=sheetObj.getLastRowNum()-startrecordindex+1;
            if(recordcount<0) recordcount=0;
        }

        public List<String> getLstColnameData()
        {
            List<String> lstResults=new ArrayList<String>();
            int colnamerowindex=xlsConfigBean.getColnamerowindex();
            if(colnamerowindex<0) return null;
            Row row=sheetObj.getRow(colnamerowindex);
            if(row==null) return null;
            int cellcount=row.getLastCellNum()-row.getFirstCellNum();
            if(cellcount<=0) return null;
            Cell cellTmp;
            for(int i=0;i<cellcount;i++)
            {
                cellTmp=row.getCell(i);
                if(cellTmp==null)
                {
                    lstResults.add("");
                }else
                {
                    lstResults.add(cellTmp.getRichStringCellValue().getString());
                }
            }
            return lstResults;
        }

        public List getRowData(int recordidx)
        {
            List lstResults=new ArrayList();
            Row row=sheetObj.getRow(recordidx);
            if(row==null) return null;
            int cellcount=row.getLastCellNum()-row.getFirstCellNum();
            if(cellcount<=0) return null;
            Cell cellTmp;
            for(int i=0;i<cellcount;i++)
            {
                cellTmp=row.getCell(i);
                lstResults.add(getCellValue(cellTmp));
            }
            return lstResults;
        }
    }

//    {
//            startrecordindex=xlsConfigBean.getStartdatacolindex();//数据的起始列号
//            HSSFRow row=sheetObj.getRow(xlsConfigBean.getStartdatarowindex());//取到数据第一行
//            int colcnt=row.getLastCellNum()-row.getFirstCellNum();//总列数
//            int endrecordidx=xlsConfigBean.getEnddatacolindex();
//            {
//        public List<String> getLstColnameData()
//            if(endnamerowidx<=0) return null;//没有数据
//            {//如果配置了字段名列的最后一行行号，且小于最大行数
//                endnamerowidx=xlsConfigBean.getEndnamerowindex();
//            for(int i=xlsConfigBean.getStartnamerowindex();i<=endnamerowidx;i++)
//                    lstResults.add("");
//                    {
//                    }
//
//            if(enddatarowidx<=0) return null;//没有数据
//                    &&xlsConfigBean.getEnddatarowindex()<enddatarowidx)
//            {//如果配置了字段名列的最后一行行号，且小于最大行数
//            HSSFCell cellTmp;
//                if(rowTmp==null)
//                    cellTmp=rowTmp.getCell(recordidx);
//            return lstResults;

    private Object getCellValue(Cell cell)
    {
        if(cell==null) return null;
        switch (cell.getCellType())
        {
            case Cell.CELL_TYPE_STRING:
                return cell.getRichStringCellValue().getString();
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_NUMERIC:
                if(DateUtil.isCellDateFormatted(cell))
                {
                    return cell.getDateCellValue();
                }else
                {
                    return String.valueOf(cell.getNumericCellValue());
                    /*double d=cell.getNumericCellValue();
                    if(d-(int)d<Double.MIN_VALUE)
                    { // 是否为int型  
                        return (int)d;
                    }else
                    { 
                        return cell.getNumericCellValue();
                    }*/
                }
            case Cell.CELL_TYPE_BLANK:
                return "";
            default:
                return null;
        }
    }
}
