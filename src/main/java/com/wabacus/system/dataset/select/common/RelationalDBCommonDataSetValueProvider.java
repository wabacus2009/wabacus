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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.config.typeprompt.TypePromptColBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.inputbox.autocomplete.AutoCompleteBean;
import com.wabacus.system.inputbox.option.SelectboxOptionBean;
import com.wabacus.util.Tools;

public class RelationalDBCommonDataSetValueProvider extends AbsCommonDataSetValueProvider
{
    protected String value;

    public String getValue()
    {
        return value;
    }

    public List<Map<String,String>> getLstSelectBoxOptions(ReportRequest rrequest,Map<String,String> mParentInputboxValues)
    {
        return null;
    }

    public List<Map<String,String>> getLstTypePromptOptions(ReportRequest rrequest,String txtValue)
    {
        return null;
    }

    public Map<String,String> getAutoCompleteColumnsData(ReportRequest rrequest,Map<String,String> params)
    {
        return null;
    }

    public List<Map<String,String>> getDynamicColGroupDataSet(ReportRequest rrequest)
    {
        return null;
    }

    protected Map<String,String> getMSelectBoxColKeyAndColumns()
    {
        Map<String,String> mColKeyAndColumn=new HashMap<String,String>();
        mColKeyAndColumn.put("label",((SelectboxOptionBean)this.ownerOptionBean).getLabel());
        mColKeyAndColumn.put("value",((SelectboxOptionBean)this.ownerOptionBean).getValue());
        return mColKeyAndColumn;
    }
    
    protected Map<String,String> getMTypePromptColKeyAndColumns()
    {
        TypePromptBean typePromptBean=((TextBox)this.ownerOptionBean.getOwnerInputboxObj()).getTypePromptBean();
        List<TypePromptColBean> lstPColsBean=typePromptBean.getLstPColBeans();
        if(lstPColsBean==null||lstPColsBean.size()==0) return null;
        Map<String,String> mColKeyAndColumn=new HashMap<String,String>();
        for(TypePromptColBean tpcbean:lstPColsBean)
        {
            mColKeyAndColumn.put(tpcbean.getLabel(),tpcbean.getLabel());
            if(tpcbean.getValue()!=null&&!tpcbean.getValue().trim().equals("")&&!tpcbean.getValue().equals(tpcbean.getLabel()))
            {
                mColKeyAndColumn.put(tpcbean.getValue(),tpcbean.getValue());
            }
        }
        return mColKeyAndColumn;
    }
    
    protected List<Map<String,String>> parseOptionsDataSet(Object optionsDataSet,Map<String,String> mColKeyAndColumns,int maxrecordcount)
    {
        List<Map<String,String>> lstResults=new ArrayList<Map<String,String>>();
        int cnt=0;
        if(optionsDataSet instanceof List)
        {
            for(Object itemTmp:(List)optionsDataSet)
            {
                if(itemTmp==null) continue;
                if(!(itemTmp instanceof Map))
                {
                    throw new WabacusRuntimeException("加载报表"+getReportBean().getPath()+"选项数据的拦截器返回的List对象中元素类型不对，必须为Map类型");
                }
                lstResults.add((Map<String,String>)itemTmp);
                if(maxrecordcount>0&&++cnt==maxrecordcount) break;
            }
        }else if(optionsDataSet instanceof ResultSet)
        {
            ResultSet rs=(ResultSet)optionsDataSet;
            Map<String,String> mOptionTmp;
            try
            {
                while(rs.next())
                {
                    mOptionTmp=new HashMap<String,String>();
                    for(Entry<String,String> entryColTmp:mColKeyAndColumns.entrySet())
                    {
                        String valueTmp=rs.getString(entryColTmp.getValue());
                        valueTmp=valueTmp==null?"":valueTmp.trim();
                        mOptionTmp.put(entryColTmp.getKey(),valueTmp);
                    }
                    lstResults.add(mOptionTmp);
                    if(maxrecordcount>0&&++cnt==maxrecordcount) break;
                }
            }catch(SQLException e)
            {
                throw new WabacusRuntimeException("加载报表"+getReportBean().getPath()+"的选项数据失败",e);
            }finally
            {
                try
                {
                    rs.close();
                }catch(SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }else if(optionsDataSet!=null)
        {
            throw new WabacusRuntimeException("加载报表"+getReportBean().getPath()+"的选项数据失败，在加载选项数据的拦截器中返回的对象类型"+optionsDataSet.getClass().getName()
                    +"不合法");
        }
        return lstResults;
    }
    
    protected Map<String,String> parseAutoCompleteDataSet(Object autoCompleteDataSet)
    {
        Map<String,String> mResults;
        if(autoCompleteDataSet instanceof Map)
        {//拦截器直接返回结果集
            mResults=(Map<String,String>)autoCompleteDataSet;
        }else if(autoCompleteDataSet instanceof ResultSet)
        {
            mResults=new HashMap<String,String>();
            ResultSet rs=(ResultSet)autoCompleteDataSet;
            try
            {
                String colValTmp;
                while(rs.next())
                {
                    if(mResults.size()>0)
                    {
                        if(AutoCompleteBean.MULTIPLE_FIRST.equals(this.ownerAutoCompleteBean.getMultiple())) return mResults;
                        if(AutoCompleteBean.MULTIPLE_NONE.equals(this.ownerAutoCompleteBean.getMultiple())) return null;
                    }
                    for(ColBean cbTmp:this.ownerAutoCompleteBean.getLstAutoCompleteColBeans())
                    {
                        colValTmp=rs.getString(cbTmp.getColumn());
                        if(colValTmp==null) colValTmp="";
                        mResults.put(cbTmp.getProperty(),colValTmp);
                    }
                }
            }catch(SQLException e)
            {
                throw new WabacusRuntimeException("获取报表"+this.getReportBean().getPath()+"的自动填充数据失败",e);
            }finally
            {
                try
                {
                    if(rs!=null) rs.close();
                }catch(SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }else
        {
            throw new WabacusRuntimeException("加载报表"+getReportBean().getPath()+"的自动填充数据失败，在加载选项数据的拦截器中返回的对象类型"
                    +autoCompleteDataSet.getClass().getName()+"不合法");
        }
        return mResults;
    }
    
    protected List<Map<String,String>> parseDynamicColGroupDataSet(Object colGroupDataSet)
    {
        if(colGroupDataSet==null) return null;
        List<Map<String,String>> lstResults=null;
        ReportBean rbean=this.getReportBean();
        if(colGroupDataSet instanceof List)
        { 
            lstResults=(List)colGroupDataSet;
            if(lstResults.size()==0) return null;
            if(!(lstResults.get(0) instanceof Map))
            {
                throw new WabacusRuntimeException("从数据库获取报表："+rbean.getPath()+"的动态标题列数据失败，拦截器加载数据前置动作返回的List中没有存放Map类型的对象");
            }
        }else if(colGroupDataSet instanceof ResultSet)
        {
            ResultSet rs=(ResultSet)colGroupDataSet;
            try
            {
                lstResults=new ArrayList<Map<String,String>>();
                Map<String,String> mRowDataTmp;
                while(rs.next())
                {
                    mRowDataTmp=new HashMap<String,String>();
                    this.ownerCrossReportColAndGroupBean.getRealLabelValueFromResultset(rs,mRowDataTmp);
                    lstResults.add(mRowDataTmp);
                }
            }catch(SQLException e)
            {
                throw new WabacusRuntimeException("从数据库获取报表："+rbean.getPath()+"的动态标题列失败",e);
            }finally
            {
                try
                {
                    rs.close();
                }catch(SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }else if(colGroupDataSet!=null)
        {
            throw new WabacusRuntimeException("获取报表："+rbean.getPath()+"的动态标题列失败，在查询交叉统计报表的标题时在拦截器中返回无效的数据类型");
        }
        return lstResults;
    }
    
    public void loadConfig(XmlElementBean eleDatasetAttributeOwnerBean)
    {
        super.loadConfig(eleDatasetAttributeOwnerBean);
        String dataset=eleDatasetAttributeOwnerBean.attributeValue("dataset");//取到配置的dataset
        String[] providerDataset=parseDatsetProvider(dataset);
        if(providerDataset==null||providerDataset.length!=2||Tools.isEmpty(providerDataset[1])) return;
        this.value=providerDataset[1];
    }

}
