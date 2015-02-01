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
package com.wabacus.config.typeprompt;

import java.util.List;
import java.util.Map;

import com.wabacus.system.ReportRequest;

public class DefaultTypePromptOptionMatcher implements ITypePromptOptionMatcher
{
    public boolean isMatch(ReportRequest rrequest,TypePromptBean typePromptBean,Map<String,String> mOptionValue,String txtValue)
    {
        if(txtValue==null||txtValue.trim().equals("")) return true;
        List<TypePromptColBean> lstPColsBean=typePromptBean.getLstPColBeans();
        String colValueTmp;
        if(!typePromptBean.isCasesensitive()) txtValue=txtValue.toLowerCase();
        for(TypePromptColBean tpColBeanTmp:lstPColsBean)
        {
            colValueTmp=mOptionValue.get(tpColBeanTmp.getLabel());
            if(colValueTmp==null) colValueTmp="";
            if(!typePromptBean.isCasesensitive()) colValueTmp=colValueTmp.toLowerCase();
            if(tpColBeanTmp.getMatchmode()==1&&colValueTmp.indexOf(txtValue)==0||tpColBeanTmp.getMatchmode()==2&&colValueTmp.indexOf(txtValue)>=0)
            {
                return true;
            }
        }
        return false;
    }

}
