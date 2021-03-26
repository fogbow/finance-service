package cloud.fogbow.fs.core.plugins;

import cloud.fogbow.common.exceptions.InternalServerErrorException;

/**
 * A payment management abstraction, capable of starting
 * payment for users based in their financial state. This 
 * interface also contains methods for providing information
 * on the users financial state.
 */
public interface PaymentManager {
	
	/**
	 * Verifies if the user financial state is adequate to 
	 * perform new operations and allocate new resources.
	 * The meaning of this operation depends on the payment type.
	 * For example, in a postpaid scenario, this operation might 
	 * check if the user has paid the previous invoices, whereas 
	 * in a prepaid scenario, this operation might check if the user
	 * still has credits to use.
	 * 
	 * @param userId the id of the user whose state must be evaluated.
	 * @return a boolean stating whether the user state is adequate or not.
	 */
	boolean hasPaid(String userId, String provider);
	
	/**
	 * Starts payment for the resources consumed by the 
	 * given user. The meaning of this operati@Override
	on depends on the
	 * payment type. For example, in a postpaid scenario, 
	 * this operation is an invoice generation process, 
	 * whereas in a prepaid scenario, this operation is a user 
	 * credits update process.
	 * 
	 * @param userId the id of the user to start payment.
	 * @throws InternalServerErrorException 
	 */
	void startPaymentProcess(String userId, String provider) throws InternalServerErrorException;
	
	/**
	 * Generates and returns a representation of the given property of
	 * a user finance state.
	 * 
	 * @param userId the id of the user whose state is used in the operation.
	 * @param property a description of the property to generate.
	 * @return a representation of the property.
	 */
	String getUserFinanceState(String userId, String provider, String property);
	
	// TODO documentation
	void setFinancePlan(String planName);
}
