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
package com.wabacus.system.dataset.select.report.value;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.component.application.report.condition.ConditionExpressionBean;
import com.wabacus.config.component.application.report.condition.ConditionInSqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.dataset.select.rationaldbassistant.BatchStatisticItems;
import com.wabacus.system.dataset.select.rationaldbassistant.report.AbsGetReportDataSetBySQL;
import com.wabacus.system.dataset.select.rationaldbassistant.report.GetReportAllDataSetBySQL;
import com.wabacus.system.dataset.select.rationaldbassistant.report.GetReportPartDataSetBySQL;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.AbsReportSQLConvertLevel;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.CompleteConvertSQLevel;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.NonConvertSQLevel;
import com.wabacus.system.dataset.select.report.value.sqlconvertor.PartConvertSQLevel;
import com.wabacus.util.Tools;

public class SQLReportDataSetValueProvider extends RelationalDBReportDataSetValueProvider
{
    public final static String startRowNumPlaceHolder="{#startrownum#}";
    
    public final static String endRowNumPlaceHolder="{#endrownum#}";
    
    public final static String pagesizePlaceHolder="{#pagesize#}";
    
    public final static String orderbyPlaceHolder="{#orderby#}";
    
    public final static String orderbyInversePlaceHolder="{#orderbyinverse#}";//SQL语句中的反向order by占位符
    
    public final static String filterConditionPlaceHolder="{#filtercondition#}";
    
    public final static String rowselectvaluesConditionPlaceHolder="{#rowselectvaluescondition#}";
    
    public final static String dependsConditionPlaceHolder="{#dependscondition#}";
    
    private final static List<String> lstKeyPlaceHoldersInSQL=new ArrayList<String>();
    
    static
    {
        lstKeyPlaceHoldersInSQL.add(startRowNumPlaceHolder);
        lstKeyPlaceHoldersInSQL.add(endRowNumPlaceHolder);
        lstKeyPlaceHoldersInSQL.add(pagesizePlaceHolder);
        lstKeyPlaceHoldersInSQL.add(orderbyPlaceHolder);
        lstKeyPlaceHoldersInSQL.add(orderbyInversePlaceHolder);
        lstKeyPlaceHoldersInSQL.add(filterConditionPlaceHolder);
        lstKeyPlaceHoldersInSQL.add(rowselectvaluesConditionPlaceHolder);
        lstKeyPlaceHoldersInSQL.add(dependsConditionPlaceHolder);
    }
    
    private AbsReportSQLConvertLevel sqlConvertObj;
    
    private List<ConditionInSqlBean> lstConditionInSqlBeans;//存放所有在SQL语句中指定的查询条件信息
    
    public AbsReportSQLConvertLevel getSqlConvertObj()
    {
        return sqlConvertObj;
    }

    public List<ConditionInSqlBean> getLstConditionInSqlBeans()
    {
        return lstConditionInSqlBeans;
    }

    public List<String> getColFilterDataSet(ReportRequest rrequest,ColBean filterColBean,boolean isGetSelectedOptions,int maxOptionCount)
    {
        ReportBean rbean=this.getReportBean();
        return parseColFilterResultDataset(rrequest,filterColBean,(new GetReportAllDataSetBySQL(rrequest,rbean,this,rbean.getSbean()
                .isSelectPreparedStatement())).getColFilterDataSet(filterColBean,isGetSelectedOptions),maxOptionCount);
    }
    
    public int getRecordcount(ReportRequest rrequest)
    {
        ReportBean rbean=this.getReportBean();
        GetReportPartDataSetBySQL sqlDataSetObj=new GetReportPartDataSetBySQL(rrequest,rbean,this,
                rbean.getSbean().isSelectPreparedStatement());
        rrequest.setAttribute(rbean.getId(),"WX_GetDataSet_Obj_"+this.ownerDataSetValueBean.getGuid(),sqlDataSetObj);
        return parseRecordCount(sqlDataSetObj.getRecordcount());
    }
    
    public List<Map<String,Object>> getDataSet(ReportRequest rrequest,List<AbsReportDataPojo> lstReportData,int startRownum,int endRownum)
    {
        if(startRownum>=0&&endRownum>0&&startRownum>endRownum||endRownum==0) return null;
        ReportBean rbean=this.getReportBean();
        AbsGetReportDataSetBySQL sqlDataSetObj=null;
        if(startRownum>=0||endRownum>0)
        {
            if(startRownum<0) startRownum=0;
            if(endRownum<0) endRownum=Integer.MAX_VALUE;
            sqlDataSetObj=(GetReportPartDataSetBySQL)rrequest.getAttribute(rbean.getId(),"WX_GetDataSet_Obj_"+this.ownerDataSetValueBean.getGuid());
            if(sqlDataSetObj==null)
            {
                sqlDataSetObj=new GetReportPartDataSetBySQL(rrequest,rbean,this,rbean.getSbean().isSelectPreparedStatement());
            }
            ((GetReportPartDataSetBySQL)sqlDataSetObj).setStartRownum(startRownum);
            ((GetReportPartDataSetBySQL)sqlDataSetObj).setEndRownum(endRownum);
        }else
        {
            sqlDataSetObj=new GetReportAllDataSetBySQL(rrequest,rbean,this,rbean.getSbean().isSelectPreparedStatement());
        }
        return parseResultDataset(rrequest,sqlDataSetObj.getReportDataSet(lstReportData));
    }
    
    protected Object getStatisticDataSet(ReportRequest rrequest,BatchStatisticItems batStatitems,String statisticsql,int startRownum,int endRownum)
    {
        if(endRownum==0) return null;
        ReportBean rbean=this.getReportBean();
        AbsGetReportDataSetBySQL sqlDataSetObj=null;
        if(startRownum>=0&&endRownum>0)
        {//加载针对某页数据的统计
            if(startRownum>=endRownum) return null;
            sqlDataSetObj=new GetReportPartDataSetBySQL(rrequest,rbean,this,rbean.getSbean().isSelectPreparedStatement());
            ((GetReportPartDataSetBySQL)sqlDataSetObj).setStartRownum(startRownum);
            ((GetReportPartDataSetBySQL)sqlDataSetObj).setEndRownum(endRownum);
        }else
        {
            sqlDataSetObj=new GetReportAllDataSetBySQL(rrequest,rbean,this,rbean.getSbean().isSelectPreparedStatement());
        }
        return sqlDataSetObj.getStatisticDataSet(batStatitems,statisticsql);
    }
    
    protected CrossListReportDatasetValueBean createCrossListDataSetValueBean()
    {
        return new CrossListReportSQLDatasetValueBean();
    }
    
    public String getDynamicSql(ReportRequest rrequest)
    {
        if(this.clrdvbean==null) return null;
        return ((CrossListReportSQLDatasetValueBean)this.clrdvbean).getDynamicSql(rrequest);
    }
    
    public ResultSet loadCrossListReportVerticalStatiResultSet(ReportRequest rrequest)
    {
        if(this.clrdvbean==null) return null;
        return ((CrossListReportSQLDatasetValueBean)this.clrdvbean).loadVerticalStatiData(rrequest);
    }
    
    public void loadConfig(XmlElementBean eleValueBean)
    {
        super.loadConfig(eleValueBean);
        String sqlconvertlevel=eleValueBean.attributeValue("sqlconvertlevel");
        if(sqlconvertlevel==null||sqlconvertlevel.trim().equals(""))
        {
            sqlconvertlevel=Config.getInstance().getSystemConfigValue("default-report-sqlconvertlevel","");
        }
        if("none".equalsIgnoreCase(sqlconvertlevel)||this.getReportBean().getSbean().isHorizontalDataset())
        {
            this.sqlConvertObj=new NonConvertSQLevel(this);
        }else if("complete".equalsIgnoreCase(sqlconvertlevel))
        {
            this.sqlConvertObj=new CompleteConvertSQLevel(this);
        }else
        {
            this.sqlConvertObj=new PartConvertSQLevel(this);
        }
        this.sqlConvertObj.loadConfig(eleValueBean);
    }

    public void afterSqlLoad()
    {
        super.afterSqlLoad();
        if(this.ownerDataSetValueBean.getMDependParents()!=null&&this.ownerDataSetValueBean.getMDependParents().size()>0)
        {
            if(this.value.indexOf(dependsConditionPlaceHolder)<0)
            {
                throw new WabacusConfigLoadingException("加载报表"+this.ownerDataSetValueBean.getReportBean().getPath()+"的id为"
                        +this.ownerDataSetValueBean.getId()+"的<value/>失败，此数据集依赖其它数据集，但没有在SQL语句中指定{#dependscondition#}占位符");
            }
        }
    }

    public void doPostLoad()
    {
        super.doPostLoad();
        parseConditionInSql();//解析SQL语句中直接指定的查询条件
        validateConditionsConfig();
        this.sqlConvertObj.parseSql(this.value);
    }

    private void parseConditionInSql()
    {
        if(this.value==null||this.value.trim().equals("")) return;
        ReportBean rbean=this.ownerDataSetValueBean.getReportBean();
        List<ConditionInSqlBean> lstConditionsInSqlBeans=new ArrayList<ConditionInSqlBean>();
        ConditionInSqlBean csbeanTmp;
        int placeholderIndex=0;
        String sql=this.value;
        StringBuffer sqlBuf=new StringBuffer();
        int idxBracketStart;
        int idxBracketEnd;
        int idxJingStart;
        int idxJingEnd;//存放sql语句中第二个有效#号的下标
        while(true)
        {
            idxBracketStart=getValidIndex(sql,'{',0);
            idxBracketEnd=getValidIndex(sql,'}',0);
            idxJingStart=getValidIndex(sql,'#',0);
            if(idxJingStart<0)
            {
                idxJingEnd=-1;
            }else
            {
                idxJingEnd=getValidIndex(sql,'#',idxJingStart+1);
            }
            if(idxBracketStart<0&&idxBracketEnd<0&&idxJingStart<0&&idxJingEnd<0) break;
            //            System.out.println("====================================================================================================");
            validateCondition(sql,idxBracketStart,idxBracketEnd,idxJingStart,idxJingEnd);
            if(idxJingEnd>=0&&(idxJingEnd<idxBracketStart||idxBracketStart<0))
            {
                String prex=sql.substring(0,idxJingStart);
                String expression=sql.substring(idxJingStart,idxJingEnd+1);
                String suffix=sql.substring(idxJingEnd+1);
                if(expression.equals("#dynamic-columns#"))
                {
                    prex=prex.trim();
                    suffix=suffix.trim();
                    if(prex.endsWith("[")&&suffix.startsWith("]")||prex.endsWith("(")&&suffix.startsWith(")"))
                    {
                        sqlBuf.append(prex).append("#dynamic-columns#").append(suffix.substring(0,1));
                        sql=suffix.substring(1);
                        continue;
                    }
                }
                String conditionname=expression;
                expression="#data#";//这里用#data#占位符即可，方便解析pattern，不用标识<condition/>的name，因为会在对应的ConditionInSqlBean的中标识
                if(prex.endsWith("%"))
                {
                    prex=prex.substring(0,prex.length()-1);
                    expression="%"+expression;
                }
                if(prex.endsWith("'"))
                {
                    prex=prex.substring(0,prex.length()-1);
                    expression="'"+expression;
                }
                if(suffix.startsWith("%"))
                {
                    suffix=suffix.substring(1);
                    expression=expression+"%";
                }
                if(suffix.startsWith("'"))
                {
                    suffix=suffix.substring(1);
                    expression=expression+"'";
                }
                sql=suffix;
                csbeanTmp=new ConditionInSqlBean(this.ownerDataSetValueBean);
                lstConditionsInSqlBeans.add(csbeanTmp);
                csbeanTmp.setConditionname(conditionname);
                csbeanTmp.setPlaceholder(" [CONDITION_PLACEHOLDER_"+placeholderIndex+"] ");
                ConditionExpressionBean expressionBean=new ConditionExpressionBean();
                csbeanTmp.setConditionExpression(expressionBean);
                expressionBean.setValue(expression);
                if(rbean.getSbean().isSelectPreparedStatement()) expressionBean.parseConditionExpression();
                sqlBuf.append(prex).append(csbeanTmp.getPlaceholder());
                placeholderIndex++;
            }else if(idxBracketStart<idxJingStart&&idxBracketEnd>idxJingEnd&&idxBracketStart>=0&&idxJingEnd>=0)
            {
                //这里面可能有多套#name#，但必须name相同
                sqlBuf.append(sql.substring(0,idxBracketStart));
                String expression=sql.substring(idxBracketStart,idxBracketEnd+1);
                if(lstKeyPlaceHoldersInSQL.contains(expression))
                {
                    sqlBuf.append(" ").append(expression).append(" ");
                }else if(expression.equals("{#condition#}"))
                {
                    csbeanTmp=new ConditionInSqlBean(this.ownerDataSetValueBean);
                    csbeanTmp.setConditionname("{#condition#}");
                    lstConditionsInSqlBeans.add(csbeanTmp);
                    sqlBuf.append(" {#condition#} ");
                }else
                {
                    csbeanTmp=new ConditionInSqlBean(this.ownerDataSetValueBean);
                    csbeanTmp.setPlaceholder(" [CONDITION_PLACEHOLDER_"+placeholderIndex+"] ");
                    sqlBuf.append(csbeanTmp.getPlaceholder());
                    if(idxBracketStart==0&&idxJingStart==1&&idxBracketEnd==expression.length()-1&&idxJingEnd==expression.length()-2)
                    {
                        csbeanTmp.setConditionname(expression);
                    }else
                    {
                        expression=expression.substring(1,expression.length()-1);
                        String conditionname=sql.substring(idxJingStart+1,idxJingEnd);//放在一个{}中的一定是从同一个<condition/>（即name属性相同）中取值做为条件，因此在这里可以取到此name属性（在##之间的值），
                        if(conditionname.equals("data"))
                        {//在sql语句中配置为#data#形式
                            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的查询SQL语句"+this.value
                                    +"失败，不能在其中直接使用占位符#data#，这是一个框架做为保留字的字符串，请使用#conditionname#格式");
                        }
                        expression=Tools.replaceAll(expression,"#"+conditionname+"#","#data#");
                        csbeanTmp.setConditionname(conditionname);
                        ConditionExpressionBean expressionBean=new ConditionExpressionBean();
                        csbeanTmp.setConditionExpression(expressionBean);
                        expressionBean.setValue(expression);
                        if(rbean.getSbean().isSelectPreparedStatement()) expressionBean.parseConditionExpression();
                    }
                    lstConditionsInSqlBeans.add(csbeanTmp);
                    placeholderIndex++;
                }
                sql=sql.substring(idxBracketEnd+1);
            }else
            {
                throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的SQL语句："+this.value+"中的动态条件失败，无法解析其中用{}和##括住的动态条件");
            }
        }
        if(!sql.equals("")) sqlBuf.append(sql);
        if(lstConditionsInSqlBeans==null||lstConditionsInSqlBeans.size()==0
                ||(lstConditionsInSqlBeans.size()==1&&lstConditionsInSqlBeans.get(0).getConditionname().equals("{#condition#}")))
        {
            this.lstConditionInSqlBeans=null;
        }else
        {
            this.value=sqlBuf.toString();
            this.lstConditionInSqlBeans=lstConditionsInSqlBeans;
        }
        this.value=Tools.replaceAllOnetime(this.value,"\\{","{");
        this.value=Tools.replaceAllOnetime(this.value,"\\}","}");
        this.value=Tools.replaceAllOnetime(this.value,"\\#","#");
    }

    private void validateCondition(String sql,int idxBracketStart,int idxBracketEnd,int idxJingStart,int idxJingEnd)
    {
        ReportBean rbean=this.ownerDataSetValueBean.getReportBean();
        if(idxBracketStart>=0&&idxBracketEnd<0||idxBracketStart<0&&idxBracketEnd>=0||idxBracketStart>=idxBracketEnd&&idxBracketEnd>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的SQL语句："+this.value+"中的动态条件失败，{和}没有成对");
        }
        if(idxJingStart>=0&&idxJingEnd<0)
        {
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的SQL语句："+this.value+"中的动态条件失败，#号没有成对");
        }
        int nextValidBracketStartIdx=getValidIndex(sql,'{',idxBracketStart+1);//当前{的下一个有效{号的下标
        if(nextValidBracketStartIdx>0&&idxBracketEnd>nextValidBracketStartIdx)
        {
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的SQL语句："+this.value+"中的动态条件失败，{和}没有成对");
        }
        if(idxBracketStart>=0&&sql.substring(idxBracketStart+1,idxBracketEnd).trim().equals(""))
        {
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的SQL语句："+this.value+"中的动态条件失败,，{和}之间不是有效的条件表达式");
        }
        if(idxJingStart>=0&&sql.substring(idxJingStart+1,idxJingEnd).trim().equals(""))
        {
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的SQL语句："+this.value+"中的动态条件失败，#和#之间不是有效的<condition/>的name属性值");
        }
        if(idxBracketStart<idxJingStart&&idxBracketEnd<idxJingEnd&&idxBracketStart>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的SQL语句："+this.value+"中的动态条件失败，{、}、#、#之间的关系混乱");
        }
        if(idxBracketEnd<idxJingStart&&idxBracketStart>=0)
        {//ab{cd}e#fg#形式，即{}之间没有##，即没有指定动态条件的name
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的SQL语句："+this.value+"中的动态条件失败，{、}之间的条件表达式没有指定动态条件的name属性");
        }
        if(idxBracketStart>idxJingStart&&idxBracketStart<idxJingEnd&&idxJingStart>=0)
        {
            throw new WabacusConfigLoadingException("解析报表"+rbean.getPath()+"的SQL语句："+this.value+"中的动态条件失败，{、}、#、#之间的关系混乱");
        }
    }

    private int getValidIndex(String sql,char sign,int startindex)
    {
        if(sql==null||sql.equals("")) return -1;
        char c;
        for(int i=startindex,len=sql.length();i<len;i++)
        {
            c=sql.charAt(i);
            if(c==sign)
            {
                if(i==startindex) return i;
                if(sql.charAt(i-1)=='\\')
                {
                    i++;
                }else
                {
                    return i;
                }
            }
        }
        return -1;
    }

    private void validateConditionsConfig()
    {
        List<String> lstConditionNamesInSql=new ArrayList<String>();//在sql语句中通过#name#指定了条件的<condition/>的name集合
        ReportBean rbean=this.ownerDataSetValueBean.getReportBean();
        if(this.lstConditionInSqlBeans!=null&&this.lstConditionInSqlBeans.size()>0)
        {//在sql语句中通过#name#指定了某些条件做为条件表达式
            ConditionBean cbReferedTmp;
            String conditionNameTmp;
            for(ConditionInSqlBean csbeanTmp:this.lstConditionInSqlBeans)
            {
                conditionNameTmp=csbeanTmp.getRealConditionname();
                if(conditionNameTmp==null||conditionNameTmp.trim().equals("")) continue;
                if(!lstConditionNamesInSql.contains(conditionNameTmp)) lstConditionNamesInSql.add(conditionNameTmp);
                cbReferedTmp=rbean.getSbean().getConditionBeanByName(conditionNameTmp);
                if(cbReferedTmp==null)
                {
                    throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的SQL语句："+this.value+"中引用的name属性为"+conditionNameTmp
                            +"的<condition/>不存在");
                }
                if(cbReferedTmp.getIterator()>1)
                {
                    throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的SQL语句："+this.value+"中引用的name属性为"+conditionNameTmp
                            +"的<condition/>的iterator值大于1");
                }
            }
        }
        //            if(lstConditionNamesInSql.contains(cbeanTmp.getName())) continue;//在<sql/>中通过#name#形式指定了条件的<condition/>可以不在<condition/>中配置条件表达式
        //            /**
        //             */
        //            {//此条件没有配置条件表达式
        //                /**
        //                 * 这里不抛出异常，因为这个条件可能用在了其它地方，比如交叉统计报表的<tablenameconditions/>中定义的查询条件如果需要输入框，则需在<sql/>中定义条件，
        //                 * 然后在<tablenameconditions/>中定义<condition/>中引用它，并在这里指定条件表达式，这个时候就不用在<sql/>的<condition/>中定义条件表达式了
        //                 */
        //                
        //                //throw new WabacusConfigLoadingException("报表"+this.getReportBean()+"的name属性为"+cbeanTmp.getName()
        //                       // +"的查询条件即没有以#name#的形式出现在sql语句中，也没有在<condition/>中配置条件表达式");
    }

    public AbsReportDataSetValueProvider clone(ReportDataSetValueBean newOwnerDataSetValueBean)
    {
        SQLReportDataSetValueProvider newProvider=(SQLReportDataSetValueProvider)super.clone(newOwnerDataSetValueBean);
        if(this.sqlConvertObj!=null) newProvider.sqlConvertObj=this.sqlConvertObj.clone(newProvider);
        if(lstConditionInSqlBeans!=null)
        {
            List<ConditionInSqlBean> lstConditionInSqlBeansNew=new ArrayList<ConditionInSqlBean>();
            for(ConditionInSqlBean csbeanTmp:lstConditionInSqlBeans)
            {
                lstConditionInSqlBeansNew.add(csbeanTmp.clone());
            }
            newProvider.lstConditionInSqlBeans=lstConditionInSqlBeansNew;
        }
        return newProvider;
    }
    
    protected class CrossListReportSQLDatasetValueBean extends AbsReportDataSetValueProvider.CrossListReportDatasetValueBean
    {
        private String getDynamicSql(ReportRequest rrequest)
        {
            return sqlConvertObj.getCrossListDynamicSql(rrequest,getDynamicCommonSelectCols(rrequest),getDynamicCrossStatiSelectCols(rrequest));
        }
        
        protected ResultSet loadVerticalStatiData(ReportRequest rrequest)
        {
            if(Tools.isEmpty(sqlConvertObj.getGetVerticalStatisDataSql())) return null;
            String crossStatiDynCols=getDynamicCrossStatiSelectColsWithVerticalStati(rrequest).trim();
            if(crossStatiDynCols.equals("")) return null;
            String statisql=Tools.replaceAll(sqlConvertObj.getGetVerticalStatisDataSql(),sqlConvertObj.getDynamicColsPlaceholder(),crossStatiDynCols);
            ReportBean rbean=getReportBean();
            if(ownerDataSetValueBean.isDependentDataSet())
            {
                List lstReportData=null;
                CacheDataBean cdb=rrequest.getCdb(rbean.getId());
                if(!cdb.isLoadAllReportData())
                {
                    lstReportData=(List)rrequest.getAttribute(rbean.getId()+"wx_all_data_tempory");
                    if(lstReportData==null)
                    {
                        lstReportData=ReportAssistant.getInstance().loadReportDataSet(rrequest,rrequest.getDisplayReportTypeObj(rbean),true);
                        rrequest.setAttribute(rbean.getId()+"wx_all_data_tempory",lstReportData);
                    }
                }else
                {
                    lstReportData=rrequest.getDisplayReportTypeObj(rbean).getLstReportData();
                }
                statisql=ReportAssistant.getInstance().replaceSQLConditionPlaceHolderByRealValue(rbean,statisql,dependsConditionPlaceHolder,
                        ownerDataSetValueBean.getRealDependsConditionExpression(lstReportData));
            }
            GetReportAllDataSetBySQL sqlDataSetObj=new GetReportAllDataSetBySQL(rrequest,rbean,SQLReportDataSetValueProvider.this,rbean.getSbean().isSelectPreparedStatement());
            Object objTmp=sqlDataSetObj.getDataSet(rrequest.getDisplayReportTypeObj(rbean),statisql);
            if(objTmp==null) return null;
            if(!(objTmp instanceof ResultSet))
            {
                throw new WabacusRuntimeException("获取报表："+rbean.getPath()+"针对每列数据的垂直统计失败，此时不能在拦截器中返回非ResultSet类型的数据");
            }
            ResultSet rs=(ResultSet)objTmp;
            try
            {
                if(!rs.next())
                {//没有数据
                    rs.close();
                    rs=null;
                }
            }catch(SQLException e)
            {
                e.printStackTrace();
            }
            return rs;
        }
        
        protected void doPostLoad()
        {
            sqlConvertObj.doPostLoadCrossList(this.lstIncludeCommonCrossColAndGroupBeans,this.lstIncludeCrossStatiColAndGroupBeans);
        }
    }
}
