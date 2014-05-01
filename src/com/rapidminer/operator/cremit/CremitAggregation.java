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
import java.util.Collections;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.tools.Ontology;

/**
 * 
 * @author Bálint Kiss (University of West Hungary, Institute of Informatics and Economics)
 */
public class CremitAggregation {
	
    private final int SUM = 0;
    private final int AVG = 1;
    private final int MAX = 2;
    private final int MIN = 3;
	
	public CremitAggregation() {}
	
	private Double sum(List<Double> list) {
    	double sum = 0;
    	
    	for(Double i : list)
    		sum += i;
    		
    	return sum;
    }

    private Double means(List<Double> valuesList) {
    	double upperFraction = 0;
    	
    	for(Double i : valuesList)
    		upperFraction += i;
    		
    	return (upperFraction/valuesList.size());
    }
    
    private Double applyOpr(List<Double> valuesList, int getOperation) {
    	double giveResult = 0;

    	switch(getOperation) {
    		case AVG:	giveResult = means(valuesList);
    					break;
    		case MAX:	giveResult = Collections.max(valuesList);
    					break;
    		case MIN:	giveResult = Collections.min(valuesList);
    					break;
    		default:	giveResult = sum(valuesList);
    					break;
    	}
    	
    	return giveResult;
    }
    
    private void aggregateWindow(ExampleSet result, 
    								List<Example> exampleList, 
    								List<Attribute> keptAttributes, 
    								List<Double> valuesInWindow, 
    								int exampleIndex1, 
    								int exampleIndex2, 
    								Attribute start, 
    								int end, 
    								int untilExample, 
    								int windowingCycle, 
    								int maxWindowWidth, 
    								int aggregationType) {
    	
    	String s1;
    	String s2;
    	
    	Attribute newAtt;
    	Attribute getAtt;
    	Attribute setAtt;
    	
    	switch(exampleIndex1) {
    		case 0: 	s1="a"; break;
    		case 1: 	s1="p"; break;
    		default:	s1=exampleIndex1 + "p"; break;
    	}
    	
    	switch(exampleIndex2) {
    		case 0: 	s2="a"; break;
    		case 1: 	s2="p"; break;
    		default:	s2=exampleIndex2 + "p"; break;
    	}
    	    	
    	for (Attribute a : keptAttributes) {
    		if(keptAttributes.indexOf(a) <= keptAttributes.indexOf(start) &&
    				exampleIndex1 == exampleIndex2)
    			continue;
    		
    		if((exampleIndex2 == untilExample && 
    			keptAttributes.indexOf(a) + 1 > end) || 
    			windowingCycle >= maxWindowWidth)
    				break;
    		   		
    		if( result.getAttributes().get(s1 + start.getName() + "-" + s2 + a.getName()) == null) {
    			newAtt = AttributeFactory.createAttribute(s1 + start.getName() + "-" + s2 + a.getName(), Ontology.REAL);
    			result.getExampleTable().addAttribute(newAtt);
    			result.getAttributes().addRegular(newAtt);			
    		}
    					
    		getAtt = result.getAttributes().get(a.getName());
    		setAtt = result.getAttributes().get(s1 + start.getName() + "-" + s2 + a.getName());
    		
    		if(exampleList.size() < exampleIndex1 + 1) {
    			exampleList.get(0).setValue(setAtt, Double.NaN);
    		} else {
    			valuesInWindow.add(exampleList.get(exampleIndex2).getValue(getAtt));		
    			exampleList.get(0).setValue(setAtt, applyOpr(valuesInWindow, aggregationType));
    		}	
    							
    		windowingCycle = windowingCycle + 1;
    	}
    	
    	
    	if(exampleIndex2 > untilExample)
    		aggregateWindow(result, exampleList, keptAttributes, valuesInWindow, exampleIndex1, (exampleIndex2 - 1), start, end, untilExample, windowingCycle, maxWindowWidth, aggregationType);
    }

    public void aggregateStart(ExampleSet result, 
    							List<Example> exampleList, 
    							List<Attribute> keptAttributes, 
    							int exampleIndex, 
    							int start, 
    							int end, 
    							int untilExample, 
    							int maxWindowWidth, 
    							int aggregationType) {
    	
    	String s;
    	
    	Attribute newAtt;
    	Attribute getAtt;
    	Attribute setAtt;
    	
    	switch(exampleIndex) {
    		case 0: 	s="a"; break;
    		case 1: 	s="p"; break;
    		default:	s=exampleIndex + "p"; break;
    	}
    	    	
    	for (Attribute a : keptAttributes) {
    		if(exampleIndex == untilExample && keptAttributes.indexOf(a) + 1 > end)
    			return;
    	
    		List<Double> valuesInWindow = new ArrayList<Double>();		
    		
    		if( result.getAttributes().get(s + a.getName()) == null) {
    			newAtt = AttributeFactory.createAttribute(s + a.getName(), Ontology.REAL);
    			result.getExampleTable().addAttribute(newAtt);
    			result.getAttributes().addRegular(newAtt);			
    		}

    		getAtt = result.getAttributes().get(a.getName());
    		setAtt = result.getAttributes().get(s + a.getName());
    			
    		if(exampleList.size() < exampleIndex + 1) {
    			exampleList.get(0).setValue(setAtt, Double.NaN);
    		} else {		
    			valuesInWindow.add(exampleList.get(exampleIndex).getValue(getAtt));		
    			exampleList.get(0).setValue(setAtt, exampleList.get(exampleIndex).getValue(getAtt));
    		}
    		
    		int windowingCycle = 1;
    		
    		aggregateWindow(result, exampleList, keptAttributes, valuesInWindow, exampleIndex, exampleIndex, a, end, untilExample, windowingCycle, maxWindowWidth, aggregationType);
    	}

    	if(exampleIndex > untilExample)
    		aggregateStart(result, exampleList, keptAttributes, (exampleIndex-1), 1, end, untilExample, maxWindowWidth, aggregationType);
    }
}
