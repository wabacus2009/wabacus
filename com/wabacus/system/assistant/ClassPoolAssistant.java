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
package com.wabacus.system.assistant;

import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;

import com.wabacus.exception.WabacusConfigLoadingException;


public class ClassPoolAssistant
{
    private final static ClassPoolAssistant instance=new ClassPoolAssistant();

    protected ClassPoolAssistant()
    {}

    public static ClassPoolAssistant getInstance()
    {
        return instance;
    }
    
    public ClassPool createClassPool()
    {
        ClassPool pool=new ClassPool();
        pool.appendSystemPath();
        pool.insertClassPath(new ClassClassPath(ClassPoolAssistant.class));
        return pool;
    }
    
    public void addImportPackages(ClassPool pool,List<String> lstImports)
    {
        if(lstImports!=null&&lstImports.size()>0)
        {
            for(String importpackage:lstImports)
            {
                if(importpackage==null||importpackage.trim().equals("")) continue;
                importpackage=importpackage.trim();
                if(importpackage.lastIndexOf(".*")==importpackage.length()-2)
                {
                    importpackage=importpackage.substring(0,importpackage.length()-2);
                }
                pool.importPackage(importpackage);
            }
        }
    }
    
    public void addFieldAndGetSetMethod(CtClass ownerclass,String property,CtClass propertytype)
    {
        CtField cfield=addField(ownerclass,property,propertytype,Modifier.PRIVATE);
        addSetMethod(ownerclass,cfield,property);
        addGetMethod(ownerclass,cfield,property);
    }
    
    public CtField addField(CtClass ownerclass,String property,CtClass propertytype,int modifier)
    {
        try
        {
            CtField cfield=new CtField(propertytype,property,ownerclass);
            cfield.setModifiers(modifier);
            ownerclass.addField(cfield);
            return cfield;
        }catch(CannotCompileException e)
        {
            throw new WabacusConfigLoadingException("向类"+ownerclass.getName()+"中添加成员变量"+property+"时失败",e);
        }
    }
    
    public CtMethod addSetMethod(CtClass ownerclass,CtField cfield,String property)
    {
        String setMethodName="set"+property.substring(0,1).toUpperCase()+property.substring(1);
        try
        {
            CtMethod setMethod=CtNewMethod.setter(setMethodName,cfield);
            ownerclass.addMethod(setMethod);
            return setMethod;
        }catch(CannotCompileException e)
        {
            throw new WabacusConfigLoadingException("向类"+ownerclass.getName()+"中添加成员变量"+property+"的set方法时失败",e);
        }
    }
    
    public CtMethod addGetMethod(CtClass ownerclass,CtField cfield,String property)
    {
        String getMethodName="get"+property.substring(0,1).toUpperCase()+property.substring(1);
        try
        {
            CtMethod getMethod=CtNewMethod.getter(getMethodName,cfield);
            ownerclass.addMethod(getMethod);
            return getMethod;
        }catch(CannotCompileException e)
        {
            throw new WabacusConfigLoadingException("向类"+ownerclass.getName()+"中添加成员变量"+property+"的get方法时失败",e);
        }
    }
    
    public CtConstructor addConstructor(CtClass ownerclass,String methodstr)
    {
        try
        {
            CtConstructor constructor=CtNewConstructor.make(methodstr,ownerclass);
            ownerclass.addConstructor(constructor);
            return constructor;
        }catch(CannotCompileException e)
        {
            throw new WabacusConfigLoadingException("向类"+ownerclass.getName()+"中添加方法"+methodstr+"失败",e);
        }
    }
    
    public CtMethod addMethod(CtClass ownerclass,String methodstr)
    {
        try
        {
            CtMethod methodObj=CtNewMethod.make(methodstr,ownerclass);//type参数用于表示当前是在统计整个报表，还是在统计某个分组，统计整个报表时，type为空，统计某个分组时，这里传入相应<rowgroup/>的column属性
            ownerclass.addMethod(methodObj);
            return methodObj;
        }catch(CannotCompileException e)
        {
            throw new WabacusConfigLoadingException("向类"+ownerclass.getName()+"中添加方法"+methodstr+"失败",e);
        }
    }
}
