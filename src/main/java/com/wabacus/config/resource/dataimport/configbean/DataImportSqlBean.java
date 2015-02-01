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
package com.wabacus.config.resource.dataimport.configbean;

import java.util.List;

import com.wabacus.system.datatype.IDataType;

public class DataImportSqlBean
{
    private String sql;

    private List<IDataType> lstParamTypes;

    private List lstParamColsInFile;

    public String getSql()
    {
        return sql;
    }

    public void setSql(String sql)
    {
        this.sql=sql;
    }

    public List getLstParamColsInFile()
    {
        return lstParamColsInFile;
    }

    public void setLstParamColsInFile(List lstParamColsInFile)
    {
        this.lstParamColsInFile=lstParamColsInFile;
    }

    public List<IDataType> getLstParamTypes()
    {
        return lstParamTypes;
    }

    public void setLstParamTypes(List<IDataType> lstParamTypes)
    {
        this.lstParamTypes=lstParamTypes;
    }
}
