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
package com.wabacus.system.component.application.report.configbean;

import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;

public class DetailReportColBean extends AbsExtendConfigBean
{
    private boolean br;

    private int labelcolspan=1;
    
    private int valuecolspan=1;
    
    public DetailReportColBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public int getLabelcolspan()
    {
        return labelcolspan;
    }

    public void setLabelcolspan(int labelcolspan)
    {
        this.labelcolspan=labelcolspan;
    }

    public int getValuecolspan()
    {
        return valuecolspan;
    }

    public void setValuecolspan(int valuecolspan)
    {
        this.valuecolspan=valuecolspan;
    }

    public boolean isBr()
    {
        return br;
    }

    public void setBr(boolean br)
    {
        this.br=br;
    }
}
