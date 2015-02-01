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
package com.wabacus.system.component.application.report.abstractreport.configbean;

import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;

public class AbsChartReportBean extends AbsExtendConfigBean
{
    public final static String DATATYPE_XML="xml";

    public final static String DATATYPE_JSON="json";

    public final static String DATATYPE_XMLURL="xmlurl";

    public final static String DATATYPE_XMLURL_SERVLET="xmlurl-servlet";

    public final static String DATATYPE_JSONURL="jsonurl";

    public final static String DATATYPE_JSONURL_SERVLET="jsonurl-servlet";

    private String chartype;

    private String datatype;

    private String chartstyleproperty;//显示图表<chart/>的样式

    private List<String> lstDynChartstylepropertyParts;

    public AbsChartReportBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getChartype()
    {
        return chartype;
    }

    public void setChartype(String chartype)
    {
        if(chartype!=null)
        {
            while(chartype.startsWith("/"))
                chartype=chartype.substring(1);
        }
        this.chartype=chartype;
    }

    public String getDatatype()
    {
        if(this.datatype==null||this.datatype.trim().equals(""))
        {
            String _datatype=Config.getInstance().getSystemConfigValue("default-chart-datatype",DATATYPE_XMLURL).trim().toLowerCase();
            if(!DATATYPE_XML.equals(_datatype)&&!DATATYPE_JSON.equals(_datatype)&&!DATATYPE_XMLURL.equals(_datatype)
                    &&!DATATYPE_JSONURL.equals(_datatype)&&!DATATYPE_XMLURL_SERVLET.equals(_datatype)&&!DATATYPE_JSONURL_SERVLET.equals(_datatype))
            {
                throw new WabacusConfigLoadingException("在wabacus.cfg.xml的default-chart-datatype中配置的"+this.datatype+"无效");
            }
            this.datatype=_datatype;
        }
        return datatype;
    }

    public void setDatatype(String datatype)
    {
        this.datatype=datatype==null?"":datatype.toLowerCase().trim();
        if(!DATATYPE_XML.equals(this.datatype)&&!DATATYPE_JSON.equals(this.datatype)&&!DATATYPE_XMLURL.equals(this.datatype)
                &&!DATATYPE_JSONURL.equals(this.datatype)&&!DATATYPE_XMLURL_SERVLET.equals(this.datatype)
                &&!DATATYPE_JSONURL_SERVLET.equals(this.datatype))
        {
            throw new WabacusConfigLoadingException("报表"+this.getOwner().getReportBean().getPath()+"配置的datatype属性值："+this.datatype+"无效");
        }
    }

    public String getChartstyleproperty(ReportRequest rrequest,boolean isStaticPart)
    {
        if(isStaticPart) return this.chartstyleproperty;
        return WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.chartstyleproperty,this.lstDynChartstylepropertyParts,"");
    }

    public void setChartstyleproperty(String chartstyleproperty,boolean isStaticPart)
    {
        if(isStaticPart)
        {
            this.chartstyleproperty=chartstyleproperty;
        }else
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(chartstyleproperty);
            this.chartstyleproperty=(String)objArr[0];
            this.lstDynChartstylepropertyParts=(List<String>)objArr[1];
        }
    }
}
