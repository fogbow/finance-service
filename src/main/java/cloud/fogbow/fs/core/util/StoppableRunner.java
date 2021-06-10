package cloud.fogbow.fs.core.util;

// Maybe we should move this class to common package and
// remove this version and RAS'.
public abstract class StoppableRunner implements Runnable {

    private boolean mustStop;
	private boolean isActive;
	protected long sleepTime;

	public void stop() {
        this.mustStop = true;
        while (isActive()) {
            try {
                // TODO Currently this stop method only works with this sleep
                // Needs further investigation
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }            
        this.mustStop = false;
    }
    
	// I believe this method should be called
	// after the doRun call, instead of being 
	// called by the doRun implementation
    protected void checkIfMustStop() { 
        if (this.mustStop) {
            this.isActive = false;
        }
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    @Override
    public void run() {
        this.isActive = true;
        while (isActive) {
            try {
                doRun();
                Thread.sleep(this.sleepTime);
            } catch (InterruptedException e) {
                isActive = false;
            }           
        }
    }
    
    public abstract void doRun();
}