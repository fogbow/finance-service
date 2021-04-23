package cloud.fogbow.fs.core.models;

public enum OperationType {
    RELOAD("reload"),
    ADD_USER("addUser"),
    REMOVE_USER("removeUser"),
    CHANGE_OPTIONS("changeOptions"),
    UPDATE_FINANCE_STATE("updateFinanceState"),
    GET_FINANCE_STATE("getFinanceState"),
    CREATE_FINANCE_PLAN("createFinancePlan"),
    GET_FINANCE_PLAN("getFinancePlan"),
    UPDATE_FINANCE_PLAN("updateFinancePlan"),
    REMOVE_FINANCE_PLAN("removeFinancePlan");

    private String value;
    
    OperationType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return this.value;
    }
    
    public static OperationType fromString(String value) {
        for (OperationType operationValue : values()) {
            if (operationValue.getValue().equals(value)) { 
                return operationValue;
            }
        }
        throw new IllegalArgumentException();
    }
}
