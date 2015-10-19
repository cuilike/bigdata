/*
 * Copyright (c) 2007-2013 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.concurrentinc.com/
 */

package pattern.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pattern.Classifier;
import pattern.PatternException;
import storm.trident.testing.MockTridentTuple;
import storm.trident.tuple.TridentTuple;

public class ModelTest {
	/** Field LOG */
	private static final Logger LOG = LoggerFactory.getLogger(ModelTest.class);

	/**
	 * Create a temporary text file, used for: a PMML model source, reference
	 * input data.
	 * 
	 * @param base
	 *            base path in the file system
	 * @param suffix
	 *            file suffix
	 * @param text
	 *            text to write into the file
	 * @return String
	 */
	protected String makeFile(String base, String suffix, String text)
			throws IOException {
		String filename = null;
		PrintStream out = null;

		try {
			File file = File.createTempFile(base, suffix);
			file.deleteOnExit();

			filename = file.getCanonicalFile().toString();

			if (LOG.isDebugEnabled())
				LOG.debug("file: {}", filename);

			out = new PrintStream(new FileOutputStream(file));
			out.print(text);
		} catch (IOException exception) {
			LOG.error("could not create temp file", exception);
			fail("cannot set up test environment");
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}

			return filename;
		}
	}

	/**
	 * For each tuple in the reference data -- assuming that the last field is a
	 * predicted "label" -- present the input tuple to the model and compare the
	 * resulting label vs. predicted as a regression test.
	 * 
	 * @param data_file
	 *            input data for the regression test
	 * @param classifier
	 *            Classifier object based on the PMML model
	 * @throws IOException
	 * @throws PatternException
	 */
	protected void eval_data(String data_file, Classifier classifier)
			throws IOException, PatternException {
		FileReader fr = new FileReader(data_file);
		BufferedReader br = new BufferedReader(fr);
		String line;
		int count = 0;

		while ((line = br.readLine()) != null) {
			if (count++ > 0) {
				// for each tuple in the reference data, assuming that the
				// predicted "label" is in the last field...

				String[] test_vector = line.split("\\t");
				String predicted = test_vector[test_vector.length - 1];

				int i = 1;
				List<Double> tempList = new LinkedList<Double>();
				for (String key : classifier.model.schema.keySet())
					tempList.add(Double.parseDouble(test_vector[i++]));

				TridentTuple values = new MockTridentTuple(new LinkedList(
						classifier.model.schema.keySet()), tempList);

				// compare classifier label vs. predicted

				classifier.prepare();

				String label = classifier.classifyTuple(values);
				LOG.debug(values.toString() + " predicted: " + predicted
						+ " score: " + label);

				if (!predicted.equals(label)) {
					StringBuilder sb = new StringBuilder();

					sb.append(
							String.format(
									"regression: classifier label [ %s ] does not match predicted [ %s ]\n",
									label, predicted)).append(line);

					fail(sb.toString());
				}

				assertEquals("Label", predicted, label);
			}
		}

		fr.close();
	}
}
