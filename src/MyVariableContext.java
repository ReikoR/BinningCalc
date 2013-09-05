import binning.VariableContext;

public class MyVariableContext implements VariableContext {
    private String[] varNames;
    String validMaskExpression = "";

    public MyVariableContext(String... varNames) {
        this.varNames = varNames;
    }

    @Override
    public int getVariableCount() {
        return varNames.length;
    }

    @Override
    public String getVariableName(int i) {
        return varNames[i];
    }

    @Override
    public int getVariableIndex(String name) {
        for (int i = 0; i < varNames.length; i++) {
            if (name.equals(varNames[i])) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getVariableExpression(int i) {
        return null;
    }


    @Override
    public String getValidMaskExpression() {
        return validMaskExpression;
    }

    public void setValidMaskExpression(String expression) {
        validMaskExpression =  expression;
    }
}
