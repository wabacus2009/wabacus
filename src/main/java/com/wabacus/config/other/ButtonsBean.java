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
package com.wabacus.config.other;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ComponentAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.DataExportButton;
import com.wabacus.system.buttons.PrintButton;
import com.wabacus.util.Consts;

public class ButtonsBean implements Cloneable
{
    private static Log log=LogFactory.getLog(ReportAssistant.class);
    
    private int buttonspacing=3;

    private String align="right";
    
    private String titleposition;
    
    private IComponentConfigBean ccbean;
    
    private Map<String,List<AbsButtonType>> mAllButtons;
    
//    private boolean isExistReferedButton;//本报表按钮中是否存在被容器引用显示的按钮
    
    public ButtonsBean(IComponentConfigBean ccbean)
    {
        this.ccbean=ccbean;
        mAllButtons=new HashMap<String,List<AbsButtonType>>();
    }

    public IComponentConfigBean getCcbean()
    {
        return ccbean;
    }

    public void setCcbean(IComponentConfigBean ccbean)
    {
        this.ccbean=ccbean;
    }
    public int getButtonspacing()
    {
        return buttonspacing;
    }

    public void setButtonspacing(int buttonspacing)
    {
        this.buttonspacing=buttonspacing;
    }
    
    public Map<String,List<AbsButtonType>> getMAllButtons()
    {
        return mAllButtons;
    }

    public void setMAllButtons(Map<String,List<AbsButtonType>> allButtons)
    {
        mAllButtons=allButtons;
    }
    
    public String getAlign()
    {
        return align;
    }

    public void setAlign(String align)
    {
        align=align==null?"":align.toLowerCase().trim();
        if(!align.equals("left")&&!align.equals("center")&&!align.equals("right"))
            align="right";//默认显示在右边
        this.align=align;
    }
    
//

    public String getTitleposition()
    {
        return titleposition;
    }

    public void setTitleposition(String titleposition)
    {
        this.titleposition=titleposition;
    }

    public AbsButtonType getButtonByName(String name)
    {
        if(mAllButtons==null) return null;
        if(name==null||name.trim().equals("")) return null;
        name=name.trim();
        Iterator<String> itPositions=mAllButtons.keySet().iterator();
        String position;
        while(itPositions.hasNext())
        {
            position=itPositions.next();
            List<AbsButtonType> lstButtons=mAllButtons.get(position);
            if(lstButtons==null) continue;
            AbsButtonType buttonObj;
            for(int i=0;i<lstButtons.size();i++)
            {
                buttonObj=lstButtons.get(i);
                if(buttonObj==null) continue;
                if(name.equals(buttonObj.getName()))
                {
                    return buttonObj;
                }
            }
        }
        return null;
    }

    public AbsButtonType getButtonByNameAndPosition(String name,String position)
    {
        if(mAllButtons==null) return null;
        if(name==null||name.trim().equals("")) return null;
        if(position==null||position.trim().equals("")) position=Consts.OTHER_PART;
        position=position.trim();
        name=name.trim();
        List<AbsButtonType> lstButtons=mAllButtons.get(position);
        if(lstButtons==null||lstButtons.size()==0)
        {
            return null;
        }
        for(int i=0;i<lstButtons.size();i++)
        {
            if(name.equals(lstButtons.get(i).getName()))
            {
                return lstButtons.get(i);
            }
        }
        return null;
    }
    
    public List<AbsButtonType> getButtonsByPosition(String position)
    {
        if(mAllButtons==null) return null;
        if(position==null||position.trim().equals("")) position=Consts.OTHER_PART;
        return mAllButtons.get(position.trim());
    }

    public void addButton(AbsButtonType button,String position)
    {
        if(mAllButtons==null) mAllButtons=new HashMap<String,List<AbsButtonType>>();
        List<AbsButtonType> lstButtons=mAllButtons.get(position);
        if(lstButtons==null)
        {
            lstButtons=new ArrayList<AbsButtonType>();
            mAllButtons.put(position,lstButtons);
        }
        if(button.getName()==null||button.getName().trim().equals(""))
        {
            throw new WabacusConfigLoadingException("添加配置的按钮对象失败，按钮对象必须配置name属性");
        }
        for(int i=0;i<lstButtons.size();i++)
        {
            if(lstButtons.get(i)==null) continue;
            if(button.getName().equals(lstButtons.get(i).getName()))
            {
                throw new WabacusConfigLoadingException("添加配置的按钮对象失败，在位置"+position+"中已经存在name为"
                        +button.getName()+"的按钮对象");
            }
        }
        lstButtons.add(button);
    }

    public int removeButton(String buttonname,String position)
    {
        if(mAllButtons==null) return 0;
        List<AbsButtonType> lstButtons=mAllButtons.get(position);
        if(lstButtons==null) return 0;
        AbsButtonType buttonObj;
        for(int i=0;i<lstButtons.size();i++)
        {
            buttonObj=lstButtons.get(i);
            if(buttonObj==null||buttonObj.getName()==null) continue;
            if(buttonObj.getName().equals(buttonname))
            {
                lstButtons.remove(i);
                return 1;
            }
        }
        return 0;
    }

    public void removeAllCertainTypeButtons(Class buttonType)
    {
        if(mAllButtons==null||buttonType==null) return;
        Iterator<String> itPositions=mAllButtons.keySet().iterator();
        String position;
        while(itPositions.hasNext())
        {
            position=itPositions.next();
            List<AbsButtonType> lstButtons=mAllButtons.get(position);
            if(lstButtons==null) continue;
            AbsButtonType buttonObj;
            boolean flag=true;
            while(flag)
            {
                flag=false;
                for(int i=0;i<lstButtons.size();i++)
                {
                    buttonObj=lstButtons.get(i);
                    if(buttonObj==null) continue;
                    if(buttonType.isInstance(buttonObj))
                    {
                        lstButtons.remove(i);
                        flag=true;
                        break;
                    }
                }
            }
        }
    }

    public AbsButtonType getcertainTypeButton(Class buttonType)
    {
        if(mAllButtons==null||buttonType==null) return null;
        Iterator<String> itPositions=mAllButtons.keySet().iterator();
        String position;
        while(itPositions.hasNext())
        {
            position=itPositions.next();
            List<AbsButtonType> lstButtons=mAllButtons.get(position);
            if(lstButtons==null) continue;
            AbsButtonType buttonObj;
            for(int i=0;i<lstButtons.size();i++)
            {
                buttonObj=lstButtons.get(i);
                if(buttonObj==null) continue;
                if(buttonType.isInstance(buttonObj))
                {
                    return buttonObj;
                }
            }
        }
        return null;
    }

    public List<AbsButtonType> getAllDistinctButtonsList()
    {
        if(mAllButtons==null) return null;
        List<AbsButtonType> lstButtonsResult=new ArrayList<AbsButtonType>();
        List<String> lstButtonNames=new ArrayList<String>();
        for(Entry<String,List<AbsButtonType>> entryButtonsTmp:this.mAllButtons.entrySet())
        {
            for(AbsButtonType buttonObj:entryButtonsTmp.getValue())
            {
                if(buttonObj==null) continue;
                if(lstButtonNames.contains(buttonObj.getName())) continue;
                lstButtonNames.add(buttonObj.getName());
                lstButtonsResult.add(buttonObj);
            }
        }
        return lstButtonsResult;
    }

    public List<AbsButtonType> getAllCertainTypeButtonsList(Class buttonType)
    {
        if(mAllButtons==null||buttonType==null) return null;
        Iterator<String> itPositions=mAllButtons.keySet().iterator();
        List<AbsButtonType> lstButtonsResult=new ArrayList<AbsButtonType>();
        String position;
        while(itPositions.hasNext())
        {
            position=itPositions.next();
            List<AbsButtonType> lstButtons=mAllButtons.get(position);
            if(lstButtons==null) continue;
            AbsButtonType buttonObj;
            for(int i=0;i<lstButtons.size();i++)
            {
                buttonObj=lstButtons.get(i);
                if(buttonObj==null) continue;
                if(buttonType.isInstance(buttonObj))
                {
                    lstButtonsResult.add(buttonObj);
                }
            }
        }
        return lstButtonsResult;
    }

    public List<AbsButtonType> getLstDataExportTypeButtons(String exporttype)
    {
        List<AbsButtonType> lstDataExportButtons=getAllCertainTypeButtonsList(DataExportButton.class);//取到配置的所有数据导出按钮
        if(lstDataExportButtons==null||lstDataExportButtons.size()==0) return null;
        exporttype=exporttype==null?"":exporttype.trim();
        List<AbsButtonType> lstResults=new ArrayList<AbsButtonType>();
        for(AbsButtonType buttonObjTmp:lstDataExportButtons)
        {
            if(exporttype.equals(((DataExportButton)buttonObjTmp).getDataexporttype())) lstResults.add(buttonObjTmp);
        }
        return lstResults;
    }
    
    public List<AbsButtonType> getLstPrintTypeButtons(String printtype)
    {
        List<AbsButtonType> lstPrintButtons=getAllCertainTypeButtonsList(PrintButton.class);
        if(lstPrintButtons==null||lstPrintButtons.size()==0) return null;
        printtype=printtype==null?"":printtype.trim();
        List<AbsButtonType> lstResults=new ArrayList<AbsButtonType>();
        for(AbsButtonType buttonObjTmp:lstPrintButtons)
        {
            if(printtype.equals(((PrintButton)buttonObjTmp).getPrinttype())) lstResults.add(buttonObjTmp);
        }
        return lstResults;
    }
    
    public List<AbsButtonType> getDistinctCertainTypeButtonsList(Class buttonType)
    {
        if(mAllButtons==null||buttonType==null) return null;
        Iterator<String> itPositions=mAllButtons.keySet().iterator();
        List<AbsButtonType> lstButtonsResult=new ArrayList<AbsButtonType>();
        List<String> lstButtonNames=new ArrayList<String>();
        String position;
        while(itPositions.hasNext())
        {
            position=itPositions.next();
            List<AbsButtonType> lstButtons=mAllButtons.get(position);
            if(lstButtons==null) continue;
            AbsButtonType buttonObj;
            for(int i=0;i<lstButtons.size();i++)
            {
                buttonObj=lstButtons.get(i);
                if(buttonObj==null) continue;
                if(buttonType.isInstance(buttonObj))
                {
                    if(lstButtonNames.contains(buttonObj.getName()))
                    {
                        continue;
                    }
                    lstButtonNames.add(buttonObj.getName());
                    lstButtonsResult.add(buttonObj);
                }
            }
        }
        return lstButtonsResult;
    }

    public List<AbsButtonType> getLstButtonsByTypeName(String typename)
    {
        if(mAllButtons==null||typename==null||typename.trim().equals("")) return null;
        List<AbsButtonType> lstButtonsResult=new ArrayList<AbsButtonType>();
        List<String> lstButtonNames=new ArrayList<String>();
        for(Entry<String,List<AbsButtonType>> entryButtonsTmp:this.mAllButtons.entrySet())
        {
            for(AbsButtonType buttonObj:entryButtonsTmp.getValue())
            {
                if(buttonObj==null||buttonObj.getButtonType()==null) continue;
                if(buttonObj.getButtonType().equals(typename))
                {
                    if(lstButtonNames.contains(buttonObj.getName())) continue;
                    lstButtonNames.add(buttonObj.getName());
                    lstButtonsResult.add(buttonObj);
                }
            }
        }
        return lstButtonsResult;
    }
    
    public void printAllButton()
    {
        if(mAllButtons==null) return;
        Iterator<String> itPositions=mAllButtons.keySet().iterator();
        String position;
        while(itPositions.hasNext())
        {
            position=itPositions.next();
            List<AbsButtonType> lstButtons=mAllButtons.get(position);
            if(lstButtons==null) continue;
            AbsButtonType buttonObj;
            for(int i=0;i<lstButtons.size();i++)
            {
                buttonObj=lstButtons.get(i);
                if(buttonObj==null) continue;
                System.out.print(buttonObj.getName()+"---");
            }
        }
    }

    public String showButtons(ReportRequest rrequest,String positiontype)
    {
        if(mAllButtons==null) return "";
        List<AbsButtonType> lstButtons=mAllButtons.get(positiontype);
        if(lstButtons==null||lstButtons.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        String buttonspace=WabacusAssistant.getInstance().getSpacingDisplayString(this.buttonspacing);
        String buttonstr;
        for(AbsButtonType buttonObjTmp:lstButtons)
        {
            buttonstr=ComponentAssistant.getInstance().showButton(this.getCcbean(),buttonObjTmp,rrequest,null);
            if(buttonstr==null||buttonstr.trim().equals("")) continue;
            resultBuf.append(buttonstr).append(buttonspace);
        }
        buttonstr=resultBuf.toString();
        if(!buttonspace.equals("")&&buttonstr.endsWith(buttonspace))
        {
            buttonstr=buttonstr.substring(0,buttonstr.length()-buttonspace.length());
        }
        return buttonstr.trim();
    }

    public void sortButtons()
    {
        if(mAllButtons==null||mAllButtons.size()==0) return;
        for(Entry<String,List<AbsButtonType>> entryButtons:mAllButtons.entrySet())
        {
            if(entryButtons.getValue()==null) continue;
            if(Consts.CONTEXTMENU_PART.equals(entryButtons.getKey()))
            {
                Map<String,List<AbsButtonType>> mButtonsTmp=new HashMap<String,List<AbsButtonType>>();
                String menugroupTmp;
                List<AbsButtonType> lstButtonsTmp;
                for(AbsButtonType buttonTypeObjTmp:entryButtons.getValue())
                {
                    menugroupTmp=buttonTypeObjTmp.getMenugroup();
                    if(menugroupTmp==null||menugroupTmp.trim().equals("")) menugroupTmp="0";
                    lstButtonsTmp=mButtonsTmp.get(menugroupTmp);
                    if(lstButtonsTmp==null) 
                    {//此menugroup下面还没有按钮
                        lstButtonsTmp=new ArrayList<AbsButtonType>();
                        mButtonsTmp.put(menugroupTmp,lstButtonsTmp);
                    }
                    lstButtonsTmp.add(buttonTypeObjTmp);
                }
                List<String> lstMenuGroups=new ArrayList<String>();
                lstMenuGroups.addAll(mButtonsTmp.keySet());
                Collections.sort(lstMenuGroups);
                List<AbsButtonType> lstResults=new ArrayList<AbsButtonType>();
                for(String menugroupTmp2:lstMenuGroups)
                {
                    lstButtonsTmp=mButtonsTmp.get(menugroupTmp2);
                    if(lstButtonsTmp.size()==0) continue;
                    Collections.sort(lstButtonsTmp);
                    lstResults.addAll(lstButtonsTmp);
                }
                mAllButtons.put(Consts.CONTEXTMENU_PART,lstResults);
            }else
            {//排序其它位置的按钮
                Collections.sort(entryButtons.getValue());
            }
        }
    }
    
    public void doPostLoad()
    {
        if(mAllButtons==null) return;
        for(Entry<String, List<AbsButtonType>> entryButtonsTmp:this.mAllButtons.entrySet())
        {
            if(entryButtonsTmp.getValue()==null) continue;
            for(AbsButtonType buttonTypeObjTmp:entryButtonsTmp.getValue())
            {
                buttonTypeObjTmp.doPostLoad();
            }
        }
    }
    
    public void doPostLoadFinally(ReportBean reportbean)
    {
        if(mAllButtons==null) return;
        for(Entry<String, List<AbsButtonType>> entryButtonsTmp:this.mAllButtons.entrySet())
        {
            if(entryButtonsTmp.getValue()==null) continue;
            for(AbsButtonType buttonTypeObjTmp:entryButtonsTmp.getValue())
            {
                buttonTypeObjTmp.doPostLoadFinally();
            }
        }
    }
    
    public ButtonsBean clone(IComponentConfigBean ccbeanNew)
    {
        try
        {
            ButtonsBean newBean=(ButtonsBean)super.clone();
            newBean.setCcbean(ccbeanNew);
            if(mAllButtons!=null)
            {
                Map<String,List<AbsButtonType>> mAllButtons_new=new HashMap<String,List<AbsButtonType>>();
                newBean.setMAllButtons(mAllButtons_new);
                String key;
                List<AbsButtonType> lstButtons;
                List<AbsButtonType> lstButtons_new;
                for(Entry<String,List<AbsButtonType>> entryButtons:mAllButtons.entrySet())
                {
                    key=entryButtons.getKey();
                    lstButtons=entryButtons.getValue();
                    if(lstButtons==null||lstButtons.size()==0) continue;
                    lstButtons_new=new ArrayList<AbsButtonType>();
                    for(AbsButtonType buttonTmp:lstButtons)
                    {
                        if(buttonTmp==null) continue;
                        buttonTmp=(AbsButtonType)buttonTmp.clone(ccbeanNew);
                        lstButtons_new.add(buttonTmp);
                    }
                    mAllButtons_new.put(key,lstButtons_new);
                }
            }
            return newBean;
        }catch(CloneNotSupportedException e)
        {
            log.error("clone 按钮失败",e);
            return null;
        }
    }
}
