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
package com.wabacus.config.component.application.jsphtml;

import com.wabacus.config.component.AbsComponentConfigBean;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.IApplicationConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.panel.TabsPanelBean;
import com.wabacus.config.dataexport.DataExportsConfigBean;
import com.wabacus.config.dataexport.PDFExportBean;
import com.wabacus.config.print.AbsPrintProviderConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;

public abstract class AbsJspHtmlComponentBean extends AbsComponentConfigBean implements IApplicationConfigBean
{
    private String printwidth;
    
    protected String belongto;

    protected IComponentConfigBean belongToCcbean;
    
    public AbsJspHtmlComponentBean(AbsContainerConfigBean parentContainer)
    {
        super(parentContainer);
    }

    public IComponentConfigBean getBelongToCcbean()
    {
        return belongToCcbean;
    }

    public void setBelongToCcbean(IComponentConfigBean belongToCcbean)
    {
        this.belongToCcbean=belongToCcbean;
    }
    
    public IComponentConfigBean getConfigBeanWithValidParentTitle()
    {
        if(this.parenttitle!=null&&!this.parenttitle.trim().equals("")) return this;
        return null;
    }
    
    public void loadExtendConfig(XmlElementBean eleJspHtml,AbsContainerConfigBean parentConfigBean)
    {
        this.belongto=eleJspHtml.attributeValue("belongto");
    }
    //<html/>、<jsp/>不能配置打印、数据导出等功能，所以下面全部置成null
    public void setDataExportsBean(DataExportsConfigBean dataExportsBean)
    {
        super.setDataExportsBean(null);
    }

    public void setPdfPrintBean(PDFExportBean pdfPrintBean)
    {
        super.setPdfPrintBean(null);
    }

    public void setPrintBean(AbsPrintProviderConfigBean printBean)
    {
        super.setPrintBean(null);
    }

    public String getPrintwidth()
    {
        return printwidth;
    }

    public void setPrintwidth(String printwidth)
    {
        this.printwidth=printwidth;
    }

    public void doPostLoad()
    {
        if(belongto!=null&&!belongto.trim().equals(""))
        {
            belongToCcbean=this.getPageBean().getChildComponentBean(belongto,true);
            if(belongToCcbean==null)
            {
                throw new WabacusConfigLoadingException("加载组件"+this.getPath()+"失败，其belongto属性配置的附属组件"+this.belongto+"不存在");
            }
            if(!(belongToCcbean instanceof ReportBean)&&!(belongToCcbean instanceof AbsContainerConfigBean))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.getPath()+"失败，其belongto属性配置的附属组件"+this.belongto+"不是容器或报表组件");
            }
            String ccbeanRefreshid=belongToCcbean.getRefreshid();
            if(ccbeanRefreshid==null||ccbeanRefreshid.trim().equals("")) ccbeanRefreshid=this.belongToCcbean.getId();
            ccbeanRefreshid=this.getPageBean().getCommonRefreshIdOfComponents(ccbeanRefreshid,this.id);
            belongToCcbean.setRefreshid(ccbeanRefreshid);
            AbsContainerConfigBean parentContainerBean=belongToCcbean.getParentContainer();
            TabsPanelBean tpbeanTmp;
            while(parentContainerBean!=null)
            {
                if(parentContainerBean instanceof TabsPanelBean)
                {
                    tpbeanTmp=(TabsPanelBean)parentContainerBean;
                    if(tpbeanTmp.getChildComponentBean(this.id,true)==null||!tpbeanTmp.isInSameTabItem(belongToCcbean,this))
                    {
                        tpbeanTmp.setAsyn(false);//强制设置为在客户端切换
                    }
                }
                if(parentContainerBean.getChildComponentBean(this.id,true)!=null) break;//当前容器已经是它们的父容器了，则不用再向上找父<tabpanel/>了
                parentContainerBean=parentContainerBean.getParentContainer();
            }
        }
    }
}

