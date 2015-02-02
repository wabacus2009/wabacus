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
package com.wabacus.config.resource;

import java.util.List;

import org.dom4j.Element;

import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Tools;

public class InterceptorRes extends AbsResource
{
    public Object getValue(Element itemElement)
    {
        if(itemElement==null)
        {
            throw new WabacusConfigLoadingException("在资源文件中没有配置拦截器资源项");
        }
        String name=itemElement.attributeValue("key");
        Element eleInterceptor=itemElement.element("interceptor");
        if(eleInterceptor==null)
        {
            throw new WabacusConfigLoadingException("在资源文件中配置的资源项"+itemElement.attributeValue("key")+"不是有效的拦截器资源项，必须以<interceptor/>做为其顶层标签");
        }
        List<String> lstImportPackages=ConfigLoadAssistant.getInstance().loadImportsConfig(eleInterceptor);
        Element elePreAction=eleInterceptor.element("preaction");
        String preaction=elePreAction==null?null:elePreAction.getText();
        Element elePostAction=eleInterceptor.element("postaction");
        String postaction=elePostAction==null?null:elePostAction.getText();
        Element eleSaveaction=eleInterceptor.element("saveaction");
        String saveaction=eleSaveaction==null?null:eleSaveaction.getText();
        Element eleSaverowaction=eleInterceptor.element("saveaction-perrow");
        String saverowaction=eleSaverowaction==null?null:eleSaverowaction.getText();
        Element eleSavesqlaction=eleInterceptor.element("saveaction-peraction");
        String savesqlaction=eleSavesqlaction==null?null:eleSavesqlaction.getText();
        Element eleBeforeLoadData=eleInterceptor.element("beforeloaddata");
        String beforeloaddata=eleBeforeLoadData==null?null:eleBeforeLoadData.getText();
        Element eleAfterLoadData=eleInterceptor.element("afterloaddata");
        Element eleBeforeDisplay=eleInterceptor.element("beforedisplay");
        String beforedisplay=eleBeforeDisplay==null?null:eleBeforeDisplay.getText();
        String afterloaddata=eleAfterLoadData==null?null:eleAfterLoadData.getText();
        Element eleDisplayPerRow=eleInterceptor.element("beforedisplay-perrow");
        String displayperrow=eleDisplayPerRow==null?null:eleDisplayPerRow.getText();
        Element eleDisplayPerCol=eleInterceptor.element("beforedisplay-percol");
        String displaypercol=eleDisplayPerCol==null?null:eleDisplayPerCol.getText();

        if(Tools.isEmpty(preaction,true)&&Tools.isEmpty(postaction,true)&&Tools.isEmpty(saveaction,true)&&Tools.isEmpty(saverowaction,true)
                &&Tools.isEmpty(savesqlaction,true)&&Tools.isEmpty(beforeloaddata,true)&&Tools.isEmpty(afterloaddata,true)
                &&Tools.isEmpty(beforedisplay,true)&&Tools.isEmpty(displayperrow,true)&&Tools.isEmpty(displaypercol,true))
        {
            return null;
        }
        Class c=ReportAssistant.getInstance().buildInterceptorClass("resource_"+name,lstImportPackages,preaction,postaction,saveaction,saverowaction,
                savesqlaction,beforeloaddata,afterloaddata,beforedisplay,displayperrow,displaypercol);
        if(c!=null)
        {
            try
            {
                return (IInterceptor)c.newInstance();

            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("在资源文件中定义的拦截器类"+name+"无法实例化对象",e);
            }
        }
        return null;
    }
}
