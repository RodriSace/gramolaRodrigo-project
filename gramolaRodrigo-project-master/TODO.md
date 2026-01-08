# TODO: Selenium Test Fixes

## Completed Tasks
- [x] Fixed login field selector from "username" to "email" in both tests
- [x] Updated SuccessfulPaymentTest to follow functional flow: login -> search song -> click pay -> handle Stripe iframe -> verify success
- [x] Updated FailedPaymentTest to follow functional flow: login -> search song -> click pay -> handle Stripe iframe -> verify error
- [x] Used correct test card numbers: 4242424242424242 for success, 4111111111111111 for failure

## Remaining Tasks
- [ ] Run the updated Selenium tests to verify they work correctly
- [ ] Check that the tests can handle the Stripe iframe properly
- [ ] Verify database changes are checked (song added to queue for successful payment)
- [ ] Ensure tests pass in headless mode

## Notes
- Tests now follow proper user flow instead of directly navigating to payment page
- Stripe payment form is handled by switching to iframe and filling test card details
- Success test verifies song is added to queue via toast notification
- Failed test verifies error message is displayed
