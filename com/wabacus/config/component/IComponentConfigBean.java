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
package com.wabacus.config.component;

import java.util.List;

import com.wabacus.config.OnloadMethodBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.page.PageBean;
import com.wabacus.config.dataexport.DataExportsConfigBean;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.config.other.ButtonsBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.template.TemplateBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.component.IComponentType;
import com.wabacus.system.component.container.AbsContainerType;

public interface IComponentConfigBean
{
    public String getId();
    
    public String getGuid();//获取当前元素的唯一id，即包括其所在的<page/>的id

    public void setId(String id);
    
    public String getRefreshid();
    
    public void setRefreshid(String refreshid);
    
    public String getRefreshGuid();
    
    public String getTop();

    public void setTop(String top);

    public String getBottom();
    
    public void setBottom(String bottom);
    
    public String getLeft();

    public void setLeft(String left);

    public String getRight();
    
    public void setRight(String right);
    
    public String getAlign();

    public void setAlign(String align);

    public String getValign();

    public void setValign(String valign);
    
    public String getWidth();

    public void setWidth(String width);
    
    public String getHeight();

    public void setHeight(String height);
    
    public void setScrollstyle(String scrollstyle);
    
    public String getScrollstyle();
    
    public String getTitle(ReportRequest rrequest);
    
    public void setTitle(String title);

    public String getSubtitle(ReportRequest rrequest);
    
    public void setSubtitle(String subtitle);
    
    public String getParenttitle(ReportRequest rrequest);
    
    public IComponentConfigBean getConfigBeanWithValidParentTitle();
    
    public void setParenttitle(String parenttitle);
    
    public void setParentSubtitle(String parentSubtitle);
    
    public String getParentSubtitle(ReportRequest rrequest);
    
    public String getPath();
    
    public void setTitlealign(String titlealign);
    
    public String getTitlealign();
    
    public boolean shouldShowContextMenu();
    
    public void setShowContextMenu(boolean showContextMenu);
    
    public TemplateBean getOuterHeaderTplBean();

    public void setOuterHeaderTplBean(TemplateBean outerHeaderTplBean);
    
    public TemplateBean getHeaderTplBean();

    public void setHeaderTplBean(TemplateBean headerTplBean);

    public TemplateBean getFooterTplBean();

    public void setFooterTplBean(TemplateBean footerTplBean);
    
    public TemplateBean getOuterFooterTplBean();

    public void setOuterFooterTplBean(TemplateBean outerFooterTplBean);
    
    public AbsContainerConfigBean getParentContainer();
    
    public List<OnloadMethodBean> getLstOnloadMethods();
    
    public void addOnloadMethod(OnloadMethodBean onLoadMethodBean);
    
    public void removeOnloadMethodByType(String type);
    
    public String getOnloadMethodName();

    public ButtonsBean getButtonsBean();

    public void setButtonsBean(ButtonsBean buttonsBean);
    
    public PageBean getPageBean();

    public void setPrintBean(AbsPrintProviderConfigBean printBean);
    
    public AbsPrintProviderConfigBean getPrintBean();
    
    public PDFExportBean getPdfPrintBean();

    public void setPdfPrintBean(PDFExportBean pdfPrintBean);
    
    public IComponentConfigBean clone(AbsContainerConfigBean parentContainer);

    public DataExportsConfigBean getDataExportsBean();
    
    public void setDataExportsBean(DataExportsConfigBean decbean);
    
    public IComponentType createComponentTypeObj(ReportRequest rrequest,AbsContainerType parentContainer);
    
    public void doPostLoad();
    
    public void doPostLoadFinally();
}
