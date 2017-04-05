/**
*
* Copyright 2017 Volker Seeker <volker@seekerscience.co.uk>.
*
* This file is part of JLagmarker.
*
* JLagmarker is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* JLagmarker is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with JLagmarker. If not, see <http://www.gnu.org/licenses/>.
*
*/
package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import mobileworkloads.mlgovernor.res.CSVResourceTools;

public abstract class WorkerConfig {
	
	public abstract class WorkerConfParams {
		public int lagId;
		
		public abstract String[] toCSVArray();
		public abstract WorkerConfParams clone(); 
	}

	protected static final String DEFAULT_MARKER = "%D%";
	protected static final String DEFAULT_PARAMS_HEAD = "DEFAULT";
	
	protected WorkerConfParams defaultConfParams;
	protected final List<WorkerConfParams> confParams;
	
	protected final boolean generateConfig;
	protected int currLagId;
	
	public WorkerConfig() {
		confParams = new ArrayList<WorkerConfParams>();
		generateConfig = true;
		currLagId = 0;
		setDefaultParams(getInitialDefaultParams());
	}

	public WorkerConfig(Path configFile) throws IOException {
		confParams = new ArrayList<WorkerConfParams>();
		generateConfig = false;
		
		parseConfig(configFile);
	}
	
	public WorkerConfParams getParams(int lagId) {
		if(confParams.size() <= lagId && generateConfig) {
			WorkerConfParams params = defaultConfParams.clone();
			params.lagId = currLagId++;
			confParams.add(params);
			return params;
		} else {
			return confParams.get(lagId);			
		}
	}

	protected abstract int getHeadLength();
	
	protected abstract String[] getHeader();
	
	protected abstract WorkerConfParams getInitialDefaultParams();
	
	public void setDefaultParams(WorkerConfParams params) {
		params.lagId = -1;
		defaultConfParams = params;
	}
	
	protected void parseConfig(Path configFile) throws IOException {
		if(configFile == null || !Files.isRegularFile(configFile))
			throw new IOException("Given worker configuration file invalid: " + configFile);

		System.out.println("Parsing worker configuration: " + configFile + " ...");
		
		try {
			List<String> header = CSVResourceTools.readHeader(Files.newBufferedReader(configFile, Charset.forName("UTF-8")));
			if(header.size() != getHeadLength())
				throw new IllegalArgumentException("Given worker config file has unexpected format: " + configFile + "\n"
						+ String.join(CSVResourceTools.SEPARATOR, header));
			
			List<List<String>> records = CSVResourceTools.readRecords(Files.newBufferedReader(configFile, Charset.forName("UTF-8")));
			if(records.size() < 1)
				throw new IllegalArgumentException("Given resource file has no records: " + configFile);
			
			defaultConfParams = parseParams(records.get(0));
			
			for (List<String> record : records.subList(1, records.size())) {
				confParams.add(parseParams(record));
			}
			
		} catch (IOException e) {
			throw new UncheckedIOException("Error opening worker configuration file: " + configFile, e);
		}
		
        System.out.println(confParams.size() + " worker configuration entries parsed successfully.");
	}
	
	protected abstract WorkerConfParams parseParams(List<String> record);
	
	protected void saveToFile(Path outputFile) {
		if(Files.exists(outputFile)) {
			try {
				Files.delete(outputFile);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	
		try(OutputStreamWriter outWriter = new OutputStreamWriter(
				Files.newOutputStream(outputFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
			CSVResourceTools.writeRawHeader(outWriter, getHeader());
			
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		try(OutputStreamWriter outWriter = new OutputStreamWriter(
				Files.newOutputStream(outputFile, StandardOpenOption.APPEND, StandardOpenOption.WRITE))) {

			List<String[]> recCsvs = confParams.stream().map(r -> r.toCSVArray()).collect(Collectors.toList());
			recCsvs.add(0, defaultConfParams.toCSVArray());
			CSVResourceTools.writeRawRecords(outWriter, recCsvs);
			
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}}
