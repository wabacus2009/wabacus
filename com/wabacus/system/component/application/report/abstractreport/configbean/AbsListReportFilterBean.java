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

import java.lang.reflect.Method;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;

public class AbsListReportFilterBean extends AbsExtendConfigBean
{
    private String filterColumnExpression;//如果不能通过直接用<col/>的column in(...)进行过滤，而需对column进行转换后再过滤，则配置对column进行转换的表达式，比如to_char(字段名,'yyyy-MM-dd')
    
    private int filterwidth;//列过滤下拉提示宽度，不配置或配置为空字符串时，与相应<td/>相同的宽度
    
    private int filtermaxheight=350;
    
    private String conditionname;//如果不为空，则存放关联的查询条件<condition/>的name属性

    private String filterformat;
    
    private Method formatMethod;
    
    private Class formatClass;

    public AbsListReportFilterBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getId()
    {
        ColBean colbean=(ColBean)this.getOwner();
        return colbean.getReportBean().getId()+colbean.getProperty();
    }

    public String getFilterColumnExpression()
    {
        return filterColumnExpression;
    }

    public void setFilterColumnExpression(String filterColumnExpression)
    {
        this.filterColumnExpression=filterColumnExpression;
    }

    public int getFilterwidth()
    {
        return filterwidth;
    }

    public void setFilterwidth(int filterwidth)
    {
        this.filterwidth=filterwidth;
    }

    public int getFiltermaxheight()
    {
        return filtermaxheight;
    }

    public void setFiltermaxheight(int filtermaxheight)
    {
        this.filtermaxheight=filtermaxheight;
    }

    public String getConditionname()
    {
        return conditionname;
    }

    public void setConditionname(String conditionname)
    {
        this.conditionname=conditionname;
    }

    public String getFilterformat()
    {
        return filterformat;
    }

    public void setFilterformat(String filterformat)
    {
        this.filterformat=filterformat;
    }

    public Method getFormatMethod()
    {
        return formatMethod;
    }

    public void setFormatMethod(Method formatMethod)
    {
        this.formatMethod=formatMethod;
    }

    public Class getFormatClass()
    {
        return formatClass;
    }

    public void setFormatClass(Class formatClass)
    {
        this.formatClass=formatClass;
    }

    public boolean isConditionRelate()
    {
        if(this.conditionname!=null&&!this.conditionname.trim().equals(""))
        {
            return true;
        }
        return false;
    }
    
    public void doPostLoad()
    {
        ColBean cbean=(ColBean)this.getOwner();
        SqlBean sqlbean=cbean.getReportBean().getSbean();
        if(cbean.getLstDatasetValueids()!=null)
        {
            for(String belongDsidTmp:cbean.getLstDatasetValueids())
            {
                if(sqlbean.isExistDependentDataset(belongDsidTmp))
                {//当前列过滤所在的列有从子数据集中取的数据
                    throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()+"的列"+cbean.getLabel(null)
                            +"失败，当前列是从子数据集中取数据，不能为它配置列过滤功能");
                }
            }
        }
        if(isConditionRelate())
        {
            ConditionBean cbTmp=sqlbean.getConditionBeanByName(conditionname);
            if(cbTmp==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，列"+((ColBean)this.getOwner()).getProperty()
                        +"过滤条件对应的查询条件不存在");
            }
            if(cbTmp.isConstant())
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，列"+((ColBean)this.getOwner()).getProperty()
                        +"过滤条件对应的查询条件是常量查询条件");
            }
            if(cbTmp.getIterator()>1)
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，列"+((ColBean)this.getOwner()).getProperty()
                        +"过滤条件对应的查询条件的iterator属性配置值大于1，即要显示多套输入框，不允许被过滤列关联");
            }
            if(!cbTmp.isConditionValueFromUrl())
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，列"+((ColBean)this.getOwner()).getProperty()
                        +"过滤条件对应的查询条件不是从url中获取数据");
            }
        }
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)this.getOwner().getReportBean().getDbean().getExtendConfigDataForReportType(
                AbsListReportType.KEY);
        alrdbean.addFilterBean(this);
    }

    public int hashCode()
    {
        return this.getId().hashCode()*17+37;
    }

    public boolean equals(Object o)
    {
        if(o==null) return false;
        if(!(o instanceof AbsListReportFilterBean))
        {
            return false;
        }
        if(this.getId().equals(((AbsListReportFilterBean)o).getId())) return true;
        return false;
    }
}
