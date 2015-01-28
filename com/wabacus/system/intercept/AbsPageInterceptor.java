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

import java.util.List;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.RegexTools;
import com.wabacus.util.Tools;

public abstract class AbsPageInterceptor
{
    private String matchpageids;

    private String matchmode;

    private List<String> lstMatchPageids;

    public String getMatchpageids()
    {
        return matchpageids;
    }

    public void setMatchpageids(String matchpageids)
    {
        this.matchpageids=matchpageids;
    }

    public String getMatchmode()
    {
        return matchmode;
    }

    public void setMatchmode(String matchmode)
    {
        this.matchmode=matchmode;
    }

    public boolean isMatch(String pageid)
    {
        if(pageid==null||pageid.trim().equals("")) return false;
        if(this.matchpageids==null||this.matchpageids.trim().equals("")) return true;
        if(matchmode!=null&&matchmode.trim().equalsIgnoreCase("regex"))
        {//正则表达式匹配
            return RegexTools.isMatch(pageid,this.matchpageids);
        }else
        {
            if(lstMatchPageids==null)
            {
                synchronized(this)
                {
                    if(lstMatchPageids==null) lstMatchPageids=Tools.parseStringToList(this.matchpageids,";",false);
                }
            }
            return lstMatchPageids.contains(pageid);
        }
    }

    public void doStart(ReportRequest rrequest)
    {

    }

    public void doStartSave(ReportRequest rrequest,List<ReportBean> lstSaveReportBeans)
    {

    }

    public void doEndSave(ReportRequest rrequest,List<ReportBean> lstSaveReportBeans)
    {

    }

    public void doEnd(ReportRequest rrequest)
    {

    }

}
