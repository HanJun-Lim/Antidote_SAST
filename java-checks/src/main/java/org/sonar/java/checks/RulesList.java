package org.sonar.java.checks;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;


public final class RulesList {

    private RulesList() {
    }

    public static List<Class<? extends JavaCheck>> getChecks() {
        List<Class<? extends JavaCheck>> checks = new ArrayList<>();
        checks.addAll(getJavaChecks());
        checks.addAll(getJavaTestChecks());
        return Collections.unmodifiableList(checks);
    }
    
    public static List<Class<? extends JavaCheck>> getJavaChecks() {
    	/*
    	Reflections reflections = new Reflections("org.sonar.java.testpkg");
    	
        Set<Class<? extends IssuableSubscriptionVisitor>> subTypes = reflections.getSubTypesOf(IssuableSubscriptionVisitor.class);

        List<Class<? extends JavaCheck>> returnValue = new ArrayList<>();
        for(Iterator<Class<? extends IssuableSubscriptionVisitor>> itr = subTypes.iterator(); itr.hasNext();) {
        	Class<? extends JavaCheck> tmp = itr.next();
            try {
            	returnValue.add(tmp);    			
    		} catch (IllegalArgumentException | SecurityException e) {
    			
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			
    		}
        }
        return returnValue;
        */
    	
    	Reflections reflections = new Reflections("org.sonar.java.testpkg");
        List<Class<? extends JavaCheck>> returnValue = new ArrayList<>();

        
        Set<Class<? extends IssuableSubscriptionVisitor>> issuableSubTypes = reflections.getSubTypesOf(IssuableSubscriptionVisitor.class);

        for(Iterator<Class<? extends IssuableSubscriptionVisitor>> itr = issuableSubTypes.iterator(); itr.hasNext();) {
        	Class<? extends JavaCheck> tmp = itr.next();
            try {
            	if(tmp.getAnnotation(org.sonar.check.Rule.class)==null)
            		continue;
            	returnValue.add(tmp);
    		} catch (IllegalArgumentException | SecurityException e) {
    			
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			
    		}
        }
        return returnValue;
        
    }

    public static List<Class<? extends JavaCheck>> getJavaTestChecks() {
        return Collections.emptyList();
    }

    public static HashMap<String, JavaFileScanner> getJavaChecksHashMap() {
    	/*
        HashMap<String, JavaFileScanner> checksHashMap = new HashMap<String, JavaFileScanner>();

        
        Reflections reflections = new Reflections("org.sonar.java.testpkg");

        Set<Class<? extends IssuableSubscriptionVisitor>> subTypes = reflections.getSubTypesOf(IssuableSubscriptionVisitor.class);
        
        
        for(Iterator<Class<? extends IssuableSubscriptionVisitor>> itr = subTypes.iterator(); itr.hasNext();) {
        	Class<? extends JavaCheck> tmp = itr.next();
            try {

            	org.sonar.check.Rule tmpAnno = tmp.getAnnotation(org.sonar.check.Rule.class);
    			checksHashMap.put(tmpAnno.key(), (JavaFileScanner)tmp.getDeclaredConstructor().newInstance());
    			
    		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
    				| NoSuchMethodException | SecurityException e) {
    			
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			
    		}
        }
        
        
        return checksHashMap;
        */

        HashMap<String, JavaFileScanner> checksHashMap = new HashMap<String, JavaFileScanner>();

        
        Reflections reflections = new Reflections("org.sonar.java.testpkg");

        Set<Class<? extends IssuableSubscriptionVisitor>> issuableSubTypes = reflections.getSubTypesOf(IssuableSubscriptionVisitor.class);
        
        for(Iterator<Class<? extends IssuableSubscriptionVisitor>> itr = issuableSubTypes.iterator(); itr.hasNext();) {
        	Class<? extends JavaCheck> tmp = itr.next();
            try {
            	org.sonar.check.Rule issuAnno = tmp.getAnnotation(org.sonar.check.Rule.class);
            	if(issuAnno==null)
            		continue;
    			checksHashMap.put(issuAnno.key(), (JavaFileScanner)tmp.getDeclaredConstructor().newInstance());
    		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
    				| NoSuchMethodException | SecurityException e) {
    			
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			
    		}
        }
        
        return checksHashMap;
        
    }


}