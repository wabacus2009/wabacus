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
package com.wabacus.config.print;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrintSubPageBean
{
    private String placeholder;

    private boolean isMergeUp=true;

    private int minpagecount;
    
    private int maxpagecount;//此页打印的最大页数，比如对于分页显示的列表报表，想每条记录打印一页，但只打印本页面的所有记录，则可以在include属性中指定此报表的pagesize为1，然后指定<page/>的maxpagecount为页面上本页记录数，只对分页打印的subpage有效
    
    private List<String> lstIncludeSplitPrintReportIds;
    
    private String tagContent;//<subpage/>标签的内容，加载时用，加载完后清空
    
    private Map<String,PrintTemplateElementBean> mPrintElements;
    
    private AbsPrintProviderConfigBean parent;
    
    public PrintSubPageBean(AbsPrintProviderConfigBean parent)
    {
        this.parent=parent;
        this.placeholder="WX_PRINT_PAGE_PLACEHOLDER_"+parent.getPlaceholderIndex();
    }
    
    public String getPlaceholder()
    {
        return placeholder;
    }

    public void setPlaceholder(String placeholder)
    {
        this.placeholder=placeholder;
    }

    public boolean isMergeUp()
    {
        return isMergeUp;
    }

    public void setMergeUp(boolean isMergeUp)
    {
        this.isMergeUp=isMergeUp;
    }

    public int getMinpagecount()
    {
        return minpagecount;
    }

    public void setMinpagecount(int minpagecount)
    {
        this.minpagecount=minpagecount;
    }

    public int getMaxpagecount()
    {
        return maxpagecount;
    }

    public void setMaxpagecount(int maxpagecount)
    {
        this.maxpagecount=maxpagecount;
    }

    public String getTagContent()
    {
        return tagContent;
    }

    public void setTagContent(String tagContent)
    {
        this.tagContent=tagContent;
    }

    public List<String> getLstIncludeSplitPrintReportIds()
    {
        return lstIncludeSplitPrintReportIds;
    }

    public void setLstIncludeSplitPrintReportIds(List<String> lstIncludeSplitPrintReportIds)
    {
        this.lstIncludeSplitPrintReportIds=lstIncludeSplitPrintReportIds;
    }

    public Map<String,PrintTemplateElementBean> getMPrintElements()
    {
        return mPrintElements;
    }

    public void addPrintElement(PrintTemplateElementBean printelebean)
    {
        if(this.mPrintElements==null) this.mPrintElements=new HashMap<String,PrintTemplateElementBean>();
        this.mPrintElements.put(printelebean.getPlaceholder(),printelebean);
    }

    public AbsPrintProviderConfigBean getParent()
    {
        return parent;
    }

    public boolean isSplitPrintPage()
    {
        if(lstIncludeSplitPrintReportIds==null||lstIncludeSplitPrintReportIds.size()==0) return false;
        return true;
    }
    
    public void addIncludeSplitPrintReportids(List<String> lstReportids)
    {
        if(lstReportids==null||lstReportids.size()==0) return;
        for(String reportidTmp:lstReportids)
        {
            addIncludeSplitPrintReportid(reportidTmp);
        }
    }
    
    public void addIncludeSplitPrintReportid(String reportid)
    {
        if(reportid==null||reportid.trim().equals("")) return;
        if(this.lstIncludeSplitPrintReportIds==null) this.lstIncludeSplitPrintReportIds=new ArrayList<String>();
        if(parent.getPrintPageSize(reportid)>0&&!this.lstIncludeSplitPrintReportIds.contains(reportid))
        {//只有当前报表是分页打印的，才加入lstIncludeSplitPrintReportIds中
            this.lstIncludeSplitPrintReportIds.add(reportid);
        }
        if(this.lstIncludeSplitPrintReportIds.size()==0) this.lstIncludeSplitPrintReportIds=null;
    }
}

