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

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.ConfigLoadManager;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.util.Tools;

public class CompositeExpressionBeanForLoad
{
    private final static List<String> lstSupportCompareTypes=new ArrayList<String>();

    static
    {
        lstSupportCompareTypes.add("eq");
        lstSupportCompareTypes.add("neq");
        lstSupportCompareTypes.add("gt");
        lstSupportCompareTypes.add("lt");
        lstSupportCompareTypes.add("gte");
        lstSupportCompareTypes.add("lte");
        lstSupportCompareTypes.add("class");
        lstSupportCompareTypes.add("ref");
    }
    
    private List lstChildExpressions;

    private List<String> lstChildRelations;

    public void parsePreConditionExpressionsStart(String reportTypeKey,AbsEditableReportEditDataBean editBean,String expressions,String datasource)
    {
        if(expressions==null||expressions.trim().equals("")) return;
        expressions=expressions.trim();
        lstChildExpressions=new ArrayList();
        lstChildRelations=new ArrayList<String>();
        while(true)
        {
            if(expressions.equals("")) break;
            if(expressions.charAt(0)=='[')
            {
                int idx=expressions.indexOf(']');
                if(idx<0)
                {
                    throw new WabacusConfigLoadingException("报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition属性："
                            +expressions+"不合法，[]没有成对");
                }
                lstChildExpressions.add(createConcreteExpressionBean(reportTypeKey,editBean,expressions.substring(1,idx).trim(),datasource));
                expressions=parseExpressionRelation(editBean,expressions.substring(idx+1).trim());
                if(Tools.isEmpty(expressions)) break;
            }else if(expressions.charAt(0)=='(')
            {//是组合表达式
                int leftBracket=1, i=0;
                for(;i<expressions.length();i++)
                {
                    if(expressions.charAt(i)==')')
                    {
                        leftBracket--;
                        if(leftBracket==0) break;
                    }else if(expressions.charAt(i)=='(')
                    {
                        leftBracket++;
                    }
                }
                if(i==expressions.length())
                {
                    throw new WabacusConfigLoadingException("报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition属性："
                            +expressions+"不合法，()没有成对");
                }
                CompositeExpressionBeanForLoad childCompositeBean=new CompositeExpressionBeanForLoad();
                childCompositeBean.parsePreConditionExpressionsStart(reportTypeKey,editBean,expressions.substring(1,i),datasource);
                lstChildExpressions.add(childCompositeBean);
                expressions=parseExpressionRelation(editBean,expressions.substring(i+1).trim());
                if(Tools.isEmpty(expressions)) break;
            }else
            {
                throw new WabacusConfigLoadingException("报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition属性："+expressions
                        +"不合法");
            }
        }
    }

    private String parseExpressionRelation(AbsEditableReportEditDataBean editBean,String expressions)
    {
        if(expressions==null||expressions.trim().length()<3) return null;
        expressions=expressions.trim();
        if(expressions.toLowerCase().startsWith("and"))
        {
            lstChildRelations.add("and");
            expressions=expressions.substring(3).trim();
        }else if(expressions.toLowerCase().startsWith("or"))
        {
            lstChildRelations.add("or");
            expressions=expressions.substring(2).trim();
        }else
        {
            throw new WabacusConfigLoadingException("报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition属性："+expressions
                    +"不合法，相临表达式之间的关系只能是and或or");
        }
        return expressions.trim();
    }

    public AbsExpressionBean parsePreConditionExpressionsEnd()
    {
        if(lstChildExpressions==null||lstChildExpressions.size()==0) return null;
        if(lstChildExpressions.size()==1)
        {//只有一个子节点，则本层就删掉，直接用子节点
            return getExpressionBeanByExpessionObject(lstChildExpressions.get(0));
        }else
        {
            List<AbsExpressionBean> lstChildExpressionBeans=new ArrayList<AbsExpressionBean>();
            String logic=null;
            if(lstChildRelations.contains("and")&&lstChildRelations.contains("or"))
            {
                logic="or";
                List<AbsExpressionBean> lstAndChildBeans=new ArrayList<AbsExpressionBean>();
                addExpressionBeanToCurrentAndRelation(lstAndChildBeans,0);
                for(int i=0;i<this.lstChildRelations.size()-1;i++)
                {
                    if(lstChildRelations.get(i).equals("or"))
                    {
                        addPrevAndChildExpression(lstChildExpressionBeans,lstAndChildBeans);
                        lstAndChildBeans=new ArrayList<AbsExpressionBean>();//重新处理下一个and连接的子表达式
                    }
                    addExpressionBeanToCurrentAndRelation(lstAndChildBeans,i+1);
                }
                addPrevAndChildExpression(lstChildExpressionBeans,lstAndChildBeans);
            }else
            {
                logic=lstChildRelations.get(0);
                AbsExpressionBean expressBeanTmp;
                for(Object objTmp:this.lstChildExpressions)
                {
                    expressBeanTmp=getExpressionBeanByExpessionObject(objTmp);
                    if(expressBeanTmp!=null) lstChildExpressionBeans.add(expressBeanTmp);
                }
            }
            if(lstChildExpressionBeans.size()==0) return null;
            if(lstChildExpressionBeans.size()==1) return lstChildExpressionBeans.get(0);
            CompositeExpressionBean cebean=new CompositeExpressionBean();
            cebean.setLstChildExpressionBeans(lstChildExpressionBeans);
            cebean.setLogic(logic);
            return cebean;
        }
    }

    private AbsExpressionBean getExpressionBeanByExpessionObject(Object expressObj)
    {
        if(expressObj==null) return null;
        if(expressObj instanceof AbsConcreteExpressionBean) return (AbsConcreteExpressionBean)expressObj;
        return ((CompositeExpressionBeanForLoad)expressObj).parsePreConditionExpressionsEnd();
    }

    private void addExpressionBeanToCurrentAndRelation(List<AbsExpressionBean> lstAndChildBeans,int index)
    {
        AbsExpressionBean expressBeanTmp=getExpressionBeanByExpessionObject(lstChildExpressions.get(index));
        if(expressBeanTmp!=null) lstAndChildBeans.add(expressBeanTmp);
    }

    private void addPrevAndChildExpression(List<AbsExpressionBean> lstChildExpressionBeans,List<AbsExpressionBean> lstAndChildBeans)
    {
        if(lstAndChildBeans.size()==1)
        {
            lstChildExpressionBeans.add(lstAndChildBeans.get(0));
        }else if(lstAndChildBeans.size()>1)
        {
            CompositeExpressionBean cebean=new CompositeExpressionBean();
            cebean.setLstChildExpressionBeans(lstAndChildBeans);
            cebean.setLogic("and");
            lstChildExpressionBeans.add(cebean);
        }
    }

    private AbsConcreteExpressionBean createConcreteExpressionBean(String reportTypeKey,AbsEditableReportEditDataBean editBean,String expressions,String datasource)
    {
        if(Tools.isEmpty(expressions)) return null;
        expressions=expressions.trim();
        List<String> lstExpressParms=Tools.parseStringToList(expressions,",",true);
        if(lstExpressParms.size()==0) return null;
        String type=lstExpressParms.get(0).toLowerCase().trim();//比较类型
        if(!lstSupportCompareTypes.contains(type))
        {
            throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"+expressions
                    +"表达式失败，不支持比较类型"+type);
        }
        AbsConcreteExpressionBean resultExpressionBean;
        if(type.equals("ref"))
        {//引用<preconditions/>中定义的<precondition/>
            if(lstExpressParms.size()<2)
            {
                throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"
                        +expressions+"表达式失败，当使用ref引用条件表达式时，必须指定被引用的条件表达式名");
            }
            String refedname=lstExpressParms.get(1);
            if(Tools.isEmpty(refedname))
            {
                throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"
                        +expressions+"表达式失败，，当使用ref引用条件表达式时，必须指定被引用的条件表达式名");
            }
            if(editBean.getPreconditionExpressionBean(refedname)==null)
            {
                throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"
                        +expressions+"表达式失败，，当使用ref引用条件表达式时，必须指定被引用的条件表达式名"+refedname+"不存在，或者被定义在本<precondition/>的后面");
            }
            boolean bolValue=false;
            if(lstExpressParms.size()>2)
            {
                String expressionvalue=lstExpressParms.get(2);
                expressionvalue=expressionvalue==null?"false":expressionvalue.toLowerCase().trim();
                if(!expressionvalue.trim().equals("")&&!expressionvalue.equals("true")&&!expressionvalue.equals("false"))
                {
                    throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"+expressions
                            +"表达式失败，，当使用ref引用条件表达式时，与被引用的表达式的比较值只能是true或false，不能是其它值");
                }
                bolValue=expressionvalue.equals("true");
            }
            resultExpressionBean=new RefConcreteExpressionBean();
            ((RefConcreteExpressionBean)resultExpressionBean).setComparedValue(bolValue);
            ((RefConcreteExpressionBean)resultExpressionBean).setRefedExpressionBean(editBean.getPreconditionExpressionBean(refedname));
        }else if(type.equals("class"))
        {
            if(lstExpressParms.size()<2)
            {
                throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"
                        +expressions+"表达式失败，当采用自定义JAVA类做为条件表达式时，必须指定JAVA类全限定类名");
            }
            String javaclass=lstExpressParms.get(1);
            if(Tools.isEmpty(javaclass))
            {
                throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"
                        +expressions+"表达式失败，当采用自定义JAVA类做为条件表达式时，指定JAVA类全限定类名不能为空");
            }
            Object obj=null;
            try
            {
                obj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(javaclass.trim()).newInstance();
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"
                        +expressions+"表达式失败，当采用自定义JAVA类做为条件表达式时，指定JAVA类全限定类"+javaclass+"无法实例化",e);
            }
            if(!(obj instanceof AbsConcreteExpressionBean))
            {
                throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"
                        +expressions+"表达式失败，当采用自定义JAVA类做为条件表达式时，指定JAVA类全限定类"+javaclass+"没有继承框架的父类："+AbsConcreteExpressionBean.class.getName());
            }
            resultExpressionBean=(AbsConcreteExpressionBean)obj;
            if(lstExpressParms.size()>2)
            {
                List<String> lstParams=new ArrayList<String>();
                for(int i=2;i<lstExpressParms.size();i++)
                {
                    lstParams.add(antiReplaceSpecialWords(lstExpressParms.get(i)));
                }
                resultExpressionBean.setLstParams(lstParams);
            }
        }else
        {
            if(lstExpressParms.size()<3)
            {
                throw new WabacusConfigLoadingException("解析报表"+editBean.getOwner().getReportBean().getPath()+"配置的precondition中的"
                        +expressions+"表达式失败，采用内置的比较条件时，必须指定比较参数和被比较参数");
            }
            resultExpressionBean=new DefaultConcreteExpressionBean();
            List<String> lstParams=new ArrayList<String>();
            for(int i=0;i<lstExpressParms.size();i++)
            {
                lstParams.add(antiReplaceSpecialWords(lstExpressParms.get(i)));
            }
            resultExpressionBean.setLstParams(lstParams);
        }
        resultExpressionBean.setOwnerEditbean(editBean);
        resultExpressionBean.setReportTypeKey(reportTypeKey);
        resultExpressionBean.setDatasource(datasource);
        resultExpressionBean.parseParams();
        return resultExpressionBean;
    }

    private String antiReplaceSpecialWords(String str)
    {
        if(str==null) return null;
        str=Tools.replaceAll(str,"$_LEFTBRACKET_$","(");
        str=Tools.replaceAll(str,"$_RIGHTBRACKET_$",")");
        str=Tools.replaceAll(str,"$_LEFTBRACKET2_$","[");
        str=Tools.replaceAll(str,"$_RIGHTBRACKET2_$","]");
        str=Tools.replaceAll(str,"$_LEFTBRACKET3_$","{");
        str=Tools.replaceAll(str,"$_RIGHTBRACKET3_$","}");
        str=Tools.replaceAll(str,"$_COMMA_$",",");
        return str;
    }
}
