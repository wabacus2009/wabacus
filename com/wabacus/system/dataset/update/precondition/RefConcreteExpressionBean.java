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

public class RefConcreteExpressionBean extends AbsConcreteExpressionBean
{
    private AbsExpressionBean refedExpressionBean;

    private boolean comparedValue;

    public void setRefedExpressionBean(AbsExpressionBean refedExpressionBean)
    {
        this.refedExpressionBean=refedExpressionBean;
    }

    public void setComparedValue(boolean comparedValue)
    {
        this.comparedValue=comparedValue;
    }

    public boolean isTrue(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues)
    {
       if(refedExpressionBean==null) return false;
       Boolean expressvalue=(Boolean)rrequest.getAttribute(this.getReportBean().getId(),refedExpressionBean.objectId+"_expression_bolValue");
       if(expressvalue==null)
       {
           expressvalue=refedExpressionBean.isTrue(rrequest,mRowData,mParamValues);
           rrequest.setAttribute(this.getReportBean().getId(),refedExpressionBean.objectId+"_expression_bolValue",expressvalue);
       }
       return expressvalue.booleanValue()==comparedValue;
    }

}
