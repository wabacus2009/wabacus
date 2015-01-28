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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.AbsSelectBox;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class SqlBean extends AbsConfigBean
{
    private Boolean isPreparedStatement=null;
    
    private Boolean isSelectPreparedStatement=null;//在<select/>标签中配置的preparedstatement属性
    
    private String datasource;//此报表所使用的数据源，默认为wabacus.cfg.xml中<datasources/>标签中的default属性配置的值

//    private Object searchTemplateObj;//搜索栏的显示模板，可能是字符串或TemplateBean
    
    private List<ConditionBean> lstConditions=new ArrayList<ConditionBean>();

    private Map<String,ConditionBean> mConditions;//运行时由lstConditions生成，方便根据<condition/>的name属性取到对应的ConditionBean对象
    
    private List<String> lstConditionFromRequestNames;
    
    private String beforeSearchMethod;
    
    private List<ReportDataSetBean> lstDatasetBeans;//存放所有<value/>子标签，存放顺序为它们的执行顺序
    
    private Map<String,ReportDataSetBean> mDatasetBeans;//根据lstDatasetBeans生成
    
    private List<List<ReportDataSetBean>> lstDatasetGroupBeans;//根据<dataset/>的groupid进行分组存放，相同的groupid存放在一个List中
    
    private ReportDataSetBean hdsOnlyTitleDatasetBean;//只查询动态标题的label和value的<dataset/>
    
    private List<List<ReportDataSetBean>> lstHdsDataDatasetGroupBeans;//如果横向数据集中存在只查询标题的<dataset/>，则除去lstDatasetGroupBeans中只查询标题的<dataset/>，即查询数据部分的<dataset/>
    
    private String hdsTitleLabelColumn;//如果是横向数据集，这里存放查询标题显示label的<col/>的column属性配置值
    
    private String hdsTitleValueColumn;//如果是横向数据集，这里存放查询标题数据的<col/>的column属性配置值
    
    private ColBean hdsTitleLabelCbean;//横向数据集中查询标题label的<col/>，在doPostLoad()方法中根据hdsTitleLabelColumn构造
    
    private ColBean hdsTitleValueCbean;//横向数据集中查询标题数据的<col/>，在doPostLoad()方法中根据hdsTitleValueColumn构造
    
    private String titlecolumndatasetid;//在横向数据集中，如果配置了多个<dataset/>，则指定以哪个<dataset/>查询出的标题行为标准，即它查出了多少条记录，就显示多少个列。如果没有配置或配置为空，则以查出最多记录的<dataset/>为准，其它如果没有相应标题的列，则数据部分显示为空。
    
    public SqlBean(AbsConfigBean parent)
    {
        super(parent);
    }

    public String getBeforeSearchMethod()
    {
        return beforeSearchMethod;
    }

    public void setBeforeSearchMethod(String beforeSearchMethod)
    {
        this.beforeSearchMethod=beforeSearchMethod;
    }

    public boolean isPreparedStatement()
    {
        if(isPreparedStatement==null)
        {
            isPreparedStatement=Config.getInstance().getSystemConfigValue("default-preparedstatement-sql","true").equalsIgnoreCase("true");
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

    public boolean isSelectPreparedStatement()
    {
        if(isSelectPreparedStatement==null)
        {
            isSelectPreparedStatement=this.isPreparedStatement();
        }
        return isSelectPreparedStatement;
    }

    public void setSelectPreparedStatement(String isPreparedStatement)
    {
        if(isPreparedStatement==null||isPreparedStatement.trim().equals(""))
        {
            this.isSelectPreparedStatement=null;
        }else
        {
            this.isSelectPreparedStatement=isPreparedStatement.trim().equalsIgnoreCase("true");
        }
    }
    
    public List<ConditionBean> getLstConditions()
    {
        return lstConditions;
    }

    public void setLstConditions(List<ConditionBean> lstConditions)
    {
        this.lstConditions=lstConditions;
    }

    public String getDatasource()
    {
        return datasource;
    }

    public void setDatasource(String datasource)
    {
        this.datasource=datasource;
    }
    
    public List<ReportDataSetBean> getLstDatasetBeans()
    {
        return lstDatasetBeans;
    }
    
    public void setLstDatasetBeans(List<ReportDataSetBean> lstDatasetBeans)
    {
        this.lstDatasetBeans=lstDatasetBeans;
        if(lstDatasetBeans==null||lstDatasetBeans.size()==0)
        {
            this.mDatasetBeans=null;
        }else
        {
            this.mDatasetBeans=new HashMap<String,ReportDataSetBean>();
            for(ReportDataSetBean dsbeanTmp:this.lstDatasetBeans)
            {
                mDatasetBeans.put(dsbeanTmp.getId(),dsbeanTmp);
            }
        }
    }

    public List<List<ReportDataSetBean>> getLstDatasetGroupBeans()
    {
        return lstDatasetGroupBeans;
    }

    public List<List<ReportDataSetBean>> getLstHdsDataDatasetGroupBeans()
    {
        return lstHdsDataDatasetGroupBeans;
    }

    public void setHdsTitleLabelColumn(String hdsTitleLabelColumn)
    {
        this.hdsTitleLabelColumn=hdsTitleLabelColumn;
    }

    public void setHdsTitleValueColumn(String hdsTitleValueColumn)
    {
        this.hdsTitleValueColumn=hdsTitleValueColumn;
    }

    public String getHdsTitleLabelColumn()
    {
        return hdsTitleLabelColumn;
    }

    public String getHdsTitleValueColumn()
    {
        return hdsTitleValueColumn;
    }

    public ColBean getHdsTitleLabelCbean()
    {
        return hdsTitleLabelCbean;
    }

    public ColBean getHdsTitleValueCbean()
    {
        return hdsTitleValueCbean;
    }

    public boolean isMultiDatasetRows(boolean isOnlyDatasetForData)
    {
        if(!this.isHorizontalDataset()||this.hdsOnlyTitleDatasetBean==null||!isOnlyDatasetForData)
        {
            return this.lstDatasetBeans!=null&&this.lstDatasetBeans.size()>1;
        }else
        {
            if(this.lstHdsDataDatasetGroupBeans==null) return false;
            if(this.lstHdsDataDatasetGroupBeans.size()>1) return true;
            return this.lstHdsDataDatasetGroupBeans.get(0).size()>1;
        }
    }
    
    public boolean isMultiDatasetGroups(boolean isOnlyDatasetForData)
    {
        if(!this.isHorizontalDataset()||this.hdsOnlyTitleDatasetBean==null||!isOnlyDatasetForData)
        {
            return this.lstDatasetGroupBeans!=null&&this.lstDatasetGroupBeans.size()>1;
        }else
        {
            return this.lstHdsDataDatasetGroupBeans!=null&&this.lstHdsDataDatasetGroupBeans.size()>1;
        }
    }
    
    public boolean isMultiDataSetCols()
    {
        if(this.lstDatasetBeans==null||this.lstDatasetBeans.size()==0) return false;
        for(ReportDataSetBean datasetBeanTmp:this.lstDatasetBeans)
        {
            if(datasetBeanTmp.getLstValueBeans()!=null&&datasetBeanTmp.getLstValueBeans().size()>1) return true;
        }
        return false;
    }
    
    public boolean isHorizontalDataset()
    {
        return !Tools.isEmpty(this.hdsTitleLabelColumn)||!Tools.isEmpty(this.hdsTitleValueColumn);
    }
    
    public ReportDataSetBean getDatasetBeanById(String datasetid)
    {
        if(this.mDatasetBeans==null) return null;
        if(datasetid==null||datasetid.trim().equals("")) datasetid=Consts.DEFAULT_KEY;
        return this.mDatasetBeans.get(datasetid);
    }
    
    public List<ReportDataSetValueBean> getLstDatasetValueBeansByValueid(String valueid)
    {
        if(this.mDatasetBeans==null) return null;
        if(valueid==null||valueid.trim().equals("")) valueid=Consts.DEFAULT_KEY;
        List<ReportDataSetValueBean> lstResults=new ArrayList<ReportDataSetValueBean>();
        ReportDataSetValueBean dsvbeanTmp;
        for(ReportDataSetBean dsbeanTmp:this.lstDatasetBeans)
        {
            dsvbeanTmp=dsbeanTmp.getDatasetValueBeanById(valueid);
            if(dsvbeanTmp!=null) lstResults.add(dsvbeanTmp);
        }
        return lstResults;
    }
    
    public List<ReportDataSetValueBean> getLstDatasetValueBeansOfCbean(ColBean cbean)
    {
        if(this.mDatasetBeans==null) return null;
        List<ReportDataSetValueBean> lstResults=new ArrayList<ReportDataSetValueBean>();
        ReportDataSetValueBean dsvbeanTmp;
        for(ReportDataSetBean dsbeanTmp:this.lstDatasetBeans)
        {
            if(this.isHorizontalDataset()
                    &&(cbean.getColumn().equals(this.hdsTitleLabelCbean.getColumn())||cbean.getColumn().equals(this.hdsTitleValueCbean.getColumn())))
            {//如果是横向数据集，且当前<col/>就是查询标题行的各列数据或显示label，则所有<value/>都会查询这两列数据
                lstResults.addAll(dsbeanTmp.getLstValueBeans());
            }else
            {
                dsvbeanTmp=dsbeanTmp.getDatasetValueBeanOfCbean(cbean);
                if(dsvbeanTmp!=null) lstResults.add(dsvbeanTmp);
            }
        }
        return lstResults;
    }
    
    public boolean isExistDependentDataset(String dsvalueid)
    {
        if(lstDatasetBeans==null) return false;
        if(dsvalueid==null||dsvalueid.trim().equals("")) dsvalueid=Consts.DEFAULT_KEY;
        for(ReportDataSetBean dsbeanTmp:lstDatasetBeans)
        {
            if(dsbeanTmp.isDependentDatasetValue(dsvalueid)) return true;
        }
        return false;
    }
    
    public List<String> getLstConditionFromUrlNames()
    {
        if(lstConditionFromRequestNames==null&&lstConditions!=null&&lstConditions.size()>0)
        {
            List<String> lstConditionFromRequestNamesTmp=new ArrayList<String>();
            for(ConditionBean cbeanTmp:lstConditions)
            {
                if(cbeanTmp==null||cbeanTmp.isConstant()) continue;
                if(cbeanTmp.isConditionValueFromUrl()) lstConditionFromRequestNamesTmp.add(cbeanTmp.getName());
            }
            this.lstConditionFromRequestNames=lstConditionFromRequestNamesTmp;
        }
        return lstConditionFromRequestNames;
    }

    public void setLstConditionFromRequestNames(List<String> lstConditionFromRequestNames)
    {
        this.lstConditionFromRequestNames=lstConditionFromRequestNames;
    }

    public String getTitlecolumndatasetid()
    {
        return titlecolumndatasetid;
    }

    public void setTitlecolumndatasetid(String titlecolumndatasetid)
    {
        this.titlecolumndatasetid=titlecolumndatasetid;
    }

    public ConditionBean getConditionBeanByName(String name)
    {
        if(name==null||name.trim().equals("")) return null;
        if(this.lstConditions==null||this.lstConditions.size()==0) return null;
        if(this.mConditions==null)
        {//还没有初始化此容器
            Map<String,ConditionBean> mConditionsTmp=new HashMap<String,ConditionBean>();
            for(ConditionBean cbTmp:lstConditions)
            {
                mConditionsTmp.put(cbTmp.getName(),cbTmp);
            }
            this.mConditions=mConditionsTmp;
        }
        return this.mConditions.get(name);
    }

    public void initConditionValues(ReportRequest rrequest)
    {
        if(this.lstConditions==null||this.lstConditions.size()==0) return;
        Map<String,String> mConditionValues=new HashMap<String,String>();
        for(ConditionBean cbean:lstConditions)
        {
            cbean.initConditionValueByInitMethod(rrequest,mConditionValues);
        }
        for(ConditionBean cbean:lstConditions)
        {
            cbean.validateConditionValue(rrequest,mConditionValues);
        }
    }
    
    public boolean isExistConditionWithInputbox(ReportRequest rrequest)
    {
        if(this.lstConditions==null||this.lstConditions.size()==0) return false;
        for(ConditionBean cbeanTmp:this.lstConditions)
        {
            if(!cbeanTmp.isConditionWithInputbox()) continue;
            if(rrequest!=null
                    &&!rrequest.checkPermission(this.getReportBean().getId(),Consts.SEARCH_PART,cbeanTmp.getName(),Consts.PERMISSION_TYPE_DISPLAY))
                continue;
            return true;
        }
        return false;
    }
    
    public List<ConditionBean> getLstDisplayConditions(ReportRequest rrequest)
    {
        if(this.lstConditions==null||this.lstConditions.size()==0) return null;
        List<ConditionBean> lstConditionsResult=new ArrayList<ConditionBean>();
        for(ConditionBean cbeanTmp:this.lstConditions)
        {
            if(!cbeanTmp.isConditionWithInputbox()) continue;
            if(rrequest!=null
                    &&!rrequest.checkPermission(this.getReportBean().getId(),Consts.SEARCH_PART,cbeanTmp.getName(),Consts.PERMISSION_TYPE_DISPLAY))
                continue;
            lstConditionsResult.add(cbeanTmp);
        }
        return lstConditions;
    }
    
    public void afterSqlLoad()
    {
        if(lstDatasetBeans==null) return;
        for(ReportDataSetBean dsbeanTmp:lstDatasetBeans)
        {
            dsbeanTmp.afterSqlLoad();
        }
    }
    
    public void doPostLoad()
    {
        ReportBean rbean=this.getReportBean();
        if(this.lstConditions!=null)
        {
            List<String> lstTmp=new ArrayList<String>();
            for(ConditionBean cbTmp:lstConditions)
            {
                if(cbTmp==null||cbTmp.getName()==null) continue;
                if(lstTmp.contains(cbTmp.getName()))
                {
                    throw new WabacusConfigLoadingException("报表 "+rbean.getPath()+"配置的查询条件name:"+cbTmp.getName()+"存在重复，必须确保唯一");
                }
                lstTmp.add(cbTmp.getName());
                if(cbTmp.getInputbox() instanceof AbsSelectBox)
                {
                    ((AbsSelectBox)cbTmp.getInputbox()).processParentInputBox();
                }
            }
            for(ConditionBean cbTmp:lstConditions)
            {
                if(cbTmp==null||cbTmp.getName()==null) continue;
                cbTmp.doPostLoad();
            }
        }
        if(lstDatasetBeans!=null)
        {
            List<String> lstGroupids=new ArrayList<String>();
            Map<String,List<ReportDataSetBean>> mReportDatasetGroupBeans=new HashMap<String,List<ReportDataSetBean>>();
            List<ReportDataSetBean> lstDatasetGroupBeansTmp;
            for(ReportDataSetBean dsbeanTmp:this.lstDatasetBeans)
            {
                for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
                {
                    dsvbeanTmp.doPostLoad();
                }
                lstDatasetGroupBeansTmp=mReportDatasetGroupBeans.get(dsbeanTmp.getGroupid());
                if(lstDatasetGroupBeansTmp==null)
                {
                    lstDatasetGroupBeansTmp=new ArrayList<ReportDataSetBean>();
                    mReportDatasetGroupBeans.put(dsbeanTmp.getGroupid(),lstDatasetGroupBeansTmp);
                    lstGroupids.add(dsbeanTmp.getGroupid());
                }
                lstDatasetGroupBeansTmp.add(dsbeanTmp);
            }
            this.lstDatasetGroupBeans=new ArrayList<List<ReportDataSetBean>>();
            for(String groupidTmp:lstGroupids)
            {
                lstDatasetGroupBeans.add(mReportDatasetGroupBeans.get(groupidTmp));
            }
            if(this.isHorizontalDataset())
            {
                parseHorizontalDatasetConfig(rbean,lstGroupids,mReportDatasetGroupBeans);
                List<Integer> lstPagesize=new ArrayList<Integer>();
                lstPagesize.add(-1);//横向数据信报表强制不分页显示
                rbean.setLstPagesize(lstPagesize);
            }
        }
    }

    private void parseHorizontalDatasetConfig(ReportBean rbean,List<String> lstGroupids,Map<String,List<ReportDataSetBean>> mReportDatasetGroupBeans)
    {
        if(!Config.getInstance().getReportType(rbean.getType()).isSupportHorizontalDataset(rbean))
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"不支持横向数据集");
        }
        this.hdsTitleLabelCbean=rbean.getDbean().getColBeanByColColumn(this.hdsTitleLabelColumn);
        if(this.hdsTitleLabelCbean==null)
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的为横向数据集，但指定的titlelabelcolumn："+this.hdsTitleLabelColumn+"没有对应的<col/>配置");
        }
        if(this.hdsTitleLabelCbean.getLstDatasetValueids()!=null&&this.hdsTitleLabelCbean.getLstDatasetValueids().size()>0)
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的为横向数据集，它的titlelabelcolumn："+this.hdsTitleLabelColumn
                    +"对应的<col/>不能指定datasetid，因为<sql/>中所有<value/>都要查询出此列的数据");
        }
        if(this.hdsTitleLabelCbean.isControlCol()||this.hdsTitleLabelCbean.getProperty()==null
                ||this.hdsTitleLabelCbean.getProperty().trim().equals("")||this.hdsTitleLabelCbean.isNonFromDbCol()
                ||this.hdsTitleLabelCbean.isNonValueCol())
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的为横向数据集，它的titlelabelcolumn："+this.hdsTitleLabelColumn
                    +"对应的<col/>不是有效的数据列，不能做为横向数据集的标题行");
        }
        this.hdsTitleValueCbean=rbean.getDbean().getColBeanByColColumn(this.hdsTitleValueColumn);
        if(this.hdsTitleValueCbean==null)
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的为横向数据集，但指定的titlevaluecolumn："+this.hdsTitleValueColumn+"没有对应的<col/>配置");
        }
        if(this.hdsTitleValueCbean.isControlCol()||this.hdsTitleValueCbean.getProperty()==null
                ||this.hdsTitleValueCbean.getProperty().trim().equals("")||this.hdsTitleValueCbean.isNonFromDbCol()
                ||this.hdsTitleValueCbean.isNonValueCol())
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的为横向数据集，它的titlevaluecolumn："+this.hdsTitleValueColumn
                    +"对应的<col/>不是有效的数据列，不能做为横向数据集的标题行");
        }
        if(this.hdsTitleValueCbean.getLstDatasetValueids()!=null&&this.hdsTitleValueCbean.getLstDatasetValueids().size()>0)
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的为横向数据集，它的titlevaluecolumn："+this.hdsTitleValueColumn
                    +"对应的<col/>不能指定datasetid，因为<sql/>中所有<value/>都要查询出此列的数据");
        }
        this.hdsOnlyTitleDatasetBean=null;
        this.lstHdsDataDatasetGroupBeans=this.lstDatasetGroupBeans;
        if(this.titlecolumndatasetid!=null&&!this.titlecolumndatasetid.trim().equals(""))
        {//指定了取横向标题列的数据集<dataset/>的ID
            ReportDataSetBean titleDsbean=this.getDatasetBeanById(this.titlecolumndatasetid);
            if(titleDsbean==null)
            {
                throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的为横向数据集，titlecolumndatasetid："+titlecolumndatasetid
                        +"没有对应的<dataset/>配置");
            }
            boolean isOnlyTitleDataset=true;//this.titlecolumndatasetid对应的数据集是否只查询标题数据，不查询数据<col/>的数据
            for(ColBean cbTmp:rbean.getDbean().getLstCols())
            {
                if(cbTmp.isControlCol()||cbTmp.isNonFromDbCol()||cbTmp.isNonValueCol()||cbTmp.isSequenceCol()) continue;
                if(this.hdsTitleLabelColumn.equals(cbTmp.getColumn())||this.hdsTitleValueColumn.equals(cbTmp.getColumn())) continue;
                if(titleDsbean.getDatasetValueBeanOfCbean(cbTmp)!=null)
                {
                    isOnlyTitleDataset=false;
                    break;
                }
            }
            if(isOnlyTitleDataset)
            {//this.titlecolumndatasetid对应的数据集是否只查询标题数据，不查询数据<col/>的数据
                this.hdsOnlyTitleDatasetBean=titleDsbean;
                if(mReportDatasetGroupBeans.get(titleDsbean.getGroupid()).size()>1)
                {
                    throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的为横向数据集，id为："+titleDsbean.getId()
                            +"的<dataset/>配置为只查询标题部分，因此不能和其它数据集通过mergetop属性进行合并，也没必要合并");
                }
                this.lstHdsDataDatasetGroupBeans=new ArrayList<List<ReportDataSetBean>>();
                for(String groupidTmp:lstGroupids)
                {
                    if(!groupidTmp.equals(titleDsbean.getGroupid())) lstHdsDataDatasetGroupBeans.add(mReportDatasetGroupBeans.get(groupidTmp));
                }
                if(this.lstHdsDataDatasetGroupBeans.size()==0||this.lstHdsDataDatasetGroupBeans.get(0).size()==0)
                {
                    throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的为横向数据集，没有配置查询数据列的数据集<dataset/>");
                }
            }
        }
    }
    
    public AbsConfigBean clone(AbsConfigBean parent)
    {
        SqlBean sbeanNew=(SqlBean)super.clone(parent);
        ((ReportBean)parent).setSbean(sbeanNew);
        sbeanNew.setLstConditions(ComponentConfigLoadAssistant.getInstance().cloneLstConditionBeans(sbeanNew,lstConditions));
        if(lstConditionFromRequestNames!=null)
        {
            sbeanNew.setLstConditionFromRequestNames((List<String>)((ArrayList<String>)lstConditionFromRequestNames).clone());
        }
        if(lstDatasetBeans!=null)
        {
            List<ReportDataSetBean> lstDataSetBeansNew=new ArrayList<ReportDataSetBean>();
            Map<String,ReportDataSetBean> mDatasetBeansNew=new HashMap<String,ReportDataSetBean>();
            ReportDataSetBean rdsbeanTmp;
            for(ReportDataSetBean svbeanTmp:lstDatasetBeans)
            {
                rdsbeanTmp=(ReportDataSetBean)svbeanTmp.clone(sbeanNew);
                lstDataSetBeansNew.add(rdsbeanTmp);
                mDatasetBeansNew.put(rdsbeanTmp.getId(),rdsbeanTmp);
            }
            sbeanNew.lstDatasetBeans=lstDataSetBeansNew;
            sbeanNew.mDatasetBeans=mDatasetBeansNew;
        }
        cloneExtendConfig(sbeanNew);
        return sbeanNew;
    }
}
