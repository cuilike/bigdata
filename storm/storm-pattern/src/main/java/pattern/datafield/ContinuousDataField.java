/*
 * Copyright (c) 2007-2013 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.concurrentinc.com/
 */

package pattern.datafield;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import pattern.PatternException;
import pattern.XPathReader;
import storm.trident.tuple.TridentTuple;

public class ContinuousDataField extends DataField {
	/** Field LOG */
	private static final Logger LOG = LoggerFactory
			.getLogger(ContinuousDataField.class);

	/**
	 * @param name
	 * @param op_type
	 * @param data_type
	 */
	public ContinuousDataField(String name, String op_type, String data_type) {
		this.name = name;
		this.op_type = op_type;
		this.data_type = data_type;
	}

	/**
	 * @param reader
	 * @param node
	 * @return String
	 * @throws PatternException
	 */
	public String getEval(XPathReader reader, Element node)
			throws PatternException {
		String operator = node.getAttribute("operator");
		String value = node.getAttribute("value");
		String eval = null;

		if (operator.equals("equal"))
			eval = name + " == " + value;
		else if (operator.equals("notEqual"))
			eval = name + " != " + value;
		else if (operator.equals("lessThan"))
			eval = name + " < " + value;
		else if (operator.equals("lessOrEqual"))
			eval = name + " <= " + value;
		else if (operator.equals("greaterThan"))
			eval = name + " > " + value;
		else if (operator.equals("greaterOrEqual"))
			eval = name + " >= " + value;
		else
			throw new PatternException("unknown operator: " + operator);

		return eval;
	}

	/** @return Class */
	public Class getClassType() {
		return double.class;
	}

	/**
	 * @param values
	 * @param i
	 * @return Object
	 * @throws PatternException
	 */
	public Object getValue(TridentTuple values, int i) throws PatternException {
		try {
			return values.getDouble(i);
		} catch (NumberFormatException exception) {
			LOG.error("tuple format is bad", exception);
			throw new PatternException("tuple format is bad", exception);
		}
	}
}
