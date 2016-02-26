/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.shell;

import org.junit.Test;

import com.axelor.shell.commands.BuiltinCommands;
import com.axelor.shell.commands.ProjectCommands;
import com.axelor.shell.core.Parser;
import com.axelor.shell.core.Shell;

public class TestShell {

	@Test
	public void testShell() throws Exception {
		
		Shell shell = new Shell();
		Parser parser = shell.getParser();
		
		shell.addCommand(new BuiltinCommands(shell));
		shell.addCommand(new ProjectCommands(shell));
		
		parser.printUsage();
	}
}
