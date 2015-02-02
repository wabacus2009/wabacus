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
package com.wabacus.system.dataset.select.rationaldbassistant;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.Oracle;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.util.Tools;

public class SPDataSetValueBean
{
    private String procedure;
    
    private List<String> lstStoreProcedureParams;

    private ISPDataSetProvider ownerSpProvider;
    
    public SPDataSetValueBean(ISPDataSetProvider spProvider)
    {
        this.ownerSpProvider=spProvider;
    }
    
    public String getProcedure()
    {
        return procedure;
    }

    public List<String> getLstStoreProcedureParams()
    {
        return lstStoreProcedureParams;
    }
    
    public ISPDataSetProvider getOwnerSpProvider()
    {
        return ownerSpProvider;
    }

    public void parseStoreProcedure(ReportBean rbean,String configProcedure)
    {
        int idxLeft=configProcedure.indexOf("(");
        int idxRight=configProcedure.lastIndexOf(")");
        if(idxLeft>0&&idxRight==configProcedure.length()-1)
        {
            StringBuffer spBuf=new StringBuffer(configProcedure.substring(0,idxLeft+1));
            String procParams=configProcedure.substring(idxLeft+1,idxRight);//取到存储过程中的参数
            if(!procParams.trim().equals(""))
            {
                List<String> lstParams=Tools.parseStringToList(procParams,",",new String[]{"'","'"},false);
                List<String> lstSPParams=new ArrayList<String>();
                for(String paramTmp:lstParams)
                {
                    if(paramTmp.trim().equals("")||WabacusAssistant.getInstance().isGetRequestContextValue(paramTmp))
                    {
                        lstSPParams.add(paramTmp);
                    }else if(paramTmp.startsWith("'")&&paramTmp.endsWith("'"))
                    {
                        lstSPParams.add(paramTmp.substring(1,paramTmp.length()-1));
                    }else
                    {
                        if(this.ownerSpProvider.getConditionBeanByName(paramTmp)==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，存储过程"+configProcedure+"引用的name为"+paramTmp+"的动态条件不存在");
                        }
                        lstSPParams.add("condition{"+paramTmp+"}");
                    }
                    spBuf.append("?,");
                }
                this.lstStoreProcedureParams=lstSPParams;
            }
            configProcedure=spBuf.toString();
        }else
        {//没有配置存储过程的参数
            configProcedure=configProcedure+"(";
        }
        if(this.ownerSpProvider.isUseSystemParams()) configProcedure=configProcedure+"?,";
        if(Config.getInstance().getDataSource(this.ownerSpProvider.getDatasource()).getDbType() instanceof Oracle)
        {
            configProcedure=configProcedure+"?";
        }else if(configProcedure.endsWith(","))
        {
            configProcedure=configProcedure.substring(0,configProcedure.length()-1);
        }
        this.procedure="{"+configProcedure+")}";
    }
}

