/*
 *  Copyright 2012 Be Informed B.V.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.beinformed.framework.osgi.frameworkstate;

/**
 * Simple token class to be returned as a handle to refer to a certain unit of work. 
 */
public final class Token {
	private final Object reference;

	public Token(Object reference) {
		this.reference = reference;
	}
	
	@Override
	public final String toString() {
		return "Token[" + reference + "]";
	}
}