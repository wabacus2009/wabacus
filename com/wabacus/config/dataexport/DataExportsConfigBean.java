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
package com.wabacus.config.dataexport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.Config;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.DataExportButton;
import com.wabacus.util.Consts;
import com.wabacus.util.UniqueArrayList;

public class DataExportsConfigBean implements Cloneable
{
    private String filename;//导出的文件名，这里是配置的本组件所有数据导出默认的文件名，每个<dataexport/>还可以配置自己的filename覆盖这里的配置
    
    private Map<String,String> mDynFilename;
    
    private List<String> lstAutoDataExportTypes;

    private Map<String,AbsDataExportBean> mDataExportBeans;

    private IComponentConfigBean owner;

    public DataExportsConfigBean(IComponentConfigBean owner)
    {
        this.owner=owner;
    }

    public String getFilename(ReportRequest rrequest)
    {
        String dataexporttype=ComponentAssistant.getInstance().getDataExportTypeByShowType(rrequest.getShowtype());
        String realfilename=null;
        if(this.mDataExportBeans!=null&&this.mDataExportBeans.get(dataexporttype)!=null)
        {
            realfilename=this.mDataExportBeans.get(dataexporttype).getFilename(rrequest);
        }
        if(realfilename!=null&&!realfilename.trim().equals("")) return realfilename;//在<dataexport/>中配置了自己的filename
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.filename,this.mDynFilename,"");
    }
    
    public Map<String,AbsDataExportBean> getMDataExportBeans()
    {
        return mDataExportBeans;
    }

    public void setMDataExportBeans(Map<String,AbsDataExportBean> dataExportBeans)
    {
        mDataExportBeans=dataExportBeans;
    }

    public List<String> getLstAutoDataExportTypes()
    {
        return lstAutoDataExportTypes;
    }

    public void setLstAutoDataExportTypes(List<String> lstAutoDataExportTypes)
    {
        if(lstAutoDataExportTypes==null)
        {
            this.lstAutoDataExportTypes=null;
        }else
        {
            List<String> lstTmp=new UniqueArrayList<String>();
            for(String typeTmp:lstAutoDataExportTypes)
            {
                typeTmp=typeTmp.toLowerCase().trim();
                if(typeTmp.equals("")) continue;
                if(typeTmp.equals(Consts.DATAEXPORT_NONE))
                {//如果配置了none，则不提供任何数据导出功能，即使还配置了其它导出类型
                    lstTmp=null;
                    break;
                }else if(!Consts.lstDataExportTypes.contains(typeTmp))
                {
                    throw new WabacusConfigLoadingException("加载组件"+this.getOwner().getPath()+"失败，无效的数据导出类型："+typeTmp);
                }
                lstTmp.add(typeTmp);
            }
            if(lstTmp!=null&&lstTmp.size()==0) lstTmp=null;
            this.lstAutoDataExportTypes=lstTmp;
        }
    }

    public IComponentConfigBean getOwner()
    {
        return owner;
    }

    public void setOwner(IComponentConfigBean owner)
    {
        this.owner=owner;
    }

    public List<String> getLstIncludeApplicationids(String dataexporttype)
    {
        if(this.mDataExportBeans==null||!this.mDataExportBeans.containsKey(dataexporttype)) return null;
        return this.mDataExportBeans.get(dataexporttype).getLstIncludeApplicationids();
    }
    
    public List<String> getLstIncludeApplicationids(int showtype)
    {
        return getLstIncludeApplicationids(ComponentAssistant.getInstance().getDataExportTypeByShowType(showtype));
    }
    
    public String getIncludeApplicationids(String dataexporttype)
    {
        if(this.mDataExportBeans==null||!this.mDataExportBeans.containsKey(dataexporttype)) return null;
        return this.mDataExportBeans.get(dataexporttype).getIncludeApplicationids();
    }
    
    public String getIncludeApplicationids(int showtype)
    {
        return getIncludeApplicationids(ComponentAssistant.getInstance().getDataExportTypeByShowType(showtype));
    }

    public AbsDataExportBean getDataExportBean(String dataexporttype)
    {
        if(this.mDataExportBeans==null) return null;
        return this.mDataExportBeans.get(dataexporttype);
    }
    
    public AbsDataExportBean getDataExportBean(int showtype)
    {
        return getDataExportBean(ComponentAssistant.getInstance().getDataExportTypeByShowType(showtype));
    }
    
    public void loadConfig(XmlElementBean eleDataExports)
    {
        String filename=eleDataExports.attributeValue("filename");
        if(filename!=null)
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(filename);
            this.filename=(String)objArr[0];
            this.mDynFilename=(Map<String,String>)objArr[1];
        }
        List<XmlElementBean> lstEleChildren=eleDataExports.getLstChildElements();
        if(lstEleChildren==null||lstEleChildren.size()==0) return;
        mDataExportBeans=new HashMap<String,AbsDataExportBean>();
        AbsDataExportBean childDataExportBean;
        for(XmlElementBean eleChildTmp:lstEleChildren)
        {
            String type=eleChildTmp.attributeValue("type");
            type=type==null?"":type.toLowerCase().trim();
            if(type.equals(""))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的数据导出<dataexports/>失败，必须指定<dataexport/>的type属性");
            }
            if(!Consts.lstDataExportTypes.contains(type))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的数据导出<dataexports/>失败，<dataexport/>配置的type属性"+type+"不支持");
            }
            if(mDataExportBeans.containsKey(type))
            {
                throw new WabacusConfigLoadingException("加载组件"+this.owner.getPath()+"的数据导出<dataexports/>失败，<dataexport/>配置的type属性"+type+"存在重复");
            }
            childDataExportBean=createDataExportBean(type);
            childDataExportBean.loadConfig(eleChildTmp);
            mDataExportBeans.put(type,childDataExportBean);
        }
        if(mDataExportBeans.size()==0) mDataExportBeans=null;
    }

    private AbsDataExportBean createDataExportBean(String type)
    {
        if(type==null||type.trim().equals("")||!Consts.lstDataExportTypes.contains(type)) return null;
        if(type.equals(Consts.DATAEXPORT_PDF)) return new PDFExportBean(owner,type);
        if(type.equals(Consts.DATAEXPORT_PLAINEXCEL)) return new PlainExcelExportBean(owner,type);
        return new WordRichExcelExportBean(owner,type);
    }

    public void doPostLoad()
    {
        checkedAndAddButtons();
        if(mDataExportBeans==null) return;
        for(Entry<String,AbsDataExportBean> entryTmp:this.mDataExportBeans.entrySet())
        {
            entryTmp.getValue().doPostLoad();
        }
    }

    private void checkedAndAddButtons()
    {
        if(lstAutoDataExportTypes==null||lstAutoDataExportTypes.size()==0) return;
        List<AbsButtonType> lstDataExportButtons=null;
        for(String dataexportTypeTmp:lstAutoDataExportTypes)
        {
            lstDataExportButtons=null;
            if(this.owner.getButtonsBean()!=null) lstDataExportButtons=this.owner.getButtonsBean().getLstDataExportTypeButtons(dataexportTypeTmp);
            if(!isHasCertainTypeDataExportButton(lstDataExportButtons))
            {//此报表没有配置这种类型的数据导出按钮（注意这里不包括那些在<button/>标签内容中指定了自己导出应用ID的按钮）
                AbsButtonType buttonObj=Config.getInstance().getResourceButton(null,this.getOwner(),
                        Consts.M_DATAEXPORT_DEFAULTBUTTONS.get(dataexportTypeTmp),DataExportButton.class);
                buttonObj.setDefaultNameIfNoName();
                if(this.owner instanceof AbsContainerConfigBean)
                {
                    buttonObj.setPosition("top");//对于容器，默认位置在顶部
                }
                ComponentConfigLoadManager.addButtonToPositions(this.getOwner(),buttonObj);
            }
        }
        lstAutoDataExportTypes=null;
    }

    private boolean isHasCertainTypeDataExportButton(List<AbsButtonType> lstDataExportButtons)
    {
        if(lstDataExportButtons==null||lstDataExportButtons.size()==0) return false;
        DataExportButton debtnTmp;
        for(AbsButtonType buttonObjTmp:lstDataExportButtons)
        {
            debtnTmp=(DataExportButton)buttonObjTmp;
            if(!debtnTmp.isExportBySpecifyApplicationids()) return true;
        }
        return false;
    }

    public DataExportsConfigBean clone(IComponentConfigBean owner)
    {
        try
        {
            DataExportsConfigBean newBean=(DataExportsConfigBean)super.clone();
            newBean.setOwner(owner);
            if(lstAutoDataExportTypes!=null) newBean.setLstAutoDataExportTypes((List<String>)((ArrayList<String>)lstAutoDataExportTypes).clone());
            if(mDataExportBeans!=null)
            {
                Map<String,AbsDataExportBean> mDataExportBeansNew=new HashMap<String,AbsDataExportBean>();
                for(Entry<String,AbsDataExportBean> entryTmp:this.mDataExportBeans.entrySet())
                {
                    mDataExportBeansNew.put(entryTmp.getKey(),entryTmp.getValue().clone(owner));
                }
                newBean.setMDataExportBeans(mDataExportBeansNew);
            }
            return newBean;
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone组件"+this.owner.getPath()+"的数据导出对象失败",e);
        }
    }
}
