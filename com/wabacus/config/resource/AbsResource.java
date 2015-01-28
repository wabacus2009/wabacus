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

import org.dom4j.Element;

public abstract class AbsResource
{
//    protected boolean dependOtherRes;//当前资源项是否依赖其它资源项的内容，在加载时，如果当前资源项依赖其它资源项，但别的资源项还没有加载，则需要用到这个变量来标识，以便稍后再给它赋真正值。
//    }
//    /**
//     */
//    {
    public abstract Object getValue(Element itemElement);
}
