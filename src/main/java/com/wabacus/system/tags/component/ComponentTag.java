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
package com.wabacus.system.tags.component;

import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspException;

import com.wabacus.config.component.application.IApplicationConfigBean;
import com.wabacus.config.dataexport.WordRichExcelExportBean;
import com.wabacus.config.print.PrintSubPageBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.component.application.AbsApplicationType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.util.Consts;

public class ComponentTag extends AbsComponentTag
{
    private static final long serialVersionUID=-4710869096949802105L;

    public int doMyStartTag() throws JspException,IOException
    {
        if(this.displayComponentObj==null) return SKIP_BODY;
        if(!(this.displayComponentObj instanceof AbsApplicationType))
        {
            throw new WabacusRuntimeException("组件"+this.displayComponentObj.getConfigBean().getPath()+"不是应用，不能调用<wx:component/>显示其内容");
        }
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {
            ((AbsApplicationType)this.displayComponentObj).displayOnPage(this);
        }else if(rrequest.getShowtype()==Consts.DISPLAY_ON_RICHEXCEL||rrequest.getShowtype()==Consts.DISPLAY_ON_WORD)
        {
            Object tplObj=null;
            boolean isDisplayFirstime=false;
            if(!this.isDisplayByMySelf())
            {//如果是在其它报表的动态模板中使用<wx:component/>显示此报表
                isDisplayFirstime=true;
                if(this.displayComponentObj.getConfigBean().getDataExportsBean()!=null)
                {
                    WordRichExcelExportBean debean=(WordRichExcelExportBean)this.displayComponentObj.getConfigBean().getDataExportsBean()
                            .getDataExportBean(rrequest.getShowtype());
                    if(debean!=null) tplObj=debean.getDataExportTplObj();
                }
            }
            ((AbsApplicationType)this.displayComponentObj).displayOnExportDataFile(tplObj,isDisplayFirstime);
        }else if(rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT)
        {//打印
            List<PrintSubPageBean> lstPrintPagebean=null;
            IApplicationConfigBean appConfigBean=(IApplicationConfigBean)this.displayComponentObj.getConfigBean();
            if(displayComponentObj instanceof AbsReportType&&appConfigBean.getPrintBean()!=null
                    &&!appConfigBean.getPrintBean().isUseGlobalDefaultPrintTemplate()&&appConfigBean.getPrintBean().isTemplatePrintValue())
            {//如果当前应用是报表，且配置了自己的<print/>，且里面的内容是模板，而不是其它代码，则用其配置的<print/>打印内容进行打印
                lstPrintPagebean=appConfigBean.getPrintBean().getLstPrintPageBeans();
            }
            ((AbsApplicationType)this.displayComponentObj).printApplication(lstPrintPagebean);
        }
        rrequest.getWResponse().setJspout(null);//清空，表示后面的显示不再是jsp的自定义标签<wx:component/>的显示
        return SKIP_BODY;
    }

    public int doMyEndTag() throws JspException,IOException
    {
        return EVAL_PAGE;
    }
}
