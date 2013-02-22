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
package com.beinformed.framework.osgi.osgitest.base;

import com.beinformed.framework.osgi.osgitest.TestCase;

/**
 * Immutable implementation of TestCase. <br />
 * Two TestMetaData instances are considered equal if their identifiers are
 * equal.
 */
public final class TestMetaData implements TestCase {

	private final String identifier;
	private final String label;

	/**
	 * Creates a TestMetaData instance with the given identifier as identifier
	 * and label.
	 * 
	 * @param identifier
	 *            The identifier and label
	 * @throws NullPointerException
	 *             when one of the required arguments is null
	 */
	public TestMetaData(final String identifier) {
		this(identifier, identifier);
	}

	/**
	 * Creates a TestMetaData instance with the given identifier and label. <br />
	 * 
	 * @param identifier
	 *            The identifier of this TestCase. May not be {@code null}.
	 * @param label
	 *            The label of this TestCase. May be {@code null}. When the
	 *            label is null, the identifier will be used as a label.
	 * @throws NullPointerException
	 *             when one of the required arguments is null
	 */
	public TestMetaData(final String identifier, final String label) {
		if (identifier == null) {
			throw new NullPointerException("The 'identifier' argument may not be null when constructing a TestMetaData instance.");
		}
		this.identifier = identifier;
		this.label = label != null ? label : identifier;
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TestMetaData other = (TestMetaData) obj;
		if (identifier == null) {
			if (other.identifier != null) {
				return false;
			}
		} else if (!identifier.equals(other.identifier)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("TestMetaData: ").append(identifier).append(", Label: ").append(label);
		return builder.toString();
	}

}
