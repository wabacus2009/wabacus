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
package com.wabacus.system.component.application.report.configbean.editablereport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.buttons.EditableReportSQLButtonDataBean;
import com.wabacus.system.dataset.update.AbsUpdateActionProvider;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.dataset.update.precondition.AbsExpressionBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public abstract class AbsEditableReportEditDataBean implements Cloneable
{
    private String refreshParentidOnSave;////如果当前报表是可编辑从报表，则保存数据时需要刷新它的哪个主报表（因为可能有多层主报表）
    
    private boolean resetNavigateInfoOnRefreshParent;
    
    private Boolean isPreparedStatement=null;
    
    private String datasource;
    
    private List<Map<AbsExpressionBean,List<AbsUpdateAction>>> lstEditActions;
    
    private List<AbsUpdateActionProvider> lstEditActionProviders;//在加载时存放所有<value/>对应的provider对象，加载完成后就会清空
    
    protected List<EditableReportExternalValueBean> lstExternalValues;//通过<params/>配置的外部值

    private Map<String,String> mPreconditionExpressions;//在在<preconditions/>中配置的所有<precondition/>子标签前置条件表达式
    
    private Map<String,AbsExpressionBean> mExpressionBeans;//在doPostLoad()中根据mPreconditionExpressions解析得到的前置条件表达式对象
    
    private IEditableReportEditGroupOwnerBean owner;

    public AbsEditableReportEditDataBean(IEditableReportEditGroupOwnerBean owner)
    {
        this.owner=owner;
        lstEditActionProviders=new ArrayList<AbsUpdateActionProvider>();
    }

    public boolean isPreparedStatement()
    {
        if(isPreparedStatement==null)
        {
            isPreparedStatement=this.owner.getReportBean().getSbean().isPreparedStatement();
        }
        return isPreparedStatement;
    }

    public void setPreparedStatement(String isPreparedStatement)
    {
        if(isPreparedStatement==null||isPreparedStatement.trim().equals(""))
        {
            this.isPreparedStatement=null;
        }else
        {
            this.isPreparedStatement=isPreparedStatement.trim().equalsIgnoreCase("true");
        }
    }
    
    public String getDatasource()
    {
        if(datasource==null||datasource.trim().equals(""))
        {
            datasource=this.owner.getReportBean().getSbean().getDatasource();
        }
        return datasource;
    }

    public void setDatasource(String datasource)
    {
        this.datasource=datasource;
    }
    
    public String getRefreshParentidOnSave()
    {
        return refreshParentidOnSave;
    }

    public void setRefreshParentidOnSave(String refreshParentidOnSave)
    {
        this.refreshParentidOnSave=refreshParentidOnSave;
    }

    public boolean isResetNavigateInfoOnRefreshParent()
    {
        return resetNavigateInfoOnRefreshParent;
    }

    public void setResetNavigateInfoOnRefreshParent(boolean resetNavigateInfoOnRefreshParent)
    {
        this.resetNavigateInfoOnRefreshParent=resetNavigateInfoOnRefreshParent;
    }

    public void setMPreconditionExpressions(Map<String,String> mPreconditionExpressions)
    {
        if(mPreconditionExpressions!=null&&mPreconditionExpressions.size()==0) mPreconditionExpressions=null;
        this.mPreconditionExpressions=mPreconditionExpressions;
    }

    public AbsExpressionBean getPreconditionExpressionBean(String name)
    {
        if(this.mExpressionBeans==null) return null;
        return this.mExpressionBeans.get(name);
    }
    
    public List<AbsUpdateAction> getLsAllEditActions()
    {
        if(this.lstEditActions==null) return null;
        List<AbsUpdateAction> lstResult=new ArrayList<AbsUpdateAction>();
        List<AbsUpdateAction> lstTmp;
        for(Map<AbsExpressionBean,List<AbsUpdateAction>> mTmp:this.lstEditActions)
        {
            if(mTmp==null||mTmp.size()==0) continue;
            lstTmp=mTmp.entrySet().iterator().next().getValue();
            if(lstTmp==null||lstTmp.size()==0) continue;
            lstResult.addAll(lstTmp);
        }
        return lstResult;
    }

    public List<AbsUpdateAction> getLstAllExecuteEditActions(ReportRequest rrequest,Map<String,String> mRowData,
            Map<String,String> mParamValues)
    {
        if(this.lstEditActions==null) return null;
        List<AbsUpdateAction> lstResult=new ArrayList<AbsUpdateAction>();
        List<AbsUpdateAction> lstTmp;
        AbsExpressionBean exressBeanTmp;
        for(Map<AbsExpressionBean,List<AbsUpdateAction>> mTmp:this.lstEditActions)
        {
            if(mTmp==null||mTmp.size()==0) continue;
            exressBeanTmp=mTmp.keySet().iterator().next();
            if(exressBeanTmp!=null&&!exressBeanTmp.isTrue(rrequest,mRowData,mParamValues)) continue;
            lstTmp=mTmp.get(exressBeanTmp);
            if(lstTmp==null||lstTmp.size()==0) continue;
            lstResult.addAll(lstTmp);
        }
        return lstResult;
    }
    
    public void addEditActionProvider(AbsUpdateActionProvider editActionProvider)
    {
        this.lstEditActionProviders.add(editActionProvider);
    }
    
    public List<EditableReportExternalValueBean> getLstExternalValues()
    {
        return lstExternalValues;
    }

    public void setLstExternalValues(List<EditableReportExternalValueBean> lstExternalValues)
    {
        this.lstExternalValues=lstExternalValues;
    }

    public IEditableReportEditGroupOwnerBean getOwner()
    {
        return owner;
    }

    public void setOwner(IEditableReportEditGroupOwnerBean owner)
    {
        this.owner=owner;
    }

    public boolean isAutoReportdata()
    {
        return true;
    }
    
    public EditableReportExternalValueBean getExternalValueBeanByName(String name,boolean isMust)
    {
        if(lstExternalValues!=null)
        {
            for(EditableReportExternalValueBean evbeanTmp:lstExternalValues)
            {
                if(evbeanTmp.getName().equals(name)) return evbeanTmp;
            }
        }
        if(isMust)
        {
            throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"失败，没有在<params/>中定义name属性为"
                    +name+"对应的变量值");
        }
        return null;
    }

    public int parseActionscripts(String reportTypeKey)
    {
        if(lstExternalValues!=null&&lstExternalValues.size()>0)
        {
            for(EditableReportExternalValueBean evbeanTmp:lstExternalValues)
            {
                evbeanTmp.parseValues(reportTypeKey);
            }
        }
        if(this.lstEditActionProviders==null||this.lstEditActionProviders.size()==0) return 0;
        if(this.mPreconditionExpressions!=null&&this.mPreconditionExpressions.size()>0)
        {
            this.mExpressionBeans=new HashMap<String,AbsExpressionBean>();
            for(Entry<String,String> entryTmp:this.mPreconditionExpressions.entrySet())
            {
                this.mExpressionBeans.put(entryTmp.getKey(),AbsExpressionBean.parsePreCondition(reportTypeKey,this,entryTmp.getValue(),this
                        .getDatasource()));
            }
        }
        List<Map<AbsExpressionBean,List<AbsUpdateAction>>> lstRealEditActions=new ArrayList<Map<AbsExpressionBean,List<AbsUpdateAction>>>();
        Map<AbsExpressionBean,List<AbsUpdateAction>> mActionsTmp;
        List<AbsUpdateAction> lstActionsTmp;
        for(AbsUpdateActionProvider actionProviderTmp:this.lstEditActionProviders)
        {
            lstActionsTmp=actionProviderTmp.parseAllUpdateActions(reportTypeKey);
            if(lstActionsTmp==null||lstActionsTmp.size()==0) continue;
            mActionsTmp=new HashMap<AbsExpressionBean,List<AbsUpdateAction>>();
            mActionsTmp.put(AbsExpressionBean.parsePreCondition(reportTypeKey,this,actionProviderTmp.getPrecondition(),actionProviderTmp
                    .getDatasource()),lstActionsTmp);
            lstRealEditActions.add(mActionsTmp);
        }
        if(lstRealEditActions.size()==0) return 0;
        this.lstEditActions=lstRealEditActions;
        this.lstEditActionProviders=null;
        return 1;
    }

    public String parseStandardEditSql(String sql,List<EditableReportParamBean> lstDynParams,String reportTypeKey,boolean isPreparedstatement,
            boolean includeQuote)
    {
        sql=EditableReportAssistant.getInstance().parseStandardEditSql(this.owner.getReportBean(),sql,lstDynParams,isPreparedstatement,includeQuote);
        if(lstDynParams.size()>0)
        {
            for(EditableReportParamBean paramBeanTmp:lstDynParams)
            {
                parseStandardSqlParamBean(paramBeanTmp,reportTypeKey);
            }
        }
        return sql;
    }
    
    private void parseStandardSqlParamBean(EditableReportParamBean paramBean,String reportTypeKey)
    {
        if(Tools.isDefineKey("@",paramBean.getParamname()))
        {
            if(this.isAutoReportdata())
            {//自动获取某列的数据进行保存操作（这个判断主要是针对配置更新脚本的<button/>，因为它的@{param}数据有可能是客户端传的，而不是从报表中获取的）
                String configproperty=Tools.getRealKeyByDefine("@",paramBean.getParamname());
                String realproperty=configproperty;
                if(realproperty.endsWith("__old")) realproperty=realproperty.substring(0,realproperty.length()-"__old".length());
                ColBean cbeanUpdateDest=this.owner.getReportBean().getDbean().getColBeanByColProperty(realproperty);
                if(cbeanUpdateDest==null)
                {
                    throw new WabacusConfigLoadingException("解析报表的更新语句失败，没有找到column/property属性为"+realproperty+"的列");
                }
                if(cbeanUpdateDest.isNonValueCol()||cbeanUpdateDest.isSequenceCol()||cbeanUpdateDest.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.owner.getReportBean().getPath()+"失败，列"+cbeanUpdateDest.getColumn()
                            +"不是从数据库获取数据的列，不能取其数据");
                }
                EditableReportColBean ercbeanDest=(EditableReportColBean)cbeanUpdateDest.getExtendConfigDataForReportType(reportTypeKey);
                if(ercbeanDest==null)
                {
                    ercbeanDest=new EditableReportColBean(cbeanUpdateDest);
                    cbeanUpdateDest.setExtendConfigDataForReportType(reportTypeKey,ercbeanDest);
                }else
                {
                    paramBean.setDefaultvalue(ercbeanDest.getDefaultvalue());
                }
                paramBean.setOwner(cbeanUpdateDest);
                ColBean cbeanUpdateSrc=cbeanUpdateDest;
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbeanUpdateDest.getDisplaytype(true)))
                {
                    ColBean cbSrcTmp=cbeanUpdateDest.getUpdateColBeanSrc(false);
                    if(cbSrcTmp!=null) cbeanUpdateSrc=cbSrcTmp;
                }
                setParamBeanInfoOfColBean(cbeanUpdateSrc,paramBean,configproperty,reportTypeKey);
            }else
            {
                ((EditableReportSQLButtonDataBean)this).setHasReportDataParams(true);
            }
        }else if(Tools.isDefineKey("#",paramBean.getParamname()))
        {
            String paramname=Tools.getRealKeyByDefine("#",paramBean.getParamname());
            paramBean.setParamname(paramname);
            paramBean.setOwner(this.getExternalValueBeanByName(paramname,true));
        }
    }
    
    public void setParamBeanInfoOfColBean(ColBean cbUpdateSrc,EditableReportParamBean paramBean,String configColProperty,String reportTypeKey)
    {
        paramBean.setParamname(configColProperty);
    }

    public void doPostLoadFinally()
    {
        /*if(!this.isAutoReportdata()) return;
        if(this.lstEditActions!=null)
        {
            AbsExpressionBean expressBeanTmp;
            List<AbsUpdateAction> lstActionsTmp;
            for(Map<AbsExpressionBean,List<AbsUpdateAction>> mEditActionsTmp:this.lstEditActions)
            {
                if(mEditActionsTmp==null||mEditActionsTmp.size()==0) continue;
                expressBeanTmp=mEditActionsTmp.keySet().iterator().next();
                if(expressBeanTmp!=null) expressBeanTmp.doPostLoadFinally();
                lstActionsTmp=mEditActionsTmp.get(expressBeanTmp);
                if(lstActionsTmp==null||lstActionsTmp.size()==0) continue;
                for(AbsUpdateAction actionTmp:lstActionsTmp)
                {
                    actionTmp.doPostLoadFinally();
                }
            }
        }
        if(lstExternalValues!=null)
        {
            for(EditableReportExternalValueBean valueBeanTmp:this.lstExternalValues)
            {
                valueBeanTmp.doPostLoadFinally();
            }
        }*/
    }
    
    public Object clone(IEditableReportEditGroupOwnerBean newowner)
    {
        try
        {
            AbsEditableReportEditDataBean newbean=(AbsEditableReportEditDataBean)super.clone();
            newbean.setOwner(newowner);
            if(this.lstEditActionProviders!=null)
            {
                List<AbsUpdateActionProvider> lstBeansNew=new ArrayList<AbsUpdateActionProvider>();
                for(AbsUpdateActionProvider actionBeanTmp:this.lstEditActionProviders)
                {
                    lstBeansNew.add((AbsUpdateActionProvider)actionBeanTmp.clone(newbean));
                }
                newbean.lstEditActionProviders=lstBeansNew;
            }

            if(lstExternalValues!=null)
            {
                List<EditableReportExternalValueBean> lstExternalValuesNew=new ArrayList<EditableReportExternalValueBean>();
                for(EditableReportExternalValueBean valueBeanTmp:lstExternalValues)
                {
                    lstExternalValuesNew.add((EditableReportExternalValueBean)valueBeanTmp.clone());
                }
                newbean.setLstExternalValues(lstExternalValuesNew);
            }
            return newbean;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
