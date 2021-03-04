package cloud.fogbow.fs.core.plugins.authorization;

import cloud.fogbow.common.models.FogbowOperation;

public class FsOperation extends FogbowOperation {
	// The purpose of this implementation is to 
	// override the default behavior of equals
	// (check if the objects are exactly the same),
	// allowing some tests using mocked methods 
	// which receive FsOperation objects as argument.
	@Override
	public boolean equals(Object o) {
		return true;
	}
}
