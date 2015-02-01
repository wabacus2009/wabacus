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
package com.wabacus.system.buttons;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class BackButton extends WabacusButton
{
    public BackButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    public String getButtonType()
    {
        return Consts_Private.BACK_BUTTON;
    }

    public String showMenu(ReportRequest rrequest,String dynclickevent)
    {
        String clickevent=getClickEvent(rrequest,dynclickevent);
        if(clickevent==null||clickevent.trim().equals(""))
            return "";
        else
            return super.showMenu(rrequest,clickevent);
    }

    public String showButton(ReportRequest rrequest,String dynclickevent)
    {
        String clickevent=getClickEvent(rrequest,dynclickevent);
        if(clickevent==null||clickevent.trim().equals(""))
            return "";
        else
            return super.showButton(rrequest,clickevent);
    }

    public String showButton(ReportRequest rrequest,String dynclickevent,String button)
    {
        String clickevent=getClickEvent(rrequest,dynclickevent);
        if(clickevent==null||clickevent.trim().equals(""))
            return "";
        else
            return super.showButton(rrequest,clickevent,button);
    }

    private String getClickEvent(ReportRequest rrequest,String dynclickevent)
    {
        if(rrequest.getLstAncestorUrls()==null||rrequest.getLstAncestorUrls().size()==0) return "";
        String clickevent=dynclickevent;
        if(clickevent==null||clickevent.trim().equals(""))
        {
            clickevent=rrequest.getStringAttribute("BACK_ACTION_EVENT","");
            if(clickevent.equals(""))
            {
                String parenturl=rrequest.getLstAncestorUrls().get(0);
                parenturl=Tools.convertBetweenStringAndAscii(parenturl,false);
                if(rrequest.getLstAncestorUrls().size()>1)
                {
                    StringBuffer tempBuf=new StringBuffer();
                    for(int i=1,len=rrequest.getLstAncestorUrls().size();i<len;i++)
                    {
                        tempBuf.append(rrequest.getLstAncestorUrls().get(i)).append("||");
                    }
                    if(tempBuf.toString().endsWith("||"))
                    {
                        tempBuf.delete(tempBuf.length()-2,tempBuf.length());
                    }
                    parenturl=Tools.replaceUrlParamValue(parenturl,"ancestorPageUrls",tempBuf.toString());
                }
                parenturl=Tools.replaceUrlParamValue(parenturl,"refreshComponentGuid","[OUTERPAGE]"+rrequest.getPagebean().getId());
                parenturl=Tools.replaceUrlParamValue(parenturl,"SLAVE_REPORTID",null);
                clickevent="refreshComponent('"+Tools.jsParamEncode(parenturl)+"',null,{keepSelectedRowsAction:true,keepSavingRowsAction:true})";
                rrequest.setAttribute("BACK_ACTION_EVENT",clickevent);
            }
        }
        return clickevent;
    }
}
