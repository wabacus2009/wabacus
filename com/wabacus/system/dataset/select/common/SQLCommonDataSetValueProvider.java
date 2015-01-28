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
package com.wabacus.system.dataset.select.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.config.database.type.SQLSERVER2K;
import com.wabacus.config.database.type.SQLSERVER2K5;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.crosslist.AbsCrossListReportColAndGroupBean;
import com.wabacus.system.dataset.select.rationaldbassistant.GetDataSetBySQL;
import com.wabacus.system.inputbox.AbsSelectBox;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.inputbox.option.TypepromptOptionBean;
import com.wabacus.util.Tools;

public class SQLCommonDataSetValueProvider extends RelationalDBCommonDataSetValueProvider
{
    private static Log log=LogFactory.getLog(SQLCommonDataSetValueProvider.class);
    
    private Boolean isPreparedStatement=null;

    private boolean isFinalSqlInDynamicCol;
    
    public boolean isPreparedStatement()
    {
        if(this.isPreparedStatement==null)
        {
            this.isPreparedStatement=this.getReportBean().getSbean().isPreparedStatement();
        }
        return isPreparedStatement;
    }

    public List<Map<String,String>> getLstSelectBoxOptions(ReportRequest rrequest,Map<String,String> mParentInputboxValues)
    {
        String sql=this.value;
        Map<String,Boolean> mParentIds=((AbsSelectBox)this.ownerOptionBean.getOwnerInputboxObj()).getMParentIds();
        if(mParentIds!=null&&mParentIds.size()>0)
        {
            String  parentValTmp;
            for(String parentNameTmp:mParentIds.keySet())
            {
                parentValTmp=mParentInputboxValues.get(parentNameTmp);
                parentValTmp=Tools.removeSQLKeyword(parentValTmp);
                if(parentValTmp==null||parentValTmp.equals(""))
                {
                    sql=Tools.replaceAll(sql,"#["+parentNameTmp+"]#","");
                    sql=Tools.replaceAll(sql,"#"+parentNameTmp+"#","_WX_NONE_OPTION_VALUE_");//配置这种占位符，则说明一个都不取
                }else if(parentValTmp.equals("[%ALL%]"))
                {
                    sql=Tools.replaceAll(sql,"#["+parentNameTmp+"]#","");
                    sql=Tools.replaceAll(sql,"#"+parentNameTmp+"#","");
                }else
                {
                    sql=Tools.replaceAll(sql,"#["+parentNameTmp+"]#",parentValTmp);
                    sql=Tools.replaceAll(sql,"#"+parentNameTmp+"#",parentValTmp);
                }
            }
        }
        GetDataSetBySQL sqlDataSet=new GetDataSetBySQL(rrequest,this.getReportBean(),this.isPreparedStatement());
        return parseOptionsDataSet(sqlDataSet.getCommonDataSet(this,this.ownerOptionBean,sql),getMSelectBoxColKeyAndColumns(),-1);
    }

    public List<Map<String,String>> getLstTypePromptOptions(ReportRequest rrequest,String txtValue)
    {
        txtValue=txtValue==null?"":txtValue.trim();
        if(!this.isPreparedStatement()) txtValue=Tools.removeSQLKeyword(txtValue);
        String sqlTemp=Tools.replaceAll(this.value,"#data#",txtValue);
        GetDataSetBySQL sqlDataSet=new GetDataSetBySQL(rrequest,this.getReportBean(),this.isPreparedStatement());
        TypePromptBean typePromptBean=((TextBox)this.ownerOptionBean.getOwnerInputboxObj()).getTypePromptBean();
        return parseOptionsDataSet(sqlDataSet.getCommonDataSet(this,this.ownerOptionBean,sqlTemp),getMTypePromptColKeyAndColumns(),
                typePromptBean.getResultcount());
    }

    public Map<String,String> getAutoCompleteColumnsData(ReportRequest rrequest,Map<String,String> mParams)
    {
        if(mParams==null||mParams.size()==0) return null;
        String realColConditionExpression=this.ownerAutoCompleteBean.getColvalueConditionExpressionForSql(mParams);
        String sqlTmp=Tools.replaceAll(this.value,"{#condition#}","{@condition@} and "+realColConditionExpression);//保留{#condition#}，因为后面还有可能拼凑<condition/>中配置的条件
        sqlTmp=Tools.replaceAll(sqlTmp,"{@condition@}","{#condition#}");
        GetDataSetBySQL sqlDataSet=new GetDataSetBySQL(rrequest,this.getReportBean(),this.isPreparedStatement());
        return parseAutoCompleteDataSet(sqlDataSet.getCommonDataSet(this,this.ownerAutoCompleteBean,sqlTmp));
    }

    public List<Map<String,String>> getDynamicColGroupDataSet(ReportRequest rrequest)
    {
        GetDataSetBySQL sqlDataSet=new GetDataSetBySQL(rrequest,this.getReportBean(),this.isPreparedStatement());
        return parseDynamicColGroupDataSet(sqlDataSet.getCommonDataSet(this,this.ownerCrossReportColAndGroupBean,this.getValue()));
    }

    public void loadConfig(XmlElementBean eleDatasetAttributeOwnerBean)
    {
        super.loadConfig(eleDatasetAttributeOwnerBean);
        String preparedstatement=eleDatasetAttributeOwnerBean.attributeValue("preparedstatement");
        if(preparedstatement!=null)
        {
            if(preparedstatement.trim().equals(""))
            {
                this.isPreparedStatement=null;
            }else
            {
                this.isPreparedStatement=preparedstatement.trim().equalsIgnoreCase("true");
            }
        }
        if(this.ownerCrossReportColAndGroupBean!=null)
        {
            String finalsql=eleDatasetAttributeOwnerBean.attributeValue("finalsql");
            if(finalsql!=null) this.isFinalSqlInDynamicCol=finalsql.toLowerCase().trim().equals("true");
        }
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        if(this.ownerCrossReportColAndGroupBean!=null&&!this.isFinalSqlInDynamicCol)
        {
            String[] selectColsAndOrderbyArr=this.ownerCrossReportColAndGroupBean.getSelectColumnsAndOrderbyClause(getMOrderbysFromDatasetSql());
            if(selectColsAndOrderbyArr==null||selectColsAndOrderbyArr.length==0) return;
            if(!Tools.isEmpty(selectColsAndOrderbyArr[1])
                    ||(this.value.toLowerCase().trim().indexOf("select ")!=0||this.value.toLowerCase().indexOf(" from ")<=0))
            {//如果order by不为空，说明是动态列是<group/>，则必须加上各分组列的order by；如果配置的是表名，也要构造查询的SQL语句
                StringBuffer getDyncolsSqlBuf=new StringBuffer();
                getDyncolsSqlBuf.append("select distinct ").append(selectColsAndOrderbyArr[0]);
                getDyncolsSqlBuf.append(" from ");
                if(this.value.toLowerCase().trim().indexOf("select ")>=0&&this.value.toLowerCase().indexOf(" from ")>0)
                {
                    getDyncolsSqlBuf.append(" (").append(this.value).append(")  tbl_getdyncol");
                }else
                {
                    getDyncolsSqlBuf.append(this.value);
                }
                getDyncolsSqlBuf.append(" ").append(selectColsAndOrderbyArr[1]);
                this.value=getDyncolsSqlBuf.toString();
            }
            //没有order by，说明交叉统计列是单个<col/>，没有<group/>，且直接配置的是SQL语句，此时不用对SQL语句进行解析，以名丢失了它的order by
        }else
        {
            if(this.value.toLowerCase().trim().indexOf("select ")!=0||this.value.toLowerCase().indexOf(" from ")<0)
            {//不是配置的sql语句，可能是直接配置的表名，则将它转换为一个SQL语句
                this.value="select * from "+this.value+" where {#condition#}";
            }else if(this.lstConditions!=null&&this.lstConditions.size()>0&&this.value.toLowerCase().trim().indexOf("{#condition#}")<0)
            {
                log
                        .warn("报表"+this.getReportBean().getPath()
                                +"配置了<datasetconditions/>，但没有在dataset属性配置的SQL语句中没有指定{#condition#}动态条件占位符，框架将多嵌套一层查询并自动加上");
                this.value="select * from ("+this.value+") wx_tblTmp where {#condition#}";
            }
            if(this.ownerOptionBean instanceof TypepromptOptionBean)
            {
                if(this.value.indexOf(TypePromptBean.MATCHCONDITION_PLACEHOLDER)<0)
                {
                    this.value="select * from ("+this.value+") tblTypepromptTmp where "+TypePromptBean.MATCHCONDITION_PLACEHOLDER;
                }
                this.value=Tools.replaceAll(this.value,TypePromptBean.MATCHCONDITION_PLACEHOLDER,((TypepromptOptionBean)this.ownerOptionBean)
                        .getMatchColSQLConditionExpression());
            }else if(this.ownerAutoCompleteBean!=null)
            {
                if(this.value.toLowerCase().trim().indexOf("{#condition#}")<0)
                {
                    log.warn("报表"+getReportBean().getPath()+"上的输入框"+this.ownerAutoCompleteBean.getOwner().getOwner().getInputBoxId()+"的自动填充配置的数据集"
                            +this.value+"是SQL语句，没有指定{#condition#}占位符，框架将自动加上此占位符");
                    this.value="select * from ("+this.value+") wx_tblAutoCompleteTmp where {#condition#}";
                }
            }
        }
    }

    private Map<String,String> getMOrderbysFromDatasetSql()
    {
        String sqlTemp=Tools.removeBracketAndContentInside(this.value,true);
        Map<String,String> mResults=new HashMap<String,String>();
        sqlTemp=Tools.replaceAll(sqlTemp,"  "," ");
        int idx_orderby=sqlTemp.toLowerCase().lastIndexOf("order by");
        if(idx_orderby>0)
        {//SQL语句中有order by子句
            AbsDatabaseType dbtype=Config.getInstance().getDataSource(this.getDatasource()).getDbType();
            if(dbtype instanceof SQLSERVER2K||dbtype instanceof SQLSERVER2K5)
            {
                this.value=Tools.replaceAll(this.value,"  "," ");
                this.value=this.value.substring(0,this.value.toLowerCase().lastIndexOf("order by"));
            }
            String orderbyTmp=sqlTemp.substring(idx_orderby+"order by ".length());
            List<String> lstOrderByColumns=Tools.parseStringToList(orderbyTmp,",",false);
            String ordercolumn, ordertype;
            for(String orderby_tmp:lstOrderByColumns)
            {
                if(orderby_tmp==null||orderby_tmp.trim().equals("")) continue;
                orderby_tmp=orderby_tmp.trim();
                int idx=orderby_tmp.indexOf(".");
                if(idx>0) orderby_tmp=orderby_tmp.substring(idx+1).trim();
                idx=orderby_tmp.indexOf(" ");
                if(idx>0)
                {//有空格，说明指定了排序顺序，即column asc|desc格式
                    ordercolumn=orderby_tmp.substring(0,idx).trim();
                    ordertype=orderby_tmp.substring(idx+1).trim();
                }else
                {
                    ordercolumn=orderby_tmp;
                    ordertype="asc";
                }
                mResults.put(ordercolumn.toLowerCase(),ordertype);
            }
        }
        return mResults;
    }

    
    protected void parseCondition(ReportBean rbean,ConditionBean cbean,ConditionBean cbeanRefered)
    {
        boolean hasConditionExpression=cbean.getConditionExpression()!=null&&!Tools.isEmpty(cbean.getConditionExpression().getValue());
        super.parseCondition(rbean,cbean,cbeanRefered);
        if(this.isPreparedStatement()&&(cbeanRefered!=null&&hasConditionExpression||cbeanRefered==null))
        {
            cbean.getConditionExpression().parseConditionExpression();
        }
    }

}
