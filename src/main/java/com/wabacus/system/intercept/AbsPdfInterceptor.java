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
package com.wabacus.system.intercept;

import java.util.Map;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfStamper;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public abstract class AbsPdfInterceptor
{
    /***************************************************************************
     *  下面是针对没有配置模板的报表显示到PDF时的拦截方法                                                  
     **************************************************************************/

    public boolean beforeDisplayReportWithoutTemplate(Document document,AbsReportType reportTypeObj)
    {
        return true;
    }

    public void afterDisplayReportWithoutTemplate(Document document,AbsReportType reportTypeObj)
    {}

    public void beforeDisplayPdfPageWithoutTemplate(Document document,AbsReportType reportTypeObj)
    {}

    public void afterDisplayPdfPageWithoutTemplate(Document document,AbsReportType reportTypeObj)
    {}

    public void displayPerColDataWithoutTemplate(AbsReportType reportTypeObj,Object configbean,int rowidx,String value,PdfPCell cell)
    {}

    /***************************************************************************
     *  下面是针对有模板的拦截方法                                                  
     **************************************************************************/

    public boolean beforeDisplayPdfPageWithTemplate(IComponentConfigBean ccbean,Map<String,AbsReportType> mReportTypeObjs,int rowindex,
            PdfStamper pdfstamp)
    {
        return true;
    }

    public String beforeDisplayFieldWithTemplate(IComponentConfigBean ccbean,Map<String,AbsReportType> mReportTypeObjs,int rowindex,
            PdfStamper pdfstamp,String fieldname,String fieldvalue)
    {
        return fieldvalue;
    }

    public void afterDisplayPdfPageWithTemplate(IComponentConfigBean ccbean,Map<String,AbsReportType> mReportTypeObjs,int rowindex,PdfStamper pdfstamp)
    {}
}
