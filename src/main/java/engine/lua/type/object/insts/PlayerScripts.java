/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.type.object.insts;

import org.luaj.vm2.LuaValue;

import engine.lua.type.object.Instance;
import engine.lua.type.object.ScriptExecutor;
import engine.lua.type.object.TreeViewable;
import ide.layout.windows.icons.Icons;

public class PlayerScripts extends Instance implements TreeViewable,ScriptExecutor {

	public PlayerScripts() {
		super("PlayerScripts");

		this.setLocked(true);
		this.setInstanceable(false);
		
		this.rawset("Archivable", LuaValue.valueOf(false));
	}

	@Override
	protected LuaValue onValueSet(LuaValue key, LuaValue value) {
		return value;
	}

	@Override
	protected boolean onValueGet(LuaValue key) {
		return true;
	}

	@Override
	public Icons getIcon() {
		return Icons.icon_player_scripts;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
	}
}
