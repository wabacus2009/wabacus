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

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.commoninterface.IListReportRoworderPersistence;
import com.wabacus.util.Consts;

public class AbsListReportBean extends AbsExtendConfigBean
{
    public final static int SCROLLTYPE_NONE=0;
    
    public final static int SCROLLTYPE_FIXED=1;
    
    public final static int SCROLLTYPE_VERTICAL=2;
    
    public final static int SCROLLTYPE_HORIZONTAL=3;
    
    public final static int SCROLLTYPE_ALL=4;//不固定行列的纵横滚动条
    
    private String rowSelectType;
    
    private boolean isSelectRowCrossPages;

    private List<String> lstRowSelectCallBackFuncs;

    private List<String> lstRoworderTypes;
    
    private IListReportRoworderPersistence loadStoreRoworderObject;//读写行顺序的对象
    
    private int fixedrows;
    
    private int fixedcols;
    
    private Integer batchouputcount;
    
    private AbsListReportSubDisplayBean subdisplaybean;
    
    public AbsListReportBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public String getRowSelectType()
    {
        return rowSelectType;
    }

    public void setRowSelectType(String rowSelectType)
    {
        this.rowSelectType=rowSelectType;
    }

    public boolean isSelectRowCrossPages()
    {
        return isSelectRowCrossPages;
    }

    public void setSelectRowCrossPages(boolean isSelectRowCrossPages)
    {
        this.isSelectRowCrossPages=isSelectRowCrossPages;
    }

    public List<String> getLstRowSelectCallBackFuncs()
    {
        return lstRowSelectCallBackFuncs;
    }

    public void setLstRowSelectCallBackFuncs(List<String> lstRowSelectCallBackFuncs)
    {
        this.lstRowSelectCallBackFuncs=lstRowSelectCallBackFuncs;
    }

    public List<String> getLstRoworderTypes()
    {
        return lstRoworderTypes;
    }

    public void setLstRoworderTypes(List<String> lstRoworderTypes)
    {
        this.lstRoworderTypes=lstRoworderTypes;
    }

    public IListReportRoworderPersistence getLoadStoreRoworderObject()
    {
        return loadStoreRoworderObject;
    }

    public void setLoadStoreRoworderObject(IListReportRoworderPersistence loadStoreRoworderObject)
    {
        this.loadStoreRoworderObject=loadStoreRoworderObject;
    }

    public void addRowSelectCallBackFunc(String rowselectMethod)
    {
        if(rowselectMethod==null||rowselectMethod.trim().equals("")||rowselectMethod.trim().equals("''")) return;
        rowselectMethod=rowselectMethod.trim();
        if(lstRowSelectCallBackFuncs==null)
        {
            lstRowSelectCallBackFuncs=new ArrayList<String>();
        }else if(lstRowSelectCallBackFuncs.contains(rowselectMethod))
        {
            return;
        }
        lstRowSelectCallBackFuncs.add(rowselectMethod);
    }

    public int getBatchouputcount()
    {
        if(batchouputcount==null)
        {
            batchouputcount=Config.getInstance().getSystemConfigValue("default-batchoutput-recordcount",-1);
        }
        return batchouputcount.intValue();
    }

    public void setBatchouputcount(Integer batchouputcount)
    {
        this.batchouputcount=batchouputcount;
    }

    public int getFixedrows()
    {
        return fixedrows;
    }

    public void setFixedrows(int fixedrows)
    {
        this.fixedrows=fixedrows;
    }

    public int getFixedcols(ReportRequest rrequest)
    {
        if(rrequest==null) return fixedcols;
        Object objVal=rrequest.getAttribute(this.getOwner().getReportBean().getId(),"DYNAMIC_FIXED_COLSCOUNT");
        if(objVal==null) return fixedcols;//没有设置运行时动态值，则用配置值
        return ((Integer)objVal).intValue();
    }

    public void setFixedcols(ReportRequest rrequest,int fixedcols)
    {
        if(rrequest==null)
        {
            this.fixedcols=fixedcols;
        }else
        {
            rrequest.setAttribute(this.getOwner().getReportBean().getId(),"DYNAMIC_FIXED_COLSCOUNT",fixedcols);
        }
    }

    public AbsListReportSubDisplayBean getSubdisplaybean()
    {
        return subdisplaybean;
    }

    public void setSubdisplaybean(AbsListReportSubDisplayBean subdisplaybean)
    {
        this.subdisplaybean=subdisplaybean;
    }

    public int getScrollType()
    {
        if(this.fixedcols>0||this.fixedrows>0) return SCROLLTYPE_FIXED;
        ReportBean rbean=(ReportBean)this.getOwner();
        if(rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("")&&rbean.getScrollwidth()!=null
                &&!rbean.getScrollwidth().trim().equals("")) return SCROLLTYPE_ALL;
        if(rbean.getScrollheight()!=null&&!rbean.getScrollheight().trim().equals("")) return SCROLLTYPE_VERTICAL;
        if(rbean.getScrollwidth()!=null&&!rbean.getScrollwidth().trim().equals("")) return SCROLLTYPE_HORIZONTAL;
        return SCROLLTYPE_NONE;
    }
    
    public boolean hasControllCol()
    {
        if(Consts.ROWSELECT_CHECKBOX.equalsIgnoreCase(this.rowSelectType)||Consts.ROWSELECT_RADIOBOX.equalsIgnoreCase(this.rowSelectType)
                ||Consts.ROWSELECT_MULTIPLE_CHECKBOX.equalsIgnoreCase(this.rowSelectType)||Consts.ROWSELECT_SINGLE_RADIOBOX.equalsIgnoreCase(this.rowSelectType))
            return true;
        if(this.lstRoworderTypes!=null&&(this.lstRoworderTypes.contains(Consts.ROWORDER_ARROW)
                ||this.lstRoworderTypes.contains(Consts.ROWORDER_INPUTBOX)||this.lstRoworderTypes.contains(Consts.ROWORDER_TOP))) return true;
        return false;
    }
    
    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        AbsListReportBean mynewrbean=(AbsListReportBean)super.clone(owner);
        if(lstRowSelectCallBackFuncs!=null)
        {
            mynewrbean
                    .setLstRowSelectCallBackFuncs((List<String>)((ArrayList<String>)lstRowSelectCallBackFuncs)
                            .clone());
        }
        if(this.subdisplaybean!=null)
        {
            mynewrbean.setSubdisplaybean((AbsListReportSubDisplayBean)subdisplaybean.clone(owner));
        }
        return mynewrbean;
    }
}
