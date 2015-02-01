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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportDeleteDataBean;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.util.Tools;

public class JavaUpdateActionProvider extends AbsUpdateActionProvider
{
    private static Log log=LogFactory.getLog(JavaUpdateActionProvider.class);
    
    private String strclasses;

    public boolean loadConfig(XmlElementBean eleValueBean)
    {
        if(!super.loadConfig(eleValueBean)) return false;
        strclasses=Tools.formatStringBlank(eleValueBean.getContent()).trim();
        return !Tools.isEmpty(strclasses);
    }

    public List<AbsUpdateAction> parseAllUpdateActions(String reportTypeKey)
    {
        if(Tools.isEmpty(strclasses)) return null;
        ReportBean rbean=this.ownerUpdateBean.getOwner().getReportBean();
        List<String> lstActionscripts=Tools.parseStringToList(this.strclasses,";",new String[]{"\"","\""},false);
        List<AbsUpdateAction> lstResults=new ArrayList<AbsUpdateAction>();
        for(String scriptTmp:lstActionscripts)
        {
            if(scriptTmp==null||scriptTmp.trim().equals("")) continue;
            scriptTmp=scriptTmp.trim();
            String javaname=scriptTmp;
            String params=null;
            int idx1=scriptTmp.indexOf("(");
            int idx2=scriptTmp.indexOf(")");
            if(idx1>0&&idx2==scriptTmp.length()-1)
            {
                javaname=scriptTmp.substring(0,idx1).trim();
                params=scriptTmp.substring(idx1+1,idx2).trim();
            }else if(idx1>=0||idx2>=0)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的更新数据JAVA类"+scriptTmp+"不合法");
            }
            Object javaActionBean;
            try
            {
                Class c=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(javaname);
                javaActionBean=c.getConstructor(new Class[]{AbsEditableReportEditDataBean.class}).newInstance(this.ownerUpdateBean);
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的更新数据JAVA类"+scriptTmp+"无法实例化",e);
            }
            if(!(javaActionBean instanceof AbsUpdateAction))
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的更新数据JAVA类"+scriptTmp+"没有继承"
                        +AbsUpdateAction.class.getName());
            }
            parseParams(params,reportTypeKey,(AbsUpdateAction)javaActionBean);
            lstResults.add((AbsUpdateAction)javaActionBean);
        }
        return lstResults;
    }

    private void parseParams(String params,String reportTypeKey,AbsUpdateAction javaaction)
    {
        if(params==null||params.trim().equals("")) return;
        if(this.ownerUpdateBean.isAutoReportdata()&&!(this.ownerUpdateBean instanceof EditableReportDeleteDataBean))
        {
            List<String> lstParamsTmp=Tools.parseStringToList(params,",",new String[]{"'","'"},false);
            for(String paramTmp:lstParamsTmp)
            {
                if(paramTmp==null||paramTmp.trim().equals("")) continue;
                if(!Tools.isDefineKey("@",paramTmp))
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.ownerUpdateBean.getOwner().getReportBean().getPath()
                            +"失败，配置的更新数据JAVA类中指定的参数"+paramTmp+"不合法，对于JAVA类，只能在参数列表中指定@{column}/@{column__old}两种之一的格式，如果要传入其它类型的参数，请配置相应的<param/>");
                }
                javaaction.createParamBeanByColbean(Tools.getRealKeyByDefine("@",paramTmp),reportTypeKey,true,true);
            }
        }else
        {
            log.warn("报表"+this.ownerUpdateBean.getOwner().getReportBean().getPath()+"的<delete/>或<button/>中配置的JAVA类不需要在括号中指定"
                    +params+"参数，在这里的指定没有任何作用，可以在<params/>中定义要传入JAVA类中的参数");
        }
    }
}

