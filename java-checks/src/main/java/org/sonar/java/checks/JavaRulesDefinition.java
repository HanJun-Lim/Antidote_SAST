package org.sonar.java.checks;

/*
 * SonarQube Java Custom Rules Example
 * Copyright (C) 2016-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.annotations.RuleTemplate;
import org.sonar.java.checks.RulesList;

/**
 * Declare rule metadata in server repository of rules.
 * That allows to list the rules in the page "Rules".
 */
public class JavaRulesDefinition implements RulesDefinition {

    // don't change that because the path is hard coded in CheckVerifier
    private static final String RESOURCE_BASE_PATH = "/org/sonar/l10n/java/rules/java";

    public static final String REPOSITORY_KEY = "KITProject-repos";

    private final Gson gson = new Gson();


    // repository에 context를 정의
    @Override
    public void define(Context context) {

        NewRepository repository = context
                .createRepository(REPOSITORY_KEY, "java")
                .setName("KITProject Rules Repository");

        // --------------- 룰 리스트로부터 룰을 하나하나 받아오는 반복문 ---------------
        for (Class<? extends JavaCheck> check : RulesList.getChecks()) {
            // check : 룰 리스트에 있는 룰 중 하나

            // 룰 중 하나에 대한 정보를 repository에 load
            new RulesDefinitionAnnotationLoader().load(repository, check);

            // 해당 룰에 대한 정보를 repository에 load
            newRule(check, repository);
        }

        repository.done();
    }

    // 해당 룰(ruleClass)에 대한 메타 데이터 정보를 repository에 등록
    protected void newRule(Class<? extends JavaCheck> ruleClass, NewRepository repository) {

        // Rule Annotation 정보를 get
        org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.check.Rule.class);

        // 1. rule annotation 정보가 없으면 예외 발생 (예외는 단순히 콘솔에 메시지 출력)
        if (ruleAnnotation == null) {
            throw new IllegalArgumentException("No Rule annotation was found on " + ruleClass);
        }

        // 2. rule key 정보를 체크함. key 정보가 없다면 예외 발생 (예외는 단순히 콘솔에 메시지 출력)
        String ruleKey = ruleAnnotation.key();
        if (StringUtils.isEmpty(ruleKey)) {
            throw new IllegalArgumentException("No key is defined in Rule annotation of " + ruleClass);
        }

        // 3. repository 로부터 rule key 이용하여 해당 룰의 정보(NewRule)를 얻어옴
        //    rule이 null이라면 key 값에 해당하는 value 가 생성되지 않은 것이므로 예외 발생 (예외는 단순히 콘솔에 메시지 출력)
        NewRule rule = repository.rule(ruleKey);
        if (rule == null) {
            throw new IllegalStateException("No rule was created for " + ruleClass + " in " + repository.key());
        }

        ruleMetadata(rule);

        rule.setTemplate(AnnotationUtils.getAnnotation(ruleClass, RuleTemplate.class) != null);
    }

    private void ruleMetadata(NewRule rule) {
        String metadataKey = rule.key();
        addHtmlDescription(rule, metadataKey);
        addMetadata(rule, metadataKey);
    }

    private void addMetadata(NewRule rule, String metadataKey) {
        URL resource = JavaRulesDefinition.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.json");
        if (resource != null) {
            RuleMetatada metatada = gson.fromJson(readResource(resource), RuleMetatada.class);
            rule.setSeverity(metatada.defaultSeverity.toUpperCase(Locale.US));
            rule.setName(metatada.title);
            rule.addTags(metatada.tags);
            rule.setType(RuleType.valueOf(metatada.type));
            rule.setStatus(RuleStatus.valueOf(metatada.status.toUpperCase(Locale.US)));
            if (metatada.remediation != null) {
                rule.setDebtRemediationFunction(metatada.remediation.remediationFunction(rule.debtRemediationFunctions()));
                rule.setGapDescription(metatada.remediation.linearDesc);
            }
        }
    }

    private static void addHtmlDescription(NewRule rule, String metadataKey) {
        URL resource = JavaRulesDefinition.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.html");
        if (resource != null) {
            rule.setHtmlDescription(readResource(resource));
        }
    }

    private static String readResource(URL resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read: " + resource, e);
        }
    }

    private static class RuleMetatada {
        String title;
        String status;
        @Nullable
        Remediation remediation;

        String type;
        String[] tags;
        String defaultSeverity;
    }

    private static class Remediation {
        String func;
        String constantCost;
        String linearDesc;
        String linearOffset;
        String linearFactor;

        public DebtRemediationFunction remediationFunction(DebtRemediationFunctions drf) {
            if (func.startsWith("Constant")) {
                return drf.constantPerIssue(constantCost.replace("mn", "min"));
            }
            if ("Linear".equals(func)) {
                return drf.linear(linearFactor.replace("mn", "min"));
            }
            return drf.linearWithOffset(linearFactor.replace("mn", "min"), linearOffset.replace("mn", "min"));
        }
    }

}