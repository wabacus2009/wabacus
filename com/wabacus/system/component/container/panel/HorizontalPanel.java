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
package com.wabacus.system.component.container.panel;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.container.AbsContainerConfigBean;
import com.wabacus.config.component.container.panel.HorizontalPanelBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.tags.component.AbsComponentTag;
import com.wabacus.util.Consts;

public class HorizontalPanel extends AbsPanelType
{
    private List<ChildDisplayBean> lstChildrenDisplayBeans;

    public HorizontalPanel(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    public void displayOnPage(AbsComponentTag displayTag)
    {
        if(!rrequest.checkPermission(this.containerConfigBean.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            wresponse.println("&nbsp;");
            return;
        }
        wresponse.println(showContainerStartPart());
        wresponse.println(showContainerTableTag());
        if(rrequest.checkPermission(this.containerConfigBean.getId(),Consts.DATA_PART,null,Consts.PERMISSION_TYPE_DISPLAY))
        {
            initChildrenDisplay();
            if(lstChildrenDisplayBeans.size()>0)
            {
                wresponse.println("<tr>");
                for(ChildDisplayBean cdbeanTmp:lstChildrenDisplayBeans)
                {
                    this.showChildObj(this.mChildren.get(cdbeanTmp.getChildConfigBean().getId()),cdbeanTmp.getParentTdWidth());
                }
                wresponse.println("</tr>");
            }
        }
        wresponse.println("</table>");
        wresponse.println(showContainerEndPart());
    }

//    private void initChildrenDisplay()
//        /**
//         * 挑出本次要显示的所有子组件
//         */
//            cdbeanTmp=new ChildDisplayBean();
//        {//如果只有一个要显示的元素，则只要在子组件本身的<table/>中控制宽度即可，不需在当前容器对应<td/>中指定宽度
//            IComponentConfigBean childCcbeanTmp=lstChildrenDisplayBeans.get(0).getChildConfigBean();
//            {
//
//        /**
//         */
//        boolean isAllSetWidthInParentTd=true;//最后一子组件前面所有子组件是否都在其对应的本容器的<td/>中控制宽度，如果是的话，则最后一个组件将不能在其对应的父容器中的<td/>中指定宽度，它必须占据剩余空间
//        {
//            }else
//            if(childWidthArrTmp==null||childWidthArrTmp[0].equals("0"))
//            {//没有配置width，或配置的width无效
//                isAllSetWidthInParentTd=false;//最后一个子组件前面的子组件存在没有配置width的
//            {//如果配置宽度大于等于100%，则不在<td/>中控制它的宽度，因为这样没有意义
//                isAllSetWidthInParentTd=false;
//            {//当前是最后一个子组件，且前面的子组件都是在其所属<td/>中控制宽度，则不管此子组件的width配置成什么值，其对应的此容器的<td/>都不能指定宽度（因为它必须占据剩余的所有空间）
//                if(!childWidthArrTmp[1].equals("%")) cdbeanTmp.setChildDisplayWidth(iwidthvalue+childWidthArrTmp[1]);//如果此子组件配置的宽度不是百分比，则由报表自己控制宽度，如果为百分比，则就显示为100%
//            }else
//                {//配置的宽度是百分比，则宽度在对应此容器<td/>中控制
//                {//如果配置的是像素，则只要子组件自己显示width
//                    cdbeanTmp.setChildDisplayWidth(iwidthvalue+childWidthArrTmp[1]);
    
    private void initChildrenDisplay()
    {
        lstChildrenDisplayBeans=new ArrayList<ChildDisplayBean>();
        ChildDisplayBean cdbeanTmp;
        IComponentConfigBean childConfigBeanTmp;
        for(int i=0,len=this.lstChildrenIds.size();i<len;i++)
        {
            childConfigBeanTmp=this.mChildren.get(lstChildrenIds.get(i)).getConfigBean();
            if(!rrequest.checkPermission(childConfigBeanTmp.getId(),null,null,Consts.PERMISSION_TYPE_DISPLAY)) continue;
            cdbeanTmp=new ChildDisplayBean();
            cdbeanTmp.setChildConfigBean(childConfigBeanTmp);
            lstChildrenDisplayBeans.add(cdbeanTmp);
        }
        String childWidthTmp;
        String[] childWidthArrTmp;
        for(int i=0,len=lstChildrenDisplayBeans.size()-1;i<=len;i++)     
        {
            cdbeanTmp=lstChildrenDisplayBeans.get(i);
            if(cdbeanTmp.getChildConfigBean() instanceof ReportBean)
            {
                childWidthTmp=((ReportBean)cdbeanTmp.getChildConfigBean()).getDisplayWidth();
            }else
            {
                childWidthTmp=cdbeanTmp.getChildConfigBean().getWidth();
            }
            childWidthArrTmp=WabacusAssistant.getInstance().parseHtmlElementSizeValueAndType(childWidthTmp);
            if(childWidthArrTmp==null||childWidthArrTmp[0].equals("0"))
            {//没有配置width，或配置的width无效
                continue;
            }
            int iwidthvalue=Integer.parseInt(childWidthArrTmp[0]);
            if(childWidthArrTmp[1].equals("%"))
            {
                if(lstChildrenDisplayBeans.size()==1)
                {//只有一个元素，则不在父容器<td/>中控制它的百分比，而是让报表自己显示百分比
                    cdbeanTmp.setParentTdWidth(null);
                    cdbeanTmp.setChildDisplayWidth(iwidthvalue+childWidthArrTmp[1]);
                }
                if(iwidthvalue>=100) cdbeanTmp.setChildDisplayWidth(iwidthvalue+childWidthArrTmp[1]);//如果配置宽度大于等于100%，则不在<td/>中控制它的宽度，因为这样没有意义
            }else
            {
                cdbeanTmp.setParentTdWidth(iwidthvalue+childWidthArrTmp[1]);
                cdbeanTmp.setChildDisplayWidth(iwidthvalue+childWidthArrTmp[1]);
            }
        }
    }
    
   /* private String showChildObj(IComponentType childObj,boolean isLast,boolean isAllExceptLastHasWidthConfig)
    {
        IComponentConfigBean childConfigBean=childObj.getConfigBean();
        String tdwidth=null;
        if(isSetWidthInParentContainerTd(isLast,isAllExceptLastHasWidthConfig))
        {//如果当前子组件是由其所属的此容器的<td/>来控制
            if(childConfigBean.getWidth()!=null&&!childConfigBean.getWidth().trim().equals(""))
            {
                tdwidth=childConfigBean.getWidth().toLowerCase().trim();
                if(tdwidth.indexOf("%")<0&&(childConfigBean.getLeft()!=null&&!childConfigBean.getLeft().trim().equals("")&&childConfigBean.getLeft().indexOf("%")<0||
                        childConfigBean.getRight()!=null&&!childConfigBean.getRight().trim().equals("")&&childConfigBean.getRight().indexOf("%")<0))
                {
                    int itdwidth=getRealIntSizeByString(tdwidth);
                    if(itdwidth<0) itdwidth=0;
                    int ileft=0;
                    int iright=0;
                    if(childConfigBean.getLeft()!=null&&!childConfigBean.getLeft().trim().equals("")&&childConfigBean.getLeft().indexOf("%")<0)
                    {
                        ileft=getRealIntSizeByString(childConfigBean.getLeft());
                        if(ileft<0) ileft=0;
                    }
                    if(childConfigBean.getRight()!=null&&!childConfigBean.getRight().trim().equals("")&&childConfigBean.getRight().indexOf("%")<0)
                    {
                        iright= getRealIntSizeByString(childConfigBean.getRight());
                        if(iright<0) iright=0;
                    }
                    itdwidth=itdwidth+ileft+iright;
                    if(itdwidth<=0)
                    {
                        tdwidth=null;
                    }else
                    {
                        tdwidth=itdwidth+" px";
                    }
                }
            }
        }
        return super.showChildObj(childObj,tdwidth);
    }
    
    *
     * 获取配置的某个大小值（比如width/height）的整数部分
     * 比如传入"30px" 得到30等。
     * @param size
     * @return
     *//*
    private int getRealIntSizeByString(String size)
    {
        if(size==null||size.trim().equals("")) return 0;
        StringBuffer tmpBuf=new StringBuffer();
        size=size.trim();
        for(int i=0;i<size.length();i++)
        {
            if(size.charAt(i)>='0'&&size.charAt(i)<='9')
            {
                tmpBuf.append(size.charAt(i));
            }else
            {
                break;
            }
        }
        if(tmpBuf.length()==0) return 0;
        return Integer.parseInt(tmpBuf.toString());
    }*/
    
    public String getChildDisplayWidth(IComponentConfigBean childBean)
    {
        for(ChildDisplayBean cdbeanTmp:lstChildrenDisplayBeans) 
        {
            if(cdbeanTmp.getChildConfigBean().getId().equals(childBean.getId()))
            {
                if(cdbeanTmp.getChildDisplayWidth()==null||cdbeanTmp.getChildDisplayWidth().trim().equals("")) return "100%";
                return cdbeanTmp.getChildDisplayWidth();
            }
        }
        return "100%";
    }

    /*private boolean isSetWidthInParentContainerTd(boolean isLast,boolean isAllExceptLastHasWidthConfig)
    {
        if(isLast&&isAllExceptLastHasWidthConfig)
        {//如果本组件是当前容器的最后一个子组件，且其前面所有子组件都配置了width属性，则它的width由它自己的<table/>控制，因为它所属的<td/>要充满整个剩下的空间，不能控制它的宽度。
            return false;
        }else
        {//如果当前子组件不是当前容器的最后一个组件，或者虽然是最后一个组件，但前面存在没有配置width的组件，则此组件的宽度由其所属的此容器的<td/>来控制
            return true;
        }
    }*/

    protected AbsContainerConfigBean createContainerConfigBean(AbsContainerConfigBean parentContainer,String tagname)
    {
        return new HorizontalPanelBean(parentContainer,tagname);
    }
    
    protected String getComponentTypeName()
    {
        return "container.hpanel";
    }
    
    private class ChildDisplayBean
    {
        private IComponentConfigBean childConfigBean;

        private String parentTdWidth;//显示此组件的本容器<td/>的width（只有子组件的width需要在父容器的<td/>中控制时，才会有这个值，否则为null）

        private String childDisplayWidth;//本子组件自己<table/>的width，如果不用在子组件中控制宽度（可能只需在父容器的<td/>中控制宽度，比如配置的宽度为百分比），则为100%
        
        public IComponentConfigBean getChildConfigBean()
        {
            return childConfigBean;
        }

        public void setChildConfigBean(IComponentConfigBean childConfigBean)
        {
            this.childConfigBean=childConfigBean;
        }

        public String getParentTdWidth()
        {
            return parentTdWidth;
        }

        public void setParentTdWidth(String parentTdWidth)
        {
            this.parentTdWidth=parentTdWidth;
        }

        public String getChildDisplayWidth()
        {
            return childDisplayWidth;
        }

        public void setChildDisplayWidth(String childDisplayWidth)
        {
            this.childDisplayWidth=childDisplayWidth;
        }
    }
}
