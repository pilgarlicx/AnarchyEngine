/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.gl.exceptions;

public class CompileShaderException extends RuntimeException {

	private static final long serialVersionUID = -8459235864100073938L;

	public CompileShaderException() {
		super();
	}

	public CompileShaderException(String error) {
		super(error);
	}

	public CompileShaderException(Exception e) {
		super(e);
	}

	public CompileShaderException(Throwable cause) {
		super(cause);
	}

	public CompileShaderException(String message, Throwable cause) {
		super(message, cause);
	}

}
