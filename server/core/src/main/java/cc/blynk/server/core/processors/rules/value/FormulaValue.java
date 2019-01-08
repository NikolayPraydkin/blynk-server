package cc.blynk.server.core.processors.rules.value;

import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.core.processors.rules.value.params.FormulaParamBase;
import cc.blynk.server.exp4j.Expression;
import cc.blynk.server.exp4j.ExpressionBuilder;
import cc.blynk.utils.NumberUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 27.12.18.
 */
public class FormulaValue extends ValueBase {

    public final String formula;

    public final Map<String, FormulaParamBase> formulaParams;

    public final transient Expression expression;

    @JsonCreator
    public FormulaValue(@JsonProperty("formula") String formula,
                        @JsonProperty("formulaParams") Map<String, FormulaParamBase> formulaParams) {
        this.formula = formula;
        this.formulaParams = formulaParams;
        this.expression = new ExpressionBuilder(formula)
                .variables(formulaParams.keySet())
                .build();
        this.expression.validateExpression();
    }

    @Override
    public boolean isValid() {
        return formula != null && !formula.isEmpty() && formulaParams != null;
    }

    private static Expression setVariable(Expression newExpressionCopy, String paramName, Object resolvedValue) {
        if (resolvedValue instanceof String) {
            double resultDouble = NumberUtil.parseDouble((String) resolvedValue);
            return newExpressionCopy.setVariableWithoutCheck(paramName, resultDouble);
        } else if (resolvedValue instanceof double[]) {
            double[] resultDoubleArray = (double[]) resolvedValue;
            return newExpressionCopy.setVariableWithoutCheck(paramName, resultDoubleArray);
        }
        throw new RuntimeException("Unexpected formula value result.");
    }

    @Override
    public double resolve(Organization org, Device device, String triggerValue) {
        //todo this is not optimal, we may cache expression per device
        Expression newExpressionCopy = new Expression(this.expression);

        for (Map.Entry<String, FormulaParamBase> entry : this.formulaParams.entrySet()) {
            FormulaParamBase formulaParam = entry.getValue();

            Object resolvedValue = formulaParam.resolve(org, device, triggerValue);
            if (resolvedValue == null) {
                return NumberUtil.NO_RESULT;
            }

            String paramName = entry.getKey();
            setVariable(newExpressionCopy, paramName, resolvedValue);

        }
        return newExpressionCopy.evaluate();
    }

}
