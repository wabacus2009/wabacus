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

import java.util.List;
import java.util.Map;

import com.wabacus.system.ReportRequest;

public class CompositeExpressionBean extends AbsExpressionBean
{
    private String logic;

    private List<AbsExpressionBean> lstChildExpressionBeans;

    public void setLogic(String logic)
    {
        this.logic=logic;
    }

    public void setLstChildExpressionBeans(List<AbsExpressionBean> lstChildExpressionBeans)
    {
        this.lstChildExpressionBeans=lstChildExpressionBeans;
    }

    public boolean isTrue(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues)
    {
        if(lstChildExpressionBeans==null||lstChildExpressionBeans.size()==0) return true;
        return "or".equalsIgnoreCase(logic)?isTrueForOr(rrequest,mRowData,mParamValues):isTrueForAnd(rrequest,mRowData,mParamValues);
    }

    public boolean isTrueForOr(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues)
    {
        for(AbsExpressionBean childExpressBeanTmp:this.lstChildExpressionBeans)
        {
            if(childExpressBeanTmp==null) continue;
            if(childExpressBeanTmp.isTrue(rrequest,mRowData,mParamValues)) return true;
        }
        return false;
    }

    public boolean isTrueForAnd(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues)
    {
        for(AbsExpressionBean childExpressBeanTmp:this.lstChildExpressionBeans)
        {
            if(childExpressBeanTmp==null) continue;
            if(!childExpressBeanTmp.isTrue(rrequest,mRowData,mParamValues)) return false;
        }
        return true;
    }

//    {
//                if(childExpressBeanTmp!=null) childExpressBeanTmp.doPostLoadFinally();
}
