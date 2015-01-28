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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Tools;

public class DefaultConcreteExpressionBean extends AbsConcreteExpressionBean
{
    private static Log log=LogFactory.getLog(DefaultConcreteExpressionBean.class);
    
    protected String type;

    private String param1;

    private List<EditableReportParamBean> lstParamsBean1;

    private String param2;

    private List<EditableReportParamBean> lstParamsBean2;//如果第二个参数是SQL语句种情况，这里存放获取数据的SQL语句所用到的动态参数列表

    private Class param1Class;

    private Class param2Class;

    private AbsCompareDataType datatypeObj;

    public boolean isTrue(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues)
    {
        Object realParam1Value=null;
        if(param1Class!=null)
        {
            realParam1Value=getRealParamValueByClass(rrequest,mRowData,mParamValues,this.param1Class);
        }else
        {
            realParam1Value=getRealParamValue(rrequest,mRowData,mParamValues,param1,lstParamsBean1);
        }
        Object realParam2Value=null;
        if(param2Class!=null)
        {//如果第二个参数是从自定义JAVA类中获取
            realParam2Value=getRealParamValueByClass(rrequest,mRowData,mParamValues,this.param2Class);
        }else
        {
            realParam2Value=getRealParamValue(rrequest,mRowData,mParamValues,param2,lstParamsBean2);
        }
        log.debug("compare value1:"+realParam1Value+";value2:"+realParam2Value);
        if(type.equals("eq"))
        {
            return datatypeObj.isEquals(realParam1Value,realParam2Value);
        }else if(type.equals("neq"))
        {
            return !datatypeObj.isEquals(realParam1Value,realParam2Value);
        }else if(type.equals("gt"))
        {
            return datatypeObj.isGreaterThan(realParam1Value,realParam2Value);
        }else if(type.equals("lt"))
        {
            return !datatypeObj.isEquals(realParam1Value,realParam2Value)&&!datatypeObj.isGreaterThan(realParam1Value,realParam2Value);
        }else if(type.equals("gte"))
        {
            return datatypeObj.isGreaterThan(realParam1Value,realParam2Value)||datatypeObj.isEquals(realParam1Value,realParam2Value);
        }else
        {
            return !datatypeObj.isGreaterThan(realParam1Value,realParam2Value);
        }
    }

    private Object getRealParamValueByClass(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues,Class c)
    {
        try
        {
            Method m=c.getDeclaredMethod("getValue",new Class[] { ReportRequest.class, ReportBean.class, Map.class, Map.class });
            return m.invoke(c.newInstance(),new Object[] { rrequest, getReportBean(), mRowData, mParamValues });
        }catch(Exception e)
        {
            throw new WabacusRuntimeException("加载报表"+getReportBean().getPath()+"的precondition的条件值失败，无法调用自定义JAVA类："+c.getName()
                    +"的方法getValue(ReportRequet,ReportBean,Map,Map)获取数据",e);
        }
    }

    private Object getRealParamValue(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues,String param,
            List<EditableReportParamBean> lstParamBeans)
    {
        if(param==null||param.trim().equals("")) return param;
        Object paramValue;
        if(WabacusAssistant.getInstance().isGetRequestContextValue(param))
        {//从url/request/rrequest/session中取数据
            paramValue=WabacusAssistant.getInstance().getRequestContextValue(rrequest,param);
        }else if(Tools.isDefineKey("@",param))
        {
            if(mRowData==null) return null;
            param=Tools.getRealKeyByDefine("@",param);
            paramValue=mRowData.get(param);
        }else if(Tools.isDefineKey("#",param))
        {
            if(mParamValues==null) return null;
            paramValue=mParamValues.get(Tools.getRealKeyByDefine("#",param));
        }else if(Tools.isDefineKey("sql",param))
        {
            String sql=Tools.getRealKeyByDefine("sql",param);
            sql=getStatementExecuteSql(rrequest,mRowData,mParamValues,sql,lstParamBeans);
            paramValue=getDataFromDB(rrequest,mRowData,mParamValues,lstParamBeans,sql);
        }else
        {//常量
            paramValue=param;
        }
        return paramValue;
    }

    private Object getDataFromDB(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues,
            List<EditableReportParamBean> lstParamBeans,String sql)
    {
        Connection conn=rrequest.getConnection(this.datasource);
        Statement stmt=null;
        ResultSet rs=null;
        try
        {
            IInterceptor interceptorObj=this.getReportBean().getInterceptor();
            if(interceptorObj!=null)
            {
                Object objTmp=interceptorObj.beforeLoadData(rrequest,this.getReportBean(),this,sql);
                if(objTmp==null) return null;
                if(!(objTmp instanceof String))
                {
                    throw new WabacusRuntimeException("加载报表"+getReportBean().getPath()+"的precondition的条件值失败，此时的拦截器前置动作只能返回SQL语句");
                }
                sql=(String)objTmp;
            }
            if(Tools.isEmpty(sql)) return null;
            if(Config.show_sql) log.info("Execute sql："+sql);
            stmt=conn.createStatement();
            rs=stmt.executeQuery(sql);
            if(!rs.next()) return null;
            return this.datatypeObj.getDataFromResultSet(rs);
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("执行报表"+getReportBean().getPath()+"的SQL语句："+sql+"失败",e);
        }finally
        {
            try
            {
                rs.close();
            }catch(SQLException e)
            {
                e.printStackTrace();
            }
            WabacusAssistant.getInstance().release(null,stmt);
        }
    }

    private String getStatementExecuteSql(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mParamValues,String sql,
            List<EditableReportParamBean> lstParamBeans)
    {
        ReportBean rbean=this.getReportBean();
        if(lstParamBeans!=null&&lstParamBeans.size()>0)
        {
            String paramValueTmp;
            AbsDatabaseType dbtype=rrequest.getDbType(this.datasource);
            for(EditableReportParamBean paramBean:lstParamBeans)
            {
                paramValueTmp=paramBean.getRuntimeParamValue(rrequest,rbean,mRowData,mParamValues,this.datasource,ownerEditbean.isAutoReportdata());
                paramValueTmp=dbtype.getStatementValue(paramBean.getDataTypeObj(),paramValueTmp);
                if(paramValueTmp==null) paramValueTmp="null";
                sql=Tools.replaceAll(sql,paramBean.getPlaceholder(),paramValueTmp);
            }
        }
        return sql;
    }

    public void parseParams()
    {
        super.parseParams();
        if(lstParams==null||lstParams.size()<3)
        {
            throw new WabacusConfigLoadingException("解析报表"+getReportBean().getPath()+"配置的precondition中表达式失败，采用内置的比较条件时，必须指定比较参数和被比较参数");
        }
        this.type=lstParams.get(0).toLowerCase().trim();
        lstParamsBean1=new ArrayList<EditableReportParamBean>();
        this.param1=parseCompareParams(lstParams.get(1),lstParamsBean1);
        this.param1Class=getParamClass(this.param1);
        lstParamsBean2=new ArrayList<EditableReportParamBean>();
        this.param2=parseCompareParams(lstParams.get(2),lstParamsBean2);
        this.param2Class=getParamClass(this.param2);
        String datatype=lstParams.size()>3?lstParams.get(3):null;//比较的数据类型
        if(datatype!=null) datatype=datatype.toLowerCase().trim();
        if("int".equalsIgnoreCase(datatype))
        {
            this.datatypeObj=new IntDataType();
        }else if("long".equalsIgnoreCase(datatype))
        {
            this.datatypeObj=new LongDataType();
        }else if("float".equalsIgnoreCase(datatype))
        {
            this.datatypeObj=new FloatDataType();
        }else if("double".equalsIgnoreCase(datatype))
        {
            this.datatypeObj=new DoubleDataType();
        }else if("boolean".equalsIgnoreCase(datatype))
        {
            this.datatypeObj=new BooleanDataType();
        }else if("object".equalsIgnoreCase(datatype))
        {
            this.datatypeObj=new ObjectDataType();
        }else
        {
            this.datatypeObj=new StringDataType();
        }
    }

    private String parseCompareParams(String param,List<EditableReportParamBean> lstSqlParams)
    {
        if(param==null||param.trim().equals("")) return param;
        if(Tools.isDefineKey("sql",param.trim()))
        {
            param=Tools.getRealKeyByDefine("sql",param);
            param=this.ownerEditbean.parseStandardEditSql(param,lstSqlParams,reportTypeKey,false,false);
            param="sql{"+param+"}";
        }else if(Tools.isDefineKey("url",param.trim()))
        {
            getReportBean().addParamNameFromURL(Tools.getRealKeyByDefine("url",param));
        }else if(param.trim().startsWith("'")&&param.trim().endsWith("'")&&param.trim().length()>=2)
        {
            param=param.trim().substring(1,param.trim().length()-1);
        }else if(param.trim().toLowerCase().equals("null"))
        {//配置的为null（不是'null'）
            param=null;
        }
        return param;
    }

    private Class getParamClass(String param)
    {
        if(!Tools.isDefineKey("class",param)) return null;
        param=Tools.getRealKeyByDefine("class",param);
        return ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(param);
    }

//        correctParamnameOfCol(this.lstParamsBean2);

    private abstract class AbsCompareDataType
    {
        public abstract Object getDataFromResultSet(ResultSet rs) throws SQLException;
        
        public boolean isEquals(Object paramObj1,Object paramObj2)
        {
            if(paramObj1==null||paramObj1.toString().equals("")||paramObj2==null||paramObj2.toString().equals(""))
            {
                return isEqualsBetweenEmptyParams(paramObj1,paramObj2);
            }else
            {
                return isEqualsBetweenNonEmptyParams(paramObj1,paramObj2);
            }
        }
        
        protected boolean isEqualsBetweenEmptyParams(Object paramObj1,Object paramObj2)
        {
            if(paramObj1==null&&paramObj2==null) return true;
            if(paramObj1==null&&paramObj2!=null||paramObj1!=null&&paramObj2==null)
            {
                return false;
            }
            if(paramObj1.toString().equals("")&&paramObj2.toString().equals("")) return true;
            return false;
        }
        
        protected boolean isEqualsBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            try
            {
                return getRealTypeValue(paramObj1)==getRealTypeValue(paramObj2);
            }catch(Exception e)
            {
                return false;
            }
        }

        public boolean isGreaterThan(Object paramObj1,Object paramObj2)
        {
            if(paramObj1==null||paramObj2==null||paramObj1.toString().trim().equals("")||paramObj2.toString().trim().equals(""))
            {
                return isGreaterThanBetweenEmptyParams(paramObj1,paramObj2);
            }
            return isGreaterThanBetweenNonEmptyParams(paramObj1,paramObj2);
        }
        
        protected boolean isGreaterThanBetweenEmptyParams(Object paramObj1,Object paramObj2)
        {
            if(paramObj1==null&&paramObj2==null) return false;
            if(paramObj1==null&&paramObj2!=null) return false;
            if(paramObj1!=null&&paramObj2==null) return true;
            if(paramObj1.toString().trim().equals("")) return false;
            return true;//paramObj1不为空而paramObj2为空字符串
        }
        
        protected abstract boolean isGreaterThanBetweenNonEmptyParams(Object paramObj1,Object paramObj2);
        
        protected abstract Object getRealTypeValue(Object paramObj);
    }
    
    private class BooleanDataType extends AbsCompareDataType
    {

        public Object getDataFromResultSet(ResultSet rs) throws SQLException
        {
            return rs.getBoolean(1);
        }

        protected Object getRealTypeValue(Object paramObj)
        {
            boolean resultValue;
            if(paramObj instanceof Boolean)
            {
                resultValue=(Boolean)paramObj;
            }else
            {
                resultValue=paramObj.toString().trim().toLowerCase().equals("true");
            }
            return resultValue;
        }
        
        protected boolean isGreaterThanBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            return false;
        }
    }
    
    private class DoubleDataType extends AbsCompareDataType
    {
        public Object getDataFromResultSet(ResultSet rs) throws SQLException
        {
            return rs.getDouble(1);
        }
        
        protected boolean isGreaterThanBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            try
            {
                double d1=(Double)getRealTypeValue(paramObj1);
                double d2=(Double)getRealTypeValue(paramObj2);
                return d1>d2;
            }catch(Exception e)
            {
                return false;
            }
        }
        
        protected Object getRealTypeValue(Object paramObj)
        {
            double resultValue;
            if(paramObj instanceof Double)
            {
                resultValue=(Double)paramObj;
            }else
            {
                resultValue=Double.parseDouble(String.valueOf(paramObj));
            }
            return resultValue;
        }
    }
    
    private class FloatDataType extends AbsCompareDataType
    {
        public Object getDataFromResultSet(ResultSet rs) throws SQLException
        {
            return rs.getFloat(1);
        }
        
        protected boolean isGreaterThanBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            try
            {
                float d1=(Float)getRealTypeValue(paramObj1);
                float d2=(Float)getRealTypeValue(paramObj2);
                return d1>d2;
            }catch(Exception e)
            {
                return false;
            }
        }
        
        protected Object getRealTypeValue(Object paramObj)
        {
            float resultValue;
            if(paramObj instanceof Float)
            {
                resultValue=(Float)paramObj;
            }else
            {
                resultValue=Float.parseFloat(String.valueOf(paramObj));
            }
            return resultValue;
        }
    }
    
    private class IntDataType extends AbsCompareDataType
    {
        public Object getDataFromResultSet(ResultSet rs) throws SQLException
        {
            return rs.getInt(1);
        }
        
        protected boolean isGreaterThanBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            try
            {
                int d1=(Integer)getRealTypeValue(paramObj1);
                int d2=(Integer)getRealTypeValue(paramObj2);
                return d1>d2;
            }catch(Exception e)
            {
                return false;
            }
        }
        
        protected Object getRealTypeValue(Object paramObj)
        {
            int resultValue;
            if(paramObj instanceof Integer)
            {
                resultValue=(Integer)paramObj;
            }else
            {
                resultValue=Integer.parseInt(String.valueOf(paramObj));
            }
            return resultValue;
        }
    }
    
    private class LongDataType extends AbsCompareDataType
    {
        public Object getDataFromResultSet(ResultSet rs) throws SQLException
        {
            return rs.getLong(1);
        }
        
        protected boolean isGreaterThanBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            try
            {
                long d1=(Long)getRealTypeValue(paramObj1);
                long d2=(Long)getRealTypeValue(paramObj2);
                return d1>d2;
            }catch(Exception e)
            {
                return false;//如果有一个不是合法的数字，则说明不相等
            }
        }
        
        protected Object getRealTypeValue(Object paramObj)
        {
            long resultValue;
            if(paramObj instanceof Long)
            {
                resultValue=(Long)paramObj;
            }else
            {
                resultValue=Long.parseLong(String.valueOf(paramObj));
            }
            return resultValue;
        }
    }
    
    private class ObjectDataType extends AbsCompareDataType
    {
        public Object getDataFromResultSet(ResultSet rs) throws SQLException
        {
            return rs.getObject(1);
        }
        
        protected Object getRealTypeValue(Object paramObj)
        {
            return paramObj;
        }

        protected boolean isEqualsBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            return paramObj1.equals(paramObj2);
        }
        
        protected boolean isGreaterThanBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            if(!(paramObj1 instanceof Comparable)||!(paramObj2 instanceof Comparable)) return false;
            return ((Comparable)paramObj1).compareTo(paramObj2)>0;
        }
    }
    
    private class StringDataType extends AbsCompareDataType
    {
        public Object getDataFromResultSet(ResultSet rs) throws SQLException
        {
            return rs.getString(1);
        }
        
        protected Object getRealTypeValue(Object paramObj)
        {
            return paramObj.toString();
        }

        protected boolean isEqualsBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            return paramObj1.equals(paramObj2);
        }
        
        protected boolean isGreaterThanBetweenNonEmptyParams(Object paramObj1,Object paramObj2)
        {
            return paramObj1.toString().compareTo(paramObj2.toString())>0;
        }
    }
}
