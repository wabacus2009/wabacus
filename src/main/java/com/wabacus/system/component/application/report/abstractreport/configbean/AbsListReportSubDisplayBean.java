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
package com.wabacus.system.component.application.report.abstractreport.configbean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javassist.ClassPool;
import javassist.CtClass;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.FormatBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ClassPoolAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.statistic.StatisticItemBean;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class AbsListReportSubDisplayBean extends AbsExtendConfigBean
{
    public final static int SUBROW_DISPLAYTYPE_REPORT=1;
    
    public final static int SUBROW_DISPLAYTYPE_PAGE=2;
    
    public final static int SUBROW_DISPLAYTYPE_PAGEREPORT=3;
    
    public final static int SUBROW_POSITION_TOP=1;
    
    public final static int SUBROW_POSITION_BOTTOM=2;//只在报表底部显示一行
    
    public final static int SUBROW_POSITION_BOTH=3;
    
    private Class pojoclass;

    private FormatBean fbean;

    private List<StatisticItemBean> lstStatitemBeans;

    private List<AbsListReportSubDisplayRowBean> lstSubDisplayRowBeans;

    private Map<String,AbsListReportRowGroupSubDisplayRowBean> mRowGroupSubDisplayRowBeans;//为行分组（包括普通分组以及树形分组）配置的行统计
    
    private Map<String,Method> mPropertiesAndGetMethods;
    
    public AbsListReportSubDisplayBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public Class getPojoclass()
    {
        return pojoclass;
    }

    public Object getPojoObject()
    {
        try
        {
            return this.pojoclass.newInstance();
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("实例化报表"+getOwner().getReportBean().getPath()+"存放统计数据的POJO对象失败",e);
        }
    }

    public Map<String,AbsListReportRowGroupSubDisplayRowBean> getMRowGroupSubDisplayRowBeans()
    {
        return mRowGroupSubDisplayRowBeans;
    }

    public void setMRowGroupSubDisplayRowBeans(Map<String,AbsListReportRowGroupSubDisplayRowBean> rowGroupSubDisplayRowBeans)
    {
        mRowGroupSubDisplayRowBeans=rowGroupSubDisplayRowBeans;
    }

    public void setPojoclass(Class pojoclass)
    {
        this.pojoclass=pojoclass;
    }

    public List<StatisticItemBean> getLstStatitemBeans()
    {
        return lstStatitemBeans;
    }

    public void setLstStatitemBeans(List<StatisticItemBean> lstStatitemBeans)
    {
        this.lstStatitemBeans=lstStatitemBeans;
    }

    public List<AbsListReportSubDisplayRowBean> getLstSubDisplayRowBeans()
    {
        return lstSubDisplayRowBeans;
    }

    public void setLstSubDisplayRowBeans(List<AbsListReportSubDisplayRowBean> lstSubDisplayRowBeans)
    {
        this.lstSubDisplayRowBeans=lstSubDisplayRowBeans;
    }

    public boolean isAllDisplayPerpageDataRows()
    {
        if(lstSubDisplayRowBeans==null||lstSubDisplayRowBeans.size()==0) return false;
        for(AbsListReportSubDisplayRowBean rowbeanTmp:lstSubDisplayRowBeans)
        {
            if(rowbeanTmp.getDisplaytype()!=SUBROW_DISPLAYTYPE_PAGE) return false;
        }
        return true;
    }

    public boolean isAllDisplayWholeReportDataRows()
    {
        if(lstSubDisplayRowBeans==null||lstSubDisplayRowBeans.size()==0) return false;
        for(AbsListReportSubDisplayRowBean rowbeanTmp:lstSubDisplayRowBeans)
        {
            if(rowbeanTmp.getDisplaytype()!=SUBROW_DISPLAYTYPE_REPORT) return false;
        }
        return true;
    }
    
    public FormatBean getFbean()
    {
        return fbean;
    }

    public void setFbean(FormatBean fbean)
    {
        this.fbean=fbean;
    }

    public Map<String,Method> getMPropertiesAndGetMethods()
    {
        return mPropertiesAndGetMethods;
    }

    public void addRowGroupSubDisplayRowBean(AbsListReportRowGroupSubDisplayRowBean srgbean)
    {
        if(srgbean==null) return;
        if(srgbean.getRowgroupcolumn()==null||srgbean.getRowgroupcolumn().trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+getOwner().getReportBean().getPath()
                    +"失败，<rowgroup-subrow>或<treerowgroup-subrow>的column属性值为空");
        }
        if(mRowGroupSubDisplayRowBeans==null)
        {
            mRowGroupSubDisplayRowBeans=new HashMap<String,AbsListReportRowGroupSubDisplayRowBean>();
        }
        mRowGroupSubDisplayRowBeans.put(srgbean.getRowgroupcolumn(),srgbean);
    }

    public void doPostLoad()
    {
        ReportBean rbean=this.getOwner().getReportBean();
        buildSubDisplayPojoClass(rbean);
        initGetSetMethod(rbean);//初始化pojo的get/set方法，以便运行时可以直接使用
        if(hasManyScolsInRow())
        {
            rbean.getDbean().setPageColselect(false);
            rbean.getDbean().setDataexportColselect(false);
            if(!rbean.getDbean().isAllColDisplaytypesEquals())
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，当前报表是行分组显示报表且统计信息显示在多个<scol/>中，因此必须保证所有列在导出文件和页面上的displaytype是一致的");
            }
        }
        AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)rbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(this.lstSubDisplayRowBeans!=null)
        {
            for(AbsListReportSubDisplayRowBean sRowBeanTmp:this.lstSubDisplayRowBeans)
            {
                validateSubDisplayColsConfig(sRowBeanTmp.getLstSubColBeans(),alrdbean);
            }
        }
        if(mRowGroupSubDisplayRowBeans!=null&&mRowGroupSubDisplayRowBeans.size()>0)
        {
            if(alrdbean.getRowgrouptype()!=1&&alrdbean.getRowgrouptype()!=2)
            {
                throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"失败，当前报表不是行分组显示报表，不能配置行分组统计功能");
            }
            for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entry:mRowGroupSubDisplayRowBeans.entrySet())
            {
                if(entry==null) continue;
                entry.getValue().validateRowGroupSubDisplayColsConfig(this,(DisplayBean)alrdbean.getOwner());
            }
        }
        if(this.lstStatitemBeans!=null&&this.lstStatitemBeans.size()>0)
        {
            for(StatisticItemBean statitemBeanTmp:this.lstStatitemBeans)
            {
                getStatitemDatasetValueBean(statitemBeanTmp).getProvider().addStaticItemBean(statitemBeanTmp);
            }
            for(ReportDataSetBean dsbeanTmp:rbean.getSbean().getLstDatasetBeans())
            {
                for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
                {
                    dsvbeanTmp.getProvider().doPostLoadStatistic();
                }
            }
//            {//配置了分组统计，则创建每个分组的统计数据的SQL语句后面的group by  having短语，以便运行时可以直接使用
//                for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entryTmp:this.mRowGroupSubDisplayRowBeans.entrySet())
        }
    }

    private void buildSubDisplayPojoClass(ReportBean reportbean)
    {
        List<String> lstProperties=new ArrayList<String>();
        List<IDataType> lstDataTypes=new ArrayList<IDataType>();
        if(this.lstStatitemBeans!=null&&this.lstStatitemBeans.size()>0)
        {//有统计数据
            for(StatisticItemBean statItemBeanTmp:this.lstStatitemBeans)
            {
                if(lstProperties.contains(statItemBeanTmp.getProperty()))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计配置失败，property属性"+statItemBeanTmp.getProperty()+"存在重复");
                }
                if(statItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                        ||statItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_REPORT)
                {
                    lstProperties.add(statItemBeanTmp.getProperty());
                    lstDataTypes.add(statItemBeanTmp.getDatatypeObj());
                }
                if(statItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_ALL
                        ||statItemBeanTmp.getStatiscope()==StatisticItemBean.STATSTIC_SCOPE_PAGE)
                {
                    lstProperties.add("page_"+statItemBeanTmp.getProperty());
                    lstDataTypes.add(statItemBeanTmp.getDatatypeObj());
                }
            }
        }
        if(lstSubDisplayRowBeans!=null&&lstSubDisplayRowBeans.size()>0)
        {
            for(AbsListReportSubDisplayRowBean sRowBeanTmp:this.lstSubDisplayRowBeans)
            {//为每行统计记录上的统计列增加一个成员变量
                getSColPropertiesAndTypes(reportbean,lstProperties,lstDataTypes,sRowBeanTmp.getLstSubColBeans());
            }
        }
        if(this.mRowGroupSubDisplayRowBeans!=null&&this.mRowGroupSubDisplayRowBeans.size()>0)
        {
            for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entryTmp:this.mRowGroupSubDisplayRowBeans.entrySet())
            {
                AbsListReportRowGroupSubDisplayRowBean srgbean=entryTmp.getValue();
                getSColPropertiesAndTypes(reportbean,lstProperties,lstDataTypes,srgbean.getLstSubColBeans());
            }
            AbsListReportDisplayBean alrdbean=(AbsListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrdbean.getLstRowgroupColsColumn()!=null)
            {
                for(String groupcol:alrdbean.getLstRowgroupColsColumn())
                {
                    if(groupcol==null||groupcol.trim().equals("")) continue;
                    if(lstProperties.contains(groupcol))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计配置失败，不能配置<subcol/>的property属性为"+groupcol
                                +"，它已经是分组列的column值");
                    }
                    lstProperties.add(groupcol);
                    lstDataTypes.add(Config.getInstance().getDataTypeByClass(VarcharType.class));
                }
            }
        }
        ClassPool pool=ClassPoolAssistant.getInstance().createClassPool();
        String classname=Consts.BASE_PACKAGE_NAME+".Pojo_"+reportbean.getPageBean().getId()+reportbean.getId()+"_subdisplay";
        CtClass cclass=pool.makeClass(classname);
        ClassPoolAssistant.getInstance().addImportPackages(pool,fbean.getLstImports());
        String propertyTmp;
        IDataType dataTypeTmp;
        try
        {
            for(int i=0;i<lstProperties.size();i++)
            {
                propertyTmp=lstProperties.get(i);
                dataTypeTmp=lstDataTypes.get(i);
                if(dataTypeTmp==null) dataTypeTmp=Config.getInstance().getDataTypeByClass(VarcharType.class);
                ClassPoolAssistant.getInstance().addFieldAndGetSetMethod(cclass,propertyTmp,dataTypeTmp.getCreatedClass(pool));
            }
            ClassPoolAssistant.getInstance().addMethod(
                    cclass,
                    "public void format("+ReportRequest.class.getName()+"  rrequest,"+ReportBean.class.getName()+" rbean,"+String.class.getName()
                            +" type){"+fbean.getFormatContent()+" \n}");//type参数用于表示当前是在统计整个报表，还是在统计某个分组，统计整个报表时，type为空，统计某个分组时，这里传入相应<rowgroup/>的column属性
            this.pojoclass=ConfigLoadManager.currentDynClassLoader.loadClass(classname,cclass.toBytecode());
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("生成报表"+reportbean.getPath()+"的存放辅助显示信息的POJO类失败",e);
        }
        cclass.detach();
        pool.clearImportedPackages();
        pool=null;
    }

    private void getSColPropertiesAndTypes(ReportBean reportbean,List<String> lstProperties,List<IDataType> lstDataTypes,
            List<AbsListReportSubDisplayColBean> lstSColBeans)
    {
        if(lstSColBeans!=null&&lstSColBeans.size()>0)
        {
            for(AbsListReportSubDisplayColBean sColBeanTmp:lstSColBeans)
            {
                if(lstProperties.contains(sColBeanTmp.getProperty()))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计配置失败，property属性"+sColBeanTmp.getProperty()+"存在重复");
                }
                lstProperties.add(sColBeanTmp.getProperty());
                lstDataTypes.add(Config.getInstance().getDataTypeByClass(VarcharType.class));
            }
        }
    }

    private void initGetSetMethod(ReportBean reportbean)
    {
        try
        {
            mPropertiesAndGetMethods=new HashMap<String,Method>();
            if(this.lstStatitemBeans!=null&&this.lstStatitemBeans.size()>0)
            {
                for(StatisticItemBean statItemBeanTmp:this.lstStatitemBeans)
                {
                    String property=statItemBeanTmp.getProperty();
                    int statiscopeType=statItemBeanTmp.getStatiscope();
                    if(statiscopeType==StatisticItemBean.STATSTIC_SCOPE_ALL||statiscopeType==StatisticItemBean.STATSTIC_SCOPE_REPORT)
                    {
                        statItemBeanTmp.setSetMethod(this.pojoclass.getMethod("set"+property.substring(0,1).toUpperCase()+property.substring(1),
                                new Class[] { statItemBeanTmp.getDatatypeObj().getJavaTypeClass() }));
                        mPropertiesAndGetMethods.put(property,pojoclass.getMethod("get"+property.substring(0,1).toUpperCase()+property.substring(1),
                                new Class[] {}));
                    }
                    if(statiscopeType==StatisticItemBean.STATSTIC_SCOPE_ALL||statiscopeType==StatisticItemBean.STATSTIC_SCOPE_PAGE)
                    {
                        statItemBeanTmp.setPageStatiSetMethod(this.pojoclass.getMethod("setPage_"+property,new Class[] { statItemBeanTmp
                                .getDatatypeObj().getJavaTypeClass() }));
                        mPropertiesAndGetMethods.put("page_"+property,pojoclass.getMethod("getPage_"+property,new Class[] {}));
                    }
                }
            }
            if(this.lstSubDisplayRowBeans!=null&&this.lstSubDisplayRowBeans.size()>0)
            {
                for(AbsListReportSubDisplayRowBean sRowBeanTmp:this.lstSubDisplayRowBeans)
                {//为每行统计记录上的统计列对应的成员变量增加get方法
                    initSColGetMethod(reportbean,sRowBeanTmp.getLstSubColBeans(),mPropertiesAndGetMethods);
                }
            }
            if(this.mRowGroupSubDisplayRowBeans!=null&&this.mRowGroupSubDisplayRowBeans.size()>0)
            {
                for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entryTmp:this.mRowGroupSubDisplayRowBeans.entrySet())
                {
                    AbsListReportRowGroupSubDisplayRowBean srgbean=entryTmp.getValue();
                    initSColGetMethod(reportbean,srgbean.getLstSubColBeans(),mPropertiesAndGetMethods);
                }
            }
        }catch(Exception e)
        {
            throw new WabacusConfigLoadingException("初始化报表"+reportbean.getPath()+"统计项的get/set方法失败",e);
        }
    }

    private void initSColGetMethod(ReportBean reportbean,List<AbsListReportSubDisplayColBean> lstSColBeans,Map<String,Method> mPropertiesAndGetMethods)
            throws Exception
    {
        if(lstSColBeans!=null&&lstSColBeans.size()>0)
        {
            Method getMethodObjTmp;
            for(AbsListReportSubDisplayColBean sColBeanTmp:lstSColBeans)
            {
                String property=sColBeanTmp.getProperty();
                String getMethodName="get"+property.substring(0,1).toUpperCase()+property.substring(1);
                getMethodObjTmp=pojoclass.getMethod(getMethodName,new Class[] {});
                sColBeanTmp.setGetMethod(getMethodObjTmp);
                mPropertiesAndGetMethods.put(property,getMethodObjTmp);
            }
        }
    }

    private boolean hasManyScolsInRow()
    {
        if(this.lstSubDisplayRowBeans!=null)
        {
            for(AbsListReportSubDisplayRowBean sRowBeanTmp:this.lstSubDisplayRowBeans)
            {
                if(sRowBeanTmp.getLstSubColBeans()!=null&&sRowBeanTmp.getLstSubColBeans().size()>1)
                {
                    return true;
                }
            }
        }
        if(mRowGroupSubDisplayRowBeans!=null&&mRowGroupSubDisplayRowBeans.size()>0)
        {
            for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entry:mRowGroupSubDisplayRowBeans.entrySet())
            {
                if(entry==null) continue;
                if(entry.getValue().getLstSubColBeans()!=null&&entry.getValue().getLstSubColBeans().size()>1)
                {
                    //                    bolHasManyScolsInRow=true;
                    return true;
                }
            }
        }
        return false;
    }

    private ReportDataSetValueBean getStatitemDatasetValueBean(StatisticItemBean statitemBean)
    {
        String datasetid=statitemBean.getDatasetid();
        ReportBean reportbean=this.getOwner().getReportBean();
        SqlBean sbean=reportbean.getSbean();
        ReportDataSetValueBean dsvbean=null;
        if(datasetid==null||datasetid.trim().equals(""))
        {
            if(sbean.isMultiDataSetCols()||sbean.isMultiDatasetRows(false))
            {
                throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+statitemBean.getProperty()
                        +"失败，此报表配置了多个数据集，因此必须为统计项配置的dataset指定统计的数据集");
            }
            dsvbean=sbean.getLstDatasetBeans().get(0).getLstValueBeans().get(0);
        }else
        {
            int idx=datasetid.indexOf(".");
            if(idx>0)
            {
                String valueid=datasetid.substring(idx+1).trim();
                datasetid=datasetid.substring(0,idx).trim();
                ReportDataSetBean dsbean=sbean.getDatasetBeanById(datasetid);
                if(dsbean==null)
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+statitemBean.getProperty()+"失败，没有取到id为"+datasetid
                            +"的<dataset/>配置");
                }
                dsvbean=dsbean.getDatasetValueBeanById(valueid);
            }else
            {
                if(reportbean.getSbean().isMultiDatasetRows(false))
                {
                    throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+statitemBean.getProperty()
                            +"失败，此报表配置了多个<dataset/>，因此在指定统计项的dataset时，不能只指定<value/>的id，还需指定<dataset/>的id");
                }
                dsvbean=sbean.getLstDatasetBeans().get(0).getDatasetValueBeanById(datasetid);
            }
        }
        if(dsvbean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"的统计项"+statitemBean.getProperty()+"失败，没有取到此统计项的dataset属性"
                    +statitemBean.getDatasetid()+"指定的数据集对象");
        }
        return dsvbean;
    }

    public void validateSubDisplayColsConfig(List<AbsListReportSubDisplayColBean> lstSubDisplayColBeans,AbsListReportDisplayBean alrdbean)
    {
        if(lstSubDisplayColBeans==null||lstSubDisplayColBeans.size()==0) return;
        if(lstSubDisplayColBeans.size()==1)
        {
            String valuestyleproperty=lstSubDisplayColBeans.get(0).getValuestyleproperty(null,true);
            String colspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
            if(colspan!=null&&!colspan.trim().equals(""))
            {//在valuestyleproperty中配置了colspan，则去掉里面的colspan，运行时会动态计算它的colspan数
                lstSubDisplayColBeans.get(0).setValuestyleproperty(Tools.removePropertyValueByName("colspan",valuestyleproperty),true);
            }
        }else
        {
            calColSpanAndStartColIdx(lstSubDisplayColBeans,alrdbean,alrdbean.getDefaultPageColPositionBean().getTotalColCount());
        }
    }

    public void calColSpanAndStartColIdx(List<AbsListReportSubDisplayColBean> lstSubDisplayColBeans,AbsListReportDisplayBean alrdbean,int colcount)
    {
        int deltaCount=0;
        if(alrdbean.getRowGroupColsNum()>0&&alrdbean.getRowgrouptype()==2)
        {
            deltaCount=alrdbean.getRowGroupColsNum()-1;
        }
        if(lstSubDisplayColBeans.size()==1)
        {
            lstSubDisplayColBeans.get(0).setPlainexcel_startcolidx(0);
            String valuestyleproperty=lstSubDisplayColBeans.get(0).getValuestyleproperty(null,true);
            String colspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
            if(colspan==null||colspan.trim().equals(""))
            {
                valuestyleproperty=valuestyleproperty+" colspan=\""+colcount+"\"";
                //lstSubDisplayColBeans.get(0).setValuestyleproperty(valuestyleproperty,true);
            }else
            {
                int icolspan=Integer.parseInt(colspan);
                if(icolspan!=colcount)
                {
                    throw new WabacusConfigLoadingException("加载报表"+alrdbean.getOwner().getReportBean().getPath()+"失败，配置的<scol/>的的colspan总数"+icolspan
                            +"与要显示的总列数"+colcount+"不相等");
                }
            }
            lstSubDisplayColBeans.get(0).setPlainexcel_colspan(colcount+deltaCount);
        }else
        {
            int total_colspan=0;
            boolean isFirstCol=true;
            for(AbsListReportSubDisplayColBean scbeanTmp:lstSubDisplayColBeans)
            {
                scbeanTmp.setPlainexcel_startcolidx(total_colspan);
                String valuestyleproperty=scbeanTmp.getValuestyleproperty(null,true);
                String colspan=Tools.getPropertyValueByName("colspan",valuestyleproperty,true);
                int icolspan=1;
                if(colspan!=null&&!colspan.trim().equals(""))
                {
                    icolspan=Integer.parseInt(colspan);
                    if(icolspan<=0) icolspan=1;
                }
                total_colspan+=icolspan;
                if(isFirstCol)
                {
                    icolspan=icolspan+deltaCount;
                    isFirstCol=false;
                }
                scbeanTmp.setPlainexcel_colspan(icolspan);
            }
            if(total_colspan!=colcount)
            {
                throw new WabacusConfigLoadingException("加载报表"+alrdbean.getOwner().getReportBean().getPath()+"失败，配置的所有<scol/>的的colspan总数"
                        +total_colspan+"与要显示的总列数"+colcount+"不相等");
            }
        }
    }

    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        AbsListReportSubDisplayBean newBean=(AbsListReportSubDisplayBean)super.clone(owner);
        if(this.lstSubDisplayRowBeans!=null)
        {
            List<AbsListReportSubDisplayRowBean> lstNew=new ArrayList<AbsListReportSubDisplayRowBean>();
            for(AbsListReportSubDisplayRowBean rowbeanTmp:this.lstSubDisplayRowBeans)
            {
                lstNew.add(rowbeanTmp.clone());
            }
            newBean.lstSubDisplayRowBeans=lstNew;
        }
        if(mRowGroupSubDisplayRowBeans!=null)
        {
            Map<String,AbsListReportRowGroupSubDisplayRowBean> mStatiRowGroupBeansTemp=new HashMap<String,AbsListReportRowGroupSubDisplayRowBean>();
            for(Entry<String,AbsListReportRowGroupSubDisplayRowBean> entry:mRowGroupSubDisplayRowBeans.entrySet())
            {
                if(entry==null) continue;
                mStatiRowGroupBeansTemp.put(entry.getKey(),(AbsListReportRowGroupSubDisplayRowBean)entry.getValue().clone());
            }
            newBean.setMRowGroupSubDisplayRowBeans(mStatiRowGroupBeansTemp);
        }
        if(fbean!=null)
        {
            newBean.setFbean((FormatBean)fbean.clone(null));
        }
        if(this.lstStatitemBeans!=null)
        {
            List<StatisticItemBean> lstItemBeansNew=new ArrayList<StatisticItemBean>();
            for(StatisticItemBean itemBeanTmp:this.lstStatitemBeans)
            {
                lstItemBeansNew.add((StatisticItemBean)itemBeanTmp.clone());
            }
            newBean.setLstStatitemBeans(lstItemBeansNew);
        }
        return newBean;
    }
}
