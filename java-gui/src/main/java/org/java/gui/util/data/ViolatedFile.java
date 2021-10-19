package org.java.gui.util.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ViolatedFile {
	private String filePath = "";
	
	private ArrayList<String> tmpRuleKeys = new ArrayList<String>();
	private List<Integer> tmpErrorCountList = new ArrayList<Integer>();
	
	private List<ViolatedRule> violatedRuleList = new ArrayList<ViolatedRule>();
	
	public ViolatedFile() {

	}

	public ViolatedFile(String filePath) {
		this.filePath = filePath;
	}

	public ViolatedFile(String filePath, String tmpRuleKey, int count) {
		this.filePath = filePath;
		this.tmpRuleKeys.add(tmpRuleKey);
		this.tmpErrorCountList.add(count);
	}

	public ViolatedFile(String filePath, List<ViolatedRule> violatedRuleList) {
		this.filePath = filePath;
		this.violatedRuleList = violatedRuleList;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFilePath() {
		return this.filePath;
	}

	public List<ViolatedRule> getViolatedRuleList() {
		return this.violatedRuleList;
	}

	public void addViolatedRule(ViolatedRule vr) {
		this.violatedRuleList.add(vr);
	}

	public int getViolationCount() {
		return this.violatedRuleList.size();
	}

	@Override
	public String toString() {
		return this.filePath;
	}

	public Map<String, Integer> getViolationResult() {
		Map<String, Integer> violationResult = new HashMap<String, Integer>();

		for (Iterator<ViolatedRule> itr = this.violatedRuleList.iterator(); itr.hasNext();) {
			ViolatedRule vr = itr.next();

			// 각 엔트리 항목 : Map<룰 키, 발생 빈도>
			violationResult.put(vr.getRuleKey(), vr.getErrorOccurenceCount());
		}

		return violationResult;
	}

	public void addTmpRule(String tmpRule, int count) {
		tmpRuleKeys.add(tmpRule);
		tmpErrorCountList.add(count);
	}
public List<Integer> getTmpErrorCountList() {
	return this.tmpErrorCountList;
}
	public ArrayList<String> getTmpRuleKeys() {
		return this.tmpRuleKeys;
	}
	
	public Map<String, Integer> getTmpViolationResult() {
		Map<String, Integer> violationResult = new HashMap<String, Integer>();
		
		for(int i=0; i<tmpRuleKeys.size();i++) {
			violationResult.put(tmpRuleKeys.get(i), tmpErrorCountList.get(i));
		}

		return violationResult;
	}
}