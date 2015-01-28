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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.database.type.AbsDatabaseType;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.datatype.AbsDateTimeType;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.IntType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;
import com.wabacus.util.UUIDGenerator;

public class EditableReportExternalValueBean implements Cloneable
{
    private String objectId;
    
    private String name;//<value/>配置的name属性

    private String value;

    private IDataType typeObj;

    private List<EditableReportParamBean> lstParamsBean;

    private AbsEditableReportEditDataBean owner;
    
    private Object refObj;
    
    private AbsEditableReportEditDataBean refEditBean;
    
    public EditableReportExternalValueBean(AbsEditableReportEditDataBean owner)
    {
        this.owner=owner;
        objectId=Tools.generateObjectId(this.getClass());
    }
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name=name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public IDataType getTypeObj()
    {
        return typeObj;
    }

    public void setTypeObj(IDataType typeObj)
    {
        this.typeObj=typeObj;
    }

    public List<EditableReportParamBean> getLstParamsBean()
    {
        return lstParamsBean;
    }

    public void setLstParamsBean(List<EditableReportParamBean> lstParamsBean)
    {
        this.lstParamsBean=lstParamsBean;
    }
    
    public String getObjectId()
    {
        return objectId;
    }

    public AbsEditableReportEditDataBean getOwner()
    {
        return owner;
    }

    public String getParamValue(ReportRequest rrequest,Map<String,String> mRowData,Map<String,String> mCustomizedValues,Map<String,String> mParamValues)
    {
        if(Tools.isEmpty(this.value)) return this.value;
        try
        {
            String paramvalue=null;
            if(this.refObj instanceof ColBean)
            {//从本报表或其它绑定保存的报表中的某列中取值
                ColBean refColBean=(ColBean)this.refObj;
                List<Map<String,String>> lstEditData=rrequest.getCdb(refColBean.getReportBean().getId()).getLstEditedData(this.refEditBean);
                Map<String,String> mRefRowData=lstEditData!=null&&lstEditData.size()>0?lstEditData.get(0):null;
                paramvalue=EditableReportAssistant.getInstance().getColParamValue(rrequest,refColBean.getReportBean(),mRefRowData,this.value);
            }else if(this.refObj instanceof EditableReportExternalValueBean)
            {
                paramvalue=EditableReportAssistant.getInstance().getParamValueOfOneRowData(this.refEditBean,
                        (EditableReportExternalValueBean)this.refObj,rrequest,0);
            }else if(this.value.equals("uuid{}"))
            {
                paramvalue=UUIDGenerator.generateID();
            }else if(Tools.isDefineKey("increment",this.value))
            {
                paramvalue=EditableReportAssistant.getInstance().getAutoIncrementIdValue(rrequest,this.owner.getOwner().getReportBean(),
                        this.owner.getDatasource(),this.value);
            }else if(WabacusAssistant.getInstance().isGetRequestContextValue(this.value))
            {
                paramvalue=WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,this.value,"");
            }else if(this.value.equals("now{}"))
            {
                SimpleDateFormat sdf=new SimpleDateFormat(((AbsDateTimeType)this.typeObj).getDateformat());
                paramvalue=sdf.format(new Date());
            }else if(Tools.isDefineKey("!",this.value))
            {
                String customizeParamName=Tools.getRealKeyByDefine("!",this.value);
                if(mCustomizedValues==null||!mCustomizedValues.containsKey(customizeParamName)) return "";
                paramvalue=mCustomizedValues.get(customizeParamName);
            }else if(Tools.isDefineKey("sequence",this.value))
            {
                Statement stmt=rrequest.getConnection(this.owner.getDatasource()).createStatement();
                ResultSet rs=stmt.executeQuery(rrequest.getDbType(this.owner.getDatasource()).getSequenceValueSql(
                        Tools.getRealKeyByDefine("sequence",this.value)));
                rs.next();
                if(this.typeObj==null) this.typeObj=Config.getInstance().getDataTypeByClass(IntType.class);
                paramvalue=String.valueOf(this.typeObj.getColumnValue(rs,1,rrequest.getDbType(this.owner.getDatasource())));
                rs.close();
                stmt.close();
            }else if(this.value.toLowerCase().trim().startsWith("select ")&&this.lstParamsBean!=null)
            {
                ReportBean rbean=this.owner.getOwner().getReportBean();
                AbsDatabaseType dbtype=rrequest.getDbType(this.owner.getDatasource());
                PreparedStatement pstmtTemp=rrequest.getConnection(this.owner.getDatasource()).prepareStatement(this.value);
                int j=1;
                String paramvalueTmp;
                for(EditableReportParamBean paramBean:this.getLstParamsBean())
                {
                    paramvalueTmp=null;
                    if(paramBean.getOwner() instanceof EditableReportExternalValueBean)
                    {
                        if(mParamValues!=null) paramvalueTmp=paramBean.getRealParamValue(mParamValues.get(paramBean.getParamname()),rrequest,rbean);
                    }else if(mRowData!=null)
                    {
                        paramvalueTmp=paramBean.getRealParamValue(EditableReportAssistant.getInstance().getColParamValue(rrequest,rbean,mRowData,
                                paramBean.getParamname()),rrequest,rbean);
                    }
                    paramBean.getDataTypeObj().setPreparedStatementValue(j++,paramvalueTmp,pstmtTemp,dbtype);
                }
                ResultSet rs=pstmtTemp.executeQuery();
                if(this.typeObj==null) this.typeObj=Config.getInstance().getDataTypeByClass(VarcharType.class);
                paramvalue=typeObj.value2label(typeObj.getColumnValue(rs,1,dbtype));
                rs.close();
                pstmtTemp.close();
            }else
            {//常量
                paramvalue=this.value;
            }
            return paramvalue==null?"":paramvalue;
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("从数据库获取报表"+this.owner.getOwner().getReportBean().getPath()+"的变量"+this.value+"的值失败",e);
        }
    }
    
    public void parseValues(String reportTypeKey)
    {
        if(Tools.isDefineKey("#",value))
        {
            parseRefOtherParamValue();
        }else if(Tools.isDefineKey("@",value))
        {
            parseRefColExternalValue();
        }else if(Tools.isDefineKey("url",value))
        {
            this.owner.getOwner().getReportBean().addParamNameFromURL(Tools.getRealKeyByDefine("url",value));
        }else if(Tools.isDefineKey("sql",value))
        {
            parseSqlExternalValue(reportTypeKey);
        }
    }

    private void parseRefOtherParamValue()
    {
        ReportBean rbean=this.owner.getOwner().getReportBean();
        Object[] refValObjsArr=parseRefValue("#");
        ReportBean refReportBean=(ReportBean)refValObjsArr[0];
        String refEditype=(String)refValObjsArr[1];
        String refParamname=(String)refValObjsArr[2];
        this.refObj=this.refEditBean.getExternalValueBeanByName(refParamname,false);
        if(this.refObj==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<params/>中name属性为"+name+"的<value/>配置错误，引用其它报表变量值时，引用的报表："
                    +refReportBean.getId()+"没有在<"+refEditype+"/>定义name为"+refParamname+"的变量");
        }
    }
    
    private void parseRefColExternalValue()
    {
        ReportBean rbean=this.owner.getOwner().getReportBean();
        Object[] refValObjsArr=parseRefValue("@");
        ReportBean refReportBean=(ReportBean)refValObjsArr[0];
        String refEditype=(String)refValObjsArr[1];
        String refColProperty=(String)refValObjsArr[2];
        if(refEditype.equals("insert")&&refColProperty.endsWith("__old"))
        {
            refColProperty=refColProperty.substring(0,refColProperty.length()-"__old".length());
        }
        String refRealColumn=refColProperty.endsWith("__old")?refColProperty.substring(0,refColProperty.length()-"__old".length()):refColProperty;
        ColBean refColbeanTmp=refReportBean.getDbean().getColBeanByColProperty(refRealColumn);
        if(refColbeanTmp==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<params/>中name属性为"+name+"的<value/>配置错误，引用其它报表列时，引用的报表："
                    +refReportBean.getId()+"没有定义column或property为"+refRealColumn+"的列");
        }
        this.value=refColProperty;
        this.refObj=refColbeanTmp;
    }
    
    private Object[] parseRefValue(String type)
    {
        ReportBean rbean=this.owner.getOwner().getReportBean();
        List<String> lstTmp=Tools.parseStringToList(Tools.getRealKeyByDefine(type,value),".",false);
        ReportBean refReportBean=null;
        String refEditype=null;
        String refParamname=null;
        String typeName=type.equals("@")?"列":"变量";//用于提示错误信息
        if(lstTmp.size()==2)
        {//insert/update/delete.paramname格式，即引用本报表的变量
            refReportBean=rbean;
            refEditype=lstTmp.get(0);
            refParamname=lstTmp.get(1);
        }else if(lstTmp.size()==3)
        {
            String refReportid=lstTmp.get(0);
            if(Tools.isEmpty(refReportid))
            {
                refReportBean=rbean;
            }else
            {
                refReportBean=rbean.getPageBean().getReportChild(refReportid,true);
                if(refReportBean==null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<params/>中name属性为"+name+"的<value/>配置错误，引用其它报表"+typeName
                            +"值时，没找到被引用的报表："+refReportid);
                }
            }
            refEditype=lstTmp.get(1);
            refParamname=lstTmp.get(2);
        }else
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<params/>中name属性为"+name+"的<value/>配置错误，引用其它报表"+typeName+"值的格式不合法，必须为"
                    +type+"{reportid.insert/update/delete.paramname}或"+type+"{insert/update/delete.paramname}格式");
        }
        refEditype=refEditype==null?"":refEditype.toLowerCase().trim();
        if(!refEditype.equals("insert")&&!refEditype.equals("update")&&!refEditype.equals("delete"))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<params/>中name属性为"+name
                    +"的<value/>配置错误，必须配置为reportid.insert/update/delete.name格式");
        }
        EditableReportSqlBean editsqlbean=(EditableReportSqlBean)refReportBean.getSbean().getExtendConfigDataForReportType(
                EditableReportSqlBean.class);
        if(editsqlbean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<params/>中name属性为"+name+"的<value/>配置错误，引用其它报表变量值时，引用的报表："
                    +refReportBean.getId()+"不是可编辑报表");
        }
        if("insert".equals(refEditype))
        {//是引用源报表中<insert/>中定义的某个变量值
            this.refEditBean=editsqlbean.getInsertbean();
        }else if("update".equals(refEditype))
        {
            this.refEditBean=editsqlbean.getUpdatebean();
        }else
        {
            this.refEditBean=editsqlbean.getDeletebean();
        }
        if(this.refEditBean==null)
        {//被引用的源报表没有配置<insert/>标签
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<params/>中name属性为"+name+"的<value/>配置错误，引用其它报表"+typeName+"值时，引用的报表："
                    +refReportBean.getId()+"没有配置<"+refEditype+"/>");
        }
        if(Tools.isEmpty(refParamname))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，在<params/>中name属性为"+name+"的<value/>配置错误，引用其它报表"+typeName+"值时，必须指定被引用的"
                    +typeName+"名");
        }
        return new Object[] { refReportBean, refEditype, refParamname.trim()};
    }
    
    private void parseSqlExternalValue(String reportTypeKey)
    {
        String sql=Tools.getRealKeyByDefine("sql",value.trim());
        if(sql.equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+this.owner.getOwner().getReportBean().getPath()+"失败，在<params/>中name属性为"+name+"的<value/>配置错误，查询数据的SQL语句不能为空");
        }
        lstParamsBean=new ArrayList<EditableReportParamBean>();
        this.value=this.owner.parseStandardEditSql(sql,lstParamsBean,reportTypeKey,this.owner.isPreparedStatement(),false);
    }
    
    public Object clone()
    {
        try
        {
            EditableReportExternalValueBean newValueBean=(EditableReportExternalValueBean)super.clone();
            newValueBean.objectId=Tools.generateObjectId(this.getClass());
            return newValueBean;
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone 对象失败",e);
        }
    }

    public int hashCode()
    {
        final int prime=31;
        int result=1;
        result=prime*result+((name==null)?0:name.hashCode());
        result=prime*result+((owner==null)?0:owner.hashCode());
        result=prime*result+((value==null)?0:value.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if(this==obj) return true;
        if(obj==null) return false;
        if(getClass()!=obj.getClass()) return false;
        final EditableReportExternalValueBean other=(EditableReportExternalValueBean)obj;
        if(name==null)
        {
            if(other.name!=null) return false;
        }else if(!name.equals(other.name)) return false;
        if(owner==null)
        {
            if(other.owner!=null) return false;
        }else if(!owner.equals(other.owner)) return false;
        if(value==null)
        {
            if(other.value!=null) return false;
        }else if(!value.equals(other.value)) return false;
        return true;
    }
    
    
}
