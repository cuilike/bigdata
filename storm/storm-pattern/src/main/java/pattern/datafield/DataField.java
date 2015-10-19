/*
 * Copyright (c) 2007-2013 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.concurrentinc.com/
 */

package pattern.datafield;

import java.io.Serializable;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import pattern.PatternException;
import pattern.XPathReader;
import storm.trident.tuple.TridentTuple;

public abstract class DataField implements Serializable {
	public String name;
	public String op_type;
	public String data_type;

	/**
	 * Does nothing. Override this method if a DataField subclass needs to parse
	 * additional info from PMML.
	 * 
	 * @param reader
	 * @param node
	 */
	public void parse(XPathReader reader, Node node) {
	}

	/**
	 * @param reader
	 * @param node
	 * @return String
	 * @throws PatternException
	 */
	public abstract String getEval(XPathReader reader, Element node)
			throws PatternException;

	/** @return */
	public abstract Class getClassType();

	/**
	 * @return Object
	 * @throws PatternException
	 */
	public abstract Object getValue(TridentTuple values, int i)
			throws PatternException;

	/** @return Object */
	@Override
	public String toString() {
		return name + ":" + op_type + ":" + data_type;
	}
}
