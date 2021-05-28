package cloud.fogbow.fs.api.parameters;

import java.util.HashMap;

public class FinanceOptions {
    private HashMap<String, String> financeOptions;

    public FinanceOptions() {
        
    }
    
    public FinanceOptions(HashMap<String, String> financeOptions) {
        this.financeOptions = financeOptions;
    }

    public HashMap<String, String> getFinanceOptions() {
        return financeOptions;
    }
}
