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
package com.wabacus.system.dataset.update.action.rationaldb;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.buttons.EditableReportSQLButtonDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.util.Tools;

public class SQLUpdateAction extends AbsRationalDBUpdateAction implements Cloneable
{

    public SQLUpdateAction(AbsEditableReportEditDataBean ownerUpdateBean)
    {
        super(ownerUpdateBean);
    }
    
    public void parseActionScript(String sql,List<AbsUpdateAction> lstUpdateActions,String reportTypeKey)
    {
        if(this.isStandardUpdateSql(sql))
        {
            List<EditableReportParamBean> lstDynParamsTmp=new ArrayList<EditableReportParamBean>();
            sql=this.ownerUpdateBean.parseStandardEditSql(sql,lstDynParamsTmp,reportTypeKey,this.isPreparedStatement,this.isOriginalParams);
            this.sqlsp=sql;
            this.isStandardSql=true;
            this.lstParamBeans=lstDynParamsTmp;
            lstUpdateActions.add(this);
        }else
        {
            int idxwhere=sql.toLowerCase().indexOf(" where ");
            String whereclause=null;
            List<EditableReportParamBean> lstParamsBeanInWhereClause=null;
            if(idxwhere>0)
            {
                lstParamsBeanInWhereClause=new ArrayList<EditableReportParamBean>();
                whereclause=sql.substring(idxwhere).trim();
                sql=sql.substring(0,idxwhere).trim();
            }
            AbsDatabaseType dbtype=Config.getInstance().getDataSource(this.datasource).getDbType();
            if(dbtype==null)
            {
                throw new WabacusConfigLoadingException("没有实现数据源"+this.datasource+"对应数据库类型的相应实现类");
            }
            List<SQLUpdateAction> lstRealUpdateSqls=dbtype.constructUpdateSql(sql.trim(),this.ownerUpdateBean.getOwner().getReportBean(),
                    reportTypeKey,this);
            if(!Tools.isEmpty(whereclause))
            {
                whereclause=this.ownerUpdateBean.parseStandardEditSql(whereclause,lstParamsBeanInWhereClause,reportTypeKey,this.isPreparedStatement,this.isOriginalParams);
            }
            List<EditableReportParamBean> lstParamsBean;
            String updatesql;
            for(SQLUpdateAction updateSqlBeanTmp:lstRealUpdateSqls)
            {
                updatesql=updateSqlBeanTmp.getSqlsp();
                lstParamsBean=updateSqlBeanTmp.getLstParamBeans();
                if(whereclause!=null&&!whereclause.trim().equals(""))
                {
                    if(updatesql.indexOf("%where%")>0)
                    {
                        updatesql=Tools.replaceAll(updatesql,"%where%",whereclause);
                    }else
                    {
                        updatesql=updatesql+"  "+whereclause;
                    }
                    lstParamsBean.addAll(lstParamsBeanInWhereClause);
                }
                updateSqlBeanTmp.setSqlsp(updatesql);
                updateSqlBeanTmp.setLstParamBeans(lstParamsBean);
                lstUpdateActions.add(updateSqlBeanTmp);
            }
        }
    }
    
    public List<SQLUpdateAction> constructUpdateSql(String configUpdateSql,ReportBean rbean,String reportTypeKey)
    {
        StringBuilder sqlBuffer=new StringBuilder();
        List<EditableReportParamBean> lstParamsBean=new ArrayList<EditableReportParamBean>();
        int idxleft=configUpdateSql.indexOf("(");
        int placeholderIdxTmp=0;
        EditableReportParamBean paramBean;
        if(idxleft<0||configUpdateSql.endsWith("()"))
        {//没有指定要更新的字段，则将所有从数据库取数据的<col/>（不包括hidden="1"和="2"的<col/>）全部更新到表中
            if(!this.ownerUpdateBean.isAutoReportdata())
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在autoreportdata属性为false的<button/>中，不能配置update table这种不带参数的SQL语句");
            }
            if(configUpdateSql.endsWith("()")) configUpdateSql=configUpdateSql.substring(0,configUpdateSql.length()-2);
            sqlBuffer.append(configUpdateSql).append(" set ");
            for(ColBean cbean:rbean.getDbean().getLstCols())
            {
                paramBean=createParamBeanByColbean(cbean.getProperty(),reportTypeKey,false,false);
                if(paramBean!=null)
                {
                    sqlBuffer.append(cbean.getColumn()+"="+getDynParamStringInSql(paramBean,placeholderIdxTmp++)+",");
                    lstParamsBean.add(paramBean);
                }
            }
        }else
        {
            sqlBuffer.append(configUpdateSql.substring(0,idxleft)).append(" set ");
            int idxright=configUpdateSql.lastIndexOf(")");
            if(idxright!=configUpdateSql.length()-1)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configUpdateSql+"不合法");
            }
            String cols=configUpdateSql.substring(idxleft+1,idxright);
            List<String> lstUpdateCols=Tools.parseStringToList(cols,",",new String[]{"'","'"},false);
            String columnname=null;
            String columnvalue=null;
            ColBean cb;
            for(String updatecol:lstUpdateCols)
            {
                if(updatecol==null||updatecol.trim().equals("")) continue;
                int idxequals=updatecol.indexOf("=");
                if(idxequals>0)
                {//配置为“字段名=值”格式
                    columnname=updatecol.substring(0,idxequals).trim();
                    columnvalue=updatecol.substring(idxequals+1).trim();
                    Object paramObjTmp=this.createEditParams(columnvalue,reportTypeKey);
                    if(paramObjTmp==null) continue;
                    sqlBuffer.append(columnname+"=");
                    if(paramObjTmp instanceof EditableReportParamBean)
                    {
                        sqlBuffer.append(getDynParamStringInSql((EditableReportParamBean)paramObjTmp,placeholderIdxTmp++));
                        lstParamsBean.add((EditableReportParamBean)paramObjTmp);
                    }else
                    {
                        sqlBuffer.append(paramObjTmp);
                    }
                    sqlBuffer.append(",");
                }else
                {
                    if(!Tools.isDefineKey("@",updatecol))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的修改数据SQL语句"+configUpdateSql+"不合法，更新的字段值必须采用@{}括住");
                    }
                    if(this.ownerUpdateBean.isAutoReportdata())
                    {
                        updatecol=Tools.getRealKeyByDefine("@",updatecol);
                        String realColProperty=updatecol.trim();
                        if(realColProperty.endsWith("__old")) realColProperty=realColProperty.substring(0,realColProperty.length()-"__old".length());
                        cb=rbean.getDbean().getColBeanByColProperty(realColProperty);
                        if(cb==null)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，配置的要更新字段"+updatecol+"不合法，没有取到其值对应的<col/>");
                        }
                        paramBean=createParamBeanByColbean(updatecol,reportTypeKey,true,true);
                        lstParamsBean.add(paramBean);
                        sqlBuffer.append(cb.getColumn()+"="+getDynParamStringInSql(paramBean,placeholderIdxTmp++)+",");
                    }else
                    {
                        ((EditableReportSQLButtonDataBean)this.ownerUpdateBean).setHasReportDataParams(true);
                        paramBean=new EditableReportParamBean();
                        paramBean.setParamname(updatecol);
                        lstParamsBean.add(paramBean);
                        sqlBuffer.append(Tools.getRealKeyByDefine("@",updatecol)+"="+getDynParamStringInSql(paramBean,placeholderIdxTmp++)+",");
                    }
                }
            }
        }
        if(sqlBuffer.charAt(sqlBuffer.length()-1)==',') sqlBuffer.deleteCharAt(sqlBuffer.length()-1);
        List<SQLUpdateAction> lstUpdateSqlActions=new ArrayList<SQLUpdateAction>();
        this.sqlsp=sqlBuffer.toString();
        this.lstParamBeans=lstParamsBean;
        lstUpdateSqlActions.add(this);
        return lstUpdateSqlActions;
    }
    
    private String getDynParamStringInSql(EditableReportParamBean paramBean,int placeholderIdx)
    {
        if(this.isPreparedStatement) return "?";
        paramBean.setPlaceholder("[PLACE_HOLDER_"+placeholderIdx+"]");
        return paramBean.getPlaceholder();
    }
    
    private boolean isStandardUpdateSql(String updatesql)
    {
        updatesql=updatesql==null?"":updatesql.toLowerCase().trim();
        if(!updatesql.startsWith("update ")) return true;
        updatesql=updatesql.substring("update ".length()).trim();
        if(updatesql.equals("")) return true;
        updatesql=Tools.replaceCharacterInQuote(updatesql,'(',"$_LEFTBRACKET_$",true);
        updatesql=Tools.replaceCharacterInQuote(updatesql,')',"$_RIGHTBRACKET_$",true);
        int idxBracket1=updatesql.indexOf("(");
        if(idxBracket1==0) return true;
        if(idxBracket1<0)
        {
            if(updatesql.indexOf(" ")<0&&updatesql.indexOf(",")<0&&updatesql.indexOf("=")<0) return false;//update tablename格式
        }else
        {
            String tablename=updatesql.substring(0,idxBracket1).trim();
            if(tablename.indexOf(" ")>=0||tablename.indexOf(",")>=0||tablename.indexOf("=")>=0) return true;
            updatesql=updatesql.substring(idxBracket1+1);
            int idxleft=1;
            int idxRightBacket=-1;
            for(int i=0;i<updatesql.length();i++)
            {
                if(updatesql.charAt(i)=='(')
                {
                    idxleft++;
                }else if(updatesql.charAt(i)==')')
                {
                    if(idxleft==1)
                    {//此右括号与第一个左括号匹配
                        idxRightBacket=i;
                        break;
                    }else if(idxleft<=0)
                    {
                        return true;
                    }else
                    {
                        idxleft--;
                    }
                }
            }
            if(idxRightBacket==-1) return true;
            if(idxRightBacket==0&&(updatesql.equals(")")||updatesql.substring(1).trim().startsWith("where "))) return false;
            updatesql=updatesql.substring(idxRightBacket+1).trim();
            if(updatesql.equals("")||updatesql.startsWith("where ")) return false;
        }
        return true;
    }
    
    public SQLUpdateAction cloneWithAllDefaultValues()
    {
        try
        {
            return (SQLUpdateAction)super.clone();
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone对象失败",e);
        }
    }
}

