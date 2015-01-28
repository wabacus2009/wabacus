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

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.util.Tools;

public class AbsListReportSubDisplayRowBean implements Cloneable
{
    private int displaytype=AbsListReportSubDisplayBean.SUBROW_DISPLAYTYPE_REPORT;
    
    private int displayposition=AbsListReportSubDisplayBean.SUBROW_POSITION_BOTTOM;
    
    protected List<AbsListReportSubDisplayColBean> lstSubColBeans;

    public int getDisplaytype()
    {
        return displaytype;
    }

    public void setDisplaytype(ReportBean rbean,String displaytype)
    {
        boolean isDisplayPerpage=false, isDisplayReport=false;
        displaytype=displaytype.toLowerCase().trim();
        if(!displaytype.equals(""))
        {
            List<String> lstTmp=Tools.parseStringToList(displaytype,"|",false);
            for(String tmp:lstTmp)
            {
                tmp=tmp.trim();
                if(tmp.equals("")) continue;
                if(tmp.equals("page"))
                {
                    isDisplayPerpage=true;
                }else if(tmp.equals("report"))
                {
                    isDisplayReport=true;
                }else
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"的统计显示行<subrow/>失败，其displaytype属性配置不合法");
                }
            }
        }
        if(isDisplayPerpage&&isDisplayReport)
        {
            this.displaytype=AbsListReportSubDisplayBean.SUBROW_DISPLAYTYPE_PAGEREPORT;
        }else if(isDisplayPerpage)
        {
            this.displaytype=AbsListReportSubDisplayBean.SUBROW_DISPLAYTYPE_PAGE;
        }else
        {
            this.displaytype=AbsListReportSubDisplayBean.SUBROW_DISPLAYTYPE_REPORT;
        }
    }

    public int getDisplayposition()
    {
        return displayposition;
    }

    public void setDisplayposition(ReportBean rbean,String displayposition)
    {
        boolean isTop=false, isBottom=false;
        displayposition=displayposition.toLowerCase().trim();
        if(!displayposition.equals(""))
        {
            List<String> lstTmp=Tools.parseStringToList(displayposition,"|",false);
            for(String tmp:lstTmp)
            {
                tmp=tmp.trim();
                if(tmp.equals("")) continue;
                if(tmp.equals("top"))
                {
                    isTop=true;
                }else if(tmp.equals("bottom"))
                {
                    isBottom=true;
                }else
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"的统计显示行<subrow/>失败，其displayposition属性配置不合法");
                }
            }
        }
        if(isTop&&isBottom)
        {
            this.displayposition=AbsListReportSubDisplayBean.SUBROW_POSITION_BOTH;
        }else if(isTop)
        {
            this.displayposition=AbsListReportSubDisplayBean.SUBROW_POSITION_TOP;
        }else
        {
            this.displayposition=AbsListReportSubDisplayBean.SUBROW_POSITION_BOTTOM;
        }
    }

    public List<AbsListReportSubDisplayColBean> getLstSubColBeans()
    {
        return lstSubColBeans;
    }

    public void setLstSubColBeans(List<AbsListReportSubDisplayColBean> lstSubColBeans)
    {
        this.lstSubColBeans=lstSubColBeans;
    }
    
    public AbsListReportSubDisplayRowBean clone()
    {
        AbsListReportSubDisplayRowBean newBean=null;
        try
        {
            newBean=(AbsListReportSubDisplayRowBean)super.clone();
            if(lstSubColBeans!=null)
            {
                List<AbsListReportSubDisplayColBean> lstStatiColBeansNew=new ArrayList<AbsListReportSubDisplayColBean>();
                for(AbsListReportSubDisplayColBean cb:lstSubColBeans)
                {
                    if(cb!=null)
                    {
                        lstStatiColBeansNew.add((AbsListReportSubDisplayColBean)cb.clone());
                    }
                }
                newBean.setLstSubColBeans(lstStatiColBeansNew);
            }
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
        }
        return newBean;
    }
}

