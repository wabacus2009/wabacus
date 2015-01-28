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
package com.wabacus.system.datatype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javassist.ClassPool;
import javassist.CtClass;

import org.dom4j.Element;

import com.wabacus.config.database.type.AbsDatabaseType;

public interface IDataType extends Cloneable
{
    public void setPreparedStatementValue(int iindex,String value,PreparedStatement pstmt,AbsDatabaseType dbtype) throws SQLException;

    public Object getColumnValue(ResultSet rs,String column,AbsDatabaseType dbtype) throws SQLException;

    public Object getColumnValue(ResultSet rs,int iindex,AbsDatabaseType dbtype) throws SQLException;

    public Object label2value(String label);

    public String value2label(Object value);

    public Class getJavaTypeClass();

    public CtClass getCreatedClass(ClassPool pool);

    public void loadTypeConfig(Element eleDataType);

    public IDataType setUserConfigString(String configstring);
}
