/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/DrawControls/TreeNode.java
 * Version: 0.7.1m  Date: 21.04.2022
 * Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/


package jimmui.view.roster.items;

import jimmui.view.icons.Icon;

//! Tree node
/*! This class is used to handle tree nodes (adding, deleting, moveing...) */
public interface TreeNode {
    String getText();
    void getLeftIcons(Icon[] icons);
    void getRightIcons(Icon[] icons);
}

