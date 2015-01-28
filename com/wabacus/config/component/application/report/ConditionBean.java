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
package com.wabacus.config.component.application.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.condition.ConditionExpressionBean;
import com.wabacus.config.component.application.report.condition.ConditionSelectItemBean;
import com.wabacus.config.component.application.report.condition.ConditionSelectorBean;
import com.wabacus.config.component.application.report.condition.ConditionValueSelectItemBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.CustomizedBox;
import com.wabacus.system.inputbox.IInputBoxOwnerBean;
import com.wabacus.system.inputbox.validate.ServerValidateBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class ConditionBean extends AbsConfigBean implements IInputBoxOwnerBean
{
    private static Log log=LogFactory.getLog(ConditionBean.class);
    
    public final static String SELECTTYPE_INNERLOGIC="innerlogic";
    
    public final static String SELECTORTYPE_COLUMNS="columns";
    
    public final static String SELECTORTYPE_VALUES="values";

    public final static String LABELPOSITION_INNER="inner";
    
    public final static String LABELPOSITION_LEFT="left";//查询条件label显示在输入框左侧
    
    public final static String LABELPOSITION_RIGHT="right";
    
    private IDataType datatypeObj;

    private AbsInputBox inputbox;

    private String name="";

    private String label;
    
    private Map<String,String> mDynLableParts;

    private String labelposition=LABELPOSITION_INNER;//显示查询条件label的位置类型

    private String defaultvalue=null;
    
    private boolean keepkeywords;
    
    private String logic=" and ";

    private String tip="";

    private boolean hidden;//是否在前台显示

    private boolean constant;

    private boolean br;

    private int left=3;
    
    private int right=0;//距离右边元素的位置

    //    private String relatetab="";//当在同一页面横向以tab形式显示多个报表时，如果某个条件输入框的值要在点击其它tab切换到其它报表时

    private String splitlike="0";
    
    private String source;

    private ConditionExpressionBean conditionExpression;//此查询条件只有一个表达式，配置查询条件表达式
    
    private int iterator;//当前查询条件重复显示的个数，只有有多个<value/>或<columns/>时才有意义。
    
    private String innerlogic;//加载时暂存用户在<condition/>中配置的innerlogic属性，当不需提供逻辑关系的选择，且不需在前面显示逻辑关系的label时，可以直接在<condition/>的innerlogic属性中配置
    
    private ConditionSelectorBean cinnerlogicbean;//存放逻辑关系选择信息，如果要提供多个逻辑关系进行选择，或者需要在输入框前面显示逻辑关系label，则可以通过配置<innerlogic/>子标签进行配置
    
    private ConditionSelectorBean ccolumnsbean;
    
    private ConditionSelectorBean cvaluesbean;
    
    private List<String> lstChildDisplayOrder;//当前查询条件如果要显示这几个子元素时，它们的显示顺序，由它们配置在<condition/>中的次序决定
    
    private List<String[]> lstBelongto;
    
    private ServerValidateBean serverValidateBean;
    
    private String onsetvalueMethod;
    
    private String ongetvalueMethod;//获取此条件值时的客户端回调函数
    
    public ConditionBean(AbsConfigBean parent)
    {
        super(parent);
    }

    public String getDefaultvalue()
    {
        return this.defaultvalue;
    }

    public void setDefaultvalue(String defaultvalue)
    {
        this.defaultvalue=defaultvalue;
    }
    
    public void setName(String name)
    {
        this.name=name;
    }

    public void setServerValidateBean(ServerValidateBean svbean)
    {
        this.serverValidateBean=svbean;
    }

    public void setLabel(String label)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.getPageBean(),label);
        this.label=(String)objArr[0];
        this.mDynLableParts=(Map<String,String>)objArr[1];
    }

    public String getLabel()
    {
        return this.label;
    }
    
    public Map<String, String> getMDynLableParts()
    {
        return this.mDynLableParts;
    }
    
    public String getLabel(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.label,this.mDynLableParts,"");
    }
    
    public String getTip()
    {
        return tip;
    }

    public void setTip(String tip)
    {
        this.tip=tip;
    }

    public String getName()
    {
        return this.name;
    }

    public String getLabelposition()
    {
        return labelposition;
    }

    public void setLabelposition(String labelposition)
    {
        this.labelposition=labelposition;
    }

    public String getLogic()
    {
        return this.logic;
    }

    public IDataType getDatatypeObj()
    {
        return datatypeObj;
    }

    public void setDatatypeObj(IDataType datatypeObj)
    {
        this.datatypeObj=datatypeObj;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden=hidden;
    }

    public boolean isKeepkeywords()
    {
        return keepkeywords;
    }

    public void setKeepkeywords(boolean keepkeywords)
    {
        this.keepkeywords=keepkeywords;
    }

    public AbsInputBox getInputbox()
    {
        return inputbox;
    }

    public void setInputbox(AbsInputBox inputbox)
    {
        this.inputbox=inputbox;
    }

    public boolean isBr()
    {
        return br;
    }

    public void setBr(boolean br)
    {
        this.br=br;
    }

    public boolean isConstant()
    {
        return constant;
    }

    public void setConstant(boolean constant)
    {
        this.constant=constant;
    }

    public String getSplitlike()
    {
        return splitlike;
    }

    public void setSplitlike(String splitlike)
    {
        this.splitlike=splitlike;
    }

    public String getOnsetvalueMethod()
    {
        return onsetvalueMethod;
    }

    public void setOnsetvalueMethod(String onsetvalueMethod)
    {
        this.onsetvalueMethod=onsetvalueMethod;
    }

    public String getOngetvalueMethod()
    {
        return ongetvalueMethod;
    }

    public void setOngetvalueMethod(String ongetvalueMethod)
    {
        this.ongetvalueMethod=ongetvalueMethod;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        if(source!=null&&!source.trim().equals(""))
        {
            String reportpath="";
            if(this.getReportBean()!=null) reportpath=this.getReportBean().getPath();
            if(Tools.isDefineKey("request",source))
            {
                if(Tools.getRealKeyByDefine("request",source).trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportpath+"的name为"+this.name+"的条件失败，source配置值："+source+"指定的从request取数据的key为空");
                }
            }else if(Tools.isDefineKey("session",source))
            {
                if(Tools.getRealKeyByDefine("session",source).trim().equals(""))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportpath+"的name为"+this.name+"的条件失败，source配置值："+source+"指定的从session取数据的key为空");
                }
            }else
            {
                throw new WabacusConfigLoadingException("加载报表"+reportpath+"的name为"+this.name+"的条件失败，source属性值"+source+"不是request{...}或session{...}格式");
            }
            this.source=source;
            this.hidden=true;
            this.constant=false;
        }else
        {
            this.source=null;
        }
    }

    public String getOwnerId()
    {
        return name;
    }

    public String getInputBoxId()
    {
        return this.getReportBean().getGuid()+"_wxcondition_"+name.trim();
    }

    public boolean isConditionWithInputbox()
    {
        if(this.isHidden()||this.isConstant()) return false;
        if(Tools.isDefineKey("request",this.source)||Tools.isDefineKey("session",this.source)) return false;
        return true;
    }
    
    public boolean isConditionValueFromUrl()
    {
        if(this.isConstant()) return false;
        if(Tools.isDefineKey("request",this.source)||Tools.isDefineKey("session",this.source)) return false;
        return true;
    }
    
    public boolean isConditionValueFromSession()
    {
        if(this.isConstant()) return false;
        return Tools.isDefineKey("session",this.source);
    }
    
    public int getLeft()
    {
        return left;
    }

    public void setLeft(int left)
    {
        this.left=left;
    }

    public int getRight()
    {
        return right;
    }

    public void setRight(int right)
    {
        this.right=right;
    }

    public ConditionExpressionBean getConditionExpression()
    {
        return conditionExpression;
    }

    public void setConditionExpression(ConditionExpressionBean conditionExpression)
    {
        this.conditionExpression=conditionExpression;
    }

    public int getIterator()
    {
        return iterator;
    }

    public void setIterator(int iterator)
    {
        this.iterator=iterator;
    }

    public String getInnerlogic()
    {
        return innerlogic;
    }

    public void setInnerlogic(String innerlogic)
    {
        if(innerlogic==null||innerlogic.trim().equals(""))
        {
            this.innerlogic=null;
        }else
        {
            if(!innerlogic.toLowerCase().trim().equals("and")&&!innerlogic.toLowerCase().trim().equals("or"))
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的查询条件"+this.getName()+"失败，配置的innerlogic必须为and或or");
            }
            this.innerlogic=innerlogic.trim();
        }
    }

    public ConditionSelectorBean getCinnerlogicbean()
    {
        return cinnerlogicbean;
    }

    public void setCinnerlogicbean(ConditionSelectorBean cinnerlogicbean)
    {
        this.cinnerlogicbean=cinnerlogicbean;
    }
    
    public ConditionSelectorBean getCcolumnsbean()
    {
        return ccolumnsbean;
    }

    public void setCcolumnsbean(ConditionSelectorBean ccolumnsbean)
    {
        this.ccolumnsbean=ccolumnsbean;
    }
    
    public ConditionSelectorBean getCvaluesbean()
    {
        return cvaluesbean;
    }

    public void setCvaluesbean(ConditionSelectorBean cvaluesbean)
    {
        this.cvaluesbean=cvaluesbean;
    }

    public List<String> getLstChildDisplayOrder()
    {
        return lstChildDisplayOrder;
    }

    public void setLstChildDisplayOrder(List<String> lstChildDisplayOrder)
    {
        this.lstChildDisplayOrder=lstChildDisplayOrder;
    }

    public void setBelongto(String belongto)
    {
        if(belongto!=null)
        {
            belongto=belongto.trim();
            if(belongto.equals("")||belongto.equals("*.*"))
            {
                this.lstBelongto=null;
            }else
            {
                this.lstBelongto=new ArrayList<String[]>();
                List<String> lstTmp=Tools.parseStringToList(belongto,";",false);
                String[] strArr;
                for(String strTmp:lstTmp)
                {
                    if(strTmp.trim().equals("")) continue;
                    int idx=strTmp.indexOf(".");
                    if(idx<=0)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                                +"的查询条件失败，其配置的belongto没有用.符号分隔所属<dataset/>和<value/>的ID");
                    }
                    strArr=new String[] { strTmp.substring(0,idx).trim(), strTmp.substring(idx+1).trim() };
                    if(strArr[0].equals("*")) strArr[0]=null;
                    if(strArr[1].equals("*")) strArr[1]=null;
                    this.lstBelongto.add(strArr);
                }
            }
        }
    }

    public boolean isBelongTo(ReportDataSetValueBean svbean)
    {
        if(this.lstBelongto==null||this.lstBelongto.size()==0) return true;
        String datasetid=((ReportDataSetBean)svbean.getParent()).getId();
        for(String[] belongtoArrTmp:this.lstBelongto)
        {
            if((belongtoArrTmp[0]==null||datasetid.equals(belongtoArrTmp[0]))&&(belongtoArrTmp[1]==null||svbean.getId().equals(belongtoArrTmp[1])))
                return true;
        }
        return false;
    }
    
    public boolean isExistConditionExpression(boolean inherit)
    {
        if(this.conditionExpression!=null&&this.conditionExpression.getValue()!=null&&!this.conditionExpression.getValue().trim().equals(""))
            return true;
        if(!inherit) return false;
        if(this.cvaluesbean==null||this.cvaluesbean.isEmpty()) return false;
        ConditionValueSelectItemBean cvbTmp=(ConditionValueSelectItemBean)this.cvaluesbean.getLstSelectItemBeans().get(0);
        if(cvbTmp.getConditionExpression()!=null&&cvbTmp.getConditionExpression().getValue()!=null
                &&!cvbTmp.getConditionExpression().getValue().trim().equals(""))
        {
            return true;
        }
        if(cvbTmp.getLstColumnsBean()==null||cvbTmp.getLstColumnsBean().size()==0) return false;
        ConditionSelectItemBean ccbeanTmp=cvbTmp.getLstColumnsBean().get(0);
        if(ccbeanTmp.getConditionExpression()!=null&&ccbeanTmp.getConditionExpression().getValue()!=null
                &&!ccbeanTmp.getConditionExpression().getValue().trim().equals(""))
        {
            return true;
        }
        return false;
    }

    public void initConditionValueByInitUrlMethod(ReportRequest rrequest)
    {
        if(!this.isConditionValueFromUrl()) return;
        if(this.conditionExpression!=null)
        {
            rrequest.addParamToUrl(name,"rrequest{"+name+"}",false);
        }else
        {
            String colselectedInputboxidTmp;
            String valueselectedInputboxidTmp;
            String innerlogicSelectedInputboxidTmp;
            if(this.iterator>1)
            {//此查询条件要显示多套条件框
                for(int i=0;i<iterator;i++)
                {
                    if(this.ccolumnsbean!=null&&!this.ccolumnsbean.isEmpty())
                    {
                        colselectedInputboxidTmp=this.ccolumnsbean.getSelectedInputboxId(i);
                        rrequest.addParamToUrl(colselectedInputboxidTmp,"rrequest{"+colselectedInputboxidTmp+"}",true);
                    }
                    if(this.cinnerlogicbean!=null&&!this.cinnerlogicbean.isEmpty()&&this.cinnerlogicbean.getLstSelectItemBeans().size()>1)
                    {//如果配置了<innerlogic/>且配置了多个<logic/>子标签
                        innerlogicSelectedInputboxidTmp=this.cinnerlogicbean.getSelectedInputboxId(i);
                        rrequest.addParamToUrl(innerlogicSelectedInputboxidTmp,"rrequest{"+innerlogicSelectedInputboxidTmp+"}",true);
                    }
                    valueselectedInputboxidTmp=this.cvaluesbean.getSelectedInputboxId(i);
                    rrequest.addParamToUrl(valueselectedInputboxidTmp,"rrequest{"+valueselectedInputboxidTmp+"}",true);
                    rrequest.addParamToUrl(name+"_"+i,"rrequest{"+name+"_"+i+"}",false);
                }
            }else
            {
                if(this.ccolumnsbean!=null&&!this.ccolumnsbean.isEmpty())
                {
                    colselectedInputboxidTmp=this.ccolumnsbean.getSelectedInputboxId(-1);
                    rrequest.addParamToUrl(colselectedInputboxidTmp,"rrequest{"+colselectedInputboxidTmp+"}",true);
                }
                if(this.cvaluesbean!=null&&!this.cvaluesbean.isEmpty())
                {
                    valueselectedInputboxidTmp=this.cvaluesbean.getSelectedInputboxId(-1);
                    rrequest.addParamToUrl(valueselectedInputboxidTmp,"rrequest{"+valueselectedInputboxidTmp+"}",true);
                }
                rrequest.addParamToUrl(name,"rrequest{"+name+"}",false);
            }
        }
    }
    
    public void initConditionValueByInitMethod(ReportRequest rrequest,Map<String,String> mConditionValues)
    {
        if(!this.isConditionValueFromUrl())
        {
            mConditionValues.put(this.name,getConditionValue(rrequest,-1));
        }else if(this.conditionExpression!=null||this.iterator<=1)
        {
            mConditionValues.put(this.name,getConditionValue(rrequest,name));
        }else
        {
            for(int i=0;i<iterator;i++)
            {
                mConditionValues.put(name+"_"+i,getConditionValue(rrequest,name+"_"+i));
            }
        }
    }
    
    private String getConditionValue(ReportRequest rrequest,String conditionname)
    {
        String conditionvalue=rrequest.getStringAttribute(conditionname,"");
        if((LABELPOSITION_INNER.equals(this.labelposition)&&conditionvalue.equals(this.getLabel(rrequest)))
                ||conditionvalue.equals("(ALL_DATA)"))
        {
            conditionvalue="";
        }
        if(conditionvalue.equals("")&&this.defaultvalue!=null)
        {//没有条件值，则取默认值，如果配置了的话
            conditionvalue=ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,this.defaultvalue);
        }
        rrequest.getAttributes().put(conditionname,conditionvalue);
        rrequest.addParamToUrl(conditionname,conditionvalue,true);
        return conditionvalue;
    }
    
    public void validateConditionValue(ReportRequest rrequest,Map<String,String> mConditionValues)
    {
        if(this.serverValidateBean==null) return;
        if(!this.isConditionValueFromUrl()||this.conditionExpression!=null||this.iterator<=1)
        {
            this.serverValidateBean.validate(rrequest,mConditionValues.get(this.name),mConditionValues,new ArrayList<String>(),null);
        }else
        {//此查询条件要显示多套条件框
            for(int i=0;i<iterator;i++)
            {
                this.serverValidateBean.validate(rrequest,mConditionValues.get(this.name+"_"+i),mConditionValues,new ArrayList<String>(),null);
            }
        }
    }
    
    public String getConditionValueForSP(ReportRequest rrequest)
    {
        if(this.isConstant()) return conditionExpression.getValue();
        StringBuffer conditionValueBuf=new StringBuffer();
        if(this.iterator<2)
        {
            conditionValueBuf.append(getRuntimeConditionExpressionForSP(rrequest,-1));
            String conditionvalue=getDynamicConditionvalueForSql(rrequest,-1);
            if(conditionValueBuf.length()>0)
            {
                conditionValueBuf.append("value=").append(conditionvalue);
            }else
            {
                conditionValueBuf.append(conditionvalue);
            }
        }else
        {
            String conditionvalueTmp;
            for(int i=0;i<this.iterator;i++)
            {
                conditionvalueTmp=getDynamicConditionvalueForSql(rrequest,i);
                if(conditionvalueTmp.equals("")) continue;
                conditionValueBuf.append(getRuntimeConditionExpressionForSP(rrequest,i));
                String innerlogicTmp=getInnerLogicValue(rrequest,i);
                conditionValueBuf.append("innerlogic["+i+"]="+innerlogicTmp).append(";");
                conditionValueBuf.append("value["+i+"]="+conditionvalueTmp).append(";");
            }
        }
        return conditionValueBuf.toString();
    }
    
    private String getRuntimeConditionExpressionForSP(ReportRequest rrequest,int index)
    {
        StringBuffer conditionValueBuf=new StringBuffer();
        if(this.cvaluesbean!=null)
        {
            String valueid=rrequest.getStringAttribute(this.cvaluesbean.getSelectedInputboxId(index),"");//选中的表达式ID
            ConditionSelectItemBean cvbean=this.cvaluesbean.getSelectItemBeanById(valueid);
            if(cvbean==null)
            {
                if(valueid.equals(""))
                {
                    throw new WabacusRuntimeException("没有为报表"+this.getReportBean().getPath()+"的查询条件"+name+"配置<value/>子标签");
                }else
                {
                    throw new WabacusRuntimeException("没有为报表"+this.getReportBean().getPath()+"的查询条件"+name+"配置id为"+valueid+"的<value/>子标签");
                }
            }
            conditionValueBuf.append("expressionid");
            if(index>=0) conditionValueBuf.append("["+index+"]");
            conditionValueBuf.append("=").append(cvbean.getId()).append(";");
        }
        if(this.ccolumnsbean!=null)
        {
            String columnid=rrequest.getStringAttribute(this.ccolumnsbean.getSelectedInputboxId(index),"");
            ConditionSelectItemBean ccbean=this.ccolumnsbean.getSelectItemBeanById(columnid);
            if(ccbean==null)
            {
                if(columnid.equals(""))
                {
                    throw new WabacusRuntimeException("没有为报表"+this.getReportBean().getPath()+"的查询条件"+name+"配置<column/>子标签");
                }else
                {
                    throw new WabacusRuntimeException("没有为报表"+this.getReportBean().getPath()+"的查询条件"+name+"配置id为"+columnid+"的<column/>子标签");
                }
            }
            conditionValueBuf.append("columnid");
            if(index>=0) conditionValueBuf.append("["+index+"]");
            conditionValueBuf.append("=").append(ccbean.getId()).append(";");
        }
        return conditionValueBuf.toString();
    }
    
    public String getConditionExpressionAndParams(ReportRequest rrequest,List<String> lstConditions,List<IDataType> lstConditionsTypes)
    {
        if(!this.isExistConditionExpression(true)) return null;//如果当前查询条件没有在<condition/>中的任意层级子标签中配置条件表达式，则说明它可能是直接通过#name#的形式在sql语句中指定条件，所以不在这里为它构造条件表达式
        if(this.isConstant()) return conditionExpression.getValue();
        if(this.iterator<2)
        {
            String conditionvalue=getDynamicConditionvalueForSql(rrequest,-1);
            if(conditionvalue.equals("")) return "";
            ConditionExpressionBean cexpressionbean=getConditionRuntimeExpressionBean(rrequest,-1);
            return cexpressionbean.getRuntimeConditionExpressionValue(this,conditionvalue,lstConditions,lstConditionsTypes);
        }else
        {
            String conditionvalueTmp;
            StringBuffer andExpressionBuf=new StringBuffer();
            StringBuffer orExpressionBuf=new StringBuffer();//所有or逻辑关系的条件表达式
            List<String> lstAndConditions=null;
            List<IDataType> lstAndConditionsType=null;new ArrayList<IDataType>();
            List<String> lstOrConditions=null;new ArrayList<String>();
            List<IDataType> lstOrConditionsType=null;
            if(lstConditions!=null&&lstConditionsTypes!=null)
            {
                lstAndConditions=new ArrayList<String>();
                lstAndConditionsType=new ArrayList<IDataType>();
                lstOrConditions=new ArrayList<String>();
                lstOrConditionsType=new ArrayList<IDataType>();
            }
            for(int i=0;i<this.iterator;i++)
            {
                conditionvalueTmp=getDynamicConditionvalueForSql(rrequest,i);
                if(conditionvalueTmp.equals("")) continue;
                String innerlogicTmp=getInnerLogicValue(rrequest,i);
                ConditionExpressionBean cexpressionbean=getConditionRuntimeExpressionBean(rrequest,i);
                if(!innerlogicTmp.equalsIgnoreCase("or"))
                {
                    andExpressionBuf.append(cexpressionbean.getRuntimeConditionExpressionValue(this,conditionvalueTmp,lstAndConditions,
                            lstAndConditionsType));
                    andExpressionBuf.append(" and ");
                }else
                {
                    orExpressionBuf.append(cexpressionbean.getRuntimeConditionExpressionValue(this,conditionvalueTmp,lstOrConditions,
                            lstOrConditionsType));
                    orExpressionBuf.append(" or ");
                }
            }
            if(andExpressionBuf.toString().endsWith(" and "))
            {
                andExpressionBuf.delete(andExpressionBuf.length()-" and ".length(),andExpressionBuf.length());
            }
            if(orExpressionBuf.toString().endsWith(" or "))
            {
                orExpressionBuf.delete(orExpressionBuf.length()-" or ".length(),orExpressionBuf.length());
            }
            if(andExpressionBuf.length()==0&&orExpressionBuf.length()==0) return "";
            if(lstConditions!=null&&lstConditionsTypes!=null)
            {
                //先and
                if(lstAndConditions.size()>0) lstConditions.addAll(lstAndConditions);
                if(lstAndConditionsType.size()>0) lstConditionsTypes.addAll(lstAndConditionsType);
                if(lstOrConditions.size()>0) lstConditions.addAll(lstOrConditions);
                if(lstOrConditionsType.size()>0) lstConditionsTypes.addAll(lstOrConditionsType);
            }
            String conditionexpression="";
            if(!andExpressionBuf.toString().trim().equals(""))
            {
                conditionexpression="("+andExpressionBuf.toString()+")";
            }
            if(!orExpressionBuf.toString().trim().equals(""))
            {
                if(conditionexpression.equals(""))
                {
                    conditionexpression="("+orExpressionBuf.toString()+")";
                }else
                {
                    conditionexpression=conditionexpression+" or ("+orExpressionBuf.toString()+")";
                }
            }
            if(!conditionexpression.trim().equals("")) conditionexpression="("+conditionexpression+")";
            return conditionexpression;
        }
    }

    private String getInnerLogicValue(ReportRequest rrequest,int iteratorindex)
    {
        if((this.innerlogic==null||this.innerlogic.trim().equals(""))&&(this.cinnerlogicbean==null||this.cinnerlogicbean.isEmpty())) return "and";
        if(this.innerlogic!=null&&!this.innerlogic.trim().equals("")) return this.innerlogic;//配置了<condition/>的innerlogic属性
        if(this.cinnerlogicbean.getLstSelectItemBeans().size()==1) return this.cinnerlogicbean.getLstSelectItemBeans().get(0).getId();//在<innerlogic/>中只配置了一个<logic/>
        String innerlogicTmp=rrequest.getStringAttribute(this.cinnerlogicbean.getSelectedInputboxId(iteratorindex),"");
        if(innerlogicTmp.equals("")) return this.cinnerlogicbean.getLstSelectItemBeans().get(0).getId();//如果用户还没有选择逻辑关系，则用配置的第一个
        return innerlogicTmp;
    }
    
    public String getDynamicConditionvalueForSql(ReportRequest rrequest,int iteratorindex)
    {
        String conditionvalue=getConditionValue(rrequest,iteratorindex);
        if(conditionvalue==null||conditionvalue.trim().equals("")) return "";
        if((this.datatypeObj instanceof VarcharType)&&!this.splitlike.equals("0"))
        {
            conditionvalue=ReportAssistant.getInstance().formatCondition(conditionvalue.trim(),this.splitlike);
        }
        return conditionvalue;
    }
    
    public String getConditionValue(ReportRequest rrequest,int iteratorindex)
    {
        if(this.isConstant())
        {
            if(this.getConditionExpression()==null) return "";
            return this.getConditionExpression().getValue();
        }
        String conditionvalue;
        if(Tools.isDefineKey("request",this.source)||Tools.isDefineKey("session",this.source))
        {
            conditionvalue=WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,this.source,"");
            if(conditionvalue.equals("")&&this.defaultvalue!=null)
            {
                conditionvalue=ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,this.defaultvalue);
            }
        }else
        {
            if(this.isHidden())
            {
                conditionvalue=rrequest.getStringAttribute(name,"");
            }else
            {//这里不用考虑defaultvalue，在初始化时即处理好了
                String conditionname=name;
                if(iteratorindex>=0) conditionname=conditionname+"_"+iteratorindex;
                conditionvalue=rrequest.getStringAttribute(conditionname,"");
            }
        }
        return conditionvalue;
    }
    
    private ConditionExpressionBean getConditionRuntimeExpressionBean(ReportRequest rrequest,int index)
    {
        if(this.conditionExpression!=null) return conditionExpression;
        String valueid=rrequest.getStringAttribute(this.cvaluesbean.getSelectedInputboxId(index),"");
        ConditionSelectItemBean cvbean=this.cvaluesbean.getSelectItemBeanById(valueid);
        if(cvbean==null)
        {
            if(valueid.equals(""))
            {
                throw new WabacusRuntimeException("没有为报表"+this.getReportBean().getPath()+"的查询条件"+name+"配置<value/>子标签");
            }else
            {
                throw new WabacusRuntimeException("没有为报表"+this.getReportBean().getPath()+"的查询条件"+name+"配置id为"+valueid+"的<value/>子标签");
            }
        }
        if(cvbean.getConditionExpression()!=null) return cvbean.getConditionExpression();//在<value/>中配置的是条件表达式
        //在<value/>中配置的是<column/>
        String columnid=rrequest.getStringAttribute(this.ccolumnsbean.getSelectedInputboxId(index),"");
        ConditionSelectItemBean ccbean=((ConditionValueSelectItemBean)cvbean).getColumnBeanById(columnid);
        if(ccbean==null)
        {
            if(columnid.equals(""))
            {
                throw new WabacusRuntimeException("没有为报表"+this.getReportBean().getPath()+"的查询条件"+name+"配置<column/>子标签");
            }else
            {
                throw new WabacusRuntimeException("没有为报表"+this.getReportBean().getPath()+"的查询条件"+name+"配置id为"+columnid+"的<column/>子标签");
            }
        }
        return ccbean.getConditionExpression();
    }
    
    public String getDisplayString(ReportRequest rrequest,String dynstyleproperty,int iteratorindex)
    {
        if(!this.isConditionWithInputbox()) return "";
        ReportBean rbean=this.getReportBean();
        if(!rrequest.checkPermission(rbean.getId(),Consts.SEARCH_PART,name,Consts.PERMISSION_TYPE_DISPLAY)) return "";
        if(this.iterator<=1&&iteratorindex>=0)
        {
            throw new WabacusRuntimeException("报表"+rbean.getPath()+"的查询条件"+this.name+"的iterator属性为"+this.iterator
                    +"，显示时不能指定iteratorindex为大于等于0的数，即只能为它显示一套输入框");
        }else if(this.iterator>1&&(iteratorindex<0||iteratorindex>this.iterator))
        {
            throw new WabacusRuntimeException("报表"+rbean.getPath()+"的查询条件"+this.name+"的iterator属性为"+this.iterator
                    +"，即显示多套输入框，因此必须指定当前显示的下标iteratorindex，且不能超过iterator配置值："+this.iterator);
        }
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("<font id=\"font_").append(rbean.getGuid()+"_conditions\"");
        resultBuf.append(" name=\"font_").append(rbean.getGuid()+"_conditions\"");
        resultBuf.append(" value_name=\"").append(this.name).append("\"");
        resultBuf.append(" inputboxid=\""+this.getInputBoxId()+"\"");//后面根据此ID取相应的<span/>，如果取到了就是可编辑列，没有取到就是不可编辑列
        resultBuf.append(" value=\""+Tools.htmlEncode(getConditionValue(rrequest,iteratorindex))+"\"");
        if(!(this.inputbox instanceof CustomizedBox))
        {
            if(this.cinnerlogicbean!=null&&!this.cinnerlogicbean.isEmpty()&&this.cinnerlogicbean.getLstSelectItemBeans().size()>1)
            {
                resultBuf.append(" innerlogicid=\"").append(this.cinnerlogicbean.getSelectedInputboxId(iteratorindex)).append("\"");
                resultBuf.append(" innerlogicinputboxtype=\"").append(cinnerlogicbean.getInputbox()==null?"":cinnerlogicbean.getInputbox()).append("\"");//逻辑关系选择框类型
            }
            if(iteratorindex>=0)
            {
                resultBuf.append(" iteratorindex=\"").append(iteratorindex).append("\"");
            }
            if(this.ccolumnsbean!=null&&!this.ccolumnsbean.isEmpty()&&this.ccolumnsbean.getLstSelectItemBeans().size()>1)
            {
                resultBuf.append(" columnid=\"").append(this.ccolumnsbean.getSelectedInputboxId(iteratorindex)).append("\"");
                resultBuf.append(" columninputboxtype=\"").append(ccolumnsbean.getInputbox()==null?"":ccolumnsbean.getInputbox()).append("\"");
            }
            if(this.cvaluesbean!=null&&!this.cvaluesbean.isEmpty()&&this.cvaluesbean.getLstSelectItemBeans().size()>1)
            {//有多个条件表达式
                resultBuf.append(" valueid=\"").append(this.cvaluesbean.getSelectedInputboxId(iteratorindex)).append("\"");
                resultBuf.append(" valueinputboxtype=\"").append(cvaluesbean.getInputbox()==null?"":cvaluesbean.getInputbox()).append("\"");
            }
            resultBuf.append(">");
            resultBuf.append(showConditionAllTypeBoxes(rrequest,dynstyleproperty,iteratorindex));
            resultBuf.append("</font>");
        }else
        {
            if(this.iterator>1)
            {
                throw new WabacusRuntimeException("显示报表"+this.getReportBean().getPath()+"的查询条件"+this.name+"失败，此查询条件的iterator大于1，不能为它提供条件输入框");
            }
            resultBuf.append(">");
        }
        return resultBuf.toString();
    }
    
    private String showConditionAllTypeBoxes(ReportRequest rrequest,String dynstyleproperty,int iteratorindex)
    {
        StringBuilder resultBuf=new StringBuilder();
        boolean isReadonlyPermission=rrequest.checkPermission(this.getReportBean().getId(),Consts.SEARCH_PART,this.name,Consts.PERMISSION_TYPE_READONLY);
        for(String childtype:this.lstChildDisplayOrder)
        {//按照配置顺序依次显示本查询条件的各类型输入框
            if(childtype.equals("values")&&this.cvaluesbean!=null)
            {
                resultBuf.append(this.cvaluesbean.showSelectedInputbox(rrequest,isReadonlyPermission,iteratorindex));
            }else if(childtype.equals("columns")&&this.ccolumnsbean!=null)
            {
                resultBuf.append(this.ccolumnsbean.showSelectedInputbox(rrequest,isReadonlyPermission,iteratorindex));
            }else if(childtype.equals("inputbox"))
            {
                resultBuf.append(showConditionInputbox(rrequest,isReadonlyPermission,dynstyleproperty,iteratorindex));
            }else if(childtype.equals("innerlogic")&&this.cinnerlogicbean!=null&&!this.cinnerlogicbean.isEmpty())
            {//只有由框架显示输入框，才有可能配置iterator>1，才会用到<innerlogic/>，所以这个时候才需显示这种输入框
                if(this.cinnerlogicbean.getLstSelectItemBeans().size()==1)
                {//如果通过<innerlogic/>只配置了一个<logic/>，则只显示它的label
                    String reallabel=cinnerlogicbean.getLstSelectItemBeans().get(0).getLabel();
                    if(reallabel!=null&&!reallabel.trim().equals(""))
                    {//显示标题
                        resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(cinnerlogicbean.getLeft()));
                        reallabel=rrequest.getI18NStringValue(reallabel);
                        resultBuf.append(reallabel);
                        resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(cinnerlogicbean.getRight()));
                    }
                }else
                {
                    resultBuf.append(this.cinnerlogicbean.showSelectedInputbox(rrequest,isReadonlyPermission,iteratorindex));
                }
            }
        }
        return resultBuf.toString();
    }
    
    private String showConditionInputbox(ReportRequest rrequest,boolean isReadonlyPermission,String dynstyleproperty,int iteratorindex)
    {
        StringBuilder resultBuf=new StringBuilder();
        String reallabel=getLabel(rrequest);
        String conditionname=this.name;
        if(iteratorindex>=0)
        {
            conditionname=conditionname+"_"+iteratorindex;
        }
        String conditionvalue=rrequest.getStringAttribute(conditionname,"");
        conditionvalue=conditionvalue==null?"":conditionvalue.trim();
        if(reallabel!=null&&!reallabel.trim().equals(""))
        {
            if(LABELPOSITION_LEFT.equals(this.labelposition))
            {//如果是标题显示在输入框左侧
                resultBuf.append("<span class=\"cls-search-label\">"+reallabel+"</span> ");
            }else if(LABELPOSITION_INNER.equals(this.labelposition)&&conditionvalue.equals(""))
            {
                conditionvalue=reallabel;
            }
        }
        if(iteratorindex>=0) rrequest.getAttributes().put("DYN_INPUTBOX_ID",this.getInputBoxId()+"__"+iteratorindex);
        resultBuf.append(this.inputbox.getDisplayStringValue(rrequest,conditionvalue,dynstyleproperty,isReadonlyPermission));
        if(reallabel!=null&&!reallabel.trim().equals("")&&this.LABELPOSITION_RIGHT.equals(this.labelposition))
        {
            resultBuf.append("<span class=\"cls-search-label\">"+reallabel+"</span> ");
        }
        if(iteratorindex>=0) rrequest.getAttributes().remove("DYN_INPUTBOX_ID");
        return resultBuf.toString();
    }
    
    public void doPostLoad()
    {
        SqlBean sbean=(SqlBean)this.getParent();
        validateConfig(sbean);
        processInnerlogic();
        if(sbean.isSelectPreparedStatement()) processConditionExpression();
//        {
//                {//只要有一个数据集采用preparedstatement方式执行，则解析此条件的参数化形式（在运行时是采用参数化形式还是普通形式由使用它的数据集决定）
//                    break;
        if(this.isConditionWithInputbox()&&this.iterator>1)
        {
            if(this.conditionExpression!=null)
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name属性为"+this.name
                        +"的查询条件失败，只为它配置了一个条件表达式，不能将iterator属性配置为大于1的数");
            }
        }
        this.ongetvalueMethod=parseOnGetSetValueMethod(this.ongetvalueMethod);
        this.onsetvalueMethod=parseOnGetSetValueMethod(this.onsetvalueMethod);
//        if(this.lstBelongto!=null&&this.lstBelongto.size()>0)
//                if(sbean.getDatasetBeanById(belongtoIdTmp.trim())==null)
//                            +"的查询条件配置的belongto错误，没有找到其所属的<value/>的id："+belongtoIdTmp);
//        }
        if(this.inputbox!=null) this.inputbox.doPostLoad();
    }

    private String parseOnGetSetValueMethod(String method)
    {
        if(Tools.isEmpty(method)) return null;
        List<String> lstMethods=Tools.parseStringToList(method,";",false);
        StringBuilder bufTmp=new StringBuilder();
        for(String methodTmp:lstMethods)
        {
            bufTmp.append("{method:"+methodTmp+"},");
        }
        method=bufTmp.toString();
        if(method.endsWith(",")) method=method.substring(0,method.length()-1);
        if(!method.trim().equals("")) method="["+method+"]";
        return method;
    }
    
    private void processConditionExpression()
    {
        if(this.conditionExpression==null&&this.cvaluesbean==null) return;
        if(this.conditionExpression!=null&&this.conditionExpression.getValue()!=null&&!this.conditionExpression.getValue().trim().equals(""))
        {//直接在<conditon/>的<value/>子标签中配置了一个条件表达式
            conditionExpression.parseConditionExpression();
        }else
        {
            conditionExpression=null;
            List<ConditionSelectItemBean> lstValueBeans=cvaluesbean.getLstSelectItemBeans();
            for(ConditionSelectItemBean cvbTmp:lstValueBeans)
            {
                if(cvbTmp.getConditionExpression()!=null&&cvbTmp.getConditionExpression().getValue()!=null
                        &&!cvbTmp.getConditionExpression().getValue().trim().equals(""))
                {//在<values/>的<value/>中配置了条件表达式
                    cvbTmp.getConditionExpression().parseConditionExpression();
                }else
                {//在<values/>的<value/>的<column/>中配置了条件表达式
                    cvbTmp.setConditionExpression(null);
                    List<ConditionSelectItemBean> lstCcbeans=((ConditionValueSelectItemBean)cvbTmp).getLstColumnsBean();
                    for(ConditionSelectItemBean ccbeanTmp:lstCcbeans)
                    {
                        if(ccbeanTmp.getConditionExpression()==null||ccbeanTmp.getConditionExpression().getValue()==null
                                ||ccbeanTmp.getConditionExpression().getValue().trim().equals(""))
                        {
                            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name属性为"+this.name
                                    +"的查询条件失败，没有为它配置条件表达式");
                        }
                        ccbeanTmp.getConditionExpression().parseConditionExpression();
                    }
                }
            }
        }
    }

    private void validateConfig(SqlBean sbean)
    {
        if(this.ccolumnsbean!=null&&this.ccolumnsbean.isEmpty()) this.ccolumnsbean=null;
        if(this.cvaluesbean!=null&&this.cvaluesbean.isEmpty()) this.cvaluesbean=null;
        if(!isConditionWithInputbox())
        {
            if(this.ccolumnsbean!=null||this.cvaluesbean!=null)
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name属性为"+this.name
                        +"的查询条件失败，此查询条件是隐藏查询条件，不能为其配置<values/>和<columns/>");
            }
            if(this.iterator>1)
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name属性为"+this.name
                        +"的查询条件失败，此查询条件是隐藏查询条件，不能将<condition/>的iterator配置为大于1");
            }
            if(this.innerlogic!=null&&!this.innerlogic.trim().equals("")||this.cinnerlogicbean!=null&&!this.cinnerlogicbean.isEmpty())
            {
                log.warn("报表"+this.getReportBean().getPath()+"的name属性为"+this.name+"的查询条件不显示输入框，因此对它配置innerlogic属性或<innerlogic/>子标签没有任何意义");
                this.innerlogic=null;
                this.cinnerlogicbean=null;
            }
        }else
        {
            /*if(!sbean.isStoreProcedure())
            {
                if(this.ccolumnsbean!=null&&this.cvaluesbean==null)
                {//配置了<columns/>，但没有为它们在<value/>中配置条件表达式
                    throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                            +"的查询条件失败，没有为<columns/>中配置的<column/>在<value/>标签中配置条件表达式");
                }
                if(this.iterator>1&&this.cvaluesbean==null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                            +"的查询条件失败，此查询条件没有提供比较列选择功能和条件表达式选择功能，不能将其iterator属性配置为大于1的数");
                }
            }*/
            if(cvaluesbean!=null)
            {
                List<ConditionSelectItemBean> lstValueBeans=cvaluesbean.getLstSelectItemBeans();
                if(lstValueBeans.size()==1&&lstValueBeans.get(0).getConditionExpression()!=null)
                {//当前<values/>下只有一个<value/>，此<value/>只有一个条件表达式，则相当于直接在<condition/>下配置<value/>
                    this.cvaluesbean=null;
                    this.conditionExpression=lstValueBeans.get(0).getConditionExpression();
                    /*if(this.ccolumnsbean!=null&&!sbean.isStoreProcedure())
                    {
                        throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                                +"的查询条件失败，没有为<columns/>中配置的<column/>在<value/>标签中配置条件表达式");
                    }*/
                    return;
                }
                /*int valuecolumncnt=-1;
                ConditionValueSelectItemBean cvsibeanTmp;
                for(ConditionSelectItemBean csibeanTmp:lstValueBeans)
                {//依次处理每个<value/>
                    cvsibeanTmp=(ConditionValueSelectItemBean)csibeanTmp;
                    if(cvsibeanTmp.getConditionExpression()!=null&&cvsibeanTmp.getLstColumnsBean()!=null&&cvsibeanTmp.getLstColumnsBean().size()>0)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                                +"的查询条件失败，不能在其<values/>的<value/>子标签中即配置<column/>又配置条件表达式");
                    }
                    if(cvsibeanTmp.getConditionExpression()!=null||sbean.isStoreProcedure())
                    {//此<value/>是直接配置的条件表达式，或者是存储过程查询报表数据
                        if(valuecolumncnt>0)
                        {//已经有其它<value/>配置了<column/>
                            if(sbean.isStoreProcedure())
                            {
                                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                                        +"的查询条件失败，此报表是使用存储过程查询报表数据，因此不能在<value/>中配置<conlumn/>子标签");
                            }else
                            {
                                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                                        +"的查询条件失败，其<values/>的<value/>子标签有的直接配置条件表达式，有的配置<conlumn/>子标签");
                            }
                        }
                        valuecolumncnt=0;
                    }else
                    {
                        if(valuecolumncnt==0)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                                    +"的查询条件失败，其<values/>的<value/>子标签有的直接配置条件表达式，有的配置<conlumn/>子标签");
                        }else if(valuecolumncnt>0&&valuecolumncnt!=cvsibeanTmp.getLstColumnsBean().size())
                        {
                            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                                    +"的查询条件失败，其<values/>的<value/>子标签之间配置的<conlumn/>子标签个数不一致");
                        }
                        valuecolumncnt=cvsibeanTmp.getLstColumnsBean().size();
                        if(valuecolumncnt<2)
                        {
                            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                                    +"的查询条件失败，不能在<value/>中只配置一个<column/>，如果不提供比较列的选择功能，请直接在<value/>中配置条件表达式");
                        }
                        for(ConditionSelectItemBean ccbeanTmp:cvsibeanTmp.getLstColumnsBean())
                        {
                            if(this.ccolumnsbean==null||this.ccolumnsbean.getSelectItemBeanById(ccbeanTmp.getId())==null)
                            {//在配置的<columns/>中没有取到对应id的<column/>
                                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()
                                        +"的<condition/>下<values/>的子标签<value/>失败，其下的<column/>子标签配置的refid："+ccbeanTmp.getId()+"在<columns/>中不存在");
                            }
                        }
                    }
                }
                if(!sbean.isStoreProcedure())
                {
                    if(valuecolumncnt>0&&(this.ccolumnsbean==null||this.ccolumnsbean.getLstSelectItemBeans().size()!=valuecolumncnt))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name为"+this.name
                                +"的查询条件失败，其<values/>的<value/>子标签配置的<conlumn/>子标签个数与<columns/>配置的<column/>子标签个数不一致");
                    }else if(valuecolumncnt==0&&this.ccolumnsbean!=null)
                    {//所有<value/>配置的都是条件表达式，没有配置<column/>，则配置了<columns/>也无效，所以置空
                        log.warn("报表"+this.getReportBean().getPath()+"的查询条件<condition/>配置的<columns/>子标签无效，因为没有在<values/>标签中使用它们");
                        this.ccolumnsbean=null;
                    }
                }*/
            }
        }
    }
    
    private void processInnerlogic()
    {
        if(this.cinnerlogicbean!=null&&!this.cinnerlogicbean.isEmpty())
        {//通过<innerlogic/>子标签配置了条件逻辑关系
            String idTmp;
            for(ConditionSelectItemBean csibeanTmp:this.cinnerlogicbean.getLstSelectItemBeans())
            {
                idTmp=csibeanTmp.getId().toLowerCase().trim();
                if(!idTmp.equals("and")&&!idTmp.equals("or"))
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name属性为"+this.name
                            +"的查询条件失败，配置的<innerlogic/>的<logic/>的id必须是and或or之一");
                }
            }
        }
        if(this.iterator<2&&cinnerlogicbean!=null&&!cinnerlogicbean.isEmpty()&&cinnerlogicbean.getLstSelectItemBeans().size()>1)
        {
            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的name属性为"+this.name
                    +"的查询条件失败，没有配置iterator属性或配置的值小于2时，不能将<innerlogic/>标签配置多个<logic/>子标签");
        }
    }
    
    public AbsConfigBean clone(AbsConfigBean parent)
    {
        ConditionBean cbNew=(ConditionBean)super.clone(parent);
        if(inputbox!=null)
        {
            cbNew.setInputbox((AbsInputBox)inputbox.clone(cbNew));
        }
        if(cinnerlogicbean!=null)
        {
            cbNew.setCinnerlogicbean(cinnerlogicbean.clone(cbNew));
        }
        if(ccolumnsbean!=null)
        {
            cbNew.setCcolumnsbean(ccolumnsbean.clone(cbNew));
        }
        if(cvaluesbean!=null)
        {
            cbNew.setCvaluesbean(cvaluesbean.clone(cbNew));
        }
        if(conditionExpression!=null)
        {
            cbNew.setConditionExpression(conditionExpression.clone());
        }
        if(lstChildDisplayOrder!=null)
        {
            cbNew.setLstChildDisplayOrder((List<String>)((ArrayList<String>)lstChildDisplayOrder).clone());
        }
        cloneExtendConfig(cbNew);
        return cbNew;
    }
}
