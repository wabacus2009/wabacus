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
package com.wabacus.system.dataset.update;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.dataset.update.action.rationaldb.AbsRationalDBUpdateAction;
import com.wabacus.system.dataset.update.action.rationaldb.SPUpdateAction;
import com.wabacus.system.dataset.update.action.rationaldb.SQLDeleteAction;
import com.wabacus.system.dataset.update.action.rationaldb.SQLInsertAction;
import com.wabacus.system.dataset.update.action.rationaldb.SQLUpdateAction;
import com.wabacus.util.Tools;

public class RationalDBUpdateActionProvider extends AbsUpdateActionProvider
{
    private Boolean isPreparedStatement=null;
    
    private String sqlsps;
    
    private boolean isOriginalParams;

    private boolean isPreparedStatement()
    {
        if(isPreparedStatement==null) isPreparedStatement=this.ownerUpdateBean.isPreparedStatement();
        return isPreparedStatement;
    }
    
    public boolean loadConfig(XmlElementBean eleValueBean)
    {
        if(!super.loadConfig(eleValueBean)) return false;
        String preparedstatement=eleValueBean.attributeValue("preparedstatement");
        if(preparedstatement!=null)
        {
            this.isPreparedStatement=Tools.isEmpty(preparedstatement)?null:preparedstatement.equalsIgnoreCase("true");
        }
        sqlsps=Tools.formatStringBlank(eleValueBean.getContent()).trim();
        isOriginalParams="true".equalsIgnoreCase(eleValueBean.attributeValue("originalparams"));
        return !Tools.isEmpty(sqlsps);
    }

    public List<AbsUpdateAction> parseAllUpdateActions(String reportTypeKey)
    {
        if(Tools.isEmpty(this.sqlsps)) return null;
        ReportBean rbean=this.ownerUpdateBean.getOwner().getReportBean();
        List<String> lstActionscripts=Tools.parseStringToList(this.sqlsps,";",new String[]{"\"","\""},false);
        String rntValTmp;
        String[] tmpArr;
        AbsRationalDBUpdateAction actionTmp;
        List<AbsUpdateAction> lstResults=new ArrayList<AbsUpdateAction>();
        for(String scriptTmp:lstActionscripts)
        {
            if(scriptTmp==null||scriptTmp.trim().equals("")) continue;
            scriptTmp=scriptTmp.trim();
            tmpArr=parseAndRemoveReturnParamname(scriptTmp);
            scriptTmp=tmpArr[0].trim();
            rntValTmp=tmpArr[1];//返回值
            if(scriptTmp.startsWith("{")&&scriptTmp.endsWith("}")) scriptTmp=scriptTmp.substring(1,scriptTmp.length()-1).trim();
            if(scriptTmp.toLowerCase().indexOf("insert ")==0)
            {
                actionTmp=new SQLInsertAction(this.ownerUpdateBean);
            }else if(scriptTmp.toLowerCase().indexOf("update ")==0)
            {
                actionTmp=new SQLUpdateAction(this.ownerUpdateBean);
            }else if(scriptTmp.toLowerCase().indexOf("delete ")==0)
            {
                actionTmp=new SQLDeleteAction(this.ownerUpdateBean);
            }else if(scriptTmp.toLowerCase().indexOf("call ")==0)
            {
                actionTmp=new SPUpdateAction(this.ownerUpdateBean);
            }else
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的更新数据的SQL语句"+scriptTmp+"不合法");
            }
            actionTmp.setDatasource(this.getDatasource());
            actionTmp.setPreparedStatement(this.isPreparedStatement());
            actionTmp.setOriginalParams(isOriginalParams);
            actionTmp.setReturnValueParamname(rntValTmp);
            actionTmp.parseActionScript(scriptTmp,lstResults,reportTypeKey);
        }
        return lstResults;
    }

    private String[] parseAndRemoveReturnParamname(String configsql)
    {
        if(configsql==null||configsql.trim().equals("")) return new String[]{configsql,null};
        int idx=configsql.indexOf("=");
        if(idx<0) return new String[]{configsql,null};
        String returnValName=configsql.substring(0,idx).trim();
        if(Tools.isDefineKey("#",returnValName)||Tools.isDefineKey("rrequest",returnValName))
        {
            returnValName=checkReturnParamname(returnValName);
            return new String[]{configsql.substring(idx+1),returnValName};
        }
        return new String[]{configsql,null};
    }
    
    private String checkReturnParamname(String paramname)
    {
        if(Tools.isEmpty(paramname)) return null;
        if(Tools.isDefineKey("#",paramname))
        {//返回值存放在<params/>定义的变量中
            String paramnameTmp=Tools.getRealKeyByDefine("#",paramname);
            if(paramnameTmp==null||paramnameTmp.trim().equals(""))
            {
                return null;
            }else if(this.ownerUpdateBean.getExternalValueBeanByName(paramnameTmp,false)==null)
            {
                throw new WabacusConfigLoadingException("加载报表"+this.ownerUpdateBean.getOwner().getReportBean()+"的更新SQL语句失败，返回值：#{"+paramnameTmp
                        +"}引用的变量没有在<params/>中定义");
            }
        }else if(Tools.isDefineKey("rrequest",paramname))
        {
            if(Tools.isEmpty(Tools.getRealKeyByDefine("rrequest",paramname))) return null;
        }else
        {
            throw new WabacusConfigLoadingException("加载报表"+this.ownerUpdateBean.getOwner().getReportBean()+"的更新SQL语句失败，返回值："+paramname
                    +"不合法，必须是#{paramname}或rrequset{key}之一的格式");
        }
        return paramname;
    }
}

