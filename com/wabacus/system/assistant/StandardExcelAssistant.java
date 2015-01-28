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
package com.wabacus.system.assistant;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;

import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.BigdecimalType;
import com.wabacus.system.datatype.CDateType;
import com.wabacus.system.datatype.CTimeType;
import com.wabacus.system.datatype.CTimestampType;
import com.wabacus.system.datatype.DateType;
import com.wabacus.system.datatype.DoubleType;
import com.wabacus.system.datatype.FloatType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.IntType;
import com.wabacus.system.datatype.LongType;
import com.wabacus.system.datatype.ShortType;
import com.wabacus.system.datatype.TimeType;
import com.wabacus.system.datatype.TimestampType;
import com.wabacus.system.datatype.VarcharType;

public class StandardExcelAssistant
{
    private final static StandardExcelAssistant instance=new StandardExcelAssistant();

    protected StandardExcelAssistant()
    {}

    public static StandardExcelAssistant getInstance()
    {
        return instance;
    }
    
    public CellStyle getTitleCellStyleForStandardExcel(Workbook workbook)
    {
        CellStyle cs=workbook.createCellStyle();
        cs.setBorderTop(CellStyle.BORDER_THIN);
        cs.setBorderLeft(CellStyle.BORDER_THIN);
        cs.setBorderBottom(CellStyle.BORDER_THIN);
        cs.setBorderRight(CellStyle.BORDER_THIN);
        cs.setWrapText(true);
        cs.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        cs.setAlignment(CellStyle.ALIGN_CENTER);
        cs.setFillPattern(CellStyle.SOLID_FOREGROUND);
        cs.setFillForegroundColor(HSSFColor.LIGHT_CORNFLOWER_BLUE.index);
        Font font=workbook.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
        font.setFontHeightInPoints((short)10);
        cs.setFont(font);
        //cs.setUserStyleName("wabacus_title_rowstyle");//暂时没用上此属性
        return cs;
    }
    
    public CellStyle getDataCellStyleForStandardExcel(Workbook workbook)
    {
        CellStyle cs=workbook.createCellStyle();
        cs.setBorderTop(CellStyle.BORDER_THIN);
        cs.setBorderLeft(CellStyle.BORDER_THIN);
        cs.setBorderBottom(CellStyle.BORDER_THIN);
        cs.setBorderRight(CellStyle.BORDER_THIN);
        cs.setWrapText(true);
        cs.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        Font font=workbook.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
        font.setFontHeightInPoints((short)10);
        cs.setFont(font);
        return cs;
    }
    
    public CellStyle setCellAlign(CellStyle cs,String align)
    {
        align=align==null?"":align.trim().toLowerCase();
        if("left".equals(align))
        {
            cs.setAlignment(CellStyle.ALIGN_LEFT);
        }else if("right".equals(align))
        {
            cs.setAlignment(CellStyle.ALIGN_RIGHT);
        }else
        {
            cs.setAlignment(CellStyle.ALIGN_CENTER);
        }
        return cs;
    }
    
    public void setRegionCellStringValue(Workbook workbook,Sheet sheet,CellRangeAddress region,
            CellStyle cellStyle,String cellvalue)
    {
        createRowAndColInRegion(sheet,region,cellStyle);
        Row rowTmp=sheet.getRow(region.getFirstRow());
        Cell cellTmp=rowTmp.getCell(region.getFirstColumn());
        cellTmp.setCellStyle(cellStyle);
        cellTmp.setCellValue(cellvalue);
        sheet.addMergedRegion(region);
    }

    public void setRegionCellRealTypeValue(Workbook workbook,Sheet sheet,CellRangeAddress region,
            CellStyle cellStyle,CellStyle cellStyleWithFormat,String align,Object cellvalue,IDataType typeObj)
    {
        createRowAndColInRegion(sheet,region,cellStyle);
        Row rowTmp=sheet.getRow(region.getFirstRow());//这里得到的row是在setRegionStyle()方法中创建的
        Cell cellTmp=rowTmp.getCell(region.getFirstColumn());
        cellTmp.setCellStyle(cellStyle);
        this.setCellValue(workbook,align,cellTmp,cellvalue,typeObj,cellStyleWithFormat);
        sheet.addMergedRegion(region);
    }
    
    private void createRowAndColInRegion(Sheet sheet,CellRangeAddress region,
            CellStyle cellStyle)
    {
        Row rowTmp;
        Cell cellTmp;
        for(int i=region.getFirstRow();i<=region.getLastRow();i++)
        {
            rowTmp=CellUtil.getRow(i,sheet);
            for(int j=region.getFirstColumn();j<=region.getLastColumn();j++)
            {
                cellTmp=CellUtil.getCell(rowTmp,j);
                cellTmp.setCellStyle(cellStyle);
            }
        }
    }
    
    public boolean setCellValue(Workbook workbook,String align,Cell cell,Object objValue,IDataType typeObj,CellStyle cellStyleWithFormat)
    {
        if(objValue==null)
        {
            cell.setCellValue("");
            return false;
        }
        if(typeObj instanceof VarcharType)
        {
            cell.setCellValue((String)objValue);
        }else if(typeObj instanceof DoubleType)
        {
            cell.setCellValue((Double)objValue);
        }else if(typeObj instanceof FloatType)
        {
            cell.setCellValue((Float)objValue);
        }else if(typeObj instanceof IntType)
        {
            cell.setCellValue((Integer)objValue);
        }else if(typeObj instanceof LongType)
        {
            cell.setCellValue(((Long)objValue));
        }else if(typeObj instanceof ShortType)
        {
            cell.setCellValue(((Short)objValue));
        }else if(typeObj instanceof BigdecimalType)
        {
            cell.setCellValue((((BigDecimal)objValue)).doubleValue());
        }else if(typeObj instanceof DateType||typeObj instanceof TimeType||typeObj instanceof TimestampType)
        {
            cellStyleWithFormat.setDataFormat(workbook.createDataFormat().getFormat(((AbsDateTimeType)typeObj).getDateformat()));
            if(align!=null&&!align.trim().equals(""))
            {
                cellStyleWithFormat=this.setCellAlign(cellStyleWithFormat,align);
            }
            cell.setCellValue(((Date)objValue));
            cell.setCellStyle(cellStyleWithFormat);
            return true;
        }else if(typeObj instanceof CDateType||typeObj instanceof CTimeType||typeObj instanceof CTimestampType)
        {
            cellStyleWithFormat.setDataFormat(workbook.createDataFormat().getFormat(((AbsDateTimeType)typeObj).getDateformat()));
            if(align!=null&&!align.trim().equals(""))
            {
                cellStyleWithFormat=this.setCellAlign(cellStyleWithFormat,align);
            }
            cell.setCellValue(((Calendar)objValue));
            cell.setCellStyle(cellStyleWithFormat);
            return true;
        }else
        {//其它的类型都做为字符串形式写到Excel文件中
            cell.setCellValue(typeObj.value2label(objValue));
        }
        return false;
    }
}
