/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2014 by  RapidMiner GmbH and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapidminer.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package com.rapidminer.operator.cremit;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeIterator;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleReader;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.EqualTypeCondition;

import com.rapidminer.operator.cremit.CremitAggregation;

/**
 * 
 * @author Bálint Kiss (University of West Hungary, Institute of Informatics and Economics)
 */
public class CremitAggregationOperator extends Operator {

    // Declaring parameters as public constants
	public static final String PARAMETER_ATTRIBUTES_TO_SKIP			= "Attributes to skip";
	public static final String PARAMETER_START_UNIT					= "Starting time unit";
    public static final String PARAMETER_END_UNIT					= "Ending time unit";
    public static final String PARAMETER_MAXIMUM_TIME_SHIFTING		= "Maximum time shifting";
    public static final String PARAMETER_UNTIL_TYPE					= "Ending record type";
    public static final String PARAMETER_UNTIL_RECORD				= "Ending previous record";
    public static final String PARAMETER_MAX_WINDOW_SIZE			= "Maximum window size";
    public static final String PARAMETER_AGGREGATION_TYPE			= "Type of aggregation";
    public static final String PARAMETER_LEAVE_ORIGINAL				= "Leave original";
	
    // Contents of "Type of aggregation" and "Ending record type" parameters
    private final String[] OPERATIONS = { "sum", "avg", "max", "min" };
    private final String[] PREVIOUS_OR_ACTUAL = { "previous time records", "actual time records" };
        
	// Getting input and output as Example Set
    private InputPort exampleSetInput 	= 	getInputPorts().createPort("example set", ExampleSet.class);
    private OutputPort exampleSetOutput = 	getOutputPorts().createPort("example set");
	    
	private List<Example> timeRecords;
	private List<Attribute> keptAttributes;
    
	public CremitAggregationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		
		types.add(new ParameterTypeAttributes(PARAMETER_ATTRIBUTES_TO_SKIP, "Select regular attributes that shouldn't be involved in the CREMIT aggregation. These attributes won't be dropped from the output.", exampleSetInput, true));
		types.add(new ParameterTypeInt(PARAMETER_START_UNIT, "Index of time attribute to start the aggregation from.", 0, Integer.MAX_VALUE, 1, false));
		types.add(new ParameterTypeInt(PARAMETER_END_UNIT, "Index of time attribute where CREMIT aggregation should end.", 0, Integer.MAX_VALUE, 12, false));
		types.add(new ParameterTypeInt(PARAMETER_MAXIMUM_TIME_SHIFTING, "Amount of time shifting and number of previous records to involve. Set to zero if you only want to work with actual records.", 0, Integer.MAX_VALUE, 1, false));
		
		types.add(new ParameterTypeCategory(PARAMETER_UNTIL_TYPE,"Determines whether the ending record should be the actual record in the aggregation or a previous record.", PREVIOUS_OR_ACTUAL, 1, false));
		ParameterType type = new ParameterTypeInt(PARAMETER_UNTIL_RECORD, "Determines at which previous record the aggregation should end.", 1, Integer.MAX_VALUE, 1, false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_UNTIL_TYPE, PREVIOUS_OR_ACTUAL, true, 0));
		
		types.add(type);
		types.add(new ParameterTypeInt(PARAMETER_MAX_WINDOW_SIZE, "Maximum number of values of time attributes to involve in one window.", 1, Integer.MAX_VALUE, 6, false));
		types.add(new ParameterTypeCategory(PARAMETER_AGGREGATION_TYPE, "Type of aggregation to apply on involved values in the windows.", OPERATIONS, 0, false));
		
		return types;
	}
	
    @Override
    public void doWork() throws OperatorException {

        ExampleSet data = exampleSetInput.getData();    
        ExampleSet result;
    	
        String attributesToSkip			= getParameterAsString(PARAMETER_ATTRIBUTES_TO_SKIP);
               
    	int startUnit 		= getParameterAsInt(PARAMETER_START_UNIT);
    	int endUnit 		= getParameterAsInt(PARAMETER_END_UNIT);
    	int maximumTimeShifting 	= getParameterAsInt(PARAMETER_MAXIMUM_TIME_SHIFTING);
    	
    	int until;
		if(getParameterAsInt(PARAMETER_UNTIL_TYPE) == 1)
    		until = 0;
    	else
    		until = getParameterAsInt(PARAMETER_UNTIL_RECORD);
    	
    	int maxWindowSize 			= getParameterAsInt(PARAMETER_MAX_WINDOW_SIZE);
    	int aggregationType			= getParameterAsInt(PARAMETER_AGGREGATION_TYPE);
   	   	
    	result = data;

    	CremitAggregation cremit = new CremitAggregation();
    	
    	ExampleReader exampleReader = (ExampleReader) result.iterator();
    	AttributeIterator attributeIterator = (AttributeIterator) result.getAttributes().iterator();
    	timeRecords = new ArrayList<Example>();
    	keptAttributes = new ArrayList<Attribute>();
    	    	
    	while (attributeIterator.hasNext()) {
    		keptAttributes.add(attributeIterator.next());
    	}
    	
    	if(attributesToSkip != null && attributesToSkip.length() != 0) {
    		for (String attributeName : attributesToSkip.split("\\|")) {
    			if (keptAttributes.contains(result.getAttributes().get(attributeName))) {
    				keptAttributes.remove(result.getAttributes().get(attributeName));
    			}
    		}
    	}
    	    	    	
    	while (exampleReader.hasNext()) {
    		timeRecords.add(0, exampleReader.next());
    		
    		if(timeRecords.size() > maximumTimeShifting + 1)
    			timeRecords.remove(timeRecords.size() - 1);
    		
    		cremit.aggregateStart(result, timeRecords, keptAttributes, maximumTimeShifting, startUnit, endUnit, until, maxWindowSize, aggregationType);	 
    	}

    	exampleSetOutput.deliver(result);    
    }
}
