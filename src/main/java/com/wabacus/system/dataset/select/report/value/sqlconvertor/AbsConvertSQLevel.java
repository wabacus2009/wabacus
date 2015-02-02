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
package com.wabacus.system.dataset.select.report.value.sqlconvertor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.crosslist.AbsCrossListReportColAndGroupBean;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Tools;

public abstract class AbsConvertSQLevel extends AbsReportSQLConvertLevel
{
    protected String convertedSql;
    
    protected String pagesplitSql;
    
    protected String kernelSql;
    
    protected String countSql;
    
    protected String filterSql;
    
    private String getVerticalStatisDataSql;
    
    public AbsConvertSQLevel(SQLReportDataSetValueProvider ownerProvider)
    {
        super(ownerProvider);
    }
    
    public String getConvertedSql()
    {
        return convertedSql;
    }

    public String getPagesplitSql()
    {
        return pagesplitSql;
    }

    public String getKernelSql()
    {
        return kernelSql;
    }

    public String getGetVerticalStatisDataSql()
    {
        return getVerticalStatisDataSql;
    }
    
    public String getCrossListDynamicSql(ReportRequest rrequest,String commonDynCols,String crossStatiDynCols)
    {
        String allSelectCols=null;
        if(crossStatiDynCols.equals(""))
        {
            allSelectCols=commonDynCols;
        }else if(commonDynCols.equals(""))
        {
            allSelectCols=crossStatiDynCols;
        }else
        {
            allSelectCols=commonDynCols+","+crossStatiDynCols;
        }
        String sql=null;
        if(allSelectCols.trim().equals("")&&this.dynamicColsPlaceholder.equals("(#dynamic-columns#)"))
        {//当前没有查询到动态列，而且没有动态列时不需查询报表数据
            sql="[NONE]";
        }else
        {
            sql=kernelSql;
            if(sql.indexOf(GROUPBY_DYNAMICOLUMNS_PLACEHOLDER)>0)
            {
                if(crossStatiDynCols.equals(""))
                {
                    Map<String,String> mSqlParts=parseStatiSql(sql);
                    sql=mSqlParts.get("prevselectpart")+" select "+mSqlParts.get("selectcolumnspart")+" from "+mSqlParts.get("frompart")+" "
                            +mSqlParts.get("lastpart");
                }else if(commonDynCols.equals(""))
                {//没有普通动态列，则把group by中普通动态列的占位符去掉
                    Map<String,String> mSqlParts=parseStatiSql(sql);
                    String groupbypart=mSqlParts.get("groupbypart").trim();
                    if(groupbypart.equals(GROUPBY_DYNAMICOLUMNS_PLACEHOLDER))
                    {
                        sql=mSqlParts.get("prevselectpart")+" select "+mSqlParts.get("selectcolumnspart")+" from "+mSqlParts.get("frompart")+" "
                                +mSqlParts.get("lastpart");
                    }else
                    {
                        groupbypart=replaceDynColPlaceHolder(groupbypart,"",GROUPBY_DYNAMICOLUMNS_PLACEHOLDER);
                        sql=mSqlParts.get("prevselectpart")+" select "+mSqlParts.get("selectcolumnspart");
                        sql=sql+" from "+mSqlParts.get("frompart")+" group by "+groupbypart+" "+mSqlParts.get("lastpart");
                    }
                }else
                {
                    sql=Tools.replaceAll(sql,GROUPBY_DYNAMICOLUMNS_PLACEHOLDER,commonDynCols);
                }
            }
            sql=replaceDynColPlaceHolder(sql,allSelectCols,this.dynamicColsPlaceholder);
        }
        return sql;
    }
    
    protected void parseSqlKernelAndOrderBy()
    {
        String sqlTemp=Tools.removeBracketAndContentInside(this.originalSql,true);
        sqlTemp=Tools.replaceAll(sqlTemp,"  "," ");
        if(sqlTemp.toLowerCase().indexOf(" order by ")>0)
        {
            int idx_orderby=this.originalSql.toLowerCase().lastIndexOf(" order by ");
            this.kernelSql=this.originalSql.substring(0,idx_orderby);
            String orderbyTmp=this.originalSql.substring(idx_orderby+" order by ".length());
            //下面代码将order by 子句中的所有排序字段前面的表名去掉。因为在select * from ("+sqlnew+") wabacus_temp_tbl  %orderby% 中的order by 子句中不能出现表名
            List<String> lstOrderByColumns=Tools.parseStringToList(orderbyTmp,",",false);
            StringBuffer orderbuf=new StringBuffer();
            for(String orderby_tmp:lstOrderByColumns)
            {
                if(orderby_tmp==null||orderby_tmp.trim().equals("")) continue;
                orderby_tmp=orderby_tmp.trim();
                int idx=orderby_tmp.indexOf(".");
                if(idx>0)
                {
                    orderbuf.append(orderby_tmp.substring(idx+1));
                }else
                {
                    orderbuf.append(orderby_tmp);
                }
                orderbuf.append(",");
            }
            if(orderbuf.length()>0&&orderbuf.charAt(orderbuf.length()-1)==',') orderbuf.deleteCharAt(orderbuf.length()-1);
            orderby=orderbuf.toString();
        }else
        {//没有配置order by，取第一个<col/>的字段为默认排序字段
            this.kernelSql=this.originalSql;
            String column=null;
            for(ColBean cbTmp:getReportBean().getDbean().getLstCols())
            {
                if(!cbTmp.isControlCol()&&!cbTmp.isNonFromDbCol()&&!cbTmp.isNonValueCol()&&!cbTmp.isSequenceCol()
                        &&cbTmp.isMatchDataSet(ownerProvider.getOwnerDataSetValueBean()))
                {
                    column=cbTmp.getColumn();
                    if(column!=null&&!column.trim().equals("")&&!column.startsWith("{")) break;
                }
            }
            orderby=column;
        }
    }
    
    public void doPostLoadCrossList(List<AbsCrossListReportColAndGroupBean> lstIncludeCommonCrossColAndGroupBeans,
            List<AbsCrossListReportColAndGroupBean> lstIncludeCrossStatiColAndGroupBeans)
    {
        super.doPostLoadCrossList(lstIncludeCommonCrossColAndGroupBeans,lstIncludeCrossStatiColAndGroupBeans);
        if(lstIncludeCrossStatiColAndGroupBeans!=null&&lstIncludeCrossStatiColAndGroupBeans.size()>0)
        {
            if(this.originalSql.toLowerCase().indexOf(" group ")<0||this.originalSql.toLowerCase().indexOf(" by ")<0)
            {
                throw new WabacusConfigLoadingException("加载报表"+getReportBean().getPath()+"失败，查询动态列的数据集的SQL语句"+this.originalSql+"没有指定 group by");
            }
            Map<String,String> mSqlParts=parseStatiSql(this.originalSql);
            if(lstIncludeCommonCrossColAndGroupBeans!=null&&lstIncludeCommonCrossColAndGroupBeans.size()>0)
            {
                String groupbypart=mSqlParts.get("groupbypart");
                if(groupbypart.indexOf(this.dynamicColsPlaceholder)>0)
                {//开发人员已经在group by中通过占位符指定了普通动态列的位置
                    groupbypart=Tools.replaceAll(groupbypart,this.dynamicColsPlaceholder,GROUPBY_DYNAMICOLUMNS_PLACEHOLDER);
                }else
                {
                    groupbypart=groupbypart+","+GROUPBY_DYNAMICOLUMNS_PLACEHOLDER;
                }
                this.originalSql=mSqlParts.get("prevselectpart")+" select "+mSqlParts.get("selectcolumnspart");
                this.originalSql=this.originalSql+" from "+mSqlParts.get("frompart")+" group by "+groupbypart+" "+mSqlParts.get("lastpart");
            }
            for(AbsCrossListReportColAndGroupBean cgBeanTmp:lstIncludeCrossStatiColAndGroupBeans)
            {
                if(cgBeanTmp.getInnerDynamicColBean().isHasVerticalstatistic())
                {
                    String lastpart=mSqlParts.get("lastpart");
                    int idx=lastpart.indexOf(")");
                    if(idx>=0)
                    {//如果当前SQL是里层的子查询语句
                        lastpart=lastpart.substring(idx);
                    }else
                    {
                        lastpart="";
                    }
                    this.getVerticalStatisDataSql=mSqlParts.get("prevselectpart")+" select "+this.dynamicColsPlaceholder+" from "
                            +mSqlParts.get("frompart")+" "+lastpart;
                    this.getVerticalStatisDataSql=this.getVerticalStatisDataSql.trim();
                    break;
                }
            }
        }
    }

    protected Map<String,String> parseStatiSql(String sql)
    {
        String frompart="", groupbypart="", lastpart="";
        String[] sqlArr=parseSelectColumnsPart(sql);
        String prevselectpart=sqlArr[0];
        String selectcolumnspart=sqlArr[1];
        String tmp=sqlArr[2];
        while(true)
        {
            int idxLocal=tmp.toLowerCase().lastIndexOf(" group ");
            if(idxLocal<0)
            {
                throw new WabacusConfigLoadingException("加载报表"+getReportBean().getPath()+"失败，针对交叉统计报表，其SQL语句中必须包含group by子句");
            }
            frompart+=tmp.substring(0,idxLocal);
            tmp=tmp.substring(idxLocal+" group ".length()).trim();
            if(tmp.toLowerCase().startsWith("by "))
            {
                tmp=tmp.substring(2).trim();
                break;
            }
            frompart+=" group ";//如果不是的话，说明当前group不是group by关键字，所以要加入SQL中
        }
        int i=0;
        tmp=tmp.trim();
        for(;i<tmp.length();i++)
        {
            if(tmp.charAt(i)==')'
                    ||(tmp.charAt(i)==' '&&!tmp.substring(0,i).trim().endsWith(",")&&(tmp.length()==i+1||!tmp.substring(i+1).trim().startsWith(","))))
            {
                break;
            }else
            {
                groupbypart+=tmp.charAt(i);
            }
        }
        if(groupbypart.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+getReportBean().getPath()+"失败，查询动态列的数据集的SQL语句"+sql+"没有指定 group by");
        }
        lastpart=tmp.substring(i);
        Map<String,String> mResults=new HashMap<String,String>();
        mResults.put("prevselectpart",prevselectpart);
        mResults.put("selectcolumnspart",selectcolumnspart);
        mResults.put("frompart",frompart);
        mResults.put("groupbypart",groupbypart);
        mResults.put("lastpart",lastpart);
        return mResults;
    }
    
    private String[] parseSelectColumnsPart(String sql)
    {
        String prevselectpart="", selectcolumnspart="", lastselectpart="";
        int idx=sql.indexOf(this.dynamicColsPlaceholder);
        String tmp=sql.substring(0,idx).trim();
        lastselectpart=sql.substring(idx);
        while(true)
        {
            int idxLocal=tmp.toLowerCase().lastIndexOf("select ");
            if(idxLocal<0)
            {
                throw new WabacusConfigLoadingException("加载报表"+getReportBean().getPath()+"失败，配置的sql语句"+sql+"不合法");
            }
            prevselectpart=tmp.substring(0,idxLocal).trim();
            selectcolumnspart=tmp.substring(idxLocal+"select".length())+selectcolumnspart;
            if(prevselectpart.equals("")||prevselectpart.endsWith("("))
            {
                break;
            }
            tmp=prevselectpart;
            selectcolumnspart="select"+selectcolumnspart;
            //这个select是查询字段中某个字段名的一部分，继续向前查找select关键字
        }
        idx=lastselectpart.toLowerCase().indexOf(" from ");
        selectcolumnspart=selectcolumnspart+lastselectpart.substring(0,idx)+" ";
        lastselectpart=lastselectpart.substring(idx+" from ".length());
        return new String[] { prevselectpart, selectcolumnspart, lastselectpart };
    }
}

