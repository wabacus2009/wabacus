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
package com.wabacus.system.dataset.update.precondition;

import java.util.Map;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.util.Tools;

public abstract class AbsExpressionBean
{
    protected String objectId;

    public AbsExpressionBean()
    {
        objectId=Tools.generateObjectId(this.getClass());
    }

    public abstract boolean isTrue(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues);


    public static AbsExpressionBean parsePreCondition(String reportTypeKey,AbsEditableReportEditDataBean editBean,String precondition,
            String datasource)
    {
        if(Tools.isEmpty(precondition)) return null;
        precondition=Tools.replaceCharacterInQuote(precondition,'(',"$_LEFTBRACKET_$",true);
        precondition=Tools.replaceCharacterInQuote(precondition,')',"$_RIGHTBRACKET_$",true);
        precondition=Tools.replaceCharacterInQuote(precondition,'[',"$_LEFTBRACKET2_$",true);
        precondition=Tools.replaceCharacterInQuote(precondition,']',"$_RIGHTBRACKET2_$",true);
        precondition=Tools.replaceCharacterInQuote(precondition,'{',"$_LEFTBRACKET3_$",true);
        precondition=Tools.replaceCharacterInQuote(precondition,'}',"$_RIGHTBRACKET3_$",true);
        precondition=Tools.replaceCharacterInQuote(precondition,',',"$_COMMA_$",true);
        CompositeExpressionBeanForLoad compositeExpBean=new CompositeExpressionBeanForLoad();
        compositeExpBean.parsePreConditionExpressionsStart(reportTypeKey,editBean,precondition,datasource);
        return compositeExpBean.parsePreConditionExpressionsEnd();
    }
}
